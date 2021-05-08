/*
 * This File is Part of LDProtest
 * Copyright (C) 2021 Covid Anti Hysterics League
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Lesser Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/
package ldprotest.server.auth;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import ldprotest.db.IndexTools;
import ldprotest.db.MainDatabase;
import ldprotest.db.codec.BsonSerializableCodec;
import ldprotest.main.ServerTime;
import ldprotest.serialization.BsonSerializable;
import ldprotest.serialization.ReflectiveConstructor;
import ldprotest.server.auth.AuthFailureLockout.AuthFailureResult;
import ldprotest.server.auth.AuthFailureLockout.RecordFailureError;
import ldprotest.util.ErrorCode;
import ldprotest.util.Result;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UserAccount {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserAccount.class);

    private static final String USER_ACCOUNT_COLLECTION_NAME = "users";

    private static final String DEFAULT_ALGO = "PBKDF2WithHmacSHA256";
    private static final int DEFAULT_ITERATIONS = 10000;

    private static final String PRNG_ALOGRITHM = "NativePRNG";
    private static final int SALT_BYTE_LENGTH = 32;

    private static final int PBKDF2_KEYLEN = 64 * 8;

    private UserAccount() {
        /** do not construct **/
    }

    public static void setupDbIndex() {
        IndexOptions options = new IndexOptions();
        MongoCollection<UserCredentialInfo> collection = collection();

        options.unique(true);

        for(Bson index: IndexTools.reflectiveBuildIndexes(UserCredentialInfo.class)) {
            collection.createIndex(index,options);
        }
    }

    public static Result<UserCreationError, UserInfo> create(
        String username, String email, UserRole role, String secret
    ) {

        MongoCollection<UserCredentialInfo> collection = collection();
        byte[] salt = generateSalt();
        byte[] hash = hashPassword(secret, salt, DEFAULT_ALGO, DEFAULT_ITERATIONS);

        UserCredentialInfo data = new UserCredentialInfo(
            UserInfo.generate(username, email, role),
            hash,
            salt,
            DEFAULT_ALGO,
            DEFAULT_ITERATIONS,
            Optional.empty()
        );

        try {
            try {
                collection.insertOne(data);
                return Result.success(data.info);
            } catch(MongoWriteException ex) {

                if(ex.getError().getCategory().equals(ErrorCategory.DUPLICATE_KEY)) {
                    LOGGER.warn("Attempted creation of duplicate user: {}, {} {}", username, email);
                    return Result.failure(UserCreationError.DUPLICATE_USER);
                } else {
                    throw ex;
                }
            }
        } catch(MongoException ex) {
            LOGGER.error("Database error creating user", ex);
            return Result.failure(UserCreationError.DATABASE_ERROR);
        }
    }

    public static Result<UserAuthenticationFailure, UserInfo> authenticate(String email, String secret) {

        MongoCollection<UserCredentialInfo> collection = collection();
        UserCredentialInfo creds;

        try {
            creds = collection.find(Filters.eq("info.email", email)).first();
        } catch(MongoException ex) {
            LOGGER.error("Database error looking up user", ex);
            return Result.failure(new UserAuthenticationFailure(UserLookupErrorCode.DATABASE_ERROR));
        }

        if(creds == null) {
            return Result.failure(new UserAuthenticationFailure(UserLookupErrorCode.INVALID_USER));
        }

        if(creds.locked) {
            return Result.failure(new UserAuthenticationFailure(UserAuthErrorCode.ACCOUNT_LOCKED));
        } else if(creds.temporarilyLoginLocked()) {
            return Result.failure(new UserAuthenticationFailure(creds.loginLockedUntil.get()));
        }

        byte[] hash = hashPassword(secret, creds.salt, creds.algo, creds.iterations);

        if(secureCompare(hash, creds.hashedSecret)) {
            clearFailure(creds);

            return Result.success(creds.info);
        } else {
            recordFailure(creds);
            return Result.failure(new UserAuthenticationFailure(UserAuthErrorCode.INVALID_CREDENTIALS));
        }
    }

    public static Result<UserLookupErrorCode, UserAccountStatus> status(UserInfo info) {
        return status(info.email);
    }

    public static Result<UserLookupErrorCode, UserAccountStatus> status(String email) {
        MongoCollection<UserCredentialInfo> collection = collection();
        UserCredentialInfo creds;

        try {
            creds = collection.find(Filters.eq("info.email", email)).first();
        } catch(MongoException ex) {
            LOGGER.error("Database error looking up user", ex);
            return Result.failure(UserLookupErrorCode.DATABASE_ERROR);
        }

        if(creds == null) {
            return Result.failure(UserLookupErrorCode.INVALID_USER);
        }

        if(creds.locked) {
            return Result.success(new UserAccountStatus(UserAccountStatusCode.ACCOUNT_LOCKED));
        } else if(creds.temporarilyLoginLocked()) {
            return Result.success(new UserAccountStatus(creds.loginLockedUntil.get()));
        } else {
            return Result.success(new UserAccountStatus(UserAccountStatusCode.ACCUNT_OKAY));
        }
    }

    public static Result<UserLookupErrorCode, UserInfo> lookupByUsername(String username) {
        MongoCollection<UserCredentialInfo> collection = collection();

        try {
            UserCredentialInfo allInfo = collection.find(Filters.eq("info.publicUsername", username)).first();

            if(allInfo == null) {
                return Result.failure(UserLookupErrorCode.INVALID_USER);
            } else {
                return Result.success(allInfo.info);
            }
        } catch(MongoException ex) {
            LOGGER.error("Database error when looking up user", ex);
            return Result.failure(UserLookupErrorCode.DATABASE_ERROR);
        }
    }

    public static ErrorCode<UserLookupErrorCode> lockUntil(UserInfo info, ZonedDateTime until) {
        MongoCollection<UserCredentialInfo> collection = collection();

        try {
            UpdateResult result = collection.updateOne(
                Filters.and(
                    Filters.eq("info.email", info.email), Filters.eq("info.publicUsername", info.publicUsername)
                ),
                Updates.set("loginLockedUntil", BsonSerializableCodec.bsonZonedDateTime(until))
            );

            if(result.wasAcknowledged()) {
                return ErrorCode.success();
            } else {
                LOGGER.warn("attempted to lock nonexistent user");
                return ErrorCode.error(UserLookupErrorCode.INVALID_USER);
            }

        } catch(MongoException ex) {
            LOGGER.error("Database error when locking user", ex);
            return ErrorCode.error(UserLookupErrorCode.DATABASE_ERROR);
        }
    }

    public static ErrorCode<UserLookupErrorCode> lock(UserInfo info) {
        return setLock(info, true);
    }

    public static ErrorCode<UserLookupErrorCode> unlock(UserInfo info) {
        return setLock(info, false);
    }

    private static ErrorCode<UserLookupErrorCode> setLock(
        UserInfo info, boolean lockValue
    ) {
        MongoCollection<UserCredentialInfo> collection = collection();

        try {
            UpdateResult result;
            if(lockValue) {
                result = collection.updateOne(
                    Filters.and(
                        Filters.eq("info.email", info.email), Filters.eq("info.publicUsername", info.publicUsername)
                    ),
                    Updates.set("locked", lockValue)
                );
            } else {
                result = collection.updateOne(
                    Filters.and(
                        Filters.eq("info.email", info.email), Filters.eq("info.publicUsername", info.publicUsername)
                    ),
                    Updates.combine(Updates.set("locked", lockValue), Updates.unset("loginLockedUntil"))
                );
            }

            if(result.wasAcknowledged()) {
                return ErrorCode.success();
            } else {
                LOGGER.warn("attempted to lock nonexistent user");
                return ErrorCode.error(UserLookupErrorCode.INVALID_USER);
            }
        } catch(MongoException ex) {
            LOGGER.error("Database error when locking user", ex);
            return ErrorCode.error(UserLookupErrorCode.DATABASE_ERROR);
        }
    }

    private static void recordFailure(UserCredentialInfo creds) {

        if(creds.info.userRole.equals(UserRole.ADMIN)) {
            /* Admin accounts cannot be locked due to excessive failures */
            return;
        }

        Result<RecordFailureError, AuthFailureResult> result = AuthFailureLockout.recordFailure(
            creds.info.globalUniqueId
        );

        if(result.isSuccess()) {
            AuthFailureResult authFail = result.result();

            if(authFail.mustLock()) {
                lockUntil(creds.info, authFail.lockUntil.get());
            }
        }
    }

    private static void clearFailure(UserCredentialInfo creds) {
        AuthFailureLockout.clearFailures(creds.info.globalUniqueId);
    }

    private static MongoCollection<UserCredentialInfo> collection() {
        return MainDatabase.database().getCollection(USER_ACCOUNT_COLLECTION_NAME, UserCredentialInfo.class);
    }

    private static byte[] hashPassword(String password, byte[] salt, String algo, int iterations) {

        SecretKeyFactory skf;

        try {
            skf = SecretKeyFactory.getInstance(DEFAULT_ALGO);
        } catch (NoSuchAlgorithmException ex) {
            LOGGER.error("Error hashing password", ex);
            throw new UserCreationException("No such password algorithm " + DEFAULT_ALGO);
        }

        if(!algo.equals(DEFAULT_ALGO) || iterations != DEFAULT_ITERATIONS ) {
            LOGGER.warn("Hashing password with non-default algorithm and/or iterations.");
        }

        try {
            if(algo.equals("PBKDF2WithHmacSHA256")) {
                PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, PBKDF2_KEYLEN);
                return skf.generateSecret(spec).getEncoded();
            } else {
                LOGGER.error("Unsupported password hash algorithm: {}", algo);
                throw new UserCreationException("Unsupported hash algorithm");
            }
        } catch (InvalidKeySpecException ex) {
            LOGGER.error("Error hashing password", ex);
            throw new UserCreationException("Invalid key spec while hashing password");
        }
    }

    private static byte[] generateSalt() {

        byte[] salt = new byte[SALT_BYTE_LENGTH];

        try {
            SecureRandom.getInstance(PRNG_ALOGRITHM).nextBytes(salt);
        } catch (NoSuchAlgorithmException ex) {
            LOGGER.error("Error generating password salt", ex);
            throw new UserCreationException("Error generating salt for new user");
        }

        return salt;
    }

    /**
     * Attempt to avoid side-channel attacks with comparison
     *
     * Compare equality of two byte arrays without using early exit. This mitigates timing attacks with leak information
     * about the password hash by taking careful timing measurements.
     *
     * Realistically, this is probably being excessively cautious (and the optimizer may defeat the attempt anyway) but
     * we do this anyway if only to take security as seriously as possible.
     *
     * @param h1
     * @param h2
     * @return
     */
    private static boolean secureCompare(byte[] h1, byte[] h2) {
        AtomicBoolean ret = new AtomicBoolean(h1.length == h2.length);
        for(int i = 0; i < Math.min(h1.length, h2.length); i++) {
            if(h1[i] != h2[i]) {
                ret.set(false);
            }
        }
        return ret.get();
    }

    private static final class UserCredentialInfo implements BsonSerializable {

        public final UserInfo info;

        public final byte[] hashedSecret;
        public final byte[] salt;
        public final String algo;
        public final int iterations;

        public final boolean locked;
        public final Optional<ZonedDateTime> loginLockedUntil;

       @ReflectiveConstructor
        private UserCredentialInfo() {
            info = null;
            hashedSecret = null;
            salt = null;
            algo = null;
            iterations = 0;
            locked = true;
            loginLockedUntil = null;
        }

        public UserCredentialInfo(
            UserInfo info,
            byte[] hashedSecret,
            byte[] salt,
            String algo,
            int iterations,
            Optional<ZonedDateTime> lockUntil
        ) {
            this.info = info;
            this.hashedSecret = hashedSecret;
            this.salt = salt;
            this.algo = algo;
            this.iterations = iterations;
            this.locked = false;
            this.loginLockedUntil = lockUntil;
        }

        public boolean temporarilyLoginLocked() {
            if(loginLockedUntil.isPresent()){
                return loginLockedUntil.get().isAfter(ServerTime.now());
            } else {
                return false;
            }
        }
    }

    public static enum UserAuthErrorCode {
        UNDEFINED_FAILURE("Failure of an undefined nature"),
        INVALID_CREDENTIALS("Invalid password"),
        ACCOUNT_LOCKED("Account is locked for logon or sesssion refresh"),
        ACCOUNT_LOCKED_TEMPORARILY("Account is locked for login, due to excessive login failures");

        public final String description;

        UserAuthErrorCode(String descrption) {
            this.description = descrption;
        }
    }

    public static enum UserAccountStatusCode {

        ACCUNT_OKAY("User exists and account is in a normal state"),
        ACCOUNT_LOCKED("Account is locked for logon or sesssion refresh"),
        ACCOUNT_LOCKED_TEMPORARILY("Account is locked for login, due to excessive login failures");

        public final String description;

        UserAccountStatusCode(String descrption) {
            this.description = descrption;
        }
    }

    public static enum UserLookupErrorCode {
        DATABASE_ERROR("Low level database error"),
        INVALID_USER("User does not exist");

        public final String description;

        UserLookupErrorCode(String descrption) {
            this.description = descrption;
        }
    }

    public static final class UserAuthenticationFailure {
        public final ErrorCode<UserLookupErrorCode> lookupError;
        public final ErrorCode<UserAuthErrorCode> authError;

        private final Optional<ZonedDateTime> lockUntil;

        private UserAuthenticationFailure(UserLookupErrorCode lookupError) {
            this.lookupError = ErrorCode.error(lookupError);
            this.authError = ErrorCode.error(UserAuthErrorCode.UNDEFINED_FAILURE);
            this.lockUntil = Optional.empty();
        }

        private UserAuthenticationFailure(UserAuthErrorCode authError) {
            this.lookupError = ErrorCode.success();
            this.authError = ErrorCode.error(authError);
            this.lockUntil = Optional.empty();
        }

        private UserAuthenticationFailure(ZonedDateTime lockedUntil) {
            this.lookupError = ErrorCode.success();
            this.authError = ErrorCode.error(UserAuthErrorCode.ACCOUNT_LOCKED_TEMPORARILY);
            this.lockUntil = Optional.of(lockedUntil);
        }

        public ZonedDateTime lockedUntil() {
            return lockUntil.get();
        }
    }

    public static final class UserAccountStatus {

        public final UserAccountStatusCode code;
        private final Optional<ZonedDateTime> lockUntil;

        private UserAccountStatus(UserAccountStatusCode code) {
            this.code = code;
            this.lockUntil = Optional.empty();
        }

        private UserAccountStatus(ZonedDateTime lockUntil) {
            this.code = UserAccountStatusCode.ACCOUNT_LOCKED_TEMPORARILY;
            this.lockUntil = Optional.of(lockUntil);
        }

        public ZonedDateTime lockedUntil() {
            return lockUntil.get();
        }
    }

    public static enum  UserCreationError {
        DATABASE_ERROR("Low level database error"),
        DUPLICATE_USER("User already exists");

        public final String description;

        UserCreationError(String descrption) {
            this.description = descrption;
        }
    }

    public static final class UserCreationException extends RuntimeException {

        public UserCreationException(String message) {
            super(message);
        }

        @Override
        public String toString() {
            return "UserCreationError: " + getMessage();
        }
    }
}

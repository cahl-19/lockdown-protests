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
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import ldprotest.db.IndexTools;
import ldprotest.db.MainDatabase;
import ldprotest.serialization.BsonSerializable;
import ldprotest.serialization.ReflectiveConstructor;
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
            DEFAULT_ITERATIONS
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

    public static Result<UserLookupError, UserInfo> authenticate(String email, String secret) {

        MongoCollection<UserCredentialInfo> collection = collection();
        UserCredentialInfo creds;

        try {
            creds = collection.find(Filters.eq("info.email", email)).first();
        } catch(MongoException ex) {
            LOGGER.error("Database error looking up user", ex);
            return Result.failure(UserLookupError.DATABASE_ERROR);
        }

        if(creds == null) {
            return Result.failure(UserLookupError.INVALID_USER);
        }

        if(creds.locked) {
            return Result.failure(UserLookupError.ACCOUNT_LOCKED);
        }

        byte[] hash = hashPassword(secret, creds.salt, creds.algo, creds.iterations);

        if(secureCompare(hash, creds.hashedSecret)) {
            return Result.success(creds.info);
        } else {
            return Result.failure(UserLookupError.INVALID_CREDENTIALS);
        }
    }

    public static ErrorCode<UserLookupError> status(UserInfo info) {
        return status(info.email);
    }

    public static ErrorCode<UserLookupError> status(String email) {
        MongoCollection<UserCredentialInfo> collection = collection();
        UserCredentialInfo creds;

        try {
            creds = collection.find(Filters.eq("info.email", email)).first();
        } catch(MongoException ex) {
            LOGGER.error("Database error looking up user", ex);
            return ErrorCode.error(UserLookupError.DATABASE_ERROR);
        }

        if(creds == null) {
            return ErrorCode.error(UserLookupError.INVALID_USER);
        }

        if(creds.locked) {
            return ErrorCode.error(UserLookupError.ACCOUNT_LOCKED);
        }

        return ErrorCode.success();
    }

    public static ErrorCode<UserLookupError> lock(UserInfo info) {
        return setLock(info, true);
    }

    public static ErrorCode<UserLookupError> unlock(UserInfo info) {
        return setLock(info, false);
    }

    private static ErrorCode<UserLookupError> setLock(UserInfo info, boolean lockValue) {
        MongoCollection<UserCredentialInfo> collection = collection();

        try {
            UpdateResult result = collection.updateOne(
                Filters.and(Filters.eq("info.email", info.email), Filters.eq("info.publicUsername")),
                Updates.set("locked", lockValue)
            );

            if(result.wasAcknowledged()) {
                return ErrorCode.success();
            } else {
                return ErrorCode.error(UserLookupError.INVALID_USER);
            }

        } catch(MongoException ex) {
            LOGGER.error("Database error when locking user", ex);
            return ErrorCode.error(UserLookupError.DATABASE_ERROR);
        }
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

       @ReflectiveConstructor
        private UserCredentialInfo() {
            info = null;
            hashedSecret = null;
            salt = null;
            algo = null;
            iterations = 0;
            locked = true;
        }

        public UserCredentialInfo(UserInfo info, byte[] hashedSecret, byte[] salt, String algo, int iterations) {
            this.info = info;
            this.hashedSecret = hashedSecret;
            this.salt = salt;
            this.algo = algo;
            this.iterations = iterations;
            this.locked = false;
        }
    }

    public static enum UserLookupError {
        DATABASE_ERROR("Low level database error"),
        INVALID_USER("User does not exist"),
        INVALID_CREDENTIALS("Incorrect password"),
        ACCOUNT_LOCKED("Account is locked for logon");

        public final String description;

        UserLookupError(String descrption) {
            this.description = descrption;
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

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
package ldprotest.server.auth.webtoken;

import com.auth0.jwt.interfaces.RSAKeyProvider;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import ldprotest.main.Main;
import ldprotest.main.ServerTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VolatileTokenKeyProvider {

    private final static Logger LOGGER = LoggerFactory.getLogger(VolatileTokenKeyProvider.class);

    private static final int RSA_KEY_SIZE = 4096;

    private final AtomicReference<KeyData> latestKeyPair;
    private final Map<UUID, KeyData> oldKeys;

    private final int keyDeletionTimeoutSeconds;

    private final PriorityQueue<KeyData> cleanupQueue;

    public VolatileTokenKeyProvider() {

        UUID uuid = UUID.randomUUID();
        KeyData kd = new KeyData(generateKeyPair(), uuid);

        this.keyDeletionTimeoutSeconds = Main.args().tokenKeyDeletionSeconds;

        this.oldKeys = new ConcurrentHashMap<>();
        this.latestKeyPair = new AtomicReference<>(kd);
        this.cleanupQueue = new PriorityQueue<>((a, b) -> {
            if(a.created.isBefore(b.created)) {
                return -1;
            } else if (b.created.isBefore(a.created)) {
                return 1;
            } else {
                return 0;
            }
        });

        oldKeys.put(uuid, kd);
        cleanupQueue.add(kd);
    }

    public final RSAKeyProvider getKeyProvider() {

        doCleanup();
        doRotation();

        KeyData keyData = latestKeyPair.get();

        return new RSAKeyProvider() {
            @Override
            public RSAPublicKey getPublicKeyById(String kid) {

                if(kid == null) {
                    return (RSAPublicKey)keyData.keyPair.getPublic();
                }

                KeyData oldKeyData = oldKeys.get(UUID.fromString(kid));

                if(oldKeyData == null) {
                    LOGGER.info("Request for non-existant RSA key: {}", kid);
                    throw new NoSuchKidException("KID does not exist");
                }

                return (RSAPublicKey)oldKeyData.keyPair.getPublic();
            }

            @Override
            public RSAPrivateKey getPrivateKey() {
                return (RSAPrivateKey)keyData.keyPair.getPrivate();
            }

            @Override
            public String getPrivateKeyId() {
                return keyData.kid.toString();
            }
        };
    }

    private synchronized void doRotation() {
        KeyData keyData = latestKeyPair.get();
        ZonedDateTime now = ServerTime.now();

        if(keyData.created.plusSeconds(Main.args().tokenKeyRotateSeconds).isAfter(now)) {
            return;
        }

        UUID uuid = UUID.randomUUID();
        KeyData kd = new KeyData(generateKeyPair(), uuid);

        oldKeys.put(uuid, kd);
        cleanupQueue.add(kd);
        latestKeyPair.set(kd);
    }

    private synchronized void doCleanup() {
        ZonedDateTime now = ServerTime.now();
         while(!cleanupQueue.isEmpty()) {
            KeyData kd = cleanupQueue.peek();

            if(kd.created.plusSeconds(keyDeletionTimeoutSeconds).isAfter(now)) {
                break;
            }

            cleanupQueue.poll();
            oldKeys.remove(kd.kid);
        }
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(RSA_KEY_SIZE);
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            LOGGER.error("Error generating RSA key", ex);
            throw new KeyGenerationError("Error generating RSA Key");
        }
    }

    private static final class KeyData {
        public final ZonedDateTime created;
        public final KeyPair keyPair;
        public final UUID kid;

        public KeyData(KeyPair keyPair, UUID kid) {
            this.created = ServerTime.now();
            this.keyPair = keyPair;
            this.kid = kid;
        }
    }
}

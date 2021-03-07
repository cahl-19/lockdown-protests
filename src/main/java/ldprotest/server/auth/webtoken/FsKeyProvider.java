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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import ldprotest.main.ServerTime;
import ldprotest.util.Result;
import ldprotest.util.types.MultiMap;
import ldprotest.util.types.Pair;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FsKeyProvider implements DeferredKeyProvider {

    private final static Logger LOGGER = LoggerFactory.getLogger(FsKeyProvider.class);
    private static final String ALGORITHM = "RSA";

    private final LocalKeyProvider delegate;
    private String prefix;

    public FsKeyProvider(String prefix) {

        File f = new File(prefix);

        if(!testFsKeyDirectory(prefix)) {
            throw new IllegalArgumentException(
                "Invalid file location for FS Key store. Directory is inaccesible or doesn't exist: " + prefix
            );
        }

        this.prefix = prefix;
        this.delegate = new LocalKeyProvider(this::loadKeys, this::saveKey, this::deleteKey);
    }

    @Override
    public RSAKeyProvider getKeyProvider() {
        return delegate.getKeyProvider();
    }

    public static boolean testFsKeyDirectory(String path) {
        File f = new File(path);
        return f.exists() && f.isDirectory() && f.canExecute() && f.canRead();
    }

    private Map<UUID, LocalKeyProvider.KeyData> loadKeys() {
        Map<UUID, LocalKeyProvider.KeyData> initialKeys = new HashMap<>();

        List<FileNameInfo> keyFiles = findKeyFiles();
        List<Pair<FileNameInfo, FileNameInfo>> pairs = pairFiles(keyFiles);

        for(Pair<FileNameInfo, FileNameInfo> pair: pairs) {
            assert pair.first.pub;

            File pubKeyFile = new File(filePath(pair.first.filename));
            File privKeyFile = new File(filePath(pair.second.filename));

            try {
                PublicKey pubKey = bytesToPubKey(parsePemFile(pubKeyFile));
                PrivateKey privKey = bytesToPrivKey(parsePemFile(privKeyFile));

                initialKeys.put(
                    pair.first.uuid,
                    new LocalKeyProvider.KeyData(
                        new KeyPair(pubKey, privKey),
                        pair.first.uuid
                    )
                );

            } catch (IOException | IllegalArgumentException ex) {
                LOGGER.warn("Unable to open key files for reading: {}", ex.getMessage());
            }
        }

        return initialKeys;
    }

    private void saveKey(LocalKeyProvider.KeyData kd) {
        writePubPem(kd.keyPair.getPublic(), new File(pubKeyFilePath(kd)));
        writePrivPem(kd.keyPair.getPrivate(), new File(privKeyFilePath(kd)));
    }

    private void deleteKey(LocalKeyProvider.KeyData kd) {
        deleteKeyFile(pubKeyFilePath(kd));
        deleteKeyFile(privKeyFilePath(kd));
    }

    private static void writePubPem(PublicKey key, File f) {
        try(PemWriter writer = new PemWriter(new FileWriter(f))) {
            writer.writeObject(new PemObject("PUBLIC KEY", key.getEncoded()));
        } catch(IOException ex) {
            LOGGER.warn("Unable to write public key file", ex);
        }
    }

    private static void writePrivPem(PrivateKey key, File f) {
        try(PemWriter writer = new PemWriter(new FileWriter(f))) {
            writer.writeObject(new PemObject("PRIVATE KEY", key.getEncoded()));
        } catch(IOException ex) {
            LOGGER.warn("Unable to write private key file", ex);
        }
    }

    private static PublicKey bytesToPubKey(byte[] keyBytes) {
        try {
            KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
            EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);

            return kf.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException| InvalidKeySpecException ex) {
            LOGGER.error("unable to load public key", ex);
            throw new IllegalArgumentException(ex.getMessage());
        }
    }

    private static PrivateKey bytesToPrivKey(byte[] keyBytes) {
        try {
            KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
            EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            return kf.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException| InvalidKeySpecException ex) {
            LOGGER.error("unable to load public key", ex);
            throw new IllegalArgumentException(ex.getMessage());
        }
    }

    private static byte[] parsePemFile(File pemFile) throws IOException {
        try(PemReader reader = new PemReader(new FileReader(pemFile))) {
            PemObject pemObject = reader.readPemObject();
            return pemObject.getContent();
        }
    }

    public static void encodePEMFile(File pemFile) throws IOException {
        try(PemWriter writer = new PemWriter(new FileWriter(pemFile))) {
        }
    }

    private void deleteKeyFile(String fname) {
        File f = new File(filePath(fname));
        if(!f.delete()) {
            LOGGER.warn("Unable to delete key file {}", fname);
        }
    }

    private String pubKeyFilePath(LocalKeyProvider.KeyData kd) {
        StringBuilder sb = new StringBuilder();

        sb.append(prefix);
        sb.append(FileSystems.getDefault().getSeparator());

        sb.append(kd.created.toEpochSecond());
        sb.append('~');
        sb.append(kd.kid);
        sb.append(".pub");

        return sb.toString();
    }

    private String privKeyFilePath(LocalKeyProvider.KeyData kd) {
        StringBuilder sb = new StringBuilder();

        sb.append(prefix);
        sb.append(FileSystems.getDefault().getSeparator());

        sb.append(kd.created.toEpochSecond());
        sb.append('~');
        sb.append(kd.kid);
        sb.append(".priv");

        return sb.toString();
    }

    private List<Pair<FileNameInfo, FileNameInfo>> pairFiles(List<FileNameInfo> l) {
        List<Pair<FileNameInfo, FileNameInfo>> pairs = new ArrayList<>();
        MultiMap<UUID, FileNameInfo> groups = new MultiMap<>();

        for(FileNameInfo fi: l) {
            groups.put(fi.uuid, fi);
        }

        for(Entry<UUID, List<FileNameInfo>> ent : groups.entrySet()) {
            List<FileNameInfo> group = ent.getValue();

            if(group.size() != 2) {
                LOGGER.warn(
                    "Found {} key files for {} when should be 2.",
                    group.size(),
                    ent.getKey()
                );
                continue;
            }

            if(group.get(0).pub == group.get(1).pub) {
                LOGGER.warn("Two keys, but no public/private key pair for {}", ent.getKey());
                continue;
            }

            if(group.get(0).pub) {
                pairs.add(new Pair<>(group.get(0), group.get(1)));
            } else {
                pairs.add(new Pair<>(group.get(1), group.get(0)));
            }
        }

        return pairs;
    }

    private List<FileNameInfo> findKeyFiles() {
        List<FileNameInfo> ret = new ArrayList<>();
        File directory = new File(prefix);

        String[] names = directory.list();

        if(names == null) {
            LOGGER.warn("Path {} does not appear to be a directory, or is inaccessible.", prefix);
            return ret;
        }

        List<String> files = Arrays.asList(names);

        for(String fname: files) {
            File f = new File(filePath(fname));
            Result<String, FileNameInfo> parseResult = FileNameInfo.parseFileName(fname);

            if(parseResult.isFailure()) {
                continue;
            }

            if(!f.canRead()) {
                LOGGER.warn("Located unreadable file matching key pattern: {}", fname);
                continue;
            }

            ret.add(parseResult.result());
        }

        return ret;
    }

    private String filePath(String filename) {
        return prefix + File.separator + filename;
    }

    private static final class FileNameInfo {
        public final String filename;
        public final ZonedDateTime created;
        public final UUID uuid;
        public final boolean pub;

        private FileNameInfo(String filename, UUID uuid, ZonedDateTime created, boolean pub) {
            this.filename = filename;
            this.uuid = uuid;
            this.created = created;
            this.pub = pub;
        }

        public static Result<String, FileNameInfo> parseFileName(String filename) {
            boolean pub;
            long createdEpoch;
            UUID uuid;

            String[] fparts = filename.split("\\.");

            if(fparts.length != 2) {
                return Result.failure("Wrong number of file extensions");
            }

            String name = fparts[0];
            String ext = fparts[1];

            if(!ext.equals("pub") && !ext.equals("priv")) {
                return Result.failure("Unknown file extension");
            }

            pub = ext.equals("pub");

            String[] nparts = name.split("~");

            if(nparts.length != 2) {
                return Result.failure("Wrong number of file name parts");
            }

            try {
                createdEpoch = Long.parseLong(nparts[0]);
            } catch(NumberFormatException ex) {
                return Result.failure(ex.getMessage());
            }

            try {
                uuid = UUID.fromString(nparts[1]);
            } catch(IllegalArgumentException ex) {
                return Result.failure(ex.getMessage());
            }

            return Result.success(new FileNameInfo(
                filename,
                uuid,
                ServerTime.ofEpochSecond(createdEpoch),
                pub
            ));
        }
    }
}

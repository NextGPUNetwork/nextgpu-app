package ai.nextgpu.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bouncycastle.jcajce.SecretKeyWithEncapsulation;
import org.bouncycastle.jcajce.spec.KEMExtractSpec;
import org.bouncycastle.jcajce.spec.KEMGenerateSpec;
import org.bouncycastle.pqc.jcajce.spec.KyberParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * Service class that uses quantum-safe cryptographic operations for encryption.
 * This class demonstrates how to generate a Kyber key pair, encapsulate/decapsulate
 * a shared secret using CRYSTALS-Kyber, and then use the shared secret (or a derived AES key)
 * to encrypt and decrypt data using AES/GCM.
 *
 */
public class SecurityService {
    private static final Logger log = LoggerFactory.getLogger(SecurityService.class);

    /**
     * Demonstrates the generation of a Kyber key pair (kyber1024),
     * followed by the encapsulation (encryption) and decapsulation (decryption)
     * of an ephemeral secret. Logs the details to show that the ephemeral
     * encryption key and decapsulation key match.
     *
     * <p><strong>Warning</strong>: This method is marked <code>@Deprecated</code>
     * and should not be used in production. It is purely illustrative.</p>
     */
    @Deprecated
    public void example() {
        // Bouncy Castle: https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk18on
        // The BC provider must either be defined here, or in SecurityConfig class
        // Security.addProvider(new BouncyCastleProvider());
        try {
            // Generate a Kyber key pair and extract Public and Private keys using 1024-bit Kyber
            KeyPair keyPair = generateKyberKeyPair(KyberParameterSpec.kyber1024);
            PrivateKey privateKey = extractPrivateKey(keyPair);
            PublicKey publicKey = extractPublicKey(keyPair);

            // Encapsulation (sender side): Sender only has the public key
            SecretKeyWithEncapsulation senderKey = generateEncryptionKey(publicKey);
            byte[] encryptionKey = senderKey.getEncoded();       // ephemeral secret

            // Generate a 256-bit AES key to encrypt data
            byte[] aesKey = Arrays.copyOf(encryptionKey, 32);

            // Encrypt data using the AES key
            byte[] plaintext = "Hello from Ayesha!".getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = encrypt(plaintext, aesKey);

            // Receiver will get the encapsulation
            byte[] encapsulated = senderKey.getEncapsulation(); // KEM ciphertext

            // Decapsulation (receiver side): Receiver has the private key and the encapsulated cyphertext
            byte[] decryptionKey = generateDecryptionKey(privateKey, encapsulated);

            // Confirm that the receiver extracted the same decryption key used in encryption
            log.info(" => Encryption key == Decryption key? {}", Arrays.equals(encryptionKey, decryptionKey));

            // Decrypt data using the AES key
            byte[] decrypted = decrypt(encrypted, aesKey);
            String decryptedMessage = new String(decrypted, StandardCharsets.UTF_8);
            log.info(" - Decrypted message:     {}", decryptedMessage);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Generates a Kyber key pair using the given parameter specification.
     * The key pair will include both a public and private key for the specified
     * security level (e.g., kyber512, kyber768, kyber1024).
     *
     * @param spec The Kyber parameter specification (e.g., kyber1024).
     * @return A newly generated {@link KeyPair} (public and private keys).
     * @throws NoSuchAlgorithmException           if the "KYBER" algorithm is not available.
     * @throws NoSuchProviderException            if the "BCPQC" provider is not registered.
     * @throws InvalidAlgorithmParameterException if the {@link KyberParameterSpec} is invalid.
     */
    public KeyPair generateKyberKeyPair(KyberParameterSpec spec)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("KYBER", "BCPQC");
        kpg.initialize(spec, new SecureRandom());
        return kpg.generateKeyPair();
    }

    /**
     * Extracts a Kyber private key from the encoded bytes contained in the private key of the given {@code publicKey}.
     *
     * @param keyPair The key pair from which to extract the private key bytes.
     * @return A {@link PrivateKey} object corresponding to the extracted bytes.
     * @throws NoSuchAlgorithmException if the "KYBER" algorithm is not available.
     * @throws NoSuchProviderException  if the "BCPQC" provider is not registered.
     * @throws InvalidKeySpecException  if the encoded key bytes are invalid.
     */
    public PrivateKey extractPrivateKey(KeyPair keyPair)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        KeyFactory kyberFactory = KeyFactory.getInstance("KYBER", "BCPQC");
        return kyberFactory.generatePrivate(new PKCS8EncodedKeySpec(keyPair.getPrivate().getEncoded()));
    }

    /**
     * Extracts a Kyber public key from the encoded bytes contained in the public key of the given {@code publicKey}.
     *
     * @param keyPair The key pair from which to extract the public key bytes.
     * @return A {@link PublicKey} object corresponding to the extracted bytes.
     * @throws NoSuchAlgorithmException if the "KYBER" algorithm is not available.
     * @throws NoSuchProviderException  if the "BCPQC" provider is not registered.
     * @throws InvalidKeySpecException  if the encoded key bytes are invalid.
     */
    public PublicKey extractPublicKey(KeyPair keyPair)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        return KeyFactory.getInstance("KYBER", "BCPQC")
                .generatePublic(new X509EncodedKeySpec(keyPair.getPublic().getEncoded()));
    }

    /**
     * Generates a {@link PublicKey} object from a string representation of a Kyber public key.
     *
     * @param publicKeyBase64 The base64-encoded string representation of the Kyber public key.
     * @return A {@link PublicKey} object corresponding to the provided public key string.
     * @throws NoSuchAlgorithmException if the "KYBER" algorithm is not available.
     * @throws InvalidKeySpecException  if the encoded key bytes are invalid.
     * @throws NoSuchProviderException  if the "BCPQC" provider is not registered.
     */
    public PublicKey publicKeyFromBase64(String publicKeyBase64)
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        byte[] der = Base64.getDecoder().decode(publicKeyBase64);
        return KeyFactory.getInstance("KYBER", "BCPQC")
                .generatePublic(new X509EncodedKeySpec(der));
    }

    public String publicKeyToBase64(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Generates (encapsulates) an ephemeral secret key using Kyber KEM,
     * paired with the provided public key. This ephemeral secret can then
     * be used for symmetric encryption (e.g. AES). Additionally, a KEM ciphertext
     * (the "encapsulated" key) is produced, which is needed by the party
     * holding the private key to recover (decapsulate) the same ephemeral secret.
     *
     * @param pubKey The recipient's Kyber public key.
     * @return A {@link SecretKeyWithEncapsulation} containing both the ephemeral
     * secret ({@link SecretKeyWithEncapsulation#getEncoded()}) and
     * the encapsulated key data
     * ({@link SecretKeyWithEncapsulation#getEncapsulation()}).
     * @throws NoSuchAlgorithmException           if the "KYBER" algorithm is not available.
     * @throws NoSuchProviderException            if the "BCPQC" provider is not registered.
     * @throws InvalidAlgorithmParameterException if specifying an unsupported cipher or spec.
     */
    public SecretKeyWithEncapsulation generateEncryptionKey(PublicKey pubKey)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        KeyGenerator keyGen = KeyGenerator.getInstance("KYBER", "BCPQC");
        // request an AES secret from the KEM
        keyGen.init(new KEMGenerateSpec(pubKey, "AES"), new SecureRandom());
        return (SecretKeyWithEncapsulation) keyGen.generateKey();
    }

    /**
     * Decapsulates (extracts) the same ephemeral secret from a Kyber KEM ciphertext.
     * This is done by using the private key plus the "encapsulated" data
     * to recover the same secret that was generated by {@link #generateEncryptionKey(PublicKey)}.
     *
     * @param privKey         The recipient's Kyber private key.
     * @param encapsulatedKey The KEM ciphertext bytes previously generated by {@code KEMGenerateSpec}.
     * @return A byte array containing the recovered secret key ({@link SecretKeyWithEncapsulation#getEncoded()}).
     * @throws NoSuchAlgorithmException           if the "KYBER" algorithm is not available.
     * @throws NoSuchProviderException            if the "BCPQC" provider is not registered.
     * @throws InvalidAlgorithmParameterException if specifying an unsupported cipher or spec.
     */
    public byte[] generateDecryptionKey(PrivateKey privKey, byte[] encapsulatedKey)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        KeyGenerator keyGen = KeyGenerator.getInstance("KYBER", "BCPQC");
        // extract an AES secret from the KEM using the private key + encapsulated data
        keyGen.init(new KEMExtractSpec(privKey, encapsulatedKey, "AES"), new SecureRandom());
        SecretKeyWithEncapsulation secret = (SecretKeyWithEncapsulation) keyGen.generateKey();
        return secret.getEncoded();
    }

    /**
     * Encrypts the given plaintext using AES/GCM.
     * <p>
     * This method:
     * <ol>
     *   <li>Generates a random IV (12 bytes) for GCM mode.</li>
     *   <li>Initializes an AES cipher with the provided <code>aesKey</code> in GCM mode.</li>
     *   <li>Returns a byte array where the first 12 bytes are the IV, and the remainder is the ciphertext + GCM tag.</li>
     * </ol>
     *
     * @param plaintext the data to encrypt
     * @param aesKey    a <strong>16-byte, 24-byte, or 32-byte</strong> array for AES-128/192/256
     * @return a byte array combining the random IV (first 12 bytes) and the resulting ciphertext/tag
     * @throws GeneralSecurityException if any crypto operation fails
     */
    public byte[] encrypt(byte[] plaintext, byte[] aesKey) throws GeneralSecurityException {
        // Create a random 12-byte IV (GCM standard)
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[12];
        random.nextBytes(iv);

        // Prepare the key and cipher
        SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);

        // Encrypt
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        byte[] ciphertext = cipher.doFinal(plaintext);

        // Combine IV + ciphertext into one array
        byte[] output = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, output, 0, iv.length);
        System.arraycopy(ciphertext, 0, output, iv.length, ciphertext.length);

        return output;
    }

    /**
     * Decrypts data that was produced by {@link #encrypt(byte[], byte[])},
     * assuming the first 12 bytes of <code>cipherWithIv</code> is the IV, and
     * the remainder is the AES/GCM ciphertext + tag.
     *
     * @param cipherWithIv the combined IV + ciphertext array
     * @param aesKey       a <strong>16-byte, 24-byte, or 32-byte</strong> array for AES-128/192/256
     * @return the original plaintext bytes
     * @throws GeneralSecurityException if any crypto operation fails or authentication fails
     */
    public byte[] decrypt(byte[] cipherWithIv, byte[] aesKey) throws GeneralSecurityException {
        // Separate the IV (first 12 bytes) from the actual ciphertext
        if (cipherWithIv.length < 12) {
            throw new IllegalArgumentException("Ciphertext too short. Missing IV.");
        }
        byte[] iv = Arrays.copyOfRange(cipherWithIv, 0, 12);
        byte[] ciphertext = Arrays.copyOfRange(cipherWithIv, 12, cipherWithIv.length);

        // Prepare the key and cipher
        SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);

        // Decrypt
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        return cipher.doFinal(ciphertext);
    }
}

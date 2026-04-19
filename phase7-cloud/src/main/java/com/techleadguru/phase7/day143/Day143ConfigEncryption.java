package com.techleadguru.phase7.day143;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;

/**
 * Day 143 — Config Server encryption / decryption
 *
 * Config Server exposes /encrypt and /decrypt endpoints.
 * Encrypted values are stored in config files as {cipher}BASE64VALUE.
 * The server decrypts them before serving to clients.
 *
 * Key options:
 *   - Symmetric: encrypt.key=mySecretKey (uses AES-256 via Spring Security Crypto)
 *   - Asymmetric: encrypt.key-store.* (uses RSA keypair from JKS/PKCS12)
 */
public class Day143ConfigEncryption {

    // ─────────────────────────────────────────────────────────────────────────
    // Algorithm registry
    // ─────────────────────────────────────────────────────────────────────────

    public enum EncryptionAlgorithm { AES, RSA }

    public record AlgorithmInfo(
            EncryptionAlgorithm algorithm,
            String springProperty,
            String keyType,
            String pros,
            String cons) {}

    public static List<AlgorithmInfo> algorithmInfos() {
        return List.of(
            new AlgorithmInfo(EncryptionAlgorithm.AES,
                "encrypt.key",
                "Symmetric secret key",
                "Simple setup; fast",
                "Key must be kept secret and distributed securely"),
            new AlgorithmInfo(EncryptionAlgorithm.RSA,
                "encrypt.key-store.location",
                "Public/private keypair in JKS/PKCS12 keystore",
                "Stronger security; can share public key for encryption",
                "More complex setup; slower")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AES cipher simulator (real javax.crypto, simplified key handling)
    // ─────────────────────────────────────────────────────────────────────────

    public static class CipherSimulator {

        private final SecretKey secretKey;

        public CipherSimulator(byte[] rawKey) {
            // raw 16-byte (AES-128) key
            this.secretKey = new SecretKeySpec(rawKey, 0, 16, "AES");
        }

        public static CipherSimulator withGeneratedKey() throws Exception {
            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            keygen.init(128);
            SecretKey key = keygen.generateKey();
            return new CipherSimulator(key.getEncoded());
        }

        public byte[] encrypt(String plaintext) throws Exception {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        public String decrypt(byte[] ciphertext) throws Exception {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(ciphertext),
                    java.nio.charset.StandardCharsets.UTF_8);
        }

        /** Returns Base64-encoded ciphertext. */
        public String encryptToBase64(String plaintext) throws Exception {
            return Base64.getEncoder().encodeToString(encrypt(plaintext));
        }

        /** Decrypts Base64-encoded ciphertext. */
        public String decryptFromBase64(String base64) throws Exception {
            return decrypt(Base64.getDecoder().decode(base64));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Encrypted property format
    // ─────────────────────────────────────────────────────────────────────────

    public record EncryptedValue(String rawPlaintext, String base64Ciphertext) {
        /** Returns the value as it should appear in a config file. */
        public String configFileValue() {
            return "{cipher}" + base64Ciphertext;
        }
    }

    public static boolean isEncryptedValue(String value) {
        return value != null && value.startsWith("{cipher}");
    }

    public static String stripCipherPrefix(String value) {
        return isEncryptedValue(value) ? value.substring(8) : value;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Key management guide
    // ─────────────────────────────────────────────────────────────────────────

    public record KeyManagementTip(int order, String tip) {}

    public static List<KeyManagementTip> keyManagementGuide() {
        return List.of(
            new KeyManagementTip(1, "Never commit plaintext secrets to Git"),
            new KeyManagementTip(2, "Store encrypt.key in an environment variable, not application.properties"),
            new KeyManagementTip(3, "For RSA, protect the private key with a strong keystore password"),
            new KeyManagementTip(4, "Rotate keys periodically; re-encrypt stored {cipher} values"),
            new KeyManagementTip(5, "Consider a dedicated secret store (HashiCorp Vault) for production"),
            new KeyManagementTip(6, "Limit access to Config Server /encrypt and /decrypt endpoints")
        );
    }
}

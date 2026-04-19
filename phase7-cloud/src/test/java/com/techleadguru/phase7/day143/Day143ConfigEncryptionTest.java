package com.techleadguru.phase7.day143;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Day143ConfigEncryptionTest {

    @Test
    void testEncryptDecryptRoundTrip() throws Exception {
        Day143ConfigEncryption.CipherSimulator cipher =
                Day143ConfigEncryption.CipherSimulator.withGeneratedKey();
        String plaintext = "supersecret-db-password";
        String base64    = cipher.encryptToBase64(plaintext);
        String decrypted = cipher.decryptFromBase64(base64);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void testEncryptedValueIsDifferentFromPlaintext() throws Exception {
        Day143ConfigEncryption.CipherSimulator cipher =
                Day143ConfigEncryption.CipherSimulator.withGeneratedKey();
        String plaintext = "my-secret";
        String base64    = cipher.encryptToBase64(plaintext);
        assertNotEquals(plaintext, base64);
    }

    @Test
    void testIsEncryptedValue() {
        assertTrue(Day143ConfigEncryption.isEncryptedValue("{cipher}AABBCC=="));
        assertFalse(Day143ConfigEncryption.isEncryptedValue("plaintext"));
        assertFalse(Day143ConfigEncryption.isEncryptedValue(null));
    }

    @Test
    void testStripCipherPrefix() {
        assertEquals("AABBCC==",
                Day143ConfigEncryption.stripCipherPrefix("{cipher}AABBCC=="));
        assertEquals("plain",
                Day143ConfigEncryption.stripCipherPrefix("plain"));
    }

    @Test
    void testEncryptedValueRecord() throws Exception {
        Day143ConfigEncryption.CipherSimulator cipher =
                Day143ConfigEncryption.CipherSimulator.withGeneratedKey();
        String ciphertext = cipher.encryptToBase64("secret");
        Day143ConfigEncryption.EncryptedValue ev =
                new Day143ConfigEncryption.EncryptedValue("secret", ciphertext);
        assertTrue(ev.configFileValue().startsWith("{cipher}"));
    }

    @Test
    void testAlgorithmInfos() {
        List<Day143ConfigEncryption.AlgorithmInfo> infos =
                Day143ConfigEncryption.algorithmInfos();
        assertEquals(2, infos.size());
    }

    @Test
    void testKeyManagementGuide() {
        List<Day143ConfigEncryption.KeyManagementTip> tips =
                Day143ConfigEncryption.keyManagementGuide();
        assertEquals(6, tips.size());
    }
}

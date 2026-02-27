package com.toelve.doas.soasa


import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoAES {

    /**
     * Decrypt AES-256-CBC
     * @param encryptedBase64 base64(iv + cipherText)
     * @param aesKeyHex 32 hex char dari backend
     */


    fun deriveKey(sessionKeyHex: String, deviceHash: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(
            hexToBytes(sessionKeyHex),
            "HmacSHA256"
        )
        mac.init(keySpec)
        return mac.doFinal(deviceHash.toByteArray(Charsets.UTF_8))
    }

    fun decrypt(
        encryptedBase64: String,
        aesKeyHex: String
    ): String {

        val allBytes = Base64.decode(encryptedBase64, Base64.DEFAULT)

        // 16 byte pertama = IV
        val iv = allBytes.copyOfRange(0, 16)

        // sisanya = cipher text
        val cipherText = allBytes.copyOfRange(16, allBytes.size)

        val keyBytes = hexToBytes(aesKeyHex)

        val secretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            IvParameterSpec(iv)
        )

        val decrypted = cipher.doFinal(cipherText)
        return String(decrypted, Charsets.UTF_8)
    }

    /**
     * OPTIONAL: Encrypt (kalau suatu saat butuh)
     */
    fun encrypt(
        plainText: String,
        aesKeyHex: String
    ): String {

        val keyBytes = hexToBytes(aesKeyHex)
        val iv = ByteArray(16).apply {
            SecureRandom().nextBytes(this)
        }

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(keyBytes, "AES"),
            IvParameterSpec(iv)
        )

        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        return Base64.encodeToString(iv + cipherText, Base64.NO_WRAP)
    }

    /**
     * HEX → BYTE ARRAY
     */
    private fun hexToBytes(hex: String): ByteArray {
        require(hex.length == 64) {
            "AES-256 key harus 64 hex char"
        }

        val result = ByteArray(hex.length / 2)
        for (i in hex.indices step 2) {
            result[i / 2] =
                ((hex[i].digitToInt(16) shl 4)
                        + hex[i + 1].digitToInt(16)).toByte()
        }
        return result
    }
}

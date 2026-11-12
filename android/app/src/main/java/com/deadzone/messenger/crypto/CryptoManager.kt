package com.deadzone.messenger.crypto

import android.content.Context
import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    private const val PREFS_NAME = "CryptoPrefs"
    private const val KEY_PUBLIC = "publicKey"
    private const val KEY_PRIVATE = "privateKey"
    
    private const val RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_PUBLIC) || !prefs.contains(KEY_PRIVATE)) {
            val keyPair = generateKeyPair()
            prefs.edit().apply {
                putString(KEY_PUBLIC, Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP))
                putString(KEY_PRIVATE, Base64.encodeToString(keyPair.private.encoded, Base64.NO_WRAP))
                apply()
            }
        }
    }

    fun getMyPublicKey(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PUBLIC, null)
    }

    private fun getMyPrivateKey(context: Context): PrivateKey? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val privateKeyStr = prefs.getString(KEY_PRIVATE, null) ?: return null
        val privateKeyBytes = Base64.decode(privateKeyStr, Base64.DEFAULT)
        val keySpec = PKCS8EncodedKeySpec(privateKeyBytes)
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec)
    }

    private fun generateKeyPair(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        return keyGen.generateKeyPair()
    }

    fun getPublicKeyFromString(publicKeyStr: String): PublicKey {
        val publicKeyBytes = Base64.decode(publicKeyStr, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(publicKeyBytes)
        return KeyFactory.getInstance("RSA").generatePublic(keySpec)
    }

    fun encrypt(plainText: String, recipientPublicKeyStr: String): String {
        return try {
            val recipientPublicKey = getPublicKeyFromString(recipientPublicKeyStr)

            // 1. Generate AES key
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256)
            val aesKey = keyGen.generateKey()

            // 2. Encrypt AES key with RSA
            val rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION)
            rsaCipher.init(Cipher.ENCRYPT_MODE, recipientPublicKey)
            val encryptedAesKey = rsaCipher.doFinal(aesKey.encoded)
            val encryptedAesKeyBase64 = Base64.encodeToString(encryptedAesKey, Base64.NO_WRAP)

            // 3. Encrypt Text with AES-GCM
            val iv = ByteArray(GCM_IV_LENGTH).apply { SecureRandom().nextBytes(this) }
            val aesCipher = Cipher.getInstance(AES_TRANSFORMATION)
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val encryptedText = aesCipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val encryptedTextBase64 = Base64.encodeToString(encryptedText, Base64.NO_WRAP)

            "$encryptedAesKeyBase64|$ivBase64|$encryptedTextBase64"
        } catch (e: Exception) {
            e.printStackTrace()
            plainText
        }
    }

    fun decrypt(context: Context, payload: String): String {
        return try {
            val parts = payload.split("|")
            if (parts.size != 3) return payload // Not encrypted or malformed

            val myPrivateKey = getMyPrivateKey(context) ?: return payload

            val encryptedAesKeyBase64 = parts[0]
            val ivBase64 = parts[1]
            val encryptedTextBase64 = parts[2]

            // 1. Decrypt AES key with RSA
            val rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION)
            rsaCipher.init(Cipher.DECRYPT_MODE, myPrivateKey)
            val aesKeyBytes = rsaCipher.doFinal(Base64.decode(encryptedAesKeyBase64, Base64.DEFAULT))
            val aesKey: SecretKey = SecretKeySpec(aesKeyBytes, "AES")

            // 2. Decrypt Text with AES-GCM
            val iv = Base64.decode(ivBase64, Base64.DEFAULT)
            val encryptedText = Base64.decode(encryptedTextBase64, Base64.DEFAULT)
            val aesCipher = Cipher.getInstance(AES_TRANSFORMATION)
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            
            val decryptedBytes = aesCipher.doFinal(encryptedText)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            payload
        }
    }

    fun generateAesKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey()
    }

    fun encryptAesKeyForPeer(aesKey: SecretKey, peerPublicKeyBase64: String): String? {
        return try {
            val publicKeyBytes = Base64.decode(peerPublicKeyBase64, Base64.DEFAULT)
            val keySpec = X509EncodedKeySpec(publicKeyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey = keyFactory.generatePublic(keySpec)

            val rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION)
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey)
            val encryptedBytes = rsaCipher.doFinal(aesKey.encoded)
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun decryptAesKeyFromPeer(context: Context, encryptedAesKeyBase64: String): SecretKey? {
        return try {
            val myPrivateKey = getMyPrivateKey(context) ?: return null
            val rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION)
            rsaCipher.init(Cipher.DECRYPT_MODE, myPrivateKey)
            val aesKeyBytes = rsaCipher.doFinal(Base64.decode(encryptedAesKeyBase64, Base64.DEFAULT))
            SecretKeySpec(aesKeyBytes, "AES")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun encryptFile(inputFile: java.io.File, outputFile: java.io.File, aesKey: SecretKey) {
        try {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val iv = ByteArray(GCM_IV_LENGTH).apply { SecureRandom().nextBytes(this) }
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

            java.io.FileOutputStream(outputFile).use { fos ->
                fos.write(iv) // Prepend IV
                javax.crypto.CipherOutputStream(fos, cipher).use { cos ->
                    java.io.FileInputStream(inputFile).use { fis ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (fis.read(buffer).also { read = it } != -1) {
                            cos.write(buffer, 0, read)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    fun decryptFile(inputStream: java.io.InputStream, outputFile: java.io.File, aesKey: SecretKey) {
        try {
            inputStream.use { fis ->
                val iv = ByteArray(GCM_IV_LENGTH)
                fis.read(iv) // Read IV
                
                val cipher = Cipher.getInstance(AES_TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

                javax.crypto.CipherInputStream(fis, cipher).use { cis ->
                    java.io.FileOutputStream(outputFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (cis.read(buffer).also { read = it } != -1) {
                            fos.write(buffer, 0, read)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}

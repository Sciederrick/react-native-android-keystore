package com.rnandroidkeystore

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Base64
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

class RnAndroidKeystoreModule(reactContext: ReactApplicationContext) :
  NativeRnAndroidKeystoreSpec(reactContext) {

  override fun getName() = NAME

  override fun generateKeyPair(
    alias: String,
    algorithm: String,
    keySize: Double,
    hardwareBacked: Boolean,
    userAuthenticationRequired: Boolean,
    promise: Promise
  ) {
    try {
      if (algorithm.uppercase() != "ECDSA") {
        promise.resolve(errorResult("Unsupported algorithm: $algorithm"))
        return
      }

      val keyStore = getKeyStore()
      if (keyStore.containsAlias(alias)) {
        val existingPublicKey = getPublicKeyFromAlias(alias)
        if (existingPublicKey == null) {
          promise.resolve(errorResult("Failed to load existing public key"))
          return
        }

        promise.resolve(successResult().apply {
          putString("publicKey", toPem(existingPublicKey))
          putString("privateKey", alias)
          putDouble("keySize", keySize)
        })
        return
      }

      createEcKeyPair(alias, userAuthenticationRequired, hardwareBacked)

      val publicKey = getPublicKeyFromAlias(alias)
      if (publicKey == null) {
        promise.resolve(errorResult("Failed to retrieve generated public key"))
        return
      }

      promise.resolve(successResult().apply {
        putString("publicKey", toPem(publicKey))
        putString("privateKey", alias)
        putDouble("keySize", keySize)
      })
    } catch (error: Exception) {
      promise.reject("GENERATE_KEY_PAIR_ERROR", error.message, error)
    }
  }

  override fun getPublicKeyPEM(alias: String, promise: Promise) {
    try {
      val publicKey = getPublicKeyFromAlias(alias)
      if (publicKey == null) {
        promise.resolve(errorResult("Key not found for alias: $alias"))
        return
      }

      promise.resolve(successResult().apply {
        putString("publicKeyPEM", toPem(publicKey))
      })
    } catch (error: Exception) {
      promise.reject("GET_PUBLIC_KEY_ERROR", error.message, error)
    }
  }

  override fun sign(alias: String, payload: String, algorithm: String, promise: Promise) {
    try {
      val keyStore = getKeyStore()
      val key = keyStore.getKey(alias, null) as? PrivateKey
      if (key == null) {
        promise.resolve(errorResult("Private key not found for alias: $alias"))
        return
      }

      val signature = Signature.getInstance(algorithm)
      signature.initSign(key)
      signature.update(payload.toByteArray(StandardCharsets.UTF_8))

      val signedBytes = signature.sign()
      val encodedSignature = Base64.encodeToString(signedBytes, Base64.NO_WRAP)

      promise.resolve(successResult().apply {
        putString("signature", encodedSignature)
      })
    } catch (error: Exception) {
      promise.reject("SIGN_ERROR", error.message, error)
    }
  }

  override fun deleteKey(alias: String, promise: Promise) {
    try {
      val keyStore = getKeyStore()
      if (keyStore.containsAlias(alias)) {
        keyStore.deleteEntry(alias)
      }

      promise.resolve(successResult())
    } catch (error: Exception) {
      promise.reject("DELETE_KEY_ERROR", error.message, error)
    }
  }

  override fun keyExists(alias: String, promise: Promise) {
    try {
      val keyStore = getKeyStore()
      val result = Arguments.createMap().apply {
        putBoolean("success", true)
        putBoolean("exists", keyStore.containsAlias(alias))
      }
      promise.resolve(result)
    } catch (error: Exception) {
      val result = Arguments.createMap().apply {
        putBoolean("success", false)
        putBoolean("exists", false)
      }
      promise.resolve(result)
    }
  }

  override fun isHardwareBackedKeystoreAvailable(promise: Promise) {
    val tempAlias = "android_hw_check_${System.currentTimeMillis()}"
    try {
      createEcKeyPair(tempAlias, userAuthenticationRequired = false, hardwareBacked = false)

      val keyStore = getKeyStore()
      val key = keyStore.getKey(tempAlias, null) as? PrivateKey
      if (key == null) {
        promise.resolve(Arguments.createMap().apply { putBoolean("available", false) })
        return
      }

      val keyFactory = KeyFactory.getInstance(key.algorithm, "AndroidKeyStore")
      val keyInfo = keyFactory.getKeySpec(key, KeyInfo::class.java) as KeyInfo
      val isHardwareBacked = keyInfo.isInsideSecureHardware

      promise.resolve(Arguments.createMap().apply {
        putBoolean("available", isHardwareBacked)
      })
    } catch (_: Exception) {
      promise.resolve(Arguments.createMap().apply {
        putBoolean("available", false)
      })
    } finally {
      try {
        val keyStore = getKeyStore()
        if (keyStore.containsAlias(tempAlias)) {
          keyStore.deleteEntry(tempAlias)
        }
      } catch (_: Exception) {
      }
    }
  }

  private fun createEcKeyPair(alias: String, userAuthenticationRequired: Boolean, hardwareBacked: Boolean) {
    try {
      createEcKeyPairInternal(alias, userAuthenticationRequired, hardwareBacked)
    } catch (error: Exception) {
      if (hardwareBacked) {
        createEcKeyPairInternal(alias, userAuthenticationRequired, false)
      } else {
        throw error
      }
    }
  }

  private fun createEcKeyPairInternal(alias: String, userAuthenticationRequired: Boolean, hardwareBacked: Boolean) {
    val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")

    val builder = KeyGenParameterSpec.Builder(
      alias,
      KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
    )
      .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
      .setDigests(KeyProperties.DIGEST_SHA256)
      .setUserAuthenticationRequired(userAuthenticationRequired)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && hardwareBacked) {
      builder.setIsStrongBoxBacked(true)
    }

    keyPairGenerator.initialize(builder.build())
    keyPairGenerator.generateKeyPair()
  }

  private fun getPublicKeyFromAlias(alias: String): PublicKey? {
    val keyStore = getKeyStore()
    val certificate = keyStore.getCertificate(alias) ?: return null
    return certificate.publicKey
  }

  private fun toPem(publicKey: PublicKey): String {
    val base64 = Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    return "-----BEGIN PUBLIC KEY-----\n$base64\n-----END PUBLIC KEY-----"
  }

  private fun getKeyStore(): KeyStore {
    val keyStore = KeyStore.getInstance("AndroidKeyStore")
    keyStore.load(null)
    return keyStore
  }

  private fun successResult() = Arguments.createMap().apply {
    putBoolean("success", true)
  }

  private fun errorResult(message: String) = Arguments.createMap().apply {
    putBoolean("success", false)
    putString("error", message)
  }

  companion object {
    const val NAME = NativeRnAndroidKeystoreSpec.NAME
  }
}

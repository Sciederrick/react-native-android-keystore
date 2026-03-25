/**
 * Android Keystore ECDSA P-256 Key Management
 * Handles generation, retrieval, and export of cryptographic key pairs
 * for request signing purposes.
 */

import { NativeModules, Platform } from 'react-native';

const KEY_ALIAS = 'android_keystore_device_signing';

function getNativeModule() {
  const { RnAndroidKeystore } = NativeModules;
  return RnAndroidKeystore;
}

function normalizePublicKeyPEM(publicKeyPEM: string): string {
  return publicKeyPEM
    .replace('-----BEGIN PUBLIC KEY-----', '')
    .replace('-----END PUBLIC KEY-----', '')
    .replace(/\s+/g, '');
}

/**
 * Initialize or retrieve an existing ECDSA P-256 key pair from Android Keystore
 * Keys are hardware-backed when available. Caching strategy is left to the application layer.
 * Consider using keyPairExists() helper to minimize native calls if needed.
 */
export async function getOrCreateKeyPair(
  alias: string = KEY_ALIAS
): Promise<{ publicKey: string; privateKey: string } | null> {
  if (Platform.OS !== 'android') {
    console.warn('getOrCreateKeyPair: Only Android is supported');
    return null;
  }

  try {
    const AndroidKeystore = getNativeModule();
    if (!AndroidKeystore) {
      throw new Error(
        'AndroidKeystore native module not available. Ensure android-keystore bridge is properly linked.'
      );
    }

    // Create new key pair via native Android Keystore
    const result = await AndroidKeystore.generateKeyPair(
      alias,
      'ECDSA',
      256,
      true,
      false
    );

    if (!result.success) {
      throw new Error(result.error || 'Failed to generate key pair');
    }

    return {
      publicKey: normalizePublicKeyPEM(result.publicKey || ''),
      privateKey: alias,
    };
  } catch (error) {
    console.error('Error in getOrCreateKeyPair:', error);
    throw error;
  }
}

/**
 * Export the public key for backend registration
 * Returns the base64 key body without PEM markers or whitespace
 */
export async function getPublicKeyPEM(
  alias: string = KEY_ALIAS
): Promise<string | null> {
  if (Platform.OS !== 'android') {
    return null;
  }

  try {
    const AndroidKeystore = getNativeModule();
    if (!AndroidKeystore) {
      throw new Error('AndroidKeystore native module not available');
    }

    const result = await AndroidKeystore.getPublicKeyPEM(alias);
    if (!result.success) {
      throw new Error(result.error || 'Failed to export public key');
    }

    return normalizePublicKeyPEM(result.publicKeyPEM || '');
  } catch (error) {
    console.error('Error in getPublicKeyPEM:', error);
    throw error;
  }
}

/**
 * Sign a payload using the private key in Android Keystore
 * Returns base64-encoded signature
 */
export async function signPayload(
  payload: string,
  alias: string = KEY_ALIAS
): Promise<string | null> {
  if (Platform.OS !== 'android') {
    return null;
  }

  try {
    const AndroidKeystore = getNativeModule();
    if (!AndroidKeystore) {
      throw new Error('AndroidKeystore native module not available');
    }

    const result = await AndroidKeystore.sign(
      alias,
      payload,
      'SHA256withECDSA'
    );
    if (!result.success) {
      throw new Error(result.error || 'Failed to sign payload');
    }

    return result.signature || null;
  } catch (error) {
    console.error('Error in signPayload:', error);
    throw error;
  }
}

/**
 * Delete a key pair from Android Keystore
 * Used for key rotation
 */
export async function deleteKeyPair(
  alias: string = KEY_ALIAS
): Promise<boolean> {
  if (Platform.OS !== 'android') {
    return false;
  }

  try {
    const AndroidKeystore = getNativeModule();
    if (!AndroidKeystore) {
      throw new Error('AndroidKeystore native module not available');
    }

    const result = await AndroidKeystore.deleteKey(alias);
    if (!result.success) {
      throw new Error(result.error || 'Failed to delete key');
    }

    return true;
  } catch (error) {
    console.error('Error in deleteKeyPair:', error);
    throw error;
  }
}

/**
 * Check if a key pair exists in Android Keystore
 */
export async function keyPairExists(
  alias: string = KEY_ALIAS
): Promise<boolean> {
  if (Platform.OS !== 'android') {
    return false;
  }

  try {
    const AndroidKeystore = getNativeModule();
    if (!AndroidKeystore) {
      return false;
    }

    const result = await AndroidKeystore.keyExists(alias);
    return result.exists === true;
  } catch (error) {
    console.error('Error in keyPairExists:', error);
    return false;
  }
}

/**
 * Check if device supports hardware-backed keystore
 */
export async function isHardwareBackedKeystoreAvailable(): Promise<boolean> {
  if (Platform.OS !== 'android') {
    return false;
  }

  try {
    const AndroidKeystore = getNativeModule();
    if (!AndroidKeystore) {
      return false;
    }

    const result = await AndroidKeystore.isHardwareBackedKeystoreAvailable();
    return result.available === true;
  } catch (error) {
    console.error('Error checking hardware-backed keystore:', error);
    return false;
  }
}

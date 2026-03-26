# rn-android-keystore

A React Native wrapper for Android Keystore that enables secure cryptographic key pair generation, storage, and payload signing on Android devices.

## Features

- 🔐 Generate ECDSA key pairs stored in Android Keystore
- 📝 Sign payloads with private keys
- 🔑 Export public keys in PEM format
- 🛡️ Hardware-backed keystore support detection
- 🔄 Key management (create, delete, check existence)
- 📱 User authentication integration
- ⚙️ Secure key parameters configuration

## Requirements

- React Native 0.60+
- Android API level 24+

## Installation

```sh
npm install rn-android-keystore
# or
yarn add rn-android-keystore
```

### Android Setup

No additional setup required. The package automatically links to your Android project.

## Usage

### Basic Example

```js
import { RnAndroidKeystore } from 'rn-android-keystore';

// Generate a new key pair
const keyPair = await RnAndroidKeystore.generateKeyPair({
  alias: 'my-signing-key',
  algorithm: 'ECDSA',
  keySize: 256,
  hardwareBacked: true,
  userAuthenticationRequired: false,
});

console.log('Public Key PEM:', keyPair.publicKey);
```

## API Reference

### generateKeyPair(options)

Generates a new ECDSA key pair in the Android Keystore.

**Parameters:**
- `alias` (string, required): Unique identifier for the key pair
- `algorithm` (string, required): Cryptographic algorithm. Currently supports `'ECDSA'`
- `keySize` (number, required): Key size in bits (e.g., 256)
- `hardwareBacked` (boolean, optional): Use hardware-backed keystore if available (default: `true`)
- `userAuthenticationRequired` (boolean, optional): Require user authentication to use the key (default: `false`)

**Returns:** `Promise<KeyPairResult>`

```typescript
interface KeyPairResult {
  success: boolean;
  publicKey: string; // PEM formatted public key
  privateKey: string; // Key alias (private key is not exported)
  keySize: number;
  error?: string;
}
```

**Example:**
```js
try {
  const result = await RnAndroidKeystore.generateKeyPair({
    alias: 'auth-key',
    algorithm: 'ECDSA',
    keySize: 256,
    hardwareBacked: true,
    userAuthenticationRequired: false,
  });

  if (result.success) {
    console.log('Key pair created:', result.publicKey);
  } else {
    console.error('Failed to create key pair:', result.error);
  }
} catch (error) {
  console.error('Error:', error);
}
```

### getPublicKeyPEM(alias)

Retrieves the public key in PEM format for an existing key pair.

**Parameters:**
- `alias` (string, required): Key pair alias

**Returns:** `Promise<PublicKeyResult>`

```typescript
interface PublicKeyResult {
  success: boolean;
  publicKeyPEM?: string;
  error?: string;
}
```

**Example:**
```js
const result = await RnAndroidKeystore.getPublicKeyPEM('my-signing-key');
if (result.success) {
  console.log('Public Key:', result.publicKeyPEM);
}
```

### sign(alias, payload, algorithm)

Signs a payload using the private key associated with the alias.

**Parameters:**
- `alias` (string, required): Key pair alias
- `payload` (string, required): Data to sign (will be encoded as UTF-8)
- `algorithm` (string, required): Signing algorithm (e.g., `'SHA256withECDSA'`)

**Returns:** `Promise<SignResult>`

```typescript
interface SignResult {
  success: boolean;
  signature?: string; // Base64-encoded signature
  error?: string;
}
```

**Example:**
```js
const payload = 'data to sign';
const result = await RnAndroidKeystore.sign(
  'my-signing-key',
  payload,
  'SHA256withECDSA'
);

if (result.success) {
  console.log('Signature:', result.signature);
}
```

### deleteKey(alias)

Deletes a key pair from the Android Keystore.

**Parameters:**
- `alias` (string, required): Key pair alias

**Returns:** `Promise<DeleteResult>`

```typescript
interface DeleteResult {
  success: boolean;
  error?: string;
}
```

**Example:**
```js
const result = await RnAndroidKeystore.deleteKey('my-signing-key');
if (result.success) {
  console.log('Key deleted successfully');
}
```

### keyExists(alias)

Checks if a key pair exists in the Android Keystore.

**Parameters:**
- `alias` (string, required): Key pair alias

**Returns:** `Promise<KeyExistsResult>`

```typescript
interface KeyExistsResult {
  success: boolean;
  exists: boolean;
}
```

**Example:**
```js
const result = await RnAndroidKeystore.keyExists('my-signing-key');
if (result.exists) {
  console.log('Key exists in Keystore');
} else {
  console.log('Key does not exist');
}
```

### isHardwareBackedKeystoreAvailable()

Checks if the device has a hardware-backed keystore available.

**Returns:** `Promise<HardwareCheckResult>`

```typescript
interface HardwareCheckResult {
  available: boolean;
}
```

**Example:**
```js
const result = await RnAndroidKeystore.isHardwareBackedKeystoreAvailable();
if (result.available) {
  console.log('Hardware-backed keystore is available');
}
```

## Complete Example: Authentication Token Signing

```js
import { RnAndroidKeystore } from 'rn-android-keystore';

async function signAuthToken() {
  const keyAlias = 'auth-signing-key';
  
  // Check if key exists
  let keyExistsResult = await RnAndroidKeystore.keyExists(keyAlias);
  
  if (!keyExistsResult.exists) {
    // Create new key pair if it doesn't exist
    const createResult = await RnAndroidKeystore.generateKeyPair({
      alias: keyAlias,
      algorithm: 'ECDSA',
      keySize: 256,
      hardwareBacked: true,
      userAuthenticationRequired: false,
    });

    if (!createResult.success) {
      throw new Error(`Failed to create key: ${createResult.error}`);
    }

    console.log('Public Key:', createResult.publicKey);
  }

  // Sign a payload
  const payload = JSON.stringify({
    userId: '12345',
    timestamp: Date.now(),
  });

  const signResult = await RnAndroidKeystore.sign(
    keyAlias,
    payload,
    'SHA256withECDSA'
  );

  if (!signResult.success) {
    throw new Error(`Failed to sign: ${signResult.error}`);
  }

  return {
    payload,
    signature: signResult.signature,
  };
}

// Usage
signAuthToken()
  .then(result => {
    console.log('Signed token:', result);
  })
  .catch(error => {
    console.error('Error:', error);
  });
```

## Error Handling

All functions return promises that resolve with a result object. Always check the `success` property before using the returned data:

```js
const result = await RnAndroidKeystore.generateKeyPair({...});

if (!result.success) {
  console.error('Error:', result.error);
  return;
}

// Use result data
```

## Platform Support

- **Android**: Full support (API 24+)

## Security Considerations

1. **Private Keys**: Private keys are **never exported** and remain secure in Android Keystore
2. **Hardware Backing**: On devices with Strongbox, keys can be stored in hardware security modules
3. **User Authentication**: Enable `userAuthenticationRequired` to require biometric/PIN authentication before key usage
4. **Key Aliases**: Use meaningful, unique aliases for your keys
5. **Payload Encoding**: Payloads are automatically encoded as UTF-8

## Contributing

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)
- [Code of conduct](CODE_OF_CONDUCT.md)

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)

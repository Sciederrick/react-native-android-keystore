import { TurboModuleRegistry, type TurboModule } from 'react-native';

export interface Spec extends TurboModule {
  generateKeyPair(
    alias: string,
    algorithm: string,
    keySize: number,
    hardwareBacked: boolean,
    userAuthenticationRequired: boolean
  ): Promise<{
    success: boolean;
    error?: string;
    publicKey?: string;
    privateKey?: string;
    keySize?: number;
  }>;
  getPublicKeyPEM(alias: string): Promise<{
    success: boolean;
    error?: string;
    publicKeyPEM?: string;
  }>;
  sign(
    alias: string,
    payload: string,
    algorithm: string
  ): Promise<{
    success: boolean;
    error?: string;
    signature?: string;
  }>;
  deleteKey(alias: string): Promise<{
    success: boolean;
    error?: string;
  }>;
  keyExists(alias: string): Promise<{
    success: boolean;
    exists: boolean;
  }>;
  isHardwareBackedKeystoreAvailable(): Promise<{
    success: boolean;
    available: boolean;
  }>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('RnAndroidKeystore');

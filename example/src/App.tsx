import { useEffect, useState } from 'react';
import { Text, View, StyleSheet } from 'react-native';
import * as k from 'rn-android-keystore';

export default function App() {
  const [hardwareBacked, setHardwareBacked] = useState<boolean | null>(null);
  const [keypair, setKeypair] = useState<{
    publicKey: string;
    privateKey: string;
  } | null>(null);

  useEffect(() => {
    (async () => {
      const isHardwareBacked = await k.isHardwareBackedKeystoreAvailable();
      setHardwareBacked(isHardwareBacked);

      const keypairResponse = await k.getOrCreateKeyPair();
      setKeypair(keypairResponse);
    })();
  }, []);

  return (
    <View style={styles.container}>
      <Text>
        Hardware-backed Keystore Available: {hardwareBacked ? 'Yes' : 'No'}
      </Text>
      <Text>Public Key: {keypair?.publicKey}</Text>
      <Text>Private Key: {keypair?.privateKey}</Text>
      <View style={styles.methodsContainer}>
        <Text>Android Keystore Methods</Text>
        <View style={styles.methodsList}>
          {Object.keys(k).map((v) => (
            <Text key={v}>* {v}</Text>
          ))}
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  methodsContainer: {
    marginTop: 16,
  },
  methodsList: {
    marginTop: 8,
  },
});

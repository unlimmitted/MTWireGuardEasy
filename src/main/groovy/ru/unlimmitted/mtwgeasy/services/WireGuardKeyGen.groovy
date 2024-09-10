package ru.unlimmitted.mtwgeasy.services

import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.KeyGenerationParameters
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import ru.unlimmitted.mtwgeasy.dto.KeyPair

import java.security.SecureRandom

class WireGuardKeyGen {
	static KeyPair keyPair() {
		KeyPair keys = new KeyPair()
		try {
			Ed25519KeyPairGenerator keyPairGenerator = new Ed25519KeyPairGenerator()
			keyPairGenerator.init(new KeyGenerationParameters(new SecureRandom(), 256))
			AsymmetricCipherKeyPair keyPair = keyPairGenerator.generateKeyPair()

			Ed25519PrivateKeyParameters privateKeyParams = (Ed25519PrivateKeyParameters) keyPair.getPrivate()
			byte[] privateKeyBytes = privateKeyParams.getEncoded()
			String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKeyBytes)

			Ed25519PublicKeyParameters publicKeyParams = (Ed25519PublicKeyParameters) keyPair.getPublic()
			byte[] publicKeyBytes = publicKeyParams.getEncoded()
			String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKeyBytes)

			keys.setPublicKey(publicKeyBase64)
			keys.setPrivateKey(privateKeyBase64)

		} catch (Exception e) {
			e.printStackTrace()
		}
		return keys
	}
}

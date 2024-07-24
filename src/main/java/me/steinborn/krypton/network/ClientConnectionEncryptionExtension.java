package me.steinborn.krypton.network;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;

public interface ClientConnectionEncryptionExtension {
    void legacy_krypton$setupEncryption(SecretKey key) throws GeneralSecurityException;
}

package me.steinborn.krypton.mixin.network.pipeline.encryption;

import java.security.PrivateKey;
import me.steinborn.krypton.network.ClientConnectionEncryptionExtension;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;

@Mixin(ServerLoginNetworkHandler.class)
public class ServerLoginNetworkHandlerMixin {

    @Shadow @Final
    public ClientConnection connection;

    @Redirect(method = "onKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/c2s/login/LoginKeyC2SPacket;decryptSecretKey(Ljava/security/PrivateKey;)Ljavax/crypto/SecretKey;"))
    private SecretKey onKey$initializeVelocityCipher(LoginKeyC2SPacket instance, PrivateKey privateKey) throws GeneralSecurityException {
        // Hijack this portion of the cipher initialization and set up our own encryption handler.
        ((ClientConnectionEncryptionExtension) this.connection).legacy_krypton$setupEncryption((SecretKey) privateKey);

        // Turn the operation into a no-op.
        return null;
    }

    @Redirect(method = "onKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;setupEncryption(Ljavax/crypto/SecretKey;)V"))
    public void onKey$ignoreMinecraftEncryptionPipelineInjection(ClientConnection instance, SecretKey secretKey) {
        // Turn the operation into a no-op.
    }

}

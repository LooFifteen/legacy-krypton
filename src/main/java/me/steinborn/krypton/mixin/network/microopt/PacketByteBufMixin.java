package me.steinborn.krypton.mixin.network.microopt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.EncoderException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import me.steinborn.krypton.network.util.VarIntUtil;
import net.minecraft.util.PacketByteBuf;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PacketByteBuf.class)
public abstract class PacketByteBufMixin extends ByteBuf {

    @Shadow @Final private ByteBuf parent;

    /**
     * @author Andrew Steinborn
     * @reason optimized version
     */
    @Overwrite
    public static int getVarIntSizeBytes(int v) {
        return VarIntUtil.getVarIntLength(v);
    }

    /**
     * @author Andrew Steinborn
     * @reason optimized version
     */
    @Overwrite
    public void writeVarInt(int value) {
        // Peel the one and two byte count cases explicitly as they are the most common VarInt sizes
        // that the server will send, to improve inlining.
        if ((value & (0xFFFFFFFF << 7)) == 0) {
            this.writeByte(value);
        } else if ((value & (0xFFFFFFFF << 14)) == 0) {
            int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
            this.writeShort(w);
        } else {
            writeVarIntFull(this, value);
        }
    }

    @Unique
    private static void writeVarIntFull(ByteBuf buf, int value) {
        // See https://steinborn.me/posts/performance/how-fast-can-you-write-a-varint/
        if ((value & (0xFFFFFFFF << 7)) == 0) {
            buf.writeByte(value);
        } else if ((value & (0xFFFFFFFF << 14)) == 0) {
            int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
            buf.writeShort(w);
        } else if ((value & (0xFFFFFFFF << 21)) == 0) {
            int w = (value & 0x7F | 0x80) << 16 | ((value >>> 7) & 0x7F | 0x80) << 8 | (value >>> 14);
            buf.writeMedium(w);
        } else if ((value & (0xFFFFFFFF << 28)) == 0) {
            int w = (value & 0x7F | 0x80) << 24 | (((value >>> 7) & 0x7F | 0x80) << 16)
                    | ((value >>> 14) & 0x7F | 0x80) << 8 | (value >>> 21);
            buf.writeInt(w);
        } else {
            int w = (value & 0x7F | 0x80) << 24 | ((value >>> 7) & 0x7F | 0x80) << 16
                    | ((value >>> 14) & 0x7F | 0x80) << 8 | ((value >>> 21) & 0x7F | 0x80);
            buf.writeInt(w);
            buf.writeByte(value >>> 28);
        }
    }

    /**
     * @author Andrew Steinborn
     * @reason optimized version
     */
    @Overwrite
    public PacketByteBuf writeString(@NotNull String string) {
        // Mojang _almost_ gets it right, but stumbles at the finish line...
        if (string.length() > 32767) {
            throw new EncoderException("String too big (was " + string.length() + " characters, max 32767)");
        }
        int utf8Bytes = ByteBufUtil.utf8Bytes(string);
        int maxBytesPermitted = ByteBufUtil.utf8MaxBytes(32767);
        if (utf8Bytes > maxBytesPermitted) {
            throw new EncoderException("String too big (was " + utf8Bytes + " bytes encoded, max " + maxBytesPermitted + ")");
        } else {
            this.writeVarInt(utf8Bytes);
            this.writeCharSequence(string, StandardCharsets.UTF_8);
        }

        return (PacketByteBuf) (Object) this;
    }

    @Override
    public int writeCharSequence(CharSequence sequence, Charset charset) {
        return this.parent.writeCharSequence(sequence, charset);
    }

}

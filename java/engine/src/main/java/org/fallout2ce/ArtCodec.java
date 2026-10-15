package org.fallout2ce;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Flat codec for {@link Art.ArtData}, mirroring the decoded C buffer layout
 * (little-endian host order): 88-byte header (with 2 alignment pad bytes
 * before the data offsets), then per-rotation frame groups
 * ({@code [12-byte header][pixels][pad-to-4]} each).
 */
public final class ArtCodec {

    private ArtCodec() {
    }

    /** Serializes decoded art to the C flat layout. */
    public static byte[] serialize(Art.ArtData art) {
        // Measure first (mirrors the destOffset walk in artRead).
        int size = Art.STRUCT_SIZE;
        for (int r = 0; r < Rotation.COUNT; r++) {
            if (r == 0 || art.rotationBase[r] != art.rotationBase[r - 1]) {
                for (int f = 0; f < art.frameCount; f++) {
                    Art.Frame frame = art.frames.get(art.rotationBase[r] + f);
                    size += Art.FRAME_STRUCT_SIZE + frame.size + Art.paddingForSize(frame.size);
                }
            }
        }

        ByteBuffer buf = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(art.version);
        buf.putShort((short) art.framesPerSecond);
        buf.putShort((short) art.actionFrame);
        buf.putShort((short) art.frameCount);
        for (int i = 0; i < Rotation.COUNT; i++) {
            buf.putShort((short) art.xOffsets[i]);
        }
        for (int i = 0; i < Rotation.COUNT; i++) {
            buf.putShort((short) art.yOffsets[i]);
        }
        buf.putShort((short) 0); // alignment pad before dataOffsets
        for (int i = 0; i < Rotation.COUNT; i++) {
            buf.putInt(art.dataOffsets[i]);
        }
        for (int i = 0; i < Rotation.COUNT; i++) {
            buf.putInt(art.padding[i]);
        }
        buf.putInt(art.dataSize);

        for (int r = 0; r < Rotation.COUNT; r++) {
            if (r == 0 || art.rotationBase[r] != art.rotationBase[r - 1]) {
                for (int f = 0; f < art.frameCount; f++) {
                    Art.Frame frame = art.frames.get(art.rotationBase[r] + f);
                    buf.putShort((short) frame.width);
                    buf.putShort((short) frame.height);
                    buf.putInt(frame.size);
                    buf.putShort((short) frame.x);
                    buf.putShort((short) frame.y);
                    buf.put(frame.pixels);
                    for (int p = 0; p < Art.paddingForSize(frame.size); p++) {
                        buf.put((byte) 0);
                    }
                }
            }
        }

        return buf.array();
    }

    /** Deserializes a flat buffer (inverse of {@link #serialize}). */
    public static Art.ArtData deserialize(byte[] blob) {
        ByteBuffer buf = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN);
        Art.ArtData art = new Art.ArtData();
        art.version = buf.getInt();
        art.framesPerSecond = buf.getShort();
        art.actionFrame = buf.getShort();
        art.frameCount = buf.getShort();
        for (int i = 0; i < Rotation.COUNT; i++) {
            art.xOffsets[i] = buf.getShort();
        }
        for (int i = 0; i < Rotation.COUNT; i++) {
            art.yOffsets[i] = buf.getShort();
        }
        buf.getShort(); // alignment pad
        for (int i = 0; i < Rotation.COUNT; i++) {
            art.dataOffsets[i] = buf.getInt();
        }
        for (int i = 0; i < Rotation.COUNT; i++) {
            art.padding[i] = buf.getInt();
        }
        art.dataSize = buf.getInt();

        // Groups follow data-offset equality (like artRead's walk).
        for (int r = 0; r < Rotation.COUNT; r++) {
            if (r == 0 || art.dataOffsets[r] != art.dataOffsets[r - 1]) {
                art.rotationBase[r] = art.frames.size();
                for (int f = 0; f < art.frameCount; f++) {
                    Art.Frame frame = new Art.Frame();
                    frame.width = buf.getShort();
                    frame.height = buf.getShort();
                    frame.size = buf.getInt();
                    frame.x = buf.getShort();
                    frame.y = buf.getShort();
                    frame.pixels = new byte[frame.size];
                    buf.get(frame.pixels);
                    buf.position(buf.position() + Art.paddingForSize(frame.size));
                    art.frames.add(frame);
                }
            } else {
                art.rotationBase[r] = art.rotationBase[r - 1];
            }
        }
        return art;
    }
}

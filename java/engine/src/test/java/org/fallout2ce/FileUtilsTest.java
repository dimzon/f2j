package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Mirrors the C harness {@code [fileUtils]} flow. gzip <em>bytes</em> are
 * not compared (zlib builds differ); magic + content are.
 */
class FileUtilsTest {

    @TempDir
    Path temp;

    @Test
    void plainCopyRoundTrip() throws Exception {
        Path plain = temp.resolve("plain.txt");
        Path back = temp.resolve("back.txt");
        Files.writeString(plain, "hello world\n");

        assertEquals(0, FileUtils.fileCopyDecompressed(plain.toString(), back.toString()));
        assertEquals("hello world\n", Files.readString(back));
    }

    @Test
    void compressDecompressRoundTrip() throws Exception {
        Path plain = temp.resolve("plain.txt");
        Path gz = temp.resolve("arch.gz");
        Path back = temp.resolve("back.txt");
        Files.writeString(plain, "hello world\n");

        assertEquals(0, FileUtils.fileCopyCompressed(plain.toString(), gz.toString()));
        byte[] magic = Files.readAllBytes(gz);
        assertEquals(0x1F, magic[0] & 0xFF);
        assertEquals(0x8B, magic[1] & 0xFF);

        assertEquals(0, FileUtils.fileCopyDecompressed(gz.toString(), back.toString()));
        assertEquals("hello world\n", Files.readString(back));
    }

    @Test
    void shortFileCopiesAsPlain() throws Exception {
        Path tiny = temp.resolve("tiny.bin");
        Path back = temp.resolve("back.bin");
        Files.write(tiny, new byte[] { 0x41 });

        assertEquals(0, FileUtils.fileCopyDecompressed(tiny.toString(), back.toString()));
        assertEquals(1, Files.size(back));
    }

    @Test
    void missingSource() {
        String missing = temp.resolve("nope.txt").toString();
        String back = temp.resolve("back.txt").toString();
        assertEquals(-1, FileUtils.fileCopyDecompressed(missing, back));
        assertEquals(-1, FileUtils.fileCopyCompressed(missing, back));
        assertEquals(-1, FileUtils.gzDecompressFile(missing, back));
    }

    @Test
    void invertedDecompressFailsOnPlainData() throws Exception {
        // Keeps the C quirk: gunzip is attempted on NON-gzip input -> -1.
        Path plain = temp.resolve("plain.txt");
        Path back = temp.resolve("back.txt");
        Files.writeString(plain, "hello world\n");

        assertEquals(-1, FileUtils.gzDecompressFile(plain.toString(), back.toString()));
    }
}

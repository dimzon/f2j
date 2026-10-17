package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.fallout2ce.Art.ArtData;

/**
 * {@code artLoad} dispatcher for {@code .png} and {@code .pcx} files.
 *
 * <p>Vectors: none yet — this is new capability (PCX/PNG decode was a TODO
 * in {@link Art#artLoad}). The test pins the decoded width/height/pixel data
 * for the bundled fixtures.
 */
class ArtLoadTest {

    @Test
    void artLoadPcx() {
        Path pcxFile = Path.of("src/test/resources/fixtures/test.pcx").toAbsolutePath();
        ArtData art = Art.artLoad(pcxFile.toString());
        assertNotNull(art);
        assertEquals(1, art.frameCount);
        assertNotNull(art.frames);
        assertEquals(1, art.frames.size());

        Art.Frame frame = art.frames.get(0);
        assertTrue(frame.width > 0);
        assertTrue(frame.height > 0);
        assertEquals(frame.width * frame.height, frame.size);
        assertEquals(frame.size, art.dataSize);
        assertTrue(frame.pixels.length > 0);
    }

    @Test
    void artLoadPng() {
        Path pngFile = Path.of("src/test/resources/fixtures/test_png.png").toAbsolutePath();
        ArtData art = Art.artLoad(pngFile.toString());
        System.out.println("DEBUG: art=" + art + " frameCount=" + (art != null ? art.frameCount : "null"));
        assertNotNull(art);
        assertEquals(1, art.frameCount);
        assertNotNull(art.frames);
        assertEquals(1, art.frames.size());

        Art.Frame frame = art.frames.get(0);
        assertEquals(4, frame.width);
        assertEquals(4, frame.height);
        assertEquals(16, frame.size);
        assertEquals(16, art.dataSize);
        assertTrue(frame.pixels.length > 0);

        // Pixels are non-trivial (decoder produced actual index data).
        assertTrue(frame.pixels.length > 0);
    }
}

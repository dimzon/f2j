package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.fallout2ce.Art.ArtData;

/**
 * {@code artLoad} dispatcher for {@code .pcx} files (previously returned
 * {@code null}, now decoded via {@link Pcx#pcxRead}).
 *
 * <p>Vectors: none yet — this is a new capability (PCX decode was a TODO in
 * {@link Art#artLoad}). The test pins the decoded width/height/pixel data
 * for the bundled {@code test.pcx} fixture.
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
}

package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.fallout2ce.ProtoInstance.UseItemResultCode;

/**
 * {@code [gametypes]} proto-instance vectors from
 * {@code docker/refharness/golden/v35.txt}.
 */
class ProtoInstanceTest {

    @Test
    void useItemResultCode() {
        assertEquals(-1, UseItemResultCode.USE_ITEM_RESULT_ERROR.id());
        assertEquals(0, UseItemResultCode.USE_ITEM_RESULT_OK.id());
        assertEquals(1, UseItemResultCode.USE_ITEM_RESULT_REMOVE.id());
        assertEquals(2, UseItemResultCode.USE_ITEM_RESULT_DROP.id());
        assertEquals(UseItemResultCode.USE_ITEM_RESULT_DROP, ProtoInstance.ofUseItemResultCode(2));
        assertNull(ProtoInstance.ofUseItemResultCode(3));
    }
}

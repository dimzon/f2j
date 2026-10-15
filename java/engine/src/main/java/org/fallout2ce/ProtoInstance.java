package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Object-use result codes, mirroring the pure parts of
 * {@code src/proto_instance.h}. The scripting-backed verbs
 * ({@code proto_instance.cc}) come with the game phase.
 */
public final class ProtoInstance {

    private ProtoInstance() {
    }

    public enum UseItemResultCode {
        USE_ITEM_RESULT_ERROR(-1),
        USE_ITEM_RESULT_OK(0),
        USE_ITEM_RESULT_REMOVE(1),
        USE_ITEM_RESULT_DROP(2);

        private final int id;

        UseItemResultCode(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, UseItemResultCode> USEITEM_BY_ID = new HashMap<>();

    static {
        for (UseItemResultCode e : UseItemResultCode.values()) {
            USEITEM_BY_ID.put(e.id, e);
        }
    }

    public static UseItemResultCode ofUseItemResultCode(int id) {
        return USEITEM_BY_ID.get(id);
    }
}

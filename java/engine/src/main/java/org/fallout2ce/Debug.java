package org.fallout2ce;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Port of {@code debugPrint} from {@code src/debug.cc} (output path only).
 * C printf-style call sites stay verbatim; formatting happens up front.
 */
public final class Debug {

    private static final Logger LOG = LoggerFactory.getLogger("fallout");

    private Debug() {
    }

    /** Port of {@code debugPrint}. */
    public static void print(String format, Object... args) {
        LOG.info(String.format(format, args));
    }
}

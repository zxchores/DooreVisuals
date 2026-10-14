package dev.doorevisuals.core;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For failures that are safe to ignore but should not vanish: writes one warning per distinct problem, so a
 * broken file or a missing library shows up in the log without flooding it from a render loop.
 */
public final class Quiet {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals");
    private static final Set<String> SEEN = ConcurrentHashMap.newKeySet();
    private static final int LIMIT = 200;

    private Quiet() {
    }

    public static void log(String where, Throwable t) {
        if (t == null || SEEN.size() >= LIMIT) {
            return;
        }
        String key = where + "|" + t.getClass().getName() + "|" + t.getMessage();
        if (SEEN.add(key)) {
            LOG.warn("{}: ignored {}: {}", where, t.getClass().getSimpleName(), t.getMessage());
        }
    }
}

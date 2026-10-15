package org.fallout2ce;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.fallout2ce.spi.PlatformPlugin;

/**
 * Platform backend loader.
 *
 * <p>Backends that need third-party native libraries (SDL/OpenGL display,
 * audio, input) are separate Maven modules discovered here via
 * {@link ServiceLoader}. Out of the box only managed (JRE-only) code
 * resolves: the engine core has built-in managed defaults, and the
 * {@code f2j-platform-headless} module provides the explicit
 * {@code headless} provider.
 *
 * <p>Selection: {@code -Df2j.platform=&lt;name&gt;} wins; otherwise the
 * working managed provider ({@code safe}) wins if present; otherwise
 * {@code headless} wins if present; otherwise a single provider wins;
 * otherwise (or on an unknown name) loading fails fast. With no providers
 * at all the built-in managed defaults stay in place.
 */
public final class Platform {

    /** System property selecting the provider by name. */
    public static final String PROPERTY = "f2j.platform";

    /** Provider name that always wins unless explicitly overridden. */
    public static final String SAFE = "safe";

    /** Silent managed provider (null backends), second after {@link #SAFE}. */
    public static final String HEADLESS = "headless";

    private Platform() {
    }

    private static PlatformPlugin current;

    /** Currently installed provider ({@code null} = built-in managed defaults). */
    public static PlatformPlugin current() {
        return current;
    }

    /**
     * Discover providers via {@link ServiceLoader}, select one and install
     * its backends into the engine seams. Returns the installed provider, or
     * {@code null} when none was found (built-ins stay).
     */
    public static PlatformPlugin load() {
        List<PlatformPlugin> providers = new ArrayList<>();
        for (PlatformPlugin provider : ServiceLoader.load(PlatformPlugin.class)) {
            providers.add(provider);
        }
        PlatformPlugin selected = select(providers, System.getProperty(PROPERTY));
        if (selected != null) {
            install(selected);
        }
        return selected;
    }

    /**
     * Selection rule (pure, tested directly): explicit name wins (unknown
     * name fails fast); otherwise {@code safe} wins if present; otherwise
     * {@code headless} wins if present; otherwise a single provider wins;
     * empty resolves to {@code null}.
     */
    static PlatformPlugin select(List<PlatformPlugin> providers, String wanted) {
        if (wanted != null) {
            for (PlatformPlugin provider : providers) {
                if (wanted.equals(provider.name())) {
                    return provider;
                }
            }
            throw new IllegalStateException(
                    "Unknown " + PROPERTY + "=" + wanted + ", available: " + names(providers));
        }
        for (PlatformPlugin provider : providers) {
            if (SAFE.equals(provider.name())) {
                return provider;
            }
        }
        for (PlatformPlugin provider : providers) {
            if (HEADLESS.equals(provider.name())) {
                return provider;
            }
        }
        if (providers.size() == 1) {
            return providers.get(0);
        }
        if (providers.isEmpty()) {
            return null;
        }
        throw new IllegalStateException(
                "Multiple platform providers, select one with -D" + PROPERTY + "=<name>: "
                        + names(providers));
    }

    private static List<String> names(List<PlatformPlugin> providers) {
        List<String> names = new ArrayList<>();
        for (PlatformPlugin provider : providers) {
            names.add(provider.name());
        }
        return names;
    }

    /** Install a provider's backends into the engine seams. */
    public static void install(PlatformPlugin plugin) {
        Color.video = plugin.video();
        Ticks.clock = plugin.clock();
        Delay.sleeper = plugin.sleeper();
        FpsLimiter.sleeper = plugin.sleeper();
        ColorCycle.tickers = plugin.tickers();
        current = plugin;
    }

    /** Test-only reset (built-in managed defaults back, no provider). */
    static void resetForTests() {
        Color.resetForTests();
        Ticks.resetForTests();
        Delay.resetForTests();
        FpsLimiter.resetForTests();
        ColorCycle.resetForTests();
        Palette.resetForTests();
        current = null;
    }
}

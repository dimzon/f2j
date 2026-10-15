package org.fallout2ce.spi;

/**
 * Background-ticker registry (C {@code tickersAdd}/{@code tickersRemove} in
 * {@code input.cc}). Pumping stays with the game loop; providers only track
 * registrations.
 */
public interface TickerRegistry {

    void add(Runnable ticker);

    void remove(Runnable ticker);
}

package org.brahypno.sophisticated_smeltery.furnace;

/** Numeric dimensions only. There are no virtual blocks or simulated world. */
public record FurnaceDimensions(int x, int y, int z, int materials, int capacity) {
    public static final FurnaceDimensions EMPTY = new FurnaceDimensions(0, 0, 0, 0, 0);
    public int volume() { return x * y * z; }

    @FunctionalInterface public interface Capacity { int at(int x, int y, int z); }

    public static FurnaceDimensions largest(int available, int innerLimit, int heightLimit,
                                             boolean frame, boolean floor, boolean ceiling, Capacity capacity) {
        FurnaceDimensions best = EMPTY;
        int caps = (floor ? 1 : 0) + (ceiling ? 1 : 0);
        for (int x = 1; x <= innerLimit; x++) {
            for (int z = x; z <= innerLimit; z++) {
                int capBlocks = caps * (frame ? (x + 2) * (z + 2) : x * z);
                int wallBlocks = 2 * (x + z + (frame ? 2 : 0));
                int y = Math.min(heightLimit, (available - capBlocks) / wallBlocks);
                if (available < capBlocks + wallBlocks || y < 1) continue;
                int fluidCapacity = capacity.at(x, y, z);
                int used = capBlocks + wallBlocks * y;
                if (fluidCapacity > best.capacity || fluidCapacity == best.capacity && x * y * z > best.volume()) {
                    best = new FurnaceDimensions(x, y, z, used, fluidCapacity);
                }
            }
        }
        return best;
    }
}

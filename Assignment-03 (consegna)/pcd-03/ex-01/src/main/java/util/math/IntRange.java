package util.math;

import java.util.Objects;

/**
 * Model a range of integers.
 */
public class IntRange {
    /** The first end of this range inclusive. */
    public final int from;
    /** The second end of this range exclusives. */
    public final int to;

    public IntRange(int fromInclusive, int toExclusive){
        this.from = fromInclusive;
        this.to = toExclusive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntRange intRange = (IntRange) o;
        return from == intRange.from && to == intRange.to;
    }
    @Override
    public int hashCode() { return Objects.hash(from, to); }
    @Override
    public String toString() { return "[" + from + "," + to + "["; }
}
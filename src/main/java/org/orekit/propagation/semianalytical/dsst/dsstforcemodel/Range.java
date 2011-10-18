package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

/**
 * The Range class define an interval used to sum recurrences. The sum will be done between the
 * minimum and the maximal value
 */
public class Range {

    /**
     * Minimum value
     */
    private final int min;

    /**
     * Maximum value
     */
    private final int max;

    /**
     * Range constructor
     * 
     * @param min
     *            min
     * @param max
     *            max
     */
    public Range(final int min,
                 final int max) {
        this.min = min;
        this.max = max;
    }

    /** Get the minimum */
    public int getMin() {
        return min;
    }

    /** Get the maximal */
    public int getMax() {
        return max;
    }

    public int getLength() {
        return max - min;
    }

}

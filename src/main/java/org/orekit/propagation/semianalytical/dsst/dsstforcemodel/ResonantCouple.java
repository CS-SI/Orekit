package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

/**
 * Resonant couple used in the expression of the Tesseral resonant term for the
 * {@link DSSTCentralBody}
 * 
 * @author Romain Di Costanzo
 */
public class ResonantCouple implements Comparable<ResonantCouple> {

    /**
     * n value
     */
    private final int n;

    /**
     * m value
     */
    private final int m;

    /**
     * Default constructor
     * 
     * @param n
     *            n-value
     * @param m
     *            m-value
     */
    public ResonantCouple(final int n,
                          final int m) {
        this.m = m;
        this.n = n;
    }

    /**
     * Get the m-value
     * 
     * @return m-value
     */
    public final int getM() {
        return m;
    }

    /**
     * Get the n-value
     * 
     * @return n-value
     */
    public final int getN() {
        return n;
    }

    /**
     * {@inheritDoc}
     */
    public final String toString() {
        return new String("n : " + n + " m : " + m);
    }

    /**
     * {@inheritDoc} Compare a resonant couple to another one. Comparison is done on the order.
     */
    public final int compareTo(final ResonantCouple couple) {
        int result = 1;
        if (n == couple.n) {
            if (m < couple.m) {
                result = -1;
            } else if (m == couple.m) {
                result = 0;
            }
        } else if (n < couple.n) {
            result = -1;
        }
        return result;
    }
}

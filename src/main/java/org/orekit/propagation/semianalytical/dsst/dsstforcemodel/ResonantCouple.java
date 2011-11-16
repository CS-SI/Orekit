package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

/**
 * Resonant couple used in the expression of the Tesseral resonant term for the
 * {@link DSSTCentralBody}
 * 
 * @author Romain Di Costanzo
 */
public class ResonantCouple {

    /**
     * j value
     */
    private final int j;

    /**
     * m value
     */
    private final int m;

    /**
     * Default constructor
     * 
     * @param j
     *            j-value
     * @param m
     *            m-value
     */
    public ResonantCouple(final int j,
                          final int m) {
        this.m = m;
        this.j = j;
    }

    /**
     * Get the m-value
     * 
     * @return m-value
     */
    public int getM() {
        return m;
    }

    /**
     * Get the j-value
     * 
     * @return j-value
     */
    public int getJ() {
        return j;
    }

    public String toString() {
        return new String("j : " + j + " m : " + m);
    }

}

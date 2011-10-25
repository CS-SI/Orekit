package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

public class ResonantCouple {

    private final int m;

    private final int j;

    public ResonantCouple(final int m,
                          final int j) {
        this.m = m;
        this.j = j;
    }

    public int getM() {
        return m;
    }

    public int getJ() {
        return j;
    }

}

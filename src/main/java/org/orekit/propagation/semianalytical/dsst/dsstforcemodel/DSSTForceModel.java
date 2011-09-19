package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;

/**
 * @author rdicosta
 */
public interface DSSTForceModel {

    /**
     * @param date
     * @param currentState
     * @return
     */
    public abstract double[] getMeanElementRate(final AbsoluteDate date,
                                                final double[] currentState);

    /**
     * @param date
     * @param currentState
     * @return
     */
    public abstract double[] getShortPeriodicVariations(final AbsoluteDate date,
                                                        final double[] currentState);

    public abstract void init(final Orbit initialState,
                              final AbsoluteDate referenceDate);

}

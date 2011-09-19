package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;

public class DSSTAtmosphericDrag implements DSSTForceModel {

    @Override
    public double[] getMeanElementRate(final AbsoluteDate date,
                                       final double[] currentState) {

        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double[] getShortPeriodicVariations(final AbsoluteDate date,
                                               final double[] currentState) {

        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void init(Orbit initialState,
                     AbsoluteDate referenceDate) {
        // TODO Auto-generated method stub

    }

}

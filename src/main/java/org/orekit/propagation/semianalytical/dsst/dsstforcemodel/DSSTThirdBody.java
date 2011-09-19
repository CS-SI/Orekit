package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;

public class DSSTThirdBody implements DSSTForceModel {

    @Override
    public double[] getMeanElementRate(AbsoluteDate date,
                                       double[] currentState) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double[] getShortPeriodicVariations(AbsoluteDate date,
                                               double[] currentState) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void init(Orbit initialState,
                     AbsoluteDate referenceDate) {
        // TODO Auto-generated method stub

    }

}

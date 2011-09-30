package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

public class DSSTThirdBody implements DSSTForceModel {

    @Override
    public double[] getMeanElementRate(SpacecraftState currentState) {
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
    public void init(SpacecraftState state) {
        // TODO Auto-generated method stub

    }

}

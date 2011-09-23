package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

public class DSSTSolarRadiationPressure implements DSSTForceModel {

    /** {@inheritDoc} */
    public double[] getMeanElementRate(final SpacecraftState state) throws OrekitException {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public double[] getShortPeriodicVariations(final AbsoluteDate date, final double[] stateVector)
        throws OrekitException {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public void init(final SpacecraftState state) throws OrekitException {
        // TODO Auto-generated method stub
        
    }

}

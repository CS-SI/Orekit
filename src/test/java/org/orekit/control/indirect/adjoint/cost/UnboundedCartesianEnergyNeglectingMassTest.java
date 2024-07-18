package org.orekit.control.indirect.adjoint.cost;

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UnboundedCartesianEnergyNeglectingMassTest {

    @Test
    void testGetMassFlowRateFactor() {
        // GIVEN
        final UnboundedCartesianEnergyNeglectingMass energyNeglectingMass = new UnboundedCartesianEnergyNeglectingMass();
        // WHEN
        final double actualFlowRate = energyNeglectingMass.getMassFlowRateFactor();
        // THEN
        Assertions.assertEquals(0., actualFlowRate);
    }

    @Test
    void testGetAdjointDimension() {
        // GIVEN
        final UnboundedCartesianEnergyNeglectingMass energyNeglectingMass = new UnboundedCartesianEnergyNeglectingMass();
        // WHEN
        final int actualDimension = energyNeglectingMass.getAdjointDimension();
        // THEN
        Assertions.assertEquals(6, actualDimension);
    }

    @Test
    void testGetThrustVector() {
        // GIVEN
        final UnboundedCartesianEnergyNeglectingMass energyNeglectingMass = new UnboundedCartesianEnergyNeglectingMass();
        final Binary64[] adjoint = MathArrays.buildArray(Binary64Field.getInstance(), 6);
        adjoint[3] = Binary64.ONE;
        // WHEN
        final FieldVector3D<Binary64> fieldThrustVector = energyNeglectingMass.getThrustVector(adjoint, Binary64.ONE);
        // THEN
        final Vector3D thrustVector = energyNeglectingMass.getThrustVector(new double[] { 0., 0., 0., 1., 0., 0.}, 1.);
        Assertions.assertEquals(thrustVector, fieldThrustVector.toVector3D());
    }

}

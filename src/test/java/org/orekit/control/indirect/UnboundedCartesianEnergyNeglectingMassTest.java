package org.orekit.control.indirect;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UnboundedCartesianEnergyNeglectingMassTest {

    @Test
    void testGetMassFlowRate() {
        // GIVEN
        final UnboundedCartesianEnergyNeglectingMass energyNeglectingMass = new UnboundedCartesianEnergyNeglectingMass();
        // WHEN
        final double actualFlowRate = energyNeglectingMass.getMassFlowRate();
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

}

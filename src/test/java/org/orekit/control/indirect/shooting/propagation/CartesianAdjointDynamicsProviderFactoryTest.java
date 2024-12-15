package org.orekit.control.indirect.shooting.propagation;

import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.control.indirect.adjoint.cost.BoundedCartesianEnergy;
import org.orekit.control.indirect.adjoint.cost.CartesianFuelCost;
import org.orekit.control.indirect.adjoint.cost.FieldBoundedCartesianEnergy;
import org.orekit.control.indirect.adjoint.cost.FieldCartesianFuelCost;
import org.orekit.control.indirect.adjoint.cost.FieldUnboundedCartesianEnergy;
import org.orekit.control.indirect.adjoint.cost.FieldUnboundedCartesianEnergyNeglectingMass;
import org.orekit.control.indirect.adjoint.cost.UnboundedCartesianEnergy;
import org.orekit.control.indirect.adjoint.cost.UnboundedCartesianEnergyNeglectingMass;
import org.orekit.propagation.events.EventDetectionSettings;

class CartesianAdjointDynamicsProviderFactoryTest {

    private static final String ADJOINT_NAME = "adjoint";

    @Test
    void testBuildUnboundedEnergyProviderNeglectingMass() {
        // GIVEN

        // WHEN
        final CartesianAdjointDynamicsProvider provider = CartesianAdjointDynamicsProviderFactory.buildUnboundedEnergyProviderNeglectingMass(ADJOINT_NAME);
        // THEN
        Assertions.assertEquals(provider.getDimension(), provider.buildAdditionalDerivativesProvider().getDimension());
        Assertions.assertEquals(ADJOINT_NAME, provider.getAdjointName());
        Assertions.assertInstanceOf(UnboundedCartesianEnergyNeglectingMass.class,
                provider.buildAdditionalDerivativesProvider().getCost());
        Assertions.assertEquals(0.,
                provider.buildAdditionalDerivativesProvider().getCost().getMassFlowRateFactor());
        final Binary64Field field = Binary64Field.getInstance();
        Assertions.assertInstanceOf(FieldUnboundedCartesianEnergyNeglectingMass.class,
                provider.buildFieldAdditionalDerivativesProvider(field).getCost());
        Assertions.assertEquals(0.,
                provider.buildFieldAdditionalDerivativesProvider(field).getCost().getMassFlowRateFactor().getReal());
    }

    @Test
    void testBuildUnboundedEnergyProvider() {
        // GIVEN
        final double massFlowRateFactor = 1.;
        // WHEN
        final CartesianAdjointDynamicsProvider provider = CartesianAdjointDynamicsProviderFactory.buildUnboundedEnergyProvider(ADJOINT_NAME,
                massFlowRateFactor, EventDetectionSettings.getDefaultEventDetectionSettings());
        // THEN
        Assertions.assertEquals(provider.getDimension(), provider.buildAdditionalDerivativesProvider().getDimension());
        Assertions.assertEquals(ADJOINT_NAME, provider.getAdjointName());
        Assertions.assertInstanceOf(UnboundedCartesianEnergy.class,
                provider.buildAdditionalDerivativesProvider().getCost());
        Assertions.assertEquals(massFlowRateFactor,
                provider.buildAdditionalDerivativesProvider().getCost().getMassFlowRateFactor());
        final Binary64Field field = Binary64Field.getInstance();
        Assertions.assertInstanceOf(FieldUnboundedCartesianEnergy.class,
                provider.buildFieldAdditionalDerivativesProvider(field).getCost());
        Assertions.assertEquals(massFlowRateFactor,
                provider.buildFieldAdditionalDerivativesProvider(field).getCost().getMassFlowRateFactor().getReal());
    }

    @Test
    void testBuildBoundedEnergyProvider() {
        // GIVEN
        final double massFlowRateFactor = 1.;
        final EventDetectionSettings detectionSettings = Mockito.mock(EventDetectionSettings.class);

        // WHEN
        final CartesianAdjointDynamicsProvider provider = CartesianAdjointDynamicsProviderFactory.buildBoundedEnergyProvider(ADJOINT_NAME,
                massFlowRateFactor, 2., detectionSettings);
        // THEN
        Assertions.assertEquals(provider.getDimension(), provider.buildAdditionalDerivativesProvider().getDimension());
        Assertions.assertEquals(ADJOINT_NAME, provider.getAdjointName());
        Assertions.assertInstanceOf(BoundedCartesianEnergy.class,
                provider.buildAdditionalDerivativesProvider().getCost());
        Assertions.assertEquals(massFlowRateFactor,
                provider.buildAdditionalDerivativesProvider().getCost().getMassFlowRateFactor());
        final Binary64Field field = Binary64Field.getInstance();
        Assertions.assertInstanceOf(FieldBoundedCartesianEnergy.class,
                provider.buildFieldAdditionalDerivativesProvider(field).getCost());
        Assertions.assertEquals(massFlowRateFactor,
                provider.buildFieldAdditionalDerivativesProvider(field).getCost().getMassFlowRateFactor().getReal());
    }

    @Test
    void testBuildBoundedFuelCostProvider() {
        // GIVEN
        final double massFlowRateFactor = 1.;
        final EventDetectionSettings detectionSettings = Mockito.mock(EventDetectionSettings.class);
        // WHEN
        final CartesianAdjointDynamicsProvider provider = CartesianAdjointDynamicsProviderFactory.buildBoundedFuelCostProvider(ADJOINT_NAME,
                massFlowRateFactor, 2., detectionSettings);
        // THEN
        Assertions.assertEquals(provider.getDimension(), provider.buildAdditionalDerivativesProvider().getDimension());
        Assertions.assertEquals(ADJOINT_NAME, provider.getAdjointName());
        Assertions.assertInstanceOf(CartesianFuelCost.class,
                provider.buildAdditionalDerivativesProvider().getCost());
        Assertions.assertEquals(massFlowRateFactor,
                provider.buildAdditionalDerivativesProvider().getCost().getMassFlowRateFactor());
        final Binary64Field field = Binary64Field.getInstance();
        Assertions.assertInstanceOf(FieldCartesianFuelCost.class,
                provider.buildFieldAdditionalDerivativesProvider(field).getCost());
        Assertions.assertEquals(massFlowRateFactor,
                provider.buildFieldAdditionalDerivativesProvider(field).getCost().getMassFlowRateFactor().getReal());
    }
    
}
package org.orekit.control.indirect.shooting.propagation;

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.orekit.control.indirect.adjoint.cost.BoundedCartesianEnergy;
import org.orekit.control.indirect.adjoint.cost.CartesianCost;
import org.orekit.control.indirect.adjoint.cost.CartesianFlightDurationCost;
import org.orekit.control.indirect.adjoint.cost.CartesianFuelCost;
import org.orekit.control.indirect.adjoint.cost.FieldBoundedCartesianEnergy;
import org.orekit.control.indirect.adjoint.cost.FieldCartesianCost;
import org.orekit.control.indirect.adjoint.cost.FieldCartesianFlightDurationCost;
import org.orekit.control.indirect.adjoint.cost.FieldCartesianFuelCost;
import org.orekit.control.indirect.adjoint.cost.FieldLogarithmicBarrierCartesianFuel;
import org.orekit.control.indirect.adjoint.cost.FieldQuadraticPenaltyCartesianFuel;
import org.orekit.control.indirect.adjoint.cost.FieldUnboundedCartesianEnergy;
import org.orekit.control.indirect.adjoint.cost.FieldUnboundedCartesianEnergyNeglectingMass;
import org.orekit.control.indirect.adjoint.cost.LogarithmicBarrierCartesianFuel;
import org.orekit.control.indirect.adjoint.cost.QuadraticPenaltyCartesianFuel;
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

    @ParameterizedTest
    @ValueSource(doubles = {0., 1})
    void testBuildFlightDurationProvider(final double massFlowRateFactor) {
        // GIVEN

        // WHEN
        final CartesianAdjointDynamicsProvider provider = CartesianAdjointDynamicsProviderFactory.buildFlightDurationProvider(ADJOINT_NAME,
                massFlowRateFactor, 2.);
        // THEN
        Assertions.assertEquals(provider.getDimension(), provider.buildAdditionalDerivativesProvider().getDimension());
        Assertions.assertEquals(ADJOINT_NAME, provider.getAdjointName());
        Assertions.assertInstanceOf(CartesianFlightDurationCost.class,
                provider.buildAdditionalDerivativesProvider().getCost());
        Assertions.assertEquals(massFlowRateFactor,
                provider.buildAdditionalDerivativesProvider().getCost().getMassFlowRateFactor());
        final Binary64Field field = Binary64Field.getInstance();
        Assertions.assertInstanceOf(FieldCartesianFlightDurationCost.class,
                provider.buildFieldAdditionalDerivativesProvider(field).getCost());
        Assertions.assertEquals(massFlowRateFactor,
                provider.buildFieldAdditionalDerivativesProvider(field).getCost().getMassFlowRateFactor().getReal());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0., 1})
    void testBuildBoundedEnergyProvider(final double massFlowRateFactor) {
        // GIVEN
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

    @Test
    void testBuildQuadraticPenaltyFuelCostProvider() {
        // GIVEN
        final double massFlowRateFactor = 1.;
        final EventDetectionSettings detectionSettings = Mockito.mock(EventDetectionSettings.class);
        // WHEN
        final CartesianAdjointDynamicsProvider provider = CartesianAdjointDynamicsProviderFactory.buildQuadraticPenaltyFuelCostProvider(ADJOINT_NAME,
                massFlowRateFactor, 2., 0.5, detectionSettings);
        // THEN
        Assertions.assertEquals(provider.getDimension(), provider.buildAdditionalDerivativesProvider().getDimension());
        Assertions.assertEquals(ADJOINT_NAME, provider.getAdjointName());
        final CartesianCost cost = provider.buildAdditionalDerivativesProvider().getCost();
        Assertions.assertInstanceOf(QuadraticPenaltyCartesianFuel.class, cost);
        Assertions.assertEquals(massFlowRateFactor, cost.getMassFlowRateFactor());
        final Binary64Field field = Binary64Field.getInstance();
        final FieldCartesianCost<Binary64> fieldCartesianCost = provider.buildFieldAdditionalDerivativesProvider(field).getCost();
        Assertions.assertInstanceOf(FieldQuadraticPenaltyCartesianFuel.class, fieldCartesianCost);
        Assertions.assertEquals(massFlowRateFactor, fieldCartesianCost.getMassFlowRateFactor().getReal());
        Assertions.assertEquals(((QuadraticPenaltyCartesianFuel) cost).getEpsilon(),
                ((FieldQuadraticPenaltyCartesianFuel<Binary64>) fieldCartesianCost).getEpsilon().getReal());
    }

    @Test
    void testBuildLogarithmicBarrierFuelCostProvider() {
        // GIVEN
        final double massFlowRateFactor = 1.;
        // WHEN
        final CartesianAdjointDynamicsProvider provider = CartesianAdjointDynamicsProviderFactory.buildLogarithmicBarrierFuelCostProvider(ADJOINT_NAME,
                massFlowRateFactor, 2., 0.5);
        // THEN
        Assertions.assertEquals(provider.getDimension(), provider.buildAdditionalDerivativesProvider().getDimension());
        Assertions.assertEquals(ADJOINT_NAME, provider.getAdjointName());
        final CartesianCost cost = provider.buildAdditionalDerivativesProvider().getCost();
        Assertions.assertInstanceOf(LogarithmicBarrierCartesianFuel.class, cost);
        Assertions.assertEquals(massFlowRateFactor, cost.getMassFlowRateFactor());
        final Binary64Field field = Binary64Field.getInstance();
        final FieldCartesianCost<Binary64> fieldCartesianCost = provider.buildFieldAdditionalDerivativesProvider(field).getCost();
        Assertions.assertInstanceOf(FieldLogarithmicBarrierCartesianFuel.class, fieldCartesianCost);
        Assertions.assertEquals(massFlowRateFactor, fieldCartesianCost.getMassFlowRateFactor().getReal());
        Assertions.assertEquals(((LogarithmicBarrierCartesianFuel) cost).getEpsilon(),
                ((FieldLogarithmicBarrierCartesianFuel<Binary64>) fieldCartesianCost).getEpsilon().getReal());
    }

}
package org.orekit.control.indirect.adjoint;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.control.indirect.adjoint.cost.CartesianCost;
import org.orekit.control.indirect.adjoint.cost.TestCost;
import org.orekit.control.indirect.adjoint.cost.UnboundedCartesianEnergyNeglectingMass;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.CombinedDerivatives;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

class CartesianAdjointDerivativesProviderTest {

    @Test
    void testInitException() {
        // GIVEN
        final String name = "name";
        final double mu = Constants.EGM96_EARTH_MU;
        final CartesianAdjointDerivativesProvider derivativesProvider = new CartesianAdjointDerivativesProvider(name,
                new UnboundedCartesianEnergyNeglectingMass(), new CartesianAdjointKeplerianTerm(mu));
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        Mockito.when(mockedState.isOrbitDefined()).thenReturn(true);
        final Orbit mockedOrbit = Mockito.mock(Orbit.class);
        Mockito.when(mockedOrbit.getType()).thenReturn(OrbitType.EQUINOCTIAL);
        Mockito.when(mockedState.getOrbit()).thenReturn(mockedOrbit);
        // WHEN
        final Exception exception = Assertions.assertThrows(OrekitException.class,
                () -> derivativesProvider.init(mockedState, null));
        Assertions.assertEquals(OrekitMessages.WRONG_COORDINATES_FOR_ADJOINT_EQUATION.getSourceString(),
                exception.getMessage());
    }

    @Test
    void testIntegration() {
        // GIVEN
        final String name = "name";
        final double mu = Constants.EGM96_EARTH_MU;
        final CartesianAdjointDerivativesProvider derivativesProvider = new CartesianAdjointDerivativesProvider(name,
                new UnboundedCartesianEnergyNeglectingMass(), new CartesianAdjointKeplerianTerm(mu));
        final NumericalPropagator propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(100.));
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(7e6, 1e3, 0), new Vector3D(10., 7e3, -200)),
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, mu);
        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.setInitialState(new SpacecraftState(orbit).addAdditionalState(name, new double[6]));
        propagator.addAdditionalDerivativesProvider(derivativesProvider);
        // WHEN
        final SpacecraftState state = propagator.propagate(orbit.getDate().shiftedBy(1000.));
        // THEN
        Assertions.assertTrue(propagator.isAdditionalStateManaged(name));
        final double[] finalAdjoint = state.getAdditionalState(name);
        Assertions.assertEquals(0, finalAdjoint[0]);
        Assertions.assertEquals(0, finalAdjoint[1]);
        Assertions.assertEquals(0, finalAdjoint[2]);
        Assertions.assertEquals(0, finalAdjoint[3]);
        Assertions.assertEquals(0, finalAdjoint[4]);
        Assertions.assertEquals(0, finalAdjoint[5]);
    }

    @Test
    void testGetName() {
        // GIVEN
        final String expectedName = "1";
        final CartesianAdjointDerivativesProvider derivativesProvider = new CartesianAdjointDerivativesProvider(expectedName,
                Mockito.mock(CartesianCost.class));
        // WHEN
        final String actualName = derivativesProvider.getName();
        // THEN
        Assertions.assertEquals(expectedName, actualName);
    }

    @Test
    void testGetCost() {
        // GIVEN
        final CartesianCost expectedCost = Mockito.mock(CartesianCost.class);
        final CartesianAdjointDerivativesProvider derivativesProvider = new CartesianAdjointDerivativesProvider("", expectedCost);
        // WHEN
        final CartesianCost actualCost = derivativesProvider.getCost();
        // THEN
        Assertions.assertEquals(expectedCost, actualCost);
    }

    @Test
    void testCombinedDerivatives() {
        // GIVEN
        final CartesianCost cost = new TestCost();
        final CartesianAdjointDerivativesProvider derivativesProvider = new CartesianAdjointDerivativesProvider("",
                cost);
        final SpacecraftState state = getState(derivativesProvider.getName());
        // WHEN
        final CombinedDerivatives combinedDerivatives = derivativesProvider.combinedDerivatives(state);
        // THEN
        final double[] increment = combinedDerivatives.getMainStateDerivativesIncrements();
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(0., increment[i]);
        }
        final double mass = state.getMass();
        Assertions.assertEquals(1., increment[3] * mass);
        Assertions.assertEquals(2., increment[4] * mass);
        Assertions.assertEquals(3., increment[5] * mass);
        Assertions.assertEquals(-10., increment[6]);
    }

    @Test
    void testCombinedDerivativesWithEquationTerm() {
        // GIVEN
        final CartesianCost cost = new TestCost();
        final CartesianAdjointEquationTerm equationTerm = new TestAdjointTerm();
        final CartesianAdjointDerivativesProvider derivativesProvider = new CartesianAdjointDerivativesProvider("",
                cost, equationTerm);
        final SpacecraftState state = getState(derivativesProvider.getName());
        // WHEN
        final CombinedDerivatives combinedDerivatives = derivativesProvider.combinedDerivatives(state);
        // THEN
        final double[] adjointDerivatives = combinedDerivatives.getAdditionalDerivatives();
        Assertions.assertEquals(1., adjointDerivatives[0]);
        Assertions.assertEquals(10., adjointDerivatives[1]);
        Assertions.assertEquals(100., adjointDerivatives[2]);
        Assertions.assertEquals(-1, adjointDerivatives[3]);
        Assertions.assertEquals(-1, adjointDerivatives[4]);
        Assertions.assertEquals(-1, adjointDerivatives[5]);
    }

    private static SpacecraftState getState(final String name) {
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(Vector3D.MINUS_I, Vector3D.PLUS_K),
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, 1.);
        final SpacecraftState stateWithoutAdditional = new SpacecraftState(orbit);
        return stateWithoutAdditional.addAdditionalState(name, 1., 1., 1., 1., 1., 1.);
    }

    private static class TestAdjointTerm implements CartesianAdjointEquationTerm {

        @Override
        public double[] getVelocityAdjointContribution(double[] stateVariables, double[] adjointVariables) {
            return new double[] { 1., 10., 100. };
        }

        @Override
        public <T extends CalculusFieldElement<T>> T[] getVelocityAdjointContribution(T[] stateVariables, T[] adjointVariables) {
            return null;
        }
    }

}

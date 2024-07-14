package org.orekit.control.indirect;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.control.indirect.adjoint.CartesianAdjointEquationTerm;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;

import java.util.stream.Stream;

class CartesianAdjointDerivativesProviderTest {

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

    private static class TestCost implements CartesianCost {

        @Override
        public double getMassFlowRate() {
            return 0;
        }

        @Override
        public Vector3D getThrustVector(double[] adjointVariables, double mass) {
            return Vector3D.ZERO;
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustVector(T[] adjointVariables, T mass) {
            return FieldVector3D.getZero(mass.getField());
        }

        @Override
        public Stream<EventDetector> getEventDetectors() {
            return null;
        }

        @Override
        public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(Field<T> field) {
            return null;
        }
    }

    private static class TestAdjointTerm implements CartesianAdjointEquationTerm {

        @Override
        public double[] getVelocityAdjointContribution(double[] stateVariables, double[] adjointVariables) {
            return new double[] { 1., 0., 0. };
        }

        @Override
        public <T extends CalculusFieldElement<T>> T[] getVelocityAdjointContribution(T[] stateVariables, T[] adjointVariables) {
            return null;
        }
    }

}

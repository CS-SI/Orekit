package org.orekit.bodies;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

import java.util.Arrays;

class PosVelChebyshevTest {

    private static final double DURATION = 10.;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data/de405-ephemerides");
    }

    @Test
    void testGetPosition() {
        // Given
        final PosVelChebyshev chebyshev = createPolynomial();
        final AbsoluteDate date = chebyshev.getDate().shiftedBy(DURATION / 2);
        // When
        final double[] actualPositionArray = chebyshev.getPosition(date).toArray();
        // Then
        final double[] expectedPositionArray = chebyshev.getPositionVelocityAcceleration(date).getPosition().toArray();
        for (int i = 0; i < expectedPositionArray.length; i++) {
            Assertions.assertEquals(expectedPositionArray[i], actualPositionArray[i]);
        }
    }

    @Test
    void testFieldGetPositionWithGradient() {
        // Given
        final PosVelChebyshev chebyshev = createPolynomial();
        final AbsoluteDate date = chebyshev.getDate().shiftedBy(DURATION / 2);
        final int freeParameters = 1;
        final GradientField field = GradientField.getField(freeParameters);
        final Gradient variable = Gradient.variable(freeParameters, 0, 0);
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(field, date).shiftedBy(variable);
        // When
        final Gradient[] actualPositionArray = chebyshev.getPosition(fieldDate).toArray();
        // Then
        final Gradient[] expectedPositionArray = chebyshev.getPositionVelocityAcceleration(fieldDate).getPosition().toArray();
        for (int i = 0; i < expectedPositionArray.length; i++) {
            Assertions.assertEquals(expectedPositionArray[i], actualPositionArray[i]);
        }
    }

    @Test
    void testFieldWithGradientVersusNonField() {
        // Given
        final PosVelChebyshev chebyshev = createPolynomial();
        final AbsoluteDate date = chebyshev.getDate().shiftedBy(DURATION / 2);
        final int freeParameters = 1;
        final GradientField field = GradientField.getField(freeParameters);
        final Gradient variable = Gradient.variable(freeParameters, 0, 0);
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(field, date).shiftedBy(variable);
        // When
        final FieldPVCoordinates<Gradient> fieldPVCoordinates = chebyshev.getPositionVelocityAcceleration(fieldDate);
        // Then
        final PVCoordinates expectedPVCoordinates = chebyshev.getPositionVelocityAcceleration(fieldDate.toAbsoluteDate());
        final PVCoordinates actualPVCoordinates = fieldPVCoordinates.toPVCoordinates();
        Assertions.assertEquals(expectedPVCoordinates.getPosition(), actualPVCoordinates.getPosition());
        Assertions.assertEquals(expectedPVCoordinates.getVelocity(), actualPVCoordinates.getVelocity());
        Assertions.assertEquals(expectedPVCoordinates.getAcceleration(), actualPVCoordinates.getAcceleration());
    }

    @Test
    void testVelocityAndAccelerationAreDerivativesOfPosition() {
        // Given
        final PosVelChebyshev chebyshev = createPolynomial();
        final AbsoluteDate date = chebyshev.getDate().shiftedBy(DURATION / 2);
        final UnivariateDerivative2 variable = new UnivariateDerivative2(0., 1., 0.);
        final FieldAbsoluteDate<UnivariateDerivative2> fieldDate =
                new FieldAbsoluteDate<>(variable.getField(), date).shiftedBy(variable);
        // When
        final FieldPVCoordinates<UnivariateDerivative2> fieldPVCoordinates = chebyshev.getPositionVelocityAcceleration(fieldDate);
        // Then
        final PVCoordinates expectedPVCoordinates = chebyshev.getPositionVelocityAcceleration(fieldDate.toAbsoluteDate());
        final Vector3D expectedVelocity = expectedPVCoordinates.getVelocity();
        final Vector3D expectedAcceleration = expectedPVCoordinates.getAcceleration();
        final FieldVector3D<UnivariateDerivative2> fieldPosition = fieldPVCoordinates.getPosition();
        final double tolerance = 1e-15;
        Assertions.assertEquals(expectedVelocity.getX(), fieldPosition.getX().getDerivative(1), tolerance);
        Assertions.assertEquals(expectedVelocity.getY(), fieldPosition.getY().getDerivative(1), tolerance);
        Assertions.assertEquals(expectedVelocity.getZ(), fieldPosition.getZ().getDerivative(1), tolerance);
        Assertions.assertEquals(expectedAcceleration.getX(), fieldPosition.getX().getDerivative(2), tolerance);
        Assertions.assertEquals(expectedAcceleration.getY(), fieldPosition.getY().getDerivative(2), tolerance);
        Assertions.assertEquals(expectedAcceleration.getZ(), fieldPosition.getZ().getDerivative(2), tolerance);
    }

    private PosVelChebyshev createPolynomial() {
        final AbsoluteDate start = AbsoluteDate.ARBITRARY_EPOCH;
        final TimeScale timeScale = TimeScalesFactory.getTAI();
        final int degree = 10;
        final double[] firstComponents = new double[degree];
        final double[] secondComponents = new double[degree];
        final double[] thirdComponents = new double[degree];
        final double arbitraryCoefficient = 1.;
        Arrays.fill(firstComponents, arbitraryCoefficient);
        Arrays.fill(secondComponents, arbitraryCoefficient);
        Arrays.fill(thirdComponents, arbitraryCoefficient);
        return new PosVelChebyshev(start, timeScale, DURATION, firstComponents, secondComponents, thirdComponents);
    }
}

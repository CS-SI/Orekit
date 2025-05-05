package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

class TargetProviderTest {

    @Test
    void testGetTargetDirection() {
        // GIVEN
        final TestTargetProvider testTargetProvider = new TestTargetProvider();
        final TimeStampedPVCoordinates pvCoordinates = new TimeStampedPVCoordinates(AbsoluteDate.ARBITRARY_EPOCH,
                new PVCoordinates());
        // WHEN
        final Vector3D targetVector = testTargetProvider.getTargetDirection(null, null, pvCoordinates, null);
        // THEN
        final Vector3D expectedVector = testTargetProvider.getDerivative2TargetDirection(null, null,
                pvCoordinates, null).toVector3D();
        Assertions.assertEquals(expectedVector, targetVector);
    }

    @Test
    void testFieldGetTargetDirection() {
        // GIVEN
        final TestTargetProvider testTargetProvider = new TestTargetProvider();
        final TimeStampedPVCoordinates pvCoordinates = new TimeStampedPVCoordinates(AbsoluteDate.ARBITRARY_EPOCH,
                new PVCoordinates());
        final TimeStampedFieldPVCoordinates<Complex> fieldPVCoordinates = new TimeStampedFieldPVCoordinates<Complex>(ComplexField.getInstance(),
                pvCoordinates);
        // WHEN
        final FieldVector3D<Complex> targetVector = testTargetProvider.getTargetDirection(null, null, fieldPVCoordinates, null);
        // THEN
        final FieldVector3D<FieldUnivariateDerivative2<Complex>> ud2Vector = testTargetProvider.getDerivative2TargetDirection(null, null,
                fieldPVCoordinates, null);
        final FieldVector3D<Complex> expectedVector = new FieldVector3D<>(ud2Vector.getX().getValue(), ud2Vector.getY().getValue(),
                ud2Vector.getZ().getValue());
        Assertions.assertEquals(expectedVector, targetVector);
    }

    private static class TestTargetProvider implements TargetProvider {

        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getTargetDirection(ExtendedPositionProvider sun, OneAxisEllipsoid earth,
                                                                                        TimeStampedFieldPVCoordinates<T> pv, Frame frame) {
            return new FieldVector3D<>(pv.getDate().getField(), pv.getPosition().toVector3D());
        }
    }

}

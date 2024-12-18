package org.orekit.forces.radiation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.ExtendedPositionProvider;

import java.util.Collections;
import java.util.List;

class AbstractLightFluxModelTest {

    @Test
    void testGetLightFluxVectorWithZeroLightingRatio() {
        // GIVEN
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        final AbstractLightFluxModel mockedFluxModel = Mockito.mock(AbstractLightFluxModel.class);
        Mockito.when(mockedFluxModel.getLightingRatio(Mockito.any(Vector3D.class), Mockito.any(Vector3D.class))).thenReturn(0.);
        Mockito.when(mockedFluxModel.getLightFluxVector(mockedState)).thenCallRealMethod();
        // WHEN
        final Vector3D actualFluxVector = mockedFluxModel.getLightFluxVector(mockedState);
        // THEN
        Assertions.assertEquals(Vector3D.ZERO, actualFluxVector);
    }

    @Test
    void testGetLightFluxVector() {
        // GIVEN
        final Vector3D position = new Vector3D(1.0, 2.0, 3.0);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame mockedFrame = Mockito.mock(Frame.class);
        final SpacecraftState mockedState = mockState(position, date, mockedFrame);
        final ExtendedPVCoordinatesProvider mockedProvider = Mockito.mock(ExtendedPVCoordinatesProvider.class);
        final Vector3D sunPosition = Vector3D.PLUS_K;
        Mockito.when(mockedProvider.getPosition(date, mockedFrame)).thenReturn(sunPosition);
        final TestLightFluxModel testLightFluxModel = new TestLightFluxModel(mockedProvider);
        // WHEN
        final Vector3D actualFluxVector = testLightFluxModel.getLightFluxVector(mockedState);
        // THEN
        final Vector3D expectedFluxVector = position.subtract(sunPosition).normalize();
        Assertions.assertEquals(expectedFluxVector, actualFluxVector);
    }

    private SpacecraftState mockState(final Vector3D position, final AbsoluteDate date, final Frame frame) {
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        Mockito.when(mockedState.getFrame()).thenReturn(frame);
        Mockito.when(mockedState.getPosition()).thenReturn(position);
        Mockito.when(mockedState.getDate()).thenReturn(date);
        return mockedState;
    }

    @Test
    void testGetLightingRatioField() {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();
        final FieldVector3D<Complex> position = new FieldVector3D<>(field, new Vector3D(1.0, 2.0, 3.0));
        final FieldAbsoluteDate<Complex> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        final Frame mockedFrame = Mockito.mock(Frame.class);
        final FieldSpacecraftState<Complex> mockedFieldState = mockState(position, date, mockedFrame);
        final ExtendedPVCoordinatesProvider mockedProvider = Mockito.mock(ExtendedPVCoordinatesProvider.class);
        final FieldVector3D<Complex> sunPosition = FieldVector3D.getMinusJ(field);
        Mockito.when(mockedProvider.getPosition(date, mockedFrame)).thenReturn(sunPosition);
        final TestLightFluxModel testLightFluxModel = new TestLightFluxModel(mockedProvider);
        // WHEN
        final Complex fieldLightingRatio = testLightFluxModel.getLightingRatio(mockedFieldState);
        // THEN
        final SpacecraftState mockedState = mockState(position.toVector3D(), date.toAbsoluteDate(), mockedFrame);
        final double expectedLightingRatio = testLightFluxModel.getLightingRatio(mockedState);
        Assertions.assertEquals(expectedLightingRatio, fieldLightingRatio.getReal());
    }

    @Test
    void testGetLightFluxVectorField() {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();
        final FieldVector3D<Complex> position = new FieldVector3D<>(field, new Vector3D(1.0, 2.0, 3.0));
        final FieldAbsoluteDate<Complex> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        final Frame mockedFrame = Mockito.mock(Frame.class);
        final FieldSpacecraftState<Complex> mockedState = mockState(position, date, mockedFrame);
        final ExtendedPVCoordinatesProvider mockedProvider = Mockito.mock(ExtendedPVCoordinatesProvider.class);
        final FieldVector3D<Complex> sunPosition = FieldVector3D.getMinusJ(field);
        Mockito.when(mockedProvider.getPosition(date, mockedFrame)).thenReturn(sunPosition);
        final TestLightFluxModel testLightFluxModel = new TestLightFluxModel(mockedProvider);
        // WHEN
        final FieldVector3D<Complex> actualFluxVector = testLightFluxModel.getLightFluxVector(mockedState);
        // THEN
        final FieldVector3D<Complex> expectedFluxVector = position.subtract(sunPosition).normalize();
        Assertions.assertEquals(expectedFluxVector, actualFluxVector);
    }

    @SuppressWarnings("unchecked")
    private FieldSpacecraftState<Complex> mockState(final FieldVector3D<Complex> position, final FieldAbsoluteDate<Complex> date,
                                                    final Frame frame) {
        final FieldSpacecraftState<Complex> mockedState = Mockito.mock(FieldSpacecraftState.class);
        Mockito.when(mockedState.getFrame()).thenReturn(frame);
        Mockito.when(mockedState.getPosition()).thenReturn(position);
        Mockito.when(mockedState.getDate()).thenReturn(date);
        return mockedState;
    }

    @Test
    void testConstructor() {
        // GIVEN
        final ExtendedPVCoordinatesProvider mockedProvider = Mockito.mock(ExtendedPVCoordinatesProvider.class);
        final TestLightFluxModel testLightFluxModel = new TestLightFluxModel(mockedProvider);
        // WHEN
        final ExtendedPositionProvider actualProvider = testLightFluxModel.getOccultedBody();
        // THEN
        Assertions.assertEquals(mockedProvider, actualProvider);
    }

    private static class TestLightFluxModel extends AbstractLightFluxModel {

        public TestLightFluxModel(final ExtendedPVCoordinatesProvider occultedBody) {
            super(occultedBody);
        }

        @Override
        protected Vector3D getUnoccultedFluxVector(Vector3D relativePosition) {
            return relativePosition.normalize();
        }

        @Override
        protected <T extends CalculusFieldElement<T>> FieldVector3D<T> getUnoccultedFluxVector(FieldVector3D<T> relativePosition) {
            return relativePosition.normalize();
        }

        @Override
        protected double getLightingRatio(Vector3D position, Vector3D occultedBodyPosition) {
            return 1.;
        }

        @Override
        protected <T extends CalculusFieldElement<T>> T getLightingRatio(FieldVector3D<T> position, FieldVector3D<T> occultedBodyPosition) {
            return position.getX().getField().getOne();
        }

        @Override
        public List<EventDetector> getEclipseConditionsDetector() {
            return Collections.emptyList();
        }

        @Override
        public <T extends CalculusFieldElement<T>> List<FieldEventDetector<T>> getFieldEclipseConditionsDetector(Field<T> field) {
            return Collections.emptyList();
        }
    }

}

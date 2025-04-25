package org.orekit.propagation.events;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative1;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative1Field;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.bodies.AnalyticalSolarPositionProvider;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;
import org.orekit.utils.FieldPVCoordinates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

class FieldExtremumAngularSeparationDetectorTest {

    @BeforeAll
    static void setUpBeforeClass() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testGetter() {
        // GIVEN
        final ExtendedPositionProvider mockedBeacon = mock();
        final ExtendedPositionProvider mockedObserver = mock();
        final FieldEventDetectionSettings<Binary64> detectionSettings = new FieldEventDetectionSettings<>(Binary64Field.getInstance(),
                EventDetectionSettings.getDefaultEventDetectionSettings());
        // WHEN
        
        final FieldExtremumAngularSeparationDetector<Binary64> detector = new FieldExtremumAngularSeparationDetector<>(detectionSettings,
                new FieldStopOnEvent<>(), mockedBeacon, mockedObserver);
        // THEN
        assertEquals(mockedBeacon, detector.getBeacon());
        assertEquals(mockedObserver, detector.getObserver());
    }

    @Test
    void testG() {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        final ExtendedPositionProvider beacon = new AnalyticalSolarPositionProvider();
        final ExtendedPositionProvider observer = new TopocentricFrame(ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true)),
                new GeodeticPoint(0., 1., 2.), "");
        final FieldEventDetectionSettings<Binary64> detectionSettings = new FieldEventDetectionSettings<>(field,
                EventDetectionSettings.getDefaultEventDetectionSettings());
        final FieldExtremumAngularSeparationDetector<Binary64> detector = new FieldExtremumAngularSeparationDetector<>(detectionSettings,
                new FieldStopOnEvent<>(), beacon, observer);
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(field, state);
        // WHEN
        final Binary64 actualG = detector.g(fieldState);
        // THEN
        final FieldPVCoordinates<FieldUnivariateDerivative1<Binary64>> pv = fieldState.getPVCoordinates().toUnivariateDerivative1PV();
        final FieldUnivariateDerivative1<Binary64> dt = new FieldUnivariateDerivative1<>(field.getZero(), field.getOne());
        final FieldAbsoluteDate<FieldUnivariateDerivative1<Binary64>> fieldDate = new FieldAbsoluteDate<>(FieldUnivariateDerivative1Field.getUnivariateDerivative1Field(field),
                state.getDate()).shiftedBy(dt);
        final FieldVector3D<FieldUnivariateDerivative1<Binary64>> bP = beacon.getPosition(fieldDate, fieldState.getFrame());
        final FieldVector3D<FieldUnivariateDerivative1<Binary64>> oP = observer.getPosition(fieldDate, fieldState.getFrame());
        final FieldUnivariateDerivative1<Binary64> separation = FieldVector3D.angle(pv.getPosition().subtract(oP), bP.subtract(oP));
        final Binary64 expectedG = separation.getFirstDerivative();
        assertEquals(expectedG, actualG);
    }
}

package org.orekit.propagation.events;

import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.bodies.AnalyticalSolarPositionProvider;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;
import org.orekit.utils.FieldPVCoordinates;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ExtremumAngularSeparationDetectorTest {

    @BeforeAll
    static void setUpBeforeClass() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testGetter() {
        // GIVEN
        final ExtendedPositionProvider mockedBeacon = mock();
        final ExtendedPositionProvider mockedObserver = mock();
        // WHEN
        final ExtremumAngularSeparationDetector detector = new ExtremumAngularSeparationDetector(EventDetectionSettings.getDefaultEventDetectionSettings(),
                new StopOnEvent(), mockedBeacon, mockedObserver);
        // THEN
        assertEquals(mockedBeacon, detector.getBeacon());
        assertEquals(mockedObserver, detector.getObserver());
    }

    @Test
    void testG() {
        // GIVEN
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        final ExtendedPositionProvider beacon = new AnalyticalSolarPositionProvider();
        final ExtendedPositionProvider observer = new TopocentricFrame(ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true)),
                new GeodeticPoint(0., 1., 2.), "");
        final ExtremumAngularSeparationDetector detector = new ExtremumAngularSeparationDetector(EventDetectionSettings.getDefaultEventDetectionSettings(),
                new StopOnEvent(), beacon, observer);
        // WHEN
        final double actualG = detector.g(state);
        // THEN
        final FieldAbsoluteDate<UnivariateDerivative2> fieldDate = new FieldAbsoluteDate<>(UnivariateDerivative2Field.getInstance(),
                state.getDate()).shiftedBy(new UnivariateDerivative2(0, 1, 0));
        final FieldVector3D<UnivariateDerivative2> bP = beacon.getPosition(fieldDate, state.getFrame());
        final FieldVector3D<UnivariateDerivative2> oP = observer.getPosition(fieldDate, state.getFrame());
        final FieldPVCoordinates<UnivariateDerivative2> pv = state.getPVCoordinates().toUnivariateDerivative2PV();
        final UnivariateDerivative2 separation = FieldVector3D.angle(pv.getPosition().subtract(oP), bP.subtract(oP));
        final double expectedG = separation.getFirstDerivative();
        assertEquals(expectedG, actualG, 1e-15);
    }
}

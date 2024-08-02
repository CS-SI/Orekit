package org.orekit.propagation.events;

import org.hipparchus.Field;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class FieldExtremumApproachDetectorTest {
    /**
     * Test the detector on a keplerian orbit and detect extremum approach with Earth.
     */
    @Test
    void testStopPropagationClosestApproachByDefault() {
        // Given
        // Loading Orekit data
        Utils.setDataRoot("regular-data");

        // Generating orbit
        final Field<Binary64>             field       = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> initialDate = new FieldAbsoluteDate<>(field, new AbsoluteDate());
        final Frame                       frame       = FramesFactory.getEME2000();
        final Binary64                    mu          = new Binary64(398600e9); //m**3/s**2

        final Binary64 rp = new Binary64((6378 + 400) * 1000); //m
        final Binary64 ra = new Binary64((6378 + 800) * 1000); //m

        final Binary64 a       = ra.add(rp).divide(2); //m
        final Binary64 e       = ra.subtract(rp).divide(ra.add(rp)); //m
        final Binary64 i       = new Binary64(0); //rad
        final Binary64 pa      = new Binary64(0); //rad
        final Binary64 raan    = new Binary64(0); //rad
        final Binary64 anomaly = new Binary64(0); //rad
        final FieldOrbit<Binary64> orbit =
                new FieldKeplerianOrbit<>(a, e, i, pa, raan, anomaly, PositionAngleType.TRUE, frame, initialDate, mu);

        // Will detect extremum approaches with Earth
        final PVCoordinatesProvider earthPVProvider = CelestialBodyFactory.getEarth();

        // Initializing detector
        final FieldExtremumApproachDetector<Binary64> detector = new FieldExtremumApproachDetector<>(field, earthPVProvider);

        // Initializing propagator
        final FieldPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(orbit);
        propagator.addEventDetector(detector);

        // When
        final FieldSpacecraftState<Binary64> stateAtEvent =
                propagator.propagate(initialDate.shiftedBy(orbit.getKeplerianPeriod().multiply(2.)));

        // Then
        assertEquals(stateAtEvent.getDate().durationFrom(initialDate).getReal(),
                                orbit.getKeplerianPeriod().getReal(), 1e-9);

    }

    /**
     * Test the detector on a keplerian orbit and detect extremum approach with Earth.
     */
    @Test
    void testStopPropagationFarthestApproachWithHandler() {

        // Given
        // Loading Orekit data
        Utils.setDataRoot("regular-data");

        // Generating orbit
        final Field<Binary64>             field       = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> initialDate = new FieldAbsoluteDate<>(field, new AbsoluteDate());
        final Frame                       frame       = FramesFactory.getEME2000();
        final Binary64                    mu          = new Binary64(398600e9); //m**3/s**2

        final Binary64 rp = new Binary64(6378 + 400 * 1000); //m
        final Binary64 ra = new Binary64((6378 + 800) * 1000); //m

        final Binary64 a       = ra.add(rp).divide(2); //m
        final Binary64 e       = ra.subtract(rp).divide(ra.add(rp)); //m
        final Binary64 i       = new Binary64(0.); //rad
        final Binary64 pa      = new Binary64(0); //rad
        final Binary64 raan    = new Binary64(0); //rad
        final Binary64 anomaly = new Binary64(0); //rad
        final FieldOrbit<Binary64> orbit =
                new FieldKeplerianOrbit<>(a, e, i, pa, raan, anomaly, PositionAngleType.TRUE, frame, initialDate, mu);

        // Will detect extremum approaches with Earth
        final PVCoordinatesProvider earthPVProvider = CelestialBodyFactory.getEarth();

        // Initializing detector with custom handler
        final FieldExtremumApproachDetector<Binary64> detector =
                new FieldExtremumApproachDetector<>(field, earthPVProvider).withHandler(new FieldStopOnEvent<>());

        // Initializing propagator
        final FieldPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(orbit);
        propagator.addEventDetector(detector);

        // When
        final FieldSpacecraftState<Binary64> stateAtEvent =
                propagator.propagate(initialDate.shiftedBy(orbit.getKeplerianPeriod().multiply(2)));

        // Then
        assertEquals(stateAtEvent.getDate().durationFrom(initialDate).getReal(),
                                orbit.getKeplerianPeriod().divide(2).getReal(), 1e-7);

    }

    @Test
    @SuppressWarnings("unchecked")
    void testSecondaryPVCoordinatesProviderGetter() {
        // Given
        final Field<Binary64>                      field               = Binary64Field.getInstance();
        final FieldPVCoordinatesProvider<Binary64> secondaryPVProvider = mock(FieldPVCoordinatesProvider.class);

        final FieldExtremumApproachDetector<Binary64> extremumApproachDetector =
                new FieldExtremumApproachDetector<>(field, secondaryPVProvider);

        // When
        final FieldPVCoordinatesProvider<Binary64> returnedSecondaryPVProvider =
                extremumApproachDetector.getSecondaryPVProvider();

        // Then
        assertEquals(secondaryPVProvider, returnedSecondaryPVProvider);
    }

}

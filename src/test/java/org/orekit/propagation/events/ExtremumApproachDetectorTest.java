package org.orekit.propagation.events;

import org.assertj.core.api.Assertions;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;

import static org.assertj.core.api.Assertions.withPrecision;

public class ExtremumApproachDetectorTest {

    @Test
    @DisplayName("Test the detector on a keplerian orbit and detect extremum approach with Earth.")
    void Should_stop_propagation_at_closest_approach_by_default() {
        // Given
        Utils.setDataRoot("regular-data");

        final AbsoluteDate initialDate = new AbsoluteDate();
        final Frame frame = FramesFactory.getEME2000();
        final double mu = 398600e9; //m**3/s**2

        final double rp = (6378 + 400) * 1000; //m
        final double ra = (6378 + 800) * 1000; //m

        final double a = (ra + rp) / 2; //m
        final double e = (ra - rp) / (ra + rp); //m
        final double i = 0; //rad
        final double pa = 0; //rad
        final double raan = 0; //rad
        final double anomaly = FastMath.toRadians(0); //rad
        final Orbit orbit =
            new KeplerianOrbit(a, e, i, pa, raan, anomaly, PositionAngle.TRUE, frame, initialDate, mu);

        final PVCoordinatesProvider earthPVProvider = CelestialBodyFactory.getEarth();

        final ExtremumApproachDetector detector = new ExtremumApproachDetector(earthPVProvider);

        final Propagator propagator = new KeplerianPropagator(orbit);
        propagator.addEventDetector(detector);

        // When
        final SpacecraftState stateAtEvent =
            propagator.propagate(initialDate.shiftedBy(orbit.getKeplerianPeriod() * 2));

        // Then
        Assertions.assertThat(stateAtEvent.getDate().durationFrom(initialDate))
            .isEqualTo(orbit.getKeplerianPeriod(), withPrecision(2.0e-12));

    }

    @Test
    @DisplayName("Test the detector on a keplerian orbit and detect extremum approach with Earth.")
    void Should_stop_propagation_at_farthest_approach_with_handler() {
        // Given
        Utils.setDataRoot("regular-data");

        final AbsoluteDate initialDate = new AbsoluteDate();
        final Frame frame = FramesFactory.getEME2000();
        final double mu = 398600e9; //m**3/s**2

        final double rp = (6378 + 400) * 1000; //m
        final double ra = (6378 + 800) * 1000; //m

        final double a = (ra + rp) / 2; //m
        final double e = (ra - rp) / (ra + rp); //m
        final double i = 0; //rad
        final double pa = 0; //rad
        final double raan = 0; //rad
        final double anomaly = FastMath.toRadians(0); //rad
        final Orbit orbit =
            new KeplerianOrbit(a, e, i, pa, raan, anomaly, PositionAngle.TRUE, frame, initialDate, mu);

        final PVCoordinatesProvider earthPVProvider = CelestialBodyFactory.getEarth();

        final ExtremumApproachDetector detector =
            new ExtremumApproachDetector(earthPVProvider).withHandler(new StopOnEvent<>());

        final Propagator propagator = new KeplerianPropagator(orbit);
        propagator.addEventDetector(detector);

        // When
        final SpacecraftState stateAtEvent =
            propagator.propagate(initialDate.shiftedBy(orbit.getKeplerianPeriod() * 2));

        // Then
        Assertions.assertThat(stateAtEvent.getDate().durationFrom(initialDate))
            .isEqualTo(orbit.getKeplerianPeriod() / 2, withPrecision(1.27798E-8));

    }
}
package org.orekit.propagation.analytical.tle;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.conversion.FiniteDifferencePropagatorConverter;
import org.orekit.propagation.conversion.TLEPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;

public class TLEFittingTest {

    @Test
    public void test() {
        /**
         * This test should not fail.
         * It was written to test issue 724 
         * due to eccentricity range check in the TLE constructor
         * "org.orekit.errors.OrekitException: invalid TLE parameter eccentricity: 1.15 not in range [0, 1]"
         */

        Utils.setDataRoot("regular-data");

        int satellite_number = 99999;
        char classification = 'U';
        int launch_year = 2020;
        int launch_number = 1;
        String launch_piece = "a";
        int ephemeris_type = 0;
        int element_number = 1;
        int revolution_number = 1;
        double mean_motion_first_derivative = 0.0;
        double mean_motion_second_derivative = 0.0;
        double b_star_first_guess = 1e-5;

        TimeScale utc = TimeScalesFactory.getUTC();
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Frame gcrf = FramesFactory.getGCRF();

        AbsoluteDate launchDate = new AbsoluteDate(2020, 9, 28, 11, 20, 32.0, utc);
        AbsoluteDate separationDate = launchDate.shiftedBy(12340.0);

        Frame launchFrame = itrf.getFrozenFrame(gcrf, launchDate, "launchFrame");

        Vector3D pos_init = new Vector3D(6048.107, 3025.385, 1585.042).scalarMultiply(1e3);
        Vector3D vel_init = new Vector3D(-1.068882, -1.694130, 7.304083).scalarMultiply(1e3);
        PVCoordinates pvInit = new PVCoordinates(pos_init, vel_init);

        CartesianOrbit initialOrbit = new CartesianOrbit(pvInit, launchFrame, separationDate,
                Constants.EIGEN5C_EARTH_MU);

        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);

        ArrayList<SpacecraftState> states_list = new ArrayList<SpacecraftState>();

        double dt = 60.0;
        AbsoluteDate date_current = separationDate;

        while (date_current.compareTo(separationDate.shiftedBy(24 * 3600.0)) <= 0) {
            states_list.add(propagator.propagate(date_current));
            date_current = date_current.shiftedBy(dt);
        }

        TLE tle_first_guess = new TLE(satellite_number, classification, launch_year, launch_number, launch_piece,
                ephemeris_type, element_number, initialOrbit.getDate(), initialOrbit.getKeplerianMeanMotion(),
                mean_motion_first_derivative, mean_motion_second_derivative, initialOrbit.getE(), initialOrbit.getI(),
                0.0, 0.0, 0.0, revolution_number, b_star_first_guess);

        double threshold = 1.0;
        TLEPropagatorBuilder tle_builder = new TLEPropagatorBuilder(tle_first_guess, PositionAngle.MEAN, 1.0);
        FiniteDifferencePropagatorConverter fitter = new FiniteDifferencePropagatorConverter(tle_builder, threshold,
                10000);
        fitter.convert(states_list, false, "BSTAR");
        TLEPropagator tle_propagator = (TLEPropagator) fitter.getAdaptedPropagator();
        TLE tle_fitted = tle_propagator.getTLE();
        
        Assert.assertEquals(0.001, tle_fitted.getE(), 1e-4);
        Assert.assertEquals(FastMath.toRadians(97.6), tle_fitted.getI(), 1e-1);
        Assert.assertEquals(15.0*2*FastMath.PI/86400.0, tle_fitted.getMeanMotion(), 1e-3);
    }
}

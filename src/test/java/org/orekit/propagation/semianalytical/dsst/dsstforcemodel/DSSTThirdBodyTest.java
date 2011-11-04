package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import java.io.IOException;
import java.text.ParseException;

import org.apache.commons.math.util.FastMath;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;

public class DSSTThirdBodyTest {

    @Test
    public void testMeanElementRateForTheMoon() throws OrekitException, IOException, ParseException {

        final DSSTThirdBody force = new DSSTThirdBody(CelestialBodyFactory.getMoon());

        // elliptic orbit
        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double inc = 2 * FastMath.asin(FastMath.sqrt((ix * ix + iy * iy) / 4.));
        double hx = FastMath.tan(inc / 2.) * ix / (2 * FastMath.sin(inc / 2.));
        double hy = FastMath.tan(inc / 2.) * iy / (2 * FastMath.sin(inc / 2.));

        Orbit orbit = new EquinoctialOrbit(42166.712, 0.5, -0.5, hx, hy, 5.300,
                                           PositionAngle.MEAN, FramesFactory.getEME2000(),
                                           new AbsoluteDate(new DateComponents(2003, 03, 21),
                                                            new TimeComponents(1, 0, 0.),
                                                            TimeScalesFactory.getUTC()),
                                           CelestialBodyFactory.getEarth().getGM());
        final SpacecraftState state = new SpacecraftState(orbit);

        final double[] daidt = force.getMeanElementRate(state);

        for (int i = 0; i < daidt.length; i++){
            System.out.println(daidt[i]);
        }

    }

    @Test
    public void testMeanElementRateForTheSun() throws OrekitException, IOException, ParseException {

        final DSSTThirdBody force = new DSSTThirdBody(CelestialBodyFactory.getSun());

        // elliptic orbit
        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double inc = 2 * FastMath.asin(FastMath.sqrt((ix * ix + iy * iy) / 4.));
        double hx = FastMath.tan(inc / 2.) * ix / (2 * FastMath.sin(inc / 2.));
        double hy = FastMath.tan(inc / 2.) * iy / (2 * FastMath.sin(inc / 2.));

        Orbit orbit = new EquinoctialOrbit(42166.712, 0.5, -0.5, hx, hy, 5.300,
                                           PositionAngle.MEAN, FramesFactory.getEME2000(),
                                           new AbsoluteDate(new DateComponents(2003, 03, 21),
                                                            new TimeComponents(1, 0, 0.),
                                                            TimeScalesFactory.getUTC()),
                                           CelestialBodyFactory.getEarth().getGM());
        final SpacecraftState state = new SpacecraftState(orbit);

        final double[] daidt = force.getMeanElementRate(state);

        for (int i = 0; i < daidt.length; i++){
            System.out.println(daidt[i]);
        }

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

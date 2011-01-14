package org.orekit.propagation.precomputed;

import junit.framework.Assert;

import org.apache.commons.math.util.FastMath;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.events.EclipseDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;

public class EphemerisEventsTest {

    private AbsoluteDate initDate;
    private AbsoluteDate finalDate;
    private int inEclipsecounter;
    private int outEclipsecounter;

    private Ephemeris buildEphem() throws IllegalArgumentException, OrekitException {

        double mass = 2500;
        double a = 7187990.1979844316;
        double e = 0.5e-4;
        double i = 1.7105407051081795;
        double omega = 1.9674147913622104;
        double OMEGA = FastMath.toRadians(261);
        double lv = 0;
        double mu  = 3.9860047e14;
        double ae  = 6.378137e6;
        double c20 = -1.08263e-3;
        double c30 = 2.54e-6;
        double c40 = 1.62e-6;
        double c50 = 2.3e-7;
        double c60 = -5.5e-7;

        double deltaT = finalDate.durationFrom(initDate);

        Orbit transPar = new KeplerianOrbit(a, e, i, omega, OMEGA,
                                            lv, KeplerianOrbit.TRUE_ANOMALY, 
                                            FramesFactory.getEME2000(), initDate, mu);

        int nbIntervals = 720;
        EcksteinHechlerPropagator eck =
            new EcksteinHechlerPropagator(transPar, mass,
                                          ae, mu, c20, c30, c40, c50, c60);

        SpacecraftState[] tab = new SpacecraftState[nbIntervals+1];
        for (int j = 0; j<= nbIntervals; j++) {
            AbsoluteDate current = initDate.shiftedBy((j * deltaT) / nbIntervals);
            tab[j] = eck.propagate(current);
        }

        return new Ephemeris(tab);
    }

    private EclipseDetector buildEclipsDetector() throws OrekitException {

        double sunRadius = 696000000.;
        double earthRadius = 6400000.;

        EclipseDetector ecl = new EclipseDetector(60., 1.e-3,
                                                  CelestialBodyFactory.getSun(), sunRadius,
                                                  CelestialBodyFactory.getEarth(), earthRadius) {
            private static final long serialVersionUID = 1L;
            public int eventOccurred(SpacecraftState s, boolean increasing) throws OrekitException {
                if (increasing) {
                    ++inEclipsecounter;
                } else {
                    ++outEclipsecounter;
                }
                return CONTINUE;
            }
        };

        return ecl;
    }

    @Test
    public void testEphem() throws IllegalArgumentException, OrekitException {

        initDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                    TimeComponents.H00,
                                    TimeScalesFactory.getUTC());

        finalDate = new AbsoluteDate(new DateComponents(2004, 01, 02),
                                     TimeComponents.H00,
                                     TimeScalesFactory.getUTC());



        BoundedPropagator ephem = buildEphem();

        ephem.addEventDetector(buildEclipsDetector());

        AbsoluteDate computeEnd = new AbsoluteDate(finalDate, -1000.0);

        ephem.setSlaveMode();
        SpacecraftState state = ephem.propagate(computeEnd);
        Assert.assertEquals(computeEnd, state.getDate());
        Assert.assertEquals(14, inEclipsecounter);
        Assert.assertEquals(14, outEclipsecounter);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        inEclipsecounter = 0;
        outEclipsecounter = 0;
    }

}

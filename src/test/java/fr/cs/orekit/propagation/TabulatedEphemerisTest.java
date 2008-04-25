package fr.cs.orekit.propagation;

import java.text.ParseException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.errors.PropagationException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.orbits.KeplerianParameters;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.orbits.OrbitalParameters;
import fr.cs.orekit.propagation.analytical.EcksteinHechlerPropagator;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.time.ChunkedDate;
import fr.cs.orekit.time.ChunkedTime;
import fr.cs.orekit.time.UTCScale;


public class TabulatedEphemerisTest extends TestCase {

    public void testInterpolation() throws ParseException, OrekitException {

        double mass = 2500;
        double a = 7187990.1979844316;
        double e = 0.5e-4;
        double i = 1.7105407051081795;
        double omega = 1.9674147913622104;
        double OMEGA = Math.toRadians(261);
        double lv = 0;

        OrbitalParameters transPar = new KeplerianParameters(a, e, i,
                                                             omega, OMEGA,
                                                             lv, KeplerianParameters.TRUE_ANOMALY, Frame.getJ2000());

        AbsoluteDate initDate = new AbsoluteDate(new ChunkedDate(2004, 01, 01),
                                                 ChunkedTime.H00,
                                                 UTCScale.getInstance());
        AbsoluteDate finalDate = new AbsoluteDate(new ChunkedDate(2004, 01, 02),
                                                  ChunkedTime.H00,
                                                  UTCScale.getInstance());
        double deltaT = finalDate.minus(initDate);

        int nbIntervals = 720;
        EcksteinHechlerPropagator eck =
            new EcksteinHechlerPropagator(new SpacecraftState(new Orbit(initDate, transPar), mass),
                                          ae, mu, c20, c30, c40, c50, c60);
        SpacecraftState[] tab = new SpacecraftState[nbIntervals+1];
        for (int j = 0; j<= nbIntervals; j++) {
            AbsoluteDate current = new AbsoluteDate(initDate, (j * deltaT) / nbIntervals);
            tab[j] = eck.getSpacecraftState(current);
        }

        TabulatedEphemeris te = new TabulatedEphemeris(tab);

        assertEquals(te.getMaxDate(), finalDate);
        assertEquals(te.getMinDate(), initDate);

        checkEphemerides(eck, te, new AbsoluteDate(initDate, 3600),  0, true);
        checkEphemerides(eck, te, new AbsoluteDate(initDate, 3660), 30, false);
        checkEphemerides(eck, te, new AbsoluteDate(initDate, 3720),  0, true);

    }

    private void checkEphemerides(Ephemeris eph1, Ephemeris eph2, AbsoluteDate date,
                                  double threshold, boolean expectedBelow)
        throws PropagationException {
        SpacecraftState state1 = eph1.getSpacecraftState(date);
        SpacecraftState state2 = eph2.getSpacecraftState(date);
        double maxError = Math.abs(state1.getA() - state2.getA());
        maxError = Math.max(maxError, Math.abs(state1.getEx() - state2.getEx()));
        maxError = Math.max(maxError, Math.abs(state1.getEy() - state2.getEy()));
        maxError = Math.max(maxError, Math.abs(state1.getHx() - state2.getHx()));
        maxError = Math.max(maxError, Math.abs(state1.getHy() - state2.getHy()));
        maxError = Math.max(maxError, Math.abs(state1.getLv() - state2.getLv()));
        if (expectedBelow) {
            assertTrue(maxError <= threshold);
        } else {
            assertTrue(maxError >= threshold);
        }
    }

    public void setUp() {
        mu  = 3.9860047e14;
        ae  = 6.378137e6;
        c20 = -1.08263e-3;
        c30 = 2.54e-6;
        c40 = 1.62e-6;
        c50 = 2.3e-7;
        c60 = -5.5e-7;
    }

    public void tearDown() {
        mu  = Double.NaN;
        ae  = Double.NaN;
        c20 = Double.NaN;
        c30 = Double.NaN;
        c40 = Double.NaN;
        c50 = Double.NaN;
        c60 = Double.NaN;
    }

    private double mu;
    private double ae;
    private double c20;
    private double c30;
    private double c40;
    private double c50;
    private double c60;

    public static Test suite() {
        return new TestSuite(TabulatedEphemerisTest.class);
    }

}

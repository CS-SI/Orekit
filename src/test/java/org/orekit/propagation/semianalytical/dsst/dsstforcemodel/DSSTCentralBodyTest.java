package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import java.io.IOException;
import java.text.ParseException;

import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.apache.commons.math.util.FastMath;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.PotentialCoefficientsProvider;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

public class DSSTCentralBodyTest {

    public FirstOrderIntegrator integrator;

    public double               ae;

    public Orbit                spotOrbit;
    public Orbit                geoOrbit;
    public AbsoluteDate         dateOrbit;

    public AbsoluteDate         dateFin;

    public double               integratorStep;

    @Test
    public void test() throws IOException, ParseException, OrekitException {

        PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        double[][] Cnm = provider.getC(5, 5, true);
        double[][] Snm = provider.getS(5, 5, true);

        DSSTPropagator dsstModel = new DSSTPropagator(integrator, spotOrbit);

        dsstModel.addForceModel(new DSSTCentralBody(ae, Cnm, Snm, null));
        AbsoluteDate currentDate = dateOrbit;

        while (currentDate.compareTo(dateFin) < 0d) {
            currentDate = currentDate.shiftedBy(100);
            SpacecraftState state = dsstModel.propagate(currentDate);
            System.out.println(new KeplerianOrbit(state.getPVCoordinates(), state.getFrame(), state.getDate(), state.getMu()).getRightAscensionOfAscendingNode());

        }
    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:potential/shm-format");
        // double[] absTolerance = { 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001};
        // double[] relTolerance = { 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7};
        //
        //
        // integrator = new DormandPrince853Integrator(0.001, 200, absTolerance, relTolerance);
        dateOrbit = AbsoluteDate.J2000_EPOCH;
        dateFin = dateOrbit.shiftedBy(86400d);
        final CelestialBody earth = CelestialBodyFactory.getEarth();
        final double mu = earth.getGM();
        spotOrbit = new KeplerianOrbit(800000.0, 1.0e-4, Math.toRadians(98.7), Math.toRadians(180), Math.toRadians(0), 0d, PositionAngle.MEAN, FramesFactory.getEME2000(), dateOrbit, mu);

        /** geostationnary orbit */
        double a = 42166712;
        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double inc = 2 * FastMath.asin(FastMath.sqrt((ix * ix + iy * iy) / 4.));
        double hx = FastMath.tan(inc / 2.) * ix / (2 * FastMath.sin(inc / 2.));
        double hy = FastMath.tan(inc / 2.) * iy / (2 * FastMath.sin(inc / 2.));
        geoOrbit = new EquinoctialOrbit(a, 1e-4, 2e-4, hx, hy, 0, PositionAngle.MEAN, FramesFactory.getEME2000(), dateOrbit, mu);

        integratorStep = 1000.;
        integrator = new ClassicalRungeKuttaIntegrator(integratorStep);
        ae = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
    }

}

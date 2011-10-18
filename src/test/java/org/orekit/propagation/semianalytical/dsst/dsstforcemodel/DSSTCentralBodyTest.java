package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import java.io.IOException;
import java.text.ParseException;

import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.apache.commons.math.ode.nonstiff.DormandPrince853Integrator;
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
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

public class DSSTCentralBodyTest {
    
    public FirstOrderIntegrator integrator;
    
    public double ae;

    @Test
    public void test() throws IOException, ParseException, OrekitException {

        PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        double[][] Cnm = provider.getC(5, 5, true);
        double[][] Snm = provider.getS(5, 5, true);

        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double inc = 2 * FastMath.asin(FastMath.sqrt((ix * ix + iy * iy) / 4.));
        double hx = FastMath.tan(inc / 2.) * ix / (2 * FastMath.sin(inc / 2.));
        double hy = FastMath.tan(inc / 2.) * iy / (2 * FastMath.sin(inc / 2.));

        // elliptic orbit
        // Computation date
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final CelestialBody earth = CelestialBodyFactory.getEarth();
        final double mu = earth.getGM();

        Orbit equi = new EquinoctialOrbit(42166.712, 0.5, -0.5, hx, hy, 5.300, PositionAngle.MEAN, FramesFactory.getEME2000(), date, mu);
        
        DSSTPropagator dsstModel = new DSSTPropagator(integrator, equi);

        dsstModel.addForceModel(new DSSTCentralBody(ae, Cnm, Snm, null));
        for (int i = 0; i < 100; i++){
            System.out.println(dsstModel.propagate(date.shiftedBy(i * 10)).getOrbit());

        }

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data:potential/shm-format");
//        double[] absTolerance = { 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001};
//        double[] relTolerance = { 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7};
//        
//        
//        integrator = new DormandPrince853Integrator(0.001, 200, absTolerance, relTolerance);
        
        
        final double step = 100.;
        integrator = new ClassicalRungeKuttaIntegrator(step);
        ae = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
    }

}

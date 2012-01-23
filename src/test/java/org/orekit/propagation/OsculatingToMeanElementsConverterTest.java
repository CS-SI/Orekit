package org.orekit.propagation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math.ode.nonstiff.DormandPrince853Integrator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.SphericalSpacecraft;
import org.orekit.forces.drag.Atmosphere;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.SimpleExponentialAtmosphere;
import org.orekit.forces.gravity.CunninghamAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.PotentialCoefficientsProvider;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.OsculatingToMeanElementsConverter;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTCentralBody;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTThirdBody;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.OrbitFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinatesProvider;


public class OsculatingToMeanElementsConverterTest {

    private PotentialCoefficientsProvider provider;
    private double                        mu;

    private double                        ae;
    private NumericalPropagator           propaNUM;

    private List<DSSTForceModel>          list = new ArrayList<DSSTForceModel>();

    private final static double           eps  = 1E-15;

    /**
     * Test averaging process with two body propagator.
     * 
     * @throws Exception
     */
    @Test
    public void testTwoBodyPropagator() throws Exception {
        // low earth orbit :
        AbsoluteDate date = new AbsoluteDate("2011-12-12T11:57:20.000", TimeScalesFactory.getUTC());
        Orbit orbit1 = new CircularOrbit(7204535.848109436, -4.484755873986251E-4, 0.0011562979012178316, Math.toRadians(98.74341600466741), Math.toRadians(43.32990110790338), Math.toRadians(180.0), PositionAngle.MEAN, FramesFactory.getGCRF(), date, mu);

        setNumProp(new SpacecraftState(orbit1));

        // No force model
        OsculatingToMeanElementsConverter converter = new OsculatingToMeanElementsConverter(new SpacecraftState(orbit1), 15, propaNUM);
        SpacecraftState meanOrbit = converter.convert();

        Assert.assertEquals(orbit1.getA(), meanOrbit.getA(), eps);
        Assert.assertEquals(orbit1.getEquinoctialEx(), meanOrbit.getEquinoctialEx(), 5e-7);
        Assert.assertEquals(orbit1.getEquinoctialEy(), meanOrbit.getEquinoctialEy(), 2.5e-7);
        Assert.assertEquals(orbit1.getHx(), meanOrbit.getHx(), 3.8e-4);
        Assert.assertEquals(orbit1.getHy(), meanOrbit.getHy(), 3.7e-4);
        Assert.assertEquals(orbit1.getLM(), meanOrbit.getLM(), 3e-3);
    }

    /**
     * Test a low earth orbit averaging. Compare the result with another method,
     * {@link OrbitFactory#getMeanOrbitFromOsculating(org.orekit.propagation.Propagator, double, int)}
     * .
     * 
     * @throws Exception
     */
    @Test
    public void testLowEarthOrbit() throws Exception {
        boolean centralBody = true;
        int degree = 5;
        int order = 5;
        boolean radiationPressure = false;
        boolean drag = true;
        boolean moon = false;
        boolean sun = false;

        AbsoluteDate date = new AbsoluteDate("2011-12-12T11:57:20.000", TimeScalesFactory.getUTC());

        Orbit orbit1 = new CircularOrbit(7204535.848109436, -4.484755873986251E-4, 0.0011562979012178316, Math.toRadians(98.74341600466741), Math.toRadians(43.32990110790338), Math.toRadians(180.0), PositionAngle.MEAN, FramesFactory.getGCRF(), date, mu);

        setNumProp(new SpacecraftState(orbit1));
        setForceModel(centralBody, degree, order, radiationPressure, drag, moon, sun);
        double[] tolerance = new double[] { 45, 3.5e-4, 3.5e-4, 9.5e-4, 9.5e-4, 5e-3 };
        checkResult(orbit1, tolerance);

    }

    /**
     * Test a geostationnary orbit averaging. Compare the result with another method,
     * {@link OrbitFactory#getMeanOrbitFromOsculating(org.orekit.propagation.Propagator, double, int)}
     * 
     * @throws Exception
     */
    @Test
    public void testGeostationaryOrbit() throws Exception {
        boolean centralBody = true;
        int degree = 5;
        int order = 5;
        boolean radiationPressure = false;
        boolean drag = false;
        boolean moon = false;
        boolean sun = false;

        AbsoluteDate date = new AbsoluteDate("2011-10-23T00:00:00.000", TimeScalesFactory.getUTC());
        Orbit orbit1 = new EquinoctialOrbit(42168449.623, -6.66e-5, 2.17e-5, 0.0027459, -0.0015674, 0.153225665, PositionAngle.MEAN, FramesFactory.getEME2000(), date, mu);

        setNumProp(new SpacecraftState(orbit1));
        setForceModel(centralBody, degree, order, radiationPressure, drag, moon, sun);

        double[] tolerance = new double[] { 22, 3e-5, 2.5e-5, 4.5e-7, 2e-7, 7.5e-4 };

        // Check result
        checkResult(orbit1, tolerance);
    }

    /**
     * Check result
     * 
     * @param orbit1
     * @param tolerance
     * @throws Exception
     */
    private void checkResult(Orbit orbit1,
                             double[] tolerance) throws Exception {

        SpacecraftState[] state1 = OrbitFactory.getMeanOrbitFromOsculating(propaNUM, 2 * orbit1.getKeplerianPeriod(), 10);

        SpacecraftState meanOrbit1 = state1[0];
        AbsoluteDate dateMean = meanOrbit1.getDate();

        SpacecraftState state2 = propaNUM.propagate(dateMean);

        SpacecraftState meanOrbit2 = new OsculatingToMeanElementsConverter(state2, 2, propaNUM).convert();

        Assert.assertEquals(meanOrbit2.getA(), meanOrbit1.getA(), tolerance[0]);
        Assert.assertEquals(meanOrbit2.getEquinoctialEx(), meanOrbit1.getEquinoctialEx(), tolerance[1]);
        Assert.assertEquals(meanOrbit2.getEquinoctialEy(), meanOrbit1.getEquinoctialEy(), tolerance[2]);
        Assert.assertEquals(meanOrbit2.getHx(), meanOrbit1.getHx(), tolerance[3]);
        Assert.assertEquals(meanOrbit2.getHy(), meanOrbit1.getHy(), tolerance[4]);
        Assert.assertEquals(meanOrbit2.getLM(), meanOrbit1.getLM(), tolerance[5]);
    }

    /**
     * Set the force model used for averaging process
     * 
     * @param centralBody
     * @param degree
     * @param order
     * @param radiationPressure
     * @param drag
     * @param moon
     * @param sun
     * @throws OrekitException
     */
    private void setForceModel(boolean centralBody,
                               int degree,
                               int order,
                               boolean radiationPressure,
                               boolean drag,
                               boolean moon,
                               boolean sun) throws OrekitException {

        if (centralBody) {
            // Central Body Force Model with un-normalized coefficients
            double[][] CnmNotNorm;
            double[][] SnmNotNorm;
            CnmNotNorm = provider.getC(degree, order, false);
            SnmNotNorm = provider.getS(degree, order, false);

            // DSST force model parameters
            DSSTForceModel centralBodyDSST = new DSSTCentralBody(Constants.WGS84_EARTH_ANGULAR_VELOCITY, ae, mu, CnmNotNorm, CnmNotNorm, null);
            ForceModel centralBodyNUM = new CunninghamAttractionModel(FramesFactory.getITRF2005(), ae, mu, CnmNotNorm, SnmNotNorm);
            list.add(centralBodyDSST);
            propaNUM.addForceModel(centralBodyNUM);
        }

        if (sun) {
            DSSTForceModel sunDSST = new DSSTThirdBody(CelestialBodyFactory.getSun());
            ForceModel sunNUM = new ThirdBodyAttraction(CelestialBodyFactory.getSun());
            list.add(sunDSST);
            propaNUM.addForceModel(sunNUM);
        }

        if (moon) {
            DSSTForceModel moonDSST = new DSSTThirdBody(CelestialBodyFactory.getMoon());
            ForceModel moonNUM = new ThirdBodyAttraction(CelestialBodyFactory.getMoon());
            list.add(moonDSST);
            propaNUM.addForceModel(moonNUM);
        }

        if (drag) {
            // Drag Force Model
            OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF2005());
            earth.setAngularThreshold(1.e-6);
            Atmosphere atm = new SimpleExponentialAtmosphere(earth, 4.e-13, 500000.0, 60000.0);
            final double cd = 2.0;
            final double sf = 5.0;
            DSSTForceModel dragDSST = new DSSTAtmosphericDrag(atm, cd, sf);
            ForceModel dragNUM = new DragForce(atm, new SphericalSpacecraft(sf, cd, 0., 0.));
            list.add(dragDSST);
            propaNUM.addForceModel(dragNUM);
        }

        if (radiationPressure) {
            // Solar Radiation Pressure Force Model
            PVCoordinatesProvider sunBody = CelestialBodyFactory.getSun();
            double sf = 5.0;
            double kA = 0.5;
            double kR = 0.5;
            double cR = 2. * (1. + (1. - kA) * (1. - kR) * 4. / 9.);
            // DSST radiation pressure force
            DSSTForceModel pressureDSST = new DSSTSolarRadiationPressure(cR, sf, sunBody, ae);
            // NUMERICAL radiation pressure force
            SphericalSpacecraft spc = new SphericalSpacecraft(sf, 0., kA, kR);
            ForceModel pressureNUM = new SolarRadiationPressure(sunBody, ae, spc);
            list.add(pressureDSST);
            propaNUM.addForceModel(pressureNUM);
        }
    }

    /**
     * Set up the numerical propagator
     * 
     * @param initialState
     */
    private void setNumProp(SpacecraftState initialState) {
        final double[][] tol = NumericalPropagator.tolerances(1.0, initialState.getOrbit(), initialState.getOrbit().getType());
        final double minStep = 1.;
        final double maxStep = 200.;
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
        integrator.setInitialStepSize(100.);
        propaNUM = new NumericalPropagator(integrator);
        propaNUM.setInitialState(initialState);
    }

    @Before
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data:potential/shm-format");
        provider = GravityFieldFactory.getPotentialProvider();
        mu = provider.getMu();
        ae = provider.getAe();
    }

}

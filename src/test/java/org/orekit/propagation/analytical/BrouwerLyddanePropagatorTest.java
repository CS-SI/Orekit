package org.orekit.propagation.analytical;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class BrouwerLyddanePropagatorTest {

	private static final AttitudeProvider DEFAULT_LAW = Utils.defaultLaw();

	@Test
	public void sameDateCartesian() {

        // Definition of initial conditions with position and velocity
        // ------------------------------------------------------------
		// e = 0.04152500499523033   and   i = 1.705015527659039

		AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Vector3D position = new Vector3D(3220103., 69623., 6149822.);
        Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                  FramesFactory.getEME2000(), initDate, provider.getMu());

        // Extrapolation at the initial date
        // ---------------------------------
        BrouwerLyddanePropagator extrapolator =
	            new BrouwerLyddanePropagator(initialOrbit, GravityFieldFactory.getUnnormalizedProvider(provider));
        SpacecraftState finalOrbit = extrapolator.propagate(initDate);

        // positions  velocity and semi major axis match perfectly
        Assert.assertEquals(0.0,
                            Vector3D.distance(initialOrbit.getPVCoordinates().getPosition(),
                                              finalOrbit.getPVCoordinates().getPosition()),
                            1.0e-8);

        Assert.assertEquals(0.0,
                            Vector3D.distance(initialOrbit.getPVCoordinates().getVelocity(),
                                              finalOrbit.getPVCoordinates().getVelocity()),
                            1.0e-11);
        Assert.assertEquals(0.0, finalOrbit.getA() - initialOrbit.getA(), 0.0);

	}

	@Test
	public void sameDateKeplerian() {

		// Definition of initial conditions with position and velocity
        // ------------------------------------------------------------
		AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
		Orbit initialOrbit = new KeplerianOrbit(6767924.41, .0005,  1.7, 2.1, 2.9,
                6.2, PositionAngle.TRUE,
                FramesFactory.getEME2000(), initDate, provider.getMu());

        BrouwerLyddanePropagator extrapolator =
	            new BrouwerLyddanePropagator(initialOrbit, DEFAULT_LAW, GravityFieldFactory.getUnnormalizedProvider(provider));

        SpacecraftState finalOrbit = extrapolator.propagate(initDate);

        // positions  velocity and semi major axis match perfectly
        Assert.assertEquals(0.0,
                            Vector3D.distance(initialOrbit.getPVCoordinates().getPosition(),
                                              finalOrbit.getPVCoordinates().getPosition()),
                            1.0e-8);

        Assert.assertEquals(0.0,
                            Vector3D.distance(initialOrbit.getPVCoordinates().getVelocity(),
                                              finalOrbit.getPVCoordinates().getVelocity()),
                            1.0e-11);
        Assert.assertEquals(0.0, finalOrbit.getA() - initialOrbit.getA(), 0.0);
	}


    @Test
    public void almostSphericalBody() {

        // Definition of initial conditions
        // ---------------------------------
        // with e around e = 1.4e-4 and i = 1.7 rad
        Vector3D position = new Vector3D(3220103., 69623., 8449822.);
        Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                  FramesFactory.getEME2000(), initDate, provider.getMu());

        // Initialisation to simulate a Keplerian extrapolation
        // To be noticed: in order to simulate a Keplerian extrapolation with the
        // analytical
        // extrapolator, one should put the zonal coefficients to 0. But due to
        // numerical pbs
        // one must put a non 0 value.
        UnnormalizedSphericalHarmonicsProvider kepProvider =
                GravityFieldFactory.getUnnormalizedProvider(6.378137e6, 3.9860047e14,
                                                            TideSystem.UNKNOWN,
                                                            new double[][] {
                                                                { 0 }, { 0 }, { 0.1e-10 }, { 0.1e-13 }, { 0.1e-13 }, { 0.1e-14 }, { 0.1e-14 }
                                                            }, new double[][] {
                                                                { 0 }, { 0 },  { 0 }, { 0 }, { 0 }, { 0 }, { 0 }
                                                            });

        // Extrapolators definitions
        // -------------------------
        BrouwerLyddanePropagator extrapolatorAna =
            new BrouwerLyddanePropagator(initialOrbit, 1000.0, kepProvider);
        KeplerianPropagator extrapolatorKep = new KeplerianPropagator(initialOrbit);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        double delta_t = 100.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);

        SpacecraftState finalOrbitAna = extrapolatorAna.propagate(extrapDate);
        SpacecraftState finalOrbitKep = extrapolatorKep.propagate(extrapDate);

        Assert.assertEquals(finalOrbitAna.getDate().durationFrom(extrapDate), 0.0,
                     Utils.epsilonTest);
        // comparison of each orbital parameters
        Assert.assertEquals(finalOrbitAna.getA(), finalOrbitKep.getA(), 10
                     * Utils.epsilonTest * finalOrbitKep.getA());
        Assert.assertEquals(finalOrbitAna.getEquinoctialEx(), finalOrbitKep.getEquinoctialEx(), Utils.epsilonE
                     * finalOrbitKep.getE());
        Assert.assertEquals(finalOrbitAna.getEquinoctialEy(), finalOrbitKep.getEquinoctialEy(), Utils.epsilonE
                     * finalOrbitKep.getE());
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getHx(), finalOrbitKep.getHx()),
                     finalOrbitKep.getHx(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getHy(), finalOrbitKep.getHy()),
                     finalOrbitKep.getHy(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLv(), finalOrbitKep.getLv()),
                     finalOrbitKep.getLv(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getLv()));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLE(), finalOrbitKep.getLE()),
                     finalOrbitKep.getLE(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getLE()));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLM(), finalOrbitKep.getLM()),
                     finalOrbitKep.getLM(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getLM()));

    }




	@Test
	public void compareToNumericalPropagation() {

        final Frame inertialFrame = FramesFactory.getEME2000();
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        double timeshift = 60000. ;

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = 0; // mean anomaly
        final Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.TRUE,
                                                      inertialFrame, initDate, provider.getMu());
        // Initial state definition
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);

        //_______________________________________________________________________________________________
        // SET UP A REFERENCE NUMERICAL PROPAGATION
        //_______________________________________________________________________________________________

        // Adaptive step integrator with a minimum step of 0.001 and a maximum step of 1000
        final double minStep = 0.001;
        final double maxstep = 1000.0;
        final double positionTolerance = 10.0;
        final OrbitType propagationType = OrbitType.KEPLERIAN;
        final double[][] tolerances =
                NumericalPropagator.tolerances(positionTolerance, initialOrbit, propagationType);
        final AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(minStep, maxstep, tolerances[0], tolerances[1]);

        // Numerical Propagator
        final NumericalPropagator NumPropagator = new NumericalPropagator(integrator);
        NumPropagator.setOrbitType(propagationType);

        final ForceModel holmesFeatherstone =
                new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);
        NumPropagator.addForceModel(holmesFeatherstone);

        // Set up initial state in the propagator
        NumPropagator.setInitialState(initialState);

        // Extrapolate from the initial to the final date
        final SpacecraftState NumFinalState = NumPropagator.propagate(initDate.shiftedBy(timeshift));
        final KeplerianOrbit NumOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(NumFinalState.getOrbit());

        //_______________________________________________________________________________________________
        // SET UP A BROUWER LYDDANE PROPAGATION
        //_______________________________________________________________________________________________

        BrouwerLyddanePropagator BLextrapolator =
	            new BrouwerLyddanePropagator(initialOrbit, GravityFieldFactory.getUnnormalizedProvider(provider));

        SpacecraftState BLFinalState = BLextrapolator.propagate(initDate.shiftedBy(timeshift));
	    final KeplerianOrbit BLOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState.getOrbit());

	    Assert.assertEquals(NumOrbit.getA(), BLOrbit.getA(), 0.073);
	    Assert.assertEquals(NumOrbit.getE(), BLOrbit.getE(), 0.00000028);
	    Assert.assertEquals(NumOrbit.getI(), BLOrbit.getI(), 0.000004);
	    Assert.assertEquals(MathUtils.normalizeAngle(NumOrbit.getPerigeeArgument(), FastMath.PI),
	    		MathUtils.normalizeAngle(BLOrbit.getPerigeeArgument(), FastMath.PI), 0.119);
	    Assert.assertEquals(MathUtils.normalizeAngle(NumOrbit.getRightAscensionOfAscendingNode(), FastMath.PI),
	    		MathUtils.normalizeAngle(BLOrbit.getRightAscensionOfAscendingNode(), FastMath.PI), 0.000072);
	    Assert.assertEquals(MathUtils.normalizeAngle(NumOrbit.getTrueAnomaly(), FastMath.PI),
	    		MathUtils.normalizeAngle(BLOrbit.getTrueAnomaly(), FastMath.PI), 0.12);
	}


	@Test
	public void compareToNumericalPropagationMeanInitialOrbit() {

        final Frame inertialFrame = FramesFactory.getEME2000();
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        double timeshift = 60000. ;

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = 0; // mean anomaly
        final Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.TRUE,
                                                      inertialFrame, initDate, provider.getMu());


        BrouwerLyddanePropagator BLextrapolator =
	            new BrouwerLyddanePropagator(initialOrbit, GravityFieldFactory.getUnnormalizedProvider(provider),
	            		                     PropagationType.MEAN);
        SpacecraftState initialOsculatingState = BLextrapolator.propagate(initDate);
        final KeplerianOrbit InitOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(initialOsculatingState.getOrbit());

        SpacecraftState BLFinalState = BLextrapolator.propagate(initDate.shiftedBy(timeshift));
        final KeplerianOrbit BLOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState.getOrbit());

        //_______________________________________________________________________________________________
        // SET UP A REFERENCE NUMERICAL PROPAGATION
        //_______________________________________________________________________________________________


        // Adaptive step integrator with a minimum step of 0.001 and a maximum step of 1000
        final double minStep = 0.001;
        final double maxstep = 1000.0;
        final double positionTolerance = 10.0;
        final OrbitType propagationType = OrbitType.KEPLERIAN;
        final double[][] tolerances =
                NumericalPropagator.tolerances(positionTolerance, InitOrbit, propagationType);
        final AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(minStep, maxstep, tolerances[0], tolerances[1]);

        // Numerical Propagator
        final NumericalPropagator NumPropagator = new NumericalPropagator(integrator);
        NumPropagator.setOrbitType(propagationType);

        final ForceModel holmesFeatherstone =
                new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);
        NumPropagator.addForceModel(holmesFeatherstone);

        // Set up initial state in the propagator
        NumPropagator.setInitialState(initialOsculatingState);

        // Extrapolate from the initial to the final date
        final SpacecraftState NumFinalState = NumPropagator.propagate(initDate.shiftedBy(timeshift));
        final KeplerianOrbit NumOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(NumFinalState.getOrbit());

        //_______________________________________________________________________________________________
        // SET UP A BROUWER LYDDANE PROPAGATION
        //_______________________________________________________________________________________________

	    Assert.assertEquals(NumOrbit.getA(), BLOrbit.getA(), 0.17);
	    Assert.assertEquals(NumOrbit.getE(), BLOrbit.getE(), 0.00000028);
	    Assert.assertEquals(NumOrbit.getI(), BLOrbit.getI(), 0.000004);
	    Assert.assertEquals(MathUtils.normalizeAngle(NumOrbit.getPerigeeArgument(), FastMath.PI),
	    		MathUtils.normalizeAngle(BLOrbit.getPerigeeArgument(), FastMath.PI), 0.197);
	    Assert.assertEquals(MathUtils.normalizeAngle(NumOrbit.getRightAscensionOfAscendingNode(), FastMath.PI),
	    		MathUtils.normalizeAngle(BLOrbit.getRightAscensionOfAscendingNode(), FastMath.PI), 0.00072);
	    Assert.assertEquals(MathUtils.normalizeAngle(NumOrbit.getTrueAnomaly(), FastMath.PI),
	    		MathUtils.normalizeAngle(BLOrbit.getTrueAnomaly(), FastMath.PI), 0.12);

	}


	@Test
	public void compareToNumericalPropagationResetInitialIntermediate() {

        final Frame inertialFrame = FramesFactory.getEME2000();
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        double timeshift = 60000.;

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = 0; // mean anomaly
        final Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.TRUE,
                                                      inertialFrame, initDate, provider.getMu());
        // Initial state definition
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);

        //_______________________________________________________________________________________________
        // SET UP A BROUWER LYDDANE PROPAGATOR
        //_______________________________________________________________________________________________

        BrouwerLyddanePropagator BLextrapolator1 =
	            new BrouwerLyddanePropagator(initialOrbit, DEFAULT_LAW, Propagator.DEFAULT_MASS, GravityFieldFactory.getUnnormalizedProvider(provider),
	            		                     PropagationType.OSCULATING);
        //_______________________________________________________________________________________________
        // SET UP ANOTHER BROUWER LYDDANE PROPAGATOR
        //_______________________________________________________________________________________________

        BrouwerLyddanePropagator BLextrapolator2 =
	            new BrouwerLyddanePropagator( new KeplerianOrbit(a + 3000, e + 0.001, i - FastMath.toRadians(12.0), omega, raan, lM, PositionAngle.TRUE,
                        inertialFrame, initDate, provider.getMu()),DEFAULT_LAW, Propagator.DEFAULT_MASS, GravityFieldFactory.getUnnormalizedProvider(provider));
        // Reset BL2 with BL1 initial state
        BLextrapolator2.resetInitialState(initialState);

        SpacecraftState BLFinalState1 = BLextrapolator1.propagate(initDate.shiftedBy(timeshift));
	    final KeplerianOrbit BLOrbit1 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState1.getOrbit());
        SpacecraftState BLFinalState2 = BLextrapolator2.propagate(initDate.shiftedBy(timeshift));
        BLextrapolator2.resetIntermediateState(BLFinalState1, true);
	    final KeplerianOrbit BLOrbit2 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState2.getOrbit());

	    Assert.assertEquals(BLOrbit1.getA(), BLOrbit2.getA(), 0.0);
	    Assert.assertEquals(BLOrbit1.getE(), BLOrbit2.getE(), 0.0);
	    Assert.assertEquals(BLOrbit1.getI(), BLOrbit2.getI(), 0.0);
	    Assert.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getPerigeeArgument(), FastMath.PI),
	    		MathUtils.normalizeAngle(BLOrbit2.getPerigeeArgument(), FastMath.PI), 0.0);
	    Assert.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getRightAscensionOfAscendingNode(), FastMath.PI),
	    		MathUtils.normalizeAngle(BLOrbit2.getRightAscensionOfAscendingNode(), FastMath.PI), 0.0);
	    Assert.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getTrueAnomaly(), FastMath.PI),
	    		MathUtils.normalizeAngle(BLOrbit2.getTrueAnomaly(), FastMath.PI), 0.0);

	}

	@Test
	public void compareConstructors() {

		final Frame inertialFrame = FramesFactory.getEME2000();
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        double timeshift = 600. ;

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = 0; // mean anomaly
        final Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.TRUE,
                                                      inertialFrame, initDate, provider.getMu());


        BrouwerLyddanePropagator BLPropagator1 = new BrouwerLyddanePropagator(initialOrbit, DEFAULT_LAW,
        		provider.getAe(), provider.getMu(), -1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7);
        BrouwerLyddanePropagator BLPropagator2 = new BrouwerLyddanePropagator(initialOrbit,
        		provider.getAe(), provider.getMu(), -1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7);
        BrouwerLyddanePropagator BLPropagator3 = new BrouwerLyddanePropagator(initialOrbit, Propagator.DEFAULT_MASS,
        		provider.getAe(), provider.getMu(), -1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7);

        SpacecraftState BLFinalState1 = BLPropagator1.propagate(initDate.shiftedBy(timeshift));
	    final KeplerianOrbit BLOrbit1 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState1.getOrbit());
        SpacecraftState BLFinalState2 = BLPropagator2.propagate(initDate.shiftedBy(timeshift));
	    final KeplerianOrbit BLOrbit2 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState2.getOrbit());
        SpacecraftState BLFinalState3 = BLPropagator3.propagate(initDate.shiftedBy(timeshift));
	    final KeplerianOrbit BLOrbit3 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState3.getOrbit());


	    Assert.assertEquals(BLOrbit1.getA(), BLOrbit2.getA(), 0.0);
	    Assert.assertEquals(BLOrbit1.getE(), BLOrbit2.getE(), 0.0);
	    Assert.assertEquals(BLOrbit1.getI(), BLOrbit2.getI(), 0.0);
	    Assert.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getPerigeeArgument(), FastMath.PI),
	    		MathUtils.normalizeAngle(BLOrbit2.getPerigeeArgument(), FastMath.PI), 0.0);
	    Assert.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getRightAscensionOfAscendingNode(), FastMath.PI),
	    		MathUtils.normalizeAngle(BLOrbit2.getRightAscensionOfAscendingNode(), FastMath.PI), 0.0);
	    Assert.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getTrueAnomaly(), FastMath.PI),
	    		MathUtils.normalizeAngle(BLOrbit2.getTrueAnomaly(), FastMath.PI), 0.0);
	    Assert.assertEquals(BLOrbit1.getA(), BLOrbit3.getA(), 0.0);
	    Assert.assertEquals(BLOrbit1.getE(), BLOrbit3.getE(), 0.0);
	    Assert.assertEquals(BLOrbit1.getI(), BLOrbit3.getI(), 0.0);
	    Assert.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getPerigeeArgument(), FastMath.PI),
	    		MathUtils.normalizeAngle(BLOrbit3.getPerigeeArgument(), FastMath.PI), 0.0);
	    Assert.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getRightAscensionOfAscendingNode(), FastMath.PI),
	    		MathUtils.normalizeAngle(BLOrbit3.getRightAscensionOfAscendingNode(), FastMath.PI), 0.0);
	    Assert.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getTrueAnomaly(), FastMath.PI),
	    		MathUtils.normalizeAngle(BLOrbit3.getTrueAnomaly(), FastMath.PI), 0.0);

	}


    @Test(expected = OrekitException.class)
    public void undergroundOrbit() {

        // for a semi major axis < equatorial radius
        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 800.0, 100.0);
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                  FramesFactory.getEME2000(), initDate, provider.getMu());
        // Extrapolator definition
        // -----------------------
        BrouwerLyddanePropagator extrapolator =
            new BrouwerLyddanePropagator(initialOrbit, DEFAULT_LAW, provider.getAe(), provider.getMu(),
            		-1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7);

        // Extrapolation at the initial date
        // ---------------------------------
        double delta_t = 0.0;
        AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);
        extrapolator.propagate(extrapDate);
    }


    @Test(expected = OrekitException.class)
    public void tooEllipticalOrbit() {
        // for an eccentricity too big for the model
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        Orbit initialOrbit = new KeplerianOrbit(67679244.0, 1.0,  1.85850, 2.1, 2.9,
                                 6.2, PositionAngle.TRUE, FramesFactory.getEME2000(),
                                 initDate, provider.getMu());
        // Extrapolator definition
        // -----------------------
        BrouwerLyddanePropagator extrapolator =
            new BrouwerLyddanePropagator(initialOrbit, provider.getAe(), provider.getMu(),
            		-1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7);

        // Extrapolation at the initial date
        // ---------------------------------
        double delta_t = 0.0;
        AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);
        extrapolator.propagate(extrapDate);
    }


    @Test(expected = OrekitException.class)
    public void criticalInclination() {
        // for an eccentricity too big for the model
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        Orbit initialOrbit = new KeplerianOrbit(67679244.0, 0.01, FastMath.toRadians(63.44), 2.1, 2.9,
                                 6.2, PositionAngle.TRUE, FramesFactory.getEME2000(),
                                 initDate, provider.getMu());
        // Extrapolator definition
        // -----------------------
        BrouwerLyddanePropagator extrapolator =
            new BrouwerLyddanePropagator(initialOrbit, DEFAULT_LAW, provider.getAe(), provider.getMu(),
            		-1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7);

        // Extrapolation at the initial date
        // ---------------------------------
        double delta_t = 0.0;
        AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);
        extrapolator.propagate(extrapDate);
    }


    @Test(expected = OrekitException.class)
    public void insideEarth() {
        // for an eccentricity too big for the model
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        Orbit initialOrbit = new KeplerianOrbit( provider.getAe()-1000.0, 0.01, FastMath.toRadians(10.0), 2.1, 2.9,
                                 6.2, PositionAngle.TRUE, FramesFactory.getEME2000(),
                                 initDate, provider.getMu());
        // Extrapolator definition
        // -----------------------
        BrouwerLyddanePropagator extrapolator =
            new BrouwerLyddanePropagator(initialOrbit, Propagator.DEFAULT_MASS, provider.getAe(), provider.getMu(),
            		-1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7);

        // Extrapolation at the initial date
        // ---------------------------------
        double delta_t = 0.0;
        AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);
        extrapolator.propagate(extrapDate);
    }



    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data:potential/icgem-format");
        provider = GravityFieldFactory.getNormalizedProvider(5, 0);
    }

    @After
    public void tearDown() {
        provider = null;
    }

    private NormalizedSphericalHarmonicsProvider provider;

}



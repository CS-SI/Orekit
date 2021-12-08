package org.orekit.propagation.analytical;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;

public class FieldBrouwerLyddanePropagatorTest {
    private static final AttitudeProvider DEFAULT_LAW = Utils.defaultLaw();

    @Test
    public void sameDateCartesian() {
        doSameDateCartesian(Decimal64Field.getInstance());
    }
	private <T extends CalculusFieldElement<T>> void doSameDateCartesian(Field<T> field) {

		T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        // Definition of initial conditions with position and velocity
        // ------------------------------------------------------------
		// e = 0.04152500499523033   and   i = 1.705015527659039

        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(3220103.), zero.add(69623.), zero.add(6149822.));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(6414.7), zero.add(-2006.), zero.add(-3180.));

        FieldOrbit<T> initialOrbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                                 FramesFactory.getEME2000(), initDate, zero.add(provider.getMu()));

        // Extrapolation at the initial date
        // ---------------------------------
        FieldBrouwerLyddanePropagator<T> extrapolator =
	            new FieldBrouwerLyddanePropagator<T>(initialOrbit, GravityFieldFactory.getUnnormalizedProvider(provider));
        FieldSpacecraftState<T> finalOrbit = extrapolator.propagate(initDate);

        // positions  velocity and semi major axis match perfectly
        Assert.assertEquals(0.0,
        		FieldVector3D.distance(initialOrbit.getPVCoordinates().getPosition(),
                                       finalOrbit.getPVCoordinates().getPosition()).getReal(),
                            1.0e-8);

        Assert.assertEquals(0.0,
        		FieldVector3D.distance(initialOrbit.getPVCoordinates().getVelocity(),
                                       finalOrbit.getPVCoordinates().getVelocity()).getReal(),
                            1.0e-11);
        Assert.assertEquals(0.0, finalOrbit.getA().getReal() - initialOrbit.getA().getReal(), 0.0);

	}


	@Test
    public void sameDateKeplerian() {
        doSameDateKeplerian(Decimal64Field.getInstance());
    }
	private <T extends CalculusFieldElement<T>> void doSameDateKeplerian(Field<T> field) {

		T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
		// Definition of initial conditions with position and velocity
        // ------------------------------------------------------------
        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(6767924.41), zero.add(.005),  zero.add(1.7),zero.add( 2.1), 
        		                                               zero.add(2.9), zero.add(6.2), PositionAngle.TRUE,
                                                               FramesFactory.getEME2000(), initDate, zero.add(provider.getMu()));

        FieldBrouwerLyddanePropagator<T> extrapolator =
	            new FieldBrouwerLyddanePropagator<T>(initialOrbit, DEFAULT_LAW, GravityFieldFactory.getUnnormalizedProvider(provider));

        FieldSpacecraftState<T> finalOrbit = extrapolator.propagate(initDate);

        // positions  velocity and semi major axis match perfectly
        Assert.assertEquals(0.0,
        		FieldVector3D.distance(initialOrbit.getPVCoordinates().getPosition(),
                                              finalOrbit.getPVCoordinates().getPosition()).getReal(),
                            1.1e-8);

        Assert.assertEquals(0.0,
        		FieldVector3D.distance(initialOrbit.getPVCoordinates().getVelocity(),
                                              finalOrbit.getPVCoordinates().getVelocity()).getReal(),
                            1.4e-11);
        Assert.assertEquals(0.0, finalOrbit.getA().getReal() - initialOrbit.getA().getReal(), 0.0);
	}


    @Test
    public void almostSphericalBody() {
        doAlmostSphericalBody(Decimal64Field.getInstance());
    }
    private <T extends CalculusFieldElement<T>> void doAlmostSphericalBody(Field<T> field) {

    	T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        // Definition of initial conditions
        // ---------------------------------
        // with e around e = 1.4e-4 and i = 1.7 rad
    	FieldVector3D<T> position = new FieldVector3D<>(zero.add(3220103.), zero.add(69623.), zero.add(8449822.));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(6414.7), zero.add(-2006.), zero.add(-3180.));
    	
    	

    	FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
    	FieldOrbit<T> initialOrbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                                 FramesFactory.getEME2000(), initDate, zero.add(provider.getMu()));

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
        FieldBrouwerLyddanePropagator<T> extrapolatorAna =
            new FieldBrouwerLyddanePropagator<>(initialOrbit, zero.add(1000.0), kepProvider);
        FieldKeplerianPropagator<T> extrapolatorKep = new FieldKeplerianPropagator<>(initialOrbit);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        double delta_t = 100.0; // extrapolation duration in seconds
        FieldAbsoluteDate<T> extrapDate = date.shiftedBy(delta_t);

        FieldSpacecraftState<T> finalOrbitAna = extrapolatorAna.propagate(extrapDate);
        FieldSpacecraftState<T> finalOrbitKep = extrapolatorKep.propagate(extrapDate);

        Assert.assertEquals(finalOrbitAna.getDate().durationFrom(extrapDate).getReal(), 0.0,
                     Utils.epsilonTest);
        // comparison of each orbital parameters
        Assert.assertEquals(finalOrbitAna.getA().getReal(), finalOrbitKep.getA().getReal(), 10
                     * Utils.epsilonTest * finalOrbitKep.getA().getReal());
        Assert.assertEquals(finalOrbitAna.getEquinoctialEx().getReal(), finalOrbitKep.getEquinoctialEx().getReal(), Utils.epsilonE
                     * finalOrbitKep.getE().getReal());
        Assert.assertEquals(finalOrbitAna.getEquinoctialEy().getReal(), finalOrbitKep.getEquinoctialEy().getReal(), Utils.epsilonE
                     * finalOrbitKep.getE().getReal());
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getHx().getReal(), finalOrbitKep.getHx().getReal()),
                     finalOrbitKep.getHx().getReal(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getI().getReal()));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getHy().getReal(), finalOrbitKep.getHy().getReal()),
                     finalOrbitKep.getHy().getReal(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getI().getReal()));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLv().getReal(), finalOrbitKep.getLv().getReal()),
                     finalOrbitKep.getLv().getReal(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getLv().getReal()));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLE().getReal(), finalOrbitKep.getLE().getReal()),
                     finalOrbitKep.getLE().getReal(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getLE().getReal()));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLM().getReal(), finalOrbitKep.getLM().getReal()),
                     finalOrbitKep.getLM().getReal(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getLM().getReal()));

    }


    @Test
    public void compareToNumericalPropagation() {
        doCompareToNumericalPropagation(Decimal64Field.getInstance());
    }
    private <T extends CalculusFieldElement<T>> void doCompareToNumericalPropagation(Field<T> field) {

    	T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final Frame inertialFrame = FramesFactory.getEME2000();
        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        double timeshift = 60000. ;

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = 0; // mean anomaly
        final FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(a), zero.add(e), zero.add(i), zero.add(omega), 
        		                                                     zero.add(raan), zero.add(lM), PositionAngle.TRUE,
                                                                     inertialFrame, initDate, zero.add(provider.getMu()));
        
        // Initial state definition
        final FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(initialOrbit);

        //_______________________________________________________________________________________________
        // SET UP A REFERENCE NUMERICAL PROPAGATION
        //_______________________________________________________________________________________________

        // Adaptive step integrator with a minimum step of 0.001 and a maximum step of 1000
        final double minStep = 0.001;
        final double maxstep = 1000.0;
        final double positionTolerance = 10.0;
        final OrbitType propagationType = OrbitType.KEPLERIAN;
        final double[][] tolerances =
                NumericalPropagator.tolerances(positionTolerance, initialOrbit.toOrbit(), propagationType);
        final AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(minStep, maxstep, tolerances[0], tolerances[1]);

        // Numerical Propagator
        final NumericalPropagator NumPropagator = new NumericalPropagator(integrator);
        NumPropagator.setOrbitType(propagationType);

        final ForceModel holmesFeatherstone =
                new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);
        NumPropagator.addForceModel(holmesFeatherstone);

        // Set up initial state in the propagator
        NumPropagator.setInitialState(initialState.toSpacecraftState());

        // Extrapolate from the initial to the final date
        final SpacecraftState NumFinalState = NumPropagator.propagate(initDate.toAbsoluteDate().shiftedBy(timeshift));
        final KeplerianOrbit NumOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(NumFinalState.getOrbit());

        //_______________________________________________________________________________________________
        // SET UP A BROUWER LYDDANE PROPAGATION
        //_______________________________________________________________________________________________

        FieldBrouwerLyddanePropagator<T> BLextrapolator =
	            new FieldBrouwerLyddanePropagator<T>(initialOrbit, GravityFieldFactory.getUnnormalizedProvider(provider));

        FieldSpacecraftState<T> BLFinalState = BLextrapolator.propagate(initDate.shiftedBy(timeshift));
        final KeplerianOrbit BLOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState.getOrbit().toOrbit());


	    Assert.assertEquals(NumOrbit.getA(), BLOrbit.getA(), 0.072);
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
        doCompareToNumericalPropagationMeanInitialOrbit(Decimal64Field.getInstance());
    }
    private <T extends CalculusFieldElement<T>> void doCompareToNumericalPropagationMeanInitialOrbit(Field<T> field) {

    	T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final Frame inertialFrame = FramesFactory.getEME2000();
        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        double timeshift = 60000. ;


     // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = 0; // mean anomaly
        final FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(a), zero.add(e), zero.add(i), zero.add(omega), 
        		                                                     zero.add(raan), zero.add(lM), PositionAngle.TRUE,
                                                                     inertialFrame, initDate, zero.add(provider.getMu()));

        FieldBrouwerLyddanePropagator<T> BLextrapolator =
	            new FieldBrouwerLyddanePropagator<>(initialOrbit, GravityFieldFactory.getUnnormalizedProvider(provider),
	            		                     PropagationType.MEAN);
        FieldSpacecraftState<T> initialOsculatingState = BLextrapolator.propagate(initDate);
        final KeplerianOrbit InitOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(initialOsculatingState.getOrbit().toOrbit());

        FieldSpacecraftState<T> BLFinalState = BLextrapolator.propagate(initDate.shiftedBy(timeshift));
        final KeplerianOrbit BLOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState.getOrbit().toOrbit());

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
        NumPropagator.setInitialState(initialOsculatingState.toSpacecraftState());

        // Extrapolate from the initial to the final date
        final SpacecraftState NumFinalState = NumPropagator.propagate(initDate.toAbsoluteDate().shiftedBy(timeshift));
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
        doCompareToNumericalPropagationResetInitialIntermediate(Decimal64Field.getInstance());
    }
	private <T extends CalculusFieldElement<T>> void doCompareToNumericalPropagationResetInitialIntermediate(Field<T> field) {

    	T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final Frame inertialFrame = FramesFactory.getEME2000();
        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        double timeshift = 60000. ;

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = 0; // mean anomaly
        final FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(a), zero.add(e), zero.add(i), zero.add(omega), 
        		                                      zero.add(raan), zero.add(lM), PositionAngle.TRUE,
                                                      inertialFrame, initDate, zero.add(provider.getMu()));
        // Initial state definition
        final FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(initialOrbit);

        //_______________________________________________________________________________________________
        // SET UP A BROUWER LYDDANE PROPAGATOR
        //_______________________________________________________________________________________________

        FieldBrouwerLyddanePropagator<T> BLextrapolator1 =
	            new FieldBrouwerLyddanePropagator<>(initialOrbit, DEFAULT_LAW, zero.add(Propagator.DEFAULT_MASS), GravityFieldFactory.getUnnormalizedProvider(provider),
	            		                     PropagationType.OSCULATING);
        //_______________________________________________________________________________________________
        // SET UP ANOTHER BROUWER LYDDANE PROPAGATOR
        //_______________________________________________________________________________________________

        FieldBrouwerLyddanePropagator<T> BLextrapolator2 =
	            new FieldBrouwerLyddanePropagator<>( new FieldKeplerianOrbit<>(zero.add(a + 3000), zero.add(e + 0.001), 
	            		                                                       zero.add(i - FastMath.toRadians(12.0)), zero.add(omega),
	            		                                                       zero.add(raan), zero.add(lM), PositionAngle.TRUE,
                                                     inertialFrame, initDate, zero.add(provider.getMu())),DEFAULT_LAW, 
	            		                             zero.add(Propagator.DEFAULT_MASS), 
	            		                             GravityFieldFactory.getUnnormalizedProvider(provider));
        // Reset BL2 with BL1 initial state
        BLextrapolator2.resetInitialState(initialState);

        FieldSpacecraftState<T> BLFinalState1 = BLextrapolator1.propagate(initDate.shiftedBy(timeshift));
	    final KeplerianOrbit BLOrbit1 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState1.getOrbit().toOrbit());
	    FieldSpacecraftState<T> BLFinalState2 = BLextrapolator2.propagate(initDate.shiftedBy(timeshift));
        BLextrapolator2.resetIntermediateState(BLFinalState1, true);
	    final KeplerianOrbit BLOrbit2 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState2.getOrbit().toOrbit());

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
        doCompareConstructors(Decimal64Field.getInstance());
    }
    private <T extends CalculusFieldElement<T>> void doCompareConstructors(Field<T> field) {

	    T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final Frame inertialFrame = FramesFactory.getEME2000();
        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        double timeshift = 600. ;

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = 0; // mean anomaly
        final FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(a), zero.add(e), zero.add(i), zero.add(omega), 
                                                                     zero.add(raan), zero.add(lM), PositionAngle.TRUE,
                                                                     inertialFrame, initDate, zero.add(provider.getMu()));


        FieldBrouwerLyddanePropagator<T> BLPropagator1 = new FieldBrouwerLyddanePropagator<T>(initialOrbit, DEFAULT_LAW,
        		provider.getAe(), zero.add(provider.getMu()), -1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7);
        FieldBrouwerLyddanePropagator<T> BLPropagator2 = new FieldBrouwerLyddanePropagator<>(initialOrbit,
        		provider.getAe(), zero.add(provider.getMu()), -1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7);
        FieldBrouwerLyddanePropagator<T> BLPropagator3 = new FieldBrouwerLyddanePropagator<>(initialOrbit, 
        		zero.add(Propagator.DEFAULT_MASS), provider.getAe(), zero.add(provider.getMu()), -1.08263e-3, 
        		2.54e-6, 1.62e-6, 2.3e-7);

        FieldSpacecraftState<T> BLFinalState1 = BLPropagator1.propagate(initDate.shiftedBy(timeshift));
	    final KeplerianOrbit BLOrbit1 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState1.getOrbit().toOrbit());
	    FieldSpacecraftState<T> BLFinalState2 = BLPropagator2.propagate(initDate.shiftedBy(timeshift));
	    final KeplerianOrbit BLOrbit2 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState2.getOrbit().toOrbit());
	    FieldSpacecraftState<T> BLFinalState3 = BLPropagator3.propagate(initDate.shiftedBy(timeshift));
	    final KeplerianOrbit BLOrbit3 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState3.getOrbit().toOrbit());


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


	@Test
    public void undergroundOrbit() {
        doUndergroundOrbit(Decimal64Field.getInstance());
    }
    private <T extends CalculusFieldElement<T>> void doUndergroundOrbit(Field<T> field) {

	    T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        
        // for a semi major axis < equatorial radius
    	FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
    	FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(800.0), zero.add(100.0));

        FieldOrbit<T> initialOrbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<T>(position, velocity),
                                                  FramesFactory.getEME2000(), initDate, zero.add(provider.getMu()));
        // Extrapolator definition
        // -----------------------
        try {
        	
            FieldBrouwerLyddanePropagator<T> extrapolator =
                new FieldBrouwerLyddanePropagator<>(initialOrbit, DEFAULT_LAW, provider.getAe(), zero.add(provider.getMu()),
                		-1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7);
            
            // Extrapolation at the initial date
            // ---------------------------------
            double delta_t = 0.0;
            FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);
            extrapolator.propagate(extrapDate);
        
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.TRAJECTORY_INSIDE_BRILLOUIN_SPHERE, oe.getSpecifier());
        }
    }


	@Test
    public void tooEllipticalOrbit() {
        doTooEllipticalOrbit(Decimal64Field.getInstance());
    }
	private <T extends CalculusFieldElement<T>> void doTooEllipticalOrbit(Field<T> field) {
        // for an eccentricity too big for the model
		T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(67679244.0), zero.add(1.0), zero.add(1.85850), 
        		                                               zero.add(2.1), zero.add(2.9), zero.add(6.2), PositionAngle.TRUE, 
        		                                               FramesFactory.getEME2000(), initDate, zero.add(provider.getMu()));
        try {
        // Extrapolator definition
        // -----------------------
        FieldBrouwerLyddanePropagator<T> extrapolator =
            new FieldBrouwerLyddanePropagator<>(initialOrbit, provider.getAe(), zero.add(provider.getMu()),
            		-1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7);

        // Extrapolation at the initial date
        // ---------------------------------
        double delta_t = 0.0;
        FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);
        extrapolator.propagate(extrapDate);
        
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.TOO_LARGE_ECCENTRICITY_FOR_PROPAGATION_MODEL, oe.getSpecifier());
        }
    }


	@Test
    public void criticalInclination() {
        doCriticalInclination(Decimal64Field.getInstance());
    }
	private <T extends CalculusFieldElement<T>> void doCriticalInclination(Field<T> field) {

        final Frame inertialFrame = FramesFactory.getEME2000();

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = 0; // mean anomaly

        T zero = field.getZero();
        FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field);
        final FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(a), zero.add(e), zero.add(i), zero.add(omega), 
                                                                     zero.add(raan), zero.add(lM), PositionAngle.TRUE,
                                                                     inertialFrame, initDate, zero.add(provider.getMu()));

        // Extrapolator definition
        // -----------------------
        FieldBrouwerLyddanePropagator<T> extrapolator =
            new FieldBrouwerLyddanePropagator<>(initialOrbit, GravityFieldFactory.getUnnormalizedProvider(provider));

        // Extrapolation at the initial date
        // ---------------------------------
        final FieldSpacecraftState<T> finalOrbit = extrapolator.propagate(initDate);

        // Verify
        Assert.assertEquals(0.0,
                            FieldVector3D.distance(initialOrbit.getPVCoordinates().getPosition(),
                                                   finalOrbit.getPVCoordinates().getPosition()).getReal(),
                            7.0e-8);

        Assert.assertEquals(0.0,
                            FieldVector3D.distance(initialOrbit.getPVCoordinates().getVelocity(),
                                                   finalOrbit.getPVCoordinates().getVelocity()).getReal(),
                            1.2e-11);

        Assert.assertEquals(0.0, finalOrbit.getA().getReal() - initialOrbit.getA().getReal(), 0.0);

    }

    @Test(expected = OrekitException.class)
    public void testUnableToComputeBLMeanParameters() {
        doTestUnableToComputeBLMeanParameters(Decimal64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestUnableToComputeBLMeanParameters(Field<T> field) {

        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final Frame inertialFrame = FramesFactory.getEME2000();
        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.9; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = FastMath.toRadians(0); // mean anomaly
        final FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(a), zero.add(e), zero.add(i), zero.add(omega), 
                                                                     zero.add(raan), zero.add(lM), PositionAngle.TRUE,
                                                                     inertialFrame, initDate, zero.add(provider.getMu()));

        // Extrapolator definition
        // -----------------------
        final FieldBrouwerLyddanePropagator<T> blField = new FieldBrouwerLyddanePropagator<>(initialOrbit, GravityFieldFactory.getUnnormalizedProvider(provider));

        // Extrapolation at the initial date
        // ---------------------------------
        T delta_t = zero;
        FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);
        blField.propagate(extrapDate);

    }
    
	@Test
	public void testMeanComparisonWithNonField() {
	    doTestMeanComparisonWithNonField(Decimal64Field.getInstance());
	}

	private <T extends CalculusFieldElement<T>> void doTestMeanComparisonWithNonField(Field<T> field) {

	    T zero = field.getZero();
	    FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
	    final Frame inertialFrame = FramesFactory.getEME2000();
	    FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
	    double timeshift = -60000.0;

	    // Initial orbit
	    final double a = 24396159; // semi major axis in meters
	    final double e = 0.9; // eccentricity
	    final double i = FastMath.toRadians(7); // inclination
	    final double omega = FastMath.toRadians(180); // perigee argument
	    final double raan = FastMath.toRadians(261); // right ascention of ascending node
	    final double lM = FastMath.toRadians(0); // mean anomaly
	    final FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(a), zero.add(e), zero.add(i), zero.add(omega), 
	                                                                 zero.add(raan), zero.add(lM), PositionAngle.TRUE,
	                                                                 inertialFrame, initDate, zero.add(provider.getMu()));

	    // Initial state definition
	    final FieldSpacecraftState<T> initialStateField = new FieldSpacecraftState<>(initialOrbit);
	    final SpacecraftState         initialState      = initialStateField.toSpacecraftState();

	    // Field propagation
	    final FieldBrouwerLyddanePropagator<T> blField = new FieldBrouwerLyddanePropagator<>(initialStateField.getOrbit(),
	                                                                                         GravityFieldFactory.getUnnormalizedProvider(provider),
	                                                                                         PropagationType.MEAN);
	    final FieldSpacecraftState<T> finalStateField     = blField.propagate(initialStateField.getDate().shiftedBy(timeshift));
	    final FieldKeplerianOrbit<T>  finalOrbitField     = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(finalStateField.getOrbit());
	    final KeplerianOrbit          finalOrbitFieldReal = finalOrbitField.toOrbit();

	    // Classical propagation
	    final BrouwerLyddanePropagator bl = new BrouwerLyddanePropagator(initialState.getOrbit(),
	                                                                     GravityFieldFactory.getUnnormalizedProvider(provider),
	                                                                     PropagationType.MEAN);
	    final SpacecraftState finalState = bl.propagate(initialState.getDate().shiftedBy(timeshift));
	    final KeplerianOrbit  finalOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(finalState.getOrbit());

	    Assert.assertEquals(finalOrbitFieldReal.getA(), finalOrbit.getA(), Double.MIN_VALUE);
	    Assert.assertEquals(finalOrbitFieldReal.getE(), finalOrbit.getE(), Double.MIN_VALUE);
	    Assert.assertEquals(finalOrbitFieldReal.getI(), finalOrbit.getI(), Double.MIN_VALUE);
	    Assert.assertEquals(finalOrbitFieldReal.getRightAscensionOfAscendingNode(), + finalOrbit.getRightAscensionOfAscendingNode(), Double.MIN_VALUE);
	    Assert.assertEquals(finalOrbitFieldReal.getPerigeeArgument(), finalOrbit.getPerigeeArgument(), Double.MIN_VALUE);
	    Assert.assertEquals(finalOrbitFieldReal.getMeanAnomaly(), finalOrbit.getMeanAnomaly(), Double.MIN_VALUE);
	    Assert.assertEquals(0.0, finalOrbitFieldReal.getPVCoordinates().getPosition().distance(finalOrbit.getPVCoordinates().getPosition()), Double.MIN_VALUE);
	    Assert.assertEquals(0.0, finalOrbitFieldReal.getPVCoordinates().getVelocity().distance(finalOrbit.getPVCoordinates().getVelocity()), Double.MIN_VALUE);
	}

	@Test
	public void testOsculatingComparisonWithNonField() {
	    doTestOsculatingComparisonWithNonField(Decimal64Field.getInstance());
	}

	private <T extends CalculusFieldElement<T>> void doTestOsculatingComparisonWithNonField(Field<T> field) {

	    T zero = field.getZero();
	    FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
	    final Frame inertialFrame = FramesFactory.getEME2000();
	    FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
	    double timeshift = 60000.0;

	    // Initial orbit
	    final double a = 24396159; // semi major axis in meters
	    final double e = 0.01; // eccentricity
	    final double i = FastMath.toRadians(7); // inclination
	    final double omega = FastMath.toRadians(180); // perigee argument
	    final double raan = FastMath.toRadians(261); // right ascention of ascending node
	    final double lM = FastMath.toRadians(0); // mean anomaly
	    final FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(a), zero.add(e), zero.add(i), zero.add(omega), 
	                                                                 zero.add(raan), zero.add(lM), PositionAngle.TRUE,
	                                                                 inertialFrame, initDate, zero.add(provider.getMu()));

	    // Initial state definition
	    final FieldSpacecraftState<T> initialStateField = new FieldSpacecraftState<>(initialOrbit);
	    final SpacecraftState         initialState      = initialStateField.toSpacecraftState();

	    // Field propagation
	    final FieldBrouwerLyddanePropagator<T> blField = new FieldBrouwerLyddanePropagator<>(initialStateField.getOrbit(),
	                                                                                         GravityFieldFactory.getUnnormalizedProvider(provider));
	    final FieldSpacecraftState<T> finalStateField     = blField.propagate(initialStateField.getDate().shiftedBy(timeshift));
	    final FieldKeplerianOrbit<T>  finalOrbitField     = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(finalStateField.getOrbit());
	    final KeplerianOrbit          finalOrbitFieldReal = finalOrbitField.toOrbit();

	    // Classical propagation
	    final BrouwerLyddanePropagator bl = new BrouwerLyddanePropagator(initialState.getOrbit(),
	                                                                     GravityFieldFactory.getUnnormalizedProvider(provider));
	    final SpacecraftState finalState = bl.propagate(initialState.getDate().shiftedBy(timeshift));
	    final KeplerianOrbit  finalOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(finalState.getOrbit());

	    Assert.assertEquals(finalOrbitFieldReal.getA(), finalOrbit.getA(), Double.MIN_VALUE);
	    Assert.assertEquals(finalOrbitFieldReal.getE(), finalOrbit.getE(), Double.MIN_VALUE);
	    Assert.assertEquals(finalOrbitFieldReal.getI(), finalOrbit.getI(), Double.MIN_VALUE);
	    Assert.assertEquals(finalOrbitFieldReal.getRightAscensionOfAscendingNode(), + finalOrbit.getRightAscensionOfAscendingNode(), Double.MIN_VALUE);
	    Assert.assertEquals(finalOrbitFieldReal.getPerigeeArgument(), finalOrbit.getPerigeeArgument(), Double.MIN_VALUE);
	    Assert.assertEquals(finalOrbitFieldReal.getMeanAnomaly(), finalOrbit.getMeanAnomaly(), Double.MIN_VALUE);
	    Assert.assertEquals(0.0, finalOrbitFieldReal.getPVCoordinates().getPosition().distance(finalOrbit.getPVCoordinates().getPosition()), Double.MIN_VALUE);
	    Assert.assertEquals(0.0, finalOrbitFieldReal.getPVCoordinates().getVelocity().distance(finalOrbit.getPVCoordinates().getVelocity()), Double.MIN_VALUE);
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

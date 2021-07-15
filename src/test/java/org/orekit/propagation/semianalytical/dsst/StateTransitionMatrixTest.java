package org.orekit.propagation.semianalytical.dsst;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.models.earth.atmosphere.HarrisPriester;
import org.orekit.models.earth.atmosphere.data.MarshallSolarActivityFutureEstimation;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.propagation.semianalytical.dsst.forces.ShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Utility class to validate the calculation of state transition matrix
 * using automatic differentiation  by comparison with finite
 * differencing method.
 * @author Bryan Cazabonne
 */
public class StateTransitionMatrixTest {

    /** Satellite mass. */
    protected static final double MASS = 685.0;

    /** Integration step. */
    private static final double STEP = 0.2 * Constants.JULIAN_DAY;

    /** Flag for force models. */
    private static final boolean USE_ZONAL    = true;
    /** Flag for force models. */
    private static final boolean USE_TESSERAL = false;
    /** Flag for force models. */
    private static final boolean USE_MOON     = false;
    /** Flag for force models. */
    private static final boolean USE_SUN      = false;
    /** Flag for force models. */
    private static final boolean USE_DRAG     = false;
    /** Flag for force models. */
    private static final boolean USE_SRP      = false;

    /** Gravity field degree and order. */
    private static final int DEGREE = 2;
    private static final int ORDER  = 0;

    /** Format for printing values. */
    private static final String DOUBLE_FORMAT = "|%25s|";

    /** Flag for saving data. */
    private static final boolean SAVE = true;

    /** Format for writing data in the file. */
    private static final String FMT = "%s";
    private static final String cReturn = "\n";

    /** File to save the data. */
    private static PrintWriter STM ;


    /** Private constructor for utility class. */
    public StateTransitionMatrixTest() throws FileNotFoundException {
        // Initialize writer if needed
        if (SAVE) {
            STM = new PrintWriter(new File("STM.txt"));
        }
    }

    @Test
    public void getB1Test() throws FileNotFoundException {

        // Configure Orekit
        initializeOrekit();

        // Run the program for B1
        StreamingStatistics stat = new StateTransitionMatrixTest().run(PropagationType.OSCULATING, PropagationType.OSCULATING, 1);

        // Close files
        closeFiles();
        
        // Tests on statistics
        Assert.assertEquals(8.6E-9, stat.getMean(), 1E-10);
        Assert.assertEquals(0, stat.getMin(), 1E-9);
        Assert.assertEquals(7.6E-7, stat.getMax(), 1E-8);
        Assert.assertEquals(4.4E-8, stat.getStandardDeviation(), 1E-9);

    }
    
    
    @Test
    public void getB2Test() throws FileNotFoundException {

        // Configure Orekit
        initializeOrekit();

        // Run the program for B2
        StreamingStatistics stat = new StateTransitionMatrixTest().run(PropagationType.MEAN, PropagationType.MEAN, 2);

        // Close files
        closeFiles();
        
     // Tests on statistics
        Assert.assertEquals(6.3E-8, stat.getMean(), 1E-9);
        Assert.assertEquals(0, stat.getMin(), 1E-8);
        Assert.assertEquals(2.5E-6, stat.getMax(), 1E-7);
        Assert.assertEquals(2.3E-7, stat.getStandardDeviation(), 1E-8);

    }
    

    /**
     * Run the program.
     */
    private StreamingStatistics run(PropagationType initType, PropagationType propType, int B) {

        // Central body
        final OneAxisEllipsoid centralBody = initializeBody();

        // Gravity field
        final UnnormalizedSphericalHarmonicsProvider gravityField = initializeGravityField();

        // Initial orbit
        final Orbit initialOrbit = initType == PropagationType.MEAN ?
                                        initializeMeanOrbit(gravityField) : initializeOscOrbit(gravityField);

        final DSSTPropagator propagator = initializePropagator(initialOrbit, initType, propType,
                                                                   centralBody, gravityField);
        
        STMFixedStepHandler stepHandler = initializeStepHandler(initType, propType, B, propagator);
        
        // Propagation
        propagator.propagate(initialOrbit.getDate().shiftedBy(10.0 * Constants.JULIAN_DAY));
        
        return stepHandler.stat;
        
    }

    /**
     * Initialize Orekit data.
     */
    private static void initializeOrekit() {
        // Configure path
        final File home       = new File(System.getProperty("user.home"));
        final File orekitData = new File(home, "orekit-data");
        // Define data provider
        final DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));
    }

    /**
     * Close files used in the program.
     */
    private static void closeFiles() {
        // Close STM file
        if (SAVE) {
            STM.close();
        }
    }

    /** Initialize the unnormalized spherical harmonics provider.
     * @return a configured unnormalized spherical harmonics provide
     */
    private static UnnormalizedSphericalHarmonicsProvider initializeGravityField() {
        return GravityFieldFactory.getUnnormalizedProvider(DEGREE, ORDER);
    }

    /**
     * Initialize the mean orbit used for the test.
     * @param gravityField gravity field provider
     * @return the mean initial orbit
     */
    private static Orbit initializeMeanOrbit(final UnnormalizedSphericalHarmonicsProvider gravityField) {

        // Reference orbit :
        // "Cefola P. J., Demonstration of the DSST State transition matrix time-update
        //  properties using Linux GTDS program, Proceedings of the AMOSS Conference, 2011"

        // Epoch
        final AbsoluteDate orbitEpoch = new AbsoluteDate("2008-09-15T21:59:46.000", TimeScalesFactory.getUTC());

        // Frame
        final Frame orbitFrame = FramesFactory.getEME2000();

        // Return the reference orbit used for the test
        return new KeplerianOrbit(6706966.2, 0.0010252154, FastMath.toRadians(87.266393),
                                  FastMath.toRadians(94.431363), FastMath.toRadians(64.668178),
                                  FastMath.toRadians(105.69973), PositionAngle.MEAN, orbitFrame,
                                  orbitEpoch, gravityField.getMu());

    }

    /**
     * Initialize the mean orbit used for the test.
     * @param gravityField gravity field provider
     * @return the mean initial orbit
     */
    private static Orbit initializeOscOrbit(final UnnormalizedSphericalHarmonicsProvider gravityField) {

        // Reference orbit :
        // "Cefola P. J., Demonstration of the DSST State transition matrix time-update
        //  properties using Linux GTDS program, Proceedings of the AMOSS Conference, 2011"

        // Epoch
        final AbsoluteDate orbitEpoch = new AbsoluteDate("2008-09-15T21:59:46.000", TimeScalesFactory.getUTC());

        // Frame
        final Frame orbitFrame = FramesFactory.getEME2000();

        // Return isculating orbit
        return new CartesianOrbit(new TimeStampedPVCoordinates(orbitEpoch,
                                                               new Vector3D(-2595256.643, -5741664.984, -2321359.682),
                                                               new Vector3D(1450.193597, 2258.205121, -7221.683085)),
                                  orbitFrame, gravityField.getMu());

    }

    /**
     * Initialize the central body (i.e. the Earth).
     * @return a configured central body
     */
    private static OneAxisEllipsoid initializeBody() {

        // Body frame
        final Frame bodyFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

        // Return the configured body
        return new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                    Constants.WGS84_EARTH_FLATTENING,
                                    bodyFrame);

    }

    /**
     * Initialize the DSST propagator (i.e. force models, derivatives without the step handler).
     * @param initialOrbit the initial orbit to use
     * @param initialStateType initial state type (mean or perturbated orbit)
     * @param propagationType propagation type (mean or osculating)
     * @param centralBody central body
     * @param gravityField gravity field
     * @return a configured propagator
     */
    private static DSSTPropagator initializePropagator(final Orbit initialOrbit,
                                                       final PropagationType initialStateType,
                                                       final PropagationType propagationType,
                                                       final OneAxisEllipsoid centralBody,
                                                       final UnnormalizedSphericalHarmonicsProvider gravityField) {

        // Initialize numerical integrator
        final ODEIntegrator integrator = new ClassicalRungeKuttaIntegrator(STEP);

        // Create the DSST propagator
        final DSSTPropagator propagator = new DSSTPropagator(integrator, propagationType);

        // Add force models to the DSST Propagator
        addForceModels(propagator, centralBody, gravityField);

        // Add the initial state to the propagator
        final SpacecraftState initialState = new SpacecraftState(propagator.getOrbitType().convertType(initialOrbit), MASS);
        propagator.setInitialState(initialState, initialStateType);

        // Return the fully configured DSST propagator without step handler
        return propagator;

    }

    
    /**
     * Initialize the propagator step handler and add it to the propagator.
     * @param initialStateType initial state type (mean or perturbated orbit)
     * @param propagationType propagation type (mean or osculating)
     * @param B to know if either getB1, getB2, getB3 or getB4 should be test.
     * @param propagator the propagtor at which the step handler should be added
     * @return a configured step handler
     */
    private static STMFixedStepHandler initializeStepHandler(final PropagationType initialStateType,
                                                       final PropagationType propagationType,
                                                       final int B, final DSSTPropagator propagator) {

        // Configure derivatives
        final DSSTJacobiansMapper mapper = configureDerivatives(propagator, propagationType);

        // Step STM step handler to the propagator
        STMFixedStepHandler stepHandler = new STMFixedStepHandler(mapper, propagator, initialStateType, propagationType, B);

        propagator.setMasterMode(STEP, stepHandler);
        
        return stepHandler;
    }
    
    
    /**
     * Add the force models to the propagator.
     * @param propagator propagator
     * @param centralBody central body
     * @param gravityField gravity field
     */
    private static void addForceModels(final DSSTPropagator propagator,
                                       final OneAxisEllipsoid centralBody,
                                       final UnnormalizedSphericalHarmonicsProvider gravityField) {

        // Zonal
        if (USE_ZONAL) {
            propagator.addForceModel(new DSSTZonal(gravityField));
        }

        // Tesseral
        if (USE_TESSERAL) {
            propagator.addForceModel(new DSSTTesseral(centralBody.getBodyFrame(), Constants.WGS84_EARTH_ANGULAR_VELOCITY, gravityField));
        }

        // Moon
        if (USE_MOON) {
            propagator.addForceModel(new DSSTThirdBody(CelestialBodyFactory.getMoon(), gravityField.getMu()));
        }

        // Sun
        final CelestialBody sun = CelestialBodyFactory.getSun();
        if (USE_SUN) {
            propagator.addForceModel(new DSSTThirdBody(sun, gravityField.getMu()));
        }

        // Drag
        if (USE_DRAG) {

            // Atmosphere model
            final MarshallSolarActivityFutureEstimation msafe =
                            new MarshallSolarActivityFutureEstimation(MarshallSolarActivityFutureEstimation.DEFAULT_SUPPORTED_NAMES,
                                                                      MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
            final DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
            manager.feed(msafe.getSupportedNames(), msafe);
            final Atmosphere atmosphere = new HarrisPriester(CelestialBodyFactory.getSun(), centralBody); // NRLMSISE00(msafe, CelestialBodyFactory.getSun(), centralBody); //new DTM2000(msafe, CelestialBodyFactory.getSun(), centralBody);

            // Add the force model
            propagator.addForceModel(new DSSTAtmosphericDrag(new DragForce(atmosphere, new IsotropicDrag(5.0, 1.0)), gravityField.getMu()));

        }

        // Solar radiation pressure
        if (USE_SRP) {

            // Satellite model
            final RadiationSensitive spacecraft = new IsotropicRadiationSingleCoefficient(5.0, 1.0);

            // Add the force model
            propagator.addForceModel(new DSSTSolarRadiationPressure(sun, centralBody.getEquatorialRadius(), spacecraft, gravityField.getMu()));

        }

    }

    /**
     * Configure the derivatives.
     * @param propagator propagator for which derivatives are computed
     * @param propagationType propagation type
     * @return a configured jacobian mapper
     */
    private static DSSTJacobiansMapper configureDerivatives(final DSSTPropagator propagator,
                                                            final PropagationType propagationType) {

        // Equation name
        final String name = "STM";

        // STM
        final DSSTPartialDerivativesEquations pde = new DSSTPartialDerivativesEquations(name, propagator);

        // Get initial state
        final SpacecraftState rawState = propagator.getInitialState();

        // Initialise the Jacobian matrix
        final SpacecraftState stateWithDerivatives = pde.setInitialJacobians(rawState);

        // Add the derivatives to the initial state and the propagator
        propagator.resetInitialState(stateWithDerivatives);

        // Return a configured jacobian mapper
        return pde.getMapper();

    }

    /**
     * Step handler to handle state transition matrix during propagation.
     * @author Bryan Cazabonne
     */
    private static class STMFixedStepHandler implements OrekitFixedStepHandler {

        /** Jacobian mapper. */
        private final DSSTJacobiansMapper mapper;

        /** Initial orbit. */
        private final Orbit initialOrbit;

        /** List of force models used. */
        private final List<DSSTForceModel> forceModels;

        /** Initial orbit type. */
        private final PropagationType initialOrbitType;

        /** Propagation type. */
        private final PropagationType propagationType;

        /** Statistics on derivatives. */
        private StreamingStatistics stat;

        /** Flag for first step. */
        private boolean isFirst;

        /** Index to know which getB should be used*/
        private int B;

        private boolean print = false;

        /**
         * Constructor.
         * @param mapper jacobian mapper
         */
        public STMFixedStepHandler(final DSSTJacobiansMapper mapper,
                                   final DSSTPropagator refPropagator,
                                   final PropagationType initialOrbitType,
                                   final PropagationType propagationType,
                                   final int B) {
            // Initialize fields
            this.mapper           = mapper;
            this.propagationType  = propagationType;
            this.initialOrbitType = initialOrbitType;
            this.initialOrbit     = refPropagator.getInitialState().getOrbit();
            this.forceModels      = refPropagator.getAllForceModels();
            // Initialize statistics tool
            this.stat             = new StreamingStatistics();
            // First step
            this.isFirst          = true;
            this.B                = B;

        }

        /** {@inheritDoc} */
        @Override
        public void handleStep(final SpacecraftState currentState, final boolean isLast) {

            // Check if it is the first step
            if (!isFirst) {

                if (print) System.out.println("Results for epoch (UTC): " + currentState.getDate().toString());

                // Time difference between current orbit and previous orbit
                final double dtDays = currentState.getDate().durationFrom(initialOrbit.getDate()) / Constants.JULIAN_DAY;

                // Get state transition matrix
                final double[][] dYdY0 = new double[6][6];
                if (propagationType == PropagationType.OSCULATING) {
                	mapper.setShortPeriodJacobians(DSSTPropagator.computeMeanState(currentState, Propagator.DEFAULT_LAW, forceModels));
                }

                if (this.B == 1) {
                    // B1 : dYdY0 of short period derivatives
                    mapper.getB1(dYdY0);
                }

                else if (this.B == 2) {
                    // B2 : dYdY0 of state
                    mapper.getB2(currentState, dYdY0);
                }

                else if (this.B == 3) {
                    // B3 : dYdP of short period derivatives
                    mapper.getB3(currentState, dYdY0);
                }

                else if (this.B == 4) {
                    // B4 : dYdP of state
                    mapper.getB4(dYdY0);
                }

                else {
                    // Mean Jacobian of short period jacobian
                    mapper.getStateJacobian(currentState, dYdY0);
                }

                if (print) {
                    for (int i = 0;  i < 6; i++) {
                        for (int j = 0; j < 6; j++) {
                            System.out.print(dYdY0[i][j] + " ");
                        }
                        System.out.println();
                    }
                }


                // Propagator used for finite differencing comparison
                final DSSTPropagator propagator = finiteDifferencePropagator();

                // Reference Jacobian matrix
                final double[][] dYdY0Ref = initialOrbitType == PropagationType.MEAN ?
                                                finiteDifferenceCalculation(propagator, currentState.getDate()) :
                                                    finiteDifferenceSPCalculation(propagator, currentState, currentState.getDate());

                ///////////////////////////
                // Compute the relative difference between references and computed Jacobians
                ///////////////////////////

                // Initialize buffer to write relative differences
                final StringBuffer matixBuffer = new StringBuffer();

                // Loop on rows
                for (int row = 0; row < 6; row++) {

                    // Initialize buffer for matrix row
                    final StringBuffer rowBuffer = new StringBuffer();

                    // Loop on columns
                    for (int column = 0; column < 6; column++) {

                        // Compute relative difference
                        final double delta = FastMath.abs((dYdY0[row][column] - dYdY0Ref[row][column]) / dYdY0Ref[row][column]);

                        if (dYdY0Ref[row][column] != 0.0 && dYdY0[row][column] != 0.0) {
                            // Add relative difference to statistics data
                            stat.addValue(delta);
                            // Edit buffers
                            rowBuffer.append(String.format(Locale.getDefault(), DOUBLE_FORMAT, delta));
                            matixBuffer.append(String.format(Locale.getDefault(), "%25s,", delta));
                        } else {
                            // Edit buffers
                            rowBuffer.append(String.format(Locale.getDefault(), DOUBLE_FORMAT, 0.0));
                            matixBuffer.append(String.format(Locale.getDefault(), "%25s,", "NaN"));
                        }
                    }

                    // Print relative difference for the current tow
                    if (print) System.out.println(rowBuffer.toString());

                }

                // Write values in file if needed
                if (SAVE) {
                    STM.printf(FMT, String.format("%25s,", dtDays) + " " + matixBuffer.toString() + cReturn);
                }

                // Print end of derivatives computation
                if (print) System.out.println("STM computed for current state, Epoch (UTC): " + currentState.getDate().toString());
                if (print) System.out.println(" ");

            }

            // Set the flag to false
            isFirst = false;

            // Print statistics
            if (isLast && print) {
                System.out.println("Mean:   " + stat.getMean());
                System.out.println("Min:    " + stat.getMin());
                System.out.println("Max:    " + stat.getMax());
                System.out.println("Sigma:  " + stat.getStandardDeviation());
            }

        }

        /**
         * Initialize the propagator used for finite differencing method.
         * @return a configured propagator
         */
        private DSSTPropagator finiteDifferencePropagator() {

            // Initialize propagator
            final DSSTPropagator finiteDifferencePropagator =
                            new DSSTPropagator(new ClassicalRungeKuttaIntegrator(STEP),
                                               propagationType);

            // Set the force models
            for (final DSSTForceModel model : forceModels) {
                finiteDifferencePropagator.addForceModel(model);
            }

            // Set the initial statev (will be changed for finite differencing computation)
            finiteDifferencePropagator.setInitialState(new SpacecraftState(initialOrbit, MASS), initialOrbitType);

            // Return a configured finite difference propagator
            return finiteDifferencePropagator;

        }

        /**Compute the reference state Jacobian using finite differencing method.
         * @param propagator orbit propagator
         * @param target target epoch
         * @return reference state Jacobian
         */
        private double[][] finiteDifferenceCalculation(final DSSTPropagator propagator,
                                                       final AbsoluteDate target) {

            // Initialize reference Jacobian
            final double[][] dYdY0Ref = new double[6][6];

            // Initial state
            final SpacecraftState initialState = new SpacecraftState(initialOrbit, MASS);

            // Orbit type
            final OrbitType orbitType = propagator.getOrbitType();

            // Step
            final double[] steps = NumericalPropagator.tolerances(100.0, initialOrbit, orbitType)[0];

            // Compute reference Jacobian
            for (int i = 0; i < 6; ++i) {
                propagator.setInitialState(shiftState(propagator, initialState, orbitType, -4.0 * steps[i], i), initialOrbitType);
                final SpacecraftState sM4h = propagator.propagate(target);
                propagator.setInitialState(shiftState(propagator, initialState, orbitType, -3.0 * steps[i], i), initialOrbitType);
                final SpacecraftState sM3h = propagator.propagate(target);
                propagator.setInitialState(shiftState(propagator, initialState, orbitType, -2.0 * steps[i], i), initialOrbitType);
                final SpacecraftState sM2h = propagator.propagate(target);
                propagator.setInitialState(shiftState(propagator, initialState, orbitType, -1.0 * steps[i], i), initialOrbitType);
                final SpacecraftState sM1h = propagator.propagate(target);
                propagator.setInitialState(shiftState(propagator, initialState, orbitType,  1.0 * steps[i], i), initialOrbitType);
                final SpacecraftState sP1h = propagator.propagate(target);
                propagator.setInitialState(shiftState(propagator, initialState, orbitType,  2.0 * steps[i], i), initialOrbitType);
                final SpacecraftState sP2h = propagator.propagate(target);
                propagator.setInitialState(shiftState(propagator, initialState, orbitType,  3.0 * steps[i], i), initialOrbitType);
                final SpacecraftState sP3h = propagator.propagate(target);
                propagator.setInitialState(shiftState(propagator, initialState, orbitType,  4.0 * steps[i], i), initialOrbitType);
                final SpacecraftState sP4h = propagator.propagate(target);
                fillJacobianColumn(propagator, dYdY0Ref, i, orbitType, steps[i],
                                   sM4h, sM3h, sM2h, sM1h, sP1h, sP2h, sP3h, sP4h);
            }

            // Return the filled reference state Jacobian
            return dYdY0Ref;

        }

        /** Compute the short periodic Jacobian using finite differencing method.
         * @param propagator orbit propagator
         * @param currentState current spacecraft state
         * @param target target epoch
         * @return reference short periodic Jacobian
         */
        private double[][] finiteDifferenceSPCalculation(final DSSTPropagator propagator,
                                                         final SpacecraftState currentState,
                                                         final AbsoluteDate target) {

            // Initialize reference Jacobian
            final double[][] dSPdYRef = new double[6][6];

            // Orbit type
            final OrbitType orbitType = propagator.getOrbitType();

            // Step
            final double[] steps = NumericalPropagator.tolerances(10.0, initialOrbit, orbitType)[0];

            // Convert current state to a mean state
            final SpacecraftState meanState = DSSTPropagator.computeMeanState(currentState, Propagator.DEFAULT_LAW, forceModels);

            // Compute reference Jacobian
            for (int i = 0; i < 6; ++i) {

                for (DSSTForceModel model : forceModels) {

                    final SpacecraftState shiftedStateM4h = shiftState(propagator, meanState, orbitType, -4.0 * steps[i], i);
                    final double[] m4H = computeShortPeriodTerms(shiftedStateM4h, model);

                    final SpacecraftState shiftedStateM3h = shiftState(propagator, meanState, orbitType, -3.0 * steps[i], i);
                    final double[] m3H = computeShortPeriodTerms(shiftedStateM3h, model);

                    final SpacecraftState shiftedStateM2h = shiftState(propagator, meanState, orbitType, -2.0 * steps[i], i);
                    final double[] m2H = computeShortPeriodTerms(shiftedStateM2h, model);

                    final SpacecraftState shiftedStateM1h = shiftState(propagator, meanState, orbitType, -1.0 * steps[i], i);
                    final double[] m1H = computeShortPeriodTerms(shiftedStateM1h, model);

                    final SpacecraftState shiftedStateP1h = shiftState(propagator, meanState, orbitType, 1.0 * steps[i], i);
                    final double[] p1H = computeShortPeriodTerms(shiftedStateP1h, model);

                    final SpacecraftState shiftedStateP2h = shiftState(propagator, meanState, orbitType, 2.0 * steps[i], i);
                    final double[] p2H = computeShortPeriodTerms(shiftedStateP2h, model);

                    final SpacecraftState shiftedStateP3h = shiftState(propagator, meanState, orbitType, 3.0 * steps[i], i);
                    final double[] p3H = computeShortPeriodTerms(shiftedStateP3h, model);

                    final SpacecraftState shiftedStateP4h = shiftState(propagator, meanState, orbitType, 4.0 * steps[i], i);
                    final double[] p4H = computeShortPeriodTerms(shiftedStateP4h, model);

                    fillSPJacobianColumn(propagator, dSPdYRef, i, orbitType, steps[i],
                                         m4H, m3H, m2H, m1H, p1H, p2H, p3H, p4H);

                }
            }

            // Return the filled reference state Jacobian
            return dSPdYRef;

        }

        private double[] computeShortPeriodTerms(final SpacecraftState state,
                                                 final DSSTForceModel force) {

            final AuxiliaryElements auxiliaryElements = new AuxiliaryElements(state.getOrbit(), 1);

            final List<ShortPeriodTerms> shortPeriodTerms = new ArrayList<ShortPeriodTerms>();
            final double[] parameters = force.getParameters();
            shortPeriodTerms.addAll(force.initializeShortPeriodTerms(auxiliaryElements, PropagationType.OSCULATING, parameters));
            force.updateShortPeriodTerms(parameters, state);

            final double[] shortPeriod = new double[6];
            for (ShortPeriodTerms spt : shortPeriodTerms) {
                final double[] spVariation = spt.value(state.getOrbit());
                for (int i = 0; i < spVariation.length; i++) {
                    shortPeriod[i] += spVariation[i];
                }
            }
            return shortPeriod;
        }

        /**
         * Fill state Jacobian column.
         */
        private void fillJacobianColumn(final DSSTPropagator propagator,
                                        final double[][] jacobian, final int column,
                                        final OrbitType orbitType, final double h,
                                        final SpacecraftState sM4h, final SpacecraftState sM3h,
                                        final SpacecraftState sM2h, final SpacecraftState sM1h,
                                        final SpacecraftState sP1h, final SpacecraftState sP2h,
                                        final SpacecraftState sP3h, final SpacecraftState sP4h) {
            final double[] aM4h = stateToArray(propagator, sM4h, orbitType)[0];
            final double[] aM3h = stateToArray(propagator, sM3h, orbitType)[0];
            final double[] aM2h = stateToArray(propagator, sM2h, orbitType)[0];
            final double[] aM1h = stateToArray(propagator, sM1h, orbitType)[0];
            final double[] aP1h = stateToArray(propagator, sP1h, orbitType)[0];
            final double[] aP2h = stateToArray(propagator, sP2h, orbitType)[0];
            final double[] aP3h = stateToArray(propagator, sP3h, orbitType)[0];
            final double[] aP4h = stateToArray(propagator, sP4h, orbitType)[0];
            for (int i = 0; i < jacobian.length; ++i) {
                jacobian[i][column] = ( -3 * (aP4h[i] - aM4h[i]) +
                                        32 * (aP3h[i] - aM3h[i]) -
                                       168 * (aP2h[i] - aM2h[i]) +
                                       672 * (aP1h[i] - aM1h[i])) / (840 * h);
            }
        }

        /**
         * Fill short period Jacobian column.
         */
        private void fillSPJacobianColumn(final DSSTPropagator propagator,
                                          final double[][] jacobian, final int column,
                                          final OrbitType orbitType, final double h,
                                          final double[] sM4h, final double[] sM3h,
                                          final double[] sM2h, final double[] sM1h,
                                          final double[] sP1h, final double[] sP2h,
                                          final double[] sP3h, final double[] sP4h) {
            for (int i = 0; i < jacobian.length; ++i) {
                jacobian[i][column] += ( -3 * (sP4h[i] - sM4h[i]) +
                                         32 * (sP3h[i] - sM3h[i]) -
                                        168 * (sP2h[i] - sM2h[i]) +
                                        672 * (sP1h[i] - sM1h[i])) / (840 * h);
            }
        }

        /**
         * Shift a spacecraft state.
         * @param propagator propagator
         * @param state state to shift
         * @param orbitType orbit type
         * @param delta delta to apply
         * @param column state matrix column
         * @return a shifted spacecraft state
         */
        private SpacecraftState shiftState(final DSSTPropagator propagator,
                                           final SpacecraftState state, final OrbitType orbitType,
                                           final double delta, final int column) {

            // Transform spacecraft state to array
            final double[][] array = stateToArray(propagator, state, orbitType);

            // Apply delta
            array[0][column] += delta;

            // Return the shifted state
            return arrayToState(propagator, array, orbitType, state.getFrame(), state.getDate(), state.getMu());

        }

        /** State to array transformation.
         * @param propagator propagator
         * @param state input state
         * @param orbitType orbit type
         * @return array representation of the input state
         */
        private double[][] stateToArray(final DSSTPropagator propagator,
                                        final SpacecraftState state, final OrbitType orbitType) {

            // Initialize array (value + derivative)
            final double[][] array = new double[2][6];

            // Map orbit to array
            orbitType.mapOrbitToArray(state.getOrbit(), propagator.getPositionAngleType(), array[0], array[1]);

            // Return the array representation of the spacecraft state
            return array;

        }

        /**
         * Array to state transformation.
         * @param propagator propagator
         * @param array input array
         * @param orbitType orbit type
         * @param frame spacecraft state related frame
         * @param date state epoch
         * @param mu mu
         * @return a spacecraft state representation of the array
         */
        private SpacecraftState arrayToState(final DSSTPropagator propagator,
                                             final double[][] array, final OrbitType orbitType,
                                             final Frame frame, final AbsoluteDate date, final double mu) {

            // Map array to orbit
            final Orbit orbit = orbitType.mapArrayToOrbit(array[0], array[1], propagator.getPositionAngleType(), date, mu, frame);

            // Return the spacecraft state
            return new SpacecraftState(orbit, MASS);

        }
    }
}

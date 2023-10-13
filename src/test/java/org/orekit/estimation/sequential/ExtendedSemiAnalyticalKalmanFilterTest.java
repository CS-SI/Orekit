package org.orekit.estimation.sequential;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataFilter;
import org.orekit.data.DataSource;
import org.orekit.data.GzipFilter;
import org.orekit.data.UnixCompressFilter;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.Position;
import org.orekit.files.ilrs.CPF;
import org.orekit.files.ilrs.CPF.CPFCoordinate;
import org.orekit.files.ilrs.CPF.CPFEphemeris;
import org.orekit.files.rinex.HatanakaCompressFilter;
import org.orekit.files.ilrs.CPFParser;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.SphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.models.earth.atmosphere.NRLMSISE00;
import org.orekit.models.earth.atmosphere.data.MarshallSolarActivityFutureEstimation;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.conversion.ClassicalRungeKuttaIntegratorBuilder;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.conversion.ODEIntegratorBuilder;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTNewtonianAttraction;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Validation against real data of the ESKF. This test is a short version of the one presented in:
 * "Cazabonne B., Bayard J., Journot M., and Cefola P. J., A Semi-analytical Approach for Orbit
 *  Determination based on Extended Kalman Filter, AAS Paper 21-614, AAS/AIAA Astrodynamics
 *  Specialist Conference, Big Sky, August 2021."
 */
public class ExtendedSemiAnalyticalKalmanFilterTest {

    /** Print. */
    private static boolean print;

    /** Header. */
    private static final String HEADER = "%-25s\t%16s\t%16s\t%16s";

    /** Data line. */
    private static final String DATA_LINE = "%-25s\t%16.9f\t%16.9f\t%16.9f";

    @Test
    public void testLageos() throws URISyntaxException, IOException {

        // Print
        print = false;

        // Configure Orekit data access
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        // Observations
        final CPFEphemeris observations = initializeObservations("orbit-determination/Lageos2/lageos2_cpf_160213_5441.sgf");

        // Central body
        final IERSConventions convention = IERSConventions.IERS_2010;
        final boolean         simpleEop  = true;
        final OneAxisEllipsoid centralBody = initializeBody(convention, simpleEop);

        // Gravity field
        final int degree = 20;
        final int order  = 20;
        final SphericalHarmonicsProvider gravityField = initializeGravityField(degree, order);

        // Initial orbit
        final PropagationType initialStateType = PropagationType.OSCULATING;
        final Orbit initialOrbit = initializeOrbit(observations, gravityField);

        // Initialize propagator
        final double  step    = 43200.0;
        final double  surface = 0.2831331;
        final double  mass    = 400.0;
        final boolean useDrag = false;
        final boolean useSrp  = true;
        final boolean useMoon = true;
        final boolean useSun  = true;
        final PropagatorBuilder propagator = initializePropagator(initialOrbit, centralBody, gravityField, step,
                                                                  mass, surface, useDrag, useSrp, useSun, useMoon,
                                                                  initialStateType);

        // Measurements
        final double sigma = 2.0;
        final List<ObservedMeasurement<?>> measurements = initializeMeasurements(observations, initialOrbit, sigma);

        // Covariance
        final RealMatrix cartesianP = MatrixUtils.createRealDiagonalMatrix(new double[] {
            100.0, 100.0, 100.0, 1.0e-3, 1.0e-3, 1.0e-3
        });

        // Jacobian of the orbital parameters w/r to Cartesian
        final Orbit orbit = OrbitType.EQUINOCTIAL.convertType(initialOrbit);
        final double[][] dYdC = new double[6][6];
        orbit.getJacobianWrtCartesian(PositionAngleType.TRUE, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);

        // Equinoctial initial covariance matrix
        final RealMatrix orbitalP = Jac.multiply(cartesianP.multiply(Jac.transpose()));
        final RealMatrix orbitalQ = MatrixUtils.createRealDiagonalMatrix(new double[] {1.0e-9, 1.0e-9, 1.0e-9, 1.0e-12, 1.0e-12, 1.0e-12});
        final CovarianceMatrixProvider provider = buildCovarianceProvider(orbitalP, orbitalQ);

        // Create estimator and run it
        final Observer observer = initializeEstimator(propagator, measurements, provider);

        // Verify
        final StreamingStatistics statX = observer.getXStatistics();
        final StreamingStatistics statY = observer.getYStatistics();
        final StreamingStatistics statZ = observer.getZStatistics();
        Assertions.assertEquals(0.0, statX.getMean(), 1.26e-4);
        Assertions.assertEquals(0.0, statY.getMean(), 1.18e-4);
        Assertions.assertEquals(0.0, statZ.getMean(), 1.29e-4);
        Assertions.assertEquals(0.0, statX.getMin(),  0.019); // It's a negative value
        Assertions.assertEquals(0.0, statY.getMin(),  0.018); // It's a negative value
        Assertions.assertEquals(0.0, statX.getMin(),  0.020); // It's a negative value
        Assertions.assertEquals(0.0, statX.getMax(),  0.031);
        Assertions.assertEquals(0.0, statY.getMax(),  0.029);
        Assertions.assertEquals(0.0, statX.getMax(),  0.033);

        // Check that "physical" matrices are null
        final KalmanEstimation estimation = observer.getEstimation();
        Assertions.assertNotNull(estimation.getPhysicalEstimatedState());
        Assertions.assertNotNull(estimation.getPhysicalInnovationCovarianceMatrix());
        Assertions.assertNotNull(estimation.getPhysicalKalmanGain());
        Assertions.assertNotNull(estimation.getPhysicalMeasurementJacobian());
        Assertions.assertNotNull(estimation.getPhysicalStateTransitionMatrix());

    }

    /**
     * Initialize the Position/Velocity observations.
     * @param fileName measurement file name
     * @return the ephemeris contained in the input file
     * @throws IOException if observations file cannot be read properly
     * @throws URISyntaxException if URI syntax is wrong
     */
    private CPFEphemeris initializeObservations(final String fileName) throws URISyntaxException, IOException {

        // Input in tutorial resources directory
        final String inputPath = ExtendedSemiAnalyticalKalmanFilterTest.class.getClassLoader().
                                 getResource(fileName).
                                 toURI().
                                 getPath();
        final File file = new File(inputPath);

        // Set up filtering for measurements files
        DataSource source = new DataSource(file.getName(), () -> new FileInputStream(new File(file.getParentFile(), file.getName())));
        for (final DataFilter filter : Arrays.asList(new GzipFilter(),
                                                     new UnixCompressFilter(),
                                                     new HatanakaCompressFilter())) {
            source = filter.filter(source);
        }

        // Return the CPF ephemeris for the wanted satellite
        final CPF cpfFile = new CPFParser().parse(source);
        return cpfFile.getSatellites().get(cpfFile.getHeader().getIlrsSatelliteId());

    }

    /**
     * Initialize the central body (i.e. the Earth).
     * @param convention IERS convention
     * @param simpleEop if true, tidal effects are ignored when interpolating EOP
     * @return a configured central body
     */
    private static OneAxisEllipsoid initializeBody(final IERSConventions convention, final boolean simpleEop) {
        // Return the configured body
        return new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                    Constants.WGS84_EARTH_FLATTENING,
                                    FramesFactory.getITRF(convention, simpleEop));
    }

    /**
     * Initialize the spherical harmonics provider.
     * @param degree degree
     * @param order order
     * @return a configured spherical harmonics provider
     */
    private static SphericalHarmonicsProvider initializeGravityField(final int degree, final int order) {
        return GravityFieldFactory.getUnnormalizedProvider(degree, order);
    }

    /**
     * Initialize initial guess.
     * <p>
     * Initial guess corresponds to the first orbit in the CPF file.
     * It is converted in EME2000 frame.
     * </p>
     * @param ephemeris CPF ephemeris
     * @param gravityField gravity field (used for the central attraction coefficient)
     * @return the configured orbit
     */
    private static Orbit initializeOrbit(final CPFEphemeris ephemeris, final SphericalHarmonicsProvider gravityField) {

        // Frame
        final Frame orbitFrame = FramesFactory.getEME2000();

        // Bounded propagator from the CPF file
        final BoundedPropagator bounded = ephemeris.getPropagator(new FrameAlignedProvider(ephemeris.getInertialFrame()));

        // Initial date
        final AbsoluteDate initialDate = bounded.getMinDate();

        // Initial orbit
        final TimeStampedPVCoordinates pvInitITRF = bounded.getPVCoordinates(initialDate, ephemeris.getFrame());
        final TimeStampedPVCoordinates pvInitInertial = ephemeris.getFrame().getTransformTo(orbitFrame, initialDate).
                                                                             transformPVCoordinates(pvInitITRF);

        // Return orbit (in J2000 frame)
        return new CartesianOrbit(new TimeStampedPVCoordinates(pvInitInertial.getDate(),
                                                               new Vector3D(pvInitInertial.getPosition().getX(),
                                                                            pvInitInertial.getPosition().getY(),
                                                                            pvInitInertial.getPosition().getZ()),
                                                               new Vector3D(pvInitInertial.getVelocity().getX(),
                                                                            pvInitInertial.getVelocity().getY(),
                                                                            pvInitInertial.getVelocity().getZ())),
                                  orbitFrame, gravityField.getMu());

    }

    /**
     * Initialize the propagator builder.
     * @param orbit initial guess
     * @param centralBody central body
     * @param gravityField gravity field
     * @param step fixed integration step
     * @param mass spacecraft mass (kg)
     * @param surface surface (m²)
     * @param useDrag true if drag acceleration must be added
     * @param useSrp true if acceleration due to the solar radiation pressure must be added
     * @param useSun true if gravitational acceleration due to the Sun attraction must be added
     * @param useMoon true if gravitational acceleration due to the Moon attraction must be added
     * @param initialStateType initial state type (MEAN or OSCULATING)
     * @return a configured propagator builder
     */
    private static PropagatorBuilder initializePropagator(final Orbit orbit,
                                                          final OneAxisEllipsoid centralBody,
                                                          final SphericalHarmonicsProvider gravityField,
                                                          final double step, final double mass, final double surface,
                                                          final boolean useDrag, final boolean useSrp,
                                                          final boolean useSun, final boolean useMoon,
                                                          final PropagationType initialStateType) {

        // Initialize numerical integrator
        final ODEIntegratorBuilder integrator = new ClassicalRungeKuttaIntegratorBuilder(step);

        // Initialize the builder
        final PropagatorBuilder builder;

        // Convert initial orbit in equinoctial elements
        final EquinoctialOrbit equinoctial = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(orbit);

        // Initialize the numerical builder
        final DSSTPropagatorBuilder propagator = new DSSTPropagatorBuilder(equinoctial, integrator, 1.0, PropagationType.MEAN, initialStateType);

        // Add the force models to the DSST propagator
        addDSSTForceModels(propagator, centralBody, gravityField, surface, useDrag, useSrp, useSun, useMoon);

        // Mass
        propagator.setMass(mass);

        // Set
        builder = propagator;

        // Reset the orbit
        builder.resetOrbit(equinoctial);

        // Return the fully configured propagator builder
        return builder;

    }

    /**
     * Add the force models to the DSST propagator.
     * @param propagator propagator
     * @param centralBody central body
     * @param gravityField gravity field
     * @param surface surface (m²)
     * @param useDrag true if drag acceleration must be added
     * @param useSrp true if acceleration due to the solar radiation pressure must be added
     * @param useSun true if gravitational acceleration due to the Sun attraction must be added
     * @param useMoon true if gravitational acceleration due to the Moon attraction must be added
     */
    private static void addDSSTForceModels(final DSSTPropagatorBuilder propagator,
                                           final OneAxisEllipsoid centralBody,
                                           final SphericalHarmonicsProvider gravityField,
                                           final double surface,
                                           final boolean useDrag, final boolean useSrp,
                                           final boolean useSun, final boolean useMoon) {

        // Drag
        if (useDrag) {

            // Atmosphere model
            final MarshallSolarActivityFutureEstimation msafe =
                            new MarshallSolarActivityFutureEstimation(MarshallSolarActivityFutureEstimation.DEFAULT_SUPPORTED_NAMES,
                                                                      MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
            final Atmosphere atmosphere = new NRLMSISE00(msafe, CelestialBodyFactory.getSun(), centralBody);

            // Drag force
            // Assuming spherical satellite
            final DSSTForceModel drag = new DSSTAtmosphericDrag(new DragForce(atmosphere, new IsotropicDrag(surface, 1.0)), gravityField.getMu());
            for (final ParameterDriver driver : drag.getParametersDrivers()) {
                if (driver.getName().equals(DragSensitive.DRAG_COEFFICIENT)) {
                    driver.setSelected(true);
                }
            }

            // Add the force model
            propagator.addForceModel(drag);

        }

        // Solar radiation pressure
        if (useSrp) {

            // Satellite model (spherical)
            final RadiationSensitive spacecraft = new IsotropicRadiationSingleCoefficient(surface, 1.13);

            // Solar radiation pressure
            final DSSTForceModel srp = new DSSTSolarRadiationPressure(CelestialBodyFactory.getSun(), centralBody, spacecraft, gravityField.getMu());
            for (final ParameterDriver driver : srp.getParametersDrivers()) {
                if (driver.getName().equals(RadiationSensitive.REFLECTION_COEFFICIENT)) {
                    //driver.setSelected(true);
                }
            }

            // Add the force model
            propagator.addForceModel(srp);

        }

        // Sun
        if (useSun) {
            propagator.addForceModel(new DSSTThirdBody(CelestialBodyFactory.getSun(), gravityField.getMu()));
        }

        // Moon
        if (useMoon) {
            propagator.addForceModel(new DSSTThirdBody(CelestialBodyFactory.getMoon(), gravityField.getMu()));
        }


        // Potential
        propagator.addForceModel(new DSSTTesseral(centralBody.getBodyFrame(), Constants.WGS84_EARTH_ANGULAR_VELOCITY, (UnnormalizedSphericalHarmonicsProvider) gravityField));
        propagator.addForceModel(new DSSTZonal((UnnormalizedSphericalHarmonicsProvider) gravityField));

        // Newton
        propagator.addForceModel(new DSSTNewtonianAttraction(gravityField.getMu()));

    }

    /**
     * Initialize the list of measurements.
     * @param ephemeris CPF ephemeris
     * @param orbit initial guess (used for orbit determination epoch)
     * @param sigma standard deviation for position measurement
     * @return the list of measurements
     */
    private static List<ObservedMeasurement<?>> initializeMeasurements(final CPFEphemeris ephemeris,
                                                                       final Orbit orbit,
                                                                       final double sigma) {

        // Satellite
        final ObservableSatellite satellite = new ObservableSatellite(0);

        // Initialize an empty list of measurements
        final List<ObservedMeasurement<?>> measurements = new ArrayList<>();

        // Loop on measurements
        for (final CPFCoordinate coordinate : ephemeris.getCoordinates()) {

            // Position in inertial frames
            final Vector3D posInertial = ephemeris.getFrame().getStaticTransformTo(orbit.getFrame(), coordinate.getDate()).
                            transformPosition(coordinate.getPosition());

            // Initialize measurement
            final Position measurement = new Position(coordinate.getDate(), posInertial, sigma, 1.0, satellite);

            // Add the measurement to the list
            measurements.add(measurement);

        }

        // Return the filled list
        return measurements;

    }

    /**
     * Initialize the estimator used for the orbit determination and run the estimation.
     * @param propagator orbit propagator
     * @param measurements list of measurements
     * @param provider covariance matrix provider
     */
    private static Observer initializeEstimator(final PropagatorBuilder propagator,
                                                final List<ObservedMeasurement<?>> measurements,
                                                final CovarianceMatrixProvider provider) {

       // Initialize builder
       final SemiAnalyticalKalmanEstimatorBuilder builder = new SemiAnalyticalKalmanEstimatorBuilder();

       // Add the propagation configuration
       builder.addPropagationConfiguration((DSSTPropagatorBuilder) propagator, provider);

       // Build filter
       final SemiAnalyticalKalmanEstimator estimator = builder.build();

       // Add observer
       final Observer observer = new Observer();
       estimator.setObserver(observer);

       // Estimation
       estimator.processMeasurements(measurements);

       // Return the observer
       return observer;

    }

    /**
     * Build the covariance matrix provider.
     * @param initialNoiseMatrix initial process noise
     * @param processNoiseMatrix constant process noise
     * @return the covariance matrix provider
     */
    private static CovarianceMatrixProvider buildCovarianceProvider(final RealMatrix initialNoiseMatrix, final RealMatrix processNoiseMatrix)  {
        // Return
        return new ConstantProcessNoise(initialNoiseMatrix, processNoiseMatrix);
    }

    /** Observer for Kalman estimation. */
    public static class Observer implements KalmanObserver {

        /** Statistics. */
        private StreamingStatistics statX;
        private StreamingStatistics statY;
        private StreamingStatistics statZ;

        /** Kalman estimation. */
        private KalmanEstimation estimation;

        /**
         * Constructor.
         */
        public Observer() {
            statX = new StreamingStatistics();
            statY = new StreamingStatistics();
            statZ = new StreamingStatistics();
            if (print) {
                String header = String.format(Locale.US, HEADER,
                        "Epoch", "X residual (m)", "Y residual (m)", "Z residual (m)");
                System.out.println(header);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void evaluationPerformed(final KalmanEstimation estimation) {

            // Estimated and observed measurements
            final EstimatedMeasurement<?> estimatedMeasurement = estimation.getCorrectedMeasurement();

            // Check
            if (estimatedMeasurement.getObservedMeasurement().getMeasurementType().equals(Position.MEASUREMENT_TYPE)) {

                if (estimatedMeasurement.getStatus() == EstimatedMeasurement.Status.REJECTED) {
                    if (print) {
                        System.out.println("REJECTED");
                    }
                } else {
                    final double[] estimated = estimatedMeasurement.getEstimatedValue();
                    final double[] observed  = estimatedMeasurement.getObservedValue();

                    // Calculate residuals
                    final double resX  = estimated[0] - observed[0];
                    final double resY  = estimated[1] - observed[1];
                    final double resZ  = estimated[2] - observed[2];
                    statX.addValue(resX);
                    statY.addValue(resY);
                    statZ.addValue(resZ);

                    if (print) {
                        // Add measurement line
                        final String line = String.format(Locale.US, DATA_LINE, estimatedMeasurement.getDate(), resX, resY, resZ);
                        System.out.println(line);
                    }

                }

            }

            this.estimation = estimation;

        }

        /**
         * Get the statistics on the X coordinate residuals.
         * @return the statistics on the X coordinate residuals
         */
        public StreamingStatistics getXStatistics() {
            if (print) {
                System.out.println("Min X res (m): " + statX.getMin() + " Max X res (m): " + statX.getMax() + " Mean X res (m): " + statX.getMean() + " STD: " + statX.getStandardDeviation());
            }
            return statX;
        }

        /**
         * Get the statistics on the Y coordinate residuals.
         * @return the statistics on the Y coordinate residuals
         */
        public StreamingStatistics getYStatistics() {
            if (print) {
                System.out.println("Min Y res (m): " + statY.getMin() + " Max Y res (m): " + statY.getMax() + " Mean Y res (m): " + statY.getMean() + " STD: " + statY.getStandardDeviation());
            }
            return statY;
        }

        /**
         * Get the statistics on the Z coordinate residuals.
         * @return the statistics on the Z coordinate residuals
         */
        public StreamingStatistics getZStatistics() {
            if (print) {
                System.out.println("Min Z res (m): " + statZ.getMin() + " Max Z res (m): " + statZ.getMax() + " Mean Z res (m): " + statZ.getMean() + " STD: " + statZ.getStandardDeviation());
            }
            return statZ;
        }

        /**
         * Get the Kalman estimation.
         * @return the Kalman estimation
         */
        public KalmanEstimation getEstimation() {
            return estimation;
        }

    }

}

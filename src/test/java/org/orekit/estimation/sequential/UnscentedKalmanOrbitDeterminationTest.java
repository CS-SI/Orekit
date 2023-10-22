/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import org.hipparchus.linear.RealVector;
import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MerweUnscentedTransform;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.bodies.CelestialBody;
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
import org.orekit.forces.ForceModel;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.forces.gravity.SolidTides;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.SphericalHarmonicsProvider;
import org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.models.earth.atmosphere.NRLMSISE00;
import org.orekit.models.earth.atmosphere.data.MarshallSolarActivityFutureEstimation;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.conversion.DormandPrince853IntegratorBuilder;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.conversion.ODEIntegratorBuilder;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

public class UnscentedKalmanOrbitDeterminationTest {

    /** Header. */
    private static final String HEADER = "%-25s\t%16s\t%16s\t%16s\t%16s\t%16s\t%16s\t%16s\t%16s\t%16s\t%16s\t%16s\t%16s\t%16s\t%16s\t%16s\t%16s\t%16s";

    /** Data line. */
    private static final String DATA_LINE = "%-25s\t%-16.6f\t%-16.6f\t%16.9f\t%-16.6f\t%-16.6f\t%16.9f\t%-16.6f\t%-16.6f\t%16.9f\t%16.9f\t%16.9f\t%16.9f\t%16.9f\t%16.9f\t%16.9f\t%16.9f\t%16.9f";

    /** Print. */
    private static boolean print;

    /**
     * Test the Lageos 2 orbit determination based on an unscented Kalman filter.
     * <p>
     * Lageos 2 positions from a CPF file are used as observations during the
     * estimation process.
     * </p>
     */
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
        final int degree = 16;
        final int order  = 16;
        final SphericalHarmonicsProvider gravityField = initializeGravityField(degree, order);

        // Initial orbit
        final Orbit initialOrbit = initializeOrbit(observations, gravityField);

        // Initialize propagator
        final double  minStep  = 0.001;
        final double  maxStep  = 300.0;
        final double  surface  = 0.2831331;
        final double  mass     = 405.38;
        final boolean useDrag  = false;
        final boolean useSrp   = true;
        final boolean useMoon  = true;
        final boolean useSun   = true;
        final boolean useTides = true;
        final PropagatorBuilder propagator = initializePropagator(initialOrbit, centralBody, gravityField,
                                                                  convention, simpleEop, minStep, maxStep,
                                                                  mass, surface, useDrag, useSrp,
                                                                  useSun, useMoon, useTides);

        // Measurements
        final double sigma = 1.0;
        final List<ObservedMeasurement<?>> measurements = initializeMeasurements(observations, initialOrbit, sigma);

        // Covariance
        final RealMatrix orbitalP = MatrixUtils.createRealDiagonalMatrix(new double[] {
            100.0, 100.0, 100.0, 1.0e-3, 1.0e-3, 1.0e-3
        });

        // Cartesian initial covariance matrix
        final RealMatrix orbitalQ = MatrixUtils.createRealDiagonalMatrix(new double[] {1.0e-6, 1.0e-6, 1.0e-6, 1.0e-9, 1.0e-9, 1.0e-9});
        final CovarianceMatrixProvider provider = buildCovarianceProvider(orbitalP, orbitalQ);

        // Create estimator and run it
        final Observer observer = initializeEstimator(propagator, measurements, provider);

        // Verify
        final KalmanEstimation    estimation = observer.getEstimation();
        final StreamingStatistics statX      = observer.getXStatistics();
        final StreamingStatistics statY      = observer.getYStatistics();
        final StreamingStatistics statZ      = observer.getZStatistics();
        Assertions.assertEquals(0.0, statX.getMean(), 1.39e-3);
        Assertions.assertEquals(0.0, statY.getMean(), 1.86e-4);
        Assertions.assertEquals(0.0, statZ.getMean(), 2.85e-4);
        Assertions.assertEquals(0.0, statX.getMin(),  0.031); // Value is negative
        Assertions.assertEquals(0.0, statY.getMin(),  0.028); // Value is negative
        Assertions.assertEquals(0.0, statZ.getMin(),  0.029); // Value is negative
        Assertions.assertEquals(0.0, statX.getMax(),  0.026);
        Assertions.assertEquals(0.0, statY.getMax(),  0.032);
        Assertions.assertEquals(0.0, statZ.getMax(),  0.027);

        // Verify the last estimated position
        final RealVector estimatedState = estimation.getPhysicalEstimatedState();
        final Vector3D ref       = ((Position) measurements.get(measurements.size() - 1)).getPosition();
        final Vector3D estimated = new Vector3D(estimatedState.getEntry(0),
                                                estimatedState.getEntry(1),
                                                estimatedState.getEntry(2));
        final double dP = 0.029;
        Assertions.assertEquals(0.0, Vector3D.distance(ref, estimated), dP);

        // Check that "physical" matrices are not null
        Assertions.assertNotNull(estimation.getPhysicalInnovationCovarianceMatrix());
        Assertions.assertNotNull(estimation.getPhysicalKalmanGain());

        // Verify that station transition and measurement matrices are null
        Assertions.assertNull(estimation.getPhysicalMeasurementJacobian());
        Assertions.assertNull(estimation.getPhysicalStateTransitionMatrix());

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
        final String inputPath = UnscentedKalmanOrbitDeterminationTest.class.getClassLoader().
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
        return GravityFieldFactory.getNormalizedProvider(degree, order);
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
        final BoundedPropagator bounded = ephemeris.getPropagator(new FrameAlignedProvider(orbitFrame));

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
     * @param convention IERS convention
     * @param simpleEop if true, tidal effects are ignored when interpolating EOP
     * @param minStep min integration step (s)
     * @param maxStep max integration step (s)
     * @param mass spacecraft mass (kg)
     * @param surface surface (m²)
     * @param useDrag true if drag acceleration must be added
     * @param useSrp true if acceleration due to the solar radiation pressure must be added
     * @param useSun true if gravitational acceleration due to the Sun attraction must be added
     * @param useMoon true if gravitational acceleration due to the Moon attraction must be added
     * @param useTides true if solid Earth tides must be added
     * @return a configured propagator builder
     */
    private static PropagatorBuilder initializePropagator(final Orbit orbit,
                                                          final OneAxisEllipsoid centralBody,
                                                          final SphericalHarmonicsProvider gravityField,
                                                          final IERSConventions convention, final boolean simpleEop,
                                                          final double minStep, final double maxStep,
                                                          final double mass, final double surface,
                                                          final boolean useDrag, final boolean useSrp,
                                                          final boolean useSun, final boolean useMoon,
                                                          final boolean useTides) {

        // Initialize numerical integrator
        final ODEIntegratorBuilder integrator = new DormandPrince853IntegratorBuilder(minStep, maxStep, 10.0);

        // Initialize the builder
        final PropagatorBuilder builder;

        // Initialize the numerical builder
        final NumericalPropagatorBuilder propagator = new NumericalPropagatorBuilder(orbit, integrator, PositionAngleType.MEAN, 10.0);

        // Add force models to the numerical propagator
        addNumericalForceModels(propagator, orbit, centralBody, gravityField, convention, simpleEop, surface, useDrag, useSrp, useSun, useMoon, useTides);

        // Mass
        propagator.setMass(mass);

        // Set
        builder = propagator;

        // Reset the orbit
        builder.resetOrbit(orbit);

        // Return the fully configured propagator builder
        return builder;

    }

    /**
     * Add the force models to the numerical propagator.
     * @param propagator propagator
     * @param initialOrbit initial orbit
     * @param centralBody central body
     * @param gravityField gravity field
     * @param convention IERS convention
     * @param simpleEop if true, tidal effects are ignored when interpolating EOP
     * @param surface surface of the satellite (m²)
     * @param useDrag true if drag acceleration must be added
     * @param useSrp true if acceleration due to the solar radiation pressure must be added
     * @param useSun true if gravitational acceleration due to the Sun attraction must be added
     * @param useMoon true if gravitational acceleration due to the Moon attraction must be added
     * @param useTides true if solid Earth tides must be added
     */
    private static void addNumericalForceModels(final NumericalPropagatorBuilder propagator,
                                                final Orbit initialOrbit,
                                                final OneAxisEllipsoid centralBody,
                                                final SphericalHarmonicsProvider gravityField,
                                                final IERSConventions convention, final boolean simpleEop,
                                                final double surface,
                                                final boolean useDrag, final boolean useSrp,
                                                final boolean useSun, final boolean useMoon,
                                                final boolean useTides) {

        // List of celestial bodies used for solid tides
        final List<CelestialBody> solidTidesBodies = new ArrayList<>();

        // Drag
        if (useDrag) {

            // Atmosphere model
            final MarshallSolarActivityFutureEstimation msafe =
                            new MarshallSolarActivityFutureEstimation(MarshallSolarActivityFutureEstimation.DEFAULT_SUPPORTED_NAMES,
                                                                      MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
            final Atmosphere atmosphere = new NRLMSISE00(msafe, CelestialBodyFactory.getSun(), centralBody);

            // Drag force
            // Assuming spherical satellite
            final ForceModel drag = new DragForce(atmosphere, new IsotropicDrag(surface, 1.0));
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
            final ForceModel srp = new SolarRadiationPressure(CelestialBodyFactory.getSun(), centralBody, spacecraft);
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
            solidTidesBodies.add(CelestialBodyFactory.getSun());
            propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun()));
        }

        // Moon
        if (useMoon) {
            solidTidesBodies.add(CelestialBodyFactory.getMoon());
            propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getMoon()));
        }

        // Solid Earth Tides
        if (useTides) {
            propagator.addForceModel(new SolidTides(centralBody.getBodyFrame(),
                                                    gravityField.getAe(), gravityField.getMu(),
                                                    gravityField.getTideSystem(), convention,
                                                    TimeScalesFactory.getUT1(convention, simpleEop),
                                                    solidTidesBodies.toArray(new CelestialBody[solidTidesBodies.size()])));
        }

        // Potential
        propagator.addForceModel(new HolmesFeatherstoneAttractionModel(centralBody.getBodyFrame(), (NormalizedSphericalHarmonicsProvider) gravityField));

        // Newton
        propagator.addForceModel(new NewtonianAttraction(gravityField.getMu()));

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
        final UnscentedKalmanEstimatorBuilder builder = new UnscentedKalmanEstimatorBuilder();

        // Add the propagation configuration
        builder.addPropagationConfiguration((NumericalPropagatorBuilder) propagator, provider);

        // Unscented transform provider
        builder.unscentedTransformProvider(new MerweUnscentedTransform(propagator.getSelectedNormalizedParameters().length));

        // Build filter
        final UnscentedKalmanEstimator estimator = builder.build();

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

        /** Statistics on X position residuals. */
        private StreamingStatistics statX;

        /** Statistics on Y position residuals. */
        private StreamingStatistics statY;

        /** Statistics on Z position residuals. */
        private StreamingStatistics statZ;

        /** Kalman estimation. */
        private KalmanEstimation estimation;

        /** Constructor. */
        public Observer() {
            statX = new StreamingStatistics();
            statY = new StreamingStatistics();
            statZ = new StreamingStatistics();
            if (print) {
                final String header = String.format(Locale.US, HEADER, "Epoch",
                                                    "X Observed (m)", "X Estimated (m)", "X residual (m)",
                                                    "Y Observed (m)", "Y Estimated (m)", "Y residual (m)",
                                                    "Z Observed (m)", "Z Estimated (m)", "Z residual (m)",
                                                    "Cov(0;0)", "Cov(1;1)", "Cov(2;2)", "Cov(3;3)", "Cov(4;4)", "Cov(5;5)",
                                                    "3D Pos Cov (m)", "3D Vel Cov (m)");
                System.out.println(header);
            }

        }

        /** {@inheritDoc} */
        @Override
        public void evaluationPerformed(final KalmanEstimation estimation) {

            // Estimated and observed measurements
            final EstimatedMeasurement<?> estimatedMeasurement = estimation.getCorrectedMeasurement();

            // Check
            if (estimatedMeasurement.getObservedMeasurement() instanceof Position) {

                if (estimatedMeasurement.getStatus() == EstimatedMeasurement.Status.REJECTED) {
                    if (print) {
                        System.out.println("REJECTED");
                    }
                } else {
                    final double[] estimated = estimatedMeasurement.getEstimatedValue();
                    final double[] observed  = estimatedMeasurement.getObservedValue();

                    // Observed
                    final double observedX = observed[0];
                    final double observedY = observed[1];
                    final double observedZ = observed[2];

                    // Estimated
                    final double estimatedX = estimated[0];
                    final double estimatedY = estimated[1];
                    final double estimatedZ = estimated[2];

                    // Calculate residuals
                    final double resX  = estimatedX - observedX;
                    final double resY  = estimatedY - observedY;
                    final double resZ  = estimatedZ - observedZ;
                    statX.addValue(resX);
                    statY.addValue(resY);
                    statZ.addValue(resZ);

                    if (print) {

                        // Covariance (diagonal elements)
                        final RealMatrix covariance = estimation.getPhysicalEstimatedCovarianceMatrix();
                        final double cov11 = covariance.getEntry(0, 0);
                        final double cov22 = covariance.getEntry(1, 1);
                        final double cov33 = covariance.getEntry(2, 2);
                        final double cov44 = covariance.getEntry(3, 3);
                        final double cov55 = covariance.getEntry(4, 4);
                        final double cov66 = covariance.getEntry(5, 5);
                        final double cPos  = FastMath.sqrt(cov11 + cov22 + cov33);
                        final double cVel  = FastMath.sqrt(cov44 + cov55 + cov66);

                        // Add measurement line
                        final String line = String.format(Locale.US, DATA_LINE, estimatedMeasurement.getDate(),
                                                          observedX, estimatedX, resX,
                                                          observedY, estimatedY, resY,
                                                          observedZ, estimatedZ, resZ,
                                                          cov11, cov22, cov33, cov44, cov55, cov66,
                                                          cPos, cVel);
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

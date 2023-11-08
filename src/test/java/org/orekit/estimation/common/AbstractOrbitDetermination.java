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
package org.orekit.estimation.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposer;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.optim.nonlinear.vector.leastsquares.GaussNewtonOptimizer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresOptimizer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresOptimizer.Optimum;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.SequentialGaussNewtonOptimizer;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MerweUnscentedTransform;
import org.hipparchus.util.Precision;
import org.orekit.KeyValueFileParser;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataFilter;
import org.orekit.data.DataSource;
import org.orekit.data.GzipFilter;
import org.orekit.data.UnixCompressFilter;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.estimation.leastsquares.SequentialBatchLSEstimator;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.MultiplexedMeasurement;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.Position;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.RangeRate;
import org.orekit.estimation.measurements.modifiers.AngularRadioRefractionModifier;
import org.orekit.estimation.measurements.modifiers.Bias;
import org.orekit.estimation.measurements.modifiers.DynamicOutlierFilter;
import org.orekit.estimation.measurements.modifiers.PhaseCentersRangeModifier;
import org.orekit.estimation.measurements.modifiers.OutlierFilter;
import org.orekit.estimation.measurements.modifiers.RangeIonosphericDelayModifier;
import org.orekit.estimation.measurements.modifiers.RangeRateIonosphericDelayModifier;
import org.orekit.estimation.measurements.modifiers.RangeTroposphericDelayModifier;
import org.orekit.estimation.measurements.modifiers.ShapiroRangeModifier;
import org.orekit.estimation.sequential.ConstantProcessNoise;
import org.orekit.estimation.sequential.KalmanEstimation;
import org.orekit.estimation.sequential.KalmanEstimator;
import org.orekit.estimation.sequential.KalmanEstimatorBuilder;
import org.orekit.estimation.sequential.KalmanObserver;
import org.orekit.estimation.sequential.UnscentedKalmanEstimator;
import org.orekit.estimation.sequential.UnscentedKalmanEstimatorBuilder;
import org.orekit.files.ilrs.CPF;
import org.orekit.files.ilrs.CPF.CPFCoordinate;
import org.orekit.files.ilrs.CPF.CPFEphemeris;
import org.orekit.files.ilrs.CPFParser;
import org.orekit.files.ilrs.CRD;
import org.orekit.files.ilrs.CRD.CRDDataBlock;
import org.orekit.files.ilrs.CRD.Meteo;
import org.orekit.files.ilrs.CRD.MeteorologicalMeasurement;
import org.orekit.files.ilrs.CRD.RangeMeasurement;
import org.orekit.files.ilrs.CRDHeader;
import org.orekit.files.ilrs.CRDHeader.RangeType;
import org.orekit.files.rinex.HatanakaCompressFilter;
import org.orekit.files.rinex.observation.ObservationData;
import org.orekit.files.rinex.observation.ObservationDataSet;
import org.orekit.files.rinex.observation.RinexObservation;
import org.orekit.files.rinex.observation.RinexObservationParser;
import org.orekit.files.ilrs.CRDParser;
import org.orekit.files.sinex.SinexLoader;
import org.orekit.files.sinex.Station;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.EOPHistory;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.antenna.FrequencyPattern;
import org.orekit.models.AtmosphericRefractionModel;
import org.orekit.models.earth.EarthITU453AtmosphereRefraction;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.models.earth.atmosphere.DTM2000;
import org.orekit.models.earth.atmosphere.data.MarshallSolarActivityFutureEstimation;
import org.orekit.models.earth.displacement.OceanLoading;
import org.orekit.models.earth.displacement.OceanLoadingCoefficientsBLQFactory;
import org.orekit.models.earth.displacement.StationDisplacement;
import org.orekit.models.earth.displacement.TidalDisplacement;
import org.orekit.models.earth.ionosphere.EstimatedIonosphericModel;
import org.orekit.models.earth.ionosphere.IonosphericMappingFunction;
import org.orekit.models.earth.ionosphere.IonosphericModel;
import org.orekit.models.earth.ionosphere.KlobucharIonoCoefficientsLoader;
import org.orekit.models.earth.ionosphere.KlobucharIonoModel;
import org.orekit.models.earth.ionosphere.SingleLayerModelMappingFunction;
import org.orekit.models.earth.troposphere.DiscreteTroposphericModel;
import org.orekit.models.earth.troposphere.EstimatedTroposphericModel;
import org.orekit.models.earth.troposphere.GlobalMappingFunctionModel;
import org.orekit.models.earth.troposphere.MappingFunction;
import org.orekit.models.earth.troposphere.MendesPavlisModel;
import org.orekit.models.earth.troposphere.NiellMappingFunctionModel;
import org.orekit.models.earth.troposphere.SaastamoinenModel;
import org.orekit.models.earth.troposphere.TimeSpanEstimatedTroposphericModel;
import org.orekit.models.earth.weather.GlobalPressureTemperatureModel;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.DormandPrince853IntegratorBuilder;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.conversion.ODEIntegratorBuilder;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;
import org.orekit.utils.TimeSpanMap.Span;

/** Base class for Orekit orbit determination tutorials.
 * @param <T> type of the propagator builder
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 * @author Julie Bayard
 */
public abstract class AbstractOrbitDetermination<T extends PropagatorBuilder> {

    /** Suffix for range bias. */
    private final String RANGE_BIAS_SUFFIX = "/range bias";

    /** Suffix for range rate bias. */
    private final String RANGE_RATE_BIAS_SUFFIX = "/range rate bias";

    /** Suffix for azimuth bias. */
    private final String AZIMUTH_BIAS_SUFFIX = "/az bias";

    /** Suffix for elevation bias. */
    private final String ELEVATION_BIAS_SUFFIX = "/el bias";

    /** CPF file mandatory key. */
    private final String CPF_MANDATORY_KEY = "cpf";

    /** Flag for range measurement use. */
    private boolean useRangeMeasurements;

    /** Flag for range rate measurement use. */
    private boolean useRangeRateMeasurements;

    /** Create a gravity field from input parameters.
     * @param parser input file parser
     * @throws NoSuchElementException if input parameters are missing
     */
    protected abstract void createGravityField(KeyValueFileParser<ParameterKey> parser)
        throws NoSuchElementException;

    /** Get the central attraction coefficient.
     * @return central attraction coefficient
     */
    protected abstract double getMu();

    /** Create a propagator builder from input parameters.
     * <p>
     * The advantage of using the DSST instead of the numerical
     * propagator is that it is possible to use greater values
     * for the minimum and maximum integration steps.
     * </p>
     * @param referenceOrbit reference orbit from which real orbits will be built
     * @param builder first order integrator builder
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @return propagator builder
     */
    protected abstract T createPropagatorBuilder(Orbit referenceOrbit,
                                                 ODEIntegratorBuilder builder,
                                                 double positionScale);

    /** Set satellite mass.
     * @param propagatorBuilder propagator builder
     * @param mass initial mass
     */
    protected abstract void setMass(T propagatorBuilder, double mass);

    /** Set gravity force model.
     * @param propagatorBuilder propagator builder
     * @param body central body
     * @return drivers for the force model
     */
    protected abstract List<ParameterDriver> setGravity(T propagatorBuilder, OneAxisEllipsoid body);

    /** Set third body attraction force model.
     * @param propagatorBuilder propagator builder
     * @param conventions IERS conventions to use
     * @param body central body
     * @param degree degree of the tide model to load
     * @param order order of the tide model to load
     * @return drivers for the force model
     */
    protected abstract List<ParameterDriver> setOceanTides(T propagatorBuilder, IERSConventions conventions,
                                                          OneAxisEllipsoid body, int degree, int order);

    /** Set third body attraction force model.
     * @param propagatorBuilder propagator builder
     * @param conventions IERS conventions to use
     * @param body central body
     * @param solidTidesBodies third bodies generating solid tides
     * @return drivers for the force model
     */
    protected abstract List<ParameterDriver> setSolidTides(T propagatorBuilder, IERSConventions conventions,
                                                           OneAxisEllipsoid body, CelestialBody[] solidTidesBodies);

    /** Set third body attraction force model.
     * @param propagatorBuilder propagator builder
     * @param thirdBody third body
     * @return drivers for the force model
     */
    protected abstract List<ParameterDriver> setThirdBody(T propagatorBuilder, CelestialBody thirdBody);

    /** Set drag force model.
     * @param propagatorBuilder propagator builder
     * @param atmosphere atmospheric model
     * @param spacecraft spacecraft model
     * @return drivers for the force model
     */
    protected abstract List<ParameterDriver> setDrag(T propagatorBuilder, Atmosphere atmosphere, DragSensitive spacecraft);

    /** Set solar radiation pressure force model.
     * @param propagatorBuilder propagator builder
     * @param sun Sun model
     * @param body central body (for shadow computation)
     * @param spacecraft spacecraft model
     * @return drivers for the force model
     */
    protected abstract List<ParameterDriver> setSolarRadiationPressure(T propagatorBuilder, CelestialBody sun,
                                                                       OneAxisEllipsoid body, RadiationSensitive spacecraft);

    /** Set Earth's albedo and infrared force model.
     * @param propagatorBuilder propagator builder
     * @param sun Sun model
     * @param equatorialRadius central body equatorial radius (for shadow computation)
     * @param angularResolution angular resolution in radians
     * @param spacecraft spacecraft model
     * @return drivers for the force model
     */
    protected abstract List<ParameterDriver> setAlbedoInfrared(T propagatorBuilder, CelestialBody sun,
                                                               double equatorialRadius, double angularResolution,
                                                               RadiationSensitive spacecraft);

    /** Set relativity force model.
     * @param propagatorBuilder propagator builder
     * @return drivers for the force model
     */
    protected abstract List<ParameterDriver> setRelativity(T propagatorBuilder);

    /** Set polynomial acceleration force model.
     * @param propagatorBuilder propagator builder
     * @param name name of the acceleration
     * @param direction normalized direction of the acceleration
     * @param degree polynomial degree
     * @return drivers for the force model
     */
    protected abstract List<ParameterDriver> setPolynomialAcceleration(T propagatorBuilder, String name,
                                                                       Vector3D direction, int degree);

    /** Set attitude provider.
     * @param propagatorBuilder propagator builder
     * @param attitudeProvider attitude provider
     */
    protected abstract void setAttitudeProvider(T propagatorBuilder, AttitudeProvider attitudeProvider);

    /** Run the batch least squares.
     * @param input input file
     * @param print if true, print logs
     * @throws IOException if input files cannot be read
     */
    protected ResultBatchLeastSquares runBLS(final File input, final boolean print) throws IOException {

        // read input parameters
        final KeyValueFileParser<ParameterKey> parser = new KeyValueFileParser<ParameterKey>(ParameterKey.class);
        try (FileInputStream fis = new FileInputStream(input)) {
            parser.parseInput(input.getAbsolutePath(), fis);
        }

        final RangeLog        rangeLog           = new RangeLog();
        final RangeRateLog    rangeRateLog       = new RangeRateLog();
        final AzimuthLog      azimuthLog         = new AzimuthLog();
        final ElevationLog    elevationLog       = new ElevationLog();
        final PositionOnlyLog positionOnlyLog    = new PositionOnlyLog();
        final PositionLog     positionLog        = new PositionLog();
        final VelocityLog     velocityLog        = new VelocityLog();

        // gravity field
        createGravityField(parser);

        // Orbit initial guess
        final Orbit initialGuess = createOrbit(parser, getMu());

        // IERS conventions
        final IERSConventions conventions;
        if (!parser.containsKey(ParameterKey.IERS_CONVENTIONS)) {
            conventions = IERSConventions.IERS_2010;
        } else {
            conventions = IERSConventions.valueOf("IERS_" + parser.getInt(ParameterKey.IERS_CONVENTIONS));
        }

        // central body
        final OneAxisEllipsoid body = createBody(parser);

        // propagator builder
        final T propagatorBuilder = configurePropagatorBuilder(parser, conventions, body, initialGuess);

        // estimator
        final BatchLSEstimator estimator = createEstimator(parser, propagatorBuilder);



        // read sinex files
        final SinexLoader                 stationPositionData      = readSinexFile(input, parser, ParameterKey.SINEX_POSITION_FILE);
        final SinexLoader                 stationEccData           = readSinexFile(input, parser, ParameterKey.SINEX_ECC_FILE);

        // use measurement types flags
        useRangeMeasurements                                       = parser.getBoolean(ParameterKey.USE_RANGE_MEASUREMENTS);
        useRangeRateMeasurements                                   = parser.getBoolean(ParameterKey.USE_RANGE_RATE_MEASUREMENTS);

        final Map<String, StationData>    stations                 = createStationsData(parser, initialGuess.getDate(),
                                                                                        stationPositionData, stationEccData, conventions, body);
        final PVData                      pvData                   = createPVData(parser);
        final ObservableSatellite         satellite                = createObservableSatellite(parser);
        final Bias<Range>                 satRangeBias             = createSatRangeBias(parser);
        final PhaseCentersRangeModifier   satAntennaRangeModifier  = createSatAntennaRangeModifier(parser);
        final ShapiroRangeModifier        shapiroRangeModifier     = createShapiroRangeModifier(parser);
        final Weights                     weights                  = createWeights(parser);
        final OutlierFilter<Range>        rangeOutliersManager     = createRangeOutliersManager(parser, false);
        final OutlierFilter<RangeRate>    rangeRateOutliersManager = createRangeRateOutliersManager(parser, false);
        final OutlierFilter<AngularAzEl>  azElOutliersManager      = createAzElOutliersManager(parser, false);
        final OutlierFilter<PV>           pvOutliersManager        = createPVOutliersManager(parser, false);

        // measurements
        final List<ObservedMeasurement<?>> independentMeasurements = new ArrayList<ObservedMeasurement<?>>();
        for (final String fileName : parser.getStringsList(ParameterKey.MEASUREMENTS_FILES, ',')) {

            // set up filtering for measurements files
            DataSource nd = new DataSource(fileName, () -> new FileInputStream(new File(input.getParentFile(), fileName)));
            for (final DataFilter filter : Arrays.asList(new GzipFilter(),
                                                         new UnixCompressFilter(),
                                                         new HatanakaCompressFilter())) {
                nd = filter.filter(nd);
            }

            if (Pattern.matches(RinexObservationParser.DEFAULT_RINEX_2_NAMES, nd.getName()) ||
                Pattern.matches(RinexObservationParser.DEFAULT_RINEX_3_NAMES, nd.getName())) {
                // the measurements come from a Rinex file
                independentMeasurements.addAll(readRinex(nd,
                                                         parser.getString(ParameterKey.SATELLITE_ID_IN_RINEX_FILES),
                                                         stations, satellite, satRangeBias, satAntennaRangeModifier, weights,
                                                         rangeOutliersManager, rangeRateOutliersManager, shapiroRangeModifier));
            } else if (Pattern.matches(CRDParser.DEFAULT_CRD_SUPPORTED_NAMES, nd.getName())) {
                // the measurements come from a CRD file
                independentMeasurements.addAll(readCrd(nd, stations, parser, satellite, satRangeBias,
                                                       weights, rangeOutliersManager, shapiroRangeModifier));
            } else if (fileName.contains(CPF_MANDATORY_KEY)) {
                // Position measurements in a CPF file
                independentMeasurements.addAll(readCpf(nd, satellite, initialGuess));
            } else {
                // the measurements come from an Orekit custom file
                independentMeasurements.addAll(readMeasurements(nd,
                                                                stations, pvData, satellite,
                                                                satRangeBias, satAntennaRangeModifier, weights,
                                                                rangeOutliersManager,
                                                                rangeRateOutliersManager,
                                                                azElOutliersManager,
                                                                pvOutliersManager));
            }

        }
        final List<ObservedMeasurement<?>> multiplexed = multiplexMeasurements(independentMeasurements, 1.0e-9);
        for (ObservedMeasurement<?> measurement : multiplexed) {
            estimator.addMeasurement(measurement);
        }

        // estimate orbit
        if (print) {
            final String header = "iteration evaluations      ΔP(m)        ΔV(m/s)           RMS          nb Range    nb Range-rate nb Angular   nb Position     nb PV%n";
            estimator.setObserver(new BatchLeastSquaresObserver(initialGuess, estimator, header, print));
        }
        final Orbit estimated = estimator.estimate()[0].getInitialState().getOrbit();

        // compute some statistics
        for (final Map.Entry<ObservedMeasurement<?>, EstimatedMeasurement<?>> entry : estimator.getLastEstimations().entrySet()) {
            logEvaluation(entry.getValue(),
                          rangeLog, rangeRateLog, azimuthLog, elevationLog, positionOnlyLog, positionLog, velocityLog);
        }

        final ParameterDriversList propagatorParameters   = estimator.getPropagatorParametersDrivers(true);
        final ParameterDriversList measurementsParameters = estimator.getMeasurementsParametersDrivers(true);
        return new ResultBatchLeastSquares(propagatorParameters, measurementsParameters,
                                           estimator.getIterationsCount(), estimator.getEvaluationsCount(), estimated.getPVCoordinates(),
                                           rangeLog.createStatisticsSummary(),  rangeRateLog.createStatisticsSummary(),
                                           azimuthLog.createStatisticsSummary(),  elevationLog.createStatisticsSummary(),
                                           positionLog.createStatisticsSummary(),  velocityLog.createStatisticsSummary(),
                                           estimator.getPhysicalCovariances(1.0e-10));

    }

    /** Run the sequential batch least squares.
     * @param print if true, print logs
     * @throws IOException if input files cannot be read
     */
    protected ResultSequentialBatchLeastSquares runSequentialBLS(final File inputModel, final boolean print) throws IOException {

        // read input parameters
        final KeyValueFileParser<ParameterKey> parser = new KeyValueFileParser<ParameterKey>(ParameterKey.class);
        try (FileInputStream fis = new FileInputStream(inputModel)) {
            parser.parseInput(inputModel.getAbsolutePath(), fis);
        }

        final RangeLog        rangeLog           = new RangeLog();
        final RangeRateLog    rangeRateLog       = new RangeRateLog();
        final AzimuthLog      azimuthLog         = new AzimuthLog();
        final ElevationLog    elevationLog       = new ElevationLog();
        final PositionOnlyLog positionOnlyLog    = new PositionOnlyLog();
        final PositionLog     positionLog        = new PositionLog();
        final VelocityLog     velocityLog        = new VelocityLog();

        // gravity field
        createGravityField(parser);

        // Orbit initial guess
        final Orbit initialGuess = createOrbit(parser, getMu());

        // IERS conventions
        final IERSConventions conventions;
        if (!parser.containsKey(ParameterKey.IERS_CONVENTIONS)) {
            conventions = IERSConventions.IERS_2010;
        } else {
            conventions = IERSConventions.valueOf("IERS_" + parser.getInt(ParameterKey.IERS_CONVENTIONS));
        }

        // central body
        final OneAxisEllipsoid body = createBody(parser);

        // propagator builder
        final T propagatorBuilder = configurePropagatorBuilder(parser, conventions, body, initialGuess);

        // estimator
        BatchLSEstimator estimator = createEstimator(parser, propagatorBuilder);

        final ObservableSatellite satellite = createObservableSatellite(parser);

        // measurements
        List<ObservedMeasurement<?>> independentMeasurements = new ArrayList<>();
        for (final String fileName : parser.getStringsList(ParameterKey.MEASUREMENTS_FILES, ',')) {

            // set up filtering for measurements files
            DataSource nd = new DataSource(fileName, () -> new FileInputStream(new File(inputModel.getParentFile(), fileName)));
            for (final DataFilter filter : Arrays.asList(new GzipFilter(),
                                                         new UnixCompressFilter(),
                                                         new HatanakaCompressFilter())) {
                nd = filter.filter(nd);
            }

            if (fileName.contains(CPF_MANDATORY_KEY)) {
                // Position measurements in a CPF file
                independentMeasurements.addAll(readCpf(nd, satellite, initialGuess));
            }

        }

        // add measurements to the estimator
        List<ObservedMeasurement<?>> multiplexed = multiplexMeasurements(independentMeasurements, 1.0e-9);
        for (ObservedMeasurement<?> measurement : multiplexed) {
            estimator.addMeasurement(measurement);
        }

        if (print) {
            final String headerBLS = "\nBatch Least Square Estimator :\n"
                            + "iteration evaluations      ΔP(m)        ΔV(m/s)           RMS          nb Range    nb Range-rate nb Angular   nb Position     nb PV%n";
            estimator.setObserver(new BatchLeastSquaresObserver(initialGuess, estimator, headerBLS, print));
        }

        // perform first estimation
        final Orbit estimatedBLS = estimator.estimate()[0].getInitialState().getOrbit();
        final int iterationCount = estimator.getIterationsCount();
        final int evalutionCount = estimator.getEvaluationsCount();
        final RealMatrix covariance = estimator.getPhysicalCovariances(1.0e-10);

        Optimum BLSEvaluation = estimator.getOptimum();

        // read second measurements file to build the sequential batch least squares
        estimator = createSequentialEstimator(BLSEvaluation, parser, propagatorBuilder);

        // measurements
        independentMeasurements = new ArrayList<>();
        for (final String fileName : parser.getStringsList(ParameterKey.MEASUREMENTS_FILES_SEQUENTIAL, ',')) {

            // set up filtering for measurements files
            DataSource nd = new DataSource(fileName, () -> new FileInputStream(new File(inputModel.getParentFile(), fileName)));
            for (final DataFilter filter : Arrays.asList(new GzipFilter(),
                                                         new UnixCompressFilter(),
                                                         new HatanakaCompressFilter())) {
                nd = filter.filter(nd);
            }

            if (fileName.contains(CPF_MANDATORY_KEY)) {
                // Position measurements in a CPF file
                independentMeasurements.addAll(readCpf(nd, satellite, initialGuess));
            }

        }

        // add measurements to the estimator
        multiplexed = multiplexMeasurements(independentMeasurements, 1.0e-9);
        for (ObservedMeasurement<?> measurement : multiplexed) {
            estimator.addMeasurement(measurement);
        }

        if (print) {
            final String headerSBLS = "\nSequentiel Batch Least Square Estimator :\n"
                            + "iteration evaluations      ΔP(m)        ΔV(m/s)           RMS          nb Range    nb Range-rate nb Angular   nb Position     nb PV%n";

            estimator.setObserver(new BatchLeastSquaresObserver(initialGuess, estimator, headerSBLS, print));
        }

        final Orbit estimatedSequentialBLS = estimator.estimate()[0].getInitialState().getOrbit();

        // compute some statistics
        for (final Map.Entry<ObservedMeasurement<?>, EstimatedMeasurement<?>> entry : estimator.getLastEstimations().entrySet()) {
            logEvaluation(entry.getValue(),
                          rangeLog, rangeRateLog, azimuthLog, elevationLog, positionOnlyLog, positionLog, velocityLog);
        }

        final ParameterDriversList propagatorParameters   = estimator.getPropagatorParametersDrivers(true);
        final ParameterDriversList measurementsParameters = estimator.getMeasurementsParametersDrivers(true);

        return new ResultSequentialBatchLeastSquares(propagatorParameters, measurementsParameters,
                                           iterationCount, evalutionCount, estimatedBLS.getPVCoordinates(),
                                           positionLog.createStatisticsSummary(),
                                           covariance,
                                           estimator.getIterationsCount(), estimator.getEvaluationsCount(),
                                           estimatedSequentialBLS.getPVCoordinates(), positionLog.createStatisticsSummary(),
                                           estimator.getPhysicalCovariances(1.0e-10));

    }

    /**
     * Run the Kalman filter estimation.
     * @param input Input configuration file
     * @param orbitType Orbit type to use (calculation and display)
     * @param print Choose whether the results are printed on console or not
     * @param cartesianOrbitalP Orbital part of the initial covariance matrix in Cartesian formalism
     * @param cartesianOrbitalQ Orbital part of the process noise matrix in Cartesian formalism
     * @param propagationP Propagation part of the initial covariance matrix
     * @param propagationQ Propagation part of the process noise matrix
     * @param measurementP Measurement part of the initial covariance matrix
     * @param measurementQ Measurement part of the process noise matrix
     */
    protected ResultKalman runKalman(final File input, final OrbitType orbitType, final boolean print,
                                     final RealMatrix cartesianOrbitalP, final RealMatrix cartesianOrbitalQ,
                                     final RealMatrix propagationP, final RealMatrix propagationQ,
                                     final RealMatrix measurementP, final RealMatrix measurementQ,
                                     final Boolean isUnscented)
        throws IOException {

        // Read input parameters
        KeyValueFileParser<ParameterKey> parser = new KeyValueFileParser<ParameterKey>(ParameterKey.class);
        parser.parseInput(input.getAbsolutePath(), new FileInputStream(input));

        // Log files
        final RangeLog        rangeLog        = new RangeLog();
        final RangeRateLog    rangeRateLog    = new RangeRateLog();
        final AzimuthLog      azimuthLog      = new AzimuthLog();
        final ElevationLog    elevationLog    = new ElevationLog();
        final PositionOnlyLog positionOnlyLog = new PositionOnlyLog();
        final PositionLog     positionLog     = new PositionLog();
        final VelocityLog     velocityLog     = new VelocityLog();

        // Gravity field
        createGravityField(parser);

        // Orbit initial guess
        Orbit initialGuess = createOrbit(parser, getMu());

        // Convert to desired orbit type
        initialGuess = orbitType.convertType(initialGuess);

        // IERS conventions
        final IERSConventions conventions;
        if (!parser.containsKey(ParameterKey.IERS_CONVENTIONS)) {
            conventions = IERSConventions.IERS_2010;
        } else {
            conventions = IERSConventions.valueOf("IERS_" + parser.getInt(ParameterKey.IERS_CONVENTIONS));
        }

        // Central body
        final OneAxisEllipsoid body = createBody(parser);

        // Propagator builder
        final T propagatorBuilder =
                        configurePropagatorBuilder(parser, conventions, body, initialGuess);

        // read sinex files
        final SinexLoader                 stationPositionData      = readSinexFile(input, parser, ParameterKey.SINEX_POSITION_FILE);
        final SinexLoader                 stationEccData           = readSinexFile(input, parser, ParameterKey.SINEX_ECC_FILE);

        // use measurement types flags
        useRangeMeasurements                                       = parser.getBoolean(ParameterKey.USE_RANGE_MEASUREMENTS);
        useRangeRateMeasurements                                   = parser.getBoolean(ParameterKey.USE_RANGE_RATE_MEASUREMENTS);

        final Map<String, StationData>    stations                 = createStationsData(parser, initialGuess.getDate(),
                                                                                        stationPositionData, stationEccData, conventions, body);
        final PVData                      pvData                   = createPVData(parser);
        final ObservableSatellite         satellite                = createObservableSatellite(parser);
        final Bias<Range>                 satRangeBias             = createSatRangeBias(parser);
        final PhaseCentersRangeModifier satAntennaRangeModifier  = createSatAntennaRangeModifier(parser);
        final ShapiroRangeModifier        shapiroRangeModifier     = createShapiroRangeModifier(parser);
        final Weights                     weights                  = createWeights(parser);
        final OutlierFilter<Range>        rangeOutliersManager     = createRangeOutliersManager(parser, true);
        final OutlierFilter<RangeRate>    rangeRateOutliersManager = createRangeRateOutliersManager(parser, true);
        final OutlierFilter<AngularAzEl>  azElOutliersManager      = createAzElOutliersManager(parser, true);
        final OutlierFilter<PV>           pvOutliersManager        = createPVOutliersManager(parser, true);

        // measurements
        final List<ObservedMeasurement<?>> independentMeasurements = new ArrayList<ObservedMeasurement<?>>();
        for (final String fileName : parser.getStringsList(ParameterKey.MEASUREMENTS_FILES, ',')) {

            // set up filtering for measurements files
            DataSource nd = new DataSource(fileName,
                                         () -> new FileInputStream(new File(input.getParentFile(), fileName)));
            for (final DataFilter filter : Arrays.asList(new GzipFilter(),
                                                         new UnixCompressFilter(),
                                                         new HatanakaCompressFilter())) {
                nd = filter.filter(nd);
            }

            if (Pattern.matches(RinexObservationParser.DEFAULT_RINEX_2_NAMES, nd.getName()) ||
                Pattern.matches(RinexObservationParser.DEFAULT_RINEX_3_NAMES, nd.getName())) {
                // the measurements come from a Rinex file
                independentMeasurements.addAll(readRinex(nd,
                                                         parser.getString(ParameterKey.SATELLITE_ID_IN_RINEX_FILES),
                                                         stations, satellite, satRangeBias, satAntennaRangeModifier, weights,
                                                         rangeOutliersManager, rangeRateOutliersManager, shapiroRangeModifier));
            } else if (Pattern.matches(CRDParser.DEFAULT_CRD_SUPPORTED_NAMES, nd.getName())) {
                // the measurements come from a CRD file
                independentMeasurements.addAll(readCrd(nd, stations, parser, satellite, satRangeBias,
                                                       weights, rangeOutliersManager, shapiroRangeModifier));
            } else {
                // the measurements come from an Orekit custom file
                independentMeasurements.addAll(readMeasurements(nd,
                                                                stations, pvData, satellite,
                                                                satRangeBias, satAntennaRangeModifier, weights,
                                                                rangeOutliersManager,
                                                                rangeRateOutliersManager,
                                                                azElOutliersManager,
                                                                pvOutliersManager));
            }

        }

        final List<ObservedMeasurement<?>> multiplexed = multiplexMeasurements(independentMeasurements, 1.0e-9);

        // Building the Kalman filter:
        // - Gather the estimated measurement parameters in a list
        // - Prepare the initial covariance matrix and the process noise matrix
        // - Build the Kalman filter
        // --------------------------------------------------------------------

        // Build the list of estimated measurements
        final ParameterDriversList estimatedMeasurementsParameters = new ParameterDriversList();
        for (ObservedMeasurement<?> measurement : multiplexed) {
            final List<ParameterDriver> drivers = measurement.getParametersDrivers();
            for (ParameterDriver driver : drivers) {
                if (driver.isSelected()) {
                    // Add the driver
                    estimatedMeasurementsParameters.add(driver);
                }
            }
        }
        // Sort the list lexicographically
        estimatedMeasurementsParameters.sort();

        // Orbital covariance matrix initialization
        // Jacobian of the orbital parameters w/r to Cartesian
        final double[][] dYdC = new double[6][6];
        initialGuess.getJacobianWrtCartesian(propagatorBuilder.getPositionAngleType(), dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);
        RealMatrix orbitalP = Jac.multiply(cartesianOrbitalP.multiply(Jac.transpose()));

        // Orbital process noise matrix
        RealMatrix orbitalQ = Jac.multiply(cartesianOrbitalQ.multiply(Jac.transpose()));


        // Build the full covariance matrix and process noise matrix
        final int nbPropag = (propagationP != null)?propagationP.getRowDimension():0;
        final int nbMeas = (measurementP != null)?measurementP.getRowDimension():0;
        final RealMatrix initialP = MatrixUtils.createRealMatrix(6 + nbPropag,
                                                                 6 + nbPropag);
        final RealMatrix Q = MatrixUtils.createRealMatrix(6 + nbPropag,
                                                          6 + nbPropag);
        // Orbital part
        initialP.setSubMatrix(orbitalP.getData(), 0, 0);
        Q.setSubMatrix(orbitalQ.getData(), 0, 0);

        // Propagation part
        if (propagationP != null) {
            initialP.setSubMatrix(propagationP.getData(), 6, 6);
            Q.setSubMatrix(propagationQ.getData(), 6, 6);
        }

        // Build the Kalman
        if (isUnscented) {
            // Unscented 
            final UnscentedKalmanEstimatorBuilder kalmanBuilder = new UnscentedKalmanEstimatorBuilder().
                    addPropagationConfiguration((NumericalPropagatorBuilder) propagatorBuilder, new ConstantProcessNoise(initialP, Q));
            if (measurementP != null) {
                // Measurement part
                kalmanBuilder.estimatedMeasurementsParameters(estimatedMeasurementsParameters, new ConstantProcessNoise(measurementP, measurementQ));
            }
            // Unscented
            final UnscentedKalmanEstimator kalman = kalmanBuilder.unscentedTransformProvider(new MerweUnscentedTransform(6 + nbPropag + nbMeas)).build();
            Observer observer = new Observer(print, rangeLog, rangeRateLog, azimuthLog, elevationLog, positionOnlyLog, positionLog, velocityLog);
            // Add an observer
            kalman.setObserver(observer);
            // Process the list measurements 
            final Orbit estimated = kalman.processMeasurements(multiplexed)[0].getInitialState().getOrbit();


            // Process the list measurements 

            // Get the last estimated physical covariances
            final RealMatrix covarianceMatrix = kalman.getPhysicalEstimatedCovarianceMatrix();

            // Parameters and measurements.
            final ParameterDriversList propagationParameters   = kalman.getPropagationParametersDrivers(true);
            final ParameterDriversList measurementsParameters = kalman.getEstimatedMeasurementsParameters();

            // Eventually, print parameter changes, statistics and covariances
            if (print) {
                
                // Display parameter change for non orbital drivers
                int length = 0;
                for (final ParameterDriver parameterDriver : propagationParameters.getDrivers()) {
                    length = FastMath.max(length, parameterDriver.getName().length());
                }
                for (final ParameterDriver parameterDriver : measurementsParameters.getDrivers()) {
                    length = FastMath.max(length, parameterDriver.getName().length());
                }
                if (propagationParameters.getNbParams() > 0) {
                    displayParametersChanges(System.out, "Estimated propagator parameters changes: ",
                                             true, length, propagationParameters);
                }
                if (measurementsParameters.getNbParams() > 0) {
                    displayParametersChanges(System.out, "Estimated measurements parameters changes: ",
                                             true, length, measurementsParameters);
                }
                // Measurements statistics summary
                System.out.println("");
                rangeLog.displaySummary(System.out);
                rangeRateLog.displaySummary(System.out);
                azimuthLog.displaySummary(System.out);
                elevationLog.displaySummary(System.out);
                positionOnlyLog.displaySummary(System.out);
                positionLog.displaySummary(System.out);
                velocityLog.displaySummary(System.out);
                
                // Covariances and sigmas
                displayFinalCovariances(System.out, kalman);
            }

            // Instantiation of the results
            return new ResultKalman(propagationParameters, measurementsParameters,
                                    kalman.getCurrentMeasurementNumber(), estimated.getPVCoordinates(),
                                    rangeLog.createStatisticsSummary(),  rangeRateLog.createStatisticsSummary(),
                                    azimuthLog.createStatisticsSummary(),  elevationLog.createStatisticsSummary(),
                                    positionLog.createStatisticsSummary(),  velocityLog.createStatisticsSummary(),
                                    covarianceMatrix);
        
        } else {
            // Extended 
            final KalmanEstimatorBuilder kalmanBuilder = new KalmanEstimatorBuilder().
                    addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q));
            if (measurementP != null) {
                // Measurement part
                kalmanBuilder.estimatedMeasurementsParameters(estimatedMeasurementsParameters, new ConstantProcessNoise(measurementP, measurementQ));
            }
            // Extended
            final KalmanEstimator kalman = kalmanBuilder.build();
            Observer observer = new Observer(print, rangeLog, rangeRateLog, azimuthLog, elevationLog, positionOnlyLog, positionLog, velocityLog);
            // Add an observer
            kalman.setObserver(observer);
            
            // Process the list measurements 
            final Orbit estimated = kalman.processMeasurements(multiplexed)[0].getInitialState().getOrbit();

            // Get the last estimated physical covariances
            final RealMatrix covarianceMatrix = kalman.getPhysicalEstimatedCovarianceMatrix();

            // Parameters and measurements.
            final ParameterDriversList propagationParameters   = kalman.getPropagationParametersDrivers(true);
            final ParameterDriversList measurementsParameters = kalman.getEstimatedMeasurementsParameters();

            // Eventually, print parameter changes, statistics and covariances
            if (print) {
                
                // Display parameter change for non orbital drivers
                int length = 0;
                for (final ParameterDriver parameterDriver : propagationParameters.getDrivers()) {
                    length = FastMath.max(length, parameterDriver.getName().length());
                }
                for (final ParameterDriver parameterDriver : measurementsParameters.getDrivers()) {
                    length = FastMath.max(length, parameterDriver.getName().length());
                }
                if (propagationParameters.getNbParams() > 0) {
                    displayParametersChanges(System.out, "Estimated propagator parameters changes: ",
                                             true, length, propagationParameters);
                }
                if (measurementsParameters.getNbParams() > 0) {
                    displayParametersChanges(System.out, "Estimated measurements parameters changes: ",
                                             true, length, measurementsParameters);

                    // Measurements statistics summary
                    System.out.println("");
                    rangeLog.displaySummary(System.out);
                    rangeRateLog.displaySummary(System.out);
                    azimuthLog.displaySummary(System.out);
                    elevationLog.displaySummary(System.out);
                    positionOnlyLog.displaySummary(System.out);
                    positionLog.displaySummary(System.out);
                    velocityLog.displaySummary(System.out);
                    
                    // Covariances and sigmas
                    displayFinalCovariances(System.out, kalman);

                }

            }
            

            // Instantiation of the results
            return new ResultKalman(propagationParameters, measurementsParameters,
                                    kalman.getCurrentMeasurementNumber(), estimated.getPVCoordinates(),
                                    rangeLog.createStatisticsSummary(),  rangeRateLog.createStatisticsSummary(),
                                    azimuthLog.createStatisticsSummary(),  elevationLog.createStatisticsSummary(),
                                    positionLog.createStatisticsSummary(),  velocityLog.createStatisticsSummary(),
                                    covarianceMatrix);
        }

    }

     /**
      * Use the physical models in the input file
      * Incorporate the initial reference values
      * And run the propagation until the last measurement to get the reference orbit at the same date
      * as the Kalman filter
      * @param input Input configuration file
      * @param orbitType Orbit type to use (calculation and display)
      * @param refPosition Initial reference position
      * @param refVelocity Initial reference velocity
      * @param refPropagationParameters Reference propagation parameters
      * @param finalDate The final date to usefinal dateame date as the Kalman filter
      * @throws IOException Input file cannot be opened
      */
     protected Orbit runReference(final File input, final OrbitType orbitType,
                                  final Vector3D refPosition, final Vector3D refVelocity,
                                  final ParameterDriversList refPropagationParameters,
                                  final AbsoluteDate finalDate) throws IOException {

         // Read input parameters
         KeyValueFileParser<ParameterKey> parser = new KeyValueFileParser<ParameterKey>(ParameterKey.class);
         parser.parseInput(input.getAbsolutePath(), new FileInputStream(input));

         // Gravity field
         createGravityField(parser);

         // Orbit initial guess
         Orbit initialRefOrbit = new CartesianOrbit(new PVCoordinates(refPosition, refVelocity),
                                                    parser.getInertialFrame(ParameterKey.INERTIAL_FRAME),
                                                    parser.getDate(ParameterKey.ORBIT_DATE,
                                                                   TimeScalesFactory.getUTC()),
                                                    getMu());

         // Convert to desired orbit type
         initialRefOrbit = orbitType.convertType(initialRefOrbit);

         // IERS conventions
         final IERSConventions conventions;
         if (!parser.containsKey(ParameterKey.IERS_CONVENTIONS)) {
             conventions = IERSConventions.IERS_2010;
         } else {
             conventions = IERSConventions.valueOf("IERS_" + parser.getInt(ParameterKey.IERS_CONVENTIONS));
         }

         // Central body
         final OneAxisEllipsoid body = createBody(parser);

         // Propagator builder
         final T propagatorBuilder =
                         configurePropagatorBuilder(parser, conventions, body, initialRefOrbit);

         // Force the selected propagation parameters to their reference values
         if (refPropagationParameters != null) {
             for (DelegatingDriver refDriver : refPropagationParameters.getDrivers()) {
                 for (DelegatingDriver driver : propagatorBuilder.getPropagationParametersDrivers().getDrivers()) {
                     if (driver.getName().equals(refDriver.getName())) {
                         for (Span<Double> span = driver.getValueSpanMap().getFirstSpan(); span != null; span = span.next()) {

                             driver.setValue(refDriver.getValue(initialRefOrbit.getDate()), span.getStart());
                         }
                     }
                 }
             }
         }

         // Build the reference propagator
         final Propagator propagator =
                         propagatorBuilder.buildPropagator(propagatorBuilder.
                                                           getSelectedNormalizedParameters());

         // Propagate until last date and return the orbit
         return propagator.propagate(finalDate).getOrbit();

     }

    /** Create a propagator builder from input parameters.
     * <p>
     * The advantage of using the DSST instead of the numerical
     * propagator is that it is possible to use greater values
     * for the minimum and maximum integration steps.
     * </p>
     * @param parser input file parser
     * @param conventions IERS conventions to use
     * @param body central body
     * @param orbit first orbit estimate
     * @return propagator builder
     * @throws NoSuchElementException if input parameters are missing
     */
    private T configurePropagatorBuilder(final KeyValueFileParser<ParameterKey> parser,
                                         final IERSConventions conventions,
                                         final OneAxisEllipsoid body,
                                         final Orbit orbit)
        throws NoSuchElementException {

        final double minStep;
        if (!parser.containsKey(ParameterKey.PROPAGATOR_MIN_STEP)) {
            minStep = 6000.0;
        } else {
            minStep = parser.getDouble(ParameterKey.PROPAGATOR_MIN_STEP);
        }

        final double maxStep;
        if (!parser.containsKey(ParameterKey.PROPAGATOR_MAX_STEP)) {
            maxStep = 86400;
        } else {
            maxStep = parser.getDouble(ParameterKey.PROPAGATOR_MAX_STEP);
        }

        final double dP;
        if (!parser.containsKey(ParameterKey.PROPAGATOR_POSITION_ERROR)) {
            dP = 10.0;
        } else {
            dP = parser.getDouble(ParameterKey.PROPAGATOR_POSITION_ERROR);
        }

        final double positionScale;
        if (!parser.containsKey(ParameterKey.ESTIMATOR_ORBITAL_PARAMETERS_POSITION_SCALE)) {
            positionScale = dP;
        } else {
            positionScale = parser.getDouble(ParameterKey.ESTIMATOR_ORBITAL_PARAMETERS_POSITION_SCALE);
        }

        final T propagatorBuilder = createPropagatorBuilder(orbit,
                                                            new DormandPrince853IntegratorBuilder(minStep, maxStep, dP),
                                                            positionScale);

        // initial mass
        final double mass;
        if (!parser.containsKey(ParameterKey.MASS)) {
            mass = 1000.0;
        } else {
            mass = parser.getDouble(ParameterKey.MASS);
        }
        setMass(propagatorBuilder, mass);

        setGravity(propagatorBuilder, body);

        // third body attraction
        if (parser.containsKey(ParameterKey.THIRD_BODY_SUN) &&
            parser.getBoolean(ParameterKey.THIRD_BODY_SUN)) {
            setThirdBody(propagatorBuilder, CelestialBodyFactory.getSun());
        }
        if (parser.containsKey(ParameterKey.THIRD_BODY_MOON) &&
            parser.getBoolean(ParameterKey.THIRD_BODY_MOON)) {
            setThirdBody(propagatorBuilder, CelestialBodyFactory.getMoon());
        }

        // ocean tides force model
        if (parser.containsKey(ParameterKey.OCEAN_TIDES_DEGREE) &&
            parser.containsKey(ParameterKey.OCEAN_TIDES_ORDER)) {
            final int degree = parser.getInt(ParameterKey.OCEAN_TIDES_DEGREE);
            final int order  = parser.getInt(ParameterKey.OCEAN_TIDES_ORDER);
            if (degree > 0 && order > 0) {
                setOceanTides(propagatorBuilder, conventions, body, degree, order);
            }
        }

        // solid tides force model
        final List<CelestialBody> solidTidesBodies = new ArrayList<CelestialBody>();
        if (parser.containsKey(ParameterKey.SOLID_TIDES_SUN) &&
            parser.getBoolean(ParameterKey.SOLID_TIDES_SUN)) {
            solidTidesBodies.add(CelestialBodyFactory.getSun());
        }
        if (parser.containsKey(ParameterKey.SOLID_TIDES_MOON) &&
            parser.getBoolean(ParameterKey.SOLID_TIDES_MOON)) {
            solidTidesBodies.add(CelestialBodyFactory.getMoon());
        }
        if (!solidTidesBodies.isEmpty()) {
            setSolidTides(propagatorBuilder, conventions, body,
                          solidTidesBodies.toArray(new CelestialBody[solidTidesBodies.size()]));
        }

        // drag
        if (parser.containsKey(ParameterKey.DRAG) && parser.getBoolean(ParameterKey.DRAG)) {
            final double  cd          = parser.getDouble(ParameterKey.DRAG_CD);
            final double  area        = parser.getDouble(ParameterKey.DRAG_AREA);
            final boolean cdEstimated = parser.getBoolean(ParameterKey.DRAG_CD_ESTIMATED);

            final MarshallSolarActivityFutureEstimation msafe =
                            new MarshallSolarActivityFutureEstimation(MarshallSolarActivityFutureEstimation.DEFAULT_SUPPORTED_NAMES,
                                                                      MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
            final Atmosphere atmosphere = new DTM2000(msafe, CelestialBodyFactory.getSun(), body);
            final List<ParameterDriver> drivers = setDrag(propagatorBuilder, atmosphere, new IsotropicDrag(area, cd));
            if (cdEstimated) {
                for (final ParameterDriver driver : drivers) {
                    if (driver.getName().equals(DragSensitive.DRAG_COEFFICIENT)) {
                        driver.setSelected(true);
                    }
                }
            }
        }

        // solar radiation pressure
        if (parser.containsKey(ParameterKey.SOLAR_RADIATION_PRESSURE) && parser.getBoolean(ParameterKey.SOLAR_RADIATION_PRESSURE)) {
            final double  cr          = parser.getDouble(ParameterKey.SOLAR_RADIATION_PRESSURE_CR);
            final double  area        = parser.getDouble(ParameterKey.SOLAR_RADIATION_PRESSURE_AREA);
            final boolean cREstimated = parser.getBoolean(ParameterKey.SOLAR_RADIATION_PRESSURE_CR_ESTIMATED);
            final List<ParameterDriver> drivers = setSolarRadiationPressure(propagatorBuilder, CelestialBodyFactory.getSun(),
                                                                            body,
                                                                            new IsotropicRadiationSingleCoefficient(area, cr));
            if (cREstimated) {
                for (final ParameterDriver driver : drivers) {
                    if (driver.getName().equals(RadiationSensitive.REFLECTION_COEFFICIENT)) {
                        driver.setSelected(true);
                    }
                }
            }
        }

        // Earth's albedo and infrared
        if (parser.containsKey(ParameterKey.EARTH_ALBEDO_INFRARED) && parser.getBoolean(ParameterKey.EARTH_ALBEDO_INFRARED)) {
            final double  cr               = parser.getDouble(ParameterKey.SOLAR_RADIATION_PRESSURE_CR);
            final double  area             = parser.getDouble(ParameterKey.SOLAR_RADIATION_PRESSURE_AREA);
            final boolean cREstimated      = parser.getBoolean(ParameterKey.SOLAR_RADIATION_PRESSURE_CR_ESTIMATED);
            final double angularResolution = parser.getAngle(ParameterKey.ALBEDO_INFRARED_ANGULAR_RESOLUTION);
            final List<ParameterDriver> drivers = setAlbedoInfrared(propagatorBuilder, CelestialBodyFactory.getSun(),
                                                                    body.getEquatorialRadius(), angularResolution,
                                                                    new IsotropicRadiationSingleCoefficient(area, cr));
            if (cREstimated) {
                for (final ParameterDriver driver : drivers) {
                    if (driver.getName().equals(RadiationSensitive.REFLECTION_COEFFICIENT)) {
                        driver.setSelected(true);
                    }
                }
            }
        }

        // post-Newtonian correction force due to general relativity
        if (parser.containsKey(ParameterKey.GENERAL_RELATIVITY) && parser.getBoolean(ParameterKey.GENERAL_RELATIVITY)) {
            setRelativity(propagatorBuilder);
        }

        // extra polynomial accelerations
        if (parser.containsKey(ParameterKey.POLYNOMIAL_ACCELERATION_NAME)) {
            final String[]       names        = parser.getStringArray(ParameterKey.POLYNOMIAL_ACCELERATION_NAME);
            final Vector3D[]     directions   = parser.getVectorArray(ParameterKey.POLYNOMIAL_ACCELERATION_DIRECTION_X,
                                                                      ParameterKey.POLYNOMIAL_ACCELERATION_DIRECTION_Y,
                                                                      ParameterKey.POLYNOMIAL_ACCELERATION_DIRECTION_Z);
            final List<String>[] coefficients = parser.getStringsListArray(ParameterKey.POLYNOMIAL_ACCELERATION_COEFFICIENTS, ',');
            final boolean[]      estimated    = parser.getBooleanArray(ParameterKey.POLYNOMIAL_ACCELERATION_ESTIMATED);

            for (int i = 0; i < names.length; ++i) {
                final List<ParameterDriver> drivers = setPolynomialAcceleration(propagatorBuilder, names[i], directions[i], coefficients[i].size() - 1);
                for (int k = 0; k < coefficients[i].size(); ++k) {
                    final String coefficientName = names[i] + "[" + k + "]";
                    for (final ParameterDriver driver : drivers) {
                        if (driver.getName().equals(coefficientName)) {
                            driver.setValue(Double.parseDouble(coefficients[i].get(k)));
                            driver.setSelected(estimated[i]);
                        }
                    }
                }
            }
        }

        // attitude mode
        final AttitudeMode mode;
        if (parser.containsKey(ParameterKey.ATTITUDE_MODE)) {
            mode = AttitudeMode.valueOf(parser.getString(ParameterKey.ATTITUDE_MODE));
        } else {
            mode = AttitudeMode.DEFAULT_LAW;
        }
        setAttitudeProvider(propagatorBuilder, mode.getProvider(orbit.getFrame(), body));

        return propagatorBuilder;

    }

    /** Create central body from input parameters.
     * @param parser input file parser
     * @return central body
     * @throws NoSuchElementException if input parameters are missing
     */
    private OneAxisEllipsoid createBody(final KeyValueFileParser<ParameterKey> parser)
        throws NoSuchElementException {

        final Frame bodyFrame;
        if (!parser.containsKey(ParameterKey.BODY_FRAME)) {
            bodyFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        } else {
            bodyFrame = parser.getEarthFrame(ParameterKey.BODY_FRAME);
        }

        final double equatorialRadius;
        if (!parser.containsKey(ParameterKey.BODY_EQUATORIAL_RADIUS)) {
            equatorialRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        } else {
            equatorialRadius = parser.getDouble(ParameterKey.BODY_EQUATORIAL_RADIUS);
        }

        final double flattening;
        if (!parser.containsKey(ParameterKey.BODY_INVERSE_FLATTENING)) {
            flattening = Constants.WGS84_EARTH_FLATTENING;
        } else {
            flattening = 1.0 / parser.getDouble(ParameterKey.BODY_INVERSE_FLATTENING);
        }

        return new OneAxisEllipsoid(equatorialRadius, flattening, bodyFrame);
    }

    /** Create an orbit from input parameters.
     * @param parser input file parser
     * @param mu     central attraction coefficient
     * @return orbit
     * @throws NoSuchElementException if input parameters are missing
     */
    private Orbit createOrbit(final KeyValueFileParser<ParameterKey> parser, final double mu)
        throws NoSuchElementException {

        final Frame frame;
        if (!parser.containsKey(ParameterKey.INERTIAL_FRAME)) {
            frame = FramesFactory.getEME2000();
        } else {
            frame = parser.getInertialFrame(ParameterKey.INERTIAL_FRAME);
        }

        // Orbit definition
        PositionAngleType angleType = PositionAngleType.MEAN;
        if (parser.containsKey(ParameterKey.ORBIT_ANGLE_TYPE)) {
            angleType = PositionAngleType.valueOf(parser.getString(ParameterKey.ORBIT_ANGLE_TYPE).toUpperCase());
        }
        if (parser.containsKey(ParameterKey.ORBIT_KEPLERIAN_A)) {
            return new KeplerianOrbit(parser.getDouble(ParameterKey.ORBIT_KEPLERIAN_A),
                                      parser.getDouble(ParameterKey.ORBIT_KEPLERIAN_E),
                                      parser.getAngle(ParameterKey.ORBIT_KEPLERIAN_I),
                                      parser.getAngle(ParameterKey.ORBIT_KEPLERIAN_PA),
                                      parser.getAngle(ParameterKey.ORBIT_KEPLERIAN_RAAN),
                                      parser.getAngle(ParameterKey.ORBIT_KEPLERIAN_ANOMALY),
                                      angleType,
                                      frame,
                                      parser.getDate(ParameterKey.ORBIT_DATE,
                                                     TimeScalesFactory.getUTC()),
                                      mu);
        } else if (parser.containsKey(ParameterKey.ORBIT_EQUINOCTIAL_A)) {
            return new EquinoctialOrbit(parser.getDouble(ParameterKey.ORBIT_EQUINOCTIAL_A),
                                        parser.getDouble(ParameterKey.ORBIT_EQUINOCTIAL_EX),
                                        parser.getDouble(ParameterKey.ORBIT_EQUINOCTIAL_EY),
                                        parser.getDouble(ParameterKey.ORBIT_EQUINOCTIAL_HX),
                                        parser.getDouble(ParameterKey.ORBIT_EQUINOCTIAL_HY),
                                        parser.getAngle(ParameterKey.ORBIT_EQUINOCTIAL_LAMBDA),
                                        angleType,
                                        frame,
                                        parser.getDate(ParameterKey.ORBIT_DATE,
                                                       TimeScalesFactory.getUTC()),
                                        mu);
        } else if (parser.containsKey(ParameterKey.ORBIT_CIRCULAR_A)) {
            return new CircularOrbit(parser.getDouble(ParameterKey.ORBIT_CIRCULAR_A),
                                     parser.getDouble(ParameterKey.ORBIT_CIRCULAR_EX),
                                     parser.getDouble(ParameterKey.ORBIT_CIRCULAR_EY),
                                     parser.getAngle(ParameterKey.ORBIT_CIRCULAR_I),
                                     parser.getAngle(ParameterKey.ORBIT_CIRCULAR_RAAN),
                                     parser.getAngle(ParameterKey.ORBIT_CIRCULAR_ALPHA),
                                     angleType,
                                     frame,
                                     parser.getDate(ParameterKey.ORBIT_DATE,
                                                    TimeScalesFactory.getUTC()),
                                     mu);
        } else if (parser.containsKey(ParameterKey.ORBIT_TLE_LINE_1)) {
            final String line1 = parser.getString(ParameterKey.ORBIT_TLE_LINE_1);
            final String line2 = parser.getString(ParameterKey.ORBIT_TLE_LINE_2);
            final TLE tle = new TLE(line1, line2);

            final TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);

            final AbsoluteDate initDate = tle.getDate();
            final SpacecraftState initialState = propagator.getInitialState();


            //Transformation from TEME to frame.
            return new CartesianOrbit(initialState.getPVCoordinates(FramesFactory.getEME2000()),
                                      frame,
                                      initDate,
                                      mu);


        } else {
            final double[] pos = {parser.getDouble(ParameterKey.ORBIT_CARTESIAN_PX),
                                  parser.getDouble(ParameterKey.ORBIT_CARTESIAN_PY),
                                  parser.getDouble(ParameterKey.ORBIT_CARTESIAN_PZ)};
            final double[] vel = {parser.getDouble(ParameterKey.ORBIT_CARTESIAN_VX),
                                  parser.getDouble(ParameterKey.ORBIT_CARTESIAN_VY),
                                  parser.getDouble(ParameterKey.ORBIT_CARTESIAN_VZ)};

            return new CartesianOrbit(new PVCoordinates(new Vector3D(pos), new Vector3D(vel)),
                                      frame,
                                      parser.getDate(ParameterKey.ORBIT_DATE,
                                                     TimeScalesFactory.getUTC()),
                                      mu);
        }
    }

    /** Set up range bias due to transponder delay.
     * @param parser input file parser
     * @return range bias (may be null if bias is fixed to zero)
     */
    private Bias<Range> createSatRangeBias(final KeyValueFileParser<ParameterKey> parser) {

        // transponder delay
        final double transponderDelayBias;
        if (!parser.containsKey(ParameterKey.ONBOARD_RANGE_BIAS)) {
            transponderDelayBias = 0;
        } else {
            transponderDelayBias = parser.getDouble(ParameterKey.ONBOARD_RANGE_BIAS);
        }

        final double transponderDelayBiasMin;
        if (!parser.containsKey(ParameterKey.ONBOARD_RANGE_BIAS_MIN)) {
            transponderDelayBiasMin = Double.NEGATIVE_INFINITY;
        } else {
            transponderDelayBiasMin = parser.getDouble(ParameterKey.ONBOARD_RANGE_BIAS_MIN);
        }

        final double transponderDelayBiasMax;
        if (!parser.containsKey(ParameterKey.ONBOARD_RANGE_BIAS_MAX)) {
            transponderDelayBiasMax = Double.NEGATIVE_INFINITY;
        } else {
            transponderDelayBiasMax = parser.getDouble(ParameterKey.ONBOARD_RANGE_BIAS_MAX);
        }

        // bias estimation flag
        final boolean transponderDelayBiasEstimated;
        if (!parser.containsKey(ParameterKey.ONBOARD_RANGE_BIAS_ESTIMATED)) {
            transponderDelayBiasEstimated = false;
        } else {
            transponderDelayBiasEstimated = parser.getBoolean(ParameterKey.ONBOARD_RANGE_BIAS_ESTIMATED);
        }

        if (FastMath.abs(transponderDelayBias) >= Precision.SAFE_MIN || transponderDelayBiasEstimated) {
            // bias is either non-zero or will be estimated,
            // we really need to create a modifier for this
            final Bias<Range> bias = new Bias<Range>(new String[] { "transponder delay bias", },
                                                     new double[] { transponderDelayBias },
                                                     new double[] { 1.0 },
                                                     new double[] { transponderDelayBiasMin },
                                                     new double[] { transponderDelayBiasMax });
            bias.getParametersDrivers().get(0).setSelected(transponderDelayBiasEstimated);
            return bias;
        } else {
            // fixed zero bias, we don't need any modifier
            return null;
        }
    }

    /** Set up range modifier taking on-board antenna offset.
     * @param parser input file parser
     * @return range modifier (may be null if antenna offset is zero or undefined)
     */
    private PhaseCentersRangeModifier createSatAntennaRangeModifier(final KeyValueFileParser<ParameterKey> parser) {
        final Vector3D offset;
        if (!parser.containsKey(ParameterKey.ON_BOARD_ANTENNA_PHASE_CENTER_X)) {
            offset = Vector3D.ZERO;
        } else {
            offset = parser.getVector(ParameterKey.ON_BOARD_ANTENNA_PHASE_CENTER_X,
                                      ParameterKey.ON_BOARD_ANTENNA_PHASE_CENTER_Y,
                                      ParameterKey.ON_BOARD_ANTENNA_PHASE_CENTER_Z);
        }
        return offset.getNorm() > 0 ?
               new PhaseCentersRangeModifier(FrequencyPattern.ZERO_CORRECTION,
                                             new FrequencyPattern(offset, null)) :
               null;
    }

    /** Set up range modifier taking shapiro effect.
     * @param parser input file parser
     * @return range modifier (may be null if antenna offset is zero or undefined)
     */
    private ShapiroRangeModifier createShapiroRangeModifier(final KeyValueFileParser<ParameterKey> parser) {
        final ShapiroRangeModifier shapiro;
        if (!parser.containsKey(ParameterKey.RANGE_SHAPIRO)) {
            shapiro = null;
        } else {
            shapiro = parser.getBoolean(ParameterKey.RANGE_SHAPIRO) ? new ShapiroRangeModifier(getMu()) : null;
        }
        return shapiro;
    }

    /** Set up stations.
     * @param parser input file parser
     * @param refDate reference date (from orbit initial guess)
     * @param sinexPosition sinex file containing station position (can be null)
     * @param sinexEcc sinex file containing station eccentricities (can be null)
     * @param conventions IERS conventions to use
     * @param body central body
     * @return name to station data map
     * @throws NoSuchElementException if input parameters are missing
     */
    private Map<String, StationData> createStationsData(final KeyValueFileParser<ParameterKey> parser,
                                                        final AbsoluteDate refDate,
                                                        final SinexLoader sinexPosition,
                                                        final SinexLoader sinexEcc,
                                                        final IERSConventions conventions,
                                                        final OneAxisEllipsoid body)
        throws NoSuchElementException {

        final Map<String, StationData> stations       = new HashMap<String, StationData>();

        final boolean   useTimeSpanTroposphericModel      = parser.getBoolean(ParameterKey.USE_TIME_SPAN_TROPOSPHERIC_MODEL);
        final String[]  stationNames                      = parser.getStringArray(ParameterKey.GROUND_STATION_NAME);
        final double[]  stationLatitudes                  = parser.getAngleArray(ParameterKey.GROUND_STATION_LATITUDE);
        final double[]  stationLongitudes                 = parser.getAngleArray(ParameterKey.GROUND_STATION_LONGITUDE);
        final double[]  stationAltitudes                  = parser.getDoubleArray(ParameterKey.GROUND_STATION_ALTITUDE);
        final boolean[] stationPositionEstimated          = parser.getBooleanArray(ParameterKey.GROUND_STATION_POSITION_ESTIMATED);
        final double[]  stationClockOffsets               = parser.getDoubleArray(ParameterKey.GROUND_STATION_CLOCK_OFFSET);
        final double[]  stationClockOffsetsMin            = parser.getDoubleArray(ParameterKey.GROUND_STATION_CLOCK_OFFSET_MIN);
        final double[]  stationClockOffsetsMax            = parser.getDoubleArray(ParameterKey.GROUND_STATION_CLOCK_OFFSET_MAX);
        final boolean[] stationClockOffsetEstimated       = parser.getBooleanArray(ParameterKey.GROUND_STATION_CLOCK_OFFSET_ESTIMATED);
        final double[]  stationRangeSigma                 = parser.getDoubleArray(ParameterKey.GROUND_STATION_RANGE_SIGMA);
        final double[]  stationRangeBias                  = parser.getDoubleArray(ParameterKey.GROUND_STATION_RANGE_BIAS);
        final double[]  stationRangeBiasMin               = parser.getDoubleArray(ParameterKey.GROUND_STATION_RANGE_BIAS_MIN);
        final double[]  stationRangeBiasMax               = parser.getDoubleArray(ParameterKey.GROUND_STATION_RANGE_BIAS_MAX);
        final boolean[] stationRangeBiasEstimated         = parser.getBooleanArray(ParameterKey.GROUND_STATION_RANGE_BIAS_ESTIMATED);
        final double[]  stationRangeRateSigma             = parser.getDoubleArray(ParameterKey.GROUND_STATION_RANGE_RATE_SIGMA);
        final double[]  stationRangeRateBias              = parser.getDoubleArray(ParameterKey.GROUND_STATION_RANGE_RATE_BIAS);
        final double[]  stationRangeRateBiasMin           = parser.getDoubleArray(ParameterKey.GROUND_STATION_RANGE_RATE_BIAS_MIN);
        final double[]  stationRangeRateBiasMax           = parser.getDoubleArray(ParameterKey.GROUND_STATION_RANGE_RATE_BIAS_MAX);
        final boolean[] stationRangeRateBiasEstimated     = parser.getBooleanArray(ParameterKey.GROUND_STATION_RANGE_RATE_BIAS_ESTIMATED);
        final double[]  stationAzimuthSigma               = parser.getAngleArray(ParameterKey.GROUND_STATION_AZIMUTH_SIGMA);
        final double[]  stationAzimuthBias                = parser.getAngleArray(ParameterKey.GROUND_STATION_AZIMUTH_BIAS);
        final double[]  stationAzimuthBiasMin             = parser.getAngleArray(ParameterKey.GROUND_STATION_AZIMUTH_BIAS_MIN);
        final double[]  stationAzimuthBiasMax             = parser.getAngleArray(ParameterKey.GROUND_STATION_AZIMUTH_BIAS_MAX);
        final double[]  stationElevationSigma             = parser.getAngleArray(ParameterKey.GROUND_STATION_ELEVATION_SIGMA);
        final double[]  stationElevationBias              = parser.getAngleArray(ParameterKey.GROUND_STATION_ELEVATION_BIAS);
        final double[]  stationElevationBiasMin           = parser.getAngleArray(ParameterKey.GROUND_STATION_ELEVATION_BIAS_MIN);
        final double[]  stationElevationBiasMax           = parser.getAngleArray(ParameterKey.GROUND_STATION_ELEVATION_BIAS_MAX);
        final boolean[] stationAzElBiasesEstimated        = parser.getBooleanArray(ParameterKey.GROUND_STATION_AZ_EL_BIASES_ESTIMATED);
        final boolean[] stationElevationRefraction        = parser.getBooleanArray(ParameterKey.GROUND_STATION_ELEVATION_REFRACTION_CORRECTION);
        final boolean[] stationTroposphericModelEstimated = parser.getBooleanArray(ParameterKey.GROUND_STATION_TROPOSPHERIC_MODEL_ESTIMATED);
        final double[]  stationTroposphericZenithDelay    = parser.getDoubleArray(ParameterKey.GROUND_STATION_TROPOSPHERIC_ZENITH_DELAY);
        final boolean[] stationZenithDelayEstimated       = parser.getBooleanArray(ParameterKey.GROUND_STATION_TROPOSPHERIC_DELAY_ESTIMATED);
        final boolean[] stationGlobalMappingFunction      = parser.getBooleanArray(ParameterKey.GROUND_STATION_GLOBAL_MAPPING_FUNCTION);
        final boolean[] stationNiellMappingFunction       = parser.getBooleanArray(ParameterKey.GROUND_STATION_NIELL_MAPPING_FUNCTION);
        final boolean[] stationWeatherEstimated           = parser.getBooleanArray(ParameterKey.GROUND_STATION_WEATHER_ESTIMATED);
        final boolean[] stationRangeTropospheric          = parser.getBooleanArray(ParameterKey.GROUND_STATION_RANGE_TROPOSPHERIC_CORRECTION);
        final boolean[] stationIonosphericCorrection      = parser.getBooleanArray(ParameterKey.GROUND_STATION_RANGE_IONOSPHERIC_CORRECTION);
        final boolean[] stationIonosphericModelEstimated  = parser.getBooleanArray(ParameterKey.GROUND_STATION_IONOSPHERIC_MODEL_ESTIMATED);
        final boolean[] stationVTECEstimated              = parser.getBooleanArray(ParameterKey.GROUND_STATION_IONOSPHERIC_VTEC_ESTIMATED);
        final double[]  stationIonosphericVTEC            = parser.getDoubleArray(ParameterKey.GROUND_STATION_IONOSPHERIC_VTEC_VALUE);
        final double[]  stationIonosphericHIon            = parser.getDoubleArray(ParameterKey.GROUND_STATION_IONOSPHERIC_HION_VALUE);

        final TidalDisplacement tidalDisplacement;
        if (parser.containsKey(ParameterKey.SOLID_TIDES_DISPLACEMENT_CORRECTION) &&
            parser.getBoolean(ParameterKey.SOLID_TIDES_DISPLACEMENT_CORRECTION)) {
            final boolean removePermanentDeformation =
                            parser.containsKey(ParameterKey.SOLID_TIDES_DISPLACEMENT_REMOVE_PERMANENT_DEFORMATION) &&
                            parser.getBoolean(ParameterKey.SOLID_TIDES_DISPLACEMENT_REMOVE_PERMANENT_DEFORMATION);
            tidalDisplacement = new TidalDisplacement(Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.JPL_SSD_SUN_EARTH_PLUS_MOON_MASS_RATIO,
                                                      Constants.JPL_SSD_EARTH_MOON_MASS_RATIO,
                                                      CelestialBodyFactory.getSun(),
                                                      CelestialBodyFactory.getMoon(),
                                                      conventions,
                                                      removePermanentDeformation);
        } else {
            tidalDisplacement = null;
        }

        final OceanLoadingCoefficientsBLQFactory blqFactory;
        if (parser.containsKey(ParameterKey.OCEAN_LOADING_CORRECTION) &&
            parser.getBoolean(ParameterKey.OCEAN_LOADING_CORRECTION)) {
            blqFactory = new OceanLoadingCoefficientsBLQFactory("^.*\\.blq$");
        } else {
            blqFactory = null;
        }

        final EOPHistory eopHistory = FramesFactory.findEOP(body.getBodyFrame());

        for (int i = 0; i < stationNames.length; ++i) {

            // displacements
            final StationDisplacement[] displacements;
            final OceanLoading oceanLoading = (blqFactory == null) ?
                                              null :
                                              new OceanLoading(body, blqFactory.getCoefficients(stationNames[i]));
            if (tidalDisplacement == null) {
                if (oceanLoading == null) {
                    displacements = new StationDisplacement[0];
                } else {
                    displacements = new StationDisplacement[] {
                        oceanLoading
                    };
                }
            } else {
                if (oceanLoading == null) {
                    displacements = new StationDisplacement[] {
                        tidalDisplacement
                    };
                } else {
                    displacements = new StationDisplacement[] {
                        tidalDisplacement, oceanLoading
                    };
                }
            }

            // the station itself
            final GeodeticPoint position;
            if (sinexPosition != null) {
                // A sinex file is available -> use the station positions inside the file
                final Station stationData = sinexPosition.getStation(stationNames[i].substring(0, 4));
                position = body.transform(stationData.getPosition(), body.getBodyFrame(), stationData.getEpoch());
            } else {
                // If a sinex file is not available -> use the values in input file
                position = new GeodeticPoint(stationLatitudes[i], stationLongitudes[i], stationAltitudes[i]);
            }
            final TopocentricFrame topo = new TopocentricFrame(body, position, stationNames[i]);
            final GroundStation station = new GroundStation(topo, eopHistory, displacements);
            station.getClockOffsetDriver().setReferenceValue(stationClockOffsets[i]);
            station.getClockOffsetDriver().setValue(stationClockOffsets[i]);
            station.getClockOffsetDriver().setMinValue(stationClockOffsetsMin[i]);
            station.getClockOffsetDriver().setMaxValue(stationClockOffsetsMax[i]);
            station.getClockOffsetDriver().setSelected(stationClockOffsetEstimated[i]);
            station.getEastOffsetDriver().setSelected(stationPositionEstimated[i]);
            station.getNorthOffsetDriver().setSelected(stationPositionEstimated[i]);
            station.getZenithOffsetDriver().setSelected(stationPositionEstimated[i]);

            // Take into consideration station eccentricities if not null
            if (sinexEcc != null) {
                final Station stationEcc = sinexEcc.getStation(stationNames[i]);
                final Vector3D eccentricities = stationEcc.getEccentricities(refDate);
                station.getZenithOffsetDriver().setValue(eccentricities.getX());
                station.getZenithOffsetDriver().setReferenceValue(eccentricities.getX());
                station.getNorthOffsetDriver().setValue(eccentricities.getY());
                station.getNorthOffsetDriver().setReferenceValue(eccentricities.getY());
                station.getEastOffsetDriver().setValue(eccentricities.getZ());
                station.getEastOffsetDriver().setReferenceValue(eccentricities.getZ());
            }

            // range
            final double rangeSigma = stationRangeSigma[i];
            final Bias<Range> rangeBias;
            if (FastMath.abs(stationRangeBias[i])   >= Precision.SAFE_MIN || stationRangeBiasEstimated[i]) {
                rangeBias = new Bias<Range>(new String[] { stationNames[i] + RANGE_BIAS_SUFFIX, },
                                            new double[] { stationRangeBias[i] },
                                            new double[] { rangeSigma },
                                            new double[] { stationRangeBiasMin[i] },
                                            new double[] { stationRangeBiasMax[i] });
                rangeBias.getParametersDrivers().get(0).setSelected(stationRangeBiasEstimated[i]);
            } else {
                // bias fixed to zero, we don't need to create a modifier for this
                rangeBias = null;
            }

            // range rate
            final double rangeRateSigma = stationRangeRateSigma[i];
            final Bias<RangeRate> rangeRateBias;
            if (FastMath.abs(stationRangeRateBias[i])   >= Precision.SAFE_MIN || stationRangeRateBiasEstimated[i]) {
                rangeRateBias = new Bias<RangeRate>(new String[] { stationNames[i] + RANGE_RATE_BIAS_SUFFIX },
                                                    new double[] { stationRangeRateBias[i] },
                                                    new double[] { rangeRateSigma },
                                                    new double[] { stationRangeRateBiasMin[i] },
                                                    new double[] {
                                                        stationRangeRateBiasMax[i]
                                                    });
                rangeRateBias.getParametersDrivers().get(0).setSelected(stationRangeRateBiasEstimated[i]);
            } else {
                // bias fixed to zero, we don't need to create a modifier for this
                rangeRateBias = null;
            }

            // angular biases
            final double[] azELSigma = new double[] {
                stationAzimuthSigma[i], stationElevationSigma[i]
            };
            final Bias<AngularAzEl> azELBias;
            if (FastMath.abs(stationAzimuthBias[i])   >= Precision.SAFE_MIN ||
                FastMath.abs(stationElevationBias[i]) >= Precision.SAFE_MIN ||
                stationAzElBiasesEstimated[i]) {
                azELBias = new Bias<AngularAzEl>(new String[] { stationNames[i] + AZIMUTH_BIAS_SUFFIX,
                                                                stationNames[i] + ELEVATION_BIAS_SUFFIX },
                                                 new double[] { stationAzimuthBias[i], stationElevationBias[i] },
                                                 azELSigma,
                                                 new double[] { stationAzimuthBiasMin[i], stationElevationBiasMin[i] },
                                                 new double[] { stationAzimuthBiasMax[i], stationElevationBiasMax[i] });
                azELBias.getParametersDrivers().get(0).setSelected(stationAzElBiasesEstimated[i]);
                azELBias.getParametersDrivers().get(1).setSelected(stationAzElBiasesEstimated[i]);
            } else {
                // bias fixed to zero, we don't need to create a modifier for this
                azELBias = null;
            }

            //Refraction correction
            final AngularRadioRefractionModifier refractionCorrection;
            if (stationElevationRefraction[i]) {
                final double                     altitude        = station.getBaseFrame().getPoint().getAltitude();
                final AtmosphericRefractionModel refractionModel = new EarthITU453AtmosphereRefraction(altitude);
                refractionCorrection = new AngularRadioRefractionModifier(refractionModel);
            } else {
                refractionCorrection = null;
            }


            //Tropospheric correction
            final RangeTroposphericDelayModifier rangeTroposphericCorrection;
            if (stationRangeTropospheric[i]) {

                MappingFunction mappingModel = null;
                if (stationGlobalMappingFunction[i]) {
                    mappingModel = new GlobalMappingFunctionModel();
                } else if (stationNiellMappingFunction[i]) {
                    mappingModel = new NiellMappingFunctionModel();
                }

                final DiscreteTroposphericModel troposphericModel;
                if (stationTroposphericModelEstimated[i] && mappingModel != null) {
                    // Estimated tropospheric model

                    // Compute pressure and temperature for estimated tropospheric model
                    final double pressure;
                    final double temperature;
                    if (stationWeatherEstimated[i]) {
                        // Empirical models to compute the pressure and the temperature
                        final GlobalPressureTemperatureModel weather = new GlobalPressureTemperatureModel(stationLatitudes[i],
                                                                                                          stationLongitudes[i],
                                                                                                          body.getBodyFrame());
                        weather.weatherParameters(stationAltitudes[i], parser.getDate(ParameterKey.ORBIT_DATE,
                                                                                      TimeScalesFactory.getUTC()));
                        temperature = weather.getTemperature();
                        pressure    = weather.getPressure();

                    } else {
                        // Standard atmosphere model : temperature: 18 degree Celsius and pressure: 1013.25 mbar
                        temperature = 273.15 + 18.0;
                        pressure    = 1013.25;
                    }

                    if (useTimeSpanTroposphericModel) {
                        // Initial model used to initialize the time span tropospheric model
                        final EstimatedTroposphericModel initialModel = new EstimatedTroposphericModel(temperature, pressure, mappingModel,
                                                                                                       stationTroposphericZenithDelay[i]);

                        // Initialize the time span tropospheric model
                        final TimeSpanEstimatedTroposphericModel timeSpanModel = new TimeSpanEstimatedTroposphericModel(initialModel);

                        // Median date
                        final AbsoluteDate epoch = parser.getDate(ParameterKey.TROPOSPHERIC_CORRECTION_DATE, TimeScalesFactory.getUTC());

                        // Station name
                        final String subName = stationNames[i].substring(0, 5);

                        // Estimated tropospheric model BEFORE the median date
                        final EstimatedTroposphericModel modelBefore = new EstimatedTroposphericModel(temperature, pressure, mappingModel,
                                                                                                      stationTroposphericZenithDelay[i]);
                        final ParameterDriver totalDelayBefore = modelBefore.getParametersDrivers().get(0);
                        totalDelayBefore.setSelected(stationZenithDelayEstimated[i]);
                        totalDelayBefore.setName(subName + TimeSpanEstimatedTroposphericModel.DATE_BEFORE + epoch.toString(TimeScalesFactory.getUTC()) + " " + EstimatedTroposphericModel.TOTAL_ZENITH_DELAY);

                        // Estimated tropospheric model AFTER the median date
                        final EstimatedTroposphericModel modelAfter = new EstimatedTroposphericModel(temperature, pressure, mappingModel,
                                                                                                     stationTroposphericZenithDelay[i]);
                        final ParameterDriver totalDelayAfter = modelAfter.getParametersDrivers().get(0);
                        totalDelayAfter.setSelected(stationZenithDelayEstimated[i]);
                        totalDelayAfter.setName(subName + TimeSpanEstimatedTroposphericModel.DATE_AFTER + epoch.toString(TimeScalesFactory.getUTC()) + " " + EstimatedTroposphericModel.TOTAL_ZENITH_DELAY);

                        // Add models to the time span tropospheric model
                        // A very ugly trick is used when no measurements are available for a specific time span.
                        // Indeed, the tropospheric parameter will not be estimated for the time span with no measurements.
                        // Therefore, the diagonal elements of the covariance matrix will be equal to zero.
                        // At the end, an exception is thrown when accessing the physical covariance matrix because of singularities issues.
                        if (subName.equals("SEAT/")) {
                            // Do not add the model because no measurements are available
                            // for the time span before the median date for this station.
                        } else {
                            timeSpanModel.addTroposphericModelValidBefore(modelBefore, epoch);
                        }
                        if (subName.equals("BADG/") || subName.equals("IRKM/")) {
                            // Do not add the model because no measurements are available
                            // for the time span after the median date for this station.
                        } else {
                            timeSpanModel.addTroposphericModelValidAfter(modelAfter, epoch);
                        }

                        troposphericModel = timeSpanModel;

                    } else {

                        troposphericModel = new EstimatedTroposphericModel(temperature, pressure,
                                                                           mappingModel, stationTroposphericZenithDelay[i]);
                        final ParameterDriver driver = troposphericModel.getParametersDrivers().get(0);
                        driver.setName(stationNames[i].substring(0, 4) + "/ " + EstimatedTroposphericModel.TOTAL_ZENITH_DELAY);
                        driver.setSelected(stationZenithDelayEstimated[i]);

                    }

                } else {
                    // Empirical tropospheric model
                    troposphericModel = SaastamoinenModel.getStandardModel();
                }

                rangeTroposphericCorrection = new  RangeTroposphericDelayModifier(troposphericModel);
            } else {
                rangeTroposphericCorrection = null;
            }

            // Ionospheric correction
            final IonosphericModel ionosphericModel;
            if (stationIonosphericCorrection[i]) {
                if (stationIonosphericModelEstimated[i]) {
                    // Estimated ionospheric model
                    final IonosphericMappingFunction mapping = new SingleLayerModelMappingFunction(stationIonosphericHIon[i]);
                    ionosphericModel  = new EstimatedIonosphericModel(mapping, stationIonosphericVTEC[i]);
                    final ParameterDriver  ionosphericDriver = ionosphericModel.getParametersDrivers().get(0);
                    ionosphericDriver.setSelected(stationVTECEstimated[i]);
                    ionosphericDriver.setName(stationNames[i].substring(0, 5) + EstimatedIonosphericModel.VERTICAL_TOTAL_ELECTRON_CONTENT);
                } else {
                    final TimeScale utc = TimeScalesFactory.getUTC();
                    // Klobuchar model
                    final KlobucharIonoCoefficientsLoader loader = new KlobucharIonoCoefficientsLoader();
                    loader.loadKlobucharIonosphericCoefficients(parser.getDate(ParameterKey.ORBIT_DATE, utc).getComponents(utc).getDate());
                    ionosphericModel = new KlobucharIonoModel(loader.getAlpha(), loader.getBeta());
                }
            } else {
                ionosphericModel = null;
            }

            stations.put(stationNames[i],
                         new StationData(station,
                                         rangeSigma,     rangeBias,
                                         rangeRateSigma, rangeRateBias,
                                         azELSigma,      azELBias,
                                         refractionCorrection, rangeTroposphericCorrection,
                                         ionosphericModel));
        }
        return stations;

    }

    /** Set up weights.
     * @param parser input file parser
     * @return base weights
     * @throws NoSuchElementException if input parameters are missing
     */
    private Weights createWeights(final KeyValueFileParser<ParameterKey> parser)
        throws NoSuchElementException {
        return new Weights(parser.getDouble(ParameterKey.RANGE_MEASUREMENTS_BASE_WEIGHT),
                           parser.getDouble(ParameterKey.RANGE_RATE_MEASUREMENTS_BASE_WEIGHT),
                           new double[] {
                               parser.getDouble(ParameterKey.AZIMUTH_MEASUREMENTS_BASE_WEIGHT),
                               parser.getDouble(ParameterKey.ELEVATION_MEASUREMENTS_BASE_WEIGHT)
                           },
                           parser.getDouble(ParameterKey.PV_MEASUREMENTS_BASE_WEIGHT));
    }

    /** Set up outliers manager for range measurements.
     * @param parser input file parser
     * @param isDynamic if true, the filter should have adjustable standard deviation
     * @return outliers manager (null if none configured)
     */
    private OutlierFilter<Range> createRangeOutliersManager(final KeyValueFileParser<ParameterKey> parser, boolean isDynamic) {
        needsBothOrNeither(parser,
                           ParameterKey.RANGE_OUTLIER_REJECTION_MULTIPLIER,
                           ParameterKey.RANGE_OUTLIER_REJECTION_STARTING_ITERATION);
        return isDynamic ?
               new DynamicOutlierFilter<>(parser.getInt(ParameterKey.RANGE_OUTLIER_REJECTION_STARTING_ITERATION),
                                          parser.getInt(ParameterKey.RANGE_OUTLIER_REJECTION_MULTIPLIER)) :
               new OutlierFilter<>(parser.getInt(ParameterKey.RANGE_OUTLIER_REJECTION_STARTING_ITERATION),
                                   parser.getInt(ParameterKey.RANGE_OUTLIER_REJECTION_MULTIPLIER));
    }

    /** Set up outliers manager for range-rate measurements.
     * @param parser input file parser
     * @param isDynamic if true, the filter should have adjustable standard deviation
     * @return outliers manager (null if none configured)
     */
    private OutlierFilter<RangeRate> createRangeRateOutliersManager(final KeyValueFileParser<ParameterKey> parser, boolean isDynamic) {
        needsBothOrNeither(parser,
                           ParameterKey.RANGE_RATE_OUTLIER_REJECTION_MULTIPLIER,
                           ParameterKey.RANGE_RATE_OUTLIER_REJECTION_STARTING_ITERATION);
        return isDynamic ?
               new DynamicOutlierFilter<>(parser.getInt(ParameterKey.RANGE_RATE_OUTLIER_REJECTION_STARTING_ITERATION),
                                          parser.getInt(ParameterKey.RANGE_RATE_OUTLIER_REJECTION_MULTIPLIER)) :
               new OutlierFilter<>(parser.getInt(ParameterKey.RANGE_RATE_OUTLIER_REJECTION_STARTING_ITERATION),
                                   parser.getInt(ParameterKey.RANGE_RATE_OUTLIER_REJECTION_MULTIPLIER));
    }

    /** Set up outliers manager for azimuth-elevation measurements.
     * @param parser input file parser
     * @param isDynamic if true, the filter should have adjustable standard deviation
     * @return outliers manager (null if none configured)
     */
    private OutlierFilter<AngularAzEl> createAzElOutliersManager(final KeyValueFileParser<ParameterKey> parser, boolean isDynamic) {
        needsBothOrNeither(parser,
                           ParameterKey.AZ_EL_OUTLIER_REJECTION_MULTIPLIER,
                           ParameterKey.AZ_EL_OUTLIER_REJECTION_STARTING_ITERATION);
        return isDynamic ?
               new DynamicOutlierFilter<>(parser.getInt(ParameterKey.AZ_EL_OUTLIER_REJECTION_STARTING_ITERATION),
                                          parser.getInt(ParameterKey.AZ_EL_OUTLIER_REJECTION_MULTIPLIER)) :
               new OutlierFilter<>(parser.getInt(ParameterKey.AZ_EL_OUTLIER_REJECTION_STARTING_ITERATION),
                                   parser.getInt(ParameterKey.AZ_EL_OUTLIER_REJECTION_MULTIPLIER));
    }

    /** Set up outliers manager for PV measurements.
     * @param parser input file parser
     * @param isDynamic if true, the filter should have adjustable standard deviation
     * @return outliers manager (null if none configured)
     */
    private OutlierFilter<PV> createPVOutliersManager(final KeyValueFileParser<ParameterKey> parser, boolean isDynamic) {
        needsBothOrNeither(parser,
                           ParameterKey.PV_OUTLIER_REJECTION_MULTIPLIER,
                           ParameterKey.PV_OUTLIER_REJECTION_STARTING_ITERATION);
        return isDynamic ?
               new DynamicOutlierFilter<>(parser.getInt(ParameterKey.PV_OUTLIER_REJECTION_STARTING_ITERATION),
                                          parser.getInt(ParameterKey.PV_OUTLIER_REJECTION_MULTIPLIER)) :
               new OutlierFilter<>(parser.getInt(ParameterKey.PV_OUTLIER_REJECTION_STARTING_ITERATION),
                                   parser.getInt(ParameterKey.PV_OUTLIER_REJECTION_MULTIPLIER));
    }

    /** Set up PV data.
     * @param parser input file parser
     * @return PV data
     * @throws NoSuchElementException if input parameters are missing
     */
    private PVData createPVData(final KeyValueFileParser<ParameterKey> parser)
        throws NoSuchElementException {
        return new PVData(parser.getDouble(ParameterKey.PV_MEASUREMENTS_POSITION_SIGMA),
                          parser.getDouble(ParameterKey.PV_MEASUREMENTS_VELOCITY_SIGMA));
    }

    /** Set up satellite data.
     * @param parser input file parser
     * @return satellite data
     * @throws NoSuchElementException if input parameters are missing
     */
    private ObservableSatellite createObservableSatellite(final KeyValueFileParser<ParameterKey> parser)
        throws NoSuchElementException {
        final ObservableSatellite obsSat = new ObservableSatellite(0);
        final ParameterDriver clockOffsetDriver = obsSat.getClockOffsetDriver();
        if (parser.containsKey(ParameterKey.ON_BOARD_CLOCK_OFFSET)) {
        	// date = null okay if validity period is infinite = only 1 estimation over the all period
            clockOffsetDriver.setReferenceValue(parser.getDouble(ParameterKey.ON_BOARD_CLOCK_OFFSET));
            clockOffsetDriver.setValue(parser.getDouble(ParameterKey.ON_BOARD_CLOCK_OFFSET));
        }
        if (parser.containsKey(ParameterKey.ON_BOARD_CLOCK_OFFSET_MIN)) {
            clockOffsetDriver.setMinValue(parser.getDouble(ParameterKey.ON_BOARD_CLOCK_OFFSET_MIN));
        }
        if (parser.containsKey(ParameterKey.ON_BOARD_CLOCK_OFFSET_MAX)) {
            clockOffsetDriver.setMaxValue(parser.getDouble(ParameterKey.ON_BOARD_CLOCK_OFFSET_MAX));
        }
        if (parser.containsKey(ParameterKey.ON_BOARD_CLOCK_OFFSET_ESTIMATED)) {
            clockOffsetDriver.setSelected(parser.getBoolean(ParameterKey.ON_BOARD_CLOCK_OFFSET_ESTIMATED));
        }
        return obsSat;
    }

    /** Set up estimator.
     * @param parser input file parser
     * @param propagatorBuilder propagator builder
     * @return estimator
     * @throws NoSuchElementException if input parameters are missing
     */
    private BatchLSEstimator createEstimator(final KeyValueFileParser<ParameterKey> parser,
                                             final PropagatorBuilder propagatorBuilder)
        throws NoSuchElementException {

        final boolean optimizerIsLevenbergMarquardt;
        if (!parser.containsKey(ParameterKey.ESTIMATOR_OPTIMIZATION_ENGINE)) {
            optimizerIsLevenbergMarquardt = true;
        } else {
            final String engine = parser.getString(ParameterKey.ESTIMATOR_OPTIMIZATION_ENGINE);
            optimizerIsLevenbergMarquardt = engine.toLowerCase().contains("levenberg");
        }
        final LeastSquaresOptimizer optimizer;

        if (optimizerIsLevenbergMarquardt) {
            // we want to use a Levenberg-Marquardt optimization engine
            final double initialStepBoundFactor;
            if (!parser.containsKey(ParameterKey.ESTIMATOR_LEVENBERG_MARQUARDT_INITIAL_STEP_BOUND_FACTOR)) {
                initialStepBoundFactor = 100.0;
            } else {
                initialStepBoundFactor = parser.getDouble(ParameterKey.ESTIMATOR_LEVENBERG_MARQUARDT_INITIAL_STEP_BOUND_FACTOR);
            }

            optimizer = new LevenbergMarquardtOptimizer().withInitialStepBoundFactor(initialStepBoundFactor);
        } else {
            // we want to use a Gauss-Newton optimization engine
            optimizer = new GaussNewtonOptimizer(new QRDecomposer(1e-11), false);
        }

        final double convergenceThreshold;
        if (!parser.containsKey(ParameterKey.ESTIMATOR_NORMALIZED_PARAMETERS_CONVERGENCE_THRESHOLD)) {
            convergenceThreshold = 1.0e-3;
        } else {
            convergenceThreshold = parser.getDouble(ParameterKey.ESTIMATOR_NORMALIZED_PARAMETERS_CONVERGENCE_THRESHOLD);
        }
        final int maxIterations;
        if (!parser.containsKey(ParameterKey.ESTIMATOR_MAX_ITERATIONS)) {
            maxIterations = 10;
        } else {
            maxIterations = parser.getInt(ParameterKey.ESTIMATOR_MAX_ITERATIONS);
        }
        final int maxEvaluations;
        if (!parser.containsKey(ParameterKey.ESTIMATOR_MAX_EVALUATIONS)) {
            maxEvaluations = 20;
        } else {
            maxEvaluations = parser.getInt(ParameterKey.ESTIMATOR_MAX_EVALUATIONS);
        }

        final BatchLSEstimator estimator = new BatchLSEstimator(optimizer, propagatorBuilder);
        estimator.setParametersConvergenceThreshold(convergenceThreshold);
        estimator.setMaxIterations(maxIterations);
        estimator.setMaxEvaluations(maxEvaluations);

        return estimator;

    }

    /** Set up sequential estimator.
     * @param parser input file parser
     * @param propagatorBuilder propagator builder
     * @return estimator
     * @throws NoSuchElementException if input parameters are missing
     */
    private BatchLSEstimator createSequentialEstimator(final Optimum optimum, final KeyValueFileParser<ParameterKey> parser,
                                                       final PropagatorBuilder propagatorBuilder)
        throws NoSuchElementException {


        final double convergenceThreshold;
        if (!parser.containsKey(ParameterKey.ESTIMATOR_NORMALIZED_PARAMETERS_CONVERGENCE_THRESHOLD)) {
            convergenceThreshold = 1.0e-3;
        } else {
            convergenceThreshold = parser.getDouble(ParameterKey.ESTIMATOR_NORMALIZED_PARAMETERS_CONVERGENCE_THRESHOLD);
        }
        final int maxIterations;
        if (!parser.containsKey(ParameterKey.ESTIMATOR_MAX_ITERATIONS)) {
            maxIterations = 10;
        } else {
            maxIterations = parser.getInt(ParameterKey.ESTIMATOR_MAX_ITERATIONS);
        }
        final int maxEvaluations;
        if (!parser.containsKey(ParameterKey.ESTIMATOR_MAX_EVALUATIONS)) {
            maxEvaluations = 20;
        } else {
            maxEvaluations = parser.getInt(ParameterKey.ESTIMATOR_MAX_EVALUATIONS);
        }

        final SequentialGaussNewtonOptimizer optimizer = new SequentialGaussNewtonOptimizer().withEvaluation(optimum);
        final SequentialBatchLSEstimator estimator = new SequentialBatchLSEstimator(optimizer, propagatorBuilder);
        estimator.setParametersConvergenceThreshold(convergenceThreshold);
        estimator.setMaxIterations(maxIterations);
        estimator.setMaxEvaluations(maxEvaluations);

        return estimator;

    }

    /** Read a sinex file corresponding to the given key.
     * @param input input file
     * @param parser input file parser
     * @param key name of the file
     * @return container for sinex data or null if key does not exist
     * @throws IOException if file is not read properly
     */
    private SinexLoader readSinexFile(final File input,
                                      final KeyValueFileParser<ParameterKey> parser,
                                      final ParameterKey key)
        throws IOException {

        // Verify if the key is defined
        if (parser.containsKey(key)) {

            // File name
            final String fileName = parser.getString(key);

            // Read the file
            DataSource nd = new DataSource(fileName, () -> new FileInputStream(new File(input.getParentFile(), fileName)));
            for (final DataFilter filter : Arrays.asList(new GzipFilter(),
                                                         new UnixCompressFilter(),
                                                         new HatanakaCompressFilter())) {
                nd = filter.filter(nd);
            }

            // Return a configured SINEX file
            return new SinexLoader(nd);

        } else {

            // File is not defines, return a null object
            return null;

        }

    }

    /** Read a measurements file.
     * @param source data source containing measurements
     * @param stations name to stations data map
     * @param pvData PV measurements data
     * @param satellite satellite reference
     * @param satRangeBias range bias due to transponder delay
     * @param satAntennaRangeModifier modifier for on-board antenna offset
     * @param weights base weights for measurements
     * @param rangeOutliersManager manager for range measurements outliers (null if none configured)
     * @param rangeRateOutliersManager manager for range-rate measurements outliers (null if none configured)
     * @param azElOutliersManager manager for azimuth-elevation measurements outliers (null if none configured)
     * @param pvOutliersManager manager for PV measurements outliers (null if none configured)
     * @return measurements list
     * @exception IOException if measurement file cannot be read
     */
    private List<ObservedMeasurement<?>> readMeasurements(final DataSource source,
                                                          final Map<String, StationData> stations,
                                                          final PVData pvData,
                                                          final ObservableSatellite satellite,
                                                          final Bias<Range> satRangeBias,
                                                          final PhaseCentersRangeModifier satAntennaRangeModifier,
                                                          final Weights weights,
                                                          final OutlierFilter<Range> rangeOutliersManager,
                                                          final OutlierFilter<RangeRate> rangeRateOutliersManager,
                                                          final OutlierFilter<AngularAzEl> azElOutliersManager,
                                                          final OutlierFilter<PV> pvOutliersManager)
        throws IOException {

        final List<ObservedMeasurement<?>> measurements = new ArrayList<ObservedMeasurement<?>>();
        try (Reader         reader = source.getOpener().openReaderOnce();
             BufferedReader br     = new BufferedReader(reader)) {
            int lineNumber = 0;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                ++lineNumber;
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("#")) {
                    final String[] fields = line.split("\\s+");
                    if (fields.length < 2) {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  lineNumber, source.getName(), line);
                    }
                    switch (fields[1]) {
                        case "RANGE" :
                            if (useRangeMeasurements) {
                                final Range range = new RangeParser().parseFields(fields, stations, pvData, satellite,
                                                                                  satRangeBias, weights,
                                                                                  line, lineNumber, source.getName());
                                if (satAntennaRangeModifier != null) {
                                    range.addModifier(satAntennaRangeModifier);
                                }
                                if (rangeOutliersManager != null) {
                                    range.addModifier(rangeOutliersManager);
                                }
                                addIfNonZeroWeight(range, measurements);
                            }
                            break;
                        case "RANGE_RATE" :
                            if (useRangeRateMeasurements) {
                                final RangeRate rangeRate = new RangeRateParser().parseFields(fields, stations, pvData, satellite,
                                                                                              satRangeBias, weights,
                                                                                              line, lineNumber, source.getName());
                                if (rangeRateOutliersManager != null) {
                                    rangeRate.addModifier(rangeRateOutliersManager);
                                }
                                addIfNonZeroWeight(rangeRate, measurements);
                            }
                            break;
                        case "AZ_EL" :
                            final AngularAzEl angular = new AzElParser().parseFields(fields, stations, pvData, satellite,
                                                                                     satRangeBias, weights,
                                                                                     line, lineNumber, source.getName());
                            if (azElOutliersManager != null) {
                                angular.addModifier(azElOutliersManager);
                            }
                            addIfNonZeroWeight(angular, measurements);
                            break;
                        case "PV" :
                            final PV pv = new PVParser().parseFields(fields, stations, pvData, satellite,
                                                                     satRangeBias, weights,
                                                                     line, lineNumber, source.getName());
                            if (pvOutliersManager != null) {
                                pv.addModifier(pvOutliersManager);
                            }
                            addIfNonZeroWeight(pv, measurements);
                            break;
                        default :
                            throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                                      "unknown measurement type " + fields[1] +
                                                      " at line " + lineNumber +
                                                      " in file " + source.getName() +
                                                      "\n" + line);
                    }
                }
            }
        }

        if (measurements.isEmpty()) {
            throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                      "not measurements read from file " + source.getName());
        }

        return measurements;

    }

    /** Read a RINEX measurements file.
     * @param source data source containing measurements
     * @param satId satellite we are interested in
     * @param stations name to stations data map
     * @param satellite satellite reference
     * @param satRangeBias range bias due to transponder delay
     * @param satAntennaRangeModifier modifier for on-board antenna offset
     * @param weights base weights for measurements
     * @param rangeOutliersManager manager for range measurements outliers (null if none configured)
     * @param rangeRateOutliersManager manager for range-rate measurements outliers (null if none configured)
     * @param shapiroRangeModifier shapiro range modifier (null if none configured)
     * @return measurements list
     * @exception IOException if measurement file cannot be read
     */
    private List<ObservedMeasurement<?>> readRinex(final DataSource source, final String satId,
                                                   final Map<String, StationData> stations,
                                                   final ObservableSatellite satellite,
                                                   final Bias<Range> satRangeBias,
                                                   final PhaseCentersRangeModifier satAntennaRangeModifier,
                                                   final Weights weights,
                                                   final OutlierFilter<Range> rangeOutliersManager,
                                                   final OutlierFilter<RangeRate> rangeRateOutliersManager,
                                                   final ShapiroRangeModifier shapiroRangeModifier)
        throws IOException {
        final String notConfigured = " not configured";
        final List<ObservedMeasurement<?>> measurements = new ArrayList<ObservedMeasurement<?>>();
        final SatelliteSystem system = SatelliteSystem.parseSatelliteSystem(satId);
        final int prnNumber;
        switch (system) {
            case GPS:
            case GLONASS:
            case GALILEO:
                prnNumber = Integer.parseInt(satId.substring(1));
                break;
            case SBAS:
                prnNumber = Integer.parseInt(satId.substring(1)) + 100;
                break;
            default:
                prnNumber = -1;
        }
        final RinexObservation rinexObs = new RinexObservationParser().parse(source);
        for (final ObservationDataSet observationDataSet : rinexObs.getObservationDataSets()) {
            if (observationDataSet.getSatellite().getSystem() == system    &&
                observationDataSet.getSatellite().getPRN()    == prnNumber) {
                for (final ObservationData od : observationDataSet.getObservationData()) {
                    final double snr = od.getSignalStrength();
                    if (!Double.isNaN(od.getValue()) && (snr == 0 || snr >= 4)) {
                        if (od.getObservationType().getMeasurementType() == MeasurementType.PSEUDO_RANGE && useRangeMeasurements) {
                            // this is a measurement we want
                            final String stationName = rinexObs.getHeader().getMarkerName() + "/" + od.getObservationType();
                            final StationData stationData = stations.get(stationName);
                            if (stationData == null) {
                                throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                                          stationName + notConfigured);
                            }
                            final Range range = new Range(stationData.getStation(), false, observationDataSet.getDate(),
                                                          od.getValue(), stationData.getRangeSigma(),
                                                          weights.getRangeBaseWeight(), satellite);
                            if (stationData.getIonosphericModel() != null) {
                                final RangeIonosphericDelayModifier ionoModifier = new RangeIonosphericDelayModifier(stationData.getIonosphericModel(),
                                                                                                                     od.getObservationType().getFrequency(system).getMHzFrequency() * 1.0e6);
                                          range.addModifier(ionoModifier);
                            }
                            if (satAntennaRangeModifier != null) {
                                range.addModifier(satAntennaRangeModifier);
                            }
                            if (shapiroRangeModifier != null) {
                                range.addModifier(shapiroRangeModifier);
                            }
                            if (stationData.getRangeBias() != null) {
                                range.addModifier(stationData.getRangeBias());
                            }
                            if (satRangeBias != null) {
                                range.addModifier(satRangeBias);
                            }
                            if (stationData.getRangeTroposphericCorrection() != null) {
                                range.addModifier(stationData.getRangeTroposphericCorrection());
                            }
                            addIfNonZeroWeight(range, measurements);

                        } else if (od.getObservationType().getMeasurementType() == MeasurementType.DOPPLER && useRangeRateMeasurements) {
                            // this is a measurement we want
                            final String stationName = rinexObs.getHeader().getMarkerName() + "/" + od.getObservationType();
                            final StationData stationData = stations.get(stationName);
                            if (stationData == null) {
                                throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                                          stationName + notConfigured);
                            }
                            final RangeRate rangeRate = new RangeRate(stationData.getStation(), observationDataSet.getDate(),
                                                                      od.getValue(), stationData.getRangeRateSigma(),
                                                                      weights.getRangeRateBaseWeight(), false, satellite);
                            if (stationData.getIonosphericModel() != null) {
                                final RangeRateIonosphericDelayModifier ionoModifier = new RangeRateIonosphericDelayModifier(stationData.getIonosphericModel(),
                                                                                                                             od.getObservationType().getFrequency(system).getMHzFrequency() * 1.0e6,
                                                                                                                             false);
                                rangeRate.addModifier(ionoModifier);
                            }
                            if (stationData.getRangeRateBias() != null) {
                                rangeRate.addModifier(stationData.getRangeRateBias());
                            }
                            addIfNonZeroWeight(rangeRate, measurements);
                        }
                    }
                }
            }
        }

        return measurements;

    }

    /**
     * Read a position CPF file
     * @param source data source containing measurements
     * @param satellite observable satellite
     * @param initialGuess initial guess (used for the frame)
     * @return list of observable measurements
     * @throws IOException if file cannot be read
     */
     private List<ObservedMeasurement<?>> readCpf(final DataSource source,
                                                  final ObservableSatellite satellite,
                                                  final Orbit initialGuess) throws IOException {

         // Initialize parser and read file
         final CPFParser parserCpf = new CPFParser();
         final CPF file = (CPF) parserCpf.parse(source);

         // Satellite ephemeris
         final CPFEphemeris ephemeris = file.getSatellites().get(file.getHeader().getIlrsSatelliteId());
         final Frame        ephFrame  = ephemeris.getFrame();

         // Measurements
         final List<ObservedMeasurement<?>> measurements = new ArrayList<>();
         for (final CPFCoordinate coordinates : ephemeris.getCoordinates()) {
             AbsoluteDate date = coordinates.getDate();
             final Vector3D posInertial = ephFrame.getStaticTransformTo(initialGuess.getFrame(), date).transformPosition(coordinates.getPosition());
             measurements.add(new Position(date, posInertial, 1, 1, satellite));
         }

         // Return
         return measurements;
    }

    /** Read a Consolidated Ranging Data measurements file.
     * @param source data source containing measurements
     * @param stations name to stations data map
     * @param kvParser input file parser
     * @param satellite observable satellite
     * @param satRangeBias range bias
     * @param weights range weight
     * @param rangeOutliersManager outlier filter for range measurements
     * @param shapiroRange correction for general relativity
     * @return a list of observable measurements
     * @throws IOException if measurement file cannot be read
     */
    private List<ObservedMeasurement<?>> readCrd(final DataSource source,
                                                 final Map<String, StationData> stations,
                                                 final KeyValueFileParser<ParameterKey> kvParser,
                                                 final ObservableSatellite satellite,
                                                 final Bias<Range> satRangeBias,
                                                 final Weights weights,
                                                 final OutlierFilter<Range> rangeOutliersManager,
                                                 final ShapiroRangeModifier shapiroRange)
        throws IOException {

        // Initialize an empty list of measurements
        final List<ObservedMeasurement<?>> measurements = new ArrayList<ObservedMeasurement<?>>();

        // Initialise parser and read file
        final CRDParser parser =  new CRDParser();
        final CRD   file   = parser.parse(source);

        // Loop on data block
        for (final CRDDataBlock block : file.getDataBlocks()) {

            // Header
            final CRDHeader header = block.getHeader();

            // Wavelength (meters)
            final double wavelength = block.getConfigurationRecords().getSystemRecord().getWavelength();

            // Meteo data
            final Meteo meteo = block.getMeteoData();

            // Station data
            final StationData stationData = stations.get(String.valueOf(header.getSystemIdentifier()));

            // Loop on measurements
            for (final RangeMeasurement range : block.getRangeData()) {

                // Time of flight
                final double timeOfFlight = range.getTimeOfFlight();

                // Transmit time
                final AbsoluteDate transmitTime = range.getDate();
                // If epoch corresponds to bounce time, take into consideration the time of flight to compute the transmit time
                if (range.getEpochEvent() == 1) {
                    transmitTime.shiftedBy(-0.5 * timeOfFlight);
                }

                // Received time taking into consideration the time of flight (two-way)
                final AbsoluteDate receivedTime = transmitTime.shiftedBy(timeOfFlight);

                // Range value
                boolean twoWays = false;
                double rangeValue = timeOfFlight * Constants.SPEED_OF_LIGHT;
                // If the range is a two way range, the value is divided by 2 to fit in Orekit Range object requirements
                if (header.getRangeType() == RangeType.TWO_WAY) {
                    twoWays = true;
                    rangeValue = 0.5 * rangeValue;
                }

                // Meteorological record for the current epoch
                final MeteorologicalMeasurement meteoData = meteo.getMeteo(receivedTime);

                // Initialize range
                final Range measurement = new Range(stationData.getStation(), twoWays, receivedTime, rangeValue,
                                                    stationData.getRangeSigma(), weights.getRangeBaseWeight(), satellite);

                // Center of mass correction
                if (kvParser.containsKey(ParameterKey.RANGE_CENTER_OF_MASS_CORRECTION)) {
                    final double bias = kvParser.getDouble(ParameterKey.RANGE_CENTER_OF_MASS_CORRECTION);
                    final Bias<Range> centerOfMass = new Bias<Range>(new String[] {"center of mass"},
                                    new double[] {bias}, new double[] {1.0}, new double[] {Double.NEGATIVE_INFINITY},
                                    new double[] {Double.POSITIVE_INFINITY});
                    measurement.addModifier(centerOfMass);
                }

                // Tropospheric model
                final DiscreteTroposphericModel model;
                if (meteoData != null) {
                    model = new MendesPavlisModel(meteoData.getTemperature(), meteoData.getPressure() * 1000.0,
                                                  0.01 * meteoData.getHumidity(), wavelength * 1.0e6);
                } else {
                    model = MendesPavlisModel.getStandardModel(wavelength * 1.0e6);
                }
                measurement.addModifier(new RangeTroposphericDelayModifier(model));

                // Shapiro
                if (shapiroRange != null) {
                    measurement.addModifier(shapiroRange);
                }

                // Station bias
                if (stationData.getRangeBias() != null) {
                    measurement.addModifier(stationData.getRangeBias());
                }

                // Satellite range bias
                if (satRangeBias != null) {
                    measurement.addModifier(satRangeBias);
                }

                // Range outlier filter
                if (rangeOutliersManager != null) {
                    measurement.addModifier(rangeOutliersManager);
                }

                addIfNonZeroWeight(measurement, measurements);
            }

        }

        return measurements;
    }

    /** Multiplex measurements.
     * @param independentMeasurements independent measurements
     * @param tol tolerance on time difference for multiplexed measurements
     * @return multiplexed measurements
     */
    private List<ObservedMeasurement<?>> multiplexMeasurements(final List<ObservedMeasurement<?>> independentMeasurements,
                                                               final double tol) {
        final List<ObservedMeasurement<?>> multiplexed = new ArrayList<>();
        independentMeasurements.sort(new ChronologicalComparator());
        List<ObservedMeasurement<?>> clump = new ArrayList<>();
        for (final ObservedMeasurement<?> measurement : independentMeasurements) {
            if (!clump.isEmpty() && measurement.getDate().durationFrom(clump.get(0).getDate()) > tol) {

                // previous clump is finished
                if (clump.size() == 1) {
                    multiplexed.add(clump.get(0));
                } else {
                    multiplexed.add(new MultiplexedMeasurement(clump));
                }

                // start new clump
                clump = new ArrayList<>();

            }
            clump.add(measurement);
        }
        // final clump is finished
        if (clump.size() == 1) {
            multiplexed.add(clump.get(0));
        } else {
            multiplexed.add(new MultiplexedMeasurement(clump));
        }
        return multiplexed;
    }

    /** Sort parameters changes.
     * @param parameters parameters list
     */
    protected void sortParametersChanges(List<? extends ParameterDriver> parameters) {

        // sort the parameters lexicographically
        Collections.sort(parameters, new Comparator<ParameterDriver>() {
            /** {@inheritDoc} */
            @Override
            public int compare(final ParameterDriver pd1, final ParameterDriver pd2) {
                return pd1.getName().compareTo(pd2.getName());
            }

        });
    }

    /** Add a measurement to a list if it has non-zero weight.
     * @param measurement measurement to add
     * @param measurements measurements list
     */
    private static void addIfNonZeroWeight(final ObservedMeasurement<?> measurement, final List<ObservedMeasurement<?>> measurements) {
        double sum = 0;
        for (double w : measurement.getBaseWeight()) {
            sum += FastMath.abs(w);
        }
        if (sum > Precision.SAFE_MIN) {
            // we only consider measurements with non-zero weight
            measurements.add(measurement);
        }
    }

    /** Check a pair of related parameters are configurated properly.
     * @param parser input file parser
     * @param key1 first key to check
     * @param key2 second key to check
     */
    private void needsBothOrNeither(final KeyValueFileParser<ParameterKey> parser,
                                    final ParameterKey key1, final ParameterKey key2) {
        if (parser.containsKey(key1) != parser.containsKey(key2)) {
            throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                      key1.toString().toLowerCase().replace('_', '.') +
                                      " and  " +
                                      key2.toString().toLowerCase().replace('_', '.') +
                                      " must be both present or both absent");
        }
    }

    /** Display parameters changes.
     * @param header header message
     * @param sort if true, parameters will be sorted lexicographically
     * @param parameters parameters list
     */
    private void displayParametersChanges(final PrintStream out, final String header, final boolean sort,
                                          final int length, final ParameterDriversList parameters) {

        List<ParameterDriver> list = new ArrayList<ParameterDriver>(parameters.getDrivers());
        if (sort) {
            // sort the parameters lexicographically
            Collections.sort(list, new Comparator<ParameterDriver>() {
                /** {@inheritDoc} */
                @Override
                public int compare(final ParameterDriver pd1, final ParameterDriver pd2) {
                    return pd1.getName().compareTo(pd2.getName());
                }

            });
        }

        out.println(header);
        int index = 0;
        for (final ParameterDriver parameter : list) {
            if (parameter.isSelected()) {
                final double factor;
                if (parameter.getName().endsWith("/az bias") || parameter.getName().endsWith("/el bias")) {
                    factor = FastMath.toDegrees(1.0);
                } else {
                    factor = 1.0;
                }
                final double initial = parameter.getReferenceValue();
                final double value   = parameter.getValue();
                out.format(Locale.US, "  %2d %s", ++index, parameter.getName());
                for (int i = parameter.getName().length(); i < length; ++i) {
                    out.format(Locale.US, " ");
                }
                out.format(Locale.US, "  %+.12f  (final value:  % .12f)%n",
                           factor * (value - initial), factor * value);
            }
        }

    }

    /** Display covariances and sigmas as predicted by a Kalman filter at date t.
     */
    private void displayFinalCovariances(final PrintStream logStream, final KalmanEstimator kalman) {

//        // Get kalman estimated propagator
//        final NumericalPropagator kalmanProp = kalman.getProcessModel().getEstimatedPropagator();
//
//        // Link the partial derivatives to this propagator
//        final String equationName = "kalman-derivatives";
//        PartialDerivativesEquations kalmanDerivatives = new PartialDerivativesEquations(equationName, kalmanProp);
//
//        // Initialize the derivatives
//        final SpacecraftState rawState = kalmanProp.getInitialState();
//        final SpacecraftState stateWithDerivatives =
//                        kalmanDerivatives.setInitialJacobians(rawState);
//        kalmanProp.resetInitialState(stateWithDerivatives);
//
//        // Propagate to target date
//        final SpacecraftState kalmanState = kalmanProp.propagate(targetDate);
//
//        // Compute STM
//        RealMatrix STM = kalman.getProcessModel().getErrorStateTransitionMatrix(kalmanState, kalmanDerivatives);
//
//        // Compute covariance matrix
//        RealMatrix P = kalman.getProcessModel().unNormalizeCovarianceMatrix(kalman.predictCovariance(STM,
//                                                                              kalman.getProcessModel().getProcessNoiseMatrix()));
        final RealMatrix P = kalman.getPhysicalEstimatedCovarianceMatrix();
        final String[] paramNames = new String[P.getRowDimension()];
        int index = 0;
        int paramSize = 0;
        for (final ParameterDriver driver : kalman.getOrbitalParametersDrivers(true).getDrivers()) {
            paramNames[index++] = driver.getName();
            paramSize = FastMath.max(paramSize, driver.getName().length());
        }
        for (final ParameterDriver driver : kalman.getPropagationParametersDrivers(true).getDrivers()) {
            paramNames[index++] = driver.getName();
            paramSize = FastMath.max(paramSize, driver.getName().length());
        }
        for (final ParameterDriver driver : kalman.getEstimatedMeasurementsParameters().getDrivers()) {
            paramNames[index++] = driver.getName();
            paramSize = FastMath.max(paramSize, driver.getName().length());
        }
        if (paramSize < 20) {
            paramSize = 20;
        }

        // Header
        logStream.format("\n%s\n", "Kalman Final Covariances:");
//        logStream.format(Locale.US, "\tDate: %-23s UTC\n",
//                         targetDate.toString(TimeScalesFactory.getUTC()));
        logStream.format(Locale.US, "\tDate: %-23s UTC\n",
                         kalman.getCurrentDate().toString(TimeScalesFactory.getUTC()));

        // Covariances
        String strFormat = String.format("%%%2ds  ", paramSize);
        logStream.format(strFormat, "Covariances:");
        for (int i = 0; i < P.getRowDimension(); i++) {
            logStream.format(Locale.US, strFormat, paramNames[i]);
        }
        logStream.println("");
        String numFormat = String.format("%%%2d.6f  ", paramSize);
        for (int i = 0; i < P.getRowDimension(); i++) {
            logStream.format(Locale.US, strFormat, paramNames[i]);
            for (int j = 0; j <= i; j++) {
                logStream.format(Locale.US, numFormat, P.getEntry(i, j));
            }
            logStream.println("");
        }

        // Correlation coeff
        final double[] sigmas = new double[P.getRowDimension()];
        for (int i = 0; i < P.getRowDimension(); i++) {
            sigmas[i] = FastMath.sqrt(P.getEntry(i, i));
        }

        logStream.format("\n" + strFormat, "Corr coef:");
        for (int i = 0; i < P.getRowDimension(); i++) {
            logStream.format(Locale.US, strFormat, paramNames[i]);
        }
        logStream.println("");
        for (int i = 0; i < P.getRowDimension(); i++) {
            logStream.format(Locale.US, strFormat, paramNames[i]);
            for (int j = 0; j <= i; j++) {
                logStream.format(Locale.US, numFormat, P.getEntry(i, j)/(sigmas[i]*sigmas[j]));
            }
            logStream.println("");
        }

        // Sigmas
        logStream.format("\n" + strFormat + "\n", "Sigmas: ");
        for (int i = 0; i < P.getRowDimension(); i++) {
            logStream.format(Locale.US, strFormat + numFormat + "\n", paramNames[i], sigmas[i]);
        }
        logStream.println("");
    } 

    /** Display covariances and sigmas as predicted by a Kalman filter at date t. 
     */
    private void displayFinalCovariances(final PrintStream logStream, final UnscentedKalmanEstimator kalman) {
        
//        // Get kalman estimated propagator
//        final NumericalPropagator kalmanProp = kalman.getProcessModel().getEstimatedPropagator();
//        
//        // Link the partial derivatives to this propagator
//        final String equationName = "kalman-derivatives";
//        PartialDerivativesEquations kalmanDerivatives = new PartialDerivativesEquations(equationName, kalmanProp);
//        
//        // Initialize the derivatives
//        final SpacecraftState rawState = kalmanProp.getInitialState();
//        final SpacecraftState stateWithDerivatives =
//                        kalmanDerivatives.setInitialJacobians(rawState);
//        kalmanProp.resetInitialState(stateWithDerivatives);
//        
//        // Propagate to target date
//        final SpacecraftState kalmanState = kalmanProp.propagate(targetDate);
//        
//        // Compute STM
//        RealMatrix STM = kalman.getProcessModel().getErrorStateTransitionMatrix(kalmanState, kalmanDerivatives);
//        
//        // Compute covariance matrix
//        RealMatrix P = kalman.getProcessModel().unNormalizeCovarianceMatrix(kalman.predictCovariance(STM,
//                                                                              kalman.getProcessModel().getProcessNoiseMatrix()));
        final RealMatrix P = kalman.getPhysicalEstimatedCovarianceMatrix();
        final String[] paramNames = new String[P.getRowDimension()];
        int index = 0;
        int paramSize = 0;
        for (final ParameterDriver driver : kalman.getOrbitalParametersDrivers(true).getDrivers()) {
            paramNames[index++] = driver.getName();
            paramSize = FastMath.max(paramSize, driver.getName().length());
        }
        for (final ParameterDriver driver : kalman.getPropagationParametersDrivers(true).getDrivers()) {
            paramNames[index++] = driver.getName();
            paramSize = FastMath.max(paramSize, driver.getName().length());
        }
        for (final ParameterDriver driver : kalman.getEstimatedMeasurementsParameters().getDrivers()) {
            paramNames[index++] = driver.getName();
            paramSize = FastMath.max(paramSize, driver.getName().length());
        }
        if (paramSize < 20) {
            paramSize = 20;
        }
        
        // Header
        logStream.format("\n%s\n", "Kalman Final Covariances:");
//        logStream.format(Locale.US, "\tDate: %-23s UTC\n",
//                         targetDate.toString(TimeScalesFactory.getUTC()));
        logStream.format(Locale.US, "\tDate: %-23s UTC\n",
                         kalman.getCurrentDate().toString(TimeScalesFactory.getUTC()));
        
        // Covariances
        String strFormat = String.format("%%%2ds  ", paramSize);
        logStream.format(strFormat, "Covariances:");
        for (int i = 0; i < P.getRowDimension(); i++) {
            logStream.format(Locale.US, strFormat, paramNames[i]);
        }
        logStream.println("");
        String numFormat = String.format("%%%2d.6f  ", paramSize);
        for (int i = 0; i < P.getRowDimension(); i++) {
            logStream.format(Locale.US, strFormat, paramNames[i]);
            for (int j = 0; j <= i; j++) {
                logStream.format(Locale.US, numFormat, P.getEntry(i, j));
            }
            logStream.println("");
        }
        
        // Correlation coeff
        final double[] sigmas = new double[P.getRowDimension()];
        for (int i = 0; i < P.getRowDimension(); i++) {
            sigmas[i] = FastMath.sqrt(P.getEntry(i, i));
        }
        
        logStream.format("\n" + strFormat, "Corr coef:");
        for (int i = 0; i < P.getRowDimension(); i++) {
            logStream.format(Locale.US, strFormat, paramNames[i]);
        }
        logStream.println("");
        for (int i = 0; i < P.getRowDimension(); i++) {
            logStream.format(Locale.US, strFormat, paramNames[i]);
            for (int j = 0; j <= i; j++) {
                logStream.format(Locale.US, numFormat, P.getEntry(i, j)/(sigmas[i]*sigmas[j]));
            }
            logStream.println("");
        }
        
        // Sigmas
        logStream.format("\n" + strFormat + "\n", "Sigmas: ");
        for (int i = 0; i < P.getRowDimension(); i++) {
            logStream.format(Locale.US, strFormat + numFormat + "\n", paramNames[i], sigmas[i]);
        }
        logStream.println("");
    }

    /** Log evaluations.
     */
    private static void logEvaluation(EstimatedMeasurement<?> evaluation,
                               EvaluationLogger<Range> rangeLog,
                               EvaluationLogger<RangeRate> rangeRateLog,
                               EvaluationLogger<AngularAzEl> azimuthLog,
                               EvaluationLogger<AngularAzEl> elevationLog,
                               EvaluationLogger<Position> positionOnlyLog,
                               EvaluationLogger<PV> positionLog,
                               EvaluationLogger<PV> velocityLog) {
        
        // Get measurement type and send measurement to proper logger.
        final String measurementType = evaluation.getObservedMeasurement().getMeasurementType();
        if (measurementType.equals(Range.MEASUREMENT_TYPE)) {
            @SuppressWarnings("unchecked")
            final EstimatedMeasurement<Range> ev = (EstimatedMeasurement<Range>) evaluation;
            if (rangeLog != null) {
                rangeLog.log(ev);
            }
        } else if (measurementType.equals(RangeRate.MEASUREMENT_TYPE)) {
            @SuppressWarnings("unchecked")
            final EstimatedMeasurement<RangeRate> ev = (EstimatedMeasurement<RangeRate>) evaluation;
            if (rangeRateLog != null) {
                rangeRateLog.log(ev);
            }
        } else if (measurementType.equals(AngularAzEl.MEASUREMENT_TYPE)) {
            @SuppressWarnings("unchecked")
            final EstimatedMeasurement<AngularAzEl> ev = (EstimatedMeasurement<AngularAzEl>) evaluation;
            if (azimuthLog != null) {
                azimuthLog.log(ev);
            }
            if (elevationLog != null) {
                elevationLog.log(ev);
            }
        }  else if (measurementType.equals(Position.MEASUREMENT_TYPE)) {
            @SuppressWarnings("unchecked")
            final EstimatedMeasurement<Position> ev = (EstimatedMeasurement<Position>) evaluation;
            if (positionOnlyLog != null) {
                positionOnlyLog.log(ev);
            }
        } else if (measurementType.equals(PV.MEASUREMENT_TYPE)) {
            @SuppressWarnings("unchecked")
            final EstimatedMeasurement<PV> ev = (EstimatedMeasurement<PV>) evaluation;
            if (positionLog != null) {
                positionLog.log(ev);
            }
            if (velocityLog != null) {
                velocityLog.log(ev);
            }
        } else if (measurementType.equals(MultiplexedMeasurement.MEASUREMENT_TYPE)) {
            for (final EstimatedMeasurement<?> em : ((MultiplexedMeasurement) evaluation.getObservedMeasurement()).getEstimatedMeasurements()) {
                logEvaluation(em, rangeLog, rangeRateLog, azimuthLog, elevationLog, positionOnlyLog, positionLog, velocityLog);
            }
        }
    }

    /** Observer for Kalman estimation. */
    public static class Observer implements KalmanObserver {

        /** Date of the first measurement.*/
        private AbsoluteDate t0;
        
        /** Printing flag. */
        private Boolean print;
        
        /** Range logger. */
        private RangeLog rangeLog;
        
        /** Range rate logger. */
        private RangeRateLog rangeRateLog;
        
        /** Azimuth logger. */
        private AzimuthLog azimuthLog;
        
        /** Elevation logger. */
        private ElevationLog elevationLog;
        
        /** Position only logger. */
        private PositionOnlyLog positionOnlyLog;
        
        /** Position logger. */
        private PositionLog positionLog;
        
        /** Velocity logger. */
        private VelocityLog velocityLog;

        public Observer(Boolean print, RangeLog rangeLog, RangeRateLog rangeRateLog, AzimuthLog azimuthLog,
                ElevationLog elevationLog, PositionOnlyLog positionOnlyLog, PositionLog positionLog,
                VelocityLog velocityLog) {
            super();
            this.print           = print;
            this.rangeLog        = rangeLog;
            this.rangeRateLog    = rangeRateLog;
            this.azimuthLog      = azimuthLog;
            this.elevationLog    = elevationLog;
            this.positionOnlyLog = positionOnlyLog;
            this.positionLog     = positionLog;
            this.velocityLog     = velocityLog;
        }



        /** {@inheritDoc} */
        @Override
        @SuppressWarnings("unchecked")
        public void evaluationPerformed(final KalmanEstimation estimation) {

            // Current measurement number, date and status
            final EstimatedMeasurement<?> estimatedMeasurement = estimation.getCorrectedMeasurement();
            final int currentNumber        = estimation.getCurrentMeasurementNumber();
            final AbsoluteDate currentDate = estimatedMeasurement.getDate();
            final EstimatedMeasurement.Status currentStatus = estimatedMeasurement.getStatus();

            // Current estimated measurement
            final ObservedMeasurement<?>  observedMeasurement  = estimatedMeasurement.getObservedMeasurement();
            
            // Measurement type & Station name
            String measType    = "";
            String stationName = "";

            // Register the measurement in the proper measurement logger
            logEvaluation(estimatedMeasurement,
                    rangeLog, rangeRateLog, azimuthLog, elevationLog, positionOnlyLog, positionLog, velocityLog);
            // Get measurement type
            final String measurementType = observedMeasurement.getMeasurementType();
            if (measurementType.equals(Range.MEASUREMENT_TYPE)) {
                measType    = "RANGE";
                stationName =  ((EstimatedMeasurement<Range>) estimatedMeasurement).getObservedMeasurement().
                                getStation().getBaseFrame().getName();
            } else if (measurementType.equals(RangeRate.MEASUREMENT_TYPE)) {
                measType    = "RANGE_RATE";
                stationName =  ((EstimatedMeasurement<RangeRate>) estimatedMeasurement).getObservedMeasurement().
                                getStation().getBaseFrame().getName();
            } else if (measurementType.equals(AngularAzEl.MEASUREMENT_TYPE)) {
                measType    = "AZ_EL";
                stationName =  ((EstimatedMeasurement<AngularAzEl>) estimatedMeasurement).getObservedMeasurement().
                                getStation().getBaseFrame().getName();
            } else if (measurementType.equals(PV.MEASUREMENT_TYPE)) {
                measType    = "PV";
            } else if (measurementType.equals(Position.MEASUREMENT_TYPE)) {
                measType    = "POSITION";
            }
            

            // Print data on terminal
            // ----------------------

            // Header
            if (print) {
                if (currentNumber == 1) {
                    // Set t0 to first measurement date
                    t0 = currentDate;

                    // Print header
                    final String formatHeader = "%-4s\t%-25s\t%15s\t%-10s\t%-10s\t%-20s\t%20s\t%20s";
                    String header = String.format(Locale.US, formatHeader,
                                                  "Nb", "Epoch", "Dt[s]", "Status", "Type", "Station",
                                                  "DP Corr", "DV Corr");
                    // Orbital drivers
                    for (DelegatingDriver driver : estimation.getEstimatedOrbitalParameters().getDrivers()) {
                        header += String.format(Locale.US, "\t%20s", driver.getName());
                        header += String.format(Locale.US, "\t%20s", "D" + driver.getName());
                    }

                    // Propagation drivers
                    for (DelegatingDriver driver : estimation.getEstimatedPropagationParameters().getDrivers()) {
                        header += String.format(Locale.US, "\t%20s", driver.getName());
                        header += String.format(Locale.US, "\t%20s", "D" + driver.getName());
                    }

                    // Measurements drivers
                    for (DelegatingDriver driver : estimation.getEstimatedMeasurementsParameters().getDrivers()) {
                        header += String.format(Locale.US, "\t%20s", driver.getName());
                        header += String.format(Locale.US, "\t%20s", "D" + driver.getName());
                    }

                    // Print header
                    System.out.println(header);
                }

                // Print current measurement info in terminal
                String line = "";
                // Line format
                final String lineFormat = "%4d\t%-25s\t%15.3f\t%-10s\t%-10s\t%-20s\t%20.9e\t%20.9e";

                // Orbital correction = DP & DV between predicted orbit and estimated orbit
                final Vector3D predictedP = estimation.getPredictedSpacecraftStates()[0].getPosition();
                final Vector3D predictedV = estimation.getPredictedSpacecraftStates()[0].getPVCoordinates().getVelocity();
                final Vector3D estimatedP = estimation.getCorrectedSpacecraftStates()[0].getPosition();
                final Vector3D estimatedV = estimation.getCorrectedSpacecraftStates()[0].getPVCoordinates().getVelocity();
                final double DPcorr       = Vector3D.distance(predictedP, estimatedP);
                final double DVcorr       = Vector3D.distance(predictedV, estimatedV);

                line = String.format(Locale.US, lineFormat,
                                     currentNumber, currentDate.toString(), 
                                     currentDate.durationFrom(t0), currentStatus.toString(),
                                     measType, stationName,
                                     DPcorr, DVcorr);

                // Handle parameters printing (value and error) 
                int jPar = 0;
                final RealMatrix Pest = estimation.getPhysicalEstimatedCovarianceMatrix();
                // Orbital drivers
                for (DelegatingDriver driver : estimation.getEstimatedOrbitalParameters().getDrivers()) {
                    line += String.format(Locale.US, "\t%20.9f", driver.getValue());
                    line += String.format(Locale.US, "\t%20.9e", FastMath.sqrt(Pest.getEntry(jPar, jPar)));
                    jPar++;
                }
                // Propagation drivers
                for (DelegatingDriver driver : estimation.getEstimatedPropagationParameters().getDrivers()) {
                    line += String.format(Locale.US, "\t%20.9f", driver.getValue());
                    line += String.format(Locale.US, "\t%20.9e", FastMath.sqrt(Pest.getEntry(jPar, jPar)));
                    jPar++;
                }
                // Measurements drivers
                for (DelegatingDriver driver : estimation.getEstimatedMeasurementsParameters().getDrivers()) {
                    line += String.format(Locale.US, "\t%20.9f", driver.getValue());
                    line += String.format(Locale.US, "\t%20.9e", FastMath.sqrt(Pest.getEntry(jPar, jPar)));
                    jPar++;
                }

                // Print the line
                System.out.println(line);
            }
        }

    
    
    }

}

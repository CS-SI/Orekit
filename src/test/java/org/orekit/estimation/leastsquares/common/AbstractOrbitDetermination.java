/* Copyright 2002-2019 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.estimation.leastsquares.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
import org.hipparchus.linear.QRDecomposer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.GaussNewtonOptimizer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresOptimizer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.KeyValueFileParser;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataFilter;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.GzipFilter;
import org.orekit.data.NamedData;
import org.orekit.data.UnixCompressFilter;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.estimation.leastsquares.BatchLSObserver;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationsProvider;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.RangeRate;
import org.orekit.estimation.measurements.modifiers.AngularRadioRefractionModifier;
import org.orekit.estimation.measurements.modifiers.Bias;
import org.orekit.estimation.measurements.modifiers.OnBoardAntennaRangeModifier;
import org.orekit.estimation.measurements.modifiers.OutlierFilter;
import org.orekit.estimation.measurements.modifiers.RangeTroposphericDelayModifier;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.EOPHistory;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.gnss.HatanakaCompressFilter;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.ObservationData;
import org.orekit.gnss.ObservationDataSet;
import org.orekit.gnss.RinexLoader;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.models.AtmosphericRefractionModel;
import org.orekit.models.earth.EarthITU453AtmosphereRefraction;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.models.earth.atmosphere.DTM2000;
import org.orekit.models.earth.atmosphere.data.MarshallSolarActivityFutureEstimation;
import org.orekit.models.earth.displacement.OceanLoading;
import org.orekit.models.earth.displacement.OceanLoadingCoefficientsBLQFactory;
import org.orekit.models.earth.displacement.StationDisplacement;
import org.orekit.models.earth.displacement.TidalDisplacement;
import org.orekit.models.earth.troposphere.DiscreteTroposphericModel;
import org.orekit.models.earth.troposphere.EstimatedTroposphericModel;
import org.orekit.models.earth.troposphere.GlobalMappingFunctionModel;
import org.orekit.models.earth.troposphere.MappingFunction;
import org.orekit.models.earth.troposphere.NiellMappingFunctionModel;
import org.orekit.models.earth.troposphere.SaastamoinenModel;
import org.orekit.models.earth.weather.GlobalPressureTemperatureModel;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.DormandPrince853IntegratorBuilder;
import org.orekit.propagation.conversion.IntegratedPropagatorBuilder;
import org.orekit.propagation.conversion.ODEIntegratorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/** Base class for Orekit orbit determination tutorials.
 * @param <T> type of the propagator builder
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 */
public abstract class AbstractOrbitDetermination<T extends IntegratedPropagatorBuilder> {

    /** Suffix for range bias. */
    private final String RANGE_BIAS_SUFFIX = "/range bias";

    /** Suffix for range rate bias. */
    private final String RANGE_RATE_BIAS_SUFFIX = "/range rate bias";

    /** Suffix for azimuth bias. */
    private final String AZIMUTH_BIAS_SUFFIX = "/az bias";

    /** Suffix for elevation bias. */
    private final String ELEVATION_BIAS_SUFFIX = "/el bias";

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
    protected abstract ParameterDriver[] setGravity(T propagatorBuilder, OneAxisEllipsoid body);

    /** Set third body attraction force model.
     * @param propagatorBuilder propagator builder
     * @param conventions IERS conventions to use
     * @param body central body
     * @param degree degree of the tide model to load
     * @param order order of the tide model to load
     * @return drivers for the force model
     */
    protected abstract ParameterDriver[] setOceanTides(T propagatorBuilder, IERSConventions conventions,
                                                       OneAxisEllipsoid body, int degree, int order);

    /** Set third body attraction force model.
     * @param propagatorBuilder propagator builder
     * @param conventions IERS conventions to use
     * @param body central body
     * @param solidTidesBodies third bodies generating solid tides
     * @return drivers for the force model
     */
    protected abstract ParameterDriver[]setSolidTides(T propagatorBuilder, IERSConventions conventions,
                                                      OneAxisEllipsoid body, CelestialBody[] solidTidesBodies);

    /** Set third body attraction force model.
     * @param propagatorBuilder propagator builder
     * @param thirdBody third body
     * @return drivers for the force model
     */
    protected abstract ParameterDriver[] setThirdBody(T propagatorBuilder, CelestialBody thirdBody);

    /** Set drag force model.
     * @param propagatorBuilder propagator builder
     * @param atmosphere atmospheric model
     * @param spacecraft spacecraft model
     * @return drivers for the force model
     */
    protected abstract ParameterDriver[] setDrag(T propagatorBuilder, Atmosphere atmosphere, DragSensitive spacecraft);

    /** Set solar radiation pressure force model.
     * @param propagatorBuilder propagator builder
     * @param sun Sun model
     * @param equatorialRadius central body equatorial radius (for shadow computation)
     * @param spacecraft spacecraft model
     * @return drivers for the force model
     */
    protected abstract ParameterDriver[] setSolarRadiationPressure(T propagatorBuilder, CelestialBody sun,
                                                                   double equatorialRadius, RadiationSensitive spacecraft);

    /** Set relativity force model.
     * @param propagatorBuilder propagator builder
     * @return drivers for the force model
     */
    protected abstract ParameterDriver[] setRelativity(T propagatorBuilder);

    /** Set polynomial acceleration force model.
     * @param propagatorBuilder propagator builder
     * @param name name of the acceleration
     * @param direction normalized direction of the acceleration
     * @param degree polynomial degree
     * @return drivers for the force model
     */
    protected abstract ParameterDriver[] setPolynomialAcceleration(T propagatorBuilder, String name,
                                                                   Vector3D direction, int degree);

    /** Set attitude provider.
     * @param propagatorBuilder propagator builder
     * @param attitudeProvider attitude provider
     */
    protected abstract void setAttitudeProvider(T propagatorBuilder, AttitudeProvider attitudeProvider);

    /** Run the program.
     * @param input input file
     * @param print if true, print logs
     * @throws IOException if input files cannot be read
     */
    protected ResultOD run(final File input, final boolean print) throws IOException {

        // read input parameters
        final KeyValueFileParser<ParameterKey> parser = new KeyValueFileParser<ParameterKey>(ParameterKey.class);
        try (FileInputStream fis = new FileInputStream(input)) {
            parser.parseInput(input.getAbsolutePath(), fis);
        }

        final RangeLog     rangeLog     = new RangeLog();
        final RangeRateLog rangeRateLog = new RangeRateLog();
        final AzimuthLog   azimuthLog   = new AzimuthLog();
        final ElevationLog elevationLog = new ElevationLog();
        final PositionLog  positionLog  = new PositionLog();
        final VelocityLog  velocityLog  = new VelocityLog();

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

        final Map<String, StationData>    stations                 = createStationsData(parser, conventions, body);
        final PVData                      pvData                   = createPVData(parser);
        final ObservableSatellite         satellite                = createObservableSatellite(parser);
        final Bias<Range>                 satRangeBias             = createSatRangeBias(parser);
        final OnBoardAntennaRangeModifier satAntennaRangeModifier  = createSatAntennaRangeModifier(parser);
        final Weights                     weights                  = createWeights(parser);
        final OutlierFilter<Range>        rangeOutliersManager     = createRangeOutliersManager(parser);
        final OutlierFilter<RangeRate>    rangeRateOutliersManager = createRangeRateOutliersManager(parser);
        final OutlierFilter<AngularAzEl>  azElOutliersManager      = createAzElOutliersManager(parser);
        final OutlierFilter<PV>           pvOutliersManager        = createPVOutliersManager(parser);

        // measurements
        final List<ObservedMeasurement<?>> measurements = new ArrayList<ObservedMeasurement<?>>();
        for (final String fileName : parser.getStringsList(ParameterKey.MEASUREMENTS_FILES, ',')) {

            // set up filtering for measurements files
            NamedData nd = new NamedData(fileName, () -> new FileInputStream(new File(input.getParentFile(), fileName)));
            for (final DataFilter filter : Arrays.asList(new GzipFilter(),
                                                         new UnixCompressFilter(),
                                                         new HatanakaCompressFilter())) {
                nd = filter.filter(nd);
            }

            if (Pattern.matches(RinexLoader.DEFAULT_RINEX_2_SUPPORTED_NAMES, nd.getName()) ||
                            Pattern.matches(RinexLoader.DEFAULT_RINEX_3_SUPPORTED_NAMES, nd.getName())) {
                // the measurements come from a Rinex file
                measurements.addAll(readRinex(nd,
                                              parser.getString(ParameterKey.SATELLITE_ID_IN_RINEX_FILES),
                                              stations, satellite, satRangeBias, satAntennaRangeModifier, weights,
                                              rangeOutliersManager, rangeRateOutliersManager));
            } else {
                // the measurements come from an Orekit custom file
                measurements.addAll(readMeasurements(nd,
                                                     stations, pvData, satellite,
                                                     satRangeBias, satAntennaRangeModifier, weights,
                                                     rangeOutliersManager,
                                                     rangeRateOutliersManager,
                                                     azElOutliersManager,
                                                     pvOutliersManager));
            }

        }
        for (ObservedMeasurement<?> measurement : measurements) {
            estimator.addMeasurement(measurement);
        }

        // estimate orbit
        if (print) {
            estimator.setObserver(new BatchLSObserver() {

                private PVCoordinates previousPV;
                {
                    previousPV = initialGuess.getPVCoordinates();
                    final String header = "iteration evaluations      ΔP(m)        ΔV(m/s)           RMS          nb Range    nb Range-rate  nb Angular     nb PV%n";
                    System.out.format(Locale.US, header);
                }

                /** {@inheritDoc} */
                @Override
                public void evaluationPerformed(final int iterationsCount, final int evaluationsCount,
                                                final Orbit[] orbits,
                                                final ParameterDriversList estimatedOrbitalParameters,
                                                final ParameterDriversList estimatedPropagatorParameters,
                                                final ParameterDriversList estimatedMeasurementsParameters,
                                                final EstimationsProvider  evaluationsProvider,
                                                final LeastSquaresProblem.Evaluation lspEvaluation) {
                    final PVCoordinates currentPV = orbits[0].getPVCoordinates();
                    final String format0 = "    %2d         %2d                                 %16.12f     %s       %s     %s     %s%n";
                    final String format  = "    %2d         %2d      %13.6f %12.9f %16.12f     %s       %s     %s     %s%n";
                    final EvaluationCounter<Range>     rangeCounter     = new EvaluationCounter<Range>();
                    final EvaluationCounter<RangeRate> rangeRateCounter = new EvaluationCounter<RangeRate>();
                    final EvaluationCounter<AngularAzEl>   angularCounter   = new EvaluationCounter<AngularAzEl>();
                    final EvaluationCounter<PV>        pvCounter        = new EvaluationCounter<PV>();
                    for (final Map.Entry<ObservedMeasurement<?>, EstimatedMeasurement<?>> entry : estimator.getLastEstimations().entrySet()) {
                        if (entry.getKey() instanceof Range) {
                            @SuppressWarnings("unchecked")
                            final EstimatedMeasurement<Range> evaluation = (EstimatedMeasurement<Range>) entry.getValue();
                            rangeCounter.add(evaluation);
                        } else if (entry.getKey() instanceof RangeRate) {
                            @SuppressWarnings("unchecked")
                            final EstimatedMeasurement<RangeRate> evaluation = (EstimatedMeasurement<RangeRate>) entry.getValue();
                            rangeRateCounter.add(evaluation);
                        } else if (entry.getKey() instanceof AngularAzEl) {
                            @SuppressWarnings("unchecked")
                            final EstimatedMeasurement<AngularAzEl> evaluation = (EstimatedMeasurement<AngularAzEl>) entry.getValue();
                            angularCounter.add(evaluation);
                        } else if (entry.getKey() instanceof PV) {
                            @SuppressWarnings("unchecked")
                            final EstimatedMeasurement<PV> evaluation = (EstimatedMeasurement<PV>) entry.getValue();
                            pvCounter.add(evaluation);
                        }
                    }
                    if (evaluationsCount == 1) {
                        System.out.format(Locale.US, format0,
                                          iterationsCount, evaluationsCount,
                                          lspEvaluation.getRMS(),
                                          rangeCounter.format(8), rangeRateCounter.format(8),
                                          angularCounter.format(8), pvCounter.format(8));
                    } else {
                        System.out.format(Locale.US, format,
                                          iterationsCount, evaluationsCount,
                                          Vector3D.distance(previousPV.getPosition(), currentPV.getPosition()),
                                          Vector3D.distance(previousPV.getVelocity(), currentPV.getVelocity()),
                                          lspEvaluation.getRMS(),
                                          rangeCounter.format(8), rangeRateCounter.format(8),
                                          angularCounter.format(8), pvCounter.format(8));
                    }
                    previousPV = currentPV;
                }
            });
        }
        final Orbit estimated = estimator.estimate()[0].getInitialState().getOrbit();

        // compute some statistics
        for (final Map.Entry<ObservedMeasurement<?>, EstimatedMeasurement<?>> entry : estimator.getLastEstimations().entrySet()) {
            if (entry.getKey() instanceof Range) {
                @SuppressWarnings("unchecked")
                final EstimatedMeasurement<Range> evaluation = (EstimatedMeasurement<Range>) entry.getValue();
                rangeLog.add(evaluation);
            } else if (entry.getKey() instanceof RangeRate) {
                @SuppressWarnings("unchecked")
                final EstimatedMeasurement<RangeRate> evaluation = (EstimatedMeasurement<RangeRate>) entry.getValue();
                rangeRateLog.add(evaluation);
            } else if (entry.getKey() instanceof AngularAzEl) {
                @SuppressWarnings("unchecked")
                final EstimatedMeasurement<AngularAzEl> evaluation = (EstimatedMeasurement<AngularAzEl>) entry.getValue();
                azimuthLog.add(evaluation);
                elevationLog.add(evaluation);
            } else if (entry.getKey() instanceof PV) {
                @SuppressWarnings("unchecked")
                final EstimatedMeasurement<PV> evaluation = (EstimatedMeasurement<PV>) entry.getValue();
                positionLog.add(evaluation);
                velocityLog.add(evaluation);
            }
        }

        final ParameterDriversList propagatorParameters   = estimator.getPropagatorParametersDrivers(true);
        final ParameterDriversList measurementsParameters = estimator.getMeasurementsParametersDrivers(true);
        return new ResultOD(propagatorParameters, measurementsParameters,
                            estimator.getIterationsCount(), estimator.getEvaluationsCount(), estimated.getPVCoordinates(),
                            rangeLog.createStatisticsSummary(),  rangeRateLog.createStatisticsSummary(),
                            azimuthLog.createStatisticsSummary(),  elevationLog.createStatisticsSummary(),
                            positionLog.createStatisticsSummary(),  velocityLog.createStatisticsSummary(),
                            estimator.getPhysicalCovariances(1.0e-10));

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
            final DataProvidersManager manager = DataProvidersManager.getInstance();
            manager.feed(msafe.getSupportedNames(), msafe);
            final Atmosphere atmosphere = new DTM2000(msafe, CelestialBodyFactory.getSun(), body);
            final ParameterDriver[] drivers = setDrag(propagatorBuilder, atmosphere, new IsotropicDrag(area, cd));
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
            final ParameterDriver[] drivers = setSolarRadiationPressure(propagatorBuilder, CelestialBodyFactory.getSun(),
                                                                        body.getEquatorialRadius(),
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
                final ParameterDriver[] drivers = setPolynomialAcceleration(propagatorBuilder, names[i], directions[i], coefficients[i].size() - 1);
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
            mode = AttitudeMode.NADIR_POINTING_WITH_YAW_COMPENSATION;
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
        PositionAngle angleType = PositionAngle.MEAN;
        if (parser.containsKey(ParameterKey.ORBIT_ANGLE_TYPE)) {
            angleType = PositionAngle.valueOf(parser.getString(ParameterKey.ORBIT_ANGLE_TYPE).toUpperCase());
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
    private OnBoardAntennaRangeModifier createSatAntennaRangeModifier(final KeyValueFileParser<ParameterKey> parser) {
        final Vector3D offset;
        if (!parser.containsKey(ParameterKey.ON_BOARD_ANTENNA_PHASE_CENTER_X)) {
            offset = Vector3D.ZERO;
        } else {
            offset = parser.getVector(ParameterKey.ON_BOARD_ANTENNA_PHASE_CENTER_X,
                                      ParameterKey.ON_BOARD_ANTENNA_PHASE_CENTER_Y,
                                      ParameterKey.ON_BOARD_ANTENNA_PHASE_CENTER_Z);
        }
        return offset.getNorm() > 0 ? new OnBoardAntennaRangeModifier(offset) : null;
    }

    /** Set up stations.
     * @param parser input file parser
     * @param conventions IERS conventions to use
     * @param body central body
     * @return name to station data map
          * @throws NoSuchElementException if input parameters are missing
     */
    private Map<String, StationData> createStationsData(final KeyValueFileParser<ParameterKey> parser,
                                                        final IERSConventions conventions,
                                                        final OneAxisEllipsoid body)
        throws NoSuchElementException {

        final Map<String, StationData> stations       = new HashMap<String, StationData>();

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
        //final boolean[] stationIonosphericCorrection    = parser.getBooleanArray(ParameterKey.GROUND_STATION_IONOSPHERIC_CORRECTION);

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
            final GeodeticPoint position = new GeodeticPoint(stationLatitudes[i],
                                                             stationLongitudes[i],
                                                             stationAltitudes[i]);
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
                    mappingModel = new GlobalMappingFunctionModel(stationLatitudes[i],
                                                                  stationLongitudes[i]);
                } else if (stationNiellMappingFunction[i]) {
                    mappingModel = new NiellMappingFunctionModel(stationLatitudes[i]);
                }

                final DiscreteTroposphericModel troposphericModel;
                if (stationTroposphericModelEstimated[i] && mappingModel != null) {

                    if (stationWeatherEstimated[i]) {
                        final GlobalPressureTemperatureModel weather = new GlobalPressureTemperatureModel(stationLatitudes[i],
                                                                                                          stationLongitudes[i],
                                                                                                          body.getBodyFrame());
                        weather.weatherParameters(stationAltitudes[i], parser.getDate(ParameterKey.ORBIT_DATE,
                                                                                      TimeScalesFactory.getUTC()));
                        final double temperature = weather.getTemperature();
                        final double pressure    = weather.getPressure();
                        troposphericModel = new EstimatedTroposphericModel(temperature, pressure, mappingModel,
                                                                           stationTroposphericZenithDelay[i]);
                    } else {
                        troposphericModel = new EstimatedTroposphericModel(mappingModel, stationTroposphericZenithDelay[i]);
                    }

                    final ParameterDriver totalDelay = troposphericModel.getParametersDrivers().get(0);
                    totalDelay.setSelected(stationZenithDelayEstimated[i]);
                    totalDelay.setName(stationNames[i].substring(0, 5) + EstimatedTroposphericModel.TOTAL_ZENITH_DELAY);
                } else {
                    troposphericModel = SaastamoinenModel.getStandardModel();
                }

                rangeTroposphericCorrection = new  RangeTroposphericDelayModifier(troposphericModel);
            } else {
                rangeTroposphericCorrection = null;
            }


            stations.put(stationNames[i],
                         new StationData(station,
                                         rangeSigma,     rangeBias,
                                         rangeRateSigma, rangeRateBias,
                                         azELSigma,      azELBias,
                                         refractionCorrection, rangeTroposphericCorrection));
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
     * @return outliers manager (null if none configured)
     */
    private OutlierFilter<Range> createRangeOutliersManager(final KeyValueFileParser<ParameterKey> parser) {
        needsBothOrNeither(parser,
                           ParameterKey.RANGE_OUTLIER_REJECTION_MULTIPLIER,
                           ParameterKey.RANGE_OUTLIER_REJECTION_STARTING_ITERATION);
        return new OutlierFilter<Range>(parser.getInt(ParameterKey.RANGE_OUTLIER_REJECTION_STARTING_ITERATION),
                                        parser.getInt(ParameterKey.RANGE_OUTLIER_REJECTION_MULTIPLIER));
    }

    /** Set up outliers manager for range-rate measurements.
     * @param parser input file parser
     * @return outliers manager (null if none configured)
     */
    private OutlierFilter<RangeRate> createRangeRateOutliersManager(final KeyValueFileParser<ParameterKey> parser) {
        needsBothOrNeither(parser,
                           ParameterKey.RANGE_RATE_OUTLIER_REJECTION_MULTIPLIER,
                           ParameterKey.RANGE_RATE_OUTLIER_REJECTION_STARTING_ITERATION);
        return new OutlierFilter<RangeRate>(parser.getInt(ParameterKey.RANGE_RATE_OUTLIER_REJECTION_STARTING_ITERATION),
                                            parser.getInt(ParameterKey.RANGE_RATE_OUTLIER_REJECTION_MULTIPLIER));
    }

    /** Set up outliers manager for azimuth-elevation measurements.
     * @param parser input file parser
     * @return outliers manager (null if none configured)
     */
    private OutlierFilter<AngularAzEl> createAzElOutliersManager(final KeyValueFileParser<ParameterKey> parser) {
        needsBothOrNeither(parser,
                           ParameterKey.AZ_EL_OUTLIER_REJECTION_MULTIPLIER,
                           ParameterKey.AZ_EL_OUTLIER_REJECTION_STARTING_ITERATION);
        return new OutlierFilter<AngularAzEl>(parser.getInt(ParameterKey.AZ_EL_OUTLIER_REJECTION_STARTING_ITERATION),
                                          parser.getInt(ParameterKey.AZ_EL_OUTLIER_REJECTION_MULTIPLIER));
    }

    /** Set up outliers manager for PV measurements.
     * @param parser input file parser
     * @return outliers manager (null if none configured)
     */
    private OutlierFilter<PV> createPVOutliersManager(final KeyValueFileParser<ParameterKey> parser) {
        needsBothOrNeither(parser,
                           ParameterKey.PV_OUTLIER_REJECTION_MULTIPLIER,
                           ParameterKey.PV_OUTLIER_REJECTION_STARTING_ITERATION);
        return new OutlierFilter<PV>(parser.getInt(ParameterKey.PV_OUTLIER_REJECTION_STARTING_ITERATION),
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
                                             final IntegratedPropagatorBuilder propagatorBuilder)
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

    /** Read a measurements file.
     * @param nd named data containing measurements
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
    private List<ObservedMeasurement<?>> readMeasurements(final NamedData nd,
                                                          final Map<String, StationData> stations,
                                                          final PVData pvData,
                                                          final ObservableSatellite satellite,
                                                          final Bias<Range> satRangeBias,
                                                          final OnBoardAntennaRangeModifier satAntennaRangeModifier,
                                                          final Weights weights,
                                                          final OutlierFilter<Range> rangeOutliersManager,
                                                          final OutlierFilter<RangeRate> rangeRateOutliersManager,
                                                          final OutlierFilter<AngularAzEl> azElOutliersManager,
                                                          final OutlierFilter<PV> pvOutliersManager)
        throws IOException {

        final List<ObservedMeasurement<?>> measurements = new ArrayList<ObservedMeasurement<?>>();
        try (InputStream is = nd.getStreamOpener().openStream();
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            int lineNumber = 0;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                ++lineNumber;
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("#")) {
                    final String[] fields = line.split("\\s+");
                    if (fields.length < 2) {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  lineNumber, nd.getName(), line);
                    }
                    switch (fields[1]) {
                        case "RANGE" :
                            final Range range = new RangeParser().parseFields(fields, stations, pvData, satellite,
                                                                              satRangeBias, weights,
                                                                              line, lineNumber, nd.getName());
                            if (satAntennaRangeModifier != null) {
                                range.addModifier(satAntennaRangeModifier);
                            }
                            if (rangeOutliersManager != null) {
                                range.addModifier(rangeOutliersManager);
                            }
                            addIfNonZeroWeight(range, measurements);
                            break;
                        case "RANGE_RATE" :
                            final RangeRate rangeRate = new RangeRateParser().parseFields(fields, stations, pvData, satellite,
                                                                                          satRangeBias, weights,
                                                                                          line, lineNumber, nd.getName());
                            if (rangeRateOutliersManager != null) {
                                rangeRate.addModifier(rangeRateOutliersManager);
                            }
                            addIfNonZeroWeight(rangeRate, measurements);
                            break;
                        case "AZ_EL" :
                            final AngularAzEl angular = new AzElParser().parseFields(fields, stations, pvData, satellite,
                                                                                     satRangeBias, weights,
                                                                                     line, lineNumber, nd.getName());
                            if (azElOutliersManager != null) {
                                angular.addModifier(azElOutliersManager);
                            }
                            addIfNonZeroWeight(angular, measurements);
                            break;
                        case "PV" :
                            final PV pv = new PVParser().parseFields(fields, stations, pvData, satellite,
                                                                     satRangeBias, weights,
                                                                     line, lineNumber, nd.getName());
                            if (pvOutliersManager != null) {
                                pv.addModifier(pvOutliersManager);
                            }
                            addIfNonZeroWeight(pv, measurements);
                            break;
                        default :
                            throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                                      "unknown measurement type " + fields[1] +
                                                      " at line " + lineNumber +
                                                      " in file " + nd.getName() +
                                                      "\n" + line);
                    }
                }
            }
        }

        if (measurements.isEmpty()) {
            throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                      "not measurements read from file " + nd.getName());
        }

        return measurements;

    }

    /** Read a RINEX measurements file.
     * @param nd named data containing measurements
     * @param satId satellite we are interested in
     * @param stations name to stations data map
     * @param satellite satellite reference
     * @param satRangeBias range bias due to transponder delay
     * @param satAntennaRangeModifier modifier for on-board antenna offset
     * @param weights base weights for measurements
     * @param rangeOutliersManager manager for range measurements outliers (null if none configured)
     * @param rangeRateOutliersManager manager for range-rate measurements outliers (null if none configured)
     * @return measurements list
     * @exception IOException if measurement file cannot be read
     */
    private List<ObservedMeasurement<?>> readRinex(final NamedData nd, final String satId,
                                                   final Map<String, StationData> stations,
                                                   final ObservableSatellite satellite,
                                                   final Bias<Range> satRangeBias,
                                                   final OnBoardAntennaRangeModifier satAntennaRangeModifier,
                                                   final Weights weights,
                                                   final OutlierFilter<Range> rangeOutliersManager,
                                                   final OutlierFilter<RangeRate> rangeRateOutliersManager)
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
        final Iono iono = new Iono(false);
        final RinexLoader loader = new RinexLoader(nd.getStreamOpener().openStream(), nd.getName());
        for (final ObservationDataSet observationDataSet : loader.getObservationDataSets()) {
            if (observationDataSet.getSatelliteSystem() == system    &&
                observationDataSet.getPrnNumber()       == prnNumber) {
                for (final ObservationData od : observationDataSet.getObservationData()) {
                    if (!Double.isNaN(od.getValue())) {
                        if (od.getObservationType().getMeasurementType() == MeasurementType.PSEUDO_RANGE) {
                            // this is a measurement we want
                            final String stationName = observationDataSet.getHeader().getMarkerName() + "/" + od.getObservationType();
                            final StationData stationData = stations.get(stationName);
                            if (stationData == null) {
                                throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                                          stationName + notConfigured);
                            }
                            final Range range = new Range(stationData.getStation(), false, observationDataSet.getDate(),
                                                          od.getValue(), stationData.getRangeSigma(),
                                                          weights.getRangeBaseWeight(), satellite);
                            range.addModifier(iono.getRangeModifier(od.getObservationType().getFrequency(system),
                                                                    observationDataSet.getDate()));
                            if (satAntennaRangeModifier != null) {
                                range.addModifier(satAntennaRangeModifier);
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

                        } else if (od.getObservationType().getMeasurementType() == MeasurementType.DOPPLER) {
                            // this is a measurement we want
                            final String stationName = observationDataSet.getHeader().getMarkerName() + "/" + od.getObservationType();
                            final StationData stationData = stations.get(stationName);
                            if (stationData == null) {
                                throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                                          stationName + notConfigured);
                            }
                            final RangeRate rangeRate = new RangeRate(stationData.getStation(), observationDataSet.getDate(),
                                                                      od.getValue(), stationData.getRangeRateSigma(),
                                                                      weights.getRangeRateBaseWeight(), false, satellite);
                            rangeRate.addModifier(iono.getRangeRateModifier(od.getObservationType().getFrequency(system),
                                                                            observationDataSet.getDate()));
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

}

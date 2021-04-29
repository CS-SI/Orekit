/* Copyright 2002-2021 CS GROUP
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.QRDecomposer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.GaussNewtonOptimizer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresOptimizer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresOptimizer.Optimum;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer;
import org.orekit.KeyValueFileParser;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataFilter;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DataSource;
import org.orekit.data.GzipFilter;
import org.orekit.data.UnixCompressFilter;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.estimation.leastsquares.BatchLSObserver;
import org.orekit.estimation.leastsquares.SequentialBatchLSEstimator;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationsProvider;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.Position;
import org.orekit.files.ilrs.CPFFile;
import org.orekit.files.ilrs.CPFFile.CPFCoordinate;
import org.orekit.files.ilrs.CPFFile.CPFEphemeris;
import org.orekit.files.ilrs.CPFParser;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.gnss.HatanakaCompressFilter;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.models.earth.atmosphere.DTM2000;
import org.orekit.models.earth.atmosphere.data.MarshallSolarActivityFutureEstimation;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.conversion.DormandPrince853IntegratorBuilder;
import org.orekit.propagation.conversion.ODEIntegratorBuilder;
import org.orekit.propagation.conversion.OrbitDeterminationPropagatorBuilder;
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
 * @author Julie Bayard
 */
public abstract class AbstractSequentialOrbitDetermination<T extends OrbitDeterminationPropagatorBuilder> {

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
     * @param equatorialRadius central body equatorial radius (for shadow computation)
     * @param spacecraft spacecraft model
     * @return drivers for the force model
     */
    protected abstract List<ParameterDriver> setSolarRadiationPressure(T propagatorBuilder, CelestialBody sun,
                                                                       double equatorialRadius, RadiationSensitive spacecraft);

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

    /** Run the sequential batch least squares.
     * @param input input file
     * @param print if true, print logs
     * @throws IOException if input files cannot be read
     */
    protected ResultSequentialBatchLeastSquares runSequentialBLS(final File inputModel, final boolean print) throws IOException {

        // read input parameters
        final KeyValueFileParser<ParameterKey> parser = new KeyValueFileParser<ParameterKey>(ParameterKey.class);
        try (FileInputStream fis = new FileInputStream(inputModel)) {
            parser.parseInput(inputModel.getAbsolutePath(), fis);
        }

        final PositionOnlyLog  positionLog  = new PositionOnlyLog();
        final PositionOnlyLog  positionLogS = new PositionOnlyLog();
        
        // gravity field
        createGravityField(parser);

        // read first measurements file to build the batch least squares
        CPFEphemeris ephemerisBLS = readCpf(parser, inputModel, 0);

        // frame related to the ephemeris
        final Frame frameBLS = ephemerisBLS.getFrame();
        
        // Orbit initial guess
        final Orbit initialGuess = createOrbit(parser, frameBLS, getMu());

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
        final BatchLSEstimator estimatorBLS = createBLSEstimator(parser, propagatorBuilder);
        
        final ObservableSatellite satellite = createObservableSatellite(parser);

        // measurements
        for (CPFCoordinate coordinates : ephemerisBLS.getCoordinates()) {
            AbsoluteDate date = coordinates.getDate();
            final PVCoordinates pvInertial = frameBLS.getTransformTo(initialGuess.getFrame(), date).transformPVCoordinates(coordinates);
            estimatorBLS.addMeasurement(new Position(date, pvInertial.getPosition(), 1, 1, satellite));
        }
        
        final String headerBLS = "\nBatch Least Square Estimator :\n"
                        + "iteration evaluations      ΔP(m)        ΔV(m/s)           RMS             nb Position%n";
        estimatorBLS.setObserver(new Observer(initialGuess, estimatorBLS, headerBLS, print));

        // perform first estimation
        final Orbit estimatedBLS = estimatorBLS.estimate()[0].getInitialState().getOrbit();
        
        Optimum BLSEvaluation = estimatorBLS.getOptimum();

        // read second measurements file to build the sequential batch least squares
        CPFEphemeris ephemerisSBLS = readCpf(parser, inputModel, 1);
        final SequentialBatchLSEstimator estimatorSBLS = createSBLSEstimator(BLSEvaluation, parser, propagatorBuilder);

        // measurements
        for (CPFCoordinate coordinates : ephemerisSBLS.getCoordinates()) {
            AbsoluteDate date = coordinates.getDate();
            final PVCoordinates pvInertial = frameBLS.getTransformTo(initialGuess.getFrame(), date).transformPVCoordinates(coordinates);
            estimatorSBLS.addMeasurement(new Position(date, pvInertial.getPosition(), 1, 1, satellite));
        }
        
        final String headerSBLS = "\nSequentiel Batch Least Square Estimator :\n"
                        + "iteration evaluations      ΔP(m)        ΔV(m/s)           RMS             nb Position%n";
        
        estimatorSBLS.setObserver(new Observer(initialGuess, estimatorSBLS, headerSBLS, print));
        
        final Orbit estimatedSBLS = estimatorSBLS.estimate()[0].getInitialState().getOrbit();
        
        // compute some statistics
        for (final Map.Entry<ObservedMeasurement<?>, EstimatedMeasurement<?>> entry : estimatorBLS.getLastEstimations().entrySet()) {
            logEvaluation(entry.getValue(), positionLog);
        }
        for (final Map.Entry<ObservedMeasurement<?>, EstimatedMeasurement<?>> entry : estimatorSBLS.getLastEstimations().entrySet()) {
            logEvaluation(entry.getValue(), positionLogS);
        }

        final ParameterDriversList propagatorParameters   = estimatorBLS.getPropagatorParametersDrivers(true);
        final ParameterDriversList measurementsParameters = estimatorBLS.getMeasurementsParametersDrivers(true);
        
        return new ResultSequentialBatchLeastSquares(propagatorParameters, measurementsParameters,
                                           estimatorBLS.getIterationsCount(), estimatorBLS.getEvaluationsCount(), estimatedBLS.getPVCoordinates(),
                                           positionLog.createStatisticsSummary(),
                                           estimatorBLS.getPhysicalCovariances(1.0e-10), 
                                           estimatorSBLS.getIterationsCount(), estimatorSBLS.getEvaluationsCount(),
                                           estimatedSBLS.getPVCoordinates(), positionLogS.createStatisticsSummary(),
                                           estimatorSBLS.getPhysicalCovariances(1.0e-10));

    }

    /**
     * Read a position CPF file
     * @param parser key/value parser
     * @param input input data
     * @param fileIndex index of the file to use (0 for BLS, 1 for Sequential-BLS)
     * @return the ephemeris contained in the file
     * @throws IOException if file cannot be read
     */
     private CPFEphemeris readCpf(final KeyValueFileParser<ParameterKey> parser,
                                  final File input,
                                  final int fileIndex) throws IOException {
         final String fileName = parser.getStringsList(ParameterKey.MEASUREMENTS_FILES, ',').get(fileIndex);

         // set up filtering for measurements files
         DataSource nd = new DataSource(fileName,
                                      () -> new FileInputStream(new File(input.getParentFile(), fileName)));
         for (final DataFilter filter : Arrays.asList(new GzipFilter(),
                                                      new UnixCompressFilter(),
                                                      new HatanakaCompressFilter())) {
             nd = filter.filter(nd);
         }

         final CPFParser parserCpf = new CPFParser();
         final CPFFile file = (CPFFile) parserCpf.parse(nd);
         return file.getSatellites().get(file.getHeader().getIlrsSatelliteId());
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
            final DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
            manager.feed(msafe.getSupportedNames(), msafe);
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
    private Orbit createOrbit(final KeyValueFileParser<ParameterKey> parser, Frame frame, final double mu)
        throws NoSuchElementException {

        final Frame eme2000 = FramesFactory.getEME2000();
        
        final double[] pos = {parser.getDouble(ParameterKey.ORBIT_CARTESIAN_PX),
            parser.getDouble(ParameterKey.ORBIT_CARTESIAN_PY),
            parser.getDouble(ParameterKey.ORBIT_CARTESIAN_PZ)};
        final double[] vel = {parser.getDouble(ParameterKey.ORBIT_CARTESIAN_VX),
            parser.getDouble(ParameterKey.ORBIT_CARTESIAN_VY),
            parser.getDouble(ParameterKey.ORBIT_CARTESIAN_VZ)};

        final AbsoluteDate date = parser.getDate(ParameterKey.ORBIT_DATE, TimeScalesFactory.getUTC());
        final PVCoordinates pvITRF = new PVCoordinates(new Vector3D(pos), new Vector3D(vel));
        final PVCoordinates pvInertial = frame.getTransformTo(eme2000, date).transformPVCoordinates(pvITRF);
        
        return new CartesianOrbit(pvInertial, eme2000, date, mu);
        
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
    private BatchLSEstimator createBLSEstimator(final KeyValueFileParser<ParameterKey> parser,
                                             final OrbitDeterminationPropagatorBuilder propagatorBuilder)
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
            optimizer = new GaussNewtonOptimizer(new QRDecomposer(1e-11), true);
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
    private SequentialBatchLSEstimator createSBLSEstimator(final Optimum optimum, final KeyValueFileParser<ParameterKey> parser,
                                             final OrbitDeterminationPropagatorBuilder propagatorBuilder)
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

        final SequentialBatchLSEstimator estimator = new SequentialBatchLSEstimator(optimum, propagatorBuilder);
        estimator.setParametersConvergenceThreshold(convergenceThreshold);
        estimator.setMaxIterations(maxIterations);
        estimator.setMaxEvaluations(maxEvaluations);

        return estimator;

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

    /** Log evaluations.
     */
    private void logEvaluation(EstimatedMeasurement<?> evaluation,
                               EvaluationLogger<Position> positionLog) {
        if (evaluation.getObservedMeasurement() instanceof PV) {
            @SuppressWarnings("unchecked")
            final EstimatedMeasurement<Position> ev = (EstimatedMeasurement<Position>) evaluation;
            if (positionLog != null) {
                positionLog.log(ev);
            }
        }
    } 

    /** Orbit determination observer. */
    class Observer implements BatchLSObserver{
        
        private PVCoordinates previousPV;
        private BatchLSEstimator estimator;
        private boolean print;
        
        
        public Observer(Orbit initialGuess, final BatchLSEstimator estimator, final String header, final boolean print){
            previousPV = initialGuess.getPVCoordinates();
            this.estimator = estimator;
            this.print = print;
            if (print) System.out.format(Locale.US, header);
        }

        @Override
        /** {@inheritDoc} */
        public void evaluationPerformed(final int iterationsCount, final int evaluationsCount,
                                        final Orbit[] orbits,
                                        final ParameterDriversList estimatedOrbitalParameters,
                                        final ParameterDriversList estimatedPropagatorParameters,
                                        final ParameterDriversList estimatedMeasurementsParameters,
                                        final EstimationsProvider  evaluationsProvider,
                                        final LeastSquaresProblem.Evaluation lspEvaluation) {
            
            if (print) {
            
                final PVCoordinates currentPV = orbits[0].getPVCoordinates();
                final String format0 = "    %2d         %2d                                 %16.12f     %s%n";
                final String format  = "    %2d         %2d      %13.6f %12.9f %16.12f     %s%n";
                final EvaluationCounter<Position>          positionCounter        = new EvaluationCounter<Position>();
                for (final Map.Entry<ObservedMeasurement<?>, EstimatedMeasurement<?>> entry : estimator.getLastEstimations().entrySet()) {
                    logEvaluation(entry.getValue(), positionCounter);
                }
                if (evaluationsCount == 1) {
                    System.out.format(Locale.US, format0,
                                      iterationsCount, evaluationsCount,
                                      lspEvaluation.getRMS(),
                                      positionCounter.format(8));
                } else {
                    System.out.format(Locale.US, format,
                                      iterationsCount, evaluationsCount,
                                      Vector3D.distance(previousPV.getPosition(), currentPV.getPosition()),
                                      Vector3D.distance(previousPV.getVelocity(), currentPV.getVelocity()),
                                      lspEvaluation.getRMS(),
                                      positionCounter.format(8));
                }
                previousPV = currentPV;
            }
        }
    }
    
}




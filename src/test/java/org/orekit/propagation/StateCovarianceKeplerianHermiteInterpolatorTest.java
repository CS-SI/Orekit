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
package org.orekit.propagation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hipparchus.analysis.polynomials.SmoothStepFactory;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.BlockRealMatrix;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.OceanTides;
import org.orekit.forces.gravity.Relativity;
import org.orekit.forces.gravity.SolidTides;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.AstronomicalAmplitudeReader;
import org.orekit.forces.gravity.potential.EGMFormatReader;
import org.orekit.forces.gravity.potential.FESCHatEpsilonReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.OceanLoadDeformationCoefficients;
import org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient;
import org.orekit.forces.radiation.KnockeRediffusedForceModel;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.models.earth.atmosphere.NRLMSISE00;
import org.orekit.models.earth.atmosphere.data.CssiSpaceWeatherData;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitBlender;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterpolator;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.TimeStampedPair;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class StateCovarianceKeplerianHermiteInterpolatorTest {
    private static Orbit  sergeiOrbit;
    private static Frame  sergeiFrame;
    private final  int    DEFAULT_SERGEI_INTERPOLATION_POINTS = 2;
    private final  double DEFAULT_SERGEI_PROPAGATION_TIME     = 2400;
    private final  double DEFAULT_SERGEI_TABULATED_TIMESTEP   = 2400;

    @BeforeAll
    public static void setUp() {
        Utils.setDataRoot("regular-data:potential/egm-format:atmosphere:tides:regular-data/de405-ephemerides");
        GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("EGM96-truncated-21x21", true));
        AstronomicalAmplitudeReader aaReader =
                new AstronomicalAmplitudeReader("hf-fes2004.dat", 5, 2, 3, 1.0);
        DataContext.getDefault().getDataProvidersManager().feed(aaReader.getSupportedNames(), aaReader);
        Map<Integer, Double> map = aaReader.getAstronomicalAmplitudesMap();
        GravityFieldFactory.addOceanTidesReader(new FESCHatEpsilonReader("fes2004-7x7.dat",
                                                                         0.01, FastMath.toRadians(1.0),
                                                                         OceanLoadDeformationCoefficients.IERS_2010,
                                                                         map));

        sergeiOrbit = generateSergeiReferenceOrbit();
        sergeiFrame = sergeiOrbit.getFrame();
    }

    public static DescriptiveStatistics[] computeStatisticsCovarianceLOFInterpolation(final double propagationDuration,
                                                                                      final double tabulatedTimeStep,
                                                                                      final LOFType lofType,
                                                                                      final TimeInterpolator<TimeStampedPair<Orbit, StateCovariance>> covarianceLOFInterpolator) {
        // Given
        final List<StateCovariance>                         referenceCovariances          = new ArrayList<>();
        final List<StateCovariance>                         interpolatedCovariances       = new ArrayList<>();
        final List<TimeStampedPair<Orbit, StateCovariance>> tabulatedOrbitsAndCovariances = new ArrayList<>();

        // Initialize reference state
        final SpacecraftState sergeiState = generateSergeiReferenceState();
        final Orbit           sergeiOrbit = sergeiState.getOrbit();
        final AbsoluteDate    initialDate = sergeiOrbit.getDate();

        // Initialize reference covariance matrix
        final RealMatrix sergeiCovarianceMatrix = generateSergeiCovarianceMatrix();

        // Initialize propagator
        final NumericalPropagator propagator = new NumericalPropagator(
                generateDefaultIntegrator(sergeiOrbit, OrbitType.CARTESIAN));

        propagator.setOrbitType(OrbitType.CARTESIAN);

        // Initialize harvester
        final MatricesHarvester harvester =
                propagator.setupMatricesComputation("harvester", null, null);

        // Initialize state covariance matrix provider
        final StateCovariance sergeiCovariance =
                new StateCovariance(sergeiCovarianceMatrix, sergeiState.getDate(), sergeiState.getFrame(),
                                    OrbitType.CARTESIAN, PositionAngleType.MEAN);

        final StateCovarianceMatrixProvider stateCovarianceMatrixProvider =
                new StateCovarianceMatrixProvider("covariance", "harvester", harvester, sergeiCovariance);

        // Configuring propagator
        propagator.setInitialState(sergeiState);

        configurePropagatorForSergeiCase(propagator);

        propagator.addAdditionalStateProvider(stateCovarianceMatrixProvider);

        propagator.getMultiplexer().add(1, (currentState) -> {

            // Save reference covariance
            final StateCovariance currentCovarianceFromProviderInCartesian =
                    stateCovarianceMatrixProvider.getStateCovariance(currentState);

            // Convert to LOF
            final StateCovariance currentCovarianceFromProviderInLOF =
                    currentCovarianceFromProviderInCartesian.changeCovarianceFrame(currentState.getOrbit(), lofType);

            referenceCovariances.add(currentCovarianceFromProviderInLOF);

            // Save tabulated orbit and covariance
            final double durationFromStart = currentState.getDate().durationFrom(sergeiState.getDate());
            if (durationFromStart % tabulatedTimeStep == 0) {
                tabulatedOrbitsAndCovariances.add(new TimeStampedPair<>(currentState.getOrbit(),
                                                                        currentCovarianceFromProviderInCartesian));
            }
        });

        // Propagation
        propagator.propagate(initialDate.shiftedBy(propagationDuration));

        // Interpolate
        for (int dt = 0; dt < referenceCovariances.size(); dt++) {
            final AbsoluteDate currentInterpolationDate = initialDate.shiftedBy(dt);
            final StateCovariance currentInterpolatedCovariance =
                    covarianceLOFInterpolator.interpolate(currentInterpolationDate, tabulatedOrbitsAndCovariances)
                                             .getSecond();

            interpolatedCovariances.add(currentInterpolatedCovariance);
        }

        // Make statistics
        return computeRMSRelativeErrorsStatistics(referenceCovariances,
                                                  interpolatedCovariances);

    }

    /**
     * Test given covariance interpolator on full force model test case from "TANYGIN, Sergei. Efficient covariance
     * interpolation using blending of approximate covariance propagations. The Journal of the Astronautical Sciences, 2014,
     * vol. 61, no 1, p. 107-132.".
     * <p>
     * For testing efficiency purpose, the propagation time shall be reduced instead of propagating throughout 2 hours.
     *
     * @param propagationDuration propagation duration (< 7200s)
     * @param tabulatedTimeStep propagation time step
     * @param covarianceInterpolator covariance interpolator to test
     *
     * @return statistics on relative position and velocity sigmas error throughout the interpolation.
     */
    public static DescriptiveStatistics[] computeStatisticsCovarianceInterpolationOnSergeiCase(
            final double propagationDuration,
            final double tabulatedTimeStep,
            final TimeInterpolator<SpacecraftState> stateInterpolator,
            final TimeInterpolator<TimeStampedPair<Orbit, StateCovariance>> covarianceInterpolator) {

        // Given
        final List<StateCovariance> referenceCovariances    = new ArrayList<>();
        final List<StateCovariance> interpolatedCovariances = new ArrayList<>();
        final List<SpacecraftState> tabulatedStates         = new ArrayList<>();
        final List<StateCovariance> tabulatedCovariances    = new ArrayList<>();

        // Initialize reference state
        final SpacecraftState sergeiState = generateSergeiReferenceState();
        final Orbit           sergeiOrbit = sergeiState.getOrbit();
        final AbsoluteDate    initialDate = sergeiOrbit.getDate();

        // Initialize reference covariance matrix
        final RealMatrix sergeiCovarianceMatrix = generateSergeiCovarianceMatrix();

        // Initialize propagator
        final NumericalPropagator propagator = new NumericalPropagator(
                generateDefaultIntegrator(sergeiOrbit, OrbitType.CARTESIAN));

        propagator.setOrbitType(OrbitType.CARTESIAN);

        // Initialize harvester
        final MatricesHarvester harvester =
                propagator.setupMatricesComputation("harvester", null, null);

        // Initialize state covariance matrix provider
        final StateCovariance sergeiCovariance =
                new StateCovariance(sergeiCovarianceMatrix, sergeiState.getDate(), sergeiState.getFrame(),
                                    OrbitType.CARTESIAN, PositionAngleType.MEAN);

        final StateCovarianceMatrixProvider stateCovarianceMatrixProvider =
                new StateCovarianceMatrixProvider("covariance", "harvester", harvester, sergeiCovariance);

        // Configuring propagator
        propagator.setInitialState(sergeiState);

        configurePropagatorForSergeiCase(propagator);

        propagator.addAdditionalStateProvider(stateCovarianceMatrixProvider);

        propagator.getMultiplexer().add(1, (currentState) -> {

            // Save reference covariance
            final StateCovariance currentCovarianceFromProviderInCartesian =
                    stateCovarianceMatrixProvider.getStateCovariance(currentState);

            referenceCovariances.add(currentCovarianceFromProviderInCartesian);

            // Save tabulated state and covariance
            final double durationFromStart = currentState.getDate().durationFrom(sergeiState.getDate());
            if (durationFromStart % tabulatedTimeStep == 0) {
                tabulatedStates.add(currentState);
                tabulatedCovariances.add(currentCovarianceFromProviderInCartesian);
            }
        });

        // Propagation
        propagator.propagate(initialDate.shiftedBy(propagationDuration));

        // Create custom Ephemeris
        final Ephemeris ephemeris =
                new Ephemeris(tabulatedStates, stateInterpolator, tabulatedCovariances, covarianceInterpolator);

        // Interpolate
        for (int dt = 0; dt < referenceCovariances.size(); dt++) {
            final AbsoluteDate currentInterpolationDate = initialDate.shiftedBy(dt);

            interpolatedCovariances.add(ephemeris.getCovariance(currentInterpolationDate).get());
        }

        // Make statistics
        return computeRMSRelativeErrorsStatistics(referenceCovariances,
                                                  interpolatedCovariances);

    }

    /**
     * Compute statistics about RMS of relative error on position and velocity sigmas.
     *
     * @param referenceCovariances reference covariances
     * @param interpolatedCovariances interpolated covariances
     *
     * @return statistics about RMS of relative error on position and velocity sigmas
     */
    public static DescriptiveStatistics[] computeRMSRelativeErrorsStatistics(
            final List<StateCovariance> referenceCovariances,
            final List<StateCovariance> interpolatedCovariances) {
        final DescriptiveStatistics[] maxRelativeRMSPosAndVelError = new DescriptiveStatistics[2];

        final DescriptiveStatistics relativeRMSPosSigmaErrorStat = new DescriptiveStatistics();
        final DescriptiveStatistics relativeRMSVelSigmaErrorStat = new DescriptiveStatistics();

        for (int i = 0; i < referenceCovariances.size(); i++) {

            final RealMatrix currentReferenceCovariance    = referenceCovariances.get(i).getMatrix();
            final RealMatrix currentInterpolatedCovariance = interpolatedCovariances.get(i).getMatrix();

            final double[] currentReferenceSigmas    = extractSigmas(currentReferenceCovariance);
            final double[] currentInterpolatedSigmas = extractSigmas(currentInterpolatedCovariance);

            final double[] currentReferencePosAndVelSigmas = computeRMSSigmaArray(currentReferenceCovariance);

            final double[] deltaPositionSigmas = new double[] {
                    currentReferenceSigmas[0] - currentInterpolatedSigmas[0],
                    currentReferenceSigmas[1] - currentInterpolatedSigmas[1],
                    currentReferenceSigmas[2] - currentInterpolatedSigmas[2] };

            final double[] deltaVelocitySigmas = new double[] {
                    currentReferenceSigmas[3] - currentInterpolatedSigmas[3],
                    currentReferenceSigmas[4] - currentInterpolatedSigmas[4],
                    currentReferenceSigmas[5] - currentInterpolatedSigmas[5] };

            // Add to statistics
            final double relativeRMSPosSigmaError =
                    computeRMS(deltaPositionSigmas) * 100 / currentReferencePosAndVelSigmas[0];
            final double relativeRMSVelSigmaError =
                    computeRMS(deltaVelocitySigmas) * 100 / currentReferencePosAndVelSigmas[1];

            relativeRMSPosSigmaErrorStat.addValue(relativeRMSPosSigmaError);
            relativeRMSVelSigmaErrorStat.addValue(relativeRMSVelSigmaError);
        }
        maxRelativeRMSPosAndVelError[0] = relativeRMSPosSigmaErrorStat;
        maxRelativeRMSPosAndVelError[1] = relativeRMSVelSigmaErrorStat;

        return maxRelativeRMSPosAndVelError;
    }

    public static double[] computeRMSSigmaArray(final RealMatrix covarianceMatrix) {

        // RMS sigma for position and velocity
        final double[] rmsSigmaArray = new double[2];

        final double[] positionSigmas = new double[] {
                FastMath.sqrt(FastMath.abs(covarianceMatrix.getEntry(0, 0))),
                FastMath.sqrt(FastMath.abs(covarianceMatrix.getEntry(1, 1))),
                FastMath.sqrt(FastMath.abs(covarianceMatrix.getEntry(2, 2))) };

        final double[] velocitySigmas = new double[] {
                FastMath.sqrt(FastMath.abs(covarianceMatrix.getEntry(3, 3))),
                FastMath.sqrt(FastMath.abs(covarianceMatrix.getEntry(4, 4))),
                FastMath.sqrt(FastMath.abs(covarianceMatrix.getEntry(5, 5))) };

        rmsSigmaArray[0] = computeRMS(positionSigmas);
        rmsSigmaArray[1] = computeRMS(velocitySigmas);

        return rmsSigmaArray;
    }

    /**
     * Compute RMS of given data array.
     *
     * @param data data array
     *
     * @return RMS of given data array
     */
    public static double computeRMS(final double[] data) {
        double sum = 0;
        for (final double element : data) {
            sum += element * element;
        }
        return FastMath.sqrt(sum / data.length);
    }

    /**
     * Extracts diagonal sigmas from given covariance 6x6 matrix.
     *
     * @param covarianceMatrix 6x6 covariance matrix
     *
     * @return diagonal sigmas
     */
    public static double[] extractSigmas(final RealMatrix covarianceMatrix) {
        return new double[]
                { FastMath.sqrt(FastMath.abs(covarianceMatrix.getEntry(0, 0))),
                  FastMath.sqrt(FastMath.abs(covarianceMatrix.getEntry(1, 1))),
                  FastMath.sqrt(FastMath.abs(covarianceMatrix.getEntry(2, 2))),
                  FastMath.sqrt(FastMath.abs(covarianceMatrix.getEntry(3, 3))),
                  FastMath.sqrt(FastMath.abs(covarianceMatrix.getEntry(4, 4))),
                  FastMath.sqrt(FastMath.abs(covarianceMatrix.getEntry(5, 5))) };
    }

    public static ODEIntegrator generateDefaultIntegrator(final Orbit orbit, final OrbitType orbitType) {
        final double     dP         = 1;
        final double[][] tolerances = NumericalPropagator.tolerances(dP, orbit, orbitType);
        final double     minStep    = 0.001;
        final double     maxStep    = 300.;

        return new DormandPrince853Integrator(minStep, maxStep, tolerances[0], tolerances[1]);
    }

    public static void configurePropagatorForSergeiCase(final NumericalPropagator propagator) {

        // Initialization
        final IERSConventions conventions = IERSConventions.IERS_2010;
        final Frame           itrf        = FramesFactory.getITRF(conventions, true);
        final BodyShape earthShape = new OneAxisEllipsoid(Constants.IERS2010_EARTH_EQUATORIAL_RADIUS,
                                                          Constants.IERS2010_EARTH_FLATTENING, itrf);

        final CelestialBody sun  = CelestialBodyFactory.getSun();
        final CelestialBody moon = CelestialBodyFactory.getMoon();

        // Gravity fields
        final NormalizedSphericalHarmonicsProvider gravity =
                GravityFieldFactory.getNormalizedProvider(21, 21);
        propagator.addForceModel(new HolmesFeatherstoneAttractionModel(itrf, gravity));

        // Object dependant forces
        final double crossSection = 20; // In m2

        // Solar radiation pressure (including Earth albedo)
        final double             cr                 = 1;
        final RadiationSensitive radiationSensitive = new IsotropicRadiationSingleCoefficient(crossSection, cr);
        final SolarRadiationPressure solarRadiationPressure =
                new SolarRadiationPressure(sun,
                                           ReferenceEllipsoid.getIers2010(itrf),
                                           radiationSensitive);

        // Earth is already considered as an occulting body
        solarRadiationPressure.addOccultingBody(moon, Constants.MOON_EQUATORIAL_RADIUS);

        final KnockeRediffusedForceModel albedo =
                new KnockeRediffusedForceModel(sun, radiationSensitive,
                                               Constants.IERS2010_EARTH_EQUATORIAL_RADIUS,
                                               FastMath.toRadians(5));

        propagator.addForceModel(solarRadiationPressure);
        propagator.addForceModel(albedo);

        // Drag
        final double        cd            = 2.2;
        final IsotropicDrag dragSensitive = new IsotropicDrag(crossSection, cd);
        final Atmosphere atmosphere =
                new NRLMSISE00(new CssiSpaceWeatherData(CssiSpaceWeatherData.DEFAULT_SUPPORTED_NAMES), sun, earthShape);
        final DragForce dragForce = new DragForce(atmosphere, dragSensitive);
        propagator.addForceModel(dragForce);

        // Third body
        final ThirdBodyAttraction moonAttraction = new ThirdBodyAttraction(moon);
        final ThirdBodyAttraction sunAttraction  = new ThirdBodyAttraction(sun);

        propagator.addForceModel(moonAttraction);
        propagator.addForceModel(sunAttraction);

        // Solid tides
        final SolidTides solidTides = new SolidTides(earthShape.getBodyFrame(), gravity.getAe(), gravity.getMu(),
                                                     gravity.getTideSystem(), conventions,
                                                     TimeScalesFactory.getUT1(conventions, true),
                                                     moon, sun);
        propagator.addForceModel(solidTides);

        // Ocean tides
        final OceanTides oceanTides = new OceanTides(earthShape.getBodyFrame(), gravity.getAe(), gravity.getMu(),
                                                     4, 4, conventions,
                                                     TimeScalesFactory.getUT1(conventions, true));
        propagator.addForceModel(oceanTides);

        // Relativity correction
        final Relativity relativity = new Relativity(Constants.IERS2010_EARTH_MU);
        propagator.addForceModel(relativity);

    }

    public static SpacecraftState generateSergeiReferenceState() {
        final Orbit  orbit      = generateSergeiReferenceOrbit();
        final double sergeiMass = 0.04;
        return new SpacecraftState(orbit, sergeiMass);
    }

    public static Orbit generateSergeiReferenceOrbit() {
        // Initial sergei date that was modified in order to use the JPL ephemerides stored in the resources
        //final AbsoluteDate sergeiDate = new AbsoluteDate(2008, 11, 22, 19, 0, 0, TimeScalesFactory.getUTC());
        final AbsoluteDate sergeiDate = new AbsoluteDate(2003, 11, 22, 19, 0, 0, TimeScalesFactory.getUTC());

        return new CartesianOrbit(new PVCoordinates(
                new Vector3D(-2397200, 4217850, 5317450),
                new Vector3D(-1303.9, 5558.9, -4839.6)), FramesFactory.getGCRF(), sergeiDate, Constants.EIGEN5C_EARTH_MU);
    }

    public static RealMatrix generateSergeiCovarianceMatrix() {

        // Sigma in position (in m)
        final double sigmaPosX = 98676;
        final double sigmaPosY = 420547;
        final double sigmaPosZ = 366438;

        // Sigma in velocity (in m/s)
        final double sigmaVelX = 194;
        final double sigmaVelY = 341;
        final double sigmaVelZ = 430;

        // Correlation in position
        final double posXYCorr = -0.999985;
        final double posXZCorr = 0.999983;
        final double posYZCorr = -0.999997;

        // Correlation in velocity
        final double velXYCorr = -0.999998;
        final double velXZCorr = -0.999997;
        final double velYZCorr = 0.999997;

        // Cross correlations
        final double posXVelXCorr = -0.999982;
        final double posXVelYCorr = 0.999989;
        final double posXVelZCorr = 0.999983;

        final double posYVelXCorr = 0.999997;
        final double posYVelYCorr = -0.999999;
        final double posYVelZCorr = -0.999995;

        final double posZVelXCorr = -0.999996;
        final double posZVelYCorr = 0.999996;
        final double posZVelZCorr = 0.999999;

        return new BlockRealMatrix(new double[][] {
                { sigmaPosX * sigmaPosX, sigmaPosX * sigmaPosY * posXYCorr, sigmaPosX * sigmaPosZ * posXZCorr,
                  sigmaPosX * sigmaVelX * posXVelXCorr, sigmaPosX * sigmaVelY * posXVelYCorr,
                  sigmaPosX * sigmaVelZ * posXVelZCorr },
                { sigmaPosX * sigmaPosY * posXYCorr, sigmaPosY * sigmaPosY, sigmaPosY * sigmaPosZ * posYZCorr,
                  sigmaPosY * sigmaVelX * posYVelXCorr, sigmaPosY * sigmaVelY * posYVelYCorr,
                  sigmaPosY * sigmaVelZ * posYVelZCorr },
                { sigmaPosX * sigmaPosZ * posXZCorr, sigmaPosY * sigmaPosZ * posYZCorr, sigmaPosZ * sigmaPosZ,
                  sigmaPosZ * sigmaVelX * posZVelXCorr, sigmaPosZ * sigmaVelY * posZVelYCorr,
                  sigmaPosZ * sigmaVelZ * posZVelZCorr },
                { sigmaPosX * sigmaVelX * posXVelXCorr, sigmaPosY * sigmaVelX * posYVelXCorr,
                  sigmaPosZ * sigmaVelX * posZVelXCorr, sigmaVelX * sigmaVelX, sigmaVelX * sigmaVelY * velXYCorr,
                  sigmaVelX * sigmaVelZ * velXZCorr },
                { sigmaPosX * sigmaVelY * posXVelYCorr, sigmaPosY * sigmaVelY * posYVelYCorr,
                  sigmaPosZ * sigmaVelY * posZVelYCorr, sigmaVelX * sigmaVelY * velXYCorr, sigmaVelY * sigmaVelY,
                  sigmaVelY * sigmaVelZ * velYZCorr },
                { sigmaPosX * sigmaVelZ * posXVelZCorr, sigmaPosY * sigmaVelZ * posYVelZCorr,
                  sigmaPosZ * sigmaVelZ * posZVelZCorr, sigmaVelX * sigmaVelZ * velXZCorr,
                  sigmaVelY * sigmaVelZ * velYZCorr, sigmaVelZ * sigmaVelZ } });
    }

    private void doTestInterpolation(final TimeInterpolator<SpacecraftState> stateInterpolator,
                                     final TimeInterpolator<TimeStampedPair<Orbit, StateCovariance>> covarianceInterpolator,
                                     final double propagationHorizon,
                                     final double tabulatedTimeStep,
                                     final double expectedMeanRMSPositionError,
                                     final double expectedMeanRMSVelocityError,
                                     final double expectedMedianRMSPositionError,
                                     final double expectedMedianRMSVelocityError,
                                     final double expectedMaxRMSPositionError,
                                     final double expectedMaxRMSVelocityError,
                                     final double tolerance,
                                     final boolean showResults) {
        final DescriptiveStatistics[] relativeRMSSigmaError =
                computeStatisticsCovarianceInterpolationOnSergeiCase(propagationHorizon, tabulatedTimeStep,
                                                                     stateInterpolator, covarianceInterpolator);

        // Then
        if (showResults) {
            System.out.format(Locale.US, "%35s = %20.16f%n", "relativeRMSSigmaError[0].getMean", relativeRMSSigmaError[0].getMean());
            System.out.format(Locale.US, "%35s = %20.16f%n", "relativeRMSSigmaError[1].getMean", relativeRMSSigmaError[1].getMean());
            System.out.format(Locale.US, "%35s = %20.16f%n", "relativeRMSSigmaError[0].getMedian", relativeRMSSigmaError[0].getPercentile(50));
            System.out.format(Locale.US, "%35s = %20.16f%n", "relativeRMSSigmaError[1].getMedian", relativeRMSSigmaError[1].getPercentile(50));
            System.out.format(Locale.US, "%35s = %20.16f%n", "relativeRMSSigmaError[0].getMax", relativeRMSSigmaError[0].getMax());
            System.out.format(Locale.US, "%35s = %20.16f%n", "relativeRMSSigmaError[1].getMax", relativeRMSSigmaError[1].getMax());
            
        }
        // Results obtained when using modified orbit date to use truncated JPL test resource file
        Assertions.assertEquals(expectedMeanRMSPositionError, relativeRMSSigmaError[0].getMean(), tolerance);
        Assertions.assertEquals(expectedMeanRMSVelocityError, relativeRMSSigmaError[1].getMean(), tolerance);
        Assertions.assertEquals(expectedMedianRMSPositionError, relativeRMSSigmaError[0].getPercentile(50), tolerance);
        Assertions.assertEquals(expectedMedianRMSVelocityError, relativeRMSSigmaError[1].getPercentile(50), tolerance);
        Assertions.assertEquals(expectedMaxRMSPositionError, relativeRMSSigmaError[0].getMax(), tolerance);
        Assertions.assertEquals(expectedMaxRMSVelocityError, relativeRMSSigmaError[1].getMax(), tolerance);
    }

    /**
     * Test based on the full force model test case from TANYGIN, Sergei. Efficient covariance interpolation using blending
     * of approximate covariance propagations. The Journal of the Astronautical Sciences, 2014, vol. 61, no 1, p. 107-132.
     * <p>
     * However, note that the exact result is not known and only an approximated value can be deduced from the available
     * graph in the aforementioned paper. Although the value obtained using a quintic Hermite interpolator is different from
     * values found in the paper, no errors were found in the algorithm. Furthermore, the results obtained in the paper are
     * not thoroughly explained so the exact cause is unknown.
     * <p>
     * This instance of the test is a non regression test aiming to test the results obtained when using both Keplerian time
     * derivatives. Hence, a quintic Keplerian interpolation.
     */
    @Test
    @DisplayName("test quintic Keplerian interpolation (two time derivatives used) on full force model test case from : "
            + "TANYGIN, Sergei. Efficient covariance interpolation using blending of approximate covariance propagations. "
            + "The Journal of the Astronautical Sciences, 2014, vol. 61, no 1, p. 107-132.")
    void testQuinticKeplerianInterpolation() {

        // Given
        final boolean showResults = false; // Show results?
        final double tolerance = 1.e-16;
        
        // Create state covariance interpolator
        final SmoothStepFactory.SmoothStepFunction blendingFunction = SmoothStepFactory.getQuadratic();

        final TimeInterpolator<Orbit> orbitInterpolator =
                new OrbitBlender(blendingFunction, new KeplerianPropagator(sergeiOrbit), sergeiFrame);

        final StateCovarianceKeplerianHermiteInterpolator covarianceInterpolator =
                new StateCovarianceKeplerianHermiteInterpolator(orbitInterpolator, sergeiFrame, OrbitType.CARTESIAN,
                                                                PositionAngleType.MEAN);

        // Create state interpolator
        final TimeInterpolator<SpacecraftState> stateInterpolator =
                new SpacecraftStateInterpolator(sergeiFrame, orbitInterpolator, null, null, null, null);

        // When & Then
        doTestInterpolation(stateInterpolator, covarianceInterpolator,
                            DEFAULT_SERGEI_PROPAGATION_TIME, DEFAULT_SERGEI_TABULATED_TIMESTEP,
                            0.0646887955936730,
                            0.1870011267826034,
                            0.0605252722806762,
                            0.2090092562980378,
                            0.1722559416492755,
                            0.3756010728001388,
                            tolerance,
                            showResults);

/*      Results obtained when using the reference sergei date
        Assertions.assertEquals(0.08333354122902344, relativeRMSSigmaError[0].getMean(), 1e-17);
        Assertions.assertEquals(0.18339504723198177, relativeRMSSigmaError[1].getMean(), 1e-17);
        Assertions.assertEquals(0.08379904791529535, relativeRMSSigmaError[0].getPercentile(50), 1e-17);
        Assertions.assertEquals(0.21301699586775608, relativeRMSSigmaError[1].getPercentile(50), 1e-17);
        Assertions.assertEquals(0.18097897860458778, relativeRMSSigmaError[0].getMax(), 1e-17);
        Assertions.assertEquals(0.25871013837895007, relativeRMSSigmaError[1].getMax(), 1e-17);
*/

        Assertions.assertEquals(CartesianDerivativesFilter.USE_PVA, covarianceInterpolator.getFilter());

    }

    /**
     * Test based on the full force model test case from TANYGIN, Sergei. Efficient covariance interpolation using blending
     * of approximate covariance propagations. The Journal of the Astronautical Sciences, 2014, vol. 61, no 1, p. 107-132.
     * <p>
     * This instance of the test is a non regression test aiming to test the results obtained when using the first Keplerian
     * time derivative only. Hence, a cubic Keplerian interpolation.
     */
    @Test
    @DisplayName("test cubic Keplerian interpolation (first time derivative used) on full force model test case from : "
            + "TANYGIN, Sergei. Efficient covariance interpolation using blending of approximate covariance propagations. "
            + "The Journal of the Astronautical Sciences, 2014, vol. 61, no 1, p. 107-132.")
    void testCubicKeplerianInterpolation() {

        // Given
        final boolean showResults = false; // Show results?
        final double tolerance = 1.e-16;
        
        // Create state covariance interpolator
        final SmoothStepFactory.SmoothStepFunction blendingFunction = SmoothStepFactory.getQuadratic();

        final TimeInterpolator<Orbit> orbitInterpolator = new OrbitBlender(blendingFunction,
                                                                           new KeplerianPropagator(sergeiOrbit),
                                                                           sergeiFrame);

        final StateCovarianceKeplerianHermiteInterpolator covarianceInterpolator =
                new StateCovarianceKeplerianHermiteInterpolator(DEFAULT_SERGEI_INTERPOLATION_POINTS, orbitInterpolator,
                                                                CartesianDerivativesFilter.USE_PV, sergeiFrame,
                                                                OrbitType.CARTESIAN,
                                                                PositionAngleType.MEAN);

        // Create state interpolator
        final TimeInterpolator<SpacecraftState> stateInterpolator =
                new SpacecraftStateInterpolator(sergeiFrame, orbitInterpolator, null, null, null, null);

        // When & then
        doTestInterpolation(stateInterpolator, covarianceInterpolator,
                            DEFAULT_SERGEI_PROPAGATION_TIME, DEFAULT_SERGEI_TABULATED_TIMESTEP,
                            0.0687107241065522,
                            0.1727658843435031,
                            0.0696685213697581,
                            0.1788064054703819,
                            0.1702870226981246,
                            0.3841670380381794,
                            tolerance,
                            showResults);

        // Results obtained when using Sergei reference date
/*        Assertions.assertEquals(0.07740033278409426, relativeRMSSigmaError[0].getMean(), 1e-17);
        Assertions.assertEquals(0.16752174969304912, relativeRMSSigmaError[1].getMean(), 1e-17);
        Assertions.assertEquals(0.08063527083126852, relativeRMSSigmaError[0].getPercentile(50), 1e-17);
        Assertions.assertEquals(0.1926905326066871 , relativeRMSSigmaError[1].getPercentile(50), 1e-17);
        Assertions.assertEquals(0.16289839792811542, relativeRMSSigmaError[0].getMax(), 1e-17);
        Assertions.assertEquals(0.23616924578204512, relativeRMSSigmaError[1].getMax(), 1e-17);*/

        Assertions.assertEquals(CartesianDerivativesFilter.USE_PV, covarianceInterpolator.getFilter());

    }

    /**
     * Test based on the full force model test case from TANYGIN, Sergei. Efficient covariance interpolation using blending
     * of approximate covariance propagations. The Journal of the Astronautical Sciences, 2014, vol. 61, no 1, p. 107-132.
     * <p>
     * This instance of the test is a non regression test aiming to test the results obtained when not using Keplerian time
     * derivatives. Hence, a linear Keplerian interpolation.
     */
    @Test
    @DisplayName("test linear Keplerian interpolation (no derivatives used) on full force model test case from : "
            + "TANYGIN, Sergei. Efficient covariance interpolation using blending of approximate covariance propagations. "
            + "The Journal of the Astronautical Sciences, 2014, vol. 61, no 1, p. 107-132.")
    void testLinearKeplerianInterpolation() {

        // Given
        final boolean showResults = false; // Show results?
        final double tolerance = 1.e-16;
        
        // Create state covariance interpolator
        final SmoothStepFactory.SmoothStepFunction blendingFunction = SmoothStepFactory.getQuadratic();

        final TimeInterpolator<Orbit> orbitInterpolator = new OrbitBlender(blendingFunction,
                                                                           new KeplerianPropagator(sergeiOrbit),
                                                                           sergeiFrame);

        final StateCovarianceKeplerianHermiteInterpolator covarianceInterpolator =
                new StateCovarianceKeplerianHermiteInterpolator(DEFAULT_SERGEI_INTERPOLATION_POINTS, orbitInterpolator,
                                                                CartesianDerivativesFilter.USE_P, sergeiFrame,
                                                                OrbitType.CARTESIAN,
                                                                PositionAngleType.MEAN);

        // Create state interpolator
        final TimeInterpolator<SpacecraftState> stateInterpolator =
                new SpacecraftStateInterpolator(sergeiFrame, orbitInterpolator, null, null, null, null);

        // When & Then
        doTestInterpolation(stateInterpolator, covarianceInterpolator,
                            DEFAULT_SERGEI_PROPAGATION_TIME, DEFAULT_SERGEI_TABULATED_TIMESTEP,
                            0.1967531616991254,
                            0.1744809570174334,
                            0.2201299654842542,
                            0.1501037774167836,
                            0.3115607775141900,
                            0.4912990230073768,
                            tolerance,
                            showResults);

        // Results obtained when using Sergei reference date
/*        Assertions.assertEquals(0.09148580146577297, relativeRMSSigmaError[0].getMean(), 1e-17);
        Assertions.assertEquals(0.11704748448308232, relativeRMSSigmaError[1].getMean(), 1e-17);
        Assertions.assertEquals(0.09727415341226611, relativeRMSSigmaError[0].getPercentile(50), 1e-17);
        Assertions.assertEquals(0.12457112100482712, relativeRMSSigmaError[1].getPercentile(50), 1e-17);
        Assertions.assertEquals(0.16611131341788263, relativeRMSSigmaError[0].getMax(), 1e-17);
        Assertions.assertEquals(0.1922012892962485, relativeRMSSigmaError[1].getMax(), 1e-17);*/

        Assertions.assertEquals(CartesianDerivativesFilter.USE_P, covarianceInterpolator.getFilter());

    }

    /**
     * Test based on the full force model test case from TANYGIN, Sergei. Efficient covariance interpolation using blending
     * of approximate covariance propagations. The Journal of the Astronautical Sciences, 2014, vol. 61, no 1, p. 107-132.
     * <p>
     * This instance of the test is a non regression test aiming to test the results obtained when interpolating in a local
     * orbital frame.
     * <p>
     * Bad results are seen regarding velocity interpolation. This has been checked to be independent of the method used to
     * interpolate the state covariance. Moreover, the same results are achieved if we interpolate in an inertial frame
     * (which has been seen to give very good results as in {@link #testQuinticKeplerianInterpolation()}),and then express it
     * in a non-inertial local orbital frame. It has also been verified that it was not (mainly) due to errors in orbit
     * interpolation as even using exact orbit for frame conversion would only slightly improve the results (<0.1%
     * improvements). Hence, the only explanation found is a sensitivity issue linked to non-inertial local orbital frame.
     */
    @Test
    @DisplayName("test quintic Keplerian interpolation (output in LOF) on full force model test case from : "
            + "TANYGIN, Sergei. Efficient covariance interpolation using blending of approximate covariance propagations. "
            + "The Journal of the Astronautical Sciences, 2014, vol. 61, no 1, p. 107-132.")
    void testLOFInterpolation() {

        // Given
        final boolean showResults = false; // Show results?
        final double tolerance = 1.e-16;
        
        // Default orbit case
        final Orbit orbit = generateSergeiReferenceOrbit();
        final Frame frame = orbit.getFrame();

        // Create state covariance interpolator
        final SmoothStepFactory.SmoothStepFunction blendingFunction = SmoothStepFactory.getQuadratic();

        final TimeInterpolator<Orbit> orbitInterpolator = new OrbitBlender(blendingFunction,
                                                                           new KeplerianPropagator(orbit),
                                                                           frame);

        final LOFType DEFAULT_LOFTYPE = LOFType.TNW;
        final TimeInterpolator<TimeStampedPair<Orbit, StateCovariance>> covarianceInterpolator =
                new StateCovarianceKeplerianHermiteInterpolator(orbitInterpolator, DEFAULT_LOFTYPE);

        // When
        final DescriptiveStatistics[] relativeRMSSigmaError =
                computeStatisticsCovarianceLOFInterpolation(DEFAULT_SERGEI_PROPAGATION_TIME,
                                                            DEFAULT_SERGEI_TABULATED_TIMESTEP,
                                                            DEFAULT_LOFTYPE, covarianceInterpolator);

        // Then
        if (showResults) {
            System.out.format(Locale.US, "%35s = %20.16f%n", "relativeRMSSigmaError[0].getMean", relativeRMSSigmaError[0].getMean());
            System.out.format(Locale.US, "%35s = %20.16f%n", "relativeRMSSigmaError[1].getMean", relativeRMSSigmaError[1].getMean());
            System.out.format(Locale.US, "%35s = %20.16f%n", "relativeRMSSigmaError[0].getMedian", relativeRMSSigmaError[0].getPercentile(50));
            System.out.format(Locale.US, "%35s = %20.16f%n", "relativeRMSSigmaError[1].getMedian", relativeRMSSigmaError[1].getPercentile(50));
            System.out.format(Locale.US, "%35s = %20.16f%n", "relativeRMSSigmaError[0].getMax", relativeRMSSigmaError[0].getMax());
            System.out.format(Locale.US, "%35s = %20.16f%n", "relativeRMSSigmaError[1].getMax", relativeRMSSigmaError[1].getMax());
            
        }
        Assertions.assertEquals( 0.0678893939532068, relativeRMSSigmaError[0].getMean(), tolerance);
        Assertions.assertEquals( 7.3610159507701730, relativeRMSSigmaError[1].getMean(), tolerance);
        Assertions.assertEquals( 0.0649252237750957, relativeRMSSigmaError[0].getPercentile(50), tolerance);
        Assertions.assertEquals( 7.7054187147650770, relativeRMSSigmaError[1].getPercentile(50), tolerance);
        Assertions.assertEquals( 0.1405955596105993, relativeRMSSigmaError[0].getMax(), tolerance);
        Assertions.assertEquals(16.0051089451628240, relativeRMSSigmaError[1].getMax(), tolerance);

    }
}

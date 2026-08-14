/* Copyright 2002-2026 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.propagation.analytical.tle.generation;

import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.frames.Frame;
import org.orekit.orbits.AbstractOrbitalStateFactory;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitParamsType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.FieldTLE;
import org.orekit.propagation.analytical.tle.FieldTLEPropagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEConstants;
import org.orekit.propagation.conversion.osc2mean.OsculatingToMeanConverter;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/**
 * Base class for generating a TLE.
 * @author Bryan Cazabonne
 * @since 12.0
 */
public abstract class TleGenerationAlgorithm extends AbstractOrbitalStateFactory<TLE> {

    /** Name for mean motion. */
    public static final String MEAN_MOTION = "TleMeanMotion";

    /** Name for eccentricity. */
    public static final String ECCENTRICITY   = "TleEccentricity";

    /** Name for inclination. */
    public static final String INCLINATION   = "TleInclination";

    /** Name for periapsis argument. */
    public static final String PERIAPSIS_ARGUMENT = "TlePeriapsisArgument";

    /** Name for right ascension of ascending node. */
    public static final String RAAN    = "TleRighAscensionAscendingNode";

    /** Name for mean anomaly. */
    public static final String MEAN_ANOM = "TleMeanAnomaly";

    /** Parameter name for B* coefficient. */
    public static final String B_STAR = "BSTAR";

    /** B* scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    public static final double B_STAR_SCALE = FastMath.scalb(1.0, -20);

    /** Number of orbital parameters, i.e. of both rows and columns of the Jacobians. */
    private static final int DEFAULT_STATE_DIMENSION = 6;

    /** Template TLE. */
    private final TLE templateTLE;

    /** Non-Keplerian drivers (containing only for ballistic coefficient parameter). */
    private ParameterDriversList nonKeplerianDrivers;

    /** Osculating to mean orbit converter. */
    private final OsculatingToMeanConverter converter;

    /** Default constructor.
     * @param templateTLE template TLE
     * @param teme teme frame
     * @param converter osculating to mean orbit converter
     */
    protected TleGenerationAlgorithm(final TLE templateTLE,  final Frame teme,
                                     final OsculatingToMeanConverter converter) {
        super(null, createOrbitalParametersDrivers(templateTLE), teme, PositionAngleType.MEAN,
              templateTLE.getDate(), TLEConstants.MU);
        this.templateTLE = templateTLE;

        // create model parameter drivers
        nonKeplerianDrivers = new ParameterDriversList();
        nonKeplerianDrivers.add(new ParameterDriver(B_STAR, templateTLE.getBStar(), B_STAR_SCALE,
                                                    Double.NEGATIVE_INFINITY,
                                                    Double.POSITIVE_INFINITY));

        // conversion algorithm
        this.converter = converter;

    }

    /** Get the template TLE.
     * @return template TLE
     */
    public TLE getTemplateTLE() {
        return templateTLE;
    }

    /** Get the osculating to mean orbit converter.
     * @return osculating to mean orbit converter
     * @since 14.0
     */
    public OsculatingToMeanConverter getConverter() {
        return converter;
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriversList getNonKeplerianParametersDrivers() {
        return nonKeplerianDrivers;
    }

    /** Create orbital parameter drivers.
     * @param tle reference TLE
     * @return drivers
     */
    private static ParameterDriversList createOrbitalParametersDrivers(final TLE tle) {
        final ParameterDriversList drivers = new ParameterDriversList();
        drivers.add(new ParameterDriver(MEAN_MOTION, tle.getMeanMotion(),
                                        FastMath.scalb(1.0, -32),
                                        0, Double.POSITIVE_INFINITY));
        drivers.add(new ParameterDriver(ECCENTRICITY, tle.getE(),
                                        FastMath.scalb(1.0, -22),
                                        0.0, 1.0));
        drivers.add(new ParameterDriver(INCLINATION, tle.getI(),
                                        FastMath.scalb(1.0, -22),
                                        0, FastMath.PI));
        drivers.add(new ParameterDriver(PERIAPSIS_ARGUMENT, tle.getPeriapsisArgument(),
                                        FastMath.scalb(1.0, -22),
                                        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        drivers.add(new ParameterDriver(RAAN, tle.getRaan(),
                                        FastMath.scalb(1.0, -22),
                                        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        drivers.add(new ParameterDriver(MEAN_ANOM, tle.getMeanAnomaly(),
                                        FastMath.scalb(1.0, -22),
                                        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        return drivers;
    }

    /** {@inheritDoc} */
    @Override
    protected double[] toArray(final Orbit orbit) {

        // fix both frame and type
        final Orbit mean               = converter.convertToMean(orbit);
        final Orbit partiallyConverted = orbit.getFrame() == getFrame() ? mean : mean.inFrame(getFrame());
        final Orbit fullyConverted     = OrbitParamsType.KEPLERIAN.convertType(partiallyConverted);

        // retrieve orbital parameters
        final double[] stateVector = new double[6];
        OrbitParamsType.KEPLERIAN.mapOrbitToArray(fullyConverted, PositionAngleType.MEAN, stateVector, null);

        // TLE uses mean motion as first parameter, not semi major axis as Keplerian orbit
        stateVector[0] = fullyConverted.getKeplerianMeanMotion();

        return stateVector;

    }

    /** {@inheritDoc} */
    @Override
    public TLE createFromDrivers() {

        final List<DelegatingDriver> drivers = getOrbitalParametersDrivers().getDrivers();

        // adjust revolution number
        // as neither SDP4 nor SGP4 use mean motion derivatives, we intentionally ignore them here
        final double latArg0 =
            MathUtils.normalizeAngle(drivers.get(3).getValue() + drivers.get(5).getValue(),
                                     FastMath.PI);
        final double deltaT   = getDate().durationFrom(templateTLE.getDate());
        final double latArg1  = latArg0 + deltaT * drivers.get(0).getValue();
        final int    deltaRev = (int) FastMath.floor(latArg1 / MathUtils.TWO_PI);

        return new TLE(templateTLE.getSatelliteNumber(), templateTLE.getClassification(),
                       templateTLE.getLaunchYear(), templateTLE.getLaunchNumber(), templateTLE.getLaunchPiece(),
                       templateTLE.getEphemerisType(),
                       templateTLE.getElementNumber() + 1,
                       getDate(),
                       drivers.get(0).getValue(),
                       templateTLE.getMeanMotionFirstDerivative(), templateTLE.getMeanMotionSecondDerivative(),
                       drivers.get(1).getValue(),
                       drivers.get(2).getValue(),
                       drivers.get(3).getValue(),
                       drivers.get(4).getValue(),
                       drivers.get(5).getValue(),
                       templateTLE.getRevolutionNumberAtEpoch() + deltaRev,
                       getBStar(),
                       templateTLE.getUtc());
    }

    /** {@inheritDoc}
     * <p>
     * The TLE orbital elements are related to the Cartesian coordinates by the SGP4/SDP4
     * model itself, which has no closed-form derivatives, so the Jacobian is obtained by
     * automatic differentiation: the six elements of the TLE built from the current drivers
     * are turned into {@link Gradient} variables, and the resulting TLE is evaluated at its
     * own epoch.
     * </p>
     */
    @Override
    public RealMatrix getJacobianWrtParameters() {
        return getJacobianWrtParameters(createFromDrivers());
    }

    /** Get the Jacobian of the Cartesian coordinates with respect to the orbital elements of a TLE.
     * <p>
     * The TLE orbital elements are related to the Cartesian coordinates by the SGP4/SDP4 model
     * itself, which has no closed-form derivatives, so the Jacobian is obtained by automatic
     * differentiation: the six orbital elements are turned into {@link Gradient} variables and
     * the TLE is evaluated at its own epoch. All the remaining data (identification, mean motion
     * derivatives, B*) are held constant.
     * </p>
     * @param tle TLE holding the orbital elements the Jacobian is computed with respect to
     * @return jacobian matrix dC/dB, at the TLE epoch and in the TEME frame
     */
    public RealMatrix getJacobianWrtParameters(final TLE tle) {

        // evaluate the Cartesian coordinates from a TLE whose orbital elements are variables
        final TimeStampedFieldPVCoordinates<Gradient> pv =
            FieldTLEPropagator.selectExtrapolator(toGradient(tle), getFrame()).
            getBaseInitialState().
            getPVCoordinates();

        // gather the derivatives of each Cartesian coordinate into a row
        final RealMatrix jacobian = MatrixUtils.createRealMatrix(DEFAULT_STATE_DIMENSION, DEFAULT_STATE_DIMENSION);
        jacobian.setRow(0, pv.getPosition().getX().getGradient());
        jacobian.setRow(1, pv.getPosition().getY().getGradient());
        jacobian.setRow(2, pv.getPosition().getZ().getGradient());
        jacobian.setRow(3, pv.getVelocity().getX().getGradient());
        jacobian.setRow(4, pv.getVelocity().getY().getGradient());
        jacobian.setRow(5, pv.getVelocity().getZ().getGradient());

        return jacobian;

    }

    /** Convert a TLE into one whose orbital elements are gradient variables.
     * @param tle TLE to convert
     * @return converted TLE, whose orbital elements carry their own derivatives
     */
    // FIXME: should this one be in TLE class instead ?
    private static FieldTLE<Gradient> toGradient(final TLE tle) {
        final GradientField field = GradientField.getField(DEFAULT_STATE_DIMENSION);
        return new FieldTLE<>(tle.getSatelliteNumber(), tle.getClassification(),
                              tle.getLaunchYear(), tle.getLaunchNumber(), tle.getLaunchPiece(),
                              tle.getEphemerisType(), tle.getElementNumber(),
                              new FieldAbsoluteDate<>(field, tle.getDate()),
                              Gradient.variable(DEFAULT_STATE_DIMENSION, 0, tle.getMeanMotion()),
                              Gradient.constant(DEFAULT_STATE_DIMENSION, tle.getMeanMotionFirstDerivative()),
                              Gradient.constant(DEFAULT_STATE_DIMENSION, tle.getMeanMotionSecondDerivative()),
                              Gradient.variable(DEFAULT_STATE_DIMENSION, 1, tle.getE()),
                              Gradient.variable(DEFAULT_STATE_DIMENSION, 2, tle.getI()),
                              Gradient.variable(DEFAULT_STATE_DIMENSION, 3, tle.getPeriapsisArgument()),
                              Gradient.variable(DEFAULT_STATE_DIMENSION, 4, tle.getRaan()),
                              Gradient.variable(DEFAULT_STATE_DIMENSION, 5, tle.getMeanAnomaly()),
                              tle.getRevolutionNumberAtEpoch(),
                              Gradient.constant(DEFAULT_STATE_DIMENSION, tle.getBStar()),
                              tle.getUtc());
    }

    /** Get the current B-star value.
     * @return current B-star value
     */
    protected double getBStar() {
        return nonKeplerianDrivers.getDrivers().getFirst().getValue();
    }

    /**
     * Generate a TLE from a given spacecraft state and a template TLE.
     * <p>
     * The template TLE is only used to get identifiers like satellite
     * number, launch year, etc.
     * In other words, the keplerian elements contained in the generated
     * TLE are based on the provided state and not the template TLE.
     * </p>
     * @param state spacecraft state
     * @param newTemplateTLE template TLE
     * @return a TLE corresponding to the given state
     */
    public TLE generate(final SpacecraftState state, final TLE newTemplateTLE) {
        final KeplerianOrbit mean =
            (KeplerianOrbit) OrbitParamsType.KEPLERIAN.convertType(converter.convertToMean(state.getOrbit()));
        return TleGenerationUtil.newTLE(mean, newTemplateTLE);
    }

    /**
     * Generate a TLE from a given spacecraft state and a template TLE.
     * <p>
     * The template TLE is only used to get identifiers like satellite
     * number, launch year, etc.
     * In other words, the keplerian elements contained in the generated
     * TLE are based on the provided state and not the template TLE.
     * </p>
     * @param <T> type of the elements
     * @param state spacecraft state
     * @param newTemplateTLE template TLE
     * @return a TLE corresponding to the given state
     */
    public <T extends CalculusFieldElement<T>> FieldTLE<T> generate(final FieldSpacecraftState<T> state,
                                                                    final FieldTLE<T> newTemplateTLE) {
        final FieldKeplerianOrbit<T> mean =
            (FieldKeplerianOrbit<T>) OrbitParamsType.KEPLERIAN.convertType(converter.convertToMean(state.getOrbit()));
        return TleGenerationUtil.newTLE(mean, newTemplateTLE);
    }

    /** {@inheritDoc} */
    @Override
    public TleGenerationAlgorithm clone() {

        final TleGenerationAlgorithm clone = (TleGenerationAlgorithm) super.clone();

        // de-couple b-star driver
        final ParameterDriversList newDrivers = new ParameterDriversList();
        final ParameterDriver driver = nonKeplerianDrivers.getDrivers().getFirst();
        newDrivers.add(new ParameterDriver(driver.getName(), driver.getValue(), driver.getScale(),
                                           driver.getMinValue(), driver.getMaxValue()));
        clone.nonKeplerianDrivers = newDrivers;

        return clone;

    }

}

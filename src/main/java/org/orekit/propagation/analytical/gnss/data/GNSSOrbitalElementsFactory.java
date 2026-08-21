/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.propagation.analytical.gnss.data;

import java.util.List;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.frames.Frame;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.orbits.AbstractOrbitalStateFactory;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldKeplerianParameters;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.KeplerianParameters;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitParamsType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.gnss.FieldGnssPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeInterval;
import org.orekit.time.TimeScales;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterObserver;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** Factory for {@link AbstractNavigationMessage}.
 * @param <O> type of the orbital elements
 * @since 14.0
*/
public abstract class GNSSOrbitalElementsFactory<O extends GNSSOrbitalElements<O>>
    extends AbstractOrbitalStateFactory<O> {

    /** Name for semi major axis parameter. */
    public static final String SEMI_MAJOR_AXIS = "GnssSemiMajorAxis";

    /** Name for eccentricity parameter. */
    public static final String ECCENTRICITY = "GnssEccentricity";

    /** Name for inclination at reference time parameter. */
    public static final String INCLINATION = "GnssInclination";

    /** Name for argument of periapsis parameter. */
    public static final String ARGUMENT_OF_PERIAPSIS = "GnssPeriapsisArgument";

    /** Name for longitude of ascending node at weekly epoch parameter. */
    public static final String NODE_LONGITUDE = "GnssNodeLongitude";

    /** Name for mean anomaly at reference time parameter. */
    public static final String MEAN_ANOMALY = "GnssMeanAnomaly";

    /** Prefix for frozen frame name. */
    public static final String FROZEN = "frozen-";

    /** Number of orbital parameters, i.e. of both rows and columns of the Jacobians. */
    private static final int DEFAULT_STATE_DIMENSION = 6;

    /** Mean angular velocity of the Earth for the GNSS model. */
    private final double angularVelocity;

    /** Known time scales. */
    private final TimeScales timeScales;

    /** Satellite system to use for interpreting week number. */
    private final SatelliteSystem system;

    /** Message type (null if not a navigation message). */
    private final String type;

    /** Reference inertial frame. */
    private final Frame inertial;

    /** Body-fixed frame. */
    private final Frame bodyFixed;

    /** Time of ephemeris. */
    private GNSSDate toe;

    /** PRN number of the satellite. */
    private int prn;

    /** Factor for non-Keplerian drivers. */
    private final NonKeplerianDriversFactory driversFactory;

    /** Non-Keplerian drivers. */
    private final ParameterDriversList nonKeplerianParametersDrivers;

    /** Time of clock. */
    private GNSSDate toc;

    /** Group delay differential TGD for L1-L2 correction. */
    private double tgd;

    /**
     * Simple constructor.
     *
     * @param angularVelocity mean angular velocity of the Earth for the GNSS model
     * @param timeScales      known time scales
     * @param system          satellite system to use for interpreting week number
     * @param type            message type (null if not a navigation message)
     * @param inertial        reference inertial frame
     * @param bodyFixed       body fixed frame (will be frozen at {@code date} to build the orbital elements
     * @param mu              central attraction coefficient (m³/s²)
     */
    protected GNSSOrbitalElementsFactory(final double angularVelocity, final TimeScales timeScales,
                                         final SatelliteSystem system, final String type,
                                         final Frame inertial, final Frame bodyFixed, final double mu) {
        super(OrbitParamsType.KEPLERIAN, buildOrbitalDrivers(), null, PositionAngleType.MEAN, null, mu);

        // immutable fields
        this.angularVelocity = angularVelocity;
        this.timeScales      = timeScales;
        this.system          = system;
        this.type            = type;
        this.inertial        = inertial;
        this.bodyFixed       = bodyFixed;

        // non-Keplerian drivers
        this.driversFactory = new NonKeplerianDriversFactory();

        this.nonKeplerianParametersDrivers = new ParameterDriversList();
        this.driversFactory.getParametersDrivers().forEach(nonKeplerianParametersDrivers::add);

        // automatically update date and frozen frame when time driver is updated
        getTimeDriver().addObserver(new ParameterObserver() {

            /** {@inheritDoc} */
            @Override
            public void valueChanged(final double previousValue, final ParameterDriver driver,
                                     final AbsoluteDate date) {
                // the check for null toe allows to set up a dummy week number
                // in case secondsInWeek is set *before* week is set
                // (this can happen when parsing YUMA almanac for example)
                final int weekNumber = toe == null ? 0 : toe.getWeekNumber();
                setTimeOfEphemerisNoRecurse(weekNumber, driver.getValue());
            }

            /** {@inheritDoc} */
            @Override
            public void valueSpanMapChanged(final TimeSpanMap<Double> previousValueSpanMap,
                                            final ParameterDriver driver) {
                // nothing to do
            }
        });

    }

    /** Get the reference inertial frame.
     * @return reference inertial frame
     */
    public Frame getInertial() {
        return inertial;
    }

    /** Get the body fixed frame.
     * @return body fixed frame
     */
    public Frame getBodyFixed() {
        return bodyFixed;
    }

    /** Get the mean angular velocity of the Earth for the GNSS model.
     * @return mean angular velocity of the Earth for the GNSS model
     */
    public double getAngularVelocity() {
        return angularVelocity;
    }

    /** Get known time scales.
     * @return known time scales
     */
    public TimeScales getTimeScales() {
        return timeScales;
    }

    /** Get the satellite system to use for interpreting week number.
     * @return satellite system to use for interpreting week number
     */
    public SatelliteSystem getSystem() {
        return system;
    }

    /** Get the message type.
     * @return Get the message type (null if not a navigation message)
     */
    public String getType() {
        return type;
    }

    /** Get the PRN number of the satellite.
     * @return PRN number of the satellite
     */
    public int getPrn() {
        return prn;
    }

    /** Set the PRN number of the satellite.
     * @param prn PRN number of the satellite
     */
    public void setPrn(final int prn) {
        this.prn = prn;
    }

    /** {@inheritDoc} */
    @Override
    public void setDate(final AbsoluteDate date) {
        setTimeOfEphemeris(new GNSSDate(date, system, timeScales));
    }

    /** Set the time of ephemeris.
     * <p>
     * If time of clock was not already set, it will be set to the same value as
     * time of ephemeris as a side effect
     * </p>
     * @param timeOfEphemeris time of ephemeris
     */
    public void setTimeOfEphemeris(final GNSSDate timeOfEphemeris) {
        this.toe = timeOfEphemeris;
        getTimeDriver().setValue(timeOfEphemeris.getSecondsInWeek());
    }

    /** Set the time of ephemeris.
     * <p>
     * If time of clock was not already set, it will be set to the same value as
     * time of ephemeris as a side effect
     * </p>
     * @param weekNumber week number
     * @param secondsInWeek seconds in week
     */
    private void setTimeOfEphemerisNoRecurse(final int weekNumber, final double secondsInWeek) {
        toe = new GNSSDate(weekNumber, secondsInWeek, system, timeScales);
        super.setDate(toe.getDate());
        setFrame(bodyFixed.getFrozenFrame(inertial, toe.getDate(), FROZEN + bodyFixed.getName()));
        if (toc == null) {
            // set time of clock too
            toc = toe;
        }
    }

    /** Get the time of ephemeris.
     * @return time of ephemeris
     */
    public GNSSDate getTimeOfEphemeris() {
        return toe;
    }

    /** Get driver for reference time of the GNSS orbit as a duration from week start.
     * @return driver for reference time of the GNSS orbit (s)
     * @see #getTimeOfEphemeris()
     */
    public ParameterDriver getTimeDriver() {
        return driversFactory.getTimeDriver();
    }

    /** Get driver for change rate in semi-major axis.
     * @return driver for the change rate in semi-major axis
     */
    public ParameterDriver getADotDriver() {
        return driversFactory.getADotDriver();
    }

    /** Get driver for the delta of satellite mean motion.
     * @return driver for the delta of satellite mean motion
     */
    public ParameterDriver getDeltaN0Driver() {
        return driversFactory.getDeltaN0Driver();
    }

    /** Get driver for the change rate in Δn₀.
     * @return driver for change rate in Δn₀
     */
    public ParameterDriver getDeltaN0DotDriver() {
        return driversFactory.getDeltaN0DotDriver();
    }

    /** Get driver for rate of inclination angle.
     * @return driver for rate of inclination angle (rad/s)
     */
    public ParameterDriver getIDotDriver() {
        return driversFactory.getIDotDriver();
    }

    /** Get driver for rate of right ascension.
     * @return driver for rate of right ascension (rad/s)
     */
    public ParameterDriver getOmegaDotDriver() {
        return driversFactory.getOmegaDotDriver();
    }

    /** Get driver for amplitude of the cosine harmonic correction term to the argument of latitude.
     * @return driver for amplitude of the cosine harmonic correction term to the argument of latitude (rad)
     */
    public ParameterDriver getCucDriver() {
        return driversFactory.getCucDriver();
    }

    /** Get driver for amplitude of the sine harmonic correction term to the argument of latitude.
     * @return driver for amplitude of the sine harmonic correction term to the argument of latitude (rad)
     */
    public ParameterDriver getCusDriver() {
        return driversFactory.getCusDriver();
    }

    /** Get driver for amplitude of the cosine harmonic correction term to the orbit radius.
     * @return driver for amplitude of the cosine harmonic correction term to the orbit radius (m)
     */
    public ParameterDriver getCrcDriver() {
        return driversFactory.getCrcDriver();
    }

    /** Get driver for amplitude of the sine harmonic correction term to the orbit radius.
     * @return driver for amplitude of the sine harmonic correction term to the orbit radius (m)
     */
    public ParameterDriver getCrsDriver() {
        return driversFactory.getCrsDriver();
    }

    /** Get driver for amplitude of the cosine harmonic correction term to the angle of inclination.
     * @return driver for amplitude of the cosine harmonic correction term to the angle of inclination (rad)
     */
    public ParameterDriver getCicDriver() {
        return driversFactory.getCicDriver();
    }

    /** Get driver for amplitude of the sine harmonic correction term to the angle of inclination.
     * @return driver for amplitude of the sine harmonic correction term to the angle of inclination (rad)
     */
    public ParameterDriver getCisDriver() {
        return driversFactory.getCisDriver();
    }

    /** Get driver for SV zero-th order clock correction.
     * @return driver for SV zero-th order clock correction (s)
     */
    public ParameterDriver getAf0Driver() {
        return driversFactory.getAf0Driver();
    }

    /** Get driver for SV first order clock correction.
     * @return driver for SV first order clock correction (s/s)
     */
    public ParameterDriver getAf1Driver() {
        return driversFactory.getAf1Driver();
    }

    /** Get driver for SV second order clock correction.
     * @return driver for SV second order clock correction (s/s²)
     */
    public ParameterDriver getAf2Driver() {
        return driversFactory.getAf2Driver();
    }

    /** Set the time of clock.
     * <p>
     * If time of ephemeris was not already set, it will be set to the same value as
     * time of clock as a side effect
     * </p>
     * @param timeOfClock time of clock
     */
    public void setTimeOfClock(final GNSSDate timeOfClock) {
        this.toc = timeOfClock;
        if (toe == null) {
            // set time of ephemeris too
            setTimeOfEphemeris(timeOfClock);
        }
    }

    /** Get the time of clock.
     * @return time of clock
     */
    public GNSSDate getTimeOfClock() {
        return toc;
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriversList getNonKeplerianParametersDrivers() {
        return nonKeplerianParametersDrivers;
    }

    /** Get the estimated group delay differential TGD for L1-L2 correction.
     * @return the estimated group delay differential TGD for L1-L2 correction (s)
     */
    public double getTgd() {
        return tgd;
    }

    /** Set the estimated group delay differential TGD for L1-L2 correction.
     * @param tgd estimated group delay differential TGD for L1-L2 correction (s)
     */
    public void setTgd(final double tgd) {
        this.tgd = tgd;
    }

    /** {@inheritDoc} */
    @Override
    protected double[] toArray(final Orbit orbit) {

        // fix both frame and type
        final Orbit partiallyConverted = orbit.getFrame() == getFrame() ? orbit : orbit.inFrame(getFrame());
        final Orbit fullyConverted     = OrbitParamsType.KEPLERIAN.convertType(partiallyConverted);

        // retrieve orbital parameters
        final double[] stateVector = new double[6];
        OrbitParamsType.KEPLERIAN.mapOrbitToArray(fullyConverted, PositionAngleType.MEAN, stateVector, null);

        return stateVector;

    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getJacobianWrtParameters() {
        return jacobianWrtParameters(createFromDrivers(), getInertial(), getBodyFixed());
    }

    /** Get the Jacobian of the Cartesian coordinates with respect to the orbital elements.
     * <p>
     * The GNSS orbital elements are related to the Cartesian coordinates by the GNSS
     * propagation model itself, which has no closed-form derivatives, so the Jacobian is
     * obtained by automatic differentiation: the six Keplerian-like orbital elements are
     * turned into {@link Gradient} variables and the elements are evaluated at their own
     * epoch. The non-Keplerian elements are held constant.
     *
     * @param <O> type of the GNSS orbital elements
     * @param elements GNSS orbital elements the Jacobian is computed with respect to
     * @param eci Earth Centered Inertial frame the Cartesian coordinates are expressed in
     * @param ecef Earth Centered Earth Fixed frame the node longitude refers to
     * @return jacobian matrix dC/dB, at the elements epoch
     */
    public static <O extends GNSSOrbitalElements<O>> RealMatrix jacobianWrtParameters(final O elements,
                                                                                      final Frame eci,
                                                                                      final Frame ecef) {

        // set up the six orbital elements as the free variables of the gradients
        final GradientField field = GradientField.getField(DEFAULT_STATE_DIMENSION);
        final KeplerianOrbit orbit = (KeplerianOrbit) OrbitParamsType.KEPLERIAN.convertType(elements.getOrbit());
        final FieldKeplerianOrbit<Gradient> gOrbit =
            new FieldKeplerianOrbit<>(new FieldKeplerianParameters<>(Gradient.variable(DEFAULT_STATE_DIMENSION, 0, orbit.getA()),
                                                                     Gradient.variable(DEFAULT_STATE_DIMENSION, 1, orbit.getE()),
                                                                     Gradient.variable(DEFAULT_STATE_DIMENSION, 2, orbit.getI()),
                                                                     Gradient.variable(DEFAULT_STATE_DIMENSION, 3, orbit.getPeriapsisArgument()),
                                                                     Gradient.variable(DEFAULT_STATE_DIMENSION, 4, orbit.getRightAscensionOfAscendingNode()),
                                                                     Gradient.variable(DEFAULT_STATE_DIMENSION, 5, orbit.getMeanAnomaly()),
                                                                     PositionAngleType.MEAN),
                                      orbit.getFrame(),
                                      new FieldAbsoluteDate<>(field, orbit.getDate()),
                                      Gradient.constant(DEFAULT_STATE_DIMENSION, orbit.getMu()));

        // keep the non-Keplerian elements constant
        final NonKeplerianDriversFactory nonKeplerianFactory = new NonKeplerianDriversFactory();
        nonKeplerianFactory.reset(elements);
        final FieldGnssOrbitalElements<Gradient, O> gElements =
            elements.toField(gOrbit, nonKeplerianFactory.toGradients(DEFAULT_STATE_DIMENSION),
                             d -> Gradient.constant(DEFAULT_STATE_DIMENSION, d));

        // evaluate the Cartesian coordinates at the elements epoch
        // FIXME: here a GNSS propagator is instantiated, not sure this is the best way to do it
        final TimeStampedFieldPVCoordinates<Gradient> pv =
            new FieldGnssPropagator<>(gElements, eci, ecef, FrameAlignedProvider.of(eci),
                                      Gradient.constant(DEFAULT_STATE_DIMENSION, Propagator.DEFAULT_MASS)).
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

    /** Create orbit from Keplerian elements drivers.
     * @return created orbit
     */
    protected KeplerianOrbit createOrbitFromDrivers() {
        final List<? extends ParameterDriver> orbitalDrivers = getOrbitalParametersDrivers().getDrivers();
        return new KeplerianOrbit(new KeplerianParameters(orbitalDrivers.get(0).getValue(),
                                                          orbitalDrivers.get(1).getValue(),
                                                          orbitalDrivers.get(2).getValue(),
                                                          orbitalDrivers.get(3).getValue(),
                                                          orbitalDrivers.get(4).getValue(),
                                                          orbitalDrivers.get(5).getValue(),
                                                          PositionAngleType.MEAN),
                                  getFrame(), getDate(), getMu());
    }

    /** Build drivers for orbital elements.
     * @return drivers for orbital elements only
     */
    public static ParameterDriversList buildOrbitalDrivers() {
        // the reference parameters are almost arbitrary and roughly represent a MEO satellite
        final ParameterDriversList drivers = new ParameterDriversList();
        drivers.add(new ParameterDriver(SEMI_MAJOR_AXIS, 26000000.0,
                                        FastMath.scalb(1.0, 0),
                                        Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Double.POSITIVE_INFINITY,
                                        TimeInterval.UNLIMITED));
        drivers.add(new ParameterDriver(ECCENTRICITY, 1.0e-8,
                                        FastMath.scalb(1.0, -24),
                                        0.0, 1.0, TimeInterval.UNLIMITED));
        drivers.add(new ParameterDriver(INCLINATION, FastMath.toRadians(56.0),
                                        FastMath.scalb(1.0, -24),
                                        0.0, FastMath.PI, TimeInterval.UNLIMITED));
        drivers.add(new ParameterDriver(ARGUMENT_OF_PERIAPSIS, 0.0,
                                        FastMath.scalb(1.0, -24),
                                        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, TimeInterval.UNLIMITED));
        drivers.add(new ParameterDriver(NODE_LONGITUDE, 0.0,
                                        FastMath.scalb(1.0, -24),
                                        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, TimeInterval.UNLIMITED));
        drivers.add(new ParameterDriver(MEAN_ANOMALY, 0.0,
                                        FastMath.scalb(1.0, -24),
                                        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, TimeInterval.UNLIMITED));
        return drivers;
    }

}

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

import org.hipparchus.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.orbits.AbstractOrbitalParameterFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.KeplerianParameters;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScales;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterObserver;
import org.orekit.utils.TimeSpanMap;

import java.util.List;

/** Factory for {@link AbstractNavigationMessage}.
 * @param <O> type of the orbital elements
 * @since 14.0
*/
public abstract class GNSSOrbitalElementsFactory<O extends GNSSOrbitalElements<O>>
    extends AbstractOrbitalParameterFactory<O> {

    /** Name for semi major axis parameter. */
    public static final String SEMI_MAJOR_AXIS = "GnssSemiMajorAxis";

    /** Name for eccentricity parameter. */
    public static final String ECCENTRICITY = "GnssEccentricity";

    /** Name for inclination at reference time parameter. */
    public static final String INCLINATION = "GnssInclination";

    /** Name for argument of perigee parameter. */
    public static final String ARGUMENT_OF_PERIGEE = "GnssPerigeeArgument";

    /** Name for longitude of ascending node at weekly epoch parameter. */
    public static final String NODE_LONGITUDE = "GnssNodeLongitude";

    /** Name for mean anomaly at reference time parameter. */
    public static final String MEAN_ANOMALY = "GnssMeanAnomaly";

    /** Prefix for frzen frame name. */
    private static final String FROZEN = "frozen-";

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

    /** Reference Week of the orbit. */
    private int week;

    /** PRN number of the satellite. */
    private int prn;

    /** Factor for non-Keplerian drivers. */
    private final NonKeplerianDriversFactory driversFactory;

    /** Non-Keplerian drivers. */
    private final ParameterDriversList nonKeplerianParametersDrivers;

    /** Group delay differential TGD for L1-L2 correction. */
    private double tgd;

    /** Time Of Clock. */
    private double toc;

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
        super(buildOrbitalDrivers(), null, PositionAngleType.MEAN, null, mu);

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
                setDate(new GNSSDate(week, driver.getValue(), system, timeScales).getDate());
            }

            /** {@inheritDoc} */
            @Override
            public void valueSpanMapChanged(final TimeSpanMap<Double> previousValueSpanMap,
                                            final ParameterDriver driver) {
                // nothing to do
            }
        });

    }

    /** {@inheritDoc} */
    @Override
    public void setDate(final AbsoluteDate date) {
        super.setDate(date);
        setFrame(bodyFixed.getFrozenFrame(inertial, date, FROZEN + bodyFixed.getName()));
    }

    /** Create parameter driver.
     * @param name name of the driver
     * @param scalePower power of two of the scale parameter
     * @return build driver
     */
    protected static ParameterDriver createDriver(final String name, final int scalePower) {
        return new ParameterDriver(name, 0, FastMath.scalb(1.0, scalePower),
                                   Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
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

    /** Get the reference Week of the orbit.
     * @return reference Week of the orbit
     */
    public int getWeek() {
        return week;
    }

    /** Set the reference Week of the orbit.
     * @param week reference Week of the orbit
     * @param time time within the week
     */
    public void setWeekAndTime(final int week, final double time) {
        this.week = week;
        getTimeDriver().setValue(time);
    }

    /** Get driver for reference time of the GNSS orbit as a duration from week start.
     * @return driver for reference time of the GNSS orbit (s)
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

    /** {@inheritDoc} */
    @Override
    public ParameterDriversList getNonKeplerianParametersDrivers() {
        return nonKeplerianParametersDrivers;
    }

    /** Get the estimated group delay differential TGD for L1-L2 correction.
     * @return the estimated group delay differential TGD for L1-L2 correction (s)
     */
    public double getTGD() {
        return tgd;
    }

    /** Set the estimated group delay differential TGD for L1-L2 correction.
     * @param tgd estimated group delay differential TGD for L1-L2 correction (s)
     */
    public void setTGD(final double tgd) {
        this.tgd = tgd;
    }

    /** Get the time of clock.
     * @return the time of clock (s)
     */
    public double getToc() {
        return toc;
    }

    /** Set the time of clock.
     * @param toc time of clock (s)
     */
    public void setToc(final double toc) {
        this.toc = toc;
    }

    /** {@inheritDoc} */
    @Override
    protected double[] toArray(final Orbit orbit) {

        // fix both frame and type
        final Orbit partiallyConverted = orbit.getFrame() == getFrame() ? orbit : orbit.inFrame(getFrame());
        final Orbit fullyConverted     = OrbitType.KEPLERIAN.convertType(partiallyConverted);

        // retrieve orbital parameters
        final double[] stateVector = new double[6];
        OrbitType.KEPLERIAN.mapOrbitToArray(fullyConverted, PositionAngleType.MEAN, stateVector, null);

        return stateVector;

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
                                        Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Double.POSITIVE_INFINITY));
        drivers.add(new ParameterDriver(ECCENTRICITY, 1.0e-8,
                                        FastMath.scalb(1.0, -24),
                                        0.0, 1.0));
        drivers.add(new ParameterDriver(INCLINATION, FastMath.toRadians(56.0),
                                        FastMath.scalb(1.0, -24),
                                        0.0, FastMath.PI));
        drivers.add(new ParameterDriver(ARGUMENT_OF_PERIGEE, 0.0,
                                        FastMath.scalb(1.0, -24),
                                        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        drivers.add(new ParameterDriver(NODE_LONGITUDE, 0.0,
                                        FastMath.scalb(1.0, -24),
                                        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        drivers.add(new ParameterDriver(MEAN_ANOMALY, 0.0,
                                        FastMath.scalb(1.0, -24),
                                        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        return drivers;
    }

}

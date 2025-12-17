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

    /** Name for time parameter. */
    public static final String TIME = "GnssTime";

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

    /** Name for change rate in semi-major axis parameter. */
    public static final String A_DOT = "GnssADot";

    /** Name for delta of satellite mean motion. */
    public static final String DELTA_N0 = "GnssDeltaN0";

    /** Name for change rate in Δn₀. */
    public static final String DELTA_N0_DOT = "GnssDeltaN0Dot";

    /** Name for inclination rate parameter. */
    public static final String INCLINATION_RATE = "GnssInclinationRate";

    /** Name for longitude rate parameter. */
    public static final String LONGITUDE_RATE = "GnssLongitudeRate";

    /** Name for cosine of latitude argument harmonic parameter. */
    public static final String LATITUDE_COSINE = "GnssLatitudeCosine";

    /** Name for sine of latitude argument harmonic parameter. */
    public static final String LATITUDE_SINE = "GnssLatitudeSine";

    /** Name for cosine of orbit radius harmonic parameter. */
    public static final String RADIUS_COSINE = "GnssRadiusCosine";

    /** Name for sine of orbit radius harmonic parameter. */
    public static final String RADIUS_SINE = "GnssRadiusSine";

    /** Name for cosine of inclination harmonic parameter. */
    public static final String INCLINATION_COSINE = "GnssInclinationCosine";

    /** Name for sine of inclination harmonic parameter. */
    public static final String INCLINATION_SINE = "GnssInclinationSine";

    /** Name for zero-th order clock correction parameter. */
    public static final String AF0 = "GnssClock0";

    /** Name for first order clock correction parameter. */
    public static final String AF1 = "GnssClock1";

    /** Name for second order clock correction parameter. */
    public static final String AF2 = "GnssClock2";

    /** Prefix for frzen frame name. */
    private static final String FROZEN = "frozen-";

    /** Mean angular velocity of the Earth for the GNSS model. */
    private final double angularVelocity;

    /** Duration of the GNSS cycle in weeks. */
    private final int weeksInCycle;

    /** Known time scales. */
    private final TimeScales timeScales;

    /** Satellite system to use for interpreting week number. */
    private final SatelliteSystem system;

    /** Message type (null if not a navigation message). */
    private final String type;

    /** PRN number of the satellite. */
    private int prn;

    /** Reference Week of the orbit. */
    private int week;

    /** Reference time. */
    private final ParameterDriver timeDriver;

    /** Change rate in semi-major axis (m/s). */
    private final ParameterDriver aDotDriver;

    /** Delta of satellite mean motion. */
    private final ParameterDriver deltaN0Driver;

    /** Change rate in Δn₀. */
    private final ParameterDriver deltaN0DotDriver;

    /** Inclination rate (rad/s). */
    private final ParameterDriver iDotDriver;

    /** Rate of right ascension (rad/s). */
    private final ParameterDriver domDriver;

    /** Amplitude of the cosine harmonic correction term to the argument of latitude. */
    private final ParameterDriver cucDriver;

    /** Amplitude of the sine harmonic correction term to the argument of latitude. */
    private final ParameterDriver cusDriver;

    /** Amplitude of the cosine harmonic correction term to the orbit radius. */
    private final ParameterDriver crcDriver;

    /** Amplitude of the sine harmonic correction term to the orbit radius. */
    private final ParameterDriver crsDriver;

    /** Amplitude of the cosine harmonic correction term to the inclination. */
    private final ParameterDriver cicDriver;

    /** Amplitude of the sine harmonic correction term to the inclination. */
    private final ParameterDriver cisDriver;

    /** SV zero-th order clock correction (s). */
    private final ParameterDriver af0Driver;

    /** SV first order clock correction (s/s). */
    private final ParameterDriver af1Driver;

    /** SV second order clock correction (s/s²). */
    private final ParameterDriver af2Driver;

    /** Non-Keplerian drivers. */
    private final ParameterDriversList nonKeplerianParametersDrivers;

    /** Group delay differential TGD for L1-L2 correction. */
    private double tgd;

    /** Time Of Clock. */
    private double toc;

    /** Simple constructor.
     * @param angularVelocity mean angular velocity of the Earth for the GNSS model
     * @param weeksInCycle    duration of the GNSS cycle in weeks
     * @param timeScales      known time scales
     * @param system          satellite system to use for interpreting week number
     * @param type            message type (null if not a navigation message)
     * @param inertial        reference inertial frame
     * @param bodyFixed       body fixed frame (will be frozen at {@code date} to build the orbital elements
     * @param date            date of the orbital parameters
     * @param mu              central attraction coefficient (m³/s²)
     */
    protected GNSSOrbitalElementsFactory(final double angularVelocity, final int weeksInCycle,
                                         final TimeScales timeScales, final SatelliteSystem system,
                                         final String type, final Frame inertial, final Frame bodyFixed,
                                         final AbsoluteDate date, final double mu) {
        super(buildOrbitalDrivers(),
              bodyFixed.getFrozenFrame(inertial, date, FROZEN + bodyFixed.getName()),
              PositionAngleType.MEAN, date, mu);

        // immutable fields
        this.angularVelocity = angularVelocity;
        this.weeksInCycle    = weeksInCycle;
        this.timeScales      = timeScales;
        this.system          = system;
        this.type            = type;

        // non-Keplerian drivers
        this.timeDriver       = createDriver(TIME,                -10);
        this.aDotDriver       = createDriver(A_DOT,               -10);
        this.deltaN0Driver    = createDriver(DELTA_N0,            -36);
        this.deltaN0DotDriver = createDriver(DELTA_N0_DOT,        -46);
        this.iDotDriver       = createDriver(INCLINATION_RATE,    -34);
        this.domDriver        = createDriver(LONGITUDE_RATE,      -34);
        this.cucDriver        = createDriver(LATITUDE_COSINE,     -24);
        this.cusDriver        = createDriver(LATITUDE_SINE,       -24);
        this.crcDriver        = createDriver(RADIUS_COSINE,         0);
        this.crsDriver        = createDriver(RADIUS_SINE,           0);
        this.cicDriver        = createDriver(INCLINATION_COSINE,  -24);
        this.cisDriver        = createDriver(INCLINATION_SINE,    -24);

        // clock drivers
        this.af0Driver = createDriver(AF0, -26);
        this.af1Driver = createDriver(AF1, -42);
        this.af2Driver = createDriver(AF2, -58);

        this.nonKeplerianParametersDrivers = new ParameterDriversList();
        nonKeplerianParametersDrivers.add(timeDriver);
        nonKeplerianParametersDrivers.add(aDotDriver);
        nonKeplerianParametersDrivers.add(deltaN0Driver);
        nonKeplerianParametersDrivers.add(deltaN0DotDriver);
        nonKeplerianParametersDrivers.add(iDotDriver);
        nonKeplerianParametersDrivers.add(domDriver);
        nonKeplerianParametersDrivers.add(cucDriver);
        nonKeplerianParametersDrivers.add(cusDriver);
        nonKeplerianParametersDrivers.add(crcDriver);
        nonKeplerianParametersDrivers.add(crsDriver);
        nonKeplerianParametersDrivers.add(cicDriver);
        nonKeplerianParametersDrivers.add(cisDriver);
        nonKeplerianParametersDrivers.add(af0Driver);
        nonKeplerianParametersDrivers.add(af1Driver);
        nonKeplerianParametersDrivers.add(af2Driver);

        // automatically update date when time driver is updated
        timeDriver.addObserver(new ParameterObserver() {

            /** {@inheritDoc} */
            @Override
            public void valueChanged(final double previousValue, final ParameterDriver driver,
                                     final AbsoluteDate date) {
                final AbsoluteDate driverDate =
                        new GNSSDate(week, driver.getValue(), system, timeScales).getDate();
                setDate(driverDate);
                setFrame(bodyFixed.getFrozenFrame(inertial, driverDate, FROZEN + bodyFixed.getName()));
            }

            /** {@inheritDoc} */
            @Override
            public void valueSpanMapChanged(final TimeSpanMap<Double> previousValueSpanMap,
                                            final ParameterDriver driver) {
                // nothing to do
            }
        });

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

    /** Get the duration of the GNSS cycle in weeks.
     * @return duration of the GNSS cycle in weeks
     */
    public int getWeeksInCycle() {
        return weeksInCycle;
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
     */
    public void setWeek(final int week) {
        this.week = week;
    }

    /** Get driver for reference time of the GNSS orbit as a duration from week start.
     * @return driver for reference time of the GNSS orbit (s)
     */
    public ParameterDriver getTimeDriver() {
        return timeDriver;
    }

    /** Get driver for change rate in semi-major axis.
     * @return driver for the change rate in semi-major axis
     */
    public ParameterDriver getADotDriver() {
        return aDotDriver;
    }

    /** Get driver for the delta of satellite mean motion.
     * @return driver for the delta of satellite mean motion
     */
    public ParameterDriver getDeltaN0Driver() {
        return deltaN0Driver;
    }

    /** Get driver for the change rate in Δn₀.
     * @return driver for change rate in Δn₀
     */
    public ParameterDriver getDeltaN0DotDriver() {
        return deltaN0DotDriver;
    }

    /** Get driver for rate of inclination angle.
     * @return driver for rate of inclination angle (rad/s)
     */
    public ParameterDriver getIDotDriver() {
        return iDotDriver;
    }

    /** Get driver for rate of right ascension.
     * @return driver for rate of right ascension (rad/s)
     */
    public ParameterDriver getOmegaDotDriver() {
        return domDriver;
    }

    /** Get driver for amplitude of the cosine harmonic correction term to the argument of latitude.
     * @return driver for amplitude of the cosine harmonic correction term to the argument of latitude (rad)
     */
    public ParameterDriver getCucDriver() {
        return cucDriver;
    }

    /** Get driver for amplitude of the sine harmonic correction term to the argument of latitude.
     * @return driver for amplitude of the sine harmonic correction term to the argument of latitude (rad)
     */
    public ParameterDriver getCusDriver() {
        return cusDriver;
    }

    /** Get driver for amplitude of the cosine harmonic correction term to the orbit radius.
     * @return driver for amplitude of the cosine harmonic correction term to the orbit radius (m)
     */
    public ParameterDriver getCrcDriver() {
        return crcDriver;
    }

    /** Get driver for amplitude of the sine harmonic correction term to the orbit radius.
     * @return driver for amplitude of the sine harmonic correction term to the orbit radius (m)
     */
    public ParameterDriver getCrsDriver() {
        return crsDriver;
    }

    /** Get driver for amplitude of the cosine harmonic correction term to the angle of inclination.
     * @return driver for amplitude of the cosine harmonic correction term to the angle of inclination (rad)
     */
    public ParameterDriver getCicDriver() {
        return cicDriver;
    }

    /** Get driver for amplitude of the sine harmonic correction term to the angle of inclination.
     * @return driver for amplitude of the sine harmonic correction term to the angle of inclination (rad)
     */
    public ParameterDriver getCisDriver() {
        return cisDriver;
    }

    /** Get driver for SV zero-th order clock correction.
     * @return driver for SV zero-th order clock correction (s)
     */
    public ParameterDriver getAf0Driver() {
        return af0Driver;
    }

    /** Get driver for SV first order clock correction.
     * @return driver for SV first order clock correction (s/s)
     */
    public ParameterDriver getAf1Driver() {
        return af1Driver;
    }

    /** Get driver for SV second order clock correction.
     * @return driver for SV second order clock correction (s/s²)
     */
    public ParameterDriver getAf2Driver() {
        return af2Driver;
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
        return new KeplerianOrbit(orbitalDrivers.get(0).getValue(),
                                  orbitalDrivers.get(1).getValue(),
                                  orbitalDrivers.get(2).getValue(),
                                  orbitalDrivers.get(3).getValue(),
                                  orbitalDrivers.get(4).getValue(),
                                  orbitalDrivers.get(5).getValue(),
                                  PositionAngleType.MEAN, PositionAngleType.MEAN,
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

/* Copyright 2002-2024 CS GROUP
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
import org.orekit.gnss.SatelliteSystem;
import org.orekit.propagation.analytical.gnss.GNSSPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScales;
import org.orekit.time.TimeStamped;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversProvider;
import org.orekit.utils.ParameterObserver;
import org.orekit.utils.TimeSpanMap;

import java.util.Arrays;
import java.util.List;

/** This class provides the minimal set of orbital elements needed by the {@link GNSSPropagator}.
 *
 * @since 13.0
 * @author Pascal Parraud
 * @author Luc Maisonobe
*/
public class GNSSOrbitalElements implements TimeStamped, ParameterDriversProvider {

    /** Name for time parameter. */
    public static final String TIME = "GnssTime";

    /** Name for semi major axis parameter. */
    public static final String SEMI_MAJOR_AXIS = "GnssSemiMajorAxis";

    /** Name for eccentricity parameter. */
    public static final String ECCENTRICITY = "GnssEccentricity";

    /** Name for inclination at reference time parameter. */
    public static final String INCLINATION = "GnssInclination";

    /** Name for inclination rate parameter. */
    public static final String INCLINATION_RATE = "GnssInclinationRate";

    /** Name for longitude of ascending node at weekly epoch parameter. */
    public static final String NODE_LONGITUDE = "GnssNodeLongitude";

    /** Name for longitude rate parameter. */
    public static final String LONGITUDE_RATE = "GnssLongitudeRate";

    /** Name for argument of perigee parameter. */
    public static final String ARGUMENT_OF_PERIGEE = "GnssPerigeeArgument";

    /** Name for mean anomaly at reference time parameter. */
    public static final String MEAN_ANOMALY = "GnssMeanAnomaly";

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

    /** Index of time in the list returned by {@link #getParametersDrivers()}. */
    public static final int TIME_INDEX = 0;

    /** Index of semi major axis in the list returned by {@link #getParametersDrivers()}. */
    public static final int SMA_INDEX = 1;

    /** Index of eccentricity in the list returned by {@link #getParametersDrivers()}. */
    public static final int E_INDEX = 2;

    /** Index of inclination in the list returned by {@link #getParametersDrivers()}. */
    public static final int I0_INDEX = 3;

    /** Index of inclination rate in the list returned by {@link #getParametersDrivers()}. */
    public static final int IO_DOT_INDEX = 4;

    /** Index of node longitude in the list returned by {@link #getParametersDrivers()}. */
    public static final int OM0_INDEX = 5;

    /** Index of longitude rate in the list returned by {@link #getParametersDrivers()}. */
    public static final int OMEGA_DOT_INDEX = 6;

    /** Index of perigee argument in the list returned by {@link #getParametersDrivers()}. */
    public static final int PA_INDEX = 7;

    /** Index of mean anomaly in the list returned by {@link #getParametersDrivers()}. */
    public static final int M0_INDEX = 8;

    /** Index of cosine on latitude argumen,t in the list returned by {@link #getParametersDrivers()}. */
    public static final int CUC_INDEX = 9;

    /** Index of sine on latitude argume,t in the list returned by {@link #getParametersDrivers()}. */
    public static final int CUS_INDEX = 10;

    /** Index of cosine on radius in the list returned by {@link #getParametersDrivers()}. */
    public static final int CRC_INDEX = 11;

    /** Index of sine on radius in the list returned by {@link #getParametersDrivers()}. */
    public static final int CRS_INDEX = 12;

    /** Index of cosine on inclination in the list returned by {@link #getParametersDrivers()}. */
    public static final int CIC_INDEX = 13;

    /** Index of sine on inclination in the list returned by {@link #getParametersDrivers()}. */
    public static final int CIS_INDEX = 14;

    /** Size of parameters array. */
    private static final int SIZE = 15;

    /** Reference epoch. */
    private AbsoluteDate date;

    /** Earth's universal gravitational parameter. */
    private final double mu;

    /** Mean angular velocity of the Earth for the GNSS model. */
    private final double angularVelocity;

    /** Duration of the GNSS cycle in seconds. */
    private final double cycleDuration;

    /** Satellite system to use for interpreting week number. */
    private final SatelliteSystem system;

    /** Known time scales. */
    private final TimeScales timeScales;

    /** PRN number of the satellite. */
    private int prn;

    /** Reference Week of the orbit. */
    private int week;

    /** Reference time. */
    private final ParameterDriver timeDriver;

    /** Semi-Major Axis (m). */
    private final ParameterDriver smaDriver;

    /** Eccentricity. */
    private final ParameterDriver eccDriver;

    /** Inclination angle at reference time (rad). */
    private final ParameterDriver i0Driver;

    /** Inclination rate (rad/s). */
    private final ParameterDriver iDotDriver;

    /** Longitude of ascending node of orbit plane at weekly epoch (rad). */
    private final ParameterDriver om0Driver;

    /** Rate of right ascension (rad/s). */
    private final ParameterDriver domDriver;

    /** Argument of perigee (rad). */
    private final ParameterDriver aopDriver;

    /** Mean anomaly at reference time (rad). */
    private final ParameterDriver anomDriver;

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

    /**
     * Constructor.
     * @param mu              Earth's universal gravitational parameter
     * @param angularVelocity mean angular velocity of the Earth for the GNSS model
     * @param weeksInCycle    number of weeks in the GNSS cycle
     * @param timeScales      known time scales
     * @param system          satellite system to consider for interpreting week number
     *                        (may be different from real system, for exmple in Rinex nav weeks
     *                        are always according to GPS)
     */
    public GNSSOrbitalElements(final double mu, final double angularVelocity, final int weeksInCycle,
                               final TimeScales timeScales, final SatelliteSystem system) {

        // immutable fields
        this.mu              = mu;
        this.angularVelocity = angularVelocity;
        this.cycleDuration   = GNSSConstants.GNSS_WEEK_IN_SECONDS * weeksInCycle;
        this.system          = system;
        this.timeScales      = timeScales;

        // fields controlled by parameter drivers
        this.timeDriver      = createDriver(TIME);
        this.smaDriver       = createDriver(SEMI_MAJOR_AXIS);
        this.eccDriver       = createDriver(ECCENTRICITY);
        this.i0Driver        = createDriver(INCLINATION);
        this.iDotDriver      = createDriver(INCLINATION_RATE);
        this.om0Driver       = createDriver(NODE_LONGITUDE);
        this.domDriver       = createDriver(LONGITUDE_RATE);
        this.aopDriver       = createDriver(ARGUMENT_OF_PERIGEE);
        this.anomDriver      = createDriver(MEAN_ANOMALY);
        this.cucDriver       = createDriver(LATITUDE_COSINE);
        this.cusDriver       = createDriver(LATITUDE_SINE);
        this.crcDriver       = createDriver(RADIUS_COSINE);
        this.crsDriver       = createDriver(RADIUS_SINE);
        this.cicDriver       = createDriver(INCLINATION_COSINE);
        this.cisDriver       = createDriver(INCLINATION_SINE);

        // automatically update date when time driver is updated
        timeDriver.addObserver(new ParameterObserver() {

            /** {@inheritDoc} */
            @Override
            public void valueChanged(final double previousValue, final ParameterDriver driver, final AbsoluteDate date) {
                GNSSOrbitalElements.this.date = new GNSSDate(week, driver.getValue(), system, timeScales).getDate();
            }

            /** {@inheritDoc} */
            @Override
            public void valueSpanMapChanged(final TimeSpanMap<Double> previousValueSpanMap, final ParameterDriver driver) {
                // nothing to do
            }
        });

    }

    /** Create parameter driver.
     * @param name name of the driver
     * @return build driver
     */
    protected static ParameterDriver createDriver(final String name) {
        return new ParameterDriver(name, 0, FastMath.scalb(1.0, -30),
                                   Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        // ensure the parameters are really at the advertised indices
        final ParameterDriver[] array = new ParameterDriver[SIZE];
        array[TIME_INDEX]      = getTimeDriver();
        array[SMA_INDEX]       = getSmaDriver();
        array[E_INDEX]         = getEDriver();
        array[I0_INDEX]        = getI0Driver();
        array[IO_DOT_INDEX]    = getIDotDriver();
        array[OM0_INDEX]       = getOmega0Driver();
        array[OMEGA_DOT_INDEX] = getOmegaDotDriver();
        array[PA_INDEX]        = getPaDriver();
        array[M0_INDEX]        = getM0Driver();
        array[CUC_INDEX]       = getCucDriver();
        array[CUS_INDEX]       = getCusDriver();
        array[CRC_INDEX]       = getCrcDriver();
        array[CRS_INDEX]       = getCrsDriver();
        array[CIC_INDEX]       = getCicDriver();
        array[CIS_INDEX]       = getCisDriver();
        return Arrays.asList(array);
    }
    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    /** Get the driver for the Earth's universal gravitational parameter.
     * @return driver for the Earth's universal gravitational parameter
     */
    public double getMu() {
        return mu;
    }

    /** Get the driver for the mean angular velocity of the Earth of the GNSS model.
     * @return driver for the mean angular velocity of the Earth of the GNSS model
     */
    public double getAngularVelocity() {
        return angularVelocity;
    }

    /** Get the driver for the duration of the GNSS cycle in seconds.
     * @return driver for the duration of the GNSS cycle in seconds
     */
    public double getCycleDuration() {
        return cycleDuration;
    }

    /** Get the PRN number of the satellite.
     * @return PRN number of the satellite
     */
    public int getPRN() {
        return prn;
    }

    /** Set the PRN number of the satellite.
     * @param number the prn number ot set
     */
    public void setPRN(final int number) {
        this.prn = number;
    }

    /** Get the reference week of the orbit.
     * @return reference week of the orbit
     */
    public int getWeek() {
        return week;
    }

    /** Set the reference week of the orbit.
     * @param week the week to set
     */
    public void setWeek(final int week) {
        this.week = week;
        this.date = new GNSSDate(week, timeDriver.getValue(), system, timeScales).getDate();
    }

    /** Get the driver for reference time of the GNSS orbit as a duration from week start.
     * @return driver for the reference time of the GNSS orbit (s)
     */
    public ParameterDriver getTimeDriver() {
        return timeDriver;
    }

    /** Get reference time of the GNSS orbit as a duration from week start.
     * @return reference time of the GNSS orbit (s)
     */
    public double getTime() {
        return getTimeDriver().getValue();
    }

    /** Set reference time of the GNSS orbit as a duration from week start.
     * @param time reference time of the GNSS orbit (s)
     */
    public void setTime(final double time) {
        getTimeDriver().setValue(time);
    }

    /** Get semi-major axis.
     * @return driver for the semi-major axis (m)
     */
    public ParameterDriver getSmaDriver() {
        return smaDriver;
    }

    /** Get semi-major axis.
     * @return semi-major axis (m)
     */
    public double getSma() {
        return getSmaDriver().getValue();
    }

    /** Set semi-major axis.
     * @param sma demi-major axis (m)
     */
    public void setSma(final double sma) {
        getSmaDriver().setValue(sma);
    }

    /** Get the mean motion.
     * @return the mean motion (rad/s)
     */
    public double getMeanMotion() {
        final double absA = FastMath.abs(getSma());
        return FastMath.sqrt(getMu() / absA) / absA;
    }


    /** Get the driver for the eccentricity.
     * @return driver for the eccentricity
     */
    public ParameterDriver getEDriver() {
        return eccDriver;
    }

    /** Get eccentricity.
     * @return eccentricity
     */
    public double getE() {
        return getEDriver().getValue();
    }

    /** Set eccentricity.
     * @param e eccentricity
     */
    public void setE(final double e) {
        getEDriver().setValue(e);
    }

    /** Get the driver for the inclination angle at reference time.
     * @return driver for the inclination angle at reference time (rad)
     */
    public ParameterDriver getI0Driver() {
        return i0Driver;
    }

    /** Get the inclination angle at reference time.
     * @return inclination angle at reference time (rad)
     */
    public double getI0() {
        return getI0Driver().getValue();
    }

    /** Set inclination angle at reference time.
     * @param i0 inclination angle at reference time (rad)
     */
    public void setI0(final double i0) {
        getI0Driver().setValue(i0);
    }

    /** Get the driver for the rate of inclination angle.
     * @return driver for the rate of inclination angle (rad/s)
     */
    public ParameterDriver getIDotDriver() {
        return iDotDriver;
    }

    /** Get rate of inclination angle.
     * @return rate of inclination angle (rad/s)
     */
    public double getIDot() {
        return getIDotDriver().getValue();
    }

    /** Set the driver for the rate of inclination angle.
     * @param iDot rate of inclination angle (rad/s)
     */
    public void setIDot(final double iDot) {
        getIDotDriver().setValue(iDot);
    }

    /** Get the driver for the longitude of ascending node of orbit plane at weekly epoch.
     * @return driver for the longitude of ascending node of orbit plane at weekly epoch (rad)
     */
    public ParameterDriver getOmega0Driver() {
        return om0Driver;
    }

    /** Get longitude of ascending node of orbit plane at weekly epoch.
     * @return longitude of ascending node of orbit plane at weekly epoch (rad)
     */
    public double getOmega0() {
        return getOmega0Driver().getValue();
    }

    /** Set longitude of ascending node of orbit plane at weekly epoch.
     * @param om0 longitude of ascending node of orbit plane at weekly epoch (rad)
     */
    public void setOmega0(final double om0) {
        getOmega0Driver().setValue(om0);
    }

    /** Get the driver for the rate of right ascension.
     * @return driver for the rate of right ascension (rad/s)
     */
    public ParameterDriver getOmegaDotDriver() {
        return domDriver;
    }

    /** Get rate of right ascension.
     * @return rate of right ascension (rad/s)
     */
    public double getOmegaDot() {
        return getOmegaDotDriver().getValue();
    }

    /** Set rate of right ascension.
     * @param dom rate of right ascension (rad/s)
     */
    public void setOmegaDot(final double dom) {
        getOmegaDotDriver().setValue(dom);
    }

    /** Get the driver for the argument of perigee.
     * @return driver for the argument of perigee (rad)
     */
    public ParameterDriver getPaDriver() {
        return aopDriver;
    }

    /** Get argument of perigee.
     * @return argument of perigee (rad)
     */
    public double getPa() {
        return getPaDriver().getValue();
    }

    /** Set argument of perigee.
     * @param aop argument of perigee (rad)
     */
    public void setPa(final double aop) {
        getPaDriver().setValue(aop);
    }

    /** Get the driver for the mean anomaly at reference time.
     * @return driver for the mean anomaly at reference time (rad)
     */
    public ParameterDriver getM0Driver() {
        return anomDriver;
    }

    /** Get mean anomaly at reference time.
     * @return mean anomaly at reference time (rad)
     */
    public double getM0() {
        return getM0Driver().getValue();
    }

    /** Set mean anomaly at reference time.
     * @param anom mean anomaly at reference time (rad)
     */
    public void setM0(final double anom) {
        getM0Driver().setValue(anom);
    }

    /** Get the driver for the amplitude of the cosine harmonic correction term to the argument of latitude.
     * @return driver for the amplitude of the cosine harmonic correction term to the argument of latitude (rad)
     */
    public ParameterDriver getCucDriver() {
        return cucDriver;
    }

    /** Get amplitude of the cosine harmonic correction term to the argument of latitude.
     * @return amplitude of the cosine harmonic correction term to the argument of latitude (rad)
     */
    public double getCuc() {
        return getCucDriver().getValue();
    }

    /** Set amplitude of the cosine harmonic correction term to the argument of latitude.
     * @param cuc amplitude of the cosine harmonic correction term to the argument of latitude (rad)
     */
    public void setCuc(final double cuc) {
        getCucDriver().setValue(cuc);
    }

    /** Get the driver for the amplitude of the sine harmonic correction term to the argument of latitude.
     * @return driver for the amplitude of the sine harmonic correction term to the argument of latitude (rad)
     */
    public ParameterDriver getCusDriver() {
        return cusDriver;
    }

    /** Get amplitude of the sine harmonic correction term to the argument of latitude.
     * @return amplitude of the sine harmonic correction term to the argument of latitude (rad)
     */
    public double getCus() {
        return getCusDriver().getValue();
    }

    /** Set amplitude of the sine harmonic correction term to the argument of latitude.
     * @param cus amplitude of the sine harmonic correction term to the argument of latitude (rad)
     */
    public void setCus(final double cus) {
        getCusDriver().setValue(cus);
    }

    /** Get the driver for the amplitude of the cosine harmonic correction term to the orbit radius.
     * @return driver for the amplitude of the cosine harmonic correction term to the orbit radius (m)
     */
    public ParameterDriver getCrcDriver() {
        return crcDriver;
    }

    /** Get amplitude of the cosine harmonic correction term to the orbit radius.
     * @return amplitude of the cosine harmonic correction term to the orbit radius (m)
     */
    public double getCrc() {
        return getCrcDriver().getValue();
    }

    /** Set amplitude of the cosine harmonic correction term to the orbit radius.
     * @param crc amplitude of the cosine harmonic correction term to the orbit radius (m)
     */
    public void setCrc(final double crc) {
        getCrcDriver().setValue(crc);
    }

    /** Get the driver for the amplitude of the sine harmonic correction term to the orbit radius.
     * @return driver for the amplitude of the sine harmonic correction term to the orbit radius (m)
     */
    public ParameterDriver getCrsDriver() {
        return crsDriver;
    }

    /** Get amplitude of the sine harmonic correction term to the orbit radius.
     * @return amplitude of the sine harmonic correction term to the orbit radius (m)
     */
    public double getCrs() {
        return getCrsDriver().getValue();
    }

    /** Set amplitude of the sine harmonic correction term to the orbit radius.
     * @param crs amplitude of the sine harmonic correction term to the orbit radius (m)
     */
    public void setCrs(final double crs) {
        getCrsDriver().setValue(crs);
    }

    /** Get the driver for the amplitude of the cosine harmonic correction term to the angle of inclination.
     * @return driver for the amplitude of the cosine harmonic correction term to the angle of inclination (rad)
     */
    public ParameterDriver getCicDriver() {
        return cicDriver;
    }

    /** Get amplitude of the cosine harmonic correction term to the angle of inclination.
     * @return amplitude of the cosine harmonic correction term to the angle of inclination (rad)
     */
    public double getCic() {
        return getCicDriver().getValue();
    }

    /** Set amplitude of the cosine harmonic correction term to the angle of inclination.
     * @param cic amplitude of the cosine harmonic correction term to the angle of inclination (rad)
     */
    public void setCic(final double cic) {
        getCicDriver().setValue(cic);
    }

    /** Get the driver for the amplitude of the sine harmonic correction term to the angle of inclination.
     * @return driver for the amplitude of the sine harmonic correction term to the angle of inclination (rad)
     */
    public ParameterDriver getCisDriver() {
        return cisDriver;
    }

    /** Get amplitude of the sine harmonic correction term to the angle of inclination.
     * @return amplitude of the sine harmonic correction term to the angle of inclination (rad)
     */
    public double getCis() {
        return getCisDriver().getValue();
    }

    /** Set amplitude of the sine harmonic correction term to the angle of inclination.
     * @param cis amplitude of the sine harmonic correction term to the angle of inclination (rad)
     */
    public void setCis(final double cis) {
        getCisDriver().setValue(cis);
    }

}

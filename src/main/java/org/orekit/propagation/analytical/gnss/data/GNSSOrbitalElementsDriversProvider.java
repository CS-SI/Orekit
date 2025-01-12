/* Copyright 2022-2025 Luc Maisonobe
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
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScales;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversProvider;
import org.orekit.utils.ParameterObserver;
import org.orekit.utils.TimeSpanMap;

import java.util.Arrays;
import java.util.List;

/** This class manages the non-keplerian parameter drivers for
 * {@link GNSSOrbitalElements} and {@link FieldGnssOrbitalElements}.
 * <p>
 * In both primitive double and field classes, only the non-Keplerian parameters
 * are returned in the {@link #getParametersDrivers()} method, the Keplerian orbital
 * parameters must be accessed independently. These groups ensure proper separate
 * computation of state transition matrix and Jacobian matrix by
 * {@link org.orekit.propagation.analytical.gnss.GNSSPropagator} and
 * {@link org.orekit.propagation.analytical.gnss.FieldGnssPropagator}.
 * </p>
 * @since 13.0
 * @author Luc Maisonobe
 */
public abstract class GNSSOrbitalElementsDriversProvider
    implements ParameterDriversProvider {

    /** Name for time parameter. */
    public static final String TIME = "GnssTime";

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

    /** Index of time in the list returned by {@link #getParametersDrivers()}. */
    public static final int TIME_INDEX = 0;

    /** Index of inclination rate in the list returned by {@link #getParametersDrivers()}. */
    public static final int I_DOT_INDEX = TIME_INDEX + 1;

    /** Index of longitude rate in the list returned by {@link #getParametersDrivers()}. */
    public static final int OMEGA_DOT_INDEX = I_DOT_INDEX + 1;

    /** Index of cosine on latitude argument in the list returned by {@link #getParametersDrivers()}. */
    public static final int CUC_INDEX = OMEGA_DOT_INDEX + 1;

    /** Index of sine on latitude argument in the list returned by {@link #getParametersDrivers()}. */
    public static final int CUS_INDEX = CUC_INDEX + 1;

    /** Index of cosine on radius in the list returned by {@link #getParametersDrivers()}. */
    public static final int CRC_INDEX = CUS_INDEX + 1;

    /** Index of sine on radius in the list returned by {@link #getParametersDrivers()}. */
    public static final int CRS_INDEX = CRC_INDEX + 1;

    /** Index of cosine on inclination in the list returned by {@link #getParametersDrivers()}. */
    public static final int CIC_INDEX = CRS_INDEX + 1;

    /** Index of sine on inclination in the list returned by {@link #getParametersDrivers()}. */
    public static final int CIS_INDEX = CIC_INDEX + 1;

    /** Size of parameters array. */
    public static final int SIZE = CIS_INDEX + 1;

    /** Mean angular velocity of the Earth for the GNSS model. */
    private final double angularVelocity;

    /** Duration of the GNSS cycle in weeks. */
    private final int weeksInCycle;

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

    /** Constructor.
     * @param angularVelocity mean angular velocity of the Earth for the GNSS model
     * @param weeksInCycle    number of weeks in the GNSS cycle
     * @param timeScales      known time scales
     * @param system          satellite system to consider for interpreting week number
     *                        (may be different from real system, for example in Rinex nav, weeks
     *                        are always according to GPS)
     */
    protected GNSSOrbitalElementsDriversProvider(final double angularVelocity, final int weeksInCycle,
                                                 final TimeScales timeScales, final SatelliteSystem system) {

        // immutable fields
        this.angularVelocity = angularVelocity;
        this.weeksInCycle    = weeksInCycle;
        this.cycleDuration   = GNSSConstants.GNSS_WEEK_IN_SECONDS * weeksInCycle;
        this.system          = system;
        this.timeScales      = timeScales;

        this.timeDriver = createDriver(TIME);
        this.iDotDriver = createDriver(INCLINATION_RATE);
        this.domDriver  = createDriver(LONGITUDE_RATE);
        this.cucDriver  = createDriver(LATITUDE_COSINE);
        this.cusDriver  = createDriver(LATITUDE_SINE);
        this.crcDriver  = createDriver(RADIUS_COSINE);
        this.crsDriver  = createDriver(RADIUS_SINE);
        this.cicDriver  = createDriver(INCLINATION_COSINE);
        this.cisDriver  = createDriver(INCLINATION_SINE);

        // automatically update date when time driver is updated
        timeDriver.addObserver(new ParameterObserver() {

            /** {@inheritDoc} */
            @Override
            public void valueChanged(final double previousValue, final ParameterDriver driver,
                                     final AbsoluteDate date) {
                setGnssDate(new GNSSDate(week, driver.getValue(), system, timeScales));
            }

            /** {@inheritDoc} */
            @Override
            public void valueSpanMapChanged(final TimeSpanMap<Double> previousValueSpanMap,
                                            final ParameterDriver driver) {
                // nothing to do
            }
        });

    }

    /** Copy drivers selection settings from another instance.
     * @param original original instance providing selection settings
     */
    protected void copySelectionSettings(final GNSSOrbitalElementsDriversProvider original) {
        timeDriver.setSelected(original.timeDriver.isSelected());
        iDotDriver.setSelected(original.iDotDriver.isSelected());
        domDriver.setSelected(original.domDriver.isSelected());
        cucDriver.setSelected(original.cucDriver.isSelected());
        cusDriver.setSelected(original.cusDriver.isSelected());
        crcDriver.setSelected(original.crcDriver.isSelected());
        crsDriver.setSelected(original.crsDriver.isSelected());
        cicDriver.setSelected(original.cicDriver.isSelected());
        cisDriver.setSelected(original.cisDriver.isSelected());
    }

    /** Set GNSS date.
     * @param gnssDate GNSS date
     */
    protected abstract void setGnssDate(GNSSDate gnssDate);

    /** Create parameter driver.
     * @param name name of the driver
     * @return build driver
     */
    protected static ParameterDriver createDriver(final String name) {
        return new ParameterDriver(name, 0, FastMath.scalb(1.0, -30),
                                   Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /** Get satellite system.
     * @return satellite system
     */
    public SatelliteSystem getSystem() {
        return system;
    }

    /** Get known time scales.
     * @return known time scales
     */
    public TimeScales getTimeScales() {
        return timeScales;
    }

    /** {@inheritDoc}
     * <p>
     * Only the 9 non-Keplerian evolution parameters are listed here,
     * i.e. {@link #getTimeDriver()} (at index {@link #TIME_INDEX}),
     * {@link #getIDotDriver()} (at index {@link #I_DOT_INDEX}),
     * {@link #getOmegaDotDriver()} (at index {@link #OMEGA_DOT_INDEX}),
     * {@link #getCucDriver()} (at index {@link #CUC_INDEX}),
     * {@link #getCusDriver()} (at index {@link #CUS_INDEX}),
     * {@link #getCrcDriver()} (at index {@link #CRC_INDEX}),
     * {@link #getCrsDriver()} (at index {@link #CRS_INDEX}),
     * {@link #getCicDriver()} (at index {@link #CIC_INDEX}),
     * and {@link #getCisDriver()} (at index {@link #CIS_INDEX})
     * </p>
     * <p>
     * The Keplerian orbital parameters drivers are not included.
     * </p>
     */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        // ensure the parameters are really at the advertised indices
        final ParameterDriver[] array = new ParameterDriver[SIZE];
        array[TIME_INDEX]      = getTimeDriver();
        array[I_DOT_INDEX]     = getIDotDriver();
        array[OMEGA_DOT_INDEX] = getOmegaDotDriver();
        array[CUC_INDEX]       = getCucDriver();
        array[CUS_INDEX]       = getCusDriver();
        array[CRC_INDEX]       = getCrcDriver();
        array[CRS_INDEX]       = getCrsDriver();
        array[CIC_INDEX]       = getCicDriver();
        array[CIS_INDEX]       = getCisDriver();
        return Arrays.asList(array);
    }

    /** Get the mean angular velocity of the Earth of the GNSS model.
     * @return mean angular velocity of the Earth of the GNSS model
     */
    public double getAngularVelocity() {
        return angularVelocity;
    }

    /** Get for the duration of the GNSS cycle in weeks.
     * @return the duration of the GNSS cycle in weeks
     */
    public int getWeeksInCycle() {
        return weeksInCycle;
    }

    /** Get for the duration of the GNSS cycle in seconds.
     * @return the duration of the GNSS cycle in seconds
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
        setGnssDate(new GNSSDate(week, timeDriver.getValue(), system, timeScales));
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

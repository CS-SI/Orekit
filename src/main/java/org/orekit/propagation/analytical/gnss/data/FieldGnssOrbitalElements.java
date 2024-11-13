/* Copyright 2002-2024 Thales Alenia Space
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScales;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversProvider;
import org.orekit.utils.ParameterObserver;
import org.orekit.utils.TimeSpanMap;

import java.util.ArrayList;
import java.util.List;

/** This class provides the minimal set of orbital elements needed by the {@link
 * org.orekit.propagation.analytical.gnss.FieldGnssPropagator}.
 * @since 13.0
 * @author Luc Maisonobe
*/
public class FieldGnssOrbitalElements<T extends CalculusFieldElement<T>> implements ParameterDriversProvider {

    /** Reference epoch. */
    private final FieldAbsoluteDate<T> date;

    /** Earth's universal gravitational parameter. */
    private final T mu;

    /** Mean angular velocity of the Earth for the GNSS model. */
    private final double angularVelocity;

    /** Duration of the GNSS cycle in seconds. */
    private final double cycleDuration;

    /** PRN number of the satellite. */
    private final int prn;

    /** Reference Week of the orbit. */
    private final int week;

    /** Semi-Major Axis (m). */
    private final T sma;

    /** Eccentricity. */
    private final T ecc;

    /** Inclination angle at reference time (rad). */
    private final T i0;

    /** Argument of perigee (rad). */
    private final T aop;

    /** Longitude of ascending node of orbit plane at weekly epoch (rad). */
    private final T om0;

    /** Mean anomaly at reference time (rad). */
    private final T anom;

    /** Non-Keplerian evolution parameters. */
    private final T[] nonKeplerian;

    /** Drivers for the non-KEplerian evolution parameters. */
    private final List<ParameterDriver> drivers;

    /** Simple constructor.
     * @param mu              Earth's universal gravitational parameter
     * @param angularVelocity mean angular velocity of the Earth for the GNSS model
     * @param cycleDuration   duration of the GNSS cycle in seconds
     * @param system          satellite system to consider for interpreting week number
     *                        (may be different from real system, for example in Rinex nav weeks
     *                        are always according to GPS)
     * @param timeScales      known time scales
     * @param prn             PRN number of the satellite
     * @param week            reference week
     * @param sma             semi-major axis (m)
     * @param ecc             eccentricity
     * @param i0              inclination angle at reference time (rad)
     * @param aop             argument of perigee (rad)
     * @param om0             longitude of ascending node of orbit plane at weekly epoch (rad)
     * @param anom            mean anomaly at reference time (rad)
     * @param nonKeplerian    non-keplerian parameters (indices according to the ones in {@link GNSSOrbitalElements})
     */
    public FieldGnssOrbitalElements(final T mu, final double angularVelocity, final double cycleDuration,
                                    final SatelliteSystem system, final TimeScales timeScales,
                                    final int prn, final int week,
                                    final T sma, final T ecc, final T i0, final T aop, final T om0, final T anom,
                                    final T[] nonKeplerian) {

        // immutable fields
        this.mu              = mu;
        this.angularVelocity = angularVelocity;
        this.cycleDuration   = cycleDuration;
        this.date            = new FieldAbsoluteDate<>(sma.getField(),
                                                       new GNSSDate(week, 0, system, timeScales).getDate()).
                               shiftedBy(nonKeplerian[GNSSOrbitalElements.TIME_INDEX]);
        this.prn             = prn;
        this.week            = week;

        // fields for Keplerian orbital elements
        this.sma = sma;
        this.ecc = ecc;
        this.i0 = i0;
        this.aop = aop;
        this.om0 = om0;
        this.anom = anom;

        // non-Keplerian evolution parameters
        this.nonKeplerian = nonKeplerian.clone();

        // drivers for the non-Keplerian evolution parameters
        // ensure the parameters are really at the advertised indices
        this.drivers = new ArrayList<>(GNSSOrbitalElements.SIZE);
        for (int i = 0; i < GNSSOrbitalElements.SIZE; ++i) {
            // fill-up with null elements so createDriver can use List.set
            drivers.add(null);
        }
        createDriver(GNSSOrbitalElements.TIME,               GNSSOrbitalElements.TIME_INDEX);
        createDriver(GNSSOrbitalElements.INCLINATION_RATE,   GNSSOrbitalElements.I_DOT_INDEX);
        createDriver(GNSSOrbitalElements.LONGITUDE_RATE,     GNSSOrbitalElements.OMEGA_DOT_INDEX);
        createDriver(GNSSOrbitalElements.LATITUDE_COSINE,    GNSSOrbitalElements.CUC_INDEX);
        createDriver(GNSSOrbitalElements.LATITUDE_SINE,      GNSSOrbitalElements.CUS_INDEX);
        createDriver(GNSSOrbitalElements.RADIUS_COSINE,      GNSSOrbitalElements.CRC_INDEX);
        createDriver(GNSSOrbitalElements.RADIUS_SINE,        GNSSOrbitalElements.CRS_INDEX);
        createDriver(GNSSOrbitalElements.INCLINATION_COSINE, GNSSOrbitalElements.CIC_INDEX);
        createDriver(GNSSOrbitalElements.INCLINATION_SINE,   GNSSOrbitalElements.CIS_INDEX);

    }

    /** Create driver for one parameter.
     * @param name parameter name
     * @param index index in the list
     */
    private void createDriver(final String name, final int index) {

        // create the driver
        final ParameterDriver driver =
            new ParameterDriver(name, nonKeplerian[index].getReal(), FastMath.scalb(1.0, -30),
                                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        // bind the driver to field elements
        driver.addObserver(new ParameterObserver() {

            /** {@inheritDoc} */
            @Override
            public void valueChanged(final double previousValue, final ParameterDriver driver,
                                     final AbsoluteDate date) {
                // increment field the same way driver was changed
                nonKeplerian[index] = nonKeplerian[index].add(driver.getValue() - previousValue);
            }

            /** {@inheritDoc} */
            @Override
            public void valueSpanMapChanged(final TimeSpanMap<Double> previousValueSpanMap,
                                            final ParameterDriver driver) {
                // nothing to do
            }
        });

        // store the driver at specified index
        drivers.set(index, driver);

    }

    /** {@inheritDoc}
     * <p>
     * The parameters are in the same order as in {@link GNSSOrbitalElements}
     * </p>
     */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return drivers;
    }

    /** Get date.
     * @return date
     */
    public FieldAbsoluteDate<T> getDate() {
        return date;
    }

    /** Get the Earth's universal gravitational parameter.
     * @return the Earth's universal gravitational parameter
     */
    public T getMu() {
        return mu;
    }

    /** Get the mean angular velocity of the Earth of the GNSS model.
     * @return the mean angular velocity of the Earth of the GNSS model
     */
    public double getAngularVelocity() {
        return angularVelocity;
    }

    /** Get the duration of the GNSS cycle in seconds.
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

    /** Get the reference week of the orbit.
     * @return reference week of the orbit
     */
    public int getWeek() {
        return week;
    }

    /** Get reference time of the GNSS orbit as a duration from week start.
     * @return reference time of the GNSS orbit (s)
     */
    public T getTime() {
        return nonKeplerian[GNSSOrbitalElements.TIME_INDEX];
    }

    /** Get semi-major axis.
     * @return semi-major axis (m)
     */
    public T getSma() {
        return sma;
    }

    /** Get the mean motion.
     * @return the mean motion (rad/s)
     */
    public T getMeanMotion() {
        final T invA = FastMath.abs(getSma()).reciprocal();
        return FastMath.sqrt(getMu().multiply(invA)).multiply(invA);
    }


    /** Get eccentricity.
     * @return eccentricity
     */
    public T getE() {
        return ecc;
    }

    /** Get the inclination angle at reference time.
     * @return inclination angle at reference time (rad)
     */
    public T getI0() {
        return i0;
    }

    /** Get rate of inclination angle.
     * @return rate of inclination angle (rad/s)
     */
    public T getIDot() {
        return nonKeplerian[GNSSOrbitalElements.I_DOT_INDEX];
    }

    /** Get longitude of ascending node of orbit plane at weekly epoch.
     * @return longitude of ascending node of orbit plane at weekly epoch (rad)
     */
    public T getOmega0() {
        return om0;
    }

    /** Get rate of right ascension.
     * @return rate of right ascension (rad/s)
     */
    public T getOmegaDot() {
        return nonKeplerian[GNSSOrbitalElements.OMEGA_DOT_INDEX];
    }

    /** Get argument of perigee.
     * @return argument of perigee (rad)
     */
    public T getPa() {
        return aop;
    }

    /** Get mean anomaly at reference time.
     * @return mean anomaly at reference time (rad)
     */
    public T getM0() {
        return anom;
    }

    /** Get amplitude of the cosine harmonic correction term to the argument of latitude.
     * @return amplitude of the cosine harmonic correction term to the argument of latitude (rad)
     */
    public T getCuc() {
        return nonKeplerian[GNSSOrbitalElements.CUC_INDEX];
    }

    /** Get amplitude of the sine harmonic correction term to the argument of latitude.
     * @return amplitude of the sine harmonic correction term to the argument of latitude (rad)
     */
    public T getCus() {
        return nonKeplerian[GNSSOrbitalElements.CUS_INDEX];
    }

    /** Get amplitude of the cosine harmonic correction term to the orbit radius.
     * @return amplitude of the cosine harmonic correction term to the orbit radius (m)
     */
    public T getCrc() {
        return nonKeplerian[GNSSOrbitalElements.CRC_INDEX];
    }

    /** Get amplitude of the sine harmonic correction term to the orbit radius.
     * @return amplitude of the sine harmonic correction term to the orbit radius (m)
     */
    public T getCrs() {
        return nonKeplerian[GNSSOrbitalElements.CRS_INDEX];
    }

    /** Get amplitude of the cosine harmonic correction term to the angle of inclination.
     * @return amplitude of the cosine harmonic correction term to the angle of inclination (rad)
     */
    public T getCic() {
        return nonKeplerian[GNSSOrbitalElements.CIC_INDEX];
    }

    /** Get amplitude of the sine harmonic correction term to the angle of inclination.
     * @return amplitude of the sine harmonic correction term to the angle of inclination (rad)
     */
    public T getCis() {
        return nonKeplerian[GNSSOrbitalElements.CIS_INDEX];
    }

}

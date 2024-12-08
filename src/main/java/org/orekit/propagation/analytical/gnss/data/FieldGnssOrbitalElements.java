/* Copyright 2002-2024 Luc Maisonobe
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
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScales;

/** This class provides the minimal set of orbital elements needed by the {@link
 * org.orekit.propagation.analytical.gnss.FieldGnssPropagator}.
 * @param <T> type of the field elements
 * @param <O> type of the orbital elements
 * @since 13.0
 * @author Luc Maisonobe
*/
public abstract class FieldGnssOrbitalElements<T extends CalculusFieldElement<T>, O extends FieldGnssOrbitalElements<T, O>>
    extends GNSSOrbitalElementsDriversProvider<FieldGnssOrbitalElements<T, O>>
    implements FieldTimeStamped<T> {

    /** Earth's universal gravitational parameter. */
    private final T mu;

    /** Reference epoch. */
    private FieldAbsoluteDate<T> date;

    /** Semi-Major Axis (m). */
    private T sma;

    /** Eccentricity. */
    private T ecc;

    /** Inclination angle at reference time (rad). */
    private T i0;

    /** Argument of perigee (rad). */
    private T aop;

    /** Longitude of ascending node of orbit plane at weekly epoch (rad). */
    private T om0;

    /** Mean anomaly at reference time (rad). */
    private T anom;

    /** Simple constructor.
     * @param mu              Earth's universal gravitational parameter
     * @param angularVelocity mean angular velocity of the Earth for the GNSS model
     * @param weeksInCycle    number of weeks in the GNSS cycle
     * @param timeScales      known time scales
     * @param system          satellite system to consider for interpreting week number
     *                        (may be different from real system, for example in Rinex nav weeks
     *                        are always according to GPS)
     */
    protected FieldGnssOrbitalElements(final T mu, final double angularVelocity, final int weeksInCycle,
                                       final TimeScales timeScales, final SatelliteSystem system) {

        super(angularVelocity, weeksInCycle, timeScales, system);

        // immutable field
        this.mu              = mu;

        // Keplerian orbital elements
        this.sma             = mu.newInstance(Double.NaN);
        this.ecc             = mu.newInstance(Double.NaN);
        this.i0              = mu.newInstance(Double.NaN);
        this.aop             = mu.newInstance(Double.NaN);
        this.om0             = mu.newInstance(Double.NaN);
        this.anom            = mu.newInstance(Double.NaN);

    }

    /** {@inheritDoc} */
    protected void setGnssDate(final GNSSDate gnssDate) {
        this.date = new FieldAbsoluteDate<>(mu.getField(), gnssDate.getDate());
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

    /** Get semi-major axis.
     * @return semi-major axis (m)
     */
    public T getSma() {
        return sma;
    }

    /** Set semi-major axis.
     * @param sma demi-major axis (m)
     */
    public void setSma(final T sma) {
        this.sma = sma;
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

    /** Set eccentricity.
     * @param e eccentricity
     */
    public void setE(final T e) {
        this.ecc = e;
    }

    /** Get the inclination angle at reference time.
     * @return inclination angle at reference time (rad)
     */
    public T getI0() {
        return i0;
    }

    /** Set inclination angle at reference time.
     * @param i0 inclination angle at reference time (rad)
     */
    public void setI0(final T i0) {
        this.i0 = i0;
    }

    /** Get longitude of ascending node of orbit plane at weekly epoch.
     * @return longitude of ascending node of orbit plane at weekly epoch (rad)
     */
    public T getOmega0() {
        return om0;
    }

    /** Set longitude of ascending node of orbit plane at weekly epoch.
     * @param om0 longitude of ascending node of orbit plane at weekly epoch (rad)
     */
    public void setOmega0(final T om0) {
        this.om0 = om0;
    }

    /** Get argument of perigee.
     * @return argument of perigee (rad)
     */
    public T getPa() {
        return aop;
    }

    /** Set argument of perigee.
     * @param aop argument of perigee (rad)
     */
    public void setPa(final T aop) {
        this.aop = aop;
    }

    /** Get mean anomaly at reference time.
     * @return mean anomaly at reference time (rad)
     */
    public T getM0() {
        return anom;
    }

    /** Set mean anomaly at reference time.
     * @param anom mean anomaly at reference time (rad)
     */
    public void setM0(final T anom) {
        this.anom = anom;
    }

}

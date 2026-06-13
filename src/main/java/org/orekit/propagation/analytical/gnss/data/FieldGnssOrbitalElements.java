/* Copyright 2022-2026 Luc Maisonobe
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
import org.hipparchus.Field;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbitalParameters;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScales;

import java.util.function.Function;

/** This class provides the minimal set of orbital elements needed by the {@link
 * org.orekit.propagation.analytical.gnss.FieldGnssPropagator}.
 * @param <T> type of the field elements
 * @param <O> type of the orbital elements (non-field version)
 * @since 13.0
 * @author Luc Maisonobe
*/
public abstract class FieldGnssOrbitalElements<T extends CalculusFieldElement<T>, O extends GNSSOrbitalElements<O>>
    implements FieldOrbitalParameters<T>, FieldGNSSClockElements<T> {

    /** Mean angular velocity of the Earth for the GNSS model. */
    private final double angularVelocity;

    /** Duration of the GNSS cycle in weeks. */
    private final int weeksInCycle;

    /** Duration of the GNSS cycle in seconds. */
    private final double cycleDuration;

    /** Known time scales. */
    private final TimeScales timeScales;

    /** Satellite system to use for interpreting week number. */
    private final SatelliteSystem system;

    /** Message type (null if not a navigation message). */
    private final String type;

    /** PRN number of the satellite. */
    private final int prn;

    /** Reference Week of the orbit. */
    private final int week;

    /** Orbit. */
    private final FieldKeplerianOrbit<T> orbit;

    /** Reference time. */
    private final T time;

    /** Change rate in semi-major axis (m/s).
     * @since 14.0
     */
    private final T aDot;

    /** Delta of satellite mean motion.
     * @since 14.0
     */
    private final T deltaN0;

    /** Change rate in Δn₀.
     * @since 14.0
     */
    private final T deltaN0Dot;

    /** Inclination rate (rad/s). */
    private final T iDot;

    /** Rate of right ascension (rad/s). */
    private final T omegaDot;

    /** Amplitude of the cosine harmonic correction term to the argument of latitude. */
    private final T cuc;

    /** Amplitude of the sine harmonic correction term to the argument of latitude. */
    private final T cus;

    /** Amplitude of the cosine harmonic correction term to the orbit radius. */
    private final T crc;

    /** Amplitude of the sine harmonic correction term to the orbit radius. */
    private final T crs;

    /** Amplitude of the cosine harmonic correction term to the inclination. */
    private final T cic;

    /** Amplitude of the sine harmonic correction term to the inclination. */
    private final T cis;

    /** SV zero-th order clock correction (s). */
    private final T af0;

    /** SV first order clock correction (s/s). */
    private final T af1;

    /** SV second order clock correction (s/s²). */
    private final T af2;

    /** Group delay differential TGD for L1-L2 correction. */
    private final T tgd;

    /** Time Of Clock. */
    private final T toc;

    /** Creates a new instance.
     * @param angularVelocity mean angular velocity of the Earth for the GNSS model
     * @param weeksInCycle    number of weeks in the GNSS cycle
     * @param timeScales      known time scales
     * @param system          satellite system to consider for interpreting week number
     *                        (may be different from real system, for example in Rinex nav, weeks
     *                        are always according to GPS)
     * @param type            type (null if not a navigation message)
     * @param prn             PRN number of the satellite
     * @param week            reference Week of the orbit
     * @param orbit           Keplerian orbit in Earth-frozen frame
     * @param time            reference time
     * @param aDot            change rate in semi-major axis (m/s)
     * @param deltaN0         delta of satellite mean motion
     * @param deltaN0Dot      change rate in Δn₀
     * @param iDot            inclination rate (rad/s)
     * @param omegaDot        rate of right ascension (rad/s)
     * @param cuc             amplitude of the cosine harmonic correction term to the argument of latitude
     * @param cus             amplitude of the sine harmonic correction term to the argument of latitude
     * @param crc             amplitude of the cosine harmonic correction term to the orbit radius
     * @param crs             amplitude of the sine harmonic correction term to the orbit radius
     * @param cic             amplitude of the cosine harmonic correction term to the inclination
     * @param cis             amplitude of the sine harmonic correction term to the inclination
     * @param af0             zero-th order clock correction (s)
     * @param af1             first order clock correction (s/s)
     * @param af2             second order clock correction (s/s²)
     * @param tgd             group delay differential TGD for L1-L2 correction
     * @param toc             time of clock
     * @since 14.0
     */
    public FieldGnssOrbitalElements(final double angularVelocity, final int weeksInCycle,
                                    final TimeScales timeScales, final SatelliteSystem system, final String type,
                                    final int prn, final int week, final FieldKeplerianOrbit<T> orbit,
                                    final T time, final T aDot,
                                    final T deltaN0, final T deltaN0Dot,
                                    final T iDot, final T omegaDot,
                                    final T cuc, final T cus,
                                    final T crc, final T crs,
                                    final T cic, final T cis,
                                    final T af0, final T af1, final T af2, final T tgd, final T toc) {

        // system parameters
        this.angularVelocity = angularVelocity;
        this.weeksInCycle    = weeksInCycle;
        this.cycleDuration   = GNSSConstants.GNSS_WEEK_IN_SECONDS * weeksInCycle;
        this.timeScales      = timeScales;
        this.system          = system;
        this.type            = type;

        // satellite identifier
        this.prn             = prn;
        this.week            = week;

        // Keplerian orbit
        this.orbit           = orbit;

        // non-Keplerian elements
        this.time            = time;
        this.aDot            = aDot;
        this.deltaN0         = deltaN0;
        this.deltaN0Dot      = deltaN0Dot;
        this.iDot            = iDot;
        this.omegaDot        = omegaDot;
        this.cuc             = cuc;
        this.cus             = cus;
        this.crc             = crc;
        this.crs             = crs;
        this.cic             = cic;
        this.cis             = cis;

        // clock elements
        this.af0             = af0;
        this.af1             = af1;
        this.af2             = af2;
        this.tgd             = tgd;
        this.toc             = toc;

    }

    /** Constructor from non-field instance.
     * @param field    field to which elements belong
     * @param original regular non-field instance
     */
    protected FieldGnssOrbitalElements(final Field<T> field, final O original) {
        this(original.getAngularVelocity(), original.getWeeksInCycle(),  original.getTimeScales(),
             original.getSystem(), original.getType(), original.getPRN(), original.getWeek(),
             new FieldKeplerianOrbit<>(field, original.getOrbit()),
             field.getZero().newInstance(original.getTime()), field.getZero().newInstance(original.getADot()),
             field.getZero().newInstance(original.getDeltaN0()), field.getZero().newInstance(original.getDeltaN0Dot()),
             field.getZero().newInstance(original.getIDot()), field.getZero().newInstance(original.getOmegaDot()),
             field.getZero().newInstance(original.getCuc()), field.getZero().newInstance(original.getCus()),
             field.getZero().newInstance(original.getCrc()), field.getZero().newInstance(original.getCrs()),
             field.getZero().newInstance(original.getCic()), field.getZero().newInstance(original.getCis()),
             field.getZero().newInstance(original.getAf0()),
             field.getZero().newInstance(original.getAf1()),
             field.getZero().newInstance(original.getAf2()),
             field.getZero().newInstance(original.getTGD()), field.getZero().newInstance(original.getToc()));
     }

    /** Constructor from different field instance.
     * @param <V> type of the old field elements
     * @param original regular non-field instance
     * @param converter for field elements
     */
    protected <V extends CalculusFieldElement<V>> FieldGnssOrbitalElements(final Function<V, T> converter,
                                                                           final FieldGnssOrbitalElements<V, O> original) {
        this(original.getAngularVelocity(), original.getWeeksInCycle(), original.getTimeScales(),
             original.getSystem(), original.getType(), original.getPRN(), original.getWeek(),
             new FieldKeplerianOrbit<>(converter.apply(original.getOrbit().getA()),
                                       converter.apply(original.getOrbit().getE()),
                                       converter.apply(original.getOrbit().getI()),
                                       converter.apply(original.getOrbit().getPerigeeArgument()),
                                       converter.apply(original.getOrbit().getRightAscensionOfAscendingNode()),
                                       converter.apply(original.getOrbit().getMeanAnomaly()),
                                       original.getOrbit().getCachedPositionAngleType(),
                                       original.getOrbit().getCachedPositionAngleType(),
                                       original.getOrbit().getFrame(),
                                       new FieldAbsoluteDate<>(converter.apply(original.getDate().getField().getZero()).getField(),
                                                               original.getDate().toAbsoluteDate()),
                                       converter.apply(original.getOrbit().getMu())),
             converter.apply(original.getTime()), converter.apply(original.getADot()),
             converter.apply(original.getDeltaN0()), converter.apply(original.getDeltaN0Dot()),
             converter.apply(original.getIDot()), converter.apply(original.getOmegaDot()),
             converter.apply(original.getCuc()), converter.apply(original.getCus()),
             converter.apply(original.getCrc()), converter.apply(original.getCrs()),
             converter.apply(original.getCic()), converter.apply(original.getCis()),
             converter.apply(original.getAf0()),
             converter.apply(original.getAf1()),
             converter.apply(original.getAf2()),
             converter.apply(original.getTGD()),
             converter.apply(original.getToc()));
    }

    /** {@inheritDoc} */
    @Override
    public FieldAbsoluteDate<T> getDate() {
        return orbit.getDate();
    }

    /** Create a non-field version of the instance.
     * @return non-field version of the instance
     */
    public abstract O toNonField();

    /** Create another field version of the instance.
     * @param <U>       type of the new field elements
     * @param <G>       type of the orbital elements (field version)
     * @param converter for field elements
     * @return field version of the instance
     */
    public abstract <U extends CalculusFieldElement<U>, G extends FieldGnssOrbitalElements<U, O>>
        G changeField(Function<T, U> converter);

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

    /** Get the message type.
     * @return message type (null if not a navigation message)
     */
    public String getType() {
        return type;
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

    /** Get the reference week of the orbit.
     * @return reference week of the orbit
     */
    public int getWeek() {
        return week;
    }

    /** Get the underlying Keplerian orbit.
     * @return underlying Keplerian orbit
     * @since 14.0
     */
    public FieldKeplerianOrbit<T> getOrbit() {
        return orbit;
    }

    /** Get reference time.
     * @return reference time
     */
    public T getTime() {
        return time;
    }

    /** Get change rate in semi-major axis.
     * @return the change rate in semi-major axis
     * @since 14.0
     */
    public T getADot() {
        return aDot;
    }

    /** Get the delta of satellite mean motion.
     * @return the delta of satellite mean motion
     * @since 14.0
     */
    public T getDeltaN0() {
        return deltaN0;
    }

    /** Get the change rate in Δn₀.
     * @return change rate in Δn₀
     * @since 14.0
     */
    public T getDeltaN0Dot() {
        return deltaN0Dot;
    }

    /** Get rate of inclination angle.
     * @return rate of inclination angle (rad/s)
     */
    public T getIDot() {
        return iDot;
    }

    /** Get rate of right ascension.
     * @return rate of right ascension (rad/s)
     */
    public T getOmegaDot() {
        return omegaDot;
    }

    /** Get amplitude of the cosine harmonic correction term to the argument of latitude.
     * @return amplitude of the cosine harmonic correction term to the argument of latitude (rad)
     */
    public T getCuc() {
        return cuc;
    }

    /** Get amplitude of the sine harmonic correction term to the argument of latitude.
     * @return amplitude of the sine harmonic correction term to the argument of latitude (rad)
     */
    public T getCus() {
        return cus;
    }

    /** Get amplitude of the cosine harmonic correction term to the orbit radius.
     * @return amplitude of the cosine harmonic correction term to the orbit radius (m)
     */
    public T getCrc() {
        return crc;
    }

    /** Get amplitude of the sine harmonic correction term to the orbit radius.
     * @return amplitude of the sine harmonic correction term to the orbit radius (m)
     */
    public T getCrs() {
        return crs;
    }

    /** Get amplitude of the cosine harmonic correction term to the angle of inclination.
     * @return amplitude of the cosine harmonic correction term to the angle of inclination (rad)
     */
    public T getCic() {
        return cic;
    }

    /** Get amplitude of the sine harmonic correction term to the angle of inclination.
     * @return amplitude of the sine harmonic correction term to the angle of inclination (rad)
     */
    public T getCis() {
        return cis;
    }

    /** {@inheritDoc} */
    @Override
    public T getAf0() {
        return af0;
    }

    /** {@inheritDoc} */
    @Override
    public T getAf1() {
        return af1;
    }

    /** {@inheritDoc} */
    @Override
    public T getAf2() {
        return af2;
    }

    /** {@inheritDoc} */
    @Override
    public T getTGD() {
        return tgd;
    }

    /** {@inheritDoc} */
    @Override
    public T getToc() {
        return toc;
    }

    /** Check if elements correspond to a civilian message.
     * @return true if elements correspond to a civilian message
     */
    public boolean isCivilianMessage() {
        return false;
    }

}

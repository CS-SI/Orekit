/* Copyright 2002-2025 CS GROUP
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
import org.hipparchus.util.MathArrays;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitalParameters;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScales;
import org.orekit.utils.ParameterDriver;

import java.util.function.DoubleFunction;
import java.util.function.ToDoubleFunction;

/** This class provides the minimal set of orbital elements needed by
 * {@link org.orekit.propagation.analytical.gnss.GNSSPropagator}.
 * @param <O> type of the orbital elements
 * @author Pascal Parraud
 * @author Luc Maisonobe
 * @since 13.0
 */
public abstract class GNSSOrbitalElements<O extends GNSSOrbitalElements<O>>
    implements OrbitalParameters, GNSSClockElements {

    /** Mean angular velocity of the Earth for the GNSS model. */
    private final double angularVelocity;

    /** Duration of the GNSS cycle in weeks. */
    private final int weeksInCycle;

    /** Known time scales. */
    private final TimeScales timeScales;

    /** Message type (null if not a navigation message). */
    private final String type;

    /** PRN number of the satellite. */
    private final int prn;

    /** GNSS Date.
     * @since 14.0
     */
    private final GNSSDate gnssDate;

    /** Orbit. */
    private final KeplerianOrbit orbit;

    /** Change rate in semi-major axis (m/s).
     * @since 14.0
     */
    private final double aDot;

    /** Delta of satellite mean motion.
     * @since 14.0
     */
    private final double deltaN0;

    /** Change rate in Δn₀.
     * @since 14.0
     */
    private final double deltaN0Dot;

    /** Inclination rate (rad/s). */
    private final double iDot;

    /** Rate of right ascension (rad/s). */
    private final double omegaDot;

    /** Amplitude of the cosine harmonic correction term to the argument of latitude. */
    private final double cuc;

    /** Amplitude of the sine harmonic correction term to the argument of latitude. */
    private final double cus;

    /** Amplitude of the cosine harmonic correction term to the orbit radius. */
    private final double crc;

    /** Amplitude of the sine harmonic correction term to the orbit radius. */
    private final double crs;

    /** Amplitude of the cosine harmonic correction term to the inclination. */
    private final double cic;

    /** Amplitude of the sine harmonic correction term to the inclination. */
    private final double cis;

    /** SV zero-th order clock correction (s). */
    private final double af0;

    /** SV first order clock correction (s/s). */
    private final double af1;

    /** SV second order clock correction (s/s²). */
    private final double af2;

    /** Group delay differential TGD for L1-L2 correction. */
    private final double tgd;

    /** Time Of Clock epoch. */
    private final AbsoluteDate toc;

    /**
     * Creates a new instance.
     *
     * @param angularVelocity mean angular velocity of the Earth for the GNSS model
     * @param weeksInCycle    number of weeks in the GNSS cycle
     * @param timeScales      known time scales
     * @param type            type (null if not a navigation message)
     * @param prn             PRN number of the satellite
     * @param gnssDate        GNSS date (<em>must</em> be consistent with {@code orbit})
     * @param orbit           Keplerian orbit in Earth-frozen frame
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
    protected GNSSOrbitalElements(final double angularVelocity, final int weeksInCycle,
                                  final TimeScales timeScales, final String type,
                                  final int prn, final GNSSDate gnssDate, final KeplerianOrbit orbit,
                                  final double aDot, final double deltaN0, final double deltaN0Dot,
                                  final double iDot, final double omegaDot,
                                  final double cuc, final double cus,
                                  final double crc, final double crs,
                                  final double cic, final double cis,
                                  final double af0, final double af1, final double af2,
                                  final double tgd, final AbsoluteDate toc) {

        // system parameters
        this.angularVelocity = angularVelocity;
        this.weeksInCycle    = weeksInCycle;
        this.timeScales      = timeScales;
        this.type            = type;

        // satellite identifier
        this.prn             = prn;

        // date
        this.gnssDate        = gnssDate;

        // Keplerian orbit
        this.orbit           = orbit;

        // non-Keplerian elements
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

    /** Constructor from field instance.
     * @param <T> type of the field elements
     * @param <A> type of the orbital elements (non-field version)
     * @param original regular field instance
     */
    protected <T extends CalculusFieldElement<T>,
               A extends GNSSOrbitalElements<A>> GNSSOrbitalElements(final FieldGnssOrbitalElements<T, A> original) {
        this(original.getAngularVelocity(), original.getWeeksInCycle(), original.getTimeScales(),
             original.getType(), original.getPrn(),
             original.getGnssDate().getGnssDate(), original.getOrbit().toOrbit(),
             original.getADot().getReal(),
             original.getDeltaN0().getReal(), original.getDeltaN0Dot().getReal(),
             original.getIDot().getReal(), original.getOmegaDot().getReal(),
             original.getCuc().getReal(), original.getCus().getReal(),
             original.getCrc().getReal(), original.getCrs().getReal(),
             original.getCic().getReal(), original.getCis().getReal(),
             original.getAf0().getReal(), original.getAf1().getReal(), original.getAf2().getReal(),
             original.getTgd().getReal(),
             original.getToc() == null ? null : original.getToc().toAbsoluteDate());
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return gnssDate.getDate();
    }

    /** Get the GNSS date.
     * @return GNSS date
     * @since 14.0
     */
    public GNSSDate getGnssDate() {
        return gnssDate;
    }

    /** Create a field version of the instance.
     * @param <T> type of the field elements
     * @param field field
     * @return field version of the instance
     * @since 14.0
     */
    public <T extends CalculusFieldElement<T>>
        FieldGnssOrbitalElements<T, O> toField(Field<T> field) {
        final T zero = field.getZero();
        final T[] parameters = MathArrays.buildArray(field, NonKeplerianDriversFactory.SIZE);
        parameters[NonKeplerianDriversFactory.TIME_INDEX]         = zero.newInstance(getGnssDate().getSecondsInWeek());
        parameters[NonKeplerianDriversFactory.A_DOT_INDEX]        = zero.newInstance(getADot());
        parameters[NonKeplerianDriversFactory.DELTA_N0_INDEX]     = zero.newInstance(getDeltaN0());
        parameters[NonKeplerianDriversFactory.DELTA_N0_DOT_INDEX] = zero.newInstance(getDeltaN0Dot());
        parameters[NonKeplerianDriversFactory.I_DOT_INDEX]        = zero.newInstance(getIDot());
        parameters[NonKeplerianDriversFactory.OMEGA_DOT_INDEX]    = zero.newInstance(getOmegaDot());
        parameters[NonKeplerianDriversFactory.CUC_INDEX]          = zero.newInstance(getCuc());
        parameters[NonKeplerianDriversFactory.CUS_INDEX]          = zero.newInstance(getCus());
        parameters[NonKeplerianDriversFactory.CRC_INDEX]          = zero.newInstance(getCrc());
        parameters[NonKeplerianDriversFactory.CRS_INDEX]          = zero.newInstance(getCrs());
        parameters[NonKeplerianDriversFactory.CIC_INDEX]          = zero.newInstance(getCic());
        parameters[NonKeplerianDriversFactory.CIS_INDEX]          = zero.newInstance(getCis());
        parameters[NonKeplerianDriversFactory.AF0_INDEX]          = zero.newInstance(getAf0());
        parameters[NonKeplerianDriversFactory.AF1_INDEX]          = zero.newInstance(getAf1());
        parameters[NonKeplerianDriversFactory.AF2_INDEX]          = zero.newInstance(getAf2());
        return toField(new FieldKeplerianOrbit<>(field, getOrbit()), parameters, zero::newInstance);
    }

    /** Create another field version of the instance.
     * @param <T>          type of the field elements
     * @param orbit        orbit in the correct gradient field
     * @param nonKeplerian non-Keplerian parameters
     * @param converter    converter for remaining elements
     * @return gradient version of the instance
     * @since 14.0
     */
    public abstract <T extends CalculusFieldElement<T>>
        FieldGnssOrbitalElements<T, O> toField(FieldKeplerianOrbit<T> orbit,
                                               T[] nonKeplerian,
                                               DoubleFunction<T> converter);

    /** Convert TOC.
     * @param <T>   type of the field elements
     * @param orbit orbit in the correct gradient field
     * @return converted Time Of Clock
     * @since 14.0
     */
    protected <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T>
        toFieldToc(final FieldKeplerianOrbit<T> orbit) {
        return getToc() == null ?
               null :
               new FieldAbsoluteDate<>(orbit.getDate().getField(), getToc());
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
        return GNSSConstants.GNSS_WEEK_IN_SECONDS * weeksInCycle;
    }

    /** Get the PRN number of the satellite.
     * @return PRN number of the satellite
     */
    public int getPrn() {
        return prn;
    }

    /** Get the underlying Keplerian orbit.
     * @return underlying Keplerian orbit
     * @since 14.0
     */
    public KeplerianOrbit getOrbit() {
        return orbit;
    }

    /** Get change rate in semi-major axis.
     * @return the change rate in semi-major axis
     * @since 14.0
     */
    public double getADot() {
        return aDot;
    }

    /** Get the delta of satellite mean motion.
     * @return the delta of satellite mean motion
     * @since 14.0
     */
    public double getDeltaN0() {
        return deltaN0;
    }

    /** Get the change rate in Δn₀.
     * @return change rate in Δn₀
     * @since 14.0
     */
    public double getDeltaN0Dot() {
        return deltaN0Dot;
    }

    /** Get rate of inclination angle.
     * @return rate of inclination angle (rad/s)
     */
    public double getIDot() {
        return iDot;
    }

    /** Get rate of right ascension.
     * @return rate of right ascension (rad/s)
     */
    public double getOmegaDot() {
        return omegaDot;
    }

    /** Get amplitude of the cosine harmonic correction term to the argument of latitude.
     * @return amplitude of the cosine harmonic correction term to the argument of latitude (rad)
     */
    public double getCuc() {
        return cuc;
    }

    /** Get amplitude of the sine harmonic correction term to the argument of latitude.
     * @return amplitude of the sine harmonic correction term to the argument of latitude (rad)
     */
    public double getCus() {
        return cus;
    }

    /** Get amplitude of the cosine harmonic correction term to the orbit radius.
     * @return amplitude of the cosine harmonic correction term to the orbit radius (m)
     */
    public double getCrc() {
        return crc;
    }

    /** Get amplitude of the sine harmonic correction term to the orbit radius.
     * @return amplitude of the sine harmonic correction term to the orbit radius (m)
     */
    public double getCrs() {
        return crs;
    }

    /** Get amplitude of the cosine harmonic correction term to the angle of inclination.
     * @return amplitude of the cosine harmonic correction term to the angle of inclination (rad)
     */
    public double getCic() {
        return cic;
    }

    /** Get amplitude of the sine harmonic correction term to the angle of inclination.
     * @return amplitude of the sine harmonic correction term to the angle of inclination (rad)
     */
    public double getCis() {
        return cis;
    }

    /** {@inheritDoc} */
    @Override
    public double getAf0() {
        return af0;
    }

    /** {@inheritDoc} */
    @Override
    public double getAf1() {
        return af1;
    }

    /** {@inheritDoc} */
    @Override
    public double getAf2() {
        return af2;
    }

    /** {@inheritDoc} */
    @Override
    public double getTgd() {
        return tgd;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getToc() {
        return toc;
    }

    /** Check if elements correspond to a civilian message.
     * @return true if elements correspond to a civilian message
     */
    public boolean isCivilianMessage() {
        return false;
    }

    /** Factory for the orbital elements.
     * @param inertial  reference inertial frame
     * @param bodyFixed body fixed frame (will be frozen at {@code date} to build the orbital elements
     * @return factory for the orbital elements
     * @since 14.0
     */
    public GNSSOrbitalElementsFactory<O> factory(final Frame inertial, final Frame bodyFixed) {

        // create base factory
        final GNSSOrbitalElementsFactory<O> factory = baseFactory(inertial, bodyFixed);

        // initialize date
        factory.setWeekAndTime(gnssDate.getWeekNumber(), gnssDate.getSecondsInWeek());

        // initialize the satellite identifier
        factory.setPrn(prn);

        // initialize the orbital parameters
        reset(factory, GNSSOrbitalElementsFactory.SEMI_MAJOR_AXIS,     KeplerianOrbit::getA);
        reset(factory, GNSSOrbitalElementsFactory.ECCENTRICITY,        KeplerianOrbit::getE);
        reset(factory, GNSSOrbitalElementsFactory.INCLINATION,         KeplerianOrbit::getI);
        reset(factory, GNSSOrbitalElementsFactory.ARGUMENT_OF_PERIGEE, KeplerianOrbit::getPerigeeArgument);
        reset(factory, GNSSOrbitalElementsFactory.NODE_LONGITUDE,      KeplerianOrbit::getRightAscensionOfAscendingNode);
        reset(factory, GNSSOrbitalElementsFactory.MEAN_ANOMALY,        KeplerianOrbit::getMeanAnomaly);

        // initialize the non-Keplerian elements
        reset(factory.getADotDriver(),       aDot);
        reset(factory.getDeltaN0Driver(),    deltaN0);
        reset(factory.getDeltaN0DotDriver(), deltaN0);
        reset(factory.getIDotDriver(),       iDot);
        reset(factory.getOmegaDotDriver(),   omegaDot);
        reset(factory.getCucDriver(),        cuc);
        reset(factory.getCusDriver(),        cus);
        reset(factory.getCrcDriver(),        crc);
        reset(factory.getCrsDriver(),        crs);
        reset(factory.getCicDriver(),        cic);
        reset(factory.getCisDriver(),        cis);

        // initialize the clock elements
        reset(factory.getAf0Driver(),        af0);
        reset(factory.getAf1Driver(),        af1);
        reset(factory.getAf2Driver(),        af2);
        factory.setTgd(tgd);
        factory.setToc(toc);

        return factory;

    }

    /** Factory for the orbital elements.
     * @param inertial  reference inertial frame
     * @param bodyFixed body fixed frame (will be frozen at {@code date} to build the orbital elements
     * @return factory for the orbital elements
     * @since 14.0
     */
    protected abstract GNSSOrbitalElementsFactory<O> baseFactory(Frame inertial, Frame bodyFixed);

    /** Set a parameter.
     * @param factory factory
     * @param name name of the parameter driver
     * @param getter value getter
     */
    private void reset(final GNSSOrbitalElementsFactory<O> factory, final String name,
                       final ToDoubleFunction<KeplerianOrbit> getter) {
        reset(factory.getOrbitalParametersDrivers().findByName(name), getter.applyAsDouble(orbit));
    }

    /** Set a parameter.
     * @param driver parameter driver
     * @param value new value
     */
    private void reset(final ParameterDriver driver, final double value) {
        driver.setValue(value);
        driver.setReferenceValue(value);
    }

}

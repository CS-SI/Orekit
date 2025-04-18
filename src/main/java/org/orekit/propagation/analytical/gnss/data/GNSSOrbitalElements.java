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
import org.hipparchus.util.FastMath;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.propagation.analytical.gnss.GNSSPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScales;
import org.orekit.time.TimeStamped;
import org.orekit.utils.ParameterDriver;

/** This class provides the minimal set of orbital elements needed by the {@link GNSSPropagator}.
 * <p>
 * The parameters are split in two groups: Keplerian orbital parameters and non-Keplerian
 * evolution parameters. All parameters can be updated as they are all instances of
 * {@link ParameterDriver}. Only the non-Keplerian parameters are returned in the
 * {@link #getParametersDrivers()} method, the Keplerian orbital parameters must
 * be accessed independently. These groups ensure proper separate computation of
 * state transition matrix and Jacobian matrix by {@link GNSSPropagator}.
 * </p>
 * @param <O> type of the orbital elements
 * @since 13.0
 * @author Pascal Parraud
 * @author Luc Maisonobe
*/
public abstract class GNSSOrbitalElements<O extends GNSSOrbitalElements<O>>
    extends GNSSOrbitalElementsDriversProvider
    implements TimeStamped {

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

    /** Earth's universal gravitational parameter. */
    private final double mu;

    /** Reference epoch. */
    private AbsoluteDate date;

    /** Semi-Major Axis (m). */
    private final ParameterDriver smaDriver;

    /** Eccentricity. */
    private final ParameterDriver eccDriver;

    /** Inclination angle at reference time (rad). */
    private final ParameterDriver i0Driver;

    /** Argument of perigee (rad). */
    private final ParameterDriver aopDriver;

    /** Longitude of ascending node of orbit plane at weekly epoch (rad). */
    private final ParameterDriver om0Driver;

    /** Mean anomaly at reference time (rad). */
    private final ParameterDriver anomDriver;

    /**
     * Constructor.
     * @param mu              Earth's universal gravitational parameter
     * @param angularVelocity mean angular velocity of the Earth for the GNSS model
     * @param weeksInCycle    number of weeks in the GNSS cycle
     * @param timeScales      known time scales
     * @param system          satellite system to consider for interpreting week number
     *                        (may be different from real system, for example in Rinex nav, weeks
     *                        are always according to GPS)
     */
    protected GNSSOrbitalElements(final double mu, final double angularVelocity, final int weeksInCycle,
                                  final TimeScales timeScales, final SatelliteSystem system) {

        super(angularVelocity, weeksInCycle, timeScales, system);

        // immutable field
        this.mu         = mu;

        // fields controlled by parameter drivers for Keplerian orbital elements
        this.smaDriver  = createDriver(SEMI_MAJOR_AXIS);
        this.eccDriver  = createDriver(ECCENTRICITY);
        this.i0Driver   = createDriver(INCLINATION);
        this.aopDriver  = createDriver(ARGUMENT_OF_PERIGEE);
        this.om0Driver  = createDriver(NODE_LONGITUDE);
        this.anomDriver = createDriver(MEAN_ANOMALY);

    }

    /** Constructor from field instance.
     * @param <T> type of the field elements
     * @param <A> type of the orbital elements (non-field version)
     * @param original regular field instance
     */
    protected <T extends CalculusFieldElement<T>,
               A extends GNSSOrbitalElements<A>> GNSSOrbitalElements(final FieldGnssOrbitalElements<T, A> original) {
        this(original.getMu().getReal(), original.getAngularVelocity(), original.getWeeksInCycle(),
             original.getTimeScales(), original.getSystem());

        // non-Keperian parameters
        setPRN(original.getPRN());
        setWeek(original.getWeek());
        setTime(original.getTime());
        setIDot(original.getIDot());
        setOmegaDot(original.getOmegaDot());
        setCuc(original.getCuc());
        setCus(original.getCus());
        setCrc(original.getCrc());
        setCrs(original.getCrs());
        setCic(original.getCic());
        setCis(original.getCis());

        // Keplerian orbital elements
        setSma(original.getSma().getReal());
        setE(original.getE().getReal());
        setI0(original.getI0().getReal());
        setPa(original.getPa().getReal());
        setOmega0(original.getOmega0().getReal());
        setM0(original.getM0().getReal());

        // copy selection settings
        copySelectionSettings(original);

    }

    /** Create a field version of the instance.
     * @param <T> type of the field elements
     * @param <F> type of the orbital elements (field version)
     * @param field field to which elements belong
     * @return field version of the instance
     */
    public abstract <T extends CalculusFieldElement<T>, F extends FieldGnssOrbitalElements<T, O>>
        F toField(Field<T> field);

    /** {@inheritDoc} */
    protected void setGnssDate(final GNSSDate gnssDate) {
        this.date = gnssDate.getDate();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    /** Get the Earth's universal gravitational parameter.
     * @return the Earth's universal gravitational parameter
     */
    public double getMu() {
        return mu;
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

    /** Getter for the change rate in semi-major axis.
     * <p>
     * This value is non-zero only in civilian navigation messages
     * </p>
     * @return the change rate in semi-major axis
     * @since 13.0
     */
    public double getADot() {
        return 0;
    }

    /** Get the computed mean motion n₀.
     * @return the computed mean motion n₀ (rad/s)
     * @since 13.0
     */
    public double getMeanMotion0() {
        final double absA = FastMath.abs(getSma());
        return FastMath.sqrt(getMu() / absA) / absA;
    }

    /** Getter for the delta of satellite mean motion.
     * <p>
     * This value is non-zero only in navigation messages
     * </p>
     * @return delta of satellite mean motion
     * @since 13.0
     */
    public double getDeltaN0() {
        return 0;
    }

    /** Getter for change rate in Δn₀.
     * <p>
     * This value is non-zero only in civilian navigation messages
     * </p>
     * @return change rate in Δn₀
     * @since 13.0
     */
    public double getDeltaN0Dot() {
        return 0;
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

}

/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.propagation;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.interpolation.FieldHermiteInterpolator;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.attitudes.FieldLofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.orbits.FieldOrbit;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.TimeStampedFieldPVCoordinates;


/** This class is the representation of a complete state holding orbit, attitude
 * and mass information at a given date.
 *
 * <p>It contains an {@link FieldOrbit orbital state} at a current
 * {@link FieldAbsoluteDate} both handled by an {@link FieldOrbit}, plus the current
 * mass and attitude. FieldOrbitand state are guaranteed to be consistent in terms
 * of date and reference frame. The spacecraft state may also contain additional
 * states, which are simply named double arrays which can hold any user-defined
 * data.
 * </p>
 * <p>
 * The state can be slightly shifted to close dates. This shift is based on
 * a simple keplerian model for orbit, a linear extrapolation for attitude
 * taking the spin rate into account and no mass change. It is <em>not</em>
 * intended as a replacement for proper orbit and attitude propagation but
 * should be sufficient for either small time shifts or coarse accuracy.
 * </p>
 * <p>
 * The instance {@code FieldSpacecraftState} is guaranteed to be immutable.
 * </p>
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 */
public class FieldSpacecraftState <T extends RealFieldElement<T>> {

    /** Default mass. */
    private static final double DEFAULT_MASS = 1000.0;

    /**
     * tolerance on date comparison in {@link #checkConsistency(FieldOrbit<T>, FieldAttitude<T>)}. 100 ns
     * corresponds to sub-mm accuracy at LEO orbital velocities.
     */
    private static final double DATE_INCONSISTENCY_THRESHOLD = 100e-9;

    /** Orbital state. */
    private final FieldOrbit<T> orbit;

    /** FieldAttitude<T>. */
    private final FieldAttitude<T> attitude;

    /** Current mass (kg). */
    private final T mass;

    /** Additional states. */
    private final Map<String, T[]> additional;

    /** Build a spacecraft state from orbit only.
     * <p>FieldAttitude<T> and mass are set to unspecified non-null arbitrary values.</p>
     * @param orbit the orbit
     * @exception OrekitException if default attitude cannot be computed
     */
    public FieldSpacecraftState(final FieldOrbit<T> orbit)
        throws OrekitException {
        this(orbit,
             new FieldLofOffset<T>(orbit.getFrame(), LOFType.VVLH, orbit.getA().getField()).getAttitude(orbit, orbit.getDate(), orbit.getFrame()),
             orbit.getA().getField().getZero().add(DEFAULT_MASS), null);
    }

    /** Build a spacecraft state from orbit and attitude provider.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param orbit the orbit
     * @param attitude attitude
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final FieldAttitude<T> attitude)
        throws IllegalArgumentException {
        this(orbit, attitude, orbit.getA().getField().getZero().add(DEFAULT_MASS), null);
    }

    /** Create a new instance from orbit and mass.
     * <p>FieldAttitude<T> law is set to an unspecified default attitude.</p>
     * @param orbit the orbit
     * @param mass the mass (kg)
     * @exception OrekitException if default attitude cannot be computed
     */
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final T mass)
        throws OrekitException {
        this(orbit,
             new FieldLofOffset<T>(orbit.getFrame(), LOFType.VVLH, orbit.getA().getField()).getAttitude(orbit, orbit.getDate(), orbit.getFrame()),
             mass, null);
    }

    /** Build a spacecraft state from orbit, attitude provider and mass.
     * @param orbit the orbit
     * @param attitude attitude
     * @param mass the mass (kg)
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final FieldAttitude<T> attitude, final T mass)
        throws IllegalArgumentException {
        this(orbit, attitude, mass, null);
    }

    /** Build a spacecraft state from orbit only.
     * <p>FieldAttitude<T> and mass are set to unspecified non-null arbitrary values.</p>
     * @param orbit the orbit
     * @param additional additional states
     * @exception OrekitException if default attitude cannot be computed
     */
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final Map<String, T[]> additional)
        throws OrekitException {
        this(orbit,
             new FieldLofOffset<T>(orbit.getFrame(), LOFType.VVLH, orbit.getA().getField()).getAttitude(orbit, orbit.getDate(), orbit.getFrame()),
             orbit.getA().getField().getZero().add(DEFAULT_MASS), additional);
    }

    /** Build a spacecraft state from orbit and attitude provider.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param orbit the orbit
     * @param attitude attitude
     * @param additional additional states
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final FieldAttitude<T> attitude, final Map<String, T[]> additional)
        throws IllegalArgumentException {
        this(orbit, attitude, orbit.getA().getField().getZero().add(DEFAULT_MASS), additional);
    }

    /** Create a new instance from orbit and mass.
     * <p>FieldAttitude<T> law is set to an unspecified default attitude.</p>
     * @param orbit the orbit
     * @param mass the mass (kg)
     * @param additional additional states
     * @exception OrekitException if default attitude cannot be computed
     */
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final T mass, final Map<String, T[]> additional)
        throws OrekitException {
        this(orbit,
             new FieldLofOffset<T>(orbit.getFrame(), LOFType.VVLH, mass.getField()).getAttitude(orbit, orbit.getDate(), orbit.getFrame()),
             mass, additional);
    }

    /** Build a spacecraft state from orbit, attitude provider and mass.
     * @param orbit the orbit
     * @param attitude attitude
     * @param mass the mass (kg)
     * @param additional additional states (may be null if no additional states are available)
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final FieldAttitude<T> attitude,
                           final T mass, final Map<String, T[]> additional)
        throws IllegalArgumentException {
        checkConsistency(orbit, attitude);
        this.orbit      = orbit;
        this.attitude   = attitude;
        this.mass       = mass;


        if (additional == null) {
            this.additional = Collections.emptyMap();
        } else {

            this.additional = new HashMap<String, T[]>(additional.size());
            for (final Map.Entry<String, T[]> entry : additional.entrySet()) {
                this.additional.put(entry.getKey(), entry.getValue().clone());
            }
        }
    }

    /** Add an additional state.
     * <p>
     * {@link FieldSpacecraftState SpacecraftState} instances are immutable,
     * so this method does <em>not</em> change the instance, but rather
     * creates a new instance, which has the same orbit, attitude, mass
     * and additional states as the original instance, except it also
     * has the specified state. If the original instance already had an
     * additional state with the same name, it will be overridden. If it
     * did not have any additional state with that name, the new instance
     * will have one more additional state than the original instance.
     * </p>
     * @param name name of the additional state
     * @param value value of the additional state
     * @return a new instance, with the additional state added
     * @see #hasAdditionalState(String)
     * @see #getAdditionalState(String)
     * @see #getAdditionalStates()
     */
    @SafeVarargs
    public final FieldSpacecraftState<T> addAdditionalState(final String name, final T ... value) {
        final Map<String, T[]> newMap = new HashMap<String, T[]>(additional.size() + 1);
        newMap.putAll(additional);
        newMap.put(name, value.clone());
        return new FieldSpacecraftState<T>(orbit, attitude, mass, newMap);
    }

    /** Check orbit and attitude dates are equal.
     * @param orbitN the orbit
     * @param attitudeN attitude
     * @exception IllegalArgumentException if orbit and attitude dates
     * are not equal
     */
    private void checkConsistency(final FieldOrbit<T> orbitN, final FieldAttitude<T> attitudeN)
        throws IllegalArgumentException {
        if (orbitN.getDate().durationFrom(attitudeN.getDate()).abs().getReal() >
            DATE_INCONSISTENCY_THRESHOLD) {

            throw new OrekitIllegalArgumentException(OrekitMessages.ORBIT_AND_ATTITUDE_DATES_MISMATCH,
                                                     orbitN.getDate(), attitudeN.getDate());
        }

        if (orbitN.getFrame() != attitudeN.getReferenceFrame()) {
            throw new OrekitIllegalArgumentException(OrekitMessages.FRAMES_MISMATCH,
                                                     orbitN.getFrame().getName(),
                                                     attitudeN.getReferenceFrame().getName());
        }
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple keplerian model for orbit, a linear extrapolation for attitude
     * taking the spin rate into account and neither mass nor additional states
     * changes. It is <em>not</em> intended as a replacement for proper orbit
     * and attitude propagation but should be sufficient for small time shifts
     * or coarse accuracy.
     * </p>
     * <p>
     * As a rough order of magnitude, the following table shows the interpolation
     * errors obtained between this simple shift method and an {@link
     * org.orekit.propagation.analytical.EcksteinHechlerPropagator Eckstein-Heschler
     * propagator} for an 800km altitude nearly circular polar Earth orbit with
     * {@link org.orekit.attitudes.BodyCenterPointing body center pointing}. Beware
     * that these results may be different for other orbits.
     * </p>
     * <table border="1" cellpadding="5">
     * <tr bgcolor="#ccccff"><th>interpolation time (s)</th>
     * <th>position error (m)</th><th>velocity error (m/s)</th>
     * <th>attitude error (&deg;)</th></tr>
     * <tr><td bgcolor="#eeeeff"> 60</td><td>  20</td><td>1</td><td>0.001</td></tr>
     * <tr><td bgcolor="#eeeeff">120</td><td> 100</td><td>2</td><td>0.002</td></tr>
     * <tr><td bgcolor="#eeeeff">300</td><td> 600</td><td>4</td><td>0.005</td></tr>
     * <tr><td bgcolor="#eeeeff">600</td><td>2000</td><td>6</td><td>0.008</td></tr>
     * <tr><td bgcolor="#eeeeff">900</td><td>4000</td><td>6</td><td>0.010</td></tr>
     * </table>
     * @param kk time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     * except for the mass which stay unchanged
     */
    public FieldSpacecraftState<T> shiftedBy(final T kk) {
        return new FieldSpacecraftState<T>(orbit.shiftedBy(kk), attitude.shiftedBy(kk),
                                   mass, additional);
    }

    /** Get an interpolated instance.
     * <p>
     * The additional states that are interpolated are the ones already present
     * in the instance. The sample instances must therefore have at least the same
     * additional states has the instance. They may have more additional states,
     * but the extra ones will be ignored.
     * </p>
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only
     * with small samples (about 10-20 points) in order to avoid <a
     * href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a>
     * and numerical problems (including NaN appearing).
     * </p>
     * @param date interpolation date
     * @param sample sample points on which interpolation should be done
     * @return a new instance, interpolated at specified date
     * @exception OrekitException if the number of point is too small for interpolating
     */
    public FieldSpacecraftState<T> interpolate(final FieldAbsoluteDate<T> date,
                                               final Collection<FieldSpacecraftState<T>> sample)
        throws OrekitException {

        // prepare interpolators
        final List<FieldOrbit<T>> orbits = new ArrayList<FieldOrbit<T>>(sample.size());
        final List<FieldAttitude<T>> attitudes = new ArrayList<FieldAttitude<T>>(sample.size());
        final FieldHermiteInterpolator<T> massInterpolator = new FieldHermiteInterpolator<T>();
        final Map<String, FieldHermiteInterpolator<T>> additionalInterpolators =
                new HashMap<String, FieldHermiteInterpolator<T>>(additional.size());
        for (final String name : additional.keySet()) {
            additionalInterpolators.put(name, new FieldHermiteInterpolator<T>());
        }

        // extract sample data
        for (final FieldSpacecraftState<T> state : sample) {
            final T deltaT = state.getDate().durationFrom(date);
            orbits.add(state.getOrbit());
            attitudes.add(state.getAttitude());
            final T[] mm = MathArrays.buildArray(orbit.getA().getField(), 1);
            mm[0] = state.getMass();
            massInterpolator.addSamplePoint(deltaT,
                                            mm);
            for (final Map.Entry<String, FieldHermiteInterpolator<T>> entry : additionalInterpolators.entrySet()) {
                entry.getValue().addSamplePoint(deltaT, state.getAdditionalState(entry.getKey()));
            }
        }

        // perform interpolations
        final FieldOrbit<T> interpolatedOrbit       = orbit.interpolate(date, orbits);
        final FieldAttitude<T> interpolatedAttitude = attitude.interpolate(date, attitudes);
        final T interpolatedMass       = massInterpolator.value(orbit.getA().getField().getZero())[0];
        final Map<String, T[]> interpolatedAdditional;
        if (additional.isEmpty()) {
            interpolatedAdditional = null;
        } else {
            interpolatedAdditional = new HashMap<String, T[]>(additional.size());
            for (final Map.Entry<String, FieldHermiteInterpolator<T>> entry : additionalInterpolators.entrySet()) {
                interpolatedAdditional.put(entry.getKey(), entry.getValue().value(orbit.getA().getField().getZero()));
            }
        }

        // create the complete interpolated state
        return new FieldSpacecraftState<T>(interpolatedOrbit, interpolatedAttitude,
                                   interpolatedMass, interpolatedAdditional);

    }

    /** Gets the current orbit.
     * @return the orbit
     */
    public FieldOrbit<T> getOrbit() {
        return orbit;
    }

    /** Get the date.
     * @return date
     */
    public FieldAbsoluteDate<T> getDate() {
        return orbit.getDate();
    }

    /** Get the inertial frame.
     * @return the frame
     */
    public Frame getFrame() {
        return orbit.getFrame();
    }

    /** Check if an additional state is available.
     * @param name name of the additional state
     * @return true if the additional state is available
     * @see #addAdditionalState(String, RealFieldElement...)
     * @see #getAdditionalState(String)
     * @see #getAdditionalStates()
     */
    public boolean hasAdditionalState(final String name) {
        return additional.containsKey(name);
    }

    /** Check if two instances have the same set of additional states available.
     * <p>
     * Only the names and dimensions of the additional states are compared,
     * not their values.
     * </p>
     * @param state state to compare to instance
     * @exception OrekitException if either instance or state supports an additional
     * state not supported by the other one
     * @exception MathIllegalArgumentException if an additional state does not have
     * the same dimension in both states
     */
    public void ensureCompatibleAdditionalStates(final FieldSpacecraftState<T> state)
        throws OrekitException, MathIllegalArgumentException {

        // check instance additional states is a subset of the other one
        for (final Map.Entry<String, T[]> entry : additional.entrySet()) {
            final T[] other = state.additional.get(entry.getKey());
            if (other == null) {
                throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_STATE,
                                          entry.getKey());
            }
            if (other.length != entry.getValue().length) {
                throw new MathIllegalStateException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                                    other.length, entry.getValue().length);
            }
        }

        if (state.additional.size() > additional.size()) {
            // the other state has more additional states
            for (final String name : state.additional.keySet()) {
                if (!additional.containsKey(name)) {
                    throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_STATE,
                                              name);
                }
            }
        }

    }

    /** Get an additional state.
     * @param name name of the additional state
     * @return value of the additional state
     * @exception OrekitException if no additional state with that name exists
     * @see #addAdditionalState(String, RealFieldElement...)
     * @see #hasAdditionalState(String)
     * @see #getAdditionalStates()
     */
    public T[] getAdditionalState(final String name) throws OrekitException {
        if (!additional.containsKey(name)) {
            throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_STATE, name);
        }
        return additional.get(name).clone();
    }

    /** Get an unmodifiable map of additional states.
     * @return unmodifiable map of additional states
     * @see #addAdditionalState(String, RealFieldElement...)
     * @see #hasAdditionalState(String)
     * @see #getAdditionalState(String)
     */
    public Map<String, T[]> getAdditionalStates() {
        return Collections.unmodifiableMap(additional);
    }

//  TODO: field implementation
//    /** Compute the transform from orbite/attitude reference frame to spacecraft frame.
//     * <p>The spacecraft frame origin is at the point defined by the orbit,
//     * and its orientation is defined by the attitude.</p>
//     * @return transform from specified frame to current spacecraft frame
//     */
////    public Transform toTransform() {
//        final FieldAbsoluteDate<T> date = orbit.getDate();
//        return new Transform(date.toAbsoluteDate(),
//        TODO                     new Transform(date.toAbsoluteDate(), orbit.getFieldPVCoordinates().negate()),
//                             new Transform(date.toAbsoluteDate(), attitude.getOrientation()));
//    }

    /** Get the central attraction coefficient.
     * @return mu central attraction coefficient (m^3/s^2)
     */
    public double getMu() {
        return orbit.getMu();
    }

    /** Get the keplerian period.
     * <p>The keplerian period is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return keplerian period in seconds
     */
    public T getKeplerianPeriod() {
        return orbit.getKeplerianPeriod();
    }

    /** Get the keplerian mean motion.
     * <p>The keplerian mean motion is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return keplerian mean motion in radians per second
     */
    public T getKeplerianMeanMotion() {
        return orbit.getKeplerianMeanMotion();
    }

    /** Get the semi-major axis.
     * @return semi-major axis (m)
     */
    public T getA() {
        return orbit.getA();
    }

    /** Get the first component of the eccentricity vector (as per equinoctial parameters).
     * @return e cos(ω + Ω), first component of eccentricity vector
     * @see #getE()
     */
    public T getEquinoctialEx() {
        return orbit.getEquinoctialEx();
    }

    /** Get the second component of the eccentricity vector (as per equinoctial parameters).
     * @return e sin(ω + Ω), second component of the eccentricity vector
     * @see #getE()
     */
    public T getEquinoctialEy() {
        return orbit.getEquinoctialEy();
    }

    /** Get the first component of the inclination vector (as per equinoctial parameters).
     * @return tan(i/2) cos(Ω), first component of the inclination vector
     * @see #getI()
     */
    public T getHx() {
        return orbit.getHx();
    }

    /** Get the second component of the inclination vector (as per equinoctial parameters).
     * @return tan(i/2) sin(Ω), second component of the inclination vector
     * @see #getI()
     */
    public T getHy() {
        return orbit.getHy();
    }

    /** Get the true latitude argument (as per equinoctial parameters).
     * @return v + ω + Ω true latitude argument (rad)
     * @see #getLE()
     * @see #getLM()
     */
    public T getLv() {
        return orbit.getLv();
    }

    /** Get the eccentric latitude argument (as per equinoctial parameters).
     * @return E + ω + Ω eccentric latitude argument (rad)
     * @see #getLv()
     * @see #getLM()
     */
    public T getLE() {
        return orbit.getLE();
    }

    /** Get the mean latitude argument (as per equinoctial parameters).
     * @return M + ω + Ω mean latitude argument (rad)
     * @see #getLv()
     * @see #getLE()
     */
    public T getLM() {
        return orbit.getLM();
    }

    // Additional orbital elements

    /** Get the eccentricity.
     * @return eccentricity
     * @see #getEquinoctialEx()
     * @see #getEquinoctialEy()
     */
    public T getE() {
        return orbit.getE();
    }

    /** Get the inclination.
     * @return inclination (rad)
     * @see #getHx()
     * @see #getHy()
     */
    public T getI() {
        return orbit.getI();
    }

    /** Get the {@link TimeStampedFieldPVCoordinates} in orbit definition frame.
     * Compute the position and velocity of the satellite. This method caches its
     * results, and recompute them only when the method is called with a new value
     * for mu. The result is provided as a reference to the internally cached
     * {@link TimeStampedFieldPVCoordinates}, so the caller is responsible to copy it in a separate
     * {@link TimeStampedFieldPVCoordinates} if it needs to keep the value for a while.
     * @return pvCoordinates in orbit definition frame
     */
    public TimeStampedFieldPVCoordinates<T> getPVCoordinates() {
        return orbit.getPVCoordinates();
    }

    /** Get the {@link TimeStampedFieldPVCoordinates} in given output frame.
     * Compute the position and velocity of the satellite. This method caches its
     * results, and recompute them only when the method is called with a new value
     * for mu. The result is provided as a reference to the internally cached
     * {@link TimeStampedFieldPVCoordinates}, so the caller is responsible to copy it in a separate
     * {@link TimeStampedFieldPVCoordinates} if it needs to keep the value for a while.
     * @param outputFrame frame in which coordinates should be defined
     * @return pvCoordinates in orbit definition frame
     * @exception OrekitException if the transformation between frames cannot be computed
     */
    public TimeStampedFieldPVCoordinates<T> getPVCoordinates(final Frame outputFrame)
        throws OrekitException {
        return orbit.getPVCoordinates(outputFrame);
    }

    /** Get the attitude.
     * @return the attitude.
     */
    public FieldAttitude<T> getAttitude() {
        return attitude;
    }

    /** Gets the current mass.
     * @return the mass (kg)
     */
    public T getMass() {
        return mass;
    }

    /**To convert a FieldSpacecraftState instance into a SpacecraftState instance.
     *
     * @return SpacecraftState instance with the same properties
     */
    public SpacecraftState toSpacecraftState() {
        return new SpacecraftState(orbit.toOrbit(), attitude.toAttitude(), mass.getReal());
    }

}

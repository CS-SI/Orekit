/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.hipparchus.analysis.interpolation.HermiteInterpolator;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitIllegalStateException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterpolable;
import org.orekit.time.TimeShiftable;
import org.orekit.time.TimeStamped;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** This class is the representation of a complete state holding orbit, attitude
 * and mass information at a given date.
 *
 * <p>It contains an {@link Orbit orbital state} at a current
 * {@link AbsoluteDate} both handled by an {@link Orbit}, plus the current
 * mass and attitude. Orbit and state are guaranteed to be consistent in terms
 * of date and reference frame. The spacecraft state may also contain additional
 * states, which are simply named double arrays which can hold any user-defined
 * data.
 * </p>
 * <p>
 * The state can be slightly shifted to close dates. This shift is based on
 * a simple Keplerian model for orbit, a linear extrapolation for attitude
 * taking the spin rate into account and no mass change. It is <em>not</em>
 * intended as a replacement for proper orbit and attitude propagation but
 * should be sufficient for either small time shifts or coarse accuracy.
 * </p>
 * <p>
 * The instance <code>SpacecraftState</code> is guaranteed to be immutable.
 * </p>
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 */
public class SpacecraftState
    implements TimeStamped, TimeShiftable<SpacecraftState>, TimeInterpolable<SpacecraftState>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130407L;

    /** Default mass. */
    private static final double DEFAULT_MASS = 1000.0;

    /**
     * tolerance on date comparison in {@link #checkConsistency(Orbit, Attitude)}. 100 ns
     * corresponds to sub-mm accuracy at LEO orbital velocities.
     */
    private static final double DATE_INCONSISTENCY_THRESHOLD = 100e-9;

    /** Orbital state. */
    private final Orbit orbit;

    /** Trajectory state, when it is not an orbit. */
    private final AbsolutePVCoordinates absPva;

    /** Attitude. */
    private final Attitude attitude;

    /** Current mass (kg). */
    private final double mass;

    /** Additional states. */
    private final Map<String, double[]> additional;

    /** Build a spacecraft state from orbit only.
     * <p>Attitude and mass are set to unspecified non-null arbitrary values.</p>
     * @param orbit the orbit
     */
    public SpacecraftState(final Orbit orbit) {
        this(orbit,
             new LofOffset(orbit.getFrame(), LOFType.VVLH).getAttitude(orbit, orbit.getDate(), orbit.getFrame()),
             DEFAULT_MASS, null);
    }

    /** Build a spacecraft state from orbit and attitude provider.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param orbit the orbit
     * @param attitude attitude
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public SpacecraftState(final Orbit orbit, final Attitude attitude)
        throws IllegalArgumentException {
        this(orbit, attitude, DEFAULT_MASS, null);
    }

    /** Create a new instance from orbit and mass.
     * <p>Attitude law is set to an unspecified default attitude.</p>
     * @param orbit the orbit
     * @param mass the mass (kg)
     */
    public SpacecraftState(final Orbit orbit, final double mass) {
        this(orbit,
             new LofOffset(orbit.getFrame(), LOFType.VVLH).getAttitude(orbit, orbit.getDate(), orbit.getFrame()),
             mass, null);
    }

    /** Build a spacecraft state from orbit, attitude provider and mass.
     * @param orbit the orbit
     * @param attitude attitude
     * @param mass the mass (kg)
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public SpacecraftState(final Orbit orbit, final Attitude attitude, final double mass)
        throws IllegalArgumentException {
        this(orbit, attitude, mass, null);
    }

    /** Build a spacecraft state from orbit only.
     * <p>Attitude and mass are set to unspecified non-null arbitrary values.</p>
     * @param orbit the orbit
     * @param additional additional states
     */
    public SpacecraftState(final Orbit orbit, final Map<String, double[]> additional) {
        this(orbit,
             new LofOffset(orbit.getFrame(), LOFType.VVLH).getAttitude(orbit, orbit.getDate(), orbit.getFrame()),
             DEFAULT_MASS, additional);
    }

    /** Build a spacecraft state from orbit and attitude provider.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param orbit the orbit
     * @param attitude attitude
     * @param additional additional states
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public SpacecraftState(final Orbit orbit, final Attitude attitude, final Map<String, double[]> additional)
        throws IllegalArgumentException {
        this(orbit, attitude, DEFAULT_MASS, additional);
    }

    /** Create a new instance from orbit and mass.
     * <p>Attitude law is set to an unspecified default attitude.</p>
     * @param orbit the orbit
     * @param mass the mass (kg)
     * @param additional additional states
     */
    public SpacecraftState(final Orbit orbit, final double mass, final Map<String, double[]> additional) {
        this(orbit,
             new LofOffset(orbit.getFrame(), LOFType.VVLH).getAttitude(orbit, orbit.getDate(), orbit.getFrame()),
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
    public SpacecraftState(final Orbit orbit, final Attitude attitude,
                           final double mass, final Map<String, double[]> additional)
        throws IllegalArgumentException {
        checkConsistency(orbit, attitude);
        this.orbit      = orbit;
        this.absPva     = null;
        this.attitude   = attitude;
        this.mass       = mass;
        if (additional == null) {
            this.additional = Collections.emptyMap();
        } else {
            this.additional = new HashMap<String, double[]>(additional.size());
            for (final Map.Entry<String, double[]> entry : additional.entrySet()) {
                this.additional.put(entry.getKey(), entry.getValue().clone());
            }
        }
    }



    /** Build a spacecraft state from orbit only.
     * <p>Attitude and mass are set to unspecified non-null arbitrary values.</p>
     * @param absPva position-velocity-acceleration
     */
    public SpacecraftState(final AbsolutePVCoordinates absPva) {
        this(absPva,
             new LofOffset(absPva.getFrame(), LOFType.VVLH).getAttitude(absPva, absPva.getDate(), absPva.getFrame()),
             DEFAULT_MASS, null);
    }

    /** Build a spacecraft state from orbit and attitude provider.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param absPva position-velocity-acceleration
     * @param attitude attitude
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public SpacecraftState(final AbsolutePVCoordinates absPva, final Attitude attitude)
        throws IllegalArgumentException {
        this(absPva, attitude, DEFAULT_MASS, null);
    }

    /** Create a new instance from orbit and mass.
     * <p>Attitude law is set to an unspecified default attitude.</p>
     * @param absPva position-velocity-acceleration
     * @param mass the mass (kg)
     */
    public SpacecraftState(final AbsolutePVCoordinates absPva, final double mass) {
        this(absPva,
             new LofOffset(absPva.getFrame(), LOFType.VVLH).getAttitude(absPva, absPva.getDate(), absPva.getFrame()),
             mass, null);
    }

    /** Build a spacecraft state from orbit, attitude provider and mass.
     * @param absPva position-velocity-acceleration
     * @param attitude attitude
     * @param mass the mass (kg)
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public SpacecraftState(final AbsolutePVCoordinates absPva, final Attitude attitude, final double mass)
        throws IllegalArgumentException {
        this(absPva, attitude, mass, null);
    }

    /** Build a spacecraft state from orbit only.
     * <p>Attitude and mass are set to unspecified non-null arbitrary values.</p>
     * @param absPva position-velocity-acceleration
     * @param additional additional states
     */
    public SpacecraftState(final AbsolutePVCoordinates absPva, final Map<String, double[]> additional) {
        this(absPva,
             new LofOffset(absPva.getFrame(), LOFType.VVLH).getAttitude(absPva, absPva.getDate(), absPva.getFrame()),
             DEFAULT_MASS, additional);
    }

    /** Build a spacecraft state from orbit and attitude provider.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param absPva position-velocity-acceleration
     * @param attitude attitude
     * @param additional additional states
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public SpacecraftState(final AbsolutePVCoordinates absPva, final Attitude attitude, final Map<String, double[]> additional)
        throws IllegalArgumentException {
        this(absPva, attitude, DEFAULT_MASS, additional);
    }

    /** Create a new instance from orbit and mass.
     * <p>Attitude law is set to an unspecified default attitude.</p>
     * @param absPva position-velocity-acceleration
     * @param mass the mass (kg)
     * @param additional additional states
     */
    public SpacecraftState(final AbsolutePVCoordinates absPva, final double mass, final Map<String, double[]> additional) {
        this(absPva,
             new LofOffset(absPva.getFrame(), LOFType.VVLH).getAttitude(absPva, absPva.getDate(), absPva.getFrame()),
             mass, additional);
    }

    /** Build a spacecraft state from orbit, attitude provider and mass.
     * @param absPva position-velocity-acceleration
     * @param attitude attitude
     * @param mass the mass (kg)
     * @param additional additional states (may be null if no additional states are available)
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public SpacecraftState(final AbsolutePVCoordinates absPva, final Attitude attitude,
                           final double mass, final Map<String, double[]> additional)
        throws IllegalArgumentException {
        checkConsistency(absPva, attitude);
        this.orbit      = null;
        this.absPva     = absPva;
        this.attitude   = attitude;
        this.mass       = mass;
        if (additional == null) {
            this.additional = Collections.emptyMap();
        } else {
            this.additional = new HashMap<String, double[]>(additional.size());
            for (final Map.Entry<String, double[]> entry : additional.entrySet()) {
                this.additional.put(entry.getKey(), entry.getValue().clone());
            }
        }
    }

    /** Add an additional state.
     * <p>
     * {@link SpacecraftState SpacecraftState} instances are immutable,
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
    public SpacecraftState addAdditionalState(final String name, final double... value) {
        final Map<String, double[]> newMap = new HashMap<String, double[]>(additional.size() + 1);
        newMap.putAll(additional);
        newMap.put(name, value.clone());
        if (absPva == null) {
            return new SpacecraftState(orbit, attitude, mass, newMap);
        } else {
            return new SpacecraftState(absPva, attitude, mass, newMap);
        }
    }

    /** Check orbit and attitude dates are equal.
     * @param orbit the orbit
     * @param attitude attitude
     * @exception IllegalArgumentException if orbit and attitude dates
     * are not equal
     */
    private static void checkConsistency(final Orbit orbit, final Attitude attitude)
        throws IllegalArgumentException {
        if (FastMath.abs(orbit.getDate().durationFrom(attitude.getDate())) >
            DATE_INCONSISTENCY_THRESHOLD) {
            throw new OrekitIllegalArgumentException(OrekitMessages.ORBIT_AND_ATTITUDE_DATES_MISMATCH,
                                                     orbit.getDate(), attitude.getDate());
        }
        if (orbit.getFrame() != attitude.getReferenceFrame()) {
            throw new OrekitIllegalArgumentException(OrekitMessages.FRAMES_MISMATCH,
                                                     orbit.getFrame().getName(),
                                                     attitude.getReferenceFrame().getName());
        }
    }

    /** Check if the state contains an orbit part.
     * <p>
     * A state contains either an {@link AbsolutePVCoordinates absolute
     * position-velocity-acceleration} or an {@link Orbit orbit}.
     * </p>
     * @return true if state contains an orbit (in which case {@link #getOrbit()}
     * will not throw an exception), or false if the state contains an
     * absolut position-velocity-acceleration (in which case {@link #getAbsPVA()}
     * will not throw an exception)
     */
    public boolean isOrbitDefined() {
        return orbit != null;
    }

    /** Check AbsolutePVCoordinates and attitude dates are equal.
     * @param absPva position-velocity-acceleration
     * @param attitude attitude
     * @exception IllegalArgumentException if orbit and attitude dates
     * are not equal
     */
    private static void checkConsistency(final AbsolutePVCoordinates absPva, final Attitude attitude)
        throws IllegalArgumentException {
        if (FastMath.abs(absPva.getDate().durationFrom(attitude.getDate())) >
            DATE_INCONSISTENCY_THRESHOLD) {
            throw new OrekitIllegalArgumentException(OrekitMessages.ORBIT_AND_ATTITUDE_DATES_MISMATCH,
                                                     absPva.getDate(), attitude.getDate());
        }
        if (absPva.getFrame() != attitude.getReferenceFrame()) {
            throw new OrekitIllegalArgumentException(OrekitMessages.FRAMES_MISMATCH,
                                                     absPva.getFrame().getName(),
                                                     attitude.getReferenceFrame().getName());
        }
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * simple models. For orbits, the model is a Keplerian one if no derivatives
     * are available in the orbit, or Keplerian plus quadratic effect of the
     * non-Keplerian acceleration if derivatives are available. For attitude,
     * a polynomial model is used. Neither mass nor additional states change.
     * Shifting is <em>not</em> intended as a replacement for proper orbit
     * and attitude propagation but should be sufficient for small time shifts
     * or coarse accuracy.
     * </p>
     * <p>
     * As a rough order of magnitude, the following table shows the extrapolation
     * errors obtained between this simple shift method and an {@link
     * org.orekit.propagation.numerical.NumericalPropagator numerical
     * propagator} for a low Earth Sun Synchronous Orbit, with a 20x20 gravity field,
     * Sun and Moon third bodies attractions, drag and solar radiation pressure.
     * Beware that these results will be different for other orbits.
     * </p>
     * <table border="1" cellpadding="5">
     * <caption>Extrapolation Error</caption>
     * <tr bgcolor="#ccccff"><th>interpolation time (s)</th>
     * <th>position error without derivatives (m)</th><th>position error with derivatives (m)</th></tr>
     * <tr><td bgcolor="#eeeeff"> 60</td><td>  18</td><td> 1.1</td></tr>
     * <tr><td bgcolor="#eeeeff">120</td><td>  72</td><td> 9.1</td></tr>
     * <tr><td bgcolor="#eeeeff">300</td><td> 447</td><td> 140</td></tr>
     * <tr><td bgcolor="#eeeeff">600</td><td>1601</td><td>1067</td></tr>
     * <tr><td bgcolor="#eeeeff">900</td><td>3141</td><td>3307</td></tr>
     * </table>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     * except for the mass and additional states which stay unchanged
     */
    public SpacecraftState shiftedBy(final double dt) {
        if (absPva == null) {
            return new SpacecraftState(orbit.shiftedBy(dt), attitude.shiftedBy(dt),
                    mass, additional);
        } else {
            return new SpacecraftState(absPva.shiftedBy(dt), attitude.shiftedBy(dt),
                    mass, additional);
        }
    }

    /** {@inheritDoc}
     * <p>
     * The additional states that are interpolated are the ones already present
     * in the instance. The sample instances must therefore have at least the same
     * additional states has the instance. They may have more additional states,
     * but the extra ones will be ignored.
     * </p>
     * <p>
     * The instance and all the sample instances <em>must</em> be based on similar
     * trajectory data, i.e. they must either all be based on orbits or all be based
     * on absolute position-velocity-acceleration. Any inconsistency will trigger
     * an {@link OrekitIllegalStateException}.
     * </p>
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only
     * with small samples (about 10-20 points) in order to avoid <a
     * href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a>
     * and numerical problems (including NaN appearing).
     * </p>
     * @exception OrekitIllegalStateException if some instances are not based on
     * similar trajectory data
     */
    public SpacecraftState interpolate(final AbsoluteDate date,
                                       final Stream<SpacecraftState> sample) {

        // prepare interpolators
        final List<Orbit> orbits;
        final List<AbsolutePVCoordinates> absPvas;
        if (isOrbitDefined()) {
            orbits  = new ArrayList<Orbit>();
            absPvas = null;
        } else {
            orbits  = null;
            absPvas = new ArrayList<AbsolutePVCoordinates>();
        }
        final List<Attitude> attitudes = new ArrayList<Attitude>();
        final HermiteInterpolator massInterpolator = new HermiteInterpolator();
        final Map<String, HermiteInterpolator> additionalInterpolators =
                new HashMap<String, HermiteInterpolator>(additional.size());
        for (final String name : additional.keySet()) {
            additionalInterpolators.put(name, new HermiteInterpolator());
        }

        // extract sample data
        sample.forEach(state -> {
            final double deltaT = state.getDate().durationFrom(date);
            if (isOrbitDefined()) {
                orbits.add(state.getOrbit());
            } else {
                absPvas.add(state.getAbsPVA());
            }
            attitudes.add(state.getAttitude());
            massInterpolator.addSamplePoint(deltaT,
                                            new double[] {
                                                state.getMass()
                                                });
            for (final Map.Entry<String, HermiteInterpolator> entry : additionalInterpolators.entrySet()) {
                entry.getValue().addSamplePoint(deltaT, state.getAdditionalState(entry.getKey()));
            }

        });

        // perform interpolations
        final Orbit interpolatedOrbit;
        final AbsolutePVCoordinates interpolatedAbsPva;
        if (isOrbitDefined()) {
            interpolatedOrbit  = orbit.interpolate(date, orbits);
            interpolatedAbsPva = null;
        } else {
            interpolatedOrbit  = null;
            interpolatedAbsPva = absPva.interpolate(date, absPvas);
        }
        final Attitude interpolatedAttitude = attitude.interpolate(date, attitudes);
        final double interpolatedMass       = massInterpolator.value(0)[0];
        final Map<String, double[]> interpolatedAdditional;
        if (additional.isEmpty()) {
            interpolatedAdditional = null;
        } else {
            interpolatedAdditional = new HashMap<String, double[]>(additional.size());
            for (final Map.Entry<String, HermiteInterpolator> entry : additionalInterpolators.entrySet()) {
                interpolatedAdditional.put(entry.getKey(), entry.getValue().value(0));
            }
        }

        // create the complete interpolated state
        if (isOrbitDefined()) {
            return new SpacecraftState(interpolatedOrbit, interpolatedAttitude,
                                       interpolatedMass, interpolatedAdditional);
        } else {
            return new SpacecraftState(interpolatedAbsPva, interpolatedAttitude,
                                       interpolatedMass, interpolatedAdditional);
        }

    }

    /** Get the absolute position-velocity-acceleration.
     * <p>
     * A state contains either an {@link AbsolutePVCoordinates absolute
     * position-velocity-acceleration} or an {@link Orbit orbit}. Which
     * one is present can be checked using {@link #isOrbitDefined()}.
     * </p>
     * @return absolute position-velocity-acceleration
     * @exception OrekitIllegalStateException if position-velocity-acceleration is null,
     * which mean the state rather contains an {@link Orbit}
     * @see #isOrbitDefined()
     * @see #getOrbit()
     */
    public AbsolutePVCoordinates getAbsPVA() throws OrekitIllegalStateException {
        if (absPva == null) {
            throw new OrekitIllegalStateException(OrekitMessages.UNDEFINED_ABSOLUTE_PVCOORDINATES);
        }
        return absPva;
    }

    /** Get the current orbit.
     * <p>
     * A state contains either an {@link AbsolutePVCoordinates absolute
     * position-velocity-acceleration} or an {@link Orbit orbit}. Which
     * one is present can be checked using {@link #isOrbitDefined()}.
     * </p>
     * @return the orbit
     * @exception OrekitIllegalStateException if orbit is null,
     * which means the state rather contains an {@link AbsolutePVCoordinates absolute
     * position-velocity-acceleration}
     * @see #isOrbitDefined()
     * @see #getAbsPVA()
     */
    public Orbit getOrbit() throws OrekitIllegalStateException {
        if (orbit == null) {
            throw new OrekitIllegalStateException(OrekitMessages.UNDEFINED_ORBIT);
        }
        return orbit;
    }

    /** Get the date.
     * @return date
     */
    public AbsoluteDate getDate() {
        return (absPva == null) ? orbit.getDate() : absPva.getDate();
    }

    /** Get the defining frame.
     * @return the frame in which state is defined
     */
    public Frame getFrame() {
        return (absPva == null) ? orbit.getFrame() : absPva.getFrame();
    }

    /** Check if an additional state is available.
     * @param name name of the additional state
     * @return true if the additional state is available
     * @see #addAdditionalState(String, double[])
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
     * @exception MathIllegalStateException if an additional state does not have
     * the same dimension in both states
     */
    public void ensureCompatibleAdditionalStates(final SpacecraftState state)
        throws MathIllegalStateException {

        // check instance additional states is a subset of the other one
        for (final Map.Entry<String, double[]> entry : additional.entrySet()) {
            final double[] other = state.additional.get(entry.getKey());
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
          * @see #addAdditionalState(String, double[])
     * @see #hasAdditionalState(String)
     * @see #getAdditionalStates()
     */
    public double[] getAdditionalState(final String name) {
        if (!additional.containsKey(name)) {
            throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_STATE, name);
        }
        return additional.get(name).clone();
    }

    /** Get an unmodifiable map of additional states.
     * @return unmodifiable map of additional states
     * @see #addAdditionalState(String, double[])
     * @see #hasAdditionalState(String)
     * @see #getAdditionalState(String)
     */
    public Map<String, double[]> getAdditionalStates() {
        return Collections.unmodifiableMap(additional);
    }

    /** Compute the transform from state defining frame to spacecraft frame.
     * <p>The spacecraft frame origin is at the point defined by the orbit
     * (or absolute position-velocity-acceleration), and its orientation is
     * defined by the attitude.</p>
     * @return transform from specified frame to current spacecraft frame
     */
    public Transform toTransform() {
        final TimeStampedPVCoordinates pv = getPVCoordinates();
        return new Transform(pv.getDate(),
                             new Transform(pv.getDate(), pv.negate()),
                             new Transform(pv.getDate(), attitude.getOrientation()));
    }

    /** Get the central attraction coefficient.
     * @return mu central attraction coefficient (m^3/s^2), or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather than an orbit
     */
    public double getMu() {
        return (absPva == null) ? orbit.getMu() : Double.NaN;
    }

    /** Get the Keplerian period.
     * <p>The Keplerian period is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return keplerian period in seconds, or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     */
    public double getKeplerianPeriod() {
        return (absPva == null) ? orbit.getKeplerianPeriod() : Double.NaN;
    }

    /** Get the Keplerian mean motion.
     * <p>The Keplerian mean motion is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return keplerian mean motion in radians per second, or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     */
    public double getKeplerianMeanMotion() {
        return (absPva == null) ? orbit.getKeplerianMeanMotion() : Double.NaN;
    }

    /** Get the semi-major axis.
     * @return semi-major axis (m), or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     */
    public double getA() {
        return (absPva == null) ? orbit.getA() : Double.NaN;
    }

    /** Get the first component of the eccentricity vector (as per equinoctial parameters).
     * @return e cos(ω + Ω), first component of eccentricity vector, or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     * @see #getE()
     */
    public double getEquinoctialEx() {
        return (absPva == null) ? orbit.getEquinoctialEx() : Double.NaN;
    }

    /** Get the second component of the eccentricity vector (as per equinoctial parameters).
     * @return e sin(ω + Ω), second component of the eccentricity vector, or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     * @see #getE()
     */
    public double getEquinoctialEy() {
        return (absPva == null) ? orbit.getEquinoctialEy() : Double.NaN;
    }

    /** Get the first component of the inclination vector (as per equinoctial parameters).
     * @return tan(i/2) cos(Ω), first component of the inclination vector, or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     * @see #getI()
     */
    public double getHx() {
        return (absPva == null) ? orbit.getHx() : Double.NaN;
    }

    /** Get the second component of the inclination vector (as per equinoctial parameters).
     * @return tan(i/2) sin(Ω), second component of the inclination vector, or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     * @see #getI()
     */
    public double getHy() {
        return (absPva == null) ? orbit.getHy() : Double.NaN;
    }

    /** Get the true latitude argument (as per equinoctial parameters).
     * @return v + ω + Ω true longitude argument (rad), or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     * @see #getLE()
     * @see #getLM()
     */
    public double getLv() {
        return (absPva == null) ? orbit.getLv() : Double.NaN;
    }

    /** Get the eccentric latitude argument (as per equinoctial parameters).
     * @return E + ω + Ω eccentric longitude argument (rad), or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     * @see #getLv()
     * @see #getLM()
     */
    public double getLE() {
        return (absPva == null) ? orbit.getLE() : Double.NaN;
    }

    /** Get the mean longitude argument (as per equinoctial parameters).
     * @return M + ω + Ω mean latitude argument (rad), or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     * @see #getLv()
     * @see #getLE()
     */
    public double getLM() {
        return (absPva == null) ? orbit.getLM() : Double.NaN;
    }

    // Additional orbital elements

    /** Get the eccentricity.
     * @return eccentricity, or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     * @see #getEquinoctialEx()
     * @see #getEquinoctialEy()
     */
    public double getE() {
        return (absPva == null) ? orbit.getE() : Double.NaN;
    }

    /** Get the inclination.
     * @return inclination (rad)
     * @see #getHx()
     * @see #getHy()
     */
    public double getI() {
        return (absPva == null) ? orbit.getI() : Double.NaN;
    }

    /** Get the {@link TimeStampedPVCoordinates} in orbit definition frame.
     * <p>
     * Compute the position and velocity of the satellite. This method caches its
     * results, and recompute them only when the method is called with a new value
     * for mu. The result is provided as a reference to the internally cached
     * {@link TimeStampedPVCoordinates}, so the caller is responsible to copy it in a separate
     * {@link TimeStampedPVCoordinates} if it needs to keep the value for a while.
     * </p>
     * @return pvCoordinates in orbit definition frame
     */
    public TimeStampedPVCoordinates getPVCoordinates() {
        return (absPva == null) ? orbit.getPVCoordinates() : absPva.getPVCoordinates();
    }

    /** Get the {@link TimeStampedPVCoordinates} in given output frame.
     * <p>
     * Compute the position and velocity of the satellite. This method caches its
     * results, and recompute them only when the method is called with a new value
     * for mu. The result is provided as a reference to the internally cached
     * {@link TimeStampedPVCoordinates}, so the caller is responsible to copy it in a separate
     * {@link TimeStampedPVCoordinates} if it needs to keep the value for a while.
     * </p>
     * @param outputFrame frame in which coordinates should be defined
     * @return pvCoordinates in orbit definition frame
     */
    public TimeStampedPVCoordinates getPVCoordinates(final Frame outputFrame) {
        return (absPva == null) ? orbit.getPVCoordinates(outputFrame) : absPva.getPVCoordinates(outputFrame);
    }

    /** Get the attitude.
     * @return the attitude.
     */
    public Attitude getAttitude() {
        return attitude;
    }

    /** Gets the current mass.
     * @return the mass (kg)
     */
    public double getMass() {
        return mass;
    }

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return isOrbitDefined() ? new DTOO(this) : new DTOA(this);
    }

    /** Internal class used only for serialization. */
    private static class DTOO implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20150916L;

        /** Orbit. */
        private final Orbit orbit;

        /** Attitude and mass double values. */
        private double[] d;

        /** Additional states. */
        private final Map<String, double[]> additional;

        /** Simple constructor.
         * @param state instance to serialize
         */
        private DTOO(final SpacecraftState state) {

            this.orbit      = state.orbit;
            this.additional = state.additional.isEmpty() ? null : state.additional;

            final Rotation rotation             = state.attitude.getRotation();
            final Vector3D spin                 = state.attitude.getSpin();
            final Vector3D rotationAcceleration = state.attitude.getRotationAcceleration();
            this.d = new double[] {
                rotation.getQ0(), rotation.getQ1(), rotation.getQ2(), rotation.getQ3(),
                spin.getX(), spin.getY(), spin.getZ(),
                rotationAcceleration.getX(), rotationAcceleration.getY(), rotationAcceleration.getZ(),
                state.mass
            };

        }

        /** Replace the de-serialized data transfer object with a {@link SpacecraftState}.
         * @return replacement {@link SpacecraftState}
         */
        private Object readResolve() {
            return new SpacecraftState(orbit,
                                       new Attitude(orbit.getFrame(),
                                                    new TimeStampedAngularCoordinates(orbit.getDate(),
                                                                                      new Rotation(d[0], d[1], d[2], d[3], false),
                                                                                      new Vector3D(d[4], d[5], d[6]),
                                                                                      new Vector3D(d[7], d[8], d[9]))),
                                       d[10], additional);
        }

    }

    /** Internal class used only for serialization. */
    private static class DTOA implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20150916L;

        /** Absolute position-velocity-acceleration. */
        private final AbsolutePVCoordinates absPva;

        /** Attitude and mass double values. */
        private double[] d;

        /** Additional states. */
        private final Map<String, double[]> additional;

        /** Simple constructor.
         * @param state instance to serialize
         */
        private DTOA(final SpacecraftState state) {

            this.absPva     = state.absPva;
            this.additional = state.additional.isEmpty() ? null : state.additional;

            final Rotation rotation             = state.attitude.getRotation();
            final Vector3D spin                 = state.attitude.getSpin();
            final Vector3D rotationAcceleration = state.attitude.getRotationAcceleration();
            this.d = new double[] {
                rotation.getQ0(), rotation.getQ1(), rotation.getQ2(), rotation.getQ3(),
                spin.getX(), spin.getY(), spin.getZ(),
                rotationAcceleration.getX(), rotationAcceleration.getY(), rotationAcceleration.getZ(),
                state.mass
            };

        }

        /** Replace the deserialized data transfer object with a {@link SpacecraftState}.
         * @return replacement {@link SpacecraftState}
         */
        private Object readResolve() {
            return new SpacecraftState(absPva,
                                       new Attitude(absPva.getFrame(),
                                                    new TimeStampedAngularCoordinates(absPva.getDate(),
                                                                                      new Rotation(d[0], d[1], d[2], d[3], false),
                                                                                      new Vector3D(d[4], d[5], d[6]),
                                                                                      new Vector3D(d[7], d[8], d[9]))),
                                       d[10], additional);
        }
    }

    @Override
    public String toString() {
        return "SpacecraftState{" +
                "orbit=" + orbit +
                ", attitude=" + attitude +
                ", mass=" + mass +
                ", additional=" + additional +
                '}';
    }
}

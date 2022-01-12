/* Copyright 2002-2022 CS GROUP
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
package org.orekit.propagation;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.interpolation.FieldHermiteInterpolator;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitIllegalStateException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeInterpolable;
import org.orekit.time.FieldTimeShiftable;
import org.orekit.time.FieldTimeStamped;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.FieldArrayDictionary;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;


/** This class is the representation of a complete state holding orbit, attitude
 * and mass information at a given date.
 *
 * <p>It contains an {@link FieldOrbit orbital state} at a current
 * {@link FieldAbsoluteDate} both handled by an {@link FieldOrbit}, plus the current
 * mass and attitude. FieldOrbit and state are guaranteed to be consistent in terms
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
 * The instance {@code FieldSpacecraftState} is guaranteed to be immutable.
 * </p>
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 * @author Vincent Mouraux
 */
public class FieldSpacecraftState <T extends CalculusFieldElement<T>>
    implements FieldTimeStamped<T>, FieldTimeShiftable<FieldSpacecraftState<T>, T>, FieldTimeInterpolable<FieldSpacecraftState<T>, T> {

    /** Default mass. */
    private static final double DEFAULT_MASS = 1000.0;

    /**
     * tolerance on date comparison in {@link #checkConsistency(FieldOrbit<T>, FieldAttitude<T>)}. 100 ns
     * corresponds to sub-mm accuracy at LEO orbital velocities.
     */
    private static final double DATE_INCONSISTENCY_THRESHOLD = 100e-9;

    /** Orbital state. */
    private final FieldOrbit<T> orbit;

    /** Trajectory state, when it is not an orbit. */
    private final FieldAbsolutePVCoordinates<T> absPva;

    /** FieldAttitude<T>. */
    private final FieldAttitude<T> attitude;

    /** Current mass (kg). */
    private final T mass;

    /** Additional states. */
    private final FieldArrayDictionary<T> additional;

    /** Additional states derivatives.
     * @since 11.1
     */
    private final FieldArrayDictionary<T> additionalDot;

    /** Build a spacecraft state from orbit only.
     * <p>FieldAttitude and mass are set to unspecified non-null arbitrary values.</p>
     * @param orbit the orbit
     */
    public FieldSpacecraftState(final FieldOrbit<T> orbit) {
        this(orbit,
             new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS).getAttitude(orbit, orbit.getDate(), orbit.getFrame()),
             orbit.getA().getField().getZero().add(DEFAULT_MASS), (FieldArrayDictionary<T>) null);
    }

    /** Build a spacecraft state from orbit and attitude.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param orbit the orbit
     * @param attitude attitude
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final FieldAttitude<T> attitude)
        throws IllegalArgumentException {
        this(orbit, attitude, orbit.getA().getField().getZero().add(DEFAULT_MASS), (FieldArrayDictionary<T>) null);
    }

    /** Create a new instance from orbit and mass.
     * <p>FieldAttitude law is set to an unspecified default attitude.</p>
     * @param orbit the orbit
     * @param mass the mass (kg)
     */
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final T mass) {
        this(orbit,
             new LofOffset(orbit.getFrame(),
                           LOFType.LVLH_CCSDS).getAttitude(orbit, orbit.getDate(), orbit.getFrame()),
             mass, (FieldArrayDictionary<T>) null);
    }

    /** Build a spacecraft state from orbit, attitude and mass.
     * @param orbit the orbit
     * @param attitude attitude
     * @param mass the mass (kg)
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final FieldAttitude<T> attitude, final T mass)
        throws IllegalArgumentException {
        this(orbit, attitude, mass, (FieldArrayDictionary<T>) null);
    }

    /** Build a spacecraft state from orbit and additional states.
     * <p>FieldAttitude and mass are set to unspecified non-null arbitrary values.</p>
     * @param orbit the orbit
     * @param additional additional states
     * @deprecated as of 11.1, replacezd by {@link #FieldSpacecraftState(FieldOrbit, FieldArrayDictionary)}
     */
    @Deprecated
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final Map<String, T[]> additional) {
        this(orbit, new FieldArrayDictionary<>(orbit.getDate().getField(), additional));
    }

    /** Build a spacecraft state from orbit and additional states.
     * <p>FieldAttitude and mass are set to unspecified non-null arbitrary values.</p>
     * @param orbit the orbit
     * @param additional additional states
     * @since 11.1
     */
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final FieldArrayDictionary<T> additional) {
        this(orbit,
             new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS).getAttitude(orbit, orbit.getDate(), orbit.getFrame()),
             orbit.getA().getField().getZero().add(DEFAULT_MASS), additional);
    }

    /** Build a spacecraft state from orbit attitude and additional states.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param orbit the orbit
     * @param attitude attitude
     * @param additional additional states
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     * @deprecated as of 11.1, replaced by {@link #FieldSpacecraftState(FieldOrbit, FieldAttitude, FieldArrayDictionary)}
     */
    @Deprecated
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final FieldAttitude<T> attitude, final Map<String, T[]> additional)
        throws IllegalArgumentException {
        this(orbit, attitude, new FieldArrayDictionary<>(orbit.getDate().getField(), additional));
    }

    /** Build a spacecraft state from orbit attitude and additional states.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param orbit the orbit
     * @param attitude attitude
     * @param additional additional states
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     * @since 11.1
     */
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final FieldAttitude<T> attitude, final FieldArrayDictionary<T> additional)
        throws IllegalArgumentException {
        this(orbit, attitude, orbit.getA().getField().getZero().add(DEFAULT_MASS), additional);
    }

    /** Create a new instance from orbit, mass and additional states.
     * <p>FieldAttitude law is set to an unspecified default attitude.</p>
     * @param orbit the orbit
     * @param mass the mass (kg)
     * @param additional additional states
     * @deprecated as of 11.1, replaced by {@link #FieldSpacecraftState(FieldOrbit, CalculusFieldElement, FieldArrayDictionary)}
     */
    @Deprecated
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final T mass, final Map<String, T[]> additional) {
        this(orbit, mass, new FieldArrayDictionary<>(orbit.getDate().getField(), additional));
    }

    /** Create a new instance from orbit, mass and additional states.
     * <p>FieldAttitude law is set to an unspecified default attitude.</p>
     * @param orbit the orbit
     * @param mass the mass (kg)
     * @param additional additional states
     * @since 11.1
     */
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final T mass, final FieldArrayDictionary<T> additional) {
        this(orbit,
             new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS).getAttitude(orbit, orbit.getDate(), orbit.getFrame()),
             mass, additional);
    }

    /** Build a spacecraft state from orbit, attitude, mass and additional states.
     * @param orbit the orbit
     * @param attitude attitude
     * @param mass the mass (kg)
     * @param additional additional states (may be null if no additional states are available)
     * @deprecated as of 11.1, replaced by {@link #FieldSpacecraftState(FieldOrbit, FieldAttitude, CalculusFieldElement, FieldArrayDictionary)}
     */
    @Deprecated
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final FieldAttitude<T> attitude,
                                final T mass, final Map<String, T[]> additional) {
        this(orbit, attitude, mass, new FieldArrayDictionary<>(orbit.getDate().getField(), additional));
    }

    /** Build a spacecraft state from orbit, attitude, mass and additional states.
     * @param orbit the orbit
     * @param attitude attitude
     * @param mass the mass (kg)
     * @param additional additional states (may be null if no additional states are available)
     * @since 11.1
     */
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final FieldAttitude<T> attitude,
                                final T mass, final FieldArrayDictionary<T> additional) {
        this(orbit, attitude, mass, additional, null);
    }

    /** Build a spacecraft state from orbit, attitude, mass, additional states and derivatives.
     * @param orbit the orbit
     * @param attitude attitude
     * @param mass the mass (kg)
     * @param additional additional states (may be null if no additional states are available)
     * @param additionalDot additional states derivatives(may be null if no additional states derivative sare available)
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     * @since 11.1
     */
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final FieldAttitude<T> attitude, final T mass,
                                final FieldArrayDictionary<T> additional,
                                final FieldArrayDictionary<T> additionalDot)
        throws IllegalArgumentException {
        checkConsistency(orbit, attitude);
        this.orbit      = orbit;
        this.attitude   = attitude;
        this.mass       = mass;
        this.absPva     = null;

        if (additional == null) {
            this.additional = new FieldArrayDictionary<>(orbit.getDate().getField());
        } else {
            this.additional = new FieldArrayDictionary<>(additional);
        }

        if (additionalDot == null) {
            this.additionalDot = new FieldArrayDictionary<>(orbit.getDate().getField());
        } else {

            this.additionalDot = new FieldArrayDictionary<>(additionalDot);
        }

    }

    /** Convert a {@link FieldSpacecraftState}.
     * @param field field to which the elements belong
     * @param state state to convert
     */
    public FieldSpacecraftState(final Field<T> field, final SpacecraftState state) {

        if (state.isOrbitDefined()) {
            final double[] stateD    = new double[6];
            final double[] stateDotD = state.getOrbit().hasDerivatives() ? new double[6] : null;
            state.getOrbit().getType().mapOrbitToArray(state.getOrbit(), PositionAngle.TRUE, stateD, stateDotD);
            final T[] stateF    = MathArrays.buildArray(field, 6);
            for (int i = 0; i < stateD.length; ++i) {
                stateF[i]    = field.getZero().add(stateD[i]);
            }
            final T[] stateDotF;
            if (stateDotD == null) {
                stateDotF = null;
            } else {
                stateDotF = MathArrays.buildArray(field, 6);
                for (int i = 0; i < stateDotD.length; ++i) {
                    stateDotF[i] = field.getZero().add(stateDotD[i]);
                }
            }

            final FieldAbsoluteDate<T> dateF = new FieldAbsoluteDate<>(field, state.getDate());

            this.orbit    = state.getOrbit().getType().mapArrayToOrbit(stateF, stateDotF,
                                                                       PositionAngle.TRUE,
                                                                       dateF, field.getZero().add(state.getMu()), state.getFrame());
            this.absPva   = null;

        } else {
            final T zero = field.getZero();
            final T x = zero.add(state.getPVCoordinates().getPosition().getX());
            final T y = zero.add(state.getPVCoordinates().getPosition().getY());
            final T z = zero.add(state.getPVCoordinates().getPosition().getZ());
            final T vx = zero.add(state.getPVCoordinates().getVelocity().getX());
            final T vy = zero.add(state.getPVCoordinates().getVelocity().getY());
            final T vz = zero.add(state.getPVCoordinates().getVelocity().getZ());
            final T ax = zero.add(state.getPVCoordinates().getAcceleration().getX());
            final T ay = zero.add(state.getPVCoordinates().getAcceleration().getY());
            final T az = zero.add(state.getPVCoordinates().getAcceleration().getZ());
            final FieldPVCoordinates<T> pva_f = new FieldPVCoordinates<>(new FieldVector3D<>(x, y, z),
                                                                         new FieldVector3D<>(vx, vy, vz),
                                                                         new FieldVector3D<>(ax, ay, az));
            final FieldAbsoluteDate<T> dateF = new FieldAbsoluteDate<>(field, state.getDate());
            this.orbit  = null;
            this.absPva = new FieldAbsolutePVCoordinates<>(state.getFrame(), dateF, pva_f);
        }

        this.attitude = new FieldAttitude<>(field, state.getAttitude());
        this.mass     = field.getZero().add(state.getMass());

        final DoubleArrayDictionary additionalD = state.getAdditionalStatesValues();
        if (additionalD.size() == 0) {
            this.additional = new FieldArrayDictionary<>(field);
        } else {
            this.additional = new FieldArrayDictionary<>(field, additionalD.size());
            for (final DoubleArrayDictionary.Entry entry : additionalD.getData()) {
                this.additional.put(entry.getKey(), entry.getValue());
            }
        }
        final DoubleArrayDictionary additionalDotD = state.getAdditionalStatesDerivatives();
        if (additionalDotD.size() == 0) {
            this.additionalDot = new FieldArrayDictionary<>(field);
        } else {
            this.additionalDot = new FieldArrayDictionary<>(field, additionalDotD.size());
            for (final DoubleArrayDictionary.Entry entry : additionalDotD.getData()) {
                this.additionalDot.put(entry.getKey(), entry.getValue());
            }
        }

    }

    /** Build a spacecraft state from orbit only.
     * <p>Attitude and mass are set to unspecified non-null arbitrary values.</p>
     * @param absPva position-velocity-acceleration
     */
    public FieldSpacecraftState(final FieldAbsolutePVCoordinates<T> absPva) {
        this(absPva,
             new LofOffset(absPva.getFrame(),
                           LOFType.LVLH_CCSDS).getAttitude(absPva, absPva.getDate(), absPva.getFrame()),
             absPva.getDate().getField().getZero().add(DEFAULT_MASS), (FieldArrayDictionary<T>) null);
    }

    /** Build a spacecraft state from orbit and attitude.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param absPva position-velocity-acceleration
     * @param attitude attitude
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public FieldSpacecraftState(final FieldAbsolutePVCoordinates<T> absPva, final FieldAttitude<T> attitude)
        throws IllegalArgumentException {
        this(absPva, attitude, absPva.getDate().getField().getZero().add(DEFAULT_MASS), (FieldArrayDictionary<T>) null);
    }

    /** Create a new instance from orbit and mass.
     * <p>Attitude law is set to an unspecified default attitude.</p>
     * @param absPva position-velocity-acceleration
     * @param mass the mass (kg)
     */
    public FieldSpacecraftState(final FieldAbsolutePVCoordinates<T> absPva, final T mass) {
        this(absPva,
             new LofOffset(absPva.getFrame(),
                           LOFType.LVLH_CCSDS).getAttitude(absPva, absPva.getDate(), absPva.getFrame()),
             mass, (FieldArrayDictionary<T>) null);
    }

    /** Build a spacecraft state from orbit, attitude and mass.
     * @param absPva position-velocity-acceleration
     * @param attitude attitude
     * @param mass the mass (kg)
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public FieldSpacecraftState(final FieldAbsolutePVCoordinates<T> absPva, final FieldAttitude<T> attitude, final T mass)
        throws IllegalArgumentException {
        this(absPva, attitude, mass, (FieldArrayDictionary<T>) null);
    }

    /** Build a spacecraft state from orbit only.
     * <p>Attitude and mass are set to unspecified non-null arbitrary values.</p>
     * @param absPva position-velocity-acceleration
     * @param additional additional states
     * @deprecated as of 11.1, replaced by {@link #FieldSpacecraftState(FieldAbsolutePVCoordinates, FieldArrayDictionary)}
     */
    @Deprecated
    public FieldSpacecraftState(final FieldAbsolutePVCoordinates<T> absPva, final Map<String, T[]> additional) {
        this(absPva, new FieldArrayDictionary<>(absPva.getDate().getField(), additional));
    }

    /** Build a spacecraft state from orbit only.
     * <p>Attitude and mass are set to unspecified non-null arbitrary values.</p>
     * @param absPva position-velocity-acceleration
     * @param additional additional states
     * @since 11.1
     */
    public FieldSpacecraftState(final FieldAbsolutePVCoordinates<T> absPva, final FieldArrayDictionary<T> additional) {
        this(absPva,
             new LofOffset(absPva.getFrame(),
                           LOFType.LVLH_CCSDS).getAttitude(absPva, absPva.getDate(), absPva.getFrame()),
             absPva.getDate().getField().getZero().add(DEFAULT_MASS), additional);
    }

    /** Build a spacecraft state from orbit and attitude.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param absPva position-velocity-acceleration
     * @param attitude attitude
     * @param additional additional states
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     * @deprecated as of 11.1, replaced by {@link #FieldSpacecraftState(FieldAbsolutePVCoordinates, FieldAttitude, FieldArrayDictionary)}
     */
    @Deprecated
    public FieldSpacecraftState(final FieldAbsolutePVCoordinates<T> absPva, final FieldAttitude<T> attitude,
                                final Map<String, T[]> additional)
        throws IllegalArgumentException {
        this(absPva, attitude, new FieldArrayDictionary<>(absPva.getDate().getField(), additional));
    }

    /** Build a spacecraft state from orbit and attitude.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param absPva position-velocity-acceleration
     * @param attitude attitude
     * @param additional additional states
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     * @since 11.1
     */
    public FieldSpacecraftState(final FieldAbsolutePVCoordinates<T> absPva, final FieldAttitude<T> attitude,
                                final FieldArrayDictionary<T> additional)
        throws IllegalArgumentException {
        this(absPva, attitude, absPva.getDate().getField().getZero().add(DEFAULT_MASS), additional);
    }

    /** Create a new instance from orbit and mass.
     * <p>Attitude law is set to an unspecified default attitude.</p>
     * @param absPva position-velocity-acceleration
     * @param mass the mass (kg)
     * @param additional additional states
     * @deprecated as of 11.1, replaced by {@link #FieldSpacecraftState(FieldAbsolutePVCoordinates, CalculusFieldElement, FieldArrayDictionary)}
     */
    @Deprecated
    public FieldSpacecraftState(final FieldAbsolutePVCoordinates<T> absPva, final T mass, final Map<String, T[]> additional) {
        this(absPva, mass, new FieldArrayDictionary<>(absPva.getDate().getField(), additional));
    }

    /** Create a new instance from orbit and mass.
     * <p>Attitude law is set to an unspecified default attitude.</p>
     * @param absPva position-velocity-acceleration
     * @param mass the mass (kg)
     * @param additional additional states
     * @since 11.1
     */
    public FieldSpacecraftState(final FieldAbsolutePVCoordinates<T> absPva, final T mass, final FieldArrayDictionary<T> additional) {
        this(absPva,
             new LofOffset(absPva.getFrame(),
                           LOFType.LVLH_CCSDS).getAttitude(absPva, absPva.getDate(), absPva.getFrame()),
             mass, additional);
    }

    /** Build a spacecraft state from orbit, attitude and mass.
     * @param absPva position-velocity-acceleration
     * @param attitude attitude
     * @param mass the mass (kg)
     * @param additional additional states (may be null if no additional states are available)
     * @deprecated as of 11.1, replaced by {@link #FieldSpacecraftState(FieldAbsolutePVCoordinates, FieldAttitude, CalculusFieldElement, FieldArrayDictionary)}
     */
    @Deprecated
    public FieldSpacecraftState(final FieldAbsolutePVCoordinates<T> absPva, final FieldAttitude<T> attitude,
                           final T mass, final Map<String, T[]> additional) {
        this(absPva, attitude, mass, new FieldArrayDictionary<>(absPva.getDate().getField(), additional));
    }

    /** Build a spacecraft state from orbit, attitude and mass.
     * @param absPva position-velocity-acceleration
     * @param attitude attitude
     * @param mass the mass (kg)
     * @param additional additional states (may be null if no additional states are available)
     * @since 11.1
     */
    public FieldSpacecraftState(final FieldAbsolutePVCoordinates<T> absPva, final FieldAttitude<T> attitude,
                           final T mass, final FieldArrayDictionary<T> additional) {
        this(absPva, attitude, mass, additional, null);
    }

    /** Build a spacecraft state from orbit, attitude and mass.
     * @param absPva position-velocity-acceleration
     * @param attitude attitude
     * @param mass the mass (kg)
     * @param additional additional states (may be null if no additional states are available)
     * @param additionalDot additional states derivatives(may be null if no additional states derivatives are available)
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     * @since 11.1
     */
    public FieldSpacecraftState(final FieldAbsolutePVCoordinates<T> absPva, final FieldAttitude<T> attitude, final T mass,
                                final FieldArrayDictionary<T> additional, final FieldArrayDictionary<T> additionalDot)
        throws IllegalArgumentException {
        checkConsistency(absPva, attitude);
        this.orbit      = null;
        this.absPva     = absPva;
        this.attitude   = attitude;
        this.mass       = mass;
        if (additional == null) {
            this.additional = new FieldArrayDictionary<T>(absPva.getDate().getField());
        } else {
            this.additional = new FieldArrayDictionary<T>(additional);
        }
        if (additionalDot == null) {
            this.additionalDot = new FieldArrayDictionary<T>(absPva.getDate().getField());
        } else {
            this.additionalDot = new FieldArrayDictionary<T>(additionalDot);
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
    public final FieldSpacecraftState<T> addAdditionalState(final String name, final T... value) {
        final FieldArrayDictionary<T> newDict = new FieldArrayDictionary<>(additional);
        newDict.put(name, value.clone());
        if (absPva == null) {
            return new FieldSpacecraftState<>(orbit, attitude, mass, newDict, additionalDot);
        } else {
            return new FieldSpacecraftState<>(absPva, attitude, mass, newDict, additionalDot);
        }
    }

    /** Add an additional state derivative.
    * {@link FieldSpacecraftState FieldSpacecraftState} instances are immutable,
     * so this method does <em>not</em> change the instance, but rather
     * creates a new instance, which has the same components as the original
     * instance, except it also has the specified state derivative. If the
     * original instance already had an additional state derivative with the
     * same name, it will be overridden. If it did not have any additional
     * state derivative with that name, the new instance will have one more
     * additional state derivative than the original instance.
     * @param name name of the additional state derivative
     * @param value value of the additional state derivative
     * @return a new instance, with the additional state derivative added
     * @see #hasAdditionalStateDerivative(String)
     * @see #getAdditionalStateDerivative(String)
     * @see #getAdditionalStatesDerivatives()
     */
    @SafeVarargs
    public final FieldSpacecraftState<T> addAdditionalStateDerivative(final String name, final T... value) {
        final FieldArrayDictionary<T> newDict = new FieldArrayDictionary<>(additionalDot);
        newDict.put(name, value.clone());
        if (absPva == null) {
            return new FieldSpacecraftState<>(orbit, attitude, mass, additional, newDict);
        } else {
            return new FieldSpacecraftState<>(absPva, attitude, mass, additional, newDict);
        }
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

    /** Check if the state contains an orbit part.
     * <p>
     * A state contains either an {@link FieldAbsolutePVCoordinates absolute
     * position-velocity-acceleration} or an {@link FieldOrbit orbit}.
     * </p>
     * @return true if state contains an orbit (in which case {@link #getOrbit()}
     * will not throw an exception), or false if the state contains an
     * absolut position-velocity-acceleration (in which case {@link #getAbsPVA()}
     * will not throw an exception)
     */
    public boolean isOrbitDefined() {
        return orbit != null;
    }

    /**
     * Check FieldAbsolutePVCoordinates and attitude dates are equal.
     * @param absPva   position-velocity-acceleration
     * @param attitude attitude
     * @param <T>      the type of the field elements
     * @exception IllegalArgumentException if orbit and attitude dates are not equal
     */
    private static <T extends CalculusFieldElement<T>> void checkConsistency(final FieldAbsolutePVCoordinates<T> absPva, final FieldAttitude<T> attitude)
        throws IllegalArgumentException {
        if (FastMath.abs(absPva.getDate().durationFrom(attitude.getDate())).getReal() >
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
     * a simple Keplerian model for orbit, a linear extrapolation for attitude
     * taking the spin rate into account and neither mass nor additional states
     * changes. It is <em>not</em> intended as a replacement for proper orbit
     * and attitude propagation but should be sufficient for small time shifts
     * or coarse accuracy.
     * </p>
     * <p>
     * As a rough order of magnitude, the following table shows the extrapolation
     * errors obtained between this simple shift method and an {@link
     * org.orekit.propagation.numerical.FieldNumericalPropagator numerical
     * propagator} for a low Earth Sun Synchronous Orbit, with a 20x20 gravity field,
     * Sun and Moon third bodies attractions, drag and solar radiation pressure.
     * Beware that these results will be different for other orbits.
     * </p>
     * <table border="1">
     * <caption>Extrapolation Error</caption>
     * <tr style="background-color: #ccccff;"><th>interpolation time (s)</th>
     * <th>position error without derivatives (m)</th><th>position error with derivatives (m)</th></tr>
     * <tr><td style="background-color: #eeeeff; padding:5px"> 60</td><td>  18</td><td> 1.1</td></tr>
     * <tr><td style="background-color: #eeeeff; padding:5px">120</td><td>  72</td><td> 9.1</td></tr>
     * <tr><td style="background-color: #eeeeff; padding:5px">300</td><td> 447</td><td> 140</td></tr>
     * <tr><td style="background-color: #eeeeff; padding:5px">600</td><td>1601</td><td>1067</td></tr>
     * <tr><td style="background-color: #eeeeff; padding:5px">900</td><td>3141</td><td>3307</td></tr>
     * </table>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     * except for the mass which stay unchanged
     */
    public FieldSpacecraftState<T> shiftedBy(final double dt) {
        if (absPva == null) {
            return new FieldSpacecraftState<>(orbit.shiftedBy(dt), attitude.shiftedBy(dt),
                    mass, additional, additionalDot);
        } else {
            return new FieldSpacecraftState<>(absPva.shiftedBy(dt), attitude.shiftedBy(dt),
                    mass, additional, additionalDot);
        }
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple Keplerian model for orbit, a linear extrapolation for attitude
     * taking the spin rate into account and neither mass nor additional states
     * changes. It is <em>not</em> intended as a replacement for proper orbit
     * and attitude propagation but should be sufficient for small time shifts
     * or coarse accuracy.
     * </p>
     * <p>
     * As a rough order of magnitude, the following table shows the extrapolation
     * errors obtained between this simple shift method and an {@link
     * org.orekit.propagation.numerical.FieldNumericalPropagator numerical
     * propagator} for a low Earth Sun Synchronous Orbit, with a 20x20 gravity field,
     * Sun and Moon third bodies attractions, drag and solar radiation pressure.
     * Beware that these results will be different for other orbits.
     * </p>
     * <table border="1">
     * <caption>Extrapolation Error</caption>
     * <tr style="background-color: #ccccff;"><th>interpolation time (s)</th>
     * <th>position error without derivatives (m)</th><th>position error with derivatives (m)</th></tr>
     * <tr><td style="background-color: #eeeeff; padding:5px"> 60</td><td>  18</td><td> 1.1</td></tr>
     * <tr><td style="background-color: #eeeeff; padding:5px">120</td><td>  72</td><td> 9.1</td></tr>
     * <tr><td style="background-color: #eeeeff; padding:5px">300</td><td> 447</td><td> 140</td></tr>
     * <tr><td style="background-color: #eeeeff; padding:5px">600</td><td>1601</td><td>1067</td></tr>
     * <tr><td style="background-color: #eeeeff; padding:5px">900</td><td>3141</td><td>3307</td></tr>
     * </table>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     * except for the mass which stay unchanged
     */
    public FieldSpacecraftState<T> shiftedBy(final T dt) {
        if (absPva == null) {
            return new FieldSpacecraftState<>(orbit.shiftedBy(dt), attitude.shiftedBy(dt),
                    mass, additional, additionalDot);
        } else {
            return new FieldSpacecraftState<>(absPva.shiftedBy(dt), attitude.shiftedBy(dt),
                    mass, additional, additionalDot);
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
    public FieldSpacecraftState<T> interpolate(final FieldAbsoluteDate<T> date,
                                               final Stream<FieldSpacecraftState<T>> sample) {

        // prepare interpolators
        final List<FieldOrbit<T>> orbits;
        final List<FieldAbsolutePVCoordinates<T>> absPvas;
        if (isOrbitDefined()) {
            orbits  = new ArrayList<FieldOrbit<T>>();
            absPvas = null;
        } else {
            orbits  = null;
            absPvas = new ArrayList<FieldAbsolutePVCoordinates<T>>();
        }
        final List<FieldAttitude<T>> attitudes = new ArrayList<>();
        final FieldHermiteInterpolator<T> massInterpolator = new FieldHermiteInterpolator<>();
        final List<FieldArrayDictionary<T>.Entry> addionalEntries = additional.getData();
        final Map<String, FieldHermiteInterpolator<T>> additionalInterpolators =
                new HashMap<String, FieldHermiteInterpolator<T>>(additional.size());
        for (final FieldArrayDictionary<T>.Entry entry : addionalEntries) {
            additionalInterpolators.put(entry.getKey(), new FieldHermiteInterpolator<>());
        }
        final List<FieldArrayDictionary<T>.Entry> addionalDotEntries = additionalDot.getData();
        final Map<String, FieldHermiteInterpolator<T>> additionalDotInterpolators =
                new HashMap<String, FieldHermiteInterpolator<T>>(additionalDot.size());
        for (final FieldArrayDictionary<T>.Entry entry : addionalDotEntries) {
            additionalDotInterpolators.put(entry.getKey(), new FieldHermiteInterpolator<>());
        }

        // extract sample data
        sample.forEach(state -> {
            final T deltaT = state.getDate().durationFrom(date);
            if (isOrbitDefined()) {
                orbits.add(state.getOrbit());
            } else {
                absPvas.add(state.getAbsPVA());
            }
            attitudes.add(state.getAttitude());
            final T[] mm = MathArrays.buildArray(date.getField(), 1);
            mm[0] = state.getMass();
            massInterpolator.addSamplePoint(deltaT,
                                            mm);
            for (final Map.Entry<String, FieldHermiteInterpolator<T>> entry : additionalInterpolators.entrySet()) {
                entry.getValue().addSamplePoint(deltaT, state.getAdditionalState(entry.getKey()));
            }
            for (final Map.Entry<String, FieldHermiteInterpolator<T>> entry : additionalDotInterpolators.entrySet()) {
                entry.getValue().addSamplePoint(deltaT, state.getAdditionalStateDerivative(entry.getKey()));
            }
        });

        // perform interpolations
        final FieldOrbit<T> interpolatedOrbit;
        final FieldAbsolutePVCoordinates<T> interpolatedAbsPva;
        if (isOrbitDefined()) {
            interpolatedOrbit  = orbit.interpolate(date, orbits);
            interpolatedAbsPva = null;
        } else {
            interpolatedOrbit  = null;
            interpolatedAbsPva = absPva.interpolate(date, absPvas);
        }
        final FieldAttitude<T> interpolatedAttitude = attitude.interpolate(date, attitudes);
        final T interpolatedMass       = massInterpolator.value(date.getField().getZero())[0];
        final FieldArrayDictionary<T> interpolatedAdditional;
        if (additionalInterpolators.isEmpty()) {
            interpolatedAdditional = null;
        } else {
            interpolatedAdditional = new FieldArrayDictionary<>(date.getField(), additionalInterpolators.size());
            for (final Map.Entry<String, FieldHermiteInterpolator<T>> entry : additionalInterpolators.entrySet()) {
                interpolatedAdditional.put(entry.getKey(), entry.getValue().value(date.getField().getZero()));
            }
        }
        final FieldArrayDictionary<T> interpolatedAdditionalDot;
        if (additionalDotInterpolators.isEmpty()) {
            interpolatedAdditionalDot = null;
        } else {
            interpolatedAdditionalDot = new FieldArrayDictionary<>(date.getField(), additionalDotInterpolators.size());
            for (final Map.Entry<String, FieldHermiteInterpolator<T>> entry : additionalDotInterpolators.entrySet()) {
                interpolatedAdditionalDot.put(entry.getKey(), entry.getValue().value(date.getField().getZero()));
            }
        }

        // create the complete interpolated state
        if (isOrbitDefined()) {
            return new FieldSpacecraftState<>(interpolatedOrbit, interpolatedAttitude, interpolatedMass,
                                              interpolatedAdditional, interpolatedAdditionalDot);
        } else {
            return new FieldSpacecraftState<>(interpolatedAbsPva, interpolatedAttitude, interpolatedMass,
                                              interpolatedAdditional, interpolatedAdditionalDot);
        }

    }

    /** Get the absolute position-velocity-acceleration.
     * <p>
     * A state contains either an {@link FieldAbsolutePVCoordinates absolute
     * position-velocity-acceleration} or an {@link FieldOrbit orbit}. Which
     * one is present can be checked using {@link #isOrbitDefined()}.
     * </p>
     * @return absolute position-velocity-acceleration
     * @exception OrekitIllegalStateException if position-velocity-acceleration is null,
     * which mean the state rather contains an {@link FieldOrbit}
     * @see #isOrbitDefined()
     * @see #getOrbit()
     */
    public FieldAbsolutePVCoordinates<T> getAbsPVA() throws OrekitIllegalStateException {
        if (absPva == null) {
            throw new OrekitIllegalStateException(OrekitMessages.UNDEFINED_ABSOLUTE_PVCOORDINATES);
        }
        return absPva;
    }

    /** Get the current orbit.
     * <p>
     * A state contains either an {@link FieldAbsolutePVCoordinates absolute
     * position-velocity-acceleration} or an {@link FieldOrbit orbit}. Which
     * one is present can be checked using {@link #isOrbitDefined()}.
     * </p>
     * @return the orbit
     * @exception OrekitIllegalStateException if orbit is null,
     * which means the state rather contains an {@link FieldAbsolutePVCoordinates absolute
     * position-velocity-acceleration}
     * @see #isOrbitDefined()
     * @see #getAbsPVA()
     */
    public FieldOrbit<T> getOrbit() throws OrekitIllegalStateException {
        if (orbit == null) {
            throw new OrekitIllegalStateException(OrekitMessages.UNDEFINED_ORBIT);
        }
        return orbit;
    }

    /** Get the date.
     * @return date
     */
    public FieldAbsoluteDate<T> getDate() {
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
     * @see #addAdditionalState(String, CalculusFieldElement...)
     * @see #getAdditionalState(String)
     * @see #getAdditionalStates()
     */
    public boolean hasAdditionalState(final String name) {
        return additional.getEntry(name) != null;
    }

    /** Check if an additional state derivative is available.
     * @param name name of the additional state derivative
     * @return true if the additional state derivative is available
     * @see #addAdditionalStateDerivative(String, CalculusFieldElement...)
     * @see #getAdditionalStateDerivative(String)
     * @see #getAdditionalStatesDerivatives()
     */
    public boolean hasAdditionalStateDerivative(final String name) {
        return additionalDot.getEntry(name) != null;
    }

    /** Check if two instances have the same set of additional states available.
     * <p>
     * Only the names and dimensions of the additional states are compared,
     * not their values.
     * </p>
     * @param state state to compare to instance
     * @exception MathIllegalArgumentException if an additional state does not have
     * the same dimension in both states
     */
    public void ensureCompatibleAdditionalStates(final FieldSpacecraftState<T> state)
        throws MathIllegalArgumentException {

        // check instance additional states is a subset of the other one
        for (final FieldArrayDictionary<T>.Entry entry : additional.getData()) {
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

        // check instance additional states derivatives is a subset of the other one
        for (final FieldArrayDictionary<T>.Entry entry : additionalDot.getData()) {
            final T[] other = state.additionalDot.get(entry.getKey());
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
            for (final FieldArrayDictionary<T>.Entry entry : state.additional.getData()) {
                if (additional.getEntry(entry.getKey()) == null) {
                    throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_STATE,
                                              entry.getKey());
                }
            }
        }

        if (state.additionalDot.size() > additionalDot.size()) {
            // the other state has more additional states
            for (final FieldArrayDictionary<T>.Entry entry : state.additionalDot.getData()) {
                if (additionalDot.getEntry(entry.getKey()) == null) {
                    throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_STATE,
                                              entry.getKey());
                }
            }
        }

    }

    /** Get an additional state.
     * @param name name of the additional state
     * @return value of the additional state
          * @see #addAdditionalState(String, CalculusFieldElement...)
     * @see #hasAdditionalState(String)
     * @see #getAdditionalStates()
     */
    public T[] getAdditionalState(final String name) {
        final FieldArrayDictionary<T>.Entry entry = additional.getEntry(name);
        if (entry == null) {
            throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_STATE, name);
        }
        return entry.getValue();
    }

    /** Get an additional state derivative.
     * @param name name of the additional state derivative
     * @return value of the additional state derivative
     * @see #addAdditionalStateDerivative(String, CalculusFieldElement...)
     * @see #hasAdditionalStateDerivative(String)
     * @see #getAdditionalStatesDerivatives()
     * @since 11.1
     */
    public T[] getAdditionalStateDerivative(final String name) {
        final FieldArrayDictionary<T>.Entry entry = additionalDot.getEntry(name);
        if (entry == null) {
            throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_STATE, name);
        }
        return entry.getValue();
    }

    /** Get an unmodifiable map of additional states.
     * @return unmodifiable map of additional states
     * @see #addAdditionalState(String, CalculusFieldElement...)
     * @see #hasAdditionalState(String)
     * @see #getAdditionalState(String)
     * @deprecated as of 11.1, replaced by {@link #getAdditionalStatesValues()}
     */
    @Deprecated
    public Map<String, T[]> getAdditionalStates() {
        return getAdditionalStatesValues().toMap();
    }

    /** Get an unmodifiable map of additional states.
     * @return unmodifiable map of additional states
     * @see #addAdditionalState(String, CalculusFieldElement...)
     * @see #hasAdditionalState(String)
     * @see #getAdditionalState(String)
     * @since 11.1
     */
    public FieldArrayDictionary<T> getAdditionalStatesValues() {
        return additional.unmodifiableView();
    }

    /** Get an unmodifiable map of additional states derivatives.
     * @return unmodifiable map of additional states derivatives
     * @see #addAdditionalStateDerivative(String, CalculusFieldElement...)
     * @see #hasAdditionalStateDerivative(String)
     * @see #getAdditionalStateDerivative(String)
    * @since 11.1
      */
    public FieldArrayDictionary<T> getAdditionalStatesDerivatives() {
        return additionalDot.unmodifiableView();
    }

    /** Compute the transform from state defining frame to spacecraft frame.
     * <p>The spacecraft frame origin is at the point defined by the orbit,
     * and its orientation is defined by the attitude.</p>
     * @return transform from specified frame to current spacecraft frame
     */
    public FieldTransform<T> toTransform() {
        final TimeStampedFieldPVCoordinates<T> pv = getPVCoordinates();
        return new FieldTransform<>(pv.getDate(),
                                    new FieldTransform<>(pv.getDate(), pv.negate()),
                                    new FieldTransform<>(pv.getDate(), attitude.getOrientation()));
    }

    /** Get the central attraction coefficient.
     * @return mu central attraction coefficient (m^3/s^2), or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather than an orbit
     */
    public T getMu() {
        return (absPva == null) ? orbit.getMu() : absPva.getDate().getField().getZero().add(Double.NaN);
    }

    /** Get the Keplerian period.
     * <p>The Keplerian period is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return keplerian period in seconds, or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     */
    public T getKeplerianPeriod() {
        return (absPva == null) ? orbit.getKeplerianPeriod() : absPva.getDate().getField().getZero().add(Double.NaN);
    }

    /** Get the Keplerian mean motion.
     * <p>The Keplerian mean motion is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return keplerian mean motion in radians per second, or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     */
    public T getKeplerianMeanMotion() {
        return (absPva == null) ? orbit.getKeplerianMeanMotion() : absPva.getDate().getField().getZero().add(Double.NaN);
    }

    /** Get the semi-major axis.
     * @return semi-major axis (m), or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     */
    public T getA() {
        return (absPva == null) ? orbit.getA() : absPva.getDate().getField().getZero().add(Double.NaN);
    }

    /** Get the first component of the eccentricity vector (as per equinoctial parameters).
     * @return e cos(ω + Ω), first component of eccentricity vector, or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     * @see #getE()
     */
    public T getEquinoctialEx() {
        return (absPva == null) ? orbit.getEquinoctialEx() : absPva.getDate().getField().getZero().add(Double.NaN);
    }

    /** Get the second component of the eccentricity vector (as per equinoctial parameters).
     * @return e sin(ω + Ω), second component of the eccentricity vector, or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     * @see #getE()
     */
    public T getEquinoctialEy() {
        return (absPva == null) ? orbit.getEquinoctialEy() : absPva.getDate().getField().getZero().add(Double.NaN);
    }

    /** Get the first component of the inclination vector (as per equinoctial parameters).
     * @return tan(i/2) cos(Ω), first component of the inclination vector, or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     * @see #getI()
     */
    public T getHx() {
        return (absPva == null) ? orbit.getHx() : absPva.getDate().getField().getZero().add(Double.NaN);
    }

    /** Get the second component of the inclination vector (as per equinoctial parameters).
     * @return tan(i/2) sin(Ω), second component of the inclination vector, or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     * @see #getI()
     */
    public T getHy() {
        return (absPva == null) ? orbit.getHy() : absPva.getDate().getField().getZero().add(Double.NaN);
    }

    /** Get the true latitude argument (as per equinoctial parameters).
     * @return v + ω + Ω true longitude argument (rad), or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     * @see #getLE()
     * @see #getLM()
     */
    public T getLv() {
        return (absPva == null) ? orbit.getLv() : absPva.getDate().getField().getZero().add(Double.NaN);
    }

    /** Get the eccentric latitude argument (as per equinoctial parameters).
     * @return E + ω + Ω eccentric longitude argument (rad), or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     * @see #getLv()
     * @see #getLM()
     */
    public T getLE() {
        return (absPva == null) ? orbit.getLE() : absPva.getDate().getField().getZero().add(Double.NaN);
    }

    /** Get the mean longitude argument (as per equinoctial parameters).
     * @return M + ω + Ω mean latitude argument (rad), or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     * @see #getLv()
     * @see #getLE()
     */
    public T getLM() {
        return (absPva == null) ? orbit.getLM() : absPva.getDate().getField().getZero().add(Double.NaN);
    }

    // Additional orbital elements

    /** Get the eccentricity.
     * @return eccentricity, or {code Double.NaN} if the
     * state is contains an absolute position-velocity-acceleration rather
     * than an orbit
     * @see #getEquinoctialEx()
     * @see #getEquinoctialEy()
     */
    public T getE() {
        return (absPva == null) ? orbit.getE() : absPva.getDate().getField().getZero().add(Double.NaN);
    }

    /** Get the inclination.
     * @return inclination (rad)
     * @see #getHx()
     * @see #getHy()
     */
    public T getI() {
        return (absPva == null) ? orbit.getI() : absPva.getDate().getField().getZero().add(Double.NaN);
    }

    /** Get the {@link TimeStampedFieldPVCoordinates} in orbit definition frame.
     * <p>
     * Compute the position and velocity of the satellite. This method caches its
     * results, and recompute them only when the method is called with a new value
     * for mu. The result is provided as a reference to the internally cached
     * {@link TimeStampedFieldPVCoordinates}, so the caller is responsible to copy it in a separate
     * {@link TimeStampedFieldPVCoordinates} if it needs to keep the value for a while.
     * </p>
     * @return pvCoordinates in orbit definition frame
     */
    public TimeStampedFieldPVCoordinates<T> getPVCoordinates() {
        return (absPva == null) ? orbit.getPVCoordinates() : absPva.getPVCoordinates();
    }

    /** Get the {@link TimeStampedFieldPVCoordinates} in given output frame.
     * <p>
     * Compute the position and velocity of the satellite. This method caches its
     * results, and recompute them only when the method is called with a new value
     * for mu. The result is provided as a reference to the internally cached
     * {@link TimeStampedFieldPVCoordinates}, so the caller is responsible to copy it in a separate
     * {@link TimeStampedFieldPVCoordinates} if it needs to keep the value for a while.
     * </p>
     * @param outputFrame frame in which coordinates should be defined
     * @return pvCoordinates in orbit definition frame
     */
    public TimeStampedFieldPVCoordinates<T>getPVCoordinates(final Frame outputFrame) {
        return (absPva == null) ? orbit.getPVCoordinates(outputFrame) : absPva.getPVCoordinates(outputFrame);
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
        final DoubleArrayDictionary dictionary;
        if (additional.size() == 0) {
            dictionary = new DoubleArrayDictionary();
        } else {
            dictionary = new DoubleArrayDictionary(additional.size());
            for (final FieldArrayDictionary<T>.Entry entry : additional.getData()) {
                final double[] array = new double[entry.getValue().length];
                for (int k = 0; k < array.length; ++k) {
                    array[k] = entry.getValue()[k].getReal();
                }
                dictionary.put(entry.getKey(), array);
            }
        }
        final DoubleArrayDictionary dictionaryDot;
        if (additionalDot.size() == 0) {
            dictionaryDot = new DoubleArrayDictionary();
        } else {
            dictionaryDot = new DoubleArrayDictionary(additionalDot.size());
            for (final FieldArrayDictionary<T>.Entry entry : additionalDot.getData()) {
                final double[] array = new double[entry.getValue().length];
                for (int k = 0; k < array.length; ++k) {
                    array[k] = entry.getValue()[k].getReal();
                }
                dictionaryDot.put(entry.getKey(), array);
            }
        }
        if (isOrbitDefined()) {
            return new SpacecraftState(orbit.toOrbit(), attitude.toAttitude(),
                                       mass.getReal(), dictionary, dictionaryDot);
        } else {
            return new SpacecraftState(absPva.toAbsolutePVCoordinates(),
                                       attitude.toAttitude(), mass.getReal(),
                                       dictionary, dictionaryDot);
        }
    }

    @Override
    public String toString() {
        return "FieldSpacecraftState{" +
                "orbit=" + orbit +
                ", attitude=" + attitude +
                ", mass=" + mass +
                ", additional=" + additional +
                ", additionalDot=" + additionalDot +
                '}';
    }

}

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
package org.orekit.propagation;


import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitIllegalStateException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeShiftable;
import org.orekit.time.FieldTimeStamped;
import org.orekit.utils.DataDictionary;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.FieldArrayDictionary;
import org.orekit.utils.FieldDataDictionary;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** This class is the representation of a complete state holding orbit, attitude
 * and mass information at a given date, meant primarily for propagation.
 *
 * <p>It contains an {@link FieldOrbit}, or a {@link FieldAbsolutePVCoordinates} if there
 * is no definite central body, plus the current mass and attitude at the intrinsic
 * {@link FieldAbsoluteDate}. Quantities are guaranteed to be consistent in terms
 * of date and reference frame. The spacecraft state may also contain additional
 * states, which are simply named double arrays which can hold any user-defined
 * data.
 * </p>
 * <p>
 * The state can be slightly shifted to close dates. This actual shift varies
 * between {@link FieldOrbit} and {@link FieldAbsolutePVCoordinates}.
 * For attitude it is a linear extrapolation taking the spin rate into account
 * and no mass change. It is <em>not</em> intended as a replacement for proper
 * orbit and attitude propagation but should be sufficient for either small
 * time shifts or coarse accuracy.
 * </p>
 * <p>
 * The instance {@code FieldSpacecraftState} is guaranteed to be immutable.
 * </p>
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @see SpacecraftState
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 * @author Vincent Mouraux
 * @param <T> type of the field elements
 */
public class FieldSpacecraftState <T extends CalculusFieldElement<T>>
    implements FieldTimeStamped<T>, FieldTimeShiftable<FieldSpacecraftState<T>, T> {

    /** Default mass. */
    private static final double DEFAULT_MASS = 1000.0;

    /**
     * tolerance on date comparison in {@link #checkConsistency(FieldOrbit, FieldAttitude)}. 100 ns
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

    /** Additional data, can be any object (String, T[], etc.). */
    private final FieldDataDictionary<T> additional;

    /** Additional states derivatives.
     * @since 11.1
     */
    private final FieldArrayDictionary<T> additionalDot;

    /** Build a spacecraft state from orbit only.
     * <p>FieldAttitude and mass are set to unspecified non-null arbitrary values.</p>
     * @param orbit the orbit
     */
    public FieldSpacecraftState(final FieldOrbit<T> orbit) {
        this(orbit, SpacecraftState.getDefaultAttitudeProvider(orbit.getFrame())
                        .getAttitude(orbit, orbit.getDate(), orbit.getFrame()),
             orbit.getA().getField().getZero().newInstance(DEFAULT_MASS), null, null);
    }

    /** Build a spacecraft state from orbit and attitude. Kept for performance.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param orbit the orbit
     * @param attitude attitude
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final FieldAttitude<T> attitude)
        throws IllegalArgumentException {
        this(orbit, attitude, orbit.getA().getField().getZero().newInstance(DEFAULT_MASS), null, null);
    }

    /** Create a new instance from orbit and mass.
     * <p>FieldAttitude law is set to an unspecified default attitude.</p>
     * @param orbit the orbit
     * @param mass the mass (kg)
     * @deprecated since 13.0, use withXXX
     */
    @Deprecated
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final T mass) {
        this(orbit, SpacecraftState.getDefaultAttitudeProvider(orbit.getFrame())
                        .getAttitude(orbit, orbit.getDate(), orbit.getFrame()),
             mass, null, null);
    }

    /** Build a spacecraft state from orbit, attitude and mass.
     * @param orbit the orbit
     * @param attitude attitude
     * @param mass the mass (kg)
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     * @deprecated since 13.0, use withXXX
     */
    @Deprecated
    public FieldSpacecraftState(final FieldOrbit<T> orbit, final FieldAttitude<T> attitude, final T mass)
        throws IllegalArgumentException {
        this(orbit, attitude, mass, null, null);
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
                                final FieldDataDictionary<T> additional,
                                final FieldArrayDictionary<T> additionalDot)
        throws IllegalArgumentException {
        checkConsistency(orbit, attitude);
        this.orbit      = orbit;
        this.attitude   = attitude;
        this.mass       = mass;
        this.absPva     = null;
        this.additional = additional == null ? new FieldDataDictionary<>(orbit.getDate().getField()) : new FieldDataDictionary<>(additional);
        this.additionalDot = additionalDot == null ? new FieldArrayDictionary<>(orbit.getDate().getField()) : new FieldArrayDictionary<>(additionalDot);

    }

    /** Convert a {@link FieldSpacecraftState}.
     * @param field field to which the elements belong
     * @param state state to convert
     */
    public FieldSpacecraftState(final Field<T> field, final SpacecraftState state) {

        if (state.isOrbitDefined()) {

            final Orbit nonFieldOrbit = state.getOrbit();
            this.orbit    = nonFieldOrbit.getType().convertToFieldOrbit(field, nonFieldOrbit);
            this.absPva   = null;

        } else {
            final TimeStampedPVCoordinates tspva = state.getPVCoordinates();
            final FieldVector3D<T> position = new FieldVector3D<>(field, tspva.getPosition());
            final FieldVector3D<T> velocity = new FieldVector3D<>(field, tspva.getVelocity());
            final FieldVector3D<T> acceleration = new FieldVector3D<>(field, tspva.getAcceleration());
            final FieldPVCoordinates<T> pva = new FieldPVCoordinates<>(position, velocity, acceleration);
            final FieldAbsoluteDate<T> dateF = new FieldAbsoluteDate<>(field, state.getDate());
            this.orbit  = null;
            this.absPva = new FieldAbsolutePVCoordinates<>(state.getFrame(), dateF, pva);
        }

        this.attitude = new FieldAttitude<>(field, state.getAttitude());
        this.mass     = field.getZero().newInstance(state.getMass());

        final DataDictionary additionalD = state.getAdditionalDataValues();
        if (additionalD.size() == 0) {
            this.additional = new FieldDataDictionary<>(field);
        } else {
            this.additional = new FieldDataDictionary<>(field, additionalD.size());
            for (final DataDictionary.Entry entry : additionalD.getData()) {
                if (entry.getValue() instanceof double[]) {
                    final double[] realValues = (double[]) entry.getValue();
                    final T[] fieldArray = MathArrays.buildArray(field, realValues.length);
                    for (int i = 0; i < fieldArray.length; i++) {
                        fieldArray[i] = field.getZero().add(realValues[i]);
                    }
                    this.additional.put(entry.getKey(), fieldArray);
                } else {
                    this.additional.put(entry.getKey(), entry.getValue());
                }
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
             SpacecraftState.getDefaultAttitudeProvider(absPva.getFrame()).
                     getAttitude(absPva, absPva.getDate(), absPva.getFrame()),
             absPva.getDate().getField().getZero().newInstance(DEFAULT_MASS), null, null);
    }

    /** Build a spacecraft state from orbit and attitude. Kept for performance.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param absPva position-velocity-acceleration
     * @param attitude attitude
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public FieldSpacecraftState(final FieldAbsolutePVCoordinates<T> absPva, final FieldAttitude<T> attitude)
        throws IllegalArgumentException {
        this(absPva, attitude, absPva.getDate().getField().getZero().newInstance(DEFAULT_MASS), null, null);
    }

    /** Create a new instance from orbit and mass.
     * <p>Attitude law is set to an unspecified default attitude.</p>
     * @param absPva position-velocity-acceleration
     * @param mass the mass (kg)
     * @deprecated since 13.0, use withXXX
     */
    @Deprecated
    public FieldSpacecraftState(final FieldAbsolutePVCoordinates<T> absPva, final T mass) {
        this(absPva, SpacecraftState.getDefaultAttitudeProvider(absPva.getFrame())
                        .getAttitude(absPva, absPva.getDate(), absPva.getFrame()),
             mass, null, null);
    }

    /** Build a spacecraft state from orbit, attitude and mass.
     * @param absPva position-velocity-acceleration
     * @param attitude attitude
     * @param mass the mass (kg)
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     * @deprecated since 13.0, use withXXX
     */
    @Deprecated
    public FieldSpacecraftState(final FieldAbsolutePVCoordinates<T> absPva, final FieldAttitude<T> attitude, final T mass)
        throws IllegalArgumentException {
        this(absPva, attitude, mass, null, null);
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
                                final FieldDataDictionary<T> additional, final FieldArrayDictionary<T> additionalDot)
        throws IllegalArgumentException {
        checkConsistency(absPva, attitude);
        this.orbit      = null;
        this.absPva     = absPva;
        this.attitude   = attitude;
        this.mass       = mass;
        this.additional = additional == null ? new FieldDataDictionary<>(absPva.getDate().getField()) : new FieldDataDictionary<>(additional);
        this.additionalDot = additionalDot == null ? new FieldArrayDictionary<>(absPva.getDate().getField()) : new FieldArrayDictionary<>(additionalDot);
    }

    /**
     * Create a new instance with input mass.
     * @param newMass mass
     * @return new state
     * @since 13.0
     */
    public FieldSpacecraftState<T> withMass(final T newMass) {
        if (isOrbitDefined()) {
            return new FieldSpacecraftState<>(orbit, attitude, newMass, additional, additionalDot);
        } else {
            return new FieldSpacecraftState<>(absPva, attitude, newMass, additional, additionalDot);
        }
    }

    /**
     * Create a new instance with input attitude.
     * @param newAttitude attitude
     * @return new state
     * @since 13.0
     */
    public FieldSpacecraftState<T> withAttitude(final FieldAttitude<T> newAttitude) {
        if (isOrbitDefined()) {
            return new FieldSpacecraftState<>(orbit, newAttitude, mass, additional, additionalDot);
        } else {
            return new FieldSpacecraftState<>(absPva, newAttitude, mass, additional, additionalDot);
        }
    }

    /**
     * Create a new instance with input additional data.
     * @param newAdditional data
     * @return new state
     * @since 13.0
     */
    public FieldSpacecraftState<T> withAdditionalData(final FieldDataDictionary<T> newAdditional) {
        if (isOrbitDefined()) {
            return new FieldSpacecraftState<>(orbit, attitude, mass, newAdditional, additionalDot);
        } else {
            return new FieldSpacecraftState<>(absPva, attitude, mass, newAdditional, additionalDot);
        }
    }

    /**
     * Create a new instance with input additional data.
     * @param newAdditionalDot additional derivatives
     * @return new state
     * @since 13.0
     */
    public FieldSpacecraftState<T> withAdditionalStatesDerivatives(final FieldArrayDictionary<T> newAdditionalDot) {
        if (isOrbitDefined()) {
            return new FieldSpacecraftState<>(orbit, attitude, mass, additional, newAdditionalDot);
        } else {
            return new FieldSpacecraftState<>(absPva, attitude, mass, additional, newAdditionalDot);
        }
    }

    /** Add an additional data.
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
     * @param name name of the additional data (names containing "orekit"
     *      * with any case are reserved for the library internal use)
     * @param value value of the additional data
     * @return a new instance, with the additional data added
     * @see #hasAdditionalData(String)
     * @see #getAdditionalData(String)
     * @see #getAdditionalDataValues()
     */
    @SuppressWarnings("unchecked") // cast including generic type is checked and unitary tested
    public final FieldSpacecraftState<T> addAdditionalData(final String name, final Object value) {
        final FieldDataDictionary<T> newDict = new FieldDataDictionary<>(additional);
        if (value instanceof CalculusFieldElement[]) {
            final CalculusFieldElement<T>[] valueArray = (CalculusFieldElement<T>[]) value;
            newDict.put(name, valueArray.clone());
        } else if (value instanceof CalculusFieldElement) {
            final CalculusFieldElement<T>[] valueArray = MathArrays.buildArray(mass.getField(), 1);
            valueArray[0] = (CalculusFieldElement<T>) value;
            newDict.put(name, valueArray);
        } else {
            newDict.put(name, value);
        }
        return withAdditionalData(newDict);
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
        return withAdditionalStatesDerivatives(newDict);
    }

    /** Check orbit and attitude dates are equal.
     * @param orbitN the orbit
     * @param attitudeN attitude
     * @exception IllegalArgumentException if orbit and attitude dates
     * are not equal
     */
    private void checkConsistency(final FieldOrbit<T> orbitN, final FieldAttitude<T> attitudeN)
        throws IllegalArgumentException {
        checkDateAndFrameConsistency(attitudeN, orbitN.getDate(), orbitN.getFrame());
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
        checkDateAndFrameConsistency(attitude, absPva.getDate(), absPva.getFrame());
    }

    /** Check attitude frame and epoch.
     * @param attitude attitude
     * @param date epoch to verify
     * @param frame frame to verify
     * @param <T> type of the elements
     */
    private static <T extends CalculusFieldElement<T>> void checkDateAndFrameConsistency(final FieldAttitude<T> attitude,
                                                                                         final FieldAbsoluteDate<T> date,
                                                                                         final Frame frame) {
        if (date.durationFrom(attitude.getDate()).abs().getReal() >
                DATE_INCONSISTENCY_THRESHOLD) {
            throw new OrekitIllegalArgumentException(OrekitMessages.ORBIT_AND_ATTITUDE_DATES_MISMATCH,
                    date, attitude.getDate());
        }
        if (frame != attitude.getReferenceFrame()) {
            throw new OrekitIllegalArgumentException(OrekitMessages.FRAMES_MISMATCH,
                    frame.getName(),
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
    @Override
    public FieldSpacecraftState<T> shiftedBy(final double dt) {
        if (isOrbitDefined()) {
            return new FieldSpacecraftState<>(orbit.shiftedBy(dt), attitude.shiftedBy(dt),
                                              mass, shiftAdditional(dt), additionalDot);
        } else {
            return new FieldSpacecraftState<>(absPva.shiftedBy(dt), attitude.shiftedBy(dt),
                                              mass, shiftAdditional(dt), additionalDot);
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
    @Override
    public FieldSpacecraftState<T> shiftedBy(final T dt) {
        if (isOrbitDefined()) {
            return new FieldSpacecraftState<>(orbit.shiftedBy(dt), attitude.shiftedBy(dt),
                                              mass, shiftAdditional(dt), additionalDot);
        } else {
            return new FieldSpacecraftState<>(absPva.shiftedBy(dt), attitude.shiftedBy(dt),
                                              mass, shiftAdditional(dt), additionalDot);
        }
    }

    /** Shift additional data.
     * @param dt time shift in seconds
     * @return shifted additional data
     * @since 11.1.1
     */
    private FieldDataDictionary<T> shiftAdditional(final double dt) {

        // fast handling when there are no derivatives at all
        if (additionalDot.size() == 0) {
            return additional;
        }

        // there are derivatives, we need to take them into account in the additional state
        final FieldDataDictionary<T> shifted = new FieldDataDictionary<>(additional);
        for (final FieldArrayDictionary<T>.Entry dotEntry : additionalDot.getData()) {
            final FieldDataDictionary<T>.Entry entry = shifted.getEntry(dotEntry.getKey());
            if (entry != null) {
                entry.scaledIncrement(dt, dotEntry);
            }
        }

        return shifted;

    }

    /** Shift additional states.
     * @param dt time shift in seconds
     * @return shifted additional states
     * @since 11.1.1
     */
    private FieldDataDictionary<T> shiftAdditional(final T dt) {

        // fast handling when there are no derivatives at all
        if (additionalDot.size() == 0) {
            return additional;
        }

        // there are derivatives, we need to take them into account in the additional state
        final FieldDataDictionary<T> shifted = new FieldDataDictionary<>(additional);
        for (final FieldArrayDictionary<T>.Entry dotEntry : additionalDot.getData()) {
            final FieldDataDictionary<T>.Entry entry = shifted.getEntry(dotEntry.getKey());
            if (entry != null) {
                entry.scaledIncrement(dt, dotEntry);
            }
        }

        return shifted;

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
        if (isOrbitDefined()) {
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

    /** {@inheritDoc} */
    @Override
    public FieldAbsoluteDate<T> getDate() {
        return isOrbitDefined() ? orbit.getDate() : absPva.getDate();
    }

    /** Get the defining frame.
     * @return the frame in which state is defined
     */
    public Frame getFrame() {
        return isOrbitDefined() ? orbit.getFrame() : absPva.getFrame();
    }


    /** Check if an additional data is available.
     * @param name name of the additional data
     * @return true if the additional data is available
     * @see #addAdditionalData(String, Object)
     * @see #getAdditionalData(String)
     * @see #getAdditionalDataValues()
     */
    public boolean hasAdditionalData(final String name) {
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
    @SuppressWarnings("unchecked") // cast including generic type is checked and unitary tested
    public void ensureCompatibleAdditionalStates(final FieldSpacecraftState<T> state)
        throws MathIllegalArgumentException {

        // check instance additional states is a subset of the other one
        for (final FieldDataDictionary<T>.Entry entry : additional.getData()) {
            final Object other = state.additional.get(entry.getKey());
            if (other == null) {
                throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_DATA,
                                          entry.getKey());
            }
            if (other instanceof CalculusFieldElement[]) {
                final CalculusFieldElement<T>[] arrayOther = (CalculusFieldElement<T>[]) other;
                final CalculusFieldElement<T>[] arrayEntry = (CalculusFieldElement<T>[]) entry.getValue();
                if (arrayEntry.length != arrayOther.length) {
                    throw new MathIllegalStateException(LocalizedCoreFormats.DIMENSIONS_MISMATCH, arrayOther.length, arrayEntry.length);
                }
            }
        }

        // check instance additional states derivatives is a subset of the other one
        for (final FieldArrayDictionary<T>.Entry entry : additionalDot.getData()) {
            final T[] other = state.additionalDot.get(entry.getKey());
            if (other == null) {
                throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_DATA,
                                          entry.getKey());
            }
            if (other.length != entry.getValue().length) {
                throw new MathIllegalStateException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                                    other.length, entry.getValue().length);
            }
        }

        if (state.additional.size() > additional.size()) {
            // the other state has more additional states
            for (final FieldDataDictionary<T>.Entry entry : state.additional.getData()) {
                if (additional.getEntry(entry.getKey()) == null) {
                    throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_DATA,
                                              entry.getKey());
                }
            }
        }

        if (state.additionalDot.size() > additionalDot.size()) {
            // the other state has more additional states
            for (final FieldArrayDictionary<T>.Entry entry : state.additionalDot.getData()) {
                if (additionalDot.getEntry(entry.getKey()) == null) {
                    throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_DATA,
                                              entry.getKey());
                }
            }
        }

    }

    /**
     * Get an additional state.
     *
     * @param name name of the additional state
     * @return value of the additional state
     * @see #hasAdditionalData(String)
     * @see #getAdditionalDataValues()
     */
    @SuppressWarnings("unchecked") // cast including generic type is checked and unitary tested
    public T[] getAdditionalState(final String name) {
        final Object data = getAdditionalData(name);
        if (data instanceof CalculusFieldElement[]) {
            return (T[]) data;
        } else if (data instanceof CalculusFieldElement) {
            final T[] values = MathArrays.buildArray(mass.getField(), 1);
            values[0] = (T) data;
            return values;
        } else {
            throw new OrekitException(OrekitMessages.ADDITIONAL_STATE_BAD_TYPE, name);
        }
    }


    /**
     * Get an additional data.
     *
     * @param name name of the additional state
     * @return value of the additional state
     * @see #addAdditionalData(String, Object)
     * @see #hasAdditionalData(String)
     * @see #getAdditionalDataValues()
     * @since 13.0
     */
    public Object getAdditionalData(final String name) {
        final FieldDataDictionary<T>.Entry entry = additional.getEntry(name);
        if (entry == null) {
            throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_DATA, name);
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
            throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_DATA, name);
        }
        return entry.getValue();
    }

    /** Get an unmodifiable map of additional states.
     * @return unmodifiable map of additional states
     * @see #addAdditionalData(String, Object)
     * @see #hasAdditionalData(String)
     * @see #getAdditionalData(String)
     * @since 11.1
     */
    public FieldDataDictionary<T> getAdditionalDataValues() {
        return additional;
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

    /** Compute the static transform from state defining frame to spacecraft frame.
     * @return static transform from specified frame to current spacecraft frame
     * @see #toTransform()
     * @since 12.0
     */
    public FieldStaticTransform<T> toStaticTransform() {
        return FieldStaticTransform.of(getDate(), getPosition().negate(), attitude.getRotation());
    }

    /** Get the position in state definition frame.
     * @return position in state definition frame
     * @since 12.0
     */
    public FieldVector3D<T> getPosition() {
        return isOrbitDefined() ? orbit.getPosition() : absPva.getPosition();
    }

    /** Get the velocity in state definition frame.
     * @return velocity in state definition frame
     * @since 13.1
     */
    public FieldVector3D<T> getVelocity() {
        return isOrbitDefined() ? orbit.getVelocity() : absPva.getVelocity();
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
        return isOrbitDefined() ? orbit.getPVCoordinates() : absPva.getPVCoordinates();
    }

    /** Get the position in given output frame.
     * @param outputFrame frame in which position should be defined
     * @return position in given output frame
     * @since 12.0
     * @see #getPVCoordinates(Frame)
     */
    public FieldVector3D<T> getPosition(final Frame outputFrame) {
        return isOrbitDefined() ? orbit.getPosition(outputFrame) : absPva.getPosition(outputFrame);
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
    public TimeStampedFieldPVCoordinates<T> getPVCoordinates(final Frame outputFrame) {
        return isOrbitDefined() ? orbit.getPVCoordinates(outputFrame) : absPva.getPVCoordinates(outputFrame);
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
    @SuppressWarnings("unchecked") // cast including generic type is checked and unitary tested
    public SpacecraftState toSpacecraftState() {
        final DataDictionary dictionary;
        if (additional.size() == 0) {
            dictionary = new DataDictionary();
        } else {
            dictionary = new DataDictionary(additional.size());
            for (final FieldDataDictionary<T>.Entry entry : additional.getData()) {
                if (entry.getValue() instanceof CalculusFieldElement[]) {
                    final CalculusFieldElement<T>[] entryArray  = (CalculusFieldElement<T>[]) entry.getValue();
                    final double[] realArray = new double[entryArray.length];
                    for (int k = 0; k < realArray.length; ++k) {
                        realArray[k] = entryArray[k].getReal();
                    }
                    dictionary.put(entry.getKey(), realArray);
                } else {
                    dictionary.put(entry.getKey(), entry.getValue());
                }
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

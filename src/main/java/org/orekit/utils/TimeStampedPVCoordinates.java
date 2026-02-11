/* Copyright 2002-2026 CS GROUP
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
package org.orekit.utils;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Derivative;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeOffset;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeStamped;

/** {@link TimeStamped time-stamped} version of {@link PVCoordinates}.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 * @since 7.0
 */
public class TimeStampedPVCoordinates extends PVCoordinates implements TimeStamped {

    /** The date. */
    private final AbsoluteDate date;

    /** Builds a TimeStampedPVCoordinates pair.
     * @param date coordinates date
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     * @param acceleration the acceleration vector (m/s²)
     */
    public TimeStampedPVCoordinates(final AbsoluteDate date, final Vector3D position,
                                    final Vector3D velocity, final Vector3D acceleration) {
        super(position, velocity, acceleration);
        this.date = date;
    }

    /**
     * Build from position and velocity. Acceleration is set to zero.
     *
     * @param date coordinates date
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     */
    public TimeStampedPVCoordinates(final AbsoluteDate date, final Vector3D position, final Vector3D velocity) {
        this(date, position, velocity, Vector3D.ZERO);
    }

    /**
     * Build from position velocity acceleration coordinates.
     *
     * @param date coordinates date
     * @param pv position velocity, and acceleration coordinates, in meters and seconds.
     */
    public TimeStampedPVCoordinates(final AbsoluteDate date, final PVCoordinates pv) {
        this(date, pv.getPosition(), pv.getVelocity(), pv.getAcceleration());
    }

    /** Multiplicative constructor
     * <p>Build a TimeStampedPVCoordinates from another one and a scale factor.</p>
     * <p>The TimeStampedPVCoordinates built will be a * pv</p>
     * @param date date of the built coordinates
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public TimeStampedPVCoordinates(final AbsoluteDate date,
                                    final double a, final PVCoordinates pv) {
        this(date, new PVCoordinates(a, pv));
    }

    /** Subtractive constructor
     * <p>Build a relative TimeStampedPVCoordinates from a start and an end position.</p>
     * <p>The TimeStampedPVCoordinates built will be end - start.</p>
     * @param date date of the built coordinates
     * @param start Starting PVCoordinates
     * @param end ending PVCoordinates
     */
    public TimeStampedPVCoordinates(final AbsoluteDate date,
                                    final PVCoordinates start, final PVCoordinates end) {
        this(date, new PVCoordinates(start, end));
    }

    /** Linear constructor
     * <p>Build a TimeStampedPVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The TimeStampedPVCoordinates built will be a1 * u1 + a2 * u2</p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public TimeStampedPVCoordinates(final AbsoluteDate date,
                                    final double a1, final PVCoordinates pv1,
                                    final double a2, final PVCoordinates pv2) {
        this(date, new PVCoordinates(a1, pv1, a2, pv2));
    }

    /** Linear constructor
     * <p>Build a TimeStampedPVCoordinates from three other ones and corresponding scale factors.</p>
     * <p>The TimeStampedPVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3</p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     */
    public TimeStampedPVCoordinates(final AbsoluteDate date,
                                    final double a1, final PVCoordinates pv1,
                                    final double a2, final PVCoordinates pv2,
                                    final double a3, final PVCoordinates pv3) {
        this(date, new PVCoordinates(a1, pv1, a2, pv2, a3, pv3));
    }

    /** Linear constructor
     * <p>Build a TimeStampedPVCoordinates from four other ones and corresponding scale factors.</p>
     * <p>The TimeStampedPVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4</p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     * @param a4 fourth scale factor
     * @param pv4 fourth base (unscaled) PVCoordinates
     */
    public TimeStampedPVCoordinates(final AbsoluteDate date,
                                    final double a1, final PVCoordinates pv1,
                                    final double a2, final PVCoordinates pv2,
                                    final double a3, final PVCoordinates pv3,
                                    final double a4, final PVCoordinates pv4) {
        this(date, new PVCoordinates(a1, pv1, a2, pv2, a3, pv3, a4, pv4));
    }

    /** Builds a TimeStampedPVCoordinates triplet from  a {@link FieldVector3D}&lt;{@link Derivative}&gt;.
     * <p>
     * The vector components must have time as their only derivation parameter and
     * have consistent derivation orders.
     * </p>
     * @param date date of the built coordinates
     * @param p vector with time-derivatives embedded within the coordinates
     * @param <U> type of the derivative
     */
    public <U extends Derivative<U>> TimeStampedPVCoordinates(final AbsoluteDate date, final FieldVector3D<U> p) {
        super(p);
        this.date = date;
    }

    /** {@inheritDoc} */
    public AbsoluteDate getDate() {
        return date;
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple Taylor expansion. It is <em>not</em> intended as a replacement for
     * proper orbit propagation (it is not even Keplerian!) but should be sufficient
     * for either small time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    @Override
    public TimeStampedPVCoordinates shiftedBy(final double dt) {
        final PVCoordinates spv = super.shiftedBy(dt);
        return new TimeStampedPVCoordinates(date.shiftedBy(dt),
                                            spv.getPosition(), spv.getVelocity(), spv.getAcceleration());
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple Taylor expansion. It is <em>not</em> intended as a replacement for
     * proper orbit propagation (it is not even Keplerian!) but should be sufficient
     * for either small time shifts or coarse accuracy.
     * </p>
     * @param dt time shift
     * @return a new state, shifted with respect to the instance (which is immutable)
     * @since 13.0
     */
    @Override
    public TimeStampedPVCoordinates shiftedBy(final TimeOffset dt) {
        final PVCoordinates spv = super.shiftedBy(dt);
        return new TimeStampedPVCoordinates(date.shiftedBy(dt),
                                            spv.getPosition(), spv.getVelocity(), spv.getAcceleration());
    }

    /** Create a local provider using simply Taylor expansion through {@link #shiftedBy(double)}.
     * <p>
     * The time evolution is based on a simple Taylor expansion. It is <em>not</em> intended as a
     * replacement for proper orbit propagation (it is not even Keplerian!) but should be sufficient
     * for either small time shifts or coarse accuracy.
     * </p>
     * @param instanceFrame frame in which the instance is defined
     * @return provider based on Taylor expansion, for small time shifts around instance date
     */
    public PVCoordinatesProvider toTaylorProvider(final Frame instanceFrame) {
        return new AbsolutePVCoordinates(instanceFrame, this);
    }

    @Override
    public TimeStampedFieldPVCoordinates<DerivativeStructure> toDerivativeStructurePV(final int order) {
        final FieldPVCoordinates<DerivativeStructure> fieldPV = super.toDerivativeStructurePV(order);
        final Field<DerivativeStructure> field = fieldPV.getPosition().getX().getField();
        final DerivativeStructure dt = field.getZero().getFactory().variable(0, 1.);
        final FieldAbsoluteDate<DerivativeStructure> fieldDate = new FieldAbsoluteDate<>(field, date).shiftedBy(dt);
        return new TimeStampedFieldPVCoordinates<>(fieldDate, fieldPV);
    }

    @Override
    public TimeStampedFieldPVCoordinates<UnivariateDerivative1> toUnivariateDerivative1PV() {
        final FieldPVCoordinates<UnivariateDerivative1> fieldPV = super.toUnivariateDerivative1PV();
        final Field<UnivariateDerivative1> field = fieldPV.getPosition().getX().getField();
        final FieldAbsoluteDate<UnivariateDerivative1> fieldDate = new FieldAbsoluteDate<>(field, date)
                .shiftedBy(new UnivariateDerivative1(0., 1.));
        return new TimeStampedFieldPVCoordinates<>(fieldDate, fieldPV);
    }

    @Override
    public TimeStampedFieldPVCoordinates<UnivariateDerivative2> toUnivariateDerivative2PV() {
        final FieldPVCoordinates<UnivariateDerivative2> fieldPV = super.toUnivariateDerivative2PV();
        final Field<UnivariateDerivative2> field = fieldPV.getPosition().getX().getField();
        final FieldAbsoluteDate<UnivariateDerivative2> fieldDate = new FieldAbsoluteDate<>(field, date)
                .shiftedBy(new UnivariateDerivative2(0., 1., 0.));
        return new TimeStampedFieldPVCoordinates<>(fieldDate, fieldPV);
    }

    /** Return a string representation of this date, position, velocity, and acceleration.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @return string representation of this.
     */
    @Override
    @DefaultDataContext
    public String toString() {
        return toString(DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Return a string representation of this date, position, velocity, and acceleration.
     *
     * @param utc time scale used to print the date.
     * @return string representation of this.
     */
    public String toString(final TimeScale utc) {
        final String comma = ", ";
        return '{' +
                date.toString(utc) + ", P(" +
                getPosition().getX() + comma +
                getPosition().getY() + comma +
                getPosition().getZ() + "), V(" +
                getVelocity().getX() + comma +
                getVelocity().getY() + comma +
                getVelocity().getZ() + "), A(" +
                getAcceleration().getX() + comma +
                getAcceleration().getY() + comma +
                getAcceleration().getZ() + ")}";
    }

}

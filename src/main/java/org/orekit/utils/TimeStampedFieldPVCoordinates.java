/* Copyright 2002-2023 CS GROUP
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.FieldDerivative;
import org.hipparchus.analysis.differentiation.FieldDerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeStamped;

/** {@link TimeStamped time-stamped} version of {@link FieldPVCoordinates}.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @param <T> the type of the field elements
 * @author Luc Maisonobe
 * @since 7.0
 */
public class TimeStampedFieldPVCoordinates<T extends CalculusFieldElement<T>>
    extends FieldPVCoordinates<T> implements FieldTimeStamped<T> {

    /** The date. */
    private final FieldAbsoluteDate<T> date;

    /** Builds a PVCoordinates pair.
     * @param date coordinates date
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     * @param acceleration the acceleration vector (m/s²)
     */
    public TimeStampedFieldPVCoordinates(final AbsoluteDate date,
                                         final FieldVector3D<T> position,
                                         final FieldVector3D<T> velocity,
                                         final FieldVector3D<T> acceleration) {
        this(new FieldAbsoluteDate<>(position.getX().getField(), date),
             position, velocity, acceleration);
    }

    /** Builds a PVCoordinates pair.
     * @param date coordinates date
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     * @param acceleration the acceleration vector (m/s²)
     */
    public TimeStampedFieldPVCoordinates(final FieldAbsoluteDate<T> date,
                                         final FieldVector3D<T> position,
                                         final FieldVector3D<T> velocity,
                                         final FieldVector3D<T> acceleration) {
        super(position, velocity, acceleration);
        this.date = date;
    }

    /** Basic constructor.
     * <p>Build a PVCoordinates from another one at a given date</p>
     * <p>The PVCoordinates built will be pv</p>
     * @param date date of the built coordinates
     * @param pv base (unscaled) PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final AbsoluteDate date, final FieldPVCoordinates<T> pv) {
        this(new FieldAbsoluteDate<>(pv.getPosition().getX().getField(), date), pv);
    }

    /** Basic constructor.
     * <p>Build a PVCoordinates from another one at a given date</p>
     * <p>The PVCoordinates built will be pv</p>
     * @param date date of the built coordinates
     * @param pv base (unscaled) PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final FieldAbsoluteDate<T> date, final FieldPVCoordinates<T> pv) {
        super(pv.getPosition(),
              pv.getVelocity(),
              pv.getAcceleration());
        this.date = date;
    }

    /** Constructor from Field and TimeStampedPVCoordinates.
     * <p>Build a TimeStampedFieldPVCoordinates from non-Field one.</p>
     * @param field CalculusField to base object on
     * @param pv non-field, time-stamped Position-Velocity coordinates
     */
    public TimeStampedFieldPVCoordinates(final Field<T> field, final TimeStampedPVCoordinates pv) {
        this(pv.getDate(), new FieldPVCoordinates<T>(field, pv));
    }

    /** Multiplicative constructor
     * <p>Build a PVCoordinates from another one and a scale factor.</p>
     * <p>The PVCoordinates built will be a * pv</p>
     * @param date date of the built coordinates
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final AbsoluteDate date,
                                         final double a, final FieldPVCoordinates<T> pv) {
        this(new FieldAbsoluteDate<>(pv.getPosition().getX().getField(), date), a, pv);
    }

    /** Multiplicative constructor
     * <p>Build a PVCoordinates from another one and a scale factor.</p>
     * <p>The PVCoordinates built will be a * pv</p>
     * @param date date of the built coordinates
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final FieldAbsoluteDate<T> date,
                                         final double a, final FieldPVCoordinates<T> pv) {
        super(new FieldVector3D<>(a, pv.getPosition()),
              new FieldVector3D<>(a, pv.getVelocity()),
              new FieldVector3D<>(a, pv.getAcceleration()));
        this.date = date;
    }

    /** Multiplicative constructor
     * <p>Build a PVCoordinates from another one and a scale factor.</p>
     * <p>The PVCoordinates built will be a * pv</p>
     * @param date date of the built coordinates
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final AbsoluteDate date,
                                         final T a, final FieldPVCoordinates<T> pv) {
        this(new FieldAbsoluteDate<>(a.getField(), date), a, pv);
    }

    /** Multiplicative constructor
     * <p>Build a PVCoordinates from another one and a scale factor.</p>
     * <p>The PVCoordinates built will be a * pv</p>
     * @param date date of the built coordinates
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final FieldAbsoluteDate<T> date,
                                         final T a, final FieldPVCoordinates<T> pv) {
        super(new FieldVector3D<>(a, pv.getPosition()),
              new FieldVector3D<>(a, pv.getVelocity()),
              new FieldVector3D<>(a, pv.getAcceleration()));
        this.date = date;
    }

    /** Multiplicative constructor
     * <p>Build a PVCoordinates from another one and a scale factor.</p>
     * <p>The PVCoordinates built will be a * pv</p>
     * @param date date of the built coordinates
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final AbsoluteDate date,
                                         final T a, final PVCoordinates pv) {
        this(new FieldAbsoluteDate<>(a.getField(), date), a, pv);
    }

    /** Multiplicative constructor
     * <p>Build a PVCoordinates from another one and a scale factor.</p>
     * <p>The PVCoordinates built will be a * pv</p>
     * @param date date of the built coordinates
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final FieldAbsoluteDate<T> date,
                                         final T a, final PVCoordinates pv) {
        super(new FieldVector3D<>(a, pv.getPosition()),
              new FieldVector3D<>(a, pv.getVelocity()),
              new FieldVector3D<>(a, pv.getAcceleration()));
        this.date = date;
    }

    /** Subtractive constructor
     * <p>Build a relative PVCoordinates from a start and an end position.</p>
     * <p>The PVCoordinates built will be end - start.</p>
     * @param date date of the built coordinates
     * @param start Starting PVCoordinates
     * @param end ending PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final AbsoluteDate date,
                                         final FieldPVCoordinates<T> start, final FieldPVCoordinates<T> end) {
        this(new FieldAbsoluteDate<>(start.getPosition().getX().getField(), date), start, end);
    }

    /** Subtractive constructor
     * <p>Build a relative PVCoordinates from a start and an end position.</p>
     * <p>The PVCoordinates built will be end - start.</p>
     * @param date date of the built coordinates
     * @param start Starting PVCoordinates
     * @param end ending PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final FieldAbsoluteDate<T> date,
                                         final FieldPVCoordinates<T> start, final FieldPVCoordinates<T> end) {
        super(end.getPosition().subtract(start.getPosition()),
              end.getVelocity().subtract(start.getVelocity()),
              end.getAcceleration().subtract(start.getAcceleration()));
        this.date = date;
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2</p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final AbsoluteDate date,
                                         final double a1, final FieldPVCoordinates<T> pv1,
                                         final double a2, final FieldPVCoordinates<T> pv2) {
        this(new FieldAbsoluteDate<>(pv1.getPosition().getX().getField(), date),
             a1, pv1, a2, pv2);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2</p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final FieldAbsoluteDate<T> date,
                                         final double a1, final FieldPVCoordinates<T> pv1,
                                         final double a2, final FieldPVCoordinates<T> pv2) {
        super(new FieldVector3D<>(a1, pv1.getPosition(),     a2, pv2.getPosition()),
              new FieldVector3D<>(a1, pv1.getVelocity(),     a2, pv2.getVelocity()),
              new FieldVector3D<>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration()));
        this.date = date;
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2</p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final AbsoluteDate date,
                                         final T a1, final FieldPVCoordinates<T> pv1,
                                         final T a2, final FieldPVCoordinates<T> pv2) {
        this(new FieldAbsoluteDate<>(a1.getField(), date),
             a1, pv1, a2, pv2);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2</p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final FieldAbsoluteDate<T> date,
                                         final T a1, final FieldPVCoordinates<T> pv1,
                                         final T a2, final FieldPVCoordinates<T> pv2) {
        super(new FieldVector3D<>(a1, pv1.getPosition(),     a2, pv2.getPosition()),
              new FieldVector3D<>(a1, pv1.getVelocity(),     a2, pv2.getVelocity()),
              new FieldVector3D<>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration()));
        this.date = date;
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2</p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final AbsoluteDate date,
                                         final T a1, final PVCoordinates pv1,
                                         final T a2, final PVCoordinates pv2) {
        this(new FieldAbsoluteDate<>(a1.getField(), date),
             a1, pv1, a2, pv2);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2</p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final FieldAbsoluteDate<T> date,
                                         final T a1, final PVCoordinates pv1,
                                         final T a2, final PVCoordinates pv2) {
        super(new FieldVector3D<>(a1, pv1.getPosition(),     a2, pv2.getPosition()),
              new FieldVector3D<>(a1, pv1.getVelocity(),     a2, pv2.getVelocity()),
              new FieldVector3D<>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration()));
        this.date = date;
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from three other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3</p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final AbsoluteDate date,
                                         final double a1, final FieldPVCoordinates<T> pv1,
                                         final double a2, final FieldPVCoordinates<T> pv2,
                                         final double a3, final FieldPVCoordinates<T> pv3) {
        this(new FieldAbsoluteDate<>(pv1.getPosition().getX().getField(), date),
             a1, pv1, a2, pv2, a3, pv3);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from three other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3</p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final FieldAbsoluteDate<T> date,
                                         final double a1, final FieldPVCoordinates<T> pv1,
                                         final double a2, final FieldPVCoordinates<T> pv2,
                                         final double a3, final FieldPVCoordinates<T> pv3) {
        super(new FieldVector3D<>(a1, pv1.getPosition(),     a2, pv2.getPosition(),     a3, pv3.getPosition()),
              new FieldVector3D<>(a1, pv1.getVelocity(),     a2, pv2.getVelocity(),     a3, pv3.getVelocity()),
              new FieldVector3D<>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration(), a3, pv3.getAcceleration()));
        this.date = date;
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from three other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3</p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final AbsoluteDate date,
                                         final T a1, final FieldPVCoordinates<T> pv1,
                                         final T a2, final FieldPVCoordinates<T> pv2,
                                         final T a3, final FieldPVCoordinates<T> pv3) {
        this(new FieldAbsoluteDate<>(a1.getField(), date),
             a1, pv1, a2, pv2, a3, pv3);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from three other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3</p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final FieldAbsoluteDate<T> date,
                                         final T a1, final FieldPVCoordinates<T> pv1,
                                         final T a2, final FieldPVCoordinates<T> pv2,
                                         final T a3, final FieldPVCoordinates<T> pv3) {
        super(new FieldVector3D<>(a1, pv1.getPosition(),     a2, pv2.getPosition(),     a3, pv3.getPosition()),
              new FieldVector3D<>(a1, pv1.getVelocity(),     a2, pv2.getVelocity(),     a3, pv3.getVelocity()),
              new FieldVector3D<>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration(), a3, pv3.getAcceleration()));
        this.date = date;
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from three other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3</p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final AbsoluteDate date,
                                         final T a1, final PVCoordinates pv1,
                                         final T a2, final PVCoordinates pv2,
                                         final T a3, final PVCoordinates pv3) {
        this(new FieldAbsoluteDate<>(a1.getField(), date),
             a1, pv1, a2, pv2, a3, pv3);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from three other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3</p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     */
    public TimeStampedFieldPVCoordinates(final FieldAbsoluteDate<T> date,
                                         final T a1, final PVCoordinates pv1,
                                         final T a2, final PVCoordinates pv2,
                                         final T a3, final PVCoordinates pv3) {
        super(new FieldVector3D<>(a1, pv1.getPosition(),     a2, pv2.getPosition(),     a3, pv3.getPosition()),
              new FieldVector3D<>(a1, pv1.getVelocity(),     a2, pv2.getVelocity(),     a3, pv3.getVelocity()),
              new FieldVector3D<>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration(), a3, pv3.getAcceleration()));
        this.date = date;
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from four other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4</p>
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
    public TimeStampedFieldPVCoordinates(final AbsoluteDate date,
                                         final double a1, final FieldPVCoordinates<T> pv1,
                                         final double a2, final FieldPVCoordinates<T> pv2,
                                         final double a3, final FieldPVCoordinates<T> pv3,
                                         final double a4, final FieldPVCoordinates<T> pv4) {
        this(new FieldAbsoluteDate<>(pv1.getPosition().getX().getField(), date),
             a1, pv1, a2, pv2, a3, pv3, a4, pv4);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from four other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4</p>
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
    public TimeStampedFieldPVCoordinates(final FieldAbsoluteDate<T> date,
                                         final double a1, final FieldPVCoordinates<T> pv1,
                                         final double a2, final FieldPVCoordinates<T> pv2,
                                         final double a3, final FieldPVCoordinates<T> pv3,
                                         final double a4, final FieldPVCoordinates<T> pv4) {
        super(new FieldVector3D<>(a1, pv1.getPosition(),     a2, pv2.getPosition(),
                                  a3, pv3.getPosition(),     a4, pv4.getPosition()),
              new FieldVector3D<>(a1, pv1.getVelocity(),     a2, pv2.getVelocity(),
                                  a3, pv3.getVelocity(),     a4, pv4.getVelocity()),
              new FieldVector3D<>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration(),
                                  a3, pv3.getAcceleration(), a4, pv4.getAcceleration()));
        this.date = date;
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from four other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4</p>
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
    public TimeStampedFieldPVCoordinates(final AbsoluteDate date,
                                         final T a1, final FieldPVCoordinates<T> pv1,
                                         final T a2, final FieldPVCoordinates<T> pv2,
                                         final T a3, final FieldPVCoordinates<T> pv3,
                                         final T a4, final FieldPVCoordinates<T> pv4) {
        this(new FieldAbsoluteDate<>(a1.getField(), date),
             a1, pv1, a2, pv2, a3, pv3, a4, pv4);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from four other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4</p>
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
    public TimeStampedFieldPVCoordinates(final FieldAbsoluteDate<T> date,
                                         final T a1, final FieldPVCoordinates<T> pv1,
                                         final T a2, final FieldPVCoordinates<T> pv2,
                                         final T a3, final FieldPVCoordinates<T> pv3,
                                         final T a4, final FieldPVCoordinates<T> pv4) {
        super(new FieldVector3D<>(a1, pv1.getPosition(),     a2, pv2.getPosition(),
                                  a3, pv3.getPosition(),     a4, pv4.getPosition()),
              new FieldVector3D<>(a1, pv1.getVelocity(),     a2, pv2.getVelocity(),
                                  a3, pv3.getVelocity(),     a4, pv4.getVelocity()),
              new FieldVector3D<>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration(),
                                  a3, pv3.getAcceleration(), a4, pv4.getAcceleration()));
        this.date = date;
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from four other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4</p>
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
    public TimeStampedFieldPVCoordinates(final AbsoluteDate date,
                                         final T a1, final PVCoordinates pv1,
                                         final T a2, final PVCoordinates pv2,
                                         final T a3, final PVCoordinates pv3,
                                         final T a4, final PVCoordinates pv4) {
        this(new FieldAbsoluteDate<>(a1.getField(), date),
             a1, pv1, a2, pv2, a3, pv3, a4, pv4);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from four other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4</p>
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
    public TimeStampedFieldPVCoordinates(final FieldAbsoluteDate<T> date,
                                         final T a1, final PVCoordinates pv1,
                                         final T a2, final PVCoordinates pv2,
                                         final T a3, final PVCoordinates pv3,
                                         final T a4, final PVCoordinates pv4) {
        super(new FieldVector3D<>(a1, pv1.getPosition(),     a2, pv2.getPosition(),
                                  a3, pv3.getPosition(),     a4, pv4.getPosition()),
              new FieldVector3D<>(a1, pv1.getVelocity(),     a2, pv2.getVelocity(),
                                  a3, pv3.getVelocity(),     a4, pv4.getVelocity()),
              new FieldVector3D<>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration(),
                                  a3, pv3.getAcceleration(), a4, pv4.getAcceleration()));
        this.date = date;
    }

    /** Builds a TimeStampedFieldPVCoordinates triplet from  a {@link FieldVector3D}&lt;{@link FieldDerivativeStructure}&gt;.
     * <p>
     * The vector components must have time as their only derivation parameter and
     * have consistent derivation orders.
     * </p>
     * @param date date of the built coordinates
     * @param <U> type of the derivative
     * @param p vector with time-derivatives embedded within the coordinates
     */
    public <U extends FieldDerivative<T, U>> TimeStampedFieldPVCoordinates(final FieldAbsoluteDate<T> date,
                                                                           final FieldVector3D<U> p) {
        super(p);
        this.date = date;
    }

    /** {@inheritDoc} */
    @Override
    public FieldAbsoluteDate<T> getDate() {
        return date;
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple linear model. It is <em>not</em> intended as a replacement for
     * proper orbit propagation (it is not even Keplerian!) but should be sufficient
     * for either small time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    public TimeStampedFieldPVCoordinates<T> shiftedBy(final double dt) {
        final FieldPVCoordinates<T> spv = super.shiftedBy(dt);
        return new TimeStampedFieldPVCoordinates<>(date.shiftedBy(dt),
                                                   spv.getPosition(), spv.getVelocity(), spv.getAcceleration());
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple linear model. It is <em>not</em> intended as a replacement for
     * proper orbit propagation (it is not even Keplerian!) but should be sufficient
     * for either small time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    public TimeStampedFieldPVCoordinates<T> shiftedBy(final T dt) {
        final FieldPVCoordinates<T> spv = super.shiftedBy(dt);
        return new TimeStampedFieldPVCoordinates<>(date.shiftedBy(dt),
                                                   spv.getPosition(), spv.getVelocity(), spv.getAcceleration());
    }

    /** Convert to a constant position-velocity.
     * @return a constant position-velocity
     * @since 9.0
     */
    public TimeStampedPVCoordinates toTimeStampedPVCoordinates() {
        return new TimeStampedPVCoordinates(date.toAbsoluteDate(),
                                            getPosition().toVector3D(),
                                            getVelocity().toVector3D(),
                                            getAcceleration().toVector3D());
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
        return toTimeStampedPVCoordinates().toString();
    }

    /**
     * Return a string representation of this date, position, velocity, and acceleration.
     *
     * @param utc time scale used to print the date.
     * @return string representation of this.
     */
    public String toString(final TimeScale utc) {
        return toTimeStampedPVCoordinates().toString(utc);
    }

}

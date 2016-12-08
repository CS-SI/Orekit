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
package org.orekit.utils;

import java.util.Collection;

import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.interpolation.FieldHermiteInterpolator;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.errors.OrekitInternalError;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeStamped;

/** {@link TimeStamped time-stamped} version of {@link FieldPVCoordinates}.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @param <T> the type of the field elements
 * @author Luc Maisonobe
 * @since 7.0
 */
public class TimeStampedFieldPVCoordinates<T extends RealFieldElement<T>>
    extends FieldPVCoordinates<T> {

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
        super(new FieldVector3D<T>(a, pv.getPosition()),
              new FieldVector3D<T>(a, pv.getVelocity()),
              new FieldVector3D<T>(a, pv.getAcceleration()));
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
        super(new FieldVector3D<T>(a, pv.getPosition()),
              new FieldVector3D<T>(a, pv.getVelocity()),
              new FieldVector3D<T>(a, pv.getAcceleration()));
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
        super(new FieldVector3D<T>(a, pv.getPosition()),
              new FieldVector3D<T>(a, pv.getVelocity()),
              new FieldVector3D<T>(a, pv.getAcceleration()));
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
        super(new FieldVector3D<T>(a1, pv1.getPosition(),     a2, pv2.getPosition()),
              new FieldVector3D<T>(a1, pv1.getVelocity(),     a2, pv2.getVelocity()),
              new FieldVector3D<T>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration()));
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
        super(new FieldVector3D<T>(a1, pv1.getPosition(),     a2, pv2.getPosition()),
              new FieldVector3D<T>(a1, pv1.getVelocity(),     a2, pv2.getVelocity()),
              new FieldVector3D<T>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration()));
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
        super(new FieldVector3D<T>(a1, pv1.getPosition(),     a2, pv2.getPosition()),
              new FieldVector3D<T>(a1, pv1.getVelocity(),     a2, pv2.getVelocity()),
              new FieldVector3D<T>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration()));
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
        super(new FieldVector3D<T>(a1, pv1.getPosition(),     a2, pv2.getPosition(),     a3, pv3.getPosition()),
              new FieldVector3D<T>(a1, pv1.getVelocity(),     a2, pv2.getVelocity(),     a3, pv3.getVelocity()),
              new FieldVector3D<T>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration(), a3, pv3.getAcceleration()));
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
        super(new FieldVector3D<T>(a1, pv1.getPosition(),     a2, pv2.getPosition(),     a3, pv3.getPosition()),
              new FieldVector3D<T>(a1, pv1.getVelocity(),     a2, pv2.getVelocity(),     a3, pv3.getVelocity()),
              new FieldVector3D<T>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration(), a3, pv3.getAcceleration()));
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
        super(new FieldVector3D<T>(a1, pv1.getPosition(),     a2, pv2.getPosition(),     a3, pv3.getPosition()),
              new FieldVector3D<T>(a1, pv1.getVelocity(),     a2, pv2.getVelocity(),     a3, pv3.getVelocity()),
              new FieldVector3D<T>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration(), a3, pv3.getAcceleration()));
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
        super(new FieldVector3D<T>(a1, pv1.getPosition(),     a2, pv2.getPosition(),
                                   a3, pv3.getPosition(),     a4, pv4.getPosition()),
              new FieldVector3D<T>(a1, pv1.getVelocity(),     a2, pv2.getVelocity(),
                                   a3, pv3.getVelocity(),     a4, pv4.getVelocity()),
              new FieldVector3D<T>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration(),
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
        super(new FieldVector3D<T>(a1, pv1.getPosition(),     a2, pv2.getPosition(),
                                   a3, pv3.getPosition(),     a4, pv4.getPosition()),
              new FieldVector3D<T>(a1, pv1.getVelocity(),     a2, pv2.getVelocity(),
                                   a3, pv3.getVelocity(),     a4, pv4.getVelocity()),
              new FieldVector3D<T>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration(),
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
        super(new FieldVector3D<T>(a1, pv1.getPosition(),     a2, pv2.getPosition(),
                                   a3, pv3.getPosition(),     a4, pv4.getPosition()),
              new FieldVector3D<T>(a1, pv1.getVelocity(),     a2, pv2.getVelocity(),
                                   a3, pv3.getVelocity(),     a4, pv4.getVelocity()),
              new FieldVector3D<T>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration(),
                                   a3, pv3.getAcceleration(), a4, pv4.getAcceleration()));
        this.date = date;
    }

    /** Get the date.
     * @return date
     */
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
        return new TimeStampedFieldPVCoordinates<T>(date.shiftedBy(dt),
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
        return new TimeStampedFieldPVCoordinates<T>(date.shiftedBy(dt.getReal()),
                                                    spv.getPosition(), spv.getVelocity(), spv.getAcceleration());
    }

    /** Interpolate position-velocity.
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * ensuring velocity remains the exact derivative of position.
     * </p>
     * <p>
     * Note that even if first time derivatives (velocities)
     * from sample can be ignored, the interpolated instance always includes
     * interpolated derivatives. This feature can be used explicitly to
     * compute these derivatives when it would be too complex to compute them
     * from an analytical formula: just compute a few sample points from the
     * explicit formula and set the derivatives to zero in these sample points,
     * then use interpolation to add derivatives consistent with the positions.
     * </p>
     * @param date interpolation date
     * @param filter filter for derivatives from the sample to use in interpolation
     * @param sample sample points on which interpolation should be done
     * @param <T> the type of the field elements
     * @return a new position-velocity, interpolated at specified date
     */
    public static <T extends RealFieldElement<T>>
        TimeStampedFieldPVCoordinates<T> interpolate(final AbsoluteDate date,
                                                     final CartesianDerivativesFilter filter,
                                                     final Collection<TimeStampedFieldPVCoordinates<T>> sample) {
        return interpolate(new FieldAbsoluteDate<>(sample.iterator().next().getPosition().getX().getField(), date),
                           filter, sample);
    }

    /** Interpolate position-velocity.
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * ensuring velocity remains the exact derivative of position.
     * </p>
     * <p>
     * Note that even if first time derivatives (velocities)
     * from sample can be ignored, the interpolated instance always includes
     * interpolated derivatives. This feature can be used explicitly to
     * compute these derivatives when it would be too complex to compute them
     * from an analytical formula: just compute a few sample points from the
     * explicit formula and set the derivatives to zero in these sample points,
     * then use interpolation to add derivatives consistent with the positions.
     * </p>
     * @param date interpolation date
     * @param filter filter for derivatives from the sample to use in interpolation
     * @param sample sample points on which interpolation should be done
     * @param <T> the type of the field elements
     * @return a new position-velocity, interpolated at specified date
     */
    public static <T extends RealFieldElement<T>>
        TimeStampedFieldPVCoordinates<T> interpolate(final FieldAbsoluteDate<T> date,
                                                     final CartesianDerivativesFilter filter,
                                                     final Collection<TimeStampedFieldPVCoordinates<T>> sample) {

        // get field properties
        final T prototype = sample.iterator().next().getPosition().getX();
        final T zero      = prototype.getField().getZero();

        // set up an interpolator taking derivatives into account
        final FieldHermiteInterpolator<T> interpolator = new FieldHermiteInterpolator<T>();

        // add sample points
        switch (filter) {
            case USE_P :
                // populate sample with position data, ignoring velocity
                for (final TimeStampedFieldPVCoordinates<T> datedPV : sample) {
                    final FieldVector3D<T> position = datedPV.getPosition();
                    interpolator.addSamplePoint(zero.add(datedPV.getDate().durationFrom(date)),
                                                position.toArray());
                }
                break;
            case USE_PV :
                // populate sample with position and velocity data
                for (final TimeStampedFieldPVCoordinates<T> datedPV : sample) {
                    final FieldVector3D<T> position = datedPV.getPosition();
                    final FieldVector3D<T> velocity = datedPV.getVelocity();
                    interpolator.addSamplePoint(zero.add(datedPV.getDate().durationFrom(date)),
                                                position.toArray(), velocity.toArray());
                }
                break;
            case USE_PVA :
                // populate sample with position, velocity and acceleration data
                for (final TimeStampedFieldPVCoordinates<T> datedPV : sample) {
                    final FieldVector3D<T> position     = datedPV.getPosition();
                    final FieldVector3D<T> velocity     = datedPV.getVelocity();
                    final FieldVector3D<T> acceleration = datedPV.getAcceleration();
                    interpolator.addSamplePoint(zero.add(datedPV.getDate().durationFrom(date)),
                                                position.toArray(), velocity.toArray(), acceleration.toArray());
                }
                break;
            default :
                // this should never happen
                throw new OrekitInternalError(null);
        }

        // interpolate
        final T[][] p = interpolator.derivatives(zero, 2);

        // build a new interpolated instance

        return new TimeStampedFieldPVCoordinates<T>(date,
                                                    new FieldVector3D<T>(p[0]),
                                                    new FieldVector3D<T>(p[1]),
                                                    new FieldVector3D<T>(p[2]));

    }

    /** Return a string representation of this position/velocity pair.
     * @return string representation of this position/velocity pair
     */
    public String toString() {
        final String comma = ", ";
        return new StringBuffer().append('{').append(date).append(", P(").
                                  append(getPosition().getX().getReal()).append(comma).
                                  append(getPosition().getY().getReal()).append(comma).
                                  append(getPosition().getZ().getReal()).append("), V(").
                                  append(getVelocity().getX().getReal()).append(comma).
                                  append(getVelocity().getY().getReal()).append(comma).
                                  append(getVelocity().getZ().getReal()).append("), A(").
                                  append(getAcceleration().getX().getReal()).append(comma).
                                  append(getAcceleration().getY().getReal()).append(comma).
                                  append(getAcceleration().getZ().getReal()).append(")}").toString();
    }

}

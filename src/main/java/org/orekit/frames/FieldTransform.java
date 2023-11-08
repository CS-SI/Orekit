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
package org.orekit.frames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldLine;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeInterpolator;
import org.orekit.time.FieldTimeShiftable;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinatesHermiteInterpolator;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinatesHermiteInterpolator;
import org.orekit.utils.TimeStampedPVCoordinates;


/** Transformation class in three-dimensional space.
 *
 * <p>This class represents the transformation engine between {@link Frame frames}.
 * It is used both to define the relationship between each frame and its
 * parent frame and to gather all individual transforms into one
 * operation when converting between frames far away from each other.</p>
 * <p>The convention used in OREKIT is vectorial transformation. It means
 * that a transformation is defined as a transform to apply to the
 * coordinates of a vector expressed in the old frame to obtain the
 * same vector expressed in the new frame.
 *
 * <p>Instances of this class are guaranteed to be immutable.</p>
 *
 * <h2> Examples </h2>
 *
 * <h3> Example of translation from R<sub>A</sub> to R<sub>B</sub> </h3>
 *
 * <p> We want to transform the {@link FieldPVCoordinates} PV<sub>A</sub> to
 * PV<sub>B</sub> with :
 * <p> PV<sub>A</sub> = ({1, 0, 0}, {2, 0, 0}, {3, 0, 0}); <br>
 *     PV<sub>B</sub> = ({0, 0, 0}, {0, 0, 0}, {0, 0, 0});
 *
 * <p> The transform to apply then is defined as follows :
 *
 * <pre>
 * Vector3D translation  = new Vector3D(-1, 0, 0);
 * Vector3D velocity     = new Vector3D(-2, 0, 0);
 * Vector3D acceleration = new Vector3D(-3, 0, 0);
 *
 * Transform R1toR2 = new Transform(date, translation, velocity, acceleration);
 *
 * PVB = R1toR2.transformPVCoordinate(PVA);
 * </pre>
 *
 * <h3> Example of rotation from R<sub>A</sub> to R<sub>B</sub> </h3>
 * <p> We want to transform the {@link FieldPVCoordinates} PV<sub>A</sub> to
 * PV<sub>B</sub> with
 *
 * <p> PV<sub>A</sub> = ({1, 0, 0}, { 1, 0, 0}); <br>
 *     PV<sub>B</sub> = ({0, 1, 0}, {-2, 1, 0});
 *
 * <p> The transform to apply then is defined as follows :
 *
 * <pre>
 * Rotation rotation = new Rotation(Vector3D.PLUS_K, FastMath.PI / 2);
 * Vector3D rotationRate = new Vector3D(0, 0, -2);
 *
 * Transform R1toR2 = new Transform(rotation, rotationRate);
 *
 * PVB = R1toR2.transformPVCoordinates(PVA);
 * </pre>
 *
 * @author Luc Maisonobe
 * @author Fabien Maussion
 * @param <T> the type of the field elements
 * @since 9.0
 */
public class FieldTransform<T extends CalculusFieldElement<T>>
    implements FieldTimeShiftable<FieldTransform<T>, T>, FieldStaticTransform<T> {

    /** Date of the transform. */
    private final FieldAbsoluteDate<T> date;

    /** Date of the transform. */
    private final AbsoluteDate aDate;

    /** Cartesian coordinates of the target frame with respect to the original frame. */
    private final FieldPVCoordinates<T> cartesian;

    /** Angular coordinates of the target frame with respect to the original frame. */
    private final FieldAngularCoordinates<T> angular;

    /** Build a transform from its primitive operations.
     * @param date date of the transform
     * @param aDate date of the transform
     * @param cartesian Cartesian coordinates of the target frame with respect to the original frame
     * @param angular angular coordinates of the target frame with respect to the original frame
     */
    private FieldTransform(final FieldAbsoluteDate<T> date, final AbsoluteDate aDate,
                           final FieldPVCoordinates<T> cartesian,
                           final FieldAngularCoordinates<T> angular) {
        this.date      = date;
        this.aDate     = aDate;
        this.cartesian = cartesian;
        this.angular   = angular;
    }

    /** Build a transform from a regular transform.
     * @param field field of the elements
     * @param transform regular transform to convert
     */
    public FieldTransform(final Field<T> field, final Transform transform) {
        this(new FieldAbsoluteDate<>(field, transform.getDate()), transform.getDate(),
             new FieldPVCoordinates<>(field, transform.getCartesian()),
             new FieldAngularCoordinates<>(field, transform.getAngular()));
    }

    /** Build a translation transform.
     * @param date date of the transform
     * @param translation translation to apply (i.e. coordinates of
     * the transformed origin, or coordinates of the origin of the
     * old frame in the new frame)
     */
    public FieldTransform(final FieldAbsoluteDate<T> date, final FieldVector3D<T> translation) {
        this(date, date.toAbsoluteDate(),
             new FieldPVCoordinates<>(translation,
                                      FieldVector3D.getZero(date.getField()),
                                      FieldVector3D.getZero(date.getField())),
             FieldAngularCoordinates.getIdentity(date.getField()));
    }

    /** Build a rotation transform.
     * @param date date of the transform
     * @param rotation rotation to apply ( i.e. rotation to apply to the
     * coordinates of a vector expressed in the old frame to obtain the
     * same vector expressed in the new frame )
     */
    public FieldTransform(final FieldAbsoluteDate<T> date, final FieldRotation<T> rotation) {
        this(date, date.toAbsoluteDate(),
             FieldPVCoordinates.getZero(date.getField()),
             new FieldAngularCoordinates<>(rotation,
                                           FieldVector3D.getZero(date.getField()),
                                           FieldVector3D.getZero(date.getField())));
    }

    /** Build a translation transform, with its first time derivative.
     * @param date date of the transform
     * @param translation translation to apply (i.e. coordinates of
     * the transformed origin, or coordinates of the origin of the
     * old frame in the new frame)
     * @param velocity the velocity of the translation (i.e. origin
     * of the old frame velocity in the new frame)
     */
    public FieldTransform(final FieldAbsoluteDate<T> date,
                          final FieldVector3D<T> translation,
                          final FieldVector3D<T> velocity) {
        this(date,
             new FieldPVCoordinates<>(translation,
                                      velocity,
                                      FieldVector3D.getZero(date.getField())));
    }

    /** Build a translation transform, with its first and second time derivatives.
     * @param date date of the transform
     * @param translation translation to apply (i.e. coordinates of
     * the transformed origin, or coordinates of the origin of the
     * old frame in the new frame)
     * @param velocity the velocity of the translation (i.e. origin
     * of the old frame velocity in the new frame)
     * @param acceleration the acceleration of the translation (i.e. origin
     * of the old frame acceleration in the new frame)
     */
    public FieldTransform(final FieldAbsoluteDate<T> date, final FieldVector3D<T> translation,
                          final FieldVector3D<T> velocity, final FieldVector3D<T> acceleration) {
        this(date,
             new FieldPVCoordinates<>(translation, velocity, acceleration));
    }

    /** Build a translation transform, with its first time derivative.
     * @param date date of the transform
     * @param cartesian Cartesian part of the transformation to apply (i.e. coordinates of
     * the transformed origin, or coordinates of the origin of the
     * old frame in the new frame, with their derivatives)
     */
    public FieldTransform(final FieldAbsoluteDate<T> date, final FieldPVCoordinates<T> cartesian) {
        this(date, date.toAbsoluteDate(),
             cartesian,
             FieldAngularCoordinates.getIdentity(date.getField()));
    }

    /** Build a rotation transform.
     * @param date date of the transform
     * @param rotation rotation to apply ( i.e. rotation to apply to the
     * coordinates of a vector expressed in the old frame to obtain the
     * same vector expressed in the new frame )
     * @param rotationRate the axis of the instant rotation
     * expressed in the new frame. (norm representing angular rate)
     */
    public FieldTransform(final FieldAbsoluteDate<T> date,
                          final FieldRotation<T> rotation,
                          final FieldVector3D<T> rotationRate) {
        this(date,
             new FieldAngularCoordinates<>(rotation,
                                           rotationRate,
                                           FieldVector3D.getZero(date.getField())));
    }

    /** Build a rotation transform.
     * @param date date of the transform
     * @param rotation rotation to apply ( i.e. rotation to apply to the
     * coordinates of a vector expressed in the old frame to obtain the
     * same vector expressed in the new frame )
     * @param rotationRate the axis of the instant rotation
     * @param rotationAcceleration the axis of the instant rotation
     * expressed in the new frame. (norm representing angular rate)
     */
    public FieldTransform(final FieldAbsoluteDate<T> date,
                          final FieldRotation<T> rotation,
                          final FieldVector3D<T> rotationRate,
                          final FieldVector3D<T> rotationAcceleration) {
        this(date, new FieldAngularCoordinates<>(rotation, rotationRate, rotationAcceleration));
    }

    /** Build a rotation transform.
     * @param date date of the transform
     * @param angular angular part of the transformation to apply (i.e. rotation to
     * apply to the coordinates of a vector expressed in the old frame to obtain the
     * same vector expressed in the new frame, with its rotation rate)
     */
    public FieldTransform(final FieldAbsoluteDate<T> date, final FieldAngularCoordinates<T> angular) {
        this(date, date.toAbsoluteDate(),
             FieldPVCoordinates.getZero(date.getField()),
             angular);
    }

    /** Build a transform by combining two existing ones.
     * <p>
     * Note that the dates of the two existing transformed are <em>ignored</em>,
     * and the combined transform date is set to the date supplied in this constructor
     * without any attempt to shift the raw transforms. This is a design choice allowing
     * user full control of the combination.
     * </p>
     * @param date date of the transform
     * @param first first transform applied
     * @param second second transform applied
     */
    public FieldTransform(final FieldAbsoluteDate<T> date,
                          final FieldTransform<T> first,
                          final FieldTransform<T> second) {
        this(date, date.toAbsoluteDate(),
             new FieldPVCoordinates<>(FieldStaticTransform.compositeTranslation(first, second),
                                      compositeVelocity(first, second),
                                      compositeAcceleration(first, second)),
             new FieldAngularCoordinates<>(FieldStaticTransform.compositeRotation(first, second),
                                           compositeRotationRate(first, second),
                                           compositeRotationAcceleration(first, second)));
    }

    /** Get the identity transform.
     * @param field field for the components
     * @param <T> the type of the field elements
     * @return identity transform
     */
    public static <T extends CalculusFieldElement<T>> FieldTransform<T> getIdentity(final Field<T> field) {
        return new FieldIdentityTransform<>(field);
    }

    /** Compute a composite velocity.
     * @param first first applied transform
     * @param second second applied transform
     * @param <T> the type of the field elements
     * @return velocity part of the composite transform
     */
    private static <T extends CalculusFieldElement<T>> FieldVector3D<T> compositeVelocity(final FieldTransform<T> first, final FieldTransform<T> second) {

        final FieldVector3D<T> v1 = first.cartesian.getVelocity();
        final FieldRotation<T> r1 = first.angular.getRotation();
        final FieldVector3D<T> o1 = first.angular.getRotationRate();
        final FieldVector3D<T> p2 = second.cartesian.getPosition();
        final FieldVector3D<T> v2 = second.cartesian.getVelocity();

        final FieldVector3D<T> crossP = FieldVector3D.crossProduct(o1, p2);

        return v1.add(r1.applyInverseTo(v2.add(crossP)));

    }

    /** Compute a composite acceleration.
     * @param first first applied transform
     * @param second second applied transform
     * @param <T> the type of the field elements
     * @return acceleration part of the composite transform
     */
    private static <T extends CalculusFieldElement<T>> FieldVector3D<T> compositeAcceleration(final FieldTransform<T> first, final FieldTransform<T> second) {

        final FieldVector3D<T> a1    = first.cartesian.getAcceleration();
        final FieldRotation<T> r1    = first.angular.getRotation();
        final FieldVector3D<T> o1    = first.angular.getRotationRate();
        final FieldVector3D<T> oDot1 = first.angular.getRotationAcceleration();
        final FieldVector3D<T> p2    = second.cartesian.getPosition();
        final FieldVector3D<T> v2    = second.cartesian.getVelocity();
        final FieldVector3D<T> a2    = second.cartesian.getAcceleration();

        final FieldVector3D<T> crossCrossP = FieldVector3D.crossProduct(o1,    FieldVector3D.crossProduct(o1, p2));
        final FieldVector3D<T> crossV      = FieldVector3D.crossProduct(o1,    v2);
        final FieldVector3D<T> crossDotP   = FieldVector3D.crossProduct(oDot1, p2);

        return a1.add(r1.applyInverseTo(new FieldVector3D<>(1, a2, 2, crossV, 1, crossCrossP, 1, crossDotP)));

    }

    /** Compute a composite rotation rate.
     * @param first first applied transform
     * @param second second applied transform
     * @param <T> the type of the field elements
     * @return rotation rate part of the composite transform
     */
    private static <T extends CalculusFieldElement<T>> FieldVector3D<T> compositeRotationRate(final FieldTransform<T> first, final FieldTransform<T> second) {

        final FieldVector3D<T> o1 = first.angular.getRotationRate();
        final FieldRotation<T> r2 = second.angular.getRotation();
        final FieldVector3D<T> o2 = second.angular.getRotationRate();

        return o2.add(r2.applyTo(o1));

    }

    /** Compute a composite rotation acceleration.
     * @param first first applied transform
     * @param second second applied transform
     * @param <T> the type of the field elements
     * @return rotation acceleration part of the composite transform
     */
    private static <T extends CalculusFieldElement<T>> FieldVector3D<T> compositeRotationAcceleration(final FieldTransform<T> first, final FieldTransform<T> second) {

        final FieldVector3D<T> o1    = first.angular.getRotationRate();
        final FieldVector3D<T> oDot1 = first.angular.getRotationAcceleration();
        final FieldRotation<T> r2    = second.angular.getRotation();
        final FieldVector3D<T> o2    = second.angular.getRotationRate();
        final FieldVector3D<T> oDot2 = second.angular.getRotationAcceleration();

        return new FieldVector3D<>( 1, oDot2,
                                    1, r2.applyTo(oDot1),
                                   -1, FieldVector3D.crossProduct(o2, r2.applyTo(o1)));

    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return aDate;
    }

    /** Get the date.
     * @return date attached to the object
     */
    public FieldAbsoluteDate<T> getFieldDate() {
        return date;
    }

    /** {@inheritDoc} */
    @Override
    public FieldTransform<T> shiftedBy(final double dt) {
        return new FieldTransform<>(date.shiftedBy(dt), aDate.shiftedBy(dt),
                                    cartesian.shiftedBy(dt), angular.shiftedBy(dt));
    }

    /** Get a time-shifted instance.
     * @param dt time shift in seconds
     * @return a new instance, shifted with respect to instance (which is not changed)
     */
    public FieldTransform<T> shiftedBy(final T dt) {
        return new FieldTransform<>(date.shiftedBy(dt), aDate.shiftedBy(dt.getReal()),
                                    cartesian.shiftedBy(dt), angular.shiftedBy(dt));
    }

    /**
     * Shift the transform in time considering all rates, then return only the
     * translation and rotation portion of the transform.
     *
     * @param dt time shift in seconds.
     * @return shifted transform as a static transform. It is static in the
     * sense that it can only be used to transform directions and positions, but
     * not velocities or accelerations.
     * @see #shiftedBy(double)
     */
    public FieldStaticTransform<T> staticShiftedBy(final T dt) {
        return FieldStaticTransform.of(date.shiftedBy(dt),
                                       cartesian.positionShiftedBy(dt),
                                       angular.rotationShiftedBy(dt));
    }

    /**
     * Create a so-called static transform from the instance.
     *
     * @return static part of the transform. It is static in the
     * sense that it can only be used to transform directions and positions, but
     * not velocities or accelerations.
     * @see FieldStaticTransform
     */
    public FieldStaticTransform<T> toStaticTransform() {
        return FieldStaticTransform.of(date, cartesian.getPosition(), angular.getRotation());
    }

    /** Interpolate a transform from a sample set of existing transforms.
     * <p>
     * Calling this method is equivalent to call {@link #interpolate(FieldAbsoluteDate,
     * CartesianDerivativesFilter, AngularDerivativesFilter, Collection)} with {@code cFilter}
     * set to {@link CartesianDerivativesFilter#USE_PVA} and {@code aFilter} set to
     * {@link AngularDerivativesFilter#USE_RRA}
     * set to true.
     * </p>
     * @param interpolationDate interpolation date
     * @param sample sample points on which interpolation should be done
     * @param <T> the type of the field elements
     * @return a new instance, interpolated at specified date
     */
    public static <T extends CalculusFieldElement<T>> FieldTransform<T> interpolate(final FieldAbsoluteDate<T> interpolationDate,
                                                                                final Collection<FieldTransform<T>> sample) {
        return interpolate(interpolationDate,
                           CartesianDerivativesFilter.USE_PVA, AngularDerivativesFilter.USE_RRA,
                           sample);
    }

    /** Interpolate a transform from a sample set of existing transforms.
     * <p>
     * Note that even if first time derivatives (velocities and rotation rates)
     * from sample can be ignored, the interpolated instance always includes
     * interpolated derivatives. This feature can be used explicitly to
     * compute these derivatives when it would be too complex to compute them
     * from an analytical formula: just compute a few sample points from the
     * explicit formula and set the derivatives to zero in these sample points,
     * then use interpolation to add derivatives consistent with the positions
     * and rotations.
     * </p>
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only
     * with small samples (about 10-20 points) in order to avoid <a
     * href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a>
     * and numerical problems (including NaN appearing).
     * </p>
     * @param date interpolation date
     * @param cFilter filter for derivatives from the sample to use in interpolation
     * @param aFilter filter for derivatives from the sample to use in interpolation
     * @param sample sample points on which interpolation should be done
     * @return a new instance, interpolated at specified date
          * @param <T> the type of the field elements
     */
    public static <T extends CalculusFieldElement<T>> FieldTransform<T> interpolate(final FieldAbsoluteDate<T> date,
                                                                                final CartesianDerivativesFilter cFilter,
                                                                                final AngularDerivativesFilter aFilter,
                                                                                final Collection<FieldTransform<T>> sample) {
        return interpolate(date, cFilter, aFilter, sample.stream());
    }

    /** Interpolate a transform from a sample set of existing transforms.
     * <p>
     * Note that even if first time derivatives (velocities and rotation rates)
     * from sample can be ignored, the interpolated instance always includes
     * interpolated derivatives. This feature can be used explicitly to
     * compute these derivatives when it would be too complex to compute them
     * from an analytical formula: just compute a few sample points from the
     * explicit formula and set the derivatives to zero in these sample points,
     * then use interpolation to add derivatives consistent with the positions
     * and rotations.
     * </p>
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only
     * with small samples (about 10-20 points) in order to avoid <a
     * href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a>
     * and numerical problems (including NaN appearing).
     * </p>
     * @param date interpolation date
     * @param cFilter filter for derivatives from the sample to use in interpolation
     * @param aFilter filter for derivatives from the sample to use in interpolation
     * @param sample sample points on which interpolation should be done
     * @return a new instance, interpolated at specified date
          * @param <T> the type of the field elements
     */
    public static <T extends CalculusFieldElement<T>> FieldTransform<T> interpolate(final FieldAbsoluteDate<T> date,
                                                                                final CartesianDerivativesFilter cFilter,
                                                                                final AngularDerivativesFilter aFilter,
                                                                                final Stream<FieldTransform<T>> sample) {

        // Create samples
        final List<TimeStampedFieldPVCoordinates<T>>      datedPV = new ArrayList<>();
        final List<TimeStampedFieldAngularCoordinates<T>> datedAC = new ArrayList<>();
        sample.forEach(t -> {
            datedPV.add(new TimeStampedFieldPVCoordinates<>(t.getDate(), t.getTranslation(), t.getVelocity(), t.getAcceleration()));
            datedAC.add(new TimeStampedFieldAngularCoordinates<>(t.getDate(), t.getRotation(), t.getRotationRate(), t.getRotationAcceleration()));
        });

        // Create interpolators
        final FieldTimeInterpolator<TimeStampedFieldPVCoordinates<T>, T> pvInterpolator =
                new TimeStampedFieldPVCoordinatesHermiteInterpolator<>(datedPV.size(), cFilter);

        final FieldTimeInterpolator<TimeStampedFieldAngularCoordinates<T>, T> angularInterpolator =
                new TimeStampedFieldAngularCoordinatesHermiteInterpolator<>(datedPV.size(), aFilter);

        // Interpolate
        final TimeStampedFieldPVCoordinates<T>      interpolatedPV = pvInterpolator.interpolate(date, datedPV);
        final TimeStampedFieldAngularCoordinates<T> interpolatedAC = angularInterpolator.interpolate(date, datedAC);

        return new FieldTransform<>(date, date.toAbsoluteDate(), interpolatedPV, interpolatedAC);
    }

    /** Get the inverse transform of the instance.
     * @return inverse transform of the instance
     */
    @Override
    public FieldTransform<T> getInverse() {

        final FieldRotation<T> r    = angular.getRotation();
        final FieldVector3D<T> o    = angular.getRotationRate();
        final FieldVector3D<T> oDot = angular.getRotationAcceleration();
        final FieldVector3D<T> rp   = r.applyTo(cartesian.getPosition());
        final FieldVector3D<T> rv   = r.applyTo(cartesian.getVelocity());
        final FieldVector3D<T> ra   = r.applyTo(cartesian.getAcceleration());

        final FieldVector3D<T> pInv        = rp.negate();
        final FieldVector3D<T> crossP      = FieldVector3D.crossProduct(o, rp);
        final FieldVector3D<T> vInv        = crossP.subtract(rv);
        final FieldVector3D<T> crossV      = FieldVector3D.crossProduct(o, rv);
        final FieldVector3D<T> crossDotP   = FieldVector3D.crossProduct(oDot, rp);
        final FieldVector3D<T> crossCrossP = FieldVector3D.crossProduct(o, crossP);
        final FieldVector3D<T> aInv        = new FieldVector3D<>(-1, ra,
                                                                  2, crossV,
                                                                  1, crossDotP,
                                                                 -1, crossCrossP);

        return new FieldTransform<>(date, aDate, new FieldPVCoordinates<>(pInv, vInv, aInv), angular.revert());

    }

    /** Get a frozen transform.
     * <p>
     * This method creates a copy of the instance but frozen in time,
     * i.e. with velocity, acceleration and rotation rate forced to zero.
     * </p>
     * @return a new transform, without any time-dependent parts
     */
    public FieldTransform<T> freeze() {
        return new FieldTransform<>(date, aDate,
                                    new FieldPVCoordinates<>(cartesian.getPosition(),
                                                             FieldVector3D.getZero(date.getField()),
                                                             FieldVector3D.getZero(date.getField())),
                                    new FieldAngularCoordinates<>(angular.getRotation(),
                                                                  FieldVector3D.getZero(date.getField()),
                                                                  FieldVector3D.getZero(date.getField())));
    }

    /** Transform {@link TimeStampedPVCoordinates} including kinematic effects.
     * <p>
     * In order to allow the user more flexibility, this method does <em>not</em> check for
     * consistency between the transform {@link #getDate() date} and the time-stamped
     * position-velocity {@link TimeStampedPVCoordinates#getDate() date}. The returned
     * value will always have the same {@link TimeStampedPVCoordinates#getDate() date} as
     * the input argument, regardless of the instance {@link #getDate() date}.
     * </p>
     * @param pv time-stamped  position-velocity to transform.
     * @return transformed time-stamped position-velocity
     */
    public FieldPVCoordinates<T> transformPVCoordinates(final PVCoordinates pv) {
        return angular.applyTo(new FieldPVCoordinates<>(cartesian.getPosition().add(pv.getPosition()),
                                                        cartesian.getVelocity().add(pv.getVelocity()),
                                                        cartesian.getAcceleration().add(pv.getAcceleration())));
    }

    /** Transform {@link TimeStampedPVCoordinates} including kinematic effects.
     * <p>
     * In order to allow the user more flexibility, this method does <em>not</em> check for
     * consistency between the transform {@link #getDate() date} and the time-stamped
     * position-velocity {@link TimeStampedPVCoordinates#getDate() date}. The returned
     * value will always have the same {@link TimeStampedPVCoordinates#getDate() date} as
     * the input argument, regardless of the instance {@link #getDate() date}.
     * </p>
     * @param pv time-stamped  position-velocity to transform.
     * @return transformed time-stamped position-velocity
     */
    public TimeStampedFieldPVCoordinates<T> transformPVCoordinates(final TimeStampedPVCoordinates pv) {
        return angular.applyTo(new TimeStampedFieldPVCoordinates<>(pv.getDate(),
                                                                   cartesian.getPosition().add(pv.getPosition()),
                                                                   cartesian.getVelocity().add(pv.getVelocity()),
                                                                   cartesian.getAcceleration().add(pv.getAcceleration())));
    }

    /** Transform {@link TimeStampedFieldPVCoordinates} including kinematic effects.
     * <p>
     * BEWARE! This method does explicit computation of velocity and acceleration by combining
     * the transform velocity, acceleration, rotation rate and rotation acceleration with the
     * velocity and acceleration from the argument. This implies that this method should
     * <em>not</em> be used when derivatives are contained in the {@link CalculusFieldElement field
     * elements} (typically when using {@link org.hipparchus.analysis.differentiation.DerivativeStructure
     * DerivativeStructure} elements where time is one of the differentiation parameter), otherwise
     * the time derivatives would be computed twice, once explicitly in this method and once implicitly
     * in the field operations. If time derivatives are contained in the field elements themselves,
     * then rather than this method the {@link #transformPosition(FieldVector3D) transformPosition}
     * method should be used, so the derivatives are computed once, as part of the field. This
     * method is rather expected to be used when the field elements are {@link
     * org.hipparchus.analysis.differentiation.DerivativeStructure DerivativeStructure} instances
     * where the differentiation parameters are not time (they can typically be initial state
     * for computing state transition matrices or force models parameters, or ground stations
     * positions, ...).
     * </p>
     * <p>
     * In order to allow the user more flexibility, this method does <em>not</em> check for
     * consistency between the transform {@link #getDate() date} and the time-stamped
     * position-velocity {@link TimeStampedFieldPVCoordinates#getDate() date}. The returned
     * value will always have the same {@link TimeStampedFieldPVCoordinates#getDate() date} as
     * the input argument, regardless of the instance {@link #getDate() date}.
     * </p>
     * @param pv time-stamped position-velocity to transform.
     * @return transformed time-stamped position-velocity
     */
    public FieldPVCoordinates<T> transformPVCoordinates(final FieldPVCoordinates<T> pv) {
        return angular.applyTo(new FieldPVCoordinates<>(pv.getPosition().add(cartesian.getPosition()),
                                                        pv.getVelocity().add(cartesian.getVelocity()),
                                                        pv.getAcceleration().add(cartesian.getAcceleration())));
    }

    /** Transform {@link TimeStampedFieldPVCoordinates} including kinematic effects.
     * <p>
     * BEWARE! This method does explicit computation of velocity and acceleration by combining
     * the transform velocity, acceleration, rotation rate and rotation acceleration with the
     * velocity and acceleration from the argument. This implies that this method should
     * <em>not</em> be used when derivatives are contained in the {@link CalculusFieldElement field
     * elements} (typically when using {@link org.hipparchus.analysis.differentiation.DerivativeStructure
     * DerivativeStructure} elements where time is one of the differentiation parameter), otherwise
     * the time derivatives would be computed twice, once explicitly in this method and once implicitly
     * in the field operations. If time derivatives are contained in the field elements themselves,
     * then rather than this method the {@link #transformPosition(FieldVector3D) transformPosition}
     * method should be used, so the derivatives are computed once, as part of the field. This
     * method is rather expected to be used when the field elements are {@link
     * org.hipparchus.analysis.differentiation.DerivativeStructure DerivativeStructure} instances
     * where the differentiation parameters are not time (they can typically be initial state
     * for computing state transition matrices or force models parameters, or ground stations
     * positions, ...).
     * </p>
     * <p>
     * In order to allow the user more flexibility, this method does <em>not</em> check for
     * consistency between the transform {@link #getDate() date} and the time-stamped
     * position-velocity {@link TimeStampedFieldPVCoordinates#getDate() date}. The returned
     * value will always have the same {@link TimeStampedFieldPVCoordinates#getDate() date} as
     * the input argument, regardless of the instance {@link #getDate() date}.
     * </p>
     * @param pv time-stamped position-velocity to transform.
     * @return transformed time-stamped position-velocity
     */
    public TimeStampedFieldPVCoordinates<T> transformPVCoordinates(final TimeStampedFieldPVCoordinates<T> pv) {
        return angular.applyTo(new TimeStampedFieldPVCoordinates<>(pv.getDate(),
                                                                   pv.getPosition().add(cartesian.getPosition()),
                                                                   pv.getVelocity().add(cartesian.getVelocity()),
                                                                   pv.getAcceleration().add(cartesian.getAcceleration())));
    }

    /** Compute the Jacobian of the {@link #transformPVCoordinates(FieldPVCoordinates)}
     * method of the transform.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of Cartesian coordinate i
     * of the transformed {@link FieldPVCoordinates} with respect to Cartesian coordinate j
     * of the input {@link FieldPVCoordinates} in method {@link #transformPVCoordinates(FieldPVCoordinates)}.
     * </p>
     * <p>
     * This definition implies that if we define position-velocity coordinates
     * <pre>PV₁ = transform.transformPVCoordinates(PV₀)</pre>
     * then their differentials dPV₁ and dPV₀ will obey the following relation
     * where J is the matrix computed by this method:
     * <pre>dPV₁ = J &times; dPV₀</pre>
     *
     * @param selector selector specifying the size of the upper left corner that must be filled
     * (either 3x3 for positions only, 6x6 for positions and velocities, 9x9 for positions,
     * velocities and accelerations)
     * @param jacobian placeholder matrix whose upper-left corner is to be filled with
     * the Jacobian, the rest of the matrix remaining untouched
     */
    public void getJacobian(final CartesianDerivativesFilter selector, final T[][] jacobian) {

        final T zero = date.getField().getZero();

        // elementary matrix for rotation
        final T[][] mData = angular.getRotation().getMatrix();

        // dP1/dP0
        System.arraycopy(mData[0], 0, jacobian[0], 0, 3);
        System.arraycopy(mData[1], 0, jacobian[1], 0, 3);
        System.arraycopy(mData[2], 0, jacobian[2], 0, 3);

        if (selector.getMaxOrder() >= 1) {

            // dP1/dV0
            Arrays.fill(jacobian[0], 3, 6, zero);
            Arrays.fill(jacobian[1], 3, 6, zero);
            Arrays.fill(jacobian[2], 3, 6, zero);

            // dV1/dP0
            final FieldVector3D<T> o = angular.getRotationRate();
            final T ox = o.getX();
            final T oy = o.getY();
            final T oz = o.getZ();
            for (int i = 0; i < 3; ++i) {
                jacobian[3][i] = oz.multiply(mData[1][i]).subtract(oy.multiply(mData[2][i]));
                jacobian[4][i] = ox.multiply(mData[2][i]).subtract(oz.multiply(mData[0][i]));
                jacobian[5][i] = oy.multiply(mData[0][i]).subtract(ox.multiply(mData[1][i]));
            }

            // dV1/dV0
            System.arraycopy(mData[0], 0, jacobian[3], 3, 3);
            System.arraycopy(mData[1], 0, jacobian[4], 3, 3);
            System.arraycopy(mData[2], 0, jacobian[5], 3, 3);

            if (selector.getMaxOrder() >= 2) {

                // dP1/dA0
                Arrays.fill(jacobian[0], 6, 9, zero);
                Arrays.fill(jacobian[1], 6, 9, zero);
                Arrays.fill(jacobian[2], 6, 9, zero);

                // dV1/dA0
                Arrays.fill(jacobian[3], 6, 9, zero);
                Arrays.fill(jacobian[4], 6, 9, zero);
                Arrays.fill(jacobian[5], 6, 9, zero);

                // dA1/dP0
                final FieldVector3D<T> oDot = angular.getRotationAcceleration();
                final T oDotx  = oDot.getX();
                final T oDoty  = oDot.getY();
                final T oDotz  = oDot.getZ();
                for (int i = 0; i < 3; ++i) {
                    jacobian[6][i] = oDotz.multiply(mData[1][i]).subtract(oDoty.multiply(mData[2][i])).add(oz.multiply(jacobian[4][i]).subtract(oy.multiply(jacobian[5][i])));
                    jacobian[7][i] = oDotx.multiply(mData[2][i]).subtract(oDotz.multiply(mData[0][i])).add(ox.multiply(jacobian[5][i]).subtract(oz.multiply(jacobian[3][i])));
                    jacobian[8][i] = oDoty.multiply(mData[0][i]).subtract(oDotx.multiply(mData[1][i])).add(oy.multiply(jacobian[3][i]).subtract(ox.multiply(jacobian[4][i])));
                }

                // dA1/dV0
                for (int i = 0; i < 3; ++i) {
                    jacobian[6][i + 3] = oz.multiply(mData[1][i]).subtract(oy.multiply(mData[2][i])).multiply(2);
                    jacobian[7][i + 3] = ox.multiply(mData[2][i]).subtract(oz.multiply(mData[0][i])).multiply(2);
                    jacobian[8][i + 3] = oy.multiply(mData[0][i]).subtract(ox.multiply(mData[1][i])).multiply(2);
                }

                // dA1/dA0
                System.arraycopy(mData[0], 0, jacobian[6], 6, 3);
                System.arraycopy(mData[1], 0, jacobian[7], 6, 3);
                System.arraycopy(mData[2], 0, jacobian[8], 6, 3);

            }

        }

    }

    /** Get the underlying elementary Cartesian part.
     * <p>A transform can be uniquely represented as an elementary
     * translation followed by an elementary rotation. This method
     * returns this unique elementary translation with its derivative.</p>
     * @return underlying elementary Cartesian part
     * @see #getTranslation()
     * @see #getVelocity()
     */
    public FieldPVCoordinates<T> getCartesian() {
        return cartesian;
    }

    /** Get the underlying elementary translation.
     * <p>A transform can be uniquely represented as an elementary
     * translation followed by an elementary rotation. This method
     * returns this unique elementary translation.</p>
     * @return underlying elementary translation
     * @see #getCartesian()
     * @see #getVelocity()
     * @see #getAcceleration()
     */
    public FieldVector3D<T> getTranslation() {
        return cartesian.getPosition();
    }

    /** Get the first time derivative of the translation.
     * @return first time derivative of the translation
     * @see #getCartesian()
     * @see #getTranslation()
     * @see #getAcceleration()
     */
    public FieldVector3D<T> getVelocity() {
        return cartesian.getVelocity();
    }

    /** Get the second time derivative of the translation.
     * @return second time derivative of the translation
     * @see #getCartesian()
     * @see #getTranslation()
     * @see #getVelocity()
     */
    public FieldVector3D<T> getAcceleration() {
        return cartesian.getAcceleration();
    }

    /** Get the underlying elementary angular part.
     * <p>A transform can be uniquely represented as an elementary
     * translation followed by an elementary rotation. This method
     * returns this unique elementary rotation with its derivative.</p>
     * @return underlying elementary angular part
     * @see #getRotation()
     * @see #getRotationRate()
     * @see #getRotationAcceleration()
     */
    public FieldAngularCoordinates<T> getAngular() {
        return angular;
    }

    /** Get the underlying elementary rotation.
     * <p>A transform can be uniquely represented as an elementary
     * translation followed by an elementary rotation. This method
     * returns this unique elementary rotation.</p>
     * @return underlying elementary rotation
     * @see #getAngular()
     * @see #getRotationRate()
     * @see #getRotationAcceleration()
     */
    public FieldRotation<T> getRotation() {
        return angular.getRotation();
    }

    /** Get the first time derivative of the rotation.
     * <p>The norm represents the angular rate.</p>
     * @return First time derivative of the rotation
     * @see #getAngular()
     * @see #getRotation()
     * @see #getRotationAcceleration()
     */
    public FieldVector3D<T> getRotationRate() {
        return angular.getRotationRate();
    }

    /** Get the second time derivative of the rotation.
     * @return Second time derivative of the rotation
     * @see #getAngular()
     * @see #getRotation()
     * @see #getRotationRate()
     */
    public FieldVector3D<T> getRotationAcceleration() {
        return angular.getRotationAcceleration();
    }

    /** Specialized class for identity transform. */
    private static class FieldIdentityTransform<T extends CalculusFieldElement<T>> extends FieldTransform<T> {

        /** Simple constructor.
         * @param field field for the components
         */
        FieldIdentityTransform(final Field<T> field) {
            super(FieldAbsoluteDate.getArbitraryEpoch(field),
                  FieldAbsoluteDate.getArbitraryEpoch(field).toAbsoluteDate(),
                  FieldPVCoordinates.getZero(field),
                  FieldAngularCoordinates.getIdentity(field));
        }

        /** {@inheritDoc} */
        @Override
        public FieldTransform<T> shiftedBy(final double dt) {
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public FieldTransform<T> getInverse() {
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public FieldVector3D<T> transformPosition(final FieldVector3D<T> position) {
            return position;
        }

        /** {@inheritDoc} */
        @Override
        public FieldVector3D<T> transformVector(final FieldVector3D<T> vector) {
            return vector;
        }

        /** {@inheritDoc} */
        @Override
        public FieldLine<T> transformLine(final FieldLine<T> line) {
            return line;
        }

        /** {@inheritDoc} */
        @Override
        public FieldPVCoordinates<T> transformPVCoordinates(final FieldPVCoordinates<T> pv) {
            return pv;
        }

        /** {@inheritDoc} */
        @Override
        public FieldTransform<T> freeze() {
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public void getJacobian(final CartesianDerivativesFilter selector, final T[][] jacobian) {
            final int n = 3 * (selector.getMaxOrder() + 1);
            for (int i = 0; i < n; ++i) {
                Arrays.fill(jacobian[i], 0, n, getFieldDate().getField().getZero());
                jacobian[i][i] = getFieldDate().getField().getOne();
            }
        }

    }

}

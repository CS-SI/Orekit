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
package org.orekit.frames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeOffset;
import org.orekit.time.TimeInterpolator;
import org.orekit.time.TimeShiftable;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinatesHermiteInterpolator;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinatesHermiteInterpolator;


/** Transformation class in three dimensional space.
 *
 * <p>This class represents the transformation engine between {@link Frame frames}.
 * It is used both to define the relationship between each frame and its
 * parent frame and to gather all individual transforms into one
 * operation when converting between frames far away from each other.</p>
 * <p> The convention used in OREKIT is vectorial transformation. It means
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
 * <p> We want to transform the {@link PVCoordinates} PV<sub>A</sub> to
 * PV<sub>B</sub> with :
 * <p> PV<sub>A</sub> = ({1, 0, 0}, {2, 0, 0}, {3, 0, 0}); <br>
 *     PV<sub>B</sub> = ({0, 0, 0}, {0, 0, 0}, {0, 0, 0});
 *
 * <p> The transform to apply then is defined as follows :
 *
 * <pre><code>
 * Vector3D translation  = new Vector3D(-1, 0, 0);
 * Vector3D velocity     = new Vector3D(-2, 0, 0);
 * Vector3D acceleration = new Vector3D(-3, 0, 0);
 *
 * Transform R1toR2 = new Transform(date, translation, velocity, acceleration);
 *
 * PVB = R1toR2.transformPVCoordinates(PVA);
 * </code></pre>
 *
 * <h3> Example of rotation from R<sub>A</sub> to R<sub>B</sub> </h3>
 * <p> We want to transform the {@link PVCoordinates} PV<sub>A</sub> to
 * PV<sub>B</sub> with
 *
 * <p> PV<sub>A</sub> = ({1, 0, 0}, { 1, 0, 0}); <br>
 *     PV<sub>B</sub> = ({0, 1, 0}, {-2, 1, 0});
 *
 * <p> The transform to apply then is defined as follows :
 *
 * <pre><code>
 * Rotation rotation = new Rotation(Vector3D.PLUS_K, FastMath.PI / 2);
 * Vector3D rotationRate = new Vector3D(0, 0, -2);
 *
 * Transform R1toR2 = new Transform(rotation, rotationRate);
 *
 * PVB = R1toR2.transformPVCoordinates(PVA);
 * </code></pre>
 *
 * @author Luc Maisonobe
 * @author Fabien Maussion
 */
public class Transform implements TimeShiftable<Transform>, KinematicTransform {

    /** Identity transform. */
    public static final Transform IDENTITY = new IdentityTransform();

    /** Date of the transform. */
    private final AbsoluteDate date;

    /** Cartesian coordinates of the target frame with respect to the original frame. */
    private final PVCoordinates cartesian;

    /** Angular coordinates of the target frame with respect to the original frame. */
    private final AngularCoordinates angular;

    /** Build a transform from its primitive operations.
     * @param date date of the transform
     * @param cartesian Cartesian coordinates of the target frame with respect to the original frame
     * @param angular angular coordinates of the target frame with respect to the original frame
     */
    public Transform(final AbsoluteDate date, final PVCoordinates cartesian, final AngularCoordinates angular) {
        this.date      = date;
        this.cartesian = cartesian;
        this.angular   = angular;
    }

    /** Build a translation transform.
     * @param date date of the transform
     * @param translation translation to apply (i.e. coordinates of
     * the transformed origin, or coordinates of the origin of the
     * old frame in the new frame)
     */
    public Transform(final AbsoluteDate date, final Vector3D translation) {
        this(date,
             new PVCoordinates(translation),
             AngularCoordinates.IDENTITY);
    }

    /** Build a rotation transform.
     * @param date date of the transform
     * @param rotation rotation to apply ( i.e. rotation to apply to the
     * coordinates of a vector expressed in the old frame to obtain the
     * same vector expressed in the new frame )
     */
    public Transform(final AbsoluteDate date, final Rotation rotation) {
        this(date,
             PVCoordinates.ZERO,
             new AngularCoordinates(rotation));
    }

    /** Build a combined translation and rotation transform.
     * @param date date of the transform
     * @param translation translation to apply (i.e. coordinates of
     * the transformed origin, or coordinates of the origin of the
     * old frame in the new frame)
     * @param rotation rotation to apply ( i.e. rotation to apply to the
     * coordinates of a vector expressed in the old frame to obtain the
     * same vector expressed in the new frame )
     * @since 12.1
     */
    public Transform(final AbsoluteDate date, final Vector3D translation, final Rotation rotation) {
        this(date, new PVCoordinates(translation), new AngularCoordinates(rotation));
    }

    /** Build a translation transform, with its first time derivative.
     * @param date date of the transform
     * @param translation translation to apply (i.e. coordinates of
     * the transformed origin, or coordinates of the origin of the
     * old frame in the new frame)
     * @param velocity the velocity of the translation (i.e. origin
     * of the old frame velocity in the new frame)
     */
    public Transform(final AbsoluteDate date, final Vector3D translation,
                     final Vector3D velocity) {
        this(date,
             new PVCoordinates(translation, velocity, Vector3D.ZERO),
             AngularCoordinates.IDENTITY);
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
    public Transform(final AbsoluteDate date, final Vector3D translation,
                     final Vector3D velocity, final Vector3D acceleration) {
        this(date,
             new PVCoordinates(translation, velocity, acceleration),
             AngularCoordinates.IDENTITY);
    }

    /** Build a translation transform, with its first time derivative.
     * @param date date of the transform
     * @param cartesian Cartesian part of the transformation to apply (i.e. coordinates of
     * the transformed origin, or coordinates of the origin of the
     * old frame in the new frame, with their derivatives)
     */
    public Transform(final AbsoluteDate date, final PVCoordinates cartesian) {
        this(date,
             cartesian,
             AngularCoordinates.IDENTITY);
    }

    /** Build a rotation transform.
     * @param date date of the transform
     * @param rotation rotation to apply ( i.e. rotation to apply to the
     * coordinates of a vector expressed in the old frame to obtain the
     * same vector expressed in the new frame )
     * @param rotationRate the axis of the instant rotation
     * expressed in the new frame. (norm representing angular rate)
     */
    public Transform(final AbsoluteDate date, final Rotation rotation, final Vector3D rotationRate) {
        this(date,
             PVCoordinates.ZERO,
             new AngularCoordinates(rotation, rotationRate, Vector3D.ZERO));
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
    public Transform(final AbsoluteDate date, final Rotation rotation, final Vector3D rotationRate,
                     final Vector3D rotationAcceleration) {
        this(date,
             PVCoordinates.ZERO,
             new AngularCoordinates(rotation, rotationRate, rotationAcceleration));
    }

    /** Build a rotation transform.
     * @param date date of the transform
     * @param angular angular part of the transformation to apply (i.e. rotation to
     * apply to the coordinates of a vector expressed in the old frame to obtain the
     * same vector expressed in the new frame, with its rotation rate)
     */
    public Transform(final AbsoluteDate date, final AngularCoordinates angular) {
        this(date, PVCoordinates.ZERO, angular);
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
    public Transform(final AbsoluteDate date, final Transform first, final Transform second) {
        this(date,
             new PVCoordinates(StaticTransform.compositeTranslation(first, second),
                               KinematicTransform.compositeVelocity(first, second),
                               compositeAcceleration(first, second)),
             new AngularCoordinates(StaticTransform.compositeRotation(first, second),
                                    KinematicTransform.compositeRotationRate(first, second),
                                    compositeRotationAcceleration(first, second)));
    }

    /** Compute a composite acceleration.
     * @param first first applied transform
     * @param second second applied transform
     * @return acceleration part of the composite transform
     */
    private static Vector3D compositeAcceleration(final Transform first, final Transform second) {

        final Vector3D a1    = first.cartesian.getAcceleration();
        final Rotation r1    = first.angular.getRotation();
        final Vector3D o1    = first.angular.getRotationRate();
        final Vector3D oDot1 = first.angular.getRotationAcceleration();
        final Vector3D p2    = second.cartesian.getPosition();
        final Vector3D v2    = second.cartesian.getVelocity();
        final Vector3D a2    = second.cartesian.getAcceleration();

        final Vector3D crossCrossP = Vector3D.crossProduct(o1,    Vector3D.crossProduct(o1, p2));
        final Vector3D crossV      = Vector3D.crossProduct(o1,    v2);
        final Vector3D crossDotP   = Vector3D.crossProduct(oDot1, p2);

        return a1.add(r1.applyInverseTo(new Vector3D(1, a2, 2, crossV, 1, crossCrossP, 1, crossDotP)));

    }

    /** Compute a composite rotation acceleration.
     * @param first first applied transform
     * @param second second applied transform
     * @return rotation acceleration part of the composite transform
     */
    private static Vector3D compositeRotationAcceleration(final Transform first, final Transform second) {

        final Vector3D o1    = first.angular.getRotationRate();
        final Vector3D oDot1 = first.angular.getRotationAcceleration();
        final Rotation r2    = second.angular.getRotation();
        final Vector3D o2    = second.angular.getRotationRate();
        final Vector3D oDot2 = second.angular.getRotationAcceleration();

        return new Vector3D( 1, oDot2,
                             1, r2.applyTo(oDot1),
                            -1, Vector3D.crossProduct(o2, r2.applyTo(o1)));

    }

    /** {@inheritDoc} */
    public AbsoluteDate getDate() {
        return date;
    }

    /** {@inheritDoc} */
    public Transform shiftedBy(final double dt) {
        return shiftedBy(new TimeOffset(dt));
    }

    /** {@inheritDoc} */
    @Override
    public Transform shiftedBy(final TimeOffset dt) {
        return new Transform(date.shiftedBy(dt), cartesian.shiftedBy(dt), angular.shiftedBy(dt));
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
    public StaticTransform staticShiftedBy(final double dt) {
        return StaticTransform.of(
                date.shiftedBy(dt),
                cartesian.positionShiftedBy(dt),
                angular.rotationShiftedBy(dt));
    }

    /**
     * Create a so-called static transform from the instance.
     *
     * @return static part of the transform. It is static in the
     * sense that it can only be used to transform directions and positions, but
     * not velocities or accelerations.
     * @see StaticTransform
     */
    public StaticTransform toStaticTransform() {
        return StaticTransform.of(date, cartesian.getPosition(), angular.getRotation());
    }

    /** Interpolate a transform from a sample set of existing transforms.
     * <p>
     * Calling this method is equivalent to call {@link #interpolate(AbsoluteDate,
     * CartesianDerivativesFilter, AngularDerivativesFilter, Collection)} with {@code cFilter}
     * set to {@link CartesianDerivativesFilter#USE_PVA} and {@code aFilter} set to
     * {@link AngularDerivativesFilter#USE_RRA}
     * set to true.
     * </p>
     * @param interpolationDate interpolation date
     * @param sample sample points on which interpolation should be done
     * @return a new instance, interpolated at specified date
     */
    public Transform interpolate(final AbsoluteDate interpolationDate, final Stream<Transform> sample) {
        return interpolate(interpolationDate,
                           CartesianDerivativesFilter.USE_PVA, AngularDerivativesFilter.USE_RRA,
                           sample.collect(Collectors.toList()));
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
     * @since 7.0
     */
    public static Transform interpolate(final AbsoluteDate date,
                                        final CartesianDerivativesFilter cFilter,
                                        final AngularDerivativesFilter aFilter,
                                        final Collection<Transform> sample) {

        // Create samples
        final List<TimeStampedPVCoordinates>      datedPV = new ArrayList<>(sample.size());
        final List<TimeStampedAngularCoordinates> datedAC = new ArrayList<>(sample.size());
        for (final Transform t : sample) {
            datedPV.add(new TimeStampedPVCoordinates(t.getDate(), t.getTranslation(), t.getVelocity(), t.getAcceleration()));
            datedAC.add(new TimeStampedAngularCoordinates(t.getDate(), t.getRotation(), t.getRotationRate(), t.getRotationAcceleration()));
        }

        // Create interpolators
        final TimeInterpolator<TimeStampedPVCoordinates> pvInterpolator =
                new TimeStampedPVCoordinatesHermiteInterpolator(datedPV.size(), cFilter);

        final TimeInterpolator<TimeStampedAngularCoordinates> angularInterpolator =
                new TimeStampedAngularCoordinatesHermiteInterpolator(datedPV.size(), aFilter);

        // Interpolate
        final TimeStampedPVCoordinates      interpolatedPV = pvInterpolator.interpolate(date, datedPV);
        final TimeStampedAngularCoordinates interpolatedAC = angularInterpolator.interpolate(date, datedAC);
        return new Transform(date, interpolatedPV, interpolatedAC);
    }

    /** Get the inverse transform of the instance.
     * @return inverse transform of the instance
     */
    @Override
    public Transform getInverse() {

        final Rotation r    = angular.getRotation();
        final Vector3D o    = angular.getRotationRate();
        final Vector3D oDot = angular.getRotationAcceleration();
        final Vector3D rp   = r.applyTo(cartesian.getPosition());
        final Vector3D rv   = r.applyTo(cartesian.getVelocity());
        final Vector3D ra   = r.applyTo(cartesian.getAcceleration());

        final Vector3D pInv        = rp.negate();
        final Vector3D crossP      = Vector3D.crossProduct(o, rp);
        final Vector3D vInv        = crossP.subtract(rv);
        final Vector3D crossV      = Vector3D.crossProduct(o, rv);
        final Vector3D crossDotP   = Vector3D.crossProduct(oDot, rp);
        final Vector3D crossCrossP = Vector3D.crossProduct(o, crossP);
        final Vector3D aInv        = new Vector3D(-1, ra,
                                                   2, crossV,
                                                   1, crossDotP,
                                                  -1, crossCrossP);

        return new Transform(getDate(), new PVCoordinates(pInv, vInv, aInv), angular.revert());

    }

    /** Get a frozen transform.
     * <p>
     * This method creates a copy of the instance but frozen in time,
     * i.e. with velocity, acceleration and rotation rate forced to zero.
     * </p>
     * @return a new transform, without any time-dependent parts
     */
    public Transform freeze() {
        return new Transform(date,
                             new PVCoordinates(cartesian.getPosition(), Vector3D.ZERO, Vector3D.ZERO),
                             new AngularCoordinates(angular.getRotation(), Vector3D.ZERO, Vector3D.ZERO));
    }

    /** Transform {@link PVCoordinates} including kinematic effects.
     * @param pva the position-velocity-acceleration triplet to transform.
     * @return transformed position-velocity-acceleration
     */
    public PVCoordinates transformPVCoordinates(final PVCoordinates pva) {
        return angular.applyTo(new PVCoordinates(1, pva, 1, cartesian));
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
     * @since 7.0
     */
    public TimeStampedPVCoordinates transformPVCoordinates(final TimeStampedPVCoordinates pv) {
        return angular.applyTo(new TimeStampedPVCoordinates(pv.getDate(), 1, pv, 1, cartesian));
    }

    /** Transform {@link FieldPVCoordinates} including kinematic effects.
     * @param pv position-velocity to transform.
     * @param <T> type of the field elements
     * @return transformed position-velocity
     */
    public <T extends CalculusFieldElement<T>> FieldPVCoordinates<T> transformPVCoordinates(final FieldPVCoordinates<T> pv) {
        return angular.applyTo(new FieldPVCoordinates<>(pv.getPosition().add(cartesian.getPosition()),
                                                        pv.getVelocity().add(cartesian.getVelocity()),
                                                        pv.getAcceleration().add(cartesian.getAcceleration())));
    }

    /** Transform {@link TimeStampedFieldPVCoordinates} including kinematic effects.
     * <p>
     * In order to allow the user more flexibility, this method does <em>not</em> check for
     * consistency between the transform {@link #getDate() date} and the time-stamped
     * position-velocity {@link TimeStampedFieldPVCoordinates#getDate() date}. The returned
     * value will always have the same {@link TimeStampedFieldPVCoordinates#getDate() date} as
     * the input argument, regardless of the instance {@link #getDate() date}.
     * </p>
     * @param pv time-stamped position-velocity to transform.
     * @param <T> type of the field elements
     * @return transformed time-stamped position-velocity
     * @since 7.0
     */
    public <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T> transformPVCoordinates(final TimeStampedFieldPVCoordinates<T> pv) {
        return angular.applyTo(new TimeStampedFieldPVCoordinates<>(pv.getDate(),
                                                                   pv.getPosition().add(cartesian.getPosition()),
                                                                   pv.getVelocity().add(cartesian.getVelocity()),
                                                                   pv.getAcceleration().add(cartesian.getAcceleration())));
    }

    /** Compute the Jacobian of the {@link #transformPVCoordinates(PVCoordinates)}
     * method of the transform.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of Cartesian coordinate i
     * of the transformed {@link PVCoordinates} with respect to Cartesian coordinate j
     * of the input {@link PVCoordinates} in method {@link #transformPVCoordinates(PVCoordinates)}.
     * </p>
     * <p>
     * This definition implies that if we define position-velocity coordinates
     * <pre>
     * PV₁ = transform.transformPVCoordinates(PV₀), then
     * </pre>
     * <p> their differentials dPV₁ and dPV₀ will obey the following relation
     * where J is the matrix computed by this method:
     * <pre>
     * dPV₁ = J &times; dPV₀
     * </pre>
     *
     * @param selector selector specifying the size of the upper left corner that must be filled
     * (either 3x3 for positions only, 6x6 for positions and velocities, 9x9 for positions,
     * velocities and accelerations)
     * @param jacobian placeholder matrix whose upper-left corner is to be filled with
     * the Jacobian, the rest of the matrix remaining untouched
     */
    public void getJacobian(final CartesianDerivativesFilter selector, final double[][] jacobian) {

        if (selector.getMaxOrder() == 0) {
            // elementary matrix for rotation
            final double[][] mData = angular.getRotation().getMatrix();

            // dP1/dP0
            System.arraycopy(mData[0], 0, jacobian[0], 0, 3);
            System.arraycopy(mData[1], 0, jacobian[1], 0, 3);
            System.arraycopy(mData[2], 0, jacobian[2], 0, 3);
        }

        else if (selector.getMaxOrder() == 1) {
            // use KinematicTransform Jacobian
            final double[][] mData = getPVJacobian();
            for (int i = 0; i < mData.length; i++) {
                System.arraycopy(mData[i], 0, jacobian[i], 0, mData[i].length);
            }
        }

        else if (selector.getMaxOrder() >= 2) {
            getJacobian(CartesianDerivativesFilter.USE_PV, jacobian);

            // dP1/dA0
            Arrays.fill(jacobian[0], 6, 9, 0.0);
            Arrays.fill(jacobian[1], 6, 9, 0.0);
            Arrays.fill(jacobian[2], 6, 9, 0.0);

            // dV1/dA0
            Arrays.fill(jacobian[3], 6, 9, 0.0);
            Arrays.fill(jacobian[4], 6, 9, 0.0);
            Arrays.fill(jacobian[5], 6, 9, 0.0);

            // dA1/dP0
            final Vector3D o = angular.getRotationRate();
            final double ox = o.getX();
            final double oy = o.getY();
            final double oz = o.getZ();
            final Vector3D oDot = angular.getRotationAcceleration();
            final double oDotx  = oDot.getX();
            final double oDoty  = oDot.getY();
            final double oDotz  = oDot.getZ();
            for (int i = 0; i < 3; ++i) {
                jacobian[6][i] = -(oDoty * jacobian[2][i] - oDotz * jacobian[1][i]) - (oy * jacobian[5][i] - oz * jacobian[4][i]);
                jacobian[7][i] = -(oDotz * jacobian[0][i] - oDotx * jacobian[2][i]) - (oz * jacobian[3][i] - ox * jacobian[5][i]);
                jacobian[8][i] = -(oDotx * jacobian[1][i] - oDoty * jacobian[0][i]) - (ox * jacobian[4][i] - oy * jacobian[3][i]);
            }

            // dA1/dV0
            for (int i = 0; i < 3; ++i) {
                jacobian[6][i + 3] = -2 * (oy * jacobian[2][i] - oz * jacobian[1][i]);
                jacobian[7][i + 3] = -2 * (oz * jacobian[0][i] - ox * jacobian[2][i]);
                jacobian[8][i + 3] = -2 * (ox * jacobian[1][i] - oy * jacobian[0][i]);
            }

            // dA1/dA0
            System.arraycopy(jacobian[0], 0, jacobian[6], 6, 3);
            System.arraycopy(jacobian[1], 0, jacobian[7], 6, 3);
            System.arraycopy(jacobian[2], 0, jacobian[8], 6, 3);

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
    public PVCoordinates getCartesian() {
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
    public Vector3D getTranslation() {
        return cartesian.getPosition();
    }

    /** Get the first time derivative of the translation.
     * @return first time derivative of the translation
     * @see #getCartesian()
     * @see #getTranslation()
     * @see #getAcceleration()
     */
    public Vector3D getVelocity() {
        return cartesian.getVelocity();
    }

    /** Get the second time derivative of the translation.
     * @return second time derivative of the translation
     * @see #getCartesian()
     * @see #getTranslation()
     * @see #getVelocity()
     */
    public Vector3D getAcceleration() {
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
    public AngularCoordinates getAngular() {
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
    public Rotation getRotation() {
        return angular.getRotation();
    }

    /** Get the first time derivative of the rotation.
     * <p>The norm represents the angular rate.</p>
     * @return First time derivative of the rotation
     * @see #getAngular()
     * @see #getRotation()
     * @see #getRotationAcceleration()
     */
    public Vector3D getRotationRate() {
        return angular.getRotationRate();
    }

    /** Get the second time derivative of the rotation.
     * @return Second time derivative of the rotation
     * @see #getAngular()
     * @see #getRotation()
     * @see #getRotationRate()
     */
    public Vector3D getRotationAcceleration() {
        return angular.getRotationAcceleration();
    }

    /** Specialized class for identity transform. */
    private static class IdentityTransform extends Transform {

        /** Simple constructor. */
        IdentityTransform() {
            super(AbsoluteDate.ARBITRARY_EPOCH, PVCoordinates.ZERO, AngularCoordinates.IDENTITY);
        }

        @Override
        public StaticTransform staticShiftedBy(final double dt) {
            return toStaticTransform();
        }

        /** {@inheritDoc} */
        @Override
        public Transform shiftedBy(final double dt) {
            return this;
        }

        @Override
        public StaticTransform getStaticInverse() {
            return toStaticTransform();
        }

        /** {@inheritDoc} */
        @Override
        public Transform shiftedBy(final TimeOffset dt) {
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public Transform getInverse() {
            return this;
        }

        @Override
        public StaticTransform toStaticTransform() {
            return StaticTransform.getIdentity();
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D transformPosition(final Vector3D position) {
            return position;
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D transformVector(final Vector3D vector) {
            return vector;
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> transformPosition(final FieldVector3D<T> position) {
            return transformVector(position);
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> transformVector(final FieldVector3D<T> vector) {
            return new FieldVector3D<>(vector.getX(), vector.getY(), vector.getZ());
        }

        /** {@inheritDoc} */
        @Override
        public Line transformLine(final Line line) {
            return line;
        }

        /** {@inheritDoc} */
        @Override
        public PVCoordinates transformPVCoordinates(final PVCoordinates pv) {
            return pv;
        }

        @Override
        public PVCoordinates transformOnlyPV(final PVCoordinates pv) {
            return new PVCoordinates(pv.getPosition(), pv.getVelocity());
        }

        @Override
        public TimeStampedPVCoordinates transformOnlyPV(final TimeStampedPVCoordinates pv) {
            return new TimeStampedPVCoordinates(pv.getDate(), pv.getPosition(), pv.getVelocity());
        }

        @Override
        public Transform freeze() {
            return this;
        }

        @Override
        public TimeStampedPVCoordinates transformPVCoordinates(
                final TimeStampedPVCoordinates pv) {
            return pv;
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldPVCoordinates<T>
            transformPVCoordinates(final FieldPVCoordinates<T> pv) {
            return pv;
        }

        @Override
        public <T extends CalculusFieldElement<T>>
            TimeStampedFieldPVCoordinates<T> transformPVCoordinates(
                    final TimeStampedFieldPVCoordinates<T> pv) {
            return pv;
        }

        /** {@inheritDoc} */
        @Override
        public void getJacobian(final CartesianDerivativesFilter selector, final double[][] jacobian) {
            final int n = 3 * (selector.getMaxOrder() + 1);
            for (int i = 0; i < n; ++i) {
                Arrays.fill(jacobian[i], 0, n, 0.0);
                jacobian[i][i] = 1.0;
            }
        }

    }

}

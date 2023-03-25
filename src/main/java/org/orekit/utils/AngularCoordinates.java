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

import java.io.Serializable;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.Derivative;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.TimeShiftable;

/** Simple container for rotation/rotation rate/rotation acceleration triplets.
 * <p>
 * The state can be slightly shifted to close dates. This shift is based on
 * an approximate solution of the fixed acceleration motion. It is <em>not</em>
 * intended as a replacement for proper attitude propagation but should be
 * sufficient for either small time shifts or coarse accuracy.
 * </p>
 * <p>
 * This class is the angular counterpart to {@link PVCoordinates}.
 * </p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 */
public class AngularCoordinates implements TimeShiftable<AngularCoordinates>, Serializable {

    /** Fixed orientation parallel with reference frame
     * (identity rotation, zero rotation rate and acceleration).
     */
    public static final AngularCoordinates IDENTITY =
            new AngularCoordinates(Rotation.IDENTITY, Vector3D.ZERO, Vector3D.ZERO);

    /** Serializable UID. */
    private static final long serialVersionUID = 20230325L;

    /** Rotation and its derivatives. */
    private final FieldRotation<UnivariateDerivative2> rotation;

    /** Simple constructor.
     * <p> Sets the Coordinates to default : Identity, Ω = (0 0 0), dΩ/dt = (0 0 0).</p>
     */
    public AngularCoordinates() {
        this(FieldRotation.getIdentity(UnivariateDerivative2Field.getInstance()));
    }

    /** Builds a AngularCoordinates from  a {@link FieldRotation}&lt;{@link Derivative}&gt;.
     * <p>
     * The rotation components must have time as their only derivation parameter and
     * have consistent derivation orders.
     * </p>
     * @param r rotation with time-derivatives embedded within the coordinates
     * @param <U> type of the derivative
     */
    public <U extends Derivative<U>> AngularCoordinates(final FieldRotation<U> r) {

        final Derivative<U> q0 = r.getQ0();
        final Derivative<U> q1 = r.getQ1();
        final Derivative<U> q2 = r.getQ2();
        final Derivative<U> q3 = r.getQ3();

        if (r.getQ0().getOrder() == 0) {
            rotation = new FieldRotation<>(new UnivariateDerivative2(q0.getReal(), 0.0, 0.0),
                                           new UnivariateDerivative2(q1.getReal(), 0.0, 0.0),
                                           new UnivariateDerivative2(q2.getReal(), 0.0, 0.0),
                                           new UnivariateDerivative2(q3.getReal(), 0.0, 0.0),
                                           false);
        } else if (r.getQ0().getOrder() == 1) {
            rotation = new FieldRotation<>(new UnivariateDerivative2(q0.getReal(), q0.getPartialDerivative(1), 0.0),
                                           new UnivariateDerivative2(q1.getReal(), q1.getPartialDerivative(1), 0.0),
                                           new UnivariateDerivative2(q2.getReal(), q2.getPartialDerivative(1), 0.0),
                                           new UnivariateDerivative2(q3.getReal(), q3.getPartialDerivative(1), 0.0),
                                           false);
        } else {
            rotation = new FieldRotation<>(new UnivariateDerivative2(q0.getReal(), q0.getPartialDerivative(1), q0.getPartialDerivative(2)),
                                           new UnivariateDerivative2(q1.getReal(), q1.getPartialDerivative(1), q1.getPartialDerivative(2)),
                                           new UnivariateDerivative2(q2.getReal(), q2.getPartialDerivative(1), q2.getPartialDerivative(2)),
                                           new UnivariateDerivative2(q3.getReal(), q3.getPartialDerivative(1), q3.getPartialDerivative(2)),
                                           false);
        }

    }

    /** Builds a rotation/rotation rate pair.
     * @param rotation rotation
     * @param rotationRate rotation rate Ω (rad/s)
     */
    public AngularCoordinates(final Rotation rotation, final Vector3D rotationRate) {
        this(rotation, rotationRate, Vector3D.ZERO);
    }

    /** Builds a rotation/rotation rate/rotation acceleration triplet.
     * @param rotation rotation
     * @param rotationRate rotation rate Ω (rad/s)
     * @param rotationAcceleration rotation acceleration dΩ/dt (rad/s²)
     */
    public AngularCoordinates(final Rotation rotation,
                              final Vector3D rotationRate, final Vector3D rotationAcceleration) {
        // quaternion components
        final double q0 = rotation.getQ0();
        final double q1 = rotation.getQ1();
        final double q2 = rotation.getQ2();
        final double q3 = rotation.getQ3();

        // first time-derivatives of the quaternion
        final double oX    = rotationRate.getX();
        final double oY    = rotationRate.getY();
        final double oZ    = rotationRate.getZ();
        final double q0Dot = 0.5 * MathArrays.linearCombination(-q1, oX, -q2, oY, -q3, oZ);
        final double q1Dot = 0.5 * MathArrays.linearCombination( q0, oX, -q3, oY,  q2, oZ);
        final double q2Dot = 0.5 * MathArrays.linearCombination( q3, oX,  q0, oY, -q1, oZ);
        final double q3Dot = 0.5 * MathArrays.linearCombination(-q2, oX,  q1, oY,  q0, oZ);

        // second time-derivatives of the quaternion
        final double oXDotO2  = 0.5  * rotationAcceleration.getX();
        final double oYDotO2  = 0.5  * rotationAcceleration.getY();
        final double oZDotO2  = 0.5  * rotationAcceleration.getZ();
        final double o2o4     = 0.25 * (oX * oX + oY * oY + oZ * oZ);
        final double q0DotDot = -0.5 * MathArrays.linearCombination(-q1, oXDotO2, -q2, oYDotO2, -q3, oZDotO2, -q0, o2o4);
        final double q1DotDot =  0.5 * MathArrays.linearCombination( q0, oXDotO2, -q3, oYDotO2,  q2, oZDotO2, -q1, o2o4);
        final double q2DotDot =  0.5 * MathArrays.linearCombination( q3, oXDotO2,  q0, oYDotO2, -q1, oZDotO2, -q2, o2o4);
        final double q3DotDot =  0.5 * MathArrays.linearCombination(-q2, oXDotO2,  q1, oYDotO2,  q0, oZDotO2, -q3, o2o4);

        final UnivariateDerivative2 q0UD = new UnivariateDerivative2(q0, q0Dot, q0DotDot);
        final UnivariateDerivative2 q1UD = new UnivariateDerivative2(q1, q1Dot, q1DotDot);
        final UnivariateDerivative2 q2UD = new UnivariateDerivative2(q2, q2Dot, q2DotDot);
        final UnivariateDerivative2 q3UD = new UnivariateDerivative2(q3, q3Dot, q3DotDot);

        this.rotation = new FieldRotation<>(q0UD, q1UD, q2UD, q3UD, false);

    }

    /**
     * Builds angular coordinates with the given rotation, zero angular
     * velocity, and zero angular acceleration.
     *
     * @param rotation rotation
     */
    public AngularCoordinates(final Rotation rotation) {
        this(rotation, Vector3D.ZERO);
    }

    /** Build the rotation that transforms a pair of pv coordinates into another one.

     * <p><em>WARNING</em>! This method requires much more stringent assumptions on
     * its parameters than the similar {@link Rotation#Rotation(Vector3D, Vector3D,
     * Vector3D, Vector3D) constructor} from the {@link Rotation Rotation} class.
     * As far as the Rotation constructor is concerned, the {@code v₂} vector from
     * the second pair can be slightly misaligned. The Rotation constructor will
     * compensate for this misalignment and create a rotation that ensure {@code
     * v₁ = r(u₁)} and {@code v₂ ∈ plane (r(u₁), r(u₂))}. <em>THIS IS NOT
     * TRUE ANYMORE IN THIS CLASS</em>! As derivatives are involved and must be
     * preserved, this constructor works <em>only</em> if the two pairs are fully
     * consistent, i.e. if a rotation exists that fulfill all the requirements: {@code
     * v₁ = r(u₁)}, {@code v₂ = r(u₂)}, {@code dv₁/dt = dr(u₁)/dt}, {@code dv₂/dt
     * = dr(u₂)/dt}, {@code d²v₁/dt² = d²r(u₁)/dt²}, {@code d²v₂/dt² = d²r(u₂)/dt²}.</p>
     * @param u1 first vector of the origin pair
     * @param u2 second vector of the origin pair
     * @param v1 desired image of u1 by the rotation
     * @param v2 desired image of u2 by the rotation
     * @param tolerance relative tolerance factor used to check singularities
     */
    public AngularCoordinates(final PVCoordinates u1, final PVCoordinates u2,
                              final PVCoordinates v1, final PVCoordinates v2,
                              final double tolerance) {
        rotation = new FieldRotation<>(u1.toUnivariateDerivative2Vector(),
                                       u2.toUnivariateDerivative2Vector(),
                                       v1.toUnivariateDerivative2Vector(),
                                       v2.toUnivariateDerivative2Vector());
    }

    /** Build one of the rotations that transform one pv coordinates into another one.

     * <p>Except for a possible scale factor, if the instance were
     * applied to the vector u it will produce the vector v. There is an
     * infinite number of such rotations, this constructor choose the
     * one with the smallest associated angle (i.e. the one whose axis
     * is orthogonal to the (u, v) plane). If u and v are collinear, an
     * arbitrary rotation axis is chosen.</p>

     * @param u origin vector
     * @param v desired image of u by the rotation
     */
    public AngularCoordinates(final PVCoordinates u, final PVCoordinates v) {
        rotation = new FieldRotation<>(u.toUnivariateDerivative2Vector(),
                                       v.toUnivariateDerivative2Vector());
    }

    /** Transform the instance to a {@link FieldRotation}&lt;{@link DerivativeStructure}&gt;.
     * <p>
     * The {@link DerivativeStructure} coordinates correspond to time-derivatives up
     * to the user-specified order.
     * </p>
     * @param order derivation order for the vector components
     * @return rotation with time-derivatives embedded within the coordinates
     */
    public FieldRotation<DerivativeStructure> toDerivativeStructureRotation(final int order) {

        final UnivariateDerivative2 q0 = rotation.getQ0();
        final UnivariateDerivative2 q1 = rotation.getQ1();
        final UnivariateDerivative2 q2 = rotation.getQ2();
        final UnivariateDerivative2 q3 = rotation.getQ3();
        final DSFactory factory;
        switch (order) {
            case 0 :
                factory = new DSFactory(1, order);
                return new FieldRotation<>(factory.build(q0.getValue()),
                                           factory.build(q1.getValue()),
                                           factory.build(q2.getValue()),
                                           factory.build(q3.getValue()),
                                           false);
            case 1 :
                factory = new DSFactory(1, order);
                return new FieldRotation<>(factory.build(q0.getValue(), q0.getFirstDerivative()),
                                           factory.build(q1.getValue(), q1.getFirstDerivative()),
                                           factory.build(q2.getValue(), q2.getFirstDerivative()),
                                           factory.build(q3.getValue(), q3.getFirstDerivative()),
                                           false);
            case 2 :
                factory = new DSFactory(1, order);
                return new FieldRotation<>(factory.build(q0.getValue(), q0.getFirstDerivative(), q0.getSecondDerivative()),
                                           factory.build(q1.getValue(), q1.getFirstDerivative(), q1.getSecondDerivative()),
                                           factory.build(q2.getValue(), q2.getFirstDerivative(), q2.getSecondDerivative()),
                                           factory.build(q3.getValue(), q3.getFirstDerivative(), q3.getSecondDerivative()),
                                           false);
            default :
                throw new OrekitException(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, order);
        }

    }

    /** Transform the instance to a {@link FieldRotation}&lt;{@link UnivariateDerivative1}&gt;.
     * <p>
     * The {@link UnivariateDerivative1} coordinates correspond to time-derivatives up
     * to the order 1.
     * </p>
     * @return rotation with time-derivatives embedded within the coordinates
     */
    public FieldRotation<UnivariateDerivative1> toUnivariateDerivative1Rotation() {
        final UnivariateDerivative2 q0 = rotation.getQ0();
        final UnivariateDerivative2 q1 = rotation.getQ1();
        final UnivariateDerivative2 q2 = rotation.getQ2();
        final UnivariateDerivative2 q3 = rotation.getQ3();
        return new FieldRotation<>(new UnivariateDerivative1(q0.getValue(), q0.getFirstDerivative()),
                                   new UnivariateDerivative1(q1.getValue(), q1.getFirstDerivative()),
                                   new UnivariateDerivative1(q2.getValue(), q2.getFirstDerivative()),
                                   new UnivariateDerivative1(q3.getValue(), q3.getFirstDerivative()),
                                   false);
    }

    /** Get the underlying {@link FieldRotation}&lt;{@link UnivariateDerivative2}&gt;.
     * <p>
     * The {@link UnivariateDerivative2} coordinates correspond to time-derivatives up
     * to the order 2.
     * </p>
     * @return rotation with time-derivatives embedded within the coordinates
     */
    public FieldRotation<UnivariateDerivative2> getUnivariateDerivative2Rotation() {
        return rotation;
    }

    /** Estimate rotation rate between two orientations.
     * <p>Estimation is based on a simple fixed rate rotation
     * during the time interval between the two orientations.</p>
     * @param start start orientation
     * @param end end orientation
     * @param dt time elapsed between the dates of the two orientations
     * @return rotation rate allowing to go from start to end orientations
     */
    public static Vector3D estimateRate(final Rotation start, final Rotation end, final double dt) {
        final Rotation evolution = start.compose(end.revert(), RotationConvention.VECTOR_OPERATOR);
        return new Vector3D(evolution.getAngle() / dt, evolution.getAxis(RotationConvention.VECTOR_OPERATOR));
    }

    /** Revert a rotation/rotation rate/ rotation acceleration triplet.
     * Build a triplet which reverse the effect of another triplet.
     * @return a new triplet whose effect is the reverse of the effect
     * of the instance
     */
    public AngularCoordinates revert() {
        return new AngularCoordinates(rotation.revert());
    }


    /** Get a time-shifted rotation. Same as {@link #shiftedBy(double)} except
     * only the shifted rotation is computed.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * an approximate solution of the fixed acceleration motion. It is <em>not</em>
     * intended as a replacement for proper attitude propagation but should be
     * sufficient for either small time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     * @see  #shiftedBy(double)
     */
    public Rotation rotationShiftedBy(final double dt) {
        return new Rotation(rotation.getQ0().taylor(dt),
                            rotation.getQ1().taylor(dt),
                            rotation.getQ2().taylor(dt),
                            rotation.getQ3().taylor(dt),
                            true);
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * an approximate solution using {@link ModifiedRodrigues modified Rodrigues
     * vector}. It is <em>not</em> intended as a replacement for proper attitude
     * propagation but should be sufficient for either small time shifts or coarse
     * accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    public AngularCoordinates shiftedBy(final double dt) {
        return new ModifiedRodrigues(1.0, this).shiftedBy(dt).toAngularCoordinates();
    }

    /** Get the rotation.
     * @return the rotation.
     */
    public Rotation getRotation() {
        return rotation.toRotation();
    }

    /** Get the rotation rate.
     * @return the rotation rate vector Ω (rad/s).
     */
    public Vector3D getRotationRate() {

        final double q0    = rotation.getQ0().getValue();
        final double q1    = rotation.getQ1().getValue();
        final double q2    = rotation.getQ2().getValue();
        final double q3    = rotation.getQ3().getValue();

        final double q0Dot = rotation.getQ0().getFirstDerivative();
        final double q1Dot = rotation.getQ1().getFirstDerivative();
        final double q2Dot = rotation.getQ2().getFirstDerivative();
        final double q3Dot = rotation.getQ3().getFirstDerivative();

        return new Vector3D(2 * MathArrays.linearCombination(-q1, q0Dot,  q0, q1Dot,  q3, q2Dot, -q2, q3Dot),
                            2 * MathArrays.linearCombination(-q2, q0Dot, -q3, q1Dot,  q0, q2Dot,  q1, q3Dot),
                            2 * MathArrays.linearCombination(-q3, q0Dot,  q2, q1Dot, -q1, q2Dot,  q0, q3Dot));

    }

    /** Get the rotation acceleration.
     * @return the rotation acceleration vector dΩ/dt (rad/s²).
     */
    public Vector3D getRotationAcceleration() {

        final double q0       = rotation.getQ0().getValue();
        final double q1       = rotation.getQ1().getValue();
        final double q2       = rotation.getQ2().getValue();
        final double q3       = rotation.getQ3().getValue();

        final double q0DotDot = rotation.getQ0().getSecondDerivative();
        final double q1DotDot = rotation.getQ1().getSecondDerivative();
        final double q2DotDot = rotation.getQ2().getSecondDerivative();
        final double q3DotDot = rotation.getQ3().getSecondDerivative();

        return new Vector3D(2 * MathArrays.linearCombination(-q1, q0DotDot,  q0, q1DotDot,  q3, q2DotDot, -q2, q3DotDot),
                            2 * MathArrays.linearCombination(-q2, q0DotDot, -q3, q1DotDot,  q0, q2DotDot,  q1, q3DotDot),
                            2 * MathArrays.linearCombination(-q3, q0DotDot,  q2, q1DotDot, -q1, q2DotDot,  q0, q3DotDot));

    }

    /** Add an offset from the instance.
     * <p>
     * We consider here that the offset rotation is applied first and the
     * instance is applied afterward. Note that angular coordinates do <em>not</em>
     * commute under this operation, i.e. {@code a.addOffset(b)} and {@code
     * b.addOffset(a)} lead to <em>different</em> results in most cases.
     * </p>
     * <p>
     * The two methods {@link #addOffset(AngularCoordinates) addOffset} and
     * {@link #subtractOffset(AngularCoordinates) subtractOffset} are designed
     * so that round trip applications are possible. This means that both {@code
     * ac1.subtractOffset(ac2).addOffset(ac2)} and {@code
     * ac1.addOffset(ac2).subtractOffset(ac2)} return angular coordinates equal to ac1.
     * </p>
     * @param offset offset to subtract
     * @return new instance, with offset subtracted
     * @see #subtractOffset(AngularCoordinates)
     */
    public AngularCoordinates addOffset(final AngularCoordinates offset) {
        return new AngularCoordinates(rotation.applyTo(offset.rotation));
    }

    /** Subtract an offset from the instance.
     * <p>
     * We consider here that the offset rotation is applied first and the
     * instance is applied afterward. Note that angular coordinates do <em>not</em>
     * commute under this operation, i.e. {@code a.subtractOffset(b)} and {@code
     * b.subtractOffset(a)} lead to <em>different</em> results in most cases.
     * </p>
     * <p>
     * The two methods {@link #addOffset(AngularCoordinates) addOffset} and
     * {@link #subtractOffset(AngularCoordinates) subtractOffset} are designed
     * so that round trip applications are possible. This means that both {@code
     * ac1.subtractOffset(ac2).addOffset(ac2)} and {@code
     * ac1.addOffset(ac2).subtractOffset(ac2)} return angular coordinates equal to ac1.
     * </p>
     * @param offset offset to subtract
     * @return new instance, with offset subtracted
     * @see #addOffset(AngularCoordinates)
     */
    public AngularCoordinates subtractOffset(final AngularCoordinates offset) {
        return addOffset(offset.revert());
    }

    /** Apply the rotation to a pv coordinates.
     * @param pv vector to apply the rotation to
     * @return a new pv coordinates which is the image of u by the rotation
     */
    public PVCoordinates applyTo(final PVCoordinates pv) {
        return new PVCoordinates(rotation.applyTo(pv.toUnivariateDerivative2Vector()));
    }

    /** Apply the rotation to a pv coordinates.
     * @param pv vector to apply the rotation to
     * @return a new pv coordinates which is the image of u by the rotation
     */
    public TimeStampedPVCoordinates applyTo(final TimeStampedPVCoordinates pv) {
        return new TimeStampedPVCoordinates(pv.getDate(), applyTo((PVCoordinates) pv));
    }

    /** Apply the rotation to a pv coordinates.
     * @param pv vector to apply the rotation to
     * @param <T> type of the field elements
     * @return a new pv coordinates which is the image of u by the rotation
     * @since 9.0
     */
    public <T extends CalculusFieldElement<T>> FieldPVCoordinates<T> applyTo(final FieldPVCoordinates<T> pv) {

        final Rotation r                    = getRotation();
        final Vector3D rotationRate         = getRotationRate();
        final Vector3D rotationAcceleration = getRotationAcceleration();

        final FieldVector3D<T> transformedP = FieldRotation.applyTo(r, pv.getPosition());
        final FieldVector3D<T> crossP       = FieldVector3D.crossProduct(rotationRate, transformedP);
        final FieldVector3D<T> transformedV = FieldRotation.applyTo(r, pv.getVelocity()).subtract(crossP);
        final FieldVector3D<T> crossV       = FieldVector3D.crossProduct(rotationRate, transformedV);
        final FieldVector3D<T> crossCrossP  = FieldVector3D.crossProduct(rotationRate, crossP);
        final FieldVector3D<T> crossDotP    = FieldVector3D.crossProduct(rotationAcceleration, transformedP);
        final FieldVector3D<T> transformedA = new FieldVector3D<>( 1, FieldRotation.applyTo(r, pv.getAcceleration()),
                                                                  -2, crossV,
                                                                  -1, crossCrossP,
                                                                  -1, crossDotP);

        return new FieldPVCoordinates<>(transformedP, transformedV, transformedA);

    }

    /** Apply the rotation to a pv coordinates.
     * @param pv vector to apply the rotation to
     * @param <T> type of the field elements
     * @return a new pv coordinates which is the image of u by the rotation
     * @since 9.0
     */
    public <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T> applyTo(final TimeStampedFieldPVCoordinates<T> pv) {
        return new TimeStampedFieldPVCoordinates<>(pv.getDate(), applyTo((FieldPVCoordinates<T>) pv));
    }

}

/* Copyright 2022-2025 Romain Serra
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
package org.orekit.control.indirect.adjoint;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.FieldGradient;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Class defining inertial forces' contributions in the adjoint equations for Cartesian coordinates.
 * If present, then the propagator should also include inertial forces.
 * @author Romain Serra
 * @see CartesianAdjointEquationTerm
 * @see org.orekit.forces.inertia.InertialForces
 * @since 12.2
 */
public class CartesianAdjointInertialTerm extends AbstractCartesianAdjointEquationTerm {

    /** Reference frame for inertial forces. Must be inertial. */
    private final Frame referenceInertialFrame;

    /**
     * Constructor.
     * @param referenceInertialFrame reference inertial frame
     */
    public CartesianAdjointInertialTerm(final Frame referenceInertialFrame) {
        this.referenceInertialFrame = referenceInertialFrame;
        if (!referenceInertialFrame.isPseudoInertial()) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME_NOT_SUITABLE_AS_REFERENCE_FOR_INERTIAL_FORCES,
                    referenceInertialFrame.getName());
        }
    }

    /**
     * Getter for reference frame.
     * @return frame
     */
    public Frame getReferenceInertialFrame() {
        return referenceInertialFrame;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getRatesContribution(final AbsoluteDate date, final double[] stateVariables,
                                         final double[] adjointVariables, final Frame frame) {
        final double[] contribution = new double[adjointVariables.length];
        final Gradient[] gradients = buildGradientCartesianVector(stateVariables);
        final Transform transform = getReferenceInertialFrame().getTransformTo(frame, date);
        final FieldTransform<Gradient> fieldTransform = new FieldTransform<>(gradients[0].getField(), transform);
        final FieldVector3D<Gradient> acceleration = getFieldAcceleration(fieldTransform, gradients);
        final double[] accelerationXgradient = acceleration.getX().getGradient();
        final double[] accelerationYgradient = acceleration.getY().getGradient();
        final double[] accelerationZgradient = acceleration.getZ().getGradient();
        for (int i = 0; i < 6; i++) {
            contribution[i] = -(accelerationXgradient[i] * adjointVariables[3] + accelerationYgradient[i] * adjointVariables[4] + accelerationZgradient[i] * adjointVariables[5]);
        }
        return contribution;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T[] getFieldRatesContribution(final FieldAbsoluteDate<T> date,
                                                                             final T[] stateVariables,
                                                                             final T[] adjointVariables, final Frame frame) {
        final T[] contribution = MathArrays.buildArray(date.getField(), 6);
        final FieldGradient<T>[] gradients = buildFieldGradientCartesianVector(stateVariables);
        final FieldTransform<T> transform = getReferenceInertialFrame().getTransformTo(frame, date);
        final FieldTransform<FieldGradient<T>> fieldTransform = new FieldTransform<>(gradients[0].getField(),
                new Transform(date.toAbsoluteDate(), transform.getAngular().toAngularCoordinates()));
        final FieldVector3D<FieldGradient<T>> acceleration = getFieldAcceleration(fieldTransform, gradients);
        final T[] accelerationXgradient = acceleration.getX().getGradient();
        final T[] accelerationYgradient = acceleration.getY().getGradient();
        final T[] accelerationZgradient = acceleration.getZ().getGradient();
        for (int i = 0; i < 6; i++) {
            contribution[i] = (accelerationXgradient[i].multiply(adjointVariables[3])
                .add(accelerationYgradient[i].multiply(adjointVariables[4])).add(accelerationZgradient[i].multiply(adjointVariables[5]))).negate();
        }
        return contribution;
    }

    /** {@inheritDoc} */
    @Override
    protected Vector3D getAcceleration(final AbsoluteDate date, final double[] stateVariables,
                                       final Frame frame) {
        final Transform transform = getReferenceInertialFrame().getTransformTo(frame, date);
        return getAcceleration(transform, stateVariables);
    }

    /**
     * Evaluates the inertial acceleration vector.
     * @param inertialToPropagationFrame transform from inertial to propagation frame
     * @param stateVariables state variables
     * @return acceleration
     */
    public Vector3D getAcceleration(final Transform inertialToPropagationFrame, final double[] stateVariables) {
        final Vector3D  a1                = inertialToPropagationFrame.getCartesian().getAcceleration();
        final Rotation r1                = inertialToPropagationFrame.getAngular().getRotation();
        final Vector3D  o1                = inertialToPropagationFrame.getAngular().getRotationRate();
        final Vector3D  oDot1             = inertialToPropagationFrame.getAngular().getRotationAcceleration();

        final Vector3D  p2                = new Vector3D(stateVariables[0], stateVariables[1], stateVariables[2]);
        final Vector3D  v2                = new Vector3D(stateVariables[3], stateVariables[4], stateVariables[5]);

        final Vector3D crossCrossP        = Vector3D.crossProduct(o1,    Vector3D.crossProduct(o1, p2));
        final Vector3D crossV             = Vector3D.crossProduct(o1,    v2);
        final Vector3D crossDotP          = Vector3D.crossProduct(oDot1, p2);

        return r1.applyTo(a1).subtract(new Vector3D(2, crossV, 1, crossCrossP, 1, crossDotP));
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> FieldVector3D<T> getFieldAcceleration(final FieldAbsoluteDate<T> date,
                                                                                        final T[] stateVariables,
                                                                                        final Frame frame) {
        final FieldTransform<T> transform = getReferenceInertialFrame().getTransformTo(frame, date);
        return getFieldAcceleration(transform, stateVariables);
    }

    /**
     * Evaluates the inertial acceleration vector in Field.
     * @param inertialToPropagationFrame transform from inertial to propagation frame
     * @param stateVariables state variables
     * @param <T> field type
     * @return acceleration
     */
    private <T extends CalculusFieldElement<T>> FieldVector3D<T> getFieldAcceleration(final FieldTransform<T> inertialToPropagationFrame,
                                                                                      final T[] stateVariables) {
        final FieldVector3D<T>  a1                = inertialToPropagationFrame.getCartesian().getAcceleration();
        final FieldRotation<T> r1                = inertialToPropagationFrame.getAngular().getRotation();
        final FieldVector3D<T>  o1                = inertialToPropagationFrame.getAngular().getRotationRate();
        final FieldVector3D<T>  oDot1             = inertialToPropagationFrame.getAngular().getRotationAcceleration();

        final FieldVector3D<T>  p2                = new FieldVector3D<>(stateVariables[0], stateVariables[1], stateVariables[2]);
        final FieldVector3D<T>  v2                = new FieldVector3D<>(stateVariables[3], stateVariables[4], stateVariables[5]);

        final FieldVector3D<T> crossCrossP        = FieldVector3D.crossProduct(o1,    FieldVector3D.crossProduct(o1, p2));
        final FieldVector3D<T> crossV             = FieldVector3D.crossProduct(o1,    v2);
        final FieldVector3D<T> crossDotP          = FieldVector3D.crossProduct(oDot1, p2);

        return r1.applyTo(a1).subtract(new FieldVector3D<>(2, crossV, 1, crossCrossP, 1, crossDotP));
    }
}

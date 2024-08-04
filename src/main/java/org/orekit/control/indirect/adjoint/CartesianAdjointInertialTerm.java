/* Copyright 2022-2024 Romain Serra
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
import org.hipparchus.analysis.differentiation.FieldGradientField;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
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
public class CartesianAdjointInertialTerm implements CartesianAdjointEquationTerm {

    /** Dimension of gradient. */
    private static final int GRADIENT_DIMENSION = 6;

    /** Reference frame for inertial forces. Must be inertial. */
    private final Frame referenceInertialFrame;

    /** Propagation frame (where state variables are integrated). Should be inertial otherwise there is no need for this contribution. */
    private final Frame propagationFrame;

    /**
     * Constructor.
     * @param referenceInertialFrame reference inertial frame
     * @param propagationFrame propagation frame (non-inertial, otherwise no need for contribution).
     */
    public CartesianAdjointInertialTerm(final Frame referenceInertialFrame, final Frame propagationFrame) {
        this.referenceInertialFrame = referenceInertialFrame;
        this.propagationFrame = propagationFrame;
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

    /**
     * Getter for propagation frame.
     * @return frame
     */
    public Frame getPropagationFrame() {
        return propagationFrame;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getContribution(final AbsoluteDate date, final double[] stateVariables,
                                    final double[] adjointVariables) {
        final double[] contribution = new double[adjointVariables.length];
        final GradientField field = GradientField.getField(GRADIENT_DIMENSION);
        final Gradient[] gradients = MathArrays.buildArray(field, GRADIENT_DIMENSION);
        gradients[0] = Gradient.variable(GRADIENT_DIMENSION, 0, stateVariables[0]);
        gradients[1] = Gradient.variable(GRADIENT_DIMENSION, 1, stateVariables[1]);
        gradients[2] = Gradient.variable(GRADIENT_DIMENSION, 2, stateVariables[2]);
        gradients[3] = Gradient.variable(GRADIENT_DIMENSION, 3, stateVariables[3]);
        gradients[4] = Gradient.variable(GRADIENT_DIMENSION, 4, stateVariables[4]);
        gradients[5] = Gradient.variable(GRADIENT_DIMENSION, 5, stateVariables[5]);
        final Transform transform = getReferenceInertialFrame().getTransformTo(getPropagationFrame(), date);
        final FieldTransform<Gradient> fieldTransform = new FieldTransform<>(field, transform);
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
    public <T extends CalculusFieldElement<T>> T[] getFieldContribution(final FieldAbsoluteDate<T> date,
                                                                        final T[] stateVariables,
                                                                        final T[] adjointVariables) {
        final T[] contribution = MathArrays.buildArray(date.getField(), 6);
        final FieldGradientField<T> field = FieldGradientField.getField(date.getField(), GRADIENT_DIMENSION);
        final FieldGradient<T>[] gradients = MathArrays.buildArray(field, GRADIENT_DIMENSION);
        gradients[0] = FieldGradient.variable(GRADIENT_DIMENSION, 0, stateVariables[0]);
        gradients[1] = FieldGradient.variable(GRADIENT_DIMENSION, 1, stateVariables[1]);
        gradients[2] = FieldGradient.variable(GRADIENT_DIMENSION, 2, stateVariables[2]);
        gradients[3] = FieldGradient.variable(GRADIENT_DIMENSION, 3, stateVariables[3]);
        gradients[4] = FieldGradient.variable(GRADIENT_DIMENSION, 4, stateVariables[4]);
        gradients[5] = FieldGradient.variable(GRADIENT_DIMENSION, 5, stateVariables[5]);
        final FieldTransform<T> transform = getReferenceInertialFrame().getTransformTo(getPropagationFrame(), date);
        final FieldTransform<FieldGradient<T>> fieldTransform = new FieldTransform<>(field,
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

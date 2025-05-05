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
import org.hipparchus.analysis.differentiation.FieldGradientField;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathArrays;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Abstract class to define terms in the adjoint equations and Hamiltonian for Cartesian coordinates.
 * @author Romain Serra
 * @see CartesianAdjointDerivativesProvider
 * @see FieldCartesianAdjointDerivativesProvider
 * @since 12.2
 */
public abstract class AbstractCartesianAdjointEquationTerm implements CartesianAdjointEquationTerm {

    /** Dimension of gradient. */
    private static final int GRADIENT_DIMENSION = 6;

    /** {@inheritDoc} */
    @Override
    public double[] getRatesContribution(final AbsoluteDate date, final double[] stateVariables,
                                         final double[] adjointVariables, final Frame frame) {
        final GradientField field = GradientField.getField(GRADIENT_DIMENSION);
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(field, date);
        final Gradient[] stateAsGradients = buildGradientCartesianVector(stateVariables);
        final FieldVector3D<Gradient> acceleration = getFieldAcceleration(fieldDate, stateAsGradients, frame);
        final double[] accelerationXgradient = acceleration.getX().getGradient();
        final double[] accelerationYgradient = acceleration.getY().getGradient();
        final double[] accelerationZgradient = acceleration.getZ().getGradient();
        final double[] contribution = new double[adjointVariables.length];
        for (int i = 0; i < 6; i++) {
            contribution[i] = -(accelerationXgradient[i] * adjointVariables[3] + accelerationYgradient[i] * adjointVariables[4] + accelerationZgradient[i] * adjointVariables[5]);
        }
        return contribution;
    }

    /** {@inheritDoc} */
    @Override
    public double getHamiltonianContribution(final AbsoluteDate date, final double[] stateVariables,
                                             final double[] adjointVariables, final Frame frame) {
        final Vector3D acceleration = getAcceleration(date, stateVariables, frame);
        return acceleration.getX() * adjointVariables[3] + acceleration.getY() * adjointVariables[4] + acceleration.getZ() * adjointVariables[5];
    }

    /**
     * Compute the acceleration vector.
     *
     * @param date           date
     * @param stateVariables state variables
     * @param frame          propagation frame
     * @return acceleration vector
     */
    protected abstract Vector3D getAcceleration(AbsoluteDate date, double[] stateVariables,
                                                Frame frame);

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T[] getFieldRatesContribution(final FieldAbsoluteDate<T> date,
                                                                             final T[] stateVariables,
                                                                             final T[] adjointVariables,
                                                                             final Frame frame) {
        final FieldGradientField<T> field = FieldGradientField.getField(date.getField(), GRADIENT_DIMENSION);
        final FieldAbsoluteDate<FieldGradient<T>> fieldDate = new FieldAbsoluteDate<>(field, date.toAbsoluteDate());
        final FieldGradient<T>[] gradients = buildFieldGradientCartesianVector(stateVariables);
        final FieldVector3D<FieldGradient<T>> acceleration = getFieldAcceleration(fieldDate, gradients, frame);
        final T[] contribution = MathArrays.buildArray(date.getField(), adjointVariables.length);
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
    public <T extends CalculusFieldElement<T>> T getFieldHamiltonianContribution(final FieldAbsoluteDate<T> date,
                                                                                 final T[] stateVariables,
                                                                                 final T[] adjointVariables,
                                                                                 final Frame frame) {
        final FieldVector3D<T> acceleration = getFieldAcceleration(date, stateVariables, frame);
        return acceleration.dotProduct(new FieldVector3D<>(adjointVariables[3], adjointVariables[4], adjointVariables[5]));
    }

    /**
     * Compute the acceleration vector.
     *
     * @param <T>            field type
     * @param date           date
     * @param stateVariables state variables
     * @param frame          propagation frame
     * @return acceleration vector
     */
    protected abstract <T extends CalculusFieldElement<T>> FieldVector3D<T> getFieldAcceleration(FieldAbsoluteDate<T> date,
                                                                                                 T[] stateVariables,
                                                                                                 Frame frame);

    /**
     * Build a Cartesian vector whose components are independent variables for automatic differentiation at order 1.
     * @param stateVariables Cartesian variables
     * @return vector of independent variables
     */
    protected static Gradient[] buildGradientCartesianVector(final double[] stateVariables) {
        final GradientField field = GradientField.getField(GRADIENT_DIMENSION);
        final Gradient[] gradients = MathArrays.buildArray(field, GRADIENT_DIMENSION);
        gradients[0] = Gradient.variable(GRADIENT_DIMENSION, 0, stateVariables[0]);
        gradients[1] = Gradient.variable(GRADIENT_DIMENSION, 1, stateVariables[1]);
        gradients[2] = Gradient.variable(GRADIENT_DIMENSION, 2, stateVariables[2]);
        gradients[3] = Gradient.variable(GRADIENT_DIMENSION, 3, stateVariables[3]);
        gradients[4] = Gradient.variable(GRADIENT_DIMENSION, 4, stateVariables[4]);
        gradients[5] = Gradient.variable(GRADIENT_DIMENSION, 5, stateVariables[5]);
        return gradients;
    }

    /**
     * Build a Cartesian vector whose components are independent variables for automatic differentiation at order 1.
     * @param stateVariables Cartesian variables
     * @param <T> field type
     * @return vector of independent variables
     */
    protected static <T extends CalculusFieldElement<T>> FieldGradient<T>[] buildFieldGradientCartesianVector(final T[] stateVariables) {
        final FieldGradientField<T> field = FieldGradientField.getField(stateVariables[0].getField(), GRADIENT_DIMENSION);
        final FieldGradient<T>[] gradients = MathArrays.buildArray(field, GRADIENT_DIMENSION);
        gradients[0] = FieldGradient.variable(GRADIENT_DIMENSION, 0, stateVariables[0]);
        gradients[1] = FieldGradient.variable(GRADIENT_DIMENSION, 1, stateVariables[1]);
        gradients[2] = FieldGradient.variable(GRADIENT_DIMENSION, 2, stateVariables[2]);
        gradients[3] = FieldGradient.variable(GRADIENT_DIMENSION, 3, stateVariables[3]);
        gradients[4] = FieldGradient.variable(GRADIENT_DIMENSION, 4, stateVariables[4]);
        gradients[5] = FieldGradient.variable(GRADIENT_DIMENSION, 5, stateVariables[5]);
        return gradients;
    }
}

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
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.FieldGradient;
import org.hipparchus.analysis.differentiation.FieldGradientField;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathArrays;
import org.orekit.forces.gravity.J2OnlyPerturbation;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Class defining a (constant) J2 contributions in the adjoint equations for Cartesian coordinates.
 * If present, then the propagator should also include a constant J2 term (oblateness) of the central body.
 * @author Romain Serra
 * @see CartesianAdjointEquationTerm
 * @see org.orekit.forces.gravity.J2OnlyPerturbation
 * @since 12.2
 */
public class CartesianAdjointJ2Term extends AbstractCartesianAdjointGravitationalTerm {

    /** J2 coefficient. */
    private final double j2;

    /** Equatorial radius of central body. */
    private final double rEq;

    /** Frame where J2 applies. */
    private final Frame j2Frame;

    /**
     * Constructor.
     * @param mu central body gravitational parameter.
     * @param rEq equatorial radius
     * @param j2 J2 coefficient
     * @param j2Frame J2 frame
     */
    public CartesianAdjointJ2Term(final double mu, final double rEq, final double j2,
                                  final Frame j2Frame) {
        super(mu);
        this.j2 = j2;
        this.rEq = rEq;
        this.j2Frame = j2Frame;
    }

    /**
     * Getter for central body equatorial radius.
     * @return equatorial radius
     */
    public double getrEq() {
        return rEq;
    }

    /**
     * Getter for J2.
     * @return J2 coefficient
     */
    public double getJ2() {
        return j2;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getVelocityAdjointContribution(final AbsoluteDate date, final double[] stateVariables,
                                                   final double[] adjointVariables, final Frame frame) {
        final double[] contribution = new double[3];
        final int numberOfGradientVariables = 3;
        final FieldVector3D<Gradient> position = new FieldVector3D<>(Gradient.variable(numberOfGradientVariables, 0, stateVariables[0]),
            Gradient.variable(numberOfGradientVariables, 1, stateVariables[1]),
            Gradient.variable(numberOfGradientVariables, 2, stateVariables[2]));
        final StaticTransform transform = frame.getStaticTransformTo(j2Frame, date);
        final FieldVector3D<Gradient> positionInJ2Frame = transform.transformPosition(position);
        final Gradient fieldJ2 = Gradient.constant(numberOfGradientVariables, j2);
        final FieldVector3D<Gradient> accelerationInJ2Frame = J2OnlyPerturbation.computeAccelerationInJ2Frame(positionInJ2Frame,
                getMu(), rEq, fieldJ2);
        final FieldVector3D<Gradient> acceleration = transform.getStaticInverse().transformVector(accelerationInJ2Frame);
        final double pvx = adjointVariables[3];
        final double pvy = adjointVariables[4];
        final double pvz = adjointVariables[5];
        final double[] gradientAccelerationX = acceleration.getX().getGradient();
        final double[] gradientAccelerationY = acceleration.getY().getGradient();
        final double[] gradientAccelerationZ = acceleration.getZ().getGradient();
        contribution[0] = -(gradientAccelerationX[0] * pvx + gradientAccelerationY[0] * pvy + gradientAccelerationZ[0] * pvz);
        contribution[1] = -(gradientAccelerationX[1] * pvx + gradientAccelerationY[1] * pvy + gradientAccelerationZ[1] * pvz);
        contribution[2] = -(gradientAccelerationX[2] * pvx + gradientAccelerationY[2] * pvy + gradientAccelerationZ[2] * pvz);
        return contribution;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T[] getVelocityAdjointFieldContribution(final FieldAbsoluteDate<T> date,
                                                                                       final T[] stateVariables,
                                                                                       final T[] adjointVariables,
                                                                                       final Frame frame) {
        final Field<T> field = adjointVariables[0].getField();
        final T[] contribution = MathArrays.buildArray(field, 3);
        final int numberOfGradientVariables = 3;
        final FieldVector3D<FieldGradient<T>> position = new FieldVector3D<>(FieldGradient.variable(numberOfGradientVariables, 0, stateVariables[0]),
            FieldGradient.variable(numberOfGradientVariables, 1, stateVariables[1]),
            FieldGradient.variable(numberOfGradientVariables, 2, stateVariables[2]));
        final T shift = date.durationFrom(date.toAbsoluteDate());
        final FieldGradientField<T> gradientField = FieldGradientField.getField(field, 3);
        final FieldAbsoluteDate<FieldGradient<T>> gradientDate = new FieldAbsoluteDate<>(gradientField, date.toAbsoluteDate())
            .shiftedBy(FieldGradient.constant(numberOfGradientVariables, shift));
        final FieldStaticTransform<FieldGradient<T>> transform = frame.getStaticTransformTo(j2Frame, gradientDate);
        final FieldVector3D<FieldGradient<T>> positionInJ2Frame = transform.transformPosition(position);
        final FieldGradient<T> fieldJ2 = FieldGradient.constant(numberOfGradientVariables, field.getZero().newInstance(j2));
        final FieldVector3D<FieldGradient<T>> accelerationInJ2Frame = J2OnlyPerturbation.computeAccelerationInJ2Frame(positionInJ2Frame,
                getMu(), rEq, fieldJ2);
        final FieldVector3D<FieldGradient<T>> acceleration = transform.getStaticInverse().transformVector(accelerationInJ2Frame);
        final T pvx = adjointVariables[3];
        final T pvy = adjointVariables[4];
        final T pvz = adjointVariables[5];
        final T[] gradientAccelerationX = acceleration.getX().getGradient();
        final T[] gradientAccelerationY = acceleration.getY().getGradient();
        final T[] gradientAccelerationZ = acceleration.getZ().getGradient();
        contribution[0] = gradientAccelerationX[0].multiply(pvx).add(gradientAccelerationY[0].multiply(pvy)).add(gradientAccelerationZ[0].multiply(pvz));
        contribution[1] = gradientAccelerationX[1].multiply(pvx).add(gradientAccelerationY[1].multiply(pvy)).add(gradientAccelerationZ[1].multiply(pvz));
        contribution[2] = gradientAccelerationX[2].multiply(pvx).add(gradientAccelerationY[2].multiply(pvy)).add(gradientAccelerationZ[2].multiply(pvz));
        contribution[0] = contribution[0].negate();
        contribution[1] = contribution[1].negate();
        contribution[2] = contribution[2].negate();
        return contribution;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getAcceleration(final AbsoluteDate date, final double[] stateVariables,
                                    final double[] adjointVariables, final Frame frame) {
        final StaticTransform transform = frame.getStaticTransformTo(j2Frame, date);
        final Vector3D positionInJ2Frame = transform.transformPosition(new Vector3D(stateVariables[0], stateVariables[1], stateVariables[2]));
        final Vector3D accelerationInJ2Frame = J2OnlyPerturbation.computeAccelerationInJ2Frame(positionInJ2Frame,
                getMu(), rEq, getJ2());
        return transform.getStaticInverse().transformVector(accelerationInJ2Frame);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getFieldAcceleration(final FieldAbsoluteDate<T> date,
                                                                                     final T[] stateVariables,
                                                                                     final T[] adjointVariables,
                                                                                     final Frame frame) {
        final FieldStaticTransform<T> transform = frame.getStaticTransformTo(j2Frame, date);
        final FieldVector3D<T> positionInJ2Frame = transform.transformPosition(new FieldVector3D<>(stateVariables[0], stateVariables[1], stateVariables[2]));
        final FieldVector3D<T> accelerationInJ2Frame = J2OnlyPerturbation.computeAccelerationInJ2Frame(positionInJ2Frame,
                getMu(), rEq, date.getField().getZero().newInstance(getJ2()));
        return transform.getStaticInverse().transformVector(accelerationInJ2Frame);
    }
}

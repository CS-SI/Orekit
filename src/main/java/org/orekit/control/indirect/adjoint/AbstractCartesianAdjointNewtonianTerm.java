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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;

/**
 * Abstract class for common computations regarding adjoint dynamics and Newtonian gravity for Cartesian coordinates.
 *
 * @author Romain Serra
 * @see CartesianAdjointEquationTerm
 * @since 12.2
 */
public abstract class AbstractCartesianAdjointNewtonianTerm extends AbstractCartesianAdjointGravitationalTerm {

    /** Minus three. */
    private static final double MINUS_THREE = -3;

    /**
     * Constructor.
     * @param mu body gravitational parameter
     */
    protected AbstractCartesianAdjointNewtonianTerm(final double mu) {
        super(mu);
    }

    /**
     * Computes the generic term of a Newtonian attraction (central or not).
     * @param relativePosition relative position w.r.t. the body
     * @param adjointVariables adjoint variables
     * @return contribution to velocity adjoint derivatives
     */
    protected double[] getNewtonianVelocityAdjointContribution(final double[] relativePosition,
                                                               final double[] adjointVariables) {
        final double[] contribution = new double[3];
        final double x = relativePosition[0];
        final double y = relativePosition[1];
        final double z = relativePosition[2];
        final double x2 = x * x;
        final double y2 = y * y;
        final double z2 = z * z;
        final double r2 = x2 + y2 + z2;
        final double r = FastMath.sqrt(r2);
        final double factor = getMu() / (r2 * r2 * r);
        final double xy = x * y;
        final double xz = x * z;
        final double yz = y * z;
        final double pvx = adjointVariables[3];
        final double pvy = adjointVariables[4];
        final double pvz = adjointVariables[5];
        contribution[0] = ((x2 * MINUS_THREE + r2) * pvx + xy * MINUS_THREE * pvy + xz * MINUS_THREE * pvz) * factor;
        contribution[1] = ((y2 * MINUS_THREE + r2) * pvy + xy * MINUS_THREE * pvx + yz * MINUS_THREE * pvz) * factor;
        contribution[2] = ((z2 * MINUS_THREE + r2) * pvz + yz * MINUS_THREE * pvy + xz * MINUS_THREE * pvx) * factor;
        return contribution;
    }

    /**
     * Computes the generic term of a Newtonian attraction (central or not).
     * @param relativePosition relative position w.r.t. the body
     * @param adjointVariables adjoint variables
     * @param <T> field type
     * @return contribution to velocity adjoint derivatives
     */
    protected <T extends CalculusFieldElement<T>> T[] getFieldNewtonianVelocityAdjointContribution(final T[] relativePosition,
                                                                                                    final T[] adjointVariables) {
        final T[] contribution = MathArrays.buildArray(adjointVariables[0].getField(), 3);
        final T x = relativePosition[0];
        final T y = relativePosition[1];
        final T z = relativePosition[2];
        final T x2 = x.multiply(x);
        final T y2 = y.multiply(y);
        final T z2 = z.multiply(z);
        final T r2 = x2.add(y2).add(z2);
        final T r = r2.sqrt();
        final T factor = (r2.multiply(r2).multiply(r)).reciprocal().multiply(getMu());
        final T xy = x.multiply(y);
        final T xz = x.multiply(z);
        final T yz = y.multiply(z);
        final T pvx = adjointVariables[3];
        final T pvy = adjointVariables[4];
        final T pvz = adjointVariables[5];
        contribution[0] = ((x2.multiply(MINUS_THREE).add(r2)).multiply(pvx).
                add((xy.multiply(MINUS_THREE)).multiply(pvy)).
                add((xz.multiply(MINUS_THREE)).multiply(pvz))).multiply(factor);
        contribution[1] = ((xy.multiply(MINUS_THREE)).multiply(pvx).
                add((y2.multiply(MINUS_THREE).add(r2)).multiply(pvy)).
                add((yz.multiply(MINUS_THREE)).multiply(pvz))).multiply(factor);
        contribution[2] = ((xz.multiply(MINUS_THREE)).multiply(pvx).
                add((yz.multiply(MINUS_THREE)).multiply(pvy)).
                add((z2.multiply(MINUS_THREE).add(r2)).multiply(pvz))).multiply(factor);
        return contribution;
    }

    /**
     * Compute the Newtonian acceleration.
     * @param position position vector as array
     * @return Newtonian acceleration vector
     */
    protected Vector3D getNewtonianAcceleration(final double[] position) {
        final Vector3D positionVector = new Vector3D(position[0], position[1], position[2]);
        final double squaredRadius = positionVector.getNormSq();
        final double factor = -getMu() / (squaredRadius * FastMath.sqrt(squaredRadius));
        return positionVector.scalarMultiply(factor);
    }

    /**
     * Compute the Newtonian acceleration.
     * @param position position vector as array
     * @param <T> field type
     * @return Newtonian acceleration vector
     */
    protected <T extends CalculusFieldElement<T>> FieldVector3D<T> getFieldNewtonianAcceleration(final T[] position) {
        final FieldVector3D<T> positionVector = new FieldVector3D<>(position[0], position[1], position[2]);
        final T squaredRadius = positionVector.getNormSq();
        final T factor = (squaredRadius.multiply(FastMath.sqrt(squaredRadius))).reciprocal().multiply(-getMu());
        return positionVector.scalarMultiply(factor);
    }
}

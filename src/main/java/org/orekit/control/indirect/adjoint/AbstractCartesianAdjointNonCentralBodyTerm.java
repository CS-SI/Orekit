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
import org.hipparchus.util.MathArrays;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;

/**
 * Abstract class defining the contributions of a point-mass, single body gravity in the adjoint equations for Cartesian coordinates.
 * @author Romain Serra
 * @see CartesianAdjointEquationTerm
 * @since 12.2
 */
public abstract class AbstractCartesianAdjointNonCentralBodyTerm extends AbstractCartesianAdjointNewtonianTerm {

    /** Extended position provider for the body. */
    private final ExtendedPositionProvider bodyPositionProvider;

    /**
     * Constructor.
     * @param mu body gravitational parameter.
     * @param bodyPositionProvider body position provider
     */
    protected AbstractCartesianAdjointNonCentralBodyTerm(final double mu,
                                                         final ExtendedPositionProvider bodyPositionProvider) {
        super(mu);
        this.bodyPositionProvider = bodyPositionProvider;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getPositionAdjointContribution(final AbsoluteDate date, final double[] stateVariables,
                                                   final double[] adjointVariables, final Frame frame) {
        return getNewtonianVelocityAdjointContribution(formRelativePosition(date, stateVariables, frame),
            adjointVariables);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T[] getPositionAdjointFieldContribution(final FieldAbsoluteDate<T> date,
                                                                                       final T[] stateVariables,
                                                                                       final T[] adjointVariables,
                                                                                       final Frame frame) {
        return getFieldNewtonianVelocityAdjointContribution(formFieldRelativePosition(date, stateVariables, frame),
            adjointVariables);
    }

    /**
     * Get body's position.
     * @param date date
     * @param frame frame
     * @return position vector
     */
    protected Vector3D getBodyPosition(final AbsoluteDate date, final Frame frame) {
        return bodyPositionProvider.getPosition(date, frame);
    }

    /**
     * Get body's position.
     * @param <T> field type
     * @param date date
     * @param frame frame
     * @return position vector
     */
    protected <T extends CalculusFieldElement<T>> FieldVector3D<T> getFieldBodyPosition(final FieldAbsoluteDate<T> date,
                                                                                        final Frame frame) {
        return bodyPositionProvider.getPosition(date, frame);
    }

    /**
     * Form relative position vector w.r.t. body.
     * @param date date
     * @param stateVariables Cartesian variables
     * @param frame frame where Cartesian coordinates apply
     * @return relative position vector as array
     */
    protected double[] formRelativePosition(final AbsoluteDate date, final double[] stateVariables, final Frame frame) {
        final Vector3D bodyPosition = getBodyPosition(date, frame);
        final double x = stateVariables[0] - bodyPosition.getX();
        final double y = stateVariables[1] - bodyPosition.getY();
        final double z = stateVariables[2] - bodyPosition.getZ();
        return new double[] { x, y, z };
    }

    /**
     * Form relative position vector w.r.t. body.
     * @param date date
     * @param stateVariables Cartesian variables
     * @param frame frame where Cartesian coordinates apply
     * @param <T> field type
     * @return relative position vector as array
     */
    protected <T extends CalculusFieldElement<T>> T[] formFieldRelativePosition(final FieldAbsoluteDate<T> date,
                                                                              final T[] stateVariables,
                                                                              final Frame frame) {
        final FieldVector3D<T> bodyPosition = getFieldBodyPosition(date, frame);
        final T x = stateVariables[0].subtract(bodyPosition.getX());
        final T y = stateVariables[1].subtract(bodyPosition.getY());
        final T z = stateVariables[2].subtract(bodyPosition.getZ());
        final T[] relativePosition = MathArrays.buildArray(date.getField(), 3);
        relativePosition[0] = x;
        relativePosition[1] = y;
        relativePosition[2] = z;
        return relativePosition;
    }
}

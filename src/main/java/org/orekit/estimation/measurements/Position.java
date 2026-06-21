/* Copyright 2002-2026 CS GROUP
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
package org.orekit.estimation.measurements;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling a position only measurement.
 * <p>
 * For position-velocity measurement see {@link PV}.
 * </p>
 * @see PV
 * @author Luc Maisonobe
 * @since 9.3
 */
public class Position extends PseudoMeasurement<Position> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "Position";

    /** Identity matrix, for states derivatives. */
    private static final double[][] IDENTITY = new double[][] {
        {
            1, 0, 0, 0, 0, 0
        }, {
            0, 1, 0, 0, 0, 0
        }, {
            0, 0, 1, 0, 0, 0
        }
    };

    /** Constructor with one double for the standard deviation.
     * <p>The double is the position's standard deviation, common to the 3 position's components.</p>
     * <p>
     * The measurement must be in the orbit propagation frame.
     * </p>
     * @param date date of the measurement
     * @param position position
     * @param sigmaPosition theoretical standard deviation on position components
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @since 9.3
     */
    public Position(final AbsoluteDate date, final Vector3D position,
                    final double sigmaPosition, final double baseWeight,
                    final ObservableSatellite satellite) {
        this(date, position, new double[] { sigmaPosition, sigmaPosition, sigmaPosition }, baseWeight, satellite);
    }

    /** Constructor with one vector for the standard deviation.
     * <p>The 3-sized vector represents the square root of the diagonal elements of the covariance matrix.</p>
     * <p>The measurement must be in the orbit propagation frame.</p>
     * @param date date of the measurement
     * @param position position
     * @param sigmaPosition 3-sized vector of the standard deviations of the position
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @since 9.3
     */
    public Position(final AbsoluteDate date, final Vector3D position,
                    final double[] sigmaPosition, final double baseWeight, final ObservableSatellite satellite) {
        this(date, position, new MeasurementQuality(sigmaPosition, new double[] {baseWeight, baseWeight, baseWeight}),
                satellite);
    }

    /** Constructor with full covariance matrix and all inputs.
     * <p>The fact that the covariance matrix is symmetric and positive definite is not checked.</p>
     * <p>The measurement must be in the orbit propagation frame.</p>
     * @param date date of the measurement
     * @param position position
     * @param covarianceMatrix 3x3 covariance matrix of the position only measurement
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @since 9.3
     */
    public Position(final AbsoluteDate date, final Vector3D position,
                    final double[][] covarianceMatrix, final double baseWeight,
                    final ObservableSatellite satellite) {
        this(date, position, new MeasurementQuality(covarianceMatrix, new double[] {baseWeight, baseWeight, baseWeight}),
                satellite);
    }

    /** Constructor with full covariance matrix and all inputs.
     * <p>The fact that the covariance matrix is symmetric and positive definite is not checked.</p>
     * <p>The measurement must be in the orbit propagation frame.</p>
     * @param date date of the measurement
     * @param position position
     * @param measurementQuality measurement quality data
     * @param satellite satellite related to this measurement
     * @since 14.0
     */
    public Position(final AbsoluteDate date, final Vector3D position, final MeasurementQuality measurementQuality,
                    final ObservableSatellite satellite) {
        super(date, position.toArray(), measurementQuality, satellite);
    }

    /** Get the position.
     * @return position
     */
    public Vector3D getPosition() {
        return new Vector3D(getObservedValue());
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<Position> theoreticalEvaluationWithoutDerivatives(final int iteration, final int evaluation,
                                                                                         final SpacecraftState[] states,
                                                                                         final boolean fillParticipants) {
        final SpacecraftState state = states[0];

        // prepare the evaluation
        final EstimatedMeasurementBase<Position> estimated = new EstimatedMeasurementBase<>(this, iteration, evaluation, states,
                fillParticipants ? new TimeStampedPVCoordinates[] { state.getPVCoordinates() } : new TimeStampedPVCoordinates[0]);

        final Vector3D position = state.getPosition();
        estimated.setEstimatedValue(position.toArray());

        return estimated;

    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<Position> theoreticalEvaluation(final int iteration, final int evaluation,
                                                                   final SpacecraftState[] states) {

        final EstimatedMeasurement<Position> estimated = new EstimatedMeasurement<>(theoreticalEvaluationWithoutDerivatives(iteration, evaluation, states));

        // partial derivatives with respect to state
        estimated.setStateDerivatives(0, IDENTITY);

        return estimated;
    }

}

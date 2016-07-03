/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** Class modeling a position-velocity state.
 * @author Luc Maisonobe
 * @since 8.0
 */
public class PV extends AbstractMeasurement<PV> {

    /** Identity matrix, for states derivatives. */
    private static final double[][] IDENTITY = new double[][] {
        {
            1, 0, 0, 0, 0, 0
        }, {
            0, 1, 0, 0, 0, 0
        }, {
            0, 0, 1, 0, 0, 0
        }, {
            0, 0, 0, 1, 0, 0
        }, {
            0, 0, 0, 0, 1, 0
        }, {
            0, 0, 0, 0, 0, 1
        }
    };

    /** Simple constructor.
     * <p>
     * The measurement must be in the orbit propagation frame.
     * </p>
     * @param date date of the measurement
     * @param position position
     * @param velocity velocity
     * @param sigmaPosition theoretical standard deviation on position components
     * @param sigmaVelocity theoretical standard deviation on velocity components
     * @param baseWeight base weight
     */
    public PV(final AbsoluteDate date, final Vector3D position, final Vector3D velocity,
              final double sigmaPosition, final double sigmaVelocity, final double baseWeight) {
        super(date,
              new double[] {
                  position.getX(), position.getY(), position.getZ(),
                  velocity.getX(), velocity.getY(), velocity.getZ()
              }, new double[] {
                  sigmaPosition, sigmaPosition, sigmaPosition,
                  sigmaVelocity, sigmaVelocity, sigmaVelocity
              }, new double[] {
                  baseWeight, baseWeight, baseWeight,
                  baseWeight, baseWeight, baseWeight
              });
    }

    /** Get the position.
     * @return position
     */
    public Vector3D getPosition() {
        final double[] pv = getObservedValue();
        return new Vector3D(pv[0], pv[1], pv[2]);
    }

    /** Get the velocity.
     * @return velocity
     */
    public Vector3D getVelocity() {
        final double[] pv = getObservedValue();
        return new Vector3D(pv[3], pv[4], pv[5]);
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<PV> theoreticalEvaluation(final int iteration, final int evaluation,
                                                             final SpacecraftState state)
        throws OrekitException {

        // prepare the evaluation
        final EstimatedMeasurement<PV> estimated =
                        new EstimatedMeasurement<PV>(this, iteration, evaluation, state);

        // PV value
        final PVCoordinates pv = state.getPVCoordinates();
        estimated.setEstimatedValue(new double[] {
            pv.getPosition().getX(), pv.getPosition().getY(), pv.getPosition().getZ(),
            pv.getVelocity().getX(), pv.getVelocity().getY(), pv.getVelocity().getZ()
        });

        // partial derivatives with respect to state
        estimated.setStateDerivatives(IDENTITY);

        return estimated;

    }

}

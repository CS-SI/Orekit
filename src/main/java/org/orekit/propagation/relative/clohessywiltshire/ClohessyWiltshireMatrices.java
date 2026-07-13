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

package org.orekit.propagation.relative.clohessywiltshire;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * This class stores the 4 sub-matrices of the 6x6 state transition matrix of the Clohessy-Wiltshire equations.
 *
 * @author Jérôme Tabeaud
 * @author Romain Cuvillon
 * @since 14.0
 */
public class ClohessyWiltshireMatrices {

    /**
     * Time since epoch used in computing the matrices, in seconds.
     */
    private final double timeSinceEpoch;

    /**
     * Matrix that multiplies the position to compute the position.
     */
    private final RealMatrix phiRR;

    /**
     * Matrix that multiplies the velocity to compute the position.
     */
    private final RealMatrix phiRV;

    /**
     * Matrix that multiplies the position to compute the velocity.
     */
    private final RealMatrix phiVR;

    /**
     * Matrix that multiplies the velocity to compute the velocity.
     */
    private final RealMatrix phiVV;

    /**
     * Constructor of the 4 sub-matrices of the 6x6 state transition matrix of the Clohessy-Wiltshire equations.
     * <blockquote>
     * &delta;<b>r</b>(t) = &phi;<sub>rr</sub>(t) &delta;<b>r</b><sub>0</sub> + &phi;<sub>rv</sub>(t)
     * &delta;<b>v</b><sub>0</sub>
     * <br><br>
     * &delta;<b>v</b>(t) = &phi;<sub>vr</sub>(t) &delta;<b>r</b><sub>0</sub> + &phi;<sub>vv</sub>(t)
     * &delta;<b>v</b><sub>0</sub>
     * </blockquote>
     *
     * @param timeSinceEpoch duration in seconds since epoch
     * @param phiRR          position to position transition sub-matrix
     * @param phiRV          velocity to position transition sub-matrix
     * @param phiVR          position to velocity transition sub-matrix
     * @param phiVV          velocity to velocity transition sub-matrix
     */
    public ClohessyWiltshireMatrices(final double timeSinceEpoch, final RealMatrix phiRR, final RealMatrix phiRV,
                                     final RealMatrix phiVR, final RealMatrix phiVV) {
        this.timeSinceEpoch = timeSinceEpoch;
        this.phiRR          = phiRR;
        this.phiRV          = phiRV;
        this.phiVR          = phiVR;
        this.phiVV          = phiVV;
    }

    /**
     * Get the sub-matrix of the transition matrix &phi;<sub>rr</sub>(t).
     *
     * @return &phi;<sub>rr</sub>(t) sub-matrix
     */
    public RealMatrix getPhiRR() {
        return phiRR;
    }

    /**
     * Get the sub-matrix of the transition matrix &phi;<sub>rv</sub>(t).
     *
     * @return &phi;<sub>rv</sub>(t) sub-matrix
     */
    public RealMatrix getPhiRV() {
        return phiRV;
    }

    /**
     * Get the sub-matrix of the transition matrix &phi;<sub>vr</sub>(t).
     *
     * @return &phi;<sub>vr</sub>(t) sub-matrix
     */
    public RealMatrix getPhiVR() {
        return phiVR;
    }

    /**
     * Get the sub-matrix of the transition matrix &phi;<sub>vv</sub>(t).
     *
     * @return &phi;<sub>vv</sub>(t) sub-matrix
     */
    public RealMatrix getPhiVV() {
        return phiVV;
    }

    /**
     * Transforms the input initial PVT expressed in the target's QSW LOF to the PVT at the time encoded in the
     * Clohessy-Wiltshire matrices contained in the object.
     * <p>pos(t) = Φrr * pos(0) + Φrv * vel(0)</p>
     * <p>vel(t) = Φvr * pos(0) + Φvv * vel(0)</p>
     *
     * @param pvt input initial PVT expressed in the target's QSW LOF.
     * @return PVT at the time encoded in the Clohessy-Wiltshire matrices, expressed in the target's QSW LOF.
     */
    public TimeStampedPVCoordinates transform(final TimeStampedPVCoordinates pvt) {
        return new TimeStampedPVCoordinates(pvt.getDate().shiftedBy(timeSinceEpoch),
                                            transform(new PVCoordinates(pvt.getPosition(), pvt.getVelocity())));
    }

    /**
     * Transforms the input initial PV expressed in the target's QSW LOF to the PV at the time encoded in the
     * Clohessy-Wiltshire matrices contained in the object.
     * <p>pos(t) = Φrr * pos(0) + Φrv * vel(0)</p>
     * <p>vel(t) = Φvr * pos(0) + Φvv * vel(0)</p>
     *
     * @param pv input initial PV expressed in the target's QSW LOF.
     * @return PV at the time encoded in the Clohessy-Wiltshire matrices, expressed in the target's QSW LOF.
     */
    public PVCoordinates transform(final PVCoordinates pv) {

        // Convert Vector3D to RealMatrix
        final RealMatrix pos0 = MatrixUtils.createColumnRealMatrix(pv.getPosition().toArray());
        final RealMatrix vel0 = MatrixUtils.createColumnRealMatrix(pv.getVelocity().toArray());

        // Transform the input PV using the current matrices
        final Vector3D pos1 = new Vector3D(phiRR.multiply(pos0).add(phiRV.multiply(vel0)).getColumn(0));
        final Vector3D vel1 = new Vector3D(phiVR.multiply(pos0).add(phiVV.multiply(vel0)).getColumn(0));

        // Return transformed PV
        return new PVCoordinates(pos1, vel1);
    }
}

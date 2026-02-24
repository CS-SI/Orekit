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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;


/**
 * This class stores the 4 sub-matrices of the 6x6 state transition matrix of the Clohessy-Wiltshire equations.
 *
 * @author Romain Cuvillon
 * @since 14.0
 * @param <T> Any scalar field.
 */
public class FieldClohessyWiltshireMatrices<T extends CalculusFieldElement<T>> {

    /**
     * Time since epoch used in computing the matrices, in seconds.
     */
    private final T timeSinceEpoch;

    /**
     * Matrix that multiplies the position to compute the position.
     */
    private final FieldMatrix<T> phi_rr;

    /**
     * Matrix that multiplies the velocity to compute the position.
     */
    private final FieldMatrix<T> phi_rv;

    /**
     * Matrix that multiplies the position to compute the velocity.
     */
    private final FieldMatrix<T> phi_vr;

    /** Matrix that multiplies the velocity to compute the velocity. */
    private final FieldMatrix<T> phi_vv;

    /**
     * Constructor of the 4 sub-matrices of the 6x6 state transition matrix of the Clohessy-Wiltshire equations.
     * <blockquote>
     *   &delta;<b>r</b>(t) = &phi;<sub>rr</sub>(t) &delta;<b>r</b><sub>0</sub> + &phi;<sub>rv</sub>(t) &delta;<b>v</b><sub>0</sub>
     *   <br><br>
     *   &delta;<b>v</b>(t) = &phi;<sub>vr</sub>(t) &delta;<b>r</b><sub>0</sub> + &phi;<sub>vv</sub>(t) &delta;<b>v</b><sub>0</sub>
     * </blockquote>
     * @param timeSinceEpoch duration in seconds since epoch.
     * @param phi_rr position to position transition sub-matrix.
     * @param phi_rv velocity to position transition sub-matrix.
     * @param phi_vr position to velocity transition sub-matrix.
     * @param phi_vv velocity to velocity transition sub-matrix.
     */
    public FieldClohessyWiltshireMatrices (final T timeSinceEpoch, final FieldMatrix<T> phi_rr, final FieldMatrix<T> phi_rv, final FieldMatrix<T> phi_vr, final FieldMatrix<T> phi_vv) {
        this.timeSinceEpoch = timeSinceEpoch;
        this.phi_rr = phi_rr;
        this.phi_rv = phi_rv;
        this.phi_vr = phi_vr;
        this.phi_vv = phi_vv;
    }

    /**
     * Get the sub-matrix of the transition matrix &phi;<sub>rr</sub>(t).
     * @return &phi;<sub>rr</sub>(t) sub-matrix
     */
    public FieldMatrix<T> getPhiRR() {
        return phi_rr;
    }

    /**
     * Get the sub-matrix of the transition matrix &phi;<sub>rv</sub>(t).
     * @return &phi;<sub>rv</sub>(t) sub-matrix
     */
    public FieldMatrix<T> getPhiRV() {
        return phi_rv;
    }

    /**
     * Get the sub-matrix of the transition matrix &phi;<sub>vr</sub>(t).
     * @return &phi;<sub>vr</sub>(t) sub-matrix
     */
    public FieldMatrix<T> getPhiVR() {
        return phi_vr;
    }

    /**
     * Get the sub-matrix of the transition matrix &phi;<sub>vv</sub>(t).
     * @return &phi;<sub>vv</sub>(t) sub-matrix
     */
    public FieldMatrix<T> getPhiVV() {
        return phi_vv;
    }

    /**
     * Transforms the input initial PVT expressed in the target's QSW LOF to the PVT at the time encoded in the Clohessy-Wiltshire matrices contained in the object.
     * <p>pos(t) = Φrr * pos(0) + Φrv * vel(0)</p>
     * <p>vel(t) = Φvr * pos(0) + Φvv * vel(0)</p>
     *
     * @param pvt input initial PVT expressed in the target's QSW LOF.
     * @return PVT at the time encoded in the Clohessy-Wiltshire matrices, expressed in the target's QSW LOF.
     */
    public TimeStampedFieldPVCoordinates<T> transform(final TimeStampedFieldPVCoordinates<T> pvt) {
        return new TimeStampedFieldPVCoordinates<>(pvt.getDate().shiftedBy(timeSinceEpoch), transform(new FieldPVCoordinates<>(pvt.getPosition(), pvt.getVelocity())));
    }

    /**
     * Transforms the input initial PV expressed in the target's QSW LOF to the PV at the time encoded in the Clohessy-Wiltshire matrices contained in the object.
     * <p>pos(t) = Φrr * pos(0) + Φrv * vel(0)</p>
     * <p>vel(t) = Φvr * pos(0) + Φvv * vel(0)</p>
     *
     * @param pv input initial PV expressed in the target's QSW LOF.
     * @return PV at the time encoded in the Clohessy-Wiltshire matrices, expressed in the target's QSW LOF.
     */
    public FieldPVCoordinates<T> transform(final FieldPVCoordinates<T> pv) {
        // Convert Vector3D to RealMatrix
        final FieldMatrix<T> pos0 = MatrixUtils.createColumnFieldMatrix(pv.getPosition().toArray());
        final FieldMatrix<T> vel0 = MatrixUtils.createColumnFieldMatrix(pv.getVelocity().toArray());

        // Transform the input PV using the current matrices
        final FieldVector3D<T> pos1 = new FieldVector3D<>(phi_rr.multiply(pos0).add(phi_rv.multiply(vel0)).getColumn(0));
        final FieldVector3D<T> vel1 = new FieldVector3D<>(phi_vr.multiply(pos0).add(phi_vv.multiply(vel0)).getColumn(0));

        // Return transformed PV
        return new FieldPVCoordinates<>(pos1, vel1);
    }
}

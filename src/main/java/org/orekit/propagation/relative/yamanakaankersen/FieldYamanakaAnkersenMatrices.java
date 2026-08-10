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
package org.orekit.propagation.relative.yamanakaankersen;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.FieldVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/**
 * This class stores the 2 matrices (in plane: xz, out of plane: y, in local LVLH CCSDS frame) of the Yamanaka Ankersen
 * equations. Matrices in LVLH CCSDS Frame.
 *
 * @param <T> Any scalar field.
 * @author Romain Cuvillon
 * @since 14.0
 */
public class FieldYamanakaAnkersenMatrices<T extends CalculusFieldElement<T>> {

    /**
     * Time since epoch used in computing the matrices in seconds.
     */
    private final T timeSinceEpoch;

    /**
     * True anomaly of the target spacecraft.
     */

    private final T targetTheta;

    /**
     * Matrix to compute in-plane (x-z) components of position and velocity from initial in-plane components.
     */

    private final FieldMatrix<T> inPlaneMatrix;

    /**
     * Matrix to compute out of plane (y) components of position and velocity from initial out of plane components.
     */

    private final FieldMatrix<T> outPlaneMatrix;

    /**
     * Constructor for the state transition matrices given by the Yamanaka-Ankersen equations.
     *
     * @param timeSinceEpoch duration in seconds since epoch
     * @param targetTheta    current true anomaly of the target
     * @param inPlaneMatrix  in-plane transition matrix
     * @param outPlaneMatrix out-plane transition matrix
     */
    public FieldYamanakaAnkersenMatrices(final T timeSinceEpoch, final T targetTheta,
                                         final FieldMatrix<T> inPlaneMatrix, final FieldMatrix<T> outPlaneMatrix) {
        this.timeSinceEpoch = timeSinceEpoch;
        this.targetTheta    = targetTheta;
        this.inPlaneMatrix  = inPlaneMatrix;
        this.outPlaneMatrix = outPlaneMatrix;
    }

    /**
     * Get the true anomaly of the target.
     *
     * @return true anomaly of the target
     */
    public T getTargetTrueAnomaly() {
        return targetTheta;
    }

    /**
     * Get the in-plane matrix at current time.
     *
     * @return In-plane matrix at current time
     */
    public FieldMatrix<T> getInPlaneMatrix() {
        return inPlaneMatrix;
    }

    /**
     * Get the out of plane matrix at current time.
     *
     * @return out of plane matrix at current time
     */
    public FieldMatrix<T> getOutPlaneMatrix() {
        return outPlaneMatrix;
    }

    /**
     * Transforms the input initial PVT expressed in the target's LVLH CCSDS LOF to the PVT at the time and true anomaly
     * associated encoded in the Yamanaka-Ankersen matrices contained in the object. First transform LOF PV coordinates
     * to ~ coordinates system as presented in the Yamanaka Ankersen paper. Compute actualized coordinates in the ~
     * system Retransform from transformed frame to LOF to get desired PVCoordinates in LOF.
     *
     * @param pvt                input initial PVT expressed in the target's LVLH CCSDS LOF
     * @param initialTrueAnomaly true anomaly of the target Spacecraft at initial time
     * @param trueAnomaly        true anomaly of the target Spacecraft at final time
     * @param eccentricity       eccentricity of the target Orbit
     * @param sma                semi-major axis of the target Orbit
     * @param mu                 mu of the target orbit
     * @return PVT at the time encoded in the Yamanaka-Ankersen matrices, expressed in the target's LVLH CCSDS LOF
     */
    public TimeStampedFieldPVCoordinates<T> transform(final TimeStampedFieldPVCoordinates<T> pvt,
                                                      final T initialTrueAnomaly, final T trueAnomaly,
                                                      final T eccentricity, final T sma, final T mu) {
        return new TimeStampedFieldPVCoordinates<>(pvt.getDate().shiftedBy(timeSinceEpoch),
                                                   transform(new FieldPVCoordinates<>(pvt.getPosition(),
                                                                                      pvt.getVelocity()),
                                                             initialTrueAnomaly, trueAnomaly, eccentricity, sma, mu));
    }

    /**
     * Transforms the input initial PVT expressed in the target's LVLH CCSDS LOF to the PVT at the time and true anomaly
     * associated encoded in the Yamanaka-Ankersen matrices contained in the object. First transform LOF PV coordinates
     * to ~ coordinates system as presented in the Yamanaka Ankersen paper. Compute actualized coordinates in the ~
     * system Retransform from transformed frame to LOF to get desired PVCoordinates in LOF.
     *
     * @param pv                 input initial PV expressed in the target's LVLH CCSDS LOF
     * @param initialTrueAnomaly true anomaly of the target Spacecraft at initial time
     * @param trueAnomaly        true anomaly of the target Spacecraft at final time
     * @param eccentricity       eccentricity of the target Orbit
     * @param sma                semi-major axis of the target Orbit
     * @param mu                 mu of the target orbit
     * @return PVT at the time encoded in the Yamanaka-Ankersen matrices, expressed in the target's LVLH CCSDS LOF
     */
    public FieldPVCoordinates<T> transform(final FieldPVCoordinates<T> pv, final T initialTrueAnomaly,
                                           final T trueAnomaly, final T eccentricity, final T sma, final T mu) {

        // Compute constant k2
        final T p = sma.multiply(sma.getField().getOne().subtract(eccentricity.pow(2)));
        final T k2 = FastMath.sqrt(mu.divide(p.pow(3)));

        // Pre-compute quantities
        final FieldSinCos<T> scNu0 = initialTrueAnomaly.sinCos();
        final T rho0 = eccentricity.multiply(scNu0.cos()).add(1);
        final FieldSinCos<T> scNu = trueAnomaly.sinCos();
        final T rho = eccentricity.multiply(scNu.cos()).add(1);

        // Convert Vector3D to RealMatrix
        final FieldMatrix<T> pos0 = MatrixUtils.createColumnFieldMatrix(pv.getPosition().toArray());
        final FieldMatrix<T> vel0 = MatrixUtils.createColumnFieldMatrix(pv.getVelocity().toArray());

        // Transform coordinates in the ~ (tilde) coordinate system
        final FieldMatrix<T> pos_tilde0 = pos0.scalarMultiply(rho0);
        final FieldMatrix<T> vel_tilde0 =
                        pos0.scalarMultiply(eccentricity.multiply(-1.).multiply(scNu0.sin()))
                            .add(vel0.scalarMultiply(sma.getField().getOne().divide(k2.multiply(rho0))));

        final T[] xz_coord0 = MathArrays.buildArray(trueAnomaly.getField(), 4);
        xz_coord0[0] = pos_tilde0.getEntry(0, 0);
        xz_coord0[1] = pos_tilde0.getEntry(2, 0);
        xz_coord0[2] = vel_tilde0.getEntry(0, 0);
        xz_coord0[3] = vel_tilde0.getEntry(2, 0);
        final T[] y_coord0 = MathArrays.buildArray(trueAnomaly.getField(), 2);
        y_coord0[0] = pos_tilde0.getEntry(1, 0);
        y_coord0[1] = vel_tilde0.getEntry(1, 0);

        // compute at time t and theta
        final FieldVector<T> xz_coord = inPlaneMatrix.operate(MatrixUtils.createFieldVector(xz_coord0));
        final FieldVector<T> y_coord = outPlaneMatrix.operate(MatrixUtils.createFieldVector(y_coord0));

        // Reshape the vectors
        final T[] array_pos = MathArrays.buildArray(trueAnomaly.getField(), 3);
        array_pos[0] = xz_coord.getEntry(0);
        array_pos[1] = y_coord.getEntry(0);
        array_pos[2] = xz_coord.getEntry(1);

        final T[] array_vel = MathArrays.buildArray(trueAnomaly.getField(), 3);
        array_vel[0] = xz_coord.getEntry(2);
        array_vel[1] = y_coord.getEntry(1);
        array_vel[2] = xz_coord.getEntry(3);

        final FieldVector<T> pos_tilde = MatrixUtils.createFieldVector(array_pos);
        final FieldVector<T> vel_tilde = MatrixUtils.createFieldVector(array_vel);

        final FieldVector<T> pos = pos_tilde.mapDivide(rho);
        final FieldVector<T> vel = pos_tilde.mapMultiply(k2.multiply(eccentricity).multiply(scNu.sin()))
                                            .add(vel_tilde.mapMultiply(k2.multiply(rho)));

        final FieldVector3D<T> pos1 = new FieldVector3D<>(pos.toArray());
        final FieldVector3D<T> vel1 = new FieldVector3D<>(vel.toArray());

        // Return transformed PV
        return new FieldPVCoordinates<>(pos1, vel1);
    }
}

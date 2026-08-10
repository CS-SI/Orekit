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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.SinCos;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * This class stores the 2 matrices (in plane: xz, out of plane: y, in Local LVLH CCSDS frame) of the Yamanaka Ankersen
 * equations. Matrices in LVLH CCSDS Frame.
 *
 * @author Romain Cuvillon
 * @since 14.0
 */
public class YamanakaAnkersenMatrices {

    /**
     * Time since epoch used in computing the matrices in seconds.
     */
    private final double timeSinceEpoch;

    /**
     * True anomaly of the target spacecraft.
     */

    private final double targetTheta;

    /**
     * Matrix to compute in-plane (x-z) components of position and velocity from initial in-plane components.
     */

    private final RealMatrix inPlaneMatrix;

    /**
     * Matrix to compute out of plane (y) components of position and velocity from initial out of plane components.
     */

    private final RealMatrix outPlaneMatrix;

    /**
     * Constructor for the state transition matrices given by the Yamanaka-Ankersen equations.
     *
     * @param timeSinceEpoch duration in seconds since epoch
     * @param targetTheta    true anomaly of the target
     * @param inPlaneMatrix  in-plane transition matrix
     * @param outPlaneMatrix out-plane transition matrix
     */
    public YamanakaAnkersenMatrices(final double timeSinceEpoch, final double targetTheta,
                                    final RealMatrix inPlaneMatrix, final RealMatrix outPlaneMatrix) {
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
    public double getTargetTrueAnomaly() {
        return targetTheta;
    }

    /**
     * Get the in-plane matrix at current time.
     *
     * @return In-plane matrix at current time
     */
    public RealMatrix getInPlaneMatrix() {
        return inPlaneMatrix;
    }

    /**
     * Get the out of plane matrix at current time.
     *
     * @return Out of plane matrix at current time
     */
    public RealMatrix getOutPlaneMatrix() {
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
     * @return PVT at the time encoded in the Yamanaka-Ankersen matrices, expressed in the target's LVLH CCSDS LOF.
     */
    public TimeStampedPVCoordinates transform(final TimeStampedPVCoordinates pvt, final double initialTrueAnomaly,
                                              final double trueAnomaly, final double eccentricity, final double sma,
                                              final double mu) {
        return new TimeStampedPVCoordinates(pvt.getDate().shiftedBy(timeSinceEpoch),
                                            transform(new PVCoordinates(pvt.getPosition(), pvt.getVelocity()),
                                                      initialTrueAnomaly, trueAnomaly, eccentricity, sma, mu));
    }

    /**
     * Transforms the input initial PVT expressed in the target's LVLH CCSDS LOF to the PVT at the time and true anomaly
     * associated encoded in the Yamanaka-Ankersen matrices contained in the object. First transform LOF PV coordinates
     * to ~ coordinates system as presented in the Yamanaka Ankersen paper. Compute actualized coordinates in the ~
     * system Retransform from transformed frame to LOF to get desired PVCoordinates in LOF.
     *
     * @param pv                 input initial PV expressed in the target's LVLH CCSDS LOF.
     * @param initialTrueAnomaly true anomaly of the target Spacecraft at initial time.
     * @param trueAnomaly        true anomaly of the target Spacecraft at final time.
     * @param eccentricity       eccentricity of the target Orbit.
     * @param sma                semi-major axis of the target Orbit.
     * @param mu                 mu of the target orbit.
     * @return PVT at the time encoded in the Yamanaka-Ankersen matrices, expressed in the target's LVLH CCSDS LOF.
     */
    public PVCoordinates transform(final PVCoordinates pv, final double initialTrueAnomaly, final double trueAnomaly,
                                   final double eccentricity, final double sma, final double mu) {
        // Compute constant k2
        final double p = sma * (1 - eccentricity * eccentricity);
        final double k2 = FastMath.sqrt(mu / (p * p * p));

        // Pre-compute quantities
        final SinCos scNu0 = FastMath.sinCos(initialTrueAnomaly);
        final double rho0 = 1 + eccentricity * scNu0.cos();
        final SinCos scNu = FastMath.sinCos(trueAnomaly);
        final double rho = 1 + eccentricity * scNu.cos();

        // Convert Vector3D to RealMatrix
        final RealMatrix pos0 = MatrixUtils.createColumnRealMatrix(pv.getPosition().toArray());
        final RealMatrix vel0 = MatrixUtils.createColumnRealMatrix(pv.getVelocity().toArray());

        // Transform coordinates in the ~ (tilde) coordinate system
        final RealMatrix pos_tilde0 = pos0.scalarMultiply(rho0);
        final RealMatrix vel_tilde0 = pos0.scalarMultiply(-eccentricity * scNu0.sin())
                                          .add(vel0.scalarMultiply(1 / (k2 * rho0)));

        // Reform the vectors [x,z,vx,vz] and [y,vy]
        final double[] xz_coord0 = {
                        pos_tilde0.getEntry(0, 0), pos_tilde0.getEntry(2, 0), vel_tilde0.getEntry(0, 0),
                        vel_tilde0.getEntry(2, 0)
        };
        final double[] y_coord0 = {pos_tilde0.getEntry(1, 0), vel_tilde0.getEntry(1, 0)};

        // compute at time t and theta
        final RealVector xz_coord = inPlaneMatrix.operate(MatrixUtils.createRealVector(xz_coord0));
        final RealVector y_coord = outPlaneMatrix.operate(MatrixUtils.createRealVector(y_coord0));

        // Reshape the vectors
        final RealVector pos_tilde = MatrixUtils.createRealVector(
                        new double[] {xz_coord.getEntry(0), y_coord.getEntry(0), xz_coord.getEntry(1)});
        final RealVector vel_tilde = MatrixUtils.createRealVector(
                        new double[] {xz_coord.getEntry(2), y_coord.getEntry(1), xz_coord.getEntry(3)});

        // Compute final vectors in LOF
        final RealVector pos = pos_tilde.mapDivide(rho);
        final RealVector vel = pos_tilde.mapMultiply(k2 * eccentricity * scNu.sin())
                                        .add(vel_tilde.mapMultiply(k2 * rho));
        final Vector3D pos1 = new Vector3D(pos.toArray());
        final Vector3D vel1 = new Vector3D(vel.toArray());

        // Return transformed PV
        return new PVCoordinates(pos1, vel1);
    }
}


/* Copyright 2023 Luc Maisonobe
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
package org.orekit.files.ccsds.ndm.adm;

import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.SinCos;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Utility to extract precession from the motion of a spin axis.
 * <p>
 * The precession is used in {@link AttitudeType#SPIN_NUTATION} and
 * {@link AttitudeType#SPIN_NUTATION_MOMENTUM} attitude types. CCSDS
 * calls it nutation, but it is really precession.
 * </p>
 * @author Luc Maisonobe
 * @since 12.0
 */
class PrecessionFinder {

    /** Precession axis (axis of the cone described by spin). */
    private final Vector3D axis;

    /** Precession angle (fixed cone angle between precession axis and spin axis). */
    private final double precessionAngle;

    /** Angular velocity around the precession axis (rad/s). */
    private final double angularVelocity;

    /** Build from spin axis motion.
     * <p>
     * Note that the derivatives up to second order are really needed
     * in order to retrieve the precession motion.
     * </p>
     * @param spin spin axis, including value, first and second derivative
     */
    PrecessionFinder(final FieldVector3D<UnivariateDerivative2> spin) {

        // using a suitable coordinates frame, the spin axis can be considered to describe
        // a cone of half aperture angle 0 ≤ η ≤ π around k axis, at angular rate ω ≥ 0
        // with an initial position in the (+i,±k) half-plane. In this frame, the normalized
        // direction of spin s = spin/||spin|| and its first and second time derivatives
        // have coordinates:
        //           s(t):        (sin η cos ω(t-t₀), sin η sin ω(t-t₀), cos η)
        //           ds/dt(t):    (-ω sin η sin ω(t-t₀), ω sin η cos ω(t-t₀), 0)
        //           d²s/dt²(t):  (-ω² sin η cos ω(t-t₀), -ω² sin η sin ω(t-t₀), 0)
        // at initial time t = t₀ this leads to:
        //           s₀ = s(t₀):       (sin η, 0, cos η)
        //           s₁ = ds/dt(t₀):   (0, ω sin η, 0)
        //           s₂ = d²s/dt²(t₀): (-ω² sin η, 0, 0)
        // however, only the spin vector itself is provided, we don't initially know the unit
        // vectors (i, j, k) of this "suitable coordinates frame". We can however easily
        // build another frame (u, v, w) from the normalized spin vector s as follows:
        //           u = s₀, v = s₁/||s₁||, w = u ⨯ v
        // the coordinates of vectors u, v and w in the "suitable coordinates frame" are:
        //           u: ( sin η,   0,  cos η)
        //           v: (     0,   1,      0)
        //           w: (-cos η,   0,  sin η)
        // we can then deduce the following relations, which can be computed regardless
        // of frames used to express the various vectors:
        //           s₁ · v = ω  sin η  = ||s₁||
        //           s₂ · w = ω² sin η cos η
        // these relations can be solved for η and ω (we know that 0 ≤ η ≤ π and ω ≥ 0):
        //           η = atan2(||s₁||², s₂ · w)
        //           ω = ||s₁|| / sin  η
        // then the k axis, which is the precession axis, can be deduced as:
        //           k = cos η u + sin η w

        final UnivariateDerivative2 nS = spin.getNorm();
        if (nS.getValue() == 0) {
            // special case, no motion at all, set up arbitrary values
            axis            = Vector3D.PLUS_K;
            precessionAngle = 0.0;
            angularVelocity = 0.0;
        } else {

            // build the derivatives vectors
            final FieldVector3D<UnivariateDerivative2> s = spin.scalarMultiply(nS.reciprocal());
            final Vector3D s0 = new Vector3D(s.getX().getValue(),
                                             s.getY().getValue(),
                                             s.getZ().getValue());
            final Vector3D s1 = new Vector3D(s.getX().getFirstDerivative(),
                                             s.getY().getFirstDerivative(),
                                             s.getZ().getFirstDerivative());
            final Vector3D s2 = new Vector3D(s.getX().getSecondDerivative(),
                                             s.getY().getSecondDerivative(),
                                             s.getZ().getSecondDerivative());

            final double nV2 = s1.getNormSq();
            if (nV2 == 0.0) {
                // special case: we have a fixed spin vector
                axis            = s0;
                precessionAngle = 0.0;
                angularVelocity = 0.0;
            } else {

                // check second derivatives were provided ; we do it on spin rather than s2
                // and use test against exact 0.0 because the normalization process changes
                // the derivatives and what we really want to check are missing derivatives
                if (new Vector3D(spin.getX().getSecondDerivative(),
                                 spin.getY().getSecondDerivative(),
                                 spin.getZ().getSecondDerivative()).getNormSq() == 0) {
                    throw new OrekitException(OrekitMessages.CANNOT_ESTIMATE_PRECESSION_WITHOUT_PROPER_DERIVATIVES);
                }

                // build the unit vectors
                final double   nV = FastMath.sqrt(nV2);
                final Vector3D v  = s1.scalarMultiply(1.0 / nV);
                final Vector3D w  = Vector3D.crossProduct(s0, v);

                // compute precession model
                precessionAngle  = FastMath.atan2(nV2, Vector3D.dotProduct(s2, w));
                final SinCos sc  = FastMath.sinCos(precessionAngle);
                angularVelocity  = nV / sc.sin();
                axis             = new Vector3D(sc.cos(), s0, sc.sin(), w);

            }
        }

    }

    /** Get the precession axis.
     * @return precession axis
     */
    public Vector3D getAxis() {
        return axis;
    }

    /** Get the precession angle.
     * @return fixed cone angle between precession axis an spin axis (rad)
     */
    public double getPrecessionAngle() {
        return precessionAngle;
    }

    /** Get the angular velocity around precession axis.
     * @return angular velocity around precession axis (rad/s)
     */
    public double getAngularVelocity() {
        return angularVelocity;
    }

}

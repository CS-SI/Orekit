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
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.util.MathArrays;


/**
 * This class implements the state transition matrices of the Clohessy-Wiltshire equations of relative motion between two spacecraft.
 * Source : Orbital Mechanics for Engineering Students, Curtis
 *
 * @author Romain Cuvillon
 * @since 14.0
 * @param <T> Any scalar field.
 */
public class FieldClohessyWiltshireEquations<T extends CalculusFieldElement<T>> {
    /**
     * Creates a new {@link FieldClohessyWiltshireEquations} object.
     */
    public FieldClohessyWiltshireEquations() {
    }

    /**
     * Computes the state transition matrices at the given time since epoch.
     *
     * @param timeSinceEpoch Time since epoch, given in seconds.
     * @param meanMotion     Mean motion of the target's CIRCULAR orbit.
     * @return 4 3x3 state transition matrices.
     */
    public FieldClohessyWiltshireMatrices<T> computeMatrices(final T timeSinceEpoch, final T meanMotion) {
        final T nt = meanMotion.multiply(timeSinceEpoch);
        final T sin_nt = nt.sin();
        final T cos_nt = nt.cos();
        final T zero = meanMotion.getField().getZero();

        final T[][] phi_rr = MathArrays.buildArray(meanMotion.getField(), 3, 3);
        phi_rr[0][0] = cos_nt.multiply(-3.).add(4);
        phi_rr[0][1] = zero;
        phi_rr[0][2] = zero;
        phi_rr[1][0] = sin_nt.subtract(nt).multiply(6);
        phi_rr[1][1] = zero.add(1);
        phi_rr[1][2] = zero;
        phi_rr[2][0] = zero;
        phi_rr[2][1] = zero;
        phi_rr[2][2] = cos_nt;

        final T[][] phi_rv = MathArrays.buildArray(meanMotion.getField(), 3, 3);
        phi_rv[0][0] = sin_nt.divide(meanMotion);
        phi_rv[0][1] = zero.add(1).subtract(cos_nt).multiply(2).divide(meanMotion);
        phi_rv[0][2] = zero;
        phi_rv[1][0] = cos_nt.subtract(1).multiply(2).divide(meanMotion);
        phi_rv[1][1] = sin_nt.multiply(4).subtract(nt.multiply(3)).divide(meanMotion);
        phi_rv[1][2] = zero;
        phi_rv[2][0] = zero;
        phi_rv[2][1] = zero;
        phi_rv[2][2] = sin_nt.divide(meanMotion);

        final T[][] phi_vr = MathArrays.buildArray(meanMotion.getField(), 3, 3);
        phi_vr[0][0] = meanMotion.multiply(sin_nt).multiply(3);
        phi_vr[0][1] = zero;
        phi_vr[0][2] = zero;
        phi_vr[1][0] = meanMotion.multiply(cos_nt.subtract(1)).multiply(6);
        phi_vr[1][1] = zero;
        phi_vr[1][2] = zero;
        phi_vr[2][0] = zero;
        phi_vr[2][1] = zero;
        phi_vr[2][2] = sin_nt.multiply(meanMotion).negate();

        final T[][] phi_vv = MathArrays.buildArray(meanMotion.getField(), 3, 3);
        phi_vv[0][0] = cos_nt;
        phi_vv[0][1] = sin_nt.multiply(2);
        phi_vv[0][2] = zero;
        phi_vv[1][0] = sin_nt.multiply(-2.);
        phi_vv[1][1] = cos_nt.multiply(4).subtract(3);
        phi_vv[1][2] = zero;
        phi_vv[2][0] = zero;
        phi_vv[2][1] = zero;
        phi_vv[2][2] = cos_nt;

        return new FieldClohessyWiltshireMatrices<>(timeSinceEpoch, MatrixUtils.createFieldMatrix(phi_rr), MatrixUtils.createFieldMatrix(phi_rv),
                MatrixUtils.createFieldMatrix(phi_vr), MatrixUtils.createFieldMatrix(phi_vv));
    }
}

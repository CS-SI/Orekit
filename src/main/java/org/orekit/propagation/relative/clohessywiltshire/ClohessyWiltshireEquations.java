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

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.util.FastMath;

/**
 * This class implements the state transition matrices of the Clohessy-Wiltshire equations of relative motion between two spacecraft.
 * Source : Orbital Mechanics for Engineering Students, Curtis
 *
 * @author Jérôme Tabeaud
 * @author Romain Cuvillon
 * @since 14.0
 */
public class ClohessyWiltshireEquations {

    /**
     * Private constructor to prevent instantiation.
     */
    private ClohessyWiltshireEquations() {
    }

    /**
     * Computes the state transition matrices at the given time since epoch.
     *
     * @param timeSinceEpoch Time since epoch, given in seconds.
     * @param meanMotion     Mean motion of the target's CIRCULAR orbit.
     * @return 4 3x3 state transition matrices.
     */
    public static ClohessyWiltshireMatrices computeMatrices(final double timeSinceEpoch, final double meanMotion) {
        final double nt = meanMotion * timeSinceEpoch;
        final double sin_nt = FastMath.sin(nt);
        final double cos_nt = FastMath.cos(nt);

        final double[][] phi_rr = { {4.0 - 3 * cos_nt, 0.0, 0.0},
                                    {6.0 * (sin_nt - nt), 1.0, 0.0},
                                    {0.0, 0.0, cos_nt}};

        final double[][] phi_rv = { {(1.0 / meanMotion) * sin_nt, (2.0 / meanMotion) * (1.0 - cos_nt), 0.0},
                                    {(2.0 / meanMotion) * (cos_nt - 1.0), (1.0 / meanMotion) * (4.0 * sin_nt - 3.0 * nt), 0.0},
                                    {0.0, 0.0, (1 / meanMotion) * sin_nt}};

        final double[][] phi_vr = { {3.0 * meanMotion * sin_nt, 0.0, 0.0},
                                    {6.0 * meanMotion * (cos_nt - 1), 0.0, 0.0},
                                    {0.0, 0.0, -meanMotion * sin_nt}};

        final double[][] phi_vv = { {cos_nt, 2.0 * sin_nt, 0.0},
                                    {-2.0 * sin_nt, 4.0 * cos_nt - 3.0, 0.0},
                                    {0.0, 0.0, cos_nt}};

        return new ClohessyWiltshireMatrices(timeSinceEpoch, MatrixUtils.createRealMatrix(phi_rr), MatrixUtils.createRealMatrix(phi_rv),
                MatrixUtils.createRealMatrix(phi_vr), MatrixUtils.createRealMatrix(phi_vv));
    }
}

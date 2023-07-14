/* Copyright 2002-2023 CS GROUP
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
package org.orekit.gnss.antenna;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;

/**
 * Interpolator for 2D phase center variation data.
 *
 * @author Luc Maisonobe
 * @since 9.2
 */
public class TwoDVariation implements PhaseCenterVariationFunction {

    /** Start polar angle. */
    private final double polarStart;

    /** Step between grid points. */
    private final double polarStep;

    /** Step between grid points. */
    private final double azimuthStep;

    /** Sampled phase center variations. */
    private final double[][] variations;

    /** Simple constructor.
     * @param polarStart start polar angle
     * @param polarStep between grid points
     * @param azimuthStep step between grid points
     * @param variations sampled phase center variations
     */
    public TwoDVariation(final double polarStart, final double polarStep,
                         final double azimuthStep, final double[][] variations) {
        this.polarStart  = polarStart;
        this.polarStep   = polarStep;
        this.azimuthStep = azimuthStep;
        this.variations  = new double[variations.length][];
        for (int i = 0; i < variations.length; ++i) {
            this.variations[i] = variations[i].clone();
        }
    }

    /** {@inheritDoc} */
    @Override
    public double value(final double polarAngle, final double azimuthAngle) {

        // find surrounding points
        final double az      = MathUtils.normalizeAngle(azimuthAngle, FastMath.PI);
        final int    iBase   = (int) FastMath.floor(az / azimuthStep);
        final int    i       = FastMath.max(0, FastMath.min(variations.length - 2, iBase));
        final int    jBase   = (int) FastMath.floor((polarAngle - polarStart) / polarStep);
        final int    j       = FastMath.max(0, FastMath.min(variations[i].length - 2, jBase));

        final double aInf    = i * azimuthStep;
        final double aSup    = aInf + azimuthStep;
        final double pInf    = polarStart + j * polarStep;
        final double pSup    = pInf + polarStep;

        final double vInfInf = variations[i][j];
        final double vInfSup = variations[i][j + 1];
        final double vSupInf = variations[i + 1][j];
        final double vSupSup = variations[i + 1][j + 1];

        // bilinear interpolation
        final double vInf = ((polarAngle - pInf) * vInfSup + (pSup - polarAngle) * vInfInf) / polarStep;
        final double vSup = ((polarAngle - pInf) * vSupSup + (pSup - polarAngle) * vSupInf) / polarStep;
        return ((az - aInf) * vSup + (aSup - az) * vInf) / azimuthStep;

    }

}

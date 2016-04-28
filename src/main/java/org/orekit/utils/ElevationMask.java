/* Copyright 2013 Applied Defense Solutions, Inc.
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.utils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;

/**
 * Class for modeling the ground elevation values around a given point.
 * <p>
 * Instances of this class can be considered to be immutable
 * @author Hank Grabowski
 * @since 6.1
 */
public class ElevationMask implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20131118L;

    /** Azimuth-elevation mask. */
    private final double[][] azelmask;

    /**
     * Creates an instance of an Elevation mask based on the passed in parameter.
     * @param mask azimuth-elevation mask (rad)
     */
    public ElevationMask(final double[][] mask) {
        this.azelmask = checkMask(mask);
    }

    /** Get the interpolated elevation for a given azimuth according to the mask.
     * @param azimuth azimuth (rad)
     * @return elevation angle (rad)
     */
    public double getElevation(final double azimuth) {
        double elevation = 0.0;
        boolean fin = false;
        for (int i = 1; i < azelmask.length & !fin; i++) {
            if (azimuth <= azelmask[i][0]) {
                fin = true;
                final double azd = azelmask[i - 1][0];
                final double azf = azelmask[i][0];
                final double eld = azelmask[i - 1][1];
                final double elf = azelmask[i][1];
                elevation = eld + (azimuth - azd) * (elf - eld) / (azf - azd);
            }
        }
        return elevation;
    }

    /** Checking and ordering the azimuth-elevation tabulation.
     * @param azimelev azimuth-elevation tabulation to be checked and ordered
     * @return ordered azimuth-elevation tabulation ordered
     */
    private static double[][] checkMask(final double[][] azimelev) {

        // Copy of the given mask
        final double[][] mask = new double[azimelev.length + 2][azimelev[0].length];
        for (int i = 0; i < azimelev.length; i++) {
            System.arraycopy(azimelev[i], 0, mask[i + 1], 0, azimelev[i].length);
            // Reducing azimuth between 0 and 2*Pi
            mask[i + 1][0] = MathUtils.normalizeAngle(mask[i + 1][0], FastMath.PI);
        }

        // Sorting the mask with respect to azimuth
        Arrays.sort(mask, 1, mask.length - 1, new Comparator<double[]>() {
            public int compare(final double[] d1, final double[] d2) {
                return Double.compare(d1[0], d2[0]);
            }
        });

        // Extending the mask in order to cover [0, 2PI] in azimuth
        mask[0][0] = mask[mask.length - 2][0] - MathUtils.TWO_PI;
        mask[0][1] = mask[mask.length - 2][1];
        mask[mask.length - 1][0] = mask[1][0] + MathUtils.TWO_PI;
        mask[mask.length - 1][1] = mask[1][1];

        // Checking the sorted mask: same azimuth modulo 2PI must have same elevation
        for (int i = 1; i < mask.length; i++) {
            if (Double.compare(mask[i - 1][0], mask[i][0]) == 0) {
                if (Double.compare(mask[i - 1][1], mask[i][1]) != 0) {
                    throw new OrekitIllegalArgumentException(OrekitMessages.UNEXPECTED_TWO_ELEVATION_VALUES_FOR_ONE_AZIMUTH,
                                                             mask[i - 1][1], mask[i][1], mask[i][0]);
                }
            }
        }

        return mask;
    }


}

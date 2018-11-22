/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.models.earth;

import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.ParameterDriver;

/** The Vienna1 tropospheric delay model for radio techniques.
 * The Vienna model data are given with a time interval of 6 hours
 * as well as on a global 2.5° * 2.0° grid.
 *
 * This version considered the height correction for the hydrostatic part
 * developed by Niell, 1996.
 *
 * @see Boehm, J., Werl, B., and Schuh, H., (2006),
 *      "Troposhere mapping functions for GPS and very long baseline
 *      interferometry from European Centre for Medium-Range Weather
 *      Forecasts operational analysis data," J. Geophy. Res., Vol. 111,
 *      B02406, doi:10.1029/2005JB003629
 *
 * @author Bryan Cazabonne
 */
public class ViennaOneModel implements DiscreteTroposphericModel {

    /** Serializable UID. */
    private static final long serialVersionUID = 2584920506094034855L;

    /** The a coefficient for the computation of the wet and hydrostatic mapping functions.*/
    private final double[] coefficientsA;

    /** Values of hydrostatic and wet delays as provided by the Vienna model. */
    private final double[] zenithDelay;

    /** Geodetic site latitude, radians.*/
    private final double latitude;

    /** Build a new instance.
     * @param coefficientA The a coefficients for the computation of the wet and hydrostatic mapping functions.
     * @param zenithDelay Values of hydrostatic and wet delays
     * @param latitude geodetic latitude of the station
     */
    public ViennaOneModel(final double[] coefficientA, final double[] zenithDelay,
                          final double latitude) {
        this.coefficientsA = coefficientA.clone();
        this.zenithDelay   = zenithDelay.clone();
        this.latitude      = latitude;
    }

    /** {@inheritDoc} */
    @Override
    public double pathDelay(final double elevation, final double height,
                            final double[] parameters, final AbsoluteDate date) {
        // zenith delay
        final double[] delays = computeZenithDelay(height, parameters);
        // mapping function
        final double[] mappingFunction = mappingFactors(height, elevation, date);
        // Tropospheric path delay
        return delays[0] * mappingFunction[0] + delays[1] * mappingFunction[1];
    }

    /** {@inheritDoc} */
    @Override
    public double[] computeZenithDelay(final double height, final double[] parameters) {
        return zenithDelay;
    }

    /** {@inheritDoc} */
    @Override
    public double[] mappingFactors(final double height, final double elevation,
                                   final AbsoluteDate date) {
        // Day of year computation
        final DateTimeComponents dtc = date.getComponents(TimeScalesFactory.getUTC());
        final int dofyear = dtc.getDate().getDayOfYear();

        // General constants | Hydrostatic part
        final double bh  = 0.0029;
        final double c0h = 0.062;
        final double c10h;
        final double c11h;
        final double psi;

        // sin(latitude) > 0 -> northern hemisphere
        if (FastMath.sin(latitude) > 0) {
            c10h = 0.001;
            c11h = 0.005;
            psi  = 0;
        } else {
            c10h = 0.002;
            c11h = 0.007;
            psi  = FastMath.PI;
        }

        // Compute hydrostatique coefficient c
        final double coef = ((dofyear - 28) / 365) * 2 * FastMath.PI + psi;
        final double ch = c0h + ((FastMath.cos(coef) + 1) * (c11h / 2) + c10h) * (1 - FastMath.cos(latitude));

        // General constants | Wet part
        final double bw = 0.00146;
        final double cw = 0.04391;

        final double[] function = new double[2];
        function[0] = computeFunction(coefficientsA[0], bh, ch, elevation);
        function[1] = computeFunction(coefficientsA[1], bw, cw, elevation);

        // Apply height correction
        final double correction = computeHeightCorrection(elevation, height);
        function[0] = function[0] + correction;

        return function;
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getParametersDrivers() {
        return new ParameterDriver[0];
    }

    /** Compute the mapping function related to the coefficient values and the elevation.
     * @param a a coefficient
     * @param b b coefficient
     * @param c c coefficient
     * @param elevation the elevation of the satellite, in radians.
     * @return the value of the function at a given elevation
     */
    private double computeFunction(final double a, final double b, final double c, final double elevation) {
        final double sinE = FastMath.sin(elevation);
        // Numerator
        final double numMP = 1 + a / (1 + b / (1 + c));
        // Denominateur
        final double denMP = sinE + a / (sinE + b / (sinE + c));

        final double felevation = numMP / denMP;

        return felevation;
    }

    /** This method computes the height correction for the hydrostatic
     *  component of the mapping function.
     *  The formulas are given by Neill's paper, 1996:
     *<p>
     *      Niell A. E. (1996)
     *      "Global mapping functions for the atmosphere delay of radio wavelengths,”
     *      J. Geophys. Res., 101(B2), pp.  3227–3246, doi:  10.1029/95JB03048.
     *</p>
     * @param elevation the elevation of the satellite, in radians.
     * @param height the height of the station in m above sea level.
     * @return the height correction, in m
     */
    private double computeHeightCorrection(final double elevation, final double height) {
        final double sinE = FastMath.sin(elevation);
        // Ref: Eq. 4
        final double function = computeFunction(2.53e-5, 5.49e-3, 1.14e-3, elevation);
        // Ref: Eq. 6
        final double dmdh = (1 / sinE) - function;
        // Ref: Eq. 7
        final double correction = dmdh * (height / 1000);
        return correction;
    }

}

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
package org.orekit.models.earth;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.optim.MaxEval;
import org.hipparchus.optim.nonlinear.scalar.GoalType;
import org.hipparchus.optim.univariate.BrentOptimizer;
import org.hipparchus.optim.univariate.SearchInterval;
import org.hipparchus.optim.univariate.UnivariateObjectiveFunction;
import org.hipparchus.util.FastMath;
import org.orekit.models.AtmosphericRefractionModel;

/** Implementation of refraction model for Earth exponential atmosphere based on ITU-R P.834-7 recommendation.
 * <p>Refraction angle is computed according to the International Telecommunication Union recommendation formula.
 *  For reference, see <b>ITU-R P.834-7</b> (October 2015).</p>
 *
 * @author Thierry Ceolin
 * @since 7.1
 */

public class EarthITU453AtmosphereRefraction implements AtmosphericRefractionModel {

    /** Altitude conversion factor. */
    private static final double KM_TO_M = 1000.0;

    /** Coefficients conversion factor. */
    private static final double INV_DEG_TO_INV_RAD = 180.0 / FastMath.PI;

    /** Default a coefficients to compute refractive index for a typical atmosphere. */
    private static final double DEFAULT_CORRECTION_ACOEF = 0.000315;

    /** Default b coefficients to compute refractive index for a typical atmosphere. */
    private static final double DEFAULT_CORRECTION_BCOEF = 0.1361 / KM_TO_M;

    /** Earth ray as defined in ITU-R P.834-7 (m). */
    private static final double EARTH_RAY = 6370.0 * KM_TO_M;

    /** Default coefficients array for Tau function (formula number 9).
     * The coefficients have been converted to SI units
     */
    private static final double[] CCOEF = {
        INV_DEG_TO_INV_RAD * 1.314,  INV_DEG_TO_INV_RAD * 0.6437,  INV_DEG_TO_INV_RAD * 0.02869,
        INV_DEG_TO_INV_RAD * 0.2305 / KM_TO_M, INV_DEG_TO_INV_RAD * 0.09428 / KM_TO_M, INV_DEG_TO_INV_RAD * 0.01096 / KM_TO_M,
        INV_DEG_TO_INV_RAD * 0.008583 / (KM_TO_M * KM_TO_M)
    };

    /** Default coefficients array for TauZero function (formula number 14).
     * The coefficients have been converted to SI units
     */
    private static final double[] CCOEF0 = {
        INV_DEG_TO_INV_RAD * 1.728, INV_DEG_TO_INV_RAD * 0.5411, INV_DEG_TO_INV_RAD * 0.03723,
        INV_DEG_TO_INV_RAD * 0.1815 / KM_TO_M, INV_DEG_TO_INV_RAD * 0.06272 / KM_TO_M, INV_DEG_TO_INV_RAD * 0.011380 / KM_TO_M,
        INV_DEG_TO_INV_RAD * 0.01727 / (KM_TO_M * KM_TO_M), INV_DEG_TO_INV_RAD * 0.008288 / (KM_TO_M * KM_TO_M)
    };

    /** Serializable UID. */
    private static final long serialVersionUID = 20160118L;

    /** station altitude (m). */
    private final double altitude;

    /** minimal elevation angle for the station (rad). */
    private final double thetamin;

    /** minimal elevation angle under free-space propagation (rad). */
    private final double theta0;

    /** elevation where elevation+refraction correction is minimal (near inequality formula number 11 validity domain). */
    private final double elev_star;

    /** refraction correction value where elevation+refraction correction is minimal (near inequality 11 validity domain). */
    private final double refrac_star;

    /** Creates a new default instance.
     * @param altitude altitude of the ground station from which measurement is performed (m)
     */
    public EarthITU453AtmosphereRefraction(final double altitude) {
        this.altitude = altitude;
        thetamin = getMinimalElevation(altitude);
        theta0   = thetamin - getTau(thetamin);

        UnivariateFunction refrac = new UnivariateFunction() {
            public double value (final double elev) {
                return elev + getBaseRefraction(elev);
            }
        };

        final double rel = 1.e-5;
        final double abs = 1.e-10;
        final BrentOptimizer optimizer = new BrentOptimizer(rel, abs);

        // Call optimizer
        elev_star = optimizer.optimize(new MaxEval(200),
                                       new UnivariateObjectiveFunction(refrac),
                                       GoalType.MINIMIZE,
                                       new SearchInterval(-FastMath.PI / 30., FastMath.PI / 4)).getPoint();
        refrac_star = getBaseRefraction(elev_star);
    };

    /** Compute the refractive index correction in the case of a typical atmosphere.
     * ITU-R P.834-7, formula number 8, page 3
     * @param alt altitude of the station at the Earth surface (m)
     * @return the refractive index
     */
    private double getRefractiveIndex(final double alt) {

        return 1.0 + DEFAULT_CORRECTION_ACOEF * FastMath.exp(-DEFAULT_CORRECTION_BCOEF * alt);
    }

    /** Compute the minimal elevation angle for a station.
     * ITU-R P.834-7, formula number 10, page 3
     * @param alt altitude of the station at the Earth surface (m)
     * @return the minimal elevation angle (rad)
     */
    private double getMinimalElevation(final double alt) {

        return -FastMath.acos( EARTH_RAY / (EARTH_RAY + alt) * getRefractiveIndex(0.0) / getRefractiveIndex(alt));
    }


    /** Compute the refraction correction in the case of a reference atmosphere.
     * ITU-R P.834-7, formula number 9, page 3
     * @param elevation elevation angle (rad)
     * @return the refraction correction angle (rad)
     */
    private double getTau(final double elevation) {

        final double eld = FastMath.toDegrees(elevation);
        final double tmp0 = CCOEF[0] + CCOEF[1] * eld + CCOEF[2] * eld * eld;
        final double tmp1 = altitude * (CCOEF[3] + CCOEF[4] * eld + CCOEF[5] * eld * eld);
        final double tmp2 = altitude * altitude * CCOEF[6];
        return 1.0 / (tmp0 + tmp1 + tmp2);
    }


    /** Compute the refraction correction in the case of a reference atmosphere.
     * ITU-R P.834-7, formula number 14, page 3
     * @param elevationZero elevation angle (rad)
     * @return the refraction correction angle (rad)
     */

    private double getTauZero(final double elevationZero) {

        final double eld = FastMath.toDegrees(elevationZero);
        final double tmp0 = CCOEF0[0] + CCOEF0[1] * eld + CCOEF0[2] * eld * eld;
        final double tmp1 = altitude * (CCOEF0[3] + CCOEF0[4] * eld + CCOEF0[5] * eld * eld);
        final double tmp2 = altitude * altitude * (CCOEF0[6] + CCOEF0[7] * eld);
        return 1.0 / (tmp0 + tmp1 + tmp2);
    }

    /** Compute the refraction correction in the case of a reference atmosphere without validity domain.
     * The computation is done even if the inequality (formula number 11) is not verified
     * ITU-R P.834-7, formula number 14, page 3
     * @param elevation elevation angle (rad)
     * @return the refraction correction angle (rad)
     */
    private double getBaseRefraction(final double elevation) {
        return getTauZero(elevation);
    }

    /** Get the station minimal elevation angle.
     * @return the minimal elevation angle (rad)
     */
    public double getThetaMin() {
        return thetamin;
    }

    /** Get the station elevation angle under free-space propagation .
     * @return the elevation angle under free-space propagation (rad)
     */
    public double getTheta0() {
        return theta0;
    }

    @Override
    /** {@inheritDoc} */
    // elevation (rad)
    // return refraction correction (rad)
    public double getRefraction(final double elevation) {
        if (elevation < elev_star ) {
            return refrac_star;
        }
        // The validity of the formula is extended for negative elevation,
        // ensuring that the refraction correction angle doesn't make visible a satellite with a too negative elevation
        // elev_star is used instead of thetam (minimal elevation angle).
        return getTauZero(elevation);
    }

}

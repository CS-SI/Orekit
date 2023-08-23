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
package org.orekit.gnss.attitude;

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;

/**
 * Attitude providers for GPS block IIA navigation satellites.
 * <p>
 * This class is based on the May 2017 version of J. Kouba eclips.f
 * subroutine available at <a href="http://acc.igs.org/orbits">IGS Analysis
 * Center Coordinator site</a>. The eclips.f code itself is not used ; its
 * hard-coded data are used and its low level models are used, but the
 * structure of the code and the API have been completely rewritten.
 * </p>
 * @author J. Kouba original fortran routine
 * @author Luc Maisonobe Java translation
 * @since 9.2
 */
public class GPSBlockIIA extends AbstractGNSSAttitudeProvider {

    /** Default yaw bias (rad). */
    public static final double DEFAULT_YAW_BIAS = FastMath.toRadians(0.5);

    /** Satellite-Sun angle limit for a midnight turn maneuver. */
    private static final double NIGHT_TURN_LIMIT = FastMath.toRadians(180.0 - 13.25);

    /** Margin on turn end. */
    private static final double END_MARGIN = 1800.0;

    /** Default yaw rates for all spacecrafts in radians per seconds (indexed by prnNumber, hence entry 0 is unused). */
    private static final double[] DEFAULT_YAW_RATES = new double[] {
        Double.NaN,  // unused entry 0
        FastMath.toRadians(0.1211), FastMath.toRadians(0.1339), FastMath.toRadians(0.1230), FastMath.toRadians(0.1233),
        FastMath.toRadians(0.1180), FastMath.toRadians(0.1266), FastMath.toRadians(0.1269), FastMath.toRadians(0.1033),
        FastMath.toRadians(0.1278), FastMath.toRadians(0.0978), FastMath.toRadians(0.2000), FastMath.toRadians(0.1990),
        FastMath.toRadians(0.2000), FastMath.toRadians(0.0815), FastMath.toRadians(0.1303), FastMath.toRadians(0.0838),
        FastMath.toRadians(0.1401), FastMath.toRadians(0.1069), FastMath.toRadians(0.0980), FastMath.toRadians(0.1030),
        FastMath.toRadians(0.1366), FastMath.toRadians(0.1025), FastMath.toRadians(0.1140), FastMath.toRadians(0.1089),
        FastMath.toRadians(0.1001), FastMath.toRadians(0.1227), FastMath.toRadians(0.1194), FastMath.toRadians(0.1260),
        FastMath.toRadians(0.1228), FastMath.toRadians(0.1165), FastMath.toRadians(0.0969), FastMath.toRadians(0.1140)
    };

    /** Yaw rate for current spacecraft. */
    private final double yawRate;

    /** Yaw bias. */
    private final double yawBias;

    /** Simple constructor.
     * @param yawRate yaw rate to use in radians per seconds (typically {@link #getDefaultYawRate(int) GPSBlockIIA.getDefaultYawRate(prnNumber)})
     * @param yawBias yaw bias to use (rad) (typicall {@link #DEFAULT_YAW_BIAS})
     * @param validityStart start of validity for this provider
     * @param validityEnd end of validity for this provider
     * @param sun provider for Sun position
     * @param inertialFrame inertial frame where velocity are computed
     * @since 9.3
     */
    public GPSBlockIIA(final double yawRate, final double yawBias,
                       final AbsoluteDate validityStart, final AbsoluteDate validityEnd,
                       final ExtendedPVCoordinatesProvider sun, final Frame inertialFrame) {
        super(validityStart, validityEnd, sun, inertialFrame);
        this.yawRate = yawRate;
        this.yawBias = yawBias;
    }

    /** Get the default yaw rate for a satellite.
     * @param prnNumber satellite PRN
     * @return default yaw rate for the specified satellite
     * @since 10.0
     */
    public static double getDefaultYawRate(final int prnNumber) {
        return DEFAULT_YAW_RATES[prnNumber];
    }

    /** {@inheritDoc} */
    @Override
    protected TimeStampedAngularCoordinates correctedYaw(final GNSSAttitudeContext context) {

        // noon beta angle limit from yaw rate
        final double aNoon  = FastMath.atan(context.getMuRate() / yawRate);
        final double aNight = NIGHT_TURN_LIMIT;
        final double cNoon  = FastMath.cos(aNoon);
        final double cNight = FastMath.cos(aNight);

        if (context.setUpTurnRegion(cNight, cNoon)) {

            final double absBeta = FastMath.abs(context.beta(context.getDate()));
            context.setHalfSpan(context.inSunSide() ?
                                absBeta * FastMath.sqrt(aNoon / absBeta - 1.0) :
                                context.inOrbitPlaneAbsoluteAngle(aNight - FastMath.PI),
                                END_MARGIN);
            if (context.inTurnTimeRange()) {

                // we need to ensure beta sign does not change during the turn
                final double beta     = context.getSecuredBeta();
                final double phiStart = context.getYawStart(beta);
                final double dtStart  = context.timeSinceTurnStart();
                final double linearPhi;
                final double phiDot;
                if (context.inSunSide()) {
                    // noon turn
                    if (beta > 0 && beta < yawBias) {
                        // noon turn problem for small positive beta in block IIA
                        // rotation is in the wrong direction for these spacecrafts
                        phiDot    = FastMath.copySign(yawRate, beta);
                        linearPhi = phiStart + phiDot * dtStart;
                    } else {
                        // regular noon turn
                        phiDot    = -FastMath.copySign(yawRate, beta);
                        linearPhi = phiStart + phiDot * dtStart;
                    }
                } else {
                    // midnight turn
                    phiDot    = yawRate;
                    linearPhi = phiStart + phiDot * dtStart;
                }

                if (context.linearModelStillActive(linearPhi, phiDot)) {
                    // we are still in the linear model phase
                    return context.turnCorrectedAttitude(linearPhi, phiDot);
                }

            }

        }

        // in nominal yaw mode
        return context.nominalYaw(context.getDate());

    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> TimeStampedFieldAngularCoordinates<T> correctedYaw(final GNSSFieldAttitudeContext<T> context) {

        final Field<T> field = context.getDate().getField();

        // noon beta angle limit from yaw rate
        final T      aNoon  = FastMath.atan(context.getMuRate().divide(yawRate));
        final T      aNight = field.getZero().add(NIGHT_TURN_LIMIT);
        final double cNoon  = FastMath.cos(aNoon.getReal());
        final double cNight = FastMath.cos(aNight.getReal());

        if (context.setUpTurnRegion(cNight, cNoon)) {

            final T absBeta = FastMath.abs(context.beta(context.getDate()));
            context.setHalfSpan(context.inSunSide() ?
                                absBeta.multiply(FastMath.sqrt(aNoon.divide(absBeta).subtract(1.0))) :
                                context.inOrbitPlaneAbsoluteAngle(aNight.subtract(aNoon.getPi())),
                                END_MARGIN);
            if (context.inTurnTimeRange()) {

                // we need to ensure beta sign does not change during the turn
                final T beta     = context.getSecuredBeta();
                final T phiStart = context.getYawStart(beta);
                final T dtStart  = context.timeSinceTurnStart();
                final T linearPhi;
                final T phiDot;
                if (context.inSunSide()) {
                    // noon turn
                    if (beta.getReal() > 0 && beta.getReal() < yawBias) {
                        // noon turn problem for small positive beta in block IIA
                        // rotation is in the wrong direction for these spacecrafts
                        phiDot    = field.getZero().add(FastMath.copySign(yawRate, beta.getReal()));
                        linearPhi = phiStart.add(phiDot.multiply(dtStart));
                    } else {
                        // regular noon turn
                        phiDot    = field.getZero().add(-FastMath.copySign(yawRate, beta.getReal()));
                        linearPhi = phiStart.add(phiDot.multiply(dtStart));
                    }
                } else {
                    // midnight turn
                    phiDot    = field.getZero().add(yawRate);
                    linearPhi = phiStart.add(phiDot.multiply(dtStart));
                }

                if (context.linearModelStillActive(linearPhi, phiDot)) {
                    // we are still in the linear model phase
                    return context.turnCorrectedAttitude(linearPhi, phiDot);
                }

            }

        }

        // in nominal yaw mode
        return context.nominalYaw(context.getDate());

    }

}

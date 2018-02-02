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
package org.orekit.gnss.attitude;

import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;

/**
 * Attitude providers for GPS block IIR navigation satellites.
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

    /** Serializable UID. */
    private static final long serialVersionUID = 20171114L;

    /** Satellite-Sun angle limit for a midnight turn maneuver. */
    private static final double NIGHT_TURN_LIMIT = FastMath.toRadians(180.0 - 13.25);

    /** Bias. */
    private static final double YAW_BIAS = FastMath.toRadians(0.5);

    /** Yaw rates for all spacecrafts. */
    private static final double[] YAW_RATES = new double[] {
        0.1211, 0.1339, 0.1230, 0.1233, 0.1180, 0.1266, 0.1269, 0.1033,
        0.1278, 0.0978, 0.2000, 0.1990, 0.2000, 0.0815, 0.1303, 0.0838,
        0.1401, 0.1069, 0.0980, 0.1030, 0.1366, 0.1025, 0.1140, 0.1089,
        0.1001, 0.1227, 0.1194, 0.1260, 0.1228, 0.1165, 0.0969, 0.1140
    };

    /** Margin on turn end. */
    private final double END_MARGIN = 1800.0;

    /** Yaw rate for current spacecraft. */
    private final double yawRate;

    /** Simple constructor.
     * @param validityStart start of validity for this provider
     * @param validityEnd end of validity for this provider
     * @param sun provider for Sun position
     * @param prnNumber number within the GPS constellation (between 1 and 32)
     */
    public GPSBlockIIA(final AbsoluteDate validityStart, final AbsoluteDate validityEnd,
                       final PVCoordinatesProvider sun, final int prnNumber) {
        super(validityStart, validityEnd, sun);
        yawRate = FastMath.toRadians(YAW_RATES[prnNumber - 1]);
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

            final double absBeta = FastMath.abs(context.getBeta());
            context.setHalfSpan(context.inSunSide() ?
                                absBeta * FastMath.sqrt(aNoon / absBeta - 1.0) :
                                context.inOrbitPlaneAngle(aNight - FastMath.PI));
            if (context.inTurnTimeRange(context.getDate(), END_MARGIN)) {

                // we need to ensure beta sign does not change during the turn
                final double beta     = context.getSecuredBeta();
                final double phiStart = context.getYawStart(beta);
                final double dtStart  = context.timeSinceTurnStart(context.getDate());
                final double phi;
                if (context.inSunSide()) {
                    // noon turn
                    final double linearPhi;
                    if (beta > 0 && beta < YAW_BIAS) {
                        // noon turn problem for small positive beta in block IIA
                        // rotation is in the wrong direction for these spacecrafts
                        linearPhi = phiStart + FastMath.copySign(yawRate, beta) * dtStart;
                    } else {
                        // regular noon turn
                        linearPhi = phiStart - FastMath.copySign(yawRate, beta) * dtStart;
                    }
                    // TODO: there is no protection against overshooting phiEnd as in night turn
                    // there should probably be some protection
                    phi = linearPhi;
                } else {
                    // midnight turn
                    final double dtEnd = dtStart - context.getTurnDuration();
                    if (dtEnd < 0) {
                        // we are within the turn itself
                        phi = phiStart + yawRate * dtStart;
                    } else {
                        // we are in the recovery phase after turn
                        final double phiEnd   = phiStart + yawRate * context.getTurnDuration();
                        final double deltaPhi = context.yawAngle() - phiEnd;
                        if (FastMath.abs(deltaPhi / yawRate) <= dtEnd) {
                            // time since turn end was sufficient for recovery
                            // we are already back in nominal yaw mode
                            return context.getNominalYaw();
                        } else {
                            // recovery is not finished yet
                            phi = phiEnd + FastMath.copySign(yawRate * dtEnd, deltaPhi);
                        }
                    }
                }

                // TODO
                return null;

            }

        }

        // in nominal yaw mode
        return context.getNominalYaw();

    }

}

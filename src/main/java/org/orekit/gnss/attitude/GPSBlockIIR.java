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
public class GPSBlockIIR extends AbstractGNSSAttitudeProvider {

    /** Default yaw rates for all spacecrafts in radians per seconds. */
    public static final double DEFAULT_YAW_RATE = FastMath.toRadians(0.2);

    /** Margin on turn end. */
    private static final double END_MARGIN = 1800.0;

    /** Yaw rate. */
    private final double yawRate;

    /** Simple constructor.
     * @param yawRate yaw rate to use in radians per seconds (typically {@link #DEFAULT_YAW_RATE})
     * @param validityStart start of validity for this provider
     * @param validityEnd end of validity for this provider
     * @param sun provider for Sun position
     * @param inertialFrame inertial frame where velocity are computed
     */
    public GPSBlockIIR(final double yawRate,
                       final AbsoluteDate validityStart, final AbsoluteDate validityEnd,
                       final ExtendedPVCoordinatesProvider sun, final Frame inertialFrame) {
        super(validityStart, validityEnd, sun, inertialFrame);
        this.yawRate = yawRate;
    }

    /** {@inheritDoc} */
    @Override
    protected TimeStampedAngularCoordinates correctedYaw(final GNSSAttitudeContext context) {

        // noon beta angle limit from yaw rate
        final double aNoon  = FastMath.atan(context.getMuRate() / yawRate);
        final double cNoon  = FastMath.cos(aNoon);
        final double cNight = -cNoon;

        if (context.setUpTurnRegion(cNight, cNoon)) {

            final double absBeta = FastMath.abs(context.beta(context.getDate()));
            context.setHalfSpan(absBeta * FastMath.sqrt(aNoon / absBeta - 1.0), END_MARGIN);
            if (context.inTurnTimeRange()) {

                // we need to ensure beta sign does not change during the turn
                final double beta     = context.getSecuredBeta();
                final double phiStart = context.getYawStart(beta);
                final double dtStart  = context.timeSinceTurnStart();
                final double phiDot;
                final double linearPhi;

                if (context.inSunSide()) {
                    // noon turn
                    phiDot    = -FastMath.copySign(yawRate, beta);
                    linearPhi = phiStart + phiDot * dtStart;
                } else {
                    // midnight turn
                    phiDot    = FastMath.copySign(yawRate, beta);
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
        final double cNoon  = FastMath.cos(aNoon.getReal());
        final double cNight = -cNoon;

        if (context.setUpTurnRegion(cNight, cNoon)) {

            final T absBeta = FastMath.abs(context.beta(context.getDate()));
            context.setHalfSpan(absBeta.multiply(FastMath.sqrt(aNoon.divide(absBeta).subtract(1.0))), END_MARGIN);
            if (context.inTurnTimeRange()) {

                // we need to ensure beta sign does not change during the turn
                final T beta     = context.getSecuredBeta();
                final T phiStart = context.getYawStart(beta);
                final T dtStart  = context.timeSinceTurnStart();
                final T phiDot;
                final T linearPhi;

                if (context.inSunSide()) {
                    // noon turn
                    phiDot    = field.getZero().add(-FastMath.copySign(yawRate, beta.getReal()));
                    linearPhi = phiStart.add(phiDot.multiply(dtStart));
                } else {
                    // midnight turn
                    phiDot    = field.getZero().add(FastMath.copySign(yawRate, beta.getReal()));
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

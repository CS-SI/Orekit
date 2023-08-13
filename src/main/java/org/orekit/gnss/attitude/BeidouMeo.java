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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;

/**
 * Attitude providers for Beidou Medium Earth Orbit navigation satellites.
 * @author Luc Maisonobe Java translation
 * @since 9.2
 */
public class BeidouMeo extends AbstractGNSSAttitudeProvider {

    /** Limit for the Yaw Steering to Orbit Normal switch. */
    private static final double BETA_YS_ON = FastMath.toRadians(4.1);

    /** Limit for the Orbit Normal to Yaw Steering switch. */
    private static final double BETA_ON_YS = FastMath.toRadians(3.9);

    /** Simple constructor.
     * @param validityStart start of validity for this provider
     * @param validityEnd end of validity for this provider
     * @param sun provider for Sun position
     * @param inertialFrame inertial frame where velocity are computed
     */
    public BeidouMeo(final AbsoluteDate validityStart, final AbsoluteDate validityEnd,
                     final ExtendedPVCoordinatesProvider sun, final Frame inertialFrame) {
        super(validityStart, validityEnd, sun, inertialFrame);
    }

    /** {@inheritDoc} */
    @Override
    protected TimeStampedAngularCoordinates correctedYaw(final GNSSAttitudeContext context) {

        // variation of the β angle over one orbital period (approximately)
        final double beta          = context.beta(context.getDate());
        final double approxPeriod  = 2 * FastMath.PI / context.getMuRate();
        final double betaVariation = beta - context.beta(context.getDate().shiftedBy(-approxPeriod));
        final double delta         = context.getOrbitAngleSinceMidnight();

        if (FastMath.abs(beta) <= BETA_YS_ON - FastMath.abs(betaVariation)) {
            // the β angle is lower than threshold for a complete orbital period
            // we are for sure in the Orbit Normal (ON) mode
            return context.orbitNormalYaw();
        } else if (FastMath.abs(beta) > BETA_ON_YS + FastMath.abs(betaVariation)) {
            // the β angle is higher than threshold for a complete orbital period,
            // we are for sure in the Yaw Steering mode
            return context.nominalYaw(context.getDate());
        } else {
            // we are in the grey zone, somewhere near a mode switch
            final boolean absBetaDecreasing = beta * betaVariation <= 0.0;

            if (absBetaDecreasing) {
                // we are going towards the β = 0 limit
                if (FastMath.abs(beta) >= BETA_YS_ON) {
                    // we have not yet reached the far limit, we are still in Yaw Steering
                    return context.nominalYaw(context.getDate());
                }
            } else {
                // we are going away from the β = 0 limit
                if (FastMath.abs(beta) <= BETA_ON_YS) {
                    // we have not yet reached the close limit, we are still in Orbit Normal
                    return context.orbitNormalYaw();
                }
            }

            // there is a mode switch near the current orbit, it occurs when orbit angle is 90°
            // we check what was the β angle at the previous quadrature to see if the switch
            // already occurred
            final double angleSinceQuadrature =
                            MathUtils.normalizeAngle(delta - 0.5 * FastMath.PI, FastMath.PI);
            final double timeSinceQuadrature = angleSinceQuadrature / context.getMuRate();
            final AbsoluteDate quadratureDate = context.getDate().shiftedBy(-timeSinceQuadrature);
            final double betaQuadrature = context.beta(quadratureDate);

            if (absBetaDecreasing) {
                // we are going towards the β = 0 limit
                if (FastMath.abs(betaQuadrature) <= BETA_YS_ON) {
                    // we have switched to Orbit Normal mode since last quadrature
                    return context.orbitNormalYaw();
                }
            } else {
                // we are going away from the β = 0 limit
                if (FastMath.abs(betaQuadrature) <= BETA_ON_YS) {
                    // β was below switch at last quadrature, we are still in the Orbit Normal mode
                    return context.orbitNormalYaw();
                }
            }

            return context.nominalYaw(context.getDate());

        }

    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> TimeStampedFieldAngularCoordinates<T> correctedYaw(final GNSSFieldAttitudeContext<T> context) {

        // variation of the β angle over one orbital period (approximately)
        final double beta          = context.beta(context.getDate()).getReal();
        final double approxPeriod  = 2 * FastMath.PI / context.getMuRate().getReal();
        final double betaVariation = beta - context.beta(context.getDate().shiftedBy(-approxPeriod)).getReal();
        final double delta         = context.getOrbitAngleSinceMidnight().getReal();

        if (FastMath.abs(beta) <= BETA_YS_ON - FastMath.abs(betaVariation)) {
            // the β angle is lower than threshold for a complete orbital period
            // we are for sure in the Orbit Normal (ON) mode
            return context.orbitNormalYaw();
        } else if (FastMath.abs(beta) > BETA_ON_YS + FastMath.abs(betaVariation)) {
            // the β angle is higher than threshold for a complete orbital period,
            // we are for sure in the Yaw Steering mode
            return context.nominalYaw(context.getDate());
        } else {
            // we are in the grey zone, somewhere near a mode switch
            final boolean absBetaDecreasing = beta * betaVariation <= 0.0;

            if (absBetaDecreasing) {
                // we are going towards the β = 0 limit
                if (FastMath.abs(beta) >= BETA_YS_ON) {
                    // we have not yet reached the far limit, we are still in Yaw Steering
                    return context.nominalYaw(context.getDate());
                }
            } else {
                // we are going away from the β = 0 limit
                if (FastMath.abs(beta) <= BETA_ON_YS) {
                    // we have not yet reached the close limit, we are still in Orbit Normal
                    return context.orbitNormalYaw();
                }
            }

            // there is a mode switch near the current orbit, it occurs when orbit angle is 90°
            // we check what was the β angle at the previous quadrature to see if the switch
            // already occurred
            final double angleSinceQuadrature =
                            MathUtils.normalizeAngle(delta - 0.5 * FastMath.PI, FastMath.PI);
            final double timeSinceQuadrature = angleSinceQuadrature / context.getMuRate().getReal();
            final FieldAbsoluteDate<T> quadratureDate = context.getDate().shiftedBy(-timeSinceQuadrature);
            final double betaQuadrature = context.beta(quadratureDate).getReal();

            if (absBetaDecreasing) {
                // we are going towards the β = 0 limit
                if (FastMath.abs(betaQuadrature) <= BETA_YS_ON) {
                    // we have switched to Orbit Normal mode since last quadrature
                    return context.orbitNormalYaw();
                }
            } else {
                // we are going away from the β = 0 limit
                if (FastMath.abs(betaQuadrature) <= BETA_ON_YS) {
                    // β was below switch at last quadrature, we are still in the Orbit Normal mode
                    return context.orbitNormalYaw();
                }
            }

            return context.nominalYaw(context.getDate());

        }

    }

}

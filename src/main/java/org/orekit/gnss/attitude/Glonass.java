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

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;

/**
 * Attitude providers for Glonass navigation satellites.
 * <p>
 * This class is based on the May 2017 version of J. Kouba eclips.f
 * subroutine available at <a href="http://acc.igs.org/orbits">IGS Analysis
 * Center Coordinator site</a>. The eclips.f code itself is not used ; its
 * hard-coded data are used and its low level models are used, but the
 * structure of the code and the API have been completely rewritten.
 * </p>
 * <p>
 * WARNING: as of release 9.2, this feature is still considered experimental
 * </p>
 * @author J. Kouba original fortran routine
 * @author Luc Maisonobe Java translation
 * @since 9.2
 */
public class Glonass extends AbstractGNSSAttitudeProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20171114L;

    /** Satellite-Sun angle limit for a midnight turn maneuver. */
    private static final double NIGHT_TURN_LIMIT = FastMath.toRadians(180.0 - 14.20);

    /** Yaw rates for all spacecrafts. */
    private static final double YAW_RATE = FastMath.toRadians(0.250);

    /** Initial yaw end at iterative search start. */
    private static final double YAW_END_ZERO = FastMath.toRadians(75.0);

    /** Simple constructor.
     * @param validityStart start of validity for this provider
     * @param validityEnd end of validity for this provider
     * @param sun provider for Sun position
     * @param inertialFrame inertial frame where velocity are computed
     */
    public Glonass(final AbsoluteDate validityStart, final AbsoluteDate validityEnd,
                   final ExtendedPVCoordinatesProvider sun, final Frame inertialFrame) {
        super(validityStart, validityEnd, sun, inertialFrame);
    }

    /** {@inheritDoc} */
    @Override
    protected TimeStampedAngularCoordinates correctedYaw(final GNSSAttitudeContext context)
        throws OrekitException {

        // noon beta angle limit from yaw rate
        final double realBeta = context.getBeta();
        final double muRate   = context.getMuRate();
        final double aNight   = NIGHT_TURN_LIMIT;
        double       aNoon    = FastMath.atan(muRate / YAW_RATE);
        if (FastMath.abs(realBeta) < aNoon) {
            double       yawEnd = YAW_END_ZERO;
            for (int i = 0; i < 3; ++i) {
                final double delta = muRate * yawEnd / YAW_RATE;
                yawEnd = 0.5 * FastMath.abs(context.computePhi(realBeta,  delta) -
                                            context.computePhi(realBeta, -delta));
            }
            aNoon = muRate * yawEnd / YAW_RATE;
        }

        final double cNoon  = FastMath.cos(aNoon);
        final double cNight = FastMath.cos(aNight);

        if (context.setUpTurnRegion(cNight, cNoon)) {

            context.setHalfSpan(context.inSunSide() ?
                                aNoon :
                                context.inOrbitPlaneAbsoluteAngle(aNight - FastMath.PI));
            if (context.inTurnTimeRange(context.getDate(), 0)) {

                // we need to ensure beta sign does not change during the turn
                final double beta     = context.getSecuredBeta();
                final double phiStart = context.getYawStart(beta);
                final double dtStart  = context.timeSinceTurnStart(context.getDate());

                final double phiDot;
                final double linearPhi;
                final double phiEnd    = context.getYawEnd(beta);
                if (context.inSunSide()) {
                    // noon turn
                    phiDot    = -FastMath.copySign(YAW_RATE, beta);
                    linearPhi = phiStart + phiDot * dtStart;
                } else {
                    // midnight turn
                    phiDot    = FastMath.copySign(YAW_RATE, beta);
                    linearPhi = phiStart + phiDot * dtStart;
                }
                if (phiEnd / linearPhi < 0 || phiEnd / linearPhi > 1) {
                    return context.turnCorrectedAttitude(phiEnd, 0.0);
                } else {
                    return context.turnCorrectedAttitude(linearPhi, phiDot);
                }

            }

        }

        // in nominal yaw mode
        return context.getNominalYaw();

    }

    /** {@inheritDoc} */
    @Override
    protected <T extends RealFieldElement<T>> TimeStampedFieldAngularCoordinates<T> correctedYaw(final GNSSFieldAttitudeContext<T> context)
        throws OrekitException {

        final Field<T> field = context.getDate().getField();

        // noon beta angle limit from yaw rate
        final T realBeta = context.getBeta();
        final T muRate   = context.getMuRate();
        final T aNight   = field.getZero().add(NIGHT_TURN_LIMIT);
        T       aNoon    = FastMath.atan(muRate.divide(YAW_RATE));
        if (FastMath.abs(realBeta).getReal() < aNoon.getReal()) {
            T       yawEnd = field.getZero().add(YAW_END_ZERO);
            for (int i = 0; i < 3; ++i) {
                final T delta = muRate.multiply(yawEnd).divide(YAW_RATE);
                yawEnd = FastMath.abs(context.computePhi(realBeta, delta).
                                      subtract(context.computePhi(realBeta, delta.negate()))).
                         multiply(0.5);
            }
            aNoon = muRate.multiply(yawEnd).divide(YAW_RATE);
        }

        final double cNoon  = FastMath.cos(aNoon.getReal());
        final double cNight = FastMath.cos(aNight.getReal());

        if (context.setUpTurnRegion(cNight, cNoon)) {

            context.setHalfSpan(context.inSunSide() ?
                                aNoon :
                                context.inOrbitPlaneAbsoluteAngle(aNight.subtract(FastMath.PI)));
            if (context.inTurnTimeRange(context.getDate(), 0)) {

                // we need to ensure beta sign does not change during the turn
                final T beta     = context.getSecuredBeta();
                final T phiStart = context.getYawStart(beta);
                final T dtStart  = context.timeSinceTurnStart(context.getDate());

                final T phiDot;
                final T linearPhi;
                final T phiEnd    = context.getYawEnd(beta);
                if (context.inSunSide()) {
                    // noon turn
                    phiDot    = field.getZero().add(-FastMath.copySign(YAW_RATE, beta.getReal()));
                    linearPhi = phiStart.add(phiDot.multiply(dtStart));
                } else {
                    // midnight turn
                    phiDot    = field.getZero().add(FastMath.copySign(YAW_RATE, beta.getReal()));
                    linearPhi = phiStart.add(phiDot.multiply(dtStart));
                }
                if (phiEnd.getReal() / linearPhi.getReal() < 0 || phiEnd.getReal() / linearPhi.getReal() > 1) {
                    return context.turnCorrectedAttitude(phiEnd, field.getZero());
                } else {
                    return context.turnCorrectedAttitude(linearPhi, phiDot);
                }

            }

        }

        // in nominal yaw mode
        return context.getNominalYaw();

    }

}

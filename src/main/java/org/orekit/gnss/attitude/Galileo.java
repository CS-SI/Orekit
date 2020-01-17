/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FieldDerivativeStructure;
import org.hipparchus.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;

/**
 * Attitude providers for Galileo navigation satellites.
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
public class Galileo extends AbstractGNSSAttitudeProvider {

    /** Default yaw rates for all spacecrafts in radians per seconds. */
    public static final double DEFAULT_YAW_RATE = FastMath.toRadians(0.203);

    /** Constants for Galileo turns. */
    private static final double BETA_X = FastMath.toRadians(15.0);

    /** Limit for the noon turn. */
    private static final double COS_NOON = FastMath.cos(BETA_X);

    /** Limit for the night turn. */
    private static final double COS_NIGHT = -COS_NOON;

    /** No margin on turn end for Galileo. */
    private static final double END_MARGIN = 0.0;

    /** Yaw rate. */
    private final double yawRate;

    /** Simple constructor.
     * @param yawRate yaw rate to use in radians per seconds (typically {@link #DEFAULT_YAW_RATE})
     * @param validityStart start of validity for this provider
     * @param validityEnd end of validity for this provider
     * @param sun provider for Sun position
     * @param inertialFrame inertial frame where velocity are computed
     */
    public Galileo(final double yawRate,
                   final AbsoluteDate validityStart, final AbsoluteDate validityEnd,
                   final ExtendedPVCoordinatesProvider sun, final Frame inertialFrame) {
        super(validityStart, validityEnd, sun, inertialFrame);
        this.yawRate = yawRate;
    }

    /** {@inheritDoc} */
    @Override
    protected TimeStampedAngularCoordinates correctedYaw(final GNSSAttitudeContext context) {

        // noon beta angle limit from yaw rate
        final double beta0 = FastMath.atan(context.getMuRate() / yawRate);

        if (FastMath.abs(context.beta(context.getDate())) < beta0 &&
            context.setUpTurnRegion(COS_NIGHT, COS_NOON)) {

            context.setHalfSpan(context.inSunSide() ?
                                BETA_X :
                                context.inOrbitPlaneAbsoluteAngle(BETA_X),
                                END_MARGIN);
            if (context.inTurnTimeRange()) {

                // handling both noon and midnight turns at once
                final DerivativeStructure beta     = context.betaDS();
                final DerivativeStructure cosBeta  = beta.cos();
                final DerivativeStructure sinBeta  = beta.sin();
                final double              sinY     = FastMath.copySign(FastMath.sin(beta0), context.getSecuredBeta());
                final DerivativeStructure sd       = FastMath.sin(context.getDeltaDS()).
                                                     multiply(FastMath.copySign(1.0, -context.getSVBcos() * context.getDeltaDS().getPartialDerivative(1)));
                final DerivativeStructure c        = sd.multiply(cosBeta);
                final DerivativeStructure shy      = sinBeta.negate().subtract(sinY).
                                                     add(sinBeta.subtract(sinY).multiply(c.abs().multiply(FastMath.PI / FastMath.sin(BETA_X)).cos())).
                                                     multiply(0.5);
                final DerivativeStructure phi      = FastMath.atan2(shy, c);

                return context.turnCorrectedAttitude(phi);

            }

        }

        // in nominal yaw mode
        return context.nominalYaw(context.getDate());

    }

    /** {@inheritDoc} */
    @Override
    protected <T extends RealFieldElement<T>> TimeStampedFieldAngularCoordinates<T> correctedYaw(final GNSSFieldAttitudeContext<T> context) {

        // noon beta angle limit from yaw rate
        final double beta0 = FastMath.atan(context.getMuRate().getReal() / yawRate);

        if (FastMath.abs(context.beta(context.getDate())).getReal() < beta0 &&
            context.setUpTurnRegion(COS_NIGHT, COS_NOON)) {

            final Field<T> field = context.getDate().getField();
            final T        betaX = field.getZero().add(BETA_X);
            context.setHalfSpan(context.inSunSide() ?
                                betaX :
                                context.inOrbitPlaneAbsoluteAngle(betaX),
                                END_MARGIN);
            if (context.inTurnTimeRange()) {

                // handling both noon and midnight turns at once
                final FieldDerivativeStructure<T> beta     = context.betaDS();
                final FieldDerivativeStructure<T> cosBeta  = beta.cos();
                final FieldDerivativeStructure<T> sinBeta  = beta.sin();
                final T                           sinY     = FastMath.sin(field.getZero().add(beta0)).copySign(context.getSecuredBeta());
                final FieldDerivativeStructure<T> sd       = FastMath.sin(context.getDeltaDS()).
                                                             multiply(FastMath.copySign(1.0, -context.getSVBcos().getReal() * context.getDeltaDS().getPartialDerivative(1).getReal()));
                final FieldDerivativeStructure<T> c        = sd.multiply(cosBeta);
                final FieldDerivativeStructure<T> shy      = sinBeta.negate().subtract(sinY).
                                                             add(sinBeta.subtract(sinY).multiply(c.abs().multiply(FastMath.PI / FastMath.sin(BETA_X)).cos())).
                                                             multiply(0.5);
                final FieldDerivativeStructure<T> phi      = FastMath.atan2(shy, c);

                return context.turnCorrectedAttitude(phi);

            }

        }

        // in nominal yaw mode
        return context.nominalYaw(context.getDate());

    }

}

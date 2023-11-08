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
import org.hipparchus.analysis.CalculusFieldUnivariateFunction;
import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.solvers.AllowedSolution;
import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.analysis.solvers.FieldBracketingNthOrderBrentSolver;
import org.hipparchus.analysis.solvers.UnivariateSolverUtils;
import org.hipparchus.util.FastMath;
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
 * @author J. Kouba original fortran routine
 * @author Luc Maisonobe Java translation
 * @since 9.2
 */
public class Glonass extends AbstractGNSSAttitudeProvider {

    /** Default yaw rates for all spacecrafts in radians per seconds. */
    public static final double DEFAULT_YAW_RATE = FastMath.toRadians(0.250);

    /** Satellite-Sun angle limit for a midnight turn maneuver. */
    private static final double NIGHT_TURN_LIMIT = FastMath.toRadians(180.0 - 14.20);

    /** Initial yaw end at iterative search start. */
    private static final double YAW_END_ZERO = FastMath.toRadians(75.0);

    /** No margin on turn end for Glonass. */
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
    public Glonass(final double yawRate,
                   final AbsoluteDate validityStart, final AbsoluteDate validityEnd,
                   final ExtendedPVCoordinatesProvider sun, final Frame inertialFrame) {
        super(validityStart, validityEnd, sun, inertialFrame);
        this.yawRate = yawRate;
    }

    /** {@inheritDoc} */
    @Override
    protected TimeStampedAngularCoordinates correctedYaw(final GNSSAttitudeContext context) {

        // noon beta angle limit from yaw rate
        final double realBeta = context.beta(context.getDate());
        final double muRate   = context.getMuRate();
        final double aNight   = NIGHT_TURN_LIMIT;
        double       aNoon    = FastMath.atan(muRate / yawRate);
        if (FastMath.abs(realBeta) < aNoon) {
            final UnivariateFunction f = yawEnd -> {
                final double delta =  muRate * yawEnd / yawRate;
                return yawEnd - 0.5 * FastMath.abs(context.computePhi(realBeta,  delta) -
                                                   context.computePhi(realBeta, -delta));
            };
            final double[] bracket = UnivariateSolverUtils.bracket(f, YAW_END_ZERO, 0.0, FastMath.PI);
            final double yawEnd = new BracketingNthOrderBrentSolver(1.0e-14, 1.0e-8, 1.0e-15, 5).
                                  solve(50, f, bracket[0], bracket[1], AllowedSolution.ANY_SIDE);
            aNoon = muRate * yawEnd / yawRate;
        }

        final double cNoon  = FastMath.cos(aNoon);
        final double cNight = FastMath.cos(aNight);

        if (context.setUpTurnRegion(cNight, cNoon)) {

            context.setHalfSpan(context.inSunSide() ?
                                aNoon :
                                context.inOrbitPlaneAbsoluteAngle(aNight - FastMath.PI),
                                END_MARGIN);
            if (context.inTurnTimeRange()) {

                // we need to ensure beta sign does not change during the turn
                final double beta     = context.getSecuredBeta();
                final double phiStart = context.getYawStart(beta);
                final double dtStart  = context.timeSinceTurnStart();

                final double phiDot;
                final double linearPhi;
                final double phiEnd    = context.getYawEnd(beta);
                if (context.inSunSide()) {
                    // noon turn
                    phiDot    = -FastMath.copySign(yawRate, beta);
                    linearPhi = phiStart + phiDot * dtStart;
                } else {
                    // midnight turn
                    phiDot    = FastMath.copySign(yawRate, beta);
                    linearPhi = phiStart + phiDot * dtStart;

                    if (phiEnd / linearPhi < 0 || phiEnd / linearPhi > 1) {
                        // this turn limitation is only computed for midnight turns in Kouba model
                        // we don't understand yet why it doesn't apply to noon turns
                        return context.turnCorrectedAttitude(phiEnd, 0.0);
                    }

                }

                return context.turnCorrectedAttitude(linearPhi, phiDot);

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
        final T realBeta = context.beta(context.getDate());
        final T muRate   = context.getMuRate();
        final T aNight   = field.getZero().add(NIGHT_TURN_LIMIT);
        T       aNoon    = FastMath.atan(muRate.divide(yawRate));
        if (FastMath.abs(realBeta).getReal() < aNoon.getReal()) {
            final CalculusFieldUnivariateFunction<T> f = yawEnd -> {
                final T delta = muRate.multiply(yawEnd).divide(yawRate);
                return yawEnd.subtract(FastMath.abs(context.computePhi(realBeta, delta).
                                                    subtract(context.computePhi(realBeta, delta.negate()))).
                                       multiply(0.5));
            };
            final T[] bracket = UnivariateSolverUtils.bracket(f, field.getZero().add(YAW_END_ZERO),
                                                              field.getZero(), field.getZero().getPi());
            final T yawEnd = new FieldBracketingNthOrderBrentSolver<>(field.getZero().add(1.0e-14),
                                                                      field.getZero().add(1.0e-8),
                                                                      field.getZero().add(1.0e-15),
                                                                      5).
                            solve(50, f, bracket[0], bracket[1], AllowedSolution.ANY_SIDE);
            aNoon = muRate.multiply(yawEnd).divide(yawRate);
        }

        final double cNoon  = FastMath.cos(aNoon.getReal());
        final double cNight = FastMath.cos(aNight.getReal());

        if (context.setUpTurnRegion(cNight, cNoon)) {

            context.setHalfSpan(context.inSunSide() ?
                                aNoon :
                                context.inOrbitPlaneAbsoluteAngle(aNight.subtract(aNight.getPi())),
                                END_MARGIN);
            if (context.inTurnTimeRange()) {

                // we need to ensure beta sign does not change during the turn
                final T beta     = context.getSecuredBeta();
                final T phiStart = context.getYawStart(beta);
                final T dtStart  = context.timeSinceTurnStart();

                final T phiDot;
                final T linearPhi;
                final T phiEnd    = context.getYawEnd(beta);
                if (context.inSunSide()) {
                    // noon turn
                    phiDot    = field.getZero().add(-FastMath.copySign(yawRate, beta.getReal()));
                    linearPhi = phiStart.add(phiDot.multiply(dtStart));
                } else {
                    // midnight turn
                    phiDot    = field.getZero().add(FastMath.copySign(yawRate, beta.getReal()));
                    linearPhi = phiStart.add(phiDot.multiply(dtStart));

                    // this turn limitation is only computed for midnight turns in Kouba model
                    // we don't understand yet why it doesn't apply to noon turns
                    if (phiEnd.getReal() / linearPhi.getReal() < 0 || phiEnd.getReal() / linearPhi.getReal() > 1) {
                        return context.turnCorrectedAttitude(phiEnd, field.getZero());
                    }

                }

                return context.turnCorrectedAttitude(linearPhi, phiDot);


            }

        }

        // in nominal yaw mode
        return context.nominalYaw(context.getDate());

    }

}

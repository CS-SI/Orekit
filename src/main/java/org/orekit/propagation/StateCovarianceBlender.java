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
package org.orekit.propagation;

import org.hipparchus.analysis.polynomials.SmoothStepFactory;
import org.hipparchus.linear.RealMatrix;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterpolator;
import org.orekit.time.TimeStampedPair;

import java.util.List;

/**
 * State covariance blender.
 * <p>
 * Its purpose is to interpolate state covariance between tabulated state covariances by using the concept of blending,
 * exposed in : "Efficient Covariance Interpolation using Blending of Approximate State Error Transitions" by Sergei
 * Tanygin.
 * <p>
 * It propagates tabulated values to the interpolation date assuming a standard keplerian model and then blend each
 * propagated covariances using a smoothstep function.
 * <p>
 * It gives accurate results as explained <a
 * href="https://orekit.org/doc/technical-notes/Implementation_of_covariance_interpolation_in_Orekit.pdf">here</a>. In the
 * very poorly tracked test case evolving in a highly dynamical environment mentioned in the linked thread, the user can
 * expect at worst errors of less than 0.25% in position sigmas and less than 0.4% in velocity sigmas with steps of 40mn
 * between tabulated values.
 *
 * @author Vincent Cucchietti
 * @see org.hipparchus.analysis.polynomials.SmoothStepFactory
 * @see org.hipparchus.analysis.polynomials.SmoothStepFactory.SmoothStepFunction
 */
public class StateCovarianceBlender extends AbstractStateCovarianceInterpolator {

    /** Blending function. */
    private final SmoothStepFactory.SmoothStepFunction blendingFunction;

    /**
     * Constructor.
     * <p>
     * <b>BEWARE:</b> If the output local orbital frame is not considered pseudo-inertial, all the covariance components
     * related to the velocity will be poorly interpolated. <b>Only the position covariance should be considered in this
     * case.</b>
     *
     * @param blendingFunction blending function
     * @param orbitInterpolator orbit interpolator
     * @param outLOF local orbital frame
     *
     * @see Frame
     * @see OrbitType
     * @see PositionAngleType
     */
    public StateCovarianceBlender(final SmoothStepFactory.SmoothStepFunction blendingFunction,
                                  final TimeInterpolator<Orbit> orbitInterpolator,
                                  final LOFType outLOF) {
        super(DEFAULT_INTERPOLATION_POINTS, 0., orbitInterpolator, outLOF);
        this.blendingFunction = blendingFunction;
    }

    /**
     * Constructor.
     *
     * @param blendingFunction blending function
     * @param orbitInterpolator orbit interpolator
     * @param outFrame desired output covariance frame
     * @param outPositionAngleType desired output position angle
     * @param outOrbitType desired output orbit type
     *
     * @see Frame
     * @see OrbitType
     * @see PositionAngleType
     */
    public StateCovarianceBlender(final SmoothStepFactory.SmoothStepFunction blendingFunction,
                                  final TimeInterpolator<Orbit> orbitInterpolator,
                                  final Frame outFrame,
                                  final OrbitType outOrbitType,
                                  final PositionAngleType outPositionAngleType) {
        super(DEFAULT_INTERPOLATION_POINTS, 0., orbitInterpolator, outFrame, outOrbitType, outPositionAngleType);
        this.blendingFunction = blendingFunction;
    }

    /** {@inheritDoc} */
    @Override
    protected StateCovariance computeInterpolatedCovarianceInOrbitFrame(
            final List<TimeStampedPair<Orbit, StateCovariance>> uncertainStates,
            final Orbit interpolatedOrbit) {

        // Necessarily only two sample for blending
        final TimeStampedPair<Orbit, StateCovariance> previousUncertainState = uncertainStates.get(0);
        final TimeStampedPair<Orbit, StateCovariance> nextUncertainState     = uncertainStates.get(1);

        // Get the interpolation date
        final AbsoluteDate interpolationDate = interpolatedOrbit.getDate();

        // Propagate previous tabulated covariance to interpolation date
        final StateCovariance forwardedCovariance =
                propagateCovarianceAnalytically(interpolationDate, interpolatedOrbit, previousUncertainState);

        // Propagate next tabulated covariance to interpolation date
        final StateCovariance backwardedCovariance =
                propagateCovarianceAnalytically(interpolationDate, interpolatedOrbit, nextUncertainState);

        // Compute normalized time parameter
        final double timeParameter =
                getTimeParameter(interpolationDate, previousUncertainState.getDate(), nextUncertainState.getDate());

        // Blend the covariance matrices
        final double     blendingValue              = blendingFunction.value(timeParameter);
        final RealMatrix forwardedCovarianceMatrix  = forwardedCovariance.getMatrix();
        final RealMatrix backwardedCovarianceMatrix = backwardedCovariance.getMatrix();

        final RealMatrix blendedCovarianceMatrix =
                forwardedCovarianceMatrix.blendArithmeticallyWith(backwardedCovarianceMatrix, blendingValue);

        return new StateCovariance(blendedCovarianceMatrix, interpolationDate, interpolatedOrbit.getFrame(),
                                   OrbitType.CARTESIAN, DEFAULT_POSITION_ANGLE);
    }

    /**
     * Propagate given state covariance to the interpolation date using keplerian motion.
     *
     * @param interpolationTime interpolation date at which we desire to interpolate the state covariance
     * @param orbitAtInterpolatingTime orbit at interpolation date
     * @param tabulatedUncertainState tabulated uncertain state
     *
     * @return state covariance at given interpolation date.
     *
     * @see StateCovariance
     */
    private StateCovariance propagateCovarianceAnalytically(final AbsoluteDate interpolationTime,
                                                            final Orbit orbitAtInterpolatingTime,
                                                            final TimeStampedPair<Orbit, StateCovariance> tabulatedUncertainState) {

        // Get orbit and covariance
        final Orbit           tabulatedOrbit         = tabulatedUncertainState.getFirst();
        final StateCovariance tabulatedCovariance    = tabulatedUncertainState.getSecond();
        final Frame           interpolatedOrbitFrame = orbitAtInterpolatingTime.getFrame();

        // Express tabulated covariance in interpolated orbit frame for consistency
        final StateCovariance tabulatedCovarianceInOrbitFrame =
                tabulatedCovariance.changeCovarianceFrame(tabulatedOrbit, interpolatedOrbitFrame);

        // First convert the covariance matrix to equinoctial elements to avoid singularities inherent to keplerian elements
        final RealMatrix covarianceMatrixInEquinoctial =
                tabulatedCovarianceInOrbitFrame.changeCovarianceType(tabulatedOrbit, OrbitType.EQUINOCTIAL,
                                                                     DEFAULT_POSITION_ANGLE).getMatrix();

        // Compute state error transition matrix in equinoctial elements (identical to the one in keplerian elements)
        final RealMatrix stateErrorTransitionMatrixInEquinoctial =
                StateCovariance.getStm(tabulatedOrbit, interpolationTime.durationFrom(tabulatedOrbit.getDate()));

        // Propagate the covariance matrix using the previously computed state error transition matrix
        final RealMatrix propagatedCovarianceMatrixInEquinoctial =
                stateErrorTransitionMatrixInEquinoctial.multiply(
                        covarianceMatrixInEquinoctial.multiplyTransposed(stateErrorTransitionMatrixInEquinoctial));

        // Recreate a StateCovariance instance
        final StateCovariance propagatedCovarianceInEquinoctial =
                new StateCovariance(propagatedCovarianceMatrixInEquinoctial, interpolationTime,
                                    interpolatedOrbitFrame, OrbitType.EQUINOCTIAL, DEFAULT_POSITION_ANGLE);

        // Output propagated state covariance after converting back to cartesian elements
        return propagatedCovarianceInEquinoctial.changeCovarianceType(orbitAtInterpolatingTime,
                                                                      OrbitType.CARTESIAN, DEFAULT_POSITION_ANGLE);
    }
}

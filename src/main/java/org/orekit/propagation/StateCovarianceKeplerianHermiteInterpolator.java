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

import org.hipparchus.analysis.interpolation.HermiteInterpolator;
import org.hipparchus.linear.BlockRealMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.errors.OrekitInternalError;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterpolator;
import org.orekit.time.TimeStampedPair;
import org.orekit.utils.CartesianDerivativesFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * State covariance Keplerian quintic interpolator.
 * <p>
 * Its purpose is to interpolate state covariance between tabulated state covariances using polynomial interpolation. To do
 * so, it uses a {@link HermiteInterpolator} and compute the first and second order derivatives at tabulated states assuming
 * a standard Keplerian motion depending on given derivatives filter.
 * <p>
 * It gives very accurate results as explained <a
 * href="https://orekit.org/doc/technical-notes/Implementation_of_covariance_interpolation_in_Orekit.pdf">here</a>. In the
 * very poorly tracked test case evolving in a highly dynamical environment mentioned in the linked thread, the user can
 * expect at worst errors of less than 0.2% in position sigmas and less than 0.35% in velocity sigmas with steps of 40mn
 * between tabulated values.
 * <p>
 * However, note that this method does not guarantee the positive definiteness of the computed state covariance as opposed to
 * {@link StateCovarianceBlender}.
 *
 * @author Vincent Cucchietti
 * @see HermiteInterpolator
 * @see StateCovarianceBlender
 */
public class StateCovarianceKeplerianHermiteInterpolator extends AbstractStateCovarianceInterpolator {

    /**
     * Filter defining if only the state covariance value are used or if first or/and second Keplerian derivatives should be
     * used.
     */
    private final CartesianDerivativesFilter filter;

    /**
     * Constructor using an output local orbital frame and :
     * <ul>
     *     <li>Default number of interpolation points of {@code DEFAULT_INTERPOLATION_POINTS}</li>
     *     <li>Default extrapolation threshold value ({@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s)</li>
     *     <li>Use of position and two time derivatives during interpolation</li>
     * </ul>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     * <p>
     * <b>BEWARE:</b> If the output local orbital frame is not considered pseudo-inertial, all the covariance components
     * related to the velocity will be poorly interpolated. <b>Only the position covariance should be considered in this
     * case.</b>
     *
     * @param orbitInterpolator orbit interpolator
     * @param outLOF output local orbital frame
     */
    public StateCovarianceKeplerianHermiteInterpolator(final TimeInterpolator<Orbit> orbitInterpolator,
                                                       final LOFType outLOF) {
        this(DEFAULT_INTERPOLATION_POINTS, orbitInterpolator, outLOF);
    }

    /**
     * Constructor using an output local orbital frame and :
     * <ul>
     *     <li>Default extrapolation threshold value ({@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s)</li>
     *     <li>Use of position and two time derivatives during interpolation</li>
     * </ul>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     * <p>
     * <b>BEWARE:</b> If the output local orbital frame is not considered pseudo-inertial, all the covariance components
     * related to the velocity will be poorly interpolated. <b>Only the position covariance should be considered in this
     * case.</b>
     *
     * @param interpolationPoints number of interpolation points
     * @param orbitInterpolator orbit interpolator
     * @param outLOF output local orbital frame
     */
    public StateCovarianceKeplerianHermiteInterpolator(final int interpolationPoints,
                                                       final TimeInterpolator<Orbit> orbitInterpolator,
                                                       final LOFType outLOF) {
        this(interpolationPoints, orbitInterpolator, CartesianDerivativesFilter.USE_PVA,
             outLOF);
    }

    /**
     * Constructor using an output local orbital frame and :
     * <ul>
     *     <li>Use of position and two time derivatives during interpolation</li>
     * </ul>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     * <p>
     * <b>BEWARE:</b> If the output local orbital frame is not considered pseudo-inertial, all the covariance components
     * related to the velocity will be poorly interpolated. <b>Only the position covariance should be considered in this
     * case.</b>
     *
     * @param interpolationPoints number of interpolation points
     * @param orbitInterpolator orbit interpolator
     * @param outLOF output local orbital frame
     * @param filter filter for derivatives from the sample to use in position-velocity-acceleration interpolation
     */
    public StateCovarianceKeplerianHermiteInterpolator(final int interpolationPoints,
                                                       final TimeInterpolator<Orbit> orbitInterpolator,
                                                       final CartesianDerivativesFilter filter,
                                                       final LOFType outLOF) {
        this(interpolationPoints, DEFAULT_EXTRAPOLATION_THRESHOLD_SEC, orbitInterpolator, filter, outLOF);
    }

    /**
     * Constructor using an output local orbital frame.
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     * <p>
     * <b>BEWARE:</b> If the output local orbital frame is not considered pseudo-inertial, all the covariance components
     * related to the velocity will be poorly interpolated. <b>Only the position covariance should be considered in this
     * case.</b>
     *
     * @param interpolationPoints number of interpolation points
     * @param extrapolationThreshold extrapolation threshold beyond which the propagation will fail
     * @param orbitInterpolator orbit interpolator
     * @param outLOF output local orbital frame
     * @param filter filter defining if only the state covariance value are used or if first or/and second Keplerian
     * derivatives should be used during the interpolation.
     */
    public StateCovarianceKeplerianHermiteInterpolator(final int interpolationPoints, final double extrapolationThreshold,
                                                       final TimeInterpolator<Orbit> orbitInterpolator,
                                                       final CartesianDerivativesFilter filter, final LOFType outLOF) {
        super(interpolationPoints, extrapolationThreshold, orbitInterpolator, outLOF);
        this.filter = filter;
    }

    /**
     * Constructor using an output frame and :
     * <ul>
     *     <li>Default number of interpolation points of {@code DEFAULT_INTERPOLATION_POINTS}</li>
     *     <li>Default extrapolation threshold value ({@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s)</li>
     *     <li>Use of position and two time derivatives during interpolation</li>
     * </ul>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     *
     * @param orbitInterpolator orbit interpolator
     * @param outFrame output frame
     * @param outOrbitType output orbit type
     * @param outPositionAngleType output position angle
     */
    public StateCovarianceKeplerianHermiteInterpolator(final TimeInterpolator<Orbit> orbitInterpolator, final Frame outFrame,
                                                       final OrbitType outOrbitType, final PositionAngleType outPositionAngleType) {
        this(DEFAULT_INTERPOLATION_POINTS, orbitInterpolator, outFrame, outOrbitType, outPositionAngleType);
    }

    /**
     * Constructor using an output frame and :
     * <ul>
     *     <li>Default number of interpolation points of {@code DEFAULT_INTERPOLATION_POINTS}</li>
     *     <li>Use of position and two time derivatives during interpolation</li>
     * </ul>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     *
     * @param interpolationPoints number of interpolation points
     * @param orbitInterpolator orbit interpolator
     * @param outFrame output frame
     * @param outOrbitType output orbit type
     * @param outPositionAngleType output position angle
     */
    public StateCovarianceKeplerianHermiteInterpolator(final int interpolationPoints,
                                                       final TimeInterpolator<Orbit> orbitInterpolator, final Frame outFrame,
                                                       final OrbitType outOrbitType, final PositionAngleType outPositionAngleType) {
        this(interpolationPoints, DEFAULT_EXTRAPOLATION_THRESHOLD_SEC, orbitInterpolator, CartesianDerivativesFilter.USE_PVA,
             outFrame, outOrbitType, outPositionAngleType);
    }

    /**
     * Constructor using an output frame and :
     * <ul>
     *     <li>Default extrapolation threshold value ({@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s)</li>
     * </ul>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     *
     * @param interpolationPoints number of interpolation points
     * @param orbitInterpolator orbit interpolator
     * @param filter filter defining if only the state covariance value are used or if first or/and second Keplerian
     * derivatives should be used during the interpolation.
     * @param outFrame output frame
     * @param outOrbitType output orbit type
     * @param outPositionAngleType output position angle
     */
    public StateCovarianceKeplerianHermiteInterpolator(final int interpolationPoints,
                                                       final TimeInterpolator<Orbit> orbitInterpolator,
                                                       final CartesianDerivativesFilter filter, final Frame outFrame,
                                                       final OrbitType outOrbitType, final PositionAngleType outPositionAngleType) {
        this(interpolationPoints, DEFAULT_EXTRAPOLATION_THRESHOLD_SEC, orbitInterpolator, filter, outFrame, outOrbitType,
                outPositionAngleType);
    }

    /**
     * Constructor using an output frame.
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     *
     * @param interpolationPoints number of interpolation points
     * @param extrapolationThreshold extrapolation threshold beyond which the propagation will fail
     * @param orbitInterpolator orbit interpolator
     * @param filter filter defining if only the state covariance value are used or if first or/and second Keplerian
     * derivatives should be used during the interpolation.
     * @param outFrame output frame
     * @param outOrbitType output orbit type
     * @param outPositionAngleType output position angle
     */
    public StateCovarianceKeplerianHermiteInterpolator(final int interpolationPoints, final double extrapolationThreshold,
                                                       final TimeInterpolator<Orbit> orbitInterpolator,
                                                       final CartesianDerivativesFilter filter, final Frame outFrame,
                                                       final OrbitType outOrbitType, final PositionAngleType outPositionAngleType) {
        super(interpolationPoints, extrapolationThreshold, orbitInterpolator, outFrame, outOrbitType, outPositionAngleType);
        this.filter = filter;
    }

    /** Get Filter defining if only the state covariance value are used or if first or/and second Keplerian derivatives
     * should be used.
     * @return Filter defining if only the state covariance value are used or if first or/and second Keplerian derivatives
     * should be used.
     */
    public CartesianDerivativesFilter getFilter() {
        return filter;
    }

    /** {@inheritDoc} */
    @Override
    protected StateCovariance computeInterpolatedCovarianceInOrbitFrame(
            final List<TimeStampedPair<Orbit, StateCovariance>> uncertainStates,
            final Orbit interpolatedOrbit) {

        // Compute and combine covariances value and time derivatives
        final List<double[][][]> covarianceValueAndDerivativesList = new ArrayList<>();
        for (TimeStampedPair<Orbit, StateCovariance> uncertainState : uncertainStates) {
            final double[][][] currentCovarianceValueAndDerivatives =
                    computeAndCombineCovarianceValueAndDerivatives(uncertainState, interpolatedOrbit);

            covarianceValueAndDerivativesList.add(currentCovarianceValueAndDerivatives);
        }

        // Interpolate covariance matrix in equinoctial elements
        final RealMatrix interpolatedCovarianceMatrixInEqui =
                computeInterpolatedStateCovariance(interpolatedOrbit.getDate(),
                                                   uncertainStates,
                                                   covarianceValueAndDerivativesList);

        return new StateCovariance(interpolatedCovarianceMatrixInEqui,
                                   interpolatedOrbit.getDate(), interpolatedOrbit.getFrame(),
                                   OrbitType.EQUINOCTIAL, DEFAULT_POSITION_ANGLE);
    }

    /**
     * Compute and combine state covariance matrix and its two Keplerian time derivatives.
     *
     * @param uncertainState orbit and its associated covariance
     * @param interpolatedOrbit interpolated orbit
     *
     * @return state covariance matrix and its two time derivatives
     */
    private double[][][] computeAndCombineCovarianceValueAndDerivatives(
            final TimeStampedPair<Orbit, StateCovariance> uncertainState, final Orbit interpolatedOrbit) {

        // Get orbit and associated covariance
        final Orbit           orbit      = uncertainState.getFirst();
        final StateCovariance covariance = uncertainState.getSecond();

        // Express covariance in interpolated orbit frame for consistency among the sample
        final StateCovariance covarianceInOrbitFrame = covariance.changeCovarianceFrame(orbit, interpolatedOrbit.getFrame());

        // Convert to equinoctial elements to avoid singularities
        final StateCovariance covarianceInOrbitFrameInEqui =
                covarianceInOrbitFrame.changeCovarianceType(orbit, OrbitType.EQUINOCTIAL, DEFAULT_POSITION_ANGLE);

        // Get matrix
        final RealMatrix covarianceInOrbitFrameInEquiMatrix = covarianceInOrbitFrameInEqui.getMatrix();

        // Compute covariance first and second time derivative according to instance filter
        final int dim = StateCovariance.STATE_DIMENSION;

        final RealMatrix covarianceMatrixFirstDerInKep;
        final RealMatrix covarianceMatrixSecondDerInKep;

        switch (filter) {
            case USE_P:
                covarianceMatrixFirstDerInKep = MatrixUtils.createRealMatrix(dim, dim);
                covarianceMatrixSecondDerInKep = MatrixUtils.createRealMatrix(dim, dim);
                break;
            case USE_PV:
                covarianceMatrixFirstDerInKep = computeCovarianceFirstDerivative(orbit, covarianceInOrbitFrameInEquiMatrix);
                covarianceMatrixSecondDerInKep = MatrixUtils.createRealMatrix(dim, dim);
                break;
            case USE_PVA:
                covarianceMatrixFirstDerInKep = computeCovarianceFirstDerivative(orbit, covarianceInOrbitFrameInEquiMatrix);
                covarianceMatrixSecondDerInKep =
                        computeCovarianceSecondDerivative(orbit, covarianceInOrbitFrameInEquiMatrix);
                break;
            default:
                // Should never happen
                throw new OrekitInternalError(null);
        }

        // Combine and output the state covariance and its first two time derivative in a single array
        return combineCovarianceValueAndDerivatives(covarianceInOrbitFrameInEquiMatrix,
                                                    covarianceMatrixFirstDerInKep,
                                                    covarianceMatrixSecondDerInKep);
    }

    /**
     * Compute interpolated state covariance in equinoctial elements using a Hermite interpolator.
     *
     * @param interpolationDate interpolation date
     * @param uncertainStates list of orbits and their associated covariances
     * @param covarianceValueAndDerivativesList list of covariances and their associated first and second time derivatives
     *
     * @return interpolated state covariance in equinoctial elements
     *
     * @see HermiteInterpolator
     */
    private RealMatrix computeInterpolatedStateCovariance(final AbsoluteDate interpolationDate,
                                                          final List<TimeStampedPair<Orbit, StateCovariance>> uncertainStates,
                                                          final List<double[][][]> covarianceValueAndDerivativesList) {

        final RealMatrix interpolatedCovarianceMatrix = new BlockRealMatrix(new double[ROW_DIM][COLUMN_DIM]);

        // Interpolate each element in the covariance matrix
        for (int i = 0; i < ROW_DIM; i++) {
            for (int j = 0; j < COLUMN_DIM; j++) {

                // Create an interpolator for each element
                final HermiteInterpolator tempInterpolator = new HermiteInterpolator();

                // Fill interpolator with all samples value and associated derivatives
                for (int k = 0; k < uncertainStates.size(); k++) {
                    final TimeStampedPair<Orbit, StateCovariance> currentUncertainStates = uncertainStates.get(k);

                    final double[][][] currentCovarianceValueAndDerivatives = covarianceValueAndDerivativesList.get(k);

                    final double deltaT = currentUncertainStates.getDate().durationFrom(interpolationDate);

                    addSampleToInterpolator(tempInterpolator, deltaT, currentCovarianceValueAndDerivatives[i][j]);
                }

                // Interpolate
                interpolatedCovarianceMatrix.setEntry(i, j, tempInterpolator.value(0)[0]);
            }
        }

        return interpolatedCovarianceMatrix;
    }

    /**
     * Add sample to given interpolator.
     *
     * @param interpolator interpolator to add sample to
     * @param deltaT abscissa for interpolation
     * @param valueAndDerivatives value and associated derivatives to add
     */
    private void addSampleToInterpolator(final HermiteInterpolator interpolator, final double deltaT,
                                         final double[] valueAndDerivatives) {
        switch (filter) {
            case USE_P:
                interpolator.addSamplePoint(deltaT, new double[] { valueAndDerivatives[0] });
                break;
            case USE_PV:
                interpolator.addSamplePoint(deltaT,
                                            new double[] { valueAndDerivatives[0] },
                                            new double[] { valueAndDerivatives[1] });
                break;
            case USE_PVA:
                interpolator.addSamplePoint(deltaT,
                                            new double[] { valueAndDerivatives[0] },
                                            new double[] { valueAndDerivatives[1] },
                                            new double[] { valueAndDerivatives[2] });
                break;
            default:
                // Should never happen
                throw new OrekitInternalError(null);
        }
    }

    /**
     * Compute state covariance first Keplerian time derivative.
     *
     * @param orbit orbit
     * @param covarianceMatrixInEqui state covariance matrix in equinoctial elements
     *
     * @return state covariance first time derivative
     */
    private RealMatrix computeCovarianceFirstDerivative(final Orbit orbit,
                                                        final RealMatrix covarianceMatrixInEqui) {

        final RealMatrix covarianceFirstDerivative = new BlockRealMatrix(ROW_DIM, COLUMN_DIM);

        // Compute common term used in papers
        final double m = orbit.getMeanAnomalyDotWrtA();

        // Compute first time derivative of each element in the covariance matrix
        for (int i = 0; i < ROW_DIM; i++) {
            for (int j = 0; j < COLUMN_DIM; j++) {
                if (i != 5 && j != 5) {
                    covarianceFirstDerivative.setEntry(i, j, 0);
                }
                else if (i == 5 && j != 5) {

                    final double value = covarianceMatrixInEqui.getEntry(0, j) * m;

                    covarianceFirstDerivative.setEntry(i, j, value);
                    covarianceFirstDerivative.setEntry(j, i, value);
                }
                else {
                    final double value = 2 * covarianceMatrixInEqui.getEntry(0, 5) * m;

                    covarianceFirstDerivative.setEntry(i, j, value);
                }
            }
        }

        return covarianceFirstDerivative;

    }

    /**
     * Compute state covariance second Keplerian time derivative.
     *
     * @param orbit orbit
     * @param covarianceMatrixInEqui state covariance matrix in equinoctial elements
     *
     * @return state covariance second time derivative
     */
    private RealMatrix computeCovarianceSecondDerivative(final Orbit orbit,
                                                         final RealMatrix covarianceMatrixInEqui) {

        final RealMatrix covarianceSecondDerivative = new BlockRealMatrix(ROW_DIM, COLUMN_DIM);

        // Compute common term used in papers
        final double m = orbit.getMeanAnomalyDotWrtA();

        // Compute second time derivative of each element in the covariance matrix
        for (int i = 0; i < ROW_DIM; i++) {
            for (int j = 0; j < COLUMN_DIM; j++) {
                if (i == 5 && j == 5) {

                    final double value = 2 * covarianceMatrixInEqui.getEntry(0, 0) * m * m;

                    covarianceSecondDerivative.setEntry(i, j, value);
                }
                else {
                    covarianceSecondDerivative.setEntry(i, j, 0);
                }
            }
        }

        return covarianceSecondDerivative;

    }

    /**
     * Combine state covariance matrix and its two Keplerian time derivatives.
     *
     * @param covarianceMatrixInEqui covariance matrix in equinoctial elements
     * @param covarianceMatrixFirstDerInEqui covariance matrix first time derivative in equinoctial elements
     * @param covarianceMatrixSecondDerInEqui covariance matrix second time derivative in equinoctial elements
     *
     * @return state covariance matrix and its two time derivatives
     */
    private double[][][] combineCovarianceValueAndDerivatives(final RealMatrix covarianceMatrixInEqui,
                                                              final RealMatrix covarianceMatrixFirstDerInEqui,
                                                              final RealMatrix covarianceMatrixSecondDerInEqui) {

        final double[][][] covarianceValueAndDerivativesArray = new double[ROW_DIM][COLUMN_DIM][3];

        // Combine covariance and its first two time derivatives in a single 3D array
        for (int i = 0; i < ROW_DIM; i++) {
            for (int j = 0; j < COLUMN_DIM; j++) {
                covarianceValueAndDerivativesArray[i][j][0] = covarianceMatrixInEqui.getEntry(i, j);
                covarianceValueAndDerivativesArray[i][j][1] = covarianceMatrixFirstDerInEqui.getEntry(i, j);
                covarianceValueAndDerivativesArray[i][j][2] = covarianceMatrixSecondDerInEqui.getEntry(i, j);
            }
        }

        return covarianceValueAndDerivativesArray;
    }
}

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

import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.AbstractTimeInterpolator;
import org.orekit.time.TimeInterpolator;
import org.orekit.time.TimeStampedPair;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract class for orbit and state covariance interpolator.
 *
 * @author Vincent Cucchietti
 * @see Orbit
 * @see StateCovariance
 * @see TimeStampedPair
 */
public abstract class AbstractStateCovarianceInterpolator
        extends AbstractTimeInterpolator<TimeStampedPair<Orbit, StateCovariance>> {

    // CHECKSTYLE: stop VisibilityModifier check
    /** Default position angle for covariance expressed in Cartesian elements. */
    protected static final PositionAngle DEFAULT_POSITION_ANGLE = PositionAngle.MEAN;

    /** Default column dimension for position-velocity state covariance. */
    protected static final int COLUMN_DIM = 6;

    /** Default row dimension for position-velocity state covariance. */
    protected static final int ROW_DIM = 6;
    // CHECKSTYLE: resume VisibilityModifier check

    /** Output frame. */
    private final Frame outFrame;

    /** Output local orbital frame. */
    private final LOFType outLOF;

    /** Output orbit type. */
    private final OrbitType outOrbitType;

    /** Output position angle. */
    private final PositionAngle outPositionAngle;

    /** Orbit interpolator. */
    private final TimeInterpolator<Orbit> orbitInterpolator;

    /**
     * Constructor.
     *
     * @param interpolationPoints number of interpolation points
     * @param extrapolationThreshold extrapolation threshold beyond which the propagation will fail
     * @param orbitInterpolator orbit interpolator
     * @param outLOF local orbital frame
     *
     * @see Frame
     * @see OrbitType
     * @see PositionAngle
     */
    public AbstractStateCovarianceInterpolator(final int interpolationPoints, final double extrapolationThreshold,
                                               final TimeInterpolator<Orbit> orbitInterpolator,
                                               final LOFType outLOF) {
        super(interpolationPoints, extrapolationThreshold);
        this.orbitInterpolator = orbitInterpolator;
        this.outLOF            = outLOF;
        this.outFrame          = null;
        this.outOrbitType      = OrbitType.CARTESIAN;
        this.outPositionAngle  = DEFAULT_POSITION_ANGLE;
    }

    /**
     * Constructor.
     *
     * @param interpolationPoints number of interpolation points
     * @param extrapolationThreshold extrapolation threshold beyond which the propagation will fail
     * @param orbitInterpolator orbit interpolator
     * @param outFrame desired output covariance frame
     * @param outPositionAngle desired output position angle
     * @param outOrbitType desired output orbit type
     *
     * @see Frame
     * @see OrbitType
     * @see PositionAngle
     */
    public AbstractStateCovarianceInterpolator(final int interpolationPoints, final double extrapolationThreshold,
                                               final TimeInterpolator<Orbit> orbitInterpolator,
                                               final Frame outFrame,
                                               final OrbitType outOrbitType,
                                               final PositionAngle outPositionAngle) {
        super(interpolationPoints, extrapolationThreshold);
        this.orbitInterpolator = orbitInterpolator;
        this.outLOF            = null;
        this.outFrame          = outFrame;
        this.outOrbitType      = outOrbitType;
        this.outPositionAngle  = outPositionAngle;
    }

    /**
     * Interpolate orbit and associated covariance.
     *
     * @param interpolationDate interpolation date
     *
     * @return interpolated orbit and associated covariance
     */
    @Override
    public TimeStampedPair<Orbit, StateCovariance> interpolate(final AbsoluteDate interpolationDate) {

        // Interpolate orbit at interpolation date
        final Orbit interpolatedOrbit = interpolateOrbit(interpolationDate);

        // Rebuild state covariance
        final StateCovariance covarianceInOrbitFrame =
                computeInterpolatedCovarianceInOrbitFrame(neighborList, interpolatedOrbit);

        // Output new blended StateCovariance instance in desired output
        return expressCovarianceInDesiredOutput(interpolatedOrbit, covarianceInOrbitFrame);
    }

    /** @return output frame. Can be null. */
    public Frame getOutFrame() {
        return outFrame;
    }

    /** @return output local orbital frame. Can be null. */
    public LOFType getOutLOF() {
        return outLOF;
    }

    /** @return output orbit type. */
    public OrbitType getOutOrbitType() {
        return outOrbitType;
    }

    /** @return output position angle. */
    public PositionAngle getOutPositionAngle() {
        return outPositionAngle;
    }

    /** @return orbit interpolator. */
    public TimeInterpolator<Orbit> getOrbitInterpolator() {
        return orbitInterpolator;
    }

    /**
     * Interpolate orbit at given interpolation date.
     *
     * @param interpolationDate interpolation date
     *
     * @return interpolated orbit
     */
    protected Orbit interpolateOrbit(final AbsoluteDate interpolationDate) {

        // Build orbit list from uncertain orbits
        final List<Orbit> orbits = buildOrbitList();

        return orbitInterpolator.interpolate(interpolationDate, orbits);
    }

    /**
     * Compute the interpolated covariance expressed in the interpolated orbit frame.
     *
     * @param uncertainStates list of orbits and associated covariances
     * @param interpolatedOrbit interpolated orbit
     *
     * @return interpolated covariance expressed in the interpolated orbit frame
     */
    protected abstract StateCovariance computeInterpolatedCovarianceInOrbitFrame(
            List<TimeStampedPair<Orbit, StateCovariance>> uncertainStates,
            Orbit interpolatedOrbit);

    /**
     * Express covariance in output configuration defined at this instance construction.
     *
     * @param interpolatedOrbit interpolated orbit
     * @param covarianceInOrbitFrame covariance expressed in interpolated orbit frame
     *
     * @return covariance in desired output configuration
     */
    protected TimeStampedPair<Orbit, StateCovariance> expressCovarianceInDesiredOutput(final Orbit interpolatedOrbit,
                                                                                       final StateCovariance covarianceInOrbitFrame) {

        final StateCovariance covarianceOutput;

        // Output frame is defined
        if (outLOF == null) {

            // Output frame is pseudo inertial
            if (outFrame.isPseudoInertial()) {
                final StateCovariance covarianceInOutputFrame =
                        covarianceInOrbitFrame.changeCovarianceFrame(interpolatedOrbit, outFrame);

                covarianceOutput =
                        covarianceInOutputFrame.changeCovarianceType(interpolatedOrbit, outOrbitType, outPositionAngle);
            }
            // Output frame is not pseudo inertial
            else {
                covarianceOutput = covarianceInOrbitFrame.changeCovarianceFrame(interpolatedOrbit, outFrame);
            }

        }
        // Output local orbital frame is defined
        else {
            covarianceOutput = covarianceInOrbitFrame.changeCovarianceFrame(interpolatedOrbit, outLOF);
        }

        return new TimeStampedPair<>(interpolatedOrbit, covarianceOutput);
    }

    /**
     * Build an orbit list from cached samples.
     *
     * @return orbit list
     */
    private List<Orbit> buildOrbitList() {

        // Get samples stream
        final Stream<TimeStampedPair<Orbit, StateCovariance>> uncertainStateStream = neighborList.stream();

        // Map to orbit
        final Stream<Orbit> orbitStream = uncertainStateStream.map(TimeStampedPair::getFirst);

        // Convert to list
        return orbitStream.collect(Collectors.toList());
    }
}

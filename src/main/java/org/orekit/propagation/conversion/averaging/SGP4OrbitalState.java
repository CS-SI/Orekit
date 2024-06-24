/* Copyright 2020-2024 Exotrail
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
package org.orekit.propagation.conversion.averaging;

import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.averaging.elements.AveragedKeplerianWithMeanAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.UTCScale;

/**
 * Class representing an averaged orbital state as in the TLE-related theory.
 * Note it is the averaged mean motion that is written in a Two-Line Element and that, for now,
 * conversions back and forth to averaged semi-major axis are approximated with the osculating ones.
 *
 * @author Romain Serra
 * @see AveragedOrbitalState
 * @see TLEPropagator
 * @since 12.1
 */
public class SGP4OrbitalState extends AbstractAveragedOrbitalState {

    /** B-star used internally and set to zero. Should not impact calculations. */
    private static final double B_STAR = 0.;

    /** Averaged Keplerian elements. */
    private final AveragedKeplerianWithMeanAngle averagedElements;
    /** UTC time scale. */
    private final UTCScale utc;

    /**
     * Constructor.
     * @param date epoch
     * @param elements averaged orbital elements
     * @param dataContext data context
     */
    public SGP4OrbitalState(final AbsoluteDate date,
                            final AveragedKeplerianWithMeanAngle elements,
                            final DataContext dataContext) {
        this(date, elements, dataContext.getFrames().getTEME(),
                dataContext.getTimeScales().getUTC());
    }

    /**
     * Constructor with default data context.
     * @param date epoch
     * @param elements averaged orbital elements
     */
    @DefaultDataContext
    public SGP4OrbitalState(final AbsoluteDate date,
                            final AveragedKeplerianWithMeanAngle elements) {
        this(date, elements, DataContext.getDefault());
    }

    /**
     * Private constructor.
     * @param date epoch
     * @param elements averaged orbital elements
     * @param teme TEME frame
     * @param utc UTC time scale
     */
    private SGP4OrbitalState(final AbsoluteDate date,
                             final AveragedKeplerianWithMeanAngle elements,
                             final Frame teme, final TimeScale utc) {
        super(date, teme);
        this.averagedElements = elements;
        this.utc = (UTCScale) utc;
    }

    /**
     * Static constructor. Input frame is implicitly assumed to be TEME (it is not checked).
     * @param tle TLE
     * @param teme TEME frame (not checked)
     * @return TLE-based averaged orbital state
     */
    public static SGP4OrbitalState of(final TLE tle, final Frame teme) {
        final double semiMajorAxis = computeSemiMajorAxis(tle);
        final AveragedKeplerianWithMeanAngle elements = new AveragedKeplerianWithMeanAngle(
                semiMajorAxis, tle.getE(), tle.getI(), tle.getPerigeeArgument(), tle.getRaan(),
                tle.getMeanAnomaly());
        return new SGP4OrbitalState(tle.getDate(), elements, teme, tle.getUtc());
    }

    /** {@inheritDoc} */
    @Override
    public double getMu() {
        return getTleMu();
    }

    /**
     * Getter for TLE's Earth gravitational constant.
     * @return mu.
     */
    private static double getTleMu() {
        return TLEPropagator.getMU();
    }

    /** {@inheritDoc} */
    @Override
    public OrbitType getOrbitType() {
        return OrbitType.KEPLERIAN;
    }

    /** {@inheritDoc} */
    @Override
    public PositionAngleType getPositionAngleType() {
        return PositionAngleType.MEAN;
    }

    /** {@inheritDoc} */
    @Override
    public AveragedKeplerianWithMeanAngle getAveragedElements() {
        return averagedElements;
    }

    /** {@inheritDoc} */
    @Override
    public Orbit toOsculatingOrbit() {
        final TLEPropagator propagator = createPropagator();
        return propagator.getInitialState().getOrbit();
    }

    /**
     * Create TLE propagator.
     * @return propagator using relevant theory
     */
    private TLEPropagator createPropagator() {
        final TLE tle = createTLE();
        return TLEPropagator.selectExtrapolator(tle, getFrame());
    }

    /**
     * Create Orekit TLE.
     * @return TLE
     */
    private TLE createTLE() {
        final double averagedMeanMotion = computeMeanMotion(getAveragedElements()
                .getAveragedSemiMajorAxis());
        final double averagedEccentricity = getAveragedElements().getAveragedEccentricity();
        final double averagedInclination = getAveragedElements().getAveragedInclination();
        final double averagedRAAN = getAveragedElements().getAveragedRightAscensionOfTheAscendingNode();
        final double averagedPerigeeArgument = getAveragedElements().getAveragedPerigeeArgument();
        final double averagedMeanAnomaly = getAveragedElements().getAveragedMeanAnomaly();
        return new TLE(0, (char) 0, 2000, 1, "1", 0, 0,
                getDate(), averagedMeanMotion, 0., 0., averagedEccentricity,
                averagedInclination, averagedPerigeeArgument, averagedRAAN, averagedMeanAnomaly,
                1, B_STAR, utc);
    }

    /**
     * Convert averaged semi-major axis to averaged mean motion. Uses an approximate transformation
     * (same as osculating).
     * @param averagedSemiMajorAxis semi-major axis
     * @return mean motion
     */
    private static double computeMeanMotion(final double averagedSemiMajorAxis) {
        final double cubedSemiMajorAxis = averagedSemiMajorAxis * averagedSemiMajorAxis *
                averagedSemiMajorAxis;
        return FastMath.sqrt(getTleMu() / cubedSemiMajorAxis);
    }

    /**
     * Compute semi-major axis from Two-Line Elements. Uses an approximate transformation
     * (same as osculating).
     * @param tle TLE
     * @return semi-major axis
     */
    private static double computeSemiMajorAxis(final TLE tle) {
        final double meanMotion = tle.getMeanMotion();
        return FastMath.cbrt(getTleMu() / (meanMotion * meanMotion));
    }
}

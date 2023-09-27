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
package org.orekit.orbits;

import org.hipparchus.analysis.interpolation.HermiteInterpolator;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitInternalError;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterpolator;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinatesHermiteInterpolator;

import java.util.List;
import java.util.stream.Stream;

/**
 * Class using a Hermite interpolator to interpolate orbits.
 * <p>
 * Depending on given sample orbit type, the interpolation may differ :
 * <ul>
 *    <li>For Keplerian, Circular and Equinoctial orbits, the interpolated instance is created by polynomial Hermite
 *    interpolation, using derivatives when available. </li>
 *    <li>For Cartesian orbits, the interpolated instance is created using the cartesian derivatives filter given at
 *    instance construction. Hence, it will fall back to Lagrange interpolation if this instance has been designed to not
 *    use derivatives.
 * </ul>
 * <p>
 * In any case, it should be used only with small number of interpolation points (about 10-20 points) in order to avoid
 * <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a> and numerical problems
 * (including NaN appearing).
 *
 * @author Luc Maisonobe
 * @author Vincent Cucchietti
 * @see Orbit
 * @see HermiteInterpolator
 */
public class OrbitHermiteInterpolator extends AbstractOrbitInterpolator {

    /** Filter for derivatives from the sample to use in position-velocity-acceleration interpolation. */
    private final CartesianDerivativesFilter pvaFilter;

    /**
     * Constructor with :
     * <ul>
     *     <li>Default number of interpolation points of {@code DEFAULT_INTERPOLATION_POINTS}</li>
     *     <li>Default extrapolation threshold value ({@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s)</li>
     *     <li>Use of position and two time derivatives during interpolation</li>
     * </ul>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     *
     * @param outputInertialFrame output inertial frame
     */
    public OrbitHermiteInterpolator(final Frame outputInertialFrame) {
        this(DEFAULT_INTERPOLATION_POINTS, outputInertialFrame);
    }

    /**
     * Constructor with :
     * <ul>
     *     <li>Default extrapolation threshold value ({@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s)</li>
     *     <li>Use of position and two time derivatives during interpolation</li>
     * </ul>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     *
     * @param interpolationPoints number of interpolation points
     * @param outputInertialFrame output inertial frame
     */
    public OrbitHermiteInterpolator(final int interpolationPoints, final Frame outputInertialFrame) {
        this(interpolationPoints, outputInertialFrame, CartesianDerivativesFilter.USE_PVA);
    }

    /**
     * Constructor with default extrapolation threshold value ({@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s).
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     *
     * @param interpolationPoints number of interpolation points
     * @param outputInertialFrame output inertial frame
     * @param pvaFilter filter for derivatives from the sample to use in position-velocity-acceleration interpolation. Used
     * only when interpolating Cartesian orbits.
     */
    public OrbitHermiteInterpolator(final int interpolationPoints, final Frame outputInertialFrame,
                                    final CartesianDerivativesFilter pvaFilter) {
        this(interpolationPoints, DEFAULT_EXTRAPOLATION_THRESHOLD_SEC, outputInertialFrame, pvaFilter);
    }

    /**
     * Constructor.
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     *
     * @param interpolationPoints number of interpolation points
     * @param extrapolationThreshold extrapolation threshold beyond which the propagation will fail
     * @param outputInertialFrame output inertial frame
     * @param pvaFilter filter for derivatives from the sample to use in position-velocity-acceleration interpolation. Used
     * only when interpolating Cartesian orbits.
     */
    public OrbitHermiteInterpolator(final int interpolationPoints, final double extrapolationThreshold,
                                    final Frame outputInertialFrame, final CartesianDerivativesFilter pvaFilter) {
        super(interpolationPoints, extrapolationThreshold, outputInertialFrame);
        this.pvaFilter = pvaFilter;
    }

    /** Get filter for derivatives from the sample to use in position-velocity-acceleration interpolation.
     * @return filter for derivatives from the sample to use in position-velocity-acceleration interpolation
     */
    public CartesianDerivativesFilter getPVAFilter() {
        return pvaFilter;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Depending on given sample orbit type, the interpolation may differ :
     * <ul>
     *    <li>For Keplerian, Circular and Equinoctial orbits, the interpolated instance is created by polynomial Hermite
     *    interpolation, using derivatives when available. </li>
     *    <li>For Cartesian orbits, the interpolated instance is created using the cartesian derivatives filter given at
     *    instance construction. Hence, it will fall back to Lagrange interpolation if this instance has been designed to not
     *    use derivatives.
     * </ul>
     * If orbit interpolation on large samples is needed, using the {@link
     * org.orekit.propagation.analytical.Ephemeris} class is a better way than using this
     * low-level interpolation. The Ephemeris class automatically handles selection of
     * a neighboring sub-sample with a predefined number of point from a large global sample
     * in a thread-safe way.
     *
     * @param interpolationData interpolation data
     *
     * @return interpolated instance at given date
     */
    @Override
    protected Orbit interpolate(final InterpolationData interpolationData) {
        // Get orbit sample
        final List<Orbit> sample = interpolationData.getNeighborList();

        // Get orbit type for interpolation
        final OrbitType orbitType = sample.get(0).getType();

        if (orbitType == OrbitType.CARTESIAN) {
            return interpolateCartesian(interpolationData.getInterpolationDate(), sample);
        }
        else {
            return interpolateCommon(interpolationData.getInterpolationDate(), sample, orbitType);
        }

    }

    /**
     * Interpolate Cartesian orbit using specific method for Cartesian orbit.
     *
     * @param interpolationDate interpolation date
     * @param sample orbits sample
     *
     * @return interpolated Cartesian orbit
     */
    private CartesianOrbit interpolateCartesian(final AbsoluteDate interpolationDate, final List<Orbit> sample) {

        // Create time stamped position-velocity-acceleration Hermite interpolator
        final TimeInterpolator<TimeStampedPVCoordinates> interpolator =
                new TimeStampedPVCoordinatesHermiteInterpolator(getNbInterpolationPoints(), getExtrapolationThreshold(),
                                                                pvaFilter);

        // Convert sample to stream
        final Stream<Orbit> sampleStream = sample.stream();

        // Map time stamped position-velocity-acceleration coordinates
        final Stream<TimeStampedPVCoordinates> sampleTimeStampedPV = sampleStream.map(Orbit::getPVCoordinates);

        // Interpolate PVA
        final TimeStampedPVCoordinates interpolated = interpolator.interpolate(interpolationDate, sampleTimeStampedPV);

        // Use first entry gravitational parameter
        final double mu = sample.get(0).getMu();

        return new CartesianOrbit(interpolated, getOutputInertialFrame(), interpolationDate, mu);
    }

    /**
     * Method gathering common parts of interpolation between circular, equinoctial and keplerian orbit.
     *
     * @param interpolationDate interpolation date
     * @param orbits orbits sample
     * @param orbitType interpolation method to use
     *
     * @return interpolated orbit
     */
    private Orbit interpolateCommon(final AbsoluteDate interpolationDate, final List<Orbit> orbits,
                                    final OrbitType orbitType) {

        // First pass to check if derivatives are available throughout the sample
        boolean useDerivatives = true;
        for (final Orbit orbit : orbits) {
            useDerivatives = useDerivatives && orbit.hasDerivatives();
        }

        // Use first entry gravitational parameter
        final double mu = orbits.get(0).getMu();

        // Interpolate and build a new instance
        final double[][] interpolated;
        switch (orbitType) {
            case CIRCULAR:
                interpolated = interpolateCircular(interpolationDate, orbits, useDerivatives);
                return new CircularOrbit(interpolated[0][0], interpolated[0][1], interpolated[0][2],
                                         interpolated[0][3], interpolated[0][4], interpolated[0][5],
                                         interpolated[1][0], interpolated[1][1], interpolated[1][2],
                                         interpolated[1][3], interpolated[1][4], interpolated[1][5],
                                         PositionAngleType.MEAN, getOutputInertialFrame(), interpolationDate, mu);
            case KEPLERIAN:
                interpolated = interpolateKeplerian(interpolationDate, orbits, useDerivatives);
                return new KeplerianOrbit(interpolated[0][0], interpolated[0][1], interpolated[0][2],
                                          interpolated[0][3], interpolated[0][4], interpolated[0][5],
                                          interpolated[1][0], interpolated[1][1], interpolated[1][2],
                                          interpolated[1][3], interpolated[1][4], interpolated[1][5],
                                          PositionAngleType.MEAN, getOutputInertialFrame(), interpolationDate, mu);
            case EQUINOCTIAL:
                interpolated = interpolateEquinoctial(interpolationDate, orbits, useDerivatives);
                return new EquinoctialOrbit(interpolated[0][0], interpolated[0][1], interpolated[0][2],
                                            interpolated[0][3], interpolated[0][4], interpolated[0][5],
                                            interpolated[1][0], interpolated[1][1], interpolated[1][2],
                                            interpolated[1][3], interpolated[1][4], interpolated[1][5],
                                            PositionAngleType.MEAN, getOutputInertialFrame(), interpolationDate, mu);
            default:
                // Should never happen
                throw new OrekitInternalError(null);
        }

    }

    /**
     * Build interpolating functions for circular orbit parameters.
     *
     * @param interpolationDate interpolation date
     * @param orbits orbits sample
     * @param useDerivatives flag defining if derivatives are available throughout the sample
     *
     * @return interpolating functions for circular orbit parameters
     */
    private double[][] interpolateCircular(final AbsoluteDate interpolationDate, final List<Orbit> orbits,
                                           final boolean useDerivatives) {

        // Set up an interpolator
        final HermiteInterpolator interpolator = new HermiteInterpolator();

        // Second pass to feed interpolator
        AbsoluteDate previousDate   = null;
        double       previousRAAN   = Double.NaN;
        double       previousAlphaM = Double.NaN;
        for (final Orbit orbit : orbits) {
            final CircularOrbit circ = (CircularOrbit) OrbitType.CIRCULAR.convertType(orbit);
            final double        continuousRAAN;
            final double        continuousAlphaM;
            if (previousDate == null) {
                continuousRAAN   = circ.getRightAscensionOfAscendingNode();
                continuousAlphaM = circ.getAlphaM();
            }
            else {
                final double dt       = circ.getDate().durationFrom(previousDate);
                final double keplerAM = previousAlphaM + circ.getKeplerianMeanMotion() * dt;
                continuousRAAN   = MathUtils.normalizeAngle(circ.getRightAscensionOfAscendingNode(), previousRAAN);
                continuousAlphaM = MathUtils.normalizeAngle(circ.getAlphaM(), keplerAM);
            }
            previousDate   = circ.getDate();
            previousRAAN   = continuousRAAN;
            previousAlphaM = continuousAlphaM;
            if (useDerivatives) {
                interpolator.addSamplePoint(circ.getDate().durationFrom(interpolationDate),
                                            new double[] { circ.getA(),
                                                           circ.getCircularEx(),
                                                           circ.getCircularEy(),
                                                           circ.getI(),
                                                           continuousRAAN,
                                                           continuousAlphaM },
                                            new double[] { circ.getADot(),
                                                           circ.getCircularExDot(),
                                                           circ.getCircularEyDot(),
                                                           circ.getIDot(),
                                                           circ.getRightAscensionOfAscendingNodeDot(),
                                                           circ.getAlphaMDot() });
            }
            else {
                interpolator.addSamplePoint(circ.getDate().durationFrom(interpolationDate),
                                            new double[] { circ.getA(),
                                                           circ.getCircularEx(),
                                                           circ.getCircularEy(),
                                                           circ.getI(),
                                                           continuousRAAN,
                                                           continuousAlphaM });
            }
        }

        return interpolator.derivatives(0.0, 1);
    }

    /**
     * Build interpolating functions for keplerian orbit parameters.
     *
     * @param interpolationDate interpolation date
     * @param orbits orbits sample
     * @param useDerivatives flag defining if derivatives are available throughout the sample
     *
     * @return interpolating functions for keplerian orbit parameters
     */
    private double[][] interpolateKeplerian(final AbsoluteDate interpolationDate, final List<Orbit> orbits,
                                            final boolean useDerivatives) {

        // Set up an interpolator
        final HermiteInterpolator interpolator = new HermiteInterpolator();

        // Second pass to feed interpolator
        AbsoluteDate previousDate = null;
        double       previousPA   = Double.NaN;
        double       previousRAAN = Double.NaN;
        double       previousM    = Double.NaN;
        for (final Orbit orbit : orbits) {
            final KeplerianOrbit kep = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(orbit);
            final double         continuousPA;
            final double         continuousRAAN;
            final double         continuousM;
            if (previousDate == null) {
                continuousPA   = kep.getPerigeeArgument();
                continuousRAAN = kep.getRightAscensionOfAscendingNode();
                continuousM    = kep.getMeanAnomaly();
            }
            else {
                final double dt      = kep.getDate().durationFrom(previousDate);
                final double keplerM = previousM + kep.getKeplerianMeanMotion() * dt;
                continuousPA   = MathUtils.normalizeAngle(kep.getPerigeeArgument(), previousPA);
                continuousRAAN = MathUtils.normalizeAngle(kep.getRightAscensionOfAscendingNode(), previousRAAN);
                continuousM    = MathUtils.normalizeAngle(kep.getMeanAnomaly(), keplerM);
            }
            previousDate = kep.getDate();
            previousPA   = continuousPA;
            previousRAAN = continuousRAAN;
            previousM    = continuousM;
            if (useDerivatives) {
                interpolator.addSamplePoint(kep.getDate().durationFrom(interpolationDate),
                                            new double[] { kep.getA(),
                                                           kep.getE(),
                                                           kep.getI(),
                                                           continuousPA,
                                                           continuousRAAN,
                                                           continuousM },
                                            new double[] { kep.getADot(),
                                                           kep.getEDot(),
                                                           kep.getIDot(),
                                                           kep.getPerigeeArgumentDot(),
                                                           kep.getRightAscensionOfAscendingNodeDot(),
                                                           kep.getMeanAnomalyDot() });
            }
            else {
                interpolator.addSamplePoint(kep.getDate().durationFrom(interpolationDate),
                                            new double[] { kep.getA(),
                                                           kep.getE(),
                                                           kep.getI(),
                                                           continuousPA,
                                                           continuousRAAN,
                                                           continuousM });
            }
        }

        return interpolator.derivatives(0.0, 1);
    }

    /**
     * Build interpolating functions for equinoctial orbit parameters.
     *
     * @param interpolationDate interpolation date
     * @param orbits orbits sample
     * @param useDerivatives flag defining if derivatives are available throughout the sample
     *
     * @return interpolating functions for equinoctial orbit parameters
     */
    private double[][] interpolateEquinoctial(final AbsoluteDate interpolationDate, final List<Orbit> orbits,
                                              final boolean useDerivatives) {

        // Set up an interpolator
        final HermiteInterpolator interpolator = new HermiteInterpolator();

        // Second pass to feed interpolator
        AbsoluteDate previousDate = null;
        double       previousLm   = Double.NaN;
        for (final Orbit orbit : orbits) {
            final EquinoctialOrbit equi = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(orbit);
            final double           continuousLm;
            if (previousDate == null) {
                continuousLm = equi.getLM();
            }
            else {
                final double dt       = equi.getDate().durationFrom(previousDate);
                final double keplerLm = previousLm + equi.getKeplerianMeanMotion() * dt;
                continuousLm = MathUtils.normalizeAngle(equi.getLM(), keplerLm);
            }
            previousDate = equi.getDate();
            previousLm   = continuousLm;
            if (useDerivatives) {
                interpolator.addSamplePoint(equi.getDate().durationFrom(interpolationDate),
                                            new double[] { equi.getA(),
                                                           equi.getEquinoctialEx(),
                                                           equi.getEquinoctialEy(),
                                                           equi.getHx(),
                                                           equi.getHy(),
                                                           continuousLm },
                                            new double[] {
                                                    equi.getADot(),
                                                    equi.getEquinoctialExDot(),
                                                    equi.getEquinoctialEyDot(),
                                                    equi.getHxDot(),
                                                    equi.getHyDot(),
                                                    equi.getLMDot() });
            }
            else {
                interpolator.addSamplePoint(equi.getDate().durationFrom(interpolationDate),
                                            new double[] { equi.getA(),
                                                           equi.getEquinoctialEx(),
                                                           equi.getEquinoctialEy(),
                                                           equi.getHx(),
                                                           equi.getHy(),
                                                           continuousLm });
            }
        }

        return interpolator.derivatives(0.0, 1);
    }
}

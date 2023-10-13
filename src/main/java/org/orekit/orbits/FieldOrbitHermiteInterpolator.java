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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.interpolation.FieldHermiteInterpolator;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitInternalError;
import org.orekit.frames.Frame;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeInterpolator;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinatesHermiteInterpolator;

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
 * @param <KK> type of the field element
 *
 * @author Luc Maisonobe
 * @author Vincent Cucchietti
 * @see FieldOrbit
 * @see FieldHermiteInterpolator
 */
public class FieldOrbitHermiteInterpolator<KK extends CalculusFieldElement<KK>> extends AbstractFieldOrbitInterpolator<KK> {

    /** Filter for derivatives from the sample to use in position-velocity-acceleration interpolation. */
    private final CartesianDerivativesFilter pvaFilter;

    /** Field of the elements. */
    private Field<KK> field;

    /** Fielded zero. */
    private KK zero;

    /** Fielded one. */
    private KK one;

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
    public FieldOrbitHermiteInterpolator(final Frame outputInertialFrame) {
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
    public FieldOrbitHermiteInterpolator(final int interpolationPoints, final Frame outputInertialFrame) {
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
     * @param pvaFilter filter for derivatives from the sample to use in position-velocity-acceleration interpolation
     */
    public FieldOrbitHermiteInterpolator(final int interpolationPoints, final Frame outputInertialFrame,
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
     * @param pvaFilter filter for derivatives from the sample to use in position-velocity-acceleration interpolation
     */
    public FieldOrbitHermiteInterpolator(final int interpolationPoints, final double extrapolationThreshold,
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
     * @return interpolated instance for given interpolation data
     */
    @Override
    protected FieldOrbit<KK> interpolate(final InterpolationData interpolationData) {

        // Get interpolation date
        final FieldAbsoluteDate<KK> interpolationDate = interpolationData.getInterpolationDate();

        // Get orbit sample
        final List<FieldOrbit<KK>> sample = interpolationData.getNeighborList();

        // Get first entry
        final FieldOrbit<KK> firstEntry = sample.get(0);

        // Get orbit type for interpolation
        final OrbitType orbitType = firstEntry.getType();

        // Extract field
        this.field = firstEntry.getA().getField();
        this.zero  = field.getZero();
        this.one   = field.getOne();

        if (orbitType == OrbitType.CARTESIAN) {
            return interpolateCartesian(interpolationDate, sample);
        }
        else {
            return interpolateCommon(interpolationDate, sample, orbitType);
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
    private FieldCartesianOrbit<KK> interpolateCartesian(final FieldAbsoluteDate<KK> interpolationDate,
                                                         final List<FieldOrbit<KK>> sample) {

        // Create time stamped position-velocity-acceleration Hermite interpolator
        final FieldTimeInterpolator<TimeStampedFieldPVCoordinates<KK>, KK> interpolator =
                new TimeStampedFieldPVCoordinatesHermiteInterpolator<>(getNbInterpolationPoints(),
                                                                       getExtrapolationThreshold(),
                                                                       pvaFilter);

        // Convert sample to stream
        final Stream<FieldOrbit<KK>> sampleStream = sample.stream();

        // Map time stamped position-velocity-acceleration coordinates
        final Stream<TimeStampedFieldPVCoordinates<KK>> sampleTimeStampedPV = sampleStream.map(FieldOrbit::getPVCoordinates);

        // Interpolate PVA
        final TimeStampedFieldPVCoordinates<KK> interpolated =
                interpolator.interpolate(interpolationDate, sampleTimeStampedPV);

        // Use first entry gravitational parameter
        final KK mu = sample.get(0).getMu();

        return new FieldCartesianOrbit<>(interpolated, getOutputInertialFrame(), interpolationDate, mu);
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
    private FieldOrbit<KK> interpolateCommon(final FieldAbsoluteDate<KK> interpolationDate,
                                             final List<FieldOrbit<KK>> orbits,
                                             final OrbitType orbitType) {

        // First pass to check if derivatives are available throughout the sample
        boolean useDerivatives = true;
        for (final FieldOrbit<KK> orbit : orbits) {
            useDerivatives = useDerivatives && orbit.hasDerivatives();
        }

        // Use first entry gravitational parameter
        final KK mu = orbits.get(0).getMu();

        // Interpolate and build a new instance
        final KK[][] interpolated;
        switch (orbitType) {
            case CIRCULAR:
                interpolated = interpolateCircular(interpolationDate, orbits, useDerivatives);
                return new FieldCircularOrbit<>(interpolated[0][0], interpolated[0][1], interpolated[0][2],
                                                interpolated[0][3], interpolated[0][4], interpolated[0][5],
                                                interpolated[1][0], interpolated[1][1], interpolated[1][2],
                                                interpolated[1][3], interpolated[1][4], interpolated[1][5],
                                                PositionAngleType.MEAN, getOutputInertialFrame(), interpolationDate, mu);
            case KEPLERIAN:
                interpolated = interpolateKeplerian(interpolationDate, orbits, useDerivatives);
                return new FieldKeplerianOrbit<>(interpolated[0][0], interpolated[0][1], interpolated[0][2],
                                                 interpolated[0][3], interpolated[0][4], interpolated[0][5],
                                                 interpolated[1][0], interpolated[1][1], interpolated[1][2],
                                                 interpolated[1][3], interpolated[1][4], interpolated[1][5],
                                                 PositionAngleType.MEAN, getOutputInertialFrame(), interpolationDate, mu);
            case EQUINOCTIAL:
                interpolated = interpolateEquinoctial(interpolationDate, orbits, useDerivatives);
                return new FieldEquinoctialOrbit<>(interpolated[0][0], interpolated[0][1], interpolated[0][2],
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
    private KK[][] interpolateCircular(final FieldAbsoluteDate<KK> interpolationDate, final List<FieldOrbit<KK>> orbits,
                                       final boolean useDerivatives) {

        // set up an interpolator
        final FieldHermiteInterpolator<KK> interpolator = new FieldHermiteInterpolator<>();

        // second pass to feed interpolator
        FieldAbsoluteDate<KK> previousDate   = null;
        KK                    previousRAAN   = zero.add(Double.NaN);
        KK                    previousAlphaM = zero.add(Double.NaN);
        for (final FieldOrbit<KK> orbit : orbits) {
            final FieldCircularOrbit<KK> circ = (FieldCircularOrbit<KK>) OrbitType.CIRCULAR.convertType(orbit);
            final KK                     continuousRAAN;
            final KK                     continuousAlphaM;
            if (previousDate == null) {
                continuousRAAN   = circ.getRightAscensionOfAscendingNode();
                continuousAlphaM = circ.getAlphaM();
            }
            else {
                final KK dt       = circ.getDate().durationFrom(previousDate);
                final KK keplerAM = previousAlphaM.add(circ.getKeplerianMeanMotion().multiply(dt));
                continuousRAAN   = MathUtils.normalizeAngle(circ.getRightAscensionOfAscendingNode(), previousRAAN);
                continuousAlphaM = MathUtils.normalizeAngle(circ.getAlphaM(), keplerAM);
            }
            previousDate   = circ.getDate();
            previousRAAN   = continuousRAAN;
            previousAlphaM = continuousAlphaM;
            final KK[] toAdd = MathArrays.buildArray(one.getField(), 6);
            toAdd[0] = circ.getA();
            toAdd[1] = circ.getCircularEx();
            toAdd[2] = circ.getCircularEy();
            toAdd[3] = circ.getI();
            toAdd[4] = continuousRAAN;
            toAdd[5] = continuousAlphaM;
            if (useDerivatives) {
                final KK[] toAddDot = MathArrays.buildArray(one.getField(), 6);
                toAddDot[0] = circ.getADot();
                toAddDot[1] = circ.getCircularExDot();
                toAddDot[2] = circ.getCircularEyDot();
                toAddDot[3] = circ.getIDot();
                toAddDot[4] = circ.getRightAscensionOfAscendingNodeDot();
                toAddDot[5] = circ.getAlphaMDot();
                interpolator.addSamplePoint(circ.getDate().durationFrom(interpolationDate),
                                            toAdd, toAddDot);
            }
            else {
                interpolator.addSamplePoint(circ.getDate().durationFrom(interpolationDate),
                                            toAdd);
            }
        }

        return interpolator.derivatives(zero, 1);
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
    private KK[][] interpolateKeplerian(final FieldAbsoluteDate<KK> interpolationDate, final List<FieldOrbit<KK>> orbits,
                                        final boolean useDerivatives) {

        // Set up an interpolator
        final FieldHermiteInterpolator<KK> interpolator = new FieldHermiteInterpolator<>();

        // Second pass to feed interpolator
        FieldAbsoluteDate<KK> previousDate = null;
        KK                    previousPA   = zero.add(Double.NaN);
        KK                    previousRAAN = zero.add(Double.NaN);
        KK                    previousM    = zero.add(Double.NaN);
        for (final FieldOrbit<KK> orbit : orbits) {
            final FieldKeplerianOrbit<KK> kep = (FieldKeplerianOrbit<KK>) OrbitType.KEPLERIAN.convertType(orbit);
            final KK                      continuousPA;
            final KK                      continuousRAAN;
            final KK                      continuousM;
            if (previousDate == null) {
                continuousPA   = kep.getPerigeeArgument();
                continuousRAAN = kep.getRightAscensionOfAscendingNode();
                continuousM    = kep.getMeanAnomaly();
            }
            else {
                final KK dt      = kep.getDate().durationFrom(previousDate);
                final KK keplerM = previousM.add(kep.getKeplerianMeanMotion().multiply(dt));
                continuousPA   = MathUtils.normalizeAngle(kep.getPerigeeArgument(), previousPA);
                continuousRAAN = MathUtils.normalizeAngle(kep.getRightAscensionOfAscendingNode(), previousRAAN);
                continuousM    = MathUtils.normalizeAngle(kep.getMeanAnomaly(), keplerM);
            }
            previousDate = kep.getDate();
            previousPA   = continuousPA;
            previousRAAN = continuousRAAN;
            previousM    = continuousM;
            final KK[] toAdd = MathArrays.buildArray(field, 6);
            toAdd[0] = kep.getA();
            toAdd[1] = kep.getE();
            toAdd[2] = kep.getI();
            toAdd[3] = continuousPA;
            toAdd[4] = continuousRAAN;
            toAdd[5] = continuousM;
            if (useDerivatives) {
                final KK[] toAddDot = MathArrays.buildArray(field, 6);
                toAddDot[0] = kep.getADot();
                toAddDot[1] = kep.getEDot();
                toAddDot[2] = kep.getIDot();
                toAddDot[3] = kep.getPerigeeArgumentDot();
                toAddDot[4] = kep.getRightAscensionOfAscendingNodeDot();
                toAddDot[5] = kep.getMeanAnomalyDot();
                interpolator.addSamplePoint(kep.getDate().durationFrom(interpolationDate),
                                            toAdd, toAddDot);
            }
            else {
                interpolator.addSamplePoint(this.zero.add(kep.getDate().durationFrom(interpolationDate)),
                                            toAdd);
            }
        }

        return interpolator.derivatives(zero, 1);
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
    private KK[][] interpolateEquinoctial(final FieldAbsoluteDate<KK> interpolationDate, final List<FieldOrbit<KK>> orbits,
                                          final boolean useDerivatives) {

        // set up an interpolator
        final FieldHermiteInterpolator<KK> interpolator = new FieldHermiteInterpolator<>();

        // second pass to feed interpolator
        FieldAbsoluteDate<KK> previousDate = null;
        KK                    previousLm   = zero.add(Double.NaN);
        for (final FieldOrbit<KK> orbit : orbits) {
            final FieldEquinoctialOrbit<KK> equi = (FieldEquinoctialOrbit<KK>) OrbitType.EQUINOCTIAL.convertType(orbit);
            final KK                        continuousLm;
            if (previousDate == null) {
                continuousLm = equi.getLM();
            }
            else {
                final KK dt       = equi.getDate().durationFrom(previousDate);
                final KK keplerLm = previousLm.add(equi.getKeplerianMeanMotion().multiply(dt));
                continuousLm = MathUtils.normalizeAngle(equi.getLM(), keplerLm);
            }
            previousDate = equi.getDate();
            previousLm   = continuousLm;
            final KK[] toAdd = MathArrays.buildArray(field, 6);
            toAdd[0] = equi.getA();
            toAdd[1] = equi.getEquinoctialEx();
            toAdd[2] = equi.getEquinoctialEy();
            toAdd[3] = equi.getHx();
            toAdd[4] = equi.getHy();
            toAdd[5] = continuousLm;
            if (useDerivatives) {
                final KK[] toAddDot = MathArrays.buildArray(one.getField(), 6);
                toAddDot[0] = equi.getADot();
                toAddDot[1] = equi.getEquinoctialExDot();
                toAddDot[2] = equi.getEquinoctialEyDot();
                toAddDot[3] = equi.getHxDot();
                toAddDot[4] = equi.getHyDot();
                toAddDot[5] = equi.getLMDot();
                interpolator.addSamplePoint(equi.getDate().durationFrom(interpolationDate),
                                            toAdd, toAddDot);
            }
            else {
                interpolator.addSamplePoint(equi.getDate().durationFrom(interpolationDate),
                                            toAdd);
            }
        }

        return interpolator.derivatives(zero, 1);
    }
}

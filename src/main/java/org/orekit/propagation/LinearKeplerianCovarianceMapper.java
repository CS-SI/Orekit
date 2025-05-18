/* Copyright 2022-2025 Romain Serra
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

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.MathArrays;
import org.orekit.frames.LOF;
import org.orekit.frames.LOFType;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleBased;
import org.orekit.orbits.PositionAngleType;
import org.orekit.utils.DerivativeStateUtils;

/**
 * Package private class to map orbital covariance using linearized Keplerian motion
 * (with non-Keplerian correction from derivatives if available).
 * The linearization uses the same coordinates as the ones for which the covariance coefficients are defined.
 *
 * @author Romain Serra
 * @since 13.1
 */
class LinearKeplerianCovarianceMapper {

    /** Default local orbital frame to use. */
    private static final LOF DEFAULT_LOF = LOFType.QSW;

    /** Reference orbit. */
    private final Orbit orbit;

    /** Reference orbital covariance. */
    private final StateCovariance stateCovariance;

    /** Converted covariance (from reference) using the same characteristics than the reference orbit. */
    private final StateCovariance convertedCovariance;

    /**
     * Constructor.
     * @param orbit initial orbit
     * @param stateCovariance initial orbital covariance
     */
    LinearKeplerianCovarianceMapper(final Orbit orbit, final StateCovariance stateCovariance) {
        this.orbit = orbit;
        this.stateCovariance = stateCovariance;
        this.convertedCovariance = changeCovarianceBefore();
    }

    /**
     * Maps the orbital covariance to the target orbit based on Keplerian linearization.
     * The coordinates and frame are kept the same as the reference covariance.
     * @param targetOrbit target orbit
     * @return mapped covariance
     */
    StateCovariance map(final Orbit targetOrbit) {
        final PositionAngleType positionAngleType = findPositionAngleType();
        final StateCovariance shiftedCovariance = shiftCovariance(orbit, convertedCovariance, targetOrbit,
                positionAngleType);
        return changeCovarianceAfter(shiftedCovariance, targetOrbit, positionAngleType);
    }

    /**
     * Convert the initial orbital covariance in the same coordinates and frame than the initial orbit.
     * @return covariance
     */
    private StateCovariance changeCovarianceBefore() {
        final PositionAngleType positionAngleType = findPositionAngleType();
        final StateCovariance intermediateCovariance = stateCovariance.changeCovarianceFrame(orbit, orbit.getFrame());
        return intermediateCovariance.changeCovarianceType(orbit, orbit.getType(), positionAngleType);
    }

    /**
     * Extract the position angle type from the initial orbit.
     * @return position angle type
     */
    private PositionAngleType findPositionAngleType() {
        if (orbit instanceof PositionAngleBased<?>) {
            final PositionAngleBased<?> positionAngleBased = (PositionAngleBased<?>) orbit;
            return positionAngleBased.getCachedPositionAngleType();
        } else {
            // Cartesian
            return null;
        }
    }

    /**
     * Shift orbital covariance according to linearized Keplerian motion, with corrections if non-Keplerian derivatives are present.
     * @param orbit reference orbit
     * @param convertedCovariance covariance to shift
     * @param nextOrbit target orbit
     * @param positionAngleType position angle type to use
     * @return shifted covariance
     */
    private static StateCovariance shiftCovariance(final Orbit orbit, final StateCovariance convertedCovariance,
                                                   final Orbit nextOrbit, final PositionAngleType positionAngleType) {
        final RealMatrix initialCovarianceMatrix = convertedCovariance.getMatrix();
        final GradientField gradientField = GradientField.getField(6);
        final FieldOrbit<Gradient> fieldOrbit = DerivativeStateUtils.buildOrbitGradient(gradientField, orbit);
        final FieldOrbit<Gradient> shiftedFieldOrbit = fieldOrbit.shiftedBy(nextOrbit.durationFrom(orbit));
        final Gradient[] orbitalArray = MathArrays.buildArray(gradientField, 6);
        shiftedFieldOrbit.getType().mapOrbitToArray(shiftedFieldOrbit, positionAngleType, orbitalArray, null);
        final RealMatrix stateTransitionMatrix = MatrixUtils.createRealMatrix(6, 6);
        for (int i = 0; i < orbitalArray.length; i++) {
            stateTransitionMatrix.setRow(i, orbitalArray[i].getGradient());
        }
        final RealMatrix covarianceMatrix = stateTransitionMatrix.multiply(initialCovarianceMatrix
                .multiplyTransposed(stateTransitionMatrix));
        return new StateCovariance(covarianceMatrix, nextOrbit.getDate(), nextOrbit.getFrame(),
                nextOrbit.getType(), positionAngleType);
    }

    /**
     * Convert an orbital covariance to the same coordinates and frame than the initial, reference one.
     * @param covarianceToConvert covariance to convert
     * @param nextOrbit target orbit
     * @param positionAngleType position angle type to use
     * @return converted covariance
     */
    private StateCovariance changeCovarianceAfter(final StateCovariance covarianceToConvert, final Orbit nextOrbit,
                                                  final PositionAngleType positionAngleType) {
        final Orbit propagatedOrbit = orbit.shiftedBy(nextOrbit.durationFrom(orbit));
        final LOF lof = stateCovariance.getLOF() == null ? DEFAULT_LOF : stateCovariance.getLOF();
        final StateCovariance cartesianCovarianceInFrame = covarianceToConvert.changeCovarianceType(propagatedOrbit,
                OrbitType.CARTESIAN, positionAngleType);
        final StateCovariance covarianceInLof = cartesianCovarianceInFrame.changeCovarianceFrame(propagatedOrbit, lof);
        if (stateCovariance.getLOF() != null) {
            return covarianceInLof;
        } else {
            // convert back from an arbitrary LOF to reduce approximation
            final StateCovariance covariance = covarianceInLof.changeCovarianceFrame(nextOrbit, nextOrbit.getFrame());
            if (stateCovariance.getOrbitType() != OrbitType.CARTESIAN) {
                return covariance.changeCovarianceType(propagatedOrbit, stateCovariance.getOrbitType(), positionAngleType);
            } else {
                return covariance;
            }
        }
    }
}

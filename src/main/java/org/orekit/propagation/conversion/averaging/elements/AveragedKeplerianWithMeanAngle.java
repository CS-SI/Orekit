/* Copyright 2020-2025 Exotrail
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
package org.orekit.propagation.conversion.averaging.elements;

/**
 * Immutable class containing values of averaged Keplerian elements from any applicable theory
 * (with MEAN as {@link org.orekit.orbits.PositionAngleType}).
 *
 * @author Romain Serra
 * @see AveragedOrbitalElements
 * @since 12.1
 */
public class AveragedKeplerianWithMeanAngle implements AveragedOrbitalElements {

    /** Averaged semi-major axis in arbitrary theory. */
    private final double averagedSemiMajorAxis;
    /** Averaged eccentricity in arbitrary theory. */
    private final double averagedEccentricity;
    /** Averaged inclination in arbitrary theory. */
    private final double averagedInclination;
    /** Averaged perigee argument in arbitrary theory. */
    private final double averagedPerigeeArgument;
    /** Averaged right ascension of the ascending node in arbitrary theory. */
    private final double averagedRightAscensionOfTheAscendingNode;
    /** Averaged mean anomaly in arbitrary theory. */
    private final double averagedMeanAnomaly;

    /**
     * Constructor.
     * @param averagedSemiMajorAxis averaged semi-major axis
     * @param averagedEccentricity averaged eccentricity
     * @param averagedInclination averaged inclination
     * @param averagedPerigeeArgument averaged perigee argument
     * @param averagedRightAscensionOfTheAscendingNode averaged RAAN
     * @param averagedMeanAnomaly averaged mean anomaly
     */
    public AveragedKeplerianWithMeanAngle(final double averagedSemiMajorAxis,
                                          final double averagedEccentricity,
                                          final double averagedInclination,
                                          final double averagedPerigeeArgument,
                                          final double averagedRightAscensionOfTheAscendingNode,
                                          final double averagedMeanAnomaly) {
        this.averagedSemiMajorAxis = averagedSemiMajorAxis;
        this.averagedEccentricity = averagedEccentricity;
        this.averagedInclination = averagedInclination;
        this.averagedPerigeeArgument = averagedPerigeeArgument;
        this.averagedRightAscensionOfTheAscendingNode = averagedRightAscensionOfTheAscendingNode;
        this.averagedMeanAnomaly = averagedMeanAnomaly;
    }

    /** {@inheritDoc} */
    @Override
    public double[] toArray() {
        return new double[] { averagedSemiMajorAxis, averagedEccentricity, averagedInclination,
            averagedPerigeeArgument, averagedRightAscensionOfTheAscendingNode, averagedMeanAnomaly };
    }

    /**
     * Getter for the averaged semi-major axis.
     * @return semi-major axis
     */
    public double getAveragedSemiMajorAxis() {
        return averagedSemiMajorAxis;
    }

    /**
     * Getter for the averaged eccentricity.
     * @return eccentricity
     */
    public double getAveragedEccentricity() {
        return averagedEccentricity;
    }

    /**
     * Getter for the averaged inclination.
     * @return inclination
     */
    public double getAveragedInclination() {
        return averagedInclination;
    }

    /**
     * Getter for the averaged perigee argument.
     * @return perigee argument.
     */
    public double getAveragedPerigeeArgument() {
        return averagedPerigeeArgument;
    }

    /**
     * Getter for the averaged RAAN.
     * @return RAAN
     */
    public double getAveragedRightAscensionOfTheAscendingNode() {
        return averagedRightAscensionOfTheAscendingNode;
    }

    /**
     * Getter for the averaged mean anomaly.
     * @return mean anomaly
     */
    public double getAveragedMeanAnomaly() {
        return averagedMeanAnomaly;
    }
}

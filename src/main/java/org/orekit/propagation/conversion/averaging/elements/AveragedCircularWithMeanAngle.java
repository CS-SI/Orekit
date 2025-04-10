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
 * Immutable class containing values of averaged circular elements from any applicable theory
 * (with MEAN as {@link org.orekit.orbits.PositionAngleType}).
 *
 * @author Romain Serra
 * @see AveragedOrbitalElements
 * @since 12.1
 */
public class AveragedCircularWithMeanAngle implements AveragedOrbitalElements {

    /** Averaged semi-major axis in arbitrary theory. */
    private final double averagedSemiMajorAxis;
    /** Averaged circular ex in arbitrary theory. */
    private final double averagedCircularEx;
    /** Averaged circular ey in arbitrary theory. */
    private final double averagedCircularEy;
    /** Averaged inclination in arbitrary theory. */
    private final double averagedInclination;
    /** Averaged right ascension of the ascending node in arbitrary theory. */
    private final double averagedRightAscensionOfTheAscendingNode;
    /** Averaged mean latitude argument in arbitrary theory. */
    private final double averagedMeanLatitudeArgument;

    /**
     * Constructor.
     * @param averagedSemiMajorAxis averaged semi-major axis
     * @param averagedCircularEx averaged circular ex
     * @param averagedCircularEy averaged circular ey
     * @param averagedInclination averaged inclination
     * @param averagedRightAscensionOfTheAscendingNode averaged RAAN
     * @param averagedMeanLatitudeArgument averaged mean latitude argument
     */
    public AveragedCircularWithMeanAngle(final double averagedSemiMajorAxis,
                                         final double averagedCircularEx,
                                         final double averagedCircularEy,
                                         final double averagedInclination,
                                         final double averagedRightAscensionOfTheAscendingNode,
                                         final double averagedMeanLatitudeArgument) {
        this.averagedSemiMajorAxis = averagedSemiMajorAxis;
        this.averagedCircularEx = averagedCircularEx;
        this.averagedCircularEy = averagedCircularEy;
        this.averagedInclination = averagedInclination;
        this.averagedRightAscensionOfTheAscendingNode = averagedRightAscensionOfTheAscendingNode;
        this.averagedMeanLatitudeArgument = averagedMeanLatitudeArgument;
    }

    /** {@inheritDoc} */
    @Override
    public double[] toArray() {
        return new double[] { averagedSemiMajorAxis, averagedCircularEx, averagedCircularEy,
            averagedInclination, averagedRightAscensionOfTheAscendingNode, averagedMeanLatitudeArgument };
    }

    /**
     * Getter for averaged semi-major axis.
     * @return semi-major axis.
     */
    public double getAveragedSemiMajorAxis() {
        return averagedSemiMajorAxis;
    }

    /**
     * Getter for averaged circular ex.
     * @return ex
     */
    public double getAveragedCircularEx() {
        return averagedCircularEx;
    }

    /**
     * Getter for averaged circular ey.
     * @return ey
     */
    public double getAveragedCircularEy() {
        return averagedCircularEy;
    }

    /**
     * Getter for averaged inclination.
     * @return inclination
     */
    public double getAveragedInclination() {
        return averagedInclination;
    }

    /**
     * Getter for averaged RAAN.
     * @return RAAN
     */
    public double getAveragedRightAscensionOfTheAscendingNode() {
        return averagedRightAscensionOfTheAscendingNode;
    }

    /**
     * Getter for averaged mean latitude argument.
     * @return mean latitude argument
     */
    public double getAveragedMeanLatitudeArgument() {
        return averagedMeanLatitudeArgument;
    }
}

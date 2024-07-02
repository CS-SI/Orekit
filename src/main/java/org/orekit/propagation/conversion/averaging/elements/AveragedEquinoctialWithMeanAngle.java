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
package org.orekit.propagation.conversion.averaging.elements;

/**
 * Immutable class containing values of averaged equinoctial elements from any applicable theory
 * (with MEAN as {@link org.orekit.orbits.PositionAngleType}).
 *
 * @author Romain Serra
 * @see AveragedOrbitalElements
 * @since 12.1
 */
public class AveragedEquinoctialWithMeanAngle implements AveragedOrbitalElements {

    /** Averaged semi-major axis in arbitrary theory. */
    private final double averagedSemiMajorAxis;
    /** Averaged equinoctial ex in arbitrary theory. */
    private final double averagedEquinoctialEx;
    /** Averaged equinoctial ey in arbitrary theory. */
    private final double averagedEquinoctialEy;
    /** Averaged hx in arbitrary theory. */
    private final double averagedHx;
    /** Averaged hy in arbitrary theory. */
    private final double averagedHy;
    /** Averaged mean longitude argument in arbitrary theory. */
    private final double averagedMeanLongitudeArgument;

    /**
     * Constructor.
     * @param averagedSemiMajorAxis semi-major axis
     * @param averagedEquinoctialEx equinoctial ex
     * @param averagedEquinoctialEy equinoctial ey
     * @param averagedHx hx
     * @param averagedHy hy
     * @param averagedMeanLongitudeArgument mean longitude argument
     */
    public AveragedEquinoctialWithMeanAngle(final double averagedSemiMajorAxis,
                                            final double averagedEquinoctialEx,
                                            final double averagedEquinoctialEy,
                                            final double averagedHx,
                                            final double averagedHy,
                                            final double averagedMeanLongitudeArgument) {
        this.averagedSemiMajorAxis = averagedSemiMajorAxis;
        this.averagedEquinoctialEx = averagedEquinoctialEx;
        this.averagedEquinoctialEy = averagedEquinoctialEy;
        this.averagedHx = averagedHx;
        this.averagedHy = averagedHy;
        this.averagedMeanLongitudeArgument = averagedMeanLongitudeArgument;
    }

    /** {@inheritDoc} */
    @Override
    public double[] toArray() {
        return new double[] { averagedSemiMajorAxis, averagedEquinoctialEx, averagedEquinoctialEy,
            averagedHx, averagedHy, averagedMeanLongitudeArgument };
    }

    /**
     * Getter for the averaged semi-major axis.
     * @return semi-major axis.
     */
    public double getAveragedSemiMajorAxis() {
        return averagedSemiMajorAxis;
    }

    /**
     * Getter for the averaged equinoctial ex.
     * @return ex
     */
    public double getAveragedEquinoctialEx() {
        return averagedEquinoctialEx;
    }

    /**
     * Getter for the averaged equinoctial ey.
     * @return ey
     */
    public double getAveragedEquinoctialEy() {
        return averagedEquinoctialEy;
    }

    /**
     * Getter for the averaged hx.
     * @return hx
     */
    public double getAveragedHx() {
        return averagedHx;
    }

    /**
     * Getter for the averaged hy.
     * @return hy
     */
    public double getAveragedHy() {
        return averagedHy;
    }

    /**
     * Getter for the averaged mean longitude argument.
     * @return mean longitude argument
     */
    public double getAveragedMeanLongitudeArgument() {
        return averagedMeanLongitudeArgument;
    }
}

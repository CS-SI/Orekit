/* Copyright 2022-2026 Romain Serra
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

/**
 * Data container for circular orbital elements.
 * @param a semi-major axis
 * @param ex first component of the eccentricity vector
 * @param ey second component of the eccentricity vector
 * @param i inclination
 * @param raan right ascension of ascending node
 * @param latitudeArgument argument of latitude corresponding to angle type
 * @param positionAngleType angle type
 * @author Romain Serra
 * @see PositionAngleType
 * @since 14.0
 */
public record CircularParameters(double a, double ex, double ey, double i, double raan, double latitudeArgument,
                                 PositionAngleType positionAngleType) {

    /**
     * Builds a new instance with the specified position angle type.
     * @param angleType angle type for the output
     * @return circular elements with the specified position angle type
     */
    public CircularParameters withPositionAngleType(final PositionAngleType angleType) {
        final double convertedArgument = CircularLatitudeArgumentUtility.convertAlpha(positionAngleType, latitudeArgument, ex, ey, angleType);
        return new CircularParameters(a, ex, ey, i, raan, convertedArgument, angleType);
    }

}

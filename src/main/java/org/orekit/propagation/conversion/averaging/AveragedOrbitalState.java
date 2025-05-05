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
package org.orekit.propagation.conversion.averaging;

import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;
import org.orekit.propagation.conversion.averaging.elements.AveragedOrbitalElements;

/**
 * Interface representing averaged orbital elements at a specific instant.
 * Inheritors shall implement a conversion method to transform into an osculating {@link Orbit}.
 *
 * @author Romain Serra
 * @see TimeStamped
 * @see AveragedOrbitalElements
 * @since 12.1
 */
public interface AveragedOrbitalState extends TimeStamped {

    /** {@inheritDoc} */
    @Override
    AbsoluteDate getDate();

    /**
     * Getter for the averaged orbital elements.
     * @return averaged elements
     */
    AveragedOrbitalElements getAveragedElements();

    /**
     * Getter for the central body's gravitational constant.
     * @return gravitational constant
     */
    double getMu();

    /**
     * Getter for the reference frame.
     * @return frame
     */
    Frame getFrame();

    /**
     * Getter for the averaged orbit type.
     * @return orbit type
     */
    OrbitType getOrbitType();

    /**
     * Getter for the averaged position angle.
     * @return position angle type
     */
    PositionAngleType getPositionAngleType();

    /**
     * Convert instance to an osculating orbit.
     * @return osculating orbit
     */
    Orbit toOsculatingOrbit();

}

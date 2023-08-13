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
package org.orekit.bodies;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.propagation.numerical.cr3bp.CR3BPConstants;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;

/**
 * Factory class creating predefined CR3BP system using CR3BPSystem class. For example, Earth-Moon CR3BP
 * System.
 * @author Vincent Mouraux
 * @see CR3BPSystem
 * @since 10.2
 */
public class CR3BPFactory {

    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private CR3BPFactory() {
    }

    /** Get the Sun-Jupiter CR3BP singleton bodies pair.
     * @param date date
     * @param timeScale time scale
     * @return Sun-Jupiter CR3BP system
     */
    @DefaultDataContext
    public static CR3BPSystem getSunJupiterCR3BP(final AbsoluteDate date, final TimeScale timeScale) {
        return getSystem(CelestialBodyFactory.getSun(), CelestialBodyFactory.getJupiter(), CR3BPConstants.getJupiterSemiMajorAxis(date, timeScale));
    }

    /** Get the Sun-Earth CR3BP singleton bodies pair.
     * @param date date
     * @param timeScale time scale
     * @return Sun-Earth CR3BP system
     */
    @DefaultDataContext
    public static CR3BPSystem getSunEarthCR3BP(final AbsoluteDate date, final TimeScale timeScale) {
        return getSystem(CelestialBodyFactory.getSun(), CelestialBodyFactory.getEarthMoonBarycenter(), CR3BPConstants.getEarthMoonBarycenterSemiMajorAxis(date, timeScale));
    }

    /** Get the Earth-Moon CR3BP singleton bodies pair.
     * @return Earth-Moon CR3BP system
     */
    @DefaultDataContext
    public static CR3BPSystem getEarthMoonCR3BP() {
        return getSystem(CelestialBodyFactory.getEarth(), CelestialBodyFactory.getMoon(), CR3BPConstants.getMoonSemiMajorAxis());
    }

    /** Get the corresponding CR3BP System.
     * @param primaryBody Primary Body in the CR3BP System
     * @param secondaryBody Secondary Body in the CR3BP System
     * @param a Semi-Major Axis of the secondary body
     * @return corresponding CR3BP System
     */
    public static CR3BPSystem getSystem(final CelestialBody primaryBody, final CelestialBody secondaryBody, final double a) {
        return new CR3BPSystem(primaryBody, secondaryBody, a);
    }
}

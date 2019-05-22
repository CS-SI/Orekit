/* Copyright 2002-2019 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

/**
 * Factory class creating predefined CR3BP system using CR3BPSystem class. For example, Earth-Moon CR3BP
 * System.
 * @author Vincent Mouraux
 * @see CR3BPSystem
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
     * @return Sun-Jupiter CR3BP system
     */
    public static CR3BPSystem getSunJupiterCR3BP() {
        return getSystem(CelestialBodyFactory.getSun(), CelestialBodyFactory.getJupiter());
    }

    /** Get the Sun-Earth CR3BP singleton bodies pair.
     * @return Sun-Earth CR3BP system
     */
    public static CR3BPSystem getSunEarthCR3BP() {
        return getSystem(CelestialBodyFactory.getSun(), CelestialBodyFactory.getEarth());
    }

    /** Get the Earth-Moon CR3BP singleton bodies pair.
     * @return Earth-Moon CR3BP system
     */
    public static CR3BPSystem getEarthMoonCR3BP() {
        return getSystem(CelestialBodyFactory.getEarth(), CelestialBodyFactory.getMoon());
    }

    /** Get the corresponding CR3BP System.
     * @param primaryBody Primary Body in the CR3BP System
     * @param secondaryBody Secondary Body in the CR3BP System
     * @return corresponding CR3BP System
     */
    public static CR3BPSystem getSystem(final CelestialBody primaryBody, final CelestialBody secondaryBody) {
        return new CR3BPSystem(primaryBody, secondaryBody);
    }
}

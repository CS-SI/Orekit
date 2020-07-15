/* Contributed in the public domain.
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

/**
 * Commonly used celestial bodies. This interface defines methods for obtaining intances
 * of the commonly used celestial bodies.
 *
 * @author Luc Maisonobe
 * @author Evan Ward
 * @see CelestialBodyFactory
 * @since 10.1
 */
public interface CelestialBodies {

    /** Get the solar system barycenter aggregated body.
     * <p>
     * Both the {@link CelestialBody#getInertiallyOrientedFrame() inertially
     * oriented frame} and {@link CelestialBody#getBodyOrientedFrame() body
     * oriented frame} for this aggregated body are aligned with
     * {@link org.orekit.frames.FramesFactory#getICRF() ICRF} (and therefore also
     * {@link org.orekit.frames.FramesFactory#getGCRF() GCRF})
     * </p>
     * @return solar system barycenter aggregated body
     */
    CelestialBody getSolarSystemBarycenter();

    /** Get the Sun singleton body.
     * @return Sun body
     */
    CelestialBody getSun();

    /** Get the Mercury singleton body.
     * @return Sun body
     */
    CelestialBody getMercury();

    /** Get the Venus singleton body.
     * @return Venus body
     */
    CelestialBody getVenus();

    /** Get the Earth-Moon barycenter singleton bodies pair.
     * <p>
     * Both the {@link CelestialBody#getInertiallyOrientedFrame() inertially
     * oriented frame} and {@link CelestialBody#getBodyOrientedFrame() body
     * oriented frame} for this bodies pair are aligned with
     * {@link org.orekit.frames.FramesFactory#getICRF() ICRF} (and therefore also
     * {@link org.orekit.frames.FramesFactory#getGCRF() GCRF})
     * </p>
     * @return Earth-Moon barycenter bodies pair
     */
    CelestialBody getEarthMoonBarycenter();

    /** Get the Earth singleton body.
     * @return Earth body
     */
    CelestialBody getEarth();

    /** Get the Moon singleton body.
     * @return Moon body
     */
    CelestialBody getMoon();

    /** Get the Mars singleton body.
     * @return Mars body
     */
    CelestialBody getMars();

    /** Get the Jupiter singleton body.
     * @return Jupiter body
     */
    CelestialBody getJupiter();

    /** Get the Saturn singleton body.
     * @return Saturn body
     */
    CelestialBody getSaturn();

    /** Get the Uranus singleton body.
     * @return Uranus body
     */
    CelestialBody getUranus();

    /** Get the Neptune singleton body.
     * @return Neptune body
     */
    CelestialBody getNeptune();

    /** Get the Pluto singleton body.
     * @return Pluto body
     */
    CelestialBody getPluto();

    /**
     * Get a celestial body. The names of the common bodies are defined as constants in
     * {@link CelestialBodyFactory}.
     *
     * @param name name of the celestial body
     * @return celestial body
     */
    CelestialBody getBody(String name);

}

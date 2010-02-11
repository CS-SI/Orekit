/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import org.orekit.errors.OrekitException;

/** Factory class for bodies of the solar system.
 * <p>The {@link #getSun() Sun}, the {@link #getMoon() Moon} and the planets
 * (including the Pluto dwarf planet) are provided by this factory. In addition,
 * two important points are provided for convenience: the {@link
 * #getSolarSystemBarycenter() solar system barycenter} and the {@link
 * #getEarthMoonBarycenter() Earth-Moon barycenter}.</p>
 * <p>The underlying body-centered frames are either direct children of {@link
 * org.orekit.frames.FramesFactory#getEME2000() EME2000} (for {@link #getMoon() Moon}
 * and {@link #getEarthMoonBarycenter() Earth-Moon barycenter}) or children from other
 * body-centered frames. For example, the path from EME2000 to
 * Jupiter-centered frame is: EME2000, Earth-Moon barycenter centered,
 * solar system barycenter centered, Jupiter-centered. The defining transforms
 * of these frames are combinations of simple linear {@link
 * org.orekit.frames.Transform#Transform(org.apache.commons.math.geometry.Vector3D,
 * org.apache.commons.math.geometry.Vector3D) translation/velocity} transforms
 * without any rotation. The frame axes are therefore always parallel to
 * {@link org.orekit.frames.FramesFactory#getEME2000() EME2000} frame axes.</p>
 * <p>The position of the bodies provided by this class are interpolated using
 * the JPL DE 405/DE 406 ephemerides.</p>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 * @deprecated since 4.2 replaced by {@link CelestialBodyFactory}
 */
@Deprecated
public class SolarSystemBody {

    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private SolarSystemBody() {
    }

    /** Get the solar system barycenter aggregated body.
     * @return solar system barycenter aggregated body
     * @exception OrekitException if the ephemerides cannot be read
     */
    public static CelestialBody getSolarSystemBarycenter() throws OrekitException {
        return CelestialBodyFactory.getSolarSystemBarycenter();
    }

    /** Get the Sun singleton body.
     * @return Sun body
     * @exception OrekitException if the ephemerides cannot be read
     */
    public static CelestialBody getSun() throws OrekitException {
        return CelestialBodyFactory.getSun();
    }

    /** Get the Mercury singleton body.
     * @return Mercury body
     * @exception OrekitException if the ephemerides cannot be read
     */
    public static CelestialBody getMercury() throws OrekitException {
        return CelestialBodyFactory.getMercury();
    }

    /** Get the Venus singleton body.
     * @return Venus body
     * @exception OrekitException if the ephemerides cannot be read
     */
    public static CelestialBody getVenus() throws OrekitException {
        return CelestialBodyFactory.getVenus();
    }

    /** Get the Earth-Moon barycenter singleton bodies pair.
     * @return Earth-Moon barycenter bodies pair
     * @exception OrekitException if the ephemerides cannot be read
     */
    public static CelestialBody getEarthMoonBarycenter() throws OrekitException {
        return CelestialBodyFactory.getEarthMoonBarycenter();
    }

    /** Get the Earth singleton body.
     * @return Earth body
     * @exception OrekitException if the ephemerides cannot be read
     */
    public static CelestialBody getEarth() throws OrekitException {
        return CelestialBodyFactory.getEarth();
    }

    /** Get the Moon singleton body.
     * @return Moon body
     * @exception OrekitException if the ephemerides cannot be read
     */
    public static CelestialBody getMoon() throws OrekitException {
        return CelestialBodyFactory.getMoon();
    }

    /** Get the Mars singleton body.
     * @return Mars body
     * @exception OrekitException if the ephemerides cannot be read
     */
    public static CelestialBody getMars() throws OrekitException {
        return CelestialBodyFactory.getMars();
    }

    /** Get the Jupiter singleton body.
     * @return Jupiter body
     * @exception OrekitException if the ephemerides cannot be read
     */
    public static CelestialBody getJupiter() throws OrekitException {
        return CelestialBodyFactory.getJupiter();
    }

    /** Get the Saturn singleton body.
     * @return Saturn body
     * @exception OrekitException if the ephemerides cannot be read
     */
    public static CelestialBody getSaturn() throws OrekitException {
        return CelestialBodyFactory.getSaturn();
    }

    /** Get the Uranus singleton body.
     * @return Uranus body
     * @exception OrekitException if the ephemerides cannot be read
     */
    public static CelestialBody getUranus() throws OrekitException {
        return CelestialBodyFactory.getUranus();
    }

    /** Get the Neptune singleton body.
     * @return Neptune body
     * @exception OrekitException if the ephemerides cannot be read
     */
    public static CelestialBody getNeptune() throws OrekitException {
        return CelestialBodyFactory.getNeptune();
    }

    /** Get the Pluto singleton body.
     * @return Pluto body
     * @exception OrekitException if the ephemerides cannot be read
     */
    public static CelestialBody getPluto() throws OrekitException {
        return CelestialBodyFactory.getPluto();
    }

}

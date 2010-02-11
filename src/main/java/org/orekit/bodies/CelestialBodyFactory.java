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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.orekit.data.DataProvidersManager;
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
 */
public class CelestialBodyFactory {

    /** Predefined name for solar system barycenter.
     * @see #getBody(String)
     */
    public static final String SOLAR_SYSTEM_BARYCENTER = "solar system barycenter";

    /** Predefined name for Sun.
     * @see #getBody(String)
     */
    public static final String SUN = "Sun";

    /** Predefined name for Mercury.
     * @see #getBody(String)
     */
    public static final String MERCURY = "Mercury";

    /** Predefined name for Venus.
     * @see #getBody(String)
     */
    public static final String VENUS = "Venus";

    /** Predefined name for Earth-Moon barycenter.
     * @see #getBody(String)
     */
    public static final String EARTH_MOON = "Earth-Moon barycenter";

    /** Predefined name for Earth.
     * @see #getBody(String)
     */
    public static final String EARTH = "Earth";

    /** Predefined name for Moon.
     * @see #getBody(String)
     */
    public static final String MOON = "Moon";

    /** Predefined name for Mars.
     * @see #getBody(String)
     */
    public static final String MARS = "Mars";

    /** Predefined name for Jupiter.
     * @see #getBody(String)
     */
    public static final String JUPITER = "Jupiter";

    /** Predefined name for Saturn.
     * @see #getBody(String)
     */
    public static final String SATURN = "Saturn";

    /** Predefined name for Uranus.
     * @see #getBody(String)
     */
    public static final String URANUS = "Uranus";

    /** Predefined name for Neptune.
     * @see #getBody(String)
     */
    public static final String NEPTUNE = "Neptune";

    /** Predefined name for Pluto.
     * @see #getBody(String)
     */
    public static final String PLUTO = "Pluto";

    /** Celestial body loaders map. */
    private static final Map<String, List<CelestialBodyLoader>> LOADERS_MAP =
        new HashMap<String, List<CelestialBodyLoader>>();

    /** Celestial body map. */
    private static final Map<String, CelestialBody> CELESTIAL_BODIES_MAP =
        new HashMap<String, CelestialBody>();

    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private CelestialBodyFactory() {
    }

    /** Add a loader for celestial bodies.
     * @param name name of the body (may be one of the predefined names or a user-defined name)
     * @param loader custom loader to add for the body
     * @see #addDefaultCelestialBodyLoader(String)
     * @see #clearCelestialBodyLoaders(String)
     * @see #clearCelestialBodyLoaders()
     */
    public static void addCelestialBodyLoader(final String name, final CelestialBodyLoader loader) {
        synchronized (LOADERS_MAP) {
            List<CelestialBodyLoader> loaders = LOADERS_MAP.get(name);
            if (loaders == null) {
                loaders = new ArrayList<CelestialBodyLoader>();
                LOADERS_MAP.put(name, loaders);
            }
            loaders.add(loader);
        }
    }

    /** Add the default loaders for all predefined celestial bodies.
     * @param supportedNames regular expression for supported files names
     * (may be null if the default JPL file names are used)
     * <p>
     * The default loaders look for DE405 or DE406 JPL ephemerides.
     * </p>
     * @see <a href="ftp://ssd.jpl.nasa.gov/pub/eph/planets/unix/de405">DE405 JPL ephemerides</a>
     * @see <a href="ftp://ssd.jpl.nasa.gov/pub/eph/planets/unix/de406">DE406 JPL ephemerides</a>
     * @see #addCelestialBodyLoader(String, CelestialBodyLoader)
     * @see #addDefaultCelestialBodyLoader(String)
     * @see #clearCelestialBodyLoaders(String)
     * @see #clearCelestialBodyLoaders()
     * @exception OrekitException if the header constants cannot be read
     */
    public static void addDefaultCelestialBodyLoader(final String supportedNames)
        throws OrekitException {
        addDefaultCelestialBodyLoader(SOLAR_SYSTEM_BARYCENTER, supportedNames);
        addDefaultCelestialBodyLoader(SUN,                     supportedNames);
        addDefaultCelestialBodyLoader(MERCURY,                 supportedNames);
        addDefaultCelestialBodyLoader(VENUS,                   supportedNames);
        addDefaultCelestialBodyLoader(EARTH_MOON,              supportedNames);
        addDefaultCelestialBodyLoader(EARTH,                   supportedNames);
        addDefaultCelestialBodyLoader(MOON,                    supportedNames);
        addDefaultCelestialBodyLoader(MARS,                    supportedNames);
        addDefaultCelestialBodyLoader(JUPITER,                 supportedNames);
        addDefaultCelestialBodyLoader(SATURN,                  supportedNames);
        addDefaultCelestialBodyLoader(URANUS,                  supportedNames);
        addDefaultCelestialBodyLoader(NEPTUNE,                 supportedNames);
        addDefaultCelestialBodyLoader(PLUTO,                   supportedNames);
    }

    /** Add the default loaders for celestial bodies.
     * @param name name of the body (if not one of the predefined names, the method does nothing)
     * @param supportedNames regular expression for supported files names
     * (may be null if the default JPL file names are used)
     * <p>
     * The default loaders look for DE405 or DE406 JPL ephemerides.
     * </p>
     * @see <a href="ftp://ssd.jpl.nasa.gov/pub/eph/planets/unix/de405">DE405 JPL ephemerides</a>
     * @see <a href="ftp://ssd.jpl.nasa.gov/pub/eph/planets/unix/de406">DE406 JPL ephemerides</a>
     * @see #addCelestialBodyLoader(String, CelestialBodyLoader)
     * @see #addDefaultCelestialBodyLoader(String)
     * @see #clearCelestialBodyLoaders(String)
     * @see #clearCelestialBodyLoaders()
     * @exception OrekitException if the header constants cannot be read
     */
    public static void addDefaultCelestialBodyLoader(final String name, final String supportedNames)
        throws OrekitException {

        CelestialBodyLoader loader = null;
        if (name.equals(SOLAR_SYSTEM_BARYCENTER)) {
            loader =
                new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.SOLAR_SYSTEM_BARYCENTER, null);
        } else if (name.equals(SUN)) {
            loader =
                new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.SUN, null);
        } else if (name.equals(MERCURY)) {
            loader =
                new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.MERCURY, null);
        } else if (name.equals(VENUS)) {
            loader =
                new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.VENUS, null);
        } else if (name.equals(EARTH_MOON)) {
            loader =
                new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.EARTH_MOON, null);
        } else if (name.equals(EARTH)) {
            loader =
                new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.EARTH, null);
        } else if (name.equals(MOON)) {
            loader =
                new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.MOON, null);
        } else if (name.equals(MARS)) {
            loader =
                new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.MARS, null);
        } else if (name.equals(JUPITER)) {
            loader =
                new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.JUPITER, null);
        } else if (name.equals(SATURN)) {
            loader =
                new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.SATURN, null);
        } else if (name.equals(URANUS)) {
            loader =
                new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.URANUS, null);
        } else if (name.equals(NEPTUNE)) {
            loader =
                new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.NEPTUNE, null);
        } else if (name.equals(PLUTO)) {
            loader =
                new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.PLUTO, null);
        }

        if (loader != null) {
            addCelestialBodyLoader(name, loader);
        }

    }

    /** Clear loaders for one celestial body.
     * @param name name of the body
     * @see #addCelestialBodyLoader(String, CelestialBodyLoader)
     * @see #clearCelestialBodyLoaders()
     */
    public static void clearCelestialBodyLoaders(final String name) {
        synchronized (LOADERS_MAP) {
            LOADERS_MAP.remove(name);
        }
    }

    /** Clear loaders for all celestial bodies.
     * @see #addCelestialBodyLoader(String, CelestialBodyLoader)
     * @see #clearCelestialBodyLoaders(String)
     */
    public static void clearCelestialBodyLoaders() {
        synchronized (LOADERS_MAP) {
            LOADERS_MAP.clear();
        }
    }

    /** Get the solar system barycenter aggregated body.
     * @return solar system barycenter aggregated body
     * @exception OrekitException if the celestial body cannot be built
     */
    public static CelestialBody getSolarSystemBarycenter() throws OrekitException {
        return getBody(SOLAR_SYSTEM_BARYCENTER);
    }

    /** Get the Sun singleton body.
     * @return Sun body
     * @exception OrekitException if the celestial body cannot be built
     */
    public static CelestialBody getSun() throws OrekitException {
        return getBody(SUN);
    }

    /** Get the Mercury singleton body.
     * @return Sun body
     * @exception OrekitException if the celestial body cannot be built
     */
    public static CelestialBody getMercury() throws OrekitException {
        return getBody(MERCURY);
    }

    /** Get the Venus singleton body.
     * @return Venus body
     * @exception OrekitException if the celestial body cannot be built
     */
    public static CelestialBody getVenus() throws OrekitException {
        return getBody(VENUS);
    }

    /** Get the Earth-Moon barycenter singleton bodies pair.
     * @return Earth-Moon barycenter bodies pair
     * @exception OrekitException if the celestial body cannot be built
     */
    public static CelestialBody getEarthMoonBarycenter() throws OrekitException {
        return getBody(EARTH_MOON);
    }

    /** Get the Earth singleton body.
     * @return Earth body
     * @exception OrekitException if the celestial body cannot be built
     */
    public static CelestialBody getEarth() throws OrekitException {
        return getBody(EARTH);
    }

    /** Get the Moon singleton body.
     * @return Moon body
     * @exception OrekitException if the celestial body cannot be built
     */
    public static CelestialBody getMoon() throws OrekitException {
        return getBody(MOON);
    }

    /** Get the Mars singleton body.
     * @return Mars body
     * @exception OrekitException if the celestial body cannot be built
     */
    public static CelestialBody getMars() throws OrekitException {
        return getBody(MARS);
    }

    /** Get the Jupiter singleton body.
     * @return Jupiter body
     * @exception OrekitException if the celestial body cannot be built
     */
    public static CelestialBody getJupiter() throws OrekitException {
        return getBody(JUPITER);
    }

    /** Get the Saturn singleton body.
     * @return Saturn body
     * @exception OrekitException if the celestial body cannot be built
     */
    public static CelestialBody getSaturn() throws OrekitException {
        return getBody(SATURN);
    }

    /** Get the Uranus singleton body.
     * @return Uranus body
     * @exception OrekitException if the celestial body cannot be built
     */
    public static CelestialBody getUranus() throws OrekitException {
        return getBody(URANUS);
    }

    /** Get the Neptune singleton body.
     * @return Neptune body
     * @exception OrekitException if the celestial body cannot be built
     */
    public static CelestialBody getNeptune() throws OrekitException {
        return getBody(NEPTUNE);
    }

    /** Get the Pluto singleton body.
     * @return Pluto body
     * @exception OrekitException if the celestial body cannot be built
     */
    public static CelestialBody getPluto() throws OrekitException {
        return getBody(PLUTO);
    }

    /** Get a celestial body.
     * <p>
     * If no {@link CelestialBodyLoader} has been added by calling {@link
     * #addCelestialBodyLoader(String, CelestialBodyLoader)
     * addCelestialBodyLoader} or if {@link #clearCelestialBodyLoaders(String)
     * clearCelestialBodyLoaders} has been called afterwards,
     * the {@link #addDefaultCelestialBodyLoader(String, String)
     * addDefaultCelestialBodyLoader} method will be called automatically with
     * a null second parameter (supported file names).
     * </p>
     * @param name name of the celestial body
     * @return celestial body
     * @exception OrekitException if the celestial body cannot be built
     */
    public static CelestialBody getBody(final String name)
        throws OrekitException {
        synchronized (CELESTIAL_BODIES_MAP) {
            CelestialBody body = CELESTIAL_BODIES_MAP.get(name);
            if (body == null) {
                synchronized (LOADERS_MAP) {
                    List<CelestialBodyLoader> loaders = LOADERS_MAP.get(name);
                    boolean loaded = false;
                    if ((loaders == null) || loaders.isEmpty()) {
                        addDefaultCelestialBodyLoader(name, null);
                        loaders = LOADERS_MAP.get(name);
                    }
                    for (CelestialBodyLoader loader : loaders) {
                        DataProvidersManager.getInstance().feed(loader.getSupportedNames(), loader);
                        if (loader.foundData()) {
                            body   = loader.loadCelestialBody(name);
                            loaded = true;
                            break;
                        }
                    }
                    if (!loaded) {
                        throw new OrekitException("no data loaded for celestial body {0}", name);
                    }

                }

                // save the body
                CELESTIAL_BODIES_MAP.put(name, body);

            }

            return body;

        }
    }

}

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
import org.orekit.data.DataContext;

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
 * org.orekit.frames.Transform#Transform(org.orekit.time.AbsoluteDate,
 * org.hipparchus.geometry.euclidean.threed.Vector3D,
 * org.hipparchus.geometry.euclidean.threed.Vector3D) translation/velocity} transforms
 * without any rotation. The frame axes are therefore always parallel to
 * {@link org.orekit.frames.FramesFactory#getEME2000() EME2000} frame axes.</p>
 * <p>The position of the bodies provided by this class are interpolated using
 * the JPL DE 405/DE 406 ephemerides.</p>
 * @author Luc Maisonobe
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

    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private CelestialBodyFactory() {
    }

    /**
     * Get the instance of {@link CelestialBodies} that is called by the static methods in
     * this class.
     *
     * @return the reference frames used by this factory.
     */
    @DefaultDataContext
    public static LazyLoadedCelestialBodies getCelestialBodies() {
        return DataContext.getDefault().getCelestialBodies();
    }

    /** Add a loader for celestial bodies.
     * @param name name of the body (may be one of the predefined names or a user-defined name)
     * @param loader custom loader to add for the body
     * @see #addDefaultCelestialBodyLoader(String)
     * @see #clearCelestialBodyLoaders(String)
     * @see #clearCelestialBodyLoaders()
     */
    @DefaultDataContext
    public static void addCelestialBodyLoader(final String name,
                                              final CelestialBodyLoader loader) {
        getCelestialBodies().addCelestialBodyLoader(name, loader);
    }

    /** Add the default loaders for all predefined celestial bodies.
     * @param supportedNames regular expression for supported files names
     * (may be null if the default JPL file names are used)
     * <p>
     * The default loaders look for DE405 or DE406 JPL ephemerides.
     * </p>
     * @see <a href="ftp://ssd.jpl.nasa.gov/pub/eph/planets/Linux/de405/">DE405 JPL ephemerides</a>
     * @see <a href="ftp://ssd.jpl.nasa.gov/pub/eph/planets/Linux/de406/">DE406 JPL ephemerides</a>
     * @see #addCelestialBodyLoader(String, CelestialBodyLoader)
     * @see #addDefaultCelestialBodyLoader(String)
     * @see #clearCelestialBodyLoaders(String)
     * @see #clearCelestialBodyLoaders()
     */
    @DefaultDataContext
    public static void addDefaultCelestialBodyLoader(final String supportedNames) {
        getCelestialBodies().addDefaultCelestialBodyLoader(supportedNames);
    }

    /** Add the default loaders for celestial bodies.
     * @param name name of the body (if not one of the predefined names, the method does nothing)
     * @param supportedNames regular expression for supported files names
     * (may be null if the default JPL file names are used)
     * <p>
     * The default loaders look for DE405 or DE406 JPL ephemerides.
     * </p>
     * @see <a href="ftp://ssd.jpl.nasa.gov/pub/eph/planets/Linux/de405/">DE405 JPL ephemerides</a>
     * @see <a href="ftp://ssd.jpl.nasa.gov/pub/eph/planets/Linux/de406/">DE406 JPL ephemerides</a>
     * @see #addCelestialBodyLoader(String, CelestialBodyLoader)
     * @see #addDefaultCelestialBodyLoader(String)
     * @see #clearCelestialBodyLoaders(String)
     * @see #clearCelestialBodyLoaders()
     */
    @DefaultDataContext
    public static void addDefaultCelestialBodyLoader(final String name,
                                                     final String supportedNames) {
        getCelestialBodies().addDefaultCelestialBodyLoader(name, supportedNames);
    }

    /** Clear loaders for one celestial body.
     * <p>
     * Calling this method also clears the celestial body that
     * has been loaded via this {@link CelestialBodyLoader}.
     * </p>
     * @param name name of the body
     * @see #addCelestialBodyLoader(String, CelestialBodyLoader)
     * @see #clearCelestialBodyLoaders()
     * @see #clearCelestialBodyCache(String)
     */
    @DefaultDataContext
    public static void clearCelestialBodyLoaders(final String name) {
        getCelestialBodies().clearCelestialBodyLoaders(name);
    }

    /** Clear loaders for all celestial bodies.
     * <p>
     * Calling this method also clears all loaded celestial bodies.
     * </p>
     * @see #addCelestialBodyLoader(String, CelestialBodyLoader)
     * @see #clearCelestialBodyLoaders(String)
     * @see #clearCelestialBodyCache()
     */
    @DefaultDataContext
    public static void clearCelestialBodyLoaders() {
        getCelestialBodies().clearCelestialBodyLoaders();
    }

    /** Clear the specified celestial body from the internal cache.
     * @param name name of the body
     */
    @DefaultDataContext
    public static void clearCelestialBodyCache(final String name) {
        getCelestialBodies().clearCelestialBodyCache(name);
    }

    /** Clear all loaded celestial bodies.
     * <p>
     * Calling this method will remove all loaded bodies from the internal
     * cache. Subsequent calls to {@link #getBody(String)} or similar methods
     * will result in a reload of the requested body from the configured loader(s).
     * </p>
     */
    @DefaultDataContext
    public static void clearCelestialBodyCache() {
        getCelestialBodies().clearCelestialBodyCache();
    }

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
    @DefaultDataContext
    public static CelestialBody getSolarSystemBarycenter() {
        return getCelestialBodies().getSolarSystemBarycenter();
    }

    /** Get the Sun singleton body.
     * @return Sun body
     */
    @DefaultDataContext
    public static CelestialBody getSun() {
        return getCelestialBodies().getSun();
    }

    /** Get the Mercury singleton body.
     * @return Sun body
     */
    @DefaultDataContext
    public static CelestialBody getMercury() {
        return getCelestialBodies().getMercury();
    }

    /** Get the Venus singleton body.
     * @return Venus body
     */
    @DefaultDataContext
    public static CelestialBody getVenus() {
        return getCelestialBodies().getVenus();
    }

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
    @DefaultDataContext
    public static CelestialBody getEarthMoonBarycenter() {
        return getCelestialBodies().getEarthMoonBarycenter();
    }

    /** Get the Earth singleton body.
     * @return Earth body
     */
    @DefaultDataContext
    public static CelestialBody getEarth() {
        return getCelestialBodies().getEarth();
    }

    /** Get the Moon singleton body.
     * @return Moon body
     */
    @DefaultDataContext
    public static CelestialBody getMoon() {
        return getCelestialBodies().getMoon();
    }

    /** Get the Mars singleton body.
     * @return Mars body
     */
    @DefaultDataContext
    public static CelestialBody getMars() {
        return getCelestialBodies().getMars();
    }

    /** Get the Jupiter singleton body.
     * @return Jupiter body
     */
    @DefaultDataContext
    public static CelestialBody getJupiter() {
        return getCelestialBodies().getJupiter();
    }

    /** Get the Saturn singleton body.
     * @return Saturn body
     */
    @DefaultDataContext
    public static CelestialBody getSaturn() {
        return getCelestialBodies().getSaturn();
    }

    /** Get the Uranus singleton body.
     * @return Uranus body
     */
    @DefaultDataContext
    public static CelestialBody getUranus() {
        return getCelestialBodies().getUranus();
    }

    /** Get the Neptune singleton body.
     * @return Neptune body
     */
    @DefaultDataContext
    public static CelestialBody getNeptune() {
        return getCelestialBodies().getNeptune();
    }

    /** Get the Pluto singleton body.
     * @return Pluto body
     */
    @DefaultDataContext
    public static CelestialBody getPluto() {
        return getCelestialBodies().getPluto();
    }

    /** Get a celestial body.
     * <p>
     * If no {@link CelestialBodyLoader} has been added by calling {@link
     * #addCelestialBodyLoader(String, CelestialBodyLoader)
     * addCelestialBodyLoader} or if {@link #clearCelestialBodyLoaders(String)
     * clearCelestialBodyLoaders} has been called afterwards,
     * the {@link #addDefaultCelestialBodyLoader(String, String)
     * addDefaultCelestialBodyLoader} method will be called automatically,
     * once with the default name for JPL DE ephemerides and once with the
     * default name for IMCCE INPOP files.
     * </p>
     * @param name name of the celestial body
     * @return celestial body
     */
    @DefaultDataContext
    public static CelestialBody getBody(final String name) {
        return getCelestialBodies().getBody(name);
    }

}

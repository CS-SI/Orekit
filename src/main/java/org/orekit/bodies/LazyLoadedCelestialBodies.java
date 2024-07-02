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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.TimeScales;

/**
 * This class lazily loads auxiliary data when it is needed by a requested body. It is
 * designed to match the behavior of {@link CelestialBodyFactory} in Orekit 10.0.
 *
 * @author Luc Maisonobe
 * @author Evan Ward
 * @see CelestialBodyFactory
 * @since 10.1
 */
public class LazyLoadedCelestialBodies implements CelestialBodies {

    /** Supplies the auxiliary data files. */
    private final DataProvidersManager dataProvidersManager;
    /** Provides access to time scales when parsing bodies. */
    private final TimeScales timeScales;
    /** Earth centered frame aligned with ICRF. */
    private final Frame gcrf;
    /** Celestial body loaders map. */
    private final Map<String, List<CelestialBodyLoader>> loadersMap = new HashMap<>();

    /** Celestial body map. */
    private final Map<String, CelestialBody> celestialBodyMap = new HashMap<>();

    /**
     * Create a celestial body factory with the given auxiliary data sources.
     *
     * @param dataProvidersManager supplies JPL ephemerides auxiliary data files.
     * @param timeScales           set of time scales to use when loading bodies.
     * @param gcrf                 Earth centered frame aligned with ICRF.
     */
    public LazyLoadedCelestialBodies(final DataProvidersManager dataProvidersManager,
                                     final TimeScales timeScales,
                                     final Frame gcrf) {
        this.dataProvidersManager = dataProvidersManager;
        this.timeScales = timeScales;
        this.gcrf = gcrf;
    }

    /** Add a loader for celestial bodies.
     * @param name name of the body (may be one of the predefined names or a user-defined name)
     * @param loader custom loader to add for the body
     * @see #addDefaultCelestialBodyLoader(String)
     * @see #clearCelestialBodyLoaders(String)
     * @see #clearCelestialBodyLoaders()
     */
    public void addCelestialBodyLoader(final String name,
                                       final CelestialBodyLoader loader) {
        synchronized (loadersMap) {
            loadersMap.computeIfAbsent(name, k -> new ArrayList<>()).add(loader);
        }
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
    public void addDefaultCelestialBodyLoader(final String supportedNames) {
        addDefaultCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, supportedNames);
        addDefaultCelestialBodyLoader(CelestialBodyFactory.SUN,                     supportedNames);
        addDefaultCelestialBodyLoader(CelestialBodyFactory.MERCURY,                 supportedNames);
        addDefaultCelestialBodyLoader(CelestialBodyFactory.VENUS,                   supportedNames);
        addDefaultCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON,              supportedNames);
        addDefaultCelestialBodyLoader(CelestialBodyFactory.EARTH,                   supportedNames);
        addDefaultCelestialBodyLoader(CelestialBodyFactory.MOON,                    supportedNames);
        addDefaultCelestialBodyLoader(CelestialBodyFactory.MARS,                    supportedNames);
        addDefaultCelestialBodyLoader(CelestialBodyFactory.JUPITER,                 supportedNames);
        addDefaultCelestialBodyLoader(CelestialBodyFactory.SATURN,                  supportedNames);
        addDefaultCelestialBodyLoader(CelestialBodyFactory.URANUS,                  supportedNames);
        addDefaultCelestialBodyLoader(CelestialBodyFactory.NEPTUNE,                 supportedNames);
        addDefaultCelestialBodyLoader(CelestialBodyFactory.PLUTO,                   supportedNames);
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
    public void addDefaultCelestialBodyLoader(final String name,
                                              final String supportedNames) {

        CelestialBodyLoader loader = null;
        if (name.equalsIgnoreCase(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER)) {
            loader =
                    new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.SOLAR_SYSTEM_BARYCENTER, dataProvidersManager, timeScales, gcrf);
        } else if (name.equalsIgnoreCase(CelestialBodyFactory.SUN)) {
            loader =
                    new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.SUN, dataProvidersManager, timeScales, gcrf);
        } else if (name.equalsIgnoreCase(CelestialBodyFactory.MERCURY)) {
            loader =
                    new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.MERCURY, dataProvidersManager, timeScales, gcrf);
        } else if (name.equalsIgnoreCase(CelestialBodyFactory.VENUS)) {
            loader =
                    new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.VENUS, dataProvidersManager, timeScales, gcrf);
        } else if (name.equalsIgnoreCase(CelestialBodyFactory.EARTH_MOON)) {
            loader =
                    new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.EARTH_MOON, dataProvidersManager, timeScales, gcrf);
        } else if (name.equalsIgnoreCase(CelestialBodyFactory.EARTH)) {
            loader =
                    new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.EARTH, dataProvidersManager, timeScales, gcrf);
        } else if (name.equalsIgnoreCase(CelestialBodyFactory.MOON)) {
            loader =
                    new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.MOON, dataProvidersManager, timeScales, gcrf);
        } else if (name.equalsIgnoreCase(CelestialBodyFactory.MARS)) {
            loader =
                    new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.MARS, dataProvidersManager, timeScales, gcrf);
        } else if (name.equalsIgnoreCase(CelestialBodyFactory.JUPITER)) {
            loader =
                    new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.JUPITER, dataProvidersManager, timeScales, gcrf);
        } else if (name.equalsIgnoreCase(CelestialBodyFactory.SATURN)) {
            loader =
                    new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.SATURN, dataProvidersManager, timeScales, gcrf);
        } else if (name.equalsIgnoreCase(CelestialBodyFactory.URANUS)) {
            loader =
                    new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.URANUS, dataProvidersManager, timeScales, gcrf);
        } else if (name.equalsIgnoreCase(CelestialBodyFactory.NEPTUNE)) {
            loader =
                    new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.NEPTUNE, dataProvidersManager, timeScales, gcrf);
        } else if (name.equalsIgnoreCase(CelestialBodyFactory.PLUTO)) {
            loader =
                    new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.PLUTO, dataProvidersManager, timeScales, gcrf);
        }

        if (loader != null) {
            addCelestialBodyLoader(name, loader);
        }

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
    public void clearCelestialBodyLoaders(final String name) {
        // use same synchronization order as in getBody to prevent deadlocks
        synchronized (celestialBodyMap) {
            // take advantage of reentrent synchronization as
            // clearCelestialBodyCache uses the same lock inside
            clearCelestialBodyCache(name);

            synchronized (loadersMap) {
                loadersMap.remove(name);
            }
        }
    }

    /** Clear loaders for all celestial bodies.
     * <p>
     * Calling this method also clears all loaded celestial bodies.
     * </p>
     * @see #addCelestialBodyLoader(String, CelestialBodyLoader)
     * @see #clearCelestialBodyLoaders(String)
     * @see #clearCelestialBodyCache()
     */
    public void clearCelestialBodyLoaders() {
        synchronized (celestialBodyMap) {
            clearCelestialBodyCache();

            synchronized (loadersMap) {
                loadersMap.clear();
            }
        }
    }

    /** Clear the specified celestial body from the internal cache.
     * @param name name of the body
     */
    public void clearCelestialBodyCache(final String name) {
        synchronized (celestialBodyMap) {
            celestialBodyMap.remove(name);
        }
    }

    /** Clear all loaded celestial bodies.
     * <p>
     * Calling this method will remove all loaded bodies from the internal
     * cache. Subsequent calls to {@link #getBody(String)} or similar methods
     * will result in a reload of the requested body from the configured loader(s).
     * </p>
     */
    public void clearCelestialBodyCache() {
        synchronized (celestialBodyMap) {
            celestialBodyMap.clear();
        }
    }

    @Override
    public CelestialBody getSolarSystemBarycenter() {
        return getBody(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER);
    }

    @Override
    public CelestialBody getSun() {
        return getBody(CelestialBodyFactory.SUN);
    }

    @Override
    public CelestialBody getMercury() {
        return getBody(CelestialBodyFactory.MERCURY);
    }

    @Override
    public CelestialBody getVenus() {
        return getBody(CelestialBodyFactory.VENUS);
    }

    @Override
    public CelestialBody getEarthMoonBarycenter() {
        return getBody(CelestialBodyFactory.EARTH_MOON);
    }

    @Override
    public CelestialBody getEarth() {
        return getBody(CelestialBodyFactory.EARTH);
    }

    @Override
    public CelestialBody getMoon() {
        return getBody(CelestialBodyFactory.MOON);
    }

    @Override
    public CelestialBody getMars() {
        return getBody(CelestialBodyFactory.MARS);
    }

    @Override
    public CelestialBody getJupiter() {
        return getBody(CelestialBodyFactory.JUPITER);
    }

    @Override
    public CelestialBody getSaturn() {
        return getBody(CelestialBodyFactory.SATURN);
    }

    @Override
    public CelestialBody getUranus() {
        return getBody(CelestialBodyFactory.URANUS);
    }

    @Override
    public CelestialBody getNeptune() {
        return getBody(CelestialBodyFactory.NEPTUNE);
    }

    @Override
    public CelestialBody getPluto() {
        return getBody(CelestialBodyFactory.PLUTO);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * If no {@link CelestialBodyLoader} has been added by calling {@link
     * #addCelestialBodyLoader(String, CelestialBodyLoader) addCelestialBodyLoader} or if
     * {@link #clearCelestialBodyLoaders(String) clearCelestialBodyLoaders} has been
     * called afterwards, the {@link #addDefaultCelestialBodyLoader(String, String)
     * addDefaultCelestialBodyLoader} method will be called automatically, once with the
     * default name for JPL DE ephemerides and once with the default name for IMCCE INPOP
     * files.
     * </p>
     */
    @Override
    public CelestialBody getBody(final String name) {
        synchronized (celestialBodyMap) {
            CelestialBody body = celestialBodyMap.get(name);
            if (body == null) {
                synchronized (loadersMap) {
                    List<CelestialBodyLoader> loaders = loadersMap.get(name);
                    if (loaders == null || loaders.isEmpty()) {
                        addDefaultCelestialBodyLoader(name, JPLEphemeridesLoader.DEFAULT_DE_SUPPORTED_NAMES);
                        addDefaultCelestialBodyLoader(name, JPLEphemeridesLoader.DEFAULT_INPOP_SUPPORTED_NAMES);
                        loaders = loadersMap.get(name);
                    }
                    OrekitException delayedException = null;
                    for (CelestialBodyLoader loader : loaders) {
                        try {
                            body = loader.loadCelestialBody(name);
                            if (body != null) {
                                break;
                            }
                        } catch (OrekitException oe) {
                            delayedException = oe;
                        }
                    }
                    if (body == null) {
                        throw (delayedException != null) ?
                                delayedException :
                                new OrekitException(OrekitMessages.NO_DATA_LOADED_FOR_CELESTIAL_BODY, name);
                    }

                }

                // save the body
                celestialBodyMap.put(name, body);

            }

            return body;

        }
    }

}

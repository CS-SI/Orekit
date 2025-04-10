/* Copyright 2002-2025 CS GROUP
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
package org.orekit.files.ccsds.definitions;

import java.util.Locale;
import java.util.function.Function;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.CelestialBodies;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataContext;
import org.orekit.frames.FactoryManagedFrame;
import org.orekit.frames.Frame;
import org.orekit.frames.Predefined;

/** Orbit central bodies for which a Celestial body can be created.
 * @author sports
 * @since 6.1
 */
public enum CenterName {
    /** Solar system barycenter aggregated body. */
    SOLAR_SYSTEM_BARYCENTER(CelestialBodies::getSolarSystemBarycenter),

    /** Sun body. */
    SUN(CelestialBodies::getSun),

    /** Mercury body. */
    MERCURY(CelestialBodies::getMercury),

    /** Venus body. */
    VENUS(CelestialBodies::getVenus),

    /** Earth-Moon barycenter bodies pair. */
    EARTH_MOON(CelestialBodies::getEarthMoonBarycenter),

    /** Earth body. */
    EARTH(CelestialBodies::getEarth),

    /** Moon body. */
    MOON(CelestialBodies::getMoon),

    /** Mars body. */
    MARS(CelestialBodies::getMars),

    /** Jupiter body. */
    JUPITER(CelestialBodies::getJupiter),

    /** Saturn body. */
    SATURN(CelestialBodies::getSaturn),

    /** Uranus body. */
    URANUS(CelestialBodies::getUranus),

    /** Neptune body. */
    NEPTUNE(CelestialBodies::getNeptune),

    /** Pluto body. */
    PLUTO(CelestialBodies::getPluto);

    /** Suffix of the name of the inertial frame attached to a planet. */
    private static final String INERTIAL_FRAME_SUFFIX = "/inertial";

    /** Suffix of the name of the rotating frame attached to a planet. */
    private static final String ROTATING_FRAME_SUFFIX = "/rotating";

    /**
     * Standardized locale to use, to ensure files can be exchanged without
     * internationalization issues.
     */
    private static final Locale STANDARDIZED_LOCALE = Locale.US;

    /** Substring common to all ITRF frames. */
    /** Celestial body getter.
     * @return getter for celestial body
     */
    private final transient Function<CelestialBodies, CelestialBody> celestialBodyGetter;

    /** Simple constructor.
     * @param celestialBodyGetter getter for celestial body
     */
    CenterName(final Function<CelestialBodies, CelestialBody> celestialBodyGetter) {
        this.celestialBodyGetter = celestialBodyGetter;
    }

    /**
     * Get the celestial body corresponding to the CCSDS constant.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @return celestial body corresponding to the CCSDS constant
     * @see #getCelestialBody(CelestialBodies)
     */
    @DefaultDataContext
    public CelestialBody getCelestialBody() {
        return getCelestialBody(DataContext.getDefault().getCelestialBodies());
    }

    /**
     * Get the celestial body corresponding to the CCSDS constant.
     *
     * @param celestialBodies the set of celestial bodies to use.
     * @return celestial body corresponding to the CCSDS constant
     * @since 10.1
     */
    public CelestialBody getCelestialBody(final CelestialBodies celestialBodies) {
        return celestialBodyGetter.apply(celestialBodies);
    }

    /**
     * Guess the name of the center of the reference frame.
     *
     * @param frame a reference frame for ephemeris output.
     * @return the string to use in the OEM file to describe the origin of {@code frame}.
     */
    public static String guessCenter(final Frame frame) {
        final String name = frame.getName();
        if (name.endsWith(INERTIAL_FRAME_SUFFIX) || name.endsWith(ROTATING_FRAME_SUFFIX)) {
            return name.substring(0, name.length() - 9).toUpperCase(STANDARDIZED_LOCALE);
        } else if (frame instanceof ModifiedFrame) {
            return ((ModifiedFrame) frame).getCenterName();
        } else if (frame.getName().equals(Predefined.ICRF.getName())) {
            return CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER.toUpperCase(STANDARDIZED_LOCALE);
        } else if (frame.getDepth() == 0 || frame instanceof FactoryManagedFrame) {
            return "EARTH";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * Map an Orekit frame to a CCSDS center.
     *
     * @param frame a reference frame.
     * @return the string to use in the OEM file to describe the origin of {@code frame},
     * or null if no such center can be found
     */
    public static CenterName map(final Frame frame) {
        try {
            return CenterName.valueOf(guessCenter(frame));
        } catch (IllegalArgumentException iae) {
            // we were unable to find a match
            return null;
        }
    }

}

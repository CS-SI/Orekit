/* Copyright 2002-2020 CS GROUP
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
package org.orekit.files.ccsds.utils;

import java.util.function.Function;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.CelestialBodies;
import org.orekit.bodies.CelestialBody;
import org.orekit.data.DataContext;

/** Orbit central bodies for which a Celestial body can be created.
 * @author sports
 * @since 6.1
 */
public enum CenterName {
    /** Solar system barycenter aggregated body. */
    SOLAR_SYSTEM_BARYCENTER(celestialBodies -> celestialBodies.getSolarSystemBarycenter()),

    /** Sun body. */
    SUN(celestialBodies -> celestialBodies.getSun()),

    /** Mercury body. */
    MERCURY(celestialBodies -> celestialBodies.getMercury()),

    /** Venus body. */
    VENUS(celestialBodies -> celestialBodies.getVenus()),

    /** Earth-Moon barycenter bodies pair. */
    EARTH_MOON(celestialBodies -> celestialBodies.getEarthMoonBarycenter()),

    /** Earth body. */
    EARTH(celestialBodies -> celestialBodies.getEarth()),

    /** Moon body. */
    MOON(celestialBodies -> celestialBodies.getMoon()),

    /** Mars body. */
    MARS(celestialBodies -> celestialBodies.getMars()),

    /** Jupiter body. */
    JUPITER(celestialBodies -> celestialBodies.getJupiter()),

    /** Saturn body. */
    SATURN(celestialBodies -> celestialBodies.getSaturn()),

    /** Uranus body. */
    URANUS(celestialBodies -> celestialBodies.getUranus()),

    /** Neptune body. */
    NEPTUNE(celestialBodies -> celestialBodies.getNeptune()),

    /** Pluto body. */
    PLUTO(celestialBodies -> celestialBodies.getPluto());

    /** Celestial body getter.
     * @return getter for celestial body
     */
    private final Function<CelestialBodies, CelestialBody> celestialBodyGetter;

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

}

/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.files.ccsds;

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
    SOLAR_SYSTEM_BARYCENTER {

        /** {@inheritDoc}
         * @param celestialBodies*/
        public CelestialBody getCelestialBody(final CelestialBodies celestialBodies) {
            return celestialBodies.getSolarSystemBarycenter();
        }
    },
    /** Sun body. */
    SUN {

        /** {@inheritDoc}
         * @param celestialBodies*/
        public CelestialBody getCelestialBody(final CelestialBodies celestialBodies) {
            return celestialBodies.getSun();
        }
    },
    /** Mercury body. */
    MERCURY {

        /** {@inheritDoc}
         * @param celestialBodies*/
        public CelestialBody getCelestialBody(final CelestialBodies celestialBodies) {
            return celestialBodies.getMercury();
        }
    },
    /** Venus body. */
    VENUS {

        /** {@inheritDoc}
         * @param celestialBodies*/
        public CelestialBody getCelestialBody(final CelestialBodies celestialBodies) {
            return celestialBodies.getVenus();
        }
    },
    /** Earth-Moon barycenter bodies pair. */
    EARTH_MOON {

        /** {@inheritDoc}
         * @param celestialBodies*/
        public CelestialBody getCelestialBody(final CelestialBodies celestialBodies) {
            return celestialBodies.getEarthMoonBarycenter();
        }
    },
    /** Earth body. */
    EARTH {

        /** {@inheritDoc}
         * @param celestialBodies*/
        public CelestialBody getCelestialBody(final CelestialBodies celestialBodies) {
            return celestialBodies.getEarth();
        }
    },
    /** Moon body. */
    MOON {

        /** {@inheritDoc}
         * @param celestialBodies*/
        public CelestialBody getCelestialBody(final CelestialBodies celestialBodies) {
            return celestialBodies.getMoon();
        }
    },
    /** Mars body. */
    MARS {

        /** {@inheritDoc}
         * @param celestialBodies*/
        public CelestialBody getCelestialBody(final CelestialBodies celestialBodies) {
            return celestialBodies.getMars();
        }
    },
    /** Jupiter body. */
    JUPITER {

        /** {@inheritDoc}
         * @param celestialBodies*/
        public CelestialBody getCelestialBody(final CelestialBodies celestialBodies) {
            return celestialBodies.getJupiter();
        }
    },
    /** Saturn body. */
    SATURN {

        /** {@inheritDoc}
         * @param celestialBodies*/
        public CelestialBody getCelestialBody(final CelestialBodies celestialBodies) {
            return celestialBodies.getSaturn();
        }
    },
    /** Uranus body. */
    URANUS {

        /** {@inheritDoc}
         * @param celestialBodies*/
        public CelestialBody getCelestialBody(final CelestialBodies celestialBodies) {
            return celestialBodies.getUranus();
        }
    },
    /** Neptune body. */
    NEPTUNE {

        /** {@inheritDoc}
         * @param celestialBodies*/
        public CelestialBody getCelestialBody(final CelestialBodies celestialBodies) {
            return celestialBodies.getNeptune();
        }
    },
    /** Pluto body. */
    PLUTO {

        /** {@inheritDoc}
         * @param celestialBodies*/
        public CelestialBody getCelestialBody(final CelestialBodies celestialBodies) {
            return celestialBodies.getPluto();
        }
    };

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
    public abstract CelestialBody getCelestialBody(CelestialBodies celestialBodies);

}

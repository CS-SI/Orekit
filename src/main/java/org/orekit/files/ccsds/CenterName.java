/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.files.ccsds;

import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;

/** Orbit central bodies for which a Celestial body can be created.
 * @author sports
 * @since 6.1
 */
public enum CenterName {
    /** Solar system barycenter aggregated body. */
    SOLAR_SYSTEM_BARYCENTER {

        /** {@inheritDoc} */
        public CelestialBody getCelestialBody()
            throws OrekitException {
            return CelestialBodyFactory.getSolarSystemBarycenter();
        }
    },
    /** Sun body. */
    SUN {

        /** {@inheritDoc} */
        public CelestialBody getCelestialBody()
            throws OrekitException {
            return CelestialBodyFactory.getSun();
        }
    },
    /** Mercury body. */
    MERCURY {

        /** {@inheritDoc} */
        public CelestialBody getCelestialBody()
            throws OrekitException {
            return CelestialBodyFactory.getMercury();
        }
    },
    /** Venus body. */
    VENUS {

        /** {@inheritDoc} */
        public CelestialBody getCelestialBody()
            throws OrekitException {
            return CelestialBodyFactory.getVenus();
        }
    },
    /** Earth-Moon barycenter bodies pair. */
    EARTH_MOON {

        /** {@inheritDoc} */
        public CelestialBody getCelestialBody()
            throws OrekitException {
            return CelestialBodyFactory.getEarthMoonBarycenter();
        }
    },
    /** Earth body. */
    EARTH {

        /** {@inheritDoc} */
        public CelestialBody getCelestialBody()
            throws OrekitException {
            return CelestialBodyFactory.getEarth();
        }
    },
    /** Moon body. */
    MOON {

        /** {@inheritDoc} */
        public CelestialBody getCelestialBody()
            throws OrekitException {
            return CelestialBodyFactory.getMoon();
        }
    },
    /** Mars body. */
    MARS {

        /** {@inheritDoc} */
        public CelestialBody getCelestialBody()
            throws OrekitException {
            return CelestialBodyFactory.getMars();
        }
    },
    /** Jupiter body. */
    JUPITER {

        /** {@inheritDoc} */
        public CelestialBody getCelestialBody()
            throws OrekitException {
            return CelestialBodyFactory.getJupiter();
        }
    },
    /** Saturn body. */
    SATURN {

        /** {@inheritDoc} */
        public CelestialBody getCelestialBody()
            throws OrekitException {
            return CelestialBodyFactory.getSaturn();
        }
    },
    /** Uranus body. */
    URANUS {

        /** {@inheritDoc} */
        public CelestialBody getCelestialBody()
            throws OrekitException {
            return CelestialBodyFactory.getUranus();
        }
    },
    /** Neptune body. */
    NEPTUNE {

        /** {@inheritDoc} */
        public CelestialBody getCelestialBody()
            throws OrekitException {
            return CelestialBodyFactory.getNeptune();
        }
    },
    /** Pluto body. */
    PLUTO {

        /** {@inheritDoc} */
        public CelestialBody getCelestialBody()
            throws OrekitException {
            return CelestialBodyFactory.getPluto();
        }
    };

    /**
     * Get the celestial body corresponding to the CCSDS constant.
     * @return celestial body corresponding to the CCSDS constant
     * @exception OrekitException if the Celestial body cannot be retrieved
     */
    public abstract CelestialBody getCelestialBody()
        throws OrekitException;

}

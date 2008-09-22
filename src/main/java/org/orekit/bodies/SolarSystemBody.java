/* Copyright 2002-2008 CS Communication & Systèmes
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

import java.util.SortedSet;
import java.util.TreeSet;

import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeStamped;
import org.orekit.utils.PVCoordinates;


/** Model for bodies of the solar system.
 * <p>The position of the bodies are interpolated from JPL DE 405 ephemerides.</p>
 * <p>The various constants in this file come from E. M. Standish 1998-08-26
 * memorandum: <a href="ftp://ssd.jpl.nasa.gov/pub/eph/export/DE405/de405iom.ps">JPL
 * Planetary and Lunar Ephemerides, DE405/LE405</a> and from Goddard Space Flight
 * Center <a href="http://nssdc.gsfc.nasa.gov/planetary/planetfact.html">planetary
 * fact sheets</a>.</p>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class SolarSystemBody implements CelestialBody {

    /** Serializable UID. */
    private static final long serialVersionUID = -8420989064528998551L;

    /** Attraction coefficient of the body (m<sup>3</sup>/s<sup>2</sup>). */
    private final double gm;

    /** Body ephemeris. */
    private final SortedSet<TimeStamped> ephemeris;

    /** Body type in DE 405 files. */
    private final DE405FilesLoader.EphemerisType type;

    /** Private constructor for the singletons.
     * @param gm attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
     * @param type body type in DE 405 files
     */
    private SolarSystemBody(final double gm, final DE405FilesLoader.EphemerisType type) {
        this.gm   = gm;
        this.type = type;
        ephemeris = new TreeSet<TimeStamped>(ChronologicalComparator.getInstance());
    }

    /** Get the attraction coefficient of the body.
     * @return attraction coefficient of the body (m<sup>3</sup>/s<sup>2</sup>)
     */
    public double getGM() {
        return gm;
    }

    /** Get the Sun singleton body.
     * @return Sun body
     */
    public static CelestialBody getSun() {
        return SunLazyHolder.INSTANCE;
    }

    /** Get the Mercury singleton body.
     * @return Sun body
     */
    public static CelestialBody getMercury() {
        return MercuryLazyHolder.INSTANCE;
    }

    /** Get the Venus singleton body.
     * @return Venus body
     */
    public static CelestialBody getVenus() {
        return VenusLazyHolder.INSTANCE;
    }

    /** Get the Earth singleton body.
     * @return Earth body
     */
    public static CelestialBody getEarth() {
        return EarthLazyHolder.INSTANCE;
    }

    /** Get the Mars singleton body.
     * @return Mars body
     */
    public static CelestialBody getMars() {
        return MarsLazyHolder.INSTANCE;
    }

    /** Get the Jupiter singleton body.
     * @return Jupiter body
     */
    public static CelestialBody getJupiter() {
        return JupiterLazyHolder.INSTANCE;
    }

    /** Get the Saturn singleton body.
     * @return Saturn body
     */
    public static CelestialBody getSaturn() {
        return SaturnLazyHolder.INSTANCE;
    }

    /** Get the Uranus singleton body.
     * @return Uranus body
     */
    public static CelestialBody getUranus() {
        return UranusLazyHolder.INSTANCE;
    }

    /** Get the Neptune singleton body.
     * @return Neptune body
     */
    public static CelestialBody getNeptune() {
        return NeptuneLazyHolder.INSTANCE;
    }

    /** Get the Pluto singleton body.
     * @return Pluto body
     */
    public static CelestialBody getPluto() {
        return PlutoLazyHolder.INSTANCE;
    }

    /** Get the Moon singleton body.
     * @return Moon body
     */
    public static CelestialBody getMoon() {
        return MoonLazyHolder.INSTANCE;
    }

    /** {@inheritDoc} */
    public PVCoordinates getPVCoordinates(AbsoluteDate date, Frame frame)
        throws OrekitException {
        // TODO Auto-generated method stub
        return null;
    }

    /** Holder for the Sun singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class SunLazyHolder {

        /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
        private static final double GM = 1.32712440017987e20;

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(GM, DE405FilesLoader.EphemerisType.SUN);

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private SunLazyHolder() {
        }

    }

    /** Holder for the Mercury singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class MercuryLazyHolder {

        /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
        private static final double GM = SunLazyHolder.GM / 6023600.0;

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(GM, DE405FilesLoader.EphemerisType.MERCURY);

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private MercuryLazyHolder() {
        }

    }

    /** Holder for the Venus singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class VenusLazyHolder {

        /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
        private static final double GM = SunLazyHolder.GM / 408523.71;

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(GM, DE405FilesLoader.EphemerisType.VENUS);

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private VenusLazyHolder() {
        }

    }

    /** Holder for the Earth singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class EarthLazyHolder {

        /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
        private static final double GM = SunLazyHolder.GM / 332946.050895;

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(GM, null);

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private EarthLazyHolder() {
        }

    }

    /** Holder for the Mars singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class MarsLazyHolder {

        /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
        private static final double GM = SunLazyHolder.GM / 3098708;

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(GM, DE405FilesLoader.EphemerisType.MARS);

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private MarsLazyHolder() {
        }

    }

    /** Holder for the Jupiter singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class JupiterLazyHolder {

        /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
        private static final double GM = SunLazyHolder.GM / 1047.3486;

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(GM, DE405FilesLoader.EphemerisType.JUPITER);

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private JupiterLazyHolder() {
        }

    }

    /** Holder for the Saturn singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class SaturnLazyHolder {

        /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
        private static final double GM = SunLazyHolder.GM / 3497.898;

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(GM, DE405FilesLoader.EphemerisType.SATURN);

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private SaturnLazyHolder() {
        }

    }

    /** Holder for the Uranus singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class UranusLazyHolder {

        /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
        private static final double GM = SunLazyHolder.GM / 22902.98;

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(GM, DE405FilesLoader.EphemerisType.URANUS);

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private UranusLazyHolder() {
        }

    }

    /** Holder for the Neptune singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class NeptuneLazyHolder {

        /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
        private static final double GM = SunLazyHolder.GM / 19412;

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(GM, DE405FilesLoader.EphemerisType.NEPTUNE);

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private NeptuneLazyHolder() {
        }

    }

    /** Holder for the Pluto singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class PlutoLazyHolder {

        /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
        private static final double GM = SunLazyHolder.GM / 135200000;

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(GM, DE405FilesLoader.EphemerisType.PLUTO);

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private PlutoLazyHolder() {
        }

    }

    /** Holder for the Moon singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class MoonLazyHolder {

        /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
        private static final double GM = SunLazyHolder.GM / 27068700.387534;

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(GM, DE405FilesLoader.EphemerisType.MOON);

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private MoonLazyHolder() {
        }

    }

}

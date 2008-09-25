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

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeStamped;
import org.orekit.utils.PVCoordinates;

/** Factory class for bodies of the solar system.
 * <p>The {@link #getSun() Sun}, the {@link #getMoon() Moon} and the planets
 * (including the Pluto dwarf planet) are provided by this factory. In addition,
 * two important points are provided for convenience: the {@link
 * #getSolarSystemBarycenter() solar system barycenter} and the {@link
 * #getEarthMoonBarycenter() Earth-Moon barycenter}.</p>
 * <p>The underlying body-centered frames are either direct children of {@link
 * Frame#getEME2000() EME2000} (for {@link #getMoon() Moon} and {@link
 * #getEarthMoonBarycenter() Earth-Moon barycenter}) or children from other
 * body-centered frames. For example, the path from EME2000 to
 * Jupiter-centered frame is: EME2000, Earth-Moon barycenter centered,
 * solar system barycenter centered, Jupiter-centered. The defining transforms
 * of these frames are combinations of simple linear {@link
 * Transform#Transform(org.apache.commons.math.geometry.Vector3D,
 * org.apache.commons.math.geometry.Vector3D) translation/velocity} transforms
 * without any rotation. The frame axes are therefore always parallel to
 * {@link Frame#getEME2000() EME2000} frame axes.</p>
 * <p>The position of the bodies provided by this class are interpolated using
 * the JPL DE 405 ephemerides. The various constants in this file come from E. M.
 * Standish 1998-08-26 memorandum: <a
 * href="ftp://ssd.jpl.nasa.gov/pub/eph/export/DE405/de405iom.ps">JPL Planetary
 * and Lunar Ephemerides, DE405/LE405</a>.</p>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class SolarSystemBody extends AbstractCelestialBody {

    /** Serializable UID. */
    private static final long serialVersionUID = -4929971459387288203L;

    /** Sun attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private static final double SUN_GM = 1.32712440017987e20;

    /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private static final double MERCURY_GM = SUN_GM / 6023600.0;

    /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private static final double VENUS_GM = SUN_GM / 408523.71;

    /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private static final double EARTH_GM = SUN_GM / 332946.050895;

    /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private static final double MOON_GM = SUN_GM / 27068700.387534;

    /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private static final double MARS_GM = SUN_GM / 3098708;

    /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private static final double JUPITER_GM = SUN_GM / 1047.3486;

    /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private static final double SATURN_GM = SUN_GM / 3497.898;

    /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private static final double URANUS_GM = SUN_GM / 22902.98;

    /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private static final double NEPTUNE_GM = SUN_GM / 19412;

    /** Attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private static final double PLUTO_GM = SUN_GM / 135200000;

    /** Body ephemeris. */
    private final SortedSet<TimeStamped> ephemeris;

    /** Body type in DE 405 files. */
    private final DE405FilesLoader.EphemerisType type;

    /** Current Chebyshev model. */
    private PosVelChebyshev model;

    /** Frame in which ephemeris are defined. */
    private final Frame definingFrame;

    /** Earth-Moon mass ratio. */
    private double earthMoonMassRatio;

    /** Private constructor for the singletons.
     * @param gm attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
     * @param definingFrame frame in which ephemeris are defined
     * @param type DE 405 ephemeris type
     * @param frameName name to use for the body-centered frame
     */
    private SolarSystemBody(final double gm, final Frame definingFrame,
                            final DE405FilesLoader.EphemerisType type,
                            final String frameName) {
        super(gm, frameName, definingFrame);
        this.ephemeris     = new TreeSet<TimeStamped>(ChronologicalComparator.getInstance());
        this.model         = null;
        this.type          = type;
        this.definingFrame = definingFrame;
    }

    /** {@inheritDoc} */
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
        throws OrekitException {

        // get position/velocity in parent frame
        setPVModel(date);
        final PVCoordinates pv = model.getPositionVelocity(date);

        // convert to required frame
        if (frame == definingFrame) {
            return pv;
        } else {
            final Transform transform = definingFrame.getTransformTo(frame, date);
            return transform.transformPVCoordinates(pv);
        }

    }

    /** Get the Earth-Moon mass ratio.
     * @return Earth-Moon mass ratio
     */
    protected double getEarthMoonMassRatio() {
        return earthMoonMassRatio;
    }

    /** Set the position-velocity model covering a specified date.
     * @param date target date
     * @exception OrekitException if current date is not covered by
     * available ephemerides
     */
    private void setPVModel(final AbsoluteDate date)
        throws OrekitException {

        // first quick check: is the current model valid for specified date ?
        if (model != null) {

            if (model.inRange(date)) {
                return;
            }

            // try searching only within the already loaded ephemeris part
            final AbsoluteDate before = new AbsoluteDate(date, -model.getValidityDuration());
            for (final Iterator<TimeStamped> iterator = ephemeris.tailSet(before).iterator();
                 iterator.hasNext();) {
                model = (PosVelChebyshev) iterator.next();
                if (model.inRange(date)) {
                    return;
                }
            }

        }

        // existing ephemeris (if any) is too far from current date
        // load a new part of ephemeris, centered around specified date
        final DE405FilesLoader loader = new DE405FilesLoader(type, date);
        ephemeris.addAll(loader.loadEphemerides());
        earthMoonMassRatio = loader.getEarthMoonMassRatio();
        final AbsoluteDate before = new AbsoluteDate(date, -loader.getChunksDuration());

        // second try, searching newly loaded part designed to bracket date
        for (final Iterator<TimeStamped> iterator = ephemeris.tailSet(before).iterator();
             iterator.hasNext();) {
            model = (PosVelChebyshev) iterator.next();
            if (model.inRange(date)) {
                return;
            }
        }

        // no way, this means we don't have available data for this date
        throw new OrekitException("out of range date for {0} ephemerides: {1}",
                                  new Object[] {
                                      type, date
                                  });

    }

    /** Get the solar system barycenter singleton aggregated body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return solar system barycenter aggregated body
     */
    public static CelestialBody getSolarSystemBarycenter() {
        return SolarSystemBarycenterLazyHolder.INSTANCE;
    }

    /** Get the Sun singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Sun body
     */
    public static CelestialBody getSun() {
        return SunLazyHolder.INSTANCE;
    }

    /** Get the Mercury singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Sun body
     */
    public static CelestialBody getMercury() {
        return MercuryLazyHolder.INSTANCE;
    }

    /** Get the Venus singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Venus body
     */
    public static CelestialBody getVenus() {
        return VenusLazyHolder.INSTANCE;
    }

    /** Get the Earth-Moon barycenter singleton bodies pair.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Earth-Moon barycenter bodies pair
     */
    public static CelestialBody getEarthMoonBarycenter() {
        return EarthMoonBarycenterLazyHolder.INSTANCE;
    }

    /** Get the Earth singleton body.
     * <p>The body-centered frame linked to this instance
     * <em>is</em> the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Earth body
     */
    public static CelestialBody getEarth() {
        return EarthLazyHolder.INSTANCE;
    }

    /** Get the Moon singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Moon body
     */
    public static CelestialBody getMoon() {
        return MoonLazyHolder.INSTANCE;
    }

    /** Get the Mars singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Mars body
     */
    public static CelestialBody getMars() {
        return MarsLazyHolder.INSTANCE;
    }

    /** Get the Jupiter singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Jupiter body
     */
    public static CelestialBody getJupiter() {
        return JupiterLazyHolder.INSTANCE;
    }

    /** Get the Saturn singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Saturn body
     */
    public static CelestialBody getSaturn() {
        return SaturnLazyHolder.INSTANCE;
    }

    /** Get the Uranus singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Uranus body
     */
    public static CelestialBody getUranus() {
        return UranusLazyHolder.INSTANCE;
    }

    /** Get the Neptune singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Neptune body
     */
    public static CelestialBody getNeptune() {
        return NeptuneLazyHolder.INSTANCE;
    }

    /** Get the Pluto singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Pluto body
     */
    public static CelestialBody getPluto() {
        return PlutoLazyHolder.INSTANCE;
    }

    /** Holder for the solar system barycenter singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class SolarSystemBarycenterLazyHolder {

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(SUN_GM + MERCURY_GM + VENUS_GM + EARTH_GM + MOON_GM + MARS_GM +
                                JUPITER_GM + SATURN_GM + URANUS_GM + NEPTUNE_GM + PLUTO_GM,
                                SolarSystemBody.getEarthMoonBarycenter().getFrame(),
                                DE405FilesLoader.EphemerisType.EARTH_MOON,
                                "solar system centered EME2000") {

                /** Serializable UID. */
                private static final long serialVersionUID = 7350102501303428347L;

                /** {@inheritDoc} */
                public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
                    throws OrekitException {
                    // we define solar system barycenter with respect to Earth-Moon barycenter
                    // so we need to revert the vectors provided by the JPL DE 405 ephemerides
                    final PVCoordinates emPV = super.getPVCoordinates(date, frame);
                    return new PVCoordinates(emPV.getPosition().negate(), emPV.getVelocity().negate());
                }

            };

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private SolarSystemBarycenterLazyHolder() {
        }

    }

    /** Holder for the Sun singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class SunLazyHolder {

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(SUN_GM,
                                SolarSystemBody.getSolarSystemBarycenter().getFrame(),
                                DE405FilesLoader.EphemerisType.SUN,
                                "Sun centered EME2000");

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

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(MERCURY_GM,
                                SolarSystemBody.getSolarSystemBarycenter().getFrame(),
                                DE405FilesLoader.EphemerisType.MERCURY,
                                "Mercury centered EME2000");

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

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(VENUS_GM,
                                SolarSystemBody.getSolarSystemBarycenter().getFrame(),
                                DE405FilesLoader.EphemerisType.VENUS,
                                "Venus centered EME2000");

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private VenusLazyHolder() {
        }

    }

    /** Holder for the Earth-Moon barycenter singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class EarthMoonBarycenterLazyHolder {

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(EARTH_GM + MOON_GM, Frame.getEME2000(),
                                DE405FilesLoader.EphemerisType.MOON,
                                "Earth-Moon centered EME2000") {

                /** Serializable UID. */
                private static final long serialVersionUID = -6860799524750318529L;

                /** {@inheritDoc} */
                public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
                    throws OrekitException {
                    // we define Earth-Moon barycenter with respect to Earth center so we need
                    // to apply a scale factor to the Moon vectors provided by the JPL DE 405 ephemerides
                    final PVCoordinates moonPV = super.getPVCoordinates(date, frame);

                    // since we have computed moonPV, we know the ephemeris has been read
                    // so now we know the Earth-Moon ratio is available
                    final double scale = 1.0 / (1.0 + getEarthMoonMassRatio());

                    return new PVCoordinates(scale, moonPV);
                }

            };

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private EarthMoonBarycenterLazyHolder() {
        }

    }

    /** Holder for the Earth singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class EarthLazyHolder {

        /** Unique instance. */
        public static final CelestialBody INSTANCE =
            new AbstractCelestialBody(EARTH_GM, Frame.getEME2000()) {

                /** Serializable UID. */
                private static final long serialVersionUID = -2542177517458975694L;

                /** {@inheritDoc} */
                public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
                    throws OrekitException {

                    // specific implementation for Earth:
                    // the Earth is always exactly at the origin of its own EME2000 frame
                    PVCoordinates pv = PVCoordinates.ZERO;
                    if (frame != getFrame()) {
                        pv = getFrame().getTransformTo(frame, date).transformPVCoordinates(pv);
                    }
                    return pv;

                }

            };

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private EarthLazyHolder() {
        }

    }

    /** Holder for the Moon singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class MoonLazyHolder {

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(MOON_GM,
                                SolarSystemBody.getEarth().getFrame(),
                                DE405FilesLoader.EphemerisType.MOON,
                                "Moon centered EME2000");

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private MoonLazyHolder() {
        }

    }

    /** Holder for the Mars singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class MarsLazyHolder {

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(MARS_GM,
                                SolarSystemBody.getSolarSystemBarycenter().getFrame(),
                                DE405FilesLoader.EphemerisType.MARS,
                                "Mars centered EME2000");

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

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(JUPITER_GM,
                                SolarSystemBody.getSolarSystemBarycenter().getFrame(),
                                DE405FilesLoader.EphemerisType.JUPITER,
                                "Jupiter centered EME2000");

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

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(SATURN_GM,
                                SolarSystemBody.getSolarSystemBarycenter().getFrame(),
                                DE405FilesLoader.EphemerisType.SATURN,
                                "Saturn centered EME2000");

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

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(URANUS_GM,
                                SolarSystemBody.getSolarSystemBarycenter().getFrame(),
                                DE405FilesLoader.EphemerisType.URANUS,
                                "Uranus centered EME2000");

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

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(NEPTUNE_GM,
                                SolarSystemBody.getSolarSystemBarycenter().getFrame(),
                                DE405FilesLoader.EphemerisType.NEPTUNE,
                                "Neptune centered EME2000");

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

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE =
            new SolarSystemBody(PLUTO_GM,
                                SolarSystemBody.getSolarSystemBarycenter().getFrame(),
                                DE405FilesLoader.EphemerisType.PLUTO,
                                "Pluto centered EME2000");

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private PlutoLazyHolder() {
        }

    }

}

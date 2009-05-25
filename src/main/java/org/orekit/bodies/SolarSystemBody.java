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
 * the JPL DE 405/DE 406 ephemerides.</p>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class SolarSystemBody extends AbstractCelestialBody {

    /** Serializable UID. */
    private static final long serialVersionUID = -4929971459387288203L;

    /** Body ephemeris. */
    private final SortedSet<TimeStamped> ephemeris;

    /** Body type in DE 405 files. */
    private final JPLEphemeridesLoader.EphemerisType type;

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
                            final JPLEphemeridesLoader.EphemerisType type,
                            final String frameName) {
        super(gm, frameName, definingFrame);
        this.ephemeris     = new TreeSet<TimeStamped>(ChronologicalComparator.getInstance());
        this.model         = null;
        this.type          = type;
        this.definingFrame = definingFrame;
    }

    /** Private constructor for the singletons.
     * @param definingFrame frame in which ephemeris are defined
     * @param type DE 405 ephemeris type
     * @param frameName name to use for the body-centered frame
     * @exception OrekitException if the header constants cannot be read
     */
    private SolarSystemBody(final Frame definingFrame,
                            final JPLEphemeridesLoader.EphemerisType type,
                            final String frameName)
        throws OrekitException {
        this(JPLEphemeridesLoader.getGravitationalCoefficient(type), definingFrame, type, frameName);
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
        final JPLEphemeridesLoader loader = new JPLEphemeridesLoader(type, date);
        ephemeris.addAll(loader.loadEphemerides());
        earthMoonMassRatio = JPLEphemeridesLoader.getEarthMoonMassRatio();
        final AbsoluteDate before = new AbsoluteDate(date, -loader.getMaxChunksDuration());

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
                                  type, date);

    }

    /** Get the solar system barycenter singleton aggregated body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return solar system barycenter aggregated body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getSolarSystemBarycenter()
        throws OrekitException {
        if (SolarSystemBarycenterLazyHolder.OREKIT_EXCEPTION != null) {
            throw SolarSystemBarycenterLazyHolder.OREKIT_EXCEPTION;
        }
        return SolarSystemBarycenterLazyHolder.INSTANCE;
    }

    /** Get the Sun singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Sun body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getSun()
        throws OrekitException {
        if (SunLazyHolder.OREKIT_EXCEPTION != null) {
            throw SunLazyHolder.OREKIT_EXCEPTION;
        }
        return SunLazyHolder.INSTANCE;
    }

    /** Get the Mercury singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Sun body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getMercury()
        throws OrekitException {
        if (MercuryLazyHolder.OREKIT_EXCEPTION != null) {
            throw MercuryLazyHolder.OREKIT_EXCEPTION;
        }
        return MercuryLazyHolder.INSTANCE;
    }

    /** Get the Venus singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Venus body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getVenus()
        throws OrekitException {
        if (VenusLazyHolder.OREKIT_EXCEPTION != null) {
            throw VenusLazyHolder.OREKIT_EXCEPTION;
        }
        return VenusLazyHolder.INSTANCE;
    }

    /** Get the Earth-Moon barycenter singleton bodies pair.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Earth-Moon barycenter bodies pair
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getEarthMoonBarycenter()
        throws OrekitException {
        if (EarthMoonBarycenterLazyHolder.OREKIT_EXCEPTION != null) {
            throw EarthMoonBarycenterLazyHolder.OREKIT_EXCEPTION;
        }
        return EarthMoonBarycenterLazyHolder.INSTANCE;
    }

    /** Get the Earth singleton body.
     * <p>The body-centered frame linked to this instance
     * <em>is</em> the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Earth body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getEarth()
        throws OrekitException {
        if (EarthLazyHolder.OREKIT_EXCEPTION != null) {
            throw EarthLazyHolder.OREKIT_EXCEPTION;
        }
        return EarthLazyHolder.INSTANCE;
    }

    /** Get the Moon singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Moon body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getMoon()
        throws OrekitException {
        if (MoonLazyHolder.OREKIT_EXCEPTION != null) {
            throw MoonLazyHolder.OREKIT_EXCEPTION;
        }
        return MoonLazyHolder.INSTANCE;
    }

    /** Get the Mars singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Mars body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getMars()
        throws OrekitException {
        if (MarsLazyHolder.OREKIT_EXCEPTION != null) {
            throw MarsLazyHolder.OREKIT_EXCEPTION;
        }
        return MarsLazyHolder.INSTANCE;
    }

    /** Get the Jupiter singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Jupiter body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getJupiter()
        throws OrekitException {
        if (JupiterLazyHolder.OREKIT_EXCEPTION != null) {
            throw JupiterLazyHolder.OREKIT_EXCEPTION;
        }
        return JupiterLazyHolder.INSTANCE;
    }

    /** Get the Saturn singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Saturn body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getSaturn()
        throws OrekitException {
        if (SaturnLazyHolder.OREKIT_EXCEPTION != null) {
            throw SaturnLazyHolder.OREKIT_EXCEPTION;
        }
        return SaturnLazyHolder.INSTANCE;
    }

    /** Get the Uranus singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Uranus body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getUranus()
        throws OrekitException {
        if (UranusLazyHolder.OREKIT_EXCEPTION != null) {
            throw UranusLazyHolder.OREKIT_EXCEPTION;
        }
        return UranusLazyHolder.INSTANCE;
    }

    /** Get the Neptune singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Neptune body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getNeptune()
        throws OrekitException {
        if (NeptuneLazyHolder.OREKIT_EXCEPTION != null) {
            throw NeptuneLazyHolder.OREKIT_EXCEPTION;
        }
        return NeptuneLazyHolder.INSTANCE;
    }

    /** Get the Pluto singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Pluto body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getPluto()
        throws OrekitException {
        if (PlutoLazyHolder.OREKIT_EXCEPTION != null) {
            throw PlutoLazyHolder.OREKIT_EXCEPTION;
        }
        return PlutoLazyHolder.INSTANCE;
    }

    /** Holder for the solar system barycenter singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class SolarSystemBarycenterLazyHolder {

        /** Unique instance. */
        public static final SolarSystemBody INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            SolarSystemBody tmpBody      = null;
            OrekitException tmpException = null;
            try {
                final double gmSum =
                    JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.SUN)        +
                    JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MERCURY)    +
                    JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.VENUS)      +
                    JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.EARTH_MOON) +
                    JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MARS)       +
                    JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.JUPITER)    +
                    JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.SATURN)     +
                    JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.URANUS)     +
                    JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.NEPTUNE)    +
                    JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.PLUTO);
                tmpBody = new SolarSystemBody(gmSum,
                                              SolarSystemBody.getEarthMoonBarycenter().getFrame(),
                                              JPLEphemeridesLoader.EphemerisType.EARTH_MOON,
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
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpBody;
            OREKIT_EXCEPTION = tmpException;
        }

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
        private static final SolarSystemBody INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            SolarSystemBody tmpBody      = null;
            OrekitException tmpException = null;
            try {
                tmpBody = new SolarSystemBody(SolarSystemBody.getSolarSystemBarycenter().getFrame(),
                                              JPLEphemeridesLoader.EphemerisType.SUN,
                                              "Sun centered EME2000");
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpBody;
            OREKIT_EXCEPTION = tmpException;
        }

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
        private static final SolarSystemBody INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            SolarSystemBody tmpBody      = null;
            OrekitException tmpException = null;
            try {
                tmpBody = new SolarSystemBody(SolarSystemBody.getSolarSystemBarycenter().getFrame(),
                                              JPLEphemeridesLoader.EphemerisType.MERCURY,
                                              "Mercury centered EME2000");
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpBody;
            OREKIT_EXCEPTION = tmpException;
        }

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
        private static final SolarSystemBody INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            SolarSystemBody tmpBody      = null;
            OrekitException tmpException = null;
            try {
                tmpBody = new SolarSystemBody(SolarSystemBody.getSolarSystemBarycenter().getFrame(),
                                              JPLEphemeridesLoader.EphemerisType.VENUS,
                                              "Venus centered EME2000");
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpBody;
            OREKIT_EXCEPTION = tmpException;
        }

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
        private static final SolarSystemBody INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            SolarSystemBody tmpBody      = null;
            OrekitException tmpException = null;
            try {
                final double moonGM  =
                    JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MOON);
                final double earthGM =
                    JPLEphemeridesLoader.getEarthMoonMassRatio() * moonGM;
                tmpBody = new SolarSystemBody(earthGM + moonGM, Frame.getEME2000(),
                                              JPLEphemeridesLoader.EphemerisType.MOON,
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
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpBody;
            OREKIT_EXCEPTION = tmpException;
        }

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
        private static final CelestialBody INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            CelestialBody tmpBody        = null;
            OrekitException tmpException = null;
            try {
                final double moonGM  =
                    JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MOON);
                final double earthGM =
                    JPLEphemeridesLoader.getEarthMoonMassRatio() * moonGM;
                tmpBody = new AbstractCelestialBody(earthGM, Frame.getEME2000()) {

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
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpBody;
            OREKIT_EXCEPTION = tmpException;

        }

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
        private static final SolarSystemBody INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            SolarSystemBody tmpBody      = null;
            OrekitException tmpException = null;
            try {
                tmpBody = new SolarSystemBody(SolarSystemBody.getEarth().getFrame(),
                                              JPLEphemeridesLoader.EphemerisType.MOON,
                                              "Moon centered EME2000");
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpBody;
            OREKIT_EXCEPTION = tmpException;
        }

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
        private static final SolarSystemBody INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            SolarSystemBody tmpBody      = null;
            OrekitException tmpException = null;
            try {
                tmpBody = new SolarSystemBody(SolarSystemBody.getSolarSystemBarycenter().getFrame(),
                                              JPLEphemeridesLoader.EphemerisType.MARS,
                                              "Mars centered EME2000");
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpBody;
            OREKIT_EXCEPTION = tmpException;
        }

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
        private static final SolarSystemBody INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            SolarSystemBody tmpBody      = null;
            OrekitException tmpException = null;
            try {
                tmpBody = new SolarSystemBody(SolarSystemBody.getSolarSystemBarycenter().getFrame(),
                                              JPLEphemeridesLoader.EphemerisType.JUPITER,
                                              "Jupiter centered EME2000");
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpBody;
            OREKIT_EXCEPTION = tmpException;
        }

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
        private static final SolarSystemBody INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            SolarSystemBody tmpBody      = null;
            OrekitException tmpException = null;
            try {
                tmpBody = new SolarSystemBody(SolarSystemBody.getSolarSystemBarycenter().getFrame(),
                                              JPLEphemeridesLoader.EphemerisType.SATURN,
                                              "Saturn centered EME2000");
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpBody;
            OREKIT_EXCEPTION = tmpException;
        }

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
        private static final SolarSystemBody INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            SolarSystemBody tmpBody      = null;
            OrekitException tmpException = null;
            try {
                tmpBody = new SolarSystemBody(SolarSystemBody.getSolarSystemBarycenter().getFrame(),
                                              JPLEphemeridesLoader.EphemerisType.URANUS,
                                              "Uranus centered EME2000");
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpBody;
            OREKIT_EXCEPTION = tmpException;
        }

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
        /** Unique instance. */
        private static final SolarSystemBody INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            SolarSystemBody tmpBody      = null;
            OrekitException tmpException = null;
            try {
                tmpBody = new SolarSystemBody(SolarSystemBody.getSolarSystemBarycenter().getFrame(),
                                              JPLEphemeridesLoader.EphemerisType.NEPTUNE,
                                              "Neptune centered EME2000");
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpBody;
            OREKIT_EXCEPTION = tmpException;
        }

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
        private static final SolarSystemBody INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            SolarSystemBody tmpBody      = null;
            OrekitException tmpException = null;
            try {
                tmpBody = new SolarSystemBody(SolarSystemBody.getSolarSystemBarycenter().getFrame(),
                                              JPLEphemeridesLoader.EphemerisType.PLUTO,
                                              "Pluto centered EME2000");
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpBody;
            OREKIT_EXCEPTION = tmpException;
        }

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private PlutoLazyHolder() {
        }

    }

}

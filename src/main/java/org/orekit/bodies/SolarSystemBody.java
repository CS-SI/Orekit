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
import org.orekit.frames.FramesFactory;
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

    /** Solar system barycenter. */
    private static CelestialBody solarSystemBarycenter = null;

    /** Sun. */
    private static CelestialBody sun;

    /** Mercury. */
    private static CelestialBody mercury;

    /** Venus. */
    private static CelestialBody venus;

    /** Earth-Moon barycenter. */
    private static CelestialBody earthMoonBarycenter;

    /** Earth. */
    private static CelestialBody earth;

    /** Moon. */
    private static CelestialBody moon;

    /** Mars. */
    private static CelestialBody mars;

    /** Jupiter. */
    private static CelestialBody jupiter;

    /** Saturn. */
    private static CelestialBody saturn;

    /** Uranus. */
    private static CelestialBody uranus;

    /** Neptune. */
    private static CelestialBody neptune;

    /** Pluto. */
    private static CelestialBody pluto;

    /** regular expression for supported files names (may be null). */
    private final String supportedNames;

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
     * @param supportedNames regular expression for supported files names (may be null)
     * @param gm attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
     * @param definingFrame frame in which ephemeris are defined
     * @param type DE 405 ephemeris type
     * @param frameName name to use for the body-centered frame
     */
    private SolarSystemBody(final String supportedNames,
                            final double gm, final Frame definingFrame,
                            final JPLEphemeridesLoader.EphemerisType type,
                            final String frameName) {
        super(gm, frameName, definingFrame);
        this.supportedNames = supportedNames;
        this.ephemeris      = new TreeSet<TimeStamped>(new ChronologicalComparator());
        this.model          = null;
        this.type           = type;
        this.definingFrame  = definingFrame;
    }

    /** Private constructor for the singletons.
     * @param supportedNames regular expression for supported files names (may be null)
     * @param definingFrame frame in which ephemeris are defined
     * @param type DE 405 ephemeris type
     * @param frameName name to use for the body-centered frame
     * @exception OrekitException if the header constants cannot be read
     */
    private SolarSystemBody(final String supportedNames,
                            final Frame definingFrame,
                            final JPLEphemeridesLoader.EphemerisType type,
                            final String frameName)
        throws OrekitException {
        this(supportedNames,
             new JPLEphemeridesLoader(supportedNames, type, null).getLoadedGravitationalCoefficient(type),
             definingFrame, type, frameName);
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
        final JPLEphemeridesLoader loader = new JPLEphemeridesLoader(supportedNames, type, date);
        ephemeris.addAll(loader.loadEphemerides());
        earthMoonMassRatio = loader.getLoadedEarthMoonMassRatio();
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

    /** Get the solar system barycenter aggregated body.
     * <p>Calling this method is equivalent to call {@link
     * #getSolarSystemBarycenter(String) getSolarSystemBarycenter(null)}.</p>
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return solar system barycenter aggregated body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getSolarSystemBarycenter()
        throws OrekitException {
        return getSolarSystemBarycenter(null);
    }

    /** Get the solar system barycenter aggregated body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @param supportedNames regular expression for supported files names (may be null)
     * @return solar system barycenter aggregated body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getSolarSystemBarycenter(final String supportedNames)
        throws OrekitException {
        synchronized (CelestialBody.class) {
            if (solarSystemBarycenter == null) {

                final JPLEphemeridesLoader loader =
                    new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.SUN, null);

                final double gmSum =
                    loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.SUN)        +
                    loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MERCURY)    +
                    loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.VENUS)      +
                    loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.EARTH_MOON) +
                    loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MARS)       +
                    loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.JUPITER)    +
                    loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.SATURN)     +
                    loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.URANUS)     +
                    loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.NEPTUNE)    +
                    loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.PLUTO);
                solarSystemBarycenter = new SolarSystemBody(supportedNames, gmSum,
                                                            getEarthMoonBarycenter(supportedNames).getFrame(),
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
            }

            return solarSystemBarycenter;

        }
    }

    /** Get the Sun singleton body.
     * <p>Calling this method is equivalent to call {@link
     * #getSun(String) getSun(null)}.</p>
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Sun body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getSun()
        throws OrekitException {
        return getSun(null);
    }

    /** Get the Sun singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @param supportedNames regular expression for supported files names (may be null)
     * @return Sun body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getSun(final String supportedNames)
        throws OrekitException {
        synchronized (CelestialBody.class) {

            if (sun == null) {
                sun = new SolarSystemBody(supportedNames,
                                          getSolarSystemBarycenter(supportedNames).getFrame(),
                                          JPLEphemeridesLoader.EphemerisType.SUN,
                                          "Sun centered EME2000");
            }

            return sun;

        }
    }

    /** Get the Mercury singleton body.
     * <p>Calling this method is equivalent to call {@link
     * #getMercury(String) getMercury(null)}.</p>
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Sun body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getMercury()
        throws OrekitException {
        return getMercury(null);
    }

    /** Get the Mercury singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @param supportedNames regular expression for supported files names (may be null)
     * @return Sun body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getMercury(final String supportedNames)
        throws OrekitException {
        synchronized (CelestialBody.class) {

            if (mercury == null) {
                mercury = new SolarSystemBody(supportedNames,
                                              getSolarSystemBarycenter(supportedNames).getFrame(),
                                              JPLEphemeridesLoader.EphemerisType.MERCURY,
                                              "Mercury centered EME2000");
            }

            return mercury;

        }
    }

    /** Get the Venus singleton body.
     * <p>Calling this method is equivalent to call {@link
     * #getVenus(String) getVenus(null)}.</p>
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Venus body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getVenus()
        throws OrekitException {
        return getVenus(null);
    }

    /** Get the Venus singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @param supportedNames regular expression for supported files names (may be null)
     * @return Venus body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getVenus(final String supportedNames)
        throws OrekitException {
        synchronized (CelestialBody.class) {

            if (venus == null) {
                venus = new SolarSystemBody(supportedNames,
                                            getSolarSystemBarycenter(supportedNames).getFrame(),
                                            JPLEphemeridesLoader.EphemerisType.VENUS,
                                            "Venus centered EME2000");
            }

            return venus;

        }
    }

    /** Get the Earth-Moon barycenter singleton bodies pair.
     * <p>Calling this method is equivalent to call {@link
     * #getEarthMoonBarycenter(String) getEarthMoonBarycenter(null)}.</p>
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Earth-Moon barycenter bodies pair
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getEarthMoonBarycenter()
        throws OrekitException {
        return getEarthMoonBarycenter(null);
    }

    /** Get the Earth-Moon barycenter singleton bodies pair.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @param supportedNames regular expression for supported files names (may be null)
     * @return Earth-Moon barycenter bodies pair
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getEarthMoonBarycenter(final String supportedNames)
        throws OrekitException {
        synchronized (CelestialBody.class) {
            if (earthMoonBarycenter == null) {
                final JPLEphemeridesLoader loader =
                    new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.MOON, null);
                final double moonGM  =
                    loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MOON);
                final double earthGM =
                    loader.getLoadedEarthMoonMassRatio() * moonGM;
                earthMoonBarycenter = new SolarSystemBody(supportedNames, earthGM + moonGM,
                                                          FramesFactory.getEME2000(),
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
            }

            return earthMoonBarycenter;

        }

    }

    /** Get the Earth singleton body.
     * <p>Calling this method is equivalent to call {@link
     * #getEarth(String) getEarth(null)}.</p>
     * <p>The body-centered frame linked to this instance
     * <em>is</em> the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Earth body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getEarth()
        throws OrekitException {
        return getEarth(null);
    }

    /** Get the Earth singleton body.
     * <p>The body-centered frame linked to this instance
     * <em>is</em> the {@link Frame#getEME2000() EME2000} frame.</p>
     * @param supportedNames regular expression for supported files names (may be null)
     * @return Earth body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getEarth(final String supportedNames)
        throws OrekitException {
        synchronized (CelestialBody.class) {

            if (earth == null) {
                final JPLEphemeridesLoader loader =
                    new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.MOON, null);
                final double moonGM  =
                    loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MOON);
                final double earthGM =
                    loader.getLoadedEarthMoonMassRatio() * moonGM;
                earth = new AbstractCelestialBody(earthGM, FramesFactory.getEME2000()) {

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
            }

            return earth;

        }
    }

    /** Get the Moon singleton body.
     * <p>Calling this method is equivalent to call {@link
     * #getMoon(String) getMoon(null)}.</p>
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Moon body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getMoon()
        throws OrekitException {
        return getMoon(null);
    }

    /** Get the Moon singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @param supportedNames regular expression for supported files names (may be null)
     * @return Moon body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getMoon(final String supportedNames)
        throws OrekitException {
        synchronized (CelestialBody.class) {

            if (moon == null) {
                moon = new SolarSystemBody(supportedNames,
                                           getEarth(supportedNames).getFrame(),
                                           JPLEphemeridesLoader.EphemerisType.MOON,
                                           "Moon centered EME2000");
            }

            return moon;

        }

    }

    /** Get the Mars singleton body.
     * <p>Calling this method is equivalent to call {@link
     * #getMars(String) getMars(null)}.</p>
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Mars body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getMars()
        throws OrekitException {
        return getMars(null);
    }

    /** Get the Mars singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @param supportedNames regular expression for supported files names (may be null)
     * @return Mars body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getMars(final String supportedNames)
        throws OrekitException {
        synchronized (CelestialBody.class) {

            if (mars == null) {
                mars = new SolarSystemBody(supportedNames,
                                           getSolarSystemBarycenter(supportedNames).getFrame(),
                                           JPLEphemeridesLoader.EphemerisType.MARS,
                                           "Mars centered EME2000");
            }

            return mars;

        }
    }

    /** Get the Jupiter singleton body.
     * <p>Calling this method is equivalent to call {@link
     * #getJupiter(String) getJupiter(null)}.</p>
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Jupiter body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getJupiter()
        throws OrekitException {
        return getJupiter(null);
    }

    /** Get the Jupiter singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @param supportedNames regular expression for supported files names (may be null)
     * @return Jupiter body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getJupiter(final String supportedNames)
        throws OrekitException {
        synchronized (CelestialBody.class) {

            if (jupiter == null) {
                jupiter = new SolarSystemBody(supportedNames,
                                              getSolarSystemBarycenter(supportedNames).getFrame(),
                                              JPLEphemeridesLoader.EphemerisType.JUPITER,
                                              "Jupiter centered EME2000");
            }

            return jupiter;

        }
    }

    /** Get the Saturn singleton body.
     * <p>Calling this method is equivalent to call {@link
     * #getSaturn(String) getSaturn(null)}.</p>
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Saturn body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getSaturn()
        throws OrekitException {
        return getSaturn(null);
    }

    /** Get the Saturn singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @param supportedNames regular expression for supported files names (may be null)
     * @return Saturn body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getSaturn(final String supportedNames)
        throws OrekitException {
        synchronized (CelestialBody.class) {

            if (saturn == null) {
                saturn = new SolarSystemBody(supportedNames,
                                             getSolarSystemBarycenter(supportedNames).getFrame(),
                                             JPLEphemeridesLoader.EphemerisType.SATURN,
                                             "Saturn centered EME2000");
            }
            return saturn;
        }
    }

    /** Get the Uranus singleton body.
     * <p>Calling this method is equivalent to call {@link
     * #getUranus(String) getUranus(null)}.</p>
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Uranus body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getUranus()
        throws OrekitException {
        return getUranus(null);
    }

    /** Get the Uranus singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @param supportedNames regular expression for supported files names (may be null)
     * @return Uranus body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getUranus(final String supportedNames)
        throws OrekitException {
        synchronized (CelestialBody.class) {

            if (uranus == null) {
                uranus = new SolarSystemBody(supportedNames,
                                             getSolarSystemBarycenter(supportedNames).getFrame(),
                                             JPLEphemeridesLoader.EphemerisType.URANUS,
                                             "Uranus centered EME2000");
            }

            return uranus;

        }
    }

    /** Get the Neptune singleton body.
     * <p>Calling this method is equivalent to call {@link
     * #getNeptune(String) getNeptune(null)}.</p>
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Neptune body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getNeptune()
        throws OrekitException {
        return getNeptune(null);
    }

    /** Get the Neptune singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @param supportedNames regular expression for supported files names (may be null)
     * @return Neptune body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getNeptune(final String supportedNames)
        throws OrekitException {
        synchronized (CelestialBody.class) {

            if (neptune == null) {
                neptune = new SolarSystemBody(supportedNames,
                                              getSolarSystemBarycenter(supportedNames).getFrame(),
                                              JPLEphemeridesLoader.EphemerisType.NEPTUNE,
                                              "Neptune centered EME2000");
            }

            return neptune;

        }
    }

    /** Get the Pluto singleton body.
     * <p>Calling this method is equivalent to call {@link
     * #getPluto(String) getPluto(null)}.</p>
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @return Pluto body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getPluto()
        throws OrekitException {
        return getPluto(null);
    }

    /** Get the Pluto singleton body.
     * <p>The axes of the body-centered frame linked to this instance
     * are parallel to the {@link Frame#getEME2000() EME2000} frame.</p>
     * @param supportedNames regular expression for supported files names (may be null)
     * @return Pluto body
     * @exception OrekitException if the JPL ephemerides cannot be read
     */
    public static CelestialBody getPluto(final String supportedNames)
        throws OrekitException {
        synchronized (CelestialBody.class) {

            if (pluto == null) {
                pluto = new SolarSystemBody(supportedNames,
                                            getSolarSystemBarycenter(supportedNames).getFrame(),
                                            JPLEphemeridesLoader.EphemerisType.PLUTO,
                                            "Pluto centered EME2000");
            }

            return pluto;

        }
    }

}

/* Copyright 2002-2011 CS Communication & Systèmes
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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math.util.FastMath;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.TimeStamped;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

/** Loader for JPL ephemerides binary files (DE 4xx) and similar formats (INPOP 06/08/10).
 * <p>JPL ephemerides binary files contain ephemerides for all solar system planets.</p>
 * <p>The JPL ephemerides binary files are recognized thanks to their base names,
 * which must match the pattern <code>[lu]nx[mp]####.ddd</code> (or
 * <code>[lu]nx[mp]####.ddd.gz</code> for gzip-compressed files) where # stands for a
 * digit character and where ddd is an ephemeris type (typically 405 or 406).</p>
 * <p>The loader supports files encoded in big-endian as well as in little-endian notation.
 * Usually, big-endian files are named <code>unx[mp]####.ddd</code>, while little-endian files
 * are named <code>lnx[mp]####.ddd</code>.</p>
 * @author Luc Maisonobe
 */
public class JPLEphemeridesLoader implements CelestialBodyLoader {

    /** Default supported files name pattern. */
    private static final String DEFAULT_SUPPORTED_NAMES = "^[lu]nx[mp](\\d\\d\\d\\d)\\.(?:4\\d\\d)$";

    /** 50 days in seconds. */
    private static final double FIFTY_DAYS = 50 * Constants.JULIAN_DAY;

    /** DE number used by INPOP files. */
    private static final int INPOP_DE_NUMBER = 100;

    /** The constant name for the astronomical unit. */
    private static final String CONSTANT_AU = "AU";

    /** The constant name for the earth-moon mass ratio. */
    private static final String CONSTANT_EMRAT = "EMRAT";

    /** List of supported ephemerides types. */
    public enum EphemerisType {

        /** Constant for solar system barycenter. */
        SOLAR_SYSTEM_BARYCENTER,

        /** Constant for the Sun. */
        SUN,

        /** Constant for Mercury. */
        MERCURY,

        /** Constant for Venus. */
        VENUS,

        /** Constant for the Earth-Moon barycenter. */
        EARTH_MOON,

        /** Constant for the Earth. */
        EARTH,

        /** Constant for the Moon. */
        MOON,

        /** Constant for Mars. */
        MARS,

        /** Constant for Jupiter. */
        JUPITER,

        /** Constant for Saturn. */
        SATURN,

        /** Constant for Uranus. */
        URANUS,

        /** Constant for Neptune. */
        NEPTUNE,

        /** Constant for Pluto. */
        PLUTO

    }

    /** Regular expression for supported files names. */
    private final String supportedNames;

    /** Constants defined in the file. */
    private final Map<String, Double> constants = new HashMap<String, Double>();

    /** Ephemeris type to generate. */
    private final EphemerisType generateType;

    /** Ephemeris type to load. */
    private final EphemerisType loadType;

    /** Desired central date. */
    private AbsoluteDate centralDate;

    /** Ephemeris for selected body (may contain holes between 100 days ranges). */
    private final SortedSet<TimeStamped> ephemerides;

    /** Current file start epoch. */
    private AbsoluteDate startEpoch;

    /** Current file final epoch. */
    private AbsoluteDate finalEpoch;

    /** Chunks duration (in seconds). */
    private double maxChunksDuration;

    /** Current file chunks duration (in seconds). */
    private double chunksDuration;

    /** Index of the first data for selected body. */
    private int firstIndex;

    /** Number of coefficients for selected body. */
    private int coeffs;

    /** Number of chunks for the selected body. */
    private int chunks;

    /** Number of components contained in the file. */
    private int components;

    /** Create a loader for JPL ephemerides binary files.
     * <p>
     * If the regular expression for supported names is null or is the
     * empty string, a default value of "^unx[mp](\\d\\d\\d\\d)\\.(?:(?:405)|(?:406))$"
     * supporting both DE405 and DE406 ephemerides is used.
     * </p>
     * <p>
     * The central date is used to load only a part of an ephemeris. If it
     * is non-null, all data within a +/-50 days range around this date will
     * be loaded. If it is null, an arbitrary 100 days range will be loaded,
     * this is useful to load only data from the header like astronomical
     * unit or gravity coefficients.
     * </p>
     * @param supportedNames regular expression for supported files names
     * (may be null)
     * @param generateType ephemeris type to generate
     * @param centralDate desired central date (may be null)
     * @exception OrekitException if the header constants cannot be read
     */
    public JPLEphemeridesLoader(final String supportedNames,
                                final EphemerisType generateType,
                                final AbsoluteDate centralDate)
        throws OrekitException {

        if ((supportedNames == null) || (supportedNames.length() == 0)) {
            this.supportedNames = DEFAULT_SUPPORTED_NAMES;
        } else {
            this.supportedNames = supportedNames;
        }

        if (constants.isEmpty()) {
            loadConstants();
        }

        this.generateType  = generateType;
        if (generateType == EphemerisType.SOLAR_SYSTEM_BARYCENTER) {
            loadType = EphemerisType.EARTH_MOON;
        } else if (generateType == EphemerisType.EARTH_MOON) {
            loadType = EphemerisType.MOON;
        } else {
            loadType = generateType;
        }
        this.centralDate   = centralDate;
        ephemerides        = new TreeSet<TimeStamped>(new ChronologicalComparator());
        maxChunksDuration  = Double.NaN;
        chunksDuration     = Double.NaN;

    }

    /** Load celestial body.
     * @param name name of the celestial body
     * @return loaded celestial body
     * @throws OrekitException if the body cannot be loaded
     */
    public CelestialBody loadCelestialBody(final String name) throws OrekitException {

        final String inertialFrameName = name + "/inertial";
        final String bodyFrameName = name + "/rotating";
        final double gm = getLoadedGravitationalCoefficient(generateType);

        switch (generateType) {
        case SOLAR_SYSTEM_BARYCENTER :
            return new JPLCelestialBody(supportedNames, name, gm, IAUPoleFactory.getIAUPole(generateType),
                                        CelestialBodyFactory.getEarthMoonBarycenter().getInertiallyOrientedFrame(),
                                        inertialFrameName, bodyFrameName) {

                /** Serializable UID. */
                private static final long serialVersionUID = -8410904683796353385L;

                /** {@inheritDoc} */
                public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
                    throws OrekitException {
                    // we define solar system barycenter with respect to Earth-Moon barycenter
                    // so we need to revert the vectors provided by the JPL DE 405 ephemerides
                    final PVCoordinates emPV = super.getPVCoordinates(date, frame);
                    return new PVCoordinates(emPV.getPosition().negate(), emPV.getVelocity().negate());
                }

            };
        case EARTH_MOON :
            final double scale = 1.0 / (1.0 + getLoadedEarthMoonMassRatio());
            return new JPLCelestialBody(supportedNames, name, gm, IAUPoleFactory.getIAUPole(generateType),
                                        FramesFactory.getEME2000(), inertialFrameName, bodyFrameName) {

                /** Serializable UID. */
                private static final long serialVersionUID = -6986513570631050939L;

                /** {@inheritDoc} */
                public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
                    throws OrekitException {
                    // we define Earth-Moon barycenter with respect to Earth center so we need
                    // to apply a scale factor to the Moon vectors provided by the JPL DE 405 ephemerides
                    return new PVCoordinates(scale, super.getPVCoordinates(date, frame));
                }
            };
        case EARTH :
            return new CelestialBody() {

                /** Serializable UID. */
                private static final long serialVersionUID = -2293993238579492125L;

                /** {@inheritDoc} */
                public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
                    throws OrekitException {

                    // specific implementation for Earth:
                    // the Earth is always exactly at the origin of its own inertial frame
                    return getInertiallyOrientedFrame().getTransformTo(frame, date).transformPVCoordinates(PVCoordinates.ZERO);

                }

                /** {@inheritDoc} */
                @Deprecated
                public Frame getFrame() {
                    return FramesFactory.getEME2000();
                }

                /** {@inheritDoc} */
                public Frame getInertiallyOrientedFrame() {
                    return FramesFactory.getEME2000();
                }

                /** {@inheritDoc} */
                public Frame getBodyOrientedFrame() throws OrekitException {
                    return FramesFactory.getITRF2005();
                }

                /** {@inheritDoc} */
                public String getName() {
                    return name;
                }

                /** {@inheritDoc} */
                public double getGM() {
                    return gm;
                }

            };
        case MOON :
            return new JPLCelestialBody(supportedNames, name, gm, IAUPoleFactory.getIAUPole(generateType),
                                        FramesFactory.getEME2000(), inertialFrameName, bodyFrameName);
        default :
            return new JPLCelestialBody(supportedNames, name, gm, IAUPoleFactory.getIAUPole(generateType),
                                        CelestialBodyFactory.getSolarSystemBarycenter().getInertiallyOrientedFrame(),
                                        inertialFrameName, bodyFrameName);
        }
    }

    /** {@inheritDoc} */
    public boolean foundData() {
        // special case for Earth: we don't really load anything so we always found something
        return (generateType == EphemerisType.EARTH) ? true : !ephemerides.isEmpty();
    }

    /** {@inheritDoc} */
    public String getSupportedNames() {
        return supportedNames;
    }

    /** Get astronomical unit.
     * <p>
     * This method loads its constants from the files using the default JPL
     * names only.
     * </p>
     * @return astronomical unit in meters
     * @exception OrekitException if constants cannot be loaded
     * @deprecated as of 4.2, replaced by the non-static method
     * {@link #getLoadedAstronomicalUnit()}
     */
    @Deprecated
    public static double getAstronomicalUnit() throws OrekitException {
        return new JPLEphemeridesLoader(null, EphemerisType.SUN, null).getLoadedAstronomicalUnit();
    }

    /** Get astronomical unit.
     * @return astronomical unit in meters
     * @exception OrekitException if constants cannot be loaded
     */
    public double getLoadedAstronomicalUnit() throws OrekitException {

        if (constants.isEmpty()) {
            loadConstants();
        }

        return 1000.0 * getLoadedConstant(CONSTANT_AU);

    }

    /** Get Earth/Moon mass ratio.
     * <p>
     * This method loads its constants from the files using the default JPL
     * names only.
     * </p>
     * @return Earth/Moon mass ratio
     * @exception OrekitException if constants cannot be loaded
     * @deprecated as of 4.2, replaced by the non-static method
     * {@link #getLoadedEarthMoonMassRatio()}
     */
    @Deprecated
    public static double getEarthMoonMassRatio() throws OrekitException {
        return new JPLEphemeridesLoader(null, EphemerisType.EARTH_MOON, null).getLoadedEarthMoonMassRatio();
    }

    /** Get Earth/Moon mass ratio.
     * @return Earth/Moon mass ratio
     * @exception OrekitException if constants cannot be loaded
     */
    public double getLoadedEarthMoonMassRatio() throws OrekitException {

        if (constants.isEmpty()) {
            loadConstants();
        }

        return getLoadedConstant(CONSTANT_EMRAT);

    }

    /** Get the gravitational coefficient of a body.
     * <p>
     * This method loads its constants from the files using the default JPL
     * names only.
     * </p>
     * @param body body for which the gravitational coefficient is requested
     * @return gravitational coefficient in m<sup>3</sup>/s<sup>2</sup>
     * @exception OrekitException if constants cannot be loaded
     * @deprecated as of 4.2, replaced by the non-static method
     * {@link #getLoadedGravitationalCoefficient(EphemerisType)}
     */
    @Deprecated
    public static double getGravitationalCoefficient(final EphemerisType body)
        throws OrekitException {
        return new JPLEphemeridesLoader(null, EphemerisType.SUN, null).getLoadedGravitationalCoefficient(body);
    }

    /** Get the gravitational coefficient of a body.
     * @param body body for which the gravitational coefficient is requested
     * @return gravitational coefficient in m<sup>3</sup>/s<sup>2</sup>
     * @exception OrekitException if constants cannot be loaded
     */
    public double getLoadedGravitationalCoefficient(final EphemerisType body)
        throws OrekitException {

        if (constants.isEmpty()) {
            loadConstants();
        }

        // coefficient in au<sup>3</sup>/day<sup>2</sup>
        final double rawGM;
        switch (body) {
        case SOLAR_SYSTEM_BARYCENTER :
            return getLoadedGravitationalCoefficient(EphemerisType.SUN)        +
                   getLoadedGravitationalCoefficient(EphemerisType.MERCURY)    +
                   getLoadedGravitationalCoefficient(EphemerisType.VENUS)      +
                   getLoadedGravitationalCoefficient(EphemerisType.EARTH_MOON) +
                   getLoadedGravitationalCoefficient(EphemerisType.MARS)       +
                   getLoadedGravitationalCoefficient(EphemerisType.JUPITER)    +
                   getLoadedGravitationalCoefficient(EphemerisType.SATURN)     +
                   getLoadedGravitationalCoefficient(EphemerisType.URANUS)     +
                   getLoadedGravitationalCoefficient(EphemerisType.NEPTUNE)    +
                   getLoadedGravitationalCoefficient(EphemerisType.PLUTO);
        case SUN :
            rawGM = getLoadedConstant("GMS");
            break;
        case MERCURY :
            rawGM = getLoadedConstant("GM1");
            break;
        case VENUS :
            rawGM = getLoadedConstant("GM2");
            break;
        case EARTH_MOON :
            rawGM = getLoadedConstant("GMB");
            break;
        case EARTH :
            return getLoadedEarthMoonMassRatio() *
                   getLoadedGravitationalCoefficient(EphemerisType.MOON);
        case MOON :
            return getLoadedGravitationalCoefficient(EphemerisType.EARTH_MOON) /
                   (1.0 + getLoadedEarthMoonMassRatio());
        case MARS :
            rawGM = getLoadedConstant("GM4");
            break;
        case JUPITER :
            rawGM = getLoadedConstant("GM5");
            break;
        case SATURN :
            rawGM = getLoadedConstant("GM6");
            break;
        case URANUS :
            rawGM = getLoadedConstant("GM7");
            break;
        case NEPTUNE :
            rawGM = getLoadedConstant("GM8");
            break;
        case PLUTO :
            rawGM = getLoadedConstant("GM9");
            break;
        default :
            throw OrekitException.createInternalError(null);
        }

        final double au    = getLoadedAstronomicalUnit();
        return rawGM * au * au * au / (Constants.JULIAN_DAY * Constants.JULIAN_DAY);

    }

    /** Get a constant defined in the ephemerides headers.
     * <p>
     * This method loads its constants from the files using the default JPL
     * names only.
     * </p>
     * <p>Note that since constants are defined in the JPL headers
     * files, they are available as soon as one file is available, even
     * if it doesn't match the desired central date. This is because the
     * header must be parsed before the dates can be checked.</p>
     * @param name name of the constant
     * @return value of the constant of NaN if the constant is not defined
     * @exception OrekitException if constants cannot be loaded
     * @deprecated as of 4.2, replaced by the non-static method
     * {@link #getLoadedConstant(String)}
     */
    @Deprecated
    public static double getConstant(final String name) throws OrekitException {
        return new JPLEphemeridesLoader(null, EphemerisType.SUN, null).getLoadedConstant(name);
    }

    /** Get a constant defined in the ephemerides headers.
     * <p>Note that since constants are defined in the JPL headers
     * files, they are available as soon as one file is available, even
     * if it doesn't match the desired central date. This is because the
     * header must be parsed before the dates can be checked.</p>
     * @param name name of the constant
     * @return value of the constant of NaN if the constant is not defined
     * @exception OrekitException if constants cannot be loaded
     */
    public double getLoadedConstant(final String name) throws OrekitException {

        if (constants.isEmpty()) {
            loadConstants();
        }

        final Double value = constants.get(name);
        return (value == null) ? Double.NaN : value.doubleValue();

    }

    /** Load the header constants.
     * @exception OrekitException if constants cannot be loaded
     */
    private void loadConstants() throws OrekitException {
        if (!DataProvidersManager.getInstance().feed(supportedNames, new HeaderConstantsLoader())) {
            throw new OrekitException(OrekitMessages.NO_JPL_EPHEMERIDES_BINARY_FILES_FOUND);
        }
    }

    /** Get the maximal chunks duration.
     * @return chunks maximal duration in seconds
     */
    public double getMaxChunksDuration() {
        return maxChunksDuration;
    }

    /** {@inheritDoc} */
    public boolean stillAcceptsData() {

        // special case for Earth: we don't really load anything
        if (generateType == EphemerisType.EARTH) {
            return false;
        }

        synchronized (this) {

            if (centralDate == null) {
                return true;
            }

            // use some safety margins
            final AbsoluteDate before = centralDate.shiftedBy(-FIFTY_DAYS);
            final AbsoluteDate after  = centralDate.shiftedBy( FIFTY_DAYS);
            synchronized (JPLEphemeridesLoader.this) {
                final Iterator<TimeStamped> iterator = ephemerides.tailSet(before).iterator();
                if (!iterator.hasNext()) {
                    return true;
                }
                PosVelChebyshev previous = (PosVelChebyshev) iterator.next();
                if (!previous.inRange(before)) {
                    // the date 50 days before central date is not covered yet
                    // we need to read more data
                    return true;
                }
                while (iterator.hasNext()) {
                    final PosVelChebyshev current = (PosVelChebyshev) iterator.next();
                    if (!current.isSuccessorOf(previous)) {
                        // there is a hole in the [-50 days ; +50 days] interval
                        // we need to read more data
                        return true;
                    }
                    if (current.inRange(after)) {
                        // the whole [-50 days ; +50 days] interval is covered
                        // we don't need to read any more data
                        return false;
                    }
                    previous = current;
                }

                // the date 50 days after central date is not covered yet
                // we need to read more data
                return true;

            }

        }
    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
        throws OrekitException, IOException {

        // read first header record
        final byte[] record = readFirstRecord(input, name);

        // parse first header record
        final boolean bigEndian = parseFirstHeaderRecord(record, name);

        synchronized (this) {
            if (centralDate == null) {
                // this is the first call to the method and the central date is not set
                // we set it arbitrarily to startEpoch + 50 days in order to load only
                // the first 100 days worth of data
                centralDate = startEpoch.shiftedBy(FIFTY_DAYS);
            } else if ((centralDate.durationFrom(finalEpoch) > FIFTY_DAYS) ||
                    (startEpoch.durationFrom(centralDate) > FIFTY_DAYS)) {
                // this file does not cover a range we are interested in,
                // there is no need to parse it further
                return;
            }

        }

        // the second record contains the values of the constants used for least-square filtering
        // we ignore them here (they have been read once for all while setting up the constants map)
        if (!readInRecord(input, record, 0)) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_READ_JPL_HEADER, name);
        }

        // read ephemerides data
        while (readInRecord(input, record, 0)) {
            parseDataRecord(record, bigEndian);
        }

    }

    /** Parse the first header record.
     * @param record first header record
     * @param name name of the file (or zip entry)
     * @return <code>true</code> if the JPL file header is encoded in big-endian notation,
     * <code>false</code> otherwise
     * @exception OrekitException if the header is not a JPL ephemerides binary file header
     */
    private boolean parseFirstHeaderRecord(final byte[] record, final String name)
        throws OrekitException {

        // detect the endianess of the file
        final boolean bigEndian = detectEndianess(record);

        // get the ephemerides type
        final int deNum = extractInt(record, 2840, bigEndian);

        // as default, 3 polynomial coefficients for the cartesian coordinates
        // (x, y, z) are contained in the file
        components = 3;

        if (deNum == INPOP_DE_NUMBER) {
            // an INPOP file may contain 6 components (including coefficients for the velocity vector)
            final double format = getLoadedConstant("FORMAT");
            if (!Double.isNaN(format) && (int) FastMath.IEEEremainder(format, 10) != 1) {
                components = 6;
            }

            // INPOP files may have their polynomials expressed in AU
            final double unite = getLoadedConstant("UNITE");
            if (!Double.isNaN(unite) && (int) unite == 0) {
                // proper handling in this case: the resulting PV coordinates from
                // the PosVelChebyshev model needs to be scaled
            }
        }

        // extract covered date range
        startEpoch = extractDate(record, 2652, bigEndian);
        finalEpoch = extractDate(record, 2660, bigEndian);
        boolean ok = finalEpoch.compareTo(startEpoch) > 0;

        // check astronomical unit consistency
        final double au = 1000 * extractDouble(record, 2680, bigEndian);
        ok = ok && (au > 1.4e11) && (au < 1.6e11);
        if (FastMath.abs(getLoadedAstronomicalUnit() - au) >= 0.001) {
            throw new OrekitException(OrekitMessages.INCONSISTENT_ASTRONOMICAL_UNIT_IN_FILES,
                                      getLoadedAstronomicalUnit(), au);
        }

        // check earth-moon mass ratio consistency
        final double emRat = extractDouble(record, 2688, bigEndian);
        ok = ok && (emRat > 80) && (emRat < 82);
        if (FastMath.abs(getLoadedEarthMoonMassRatio() - emRat) >= 1.0e-8) {
            throw new OrekitException(OrekitMessages.INCONSISTENT_EARTH_MOON_RATIO_IN_FILES,
                                      getLoadedEarthMoonMassRatio(), emRat);
        }

        synchronized (this) {

            // indices of the Chebyshev coefficients for each ephemeris
            for (int i = 0; i < 12; ++i) {
                final int row1 = extractInt(record, 2696 + 12 * i, bigEndian);
                final int row2 = extractInt(record, 2700 + 12 * i, bigEndian);
                final int row3 = extractInt(record, 2704 + 12 * i, bigEndian);
                ok = ok && (row1 >= 0) && (row2 >= 0) && (row3 >= 0);
                if (((i ==  0) && (loadType == EphemerisType.MERCURY))    ||
                        ((i ==  1) && (loadType == EphemerisType.VENUS))      ||
                        ((i ==  2) && (loadType == EphemerisType.EARTH_MOON)) ||
                        ((i ==  3) && (loadType == EphemerisType.MARS))       ||
                        ((i ==  4) && (loadType == EphemerisType.JUPITER))    ||
                        ((i ==  5) && (loadType == EphemerisType.SATURN))     ||
                        ((i ==  6) && (loadType == EphemerisType.URANUS))     ||
                        ((i ==  7) && (loadType == EphemerisType.NEPTUNE))    ||
                        ((i ==  8) && (loadType == EphemerisType.PLUTO))      ||
                        ((i ==  9) && (loadType == EphemerisType.MOON))       ||
                        ((i == 10) && (loadType == EphemerisType.SUN))) {
                    firstIndex = row1;
                    coeffs     = row2;
                    chunks     = row3;
                }
            }

            // compute chunks duration
            final double timeSpan = extractDouble(record, 2668, bigEndian);
            ok = ok && (timeSpan > 0) && (timeSpan < 100);
            chunksDuration = Constants.JULIAN_DAY * (timeSpan / chunks);
            if (Double.isNaN(maxChunksDuration)) {
                maxChunksDuration = chunksDuration;
            } else {
                maxChunksDuration = FastMath.max(maxChunksDuration, chunksDuration);
            }

        }

        // sanity checks
        if (!ok) {
            throw new OrekitException(OrekitMessages.NOT_A_JPL_EPHEMERIDES_BINARY_FILE, name);
        }

        return bigEndian;
    }

    /** Parse regular ephemeris record.
     * @param record record to parse
     * @param bigEndian indicates whether the data record contains data in
     *                  big-endian or little-endian notation
     * @exception OrekitException if the header is not a JPL ephemerides binary file header
     */
    private void parseDataRecord(final byte[] record, final boolean bigEndian) throws OrekitException {

        // extract time range covered by the record
        final AbsoluteDate rangeStart = extractDate(record, 0, bigEndian);
        if (rangeStart.compareTo(startEpoch) < 0) {
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE, rangeStart, startEpoch, finalEpoch);
        }

        final AbsoluteDate rangeEnd   = extractDate(record, 8, bigEndian);
        if (rangeEnd.compareTo(finalEpoch) > 0) {
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE, rangeEnd, startEpoch, finalEpoch);
        }

        synchronized (this) {

            if ((centralDate.durationFrom(rangeEnd) > FIFTY_DAYS) ||
                    (rangeStart.durationFrom(centralDate) > FIFTY_DAYS)) {
                // we are not interested in this record, don't parse it
                return;
            }

            // loop over chunks inside the time range
            AbsoluteDate chunkEnd = rangeStart;
            final int nbChunks    = chunks;
            final int nbCoeffs    = coeffs;
            final int first       = firstIndex;
            final double duration = chunksDuration;
            for (int i = 0; i < nbChunks; ++i) {

                // set up chunk validity range
                final AbsoluteDate chunkStart = chunkEnd;
                chunkEnd = (i == nbChunks - 1) ?
                        rangeEnd : rangeStart.shiftedBy((i + 1) * duration);

                // extract Chebyshev coefficients for the selected body
                // and convert them from kilometers to meters
                final double[] xCoeffs = new double[nbCoeffs];
                final double[] yCoeffs = new double[nbCoeffs];
                final double[] zCoeffs = new double[nbCoeffs];

                for (int k = 0; k < nbCoeffs; ++k) {
                    // by now, only use the position components
                    // if there are also velocity components contained in the file, ignore them
                    final int index = first + components * i * nbCoeffs + k - 1;
                    xCoeffs[k] = 1000.0 * extractDouble(record, 8 * index, bigEndian);
                    yCoeffs[k] = 1000.0 * extractDouble(record, 8 * (index +  nbCoeffs), bigEndian);
                    zCoeffs[k] = 1000.0 * extractDouble(record, 8 * (index + 2 * nbCoeffs), bigEndian);
                }

                // build the position-velocity model for current chunk
                ephemerides.add(new PosVelChebyshev(chunkStart, duration,
                                                    xCoeffs, yCoeffs, zCoeffs));

            }
        }

    }

    /** Read first header record.
     * @param input input stream
     * @param name name of the file (or zip entry)
     * @return record record where to put bytes
     * @exception OrekitException if the stream does not contain a JPL ephemeris
     * @exception IOException if a read error occurs
     */
    private static byte[] readFirstRecord(final InputStream input, final String name)
        throws OrekitException, IOException {

        // read first part of record, up to the ephemeris type
        final byte[] firstPart = new byte[2860];
        if (!readInRecord(input, firstPart, 0)) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_READ_JPL_HEADER, name);
        }

        // detect the endian format
        final boolean bigEndian = detectEndianess(firstPart);

        // get the ephemerides type
        final int deNum = extractInt(firstPart, 2840, bigEndian);

        // the record size for this file
        int recordSize = 0;

        if (deNum == INPOP_DE_NUMBER) {
            // INPOP files have an extended DE format, which includes also the record size
            recordSize = extractInt(firstPart, 2856, bigEndian) << 3;
        } else {
            // compute the record size for original JPL files
            recordSize = computeRecordSize(firstPart, bigEndian, name);
        }

        if (recordSize <= 0) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_READ_JPL_HEADER, name);
        }

        // build a record with the proper size and finish read of the first complete record
        final int start = firstPart.length;
        final byte[] record = new byte[recordSize];
        System.arraycopy(firstPart, 0, record, 0, firstPart.length);
        if (!readInRecord(input, record, start)) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_READ_JPL_HEADER, name);
        }

        return record;

    }

    /** Read bytes into the current record array.
     * @param input input stream
     * @param record record where to put bytes
     * @param start start index where to put bytes
     * @return true if record has been filled up
     * @exception IOException if a read error occurs
     */
    private static boolean readInRecord(final InputStream input,
                                        final byte[] record, final int start)
        throws IOException {
        int index = start;
        while (index != record.length) {
            final int n = input.read(record, index, record.length - index);
            if (n < 0) {
                return false;
            }
            index += n;
        }
        return true;
    }

    /** Detect whether the JPL ephemerides file is stored in big-endian or
     * little-endian notation.
     * @param record the array containing the binary JPL header
     * @return <code>true</code> if the file is stored in big-endian,
     * <code>false</code> otherwise
     */
    private static boolean detectEndianess(final byte[] record) {

        // default to big-endian
        boolean bigEndian = true;

        // first try to read the DE number in big-endian format
        final int deNum = extractInt(record, 2840, true);

        // simple heuristic: if the read value is larger than half the range of an integer
        //                   assume the file is in little-endian format
        if ((deNum & 0xffffffffL) > (1 << 15)) {
            bigEndian = false;
        }

        return bigEndian;

    }

    /** Calculate the record size of a JPL ephemerides file.
     * @param record the byte array containing the header record
     * @param bigEndian indicates the endianess of the file
     * @param name the name of the data file
     * @return the record size for this file
     * @throws OrekitException if the file contains unexpected data
     */
    private static int computeRecordSize(final byte[] record,
                                         final boolean bigEndian,
                                         final String name)
        throws OrekitException {

        int recordSize = 0;
        boolean ok = true;
        // JPL files always have 3 position components
        final int nComp = 3;

        // iterate over the coefficient ptr array and sum up the record size
        // the coeffPtr array has the dimensions [12][nComp]
        for (int j = 0; j < 12; j++) {
            final int nCompCur = (j == 11) ? 2 : nComp;

            // the coeffPtr array starts at offset 2696
            // Note: the array element coeffPtr[j][0] is not needed for the calculation
            final int idx = 2696 + j * nComp * 4;
            final int coeffPtr1 = extractInt(record, idx + 4, bigEndian);
            final int coeffPtr2 = extractInt(record, idx + 8, bigEndian);

            // sanity checks
            ok = ok && (coeffPtr1 >= 0 || coeffPtr2 >= 0);

            recordSize += coeffPtr1 * coeffPtr2 * nCompCur;
        }

        // the libration ptr array starts at offset 2844 and has the dimension [3]
        // Note: the array element libratPtr[0] is not needed for the calculation
        final int libratPtr1 = extractInt(record, 2844 + 4, bigEndian);
        final int libratPtr2 = extractInt(record, 2844 + 8, bigEndian);

        // sanity checks
        ok = ok && (libratPtr1 >= 0 || libratPtr2 >= 0);

        recordSize += libratPtr1 * libratPtr2 * nComp + 2;
        recordSize <<= 3;

        if (!ok || recordSize <= 0) {
            throw new OrekitException(OrekitMessages.NOT_A_JPL_EPHEMERIDES_BINARY_FILE, name);
        }

        return recordSize;

    }

    /** Extract a date from a record.
     * @param record record to parse
     * @param offset offset of the double within the record
     * @param bigEndian if <code>true</code> the parsed date is extracted in big-endian
     * format, otherwise it is extracted in little-endian format
     * @return extracted date
     */
    private static AbsoluteDate extractDate(final byte[] record,
                                            final int offset,
                                            final boolean bigEndian) {

        final double t       = extractDouble(record, offset, bigEndian);
        int    jDay    = (int) FastMath.floor(t);
        double seconds = (t + 0.5 - jDay) * Constants.JULIAN_DAY;
        if (seconds >= Constants.JULIAN_DAY) {
            ++jDay;
            seconds -= Constants.JULIAN_DAY;
        }
        final AbsoluteDate date =
            new AbsoluteDate(new DateComponents(DateComponents.JULIAN_EPOCH, jDay),
                             new TimeComponents(seconds), TimeScalesFactory.getTDB());
        return date;
    }

    /** Extract a double from a record.
     * <p>Double numbers are stored according to IEEE 754 standard, with
     * most significant byte first.</p>
     * @param record record to parse
     * @param offset offset of the double within the record
     * @param bigEndian if <code>true</code> the parsed double is extracted in big-endian
     * format, otherwise it is extracted in little-endian format
     * @return extracted double
     */
    private static double extractDouble(final byte[] record, final int offset, final boolean bigEndian) {
        final long l8 = ((long) record[offset + 0]) & 0xffl;
        final long l7 = ((long) record[offset + 1]) & 0xffl;
        final long l6 = ((long) record[offset + 2]) & 0xffl;
        final long l5 = ((long) record[offset + 3]) & 0xffl;
        final long l4 = ((long) record[offset + 4]) & 0xffl;
        final long l3 = ((long) record[offset + 5]) & 0xffl;
        final long l2 = ((long) record[offset + 6]) & 0xffl;
        final long l1 = ((long) record[offset + 7]) & 0xffl;
        long l;
        if (bigEndian) {
            l = (l8 << 56) | (l7 << 48) | (l6 << 40) | (l5 << 32) |
                (l4 << 24) | (l3 << 16) | (l2 <<  8) | l1;
        } else {
            l = (l1 << 56) | (l2 << 48) | (l3 << 40) | (l4 << 32) |
                (l5 << 24) | (l6 << 16) | (l7 <<  8) | l8;
        }
        return Double.longBitsToDouble(l);
    }

    /** Extract an int from a record.
     * @param record record to parse
     * @param offset offset of the double within the record
     * @param bigEndian if <code>true</code> the parsed int is extracted in big-endian
     * format, otherwise it is extracted in little-endian format
     * @return extracted int
     */
    private static int extractInt(final byte[] record, final int offset, final boolean bigEndian) {
        final int l4 = ((int) record[offset + 0]) & 0xff;
        final int l3 = ((int) record[offset + 1]) & 0xff;
        final int l2 = ((int) record[offset + 2]) & 0xff;
        final int l1 = ((int) record[offset + 3]) & 0xff;

        if (bigEndian) {
            return (l4 << 24) | (l3 << 16) | (l2 <<  8) | l1;
        } else {
            return (l1 << 24) | (l2 << 16) | (l3 <<  8) | l4;
        }
    }

    /** Extract a String from a record.
     * @param record record to parse
     * @param offset offset of the string within the record
     * @param length maximal length of the string
     * @return extracted string, with whitespace characters stripped
     */
    private static String extractString(final byte[] record, final int offset, final int length) {
        try {
            return new String(record, offset, length, "US-ASCII").trim();
        } catch (UnsupportedEncodingException uee) {
            throw OrekitException.createInternalError(uee);
        }
    }

    /** Specialized loader for extracting constants from the headers. */
    private class HeaderConstantsLoader implements DataLoader {

        /** {@inheritDoc} */
        public boolean stillAcceptsData() {
            // we try to load files only until the constants map has been set up
            return constants.isEmpty();
        }

        /** {@inheritDoc} */
        public void loadData(final InputStream input, final String name)
            throws IOException, ParseException, OrekitException {

            // read header records
            final byte[] first  = readFirstRecord(input, name);
            final byte[] second = new byte[first.length];
            if (!readInRecord(input, second, 0)) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_READ_JPL_HEADER, name);
            }

            final boolean bigEndian = detectEndianess(first);

            // constants defined in the file
            for (int i = 0; i < 400; ++i) {
                // Note: for extracting the strings from the binary file, it makes no difference
                //       if the file is stored in big-endian or little-endian notation
                final String constantName = extractString(first, 252 + i * 6, 6);
                if (constantName.length() == 0) {
                    // no more constants to read
                    break;
                }
                final double constantValue = extractDouble(second, 8 * i, bigEndian);
                constants.put(constantName, constantValue);
            }

            // INPOP files do not have constants for AU and EMRAT, thus extract them from
            // the header record and create a constant for them to be consistent with JPL files

            double au = getLoadedAstronomicalUnit();
            if (Double.isNaN(au)) {
                au = extractDouble(first, 2680, bigEndian);
                if (au < 1.4e8 || au > 1.6e8) {
                    throw new OrekitException(OrekitMessages.NOT_A_JPL_EPHEMERIDES_BINARY_FILE, name);
                } else {
                    constants.put(CONSTANT_AU, au);
                }
            }

            double emRat = getLoadedEarthMoonMassRatio();
            if (Double.isNaN(emRat)) {
                emRat = extractDouble(first, 2688, bigEndian);
                if (emRat < 80 || emRat > 82) {
                    throw new OrekitException(OrekitMessages.NOT_A_JPL_EPHEMERIDES_BINARY_FILE, name);
                } else {
                    constants.put(CONSTANT_EMRAT, emRat);
                }
            }

        }
    };

    /** Private CelestialBody class. */
    private class JPLCelestialBody extends AbstractCelestialBody {

        /** Serializable UID. */
        private static final long serialVersionUID = 6900839423652963125L;

        /** Current Chebyshev model. */
        private PosVelChebyshev model;

        /** Frame in which ephemeris are defined. */
        private final Frame definingFrame;

        /** Private constructor for the singletons.
         * @param supportedNames regular expression for supported files names (may be null)
         * @param name name of the body
         * @param gm attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
         * @param iauPole IAU pole implementation
         * @param definingFrame frame in which ephemeris are defined
         * @param inertialFrameName name to use for inertially oriented body centered frame
         * @param bodyFrameName name to use for body oriented body centered frame
         */
        private JPLCelestialBody(final String supportedNames, final String name, final double gm,
                                 final IAUPole iauPole, final Frame definingFrame,
                                 final String inertialFrameName, final String bodyFrameName) {
            super(name, gm, iauPole, definingFrame, inertialFrameName, bodyFrameName);
            this.model         = null;
            this.definingFrame = definingFrame;
        }

        /** {@inheritDoc} */
        public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
            throws OrekitException {

            // get position/velocity in parent frame
            final PVCoordinates pv;
            synchronized (JPLEphemeridesLoader.this) {
                setPVModel(date);
                pv = model.getPositionVelocity(date);
            }

            // convert to required frame
            if (frame == definingFrame) {
                return pv;
            } else {
                final Transform transform = definingFrame.getTransformTo(frame, date);
                return transform.transformPVCoordinates(pv);
            }

        }

        /** Set the position-velocity model covering a specified date.
         * @param date target date
         * @exception OrekitException if current date is not covered by
         * available ephemerides
         */
        private void setPVModel(final AbsoluteDate date) throws OrekitException {

            // first quick check: is the current model valid for specified date ?
            if (model != null) {

                if (model.inRange(date)) {
                    return;
                }

                // try searching only within the already loaded ephemeris part
                final AbsoluteDate before = date.shiftedBy(-model.getValidityDuration());
                synchronized (JPLEphemeridesLoader.this) {
                    for (final Iterator<TimeStamped> iterator = ephemerides.tailSet(before).iterator();
                         iterator.hasNext();) {
                        model = (PosVelChebyshev) iterator.next();
                        if (model.inRange(date)) {
                            return;
                        }
                    }
                }

            }

            // existing ephemerides (if any) are too far from current date
            // load a new part of ephemerides, centered around specified date
            synchronized (JPLEphemeridesLoader.this) {
                centralDate = date;
            }

            if (!DataProvidersManager.getInstance().feed(supportedNames, JPLEphemeridesLoader.this)) {
                throw new OrekitException(OrekitMessages.NO_JPL_EPHEMERIDES_BINARY_FILES_FOUND);
            }

            // second try, searching newly loaded part designed to bracket date
            synchronized (JPLEphemeridesLoader.this) {
                final AbsoluteDate before = date.shiftedBy(-maxChunksDuration);
                for (final Iterator<TimeStamped> iterator = ephemerides.tailSet(before).iterator();
                     iterator.hasNext();) {
                    model = (PosVelChebyshev) iterator.next();
                    if (model.inRange(date)) {
                        return;
                    }
                }
            }

            // no way, this means we don't have available data for this date
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_BODY_EPHEMERIDES_DATE,
                                      loadType, date);

        }
    }

}

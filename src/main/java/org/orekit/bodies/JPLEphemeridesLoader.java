/* Copyright 2002-2012 CS Systèmes d'Information
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
package org.orekit.bodies;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math3.util.FastMath;
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
import org.orekit.time.TimeScale;
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
 * <p>The IMCCE ephemerides binary files are recognized thanks to their base names,
 * which must match the pattern <code>inpop*.dat</code> (or
 * <code>inpop*.dat.gz</code> for gzip-compressed files) where * stands for any string.</p>
 * <p>The loader supports files encoded in big-endian as well as in little-endian notation.
 * Usually, big-endian files contain <code>bigendian</code> in their names, while little-endian files
 * contain <code>littleendian</code> in their names.</p>
 * <p>The loader supports files in TDB or TCB time scales.</p>
 * @author Luc Maisonobe
 */
public class JPLEphemeridesLoader implements CelestialBodyLoader {

    /** Default supported files name pattern for JPL DE files. */
    public static final String DEFAULT_DE_SUPPORTED_NAMES = "^[lu]nx[mp](\\d\\d\\d\\d)\\.(?:4\\d\\d)$";

    /** Default supported files name pattern for IMCCE INPOP files. */
    public static final String DEFAULT_INPOP_SUPPORTED_NAMES = "^inpop.*\\.dat$";

   /** 50 days in seconds. */
    private static final double FIFTY_DAYS = 50 * Constants.JULIAN_DAY;

    /** DE number used by INPOP files. */
    private static final int INPOP_DE_NUMBER = 100;

    /** Maximal number of constants in headers. */
    private static final int CONSTANTS_MAX_NUMBER           = 400;

    /** Offset of the ephemeris type in first header record. */
    private static final int HEADER_EPHEMERIS_TYPE_OFFSET   = 2840;

    /** Offset of the record size (for INPOP files) in first header record. */
    private static final int HEADER_RECORD_SIZE_OFFSET      = 2856;

    /** Offset of the start epoch in first header record. */
    private static final int HEADER_START_EPOCH_OFFSET      = 2652;

    /** Offset of the end epoch in first header record. */
    private static final int HEADER_END_EPOCH_OFFSET        = 2660;

    /** Offset of the astronomical unit in first header record. */
    private static final int HEADER_ASTRONOMICAL_UNIT_OFFSET = 2680;

    /** Offset of the Earth-Moon mass ratio in first header record. */
    private static final int HEADER_EM_RATIO_OFFSET         = 2688;

    /** Offset of Chebishev coefficients indices in first header record. */
    private static final int HEADER_CHEBISHEV_INDICES_OFFSET = 2696;

    /** Offset of libration coefficients indices in first header record. */
    private static final int HEADER_LIBRATION_INDICES_OFFSET = 2844;

    /** Offset of chunks duration in first header record. */
    private static final int HEADER_CHUNK_DURATION_OFFSET    = 2668;

    /** Offset of the constants names in first header record. */
    private static final int HEADER_CONSTANTS_NAMES_OFFSET  = 252;

    /** Offset of the constants values in second header record. */
    private static final int HEADER_CONSTANTS_VALUES_OFFSET = 0;

    /** Offset of the range start in the data records. */
    private static final int DATA_START_RANGE_OFFSET        = 0;

    /** Offset of the range end in the data records. */
    private static final int DATE_END_RANGE_OFFSET          = 8;

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
    private final Map<String, Double> constants;

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

    /** Unit of the position coordinates (as a multiple of meters). */
    private double positionUnit;

    /** Time scale of the date coordinates. */
    private TimeScale timeScale;

    /** Indicator for binary file endianness. */
    private boolean bigEndian;

    /** Create a loader for JPL ephemerides binary files.
     * @param supportedNames regular expression for supported files names
     * @param generateType ephemeris type to generate
     * @exception OrekitException if the header constants cannot be read
     */
    public JPLEphemeridesLoader(final String supportedNames, final EphemerisType generateType)
        throws OrekitException {

        this.supportedNames = supportedNames;
        constants = new HashMap<String, Double>();

        this.generateType  = generateType;
        if (generateType == EphemerisType.SOLAR_SYSTEM_BARYCENTER) {
            loadType = EphemerisType.EARTH_MOON;
        } else if (generateType == EphemerisType.EARTH_MOON) {
            loadType = EphemerisType.MOON;
        } else {
            loadType = generateType;
        }

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
            final JPLEphemeridesLoader embLoader = new JPLEphemeridesLoader(supportedNames, EphemerisType.EARTH_MOON);
            final CelestialBody embBody = embLoader.loadCelestialBody(CelestialBodyFactory.EARTH_MOON);
            return new JPLCelestialBody(supportedNames, name, gm, IAUPoleFactory.getIAUPole(generateType),
                                        embBody.getInertiallyOrientedFrame(), inertialFrameName, bodyFrameName) {

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
            final JPLEphemeridesLoader ssbLoader = new JPLEphemeridesLoader(supportedNames, EphemerisType.SOLAR_SYSTEM_BARYCENTER);
            final CelestialBody ssbBody = ssbLoader.loadCelestialBody(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER);
            return new JPLCelestialBody(supportedNames, name, gm, IAUPoleFactory.getIAUPole(generateType),
                                        ssbBody.getInertiallyOrientedFrame(),
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
     * @return astronomical unit in meters
     * @exception OrekitException if constants cannot be loaded
     */
    public double getLoadedAstronomicalUnit() throws OrekitException {
        return 1000.0 * getLoadedConstant(CONSTANT_AU);
    }

    /** Get Earth/Moon mass ratio.
     * @return Earth/Moon mass ratio
     * @exception OrekitException if constants cannot be loaded
     */
    public double getLoadedEarthMoonMassRatio() throws OrekitException {
        return getLoadedConstant(CONSTANT_EMRAT);
    }

    /** Get the gravitational coefficient of a body.
     * @param body body for which the gravitational coefficient is requested
     * @return gravitational coefficient in m<sup>3</sup>/s<sup>2</sup>
     * @exception OrekitException if constants cannot be loaded
     */
    public double getLoadedGravitationalCoefficient(final EphemerisType body)
        throws OrekitException {

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
            rawGM = getLoadedConstant("GMS", "GM_Sun");
            break;
        case MERCURY :
            rawGM = getLoadedConstant("GM1", "GM_Mer");
            break;
        case VENUS :
            rawGM = getLoadedConstant("GM2", "GM_Ven");
            break;
        case EARTH_MOON :
            rawGM = getLoadedConstant("GMB", "GM_EMB");
            break;
        case EARTH :
            return getLoadedEarthMoonMassRatio() *
                   getLoadedGravitationalCoefficient(EphemerisType.MOON);
        case MOON :
            return getLoadedGravitationalCoefficient(EphemerisType.EARTH_MOON) /
                   (1.0 + getLoadedEarthMoonMassRatio());
        case MARS :
            rawGM = getLoadedConstant("GM4", "GM_Mar");
            break;
        case JUPITER :
            rawGM = getLoadedConstant("GM5", "GM_Jup");
            break;
        case SATURN :
            rawGM = getLoadedConstant("GM6", "GM_Sat");
            break;
        case URANUS :
            rawGM = getLoadedConstant("GM7", "GM_Ura");
            break;
        case NEPTUNE :
            rawGM = getLoadedConstant("GM8", "GM_Nep");
            break;
        case PLUTO :
            rawGM = getLoadedConstant("GM9", "GM_Plu");
            break;
        default :
            throw OrekitException.createInternalError(null);
        }

        final double au    = getLoadedAstronomicalUnit();
        return rawGM * au * au * au / (Constants.JULIAN_DAY * Constants.JULIAN_DAY);

    }

    /** Get a constant defined in the ephemerides headers.
     * <p>Note that since constants are defined in the JPL headers
     * files, they are available as soon as one file is available, even
     * if it doesn't match the desired central date. This is because the
     * header must be parsed before the dates can be checked.</p>
     * <p>
     * There are alternate names for constants since for example JPL names are
     * different from INPOP names (Sun gravity: GMS or GM_Sun, Mercury gravity:
     * GM4 or GM_Mar...).
     * </p>
     * @param names alternate names of the constant
     * @return value of the constant of NaN if the constant is not defined
     * @exception OrekitException if constants cannot be loaded
     */
    public double getLoadedConstant(final String ... names) throws OrekitException {

        // lazy loading of constants
        if (constants.isEmpty()) {
            if (!DataProvidersManager.getInstance().feed(supportedNames, this)) {
                throw new OrekitException(OrekitMessages.NO_JPL_EPHEMERIDES_BINARY_FILES_FOUND);
            }
        }

        for (final String name : names) {
            if (constants.containsKey(name)) {
                return constants.get(name).doubleValue();
            }
        }

        return Double.NaN;

    }

    /** Get the maximal chunks duration.
     * @return chunks maximal duration in seconds
     */
    public double getMaxChunksDuration() {
        return maxChunksDuration;
    }

    /** {@inheritDoc} */
    public boolean stillAcceptsData() {

        // special case for Earth: we load only constants
        if (generateType == EphemerisType.EARTH) {
            return constants.isEmpty();
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
        final byte[] first = readFirstRecord(input, name);

        // the second record contains the values of the constants used for least-square filtering
        final byte[] second = new byte[first.length];
        if (!readInRecord(input, second, 0)) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_READ_JPL_HEADER, name);
        }

        if (constants.isEmpty()) {
            parseConstants(first, second, name);
        }

        // check astronomical unit consistency
        final double au = 1000 * extractDouble(first, HEADER_ASTRONOMICAL_UNIT_OFFSET);
        if ((au < 1.4e11) || (au > 1.6e11)) {
            throw new OrekitException(OrekitMessages.NOT_A_JPL_EPHEMERIDES_BINARY_FILE, name);
        }
        if (FastMath.abs(getLoadedAstronomicalUnit() - au) >= 0.001) {
            throw new OrekitException(OrekitMessages.INCONSISTENT_ASTRONOMICAL_UNIT_IN_FILES,
                                      getLoadedAstronomicalUnit(), au);
        }

        // check Earth-Moon mass ratio consistency
        final double emRat = extractDouble(first, HEADER_EM_RATIO_OFFSET);
        if ((emRat < 80) || (emRat > 82)) {
            throw new OrekitException(OrekitMessages.NOT_A_JPL_EPHEMERIDES_BINARY_FILE, name);
        }
        if (FastMath.abs(getLoadedEarthMoonMassRatio() - emRat) >= 1.0e-8) {
            throw new OrekitException(OrekitMessages.INCONSISTENT_EARTH_MOON_RATIO_IN_FILES,
                                      getLoadedEarthMoonMassRatio(), emRat);
        }

        // parse first header record
        parseFirstHeaderRecord(first, name);

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

        // read ephemerides data
        final byte[] data = new byte[first.length];
        while (readInRecord(input, data, 0)) {
            parseDataRecord(data);
        }

    }

    /** Parse the first header record.
     * @param record first header record
     * @param name name of the file (or zip entry)
     * @return <code>true</code> if the JPL file header is encoded in big-endian notation,
     * <code>false</code> otherwise
     * @exception OrekitException if the header is not a JPL ephemerides binary file header
     */
    private void parseFirstHeaderRecord(final byte[] record, final String name)
        throws OrekitException {

        // get the ephemerides type
        final int deNum = extractInt(record, HEADER_EPHEMERIS_TYPE_OFFSET);

        // as default, 3 polynomial coefficients for the cartesian coordinates
        // (x, y, z) are contained in the file, positions are in kilometers
        // and times are in TDB
        components   = 3;
        positionUnit = 1000.0;
        timeScale    = TimeScalesFactory.getTDB();

        if (deNum == INPOP_DE_NUMBER) {
            // an INPOP file may contain 6 components (including coefficients for the velocity vector)
            final double format = getLoadedConstant("FORMAT");
            if (!Double.isNaN(format) && (int) FastMath.IEEEremainder(format, 10) != 1) {
                components = 6;
            }

            // INPOP files may have their polynomials expressed in AU
            final double unite = getLoadedConstant("UNITE");
            if (!Double.isNaN(unite) && (int) unite == 0) {
                positionUnit = getLoadedAstronomicalUnit();
            }

            // INPOP files may have their times expressed in TCB
            final double timesc = getLoadedConstant("TIMESC");
            if (!Double.isNaN(timesc) && (int) timesc == 1) {
                timeScale = TimeScalesFactory.getTCB();
            }

        }

        // extract covered date range
        startEpoch = extractDate(record, HEADER_START_EPOCH_OFFSET);
        finalEpoch = extractDate(record, HEADER_END_EPOCH_OFFSET);
        boolean ok = finalEpoch.compareTo(startEpoch) > 0;

        synchronized (this) {

            // indices of the Chebyshev coefficients for each ephemeris
            for (int i = 0; i < 12; ++i) {
                final int row1 = extractInt(record, HEADER_CHEBISHEV_INDICES_OFFSET     + 12 * i);
                final int row2 = extractInt(record, HEADER_CHEBISHEV_INDICES_OFFSET + 4 + 12 * i);
                final int row3 = extractInt(record, HEADER_CHEBISHEV_INDICES_OFFSET + 8 + 12 * i);
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
            final double timeSpan = extractDouble(record, HEADER_CHUNK_DURATION_OFFSET);
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

    }

    /** Parse constants from first two header records
     * @param first first header record
     * @param second second header record
     * @param name name of the file (or zip entry)
     */
    private void parseConstants(final byte[] first, final byte[] second, final String name)
        throws OrekitException {

        for (int i = 0; i < CONSTANTS_MAX_NUMBER; ++i) {
            // Note: for extracting the strings from the binary file, it makes no difference
            //       if the file is stored in big-endian or little-endian notation
            final String constantName = extractString(first, HEADER_CONSTANTS_NAMES_OFFSET + i * 6, 6);
            if (constantName.length() == 0) {
                // no more constants to read
                break;
            }
            final double constantValue = extractDouble(second, HEADER_CONSTANTS_VALUES_OFFSET + 8 * i);
            constants.put(constantName, constantValue);
        }

        // INPOP files do not have constants for AU and EMRAT, thus extract them from
        // the header record and create a constant for them to be consistent with JPL files

        double au = getLoadedAstronomicalUnit();
        if (Double.isNaN(au)) {
            au = extractDouble(first, 2680);
            if (au < 1.4e8 || au > 1.6e8) {
                throw new OrekitException(OrekitMessages.NOT_A_JPL_EPHEMERIDES_BINARY_FILE, name);
            } else {
                constants.put(CONSTANT_AU, au);
            }
        }

        double emRat = getLoadedEarthMoonMassRatio();
        if (Double.isNaN(emRat)) {
            emRat = extractDouble(first, 2688);
            if (emRat < 80 || emRat > 82) {
                throw new OrekitException(OrekitMessages.NOT_A_JPL_EPHEMERIDES_BINARY_FILE, name);
            } else {
                constants.put(CONSTANT_EMRAT, emRat);
            }
        }

    }

    /** Parse regular ephemeris record.
     * @param record record to parse
     * @exception OrekitException if the header is not a JPL ephemerides binary file header
     */
    private void parseDataRecord(final byte[] record) throws OrekitException {

        // extract time range covered by the record
        final AbsoluteDate rangeStart = extractDate(record, DATA_START_RANGE_OFFSET);
        if (rangeStart.compareTo(startEpoch) < 0) {
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE, rangeStart, startEpoch, finalEpoch);
        }

        final AbsoluteDate rangeEnd   = extractDate(record, DATE_END_RANGE_OFFSET);
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
                    xCoeffs[k] = positionUnit * extractDouble(record, 8 * index);
                    yCoeffs[k] = positionUnit * extractDouble(record, 8 * (index +  nbCoeffs));
                    zCoeffs[k] = positionUnit * extractDouble(record, 8 * (index + 2 * nbCoeffs));
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
    private byte[] readFirstRecord(final InputStream input, final String name)
        throws OrekitException, IOException {

        // read first part of record, up to the ephemeris type
        final byte[] firstPart = new byte[HEADER_RECORD_SIZE_OFFSET + 4];
        if (!readInRecord(input, firstPart, 0)) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_READ_JPL_HEADER, name);
        }

        // detect the endian format
        detectEndianess(firstPart);

        // get the ephemerides type
        final int deNum = extractInt(firstPart, HEADER_EPHEMERIS_TYPE_OFFSET);

        // the record size for this file
        int recordSize = 0;

        if (deNum == INPOP_DE_NUMBER) {
            // INPOP files have an extended DE format, which includes also the record size
            recordSize = extractInt(firstPart, HEADER_RECORD_SIZE_OFFSET) << 3;
        } else {
            // compute the record size for original JPL files
            recordSize = computeRecordSize(firstPart, name);
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
    private static boolean readInRecord(final InputStream input, final byte[] record, final int start)
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
    private void detectEndianess(final byte[] record) {

        // default to big-endian
        bigEndian = true;

        // first try to read the DE number in big-endian format
        // the number is stored as unsigned int, so we have to convert it properly
        final long deNum = extractInt(record, HEADER_EPHEMERIS_TYPE_OFFSET) & 0xffffffffL;

        // simple heuristic: if the read value is larger than half the range of an integer
        //                   assume the file is in little-endian format
        if (deNum > (1 << 15)) {
            bigEndian = false;
        }

    }

    /** Calculate the record size of a JPL ephemerides file.
     * @param record the byte array containing the header record
     * @param name the name of the data file
     * @return the record size for this file
     * @throws OrekitException if the file contains unexpected data
     */
    private int computeRecordSize(final byte[] record, final String name)
        throws OrekitException {

        int recordSize = 0;
        boolean ok = true;
        // JPL files always have 3 position components
        final int nComp = 3;

        // iterate over the coefficient ptr array and sum up the record size
        // the coeffPtr array has the dimensions [12][nComp]
        for (int j = 0; j < 12; j++) {
            final int nCompCur = (j == 11) ? 2 : nComp;

            // Note: the array element coeffPtr[j][0] is not needed for the calculation
            final int idx = HEADER_CHEBISHEV_INDICES_OFFSET + j * nComp * 4;
            final int coeffPtr1 = extractInt(record, idx + 4);
            final int coeffPtr2 = extractInt(record, idx + 8);

            // sanity checks
            ok = ok && (coeffPtr1 >= 0 || coeffPtr2 >= 0);

            recordSize += coeffPtr1 * coeffPtr2 * nCompCur;
        }

        // the libration ptr array has the dimension [3]
        // Note: the array element libratPtr[0] is not needed for the calculation
        final int libratPtr1 = extractInt(record, HEADER_LIBRATION_INDICES_OFFSET + 4);
        final int libratPtr2 = extractInt(record, HEADER_LIBRATION_INDICES_OFFSET + 8);

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
     * @return extracted date
     */
    private AbsoluteDate extractDate(final byte[] record, final int offset) {

        final double t = extractDouble(record, offset);
        int    jDay    = (int) FastMath.floor(t);
        double seconds = (t + 0.5 - jDay) * Constants.JULIAN_DAY;
        if (seconds >= Constants.JULIAN_DAY) {
            ++jDay;
            seconds -= Constants.JULIAN_DAY;
        }
        return new AbsoluteDate(new DateComponents(DateComponents.JULIAN_EPOCH, jDay),
                                new TimeComponents(seconds), timeScale);
    }

    /** Extract a double from a record.
     * <p>Double numbers are stored according to IEEE 754 standard, with
     * most significant byte first.</p>
     * @param record record to parse
     * @param offset offset of the double within the record
     * @return extracted double
     */
    private double extractDouble(final byte[] record, final int offset) {
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
     * @return extracted int
     */
    private int extractInt(final byte[] record, final int offset) {
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
    private String extractString(final byte[] record, final int offset, final int length) {
        try {
            return new String(record, offset, length, "US-ASCII").trim();
        } catch (UnsupportedEncodingException uee) {
            throw OrekitException.createInternalError(uee);
        }
    }

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

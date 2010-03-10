/* Copyright 2002-2010 CS Communication & Systèmes
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

import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.TimeStamped;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

/** Loader for JPL ephemerides binary files (DE 405, DE 406).
 * <p>JPL ephemerides binary files contain ephemerides for all solar system planets.</p>
 * <p>The JPL ephemerides binary files are recognized thanks to their base names,
 * which must match the pattern <code>unx[mp]####.ddd</code> (or
 * <code>unx[mp]####.ddd.gz</code> for gzip-compressed files) where # stands for a
 * digit character and where ddd is an ephemeris type (typically 405 or 406).</p>
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class JPLEphemeridesLoader implements CelestialBodyLoader {

    /** Error message for no JPL files. */
    private static final String NO_JPL_FILES_FOUND =
        "no JPL ephemerides binary files found";

    /** Error message for header read error. */
    private static final String HEADER_READ_ERROR =
        "unable to read header record from JPL ephemerides binary file {0}";

    /** Error message for unsupported file. */
    private static final String NOT_JPL_EPHEMERIS =
        "file {0} is not a JPL ephemerides binary file";

    /** Error message for unsupported file. */
    private static final String OUT_OF_RANGE_DATE =
        "out of range date for ephemerides: {0}, [{1}, {2}]";

    /** Binary record size in bytes for DE 405. */
    private static final int DE405_RECORD_SIZE = 1018 * 8;

    /** Binary record size in bytes for DE 406. */
    private static final int DE406_RECORD_SIZE =  728 * 8;

    /** Default supported files name pattern. */
    private static final String DEFAULT_SUPPORTED_NAMES = "^unx[mp](\\d\\d\\d\\d)\\.(?:(?:405)|(?:406))$";

    /** 50 days in seconds. */
    private static final double FIFTY_DAYS = 50 * Constants.JULIAN_DAY;

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

    /** {@inheritDoc} */
    public CelestialBody loadCelestialBody(final String name) throws OrekitException {

        final String frameName = name + "/EME2000";
        final double gm = getLoadedGravitationalCoefficient(generateType);

        switch (generateType) {
        case SOLAR_SYSTEM_BARYCENTER :
            return new JPLCelestialBody(supportedNames, gm,
                                        CelestialBodyFactory.getEarthMoonBarycenter().getFrame(),
                                        frameName) {

                /** Serializable UID. */
                private static final long serialVersionUID = -949534646302786503L;

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
            return new JPLCelestialBody(supportedNames, gm,
                                        FramesFactory.getEME2000(), frameName) {

                /** Serializable UID. */
                private static final long serialVersionUID = -3710160379028246246L;

                /** {@inheritDoc} */
                public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
                    throws OrekitException {
                    // we define Earth-Moon barycenter with respect to Earth center so we need
                    // to apply a scale factor to the Moon vectors provided by the JPL DE 405 ephemerides
                    return new PVCoordinates(scale, super.getPVCoordinates(date, frame));
                }
            };
        case EARTH :
            return new AbstractCelestialBody(gm, FramesFactory.getEME2000()) {

                /** Serializable UID. */
                private static final long serialVersionUID = -6542444016613134811L;

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
        case MOON :
            return new JPLCelestialBody(supportedNames, gm,
                                        FramesFactory.getEME2000(), frameName);
        default :
            return new JPLCelestialBody(supportedNames, gm,
                                        CelestialBodyFactory.getSolarSystemBarycenter().getFrame(),
                                        frameName);
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

        return 1000.0 * getLoadedConstant("AU");

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

        return getLoadedConstant("EMRAT");

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
            throw new OrekitException(NO_JPL_FILES_FOUND);
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
        parseFirstHeaderRecord(record, name);

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
            throw new OrekitException(HEADER_READ_ERROR, name);
        }

        // read ephemerides data
        while (readInRecord(input, record, 0)) {
            parseDataRecord(record);
        }

    }

    /** Parse the first header record.
     * @param record first header record
     * @param name name of the file (or zip entry)
     * @exception OrekitException if the header is not a JPL ephemerides binary file header
     */
    private void parseFirstHeaderRecord(final byte[] record, final String name)
        throws OrekitException {

        // extract covered date range
        startEpoch = extractDate(record, 2652);
        finalEpoch = extractDate(record, 2660);
        boolean ok = finalEpoch.compareTo(startEpoch) > 0;

        // check astronomical unit consistency
        final double au = 1000 * extractDouble(record, 2680);
        ok = ok && (au > 1.4e11) && (au < 1.6e11);
        if (Math.abs(getLoadedAstronomicalUnit() - au) >= 0.001) {
            throw new OrekitException("inconsistent values of astronomical unit in JPL ephemerides files: ({0} and {1})",
                                      getLoadedAstronomicalUnit(), au);
        }

        final double emRat = extractDouble(record, 2688);
        ok = ok && (emRat > 80) && (emRat < 82);
        if (Math.abs(getLoadedEarthMoonMassRatio() - emRat) >= 1.0e-8) {
            throw new OrekitException("inconsistent values of Earth/Moon mass ratio in JPL ephemerides files: ({0} and {1})",
                                      getLoadedEarthMoonMassRatio(), emRat);
        }

        synchronized (this) {

            // indices of the Chebyshev coefficients for each ephemeris
            for (int i = 0; i < 12; ++i) {
                final int row1 = extractInt(record, 2696 + 12 * i);
                final int row2 = extractInt(record, 2700 + 12 * i);
                final int row3 = extractInt(record, 2704 + 12 * i);
                ok = ok && (row1 > 0) && (row2 >= 0) && (row3 >= 0);
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
            final double timeSpan = extractDouble(record, 2668);
            ok = ok && (timeSpan > 0) && (timeSpan < 100);
            chunksDuration = Constants.JULIAN_DAY * (timeSpan / chunks);
            if (Double.isNaN(maxChunksDuration)) {
                maxChunksDuration = chunksDuration;
            } else {
                maxChunksDuration = Math.max(maxChunksDuration, chunksDuration);
            }

        }

        // sanity checks
        if (!ok) {
            throw new OrekitException(NOT_JPL_EPHEMERIS, name);
        }

    }

    /** Parse regular ephemeris record.
     * @param record record to parse
     * @exception OrekitException if the header is not a JPL ephemerides binary file header
     */
    private void parseDataRecord(final byte[] record) throws OrekitException {

        // extract time range covered by the record
        final AbsoluteDate rangeStart = extractDate(record, 0);
        if (rangeStart.compareTo(startEpoch) < 0) {
            throw new OrekitException(OUT_OF_RANGE_DATE, rangeStart, startEpoch, finalEpoch);
        }

        final AbsoluteDate rangeEnd   = extractDate(record, 8);
        if (rangeEnd.compareTo(finalEpoch) > 0) {
            throw new OrekitException(OUT_OF_RANGE_DATE, rangeEnd, startEpoch, finalEpoch);
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
                    final int index = first + 3 * i * nbCoeffs + k - 1;
                    xCoeffs[k] = 1000.0 * extractDouble(record, 8 * index);
                    yCoeffs[k] = 1000.0 * extractDouble(record, 8 * (index +  nbCoeffs));
                    zCoeffs[k] = 1000.0 * extractDouble(record, 8 * (index + 2 * nbCoeffs));
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
        final byte[] firstPart = new byte[2844];
        if (!readInRecord(input, firstPart, 0)) {
            throw new OrekitException(HEADER_READ_ERROR, name);
        }

        // get the ephemeris type, deduce the record size
        int recordSize = 0;
        switch (extractInt(firstPart, 2840)) {
        case 405 :
            recordSize = DE405_RECORD_SIZE;
            break;
        case 406 :
            recordSize = DE406_RECORD_SIZE;
            break;
        default :
            throw new OrekitException(NOT_JPL_EPHEMERIS, name);
        }

        // build a record with the proper size and finish read of the first complete record
        final int start = firstPart.length;
        final byte[] record = new byte[recordSize];
        System.arraycopy(firstPart, 0, record, 0, firstPart.length);
        if (!readInRecord(input, record, start)) {
            throw new OrekitException(HEADER_READ_ERROR, name);
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

    /** Extract a date from a record.
     * @param record record to parse
     * @param offset offset of the double within the record
     * @return extracted date
     */
    private static AbsoluteDate extractDate(final byte[] record, final int offset) {
        final double dt = extractDouble(record, offset) * Constants.JULIAN_DAY;
        return new AbsoluteDate(AbsoluteDate.JULIAN_EPOCH, dt, TimeScalesFactory.getTT());
    }

    /** Extract a double from a record.
     * <p>Double numbers are stored according to IEEE 754 standard, with
     * most significant byte first.</p>
     * @param record record to parse
     * @param offset offset of the double within the record
     * @return extracted double
     */
    private static double extractDouble(final byte[] record, final int offset) {
        final long l8 = ((long) record[offset + 0]) & 0xffl;
        final long l7 = ((long) record[offset + 1]) & 0xffl;
        final long l6 = ((long) record[offset + 2]) & 0xffl;
        final long l5 = ((long) record[offset + 3]) & 0xffl;
        final long l4 = ((long) record[offset + 4]) & 0xffl;
        final long l3 = ((long) record[offset + 5]) & 0xffl;
        final long l2 = ((long) record[offset + 6]) & 0xffl;
        final long l1 = ((long) record[offset + 7]) & 0xffl;
        final long l = (l8 << 56) | (l7 << 48) | (l6 << 40) | (l5 << 32) |
                       (l4 << 24) | (l3 << 16) | (l2 <<  8) | l1;
        return Double.longBitsToDouble(l);
    }

    /** Extract an int from a record.
     * @param record record to parse
     * @param offset offset of the double within the record
     * @return extracted int
     */
    private static int extractInt(final byte[] record, final int offset) {
        final int l4 = ((int) record[offset + 0]) & 0xff;
        final int l3 = ((int) record[offset + 1]) & 0xff;
        final int l2 = ((int) record[offset + 2]) & 0xff;
        final int l1 = ((int) record[offset + 3]) & 0xff;
        return (l4 << 24) | (l3 << 16) | (l2 <<  8) | l1;
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
                throw new OrekitException(HEADER_READ_ERROR, name);
            }

            // constants defined in the file
            for (int i = 0; i < 400; ++i) {
                final String constantName = extractString(first, 252 + i * 6, 6);
                if (constantName.length() == 0) {
                    // no more constants to read
                    return;
                }
                final double constantValue = extractDouble(second, 8 * i);
                constants.put(constantName, constantValue);
            }

        }
    };

    /** Private CelestialBody class. */
    private class JPLCelestialBody extends AbstractCelestialBody {

        /** Serializable UID. */
        private static final long serialVersionUID = 7425624219901103158L;

        /** Current Chebyshev model. */
        private PosVelChebyshev model;

        /** Frame in which ephemeris are defined. */
        private final Frame definingFrame;

        /** Private constructor for the singletons.
         * @param supportedNames regular expression for supported files names (may be null)
         * @param gm attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
         * @param definingFrame frame in which ephemeris are defined
         * @param frameName name to use for the body-centered frame
         */
        private JPLCelestialBody(final String supportedNames, final double gm,
                                 final Frame definingFrame, final String frameName) {
            super(gm, frameName, definingFrame);
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
                throw new OrekitException(NO_JPL_FILES_FOUND);
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
            throw new OrekitException("out of range date for {0} ephemerides: {1}",
                                      loadType, date);

        }
    }

}

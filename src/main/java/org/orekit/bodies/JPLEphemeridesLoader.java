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

import java.io.IOException;
import java.io.InputStream;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.orekit.data.DataDirectoryCrawler;
import org.orekit.data.DataFileLoader;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TTScale;
import org.orekit.time.TimeStamped;

/** Loader for JPL ephemerides binary files (DE 405, DE 406).
 * <p>JPL ephemerides binary files contain ephemerides for all solar system planets.</p>
 * <p>The JPL ephemerides binary files are recognized thanks to their base names,
 * which must match the pattern <code>unx[mp]####.ddd</code> (or
 * <code>unx[mp]####.ddd.gz</code> for gzip-compressed files) where # stands for a
 * digit character and where ddd is an ephemeris type (typically 405 or 406).</p>
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
class JPLEphemeridesLoader implements DataFileLoader {

    /** Error message for header read error. */
    private static final String HEADER_READ_ERROR =
        "unable to read header record from JPL ephemerides binary file {0}";

    /** Error message for unsupported file. */
    private static final String NOT_JPL_EPHEMERIS =
        "file {0} is not a JPL ephemerides binary file";

    /** Error message for unsupported file. */
    private static final String OUT_OF_RANGE_DATE =
        "out of range date for ephemerides: {0}";

    /** Binary record size in bytes for DE 405. */
    private static final int DE405_RECORD_SIZE = 1018 * 8;

    /** Binary record size in bytes for DE 406. */
    private static final int DE406_RECORD_SIZE =  728 * 8;

    /** Supported files name pattern. */
    private Pattern namePattern;

    /** List of supported ephemerides types. */
    public enum EphemerisType {

        /** Constant for Mercury. */
        MERCURY,

        /** Constant for Venus. */
        VENUS,

        /** Constant for the Earth-Moon barycenter. */
        EARTH_MOON,

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
        PLUTO,

        /** Constant for the Moon. */
        MOON,

        /** Constant for the Sun. */
        SUN

    }

    /** Ephemeris type to load. */
    private final EphemerisType type;

    /** Desired central date. */
    private final AbsoluteDate centralDate;

    /** Ephemeris for selected body. */
    private SortedSet<TimeStamped> ephemerides;

    /** Current file start epoch. */
    private AbsoluteDate startEpoch;

    /** Current file final epoch. */
    private AbsoluteDate finalEpoch;

    /** Astronomical unit (in meters). */
    private double astronomicalUnit;

    /** Earth to Moon mass ratio. */
    private double earthMoonMassRatio;

    /** Chunks duration (in seconds). */
    private double maxChunksDuration;

    /** Index of the first data for selected body. */
    private int firstIndex;

    /** Number of coefficients for selected body. */
    private int coeffs;

    /** Number of chunks for the selected body. */
    private int chunks;

    /** Current record. */
    private byte[] record;

    /** Create a loader for JPL ephemerides binary files.
     * @param type ephemeris type to load
     * @param centralDate desired central date
     * (all data within a +/-50 days range around this date will be loaded)
     */
    public JPLEphemeridesLoader(final EphemerisType type, final AbsoluteDate centralDate) {
        namePattern = Pattern.compile("^unx[mp](\\d\\d\\d\\d)\\.(?:(?:405)|(?:406))$");
        this.type          = type;
        this.centralDate   = centralDate;
        astronomicalUnit   = Double.NaN;
        earthMoonMassRatio = Double.NaN;
        maxChunksDuration  = Double.NaN;
    }

    /** Load ephemerides.
     * <p>The data is concatenated from all JPL ephemerides files
     * which can be found in the configured data directories tree.</p>
     * @return a set of ephemerides (all contained elements are really
     * {@link PosVelChebyshev} instances)
     * @exception OrekitException if some data can't be read or some
     * file content is corrupted
     */
    public synchronized SortedSet<TimeStamped> loadEphemerides() throws OrekitException {
        ephemerides = new TreeSet<TimeStamped>(ChronologicalComparator.getInstance());
        new DataDirectoryCrawler().crawl(this);
        return ephemerides;
    }

    /** Get astronomical unit.
     * @return astronomical unit in meters
     */
    public double getAstronomicalUnit() {
        return astronomicalUnit;
    }

    /** Get Earth/Moon mass ration.
     * @return Earth/Moon mass ratio
     */
    public double getEarthMoonMassRatio() {
        return earthMoonMassRatio;
    }

    /** Get the maximal chunks duration.
     * @return chunks maximal duration in seconds
     */
    public double getMaxChunksDuration() {
        return maxChunksDuration;
    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
        throws OrekitException, IOException {

        // read first part of record, up to the ephemeris type
        record = new byte[2844];
        if (!readInRecord(input, 0)) {
            throw new OrekitException(HEADER_READ_ERROR,
                                      new Object[] {
                                          name
                                      });
        }

        // get the ephemeris type, deduce the record size
        int recordSize = 0;
        switch (extractInt(2840)) {
        case 405 :
            recordSize = DE405_RECORD_SIZE;
            break;
        case 406 :
            recordSize = DE406_RECORD_SIZE;
            break;
        default :
            throw new OrekitException(NOT_JPL_EPHEMERIS,
                                      new Object[] {
                                          name
                                      });
        }

        // build a record with the proper size and finish read of the first complete record
        final int start = record.length;
        final byte[] newRecord = new byte[recordSize];
        System.arraycopy(record, 0, newRecord, 0, record.length);
        record = newRecord;
        if (!readInRecord(input, start)) {
            throw new OrekitException(HEADER_READ_ERROR,
                                      new Object[] {
                                          name
                                      });
        }
        record = newRecord;

        // parse completed header record
        parseHeaderRecord(name);

        if (tooFarRange(startEpoch, finalEpoch)) {
            // this file does not cover a range we are interested in,
            // there is no need to parse it further
            return;
        }

        // the second record contains the values of the constants used for least-square filtering
        // we ignore all of them so don't do anything here
        if (!readInRecord(input, 0)) {
            throw new OrekitException(HEADER_READ_ERROR,
                                      new Object[] {
                                          name
                                      });
        }

        // read ephemerides data
        while (readInRecord(input, 0)) {
            parseDataRecord();
        }

    }

    /** Read bytes into the current record array.
     * @param input input stream
     * @param start start index where to put bytes
     * @return true if record has been filled up
     * @exception IOException if a read error occurs
     */
    private boolean readInRecord(final InputStream input, final int start)
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

    /** Check if a range is too far from the central date.
     * <p>"Too far" is considered to be either end more than one year
     * before central date or to start more than one year after central
     * date.</p>
     * @param start start date of the range
     * @param end end date of the range
     * @return true if the range is closer than one year to the central date
     */
    private boolean tooFarRange(final AbsoluteDate start, final AbsoluteDate end) {

        // 50 days in seconds
        final double fiftyDays = 50 * 86400;

        // check range bounds
        return (centralDate.durationFrom(end) > fiftyDays) ||
               (start.durationFrom(centralDate) > fiftyDays);

    }

    /** Parse the header record.
     * @param name name of the file (or zip entry)
     * @exception OrekitException if the header is not a JPL ephemerides binary file header
     */
    private void parseHeaderRecord(final String name) throws OrekitException {

        // extract covered date range
        startEpoch = extractDate(2652);
        finalEpoch = extractDate(2660);
        boolean ok = finalEpoch.compareTo(startEpoch) > 0;

        // check astronomical unit consistency
        final double au = 1000 * extractDouble(2680);
        ok = ok && (au > 1.4e11) && (au < 1.6e11);
        if (Double.isNaN(astronomicalUnit)) {
            astronomicalUnit = au;
        } else {
            if (Math.abs(astronomicalUnit - au) >= 0.001) {
                throw new OrekitException("inconsistent values of astronomical unit in JPL ephemerides files: ({0} and {1})",
                                          new Object[] {
                                              Double.valueOf(astronomicalUnit),
                                              Double.valueOf(au)
                                          });
            }
        }

        final double emRat = extractDouble(2688);
        ok = ok && (emRat > 80) && (emRat < 82);
        if (Double.isNaN(earthMoonMassRatio)) {
            earthMoonMassRatio = emRat;
        } else {
            if (Math.abs(earthMoonMassRatio - emRat) >= 1.0e-8) {
                throw new OrekitException("inconsistent values of Earth/Moon mass ratio in JPL ephemerides files: ({0} and {1})",
                                          new Object[] {
                                              Double.valueOf(earthMoonMassRatio),
                                              Double.valueOf(emRat)
                                          });
            }
        }

        // indices of the Chebyshev coefficients for each ephemeris
        for (int i = 0; i < 12; ++i) {
            final int row1 = extractInt(2696 + 12 * i);
            final int row2 = extractInt(2700 + 12 * i);
            final int row3 = extractInt(2704 + 12 * i);
            ok = ok && (row1 > 0) && (row2 >= 0) && (row3 >= 0);
            if (((i ==  0) && (type == EphemerisType.MERCURY))    ||
                ((i ==  1) && (type == EphemerisType.VENUS))      ||
                ((i ==  2) && (type == EphemerisType.EARTH_MOON)) ||
                ((i ==  3) && (type == EphemerisType.MARS))       ||
                ((i ==  4) && (type == EphemerisType.JUPITER))    ||
                ((i ==  5) && (type == EphemerisType.SATURN))     ||
                ((i ==  6) && (type == EphemerisType.URANUS))     ||
                ((i ==  7) && (type == EphemerisType.NEPTUNE))    ||
                ((i ==  8) && (type == EphemerisType.PLUTO))      ||
                ((i ==  9) && (type == EphemerisType.MOON))       ||
                ((i == 10) && (type == EphemerisType.SUN))) {
                firstIndex = row1;
                coeffs     = row2;
                chunks     = row3;
            }
        }

        // compute chunks duration
        final double timeSpan = extractDouble(2668);
        ok = ok && (timeSpan > 0) && (timeSpan < 100);
        final double cd = 86400.0 * (timeSpan / chunks);
        if (Double.isNaN(maxChunksDuration)) {
            maxChunksDuration = cd;
        } else {
            maxChunksDuration = Math.max(maxChunksDuration, cd);
        }

        // sanity checks
        if (!ok) {
            throw new OrekitException(NOT_JPL_EPHEMERIS,
                                      new Object[] {
                                          name
                                      });
        }

    }

    /** Parse regular ephemeris record.
     * @exception OrekitException if the header is not a JPL ephemerides binary file header
     */
    private void parseDataRecord() throws OrekitException {

        // extract time range covered by the record
        final AbsoluteDate rangeStart = extractDate(0);
        if (rangeStart.compareTo(startEpoch) < 0) {
            throw new OrekitException(OUT_OF_RANGE_DATE,
                                      new Object[] {
                                          rangeStart
                                      });
        }

        final AbsoluteDate rangeEnd   = extractDate(8);
        if (rangeEnd.compareTo(finalEpoch) > 0) {
            throw new OrekitException(OUT_OF_RANGE_DATE,
                                      new Object[] {
                                          rangeEnd
                                      });
        }

        if (tooFarRange(rangeStart, rangeEnd)) {
            // we are not interested in this record, don't parse it
            return;
        }

        // loop over chunks inside the time range
        AbsoluteDate chunkEnd = rangeStart;
        final int nbChunks    = chunks;
        final int nbCoeffs    = coeffs;
        final int first       = firstIndex;
        final double duration = maxChunksDuration;
        synchronized (this) {
            for (int i = 0; i < nbChunks; ++i) {

                // set up chunk validity range
                final AbsoluteDate chunkStart = chunkEnd;
                chunkEnd = (i == nbChunks - 1) ?
                        rangeEnd : new AbsoluteDate(rangeStart, (i + 1) * duration);

                // extract Chebyshev coefficients for the selected body
                // and convert them from kilometers to meters
                final double[] xCoeffs = new double[nbCoeffs];
                final double[] yCoeffs = new double[nbCoeffs];
                final double[] zCoeffs = new double[nbCoeffs];
                for (int k = 0; k < nbCoeffs; ++k) {
                    final int index = first + 3 * i * nbCoeffs + k - 1;
                    xCoeffs[k] = 1000.0 * extractDouble(8 * index);
                    yCoeffs[k] = 1000.0 * extractDouble(8 * (index +  nbCoeffs));
                    zCoeffs[k] = 1000.0 * extractDouble(8 * (index + 2 * nbCoeffs));
                }

                // build the position-velocity model for current chunk
                ephemerides.add(new PosVelChebyshev(chunkStart, duration,
                                                    xCoeffs, yCoeffs, zCoeffs));

            }
        }

    }

    /** Extract a date from a record.
     * @param offset offset of the double within the record
     * @return extracted date
     */
    private AbsoluteDate extractDate(final int offset) {
        final double dt = extractDouble(offset) * 86400;
        return new AbsoluteDate(AbsoluteDate.JULIAN_EPOCH, dt, TTScale.getInstance());
    }

    /** Extract a double from a record.
     * <p>Double numbers are stored according to IEEE 754 standard, with
     * most significant byte first.</p>
     * @param offset offset of the double within the record
     * @return extracted double
     */
    private double extractDouble(final int offset) {
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
     * @param offset offset of the double within the record
     * @return extracted int
     */
    private int extractInt(final int offset) {
        final int l4 = ((int) record[offset + 0]) & 0xff;
        final int l3 = ((int) record[offset + 1]) & 0xff;
        final int l2 = ((int) record[offset + 2]) & 0xff;
        final int l1 = ((int) record[offset + 3]) & 0xff;
        return (l4 << 24) | (l3 << 16) | (l2 <<  8) | l1;
    }

    /** {@inheritDoc} */
    public boolean fileIsSupported(final String fileName) {
        return namePattern.matcher(fileName).matches();
    }

}

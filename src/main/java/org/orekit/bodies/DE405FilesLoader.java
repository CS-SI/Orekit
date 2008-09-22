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
import java.io.Serializable;
import java.util.SortedSet;
import java.util.TreeSet;

import org.orekit.data.DataDirectoryCrawler;
import org.orekit.data.DataFileCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TTScale;
import org.orekit.time.TimeStamped;


/** Loader for JPL DE 405 ephemerides binary files.
 * <p>DE 405 binary files contain ephemerides for all solar system planets.</p>
 * <p>The DE 405 binary files are recognized thanks to their base names,
 * which must match the pattern <code>unxp####.405</code> (or
 * <code>unxp####.405.gz</code> for gzip-compressed files) where # stands for a
 * digit character.</p>
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
class DE405FilesLoader extends DataFileCrawler implements Serializable {

    /** Serializable version ID. */
    private static final long serialVersionUID = 1541118712237046123L;

    /** List of supported ephemerides types. */
    public enum EphemerisType {
        MERCURY, VENUS, EARTH_MOON, MARS, JUPITER,
        SATURN, URANUS, NEPTUNE, PLUTO, MOON, SUN
    }

    /** Binary record size in bytes. */
    private static final int RECORD_SIZE = 1018 * 8;

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

    /** Index of the first data for selected body. */
    private int firstIndex;

    /** Number of coefficients for selected body. */
    private int coeffs;

    /** Number of granules for the selected body. */
    private int granules;

    /** Current record. */
    private byte[] record;

    /** Create a loader for DE 405 binary files.
     * @param type ephemeris type to load
     * @param centralDate desired central date
     * (all data within a one year range from this date will be loaded)
     */
    public DE405FilesLoader(final EphemerisType type, final AbsoluteDate centralDate) {
        super("^unxp(\\d\\d\\d\\d)\\.405(?:\\.gz)?$");
        this.type          = type;
        this.centralDate   = centralDate;
        astronomicalUnit   = Double.NaN;
        earthMoonMassRatio = Double.NaN;
    }

    /** Load ephemerides.
     * <p>The data is concatenated from all DE 405 ephemerides files
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

    /** {@inheritDoc} */
    protected void visit(final InputStream input)
        throws OrekitException, IOException {

        record = new byte[RECORD_SIZE];

        // parse header record
        if (input.read(record) != RECORD_SIZE) {
            throw new OrekitException("unable to read header record from DE 405 ephemerides binary file {0}",
                                      new Object[] {
                                          getFile().getAbsolutePath()
                                      });
        }
        parseHeaderRecord();

        if (tooFarRange(startEpoch, finalEpoch)) {
            // this file does not cover a range we are interested in,
            // there is no need to parse it further
            return;
        }

        // the second record contains the values of the constants used for least-square filtering
        // we ignore all of them so don't do anything here
        input.read(record);

        // read ephemerides data
        while (input.read(record) == RECORD_SIZE) {
            parseDataRecord();
        }

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

        // one year in seconds
        final double oneYear = 365.25 * 86400;

        // check range bounds
        return (centralDate.durationFrom(end) > oneYear) ||
               (start.durationFrom(centralDate) > oneYear);

    }

    /** Parse the header record.
     * @exception OrekitException if the header is not a DE 405 binary file header
     */
    private void parseHeaderRecord() throws OrekitException {

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
            if (Math.abs(astronomicalUnit- au) >= 0.001) {
                throw new OrekitException("inconsistent values of astronomical unit in DE 405 files: ({0} and {1})",
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
            if (Math.abs(earthMoonMassRatio- emRat) >= 1.0e-8) {
                throw new OrekitException("inconsistent values of Earth/Moon mass ratio in DE 405 files: ({0} and {1})",
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
            ok = ok && (row1 > 0) && (row2 > 0) && (row3 > 0);
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
                granules   = row3;
            }
        }

        int deNumber = extractInt(2840);
        ok = ok && (deNumber == 405);

        // sanity checks
        if (!ok) {
            throw new OrekitException("file {0} is not a DE 405 ephemerides binary file",
                                      new Object[] {
                                          getFile().getAbsolutePath()
                                      });
        }

    }

    /** Parse regular ephemeris record.
     * @exception OrekitException if the header is not a DE 405 binary file header
     */
    private void parseDataRecord() throws OrekitException {

        // extract time range covered by the record
        final AbsoluteDate rangeStart = extractDate(0);
        final AbsoluteDate rangeEnd   = extractDate(8);
        if (tooFarRange(rangeStart, rangeEnd)) {
            // we are not interested in this record, don't parse it
            return;
        }

        // loop over granules inside the time range
        double granuleDuration = rangeEnd.durationFrom(rangeStart) / granules;
        AbsoluteDate granuleEnd = rangeStart;
        for (int i = 0; i < granules; ++i) {

            // set up granule validity range
            AbsoluteDate granuleStart = granuleEnd;
            granuleEnd = (i == granules - 1) ?
                         rangeEnd : new AbsoluteDate(rangeStart, (i + 1) * granuleDuration);

            // extract Chebyshev coefficients for the selected body
            // and convert them from kilometers to meters
            double[] xCoeffs = new double[coeffs];
            double[] yCoeffs = new double[coeffs];
            double[] zCoeffs = new double[coeffs];
            for (int k = 0; k < coeffs; ++k) {
                final int index = firstIndex + 3 * i * coeffs + k - 1;
                xCoeffs[k] = 1000.0 * extractDouble(8 * index);
                yCoeffs[k] = 1000.0 * extractDouble(8 * (index +  coeffs));
                zCoeffs[k] = 1000.0 * extractDouble(8 * (index + 2 * coeffs));
            }

            // build the position-velocity model for current granule
            ephemerides.add(new PosVelChebyshev(granuleStart, granuleDuration,
                                              xCoeffs, yCoeffs, zCoeffs));

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

}

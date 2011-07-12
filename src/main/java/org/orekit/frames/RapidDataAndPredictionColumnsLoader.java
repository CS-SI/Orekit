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
package org.orekit.frames;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.time.DateComponents;

/** Loader for IERS rapid data and prediction files in columns format (finals file).
 * <p>Rapid data and prediction files contain {@link TimeStampedEntry
 * Earth Orientation Parameters} for several years periods, in one file
 * only that is updated regularly.</p>
 * <p>
 * These files contain both the data from IERS Bulletin A and IERS bulletin B.
 * This class parses only the part from Bulletin A.
 * </p>
 * <p>The rapid data and prediction file is recognized thanks to its base name,
 * which must match one of the the patterns <code>finals.*</code> or
 * <code>finals2000A.*</code> (or the same ending with <code>.gz</code>
 * for gzip-compressed files) where * stands for a word like "all", "daily",
 * or "data". The file with 2000A in their name correspond to the
 * IAU-2000 precession-nutation model whereas the files without any identifier
 * correspond to the IAU-1980 precession-nutation model. The files with the all
 * suffix start from 1973-01-01, the file with the data suffix start
 * from 1992-01-01 and the files with the daily suffix.</p>
 * @author Romain Di Costanzo
 * @see <a href="http://maia.usno.navy.mil/ser7/readme.finals2000A">file format description at USNO</a>
 */
class RapidDataAndPredictionColumnsLoader implements EOP1980HistoryLoader, EOP2000HistoryLoader {

    /** Conversion factor. */
    private static final double  ARC_SECONDS_TO_RADIANS       = 2 * Math.PI / 1296000;

    /** Conversion factor. */
    private static final double  MILLI_ARC_SECONDS_TO_RADIANS = ARC_SECONDS_TO_RADIANS / 1000;

    /** Conversion factor. */
    private static final double  MILLI_SECONDS_TO_SECONDS     = 1.0e-3;

    /** Field for year, month and day parsing. */
    private static final String  INTEGER2_FIELD               = "((?:\\p{Blank}|\\p{Digit})\\p{Digit})";

    /** Field for modified Julian day parsing. */
    private static final String  MJD_FIELD                    = "\\p{Blank}+(\\p{Digit}+)(?:\\.00*)";

    /** Field for separator parsing. */
    private static final String  SEPARATOR                    = "\\p{Blank}*[IP]";

    /** Field for real parsing. */
    private static final String  REAL_FIELD                   = "\\p{Blank}*(-?\\p{Digit}*\\.\\p{Digit}*)";

    /** Start index of the date part of the line. */
    private static int DATE_START = 0;

    /** end index of the date part of the line. */
    private static int DATE_END   = 15;

    /** Pattern to match the date part of the line (always present). */
    private static final Pattern DATE_PATTERN = Pattern.compile(INTEGER2_FIELD + INTEGER2_FIELD + INTEGER2_FIELD + MJD_FIELD);

    /** Start index of the pole part of the line. */
    private static int POLE_START = 16;

    /** end index of the pole part of the line. */
    private static int POLE_END   = 55;

    /** Pattern to match the pole part of the line. */
    private static final Pattern POLE_PATTERN = Pattern.compile(SEPARATOR + REAL_FIELD + REAL_FIELD + REAL_FIELD + REAL_FIELD);

    /** Start index of the UT1-UTC part of the line. */
    private static int UT1_UTC_START = 57;

    /** end index of the UT1-UTC part of the line. */
    private static int UT1_UTC_END   = 78;

    /** Pattern to match the UT1-UTC part of the line. */
    private static final Pattern UT1_UTC_PATTERN = Pattern.compile(SEPARATOR + REAL_FIELD + REAL_FIELD);

    /** Start index of the LOD part of the line. */
    private static int LOD_START = 79;

    /** end index of the LOD part of the line. */
    private static int LOD_END   = 93;

    /** Pattern to match the LOD part of the line. */
    private static final Pattern LOD_PATTERN = Pattern.compile(REAL_FIELD + REAL_FIELD);

    /** Start index of the nutation part of the line. */
    private static int NUTATION_START = 95;

    /** end index of the nutation part of the line. */
    private static int NUTATION_END   = 134;

    /** Pattern to match the nutation part of the line. */
    private static final Pattern NUTATION_PATTERN = Pattern.compile(SEPARATOR + REAL_FIELD + REAL_FIELD + REAL_FIELD + REAL_FIELD);

    /** History entries for IAU1980. */
    private EOP1980History       history1980;

    /** History entries for IAU2000. */
    private EOP2000History       history2000;

    /** File supported name. */
    private String               supportedNames;

    /**
     * Build a loader for IERS bulletins B files.
     * 
     * @param supportedNames
     *            regular expression for supported files names
     */
    public RapidDataAndPredictionColumnsLoader(final String supportedNames) {
        this.supportedNames = supportedNames;
    }

    /** {@inheritDoc} */
    public boolean stillAcceptsData() {
        return true;
    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
        throws OrekitException, IOException {

        // set up a reader for line-oriented bulletin B files
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        // Init
        double x    = 0;
        double y    = 0;
        double dtu1 = 0;
        double lod  = 0;
        double dpsi = 0;
        double deps = 0;
        int date    = 0;
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {

            // split the lines in its various columns (some of them can be blank)
            final String datePart      = (line.length() >= DATE_END)     ? line.substring(DATE_START,       DATE_END)     : "";
            final String polePart      = (line.length() >= POLE_END)     ? line.substring(POLE_START,       POLE_END)     : "";
            final String ut1utcPart    = (line.length() >= UT1_UTC_END ) ? line.substring(UT1_UTC_START,    UT1_UTC_END)  : "";
            final String lodPart       = (line.length() >= LOD_END)      ? line.substring(LOD_START,        LOD_END)      : "";
            final String nutationPart  = (line.length() >= NUTATION_END) ? line.substring(NUTATION_START,   NUTATION_END) : "";

            // parse the date part
            final Matcher dateMatcher = DATE_PATTERN.matcher(datePart);
            if (dateMatcher.matches()) {
                final int yy = Integer.parseInt(dateMatcher.group(1).trim());
                final int mm = Integer.parseInt(dateMatcher.group(2).trim());
                final int dd = Integer.parseInt(dateMatcher.group(3).trim());
                final int yyyy = (yy > 70) ? yy + 1900 : yy + 2000;
                date = Integer.parseInt(dateMatcher.group(4).trim());
                final int reconstructedDate = new DateComponents(yyyy, mm, dd).getMJD();
                if (reconstructedDate != date) {
                    notifyUnexpectedErrorEncountered(name);
                }
            } else {
                notifyUnexpectedErrorEncountered(name);
            }

            // parse the pole part
            if (polePart.trim().length() == 0) {
                // pole part is blank
                x = 0;
                y = 0;
            } else {
                final Matcher poleMatcher = POLE_PATTERN.matcher(polePart);
                if (poleMatcher.matches()) {
                    x = ARC_SECONDS_TO_RADIANS * Double.parseDouble(poleMatcher.group(1));
                    y = ARC_SECONDS_TO_RADIANS * Double.parseDouble(poleMatcher.group(3));
                } else {
                    notifyUnexpectedErrorEncountered(name);
                }
            }

            // parse the UT1-UTC part
            if (ut1utcPart.trim().length() == 0) {
                // UT1-UTC part is blank
                dtu1 = 0;
            } else {
                final Matcher ut1utcMatcher = UT1_UTC_PATTERN.matcher(ut1utcPart);
                if (ut1utcMatcher.matches()) {
                    dtu1 = Double.parseDouble(ut1utcMatcher.group(1));
                } else {
                    notifyUnexpectedErrorEncountered(name);
                }
            }

            // parse the lod part
            if (lodPart.trim().length() == 0) {
                // lod part is blank
                lod = 0;
            } else {
                final Matcher lodMatcher = LOD_PATTERN.matcher(lodPart);
                if (lodMatcher.matches()) {
                    lod = MILLI_SECONDS_TO_SECONDS * Double.parseDouble(lodMatcher.group(1));
                } else {
                    notifyUnexpectedErrorEncountered(name);
                }
            }

            // parse the nutation part
            if (nutationPart.trim().length() == 0) {
                // nutation part is blank
                dpsi = 0;
                deps = 0;
            } else {
                final Matcher nutationMatcher = NUTATION_PATTERN.matcher(nutationPart);
                if (nutationMatcher.matches()) {
                    dpsi = MILLI_ARC_SECONDS_TO_RADIANS * Double.parseDouble(nutationMatcher.group(1));
                    deps = MILLI_ARC_SECONDS_TO_RADIANS * Double.parseDouble(nutationMatcher.group(3));
                } else {
                    notifyUnexpectedErrorEncountered(name);
                }
            }

            if (history1980 != null) {
                history1980.addEntry(new EOP1980Entry(date, dtu1, lod, dpsi, deps));
            }

            if (history2000 != null) {
                history2000.addEntry(new EOP2000Entry(date, dtu1, lod, x, y));
            }

        }
    }

    /** {@inheritDoc} */
    public void fillHistory(final EOP1980History history) throws OrekitException {
        synchronized (this) {
            history1980 = history;
            history2000 = null;
            DataProvidersManager.getInstance().feed(supportedNames, this);
        }
    }

    /** {@inheritDoc} */
    public void fillHistory(final EOP2000History history) throws OrekitException {
        synchronized (this) {
            history1980 = null;
            history2000 = history;
            DataProvidersManager.getInstance().feed(supportedNames, this);
        }
    }

    /** Throw an exception for an unexpected format error.
     * @param name name of the file (or zip entry)
     * @exception OrekitException always thrown to notify an unexpected error has been
     * encountered by the caller
     */
    private void notifyUnexpectedErrorEncountered(final String name) throws OrekitException {
        throw new OrekitException("file {0} is not a supported IERS data file", name);
    }

}

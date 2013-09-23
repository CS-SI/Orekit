/* Copyright 2002-2013 CS Systèmes d'Information
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
package org.orekit.frames;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.DateComponents;
import org.orekit.utils.Constants;

/** Loader for EOP 08 C04 files.
 * <p>EOP 08 C04 files contain {@link EOPEntry
 * Earth Orientation Parameters} consistent with ITRF2008 for one year periods.</p>
 * <p>The EOP 08 C04 files are recognized thanks to their base names, which
 * must match one of the the patterns <code>eopc04_08_IAU2000.##</code> or
 * <code>eopc04_08.##</code> (or the same ending with <code>.gz</code> for
 * gzip-compressed files) where # stands for a digit character.</p>
 * <p>Between 2002 and 2007, another series of Earth Orientation Parameters was
 * in use: EOPC04 (without the 08). These parameters were consistent with the
 * previous ITRS realization: ITRF2000.</p>
 * <p>Between 2008 and 20011, another series of Earth Orientation Parameters was
 * in use: EOP 05 C04 (instead of 08). These parameters were consistent with the
 * previous ITRS realization: ITRF2005.</p>
 * <p>These files are no longer provided by IERS and only the new files consistent
 * with ITRF 2008 are available now (as of early 2013). The content of the older
 * pre-2005 files is not the same as the content of the new files supported by this class,
 * however IERS uses the same file naming convention for all of them. If a file from the older
 * series is found by this class, a parse error will be triggered. Users must remove
 * such files to avoid being lured in believing they do have EOP data.</p>
 * <p>Files containing old data (back to 1962) have been regenerated in the new file
 * format and are available at IERS web site: <a
 * href="http://hpiers.obspm.fr/eoppc/eop/eopc04/">Index of /eoppc/eop/eopc04</a>.</p>
 * @author Luc Maisonobe
 */
class EOP08C04FilesLoader implements EOPHistoryEquinoxLoader, EOPHistoryNonRotatingOriginLoader {

    /** Pattern to match the columns header. */
    private static final Pattern COLUMNS_HEADER_PATTERN;

    /** Pattern for data lines. */
    private static final Pattern DATA_LINE_PATTERN;

    /** Year field. */
    private static final int YEAR_FIELD;

    /** Month field. */
    private static final int MONTH_FIELD;

    /** Day field. */
    private static final int DAY_FIELD;

    /** MJD field. */
    private static final int MJD_FIELD;

    /** X component of pole motion field. */
    private static final int POLE_X_FIELD;

    /** Y component of pole motion field. */
    private static final int POLE_Y_FIELD;

    /** UT1-UTC field. */
    private static final int UT1_UTC_FIELD;

    /** LoD field. */
    private static final int LOD_FIELD;

    /** Correction for nutation first field (either dX or dPsi). */
    private static final int NUT_0_FIELD;

    /** Correction for nutation second field (either dY or dEps). */
    private static final int NUT_1_FIELD;

    static {
        // Header have either the following form:
        //       Date      MJD      x          y        UT1-UTC       LOD         dPsi      dEps       x Err     y Err   UT1-UTC Err  LOD Err    dPsi Err   dEpsilon Err
        //                          "          "           s           s            "         "        "          "          s           s            "         "
        //      (0h UTC)
        // or the following form:
        //       Date      MJD      x          y        UT1-UTC       LOD         dX        dY        x Err     y Err   UT1-UTC Err  LOD Err     dX Err       dY Err
        //                          "          "           s           s          "         "           "          "          s         s            "           "
        //      (0h UTC)
        //
        COLUMNS_HEADER_PATTERN = Pattern.compile("^ *Date +MJD +x +y +UT1-UTC +LOD +((?:dPsi +dEps)|(?:dX +dY)) .*");

        // The data lines in the EOP 08 C04 yearly data files have the following fixed form:
        // year month day MJD ...12 floating values fields in decimal format...
        // 2000   1   1  51544   0.043242   0.377915   0.3554777   ...
        // 2000   1   2  51545   0.043515   0.377753   0.3546065   ...
        // 2000   1   3  51546   0.043623   0.377452   0.3538444   ...
        // the corresponding fortran format is:
        // 3(I4),I7,2(F11.6),2(F12.7),2(F12.6),2(F11.6),2(F12.7),2F12.6
        DATA_LINE_PATTERN = Pattern.compile("^\\d+ +\\d+ +\\d+ +\\d+(?: +-?\\d+\\.\\d+){12}$");

        YEAR_FIELD    = 0;
        MONTH_FIELD   = 1;
        DAY_FIELD     = 2;
        MJD_FIELD     = 3;
        POLE_X_FIELD  = 4;
        POLE_Y_FIELD  = 5;
        UT1_UTC_FIELD = 6;
        LOD_FIELD     = 7;
        NUT_0_FIELD   = 8;
        NUT_1_FIELD   = 9;

    }

    /** Regular expression for supported files names. */
    private final String supportedNames;

    /** History entries for equinox-based paradigm. */
    private Collection<? super EOPEntryEquinox> historyEquinox;

    /** History entries for Non-Rotating Origin paradigm. */
    private Collection<? super EOPEntryNonRotatingOrigin> historyNRO;

    /** Build a loader for IERS EOP 08 C04 files.
     * @param supportedNames regular expression for supported files names
     */
    public EOP08C04FilesLoader(final String supportedNames) {
        this.supportedNames = supportedNames;
    }

    /** {@inheritDoc} */
    public boolean stillAcceptsData() {
        return true;
    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
        throws IOException, OrekitException {

        // set up a reader for line-oriented bulletin B files
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));

        // read all file
        synchronized (this) {

            int lineNumber = 0;
            boolean inHeader = true;
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                ++lineNumber;
                boolean parsed = false;

                if (inHeader) {
                    final Matcher matcher = COLUMNS_HEADER_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        if (matcher.group(1).startsWith("dX") ^ (historyNRO != null)) {
                            // the file content does not match what we expect
                            throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, name);
                        }
                    }
                }

                if (DATA_LINE_PATTERN.matcher(line).matches()) {
                    inHeader = false;
                    // this is a data line, build an entry from the extracted fields
                    final String[] fields = line.split(" +");
                    final int    year  = Integer.parseInt(fields[YEAR_FIELD]);
                    final int    month = Integer.parseInt(fields[MONTH_FIELD]);
                    final int    day   = Integer.parseInt(fields[DAY_FIELD]);
                    final int    mjd   = Integer.parseInt(fields[MJD_FIELD]);
                    if (new DateComponents(year, month, day).getMJD() != mjd) {
                        throw new OrekitException(OrekitMessages.INCONSISTENT_DATES_IN_IERS_FILE,
                                                  name, year, month, day, mjd);
                    }

                    // the first six fields are consistent with the expected format
                    final double x    = Double.parseDouble(fields[POLE_X_FIELD]) * Constants.ARC_SECONDS_TO_RADIANS;
                    final double y    = Double.parseDouble(fields[POLE_Y_FIELD]) * Constants.ARC_SECONDS_TO_RADIANS;
                    final double dtu1 = Double.parseDouble(fields[UT1_UTC_FIELD]);
                    final double lod  = Double.parseDouble(fields[LOD_FIELD]);
                    final double nut0 = Double.parseDouble(fields[NUT_0_FIELD]) * Constants.ARC_SECONDS_TO_RADIANS;
                    final double nut1 = Double.parseDouble(fields[NUT_1_FIELD]) * Constants.ARC_SECONDS_TO_RADIANS;
                    if (historyEquinox != null) {
                        historyEquinox.add(new EOPEntryEquinox(mjd, dtu1, lod, x, y, nut0, nut1));
                    }
                    if (historyNRO != null) {
                        historyNRO.add(new EOPEntryNonRotatingOrigin(mjd, dtu1, lod, x, y, nut0, nut1));
                    }
                    parsed = true;

                }
                if (!(inHeader || parsed)) {
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              lineNumber, name, line);
                }
            }

            // check if we have read something
            if (inHeader) {
                throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, name);
            }

        }

    }

    /** {@inheritDoc} */
    public void fillHistoryEquinox(final Collection<? super EOPEntryEquinox> history)
        throws OrekitException {
        synchronized (this) {
            historyEquinox = history;
            historyNRO     = null;
            DataProvidersManager.getInstance().feed(supportedNames, this);
        }
    }

    /** {@inheritDoc} */
    public void fillHistoryNonRotatingOrigin(final Collection<? super EOPEntryNonRotatingOrigin> history)
        throws OrekitException {
        synchronized (this) {
            historyEquinox = null;
            historyNRO     = history;
            DataProvidersManager.getInstance().feed(supportedNames, this);
        }
    }


}

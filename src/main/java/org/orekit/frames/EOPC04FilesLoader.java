/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.IERSConventions.NutationCorrectionConverter;

/** Loader for EOP xx C04 files.
 * <p>EOP xx C04 files contain {@link EOPEntry
 * Earth Orientation Parameters} consistent with ITRF20xx for one year periods, with various
 * xx (05, 08, 14) depending on the data source.</p>
 * <p>The EOP xx C04 files retrieved from the ftp site
 * <a href="ftp://ftp.iers.org/products/eop/long-term/">ftp://ftp.iers.org/products/eop/long-term/</a>
 * are recognized thanks to their base names, which must match one of the patterns
 * {@code eopc04_##_IAU2000.##} or {@code eopc04_##.##} (or the same ending with <code>.gz</code> for
 * gzip-compressed files) where # stands for a digit character.</p>
 * <p>
 * Beware that files retrieved from the web site
 * <a href="http://hpiers.obspm.fr/eoppc/eop/">http://hpiers.obspm.fr/eoppc/eop/</a> do
 * <em>not</em> follow the same naming convention. They lack the _05, _08 or _14
 * marker (and for EOP 14 C04 even the directory does not have this marker anymore...),
 * so all the files in this site have the same name and can easily be confused. This is the reason
 * why the default regular expression in {@link FramesFactory} does not recognize them and
 * enforces the year marker presence.
 * </p>
 * <p>Between 2002 and 2007, another series of Earth Orientation Parameters was
 * in use: EOPC04 (without the _##). These parameters were consistent with the
 * previous ITRS realization: ITRF2000.</p>
 * <p>Since 2008, several series of Earth Orientation Parameters have been available:
 * EOP 05 C04 consistent with ITRF 2005, EOP 08 C04 consistent with ITRF 2008, and
 * EOP 14 C04 consistent with ITRF 2014. As of mid 2017, only the EOP 08 C04 and
 * EOP 14 C04 are updated for current years.</p>
 * <p>Files are available at IERS FTP site: <a
 * href="ftp://ftp.iers.org/products/eop/long-term/">ftp://ftp.iers.org/products/eop/long-term/</a>.</p>
 * <p>
 * This class is immutable and hence thread-safe
 * </p>
 * @author Luc Maisonobe
 */
class EOPC04FilesLoader extends AbstractEopLoader implements EOPHistoryLoader {

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

        // The data lines in the EOP C04 yearly data files have the following fixed form:
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

    /** Build a loader for IERS EOP C04 files.
     * @param supportedNames regular expression for supported files names
     * @param manager provides access to the EOP C04 files.
     * @param utcSupplier UTC time scale.
     */
    EOPC04FilesLoader(final String supportedNames,
                      final DataProvidersManager manager,
                      final Supplier<TimeScale> utcSupplier) {
        super(supportedNames, manager, utcSupplier);
    }

    /** {@inheritDoc} */
    public void fillHistory(final IERSConventions.NutationCorrectionConverter converter,
                            final SortedSet<EOPEntry> history) {
        final ItrfVersionProvider itrfVersionProvider = new ITRFVersionLoader(
                ITRFVersionLoader.SUPPORTED_NAMES,
                getDataProvidersManager());
        final Parser parser = new Parser(converter, itrfVersionProvider, getUtc());
        final EopParserLoader loader = new EopParserLoader(parser);
        this.feed(loader);
        history.addAll(loader.getEop());
    }

    /** Internal class performing the parsing. */
    static class Parser extends AbstractEopParser {

        /**
         * Simple constructor.
         *
         * @param converter           converter to use
         * @param itrfVersionProvider to use for determining the ITRF version of the EOP.
         * @param utc                 time scale for parsing dates.
         */
        Parser(final NutationCorrectionConverter converter,
               final ItrfVersionProvider itrfVersionProvider,
               final TimeScale utc) {
            super(converter, itrfVersionProvider, utc);
        }

        /** {@inheritDoc} */
        public Collection<EOPEntry> parse(final InputStream input, final String name)
            throws IOException, OrekitException {

            final List<EOPEntry> history = new ArrayList<>();
            ITRFVersionLoader.ITRFVersionConfiguration configuration = null;

            // set up a reader for line-oriented bulletin B files
            final BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

            // reset parse info to start new file (do not clear history!)
            int lineNumber              = 0;
            boolean inHeader            = true;
            boolean isNonRotatingOrigin = false;

            // read all file
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                ++lineNumber;
                boolean parsed = false;

                if (inHeader) {
                    final Matcher matcher = COLUMNS_HEADER_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        if (matcher.group(1).startsWith("dX")) {
                            isNonRotatingOrigin = true;
                        }
                    }
                }

                if (DATA_LINE_PATTERN.matcher(line).matches()) {
                    inHeader = false;
                    // this is a data line, build an entry from the extracted fields
                    final String[] fields = line.split(" +");
                    final DateComponents dc = new DateComponents(Integer.parseInt(fields[YEAR_FIELD]),
                                                                 Integer.parseInt(fields[MONTH_FIELD]),
                                                                 Integer.parseInt(fields[DAY_FIELD]));
                    final int    mjd   = Integer.parseInt(fields[MJD_FIELD]);
                    if (dc.getMJD() != mjd) {
                        throw new OrekitException(OrekitMessages.INCONSISTENT_DATES_IN_IERS_FILE,
                                                  name, dc.getYear(), dc.getMonth(), dc.getDay(), mjd);
                    }
                    final AbsoluteDate date = new AbsoluteDate(dc, getUtc());

                    // the first six fields are consistent with the expected format
                    final double x     = Double.parseDouble(fields[POLE_X_FIELD]) * Constants.ARC_SECONDS_TO_RADIANS;
                    final double y     = Double.parseDouble(fields[POLE_Y_FIELD]) * Constants.ARC_SECONDS_TO_RADIANS;
                    final double dtu1  = Double.parseDouble(fields[UT1_UTC_FIELD]);
                    final double lod   = Double.parseDouble(fields[LOD_FIELD]);
                    final double[] equinox;
                    final double[] nro;
                    if (isNonRotatingOrigin) {
                        nro = new double[] {
                            Double.parseDouble(fields[NUT_0_FIELD]) * Constants.ARC_SECONDS_TO_RADIANS,
                            Double.parseDouble(fields[NUT_1_FIELD]) * Constants.ARC_SECONDS_TO_RADIANS
                        };
                        equinox = getConverter().toEquinox(date, nro[0], nro[1]);
                    } else {
                        equinox = new double[] {
                            Double.parseDouble(fields[NUT_0_FIELD]) * Constants.ARC_SECONDS_TO_RADIANS,
                            Double.parseDouble(fields[NUT_1_FIELD]) * Constants.ARC_SECONDS_TO_RADIANS
                        };
                        nro = getConverter().toNonRotating(date, equinox[0], equinox[1]);
                    }
                    if (configuration == null || !configuration.isValid(mjd)) {
                        // get a configuration for current name and date range
                        configuration = getItrfVersionProvider().getConfiguration(name, mjd);
                    }
                    history.add(new EOPEntry(mjd, dtu1, lod, x, y, equinox[0], equinox[1], nro[0], nro[1],
                                             configuration.getVersion(), date));
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

            return history;
        }

    }

}

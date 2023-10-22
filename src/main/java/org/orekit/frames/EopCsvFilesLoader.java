/* Copyright 2023 Luc Maisonobe
 * Licensed to CS GROUP (CS) under one or more
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

import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScale;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.IERSConventions.NutationCorrectionConverter;
import org.orekit.utils.units.Unit;

/** Loader for EOP csv files (can be bulletin A, bulletin B, EOP C04…).
 * <p>
 * This class is immutable and hence thread-safe
 * </p>
 * @author Luc Maisonobe
 * @since 12.0
 */
class EopCsvFilesLoader extends AbstractEopLoader implements EopHistoryLoader {

    /** Separator. */
    private static final String SEPARATOR = ";";

    /** Header for MJD. */
    private static final String MJD = "MJD";

    /** Header for Year. */
    private static final String YEAR = "Year";

    /** Header for Month. */
    private static final String MONTH = "Month";

    /** Header for Day. */
    private static final String DAY = "Day";

    /** Header for x_pole. */
    private static final String X_POLE = "x_pole";

    /** Header for y_pole. */
    private static final String Y_POLE = "y_pole";

    /** Header for x_rate. */
    private static final String X_RATE = "x_rate";

    /** Header for y_rate. */
    private static final String Y_RATE = "y_rate";

    /** Header for UT1-UTC. */
    private static final String UT1_UTC = "UT1-UTC";

    /** Header for LOD. */
    private static final String LOD = "LOD";

    /** Header for dPsi. */
    private static final String DPSI = "dPsi";

    /** Header for dEpsilon. */
    private static final String DEPSILON = "dEpsilon";

    /** Header for dX. */
    private static final String DX = "dX";

    /** Header for dY. */
    private static final String DY = "dY";

    /** Converter for milliarcseconds. */
    private static final Unit MAS = Unit.parse("mas");

    /** Converter for milliarcseconds per day. */
    private static final Unit MAS_D = Unit.parse("mas/day");

    /** Converter for milliseconds. */
    private static final Unit MS = Unit.parse("ms");

    /** Build a loader for IERS EOP csv files.
     * @param supportedNames regular expression for supported files names
     * @param manager provides access to the EOP C04 files.
     * @param utcSupplier UTC time scale.
     */
    EopCsvFilesLoader(final String supportedNames,
                      final DataProvidersManager manager,
                      final Supplier<TimeScale> utcSupplier) {
        super(supportedNames, manager, utcSupplier);
    }

    /** {@inheritDoc} */
    public void fillHistory(final IERSConventions.NutationCorrectionConverter converter,
                            final SortedSet<EOPEntry> history) {
        final Parser parser = new Parser(converter, getUtc());
        final EopParserLoader loader = new EopParserLoader(parser);
        this.feed(loader);
        history.addAll(loader.getEop());
    }

    /** Internal class performing the parsing. */
    class Parser extends AbstractEopParser {

        /** Configuration for ITRF versions. */
        private final ItrfVersionProvider itrfVersionProvider;

        /** Column number for MJD field. */
        private int mjdColumn;

        /** Column number for year field. */
        private int yearColumn;

        /** Column number for month field. */
        private int monthColumn;

        /** Column number for day field. */
        private int dayColumn;

        /** Column number for X pole field. */
        private int xPoleColumn;

        /** Column number for Y pole field. */
        private int yPoleColumn;

        /** Column number for X rate pole field. */
        private int xRatePoleColumn;

        /** Column number for Y rate pole field. */
        private int yRatePoleColumn;

        /** Column number for UT1-UTC field. */
        private int ut1Column;

        /** Column number for LOD field. */
        private int lodColumn;

        /** Column number for dX field. */
        private int dxColumn;

        /** Column number for dY field. */
        private int dyColumn;

        /** Column number for dPsi field. */
        private int dPsiColumn;

        /** Column number for dEpsilon field. */
        private int dEpsilonColumn;

        /** ITRF version configuration. */
        private ITRFVersionLoader.ITRFVersionConfiguration configuration;

        /** Simple constructor.
         * @param converter converter to use
         * @param utc       time scale for parsing dates.
         */
        Parser(final NutationCorrectionConverter converter,
               final TimeScale utc) {
            super(converter, null, utc);
            this.itrfVersionProvider = new ITRFVersionLoader(ITRFVersionLoader.SUPPORTED_NAMES,
                                                             getDataProvidersManager());
        }

        /** {@inheritDoc} */
        public Collection<EOPEntry> parse(final InputStream input, final String name)
            throws IOException, OrekitException {

            final List<EOPEntry> history = new ArrayList<>();

            // set up a reader for line-oriented csv files
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                // reset parse info to start new file (do not clear history!)
                int lineNumber = 0;
                configuration  = null;

                // read all file
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    ++lineNumber;

                    final boolean parsed;
                    if (lineNumber == 1) {
                        parsed = parseHeaderLine(line);
                    } else {
                        history.add(parseDataLine(line, name));
                        parsed = true;
                    }

                    if (!parsed) {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                lineNumber, name, line);
                    }
                }

                // check if we have read something
                if (lineNumber < 2) {
                    throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, name);
                }
            }

            return history;
        }

        /** Parse the header line.
         * @param headerLine header line
         * @return true if line was parsed correctly
         */
        private boolean parseHeaderLine(final String headerLine) {

            // reset columns numbers
            mjdColumn       = -1;
            yearColumn      = -1;
            monthColumn     = -1;
            dayColumn       = -1;
            xPoleColumn     = -1;
            yPoleColumn     = -1;
            xRatePoleColumn = -1;
            yRatePoleColumn = -1;
            ut1Column       = -1;
            lodColumn       = -1;
            dxColumn        = -1;
            dyColumn        = -1;
            dPsiColumn      = -1;
            dEpsilonColumn  = -1;

            // split header fields
            final String[] fields = headerLine.split(SEPARATOR);

            // affect column numbers according to header fields
            for (int column = 0; column < fields.length; ++column) {
                switch (fields[column]) {
                    case MJD :
                        mjdColumn = column;
                        break;
                    case YEAR :
                        yearColumn = column;
                        break;
                    case MONTH :
                        monthColumn = column;
                        break;
                    case DAY :
                        dayColumn = column;
                        break;
                    case X_POLE :
                        xPoleColumn = column;
                        break;
                    case Y_POLE :
                        yPoleColumn = column;
                        break;
                    case X_RATE :
                        xRatePoleColumn = column;
                        break;
                    case Y_RATE :
                        yRatePoleColumn = column;
                        break;
                    case UT1_UTC :
                        ut1Column = column;
                        break;
                    case LOD :
                        lodColumn = column;
                        break;
                    case DX :
                        dxColumn = column;
                        break;
                    case DY :
                        dyColumn = column;
                        break;
                    case DPSI :
                        dPsiColumn = column;
                        break;
                    case DEPSILON :
                        dEpsilonColumn = column;
                        break;
                    default :
                        // ignored column
                }
            }

            // check all required files are present (we just allow pole rates to be missing)
            return mjdColumn >= 0 && yearColumn >= 0 && monthColumn >= 0 && dayColumn >= 0 &&
                   xPoleColumn >= 0 && yPoleColumn >= 0 && ut1Column >= 0 && lodColumn >= 0 &&
                   (dxColumn >= 0 && dyColumn >= 0 || dPsiColumn >= 0 && dEpsilonColumn >= 0);

        }

        /** Parse a data line.
         * @param line line to parse
         * @param name file name (for error messages)
         * @return parsed entry
         */
        private EOPEntry parseDataLine(final String line, final String name) {

            final String[] fields = line.split(SEPARATOR);

            // check date
            final DateComponents dc = new DateComponents(Integer.parseInt(fields[yearColumn]),
                                                         Integer.parseInt(fields[monthColumn]),
                                                         Integer.parseInt(fields[dayColumn]));
            final int    mjd   = Integer.parseInt(fields[mjdColumn]);
            if (dc.getMJD() != mjd) {
                throw new OrekitException(OrekitMessages.INCONSISTENT_DATES_IN_IERS_FILE,
                                          name, dc.getYear(), dc.getMonth(), dc.getDay(), mjd);
            }
            final AbsoluteDate date = new AbsoluteDate(dc, getUtc());

            if (configuration == null || !configuration.isValid(mjd)) {
                // get a configuration for current name and date range
                configuration = itrfVersionProvider.getConfiguration(name, mjd);
            }

            final double x     = parseField(fields, xPoleColumn,     MAS);
            final double y     = parseField(fields, yPoleColumn,     MAS);
            final double xRate = parseField(fields, xRatePoleColumn, MAS_D);
            final double yRate = parseField(fields, yRatePoleColumn, MAS_D);
            final double dtu1  = parseField(fields, ut1Column,       MS);
            final double lod   = parseField(fields, lodColumn,       MS);

            if (dxColumn >= 0) {
                // non-rotatin origin paradigm
                final double dx = parseField(fields, dxColumn, MAS);
                final double dy = parseField(fields, dyColumn, MAS);
                final double[] equinox = getConverter().toEquinox(date, dx, dy);
                return new EOPEntry(dc.getMJD(), dtu1, lod, x, y, xRate, yRate,
                                    equinox[0], equinox[1], dx, dy,
                                    configuration.getVersion(), date);
            } else {
                // equinox paradigm
                final double ddPsi      = parseField(fields, dPsiColumn,     MAS);
                final double dddEpsilon = parseField(fields, dEpsilonColumn, MAS);
                final double[] nro = getConverter().toNonRotating(date, ddPsi, dddEpsilon);
                return new EOPEntry(dc.getMJD(), dtu1, lod, x, y, xRate, yRate,
                                    ddPsi, dddEpsilon, nro[0], nro[1],
                                    configuration.getVersion(), date);
            }


        }

        /** Parse one field.
         * @param fields fields array to parse
         * @param index index in the field array (negative for ignored fields)
         * @param unit field unit
         * @return parsed and converted field
         */
        private double parseField(final String[] fields, final int index, final Unit unit) {
            return (index < 0 || index >= fields.length || fields[index].isEmpty()) ?
                   Double.NaN :
                   unit.toSI(Double.parseDouble(fields[index]));
        }

    }

}

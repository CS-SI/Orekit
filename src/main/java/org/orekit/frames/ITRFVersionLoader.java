/* Copyright 2002-2023 CS GROUP
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.DateComponents;

/** Loader for ITRF version configuration file.
 * <p>
 * The ITRF version configuration file specifies
 * the {@link ITRFVersion ITRF versions} that each
 * type of Earth Orientation Parameter file contains
 * for each date. This configuration file is used to
 * interpret {@link EopC04FilesLoader EOP C04} files,
 * {@link BulletinAFilesLoader Bulletin A} files,
 * {@link BulletinBFilesLoader Bulletin B} files,
 * {@link RapidDataAndPredictionColumnsLoader rapid data
 * and prediction files in columns format} files,
 * {@link EopXmlLoader rapid data
 * and prediction files in XML format} files...
 * </p>
 * <p>This file is an Orekit-specific configuration file.
 * </p>
 * <p>
 * This class is immutable and hence thread-safe
 * </p>
 * @see EopC04FilesLoader
 * @see BulletinAFilesLoader
 * @see BulletinBFilesLoader
 * @see RapidDataAndPredictionColumnsLoader
 * @see EopXmlLoader
 * @author Luc Maisonobe
 * @since 9.2
 */
public class ITRFVersionLoader implements ItrfVersionProvider {

    /** Regular expression for supported files names. */
    public static final String SUPPORTED_NAMES = "itrf-versions.conf";

    /** Default entry to use if no suitable configuration is found. */
    private static final ITRFVersionConfiguration DEFAULT =
                    new ITRFVersionConfiguration("", ITRFVersion.ITRF_2014,
                                                 Integer.MIN_VALUE, Integer.MAX_VALUE);

    /** Configuration. */
    private final List<ITRFVersionConfiguration> configurations;

    /**
     * Build a loader for ITRF version configuration file. This constructor uses the
     * {@link DataContext#getDefault() default data context}.
     *
     * @param supportedNames regular expression for supported files names
     * @see #ITRFVersionLoader(String, DataProvidersManager)
     */
    @DefaultDataContext
    public ITRFVersionLoader(final String supportedNames) {
        this(supportedNames, DataContext.getDefault().getDataProvidersManager());
    }

    /**
     * Build a loader for ITRF version configuration file.
     *
     * @param supportedNames       regular expression for supported files names
     * @param dataProvidersManager provides access to the {@code itrf-versions.conf}
     *                             file.
     */
    public ITRFVersionLoader(final String supportedNames,
                             final DataProvidersManager dataProvidersManager) {
        this.configurations = new ArrayList<>();
        dataProvidersManager.feed(supportedNames, new Parser());
    }

    /**
     * Build a loader for ITRF version configuration file using the default name. This
     * constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @see #ITRFVersionLoader(String)
     * @see #ITRFVersionLoader(String, DataProvidersManager)
     * @see #SUPPORTED_NAMES
     */
    @DefaultDataContext
    public ITRFVersionLoader() {
        this(SUPPORTED_NAMES);
    }

    @Override
    public ITRFVersionConfiguration getConfiguration(final String name, final int mjd) {

        for (final ITRFVersionConfiguration configuration : configurations) {
            if (configuration.appliesTo(name) && configuration.isValid(mjd)) {
                // we have found a matching configuration
                return configuration;
            }
        }

        // no suitable configuration found, use the default value
        return DEFAULT;

    }

    /** Internal class performing the parsing. */
    private class Parser implements DataLoader {

        /** Regular expression matching start of line. */
        private static final String START  = "^";

        /** Regular expression matching a non-blank field (for names regexp). */
        private static final String NON_BLANK_FIELD = "(\\S+)";

        /** Regular expression matching a calendar date. */
        private static final String CALENDAR_DATE  = "\\s+(\\d\\d\\d\\d)-(\\d\\d)-(\\d\\d)";

        /** Regular expression matching a date at infinity. */
        private static final String INFINITY_DATE  = "\\s+-+";

        /** Regular expression matching an ITRF version. */
        private static final String ITRF  = "\\s+([Ii][Tt][Rr][Ff][-_ ]?[0-9]{2,4})";

        /** Regular expression matching end of line. */
        private static final String END  = "$";

        /** {@inheritDoc} */
        public boolean stillAcceptsData() {
            return configurations.isEmpty();
        }

        /** {@inheritDoc} */
        public void loadData(final InputStream input, final String name)
            throws IOException {

            // regular expressions for date lines
            final Pattern patternII = Pattern.compile(START + NON_BLANK_FIELD + INFINITY_DATE + INFINITY_DATE + ITRF + END);
            final Pattern patternID = Pattern.compile(START + NON_BLANK_FIELD + INFINITY_DATE + CALENDAR_DATE + ITRF + END);
            final Pattern patternDI = Pattern.compile(START + NON_BLANK_FIELD + CALENDAR_DATE + INFINITY_DATE + ITRF + END);
            final Pattern patternDD = Pattern.compile(START + NON_BLANK_FIELD + CALENDAR_DATE + CALENDAR_DATE + ITRF + END);


            int lineNumber =  0;
            String line = null;
            // set up a reader for line-oriented bulletin A files
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                for (line = reader.readLine(); line != null; line = reader.readLine()) {

                    lineNumber++;
                    line = line.trim();
                    if (!(line.startsWith("#") || line.isEmpty())) {
                        String prefix       = null;
                        ITRFVersion version = null;
                        int validityStart   = Integer.MIN_VALUE;
                        int validityEnd     = Integer.MAX_VALUE;
                        final Matcher matcherII = patternII.matcher(line);
                        if (matcherII.matches()) {
                            // both start and end of validity are at infinity
                            // the ITRF version applies throughout history
                            prefix  = matcherII.group(1);
                            version = ITRFVersion.getITRFVersion(matcherII.group(2));
                        } else {
                            final Matcher matcherID = patternID.matcher(line);
                            if (matcherID.matches()) {
                                // both start of validity is at infinity
                                // the ITRF version applies in the far past
                                prefix      = matcherID.group(1);
                                validityEnd = new DateComponents(Integer.parseInt(matcherID.group(2)),
                                                                 Integer.parseInt(matcherID.group(3)),
                                                                 Integer.parseInt(matcherID.group(4))).getMJD();
                                version     = ITRFVersion.getITRFVersion(matcherID.group(5));
                            } else {
                                final Matcher matcherDI = patternDI.matcher(line);
                                if (matcherDI.matches()) {
                                    // both end of validity is at infinity
                                    // the ITRF version applies to the upcoming future
                                    prefix        = matcherDI.group(1);
                                    validityStart = new DateComponents(Integer.parseInt(matcherDI.group(2)),
                                                                       Integer.parseInt(matcherDI.group(3)),
                                                                       Integer.parseInt(matcherDI.group(4))).getMJD();
                                    version       = ITRFVersion.getITRFVersion(matcherDI.group(5));
                                } else {
                                    final Matcher matcherDD = patternDD.matcher(line);
                                    if (matcherDD.matches()) {
                                        // the ITRF version applies during a limited range
                                        prefix        = matcherDD.group(1);
                                        validityStart = new DateComponents(Integer.parseInt(matcherDD.group(2)),
                                                                           Integer.parseInt(matcherDD.group(3)),
                                                                           Integer.parseInt(matcherDD.group(4))).getMJD();
                                        validityEnd   = new DateComponents(Integer.parseInt(matcherDD.group(5)),
                                                                           Integer.parseInt(matcherDD.group(6)),
                                                                           Integer.parseInt(matcherDD.group(7))).getMJD();
                                        version       = ITRFVersion.getITRFVersion(matcherDD.group(8));
                                    } else {
                                        // data line was not recognized
                                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                                  lineNumber, name, line);
                                    }
                                }
                            }
                        }
                        // error if prefix contains / or \ since these will never match
                        // CHECKSTYLE: stop MultipleStringLiterals check
                        if (prefix.contains("\\") || prefix.contains("/")) {
                            throw new OrekitException(
                                    OrekitMessages.ITRF_VERSIONS_PREFIX_ONLY, prefix);
                        }
                        // CHECKSTYLE: resume MultipleStringLiterals check
                        // store the parsed entry
                        configurations.add(new ITRFVersionConfiguration(prefix, version, validityStart, validityEnd));

                    }

                }
            } catch (IllegalArgumentException e) {
                throw new OrekitException(e,
                                          OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          lineNumber, name, line);
            }

        }

    }

    /** ITRF version configuration entry. */
    public static class ITRFVersionConfiguration {

        /** File names to which this configuration applies. */
        private final String prefix;

        /** ITRF version. */
        private final ITRFVersion version;

        /** Start of validity. */
        private final int validityStart;

        /** End of validity. */
        private final int validityEnd;

        /** Simple constructor.
         * @param prefix file names to which this configuration applies
         * @param version ITRF version
         * @param validityStart start of validity MJD (included)
         * @param validityEnd end of validity MJD (excluded)
         */
        public ITRFVersionConfiguration(final String prefix,
                                        final ITRFVersion version,
                                        final int validityStart,
                                        final int validityEnd) {
            this.prefix        = prefix;
            this.version       = version;
            this.validityStart = validityStart;
            this.validityEnd   = validityEnd;
        }

        /** Check if this entry applies to a file name.
         * @param name file name to check
         * @return true if the configuration applies to the specified file
         */
        public boolean appliesTo(final String name) {
            final int i = FastMath.max(name.lastIndexOf("/"), name.lastIndexOf("\\"));
            return name.startsWith(prefix, i + 1);
        }

        /** Get ITRF version.
         * @return ITRF version
         */
        public ITRFVersion getVersion() {
            return version;
        }

        /** Check if configuration entry is valid for a date.
         * @param mjd date to check in modified Julian day
         * @return true if entry is valid for the specified date
         */
        public boolean isValid(final int mjd) {
            return validityStart <= mjd && mjd < validityEnd;
        }

    }

}

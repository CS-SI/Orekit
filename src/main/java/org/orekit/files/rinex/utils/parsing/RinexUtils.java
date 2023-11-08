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
package org.orekit.files.rinex.utils.parsing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.RinexFile;
import org.orekit.files.rinex.section.RinexBaseHeader;
import org.orekit.files.rinex.section.RinexComment;
import org.orekit.files.rinex.utils.RinexFileType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.TimeSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.Month;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;

/** Utilities for RINEX various messages files.
 * @author Luc Maisonobe
 * @since 12.0
 *
 */
public class RinexUtils {

    /** Index of label in header lines. */
    public static final int LABEL_INDEX = 60;

    /** Pattern for splitting date, time and time zone. */
    private static final Pattern SPLITTING_PATTERN = Pattern.compile("([0-9A-Za-z/-]+) *([0-9:]+) *([A-Z][A-Z0-9_-]*)?");

    /** Pattern for dates with month abbrevation. */
    private static final Pattern DATE_DD_MMM_YY_PATTERN = Pattern.compile("([0-9]{2})-([A-Za-z]{3})-([0-9]{2})");

    /** Pattern for dates in ISO-8601 complete representation (basic or extended). */
    private static final Pattern DATE_ISO_8601_PATTERN = Pattern.compile("([0-9]{4})-?([0-9]{2})-?([0-9]{2})");

    /** Pattern for dates in european format. */
    private static final Pattern DATE_EUROPEAN_PATTERN = Pattern.compile("([0-9]{2})/([0-9]{2})/([0-9]{2})");

    /** Pattern for time. */
    private static final Pattern TIME_PATTERN = Pattern.compile("([0-9]{2}):?([0-9]{2})(?::?([0-9]{2}))?");

    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private RinexUtils() {
    }

    /** Get the trimmed label from a header line.
     * @param line header line to parse
     * @return trimmed label
     */
    public static String getLabel(final String line) {
        return line.length() < LABEL_INDEX ? "" : line.substring(LABEL_INDEX).trim();
    }

    /** Check if a header line matches an expected label.
     * @param line header line to check
     * @param label expected label
     * @return true if line matches expected label
     */
    public static boolean matchesLabel(final String line, final String label) {
        return getLabel(line).equals(label);
    }

    /** Parse version, file type and satellite system.
     * @param line line to parse
     * @param name file name (for error message generation)
     * @param header header to fill with parsed data
     * @param supportedVersions supported versions
     */
    public static void parseVersionFileTypeSatelliteSystem(final String line, final String name,
                                                           final RinexBaseHeader header,
                                                           final double... supportedVersions) {

        // Rinex version
        final double parsedVersion = parseDouble(line, 0, 9);

        boolean found = false;
        for (final double supported : supportedVersions) {
            if (FastMath.abs(parsedVersion - supported) < 1.0e-4) {
                found = true;
                break;
            }
        }
        if (!found) {
            final StringBuilder builder = new StringBuilder();
            for (final double supported : supportedVersions) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(supported);
            }
            throw new OrekitException(OrekitMessages.UNSUPPORTED_FILE_FORMAT_VERSION,
                                      parsedVersion, name, builder.toString());
        }
        header.setFormatVersion(parsedVersion);

        // File type
        if (header.getFileType() != RinexFileType.parseRinexFileType(parseString(line, 20, 1))) {
            throw new OrekitException(OrekitMessages.WRONG_PARSING_TYPE, name);
        }

        // Satellite system
        switch (header.getFileType()) {
            case OBSERVATION:
                // for observation files, the satellite system is in column 40, and empty defaults to GPS
                header.setSatelliteSystem(SatelliteSystem.parseSatelliteSystemWithGPSDefault(parseString(line, 40, 1)));
                break;
            case NAVIGATION: {
                if (header.getFormatVersion() < 3.0) {
                    // the satellite system is hidden within the entry, with GPS as default

                    // set up default
                    header.setSatelliteSystem(SatelliteSystem.GPS);

                    // look if default is overridden somewhere in the entry
                    final String entry = parseString(line, 0, LABEL_INDEX).toUpperCase();
                    for (final SatelliteSystem satelliteSystem : SatelliteSystem.values()) {
                        if (entry.contains(satelliteSystem.name())) {
                            // we found a satellite system hidden in the middle of the line
                            header.setSatelliteSystem(satelliteSystem);
                            break;
                        }
                    }

                } else {
                    // the satellite system is in column 40 for 3.X and later
                    header.setSatelliteSystem(SatelliteSystem.parseSatelliteSystemWithGPSDefault(parseString(line, 40, 1)));
                }
                break;
            }
            default:
                //  this should never happen
                throw new OrekitInternalError(null);
        }

    }

    /** Parse program, run/by and date.
     * @param line line to parse
     * @param lineNumber line number
     * @param name file name (for error message generation)
     * @param timeScales the set of time scales used for parsing dates.
     * @param header header to fill with parsed data
     */
    public static void parseProgramRunByDate(final String line, final int lineNumber,
                                             final String name, final TimeScales timeScales,
                                             final RinexBaseHeader header) {

        // Name of the generating program
        header.setProgramName(parseString(line, 0, 20));

        // Name of the run/by name
        header.setRunByName(parseString(line, 20, 20));

        // there are several variations for date formatting in the PGM / RUN BY / DATE line

        // in versions 2.x, the pattern is expected to be:
        // XXRINEXO V9.9       AIUB                24-MAR-01 14:43     PGM / RUN BY / DATE
        // however, we have also found this:
        // teqc  2016Nov7      root                20180130 10:38:06UTCPGM / RUN BY / DATE
        // BJFMTLcsr           UTCSR               2007-09-30 05:30:06 PGM / RUN BY / DATE
        // NEODIS              TAS                 27/05/22 10:28      PGM / RUN BY / DATE

        // in versions 3.x, the pattern is expected to be:
        // sbf2rin-11.3.3                          20180130 002558 LCL PGM / RUN BY / DATE
        // however, we have also found:
        // NetR9 5.03          Receiver Operator   11-JAN-16 00:00:00  PGM / RUN BY / DATE

        // so we cannot rely on the format version, we have to check several variations
        final Matcher splittingMatcher = SPLITTING_PATTERN.matcher(parseString(line, 40, 20));
        if (splittingMatcher.matches()) {

            // date part
            final DateComponents dc;
            final Matcher abbrevMatcher = DATE_DD_MMM_YY_PATTERN.matcher(splittingMatcher.group(1));
            if (abbrevMatcher.matches()) {
                // hoping this obsolete format will not be used past year 2079…
                dc = new DateComponents(convert2DigitsYear(Integer.parseInt(abbrevMatcher.group(3))),
                                        Month.parseMonth(abbrevMatcher.group(2)).getNumber(),
                                        Integer.parseInt(abbrevMatcher.group(1)));
            } else {
                final Matcher isoMatcher = DATE_ISO_8601_PATTERN.matcher(splittingMatcher.group(1));
                if (isoMatcher.matches()) {
                    dc = new DateComponents(Integer.parseInt(isoMatcher.group(1)),
                                            Integer.parseInt(isoMatcher.group(2)),
                                            Integer.parseInt(isoMatcher.group(3)));
                } else {
                    final Matcher europeanMatcher = DATE_EUROPEAN_PATTERN.matcher(splittingMatcher.group(1));
                    if (europeanMatcher.matches()) {
                        dc = new DateComponents(convert2DigitsYear(Integer.parseInt(europeanMatcher.group(3))),
                                                Integer.parseInt(europeanMatcher.group(2)),
                                                Integer.parseInt(europeanMatcher.group(1)));
                    } else {
                        dc = null;
                    }
                }
            }

            // time part
            final TimeComponents tc;
            final Matcher timeMatcher = TIME_PATTERN.matcher(splittingMatcher.group(2));
            if (timeMatcher.matches()) {
                tc = new TimeComponents(Integer.parseInt(timeMatcher.group(1)),
                                        Integer.parseInt(timeMatcher.group(2)),
                                        timeMatcher.group(3) != null ? Integer.parseInt(timeMatcher.group(3)) : 0);
            } else {
                tc = null;
            }

            // zone part
            final String zone = splittingMatcher.groupCount() > 2 ? splittingMatcher.group(3) : "";

            if (dc != null && tc != null) {
                // we successfully parsed everything
                final DateTimeComponents dtc = new DateTimeComponents(dc, tc);
                header.setCreationDateComponents(dtc);
                final TimeScale timeScale = zone == null ?
                                            timeScales.getUTC() :
                                            TimeSystem.parseTimeSystem(zone).getTimeScale(timeScales);
                header.setCreationDate(new AbsoluteDate(dtc, timeScale));
                header.setCreationTimeZone(zone);
                return;
            }

        }

        // we were not able to extract date
        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                      lineNumber, name, line);

    }

    /** Parse a comment.
     * @param lineNumber line number
     * @param line line to parse
     * @param rinexFile rinex file
     */
    public static void parseComment(final int lineNumber, final String line, final RinexFile<?> rinexFile) {
        rinexFile.addComment(new RinexComment(lineNumber, parseString(line, 0, 60)));
    }

    /**
     * Parse a double value.
     * @param line line to parse
     * @param startIndex start index
     * @param size size of the value
     * @return the parsed value
     */
    public static double parseDouble(final String line, final int startIndex, final int size) {
        final String subString = parseString(line, startIndex, size);
        if (subString == null || subString.isEmpty()) {
            return Double.NaN;
        } else {
            return Double.parseDouble(subString.replace('D', 'E').trim());
        }
    }

    /**
     * Parse an integer value.
     * @param line line to parse
     * @param startIndex start index
     * @param size size of the value
     * @return the parsed value
     */
    public static int parseInt(final String line, final int startIndex, final int size) {
        final String subString = parseString(line, startIndex, size);
        if (subString == null || subString.isEmpty()) {
            return 0;
        } else {
            return Integer.parseInt(subString.trim());
        }
    }

    /**
     * Parse a string value.
     * @param line line to parse
     * @param startIndex start index
     * @param size size of the value
     * @return the parsed value
     */
    public static String parseString(final String line, final int startIndex, final int size) {
        if (line.length() > startIndex) {
            return line.substring(startIndex, FastMath.min(line.length(), startIndex + size)).trim();
        } else {
            return null;
        }
    }

    /** Convert a 2 digits year to a complete year.
     * @param yy year between 0 and 99
     * @return complete year
     * @since 12.0
     */
    public static int convert2DigitsYear(final int yy) {
        return yy >= 80 ? (yy + 1900) : (yy + 2000);
    }

}

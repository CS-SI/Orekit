/* Copyright 2002-2025 CS GROUP
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
package org.orekit.files.rinex.section;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.utils.ParsingUtils;
import org.orekit.files.rinex.utils.RinexFileType;
import org.orekit.gnss.PredefinedTimeSystem;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.Month;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Base container for Rinex headers.
 * @since 12.0
 */
public abstract class RinexBaseHeader {

    /** Pattern for splitting date, time and time zone. */
    private static final Pattern SPLITTING_PATTERN = Pattern.compile("([0-9]+[/ -]?[0-9A-Za-z]+[/ -]?[0-9]+) +([0-9:]+) *([A-Z]+)?");

    /** Pattern for dates with month abbrevation. */
    private static final Pattern DATE_DD_MMM_YY_PATTERN = Pattern.compile("([0-9]{1,2})-([A-Za-z]{3})-([0-9]{2,4})");

    /** Pattern for dates with month abbrevation.
     * @since 14.0
     */
    private static final Pattern DATE_YYYY_MMM_DD_PATTERN = Pattern.compile("([0-9]{4})[- ]([A-Za-z]{3})[- ]([0-9]{1,2})");

    /** Pattern for dates in ISO-8601 complete representation (basic or extended). */
    private static final Pattern DATE_ISO_8601_PATTERN = Pattern.compile("([0-9]{4})-?([0-9]{2})-?([0-9]{2})");

    /** Pattern for dates in european format. */
    private static final Pattern DATE_EUROPEAN_PATTERN = Pattern.compile("([0-9]{2})/([0-9]{2})/([0-9]{2})");

    /** Pattern for time. */
    private static final Pattern TIME_PATTERN = Pattern.compile("([0-9]{2}):?([0-9]{2})(?::?([0-9]{2}))?");

    /** Orekit program name.
     * @since 14.0
     */
    private static final String OREKIT = "Orekit";

    /** User name property.
     * @since 14.0
     */
    private static final String USER_NAME = "user.name";

    /** File type . */
    private final RinexFileType fileType;

    /** Rinex format Version. */
    private double formatVersion;

    /** Satellite System of the Rinex file (G/R/S/E/M). */
    private SatelliteSystem satelliteSystem;

    /** Name of the program creating current file. */
    private String programName;

    /** Name of the creator of the current file. */
    private String runByName;

    /** Date of the file creation. */
    private DateTimeComponents creationDateComponents;

    /** Time zone of the file creation. */
    private String creationTimeZone;

    /** Creation date as absolute date. */
    private AbsoluteDate creationDate;

    /** Number of leap seconds separating UTC and GNSS time systems.
     * <p>
     * This is really the number of leap seconds since GPS epoch
     * on 1980-01-06.
     * </p>
     * @since 14.0
     */
    private int leapSecondsGNSS;

    /** Digital Object Identifier.
     * @since 12.0
     */
    private String doi;

    /** License of use.
     * @since 12.0
     */
    private String license;

    /** Station information.
     * @since 12.0
     */
    private String stationInformation;

    /** Simple constructor.
     * @param fileType file type
     */
    protected RinexBaseHeader(final RinexFileType fileType) {

        this.fileType      = fileType;
        this.formatVersion = Double.NaN;

        // set default creation date to now
        final ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        setCreationDateComponents(new DateTimeComponents(new DateComponents(now.getYear(),
                                                                            now.getMonthValue(),
                                                                            now.getDayOfMonth()),
                                                         new TimeComponents(now.getHour(),
                                                                            now.getMinute(),
                                                                            now.getSecond())));

        // set default program name to Orekit
        setProgramName(OREKIT);

        // set default run-by name to user
        setRunByName(System.getProperty(USER_NAME));

    }

    /**
     * Get the file type.
     * @return file type
     */
    public RinexFileType getFileType() {
        return fileType;
    }

    /**
     * Getter for the format version.
     * @return the format version
     */
    public double getFormatVersion() {
        return formatVersion;
    }

    /**
     * Setter for the format version.
     * @param formatVersion the format version to set
     */
    public void setFormatVersion(final double formatVersion) {
        this.formatVersion = formatVersion;
    }

    /**
     * Getter for the satellite system.
     * <p>
     * Not specified for RINEX 2.X versions (value is null).
     * </p>
     * @return the satellite system
     */
    public SatelliteSystem getSatelliteSystem() {
        return satelliteSystem;
    }

    /**
     * Setter for the satellite system.
     * @param satelliteSystem the satellite system to set
     */
    public void setSatelliteSystem(final SatelliteSystem satelliteSystem) {
        this.satelliteSystem = satelliteSystem;
    }

    /**
     * Parse satellite system.
     * @param line header line
     * @param defaultSatelliteSystem satellite system to use if string is null or empty
     * @return parsed satellite system
     * @since 14.0
     */
    public abstract SatelliteSystem parseSatelliteSystem(String line, SatelliteSystem defaultSatelliteSystem);

    /**
     * Getter for the program name.
     * @return the program name
     */
    public String getProgramName() {
        return programName;
    }

    /**
     * Setter for the program name.
     * @param programName the program name to set
     */
    public void setProgramName(final String programName) {
        this.programName = programName;
    }

    /**
     * Getter for the run/by name.
     * @return the run/by name
     */
    public String getRunByName() {
        return runByName;
    }

    /**
     * Setter for the run/by name.
     * @param runByName the run/by name to set
     */
    public void setRunByName(final String runByName) {
        this.runByName = runByName;
    }

    /**
     * Getter for the creation date of the file as a string.
     * @return the creation date
     */
    public DateTimeComponents getCreationDateComponents() {
        return creationDateComponents;
    }

    /**
     * Setter for the creation date as a string.
     * @param creationDateComponents the creation date to set
     */
    public void setCreationDateComponents(final DateTimeComponents creationDateComponents) {
        this.creationDateComponents = creationDateComponents;
    }

    /**
     * Getter for the creation time zone of the file as a string.
     * @return the creation time zone as a string
     */
    public String getCreationTimeZone() {
        return creationTimeZone;
    }

    /**
     * Setter for the creation time zone.
     * @param creationTimeZone the creation time zone to set
     */
    public void setCreationTimeZone(final String creationTimeZone) {
        this.creationTimeZone = creationTimeZone;
    }

    /**
     * Getter for the creation date.
     * <p>
     * The creation date seems to be mandatory, but we have seen several files
     * missing it, even files created by IGS itself (in clock files, essentially).
     * We accept these null dates to at least allow parsing the files
     * as this header information does not really seem essential
     * </p>
     * @return the creation date
     */
    public AbsoluteDate getCreationDate() {
        return creationDate;
    }

    /**
     * Setter for the creation date.
     * @param creationDate the creation date to set
     */
    public void setCreationDate(final AbsoluteDate creationDate) {
        this.creationDate = creationDate;
    }

    /** Getter for the number of leap second for GNSS time scales.
     * @return the number of leap seconds for GNSS time scales
     * @since 14.0
     */
    public int getLeapSecondsGNSS() {
        return leapSecondsGNSS;
    }

    /** Setter for the number of leap seconds for GNSS time scales.
     * @param leapSecondsGNSS the number of leap seconds for GNSS time scales to set
     * @since 14.0
     */
    public void setLeapSecondsGNSS(final int leapSecondsGNSS) {
        this.leapSecondsGNSS = leapSecondsGNSS;
    }

    /**
     *  Getter for the Digital Object Information.
     * @return the Digital Object Information
     * @since 12.0
     */
    public String getDoi() {
        return doi;
    }

    /**
     * Setter for the Digital Object Information.
     * @param doi the Digital Object Information to set
     * @since 12.0
     */
    public void setDoi(final String doi) {
        this.doi = doi;
    }

    /**
     *  Getter for the license of use.
     * @return the license of use
     * @since 12.0
     */
    public String getLicense() {
        return license;
    }

    /**
     * Setter for the license of use.
     * @param license the license of use
     * @since 12.0
     */
    public void setLicense(final String license) {
        this.license = license;
    }

    /**
     *  Getter for the station information.
     * @return the station information
     * @since 12.0
     */
    public String getStationInformation() {
        return stationInformation;
    }

    /**
     * Setter for the station information.
     * @param stationInformation the station information to set
     * @since 12.0
     */
    public void setStationInformation(final String stationInformation) {
        this.stationInformation = stationInformation;
    }

    /** Parse version, file type and satellite system.
     * @param line line to parse
     * @param defaultSatelliteSystem satellite system to use if string is null or empty
     * @param name file name (for error message generation)
     * @param supportedVersions supported versions
     * @since 14.0
     */
    public void parseVersionFileTypeSatelliteSystem(final String line, final SatelliteSystem defaultSatelliteSystem,
                                                    final String name, final double... supportedVersions) {

        // Rinex version
        final double parsedVersion = ParsingUtils.parseDouble(line, 0, 9);

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
        setFormatVersion(parsedVersion);

        // file type
        checkType(line, name);

        // Satellite system
        setSatelliteSystem(parseSatelliteSystem(line, defaultSatelliteSystem));

    }

    /** Parse program, run/by and date.
     * @param line line to parse
     * @param timeScales the set of time scales used for parsing dates
     * @since 14.0
     */
    public abstract void parseProgramRunByDate(String line, TimeScales timeScales);

    /** Parse program, run/by and date.
     * @param prgm  PGM field
     * @param run  RUN BY field
     * @param date  date field
     * @param timeScales the set of time scales used for parsing dates
     * @since 14.0
     */
    protected void parseProgramRunByDate(final String prgm, final String run, final String date,
                                         final TimeScales timeScales) {

        // Name of the generating program
        setProgramName(prgm);

        // Name of the run/by name
        setRunByName(run);

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

        // in clock files, we have found patterns like:
        // CLKRINEX V1.0       NRCan               1-Mar-2000 20:36    PGM / RUN BY / DATE
        // tdp2clk v1.13       JPL                 2002 Jan 3 13:36:17 PGM / RUN BY / DATE

        // so we cannot rely on the format version, we have to check several variations
        final Matcher splittingMatcher = SPLITTING_PATTERN.matcher(date);
        if (splittingMatcher.matches()) {

            // date part
            final DateComponents dc;
            final Matcher abbrev1Matcher = DATE_DD_MMM_YY_PATTERN.matcher(splittingMatcher.group(1));
            if (abbrev1Matcher.matches()) {
                final int rawYear = Integer.parseInt(abbrev1Matcher.group(3));
                // hoping this obsolete format will not be used past year 2079…
                dc = new DateComponents(rawYear < 100 ? ParsingUtils.convert2DigitsYear(rawYear) : rawYear,
                                        Month.parseMonth(abbrev1Matcher.group(2)).getNumber(),
                                        Integer.parseInt(abbrev1Matcher.group(1)));
            } else {
                final Matcher abbrev2Matcher = DATE_YYYY_MMM_DD_PATTERN.matcher(splittingMatcher.group(1));
                if (abbrev2Matcher.matches()) {
                    dc = new DateComponents(Integer.parseInt(abbrev2Matcher.group(1)),
                                            Month.parseMonth(abbrev2Matcher.group(2)).getNumber(),
                                            Integer.parseInt(abbrev2Matcher.group(3)));
                } else {
                    final Matcher isoMatcher = DATE_ISO_8601_PATTERN.matcher(splittingMatcher.group(1));
                    if (isoMatcher.matches()) {
                        dc = new DateComponents(Integer.parseInt(isoMatcher.group(1)),
                                               Integer.parseInt(isoMatcher.group(2)),
                                               Integer.parseInt(isoMatcher.group(3)));
                    } else {
                        final Matcher europeanMatcher = DATE_EUROPEAN_PATTERN.matcher(splittingMatcher.group(1));
                        if (europeanMatcher.matches()) {
                            dc = new DateComponents(
                                ParsingUtils.convert2DigitsYear(Integer.parseInt(europeanMatcher.group(3))),
                                Integer.parseInt(europeanMatcher.group(2)),
                                Integer.parseInt(europeanMatcher.group(1)));
                        } else {
                            dc = null;
                        }
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
                tc = TimeComponents.H00;
            }

            // zone part
            final String zone = splittingMatcher.group(3);
            setCreationTimeZone(zone == null ? "" : zone);

            if (dc == null) {
                // despite the creation date seems to be mandatory, we have seen several files
                // missing it, even files created by IGS itself (in clock files, essentially).
                // We accept these null dates to at least allow parsing the files
                // as this header information does not really seem essential
                setCreationDate(null);
            } else {
                // we successfully parsed everything
                final DateTimeComponents dtc = new DateTimeComponents(dc, tc);
                setCreationDateComponents(dtc);
                final TimeScale timeScale = zone == null ?
                                            timeScales.getUTC() :
                                            PredefinedTimeSystem.parseTimeSystem(zone).getTimeScale(timeScales);
                setCreationDate(new AbsoluteDate(dtc, timeScale));
            }

        } else {
            setCreationDate(null);
            setCreationTimeZone("");
        }

    }

    /** Check file type.
     * @param line header line
     * @param name file name (for error message)
     * @since 14.0
     */
    public abstract void checkType(String line, String name);

    /** Check file type.
     * @param line header line
     * @param typeIndex index of the file type in the line
     * @param name file name (for error message)
     * @since 14.0
     */
    protected void checkType(final String line, final int typeIndex, final String name) {
        if (fileType != RinexFileType.parseRinexFileType(line.substring(typeIndex, typeIndex + 1))) {
            throw new OrekitException(OrekitMessages.WRONG_PARSING_TYPE, name);
        }
    }

    /** Get the index of the header label.
     * @return index of the header label
     * @since 14.0
     */
    public abstract int getLabelIndex();

    /** Check if a label is found in a line.
     * @param label label to check
     * @param line header line
     * @return true if label is found in the header line
     * @since 14.0
     */
    public abstract boolean matchFound(Label label, String line);

}

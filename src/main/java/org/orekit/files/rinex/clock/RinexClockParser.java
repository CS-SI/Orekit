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
package org.orekit.files.rinex.clock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.AppliedDCBS;
import org.orekit.files.rinex.AppliedPCVS;
import org.orekit.files.rinex.clock.RinexClock.ClockDataType;
import org.orekit.files.rinex.clock.RinexClock.Receiver;
import org.orekit.files.rinex.clock.RinexClock.ReferenceClock;
import org.orekit.frames.Frame;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.TimeSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.utils.IERSConventions;

/** A parser for the clock file from the IGS.
 * This parser handles versions 2.0 to 3.04 of the RINEX clock files.
 * <p> It is able to manage some mistakes in file writing and format compliance such as wrong date format,
 * misplaced header blocks or missing information. </p>
 * <p> A time system should be specified in the file. However, if it is not, default time system will be chosen
 * regarding the satellite system. If it is mixed or not specified, default time system will be UTC. </p>
 * <p> Caution, files with missing information in header can lead to wrong data dates and station positions.
 * It is advised to check the correctness and format compliance of the clock file to be parsed. </p>
 * @see <a href="https://files.igs.org/pub/data/format/rinex_clock300.txt"> 3.00 clock file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex_clock302.txt"> 3.02 clock file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex_clock304.txt"> 3.04 clock file format</a>
 *
 * @author Thomas Paulet
 * @since 11.0
 */
public class RinexClockParser {

    /** Handled clock file format versions. */
    private static final List<Double> HANDLED_VERSIONS = Arrays.asList(2.00, 3.00, 3.01, 3.02, 3.04);

    /** Pattern for date format yyyy-mm-dd hh:mm. */
    private static final Pattern DATE_PATTERN_1 = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}.*$");;

    /** Pattern for date format yyyymmdd hhmmss zone or YYYYMMDD  HHMMSS zone. */
    private static final Pattern DATE_PATTERN_2 = Pattern.compile("^[0-9]{8}\\s{1,2}[0-9]{6}.*$");

    /** Pattern for date format dd-MONTH-yyyy hh:mm zone or d-MONTH-yyyy hh:mm zone. */
    private static final Pattern DATE_PATTERN_3 = Pattern.compile("^[0-9]{1,2}-[a-z,A-Z]{3}-[0-9]{4} [0-9]{2}:[0-9]{2}.*$");

    /** Pattern for date format dd-MONTH-yy hh:mm zone or d-MONTH-yy hh:mm zone. */
    private static final Pattern DATE_PATTERN_4 = Pattern.compile("^[0-9]{1,2}-[a-z,A-Z]{3}-[0-9]{2} [0-9]{2}:[0-9]{2}.*$");

    /** Pattern for date format yyyy MONTH dd hh:mm:ss or yyyy MONTH d hh:mm:ss. */
    private static final Pattern DATE_PATTERN_5 = Pattern.compile("^[0-9]{4} [a-z,A-Z]{3} [0-9]{1,2} [0-9]{2}:[0-9]{2}:[0-9]{2}.*$");

    /** Spaces delimiters. */
    private static final String SPACES = "\\s+";

    /** SYS string for line browsing stop. */
    private static final String SYS = "SYS";

    /** One millimeter, in meters. */
    private static final double MILLIMETER = 1.0e-3;

    /** Mapping from frame identifier in the file to a {@link Frame}. */
    private final Function<? super String, ? extends Frame> frameBuilder;

    /** Set of time scales. */
    private final TimeScales timeScales;

    /**
     * Create an clock file parser using default values.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @see #RinexClockParser(Function)
     */
    @DefaultDataContext
    public RinexClockParser() {
        this(RinexClockParser::guessFrame);
    }

    /**
     * Create a clock file parser and specify the frame builder.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param frameBuilder is a function that can construct a frame from a clock file
     *                     coordinate system string. The coordinate system can be
     *                     any 5 character string e.g. ITR92, IGb08.
     * @see #RinexClockParser(Function, TimeScales)
     */
    @DefaultDataContext
    public RinexClockParser(final Function<? super String, ? extends Frame> frameBuilder) {
        this(frameBuilder, DataContext.getDefault().getTimeScales());
    }

    /** Constructor, build the IGS clock file parser.
     * @param frameBuilder is a function that can construct a frame from a clock file
     *                     coordinate system string. The coordinate system can be
     *                     any 5 character string e.g. ITR92, IGb08.
     * @param timeScales   the set of time scales used for parsing dates.
     */
    public RinexClockParser(final Function<? super String, ? extends Frame> frameBuilder,
                           final TimeScales timeScales) {

        this.frameBuilder = frameBuilder;
        this.timeScales   = timeScales;
    }

    /**
     * Default string to {@link Frame} conversion for {@link #CLockFileParser()}.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param name of the frame.
     * @return by default, return ITRF based on 2010 conventions,
     *         with tidal effects considered during EOP interpolation.
     * <p>If String matches to other already recorded frames, it will return the corresponding frame.</p>
     * Already embedded frames are:
     * <p> - ITRF96
     */
    @DefaultDataContext
    private static Frame guessFrame(final String name) {
        if (name.equals("ITRF96")) {
            return DataContext.getDefault().getFrames()
                              .getITRF(IERSConventions.IERS_1996, false);
        } else {
            return DataContext.getDefault().getFrames()
                              .getITRF(IERSConventions.IERS_2010, false);
        }
    }

    /**
     * Parse an IGS clock file from an input stream using the UTF-8 charset.
     *
     * <p> This method creates a {@link BufferedReader} from the stream and as such this
     * method may read more data than necessary from {@code stream} and the additional
     * data will be lost. The other parse methods do not have this issue.
     *
     * @param stream to read the IGS clock file from
     * @return a parsed IGS clock file
     * @throws IOException if {@code stream} throws one
     * @see #parse(String)
     * @see #parse(BufferedReader, String)
     */
    public RinexClock parse(final InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return parse(reader, stream.toString());
        }
    }

    /**
     * Parse an IGS clock file from a file on the local file system.
     * @param fileName file name
     * @return a parsed IGS clock file
     * @throws IOException if one is thrown while opening or reading from {@code fileName}
     * @see #parse(InputStream)
     * @see #parse(BufferedReader, String)
     */
    public RinexClock parse(final String fileName) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(fileName),
                                                             StandardCharsets.UTF_8)) {
            return parse(reader, fileName);
        }
    }

    /**
     * Parse an IGS clock file from a stream.
     * @param reader containing the clock file
     * @param fileName file name
     * @return a parsed IGS clock file
     * @throws IOException if {@code reader} throws one
     * @see #parse(InputStream)
     * @see #parse(String)
     */
    public RinexClock parse(final BufferedReader reader,
                           final String fileName) throws IOException {

        // initialize internal data structures
        final ParseInfo pi = new ParseInfo();

        int lineNumber = 0;
        Iterable<LineParser> candidateParsers = Collections.singleton(LineParser.HEADER_VERSION);
        nextLine:
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            ++lineNumber;
            for (final LineParser candidate : candidateParsers) {
                if (candidate.canHandle(line)) {
                    try {
                        candidate.parse(line, pi);
                        candidateParsers = candidate.allowedNext();
                        continue nextLine;
                    } catch (StringIndexOutOfBoundsException | NumberFormatException | InputMismatchException e) {
                        throw new OrekitException(e,
                                                  OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  lineNumber, fileName, line);
                    }
                }
            }

            // no parsers found for this line
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                      lineNumber, fileName, line);

        }

        return pi.file;

    }

    /** Transient data used for parsing a clock file. */
    private class ParseInfo {

        /** Set of time scales for parsing dates. */
        private final TimeScales timeScales;

        /** The corresponding clock file object. */
        private RinexClock file;

        /** Current satellite system for observation type parsing. */
        private SatelliteSystem currentSatelliteSystem;

        /** Current start date for reference clocks. */
        private AbsoluteDate referenceClockStartDate;

        /** Current end date for reference clocks. */
        private AbsoluteDate referenceClockEndDate;

        /** Current reference clock list. */
        private List<ReferenceClock> currentReferenceClocks;

        /** Current clock data type. */
        private ClockDataType currentDataType;

        /** Current receiver/satellite name. */
        private String currentName;

        /** Current data date components. */
        private DateComponents currentDateComponents;

        /** Current data time components. */
        private TimeComponents currentTimeComponents;

        /** Current data number of data values to follow. */
        private int currentNumberOfValues;

        /** Current data values. */
        private double[] currentDataValues;

        /** Constructor, build the ParseInfo object. */
        protected ParseInfo () {
            this.timeScales = RinexClockParser.this.timeScales;
            this.file = new RinexClock(frameBuilder);
        }
    }


    /** Parsers for specific lines. */
    private enum LineParser {

        /** Parser for version, file type and satellite system. */
        HEADER_VERSION("^.+RINEX VERSION / TYPE( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                try (Scanner s1      = new Scanner(line);
                     Scanner s2      = s1.useDelimiter(SPACES);
                     Scanner scanner = s2.useLocale(Locale.US)) {

                    // First element of the line is format version
                    final double version = scanner.nextDouble();

                    // Throw exception if format version is not handled
                    if (!HANDLED_VERSIONS.contains(version)) {
                        throw new OrekitException(OrekitMessages.CLOCK_FILE_UNSUPPORTED_VERSION, version);
                    }

                    pi.file.setFormatVersion(version);

                    // Second element is clock file indicator, not used here

                    // Last element is the satellite system, might be missing
                    final String satelliteSystemString = line.substring(40, 45).trim();

                    // Check satellite if system is recorded
                    if (!satelliteSystemString.equals("")) {
                        // Record satellite system and default time system in clock file object
                        final SatelliteSystem satelliteSystem = SatelliteSystem.parseSatelliteSystem(satelliteSystemString);
                        pi.file.setSatelliteSystem(satelliteSystem);
                        pi.file.setTimeScale(satelliteSystem.getObservationTimeScale().getTimeScale(pi.timeScales));
                    }
                    // Set time scale to UTC by default
                    if (pi.file.getTimeScale() == null) {
                        pi.file.setTimeScale(pi.timeScales.getUTC());
                    }
                }
            }

        },

        /** Parser for generating program and emiting agency. */
        HEADER_PROGRAM("^.+PGM / RUN BY / DATE( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // First element of the name of the generating program
                final String programName = line.substring(0, 20).trim();
                pi.file.setProgramName(programName);

                // Second element is the name of the emiting agency
                final String agencyName = line.substring(20, 40).trim();
                pi.file.setAgencyName(agencyName);

                // Third element is date
                String dateString = "";

                if (pi.file.getFormatVersion() < 3.04) {

                    // Date string location before 3.04 format version
                    dateString = line.substring(40, 60);

                } else {

                    // Date string location after 3.04 format version
                    dateString = line.substring(42, 65);

                }

                parseDateTimeZone(dateString, pi);

            }

        },

        /** Parser for comments. */
        HEADER_COMMENT("^.+COMMENT( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                if (pi.file.getFormatVersion() < 3.04) {
                    pi.file.addComment(line.substring(0, 60).trim());
                } else {
                    pi.file.addComment(line.substring(0, 65).trim());
                }
            }

        },

        /** Parser for satellite system and related observation types. */
        HEADER_SYSTEM_OBS("^[A-Z] .*SYS / # / OBS TYPES( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                try (Scanner s1      = new Scanner(line);
                     Scanner s2      = s1.useDelimiter(SPACES);
                     Scanner scanner = s2.useLocale(Locale.US)) {

                    // First element of the line is satellite system code
                    final SatelliteSystem satelliteSystem = SatelliteSystem.parseSatelliteSystem(scanner.next());
                    pi.currentSatelliteSystem = satelliteSystem;

                    // Second element is the number of different observation types
                    scanner.nextInt();

                    // Parse all observation types
                    String currentObsType = scanner.next();
                    while (!currentObsType.equals(SYS)) {
                        final ObservationType obsType = ObservationType.valueOf(currentObsType);
                        pi.file.addSystemObservationType(satelliteSystem, obsType);
                        currentObsType = scanner.next();
                    }
                }
            }

        },

        /** Parser for continuation of satellite system and related observation types. */
        HEADER_SYSTEM_OBS_CONTINUATION("^ .*SYS / # / OBS TYPES( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                try (Scanner s1      = new Scanner(line);
                     Scanner s2      = s1.useDelimiter(SPACES);
                     Scanner scanner = s2.useLocale(Locale.US)) {

                    // This is a continuation line, there are only observation types
                    // Parse all observation types
                    String currentObsType = scanner.next();
                    while (!currentObsType.equals(SYS)) {
                        final ObservationType obsType = ObservationType.valueOf(currentObsType);
                        pi.file.addSystemObservationType(pi.currentSatelliteSystem, obsType);
                        currentObsType = scanner.next();
                    }
                }
            }

        },

        /** Parser for data time system. */
        HEADER_TIME_SYSTEM("^.+TIME SYSTEM ID( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                try (Scanner s1      = new Scanner(line);
                     Scanner s2      = s1.useDelimiter(SPACES);
                     Scanner scanner = s2.useLocale(Locale.US)) {

                    // Only element is the time system code
                    final TimeSystem timeSystem = TimeSystem.parseTimeSystem(scanner.next());
                    final TimeScale timeScale = timeSystem.getTimeScale(pi.timeScales);
                    pi.file.setTimeSystem(timeSystem);
                    pi.file.setTimeScale(timeScale);
                }
            }

        },

        /** Parser for leap seconds. */
        HEADER_LEAP_SECONDS("^.+LEAP SECONDS( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                try (Scanner s1      = new Scanner(line);
                     Scanner s2      = s1.useDelimiter(SPACES);
                     Scanner scanner = s2.useLocale(Locale.US)) {

                    // Only element is the number of leap seconds
                    final int numberOfLeapSeconds = scanner.nextInt();
                    pi.file.setNumberOfLeapSeconds(numberOfLeapSeconds);
                }
            }

        },

        /** Parser for leap seconds GNSS. */
        HEADER_LEAP_SECONDS_GNSS("^.+LEAP SECONDS GNSS( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                try (Scanner s1      = new Scanner(line);
                     Scanner s2      = s1.useDelimiter(SPACES);
                     Scanner scanner = s2.useLocale(Locale.US)) {

                    // Only element is the number of leap seconds GNSS
                    final int numberOfLeapSecondsGNSS = scanner.nextInt();
                    pi.file.setNumberOfLeapSecondsGNSS(numberOfLeapSecondsGNSS);
                }
            }

        },

        /** Parser for applied differencial code bias corrections. */
        HEADER_DCBS("^.+SYS / DCBS APPLIED( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // First element is the related satellite system
                final SatelliteSystem satelliteSystem = SatelliteSystem.parseSatelliteSystem(line.substring(0, 1));

                // Second element is the program name
                final String progDCBS = line.substring(2, 20).trim();

                // Third element is the source of the corrections
                String sourceDCBS = "";
                if (pi.file.getFormatVersion() < 3.04) {
                    sourceDCBS = line.substring(19, 60).trim();
                } else {
                    sourceDCBS = line.substring(22, 65).trim();
                }

                // Check if sought fields were not actually blanks
                if (!progDCBS.equals("")) {
                    pi.file.addAppliedDCBS(new AppliedDCBS(satelliteSystem, progDCBS, sourceDCBS));
                }
            }

        },

        /** Parser for applied phase center variation corrections. */
        HEADER_PCVS("^.+SYS / PCVS APPLIED( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // First element is the related satellite system
                final SatelliteSystem satelliteSystem = SatelliteSystem.parseSatelliteSystem(line.substring(0, 1));

                // Second element is the program name
                final String progPCVS = line.substring(2, 20).trim();

                // Third element is the source of the corrections
                String sourcePCVS = "";
                if (pi.file.getFormatVersion() < 3.04) {
                    sourcePCVS = line.substring(19, 60).trim();
                } else {
                    sourcePCVS = line.substring(22, 65).trim();
                }

                // Check if sought fields were not actually blanks
                if (!progPCVS.equals("") || !sourcePCVS.equals("")) {
                    pi.file.addAppliedPCVS(new AppliedPCVS(satelliteSystem, progPCVS, sourcePCVS));
                }
            }

        },

        /** Parser for the different clock data types that are stored in the file. */
        HEADER_TYPES_OF_DATA("^.+# / TYPES OF DATA( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                try (Scanner s1      = new Scanner(line);
                     Scanner s2      = s1.useDelimiter(SPACES);
                     Scanner scanner = s2.useLocale(Locale.US)) {

                    // First element is the number of different types of data
                    final int numberOfDifferentDataTypes = scanner.nextInt();

                    // Loop over data types
                    for (int i = 0; i < numberOfDifferentDataTypes; i++) {
                        final ClockDataType dataType = ClockDataType.parseClockDataType(scanner.next());
                        pi.file.addClockDataType(dataType);
                    }
                }
            }

        },

        /** Parser for the station with reference clock. */
        HEADER_STATIONS_NAME("^.+STATION NAME / NUM( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                try (Scanner s1      = new Scanner(line);
                     Scanner s2      = s1.useDelimiter(SPACES);
                     Scanner scanner = s2.useLocale(Locale.US)) {

                    // First element is the station clock reference ID
                    final String stationName = scanner.next();
                    pi.file.setStationName(stationName);

                    // Second element is the station clock reference identifier
                    final String stationIdentifier = scanner.next();
                    pi.file.setStationIdentifier(stationIdentifier);
                }
            }

        },

        /** Parser for the reference clock in case of calibration data. */
        HEADER_STATION_CLOCK_REF("^.+STATION CLK REF( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                if (pi.file.getFormatVersion() < 3.04) {
                    pi.file.setExternalClockReference(line.substring(0, 60).trim());
                } else {
                    pi.file.setExternalClockReference(line.substring(0, 65).trim());
                }
            }

        },

        /** Parser for the analysis center. */
        HEADER_ANALYSIS_CENTER("^.+ANALYSIS CENTER( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // First element is IGS AC designator
                final String analysisCenterID = line.substring(0, 3).trim();
                pi.file.setAnalysisCenterID(analysisCenterID);

                // Then, the full name of the analysis center
                String analysisCenterName = "";
                if (pi.file.getFormatVersion() < 3.04) {
                    analysisCenterName = line.substring(5, 60).trim();
                } else {
                    analysisCenterName = line.substring(5, 65).trim();
                }
                pi.file.setAnalysisCenterName(analysisCenterName);
            }

        },

        /** Parser for the number of reference clocks over a period. */
        HEADER_NUMBER_OF_CLOCK_REF("^.+# OF CLK REF( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                try (Scanner s1      = new Scanner(line);
                     Scanner s2      = s1.useDelimiter(SPACES);
                     Scanner scanner = s2.useLocale(Locale.US)) {

                    // Initialize current reference clock list corresponding to the period
                    pi.currentReferenceClocks = new ArrayList<ReferenceClock>();

                    // First element is the number of reference clocks corresponding to the period
                    scanner.nextInt();

                    if (scanner.hasNextInt()) {
                        // Second element is the start epoch of the period
                        final int startYear   = scanner.nextInt();
                        final int startMonth  = scanner.nextInt();
                        final int startDay    = scanner.nextInt();
                        final int startHour   = scanner.nextInt();
                        final int startMin    = scanner.nextInt();
                        final double startSec = scanner.nextDouble();
                        final AbsoluteDate startEpoch = new AbsoluteDate(startYear, startMonth, startDay,
                                                                         startHour, startMin, startSec,
                                                                         pi.file.getTimeScale());
                        pi.referenceClockStartDate = startEpoch;

                        // Third element is the end epoch of the period
                        final int endYear   = scanner.nextInt();
                        final int endMonth  = scanner.nextInt();
                        final int endDay    = scanner.nextInt();
                        final int endHour   = scanner.nextInt();
                        final int endMin    = scanner.nextInt();
                        double endSec       = 0.0;
                        if (pi.file.getFormatVersion() < 3.04) {
                            endSec = Double.parseDouble(line.substring(51, 60));
                        } else {
                            endSec = scanner.nextDouble();
                        }
                        final AbsoluteDate endEpoch = new AbsoluteDate(endYear, endMonth, endDay,
                                                                       endHour, endMin, endSec,
                                                                       pi.file.getTimeScale());
                        pi.referenceClockEndDate = endEpoch;
                    } else {
                        pi.referenceClockStartDate = AbsoluteDate.PAST_INFINITY;
                        pi.referenceClockEndDate = AbsoluteDate.FUTURE_INFINITY;
                    }
                }
            }

        },

        /** Parser for the reference clock over a period. */
        HEADER_ANALYSIS_CLOCK_REF("^.+ANALYSIS CLK REF( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                try (Scanner s1      = new Scanner(line);
                     Scanner s2      = s1.useDelimiter(SPACES);
                     Scanner scanner = s2.useLocale(Locale.US)) {

                    // First element is the name of the receiver/satellite embedding the reference clock
                    final String referenceName = scanner.next();

                    // Second element is the reference clock ID
                    final String clockID = scanner.next();

                    // Optionally, third element is an a priori clock constraint, by default equal to zero
                    double clockConstraint = 0.0;
                    if (scanner.hasNextDouble()) {
                        clockConstraint = scanner.nextDouble();
                    }

                    // Add reference clock to current reference clock list
                    final ReferenceClock referenceClock = new ReferenceClock(referenceName, clockID, clockConstraint,
                                                                             pi.referenceClockStartDate, pi.referenceClockEndDate);
                    pi.currentReferenceClocks.add(referenceClock);

                    // Modify time span map of the reference clocks to accept the new reference clock
                    pi.file.addReferenceClockList(pi.currentReferenceClocks, pi.referenceClockStartDate);
                }
            }

        },

        /** Parser for the number of stations embedded in the file and the related frame. */
        HEADER_NUMBER_OF_SOLN_STATIONS("^.+SOLN STA / TRF( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                try (Scanner s1      = new Scanner(line);
                     Scanner s2      = s1.useDelimiter(SPACES);
                     Scanner scanner = s2.useLocale(Locale.US)) {

                    // First element is the number of receivers embedded in the file
                    scanner.nextInt();

                    // Second element is the frame linked to given receiver positions
                    final String frameString = scanner.next();
                    pi.file.setFrameName(frameString);
                }
            }

        },

        /** Parser for the stations embedded in the file and the related positions. */
        HEADER_SOLN_STATIONS("^.+SOLN STA NAME / NUM( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // First element is the receiver designator
                String designator = line.substring(0, 10).trim();

                // Second element is the receiver identifier
                String receiverIdentifier = line.substring(10, 30).trim();

                // Third element if X coordinates, in millimeters in the file frame.
                String xString = "";

                // Fourth element if Y coordinates, in millimeters in the file frame.
                String yString = "";

                // Fifth element if Z coordinates, in millimeters in the file frame.
                String zString = "";

                if (pi.file.getFormatVersion() < 3.04) {
                    designator = line.substring(0, 4).trim();
                    receiverIdentifier = line.substring(5, 25).trim();
                    xString = line.substring(25, 36).trim();
                    yString = line.substring(37, 48).trim();
                    zString = line.substring(49, 60).trim();
                } else {
                    designator = line.substring(0, 10).trim();
                    receiverIdentifier = line.substring(10, 30).trim();
                    xString = line.substring(30, 41).trim();
                    yString = line.substring(42, 53).trim();
                    zString = line.substring(54, 65).trim();
                }

                final double x = MILLIMETER * Double.parseDouble(xString);
                final double y = MILLIMETER * Double.parseDouble(yString);
                final double z = MILLIMETER * Double.parseDouble(zString);

                final Receiver receiver = new Receiver(designator, receiverIdentifier, x, y, z);
                pi.file.addReceiver(receiver);

            }

        },

        /** Parser for the number of satellites embedded in the file. */
        HEADER_NUMBER_OF_SOLN_SATS("^.+# OF SOLN SATS( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                    // Only element in the line is number of satellites, not used here.
                    // Do nothing...
            }

        },

        /** Parser for the satellites embedded in the file. */
        HEADER_PRN_LIST("^.+PRN LIST( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                try (Scanner s1      = new Scanner(line);
                     Scanner s2      = s1.useDelimiter(SPACES);
                     Scanner scanner = s2.useLocale(Locale.US)) {

                    // Only PRN numbers are stored in these lines
                    // Initialize first PRN number
                    String prn = scanner.next();

                    // Browse the line until its end
                    while (!prn.equals("PRN")) {
                        pi.file.addSatellite(prn);
                        prn = scanner.next();
                    }
                }
            }

        },

        /** Parser for the end of header. */
        HEADER_END("^.+END OF HEADER( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // do nothing...
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Collections.singleton(CLOCK_DATA);
            }
        },

        /** Parser for a clock data line. */
        CLOCK_DATA("(^AR |^AS |^CR |^DR |^MS ).+$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                try (Scanner s1      = new Scanner(line);
                     Scanner s2      = s1.useDelimiter(SPACES);
                     Scanner scanner = s2.useLocale(Locale.US)) {

                    // Initialise current values
                    pi.currentDataValues = new double[6];

                    // First element is clock data type
                    pi.currentDataType = ClockDataType.parseClockDataType(scanner.next());

                    // Second element is receiver/satellite name
                    pi.currentName = scanner.next();

                    // Third element is data epoch
                    final int year   = scanner.nextInt();
                    final int month  = scanner.nextInt();
                    final int day    = scanner.nextInt();
                    final int hour   = scanner.nextInt();
                    final int min    = scanner.nextInt();
                    final double sec = scanner.nextDouble();
                    pi.currentDateComponents = new DateComponents(year, month, day);
                    pi.currentTimeComponents = new TimeComponents(hour, min, sec);

                    // Fourth element is number of data values
                    pi.currentNumberOfValues = scanner.nextInt();

                    // Get the values in this line, there are at most 2.
                    // Some entries claim less values than there actually are.
                    // All values are added to the set, regardless of their claimed number.
                    int i = 0;
                    while (scanner.hasNextDouble()) {
                        pi.currentDataValues[i++] = scanner.nextDouble();
                    }

                    // Check if continuation line is required
                    if (pi.currentNumberOfValues <= 2) {
                        // No continuation line is required
                        pi.file.addClockData(pi.currentName, pi.file.new ClockDataLine(pi.currentDataType,
                                                                                       pi.currentName,
                                                                                       pi.currentDateComponents,
                                                                                       pi.currentTimeComponents,
                                                                                       pi.currentNumberOfValues,
                                                                                       pi.currentDataValues[0],
                                                                                       pi.currentDataValues[1],
                                                                                       0.0, 0.0, 0.0, 0.0));
                    }
                }
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(CLOCK_DATA, CLOCK_DATA_CONTINUATION);
            }
        },

        /** Parser for a continuation clock data line. */
        CLOCK_DATA_CONTINUATION("^   .+") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                try (Scanner s1      = new Scanner(line);
                     Scanner s2      = s1.useDelimiter(SPACES);
                     Scanner scanner = s2.useLocale(Locale.US)) {

                    // Get the values in this continuation line.
                    // Some entries claim less values than there actually are.
                    // All values are added to the set, regardless of their claimed number.
                    int i = 2;
                    while (scanner.hasNextDouble()) {
                        pi.currentDataValues[i++] = scanner.nextDouble();
                    }

                    // Add clock data line
                    pi.file.addClockData(pi.currentName, pi.file.new ClockDataLine(pi.currentDataType,
                                                                                   pi.currentName,
                                                                                   pi.currentDateComponents,
                                                                                   pi.currentTimeComponents,
                                                                                   pi.currentNumberOfValues,
                                                                                   pi.currentDataValues[0],
                                                                                   pi.currentDataValues[1],
                                                                                   pi.currentDataValues[2],
                                                                                   pi.currentDataValues[3],
                                                                                   pi.currentDataValues[4],
                                                                                   pi.currentDataValues[5]));

                }
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Collections.singleton(CLOCK_DATA);
            }
        };

        /** Pattern for identifying line. */
        private final Pattern pattern;

        /** Simple constructor.
         * @param lineRegexp regular expression for identifying line
         */
        LineParser(final String lineRegexp) {
            pattern = Pattern.compile(lineRegexp);
        }

        /** Parse a line.
         * @param line line to parse
         * @param pi holder for transient data
         */
        public abstract void parse(String line, ParseInfo pi);

        /** Get the allowed parsers for next line.
         * <p>
         * Because the standard only recommends an order for header keys,
         * the default implementation of the method returns all the
         * header keys. Specific implementations must overrides the method.
         * </p>
         * @return allowed parsers for next line
         */
        public Iterable<LineParser> allowedNext() {
            return Arrays.asList(HEADER_PROGRAM, HEADER_COMMENT, HEADER_SYSTEM_OBS, HEADER_SYSTEM_OBS_CONTINUATION, HEADER_TIME_SYSTEM, HEADER_LEAP_SECONDS,
                                 HEADER_LEAP_SECONDS_GNSS, HEADER_DCBS, HEADER_PCVS, HEADER_TYPES_OF_DATA, HEADER_STATIONS_NAME, HEADER_STATION_CLOCK_REF,
                                 HEADER_ANALYSIS_CENTER, HEADER_NUMBER_OF_CLOCK_REF, HEADER_ANALYSIS_CLOCK_REF, HEADER_NUMBER_OF_SOLN_STATIONS,
                                 HEADER_SOLN_STATIONS, HEADER_NUMBER_OF_SOLN_SATS, HEADER_PRN_LIST, HEADER_END);
        }

        /** Check if parser can handle line.
         * @param line line to parse
         * @return true if parser can handle the specified line
         */
        public boolean canHandle(final String line) {
            return pattern.matcher(line).matches();
        }

        /** Parse existing date - time - zone formats.
         * If zone field is not missing, a proper Orekit date can be created and set into clock file object.
         * This feature depends on the date format.
         * @param dateString the whole date - time - zone string
         * @param pi holder for transient data
         */
        private static void parseDateTimeZone(final String dateString, final ParseInfo pi) {

            String date = "";
            String time = "";
            String zone = "";
            DateComponents dateComponents = null;
            TimeComponents timeComponents = null;

            if (DATE_PATTERN_1.matcher(dateString).matches()) {

                date = dateString.substring(0, 10).trim();
                time = dateString.substring(11, 16).trim();
                zone = dateString.substring(16).trim();

            } else if (DATE_PATTERN_2.matcher(dateString).matches()) {

                date = dateString.substring(0, 8).trim();
                time = dateString.substring(9, 16).trim();
                zone = dateString.substring(16).trim();

                if (!zone.equals("")) {
                    // Get date and time components
                    dateComponents = new DateComponents(Integer.parseInt(date.substring(0, 4)),
                                                        Integer.parseInt(date.substring(4, 6)),
                                                        Integer.parseInt(date.substring(6, 8)));
                    timeComponents = new TimeComponents(Integer.parseInt(time.substring(0, 2)),
                                                        Integer.parseInt(time.substring(2, 4)),
                                                        Integer.parseInt(time.substring(4, 6)));

                }

            } else if (DATE_PATTERN_3.matcher(dateString).matches()) {

                date = dateString.substring(0, 11).trim();
                time = dateString.substring(11, 17).trim();
                zone = dateString.substring(17).trim();

            } else if (DATE_PATTERN_4.matcher(dateString).matches()) {

                date = dateString.substring(0, 9).trim();
                time = dateString.substring(9, 15).trim();
                zone = dateString.substring(15).trim();

            } else if (DATE_PATTERN_5.matcher(dateString).matches()) {

                date = dateString.substring(0, 11).trim();
                time = dateString.substring(11, 20).trim();

            } else {
                // Format is not handled or date is missing. Do nothing...
            }

            pi.file.setCreationDateString(date);
            pi.file.setCreationTimeString(time);
            pi.file.setCreationTimeZoneString(zone);

            if (dateComponents != null) {
                pi.file.setCreationDate(new AbsoluteDate(dateComponents,
                                                         timeComponents,
                                                         TimeSystem.parseTimeSystem(zone).getTimeScale(pi.timeScales)));
            }
        }
    }

}

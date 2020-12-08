/* Copyright 2002-2012 Space Applications Services
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
package org.orekit.gnss.clock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.TimeSystem;
import org.orekit.gnss.clock.ClockFile.ClockDataType;
import org.orekit.gnss.clock.ClockFile.ReferenceClock;
import org.orekit.gnss.corrections.AppliedDCBS;
import org.orekit.gnss.corrections.AppliedPCVS;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.utils.IERSConventions;

/** A parser for the clock file from the IGS.
 * This parser can handle version 3.04 of the clock files.
 * @see <a href="ftp://igs.org/pub/data/format/rinex_clock304.txt">Clock file format</a>
 *
 * @author Thomas Paulet
 */

public class ClockFileParser {

    /** Spaces delimiters. */
    private static final String SPACES = "\\s+";

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
     * @see #ClockFileParser(Function)
     */
    @DefaultDataContext
    public ClockFileParser() {
        this(ClockFileParser::guessFrame);
    }


    /**
     * Create an clock file parser and specify the frame builder.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param frameBuilder         is a function that can construct a frame from a clock file
     *                             coordinate system string. The coordinate system can be
     *                             any 5 character string e.g. ITR92, IGb08.
     * @see #ClockFileParser(Function, TimeScales)
     */
    @DefaultDataContext
    public ClockFileParser(final Function<? super String, ? extends Frame> frameBuilder) {
        this(frameBuilder, DataContext.getDefault().getTimeScales());
    }

    /** Constructor, build the IGS clock file parser.
     * @param frameBuilder         is a function that can construct a frame from a clock file
     *                             coordinate system string. The coordinate system can be
     *                             any 5 character string e.g. ITR92, IGb08.
     * @param timeScales           the set of time scales used for parsing dates.
     */
    public ClockFileParser(final Function<? super String, ? extends Frame> frameBuilder,
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
     * @return defaultly, return ITRF based on 2010 conventions,
     * with tidal effects considered during EOP interpolation.
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
     * @param stream to read the IGS clock file from.
     * @return a parsed IGS clock file.
     * @throws IOException     if {@code stream} throws one.
     * @see #parse(String)
     * @see #parse(BufferedReader, String)
     */
    public ClockFile parse(final InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return parse(reader, stream.toString());
        }
    }


    public ClockFile parse(final String fileName) throws IOException, OrekitException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(fileName),
                                                             StandardCharsets.UTF_8)) {
            return parse(reader, fileName);
        }
    }


    public ClockFile parse(final BufferedReader reader,
                           final String fileName) throws IOException {

        // initialize internal data structures
        final ParseInfo pi = new ParseInfo();

        int lineNumber = 0;
        Stream<LineParser> candidateParsers = Stream.of(LineParser.HEADER_VERSION);
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            ++lineNumber;
            final String l = line;
            final Optional<LineParser> selected = candidateParsers.filter(p -> p.canHandle(l)).findFirst();
            if (selected.isPresent()) {
                try {
                    selected.get().parse(line, pi);
                } catch (StringIndexOutOfBoundsException | NumberFormatException e) {
                    throw new OrekitException(e,
                                              OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              lineNumber, fileName, line);
                }
                candidateParsers = selected.get().allowedNext();
            } else {
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          lineNumber, fileName, line);
            }
        }

        // we never reached the EOF marker
        return pi.file;

    }

    private class ParseInfo {

        /** Set of time scales for parsing dates. */
        private final TimeScales timeScales;

        /** The corresponding clock file object. */
        private ClockFile file;

        /** Current satellite system for observation type parsing. */
        private SatelliteSystem currentSatelliteSystem;

        /** Current number of observation types for observation type parsing. */
        private int currentNumberOfObsTypes;

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

        /**Current data values. */
        private double[] currentDataValues;

        /** Constructor, build the ParseInfo object. */
        protected ParseInfo () {

            this.timeScales = ClockFileParser.this.timeScales;
            this.file = new ClockFile(frameBuilder);
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
                    pi.file.setFormatVersion(version);

                    // Second element is clock file indicator
                    scanner.next();

                    // Last element represents the satellite system
                    final SatelliteSystem satelliteSystem = SatelliteSystem.parseSatelliteSystem(scanner.next());
                    pi.file.setSatelliteSystem(satelliteSystem);
                    pi.file.setTimeScale(satelliteSystem.getDefaultTimeSystem(pi.timeScales));
                }
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_PROGRAM);
            }
        },

        /** Parser for generating program and emiting agency. */
        HEADER_PROGRAM("^.+PGM / RUN BY / DATE( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // First element of the name of the generating program
                final String programName = line.substring(0, 19).trim();
                pi.file.setProgramName(programName);

                // Second element is the name of the emiting agency
                final String agencyName = line.substring(21, 40).trim();
                pi.file.setAgencyName(agencyName);

                // Third element represents the creation date
                final String creationDateString = line.substring(42, 50);
                pi.file.setCreationDateString(creationDateString);

                // Fourth elment represents the creation time
                final String creationTimeString = line.substring(52, 58);
                pi.file.setCreationTimeString(creationTimeString);

                // Fifth element is the creation time zone
                final String creationTimeZoneString = line.substring(59, 63).trim();
                pi.file.setCreationTimeZoneString(creationTimeZoneString);

                // Get creation date in Orekit format
                final DateComponents dateComponents = new DateComponents(Integer.parseInt(creationDateString.substring(0, 4)),
                                                                         Integer.parseInt(creationDateString.substring(4, 6)),
                                                                         Integer.parseInt(creationDateString.substring(6, 8)));
                final TimeComponents timeComponents = new TimeComponents(Integer.parseInt(creationTimeString.substring(0, 2)),
                                                                         Integer.parseInt(creationTimeString.substring(2, 4)),
                                                                         Integer.parseInt(creationTimeString.substring(4, 6)));
                final AbsoluteDate creationDate = new AbsoluteDate(dateComponents,
                                                                   timeComponents,
                                                                   TimeSystem.parseTimeSystem(creationTimeZoneString).getTimeScale(pi.timeScales));
                pi.file.setCreationDate(creationDate);

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_COMMENT, HEADER_SYSTEM_OBS, HEADER_TIME_SYSTEM, HEADER_LEAP_SECONDS, HEADER_LEAP_SECONDS_GNSS);
            }
        },

        /** Parser for comments. */
        HEADER_COMMENT("^.+COMMENT( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                pi.file.addComment(line.substring(0, 65).trim());
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_COMMENT, HEADER_SYSTEM_OBS, HEADER_TIME_SYSTEM, HEADER_LEAP_SECONDS, HEADER_LEAP_SECONDS_GNSS);
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
                    final int numberOfObsTypes = scanner.nextInt();
                    pi.currentNumberOfObsTypes = numberOfObsTypes;

                    // There are at most 13 observation data types in a line
                    for (int i = 0; i < FastMath.min(numberOfObsTypes, 13); i++) {
                        final ObservationType obsType = ObservationType.valueOf(scanner.next());
                        pi.file.AddSystemObservationType(satelliteSystem, obsType);
                    }
                }
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_SYSTEM_OBS, HEADER_SYSTEM_OBS_CONTINUATION, HEADER_TIME_SYSTEM, HEADER_LEAP_SECONDS, HEADER_LEAP_SECONDS_GNSS);
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
                    for (int i = 13; i < pi.currentNumberOfObsTypes; i++) {
                        final ObservationType obsType = ObservationType.valueOf(scanner.next());
                        pi.file.AddSystemObservationType(pi.currentSatelliteSystem, obsType);
                    }
                }
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_SYSTEM_OBS, HEADER_TIME_SYSTEM, HEADER_LEAP_SECONDS, HEADER_LEAP_SECONDS_GNSS);
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

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_LEAP_SECONDS, HEADER_LEAP_SECONDS_GNSS, HEADER_DBCS, HEADER_PCVS, HEADER_TYPES_OF_DATA);
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

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_LEAP_SECONDS_GNSS, HEADER_DBCS, HEADER_PCVS, HEADER_TYPES_OF_DATA);
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

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_DBCS, HEADER_PCVS, HEADER_TYPES_OF_DATA);
            }
        },

        /** Parser for applied differencial code bias corrections. */
        HEADER_DBCS("^.+SYS / DCBS APPLIED( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // First element is the related satellite system
                final SatelliteSystem satelliteSystem = SatelliteSystem.parseSatelliteSystem(line.substring(0, 1));

                // Second element is the program name
                final String progDCBS = line.substring(3, 20).trim();

                // Thrid element is the source of the corrections
                final String sourceDCBS = line.substring(22, 65).trim();

                // Check if sought fields were not actually blanks
                if (!progDCBS.equals("")) {
                    pi.file.addAppliedDCBS(new AppliedDCBS(satelliteSystem, progDCBS, sourceDCBS));
                }
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_DBCS, HEADER_PCVS, HEADER_TYPES_OF_DATA, HEADER_END);
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
                final String progPCVS = line.substring(3, 20).trim();

                // Thrid element is the source of the corrections
                final String sourcePCVS = line.substring(22, 65).trim();

                // Check if sought fields were not actually blanks
                if (!progPCVS.equals("")) {
                    pi.file.addAppliedPCVS(new AppliedPCVS(satelliteSystem, progPCVS, sourcePCVS));
                }
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_PCVS, HEADER_TYPES_OF_DATA, HEADER_END);
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

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_STATIONS_NAME, HEADER_ANALYSIS_CENTER, HEADER_END);
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

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_STATION_CLOCK_REF, HEADER_ANALYSIS_CENTER, HEADER_END);
            }
        },

        /** Parser for the reference clock in case of calibration data. */
        HEADER_STATION_CLOCK_REF("^.+STATION CLK REF( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                pi.file.setExternalClockReference(line.substring(0, 65).trim());
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_ANALYSIS_CENTER, HEADER_END);
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
                final String analysisCenterName = line.substring(5, 65).trim();
                pi.file.setAnalysisCenterName(analysisCenterName);
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_NUMBER_OF_CLOCK_REF, HEADER_NUMBER_OF_SOLN_STATIONS, HEADER_END);
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

                    // Thrid element is the end epoch of the period
                    final int endYear   = scanner.nextInt();
                    final int endMonth  = scanner.nextInt();
                    final int endDay    = scanner.nextInt();
                    final int endHour   = scanner.nextInt();
                    final int endMin    = scanner.nextInt();
                    final double endSec = scanner.nextDouble();
                    final AbsoluteDate endEpoch = new AbsoluteDate(endYear, endMonth, endDay,
                                                                   endHour, endMin, endSec,
                                                                   pi.file.getTimeScale());
                    pi.referenceClockEndDate = endEpoch;
                }
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_ANALYSIS_CLOCK_REF, HEADER_NUMBER_OF_SOLN_STATIONS, HEADER_NUMBER_OF_SOLN_SATS, HEADER_END);
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

                    // Optionally, third element is an a priori clock constraint, defaultly equal to zero
                    double clockConstraint = 0.0;
                    if (scanner.hasNextDouble()) {
                        clockConstraint = scanner.nextDouble();
                    }

                    // Add reference clock to current reference clock list
                    pi.currentReferenceClocks.add(pi.file.new ReferenceClock(referenceName, clockID, clockConstraint,
                                                                             pi.referenceClockStartDate, pi.referenceClockEndDate));

                    // Modify time span map of the reference clocks to accept the new reference clock
                    pi.file.addReferenceClockList(pi.currentReferenceClocks, pi.referenceClockStartDate);
                }
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_NUMBER_OF_CLOCK_REF, HEADER_ANALYSIS_CLOCK_REF, HEADER_NUMBER_OF_SOLN_STATIONS, HEADER_NUMBER_OF_SOLN_SATS,
                                 HEADER_END);
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

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_SOLN_STATIONS, HEADER_NUMBER_OF_SOLN_SATS, HEADER_END);
            }
        },

        /** Parser for the stations embedded in the file and the related positions. */
        HEADER_SOLN_STATIONS("^.+SOLN STA NAME / NUM( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // First element is the receiver designator
                final String designator = line.substring(0, 10).trim();

                // Second element is the receiver identifier
                final String receiverIdentifier = line.substring(10, 30).trim();

                // Third element if X coordinates, in millimeters in the file frame.
                final double x = MILLIMETER * Double.parseDouble(line.substring(30, 41).trim());

                // Fourth element if Y coordinates, in millimeters in the file frame.
                final double y = MILLIMETER * Double.parseDouble(line.substring(42, 53).trim());

                // Fifth element if Z coordinates, in millimeters in the file frame.
                final double z = MILLIMETER * Double.parseDouble(line.substring(54, 65).trim());

                pi.file.addReceiver(designator, pi.file.new Receiver(designator, receiverIdentifier, x, y, z));

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_SOLN_STATIONS, HEADER_NUMBER_OF_SOLN_SATS, HEADER_END);
            }
        },

        /** Parser for the number of satellites embedded in the file. */
        HEADER_NUMBER_OF_SOLN_SATS("^.+# OF SOLN SATS( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                    // Only element in the line is number of satellites, not used here.
                    // do nothing...

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_PRN_LIST, HEADER_END);
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

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_PRN_LIST, HEADER_DBCS, HEADER_PCVS, HEADER_END);
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
            public Stream<LineParser> allowedNext() {
                return Stream.of(CLOCK_DATA);
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

                    // Get the values in this line, they are at most 2
                    for (int i = 0; i < FastMath.min(2, pi.currentNumberOfValues); i++) {
                        pi.currentDataValues[i] = scanner.nextDouble();
                    }

                    // Check if continuation line is required
                    if (pi.currentNumberOfValues <= 2) {
                        // No continuation line is required
                        pi.file.AddClockData(pi.currentName, pi.file.new ClockDataLine(pi.currentDataType,
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
            public Stream<LineParser> allowedNext() {
                return Stream.of(CLOCK_DATA, CLOCK_DATA_CONTINUATION);
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

                    // Get the values in this continuation line
                    for (int i = 2; i < pi.currentNumberOfValues; i++) {
                        pi.currentDataValues[i] = scanner.nextDouble();
                    }

                    // Add clock data line
                    pi.file.AddClockData(pi.currentName, pi.file.new ClockDataLine(pi.currentDataType,
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
            public Stream<LineParser> allowedNext() {
                return Stream.of(CLOCK_DATA);
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
         * @return allowed parsers for next line
         */
        public abstract Stream<LineParser> allowedNext();

        /** Check if parser can handle line.
         * @param line line to parse
         * @return true if parser can handle the specified line
         */
        public boolean canHandle(final String line) {
            return pattern.matcher(line).matches();
        }
    }

}

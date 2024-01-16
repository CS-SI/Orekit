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
package org.orekit.files.sp3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.EphemerisFileParser;
import org.orekit.frames.Frame;
import org.orekit.gnss.TimeSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/** A parser for the SP3 orbit file format. It supports all formats from sp3-a
 * to sp3-d.
 * <p>
 * <b>Note:</b> this parser is thread-safe, so calling {@link #parse} from
 * different threads is allowed.
 * </p>
 * @see <a href="https://files.igs.org/pub/data/format/sp3_docu.txt">SP3-a file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/sp3c.txt">SP3-c file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/sp3d.pdf">SP3-d file format</a>
 * @author Thomas Neidhart
 * @author Luc Maisonobe
 */
public class SP3Parser implements EphemerisFileParser<SP3> {

    /** String representation of the center of ephemeris coordinate system. **/
    public static final String SP3_FRAME_CENTER_STRING = "EARTH";

    /** Spaces delimiters. */
    private static final String SPACES = "\\s+";

    /** Standard gravitational parameter in m³/s². */
    private final double mu;

    /** Number of data points to use in interpolation. */
    private final int interpolationSamples;

    /** Mapping from frame identifier in the file to a {@link Frame}. */
    private final Function<? super String, ? extends Frame> frameBuilder;

    /** Set of time scales. */
    private final TimeScales timeScales;

    /**
     * Create an SP3 parser using default values.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @see #SP3Parser(double, int, Function)
     */
    @DefaultDataContext
    public SP3Parser() {
        this(Constants.EIGEN5C_EARTH_MU, 7, SP3Parser::guessFrame);
    }

    /**
     * Create an SP3 parser and specify the extra information needed to create a {@link
     * org.orekit.propagation.Propagator Propagator} from the ephemeris data.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param mu                   is the standard gravitational parameter to use for
     *                             creating {@link org.orekit.orbits.Orbit Orbits} from
     *                             the ephemeris data. See {@link Constants}.
     * @param interpolationSamples is the number of samples to use when interpolating.
     * @param frameBuilder         is a function that can construct a frame from an SP3
     *                             coordinate system string. The coordinate system can be
     *                             any 5 character string e.g. ITR92, IGb08.
     * @see #SP3Parser(double, int, Function, TimeScales)
     */
    @DefaultDataContext
    public SP3Parser(final double mu,
                     final int interpolationSamples,
                     final Function<? super String, ? extends Frame> frameBuilder) {
        this(mu, interpolationSamples, frameBuilder,
                DataContext.getDefault().getTimeScales());
    }

    /**
     * Create an SP3 parser and specify the extra information needed to create a {@link
     * org.orekit.propagation.Propagator Propagator} from the ephemeris data.
     *
     * @param mu                   is the standard gravitational parameter to use for
     *                             creating {@link org.orekit.orbits.Orbit Orbits} from
     *                             the ephemeris data. See {@link Constants}.
     * @param interpolationSamples is the number of samples to use when interpolating.
     * @param frameBuilder         is a function that can construct a frame from an SP3
     *                             coordinate system string. The coordinate system can be
     * @param timeScales           the set of time scales used for parsing dates.
     * @since 10.1
     */
    public SP3Parser(final double mu,
                     final int interpolationSamples,
                     final Function<? super String, ? extends Frame> frameBuilder,
                     final TimeScales timeScales) {
        this.mu                   = mu;
        this.interpolationSamples = interpolationSamples;
        this.frameBuilder         = frameBuilder;
        this.timeScales           = timeScales;
    }

    /**
     * Default string to {@link Frame} conversion for {@link #SP3Parser()}.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param name of the frame.
     * @return ITRF based on 2010 conventions,
     * with tidal effects considered during EOP interpolation.
     */
    @DefaultDataContext
    private static Frame guessFrame(final String name) {
        return DataContext.getDefault().getFrames()
                .getITRF(IERSConventions.IERS_2010, false);
    }

    @Override
    public SP3 parse(final DataSource source) {

        try (Reader reader = source.getOpener().openReaderOnce();
             BufferedReader br = (reader == null) ? null : new BufferedReader(reader)) {

            if (br == null) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, source.getName());
            }

            // initialize internal data structures
            final ParseInfo pi = new ParseInfo(source.getName());

            int lineNumber = 0;
            Iterable<LineParser> candidateParsers = Collections.singleton(LineParser.HEADER_VERSION);
            nextLine:
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    ++lineNumber;
                    for (final LineParser candidate : candidateParsers) {
                        if (candidate.canHandle(line)) {
                            try {
                                candidate.parse(line, pi);
                                if (pi.done) {
                                    break nextLine;
                                }
                                candidateParsers = candidate.allowedNext();
                                continue nextLine;
                            } catch (StringIndexOutOfBoundsException | NumberFormatException e) {
                                throw new OrekitException(e,
                                                          OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                          lineNumber, pi.fileName, line);
                            }
                        }
                    }

                    // no parsers found for this line
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              lineNumber, pi.fileName, line);

                }

            pi.file.validate(true, pi.fileName);
            return pi.file;

        } catch (IOException ioe) {
            throw new OrekitException(ioe, LocalizedCoreFormats.SIMPLE_MESSAGE, ioe.getLocalizedMessage());
        }

    }

    /** Transient data used for parsing a sp3 file. The data is kept in a
     * separate data structure to make the parser thread-safe.
     * <p><b>Note</b>: The class intentionally does not provide accessor
     * methods, as it is only used internally for parsing a SP3 file.</p>
     */
    private class ParseInfo {

        /** File name.
         * @since 12.0
         */
        private final String fileName;

        /** Set of time scales for parsing dates. */
        private final TimeScales timeScales;

        /** The corresponding SP3File object. */
        private SP3 file;

        /** The latest epoch as read from the SP3 file. */
        private AbsoluteDate latestEpoch;

        /** The latest position as read from the SP3 file. */
        private Vector3D latestPosition;

        /** The latest position accuracy as read from the SP3 file.
         * @since 12.0
         */
        private Vector3D latestPositionAccuracy;

        /** The latest clock value as read from the SP3 file. */
        private double latestClock;

        /** The latest clock value as read from the SP3 file.
         * @since 12.0
         */
        private double latestClockAccuracy;

        /** The latest clock event flag as read from the SP3 file.
         * @since 12.0
         */
        private boolean latestClockEvent;

        /** The latest clock prediction flag as read from the SP3 file.
         * @since 12.0
         */
        private boolean latestClockPrediction;

        /** The latest orbit maneuver event flag as read from the SP3 file.
         * @since 12.0
         */
        private boolean latestOrbitManeuverEvent;

        /** The latest orbit prediction flag as read from the SP3 file.
         * @since 12.0
         */
        private boolean latestOrbitPrediction;

        /** Indicates if the SP3 file has velocity entries. */
        private boolean hasVelocityEntries;

        /** The timescale used in the SP3 file. */
        private TimeScale timeScale;

        /** Date and time of the file. */
        private DateTimeComponents epoch;

        /** The number of satellites as contained in the SP3 file. */
        private int maxSatellites;

        /** The number of satellites accuracies already seen. */
        private int nbAccuracies;

        /** End Of File reached indicator. */
        private boolean done;

        /** Create a new {@link ParseInfo} object.
         * @param fileName file name
         */
        protected ParseInfo(final String fileName) {
            this.fileName      = fileName;
            this.timeScales    = SP3Parser.this.timeScales;
            file               = new SP3(mu, interpolationSamples, frameBuilder.apply(SP3_FRAME_CENTER_STRING));
            latestEpoch        = null;
            latestPosition     = null;
            latestClock        = 0.0;
            hasVelocityEntries = false;
            epoch              = DateTimeComponents.JULIAN_EPOCH;
            timeScale          = timeScales.getGPS();
            maxSatellites      = 0;
            nbAccuracies       = 0;
            done               = false;
        }
    }

    /** Parsers for specific lines. */
    private enum LineParser {

        /** Parser for version, epoch, data used and agency information. */
        HEADER_VERSION("^#[a-z].*") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                try (Scanner s1      = new Scanner(line);
                     Scanner s2      = s1.useDelimiter(SPACES);
                     Scanner scanner = s2.useLocale(Locale.US)) {
                    scanner.skip("#");
                    final String v = scanner.next();

                    pi.file.getHeader().setVersion(v.substring(0, 1).toLowerCase().charAt(0));

                    pi.hasVelocityEntries = "V".equals(v.substring(1, 2));
                    pi.file.getHeader().setFilter(pi.hasVelocityEntries ?
                                                  CartesianDerivativesFilter.USE_PV :
                                                  CartesianDerivativesFilter.USE_P);

                    final int    year   = Integer.parseInt(v.substring(2));
                    final int    month  = scanner.nextInt();
                    final int    day    = scanner.nextInt();
                    final int    hour   = scanner.nextInt();
                    final int    minute = scanner.nextInt();
                    final double second = scanner.nextDouble();

                    pi.epoch = new DateTimeComponents(year, month, day,
                                                      hour, minute, second);

                    final int numEpochs = scanner.nextInt();
                    pi.file.getHeader().setNumberOfEpochs(numEpochs);

                    // data used indicator
                    final String fullSpec = scanner.next();
                    final List<DataUsed> dataUsed = new ArrayList<>();
                    for (final String specifier : fullSpec.split("\\+")) {
                        dataUsed.add(DataUsed.parse(specifier, pi.fileName, pi.file.getHeader().getVersion()));
                    }
                    pi.file.getHeader().setDataUsed(dataUsed);

                    pi.file.getHeader().setCoordinateSystem(scanner.next());
                    pi.file.getHeader().setOrbitTypeKey(scanner.next());
                    pi.file.getHeader().setAgency(scanner.next());
                }
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Collections.singleton(HEADER_DATE_TIME_REFERENCE);
            }

        },

        /** Parser for additional date/time references in gps/julian day notation. */
        HEADER_DATE_TIME_REFERENCE("^##.*") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                try (Scanner s1      = new Scanner(line);
                     Scanner s2      = s1.useDelimiter(SPACES);
                     Scanner scanner = s2.useLocale(Locale.US)) {
                    scanner.skip("##");

                    // gps week
                    pi.file.getHeader().setGpsWeek(scanner.nextInt());
                    // seconds of week
                    pi.file.getHeader().setSecondsOfWeek(scanner.nextDouble());
                    // epoch interval
                    pi.file.getHeader().setEpochInterval(scanner.nextDouble());
                    // modified julian day
                    pi.file.getHeader().setModifiedJulianDay(scanner.nextInt());
                    // day fraction
                    pi.file.getHeader().setDayFraction(scanner.nextDouble());
                }
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Collections.singleton(HEADER_SAT_IDS);
            }

        },

        /** Parser for satellites identifiers. */
        HEADER_SAT_IDS("^\\+ .*") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                if (pi.maxSatellites == 0) {
                    // this is the first ids line, it also contains the number of satellites
                    pi.maxSatellites = Integer.parseInt(line.substring(3, 6).trim());
                }

                final int lineLength = line.length();
                int count = pi.file.getSatelliteCount();
                int startIdx = 9;
                while (count++ < pi.maxSatellites && (startIdx + 3) <= lineLength) {
                    final String satId = line.substring(startIdx, startIdx + 3).trim();
                    if (satId.length() > 0) {
                        pi.file.addSatellite(satId);
                    }
                    startIdx += 3;
                }
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(HEADER_SAT_IDS, HEADER_ACCURACY);
            }

        },

        /** Parser for general accuracy information for each satellite. */
        HEADER_ACCURACY("^\\+\\+.*") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                final int lineLength = line.length();
                int startIdx = 9;
                while (pi.nbAccuracies < pi.maxSatellites && (startIdx + 3) <= lineLength) {
                    final String sub = line.substring(startIdx, startIdx + 3).trim();
                    if (sub.length() > 0) {
                        final int exponent = Integer.parseInt(sub);
                        // the accuracy is calculated as 2**exp (in mm)
                        pi.file.getHeader().setAccuracy(pi.nbAccuracies++,
                                                        SP3Utils.siAccuracy(SP3Utils.POSITION_ACCURACY_UNIT,
                                                                            SP3Utils.POS_VEL_BASE_ACCURACY,
                                                                            exponent));
                    }
                    startIdx += 3;
                }
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(HEADER_ACCURACY, HEADER_TIME_SYSTEM);
            }

        },

        /** Parser for time system. */
        HEADER_TIME_SYSTEM("^%c.*") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                if (pi.file.getHeader().getType() == null) {
                    // this the first custom fields line, the only one really used
                    pi.file.getHeader().setType(SP3FileType.parse(line.substring(3, 5).trim()));

                    // now identify the time system in use
                    final String tsStr = line.substring(9, 12).trim();
                    final TimeSystem ts;
                    if (tsStr.equalsIgnoreCase("ccc")) {
                        ts = TimeSystem.GPS;
                    } else {
                        ts = TimeSystem.valueOf(tsStr);
                    }
                    pi.file.getHeader().setTimeSystem(ts);
                    pi.timeScale = ts.getTimeScale(pi.timeScales);

                    // now we know the time scale used, we can set the file epoch
                    pi.file.getHeader().setEpoch(new AbsoluteDate(pi.epoch, pi.timeScale));
                }

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(HEADER_TIME_SYSTEM, HEADER_STANDARD_DEVIATIONS);
            }

        },

        /** Parser for standard deviations of position/velocity/clock components. */
        HEADER_STANDARD_DEVIATIONS("^%f.*") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                final double posVelBase = Double.parseDouble(line.substring(3, 13).trim());
                if (posVelBase != 0.0) {
                    // (mm or 10⁻⁴ mm/s)
                    pi.file.getHeader().setPosVelBase(posVelBase);
                }

                final double clockBase = Double.parseDouble(line.substring(14, 26).trim());
                if (clockBase != 0.0) {
                    // (ps or 10⁻⁴ ps/s)
                    pi.file.getHeader().setClockBase(clockBase);
                }
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(HEADER_STANDARD_DEVIATIONS, HEADER_CUSTOM_PARAMETERS);
            }

        },

        /** Parser for custom parameters. */
        HEADER_CUSTOM_PARAMETERS("^%i.*") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // ignore additional custom parameters
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(HEADER_CUSTOM_PARAMETERS, HEADER_COMMENTS);
            }

        },

        /** Parser for comments. */
        HEADER_COMMENTS("^[%]?/\\*.*|") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                pi.file.getHeader().addComment(line.substring(line.indexOf('*') + 1).trim());
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(HEADER_COMMENTS, DATA_EPOCH);
            }

        },

        /** Parser for epoch. */
        DATA_EPOCH("^\\* .*") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                final int    year;
                final int    month;
                final int    day;
                final int    hour;
                final int    minute;
                final double second;
                try (Scanner s1      = new Scanner(line);
                     Scanner s2      = s1.useDelimiter(SPACES);
                     Scanner scanner = s2.useLocale(Locale.US)) {
                    scanner.skip("\\*");
                    year   = scanner.nextInt();
                    month  = scanner.nextInt();
                    day    = scanner.nextInt();
                    hour   = scanner.nextInt();
                    minute = scanner.nextInt();
                    second = scanner.nextDouble();
                }

                // some SP3 files have weird epochs as in the following three examples, where
                // the middle dates are wrong
                //
                // *  2016  7  6 16 58  0.00000000
                // PL51  11872.234459   3316.551981    101.400098 999999.999999
                // VL51   8054.606014 -27076.640110 -53372.762255 999999.999999
                // *  2016  7  6 16 60  0.00000000
                // PL51  11948.228978   2986.113872   -538.901114 999999.999999
                // VL51   4605.419303 -27972.588048 -53316.820671 999999.999999
                // *  2016  7  6 17  2  0.00000000
                // PL51  11982.652569   2645.786926  -1177.549463 999999.999999
                // VL51   1128.248622 -28724.293303 -53097.358387 999999.999999
                //
                // *  2016  7  6 23 58  0.00000000
                // PL51   3215.382310  -7958.586164   8812.395707
                // VL51 -18058.659942 -45834.335707 -34496.540437
                // *  2016  7  7 24  0  0.00000000
                // PL51   2989.229334  -8494.421415   8385.068555
                // VL51 -19617.027447 -43444.824985 -36706.159070
                // *  2016  7  7  0  2  0.00000000
                // PL51   2744.983592  -9000.639164   7931.904779
                // VL51 -21072.925764 -40899.633288 -38801.567078
                //
                // * 2021 12 31  0  0  0.00000000
                // PL51   6578.459330   5572.231927  -8703.502054
                // VL51  -5356.007694 -48869.881161 -35036.676469
                // * 2022  1  0  0  2  0.00000000
                // PL51   6499.035610   4978.263048  -9110.135595
                // VL51  -7881.633197 -50092.564035 -32717.740919
                // * 2022  1  0  0  4  0.00000000
                // PL51   6389.313975   4370.794537  -9488.314264
                // VL51 -10403.797055 -51119.231402 -30295.421935
                // In the first case, the date should really be 2016  7  6 17  0  0.00000000,
                // i.e as the minutes field overflows, the hours field should be incremented
                // In the second case, the date should really be 2016  7  7  0  0  0.00000000,
                // i.e. as the hours field overflows, the day field should be kept as is
                // we cannot be sure how carry was managed when these bogus files were written
                // so we try different options, incrementing or not previous field, and selecting
                // the closest one to expected date
                // In the third case, there are two different errors: the date is globally
                // shifted to the left by one character, and the day is 0 instead of 1
                DateComponents dc = day == 0 ?
                                    new DateComponents(new DateComponents(year, month, 1), -1) :
                                    new DateComponents(year, month, day);
                final List<AbsoluteDate> candidates = new ArrayList<>();
                int h = hour;
                int m = minute;
                double s = second;
                if (s >= 60.0) {
                    s -= 60;
                    addCandidate(candidates, dc, h, m, s, pi.timeScale);
                    m++;
                }
                if (m > 59) {
                    m = 0;
                    addCandidate(candidates, dc, h, m, s, pi.timeScale);
                    h++;
                }
                if (h > 23) {
                    h = 0;
                    addCandidate(candidates, dc, h, m, s, pi.timeScale);
                    dc = new DateComponents(dc, 1);
                }
                addCandidate(candidates, dc, h, m, s, pi.timeScale);
                final AbsoluteDate expected = pi.latestEpoch == null ?
                                              pi.file.getHeader().getEpoch() :
                                              pi.latestEpoch.shiftedBy(pi.file.getHeader().getEpochInterval());
                pi.latestEpoch = null;
                for (final AbsoluteDate candidate : candidates) {
                    if (FastMath.abs(candidate.durationFrom(expected)) < 0.01 * pi.file.getHeader().getEpochInterval()) {
                        pi.latestEpoch = candidate;
                    }
                }
                if (pi.latestEpoch == null) {
                    // no date recognized, just parse again the initial fields
                    // in order to generate again an exception
                    pi.latestEpoch = new AbsoluteDate(year, month, day, hour, minute, second, pi.timeScale);
                }

            }

            /** Add an epoch candidate to a list.
             * @param candidates list of candidates
             * @param dc date components
             * @param hour hour number from 0 to 23
             * @param minute minute number from 0 to 59
             * @param second second number from 0.0 to 60.0 (excluded)
             * @param timeScale time scale
             * @since 11.1.1
             */
            private void addCandidate(final List<AbsoluteDate> candidates, final DateComponents dc,
                                      final int hour, final int minute, final double second,
                                      final TimeScale timeScale) {
                try {
                    candidates.add(new AbsoluteDate(dc, new TimeComponents(hour, minute, second), timeScale));
                } catch (OrekitIllegalArgumentException oiae) {
                    // ignored
                }
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Collections.singleton(DATA_POSITION);
            }

        },

        /** Parser for position. */
        DATA_POSITION("^P.*") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                final String satelliteId = line.substring(1, 4).trim();

                if (!pi.file.containsSatellite(satelliteId)) {
                    pi.latestPosition = Vector3D.ZERO;
                } else {

                    final SP3Header header = pi.file.getHeader();

                    // the position values are in km and have to be converted to m
                    pi.latestPosition = new Vector3D(SP3Utils.POSITION_UNIT.toSI(Double.parseDouble(line.substring(4, 18).trim())),
                                                     SP3Utils.POSITION_UNIT.toSI(Double.parseDouble(line.substring(18, 32).trim())),
                                                     SP3Utils.POSITION_UNIT.toSI(Double.parseDouble(line.substring(32, 46).trim())));

                    // clock (microsec)
                    pi.latestClock = SP3Utils.CLOCK_UNIT.toSI(line.trim().length() <= 46 ?
                                                              SP3Utils.DEFAULT_CLOCK_VALUE :
                                                              Double.parseDouble(line.substring(46, 60).trim()));

                    if (pi.latestPosition.getNorm() > 0) {

                        if (line.length() < 69 ||
                            line.substring(61, 63).trim().length() == 0 ||
                            line.substring(64, 66).trim().length() == 0 ||
                            line.substring(67, 69).trim().length() == 0) {
                            pi.latestPositionAccuracy = null;
                        } else {
                            pi.latestPositionAccuracy = new Vector3D(SP3Utils.siAccuracy(SP3Utils.POSITION_ACCURACY_UNIT,
                                                                                         header.getPosVelBase(),
                                                                                         Integer.parseInt(line.substring(61, 63).trim())),
                                                                     SP3Utils.siAccuracy(SP3Utils.POSITION_ACCURACY_UNIT,
                                                                                         header.getPosVelBase(),
                                                                                         Integer.parseInt(line.substring(64, 66).trim())),
                                                                     SP3Utils.siAccuracy(SP3Utils.POSITION_ACCURACY_UNIT,
                                                                                         header.getPosVelBase(),
                                                                                         Integer.parseInt(line.substring(67, 69).trim())));
                        }

                        if (line.length() < 73 || line.substring(70, 73).trim().length() == 0) {
                            pi.latestClockAccuracy    = Double.NaN;
                        } else {
                            pi.latestClockAccuracy    = SP3Utils.siAccuracy(SP3Utils.CLOCK_ACCURACY_UNIT,
                                                                            header.getClockBase(),
                                                                            Integer.parseInt(line.substring(70, 73).trim()));
                        }

                        pi.latestClockEvent         = line.length() < 75 ? false : line.substring(74, 75).equals("E");
                        pi.latestClockPrediction    = line.length() < 76 ? false : line.substring(75, 76).equals("P");
                        pi.latestOrbitManeuverEvent = line.length() < 79 ? false : line.substring(78, 79).equals("M");
                        pi.latestOrbitPrediction    = line.length() < 80 ? false : line.substring(79, 80).equals("P");

                        if (!pi.hasVelocityEntries) {
                            final SP3Coordinate coord =
                                            new SP3Coordinate(pi.latestEpoch,
                                                              pi.latestPosition,           pi.latestPositionAccuracy,
                                                              Vector3D.ZERO,               null,
                                                              pi.latestClock,              pi.latestClockAccuracy,
                                                              0.0,                         Double.NaN,
                                                              pi.latestClockEvent,         pi.latestClockPrediction,
                                                              pi.latestOrbitManeuverEvent, pi.latestOrbitPrediction);
                            pi.file.getEphemeris(satelliteId).addCoordinate(coord, header.getEpochInterval());
                        }
                    }
                }
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(DATA_EPOCH, DATA_POSITION, DATA_POSITION_CORRELATION, DATA_VELOCITY, EOF);
            }

        },

        /** Parser for position correlation. */
        DATA_POSITION_CORRELATION("^EP.*") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // ignored for now
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(DATA_EPOCH, DATA_POSITION, DATA_VELOCITY, EOF);
            }

        },

        /** Parser for velocity. */
        DATA_VELOCITY("^V.*") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                final String satelliteId = line.substring(1, 4).trim();

                if (pi.file.containsSatellite(satelliteId) && pi.latestPosition.getNorm() > 0) {

                    final SP3Header header = pi.file.getHeader();

                    // the velocity values are in dm/s and have to be converted to m/s
                    final Vector3D velocity = new Vector3D(SP3Utils.VELOCITY_UNIT.toSI(Double.parseDouble(line.substring(4, 18).trim())),
                                                           SP3Utils.VELOCITY_UNIT.toSI(Double.parseDouble(line.substring(18, 32).trim())),
                                                           SP3Utils.VELOCITY_UNIT.toSI(Double.parseDouble(line.substring(32, 46).trim())));

                    // clock rate in file is 1e-4 us / s
                    final double clockRateChange = SP3Utils.CLOCK_RATE_UNIT.toSI(line.trim().length() <= 46 ?
                                                                                 SP3Utils.DEFAULT_CLOCK_RATE_VALUE :
                                                                                 Double.parseDouble(line.substring(46, 60).trim()));

                    final Vector3D velocityAccuracy;
                    if (line.length() < 69 ||
                        line.substring(61, 63).trim().length() == 0 ||
                        line.substring(64, 66).trim().length() == 0 ||
                        line.substring(67, 69).trim().length() == 0) {
                        velocityAccuracy  = null;
                    } else {
                        velocityAccuracy = new Vector3D(SP3Utils.siAccuracy(SP3Utils.VELOCITY_ACCURACY_UNIT,
                                                                            header.getPosVelBase(),
                                                                            Integer.parseInt(line.substring(61, 63).trim())),
                                                        SP3Utils.siAccuracy(SP3Utils.VELOCITY_ACCURACY_UNIT,
                                                                            header.getPosVelBase(),
                                                                            Integer.parseInt(line.substring(64, 66).trim())),
                                                        SP3Utils.siAccuracy(SP3Utils.VELOCITY_ACCURACY_UNIT,
                                                                            header.getPosVelBase(),
                                                                            Integer.parseInt(line.substring(67, 69).trim())));
                    }

                    final double clockRateAccuracy;
                    if (line.length() < 73 || line.substring(70, 73).trim().length() == 0) {
                        clockRateAccuracy = Double.NaN;
                    } else {
                        clockRateAccuracy = SP3Utils.siAccuracy(SP3Utils.CLOCK_RATE_ACCURACY_UNIT,
                                                                header.getClockBase(),
                                                                Integer.parseInt(line.substring(70, 73).trim()));
                    }

                    final SP3Coordinate coord =
                            new SP3Coordinate(pi.latestEpoch,
                                              pi.latestPosition,           pi.latestPositionAccuracy,
                                              velocity,                    velocityAccuracy,
                                              pi.latestClock,              pi.latestClockAccuracy,
                                              clockRateChange,             clockRateAccuracy,
                                              pi.latestClockEvent,         pi.latestClockPrediction,
                                              pi.latestOrbitManeuverEvent, pi.latestOrbitPrediction);
                    pi.file.getEphemeris(satelliteId).addCoordinate(coord, header.getEpochInterval());
                }
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(DATA_EPOCH, DATA_POSITION, DATA_VELOCITY_CORRELATION, EOF);
            }

        },

        /** Parser for velocity correlation. */
        DATA_VELOCITY_CORRELATION("^EV.*") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // ignored for now
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(DATA_EPOCH, DATA_POSITION, EOF);
            }

        },

        /** Parser for End Of File marker. */
        EOF("^[eE][oO][fF]\\s*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                pi.done = true;
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Collections.singleton(EOF);
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
        public abstract Iterable<LineParser> allowedNext();

        /** Check if parser can handle line.
         * @param line line to parse
         * @return true if parser can handle the specified line
         */
        public boolean canHandle(final String line) {
            return pattern.matcher(line).matches();
        }

    }

}

/* Copyright 2002-2012 Space Applications Services
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
package org.orekit.files.sp3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.EphemerisFileParser;
import org.orekit.files.sp3.SP3File.SP3Coordinate;
import org.orekit.files.sp3.SP3File.SP3FileType;
import org.orekit.files.sp3.SP3File.TimeSystem;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
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
 * @see <a href="ftp://igs.org/pub/data/format/sp3_docu.txt">SP3-a file format</a>
 * @see <a href="ftp://igs.org/pub/data/format/sp3c.txt">SP3-c file format</a>
 * @see <a href="ftp://igs.org/pub/data/format/sp3d.pdf">SP3-d file format</a>
 * @author Thomas Neidhart
 * @author Luc Maisonobe
 */
public class SP3Parser implements EphemerisFileParser {

    /** Spaces delimiters. */
    private static final String SPACES = "\\s+";

    /** One millimeter, in meters. */
    private static final double MILLIMETER = 1.0e-3;

    /** Standard gravitational parameter in m^3 / s^2. */
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
        this.mu = mu;
        this.interpolationSamples = interpolationSamples;
        this.frameBuilder = frameBuilder;
        this.timeScales = timeScales;
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

    /**
     * Parse a SP3 file from an input stream using the UTF-8 charset.
     *
     * <p> This method creates a {@link BufferedReader} from the stream and as such this
     * method may read more data than necessary from {@code stream} and the additional
     * data will be lost. The other parse methods do not have this issue.
     *
     * @param stream to read the SP3 file from.
     * @return a parsed SP3 file.
     * @throws IOException     if {@code stream} throws one.
     * @see #parse(String)
     * @see #parse(BufferedReader, String)
     */
    public SP3File parse(final InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return parse(reader, stream.toString());
        }
    }

    @Override
    public SP3File parse(final String fileName) throws IOException, OrekitException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(fileName),
                                                             StandardCharsets.UTF_8)) {
            return parse(reader, fileName);
        }
    }

    @Override
    public SP3File parse(final BufferedReader reader,
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
            if (pi.done) {
                if (pi.nbEpochs != pi.file.getNumberOfEpochs()) {
                    throw new OrekitException(OrekitMessages.SP3_NUMBER_OF_EPOCH_MISMATCH,
                                              pi.nbEpochs, fileName, pi.file.getNumberOfEpochs());
                }
                return pi.file;
            }
        }

        // we never reached the EOF marker
        throw new OrekitException(OrekitMessages.SP3_UNEXPECTED_END_OF_FILE, lineNumber);

    }

    /** Returns the {@link SP3FileType} that corresponds to a given string in a SP3 file.
     * @param fileType file type as string
     * @return file type as enum
     */
    private static SP3FileType getFileType(final String fileType) {
        SP3FileType type = SP3FileType.UNDEFINED;
        if ("G".equalsIgnoreCase(fileType)) {
            type = SP3FileType.GPS;
        } else if ("M".equalsIgnoreCase(fileType)) {
            type = SP3FileType.MIXED;
        } else if ("R".equalsIgnoreCase(fileType)) {
            type = SP3FileType.GLONASS;
        } else if ("L".equalsIgnoreCase(fileType)) {
            type = SP3FileType.LEO;
        } else if ("E".equalsIgnoreCase(fileType)) {
            type = SP3FileType.GALILEO;
        } else if ("C".equalsIgnoreCase(fileType)) {
            type = SP3FileType.COMPASS;
        } else if ("J".equalsIgnoreCase(fileType)) {
            type = SP3FileType.QZSS;
        }
        return type;
    }

    /** Transient data used for parsing a sp3 file. The data is kept in a
     * separate data structure to make the parser thread-safe.
     * <p><b>Note</b>: The class intentionally does not provide accessor
     * methods, as it is only used internally for parsing a SP3 file.</p>
     */
    private class ParseInfo {

        /** Set of time scales for parsing dates. */
        private final TimeScales timeScales;

        /** The corresponding SP3File object. */
        private SP3File file;

        /** The latest epoch as read from the SP3 file. */
        private AbsoluteDate latestEpoch;

        /** The latest position as read from the SP3 file. */
        private Vector3D latestPosition;

        /** The latest clock value as read from the SP3 file. */
        private double latestClock;

        /** Indicates if the SP3 file has velocity entries. */
        private boolean hasVelocityEntries;

        /** The timescale used in the SP3 file. */
        private TimeScale timeScale;

        /** The number of satellites as contained in the SP3 file. */
        private int maxSatellites;

        /** The number of satellites accuracies already seen. */
        private int nbAccuracies;

        /** The number of epochs already seen. */
        private int nbEpochs;

        /** End Of File reached indicator. */
        private boolean done;

        /** The base for pos/vel. */
        //private double posVelBase;

        /** The base for clock/rate. */
        //private double clockBase;

        /** Create a new {@link ParseInfo} object. */
        protected ParseInfo() {
            this.timeScales = SP3Parser.this.timeScales;
            file               = new SP3File(mu, interpolationSamples, frameBuilder);
            latestEpoch        = null;
            latestPosition     = null;
            latestClock        = 0.0;
            hasVelocityEntries = false;
            timeScale          = timeScales.getGPS();
            maxSatellites      = 0;
            nbAccuracies       = 0;
            nbEpochs           = 0;
            done               = false;
            //posVelBase = 2d;
            //clockBase = 2d;
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

                    final char version = v.substring(0, 1).toLowerCase().charAt(0);
                    if (version != 'a' && version != 'b' && version != 'c' && version != 'd') {
                        throw new OrekitException(OrekitMessages.SP3_UNSUPPORTED_VERSION, version);
                    }

                    pi.hasVelocityEntries = "V".equals(v.substring(1, 2));
                    pi.file.setFilter(pi.hasVelocityEntries ?
                                      CartesianDerivativesFilter.USE_PV :
                                      CartesianDerivativesFilter.USE_P);

                    final int    year   = Integer.parseInt(v.substring(2));
                    final int    month  = scanner.nextInt();
                    final int    day    = scanner.nextInt();
                    final int    hour   = scanner.nextInt();
                    final int    minute = scanner.nextInt();
                    final double second = scanner.nextDouble();

                    final AbsoluteDate epoch = new AbsoluteDate(year, month, day,
                                                                hour, minute, second,
                                                                pi.timeScales.getGPS());

                    pi.file.setEpoch(epoch);

                    final int numEpochs = scanner.nextInt();
                    pi.file.setNumberOfEpochs(numEpochs);

                    // data used indicator
                    pi.file.setDataUsed(scanner.next());

                    pi.file.setCoordinateSystem(scanner.next());
                    pi.file.setOrbitTypeKey(scanner.next());
                    pi.file.setAgency(scanner.next());
                }
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_DATE_TIME_REFERENCE);
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
                    pi.file.setGpsWeek(scanner.nextInt());
                    // seconds of week
                    pi.file.setSecondsOfWeek(scanner.nextDouble());
                    // epoch interval
                    pi.file.setEpochInterval(scanner.nextDouble());
                    // julian day
                    pi.file.setJulianDay(scanner.nextInt());
                    // day fraction
                    pi.file.setDayFraction(scanner.nextDouble());
                }
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_SAT_IDS);
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
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_SAT_IDS, HEADER_ACCURACY);
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
                        pi.file.setAccuracy(pi.nbAccuracies++, (2 << exponent) * MILLIMETER);
                    }
                    startIdx += 3;
                }
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_ACCURACY, HEADER_TIME_SYSTEM);
            }

        },

        /** Parser for time system. */
        HEADER_TIME_SYSTEM("^%c.*") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                if (pi.file.getType() == null) {
                    // this the first custom fields line, the only one really used
                    pi.file.setType(getFileType(line.substring(3, 5).trim()));

                    // now identify the time system in use
                    final String tsStr = line.substring(9, 12).trim();
                    pi.file.setTimeScaleString(tsStr);
                    final TimeSystem ts;
                    if (tsStr.equalsIgnoreCase("ccc")) {
                        ts = TimeSystem.GPS;
                    } else {
                        ts = TimeSystem.valueOf(tsStr);
                    }
                    pi.file.setTimeSystem(ts);

                    switch (ts) {
                        case GPS:
                            pi.timeScale = pi.timeScales.getGPS();
                            break;

                        case GAL:
                            pi.timeScale = pi.timeScales.getGST();
                            break;

                        case GLO:
                            pi.timeScale = pi.timeScales.getGLONASS();
                            break;

                        case QZS:
                            pi.timeScale = pi.timeScales.getQZSS();
                            break;

                        case TAI:
                            pi.timeScale = pi.timeScales.getTAI();
                            break;

                        case UTC:
                            pi.timeScale = pi.timeScales.getUTC();
                            break;

                        default:
                            pi.timeScale = pi.timeScales.getGPS();
                            break;
                    }
                    pi.file.setTimeScale(pi.timeScale);
                }

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_TIME_SYSTEM, HEADER_STANDARD_DEVIATIONS);
            }

        },

        /** Parser for standard deviations of position/velocity/clock components. */
        HEADER_STANDARD_DEVIATIONS("^%f.*") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // String base = line.substring(3, 13).trim();
                // if (!base.equals("0.0000000")) {
                //    // (mm or 10**-4 mm/sec)
                //    pi.posVelBase = Double.valueOf(base);
                // }

                // base = line.substring(14, 26).trim();
                // if (!base.equals("0.000000000")) {
                //    // (psec or 10**-4 psec/sec)
                //    pi.clockBase = Double.valueOf(base);
                // }
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_STANDARD_DEVIATIONS, HEADER_CUSTOM_PARAMETERS);
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
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_CUSTOM_PARAMETERS, HEADER_COMMENTS);
            }

        },

        /** Parser for comments. */
        HEADER_COMMENTS("^/\\*.*") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // ignore comments
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_COMMENTS, DATA_EPOCH);
            }

        },

        /** Parser for epoch. */
        DATA_EPOCH("^\\* .*") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                final int    year   = Integer.parseInt(line.substring(3, 7).trim());
                final int    month  = Integer.parseInt(line.substring(8, 10).trim());
                final int    day    = Integer.parseInt(line.substring(11, 13).trim());
                final int    hour   = Integer.parseInt(line.substring(14, 16).trim());
                final int    minute = Integer.parseInt(line.substring(17, 19).trim());
                final double second = Double.parseDouble(line.substring(20, 31).trim());

                pi.latestEpoch = new AbsoluteDate(year, month, day,
                                                  hour, minute, second,
                                                  pi.timeScale);
                pi.nbEpochs++;
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(DATA_POSITION);
            }

        },

        /** Parser for position. */
        DATA_POSITION("^P.*") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                final String satelliteId = line.substring(1, 4).trim();

                if (!pi.file.containsSatellite(satelliteId)) {
                    pi.latestPosition = null;
                } else {
                    final double x = Double.parseDouble(line.substring(4, 18).trim());
                    final double y = Double.parseDouble(line.substring(18, 32).trim());
                    final double z = Double.parseDouble(line.substring(32, 46).trim());

                    // the position values are in km and have to be converted to m
                    pi.latestPosition = new Vector3D(x * 1000, y * 1000, z * 1000);

                    // clock (microsec)
                    pi.latestClock =
                            Double.parseDouble(line.substring(46, 60).trim()) * 1e-6;

                    // the additional items are optional and not read yet

                    // if (line.length() >= 73) {
                    // // x-sdev (b**n mm)
                    // int xStdDevExp = Integer.valueOf(line.substring(61,
                    // 63).trim());
                    // // y-sdev (b**n mm)
                    // int yStdDevExp = Integer.valueOf(line.substring(64,
                    // 66).trim());
                    // // z-sdev (b**n mm)
                    // int zStdDevExp = Integer.valueOf(line.substring(67,
                    // 69).trim());
                    // // c-sdev (b**n psec)
                    // int cStdDevExp = Integer.valueOf(line.substring(70,
                    // 73).trim());
                    //
                    // pi.posStdDevRecord =
                    // new PositionStdDevRecord(FastMath.pow(pi.posVelBase, xStdDevExp),
                    // FastMath.pow(pi.posVelBase,
                    // yStdDevExp), FastMath.pow(pi.posVelBase, zStdDevExp),
                    // FastMath.pow(pi.clockBase, cStdDevExp));
                    //
                    // String clockEventFlag = line.substring(74, 75);
                    // String clockPredFlag = line.substring(75, 76);
                    // String maneuverFlag = line.substring(78, 79);
                    // String orbitPredFlag = line.substring(79, 80);
                    // }

                    if (!pi.hasVelocityEntries) {
                        final SP3Coordinate coord =
                                new SP3Coordinate(pi.latestEpoch,
                                                  pi.latestPosition,
                                                  pi.latestClock);
                        pi.file.addSatelliteCoordinate(satelliteId, coord);
                    }
                }
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(DATA_EPOCH, DATA_POSITION, DATA_POSITION_CORRELATION, DATA_VELOCITY, EOF);
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
            public Stream<LineParser> allowedNext() {
                return Stream.of(DATA_EPOCH, DATA_POSITION, DATA_VELOCITY, EOF);
            }

        },

        /** Parser for velocity. */
        DATA_VELOCITY("^V.*") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                final String satelliteId = line.substring(1, 4).trim();

                if (pi.file.containsSatellite(satelliteId)) {
                    final double xv = Double.parseDouble(line.substring(4, 18).trim());
                    final double yv = Double.parseDouble(line.substring(18, 32).trim());
                    final double zv = Double.parseDouble(line.substring(32, 46).trim());

                    // the velocity values are in dm/s and have to be converted to m/s
                    final Vector3D velocity = new Vector3D(xv / 10d, yv / 10d, zv / 10d);

                    // clock rate in file is 1e-4 us / s
                    final double clockRateChange =
                            Double.parseDouble(line.substring(46, 60).trim()) * 1e-4;

                    // the additional items are optional and not read yet

                    // if (line.length() >= 73) {
                    // // xvel-sdev (b**n 10**-4 mm/sec)
                    // int xVstdDevExp = Integer.valueOf(line.substring(61,
                    // 63).trim());
                    // // yvel-sdev (b**n 10**-4 mm/sec)
                    // int yVstdDevExp = Integer.valueOf(line.substring(64,
                    // 66).trim());
                    // // zvel-sdev (b**n 10**-4 mm/sec)
                    // int zVstdDevExp = Integer.valueOf(line.substring(67,
                    // 69).trim());
                    // // clkrate-sdev (b**n 10**-4 psec/sec)
                    // int clkStdDevExp = Integer.valueOf(line.substring(70,
                    // 73).trim());
                    // }

                    final SP3Coordinate coord =
                            new SP3Coordinate(pi.latestEpoch,
                                              pi.latestPosition,
                                              velocity,
                                              pi.latestClock,
                                              clockRateChange);
                    pi.file.addSatelliteCoordinate(satelliteId, coord);
                }
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(DATA_EPOCH, DATA_POSITION, DATA_VELOCITY_CORRELATION, EOF);
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
            public Stream<LineParser> allowedNext() {
                return Stream.of(DATA_EPOCH, DATA_POSITION, EOF);
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
            public Stream<LineParser> allowedNext() {
                return Stream.of(EOF);
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

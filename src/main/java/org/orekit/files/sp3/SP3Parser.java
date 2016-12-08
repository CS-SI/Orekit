/* Copyright 2002-2012 Space Applications Services
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
package org.orekit.files.sp3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;
import java.util.function.Function;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.EphemerisFileParser;
import org.orekit.files.sp3.SP3File.SP3Coordinate;
import org.orekit.files.sp3.SP3File.SP3FileType;
import org.orekit.files.sp3.SP3File.SP3OrbitType;
import org.orekit.files.sp3.SP3File.TimeSystem;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/** A parser for the SP3 orbit file format. It supports the original format as
 * well as the latest SP3-c version.
 * <p>
 * <b>Note:</b> this parser is thread-safe, so calling {@link #parse} from
 * different threads is allowed.
 * </p>
 * @see <a href="http://igscb.jpl.nasa.gov/igscb/data/format/sp3_docu.txt">SP3-a file format</a>
 * @see <a href="http://igscb.jpl.nasa.gov/igscb/data/format/sp3c.txt">SP3-c file format</a>
 * @author Thomas Neidhart
 */
public class SP3Parser implements EphemerisFileParser {

    /** Standard gravitational parameter in m^3 / s^2. */
    private final double mu;
    /** Number of data points to use in interpolation. */
    private final int interpolationSamples;
    /** Mapping from frame identifier in the file to a {@link Frame}. */
    private final Function<? super String, ? extends Frame> frameBuilder;

    /**
     * Create an SP3 parser using default values.
     *
     * @see #SP3Parser(double, int, Function)
     */
    public SP3Parser() {
        this(Constants.EIGEN5C_EARTH_MU, 7, SP3Parser::guessFrame);
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
     *                             any 5 character string e.g. ITR92, IGb08.
     */
    public SP3Parser(final double mu,
                     final int interpolationSamples,
                     final Function<? super String, ? extends Frame> frameBuilder) {
        this.mu = mu;
        this.interpolationSamples = interpolationSamples;
        this.frameBuilder = frameBuilder;
    }

    /**
     * Default string to {@link Frame} conversion for {@link #SP3Parser()}.
     *
     * @param name of the frame.
     * @return ITRF based on 2010 conventions.
     */
    private static Frame guessFrame(final String name) {
        try {
            return FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        } catch (OrekitException e) {
            throw new RuntimeException(e);
        }
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
     * @throws OrekitException if the ephemeris file cannot be parsed.
     * @throws IOException     if {@code stream} throws one.
     * @see #parse(String)
     * @see #parse(BufferedReader, String)
     */
    public SP3File parse(final InputStream stream) throws OrekitException, IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return parse(reader, stream.toString());
        }
    }

    @Override
    public SP3File parse(final BufferedReader reader,
                         final String fileName) throws OrekitException, IOException {

        // initialize internal data structures
        final ParseInfo pi = new ParseInfo();

        String line = null;
        int lineNumber = 1;
        try {
            while (lineNumber < 23) {
                line = reader.readLine();
                if (line == null) {
                    throw new OrekitException(OrekitMessages.SP3_UNEXPECTED_END_OF_FILE, lineNumber - 1);
                } else {
                    parseHeaderLine(lineNumber++, line, pi);
                }
            }

            // now handle the epoch/position/velocity entries

            boolean done = false;
            do {
                line = reader.readLine();
                if (line == null || "EOF".equalsIgnoreCase(line.trim())) {
                    done = true;
                } else if (line.length() > 0) {
                    parseContentLine(line, pi);
                }
            } while (!done);
        } finally {
            try {
                reader.close();
            } catch (IOException e1) {
                // ignore
            }
        }

        return pi.file;
    }

    /** Parses a header line from the SP3 file (line number 1 - 22).
     * @param lineNumber the current line number
     * @param line the line as read from the SP3 file
     * @param pi the current {@link ParseInfo} object
     * @throws OrekitException if a non-supported construct is found
     */
    private void parseHeaderLine(final int lineNumber, final String line, final ParseInfo pi)
        throws OrekitException {

        final SP3File file = pi.file;

        try (Scanner s1      = new Scanner(line);
             Scanner s2      = s1.useDelimiter("\\s+");
             Scanner scanner = s2.useLocale(Locale.US)) {

            // CHECKSTYLE: stop FallThrough check

            switch (lineNumber) {

                // version, epoch, data used and agency information
                case 1: {
                    scanner.skip("#");
                    final String v = scanner.next();

                    final char version = v.substring(0, 1).toLowerCase().charAt(0);
                    if (version != 'a' && version != 'b' && version != 'c') {
                        throw new OrekitException(OrekitMessages.SP3_UNSUPPORTED_VERSION, version);
                    }

                    pi.hasVelocityEntries = "V".equals(v.substring(1, 2));
                    file.setFilter(pi.hasVelocityEntries ?
                            CartesianDerivativesFilter.USE_PV :
                            CartesianDerivativesFilter.USE_P);

                    final int year = Integer.parseInt(v.substring(2));
                    final int month = scanner.nextInt();
                    final int day = scanner.nextInt();
                    final int hour = scanner.nextInt();
                    final int minute = scanner.nextInt();
                    final double second = scanner.nextDouble();

                    final AbsoluteDate epoch = new AbsoluteDate(year, month, day,
                                                                hour, minute, second,
                                                                TimeScalesFactory.getGPS());

                    file.setEpoch(epoch);

                    final int numEpochs = scanner.nextInt();
                    file.setNumberOfEpochs(numEpochs);

                    // data used indicator
                    file.setDataUsed(scanner.next());

                    file.setCoordinateSystem(scanner.next());
                    file.setOrbitType(SP3OrbitType.parseType(scanner.next()));
                    file.setAgency(scanner.next());
                    break;
                }

                // additional date/time references in gps/julian day notation
                case 2: {
                    scanner.skip("##");

                    // gps week
                    file.setGpsWeek(scanner.nextInt());
                    // seconds of week
                    file.setSecondsOfWeek(scanner.nextDouble());
                    // epoch interval
                    file.setEpochInterval(scanner.nextDouble());
                    // julian day
                    file.setJulianDay(scanner.nextInt());
                    // day fraction
                    file.setDayFraction(scanner.nextDouble());
                    break;
                }

                // line 3 contains the number of satellites
                case 3:
                    pi.maxSatellites = Integer.parseInt(line.substring(4, 6).trim());
                    // fall-through intended - the line contains already the first entries

                    // the following 4 lines contain additional satellite ids
                case 4:
                case 5:
                case 6:
                case 7: {
                    final int lineLength = line.length();
                    int count = file.getSatelliteCount();
                    int startIdx = 9;
                    while (count++ < pi.maxSatellites && (startIdx + 3) <= lineLength) {
                        final String satId = line.substring(startIdx, startIdx + 3).trim();
                        file.addSatellite(satId);
                        startIdx += 3;
                    }
                    break;
                }

                // the following 5 lines contain general accuracy information for each satellite
                case 8:
                case 9:
                case 10:
                case 11:
                case 12: {
                    final int lineLength = line.length();
                    int satIdx = (lineNumber - 8) * 17;
                    int startIdx = 9;
                    while (satIdx < pi.maxSatellites && (startIdx + 3) <= lineLength) {
                        final int exponent = Integer.parseInt(line.substring(startIdx, startIdx + 3).trim());
                        // the accuracy is calculated as 2**exp (in m) -> can be safely
                        // converted to an integer as there will be no fraction
                        file.setAccuracy(satIdx++, FastMath.pow(2d, exponent));
                        startIdx += 3;
                    }
                    break;
                }

                case 13: {
                    file.setType(getFileType(line.substring(3, 5).trim()));

                    // now identify the time system in use
                    final String tsStr = line.substring(9, 12).trim();
                    file.setTimeScaleString(tsStr);
                    final TimeSystem ts;
                    if (tsStr.equalsIgnoreCase("ccc")) {
                        ts = TimeSystem.GPS;
                    } else {
                        ts = TimeSystem.valueOf(tsStr);
                    }
                    file.setTimeSystem(ts);

                    switch (ts) {
                        case GPS:
                            pi.timeScale = TimeScalesFactory.getGPS();
                            break;

                        case GAL:
                            pi.timeScale = TimeScalesFactory.getGST();
                            break;

                        case GLO:
                            pi.timeScale = TimeScalesFactory.getGLONASS();
                            break;

                        case QZS:
                            pi.timeScale = TimeScalesFactory.getQZSS();

                        case TAI:
                            pi.timeScale = TimeScalesFactory.getTAI();
                            break;

                        case UTC:
                            pi.timeScale = TimeScalesFactory.getUTC();
                            break;

                        default:
                            pi.timeScale = TimeScalesFactory.getGPS();
                            break;
                    }
                    file.setTimeScale(pi.timeScale);

                    break;
                }

                case 14:
                    // ignore additional custom fields
                    break;

                    // load base numbers for the standard deviations of
                    // position/velocity/clock components
                case 15: {
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
                    break;
                }

                case 16:
                case 17:
                case 18:
                    // ignore additional custom parameters
                    break;

                case 19:
                case 20:
                case 21:
                case 22:
                    // ignore comment lines
                    break;
                default:
                    // ignore -> method should only be called up to line 22
                    break;
            }

            // CHECKSTYLE: resume FallThrough check

        }

    }

    /** Parses a single content line as read from the SP3 file.
     * @param line a string containing the line
     * @param pi the current {@link ParseInfo} object
     */
    private void parseContentLine(final String line, final ParseInfo pi) {
        // EP and EV lines are ignored so far

        final SP3File file = pi.file;

        switch (line.charAt(0)) {
            case '*': {
                final int year = Integer.parseInt(line.substring(3, 7).trim());
                final int month = Integer.parseInt(line.substring(8, 10).trim());
                final int day = Integer.parseInt(line.substring(11, 13).trim());
                final int hour = Integer.parseInt(line.substring(14, 16).trim());
                final int minute = Integer.parseInt(line.substring(17, 19).trim());
                final double second = Double.parseDouble(line.substring(20, 31).trim());

                pi.latestEpoch = new AbsoluteDate(year, month, day,
                                                  hour, minute, second,
                                                  pi.timeScale);
                break;
            }

            case 'P': {
                final String satelliteId = line.substring(1, 4).trim();

                if (!file.containsSatellite(satelliteId)) {
                    pi.latestPosition = null;
                } else {
                    final double x = Double.parseDouble(line.substring(4, 18).trim());
                    final double y = Double.parseDouble(line.substring(18, 32).trim());
                    final double z = Double.parseDouble(line.substring(32, 46).trim());

                    // the position values are in km and have to be converted to m
                    pi.latestPosition = new Vector3D(x * 1000, y * 1000, z * 1000);

                    // clock (microsec)
                    pi.latestClock =
                            Double.parseDouble(line.substring(46, 60).trim()) * 1e6;

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
                        file.addSatelliteCoordinate(satelliteId, coord);
                    }
                }
                break;
            }

            case 'V': {
                final String satelliteId = line.substring(1, 4).trim();

                if (file.containsSatellite(satelliteId)) {
                    final double xv = Double.parseDouble(line.substring(4, 18).trim());
                    final double yv = Double.parseDouble(line.substring(18, 32).trim());
                    final double zv = Double.parseDouble(line.substring(32, 46).trim());

                    // the velocity values are in dm/s and have to be converted to m/s
                    final Vector3D velocity = new Vector3D(xv / 10d, yv / 10d, zv / 10d);

                    // clock rate in file is 1e-4 us / s
                    final double clockRateChange =
                            Double.parseDouble(line.substring(46, 60).trim()) * 1e10;

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
                    file.addSatelliteCoordinate(satelliteId, coord);
                }
                break;
            }

            default:
                // ignore everything else
                break;
        }
    }

    /** Returns the {@link SP3FileType} that corresponds to a given string in a SP3 file.
     * @param fileType file type as string
     * @return file type as enum
     */
    private SP3FileType getFileType(final String fileType) {
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

        /** The base for pos/vel. */
        //private double posVelBase;

        /** The base for clock/rate. */
        //private double clockBase;

        /** Create a new {@link ParseInfo} object. */
        protected ParseInfo() {
            file = new SP3File(mu, interpolationSamples, frameBuilder);
            latestEpoch = null;
            latestPosition = null;
            latestClock = 0.0d;
            hasVelocityEntries = false;
            timeScale = TimeScalesFactory.getGPS();
            maxSatellites = 0;
            //posVelBase = 2d;
            //clockBase = 2d;
        }
    }
}

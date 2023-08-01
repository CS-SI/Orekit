/* Copyright 2002-2023 Andrew Goetz
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
package org.orekit.files.stk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.EphemerisFileParser;
import org.orekit.files.stk.STKEphemerisFile.STKCoordinateSystem;
import org.orekit.files.stk.STKEphemerisFile.STKEphemeris;
import org.orekit.files.stk.STKEphemerisFile.STKEphemerisSegment;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.Month;
import org.orekit.time.UTCScale;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Parser of {@link STKEphemerisFile}s.
 *
 * <p> The STK ephemeris file format specification is quite extensive and this implementation does not
 * attempt (nor is it possible, given the lack of an STK scenario to provide context) to support all
 * possible variations of the format. The following keywords are recognized (case-insensitive):
 * <table>
 *     <caption>Recognized Keywords</caption>
 *     <thead>
 *         <tr>
 *             <th>Keyword</th>
 *             <th>Supported</th>
 *             <th>Comment</th>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td>stk.v.*.*</td>
 *             <td>Yes</td>
 *             <td>STK version number</td>
 *         </tr>
 *         <tr>
 *             <td>BEGIN/END Ephemeris</td>
 *             <td>Yes</td>
 *             <td></td>
 *         </tr>
 *         <tr>
 *             <td>ScenarioEpoch</td>
 *             <td>Yes</td>
 *             <td>Gregorian UTC time format (<code>dd mmm yyyy hh:mm:ss.s</code>) assumed;
 *                 the <code>TimeFormat</code> keyword is not recognized.</td>
 *         </tr>
 *         <tr>
 *             <td>CentralBody</td>
 *             <td>No</td>
 *             <td>Class constructors require gravitational parameter.</td>
 *         </tr>
 *         <tr>
 *             <td>CoordinateSystem</td>
 *             <td>Yes</td>
 *             <td>Implementation uses a frame mapping to map {@link STKCoordinateSystem}s to {@link Frame}s.</td>
 *         </tr>
 *         <tr>
 *             <td>DistanceUnit</td>
 *             <td>Yes</td>
 *             <td>Only <code>Meters</code> and <code>Kilometers</code> are supported.</td>
 *         </tr>
 *         <tr>
 *             <td>InterpolationMethod</td>
 *             <td>No</td>
 *             <td>The Orekit EphemerisSegmentPropagator class uses
 *             {@link org.orekit.utils.TimeStampedPVCoordinatesHermiteInterpolator#interpolate(AbsoluteDate, Stream)}
 *             to do Hermite interpolation, so the value of <code>InterpolationMethod</code>, if present, is
 *             ignored.</td>
 *         </tr>
 *         <tr>
 *             <td>InterpolationSamplesM1</td>
 *             <td>Yes</td>
 *             <td>Note that the <code>InterpolationMethod</code> keyword is ignored, but the value of
 *             <code>InterpolationSamplesM1</code> will be used to determine the number of sample points in the
 *             Hermite interpolator used by Orekit.</td>
 *         </tr>
 *         <tr>
 *             <td>NumberOfEphemerisPoints</td>
 *             <td>Yes</td>
 *             <td></td>
 *         </tr>
 *         <tr>
 *             <td>BEGIN/END SegmentBoundaryTimes</td>
 *             <td>Yes</td>
 *             <td></td>
 *         </tr>
 *     </tbody>
 * </table>
 *
 * <p> Any keyword in the format specification which is not explicitly named in the above table is not recognized and
 * will cause a parse exception. Those keywords that are listed above as recognized but not supported are simply
 * ignored.
 *
 * <p> The following ephemeris formats are recognized and supported:
 * <ul>
 *     <li>EphemerisTimePos</li>
 *     <li>EphemerisTimePosVel</li>
 *     <li>EphemerisTimePosVelAcc</li>
 * </ul>
 * Any ephemeris format in the format specification which is not explicitly named in the above list is not recognized
 * and will cause an exception.
 *
 * @author Andrew Goetz
 * @since 12.0
 */
public class STKEphemerisFileParser implements EphemerisFileParser<STKEphemerisFile> {

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** Pattern for ignorable lines. Comments are preceded by '#'. */
    private static final Pattern IGNORABLE_LINE = Pattern.compile("^\\s*(#.*)?");

    /** Regular expression that matches anything. */
    private static final String MATCH_ANY_REGEX = ".*";

    /** Recognized keywords. */
    private static final List<LineParser> KEYWORDS = Arrays.asList(
            LineParser.NUMBER_OF_EPHEMERIS_POINTS,
            LineParser.SCENARIO_EPOCH,
            LineParser.INTERPOLATION_METHOD,
            LineParser.INTERPOLATION_SAMPLESM1,
            LineParser.CENTRAL_BODY,
            LineParser.COORDINATE_SYSTEM,
            LineParser.BEGIN_SEGMENT_BOUNDARY_TIMES,
            LineParser.EPHEMERIS_TIME_POS,
            LineParser.EPHEMERIS_TIME_POS_VEL,
            LineParser.EPHEMERIS_TIME_POS_VEL_ACC
    );

    /** Satellite id. */
    private final String satelliteId;

    /** Gravitational parameter (m^3/s^2). */
    private final double mu;

    /** UTC time scale. */
    private final UTCScale utc;

    /** Mapping of STK coordinate system to Orekit reference frame. */
    private final Map<STKCoordinateSystem, Frame> frameMapping;

    /**
     * Constructs a {@link STKEphemerisFileParser} instance.
     * @param satelliteId satellite id for satellites parsed by the parser
     * @param mu gravitational parameter (m^3/s^2)
     * @param utc UTC scale for parsed dates
     * @param frameMapping mapping from STK coordinate system to Orekit frame
     */
    public STKEphemerisFileParser(final String satelliteId, final double mu, final UTCScale utc,
            final Map<STKCoordinateSystem, Frame> frameMapping) {
        this.satelliteId = Objects.requireNonNull(satelliteId);
        this.mu = mu;
        this.utc = Objects.requireNonNull(utc);
        this.frameMapping = Collections.unmodifiableMap(new EnumMap<>(frameMapping));
    }

    @Override
    public STKEphemerisFile parse(final DataSource source) {

        try (Reader reader = source.getOpener().openReaderOnce();
             BufferedReader br = (reader == null) ? null : new BufferedReader(reader)) {

            if (br == null) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, source.getName());
            }

            // initialize internal data structures
            final ParseInfo pi = new ParseInfo();

            int lineNumber = 0;
            Iterable<LineParser> parsers = Collections.singleton(LineParser.VERSION);
            nextLine:
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                ++lineNumber;
                if (pi.file != null) {
                    break;
                } else if (IGNORABLE_LINE.matcher(line).matches()) {
                    continue;
                }
                for (final LineParser candidate : parsers) {
                    if (candidate.canHandle(line)) {
                        try {
                            candidate.parse(line, pi);
                            parsers = candidate.allowedNext();
                            continue nextLine;
                        } catch (StringIndexOutOfBoundsException | IllegalArgumentException e) {
                            throw new OrekitException(e, OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, lineNumber,
                                                      source.getName(), line);
                        }
                    }
                }

                // no parsers found for this line
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, lineNumber, source.getName(),
                                          line);

                }

            if (pi.file != null) {
                return pi.file;
            } else {
                throw new OrekitException(OrekitMessages.STK_UNEXPECTED_END_OF_FILE, lineNumber);
            }

        } catch (IOException ioe) {
            throw new OrekitException(ioe, LocalizedCoreFormats.SIMPLE_MESSAGE, ioe.getLocalizedMessage());
        }
    }

    /**
     * Transient data used for parsing an STK ephemeris file. The data is kept in a
     * separate data structure to make the parser thread-safe.
     * <p>
     * <b>Note</b>: The class intentionally does not provide accessor methods, as it
     * is only used internally for parsing an STK ephemeris file.
     * </p>
     */
    private final class ParseInfo {

        /** STK version. */
        private String stkVersion;

        /** Scenario epoch. */
        private AbsoluteDate scenarioEpoch; // technically optional but required here b/c no STK scenario for context

        /** Number of ephemeris points. */
        private Integer numberOfEphemerisPoints;

        /** One less than the number of points used in the interpolation. */
        private int interpolationSamplesM1;

        /** Cartesian derivatives filter for interpolation. */
        private CartesianDerivativesFilter cartesianDerivativesFilter;

        /** Coordinate system. */
        private STKCoordinateSystem coordinateSystem;

        /** Distance unit. */
        private STKDistanceUnit distanceUnit;

        /** Number of ephemeris points read. */
        private int numberOfEphemerisPointsRead;

        /** Segment boundary times. */
        private SortedSet<Double> segmentBoundaryTimes;

        /** Ephemeris segments. */
        private List<STKEphemerisSegment> ephemerisSegments;

        /** Last-saved ephemeris. */
        private TimeStampedPVCoordinates lastSavedEphemeris;

        /** Ephemeris for current segment. */
        private List<TimeStampedPVCoordinates> segmentEphemeris;

        /** Completely parsed ephemeris file. */
        private STKEphemerisFile file;

        /**
         * Constructs a {@link ParseInfo} instance.
         */
        private ParseInfo() {
            // Set defaults.
            this.distanceUnit = STKDistanceUnit.METERS;
            this.interpolationSamplesM1 = 5;
            this.coordinateSystem = STKCoordinateSystem.FIXED;

            // Other initialization.
            this.ephemerisSegments = new ArrayList<>();
            this.segmentBoundaryTimes = new TreeSet<>();
            this.segmentEphemeris = new ArrayList<>();
        }

        /**
         * Returns the UTC scale.
         * @return UTC scale
         */
        private UTCScale getUTCScale() {
            return utc;
        }

        /**
         * Adds an ephemeris point.
         * @param time time
         * @param pvCoordinates position/velocity coordinates
         */
        private void addEphemeris(final double time, final PVCoordinates pvCoordinates) {
            if (numberOfEphemerisPoints != null && numberOfEphemerisPointsRead == numberOfEphemerisPoints) {
                return;
            }
            final AbsoluteDate date = scenarioEpoch.shiftedBy(time);
            final TimeStampedPVCoordinates timeStampedPVCoordinates = new TimeStampedPVCoordinates(date, pvCoordinates);
            if (segmentBoundaryTimes.contains(time) && numberOfEphemerisPointsRead > 0) {
                if (segmentEphemeris.isEmpty()) { // begin new segment
                    if (!date.equals(lastSavedEphemeris.getDate())) {
                        segmentEphemeris.add(lastSavedEphemeris); // no gaps allowed
                    }
                    segmentEphemeris.add(timeStampedPVCoordinates);
                } else { // end segment
                    segmentEphemeris.add(timeStampedPVCoordinates);
                    ephemerisSegments.add(new STKEphemerisSegment(mu, getFrame(), 1 + interpolationSamplesM1,
                            cartesianDerivativesFilter, segmentEphemeris));
                    segmentEphemeris = new ArrayList<>();
                }
            } else {
                segmentEphemeris.add(timeStampedPVCoordinates);
            }
            lastSavedEphemeris = timeStampedPVCoordinates;
            ++numberOfEphemerisPointsRead;
        }

        /**
         * Returns the frame.
         * @return frame
         */
        private Frame getFrame() {
            final STKCoordinateSystem stkCoordinateSystem = coordinateSystem == null ? STKCoordinateSystem.FIXED :
                    coordinateSystem;
            final Frame frame = frameMapping.get(stkCoordinateSystem);
            if (frame == null) {
                throw new OrekitException(OrekitMessages.STK_UNMAPPED_COORDINATE_SYSTEM, stkCoordinateSystem);
            }
            return frame;
        }

        /**
         * Completes parsing.
         */
        private void complete() {
            if (!segmentEphemeris.isEmpty()) {
                ephemerisSegments.add(new STKEphemerisSegment(mu, getFrame(), 1 + interpolationSamplesM1,
                        cartesianDerivativesFilter, segmentEphemeris));
            }
            final STKEphemeris ephemeris = new STKEphemeris(satelliteId, mu, ephemerisSegments);
            file = new STKEphemerisFile(stkVersion, satelliteId, ephemeris);
        }

    }

    /** Parser for specific line. */
    private enum LineParser {

        /** STK version. */
        VERSION("^stk\\.v\\.\\d+\\.\\d+$") {

            @Override
            public void parse(final String line, final ParseInfo pi) {
                pi.stkVersion = line;
            }

            @Override
            public Iterable<LineParser> allowedNext() {
                return Collections.singleton(BEGIN_EPHEMERIS);
            }

        },

        /** BEGIN Ephemeris keyword. */
        BEGIN_EPHEMERIS("^\\s*BEGIN Ephemeris\\s*(#.*)?$") {

            @Override
            public void parse(final String line, final ParseInfo pi) {
                // nothing to do
            }

            @Override
            public Iterable<LineParser> allowedNext() {
                return KEYWORDS;
            }

        },

        /** NumberOfEphemerisPoints keyword. */
        NUMBER_OF_EPHEMERIS_POINTS("^\\s*NumberOfEphemerisPoints\\s*\\d+\\s*(#.*)?$") {

            @Override
            public void parse(final String line, final ParseInfo pi) {
                pi.numberOfEphemerisPoints = Integer.parseInt(SEPARATOR.split(line.trim())[1]);
            }

            @Override
            public Iterable<LineParser> allowedNext() {
                return KEYWORDS;
            }

        },

        /** ScenarioEpoch keyword. */
        SCENARIO_EPOCH("^\\s*ScenarioEpoch\\s* \\d{2} [a-zA-Z]{3} \\d{4} \\d{2}:\\d{2}:\\d{2}(\\.\\d*)?\\s*(#.*)?$") {

            @Override
            public void parse(final String line, final ParseInfo pi) {
                final String[] tokens = SEPARATOR.split(line.trim());
                final int dayOfMonth = Integer.parseInt(tokens[1]);
                final Month month = Month.parseMonth(tokens[2]);
                final int year = Integer.parseInt(tokens[3]);
                final int hour = Integer.parseInt(tokens[4].substring(0, 2));
                final int minute = Integer.parseInt(tokens[4].substring(3, 5));
                final double seconds = Double.parseDouble(tokens[4].substring(6));
                final DateTimeComponents dateTimeComponents = new DateTimeComponents(year, month, dayOfMonth, hour, minute, seconds);
                pi.scenarioEpoch = new AbsoluteDate(dateTimeComponents, pi.getUTCScale());
            }

            @Override
            public Iterable<LineParser> allowedNext() {
                return KEYWORDS;
            }

        },

        /** InterpolationMethod keyword. */
        INTERPOLATION_METHOD("^\\s*InterpolationMethod\\s+[a-zA-Z]+\\s*(#.*)?$") {

            @Override
            public void parse(final String line, final ParseInfo pi) {
                // do nothing; this keyword is recognized, but ignored and unsupported
            }

            @Override
            public Iterable<LineParser> allowedNext() {
                return KEYWORDS;
            }

        },

        /** InterpolationSamplesM1 keyword. */
        INTERPOLATION_SAMPLESM1("^\\s*InterpolationSamplesM1\\s+\\d+\\s*(#.*)?$") {

            @Override
            public void parse(final String line, final ParseInfo pi) {
                pi.interpolationSamplesM1 = Integer.parseInt(SEPARATOR.split(line.trim())[1]);
            }

            @Override
            public Iterable<LineParser> allowedNext() {
                return KEYWORDS;
            }

        },

        /** CentralBody keyword. */
        CENTRAL_BODY("^\\s*CentralBody\\s+[a-zA-Z]+\\s*(#.*)?$") {

            @Override
            public void parse(final String line, final ParseInfo pi) {
                // do nothing; this keyword is recognized, but ignored and unsupported; Earth
                // assumed
            }

            @Override
            public Iterable<LineParser> allowedNext() {
                return KEYWORDS;
            }

        },

        /** CoordinateSystem keyword. */
        COORDINATE_SYSTEM("^\\s*CoordinateSystem\\s+[a-zA-Z0-9]+\\s*(#.*)?$") {

            @Override
            public void parse(final String line, final ParseInfo pi) {
                pi.coordinateSystem = STKCoordinateSystem.parse(SEPARATOR.split(line.trim())[1]);
            }

            @Override
            public Iterable<LineParser> allowedNext() {
                return KEYWORDS;
            }

        },

        /** DistanceUnit keyword. */
        DISTANCE_UNIT("^\\s*DistanceUnit\\s+[a-zA-Z0-9]+\\s*(#.*)?$") {

            @Override
            public void parse(final String line, final ParseInfo pi) {
                pi.distanceUnit = STKDistanceUnit.valueOf(SEPARATOR.split(line.trim())[1].toUpperCase());
            }

            @Override
            public Iterable<LineParser> allowedNext() {
                return KEYWORDS;
            }

        },

        /** BEGIN SegmentBoundaryTimes keyword. */
        BEGIN_SEGMENT_BOUNDARY_TIMES("^\\s*BEGIN SegmentBoundaryTimes\\s*(#.*)?$") {

            @Override
            public void parse(final String line, final ParseInfo pi) {
                // nothing to be done
            }

            @Override
            public Iterable<LineParser> allowedNext() {
                return Collections.singleton(SEGMENT_BOUNDARY_TIME);
            }

        },

        /** Segment boundary time. */
        SEGMENT_BOUNDARY_TIME(MATCH_ANY_REGEX) {

            @Override
            public void parse(final String line, final ParseInfo pi) {
                pi.segmentBoundaryTimes.add(Double.parseDouble(SEPARATOR.split(line.trim())[0]));
            }

            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(END_SEGMENT_BOUNDARY_TIMES, SEGMENT_BOUNDARY_TIME);
            }

        },

        /** END SegmentBoundaryTimes keyword. */
        END_SEGMENT_BOUNDARY_TIMES("^\\s*END SegmentBoundaryTimes\\s*(#.*)?$") {

            @Override
            public void parse(final String line, final ParseInfo pi) {
                // nothing to be done
            }

            @Override
            public Iterable<LineParser> allowedNext() {
                return KEYWORDS;
            }

        },

        /** EphemerisTimePos keyword. */
        EPHEMERIS_TIME_POS("^\\s*EphemerisTimePos\\s*(#.*)?$") {

            @Override
            public void parse(final String line, final ParseInfo pi) {
                pi.cartesianDerivativesFilter = CartesianDerivativesFilter.USE_P;
            }

            @Override
            public Iterable<LineParser> allowedNext() {
                return Collections.singleton(EPHEMERIS_TIME_POS_DATUM);
            }

        },

        /** EphemerisTimePos datum. */
        EPHEMERIS_TIME_POS_DATUM(MATCH_ANY_REGEX) {

            @Override
            public void parse(final String line, final ParseInfo pi) {
                final String[] tokens = SEPARATOR.split(line.trim());
                final double time = Double.parseDouble(tokens[0]);
                final double px = Double.parseDouble(tokens[1]) * pi.distanceUnit.conversionToMetersFactor;
                final double py = Double.parseDouble(tokens[2]) * pi.distanceUnit.conversionToMetersFactor;
                final double pz = Double.parseDouble(tokens[3]) * pi.distanceUnit.conversionToMetersFactor;

                final Vector3D position = new Vector3D(px, py, pz);
                final Vector3D velocity = Vector3D.ZERO;

                pi.addEphemeris(time, new PVCoordinates(position, velocity));
            }

            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(END_EPHEMERIS, EPHEMERIS_TIME_POS_DATUM);
            }

        },

        /** EphemerisTimePosVel keyword. */
        EPHEMERIS_TIME_POS_VEL("^\\s*EphemerisTimePosVel\\s*(#.*)?$") {

            @Override
            public void parse(final String line, final ParseInfo pi) {
                pi.cartesianDerivativesFilter = CartesianDerivativesFilter.USE_PV;
            }

            @Override
            public Iterable<LineParser> allowedNext() {
                return Collections.singleton(EPHEMERIS_TIME_POS_VEL_DATUM);
            }

        },

        /** EphemerisTimePosVel datum. */
        EPHEMERIS_TIME_POS_VEL_DATUM(MATCH_ANY_REGEX) {

            @Override
            public void parse(final String line, final ParseInfo pi) {
                final String[] tokens = SEPARATOR.split(line.trim());
                final double time = Double.parseDouble(tokens[0]);
                final double px = Double.parseDouble(tokens[1]) * pi.distanceUnit.conversionToMetersFactor;
                final double py = Double.parseDouble(tokens[2]) * pi.distanceUnit.conversionToMetersFactor;
                final double pz = Double.parseDouble(tokens[3]) * pi.distanceUnit.conversionToMetersFactor;
                final double vx = Double.parseDouble(tokens[4]) * pi.distanceUnit.conversionToMetersFactor;
                final double vy = Double.parseDouble(tokens[5]) * pi.distanceUnit.conversionToMetersFactor;
                final double vz = Double.parseDouble(tokens[6]) * pi.distanceUnit.conversionToMetersFactor;

                final Vector3D position = new Vector3D(px, py, pz);
                final Vector3D velocity = new Vector3D(vx, vy, vz);

                pi.addEphemeris(time, new PVCoordinates(position, velocity));
            }

            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(END_EPHEMERIS, EPHEMERIS_TIME_POS_VEL_DATUM);
            }

        },

        /** EphemerisTimePosVelAcc keyword. */
        EPHEMERIS_TIME_POS_VEL_ACC("^\\s*EphemerisTimePosVelAcc\\s*(#.*)?$") {

            @Override
            public void parse(final String line, final ParseInfo pi) {
                pi.cartesianDerivativesFilter = CartesianDerivativesFilter.USE_PVA;
            }

            @Override
            public Iterable<LineParser> allowedNext() {
                return Collections.singleton(EPHEMERIS_TIME_POS_VEL_ACC_DATUM);
            }

        },

        /** EphemerisTimePosVelAcc datum. */
        EPHEMERIS_TIME_POS_VEL_ACC_DATUM(MATCH_ANY_REGEX) {

            @Override
            public void parse(final String line, final ParseInfo pi) {
                final String[] tokens = SEPARATOR.split(line.trim());
                final double time = Double.parseDouble(tokens[0]);
                final double px = Double.parseDouble(tokens[1]) * pi.distanceUnit.conversionToMetersFactor;
                final double py = Double.parseDouble(tokens[2]) * pi.distanceUnit.conversionToMetersFactor;
                final double pz = Double.parseDouble(tokens[3]) * pi.distanceUnit.conversionToMetersFactor;
                final double vx = Double.parseDouble(tokens[4]) * pi.distanceUnit.conversionToMetersFactor;
                final double vy = Double.parseDouble(tokens[5]) * pi.distanceUnit.conversionToMetersFactor;
                final double vz = Double.parseDouble(tokens[6]) * pi.distanceUnit.conversionToMetersFactor;
                final double ax = Double.parseDouble(tokens[7]) * pi.distanceUnit.conversionToMetersFactor;
                final double ay = Double.parseDouble(tokens[8]) * pi.distanceUnit.conversionToMetersFactor;
                final double az = Double.parseDouble(tokens[9]) * pi.distanceUnit.conversionToMetersFactor;

                final Vector3D position = new Vector3D(px, py, pz);
                final Vector3D velocity = new Vector3D(vx, vy, vz);
                final Vector3D acceleration = new Vector3D(ax, ay, az);

                pi.addEphemeris(time, new PVCoordinates(position, velocity, acceleration));
            }

            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(END_EPHEMERIS, EPHEMERIS_TIME_POS_VEL_ACC_DATUM);
            }

        },

        /** END Ephemeris keyword. */
        END_EPHEMERIS("\\s*END Ephemeris\\s*(#.*)?") {

            @Override
            public void parse(final String line, final ParseInfo pi) {
                pi.complete();
            }

            @Override
            public Iterable<LineParser> allowedNext() {
                return Collections.emptyList();
            }

        };

        /** Pattern for identifying line. */
        private final Pattern pattern;

        /**
         * Constructs a {@link LineParser} instance.
         * @param regex regular expression for identifying line
         */
        LineParser(final String regex) {
            pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        }

        /**
         * Parses a line.
         * @param line line to parse
         * @param pi holder for transient data
         */
        public abstract void parse(String line, ParseInfo pi);

        /**
         * Returns the allowed parsers for the next line.
         * @return returns the allowed parsers for the next line
         */
        public abstract Iterable<LineParser> allowedNext();

        /**
         * Checks if a parser can handle line.
         * @param line line to parse
         * @return true if parser can handle the specified line
         */
        public boolean canHandle(final String line) {
            return pattern.matcher(line).matches();
        }

    }

    /** STK distance unit. */
    private enum STKDistanceUnit {

        /** Kilometers. */
        KILOMETERS(1000.0),

        /** Meters. */
        METERS(1.0);

        /** Factor by which to multiply to convert the distance unit to meters. */
        private final double conversionToMetersFactor;

        /**
         * Constructs a {@link STKDistanceUnit} instance.
         * @param conversionToMetersFactor factor by which to multiply to
         *        convert the distance unit to meters
         */
        STKDistanceUnit(final double conversionToMetersFactor) {
            this.conversionToMetersFactor = conversionToMetersFactor;
        }

    }

}

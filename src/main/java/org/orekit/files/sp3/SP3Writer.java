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
package org.orekit.files.sp3;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.utils.CartesianDerivativesFilter;

/** Writer for SP3 file.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class SP3Writer {

    /** End Of Line. */
    private static final String EOL = System.lineSeparator();

    /** Prefix for accuracy lines. */
    private static final String ACCURACY_LINE_PREFIX = "++       ";

    /** Prefix for comment lines. */
    private static final String COMMENT_LINE_PREFIX = "/* ";

    /** Format for accuracy base lines. */
    private static final String ACCURACY_BASE_FORMAT = "%%f %10.7f %12.9f %14.11f %18.15f%n";

    /** Constant additional parameters lines. */
    private static final String ADDITIONAL_PARAMETERS_LINE = "%i    0    0    0    0      0      0      0      0         0";

    /** Format for one 2 digits integer field. */
    private static final String TWO_DIGITS_INTEGER = "%2d";

    /** Format for one 3 digits integer field. */
    private static final String THREE_DIGITS_INTEGER = "%3d";

    /** Format for one 14.6 digits float field. */
    private static final String FOURTEEN_SIX_DIGITS_FLOAT = "%14.6f";

    /** Format for three blanks field. */
    private static final String THREE_BLANKS = "   ";

    /** Time system default line. */
    private static final String TIME_SYSTEM_DEFAULT = "%c cc cc ccc ccc cccc cccc cccc cccc ccccc ccccc ccccc ccccc";

    /** Destination of generated output. */
    private final Appendable output;

    /** Output name for error messages. */
    private final String outputName;

    /** Set of time scales used for parsing dates. */
    private final TimeScales timeScales;

    /** Simple constructor.
     * @param output destination of generated output
     * @param outputName output name for error messages
     * @param timeScales set of time scales used for parsing dates
     */
    public SP3Writer(final Appendable output, final String outputName, final TimeScales timeScales) {
        this.output     = output;
        this.outputName = outputName;
        this.timeScales = timeScales;
    }

    /** Write a SP3 file.
     * @param sp3 SP3 file to write
     * @exception IOException if an I/O error occurs.
     */
    public void write(final SP3 sp3)
        throws IOException {
        sp3.validate(false, outputName);
        writeHeader(sp3.getHeader());

        // set up iterators for all satellites
        final CoordinatesIterator[] iterators = new CoordinatesIterator[sp3.getSatelliteCount()];
        int k = 0;
        for (final Map.Entry<String, SP3Ephemeris> entry : sp3.getSatellites().entrySet()) {
            iterators[k++] = new CoordinatesIterator(entry.getValue());
        }

        final TimeScale timeScale  = sp3.getHeader().getTimeSystem().getTimeScale(timeScales);
        for (AbsoluteDate date = earliest(iterators); !date.equals(AbsoluteDate.FUTURE_INFINITY); date = earliest(iterators)) {

            // epoch
            final DateTimeComponents dtc = date.getComponents(timeScale);
            output.append(String.format(Locale.US, "*  %4d %2d %2d %2d %2d %11.8f%n",
                                        dtc.getDate().getYear(),
                                        dtc.getDate().getMonth(),
                                        dtc.getDate().getDay(),
                                        dtc.getTime().getHour(),
                                        dtc.getTime().getMinute(),
                                        dtc.getTime().getSecond()));

            for (final CoordinatesIterator iter : iterators) {

                final SP3Coordinate coordinate;
                if (iter.pending != null &&
                    FastMath.abs(iter.pending.getDate().durationFrom(date)) <= 0.001 * sp3.getHeader().getEpochInterval()) {
                    // the pending coordinate date matches current epoch
                    coordinate = iter.pending;
                    iter.advance();
                } else {
                    // the pending coordinate  does not match current epoch
                    coordinate = SP3Coordinate.DUMMY;
                }

                // position
                writePosition(sp3.getHeader(), iter.id, coordinate);

                if (sp3.getHeader().getFilter() != CartesianDerivativesFilter.USE_P) {
                    // velocity
                    writeVelocity(sp3.getHeader(), iter.id, coordinate);
                }

            }

        }

        output.append("EOF").
               append(EOL);

    }

    /** Find earliest date in ephemerides.
     * @param iterators ephemerides iterators
     * @return earliest date in iterators
     */
    private AbsoluteDate earliest(final CoordinatesIterator[] iterators) {
        AbsoluteDate date = AbsoluteDate.FUTURE_INFINITY;
        for (final CoordinatesIterator iter : iterators) {
            if (iter.pending != null && iter.pending.getDate().isBefore(date)) {
                date = iter.pending.getDate();
            }
        }
        return date;
    }

    /** Write position.
     * @param header file header
     * @param satId satellite id
     * @param coordinate coordinate
     * @exception IOException if an I/O error occurs.
     */
    private void writePosition(final SP3Header header, final String satId, final SP3Coordinate coordinate)
        throws IOException {

        final StringBuilder lineBuilder = new StringBuilder();

        // position
        lineBuilder.append(String.format(Locale.US, "P%3s%14.6f%14.6f%14.6f",
                                         satId,
                                         SP3Utils.POSITION_UNIT.fromSI(coordinate.getPosition().getX()),
                                         SP3Utils.POSITION_UNIT.fromSI(coordinate.getPosition().getY()),
                                         SP3Utils.POSITION_UNIT.fromSI(coordinate.getPosition().getZ())));

        // clock
        lineBuilder.append(String.format(Locale.US, FOURTEEN_SIX_DIGITS_FLOAT,
                                         SP3Utils.CLOCK_UNIT.fromSI(coordinate.getClockCorrection())));

        // position accuracy
        if (coordinate.getPositionAccuracy() == null) {
            lineBuilder.append(THREE_BLANKS).
                        append(THREE_BLANKS).
                        append(THREE_BLANKS);
        } else {
            lineBuilder.append(' ');
            lineBuilder.append(String.format(Locale.US, TWO_DIGITS_INTEGER,
                                             SP3Utils.indexAccuracy(SP3Utils.POSITION_ACCURACY_UNIT, header.getPosVelBase(),
                                                                    coordinate.getPositionAccuracy().getX())));
            lineBuilder.append(' ');
            lineBuilder.append(String.format(Locale.US, TWO_DIGITS_INTEGER,
                                             SP3Utils.indexAccuracy(SP3Utils.POSITION_ACCURACY_UNIT, header.getPosVelBase(),
                                                                    coordinate.getPositionAccuracy().getY())));
            lineBuilder.append(' ');
            lineBuilder.append(String.format(Locale.US, TWO_DIGITS_INTEGER,
                                             SP3Utils.indexAccuracy(SP3Utils.POSITION_ACCURACY_UNIT, header.getPosVelBase(),
                                                                    coordinate.getPositionAccuracy().getZ())));
        }

        // clock accuracy
        lineBuilder.append(' ');
        if (Double.isNaN(coordinate.getClockAccuracy())) {
            lineBuilder.append(THREE_BLANKS);
        } else {
            lineBuilder.append(String.format(Locale.US, THREE_DIGITS_INTEGER,
                                             SP3Utils.indexAccuracy(SP3Utils.CLOCK_ACCURACY_UNIT, header.getClockBase(),
                                                                    coordinate.getClockAccuracy())));
        }

        // events
        lineBuilder.append(' ');
        lineBuilder.append(coordinate.hasClockEvent()         ? 'E' : ' ');
        lineBuilder.append(coordinate.hasClockPrediction()    ? 'P' : ' ');
        lineBuilder.append(' ');
        lineBuilder.append(' ');
        lineBuilder.append(coordinate.hasOrbitManeuverEvent() ? 'M' : ' ');
        lineBuilder.append(coordinate.hasOrbitPrediction()    ? 'P' : ' ');

        output.append(lineBuilder.toString().trim()).append(EOL);

    }

    /** Write velocity.
     * @param header file header
     * @param satId satellite id
     * @param coordinate coordinate
     * @exception IOException if an I/O error occurs.
     */
    private void writeVelocity(final SP3Header header, final String satId, final SP3Coordinate coordinate)
        throws IOException {

        final StringBuilder lineBuilder = new StringBuilder();
         // velocity
        lineBuilder.append(String.format(Locale.US, "V%3s%14.6f%14.6f%14.6f",
                                         satId,
                                         SP3Utils.VELOCITY_UNIT.fromSI(coordinate.getVelocity().getX()),
                                         SP3Utils.VELOCITY_UNIT.fromSI(coordinate.getVelocity().getY()),
                                         SP3Utils.VELOCITY_UNIT.fromSI(coordinate.getVelocity().getZ())));

        // clock rate
        lineBuilder.append(String.format(Locale.US, FOURTEEN_SIX_DIGITS_FLOAT,
                                         SP3Utils.CLOCK_RATE_UNIT.fromSI(coordinate.getClockRateChange())));

        // velocity accuracy
        if (coordinate.getVelocityAccuracy() == null) {
            lineBuilder.append(THREE_BLANKS).
                        append(THREE_BLANKS).
                        append(THREE_BLANKS);
        } else {
            lineBuilder.append(' ');
            lineBuilder.append(String.format(Locale.US, TWO_DIGITS_INTEGER,
                                             SP3Utils.indexAccuracy(SP3Utils.VELOCITY_ACCURACY_UNIT, header.getPosVelBase(),
                                                                    coordinate.getVelocityAccuracy().getX())));
            lineBuilder.append(' ');
            lineBuilder.append(String.format(Locale.US, TWO_DIGITS_INTEGER,
                                             SP3Utils.indexAccuracy(SP3Utils.VELOCITY_ACCURACY_UNIT, header.getPosVelBase(),
                                                                    coordinate.getVelocityAccuracy().getY())));
            lineBuilder.append(' ');
            lineBuilder.append(String.format(Locale.US, TWO_DIGITS_INTEGER,
                                             SP3Utils.indexAccuracy(SP3Utils.VELOCITY_ACCURACY_UNIT, header.getPosVelBase(),
                                                                    coordinate.getVelocityAccuracy().getZ())));
        }

        // clock rate accuracy
        lineBuilder.append(' ');
        if (Double.isNaN(coordinate.getClockRateAccuracy())) {
            lineBuilder.append(THREE_BLANKS);
        } else {
            lineBuilder.append(String.format(Locale.US, THREE_DIGITS_INTEGER,
                                             SP3Utils.indexAccuracy(SP3Utils.CLOCK_RATE_ACCURACY_UNIT, header.getClockBase(),
                                                                    coordinate.getClockRateAccuracy())));
        }

        output.append(lineBuilder.toString().trim()).append(EOL);

    }

    /** Write header.
     * @param header SP3 header to write
     * @exception IOException if an I/O error occurs.
     */
    private void writeHeader(final SP3Header header)
        throws IOException {
        final TimeScale timeScale = header.getTimeSystem().getTimeScale(timeScales);
        final DateTimeComponents dtc = header.getEpoch().getComponents(timeScale);
        final StringBuilder dataUsedBuilder = new StringBuilder();
        for (final DataUsed du : header.getDataUsed()) {
            if (dataUsedBuilder.length() > 0) {
                dataUsedBuilder.append('+');
            }
            dataUsedBuilder.append(du.getKey());
        }
        final String dataUsed = dataUsedBuilder.length() <= 5 ?
                                dataUsedBuilder.toString() :
                                DataUsed.MIXED.getKey();

        // header first line: version, epoch...
        output.append(String.format(Locale.US, "#%c%c%4d %2d %2d %2d %2d %11.8f %7d %5s %5s %3s %4s%n",
                                    header.getVersion(),
                                    header.getFilter() == CartesianDerivativesFilter.USE_P ? 'P' : 'V',
                                    dtc.getDate().getYear(),
                                    dtc.getDate().getMonth(),
                                    dtc.getDate().getDay(),
                                    dtc.getTime().getHour(),
                                    dtc.getTime().getMinute(),
                                    dtc.getTime().getSecond(),
                                    header.getNumberOfEpochs(),
                                    dataUsed,
                                    header.getCoordinateSystem(),
                                    header.getOrbitTypeKey(),
                                    header.getAgency()));

        // header second line : dates
        output.append(String.format(Locale.US, "## %4d %15.8f %14.8f %5d %15.13f%n",
                                    header.getGpsWeek(),
                                    header.getSecondsOfWeek(),
                                    header.getEpochInterval(),
                                    header.getModifiedJulianDay(),
                                    header.getDayFraction()));

        // list of satellites
        final List<String> satellites = header.getSatIds();
        output.append(String.format(Locale.US, "+  %3d   ", satellites.size()));
        int lines  = 0;
        int column = 9;
        int remaining = satellites.size();
        for (final String satId : satellites) {
            output.append(String.format(Locale.US, "%3s", satId));
            --remaining;
            column += 3;
            if (column >= 60) {
                // finish line
                output.append(EOL);
                ++lines;
                if (remaining > 0) {
                    // start new line
                    output.append("+        ");
                    column = 9;
                }
            }
        }
        while (column < 60) {
            output.append(' ').
                   append(' ').
                   append('0');
            column += 3;
        }
        output.append(EOL);
        ++lines;
        while (lines++ < 5) {
            // write extra lines to have at least 85 satellites
            output.append("+          0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0").
                   append(EOL);
        }

        // general accuracy
        output.append(ACCURACY_LINE_PREFIX);
        lines  = 0;
        column = 9;
        remaining = satellites.size();
        for (final String satId : satellites) {
            final double accuracy    = header.getAccuracy(satId);
            final int    accuracyExp = SP3Utils.indexAccuracy(SP3Utils.POSITION_ACCURACY_UNIT, SP3Utils.POS_VEL_BASE_ACCURACY, accuracy);
            output.append(String.format(Locale.US, THREE_DIGITS_INTEGER, accuracyExp));
            --remaining;
            column += 3;
            if (column >= 60) {
                // finish line
                output.append(EOL);
                ++lines;
                if (remaining > 0) {
                    // start new line
                    output.append(ACCURACY_LINE_PREFIX);
                    column = 9;
                }
            }
        }
        while (column < 60) {
            output.append(' ').
                   append(' ').
                   append('0');
            column += 3;
        }
        output.append(EOL);
        ++lines;
        while (lines++ < 5) {
            // write extra lines to have at least 85 satellites
            output.append("++         0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0").
                   append(EOL);
        }

        // type
        if (header.getVersion() == 'a') {
            output.append(TIME_SYSTEM_DEFAULT).append(EOL);
        } else {
            output.append(String.format(Locale.US, "%%c %1s  cc %3s ccc cccc cccc cccc cccc ccccc ccccc ccccc ccccc%n",
                                        header.getType().getKey(),
                                        header.getTimeSystem().getKey()));
        }
        output.append(TIME_SYSTEM_DEFAULT).append(EOL);

        // entries accuracy
        output.append(String.format(Locale.US, ACCURACY_BASE_FORMAT,
                                    header.getPosVelBase(), header.getClockBase(), 0.0, 0.0));
        output.append(String.format(Locale.US, ACCURACY_BASE_FORMAT,
                                    0.0, 0.0, 0.0, 0.0));

        // additional parameters
        output.append(ADDITIONAL_PARAMETERS_LINE).append(EOL);
        output.append(ADDITIONAL_PARAMETERS_LINE).append(EOL);

        // comments
        int count = 0;
        for (final String comment : header.getComments()) {
            ++count;
            output.append(COMMENT_LINE_PREFIX).append(comment).append(EOL);
        }
        while (count < 4) {
            // add dummy comments to get at least the four comments specified for versions a, b and c
            ++count;
            output.append(COMMENT_LINE_PREFIX).append(EOL);
        }

    }

    /** Iterator for coordinates. */
    private static class CoordinatesIterator {

        /** Satellite ID. */
        private final String id;

        /** Iterator over segments. */
        private Iterator<SP3Segment> segmentsIterator;

        /** Iterator over coordinates. */
        private Iterator<SP3Coordinate> coordinatesIterator;

        /** Pending coordinate. */
        private SP3Coordinate pending;

        /** Simple constructor.
         * @param ephemeris underlying ephemeris
         */
        CoordinatesIterator(final SP3Ephemeris ephemeris) {
            this.id                  = ephemeris.getId();
            this.segmentsIterator    = ephemeris.getSegments().iterator();
            this.coordinatesIterator = null;
            advance();
        }

        /** Advance to next coordinates.
         */
        private void advance() {

            while (coordinatesIterator == null || !coordinatesIterator.hasNext()) {
                // we have exhausted previous segment
                if (segmentsIterator != null && segmentsIterator.hasNext()) {
                    coordinatesIterator = segmentsIterator.next().getCoordinates().iterator();
                } else {
                    // we have exhausted the ephemeris
                    segmentsIterator = null;
                    pending          = null;
                    return;
                }
            }

            // retrieve the next entry
            pending = coordinatesIterator.next();

        }

    }

}

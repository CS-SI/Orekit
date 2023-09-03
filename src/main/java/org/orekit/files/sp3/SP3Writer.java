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
import java.util.Locale;
import java.util.Map;

import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.files.sp3.SP3.SP3Ephemeris;
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
    private static final String EOL = "%n";

    /** Prefix for accuracy lines. */
    private static final String ACCURACY_LINE_PREFIX = "++       ";

    /** Prefix for comment lines. */
    private static final String COMMENT_LINE_PREFIX = "/* ";

    /** Format for accuracy base lines. */
    private static final String ACCURACY_BASE_FORMAT = "%%f %10.7f %12.9f %14.11f %18.15f%n";

    /** Constant additional parameters lines. */
    private static final String ADDITIONAL_PARAMETERS_LINE = "%i    0    0    0    0      0      0      0      0         0";

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
    @DefaultDataContext
    public void write(final SP3 sp3)
        throws IOException {
        sp3.validate(false, outputName);
        writeHeader(sp3);

        final int[]          nextIndex   = new int[sp3.getSatelliteCount()];
        final SP3Ephemeris[] ephemerides = new SP3Ephemeris[sp3.getSatelliteCount()];
        int i = 0;
        for (final Map.Entry<String, SP3Ephemeris> entry : sp3.getSatellites().entrySet()) {
            ephemerides[i++] = entry.getValue();
        }

        // TODO: write orbit data

    }

    /** Write header.
     * @param sp3 SP3 file containing the header to write
     * @exception IOException if an I/O error occurs.
     */
    private void writeHeader(final SP3 sp3)
        throws IOException {
        final TimeScale timeScale = sp3.getTimeSystem().getTimeScale(timeScales);
        final DateTimeComponents dtc = sp3.getEpoch().getComponents(timeScale);
        final StringBuilder dataUsedBuilder = new StringBuilder();
        for (final DataUsed du : sp3.getDataUsed()) {
            if (dataUsedBuilder.length() > 0) {
                dataUsedBuilder.append('+');
            }
            dataUsedBuilder.append(du.getKey());
        }
        final String dataUsed = dataUsedBuilder.length() <= 5 ?
                                dataUsedBuilder.toString() :
                                DataUsed.MIXED.getKey();

        // header first line: version, epoch...
        output.append(String.format(Locale.US, "#%c%c%4d %02d %02d %02d %02d %11.8f %7d %5s %5s %3s %4s%n",
                                    sp3.getVersion(),
                                    sp3.getFilter() == CartesianDerivativesFilter.USE_P ? 'P' : 'V',
                                    dtc.getDate().getYear(),
                                    dtc.getDate().getMonth(),
                                    dtc.getDate().getDay(),
                                    dtc.getTime().getHour(),
                                    dtc.getTime().getMinute(),
                                    dtc.getTime().getSecondsInLocalDay(),
                                    sp3.getNumberOfEpochs(),
                                    dataUsed,
                                    sp3.getCoordinateSystem(),
                                    sp3.getOrbitTypeKey(),
                                    sp3.getAgency()));

        // header second line : dates
        output.append(String.format(Locale.US, "## %4d %15.8f %14.8f %5d %15.13%n",
                                    sp3.getGpsWeek(),
                                    sp3.getSecondsOfWeek(),
                                    sp3.getEpochInterval(),
                                    sp3.getModifiedJulianDay(),
                                    sp3.getDayFraction()));

        // list of satellites
        output.append(String.format(Locale.US, "+   %2d   ", sp3.getSatelliteCount()));
        int column = 9;
        int remaining = sp3.getSatelliteCount();
        for (final Map.Entry<String, SP3Ephemeris> entry : sp3.getSatellites().entrySet()) {
            output.append(String.format(Locale.US, "%3s", entry.getKey()));
            --remaining;
            column += 3;
            if (column >= 60) {
                // finish line
                output.append(EOL);
                if (remaining > 0) {
                    // start new line
                    output.append("+        ");
                    column = 9;
                }
            }
        }
        while (column < 60) {
            output.append(' ');
        }
        output.append(EOL);

        // general accuracy
        output.append(ACCURACY_LINE_PREFIX);
        column = 9;
        remaining = sp3.getSatelliteCount();
        for (final Map.Entry<String, SP3Ephemeris> entry : sp3.getSatellites().entrySet()) {
            final double accuracy = entry.getValue().getAccuracy();
            final int    accuracyExp = (int) FastMath.ceil(FastMath.log(SP3Constants.POSITION_ACCURACY_UNIT.fromSI(accuracy)) /
                                                           FastMath.log(2.0));
            output.append(String.format(Locale.US, "%3d", accuracyExp));
            --remaining;
            column += 3;
            if (column >= 60) {
                // finish line
                output.append(EOL);
                if (remaining > 0) {
                    // start new line
                    output.append(ACCURACY_LINE_PREFIX);
                    column = 9;
                }
            }
        }
        while (column < 60) {
            output.append(' ');
        }
        output.append(EOL);

        // type
        output.append(String.format(Locale.US, "%%c %1s  cc %3s ccc cccc cccc cccc cccc ccccc ccccc ccccc ccccc%n",
                                    sp3.getType().getKey(),
                                    sp3.getTimeSystem().getKey()));
        output.append("%c cc cc ccc ccc cccc cccc cccc cccc ccccc ccccc ccccc ccccc").append(EOL);

        // entries accuracy
        output.append(String.format(Locale.US, ACCURACY_BASE_FORMAT,
                                    sp3.getPosVelBase(), sp3.getClass(), 0.0, 0.0));
        output.append(String.format(Locale.US, ACCURACY_BASE_FORMAT,
                                    0.0, 0.0, 0.0, 0.0));

        // additional parameters
        output.append(ADDITIONAL_PARAMETERS_LINE).append(EOL);
        output.append(ADDITIONAL_PARAMETERS_LINE).append(EOL);

        // comments
        int count = 0;
        for (final String comment : sp3.getComments()) {
            ++count;
            output.append(COMMENT_LINE_PREFIX).append(comment).append(EOL);
        }
        while (count < 4) {
            // add dummy comments to get at least the four comments specified for versions a, b and c
            ++count;
            output.append(COMMENT_LINE_PREFIX).append(EOL);
        }

    }

}

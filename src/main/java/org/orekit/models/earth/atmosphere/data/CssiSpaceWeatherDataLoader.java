/* Copyright 2020 Clément Jonglez
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Clément Jonglez licenses this file to You under the Apache License, Version 2.0
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

package org.orekit.models.earth.atmosphere.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hipparchus.exception.Localizable;
import org.orekit.data.DataLoader;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeStamped;

/**
 * This class reads solar activity data from CSSI Space Weather files for the
 * class {@link CssiSpaceWeatherData}.
 * <p>
 * The data are retrieved through space weather files offered by CSSI/AGI. The
 * data can be retrieved on the AGI
 * <a href="ftp://ftp.agi.com/pub/DynamicEarthData/SpaceWeather-All-v1.2.txt">
 * FTP</a>. This file is updated several times a day by using several sources
 * mentioned in the <a href="http://celestrak.com/SpaceData/SpaceWx-format.php">
 * Celestrak space weather data documentation</a>.
 * </p>
 *
 * @author Clément Jonglez
 * @since 10.2
 */
public class CssiSpaceWeatherDataLoader implements DataLoader {

    /** Helper class to parse line data and to raise exceptions if needed. */
    public static class LineReader {

        /** Name of the file. Used in error messages. */
        private final String name;

        /** The input stream. */
        private final BufferedReader in;

        /** The last line read from the file. */
        private String line;

        /** The number of the last line read from the file. */
        private long lineNo;

        /**
         * Create a line reader.
         *
         * @param name of the data source for error messages.
         * @param in   the input data stream.
         */
        public LineReader(final String name, final BufferedReader in) {
            this.name = name;
            this.in = in;
            this.line = null;
            this.lineNo = 0;
        }

        /**
         * Read a line from the input data stream.
         *
         * @return the next line without the line termination character, or {@code null}
         *         if the end of the stream has been reached.
         * @throws IOException if an I/O error occurs.
         * @see BufferedReader#readLine()
         */
        public String readLine() throws IOException {
            line = in.readLine();
            lineNo++;
            return line;
        }

        /**
         * Read a line from the input data stream, or if the end of the stream has been
         * reached throw an exception.
         *
         * @param message for the exception if the end of the stream is reached.
         * @param args    for the exception if the end of stream is reached.
         * @return the next line without the line termination character, or {@code null}
         *         if the end of the stream has been reached.
         * @throws IOException     if an I/O error occurs.
         * @throws OrekitException if a line could not be read because the end of the
         *                         stream has been reached.
         * @see #readLine()
         */
        public String readLineOrThrow(final Localizable message, final Object... args)
                throws IOException, OrekitException {

            final String text = readLine();
            if (text == null) {
                throw new OrekitException(message, args);
            }
            return text;
        }

        /**
         * Annotate an exception with the file context.
         *
         * @param cause the reason why the line could not be parsed.
         * @return an exception with the cause, file name, line number, and line text.
         */
        public OrekitException unableToParseLine(final Throwable cause) {
            return new OrekitException(cause, OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, lineNo, name, line);
        }

        /**
         * Get the last line read from the stream.
         *
         * @return May be {@code null} if no lines have been read or the end of stream
         *         has been reached.
         */
        public String getLine() {
            return line;
        }

        /**
         * Get the line number of the last line read from the file.
         *
         * @return the line number.
         */
        public long getLineNumber() {
            return lineNo;
        }

    }

    /** Container class for Solar activity indexes. */
    public static class LineParameters implements TimeStamped, Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 8151260459653484163L;

        /** Entry date. */
        private final AbsoluteDate date;

        /** Array of 8 three-hourly Kp indices for this entry. */
        private final double[] threeHourlyKp;

        /**
         * Sum of the 8 Kp indices for the day expressed to the nearest third of a unit.
         */
        private final double kpSum;

        /** Array of 8 three-hourly Ap indices for this entry. */
        private final double[] threeHourlyAp;

        /** Arithmetic average of the 8 Ap indices for the day. */
        private final double apAvg;

        /** 10.7-cm Solar Radio Flux (F10.7) Adjusted to 1 AU. */
        private final double f107Adj;

        /** Flux Qualifier. */
        private final int fluxQualifier;

        /** Centered 81-day arithmetic average of F10.7 (adjusted). */
        private final double ctr81Adj;

        /** Last 81-day arithmetic average of F10.7 (adjusted). */
        private final double lst81Adj;

        /** Observed (unadjusted) value of F10.7. */
        private final double f107Obs;

        /** Centered 81-day arithmetic average of F10.7 (observed). */
        private final double ctr81Obs;

        /** Last 81-day arithmetic average of F10.7 (observed). */
        private final double lst81Obs;

        /**
         * Constructor.
         * @param date entry date
         * @param threeHourlyKp array of 8 three-hourly Kp indices for this entry
         * @param kpSum sum of the 8 Kp indices for the day expressed to the nearest third of a unit
         * @param threeHourlyAp array of 8 three-hourly Ap indices for this entry
         * @param apAvg arithmetic average of the 8 Ap indices for the day
         * @param f107Adj 10.7-cm Solar Radio Flux (F10.7)
         * @param fluxQualifier flux Qualifier
         * @param ctr81Adj centered 81-day arithmetic average of F10.7
         * @param lst81Adj last 81-day arithmetic average of F10.7
         * @param f107Obs observed (unadjusted) value of F10.7
         * @param ctr81Obs centered 81-day arithmetic average of F10.7 (observed)
         * @param lst81Obs last 81-day arithmetic average of F10.7 (observed)
         */
        public LineParameters(final AbsoluteDate date, final double[] threeHourlyKp, final double kpSum,
                final double[] threeHourlyAp, final double apAvg, final double f107Adj, final int fluxQualifier,
                final double ctr81Adj, final double lst81Adj, final double f107Obs, final double ctr81Obs,
                final double lst81Obs) {
            this.date = date;
            this.threeHourlyKp = threeHourlyKp.clone();
            this.kpSum = kpSum;
            this.threeHourlyAp = threeHourlyAp.clone();
            this.apAvg = apAvg;
            this.f107Adj = f107Adj;
            this.fluxQualifier = fluxQualifier;
            this.ctr81Adj = ctr81Adj;
            this.lst81Adj = lst81Adj;
            this.f107Obs = f107Obs;
            this.ctr81Obs = ctr81Obs;
            this.lst81Obs = lst81Obs;
        }

        @Override
        public AbsoluteDate getDate() {
            return date;
        }

        /**
         * Gets the array of the eight three-hourly Kp indices for the current entry.
         * @return the array of eight three-hourly Kp indices
         */
        public double[] getThreeHourlyKp() {
            return threeHourlyKp.clone();
        }

        /**
         * Gets the three-hourly Kp index at index i from the threeHourlyKp array.
         * @param i index of the Kp index to retrieve [0-7]
         * @return the three hourly Kp index at index i
         */
        public double getThreeHourlyKp(final int i) {
            return threeHourlyKp[i];
        }

        /**
         * Gets the sum of all eight Kp indices for the current entry.
         * @return the sum of all eight Kp indices
         */
        public double getKpSum() {
            return kpSum;
        }

        /**
         * Gets the array of the eight three-hourly Ap indices for the current entry.
         * @return the array of eight three-hourly Ap indices
         */
        public double[] getThreeHourlyAp() {
            return threeHourlyAp.clone();
        }

        /**
         * Gets the three-hourly Ap index at index i from the threeHourlyAp array.
         * @param i index of the Ap to retrieve [0-7]
         * @return the three hourly Ap index at index i
         */
        public double getThreeHourlyAp(final int i) {
            return threeHourlyAp[i];
        }

        /**
         * Gets the arithmetic average of all eight Ap indices for the current entry.
         * @return the average of all eight Ap indices
         */
        public double getApAvg() {
            return apAvg;
        }

        /**
         * Gets the last 81-day arithmetic average of F10.7 (observed).
         * @return the last 81-day arithmetic average of F10.7 (observed)
         */
        public double getLst81Obs() {
            return lst81Obs;
        }

        /**
         * Gets the centered 81-day arithmetic average of F10.7 (observed).
         * @return the centered 81-day arithmetic average of F10.7 (observed)
         */
        public double getCtr81Obs() {
            return ctr81Obs;
        }

        /**
         * Gets the observed (unadjusted) value of F10.7.
         * @return the observed (unadjusted) value of F10.7
         */
        public double getF107Obs() {
            return f107Obs;
        }

        /**
         * Gets the last 81-day arithmetic average of F10.7 (adjusted).
         * @return the last 81-day arithmetic average of F10.7 (adjusted)
         */
        public double getLst81Adj() {
            return lst81Adj;
        }

        /**
         * Gets the centered 81-day arithmetic average of F10.7 (adjusted).
         * @return the centered 81-day arithmetic average of F10.7 (adjusted)
         */
        public double getCtr81Adj() {
            return ctr81Adj;
        }

        /**
         * Gets the Flux Qualifier.
         * @return the Flux Qualifier
         */
        public int getFluxQualifier() {
            return fluxQualifier;
        }

        /**
         * Gets the 10.7-cm Solar Radio Flux (F10.7) Adjusted to 1 AU.
         * @return the 10.7-cm Solar Radio Flux (F10.7) Adjusted to 1 AU
         */
        public double getF107Adj() {
            return f107Adj;
        }
    }

    /** UTC time scale. */
    private final TimeScale utc;

    /** First available date. */
    private AbsoluteDate firstDate;

    /** Date of last data before the prediction starts. */
    private AbsoluteDate lastObservedDate;

    /** Date of last daily prediction before the monthly prediction starts. */
    private AbsoluteDate lastDailyPredictedDate;

    /** Last available date. */
    private AbsoluteDate lastDate;

    /** Data set. */
    private SortedSet<TimeStamped> set;

    /**
     * Constructor.
     * @param utc UTC time scale
     */
    public CssiSpaceWeatherDataLoader(final TimeScale utc) {
        this.utc = utc;
        firstDate = null;
        lastDailyPredictedDate = null;
        lastDate = null;
        lastObservedDate = null;
        set = new TreeSet<>(new ChronologicalComparator());
    }

    /**
     * Getter for the data set.
     * @return the data set
     */
    public SortedSet<TimeStamped> getDataSet() {
        return set;
    }

    /**
     * Gets the available data range minimum date.
     * @return the minimum date.
     */
    public AbsoluteDate getMinDate() {
        return firstDate;
    }

    /**
     * Gets the available data range maximum date.
     * @return the maximum date.
     */
    public AbsoluteDate getMaxDate() {
        return lastDate;
    }

    /**
     * Gets the day (at data start) of the last daily data entry.
     * @return the last daily predicted date
     */
    public AbsoluteDate getLastDailyPredictedDate() {
        return lastDailyPredictedDate;
    }

    /**
     * Gets the day (at data start) of the last observed data entry.
     * @return the last observed date
     */
    public AbsoluteDate getLastObservedDate() {
        return lastObservedDate;
    }

    /**
     * Checks if the string contains a floating point number.
     *
     * @param strNum string to check
     * @return true if string contains a valid floating point number, else false
     */
    private static boolean isNumeric(final String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
            throws IOException, ParseException, OrekitException {

        // read the data
        int lineNumber = 0;
        String line = null;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

            final LineReader reader = new LineReader(name, br);

            for (line = reader.readLine(); line != null; line = reader.readLine()) {
                lineNumber++;

                line = line.trim();
                if (line.length() > 0) {

                    if (line.equals("BEGIN DAILY_PREDICTED")) {
                        lastObservedDate = set.last().getDate();
                    }

                    if (line.equals("BEGIN MONTHLY_FIT")) {
                        lastDailyPredictedDate = set.last().getDate();
                    }

                    if (line.length() == 130 && isNumeric(line.substring(0, 4))) {
                        // extract the data from the line
                        final int year = Integer.parseInt(line.substring(0, 4));
                        final int month = Integer.parseInt(line.substring(5, 7));
                        final int day = Integer.parseInt(line.substring(8, 10));
                        final AbsoluteDate date = new AbsoluteDate(year, month, day, this.utc);

                        if (!set.contains(date)) { // Checking if entry doesn't exist yet
                            final double[] threeHourlyKp = new double[8];
                            /**
                             * Kp is written as an integer where a unit equals 0.1, the conversion is
                             * Kp_double = 0.1 * double(Kp_integer)
                             */
                            for (int i = 0; i < 8; i++) {
                                threeHourlyKp[i] = 0.1 * Double.parseDouble(line.substring(19 + 3 * i, 21 + 3 * i));
                            }
                            final double kpSum = 0.1 * Double.parseDouble(line.substring(43, 46));

                            final double[] threeHourlyAp = new double[8];
                            for (int i = 0; i < 8; i++) {
                                threeHourlyAp[i] = Double.parseDouble(line.substring(47 + 4 * i, 50 + 4 * i));
                            }
                            final double apAvg = Double.parseDouble(line.substring(79, 82));

                            final double f107Adj = Double.parseDouble(line.substring(93, 98));

                            final int fluxQualifier = Integer.parseInt(line.substring(99, 100));

                            final double ctr81Adj = Double.parseDouble(line.substring(101, 106));

                            final double lst81Adj = Double.parseDouble(line.substring(107, 112));

                            final double f107Obs = Double.parseDouble(line.substring(113, 118));

                            final double ctr81Obs = Double.parseDouble(line.substring(119, 124));

                            final double lst81Obs = Double.parseDouble(line.substring(125, 130));

                            set.add(new LineParameters(date, threeHourlyKp, kpSum, threeHourlyAp, apAvg, f107Adj,
                                    fluxQualifier, ctr81Adj, lst81Adj, f107Obs, ctr81Obs, lst81Obs));
                        }
                    }
                }
            }
        } catch (NumberFormatException nfe) {
            throw new OrekitException(nfe, OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, lineNumber, name, line);
        }

        try {
            firstDate = set.first().getDate();
            lastDate = set.last().getDate();
        } catch (NoSuchElementException nse) {
            throw new OrekitException(nse, OrekitMessages.NO_DATA_IN_FILE, name);
        }

    }

    /** {@inheritDoc} */
    public boolean stillAcceptsData() {
        return true;
    }
}

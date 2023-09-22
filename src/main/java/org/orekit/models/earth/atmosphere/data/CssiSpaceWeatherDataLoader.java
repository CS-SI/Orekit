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

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeScale;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class reads solar activity data from CSSI Space Weather files for the class {@link CssiSpaceWeatherData}.
 * <p>
 * The data are retrieved through space weather files offered by CSSI/AGI. The data can be retrieved on the AGI
 * <a href="ftp://ftp.agi.com/pub/DynamicEarthData/SpaceWeather-All-v1.2.txt">
 * FTP</a>. This file is updated several times a day by using several sources mentioned in the <a
 * href="http://celestrak.com/SpaceData/SpaceWx-format.php"> Celestrak space weather data documentation</a>.
 * </p>
 *
 * @author Clément Jonglez
 * @since 10.2
 */
public class CssiSpaceWeatherDataLoader extends AbstractSolarActivityDataLoader<CssiSpaceWeatherDataLoader.LineParameters> {

    /** Date of last data before the prediction starts. */
    private AbsoluteDate lastObservedDate;

    /** Date of last daily prediction before the monthly prediction starts. */
    private AbsoluteDate lastDailyPredictedDate;

    /** Data set. */
    private final SortedSet<LineParameters> set;

    /**
     * Constructor.
     *
     * @param utc UTC time scale
     */
    public CssiSpaceWeatherDataLoader(final TimeScale utc) {
        super(utc);
        this.lastDailyPredictedDate = null;
        this.lastObservedDate       = null;
        this.set                    = new TreeSet<>(new ChronologicalComparator());
    }

    /**
     * Checks if the string contains a floating point number.
     *
     * @param strNum string to check
     *
     * @return true if string contains a valid floating point number, else false
     */
    private static boolean isNumeric(final String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            Double.parseDouble(strNum);
        }
        catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name) throws IOException, ParseException, OrekitException {

        int                     lineNumber   = 0;
        String                  line         = null;
        final Set<AbsoluteDate> parsedEpochs = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

            final CommonLineReader reader = new CommonLineReader(br);

            for (line = reader.readLine(); line != null; line = reader.readLine()) {
                lineNumber++;

                line = line.trim();
                if (!line.isEmpty()) {

                    if (line.equals("BEGIN DAILY_PREDICTED")) {
                        lastObservedDate = set.last().getDate();
                    }

                    if (line.equals("BEGIN MONTHLY_FIT")) {
                        lastDailyPredictedDate = set.last().getDate();
                    }

                    if (line.length() == 130 && isNumeric(line.substring(0, 4))) {
                        // extract the data from the line
                        final int          year  = Integer.parseInt(line.substring(0, 4));
                        final int          month = Integer.parseInt(line.substring(5, 7));
                        final int          day   = Integer.parseInt(line.substring(8, 10));
                        final AbsoluteDate date  = new AbsoluteDate(year, month, day, getUTC());

                        if (parsedEpochs.add(date)) { // Checking if entry doesn't exist yet
                            final double[] threeHourlyKp = new double[8];
                            /* Kp is written as an integer where a unit equals 0.1, the conversion is
                             * Kp_double = 0.1 * double(Kp_integer) */
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
        }
        catch (NumberFormatException nfe) {
            throw new OrekitException(nfe, OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, lineNumber, name, line);
        }

        try {
            setMinDate(set.first().getDate());
            setMaxDate(set.last().getDate());
        }
        catch (NoSuchElementException nse) {
            throw new OrekitException(nse, OrekitMessages.NO_DATA_IN_FILE, name);
        }

    }

    /**
     * Getter for the data set.
     *
     * @return the data set
     */
    @Override
    public SortedSet<LineParameters> getDataSet() {
        return set;
    }

    /**
     * Gets the day (at data start) of the last daily data entry.
     *
     * @return the last daily predicted date
     */
    public AbsoluteDate getLastDailyPredictedDate() {
        return lastDailyPredictedDate;
    }

    /**
     * Gets the day (at data start) of the last observed data entry.
     *
     * @return the last observed date
     */
    public AbsoluteDate getLastObservedDate() {
        return lastObservedDate;
    }

    /** Container class for Solar activity indexes. */
    public static class LineParameters extends AbstractSolarActivityDataLoader.LineParameters {

        /** Serializable UID. */
        private static final long serialVersionUID = 8151260459653484163L;

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
         *
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
                              final double[] threeHourlyAp, final double apAvg, final double f107Adj,
                              final int fluxQualifier, final double ctr81Adj, final double lst81Adj,
                              final double f107Obs, final double ctr81Obs, final double lst81Obs) {
            super(date);
            this.threeHourlyKp = threeHourlyKp.clone();
            this.kpSum         = kpSum;
            this.threeHourlyAp = threeHourlyAp.clone();
            this.apAvg         = apAvg;
            this.f107Adj       = f107Adj;
            this.fluxQualifier = fluxQualifier;
            this.ctr81Adj      = ctr81Adj;
            this.lst81Adj      = lst81Adj;
            this.f107Obs       = f107Obs;
            this.ctr81Obs      = ctr81Obs;
            this.lst81Obs      = lst81Obs;
        }

        /** {@inheritDoc} */
        @Override
        public int compareTo(final AbstractSolarActivityDataLoader.LineParameters lineParameters) {
            return getDate().compareTo(lineParameters.getDate());
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final LineParameters that = (LineParameters) o;

            if (Double.compare(getKpSum(), that.getKpSum()) != 0) {
                return false;
            }
            if (Double.compare(getApAvg(), that.getApAvg()) != 0) {
                return false;
            }
            if (Double.compare(getF107Adj(), that.getF107Adj()) != 0) {
                return false;
            }
            if (getFluxQualifier() != that.getFluxQualifier()) {
                return false;
            }
            if (Double.compare(getCtr81Adj(), that.getCtr81Adj()) != 0) {
                return false;
            }
            if (Double.compare(getLst81Adj(), that.getLst81Adj()) != 0) {
                return false;
            }
            if (Double.compare(getF107Obs(), that.getF107Obs()) != 0) {
                return false;
            }
            if (Double.compare(getCtr81Obs(), that.getCtr81Obs()) != 0) {
                return false;
            }
            if (Double.compare(getLst81Obs(), that.getLst81Obs()) != 0) {
                return false;
            }
            if (!Arrays.equals(getThreeHourlyKp(), that.getThreeHourlyKp())) {
                return false;
            }
            return Arrays.equals(getThreeHourlyAp(), that.getThreeHourlyAp());
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            int  result;
            long temp;
            result = Arrays.hashCode(getThreeHourlyKp());
            temp   = Double.doubleToLongBits(getKpSum());
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            result = 31 * result + Arrays.hashCode(getThreeHourlyAp());
            temp   = Double.doubleToLongBits(getApAvg());
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp   = Double.doubleToLongBits(getF107Adj());
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            result = 31 * result + getFluxQualifier();
            temp   = Double.doubleToLongBits(getCtr81Adj());
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp   = Double.doubleToLongBits(getLst81Adj());
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp   = Double.doubleToLongBits(getF107Obs());
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp   = Double.doubleToLongBits(getCtr81Obs());
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp   = Double.doubleToLongBits(getLst81Obs());
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        /**
         * Gets the three-hourly Kp index at index i from the threeHourlyKp array.
         *
         * @param i index of the Kp index to retrieve [0-7]
         *
         * @return the three hourly Kp index at index i
         */
        public double getThreeHourlyKp(final int i) {
            return threeHourlyKp[i];
        }

        /**
         * Gets the three-hourly Ap index at index i from the threeHourlyAp array.
         *
         * @param i index of the Ap to retrieve [0-7]
         *
         * @return the three hourly Ap index at index i
         */
        public double getThreeHourlyAp(final int i) {
            return threeHourlyAp[i];
        }

        /**
         * Gets the array of the eight three-hourly Kp indices for the current entry.
         *
         * @return the array of eight three-hourly Kp indices
         */
        public double[] getThreeHourlyKp() {
            return threeHourlyKp.clone();
        }

        /**
         * Gets the sum of all eight Kp indices for the current entry.
         *
         * @return the sum of all eight Kp indices
         */
        public double getKpSum() {
            return kpSum;
        }

        /**
         * Gets the array of the eight three-hourly Ap indices for the current entry.
         *
         * @return the array of eight three-hourly Ap indices
         */
        public double[] getThreeHourlyAp() {
            return threeHourlyAp.clone();
        }

        /**
         * Gets the arithmetic average of all eight Ap indices for the current entry.
         *
         * @return the average of all eight Ap indices
         */
        public double getApAvg() {
            return apAvg;
        }

        /**
         * Gets the last 81-day arithmetic average of F10.7 (observed).
         *
         * @return the last 81-day arithmetic average of F10.7 (observed)
         */
        public double getLst81Obs() {
            return lst81Obs;
        }

        /**
         * Gets the centered 81-day arithmetic average of F10.7 (observed).
         *
         * @return the centered 81-day arithmetic average of F10.7 (observed)
         */
        public double getCtr81Obs() {
            return ctr81Obs;
        }

        /**
         * Gets the observed (unadjusted) value of F10.7.
         *
         * @return the observed (unadjusted) value of F10.7
         */
        public double getF107Obs() {
            return f107Obs;
        }

        /**
         * Gets the last 81-day arithmetic average of F10.7 (adjusted).
         *
         * @return the last 81-day arithmetic average of F10.7 (adjusted)
         */
        public double getLst81Adj() {
            return lst81Adj;
        }

        /**
         * Gets the centered 81-day arithmetic average of F10.7 (adjusted).
         *
         * @return the centered 81-day arithmetic average of F10.7 (adjusted)
         */
        public double getCtr81Adj() {
            return ctr81Adj;
        }

        /**
         * Gets the Flux Qualifier.
         *
         * @return the Flux Qualifier
         */
        public int getFluxQualifier() {
            return fluxQualifier;
        }

        /**
         * Gets the 10.7-cm Solar Radio Flux (F10.7) Adjusted to 1 AU.
         *
         * @return the 10.7-cm Solar Radio Flux (F10.7) Adjusted to 1 AU
         */
        public double getF107Adj() {
            return f107Adj;
        }
    }

}

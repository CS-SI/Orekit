/*
 * Copyright 2020 CS Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.TreeSet;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.AbstractSelfFeedingLoader;
import org.orekit.data.DataContext;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.models.earth.atmosphere.DTM2000InputParameters;
import org.orekit.models.earth.atmosphere.NRLMSISE00InputParameters;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeStamped;
import org.orekit.utils.Constants;
import org.orekit.utils.ImmutableTimeStampedCache;

/**
 * This class reads and provides solar activity data needed by
 * atmospheric models: F107 solar flux, Ap and Kp indexes.
 * <p>
 * The data are retrieved through space weather files offered by CSSI/AGI.
 * The data can be retrieved on the AGI <a
 * href="ftp://ftp.agi.com/pub/DynamicEarthData/SpaceWeather-All-v1.2.txt">
 * FTP</a>.
 * This file is updated several times a day by using several sources mentioned 
 * in the <a href="http://celestrak.com/SpaceData/SpaceWx-format.php">
 * Celestrak space weather data documentation</a>.
 * </p>
 * 
 * @author Clément Jonglez
 */
public class CssiSpaceWeatherLoader extends AbstractSelfFeedingLoader
        implements DataLoader, DTM2000InputParameters, NRLMSISE00InputParameters {

    /** Serializable UID. */
    private static final long serialVersionUID = 4249411710645968978L;

    /** Number of neighbors to use. */
    private static final int N_NEIGHBORS = 7;

    /** Data set. */
    private ImmutableTimeStampedCache<LineParameters> data;

    /** UTC time scale. */
    private final TimeScale utc;

    /** First available date. */
    private AbsoluteDate firstDate;

    /** Date of last data before the prediction starts */
    private AbsoluteDate lastObservedDate;

    /** Date of last daily prediction before the monthly prediction starts */
    private AbsoluteDate lastDailyPredictedDate;

    /** Last available date. */
    private AbsoluteDate lastDate;

    /** Previous set of solar activity parameters. */
    private LineParameters previousParam;

    /** Current set of solar activity parameters. */
    private LineParameters nextParam;

    /**
     * Simple constructor. This constructor uses the default data context.
     * 
     * @param fileName name of the CSSI space weather file.
     */
    @DefaultDataContext
    public CssiSpaceWeatherLoader(final String fileName) {
        this(fileName, DataContext.getDefault().getDataProvidersManager(),
                DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Constructor that allows specifying the source of the CSSI space weather file.
     *
     * @param fileName name of the CSSI space weather file.
     * @param dataProvidersManager provides access to auxiliary data files.
     * @param utc UTC time scale.
     */
    public CssiSpaceWeatherLoader(final String fileName, final DataProvidersManager dataProvidersManager,
            final TimeScale utc) {
        super(fileName, dataProvidersManager);

        firstDate = null;
        lastObservedDate = null;
        lastDailyPredictedDate = null;
        lastDate = null;
        data = ImmutableTimeStampedCache.emptyCache();
        this.utc = utc;
    }

    /** {@inheritDoc} */
    public AbsoluteDate getMinDate() {
        if (firstDate == null) {
            feed(this);
        }
        return firstDate;
    }

    /** {@inheritDoc} */
    public AbsoluteDate getMaxDate() {
        if (lastDate == null) {
            feed(this);
        }
        return lastDate;
    }

    /** Find the data bracketing a specified date.
     * @param date date to bracket
     */
    private void bracketDate(final AbsoluteDate date) {
        if ((date.durationFrom(firstDate) < 0) || (date.durationFrom(lastDate) > 0)) {
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE, date, firstDate, lastDate);
        }

        // don't search if the cached selection is fine
        if ((previousParam != null) && (date.durationFrom(previousParam.getDate()) > 0)
                && (date.durationFrom(nextParam.getDate()) <= 0)) {
            return;
        }

        Object daNeigbors[] = data.getNeighbors(date).toArray();
        previousParam = (LineParameters) daNeigbors[3];
        nextParam = (LineParameters) daNeigbors[4];
        if (previousParam.getDate().compareTo(date) > 0) {
            /**
             * Throwing exception if neighbors are unbalanced because we are at the beginning of the data set
             */
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE, date, firstDate, lastDate);
        }

        /*
         * if (date.equals(firstDate)) { nextParam = (LineParameters) data.higher(date);
         * previousParam = (LineParameters) data.first(); } else if
         * (date.equals(lastDate)) { nextParam = (LineParameters) data.last();
         * previousParam = (LineParameters) data.floor(date); } else { nextParam =
         * (LineParameters) data.higher(date); previousParam = (LineParameters)
         * data.floor(date); }
         */

    }

    /**
     * Performs a linear interpolation between two values
     * The weights are computed from the time delta between previous date, current date, next date
     * @param date the current date
     * @param previousValue the value at previous date
     * @param nextValue the value at next date
     * @return the value interpolated for the current date
     */
    private double getLinearInterpolation(final AbsoluteDate date, final double previousValue, final double nextValue) {
        // perform a linear interpolation
        final AbsoluteDate previousDate = previousParam.getDate();
        final AbsoluteDate currentDate = nextParam.getDate();
        final double dt = currentDate.durationFrom(previousDate);
        final double previousWeight = currentDate.durationFrom(date) / dt;
        final double nextWeight = date.durationFrom(previousDate) / dt;

        // returns the daily flux interpolated at the date
        return previousValue * previousWeight + nextValue * nextWeight;
    }

    /** {@inheritDoc} */
    public double getInstantFlux(final AbsoluteDate date) {
        // Interpolating two neighboring daily fluxes
        // get the neighboring dates
        bracketDate(date);
        return getLinearInterpolation(date, previousParam.getF107Adj(), nextParam.getF107Adj());
    }

    /** {@inheritDoc} */
    /**
     * TODO: not sure if the mean flux for DTM2000 should be computed differently than the average flux for NRLMSISE00
     */
    public double getMeanFlux(final AbsoluteDate date) {
        return getAverageFlux(date);
    }

    /** {@inheritDoc} */
    public double getThreeHourlyKP(final AbsoluteDate date) {
        if (date.compareTo(lastObservedDate) <= 0) {
            /**
             * If observation data is available, 
             * it contains three-hourly data
             */
            bracketDate(date);
            final double hourOfDay = date.offsetFrom(previousParam.getDate(), utc) / 3600;
            int i_kp = (int) (hourOfDay / 3);
            if (i_kp >= 8) {
                /**
                 * hourOfDay can take the value 24.0 at midnight
                 * due to floating point precision when bracketing the dates
                 * or during a leap second because the hour of day is computed in UTC view
                 */
                i_kp = 7;
            }
            return previousParam.getThreeHourlyKp(i_kp);
        } else {
            /**
             * Only predictions are available,
             * there are no three-hourly data
             */
            return get24HoursKp(date);
        }
    }

    /** {@inheritDoc} */
    public double get24HoursKp(final AbsoluteDate date) {
        if (date.compareTo(lastDailyPredictedDate) <= 0) {
            // Daily data is available, just taking the daily average
            bracketDate(date);
            return previousParam.getKpSum() / 8;
        } else {
            // Only monthly data is available, better interpolate between two months
            // get the neighboring dates
            bracketDate(date);
            return getLinearInterpolation(date, previousParam.getKpSum() / 8, nextParam.getKpSum() / 8);
        }
    }

    /** {@inheritDoc} */
    public double getDailyFlux(final AbsoluteDate date) {
        // Getting the value for the previous day
        return getDailyFluxOnDay(date.shiftedBy(-Constants.JULIAN_DAY));
    }

    /**
     * Gets the daily flux on the current day.
     * @param date the current date
     * @return the daily F10.7 flux (adjusted)
     */
    private double getDailyFluxOnDay(final AbsoluteDate date) {
        if (date.compareTo(lastDailyPredictedDate) <= 0) {
            // Getting the value for the previous day
            bracketDate(date);
            return previousParam.getF107Adj();
        } else {
            // Only monthly data is available, better interpolate between two months
            // get the neighboring dates
            bracketDate(date);
            return getLinearInterpolation(date, previousParam.getF107Adj(), nextParam.getF107Adj());
        }
    }

    /** {@inheritDoc} */
    public double getAverageFlux(final AbsoluteDate date) {
        if (date.compareTo(lastDailyPredictedDate) <= 0) {
            bracketDate(date);
            return previousParam.getCtr81Adj();
        } else {
            // Only monthly data is available, better interpolate between two months
            // get the neighboring dates
            bracketDate(date);
            return getLinearInterpolation(date, previousParam.getCtr81Adj(), nextParam.getCtr81Adj());
        }
    }

    /** {@inheritDoc} */
    public double[] getAp(final AbsoluteDate date) {
        final double[] apArray = new double[7];
        apArray[0] = getDailyAp(date);
        apArray[1] = getThreeHourlyAp(date);
        apArray[2] = getThreeHourlyAp(date.shiftedBy(-3.0 * 3600.0));
        apArray[3] = getThreeHourlyAp(date.shiftedBy(-6.0 * 3600.0));
        apArray[4] = getThreeHourlyAp(date.shiftedBy(-9.0 * 3600.0));
        apArray[5] = get24HoursAverageAp(date.shiftedBy(-12.0 * 3600.0));
        apArray[6] = get24HoursAverageAp(date.shiftedBy(-36.0 * 3600.0));
        return apArray;
    }

    /**
     * Gets the value of the three-hourly Ap index for the given date
     * @param date the current date
     * @return the current three-hourly Ap index
     */
    private double getThreeHourlyAp(final AbsoluteDate date) {
        if (date.compareTo(lastObservedDate) <= 0) {
            /**
             * If observation data is available, it contains three-hourly data.
             */
            bracketDate(date);
            final double hourOfDay = date.offsetFrom(previousParam.getDate(), utc) / 3600;
            int i_ap = (int) (hourOfDay / 3);
            if (i_ap >= 8) {
                /**
                 * hourOfDay can take the value 24.0 at midnight
                 * due to floating point precision when bracketing the dates
                 * or during a leap second because the hour of day is computed in UTC view
                 */
                i_ap = 7;
            }
            return previousParam.getThreeHourlyAp(i_ap);
        } else {
            /**
             * Only predictions are available,
             * there are no three-hourly data
             */
            return getDailyAp(date);
        }
    }

    /**
     * Gets the running average of the 8 three-hourly Ap indices prior to current time
     * If three-hourly data is available, the result is different than getDailyAp
     * @param date the current date
     * @return the 24 hours running average of the Ap index 
     */
    private double get24HoursAverageAp(final AbsoluteDate date) {
        if (date.compareTo(lastDailyPredictedDate) <= 0) {
            // Computing running mean
            double apSum = 0.0;
            for (int i = 0; i < 8; i++) {
                apSum += getThreeHourlyAp(date.shiftedBy(-3.0 * 3600 * i));
            }
            return apSum / 8;
        } else {
            /**
             * Only monthly predictions are available,
             * no need to compute the average from three hourly data
             */
            return getDailyAp(date);
        }
    }

    /**
     * Get the daily Ap index for the given date
     * @param date the current date
     * @return the daily Ap index
     */
    private double getDailyAp(final AbsoluteDate date) {
        if (date.compareTo(lastDailyPredictedDate) <= 0) {
            // Daily data is available, just taking the daily average
            bracketDate(date);
            return previousParam.getApAvg();
        } else {
            // Only monthly data is available, better interpolate between two months
            // get the neighboring dates
            bracketDate(date);
            return getLinearInterpolation(date, previousParam.getApAvg(), nextParam.getApAvg());
        }
    }

    public String getSupportedNames() {
        return super.getSupportedNames();
    }

    /** Container class for Solar activity indexes.  */
    private static class LineParameters implements TimeStamped, Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 8151260459653484163L;

        /** Entry date. */
        private final AbsoluteDate date;

        /** Array of 8 three-hourly Kp indices for this entry */
        private final double[] threeHourlyKp;

        /** Sum of the 8 Kp indices for the day expressed to the nearest third of a unit. */
        private final double kpSum;

        /** Array of 8 three-hourly Ap indices for this entry */
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

        public LineParameters(AbsoluteDate date, final double[] threeHourlyKp, final double kpSum,
                final double[] threeHourlyAp, final double apAvg, final double f107Adj, final int fluxQualifier,
                final double ctr81Adj, final double lst81Adj, final double f107Obs, final double ctr81Obs,
                final double lst81Obs) {
            this.date = date;
            this.threeHourlyKp = threeHourlyKp;
            this.kpSum = kpSum;
            this.threeHourlyAp = threeHourlyAp;
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

        public double[] getThreeHourlyKp() {
            return threeHourlyKp;
        }

        public double getThreeHourlyKp(final int i) {
            return threeHourlyKp[i];
        }

        public double getKpSum() {
            return kpSum;
        }

        public double[] getThreeHourlyAp() {
            return threeHourlyAp;
        }

        public double getThreeHourlyAp(final int i) {
            return threeHourlyAp[i];
        }

        public double getApAvg() {
            return apAvg;
        }

        public double getLst81Obs() {
            return lst81Obs;
        }

        public double getCtr81Obs() {
            return ctr81Obs;
        }

        public double getF107Obs() {
            return f107Obs;
        }

        public double getLst81Adj() {
            return lst81Adj;
        }

        public double getCtr81Adj() {
            return ctr81Adj;
        }

        public int getFluxQualifier() {
            return fluxQualifier;
        }

        public double getF107Adj() {
            return f107Adj;
        }

    }

    /**
     * checks if the string contains a floating point number
     * @param strNum string to check
     * @return true if string contains a valid floating point number, else false
     */
    private static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
            throws IOException, ParseException, OrekitException {

        TreeSet<LineParameters> set = new TreeSet<LineParameters>(new ChronologicalComparator());

        // read the data
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
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
                         * Kp is written as an integer where a unit equals 0.1,
                         * the conversion is Kp_double = 0.1 * double(Kp_integer)
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

        firstDate = set.first().getDate();
        lastDate = set.last().getDate();

        data = new ImmutableTimeStampedCache<LineParameters>(N_NEIGHBORS, set);

        return;
    }

    /** {@inheritDoc} */
    public boolean stillAcceptsData() {
        return true;
    }

}

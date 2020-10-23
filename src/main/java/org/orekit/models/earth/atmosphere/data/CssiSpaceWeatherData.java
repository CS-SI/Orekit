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

import java.util.List;
import java.util.stream.Collectors;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.AbstractSelfFeedingLoader;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.models.earth.atmosphere.DTM2000InputParameters;
import org.orekit.models.earth.atmosphere.NRLMSISE00InputParameters;
import org.orekit.models.earth.atmosphere.data.CssiSpaceWeatherDataLoader.LineParameters;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeStamped;
import org.orekit.utils.Constants;
import org.orekit.utils.ImmutableTimeStampedCache;

/**
 * This class provides three-hourly and daily solar activity data needed by atmospheric
 * models: F107 solar flux, Ap and Kp indexes.
 * The {@link org.orekit.data.DataLoader} implementation and the parsing is handled by the class {@link CssiSpaceWeatherDataLoader}.
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
public class CssiSpaceWeatherData extends AbstractSelfFeedingLoader
        implements DTM2000InputParameters, NRLMSISE00InputParameters {

    /** Serializable UID. */
    private static final long serialVersionUID = 4249411710645968978L;

    /** Size of the list. */
    private static final int N_NEIGHBORS = 2;

    /** Data set. */
    private final transient ImmutableTimeStampedCache<TimeStamped> data;

    /** UTC time scale. */
    private final TimeScale utc;

    /** First available date. */
    private final AbsoluteDate firstDate;

    /** Date of last data before the prediction starts. */
    private final AbsoluteDate lastObservedDate;

    /** Date of last daily prediction before the monthly prediction starts. */
    private final AbsoluteDate lastDailyPredictedDate;

    /** Last available date. */
    private final AbsoluteDate lastDate;

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
    public CssiSpaceWeatherData(final String fileName) {
        this(fileName, DataContext.getDefault().getDataProvidersManager(),
                DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Constructor that allows specifying the source of the CSSI space weather file.
     *
     * @param fileName             name of the CSSI space weather file.
     * @param dataProvidersManager provides access to auxiliary data files.
     * @param utc                  UTC time scale.
     */
    public CssiSpaceWeatherData(final String fileName, final DataProvidersManager dataProvidersManager,
            final TimeScale utc) {
        super(fileName, dataProvidersManager);

        this.utc = utc;
        final CssiSpaceWeatherDataLoader loader = new CssiSpaceWeatherDataLoader(utc);
        this.feed(loader);
        data = new ImmutableTimeStampedCache<>(N_NEIGHBORS, loader.getDataSet());
        firstDate = loader.getMinDate();
        lastDate = loader.getMaxDate();
        lastObservedDate = loader.getLastObservedDate();
        lastDailyPredictedDate = loader.getLastDailyPredictedDate();
    }

    /** {@inheritDoc} */
    public AbsoluteDate getMinDate() {
        return firstDate;
    }

    /** {@inheritDoc} */
    public AbsoluteDate getMaxDate() {
        return lastDate;
    }

    /**
     * Find the data bracketing a specified date.
     *
     * @param date date to bracket
     */
    private void bracketDate(final AbsoluteDate date) {
        if ((date.durationFrom(firstDate) < 0) || (date.durationFrom(lastDate) > 0)) {
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE, date, firstDate, lastDate);
        }

        // don't search if the cached selection is fine
        if ((previousParam != null) && (date.durationFrom(previousParam.getDate()) > 0) &&
                        (date.durationFrom(nextParam.getDate()) <= 0)) {
            return;
        }

        final List<TimeStamped> neigbors = data.getNeighbors(date).collect(Collectors.toList());
        previousParam = (LineParameters) neigbors.get(0);
        nextParam = (LineParameters) neigbors.get(1);
        if (previousParam.getDate().compareTo(date) > 0) {
            /**
             * Throwing exception if neighbors are unbalanced because we are at the
             * beginning of the data set
             */
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE, date, firstDate, lastDate);
        }
    }

    /**
     * Performs a linear interpolation between two values The weights are computed
     * from the time delta between previous date, current date, next date.
     *
     * @param date          the current date
     * @param previousValue the value at previous date
     * @param nextValue     the value at next date
     * @return the value interpolated for the current date
     */
    private double getLinearInterpolation(final AbsoluteDate date, final double previousValue, final double nextValue) {
        // perform a linear interpolation
        final AbsoluteDate previousDate = previousParam.getDate();
        final AbsoluteDate currentDate = nextParam.getDate();
        final double dt = currentDate.durationFrom(previousDate);
        final double previousWeight = currentDate.durationFrom(date) / dt;
        final double nextWeight = date.durationFrom(previousDate) / dt;

        // returns the data interpolated at the date
        return previousValue * previousWeight + nextValue * nextWeight;
    }

    /** {@inheritDoc} */
    public double getInstantFlux(final AbsoluteDate date) {
        // Interpolating two neighboring daily fluxes
        // get the neighboring dates
        bracketDate(date);
        return getLinearInterpolation(date, previousParam.getF107Adj(), nextParam.getF107Adj());
    }

    /** {@inheritDoc}
     * FIXME not sure if the mean flux for DTM2000 should be computed differently
     * than the average flux for NRLMSISE00
     */
    public double getMeanFlux(final AbsoluteDate date) {
        return getAverageFlux(date);
    }

    /** {@inheritDoc} */
    public double getThreeHourlyKP(final AbsoluteDate date) {
        if (date.compareTo(lastObservedDate) <= 0) {
            /**
             * If observation data is available, it contains three-hourly data
             */
            bracketDate(date);
            final double hourOfDay = date.offsetFrom(previousParam.getDate(), utc) / 3600;
            int i_kp = (int) (hourOfDay / 3);
            if (i_kp >= 8) {
                /**
                 * hourOfDay can take the value 24.0 at midnight due to floating point precision
                 * when bracketing the dates or during a leap second because the hour of day is
                 * computed in UTC view
                 */
                i_kp = 7;
            }
            return previousParam.getThreeHourlyKp(i_kp);
        } else {
            /**
             * Only predictions are available, there are no three-hourly data
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
     *
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
     * Gets the value of the three-hourly Ap index for the given date.
     *
     * @param date the current date
     * @return the current three-hourly Ap index
     */
    private double getThreeHourlyAp(final AbsoluteDate date) {
        if (date.compareTo(lastObservedDate.shiftedBy(Constants.JULIAN_DAY)) < 0) {
            /**
             * If observation data is available, it contains three-hourly data.
             */
            bracketDate(date);
            final double hourOfDay = date.offsetFrom(previousParam.getDate(), utc) / 3600;
            int i_ap = (int) (hourOfDay / 3);
            if (i_ap >= 8) {
                /**
                 * hourOfDay can take the value 24.0 at midnight due to floating point precision
                 * when bracketing the dates or during a leap second because the hour of day is
                 * computed in UTC view
                 */
                i_ap = 7;
            }
            return previousParam.getThreeHourlyAp(i_ap);
        } else {
            /**
             * Only predictions are available, there are no three-hourly data
             */
            return getDailyAp(date);
        }
    }

    /**
     * Gets the running average of the 8 three-hourly Ap indices prior to current
     * time If three-hourly data is available, the result is different than
     * getDailyAp.
     *
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
             * Only monthly predictions are available, no need to compute the average from
             * three hourly data
             */
            return getDailyAp(date);
        }
    }

    /**
     * Get the daily Ap index for the given date.
     *
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
}

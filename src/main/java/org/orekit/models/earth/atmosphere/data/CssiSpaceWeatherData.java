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

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DataSource;
import org.orekit.models.earth.atmosphere.data.CssiSpaceWeatherDataLoader.LineParameters;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;
import org.orekit.utils.GenericTimeStampedCache;
import org.orekit.utils.OrekitConfiguration;

/**
 * This class provides three-hourly and daily solar activity data needed by atmospheric models: F107 solar flux, Ap and Kp
 * indexes. The {@link org.orekit.data.DataLoader} implementation and the parsing is handled by the class
 * {@link CssiSpaceWeatherDataLoader}.
 * <p>
 * The data are retrieved through space weather files offered by AGI/CSSI on the AGI
 * <a href="https://ftp.agi.com/pub/DynamicEarthData/SpaceWeather-All-v1.2.txt">FTP</a> as
 * well as on the CelesTrack <a href="http://celestrak.com/SpaceData/">website</a>. These files are updated several times a
 * day by using several sources mentioned in the
 * <a href="http://celestrak.com/SpaceData/SpaceWx-format.php">Celestrak space
 * weather data documentation</a>.
 * </p>
 *
 * @author Clément Jonglez
 * @author Vincent Cucchietti
 * @since 10.2
 */
public class CssiSpaceWeatherData extends AbstractSolarActivityData<LineParameters, CssiSpaceWeatherDataLoader> {

    /** Default regular expression for supported names that works with all officially published files. */
    public static final String DEFAULT_SUPPORTED_NAMES = "^S(?:pace)?W(?:eather)?-(?:All)?.*\\.txt$";

    /** Serializable UID. */
    private static final long serialVersionUID = 4249411710645968978L;

    /** Date of last data before the prediction starts. */
    private final AbsoluteDate lastObservedDate;

    /** Date of last daily prediction before the monthly prediction starts. */
    private final AbsoluteDate lastDailyPredictedDate;

    /**
     * Simple constructor. This constructor uses the default data context.
     * <p>
     * The original file names provided by AGI/CSSI are of the form: SpaceWeather-All-v1.2.txt
     * (<a href="https://ftp.agi.com/pub/DynamicEarthData/SpaceWeather-All-v1.2.txt">AGI's ftp</a>). So a recommended regular
     * expression for the supported names that works with all published files is: {@link #DEFAULT_SUPPORTED_NAMES}.
     * <p>
     * It provides a default configuration for the thread safe cache :
     * <ul>
     *     <li>Number of slots : {@code OrekitConfiguration.getCacheSlotsNumber()}</li>
     *     <li>Max span : {@code Constants.JULIAN_DAY}</li>
     *     <li>Max span interval : {@code 0}</li>
     * </ul>
     *
     * @param supportedNames regular expression for supported AGI/CSSI space weather files names
     */
    @DefaultDataContext
    public CssiSpaceWeatherData(final String supportedNames) {
        this(supportedNames, DataContext.getDefault().getDataProvidersManager(),
             DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Constructor that allows specifying the source of the CSSI space weather file.
     * <p>
     * It provides a default configuration for the thread safe cache :
     * <ul>
     *     <li>Number of slots : {@code OrekitConfiguration.getCacheSlotsNumber()}</li>
     *     <li>Max span : {@code Constants.JULIAN_DAY}</li>
     *     <li>Max span interval : {@code 0}</li>
     * </ul>
     *
     * @param supportedNames regular expression for supported AGI/CSSI space weather files names
     * @param dataProvidersManager provides access to auxiliary data files.
     * @param utc UTC time scale.
     */
    public CssiSpaceWeatherData(final String supportedNames, final DataProvidersManager dataProvidersManager,
                                final TimeScale utc) {
        this(supportedNames, new CssiSpaceWeatherDataLoader(utc), dataProvidersManager, utc);
    }

    /**
     * Constructor that allows specifying the source of the CSSI space weather file.
     * <p>
     * It provides a default configuration for the thread safe cache :
     * <ul>
     *     <li>Number of slots : {@code OrekitConfiguration.getCacheSlotsNumber()}</li>
     *     <li>Max span : {@code Constants.JULIAN_DAY}</li>
     *     <li>Max span interval : {@code 0}</li>
     * </ul>
     *
     * @param supportedNames regular expression for supported AGI/CSSI space weather files names
     * @param loader data loader
     * @param dataProvidersManager provides access to auxiliary data files.
     * @param utc UTC time scale
     */
    public CssiSpaceWeatherData(final String supportedNames, final CssiSpaceWeatherDataLoader loader,
                                final DataProvidersManager dataProvidersManager, final TimeScale utc) {
        this(supportedNames, loader, dataProvidersManager, utc, OrekitConfiguration.getCacheSlotsNumber(),
             Constants.JULIAN_DAY, 0);
    }

    /**
     * Constructor that allows specifying the source of the CSSI space weather file and customizable thread safe cache
     * configuration.
     *
     * @param supportedNames regular expression for supported AGI/CSSI space weather files names
     * @param loader data loader
     * @param dataProvidersManager provides access to auxiliary data files.
     * @param utc UTC time scale
     * @param maxSlots maximum number of independent cached time slots in the
     * {@link GenericTimeStampedCache time-stamped cache}
     * @param maxSpan maximum duration span in seconds of one slot in the {@link GenericTimeStampedCache time-stamped cache}
     * @param maxInterval time interval above which a new slot is created in the
     * {@link GenericTimeStampedCache time-stamped cache}
     */
    public CssiSpaceWeatherData(final String supportedNames, final CssiSpaceWeatherDataLoader loader,
                                final DataProvidersManager dataProvidersManager, final TimeScale utc, final int maxSlots,
                                final double maxSpan, final double maxInterval) {
        super(supportedNames, loader, dataProvidersManager, utc, maxSlots, maxSpan, maxInterval, Constants.JULIAN_DAY);

        // Initialise fields
        this.lastObservedDate       = loader.getLastObservedDate();
        this.lastDailyPredictedDate = loader.getLastDailyPredictedDate();
    }

    /**
     * Simple constructor which use the {@link DataContext#getDefault() default data context}.
     * <p>
     * It provides a default configuration for the thread safe cache :
     * <ul>
     *     <li>Number of slots : {@code OrekitConfiguration.getCacheSlotsNumber()}</li>
     *     <li>Max span : {@code Constants.JULIAN_DAY}</li>
     *     <li>Max span interval : {@code 0}</li>
     * </ul>
     *
     * @param source source for the data
     *
     * @since 12.0
     */
    @DefaultDataContext
    public CssiSpaceWeatherData(final DataSource source) {
        this(source, DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Simple constructor.
     * <p>
     * It provides a default configuration for the thread safe cache :
     * <ul>
     *     <li>Number of slots : {@code OrekitConfiguration.getCacheSlotsNumber()}</li>
     *     <li>Max span : {@code Constants.JULIAN_DAY}</li>
     *     <li>Max span interval : {@code 0}</li>
     * </ul>
     *
     * @param source source for the data
     * @param utc UTC time scale
     *
     * @since 12.0
     */
    public CssiSpaceWeatherData(final DataSource source, final TimeScale utc) {
        this(source, new CssiSpaceWeatherDataLoader(utc), utc);
    }

    /**
     * Simple constructor.
     * <p>
     * It provides a default configuration for the thread safe cache :
     * <ul>
     *     <li>Number of slots : {@code OrekitConfiguration.getCacheSlotsNumber()}</li>
     *     <li>Max span : {@code Constants.JULIAN_DAY}</li>
     *     <li>Max span interval : {@code 0}</li>
     * </ul>
     *
     * @param source source for the data
     * @param loader data loader
     * @param utc UTC time scale
     *
     * @since 12.0
     */
    public CssiSpaceWeatherData(final DataSource source, final CssiSpaceWeatherDataLoader loader, final TimeScale utc) {
        this(source, loader, utc, OrekitConfiguration.getCacheSlotsNumber(), Constants.JULIAN_DAY, 0);
    }

    /**
     * Simple constructor with customizable thread safe cache configuration.
     *
     * @param source source for the data
     * @param loader data loader
     * @param utc UTC time scale
     * @param maxSlots maximum number of independent cached time slots in the
     * {@link GenericTimeStampedCache time-stamped cache}
     * @param maxSpan maximum duration span in seconds of one slot in the {@link GenericTimeStampedCache time-stamped cache}
     * @param maxInterval time interval above which a new slot is created in the
     * {@link GenericTimeStampedCache time-stamped cache}
     *
     * @since 12.0
     */
    public CssiSpaceWeatherData(final DataSource source, final CssiSpaceWeatherDataLoader loader, final TimeScale utc,
                                final int maxSlots, final double maxSpan, final double maxInterval) {
        super(source, loader, utc, maxSlots, maxSpan, maxInterval, Constants.JULIAN_DAY);
        this.lastObservedDate       = loader.getLastObservedDate();
        this.lastDailyPredictedDate = loader.getLastDailyPredictedDate();
    }

    /** {@inheritDoc} */
    public double getInstantFlux(final AbsoluteDate date) {
        return getLinearInterpolation(date, LineParameters::getF107Obs);
    }

    /** {@inheritDoc} */
    public double getMeanFlux(final AbsoluteDate date) {
        return getAverageFlux(date);
    }

    /** {@inheritDoc} */
    public double getThreeHourlyKP(final AbsoluteDate date) {
        if (date.compareTo(lastObservedDate) <= 0) {
            /* If observation data is available, it contains three-hourly data */
            final LocalSolarActivity localSolarActivity = new LocalSolarActivity(date);
            final LineParameters     previousParam      = localSolarActivity.getPreviousParam();
            final double             hourOfDay          = date.offsetFrom(previousParam.getDate(), getUTC()) / 3600;
            int                      i_kp               = (int) (hourOfDay / 3);
            if (i_kp >= 8) {
                /* hourOfDay can take the value 24.0 at midnight due to floating point precision
                 * when bracketing the dates or during a leap second because the hour of day is
                 * computed in UTC view */
                i_kp = 7;
            }
            return previousParam.getThreeHourlyKp(i_kp);
        } else {
            /* Only predictions are available, there are no three-hourly data */
            return get24HoursKp(date);
        }
    }

    /** {@inheritDoc} */
    public double get24HoursKp(final AbsoluteDate date) {
        // Get the neighboring solar activity
        final LocalSolarActivity localSolarActivity = new LocalSolarActivity(date);

        if (date.compareTo(lastDailyPredictedDate) <= 0) {
            // Daily data is available, just taking the daily average
            return localSolarActivity.getPreviousParam().getKpSum() / 8;
        } else {
            // Only monthly data is available, better interpolate between two months
            return getLinearInterpolation(localSolarActivity, lineParam -> lineParam.getKpSum() / 8);
        }
    }

    /** {@inheritDoc} */
    public double getDailyFlux(final AbsoluteDate date) {
        // Getting the value for the previous day
        return getDailyFluxOnDay(date.shiftedBy(-Constants.JULIAN_DAY));
    }

    /** {@inheritDoc} */
    public double getAverageFlux(final AbsoluteDate date) {
        // Get the neighboring solar activity
        final LocalSolarActivity localSolarActivity = new LocalSolarActivity(date);

        if (date.compareTo(lastDailyPredictedDate) <= 0) {
            return localSolarActivity.getPreviousParam().getCtr81Obs();
        } else {
            // Only monthly data is available, better interpolate between two months
            return getLinearInterpolation(localSolarActivity, LineParameters::getCtr81Obs);
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
     * Gets the daily flux on the current day.
     *
     * @param date the current date
     *
     * @return the daily F10.7 flux (observed)
     */
    private double getDailyFluxOnDay(final AbsoluteDate date) {
        // Get the neighboring solar activity
        final LocalSolarActivity localSolarActivity = new LocalSolarActivity(date);

        if (date.compareTo(lastDailyPredictedDate) <= 0) {
            // Getting the value for the previous day
            return localSolarActivity.getPreviousParam().getF107Obs();
        } else {
            // Only monthly data is available, better interpolate between two months
            return getLinearInterpolation(localSolarActivity, LineParameters::getF107Obs);
        }
    }

    /**
     * Gets the value of the three-hourly Ap index for the given date.
     *
     * @param date the current date
     *
     * @return the current three-hourly Ap index
     */
    private double getThreeHourlyAp(final AbsoluteDate date) {
        if (date.compareTo(lastObservedDate.shiftedBy(Constants.JULIAN_DAY)) < 0) {
            // If observation data is available, it contains three-hourly data.
            // Get the neighboring solar activity
            final LocalSolarActivity localSolarActivity = new LocalSolarActivity(date);

            final LineParameters previousParam = localSolarActivity.getPreviousParam();
            final double         hourOfDay     = date.offsetFrom(previousParam.getDate(), getUTC()) / 3600;
            int                  i_ap          = (int) (hourOfDay / 3);
            if (i_ap >= 8) {
                /* hourOfDay can take the value 24.0 at midnight due to floating point precision
                 * when bracketing the dates or during a leap second because the hour of day is
                 * computed in UTC view */
                i_ap = 7;
            }
            return previousParam.getThreeHourlyAp(i_ap);
        } else {
            /* Only predictions are available, there are no three-hourly data */
            return getDailyAp(date);
        }
    }

    /**
     * Gets the running average of the 8 three-hourly Ap indices prior to current time If three-hourly data is available, the
     * result is different than getDailyAp.
     *
     * @param date the current date
     *
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
            /* Only monthly predictions are available, no need to compute the average from
             * three hourly data */
            return getDailyAp(date);
        }
    }

    /**
     * Get the daily Ap index for the given date.
     *
     * @param date the current date
     *
     * @return the daily Ap index
     */
    private double getDailyAp(final AbsoluteDate date) {
        // Get the neighboring solar activity
        final LocalSolarActivity localSolarActivity = new LocalSolarActivity(date);

        if (date.compareTo(lastDailyPredictedDate) <= 0) {
            // Daily data is available, just taking the daily average
            return localSolarActivity.getPreviousParam().getApAvg();
        } else {
            // Only monthly data is available, better interpolate between two months
            return getLinearInterpolation(localSolarActivity, LineParameters::getApAvg);
        }
    }

}

/* Copyright 2002-2023 CS GROUP
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
package org.orekit.models.earth.atmosphere.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.interpolation.LinearInterpolator;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.models.earth.atmosphere.data.MarshallSolarActivityFutureEstimationLoader.LineParameters;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeStampedDouble;
import org.orekit.utils.Constants;
import org.orekit.utils.GenericTimeStampedCache;
import org.orekit.utils.OrekitConfiguration;
import org.orekit.utils.TimeStampedGenerator;

/**
 * This class provides solar activity data needed by atmospheric models: F107 solar flux, Ap and Kp indexes.
 * <p>
 * Data comes from the NASA Marshall Solar Activity Future Estimation (MSAFE) as estimates of monthly F10.7
 * Mean solar flux and Ap geomagnetic parameter
 * (see <a href="https://www.nasa.gov/solar-cycle-progression-and-forecast"> Marshall Solar Activity website</a>).
 *
 * <p>Data can be retrieved at the NASA
 * <a href="https://www.nasa.gov/solar-cycle-progression-and-forecast/archived-forecast/"> Marshall Solar Activity archived forecast</a>.
 * Here Kp indices are deduced from Ap indexes, which in turn are tabulated equivalent of retrieved Ap values.
 *
 * <p> If several MSAFE files are available, some dates may appear in several files (for example August 2007 is in all files from
 * the first one published in March 1999 to the February 2008 file). In this case, the data from the most recent file is used
 * and the older ones are discarded. The date of the file is assumed to be 6 months after its first entry (which explains why
 * the file having August 2007 as its first entry is the February 2008 file). This implies that MSAFE files must <em>not</em>
 * be edited to change their time span, otherwise this would break the old entries overriding mechanism.
 *
 * <p>With these data, the {@link #getInstantFlux(AbsoluteDate)} and {@link #getMeanFlux(AbsoluteDate)} methods return the same
 * values and the {@link #get24HoursKp(AbsoluteDate)} and {@link #getThreeHourlyKP(AbsoluteDate)} methods return the same
 * values.
 *
 * <p>Conversion from Ap index values in the MSAFE file to Kp values used by atmosphere models is done using Jacchia's equation
 * in [1].
 *
 * <p>With these data, the {@link #getAp(AbsoluteDate date)} method returns an array of seven times the same daily Ap value,
 * i.e. it is suited to be used only with the {@link org.orekit.models.earth.atmosphere.NRLMSISE00 NRLMSISE00} atmospheric
 * model where the switch #9 is set to 1.
 *
 * <h2>References</h2>
 *
 * <ol> <li> Jacchia, L. G. "CIRA 1972, recent atmospheric models, and improvements in
 * progress." COSPAR, 21st Plenary Meeting. Vol. 1. 1978. </li> </ol>
 *
 * @author Bruno Revelin
 * @author Luc Maisonobe
 * @author Evan Ward
 * @author Pascal Parraud
 * @author Vincent Cucchietti
 */
public class MarshallSolarActivityFutureEstimation
        extends AbstractSolarActivityData<LineParameters, MarshallSolarActivityFutureEstimationLoader> {

    /**
     * Default regular expression for the supported name that work with all officially published files.
     *
     * @since 10.0
     */
    public static final String DEFAULT_SUPPORTED_NAMES =
            "\\p{Alpha}\\p{Lower}\\p{Lower}\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit}(?:f|F)10(?:[-_]prd)?\\.(?:txt|TXT)";

    /** Serializable UID. */
    private static final long serialVersionUID = -5212198874900835369L;

    /** Selected strength level of activity. */
    private final StrengthLevel strengthLevel;

    /** Cache dedicated to average flux. */
    private final transient GenericTimeStampedCache<TimeStampedDouble> averageFluxCache;

    /**
     * Simple constructor. This constructor uses the {@link DataContext#getDefault() default data context}.
     * <p>
     * The original file names used by NASA Marshall space center are of the form: may2019f10_prd.txt or Oct1999F10.TXT. So a
     * recommended regular expression for the supported name that work with all published files is:
     * {@link #DEFAULT_SUPPORTED_NAMES}.
     * <p>
     * It provides a default configuration for the thread safe cache :
     * <ul>
     *     <li>Number of slots : {@code OrekitConfiguration.getCacheSlotsNumber()}</li>
     *     <li>Max span : {@code Constants.JULIAN_YEAR}</li>
     *     <li>Max span interval : {@code 0}</li>
     * </ul>
     *
     * @param supportedNames regular expression for supported files names
     * @param strengthLevel selected strength level of activity
     *
     * @see #MarshallSolarActivityFutureEstimation(String, StrengthLevel, DataProvidersManager, TimeScale)
     */
    @DefaultDataContext
    public MarshallSolarActivityFutureEstimation(final String supportedNames,
                                                 final StrengthLevel strengthLevel) {
        this(supportedNames, strengthLevel,
             DataContext.getDefault().getDataProvidersManager(),
             DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Constructor that allows specifying the source of the MSAFE auxiliary data files.
     * <p>
     * It provides a default configuration for the thread safe cache :
     * <ul>
     *     <li>Number of slots : {@code OrekitConfiguration.getCacheSlotsNumber()}</li>
     *     <li>Max span : {@code 31 * Constants.JULIAN_DAY}</li>
     *     <li>Max interval : {@code 0}</li>
     *     <li>Minimum step: {@code 27 * Constants.JULIAN_DAY}</li>
     * </ul>
     *
     * @param supportedNames regular expression for supported files names
     * @param strengthLevel selected strength level of activity
     * @param dataProvidersManager provides access to auxiliary data files.
     * @param utc UTC time scale.
     *
     * @since 10.1
     */
    public MarshallSolarActivityFutureEstimation(final String supportedNames,
                                                 final StrengthLevel strengthLevel,
                                                 final DataProvidersManager dataProvidersManager,
                                                 final TimeScale utc) {
        this(supportedNames, strengthLevel, dataProvidersManager, utc, OrekitConfiguration.getCacheSlotsNumber(),
             Constants.JULIAN_DAY * 31, 0, Constants.JULIAN_DAY * 27);
    }

    /**
     * Constructor that allows specifying the source of the MSAFE auxiliary data files and customizable thread safe cache
     * configuration.
     *
     * @param supportedNames regular expression for supported files names
     * @param strengthLevel selected strength level of activity
     * @param dataProvidersManager provides access to auxiliary data files.
     * @param utc UTC time scale.
     * @param maxSlots maximum number of independent cached time slots in the
     * {@link GenericTimeStampedCache time-stamped cache}
     * @param maxSpan maximum duration span in seconds of one slot in the {@link GenericTimeStampedCache time-stamped cache}
     * @param maxInterval time interval above which a new slot is created in the
     * {@link GenericTimeStampedCache time-stamped cache}
     * @param minimumStep overriding minimum step designed for non-homogeneous tabulated values. To be used for example when
     * caching monthly tabulated values. May be null.
     *
     * @since 10.1
     */
    public MarshallSolarActivityFutureEstimation(final String supportedNames,
                                                 final StrengthLevel strengthLevel,
                                                 final DataProvidersManager dataProvidersManager,
                                                 final TimeScale utc,
                                                 final int maxSlots,
                                                 final double maxSpan,
                                                 final double maxInterval,
                                                 final double minimumStep) {
        super(supportedNames, new MarshallSolarActivityFutureEstimationLoader(strengthLevel, utc),
              dataProvidersManager, utc, maxSlots, maxSpan, maxInterval, minimumStep);

        // Initialise fields
        this.strengthLevel    = strengthLevel;
        this.averageFluxCache = new GenericTimeStampedCache<>(N_NEIGHBORS, OrekitConfiguration.getCacheSlotsNumber(),
                                                              Constants.JULIAN_DAY, 0, new AverageFluxGenerator());
    }

    /**
     * Simple constructor which use the {@link DataContext#getDefault() default data context}.
     * <p>
     * It provides a default configuration for the thread safe cache :
     * <ul>
     *     <li>Number of slots : {@code OrekitConfiguration.getCacheSlotsNumber()}</li>
     *     <li>Max span : {@code 31 * Constants.JULIAN_DAY}</li>
     *     <li>Max interval : {@code 0}</li>
     *     <li>Minimum step: {@code 27 * Constants.JULIAN_DAY}</li>
     * </ul>
     *
     * @param source source for the data
     * @param strengthLevel selected strength level of activity
     *
     * @since 12.0
     */
    @DefaultDataContext
    public MarshallSolarActivityFutureEstimation(final DataSource source,
                                                 final StrengthLevel strengthLevel) {
        this(source, strengthLevel, DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Simple constructor.
     * <p>
     * It provides a default configuration for the thread safe cache :
     * <ul>
     *     <li>Number of slots : {@code OrekitConfiguration.getCacheSlotsNumber()}</li>
     *     <li>Max span : {@code 31 * Constants.JULIAN_DAY}</li>
     *     <li>Max interval : {@code 0}</li>
     *     <li>Minimum step: {@code 27 * Constants.JULIAN_DAY}</li>
     * </ul>
     *
     * @param source source for the data
     * @param strengthLevel selected strength level of activity
     * @param utc UTC time scale
     *
     * @since 12.0
     */
    public MarshallSolarActivityFutureEstimation(final DataSource source,
                                                 final StrengthLevel strengthLevel,
                                                 final TimeScale utc) {
        this(source, strengthLevel, utc, OrekitConfiguration.getCacheSlotsNumber(),
             Constants.JULIAN_DAY * 31, 0, Constants.JULIAN_DAY * 27);
    }

    /**
     * Constructor with customizable thread safe cache configuration.
     *
     * @param source source for the data
     * @param strengthLevel selected strength level of activity
     * @param utc UTC time scale
     * @param maxSlots maximum number of independent cached time slots in the
     * {@link GenericTimeStampedCache time-stamped cache}
     * @param maxSpan maximum duration span in seconds of one slot in the {@link GenericTimeStampedCache time-stamped cache}
     * @param maxInterval time interval above which a new slot is created in the
     * {@link GenericTimeStampedCache time-stamped cache}
     * @param minimumStep overriding minimum step designed for non-homogeneous tabulated values. To be used for example when
     * caching monthly tabulated values. Use {@code Double.NaN} otherwise.
     *
     * @since 12.0
     */
    public MarshallSolarActivityFutureEstimation(final DataSource source,
                                                 final StrengthLevel strengthLevel,
                                                 final TimeScale utc,
                                                 final int maxSlots,
                                                 final double maxSpan,
                                                 final double maxInterval,
                                                 final double minimumStep) {
        super(source, new MarshallSolarActivityFutureEstimationLoader(strengthLevel, utc), utc,
              maxSlots, maxSpan, maxInterval, minimumStep);
        this.strengthLevel    = strengthLevel;
        this.averageFluxCache = new GenericTimeStampedCache<>(N_NEIGHBORS, OrekitConfiguration.getCacheSlotsNumber(),
                                                              Constants.JULIAN_DAY, 0, new AverageFluxGenerator());
    }

    /** {@inheritDoc} */
    public double getInstantFlux(final AbsoluteDate date) {
        return getMeanFlux(date);
    }

    /** {@inheritDoc} */
    public double getMeanFlux(final AbsoluteDate date) {
        return getLinearInterpolation(date, LineParameters::getF107);
    }

    /** {@inheritDoc} */
    public double getThreeHourlyKP(final AbsoluteDate date) {
        return get24HoursKp(date);
    }

    /**
     * Get the date of the file from which data at the specified date comes from.
     * <p>
     * If several MSAFE files are available, some dates may appear in several files (for example August 2007 is in all files
     * from the first one published in March 1999 to the February 2008 file). In this case, the data from the most recent
     * file is used and the older ones are discarded. The date of the file is assumed to be 6 months after its first entry
     * (which explains why the file having August 2007 as its first entry is the February 2008 file). This implies that MSAFE
     * files must <em>not</em> be edited to change their time span, otherwise this would break the old entries overriding
     * mechanism.
     * </p>
     *
     * @param date date of the solar activity data
     *
     * @return date of the file
     */
    public DateComponents getFileDate(final AbsoluteDate date) {
        // Get the neighboring solar activity
        final LocalSolarActivity localSolarActivity = new LocalSolarActivity(date);
        final LineParameters     previousParam      = localSolarActivity.getPreviousParam();
        final LineParameters     currentParam       = localSolarActivity.getNextParam();

        // Choose which file date to return
        final double dtP = date.durationFrom(previousParam.getDate());
        final double dtC = currentParam.getDate().durationFrom(date);
        return (dtP < dtC) ? previousParam.getFileDate() : currentParam.getFileDate();
    }

    /**
     * The Kp index is derived from the Ap index.
     * <p>The method used is explained on <a
     * href="http://www.ngdc.noaa.gov/stp/GEOMAG/kp_ap.html"> NOAA website.</a> as follows:</p>
     * <p>The scale is 0 to 9 expressed in thirds of a unit, e.g. 5- is 4 2/3,
     * 5 is 5 and 5+ is 5 1/3. The ap (equivalent range) index is derived from the Kp index as follows:</p>
     * <table border="1">
     * <caption>Kp / Ap Conversion Table</caption>
     * <tbody>
     * <tr>
     * <td>Kp</td><td>0o</td><td>0+</td><td>1-</td><td>1o</td><td>1+</td><td>2-</td><td>2o</td><td>2+</td><td>3-</td><td>3o</td><td>3+</td><td>4-</td><td>4o</td><td>4+</td>
     * </tr>
     * <tr>
     * <td>ap</td><td>0</td><td>2</td><td>3</td><td>4</td><td>5</td><td>6</td><td>7</td><td>9</td><td>12</td><td>15</td><td>18</td><td>22</td><td>27</td><td>32</td>
     * </tr>
     * <tr>
     * <td>Kp</td><td>5-</td><td>5o</td><td>5+</td><td>6-</td><td>6o</td><td>6+</td><td>7-</td><td>7o</td><td>7+</td><td>8-</td><td>8o</td><td>8+</td><td>9-</td><td>9o</td>
     * </tr>
     * <tr>
     * <td>ap</td><td>39</td><td>48</td><td>56</td><td>67</td><td>80</td><td>94</td><td>111</td><td>132</td><td>154</td><td>179</td><td>207</td><td>236</td><td>300</td><td>400</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @param date date of the Kp data
     *
     * @return the 24H geomagnetic index
     */
    public double get24HoursKp(final AbsoluteDate date) {
        // get the daily Ap
        final double ap = getDailyAp(date);

        // get the corresponding Kp index from
        // equation 4 in [1] for Ap to Kp conversion
        return 1.89 * FastMath.asinh(0.154 * ap);
    }

    /** {@inheritDoc} */
    public double getDailyFlux(final AbsoluteDate date) {
        return getMeanFlux(date.shiftedBy(-Constants.JULIAN_DAY));
    }

    public double getAverageFlux(final AbsoluteDate date) {
        // Extract closest neighbours average
        final List<TimeStampedDouble> neighbors = averageFluxCache.getNeighbors(date).collect(Collectors.toList());

        // Create linear interpolating function
        final double[] x = new double[] { 0, 1 };
        final double[] y = neighbors.stream().map(TimeStampedDouble::getValue).mapToDouble(Double::doubleValue).toArray();

        final LinearInterpolator interpolator          = new LinearInterpolator();
        final UnivariateFunction interpolatingFunction = interpolator.interpolate(x, y);

        // Interpolate
        final AbsoluteDate previousDate = neighbors.get(0).getDate();
        final AbsoluteDate nextDate     = neighbors.get(1).getDate();
        return interpolatingFunction.value(date.durationFrom(previousDate) / nextDate.durationFrom(previousDate));
    }

    /** {@inheritDoc} */
    public double[] getAp(final AbsoluteDate date) {
        // Gets the AP for the current date
        final double ap = getDailyAp(date);

        // Retuns an array of Ap filled in with the daily Ap only
        return new double[] { ap, ap, ap, ap, ap, ap, ap };
    }

    /**
     * Gets the daily Ap index.
     *
     * @param date the current date
     *
     * @return the daily Ap index
     */
    private double getDailyAp(final AbsoluteDate date) {
        return getLinearInterpolation(date, LineParameters::getAp);
    }

    /**
     * Replace the instance with a data transfer object for serialization.
     *
     * @return data transfer object that will be serialized
     */
    @DefaultDataContext
    private Object writeReplace() {
        return new DataTransferObject(getSupportedNames(), strengthLevel);
    }

    /**
     * Get the strength level for activity.
     *
     * @return strength level to set
     */
    public StrengthLevel getStrengthLevel() {
        return strengthLevel;
    }

    /** Strength level of activity. */
    public enum StrengthLevel {

        /** Strong level of activity. */
        STRONG,

        /** Average level of activity. */
        AVERAGE,

        /** Weak level of activity. */
        WEAK

    }

    /** Generator generating average flux data between given dates. */
    private class AverageFluxGenerator implements TimeStampedGenerator<TimeStampedDouble> {

        /** {@inheritDoc} */
        @Override
        public List<TimeStampedDouble> generate(final AbsoluteDate existingDate, final AbsoluteDate date) {
            // No prior data in the cache
            if (existingDate == null) {
                return generateDataFromEarliestToLatestDates(getCurrentDay(date), getNextDay(date));
            }
            // Prior data in the cache, fill with data between date and existing date
            if (date.isBefore(existingDate)) {
                return generateDataFromEarliestToLatestDates(date, existingDate);
            }
            return generateDataFromEarliestToLatestDates(existingDate, date);
        }

        /**
         * Generate data from earliest to latest date.
         *
         * @param earliest earliest date
         * @param latest latest date
         *
         * @return data generated from earliest to latest date
         */
        private List<TimeStampedDouble> generateDataFromEarliestToLatestDates(final AbsoluteDate earliest,
                                                                              final AbsoluteDate latest) {
            final List<TimeStampedDouble> generated = new ArrayList<>();

            // Add next computed average until it brackets the latest date
            AbsoluteDate latestNeighbourDate = getCurrentDay(earliest);
            while (latestNeighbourDate.isBeforeOrEqualTo(latest)) {
                generated.add(computeAverageFlux(latestNeighbourDate));
                latestNeighbourDate = getNextDay(latestNeighbourDate);
            }
            return generated;
        }

        /**
         * Get the current day at midnight.
         *
         * @param date date
         *
         * @return current day at midnight.
         */
        private AbsoluteDate getCurrentDay(final AbsoluteDate date) {
            // Find previous day date time components
            final TimeScale      utc            = getUTC();
            final DateComponents dateComponents = date.getComponents(utc).getDate();

            // Create absolute date for previous day
            return new AbsoluteDate(new DateTimeComponents(dateComponents, TimeComponents.H00), utc);
        }

        /**
         * Get the next day at midnight.
         *
         * @param date date
         *
         * @return next day at midnight.
         */
        private AbsoluteDate getNextDay(final AbsoluteDate date) {
            // Find previous day date time components
            final TimeScale      utc               = getUTC();
            final DateComponents dateComponents    = date.getComponents(utc).getDate();
            final DateComponents shiftedComponents = new DateComponents(dateComponents, 1);

            // Create absolute date for previous day
            return new AbsoluteDate(new DateTimeComponents(shiftedComponents, TimeComponents.H00), utc);
        }

        /**
         * Compute the average flux for given absolute date.
         *
         * @param date date at which the average flux is desired
         *
         * @return average flux
         */
        private TimeStampedDouble computeAverageFlux(final AbsoluteDate date) {
            // Extract list of neighbors to compute average
            final TimeStampedGenerator<LineParameters> generator   = getCache().getGenerator();
            final AbsoluteDate                         initialDate = date.shiftedBy(-40 * Constants.JULIAN_DAY);
            final AbsoluteDate                         finalDate   = date.shiftedBy(40 * Constants.JULIAN_DAY);
            final List<LineParameters>                 monthlyData = generator.generate(initialDate, finalDate);

            // Create interpolator for given data
            final LinearInterpolator interpolator = new LinearInterpolator();

            final double[] x = monthlyData.stream().map(param -> param.getDate().durationFrom(initialDate))
                                          .mapToDouble(Double::doubleValue).toArray();
            final double[] y = monthlyData.stream().map(LineParameters::getF107).mapToDouble(Double::doubleValue).toArray();

            final UnivariateFunction interpolatingFunction = interpolator.interpolate(x, y);

            // Loops over the 81 days centered on the given date
            double average = 0;
            for (int i = -40; i < 41; i++) {
                average += interpolatingFunction.value(date.shiftedBy(i * Constants.JULIAN_DAY).durationFrom(initialDate));
            }

            // Returns the 81 day average flux
            return new TimeStampedDouble(average / 81, date);
        }
    }

    /** Internal class used only for serialization. */
    @DefaultDataContext
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = -5212198874900835369L;

        /** Regular expression that matches the names of the IONEX files. */
        private final String supportedNames;

        /** Selected strength level of activity. */
        private final StrengthLevel strengthLevel;

        /**
         * Simple constructor.
         *
         * @param supportedNames regular expression for supported files names
         * @param strengthLevel selected strength level of activity
         */
        DataTransferObject(final String supportedNames,
                           final StrengthLevel strengthLevel) {
            this.supportedNames = supportedNames;
            this.strengthLevel  = strengthLevel;
        }

        /**
         * Replace the deserialized data transfer object with a {@link MarshallSolarActivityFutureEstimation}.
         *
         * @return replacement {@link MarshallSolarActivityFutureEstimation}
         */
        private Object readResolve() {
            try {
                return new MarshallSolarActivityFutureEstimation(supportedNames, strengthLevel);
            }
            catch (OrekitException oe) {
                throw new OrekitInternalError(oe);
            }
        }
    }
}

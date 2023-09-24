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

import org.hipparchus.exception.DummyLocalizable;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.models.earth.atmosphere.DTM2000InputParameters;
import org.orekit.models.earth.atmosphere.NRLMSISE00InputParameters;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeStamped;
import org.orekit.utils.GenericTimeStampedCache;
import org.orekit.utils.ImmutableTimeStampedCache;
import org.orekit.utils.TimeStampedGenerator;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstract class for solar activity data.
 *
 * @param <L> type of the line parameters
 * @param <D> type of the solar activity data loader
 *
 * @author Vincent Cucchietti
 * @since 12.0
 */
public abstract class AbstractSolarActivityData<L extends AbstractSolarActivityDataLoader.LineParameters, D extends AbstractSolarActivityDataLoader<L>>
        implements DTM2000InputParameters, NRLMSISE00InputParameters {

    /** Size of the list. */
    protected static final int N_NEIGHBORS = 2;

    /** Serializable UID. */
    private static final long serialVersionUID = 8804818166227680449L;

    /** Weather data thread safe cache. */
    private final transient GenericTimeStampedCache<L> cache;

    /** Supported names. */
    private final String supportedNames;

    /** UTC time scale. */
    private final TimeScale utc;

    /** First available date. */
    private final AbsoluteDate firstDate;

    /** Last available date. */
    private final AbsoluteDate lastDate;

    /**
     * @param supportedNames regular expression for supported AGI/CSSI space weather files names
     * @param loader data loader
     * @param dataProvidersManager provides access to auxiliary data files.
     * @param utc UTC time scale
     * @param maxSlots maximum number of independent cached time slots in the
     * {@link GenericTimeStampedCache time-stamped cache}
     * @param maxSpan maximum duration span in seconds of one slot in the {@link GenericTimeStampedCache time-stamped cache}
     * @param maxInterval time interval above which a new slot is created in the
     * {@link GenericTimeStampedCache time-stamped cache}
     * @param minimumStep overriding minimum step designed for non-homogeneous tabulated values. To be used for example when
     * caching monthly tabulated values. May be null.
     */
    protected AbstractSolarActivityData(final String supportedNames, final D loader,
                                        final DataProvidersManager dataProvidersManager, final TimeScale utc,
                                        final int maxSlots, final double maxSpan, final double maxInterval,
                                        final double minimumStep) {
        // Load data
        dataProvidersManager.feed(supportedNames, loader);

        // Create thread safe cache
        this.cache = new GenericTimeStampedCache<>(N_NEIGHBORS, maxSlots, maxSpan, maxInterval,
                                                   new SolarActivityGenerator(loader.getDataSet()), minimumStep);

        // Initialise fields
        this.supportedNames = supportedNames;
        this.utc            = utc;
        this.firstDate      = loader.getMinDate();
        this.lastDate       = loader.getMaxDate();
    }

    /**
     * Simple constructor.
     *
     * @param source source for the data
     * @param loader data loader
     * @param utc UTC time scale
     * @param maxSlots maximum number of independent cached time slots in the
     * {@link GenericTimeStampedCache time-stamped cache}
     * @param maxSpan maximum duration span in seconds of one slot in the {@link GenericTimeStampedCache time-stamped cache}
     * @param maxInterval time interval above which a new slot is created in the
     * {@link GenericTimeStampedCache time-stamped cache}
     * @param minimumStep overriding minimum step designed for non-homogeneous tabulated values. To be used for example when
     * caching monthly tabulated values. May be null.
     *
     * @since 12.0
     */
    public AbstractSolarActivityData(final DataSource source, final D loader, final TimeScale utc, final int maxSlots,
                                     final double maxSpan, final double maxInterval, final double minimumStep) {
        try {
            // Load file
            try (InputStream is = source.getOpener().openStreamOnce();
                    BufferedInputStream bis = new BufferedInputStream(is)) {
                loader.loadData(bis, source.getName());
            }

            // Create thread safe cache
            this.cache = new GenericTimeStampedCache<>(N_NEIGHBORS, maxSlots, maxSpan, maxInterval,
                                                       new SolarActivityGenerator(loader.getDataSet()), minimumStep);

            // Initialise fields
            this.supportedNames = source.getName();
            this.utc            = utc;
            this.firstDate      = loader.getMinDate();
            this.lastDate       = loader.getMaxDate();
        }
        catch (IOException | ParseException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /**
     * Performs a linear interpolation between two values The weights are computed from the time delta between previous date,
     * current date, next date.
     *
     * @param date current date
     * @param solarActivityToDoubleMapper mapping function taking solar activity as input and returning a double
     *
     * @return the value interpolated for the current date
     */
    protected double getLinearInterpolation(final AbsoluteDate date, final Function<L, Double> solarActivityToDoubleMapper) {
        // Create solar activity around current date
        final LocalSolarActivity localSolarActivity = new LocalSolarActivity(date);

        // Extract values
        return getLinearInterpolation(localSolarActivity, solarActivityToDoubleMapper);
    }

    /**
     * Performs a linear interpolation between two values The weights are computed from the time delta between previous date,
     * current date, next date.
     *
     * @param localSolarActivity solar activity around current date
     * @param solarActivityToDoubleMapper mapping function taking solar activity as input and returning a double
     *
     * @return the value interpolated for the current date
     */
    protected double getLinearInterpolation(final LocalSolarActivity localSolarActivity,
                                            final Function<L, Double> solarActivityToDoubleMapper) {
        // Extract values
        final L      previousParameters = localSolarActivity.getPreviousParam();
        final double previousValue      = solarActivityToDoubleMapper.apply(previousParameters);

        final L      nextParameters = localSolarActivity.getNextParam();
        final double nextValue      = solarActivityToDoubleMapper.apply(nextParameters);

        // Perform a linear interpolation
        final AbsoluteDate previousDate   = localSolarActivity.getPreviousParam().getDate();
        final AbsoluteDate currentDate    = localSolarActivity.getNextParam().getDate();
        final double       dt             = currentDate.durationFrom(previousDate);
        final AbsoluteDate date           = localSolarActivity.getDate();
        final double       previousWeight = currentDate.durationFrom(date) / dt;
        final double       nextWeight     = date.durationFrom(previousDate) / dt;

        // Returns the data interpolated at the date
        return previousValue * previousWeight + nextValue * nextWeight;
    }

    /**
     * Get underlying cache.
     *
     * @return cache
     */
    public GenericTimeStampedCache<L> getCache() {
        return cache;
    }

    /**
     * Get the supported names regular expression.
     *
     * @return the supported names.
     */
    public String getSupportedNames() {
        return supportedNames;
    }

    /**
     * Get the UTC timescale.
     *
     * @return UTC timescale
     */
    public TimeScale getUTC() {
        return utc;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getMinDate() {
        return firstDate;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getMaxDate() {
        return lastDate;
    }

    /** Container for weather parameters around current date. Allows for thread safe use. */
    protected class LocalSolarActivity implements TimeStamped {

        /** Date. */
        private final AbsoluteDate currentDate;

        /** Previous parameters. */
        private final L previousParam;

        /** Next parameters. */
        private final L nextParam;

        /**
         * Constructor.
         *
         * @param date current date
         */
        public LocalSolarActivity(final AbsoluteDate date) {
            // Asked date is before earliest available data
            if (date.durationFrom(firstDate) < 0) {
                throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_BEFORE, date, firstDate, lastDate,
                                          firstDate.durationFrom(date));
            }
            // Asked date is after latest available data
            if (date.durationFrom(lastDate) > 0) {
                throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_AFTER, date, firstDate, lastDate,
                                          date.durationFrom(lastDate));
            }

            final List<L> neighbours = cache.getNeighbors(date).collect(Collectors.toList());

            this.currentDate   = date;
            this.previousParam = neighbours.get(0);
            this.nextParam     = neighbours.get(1);
        }

        /** @return current date */
        public AbsoluteDate getDate() {
            return currentDate;
        }

        /** @return previous parameters */
        public L getPreviousParam() {
            return previousParam;
        }

        /** @return next parameters */
        public L getNextParam() {
            return nextParam;
        }
    }

    /** Generator used in the weather data cache. */
    protected class SolarActivityGenerator implements TimeStampedGenerator<L> {

        /**
         * Default time step to shift the date.
         * <p>
         * It is used so that, in the case where the earliest date is exactly at noon, we do not get the following interval
         * [previous day; current day] but rather the expected interval [current day; next day]
         */
        private static final double STEP = 1;

        /** Data set. */
        private final ImmutableTimeStampedCache<L> data;

        /**
         * Constructor.
         *
         * @param dataSet weather data
         */
        protected SolarActivityGenerator(final Collection<L> dataSet) {
            this.data = new ImmutableTimeStampedCache<>(N_NEIGHBORS, dataSet);
        }

        /** {@inheritDoc} */
        @Override
        public List<L> generate(final AbsoluteDate existingDate, final AbsoluteDate date) {
            // No prior data in the cache
            if (existingDate == null) {
                return data.getNeighbors(date).collect(Collectors.toList());
            }
            // Prior data in the cache, fill with data between date and existing date
            if (date.isBefore(existingDate)) {
                return generateDataFromEarliestToLatestDates(date, existingDate);
            }
            return generateDataFromEarliestToLatestDates(existingDate, date);
        }

        /**
         * Generate a list of parameters between earliest and latest dates.
         *
         * @param earliest earliest date
         * @param latest latest date
         *
         * @return list of parameters between earliest and latest dates
         */
        public List<L> generateDataFromEarliestToLatestDates(final AbsoluteDate earliest, final AbsoluteDate latest) {
            /* Gives first two parameters bracketing the earliest date
             * A date shifted by step is used so that, in the case where the earliest date is exactly noon, we do not get the
             * following interval [previous day; current day] but rather the expected interval [current day; next day] */
            List<L> neighbours = data.getNeighbors(earliest.shiftedBy(STEP)).collect(Collectors.toList());

            // Get next parameter until it brackets the latest date
            AbsoluteDate  latestNeighbourDate = neighbours.get(1).getDate();
            final List<L> params              = new ArrayList<>(neighbours);
            while (latestNeighbourDate.isBefore(latest)) {
                neighbours = data.getNeighbors(latestNeighbourDate.shiftedBy(STEP)).collect(Collectors.toList());
                params.add(neighbours.get(1));
                latestNeighbourDate = neighbours.get(1).getDate();
            }
            return params;
        }
    }
}

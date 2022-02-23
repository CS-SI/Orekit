/* Copyright 2002-2022 CS GROUP
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

import java.util.List;
import java.util.stream.Collectors;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.AbstractSelfFeedingLoader;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.models.earth.atmosphere.JB2008InputParameters;
import org.orekit.models.earth.atmosphere.data.SOLFSMYDataLoader.LineParameters;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.ImmutableTimeStampedCache;

/**
 * This class provides a container for the solar indices data required by the JB2008
 * atmospheric model. This container only stores information provided in the SOLFSMY text file
 * provided by Space Environment Technologies, therefore providing neither the temperature correction
 * available in the DST and DTC files, nor the geomagnetic storm indices available in the SOLRESAP file.
 * The {@link org.orekit.data.DataLoader} implementation and the parsing is handled by
 * the class {@link SOLFSMYDataLoader}.
 * <p>
 * The data is available on Space Environment Technologies' website at
 * <a href="http://sol.spacenvironment.net/jb2008"></a>.
 *
 * The work done for this class is based on the CssiSpaceWeatherData class
 * by Clément Jonglez, the JB2008 interface by Pascal Parraud, and corrections for
 * the CssiSpaceWeatherData implementation by Bryan Cazabonne and Evan Ward.
 *
 * @author Louis Aucouturier
 * @since 11.2
 */
public class JB2008SpaceWeatherData extends AbstractSelfFeedingLoader
    implements JB2008InputParameters {

    /** Default regular expression for supported names that works with all officially published files. */
    public static final String DEFAULT_SUPPORTED_NAMES = "(SOLFSMY)(.*)((\\.txt)|(\\.TXT))";

    /** Serializable UID. */
    private static final long serialVersionUID = 7735042547323407578L;

    /** Size of the list. */
    private static final int N_NEIGHBORS = 2;

    /** Data set. */
    private final transient ImmutableTimeStampedCache<LineParameters> data;

    /** First available date. */
    private final AbsoluteDate firstDate;

    /** Last available date. */
    private final AbsoluteDate lastDate;

    /** Previous set of solar activity parameters. */
    private LineParameters previousParam;

    /** Current set of solar activity parameters. */
    private LineParameters nextParam;

    /**
     * Simple constructor. This constructor uses the default data context.
     * <p>
     * The original file names is SOLFSMY.TXT
     * A regular expression is to be computed in order to take into account
     * various filenames derived from the original one.
     * </p>
     *
     * @param supportedNames regular expression for SOLFSMY space weather files names
     * with variations allowed between SOLFSMY and the file extension.
     */
    @DefaultDataContext
    public JB2008SpaceWeatherData(final String supportedNames) {
        this(supportedNames, DataContext.getDefault().getDataProvidersManager(),
                DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Constructor that allows specifying the source of the SOLFSMY space weather
     * file.
     *
     * @param supportedNames regular expression for SOLFSMY space weather files names
     * with variations allowed between SOLFSMY and the file extension.
     * @param dataProvidersManager provides access to auxiliary data files.
     * @param utc                  UTC time scale.
     */
    public JB2008SpaceWeatherData(final String supportedNames,
            final DataProvidersManager dataProvidersManager,
            final TimeScale utc) {
        super(supportedNames, dataProvidersManager);

        final SOLFSMYDataLoader loader =
                new SOLFSMYDataLoader(utc);
        this.feed(loader);
        data =
                new ImmutableTimeStampedCache<>(N_NEIGHBORS, loader.getDataSet());
        firstDate = loader.getMinDate();
        lastDate = loader.getMaxDate();
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
        if (date.durationFrom(firstDate) < 0) {
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_BEFORE,
                    date, firstDate, lastDate, firstDate.durationFrom(date));
        }
        if (date.durationFrom(lastDate) > 0) {
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_AFTER,
                    date, firstDate, lastDate, date.durationFrom(lastDate));
        }

        // don't search if the cached selection is fine
        if (previousParam != null && date.durationFrom(previousParam.getDate()) > 0 &&
                date.durationFrom(nextParam.getDate()) <= 0) {
            return;
        }

        final List<LineParameters> neigbors = data.getNeighbors(date).collect(Collectors.toList());
        previousParam = neigbors.get(0);
        nextParam = neigbors.get(1);
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
    public double getF10(final AbsoluteDate date) {
        bracketDate(date);
        return getLinearInterpolation(date, previousParam.getF10(), nextParam.getF10());
    }

    /** {@inheritDoc} */
    public double getF10B(final AbsoluteDate date) {
        bracketDate(date);
        return getLinearInterpolation(date, previousParam.getF10B(), nextParam.getF10B());
    }

    /** {@inheritDoc} */
    public double getS10(final AbsoluteDate date) {
        bracketDate(date);
        return getLinearInterpolation(date, previousParam.getS10(), nextParam.getS10());
    }

    /** {@inheritDoc} */
    public double getS10B(final AbsoluteDate date) {
        bracketDate(date);
        return getLinearInterpolation(date, previousParam.getS10B(), nextParam.getS10B());
    }

    /** {@inheritDoc} */
    public double getXM10(final AbsoluteDate date) {
        bracketDate(date);
        return getLinearInterpolation(date, previousParam.getXM10(), nextParam.getXM10());
    }

    /** {@inheritDoc} */
    public double getXM10B(final AbsoluteDate date) {
        bracketDate(date);
        return getLinearInterpolation(date, previousParam.getXM10B(), nextParam.getXM10B());
    }

    /** {@inheritDoc} */
    public double getY10(final AbsoluteDate date) {
        bracketDate(date);
        return getLinearInterpolation(date, previousParam.getY10(), nextParam.getY10());
    }

    /** {@inheritDoc} */
    public double getY10B(final AbsoluteDate date) {
        bracketDate(date);
        return getLinearInterpolation(date, previousParam.getY10B(), nextParam.getY10B());
    }

    /** {@inheritDoc} */
    public double getDSTDTC(final AbsoluteDate date) {
        // TODO Currently returns 0
        // DSTDTC class to be defined
        return 0;
    }


    public String getSupportedNames() {
        return super.getSupportedNames();
    }
}

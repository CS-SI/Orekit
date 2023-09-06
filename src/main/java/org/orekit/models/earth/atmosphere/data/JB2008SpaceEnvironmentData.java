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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

import org.hipparchus.exception.DummyLocalizable;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.models.earth.atmosphere.JB2008InputParameters;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;
import org.orekit.utils.ImmutableTimeStampedCache;


/**
 * This class provides a container for the solar indices data required by the JB2008
 * atmospheric model. This container only stores information provided in the SOLFSMY and DTCFILE text file
 * provided by Space Environment Technologies. Therefore it doesn't provide  the geomagnetic storm
 * indices available in the SOLRESAP file.
 * The {@link org.orekit.data.DataLoader} implementations and the parsing are handled by
 * the {@link SOLFSMYDataLoader} {@link DtcDataLoader} classes.
 * <p>
 * Data are available on Space Environment Technologies'
 * <a href="http://sol.spacenvironment.net/jb2008">website</a>.
 *
 * The work done for this class is based on the CssiSpaceWeatherData class
 * by Clément Jonglez, the JB2008 interface by Pascal Parraud, and corrections for
 * the CssiSpaceWeatherData implementation by Bryan Cazabonne and Evan Ward.
 *
 * @author Louis Aucouturier
 * @since 11.2
 */
public class JB2008SpaceEnvironmentData implements JB2008InputParameters {

    /** Default regular expression for supported names that works with test and published files for the SOLFSMY file. */
    public static final String DEFAULT_SUPPORTED_NAMES_SOLFSMY = "(SOLFSMY)(.*)((\\.txt)|(\\.TXT))";

    /** Default regular expression for supported names that works with test and published files for the DTCFILE file. */
    public static final String DEFAULT_SUPPORTED_NAMES_DTC = "DTCFILE.TXT";

    /** Serializable UID. */
    private static final long serialVersionUID = 7735042547323407578L;

    /** Size of the list. */
    private static final int N_NEIGHBORS = 2;

    /** Data set for SOLFSMY file. */
    private final transient ImmutableTimeStampedCache<SOLFSMYDataLoader.LineParameters> dataSOL;

    /** Data set for DTCFILE file. */
    private final transient ImmutableTimeStampedCache<DtcDataLoader.LineParameters> dataDTC;

    /** First available date. */
    private final AbsoluteDate firstDate;

    /** Last available date. */
    private final AbsoluteDate lastDate;

    /** Previous set of solar activity parameters. */
    private SOLFSMYDataLoader.LineParameters previousParamSOL;

    /** Current set of solar activity parameters. */
    private SOLFSMYDataLoader.LineParameters nextParamSOL;

    /** Previous set of solar activity parameters. */
    private DtcDataLoader.LineParameters previousParamDTC;

    /** Current set of solar activity parameters. */
    private DtcDataLoader.LineParameters nextParamDTC;

    /**
     * Simple constructor. This constructor uses the default data context.
     *
     * @param supportedNamesSOL regular expression for SOLFSMY space weather files names
     * with variations allowed between SOLFSMY and the file extension.
     * @param supportedNamesDTC regular expression for DTCFILE files names
     * with variations allowed between DTCFILE and the file extension.
     */
    @DefaultDataContext
    public JB2008SpaceEnvironmentData(final String supportedNamesSOL, final String supportedNamesDTC) {
        this(supportedNamesSOL, supportedNamesDTC, DataContext.getDefault().getDataProvidersManager(),
                DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Constructor that allows specifying the source of the SOLFSMY space weather
     * file.
     * This constructor takes a supplementary argument, the supported names for DTCFILE,
     * in order to setup the second loader.
     *
     * @param supportedNamesSOL regular expression for SOLFSMY space weather files names
     * with variations allowed between SOLFSMY and the file extension.
     * @param supportedNamesDTC regular expression for DTCFILE files names
     * with variations allowed between DTCFILE and the file extension.
     * @param dataProvidersManager provides access to auxiliary data files.
     * @param utc                  UTC time scale.
     */
    public JB2008SpaceEnvironmentData(final String supportedNamesSOL,
                                      final String supportedNamesDTC,
                                      final DataProvidersManager dataProvidersManager,
                                      final TimeScale utc) {

        // Load SOLFSMY data
        final SOLFSMYDataLoader loaderSOL = new SOLFSMYDataLoader(utc);
        dataProvidersManager.feed(supportedNamesSOL, loaderSOL);
        dataSOL = new ImmutableTimeStampedCache<>(N_NEIGHBORS, loaderSOL.getDataSet());

        // Load DTC data
        final DtcDataLoader loaderDTC = new DtcDataLoader(utc);
        dataProvidersManager.feed(supportedNamesDTC, loaderDTC);
        dataDTC = new ImmutableTimeStampedCache<>(N_NEIGHBORS, loaderDTC.getDataSet());

        // Because the two files are generated by the same organism,
        // the first and last epochs are identical between the two files
        firstDate = loaderSOL.getMinDate();
        lastDate  = loaderSOL.getMaxDate();

    }

    /**
     * Simple constructor. This constructor uses the {@link DataContext#getDefault()
     * default data context}.
     * @param sourceSolfsmy source for the SOLFSMY data
     * @param sourceDtc     source for the DTC data
     * @since 12.0
     */
    @DefaultDataContext
    public JB2008SpaceEnvironmentData(final DataSource sourceSolfsmy,
                                      final DataSource sourceDtc) {
        this(sourceSolfsmy, sourceDtc, DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Constructor that allows specifying the source of the SOLFSMY space weather
     * file.
     * This constructor takes a supplementary argument, the source for DTCFILE,
     * in order to setup the second loader.
     *
     * @param sourceSolfsmy source for the SOLFSMY data
     * @param sourceDtc     source for the DTC data
     * @param utc           UTC time scale
     * @since 12.0
     */
    public JB2008SpaceEnvironmentData(final DataSource sourceSolfsmy,
                                      final DataSource sourceDtc,
                                      final TimeScale utc) {
        try {

            // Load SOLFSMY data
            final SOLFSMYDataLoader loaderSOL = new SOLFSMYDataLoader(utc);
            try (InputStream is = sourceSolfsmy.getOpener().openStreamOnce();
                 BufferedInputStream bis = new BufferedInputStream(is)) {
                loaderSOL.loadData(bis, sourceSolfsmy.getName());
            }

            // Load DTC data
            final DtcDataLoader loaderDTC = new DtcDataLoader(utc);
            try (InputStream is = sourceDtc.getOpener().openStreamOnce();
                 BufferedInputStream bis = new BufferedInputStream(is)) {
                loaderDTC.loadData(bis, sourceDtc.getName());
            }

            // Initialise fields
            dataSOL = new ImmutableTimeStampedCache<>(N_NEIGHBORS, loaderSOL.getDataSet());
            dataDTC = new ImmutableTimeStampedCache<>(N_NEIGHBORS, loaderDTC.getDataSet());
            // Because the two files are generated by the same organism,
            // the first and last epochs are identical between the two files
            firstDate = loaderSOL.getMinDate();
            lastDate  = loaderSOL.getMaxDate();

        } catch (IOException | ParseException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }

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
     * Find the data bracketing a specified date in dataSOL.
     *
     * @param date date to bracket
     */
    private void bracketDateSOL(final AbsoluteDate date) {
        /**
         * The presence of the shift in dates for checks on the validity of dates
         * is here to enforce the lag on the parameters (5-day lag max for Y10 parameters).
         * This lag is already present in the date parameter.
         */
        final AbsoluteDate firstDateUsefulSOL = firstDate.shiftedBy(-5 * Constants.JULIAN_DAY);
        if (date.durationFrom(firstDateUsefulSOL) < 0) {
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_BEFORE,
                    date, firstDateUsefulSOL, lastDate, firstDateUsefulSOL.durationFrom(date));
        }
        if (date.durationFrom(lastDate) > 0) {
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_AFTER,
                    date, firstDateUsefulSOL, lastDate, date.durationFrom(lastDate));
        }

        // don't search if the cached selection is fine
        if (previousParamSOL != null && date.durationFrom(previousParamSOL.getDate()) > 0 &&
                date.durationFrom(nextParamSOL.getDate()) <= 0) {
            return;
        }

        final List<SOLFSMYDataLoader.LineParameters> neigbors = dataSOL.getNeighbors(date).collect(Collectors.toList());
        previousParamSOL = neigbors.get(0);
        nextParamSOL = neigbors.get(1);

    }

    /**
     * Find the data bracketing a specified date in dataDTC.
     *
     * @param date date to bracket
     */
    private void bracketDateDTC(final AbsoluteDate date) {
        // No data lag
        final AbsoluteDate firstDateUsefulDTC = firstDate;
        if (date.durationFrom(firstDateUsefulDTC) < 0) {
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_BEFORE,
                    date, firstDateUsefulDTC, lastDate, firstDateUsefulDTC.durationFrom(date));
        }
        if (date.durationFrom(lastDate) > 0) {
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_AFTER,
                    date, firstDateUsefulDTC, lastDate, date.durationFrom(lastDate));
        }

        // don't search if the cached selection is fine
        if (previousParamDTC != null && date.durationFrom(previousParamDTC.getDate()) > 0 &&
                date.durationFrom(nextParamDTC.getDate()) <= 0) {
            return;
        }

        final List<DtcDataLoader.LineParameters> neigbors = dataDTC.getNeighbors(date).collect(Collectors.toList());
        previousParamDTC = neigbors.get(0);
        nextParamDTC = neigbors.get(1);

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
    private double getLinearInterpolationSOL(final AbsoluteDate date, final double previousValue, final double nextValue) {
        // perform a linear interpolation
        return linearInterpolation(date, previousValue, previousParamSOL.getDate(), nextValue, nextParamSOL.getDate());
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
    private double getLinearInterpolationDTC(final AbsoluteDate date, final double previousValue, final double nextValue) {
        // perform a linear interpolation
        return linearInterpolation(date, previousValue, previousParamDTC.getDate(), nextValue, nextParamDTC.getDate());
    }

    /**
     * Linear interpolation.
     * @param date the current date
     * @param previousValue the value at previous date
     * @param previousDate the previous date
     * @param nextValue the value at next date
     * @param nextDate the next date
     * @return the inteprolated value
     */
    private double linearInterpolation(final AbsoluteDate date,
                                       final double previousValue, final AbsoluteDate previousDate,
                                       final double nextValue, final AbsoluteDate nextDate) {
        // perform a linear interpolation
        final double dt = nextDate.durationFrom(previousDate);
        final double previousWeight = nextDate.durationFrom(date) / dt;
        final double nextWeight = date.durationFrom(previousDate) / dt;

        // returns the data interpolated at the date
        return previousValue * previousWeight + nextValue * nextWeight;
    }

    /** {@inheritDoc} */
    public double getF10(final AbsoluteDate date) {
        // The date is shifted by 1 day as described in the JB2008 Model with a 1-day lag.
        final AbsoluteDate workDate = date.shiftedBy(-Constants.JULIAN_DAY);
        bracketDateSOL(workDate);
        return getLinearInterpolationSOL(workDate, previousParamSOL.getF10(), nextParamSOL.getF10());
    }

    /** {@inheritDoc} */
    public double getF10B(final AbsoluteDate date) {
        // The date is shifted by 1 day as described in the JB2008 Model with a 1-day lag.
        final AbsoluteDate workDate = date.shiftedBy(-Constants.JULIAN_DAY);
        bracketDateSOL(workDate);
        return getLinearInterpolationSOL(workDate, previousParamSOL.getF10B(), nextParamSOL.getF10B());
    }

    /** {@inheritDoc} */
    public double getS10(final AbsoluteDate date) {
        // The date is shifted by 1 day as described in the JB2008 Model with a 1-day lag.
        final AbsoluteDate workDate = date.shiftedBy(-Constants.JULIAN_DAY);
        bracketDateSOL(workDate);
        return getLinearInterpolationSOL(workDate, previousParamSOL.getS10(), nextParamSOL.getS10());
    }

    /** {@inheritDoc} */
    public double getS10B(final AbsoluteDate date) {
        // The date is shifted by 1 day as described in the JB2008 Model with a 1-day lag.
        final AbsoluteDate workDate = date.shiftedBy(-Constants.JULIAN_DAY);
        bracketDateSOL(workDate);
        return getLinearInterpolationSOL(workDate, previousParamSOL.getS10B(), nextParamSOL.getS10B());
    }

    /** {@inheritDoc} */
    public double getXM10(final AbsoluteDate date) {
        // The date is shifted by 2 day as described in the JB2008 Model with a 2-day lag.
        final AbsoluteDate workDate = date.shiftedBy(-2.0 * Constants.JULIAN_DAY);
        bracketDateSOL(workDate);
        return getLinearInterpolationSOL(workDate, previousParamSOL.getXM10(), nextParamSOL.getXM10());
    }

    /** {@inheritDoc} */
    public double getXM10B(final AbsoluteDate date) {
        // The date is shifted by 2 day as described in the JB2008 Model with a 2-day lag.
        final AbsoluteDate workDate = date.shiftedBy(-2.0 * Constants.JULIAN_DAY);
        bracketDateSOL(workDate);;
        return getLinearInterpolationSOL(workDate, previousParamSOL.getXM10B(), nextParamSOL.getXM10B());
    }

    /** {@inheritDoc} */
    public double getY10(final AbsoluteDate date) {
        // The date is shifted by 5 day as described in the JB2008 Model with a 5-day lag.
        final AbsoluteDate workDate = date.shiftedBy(-5.0 * Constants.JULIAN_DAY);
        bracketDateSOL(workDate);
        return getLinearInterpolationSOL(workDate, previousParamSOL.getY10(), nextParamSOL.getY10());
    }

    /** {@inheritDoc} */
    public double getY10B(final AbsoluteDate date) {
        // The date is shifted by 5 day as described in the JB2008 Model with a 5-day lag.
        final AbsoluteDate workDate = date.shiftedBy(-5.0 * Constants.JULIAN_DAY);
        bracketDateSOL(workDate);
        return getLinearInterpolationSOL(workDate, previousParamSOL.getY10B(), nextParamSOL.getY10B());
    }

    /** {@inheritDoc} */
    public double getDSTDTC(final AbsoluteDate date) {
        bracketDateDTC(date);
        return getLinearInterpolationDTC(date, previousParamDTC.getDSTDTC(), nextParamDTC.getDSTDTC());
    }

}

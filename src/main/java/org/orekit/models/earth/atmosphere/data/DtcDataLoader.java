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
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.Set;

import org.orekit.data.DataLoader;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeStamped;
import org.orekit.utils.Constants;


/**
 * This class reads solar activity data from DTCFILE files for the class
 * {@link JB2008SpaceEnvironmentData}. The code in this class is based of the
 * CssiSpaceWeatherDataLoader class.
 * The DTCFILE file contain pre-computed data from Space Environment using the Dst indices
 * as well as Ap indices. This computation can be realised using the Fortran code provided
 * by Space Environment Technologies. See <a href="https://sol.spacenvironment.net/JB2008/indices/DTCFILE.TXT">
 * this link</a> for more information.
 * <p>
 * The data is provided by Space Environment Technologies through their website
 * <a href="https://sol.spacenvironment.net/JB2008/indices/DTCFILE.TXT">Link</a>.
 * </p>
 * The work done for this class is based on the CssiSpaceWeatherDataLoader class
 * by Clément Jonglez, the JB2008 interface by Pascal Parraud, and corrections for
 * DataLoader implementation by Bryan Cazabonne and Evan Ward .
 *
 * @author Louis Aucouturier
 * @since 11.2
 */
public class DtcDataLoader implements DataLoader {

    /** Container class for Solar activity indexes. */
    public static class LineParameters implements TimeStamped, Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 8239275953453087629L;

        /** Entry date. */
        private final AbsoluteDate date;

        /** dTc temperature correction data. */
        private final double dtc;

        /**
         * Constructor.
         * @param date  entry date
         * @param dtc   Temperature correction for geomagnetic storms
         */
        public LineParameters(final AbsoluteDate date, final double dtc) {
            this.date = date;
            this.dtc = dtc;
        }

        @Override
        public AbsoluteDate getDate() {
            return date;
        }

        /**
         * Get the DSTDTC parameter.
         * <p>
         * It represents the temperature correction for geomagnetic storms.
         * </p>
         * @return dTc  Temperature correction for geomagnetic storms
         */
        public double getDSTDTC() {
            return dtc;
        }

    }

    /** Pattern for regular data. */
    private static final Pattern PATTERN_SPACE = Pattern.compile("\\s+");

    /** UTC time scale. */
    private final TimeScale utc;

    /** First available date. */
    private AbsoluteDate firstDate;

    /** Last available date. */
    private AbsoluteDate lastDate;

    /** Data set. */
    private SortedSet<LineParameters> set;

    /**
     * Constructor.
     * @param utc UTC time scale
     */
    public DtcDataLoader(final TimeScale utc) {
        this.utc = utc;
        firstDate = null;
        lastDate = null;
        set = new TreeSet<>(new ChronologicalComparator());
    }

    /**
     * Getter for the data set.
     * @return the data set
     */
    public SortedSet<LineParameters> getDataSet() {
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

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
            throws IOException, ParseException, OrekitException {

        int lineNumber = 0;
        String line = null;
        final int nHours = 24;
        final Set<AbsoluteDate> parsedEpochs = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

            final CommonLineReader reader = new CommonLineReader(br);

            for (line = reader.readLine(); line != null; line = reader.readLine()) {
                lineNumber++;
                if (!reader.isEmptyLine()) {
                    /** extract the data from the line
                     * The data is extracted from substrings as the spacing between
                     * columns is constant.
                     */

                    /**
                     * The date is expressed as a year and the day-number in this year.
                     * Then the dTc is expressed in each column at a different hour, with
                     * column 4 being the first hour of  the day and column 28 the last hour
                     * of the day.
                     * Each column is converted to a single LineParameters object.
                     */

                    final String[] splitLine = PATTERN_SPACE.split(line);
                    final int year = Integer.parseInt(splitLine[1]);
                    final int dayYear = Integer.parseInt(splitLine[2]);
                    final AbsoluteDate initDate = new AbsoluteDate(year, 1, 1, utc);
                    final AbsoluteDate currDate = initDate.shiftedBy((dayYear - 1) * Constants.JULIAN_DAY);

                    for (int i = 0; i < nHours; i++) {

                        final AbsoluteDate date = currDate.shiftedBy(3600 * i);

                        if (parsedEpochs.add(date)) { // Checking if entry doesn't exist yet
                            final double dtc = Integer.parseInt(splitLine[3 + i]);
                            set.add(new LineParameters(date, dtc));
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

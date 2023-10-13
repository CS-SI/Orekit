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

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.models.earth.atmosphere.data.MarshallSolarActivityFutureEstimation.StrengthLevel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.DateComponents;
import org.orekit.time.Month;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeStamped;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class reads solar activity data needed by atmospheric models: F107 solar flux, Ap and Kp indexes.
 * <p>
 * The data are retrieved through the NASA Marshall Solar Activity Future Estimation (MSAFE) as estimates of monthly F10.7
 * Mean solar flux and Ap geomagnetic parameter. The data can be retrieved at the NASA <a
 * href="https://www.nasa.gov/msfcsolar/archivedforecast"> Marshall Solar Activity website</a>. Here Kp indices are deduced
 * from Ap indexes, which in turn are tabulated equivalent of retrieved Ap values.
 * </p>
 * <p>
 * If several MSAFE files are available, some dates may appear in several files (for example August 2007 is in all files from
 * the first one published in March 1999 to the February 2008 file). In this case, the data from the most recent file is used
 * and the older ones are discarded. The date of the file is assumed to be 6 months after its first entry (which explains why
 * the file having August 2007 as its first entry is the February 2008 file). This implies that MSAFE files must <em>not</em>
 * be edited to change their time span, otherwise this would break the old entries overriding mechanism.
 * </p>
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
public class MarshallSolarActivityFutureEstimationLoader
        extends AbstractSolarActivityDataLoader<MarshallSolarActivityFutureEstimationLoader.LineParameters> {

    /** Pattern for the data fields of MSAFE data. */
    private final Pattern dataPattern;

    /** Data set. */
    private final SortedSet<TimeStamped> data;

    /** Selected strength level of activity. */
    private final StrengthLevel strengthLevel;

    /**
     * Simple constructor. This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param strengthLevel selected strength level of activity
     */
    @DefaultDataContext
    public MarshallSolarActivityFutureEstimationLoader(final StrengthLevel strengthLevel) {
        this(strengthLevel, DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Constructor that allows specifying the source of the MSAFE auxiliary data files.
     *
     * @param strengthLevel selected strength level of activity
     * @param utc UTC time scale.
     *
     * @since 10.1
     */
    public MarshallSolarActivityFutureEstimationLoader(final StrengthLevel strengthLevel, final TimeScale utc) {
        super(utc);

        this.data          = new TreeSet<>(new ChronologicalComparator());
        this.strengthLevel = strengthLevel;

        // the data lines have the following form:
        // 2010.5003   JUL    83.4      81.3      78.7       6.4       5.9       5.2
        // 2010.5837   AUG    87.3      83.4      78.5       7.0       6.1       4.9
        // 2010.6670   SEP    90.8      85.5      79.4       7.8       6.2       4.7
        // 2010.7503   OCT    94.2      87.6      80.4       9.1       6.4       4.9
        final StringBuilder builder = new StringBuilder("^");

        // first group: year
        builder.append("\\p{Blank}*(\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit})");

        // month as fraction of year, not stored in a group
        builder.append("\\.\\p{Digit}+");

        // second group: month as a three upper case letters abbreviation
        builder.append("\\p{Blank}+(");
        for (final Month month : Month.values()) {
            builder.append(month.getUpperCaseAbbreviation());
            builder.append('|');
        }
        builder.delete(builder.length() - 1, builder.length());
        builder.append(")");

        // third to eighth group: data fields
        for (int i = 0; i < 6; ++i) {
            builder.append("\\p{Blank}+([-+]?[0-9]+\\.[0-9]+)");
        }

        // end of line
        builder.append("\\p{Blank}*$");

        // compile the pattern
        this.dataPattern = Pattern.compile(builder.toString());

    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
            throws IOException, ParseException, OrekitException {

        // select the groups we want to store
        final int f107Group;
        final int apGroup;
        switch (strengthLevel) {
            case STRONG:
                f107Group = 3;
                apGroup = 6;
                break;
            case AVERAGE:
                f107Group = 4;
                apGroup = 7;
                break;
            default:
                f107Group = 5;
                apGroup = 8;
                break;
        }

        boolean        inData   = false;
        DateComponents fileDate = null;

        // try to read the data
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

            // Go through each line
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                line = line.trim();
                if (!line.isEmpty()) {
                    final Matcher matcher = dataPattern.matcher(line);
                    if (matcher.matches()) {

                        // We are in the data section
                        inData = true;

                        // Extract the data from the line
                        final int          year  = Integer.parseInt(matcher.group(1));
                        final Month        month = Month.parseMonth(matcher.group(2));
                        final AbsoluteDate date  = new AbsoluteDate(year, month, 1, getUTC());
                        if (fileDate == null) {
                            /* The first entry of each file correspond exactly to 6 months before file publication,
                            so we compute the file date by adding 6 months to its first entry */
                            if (month.getNumber() > 6) {
                                fileDate = new DateComponents(year + 1, month.getNumber() - 6, 1);
                            } else {
                                fileDate = new DateComponents(year, month.getNumber() + 6, 1);
                            }
                        }

                        // check if there is already an entry for this date or not
                        boolean                     addEntry = false;
                        final Iterator<TimeStamped> iterator = data.tailSet(date).iterator();
                        if (iterator.hasNext()) {
                            final LineParameters existingEntry = (LineParameters) iterator.next();
                            if (existingEntry.getDate().equals(date)) {
                                // there is an entry for this date
                                if (existingEntry.getFileDate().compareTo(fileDate) < 0) {
                                    // the entry was read from an earlier file
                                    // we replace it with the new entry as it is fresher
                                    iterator.remove();
                                    addEntry = true;
                                }
                            } else {
                                // it is the first entry we get for this date
                                addEntry = true;
                            }
                        } else {
                            // it is the first entry we get for this date
                            addEntry = true;
                        }
                        if (addEntry) {
                            // we must add the new entry
                            data.add(new LineParameters(fileDate, date,
                                                        Double.parseDouble(matcher.group(f107Group)),
                                                        Double.parseDouble(matcher.group(apGroup))));
                        }

                    } else {
                        if (inData) {
                            /* We have already read some data, so we are not in the header anymore
                            however, we don't recognize this non-empty line, we consider the file is corrupted */
                            throw new OrekitException(OrekitMessages.NOT_A_MARSHALL_SOLAR_ACTIVITY_FUTURE_ESTIMATION_FILE,
                                                      name);
                        }
                    }
                }
            }

        }

        if (data.isEmpty()) {
            throw new OrekitException(OrekitMessages.NOT_A_MARSHALL_SOLAR_ACTIVITY_FUTURE_ESTIMATION_FILE, name);
        }
        setMinDate(data.first().getDate());
        setMaxDate(data.last().getDate());

    }

    /** @return the data set */
    @Override
    public SortedSet<LineParameters> getDataSet() {
        return data.stream().map(value -> (LineParameters) value).collect(Collectors.toCollection(TreeSet::new));
    }

    /** Container class for Solar activity indexes. */
    public static class LineParameters extends AbstractSolarActivityDataLoader.LineParameters {

        /** Serializable UID. */
        private static final long serialVersionUID = 6607862001953526475L;

        /** File date. */
        private final DateComponents fileDate;

        /** F10.7 flux at date. */
        private final double f107;

        /** Ap index at date. */
        private final double ap;

        /**
         * Simple constructor.
         *
         * @param fileDate file date
         * @param date entry date
         * @param f107 F10.7 flux at date
         * @param ap Ap index at date
         */
        private LineParameters(final DateComponents fileDate, final AbsoluteDate date, final double f107, final double ap) {
            super(date);
            this.fileDate = fileDate;
            this.f107     = f107;
            this.ap       = ap;
        }

        /** {@inheritDoc} */
        @Override
        public int compareTo(final AbstractSolarActivityDataLoader.LineParameters lineParameters) {
            return getDate().compareTo(lineParameters.getDate());
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object otherInstance) {
            if (this == otherInstance) {
                return true;
            }
            if (otherInstance == null || getClass() != otherInstance.getClass()) {
                return false;
            }

            final LineParameters msafeParams = (LineParameters) otherInstance;

            if (Double.compare(getF107(), msafeParams.getF107()) != 0) {
                return false;
            }
            if (Double.compare(getAp(), msafeParams.getAp()) != 0) {
                return false;
            }
            return getFileDate().equals(msafeParams.getFileDate());
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            int  result;
            long temp;
            result = getFileDate().hashCode();
            temp   = Double.doubleToLongBits(getF107());
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp   = Double.doubleToLongBits(getAp());
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        /**
         * Get the file date.
         *
         * @return file date
         */
        public DateComponents getFileDate() {
            return fileDate;
        }

        /**
         * Get the F10.0 flux.
         *
         * @return f10.7 flux
         */
        public double getF107() {
            return f107;
        }

        /**
         * Get the Ap index.
         *
         * @return Ap index
         */
        public double getAp() {
            return ap;
        }

    }

}

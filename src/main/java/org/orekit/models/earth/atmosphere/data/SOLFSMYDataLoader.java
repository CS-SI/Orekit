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
import java.util.HashSet;
import java.util.Set;

import org.hipparchus.exception.Localizable;
import org.orekit.data.DataLoader;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeStamped;


/**
 * This class reads solar activity data from SOLFSMY files for the class
 * {@link JB2008SpaceWeatherData}. The code in this class is based of the
 * CssiSpaceWeatherDataLoader.
 * <p>
 * The data is provided by Space Environment Technologies through their website
 * <a href="https://sol.spacenvironment.net/JB2008/indices/SOLFSMY.TXT">Link</a>.
 * </p>
 * The work done for this class is based on the CssiWpaceWeatherDataLoader class
 * by Clément Jonglez, the JB2008 interface by Pascal Parraud, and corrections for
 * DataLoader implementation by Bryan Cazabonne and Evan Ward .
 *
 * @author Louis Aucouturier
 * @since 11.2
 */


public class SOLFSMYDataLoader implements DataLoader {

    /** Helper class to parse line data and to raise exceptions if needed. */
    public static class LineReader {
        /** Name of the file. */
        private final String name;

        /** The input stream. */
        private final BufferedReader in;

        /** The last line read from the file. */
        private String line;

        /** The number of the last line read from the file. */
        private long lineNo;

        /**
         * Create a line reader.
         *
         * @param name of the data source for error messages.
         * @param in   the input data stream.
         */
        public LineReader(final String name, final BufferedReader in) {
            this.name = name;
            this.in = in;
            this.line = null;
            this.lineNo = 0;
        }

        /**
         * Read a line from the input data stream.
         *
         * @return the next line without the line termination character, or {@code null}
         *         if the end of the stream has been reached.
         * @throws IOException if an I/O error occurs.
         * @see BufferedReader#readLine()
         */
        public String readLine() throws IOException {
            line = in.readLine();
            lineNo++;
            return line;
        }

        /**
         * Read a line from the input data stream, or if the end of the stream has been
         * reached throw an exception.
         *
         * @param message for the exception if the end of the stream is reached.
         * @param args    for the exception if the end of stream is reached.
         * @return the next line without the line termination character, or {@code null}
         *         if the end of the stream has been reached.
         * @throws IOException     if an I/O error occurs.
         * @throws OrekitException if a line could not be read because the end of the
         *                         stream has been reached.
         * @see #readLine()
         */
        public String readLineOrThrow(final Localizable message, final Object... args)
                throws IOException, OrekitException {

            final String text = readLine();
            if (text == null) {
                throw new OrekitException(message, args);
            }
            return text;
        }

        /**
         * Annotate an exception with the file context.
         *
         * @param cause the reason why the line could not be parsed.
         * @return an exception with the cause, file name, line number, and line text.
         */
        public OrekitException unableToParseLine(final Throwable cause) {
            return new OrekitException(cause, OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, lineNo, name, line);
        }

        /**
         * Get the last line read from the stream.
         *
         * @return May be {@code null} if no lines have been read or the end of stream
         *         has been reached.
         */
        public String getLine() {
            return line;
        }

        /**
         * Get the line number of the last line read from the file.
         *
         * @return the line number.
         */
        public long getLineNumber() {
            return lineNo;
        }

    }

    /** Container class for Solar activity indexes. */
    public static class LineParameters implements TimeStamped, Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = -9008818050532123587L;

        /** Entry date. */
        private final AbsoluteDate date;

        /** 10.7-cm Solar flux (1e<sup>-22</sup>*Watt/(m²*Hertz))<br>
         * (Tabular time 1.0 day earlier). */
        private final double f10;

        /** 10.7-cm Solar Flux, averaged 81-day centered on the input time.  */
        private final double f10b;

        /** EUV index (26-34 nm) scaled to F10. */
        private final double s10;

        /** UV 81-day averaged centered index. */
        private final double s10b;

        /** MG2 index scaled to F10. */
        private final double xm10;

        /** MG2 81-day average centered index. */
        private final double xm10b;

        /** Solar X-Ray &amp; Lya index scaled to F10. */
        private final double y10;

        /** Solar X-Ray &amp; Lya 81-day average centered index. */
        private final double y10b;

        /**
         * Constructor.
         * @param date  entry date
         * @param f10   10.7-cm Solar Radio Flux (F10.7)
         * @param f10b  10.7-cm Solar Flux, averaged 81-day centered on the input time
         * @param s10   EUV index (26-34 nm) scaled to F10
         * @param s10b  UV 81-day averaged centered index
         * @param xm10  MG2 index scaled to F10
         * @param xm10b MG2 81-day average centered index
         * @param y10   Solar X-Ray &amp; Lya index scaled to F10
         * @param y10b  Solar X-Ray &amp; Lya 81-day average centered index
         */
        public LineParameters(final AbsoluteDate date, final double f10, final double f10b, final double s10,
                final double s10b, final double xm10, final double xm10b, final double y10, final double y10b) {
            this.date = date;
            this.f10 = f10;
            this.f10b = f10b;
            this.s10 = s10;
            this.s10b = s10b;
            this.xm10 = xm10;
            this.xm10b = xm10b;
            this.y10 = y10;
            this.y10b = y10b;
        }

        @Override
        public AbsoluteDate getDate() {
            return date;
        }

        /** Get the value of the instantaneous solar flux index
         *  (1e<sup>-22</sup>*Watt/(m²*Hertz)).
         * <p>Tabular time 1.0 day earlier.</p>
         * @return the instantaneous F10.7 index
         */
        public double getF10() {
            return f10;
        }

        /** Get the value of the mean solar flux.
         * Averaged 81-day centered F10.7 B index on the input time.
         * <p>Tabular time 1.0 day earlier.</p>
         * @return the mean solar flux F10.7B index
         */
        public double getF10B() {
            return f10b;
        }

        /** Get the EUV index (26-34 nm) scaled to F10.
         * <p>Tabular time 1.0 day earlier.</p>
         * @return the the EUV S10 index
         */
        public double getS10() {
            return s10;
        }

        /** Get the EUV 81-day averaged centered index.
         * <p>Tabular time 1.0 day earlier.</p>
         * @return the the mean EUV S10B index
         */
        public double getS10B() {
            return s10b;
        }

        /** Get the MG2 index scaled to F10.
         * <p>Tabular time 2.0 days earlier.</p>
         * @return the the MG2 index
         */
        public double getXM10() {
            return xm10;
        }

        /** Get the MG2 81-day average centered index.
         * <p>Tabular time 2.0 days earlier.</p>
         * @return the the mean MG2 index
         */
        public double getXM10B() {
            return xm10b;
        }

        /** Get the Solar X-Ray &amp; Lya index scaled to F10.
         * <p>Tabular time 5.0 days earlier.</p>
         * @return the Solar X-Ray &amp; Lya index scaled to F10
         */
        public double getY10() {
            return y10;
        }

        /** Get the Solar X-Ray &amp; Lya 81-day ave. centered index.
         * <p>Tabular time 5.0 days earlier.</p>
         * @return the Solar X-Ray &amp; Lya 81-day ave. centered index
         */
        public double getY10B() {
            return y10b;
        }
    }

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
    public SOLFSMYDataLoader(final TimeScale utc) {
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
        final Set<AbsoluteDate> parsedEpochs = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

            final LineReader reader = new LineReader(name, br);

            for (line = reader.readLine(); line != null; line = reader.readLine()) {
                lineNumber++;
                if (line.length() > 0) {
                    /** extract the data from the line
                     * The data is extracted from substrings as the spacing between
                     * columns is constant.
                     */
                    if (!(line.charAt(0) == '#') && !line.isEmpty()) {
                        /**
                         * The Julian Date is expressed as float in the text file,
                         * and supposed to be taken at 12UT.
                         */

                        final String[] splitLine = line.split("\\s+");
                        final double julianDay = Double.parseDouble(splitLine[3]);
                        final int julianDayInt = (int) julianDay;
                        final double julianSeconds = (julianDay - julianDayInt) * 24 * 3600;
                        final AbsoluteDate date = AbsoluteDate.createJDDate(julianDayInt, julianSeconds, utc);

                        if (parsedEpochs.add(date)) {

                            final double f10 = Double.parseDouble(splitLine[4]);

                            final double f10b = Double.parseDouble(splitLine[5]);

                            final double s10 = Double.parseDouble(splitLine[6]);

                            final double s10b = Double.parseDouble(splitLine[7]);

                            final double xm10 = Double.parseDouble(splitLine[8]);

                            final double xm10b = Double.parseDouble(splitLine[9]);

                            final double y10 = Double.parseDouble(splitLine[10]);

                            final double y10b = Double.parseDouble(splitLine[11]);

                            set.add(new LineParameters(date, f10, f10b, s10, s10b, xm10,
                                    xm10b, y10, y10b));
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

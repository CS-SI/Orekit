/* Copyright 2002-2019 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.gnss;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.util.FastMath;
import org.orekit.data.DataFilter;
import org.orekit.data.NamedData;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Decompression filter for Hatanaka compressed RINEX files.
 * @see <a href="http://cedadocs.ceda.ac.uk/1254/1/Hatanaka%5C_compressed%5C_format%5C_help.pdf">A
 * Compression Format and Tools for GNSS Observation Data</a>
 * @since 10.1
 */
public class HatanakaCompressFilter implements DataFilter {

    /** Pattern for rinex 2 observation files. */
    private static final Pattern RINEX_2_PATTERN = Pattern.compile("^(\\w{4}\\d{3}[0a-x](?:\\d{2})?\\.\\d{2})[dD]$");

    /** Pattern for rinex 3 observation files. */
    private static final Pattern RINEX_3_PATTERN = Pattern.compile("^(\\w{9}_\\w{1}_\\d{11}_\\d{2}\\w_\\d{2}\\w{1}_\\w{2})\\.crx$");

    /** {@inheritDoc} */
    @Override
    public NamedData filter(final NamedData original) {

        final String                 oName   = original.getName();
        final NamedData.StreamOpener oOpener = original.getStreamOpener();

        final Matcher rinex2Matcher = RINEX_2_PATTERN.matcher(oName);
        if (rinex2Matcher.matches()) {
            // this is a rinex 2 file compressed with Hatanaka method
            final String                 fName   = rinex2Matcher.group(1) + "o";
            final NamedData.StreamOpener fOpener = () -> new HatanakaInputStream(oName, oOpener.openStream());
            return new NamedData(fName, fOpener);
        }

        final Matcher rinex3Matcher = RINEX_3_PATTERN.matcher(oName);
        if (rinex3Matcher.matches()) {
            // this is a rinex 3 file compressed with Hatanaka method
            final String                 fName   = rinex3Matcher.group(1) + ".rnx";
            final NamedData.StreamOpener fOpener = () -> new HatanakaInputStream(oName, oOpener.openStream());
            return new NamedData(fName, fOpener);
        }

        // it is not an Hatanaka compressed rinex file
        return original;

    }

    /** Filtering of Hatanaka compressed stream. */
    private static class HatanakaInputStream extends InputStream {

        /** Index of label in data lines. */
        private static final int LABEL_START = 60;

        /** Label for compact Rinex version. */
        private static final String CRINEX_VERSION_TYPE = "CRINEX VERS   / TYPE";

        /** Label for compact Rinex program. */
        private static final String CRINEX_PROG_DATE    = "CRINEX PROG / DATE";

        /** Label for end of header. */
        private static final String END_OF_HEADER        = "END OF HEADER";

        /** File name. */
        private final String name;

        /** Line-oriented input. */
        private final BufferedReader reader;

        /** Compact rinex version multiplied by 100. */
        private final int cVersion100;

        /** Indicator for header. */
        private boolean inHeader;

        /** Line pending output. */
        private String pending;

        /** Number of characters already output in pending line. */
        private int countOut;

        /** Simple constructor.
         * @param name file name
         * @param input underlying compressed stream
         * @exception IOException if first bytes cannot be read
         */
        HatanakaInputStream(final String name, final InputStream input)
            throws IOException {

            this.name   = name;
            this.reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

            // check header
            final String line1 = reader.readLine();
            if (!CRINEX_VERSION_TYPE.equals(parseString(line1, LABEL_START, CRINEX_VERSION_TYPE.length()))) {
                throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_HATANAKA_COMPRESSED_FILE, name);
            }
            cVersion100 = (int) FastMath.rint(100 * parseDouble(line1, 0, 9));
            if ((cVersion100 != 100) && (cVersion100 != 300)) {
                throw new OrekitException(OrekitMessages.UNSUPPORTED_FILE_FORMAT, name);
            }
            final String line2 = reader.readLine();
            if (!CRINEX_PROG_DATE.equals(parseString(line2, LABEL_START, CRINEX_PROG_DATE.length()))) {
                throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_HATANAKA_COMPRESSED_FILE, name);
            }

            inHeader = true;
            pending  = null;

        }

        /** {@inheritDoc} */
        @Override
        public int read() throws IOException {

            if (pending == null) {
                // we need to read another line from the underlying stream and uncompress it
                uncompressLine();
                if (pending == null) {
                    // there are no lines left
                    return -1;
                }
            }

            if (countOut == pending.length()) {
                // output an end of line
                pending = null;
                return '\n';
            } else {
                // output a character from the uncompressed line
                return pending.charAt(countOut++);
            }

        }

        /** Read and uncompress one line.
         * @exception IOException if we cannot read a line from underlying stream
         */
        private void uncompressLine() throws IOException {
            countOut = 0;
            final String inLine = reader.readLine();
            if (inLine == null) {
                // there are no lines left
                pending = null;
            } else {
                if (inHeader) {
                    // within header, lines are simply copied out without any processing
                    pending  = inLine;
                    inHeader = !END_OF_HEADER.equals(parseString(inLine, LABEL_START, END_OF_HEADER.length()));
                } else {
                    // TODO
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public void close() throws IOException {
            reader.close();
        }

        /** Extract a string from a line.
         * @param line to parse
         * @param start start index of the string
         * @param length length of the string
         * @return parsed string
         */
        private String parseString(final String line, final int start, final int length) {
            if (line.length() > start) {
                return line.substring(start, FastMath.min(line.length(), start + length)).trim();
            } else {
                return null;
            }
        }

        /** Extract an integer from a line.
         * @param line to parse
         * @param start start index of the integer
         * @param length length of the integer
         * @return parsed integer
         */
        private int parseInt(final String line, final int start, final int length) {
            if (line.length() > start && !parseString(line, start, length).isEmpty()) {
                return Integer.parseInt(parseString(line, start, length));
            } else {
                return 0;
            }
        }

        /** Extract a double from a line.
         * @param line to parse
         * @param start start index of the real
         * @param length length of the real
         * @return parsed real, or {@code Double.NaN} if field was empty
         */
        private double parseDouble(final String line, final int start, final int length) {
            if (line.length() > start && !parseString(line, start, length).isEmpty()) {
                return Double.parseDouble(parseString(line, start, length));
            } else {
                return Double.NaN;
            }
        }

    }

    /** Processor handling differential compression for one data field. */
    private static class Differential {

        /** Length of the uncompressed text field. */
        private final int fieldLength;

        /** Number of decimal places uncompressed text field. */
        private final int decimalPlaces;

        /** State vector. */
        private final long[] state;

        /** Number of components in the state vector. */
        private int nbComponents;

        /** simple constructor.
         * @param fieldLength length of the uncompressed text field
         * @param decimalPlaces number of decimal places uncompressed text field
         * @param order differential order
         */
        Differential(final int fieldLength, final int decimalPlaces, final int order) {
            this.fieldLength   = fieldLength;
            this.decimalPlaces = decimalPlaces;
            this.state         = new long[order + 1];
            this.nbComponents  = 0;
        }

        /** Handle a new compressed value.
         * @param value value to add
         * @param buffer buffer where character representation should be appended
         * @exception IOException if buffer cannot be appended
         */
        public void accept(final long value, final Appendable buffer)
            throws IOException {

            // store the value as the last component of state vector
            state[nbComponents] = value;

            // update state vector
            for (int i = nbComponents; i > 0; --i) {
                state[i - 1] += state[i];
            }

            if (++nbComponents == state.length) {
                // the state vector is full
                --nbComponents;
            }

            // output uncompressed value
            final String unscaled = Long.toString(state[0]);
            for (int padding = fieldLength - (unscaled.length() + 1); padding > 0; --padding) {
                buffer.append(' ');
            }
            buffer.append(unscaled, 0, unscaled.length() - decimalPlaces);
            buffer.append('.');
            buffer.append(unscaled, unscaled.length() - decimalPlaces, unscaled.length());

        }

    }

}

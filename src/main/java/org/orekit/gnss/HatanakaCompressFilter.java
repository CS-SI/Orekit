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
    private static final Pattern RINEX_2_PATTERN = Pattern.compile("^(\\w{4}\\d{3}[0a-x](?:\\d{2})?)\\.(\\d{2})[dD]$");

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
            final String                 fName   = rinex2Matcher.group(1) + "." + rinex2Matcher.group(2) + "o";
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

        /** Compact Rinex version key. */
        private static final String CRINEX_VERSION_TYPE = "CRINEX VERS   / TYPE";

        /** Compact Rinex program key. */
        private static final String CRINEX_PROG_DATE    = "CRINEX PROG / DATE";

        /** File name. */
        private final String name;

        /** Line-oriented input. */
        private final BufferedReader reader;

        /** Simple constructor.
         * @param name file name
         * @param input underlying compressed stream
         * @exception IOException if first bytes cannot be read
         */
        HatanakaInputStream(final String name, final InputStream input)
            throws IOException {

            this.name  = name;
            this.reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

            // check header
            final String line1 = reader.readLine();
            if (!CRINEX_VERSION_TYPE.equals(parseString(line1, 60, CRINEX_VERSION_TYPE.length()))) {
                throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_HATANAKA_COMPRESSED_FILE, name);
            }
            final int format100 = (int) FastMath.rint(100 * parseDouble(line1, 0, 9));
            if ((format100 != 100) && (format100 != 300)) {
                throw new OrekitException(OrekitMessages.UNSUPPORTED_FILE_FORMAT, name);
            }
            final String line2 = reader.readLine();
            if (!CRINEX_PROG_DATE.equals(parseString(line2, 60, CRINEX_PROG_DATE.length()))) {
                throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_HATANAKA_COMPRESSED_FILE, name);
            }

        }

        /** {@inheritDoc} */
        @Override
        public int read() throws IOException {
            // TODO
            return -1;
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

}

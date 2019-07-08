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
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        private static final String CRINEX_VERSION_TYPE  = "CRINEX VERS   / TYPE";

        /** Label for compact Rinex program. */
        private static final String CRINEX_PROG_DATE     = "CRINEX PROG / DATE";

        /** Default number of satellites (used if not present in the file). */
        private static final int    DEFAULT_NB_SAT               = 500;

        /** File name. */
        private final String name;

        /** Line-oriented input. */
        private final BufferedReader reader;

        /** Format of the current file. */
        private final RinexFormat format;

        /** Maximum number of observations for one satellite. */
        private int maxObs;

        /** Number of satellites. */
        private int nbSat;

        /** Line number within epoch. */
        private int lineInEpoch;

        /** Differential engine for epoch. */
        private TextDifferential epochDifferential;

        /** Receiver clock offset differential. */
        private NumericDifferential clockDifferential;

        /** Differential engine for satellites list. */
        private TextDifferential satListDifferential;

        /** Satellites observed at current epoch. */
        private List<CharSequence> satellites;

        /** Differential engines for each satellite. */
        private Map<CharSequence, List<NumericDifferential>> differentials;

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
            format = RinexFormat.getFormat(name, line1);
            final String line2 = reader.readLine();
            if (!CRINEX_PROG_DATE.equals(parseString(line2, LABEL_START, CRINEX_PROG_DATE.length()))) {
                throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_HATANAKA_COMPRESSED_FILE, name);
            }

            maxObs       = 0;
            nbSat        = DEFAULT_NB_SAT;
            lineInEpoch  = -1;
            pending      = null;

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
                if (lineInEpoch < 0) {

                    // pick up a few data on the fly
                    pending = format.parseHeaderLine(this, inLine);

                } else if (lineInEpoch == 0) {

                    // epoch line
                    final String clockLine = reader.readLine().trim();
                    if (clockLine == null) {
                        throw new OrekitException(OrekitMessages.UNEXPECTED_END_OF_FILE, name);
                    }
                    pending = format.parseEpochAndClockLines(this, inLine, clockLine);

                } else {

                    // observation line
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
        private static String parseString(final String line, final int start, final int length) {
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
        private static int parseInt(final String line, final int start, final int length) {
            if (line.length() > start && !parseString(line, start, length).isEmpty()) {
                return Integer.parseInt(parseString(line, start, length));
            } else {
                return 0;
            }
        }

        /** Extract a long integer from a line.
         * @param line to parse
         * @param start start index of the integer
         * @param length length of the integer
         * @return parsed long integer
         */
        private static long parseLong(final String line, final int start, final int length) {
            if (line.length() > start && !parseString(line, start, length).isEmpty()) {
                return Long.parseLong(parseString(line, start, length));
            } else {
                return 0l;
            }
        }

        /** Extract a double from a line.
         * @param line to parse
         * @param start start index of the real
         * @param length length of the real
         * @return parsed real, or {@code Double.NaN} if field was empty
         */
        private static double parseDouble(final String line, final int start, final int length) {
            if (line.length() > start && !parseString(line, start, length).isEmpty()) {
                return Double.parseDouble(parseString(line, start, length));
            } else {
                return Double.NaN;
            }
        }

    }

    /** Processor handling differential compression for one numerical data field. */
    private static class NumericDifferential {

        /** Length of the uncompressed text field. */
        private final int fieldLength;

        /** Number of decimal places uncompressed text field. */
        private final int decimalPlaces;

        /** State vector. */
        private final long[] state;

        /** Number of components in the state vector. */
        private int nbComponents;

        /** Simple constructor.
         * @param fieldLength length of the uncompressed text field
         * @param decimalPlaces number of decimal places uncompressed text field
         * @param order differential order
         */
        NumericDifferential(final int fieldLength, final int decimalPlaces, final int order) {
            this.fieldLength   = fieldLength;
            this.decimalPlaces = decimalPlaces;
            this.state         = new long[order + 1];
            this.nbComponents  = 0;
        }

        /** Handle a new compressed value.
         * @param value value to add
         * @return string representation of the uncompressed value
         */
        public String accept(final long value) {

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
            final StringBuilder builder = new StringBuilder();
            for (int padding = fieldLength - (unscaled.length() + 1); padding > 0; --padding) {
                builder.append(' ');
            }
            builder.append(unscaled, 0, unscaled.length() - decimalPlaces);
            builder.append('.');
            builder.append(unscaled, unscaled.length() - decimalPlaces, unscaled.length());

            return builder.toString();

        }

    }

    /** Processor handling text compression for one text data field. */
    private static class TextDifferential {

        /** Buffer holding the current state. */
        private CharBuffer state;

        /** Simple constructor.
         * @param fieldLength length of the uncompressed text field
         */
        TextDifferential(final int fieldLength) {
            this.state = CharBuffer.allocate(fieldLength);
            for (int i = 0; i < fieldLength; ++i) {
                state.put(i, ' ');
            }
        }

        /** Handle a new compressed value.
         * @param complete sequence containing the value to consider
         * @param start start index of the value within the sequence
         * @param end end index of the value within the sequence
         * @return string representation of the uncompressed value
         */
        public String accept(final CharSequence complete, final int start, final int end) {

            // update state
            final int length = FastMath.min(state.capacity(), end - start);
            for (int i = 0; i < length; ++i) {
                final char c = complete.charAt(start + i);
                if (c == '&') {
                    // update state with disappearing character
                    state.put(i, ' ');
                } else if (c != ' ') {
                    // update state with changed character
                    state.put(i, c);
                }
            }

            // output uncompressed value
            return state.toString();

        }

    }

    /** Enumerate for rinex formats. */
    private enum RinexFormat {

        /** Rinex 2 format. */
        RINEX_2 {

            /** Label for number of observations. */
            private static final String NB_TYPES_OF_OBSERV   = "# / TYPES OF OBSERV";

            /** Start of epoch field. */
            private static final int    EPOCH_START          = 0;

            /** End of epoch field. */
            private static final int    EPOCH_LENGTH         = 32;

            /** Start of satellites list field. */
            private static final int    SAT_LIST_START       = EPOCH_START + EPOCH_LENGTH;

            /** End of satellites list field. */
            private static final int    SAT_LIST_LENGTH      = 36;

            /** Start of receiver clock field. */
            private static final int    CLOCK_START          = SAT_LIST_START + SAT_LIST_LENGTH;

            /** End of receiver clock field. */
            private static final int    CLOCK_LENGTH         = 12;

            /** Field for empty clock. */
            private static final String EMPTY_CLOCK          = "            ";

            /** Empty clock field. */
            /** Number of decimal places for receiver clock offset. */
            private static final int    CLOCK_DECIMAL_PLACES = 9;

            @Override
            /** {@inheritDoc} */
            public String parseEpochAndClockLines(final HatanakaInputStream his,
                                                 final String epochLine, final String clockLine) {

                // check reset
                final boolean reset = epochLine.charAt(0) == '&';
                if (reset) {
                    his.epochDifferential   = new TextDifferential(EPOCH_LENGTH);
                    his.satListDifferential = new TextDifferential(SAT_LIST_LENGTH);
                    his.differentials       = new HashMap<>();
                    his.clockDifferential   = clockLine.isEmpty() ?
                                              null :
                                              new NumericDifferential(CLOCK_LENGTH,
                                                                      CLOCK_DECIMAL_PLACES,
                                                                      HatanakaInputStream.parseInt(clockLine, 0, 1));
                }

                // parse epoch
                final String epochPart   = his.epochDifferential.accept(epochLine, EPOCH_START, EPOCH_START + EPOCH_LENGTH);
                final String satListPart = his.satListDifferential.accept(epochLine, SAT_LIST_START, epochLine.length());
                his.satellites = new ArrayList<>();
                for (int i = 0; i < satListPart.length(); i += 3) {
                    his.satellites.add(satListPart.subSequence(i, i + 3));
                }

                // parse clock offset
                final String clockPart;
                if (clockLine.isEmpty()) {
                    clockPart = EMPTY_CLOCK;
                } else {
                    final int skip = reset ? 2 : 0;
                    clockPart = his.clockDifferential.accept(HatanakaInputStream.parseLong(clockLine, skip, clockLine.length() - skip));
                }

                his.lineInEpoch = 1;

                // concatenate everything
                final StringBuilder builder = new StringBuilder();
                builder.append(epochPart);
                builder.append(satListPart);
                while (builder.length() < CLOCK_START) {
                    builder.append(' ');
                }
                builder.append(clockPart);
                return builder.toString();

            }

            @Override
            /** {@inheritDoc} */
            public String parseHeaderLine(final HatanakaInputStream his, final String line) {
                if (isHeaderLine(NB_TYPES_OF_OBSERV, line)) {
                    his.maxObs = FastMath.max(his.maxObs, HatanakaInputStream.parseInt(line, 0, 6));
                    return line;
                } else {
                    return super.parseHeaderLine(his, line);
                }
            }

        },

        /** Rinex 3 format. */
        RINEX_3 {

            /** Label for number of observation types. */
            private static final String SYS_NB_OBS_TYPES     = "SYS / # / OBS TYPES";

            /** Start of epoch field. */
            private static final int    EPOCH_START          = 0;

            /** End of epoch field. */
            private static final int    EPOCH_LENGTH         = 41;

            /** Start of receiver clock field. */
            private static final int    CLOCK_START          = EPOCH_START + EPOCH_LENGTH;

            /** End of receiver clock field. */
            private static final int    CLOCK_LENGTH         = 15;

            /** Number of decimal places for receiver clock offset. */
            private static final int    CLOCK_DECIMAL_PLACES = 12;

            /** Start of satellites list field (only in the compact rinex). */
            private static final int    SAT_LIST_START       = CLOCK_START + CLOCK_LENGTH;

            @Override
            /** {@inheritDoc} */
            public String parseEpochAndClockLines(final HatanakaInputStream his,
                                                  final String epochLine, final String clockLine) {

                // check reset
                final boolean reset = epochLine.charAt(0) == '>';
                if (reset) {
                    his.epochDifferential   = new TextDifferential(EPOCH_LENGTH);
                    his.satListDifferential = new TextDifferential(his.nbSat * 3);
                    his.differentials       = new HashMap<>();
                    his.clockDifferential   = clockLine.isEmpty() ?
                                              null :
                                              new NumericDifferential(CLOCK_LENGTH,
                                                                      CLOCK_DECIMAL_PLACES,
                                                                      HatanakaInputStream.parseInt(clockLine, 0, 1));
                }

                // parse epoch
                final String epochPart   = his.epochDifferential.accept(epochLine, EPOCH_START, EPOCH_START + EPOCH_LENGTH);
                final String satListPart = his.satListDifferential.accept(epochLine, SAT_LIST_START, epochLine.length());
                his.satellites = new ArrayList<>();
                for (int i = 0; i < satListPart.length(); i += 3) {
                    his.satellites.add(satListPart.subSequence(i, i + 3));
                }

                // parse clock offset
                final String clockPart;
                if (clockLine.isEmpty()) {
                    clockPart = null;
                } else {
                    final int skip = reset ? 2 : 0;
                    clockPart = his.clockDifferential.accept(HatanakaInputStream.parseLong(clockLine, skip, clockLine.length() - skip));
                }

                his.lineInEpoch = 1;

                // concatenate everything
                return (clockPart == null) ? epochPart : epochPart + clockPart;

            }

            @Override
            /** {@inheritDoc} */
            public String parseHeaderLine(final HatanakaInputStream his, final String line) {
                if (isHeaderLine(SYS_NB_OBS_TYPES, line)) {
                    his.maxObs = FastMath.max(his.maxObs, HatanakaInputStream.parseInt(line, 1, 5));
                    return line;
                } else {
                    return super.parseHeaderLine(his, line);
                }
            }

        };

        /** Label for number of satellites. */
        private static final String NB_OF_SATELLITES = "# OF SATELLITES";

        /** Label for end of header. */
        private static final String END_OF_HEADER    = "END OF HEADER";

        /** Length of a data field. */
        private static final int    DATA_LENGTH      = 19;

        /** Parse a header line.
         * @param his Hatanaka input stream
         * @param line header line
         * @return uncompressed line
         */
        public String parseHeaderLine(final HatanakaInputStream his, final String line) {

            if (isHeaderLine(NB_OF_SATELLITES, line)) {
                // number of satellites
                his.nbSat = HatanakaInputStream.parseInt(line, 0, 6);
            } else if (isHeaderLine(END_OF_HEADER, line)) {
                // we have reached end of header, prepare parsing of data records
                his.lineInEpoch = 0;
            }

            // within header, lines are simply copied
            return line;

        }

        /** Parse epoch and receiver clock offset lines.
         * @param his Hatanaka input stream
         * @param epochLine epoch line
         * @param clockLine receiver clock offset line
         * @return uncompressed line
         */
        public abstract String parseEpochAndClockLines(HatanakaInputStream his, String epochLine, String clockLine);

        /** Get the rinex format corresponding to this compact rinex format.
         * @param name file name
         * @param line1 first compact rinex line
         * @return rinex format associated with this compact rinex format
         */
        public static RinexFormat getFormat(final String name, final String line1) {
            final int cVersion100 = (int) FastMath.rint(100 * HatanakaInputStream.parseDouble(line1, 0, 9));
            if ((cVersion100 != 100) && (cVersion100 != 300)) {
                throw new OrekitException(OrekitMessages.UNSUPPORTED_FILE_FORMAT, name);
            }
            return cVersion100 < 300 ? RINEX_2 : RINEX_3;
        }

        /** Check if a line corresponds to a header.
         * @param label header label
         * @param line header line
         * @return true if line corresponds to header
         */
        private static boolean isHeaderLine(final String label, final String line) {
            return label.equals(HatanakaInputStream.parseString(line,
                                HatanakaInputStream.LABEL_START,
                                label.length()));
        }

    }

}

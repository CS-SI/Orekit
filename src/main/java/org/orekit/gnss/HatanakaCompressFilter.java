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

        /** Format of the current file. */
        private final CompactRinexFormat format;

        /** Line-oriented input. */
        private final BufferedReader reader;

        /** Pending uncompressed output lines. */
        private String pending;

        /** Number of characters already output in pending lines. */
        private int countOut;

        /** Simple constructor.
         * @param name file name
         * @param input underlying compressed stream
         * @exception IOException if first lines cannot be read
         */
        HatanakaInputStream(final String name, final InputStream input)
            throws IOException {

            reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

            // check header
            format = CompactRinexFormat.getFormat(name, reader);

            pending  = null;

        }

        /** {@inheritDoc} */
        @Override
        public int read() throws IOException {

            if (pending == null) {
                // we need to read another section from the underlying stream and uncompress it
                countOut = 0;
                final String firstLine = reader.readLine();
                if (firstLine == null) {
                    // there are no lines left
                    return -1;
                } else {
                    pending = format.uncompressSection(firstLine);
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

        /** {@inheritDoc} */
        @Override
        public void close() throws IOException {
            reader.close();
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

    /** Base class for parsing compact RINEX format. */
    private abstract static class CompactRinexFormat {

        /** Index of label in data lines. */
        private static final int LABEL_START = 60;

        /** Label for compact Rinex version. */
        private static final String CRINEX_VERSION_TYPE  = "CRINEX VERS   / TYPE";

        /** Label for compact Rinex program. */
        private static final String CRINEX_PROG_DATE     = "CRINEX PROG / DATE";

        /** Label for number of satellites. */
        private static final String NB_OF_SATELLITES = "# OF SATELLITES";

        /** Label for end of header. */
        private static final String END_OF_HEADER    = "END OF HEADER";

        /** Default number of satellites (used if not present in the file). */
        private static final int DEFAULT_NB_SAT = 500;

        /** File name. */
        private final String name;

        /** Line-oriented input. */
        private final BufferedReader reader;

        /** Maximum number of observations for one satellite. */
        private int maxObs;

        /** Number of satellites. */
        private int nbSat;

        /** Indicator for current section type. */
        private Section section;

        /** Uncompressed epoch part. */
        private String epochPart;

        /** Uncompressed clock part. */
        private String clockPart;

        /** Satellites observed at current epoch. */
        private List<CharSequence> satellites;

        /** Differential engine for epoch. */
        private TextDifferential epochDifferential;

        /** Receiver clock offset differential. */
        private NumericDifferential clockDifferential;

        /** Differential engine for satellites list. */
        private TextDifferential satListDifferential;

        /** Differential engines for each satellite. */
        private Map<CharSequence, List<NumericDifferential>> differentials;

        /** Simple constructor.
         * @param name file name
         * @param reader line-oriented input
         */
        protected CompactRinexFormat(final String name, final BufferedReader reader) {
            this.name    = name;
            this.reader  = reader;
            this.maxObs  = 0;
            this.nbSat   = DEFAULT_NB_SAT;
            this.section = Section.HEADER;
        }

        /** Uncompress a section.
         * @param firstLine first line of the section
         * @return uncompressed section (contains several lines)
         * @exception IOException if we cannot read lines from underlying stream
         */
        public String uncompressSection(final String firstLine)
            throws IOException {
            final String uncompressed;
            switch (section) {

                case HEADER : {
                    // header lines
                    final StringBuilder builder = new StringBuilder();
                    String line = firstLine;
                    while (section == Section.HEADER) {
                        if (line == null) {
                            line = reader.readLine();
                            if (line == null) {
                                throw new OrekitException(OrekitMessages.UNEXPECTED_END_OF_FILE, name);
                            }
                        }
                        if (builder.length() > 0) {
                            builder.append('\n');
                        }
                        builder.append(parseHeaderLine(line));
                        line = null;
                    }
                    uncompressed = builder.toString();
                    section      = Section.EPOCH;
                    break;
                }

                case EPOCH : {
                    // epoch and receiver clock offset lines
                    final String clockLine = reader.readLine();
                    if (clockLine == null) {
                        throw new OrekitException(OrekitMessages.UNEXPECTED_END_OF_FILE, name);
                    }
                    uncompressed = parseEpochAndClockLines(firstLine, clockLine.trim());
                    section      = Section.OBSERVATION;
                    break;
                }

                default : {
                    // observation lines
                    final String[] lines = new String[satellites.size()];
                    lines[0] = firstLine;
                    for (int i = 1; i < lines.length; ++i) {
                        lines[i] = reader.readLine();
                        if (lines[i] == null) {
                            throw new OrekitException(OrekitMessages.UNEXPECTED_END_OF_FILE, name);
                        }
                    }
                    uncompressed = parseObservationLines(lines);
                    section      = Section.EPOCH;
                }

            }

            return uncompressed;

        }

        /** Parse a header line.
         * @param line header line
         * @return uncompressed line
         */
        public String parseHeaderLine(final String line) {

            if (isHeaderLine(NB_OF_SATELLITES, line)) {
                // number of satellites
                nbSat = parseInt(line, 0, 6);
            } else if (isHeaderLine(END_OF_HEADER, line)) {
                // we have reached end of header, prepare parsing of data records
                section = Section.EPOCH;
            }

            // within header, lines are simply copied
            return line;

        }

        /** Parse epoch and receiver clock offset lines.
         * @param epochLine epoch line
         * @param clockLine receiver clock offset line
         * @return uncompressed line
         */
        public abstract String parseEpochAndClockLines(String epochLine, String clockLine);

        /** Parse epoch and receiver clock offset lines.
         * @param epochStart start of the epoch field
         * @param epochLength length of epoch field
         * @param nbSatStart start of the number of satellites field
         * @param satListStart start of the satellites list
         * @param clockLength length of receiver clock field
         * @param clockDecimalPlaces number of decimal places for receiver clock offset
         * @param epochLine epoch line
         * @param clockLine receiver clock offset line
         * @param resetChar character indicating differentials reset
         */
        protected void doParseEpochAndClockLines(final int epochStart, final int epochLength,
                                                 final int nbSatStart, final int satListStart,
                                                 final int clockLength, final int clockDecimalPlaces,
                                                 final String epochLine, final String clockLine, final char resetChar) {

            // check if differentials should be reset
            final boolean reset = (epochDifferential == null) || (epochLine.charAt(0) == resetChar);
            if (reset) {
                epochDifferential   = new TextDifferential(epochLength);
                satListDifferential = new TextDifferential(nbSat * 3);
                differentials       = new HashMap<>();
                clockDifferential   = clockLine.isEmpty() ?
                                      null :
                                      new NumericDifferential(clockLength, clockDecimalPlaces, parseInt(clockLine, 0, 1));
            }

            // parse epoch
            epochPart   = epochDifferential.accept(epochLine, epochStart, epochStart + epochLength);
            nbSat                    = parseInt(epochPart, nbSatStart, 3);
            final String satListPart = satListDifferential.accept(epochLine, satListStart, epochLine.length());
            satellites = new ArrayList<>(nbSat);
            for (int i = 0; i < nbSat; ++i) {
                satellites.add(satListPart.subSequence(i * 3, (i + 1) * 3));
            }

            // parse clock offset
            if (clockLine.isEmpty()) {
                clockPart = null;
            } else {
                final int skip = reset ? 2 : 0;
                clockPart = clockDifferential.accept(parseLong(clockLine, skip, clockLine.length() - skip));
            }

        }

        /** Get the uncompressed epoch part.
         * @return uncompressed epoch part
         */
        protected String getEpochPart() {
            return epochPart;
        }

        /** Get the uncompressed clock part.
         * @return uncompressed clock part
         */
        protected String getClockPart() {
            return clockPart;
        }

        /** Get the satellites for current observations.
         * @return satellites for current observation
         */
        protected List<CharSequence> getSatellites() {
            return satellites;
        }

        /** Parse observation lines.
         * @param observationLines observation lines
         * @return uncompressed lines
         */
        public abstract String parseObservationLines(String[] observationLines);

        /** Check if a line corresponds to a header.
         * @param label header label
         * @param line header line
         * @return true if line corresponds to header
         */
        protected boolean isHeaderLine(final String label, final String line) {
            return label.equals(parseString(line, LABEL_START, label.length()));
        }

        /** Update the max number of observations.
         * @param nbObs number of observations
         */
        protected void updateMaxObs(final int nbObs) {
            maxObs = FastMath.max(maxObs, nbObs);
        }

        /** Get the rinex format corresponding to this compact rinex format.
         * @param name file name
         * @param reader line-oriented input
         * @return rinex format associated with this compact rinex format
         * @exception IOException if first lines cannot be read
         */
        public static CompactRinexFormat getFormat(final String name, final BufferedReader reader)
            throws IOException {

            // read the first two lines of the file
            final String line1 = reader.readLine();
            final String line2 = reader.readLine();

            // extract format version
            final int cVersion100 = (int) FastMath.rint(100 * parseDouble(line1, 0, 9));
            if ((cVersion100 != 100) && (cVersion100 != 300)) {
                throw new OrekitException(OrekitMessages.UNSUPPORTED_FILE_FORMAT, name);
            }
            if (!CRINEX_VERSION_TYPE.equals(parseString(line1, LABEL_START, CRINEX_VERSION_TYPE.length()))) {
                throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_HATANAKA_COMPRESSED_FILE, name);
            }
            if (!CRINEX_PROG_DATE.equals(parseString(line2, LABEL_START, CRINEX_PROG_DATE.length()))) {
                throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_HATANAKA_COMPRESSED_FILE, name);
            }

            // build the appropriate parser
            return cVersion100 < 300 ? new CompactRinex1(name, reader) : new CompactRinex3(name, reader);

        }

        /** Extract a string from a line.
         * @param line to parse
         * @param start start index of the string
         * @param length length of the string
         * @return parsed string
         */
        public static String parseString(final String line, final int start, final int length) {
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
        public static int parseInt(final String line, final int start, final int length) {
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
        public static long parseLong(final String line, final int start, final int length) {
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
        public static double parseDouble(final String line, final int start, final int length) {
            if (line.length() > start && !parseString(line, start, length).isEmpty()) {
                return Double.parseDouble(parseString(line, start, length));
            } else {
                return Double.NaN;
            }
        }

        /** Enumerate for parsing sections. */
        private enum Section {

            /** Header section. */
            HEADER,

            /** Epoch and receiver clock offset section. */
            EPOCH,

            /** Observation section. */
            OBSERVATION;

        }

    }

    /** Compact RINEX 1 format (for RINEX 2.x). */
    private static class CompactRinex1 extends CompactRinexFormat {

        /** Label for number of observations. */
        private static final String NB_TYPES_OF_OBSERV   = "# / TYPES OF OBSERV";

        /** Start of epoch field. */
        private static final int    EPOCH_START          = 0;

        /** Length of epoch field. */
        private static final int    EPOCH_LENGTH         = 32;

        /** Start of number of satellites field. */
        private static final int    NB_SAT_START         = EPOCH_START + EPOCH_LENGTH - 3;

        /** Start of satellites list field. */
        private static final int    SAT_LIST_START       = EPOCH_START + EPOCH_LENGTH;

        /** Length of satellites list field. */
        private static final int    SAT_LIST_LENGTH      = 36;

        /** Maximum number of satellites per epoch line. */
        private static final int    MAX_SAT_EPOCH_LINE   = 12;

        /** Start of receiver clock field. */
        private static final int    CLOCK_START          = SAT_LIST_START + SAT_LIST_LENGTH;

        /** Length of receiver clock field. */
        private static final int    CLOCK_LENGTH         = 12;

        /** Number of decimal places for receiver clock offset. */
        private static final int    CLOCK_DECIMAL_PLACES = 9;

        /** Length of a data field. */
        private static final int    DATA_LENGTH          = 14;

        /** Simple constructor.
         * @param name file name
         * @param reader line-oriented input
         */
        CompactRinex1(final String name, final BufferedReader reader) {
            super(name, reader);
        }

        @Override
        /** {@inheritDoc} */
        public String parseHeaderLine(final String line) {
            if (isHeaderLine(NB_TYPES_OF_OBSERV, line)) {
                updateMaxObs(parseInt(line, 0, 6));
                return line;
            } else {
                return super.parseHeaderLine(line);
            }
        }

        @Override
        /** {@inheritDoc} */
        public String parseEpochAndClockLines(final String epochLine, final String clockLine) {

            doParseEpochAndClockLines(EPOCH_START, EPOCH_LENGTH, NB_SAT_START, SAT_LIST_START,
                                      CLOCK_LENGTH, CLOCK_DECIMAL_PLACES,
                                      epochLine, clockLine, '&');

            // build uncompressed lines, taking care of clock being put
            // back in line 1 and satellites after 12th put in continuation lines
            final List<CharSequence> satellites = getSatellites();
            final StringBuilder builder = new StringBuilder();
            builder.append(getEpochPart());
            int iSat = 0;
            while (iSat < FastMath.min(satellites.size(), MAX_SAT_EPOCH_LINE)) {
                builder.append(satellites.get(iSat++));
            }
            if (getClockPart() != null) {
                while (builder.length() < CLOCK_START) {
                    builder.append(' ');
                }
                builder.append(getClockPart());
            }

            while (iSat < satellites.size()) {
                // add a continuation line
                builder.append('\n');
                for (int k = 0; k < SAT_LIST_START; ++k) {
                    builder.append(' ');
                }
                while (iSat < FastMath.min(satellites.size(), MAX_SAT_EPOCH_LINE)) {
                    builder.append(satellites.get(iSat++));
                }
            }
            return builder.toString();

        }

        @Override
        /** {@inheritDoc} */
        public String parseObservationLines(final String[] observationLines) {
            // TODO
            return null;
        }

    }

    /** Compact RINEX 3 format (for RINEX 3.x). */
    private static class CompactRinex3 extends CompactRinexFormat {

        /** Label for number of observation types. */
        private static final String SYS_NB_OBS_TYPES     = "SYS / # / OBS TYPES";

        /** Start of epoch field. */
        private static final int    EPOCH_START          = 0;

        /** Length of epoch field. */
        private static final int    EPOCH_LENGTH         = 41;

        /** Start of receiver clock field. */
        private static final int    CLOCK_START          = EPOCH_START + EPOCH_LENGTH;

        /** Length of receiver clock field. */
        private static final int    CLOCK_LENGTH         = 15;

        /** Number of decimal places for receiver clock offset. */
        private static final int    CLOCK_DECIMAL_PLACES = 12;

        /** Start of number of satellites field. */
        private static final int    NB_SAT_START         = EPOCH_START + EPOCH_LENGTH - 3; // TBC

        /** Start of satellites list field (only in the compact rinex). */
        private static final int    SAT_LIST_START       = CLOCK_START + CLOCK_LENGTH;

        /** Simple constructor.
         * @param name file name
         * @param reader line-oriented input
         */
        CompactRinex3(final String name, final BufferedReader reader) {
            super(name, reader);
        }

        @Override
        /** {@inheritDoc} */
        public String parseHeaderLine(final String line) {
            if (isHeaderLine(SYS_NB_OBS_TYPES, line)) {
                updateMaxObs(parseInt(line, 1, 5));
                return line;
            } else {
                return super.parseHeaderLine(line);
            }
        }

        @Override
        /** {@inheritDoc} */
        public String parseEpochAndClockLines(final String epochLine, final String clockLine) {

            doParseEpochAndClockLines(EPOCH_START, EPOCH_LENGTH, NB_SAT_START, SAT_LIST_START,
                                      CLOCK_LENGTH, CLOCK_DECIMAL_PLACES,
                                      epochLine, clockLine, '>');

            // concatenate everything
            return (getClockPart() == null) ? getEpochPart() : getEpochPart() + getClockPart();

        }

        @Override
        /** {@inheritDoc} */
        public String parseObservationLines(final String[] observationLines) {
            // TODO
            return null;
        }

    }

}

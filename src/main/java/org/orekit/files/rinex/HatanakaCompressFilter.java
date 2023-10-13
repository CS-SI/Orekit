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
package org.orekit.files.rinex;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.util.FastMath;
import org.orekit.data.DataFilter;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.SatelliteSystem;

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

    /** Empty constructor.
     * <p>
     * This constructor is not strictly necessary, but it prevents spurious
     * javadoc warnings with JDK 18 and later.
     * </p>
     * @since 12.0
     */
    public HatanakaCompressFilter() {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public DataSource filter(final DataSource original) {

        final String            oName   = original.getName();
        final DataSource.Opener oOpener = original.getOpener();

        final Matcher rinex2Matcher = RINEX_2_PATTERN.matcher(oName);
        if (rinex2Matcher.matches()) {
            // this is a rinex 2 file compressed with Hatanaka method
            final String                  fName   = rinex2Matcher.group(1) + "o";
            final DataSource.ReaderOpener fOpener = () -> new HatanakaReader(oName, oOpener.openReaderOnce());
            return new DataSource(fName, fOpener);
        }

        final Matcher rinex3Matcher = RINEX_3_PATTERN.matcher(oName);
        if (rinex3Matcher.matches()) {
            // this is a rinex 3 file compressed with Hatanaka method
            final String                  fName   = rinex3Matcher.group(1) + ".rnx";
            final DataSource.ReaderOpener fOpener = () -> new HatanakaReader(oName, oOpener.openReaderOnce());
            return new DataSource(fName, fOpener);
        }

        // it is not an Hatanaka compressed rinex file
        return original;

    }

    /** Filtering of Hatanaka compressed characters stream. */
    private static class HatanakaReader extends Reader {

        /** Format of the current file. */
        private final CompactRinexFormat format;

        /** Line-oriented input. */
        private final BufferedReader reader;

        /** Pending uncompressed output lines. */
        private CharSequence pending;

        /** Number of characters already output in pending lines. */
        private int countOut;

        /** Simple constructor.
         * @param name file name
         * @param input underlying compressed stream
         * @exception IOException if first lines cannot be read
         */
        HatanakaReader(final String name, final Reader input)
            throws IOException {

            reader = new BufferedReader(input);

            // check header
            format = CompactRinexFormat.getFormat(name, reader);

            pending = null;

        }

        /** {@inheritDoc} */
        @Override
        public int read(final char[] b, final int offset, final int len) throws IOException {

            if (pending == null) {
                // we need to read another section from the underlying characters stream and uncompress it
                countOut = 0;
                final String firstLine = reader.readLine();
                if (firstLine == null) {
                    // there are no lines left
                    return -1;
                } else {
                    pending = format.uncompressSection(firstLine);
                }
            }

            // copy as many characters as possible from current line
            int n = FastMath.min(len, pending.length() - countOut);
            for (int i = 0; i < n; ++i) {
                b[offset + i] = pending.charAt(countOut + i);
            }

            if (n < len) {
                // line has been completed and we can still output end of line
                b[offset + n] = '\n';
                pending       = null;
                ++n;
            } else {
                // there are still some pending characters
                countOut += n;
            }

            return n;

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

        /** Uncompressed value. */
        private CharSequence uncompressed;

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
         * @param sequence sequence containing the value to consider
         */
        public void accept(final CharSequence sequence) {

            // store the value as the last component of state vector
            state[nbComponents] = Long.parseLong(sequence.toString());

            // update state vector
            for (int i = nbComponents; i > 0; --i) {
                state[i - 1] += state[i];
            }

            if (++nbComponents == state.length) {
                // the state vector is full
                --nbComponents;
            }

            // output uncompressed value
            final String unscaled = Long.toString(FastMath.abs(state[0]));
            final int    length   = unscaled.length();
            final int    digits   = FastMath.max(length, decimalPlaces);
            final int    padding  = fieldLength - (digits + (state[0] < 0 ? 2 : 1));
            final StringBuilder builder = new StringBuilder();
            for (int i = 0; i < padding; ++i) {
                builder.append(' ');
            }
            if (state[0] < 0) {
                builder.append('-');
            }
            if (length > decimalPlaces) {
                builder.append(unscaled, 0, length - decimalPlaces);
            }
            builder.append('.');
            for (int i = decimalPlaces; i > 0; --i) {
                builder.append(i > length ? '0' : unscaled.charAt(length - i));
            }

            uncompressed = builder;

        }

        /** Get a string representation of the uncompressed value.
         * @return string representation of the uncompressed value
         */
        public CharSequence getUncompressed() {
            return uncompressed;
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
         * @param sequence sequence containing the value to consider
         */
        public void accept(final CharSequence sequence) {

            // update state
            final int length = FastMath.min(state.capacity(), sequence.length());
            for (int i = 0; i < length; ++i) {
                final char c = sequence.charAt(i);
                if (c == '&') {
                    // update state with disappearing character
                    state.put(i, ' ');
                } else if (c != ' ') {
                    // update state with changed character
                    state.put(i, c);
                }
            }

        }

        /** Get a string representation of the uncompressed value.
         * @return string representation of the uncompressed value
         */
        public CharSequence getUncompressed() {
            return state;
        }

    }

    /** Container for combined observations and flags. */
    private static class CombinedDifferentials {

        /** Observation differentials. */
        private NumericDifferential[] observations;

        /** Flags differential. */
        private TextDifferential flags;

        /** Simple constructor.
         * Build an empty container.
         * @param nbObs number of observations
         */
        CombinedDifferentials(final int nbObs) {
            this.observations = new NumericDifferential[nbObs];
            this.flags        = new TextDifferential(2 * nbObs);
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

        /** Current line number. */
        private int lineNumber;

        /** Maximum number of observations for one satellite. */
        private final Map<SatelliteSystem, Integer> maxObs;

        /** Number of satellites. */
        private int nbSat;

        /** Indicator for current section type. */
        private Section section;

        /** Satellites observed at current epoch. */
        private List<String> satellites;

        /** Differential engine for epoch. */
        private TextDifferential epochDifferential;

        /** Receiver clock offset differential. */
        private NumericDifferential clockDifferential;

        /** Differential engine for satellites list. */
        private TextDifferential satListDifferential;

        /** Differential engines for each satellite. */
        private Map<String, CombinedDifferentials> differentials;

        /** Simple constructor.
         * @param name file name
         * @param reader line-oriented input
         */
        protected CompactRinexFormat(final String name, final BufferedReader reader) {
            this.name    = name;
            this.reader  = reader;
            this.maxObs  = new HashMap<>();
            for (final SatelliteSystem system : SatelliteSystem.values()) {
                maxObs.put(system, 0);
            }
            this.nbSat   = DEFAULT_NB_SAT;
            this.section = Section.HEADER;
        }

        /** Uncompress a section.
         * @param firstLine first line of the section
         * @return uncompressed section (contains several lines)
         * @exception IOException if we cannot read lines from underlying stream
         */
        public CharSequence uncompressSection(final String firstLine)
            throws IOException {
            final CharSequence uncompressed;
            switch (section) {

                case HEADER : {
                    // header lines
                    final StringBuilder builder = new StringBuilder();
                    String line = firstLine;
                    lineNumber = 3; // there are 2 CRINEX lines before the RINEX header line
                    while (section == Section.HEADER) {
                        if (builder.length() > 0) {
                            builder.append('\n');
                            line = readLine();
                        }
                        builder.append(parseHeaderLine(line));
                        trimTrailingSpaces(builder);
                    }
                    uncompressed = builder;
                    section      = Section.EPOCH;
                    break;
                }

                case EPOCH : {
                    // epoch and receiver clock offset lines
                    ++lineNumber; // the caller has read one epoch line
                    uncompressed = parseEpochAndClockLines(firstLine, readLine().trim());
                    section      = Section.OBSERVATION;
                    break;
                }

                default : {
                    // observation lines
                    final String[] lines = new String[satellites.size()];
                    ++lineNumber; // the caller has read one observation line
                    lines[0] = firstLine;
                    for (int i = 1; i < lines.length; ++i) {
                        lines[i] = readLine();
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
        public CharSequence parseHeaderLine(final String line) {

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
         * @exception IOException if we cannot read additional special events lines
         */
        public abstract CharSequence parseEpochAndClockLines(String epochLine, String clockLine)
            throws IOException;

        /** Parse epoch and receiver clock offset lines.
         * @param builder builder that may used to copy special event lines
         * @param epochStart start of the epoch field
         * @param epochLength length of epoch field
         * @param eventStart start of the special events field
         * @param nbSatStart start of the number of satellites field
         * @param satListStart start of the satellites list
         * @param clockLength length of receiver clock field
         * @param clockDecimalPlaces number of decimal places for receiver clock offset
         * @param epochLine epoch line
         * @param clockLine receiver clock offset line
         * @param resetChar character indicating differentials reset
         * @exception IOException if we cannot read additional special events lines
         */
        protected void doParseEpochAndClockLines(final StringBuilder builder,
                                                 final int epochStart, final int epochLength,
                                                 final int eventStart, final int nbSatStart, final int satListStart,
                                                 final int clockLength, final int clockDecimalPlaces,
                                                 final String epochLine,
                                                 final String clockLine, final char resetChar)
            throws IOException {

            boolean loop = true;
            String loopEpochLine = epochLine;
            String loopClockLine = clockLine;
            while (loop) {

                // check if differentials should be reset
                if (epochDifferential == null || loopEpochLine.charAt(0) == resetChar) {
                    epochDifferential   = new TextDifferential(epochLength);
                    satListDifferential = new TextDifferential(nbSat * 3);
                    differentials       = new HashMap<>();
                }

                // check for special events
                epochDifferential.accept(loopEpochLine.subSequence(epochStart,
                                                                   FastMath.min(loopEpochLine.length(), epochStart + epochLength)));
                if (parseInt(epochDifferential.getUncompressed(), eventStart, 1) > 1) {
                    // this was not really the epoch, but rather a special event
                    // we just copy the lines and skip to real epoch and clock lines
                    builder.append(epochDifferential.getUncompressed());
                    trimTrailingSpaces(builder);
                    builder.append('\n');
                    final int skippedLines = parseInt(epochDifferential.getUncompressed(), nbSatStart, 3);
                    for (int i = 0; i < skippedLines; ++i) {
                        builder.append(loopClockLine);
                        trimTrailingSpaces(builder);
                        builder.append('\n');
                        loopClockLine = readLine();
                    }

                    // the epoch and clock are in the next lines
                    loopEpochLine = loopClockLine;
                    loopClockLine = readLine();
                    loop = true;

                } else {
                    loop = false;
                    final int n = parseInt(epochDifferential.getUncompressed(), nbSatStart, 3);
                    satellites = new ArrayList<>(n);
                    if (satListStart < loopEpochLine.length()) {
                        satListDifferential.accept(loopEpochLine.subSequence(satListStart, loopEpochLine.length()));
                    }
                    final CharSequence satListPart = satListDifferential.getUncompressed();
                    for (int i = 0; i < n; ++i) {
                        satellites.add(satListPart.subSequence(i * 3, (i + 1) * 3).toString());
                    }

                    // parse clock offset
                    if (!loopClockLine.isEmpty()) {
                        if (loopClockLine.length() > 2 && loopClockLine.charAt(1) == '&') {
                            clockDifferential = new NumericDifferential(clockLength, clockDecimalPlaces, parseInt(loopClockLine, 0, 1));
                            clockDifferential.accept(loopClockLine.subSequence(2, loopClockLine.length()));
                        } else if (clockDifferential == null) {
                            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                      lineNumber, name, loopClockLine);
                        } else {
                            clockDifferential.accept(loopClockLine);
                        }
                    }
                }
            }

        }

        /** Get the uncompressed epoch part.
         * @return uncompressed epoch part
         */
        protected CharSequence getEpochPart() {
            return epochDifferential.getUncompressed();
        }

        /** Get the uncompressed clock part.
         * @return uncompressed clock part
         */
        protected CharSequence getClockPart() {
            return clockDifferential == null ? "" : clockDifferential.getUncompressed();
        }

        /** Get the satellites for current observations.
         * @return satellites for current observation
         */
        protected List<String> getSatellites() {
            return satellites;
        }

        /** Get the combined differentials for one satellite.
         * @param sat satellite id
         * @return observationDifferentials
         */
        protected CombinedDifferentials getCombinedDifferentials(final CharSequence sat) {
            return differentials.get(sat);
        }

        /** Parse observation lines.
         * @param observationLines observation lines
         * @return uncompressed lines
         */
        public abstract CharSequence parseObservationLines(String[] observationLines);

        /** Parse observation lines.
         * @param dataLength length of data fields
         * @param dataDecimalPlaces number of decimal places for data fields
         * @param observationLines observation lines
         */
        protected void doParseObservationLines(final int dataLength, final int dataDecimalPlaces,
                                               final String[] observationLines) {

            for (int i = 0; i < observationLines.length; ++i) {

                final CharSequence line = observationLines[i];

                // get the differentials associated with this observations line
                final String sat = satellites.get(i);
                CombinedDifferentials satDiffs = differentials.get(sat);
                if (satDiffs == null) {
                    final SatelliteSystem system = SatelliteSystem.parseSatelliteSystem(sat.subSequence(0, 1).toString());
                    satDiffs = new CombinedDifferentials(maxObs.get(system));
                    differentials.put(sat, satDiffs);
                }

                // parse observations
                int k = 0;
                for (int j = 0; j < satDiffs.observations.length; ++j) {

                    if (k >= line.length() || line.charAt(k) == ' ') {
                        // the data field is missing
                        satDiffs.observations[j] = null;
                    } else {
                        // the data field is present

                        if (k + 1 < line.length() &&
                            Character.isDigit(line.charAt(k)) &&
                            line.charAt(k + 1) == '&') {
                            // reinitialize differentials
                            satDiffs.observations[j] = new NumericDifferential(dataLength, dataDecimalPlaces,
                                                                               Character.digit(line.charAt(k), 10));
                            k += 2;
                        }

                        // extract the compressed differenced value
                        final int start = k;
                        while (k < line.length() && line.charAt(k) != ' ') {
                            ++k;
                        }
                        try {
                            satDiffs.observations[j].accept(line.subSequence(start, k));
                        } catch (NumberFormatException nfe) {
                            throw new OrekitException(nfe,
                                                      OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                      lineNumber + i - (observationLines.length - 1),
                                                      name, observationLines[i]);
                        }

                    }

                    // skip blank separator
                    ++k;

                }

                if (k < line.length()) {
                    satDiffs.flags.accept(line.subSequence(k, line.length()));
                }

            }

        }

        /** Check if a line corresponds to a header.
         * @param label header label
         * @param line header line
         * @return true if line corresponds to header
         */
        protected boolean isHeaderLine(final String label, final String line) {
            return label.equals(parseString(line, LABEL_START, label.length()));
        }

        /** Update the max number of observations.
         * @param system satellite system
         * @param nbObs number of observations
         */
        protected void updateMaxObs(final SatelliteSystem system, final int nbObs) {
            maxObs.put(system, FastMath.max(maxObs.get(system), nbObs));
        }

        /** Read a new line.
         * @return line read
         * @exception IOException if a read error occurs
         */
        private String readLine()
            throws IOException {
            final String line = reader.readLine();
            if (line == null) {
                throw new OrekitException(OrekitMessages.UNEXPECTED_END_OF_FILE, name);
            }
            lineNumber++;
            return line;
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
            if (line1 == null || line2 == null) {
                throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_HATANAKA_COMPRESSED_FILE, name);
            }

            // extract format version
            final int cVersion100 = (int) FastMath.rint(100 * parseDouble(line1, 0, 9));
            if (cVersion100 != 100 && cVersion100 != 300) {
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
        public static String parseString(final CharSequence line, final int start, final int length) {
            if (line.length() > start) {
                return line.subSequence(start, FastMath.min(line.length(), start + length)).toString().trim();
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
        public static int parseInt(final CharSequence line, final int start, final int length) {
            if (line.length() > start && parseString(line, start, length).length() > 0) {
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
        public static double parseDouble(final CharSequence line, final int start, final int length) {
            if (line.length() > start && parseString(line, start, length).length() > 0) {
                return Double.parseDouble(parseString(line, start, length));
            } else {
                return Double.NaN;
            }
        }

        /** Trim trailing spaces in a builder.
         * @param builder builder to trim
         */
        public static void trimTrailingSpaces(final StringBuilder builder) {
            for (int i = builder.length() - 1; i >= 0 && builder.charAt(i) == ' '; --i) {
                builder.deleteCharAt(i);
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

        /** Start of events flag. */
        private static final int    EVENT_START          = EPOCH_START + EPOCH_LENGTH - 4;

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

        /** Number of decimal places for data fields. */
        private static final int    DATA_DECIMAL_PLACES  = 3;

        /** Simple constructor.
         * @param name file name
         * @param reader line-oriented input
         */
        CompactRinex1(final String name, final BufferedReader reader) {
            super(name, reader);
        }

        @Override
        /** {@inheritDoc} */
        public CharSequence parseHeaderLine(final String line) {
            if (isHeaderLine(NB_TYPES_OF_OBSERV, line)) {
                for (final SatelliteSystem system : SatelliteSystem.values()) {
                    updateMaxObs(system, parseInt(line, 0, 6));
                }
                return line;
            } else {
                return super.parseHeaderLine(line);
            }
        }

        @Override
        /** {@inheritDoc} */
        public CharSequence parseEpochAndClockLines(final String epochLine, final String clockLine)
            throws IOException {

            final StringBuilder builder = new StringBuilder();
            doParseEpochAndClockLines(builder,
                                      EPOCH_START, EPOCH_LENGTH, EVENT_START, NB_SAT_START, SAT_LIST_START,
                                      CLOCK_LENGTH, CLOCK_DECIMAL_PLACES, epochLine,
                                      clockLine, '&');

            // build uncompressed lines, taking care of clock being put
            // back in line 1 and satellites after 12th put in continuation lines
            final List<String> satellites = getSatellites();
            builder.append(getEpochPart());
            int iSat = 0;
            while (iSat < FastMath.min(satellites.size(), MAX_SAT_EPOCH_LINE)) {
                builder.append(satellites.get(iSat++));
            }
            if (getClockPart().length() > 0) {
                while (builder.length() < CLOCK_START) {
                    builder.append(' ');
                }
                builder.append(getClockPart());
            }

            while (iSat < satellites.size()) {
                // add a continuation line
                trimTrailingSpaces(builder);
                builder.append('\n');
                for (int k = 0; k < SAT_LIST_START; ++k) {
                    builder.append(' ');
                }
                final int iSatStart = iSat;
                while (iSat < FastMath.min(satellites.size(), iSatStart + MAX_SAT_EPOCH_LINE)) {
                    builder.append(satellites.get(iSat++));
                }
            }
            trimTrailingSpaces(builder);
            return builder;

        }

        @Override
        /** {@inheritDoc} */
        public CharSequence parseObservationLines(final String[] observationLines) {

            // parse the observation lines
            doParseObservationLines(DATA_LENGTH, DATA_DECIMAL_PLACES, observationLines);

            // build uncompressed lines
            final StringBuilder builder = new StringBuilder();
            for (final CharSequence sat : getSatellites()) {
                if (builder.length() > 0) {
                    trimTrailingSpaces(builder);
                    builder.append('\n');
                }
                final CombinedDifferentials cd    = getCombinedDifferentials(sat);
                final CharSequence          flags = cd.flags.getUncompressed();
                for (int i = 0; i < cd.observations.length; ++i) {
                    if (i > 0 && i % 5 == 0) {
                        trimTrailingSpaces(builder);
                        builder.append('\n');
                    }
                    if (cd.observations[i] == null) {
                        // missing observation
                        for (int j = 0; j < DATA_LENGTH + 2; ++j) {
                            builder.append(' ');
                        }
                    } else {
                        builder.append(cd.observations[i].getUncompressed());
                        if (2 * i < flags.length()) {
                            builder.append(flags.charAt(2 * i));
                        }
                        if (2 * i + 1 < flags.length()) {
                            builder.append(flags.charAt(2 * i + 1));
                        }
                    }
                }
            }
            trimTrailingSpaces(builder);
            return builder;

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

        /** Start of events flag. */
        private static final int    EVENT_START          = EPOCH_START + EPOCH_LENGTH - 10;

        /** Start of number of satellites field. */
        private static final int    NB_SAT_START         = EPOCH_START + EPOCH_LENGTH - 9;

        /** Start of satellites list field (only in the compact rinex). */
        private static final int    SAT_LIST_START       = EPOCH_START + EPOCH_LENGTH;

        /** Length of a data field. */
        private static final int    DATA_LENGTH          = 14;

        /** Number of decimal places for data fields. */
        private static final int    DATA_DECIMAL_PLACES  = 3;

        /** Simple constructor.
         * @param name file name
         * @param reader line-oriented input
         */
        CompactRinex3(final String name, final BufferedReader reader) {
            super(name, reader);
        }

        @Override
        /** {@inheritDoc} */
        public CharSequence parseHeaderLine(final String line) {
            if (isHeaderLine(SYS_NB_OBS_TYPES, line)) {
                if (line.charAt(0) != ' ') {
                    // it is the first line of an observation types description
                    // (continuation lines are ignored here)
                    updateMaxObs(SatelliteSystem.parseSatelliteSystem(parseString(line, 0, 1)),
                                 parseInt(line, 1, 5));
                }
                return line;
            } else {
                return super.parseHeaderLine(line);
            }
        }

        @Override
        /** {@inheritDoc} */
        public CharSequence parseEpochAndClockLines(final String epochLine, final String clockLine)
            throws IOException {

            final StringBuilder builder = new StringBuilder();
            doParseEpochAndClockLines(builder,
                                      EPOCH_START, EPOCH_LENGTH, EVENT_START, NB_SAT_START, SAT_LIST_START,
                                      CLOCK_LENGTH, CLOCK_DECIMAL_PLACES, epochLine,
                                      clockLine, '>');

            // build uncompressed line
            builder.append(getEpochPart());
            if (getClockPart().length() > 0) {
                while (builder.length() < CLOCK_START) {
                    builder.append(' ');
                }
                builder.append(getClockPart());
            }

            trimTrailingSpaces(builder);
            return builder;

        }

        @Override
        /** {@inheritDoc} */
        public CharSequence parseObservationLines(final String[] observationLines) {

            // parse the observation lines
            doParseObservationLines(DATA_LENGTH, DATA_DECIMAL_PLACES, observationLines);

            // build uncompressed lines
            final StringBuilder builder = new StringBuilder();
            for (final CharSequence sat : getSatellites()) {
                if (builder.length() > 0) {
                    trimTrailingSpaces(builder);
                    builder.append('\n');
                }
                builder.append(sat);
                final CombinedDifferentials cd    = getCombinedDifferentials(sat);
                final CharSequence          flags = cd.flags.getUncompressed();
                for (int i = 0; i < cd.observations.length; ++i) {
                    if (cd.observations[i] == null) {
                        // missing observation
                        for (int j = 0; j < DATA_LENGTH + 2; ++j) {
                            builder.append(' ');
                        }
                    } else {
                        builder.append(cd.observations[i].getUncompressed());
                        if (2 * i < flags.length()) {
                            builder.append(flags.charAt(2 * i));
                        }
                        if (2 * i + 1 < flags.length()) {
                            builder.append(flags.charAt(2 * i + 1));
                        }
                    }
                }
            }
            trimTrailingSpaces(builder);
            return builder;

        }

    }

}

/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.files.rinex.utils;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.section.CommonLabel;
import org.orekit.files.rinex.section.Label;
import org.orekit.files.rinex.section.RinexBaseHeader;
import org.orekit.files.rinex.section.RinexComment;
import org.orekit.time.DateTimeComponents;
import org.orekit.utils.formatting.FastDecimalFormatter;
import org.orekit.utils.formatting.FastDoubleFormatter;
import org.orekit.utils.formatting.FastLongFormatter;
import org.orekit.utils.formatting.FastScientificFormatter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/** Base write for Rinex files.
 * @param <T> type of the header
 * @author Luc Maisonobe
 * @since 14.0
 */
public abstract class BaseRinexWriter<T extends RinexBaseHeader> {

    /** Format for one 2 digits integer field. */
    public static final FastLongFormatter TWO_DIGITS_INTEGER = new FastLongFormatter(2, false);

    /** Format for one 2 digits integer field. */
    public static final FastLongFormatter PADDED_TWO_DIGITS_INTEGER = new FastLongFormatter(2, true);

    /** Format for one 3 digits integer field. */
    public static final FastLongFormatter THREE_DIGITS_INTEGER = new FastLongFormatter(3, false);

    /** Format for one 4 digits integer field. */
    public static final FastLongFormatter FOUR_DIGITS_INTEGER = new FastLongFormatter(4, false);

    /** Format for one 4 digits integer field. */
    public static final FastLongFormatter PADDED_FOUR_DIGITS_INTEGER = new FastLongFormatter(4, true);

    /** Format for one 6 digits integer field. */
    public static final FastLongFormatter SIX_DIGITS_INTEGER = new FastLongFormatter(6, false);

    /** Format for one 9.2 digits float field. */
    public static final FastDoubleFormatter NINE_TWO_DIGITS_FLOAT = new FastDecimalFormatter(9, 2);

    /** Format for one 19 characters wide field in scientific notation. */
    public static final FastDoubleFormatter NINETEEN_SCIENTIFIC_FLOAT = new FastScientificFormatter(19);

    /** Destination of generated output. */
    private final Appendable output;

    /** Output name for error messages. */
    private final String outputName;

    /** Saved header. */
    private T savedHeader;

    /** Index of the labels in header lines. */
    private int savedLabelIndex;

    /** Saved comments. */
    private List<RinexComment> savedComments;

    /** Line number. */
    private int lineNumber;

    /** Column number. */
    private int column;

    /** Simple constructor.
     * @param output destination of generated output
     * @param outputName output name for error messages
     */
    public BaseRinexWriter(final Appendable output, final String outputName) {
        this.output        = output;
        this.outputName    = outputName;
        this.savedComments = Collections.emptyList();
    }

    /** Prepare comments to be emitted at specified lines.
     * @param comments comments to be emitted
     */
    public void prepareComments(final List<RinexComment> comments) {
        savedComments = comments;
    }

    /** Write header.
     * @param header     header to write
     * @param labelIndex index of the label in header
     * @exception IOException if an I/O error occurs.
     */
    protected void writeHeader(final T header, final int labelIndex) throws IOException {

        // check header is written exactly once
        if (savedHeader != null) {
            throw new OrekitException(OrekitMessages.HEADER_ALREADY_WRITTEN, outputName);
        }
        savedHeader     = header;
        savedLabelIndex = labelIndex;
        lineNumber      = 1;

    }

    /** Get the header.
     * @return header
     */
    protected T getHeader() {
        return savedHeader;
    }

    /** Get column number.
     * @return column number
     */
    protected int getColumn() {
        return column;
    }

    /** Finish one header line.
     * @param label line label
     * @throws IOException if an I/O error occurs.
     */
    protected void finishHeaderLine(final Label label) throws IOException {
        for (int i = column; i < savedLabelIndex; ++i) {
            output.append(' ');
        }
        output.append(label.getLabel());
        finishLine();
    }

    /** Finish one line.
     * @throws IOException if an I/O error occurs.
     */
    public void finishLine() throws IOException {

        // pending line
        output.append(System.lineSeparator());
        lineNumber++;
        column = 0;

        // emit comments that should be placed at next lines
        for (final RinexComment comment : savedComments) {
            if (comment.getLineNumber() == lineNumber) {
                outputField(comment.getText(), savedLabelIndex, true);
                output.append(CommonLabel.COMMENT.getLabel());
                output.append(System.lineSeparator());
                lineNumber++;
                column = 0;
            } else if (comment.getLineNumber() > lineNumber) {
                break;
            }
        }

    }

    /** Write one header string.
     * @param s string data (may be null)
     * @param label line label
     * @throws IOException if an I/O error occurs.
     */
    protected void writeHeaderLine(final String s, final Label label) throws IOException {
        if (s != null) {
            outputField(s, s.length(), true);
            finishHeaderLine(label);
        }
    }

    /** Check header has been written.
     */
    protected void checkHeaderWritten() {
        if (savedHeader == null) {
            throw new OrekitException(OrekitMessages.HEADER_NOT_WRITTEN, outputName);
        }
    }

    /** Check if column exceeds header line length.
     * @param tentative tentative column number
     * @return true if tentative column number exceeds header line length
     */
    protected boolean exceedsHeaderLength(final int tentative) {
        return tentative > savedLabelIndex;
    }

    /** Write the PGM / RUN BY / DATE header line.
     * @param header header to write
     * @throws IOException if an I/O error occurs.
     */
    protected void writeProgramRunByDate(final RinexBaseHeader header)
        throws IOException {
        outputField(header.getProgramName(), 20, true);
        outputField(header.getRunByName(),   40, true);
        final DateTimeComponents dtc = header.getCreationDateComponents();
        if (header.getFormatVersion() < 3.0 && dtc.getTime().getSecond() < 0.5) {
            outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getDate().getDay(), 42);
            outputField('-', 43);
            outputField(dtc.getDate().getMonthEnum().getUpperCaseAbbreviation(), 46,  true);
            outputField('-', 47);
            outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getDate().getYear() % 100, 49);
            outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getTime().getHour(), 52);
            outputField(':', 53);
            outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getTime().getMinute(), 55);
            outputField(header.getCreationTimeZone(), 58, true);
        } else {
            outputField(PADDED_FOUR_DIGITS_INTEGER, dtc.getDate().getYear(), 44);
            outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getDate().getMonth(), 46);
            outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getDate().getDay(), 48);
            outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getTime().getHour(), 51);
            outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getTime().getMinute(), 53);
            outputField(PADDED_TWO_DIGITS_INTEGER, (int) FastMath.rint(dtc.getTime().getSecond()), 55);
            outputField(header.getCreationTimeZone(), 59, false);
        }
        finishHeaderLine(CommonLabel.PROGRAM);
    }

    /** Output one single character field.
     * @param c field value
     * @param next target column for next field
     * @throws IOException if an I/O error occurs.
     */
    public void outputField(final char c, final int next) throws IOException {
        outputField(Character.toString(c), next, false);
    }

    /** Output one integer field.
     * @param formatter formatter to use
     * @param value field value
     * @param next target column for next field
     * @throws IOException if an I/O error occurs.
     */
    public void outputField(final FastLongFormatter formatter, final int value, final int next) throws IOException {
        outputField(formatter.toString(value), next, false);
    }

    /** Output one long integer field.
     * @param formatter formatter to use
     * @param value field value
     * @param next target column for next field
     * @throws IOException if an I/O error occurs.
     */
    public void outputField(final FastLongFormatter formatter, final long value, final int next) throws IOException {
        outputField(formatter.toString(value), next, false);
    }

    /** Output one double field.
     * @param formatter formatter to use
     * @param value field value
     * @param next target column for next field
     * @throws IOException if an I/O error occurs.
     */
    public void outputField(final FastDoubleFormatter formatter, final double value, final int next) throws IOException {
        if (Double.isNaN(value)) {
            // NaN values are replaced by blank fields
            outputField("", next, true);
        } else {
            outputField(formatter.toString(value), next, false);
        }
    }

    /** Output one field.
     * @param field field to output
     * @param next target column for next field
     * @param leftJustified if true, field is left-justified
     * @throws IOException if an I/O error occurs.
     */
    public void outputField(final String field, final int next, final boolean leftJustified) throws IOException {
        final int padding = next - (field == null ? 0 : field.length()) - column;
        if (padding < 0) {
            throw new OrekitException(OrekitMessages.FIELD_TOO_LONG, field, next - column);
        }
        if (leftJustified && field != null) {
            output.append(field);
        }
        for (int i = 0; i < padding; ++i) {
            output.append(' ');
        }
        if (!leftJustified && field != null) {
            output.append(field);
        }
        column = next;
    }

}

/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.utils.lexical;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.CcsdsFrame;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.files.ccsds.utils.CenterName;
import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/** Token occurring during CCSDS file parsing.
 * <p>
 * Parse tokens correspond to:
 * <ul>
 *   <li>bloc or entry start</li>
 *   <li>entry content</li>
 *   <li>bloc or entry end</li>
 *   <li>raw lines</li>
 * </ul>
 * </p>
 * @see MessageParser
 * @author Luc Maisonobe
 * @since 11.0
 */
public class ParseToken {

    /** Pattern for dash. */
    private static final Pattern DASH = Pattern.compile("-");

    /** Pattern for spaces. */
    private static final Pattern SPACE = Pattern.compile("\\p{Space}+");

    /** Pattern for splitting comma-separated lists. */
    private static final Pattern SPLIT_AT_COMMAS = Pattern.compile("\\p{Space}*,\\p{Space}*");

    /** Type of the token. */
    private TokenType type;

    /** Name of the entry. */
    private final String name;

    /** Entry content. */
    private final String content;

    /** Units of the entry (may be null). */
    private final String units;

    /** Number of the line from which pair is extracted. */
    private final int lineNumber;

    /** Name of the file. */
    private final String fileName;

    /** Simple constructor.
     * @param type type of the token
     * @param name name of the block or entry
     * @param content entry content
     * @param units units of the entry (may be null)
     * @param lineNumber number of the line in the CCSDS data message
     * @param fileName name of the file
     */
    protected ParseToken(final TokenType type, final String name, final String content, final String units,
                         final int lineNumber, final String fileName) {
        this.type       = type;
        this.name       = name;
        this.content    = content;
        this.units      = units;
        this.lineNumber = lineNumber;
        this.fileName   = fileName;
    }

    /** Get the type of the token.
     * @return type of the token
     */
    public TokenType getType() {
        return type;
    }

    /** Get the name of the block or entry.
     * @return name of the block or entry
     */
    public String getName() {
        return name;
    }

    /** Get the content of the entry.
     * @return entry content
     */
    public String getContent() {
        return content;
    }

    /** Get the content of the entry as a list of free-text strings.
     * @return content of the entry as a list of free-test strings
     */
    public List<String> getContentAsFreeTextStringList() {
        return Arrays.asList(SPLIT_AT_COMMAS.split(getContent()));
    }

    /** Get the normalized content of the entry.
     * <p>
     * Normalized strings are all uppercase,
     * have '_' characters replaced by spaces,
     * and have multiple spaces collapsed as one space only.
     * </p>
     * @return entry normalized content
     */
    public String getContentAsNormalizedString() {
        return SPACE.matcher(content.replace('_', ' ')).replaceAll(" ").trim().toUpperCase(Locale.US);
    }

    /** Get the content of the entry as a list of normalized strings.
     * @return content of the entry as a list of normalized strings
     */
    public List<String> getContentAsNormalizedStringList() {
        return Arrays.asList(SPLIT_AT_COMMAS.split(getContentAsNormalizedString()));
    }

    /** Get the content of the entry as a double.
     * @return content as a double
     */
    public double getContentAsDouble() {
        try {
            return Double.parseDouble(content);
        } catch (NumberFormatException nfe) {
            throw generateException(nfe);
        }
    }

    /** Get the content of the entry as an angle.
     * @return content as an angle
     */
    public double getContentAsAngle() {
        return FastMath.toRadians(getContentAsDouble());
    }

    /** Get the content of the entry as an integer.
     * @return content as an integer
     */
    public int getContentAsInt() {
        try {
            return Integer.parseInt(content);
        } catch (NumberFormatException nfe) {
            throw generateException(nfe);
        }
    }

    /** Get the content of the entry as a normalized character.
     * @return content as a normalized character
     */
    public char getContentAsNormalizedCharacter() {
        try {
            return getContentAsNormalizedString().charAt(0);
        } catch (NumberFormatException nfe) {
            throw generateException(nfe);
        }
    }

    /** Get the units.
     * @return units of the entry (may be null)
     */
    public String getUnits() {
        return units;
    }

    /** Get the number of the line in the CCSDS data message.
     * @return number of the line in the CCSDS data message
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /** Get the name of the file.
     * @return name of the file
     */
    public String getFileName() {
        return fileName;
    }

    /** Process the content as a normalized string.
     * @param consumer consumer of the normalized string
     * @return always returns {@code true}
     * @see #processAsFreeTextString(StringConsumer)
     */
    public boolean processAsNormalizedString(final StringConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(getContentAsNormalizedString());
        }
        return true;
    }

    /** Process the content as an indexed free text string.
     * @param index index
     * @param consumer consumer of the indexed free test string
     * @return always returns {@code true}
     */
    public boolean processAsIndexedFreeTextString(final int index, final IndexedStringConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(index, getContent());
        }
        return true;
    }

    /** Process the content as an indexed normalized string.
     * @param index index
     * @param consumer consumer of the indexed normalized string
     * @return always returns {@code true}
     */
    public boolean processAsIndexedNormalizedString(final int index, final IndexedStringConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(index, getContentAsNormalizedString());
        }
        return true;
    }

    /** Process the content as a list of normalized strings.
     * @param consumer consumer of the normalized strings list
     * @return always returns {@code true}
     */
    public boolean processAsNormalizedStringList(final StringListConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(getContentAsNormalizedStringList());
        }
        return true;
    }

    /** Process the content as an integer.
     * @param consumer consumer of the integer
     * @return always returns {@code true}
     */
    public boolean processAsInteger(final IntConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(getContentAsInt());
        }
        return true;
    }

    /** Process the content as a normalized character.
     * @param consumer consumer of the normalized character
     * @return always returns {@code true}
     */
    public boolean processAsNormalizedCharacter(final CharConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(getContentAsNormalizedCharacter());
        }
        return true;
    }

    /** Process the content as a double.
     * @param scalingFactor scaling factor to apply
     * @param consumer consumer of the double
     * @return always returns {@code true}
     */
    public boolean processAsDouble(final double scalingFactor, final DoubleConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(scalingFactor * getContentAsDouble());
        }
        return true;
    }

    /** Process the content as an indexed double.
     * @param i index
     * @param scalingFactor scaling factor to apply
     * @param consumer consumer of the indexed double
     * @return always returns {@code true}
     */
    public boolean processAsIndexedDouble(final int i, final double scalingFactor,
                                          final IndexedDoubleConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(i, scalingFactor * getContentAsDouble());
        }
        return true;
    }

    /** Process the content as a doubly-indexed double.
     * @param i first index
     * @param j second index
     * @param scalingFactor scaling factor to apply
     * @param consumer consumer of the doubly-indexed double
     * @return always returns {@code true}
     */
    public boolean processAsDoublyIndexedDouble(final int i, final int j, final double scalingFactor,
                                                final DoublyIndexedDoubleConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(i, j, scalingFactor * getContentAsDouble());
        }
        return true;
    }

    /** Process the content as an angle (i.e. converting from degrees to radians upon reading).
     * @param consumer consumer of the angle in radians
     * @return always returns {@code true}
     */
    public boolean processAsAngle(final DoubleConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(getContentAsAngle());
        }
        return true;
    }

    /** Process the content as an indexed angle.
     * @param index index
     * @param consumer consumer of the indexed angle
     * @return always returns {@code true}
     */
    public boolean processAsIndexedAngle(final int index, final IndexedDoubleConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(index, FastMath.toRadians(getContentAsDouble()));
        }
        return true;
    }

    /** Process the content as a date.
     * @param consumer consumer of the date
     * @param context parsing context
     * @return always returns {@code true} (or throws an exception)
     */
    public boolean processAsDate(final DateConsumer consumer, final ParsingContext context) {
        if (type == TokenType.ENTRY) {
            if (context.getTimeScale() == null) {
                throw new OrekitException(OrekitMessages.CCSDS_TIME_SYSTEM_NOT_READ_YET,
                                          getLineNumber(), getFileName());
            }
            consumer.accept(context.getTimeScale().parseDate(content, context));
        }
        return true;
    }

    /** Process the content as a time scale.
     * @param consumer consumer of the time scale
     * @return always returns {@code true} (or throws an exception)
     */
    public boolean processAsTimeScale(final TimeScaleConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(CcsdsTimeScale.parse(content));
        }
        return true;
    }

    /** Process the content as a frame.
     * @param consumer consumer of the frame
     * @param context parsing context
     * @param allowLOF if true, {@link CcsdsFrame#isLof() Local Orbital Frames} are allowed
     * @return always returns {@code true}
     */
    public boolean processAsFrame(final FrameConsumer consumer,
                                  final ParsingContext context,
                                  final boolean allowLOF) {
        if (type == TokenType.ENTRY) {
            try {
                final CcsdsFrame frame = CcsdsFrame.valueOf(DASH.matcher(content).replaceAll(""));
                consumer.accept(allowLOF && frame.isLof() ?
                                null : frame.getFrame(context.getConventions(),
                                                      context.isSimpleEOP(),
                                                      context.getDataContext()),
                                frame);
            } catch (IllegalArgumentException iae) {
                throw generateException(iae);
            }
        }
        return true;
    }

    /** Process the content as a body center.
     * @param consumer consumer of the body center
     * @param complainIfUnknown if true, unknown centers generate an exception, otherwise
     * they are silently ignored
     * @return always returns {@code true}
     */
    public boolean processAsCenter(final CenterConsumer consumer, final boolean complainIfUnknown) {
        if (type == TokenType.ENTRY) {
            String canonicalValue = getContentAsNormalizedString();
            if (canonicalValue.equals("SOLAR SYSTEM BARYCENTER") || canonicalValue.equals("SSB")) {
                canonicalValue = "SOLAR_SYSTEM_BARYCENTER";
            } else if (canonicalValue.equals("EARTH MOON BARYCENTER") || canonicalValue.equals("EARTH-MOON BARYCENTER") ||
                       canonicalValue.equals("EARTH BARYCENTER") || canonicalValue.equals("EMB")) {
                canonicalValue = "EARTH_MOON";
            }
            try {
                consumer.accept(CenterName.valueOf(canonicalValue));
            } catch (IllegalArgumentException iae) {
                if (complainIfUnknown) {
                    throw generateException(iae);
                }
            }
        }
        return true;
    }

    /** Generate a parse exception for this entry.
     * @param cause underlying cause exception (may be null)
     * @return exception for this entry
     */
    public OrekitException generateException(final Exception cause) {
        return new OrekitException(cause, OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE,
                                   getName(), getLineNumber(), getFileName());
    }

    /** Interface representing instance methods that consume string values. */
    public interface StringConsumer {
        /** Consume a string.
         * @param value value to consume
         */
        void accept(String value);
    }

    /** Interface representing instance methods that consume indexed string values. */
    public interface IndexedStringConsumer {
        /** Consume an indexed string.
         * @param index index
         * @param value value to consume
         */
        void accept(int index, String value);
    }

    /** Interface representing instance methods that consume lists of strings values. */
    public interface StringListConsumer {
        /** Consume a list of strings.
         * @param value value to consume
         */
        void accept(List<String> value);
    }

    /** Interface representing instance methods that consume integer values. */
    public interface IntConsumer {
        /** Consume an integer.
         * @param value value to consume
         */
        void accept(int value);
    }

    /** Interface representing instance methods that consume character values. */
    public interface CharConsumer {
        /** Consume a character.
         * @param value value to consume
         */
        void accept(char value);
    }

    /** Interface representing instance methods that consume double values. */
    public interface DoubleConsumer {
        /** Consume a double.
         * @param value value to consume
         */
        void accept(double value);
    }

    /** Interface representing instance methods that consume indexed double values. */
    public interface IndexedDoubleConsumer {
        /** Consume an indexed double.
         * @param i index
         * @param value value to consume
         */
        void accept(int i, double value);
    }

    /** Interface representing instance methods that consume doubly-indexed double values. */
    public interface DoublyIndexedDoubleConsumer {
        /** Consume a doubly indexed double.
         * @param i first index
         * @param j second index
         * @param value value to consume
         */
        void accept(int i, int j, double value);
    }

    /** Interface representing instance methods that consume date values. */
    public interface DateConsumer {
        /** Consume a data.
         * @param value value to consume
         */
        void accept(AbsoluteDate value);
    }

    /** Interface representing instance methods that consume time systems values. */
    public interface TimeScaleConsumer {
        /** Consume a time system.
         * @param value value to consume
         */
        void accept(CcsdsTimeScale value);
    }

    /** Interface representing instance methods that consume frame values. */
    public interface FrameConsumer {
        /** Consume a frame.
         * @param frame Orekit frame
         * @param ccsdsFrame CCSDS frame
         */
        void accept(Frame frame, CcsdsFrame ccsdsFrame);
    }

    /** Interface representing instance methods that consume center values. */
    public interface CenterConsumer {
        /** Consume a body center.
         * @param value value to consume
         */
        void accept(CenterName value);
    }

}

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
import java.util.regex.Pattern;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.ParsingContext;
import org.orekit.files.ccsds.utils.CCSDSFrame;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.files.ccsds.utils.CenterName;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/** Event occurring during CCSDS file parsing.
 * <p>
 * Parse events correspond to:
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
public class ParseEvent {

    /** Pattern for dash. */
    private static final Pattern DASH = Pattern.compile("-");

    /** Pattern for spaces. */
    private static final Pattern SPACE = Pattern.compile("\\p{Space}+");

    /** Regular expression for splitting comma-separated lists. */
    private final String COMMA_SEPARATORS = "\\p{Space}*,\\p{Space}*";

    /** Type of the event. */
    private EventType type;

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
     * @param type type of the event
     * @param name name of the block or entry
     * @param content entry content
     * @param units units of the entry (may be null)
     * @param lineNumber number of the line in the CCSDS data message
     * @param fileName name of the file
     */
    protected ParseEvent(final EventType type, final String name, final String content, final String units,
                         final int lineNumber, final String fileName) {
        this.type       = type;
        this.name       = name;
        this.content    = content;
        this.units      = units;
        this.lineNumber = lineNumber;
        this.fileName   = fileName;
    }

    /** Get the type of the event.
     * @return type of the event
     */
    public EventType getType() {
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

    /** Get the normalized content of the entry.
     * <p>
     * Normalized strings have '_' characters replaced by spaces,
     * and multiple spaces collapsed as one space only.
     * </p>
     * @return entry normalized content
     */
    public String getNormalizedContent() {
        return SPACE.matcher(content.replace('_', ' ')).replaceAll(" ");
    }

    /** Get the content of the entry as a double.
     * @return content as a double
     */
    public double getContentAsDouble() {
        try {
            return Double.parseDouble(content);
        } catch (NumberFormatException nfe) {
            throw generateException();
        }
    }

    /** Get the content of the entry as an integer.
     * @return content as an integer
     */
    public int getContentAsInt() {
        try {
            return Integer.parseInt(content);
        } catch (NumberFormatException nfe) {
            throw generateException();
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

    /** Process the content as a free-text string.
     * @param consumer consumer of the free-text string
     * @see #processAsNormalizedString(StringConsumer)
     */
    public void processAsFreeTextString(final StringConsumer consumer) {
        if (type == EventType.ENTRY) {
            consumer.accept(content);
        }
    }

    /** Process the content as a normalized string.
     * @param consumer consumer of the normalized string
     * @see #processAsFreeTextString(StringConsumer)
     */
    public void processAsNormalizedString(final StringConsumer consumer) {
        if (type == EventType.ENTRY) {
            consumer.accept(getNormalizedContent());
        }
    }

    /** Process the content as an indexed normalized string.
     * @param consumer consumer of the indexed normalized string
     * @param index index
     */
    public void processAsIndexedNormalizedString(final IndexedStringConsumer consumer, final int index) {
        if (type == EventType.ENTRY) {
            consumer.accept(index, getNormalizedContent());
        }
    }

    /** Process the content as a list of normalized strings.
     * @param consumer consumer of the normalized strings list
     */
    public void processAsNormalizedStringList(final StringListConsumer consumer) {
        if (type == EventType.ENTRY) {
            consumer.accept(Arrays.asList(getNormalizedContent().split(COMMA_SEPARATORS)));
        }
    }

    /** Process the content as an integer.
     * @param consumer consumer of the integer
     */
    public void processAsInteger(final IntConsumer consumer) {
        if (type == EventType.ENTRY) {
            consumer.accept(getContentAsInt());
        }
    }

    /** Process the content as a double.
     * @param consumer consumer of the double
     */
    public void processAsDouble(final DoubleConsumer consumer) {
        if (type == EventType.ENTRY) {
            consumer.accept(getContentAsDouble());
        }
    }

    /** Process the content as an indexed double.
     * @param consumer consumer of the indexed double
     * @param index index
     */
    public void processAsIndexedDouble(final IndexedDoubleConsumer consumer, final int index) {
        if (type == EventType.ENTRY) {
            consumer.accept(index, getContentAsDouble());
        }
    }

    /** Process the content as a date.
     * @param consumer consumer of the date
     * @param context parsing context
     */
    public void processAsDate(final DateConsumer consumer, final ParsingContext context) {
        if (type == EventType.ENTRY) {
            if (context.getTimeScale() == null) {
                throw new OrekitException(OrekitMessages.CCSDS_TIME_SYSTEM_NOT_READ_YET,
                                          getLineNumber(), getFileName());
            }
            consumer.accept(context.getTimeScale().parseDate(content,
                                                             context.getConventions(),
                                                             context.getMissionReferenceDate()));
        }
    }

    /** Process the content as a time scale.
     * @param consumer consumer of the time scale
     */
    public void processAsTimeScale(final TimeScaleConsumer consumer) {
        if (type == EventType.ENTRY) {
            consumer.accept(CcsdsTimeScale.parse(content));
        }
    }

    /** Process the content as a frame.
     * @param consumer consumer of the frame
     * @param context parsing context
     */
    public void processAsFrame(final FrameConsumer consumer, final ParsingContext context) {
        if (type == EventType.ENTRY) {
            final CCSDSFrame frame = CCSDSFrame.valueOf(DASH.matcher(content).replaceAll(""));
            consumer.accept(frame.getFrame(context.getConventions(),
                                           context.isSimpleEOP(),
                                           context.getDataContext()));
        }
    }

    /** Process the content as a body center.
     * @param consumer consumer of the body center
     * @param complainIfUnknown if true, unknown centers generate an exception, otherwise
     * they are silently ignored
     */
    public void processAsCenter(final CenterConsumer consumer, final boolean complainIfUnknown) {
        if (type == EventType.ENTRY) {
            String canonicalValue = getNormalizedContent();
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
                    throw generateException();
                }
            }
        }
    }

    /** Generate a parse exception for this entry.
     * @return exception for this entry
     */
    public OrekitException generateException() {
        return new OrekitException(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE,
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

    /** Interface representing instance methods that consume double values. */
    public interface DoubleConsumer {
        /** Consume a double.
         * @param value value to consume
         */
        void accept(double value);
    }

    /** Interface representing instance methods that consume indexed double values. */
    public interface IndexedDoubleConsumer {
        /** Consume an indexed string.
         * @param index index
         * @param value value to consume
         */
        void accept(int index, double value);
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
         * @param value value to consume
         */
        void accept(Frame value);
    }

    /** Interface representing instance methods that consume center values. */
    public interface CenterConsumer {
        /** Consume a body center.
         * @param value value to consume
         */
        void accept(CenterName value);
    }

}

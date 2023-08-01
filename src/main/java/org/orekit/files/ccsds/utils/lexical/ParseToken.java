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
package org.orekit.files.ccsds.utils.lexical;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBodies;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.BodyFacade;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.definitions.CenterName;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.OrbitRelativeFrame;
import org.orekit.files.ccsds.definitions.SpacecraftBodyFrame;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.cdm.Maneuvrable;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.units.Unit;

/** Token occurring during CCSDS file parsing.
 * <p>
 * Parse tokens correspond to:
 * <ul>
 *   <li>bloc or entry start</li>
 *   <li>entry content</li>
 *   <li>bloc or entry end</li>
 *   <li>raw lines</li>
 * </ul>
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

    /** Pattern for splitting comma-separated lists with no space in between. */
    private static final Pattern SPLIT_AT_COMMAS_NO_SPACE = Pattern.compile(",");

    /** Pattern for true boolean value. */
    private static final Pattern BOOLEAN_TRUE = Pattern.compile("(?:yes)|(?:true)",
                                                                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Pattern for false boolean value. */
    private static final Pattern BOOLEAN_FALSE = Pattern.compile("(?:no)|(?:false)",
                                                                 Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Type of the token. */
    private TokenType type;

    /** Name of the entry. */
    private final String name;

    /** Entry content. */
    private final String content;

    /** Units of the entry. */
    private final Unit units;

    /** Number of the line from which pair is extracted. */
    private final int lineNumber;

    /** Name of the file. */
    private final String fileName;

    /** Simple constructor.
     * @param type type of the token
     * @param name name of the block or entry
     * @param content entry content
     * @param units units of the entry
     * @param lineNumber number of the line in the CCSDS data message
     * @param fileName name of the file
     */
    public ParseToken(final TokenType type, final String name, final String content, final Unit units,
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

    /** Get the raw content of the entry.
     * @return entry raw content
     */
    public String getRawContent() {
        return content;
    }

    /** Get the content of the entry.
     * <p>
     * Free-text strings are normalized by replacing all occurrences
     * of '_' with space, and collapsing several spaces as one space only.
     * </p>
     * @return entry content
     */
    public String getContentAsNormalizedString() {
        return SPACE.matcher(content.replace('_', ' ')).replaceAll(" ").trim();
    }

    /** Get the content of the entry as a list of free-text strings.
     * @return content of the entry as a list of free-test strings
     * @since 12.0
     */
    public List<String> getContentAsFreeTextList() {
        return Arrays.asList(SPLIT_AT_COMMAS.split(getRawContent()));
    }

    /** Get the content of the entry as a list of normalized strings.
     * <p>
     * Normalization is performed by replacing all occurrences
     * of '_' with space, and collapsing several spaces as one space only.
     * </p>
     * @return content of the entry as a list of free-test strings
     */
    public List<String> getContentAsNormalizedList() {
        return Arrays.asList(SPLIT_AT_COMMAS.split(getContentAsNormalizedString()));
    }

    /** Get the content of the entry as normalized and uppercased.
     * @return entry normalized and uppercased content
     */
    public String getContentAsUppercaseString() {
        return getContentAsNormalizedString().toUpperCase(Locale.US);
    }

    /** Get the content of the entry as a list of normalized and uppercased strings.
     * @return content of the entry as a list of normalized and uppercased strings
     */
    public List<String> getContentAsUppercaseList() {
        return Arrays.asList(SPLIT_AT_COMMAS.split(getContentAsUppercaseString()));
    }

    /** Get the content of the entry as an enum.
     * @param cls enum class
     * @param <T> type of the enum
     * @return entry content
     */
    public <T extends Enum<T>> T getContentAsEnum(final Class<T> cls) {
        return toEnum(cls, getRawContent());
    }

    /** Get the content of the entry as a list of enum.
     * @param cls enum class
     * @param <T> type of the enum
     * @return entry content
     */
    public <T extends Enum<T>> List<T> getContentAsEnumList(final Class<T> cls) {
        final String[] elements = SPLIT_AT_COMMAS.split(getRawContent());
        final List<T> list = new ArrayList<>(elements.length);
        for (int i = 0; i < elements.length; ++i) {
            list.add(toEnum(cls, elements[i]));
        }
        return list;
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

    /** Get the content of the entry as a vector.
     * @return content as a vector
     */
    public Vector3D getContentAsVector() {
        try {
            final String[] fields = SPACE.split(content);
            if (fields.length == 3) {
                return new Vector3D(Double.parseDouble(fields[0]),
                                    Double.parseDouble(fields[1]),
                                    Double.parseDouble(fields[2]));
            }
        } catch (NumberFormatException nfe) {
            // ignored, error handled below, together with wrong number of fields
        }
        throw generateException(null);
    }

    /** Get the content of the entry as a boolean.
     * @return content as a boolean
     */
    public boolean getContentAsBoolean() {
        if (BOOLEAN_TRUE.matcher(content).matches()) {
            return true;
        } else if (BOOLEAN_FALSE.matcher(content).matches()) {
            return false;
        } else {
            throw generateException(null);
        }
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

    /** Get the content of the entry as an uppercase character.
     * @return content as an uppercase character
     */
    public char getContentAsUppercaseCharacter() {
        try {
            return getContentAsUppercaseString().charAt(0);
        } catch (NumberFormatException nfe) {
            throw generateException(nfe);
        }
    }

    /** Get the units.
     * @return units of the entry (may be null)
     */
    public Unit getUnits() {
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
     * @see #processAsUppercaseString(StringConsumer)
     */
    public boolean processAsNormalizedString(final StringConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(getContentAsNormalizedString());
        }
        return true;
    }

    /** Process the content as a normalized uppercase string.
     * @param consumer consumer of the normalized uppercase string
     * @return always returns {@code true}
     * @see #processAsNormalizedString(StringConsumer)
     */
    public boolean processAsUppercaseString(final StringConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(getContentAsUppercaseString());
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

    /** Process the content as an indexed normalized uppercase string.
     * @param index index
     * @param consumer consumer of the indexed normalized uppercase string
     * @return always returns {@code true}
     */
    public boolean processAsIndexedUppercaseString(final int index, final IndexedStringConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(index, getContentAsUppercaseString());
        }
        return true;
    }

    /** Process the content as a list of free-text strings.
     * @param consumer consumer of the free-text strings list
     * @return always returns {@code true}
     * @since 12.0
     */
    public boolean processAsFreeTextList(final StringListConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(getContentAsFreeTextList());
        }
        return true;
    }

    /** Process the content as a list of normalized strings.
     * @param consumer consumer of the normalized strings list
     * @return always returns {@code true}
     */
    public boolean processAsNormalizedList(final StringListConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(getContentAsNormalizedList());
        }
        return true;
    }

    /** Process the content as a list of normalized uppercase strings.
     * @param consumer consumer of the normalized uppercase strings list
     * @return always returns {@code true}
     */
    public boolean processAsUppercaseList(final StringListConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(getContentAsUppercaseList());
        }
        return true;
    }

    /** Process the content as an enum.
     * @param cls enum class
     * @param consumer consumer of the enum
     * @param <T> type of the enum
     * @return always returns {@code true}
     */
    public <T extends Enum<T>> boolean processAsEnum(final Class<T> cls, final EnumConsumer<T> consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(getContentAsEnum(cls));
        }
        return true;
    }

    /** Process the content as a list of enums.
     * @param cls enum class
     * @param consumer consumer of the enums list
     * @param <T> type of the enum
     * @return always returns {@code true}
     */
    public <T extends Enum<T>> boolean processAsEnumsList(final Class<T> cls, final EnumListConsumer<T> consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(getContentAsEnumList(cls));
        }
        return true;
    }

    /** Process the content as a boolean.
     * @param consumer consumer of the boolean
     * @return always returns {@code true}
     */
    public boolean processAsBoolean(final BooleanConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(getContentAsBoolean());
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

    /** Process the content as an indexed integer.
     * @param index index
     * @param consumer consumer of the integer
     * @return always returns {@code true}
     * @since 12.0
     */
    public boolean processAsIndexedInteger(final int index, final IndexedIntConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(index, getContentAsInt());
        }
        return true;
    }

    /** Process the content as an array of integers. No spaces between commas are allowed.
     * @param consumer consumer of the array
     * @return always returns {@code true}
     */
    public boolean processAsIntegerArrayNoSpace(final IntegerArrayConsumer consumer) {
        try {
            if (type == TokenType.ENTRY) {
                // Do not allow spaces
                final String[] fields = SPLIT_AT_COMMAS_NO_SPACE.split(getRawContent());
                final int[] integers = new int[fields.length];
                for (int i = 0; i < fields.length; ++i) {
                    integers[i] = Integer.parseInt(fields[i]);
                }
                consumer.accept(integers);
            }
            return true;
        } catch (NumberFormatException nfe) {
            throw generateException(nfe);
        }
    }

    /** Process the content as an array of integers. Spaces are replaced by commas.
     * @param consumer consumer of the array
     * @return always returns {@code true}
     */
    public boolean processAsIntegerArray(final IntegerArrayConsumer consumer) {
        try {
            if (type == TokenType.ENTRY) {
                final String[] fields = SPACE.split(getRawContent());
                final int[] integers = new int[fields.length];
                for (int i = 0; i < fields.length; ++i) {
                    integers[i] = Integer.parseInt(fields[i]);
                }
                consumer.accept(integers);
            }
            return true;
        } catch (NumberFormatException nfe) {
            throw generateException(nfe);
        }
    }

    /** Process the content as a normalized character.
     * @param consumer consumer of the normalized character
     * @return always returns {@code true}
     */
    public boolean processAsNormalizedCharacter(final CharConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(getContentAsUppercaseCharacter());
        }
        return true;
    }

    /** Process the content as a double.
     * @param standard units of parsed content as specified by CCSDS standard
     * @param behavior behavior to adopt for parsed unit
     * @param consumer consumer of the double
     * @return always returns {@code true}
     */
    public boolean processAsDouble(final Unit standard, final ParsedUnitsBehavior behavior,
                                   final DoubleConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(behavior.select(getUnits(), standard).toSI(getContentAsDouble()));
        }
        return true;
    }

    /** Process the content as a labeled double.
     * @param label label
     * @param standard units of parsed content as specified by CCSDS standard
     * @param behavior behavior to adopt for parsed unit
     * @param consumer consumer of the indexed double
     * @return always returns {@code true}
     */
    public boolean processAsLabeledDouble(final char label,
                                          final Unit standard, final ParsedUnitsBehavior behavior,
                                          final LabeledDoubleConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(label, behavior.select(getUnits(), standard).toSI(getContentAsDouble()));
        }
        return true;
    }

    /** Process the content as an indexed double.
     * @param i index
     * @param standard units of parsed content as specified by CCSDS standard
     * @param behavior behavior to adopt for parsed unit
     * @param consumer consumer of the indexed double
     * @return always returns {@code true}
     */
    public boolean processAsIndexedDouble(final int i,
                                          final Unit standard, final ParsedUnitsBehavior behavior,
                                          final IndexedDoubleConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(i, behavior.select(getUnits(), standard).toSI(getContentAsDouble()));
        }
        return true;
    }

    /** Process the content as a doubly-indexed double.
     * @param i first index
     * @param j second index
     * @param standard units of parsed content as specified by CCSDS standard
     * @param behavior behavior to adopt for parsed unit
     * @param consumer consumer of the doubly-indexed double
     * @return always returns {@code true}
     */
    public boolean processAsDoublyIndexedDouble(final int i, final int j,
                                                final Unit standard, final ParsedUnitsBehavior behavior,
                                                final DoublyIndexedDoubleConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(i, j, behavior.select(getUnits(), standard).toSI(getContentAsDouble()));
        }
        return true;
    }

    /** Process the content as an array of doubles.
     * @param standard units of parsed content as specified by CCSDS standard
     * @param behavior behavior to adopt for parsed unit
     * @param consumer consumer of the array
     * @return always returns {@code true}
     * @since 12.0
     */
    public boolean processAsDoubleArray(final Unit standard, final ParsedUnitsBehavior behavior,
                                        final DoubleArrayConsumer consumer) {
        try {
            if (type == TokenType.ENTRY) {
                final String[] fields = SPACE.split(getRawContent());
                final double[] doubles = new double[fields.length];
                for (int i = 0; i < fields.length; ++i) {
                    doubles[i] = behavior.select(getUnits(), standard).toSI(Double.parseDouble(fields[i]));
                }
                consumer.accept(doubles);
            }
            return true;
        } catch (NumberFormatException nfe) {
            throw generateException(nfe);
        }
    }

    /** Process the content as an indexed double array.
     * @param index index
     * @param standard units of parsed content as specified by CCSDS standard
     * @param behavior behavior to adopt for parsed unit
     * @param consumer consumer of the indexed double array
     * @return always returns {@code true}
     * @since 12.0
     */
    public boolean processAsIndexedDoubleArray(final int index,
                                               final Unit standard, final ParsedUnitsBehavior behavior,
                                               final IndexedDoubleArrayConsumer consumer) {
        if (type == TokenType.ENTRY) {
            final String[] fields = SPACE.split(content);
            final double[] values = new double[fields.length];
            for (int i = 0; i < fields.length; ++i) {
                values[i] = behavior.select(getUnits(), standard).toSI(Double.parseDouble(fields[i]));
            }
            consumer.accept(index, values);
        }
        return true;
    }

    /** Process the content as a vector.
     * @param standard units of parsed content as specified by CCSDS standard
     * @param behavior behavior to adopt for parsed unit
     * @param consumer consumer of the vector
     * @return always returns {@code true} (or throws an exception)
     */
    public boolean processAsVector(final Unit standard, final ParsedUnitsBehavior behavior,
                                   final VectorConsumer consumer) {
        if (type == TokenType.ENTRY) {
            final double scale = behavior.select(getUnits(), standard).getScale();
            consumer.accept(getContentAsVector().scalarMultiply(scale));
        }
        return true;
    }

    /** Process the content as a date.
     * @param consumer consumer of the date
     * @param context context binding
     * @return always returns {@code true} (or throws an exception)
     */
    public boolean processAsDate(final DateConsumer consumer, final ContextBinding context) {
        if (type == TokenType.ENTRY) {
            if (context.getTimeSystem() == null) {
                throw new OrekitException(OrekitMessages.CCSDS_TIME_SYSTEM_NOT_READ_YET,
                                          getLineNumber(), getFileName());
            }
            consumer.accept(context.getTimeSystem().getConverter(context).parse(content));
        }
        return true;
    }

    /** Process the content as a time system.
     * @param consumer consumer of the time system
     * @return always returns {@code true} (or throws an exception)
     */
    public boolean processAsTimeSystem(final TimeSystemConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(TimeSystem.parse(getContentAsUppercaseString()));
        }
        return true;
    }

    /** Process the content as a frame.
     * @param consumer consumer of the frame
     * @param context context binding
     * @param allowCelestial if true, {@link CelestialBodyFrame} are allowed
     * @param allowOrbit if true, {@link OrbitRelativeFrame} are allowed
     * @param allowSpacecraft if true, {@link SpacecraftBodyFrame} are allowed
     * @return always returns {@code true}
     */
    public boolean processAsFrame(final FrameConsumer consumer, final ContextBinding context,
                                  final boolean allowCelestial, final boolean allowOrbit,
                                  final boolean allowSpacecraft) {
        if (type == TokenType.ENTRY) {
            try {
                consumer.accept(FrameFacade.parse(DASH.
                                                  matcher(getContentAsUppercaseString()).
                                                  replaceAll("").
                                                  replace(' ', '_'),
                                                  context.getConventions(),
                                                  context.isSimpleEOP(), context.getDataContext(),
                                                  allowCelestial, allowOrbit, allowSpacecraft));
            } catch (OrekitException oe) {
                throw generateException(oe);
            }
        }
        return true;
    }

    /** Process the content as a body center.
     * @param consumer consumer of the body center
     * @param celestialBodies factory for celestial bodies
     * @return always returns {@code true}
     */
    public boolean processAsCenter(final CenterConsumer consumer, final CelestialBodies celestialBodies) {
        if (type == TokenType.ENTRY) {
            final String centerName = getContentAsUppercaseString();
            consumer.accept(new BodyFacade(centerName, body(centerName, celestialBodies)));
        }
        return true;
    }

    /** Process the content as a body center list.
     * @param consumer consumer of the body center list
     * @param celestialBodies factory for celestial bodies
     * @return always returns {@code true}
     */
    public boolean processAsCenterList(final CenterListConsumer consumer, final CelestialBodies celestialBodies) {
        if (type == TokenType.ENTRY) {
            final List<BodyFacade> facades = new ArrayList<>();
            for (final String centerName : SPLIT_AT_COMMAS.split(getContentAsUppercaseString())) {
                facades.add(new BodyFacade(centerName, body(centerName, celestialBodies)));
            }
            consumer.accept(facades);
        }
        return true;
    }

    /** Process the content as a rotation sequence.
     * @param consumer consumer of the rotation sequence
     * @return always returns {@code true}
     * @since 12.0
     */
    public boolean processAsRotationOrder(final RotationOrderConsumer consumer) {
        if (type == TokenType.ENTRY) {
            try {
                consumer.accept(RotationOrder.valueOf(getContentAsUppercaseString().
                                                      replace('1', 'X').
                                                      replace('2', 'Y').
                                                      replace('3', 'Z')));
            } catch (IllegalArgumentException iae) {
                throw new OrekitException(OrekitMessages.CCSDS_INVALID_ROTATION_SEQUENCE,
                                          getContentAsUppercaseString(), getLineNumber(), getFileName());
            }
        }
        return true;
    }

    /** Process the content as a list of units.
     * @param consumer consumer of the time scale
     * @return always returns {@code true} (or throws an exception)
     */
    public boolean processAsUnitList(final UnitListConsumer consumer) {
        if (type == TokenType.ENTRY) {
            final String bracketed = getContentAsNormalizedString();
            if (bracketed.charAt(0) != '[' || bracketed.charAt(bracketed.length() - 1) != ']') {
                throw generateException(null);
            }
            final String unbracketed = bracketed.substring(1, bracketed.length() - 1).trim();
            try {
                consumer.accept(Stream.of(SPLIT_AT_COMMAS.split(unbracketed)).
                                map(s -> Unit.parse(s)).
                                collect(Collectors.toList()));
            } catch (OrekitException oe) {
                // one unit is unknown
                throw generateException(oe);
            }
        }
        return true;
    }

     /** Process the content as free text string.
     * @param consumer consumer of the string
     * @return always returns {@code true}
     */
    public boolean processAsFreeTextString(final StringConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(getRawContent());
        }
        return true;
    }

    /** Process the content of the Maneuvrable enum.
     * @param consumer consumer of the enum
     * @return always returns {@code true}
     */
    public boolean processAsManeuvrableEnum(final ManeuvrableConsumer consumer) {
        if (type == TokenType.ENTRY) {
            consumer.accept(Maneuvrable.getEnum(getRawContent()));
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

    /** Get the body corresponding to a center name.
     * @param centerName name of the center
     * @param celestialBodies factory for celestial bodies
     * @return celestial body corresponding to name, or null
     */
    private CelestialBody body(final String centerName, final CelestialBodies celestialBodies) {

        // convert some known names
        final String canonicalValue;
        if (centerName.equals("SOLAR SYSTEM BARYCENTER") || centerName.equals("SSB")) {
            canonicalValue = "SOLAR_SYSTEM_BARYCENTER";
        } else if (centerName.equals("EARTH MOON BARYCENTER") || centerName.equals("EARTH-MOON BARYCENTER") ||
                        centerName.equals("EARTH BARYCENTER") || centerName.equals("EMB")) {
            canonicalValue = "EARTH_MOON";
        } else {
            canonicalValue = centerName;
        }

        try {
            return CenterName.valueOf(canonicalValue).getCelestialBody(celestialBodies);
        } catch (IllegalArgumentException iae) {
            // ignored, we just let body set to null
            return null;
        }

    }

    /** Convert a value to an enum.
     * @param cls enum class
     * @param value value to convert to an enum
     * @param <T> type of the enum
     * @return enumerate corresponding to the value
     */
    private <T extends Enum<T>> T toEnum(final Class<T> cls, final String value) {
        // first replace space characters
        final String noSpace = value.replace(' ', '_');
        try {
            // first try without changing case, as some CCSDS enums are mixed case (like RangeUnits for TDM)
            return Enum.valueOf(cls, noSpace);
        } catch (IllegalArgumentException iae1) {
            try {
                // second try, using more standard uppercase
                return Enum.valueOf(cls, noSpace.toUpperCase(Locale.US));
            } catch (IllegalArgumentException iae2) {
                // use the first exception for the message
                throw generateException(iae1);
            }
        }
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

    /** Interface representing instance methods that consume enum values.
     * @param <T> type of the enum
     */
    public interface EnumConsumer<T extends Enum<T>> {
        /** Consume an enum.
         * @param value value to consume
         */
        void accept(T value);
    }

    /** Interface representing instance methods that consume lists of enum values.
     * @param <T> type of the enum
     */
    public interface EnumListConsumer<T extends Enum<T>> {
        /** Consume an enum.
         * @param value value to consume
         */
        void accept(List<T> value);
    }

    /** Interface representing instance methods that consume boolean values. */
    public interface BooleanConsumer {
        /** Consume a boolean.
         * @param value value to consume
         */
        void accept(boolean value);
    }

    /** Interface representing instance methods that consume integer values. */
    public interface IntConsumer {
        /** Consume an integer.
         * @param value value to consume
         */
        void accept(int value);
    }

    /** Interface representing instance methods that consume indexed integer values.
     * @since 12.0
     */
    public interface IndexedIntConsumer {
        /** Consume an integer.
         * @param index index
         * @param value value to consume
         */
        void accept(int index, int value);
    }

    /** Interface representing instance methods that consume integer array. */
    public interface IntegerArrayConsumer {
        /** Consume an array of integers.
         * @param integers array of integers
         */
        void accept(int[] integers);
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

    /** Interface representing instance methods that consume labeled double values. */
    public interface LabeledDoubleConsumer {
        /** Consume an indexed double.
         * @param label label
         * @param value value to consume
         */
        void accept(char label, double value);
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

    /** Interface representing instance methods that consume double array. */
    public interface DoubleArrayConsumer {
        /** Consume an array of doubles.
         * @param doubles array of doubles
         */
        void accept(double[] doubles);
    }

    /** Interface representing instance methods that consume indexed double array values.
     * @since 12.0
     */
    public interface IndexedDoubleArrayConsumer {
        /** Consume an indexed double array.
         * @param index index
         * @param value array value to consume
         */
        void accept(int index, double[] value);
    }

    /** Interface representing instance methods that consume vector values. */
    public interface VectorConsumer {
        /** Consume a vector.
         * @param value value to consume
         */
        void accept(Vector3D value);
    }

    /** Interface representing instance methods that consume date values. */
    public interface DateConsumer {
        /** Consume a date.
         * @param value value to consume
         */
        void accept(AbsoluteDate value);
    }

    /** Interface representing instance methods that consume time systems values. */
    public interface TimeSystemConsumer {
        /** Consume a time system.
         * @param value value to consume
         */
        void accept(TimeSystem value);
    }

    /** Interface representing instance methods that consume frame values. */
    public interface FrameConsumer {
        /** Consume a frame.
         * @param frameFacade facade in front of several frames types
         */
        void accept(FrameFacade frameFacade);
    }

    /** Interface representing instance methods that consume center values. */
    public interface CenterConsumer {
        /** Consume a body center.
         * @param bodyFacade facade for celestial body name and body
         */
        void accept(BodyFacade bodyFacade);
    }

    /** Interface representing instance methods that consume center lists. */
    public interface CenterListConsumer {
        /** Consume a body center.
         * @param bodyFacades facades for celestial bodies name and bodies
         */
        void accept(List<BodyFacade> bodyFacades);
    }

    /** Interface representing instance methods that consume otation order values.
     * @since 12.0
     */
    public interface RotationOrderConsumer {
        /** Consume a data.
         * @param value value to consume
         */
        void accept(RotationOrder value);
    }

    /** Interface representing instance methods that consume units lists values. */
    public interface UnitListConsumer {
        /** Consume a list of units.
         * @param value value to consume
         */
        void accept(List<Unit> value);
    }

    /** Interface representing instance methods that consume Maneuvrable values. */
    public interface ManeuvrableConsumer {
        /** Consume a Maneuvrable.
         * @param value value to consume
         */
        void accept(Maneuvrable value);
    }
}

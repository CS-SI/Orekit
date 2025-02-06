/* Copyright 2024-2025 The Johns Hopkins University Applied Physics Laboratory
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
package org.orekit.files.iirv;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.iirv.terms.CheckSumTerm;
import org.orekit.files.iirv.terms.CoordinateSystemTerm;
import org.orekit.files.iirv.terms.CrossSectionalAreaTerm;
import org.orekit.files.iirv.terms.DataSourceTerm;
import org.orekit.files.iirv.terms.DayOfYearTerm;
import org.orekit.files.iirv.terms.DragCoefficientTerm;
import org.orekit.files.iirv.terms.IIRVTermUtils;
import org.orekit.files.iirv.terms.MassTerm;
import org.orekit.files.iirv.terms.MessageClassTerm;
import org.orekit.files.iirv.terms.MessageEndConstantTerm;
import org.orekit.files.iirv.terms.MessageIDTerm;
import org.orekit.files.iirv.terms.MessageSourceTerm;
import org.orekit.files.iirv.terms.MessageStartConstantTerm;
import org.orekit.files.iirv.terms.MessageTypeTerm;
import org.orekit.files.iirv.terms.OriginIdentificationTerm;
import org.orekit.files.iirv.terms.OriginatorRoutingIndicatorTerm;
import org.orekit.files.iirv.terms.PositionVectorComponentTerm;
import org.orekit.files.iirv.terms.RoutingIndicatorTerm;
import org.orekit.files.iirv.terms.SequenceNumberTerm;
import org.orekit.files.iirv.terms.SolarReflectivityCoefficientTerm;
import org.orekit.files.iirv.terms.SpareConstantTerm;
import org.orekit.files.iirv.terms.SupportIdCodeTerm;
import org.orekit.files.iirv.terms.TransferTypeConstantTerm;
import org.orekit.files.iirv.terms.VectorEpochTerm;
import org.orekit.files.iirv.terms.VectorTypeTerm;
import org.orekit.files.iirv.terms.VehicleIdCodeTerm;
import org.orekit.files.iirv.terms.VelocityVectorComponentTerm;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.UTCScale;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A vector containing ephemeris state, epoch, and metadata information that, when taken as a series,
 * comprises an {@link IIRVMessage object}.
 * <p>
 * See {@link IIRVMessage} for documentation on the structure of a single IIRV vector within an IIRV message body.
 *
 * @author Nick LaFarge
 * @see IIRVMessage
 * @see IIRVEphemerisFile.IIRVEphemeris
 * @since 13.0
 */
public class IIRVVector implements Comparable<IIRVVector> {

    /** Line separator: two ASCII carriage returns and two ASCII line feeds. */
    public static final String LINE_SEPARATOR = "\r\r\n\n";


    /** For creating from a list of Strings, OK to omit the line separators. */
    private static final String LINE_SEPARATOR_PATTERN = "(" + LINE_SEPARATOR + ")?";

    /**
     * Regular expression for validating for line 1 when message metadata is included.
     * <p>
     * Message metadata fields refer to the first four terms defined for an IIRV vector:
     * <ul>
     *     <li>{@link MessageTypeTerm}</li>
     *     <li>{@link MessageIDTerm}</li>
     *     <li>{@link MessageSourceTerm}</li>
     *     <li>{@link MessageClassTerm}</li>
     * </ul>
     */
    public static final Pattern LINE_1_PATTERN_METADATA_INCLUDED = Pattern.compile("^" + MessageTypeTerm.MESSAGE_TYPE_TERM_PATTERN + MessageIDTerm.MESSAGE_ID_TERM_PATTERN +
        MessageSourceTerm.MESSAGE_SOURCE_TERM_PATTERN + MessageClassTerm.MESSAGE_CLASS_TERM_PATTERN + MessageStartConstantTerm.MESSAGE_START_TERM_STRING +
        OriginIdentificationTerm.ORIGIN_IDENTIFICATION_TERM_PATTERN + RoutingIndicatorTerm.ROUTING_INDICATOR_TERM_PATTERN + LINE_SEPARATOR_PATTERN);

    /**
     * Regular expression for validating for line 1 when message metadata is omitted.
     * <p>
     * Message metadata fields refer to the first four terms defined for an IIRV vector:
     * <ul>
     *     <li>{@link MessageTypeTerm}</li>
     *     <li>{@link MessageIDTerm}</li>
     *     <li>{@link MessageSourceTerm}</li>
     *     <li>{@link MessageClassTerm}</li>
     * </ul>
     */
    public static final Pattern LINE_1_PATTERN_METADATA_OMITTED = Pattern.compile("^" + MessageStartConstantTerm.MESSAGE_START_TERM_STRING + OriginIdentificationTerm.ORIGIN_IDENTIFICATION_TERM_PATTERN + RoutingIndicatorTerm.ROUTING_INDICATOR_TERM_PATTERN + LINE_SEPARATOR_PATTERN);

    /** Regular expression for validating for line 2. */
    public static final Pattern LINE_2_PATTERN = Pattern.compile("^" + VectorTypeTerm.VECTOR_TYPE_TERM_PATTERN + DataSourceTerm.DATA_SOURCE_TERM_PATTERN + TransferTypeConstantTerm.TRANSFER_TYPE_TERM_STRING + CoordinateSystemTerm.COORDINATE_SYSTEM_TERM_PATTERN + SupportIdCodeTerm.SUPPORT_ID_TERM_PATTERN + VehicleIdCodeTerm.VEHICLE_ID_TERM_PATTERN + SequenceNumberTerm.SEQUENCE_NUMBER_TERM_PATTERN + DayOfYearTerm.DAY_OF_YEAR_PATTERN + VectorEpochTerm.VECTOR_EPOCH_TERM_PATTERN + CheckSumTerm.CHECK_SUM_TERM_PATTERN + LINE_SEPARATOR_PATTERN);

    /** Regular expression for validating for line 3. */
    public static final Pattern LINE_3_PATTERN = Pattern.compile("^" + PositionVectorComponentTerm.POSITION_VECTOR_COMPONENT_TERM_PATTERN + PositionVectorComponentTerm.POSITION_VECTOR_COMPONENT_TERM_PATTERN + PositionVectorComponentTerm.POSITION_VECTOR_COMPONENT_TERM_PATTERN + CheckSumTerm.CHECK_SUM_TERM_PATTERN + LINE_SEPARATOR_PATTERN);

    /** Regular expression for validating for line 4. */
    public static final Pattern LINE_4_PATTERN = Pattern.compile("^" + VelocityVectorComponentTerm.VELOCITY_VECTOR_COMPONENT_TERM_PATTERN + VelocityVectorComponentTerm.VELOCITY_VECTOR_COMPONENT_TERM_PATTERN + VelocityVectorComponentTerm.VELOCITY_VECTOR_COMPONENT_TERM_PATTERN + CheckSumTerm.CHECK_SUM_TERM_PATTERN + LINE_SEPARATOR_PATTERN);

    /** Regular expression for validating for line 5. */
    public static final Pattern LINE_5_PATTERN = Pattern.compile("^" + MassTerm.MASS_TERM_PATTERN + CrossSectionalAreaTerm.CROSS_SECTIONAL_AREA_TERM_PATTERN + DragCoefficientTerm.DRAG_COEFFICIENT_TERM_PATTERN + SolarReflectivityCoefficientTerm.SOLAR_REFLECTIVITY_COEFFICIENT_TERM_PATTERN + CheckSumTerm.CHECK_SUM_TERM_PATTERN + LINE_SEPARATOR_PATTERN);

    /** Regular expression for validating for line 6. */
    public static final Pattern LINE_6_PATTERN = Pattern.compile("^" + MessageEndConstantTerm.MESSAGE_END_TERM_STRING + SpareConstantTerm.SPARE_TERM_STRING + OriginatorRoutingIndicatorTerm.ORIGINATOR_ROUTING_INDICATOR_TERM_PATTERN + LINE_SEPARATOR_PATTERN);

    /** Line 1: Message Type. */
    private final MessageTypeTerm messageType;

    /** Line 1: Message Identification. */
    private final MessageIDTerm messageID;

    /** Line 1: Message Source. */
    private final MessageSourceTerm messageSource;

    /** Line 1: Message class. */
    private final MessageClassTerm messageClass;

    /** Line 1: Message Start. */
    private final MessageStartConstantTerm messageStart;

    /** Line 1: Origin identification. */
    private final OriginIdentificationTerm originIdentification;

    /** Line 1: Destination routing indicator. */
    private final RoutingIndicatorTerm routingIndicator;

    /** Line 2: Vector type. */
    private final VectorTypeTerm vectorType;

    /** Line 2: Source of data. */
    private final DataSourceTerm dataSource;

    /** Line 2: Transfer Type. */
    private final TransferTypeConstantTerm transferType;

    /** Line 2: Transfer Type. */
    private final CoordinateSystemTerm coordinateSystem;

    /** Line 2: Support ID Code. */
    private final SupportIdCodeTerm supportIdCode;

    /** Line 2: Vehicle ID Code. */
    private final VehicleIdCodeTerm vehicleIdCode;

    /** Line 2: Sequence number. */
    private final SequenceNumberTerm sequenceNumber;

    /** Line 2: Day of year (1-366). */
    private final DayOfYearTerm dayOfYear;

    /** Line 2: Transfer Type. */
    private final VectorEpochTerm vectorEpoch;

    /** Line 2: Check Sum. */
    private final CheckSumTerm line2CheckSum;

    /** Line 3: x component of the position vector [m]. */
    private final PositionVectorComponentTerm xPosition;

    /** Line 3: y component of the position vector [m]. */
    private final PositionVectorComponentTerm yPosition;

    /** Line 3: z component of the position vector [m]. */
    private final PositionVectorComponentTerm zPosition;

    /** Line 3: Check Sum. */
    private final CheckSumTerm line3CheckSum;

    /** Line 3: x component of the velocity vector [m/s]. */
    private final VelocityVectorComponentTerm xVelocity;

    /** Line 3: y component of the position vector [m/s]. */
    private final VelocityVectorComponentTerm yVelocity;

    /** Line 3: z component of the position vector [m/s]. */
    private final VelocityVectorComponentTerm zVelocity;

    /** Line 4: Check Sum. */
    private final CheckSumTerm line4CheckSum;

    /** Line 5: Satellite mass [kg]. */
    private final MassTerm mass;

    /** Line 5: Average satellite cross-sectional area [m^2]. */
    private final CrossSectionalAreaTerm crossSectionalArea;

    /** Line 5: Drag coefficient [dimensionless]. */
    private final DragCoefficientTerm dragCoefficient;

    /** Line 5: Solar reflectivity coefficient [dimensionless]. */
    private final SolarReflectivityCoefficientTerm solarReflectivityCoefficient;

    /** Line 5: Check Sum. */
    private final CheckSumTerm line5CheckSum;

    /** Line 6: End of message (always ITERM). */
    private final MessageEndConstantTerm endOfMessage;

    /** Line 6: Spare term (always ASCII space). */
    private final SpareConstantTerm spareTerm;

    /** Line 6: originator of message (GCQU or GAQD). */
    private final OriginatorRoutingIndicatorTerm originatorRoutingIndicatorTerm;

    /** UTC time scale. */
    private final UTCScale utc;

    /**
     * Constructs an IIRV message from its composite terms.
     *
     * @param messageType                    Message type
     * @param messageID                      Message identification
     * @param messageSource                  Message source
     * @param messageClass                   Message class
     * @param originIdentification           Origin identification
     * @param routingIndicator               Destination routing indicator
     * @param vectorType                     Vector type
     * @param dataSource                     Data source
     * @param coordinateSystem               Coordinate system
     * @param supportIdCode                  Support identification code
     * @param vehicleIdCode                  Vehicle identification code
     * @param sequenceNumber                 Sequence number
     * @param dayOfYear                      Day of year
     * @param vectorEpoch                    Vector epoch
     * @param xPosition                      X component of the position vector [m]
     * @param yPosition                      Y component of the position vector [m]
     * @param zPosition                      Z component of the position vector [m]
     * @param xVelocity                      X component of the velocity vector [m/s]
     * @param yVelocity                      Y component of the velocity vector [m/s]
     * @param zVelocity                      Z component of the velocity vector [m/s]
     * @param mass                           Satellite mass (kg)
     * @param crossSectionalArea             Average satellite cross-sectional area [m^2]
     * @param dragCoefficient                Drag coefficient [dimensionless]
     * @param solarReflectivityCoefficient   Solar reflectivity coefficient [dimensionless]
     * @param originatorRoutingIndicatorTerm Originator of message (GCQU or GAQD)
     * @param utc                            UTC time scale
     */
    public IIRVVector(final MessageTypeTerm messageType, final MessageIDTerm messageID, final MessageSourceTerm messageSource, final MessageClassTerm messageClass, final OriginIdentificationTerm originIdentification, final RoutingIndicatorTerm routingIndicator, final VectorTypeTerm vectorType, final DataSourceTerm dataSource, final CoordinateSystemTerm coordinateSystem, final SupportIdCodeTerm supportIdCode, final VehicleIdCodeTerm vehicleIdCode, final SequenceNumberTerm sequenceNumber, final DayOfYearTerm dayOfYear, final VectorEpochTerm vectorEpoch, final PositionVectorComponentTerm xPosition, final PositionVectorComponentTerm yPosition, final PositionVectorComponentTerm zPosition, final VelocityVectorComponentTerm xVelocity, final VelocityVectorComponentTerm yVelocity, final VelocityVectorComponentTerm zVelocity, final MassTerm mass, final CrossSectionalAreaTerm crossSectionalArea, final DragCoefficientTerm dragCoefficient, final SolarReflectivityCoefficientTerm solarReflectivityCoefficient, final OriginatorRoutingIndicatorTerm originatorRoutingIndicatorTerm, final UTCScale utc) {

        // Line 1
        this.messageType = messageType;
        this.messageID = messageID;
        this.messageSource = messageSource;
        this.messageClass = messageClass;
        this.messageStart = new MessageStartConstantTerm();
        this.originIdentification = originIdentification;
        this.routingIndicator = routingIndicator;

        // Line 2
        this.vectorType = vectorType;
        this.dataSource = dataSource;
        this.transferType = new TransferTypeConstantTerm();
        this.coordinateSystem = coordinateSystem;
        this.supportIdCode = supportIdCode;
        this.vehicleIdCode = vehicleIdCode;
        this.sequenceNumber = sequenceNumber;
        this.dayOfYear = dayOfYear;
        this.vectorEpoch = vectorEpoch;
        this.line2CheckSum = CheckSumTerm.fromIIRVTerms(this.vectorType, this.dataSource, this.transferType, this.coordinateSystem, this.supportIdCode, this.vehicleIdCode, this.sequenceNumber, this.dayOfYear, this.vectorEpoch);

        // Line 3
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.zPosition = zPosition;
        this.line3CheckSum = CheckSumTerm.fromIIRVTerms(this.xPosition, this.yPosition, this.zPosition);

        // Line 4
        this.xVelocity = xVelocity;
        this.yVelocity = yVelocity;
        this.zVelocity = zVelocity;
        this.line4CheckSum = CheckSumTerm.fromIIRVTerms(this.xVelocity, this.yVelocity, this.zVelocity);

        // Line 5
        this.mass = mass;
        this.crossSectionalArea = crossSectionalArea;
        this.dragCoefficient = dragCoefficient;
        this.solarReflectivityCoefficient = solarReflectivityCoefficient;
        this.line5CheckSum = CheckSumTerm.fromIIRVTerms(this.mass, this.crossSectionalArea, this.dragCoefficient, this.solarReflectivityCoefficient);

        // Line 6
        this.endOfMessage = new MessageEndConstantTerm();
        this.spareTerm = new SpareConstantTerm();
        this.originatorRoutingIndicatorTerm = originatorRoutingIndicatorTerm;


        // UTC time scale
        this.utc = utc;
    }

    /**
     * Constructs an IIRV message object given a list of 6 lines of message body (omitting blank line feeds).
     *
     * @param lines Six-element list of lines in an IIRV message
     * @param utc   UTC time scale
     */
    public IIRVVector(final List<String> lines, final UTCScale utc) {
        this(lines.get(0), lines.get(1), lines.get(2), lines.get(3), lines.get(4), lines.get(5), utc);
    }

    /**
     * Constructs an IIRV message object given 6 lines of message body (omitting blank line feeds).
     *
     * @param line1 Line 1 of IIRV message body
     * @param line2 Line 2 of IIRV message body
     * @param line3 Line 3 of IIRV message body
     * @param line4 Line 4 of IIRV message body
     * @param line5 Line 5 of IIRV message body
     * @param line6 Line 6 of IIRV message body
     * @param utc   UTC time scale
     */
    public IIRVVector(final String line1, final String line2, final String line3, final String line4, final String line5, final String line6, final UTCScale utc) {
        this.utc = utc;
        final ArrayList<String> iirvLines = new ArrayList<>(Arrays.asList(line1, line2, line3, line4, line5, line6));

        if (!IIRVVector.isFormatOK(iirvLines)) {
            // trigger line-specific error
            validateLines(iirvLines, true);
            validateLines(iirvLines, false);

            // this should never happen; error should have been triggered by one of the calls above
            throw new OrekitInternalError(null);
        }

        // Parse Line 1
        messageType = new MessageTypeTerm(line1.substring(0, 2));
        messageStart = new MessageStartConstantTerm();
        if (LINE_1_PATTERN_METADATA_INCLUDED.matcher(line1).matches()) {
            messageID = new MessageIDTerm(line1.substring(2, 9));
            messageSource = new MessageSourceTerm(line1.substring(9, 10));
            messageClass = new MessageClassTerm(line1.substring(10, 12));
            originIdentification = new OriginIdentificationTerm(line1.substring(17, 18));
            routingIndicator = new RoutingIndicatorTerm(line1.substring(18, 22));
        } else {
            messageID = new MessageIDTerm(0);     // Default to 0
            messageSource = MessageSourceTerm.DEFAULT;  // Default to "0"
            messageClass = MessageClassTerm.NOMINAL;    // Default to "10" (nominal)
            originIdentification = new OriginIdentificationTerm(line1.substring(5, 6));
            routingIndicator = new RoutingIndicatorTerm(line1.substring(6, 10));
        }

        // Parse Line 2
        vectorType = new VectorTypeTerm(line2.substring(0, 1));
        dataSource = new DataSourceTerm(line2.substring(1, 2));
        transferType = new TransferTypeConstantTerm();
        coordinateSystem = new CoordinateSystemTerm(line2.substring(3, 4));
        supportIdCode = new SupportIdCodeTerm(line2.substring(4, 8));
        vehicleIdCode = new VehicleIdCodeTerm(line2.substring(8, 10));
        sequenceNumber = new SequenceNumberTerm(line2.substring(10, 13));
        dayOfYear = new DayOfYearTerm(line2.substring(13, 16));
        vectorEpoch = new VectorEpochTerm(line2.substring(16, 25));
        line2CheckSum = new CheckSumTerm(line2.substring(25, 28));

        // Parse Line 3 (position coordinates in meters)
        xPosition = new PositionVectorComponentTerm(line3.substring(0, 13));
        yPosition = new PositionVectorComponentTerm(line3.substring(13, 26));
        zPosition = new PositionVectorComponentTerm(line3.substring(26, 39));
        line3CheckSum = new CheckSumTerm(line3.substring(39));

        // Parse Line 4 (velocity coordinates in m/s)
        xVelocity = new VelocityVectorComponentTerm(line4.substring(0, 13));
        yVelocity = new VelocityVectorComponentTerm(line4.substring(13, 26));
        zVelocity = new VelocityVectorComponentTerm(line4.substring(26, 39));
        line4CheckSum = new CheckSumTerm(line4.substring(39));

        // Parse Line 5
        mass = new MassTerm(line5.substring(0, 8));
        crossSectionalArea = new CrossSectionalAreaTerm(line5.substring(8, 13));
        dragCoefficient = new DragCoefficientTerm(line5.substring(13, 17));
        solarReflectivityCoefficient = new SolarReflectivityCoefficientTerm(line5.substring(17, 25));
        line5CheckSum = new CheckSumTerm(line5.substring(25));

        // Parse Line 6
        endOfMessage = new MessageEndConstantTerm();
        spareTerm = new SpareConstantTerm();
        originatorRoutingIndicatorTerm = new OriginatorRoutingIndicatorTerm(line6.substring(6, 10));
    }

    /**
     * Copy constructor.
     *
     * @param other Other IIRVVector to create from.
     */
    public IIRVVector(final IIRVVector other) {
        this(other.toIIRVStrings(true), other.utc);
    }

    /**
     * Checks the format validity for each line using regular expressions.
     *
     * @param lines the six-element list of lines
     * @return true if format is valid, false otherwise
     */
    public static boolean isFormatOK(final List<String> lines) {
        if (lines == null) {
            return false;
        }
        if (lines.size() != 6) {
            return false;
        }
        return isFormatOK(lines.get(0), lines.get(1), lines.get(2), lines.get(3), lines.get(4), lines.get(5));
    }

    /**
     * Checks the format validity for each line using regular expressions.
     *
     * @param line1 Line 1 of IIRV message body
     * @param line2 Line 2 of IIRV message body
     * @param line3 Line 3 of IIRV message body
     * @param line4 Line 4 of IIRV message body
     * @param line5 Line 5 of IIRV message body
     * @param line6 Line 6 of IIRV message body
     * @return true if format is valid, false otherwise
     */
    public static boolean isFormatOK(final String line1, final String line2, final String line3, final String line4, final String line5, final String line6) {
        // Attempt to validate the input lines: with metadata
        try {
            validateLines(line1, line2, line3, line4, line5, line6, true);
            return true;
        } catch (OrekitIllegalArgumentException oe) {
            // Continue to other check
        }

        // Attempt to validate the input lines: without metadata
        try {
            validateLines(line1, line2, line3, line4, line5, line6, false);
            return true;
        } catch (OrekitIllegalArgumentException oe) {
            return false;  // both validation checks failed
        }
    }

    /**
     * Check the format validity for each line using regular expressions.
     *
     * @param lines                            the six-element list of lines
     * @param firstLineIncludesMessageMetadata true if message metadata terms are included in the first line
     *                                         of the vector
     */
    public static void validateLines(final List<String> lines, final boolean firstLineIncludesMessageMetadata) {
        if (lines == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NULL_ARGUMENT, "lines");
        }
        if (lines.size() != 6) {
            throw new OrekitIllegalArgumentException(OrekitMessages.INCONSISTENT_NUMBER_OF_ELEMENTS, 6, lines.size());
        }

        validateLines(lines.get(0), lines.get(1), lines.get(2), lines.get(3), lines.get(4), lines.get(5), firstLineIncludesMessageMetadata);
    }


    /**
     * Check the format validity for each line using regular expressions.
     *
     * @param line1                            Line 1 of IIRV message body
     * @param line2                            Line 2 of IIRV message body
     * @param line3                            Line 3 of IIRV message body
     * @param line4                            Line 4 of IIRV message body
     * @param line5                            Line 5 of IIRV message body
     * @param line6                            Line 6 of IIRV message body
     * @param firstLineIncludesMessageMetadata true if message metadata terms are included in the first line
     *                                         of the vector
     */
    public static void validateLines(final String line1, final String line2, final String line3, final String line4, final String line5, final String line6, final boolean firstLineIncludesMessageMetadata) {
        // Validate line1 separately based on if metadata is included
        if (line1 == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NULL_ARGUMENT, "line1");
        }
        final Pattern line1Pattern = firstLineIncludesMessageMetadata ? LINE_1_PATTERN_METADATA_INCLUDED : LINE_1_PATTERN_METADATA_OMITTED;
        final boolean lineOneOkay = line1Pattern.matcher(line1).matches();

        // Validate remaining lines
        final boolean lineTwoOkay = validateLine(1, line2);
        final boolean lineThreeOkay = validateLine(2, line3);
        final boolean lineFourOkay = validateLine(3, line4);
        final boolean lineFiveOkay = validateLine(4, line5);
        final boolean lineSixOkay = validateLine(5, line6);

        if (!lineTwoOkay) {
            throw new OrekitIllegalArgumentException(OrekitMessages.IIRV_INVALID_LINE_IN_VECTOR, 2, line2);
        } else if (!lineThreeOkay) {
            throw new OrekitIllegalArgumentException(OrekitMessages.IIRV_INVALID_LINE_IN_VECTOR, 3, line3);
        } else if (!lineFourOkay) {
            throw new OrekitIllegalArgumentException(OrekitMessages.IIRV_INVALID_LINE_IN_VECTOR, 4, line4);
        } else if (!lineFiveOkay) {
            throw new OrekitIllegalArgumentException(OrekitMessages.IIRV_INVALID_LINE_IN_VECTOR, 5, line5);
        } else if (!lineSixOkay) {
            throw new OrekitIllegalArgumentException(OrekitMessages.IIRV_INVALID_LINE_IN_VECTOR, 6, line6);
        } else if (!lineOneOkay) {
            // Do line 1 last since this is the one that can change format
            throw new OrekitIllegalArgumentException(OrekitMessages.IIRV_INVALID_LINE_IN_VECTOR, 1, line1);
        }
    }

    /**
     * Check an input string against the specified line's regular expression, and validate the checksum for lines 2-5.
     *
     * @param lineIndex Line index to validate against (0-5)
     * @param line      String to validate
     * @return true if the inputted line string is valid, false otherwise.
     */
    public static boolean validateLine(final int lineIndex, final String line) {
        if (line == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NULL_ARGUMENT, "line");
        } else if (line.length() < 3) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NOT_ENOUGH_DATA, line.length());
        }

        // Validate against regular expressions
        final boolean regexMatches;
        switch (lineIndex) {
            case 0:
                final boolean line1Metadata = LINE_1_PATTERN_METADATA_INCLUDED.matcher(line).matches();
                final boolean line1NoMetadata = LINE_1_PATTERN_METADATA_OMITTED.matcher(line).matches();
                regexMatches = line1Metadata || line1NoMetadata;
                break;
            case 1:
                regexMatches = LINE_2_PATTERN.matcher(line).matches();
                break;
            case 2:
                regexMatches = LINE_3_PATTERN.matcher(line).matches();
                break;
            case 3:
                regexMatches = LINE_4_PATTERN.matcher(line).matches();
                break;
            case 4:
                regexMatches = LINE_5_PATTERN.matcher(line).matches();
                break;
            case 5:
                regexMatches = LINE_6_PATTERN.matcher(line).matches();
                break;
            default:
                throw new OrekitIllegalArgumentException(OrekitMessages.INVALID_PARAMETER_RANGE, "lineIndex", lineIndex, 0, 5);
        }

        // Validate checksum. Value is always "true" for lines that omit checksum (1, 6))
        final boolean checksumValid;
        if (lineIndex == 0 || lineIndex == 5) {
            checksumValid = true;
        } else {
            try {
                checksumValid = CheckSumTerm.validateLineCheckSum(line);
            } catch (OrekitIllegalArgumentException e) {
                return false;
            }
        }

        return regexMatches && checksumValid;
    }

    /**
     * Compares two IIRV vectors for equality based on their string representations, include message
     * metadata information.
     */
    @Override
    public int compareTo(final IIRVVector o) {
        final String thisIIRV = toIIRVString(true);
        final String otherIIRV = o.toIIRVString(true);
        return thisIIRV.compareTo(otherIIRV);
    }

    /**
     * Compares two IIRV vectors for equality based on their string representations, include message
     * metadata information.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final IIRVVector vector = (IIRVVector) o;
        return vector.toIIRVString(true).equals(toIIRVString(true));
    }

    @Override
    public String toString() {
        return toIIRVString(true);
    }

    @Override
    public int hashCode() {
        return Objects.hash(toString());
    }

    /**
     * Combine each line to build a String containing the entire IIRV message (including carriage returns and line
     * feeds).
     *
     * @param includeMessageMetadata If true, include message metadata terms
     *                               ({@link MessageTypeTerm},
     *                               {@link MessageIDTerm},
     *                               {@link MessageSourceTerm},
     *                               {@link MessageClassTerm})
     *                               at the beginning of the first line.
     * @return Full IIRV vector in String format
     */
    public String toIIRVString(final boolean includeMessageMetadata) {
        final List<String> lines = toIIRVStrings(includeMessageMetadata);
        final StringBuilder iirvVectorString = new StringBuilder();
        for (String x : lines) {
            iirvVectorString.append(x);
            iirvVectorString.append(LINE_SEPARATOR);
        }

        return iirvVectorString.toString();
    }

    /**
     * Computes each IIRV lines as Strings, validate it, and return it in a list.
     *
     * @param includeMessageMetadata If true, include message metadata terms
     *                               ({@link MessageTypeTerm},
     *                               {@link MessageIDTerm},
     *                               {@link MessageSourceTerm},
     *                               {@link MessageClassTerm})
     *                               at the beginning of the first line.
     * @return List of IIRV lines as Strings
     */
    public List<String> toIIRVStrings(final boolean includeMessageMetadata) {
        final String line1 = buildLine1(includeMessageMetadata);
        final String line2 = buildLine2();
        final String line3 = buildLine3();
        final String line4 = buildLine4();
        final String line5 = buildLine5();
        final String line6 = buildLine6();

        // Validate each line and return
        validateLines(line1, line2, line3, line4, line5, line6, includeMessageMetadata);
        return new ArrayList<>(Arrays.asList(line1, line2, line3, line4, line5, line6));
    }

    /**
     * Computes each IIRV lines as Strings, with each IIRV vector term separated by a forward slash '/'.
     * <p>
     * This method is intended to display more human-readable IIRV vectors, and should never be used
     * for writing an actual IIRV message (see {@link #toIIRVStrings(boolean)}).
     *
     * @return List of six human-readable IIRV lines, with each IIRV vector term separated by a forward slash '/'
     */
    public List<String> toHumanReadableLines() {
        final String deliminator = "/";
        return new ArrayList<>(Arrays.asList(buildLine1SplitByTerm(deliminator, true), buildLine2SplitByTerm(deliminator), buildLine3SplitByTerm(deliminator), buildLine4SplitByTerm(deliminator), buildLine5SplitByTerm(deliminator), buildLine6SplitByTerm(deliminator)));
    }

    /**
     * Builds the first line of the IIRV vector, not including ASCII carriage returns and line feeds.
     * <br>
     * Only includes message type, id, source, and class if this is the first IIRV in the sequence, i.e.,
     * sequenceNumber=0
     *
     * @param includeMessageMetadata If true, include message metadata terms
     *                               ({@link MessageTypeTerm},
     *                               {@link MessageIDTerm},
     *                               {@link MessageSourceTerm},
     *                               {@link MessageClassTerm}) at the beginning of the line.
     * @return Line 1 of IIRV vector
     */
    public String buildLine1(final boolean includeMessageMetadata) {
        return buildLine1SplitByTerm("", includeMessageMetadata);
    }

    /**
     * Builds the second line of the IIRV vector, not including ASCII carriage returns and line feeds.
     *
     * @return Line 2 of IIRV vector
     */
    public String buildLine2() {
        return buildLine2SplitByTerm("");
    }

    /**
     * Builds the third line of the IIRV vector, not including ASCII carriage returns and line feeds.
     *
     * @return Line 3 of IIRV vector
     */
    public String buildLine3() {
        return buildLine3SplitByTerm("");
    }

    /**
     * Builds the fourth line of the IIRV vector, not including ASCII carriage returns and line feeds.
     *
     * @return Line 4 of IIRV vector
     */
    public String buildLine4() {
        return buildLine4SplitByTerm("");
    }

    /**
     * Builds the fifth line of the IIRV vector, not including ASCII carriage returns and line feeds.
     *
     * @return Line 5 of IIRV vector
     */
    public String buildLine5() {
        return buildLine5SplitByTerm("");
    }

    /**
     * Builds the sixth line of the IIRV vector, not including ASCII carriage returns and line feeds.
     *
     * @return Line 6 of IIRV vector
     */
    public String buildLine6() {
        return buildLine6SplitByTerm("");
    }

    /**
     * Creates an {@link AbsoluteDate} instance (in UTC), with time components given by the {@link VectorEpochTerm},
     * and date components from the {@link DayOfYearTerm} / inputted year.
     *
     * @param year Year associated with the created
     * @return created {@link AbsoluteDate} instance
     */
    public AbsoluteDate getAbsoluteDate(final int year) {
        return new AbsoluteDate(dayOfYear.getDateComponents(year), vectorEpoch.value(), utc);
    }

    /**
     * Gets a position vector [x y z], represented as a {@link Vector3D} instance.
     * <p>
     * Data supplied by the {@link PositionVectorComponentTerm} values; See {@link #getXPosition()},
     * {@link #getYPosition()}, {@link #getZPosition()}.
     *
     * @return Vector containing x,y,z position components
     */
    public Vector3D getPositionVector() {
        return new Vector3D(xPosition.value(), yPosition.value(), zPosition.value());
    }

    /**
     * Gets a velocity vector [vx vy vz], represented as a {@link Vector3D} instance.
     * <p>
     * Data supplied by the {@link VelocityVectorComponentTerm} values; See {@link #getXVelocity()},
     * {@link #getYVelocity()}, {@link #getZVelocity()}.
     *
     * @return Vector containing x,y,z velocity components
     */
    public Vector3D getVelocityVector() {
        return new Vector3D(xVelocity.value(), yVelocity.value(), zVelocity.value());
    }

    /**
     * Gets the state vector and time data as a {@link TimeStampedPVCoordinates} instance.
     * <p>
     * Position and velocity data supplied by the x,y,z position components ({@link PositionVectorComponentTerm}) and
     * vx, vy, vz velocity components ({@link VelocityVectorComponentTerm}). Epoch data given by {@link DayOfYearTerm}
     * {@link VectorEpochTerm}.
     *
     * @param year The year of the corresponding coordinates (IIRV vector does not contain year information)
     * @return Newly created {@link TimeStampedPVCoordinates} instance populated with data from the IIRV vector terms.
     */
    public TimeStampedPVCoordinates getTimeStampedPVCoordinates(final int year) {
        return new TimeStampedPVCoordinates(getAbsoluteDate(year), getPositionVector(), getVelocityVector());
    }

    /**
     * Gets the state vector as a {@link org.orekit.utils.PVCoordinates} instance.
     * <p>
     * Position and velocity data supplied by the x,y,z position components ({@link PositionVectorComponentTerm}) and
     * vx, vy, vz velocity components ({@link VelocityVectorComponentTerm}).
     *
     * @return Newly created {@link PVCoordinates} instance populated with data from the IIRV vector terms.
     */
    public PVCoordinates getPVCoordinates() {
        return new PVCoordinates(getPositionVector(), getVelocityVector());
    }

    /**
     * Returns the {@link Frame} associated with the IIRV vector based on its {@link CoordinateSystemTerm}.
     *
     * @param context data context used to retrieve frames
     * @return coordinate system
     * @see CoordinateSystemTerm#getFrame
     */
    public Frame getFrame(final DataContext context) {
        return coordinateSystem.getFrame(context);
    }

    /**
     * Returns the {@link Frame} associated with the IIRV vector based on its {@link CoordinateSystemTerm}.
     *
     * @return coordinate system
     * @see CoordinateSystemTerm#getFrame
     */
    @DefaultDataContext
    public Frame getFrame() {
        return coordinateSystem.getFrame();
    }

    /**
     * Gets the message type term.
     *
     * @return the message type term
     */
    public MessageTypeTerm getMessageType() {
        return messageType;
    }

    /**
     * Gets the message ID term.
     *
     * @return the message ID term
     */
    public MessageIDTerm getMessageID() {
        return messageID;
    }

    /**
     * Gets the message source term.
     *
     * @return the message source term
     */
    public MessageSourceTerm getMessageSource() {
        return messageSource;
    }

    /**
     * Gets the message class term.
     *
     * @return the message class term
     */
    public MessageClassTerm getMessageClass() {
        return messageClass;
    }

    /**
     * Gets the message start term.
     *
     * @return the message start term
     */
    public MessageStartConstantTerm getMessageStart() {
        return messageStart;
    }

    /**
     * Gets the origin identification term.
     *
     * @return the origin identification term
     */
    public OriginIdentificationTerm getOriginIdentification() {
        return originIdentification;
    }

    /**
     * Gets the routing indicator term.
     *
     * @return the routing indicator term
     */
    public RoutingIndicatorTerm getRoutingIndicator() {
        return routingIndicator;
    }

    /**
     * Gets the vector type term.
     *
     * @return the vector type term
     */
    public VectorTypeTerm getVectorType() {
        return vectorType;
    }

    /**
     * Gets the data source term.
     *
     * @return the data source term
     */
    public DataSourceTerm getDataSource() {
        return dataSource;
    }

    /**
     * Gets the transfer type term.
     *
     * @return the transfer type term
     */
    public TransferTypeConstantTerm getTransferType() {
        return transferType;
    }

    /**
     * Gets the coordinate system term.
     *
     * @return the coordinate system term
     */
    public CoordinateSystemTerm getCoordinateSystem() {
        return coordinateSystem;
    }

    /**
     * Gets the support ID code term.
     *
     * @return the support ID code term
     */
    public SupportIdCodeTerm getSupportIdCode() {
        return supportIdCode;
    }

    /**
     * Gets the vehicle ID code term.
     *
     * @return the vehicle ID code term
     */
    public VehicleIdCodeTerm getVehicleIdCode() {
        return vehicleIdCode;
    }

    /**
     * Gets the sequence number term.
     *
     * @return the sequence number term
     */
    public SequenceNumberTerm getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Gets the day of year term.
     *
     * @return the day of year term
     */
    public DayOfYearTerm getDayOfYear() {
        return dayOfYear;
    }

    /**
     * Gets the vector epoch term.
     *
     * @return the vector epoch term
     */
    public VectorEpochTerm getVectorEpoch() {
        return vectorEpoch;
    }

    /**
     * Gets the checksum value for line 2.
     *
     * @return the checksum value for line 2
     */
    public CheckSumTerm getLine2CheckSum() {
        return line2CheckSum;
    }

    /**
     * Gets the x component of position.
     *
     * @return the x component of position
     */
    public PositionVectorComponentTerm getXPosition() {
        return xPosition;
    }

    /**
     * Gets the y component of position.
     *
     * @return the y component of position
     */
    public PositionVectorComponentTerm getYPosition() {
        return yPosition;
    }

    /**
     * Gets the z component of position.
     *
     * @return the z component of position
     */
    public PositionVectorComponentTerm getZPosition() {
        return zPosition;
    }

    /**
     * Gets the checksum term for line 3.
     *
     * @return the checksum term for line 3
     */
    public CheckSumTerm getLine3CheckSum() {
        return line3CheckSum;
    }

    /**
     * Gets the x component of velocity.
     *
     * @return the x component of velocity
     */
    public VelocityVectorComponentTerm getXVelocity() {
        return xVelocity;
    }

    /**
     * Gets the y component of velocity.
     *
     * @return the y component of velocity
     */
    public VelocityVectorComponentTerm getYVelocity() {
        return yVelocity;
    }

    /**
     * Gets the z component of velocity.
     *
     * @return the z component of velocity
     */
    public VelocityVectorComponentTerm getZVelocity() {
        return zVelocity;
    }

    /**
     * Gets the checksum value for line 4 term.
     *
     * @return the checksum value for line 4 term
     */
    public CheckSumTerm getLine4CheckSum() {
        return line4CheckSum;
    }

    /**
     * Gets the mass term.
     *
     * @return the mass term
     */
    public MassTerm getMass() {
        return mass;
    }

    /**
     * Gets the cross-sectional area term.
     *
     * @return the cross-sectional area term
     */
    public CrossSectionalAreaTerm getCrossSectionalArea() {
        return crossSectionalArea;
    }

    /**
     * Gets the drag coefficient term.
     *
     * @return the drag coefficient term
     */
    public DragCoefficientTerm getDragCoefficient() {
        return dragCoefficient;
    }

    /**
     * Gets the solar reflectivity coefficient term.
     *
     * @return the solar reflectivity coefficient term
     */
    public SolarReflectivityCoefficientTerm getSolarReflectivityCoefficient() {
        return solarReflectivityCoefficient;
    }

    /**
     * Gets the checksum term for line 5.
     *
     * @return the checksum term for line 5
     */
    public CheckSumTerm getLine5CheckSum() {
        return line5CheckSum;
    }

    /**
     * Gets the "ITERM" message end term.
     *
     * @return the "ITERM" message end term
     */
    public MessageEndConstantTerm getMessageEnd() {
        return endOfMessage;
    }

    /**
     * Gets the spare character term (ASCII space).
     *
     * @return the spare character term (ASCII space)
     */
    public SpareConstantTerm getSpareTerm() {
        return spareTerm;
    }

    /**
     * Gets the originator routing indicator term.
     *
     * @return the originator routing indicator term
     */
    public OriginatorRoutingIndicatorTerm getOriginatorRoutingIndicator() {
        return originatorRoutingIndicatorTerm;
    }

    /**
     * Builds the first line of the IIRV, not including ASCII carriage returns and line feeds.
     * <br>
     * <p>
     * Only includes message type, id, source, and class if this is the first IIRV in the sequence, i.e.,
     * sequenceNumber=0.
     *
     * @param delimiter              Delimiter to split the terms in the message. Should always be "", except when printing
     *                               for human readability.
     * @param includeMessageMetadata If true, include message metadata terms
     *                               ({@link MessageTypeTerm},
     *                               {@link MessageIDTerm},
     *                               {@link MessageSourceTerm},
     *                               {@link MessageClassTerm}) at the beginning of the line.
     * @return Line 1 of IIRV message
     */
    private String buildLine1SplitByTerm(final String delimiter, final boolean includeMessageMetadata) {
        final String splitLine;
        if (includeMessageMetadata) {
            splitLine = IIRVTermUtils.iirvTermsToLineStringSplitByTerm(delimiter, messageType, messageID, messageSource, messageClass, messageStart, originIdentification, routingIndicator);

        } else {
            splitLine = IIRVTermUtils.iirvTermsToLineStringSplitByTerm(delimiter, messageStart, originIdentification, routingIndicator);
        }
        return splitLine;
    }

    /**
     * Builds the second line of the IIRV message, not including ASCII carriage returns and line feeds.
     *
     * @param delimiter Delimiter to split the terms in the message. Should always be "", except when printing
     *                  for human readability.
     * @return Line 2 of IIRV message
     */
    private String buildLine2SplitByTerm(final String delimiter) {
        return IIRVTermUtils.iirvTermsToLineStringSplitByTerm(delimiter, vectorType, dataSource, transferType, coordinateSystem, supportIdCode, vehicleIdCode, sequenceNumber, dayOfYear, vectorEpoch, line2CheckSum);
    }

    /**
     * Builds the third line of the IIRV message, not including ASCII carriage returns and line feeds.
     *
     * @param delimiter Delimiter to split the terms in the message. Should always be "", except when printing
     *                  for human readability.
     * @return Line 3 of IIRV message
     */
    private String buildLine3SplitByTerm(final String delimiter) {
        return IIRVTermUtils.iirvTermsToLineStringSplitByTerm(delimiter, xPosition, yPosition, zPosition, line3CheckSum);
    }

    /**
     * Builds the fourth line of the IIRV message, not including ASCII carriage returns and line feeds.
     *
     * @param delimiter Delimiter to split the terms in the message. Should always be "", except when printing
     *                  for human readability.
     * @return Line f of IIRV message
     */
    private String buildLine4SplitByTerm(final String delimiter) {
        return IIRVTermUtils.iirvTermsToLineStringSplitByTerm(delimiter, xVelocity, yVelocity, zVelocity, line4CheckSum);
    }

    /**
     * Builds the fifth line of the IIRV message, not including ASCII carriage returns and line feeds.
     *
     * @param delimiter Delimiter to split the terms in the message. Should always be "", except when printing
     *                  for human readability.
     * @return Line 5 of IIRV message
     */
    private String buildLine5SplitByTerm(final String delimiter) {
        return IIRVTermUtils.iirvTermsToLineStringSplitByTerm(delimiter, mass, crossSectionalArea, dragCoefficient, solarReflectivityCoefficient, line5CheckSum);
    }

    /**
     * Builds the sixth line of the IIRV message, not including ASCII carriage returns and line feeds.
     *
     * @param delimiter Delimiter to split the terms in the message. Should always be "", except when printing
     *                  for human readability.
     * @return Line 6 of IIRV message
     */
    private String buildLine6SplitByTerm(final String delimiter) {
        return IIRVTermUtils.iirvTermsToLineStringSplitByTerm(delimiter, endOfMessage, spareTerm, originatorRoutingIndicatorTerm);
    }

}

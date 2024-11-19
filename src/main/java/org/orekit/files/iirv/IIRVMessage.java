/* Copyright 2024 The Johns Hopkins University Applied Physics Laboratory
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

import org.orekit.errors.OrekitIllegalStateException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.iirv.terms.CheckSumTerm;
import org.orekit.files.iirv.terms.CoordinateSystemTerm;
import org.orekit.files.iirv.terms.CrossSectionalAreaTerm;
import org.orekit.files.iirv.terms.DataSourceTerm;
import org.orekit.files.iirv.terms.DayOfYearTerm;
import org.orekit.files.iirv.terms.DragCoefficientTerm;
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
import org.orekit.files.iirv.terms.VectorEpochTerm;
import org.orekit.files.iirv.terms.VectorTypeTerm;
import org.orekit.files.iirv.terms.VehicleIdCodeTerm;
import org.orekit.files.iirv.terms.VelocityVectorComponentTerm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Container for Improved Interrange Vector (IIRV) messages, implemented as a list of sequential {@link IIRVVector}
 * instances.
 * <p>
 * The IIRV message consists of a series of sequential {@link IIRVVector}s that each contains ephemeris state data
 * at a particular epoch. The message body is defined as:<p>
 * {@code ttuuuuuuuqjjGIIRVarrrr<<≡≡}<br>
 * {@code vs1ciiiibbnnndoyhhmmsssssccc<<≡≡}<br>
 * {@code sxxxxxxxxxxxxsyyyyyyyyyyyyszzzzzzzzzzzzccc<<==}<br>
 * {@code sxxxxxxxxxxxxsyyyyyyyyyyyyszzzzzzzzzzzzccc<<==}<br>
 * {@code mmmmmmmmaaaaakkkksrrrrrrrccc<<==}<br>
 * {@code ITERM oooo<<==}
 * <table border="1">
 *     <thead>
 *         <tr>
 *             <th>Line No.</th>
 *             <th>Characters</th>
 *             <th>Value</th>
 *             <th>Type</th>
 *             <th>Description</th>
 *             <th>Class</th>
 *    </thead>
 *    <tbody>
 *        <tr>
 *            <th rowspan="9">1</th>
 *            <td>{@code tt}</td>
 *            <td>2 characters</td>
 *            <td>Constant</td>
 *            <td>Message Type (Operations Data Message)</td>
 *            <td>{@link MessageTypeTerm}</td>
 *        <tr>
 *            <td>{@code uuuuuuu}</td>
 *            <td>0000000 to 9999999</td>
 *            <td>Integer</td>
 *            <td>Message ID</td>
 *            <td>{@link MessageIDTerm}</td>
 *        <tr>
 *            <td>{@code q}</td>
 *            <td>1 character</td>
 *            <td>String</td>
 *            <td>Message Source</td>
 *            <td>{@link MessageSourceTerm}</td>
 *        <tr>
 *            <td>{@code jj}</td>
 *            <td>
 *                <ul>
 *                <li> 10 = Nominal
 *                <li> 15 = In-flight Update
 *                </ul></td>
 *            <td>String</td>
 *            <td>Message class (10=nominal)</td>
 *            <td>{@link MessageClassTerm}</td>
 *        <tr>
 *            <td>{@code GIIRV}</td>
 *            <td>"GIIRV"</td>
 *            <td>Constant</td>
 *            <td>Message ID</td>
 *            <td>{@link MessageStartConstantTerm}</td>
 *        <tr>
 *            <td>{@code a}</td>
 *            <td>
 *                <ul>
 *                <li> ASCII space  = GSFC
 *                <li> Z            = WLP
 *                <li> E            = ETR
 *                <li> L            = JPL
 *                <li> W            = WTR
 *                <li> J            = JSC
 *                <li> P            = PMR
 *                <li> A            = CSTC
 *                <li> K            = KMR
 *                <li> C            = CNES
 *                </ul></td>
 *            <td>String</td>
 *            <td>Origin identification</td>
 *            <td>{@link OriginIdentificationTerm}</td>
 *        <tr>
 *            <td>{@code rrrr}</td>
 *            <td>
 *                <ul>
 *                <li>GSFC    = NASA Goddard Space Flight Center
 *                <li>WLP     = Wallops Island tracking radars
 *                <li>ETR     = NASA/USFC Eastern Test Range
 *                <li>JPL     = NASA Jet Propulsion Laboratory
 *                <li>WTR     = NASA/USFC Western Test Range
 *                <li>JSC     = NASA Johnson Space Center
 *                <li>PMR     = Navy Pacific Missile Range
 *                <li>CSTC    = Air Force Satellite Control Facility
 *                <li>KMR     = Army Kwajalein Missile Range
 *                <li>CNES    = French Space Agency National <br>Centre for Space Studies (CNES)
 *                <li>MANY    = Message originated from more <br>than one of the above stations
 *                </ul></td>
 *            <td>String</td>
 *            <td>Destination routing indicator</td>
 *            <td>{@link RoutingIndicatorTerm}</td>
 *        <tr>
 *            <td>{@code <<}</td>
 *            <td>"\r\r"</td>
 *            <td>Constant</td>
 *            <td>Carriage returns</td>
 *            <td>n/a</td>
 *        <tr>
 *            <td>==</td>
 *            <td>"\n\n"</td>
 *            <td>Constant</td>
 *            <td>Line feeds</td>
 *            <td>n/a</td>
 *        <tr>
 *            <th rowspan="12">2</th>
 *            <td>{@code v}</td>
 *            <td>
 *                <ul>
 *                <li> 1 = Free flight (routine on-orbit)
 *                <li> 2 = Forced (special orbit update)
 *                <li> 3 = Spare
 *                <li> 4 = Maneuver ignition
 *                <li> 5 = Maneuver cutoff
 *                <li> 6 = Reentry
 *                <li> 7 = Powered flight
 *                <li> 8 = Stationary
 *                <li> 9 = Spare
 *                </ul></td>
 *            <td>Integer</td>
 *            <td>Vector type</td>
 *            <td>{@link VectorTypeTerm}</td>
 *        <tr>
 *            <td>{@code s}</td>
 *            <td>
 *                <ul>
 *                <li> 1 = Nominal/planning
 *                <li> 2 = Real-time
 *                <li> 3 = Off-line
 *                <li> 4 = Off-line/mean
 *                </ul></td>
 *            <td>Integer</td>
 *            <td>Source of data</td>
 *            <td>{@link DataSourceTerm}</td>
 *        <tr>
 *            <td>{@code 1}</td>
 *            <td>"1" = interrange message</td>
 *            <td>Constant</td>
 *            <td>Transfer type</td>
 *            <td>{@link org.orekit.files.iirv.terms.TransferTypeConstantTerm}</td>
 *        <tr>
 *            <td>{@code c}</td>
 *            <td>
 *                <ul>
 *                <li> 1 = Geocentric True-of-Date Rotating
 *                <li> 2 = Geocentric mean of 1950.0 (B1950.0)
 *                <li> 3 = Heliocentric B1950.0
 *                <li> 4 = Reserved for JPL use (non-GSFC)
 *                <li> 5 = Reserved for JPL use (non-GSFC)
 *                <li> 6 = Geocentric mean of 2000.0 (J2000.0)
 *                <li> 7 = Heliocentric J2000.0
 *                </ul></td>
 *            <td>Integer</td>
 *            <td>Coordinate system</td>
 *            <td>{@link CoordinateSystemTerm}</td>
 *        <tr>
 *            <td>{@code iiii}</td>
 *            <td>0000-9999</td>
 *            <td>Integer</td>
 *            <td>Support Identification Code (SIC)</td>
 *            <td>{@link SupportIdCodeTerm}</td>
 *        <tr>
 *            <td>{@code bb}</td>
 *            <td>00-99</td>
 *            <td>Integer</td>
 *            <td>Vehicle Identification Code (VIC)</td>
 *            <td>{@link VehicleIdCodeTerm}</td>
 *        <tr>
 *            <td>{@code nnn}</td>
 *            <td>000-999</td>
 *            <td>Integer</td>
 *            <td>Sequence number</td>
 *            <td>{@link SequenceNumberTerm}</td>
 *        <tr>
 *            <td>{@code doy}</td>
 *            <td>001-366</td>
 *            <td>Integer</td>
 *            <td>Day of year</td>
 *            <td>{@link DayOfYearTerm}</td>
 *        <tr>
 *            <td>{@code hhmmsssss}</td>
 *            <td>000000000 - 235959999</td>
 *            <td>Integer</td>
 *            <td>Vector epoch (in UTC)<br>HH:mm:ss.SSS</td>
 *            <td>{@link VectorEpochTerm}</td>
 *        <tr>
 *            <td>{@code ccc}</td>
 *            <td>000-999</td>
 *            <td>Integer</td>
 *            <td>Checksum for line 2</td>
 *            <td>{@link CheckSumTerm}</td>
 *        <tr>
 *            <td>{@code <<}</td>
 *            <td>"\r\r"</td>
 *            <td>Constant</td>
 *            <td>Carriage returns</td>
 *            <td>n/a</td>
 *        <tr>
 *            <td>==</td>
 *            <td>"\n\n"</td>
 *            <td>Constant</td>
 *            <td>Line feeds</td>
 *            <td>n/a</td>
 *        <tr>
 *            <th rowspan="7">3</th>
 *            <td>{@code s}</td>
 *            <td>
 *                <ul>
 *                <li> " " (ASCII Space) = positive
 *                <li> "-" = Negative
 *                </ul></td>
 *            <td>Integer</td>
 *            <td>Positive/negative sign</td>
 *            <td>n/a</td>
 *        <tr>
 *            <td>{@code xxxxxxxxxxxx}</td>
 *            <td>0 - 9999999999999</td>
 *            <td>Integer</td>
 *            <td>X component of position (m)</td>
 *            <td>{@link PositionVectorComponentTerm}</td>
 *        <tr>
 *            <td>{@code yyyyyyyyyyyy}</td>
 *            <td>0 - 9999999999999</td>
 *            <td>Integer</td>
 *            <td>Y component of position (m)</td>
 *            <td>{@link PositionVectorComponentTerm}</td>
 *        <tr>
 *            <td>{@code zzzzzzzzzzzz}</td>
 *            <td>0 - 9999999999999</td>
 *            <td>Integer</td>
 *            <td>Z component of position (m)</td>
 *            <td>{@link PositionVectorComponentTerm}</td>
 *        <tr>
 *            <td>{@code ccc}</td>
 *            <td>000-999</td>
 *            <td>Integer</td>
 *            <td>Checksum for line 3</td>
 *            <td>{@link CheckSumTerm}</td>
 *        <tr>
 *            <td>{@code <<}</td>
 *            <td>"\r\r"</td>
 *            <td>Constant</td>
 *            <td>Carriage returns</td>
 *            <td>n/a</td>
 *        <tr>
 *            <td>==</td>
 *            <td>"\n\n"</td>
 *            <td>Constant</td>
 *            <td>Line feeds</td>
 *            <td>n/a</td>
 *        <tr>
 *            <th rowspan="7">4</th>
 *            <td>{@code s}</td>
 *            <td>
 *                <ul>
 *                <li> " " (ASCII Space) = positive
 *                <li> "-" = Negative
 *                </ul></td>
 *            <td>Integer</td>
 *            <td>Positive/negative sign</td>
 *            <td>n/a</td>
 *        <tr>
 *            <td>{@code xxxxxxxxxxxx}</td>
 *            <td>0 - 9999999999.999</td>
 *            <td>Double</td>
 *            <td>X component of velocity (m/s)</td>
 *            <td>{@link VelocityVectorComponentTerm}</td>
 *        <tr>
 *            <td>{@code yyyyyyyyyyyy}</td>
 *            <td>0 - 9999999999.999</td>
 *            <td>Double</td>
 *            <td>Y component of velocity (m/s)</td>
 *            <td>{@link VelocityVectorComponentTerm}</td>
 *        <tr>
 *            <td>{@code zzzzzzzzzzzz}</td>
 *            <td>0 - 9999999999.999</td>
 *            <td>Double</td>
 *            <td>Z component of velocity (m/s)</td>
 *            <td>{@link VelocityVectorComponentTerm}</td>
 *        <tr>
 *            <td>{@code ccc}</td>
 *            <td>000-999</td>
 *            <td>Integer</td>
 *            <td>Checksum for line 4</td>
 *            <td>{@link CheckSumTerm}</td>
 *        <tr>
 *            <td>{@code <<}</td>
 *            <td>"\r\r"</td>
 *            <td>Constant</td>
 *            <td>Carriage returns</td>
 *            <td>n/a</td>
 *        <tr>
 *            <td>==</td>
 *            <td>"\n\n"</td>
 *            <td>Constant</td>
 *            <td>Line feeds</td>
 *            <td>n/a</td>
 *        <tr>
 *            <th rowspan="7">5</th>
 *            <td>{@code mmmmmmmm}</td>
 *            <td>0 - 99999999.9</td>
 *            <td>Double</td>
 *            <td>Spacecraft mass (kg)</td>
 *            <td>{@link MassTerm}</td>
 *        <tr>
 *            <td>{@code aaaaa}</td>
 *            <td>0 - 999.99</td>
 *            <td>Double</td>
 *            <td>Average cross-sectional area (m^2)</td>
 *            <td>{@link CrossSectionalAreaTerm}</td>
 *        <tr>
 *            <td>{@code kkkk}</td>
 *            <td>0 - 99.99</td>
 *            <td>Double</td>
 *            <td>Drag coefficient (dimensionless)</td>
 *            <td>{@link DragCoefficientTerm}</td>
 *        <tr>
 *            <td>{@code srrrrrrr}</td>
 *            <td>-99.99999 to 99.99999</td>
 *            <td>Double</td>
 *            <td>Solar reflectivity coefficient (dimensionless)</td>
 *            <td>{@link SolarReflectivityCoefficientTerm}</td>
 *        <tr>
 *            <td>{@code ccc}</td>
 *            <td>000-999</td>
 *            <td>Integer</td>
 *            <td>Checksum for line 5</td>
 *            <td>{@link CheckSumTerm}</td>
 *        <tr>
 *            <td>{@code <<}</td>
 *            <td>"\r\r"</td>
 *            <td>Constant</td>
 *            <td>Carriage returns</td>
 *            <td>n/a</td>
 *        <tr>
 *            <td>==</td>
 *            <td>"\n\n"</td>
 *            <td>Constant</td>
 *            <td>Line feeds</td>
 *            <td>n/a</td>
 *        <tr>
 *            <th rowspan="5">6</th>
 *            <td>{@code ITERM}</td>
 *            <td>"ITERM"</td>
 *            <td>Constant</td>
 *            <td>End of message</td>
 *            <td>{@link MessageEndConstantTerm}</td>
 *        <tr>
 *            <td>{@code ITERM}</td>
 *            <td>ASCII Space</td>
 *            <td>Constant</td>
 *            <td>Spare (blank) character</td>
 *            <td>{@link SpareConstantTerm}</td>
 *        <tr>
 *            <td>{@code oooo}</td>
 *            <td>"GCQU" or "GAQD"</td>
 *            <td>String</td>
 *            <td>Originator routing indicator</td>
 *            <td>{@link OriginatorRoutingIndicatorTerm}</td>
 *        <tr>
 *            <td>{@code <<}</td>
 *            <td>"\r\r"</td>
 *            <td>Constant</td>
 *            <td>Carriage returns</td>
 *            <td>n/a</td>
 *        <tr>
 *            <td>==</td>
 *            <td>"\n\n"</td>
 *            <td>Constant</td>
 *            <td>Line feeds</td>
 *            <td>n/a</td>
 *        <tr>
 *            <th>7-12</th>
 *            <td colspan="5">Second {@link IIRVVector} in message</td>
 *        <tr>
 *            <th>13-18</th>
 *            <td colspan="5">Third {@link IIRVVector} in message</td>
 *        <tr>
 *            <th>...</th>
 *            <td colspan="5">nth {@link IIRVVector} in message</td>
 *    </tbody>
 * </table>
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class IIRVMessage {

    /** List of vectors that comprise the IIRV message. */
    private final List<IIRVVector> vectors;

    /** Constructor that initializes to an empty list of vectors. */
    public IIRVMessage() {
        this.vectors = new ArrayList<>();
    }

    /**
     * Constructor from a list of IIRV {@link IIRVVector}s that monotonically increase in both
     * {@link org.orekit.files.iirv.terms.SequenceNumberTerm} and time ({@link org.orekit.files.iirv.terms.DayOfYearTerm}
     * and {@link org.orekit.files.iirv.terms.VectorEpochTerm}).
     *
     * @param vectors list of sequential {@link IIRVVector}s.
     */
    public IIRVMessage(final List<IIRVVector> vectors) {
        // Perform validation checks
        validateSequenceNumberIncreasing(vectors);
        validateStaticValues(vectors);

        this.vectors = vectors;
    }

    /**
     * Constructor from a list of IIRV {@link IIRVVector}s that monotonically increase in both
     * {@link org.orekit.files.iirv.terms.SequenceNumberTerm} and time ({@link org.orekit.files.iirv.terms.DayOfYearTerm}
     * and {@link org.orekit.files.iirv.terms.VectorEpochTerm}).
     *
     * @param vectors list of sequential IIRV vectors.
     */
    public IIRVMessage(final IIRVVector... vectors) {
        this(Arrays.asList(vectors));
    }

    /**
     * Copy constructor.
     *
     * @param other other {@link IIRVMessage} instance.
     */
    public IIRVMessage(final IIRVMessage other) {
        final List<IIRVVector> copied_vectors = new ArrayList<>();
        for (IIRVVector v : other.getVectors()) {
            copied_vectors.add(new IIRVVector(v));
        }
        validateSequenceNumberIncreasing(copied_vectors);
        validateStaticValues(copied_vectors);
        this.vectors = copied_vectors;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final IIRVMessage that = (IIRVMessage) o;
        return Objects.equals(toMessageString(IncludeMessageMetadata.ALL_VECTORS), that.toMessageString(IncludeMessageMetadata.ALL_VECTORS));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(vectors);
    }

    /**
     * Adds an {@link IIRVVector} to the message (see {@link ArrayList#add(Object)}).
     *
     * @param v IIRV vector to add to the message
     */
    public void add(final IIRVVector v) {
        if (vectors.isEmpty()) {
            vectors.add(v);
            return;
        }

        // Check that the time and sequence number are increasing
        final int prevLineNumber = vectors.size() - 1;
        final IIRVVector prev = vectors.get(prevLineNumber);

        // Verify sequence number is increasing by one
        if (prev.getSequenceNumber().value() + 1 != v.getSequenceNumber().value()) {
            throw new OrekitIllegalStateException(OrekitMessages.IIRV_SEQUENCE_NUMBER_MUST_BE_INCREASING_BY_ONE, prevLineNumber, prev.getSequenceNumber().value(), prevLineNumber, v.getSequenceNumber().value());
        }

        // Ensure the static values are consistent across all the vectors
        validateStaticValues(vectors);

        this.vectors.add(v);
    }

    /**
     * Gets the {@link IIRVVector} located at a given index in the message.
     *
     * @param i index of the element to return
     * @return element at the given index
     * @see ArrayList#get(int)
     */
    public IIRVVector get(final int i) {
        return vectors.get(i);
    }

    /**
     * Returns the number of IIRV vectors contained in the message.
     *
     * @return number of IIRV vectors contained in the message
     * @see ArrayList#size()
     */
    public int size() {
        return vectors.size();
    }

    /**
     * Returns true if no vectors exist in the message.
     *
     * @return true if no vectors exist in the message
     * @see ArrayList#isEmpty()
     */
    public boolean isEmpty() {
        return vectors.isEmpty();
    }

    /**
     * Converts the {@link IIRVVector}s contained in the message file into a list of their String representations.
     *
     * @param includeMessageMetadataSetting Setting for when message metadata terms appear in the created IIRV message
     * @return list of {@link IIRVVector} strings for each vector the message
     * @see IIRVVector#toIIRVString
     */
    public ArrayList<String> getVectorStrings(final IncludeMessageMetadata includeMessageMetadataSetting) {
        final ArrayList<String> messageStrings = new ArrayList<>();
        for (int i = 0; i < vectors.size(); i++) {
            final boolean includeMessageMetadata;
            switch (includeMessageMetadataSetting) {
                case ALL_VECTORS: {
                    includeMessageMetadata = true;
                    break;
                }
                case FIRST_VECTOR_ONLY: {
                    includeMessageMetadata = i == 0;
                    break;
                }
                default:
                    throw new OrekitInternalError(null);
            }
            messageStrings.add(vectors.get(i).toIIRVString(includeMessageMetadata));
        }

        return messageStrings;
    }

    /**
     * Converts the {@link IIRVVector}s contained in the message file into a single String, where no deliminator
     * included between each vector (the vectors already have trailing line carriage and line returns).
     *
     * @param includeMessageMetadataSetting Setting for when message metadata terms appear in the created IIRV message
     * @return String containing all {@link IIRVVector}s for the IIRV message
     * @see IIRVVector#toIIRVString
     */
    public String toMessageString(final IncludeMessageMetadata includeMessageMetadataSetting) {
        return String.join("", getVectorStrings(includeMessageMetadataSetting));
    }

    /**
     * Gets the list of sequential {@link IIRVVector} instances contained within the overall IIRV message.
     *
     * @return list of sequential {@link IIRVVector} instances contained within the overall IIRV message.
     */
    public List<IIRVVector> getVectors() {
        return vectors;
    }

    /**
     * Validates that values that are expected to remain constant do not change across a series of inputted
     * IIRV vectors.
     *
     * @param iirvVectors List of {@link IIRVVector} instances to validate.
     */
    private void validateStaticValues(final List<IIRVVector> iirvVectors) {

        // Check thatM select values are consistent across entire vector
        final IIRVVector firstIIRV = iirvVectors.get(0);
        for (int i = 1; i < iirvVectors.size(); i++) {
            final IIRVVector iirv = iirvVectors.get(i);

            // Check that terms you expect to remain constant, do remain constant
            if (!firstIIRV.getMessageID().equals(iirv.getMessageID())) {
                throw new OrekitIllegalStateException(OrekitMessages.IIRV_TERM_CHANGES_WITHIN_FILE, "Message ID");
            } else if (!firstIIRV.getMessageClass().equals(iirv.getMessageClass())) {
                throw new OrekitIllegalStateException(OrekitMessages.IIRV_TERM_CHANGES_WITHIN_FILE, "Message class");
            } else if (!firstIIRV.getOriginIdentification().equals(iirv.getOriginIdentification())) {
                throw new OrekitIllegalStateException(OrekitMessages.IIRV_TERM_CHANGES_WITHIN_FILE, "Origin ID");
            } else if (!firstIIRV.getRoutingIndicator().equals(iirv.getRoutingIndicator())) {
                throw new OrekitIllegalStateException(OrekitMessages.IIRV_TERM_CHANGES_WITHIN_FILE, "Routing indicator");
            } else if (!firstIIRV.getVectorType().equals(iirv.getVectorType())) {
                throw new OrekitIllegalStateException(OrekitMessages.IIRV_TERM_CHANGES_WITHIN_FILE, "Vector type");
            } else if (!firstIIRV.getDataSource().equals(iirv.getDataSource())) {
                throw new OrekitIllegalStateException(OrekitMessages.IIRV_TERM_CHANGES_WITHIN_FILE, "Data source");
            } else if (!firstIIRV.getCoordinateSystem().equals(iirv.getCoordinateSystem())) {
                throw new OrekitIllegalStateException(OrekitMessages.IIRV_TERM_CHANGES_WITHIN_FILE, "Coordinate system");
            } else if (!firstIIRV.getSupportIdCode().equals(iirv.getSupportIdCode())) {
                throw new OrekitIllegalStateException(OrekitMessages.IIRV_TERM_CHANGES_WITHIN_FILE, "Support ID code (SIC)");
            } else if (!firstIIRV.getVehicleIdCode().equals(iirv.getVehicleIdCode())) {
                throw new OrekitIllegalStateException(OrekitMessages.IIRV_TERM_CHANGES_WITHIN_FILE, "Vehicle ID code (VID)");
            }
        }
    }

    /**
     * Returns the satellite ID (set to the value of the {@link org.orekit.files.iirv.terms.VehicleIdCodeTerm}).
     *
     * @return the satellite ID
     * @see org.orekit.files.iirv.terms.VehicleIdCodeTerm
     */
    public String getSatelliteID() {
        return vectors.get(0).getVehicleIdCode().toEncodedString();
    }

    /**
     * Validates that the sequence number increases by one for each element in a series of {@link IIRVVector}s.
     *
     * @param iirvVectors List of {@link IIRVVector} instances to validate.
     */
    private void validateSequenceNumberIncreasing(final List<IIRVVector> iirvVectors) {
        if (iirvVectors.size() < 2) {
            return;
        }

        // Check that sequence number increases by 1 each time
        for (int i = 1; i < iirvVectors.size(); i++) {
            final IIRVVector current = iirvVectors.get(i);
            final IIRVVector prev = iirvVectors.get(i - 1);
            if (current.getSequenceNumber().value() - prev.getSequenceNumber().value() != 1) {
                throw new OrekitIllegalStateException(OrekitMessages.IIRV_SEQUENCE_NUMBER_MUST_BE_INCREASING_BY_ONE, i - 1, prev.getSequenceNumber().value(), current.getSequenceNumber().value());
            }
        }
    }

    /**
     * Options for how message metadata appears in the IIRV message file.
     * <p>
     * Message metadata fields refer to the first four terms defined for an IIRV vector:
     * <ul>
     *     <li>{@link MessageTypeTerm}</li>
     *     <li>{@link org.orekit.files.iirv.terms.MessageIDTerm}</li>
     *     <li>{@link MessageSourceTerm}</li>
     *     <li>{@link org.orekit.files.iirv.terms.MessageClassTerm}</li>
     * </ul>
     *
     * @author Nick LaFarge
     * @since 13.0
     */
    public enum IncludeMessageMetadata {
        /**
         * Include message metadata fields in the first line of the first vector
         * (when {@link org.orekit.files.iirv.terms.SequenceNumberTerm} is 0), and omit for all other vectors
         * in a given IIRV message file.
         */
        FIRST_VECTOR_ONLY,

        /** Include message metadata fields from all vectors in a given IIRV message file. */
        ALL_VECTORS
    }

}

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
package org.orekit.files.ccsds.ndm.odm.ocm;

import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.parsing.ParsingContext;
import org.orekit.utils.Constants;


/** Keys for {@link OcmMetadata OCM metadata} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum OcmMetadataKey {

    /** Classification of this message. */
    CLASSIFICATION((token, context, metadata) -> token.processAsNormalizedString(metadata::setClassification)),

    /** International designator for the object as assigned by the UN Committee
     * on Space Research (COSPAR) and the US National Space Science Data Center (NSSDC). */
    INTERNATIONAL_DESIGNATOR((token, context, metadata) -> token.processAsNormalizedString(metadata::setInternationalDesignator)),

    /** Specification of satellite catalog source. */
    CATALOG_NAME((token, context, metadata) -> token.processAsNormalizedString(metadata::setCatalogName)),

    /** Unique satellite identification designator for the object. */
    OBJECT_DESIGNATOR((token, context, metadata) -> token.processAsNormalizedString(metadata::setObjectDesignator)),

    /** Alternate names fir this space object. */
    ALTERNATE_NAMES((token, context, metadata) -> token.processAsNormalizedStringList(metadata::setAlternateNames)),

    /** Programmatic Point Of Contact at originator. */
    ORIGINATOR_POC((token, context, metadata) -> token.processAsNormalizedString(metadata::setOriginatorPOC)),

    /** Position of Programmatic Point Of Contact at originator. */
    ORIGINATOR_POSITION((token, context, metadata) -> token.processAsNormalizedString(metadata::setOriginatorPosition)),

    /** Phone number of Programmatic Point Of Contact at originator. */
    ORIGINATOR_PHONE((token, context, metadata) -> token.processAsNormalizedString(metadata::setOriginatorPhone)),

    /** Address of Programmatic Point Of Contact at originator. */
    ORIGINATOR_ADDRESS((token, context, metadata) -> token.processAsNormalizedString(metadata::setOriginatorAddress)),

    /** Creating agency or operator. */
    TECH_ORG((token, context, metadata) -> token.processAsNormalizedString(metadata::setTechOrg)),

    /** Technical Point Of Contact at originator. */
    TECH_POC((token, context, metadata) -> token.processAsNormalizedString(metadata::setTechPOC)),

    /** Position of Technical Point Of Contact at originator. */
    TECH_POSITION((token, context, metadata) -> token.processAsNormalizedString(metadata::setTechPosition)),

    /** Phone number of Technical Point Of Contact at originator. */
    TECH_PHONE((token, context, metadata) -> token.processAsNormalizedString(metadata::setTechPhone)),

    /** Address of Technical Point Of Contact at originator. */
    TECH_ADDRESS((token, context, metadata) -> token.processAsNormalizedString(metadata::setTechAddress)),

    /** Unique ID identifying previous message from a given originator. */
    PREVIOUS_MESSAGE_ID((token, context, metadata) -> token.processAsNormalizedString(metadata::setPreviousMessageID)),

    /** Unique ID identifying next message from a given originator. */
    NEXT_MESSAGE_ID((token, context, metadata) -> token.processAsNormalizedString(metadata::setNextMessageID)),

    /** Unique identifier of Attitude Data Message linked to this Orbit Data Message. */
    ADM_MESSAGE_LINK((token, context, metadata) -> token.processAsNormalizedString(metadata::setAdmMessageLink)),

    /** Unique identifier of Conjunction Data Message linked to this Orbit Data Message. */
    CDM_MESSAGE_LINK((token, context, metadata) -> token.processAsNormalizedString(metadata::setCdmMessageLink)),

    /** Unique identifier of Pointing Request Message linked to this Orbit Data Message. */
    PRM_MESSAGE_LINK((token, context, metadata) -> token.processAsNormalizedString(metadata::setPrmMessageLink)),

    /** Unique identifier of Reentry Data Message linked to this Orbit Data Message. */
    RDM_MESSAGE_LINK((token, context, metadata) -> token.processAsNormalizedString(metadata::setRdmMessageLink)),

    /** Unique identifier of Tracking Data Message linked to this Orbit Data Message. */
    TDM_MESSAGE_LINK((token, context, metadata) -> token.processAsNormalizedString(metadata::setTdmMessageLink)),

    /** Operator of the space object. */
    OPERATOR((token, context, metadata) -> token.processAsNormalizedString(metadata::setOperator)),

    /** Owner of the space object. */
    OWNER((token, context, metadata) -> token.processAsNormalizedString(metadata::setOwner)),

    /** Name of the country where the space object owner is based. */
    COUNTRY((token, context, metadata) -> token.processAsNormalizedString(metadata::setCountry)),

    /** Name of the constellation this space object belongs to. */
    CONSTELLATION((token, context, metadata) -> token.processAsNormalizedString(metadata::setConstellation)),

    /** Type of object.
     * @see ObjectType
     */
    OBJECT_TYPE((token, context, metadata) -> {
        if (token.getType() == TokenType.ENTRY) {
            try {
                metadata.setObjectType(ObjectType.valueOf(token.getContentAsNormalizedString().replace(' ', '_')));
            } catch (IllegalArgumentException iae) {
                throw token.generateException(iae);
            }
        }
        return true;
    }),

    /** Default epoch to which <em>all</em> relative times are referenced in data blocks,
     * unless overridden by block-specific {@link #EPOCH_TZERO} values. */
    EPOCH_TZERO((token, context, metadata) -> token.processAsDate(metadata::setEpochT0, context)),

    /** Operational status.
     * @see OpsStatus
     */
    OPS_STATUS((token, context, metadata) -> {
        if (token.getType() == TokenType.ENTRY) {
            try {
                metadata.setOpsStatus(OpsStatus.valueOf(token.getContentAsNormalizedString().replace(' ', '_')));
            } catch (IllegalArgumentException iae) {
                throw token.generateException(iae);
            }
        }
        return true;
    }),

    /** Orbit category.
     * @see OrbitCategory
     */
    ORBIT_CATEGORY((token, context, metadata) -> {
        if (token.getType() == TokenType.ENTRY) {
            try {
                metadata.setOrbitCategory(OrbitCategory.valueOf(token.getContentAsNormalizedString().replace(' ', '_')));
            } catch (IllegalArgumentException iae) {
                throw token.generateException(iae);
            }
        }
        return true;
    }),

    /** List of elements of information data blocks included in this message. */
    OCM_DATA_ELEMENTS((token, context, metadata) -> token.processAsNormalizedStringList(metadata::setOcmDataElements)),

    /** Spacecraft clock count at {@link #EPOCH_TZERO}. */
    SCLK_OFFSET_AT_EPOCH((token, context, metadata) -> token.processAsDouble(1.0, metadata::setSclkOffsetAtEpoch)),

    /** Number of clock seconds occurring during one SI second. */
    SCLK_SEC_PER_SI_SEC((token, context, metadata) -> token.processAsDouble(1.0, metadata::setClockSecPerSISec)),

    /** Creation date of previous message from a given originator. */
    PREVIOUS_MESSAGE_EPOCH((token, context, metadata) -> token.processAsDate(metadata::setPreviousMessageEpoch, context)),

    /** Creation date of next message from a given originator. */
    NEXT_MESSAGE_EPOCH((token, context, metadata) -> token.processAsDate(metadata::setNextMessageEpoch, context)),

    /** Start time entry. */
    START_TIME((token, context, metadata) -> token.processAsDate(metadata::setStartTime, context)),

    /** Stop time entry. */
    STOP_TIME((token, context, metadata) -> token.processAsDate(metadata::setStopTime, context)),

    /** Span of time that the OCM covers. */
    TIME_SPAN((token, context, metadata) -> token.processAsDouble(Constants.JULIAN_DAY, metadata::setTimeSpan)),

    /** Difference (TAI – UTC) in seconds at epoch {@link #EPOCH_TZERO}. */
    TAIMUTC_AT_TZERO((token, context, metadata) -> token.processAsDouble(1.0, metadata::setTaimutcT0)),

    /** Difference (UT1 – UTC) in seconds at epoch {@link #EPOCH_TZERO}. */
    UT1MUTC_AT_TZERO((token, context, metadata) -> token.processAsDouble(1.0, metadata::setUt1mutcT0)),

    /** Source and version of Earth Orientation Parameters. */
    EOP_SOURCE((token, context, metadata) -> token.processAsNormalizedString(metadata::setEopSource)),

    /** Interpolation method for Earth Orientation Parameters. */
    INTERP_METHOD_EOP((token, context, metadata) -> token.processAsNormalizedString(metadata::setInterpMethodEOP)),

    /** Interpolation method for space weather data. */
    INTERP_METHOD_SW((token, context, metadata) -> token.processAsNormalizedString(metadata::setInterpMethodSW)),

    /** Source and version of celestial body (e.g. Sun/Earth/Planetary). */
    CELESTIAL_SOURCE((token, context, metadata) -> token.processAsNormalizedString(metadata::setCelestialSource));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    OcmMetadataKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context parsing context
     * @param metadata metadata to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ParsingContext context, final OcmMetadata metadata) {
        return processor.process(token, context, metadata);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param context parsing context
         * @param metadata metadata to fill
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ParsingContext context, OcmMetadata metadata);
    }

}

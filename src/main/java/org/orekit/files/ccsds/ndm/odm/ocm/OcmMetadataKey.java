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

import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.utils.units.Unit;


/** Keys for {@link OcmMetadata OCM container} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum OcmMetadataKey {

    /** Classification of this message. */
    CLASSIFICATION((token, context, container) -> token.processAsNormalizedString(container::setClassification)),

    /** International designator for the object as assigned by the UN Committee
     * on Space Research (COSPAR) and the US National Space Science Data Center (NSSDC). */
    INTERNATIONAL_DESIGNATOR((token, context, container) -> token.processAsNormalizedString(container::setInternationalDesignator)),

    /** Specification of satellite catalog source. */
    CATALOG_NAME((token, context, container) -> token.processAsNormalizedString(container::setCatalogName)),

    /** Unique satellite identification designator for the object. */
    OBJECT_DESIGNATOR((token, context, container) -> token.processAsNormalizedString(container::setObjectDesignator)),

    /** Alternate names fir this space object. */
    ALTERNATE_NAMES((token, context, container) -> token.processAsNormalizedList(container::setAlternateNames)),

    /** Programmatic Point Of Contact at originator. */
    ORIGINATOR_POC((token, context, container) -> token.processAsNormalizedString(container::setOriginatorPOC)),

    /** Position of Programmatic Point Of Contact at originator. */
    ORIGINATOR_POSITION((token, context, container) -> token.processAsNormalizedString(container::setOriginatorPosition)),

    /** Phone number of Programmatic Point Of Contact at originator. */
    ORIGINATOR_PHONE((token, context, container) -> token.processAsNormalizedString(container::setOriginatorPhone)),

    /** Address of Programmatic Point Of Contact at originator. */
    ORIGINATOR_ADDRESS((token, context, container) -> token.processAsNormalizedString(container::setOriginatorAddress)),

    /** Creating agency or operator. */
    TECH_ORG((token, context, container) -> token.processAsNormalizedString(container::setTechOrg)),

    /** Technical Point Of Contact at originator. */
    TECH_POC((token, context, container) -> token.processAsNormalizedString(container::setTechPOC)),

    /** Position of Technical Point Of Contact at originator. */
    TECH_POSITION((token, context, container) -> token.processAsNormalizedString(container::setTechPosition)),

    /** Phone number of Technical Point Of Contact at originator. */
    TECH_PHONE((token, context, container) -> token.processAsNormalizedString(container::setTechPhone)),

    /** Address of Technical Point Of Contact at originator. */
    TECH_ADDRESS((token, context, container) -> token.processAsNormalizedString(container::setTechAddress)),

    /** Unique ID identifying previous message from a given originator. */
    PREVIOUS_MESSAGE_ID((token, context, container) -> token.processAsNormalizedString(container::setPreviousMessageID)),

    /** Unique ID identifying next message from a given originator. */
    NEXT_MESSAGE_ID((token, context, container) -> token.processAsNormalizedString(container::setNextMessageID)),

    /** Unique identifier of Attitude Data Message linked to this Orbit Data Message. */
    ADM_MESSAGE_LINK((token, context, container) -> token.processAsNormalizedString(container::setAdmMessageLink)),

    /** Unique identifier of Conjunction Data Message linked to this Orbit Data Message. */
    CDM_MESSAGE_LINK((token, context, container) -> token.processAsNormalizedString(container::setCdmMessageLink)),

    /** Unique identifier of Pointing Request Message linked to this Orbit Data Message. */
    PRM_MESSAGE_LINK((token, context, container) -> token.processAsNormalizedString(container::setPrmMessageLink)),

    /** Unique identifier of Reentry Data Message linked to this Orbit Data Message. */
    RDM_MESSAGE_LINK((token, context, container) -> token.processAsNormalizedString(container::setRdmMessageLink)),

    /** Unique identifier of Tracking Data Message linked to this Orbit Data Message. */
    TDM_MESSAGE_LINK((token, context, container) -> token.processAsNormalizedString(container::setTdmMessageLink)),

    /** Operator of the space object. */
    OPERATOR((token, context, container) -> token.processAsNormalizedString(container::setOperator)),

    /** Owner of the space object. */
    OWNER((token, context, container) -> token.processAsNormalizedString(container::setOwner)),

    /** Name of the country where the space object owner is based. */
    COUNTRY((token, context, container) -> token.processAsNormalizedString(container::setCountry)),

    /** Name of the constellation this space object belongs to. */
    CONSTELLATION((token, context, container) -> token.processAsNormalizedString(container::setConstellation)),

    /** Type of object.
     * @see ObjectType
     */
    OBJECT_TYPE((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            try {
                container.setObjectType(ObjectType.valueOf(token.getContentAsUppercaseString().replace(' ', '_')));
            } catch (IllegalArgumentException iae) {
                throw token.generateException(iae);
            }
        }
        return true;
    }),

    /** Default epoch to which <em>all</em> relative times are referenced in data blocks,
     * unless overridden by block-specific {@link #EPOCH_TZERO} values. */
    EPOCH_TZERO((token, context, container) -> token.processAsDate(container::setEpochT0, context)),

    /** Operational status.
     * @see OpsStatus
     */
    OPS_STATUS((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            try {
                container.setOpsStatus(OpsStatus.valueOf(token.getContentAsUppercaseString().replace(' ', '_')));
            } catch (IllegalArgumentException iae) {
                throw token.generateException(iae);
            }
        }
        return true;
    }),

    /** Orbit category.
     * @see OrbitCategory
     */
    ORBIT_CATEGORY((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            try {
                container.setOrbitCategory(OrbitCategory.valueOf(token.getContentAsUppercaseString().replace(' ', '_')));
            } catch (IllegalArgumentException iae) {
                throw token.generateException(iae);
            }
        }
        return true;
    }),

    /** List of elements of information data blocks included in this message. */
    OCM_DATA_ELEMENTS((token, context, container) -> token.processAsUppercaseList(container::setOcmDataElements)),

    /** Spacecraft clock count at {@link #EPOCH_TZERO}. */
    SCLK_OFFSET_AT_EPOCH((token, context, container) -> token.processAsDouble(Unit.SECOND, container::setSclkOffsetAtEpoch)),

    /** Number of clock seconds occurring during one SI second. */
    SCLK_SEC_PER_SI_SEC((token, context, container) -> token.processAsDouble(Unit.ONE, container::setSclkSecPerSISec)),

    /** Creation date of previous message from a given originator. */
    PREVIOUS_MESSAGE_EPOCH((token, context, container) -> token.processAsDate(container::setPreviousMessageEpoch, context)),

    /** Creation date of next message from a given originator. */
    NEXT_MESSAGE_EPOCH((token, context, container) -> token.processAsDate(container::setNextMessageEpoch, context)),

    /** Start time entry. */
    START_TIME((token, context, container) -> token.processAsDate(container::setStartTime, context)),

    /** Stop time entry. */
    STOP_TIME((token, context, container) -> token.processAsDate(container::setStopTime, context)),

    /** Span of time that the OCM covers. */
    TIME_SPAN((token, context, container) -> token.processAsDouble(Unit.DAY, container::setTimeSpan)),

    /** Difference (TAI – UTC) in seconds at epoch {@link #EPOCH_TZERO}. */
    TAIMUTC_AT_TZERO((token, context, container) -> token.processAsDouble(Unit.SECOND, container::setTaimutcT0)),

    /** Difference (UT1 – UTC) in seconds at epoch {@link #EPOCH_TZERO}. */
    UT1MUTC_AT_TZERO((token, context, container) -> token.processAsDouble(Unit.SECOND, container::setUt1mutcT0)),

    /** Source and version of Earth Orientation Parameters. */
    EOP_SOURCE((token, context, container) -> token.processAsNormalizedString(container::setEopSource)),

    /** Interpolation method for Earth Orientation Parameters. */
    INTERP_METHOD_EOP((token, context, container) -> token.processAsNormalizedString(container::setInterpMethodEOP)),

    /** Source and version of celestial body (e.g. Sun/Earth/Planetary). */
    CELESTIAL_SOURCE((token, context, container) -> token.processAsNormalizedString(container::setCelestialSource));

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
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final OcmMetadata container) {
        return processor.process(token, context, container);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param context context binding
         * @param container container to fill
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ContextBinding context, OcmMetadata container);
    }

}

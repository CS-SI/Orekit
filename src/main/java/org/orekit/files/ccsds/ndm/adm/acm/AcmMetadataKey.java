/* Copyright 2023 Luc Maisonobe
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
package org.orekit.files.ccsds.ndm.adm.acm;

import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.utils.units.Unit;


/** Keys for {@link AcmMetadata ACM container} entries.
 * @author Luc Maisonobe
 * @since 12.0
 */
public enum AcmMetadataKey {

    /** International designator for the object as assigned by the UN Committee
     * on Space Research (COSPAR) and the US National Space Science Data Center (NSSDC). */
    INTERNATIONAL_DESIGNATOR((token, context, container) -> token.processAsNormalizedString(container::setInternationalDesignator)),

    /** Specification of satellite catalog source. */
    CATALOG_NAME((token, context, container) -> token.processAsNormalizedString(container::setCatalogName)),

    /** Unique satellite identification designator for the object. */
    OBJECT_DESIGNATOR((token, context, container) -> token.processAsNormalizedString(container::setObjectDesignator)),

    /** Programmatic Point Of Contact at originator. */
    ORIGINATOR_POC((token, context, container) -> token.processAsFreeTextString(container::setOriginatorPOC)),

    /** Position of Programmatic Point Of Contact at originator. */
    ORIGINATOR_POSITION((token, context, container) -> token.processAsFreeTextString(container::setOriginatorPosition)),

    /** Phone number of Programmatic Point Of Contact at originator. */
    ORIGINATOR_PHONE((token, context, container) -> token.processAsFreeTextString(container::setOriginatorPhone)),

    /** Email address of Programmatic Point Of Contact at originator. */
    ORIGINATOR_EMAIL((token, context, container) -> token.processAsFreeTextString(container::setOriginatorEmail)),

    /** Address of Programmatic Point Of Contact at originator. */
    ORIGINATOR_ADDRESS((token, context, container) -> token.processAsFreeTextString(container::setOriginatorAddress)),

    /** Unique identifier of Orbit Data Message linked to this Attitude Data Message. */
    ODM_MSG_LINK((token, context, container) -> token.processAsFreeTextString(container::setOdmMessageLink)),

    /** Default epoch to which <em>all</em> relative times are referenced in data blocks,
     * unless overridden by block-specific {@link #EPOCH_TZERO} values. */
    EPOCH_TZERO((token, context, container) -> token.processAsDate(container::setEpochT0, context)),

    /** List of elements of information data blocks included in this message. */
    ACM_DATA_ELEMENTS((token, context, container) -> token.processAsEnumsList(AcmElements.class, container::setAcmDataElements)),

    /** Start time entry. */
    START_TIME((token, context, container) -> token.processAsDate(container::setStartTime, context)),

    /** Stop time entry. */
    STOP_TIME((token, context, container) -> token.processAsDate(container::setStopTime, context)),

    /** Difference (TAI – UTC) in seconds at epoch {@link #EPOCH_TZERO}. */
    TAIMUTC_AT_TZERO((token, context, container) -> token.processAsDouble(Unit.SECOND, context.getParsedUnitsBehavior(),
                                                                          container::setTaimutcT0)),

    /** Epoch of next leap second. */
    NEXT_LEAP_EPOCH((token, context, container) -> token.processAsDate(container::setNextLeapEpoch, context)),

    /** Difference (TAI – UTC) in seconds incorporated at {@link #NEXT_LEAP_EPOCH}. */
    NEXT_LEAP_TAIMUTC((token, context, container) -> token.processAsDouble(Unit.SECOND, context.getParsedUnitsBehavior(),
                                                                           container::setNextLeapTaimutc));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    AcmMetadataKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final AcmMetadata container) {
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
        boolean process(ParseToken token, ContextBinding context, AcmMetadata container);
    }

}

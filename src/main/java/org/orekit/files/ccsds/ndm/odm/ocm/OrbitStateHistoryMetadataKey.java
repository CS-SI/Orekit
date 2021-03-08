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

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.ElementsType;
import org.orekit.files.ccsds.ndm.odm.oem.InterpolationMethod;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.parsing.ParsingContext;


/** Keys for {@link OrbitStateHistoryMetadata orbit state history container} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum OrbitStateHistoryMetadataKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** Orbit identification number. */
    ORB_ID((token, context, container) -> token.processAsUppercaseString(container::setOrbID)),

    /** Identification number of previous orbit. */
    ORB_PREV_ID((token, context, container) -> token.processAsUppercaseString(container::setOrbPrevID)),

    /** Identification number of next orbit. */
    ORB_NEXT_ID((token, context, container) -> token.processAsUppercaseString(container::setOrbNextID)),

    /** Basis of this orbit state time history data. */
    ORB_BASIS((token, context, container) -> token.processAsUppercaseString(container::setOrbBasis)),

    /** Identification number of the orbit determination or simulation upon which this orbit is based.*/
    ORB_BASIS_ID((token, context, container) -> token.processAsUppercaseString(container::setOrbBasisID)),

    /** Interpolation method to be used. */
    INTERPOLATION((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            try {
                container.setInterpolationMethod(InterpolationMethod.valueOf(token.getContentAsUppercaseString()));
            } catch (IllegalArgumentException iae) {
                throw new OrekitException(iae, OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE,
                                          token.getName(), token.getLineNumber(), token.getFileName());
            }
        }
        return true;
    }),

    /** Interpolation degree. */
    INTERPOLATION_DEGREE((token, context, container) -> token.processAsInteger(container::setInterpolationDegree)),

    /** Type of averaging (Osculating, mean Brouwer, other...). */
    ORB_AVERAGING((token, context, container) -> token.processAsUppercaseString(container::setOrbAveraging)),

    /** Origin of the reference frame of the orbit. */
    CENTER_NAME((token, context, container) -> token.processAsCenter(container::setCenter,
                                                                    context.getDataContext().getCelestialBodies())),

    /** Reference frame of the orbit. */
    ORB_REF_FRAME((token, context, container) -> token.processAsFrame(container::setOrbReferenceFrame, context, true, false, false)),

    /** Epoch of the {@link #ORB_REF_FRAME orbit reference frame}. */
    ORB_FRAME_EPOCH((token, context, container) -> token.processAsDate(container::setOrbFrameEpoch, context)),

    /** Useable start time entry. */
    USEABLE_START_TIME((token, context, container) -> token.processAsDate(container::setUseableStartTime, context)),

    /** Useable stop time entry. */
    USEABLE_STOP_TIME((token, context, container) -> token.processAsDate(container::setUseableStopTime, context)),

    /** Orbit element set type.
     * @see ElementsType
     */
    ORB_TYPE((token, context, container) -> {
        try {
            container.setOrbType(ElementsType.valueOf(token.getContentAsUppercaseString()));
        } catch (IllegalArgumentException iae) {
            throw token.generateException(iae);
        }
        return true;
    }),

    /** SI units for each elements of the orbit state. */
    ORB_UNITS((token, context, container) -> token.processAsUnitList(container::setOrbUnits));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    OrbitStateHistoryMetadataKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context parsing context
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ParsingContext context, final OrbitStateHistoryMetadata container) {
        return processor.process(token, context, container);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param context parsing context
         * @param container container to fill
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ParsingContext context, OrbitStateHistoryMetadata container);
    }

}

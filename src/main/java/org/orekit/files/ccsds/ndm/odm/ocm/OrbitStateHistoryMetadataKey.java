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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.odm.oem.InterpolationMethod;
import org.orekit.files.ccsds.utils.CcsdsUnit;
import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;


/** Keys for {@link OrbitStateHistoryMetadata orbit state history metadata} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum OrbitStateHistoryMetadataKey {

    /** Comment entry. */
    COMMENT((token, context, metadata) ->
            token.getType() == TokenType.ENTRY ? metadata.addComment(token.getContent()) : true),

    /** Orbit identification number. */
    ORB_ID((token, context, metadata) -> token.processAsNormalizedString(metadata::setOrbID)),

    /** Identification number of previous orbit. */
    ORB_PREV_ID((token, context, metadata) -> token.processAsNormalizedString(metadata::setOrbPrevID)),

    /** Identification number of next orbit. */
    ORB_NEXT_ID((token, context, metadata) -> token.processAsNormalizedString(metadata::setOrbNextID)),

    /** Basis of this orbit state time history data. */
    ORB_BASIS((token, context, metadata) -> token.processAsNormalizedString(metadata::setOrbBasis)),

    /** Identification number of the orbit determination or simulation upon which this orbit is based.*/
    ORB_BASIS_ID((token, context, metadata) -> token.processAsNormalizedString(metadata::setOrbBasisID)),

    /** Interpolation method to be used. */
    INTERPOLATION((token, context, metadata) -> {
        if (token.getType() == TokenType.ENTRY) {
            try {
                metadata.setInterpolationMethod(InterpolationMethod.valueOf(token.getContentAsNormalizedString()));
            } catch (IllegalArgumentException iae) {
                throw new OrekitException(iae, OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE,
                                          token.getName(), token.getLineNumber(), token.getFileName());
            }
        }
        return true;
    }),

    /** Interpolation degree. */
    INTERPOLATION_DEGREE((token, context, metadata) -> token.processAsInteger(metadata::setInterpolationDegree)),

    /** Type of averaging (Osculating, mean Brouwer, other...). */
    ORB_AVERAGING((token, context, metadata) -> token.processAsNormalizedString(metadata::setOrbAveraging)),

    /** Origin of the reference frame of the orbit. */
    CENTER_NAME((token, context, metadata) -> {
        if (token.getType() == TokenType.ENTRY) {
            metadata.setCenterName(token.getContentAsNormalizedString(), context.getDataContext().getCelestialBodies());
        }
        return true;
    }),

    /** Reference frame of the orbit. */
    ORB_REF_FRAME((token, context, metadata) -> token.processAsFrame(metadata::setOrbRefFrame, context, false)),

    /** Epoch of the {@link #ORB_REF_FRAME orbit reference frame}. */
    ORB_FRAME_EPOCH((token, context, metadata) -> token.processAsDate(metadata::setOrbFrameEpoch, context)),

    /** Orbit element set type.
     * @see ElementsType
     */
    ORB_TYPE((token, context, metadata) -> {
        try {
            metadata.setOrbType(ElementsType.valueOf(token.getContentAsNormalizedString()));
        } catch (IllegalArgumentException iae) {
            throw token.generateException(iae);
        }
        return true;
    }),

    /** SI units for each elements of the orbit state.
     * @see CCSDSUnit
     */
    ORB_UNITS(new UnitsProcessor());

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
     * @param metadata metadata to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ParsingContext context, final OrbitStateHistoryMetadata metadata) {
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
        boolean process(ParseToken token, ParsingContext context, OrbitStateHistoryMetadata metadata);
    }

    /** dedicated processor for units. */
    private static class UnitsProcessor implements TokenProcessor {

        /** Pattern for splitting units lines. */
        private static final Pattern UNITS_FINDER = Pattern.compile("((?:\\p{Alnum}|\\*|/)+)\\p{Space}*(?:,\\p{Space}*)?");

        public boolean process(final ParseToken token, final ParsingContext context,
                               final OrbitStateHistoryMetadata metadata) {

            final String line = token.getContent();
            if (line.charAt(0) != '[' || line.charAt(line.length() - 1) != ']') {
                throw token.generateException(null);
            }
            final List<CcsdsUnit> orbUnits = new ArrayList<>();
            final Matcher matcher = UNITS_FINDER.matcher(line.substring(1, line.length() - 1).trim());
            while (!matcher.hitEnd()) {
                if (matcher.find()) {
                    final String u = matcher.group(1).replace("*", "").replace('/', '_').toUpperCase(Locale.US);
                    orbUnits.add(CcsdsUnit.valueOf(u));
                } else {
                    throw token.generateException(null);
                }
            }
            metadata.setOrbUnits(orbUnits);
            return true;
        }
    }

}

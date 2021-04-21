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
package org.orekit.files.ccsds.ndm;

import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;

/** Keywords for NDM  structure in XML files.
 * @author Luc Maisonobe
 * @since 11.0
 */
enum NdmStructureKey {

    /** Comment entry. */
    COMMENT((token, parser) ->
            token.getType() == TokenType.ENTRY ? parser.addComment(token.getContentAsNormalizedString()) : true),

    /** Root element. */
    ndm((token, parser) -> true),

    /** TDM constituent. */
    CCSDS_TDM_VERSION((token, parser) -> parser.manageConstituent(builder -> builder.buildTdmParser())),

    /** OPM constituent. */
    CCSDS_OPM_VERSION((token, parser) -> parser.manageConstituent(builder -> builder.buildOpmParser())),

    /** OMM constituent. */
    CCSDS_OMM_VERSION((token, parser) -> parser.manageConstituent(builder -> builder.buildOmmParser())),

    /** OEM constituent. */
    CCSDS_OEM_VERSION((token, parser) -> parser.manageConstituent(builder -> builder.buildOemParser())),

    /** OCM constituent. */
    CCSDS_OCM_VERSION((token, parser) -> parser.manageConstituent(builder -> builder.buildOcmParser())),

    /** APM constituent. */
    CCSDS_APM_VERSION((token, parser) -> parser.manageConstituent(builder -> builder.buildApmParser())),

    /** AEM constituent. */
    CCSDS_AEM_VERSION((token, parser) -> parser.manageConstituent(builder -> builder.buildAemParser()));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    NdmStructureKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param parser OPM file parser
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final NdmParser parser) {
        return processor.process(token, parser);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param parser NDM file parser
         * @return true of token was accepted
         */
        boolean process(ParseToken token, NdmParser parser);
    }

}

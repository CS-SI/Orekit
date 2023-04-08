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
    tdm((token, parser) -> parser.manageTdmConstituent()),

    /** OPM constituent. */
    opm((token, parser) -> parser.manageOpmConstituent()),

    /** OMM constituent. */
    omm((token, parser) -> parser.manageOmmConstituent()),

    /** OEM constituent. */
    oem((token, parser) -> parser.manageOemConstituent()),

    /** OCM constituent. */
    ocm((token, parser) -> parser.manageOcmConstituent()),

    /** APM constituent. */
    apm((token, parser) -> parser.manageApmConstituent()),

    /** AEM constituent. */
    aem((token, parser) -> parser.manageAemConstituent()),

    /** ACM constituent. */
    acm((token, parser) -> parser.manageAcmConstituent()),

    /** CDM constituent. */
    cdm((token, parser) -> parser.manageCdmConstituent());

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

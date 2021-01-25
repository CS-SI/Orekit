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
package org.orekit.files.ccsds.section;

import java.util.Deque;

import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.state.ProcessingState;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.time.AbsoluteDate;

/** {@link ProcessingState} for {@link Header NDM header}.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class HeaderProcessingState implements ProcessingState {

    /** Data context used for getting frames, time scales, and celestial bodies. */
    private final DataContext dataContext;

    /** Key for format version. */
    private final String formatVersionKey;

    /** Header. */
    private final Header header;

    /** Next state to use. */
    private final ProcessingState nextState;

    /** Simple constructor.
     * @param dataContext data context used for getting frames, time scales, and celestial bodies
     * @param formatVersionKey key for format version
     * @param header header to fill
     * @param nextState state to use when this state cannot parse an token by itself
     */
    public HeaderProcessingState(final DataContext dataContext,
                                 final String formatVersionKey,
                                 final Header header,
                                 final ProcessingState nextState) {
        this.dataContext      = dataContext;
        this.nextState        = nextState;
        this.formatVersionKey = formatVersionKey;
        this.header           = header;
    }

    /** {@inheritDoc} */
    @Override
    public ProcessingState processToken(final ParseToken token, final Deque<ParseToken> next) {

        if (Double.isNaN(header.getFormatVersion())) {
            // the first thing we expect is the format version
            // (however, in XML files it is set before entering header)
            if (formatVersionKey.equals(token.getName()) && token.getType() == TokenType.ENTRY) {
                header.setFormatVersion(token.getContentAsDouble());
                return this;
            } else {
                throw new OrekitException(OrekitMessages.UNSUPPORTED_FILE_FORMAT, token.getFileName());
            }
        }

        switch (token.getName()) {

            case "COMMENT" :
                if (header.getCreationDate() != null) {
                    // we have already processed some content in the block
                    // the comment belongs to the next block
                    next.offerLast(token);
                    return nextState;
                }
                token.processAsFreeTextString(header::addComment);
                return this;

            case "CREATION_DATE" :
                if (token.getType() == TokenType.ENTRY) {
                    header.setCreationDate(new AbsoluteDate(token.getContent(),
                                                            dataContext.getTimeScales().getUTC()));
                }
                return this;

            case "ORIGINATOR" :
                token.processAsNormalizedString(header::setOriginator);
                return this;

            default :
                // we were not able to parse the token, we push it back in the queue
                next.offerLast(token);
                return nextState;

        }

    }

}

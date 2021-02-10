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
package org.orekit.files.ccsds.ndm.odm;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.section.HeaderProcessingState;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.state.AbstractMessageParser;
import org.orekit.files.ccsds.utils.state.ProcessingState;

/** {@link ProcessingState} for {@link ODMHeader ODM header}.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class ODMHeaderProcessingState extends HeaderProcessingState {

    /** ODM extended header. */
    private final ODMHeader header;

    /** Simple constructor.
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param parser parser for the complete message
     * @param header ODM extended header
     */
    public ODMHeaderProcessingState(final DataContext dataContext,
                                    final AbstractMessageParser<?, ?> parser,
                                    final ODMHeader header) {
        super(dataContext, parser);
        this.header = header;
    }

    /** {@inheritDoc} */
    @Override
    public boolean processToken(final ParseToken token) {

        if (super.processToken(token)) {
            return true;
        }

        if (token.getType() == TokenType.ENTRY && ODMHeader.MESSAGE_ID.equals(token.getName())) {
            return token.processAsNormalizedString(header::setMessageId);
        }

        // this was not an ODM header token
        return false;

    }

}

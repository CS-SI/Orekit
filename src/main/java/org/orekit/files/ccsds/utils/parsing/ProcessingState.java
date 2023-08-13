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
package org.orekit.files.ccsds.utils.parsing;

import org.orekit.files.ccsds.utils.lexical.MessageParser;
import org.orekit.files.ccsds.utils.lexical.ParseToken;

/**
 * Interface for processing parsing tokens for CCSDS NDM files.
 * <p>
 * This interface is intended for use as the state in
 * state design pattern, the {@link MessageParser message parser}
 * itself being used as the context that holds the active state.
 * </p>
 * @see MessageParser
 *
 * @author Luc Maisonobe
 * @since 11.0
 */
public interface ProcessingState {

    /** Process one token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    boolean processToken(ParseToken token);

}

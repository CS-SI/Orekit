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
package org.orekit.files.ccsds.utils.lexical;

import org.xml.sax.Attributes;

/** Regular builder using XML elements names and content for tokens.
 * <p>
 * Instances of this class are immutable.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class RegularXmlTokenBuilder implements XmlTokenBuilder {

    /** Attribute name for units. */
    private static final String UNITS = "units";

    /** {@inheritDoc} */
    @Override
    public ParseToken buildToken(final boolean startTag, final String qName,
                                 final String content, final Attributes attributes,
                                 final int lineNumber, final String fileName) {

        // elaborate the token type
        final TokenType tokenType = (content == null) ?
                                    (startTag ? TokenType.START : TokenType.STOP) :
                                    TokenType.ENTRY;

        // final build
        return new ParseToken(tokenType, qName, content, attributes.getValue(UNITS),
                              lineNumber, fileName);

    }

}

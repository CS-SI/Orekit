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

/** Builder for the root element with CCSDS message version.
 * <p>
 * Instances of this class are immutable.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class MessageVersionXmlTokenBuilder implements XmlTokenBuilder {

    /** Attribute name for id. */
    private static final String ID = "id";

    /** Attribute name for version. */
    private static final String VERSION = "version";

    /** {@inheritDoc} */
    @Override
    public ParseToken buildToken(final boolean startTag, final String qName,
                                 final String content, final Attributes attributes,
                                 final int lineNumber, final String fileName) {
        if (startTag) {
            // we replace the start tag with the message version specification
            return new ParseToken(TokenType.ENTRY,
                                  attributes.getValue(ID),
                                  attributes.getValue(VERSION),
                                  null,
                                  lineNumber, fileName);
        } else {
            return new ParseToken(TokenType.STOP, qName, null, null, lineNumber, fileName);
        }
    }

}

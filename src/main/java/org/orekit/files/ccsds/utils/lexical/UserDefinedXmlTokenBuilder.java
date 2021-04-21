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

import java.util.Collections;
import java.util.List;

import org.orekit.files.ccsds.ndm.odm.UserDefined;
import org.orekit.utils.units.Unit;
import org.xml.sax.Attributes;

/** Builder for user-defined parameters.
 * <p>
 * User-defined elements are of the form:
 * </p>
 * <pre>
 *   &lt;USER_DEFINED parameter="SOME_PARAMETER_NAME"&gt;value&lt;/USER_DEFINED&gt;
 * </pre>
 * <p>
 * This {@link XmlTokenBuilder token builder} will generate a single
 * {@link ParseToken parse token} from this root element with name set to
 * "SOME_PARAMETER_NAME", type set to {@link TokenType#ENTRY} and content
 * set to {@code value}.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class UserDefinedXmlTokenBuilder implements XmlTokenBuilder {

    /** {@inheritDoc} */
    @Override
    public List<ParseToken> buildTokens(final boolean startTag, final String qName,
                                        final String content, final Attributes attributes,
                                        final int lineNumber, final String fileName) {

        // elaborate the token type
        final TokenType tokenType = (content == null) ?
                                    (startTag ? TokenType.START : TokenType.STOP) :
                                    TokenType.ENTRY;

        // final build
        final String name = attributes.getValue(UserDefined.USER_DEFINED_XML_ATTRIBUTE);
        final ParseToken token = new ParseToken(tokenType,
                                                UserDefined.USER_DEFINED_PREFIX + name,
                                                content, Unit.NONE,
                                                lineNumber, fileName);

        return Collections.singletonList(token);

    }

}

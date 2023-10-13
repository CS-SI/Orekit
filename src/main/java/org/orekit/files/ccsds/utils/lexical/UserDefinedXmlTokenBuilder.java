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
package org.orekit.files.ccsds.utils.lexical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.orekit.files.ccsds.ndm.odm.UserDefined;
import org.orekit.utils.units.Unit;

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

    /** Empty constructor.
     * <p>
     * This constructor is not strictly necessary, but it prevents spurious
     * javadoc warnings with JDK 18 and later.
     * </p>
     * @since 12.0
     */
    public UserDefinedXmlTokenBuilder() {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public List<ParseToken> buildTokens(final boolean startTag, final boolean isLeaf, final String qName,
                                        final String content, final Map<String, String> attributes,
                                        final int lineNumber, final String fileName) {

        // elaborate name
        final String name = UserDefined.USER_DEFINED_PREFIX +
                            attributes.get(UserDefined.USER_DEFINED_XML_ATTRIBUTE);

        if (startTag) {
            return Collections.singletonList(new ParseToken(TokenType.START, name, content, Unit.NONE, lineNumber, fileName));
        } else {
            final List<ParseToken> built = new ArrayList<>(2);
            if (isLeaf) {
                built.add(new ParseToken(TokenType.ENTRY, name, content, Unit.NONE, lineNumber, fileName));
            }
            built.add(new ParseToken(TokenType.STOP, name, null, Unit.NONE, lineNumber, fileName));
            return built;
        }

    }

}

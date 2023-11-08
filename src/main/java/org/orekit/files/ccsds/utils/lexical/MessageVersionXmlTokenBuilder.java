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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.orekit.utils.units.Unit;

/** Builder for the root element with CCSDS message version.
 * <p>
 * All parsers for CCSDS ADM, ODM and TDM messages need to handle the
 * root level XML element specially. OPM file for example have a root
 * element of the form:
 * </p>
 * <pre>
 *   &lt;opm id="CCSDS_OPM_VERS" verion="3.0"&gt;
 * </pre>
 * <p>
 * This {@link XmlTokenBuilder token builder} will generate two
 * {@link ParseToken parse tokens} from this root element:
 * </p>
 * <ol>
 *   <li>one with name set to "opm", type set to {@link TokenType#START} and no content</li>
 *   <li>one with name set to "CCSDS_OPM_VERS", type set to {@link TokenType#ENTRY} and content set to "3.0"</li>
 * </ol>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class MessageVersionXmlTokenBuilder implements XmlTokenBuilder {

    /** Attribute name for id. */
    private static final String ID = "id";

    /** Attribute name for version. */
    private static final String VERSION = "version";

    /** Empty constructor.
     * <p>
     * This constructor is not strictly necessary, but it prevents spurious
     * javadoc warnings with JDK 18 and later.
     * </p>
     * @since 12.0
     */
    public MessageVersionXmlTokenBuilder() {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public List<ParseToken> buildTokens(final boolean startTag, final boolean isLeaf, final String qName,
                                        final String content, final Map<String, String> attributes,
                                        final int lineNumber, final String fileName) {
        if (startTag) {
            // we replace the start tag with the message version specification
            final String     id      = attributes.get(ID);
            final String     version = attributes.get(VERSION);
            final ParseToken start   = new ParseToken(TokenType.START, qName, null, Unit.NONE, lineNumber, fileName);
            final ParseToken entry   = new ParseToken(TokenType.ENTRY, id, version, Unit.NONE, lineNumber, fileName);
            return Arrays.asList(start, entry);
        } else {
            final ParseToken stop = new ParseToken(TokenType.STOP, qName, null, Unit.NONE, lineNumber, fileName);
            return Collections.singletonList(stop);
        }
    }

}

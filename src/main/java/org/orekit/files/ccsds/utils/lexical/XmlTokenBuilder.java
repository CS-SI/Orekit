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

import java.util.List;
import java.util.Map;

/** Builder for building {@link ParseToken} from XML elements.
 * <p>
 * The regular handling of regular XML elements is to used the element
 * name as the token name, the element content as the token content and
 * the "units" attribute for the units. In some cases however the token
 * name should be extracted from attributes, and sometimes even the
 * content. This interface allows to define all these behaviors, by
 * providing specialized builders to the lexical analyzer when it calls
 * their {@link MessageParser#getSpecialXmlElementsBuilders()
 * getSpecialXmlElementsHandlers} method.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public interface XmlTokenBuilder {


    /** Create a list of parse tokens.
     * @param startTag if true we are parsing the start tag from an XML element
     * @param isLeaf if true and startTag is false, we are processing the end tag of a leaf XML element
     * @param qName element qualified name
     * @param content element content
     * @param attributes element attributes
     * @param lineNumber number of the line in the CCSDS data message
     * @param fileName name of the file
     * @return list of parse tokens
     * @since 12.0
     */
    List<ParseToken> buildTokens(boolean startTag, boolean isLeaf, String qName,
                                 String content, Map<String, String> attributes,
                                 int lineNumber, String fileName);

}

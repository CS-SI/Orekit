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

/** Builder for building {@link ParseToken} from XML elements.
 * <p>
 * The regular handling of regular XML elements is to used the element name
 * as the token name, the element content as the token content and the
 * "units" attribute for the units. In some cases however the token name
 * should be extracted from attributes, and sometimes even the content. This
 * interface allows to define all these behaviors, by providing special builders
 * to the lexical analyzer when it calls their {@link MessageParser#getSpecialXmlElementsBuilders()
 * getSpecialXmlElementsHandlers} method.
 * </p>
 * <p>
 * A typical example, needed for all parsers, is to handle the top level XML
 * element. The {@link org.orekit.files.ccsds.ndm.odm.opm.OpmParser OPM parser}
 * for example would put in this list an {@link XmlTokenBuilder} configured
 * with {@code name = "opm"}, {@code nameAttributes= ["id"]} and
 * {@code contentAttribute="version"}. With this setting, when the
 * lexical analyzer sees the element: {@code <opm id="CCSDS_OPM_VERS" version="3.0">}
 * it generates a {@link ParseToken} with name set to "CCSDS_OPM_VERS"
 * and content set to "3.0".
 * <p>
 * <p>
 * If multiple attributes can be used to get the token name, they are tested
 * in turn and the first match is used. This is useful in ADM files where
 * the XML elements {@code rotation1},  {@code rotation2}, and {@code rotation3},
 * use attribute {@link angle} in rotation angle sub-section but use attribute
 * {@link rate} in rotation rate sub-section.
 * @author Luc Maisonobe
 * @since 11.0
 */
public interface XmlTokenBuilder {


    /** Create a token.
     * @param startTag if true we are parsing the start tag from an XML element
     * @param qName element qualified name
     * @param content element content
     * @param attributes element attributes
     * @param lineNumber number of the line in the CCSDS data message
     * @param fileName name of the file
     * @return parse token
     */
    ParseToken buildToken(boolean startTag, String qName,
                          String content, Attributes attributes,
                          int lineNumber, String fileName);

}

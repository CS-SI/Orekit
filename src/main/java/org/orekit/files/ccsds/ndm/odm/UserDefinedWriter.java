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

package org.orekit.files.ccsds.ndm.odm;

import java.io.IOException;
import java.util.Map;

import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.XmlGenerator;

/** Writer for user defined parameters data.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class UserDefinedWriter extends AbstractWriter {

    /** User defined parameters block. */
    private final UserDefined userDefined;

    /** Create a writer.
     * @param xmlTag name of the XML tag surrounding the section
     * @param kvnTag name of the KVN tag surrounding the section (may be null)
     * @param userDefined user defined parameters to write
     */
    public UserDefinedWriter(final String xmlTag, final String kvnTag, final UserDefined userDefined) {
        super(xmlTag, kvnTag);
        this.userDefined   = userDefined;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // user-defined parameters block
        generator.writeComments(userDefined.getComments());

        // entries
        if (generator.getFormat() == FileFormat.XML) {
            final XmlGenerator xmlGenerator = (XmlGenerator) generator;
            for (Map.Entry<String, String> entry : userDefined.getParameters().entrySet()) {
                xmlGenerator.writeOneAttributeElement(UserDefined.USER_DEFINED_XML_TAG,       entry.getValue(),
                                                      UserDefined.USER_DEFINED_XML_ATTRIBUTE, entry.getKey());
            }
        } else {
            for (Map.Entry<String, String> entry : userDefined.getParameters().entrySet()) {
                generator.writeEntry(UserDefined.USER_DEFINED_PREFIX + entry.getKey(), entry.getValue(), null, false);
            }
        }

    }

}

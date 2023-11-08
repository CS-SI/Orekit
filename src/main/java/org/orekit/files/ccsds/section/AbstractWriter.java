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

package org.orekit.files.ccsds.section;

import java.io.IOException;

import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;

/** Top level class for writing CCSDS message sections.
 * @author Luc Maisonobe
 * @since 11.0
 */
public abstract class AbstractWriter {

    /** Name of the XML tag surrounding the section. */
    private String xmlTag;

    /** Name of the KVN tag surrounding the section (may be null). */
    private String kvnTag;

    /** Simple constructor.
     * @param xmlTag name of the XML tag surrounding the section
     * @param kvnTag name of the KVN tag surrounding the section (may be null)
     */
    protected AbstractWriter(final String xmlTag, final String kvnTag) {
        this.xmlTag = xmlTag;
        this.kvnTag = kvnTag;
    }

    /** Write the section, including surrounding tags.
     * @param generator generator to use for producing output
     * @throws IOException if any buffer writing operations fails
     */
    public void write(final Generator generator) throws IOException {
        enterSection(generator);
        writeContent(generator);
        exitSection(generator);
    }

    /** Enter the section.
     * @param generator generator to use for producing output
     * @throws IOException if an I/O error occurs.
     * @since 12.0
     */
    public void enterSection(final Generator generator) throws IOException {
        if (generator.getFormat() == FileFormat.XML) {
            generator.enterSection(xmlTag);
        } else if (generator.getFormat() == FileFormat.KVN && kvnTag != null) {
            generator.enterSection(kvnTag);
        }
    }

    /** Exit the section.
     * @param generator generator to use for producing output
     * @throws IOException if an I/O error occurs.
     * @since 12.0
     */
    public void exitSection(final Generator generator) throws IOException {
        if (generator.getFormat() == FileFormat.XML ||
            generator.getFormat() == FileFormat.KVN && kvnTag != null) {
            generator.exitSection();
        }
    }

    /** Write the content of the section, excluding surrounding tags.
     * @param generator generator to use for producing output
     * @throws IOException if any buffer writing operations fails
     */
    protected abstract void writeContent(Generator generator) throws IOException;

    /** Convert an array of integer to a comma-separated list.
     * @param integers integers to write
     * @return arrays as a string
     */
    protected String intArrayToString(final int[] integers) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < integers.length; ++i) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(integers[i]);
        }
        return builder.toString();
    }

}

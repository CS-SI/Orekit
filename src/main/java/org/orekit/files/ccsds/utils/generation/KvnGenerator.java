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
package org.orekit.files.ccsds.utils.generation;

import java.io.IOException;

import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.utils.FileFormat;

/** Generator for Key-Value Notation CCSDS messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class KvnGenerator extends AbstractGenerator {

    /** Comment keyword. */
    private static final String COMMENT = "COMMENT";

    /** Start suffix for sections. */
    private static final String START = "_START";

    /** Stop suffix for sections. */
    private static final String STOP = "_STOP";

    /** String format used for all key/value pair lines. **/
    private final String kvFormat;

    /** String format used for all comment lines. **/
    private final String commentFormat;

    /** Simple constructor.
     * @param output destination of generated output
     * @param keyWidth minimum width of the key (can be used for aligning the '=' sign),
     * TDM needs 25 characters for its longest key, other messages need 20 characters at most
     * @param fileName file name for error messages
     */
    public KvnGenerator(final Appendable output, final int keyWidth, final String fileName) {
        super(output, fileName);
        kvFormat = "%-" + keyWidth + "s = %s%n";
        final StringBuilder builder = new StringBuilder(COMMENT);
        builder.append(' ');
        while (builder.length() < keyWidth + 3) {
            builder.append(' ');
        }
        builder.append("%s%n");
        commentFormat = builder.toString();
    }

    /** {@inheritDoc} */
    @Override
    public FileFormat getFormat() {
        return FileFormat.KVN;
    }

    /** {@inheritDoc} */
    @Override
    public void startMessage(final String messageTypeKey, final double version) throws IOException {
        writeEntry(messageTypeKey, String.format(STANDARDIZED_LOCALE, "%.1f", version), true);
    }

    /** {@inheritDoc} */
    @Override
    public void writeComments(final CommentsContainer comments) throws IOException {
        for (final String comment : comments.getComments()) {
            append(String.format(STANDARDIZED_LOCALE, commentFormat, comment));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void writeEntry(final String key, final String value, final boolean mandatory) throws IOException {
        if (value == null) {
            complain(key, mandatory);
        } else {
            append(String.format(STANDARDIZED_LOCALE, kvFormat, key, value));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void enterSection(final String name) throws IOException {
        append(name).append(START).newLine();
        super.enterSection(name);
    }

    /** {@inheritDoc} */
    @Override
    public String exitSection() throws IOException {
        final String name = super.exitSection();
        append(name).append(STOP).newLine();
        return name;
    }

}

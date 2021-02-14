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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Utility class for selecting either {@link XMLLexicalAnalyzer} or {@link KVNLexicalAnalyzer} depending on
 * data first bytes.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class LexicalAnalyzerSelector {

    /** Buffer size. */
    private static final int BUFFER = 4096;

    /** First bytes in XML document, UCS-4, big-endian, with Byte Order Mark. */
    private static final byte[] UCS_4_BE_BOM = {
        0x00, 0x00, -0x02, -0X01, 0x00, 0x00, 0x00, 0x3c, 0x00, 0x00, 0x00, 0x3f, 0x00, 0x00, 0x00, 0x78, 0x00, 0x00, 0x00, 0x6d, 0x00, 0x00, 0x00, 0x6c
    };

    /** First bytes in XML document, UCS-4, little-endian, with Byte Order Mark. */
    private static final byte[] UCS_4_LE_BOM = {
        -0x01, -0X02, 0x00, 0x00, 0x3c, 0x00, 0x00, 0x00, 0x3f, 0x00, 0x00, 0x00, 0x78, 0x00, 0x00, 0x00, 0x6d, 0x00, 0x00, 0x00, 0x6c, 0x00, 0x00, 0x00
    };

    /** First bytes in XML document, UTF-16, big-endian, with Byte Order Mark. */
    private static final byte[] UTF_16_BE_BOM = {
        -0x02, -0X01, 0x00, 0x3c, 0x00, 0x3f, 0x00, 0x78, 0x00, 0x6d, 0x00, 0x6c
    };

    /** First bytes in XML document, UTF-16, little-endian, with Byte Order Mark. */
    private static final byte[] UTF_16_LE_BOM = {
        -0x01, -0X02, 0x3c, 0x00, 0x3f, 0x00, 0x78, 0x00, 0x6d, 0x00, 0x6c, 0x00
    };

    /** First bytes in XML document, UTF-8, endianness irrelevant, with Byte Order Mark. */
    private static final byte[] UTF_8_BOM = {
        -0x11, -0x45, -0x41, 0x3c, 0x3f, 0x78, 0x6d, 0x6c
    };

    /** First bytes in XML document, UCS-4, big-endian, without Byte Order Mark. */
    private static final byte[] UCS_4_BE = {
        0x00, 0x00, 0x00, 0x3c, 0x00, 0x00, 0x00, 0x3f, 0x00, 0x00, 0x00, 0x78, 0x00, 0x00, 0x00, 0x6d, 0x00, 0x00, 0x00, 0x6c
    };

    /** First bytes in XML document, UCS-4, little-endian, without Byte Order Mark. */
    private static final byte[] UCS_4_LE = {
        0x3c, 0x00, 0x00, 0x00, 0x3f, 0x00, 0x00, 0x00, 0x78, 0x00, 0x00, 0x00, 0x6d, 0x00, 0x00, 0x00, 0x6c, 0x00, 0x00, 0x00
    };

    /** First bytes in XML document, UTF-16, big-endian, without Byte Order Mark. */
    private static final byte[] UTF_16_BE = {
        0x00, 0x3c, 0x00, 0x3f, 0x00, 0x78, 0x00, 0x6d, 0x00, 0x6c
    };

    /** First bytes in XML document, UTF-16, little-endian, without Byte Order Mark. */
    private static final byte[] UTF_16_LE = {
        0x3c, 0x00, 0x3f, 0x00, 0x78, 0x00, 0x6d, 0x00, 0x6c, 0x00
    };

    /** First bytes in XML document, UTF-8, endianness irrelevant, without Byte Order Mark. */
    private static final byte[] UTF_8 = {
        0x3c, 0x3f, 0x78, 0x6d, 0x6c
    };


    /** Private constructor for a utility class.
     */
    private LexicalAnalyzerSelector() {
        // never called
    }

    /** Select a {@link LexicalAnalyzer} for a {@link DataSource} based on content.
     * @param source data source to analyze
     * @return lexical analyzer suited for the data source format
     * @throws IOException if first bytes of source cannot be read
     */
    public static LexicalAnalyzer select(final DataSource source) throws IOException {

        final InputStream is = source.getStreamOpener().openStream();
        if (is == null) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, source.getName());
        }
        final BufferedInputStream bis = new BufferedInputStream(is, BUFFER);

        // read the first bytes
        final int size = UCS_4_BE_BOM.length; // UCS-4 with BOM is the longest reference sequence
        bis.mark(size);
        final byte[] first = new byte[size];
        int read = 0;
        while (read < first.length) {
            final int n = bis.read(first, read, size - read);
            if (n < 0) {
                // the file is too short for a proper CCSDS message,
                // we return arbitrarily a KVN lexical analyzer,
                // anyway, it will fail shortly during parsing
                bis.reset();
                return new KVNLexicalAnalyzer(new DataSource(source.getName(), () -> bis));
            }
            read += n;
        }

        // attempt to recognize an XML prolog, taking care of Byte Order Mark and encoding
        // we use the tables from section F of Extensible Markup Language (XML) 1.0 (Fifth Edition)
        // W3C Recommendation 26 November 2008 (https://www.w3.org/TR/2008/REC-xml-20081126/#sec-guessing),
        // ignoring the unusual octet orders 2143 and 3412
        if (checkSequence(first, UTF_8)     || checkSequence(first, UTF_8_BOM)     ||
            checkSequence(first, UTF_16_LE) || checkSequence(first, UTF_16_LE_BOM) ||
            checkSequence(first, UTF_16_BE) || checkSequence(first, UTF_16_BE_BOM) ||
            checkSequence(first, UCS_4_LE)  || checkSequence(first, UCS_4_LE_BOM)  ||
            checkSequence(first, UCS_4_BE)  || checkSequence(first, UCS_4_BE_BOM)) {
            // we recognized the "<?xml" sequence at start of an XML file
            bis.reset();
            return new XMLLexicalAnalyzer(new DataSource(source.getName(), () -> bis));
        } else {
            // it was not XML, the only other option is KVN
            bis.reset();
            return new KVNLexicalAnalyzer(new DataSource(source.getName(), () -> bis));
        }

    }

    /** Check if first bytes match reference sequence.
     * @param first first bytes read
     * @param reference reference sequence
     * @return true if first bytes match reference sequence
     */
    private static boolean checkSequence(final byte[] first, final byte[] reference) {
        for (int i = 0; i < reference.length; ++i) {
            if (first[i] != reference[i]) {
                return false;
            }
        }
        return true;
    }

}

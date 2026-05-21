/* Contributed in the public domain.
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
package org.orekit.files.spice.binary.daf.generic;

import java.util.regex.Pattern;

/**
 * Constants used for DAF file parsing and writing.
 *
 * @author Rafael Ayala
 * @since 14.0
 */
public final class DAFConstants {

    /**
     * Each DAF record is 1024 bytes.
     */
    public static final int RECORD_LENGTH_BYTES = 1024;

    /**
     * Each double is 8 bytes.
     */
    public static final int DOUBLE_SIZE_BYTES = 8;

    /**
     * Each int is 4 bytes.
     */
    public static final int INT_SIZE_BYTES = 4;

    /**
     * Number of doubles per record (128 = 1024 / 8).
     */
    public static final int DOUBLES_PER_RECORD = RECORD_LENGTH_BYTES / DOUBLE_SIZE_BYTES;

    /**
     * Number of control words stored at the beginning of each summary record.
     * Note that even though these are integers, they are stored as doubles.
     */
    public static final int SUMMARY_RECORD_CONTROL_WORDS = 3;

    /**
     * Maximum number of summary doubles that could be stored in a summary record.
     * This is calculated as DOUBLES_PER_RECORD minus SUMMARY_RECORD_CONTROL_WORDS.
     */
    public static final int SUMMARY_RECORD_MAX_SUMMARY_DOUBLES = DOUBLES_PER_RECORD - SUMMARY_RECORD_CONTROL_WORDS;

    /**
     * Maximum number of characters in a comment record.
     */
    public static final int COMMENT_RECORD_MAX_CHARS = 1000;

    /**
     * Maximum number of characters in type string.
     * This is LOCIDW_MAX_CHARS in SPICE docs.
     */
    public static final int TYPE_STRING_LENGTH = 8;

    /**
     * Number of characters in endian string.
     */
    public static final int ENDIAN_STRING_LENGTH = 8;

    /**
     * Maximum number of characters in description.
     * This is LOCIFN_MAX_CHARS in SPICE docs.
     */
    public static final int DESCRIPTION_LENGTH = 60;

    /**
     * Byte offset for FTP string.
     */
    public static final int FTP_STRING_OFFSET = 699;

    /**
     * Standard valid FTPSTR for integrity checking.
     */
    public static final String FTPSTR = "FTPSTR:\r:\n:\r\n:\r\0:\u0081:\u0010\u00ce:ENDFTP";

    /**
     * Number of characters in the FTP string.
     */
    public static final int FTP_STRING_LENGTH = 28;

    /**
     * Little endian string.
     */
    public static final String LITTLE_ENDIAN_STRING = "LTL-IEEE";

    /**
     * Big endian string.
     */
    public static final String BIG_ENDIAN_STRING = "BIG-IEEE";

    /**
     * Pattern for validating DAF file type string format.
     * File type should be "DAF/" followed by 0 to 4 characters.
     */
    public static final Pattern FILE_TYPE_PATTERN = Pattern.compile("DAF/.{0,4}");

    /**
     * Buffer size for parsing DAF files.
     */
    public static final int BUFFER_SIZE = 8192;

    /**
     * ASCII code for null character.
     */
    public static final byte NULL_ASCII = 0;

    /**
     * ASCII code for EOT character.
     */
    public static final byte EOT_ASCII = 4;

    /**
     * ASCII code for space character.
     */
    public static final byte SPACE_ASCII = 32;

    /**
     * Private constructor to prevent instantiation.
     */
    private DAFConstants() {
        // utility class
    }
}

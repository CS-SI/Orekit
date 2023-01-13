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
package org.orekit.gnss.metric.ntrip;

import java.util.regex.Pattern;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Enumerate for data format in {@link DataStreamRecord}.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum DataFormat {

    /** RTCM 2.x. */
    RTCM_2("RTCM 2(?:[\\.0-9]+)?"),

    /** RTCM 3.x. */
    RTCM_3("RTCM 3(?:[\\.0-9]+)?"),

    /** RTCM SAPOS. */
    RTCM_SAPOS("RTCM SAPOS"),

    /** CMR. */
    CMR("CMR"),

    /** CMR+. */
    CMR_PLUS("CMR +"),

    /** SAPOS-AdV. */
    SAPOS_ADV("SAPOS-AdV"),

    /** RTCA. */
    RTCA("RTCA"),

    /** RAW. */
    RAW("RAW"),

    /** RINEX. */
    RINEX("RINEX"),

    /** SP3. */
    SP3("SP3"),

    /** BINEX. */
    BINEX("BINEX");

    /** Keyword pattern. */
    private final Pattern pattern;

    /** Simple constructor.
     * @param keywordPattern pattern for keyword in the sourcetable records
     */
    DataFormat(final String keywordPattern) {
        this.pattern = Pattern.compile(keywordPattern);
    }

    /** Get pattern for keyword.
     * @return pattern for keyword
     */
    private Pattern getPattern() {
        return pattern;
    }

    /** Get the message type corresponding to a keyword.
     * @param keyword data format keyword
     * @return the message type corresponding to the keyword
     */
    public static DataFormat getDataFormat(final String keyword) {
        for (final DataFormat format : values()) {
            if (format.getPattern().matcher(keyword).matches()) {
                return format;
            }
        }
        throw new OrekitException(OrekitMessages.UNKNOWN_DATA_FORMAT, keyword);
    }

}

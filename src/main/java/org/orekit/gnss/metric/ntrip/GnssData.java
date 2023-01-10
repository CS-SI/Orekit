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

import java.util.Arrays;

/** GNSS data retrieved from Ntrip caster.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class GnssData {

    /** Data bytes. */
    private final byte[] data;

    /** Build a GNSS data bloc.
     * @param data data bytes
     * @param len length of the data (may be smaller than {@code data.length}
     */
    public GnssData(final byte[] data, final int len) {
        this.data = Arrays.copyOf(data, len);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < data.length; ++i) {
            builder.append(Integer.toHexString(Byte.toUnsignedInt(data[i])));
            if ((i % 16) == 15) {
                builder.append(System.getProperty("line.separator"));
            }
        }
        return builder.toString();
    }

}

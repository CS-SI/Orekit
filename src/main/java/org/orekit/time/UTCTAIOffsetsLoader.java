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
package org.orekit.time;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Interface for loading UTC-TAI offsets data files.
 * @author Luc Maisonobe
 * @since 7.1
 */
public interface UTCTAIOffsetsLoader {

    /** Load UTC-TAI offsets entries.
     * @return sorted UTC-TAI offsets entries (if the linear offsets used
     * prior to 1972 are missing, they will be inserted automatically)
     */
    List<OffsetModel> loadOffsets();

    /**
     * Interface for parsing UTC-TAI offsets from a stream.
     *
     * @author Evan Ward
     * @since 10.1
     */
    interface Parser {

        /**
         * Parse leap seconds from the input stream.
         *
         * @param input stream to parse.
         * @param name  of the input stream to use in error messages.
         * @return parsed UTC-TAI offsets.
         * @throws IOException if {@code input} throws one during parsing.
         */
        List<OffsetModel> parse(InputStream input, String name) throws IOException;

    }

}

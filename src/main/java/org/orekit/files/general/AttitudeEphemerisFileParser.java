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
package org.orekit.files.general;

import org.orekit.data.DataSource;

/**
 * Parse an ephemeris file.
 *
 * @param <T> type of the parsed file
 * @author Raphaël Fermé
 * @since 10.3
 */
public interface AttitudeEphemerisFileParser<T extends AttitudeEphemerisFile<?, ?>> {

    /**
     * Parse an attitude ephemeris file from a data source.
     *
     * @param source source providing the data to parse
     * @return a parsed attitude ephemeris file.
     */
    T parse(DataSource source);

}

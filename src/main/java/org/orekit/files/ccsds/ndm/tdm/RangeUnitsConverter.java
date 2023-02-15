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
package org.orekit.files.ccsds.ndm.tdm;

import org.orekit.time.AbsoluteDate;

/** Interface for converting {@link RangeUnits#RU Range Units} to meters.
 * <p>
 * Implementations of this interface must be provided by user when dealing
 * with {@link Tdm Tracking Data Messages} that include range observations
 * in {@link RangeUnits#RU Range Units}. These units are intended for mission-specific
 * measurements and must be described in an Interface Control Document.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public interface RangeUnitsConverter {

    /** Convert a range expressed in {@link RangeUnits#RU Range Units}.
     * @param metadata metadata corresponding to the observation
     * @param date observation date
     * @param range range value in {@link RangeUnits#RU Range Units}
     * @return range range value in meters
     */
    double ruToMeters(TdmMetadata metadata, AbsoluteDate date, double range);

    /** Convert a range expressed in meters.
     * @param metadata metadata corresponding to the observation
     * @param date observation date
     * @param range range value in meters
     * @return range range value in {@link RangeUnits#RU Range Units}
     */
    double metersToRu(TdmMetadata metadata, AbsoluteDate date, double range);

}

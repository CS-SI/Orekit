/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.util.SortedMap;

import org.orekit.data.DataLoader;
import org.orekit.errors.OrekitException;

/** Interface for loading UTC-TAI offsets data files.
 * @author Pascal Parraud
 * @deprecated as of 7.1, replaced with {@link UTCTAIOffsetsLoader}
 */
@Deprecated
public interface UTCTAILoader extends DataLoader {

    /** Load UTC-TAI offsets entries.
     * <p>
     * Only the integer offsets used since 1972-01-01 are loaded here, the
     * linear offsets used between 1961-01-01 and 1971-12-31 are hard-coded
     * in the {@link UTCScale UTCScale} class itself.
     * </p>
     * @return sorted UTC-TAI offsets entries (may be empty)
     * @exception OrekitException if time steps are inconsistent
     */
    SortedMap<DateComponents, Integer> loadTimeSteps() throws OrekitException;

    /** Get the regular expression for supported UTC-TAI offsets files names.
     * @return regular expression for supported UTC-TAI offsets files names
     */
    String getSupportedNames();

}

/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

/** Interface for loading UTC-TAI offsets data files.
 * @author Pascal Parraud
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public interface UTCTAILoader extends DataLoader {

    /** Load UTC-TAI offsets entries.
     * @return sorted UTC-TAI offsets entries (may be empty)
     */
    SortedMap<DateComponents, Integer> loadTimeSteps();

    /** Get the regular expression for supported UTC-TAI offsets files names.
     * @return regular expression for supported UTC-TAI offsets files names
     */
    String getSupportedNames();

}

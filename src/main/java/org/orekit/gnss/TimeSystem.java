/* Copyright 2002-2025 CS GROUP
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
package org.orekit.gnss;

import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;

/** Time systems used in navigation files.
 *
 * @author Thomas Neidhart
 * @author Evan Ward
 * @author Thomas Paulet
 * @since 11.0
 */
public interface TimeSystem {

    /** Get the 3 letters key of the time system.
     * @return 3 letters key
     * @since 12.0
     */
    String getKey();

    /** Get the two letters code.
     * @return two letters code (may be null for non-GNSS time systems)
     * @since 12.2
     */
    String getTwoLettersCode();

    /** Get the one letter code.
     * @return one letter code (may be null for non-GNSS time systems)
     * @since 12.2
     */
    String getOneLetterCode();

    /** Get the time scale corresponding to time system.
     * @param timeScales the set of time scales to use
     * @return the time scale corresponding to time system in the set of time scales
     */
    TimeScale getTimeScale(final TimeScales timeScales);

}

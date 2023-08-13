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
package org.orekit.propagation.analytical.gnss.data;

import org.orekit.time.TimeStamped;

/**This interface provides the minimal set of orbital elements needed by the
 * {@link org.orekit.propagation.analytical.gnss.ClockCorrectionsProvider}.
*
* @author Pascal Parraud
* @since 11.0
*/
public interface GNSSClockElements extends TimeStamped {

    /**
     * Gets the Zeroth Order Clock Correction.
     *
     * @return the Zeroth Order Clock Correction (s)
     * @see #getAf1()
     * @see #getAf2()
     * @see #getToc()
     */
    double getAf0();

    /**
     * Gets the First Order Clock Correction.
     *
     * @return the First Order Clock Correction (s/s)
     * @see #getAf0()
     * @see #getAf2()
     * @see #getToc()
     */
    double getAf1();

    /**
     * Gets the Second Order Clock Correction.
     *
     * @return the Second Order Clock Correction (s/s²)
     * @see #getAf0()
     * @see #getAf1()
     * @see #getToc()
     */
    double getAf2();

    /**
     * Gets the estimated group delay differential TGD for L1-L2 correction.
     *
     * @return the estimated group delay differential TGD for L1-L2 correction (s)
     */
    double getTGD();

    /**
     * Gets the duration of the GNSS cycle in seconds.
     *
     * @return the duration of the GNSS cycle in seconds
     */
    double getCycleDuration();

    /**
     * Gets the clock correction reference time toc.
     *
     * @return the clock correction reference time (s)
     * @see #getAf0()
     * @see #getAf1()
     * @see #getAf2()
     */
    default double getToc() {
        return 0.0;
    }

}

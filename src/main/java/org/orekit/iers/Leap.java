/* Copyright 2002-2008 CS Communication & Systèmes
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
package org.orekit.iers;

/** UTC Time steps.
 * <p>This class is a simple container.</p>
 * @author Luc Maisonobe
 * @see org.orekit.time.UTCScale
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class Leap {

    /** Time in UTC at which the step occurs. */
    private final double utcTime;

    /** Step value. */
    private final double step;

    /** Offset in seconds after the leap. */
    private final double offsetAfter;

    /** Simple constructor.
     * @param utcTime time in UTC at which the step occurs
     * @param step step value
     * @param offsetAfter offset in seconds after the leap
     */
    public Leap(final double utcTime, final double step,
                final double offsetAfter) {
        this.utcTime     = utcTime;
        this.step        = step;
        this.offsetAfter = offsetAfter;
    }

    /** Get the time in UTC at which the step occurs.
     * @return time in UTC at which the step occurs.
     */
    public double getUtcTime() {
        return utcTime;
    }

    /** Get the step value.
     * @return step value.
     */
    public double getStep() {
        return step;
    }

    /** Get the offset in seconds after the leap.
     * @return offset in seconds after the leap.
     */
    public double getOffsetAfter() {
        return offsetAfter;
    }

}

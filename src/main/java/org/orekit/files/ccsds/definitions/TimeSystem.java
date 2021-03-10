/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.definitions;

import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScale;

/** Dates reader/writer based on a {@link TimeScale}.
 *
 * @author Luc Maisonobe
 * @since 11.0
 */
public class TimeSystem {

    /** Base time scale. */
    private final TimeScale timeScale;

    /** Build a time sytem from a time scale.
     * @param timeScale base time scale
     */
    public TimeSystem(final TimeScale timeScale) {
        this.timeScale = timeScale;
    }

    /** Parse a date.
     * @param s string to parse
     * @return parsed date
     */
    public AbsoluteDate parse(final String s) {
        return new AbsoluteDate(s, timeScale);
    }

    /** Generate calendar components.
     * @param date date to convert
     * @return date components
     */
    public DateTimeComponents toComponents(final AbsoluteDate date) {
        return date.getComponents(timeScale);
    }

    /** Get the base time scale.
     * @return base time scale
     */
    public TimeScale getTimeScale() {
        return timeScale;
    }

}

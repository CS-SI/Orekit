/* Copyright 2022-2025 Luc Maisonobe
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
package org.orekit.files.sinex;

import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScales;

/**
 * Base container for Solution INdependent EXchange (SINEX) files.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 13.0
 */
public class AbstractSinex {

    /** Time scales. */
    private final TimeScales timeScales;

    /** SINEX file creation date as extracted for the first line. */
    private final AbsoluteDate creationDate;

    /** Start time of the data used in the Sinex solution.*/
    private final AbsoluteDate startDate;

    /** End time of the data used in the Sinex solution.*/
    private final AbsoluteDate endDate;

    /** Simple constructor.
     * @param timeScales time scales
     * @param creationDate SINEX file creation date
     * @param startDate start time of the data used in the Sinex solution
     * @param endDate end time of the data used in the Sinex solution
     */
    public AbstractSinex(final TimeScales timeScales, final AbsoluteDate creationDate,
                         final AbsoluteDate startDate, final AbsoluteDate endDate) {
        this.timeScales   = timeScales;
        this.creationDate = creationDate;
        this.startDate    = startDate;
        this.endDate      = endDate;
    }

    /** Get the time scales.
     * @return time scales
     */
    public TimeScales getTimeScales() {
        return timeScales;
    }

    /** Get the creation date of the parsed SINEX file.
     * @return SINEX file creation date as an AbsoluteDate
     */
    public AbsoluteDate getCreationDate() {
        return creationDate;
    }

    /** Get the file epoch start time.
     * @return the file epoch start time
     */
    public AbsoluteDate getFileEpochStartTime() {
        return startDate;
    }

    /** Get the file epoch end time.
     * @return the file epoch end time
     */
    public AbsoluteDate getFileEpochEndTime() {
        return endDate;
    }

}

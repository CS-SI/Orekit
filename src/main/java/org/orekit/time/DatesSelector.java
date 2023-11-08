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

import java.util.List;

/** Interface for selecting dates within an interval.
 * <p>
 * This interface is mainly useful for {@link
 * org.orekit.estimation.measurements.generation.AbstractScheduler scheduling}
 * measurements {@link org.orekit.estimation.measurements.generation.Generator
 * generation}.
 * </p>
 * @see org.orekit.estimation.measurements.generation.AbstractScheduler AbstractScheduler
 * @see org.orekit.estimation.measurements.generation.Generator Generator
 * @author Luc Maisonobe
 * @since 9.3
 */
public interface DatesSelector {

    /** Select dates within an interval.
     * <p>
     * The {@code start} and {@code end} date may be either in direct or reverse
     * chronological order. The list is produced in the same order as {@code start}
     * and {@code end}, i.e. direct chronological order if {@code start} is earlier
     * than {@code end} or reverse chronological order if {@code start} is later
     * than {@code end}.
     * </p>
     * <p>
     * The ordering (direct or reverse chronological order) should not be changed
     * between calls, otherwise unpredictable results may occur.
     * </p>
     * @param start interval start
     * @param end interval end
     * @return selected dates within this interval
     */
    List<AbsoluteDate> selectDates(AbsoluteDate start, AbsoluteDate end);

}

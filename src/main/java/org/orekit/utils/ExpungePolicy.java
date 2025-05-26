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
package org.orekit.utils;

import org.orekit.time.AbsoluteDate;

/** Expunge policy to apply when a {@link org.orekit.utils.TimeSpanMap} exceeds its capacity.
 * @author Luc Maisonobe
 * @since 13.1
 */
public enum ExpungePolicy {

    /** Expunge the span before the first transition.
     * <p>
     * Note that if we add data to the map in reverse chronological order, then entries
     * exceeding capacity are expunged as soon as we attempt to add them, so this
     * policy should probably not be used in reverse chronological order.
     * </p>
     */
    EXPUNGE_EARLIEST {
        boolean expungeEarliest(final AbsoluteDate date,
                                final AbsoluteDate earliest, final AbsoluteDate latest) {
            return true;
        }
    },

    /** Expunge the span after the latest transition.
     * <p>
     * Note that if we add data to the map in chronological order, then entries
     * exceeding capacity are expunged as soon as we attempt to add them, so this
     * policy should probably not be used in chronological order.
     * </p>
     */
    EXPUNGE_LATEST {
        boolean expungeEarliest(final AbsoluteDate date,
                                final AbsoluteDate earliest, final AbsoluteDate latest) {
            return false;
        }
    },

    /** Expunge either the earliest or latest span, depending on which is farthest from the last added transition. */
    EXPUNGE_FARTHEST {
        boolean expungeEarliest(final AbsoluteDate date,
                                final AbsoluteDate earliest, final AbsoluteDate latest) {
            return date.durationFrom(earliest) >= latest.durationFrom(date);
        }
    };

    /** Check if data to be expunged is the earliest data.
     * @param date current date
     * @param earliest earliest date
     * @param latest latest date
     * @return true if data to be expunged is the earliest data
     */
    abstract boolean expungeEarliest(AbsoluteDate date, AbsoluteDate earliest, AbsoluteDate latest);

}

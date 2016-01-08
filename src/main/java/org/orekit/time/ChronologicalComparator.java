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

import java.io.Serializable;
import java.util.Comparator;

/** Comparator for {@link TimeStamped} instance.
 * @see AbsoluteDate
 * @see TimeStamped
 * @author Luc Maisonobe
 */
public class ChronologicalComparator implements Comparator<TimeStamped>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 3092980292741000025L;

    /** Simple constructor.
     */
    public ChronologicalComparator() {
        // nothing to do
    }

    /** Compare two time-stamped instances.
     * @param timeStamped1 first time-stamped instance
     * @param timeStamped2 second time-stamped instance
     * @return a negative integer, zero, or a positive integer as the first
     * instance is before, simultaneous, or after the second one.
     */
    public int compare(final TimeStamped timeStamped1,
                       final TimeStamped timeStamped2) {
        return timeStamped1.getDate().compareTo(timeStamped2.getDate());
    }

}

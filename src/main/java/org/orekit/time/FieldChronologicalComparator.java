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

import org.hipparchus.CalculusFieldElement;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Comparator for {@link FieldTimeStamped} instance.
 *
 * @author Luc Maisonobe
 * @author Vincent Cucchietti
 * @see FieldAbsoluteDate
 * @see FieldTimeStamped
 * @param <KK> type of the field elements
 */
public class FieldChronologicalComparator<KK extends CalculusFieldElement<KK>>
        implements Comparator<FieldTimeStamped<KK>>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -5373507372120707293L;

    /** Simple constructor. */
    public FieldChronologicalComparator() {
        // nothing to do
    }

    /**
     * Compare two time-stamped instances.
     *
     * @param timeStamped1 first time-stamped instance
     * @param timeStamped2 second time-stamped instance
     *
     * @return a negative integer, zero, or a positive integer as the first instance is before, simultaneous, or after the
     * second one.
     */
    public int compare(final FieldTimeStamped<KK> timeStamped1,
                       final FieldTimeStamped<KK> timeStamped2) {
        return timeStamped1.getDate().compareTo(timeStamped2.getDate());
    }
}

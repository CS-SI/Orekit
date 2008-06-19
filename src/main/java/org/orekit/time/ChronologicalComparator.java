/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
 * <p>This comparator is implemented as a singleton.</p>
 * @see AbsoluteDate
 * @see TimeStamped
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class ChronologicalComparator implements Comparator<TimeStamped>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -602997163791160921L;

    /** Private constructor for singleton.
     */
    private ChronologicalComparator() {
        // nothing to do
    }

    /** Get the unique instance.
     * @return the unique instance
     */
    public static ChronologicalComparator getInstance() {
        return LazyChronologicalComparatorHolder.INSTANCE;
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

    // We use the Initialization on demand holder idiom to store
    // the singleton, as it is both thread-safe, efficient (no
    // synchronization) and works with all version of java.

    /** Holder for the ChronologicalComparator frame singleton. */
    private static class LazyChronologicalComparatorHolder {
        /** Unique instance. */
        private static final ChronologicalComparator INSTANCE =
            new ChronologicalComparator();
    }

}

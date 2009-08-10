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
        return LazyHolder.INSTANCE;
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

    // The following marker comment is used to prevent checkstyle from complaining
    // about utility classes missing an hidden (private) constructor
    // These classes should have such constructors, that are obviously never called.
    // Unfortunately, since cobertura currently cannot mark untestable code, these
    // constructors on such small classes lead to artificially low code coverage.
    // So to make sure both checkstyle and cobertura are happy, we locally inhibit
    // checkstyle verification for the special case of small classes implementing
    // the initialization on demand holder idiom used for singletons. This choice is
    // safe as the classes are themselves private and completely under control. In fact,
    // even if someone could instantiate them, this would be harmless since they only
    // have static fields and no methods at all.
    // CHECKSTYLE: stop HideUtilityClassConstructor

    /** Holder for the singleton.
     * <p>
     * We use the Initialization on demand holder idiom to store
     * the singletons, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.
     * </p>
     */
    private static class LazyHolder {

        /** Unique instance. */
        private static final ChronologicalComparator INSTANCE =
            new ChronologicalComparator();

    }

    // CHECKSTYLE: resume HideUtilityClassConstructor

}

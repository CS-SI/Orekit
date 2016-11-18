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

import org.hipparchus.RealFieldElement;

/** This interface represents objects that have a {@link AbsoluteDate}
 * date attached to them.
 * <p>Classes implementing this interface can be stored chronologically
 * in sorted sets using {@link ChronologicalComparator} as the
 * underlying comparator. An example using for {@link org.orekit.orbits.Orbit
 * Orbit} instances is given here:</p>
 * <pre>
 *     SortedSet&lt;Orbit> sortedOrbits =
 *         new TreeSet&lt;Orbit>(new ChronologicalComparator());
 *     sortedOrbits.add(orbit1);
 *     sortedOrbits.add(orbit2);
 *     ...
 * </pre>
 * <p>This interface is also the base interface used to {@link
 * org.orekit.utils.TimeStampedCache cache} series of time-dependent
 * objects for interpolation in a thread-safe manner.</p>
 * @see AbsoluteDate
 * @see ChronologicalComparator
 * @see org.orekit.utils.TimeStampedCache
 * @author Luc Maisonobe
 */
public interface FieldTimeStamped<T extends RealFieldElement<T>> {

    /** Get the date.
     * @return date attached to the object
     */
    FieldAbsoluteDate<T> getDate();

}

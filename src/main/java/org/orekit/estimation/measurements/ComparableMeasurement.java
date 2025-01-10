/* Copyright 2002-2025 CS GROUP
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
package org.orekit.estimation.measurements;

import org.orekit.time.TimeStamped;


/** Base interface for comparing measurements regardless of their type.
 * @author Luc Maisonobe
 * @author Evan M. Ward
 * @since 9.2
 */
public interface ComparableMeasurement extends TimeStamped, Comparable<ComparableMeasurement> {

    /** Get the observed value.
     * <p>
     * The observed value is the value that was measured by the instrument.
     * </p>
     * @return observed value
     */
    double[] getObservedValue();

    /** Set the observed value.
     * <p>
     * The observed value is the value that was measured by the instrument.
     * </p>
     * @param newObserved observed value
     * @since 13.0
     */
    void setObservedValue(double[] newObserved);

    /**
     * {@inheritDoc}
     *
     * <p>Measurements comparison is primarily chronological, but measurements with
     * the same date are sorted based on the observed value. Even if they have
     * the same value too, they will <em>likely</em> not be considered equal if
     * they correspond to different instances.
     *
     * <p>Care should be taken before storing measurements in a
     * {@link java.util.SortedSet SortedSet} as it may lose redundant
     * measurements if they, by chance, have the same identity hash code.
     *
     * @see System#identityHashCode(Object)
     */
    @Override
    default int compareTo(final ComparableMeasurement other) {

        if (this == other) {
            // quick return for comparing a measurement to itself
            return 0;
        }

        // Compare date first
        int result = getDate().compareTo(other.getDate());
        if (result != 0) {
            return result;
        }

        // Simultaneous measurements, we compare the size of the measurements
        final double[] thisV  = getObservedValue();
        final double[] otherV = other.getObservedValue();
        // "Bigger" measurements after "smaller" measurement
        if (thisV.length > otherV.length) {
            return +1;
        } else if (thisV.length < otherV.length) {
            return -1;
        }

        // Measurements have same size
        // Compare the first different value
        // "Bigger" measurements after "smaller" measurement
        for (int i = 0; i < thisV.length; ++i) {
            result = Double.compare(thisV[i], otherV[i]);
            if (result != 0) {
                return result;
            }
        }

        // Measurements have the same value,
        // but we do not want them to appear as equal
        // we set up an arbitrary order based on hash code
        result = Integer.compare(this.hashCode(), other.hashCode());
        if (result != 0) {
            return result;
        }
        // next try identity hash code
        result = Integer.compare(
                System.identityHashCode(this),
                System.identityHashCode(other));
        // Tried all the fields to compare, and though we want an arbitrary
        // total order, we still must obey the contract of compareTo, see #1364.
        // So this may return zero if this==other, or for equal objects that by
        // chance have the same identity hash code.
        return result;

    }

}

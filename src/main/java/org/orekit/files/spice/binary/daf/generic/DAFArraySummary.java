/* Contributed in the public domain.
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
package org.orekit.files.spice.binary.daf.generic;

import java.util.List;

/**
 * Represents the summary of a single {@link DAFArray} in a {@link DAF} file.
 *
 * @author Rafael Ayala
 * @since 14.0
 */
public class DAFArraySummary {

    /**
     * A list with the doubles in the array summary.
     */
    private final List<Double> summaryDoubles;

    /**
     * A list with the integers in the array summary.
     */
    private final List<Integer> summaryInts;

    /**
     * The initial byte address of the array elements.
     */
    private final int initialArrayAddress;

    /**
     * The final byte address of the array elements.
     */
    private final int finalArrayAddress;

    /**
     * Simple constructor.
     *
     * @param summaryDoubles list of doubles in the summary
     * @param summaryInts list of integers in the summary
     * @param initialArrayAddress initial byte address of array elements
     * @param finalArrayAddress final byte address of array elements
     */
    public DAFArraySummary(final List<Double> summaryDoubles, final List<Integer> summaryInts,
            final int initialArrayAddress, final int finalArrayAddress) {
        this.summaryDoubles = summaryDoubles;
        this.summaryInts = summaryInts;
        this.initialArrayAddress = initialArrayAddress;
        this.finalArrayAddress = finalArrayAddress;
    }

    /**
     * Get the summary doubles.
     *
     * @return list of summary doubles
     */
    public List<Double> getSummaryDoubles() {
        return summaryDoubles;
    }

    /**
     * Get the summary integers.
     *
     * @return list of summary integers
     */
    public List<Integer> getSummaryInts() {
        return summaryInts;
    }

    /**
     * Get the initial byte address of the array elements.
     *
     * @return initial byte address of the array elements
     */
    public int getInitialArrayAddress() {
        return initialArrayAddress;
    }

    /**
     * Get the final byte address of the array elements.
     *
     * @return final byte address of the array elements.
     */
    public int getFinalArrayAddress() {
        return finalArrayAddress;
    }
}

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

import java.util.Collections;
import java.util.List;

/**
 * Represents a single array in a {@link DAF} file, including its name, summary,
 * and elements.
 *
 * @author Rafael Ayala
 * @since XX.XX
 */
public class DAFArray {

    /**
     * Name of the array.
     */
    private final String arrayName;

    /**
     * Summary of the array.
     */
    private final DAFArraySummary arraySummary;

    /**
     * Elements (doubles) of the array.
     */
    private final List<Double> arrayElements;

    /**
     * Simple constructor.
     *
     * @param arrayName name of the array
     * @param arraySummary summary of the array
     * @param arrayElements list of array elements
     */
    public DAFArray(final String arrayName, final DAFArraySummary arraySummary, final List<Double> arrayElements) {
        this.arrayName = arrayName;
        this.arraySummary = arraySummary;
        this.arrayElements = arrayElements != null ? arrayElements : Collections.emptyList();
    }

    /**
     * Get the array name.
     *
     * @return array name
     */
    public String getArrayName() {
        return arrayName;
    }

    /**
     * Get the array summary.
     *
     * @return array summary
     */
    public DAFArraySummary getArraySummary() {
        return arraySummary;
    }

    /**
     * Get the array elements.
     *
     * @return list of array elements
     */
    public List<Double> getArrayElements() {
        return Collections.unmodifiableList(arrayElements);
    }
}

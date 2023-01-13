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
package org.orekit.gnss.metric.messages.common;

/** Signal-In-Space Accuracy (SISA).
 * @see "Galileo OS Signal-In-Space Interface Control Document, Issue 1.3, December 2016, Table 76"
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class SignalInSpaceAccuracy implements AccuracyProvider {

    /** Signal In Space Accuracy indicator. */
    private final int sisaIndex;

    /**
     * Simple constructor.
     * @param index integer value of the signal in space accuracy
     */
    public SignalInSpaceAccuracy(final int index) {
        this.sisaIndex = index;
    }

    /** {@inheritDoc} */
    @Override
    public double getAccuracy() {
        // Cast index to a double value
        final double id = (double) sisaIndex;
        // Compute accuracy
        if (sisaIndex < 50) {
            // Accuracy is between 0 cm and 49 cm with 1 cm resolution
            return 0.01 * id;
        } else if (sisaIndex < 75) {
            // Accuracy is between 50 cm and 98 cm with 2 cm resolution
            return 0.01 * (50.0 + 2.0 * (id - 50.0));
        } else if (sisaIndex < 100) {
            // Accuracy is between 1 m and 2 m with 4 cm resolution
            return 1.0 + 0.04 * (id - 75.0);
        } else if (sisaIndex < 126) {
            // Accuracy is between 2 m and 6 m with 16 cm resolution
            return 2.0 + 0.16 * (id - 100.0);
        } else {
            // Spare or No Accuracy Predicition Available (NAPA)
            return -1.0;
        }
    }

}

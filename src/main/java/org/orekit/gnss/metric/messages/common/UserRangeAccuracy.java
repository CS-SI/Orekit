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

import org.hipparchus.util.FastMath;

/** User Range Accuracy.
 * @see "IS-GPS-200K, 4 March 2016, Section 20.3.3.3.1.3"
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class UserRangeAccuracy implements AccuracyProvider {

    /** User Range Accuracy indicator. */
    private final int uraIndex;

    /**
     * Simple constructor.
     * @param index integer value of the user range accuracy
     */
    public UserRangeAccuracy(final int index) {
        this.uraIndex = index;
    }

    /** {@inheritDoc} */
    @Override
    public double getAccuracy() {
        // Cast index to a double value
        final double id = (double) uraIndex;
        // Compute accuracy
        if (uraIndex < 7) {
            if (uraIndex == 1) {
                return 2.8;
            } else if (uraIndex == 3) {
                return 5.7;
            } else if (uraIndex == 5) {
                return 11.3;
            } else {
                return FastMath.pow(2.0, 0.5 * id + 1.0);
            }
        } else if (uraIndex < 15) {
            return FastMath.pow(2.0, id - 2.0);
        } else {
            // Data shall not be used
            return 8192.0;
        }
    }

}

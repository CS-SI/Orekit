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

/** Enumerate for GLONASS User Range Accuracy.
 * @see "ICD L1, L2 GLONASS, Edition 5.1, Table 4.4, 2008"
 * @author Bryan Cazabonne
 */
public class GlonassUserRangeAccuracy implements AccuracyProvider {

    /** User Range Accuracy indicator (word F<sub>T</sub>). */
    private final int glonassUraIndex;

    /**
     * Simple constructor.
     * @param index integer value of the Glonass user range accuracy
     */
    public GlonassUserRangeAccuracy(final int index) {
        this.glonassUraIndex = index;
    }

    /** {@inheritDoc} */
    @Override
    public double getAccuracy() {
        switch (glonassUraIndex) {
            case 0  : return 1.0;
            case 1  : return 2.0;
            case 2  : return 2.5;
            case 3  : return 4.0;
            case 4  : return 5.0;
            case 5  : return 7.0;
            case 6  : return 10.0;
            case 7  : return 12.0;
            case 8  : return 14.0;
            case 9  : return 16.0;
            case 10 : return 32.0;
            case 11 : return 64.0;
            case 12 : return 128.0;
            case 13 : return 256.0;
            case 14 : return 512.0;
            default : return 1024.0; // Data shall not be used
        }
    }

}

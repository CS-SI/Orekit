/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.propagation.analytical.gnss.data;

import org.orekit.gnss.PredefinedGnssSignal;
import org.orekit.gnss.RadioWave;

/** Enumerate for {@link BeidouCivilianNavigationMessage}.
 * @author Luc Maisonobe
 * @since 14.0
 */
public enum BeidouCivilianType {

    /** CNV1 message. */
    CNV1(PredefinedGnssSignal.B1C),

    /** CNV2 message. */
    CNV2(PredefinedGnssSignal.B2A),

    /** CNV3 message. */
    CNV3(PredefinedGnssSignal.B2B);

    /** Radio wave on which navigation signal is sent. */
    private final RadioWave radioWave;

    /** Simple constructor.
     * @param radioWave radio wave on which navigation signal is sent
     */
    BeidouCivilianType(final RadioWave radioWave) {
        this.radioWave = radioWave;
    }

    /** Get radiowave for message.
     * @return radio wave on which navigation signal is sent
     */
    public RadioWave getRadioWave() {
        return radioWave;
    }

}

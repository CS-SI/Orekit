/* Copyright 2023 Luc Maisonobe
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
package org.orekit.files.rinex.observation;

import org.orekit.gnss.SatInSystem;

/** Container for association between GLONASS satellites and frequency channels (f = f₀ + k Δf with k ranging-7 to +6).
 * @author Luc Maisonobe
 * @since 12.0
 */
public class GlonassSatelliteChannel {

    /** Satellite. */
    private final SatInSystem satellite;

    /** Channel frequency multiplier. */
    private final int k;

    /** Simple constructor.
     * @param satellite satellite identifier
     * @param k channel frequency multiplier (should be between -7 and +6)
     */
    public GlonassSatelliteChannel(final SatInSystem satellite, final int k) {
        this.satellite = satellite;
        this.k         = k;
    }

    /** Get the satellite identifier.
     * @return satellite identifier
     */
    public SatInSystem getSatellite() {
        return satellite;
    }

    /** Get the channel frequency multiplier.
     * @return channel frequency multiplier
     */
    public int getK() {
        return k;
    }

}

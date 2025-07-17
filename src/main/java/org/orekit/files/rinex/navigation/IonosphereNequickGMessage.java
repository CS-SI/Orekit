/* Copyright 2022-2025 Luc Maisonobe
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
package org.orekit.files.rinex.navigation;

import org.orekit.gnss.SatelliteSystem;

/** Container for data contained in a ionosphere Nequick G message.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class IonosphereNequickGMessage extends IonosphereBaseMessage {

    /** Aij. */
    private final IonosphereAij aij;

    /** Disturbance flags. */
    private int flags;

    /** Simple constructor.
     * @param system satellite system
     * @param prn satellite number
     * @param navigationMessageType navigation message type
     * @param subType message subtype
     */
    public IonosphereNequickGMessage(final SatelliteSystem system, final int prn,
                                     final String navigationMessageType, final String subType) {
        super(system, prn, navigationMessageType, subType);
        this.aij = new IonosphereAij();
    }

    /** Get aᵢⱼ coefficients.
     */
    public IonosphereAij getAij() {
        return aij;
    }

    /** Get the disturbance flags.
     * @return disturbance flags
     */
    public int getFlags() {
        return flags;
    }

    /** Set the disturbance flags.
     * @param flags disturbance flags
     */
    public void setFlags(final int flags) {
        this.flags = flags;
    }

}

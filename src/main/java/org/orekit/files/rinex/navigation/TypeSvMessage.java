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
package org.orekit.files.rinex.navigation;

import org.orekit.gnss.SatelliteSystem;

/** Container for data shared by several navigation messages.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class TypeSvMessage {

    /** Satellite system. */
    private final SatelliteSystem system;

    /** Satellite number. */
    private final int prn;

    /** Navigation message type. */
    private final String navigationMessageType;

    /** Simple constructor.
     * @param system satellite system
     * @param prn satellite number
     * @param navigationMessageType navigation message type
     */
    protected TypeSvMessage(final SatelliteSystem system, final int prn, final String navigationMessageType) {
        this.system                = system;
        this.prn                   = prn;
        this.navigationMessageType = navigationMessageType;
    }

    /** Get satellite system.
     * @return the system
     */
    public SatelliteSystem getSystem() {
        return system;
    }

    /** Get satellite number.
     * @return the prn
     */
    public int getPrn() {
        return prn;
    }

    /** Get navigation message type.
     * @return the navigation message type
     */
    public String getNavigationMessageType() {
        return navigationMessageType;
    }

}

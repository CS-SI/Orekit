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
package org.orekit.files.rinex.navigation;

import org.orekit.gnss.SatelliteSystem;

/** Container for data contained in a GLONASS ionosphere message.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class IonosphereGlonassCdmsMessage
    extends IonosphereBaseMessage {

    /** c_A. */
    private double cA;

    /** c_F10.7. */
    private double cF107;

    /** c_Ap. */
    private double cAp;

    /** Simple constructor.
     * @param system satellite system
     * @param prn satellite number
     * @param navigationMessageType navigation message type
     * @param subType message subtype
     */
    public IonosphereGlonassCdmsMessage(final SatelliteSystem system, final int prn,
                                        final String navigationMessageType, final String subType) {
        super(system, prn, navigationMessageType, subType);
    }

    /** Get the c_A coefficient.
     * @return c_A
     */
    public double getCA() {
        return cA;
    }

    /** Set the c_A coefficient.
     * @param newCa c_A
     */
    public void setCA(final double newCa) {
        this.cA =  newCa;
    }

    /** Get the c_F10.7 coefficient.
     * @return c_F10.7
     */
    public double getCF107() {
        return cF107;
    }

    /** Set the c_F10.7 coefficient.
     * @param newCf107 c_F10.7
     */
    public void setCF107(final double newCf107) {
        this.cF107 =  newCf107;
    }

    /** Get the c_Ap coefficient.
     * @return c_Ap
     */
    public double getCAP() {
        return cAp;
    }

    /** Set the c_AP coefficient.
     * @param newCAP c_Ap
     */
    public void setCAP(final double newCAP) {
        this.cAp =  newCAP;
    }

}

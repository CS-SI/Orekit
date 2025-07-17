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

/** Container for data contained in a ionosphere NavIC Klobuchar message.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class IonosphereNavICNeQuickNMessage
    extends IonosphereBaseMessage {

    /** Issue Of Data. */
    private int iod;

    /** Region 1. */
    private final RegionalAij region1;

    /** Region 2. */
    private final RegionalAij region2;

    /** Region 3. */
    private final RegionalAij region3;

    /** Simple constructor.
     * @param system satellite system
     * @param prn satellite number
     * @param navigationMessageType navigation message type
     * @param subType message subtype
     */
    public IonosphereNavICNeQuickNMessage(final SatelliteSystem system, final int prn,
                                          final String navigationMessageType, final String subType) {
        super(system, prn, navigationMessageType, subType);
        region1 =  new RegionalAij();
        region2 =  new RegionalAij();
        region3 =  new RegionalAij();
    }

    /** Get Issue Of Data (IOD).
     * @return  Issue Of Data
     */
    public int getIOD() {
        return iod;
    }

    /** Set Issue Of Data.
     * @param iod Issue Of Data
     */
    public void setIOD(final double iod) {
        // The value is given as a floating number in the navigation message
        this.iod = (int) iod;
    }

    /** Get the regional aᵢⱼ for region 1.
    * @return regional aᵢⱼ for region 1
     */
    public RegionalAij getRegion1() {
        return region1;
    }

    /** Get the regional aᵢⱼ for region 2.
    * @return regional aᵢⱼ for region 2
     */
    public RegionalAij getRegion2() {
        return region2;
    }

    /** Get the regional aᵢⱼ for region 1.
    * @return regional aᵢⱼ for region 1
     */
    public RegionalAij getRegion3() {
        return region3;
    }

}

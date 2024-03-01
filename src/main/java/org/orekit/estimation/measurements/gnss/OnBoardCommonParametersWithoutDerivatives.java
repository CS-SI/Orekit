/* Copyright 2002-2024 Luc Maisonobe
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
package org.orekit.estimation.measurements.gnss;

import org.orekit.estimation.measurements.CommonParametersWithoutDerivatives;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Common intermediate parameters used to estimate measurements where receiver is a satellite.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class OnBoardCommonParametersWithoutDerivatives
    extends CommonParametersWithoutDerivatives {

    /** Local clock offset. */
    private final double localOffset;

    /** Local clock rate. */
    private final double localRate;

    /** Remote clock offset. */
    private final double remoteOffset;

    /** Remote clock rate. */
    private final double remoteRate;

    /** Remote satellite position/velocity. */
    private final TimeStampedPVCoordinates remotePV;

    /** Simple constructor.
     * @param localState local spacecraft state
     * @param localOffset local clock offset
     * @param localRate local clock rate
     * @param remoteOffset remote clock offset
     * @param remoteRate remote clock rate
     * @param tauD downlink delay
     * @param localPV local satellite position/velocity
     * @param remotePV remote satellite position/velocity
     */
    public OnBoardCommonParametersWithoutDerivatives(final SpacecraftState localState,
                                                     final double localOffset, final double localRate,
                                                     final double remoteOffset, final double remoteRate,
                                                     final double tauD,
                                                     final TimeStampedPVCoordinates localPV,
                                                     final TimeStampedPVCoordinates remotePV) {
        super(localState, tauD, localState, localPV);
        this.localOffset  = localOffset;
        this.localRate    = localRate;
        this.remoteOffset = remoteOffset;
        this.remoteRate   = remoteRate;
        this.remotePV     = remotePV;
    }

    /** Get local clock offset.
     * @return local clock offset
     */
    public double getLocalOffset() {
        return localOffset;
    }

    /** Get local clock rate.
     * @return local clock rate
     */
    public double getLocalRate() {
        return localRate;
    }

    /** Get remote clock offset.
     * @return remote clock offset
     */
    public double getRemoteOffset() {
        return remoteOffset;
    }

    /** Get remote clock rate.
     * @return remote clock rate
     */
    public double getRemoteRate() {
        return remoteRate;
    }

    /** Get remote satellite position/velocity.
     * @return remote satellite position/velocity
     */
    public TimeStampedPVCoordinates getRemotePV() {
        return remotePV;
    }

}

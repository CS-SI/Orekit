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
package org.orekit.estimation.measurements.gnss;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.estimation.measurements.CommonParametersWithDerivatives;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

import java.util.Map;

/** Common intermediate parameters used to estimate measurements where receiver is a satellite.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class OnBoardCommonParametersWithDerivatives
    extends CommonParametersWithDerivatives {

    /** Local clock offset. */
    private final Gradient localOffset;

    /** Local clock rate. */
    private final Gradient localRate;

    /** Remote clock offset. */
    private final Gradient remoteOffset;

    /** Remote clock rate. */
    private final Gradient remoteRate;

    /** Remote satellite position/velocity. */
    private final TimeStampedFieldPVCoordinates<Gradient> remotePV;

    /** Simple constructor.
     * @param localState local spacecraft state
     * @param indices derivatives indices map
     * @param localOffset local clock offset
     * @param localRate local clock rate
     * @param remoteOffset remote clock offset
     * @param remoteRate remote clock rate
     * @param tauD downlink delay
     * @param localPV local satellite position/velocity
     * @param remotePV remote satellite position/velocity
     */
    public OnBoardCommonParametersWithDerivatives(final SpacecraftState localState,
                                                  final Map<String, Integer> indices,
                                                  final Gradient localOffset, final Gradient localRate,
                                                  final Gradient remoteOffset, final Gradient remoteRate,
                                                  final Gradient tauD,
                                                  final TimeStampedFieldPVCoordinates<Gradient> localPV,
                                                  final TimeStampedFieldPVCoordinates<Gradient> remotePV) {
        super(localState, indices, tauD, localState, localPV);
        this.localOffset  = localOffset;
        this.localRate    = localRate;
        this.remoteOffset = remoteOffset;
        this.remoteRate   = remoteRate;
        this.remotePV     = remotePV;
    }

    /** Get local clock offset.
     * @return local clock offset
     */
    public Gradient getLocalOffset() {
        return localOffset;
    }

    /** Get local clock rate.
     * @return local clock rate
     */
    public Gradient getLocalRate() {
        return localRate;
    }

    /** Get remote clock offset.
     * @return remote clock offset
     */
    public Gradient getRemoteOffset() {
        return remoteOffset;
    }

    /** Get remote clock rate.
     * @return remote clock rate
     */
    public Gradient getRemoteRate() {
        return remoteRate;
    }

    /** Get remote satellite position/velocity.
     * @return remote satellite position/velocity
     */
    public TimeStampedFieldPVCoordinates<Gradient> getRemotePV() {
        return remotePV;
    }

}

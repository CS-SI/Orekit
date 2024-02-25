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
    final Gradient dtLocal;

    /** Remote clock offset. */
    final Gradient dtRemote;

    /** Remote satellite position/velocity. */
    private final TimeStampedFieldPVCoordinates<Gradient> remotePV;

    /** Simple constructor.
     * @param localState local spacecraft state
     * @param indices derivatives indices map
     * @param dtLocal local clock offset
     * @param dtRemote remote clock offset
     * @param tauD downlink delay
     * @param localPV local satellite position/velocity
     * @param remotePV remote satellite position/velocity
     */
    public OnBoardCommonParametersWithDerivatives(final SpacecraftState localState,
                                                  final Map<String, Integer> indices,
                                                  final Gradient dtLocal, final Gradient dtRemote,
                                                  final Gradient tauD,
                                                  final TimeStampedFieldPVCoordinates<Gradient> localPV,
                                                  final TimeStampedFieldPVCoordinates<Gradient> remotePV) {
        super(localState, indices, tauD, localState, localPV);
        this.dtLocal  = dtLocal;
        this.dtRemote = dtRemote;
        this.remotePV = remotePV;
    }

    /** Get local clock offset.
     * @return local clock offset
     */
    public Gradient getDtLocal() {
        return dtLocal;
    }

    /** Get remote clock offset.
     * @return remotr clock offset
     */
    public Gradient getDtRemote() {
        return dtRemote;
    }

    /** Get remote satellite position/velocity.
     * @return remote satellite position/velocity
     */
    public TimeStampedFieldPVCoordinates<Gradient> getRemotePV() {
        return remotePV;
    }

}

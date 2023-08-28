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
package org.orekit.files.rinex.observation;

import java.util.List;

import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatInSystem;
import org.orekit.gnss.SatelliteSystem;

/** Phase Shift corrections.
 * Contains the phase shift corrections used to
 * generate phases consistent with respect to cycle shifts.
 * @since 12.0
 */
public class PhaseShiftCorrection {

    /** Satellite System. */
    private final SatelliteSystem satSystemPhaseShift;

    /** Carrier Phase Observation Code (may be null). */
    private final ObservationType typeObsPhaseShift;

    /** Phase Shift Corrections (cycles). */
    private final double phaseShiftCorrection;

    /** List of satellites involved. */
    private final List<SatInSystem> satsPhaseShift;

    /** Simple constructor.
     * @param satSystemPhaseShift Satellite System
     * @param typeObsPhaseShift Carrier Phase Observation Code (may be null)
     * @param phaseShiftCorrection Phase Shift Corrections (cycles)
     * @param satsPhaseShift List of satellites involved
     */
    public PhaseShiftCorrection(final SatelliteSystem satSystemPhaseShift,
                                final ObservationType typeObsPhaseShift,
                                final double phaseShiftCorrection,
                                final List<SatInSystem> satsPhaseShift) {
        this.satSystemPhaseShift = satSystemPhaseShift;
        this.typeObsPhaseShift = typeObsPhaseShift;
        this.phaseShiftCorrection = phaseShiftCorrection;
        this.satsPhaseShift = satsPhaseShift;
    }

    /** Get the Satellite System.
     * @return Satellite System.
     */
    public SatelliteSystem getSatelliteSystem() {
        return satSystemPhaseShift;
    }

    /** Get the Carrier Phase Observation Code.
     * <p>
     * The observation code may be null for the uncorrected reference
     * signal group
     * </p>
     * @return Carrier Phase Observation Code.
     */
    public ObservationType getTypeObs() {
        return typeObsPhaseShift;
    }

    /** Get the Phase Shift Corrections.
     * @return Phase Shift Corrections (cycles)
     */
    public double getCorrection() {
        return phaseShiftCorrection;
    }

    /** Get the list of satellites involved.
     * @return List of satellites involved (if empty, all the sats are involved)
     */
    public List<SatInSystem> getSatsCorrected() {
        //If empty, all the satellites of this constellation are affected for this Observation type
        return satsPhaseShift;
    }

}

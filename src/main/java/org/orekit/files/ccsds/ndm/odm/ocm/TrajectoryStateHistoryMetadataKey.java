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
package org.orekit.files.ccsds.ndm.odm.ocm;

import org.orekit.files.ccsds.ndm.odm.oem.InterpolationMethod;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;


/** Keys for {@link TrajectoryStateHistoryMetadata rajectory state history container} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum TrajectoryStateHistoryMetadataKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** Trajectory identification number. */
    TRAJ_ID((token, context, container) -> token.processAsFreeTextString(container::setTrajID)),

    /** Identification number of previous trajectory. */
    TRAJ_PREV_ID((token, context, container) -> token.processAsFreeTextString(container::setTrajPrevID)),

    /** Identification number of next trajectory. */
    TRAJ_NEXT_ID((token, context, container) -> token.processAsFreeTextString(container::setTrajNextID)),

    /** Basis of this trajectory state time history data. */
    TRAJ_BASIS((token, context, container) -> token.processAsFreeTextString(container::setTrajBasis)),

    /** Identification number of the orbit determination or simulation upon which this trajectory is based.*/
    TRAJ_BASIS_ID((token, context, container) -> token.processAsFreeTextString(container::setTrajBasisID)),

    /** Interpolation method to be used. */
    INTERPOLATION((token, context, container) -> token.processAsEnum(InterpolationMethod.class, container::setInterpolationMethod)),

    /** Interpolation degree. */
    INTERPOLATION_DEGREE((token, context, container) -> token.processAsInteger(container::setInterpolationDegree)),

    /** Orbit propagator used to generate this trajectory.
     * @since 11.2
     */
    PROPAGATOR((token, context, container) -> token.processAsFreeTextString(container::setPropagator)),

    /** Origin of the reference frame of the trajectory. */
    CENTER_NAME((token, context, container) -> token.processAsCenter(container::setCenter,
                                                                     context.getDataContext().getCelestialBodies())),

    /** Reference frame of the trajectory. */
    TRAJ_REF_FRAME((token, context, container) -> token.processAsFrame(container::setTrajReferenceFrame, context, true, false, false)),

    /** Epoch of the {@link #TRAJ_REF_FRAME trajectory reference frame}. */
    TRAJ_FRAME_EPOCH((token, context, container) -> token.processAsDate(container::setTrajFrameEpoch, context)),

    /** Useable start time entry. */
    USEABLE_START_TIME((token, context, container) -> token.processAsDate(container::setUseableStartTime, context)),

    /** Useable stop time entry. */
    USEABLE_STOP_TIME((token, context, container) -> token.processAsDate(container::setUseableStopTime, context)),

    /** Integer orbit revolution number entry. */
    ORB_REVNUM((token, context, container) -> token.processAsInteger(container::setOrbRevNum)),

    /** Basis for orbit revolution number entry. */
    ORB_REVNUM_BASIS((token, context, container) -> token.processAsInteger(container::setOrbRevNumBasis)),

    /** Type of averaging (Osculating, mean Brouwer, other...). */
    ORB_AVERAGING((token, context, container) -> token.processAsFreeTextString(container::setOrbAveraging)),

    /** Trajectory element set type.
     * @see OrbitElementsType
     */
    TRAJ_TYPE((token, context, container) -> token.processAsEnum(OrbitElementsType.class, container::setTrajType)),

    /** SI units for each elements of the trajectory state. */
    TRAJ_UNITS((token, context, container) -> token.processAsUnitList(container::setTrajUnits));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    TrajectoryStateHistoryMetadataKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final TrajectoryStateHistoryMetadata container) {
        return processor.process(token, context, container);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param context context binding
         * @param container container to fill
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ContextBinding context, TrajectoryStateHistoryMetadata container);
    }

}

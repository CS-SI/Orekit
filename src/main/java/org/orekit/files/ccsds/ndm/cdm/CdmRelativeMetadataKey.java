/* Copyright 2002-2022 CS GROUP
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
package org.orekit.files.ccsds.ndm.cdm;

import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.utils.units.Unit;
import org.orekit.files.ccsds.definitions.PocMethodFacade;
import org.orekit.files.ccsds.definitions.Units;

/** Keys for {@link CdmRelativeMetadata CDM container} entries.
 * @author Melina Vanel
 * @since 11.2
 */
public enum CdmRelativeMetadataKey {

    /** Date and time in UTC of the closest approach. */
    TCA((token, context, container) -> token.processAsDate(container::setTca, context)),

    /** Norm of relative position vector at TCA. */
    MISS_DISTANCE((token, context, container) -> token.processAsDouble(Unit.METRE, context.getParsedUnitsBehavior(),
                                                                             container::setMissDistance)),

    /** Norm of relative velocity vector at TCA. */
    RELATIVE_SPEED((token, context, container) -> token.processAsDouble(Units.M_PER_S, context.getParsedUnitsBehavior(),
                                                                             container::setRelativeSpeed)),

    /** The R component of Object2’s position relative to Object1’s position in the Radial/Transverse/Normal coordinate frame. */
    RELATIVE_POSITION_R((token, context, container) -> token.processAsDouble(Unit.METRE, context.getParsedUnitsBehavior(),
                                                                             container::setRelativePositionR)),

    /** The T component of Object2’s position relative to Object1’s position in the Radial/Transverse/Normal coordinate frame. */
    RELATIVE_POSITION_T((token, context, container) -> token.processAsDouble(Unit.METRE, context.getParsedUnitsBehavior(),
                                                                             container::setRelativePositionT)),

    /** The N component of Object2’s position relative to Object1’s position in the Radial/Transverse/Normal coordinate frame. */
    RELATIVE_POSITION_N((token, context, container) -> token.processAsDouble(Unit.METRE, context.getParsedUnitsBehavior(),
                                                                            container::setRelativePositionN)),

    /** The R component of Object2’s velocity relative to Object1’s veloity in the Radial/Transverse/Normal coordinate frame. */
    RELATIVE_VELOCITY_R((token, context, container) -> token.processAsDouble(Units.M_PER_S, context.getParsedUnitsBehavior(),
                                                                             container::setRelativeVelocityR)),

    /** The T component of Object2’s velocity relative to Object1’s veloity in the Radial/Transverse/Normal coordinate frame. */
    RELATIVE_VELOCITY_T((token, context, container) -> token.processAsDouble(Units.M_PER_S, context.getParsedUnitsBehavior(),
                                                                             container::setRelativeVelocityT)),

    /** The N component of Object2’s velocity relative to Object1’s veloity in the Radial/Transverse/Normal coordinate frame. */
    RELATIVE_VELOCITY_N((token, context, container) -> token.processAsDouble(Units.M_PER_S, context.getParsedUnitsBehavior(),
                                                                             container::setRelativeVelocityN)),

    /** The start time in UTC of the screening period for the conjunction assessment. */
    START_SCREEN_PERIOD((token, context, container) -> token.processAsDate(container::setStartScreenPeriod, context)),

    /** The stop time in UTC of the screening period for the conjunction assessment. */
    STOP_SCREEN_PERIOD((token, context, container) -> token.processAsDate(container::setStopScreenPeriod, context)),

    /** Name of the Object1 centered reference frame in which the screening volume data are given. */
    SCREEN_VOLUME_FRAME((token, context, container) -> token.processAsEnum(ScreenVolumeFrame.class, container::setScreenVolumeFrame)),

    /** Shape of the screening volume. */
    SCREEN_VOLUME_SHAPE((token, context, container) -> token.processAsEnum(ScreenVolumeShape.class, container::setScreenVolumeShape)),

    /** The R or T (depending on if RTN or TVN is selected) component size of the screening volume in the SCREEN_VOLUME_FRAME. */
    SCREEN_VOLUME_X((token, context, container) -> token.processAsDouble(Unit.METRE, context.getParsedUnitsBehavior(),
                                                                         container::setScreenVolumeX)),

    /** The T or V (depending on if RTN or TVN is selected) component size of the screening volume in the SCREEN_VOLUME_FRAME. */
    SCREEN_VOLUME_Y((token, context, container) -> token.processAsDouble(Unit.METRE, context.getParsedUnitsBehavior(),
                                                                         container::setScreenVolumeY)),

    /** The N component size of the screening volume in the SCREEN_VOLUME_FRAME. */
    SCREEN_VOLUME_Z((token, context, container) -> token.processAsDouble(Unit.METRE, context.getParsedUnitsBehavior(),
                                                                         container::setScreenVolumeZ)),

    /** The time in UTC when Object2 enters the screening volume. */
    SCREEN_ENTRY_TIME((token, context, container) -> token.processAsDate(container::setScreenEntryTime, context)),

    /** The time in UTC when Object2 exits the screening volume. */
    SCREEN_EXIT_TIME((token, context, container) -> token.processAsDate(container::setScreenExitTime, context)),

    /** The probability (between 0.0 and 1.0) that Object1 and Object2 will collide. */
    COLLISION_PROBABILITY((token, context, container) -> token.processAsDouble(Unit.ONE, context.getParsedUnitsBehavior(),
                                                                               container::setCollisionProbability)),

    /** The method that was used to calculate the collision probability. */
    COLLISION_PROBABILITY_METHOD((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            container.setCollisionProbaMethod(PocMethodFacade.parse(token.getContentAsNormalizedString()));
        }
        return true;
    });

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    CdmRelativeMetadataKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final CdmRelativeMetadata container) {
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
        boolean process(ParseToken token, ContextBinding context, CdmRelativeMetadata container);
    }


}

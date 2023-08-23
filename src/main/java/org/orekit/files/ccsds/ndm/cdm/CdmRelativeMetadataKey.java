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

    /** The Originator’s ID that uniquely identifies the conjunction to which the message refers. */
    CONJUNCTION_ID((token, context, container) -> token.processAsNormalizedString(container::setConjunctionId)),

    /** Date and time in UTC of the closest approach. */
    TCA((token, context, container) -> token.processAsDate(container::setTca, context)),

    /** Norm of relative position vector at TCA. */
    MISS_DISTANCE((token, context, container) -> token.processAsDouble(Unit.METRE, context.getParsedUnitsBehavior(),
                                                                             container::setMissDistance)),
    /** The length of the relative position vector, normalized to one-sigma dispersions of the combined error covariance
     * in the direction of the relative position vector. */
    MAHALANOBIS_DISTANCE((token, context, container) -> token.processAsDouble(Unit.NONE, context.getParsedUnitsBehavior(),
                                                                             container::setMahalanobisDistance)),

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

    /** The approach angle computed between Objects 1 and 2 in the RTN coordinate frame relative to object 1. */
    APPROACH_ANGLE((token, context, container) -> token.processAsDouble(Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                         container::setApproachAngle)),

    /** The start time in UTC of the screening period for the conjunction assessment. */
    START_SCREEN_PERIOD((token, context, container) -> token.processAsDate(container::setStartScreenPeriod, context)),

    /** The stop time in UTC of the screening period for the conjunction assessment. */
    STOP_SCREEN_PERIOD((token, context, container) -> token.processAsDate(container::setStopScreenPeriod, context)),

    /** Name of the Object1 centered reference frame in which the screening volume data are given. */
    SCREEN_VOLUME_FRAME((token, context, container) -> token.processAsEnum(ScreenVolumeFrame.class, container::setScreenVolumeFrame)),

    /** The type of screening to be used. */
    SCREEN_TYPE((token, context, container) -> token.processAsEnum(ScreenType.class, container::setScreenType)),

    /** Shape of the screening volume. */
    SCREEN_VOLUME_SHAPE((token, context, container) -> token.processAsEnum(ScreenVolumeShape.class, container::setScreenVolumeShape)),

    /** The radius of the screening volume. */
    SCREEN_VOLUME_RADIUS((token, context, container) -> token.processAsDouble(Unit.METRE, context.getParsedUnitsBehavior(),
                                                                             container::setScreenVolumeRadius)),

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

    /** The collision probability screening threshold used to identify this conjunction. */
    SCREEN_PC_THRESHOLD((token, context, container) -> token.processAsDouble(Unit.ONE, context.getParsedUnitsBehavior(),
                                                                             container::setScreenPcThreshold)),

    /** An array of 1 to n elements indicating the percentile(s) for which estimates of the collision probability are provided in the
     * COLLISION_PROBABILITY variable. */
    COLLISION_PERCENTILE((token, context, container) -> token.processAsIntegerArray(container::setCollisionPercentile)),

    /** The probability (between 0.0 and 1.0) that Object1 and Object2 will collide. */
    COLLISION_PROBABILITY((token, context, container) -> token.processAsDouble(Unit.ONE, context.getParsedUnitsBehavior(),
                                                                               container::setCollisionProbability)),

    /** The method that was used to calculate the collision probability. */
    COLLISION_PROBABILITY_METHOD((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            container.setCollisionProbaMethod(PocMethodFacade.parse(token.getContentAsNormalizedString()));
        }
        return true;
    }),

    /** The maximum collision probability that Object1 and Object2 will collide. */
    COLLISION_MAX_PROBABILITY((token, context, container) -> token.processAsDouble(Unit.ONE, context.getParsedUnitsBehavior(),
                                                                               container::setMaxCollisionProbability)),

    /** The method that was used to calculate the maximum collision probability. */
    COLLISION_MAX_PC_METHOD((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            container.setMaxCollisionProbabilityMethod(PocMethodFacade.parse(token.getRawContent()));
        }
        return true;
    }),

    /**  The space environment fragmentation impact (SEFI) adjusted estimate of collision probability that Object1 and Object2 will collide. */
    SEFI_COLLISION_PROBABILITY((token, context, container) -> token.processAsDouble(Unit.ONE, context.getParsedUnitsBehavior(),
                                                                               container::setSefiCollisionProbability)),

    /** The method that was used to calculate the space environment fragmentation impact collision probability. */
    SEFI_COLLISION_PROBABILITY_METHOD((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            container.setSefiCollisionProbabilityMethod(PocMethodFacade.parse(token.getRawContent()));
        }
        return true;
    }),

    /** The Space environment fragmentation model used. */
    SEFI_FRAGMENTATION_MODEL((token, context, container) -> token.processAsNormalizedString(container::setSefiFragmentationModel)),

    /** ID of previous CDM issued for event identified by CONJUNCTION_ID. */
    PREVIOUS_MESSAGE_ID((token, context, container) -> token.processAsFreeTextString(container::setPreviousMessageId)),

    /** UTC epoch of the previous CDM issued for the event identified by CONJUNCTION_ID. */
    PREVIOUS_MESSAGE_EPOCH((token, context, container) -> token.processAsDate(container::setPreviousMessageEpoch, context)),

    /** Scheduled UTC epoch of the next CDM associated with the event identified by CONJUNCTION_ID. */
    NEXT_MESSAGE_EPOCH((token, context, container) -> token.processAsDate(container::setNextMessageEpoch, context));


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

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

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.utils.units.Unit;

/** Keys for {@link AdditionalParameters CDM additional parameters} entries.
 * @author Melina Vanel
 * @since 11.2
 */
public enum AdditionalParametersKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** The actual area of the object. */
    AREA_PC((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                                 container::setAreaPC)),

    /** The effective area of the object exposed to atmospheric drag. */
    AREA_DRG((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                                 container::setAreaDRG)),

    /** The effective area of the object exposed to solar radiation pressure. */
    AREA_SRP((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                                 container::setAreaSRP)),

    /** The mass of the object. */
    MASS((token, context, container) -> token.processAsDouble(Unit.KILOGRAM, context.getParsedUnitsBehavior(),
                                                                 container::setMass)),

    /** The object’s Cd x A/m used to propagate the state vector and covariance to TCA. */
    CD_AREA_OVER_MASS((token, context, container) -> token.processAsDouble(Units.M2_PER_KG, context.getParsedUnitsBehavior(),
                                                                 container::setCDAreaOverMass)),

    /** The object’s Cr x A/m used to propagate the state vector and covariance to TCA. */
    CR_AREA_OVER_MASS((token, context, container) -> token.processAsDouble(Units.M2_PER_KG, context.getParsedUnitsBehavior(),
                                                                 container::setCRAreaOverMass)),

    /** The object’s acceleration due to in-track thrust used to propagate the state vector and covariance to TCA. */
    THRUST_ACCELERATION((token, context, container) -> token.processAsDouble(Units.M_PER_S2, context.getParsedUnitsBehavior(),
                                                                 container::setThrustAcceleration)),

    /** The amount of energy being removed from the object’s orbit by atmospheric drag. This value is an average calculated during the OD. */
    SEDR((token, context, container) -> token.processAsDouble(Units.W_PER_KG, context.getParsedUnitsBehavior(),
                                                                 container::setSedr));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    AdditionalParametersKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final AdditionalParameters container) {
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
        boolean process(ParseToken token, ContextBinding context, AdditionalParameters container);
    }

}

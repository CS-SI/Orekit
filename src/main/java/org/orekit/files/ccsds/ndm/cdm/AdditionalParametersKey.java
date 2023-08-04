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

    /** Minimum area (or cross-section) of the object to be used in the calculation of the probability of collision. */
    AREA_PC_MIN((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                                 container::setAreaPCMin)),

    /** Maximum area (or cross-section) of the object to be used in the calculation of the probability of collision. */
    AREA_PC_MAX((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                                 container::setAreaPCMax)),

    /** The effective area of the object exposed to atmospheric drag. */
    AREA_DRG((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                                 container::setAreaDRG)),

    /** The effective area of the object exposed to solar radiation pressure. */
    AREA_SRP((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                                 container::setAreaSRP)),

    /** Optimally Enclosing Box parent reference frame. */
    OEB_PARENT_FRAME((token, context, container) -> token.processAsFrame(container::setOebParentFrame, context, true, true, false)),

    /** Optimally Enclosing Box parent reference frame epoch. */
    OEB_PARENT_FRAME_EPOCH((token, context, container) -> token.processAsDate(container::setOebParentFrameEpoch, context)),

    /** Quaternion defining Optimally Enclosing Box (first vectorial component). */
    OEB_Q1((token, context, container) -> token.processAsIndexedDouble(1, Unit.ONE, context.getParsedUnitsBehavior(),
                                                                       container::setOebQ)),

    /** Quaternion defining Optimally Enclosing Box (second vectorial component). */
    OEB_Q2((token, context, container) -> token.processAsIndexedDouble(2, Unit.ONE, context.getParsedUnitsBehavior(),
                                                                       container::setOebQ)),

    /** Quaternion defining Optimally Enclosing Box (third vectorial component). */
    OEB_Q3((token, context, container) -> token.processAsIndexedDouble(3, Unit.ONE, context.getParsedUnitsBehavior(),
                                                                       container::setOebQ)),

    /** Quaternion defining Optimally Enclosing Box (scalar component). */
    OEB_QC((token, context, container) -> token.processAsIndexedDouble(0, Unit.ONE, context.getParsedUnitsBehavior(),
                                                                       container::setOebQ)),

    /**  Maximum physical dimension of Optimally Enclosing Box. */
    OEB_MAX((token, context, container) -> token.processAsDouble(Unit.METRE, context.getParsedUnitsBehavior(),
                                                                 container::setOebMax)),

    /**  Intermediate physical dimension of Optimally Enclosing Box. */
    OEB_INT((token, context, container) -> token.processAsDouble(Unit.METRE, context.getParsedUnitsBehavior(),
                                                                 container::setOebIntermediate)),

    /**  Minium physical dimension of Optimally Enclosing Box. */
    OEB_MIN((token, context, container) -> token.processAsDouble(Unit.METRE, context.getParsedUnitsBehavior(),
                                                                 container::setOebMin)),

    /** Cross-sectional area of Optimally Enclosing Box when viewed along the maximum OEB direction. */
    AREA_ALONG_OEB_MAX((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                                            container::setOebAreaAlongMax)),

    /** Cross-sectional area of Optimally Enclosing Box when viewed along the intermediate OEB direction. */
    AREA_ALONG_OEB_INT((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                                            container::setOebAreaAlongIntermediate)),

    /** Cross-sectional area of Optimally Enclosing Box when viewed along the minimum OEB direction. */
    AREA_ALONG_OEB_MIN((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                                            container::setOebAreaAlongMin)),
    /** Typical (50th percentile) radar cross-section. */
    RCS((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                             container::setRcs)),

    /** Minimum radar cross-section. */
    RCS_MIN((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                                 container::setMinRcs)),

    /** Maximum radar cross-section. */
    RCS_MAX((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                                 container::setMaxRcs)),

    /** Typical (50th percentile) absolute visual magnitude. */
    VM_ABSOLUTE((token, context, container) -> token.processAsDouble(Unit.ONE, context.getParsedUnitsBehavior(),
                                                                     container::setVmAbsolute)),

    /** Minimum apparent visual magnitude. */
    VM_APPARENT_MIN((token, context, container) -> token.processAsDouble(Unit.ONE, context.getParsedUnitsBehavior(),
                                                                         container::setVmApparentMin)),

    /** Typical (50th percentile) apparent visual magnitude. */
    VM_APPARENT((token, context, container) -> token.processAsDouble(Unit.ONE, context.getParsedUnitsBehavior(),
                                                                     container::setVmApparent)),

    /** Maximum apparent visual magnitude. */
    VM_APPARENT_MAX((token, context, container) -> token.processAsDouble(Unit.ONE, context.getParsedUnitsBehavior(),
                                                                         container::setVmApparentMax)),

    /** Typical (50th percentile) coefficient of reflectance. */
    REFLECTANCE((token, context, container) -> token.processAsDouble(Unit.ONE, context.getParsedUnitsBehavior(),
                                                                     container::setReflectance)),

    /** The mass of the object. */
    MASS((token, context, container) -> token.processAsDouble(Unit.KILOGRAM, context.getParsedUnitsBehavior(),
                                                                 container::setMass)),

    /** Object hard body radius. */
    HBR((token, context, container) -> token.processAsDouble(Unit.METRE, context.getParsedUnitsBehavior(),
                                                                     container::setHbr)),

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
                                                                 container::setSedr)),

    /** The distance of the furthest point in the objects orbit above the equatorial radius of the central body. */
    APOAPSIS_ALTITUDE((token, context, container) -> token.processAsDouble(Unit.KILOMETRE, context.getParsedUnitsBehavior(),
                                                                     container::setApoapsisAltitude)),

    /** The distance of the closest point in the objects orbit above the equatorial radius of the central body. */
    PERIAPSIS_ALTITUDE((token, context, container) -> token.processAsDouble(Unit.KILOMETRE, context.getParsedUnitsBehavior(),
                                                                     container::setPeriapsisAltitude)),

    /** The angle between the objects orbit plane and the orbit centers equatorial plane. */
    INCLINATION((token, context, container) -> token.processAsDouble(Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                     container::setInclination)),

    /** A measure of the confidence in the covariance errors matching reality. */
    COV_CONFIDENCE((token, context, container) -> token.processAsDouble(Unit.NONE, context.getParsedUnitsBehavior(),
                                                                     container::setCovConfidence)),

    /** The method used for the calculation of COV_CONFIDENCE. */
    COV_CONFIDENCE_METHOD((token, context, container) -> token.processAsFreeTextString(container::setCovConfidenceMethod));

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

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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.utils.units.Unit;


/** Keys for {@link Perturbations perturbations data} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum PerturbationsKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** Name of atmospheric model. */
    ATMOSPHERIC_MODEL((token, context, container) -> token.processAsFreeTextString(container::setAtmosphericModel)),

    /** Gravity model. */
    GRAVITY_MODEL(new GravityProcessor()),

    /** Oblate spheroid equatorial radius of central body. */
    EQUATORIAL_RADIUS((token, context, container) -> token.processAsDouble(Unit.KILOMETRE, context.getParsedUnitsBehavior(),
                                                                           container::setEquatorialRadius)),

    /** Central body oblate spheroid oblateness. */
    GM((token, context, container) -> token.processAsDouble(Units.KM3_PER_S2, context.getParsedUnitsBehavior(),
                                                            container::setGm)),

    /** N-body perturbation bodies. */
    N_BODY_PERTURBATIONS((token, context, container) -> token.processAsCenterList(container::setNBodyPerturbations,
                                                                                  context.getDataContext().getCelestialBodies())),

    /** Central body angular rotation rate. */
    CENTRAL_BODY_ROTATION((token, context, container) -> token.processAsDouble(Units.DEG_PER_S, context.getParsedUnitsBehavior(),
                                                                               container::setCentralBodyRotation)),

    /** Central body oblate spheroid oblateness. */
    OBLATE_FLATTENING((token, context, container) -> token.processAsDouble(Unit.ONE, context.getParsedUnitsBehavior(),
                                                                           container::setOblateFlattening)),

    /** Ocean tides model. */
    OCEAN_TIDES_MODEL((token, context, container) -> token.processAsFreeTextString(container::setOceanTidesModel)),

    /** Solid tides model. */
    SOLID_TIDES_MODEL((token, context, container) -> token.processAsFreeTextString(container::setSolidTidesModel)),

    /** Reduction theory used for precession and nutation modeling. */
    REDUCTION_THEORY((token, context, container) -> token.processAsFreeTextString(container::setReductionTheory)),

    /** Albedo model. */
    ALBEDO_MODEL((token, context, container) -> token.processAsFreeTextString(container::setAlbedoModel)),

    /** Albedo grid size. */
    ALBEDO_GRID_SIZE((token, context, container) -> token.processAsInteger(container::setAlbedoGridSize)),

    /** Shadow model used for solar radiation pressure. */
    SHADOW_MODEL((token, context, container) -> token.processAsEnum(ShadowModel.class, container::setShadowModel)),

    /** Names of shadow bodies. */
    SHADOW_BODIES((token, context, container) -> token.processAsCenterList(container::setShadowBodies,
                                                                           context.getDataContext().getCelestialBodies())),

    /** Solar Radiation Pressure model. */
    SRP_MODEL((token, context, container) -> token.processAsFreeTextString(container::setSrpModel)),

    /** Space Weather data source. */
    SW_DATA_SOURCE((token, context, container) -> token.processAsFreeTextString(container::setSpaceWeatherSource)),

    /** Epoch of the Space Weather data. */
    SW_DATA_EPOCH((token, context, container) -> token.processAsDate(container::setSpaceWeatherEpoch, context)),

    /** Interpolation method for Space Weather data. */
    SW_INTERP_METHOD((token, context, container) -> token.processAsFreeTextString(container::setInterpMethodSW)),

    /** Fixed (time invariant) value of the planetary 3-hour-range geomagnetic index Kₚ. */
    FIXED_GEOMAG_KP((token, context, container) -> token.processAsDouble(Units.NANO_TESLA, context.getParsedUnitsBehavior(),
                                                                         container::setFixedGeomagneticKp)),

    /** Fixed (time invariant) value of the planetary 3-hour-range geomagnetic index aₚ. */
    FIXED_GEOMAG_AP((token, context, container) -> token.processAsDouble(Units.NANO_TESLA, context.getParsedUnitsBehavior(),
                                                                         container::setFixedGeomagneticAp)),

    /** Fixed (time invariant) value of the planetary 1-hour-range geomagnetic index Dst. */
    FIXED_GEOMAG_DST((token, context, container) -> token.processAsDouble(Units.NANO_TESLA, context.getParsedUnitsBehavior(),
                                                                          container::setFixedGeomagneticDst)),

    /** Fixed (time invariant) value of the Solar Flux Unit daily proxy F10.7. */
    FIXED_F10P7((token, context, container) -> token.processAsDouble(Unit.SOLAR_FLUX_UNIT, context.getParsedUnitsBehavior(),
                                                                     container::setFixedF10P7)),

    /** Fixed (time invariant) value of the Solar Flux Unit 81-day running center-average proxy F10.7. */
    FIXED_F10P7_MEAN((token, context, container) -> token.processAsDouble(Unit.SOLAR_FLUX_UNIT, context.getParsedUnitsBehavior(),
                                                                          container::setFixedF10P7Mean)),

    /** Fixed (time invariant) value of the Solar Flux daily proxy M10.7. */
    FIXED_M10P7((token, context, container) -> token.processAsDouble(Unit.SOLAR_FLUX_UNIT, context.getParsedUnitsBehavior(),
                                                                     container::setFixedM10P7)),

    /** Fixed (time invariant) value of the Solar Flux 81-day running center-average proxy M10.7. */
    FIXED_M10P7_MEAN((token, context, container) -> token.processAsDouble(Unit.SOLAR_FLUX_UNIT, context.getParsedUnitsBehavior(),
                                                                          container::setFixedM10P7Mean)),

    /** Fixed (time invariant) value of the Solar Flux daily proxy S10.7. */
    FIXED_S10P7((token, context, container) -> token.processAsDouble(Unit.SOLAR_FLUX_UNIT, context.getParsedUnitsBehavior(),
                                                                     container::setFixedS10P7)),

    /** Fixed (time invariant) value of the Solar Flux 81-day running center-average proxy S10.7. */
    FIXED_S10P7_MEAN((token, context, container) -> token.processAsDouble(Unit.SOLAR_FLUX_UNIT, context.getParsedUnitsBehavior(),
                                                                          container::setFixedS10P7Mean)),

    /** Fixed (time invariant) value of the Solar Flux daily proxy Y10.7. */
    FIXED_Y10P7((token, context, container) -> token.processAsDouble(Unit.SOLAR_FLUX_UNIT, context.getParsedUnitsBehavior(),
                                                                     container::setFixedY10P7)),

    /** Fixed (time invariant) value of the Solar Flux 81-day running center-average proxy Y10.7. */
    FIXED_Y10P7_MEAN((token, context, container) -> token.processAsDouble(Unit.SOLAR_FLUX_UNIT, context.getParsedUnitsBehavior(),
                                                                          container::setFixedY10P7Mean));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    PerturbationsKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final Perturbations container) {
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
        boolean process(ParseToken token, ContextBinding context, Perturbations container);
    }

    /** Dedicated processor for gravity field. */
    private static class GravityProcessor implements TokenProcessor {

        /** Pattern for splitting gravity specification. */
        private static final Pattern GRAVITY_PATTERN =
                        Pattern.compile("^\\p{Blank}*([-_A-Za-z0-9]+)\\p{Blank}*:" +
                                        "\\p{Blank}*([0-9]+)D" +
                                        "\\p{Blank}*([0-9]+)O" +
                                        "\\p{Blank}*$");

        /** {@inheritDoc} */
        @Override
        public boolean process(final ParseToken token, final ContextBinding context, final Perturbations container) {
            if (token.getType() == TokenType.ENTRY) {
                final Matcher matcher = GRAVITY_PATTERN.matcher(token.getRawContent());
                if (!matcher.matches()) {
                    throw token.generateException(null);
                }
                container.setGravityModel(matcher.group(1),
                                          Integer.parseInt(matcher.group(2)),
                                          Integer.parseInt(matcher.group(3)));
            }
            return true;
        }
    }

}

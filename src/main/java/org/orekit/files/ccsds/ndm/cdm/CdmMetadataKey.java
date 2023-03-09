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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orekit.files.ccsds.definitions.YesNoUnknown;
import org.orekit.files.ccsds.ndm.odm.ocm.ObjectType;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;

/** Keys for {@link CdmMetadata CDM container} entries.
 * @author Melina Vanel
 * @since 11.2
 */
public enum CdmMetadataKey {

    /** Object to which data apply. */
    OBJECT((token, context, container) -> token.processAsNormalizedString(container::setObject)),

    /** Satellite catalog designator for object. */
    OBJECT_DESIGNATOR((token, context, container) -> token.processAsNormalizedString(container::setObjectDesignator)),

    /** Satellite catalog used for the object. */
    CATALOG_NAME((token, context, container) -> token.processAsNormalizedString(container::setCatalogName)),

    /** Spacecraft name for the object. */
    OBJECT_NAME((token, context, container) -> token.processAsNormalizedString(container::setObjectName)),

    /** Full international designator name for the object. */
    INTERNATIONAL_DESIGNATOR((token, context, container) -> token.processAsNormalizedString(container::setInternationalDes)),

    /** Object Type. */
    OBJECT_TYPE((token, context, container) -> token.processAsEnum(ObjectType.class, container::setObjectType)),

    /** Contact position of the owner/operator of the object. */
    OPERATOR_CONTACT_POSITION((token, context, container) -> token.processAsNormalizedString(container::setOperatorContactPosition)),

    /** Contact organization of the object. */
    OPERATOR_ORGANIZATION((token, context, container) -> token.processAsNormalizedString(container::setOperatorOrganization)),

    /** Phone number of the contact position for the object. */
    OPERATOR_PHONE((token, context, container) -> token.processAsNormalizedString(container::setOperatorPhone)),

    /** Email address of the contact position for the object. */
    OPERATOR_EMAIL((token, context, container) -> token.processAsNormalizedString(container::setOperatorEmail)),

    /** Unique identifier of Orbit Data Message(s) that are linked (relevant) to this Conjunction Data Message. */
    ODM_MSG_LINK((token, context, container) -> token.processAsFreeTextString(container::setOdmMsgLink)),

    /** Unique identifier of Attitude Data Message(s) that are linked (relevant) to this Conjunction Data Message. */
    ADM_MSG_LINK((token, context, container) -> token.processAsFreeTextString(container::setAdmMsgLink)),

    /** Unique name of the external ephemeris file used for the object or NONE. */
    EPHEMERIS_NAME((token, context, container) -> token.processAsNormalizedString(container::setEphemName)),

    /** Flag indicating whether new tracking observations are anticipated prior to the issue of the next CDM associated with the event
     * specified by CONJUNCTION_ID. */
    OBS_BEFORE_NEXT_MESSAGE((token, context, container) -> token.processAsEnum(YesNoUnknown.class, container::setObsBeforeNextMessage)),

    /** Method used to calculate the covariance. */
    COVARIANCE_METHOD((token, context, container) -> token.processAsEnum(CovarianceMethod.class, container::setCovarianceMethod)),

    /** The source from which the covariance data used in the report for both Object 1 and Object 2 originates. */
    COVARIANCE_SOURCE((token, context, container) -> token.processAsNormalizedString(container::setCovarianceSource)),

    /** Manoeuver capacity. */
    MANEUVERABLE((token, context, container) -> token.processAsManeuvrableEnum(container::setManeuverable)),

    /** Central body for Object 1 and 2. */
    ORBIT_CENTER((token, context, container) -> token.processAsCenter(container::setOrbitCenter,
                                                                    context.getDataContext().getCelestialBodies())),

    /** Name of the reference frame, in which state vector data are given. */
    REF_FRAME((token, context, container) -> token.processAsFrame(container::setRefFrame, context, true, true, true)),

    /** Flag indicating the type of alternate covariance information provided. */
    ALT_COV_TYPE((token, context, container) -> token.processAsEnum(AltCovarianceType.class, container::setAltCovType)),

    /** Name of the reference frame in which the alternate covariance data are given. */
    ALT_COV_REF_FRAME((token, context, container) -> token.processAsFrame(container::setAltCovRefFrame, context, true, false, false)),

    /** Gravity model. */
    GRAVITY_MODEL(new GravityProcessor()),

    /** Name of atmospheric model. */
    ATMOSPHERIC_MODEL((token, context, container) -> token.processAsNormalizedString(container::setAtmosphericModel)),

    /** N-body perturbation bodies. */
    N_BODY_PERTURBATIONS((token, context, container) -> token.processAsCenterList(container::setNBodyPerturbations,
                                                                                  context.getDataContext().getCelestialBodies())),

    /** Is solar radiation pressure used for the OD of the object ? */
    SOLAR_RAD_PRESSURE((token, context, container) -> token.processAsEnum(YesNoUnknown.class, container::setSolarRadiationPressure)),

    /** Is solid Earth and ocean tides used for the OD of the object ? */
    EARTH_TIDES((token, context, container) -> token.processAsEnum(YesNoUnknown.class, container::setEarthTides)),

    /** Indication of whether in-track thrust modeling used for the object. */
    INTRACK_THRUST((token, context, container) -> token.processAsEnum(YesNoUnknown.class, container::setIntrackThrust));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    CdmMetadataKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final CdmMetadata container) {
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
        boolean process(ParseToken token, ContextBinding context, CdmMetadata container);
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
        public boolean process(final ParseToken token, final ContextBinding context, final CdmMetadata container) {
            if (token.getType() == TokenType.ENTRY) {
                final Matcher matcher = GRAVITY_PATTERN.matcher(token.getContentAsNormalizedString());
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

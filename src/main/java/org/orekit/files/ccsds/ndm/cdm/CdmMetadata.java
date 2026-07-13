/* Copyright 2002-2026 CS GROUP
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.orekit.annotation.Nullable;
import org.orekit.bodies.CelestialBody;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.BodyFacade;
import org.orekit.files.ccsds.definitions.CcsdsFrameMapper;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.definitions.YesNoUnknown;
import org.orekit.files.ccsds.ndm.odm.ocm.ObjectType;
import org.orekit.files.ccsds.section.Metadata;
import org.orekit.frames.Frame;

/**
 * This class gathers the meta-data present in the Conjunction Data Message (CDM).
 * @author Melina Vanel
 * @since 11.2
 */
public class CdmMetadata extends Metadata {

    /** CDM relative metadata. */
    private CdmRelativeMetadata relativeMetadata;

    /** Refering to object 1 or 2. */
    private String object;

    /** Unique satellite identification designator for the object. */
    private String objectDesignator;

    /** Specification of satellite catalog source. */
    private String catalogName;

    /** Object name. */
    private String objectName;

    /** International designator for the object as assigned by the UN Committee
     * on Space Research (COSPAR) and the US National Space Science Data Center (NSSDC). */
    private String internationalDesignator;

    /** Type of object. */
    @Nullable
    private ObjectType objectType;

    /** Operator contact position for the space object. */
    @Nullable
    private String operatorContact;

    /** Operator organization for the space object. */
    @Nullable
    private String operatorOrganization;

    /** Operator phone for the space object. */
    @Nullable
    private String operatorPhone;

    /** Operator email for the space object. */
    @Nullable
    private String operatorEmail;

    /** Unique identifier of Orbit Data Message(s) that are linked (relevant) to this Conjunction Data Message. */
    @Nullable
    private String odmMsgLink;

    /** Unique identifier of Attitude Data Message(s) that are linked (relevant) to this Conjunction Data Message. */
    @Nullable
    private String admMsgLink;

    /** Unique name of the external ephemeris file used for the object or NONE. */
    private String ephemName;

    /** Flag indicating whether new tracking observations are anticipated prior to the issue of the next CDM associated with the event
     * specified by CONJUNCTION_ID. */
    @Nullable
    private YesNoUnknown obsBeforeNextMessage;

    /** Operator email for the space object. */
    private CovarianceMethod covarianceMethod;

    /** Maneuver capacity. */
    private Maneuvrable maneuverable;

    /** Central body around which Object1 and 2 are orbiting. */
    @Nullable
    private BodyFacade orbitCenter;

    /** Reference frame in which state vector data are given. */
    private FrameFacade refFrame;

    /** Gravity model name. */
    @Nullable
    private String gravityModel;

    /** Degree of the gravity model. */
    @Nullable
    private Integer gravityDegree;

    /** Order of the gravity model. */
    @Nullable
    private Integer gravityOrder;

    /** Name of atmospheric model. */
    @Nullable
    private String atmosphericModel;

    /** N-body perturbation bodies. */
    private List<BodyFacade> nBodyPerturbations;

    /** Solar radiation pressure taken indicator.
     * <p>
     *     STANDARD CCSDS saying YES/NO choice and optional
     * </p>
     */
    @Nullable
    private YesNoUnknown isSolarRadPressure;

    /** Is solid Earth and ocean tides taken into account or not. STANDARD CCSDS saying YES/NO choice and optional */
    @Nullable
    private YesNoUnknown isEarthTides;

    /** Is in-track thrust modelling used or not. STANDARD CCSDS saying YES/NO choice and optional */
    @Nullable
    private YesNoUnknown isIntrackThrustModeled;

    /** The source from which the covariance data used in the report for both Object 1 and Object 2 originates. */
    @Nullable
    private String covarianceSource;

    /** Flag indicating the type of alternate covariance information provided. */
    @Nullable
    private AltCovarianceType altCovType;

    /** Reference frame in which the alternate covariance data are given. */
    @Nullable
    private FrameFacade altCovRefFrame;

    /** Simple constructor.
     *
     * @param dataContext data context
     * @param frameMapper for creating an Orekit {@link Frame}.
     * @since 13.1.5
     */
    public CdmMetadata(final DataContext dataContext,
                       final CcsdsFrameMapper frameMapper) {
        super(null, frameMapper);
        this.nBodyPerturbations = new ArrayList<>();
        final CelestialBody earth = dataContext.getCelestialBodies().getEarth();
        orbitCenter = new BodyFacade(earth.getName().toUpperCase(), earth);
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        // We only check values that are mandatory in a cdm file
        checkNotNull(object,                  CdmMetadataKey.OBJECT.name());
        checkNotNull(objectDesignator,        CdmMetadataKey.OBJECT_DESIGNATOR.name());
        checkNotNull(catalogName,             CdmMetadataKey.CATALOG_NAME.name());
        checkNotNull(objectName,              CdmMetadataKey.OBJECT_NAME.name());
        checkNotNull(internationalDesignator, CdmMetadataKey.INTERNATIONAL_DESIGNATOR.name());
        checkNotNull(ephemName,               CdmMetadataKey.EPHEMERIS_NAME.name());
        checkNotNull(covarianceMethod,        CdmMetadataKey.COVARIANCE_METHOD.name());
        checkNotNull(maneuverable,            CdmMetadataKey.MANEUVERABLE.name());
        checkNotNull(refFrame,                CdmMetadataKey.REF_FRAME.name());
    }

    /**
     * Get the relative metadata following header, they are the common metadata for the CDM.
     * @return relativeMetadata relative metadata
     */
    public CdmRelativeMetadata getRelativeMetadata() {
        return relativeMetadata;
    }

    /**
     * Set the relative metadata following header, they are the common metadata for the CDM.
     * @param relativeMetadata relative metadata
     */
    public void setRelativeMetadata(final CdmRelativeMetadata relativeMetadata) {
        this.relativeMetadata = relativeMetadata;
    }

    /**
     * Get the object name for which metadata are given.
     * @return the object name
     */
    public String getObject() {
        return object;
    }

    /**
     * Set the object name for which metadata are given.
     * @param object = object 1 or 2 to be set
     */
    public void setObject(final String object) {
        this.setTimeSystem(TimeSystem.UTC);
        refuseFurtherComments();
        this.object = object;
    }

    /**
     * Get the object satellite catalog designator for which metadata are given.
     * @return the satellite catalog designator for the object
     */
    public String getObjectDesignator() {
        return objectDesignator;
    }

    /**
     * Set the satellite designator for the object for which metadata are given.
     * @param objectDesignator for the spacecraft to be set
     */
    public void setObjectDesignator(final String objectDesignator) {
        refuseFurtherComments();
        this.objectDesignator = objectDesignator;
    }

    /**
     * Get the satellite catalog used for the object.
     * @return the catalog name
     */
    public String getCatalogName() {
        return catalogName;
    }

    /**
     * Set the satellite catalog name used for object.
     * @param catalogName for the spacecraft to be set
     */
    public void setCatalogName(final String catalogName) {
        refuseFurtherComments();
        this.catalogName = catalogName;
    }

    /**
     * Get the spacecraft name for the object.
     * @return the spacecraft name
     */
    public String getObjectName() {
        return objectName;
    }

    /**
     * Set the spacecraft name used for object.
     * @param objectName for the spacecraft to be set
     */
    public void setObjectName(final String objectName) {
        refuseFurtherComments();
        this.objectName = objectName;
    }

    /**
     * Get the international designator for the object.
     * @return the international designator
     */
    public String getInternationalDes() {
        return internationalDesignator;
    }

    /**
     * Set the international designator used for object.
     * @param internationalDes for the object to be set
     */
    public void setInternationalDes(final String internationalDes) {
        refuseFurtherComments();
        this.internationalDesignator = internationalDes;
    }

    /**
     * Get the type of object.
     * @return the object type
     */
    public Optional<ObjectType> getObjectType() {
        return Optional.ofNullable(objectType);
    }

    /**
     * Set the type of object.
     * @param objectType type of object
     */
    public void setObjectType(final ObjectType objectType) {
        refuseFurtherComments();
        this.objectType = objectType;
    }

    /**
     * Get the contact position of the owner / operator of the object.
     * @return the contact position
     */
    public Optional<String> getOperatorContactPosition() {
        return Optional.ofNullable(operatorContact);
    }

    /**
     * Set the contact position for the object owner / operator.
     * @param opContact for the object to be set
     */
    public void setOperatorContactPosition(final String opContact) {
        refuseFurtherComments();
        this.operatorContact = opContact;
    }

    /**
     * Get the contact organisation of the object.
     * @return the contact organisation
     */
    public Optional<String> getOperatorOrganization() {
        return Optional.ofNullable(operatorOrganization);
    }

    /**
     * Set the contact organisation of the object.
     * @param operatorOrganization contact organisation for the object to be set
     */
    public void setOperatorOrganization(final String operatorOrganization) {
        refuseFurtherComments();
        this.operatorOrganization = operatorOrganization;
    }

    /**
     * Get the contact phone of the operator of the object.
     * @return the operator phone
     */
    public Optional<String> getOperatorPhone() {
        return Optional.ofNullable(operatorPhone);
    }

    /**
     * Set the operator phone of the object.
     * @param operatorPhone contact phone for the object to be set
     */
    public void setOperatorPhone(final String operatorPhone) {
        refuseFurtherComments();
        this.operatorPhone = operatorPhone;
    }

    /**
     * Get the email of the operator of the object.
     * @return the operator email
     */
    public Optional<String> getOperatorEmail() {
        return Optional.ofNullable(operatorEmail);
    }

    /**
     * Set the object operator email.
     * @param operatorEmail operator email for the object to be set
     */
    public void setOperatorEmail(final String operatorEmail) {
        refuseFurtherComments();
        this.operatorEmail = operatorEmail;
    }

    /**
     * Get the unique name of the external ephemeris used for OD.
     * @return the name of ephemeris used
     */
    public String getEphemName() {
        return ephemName;
    }

    /**
     * Set the name of external ephemeris used for OD.
     * @param ephemName me of external ephemeris used
     */
    public void setEphemName(final String ephemName) {
        refuseFurtherComments();
        this.ephemName = ephemName;
    }

    /**
     * Get the method name used to calculate covariance during OD.
     * @return the name of covariance calculation method
     */
    public CovarianceMethod getCovarianceMethod() {
        return covarianceMethod;
    }

    /**
     * Set the method name used to calculate covariance during OD.
     * @param covarianceMethod method name for covariance calculation
     */
    public void setCovarianceMethod(final CovarianceMethod covarianceMethod) {
        refuseFurtherComments();
        this.covarianceMethod = covarianceMethod;
    }

    /**
     * Get the ability of object to maneuver or not.
     * @return the ability to maneuver
     */
    public Maneuvrable getManeuverable() {
        return maneuverable;
    }

    /**
     * Set the object maneuver ability.
     * @param maneuverable ability to maneuver
     */
    public void setManeuverable(final Maneuvrable maneuverable) {
        refuseFurtherComments();
        this.maneuverable = maneuverable;
    }

    /**
     * Get the central body for object 1 and 2.
     * @return the name of the central body
     */
    public Optional<BodyFacade> getOrbitCenter() {
        return Optional.ofNullable(orbitCenter);
    }

    /**
     * Set the central body name for object 1 and 2.
     * @param orbitCenter name of the central body
     */
    public void setOrbitCenter(final BodyFacade orbitCenter) {
        refuseFurtherComments();
        this.orbitCenter = orbitCenter;
    }

    /**
     * Get the reference frame in which data are given: used for state vector and
     * Keplerian elements data (and for the covariance reference frame if none is given).
     *
     * @return the reference frame
     */
    public Frame getFrame() {
        // CDM format does not allow specifying frame epoch
        return getFrameMapper().buildCcsdsFrame(orbitCenter, refFrame, null);
    }

    /**
     * Get the value of {@code REF_FRAME} as an Orekit {@link Frame}. The {@code
     * ORBIT_CENTER} key word has not been applied yet, so the returned frame may not
     * correspond to the reference frame of the data in the file.
     * @return the reference frame
     */
    public FrameFacade getRefFrame() {
        return refFrame;
    }

    /**
     * Set the name of the reference frame in which the state vector data are given.
     * @param refFrame reference frame
     */
    public void setRefFrame(final FrameFacade refFrame) {
        refuseFurtherComments();
        this.refFrame = refFrame;
    }

    /** Get gravity model name.
     * @return gravity model name
     */
    public Optional<String> getGravityModel() {
        return Optional.ofNullable(gravityModel);
    }

    /** Get degree of the gravity model.
     * @return degree of the gravity model
     */
    public Optional<Integer> getGravityDegree() {
        return Optional.ofNullable(gravityDegree);
    }

    /** Get order of the gravity model.
     * @return order of the gravity model
     */
    public Optional<Integer> getGravityOrder() {
        return Optional.ofNullable(gravityOrder);
    }

    /** Set gravity model.
     * @param name name of the model
     * @param degree degree of the model
     * @param order order of the model
     */
    public void setGravityModel(final String name, final int degree, final int order) {
        refuseFurtherComments();
        this.gravityModel  = name;
        this.gravityDegree = degree;
        this.gravityOrder  = order;
    }

    /** Get name of atmospheric model.
     * @return name of atmospheric model
     */
    public Optional<String> getAtmosphericModel() {
        return Optional.ofNullable(atmosphericModel);
    }

    /** Set name of atmospheric model.
     * @param atmosphericModel name of atmospheric model
     */
    public void setAtmosphericModel(final String atmosphericModel) {
        refuseFurtherComments();
        this.atmosphericModel = atmosphericModel;
    }

    /** Get n-body perturbation bodies.
     * @return n-body perturbation bodies
     */
    public List<BodyFacade> getNBodyPerturbations() {
        return nBodyPerturbations;
    }

    /** Set n-body perturbation bodies.
     * @param nBody n-body perturbation bodies
     */
    public void setNBodyPerturbations(final List<BodyFacade> nBody) {
        refuseFurtherComments();
        this.nBodyPerturbations = nBody;
    }

    /**
     * Get Enum YesNoUnknown that indicates if Solar Radiation Pressure is taken into account or not.
     * @return isSolarRadPressure YesNoUnknown
     */
    public Optional<YesNoUnknown> getSolarRadiationPressure() {
        return Optional.ofNullable(isSolarRadPressure);
    }

    /**
     * Set Enum that indicates if Solar Radiation Pressure is taken into account or not.
     * @param isSolRadPressure YesNoUnknown
     */
    public void setSolarRadiationPressure(final YesNoUnknown isSolRadPressure) {
        refuseFurtherComments();
        this.isSolarRadPressure = isSolRadPressure;
    }

    /**
     * Get Enum YesNoUnknown that indicates if Earth and ocean tides are taken into account or not.
     * @return isEarthTides YesNoUnknown
     */
    public Optional<YesNoUnknown> getEarthTides() {
        return Optional.ofNullable(isEarthTides);
    }

    /**
     * Set Enum YesNoUnknown that indicates if Earth and ocean tides are taken into account or not.
     * @param EarthTides YesNoUnknown
     */
    public void setEarthTides(final YesNoUnknown EarthTides) {
        refuseFurtherComments();
        this.isEarthTides = EarthTides;
    }

    /**
     * Get Enum YesNoUnknown that indicates if intrack thrust modeling was into account or not.
     * @return isEarthTides YesNoUnknown
     */
    public Optional<YesNoUnknown> getIntrackThrust() {
        return Optional.ofNullable(isIntrackThrustModeled);
    }

    /**
     * Set boolean that indicates if intrack thrust modeling was into account or not.
     * @param IntrackThrustModeled YesNoUnknown
     */
    public void setIntrackThrust(final YesNoUnknown IntrackThrustModeled) {
        refuseFurtherComments();
        this.isIntrackThrustModeled = IntrackThrustModeled;
    }

    /** Get the source of the covariance data.
     * @return the covarianceSource
     */
    public Optional<String> getCovarianceSource() {
        return Optional.ofNullable(covarianceSource);
    }

    /** Set the source of the covariance data.
     * @param covarianceSource the covarianceSource to set
     */
    public void setCovarianceSource(final String covarianceSource) {
        refuseFurtherComments();
        this.covarianceSource = covarianceSource;
    }

    /** Get the flag indicating the type of alternate covariance information provided.
     * @return the altCovType
     */
    public Optional<AltCovarianceType> getAltCovType() {
        return Optional.ofNullable(altCovType);
    }

    /** Set the flag indicating the type of alternate covariance information provided.
     * @param altCovType the altCovType to set
     */
    public void setAltCovType(final AltCovarianceType altCovType) {
        refuseFurtherComments();
        this.altCovType = altCovType;
    }

     /**
     * Get the value of {@code ALT_COV_REF_FRAME} as an Orekit {@link Frame}.
     * @return the reference frame
     */
    public Optional<FrameFacade> getAltCovRefFrame() {
        return Optional.ofNullable(altCovRefFrame);
    }

    /**
     * Set the name of the reference frame in which the alternate covariance data are given.
     * @param altCovRefFrame alternate covariance reference frame
     */
    public void setAltCovRefFrame(final FrameFacade altCovRefFrame) {
        refuseFurtherComments();

        getAltCovType().
            orElseThrow(() -> new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD,
                                                  CdmMetadataKey.ALT_COV_TYPE));

        altCovRefFrame.
            asFrame().
            orElseThrow(() -> new OrekitException(OrekitMessages.CCSDS_INVALID_FRAME,
                                                  altCovRefFrame.getName()));

        // Only set the frame if within the allowed options: GCRF, EME2000, ITRF
        final CelestialBodyFrame celestialBodyFrame = altCovRefFrame.asCelestialBodyFrame().orElseThrow();
        if ( celestialBodyFrame == CelestialBodyFrame.GCRF ||
                 celestialBodyFrame == CelestialBodyFrame.EME2000 ||
                     celestialBodyFrame.name().contains("ITRF") ) {
            this.altCovRefFrame = altCovRefFrame;
        } else {
            throw new OrekitException(OrekitMessages.CCSDS_INVALID_FRAME, altCovRefFrame.getName());
        }
    }

    /**
     * Get the reference frame in which the alternative covariance data is
     * given.
     * <p>
     * Can be empty if {@link #getAltCovType()} is empty.
     * </p>
     * @return alternative covariance reference frame.
     * @see #getAltCovType()
     * @see #getAltCovRefFrame()
     * @since 13.1.5
     */
    public Optional<Frame> getAltCovFrame() {
        // Epoch of AltCovRefFrame can't be specified.
        return getAltCovRefFrame().isEmpty() ?
               Optional.empty() :
               Optional.of(getFrameMapper().buildCcsdsFrame(getAltCovRefFrame().get(), null));
    }

    /** Get the unique identifier of Orbit Data Message(s) that are linked (relevant) to this Conjunction Data Message.
     * @return the odmMsgLink
     */
    public Optional<String> getOdmMsgLink() {
        return Optional.ofNullable(odmMsgLink);
    }

    /** Set the unique identifier of Orbit Data Message(s) that are linked (relevant) to this Conjunction Data Message.
     * @param odmMsgLink the odmMsgLink to set
     */
    public void setOdmMsgLink(final String odmMsgLink) {
        refuseFurtherComments();
        this.odmMsgLink = odmMsgLink;
    }

    /** Get the unique identifier of Attitude Data Message(s) that are linked (relevant) to this Conjunction Data Message.
     * @return the admMsgLink
     */
    public Optional<String> getAdmMsgLink() {
        return Optional.ofNullable(admMsgLink);
    }

    /** Set the unique identifier of Attitude Data Message(s) that are linked (relevant) to this Conjunction Data Message.
     * @param admMsgLink the admMsgLink to set
     */
    public void setAdmMsgLink(final String admMsgLink) {
        refuseFurtherComments();
        this.admMsgLink = admMsgLink;
    }

    /** Get the flag indicating whether new tracking observations are anticipated prior to the issue of the next CDM associated with the event
     * specified by CONJUNCTION_ID.
     * @return the obsBeforeNextMessage
     */
    public Optional<YesNoUnknown> getObsBeforeNextMessage() {
        return Optional.ofNullable(obsBeforeNextMessage);
    }

    /** Set the flag indicating whether new tracking observations are anticipated prior to the issue of the next CDM associated with the event
     * specified by CONJUNCTION_ID.
     * @param obsBeforeNextMessage the obsBeforeNextMessage to set
     */
    public void setObsBeforeNextMessage(final YesNoUnknown obsBeforeNextMessage) {
        refuseFurtherComments();
        this.obsBeforeNextMessage = obsBeforeNextMessage;
    }
}

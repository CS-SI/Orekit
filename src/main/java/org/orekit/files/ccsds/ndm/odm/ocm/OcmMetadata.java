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

import java.util.List;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.odm.OdmMetadata;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.time.AbsoluteDate;

/** Meta-data for {@link OcmMetadata Orbit Comprehensive Message}.
 * @since 11.0
 */
public class OcmMetadata extends OdmMetadata {

    /** Default value for SCLK_OFFSET_AT_EPOCH.
     * @since 12.0
     */
    public static final double DEFAULT_SCLK_OFFSET_AT_EPOCH = 0.0;

    /** Default value for SCLK_SEC_PER_SI_SEC.
     * @since 12.0
     */
    public static final double DEFAULT_SCLK_SEC_PER_SI_SEC = 1.0;

    /** International designator for the object as assigned by the UN Committee
     * on Space Research (COSPAR) and the US National Space Science Data Center (NSSDC). */
    private String internationalDesignator;

    /** Specification of satellite catalog source. */
    private String catalogName;

    /** Unique satellite identification designator for the object. */
    private String objectDesignator;

    /** Alternate names for this space object. */
    private List<String> alternateNames;

    /** Programmatic Point Of Contact at originator. */
    private String originatorPOC;

    /** Position of Programmatic Point Of Contact at originator. */
    private String originatorPosition;

    /** Phone number of Programmatic Point Of Contact at originator. */
    private String originatorPhone;

    /** Email address of Programmatic Point Of Contact at originator.
     * @since 11.2
     */
    private String originatorEmail;

    /** Address of Programmatic Point Of Contact at originator. */
    private String originatorAddress;

    /** Creating agency or operator. */
    private String techOrg;

    /** Technical Point Of Contact at originator. */
    private String techPOC;

    /** Position of Technical Point Of Contact at originator. */
    private String techPosition;

    /** Phone number of Technical Point Of Contact at originator. */
    private String techPhone;

    /** Email address of Technical Point Of Contact at originator.
     * @since 11.2
     */
    private String techEmail;

    /** Address of Technical Point Of Contact at originator. */
    private String techAddress;

    /** Unique ID identifying previous message from a given originator. */
    private String previousMessageID;

    /** Unique ID identifying next message from a given originator. */
    private String nextMessageID;

    /** Unique identifier of Attitude Data Message linked to this Orbit Data Message. */
    private String admMessageLink;

    /** Unique identifier of Conjunction Data Message linked to this Orbit Data Message. */
    private String cdmMessageLink;

    /** Unique identifier of Pointing Request Message linked to this Orbit Data Message. */
    private String prmMessageLink;

    /** Unique identifier of Reentry Data Messages linked to this Orbit Data Message. */
    private String rdmMessageLink;

    /** Unique identifier of Tracking Data Messages linked to this Orbit Data Message. */
    private String tdmMessageLink;

    /** Operator of the space object. */
    private String operator;

    /** Owner of the space object. */
    private String owner;

    /** Name of the country where the space object owner is based. */
    private String country;

    /** Name of the constellation this space object belongs to. */
    private String constellation;

    /** Type of object. */
    private ObjectType objectType;

    /** Epoch to which <em>all</em> relative times are referenced in data blocks;
     * unless overridden by block-specific {@code EPOCH_TZERO} values. */
    private AbsoluteDate epochT0;

    /** Operational status. */
    private OpsStatus opsStatus;

    /** Orbit category. */
    private OrbitCategory orbitCategory;

    /** List of elements of information data blocks included in this message. */
    private List<OcmElements> ocmDataElements;

    /** Spacecraft clock count at {@link #getEpochT0()}. */
    private double sclkOffsetAtEpoch;

    /** Number of spacecraft clock seconds occurring during one SI second. */
    private double sclkSecPerSISec;

    /** Creation date of previous message from a given originator. */
    private AbsoluteDate previousMessageEpoch;

    /** Creation date of next message from a given originator. */
    private AbsoluteDate nextMessageEpoch;

    /** Time of the earliest data contained in the OCM. */
    private AbsoluteDate startTime;

    /** Time of the latest data contained in the OCM. */
    private AbsoluteDate stopTime;

    /** Span of time that the OCM covers. */
    private double timeSpan;

    /** Difference (TAI – UTC) in seconds at epoch {@link #epochT0}. */
    private double taimutcT0;

    /** Epoch of next leap second.
     * @since 11.2
     */
    private AbsoluteDate nextLeapEpoch;

    /** Difference (TAI – UTC) in seconds incorporated at {@link #nextLeapEpoch}.
     * @since 11.2
     */
    private double nextLeapTaimutc;

    /** Difference (UT1 – UTC) in seconds at epoch {@link #epochT0}. */
    private double ut1mutcT0;

    /** Source and version of Earth Orientation Parameters. */
    private String eopSource;

    /** Interpolation method for Earth Orientation Parameters. */
    private String interpMethodEOP;

    /** Source and version of celestial body (e.g. Sun/Earth/Planetary). */
    private String celestialSource;

    /** Data context.
     * @since 12.0
     */
    private final DataContext dataContext;

    /** Create a new meta-data.
     * @param dataContext data context
     */
    public OcmMetadata(final DataContext dataContext) {

        // set up the few fields that have default values as per CCSDS standard
        super(TimeSystem.UTC);
        sclkOffsetAtEpoch = DEFAULT_SCLK_OFFSET_AT_EPOCH;
        sclkSecPerSISec   = DEFAULT_SCLK_SEC_PER_SI_SEC;
        timeSpan          = Double.NaN;
        taimutcT0         = Double.NaN;
        ut1mutcT0         = Double.NaN;
        nextLeapTaimutc   = Double.NaN;
        this.dataContext  = dataContext;

    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        // we don't call super.checkMandatoryEntries() because
        // all of the parameters considered mandatory at ODM level
        // for OPM, OMM and OEM are in fact optional in OCM
        // only TIME_SYSTEM and EPOCH_TZERO are mandatory
        checkNotNull(getTimeSystem(), MetadataKey.TIME_SYSTEM.name());
        checkNotNull(epochT0,         OcmMetadataKey.EPOCH_TZERO.name());
        if (nextLeapEpoch != null) {
            checkNotNaN(nextLeapTaimutc, OcmMetadataKey.NEXT_LEAP_TAIMUTC.name());
        }
    }

    /** Get the international designator for the object.
     * @return international designator for the object
     */
    public String getInternationalDesignator() {
        return internationalDesignator;
    }

    /** Set the international designator for the object.
     * @param internationalDesignator international designator for the object
     */
    public void setInternationalDesignator(final String internationalDesignator) {
        refuseFurtherComments();
        this.internationalDesignator = internationalDesignator;
    }

    /** Get the specification of satellite catalog source.
     * @return specification of satellite catalog source
     */
    public String getCatalogName() {
        return catalogName;
    }

    /** Set the specification of satellite catalog source.
     * @param catalogName specification of satellite catalog source
     */
    public void setCatalogName(final String catalogName) {
        refuseFurtherComments();
        this.catalogName = catalogName;
    }

    /** Get the unique satellite identification designator for the object.
     * @return unique satellite identification designator for the object.
     */
    public String getObjectDesignator() {
        return objectDesignator;
    }

    /** Set the unique satellite identification designator for the object.
     * @param objectDesignator unique satellite identification designator for the object
     */
    public void setObjectDesignator(final String objectDesignator) {
        refuseFurtherComments();
        this.objectDesignator = objectDesignator;
    }

    /** Get the alternate names for this space object.
     * @return alternate names
     */
    public List<String> getAlternateNames() {
        return alternateNames;
    }

    /** Set the alternate names for this space object.
     * @param alternateNames alternate names
     */
    public void setAlternateNames(final List<String> alternateNames) {
        refuseFurtherComments();
        this.alternateNames = alternateNames;
    }

    /** Get the programmatic Point Of Contact at originator.
     * @return programmatic Point Of Contact at originator
     */
    public String getOriginatorPOC() {
        return originatorPOC;
    }

    /** Set the programmatic Point Of Contact at originator.
     * @param originatorPOC programmatic Point Of Contact at originator
     */
    public void setOriginatorPOC(final String originatorPOC) {
        refuseFurtherComments();
        this.originatorPOC = originatorPOC;
    }

    /** Get the position of Programmatic Point Of Contact at originator.
     * @return position of Programmatic Point Of Contact at originator
     */
    public String getOriginatorPosition() {
        return originatorPosition;
    }

    /** Set the position of Programmatic Point Of Contact at originator.
     * @param originatorPosition position of Programmatic Point Of Contact at originator
     */
    public void setOriginatorPosition(final String originatorPosition) {
        refuseFurtherComments();
        this.originatorPosition = originatorPosition;
    }

    /** Get the phone number of Programmatic Point Of Contact at originator.
     * @return phone number of Programmatic Point Of Contact at originator
     */
    public String getOriginatorPhone() {
        return originatorPhone;
    }

    /** Set the phone number of Programmatic Point Of Contact at originator.
     * @param originatorPhone phone number of Programmatic Point Of Contact at originator
     */
    public void setOriginatorPhone(final String originatorPhone) {
        refuseFurtherComments();
        this.originatorPhone = originatorPhone;
    }

    /** Get the email address of Programmatic Point Of Contact at originator.
     * @return email address of Programmatic Point Of Contact at originator
     * @since 11.2
     */
    public String getOriginatorEmail() {
        return originatorEmail;
    }

    /** Set the email address of Programmatic Point Of Contact at originator.
     * @param originatorEmail email address of Programmatic Point Of Contact at originator
     * @since 11.2
     */
    public void setOriginatorEmail(final String originatorEmail) {
        refuseFurtherComments();
        this.originatorEmail = originatorEmail;
    }

    /** Get the address of Programmatic Point Of Contact at originator.
     * @return address of Programmatic Point Of Contact at originator
     */
    public String getOriginatorAddress() {
        return originatorAddress;
    }

    /** Set the address of Programmatic Point Of Contact at originator.
     * @param originatorAddress address of Programmatic Point Of Contact at originator
     */
    public void setOriginatorAddress(final String originatorAddress) {
        refuseFurtherComments();
        this.originatorAddress = originatorAddress;
    }

    /** Get the creating agency or operator.
     * @return creating agency or operator
     */
    public String getTechOrg() {
        return techOrg;
    }

    /** Set the creating agency or operator.
     * @param techOrg creating agency or operator
     */
    public void setTechOrg(final String techOrg) {
        refuseFurtherComments();
        this.techOrg = techOrg;
    }

    /** Get the Technical Point Of Contact at originator.
     * @return Technical Point Of Contact at originator
     */
    public String getTechPOC() {
        return techPOC;
    }

    /** Set the Technical Point Of Contact at originator.
     * @param techPOC Technical Point Of Contact at originator
     */
    public void setTechPOC(final String techPOC) {
        refuseFurtherComments();
        this.techPOC = techPOC;
    }

    /** Get the position of Technical Point Of Contact at originator.
     * @return position of Technical Point Of Contact at originator
     */
    public String getTechPosition() {
        return techPosition;
    }

    /** Set the position of Technical Point Of Contact at originator.
     * @param techPosition position of Technical Point Of Contact at originator
     */
    public void setTechPosition(final String techPosition) {
        refuseFurtherComments();
        this.techPosition = techPosition;
    }

    /** Get the phone number of Technical Point Of Contact at originator.
     * @return phone number of Technical Point Of Contact at originator
     */
    public String getTechPhone() {
        return techPhone;
    }

    /** Set the phone number of Technical Point Of Contact at originator.
     * @param techPhone phone number of Technical Point Of Contact at originator
     */
    public void setTechPhone(final String techPhone) {
        refuseFurtherComments();
        this.techPhone = techPhone;
    }

    /** Get the email address of Technical Point Of Contact at originator.
     * @return email address of Technical Point Of Contact at originator
     * @since 11.2
     */
    public String getTechEmail() {
        return techEmail;
    }

    /** Set the email address of Technical Point Of Contact at originator.
     * @param techEmail email address of Technical Point Of Contact at originator
     * @since 11.2
     */
    public void setTechEmail(final String techEmail) {
        refuseFurtherComments();
        this.techEmail = techEmail;
    }

    /** Get the address of Technical Point Of Contact at originator.
     * @return address of Technical Point Of Contact at originator
     */
    public String getTechAddress() {
        return techAddress;
    }

    /** Set the address of Technical Point Of Contact at originator.
     * @param techAddress address of Technical Point Of Contact at originator
     */
    public void setTechAddress(final String techAddress) {
        refuseFurtherComments();
        this.techAddress = techAddress;
    }

    /** Get the unique ID identifying previous message from a given originator.
     * @return unique ID identifying previous message from a given originator
     */
    public String getPreviousMessageID() {
        return previousMessageID;
    }

    /** Set the unique ID identifying previous message from a given originator.
     * @param previousMessageID unique ID identifying previous message from a given originator
     */
    public void setPreviousMessageID(final String previousMessageID) {
        refuseFurtherComments();
        this.previousMessageID = previousMessageID;
    }

    /** Get the unique ID identifying next message from a given originator.
     * @return unique ID identifying next message from a given originator
     */
    public String getNextMessageID() {
        return nextMessageID;
    }

    /** Set the unique ID identifying next message from a given originator.
     * @param nextMessageID unique ID identifying next message from a given originator
     */
    public void setNextMessageID(final String nextMessageID) {
        refuseFurtherComments();
        this.nextMessageID = nextMessageID;
    }

    /** Get the Unique identifier of Attitude Data Message linked to this Orbit Data Message.
     * @return Unique identifier of Attitude Data Message linked to this Orbit Data Message
     */
    public String getAdmMessageLink() {
        return admMessageLink;
    }

    /** Set the Unique identifier of Attitude Data Message linked to this Orbit Data Message.
     * @param admMessageLink Unique identifier of Attitude Data Message linked to this Orbit Data Message
     */
    public void setAdmMessageLink(final String admMessageLink) {
        refuseFurtherComments();
        this.admMessageLink = admMessageLink;
    }

    /** Get the Unique identifier of Conjunction Data Message linked to this Orbit Data Message.
     * @return Unique identifier of Conjunction Data Message linked to this Orbit Data Message
     */
    public String getCdmMessageLink() {
        return cdmMessageLink;
    }

    /** Set the Unique identifier of Conjunction Data Message linked to this Orbit Data Message.
     * @param cdmMessageLink Unique identifier of Conjunction Data Message linked to this Orbit Data Message
     */
    public void setCdmMessageLink(final String cdmMessageLink) {
        refuseFurtherComments();
        this.cdmMessageLink = cdmMessageLink;
    }

    /** Get the Unique identifier of Pointing Request Message linked to this Orbit Data Message.
     * @return Unique identifier of Pointing Request Message linked to this Orbit Data Message
     */
    public String getPrmMessageLink() {
        return prmMessageLink;
    }

    /** Set the Unique identifier of Pointing Request Message linked to this Orbit Data Message.
     * @param prmMessageLink Unique identifier of Pointing Request Message linked to this Orbit Data Message
     */
    public void setPrmMessageLink(final String prmMessageLink) {
        refuseFurtherComments();
        this.prmMessageLink = prmMessageLink;
    }

    /** Get the Unique identifier of Reentry Data Message linked to this Orbit Data Message.
     * @return Unique identifier of Reentry Data Message linked to this Orbit Data Message
     */
    public String getRdmMessageLink() {
        return rdmMessageLink;
    }

    /** Set the Unique identifier of Reentry Data Message linked to this Orbit Data Message.
     * @param rdmMessageLink Unique identifier of Reentry Data Message linked to this Orbit Data Message
     */
    public void setRdmMessageLink(final String rdmMessageLink) {
        refuseFurtherComments();
        this.rdmMessageLink = rdmMessageLink;
    }

    /** Get the Unique identifier of Tracking Data Message linked to this Orbit Data Message.
     * @return Unique identifier of Tracking Data Message linked to this Orbit Data Message
     */
    public String getTdmMessageLink() {
        return tdmMessageLink;
    }

    /** Set the Unique identifier of Tracking Data Message linked to this Orbit Data Message.
     * @param tdmMessageLink Unique identifier of Tracking Data Message linked to this Orbit Data Message
     */
    public void setTdmMessageLink(final String tdmMessageLink) {
        refuseFurtherComments();
        this.tdmMessageLink = tdmMessageLink;
    }

    /** Get the operator of the space object.
     * @return operator of the space object
     */
    public String getOperator() {
        return operator;
    }

    /** Set the operator of the space object.
     * @param operator operator of the space object
     */
    public void setOperator(final String operator) {
        refuseFurtherComments();
        this.operator = operator;
    }

    /** Get the owner of the space object.
     * @return owner of the space object
     */
    public String getOwner() {
        return owner;
    }

    /** Set the owner of the space object.
     * @param owner owner of the space object
     */
    public void setOwner(final String owner) {
        refuseFurtherComments();
        this.owner = owner;
    }

    /** Get the name of the country where the space object owner is based.
     * @return name of the country where the space object owner is based
     */
    public String getCountry() {
        return country;
    }

    /** Set the name of the country where the space object owner is based.
     * @param country name of the country where the space object owner is based
     */
    public void setCountry(final String country) {
        refuseFurtherComments();
        this.country = country;
    }

    /** Get the name of the constellation this space object belongs to.
     * @return name of the constellation this space object belongs to
     */
    public String getConstellation() {
        return constellation;
    }

    /** Set the name of the constellation this space object belongs to.
     * @param constellation name of the constellation this space object belongs to
     */
    public void setConstellation(final String constellation) {
        refuseFurtherComments();
        this.constellation = constellation;
    }

    /** Get the type of object.
     * @return type of object
     */
    public ObjectType getObjectType() {
        return objectType;
    }

    /** Set the type of object.
     * @param objectType type of object
     */
    public void setObjectType(final ObjectType objectType) {
        refuseFurtherComments();
        this.objectType = objectType;
    }

    /** Get the epoch to which <em>all</em> relative times are referenced in data blocks.
     * @return epoch to which <em>all</em> relative times are referenced in data blocks
     */
    public AbsoluteDate getEpochT0() {
        return epochT0;
    }

    /** Set the epoch to which <em>all</em> relative times are referenced in data blocks.
     * @param epochT0 epoch to which <em>all</em> relative times are referenced in data blocks
     */
    public void setEpochT0(final AbsoluteDate epochT0) {
        refuseFurtherComments();
        this.epochT0 = epochT0;
    }

    /** Get the operational status.
     * @return operational status
     */
    public OpsStatus getOpsStatus() {
        return opsStatus;
    }

    /** Set the operational status.
     * @param opsStatus operational status
     */
    public void setOpsStatus(final OpsStatus opsStatus) {
        refuseFurtherComments();
        this.opsStatus = opsStatus;
    }

    /** Get the orbit category.
     * @return orbit category
     */
    public OrbitCategory getOrbitCategory() {
        return orbitCategory;
    }

    /** Set the orbit category.
     * @param orbitCategory orbit category
     */
    public void setOrbitCategory(final OrbitCategory orbitCategory) {
        refuseFurtherComments();
        this.orbitCategory = orbitCategory;
    }

    /** Get the list of elements of information data blocks included in this message.
     * @return list of elements of information data blocks included in this message
     */
    public List<OcmElements> getOcmDataElements() {
        return ocmDataElements;
    }

    /** Set the list of elements of information data blocks included in this message.
     * @param ocmDataElements list of elements of information data blocks included in this message
     */
    public void setOcmDataElements(final List<OcmElements> ocmDataElements) {
        refuseFurtherComments();
        this.ocmDataElements = ocmDataElements;
    }

    /** Get the spacecraft clock count at {@link #getEpochT0()}.
     * @return spacecraft clock count at {@link #getEpochT0()}
     */
    public double getSclkOffsetAtEpoch() {
        return sclkOffsetAtEpoch;
    }

    /** Set the spacecraft clock count at {@link #getEpochT0()}.
     * @param sclkOffsetAtEpoch spacecraft clock count at {@link #getEpochT0()}
     */
    public void setSclkOffsetAtEpoch(final double sclkOffsetAtEpoch) {
        refuseFurtherComments();
        this.sclkOffsetAtEpoch = sclkOffsetAtEpoch;
    }

    /** Get the number of spacecraft clock seconds occurring during one SI second.
     * @return number of spacecraft clock seconds occurring during one SI second
     */
    public double getSclkSecPerSISec() {
        return sclkSecPerSISec;
    }

    /** Set the number of spacecraft clock seconds occurring during one SI second.
     * @param secClockPerSISec number of spacecraft clock seconds occurring during one SI second
     */
    public void setSclkSecPerSISec(final double secClockPerSISec) {
        refuseFurtherComments();
        this.sclkSecPerSISec = secClockPerSISec;
    }

    /** Get the creation date of previous message from a given originator.
     * @return creation date of previous message from a given originator
     */
    public AbsoluteDate getPreviousMessageEpoch() {
        return previousMessageEpoch;
    }

    /** Set the creation date of previous message from a given originator.
     * @param previousMessageEpoch creation date of previous message from a given originator
     */
    public void setPreviousMessageEpoch(final AbsoluteDate previousMessageEpoch) {
        refuseFurtherComments();
        this.previousMessageEpoch = previousMessageEpoch;
    }

    /** Get the creation date of next message from a given originator.
     * @return creation date of next message from a given originator
     */
    public AbsoluteDate getNextMessageEpoch() {
        return nextMessageEpoch;
    }

    /** Set the creation date of next message from a given originator.
     * @param nextMessageEpoch creation date of next message from a given originator
     */
    public void setNextMessageEpoch(final AbsoluteDate nextMessageEpoch) {
        refuseFurtherComments();
        this.nextMessageEpoch = nextMessageEpoch;
    }

    /** Get the time of the earliest data contained in the OCM.
     * @return time of the earliest data contained in the OCM
     */
    public AbsoluteDate getStartTime() {
        return startTime;
    }

    /** Set the time of the earliest data contained in the OCM.
     * @param startTime time of the earliest data contained in the OCM
     */
    public void setStartTime(final AbsoluteDate startTime) {
        refuseFurtherComments();
        this.startTime = startTime;
    }

    /** Get the time of the latest data contained in the OCM.
     * @return time of the latest data contained in the OCM
     */
    public AbsoluteDate getStopTime() {
        return stopTime;
    }

    /** Set the time of the latest data contained in the OCM.
     * @param stopTime time of the latest data contained in the OCM
     */
    public void setStopTime(final AbsoluteDate stopTime) {
        refuseFurtherComments();
        this.stopTime = stopTime;
    }

    /** Get the span of time in seconds that the OCM covers.
     * @return span of time in seconds that the OCM covers
     */
    public double getTimeSpan() {
        return timeSpan;
    }

    /** Set the span of time in seconds that the OCM covers.
     * @param timeSpan span of time in seconds that the OCM covers
     */
    public void setTimeSpan(final double timeSpan) {
        refuseFurtherComments();
        this.timeSpan = timeSpan;
    }

    /** Get the difference (TAI – UTC) in seconds at epoch {@link #getEpochT0()}.
     * @return difference (TAI – UTC) in seconds at epoch {@link #getEpochT0()}
     */
    public double getTaimutcT0() {
        return taimutcT0;
    }

    /** Set the difference (TAI – UTC) in seconds at epoch {@link #getEpochT0()}.
     * @param taimutcT0 difference (TAI – UTC) in seconds at epoch {@link #getEpochT0()}
     */
    public void setTaimutcT0(final double taimutcT0) {
        refuseFurtherComments();
        this.taimutcT0 = taimutcT0;
    }

    /** Get the epoch of next leap second.
     * @return epoch of next leap second
     * @since 11.2
     */
    public AbsoluteDate getNextLeapEpoch() {
        return nextLeapEpoch;
    }

    /** Set the epoch of next leap second.
     * @param nextLeapEpoch epoch of next leap second
     * @since 11.2
     */
    public void setNextLeapEpoch(final AbsoluteDate nextLeapEpoch) {
        refuseFurtherComments();
        this.nextLeapEpoch = nextLeapEpoch;
    }

    /** Get the difference (TAI – UTC) in seconds incorporated at epoch {@link #getNextLeapEpoch()}.
     * @return difference (TAI – UTC) in seconds incorporated at epoch {@link #getNextLeapEpoch()}
     * @since 11.2
     */
    public double getNextLeapTaimutc() {
        return nextLeapTaimutc;
    }

    /** Set the difference (TAI – UTC) in seconds incorporated at epoch {@link #getNextLeapEpoch()}.
     * @param nextLeapTaimutc difference (TAI – UTC) in seconds incorporated at epoch {@link #getNextLeapEpoch()}
     * @since 11.2
     */
    public void setNextLeapTaimutc(final double nextLeapTaimutc) {
        refuseFurtherComments();
        this.nextLeapTaimutc = nextLeapTaimutc;
    }

    /** Get the difference (UT1 – UTC) in seconds at epoch {@link #getEpochT0()}.
     * @return difference (UT1 – UTC) in seconds at epoch {@link #getEpochT0()}
     */
    public double getUt1mutcT0() {
        return ut1mutcT0;
    }

    /** Set the difference (UT1 – UTC) in seconds at epoch {@link #getEpochT0()}.
     * @param ut1mutcT0 difference (UT1 – UTC) in seconds at epoch {@link #getEpochT0()}
     */
    public void setUt1mutcT0(final double ut1mutcT0) {
        refuseFurtherComments();
        this.ut1mutcT0 = ut1mutcT0;
    }

    /** Get the source and version of Earth Orientation Parameters.
     * @return source and version of Earth Orientation Parameters
     */
    public String getEopSource() {
        return eopSource;
    }

    /** Set the source and version of Earth Orientation Parameters.
     * @param eopSource source and version of Earth Orientation Parameters
     */
    public void setEopSource(final String eopSource) {
        refuseFurtherComments();
        this.eopSource = eopSource;
    }

    /** Get the interpolation method for Earth Orientation Parameters.
     * @return interpolation method for Earth Orientation Parameters
     */
    public String getInterpMethodEOP() {
        return interpMethodEOP;
    }

    /** Set the interpolation method for Earth Orientation Parameters.
     * @param interpMethodEOP interpolation method for Earth Orientation Parameters
     */
    public void setInterpMethodEOP(final String interpMethodEOP) {
        refuseFurtherComments();
        this.interpMethodEOP = interpMethodEOP;
    }

    /** Get the source and version of celestial body (e.g. Sun/Earth/Planetary).
     * @return source and version of celestial body (e.g. Sun/Earth/Planetary)
     */
    public String getCelestialSource() {
        return celestialSource;
    }

    /** Set the source and version of celestial body (e.g. Sun/Earth/Planetary).
     * @param celestialSource source and version of celestial body (e.g. Sun/Earth/Planetary)
     */
    public void setCelestialSource(final String celestialSource) {
        refuseFurtherComments();
        this.celestialSource = celestialSource;
    }

    /** Copy the instance, making sure mandatory fields have been initialized.
     * <p>
     * Message ID, previous/next references, start and stop times are not copied.
     * </p>
     * @param version format version
     * @return a new copy
     * @since 12.0
     */
    public OcmMetadata copy(final double version) {

        validate(version);

        // allocate new instance
        final OcmMetadata copy = new OcmMetadata(dataContext);

        // copy comments
        for (String comment : getComments()) {
            copy.addComment(comment);
        }

        // copy metadata
        copy.setInternationalDesignator(getInternationalDesignator());
        copy.setCatalogName(getCatalogName());
        copy.setObjectDesignator(getObjectDesignator());
        copy.setAlternateNames(getAlternateNames());
        copy.setOriginatorPOC(getOriginatorPOC());
        copy.setOriginatorPosition(getOriginatorPosition());
        copy.setOriginatorPhone(getOriginatorPhone());
        copy.setOriginatorEmail(getOriginatorEmail());
        copy.setOriginatorAddress(getOriginatorAddress());
        copy.setTechOrg(getTechOrg());
        copy.setTechPOC(getTechPOC());
        copy.setTechPosition(getTechPosition());
        copy.setTechPhone(getTechPhone());
        copy.setTechEmail(getTechEmail());
        copy.setTechAddress(getTechAddress());
        copy.setAdmMessageLink(getAdmMessageLink());
        copy.setCdmMessageLink(getCdmMessageLink());
        copy.setPrmMessageLink(getPrmMessageLink());
        copy.setRdmMessageLink(getRdmMessageLink());
        copy.setTdmMessageLink(getTdmMessageLink());
        copy.setOperator(getOperator());
        copy.setOwner(getOwner());
        copy.setCountry(getCountry());
        copy.setConstellation(getConstellation());
        copy.setObjectType(getObjectType());
        copy.setEpochT0(getEpochT0());
        copy.setOpsStatus(getOpsStatus());
        copy.setOrbitCategory(getOrbitCategory());
        copy.setOcmDataElements(getOcmDataElements());
        copy.setSclkOffsetAtEpoch(getSclkOffsetAtEpoch());
        copy.setSclkSecPerSISec(getSclkSecPerSISec());
        copy.setTaimutcT0(getTaimutcT0());
        copy.setNextLeapEpoch(getNextLeapEpoch());
        copy.setNextLeapTaimutc(getNextLeapTaimutc());
        copy.setUt1mutcT0(getUt1mutcT0());
        copy.setEopSource(getEopSource());
        copy.setInterpMethodEOP(getInterpMethodEOP());
        copy.setCelestialSource(getCelestialSource());

        return copy;

    }

}

/* Copyright 2002-2021 CS GROUP
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.orekit.files.ccsds.ndm.odm.ODMMetadata;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.time.AbsoluteDate;

/** Meta-data for {@link OCMMetadata Orbit Comprehensive Message}.
 * @since 11.0
 */
public class OCMMetadata extends ODMMetadata {

    /** Default interpolation method for EOP and Space Weather data. */
    private static final String DEFAULT_INTERPOLATION_METHOD = "LINEAR";

    /** Classification for this message. */
    private String classification;

    /** Alternate names for this space object. */
    private List<String> alternateNames;

    /** Unique satellite identification designator for the object. */
    private String objectDesignator;

    /** Programmatic Point Of Contact at originator. */
    private String originatorPOC;

    /** Position of Programmatic Point Of Contact at originator. */
    private String originatorPosition;

    /** Phone number of Programmatic Point Of Contact at originator. */
    private String originatorPhone;

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

    /** Address of Technical Point Of Contact at originator. */
    private String techAddress;

    /** Unique ID identifying previous message from a given originator. */
    private String previousMessageID;

    /** Unique ID identifying next message from a given originator. */
    private String nextMessageID;

    /** Names of Attitude Data Messages link to this Orbit Data Message. */
    private List<String> attMessageLink;

    /** Names of Conjunction Data Messages link to this Orbit Data Message. */
    private List<String> cdmMessageLink;

    /** Names of Pointing Request Messages link to this Orbit Data Message. */
    private List<String> prmMessageLink;

    /** Names of Reentry Data Messages link to this Orbit Data Message. */
    private List<String> rdmMessageLink;

    /** International designator for the object as assigned by the UN Committee
     * on Space Research (COSPAR) and the US National Space Science Data Center (NSSDC). */
    private String internationalDesignator;

    /** Operator of the space object. */
    private String operator;

    /** Owner of the space object. */
    private String owner;

    /** Name of the constellation this space object belongs to. */
    private String constellation;

    /** Name of the country where the space object owner is based. */
    private String country;

    /** Specification of satellite catalog source. */
    private String catalogName;

    /** Type of object. */
    private ObjectType objectType;

    /** Operational status. */
    private OpsStatus opsStatus;

    /** Orbit catgory. */
    private OrbitCategory orbitCategory;

    /** List of elements of information data blocks included in this message. */
    private List<String> ocmDataElements;

    /** Epoch to which <em>all</em> relative times are referenced in data blocks;
     * unless overridden by block-specific {@code EPOCH_TZERO} values. */
    private AbsoluteDate epochT0;

    /** Epoch corresponding to t=0 for the spacecraft clock. */
    private AbsoluteDate sclkEpoch;

    /** Number of clock seconds occurring during one SI second. */
    private double clockSecPerSISec;

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

    /** Difference (UT1 – UTC) in seconds at epoch {@link #epochT0}. */
    private double ut1mutcT0;

    /** Source and version of Earth Orientation Parameters. */
    private String eopSource;

    /** Interpolation method for Earth Orientation Parameters. */
    private String interpMethodEOP;

    /** Interpolation method for Space Weather data. */
    private String interpMethodSW;

    /** Source and version of celestial body (e.g. Sun/Earth/Planetary). */
    private String celestialSource;

    /** Create a new meta-data.
     */
    OCMMetadata() {

        // set up the few fields that have default values as per CCSDS standard
        setTimeSystem(CcsdsTimeScale.UTC);
        catalogName      = "CSPOC";
        clockSecPerSISec = 1.0;
        interpMethodEOP  = DEFAULT_INTERPOLATION_METHOD;
        interpMethodSW   = DEFAULT_INTERPOLATION_METHOD;

    }

    /** Get the message classification.
     * @return message classification.
     */
    public String getClassification() {
        return classification;
    }

    /** Set the message classification.
     * @param classification message classification
     */
    void setClassification(final String classification) {
        refuseFurtherComments();
        this.classification = classification;
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

    /** Get the unique satellite identification designator for the object.
     * @return unique satellite identification designator for the object.
     */
    public String getObjectDesignator() {
        return objectDesignator;
    }

    /** Set the unique satellite identification designator for the object.
     * @param objectDesignator unique satellite identification designator for the object
     */
    void setObjectDesignator(final String objectDesignator) {
        refuseFurtherComments();
        this.objectDesignator = objectDesignator;
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
    void setOriginatorPOC(final String originatorPOC) {
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
    void setOriginatorPosition(final String originatorPosition) {
        refuseFurtherComments();
        this.originatorPosition = originatorPosition;
    }

    /** Get the phone number of Programmatic Point Of Contact at originator.
     * @return phone number of Programmatic Point Of Contact at originator
     */
    public String getOriginatorPhone() {
        return originatorPhone;
    }

    /** GSet the phone number of Programmatic Point Of Contact at originator.
     * @param originatorPhone phone number of Programmatic Point Of Contact at originator
     */
    void setOriginatorPhone(final String originatorPhone) {
        refuseFurtherComments();
        this.originatorPhone = originatorPhone;
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
    void setOriginatorAddress(final String originatorAddress) {
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
    void setTechOrg(final String techOrg) {
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
    void setTechPOC(final String techPOC) {
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
    void setTechPosition(final String techPosition) {
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
    void setTechPhone(final String techPhone) {
        refuseFurtherComments();
        this.techPhone = techPhone;
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
    void setTechAddress(final String techAddress) {
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
    void setPreviousMessageID(final String previousMessageID) {
        refuseFurtherComments();
        this.previousMessageID = previousMessageID;
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
    void setPreviousMessageEpoch(final AbsoluteDate previousMessageEpoch) {
        refuseFurtherComments();
        this.previousMessageEpoch = previousMessageEpoch;
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
    void setNextMessageID(final String nextMessageID) {
        refuseFurtherComments();
        this.nextMessageID = nextMessageID;
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
    void setNextMessageEpoch(final AbsoluteDate nextMessageEpoch) {
        refuseFurtherComments();
        this.nextMessageEpoch = nextMessageEpoch;
    }

    /** Get the names of Attitude Data Messages link to this Orbit Data Message.
     * @return names of Attitude Data Messages link to this Orbit Data Message
     */
    public List<String> getAttMessageLink() {
        return attMessageLink;
    }

    /** Set the names of Attitude Data Messages link to this Orbit Data Message.
     * @param attMessageLink names of Attitude Data Messages link to this Orbit Data Message
     */
    void setAttMessageLink(final List<String> attMessageLink) {
        refuseFurtherComments();
        this.attMessageLink = attMessageLink;
    }

    /** Get the names of Conjunction Data Messages link to this Orbit Data Message.
     * @return names of Conjunction Data Messages link to this Orbit Data Message
     */
    public List<String> getCdmMessageLink() {
        return cdmMessageLink;
    }

    /** Set the names of Conjunction Data Messages link to this Orbit Data Message.
     * @param cdmMessageLink names of Conjunction Data Messages link to this Orbit Data Message
     */
    void setCdmMessageLink(final List<String> cdmMessageLink) {
        refuseFurtherComments();
        this.cdmMessageLink = cdmMessageLink;
    }

    /** Get the names of Pointing Request Messages link to this Orbit Data Message.
     * @return names of Pointing Request Messages link to this Orbit Data Message
     */
    public List<String> getPrmMessageLink() {
        return prmMessageLink;
    }

    /** Set the names of Pointing Request Messages link to this Orbit Data Message.
     * @param prmMessageLink names of Pointing Request Messages link to this Orbit Data Message
     */
    void setPrmMessageLink(final List<String> prmMessageLink) {
        refuseFurtherComments();
        this.prmMessageLink = prmMessageLink;
    }

    /** Get the names of Reentry Data Messages link to this Orbit Data Message.
     * @return names of Reentry Data Messages link to this Orbit Data Message
     */
    public List<String> getRdmMessageLink() {
        return rdmMessageLink;
    }

    /** Set the names of Reentry Data Messages link to this Orbit Data Message.
     * @param rdmMessageLink names of Reentry Data Messages link to this Orbit Data Message
     */
    void setRdmMessageLink(final List<String> rdmMessageLink) {
        refuseFurtherComments();
        this.rdmMessageLink = rdmMessageLink;
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
    void setInternationalDesignator(final String internationalDesignator) {
        refuseFurtherComments();
        this.internationalDesignator = internationalDesignator;
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
    void setOperator(final String operator) {
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
    void setOwner(final String owner) {
        refuseFurtherComments();
        this.owner = owner;
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
    void setConstellation(final String constellation) {
        refuseFurtherComments();
        this.constellation = constellation;
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
    void setCountry(final String country) {
        refuseFurtherComments();
        this.country = country;
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
    void setCatalogName(final String catalogName) {
        refuseFurtherComments();
        this.catalogName = catalogName;
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
    void setObjectType(final ObjectType objectType) {
        refuseFurtherComments();
        this.objectType = objectType;
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
    void setOpsStatus(final OpsStatus opsStatus) {
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
    void setOrbitCategory(final OrbitCategory orbitCategory) {
        refuseFurtherComments();
        this.orbitCategory = orbitCategory;
    }

    /** Get the list of elements of information data blocks included in this message.
     * @return list of elements of information data blocks included in this message
     */
    public List<String> getOcmDataElements() {
        return ocmDataElements;
    }

    /** Set the list of elements of information data blocks included in this message.
     * @param ocmDataElements list of elements of information data blocks included in this message
     */
    void setOcmDataElements(final List<String> ocmDataElements) {
        refuseFurtherComments();
        this.ocmDataElements = ocmDataElements;
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
    void setEpochT0(final AbsoluteDate epochT0) {
        refuseFurtherComments();
        this.epochT0 = epochT0;
    }

    /** Get the epoch corresponding to t=0 for the spacecraft clock.
     * @return epoch corresponding to t=0 for the spacecraft clock
     */
    public AbsoluteDate getSclkEpoch() {
        return sclkEpoch;
    }

    /** Set the epoch corresponding to t=0 for the spacecraft clock.
     * @param sclkEpoch epoch corresponding to t=0 for the spacecraft clock
     */
    void setSclkEpoch(final AbsoluteDate sclkEpoch) {
        refuseFurtherComments();
        this.sclkEpoch = sclkEpoch;
    }

    /** Get the number of clock seconds occurring during one SI second.
     * @return number of clock seconds occurring during one SI second
     */
    public double getClockSecPerSISec() {
        return clockSecPerSISec;
    }

    /** Set the number of clock seconds occurring during one SI second.
     * @param secClockPerSISec number of clock seconds occurring during one SI second
     */
    void setClockSecPerSISec(final double secClockPerSISec) {
        refuseFurtherComments();
        this.clockSecPerSISec = secClockPerSISec;
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
    void setStartTime(final AbsoluteDate startTime) {
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
    void setStopTime(final AbsoluteDate stopTime) {
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
    void setTimeSpan(final double timeSpan) {
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
    void setTaimutcT0(final double taimutcT0) {
        refuseFurtherComments();
        this.taimutcT0 = taimutcT0;
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
    void setUt1mutcT0(final double ut1mutcT0) {
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
    void setEopSource(final String eopSource) {
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
    void setInterpMethodEOP(final String interpMethodEOP) {
        refuseFurtherComments();
        this.interpMethodEOP = interpMethodEOP;
    }

    /** Get the interpolation method for Space Weather data.
     * @return interpolation method for Space Weather data
     */
    public String getInterpMethodSW() {
        return interpMethodSW;
    }

    /** Set the interpolation method for Space Weather data.
     * @param interpMethodSW interpolation method for Space Weather data
     */
    void setInterpMethodSW(final String interpMethodSW) {
        refuseFurtherComments();
        this.interpMethodSW = interpMethodSW;
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
    void setCelestialSource(final String celestialSource) {
        refuseFurtherComments();
        this.celestialSource = celestialSource;
    }

}

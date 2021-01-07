/* Copyright 2002-2019 CS Systèmes d'Information
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.orekit.files.ccsds.ndm.NDMSegment;
import org.orekit.files.ccsds.ndm.odm.ODMFile;
import org.orekit.files.ccsds.ndm.odm.ODMMetadata;
import org.orekit.files.ccsds.utils.CCSDSFrame;
import org.orekit.files.ccsds.utils.CCSDSUnit;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;
import org.orekit.utils.Constants;

/** Meta-data for {@link OCMMetadata Orbit Comprehensive Message}.
 * @since 11.0
 */
public class OCMMetadata extends ODMMetadata {

    /** Classification for this message. */
    private String classification;

    /** Alternate names for this space object. */
    private List<String> alternateNames;

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
    private String prevMessageID;

    /** Creation date of previous message from a given originator. */
    private String prevMessageEpoch;

    /** Unique ID identifying next message from a given originator. */
    private String nextMessageID;

    /** Creation date of next message from a given originator. */
    private String nextMessageEpoch;

    /** Names of Attitude Data Messages link to this Orbit Data Message. */
    private List<String> attMessageLink;

    /** Names of Conjunction Data Messages link to this Orbit Data Message. */
    private List<String> cdmMessageLink;

    /** Names of Pointing Request Messages link to this Orbit Data Message. */
    private List<String> prmMessageLink;

    /** Names of Reentry Data Messages link to this Orbit Data Message. */
    private List<String> rdmMessageLink;

    /** Names of Tracking Data Messages link to this Orbit Data Message. */
    private List<String> tdmMessageLink;

    /** International designator for the object as assigned by the UN Committee
     * on Space Research (COSPAR) and the US National Space Science Data Center (NSSDC). */
    private String internationalDesignator;

    /** Operator of the space object. */
    private String operator;

    /** Owner of the space object. */
    private String owner;

    /** Name of the space object mission. */
    private String mission;

    /** Name of the constellation this space object belongs to. */
    private String constellation;

    /** Epoch of initial launch. */
    private String launchEpoch;

    /** Country of launch. */
    private String launchCountry;

    /** Launch site. */
    private String launchSite;

    /** Launch provider. */
    private String launchProvider;

    /** Integrator of launch. */
    private String launchIntegrator;

    /** Launch pad. */
    private String launchPad;

    /** Launch platform. */
    private String launchPlatform;

    /** Epoch of the <em>most recent</em> deployement of this space object in the parent/child deployement sequence. */
    private String releaseEpoch;

    /** Epoch of the beginning of mission operations. */
    private String missionStartEpoch;

    /** Epoch of the cessation of mission operations. */
    private String missionEndEpoch;

    /** Epoch (actual or estimated) of the space object reentry. */
    private String reentryEpoch;

    /** Estimated remaining lifetime in days. */
    private double lifetime;

    /** Specification of satellite catalog source. */
    private String catalogName;

    /** Type of object. */
    private ObjectType objectType;

    /** Operational status. */
    private OpsStatus opsStatus;

    /** Orbit type. */
    private OrbitType orbitType;

    /** List of elements of information data blocks included in this message. */
    private List<String> ocmDataElements;

    /** Epoch to which <em>all</em> relative times are referenced in data blocks;
     * unless overridden by block-specific {@code EPOCH_TZERO} values. */
    private String epochT0;

    /** Number of clock seconds occurring during one SI second. */
    private double secClockPerSISecond;

    /** Number of SI seconds in the chosen central body’s “day”. */
    private double secPerDay;

    /** Time of the earliest data contained in the OCM. */
    private String earliestTime;

    /** Time of the latest data contained in the OCM. */
    private String latestTime;

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

    /** Create a new meta-data.
     * @param ocmFile OCM file to which these meta-data belongs
     */
    OCMMetadata() {

        // set up the few fields that have default values as per CCSDS standard
        catalogName         = "CSPOC";
        secClockPerSISecond = 1.0;
        secPerDay           = Constants.JULIAN_DAY;
        interpMethodEOP     = "LINEAR";

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
    void setOriginatorPOC(final String originatorPOC) {
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
        this.techAddress = techAddress;
    }

    /** Get the unique ID identifying previous message from a given originator.
     * @return unique ID identifying previous message from a given originator
     */
    public String getPrevMessageID() {
        return prevMessageID;
    }

    /** Set the unique ID identifying previous message from a given originator.
     * @param prevMessageID unique ID identifying previous message from a given originator
     */
    void setPrevMessageID(final String prevMessageID) {
        this.prevMessageID = prevMessageID;
    }

    /** Get the creation date of previous message from a given originator.
     * @return creation date of previous message from a given originator
     */
    public AbsoluteDate getPrevMessageEpoch() {
        return absoluteToEpoch(prevMessageEpoch);
    }

    /** Set the creation date of previous message from a given originator.
     * @param prevMessageEpoch creation date of previous message from a given originator
     */
    void setPrevMessageEpoch(final String prevMessageEpoch) {
        this.prevMessageEpoch = prevMessageEpoch;
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
        this.nextMessageID = nextMessageID;
    }

    /** Get the creation date of next message from a given originator.
     * @return creation date of next message from a given originator
     */
    public AbsoluteDate getNextMessageEpoch() {
        return absoluteToEpoch(nextMessageEpoch);
    }

    /** Set the creation date of next message from a given originator.
     * @param nextMessageEpoch creation date of next message from a given originator
     */
    void setNextMessageEpoch(final String nextMessageEpoch) {
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
        this.rdmMessageLink = rdmMessageLink;
    }

    /** Get the names of Tracking Data Messages link to this Orbit Data Message.
     * @return names of Tracking Data Messages link to this Orbit Data Message
     */
    public List<String> getTdmMessageLink() {
        return tdmMessageLink;
    }

    /** Set the names of Tracking Data Messages link to this Orbit Data Message.
     * @param tdmMessageLink names of Tracking Data Messages link to this Orbit Data Message
     */
    void setTdmMessageLink(final List<String> tdmMessageLink) {
        this.tdmMessageLink = tdmMessageLink;
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
        this.owner = owner;
    }

    /** Get the name of the space object mission.
     * @return name of the space object mission
     */
    public String getMission() {
        return mission;
    }

    /** Set the name of the space object mission.
     * @param mission name of the space object mission
     */
    void setMission(final String mission) {
        this.mission = mission;
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
        this.constellation = constellation;
    }

    /** Get the epoch of initial launch.
     * @return epoch of initial launch
     */
    public AbsoluteDate getLaunchEpoch() {
        return absoluteToEpoch(launchEpoch);
    }

    /** Set the epoch of initial launch.
     * @param launchEpoch epoch of initial launch
     */
    void setLaunchEpoch(final String launchEpoch) {
        this.launchEpoch = launchEpoch;
    }

    /** Get the country of launch.
     * @return country of launch
     */
    public String getLaunchCountry() {
        return launchCountry;
    }

    /** Set the country of launch.
     * @param launchCountry country of launch
     */
    void setLaunchCountry(final String launchCountry) {
        this.launchCountry = launchCountry;
    }

    /** Get the launch site.
     * @return launch site
     */
    public String getLaunchSite() {
        return launchSite;
    }

    /** Set the launch site.
     * @param launchSite launch site
     */
    void setLaunchSite(final String launchSite) {
        this.launchSite = launchSite;
    }

    /** Get the launch provider.
     * @return launch provider
     */
    public String getLaunchProvider() {
        return launchProvider;
    }

    /** Set the launch provider.
     * @param launchProvider launch provider
     */
    void setLaunchProvider(final String launchProvider) {
        this.launchProvider = launchProvider;
    }

    /** Get the integrator of launch.
     * @return integrator of launch
     */
    public String getLaunchIntegrator() {
        return launchIntegrator;
    }

    /** Set the integrator of launch.
     * @param launchIntegrator integrator of launch
     */
    void setLaunchIntegrator(final String launchIntegrator) {
        this.launchIntegrator = launchIntegrator;
    }

    /** Get the launch pad.
     * @return launch pad
     */
    public String getLaunchPad() {
        return launchPad;
    }

    /** Set the launch pad.
     * @param launchPad launch pad
     */
    void setLaunchPad(final String launchPad) {
        this.launchPad = launchPad;
    }

    /** Get the launch platform.
     * @return launch platform
     */
    public String getLaunchPlatform() {
        return launchPlatform;
    }

    /** Set the launch platform.
     * @param launchPlatform launch platform
     */
    void setLaunchPlatform(final String launchPlatform) {
        this.launchPlatform = launchPlatform;
    }

    /** Get the epoch of the <em>most recent</em> deployement of this space object.
     * @return epoch of the <em>most recent</em> deployement of this space object
     */
    public AbsoluteDate getReleaseEpoch() {
        return absoluteToEpoch(releaseEpoch);
    }

    /** Set the epoch of the <em>most recent</em> deployement of this space object.
     * @param releaseEpoch epoch of the <em>most recent</em> deployement of this space object
     */
    void setReleaseEpoch(final String releaseEpoch) {
        this.releaseEpoch = releaseEpoch;
    }

    /** Get the epoch of the beginning of mission operations.
     * @return epoch of the beginning of mission operations
     */
    public AbsoluteDate getMissionStartEpoch() {
        return absoluteToEpoch(missionStartEpoch);
    }

    /** Set the epoch of the beginning of mission operations.
     * @param missionStartEpoch epoch of the beginning of mission operations
     */
    void setMissionStartEpoch(final String missionStartEpoch) {
        this.missionStartEpoch = missionStartEpoch;
    }

    /** Get the epoch of the cessation of mission operations.
     * @return epoch of the cessation of mission operations
     */
    public AbsoluteDate getMissionEndEpoch() {
        return absoluteToEpoch(missionEndEpoch);
    }

    /** Set the epoch of the cessation of mission operations.
     * @param missionEndEpoch epoch of the cessation of mission operations
     */
    void setMissionEndEpoch(final String missionEndEpoch) {
        this.missionEndEpoch = missionEndEpoch;
    }

    /** Get the epoch (actual or estimated) of the space object reentry.
     * @return epoch (actual or estimated) of the space object reentry
     */
    public AbsoluteDate getReentryEpoch() {
        return absoluteToEpoch(reentryEpoch);
    }

    /** Set the epoch (actual or estimated) of the space object reentry.
     * @param reentryEpoch epoch (actual or estimated) of the space object reentry
     */
    void setReentryEpoch(final String reentryEpoch) {
        this.reentryEpoch = reentryEpoch;
    }

    /** Get the estimated remaining lifetime in days.
     * @return estimated remaining lifetime in days
     */
    public double getLifetime() {
        return lifetime;
    }

    /** Set the estimated remaining lifetime in days.
     * @param lifetime estimated remaining lifetime in days
     */
    void setLifetime(final double lifetime) {
        this.lifetime = lifetime;
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
        this.opsStatus = opsStatus;
    }

    /** Get the orbit type.
     * @return orbit type
     */
    public OrbitType getOrbitType() {
        return orbitType;
    }

    /** Set the orbit type.
     * @param orbitType orbit type
     */
    void setOrbitType(final OrbitType orbitType) {
        this.orbitType = orbitType;
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
        this.ocmDataElements = ocmDataElements;
    }

    /** Get the epoch to which <em>all</em> relative times are referenced in data blocks.
     * @return epoch to which <em>all</em> relative times are referenced in data blocks
     */
    String getEpochT0String() {
        return epochT0;
    }

    /** Get the epoch to which <em>all</em> relative times are referenced in data blocks.
     * @return epoch to which <em>all</em> relative times are referenced in data blocks
     */
    public AbsoluteDate getEpochT0() {
        return absoluteToEpoch(epochT0);
    }

    /** Set the epoch to which <em>all</em> relative times are referenced in data blocks.
     * @param epochT0 epoch to which <em>all</em> relative times are referenced in data blocks
     */
    void setEpochT0(final String epochT0) {
        this.epochT0 = epochT0;
    }

    /** Get the number of clock seconds occurring during one SI second.
     * @return number of clock seconds occurring during one SI second
     */
    public double getSecClockPerSISecond() {
        return secClockPerSISecond;
    }

    /** Set the number of clock seconds occurring during one SI second.
     * @param secClockPerSISecond number of clock seconds occurring during one SI second
     */
    void setSecClockPerSISecond(final double secClockPerSISecond) {
        this.secClockPerSISecond = secClockPerSISecond;
    }

    /** Get the number of SI seconds in the chosen central body’s “day”.
     * @return number of SI seconds in the chosen central body’s “day”
     */
    public double getSecPerDay() {
        return secPerDay;
    }

    /** Set the number of SI seconds in the chosen central body’s “day”.
     * @param secPerDay number of SI seconds in the chosen central body’s “day”
     */
    void setSecPerDay(final double secPerDay) {
        this.secPerDay = secPerDay;
    }

    /** Get the time of the earliest data contained in the OCM.
     * @return time of the earliest data contained in the OCM
     */
    public AbsoluteDate getEarliestTime() {
        return absoluteOrRelativeToEpoch(earliestTime);
    }

    /** Set the time of the earliest data contained in the OCM.
     * @param earliestTime time of the earliest data contained in the OCM
     */
    void setEarliestTime(final String earliestTime) {
        this.earliestTime = earliestTime;
    }

    /** Get the time of the latest data contained in the OCM.
     * @return time of the latest data contained in the OCM
     */
    public AbsoluteDate getLatestTime() {
        return absoluteOrRelativeToEpoch(latestTime);
    }

    /** Set the time of the latest data contained in the OCM.
     * @param latestTime time of the latest data contained in the OCM
     */
    void setLatestTime(final String latestTime) {
        this.latestTime = latestTime;
    }

    /** Get the span of time that the OCM covers.
     * @return span of time that the OCM covers
     */
    public double getTimeSpan() {
        return timeSpan;
    }

    /** Set the span of time that the OCM covers.
     * @param timeSpan span of time that the OCM covers
     */
    void setTimeSpan(final double timeSpan) {
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
        this.interpMethodEOP = interpMethodEOP;
    }

    /** Convert a string to an epoch.
     * @param value string to convert
     * @return converted epoch
     */
    private AbsoluteDate absoluteToEpoch(final String value) {
        return getTimeSystem().parseDate(value,
                                         getODMFile().getConventions(),
                                         getODMFile().getMissionReferenceDate());
    }

    /** Convert a string to an epoch.
     * @param value string to convert
     * @return converted epoch
     */
    private AbsoluteDate absoluteOrRelativeToEpoch(final String value) {
        if (value.contains("T")) {
            // absolute date
            return getTimeSystem().parseDate(value,
                                             getODMFile().getConventions(),
                                             getODMFile().getMissionReferenceDate());
        } else {
            // relative date
            return getEpochT0().shiftedBy(Double.parseDouble(value));
        }
    }

}

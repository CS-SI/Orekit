/* Copyright 2023 Luc Maisonobe
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

package org.orekit.files.ccsds.ndm.adm.acm;

import java.util.List;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.adm.AdmMetadata;
import org.orekit.files.ccsds.ndm.adm.AdmMetadataKey;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.time.AbsoluteDate;

/** Meta-data for {@link AcmMetadata Attitude Comprehensive Message}.
 * @since 12.0
 */
public class AcmMetadata extends AdmMetadata {

    /** Specification of satellite catalog source. */
    private String catalogName;

    /** Unique satellite identification designator for the object. */
    private String objectDesignator;

    /** Programmatic Point Of Contact at originator. */
    private String originatorPOC;

    /** Position of Programmatic Point Of Contact at originator. */
    private String originatorPosition;

    /** Phone number of Programmatic Point Of Contact at originator. */
    private String originatorPhone;

    /** Email address of Programmatic Point Of Contact at originator. */
    private String originatorEmail;

    /** Address of Programmatic Point Of Contact at originator. */
    private String originatorAddress;

    /** Unique identifier of Orbit Data Message linked to this Orbit Data Message. */
    private String odmMessageLink;

    /** Epoch to which <em>all</em> relative times are referenced in data blocks;
     * unless overridden by block-specific {@code EPOCH_TZERO} values. */
    private AbsoluteDate epochT0;

    /** List of elements of information data blocks included in this message. */
    private List<AcmElements> acmDataElements;

    /** Time of the earliest data contained in the OCM. */
    private AbsoluteDate startTime;

    /** Time of the latest data contained in the OCM. */
    private AbsoluteDate stopTime;

    /** Difference (TAI – UTC) in seconds at epoch {@link #epochT0}. */
    private double taimutcT0;

    /** Epoch of next leap second. */
    private AbsoluteDate nextLeapEpoch;

    /** Difference (TAI – UTC) in seconds incorporated at {@link #nextLeapEpoch}. */
    private double nextLeapTaimutc;

    /** Create a new meta-data.
     * @param dataContext data context
     */
    public AcmMetadata(final DataContext dataContext) {

        // set up the few fields that have default values as per CCSDS standard
        taimutcT0         = Double.NaN;
        nextLeapTaimutc   = Double.NaN;

    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        // we don't call super.checkMandatoryEntries() because
        // all of the parameters considered mandatory at ADM level
        // for APM and AEM are in fact optional in ACM
        // only OBJECT_NAME, TIME_SYSTEM and EPOCH_TZERO are mandatory
        checkNotNull(getObjectName(), AdmMetadataKey.OBJECT_NAME.name());
        checkNotNull(getTimeSystem(), MetadataKey.TIME_SYSTEM.name());
        checkNotNull(epochT0,         AcmMetadataKey.EPOCH_TZERO.name());
        if (nextLeapEpoch != null) {
            checkNotNaN(nextLeapTaimutc, AcmMetadataKey.NEXT_LEAP_TAIMUTC.name());
        }
    }

    /** Get the international designator for the object.
     * @return international designator for the object
     */
    public String getInternationalDesignator() {
        return getObjectID();
    }

    /** Set the international designator for the object.
     * @param internationalDesignator international designator for the object
     */
    public void setInternationalDesignator(final String internationalDesignator) {
        setObjectID(internationalDesignator);
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
     */
    public String getOriginatorEmail() {
        return originatorEmail;
    }

    /** Set the email address of Programmatic Point Of Contact at originator.
     * @param originatorEmail email address of Programmatic Point Of Contact at originator
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

    /** Get the Unique identifier of Orbit Data Message linked to this Attitude Data Message.
     * @return Unique identifier of Orbit Data Message linked to this Attitude Data Message
     */
    public String getOdmMessageLink() {
        return odmMessageLink;
    }

    /** Set the Unique identifier of Orbit Data Message linked to this Attitude Data Message.
     * @param odmMessageLink Unique identifier of Orbit Data Message linked to this Attitude Data Message
     */
    public void setOdmMessageLink(final String odmMessageLink) {
        refuseFurtherComments();
        this.odmMessageLink = odmMessageLink;
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

    /** Get the list of elements of information data blocks included in this message.
     * @return list of elements of information data blocks included in this message
     */
    public List<AcmElements> getAcmDataElements() {
        return acmDataElements;
    }

    /** Set the list of elements of information data blocks included in this message.
     * @param acmDataElements list of elements of information data blocks included in this message
     */
    public void setAcmDataElements(final List<AcmElements> acmDataElements) {
        refuseFurtherComments();
        this.acmDataElements = acmDataElements;
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
     */
    public AbsoluteDate getNextLeapEpoch() {
        return nextLeapEpoch;
    }

    /** Set the epoch of next leap second.
     * @param nextLeapEpoch epoch of next leap second
     */
    public void setNextLeapEpoch(final AbsoluteDate nextLeapEpoch) {
        refuseFurtherComments();
        this.nextLeapEpoch = nextLeapEpoch;
    }

    /** Get the difference (TAI – UTC) in seconds incorporated at epoch {@link #getNextLeapEpoch()}.
     * @return difference (TAI – UTC) in seconds incorporated at epoch {@link #getNextLeapEpoch()}
     */
    public double getNextLeapTaimutc() {
        return nextLeapTaimutc;
    }

    /** Set the difference (TAI – UTC) in seconds incorporated at epoch {@link #getNextLeapEpoch()}.
     * @param nextLeapTaimutc difference (TAI – UTC) in seconds incorporated at epoch {@link #getNextLeapEpoch()}
     */
    public void setNextLeapTaimutc(final double nextLeapTaimutc) {
        refuseFurtherComments();
        this.nextLeapTaimutc = nextLeapTaimutc;
    }

}

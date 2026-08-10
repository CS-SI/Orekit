/* Copyright 2022-2026 Luc Maisonobe
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.orekit.annotation.Nullable;
import org.orekit.files.ccsds.definitions.CcsdsFrameMapper;
import org.orekit.files.ccsds.ndm.adm.AdmMetadata;
import org.orekit.files.ccsds.ndm.adm.AdmMetadataKey;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/** Meta-data for {@link AcmMetadata Attitude Comprehensive Message}.
 * @since 12.0
 */
public class AcmMetadata extends AdmMetadata {

    /** Specification of satellite catalog source. */
    @Nullable
    private String catalogName;

    /** Unique satellite identification designator for the object. */
    @Nullable
    private String objectDesignator;

    /** Programmatic Point Of Contact at originator. */
    @Nullable
    private String originatorPOC;

    /** Position of Programmatic Point Of Contact at originator. */
    @Nullable
    private String originatorPosition;

    /** Phone number of Programmatic Point Of Contact at originator. */
    @Nullable
    private String originatorPhone;

    /** Email address of Programmatic Point Of Contact at originator. */
    @Nullable
    private String originatorEmail;

    /** Address of Programmatic Point Of Contact at originator. */
    @Nullable
    private String originatorAddress;

    /** Unique identifier of Orbit Data Message linked to this Orbit Data Message. */
    @Nullable
    private String odmMessageLink;

    /** Epoch to which <em>all</em> relative times are referenced in data blocks;
     * unless overridden by block-specific {@code EPOCH_TZERO} values. */
    private AbsoluteDate epochT0;

    /** List of elements of information data blocks included in this message. */
    private List<AcmElements> acmDataElements;

    /** Time of the earliest data contained in the OCM. */
    @Nullable
    private AbsoluteDate startTime;

    /** Time of the latest data contained in the OCM. */
    @Nullable
    private AbsoluteDate stopTime;

    /** Difference (TAI – UTC) in seconds at epoch {@link #epochT0}. */
    @Nullable
    private Double taimutcT0;

    /** Epoch of next leap second. */
    @Nullable
    private AbsoluteDate nextLeapEpoch;

    /** Difference (TAI – UTC) in seconds incorporated at {@link #nextLeapEpoch}. */
    @Nullable
    private Double nextLeapTaimutc;

    /**
     * Create a new meta-data.
     *
     * @param frameMapper for creating an Orekit {@link Frame}.
     * @since 13.1.5
     */
    public AcmMetadata(final CcsdsFrameMapper frameMapper) {
        super(frameMapper);
        acmDataElements = Collections.emptyList();
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
    public Optional<String> getCatalogName() {
        return Optional.ofNullable(catalogName);
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
    public Optional<String> getObjectDesignator() {
        return Optional.ofNullable(objectDesignator);
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
    public Optional<String> getOriginatorPOC() {
        return Optional.ofNullable(originatorPOC);
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
    public Optional<String> getOriginatorPosition() {
        return Optional.ofNullable(originatorPosition);
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
    public Optional<String> getOriginatorPhone() {
        return Optional.ofNullable(originatorPhone);
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
    public Optional<String> getOriginatorEmail() {
        return Optional.ofNullable(originatorEmail);
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
    public Optional<String> getOriginatorAddress() {
        return Optional.ofNullable(originatorAddress);
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
    public Optional<String> getOdmMessageLink() {
        return Optional.ofNullable(odmMessageLink);
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
    public Optional<AbsoluteDate> getStartTime() {
        return Optional.ofNullable(startTime);
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
    public Optional<AbsoluteDate> getStopTime() {
        return Optional.ofNullable(stopTime);
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
    public Optional<Double> getTaimutcT0() {
        return Optional.ofNullable(taimutcT0);
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
    public Optional<AbsoluteDate> getNextLeapEpoch() {
        return Optional.ofNullable(nextLeapEpoch);
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
    public Optional<Double> getNextLeapTaimutc() {
        return Optional.ofNullable(nextLeapTaimutc);
    }

    /** Set the difference (TAI – UTC) in seconds incorporated at epoch {@link #getNextLeapEpoch()}.
     * @param nextLeapTaimutc difference (TAI – UTC) in seconds incorporated at epoch {@link #getNextLeapEpoch()}
     */
    public void setNextLeapTaimutc(final double nextLeapTaimutc) {
        refuseFurtherComments();
        this.nextLeapTaimutc = nextLeapTaimutc;
    }

}

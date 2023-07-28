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
package org.orekit.files.ccsds.section;

import org.orekit.time.AbsoluteDate;

/**
 * Header of a CCSDS Navigation Data Message.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class Header extends CommentsContainer {

    /** CCSDS Format version. */
    private double formatVersion;

    /** Classification. */
    private String classification;

    /** Message creation date and time in UTC. */
    private AbsoluteDate creationDate;

    /** Creating agency or operator. */
    private String originator;

    /** ID that uniquely identifies a message from a given originator. */
    private String messageId;

    /** Minimum version for {@link HeaderKey#MESSAGE_ID}. */
    private final double minVersionMessageId;

    /** Minimum version for {@link HeaderKey#CLASSIFICATION}. */
    private final double minVersionClassification;

    /**
     * Constructor.
     * @param minVersionMessageId minimum version for {@link HeaderKey#MESSAGE_ID}
     * @param minVersionClassification minimum version for {@link HeaderKey#CLASSIFICATION}
     */
    public Header(final double minVersionMessageId,
                  final double minVersionClassification) {
        this.formatVersion            = Double.NaN;
        this.minVersionMessageId      = minVersionMessageId;
        this.minVersionClassification = minVersionClassification;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        checkNotNull(creationDate, HeaderKey.CREATION_DATE.name());
        checkNotNull(originator,   HeaderKey.ORIGINATOR.name());
        checkAllowed(version, messageId,      HeaderKey.MESSAGE_ID.name(),
                     minVersionMessageId, Double.POSITIVE_INFINITY);
        checkAllowed(version, classification, HeaderKey.CLASSIFICATION.name(),
                     minVersionClassification, Double.POSITIVE_INFINITY);
    }

    /**
     * Get the CCSDS NDM (ADM, ODM or TDM) format version.
     * @return format version
     */
    public double getFormatVersion() {
        return formatVersion;
    }

    /**
     * Set the CCSDS NDM (ADM, ODM or TDM) format version.
     * @param formatVersion the format version to be set
     */
    public void setFormatVersion(final double formatVersion) {
        this.formatVersion = formatVersion;
    }

    /**
     * Get the classification/caveats.
     * @return classification/caveats.
     */
    public String getClassification() {
        return classification;
    }

    /**
     * Set the classification/caveats.
     * @param classification classification/caveats to be set
     */
    public void setClassification(final String classification) {
        refuseFurtherComments();
        this.classification = classification;
    }

    /**
     * Get the message creation date and time in UTC.
     * @return the message creation date and time in UTC.
     */
    public AbsoluteDate getCreationDate() {
        return creationDate;
    }

    /**
     * Set the message creation date and time in UTC.
     * @param creationDate the creation date to be set
     */
    public void setCreationDate(final AbsoluteDate creationDate) {
        refuseFurtherComments();
        this.creationDate = creationDate;
    }

    /**
     * Get the message originator.
     * @return originator the message originator.
     */
    public String getOriginator() {
        return originator;
    }

    /**
     * Set the message originator.
     * @param originator the originator to be set
     */
    public void setOriginator(final String originator) {
        refuseFurtherComments();
        this.originator = originator;
    }

    /**
     * Get the ID that uniquely identifies a message from a given originator.
     * @return ID that uniquely identifies a message from a given originator
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Set the ID that uniquely identifies a message from a given originator.
     * @param messageId ID that uniquely identifies a message from a given originator
     */
    public void setMessageId(final String messageId) {
        this.messageId = messageId;
    }

}

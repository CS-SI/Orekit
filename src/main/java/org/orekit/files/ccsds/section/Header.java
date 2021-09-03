/* Copyright 2002-2021 CS GROUP
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

    /** File creation date and time in UTC. */
    private AbsoluteDate creationDate;

    /** Creating agency or operator. */
    private String originator;

    /** ID that uniquely identifies a message from a given originator. */
    private String messageId;

    /** Minimum version for {@link HeaderKey#MESSAGE_ID}. */
    private final double minVersionMessageId;

    /**
     * Constructor.
     * @param minVersionMessageId minimum version for {@link HeaderKey#MESSAGE_ID}
     */
    public Header(final double minVersionMessageId) {
        this.formatVersion       = Double.NaN;
        this.minVersionMessageId = minVersionMessageId;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        checkNotNull(creationDate, HeaderKey.CREATION_DATE);
        checkNotNull(originator,   HeaderKey.ORIGINATOR);
        checkAllowed(version, messageId, HeaderKey.MESSAGE_ID, minVersionMessageId, Double.POSITIVE_INFINITY);
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
     * Get the file creation date and time in UTC.
     * @return the file creation date and time in UTC.
     */
    public AbsoluteDate getCreationDate() {
        return creationDate;
    }

    /**
     * Set the file creation date and time in UTC.
     * @param creationDate the creation date to be set
     */
    public void setCreationDate(final AbsoluteDate creationDate) {
        refuseFurtherComments();
        this.creationDate = creationDate;
    }

    /**
     * Get the file originator.
     * @return originator the file originator.
     */
    public String getOriginator() {
        return originator;
    }

    /**
     * Set the file originator.
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

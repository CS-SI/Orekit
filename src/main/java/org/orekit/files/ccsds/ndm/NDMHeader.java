/* Copyright 2002-2020 CS GROUP
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
package org.orekit.files.ccsds.ndm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.orekit.time.AbsoluteDate;

/**
 * The NDMFile (Navigation Data Message) class represents the navigation
 * messages used by the CCSDS format, (i.e. the Attitude Data Message (ADM),
 * the Orbit Data Message (ODM) and the Tracking Data Message (TDM)).
 * It contains the information of the message's header.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class NDMHeader {

    /** CCSDS Format version. */
    private double formatVersion;

    /** Header comments. The list contains a string for each line of comment. */
    private List<String> comments;

    /** File creation date and time in UTC. */
    private AbsoluteDate creationDate;

    /** Creating agency or operator. */
    private String originator;

    /**
     * Constructor.
     */
    public NDMHeader() {
        // Initialise an empty comments list
        comments = new ArrayList<>();
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
     * Get the header comments.
     * @return header comments
     */
    public List<String> getComments() {
        return Collections.unmodifiableList(comments);
    }

    /**
     * Add header comment.
     * @param comment comment line
     */
    public void addComment(final String comment) {
        this.comments.add(comment);
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
        this.originator = originator;
    }

}

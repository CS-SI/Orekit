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
package org.orekit.files.ccsds;

import java.util.ArrayList;
import java.util.List;

import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/**
 * The NDMFile (Navigation Data Message) class represents the navigation
 * messages used by the CCSDS format, (i.e. the Attitude Data Message (ADM),
 * the Orbit Data Message (ODM) and the Tracking Data Message (TDM)).
 * It contains the information of the message's header and configuration data
 * (set in the parser).
 * @see ADMFile
 * @see ODMFile
 * @see TDMFile
 * @author Bryan Cazabonne
 * @since 10.2
 */
public abstract class NDMFile {

    /** CCSDS Format version. */
    private double formatVersion;

    /** Header comments. The list contains a string for each line of comment. */
    private List<String> headerComment;

    /** File creation date and time in UTC. */
    private AbsoluteDate creationDate;

    /** Creating agency or operator. */
    private String originator;

    /** Data context. */
    private DataContext dataContext;

    /** IERS conventions used. */
    private IERSConventions conventions;

    /** Gravitational coefficient. */
    private double mu;

    /** Initial Date for MET or MRT time systems. */
    private AbsoluteDate missionReferenceDate;

    /**
     * Constructor.
     */
    public NDMFile() {
        mu = Double.NaN;
        // Initialise an empty comments list
        headerComment = new ArrayList<>();
    }

    /**
     * Get the used gravitational coefficient.
     * @return the coefficient
     */
    public double getMu() {
        return mu;
    }

    /**
     * Set the used gravitational coefficient.
     * @param mu the coefficient to set
     */
    public void setMu(final double mu) {
        this.mu = mu;
    }

    /**
     * Get the CCSDS NDM (ADM or ODM) format version.
     * @return format version
     */
    public double getFormatVersion() {
        return formatVersion;
    }

    /**
     * Set the CCSDS NDM (ADM or ODM) format version.
     * @param formatVersion the format version to be set
     */
    public void setFormatVersion(final double formatVersion) {
        this.formatVersion = formatVersion;
    }

    /**
     * Get the header comment.
     * @return header comment
     */
    public List<String> getHeaderComment() {
        return headerComment;
    }

    /**
     * Set the header comment.
     * @param headerComment header comment
     */
    public void setHeaderComment(final List<String> headerComment) {
        this.headerComment = new ArrayList<String>(headerComment);
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

    /**
     * Get IERS conventions.
     * @return conventions IERS conventions
     */
    public IERSConventions getConventions() {
        if (conventions != null) {
            return conventions;
        } else {
            throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
        }
    }

    /**
     * Set IERS conventions.
     * @param conventions IERS conventions to be set
     */
    public void setConventions(final IERSConventions conventions) {
        this.conventions = conventions;
    }

    /**
     * Get reference date for Mission Elapsed Time and Mission Relative Time time systems.
     * @return the reference date
     */
    public AbsoluteDate getMissionReferenceDate() {
        return missionReferenceDate;
    }

    /**
     * Set reference date for Mission Elapsed Time and Mission Relative Time time systems.
     * @param missionReferenceDate reference date for Mission Elapsed Time and Mission Relative Time time systems.
     */
    public void setMissionReferenceDate(final AbsoluteDate missionReferenceDate) {
        this.missionReferenceDate = missionReferenceDate;
    }

    /**
     * Get the data context.
     * @return the data context used for creating frames, time scales, etc.
     */
    public DataContext getDataContext() {
        return dataContext;
    }

    /**
     * Set the data context.
     * @param dataContext used for creating frames, time scales, etc.
     */
    public void setDataContext(final DataContext dataContext) {
        this.dataContext = dataContext;
    }

}

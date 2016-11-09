/* Copyright 2002-2016 CS Systèmes d'Information
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

package org.orekit.files.ccsds;

import java.util.ArrayList;
import java.util.List;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/**
 * The ODMFile (Orbit Data Message) class represents any of the three orbit messages used by the CCSDS,
 * i.e. the Orbit Parameter Message (OPM), the Mean-Elements Message (OMM) and the Orbit Ephemeris Message (OEM).
 * It contains the information of the message's header and configuration data (set in the parser).
 * @author sports
 * @since 6.1
 */
public abstract class ODMFile {

    /** CCSDS Format version. */
    private double formatVersion;

    /** Header comments. The list contains a string for each line of comment. */
    private List<String> headerComment;

    /** File creation date and time in UTC. */
    private AbsoluteDate creationDate;

    /** Creating agency or operator. */
    private String originator;

    /** Gravitational coefficient set by the user in the parser. */
    private double muSet;

    /** Gravitational coefficient parsed in the ODM File. */
    private double muParsed;

    /** Gravitational coefficient created from the knowledge of the central body. */
    private double muCreated;

    /** IERS conventions used. */
    private IERSConventions conventions;

    /** Final gravitational coefficient (used for the public methods that need such a parameter, ex: generateCartesianOrbit).
     * In order of decreasing priority, finalMU is equal to: the coefficient parsed in the file, the coefficient set by the
     * user with the parser's method setMu, the coefficient created from the knowledge of the central body.
     */
    private double muUsed;

    /** Initial Date for MET or MRT time systems. */
    private AbsoluteDate missionReferenceDate;

    /** ODMFile constructor. */
    public ODMFile() {
        muSet     = Double.NaN;
        muParsed  = Double.NaN;
        muCreated = Double.NaN;
        muUsed    = Double.NaN;
    }

    /**
     * Get the gravitational coefficient set by the user.
     * @return the coefficient
     */
    public double getMuSet() {
        return muSet;
    }

    /**
     * Set the gravitational coefficient set by the user.
     * @param muSet the coefficient to be set
     */
    void setMuSet(final double muSet) {
        this.muSet = muSet;
    }

    /**
     * Get the gravitational coefficient parsed in the ODM File.
     * @return the coefficient
     */
    public double getMuParsed() {
        return muParsed;
    }

    /**
     * Set the gravitational coefficient parsed in the ODM File.
     * @param muParsed the coefficient to be set
     */
    void setMuParsed(final double muParsed) {
        this.muParsed = muParsed;
    }

    /**
     * Get the gravitational coefficient created from the knowledge of the central body.
     * @return the coefficient
     */
    public double getMuCreated() {
        return muCreated;
    }

    /**
     * Set the gravitational coefficient created from the knowledge of the central body.
     * @param muCreated the coefficient to be set
     */
    void setMuCreated(final double muCreated) {
        this.muCreated = muCreated;
    }

    /**
     * Get the used gravitational coefficient.
     * @return the coefficient
     */
    public double getMuUsed() {
        return muUsed;
    }

    /**
     * Set the gravitational coefficient created from the knowledge of the central body.
     * In order of decreasing priority, finalMU is set equal to:
     * <ol>
     *   <li>the coefficient parsed in the file,</li>
     *   <li>the coefficient set by the user with the parser's method setMu,</li>
     *   <li>the coefficient created from the knowledge of the central body.</li>
     * </ol>
     * @throws OrekitException if no gravitational coefficient can be found
     */
    protected void setMuUsed() throws OrekitException {
        if (!Double.isNaN(muParsed)) {
            muUsed = muParsed;
        } else if (!Double.isNaN(muSet)) {
            muUsed = muSet;
        } else if (!Double.isNaN(muCreated)) {
            muUsed = muCreated;
        } else {
            throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_GM);
        }
    }

    /** Get IERS conventions.
     * @return conventions IERS conventions
     * @exception OrekitException if no IERS conventions have been set
     */
    public IERSConventions getConventions() throws OrekitException {
        if (conventions != null) {
            return conventions;
        } else {
            throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
        }
    }

    /** Set IERS conventions.
     * @param conventions IERS conventions to be set
     */
    void setConventions(final IERSConventions conventions) {
        this.conventions = conventions;
    }

    /** Get reference date for Mission Elapsed Time and Mission Relative Time time systems.
     * @return the reference date
     */
    public AbsoluteDate getMissionReferenceDate() {
        return missionReferenceDate;
    }

    /** Set reference date for Mission Elapsed Time and Mission Relative Time time systems.
     * @param missionReferenceDate reference date for Mission Elapsed Time and Mission Relative Time time systems.
     */
    void setMissionReferenceDate(final AbsoluteDate missionReferenceDate) {
        this.missionReferenceDate = missionReferenceDate;
    }

    /** Get the CCSDS ODM (OPM, OMM or OEM) format version.
     * @return format version
     */
    public double getFormatVersion() {
        return formatVersion;
    }

    /** Set the CCSDS ODM (OPM, OMM or OEM) format version.
     * @param formatVersion the format version to be set
     */
    void setFormatVersion(final double formatVersion) {
        this.formatVersion = formatVersion;
    }

    /** Get the header comment.
     * @return header comment
     */
    public List<String> getHeaderComment() {
        return headerComment;
    }

    /** Set the header comment.
     * @param headerComment header comment
     */
    void setHeaderComment(final List<String> headerComment) {
        this.headerComment = new ArrayList<String>(headerComment);
    }

    /** Get the file creation date and time in UTC.
     * @return the file creation date and time in UTC.
     */
    public AbsoluteDate getCreationDate() {
        return creationDate;
    }

    /** Set the file creation date and time in UTC.
     * @param creationDate the creation date to be set
     */
    void setCreationDate(final AbsoluteDate creationDate) {
        this.creationDate = creationDate;
    }

    /** Get the file originator.
     * @return originator the file originator.
     */
    public String getOriginator() {
        return originator;
    }

    /** Set the file originator.
     * @param originator the originator to be set
     */
    void setOriginator(final String originator) {
        this.originator = originator;
    }

}


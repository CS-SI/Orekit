/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
 * The ADMFile (Attitude Data Message) class represents any of the two attitude
 * messages used by the CCSDS, (i.e. the Attitude Parameter Message (APM),
 * and the Attitude Ephemeris Message (AEM). It contains the information of the message's
 * header and configuration data (set in the parser).
 * @author Bryan Cazabonne
 * @since 10.2
 */
public abstract class ADMFile {

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

    /**
     * Constructor.
     */
    public ADMFile() {
        // Do nothing
    }

    /** Get the CCSDS ADM (APM or AEM) format version.
     * @return format version
     */
    public double getFormatVersion() {
        return formatVersion;
    }

    /** Set the CCSDS ADM (APM or AEM) format version.
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

    /** Get IERS conventions.
     * @return conventions IERS conventions
     */
    public IERSConventions getConventions() {
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

    /** Get the data context.
     * @return the data context used for creating frames, time scales, etc.
     */
    public DataContext getDataContext() {
        return dataContext;
    }

    /** Set the data context.
     * @param dataContext used for creating frames, time scales, etc.
     */
    void setDataContext(final DataContext dataContext) {
        this.dataContext = dataContext;
    }

}

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
import java.util.Collections;
import java.util.List;

import org.orekit.time.TimeScale;

/** This class gathers the meta-data present in the Attitude Data Message (ADM).
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class ADMMetaData {

    /** ODM file to which these meta-data belong. */
    private final ADMFile admFile;

    /** Time System: used for metadata, orbit state and covariance data. */
    private CcsdsTimeScale timeSystem;

    /** Spacecraft name for which the orbit state is provided. */
    private String objectName;

    /** Object identifier of the object for which the orbit state is provided. */
    private String objectID;

    /** Origin of reference frame. */
    private String centerName;

    /** Metadata comments. The list contains a string for each line of comment. */
    private List<String> comment;

    /** Create a new meta-data.
     * @param admFile ADM file to which these meta-data belong
     */
    public ADMMetaData(final ADMFile admFile) {
        this.admFile = admFile;
        comment = new ArrayList<String>();
    }

    /** Get the ADM file to which these meta-data belong.
     * @return ADM file to which these meta-data belong
     */
    public ADMFile getODMFile() {
        return admFile;
    }

    /** Get the Time System.
     * @return the time system
     */
    public CcsdsTimeScale getTimeSystem() {
        return timeSystem;
    }

    /** Set the Time System.
     * @param timeSystem the time system to be set
     */
    void setTimeSystem(final CcsdsTimeScale timeSystem) {
        this.timeSystem = timeSystem;
    }

    /** Get the time scale.
     * @return the time scale.
     * @see #getTimeSystem()
     */
    public TimeScale getTimeScale() {
        return getTimeSystem().getTimeScale(
                admFile.getConventions(),
                admFile.getDataContext().getTimeScales());
    }

    /** Get the spacecraft name for which the orbit state is provided.
     * @return the spacecraft name
     */
    public String getObjectName() {
        return objectName;
    }

    /** Set the spacecraft name for which the orbit state is provided.
     * @param objectName the spacecraft name to be set
     */
    void setObjectName(final String objectName) {
        this.objectName = objectName;
    }

    /** Get the spacecraft ID for which the orbit state is provided.
     * @return the spacecraft ID
     */
    public String getObjectID() {
        return objectID;
    }

    /** Set the spacecraft ID for which the orbit state is provided.
     * @param objectID the spacecraft ID to be set
     */
    void setObjectID(final String objectID) {
        this.objectID = objectID;
    }

    /** Get the origin of reference frame.
     * @return the origin of reference frame.
     */
    public String getCenterName() {
        return centerName;
    }

    /** Set the origin of reference frame.
     * @param centerName the origin of reference frame to be set
     */
    void setCenterName(final String centerName) {
        this.centerName = centerName;
    }

    /** Get the meta-data comment.
     * @return meta-data comment
     */
    public List<String> getComment() {
        return Collections.unmodifiableList(comment);
    }

    /** Set the meta-data comment.
     * @param comment comment to set
     */
    void setComment(final List<String> comment) {
        this.comment = new ArrayList<String>(comment);
    }

}

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
import java.util.Collections;
import java.util.List;

import org.orekit.bodies.CelestialBodies;
import org.orekit.bodies.CelestialBody;
import org.orekit.time.TimeScale;

/** This class gathers the meta-data present in the Attitude Data Message (ADM).
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class ADMMetaData {

    /** ADM file to which these meta-data belong. */
    private final ADMFile admFile;

    /** Time System: used for metadata and attitude data data. */
    private CcsdsTimeScale timeSystem;

    /** Spacecraft name for which the attitude data are provided. */
    private String objectName;

    /** Object identifier of the object for which the attitude data are provided. */
    private String objectID;

    /** Launch Year. */
    private int launchYear;

    /** Launch number. */
    private int launchNumber;

    /** Piece of launch (from "A" to "ZZZ"). */
    private String launchPiece;

    /** Origin of reference frame. */
    private String centerName;

    /** Celestial body corresponding to the center name. */
    private CelestialBody centerBody;

    /** Tests whether the body corresponding to the center name can be
     * created through {@link CelestialBodies} in order to obtain the
     * corresponding gravitational coefficient. */
    private boolean hasCreatableBody;

    /** Metadata comments. The list contains a string for each line of comment. */
    private List<String> comment;

    /**
     * Create a new meta-data.
     * @param admFile ADM file to which these meta-data belong
     */
    public ADMMetaData(final ADMFile admFile) {
        this.admFile = admFile;
        comment = new ArrayList<String>();
    }

    /**
     * Get the ADM file to which these meta-data belong.
     * @return ADM file to which these meta-data belong
     */
    public ADMFile getADMFile() {
        return admFile;
    }

    /**
     * Get the Time System.
     * @return the time system
     */
    public CcsdsTimeScale getTimeSystem() {
        return timeSystem;
    }

    /**
     * Set the Time System.
     * @param timeSystem the time system to be set
     */
    public void setTimeSystem(final CcsdsTimeScale timeSystem) {
        this.timeSystem = timeSystem;
    }

    /**
     * Get the time scale.
     * @return the time scale.
     * @see #getTimeSystem()
     */
    public TimeScale getTimeScale() {
        return getTimeSystem().getTimeScale(
                admFile.getConventions(),
                admFile.getDataContext().getTimeScales());
    }

    /**
     * Get the spacecraft name for which the attitude data are provided.
     * @return the spacecraft name
     */
    public String getObjectName() {
        return objectName;
    }

    /**
     * Set the spacecraft name for which the attitude data are provided.
     * @param objectName the spacecraft name to be set
     */
    public void setObjectName(final String objectName) {
        this.objectName = objectName;
    }

    /**
     * Get the spacecraft ID for which the attitude data are provided.
     * @return the spacecraft ID
     */
    public String getObjectID() {
        return objectID;
    }

    /**
     * Set the spacecraft ID for which the attitude data are provided.
     * @param objectID the spacecraft ID to be set
     */
    public void setObjectID(final String objectID) {
        this.objectID = objectID;
    }

    /**
     * Get the launch year.
     * @return launch year
     */
    public int getLaunchYear() {
        return launchYear;
    }

    /**
     * Set the launch year.
     * @param launchYear launch year
     */
    public void setLaunchYear(final int launchYear) {
        this.launchYear = launchYear;
    }

    /**
     * Get the launch number.
     * @return launch number
     */
    public int getLaunchNumber() {
        return launchNumber;
    }

    /**
     * Set the launch number.
     * @param launchNumber launch number
     */
    public void setLaunchNumber(final int launchNumber) {
        this.launchNumber = launchNumber;
    }

    /**
     * Get the piece of launch.
     * @return piece of launch
     */
    public String getLaunchPiece() {
        return launchPiece;
    }

    /**
     * Set the piece of launch.
     * @param launchPiece piece of launch
     */
    public void setLaunchPiece(final String launchPiece) {
        this.launchPiece = launchPiece;
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
    public void setCenterName(final String centerName) {
        this.centerName = centerName;
    }

    /**
     * Get the {@link CelestialBody} corresponding to the center name.
     * @return the center body
     */
    public CelestialBody getCenterBody() {
        return centerBody;
    }

    /**
     * Set the {@link CelestialBody} corresponding to the center name.
     * @param centerBody the {@link CelestialBody} to be set
     */
    public void setCenterBody(final CelestialBody centerBody) {
        this.centerBody = centerBody;
    }

    /**
     * Get boolean testing whether the body corresponding to the centerName
     * attribute can be created through the {@link CelestialBodies}.
     * @return true if {@link CelestialBody} can be created from centerName
     *         false otherwise
     */
    public boolean getHasCreatableBody() {
        return hasCreatableBody;
    }

    /**
     * Set boolean testing whether the body corresponding to the centerName
     * attribute can be created through the {@link CelestialBodies}.
     * @param hasCreatableBody the boolean to be set.
     */
    public void setHasCreatableBody(final boolean hasCreatableBody) {
        this.hasCreatableBody = hasCreatableBody;
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
    public void setComment(final List<String> comment) {
        this.comment = new ArrayList<String>(comment);
    }

}

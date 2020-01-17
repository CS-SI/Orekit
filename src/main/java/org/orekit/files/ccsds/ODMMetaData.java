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

import org.orekit.bodies.CelestialBodies;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;

/** This class gathers the meta-data present in the Orbital Data Message (ODM).
 * @author sports
 * @since 6.1
 */
public class ODMMetaData {

    /** ODM file to which these meta-data belong. */
    private final ODMFile odmFile;

    /** Time System: used for metadata, orbit state and covariance data. */
    private CcsdsTimeScale timeSystem;

    /** Spacecraft name for which the orbit state is provided. */
    private String objectName;

    /** Object identifier of the object for which the orbit state is provided. */
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

    /** Reference frame in which data are given: used for state vector
     * and Keplerian elements data (and for the covariance reference frame if none is given). */
    private Frame refFrame;

    /** The reference frame specifier, as it appeared in the file. */
    private String refFrameString;

    /** Epoch of reference frame, if not intrinsic to the definition of the
     * reference frame. */
    private String frameEpochString;

    /** Epoch of reference frame, if not intrinsic to the definition of the
     * reference frame. */
    private AbsoluteDate frameEpoch;

    /** Metadata comments. The list contains a string for each line of comment. */
    private List<String> comment;

    /** Create a new meta-data.
     * @param odmFile ODM file to which these meta-data belong
     */
    ODMMetaData(final ODMFile odmFile) {
        this.odmFile = odmFile;
        comment = new ArrayList<String>();
    }

    /** Get the ODM file to which these meta-data belong.
     * @return ODM file to which these meta-data belong
     */
    public ODMFile getODMFile() {
        return odmFile;
    }

    /** Get the Time System that: for OPM, is used for metadata, state vector,
     * maneuver and covariance data, for OMM, is used for metadata, orbit state
     * and covariance data, for OEM, is used for metadata, ephemeris and
     * covariance data.
     * @return the time system
     */
    public CcsdsTimeScale getTimeSystem() {
        return timeSystem;
    }

    /** Set the Time System that: for OPM, is used for metadata, state vector,
     * maneuver and covariance data, for OMM, is used for metadata, orbit state
     * and covariance data, for OEM, is used for metadata, ephemeris and
     * covariance data.
     * @param timeSystem the time system to be set
     */
    void setTimeSystem(final CcsdsTimeScale timeSystem) {
        this.timeSystem = timeSystem;
    }

    /**
     * Get the time scale.
     *
     * @return the time scale.
     * @see #getTimeSystem()
     * @throws OrekitException if there is not corresponding time scale.
     * @since 10.1
     */
    TimeScale getTimeScale() {
        return getTimeSystem().getTimeScale(
                odmFile.getConventions(),
                odmFile.getDataContext().getTimeScales());
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

    /** Set the launch year.
     * @param launchYear launch year
     */
    void setLaunchYear(final int launchYear) {
        this.launchYear = launchYear;
    }

    /** Get the launch year.
     * @return launch year
     */
    public int getLaunchYear() {
        return launchYear;
    }

    /** Set the launch number.
     * @param launchNumber launch number
     */
    void setLaunchNumber(final int launchNumber) {
        this.launchNumber = launchNumber;
    }

    /** Get the launch number.
     * @return launch number
     */
    public int getLaunchNumber() {
        return launchNumber;
    }

    /** Set the piece of launch.
     * @param launchPiece piece of launch
     */
    void setLaunchPiece(final String launchPiece) {
        this.launchPiece = launchPiece;
    }

    /** Get the piece of launch.
     * @return piece of launch
     */
    public String getLaunchPiece() {
        return launchPiece;
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

    /** Get the {@link CelestialBody} corresponding to the center name.
     * @return the center body
     */
    public CelestialBody getCenterBody() {
        return centerBody;
    }

    /** Set the {@link CelestialBody} corresponding to the center name.
     * @param centerBody the {@link CelestialBody} to be set
     */
    void setCenterBody(final CelestialBody centerBody) {
        this.centerBody = centerBody;
    }

    /** Get boolean testing whether the body corresponding to the centerName
     * attribute can be created through the {@link CelestialBodies}.
     * @return true if {@link CelestialBody} can be created from centerName
     *         false otherwise
     */
    public boolean getHasCreatableBody() {
        return hasCreatableBody;
    }

    /** Set boolean testing whether the body corresponding to the centerName
     * attribute can be created through the {@link CelestialBodies}.
     * @param hasCreatableBody the boolean to be set.
     */
    void setHasCreatableBody(final boolean hasCreatableBody) {
        this.hasCreatableBody = hasCreatableBody;
    }

    /**
     * Get the reference frame in which data are given: used for state vector and
     * Keplerian elements data (and for the covariance reference frame if none is given).
     *
     * @return the reference frame
     */
    public Frame getFrame() {
        final Frame frame = this.getRefFrame();
        final CelestialBody body = this.getCenterBody();
        if (body == null) {
            throw new OrekitException(OrekitMessages.NO_DATA_LOADED_FOR_CELESTIAL_BODY,
                    this.getCenterName());
        }
        // Just return frame if we don't need to shift the center based on CENTER_NAME
        // MCI and ICRF are the only non-earth centered frames specified in Annex A.
        final String frameString = this.getFrameString();
        final boolean isMci = "MCI".equals(frameString);
        final boolean isIcrf = "ICRF".equals(frameString);
        final boolean isSolarSystemBarycenter =
                CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER.equals(body.getName());
        if ((!(isMci || isIcrf) && CelestialBodyFactory.EARTH.equals(body.getName())) ||
                (isMci && CelestialBodyFactory.MARS.equals(body.getName())) ||
                (isIcrf && isSolarSystemBarycenter)) {
            return frame;
        }
        // else, translate frame to specified center.
        return new CcsdsModifiedFrame(frame, frameString, body, this.getCenterName());
    }

    /**
     * Get the the value of {@code REF_FRAME} as an Orekit {@link Frame}. The {@code
     * CENTER_NAME} key word has not been applied yet, so the returned frame may not
     * correspond to the reference frame of the data in the file.
     *
     * @return The reference frame specified by the {@code REF_FRAME} keyword.
     * @see #getFrame()
     */
    public Frame getRefFrame() {
        return refFrame;
    }

    /** Set the reference frame in which data are given: used for state vector
     * and Keplerian elements data (and for the covariance reference frame if none is given).
     * @param refFrame the reference frame to be set
     */
    void setRefFrame(final Frame refFrame) {
        this.refFrame = refFrame;
    }

    /**
     * Get the reference frame specifier as it appeared in the file.
     *
     * @return the frame name as it appeared in the file.
     * @see #getFrame()
     */
    public String getFrameString() {
        return this.refFrameString;
    }

    /**
     * Set the reference frame name.
     *
     * @param frame specifier as it appeared in the file.
     */
    void setFrameString(final String frame) {
        this.refFrameString = frame;
    }

    /** Get epoch of reference frame, if not intrinsic to the definition of the
     * reference frame.
     * @return epoch of reference frame
     */
    public String getFrameEpochString() {
        return frameEpochString;
    }

    /** Set epoch of reference frame, if not intrinsic to the definition of the
     * reference frame.
     * @param frameEpochString the epoch of reference frame to be set
     */
    void setFrameEpochString(final String frameEpochString) {
        this.frameEpochString = frameEpochString;
    }

    /** Get epoch of reference frame, if not intrinsic to the definition of the
     * reference frame.
     * @return epoch of reference frame
     */
    public AbsoluteDate getFrameEpoch() {
        return frameEpoch;
    }

    /** Set epoch of reference frame, if not intrinsic to the definition of the
     * reference frame.
     * @param frameEpoch the epoch of reference frame to be set
     */
    void setFrameEpoch(final AbsoluteDate frameEpoch) {
        this.frameEpoch = frameEpoch;
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




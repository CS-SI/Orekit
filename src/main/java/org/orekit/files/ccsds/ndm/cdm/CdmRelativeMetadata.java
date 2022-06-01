/* Copyright 2002-2022 CS GROUP
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
package org.orekit.files.ccsds.ndm.cdm;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.PocMethodFacade;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.time.AbsoluteDate;

/** This class gathers the relative meta-data present in the Conjunction Data Message (CDM).
 * @author Melina Vanel
 * @since 11.2
 */
public class CdmRelativeMetadata {

    /** Time System: used for metadata, orbit state and covariance data. */
    private TimeSystem timeSystem;

    /** Comment. */
    private List<String> comment;

    /** Date and time in UTC of the closest approach. */
    private AbsoluteDate tca;

    /** Norm of relative position vector at TCA. */
    private double missDistance;

    /** Norm of relative velocity vector at TCA. */
    private double relativeSpeed;

    /** The R component of Object2’s position relative to Object1’s position in the Radial/Transverse/Normal coordinate frame. */
    private double relativePositionR;

    /** The T component of Object2’s position relative to Object1’s position in the Radial/Transverse/Normal coordinate frame. */
    private double relativePositionT;

    /** The N component of Object2’s position relative to Object1’s position in the Radial/Transverse/Normal coordinate frame. */
    private double relativePositionN;

    /** The R component of Object2’s velocity relative to Object1’s veloity in the Radial/Transverse/Normal coordinate frame. */
    private double relativeVelocityR;

    /** The T component of Object2’s velocity relative to Object1’s veloity in the Radial/Transverse/Normal coordinate frame. */
    private double relativeVelocityT;

    /** The N component of Object2’s velocity relative to Object1’s veloity in the Radial/Transverse/Normal coordinate frame. */
    private double relativeVelocityN;

    /** The start time in UTC of the screening period for the conjunction assessment. */
    private AbsoluteDate startScreenPeriod;

    /** The stop time in UTC of the screening period for the conjunction assessment. */
    private AbsoluteDate stopScreenPeriod;

    /** Name of the Object1 centered reference frame in which the screening volume data are given. */
    private ScreenVolumeFrame screenVolumeFrame;

    /** Shape of the screening volume. */
    private ScreenVolumeShape screenVolumeShape;

    /** The R or T (depending on if RTN or TVN is selected) component size of the screening volume in the SCREEN_VOLUME_FRAME. */
    private double screenVolumeX;

    /** The T or V (depending on if RTN or TVN is selected) component size of the screening volume in the SCREEN_VOLUME_FRAME. */
    private double screenVolumeY;

    /** The N component size of the screening volume in the SCREEN_VOLUME_FRAME. */
    private double screenVolumeZ;

    /** The time in UTC when Object2 enters the screening volume. */
    private AbsoluteDate screenEntryTime;

    /** The time in UTC when Object2 exits the screening volume. */
    private AbsoluteDate screenExitTime;

    /** The probability (denoted ‘p’ where 0.0<=p<=1.0), that Object1 and Object2 will collide. */
    private double collisionProbability;

    /** The method that was used to calculate the collision probability. */
    private PocMethodFacade collisionProbabilityMethod;

    /** Simple constructor.
     */
    public CdmRelativeMetadata() {
        this.comment = new ArrayList<>();

        this.relativeSpeed        = Double.NaN;
        this.relativePositionR    = Double.NaN;
        this.relativePositionT    = Double.NaN;
        this.relativePositionN    = Double.NaN;

        this.relativeVelocityR    = Double.NaN;
        this.relativeVelocityT    = Double.NaN;
        this.relativeVelocityN    = Double.NaN;

        this.screenVolumeX        = Double.NaN;
        this.screenVolumeY        = Double.NaN;
        this.screenVolumeZ        = Double.NaN;
        this.collisionProbability = Double.NaN;
    }

    /** Check is all mandatory entries have been initialized.
    */
    public void validate() {
        checkNotNull(tca,            CdmRelativeMetadataKey.TCA);
        checkNotNull(missDistance,   CdmRelativeMetadataKey.MISS_DISTANCE);
    }

    /**
     * Get the date and time in UTC of the closest approach.
     * @return time of closest approach
     */
    public AbsoluteDate getTca() {
        return tca;
    }

    /**
     * Set the date and time in UTC of the closest approach.
     * @param tca time of closest approach to be set
     */
    public void setTca(final AbsoluteDate tca) {
        this.tca = tca;
    }

    /**
     * Get the norm of relative position vector at TCA.
     * @return the miss distance (in m)
     */
    public double getMissDistance() {
        return missDistance;
    }

    /**
     * Set the norm of relative position vector at TCA.
     * @param missDistance the miss distance to be set (in m)
     */
    public void setMissDistance(final double missDistance) {
        this.missDistance = missDistance;
    }

    /**
     * Get the norm of relative velocity vector at TCA.
     * @return the relative speed at TCA (in m/s)
     */
    public double getRelativeSpeed() {
        return relativeSpeed;
    }

    /**
     * Set the norm of relative velocity vector at TCA.
     * @param relativeSpeed the relative speed (in m/s) at TCA to be set
     */
    public void setRelativeSpeed(final double relativeSpeed) {
        this.relativeSpeed = relativeSpeed;
    }

    /**
     * Get the Object2’s velocity vector relative to Object1's at TCA in RTN frame, getX for R component,
     * getY for T component, getZ for N component.
     * @return the relative speed vector at TCA (in m/s)
     */
    public Vector3D getRelativeVelocity() {
        return new Vector3D(relativeVelocityR, relativeVelocityT, relativeVelocityN);
    }

    /**
     * Get the Object2’s position vector relative to Object1's at TCA in RTN frame, getX for R component,
     * getY for T component, getZ for N component.
     * @return the relative position vector at TCA (in m)
     */
    public Vector3D getRelativePosition() {
        return new Vector3D(relativePositionR, relativePositionT, relativePositionN);
    }

    /**
     * Set the R component of Object2’s position relative to Object1’s in RTN frame.
     * @param relativePositionR the R component (in m) of Object2’s position relative to Object1’s
     */
    public void setRelativePositionR(final double relativePositionR) {
        this.relativePositionR = relativePositionR;
    }

    /**
     * Set the T component of Object2’s position relative to Object1’s in RTN frame.
     * @param relativePositionT the T component (in m) of Object2’s position relative to Object1’s
     */
    public void setRelativePositionT(final double relativePositionT) {
        this.relativePositionT = relativePositionT;
    }

    /**
     * Set the N component of Object2’s position relative to Object1’s in RTN frame.
     * @param relativePositionN the N component (in m) of Object2’s position relative to Object1’s
     */
    public void setRelativePositionN(final double relativePositionN) {
        this.relativePositionN = relativePositionN;
    }

    /**
     * Set the R component of Object2’s velocity relative to Object1’s in RTN frame.
     * @param relativeVelocityR the R component (in m/s) of Object2’s velocity relative to Object1’s
     */
    public void setRelativeVelocityR(final double relativeVelocityR) {
        this.relativeVelocityR = relativeVelocityR;
    }

    /**
     * Set the T component of Object2’s velocity relative to Object1’s in RTN frame.
     * @param relativeVelocityT the T component (in m/s) of Object2’s velocity relative to Object1’s
     */
    public void setRelativeVelocityT(final double relativeVelocityT) {
        this.relativeVelocityT = relativeVelocityT;
    }

    /**
     * Set the N component of Object2’s velocity relative to Object1’s in RTN frame.
     * @param relativeVelocityN the N component (in m/s) of Object2’s velocity relative to Object1’s
     */
    public void setRelativeVelocityN(final double relativeVelocityN) {
        this.relativeVelocityN = relativeVelocityN;
    }

    /**
     * Get the start time in UTC of the screening period for the conjunction assessment.
     * @return start time in UTC of the screening period
     */
    public AbsoluteDate getStartScreenPeriod() {
        return startScreenPeriod;
    }

    /**
     * Set the start time in UTC of the screening period for the conjunction assessment.
     * @param startScreenPeriod start time in UTC of the screening period to be set
     */
    public void setStartScreenPeriod(final AbsoluteDate startScreenPeriod) {
        this.startScreenPeriod = startScreenPeriod;
    }

    /**
     * Get the stop time in UTC of the screening period for the conjunction assessment.
     * @return stop time in UTC of the screening period
     */
    public AbsoluteDate getStopScreenPeriod() {
        return stopScreenPeriod;
    }

    /**
     * Set the stop time in UTC of the screening period for the conjunction assessment.
     * @param stopScreenPeriod stop time in UTC of the screening period to be set
     */
    public void setStopScreenPeriod(final AbsoluteDate stopScreenPeriod) {
        this.stopScreenPeriod = stopScreenPeriod;
    }

    /**
     * Get the name of the Object1 centered reference frame in which the screening volume data are given.
     * @return name of screen volume frame
     */
    public ScreenVolumeFrame getScreenVolumeFrame() {
        return screenVolumeFrame;
    }

    /**
     * Set the name of the Object1 centered reference frame in which the screening volume data are given.
     * @param screenVolumeFrame name of screen volume frame
     */
    public void setScreenVolumeFrame(final ScreenVolumeFrame screenVolumeFrame) {
        this.screenVolumeFrame = screenVolumeFrame;
    }

    /**
     * Get the shape of the screening volume.
     * @return shape of the screening volume
     */
    public ScreenVolumeShape getScreenVolumeShape() {
        return screenVolumeShape;
    }

    /**
     * Set the shape of the screening volume.
     * @param screenVolumeShape shape of the screening volume
     */
    public void setScreenVolumeShape(final ScreenVolumeShape screenVolumeShape) {
        this.screenVolumeShape = screenVolumeShape;
    }

    /**
     * Get the R or T (depending on if RTN or TVN is selected) component size of the screening volume in the corresponding frame.
     * @return first component size of the screening volume (in m)
     */
    public double getScreenVolumeX() {
        return screenVolumeX;
    }

    /**
     * Set the R or T (depending on if RTN or TVN is selected) component size of the screening volume in the corresponding frame.
     * @param screenVolumeX first component size of the screening volume (in m)
     */
    public void setScreenVolumeX(final double screenVolumeX) {
        this.screenVolumeX = screenVolumeX;
    }

    /**
     * Get the T or V (depending on if RTN or TVN is selected) component size of the screening volume in the corresponding frame.
     * @return second component size of the screening volume (in m)
     */
    public double getScreenVolumeY() {
        return screenVolumeY;
    }

    /**
     * Set the T or V (depending on if RTN or TVN is selected) component size of the screening volume in the corresponding frame.
     * @param screenVolumeY second component size of the screening volume (in m)
     */
    public void setScreenVolumeY(final double screenVolumeY) {
        this.screenVolumeY = screenVolumeY;
    }

    /**
     * Get the N component size of the screening volume in the corresponding frame.
     * @return third component size of the screening volume (in m)
     */
    public double getScreenVolumeZ() {
        return screenVolumeZ;
    }

    /**
     * Set the N component size of the screening volume in the corresponding frame.
     * @param screenVolumeZ third component size of the screening volume (in m)
     */
    public void setScreenVolumeZ(final double screenVolumeZ) {
        this.screenVolumeZ = screenVolumeZ;
    }

    /**
     * Get the time in UTC when Object2 enters the screening volume.
     * @return time in UTC when Object2 enters the screening volume
     */
    public AbsoluteDate getScreenEntryTime() {
        return screenEntryTime;
    }

    /**
     * Set the time in UTC when Object2 enters the screening volume.
     * @param screenEntryTime time in UTC when Object2 enters the screening volume
     */
    public void setScreenEntryTime(final AbsoluteDate screenEntryTime) {
        this.screenEntryTime = screenEntryTime;
    }

    /**
     * Get the time in UTC when Object2 exits the screening volume.
     * @return time in UTC when Object2 exits the screening volume
     */
    public AbsoluteDate getScreenExitTime() {
        return screenExitTime;
    }

    /**
     * Set the time in UTC when Object2 exits the screening volume.
     * @param screenExitTime time in UTC when Object2 exits the screening volume
     */
    public void setScreenExitTime(final AbsoluteDate screenExitTime) {
        this.screenExitTime = screenExitTime;
    }

    /**
     * Get the probability (between 0.0 and 1.0) that Object1 and Object2 will collide.
     * @return probability of collision
     */
    public double getCollisionProbability() {
        return collisionProbability;
    }

    /**
     * Set the probability (between 0.0 and 1.0) that Object1 and Object2 will collide.
     * @param collisionProbability first component size of the screening volume
     */
    public void setCollisionProbability(final double collisionProbability) {
        this.collisionProbability = collisionProbability;
    }

    /**
     * Get the method that was used to calculate the collision probability.
     * @return method to calculate probability of collision
     */
    public PocMethodFacade getCollisionProbaMethod() {
        return collisionProbabilityMethod;
    }

    /**
     * Set the method that was used to calculate the collision probability.
     * @param collisionProbaMethod method used to calculate probability of collision
     */
    public void setCollisionProbaMethod(final PocMethodFacade collisionProbaMethod) {
        this.collisionProbabilityMethod = collisionProbaMethod;
    }

    /** Complain if a field is null.
     * @param field field to check
     * @param key key associated with the field
     */
    public void checkNotNull(final Object field, final Enum<?> key) {
        if (field == null) {
            throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, key.name());
        }
    }

    /** Set the Time System that: for CDM, is used for relative metadata, metadata,
     * OD parameters, state vector. In CDM all date are given in UTC.
     * @param timeSystem the time system to be set
     */
    public void setTimeSystem(final TimeSystem timeSystem) {
        this.timeSystem = timeSystem;
    }

    /** Get the Time System that: for CDM, is used for relative metadata, metadata,
     * OD parameters, state vector. In CDM all date are given in UTC.
     * @return the time system
     */
    public TimeSystem getTimeSystem() {
        return timeSystem;
    }

    /** Set comment for relative metadata.
     * @param comments to be set
     */
    public void addComment(final String comments) {
        this.comment.add(comments);
    }

    /** Get comment for relative metadata.
     * @return the time system
     */
    public List<String> getComment() {
        return comment;
    }

}

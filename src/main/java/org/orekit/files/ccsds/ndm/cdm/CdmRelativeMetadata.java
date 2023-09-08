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

    /** The length of the relative position vector, normalized to one-sigma dispersions of the combined error covariance
     * in the direction of the relative position vector. */
    private double mahalanobisDistance;

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

    /** Shape of the screening volume. */
    private ScreenVolumeShape screenVolumeShape;

    /** Shape of the screening volume. */
    private double screenVolumeRadius;

    /** Name of the Object1 centered reference frame in which the screening volume data are given. */
    private ScreenVolumeFrame screenVolumeFrame;

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

    /** the Originator’s ID that uniquely identifies the conjunction to which the message refers. */
    private String conjunctionId;

    /** The approach angle computed between Objects 1 and 2 in the RTN coordinate frame relative to object 1. */
    private double approachAngle;

    /** The type of screening to be used. */
    private ScreenType screenType;

    /** The maximum collision probability that Object1 and Object2 will collide. */
    private double maxCollisionProbability;

    /** The method that was used to calculate the maximum collision probability. */
    private PocMethodFacade maxCollisionProbabilityMethod;

   /**  The space environment fragmentation impact (SEFI) adjusted estimate of collision probability that Object1 and Object2 will collide. */
    private double sefiCollisionProbability;

    /** The method that was used to calculate the space environment fragmentation impact collision probability. */
    private PocMethodFacade sefiCollisionProbabilityMethod;

    /** The Space environment fragmentation model used. */
    private String sefiFragmentationModel;

    /** The collision probability screening threshold used to identify this conjunction. */
    private double screenPcThreshold;

    /** An array of 1 to n elements indicating the percentile(s) for which estimates of the collision probability are provided in the
     * COLLISION_PROBABILITY variable. */
    private int[] collisionPercentile;

    /** ID of previous CDM issued for event identified by CONJUNCTION_ID. */
    private String previousMessageId;

    /** UTC epoch of the previous CDM issued for the event identified by CONJUNCTION_ID. */
    private AbsoluteDate previousMessageEpoch;

    /** Scheduled UTC epoch of the next CDM associated with the event identified by CONJUNCTION_ID. */
    private AbsoluteDate nextMessageEpoch;

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

        this.approachAngle        = Double.NaN;
        this.screenVolumeRadius   = Double.NaN;
        this.screenPcThreshold    = Double.NaN;
        this.mahalanobisDistance  = Double.NaN;


        this.screenVolumeX        = Double.NaN;
        this.screenVolumeY        = Double.NaN;
        this.screenVolumeZ        = Double.NaN;
        this.collisionProbability = Double.NaN;
        this.maxCollisionProbability  = Double.NaN;
        this.sefiCollisionProbability = Double.NaN;

    }

    /** Check is all mandatory entries have been initialized.
    */
    public void validate() {
        checkNotNull(tca,            CdmRelativeMetadataKey.TCA);
        checkNotNull(missDistance,   CdmRelativeMetadataKey.MISS_DISTANCE);
        checkScreenVolumeConditions();
    }

    /**
     * Get the Originator’s ID that uniquely identifies the conjunction to which the message refers.
     * @return the conjunction id
     */
    public String getConjunctionId() {
        return conjunctionId;
    }

    /**
     * Set the Originator’s ID that uniquely identifies the conjunction to which the message refers.
     * @param conjunctionId the conjunction id to be set
     */
    public void setConjunctionId(final String conjunctionId) {
        this.conjunctionId = conjunctionId;
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

    /** Get the approach angle computed between Objects 1 and 2 in the RTN coordinate frame relative to object 1.
     * @return the approachAngle
     */
    public double getApproachAngle() {
        return approachAngle;
    }

    /** Set the approach angle computed between Objects 1 and 2 in the RTN coordinate frame relative to object 1.
     * @param approachAngle the approachAngle to set
     */
    public void setApproachAngle(final double approachAngle) {
        this.approachAngle = approachAngle;
    }

    /** Get the type of screening to be used.
     * @return the screenType
     */
    public ScreenType getScreenType() {
        return screenType;
    }

    /** Set the type of screening to be used.
     * @param screenType the screenType to set
     */
    public void setScreenType(final ScreenType screenType) {
        this.screenType = screenType;
    }

    /** Get max collision probability.
     * @return the max collision probability
     */
    public double getMaxCollisionProbability() {
        return maxCollisionProbability;
    }

    /** Set max collision probability.
     * @param maxCollisionProbability the max collision probability to set
     */
    public void setMaxCollisionProbability(final double maxCollisionProbability) {
        this.maxCollisionProbability = maxCollisionProbability;
    }

    /** Get max collision probability method.
     * @return the max collision probability method
     */
    public PocMethodFacade getMaxCollisionProbabilityMethod() {
        return maxCollisionProbabilityMethod;
    }

    /** Set max collision probability method.
     * @param pocMethodFacade the max collision probability method to set
     */
    public void setMaxCollisionProbabilityMethod(final PocMethodFacade pocMethodFacade) {
        this.maxCollisionProbabilityMethod = pocMethodFacade;
    }

    /** Get the Space Environment Fragmentation Impact probability.
     * @return the Space Environment Fragmentation Impact probability
     */
    public double getSefiCollisionProbability() {
        return sefiCollisionProbability;
    }

    /** Set the Space Environment Fragmentation Impact probability.
     * @param sefiCollisionProbability the Space Environment Fragmentation Impact probability to set
     */
    public void setSefiCollisionProbability(final double sefiCollisionProbability) {
        this.sefiCollisionProbability = sefiCollisionProbability;
    }

    /** Get the Space Environment Fragmentation Impact probability method.
     * @return the Space Environment Fragmentation Impact probability method
     */
    public PocMethodFacade getSefiCollisionProbabilityMethod() {
        return sefiCollisionProbabilityMethod;
    }

    /** Set the Space Environment Fragmentation Impact probability method.
     * @param pocMethodFacade the Space Environment Fragmentation Impact probability method to set
     */
    public void setSefiCollisionProbabilityMethod(final PocMethodFacade pocMethodFacade) {
        this.sefiCollisionProbabilityMethod = pocMethodFacade;
    }

    /** Get the Space Environment Fragmentation Impact fragmentation model.
     * @return the Space Environment Fragmentation Impact fragmentation model
     */
    public String getSefiFragmentationModel() {
        return sefiFragmentationModel;
    }

    /** Set the Space Environment Fragmentation Impact fragmentation model.
     * @param sefiFragmentationModel the Space Environment Fragmentation Impact fragmentation model to set
     */
    public void setSefiFragmentationModel(final String sefiFragmentationModel) {
        this.sefiFragmentationModel = sefiFragmentationModel;
    }

    /** Get the Mahalanobis Distance. The length of the relative position vector, normalized to one-sigma dispersions of the combined error covariance
     * in the direction of the relative position vector.
     * @return the mahalanobisDistance
     */
    public double getMahalanobisDistance() {
        return mahalanobisDistance;
    }

    /** Set the Mahalanobis Distance. The length of the relative position vector, normalized to one-sigma dispersions of the combined error covariance
     * in the direction of the relative position vector.
     * @param mahalanobisDistance the mahalanobisDistance to set
     */
    public void setMahalanobisDistance(final double mahalanobisDistance) {
        this.mahalanobisDistance = mahalanobisDistance;
    }

    /** Get the screen volume radius.
     * @return the screen volume radius
     */
    public double getScreenVolumeRadius() {
        return screenVolumeRadius;
    }

    /** set the screen volume radius.
     * @param screenVolumeRadius the screen volume radius to set
     */
    public void setScreenVolumeRadius(final double screenVolumeRadius) {
        this.screenVolumeRadius = screenVolumeRadius;
    }

    /** Get the collision probability screening threshold used to identify this conjunction.
    * @return the screenPcThreshold
    */
    public double getScreenPcThreshold() {
        return screenPcThreshold;
    }

    /** Set the collision probability screening threshold used to identify this conjunction.
    * @param screenPcThreshold the screenPcThreshold to set
    */
    public void setScreenPcThreshold(final double screenPcThreshold) {
        this.screenPcThreshold = screenPcThreshold;
    }

    /**
     * Check screen volume conditions.
     * <p>
     * The method verifies that all keys are present.
     * Otherwise, an exception is thrown.
     * </p>
     */
    public void checkScreenVolumeConditions() {

        if (this.getScreenType() == ScreenType.SHAPE) {

            if (this.getScreenEntryTime() == null) {
                throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD, CdmRelativeMetadataKey.SCREEN_ENTRY_TIME);
            }

            if (this.getScreenExitTime() == null) {
                throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD, CdmRelativeMetadataKey.SCREEN_EXIT_TIME);
            }

            if (this.getScreenVolumeShape() == null) {
                throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD, CdmRelativeMetadataKey.SCREEN_VOLUME_SHAPE);
            }

            if (this.getScreenVolumeShape() == ScreenVolumeShape.SPHERE) {

                if (Double.isNaN(this.getScreenVolumeRadius())) {
                    throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD, CdmRelativeMetadataKey.SCREEN_VOLUME_RADIUS);
                }

            } else if (this.getScreenVolumeShape() == ScreenVolumeShape.ELLIPSOID || this.getScreenVolumeShape() == ScreenVolumeShape.BOX) {

                if (this.getScreenVolumeFrame() == null) {
                    throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD, CdmRelativeMetadataKey.SCREEN_VOLUME_FRAME);
                }
                if (Double.isNaN(this.getScreenVolumeX())) {
                    throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD, CdmRelativeMetadataKey.SCREEN_VOLUME_X);
                }
                if (Double.isNaN(this.getScreenVolumeY())) {
                    throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD, CdmRelativeMetadataKey.SCREEN_VOLUME_Y);
                }
                if (Double.isNaN(this.getScreenVolumeZ())) {
                    throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD, CdmRelativeMetadataKey.SCREEN_VOLUME_Z);
                }
            }

        } else if (this.getScreenType() == ScreenType.PC || this.getScreenType() == ScreenType.PC_MAX) {

            if (Double.isNaN(this.getScreenPcThreshold())) {
                throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD, CdmRelativeMetadataKey.SCREEN_PC_THRESHOLD);
            }
        }

    }

    /** Get the array of 1 to n elements indicating the percentile(s) for which estimates of the collision probability are provided in the
     * COLLISION_PROBABILITY variable.
     * @return the collisionPercentile
     */
    public int[] getCollisionPercentile() {
        return collisionPercentile == null ? null : collisionPercentile.clone();
    }

    /** Set the array of 1 to n elements indicating the percentile(s) for which estimates of the collision probability are provided in the
     * COLLISION_PROBABILITY variable.
     * @param collisionPercentile the collisionPercentile to set
     */
    public void setCollisionPercentile(final int[] collisionPercentile) {
        this.collisionPercentile = collisionPercentile == null ? null : collisionPercentile.clone();;
    }

    /** Get the ID of previous CDM issued for event identified by CONJUNCTION_ID.
     * @return the previousMessageId
     */
    public String getPreviousMessageId() {
        return previousMessageId;
    }

    /** Set the ID of previous CDM issued for event identified by CONJUNCTION_ID.
     * @param previousMessageId the previousMessageId to set
     */
    public void setPreviousMessageId(final String previousMessageId) {
        this.previousMessageId = previousMessageId;
    }

    /** Get the UTC epoch of the previous CDM issued for the event identified by CONJUNCTION_ID.
     * @return the previousMessageEpoch
     */
    public AbsoluteDate getPreviousMessageEpoch() {
        return previousMessageEpoch;
    }

    /** Set the UTC epoch of the previous CDM issued for the event identified by CONJUNCTION_ID.
     * @param previousMessageEpoch the previousMessageEpoch to set
     */
    public void setPreviousMessageEpoch(final AbsoluteDate previousMessageEpoch) {
        this.previousMessageEpoch = previousMessageEpoch;
    }

    /** Get Scheduled UTC epoch of the next CDM associated with the event identified by CONJUNCTION_ID.
     * @return the nextMessageEpoch
     */
    public AbsoluteDate getNextMessageEpoch() {
        return nextMessageEpoch;
    }

    /** Set Scheduled UTC epoch of the next CDM associated with the event identified by CONJUNCTION_ID.
     * @param nextMessageEpoch the nextMessageEpoch to set
     */
    public void setNextMessageEpoch(final AbsoluteDate nextMessageEpoch) {
        this.nextMessageEpoch = nextMessageEpoch;
    }
}

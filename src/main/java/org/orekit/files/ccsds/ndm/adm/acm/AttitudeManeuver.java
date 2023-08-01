/* Copyright 2023 Luc Maisonobe
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

package org.orekit.files.ccsds.ndm.adm.acm;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.section.CommentsContainer;

/** Maneuver entry.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class AttitudeManeuver extends CommentsContainer {

    /** Maneuver identification number. */
    private String id;

    /** Identification number of previous maneuver. */
    private String prevID;

    /** Purpose of the maneuver. */
    private String manPurpose;

    /** Start time of actual maneuver, relative to t₀. */
    private double beginTime;

    /** End time of actual maneuver, relative to t₀. */
    private double endTime;

    /** Duration. */
    private double duration;

    /** Actuator used. */
    private String actuatorUsed;

    /** Target momentum (if purpose is momentum desaturation). */
    private Vector3D targetMomentum;

    /** Reference frame for {@link #targetMomentum}. */
    private FrameFacade targetMomFrame;

    /** Target attitude (if purpose is attitude adjustment). */
    private Rotation targetAttitude;

    /** Target spin rate (if purpose is spin rate adjustment). */
    private double targetSpinRate;

    /** Build an uninitialized maneuver.
     */
    public AttitudeManeuver() {
        beginTime      = Double.NaN;
        endTime        = Double.NaN;
        duration       = Double.NaN;
        targetSpinRate = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        checkNotNull(manPurpose, AttitudeManeuverKey.MAN_PURPOSE.name());
        checkNotNaN(beginTime,   AttitudeManeuverKey.MAN_BEGIN_TIME.name());
        if (!Double.isNaN(endTime + duration)) {
            throw new OrekitException(OrekitMessages.CCSDS_INCOMPATIBLE_KEYS_BOTH_USED,
                                      AttitudeManeuverKey.MAN_END_TIME,
                                      AttitudeManeuverKey.MAN_DURATION);
        }
        if (targetMomFrame != null) {
            checkNotNull(targetMomentum, AttitudeManeuverKey.TARGET_MOMENTUM.name());
        }
    }

    /** Get maneuver identification number.
     * @return maneuver identification number
     */
    public String getID() {
        return id;
    }

    /** Set maneuver identification number.
     * @param manId maneuver identification number
     */
    public void setID(final String manId) {
        refuseFurtherComments();
        this.id = manId;
    }

    /** Get identification number of previous maneuver.
     * @return identification number of previous maneuver
     */
    public String getPrevID() {
        return prevID;
    }

    /** Set identification number of previous maneuver.
     * @param prevID identification number of previous maneuver
     */
    public void setPrevID(final String prevID) {
        refuseFurtherComments();
        this.prevID = prevID;
    }

    /** Get purpose of maneuver.
     * @return purpose of maneuver
     */
    public String getManPurpose() {
        return manPurpose;
    }

    /** Set purpose of maneuver.
     * @param manPurpose purpose of maneuver
     */
    public void setManPurpose(final String manPurpose) {
        refuseFurtherComments();
        this.manPurpose = manPurpose;
    }

    /** Get start time of actual maneuver, relative to t₀.
     * @return start time of actual maneuver, relative to t₀
     */
    public double getBeginTime() {
        return beginTime;
    }

    /** Set start time of actual maneuver, relative to t₀.
     * @param beginTime start time of actual maneuver, relative to t₀
     */
    public void setBeginTime(final double beginTime) {
        this.beginTime = beginTime;
    }

    /** Get end time of actual maneuver, relative to t₀.
     * @return end time of actual maneuver, relative to t₀
     */
    public double getEndTime() {
        return endTime;
    }

    /** Set end time of actual maneuver, relative to t₀.
     * @param endTime end time of actual maneuver, relative to t₀
     */
    public void setEndTime(final double endTime) {
        this.endTime = endTime;
    }

    /** Get duration.
     * @return duration
     */
    public double getDuration() {
        return duration;
    }

    /** Set duration.
     * @param duration duration
     */
    public void setDuration(final double duration) {
        this.duration = duration;
    }

    /** Get the actuator used.
     * @return actuator used
     */
    public String getActuatorUsed() {
        return actuatorUsed;
    }

    /** Set actuator used.
     * @param actuatorUsed actuator used
     */
    public void setActuatorUsed(final String actuatorUsed) {
        this.actuatorUsed = actuatorUsed;
    }

    /** Get target momentum (if purpose is momentum desaturation).
     * @return target momentum
     */
    public Vector3D getTargetMomentum() {
        return targetMomentum;
    }

    /** Set target momentum (if purpose is momentum desaturation).
     * @param targetMomentum target momentum
     */
    public void setTargetMomentum(final Vector3D targetMomentum) {
        this.targetMomentum = targetMomentum;
    }

    /** Get reference frame for {@link #getTargetMomentum()}.
     * @return reference frame for {@link #getTargetMomentum()}
     */
    public FrameFacade getTargetMomFrame() {
        return targetMomFrame;
    }

    /** Set reference frame for {@link #getTargetMomentum()}.
     * @param targetMomFrame reference frame for {@link #getTargetMomentum()}
     */
    public void setTargetMomFrame(final FrameFacade targetMomFrame) {
        this.targetMomFrame = targetMomFrame;
    }

    /** Get target attitude (if purpose is attitude adjustment).
     * @return target attitude
     */
    public Rotation getTargetAttitude() {
        return targetAttitude;
    }

    /** Set target attitude (if purpose is attitude adjustment).
     * @param targetAttitude target attitude
     */
    public void setTargetAttitude(final Rotation targetAttitude) {
        this.targetAttitude = targetAttitude;
    }

    /** Get target spin rate (if purpose is spin rate adjustment).
     * @return target spin rate
     */
    public double getTargetSpinRate() {
        return targetSpinRate;
    }

    /** Set target spin rate (if purpose is spin rate adjustment).
     * @param targetSpinRate target spin rate
     */
    public void setTargetSpinRate(final double targetSpinRate) {
        this.targetSpinRate = targetSpinRate;
    }

}

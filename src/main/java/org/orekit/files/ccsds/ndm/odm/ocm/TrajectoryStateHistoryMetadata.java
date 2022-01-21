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

package org.orekit.files.ccsds.ndm.odm.ocm;

import java.util.List;

import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.BodyFacade;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.definitions.ElementsType;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.ndm.odm.oem.InterpolationMethod;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.units.Unit;

/** Metadata for trajectory state history.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class TrajectoryStateHistoryMetadata extends CommentsContainer {

    /** Trajectory identification number. */
    private String trajID;

    /** Identification number of previous trajectory. */
    private String trajPrevID;

    /** Identification number of next trajectory. */
    private String trajNextID;

    /** Basis of this trajectory state time history data. */
    private String trajBasis;

    /** Identification number of the orbit determination or simulation upon which this trajectory is based. */
    private String trajBasisID;

    /** Interpolation method. */
    private InterpolationMethod interpolationMethod;

    /** Interpolation degree. */
    private int interpolationDegree;

    /** Type of averaging (Osculating, mean Brouwer, other...). */
    private String orbAveraging;

    /** Origin of reference frame. */
    private BodyFacade center;

    /** Reference frame of the trajectory. */
    private FrameFacade trajReferenceFrame;

    /** Epoch of the trajectory reference frame. */
    private AbsoluteDate trajFrameEpoch;

    /** Start of useable time span covered by ephemerides data, it may be
     * necessary to allow for proper interpolation. */
    private AbsoluteDate useableStartTime;

    /** End of useable time span covered by ephemerides data, it may be
     * necessary to allow for proper interpolation. */
    private AbsoluteDate useableStopTime;

    /** Integer orbit revolution number. */
    private int orbRevNum;

    /** Basis for orbit revolution counter (i.e is first launch/deployment on orbit 0 or 1). */
    private int orbRevNumBasis;

    /** Trajectory element set type. */
    private ElementsType trajType;

    /** Units of trajectory element set. */
    private List<Unit> trajUnits;

    /** Simple constructor.
     * @param epochT0 T0 epoch from file metadata
     * @param dataContext data context
     */
    TrajectoryStateHistoryMetadata(final AbsoluteDate epochT0, final DataContext dataContext) {
        // we don't call the setXxx() methods in order to avoid
        // calling refuseFurtherComments as a side effect
        trajBasis           = "PREDICTED";
        interpolationMethod = InterpolationMethod.HERMITE;
        interpolationDegree = 3;
        orbAveraging        = "OSCULATING";
        center              = new BodyFacade("EARTH",
                                             dataContext.getCelestialBodies().getEarth());
        trajReferenceFrame  = new FrameFacade(dataContext.getFrames().getICRF(),
                                              CelestialBodyFrame.ICRF, null, null,
                                              CelestialBodyFrame.ICRF.name());
        trajFrameEpoch      = epochT0;
        trajType            = ElementsType.CARTPV;
        orbRevNum           = -1;
        orbRevNumBasis      = -1;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        if (trajUnits != null) {
            trajType.checkUnits(trajUnits);
        }
        if (orbRevNum >= 0 && orbRevNumBasis < 0) {
            throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY,
                                      TrajectoryStateHistoryMetadataKey.ORB_REVNUM_BASIS.name());
        }
    }

    /** Get trajectory identification number.
     * @return trajectory identification number
     */
    public String getTrajID() {
        return trajID;
    }

    /** Set trajectory identification number.
     * @param trajID trajectory identification number
     */
    public void setTrajID(final String trajID) {
        refuseFurtherComments();
        this.trajID = trajID;
    }

    /** Get identification number of previous trajectory.
     * @return identification number of previous trajectory
     */
    public String getTrajPrevID() {
        return trajPrevID;
    }

    /** Set identification number of previous trajectory.
     * @param trajPrevID identification number of previous trajectory
     */
    public void setTrajPrevID(final String trajPrevID) {
        refuseFurtherComments();
        this.trajPrevID = trajPrevID;
    }

    /** Get identification number of next trajectory.
     * @return identification number of next trajectory
     */
    public String getTrajNextID() {
        return trajNextID;
    }

    /** Set identification number of next trajectory.
     * @param trajNextID identification number of next trajectory
     */
    public void setTrajNextID(final String trajNextID) {
        refuseFurtherComments();
        this.trajNextID = trajNextID;
    }

    /** Get basis of this trajectory state time history data.
     * @return basis of this trajectory state time history data
     */
    public String getTrajBasis() {
        return trajBasis;
    }

    /** Set basis of this trajectory state time history data.
     * @param trajBasis basis of this trajectory state time history data
     */
    public void setTrajBasis(final String trajBasis) {
        refuseFurtherComments();
        this.trajBasis = trajBasis;
    }

    /** Get identification number of the orbit determination or simulation upon which this trajectory is based.
     * @return identification number of the orbit determination or simulation upon which this trajectory is based
     */
    public String getTrajBasisID() {
        return trajBasisID;
    }

    /** Set identification number of the orbit determination or simulation upon which this trajectory is based.
     * @param trajBasisID identification number of the orbit determination or simulation upon which this trajectory is based
     */
    public void setTrajBasisID(final String trajBasisID) {
        refuseFurtherComments();
        this.trajBasisID = trajBasisID;
    }

    /** Get the interpolation method to be used.
     * @return the interpolation method
     */
    public InterpolationMethod getInterpolationMethod() {
        return interpolationMethod;
    }

    /** Set the interpolation method to be used.
     * @param interpolationMethod the interpolation method to be set
     */
    public void setInterpolationMethod(final InterpolationMethod interpolationMethod) {
        refuseFurtherComments();
        this.interpolationMethod = interpolationMethod;
    }

    /** Get the interpolation degree.
     * @return the interpolation degree
     */
    public int getInterpolationDegree() {
        return interpolationDegree;
    }

    /** Set the interpolation degree.
     * @param interpolationDegree the interpolation degree to be set
     */
    public void setInterpolationDegree(final int interpolationDegree) {
        refuseFurtherComments();
        this.interpolationDegree = interpolationDegree;
    }

    /** Get type of averaging (Osculating, mean Brouwer, other.
     * @return type of averaging (Osculating, mean Brouwer, other
     .). */
    public String getOrbAveraging() {
        return orbAveraging;
    }

    /** Set type of averaging (Osculating, mean Brouwer, other.
     * @param orbAveraging type of averaging (Osculating, mean Brouwer, other
     .). */
    public void setOrbAveraging(final String orbAveraging) {
        refuseFurtherComments();
        this.orbAveraging = orbAveraging;
    }

    /** Get the origin of reference frame.
     * @return the origin of reference frame.
     */
    public BodyFacade getCenter() {
        return center;
    }

    /** Set the origin of reference frame.
     * @param center origin of reference frame to be set
     */
    public void setCenter(final BodyFacade center) {
        refuseFurtherComments();
        this.center = center;
    }

    /** Get reference frame of the trajectory.
     * @return reference frame of the trajectory
     */
    public FrameFacade getTrajReferenceFrame() {
        return trajReferenceFrame;
    }

    /** Set reference frame of the trajectory.
     * @param trajReferenceFrame the reference frame to be set
     */
    public void setTrajReferenceFrame(final FrameFacade trajReferenceFrame) {
        refuseFurtherComments();
        this.trajReferenceFrame = trajReferenceFrame;
    }

    /** Get epoch of the {@link #getTrajReferenceFrame() trajectory reference frame}.
     * @return epoch of the {@link #getTrajReferenceFrame() trajectory reference frame}
     */
    public AbsoluteDate getTrajFrameEpoch() {
        return trajFrameEpoch;
    }

    /** Set epoch of the {@link #getTrajReferenceFrame() trajectory reference frame}.
     * @param trajFrameEpoch epoch of the {@link #getTrajReferenceFrame() trajectory reference frame}
     */
    public void setTrajFrameEpoch(final AbsoluteDate trajFrameEpoch) {
        refuseFurtherComments();
        this.trajFrameEpoch = trajFrameEpoch;
    }

    /** Get start of useable time span covered by ephemerides data, it may be
     * necessary to allow for proper interpolation.
     * @return the useable start time
     */
    public AbsoluteDate getUseableStartTime() {
        return useableStartTime;
    }

    /** Set start of useable time span covered by ephemerides data, it may be
     * necessary to allow for proper interpolation.
     * @param useableStartTime the time to be set
     */
    public void setUseableStartTime(final AbsoluteDate useableStartTime) {
        refuseFurtherComments();
        this.useableStartTime = useableStartTime;
    }

    /** Get end of useable time span covered by ephemerides data, it may be
     * necessary to allow for proper interpolation.
     * @return the useable stop time
     */
    public AbsoluteDate getUseableStopTime() {
        return useableStopTime;
    }

    /** Set end of useable time span covered by ephemerides data, it may be
     * necessary to allow for proper interpolation.
     * @param useableStopTime the time to be set
     */
    public void setUseableStopTime(final AbsoluteDate useableStopTime) {
        refuseFurtherComments();
        this.useableStopTime = useableStopTime;
    }

    /** Get the integer orbit revolution number.
     * @return integer orbit revolution number (-1 if not set)
     */
    public int getOrbRevNum() {
        return orbRevNum;
    }

    /** Set the integer orbit revolution number.
     * @param orbRevNum integer orbit revolution number
     */
    public void setOrbRevNum(final int orbRevNum) {
        this.orbRevNum = orbRevNum;
    }

    /** Get the basis for orbit revolution number.
     * <p>
     * This specifies if first launch/deployment is on orbit 0 or 1.
     * </p>
     * @return basis for orbit revolution number (-1 if not set)
     */
    public int getOrbRevNumBasis() {
        return orbRevNumBasis;
    }

    /** Set the basis for orbit revolution number.
     * <p>
     * This specifies if first launch/deployment is on orbit 0 or 1.
     * </p>
     * @param orbRevNumBasis basis for orbit revolution number
     */
    public void setOrbRevNumBasis(final int orbRevNumBasis) {
        this.orbRevNumBasis = orbRevNumBasis;
    }

    /** Get trajectory element set type.
     * @return trajectory element set type
     */
    public ElementsType getTrajType() {
        return trajType;
    }

    /** Set trajectory element set type.
     * @param trajType trajectory element set type
     */
    public void setTrajType(final ElementsType trajType) {
        refuseFurtherComments();
        this.trajType = trajType;
    }

    /** Get trajectory element set units.
     * @return trajectory element set units
     */
    public List<Unit> getTrajUnits() {
        return trajUnits;
    }

    /** Set trajectory element set units.
     * @param trajUnits trajectory element set units
     */
    public void setTrajUnits(final List<Unit> trajUnits) {
        refuseFurtherComments();
        this.trajUnits = trajUnits;
    }

}

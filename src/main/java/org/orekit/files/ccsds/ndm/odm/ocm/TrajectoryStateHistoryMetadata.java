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

package org.orekit.files.ccsds.ndm.odm.ocm;

import java.util.List;

import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.BodyFacade;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
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

    /** Default interpolation method.
     * @since 12.0
     */
    public static final InterpolationMethod DEFAULT_INTERPOLATION_METHOD = InterpolationMethod.HERMITE;

    /** Default interpolation degree.
     * @since 12.0
     */
    public static final int DEFAULT_INTERPOLATION_DEGREE = 3;

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

    /** Orbit propagator used to generate this trajectory.
     * @since 11.2
     */
    private String propagator;

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
    private OrbitElementsType trajType;

    /** Type of averaging (Osculating, mean Brouwer, other...). */
    private String orbAveraging;

    /** Units of trajectory element set. */
    private List<Unit> trajUnits;

    /** Data context.
     * @since 12.0
     */
    private final DataContext dataContext;

    /** Simple constructor.
     * @param epochT0 T0 epoch from file metadata
     * @param dataContext data context
     */
    public TrajectoryStateHistoryMetadata(final AbsoluteDate epochT0, final DataContext dataContext) {
        // we don't call the setXxx() methods in order to avoid
        // calling refuseFurtherComments as a side effect
        trajBasis           = "PREDICTED";
        interpolationMethod = DEFAULT_INTERPOLATION_METHOD;
        interpolationDegree = DEFAULT_INTERPOLATION_DEGREE;
        orbAveraging        = "OSCULATING";
        center              = new BodyFacade("EARTH",
                                             dataContext.getCelestialBodies().getEarth());
        trajReferenceFrame  = new FrameFacade(dataContext.getFrames().getICRF(),
                                              CelestialBodyFrame.ICRF, null, null,
                                              CelestialBodyFrame.ICRF.name());
        trajFrameEpoch      = epochT0;
        trajType            = OrbitElementsType.CARTPV;
        orbRevNum           = -1;
        orbRevNumBasis      = -1;

        this.dataContext    = dataContext;

    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        checkMandatoryEntriesExceptOrbitsCounter(version);
        if (orbRevNum >= 0 && orbRevNumBasis < 0) {
            throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY,
                                      TrajectoryStateHistoryMetadataKey.ORB_REVNUM_BASIS.name());
        }
    }

    /** Check is mandatory entries EXCEPT orbits counters have been initialized.
     * <p>
     * This method should throw an exception if some mandatory entry is missing
     * </p>
     * @param version format version
     */
    private void checkMandatoryEntriesExceptOrbitsCounter(final double version) {
        super.validate(version);
        if (trajType != OrbitElementsType.CARTP   &&
            trajType != OrbitElementsType.CARTPV  &&
            trajType != OrbitElementsType.CARTPVA) {
            checkNotNull(orbAveraging, TrajectoryStateHistoryMetadataKey.ORB_AVERAGING.name());
        }
        if (trajUnits != null) {
            Unit.ensureCompatible(trajType.toString(), trajType.getUnits(), false, trajUnits);
        }
    }

    /** Increments a trajectory ID.
     * <p>
     * The trajectory blocks metadata contains three identifiers ({@code TRAJ_ID},
     * {@code TRAJ_PREV_ID}, {@code TRAJ_NEXT_ID}) that link the various blocks together.
     * This helper method allows to update one identifier based on the value of another
     * identifier. The update is performed by looking for an integer suffix at the end
     * of the {@code original} identifier and incrementing it by one, taking care to use
     * at least the same number of digits. If for example the original identifier is set
     * to {@code trajectory 037}, then the updated identifier will be {@code trajectory 038}.
     * </p>
     * <p>
     * This helper function is intended to be used by ephemeris generators like {@link EphemerisOcmWriter}
     * and {@link StreamingOcmWriter}, allowing users to call only {@link #setTrajBasisID(String)}
     * in the trajectory metadata template. The ephemeris generators call {@code
     * template.setTrajNextID(TrajectoryStateHistoryMetadata.incrementTrajID(template.getTrajID()))}
     * before generating each trajectory block and call both {@code template.setTrajPrevID(template.getTrajID()))}
     * and {@code template.setTrajID(template.getTrajNextID()))} after having generated each block.
     * </p>
     * @param original original ID (may be null)
     * @return incremented ID, or null if original was null
     */
    public static String incrementTrajID(final String original) {

        if (original == null) {
            // no trajectory ID at all
            return null;
        }

        // split the ID into prefix and numerical index
        int end = original.length();
        while (end > 0 && Character.isDigit(original.charAt(end - 1))) {
            --end;
        }
        final String prefix   = original.substring(0, end);
        final int    index    = end < original.length() ? Integer.parseInt(original.substring(end)) : 0;

        // build offset index, taking care to use at least the same number of digits
        final String newIndex = String.format(String.format("%%0%dd", original.length() - end),
                                              index + 1);

        return prefix + newIndex;

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

    /** Get the orbit propagator used to generate this trajectory.
     * @return orbit propagator used to generate this trajectory
     * @since 11.2
     */
    public String getPropagator() {
        return propagator;
    }

    /** Set the orbit propagator used to generate this trajectory.
     * @param propagator orbit propagator used to generate this trajectory
     * @since 11.2
     */
    public void setPropagator(final String propagator) {
        refuseFurtherComments();
        this.propagator = propagator;
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

    /** Get type of averaging (Osculating, mean Brouwer, other.
     * @return type of averaging (Osculating, mean Brouwer, other)
     */
    public String getOrbAveraging() {
        return orbAveraging;
    }

    /** Set type of averaging (Osculating, mean Brouwer, other.
     * @param orbAveraging type of averaging (Osculating, mean Brouwer, other).
     */
    public void setOrbAveraging(final String orbAveraging) {
        refuseFurtherComments();
        this.orbAveraging = orbAveraging;
    }

    /** Get trajectory element set type.
     * @return trajectory element set type
     */
    public OrbitElementsType getTrajType() {
        return trajType;
    }

    /** Set trajectory element set type.
     * @param trajType trajectory element set type
     */
    public void setTrajType(final OrbitElementsType trajType) {
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

    /** Copy the instance, making sure mandatory fields have been initialized.
     * <p>
     * Dates and orbit counter are not copied.
     * </p>
     * @param version format version
     * @return a new copy
     * @since 12.0
     */
    public TrajectoryStateHistoryMetadata copy(final double version) {

        checkMandatoryEntriesExceptOrbitsCounter(version);

        // allocate new instance
        final TrajectoryStateHistoryMetadata copy = new TrajectoryStateHistoryMetadata(trajFrameEpoch, dataContext);

        // copy comments
        for (String comment : getComments()) {
            copy.addComment(comment);
        }

        // copy metadata
        copy.setTrajPrevID(getTrajPrevID());
        copy.setTrajID(getTrajID());
        copy.setTrajNextID(getTrajNextID());
        copy.setTrajBasis(getTrajBasis());
        copy.setTrajBasisID(getTrajBasisID());
        copy.setInterpolationMethod(getInterpolationMethod());
        copy.setInterpolationDegree(getInterpolationDegree());
        copy.setPropagator(getPropagator());
        copy.setCenter(getCenter());
        copy.setTrajReferenceFrame(getTrajReferenceFrame());
        copy.setTrajFrameEpoch(getTrajFrameEpoch());
        copy.setOrbRevNumBasis(getOrbRevNumBasis());
        copy.setOrbAveraging(getOrbAveraging());
        copy.setTrajType(getTrajType());
        copy.setTrajUnits(getTrajUnits());

        return copy;

    }

}

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

import java.util.Collections;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.BodyFacade;
import org.orekit.files.ccsds.definitions.DutyCycleType;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.OrbitRelativeFrame;
import org.orekit.files.ccsds.definitions.SpacecraftBodyFrame;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.units.Unit;

/** Metadata for maneuver history.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OrbitManeuverHistoryMetadata extends CommentsContainer {

    /** Default duty cycle type.
     * @since 12.0
     */
    public static final DutyCycleType DEFAULT_DC_TYPE = DutyCycleType.CONTINUOUS;

    /** Maneuver identification number. */
    private String manID;

    /** Identification number of previous maneuver. */
    private String manPrevID;

    /** Identification number of next maneuver. */
    private String manNextID;

    /** Basis of this maneuver history data. */
    private ManBasis manBasis;

    /** Identification number of the orbit determination or simulation upon which this maneuver is based. */
    private String manBasisID;

    /** Identifier of the device used for this maneuver. */
    private String manDeviceID;

    /** Completion time of previous maneuver. */
    private AbsoluteDate manPrevEpoch;

    /** Start time of next maneuver. */
    private AbsoluteDate manNextEpoch;

    /** Reference frame of the maneuver. */
    private FrameFacade manReferenceFrame;

    /** Epoch of the maneuver reference frame. */
    private AbsoluteDate manFrameEpoch;

    /** Purposes of the maneuver. */
    private List<String> manPurpose;

    /** Prediction source on which this maneuver is based. */
    private String manPredSource;

    /** Origin of maneuver gravitational assist body. */
    private BodyFacade gravitationalAssist;

    /** Type of duty cycle. */
    private DutyCycleType dcType;

    /** Start time of duty cycle-based maneuver window. */
    private AbsoluteDate dcWindowOpen;

    /** End time of duty cycle-based maneuver window. */
    private AbsoluteDate dcWindowClose;

    /** Minimum number of "ON" duty cycles. */
    private int dcMinCycles;

    /** Maximum number of "ON" duty cycles. */
    private int dcMaxCycles;

    /** Start time of initial duty cycle-based maneuver execution. */
    private AbsoluteDate dcExecStart;

    /** End time of final duty cycle-based maneuver execution. */
    private AbsoluteDate dcExecStop;

    /** Duty cycle thrust reference time. */
    private AbsoluteDate dcRefTime;

    /** Duty cycle pulse "ON" duration. */
    private double dcTimePulseDuration;

    /** Duty cycle elapsed time between start of a pulse and start of next pulse. */
    private double dcTimePulsePeriod;

    /** Reference direction for triggering duty cycle. */
    private Vector3D dcRefDir;

    /** Spacecraft body frame in which {@link #dcBodyTrigger} is specified. */
    private SpacecraftBodyFrame dcBodyFrame;

    /** Direction in {@link #dcBodyFrame body frame} for triggering duty cycle. */
    private Vector3D dcBodyTrigger;

    /** Phase angle of pulse start. */
    private double dcPhaseStartAngle;

    /** Phase angle of pulse stop. */
    private double dcPhaseStopAngle;

    /** Maneuver elements of information. */
    private List<ManeuverFieldType> manComposition;

    /** Units of covariance element set. */
    private List<Unit> manUnits;

    /** Simple constructor.
     * @param epochT0 T0 epoch from file metadata
     */
    public OrbitManeuverHistoryMetadata(final AbsoluteDate epochT0) {
        // we don't call the setXxx() methods in order to avoid
        // calling refuseFurtherComments as a side effect
        manBasis            = ManBasis.PLANNED;
        manReferenceFrame   = new FrameFacade(null, null,
                                              OrbitRelativeFrame.TNW_INERTIAL, null,
                                              OrbitRelativeFrame.TNW_INERTIAL.name());
        manFrameEpoch       = epochT0;
        manPurpose          = Collections.emptyList();
        dcType              = DEFAULT_DC_TYPE;
        dcMinCycles         = -1;
        dcMaxCycles         = -1;
        dcTimePulseDuration = Double.NaN;
        dcTimePulsePeriod   = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        checkNotNull(manID,          OrbitManeuverHistoryMetadataKey.MAN_ID.name());
        checkNotNull(manDeviceID,    OrbitManeuverHistoryMetadataKey.MAN_DEVICE_ID.name());

        if (dcType != DutyCycleType.CONTINUOUS) {
            checkNotNull(dcWindowOpen,       OrbitManeuverHistoryMetadataKey.DC_WIN_OPEN.name());
            checkNotNull(dcWindowClose,      OrbitManeuverHistoryMetadataKey.DC_WIN_CLOSE.name());
            checkNotNull(dcExecStart,        OrbitManeuverHistoryMetadataKey.DC_EXEC_START.name());
            checkNotNull(dcExecStop,         OrbitManeuverHistoryMetadataKey.DC_EXEC_STOP.name());
            checkNotNull(dcRefTime,          OrbitManeuverHistoryMetadataKey.DC_REF_TIME.name());
            checkNotNaN(dcTimePulseDuration, OrbitManeuverHistoryMetadataKey.DC_TIME_PULSE_DURATION.name());
            checkNotNaN(dcTimePulsePeriod,   OrbitManeuverHistoryMetadataKey.DC_TIME_PULSE_PERIOD.name());
        }
        if (dcType == DutyCycleType.TIME_AND_ANGLE) {
            checkNotNull(dcRefDir,           OrbitManeuverHistoryMetadataKey.DC_REF_DIR.name());
            checkNotNull(dcBodyFrame,        OrbitManeuverHistoryMetadataKey.DC_BODY_FRAME.name());
            checkNotNull(dcBodyTrigger,      OrbitManeuverHistoryMetadataKey.DC_BODY_TRIGGER.name());
            checkNotNull(dcPhaseStartAngle,  OrbitManeuverHistoryMetadataKey.DC_PA_START_ANGLE.name());
            checkNotNull(dcPhaseStopAngle,   OrbitManeuverHistoryMetadataKey.DC_PA_STOP_ANGLE.name());
        }

        checkNotNull(manComposition, OrbitManeuverHistoryMetadataKey.MAN_COMPOSITION.name());
        if (!manComposition.get(0).isTime()) {
            throw new OrekitException(OrekitMessages.CCSDS_MANEUVER_MISSING_TIME, manID);
        }
        final int firstNonTime = (manComposition.size() > 1 && manComposition.get(1).isTime()) ? 2 : 1;

        if (manUnits != null) {
            if (manUnits.size() != manComposition.size() - firstNonTime) {
                throw new OrekitException(OrekitMessages.CCSDS_MANEUVER_UNITS_WRONG_NB_COMPONENTS,
                                          manID);
            }
            for (int i = 0; i < manUnits.size(); ++i) {
                manComposition.get(firstNonTime + i).checkUnit(manUnits.get(i));
            }
        }
    }

    /** Check if a field is a time field.
     * @param Ma
     */
    /** Get maneuver identification number.
     * @return maneuver identification number
     */
    public String getManID() {
        return manID;
    }

    /** Set maneuver identification number.
     * @param manID maneuver identification number
     */
    public void setManID(final String manID) {
        refuseFurtherComments();
        this.manID = manID;
    }

    /** Get identification number of previous maneuver.
     * @return identification number of previous maneuver
     */
    public String getManPrevID() {
        return manPrevID;
    }

    /** Set identification number of previous maneuver.
     * @param manPrevID identification number of previous maneuver
     */
    public void setManPrevID(final String manPrevID) {
        refuseFurtherComments();
        this.manPrevID = manPrevID;
    }

    /** Get identification number of next maneuver.
     * @return identification number of next maneuver
     */
    public String getManNextID() {
        return manNextID;
    }

    /** Set identification number of next maneuver.
     * @param manNextID identification number of next maneuver
     */
    public void setManNextID(final String manNextID) {
        refuseFurtherComments();
        this.manNextID = manNextID;
    }

    /** Get basis of this maneuver history data.
     * @return basis of this maneuver history data
     */
    public ManBasis getManBasis() {
        return manBasis;
    }

    /** Set basis of this maneuver history data.
     * @param manBasis basis of this maneuver history data
     */
    public void setManBasis(final ManBasis manBasis) {
        refuseFurtherComments();
        this.manBasis = manBasis;
    }

    /** Get identification number of the orbit determination or simulation upon which this maneuver is based.
     * @return identification number of the orbit determination or simulation upon which this maneuver is based
     */
    public String getManBasisID() {
        return manBasisID;
    }

    /** Set identification number of the orbit determination or simulation upon which this maneuver is based.
     * @param manBasisID identification number of the orbit determination or simulation upon which this maneuver is based
     */
    public void setManBasisID(final String manBasisID) {
        refuseFurtherComments();
        this.manBasisID = manBasisID;
    }

    /** Get identifier of the device used for this maneuver.
     * @return identifier of the device used for this maneuver
     */
    public String getManDeviceID() {
        return manDeviceID;
    }

    /** Set identifier of the device used for this maneuver.
     * @param manDeviceID identifier of the device used for this maneuver
     */
    public void setManDeviceID(final String manDeviceID) {
        refuseFurtherComments();
        this.manDeviceID = manDeviceID;
    }

    /** Get completion time of previous maneuver.
     * @return completion time of previous maneuver
     */
    public AbsoluteDate getManPrevEpoch() {
        return manPrevEpoch;
    }

    /** Set completion time of previous maneuver.
     * @param manPrevEpoch completion time of previous maneuver
     */
    public void setManPrevEpoch(final AbsoluteDate manPrevEpoch) {
        refuseFurtherComments();
        this.manPrevEpoch = manPrevEpoch;
    }

    /** Get start time of next maneuver.
     * @return start time of next maneuver
     */
    public AbsoluteDate getManNextEpoch() {
        return manNextEpoch;
    }

    /** Set start time of next maneuver.
     * @param manNextEpoch start time of next maneuver
     */
    public void setManNextEpoch(final AbsoluteDate manNextEpoch) {
        refuseFurtherComments();
        this.manNextEpoch = manNextEpoch;
    }

    /** Get the purposes of the maneuver.
     * @return purposes of the maneuver
     */
    public List<String> getManPurpose() {
        return manPurpose;
    }

    /** Set the purposes of the maneuver.
     * @param manPurpose purposes of the maneuver
     */
    public void setManPurpose(final List<String> manPurpose) {
        this.manPurpose = manPurpose;
    }

    /** Get prediction source on which this maneuver is based.
     * @return prediction source on which this maneuver is based
     */
    public String getManPredSource() {
        return manPredSource;
    }

    /** Set prediction source on which this maneuver is based.
     * @param manPredSource prediction source on which this maneuver is based
     */
    public void setManPredSource(final String manPredSource) {
        refuseFurtherComments();
        this.manPredSource = manPredSource;
    }

    /** Get reference frame of the maneuver.
     * @return reference frame of the maneuver
     */
    public FrameFacade getManReferenceFrame() {
        return manReferenceFrame;
    }

    /** Set reference frame of the maneuver.
     * @param manReferenceFrame the reference frame to be set
     */
    public void setManReferenceFrame(final FrameFacade manReferenceFrame) {
        refuseFurtherComments();
        this.manReferenceFrame = manReferenceFrame;
    }

    /** Get epoch of the {@link #getManReferenceFrame() maneuver reference frame}.
     * @return epoch of the {@link #getManReferenceFrame() maneuver reference frame}
     */
    public AbsoluteDate getManFrameEpoch() {
        return manFrameEpoch;
    }

    /** Set epoch of the {@link #getManReferenceFrame() maneuver reference frame}.
     * @param manFrameEpoch epoch of the {@link #getManReferenceFrame() maneuver reference frame}
     */
    public void setManFrameEpoch(final AbsoluteDate manFrameEpoch) {
        refuseFurtherComments();
        this.manFrameEpoch = manFrameEpoch;
    }

    /** Get the origin of gravitational assist.
     * @return the origin of gravitational assist.
     */
    public BodyFacade getGravitationalAssist() {
        return gravitationalAssist;
    }

    /** Set the origin of gravitational assist.
     * @param gravitationalAssist origin of gravitational assist to be set
     */
    public void setGravitationalAssist(final BodyFacade gravitationalAssist) {
        refuseFurtherComments();
        this.gravitationalAssist = gravitationalAssist;
    }

    /** Get type of duty cycle.
     * @return type of duty cycle
     */
    public DutyCycleType getDcType() {
        return dcType;
    }

    /** Set type of duty cycle.
     * @param dcType type of duty cycle
     */
    public void setDcType(final DutyCycleType dcType) {
        this.dcType = dcType;
    }

    /** Get the start time of duty cycle-based maneuver window.
     * @return start time of duty cycle-based maneuver window
     */
    public AbsoluteDate getDcWindowOpen() {
        return dcWindowOpen;
    }

    /** Set the start time of duty cycle-based maneuver window.
     * @param dcWindowOpen start time of duty cycle-based maneuver window
     */
    public void setDcWindowOpen(final AbsoluteDate dcWindowOpen) {
        this.dcWindowOpen = dcWindowOpen;
    }

    /** Get the end time of duty cycle-based maneuver window.
     * @return end time of duty cycle-based maneuver window
     */
    public AbsoluteDate getDcWindowClose() {
        return dcWindowClose;
    }

    /** Set the end time of duty cycle-based maneuver window.
     * @param dcWindowClose end time of duty cycle-based maneuver window
     */
    public void setDcWindowClose(final AbsoluteDate dcWindowClose) {
        this.dcWindowClose = dcWindowClose;
    }

    /** Get the minimum number of "ON" duty cycles.
     * @return minimum number of "ON" duty cycles (-1 if not set)
     */
    public int getDcMinCycles() {
        return dcMinCycles;
    }

    /** Set the minimum number of "ON" duty cycles.
     * @param dcMinCycles minimum number of "ON" duty cycles
     */
    public void setDcMinCycles(final int dcMinCycles) {
        this.dcMinCycles = dcMinCycles;
    }

    /** Get the maximum number of "ON" duty cycles.
     * @return maximum number of "ON" duty cycles (-1 if not set)
     */
    public int getDcMaxCycles() {
        return dcMaxCycles;
    }

    /** Set the maximum number of "ON" duty cycles.
     * @param dcMaxCycles maximum number of "ON" duty cycles
     */
    public void setDcMaxCycles(final int dcMaxCycles) {
        this.dcMaxCycles = dcMaxCycles;
    }

    /** Get the start time of initial duty cycle-based maneuver execution.
     * @return start time of initial duty cycle-based maneuver execution
     */
    public AbsoluteDate getDcExecStart() {
        return dcExecStart;
    }

    /** Set the start time of initial duty cycle-based maneuver execution.
     * @param dcExecStart start time of initial duty cycle-based maneuver execution
     */
    public void setDcExecStart(final AbsoluteDate dcExecStart) {
        this.dcExecStart = dcExecStart;
    }

    /** Get the end time of final duty cycle-based maneuver execution.
     * @return end time of final duty cycle-based maneuver execution
     */
    public AbsoluteDate getDcExecStop() {
        return dcExecStop;
    }

    /** Set the end time of final duty cycle-based maneuver execution.
     * @param dcExecStop end time of final duty cycle-based maneuver execution
     */
    public void setDcExecStop(final AbsoluteDate dcExecStop) {
        this.dcExecStop = dcExecStop;
    }

    /** Get duty cycle thrust reference time.
     * @return duty cycle thrust reference time
     */
    public AbsoluteDate getDcRefTime() {
        return dcRefTime;
    }

    /** Set duty cycle thrust reference time.
     * @param dcRefTime duty cycle thrust reference time
     */
    public void setDcRefTime(final AbsoluteDate dcRefTime) {
        this.dcRefTime = dcRefTime;
    }

    /** Get duty cycle pulse "ON" duration.
     * @return duty cycle pulse "ON" duration
     */
    public double getDcTimePulseDuration() {
        return dcTimePulseDuration;
    }

    /** Set duty cycle pulse "ON" duration.
     * @param dcTimePulseDuration duty cycle pulse "ON" duration
     */
    public void setDcTimePulseDuration(final double dcTimePulseDuration) {
        this.dcTimePulseDuration = dcTimePulseDuration;
    }

    /** Get duty cycle elapsed time between start of a pulse and start of next pulse.
     * @return duty cycle elapsed time between start of a pulse and start of next pulse
     */
    public double getDcTimePulsePeriod() {
        return dcTimePulsePeriod;
    }

    /** Set duty cycle elapsed time between start of a pulse and start of next pulse.
     * @param dcTimePulsePeriod duty cycle elapsed time between start of a pulse and start of next pulse
     */
    public void setDcTimePulsePeriod(final double dcTimePulsePeriod) {
        this.dcTimePulsePeriod = dcTimePulsePeriod;
    }

    /** Get reference direction for triggering duty cycle.
     * @return reference direction for triggering duty cycle
     */
    public Vector3D getDcRefDir() {
        return dcRefDir;
    }

    /** Set reference direction for triggering duty cycle.
     * @param dcRefDir reference direction for triggering duty cycle
     */
    public void setDcRefDir(final Vector3D dcRefDir) {
        this.dcRefDir = dcRefDir;
    }

    /** Get spacecraft body frame in which {@link #getDcBodyTrigger()} is specified.
     * @return spacecraft body frame in which {@link #getDcBodyTrigger()} is specified
     */
    public SpacecraftBodyFrame getDcBodyFrame() {
        return dcBodyFrame;
    }

    /** Set spacecraft body frame in which {@link #getDcBodyTrigger()} is specified.
     * @param dcBodyFrame spacecraft body frame in which {@link #getDcBodyTrigger()} is specified
     */
    public void setDcBodyFrame(final SpacecraftBodyFrame dcBodyFrame) {
        this.dcBodyFrame = dcBodyFrame;
    }

    /** Get direction in {@link #getDcBodyFrame() body frame} for triggering duty cycle.
     * @return direction in {@link #getDcBodyFrame() body frame} for triggering duty cycle
     */
    public Vector3D getDcBodyTrigger() {
        return  dcBodyTrigger;
    }

    /** Set direction in {@link #getDcBodyFrame() body frame} for triggering duty cycle.
     * @param dcBodyTrigger direction in {@link #getDcBodyFrame() body frame} for triggering duty cycle
     */
    public void setDcBodyTrigger(final Vector3D dcBodyTrigger) {
        this.dcBodyTrigger = dcBodyTrigger;
    }

    /** Get phase angle of pulse start.
     * @return phase angle of pulse start
     */
    public double getDcPhaseStartAngle() {
        return dcPhaseStartAngle;
    }

    /** Set phase angle of pulse start.
     * @param dcPhaseStartAngle phase angle of pulse start
     */
    public void setDcPhaseStartAngle(final double dcPhaseStartAngle) {
        this.dcPhaseStartAngle = dcPhaseStartAngle;
    }

    /** Get phase angle of pulse stop.
     * @return phase angle of pulse stop
     */
    public double getDcPhaseStopAngle() {
        return dcPhaseStopAngle;
    }

    /** Set phase angle of pulse stop.
     * @param dcPhaseStopAngle phase angle of pulse stop
     */
    public void setDcPhaseStopAngle(final double dcPhaseStopAngle) {
        this.dcPhaseStopAngle = dcPhaseStopAngle;
    }

    /** Get maneuver elements of information.
     * @return maneuver element of information
     */
    public List<ManeuverFieldType> getManComposition() {
        return manComposition;
    }

    /** Set maneuver element of information.
     * @param manComposition maneuver element of information
     */
    public void setManComposition(final List<ManeuverFieldType> manComposition) {
        refuseFurtherComments();
        this.manComposition = manComposition;
    }

    /** Get maneuver elements of information units.
     * @return maneuver element of information units
     */
    public List<Unit> getManUnits() {
        return manUnits;
    }

    /** Set maneuver element of information units.
     * @param manUnits maneuver element of information units
     */
    public void setManUnits(final List<Unit> manUnits) {
        refuseFurtherComments();
        this.manUnits = manUnits;
    }

}

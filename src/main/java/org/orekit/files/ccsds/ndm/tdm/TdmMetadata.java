/* Copyright 2002-2021 CS GROUP
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

package org.orekit.files.ccsds.ndm.tdm;

import java.util.Map;
import java.util.TreeMap;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.section.Metadata;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/** The TDMMetadata class gathers the meta-data present in the Tracking Data Message (TDM).<p>
 *  References:<p>
 *  <a href="https://public.ccsds.org/Pubs/503x0b1c1.pdf">CCSDS 503.0-B-1 recommended standard</a>. §3.3 ("Tracking Data Message", Blue Book, Version 1.0, November 2007).
 *
 * @author Maxime Journot
 * @since 9.0
 */
public class TdmMetadata extends Metadata {

    /** Start epoch of total time span covered by observations block. */
    private AbsoluteDate startTime;

    /** End epoch of total time span covered by observations block. */
    private AbsoluteDate stopTime;

    /** Map of participants in the tracking data session (minimum 1 and up to 5).<p>
     *  Participants may include ground stations, spacecraft, and/or quasars.<p>
     *  Participants represent the classical transmitting parties, transponding parties, and receiving parties.
     */
    private Map<Integer, String> participants;

    /** Tracking mode. */
    private TrackingMode mode;

    /** The path shall reflect the signal path by listing the index of each participant
     *  in order, separated by commas, with no inserted white space.<p>
     *  The integers 1, 2, 3, 4, 5 used to specify the signal path correlate
     *  with the indices of the PARTICIPANT keywords.<p>
     *  The first entry in the PATH shall be the transmit participant.<p>
     *  The non-indexed ‘PATH’ keyword shall be used if the MODE is ‘SEQUENTIAL’.<p>
     *  The indexed ‘PATH_1’ and ‘PATH_2’ keywords shall be used where the MODE is ‘SINGLE_DIFF’.
     */
    private int[] path;

    /** Path 1 (see above). */
    private int[] path1;

    /** Path 2 (see above). */
    private int[] path2;

    /** Frequency band for transmitted frequencies. */
    private String transmitBand;

    /** Frequency band for received frequencies. */
    private String receiveBand;

    /** Turn-around ratio numerator.<p>
     *  Numerator of the turn-around ratio that is necessary to calculate the coherent downlink from the uplink frequency.
     */
    private int turnaroundNumerator;

    /** Turn-around ratio denominator .*/
    private int turnaroundDenominator;

    /** Timetag reference.<p>
     *  Provides a reference for time tags in the tracking data.<p>
     *  It indicates whether the timetag associated with the data is the transmit time or the receive time.
     */
    private TimetagReference timetagRef;

    /** Integration interval. <p>
     *  Provides the Doppler count time in seconds for Doppler data or for the creation
     *  of normal points.
     */
    private double integrationInterval;

    /** Integration reference.<p>
     *  Used in conjunction with timetag reference and integration interval.<p>
     *  Indicates whether the timetag represents the start, middle or end of the integration interval.
     */
    private IntegrationReference integrationRef;

    /** Frequency offset.<p>
     *  A frequency in Hz that must be added to every RECEIVE_FREQ to reconstruct it.
     */
    private double freqOffset;

    /** Range mode. */
    private RangeMode rangeMode;

    /** Raw range modulus (in RangeUnits). */
    private double rawRangeModulus;

    /** Range units. */
    private RangeUnits rangeUnits;

    /** Angle type. */
    private AngleType angleType;

    /** Reference frame in which data are given: used in combination with ANGLE_TYPE=RADEC. */
    private FrameFacade referenceFrame;

    /** Transmit delays map.<p>
     *  Specifies a fixed interval of time, in seconds, for the signal to travel from the transmitting
     *  electronics to the transmit point. Each item in the list corresponds to the each participants.
     */
    private Map<Integer, Double> transmitDelays;

    /** Receive delays list.<p>
     *  Specifies a fixed interval of time, in seconds, for the signal to travel from the tracking
     *  point to the receiving electronics. Each item in the list corresponds to the each participants.
     */
    private Map<Integer, Double> receiveDelays;

    /** Data quality. */
    private DataQuality dataQuality;

    /** Correction angle 1.<p>
     *  Angle correction that has been added or should be added to the ANGLE_1 data.
     */
    private double correctionAngle1;

    /** Correction angle 2.<p>
     *  Angle correction that has been added or should be added to the ANGLE_2 data.
     */
    private double correctionAngle2;

    /** Correction Doppler.<p>
     *  Doppler correction that has been added or should be added to the DOPPLER data.
     */
    private double correctionDoppler;

    /** Raw correction Range in {@link #getRangeUnits()}.<p>
     *  Range correction that has been added or should be added to the RANGE data.
     */
    private double rawCorrectionRange;

    /** Correction receive.<p>
     *  Receive correction that has been added or should be added to the RECEIVE data.
     */
    private double correctionReceive;

    /** Correction transmit.<p>
     *  Transmit correction that has been added or should be added to the TRANSMIT data.
     */
    private double correctionTransmit;

    /** Correction applied ? YES/NO<p>
     *  Indicate whethers or not the values associated with the CORRECTION_* keywords have been
     *  applied to the tracking data.
     */
    private CorrectionApplied correctionsApplied;

    /** Create a new TDM meta-data.
     */
    public TdmMetadata() {
        super(null);
        participants   = new TreeMap<>();
        transmitDelays = new TreeMap<>();
        receiveDelays  = new TreeMap<>();
    }

    /** {@inheritDoc} */
    @Override
    public void checkMandatoryEntries() {
        super.checkMandatoryEntries();
        if (participants.isEmpty()) {
            throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, TdmMetadataKey.PARTICIPANT_1);
        }
    }

    /** Getter for the startTime.
     * @return the startTime
     */
    public AbsoluteDate getStartTime() {
        return startTime;
    }

    /** Setter for the startTime.
     * @param startTime the startTime to set
     */
    public void setStartTime(final AbsoluteDate startTime) {
        refuseFurtherComments();
        this.startTime = startTime;
    }

    /** Getter for the stopTime.
     * @return the stopTime
     */
    public AbsoluteDate getStopTime() {
        return stopTime;
    }

    /** Setter for the stopTime.
     * @param stopTime the stopTime to set
     */
    public void setStopTime(final AbsoluteDate stopTime) {
        refuseFurtherComments();
        this.stopTime = stopTime;
    }

    /** Getter for the participants.
     * @return the participants
     */
    public Map<Integer, String> getParticipants() {
        return participants;
    }

    /** Setter for the participants.
     * @param participants the participants to set
     */
    public void setParticipants(final Map<Integer, String> participants) {
        refuseFurtherComments();
        this.participants = new TreeMap<Integer, String>();
        this.participants.putAll(participants);
    }

    /** Adds a participant to the list.
     * @param participantNumber the number of the participant to add
     * @param participant the name of the participant to add
     */
    public void addParticipant(final int participantNumber, final String participant) {
        refuseFurtherComments();
        this.participants.put(participantNumber, participant);
    }

    /** Getter for the mode.
     * @return the mode
     */
    public TrackingMode getMode() {
        return mode;
    }

    /** Setter for the mode.
     * @param mode the mode to set
     */
    public void setMode(final TrackingMode mode) {
        refuseFurtherComments();
        this.mode = mode;
    }

    /** Getter for the path.
     * @return the path
     */
    public int[] getPath() {
        return path.clone();
    }

    /** Setter for the path.
     * @param path the path to set
     */
    public void setPath(final int[] path) {
        refuseFurtherComments();
        this.path = path.clone();
    }

    /** Getter for the path1.
     * @return the path1
     */
    public int[] getPath1() {
        return path1.clone();
    }

    /** Setter for the path1.
     * @param path1 the path1 to set
     */
    public void setPath1(final int[] path1) {
        refuseFurtherComments();
        this.path1 = path1.clone();
    }

    /** Getter for the path2.
     * @return the path2
     */
    public int[] getPath2() {
        return path2.clone();
    }

    /** Setter for the path2.
     * @param path2 the path2 to set
     */
    public void setPath2(final int[] path2) {
        refuseFurtherComments();
        this.path2 = path2.clone();
    }

    /** Getter for the transmitBand.
     * @return the transmitBand
     */
    public String getTransmitBand() {
        return transmitBand;
    }

    /** Setter for the transmitBand.
     * @param transmitBand the transmitBand to set
     */
    public void setTransmitBand(final String transmitBand) {
        refuseFurtherComments();
        this.transmitBand = transmitBand;
    }

    /** Getter for the receiveBand.
     * @return the receiveBand
     */
    public String getReceiveBand() {
        return receiveBand;
    }

    /** Setter for the receiveBand.
     * @param receiveBand the receiveBand to set
     */
    public void setReceiveBand(final String receiveBand) {
        refuseFurtherComments();
        this.receiveBand = receiveBand;
    }

    /** Getter for the turnaroundNumerator.
     * @return the turnaroundNumerator
     */
    public int getTurnaroundNumerator() {
        return turnaroundNumerator;
    }

    /** Setter for the turnaroundNumerator.
     * @param turnaroundNumerator the turnaroundNumerator to set
     */
    public void setTurnaroundNumerator(final int turnaroundNumerator) {
        refuseFurtherComments();
        this.turnaroundNumerator = turnaroundNumerator;
    }

    /** Getter for the turnaroundDenominator.
     * @return the turnaroundDenominator
     */
    public int getTurnaroundDenominator() {
        return turnaroundDenominator;
    }

    /** Setter for the turnaroundDenominator.
     * @param turnaroundDenominator the turnaroundDenominator to set
     */
    public void setTurnaroundDenominator(final int turnaroundDenominator) {
        refuseFurtherComments();
        this.turnaroundDenominator = turnaroundDenominator;
    }

    /** Getter for the timetagRef.
     * @return the timetagRef
     */
    public TimetagReference getTimetagRef() {
        return timetagRef;
    }

    /** Setter for the timetagRef.
     * @param timetagRef the timetagRef to set
     */
    public void setTimetagRef(final TimetagReference timetagRef) {
        refuseFurtherComments();
        this.timetagRef = timetagRef;
    }

    /** Getter for the integrationInterval.
     * @return the integrationInterval
     */
    public double getIntegrationInterval() {
        return integrationInterval;
    }

    /** Setter for the integrationInterval.
     * @param integrationInterval the integrationInterval to set
     */
    public void setIntegrationInterval(final double integrationInterval) {
        refuseFurtherComments();
        this.integrationInterval = integrationInterval;
    }

    /** Getter for the integrationRef.
     * @return the integrationRef
     */
    public IntegrationReference getIntegrationRef() {
        return integrationRef;
    }

    /** Setter for the integrationRef.
     * @param integrationRef the integrationRef to set
     */
    public void setIntegrationRef(final IntegrationReference integrationRef) {
        refuseFurtherComments();
        this.integrationRef = integrationRef;
    }

    /** Getter for the freqOffset.
     * @return the freqOffset
     */
    public double getFreqOffset() {
        return freqOffset;
    }

    /** Setter for the freqOffset.
     * @param freqOffset the freqOffset to set
     */
    public void setFreqOffset(final double freqOffset) {
        refuseFurtherComments();
        this.freqOffset = freqOffset;
    }

    /** Getter for the rangeMode.
     * @return the rangeMode
     */
    public RangeMode getRangeMode() {
        return rangeMode;
    }

    /** Setter for the rangeMode.
     * @param rangeMode the rangeMode to set
     */
    public void setRangeMode(final RangeMode rangeMode) {
        refuseFurtherComments();
        this.rangeMode = rangeMode;
    }

    /** Getter for the range modulus in meters.
     * @param converter converter to use if {@link #getRangeUnits() range units}
     * are set to {@link RangeUnits#RU}
     * @return the range modulus in meters
     */
    public double getRangeModulus(final RangeUnitsConverter converter) {
        if (rangeUnits == RangeUnits.km) {
            return rawRangeModulus * 1000;
        } else if (rangeUnits == RangeUnits.s) {
            return rawRangeModulus * Constants.SPEED_OF_LIGHT;
        } else {
            return converter.ruToMeters(this, startTime, rawRangeModulus);
        }
    }

    /** Getter for the raw range modulus.
     * @return the raw range modulus in range units
     */
    public double getRawRangeModulus() {
        return rawRangeModulus;
    }

    /** Setter for the raw range modulus.
     * @param rawRangeModulus the raw range modulus to set
     */
    public void setRawRangeModulus(final double rawRangeModulus) {
        refuseFurtherComments();
        this.rawRangeModulus = rawRangeModulus;
    }

    /** Getter for the rangeUnits.
     * @return the rangeUnits
     */
    public RangeUnits getRangeUnits() {
        return rangeUnits;
    }

    /** Setter for the rangeUnits.
     * @param rangeUnits the rangeUnits to set
     */
    public void setRangeUnits(final RangeUnits rangeUnits) {
        refuseFurtherComments();
        this.rangeUnits = rangeUnits;
    }

    /** Getter for angleType.
     * @return the angleType
     */
    public AngleType getAngleType() {
        return angleType;
    }

    /** Setter for the angleType.
     * @param angleType the angleType to set
     */
    public void setAngleType(final AngleType angleType) {
        refuseFurtherComments();
        this.angleType = angleType;
    }

    /** Get the the value of {@code REFERENCE_FRAME} as an Orekit {@link Frame}.
     * @return The reference frame specified by the {@code REFERENCE_FRAME} keyword.
     */
    public FrameFacade getReferenceFrame() {
        return referenceFrame;
    }

    /** Set the reference frame in which data are given: used for RADEC tracking data.
     * @param referenceFrame the reference frame to be set
     */
    public void setReferenceFrame(final FrameFacade referenceFrame) {
        refuseFurtherComments();
        this.referenceFrame = referenceFrame;
    }

    /** Getter for the transmitDelays.
     * @return the transmitDelays
     */
    public Map<Integer, Double> getTransmitDelays() {
        return transmitDelays;
    }

    /** Setter for the transmitDelays.
     * @param transmitDelays the transmitDelays to set
     */
    public void setTransmitDelays(final Map<Integer, Double> transmitDelays) {
        refuseFurtherComments();
        this.transmitDelays = new TreeMap<Integer, Double>();
        this.transmitDelays.putAll(transmitDelays);
    }

    /** Adds a transmit delay to the list.
     *  @param participantNumber the number of the participants for which the transmit delay is given
     *  @param transmitDelay the transmit delay value to add
     */
    public void addTransmitDelay(final int participantNumber, final double transmitDelay) {
        refuseFurtherComments();
        this.transmitDelays.put(participantNumber, transmitDelay);
    }

    /** Getter for receiveDelays.
     * @return the receiveDelays
     */
    public Map<Integer, Double> getReceiveDelays() {
        return receiveDelays;
    }

    /** Setter for the receiveDelays.
     * @param receiveDelays the receiveDelays to set
     */
    public void setReceiveDelays(final Map<Integer, Double> receiveDelays) {
        refuseFurtherComments();
        this.receiveDelays = new TreeMap<Integer, Double>();
        this.receiveDelays.putAll(receiveDelays);
    }

    /** Adds a receive delay to the list.
     * @param participantNumber the number of the participants for which the receive delay is given
     * @param receiveDelay the receive delay value to add
     */
    public void addReceiveDelay(final int participantNumber, final double receiveDelay) {
        refuseFurtherComments();
        this.receiveDelays.put(participantNumber, receiveDelay);
    }
    /** Getter for the dataQuality.
     * @return the dataQuality
     */
    public DataQuality getDataQuality() {
        return dataQuality;
    }

    /** Setter for the dataQuality.
     * @param dataQuality the dataQuality to set
     */
    public void setDataQuality(final DataQuality dataQuality) {
        refuseFurtherComments();
        this.dataQuality = dataQuality;
    }

    /** Getter for the correctionAngle1.
     * @return the correctionAngle1 (in radians)
     */
    public double getCorrectionAngle1() {
        return correctionAngle1;
    }

    /** Setter for the correctionAngle1.
     * @param correctionAngle1 the correctionAngle1 to set (in radians)
     */
    public void setCorrectionAngle1(final double correctionAngle1) {
        refuseFurtherComments();
        this.correctionAngle1 = correctionAngle1;
    }

    /** Getter for the correctionAngle2.
     * @return the correctionAngle2 (in radians)
     */
    public double getCorrectionAngle2() {
        return correctionAngle2;
    }

    /** Setter for the correctionAngle2.
     * @param correctionAngle2 the correctionAngle2 to set (in radians)
     */
    public void setCorrectionAngle2(final double correctionAngle2) {
        refuseFurtherComments();
        this.correctionAngle2 = correctionAngle2;
    }

    /** Getter for the correctionDoppler.
     * @return the correctionDoppler (in m/s)
     */
    public double getCorrectionDoppler() {
        return correctionDoppler;
    }

    /** Setter for the correctionDoppler.
     * @param correctionDoppler the correctionDoppler to set (in m/s)
     */
    public void setCorrectionDoppler(final double correctionDoppler) {
        refuseFurtherComments();
        this.correctionDoppler = correctionDoppler;
    }

    /** Getter for the raw correction for range in meters.
     * @param converter converter to use if {@link #getRangeUnits() range units}
     * are set to {@link RangeUnits#RU}
     * @return the raw correction for range in meters
     */
    public double getCorrectionRange(final RangeUnitsConverter converter) {
        if (rangeUnits == RangeUnits.km) {
            return rawCorrectionRange * 1000;
        } else if (rangeUnits == RangeUnits.s) {
            return rawCorrectionRange * Constants.SPEED_OF_LIGHT;
        } else {
            return converter.ruToMeters(this, startTime, rawCorrectionRange);
        }
    }

    /** Getter for the raw correction for range.
     * @return the raw correction for range (in {@link #getRangeUnits()})
     */
    public double getRawCorrectionRange() {
        return rawCorrectionRange;
    }

    /** Setter for the raw correction for range.
     * @param rawCorrectionRange the raw correction for range to set (in {@link #getRangeUnits()})
     */
    public void setRawCorrectionRange(final double rawCorrectionRange) {
        refuseFurtherComments();
        this.rawCorrectionRange = rawCorrectionRange;
    }

    /** Getter for the correctionReceive.
     * @return the correctionReceive (in TDM units, without conversion)
     */
    public double getCorrectionReceive() {
        return correctionReceive;
    }

    /** Setter for the correctionReceive.
     * @param correctionReceive the correctionReceive to set (in TDM units, without conversion)
     */
    public void setCorrectionReceive(final double correctionReceive) {
        refuseFurtherComments();
        this.correctionReceive = correctionReceive;
    }

    /** Getter for the correctionTransmit.
     * @return the correctionTransmit (in TDM units, without conversion)
     */
    public double getCorrectionTransmit() {
        return correctionTransmit;
    }

    /** Setter for the correctionTransmit.
     * @param correctionTransmit the correctionTransmit to set (in TDM units, without conversion)
     */
    public void setCorrectionTransmit(final double correctionTransmit) {
        refuseFurtherComments();
        this.correctionTransmit = correctionTransmit;
    }

    /** Getter for the correctionApplied.
     * @return the correctionApplied (in TDM units, without conversion)
     */
    public CorrectionApplied getCorrectionsApplied() {
        return correctionsApplied;
    }

    /** Setter for the correctionApplied.
     * @param correctionsApplied the correctionApplied to set (in TDM units, without conversion)
     */
    public void setCorrectionsApplied(final CorrectionApplied correctionsApplied) {
        refuseFurtherComments();
        this.correctionsApplied = correctionsApplied;
    }

}

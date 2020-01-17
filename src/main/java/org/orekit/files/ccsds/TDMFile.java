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
import java.util.TreeMap;
import java.util.List;
import java.util.Map;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/** This class stocks all the information of the CCSDS Tracking Data Message file parsed by TDMParser or TDMXMLParser. <p>
 * It contains the header and a list of Observations Blocks each containing
 * TDM metadata and a list of observation data lines. <p>
 * At this level the observations are not Orekit objects but custom object containing a keyword (type of observation),
 * a timetag (date of the observation) and a measurement (value of the observation). <p>
 * It is up to the user to convert these observations to Orekit tracking object (Range, Angular, TurnAroundRange etc...).<p>
 * References:<p>
 *  <a href="https://public.ccsds.org/Pubs/503x0b1c1.pdf">CCSDS 503.0-B-1 recommended standard</a> ("Tracking Data Message", Blue Book, Version 1.0, November 2007).
 * @author Maxime Journot
 * @since 9.0
 */
public class TDMFile {

    /** CCSDS Format version. */
    private double formatVersion;

    /** Header comments. The list contains a string for each line of comment. */
    private List<String> headerComment;

    /** File creation date and time in UTC. */
    private AbsoluteDate creationDate;

    /** Creating agency or operator. */
    private String originator;

    /** List of observation blocks. */
    private List<ObservationsBlock> observationsBlocks;

    /** OEMFile constructor. */
    public TDMFile() {
        observationsBlocks = new ArrayList<>();
    }

    /** Get the CCSDS TDM format version.
     * @return format version
     */
    public double getFormatVersion() {
        return formatVersion;
    }

    /** Set the CCSDS ODM (OPM, OMM or OEM) format version.
     * @param formatVersion the format version to be set
     */
    public void setFormatVersion(final double formatVersion) {
        this.formatVersion = formatVersion;
    }

    /** Get the header comment.
     * @return header comment
     */
    public List<String> getHeaderComment() {
        return headerComment;
    }

    /** Set the header comment.
     * @param headerComment header comment
     */
    public void setHeaderComment(final List<String> headerComment) {
        this.headerComment = new ArrayList<>(headerComment);
    }

    /** Get the file creation date and time in UTC.
     * @return the file creation date and time in UTC.
     */
    public AbsoluteDate getCreationDate() {
        return creationDate;
    }

    /** Set the file creation date and time in UTC.
     * @param creationDate the creation date to be set
     */
    public void setCreationDate(final AbsoluteDate creationDate) {
        this.creationDate = creationDate;
    }

    /** Get the file originator.
     * @return originator the file originator.
     */
    public String getOriginator() {
        return originator;
    }

    /** Set the file originator.
     * @param originator the originator to be set
     */
    public void setOriginator(final String originator) {
        this.originator = originator;
    }

    /** Add a block to the list of observations blocks. */
    public void addObservationsBlock() {
        observationsBlocks.add(new ObservationsBlock());
    }

    /** Get the list of observations blocks as an unmodifiable list.
     * @return the list of observations blocks
     */
    public List<ObservationsBlock> getObservationsBlocks() {
        return Collections.unmodifiableList(observationsBlocks);
    }

    /** Set the list of Observations Blocks.
     * @param observationsBlocks the list of Observations Blocks to set
     */
    public void setObservationsBlocks(final List<ObservationsBlock> observationsBlocks) {
        this.observationsBlocks = new ArrayList<>(observationsBlocks);
    }

    /** Check that, according to the CCSDS standard, every ObservationsBlock has the same time system.
     */
    public void checkTimeSystems() {
        final CcsdsTimeScale timeSystem = getObservationsBlocks().get(0).getMetaData().getTimeSystem();
        for (final ObservationsBlock block : observationsBlocks) {
            if (!timeSystem.equals(block.getMetaData().getTimeSystem())) {
                throw new OrekitException(OrekitMessages.CCSDS_TDM_INCONSISTENT_TIME_SYSTEMS,
                                          timeSystem, block.getMetaData().getTimeSystem());
            }
        }
    }

    /** The Observations Block class contain metadata and the list of observation data lines.<p>
     * The reason for which the observations have been separated into blocks is that the different
     * data blocks in a TDM file usually refers to different types of observations.<p>
     * An observation block contains a TDM metadata object and a list of observations.<p>
     * At this level, an observation is not an Orekit object, it is a custom object containing:<p>
     *  - a keyword, the type of the observation;<p>
     *  - a timetag, the date of the observation;<p>
     *  - a measurement, the value of the observation.
     * @author Maxime Journot
     */
    public static class ObservationsBlock {

        /** Meta-data for the block. */
        private TDMMetaData metaData;

        /** List of observations data lines. */
        private List<Observation> observations;

        /** Observations Data Lines comments. The list contains a string for each line of comment. */
        private List<String> observationsComment;

        /** ObservationsBlock constructor. */
        public ObservationsBlock() {
            metaData = new TDMMetaData();
            observations = new ArrayList<>();
            observationsComment = new ArrayList<>();
        }

        /** Get the list of Observations data lines.
         * @return a reference to the internal list of Observations data lines
         */
        public List<Observation> getObservations() {
            return this.observations;
        }

        /** Set the list of Observations Data Lines.
         * @param observations the list of Observations Data Lines to set
         */
        public void setObservations(final List<Observation> observations) {
            this.observations = new ArrayList<>(observations);
        }

        /** Adds an observation data line.
         * @param observation the observation to add to the list
         */
        public void addObservation(final Observation observation) {
            this.observations.add(observation);
        }

        /** Adds an observation data line.
         * @param keyword the keyword
         * @param epoch the timetag
         * @param measurement the measurement
         */
        public void addObservation(final String keyword,
                                   final AbsoluteDate epoch,
                                   final double measurement) {
            this.addObservation(new Observation(keyword, epoch, measurement));
        }

        /** Get the meta-data for the block.
         * @return meta-data for the block
         */
        public TDMMetaData getMetaData() {
            return metaData;
        }

        /** Set the meta-data for the block.
         * @param metaData the meta-data to set
         */
        public void setMetaData(final TDMMetaData metaData) {
            this.metaData = metaData;
        }

        /** Get the observations data lines comment.
         * @return the comment
         */
        public List<String> getObservationsComment() {
            return observationsComment;
        }

        /** Set the observations data lines comment.
         * @param observationsComment the comment to be set
         */
        public void setObservationsComment(final List<String> observationsComment) {
            this.observationsComment = new ArrayList<>(observationsComment);
        }

        /** Add an observation data line comment.
         *  @param observationComment the comment line to add
         */
        public void addObservationComment(final String observationComment) {
            this.observationsComment.add(observationComment);
        }

    }

    /** The Observation class contains the data from an observation line.<p>
     * It is not an Orekit object yet.<p>
     * It is a simple container holding:<p>
     *  - a keyword, the type of the observation;<p>
     *  - a timetag, the epoch of the observation;<p>
     *  - a measurement, the value of the observation.<p>
     * @see Keyword
     * @author mjournot
     */
    public static class Observation {

        /** CCSDS Keyword: the type of the observation. */
        private String keyword;

        /** Epoch: the timetag of the observation. */
        private AbsoluteDate epoch;

        /** Measurement: the value of the observation. */
        private double measurement;

        /** Simple constructor.
         * @param keyword the keyword
         * @param epoch the timetag
         * @param measurement the measurement
         */
        Observation(final String keyword, final AbsoluteDate epoch, final double measurement) {
            this.keyword = keyword;
            this.epoch = epoch;
            this.measurement = measurement;
        }

        /** Getter for the keyword.
         * @return the keyword
         */
        public String getKeyword() {
            return keyword;
        }

        /** Setter for the keyword.
         * @param keyword the keyword to set
         */
        public void setKeyword(final String keyword) {
            this.keyword = keyword;
        }

        /** Getter for the epoch.
         * @return the epoch
         */
        public AbsoluteDate getEpoch() {
            return epoch;
        }

        /** Setter for the epoch.
         * @param epoch the epoch to set
         */
        public void setEpoch(final AbsoluteDate epoch) {
            this.epoch = epoch;
        }

        /** Getter for the measurement.
         * @return the measurement
         */
        public double getMeasurement() {
            return measurement;
        }

        /** Setter for the measurement.
         * @param measurement the measurement to set
         */
        public void setMeasurement(final double measurement) {
            this.measurement = measurement;
        }
    }

    /** The TDMMetadata class gathers the meta-data present in the Tracking Data Message (TDM).<p>
     *  References:<p>
     *  <a href="https://public.ccsds.org/Pubs/503x0b1c1.pdf">CCSDS 503.0-B-1 recommended standard</a>. §3.3 ("Tracking Data Message", Blue Book, Version 1.0, November 2007).
     *
     * @author Maxime Journot
     * @since 9.0
     */
    public static class TDMMetaData {

        /** Time System used in the tracking data session. */
        private CcsdsTimeScale timeSystem;

        /** Start epoch of total time span covered by observations block. */
        private AbsoluteDate startTime;

        /** Start time as read in the file. */
        private String startTimeString;
        /** End epoch of total time span covered by observations block. */
        private AbsoluteDate stopTime;

        /** Stop time as read in the file. */
        private String stopTimeString;

        /** Map of participants in the tracking data session (minimum 1 and up to 5).<p>
         *  Participants may include ground stations, spacecraft, and/or quasars.<p>
         *  Participants represent the classical transmitting parties, transponding parties, and receiving parties.
         */
        private Map<Integer, String> participants;

        /** Tracking mode associated with the Data Section of the segment.<p>
         *  - SEQUENTIAL : Sequential signal path between participants (range, Doppler, angles and line of sight ionosphere calibration);<p>
         *  - SINGLE_DIFF: Differenced data.
         */
        private String mode;

        /** The path shall reflect the signal path by listing the index of each participant
         *  in order, separated by commas, with no inserted white space.<p>
         *  The integers 1, 2, 3, 4, 5 used to specify the signal path correlate
         *  with the indices of the PARTICIPANT keywords.<p>
         *  The first entry in the PATH shall be the transmit participant.<p>
         *  The non-indexed ‘PATH’ keyword shall be used if the MODE is ‘SEQUENTIAL’.<p>
         *  The indexed ‘PATH_1’ and ‘PATH_2’ keywords shall be used where the MODE is ‘SINGLE_DIFF’.
         */
        private String path;

        /** Path 1 (see above). */
        private String path1;

        /** Path 2 (see above). */
        private String path2;

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
        private String timetagRef;

        /** Integration interval. <p>
         *  Provides the Doppler count time in seconds for Doppler data or for the creation
         *  of normal points.
         */
        private double integrationInterval;

        /** Integration reference.<p>
         *  Used in conjunction with timetag reference and integration interval.<p>
         *  Indicates whether the timetag represents the start, middle or end of the integration interval.
         */
        private String integrationRef;

        /** Frequency offset.<p>
         *  A frequency in Hz that must be added to every RECEIVE_FREQ to reconstruct it.
         */
        private double freqOffset;

        /** Range mode.<p>
         *  COHERENT, CONSTANT or ONE_WAY.
         */
        private String rangeMode;

        /** Range modulus.<p>
         *  Modulus of the range observable in the units as specified by the RANGE_UNITS keyword.
         */
        private double rangeModulus;

        /** Range units.<p>
         *  The units for the range observable: 'km', 's' or 'RU' (for 'range units').
         */
        private String rangeUnits;

        /** Angle type.<p>
         *  Type of the antenna geometry represented in the angle data ANGLE_1 and ANGLE_2.<p>
         *  – AZEL for azimuth, elevation (local horizontal);<p>
         *  – RADEC for right ascension, declination or hour angle, declination (needs to be referenced to an inertial frame);<p>
         *  – XEYN for x-east, y-north;<p>
         *  – XSYE for x-south, y-east.<p>
         *  Note: Angle units are always degrees
         */
        private String angleType;

        /** The reference frame specifier, as it appeared in the file. */
        private String referenceFrameString;

        /** Reference frame in which data are given: used in combination with ANGLE_TYPE=RADEC. */
        private Frame referenceFrame;

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

        /** Data quality.<p>
         *  Estimate of the quality of the data: RAW, DEGRADED or VALIDATED.
         */
        private String dataQuality;

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

        /** Correction Range.<p>
         *  Range correction that has been added or should be added to the RANGE data.
         */
        private double correctionRange;

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
        private String correctionsApplied;

        /** Meta-data comments. The list contains a string for each line of comment. */
        private List<String> comment;

        /** Create a new TDM meta-data.
         */
        public TDMMetaData() {
            participants   = new TreeMap<>();
            transmitDelays = new TreeMap<>();
            receiveDelays  = new TreeMap<>();
            comment        = new ArrayList<>();
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
        public void setTimeSystem(final CcsdsTimeScale timeSystem) {
            this.timeSystem = timeSystem;
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
            this.startTime = startTime;
        }

        /** Getter for the startTime String.
         * @return the startTime String
         */
        public String getStartTimeString() {
            return startTimeString;
        }

        /** Setter for the startTime String.
         * @param startTimeString the startTime String to set
         */
        public void setStartTimeString(final String startTimeString) {
            this.startTimeString = startTimeString;
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
            this.stopTime = stopTime;
        }

        /** Getter for the stopTime String.
         * @return the stopTime String
         */
        public String getStopTimeString() {
            return stopTimeString;
        }

        /** Setter for the stopTime String.
         * @param stopTimeString the stopTime String to set
         */
        public void setStopTimeString(final String stopTimeString) {
            this.stopTimeString = stopTimeString;
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
            this.participants = new TreeMap<Integer, String>();
            this.participants.putAll(participants);
        }

        /** Adds a participant to the list.
         * @param participantNumber the number of the participant to add
         * @param participant the name of the participant to add
         */
        public void addParticipant(final int participantNumber, final String participant) {
            this.participants.put(participantNumber, participant);
        }

        /** Getter for the mode.
         * @return the mode
         */
        public String getMode() {
            return mode;
        }

        /** Setter for the mode.
         * @param mode the mode to set
         */
        public void setMode(final String mode) {
            this.mode = mode;
        }

        /** Getter for the path.
         * @return the path
         */
        public String getPath() {
            return path;
        }

        /** Setter for the path.
         * @param path the path to set
         */
        public void setPath(final String path) {
            this.path = path;
        }

        /** Getter for the path1.
         * @return the path1
         */
        public String getPath1() {
            return path1;
        }

        /** Setter for the path1.
         * @param path1 the path1 to set
         */
        public void setPath1(final String path1) {
            this.path1 = path1;
        }

        /** Getter for the path2.
         * @return the path2
         */
        public String getPath2() {
            return path2;
        }

        /** Setter for the path2.
         * @param path2 the path2 to set
         */
        public void setPath2(final String path2) {
            this.path2 = path2;
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
            this.turnaroundDenominator = turnaroundDenominator;
        }

        /** Getter for the timetagRef.
         * @return the timetagRef
         */
        public String getTimetagRef() {
            return timetagRef;
        }

        /** Setter for the timetagRef.
         * @param timetagRef the timetagRef to set
         */
        public void setTimetagRef(final String timetagRef) {
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
            this.integrationInterval = integrationInterval;
        }

        /** Getter for the integrationRef.
         * @return the integrationRef
         */
        public String getIntegrationRef() {
            return integrationRef;
        }

        /** Setter for the integrationRef.
         * @param integrationRef the integrationRef to set
         */
        public void setIntegrationRef(final String integrationRef) {
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
            this.freqOffset = freqOffset;
        }

        /** Getter for the rangeMode.
         * @return the rangeMode
         */
        public String getRangeMode() {
            return rangeMode;
        }

        /** Setter for the rangeMode.
         * @param rangeMode the rangeMode to set
         */
        public void setRangeMode(final String rangeMode) {
            this.rangeMode = rangeMode;
        }

        /** Getter for the rangeModulus.
         * @return the rangeModulus
         */
        public double getRangeModulus() {
            return rangeModulus;
        }

        /** Setter for the rangeModulus.
         * @param rangeModulus the rangeModulus to set
         */
        public void setRangeModulus(final double rangeModulus) {
            this.rangeModulus = rangeModulus;
        }

        /** Getter for the rangeUnits.
         * @return the rangeUnits
         */
        public String getRangeUnits() {
            return rangeUnits;
        }

        /** Setter for the rangeUnits.
         * @param rangeUnits the rangeUnits to set
         */
        public void setRangeUnits(final String rangeUnits) {
            this.rangeUnits = rangeUnits;
        }

        /** Getter for angleType.
         * @return the angleType
         */
        public String getAngleType() {
            return angleType;
        }

        /** Setter for the angleType.
         * @param angleType the angleType to set
         */
        public void setAngleType(final String angleType) {
            this.angleType = angleType;
        }

        /** Get the the value of {@code REFERENCE_FRAME} as an Orekit {@link Frame}.
         * @return The reference frame specified by the {@code REFERENCE_FRAME} keyword.
         */
        public Frame getReferenceFrame() {
            return referenceFrame;
        }

        /** Set the reference frame in which data are given: used for RADEC tracking data.
         * @param refFrame the reference frame to be set
         */
        public void setReferenceFrame(final Frame refFrame) {
            this.referenceFrame = refFrame;
        }

        /** Get the reference frame specifier as it appeared in the file.
         * @return the frame name as it appeared in the file.
         */
        public String getReferenceFrameString() {
            return this.referenceFrameString;
        }

        /** Set the reference frame name.
         * @param frame specifier as it appeared in the file.
         */
        public void setReferenceFrameString(final String frame) {
            this.referenceFrameString = frame;
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
            this.transmitDelays = new TreeMap<Integer, Double>();
            this.transmitDelays.putAll(transmitDelays);
        }

        /** Adds a transmit delay to the list.
         *  @param participantNumber the number of the participants for which the transmit delay is given
         *  @param transmitDelay the transmit delay value to add
         */
        public void addTransmitDelay(final int participantNumber, final double transmitDelay) {
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
            this.receiveDelays = new TreeMap<Integer, Double>();
            this.receiveDelays.putAll(receiveDelays);
        }

        /** Adds a receive delay to the list.
         * @param participantNumber the number of the participants for which the receive delay is given
         * @param receiveDelay the receive delay value to add
         */
        public void addReceiveDelay(final int participantNumber, final double receiveDelay) {
            this.receiveDelays.put(participantNumber, receiveDelay);
        }
        /** Getter for the dataQuality.
         * @return the dataQuality
         */
        public String getDataQuality() {
            return dataQuality;
        }

        /** Setter for the dataQuality.
         * @param dataQuality the dataQuality to set
         */
        public void setDataQuality(final String dataQuality) {
            this.dataQuality = dataQuality;
        }

        /** Getter for the correctionAngle1.
         * @return the correctionAngle1
         */
        public double getCorrectionAngle1() {
            return correctionAngle1;
        }

        /** Setter for the correctionAngle1.
         * @param correctionAngle1 the correctionAngle1 to set
         */
        public void setCorrectionAngle1(final double correctionAngle1) {
            this.correctionAngle1 = correctionAngle1;
        }

        /** Getter for the correctionAngle2.
         * @return the correctionAngle2
         */
        public double getCorrectionAngle2() {
            return correctionAngle2;
        }

        /** Setter for the correctionAngle2.
         * @param correctionAngle2 the correctionAngle2 to set
         */
        public void setCorrectionAngle2(final double correctionAngle2) {
            this.correctionAngle2 = correctionAngle2;
        }

        /** Getter for the correctionDoppler.
         * @return the correctionDoppler
         */
        public double getCorrectionDoppler() {
            return correctionDoppler;
        }

        /** Setter for the correctionDoppler.
         * @param correctionDoppler the correctionDoppler to set
         */
        public void setCorrectionDoppler(final double correctionDoppler) {
            this.correctionDoppler = correctionDoppler;
        }

        /** Getter for the correctionRange.
         * @return the correctionRange
         */
        public double getCorrectionRange() {
            return correctionRange;
        }

        /** Setter for the correctionRange.
         * @param correctionRange the correctionRange to set
         */
        public void setCorrectionRange(final double correctionRange) {
            this.correctionRange = correctionRange;
        }

        /** Getter for the correctionReceive.
         * @return the correctionReceive
         */
        public double getCorrectionReceive() {
            return correctionReceive;
        }

        /** Setter for the correctionReceive.
         * @param correctionReceive the correctionReceive to set
         */
        public void setCorrectionReceive(final double correctionReceive) {
            this.correctionReceive = correctionReceive;
        }

        /** Getter for the correctionTransmit.
         * @return the correctionTransmit
         */
        public double getCorrectionTransmit() {
            return correctionTransmit;
        }

        /** Setter for the correctionTransmit.
         * @param correctionTransmit the correctionTransmit to set
         */
        public void setCorrectionTransmit(final double correctionTransmit) {
            this.correctionTransmit = correctionTransmit;
        }

        /** Getter for the correctionApplied.
         * @return the correctionApplied
         */
        public String getCorrectionsApplied() {
            return correctionsApplied;
        }

        /** Setter for the correctionApplied.
         * @param correctionsApplied the correctionApplied to set
         */
        public void setCorrectionsApplied(final String correctionsApplied) {
            this.correctionsApplied = correctionsApplied;
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
            this.comment = new ArrayList<>(comment);
        }
    }
}

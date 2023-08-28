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
package org.orekit.files.rinex.clock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.AppliedDCBS;
import org.orekit.files.rinex.AppliedPCVS;
import org.orekit.frames.Frame;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.TimeSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.utils.TimeSpanMap;

/** Represents a parsed clock file from the IGS.
 * <p> A time system should be specified in the file. However, if it is not, default time system will be chosen
 * regarding the satellite system. If it is mixed or not specified, default time system will be UTC. </p>
 * <p> Some fields might be null after parsing. It is expected because of the numerous kind of data that can be stored in clock data file. </p>
 * <p> Caution, files with missing information in header can lead to wrong data dates and station positions.
 * It is adviced to check the correctness and format compliance of the clock file to be parsed.
 * Some values such as file time scale still can be set by user. </p>
 * @see <a href="ftp://igs.org/pub/data/format/rinex_clock300.txt"> 3.00 clock file format</a>
 * @see <a href="ftp://igs.org/pub/data/format/rinex_clock302.txt"> 3.02 clock file format</a>
 * @see <a href="ftp://igs.org/pub/data/format/rinex_clock304.txt"> 3.04 clock file format</a>
 *
 * @author Thomas Paulet
 * @since 11.0
 */
public class RinexClock {

    /** Format version. */
    private double formatVersion;

    /** Satellite system. */
    private SatelliteSystem satelliteSystem;

    /** Name of the program creating current file. */
    private String programName;

    /** Name of the agency creating the current file. */
    private String agencyName;

    /** Date of the file creation as a string. */
    private String creationDateString;

    /** Time of the file creation as a string. */
    private String creationTimeString;

    /** Time zone of the file creation as a string. */
    private String creationTimeZoneString;

    /** Creation date as absolute date. */
    private AbsoluteDate creationDate;

    /** Comments. */
    private String comments;

    /** Satellite system code. */
    private Map<SatelliteSystem, List<ObservationType>> systemObservationTypes;

    /** Time system. */
    private TimeSystem timeSystem;

    /** Data time scale related to time system. */
    private TimeScale timeScale;

    /** Number of leap seconds separating UTC and TAI (UTC = TAI - numberOfLeapSeconds). */
    private int numberOfLeapSeconds;

    /** Number of leap seconds separating UTC and GNSS time systems. */
    private int numberOfLeapSecondsGNSS;

    /** List of applied differential code bias corrections. */
    private List<AppliedDCBS> listAppliedDCBS;

    /** List of antenna center variation corrections. */
    private List<AppliedPCVS> listAppliedPCVS;

    /** List of the data types in the file. */
    private List<ClockDataType> clockDataTypes;

    /** Station name for calibration and discontinuity data. */
    private String stationName;

    /** Station identifier for calibration and discontinuity data. */
    private String stationIdentifier;

    /** External reference clock identifier for calibration. */
    private String externalClockReference;

    /** Analysis center ID. */
    private String analysisCenterID;

    /** Full analysis center name. */
    private String analysisCenterName;

    /** Reference clocks. */
    private TimeSpanMap<List<ReferenceClock>> referenceClocks;

    /** Earth centered frame name as a string. */
    private String frameName;

    /** Maps {@link #coordinateSystem} to a {@link Frame}. */
    private final Function<? super String, ? extends Frame> frameBuilder;

    /** List of the receivers in the file. */
    private List<Receiver> receivers;

    /** List of the satellites in the file. */
    private List<String> satellites;

    /** A map containing receiver/satellite information. */
    private Map<String, List<ClockDataLine>> clockData;

    /** Constructor.
     * @param frameBuilder for constructing a reference frame from the identifier
     */
    public RinexClock(final Function<? super String, ? extends Frame> frameBuilder) {
        // Initialize fields with default data
        this.systemObservationTypes  = new HashMap<SatelliteSystem, List<ObservationType>>();
        this.listAppliedDCBS         = new ArrayList<AppliedDCBS>();
        this.listAppliedPCVS         = new ArrayList<AppliedPCVS>();
        this.clockDataTypes          = new ArrayList<ClockDataType>();
        this.receivers               = new ArrayList<Receiver>();
        this.satellites              = new ArrayList<String>();
        this.clockData               = new HashMap<String, List<ClockDataLine>>();
        this.agencyName              = "";
        this.analysisCenterID        = "";
        this.analysisCenterName      = "";
        this.comments                = "";
        this.creationDate            = null;
        this.creationDateString      = "";
        this.creationTimeString      = "";
        this.creationTimeZoneString  = "";
        this.externalClockReference  = "";
        this.formatVersion           = 0.0;
        this.frameBuilder            = frameBuilder;
        this.frameName               = "";
        this.numberOfLeapSeconds     = 0;
        this.numberOfLeapSecondsGNSS = 0;
        this.programName             = "";
        this.referenceClocks         = null;
        this.satelliteSystem         = null;
        this.stationIdentifier       = "";
        this.stationName             = "";
        this.timeScale               = null;
        this.timeSystem              = null;
    }

    /** Add a new satellite with a given identifier to the list of stored satellites.
     * @param satId the satellite identifier
     */
    public void addSatellite(final String satId) {
        // only add satellites which have not been added before
        if (!satellites.contains(satId)) {
            satellites.add(satId);
        }
    }

    /** Add a new receiver to the list of stored receivers.
     * @param receiver the receiver
     */
    public void addReceiver(final Receiver receiver) {

        boolean notInList = true;
        for (Receiver rec : receivers) {
            if (rec.designator.equals(receiver.designator)) {
                notInList = false;
            }
        }
        // only add satellites which have not been added before
        if (notInList) {
            receivers.add(receiver);
        }
    }

    /** Get the number of different clock data types in the file.
     * @return the number of different clock data types
     */
    public int getNumberOfClockDataTypes() {
        return clockDataTypes.size();
    }

    /** Get the total number of complete data lines in the file.
     * @return the total number of complete data lines in the file
     */
    public int getTotalNumberOfDataLines() {
        int result = 0;
        final Map<String, List<ClockDataLine>> data = getClockData();
        for (final Map.Entry<String, List<ClockDataLine>> entry : data.entrySet()) {
            result += entry.getValue().size();
        }
        return result;
    }

    /** Get the number of observation types for a given system.
     * @param system the satellite system to consider
     * @return the number of observation types for a given system
     */
    public int numberOfObsTypes(final SatelliteSystem system) {
        if (systemObservationTypes.containsKey(system)) {
            return systemObservationTypes.get(system).size();
        } else {
            return 0;
        }
    }

    /** Get the number of receivers that are considered in the file.
     * @return the number of receivers that are considered in the file
     */
    public int getNumberOfReceivers() {
        return receivers.size();
    }

    /** Get the number of satellites that are considered in the file.
     * @return the number of satellites that are considered in the file
     */
    public int getNumberOfSatellites() {
        return satellites.size();
    }

    /** Getter for the format version.
     * @return the format version
     */
    public double getFormatVersion() {
        return formatVersion;
    }

    /** Setter for the format version.
     * @param formatVersion the format version to set
     */
    public void setFormatVersion(final double formatVersion) {
        this.formatVersion = formatVersion;
    }

    /** Getter for the satellite system.
     * @return the satellite system
     */
    public SatelliteSystem getSatelliteSystem() {
        return satelliteSystem;
    }

    /** Setter for the satellite system.
     * @param satelliteSystem the satellite system to set
     */
    public void setSatelliteSystem(final SatelliteSystem satelliteSystem) {
        this.satelliteSystem = satelliteSystem;
    }

    /** Getter for the program name.
     * @return the program name
     */
    public String getProgramName() {
        return programName;
    }

    /** Setter for the program name.
     * @param programName the program name to set
     */
    public void setProgramName(final String programName) {
        this.programName = programName;
    }

    /** Getter for the agency name.
     * @return the agencyName
     */
    public String getAgencyName() {
        return agencyName;
    }

    /** Setter for the agency name.
     * @param agencyName the agency name to set
     */
    public void setAgencyName(final String agencyName) {
        this.agencyName = agencyName;
    }

    /** Getter for the creation date of the file as a string.
     * @return the creation date as a string
     */
    public String getCreationDateString() {
        return creationDateString;
    }

    /** Setter for the creation date as a string.
     * @param creationDateString the creation date as a string to set
     */
    public void setCreationDateString(final String creationDateString) {
        this.creationDateString = creationDateString;
    }

    /** Getter for the creation time of the file as a string.
     * @return the creation time as a string
     */
    public String getCreationTimeString() {
        return creationTimeString;
    }

    /** Setter for the creation time as a string.
     * @param creationTimeString the creation time as a string to set
     */
    public void setCreationTimeString(final String creationTimeString) {
        this.creationTimeString = creationTimeString;
    }

    /** Getter for the creation time zone of the file as a string.
     * @return the creation time zone as a string
     */
    public String getCreationTimeZoneString() {
        return creationTimeZoneString;
    }

    /** Setter for the creation time zone.
     * @param creationTimeZoneString the creation time zone as a string to set
     */
    public void setCreationTimeZoneString(final String creationTimeZoneString) {
        this.creationTimeZoneString = creationTimeZoneString;
    }

    /** Getter for the creation date.
     * @return the creation date
     */
    public AbsoluteDate getCreationDate() {
        return creationDate;
    }

    /** Setter for the creation date.
     * @param creationDate the creation date to set
     */
    public void setCreationDate(final AbsoluteDate creationDate) {
        this.creationDate = creationDate;
    }

    /** Getter for the comments.
     * @return the comments
     */
    public String getComments() {
        return comments;
    }

    /** Add a comment line.
     * @param comment the comment line to add
     */
    public void addComment(final String comment) {
        this.comments = comments.concat(comment + "\n");
    }

    /** Getter for the different observation type for each satellite system.
     * @return the map of the different observation type per satellite system
     */
    public Map<SatelliteSystem, List<ObservationType>> getSystemObservationTypes() {
        return Collections.unmodifiableMap(systemObservationTypes);
    }

    /** Add an observation type for a specified satellite system.
     * @param satSystem the satellite system to add observation type
     * @param observationType the system observation type to set
     */
    public void addSystemObservationType(final SatelliteSystem satSystem,
                                         final ObservationType observationType) {
        systemObservationTypes.putIfAbsent(satSystem, new ArrayList<ObservationType>());
        systemObservationTypes.get(satSystem).add(observationType);
    }

    /** Getter for the file time system.
     * @return the file time system
     */
    public TimeSystem getTimeSystem() {
        return timeSystem;
    }

    /** Setter for the file time system.
     * @param timeSystem the file time system to set
     */
    public void setTimeSystem(final TimeSystem timeSystem) {
        this.timeSystem = timeSystem;
    }

    /** Getter for the data time scale.
     * @return the data time scale
     */
    public TimeScale getTimeScale() {
        return timeScale;
    }

    /** Setter for the data time scale.
     * @param timeScale the data time scale to set
     */
    public void setTimeScale(final TimeScale timeScale) {
        this.timeScale = timeScale;
    }

    /** Getter for the number of leap seconds.
     * @return the number of leap seconds
     */
    public int getNumberOfLeapSeconds() {
        return numberOfLeapSeconds;
    }

    /** Setter for the number of leap seconds.
     * @param numberOfLeapSeconds the number of leap seconds to set
     */
    public void setNumberOfLeapSeconds(final int numberOfLeapSeconds) {
        this.numberOfLeapSeconds = numberOfLeapSeconds;
    }

    /** Getter for the number of leap second for GNSS time scales.
     * @return the number of leap seconds for GNSS time scales
     */
    public int getNumberOfLeapSecondsGNSS() {
        return numberOfLeapSecondsGNSS;
    }

    /** Setter for the number of leap seconds for GNSS time scales.
     * @param numberOfLeapSecondsGNSS the number of leap seconds for GNSS time scales to set
     */
    public void setNumberOfLeapSecondsGNSS(final int numberOfLeapSecondsGNSS) {
        this.numberOfLeapSecondsGNSS = numberOfLeapSecondsGNSS;
    }

    /** Getter for the applied differential code bias corrections.
     * @return the list of applied differential code bias corrections
     */
    public List<AppliedDCBS> getListAppliedDCBS() {
        return Collections.unmodifiableList(listAppliedDCBS);
    }

    /** Add an applied differencial code bias corrections.
     * @param appliedDCBS the applied differencial code bias corrections to add
     */
    public void addAppliedDCBS(final AppliedDCBS appliedDCBS) {
        listAppliedDCBS.add(appliedDCBS);
    }

    /** Getter for the applied phase center variations.
     * @return the list of the applied phase center variations
     */
    public List<AppliedPCVS> getListAppliedPCVS() {
        return Collections.unmodifiableList(listAppliedPCVS);
    }

    /** Add an applied phase center variations.
     * @param appliedPCVS the phase center variations to add
     */
    public void addAppliedPCVS(final AppliedPCVS appliedPCVS) {
        listAppliedPCVS.add(appliedPCVS);
    }

    /** Getter for the different clock data types.
     * @return the list of the different clock data types
     */
    public List<ClockDataType> getClockDataTypes() {
        return Collections.unmodifiableList(clockDataTypes);
    }

    /** Add a clock data types.
     * @param clockDataType the clock data types to add
     */
    public void addClockDataType(final ClockDataType clockDataType) {
        clockDataTypes.add(clockDataType);
    }

    /** Getter for the station name.
     * @return the station name
     */
    public String getStationName() {
        return stationName;
    }

    /** Setter for the station name.
     * @param stationName the station name to set
     */
    public void setStationName(final String stationName) {
        this.stationName = stationName;
    }

    /** Getter for the station identifier.
     * @return the station identifier
     */
    public String getStationIdentifier() {
        return stationIdentifier;
    }

    /** Setter for the station identifier.
     * @param stationIdentifier the station identifier to set
     */
    public void setStationIdentifier(final String stationIdentifier) {
        this.stationIdentifier = stationIdentifier;
    }

    /** Getter for the external clock reference.
     * @return the external clock reference
     */
    public String getExternalClockReference() {
        return externalClockReference;
    }

    /** Setter for the external clock reference.
     * @param externalClockReference the external clock reference to set
     */
    public void setExternalClockReference(final String externalClockReference) {
        this.externalClockReference = externalClockReference;
    }

    /** Getter for the analysis center ID.
     * @return the analysis center ID
     */
    public String getAnalysisCenterID() {
        return analysisCenterID;
    }

    /** Setter for the analysis center ID.
     * @param analysisCenterID the analysis center ID to set
     */
    public void setAnalysisCenterID(final String analysisCenterID) {
        this.analysisCenterID = analysisCenterID;
    }

    /** Getter for the analysis center name.
     * @return the analysis center name
     */
    public String getAnalysisCenterName() {
        return analysisCenterName;
    }

    /** Setter for the analysis center name.
     * @param analysisCenterName the analysis center name to set
     */
    public void setAnalysisCenterName(final String analysisCenterName) {
        this.analysisCenterName = analysisCenterName;
    }

    /** Getter for the reference clocks.
     * @return the time span map of the different refence clocks
     */
    public TimeSpanMap<List<ReferenceClock>> getReferenceClocks() {
        return referenceClocks;
    }

    /** Add a list of reference clocks wich will be used after a specified date.
     * If the reference map has not been already created, it will be.
     * @param referenceClockList the reference clock list
     * @param startDate the date the list will be valid after.
     */
    public void addReferenceClockList(final List<ReferenceClock> referenceClockList,
                                      final AbsoluteDate startDate) {
        if (referenceClocks == null) {
            referenceClocks = new TimeSpanMap<>(null);
        }
        referenceClocks.addValidAfter(referenceClockList, startDate, false);
    }

    /** Getter for the frame name.
     * @return the frame name
     */
    public String getFrameName() {
        return frameName;
    }


    /** Setter for the frame name.
     * @param frameName the frame name to set
     */
    public void setFrameName(final String frameName) {
        this.frameName = frameName;
    }


    /** Getter for the receivers.
     * @return the list of the receivers
     */
    public List<Receiver> getReceivers() {
        return Collections.unmodifiableList(receivers);
    }

    /** Getter for the satellites.
     * @return the list of the satellites
     */
    public List<String> getSatellites() {
        return Collections.unmodifiableList(satellites);
    }

    /** Get the reference frame for the station positions.
     * @return the reference frame for station positions
     */
    public Frame getFrame() {
        return frameBuilder.apply(frameName);
    }


    /** Getter for an unmodifiable map of clock data.
     * @return the clock data
     */
    public Map<String, List<ClockDataLine>> getClockData() {
        return Collections.unmodifiableMap(clockData);
    }


    /** Add a clock data line to a specified receiver/satellite.
     * @param id the satellite system to add observation type
     * @param clockDataLine the clock data line to add
     */
    public void addClockData(final String id,
                             final ClockDataLine clockDataLine) {
        clockData.putIfAbsent(id, new ArrayList<ClockDataLine>());
        clockData.get(id).add(clockDataLine);
    }

    /** Clock data for a single station.
     * <p> Data epoch is not linked to any time system in order to pars files with missing lines.
     * Though, the default version of the getEpoch() method links the data time components with the clock file object time scale.
     * The latter can be set with a default value (UTC). Caution is recommanded.
     */
    public class ClockDataLine {

        /** Clock data type. */
        private ClockDataType dataType;

        /** Receiver/Satellite name. */
        private String name;

        /** Epoch date components. */
        private DateComponents dateComponents;

        /** Epoch time components. */
        private TimeComponents timeComponents;

        /** Number of data values to follow.
         * This number might not represent the non zero values in the line.
         */
        private int numberOfValues;

        /** Clock bias (seconds). */
        private double clockBias;

        /** Clock bias sigma (seconds). */
        private double clockBiasSigma;

        /** Clock rate (dimensionless). */
        private double clockRate;

        /** Clock rate sigma (dimensionless). */
        private double clockRateSigma;

        /** Clock acceleration (seconds^-1). */
        private double clockAcceleration;

        /** Clock acceleration sigma (seconds^-1). */
        private double clockAccelrerationSigma;

        /** Constructor.
         * @param type the clock data type
         * @param name the receiver/satellite name
         * @param dateComponents the epoch date components
         * @param timeComponents the epoch time components
         * @param numberOfValues the number of values to follow
         * @param clockBias the clock bias in seconds
         * @param clockBiasSigma the clock bias sigma in seconds
         * @param clockRate the clock rate
         * @param clockRateSigma the clock rate sigma
         * @param clockAcceleration the clock acceleration in seconds^-1
         * @param clockAccelerationSigma the clock acceleration in seconds^-1
         */
        public ClockDataLine (final ClockDataType type, final String name,
                              final DateComponents dateComponents,
                              final TimeComponents timeComponents,
                              final int numberOfValues,
                              final double clockBias, final double clockBiasSigma,
                              final double clockRate, final double clockRateSigma,
                              final double clockAcceleration, final double clockAccelerationSigma) {

            this.dataType                = type;
            this.name                    = name;
            this.dateComponents          = dateComponents;
            this.timeComponents          = timeComponents;
            this.numberOfValues          = numberOfValues;
            this.clockBias               = clockBias;
            this.clockBiasSigma          = clockBiasSigma;
            this.clockRate               = clockRate;
            this.clockRateSigma          = clockRateSigma;
            this.clockAcceleration       = clockAcceleration;
            this.clockAccelrerationSigma = clockAccelerationSigma;
        }

        /** Getter for the clock data type.
         * @return the clock data type
         */
        public ClockDataType getDataType() {
            return dataType;
        }

        /** Getter for the receiver/satellite name.
         * @return the receiver/satellite name
         */
        public String getName() {
            return name;
        }

        /** Getter for the number of values to follow.
         * @return the number of values to follow
         */
        public int getNumberOfValues() {
            return numberOfValues;
        }

        /** Get data line epoch.
         * This method should be used if Time System ID line is present in the clock file.
         * If it is missing, UTC time scale will be applied.
         * To specify tim scale, use {@link #getEpoch(TimeScale) getEpoch(TimeScale)} method.
         * @return the data line epoch
         */
        public AbsoluteDate getEpoch() {
            return new AbsoluteDate(dateComponents, timeComponents, timeScale);
        }

        /** Get data line epoch.
         * This method should be used in case Time System ID line is missing.
         * Otherwise, it is adviced to rather use {@link #getEpoch() getEpoch()} method.
         * @param epochTimeScale the time scale in which the epoch is defined
         * @return the data line epoch set in the specified time scale
         */
        public AbsoluteDate getEpoch(final TimeScale epochTimeScale) {
            return new AbsoluteDate(dateComponents, timeComponents, epochTimeScale);
        }

        /** Getter for the clock bias.
         * @return the clock bias in seconds
         */
        public double getClockBias() {
            return clockBias;
        }

        /** Getter for the clock bias sigma.
         * @return the clock bias sigma in seconds
         */
        public double getClockBiasSigma() {
            return clockBiasSigma;
        }

        /** Getter for the clock rate.
         * @return the clock rate
         */
        public double getClockRate() {
            return clockRate;
        }

        /** Getter for the clock rate sigma.
         * @return the clock rate sigma
         */
        public double getClockRateSigma() {
            return clockRateSigma;
        }

        /** Getter for the clock acceleration.
         * @return the clock acceleration in seconds^-1
         */
        public double getClockAcceleration() {
            return clockAcceleration;
        }

        /** Getter for the clock acceleration sigma.
         * @return the clock acceleration sigma in seconds^-1
         */
        public double getClockAccelerationSigma() {
            return clockAccelrerationSigma;
        }

    }

    /** Represents a reference clock with its validity time span. */
    public static class ReferenceClock {

        /** Receiver/satellite embedding the reference clock name. */
        private String referenceName;

        /** Clock ID. */
        private String clockID;

        /** A priori clock constraint (in seconds). */
        private double clockConstraint;

        /** Start date of the validity period. */
        private AbsoluteDate startDate;

        /** End date of the validity period. */
        private AbsoluteDate endDate;

        /** Constructor.
         * @param referenceName the name of the receiver/satellite embedding the reference clock
         * @param clockID the clock ID
         * @param clockConstraint the a priori clock constraint
         * @param startDate the validity period start date
         * @param endDate the validity period end date
         */
        public ReferenceClock (final String referenceName, final String clockID, final double clockConstraint,
                               final AbsoluteDate startDate, final AbsoluteDate endDate) {
            this.referenceName   = referenceName;
            this.clockID         = clockID;
            this.clockConstraint = clockConstraint;
            this.startDate       = startDate;
            this.endDate         = endDate;
        }

        /** Getter for the name of the receiver/satellite embedding the reference clock.
         * @return the name of the receiver/satellite embedding the reference clock
         */
        public String getReferenceName() {
            return referenceName;
        }

        /** Getter for the clock ID.
         * @return the clock ID
         */
        public String getClockID() {
            return clockID;
        }

        /** Getter for the clock constraint.
         * @return the clock constraint
         */
        public double getClockConstraint() {
            return clockConstraint;
        }

        /** Getter for the validity period start date.
         * @return the validity period start date
         */
        public AbsoluteDate getStartDate() {
            return startDate;
        }

        /** Getter for the validity period end date.
         * @return the validity period end date
         */
        public AbsoluteDate getEndDate() {
            return endDate;
        }

    }

    /** Represents a receiver or a satellite with its position in the considered frame. */
    public static class Receiver {

        /** Designator. */
        private String designator;

        /** Receiver identifier. */
        private String receiverIdentifier;

        /** X coordinates in file considered Earth centered frame (in meters). */
        private double x;

        /** Y coordinates in file considered Earth centered frame (in meters). */
        private double y;

        /** Z coordinates in file considered Earth centered frame (in meters). */
        private double z;

        /** Constructor.
         * @param designator the designator
         * @param receiverIdentifier the receiver identifier
         * @param x the X coordinate in meters in considered Earth centered frame
         * @param y the Y coordinate in meters in considered Earth centered frame
         * @param z the Z coordinate in meters in considered Earth centered frame
         */
        public Receiver(final String designator, final String receiverIdentifier,
                        final double x, final double y, final double z) {
            this.designator         = designator;
            this.receiverIdentifier = receiverIdentifier;
            this.x                  = x;
            this.y                  = y;
            this.z                  = z;
        }

        /** Getter for the designator.
         * @return the designator
         */
        public String getDesignator() {
            return designator;
        }

        /** Getter for the receiver identifier.
         * @return the receiver identifier
         */
        public String getReceiverIdentifier() {
            return receiverIdentifier;
        }

        /** Getter for the X coordinate in meters in considered Earth centered frame.
         * @return  the X coordinate in meters in considered Earth centered frame
         */
        public double getX() {
            return x;
        }

        /** Getter for the Y coordinate in meters in considered Earth centered frame.
         * @return  the Y coordinate in meters in considered Earth centered frame
         */
        public double getY() {
            return y;
        }

        /** Getter for the Z coordinate in meters in considered Earth centered frame.
         * @return  the Z coordinate in meters in considered Earth centered frame
         */
        public double getZ() {
            return z;
        }
    }

    /** Clock data type.
     * In case of a DR type, clock data are in the sense of clock value after discontinuity minus prior.
     * In other cases, clock data are in the sense of reported station/satellite clock minus reference clock value. */
    public enum ClockDataType {

        /** Data analysis for receiver clocks. Clock Data are*/
        AR("AR"),

        /** Data analysis for satellite clocks. */
        AS("AS"),

        /** Calibration measurement for a single GPS receiver. */
        CR("CR"),

        /** Discontinuity measurements for a single GPS receiver. */
        DR("DR"),

        /** Monitor measurements for the broadcast satellite clocks. */
        MS("MS");

        /** Parsing map. */
        private static final Map<String, ClockDataType> KEYS_MAP = new HashMap<>();
        static {
            for (final ClockDataType timeSystem : values()) {
                KEYS_MAP.put(timeSystem.getKey(), timeSystem);
            }
        }

        /** Key for the system. */
        private final String key;

        /** Simple constructor.
         * @param key key letter
         */
        ClockDataType(final String key) {
            this.key = key;
        }

        /** Get the key for the system.
         * @return key for the system
         */
        public String getKey() {
            return key;
        }

        /** Parse a string to get the time system.
         * <p>
         * The string must be the time system.
         * </p>
         * @param s string to parse
         * @return the time system
         * @exception OrekitIllegalArgumentException if the string does not correspond to a time system key
         */
        public static ClockDataType parseClockDataType(final String s)
            throws OrekitIllegalArgumentException {
            final ClockDataType clockDataType = KEYS_MAP.get(s);
            if (clockDataType == null) {
                throw new OrekitIllegalArgumentException(OrekitMessages.UNKNOWN_CLOCK_DATA_TYPE, s);
            }
            return clockDataType;
        }
    }
}

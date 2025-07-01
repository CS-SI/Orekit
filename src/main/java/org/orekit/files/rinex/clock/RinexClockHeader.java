/* Copyright 2022-2025 Thales Alenia Space
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

import org.orekit.files.rinex.section.RinexClockObsBaseHeader;
import org.orekit.files.rinex.utils.RinexFileType;
import org.orekit.frames.Frame;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.TimeSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeSpanMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Header for Rinex Clock.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class RinexClockHeader extends RinexClockObsBaseHeader {

    /** Maps {@link #frameName} to a {@link Frame}. */
    private final Function<? super String, ? extends Frame> frameBuilder;

    /** Earth centered frame name as a string. */
    private String frameName;

    /** Time system. */
    private TimeSystem timeSystem;

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
    private final TimeSpanMap<List<ReferenceClock>> referenceClocks;

    /** Satellite system code. */
    private final Map<SatelliteSystem, List<ObservationType>> systemObservationTypes;

    /** List of the data types in the file. */
    private final List<ClockDataType> clockDataTypes;

    /** Number of leap seconds separating UTC and GNSS time systems. */
    private int leapSecondsGNSS;

    /** List of the receivers in the file. */
    private final List<Receiver> receivers;

    /** List of the satellites in the file. */
    private final List<String> satellites;

    /** Simple constructor.
     * @param frameBuilder for constructing a reference frame from the identifier
     */
    public RinexClockHeader(final Function<? super String, ? extends Frame> frameBuilder) {
        super(RinexFileType.CLOCK);
        this.frameBuilder            = frameBuilder;
        this.frameName               = "";
        this.systemObservationTypes  = new HashMap<>();
        this.clockDataTypes          = new ArrayList<>();
        this.timeSystem              = null;
        this.stationIdentifier       = "";
        this.stationName             = "";
        this.externalClockReference  = "";
        this.analysisCenterID        = "";
        this.analysisCenterName      = "";
        this.referenceClocks         = new TimeSpanMap<>(null);
        this.leapSecondsGNSS         = 0;
        this.receivers               = new ArrayList<>();
        this.satellites              = new ArrayList<>();
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

    /** Add a list of reference clocks which will be used after a specified date.
     * If the reference map has not been already created, it will be.
     * @param referenceClockList the reference clock list
     * @param startDate the date the list will be valid after.
     */
    public void addReferenceClockList(final List<ReferenceClock> referenceClockList,
                                      final AbsoluteDate startDate) {
        referenceClocks.addValidAfter(referenceClockList, startDate, false);
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
        final List<ObservationType> list;
        synchronized (systemObservationTypes) {
            list = systemObservationTypes.computeIfAbsent(satSystem, s -> new ArrayList<>());
        }
        list.add(observationType);
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

    /** Get the number of different clock data types in the file.
     * @return the number of different clock data types
     */
    public int getNumberOfClockDataTypes() {
        return clockDataTypes.size();
    }

    /** Getter for the number of leap second for GNSS time scales.
     * @return the number of leap seconds for GNSS time scales
     */
    public int getLeapSecondsGNSS() {
        return leapSecondsGNSS;
    }

    /** Setter for the number of leap seconds for GNSS time scales.
     * @param leapSecondsGNSS the number of leap seconds for GNSS time scales to set
     */
    public void setLeapSecondsGNSS(final int leapSecondsGNSS) {
        this.leapSecondsGNSS = leapSecondsGNSS;
    }

    /** Get the reference frame for the station positions.
     * @return the reference frame for station positions
     */
    public Frame getFrame() {
        return frameBuilder.apply(frameName);
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

    /** Get the frame builder.
     * @return frame builder
     */
    public Function<? super String, ? extends Frame> getFrameBuilder() {
        return frameBuilder;
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
            if (rec.getDesignator().equals(receiver.getDesignator())) {
                notInList = false;
                break;
            }
        }
        // only add satellites which have not been added before
        if (notInList) {
            receivers.add(receiver);
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

}

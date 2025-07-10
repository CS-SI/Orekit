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

import org.orekit.files.rinex.section.CommonLabel;
import org.orekit.files.rinex.section.Label;
import org.orekit.files.rinex.section.RinexClockObsBaseHeader;
import org.orekit.files.rinex.utils.ParsingUtils;
import org.orekit.files.rinex.utils.RinexFileType;
import org.orekit.frames.Frame;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatInSystem;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.TimeSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.utils.TimeSpanMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Header for Rinex Clock.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class RinexClockHeader extends RinexClockObsBaseHeader {

    /** Index of label in header lines before 3.04. */
    public static final int LABEL_INDEX_300_302 = 60;

    /** Index of label in header lines for version 3.04 and later. */
    public static final int LABEL_INDEX_304_PLUS = 65;

    /** Indicator for header before version 3.04. */
    private boolean before304;

    /** System time scale. */
    private TimeScale timeScale;

    /** Earth centered frame. */
    private Frame frame;

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
    private final List<SatInSystem> satellites;

    /** Merged satellites systems. */
    private SatelliteSystem mergedSystems;

    /** Simple constructor.
     */
    public RinexClockHeader() {
        super(RinexFileType.CLOCK);
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
        this.mergedSystems           = null;
    }

    /** {@inheritDoc} */
    @Override
    public void setFormatVersion(final double formatVersion) {
        super.setFormatVersion(formatVersion);
        before304 = formatVersion < 3.04;
    }

    /** Check if header corresponds to a version before 3.04.
     * @return true if header corresponds to a version before 3.04
     */
    boolean isBefore304() {
        return before304;
    }

    /** {@inheritDoc} */
    @Override
    public SatelliteSystem parseSatelliteSystem(final String line, final SatelliteSystem defaultSatelliteSystem) {
        final String satSystemString = (getFormatVersion() < 3.04 ? line.substring(40, 41) : line.substring(42, 43)).trim();
        return SatelliteSystem.parseSatelliteSystem(satSystemString, defaultSatelliteSystem);
    }

    /** {@inheritDoc} */
    @Override
    public void parseProgramRunByDate(final String line, final TimeScales timeScales) {
        if (getFormatVersion() < 3.04) {
            parseProgramRunByDate(ParsingUtils.parseString(line, 0, 20),
                                  ParsingUtils.parseString(line, 20, 20),
                                  ParsingUtils.parseString(line, 40, 20),
                                  timeScales);
        } else {
            parseProgramRunByDate(ParsingUtils.parseString(line, 0, 19),
                                  ParsingUtils.parseString(line, 21, 19),
                                  ParsingUtils.parseString(line, 42, 21),
                                  timeScales);
        }
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

    /** Get the system time scale.
     * @return system time scale
     */
    public TimeScale getTimeScale() {
        return timeScale;
    }

    /** Set the system time scale.
     * @param timeScale system time scale
     */
    public void setTimeScale(final TimeScale timeScale) {
        this.timeScale = timeScale;
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
        return frame;
    }

    /** Set the reference frame for the station positions.
     * @param frame reference frame for station positions
     */
    public void setFrame(final Frame frame) {
        this.frame = frame;
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

    /** Add a new satellite with a given identifier to the list of stored satellites.
     * @param satId the satellite identifier
     */
    public void addSatellite(final SatInSystem satId) {

        // only add satellites which have not been added before
        if (!satellites.contains(satId)) {
            satellites.add(satId);

            // check if we have only one satellite system or mixed systems
            if (mergedSystems == null) {
                mergedSystems = satId.getSystem();
            } else if (satId.getSystem() != mergedSystems) {
                mergedSystems = SatelliteSystem.MIXED;
            }

        }

    }

    /** Get the merged satellites systems.
     * @return merged satellites systems
     * @since 14.0
     */
    SatelliteSystem getMergedSystem() {
        return mergedSystems;
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
    public List<SatInSystem> getSatellites() {
        return Collections.unmodifiableList(satellites);
    }

    /** {@inheritDoc} */
    @Override
    public void checkType(final String line, final String name) {
        checkType(line, getFormatVersion() < 3.04 ? 20 : 21, name);
    }

    /** {@inheritDoc} */
    @Override
    public int getLabelIndex() {
        return getFormatVersion() < 3.04 ? LABEL_INDEX_300_302 : LABEL_INDEX_304_PLUS;
    }

    /** {@inheritDoc} */
    @Override
    public boolean matchFound(final Label label, final String line) {
        // the position of the labels changes depending on version
        if (label == CommonLabel.VERSION) {
            // we are parsing the line RINEX VERSION / TYPE itself, the version is not known yet
            // so we try both positions
            return label.matches(line.substring(LABEL_INDEX_300_302).trim()) ||
                   label.matches(line.substring(LABEL_INDEX_304_PLUS).trim());
        } else {
            return label.matches(line.substring(getLabelIndex()).trim());
        }
    }

}

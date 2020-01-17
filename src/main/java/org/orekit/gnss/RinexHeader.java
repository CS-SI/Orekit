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
package org.orekit.gnss;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.orekit.gnss.RinexLoader.Parser.AppliedDCBS;
import org.orekit.gnss.RinexLoader.Parser.AppliedPCVS;
import org.orekit.gnss.RinexLoader.Parser.PhaseShiftCorrection;
import org.orekit.time.AbsoluteDate;

/** Container for Rinex file header.
 * @since 9.2
 */
public class RinexHeader {

    /** Rinex Version. */
    private final double rinexVersion;

    /** Satellite System of the Rinex file (G/R/S/E/M). */
    private final SatelliteSystem satelliteSystem;

    /** Name of the Antenna Marker. */
    private final String markerName;

    /** Number of Antenna marker. */
    private final String markerNumber;

    /** Type of Antenna marker. */
    private String markerType;

    /** Name of Observer. */
    private final String observerName;

    /** Name of Agency. */
    private final String agencyName;

    /** Receiver Number. */
    private final String receiverNumber;

    /** Receiver Type. */
    private final String receiverType;

    /** Receiver version. */
    private final String receiverVersion;

    /** Antenna Number. */
    private final String antennaNumber;

    /** Antenna Type. */
    private final String antennaType;

    /** Approximate Marker Position (WGS84). */
    private final Vector3D approxPos;

    /** Antenna Height. */
    private final double antHeight;

    /** Eccentricities of antenna center. */
    private final Vector2D eccentricities;

    /** Position of antenna reference point for antenna on vehicle. */
    private Vector3D antRefPoint;

    /** Observation code of the average phasecenter position w/r to antenna reference point. */
    private String obsCode;

    /** Antenna phasecenter.
     * North/East/Up (fixed station) or X/Y/Z in body fixed system (vehicle). */
    private Vector3D antPhaseCenter;

    /** Antenna B.Sight.
     * Direction of the “vertical” antenna axis towards the GNSS satellites.  */
    private Vector3D antBSight;

    /** Azimuth of the zero direction of a fixed antenna (degrees, from north). */
    private double antAzi;

    /** Zero direction of antenna. */
    private Vector3D antZeroDir;

    /** Current center of mass (X,Y,Z, meters) of vehicle in body fixed coordinate system. */
    private Vector3D centerMass;

    /** Unit of the carrier to noise ratio observables Snn (if present) DBHZ: S/N given in dbHz. */
    private String sigStrengthUnit;

    /** Observation interval in seconds. */
    private final double interval;

    /** Time of First observation record. */
    private final AbsoluteDate tFirstObs;

    /** Time of las observation record. */
    private final AbsoluteDate tLastObs;

    /** Realtime-derived receiver clock offset. */
    private final int clkOffset;

    /** List of applied differential code bias corrections. */
    private List<AppliedDCBS> listAppliedDCBS;

    /** List of antenna center variation corrections. */
    private List<AppliedPCVS> listAppliedPCVS;

    /** List of phase shift correction used to generate phases consistent w/r to cycle shifts. */
    private List<PhaseShiftCorrection> phaseShiftCorrections;

    /** Number of leap seconds since 6-Jan-1980. */
    private final int leapSeconds;

    /** Future or past leap seconds ΔtLSF (BNK).
     * i.e. future leap second if the week and day number are in the future. */
    private int leapSecondsFuture;

    /** Respective leap second week number.
     * For GPS, GAL, QZS and IRN, weeks since 6-Jan-1980.
     * When BDS only file leap seconds specified, weeks since 1-Jan-2006 */
    private int leapSecondsWeekNum;

    /** Respective leap second day number. */
    private int leapSecondsDayNum;

    /** Simple constructor, for Rinex 2 Header.
     * @param rinexVersion rinex version
     * @param satelliteSystem Satellite System of the observation file (G/R/S/E/M)
     * @param markerName name of the antenna marker
     * @param markerNumber number of the antenna marker
     * @param markerType Type of Antenna marker
     * @param observerName name of the observer
     * @param agencyName name of the agency
     * @param receiverNumber number of the receiver
     * @param receiverType type of the receiver
     * @param receiverVersion version of the receiver
     * @param antennaNumber antenna number
     * @param antennaType type of the antenna
     * @param approxPos Approximate Marker Position (WGS84)
     * @param antHeight antenna height
     * @param eccentricities Eccentricities of antenna center
     * @param antRefPoint Position of antenna reference point for antenna on vehicle
     * @param antBSight Antenna B.Sight
     * @param centerMass Current center of mass of vehicle in body fixed coordinate system
     * @param interval Observation interval in seconds
     * @param tFirstObs Time of First observation record
     * @param tLastObs Time of last observation record
     * @param clkOffset Realtime-derived receiver clock offset
     * @param leapSeconds Number of leap seconds since 6-Jan-1980
     */
    public RinexHeader(final double rinexVersion, final SatelliteSystem satelliteSystem,
                       final String markerName, final String markerNumber, final String markerType,
                       final String observerName, final String agencyName, final String receiverNumber,
                       final String receiverType, final String receiverVersion, final String antennaNumber,
                       final String antennaType, final Vector3D approxPos, final double antHeight,
                       final Vector2D eccentricities, final Vector3D antRefPoint, final Vector3D antBSight,
                       final Vector3D centerMass, final double interval, final AbsoluteDate tFirstObs, final AbsoluteDate tLastObs,
                       final int clkOffset, final int leapSeconds) {
        this.rinexVersion = rinexVersion;
        this.satelliteSystem = satelliteSystem;
        this.markerName = markerName;
        this.markerNumber = markerNumber;
        this.markerType = markerType;
        this.observerName = observerName;
        this.agencyName = agencyName;
        this.receiverNumber = receiverNumber;
        this.receiverType = receiverType;
        this.receiverVersion = receiverVersion;
        this.antennaNumber = antennaNumber;
        this.antennaType = antennaType;
        this.approxPos = approxPos;
        this.antHeight = antHeight;
        this.eccentricities = eccentricities;
        this.antRefPoint = antRefPoint;
        this.antBSight = antBSight;
        this.centerMass = centerMass;
        this.interval = interval;
        this.tFirstObs = tFirstObs;
        this.tLastObs = tLastObs;
        this.clkOffset = clkOffset;
        this.leapSeconds = leapSeconds;

    }

    /** Simple constructor, for Rinex 3 Header.
    * @param rinexVersion rinex version
    * @param satelliteSystem Satellite System of the observation file (G/R/S/E/M)
    * @param markerName name of the antenna marker
    * @param markerNumber number of the antenna marker
    * @param markerType Type of Antenna marker
    * @param observerName name of the observer
    * @param agencyName name of the agency
    * @param receiverNumber number of the receiver
    * @param receiverType type of the receiver
    * @param receiverVersion version of the receiver
    * @param antennaNumber antenna number
    * @param antennaType type of the antenna
    * @param approxPos Approximate Marker Position (WGS84)
    * @param antHeight antenna height
    * @param eccentricities Eccentricities of antenna center
    * @param antRefPoint Position of antenna reference point for antenna on vehicle
    * @param obsCode Observation code of the average phasecenter position w/r to antenna reference point
    * @param antPhaseCenter Antenna phasecenter
    * @param antBSight Antenna B.Sight
    * @param antAzi Azimuth of the zero direction of a fixed antenna
    * @param antZeroDir Zero direction of antenna
    * @param centerMass Current center of mass of vehicle in body fixed coordinate system
    * @param sigStrengthUnit Unit of the carrier to noise ratio observables
    * @param interval Observation interval in seconds
    * @param tFirstObs Time of First observation record
    * @param tLastObs Time of last observation record
    * @param clkOffset Realtime-derived receiver clock offset
    * @param listAppliedDCBS List of applied differential code bias corrections
    * @param listAppliedPCVS List of antenna center variation corrections
    * @param phaseShiftCorrections List of phase shift correction used to generate phases consistent w/r to cycle shifts
    * @param leapSeconds Number of leap seconds since 6-Jan-1980
    * @param leapSecondsFuture Future or past leap seconds
    * @param leapSecondsWeekNum Respective leap second week number
    * @param leapSecondsDayNum Respective leap second day number
    */
    public RinexHeader(final double rinexVersion, final SatelliteSystem satelliteSystem,
                       final String markerName, final String markerNumber, final String markerType,
                       final String observerName, final String agencyName, final String receiverNumber,
                       final String receiverType, final String receiverVersion, final String antennaNumber,
                       final String antennaType, final Vector3D approxPos, final double antHeight,
                       final Vector2D eccentricities, final Vector3D antRefPoint, final String obsCode,
                       final Vector3D antPhaseCenter, final Vector3D antBSight, final double antAzi,
                       final Vector3D antZeroDir, final Vector3D centerMass, final String sigStrengthUnit,
                       final double interval, final AbsoluteDate tFirstObs, final AbsoluteDate tLastObs,
                       final int clkOffset, final List<AppliedDCBS> listAppliedDCBS,
                       final List<AppliedPCVS> listAppliedPCVS,
                       final List<PhaseShiftCorrection> phaseShiftCorrections, final int leapSeconds,
                       final int leapSecondsFuture, final int leapSecondsWeekNum, final int leapSecondsDayNum) {
        this.rinexVersion = rinexVersion;
        this.satelliteSystem = satelliteSystem;
        this.markerName = markerName;
        this.markerNumber = markerNumber;
        this.observerName = observerName;
        this.agencyName = agencyName;
        this.receiverNumber = receiverNumber;
        this.receiverType = receiverType;
        this.receiverVersion = receiverVersion;
        this.antennaNumber = antennaNumber;
        this.antennaType = antennaType;
        this.approxPos = approxPos;
        this.antHeight = antHeight;
        this.eccentricities = eccentricities;
        this.clkOffset = clkOffset;
        this.interval = interval;
        this.tFirstObs = tFirstObs;
        this.tLastObs = tLastObs;
        this.leapSeconds = leapSeconds;
        this.markerType = markerType;
        this.sigStrengthUnit = sigStrengthUnit;
        this.phaseShiftCorrections = phaseShiftCorrections;
        this.obsCode = obsCode;
        this.listAppliedDCBS = listAppliedDCBS;
        this.listAppliedPCVS = listAppliedPCVS;
        this.leapSecondsDayNum = leapSecondsDayNum;
        this.leapSecondsFuture = leapSecondsFuture;
        this.leapSecondsWeekNum = leapSecondsWeekNum;
        this.centerMass = centerMass;
        this.antAzi = antAzi;
        this.antBSight = antBSight;
        this.antZeroDir = antZeroDir;
        this.antRefPoint = antRefPoint;
        this.antPhaseCenter = antPhaseCenter;

    }

    /** Get Rinex Version.
     * @return rinex version of the file
     */
    public double getRinexVersion() {
        return rinexVersion;
    }

    /** Get Satellite System.
     * @return satellite system of the observation file
     */
    public SatelliteSystem getSatelliteSystem() {
        return satelliteSystem;
    }

    /** Get name of the antenna marker.
     * @return name of the antenna marker
     */
    public String getMarkerName() {
        return markerName;
    }

    /** Get number of the antenna marker.
     * @return number of the antenna marker
     */
    public String getMarkerNumber() {
        return markerNumber;
    }

    /** Get name of the observer.
     * @return name of the observer
     */
    public String getObserverName() {
        return observerName;
    }

    /** Get name of the agency.
     * @return name of the agency
     */
    public String getAgencyName() {
        return agencyName;
    }

    /** Get the number of the receiver.
     * @return number of the receiver
     */
    public String getReceiverNumber() {
        return receiverNumber;
    }

    /** Get the type of the receiver.
     * @return type of the receiver
     */
    public String getReceiverType() {
        return receiverType;
    }

    /** Get the version of the receiver.
     * @return version of the receiver
     */
    public String getReceiverVersion() {
        return receiverVersion;
    }

    /** Get the number of the antenna.
     * @return number of the antenna
     */
    public String getAntennaNumber() {
        return antennaNumber;
    }

    /** Get the type of the antenna.
     * @return type of the antenna
     */
    public String getAntennaType() {
        return antennaType;
    }

    /** Get the Approximate Marker Position.
     * @return Approximate Marker Position
     */
    public Vector3D getApproxPos() {
        return approxPos;
    }

    /** Get the antenna height.
     * @return height of the antenna
     */
    public double getAntennaHeight() {
        return antHeight;
    }

    /** Get the eccentricities of antenna center.
     * @return Eccentricities of antenna center
     */
    public Vector2D getEccentricities() {
        return eccentricities;
    }

    /** Get the realtime-derived receiver clock offset.
     * @return realtime-derived receiver clock offset
     */
    public int getClkOffset() {
        return clkOffset;
    }

    /** Get the observation interval in seconds.
     * @return Observation interval in seconds
     */
    public double getInterval() {
        return interval;
    }

    /** Get the time of First observation record.
     * @return Time of First observation record
     */
    public AbsoluteDate getTFirstObs() {
        return tFirstObs;
    }

    /** Get the time of last observation record.
     * @return Time of last observation record
     */
    public AbsoluteDate getTLastObs() {
        return tLastObs;
    }

    /** Get the Number of leap seconds since 6-Jan-1980.
     * @return Number of leap seconds since 6-Jan-1980
     */
    public int getLeapSeconds() {
        return leapSeconds;
    }

    /** Get type of the antenna marker.
     * @return type of the antenna marker
     */
    public String getMarkerType() {
        return markerType;
    }

    /** Get the position of antenna reference point for antenna on vehicle.
     * @return Position of antenna reference point for antenna on vehicle
     */
    public Vector3D getAntennaReferencePoint() {
        return antRefPoint;
    }

    /** Get the observation code of the average phasecenter position w/r to antenna reference point.
     * @return Observation code of the average phasecenter position w/r to antenna reference point
     */
    public String getObservationCode() {
        return obsCode;
    }

    /** Get the antenna phasecenter.
     * @return Antenna phasecenter
     */
    public Vector3D getAntennaPhaseCenter() {
        return antPhaseCenter;
    }

    /** Get the antenna B.Sight.
     * @return Antenna B.Sight
     */
    public Vector3D getAntennaBSight() {
        return antBSight;
    }

    /** Get the azimuth of the zero direction of a fixed antenna.
     * @return Azimuth of the zero direction of a fixed antenna
     */
    public double getAntennaAzimuth() {
        return antAzi;
    }

    /** Get the zero direction of antenna.
     * @return Zero direction of antenna
     */
    public Vector3D getAntennaZeroDirection() {
        return antZeroDir;
    }

    /** Get the current center of mass of vehicle in body fixed coordinate system.
     * @return Current center of mass of vehicle in body fixed coordinate system
     */
    public Vector3D getCenterMass() {
        return centerMass;
    }

    /** Get the unit of the carrier to noise ratio observables.
     * @return Unit of the carrier to noise ratio observables
     */
    public String getSignalStrengthUnit() {
        return sigStrengthUnit;
    }

    /** Get the future or past leap seconds.
     * @return Future or past leap seconds
     */
    public int getLeapSecondsFuture() {
        return leapSecondsFuture;
    }

    /** Get the respective leap second week number.
     * @return Respective leap second week number
     */
    public int getLeapSecondsWeekNum() {
        return leapSecondsWeekNum;
    }

    /** Get the respective leap second day number.
     * @return Respective leap second day number
     */
    public int getLeapSecondsDayNum() {
        return leapSecondsDayNum;
    }

    /** Get the list of applied differential code bias corrections.
     * @return list of applied differential code bias corrections
     */
    public List<AppliedDCBS> getListAppliedDCBS() {
        return listAppliedDCBS;
    }

    /** Get the list of antenna center variation corrections.
     * @return List of antenna center variation corrections
     */
    public List<AppliedPCVS> getListAppliedPCVS() {
        return listAppliedPCVS;
    }

    /** Get the list of phase shift correction used to generate phases consistent w/r to cycle shifts.
     * @return List of phase shift correction used to generate phases consistent w/r to cycle shifts
     */
    public List<PhaseShiftCorrection> getPhaseShiftCorrections() {
        return phaseShiftCorrections;
    }

}

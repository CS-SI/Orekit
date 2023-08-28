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
package org.orekit.files.rinex.observation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.orekit.files.rinex.AppliedDCBS;
import org.orekit.files.rinex.AppliedPCVS;
import org.orekit.files.rinex.section.RinexBaseHeader;
import org.orekit.files.rinex.utils.RinexFileType;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatInSystem;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;

/** Container for Rinex observation file header.
 * @since 9.2
 */
public class RinexObservationHeader extends RinexBaseHeader {

    /** Name of the Antenna Marker. */
    private String markerName;

    /** Number of Antenna marker. */
    private String markerNumber;

    /** Type of Antenna marker. */
    private String markerType;

    /** Name of Observer. */
    private String observerName;

    /** Name of Agency. */
    private String agencyName;

    /** Receiver Number. */
    private String receiverNumber;

    /** Receiver Type. */
    private String receiverType;

    /** Receiver version. */
    private String receiverVersion;

    /** Antenna Number. */
    private String antennaNumber;

    /** Antenna Type. */
    private String antennaType;

    /** Approximate Marker Position (WGS84). */
    private Vector3D approxPos;

    /** Antenna Height. */
    private double antennaHeight;

    /** Eccentricities of antenna center. */
    private Vector2D eccentricities;

    /** Position of antenna reference point for antenna on vehicle. */
    private Vector3D antRefPoint;

    /** Satellite system for average phasecenter position.
     * @since 12.0
     */
    private SatelliteSystem phaseCenterSystem;

    /** Observation code of the average phasecenter position w/r to antenna reference point. */
    private String observationCode;

    /** Antenna phasecenter.
     * North/East/Up (fixed station) or X/Y/Z in body fixed system (vehicle). */
    private Vector3D antennaPhaseCenter;

    /** Antenna B.Sight.
     * Direction of the “vertical” antenna axis towards the GNSS satellites.  */
    private Vector3D antennaBSight;

    /** Azimuth of the zero direction of a fixed antenna (degrees, from north). */
    private double antennaAzimuth;

    /** Zero direction of antenna. */
    private Vector3D antennaZeroDirection;

    /** Current center of mass (X,Y,Z, meters) of vehicle in body fixed coordinate system. */
    private Vector3D centerMass;

    /** Unit of the carrier to noise ratio observables Snn (if present) DBHZ: S/N given in dbHz. */
    private String signalStrengthUnit;

    /** Observation interval in seconds. */
    private double interval;

    /** Time of First observation record. */
    private AbsoluteDate tFirstObs;

    /** Time of last observation record. */
    private AbsoluteDate tLastObs;

    /** Real time-derived receiver clock offset. */
    private int clkOffset;

    /** List of applied differential code bias corrections. */
    private List<AppliedDCBS> listAppliedDCBS;

    /** List of antenna center variation corrections. */
    private List<AppliedPCVS> listAppliedPCVS;

    /** List of phase shift correction used to generate phases consistent w/r to cycle shifts. */
    private List<PhaseShiftCorrection> phaseShiftCorrections;

    /** List of scale factor corrections. */
    private Map<SatelliteSystem, List<ScaleFactorCorrection>> scaleFactorCorrections;

    /** List of GLONASS satellite-channel associations.
     * @since 12.0
     */
    private final List<GlonassSatelliteChannel> glonassChannels;

    /** Number of satellites.
     * @since 12.0
     */
    private int nbSat;

    /** Number of observations per satellite.
     * @since 12.0
     */
    private final Map<SatInSystem, Map<ObservationType, Integer>> nbObsPerSat;

    /** Observation types for each satellite systems.
     * @since 12.0
     */
    private final Map<SatelliteSystem, List<ObservationType>> mapTypeObs;

    /** Number of leap seconds since 6-Jan-1980. */
    private int leapSeconds;

    /** Future or past leap seconds ΔtLSF (BNK).
     * i.e. future leap second if the week and day number are in the future.
     */
    private int leapSecondsFuture;

    /** Respective leap second week number.
     * For GPS, GAL, QZS and IRN, weeks since 6-Jan-1980.
     * When BDS only file leap seconds specified, weeks since 1-Jan-2006
     */
    private int leapSecondsWeekNum;

    /** Respective leap second day number. */
    private int leapSecondsDayNum;

    /** Code phase bias correction for GLONASS C1C signal.
     * @since 12.0
     */
    private double c1cCodePhaseBias;

    /** Code phase bias correction for GLONASS C1P signal.
     * @since 12.0
     */
    private double c1pCodePhaseBias;

    /** Code phase bias correction for GLONASS C2C signal.
     * @since 12.0
     */
    private double c2cCodePhaseBias;

    /** Code phase bias correction for GLONASS C2P signal.
     * @since 12.0
     */
    private double c2pCodePhaseBias;

    /** Simple constructor.
     */
    public RinexObservationHeader() {
        super(RinexFileType.OBSERVATION);
        antennaAzimuth         = Double.NaN;
        antennaHeight          = Double.NaN;
        eccentricities         = Vector2D.ZERO;
        clkOffset              = -1;
        nbSat                  = -1;
        interval               = Double.NaN;
        leapSeconds            = 0;
        listAppliedDCBS        = new ArrayList<>();
        listAppliedPCVS        = new ArrayList<>();
        phaseShiftCorrections  = new ArrayList<>();
        scaleFactorCorrections = new HashMap<>();
        glonassChannels        = new ArrayList<>();
        nbObsPerSat            = new HashMap<>();
        mapTypeObs             = new HashMap<>();
        tLastObs               = AbsoluteDate.FUTURE_INFINITY;
        c1cCodePhaseBias       = Double.NaN;
        c1pCodePhaseBias       = Double.NaN;
        c2cCodePhaseBias       = Double.NaN;
        c2pCodePhaseBias       = Double.NaN;
    }

    /** Set name of the antenna marker.
     * @param markerName name of the antenna marker
     */
    public void setMarkerName(final String markerName) {
        this.markerName = markerName;
    }

    /** Get name of the antenna marker.
     * @return name of the antenna marker
     */
    public String getMarkerName() {
        return markerName;
    }

    /** Set number of the antenna marker.
     * @param markerNumber number of the antenna marker
     */
    public void setMarkerNumber(final String markerNumber) {
        this.markerNumber = markerNumber;
    }

    /** Get number of the antenna marker.
     * @return number of the antenna marker
     */
    public String getMarkerNumber() {
        return markerNumber;
    }

    /** Set name of the observer.
     * @param observerName name of the observer
     */
    public void setObserverName(final String observerName) {
        this.observerName = observerName;
    }

    /** Get name of the observer.
     * @return name of the observer
     */
    public String getObserverName() {
        return observerName;
    }

    /**
     * Setter for the agency name.
     * @param agencyName the agency name to set
     */
    public void setAgencyName(final String agencyName) {
        this.agencyName = agencyName;
    }

    /** Get name of the agency.
     * @return name of the agency
     */
    public String getAgencyName() {
        return agencyName;
    }

    /** Set the number of the receiver.
     * @param receiverNumber number of the receiver
     */
    public void setReceiverNumber(final String receiverNumber) {
        this.receiverNumber = receiverNumber;
    }

    /** Get the number of the receiver.
     * @return number of the receiver
     */
    public String getReceiverNumber() {
        return receiverNumber;
    }

    /** Set the type of the receiver.
     * @param receiverType type of the receiver
     */
    public void setReceiverType(final String receiverType) {
        this.receiverType = receiverType;
    }

    /** Get the type of the receiver.
     * @return type of the receiver
     */
    public String getReceiverType() {
        return receiverType;
    }

    /** Set the version of the receiver.
     * @param receiverVersion version of the receiver
     */
    public void setReceiverVersion(final String receiverVersion) {
        this.receiverVersion = receiverVersion;
    }

    /** Get the version of the receiver.
     * @return version of the receiver
     */
    public String getReceiverVersion() {
        return receiverVersion;
    }

    /** Set the number of the antenna.
     * @param antennaNumber number of the antenna
     */
    public void setAntennaNumber(final String antennaNumber) {
        this.antennaNumber = antennaNumber;
    }

    /** Get the number of the antenna.
     * @return number of the antenna
     */
    public String getAntennaNumber() {
        return antennaNumber;
    }

    /** Set the type of the antenna.
     * @param antennaType type of the antenna
     */
    public void setAntennaType(final String antennaType) {
        this.antennaType = antennaType;
    }

    /** Get the type of the antenna.
     * @return type of the antenna
     */
    public String getAntennaType() {
        return antennaType;
    }

    /** Set the Approximate Marker Position.
     * @param approxPos Approximate Marker Position
     */
    public void setApproxPos(final Vector3D approxPos) {
        this.approxPos = approxPos;
    }

    /** Get the Approximate Marker Position.
     * @return Approximate Marker Position
     */
    public Vector3D getApproxPos() {
        return approxPos;
    }

    /** Set the antenna height.
     * @param antennaHeight height of the antenna
     */
    public void setAntennaHeight(final double antennaHeight) {
        this.antennaHeight = antennaHeight;
    }

    /** Get the antenna height.
     * @return height of the antenna
     */
    public double getAntennaHeight() {
        return antennaHeight;
    }

    /** Set the eccentricities of antenna center.
     * @param eccentricities Eccentricities of antenna center
     */
    public void setEccentricities(final Vector2D eccentricities) {
        this.eccentricities = eccentricities;
    }

    /** Get the eccentricities of antenna center.
     * @return Eccentricities of antenna center
     */
    public Vector2D getEccentricities() {
        return eccentricities;
    }

    /** Set the realtime-derived receiver clock offset.
     * @param clkOffset realtime-derived receiver clock offset
     */
    public void setClkOffset(final int clkOffset) {
        this.clkOffset = clkOffset;
    }

    /** Get the realtime-derived receiver clock offset.
     * @return realtime-derived receiver clock offset
     */
    public int getClkOffset() {
        return clkOffset;
    }

    /** Set the observation interval in seconds.
     * @param interval Observation interval in seconds
     */
    public void setInterval(final double interval) {
        this.interval = interval;
    }

    /** Get the observation interval in seconds.
     * @return Observation interval in seconds
     */
    public double getInterval() {
        return interval;
    }

    /** Set the time of First observation record.
     * @param firstObs Time of First observation record
     */
    public void setTFirstObs(final AbsoluteDate firstObs) {
        this.tFirstObs = firstObs;
    }

    /** Get the time of First observation record.
     * @return Time of First observation record
     */
    public AbsoluteDate getTFirstObs() {
        return tFirstObs;
    }

    /** Set the time of last observation record.
     * @param lastObs Time of last observation record
     */
    public void setTLastObs(final AbsoluteDate lastObs) {
        this.tLastObs = lastObs;
    }

    /** Get the time of last observation record.
     * @return Time of last observation record
     */
    public AbsoluteDate getTLastObs() {
        return tLastObs;
    }

    /** Set the Number of leap seconds since 6-Jan-1980.
     * @param leapSeconds Number of leap seconds since 6-Jan-1980
     */
    public void setLeapSeconds(final int leapSeconds) {
        this.leapSeconds = leapSeconds;
    }

    /** Get the Number of leap seconds since 6-Jan-1980.
     * @return Number of leap seconds since 6-Jan-1980
     */
    public int getLeapSeconds() {
        return leapSeconds;
    }

    /** Set type of the antenna marker.
     * @param markerType type of the antenna marker
     */
    public void setMarkerType(final String markerType) {
        this.markerType = markerType;
    }

    /** Get type of the antenna marker.
     * @return type of the antenna marker
     */
    public String getMarkerType() {
        return markerType;
    }

    /** Set the position of antenna reference point for antenna on vehicle.
     * @param refPoint Position of antenna reference point for antenna on vehicle
     */
    public void setAntennaReferencePoint(final Vector3D refPoint) {
        this.antRefPoint = refPoint;
    }

    /** Get the position of antenna reference point for antenna on vehicle.
     * @return Position of antenna reference point for antenna on vehicle
     */
    public Vector3D getAntennaReferencePoint() {
        return antRefPoint;
    }

    /** Set satellite system for average phase center.
     * @param phaseCenterSystem satellite system for average phase center
     * @since 12.0
     */
    public void setPhaseCenterSystem(final SatelliteSystem phaseCenterSystem) {
        this.phaseCenterSystem = phaseCenterSystem;
    }

    /** Get satellite system for average phase center.
     * @return satellite system for average phase center
     * @since 12.0
     */
    public SatelliteSystem getPhaseCenterSystem() {
        return phaseCenterSystem;
    }

    /** Set the observation code of the average phasecenter position w/r to antenna reference point.
     * @param observationCode Observation code of the average phasecenter position w/r to antenna reference point
     */
    public void setObservationCode(final String observationCode) {
        this.observationCode = observationCode;
    }

    /** Get the observation code of the average phasecenter position w/r to antenna reference point.
     * @return Observation code of the average phasecenter position w/r to antenna reference point
     */
    public String getObservationCode() {
        return observationCode;
    }

    /** Set the antenna phasecenter.
     * @param antennaPhaseCenter Antenna phasecenter
     */
    public void setAntennaPhaseCenter(final Vector3D antennaPhaseCenter) {
        this.antennaPhaseCenter = antennaPhaseCenter;
    }

    /** Get the antenna phasecenter.
     * @return Antenna phasecenter
     */
    public Vector3D getAntennaPhaseCenter() {
        return antennaPhaseCenter;
    }

    /** Set the antenna B.Sight.
     * @param antennaBSight Antenna B.Sight
     */
    public void setAntennaBSight(final Vector3D antennaBSight) {
        this.antennaBSight = antennaBSight;
    }

    /** Get the antenna B.Sight.
     * @return Antenna B.Sight
     */
    public Vector3D getAntennaBSight() {
        return antennaBSight;
    }

    /** Set the azimuth of the zero direction of a fixed antenna.
     * @param antennaAzimuth Azimuth of the zero direction of a fixed antenna
     */
    public void setAntennaAzimuth(final double antennaAzimuth) {
        this.antennaAzimuth = antennaAzimuth;
    }

    /** Get the azimuth of the zero direction of a fixed antenna.
     * @return Azimuth of the zero direction of a fixed antenna
     */
    public double getAntennaAzimuth() {
        return antennaAzimuth;
    }

    /** Set the zero direction of antenna.
     * @param antennaZeroDirection Zero direction of antenna
     */
    public void setAntennaZeroDirection(final Vector3D antennaZeroDirection) {
        this.antennaZeroDirection = antennaZeroDirection;
    }

    /** Get the zero direction of antenna.
     * @return Zero direction of antenna
     */
    public Vector3D getAntennaZeroDirection() {
        return antennaZeroDirection;
    }

    /** Set the current center of mass of vehicle in body fixed coordinate system.
     * @param centerMass Current center of mass of vehicle in body fixed coordinate system
     */
    public void setCenterMass(final Vector3D centerMass) {
        this.centerMass = centerMass;
    }

    /** Get the current center of mass of vehicle in body fixed coordinate system.
     * @return Current center of mass of vehicle in body fixed coordinate system
     */
    public Vector3D getCenterMass() {
        return centerMass;
    }

    /** Set the unit of the carrier to noise ratio observables.
     * @param signalStrengthUnit Unit of the carrier to noise ratio observables
     */
    public void setSignalStrengthUnit(final String signalStrengthUnit) {
        this.signalStrengthUnit = signalStrengthUnit;
    }

    /** Get the unit of the carrier to noise ratio observables.
     * @return Unit of the carrier to noise ratio observables
     */
    public String getSignalStrengthUnit() {
        return signalStrengthUnit;
    }

    /** Set the future or past leap seconds.
     * @param leapSecondsFuture Future or past leap seconds
     */
    public void setLeapSecondsFuture(final int leapSecondsFuture) {
        this.leapSecondsFuture = leapSecondsFuture;
    }

    /** Get the future or past leap seconds.
     * @return Future or past leap seconds
     */
    public int getLeapSecondsFuture() {
        return leapSecondsFuture;
    }

    /** Set the respective leap second week number.
     * @param leapSecondsWeekNum Respective leap second week number
     */
    public void setLeapSecondsWeekNum(final int leapSecondsWeekNum) {
        this.leapSecondsWeekNum = leapSecondsWeekNum;
    }

    /** Get the respective leap second week number.
     * @return Respective leap second week number
     */
    public int getLeapSecondsWeekNum() {
        return leapSecondsWeekNum;
    }

    /** Set the respective leap second day number.
     * @param leapSecondsDayNum Respective leap second day number
     */
    public void setLeapSecondsDayNum(final int leapSecondsDayNum) {
        this.leapSecondsDayNum = leapSecondsDayNum;
    }

    /** Get the respective leap second day number.
     * @return Respective leap second day number
     */
    public int getLeapSecondsDayNum() {
        return leapSecondsDayNum;
    }

    /** Add applied differential code bias corrections.
     * @param appliedDCBS applied differential code bias corrections to add
     */
    public void addAppliedDCBS(final AppliedDCBS appliedDCBS) {
        listAppliedDCBS.add(appliedDCBS);
    }

    /** Get the list of applied differential code bias corrections.
     * @return list of applied differential code bias corrections
     */
    public List<AppliedDCBS> getListAppliedDCBS() {
        return Collections.unmodifiableList(listAppliedDCBS);
    }

    /** Add antenna center variation corrections.
     * @param appliedPCVS antenna center variation corrections
     */
    public void addAppliedPCVS(final AppliedPCVS appliedPCVS) {
        listAppliedPCVS.add(appliedPCVS);
    }

    /** Get the list of antenna center variation corrections.
     * @return List of antenna center variation corrections
     */
    public List<AppliedPCVS> getListAppliedPCVS() {
        return Collections.unmodifiableList(listAppliedPCVS);
    }

    /** Add phase shift correction used to generate phases consistent w/r to cycle shifts.
     * @param phaseShiftCorrection phase shift correction used to generate phases consistent w/r to cycle shifts
     */
    public void addPhaseShiftCorrection(final PhaseShiftCorrection phaseShiftCorrection) {
        phaseShiftCorrections.add(phaseShiftCorrection);
    }

    /** Get the list of phase shift correction used to generate phases consistent w/r to cycle shifts.
     * @return List of phase shift correction used to generate phases consistent w/r to cycle shifts
     */
    public List<PhaseShiftCorrection> getPhaseShiftCorrections() {
        return Collections.unmodifiableList(phaseShiftCorrections);
    }

    /** Add scale factor correction.
     * @param satelliteSystem system to which this scaling factor applies
     * @param scaleFactorCorrection scale factor correction
     */
    public void addScaleFactorCorrection(final SatelliteSystem satelliteSystem, final ScaleFactorCorrection scaleFactorCorrection) {
        List<ScaleFactorCorrection> sfc = scaleFactorCorrections.get(satelliteSystem);
        if (sfc == null) {
            sfc = new ArrayList<>();
            scaleFactorCorrections.put(satelliteSystem, sfc);
        }
        sfc.add(scaleFactorCorrection);
    }

    /** Get the list of scale factor correction.
     * @param satelliteSystem system to which this scaling factor applies
     * @return List of scale factor correction
     */
    public List<ScaleFactorCorrection> getScaleFactorCorrections(final SatelliteSystem satelliteSystem) {
        final List<ScaleFactorCorrection> sfc = scaleFactorCorrections.get(satelliteSystem);
        return sfc == null ? Collections.emptyList() : Collections.unmodifiableList(sfc);
    }

    /** Add GLONASS satellite/channel association.
     * @param glonassChannel GLONASS satellite/channel association
     * @since 12.0
     */
    public void addGlonassChannel(final GlonassSatelliteChannel glonassChannel) {
        glonassChannels.add(glonassChannel);
    }

    /** Get the list of GLONASS satellite/channel associations.
     * @return List of GLONASS satellite/channel associations
     * @since 12.0
     */
    public List<GlonassSatelliteChannel> getGlonassChannels() {
        return Collections.unmodifiableList(glonassChannels);
    }

    /** Set number of satellites.
     * @param nbSat number of satellites
     * @since 12.0
     */
    public void setNbSat(final int nbSat) {
        this.nbSat = nbSat;
    }

    /** Get number of satellites.
     * @return number of satellites
     * @since 12.0
     */
    public int getNbSat() {
        return nbSat;
    }

    /** Set number of observations for a satellite.
     * @param sat satellite
     * @param type observation type
     * @param nbObs number of observations of this type for this satellite
     * @since 12.0
     */
    public void setNbObsPerSatellite(final SatInSystem sat, final ObservationType type, final int nbObs) {
        Map<ObservationType, Integer> satNbObs = nbObsPerSat.get(sat);
        if (satNbObs == null) {
            satNbObs = new HashMap<>();
            nbObsPerSat.put(sat, satNbObs);
        }
        satNbObs.put(type, nbObs);
    }

    /** Get an unmodifiable view of the map of number of observations per satellites.
     * @return unmodifiable view of the map of number of observations per satellites
     * @since 12.0
     */
    public Map<SatInSystem, Map<ObservationType, Integer>> getNbObsPerSat() {
        return Collections.unmodifiableMap(nbObsPerSat);
    }

    /** Set number of observations for a satellite.
     * @param system satellite system
     * @param types observation types
     * @since 12.0
     */
    public void setTypeObs(final SatelliteSystem system, final List<ObservationType> types) {
        mapTypeObs.put(system, new ArrayList<>(types));
    }

    /** Get an unmodifiable view of the map of observation types.
     * @return unmodifiable view of the map of observation types
     * @since 12.0
     */
    public Map<SatelliteSystem, List<ObservationType>> getTypeObs() {
        return Collections.unmodifiableMap(mapTypeObs);
    }

    /** Set the code phase bias correction for GLONASS {@link ObservationType#C1C} signal.
     * @param c1cCodePhaseBias code phase bias correction for GLONASS {@link ObservationType#C1C} signal
     * @since 12.0
     */
    public void setC1cCodePhaseBias(final double c1cCodePhaseBias) {
        this.c1cCodePhaseBias = c1cCodePhaseBias;
    }

    /** Get the code phase bias correction for GLONASS {@link ObservationType#C1C} signal.
     * @return code phase bias correction for GLONASS {@link ObservationType#C1C} signal
     * @since 12.0
     */
    public double getC1cCodePhaseBias() {
        return c1cCodePhaseBias;
    }

    /** Set the code phase bias correction for GLONASS {@link ObservationType#C1P} signal.
     * @param c1pCodePhaseBias code phase bias correction for GLONASS {@link ObservationType#C1P} signal
     * @since 12.0
     */
    public void setC1pCodePhaseBias(final double c1pCodePhaseBias) {
        this.c1pCodePhaseBias = c1pCodePhaseBias;
    }

    /** Get the code phase bias correction for GLONASS {@link ObservationType#C1P} signal.
     * @return code phase bias correction for GLONASS {@link ObservationType#C1P} signal
     * @since 12.0
     */
    public double getC1pCodePhaseBias() {
        return c1pCodePhaseBias;
    }

    /** Set the code phase bias correction for GLONASS {@link ObservationType#C2C} signal.
     * @param c2cCodePhaseBias code phase bias correction for GLONASS {@link ObservationType#C2C} signal
     * @since 12.0
     */
    public void setC2cCodePhaseBias(final double c2cCodePhaseBias) {
        this.c2cCodePhaseBias = c2cCodePhaseBias;
    }

    /** Get the code phase bias correction for GLONASS {@link ObservationType#C2C} signal.
     * @return code phase bias correction for GLONASS {@link ObservationType#C2C} signal
     * @since 12.0
     */
    public double getC2cCodePhaseBias() {
        return c2cCodePhaseBias;
    }

    /** Set the code phase bias correction for GLONASS {@link ObservationType#C2P} signal.
     * @param c2pCodePhaseBias code phase bias correction for GLONASS {@link ObservationType#C2P} signal
     * @since 12.0
     */
    public void setC2pCodePhaseBias(final double c2pCodePhaseBias) {
        this.c2pCodePhaseBias = c2pCodePhaseBias;
    }

    /** Get the code phase bias correction for GLONASS {@link ObservationType#C2P} signal.
     * @return code phase bias correction for GLONASS {@link ObservationType#C2P} signal
     * @since 12.0
     */
    public double getC2pCodePhaseBias() {
        return c2pCodePhaseBias;
    }

}

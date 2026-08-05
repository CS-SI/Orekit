/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.files.sinex.orbex;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.sinex.ParseInfo;
import org.orekit.frames.Frame;
import org.orekit.gnss.SatInSystem;
import org.orekit.gnss.TimeSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScales;
import org.orekit.time.clocks.ClockOffset;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.units.Unit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Parse information for Orbit Exchange Format (ORBEX) files.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class OrbexParseInfo extends ParseInfo<Orbex> {

    /** Mapping from frame identifier in the file to a {@link Frame}. */
    private final Function<? super String, ? extends Frame> frameBuilder;

    /** Mapper from string to time system. */
    private final Function<? super String, ? extends TimeSystem> timeSystemBuilder;

    /** Completed ephemeris data. */
    private final Map<SatInSystem, Orbex.Data> ephemerisData;

    /** Satellite parsed. */
    private final HashMap<SatInSystem, SatData> parsedSatellites;

    /** Description of the file content. */
    private String description;

    /** Name of agency which created the file. */
    private String createdBy;

    /** Input used to generate this file. */
    private String inputData;

    /** E-mail address of the relevant contact person. */
    private String contact;

    /** Time system. */
    private TimeSystem timeSystem;

    /** Number of seconds between each epoch (NaN if irregular). */
    private double epochInterval;

    /** Reference frame. */
    private Frame coordinateSystem;

    /** Frame type. */
    private String frameType;

    /** Orbit type. */
    private String orbitType;

    /** Record types. */
    private List<EphemerisDataPredicate> recordTypes;

    /** Orbit reference. */
    private String orbitReference;

    /** Orbit position unit. */
    private Unit positionUnit;

    /** Orbit velocity unit. */
    private Unit velocityUnit;

    /** Clock correction unit. */
    private Unit clockCorrectionUnit;

    /** Clock rate unit. */
    private Unit clockRateUnit;

    /** Current date. */
    private AbsoluteDate date;

    /** Expected number of satellites for this time tag. */
    private int expectedSatellites;

    /** Simple constructor.
     * @param frameBuilder      is a function that can construct a frame from an orbex file
     *                          coordinate system string. The coordinate system can be
     *                          any 5 characters string e.g., ITR92, IGb08.
     * @param timeSystemBuilder mapper from string to time system (useful for user-defined time systems)
     * @param timeScales        the set of time scales used for parsing dates
     */
    OrbexParseInfo(final Function<? super String, ? extends Frame> frameBuilder,
                   final Function<? super String, ? extends TimeSystem> timeSystemBuilder,
                   final TimeScales timeScales) {
        super(timeScales);
        this.frameBuilder      = frameBuilder;
        this.timeSystemBuilder = timeSystemBuilder;
        this.ephemerisData     = new HashMap<>();
        this.parsedSatellites  = new HashMap<>();
    }

    /** {@inheritDoc} */
    @Override
    protected Orbex build() {

        // close last parsed group
        timeTag(DateTimeComponents.JULIAN_EPOCH, 0);

        return new Orbex(getTimeScales(), getCreationDate(), getStartDate(), getEndDate(), ephemerisData);

    }

    /** Set the description of the file content.
     * @param description description of the file content
     */
    void setDescription(final String description) {
        this.description = description;
    }

    /** Set the name of agency which created the file.
     * @param createdBy name of agency which created the file*/
    void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    /** Set the input used to generate this file.
     * @param inputData input used to generate this file
     */
    void setInputData(final String inputData) {
        this.inputData = inputData;
    }

    /** Set the E-mail address of the relevant contact person.
     * @param contact E-mail address of the relevant contact person
     */
    void setContact(final String contact) {
        this.contact = contact;
    }

    /** Set the time system.
     * @param timeSystem time system
     */
    void setTimeSystem(final String timeSystem) {
        this.timeSystem = timeSystemBuilder.apply(timeSystem);
        setTimeScale(this.timeSystem.getTimeScale(getTimeScales()));
    }

    /** Set the number of seconds between each epoch.
     * @param epochInterval number of seconds between each epoch (NaN if irregular)
     */
    void setEpochInterval(final double epochInterval) {
        this.epochInterval = epochInterval;
    }

    /** Set the name of reference frame.
     * @param coordinateSystem name of reference frame
     */
    void setCoordinateSystem(final String coordinateSystem) {
        this.coordinateSystem = frameBuilder.apply(coordinateSystem);
    }

    /** Set the frame type.
     * @param frameType frame type
     */
    void setFrameType(final String frameType) {
        this.frameType = frameType;
    }

    /** Set the orbit type.
     * @param orbitType orbit type
     */
    void setOrbitType(final String orbitType) {
        this.orbitType = orbitType;
    }

    /** Set the record types.
     * @param recordTypes recordTypes
     */
    void setRecordTypes(final List<EphemerisDataPredicate> recordTypes) {
        this.recordTypes = recordTypes;
    }

    /** Set the orbit reference.
     * @param orbitReference orbit reference
     */
    void setOrbitReference(final String orbitReference) {
        this.orbitReference = orbitReference;
    }

    /** Set the unit for position.
     * @param positionUnit unit for position
     */
    void setPositionUnit(final Unit positionUnit) {
        this.positionUnit = positionUnit;
    }

    /** Set the unit for velocity.
     * @param velocityUnit unit for velocity
     */
    void setVelocityUnit(final Unit velocityUnit) {
        this.velocityUnit = velocityUnit;
    }

    /** Set the unit for clock correction.
     * @param clockCorrectionUnit unit for clock correction
     */
    void setClockCorrectionUnit(final Unit clockCorrectionUnit) {
        this.clockCorrectionUnit = clockCorrectionUnit;
    }

    /** Set the unit for clock rate.
     * @param clockRateUnit unit for clock rate
     */
    void setClockRateUnit(final Unit clockRateUnit) {
        this.clockRateUnit = clockRateUnit;
    }

    /** Add a satellite id and description.
     * @param satId       satellite id
     * @param description satellite description
     */
    void addSatIdAndDescription(final SatInSystem satId, final String description) {
        if (ephemerisData.containsKey(satId)) {
            throw new OrekitException(OrekitMessages.DUPLICATED_SATELLITE,
                                      satId, getLineNumber(), getName());
        } else {
            ephemerisData.put(satId, new Orbex.Data(satId, description));
        }
    }

    /** Close a time tag.
     */
    private void closeTimeTag() {

        // check previous time tag was properly completed
        if (parsedSatellites.size() != expectedSatellites) {
            throw new OrekitException(OrekitMessages.INCOMPLETE_ORBEX_DATA,
                                      expectedSatellites, date.toString(getTimeScale()), parsedSatellites.size(),
                                      getLineNumber(), getName());
        }

        // store coordinates for current time tag
        for (final Map.Entry<SatInSystem, SatData> entry : parsedSatellites.entrySet()) {

            final Orbex.Data orbexData = ephemerisData.get(entry.getKey());

            // check the satellite was properly declared in the SATELLITE/ID_AND_DESCRIPTION block
            if (orbexData == null) {
                throw new OrekitException(OrekitMessages.INVALID_SATELLITE_ID, entry.getKey());
            }

            final SatData satData = entry.getValue();

            // orbit
            if (satData.position != null) {
                final TimeStampedPVCoordinates pv =
                    new TimeStampedPVCoordinates(date,
                                                 satData.position,
                                                 satData.velocity == null ? Vector3D.ZERO : satData.velocity);
                orbexData.orbit().add(pv);
            }

            // clock
            if (satData.clockCorrection != null) {
                final ClockOffset co =
                    new ClockOffset(date,
                                    satData.clockCorrection,
                                    satData.clockRate == null ? 0.0 : satData.clockRate,
                                    0.0);
                orbexData.clock().add(co);
            }

            // attitude
            if (satData.attitude != null) {
                final TimeStampedAngularCoordinates ac =
                    new TimeStampedAngularCoordinates(date,
                                                      new AngularCoordinates(satData.attitude, Vector3D.ZERO));
                orbexData.attitude().add(ac);
            }

        }

        parsedSatellites.clear();
        expectedSatellites = 0;

    }

    /** Start a new time tag.
     * @param timeTag time tag
     * @param nbSats expected number of satellites
     */
    void timeTag(final DateTimeComponents timeTag, final int nbSats) {

        // close previous time tag
        closeTimeTag();

        // start new time tag
        this.expectedSatellites = nbSats;
        this.date         = new AbsoluteDate(timeTag, getTimeScale());

    }

    /** Add a position.
     * @param satId satellite id
     * @param parsedPosition parsed position (without unit conversion)
     * @param recordType type of record
     */
    void addPosition(final SatInSystem satId, final Vector3D parsedPosition,
                     final String recordType) {
        checkUnit(positionUnit, recordType);
        parsedSatellites.computeIfAbsent(satId, k -> new SatData()).position =
            new Vector3D(positionUnit.toSI(parsedPosition.getX()),
                         positionUnit.toSI(parsedPosition.getY()),
                         positionUnit.toSI(parsedPosition.getZ()));
    }

    /** Add a velocity.
     * @param satId satellite id
     * @param parsedVelocity parsed velocity (without unit conversion)
     * @param recordType type of record
     */
    void addVelocity(final SatInSystem satId, final Vector3D parsedVelocity,
                     final String recordType) {
        checkUnit(velocityUnit, recordType);
        parsedSatellites.computeIfAbsent(satId, k -> new SatData()).velocity =
            new Vector3D(velocityUnit.toSI(parsedVelocity.getX()),
                         velocityUnit.toSI(parsedVelocity.getY()),
                         velocityUnit.toSI(parsedVelocity.getZ()));
    }

    /** Add a clock correction.
     * @param satId satellite id
     * @param parsedClockCorrection parsed clock correction (without unit conversion)
     * @param recordType type of record
     */
    void addClockCorrection(final SatInSystem satId, final double parsedClockCorrection,
                     final String recordType) {
        checkUnit(clockCorrectionUnit, recordType);
        parsedSatellites.computeIfAbsent(satId, k -> new SatData()).clockCorrection =
            clockCorrectionUnit.toSI(parsedClockCorrection);
    }

    /** Add a clock rate.
     * @param satId satellite id
     * @param parsedClockRate parsed clock rate (without unit conversion)
     * @param recordType type of record
     */
    void addClockRate(final SatInSystem satId, final double parsedClockRate,
                     final String recordType) {
        checkUnit(clockRateUnit, recordType);
        parsedSatellites.computeIfAbsent(satId, k -> new SatData()).clockRate =
            clockRateUnit.toSI(parsedClockRate);
    }

    /** Add an attitude.
     * @param satId satellite id
     * @param attitude attitude
     */
    void addAttitude(final SatInSystem satId, final Rotation attitude) {
        parsedSatellites.computeIfAbsent(satId, k -> new SatData()).attitude = attitude;
    }

    /** Check unit has been initialized.
     * @param unit unit to check
     * @param recordType type of record
     */
    private void checkUnit(final Unit unit, final String recordType) {
        if (unit == null) {
            throw new OrekitException(OrekitMessages.MISSING_ORBEX_UNIT,
                                      recordType, getLineNumber(), getName());
        }
    }

    /** Container for one satellite parsed data. */
    private static class SatData {

        /** Position. */
        private Vector3D position;

        /** Velocity. */
        private Vector3D velocity;

        /** Clock correction. */
        private Double clockCorrection;

        /** Clock rate. */
        private Double clockRate;

        /** Attitude. */
        private Rotation attitude;

    }

}

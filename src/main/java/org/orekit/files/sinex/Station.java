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
package org.orekit.files.sinex;

import java.util.HashMap;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeSpanMap;

/**
 * Station model.
 * <p>
 * Since Orekit 11.1, this class handles multiple site antenna
 * eccentricity.
 * The {@link #getEccentricities(AbsoluteDate)} method can be
 * used to access the site antenna eccentricity values for a
 * given epoch.
 * </p>
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class Station {

    /** Site code. */
    private String siteCode;

    /** DOMES number. */
    private String domes;

    /** Start of validity. */
    private AbsoluteDate validFrom;

    /** End of validity. */
    private AbsoluteDate validUntil;

    /** Eccentricity reference system. */
    private ReferenceSystem eccRefSystem;

    /** TimeSpanMap of site antenna eccentricities. */
    private TimeSpanMap<Vector3D> eccentricitiesTimeSpanMap;

    /** Antenna type.
     * @since 12.0
     */
    private TimeSpanMap<String> antennaTypesMap;

    /** Station position. */
    private Vector3D position;

    /** Station velocity. */
    private Vector3D velocity;

    /** Coordinates reference epoch. */
    private AbsoluteDate epoch;

    /**
     * Constructor.
     */
    public Station() {
        this.eccentricitiesTimeSpanMap = new TimeSpanMap<>(null);
        this.antennaTypesMap           = new TimeSpanMap<>(null);
        this.position                  = Vector3D.ZERO;
        this.velocity                  = Vector3D.ZERO;
    }

    /**
     * Get the site code (station identifier).
     * @return the site code
     */
    public String getSiteCode() {
        return siteCode;
    }

    /**
     * Set the site code (station identifier).
     * @param siteCode the site code to set
     */
    public void setSiteCode(final String siteCode) {
        this.siteCode = siteCode;
    }

    /**
     * Get the site DOMES number.
     * @return the DOMES number
     */
    public String getDomes() {
        return domes;
    }

    /**
     * Set the DOMES number.
     * @param domes the DOMES number to set
     */
    public void setDomes(final String domes) {
        this.domes = domes;
    }

    /**
     * Get start of validity.
     * @return start of validity
     */
    public AbsoluteDate getValidFrom() {
        return validFrom;
    }

    /**
     * Set the start of validity.
     * @param validFrom the start of validity to set
     */
    public void setValidFrom(final AbsoluteDate validFrom) {
        this.validFrom = validFrom;
    }

    /**
     * Get end of validity.
     * @return end of validity
     */
    public AbsoluteDate getValidUntil() {
        return validUntil;
    }

    /**
     * Set the end of validity.
     * @param validUntil the end of validity to set
     */
    public void setValidUntil(final AbsoluteDate validUntil) {
        this.validUntil = validUntil;
    }

    /**
     * Get the reference system used to define the eccentricity vector (local or cartesian).
     * @return the reference system used to define the eccentricity vector
     */
    public ReferenceSystem getEccRefSystem() {
        return eccRefSystem;
    }

    /**
     * Set the reference system used to define the eccentricity vector (local or cartesian).
     * @param eccRefSystem the reference system used to define the eccentricity vector
     */
    public void setEccRefSystem(final ReferenceSystem eccRefSystem) {
        this.eccRefSystem = eccRefSystem;
    }

    /**
     * Get the station antenna eccentricities for the given epoch.
     * <p>
     * Vector convention: X-Y-Z or UP-NORTH-EAST.
     * See {@link #getEccRefSystem()} method.
     * <p>
     * If there is no eccentricity values for the given epoch, an
     * exception is thrown.
     * @param date epoch
     * @return station antenna eccentricities (m)
     * @since 11.1
     */
    public Vector3D getEccentricities(final AbsoluteDate date) {
        final Vector3D eccAtEpoch = eccentricitiesTimeSpanMap.get(date);
        // If the entry is null, there is no valid eccentricity values for the input epoch
        if (eccAtEpoch == null) {
            // Throw an exception
            throw new OrekitException(OrekitMessages.MISSING_STATION_DATA_FOR_EPOCH, date);
        }
        return eccAtEpoch;
    }

    /**
     * Get the TimeSpanMap of site antenna eccentricities.
     * @return the TimeSpanMap of site antenna eccentricities
     * @since 11.1
     */
    public TimeSpanMap<Vector3D> getEccentricitiesTimeSpanMap() {
        return eccentricitiesTimeSpanMap;
    }

    /** Add a station eccentricity vector entry valid before a limit date.<br>
     * Using <code>addStationEccentricitiesValidBefore(entry, t)</code> will make <code>entry</code>
     * valid in ]-∞, t[ (note the open bracket).
     * @param entry station eccentricity vector entry
     * @param latestValidityDate date before which the entry is valid
     * (must be different from <b>all</b> dates already used for transitions)
     * @since 11.1
     */
    public void addStationEccentricitiesValidBefore(final Vector3D entry, final AbsoluteDate latestValidityDate) {
        eccentricitiesTimeSpanMap.addValidBefore(entry, latestValidityDate, false);
    }

    /** Add a station eccentricity vector entry valid after a limit date.<br>
     * Using <code>addStationEccentricitiesValidAfter(entry, t)</code> will make <code>entry</code>
     * valid in [t, +∞[ (note the closed bracket).
     * @param entry station eccentricity vector entry
     * @param earliestValidityDate date after which the entry is valid
     * (must be different from <b>all</b> dates already used for transitions)
     * @since 11.1
     */
    public void addStationEccentricitiesValidAfter(final Vector3D entry, final AbsoluteDate earliestValidityDate) {
        eccentricitiesTimeSpanMap.addValidAfter(entry, earliestValidityDate, false);
    }

    /**
     * Get the antenna type for the given epoch.
     * If there is no antenna types for the given epoch, an
     * exception is thrown.
     * @param date epoch
     * @return antenna type
     * @since 12.0
     */
    public String getAntennaType(final AbsoluteDate date) {
        final String typeAtEpoch = antennaTypesMap.get(date);
        // If the entry is null, there is no valid type for the input epoch
        if (typeAtEpoch == null) {
            // Throw an exception
            throw new OrekitException(OrekitMessages.MISSING_STATION_DATA_FOR_EPOCH, date);
        }
        return typeAtEpoch;
    }

    /**
     * Get the TimeSpanMap of site antenna type.
     * @return the TimeSpanMap of site antenna type
     * @since 12.0
     */
    public TimeSpanMap<String> getAntennaTypeTimeSpanMap() {
        return antennaTypesMap;
    }

    /** Add a antenna type entry valid before a limit date.<br>
     * Using <code>addAntennaTypeValidBefore(entry, t)</code> will make <code>entry</code>
     * valid in ]-∞, t[ (note the open bracket).
     * @param entry antenna type entry
     * @param latestValidityDate date before which the entry is valid
     * (must be different from <b>all</b> dates already used for transitions)
     * @since 12.0
     */
    public void addAntennaTypeValidBefore(final String entry, final AbsoluteDate latestValidityDate) {
        antennaTypesMap.addValidBefore(entry, latestValidityDate, false);
    }

    /** Add a antenna type entry valid after a limit date.<br>
     * Using <code>addAntennaTypeValidAfter(entry, t)</code> will make <code>entry</code>
     * valid in [t, +∞[ (note the closed bracket).
     * @param entry antenna type entry
     * @param earliestValidityDate date after which the entry is valid
     * (must be different from <b>all</b> dates already used for transitions)
     * @since 12.0
     */
    public void addAntennaTypeValidAfter(final String entry, final AbsoluteDate earliestValidityDate) {
        antennaTypesMap.addValidAfter(entry, earliestValidityDate, false);
    }

    /**
     * Get the station position.
     * @return the station position (m)
     */
    public Vector3D getPosition() {
        return position;
    }

    /**
     * Set the station position.
     * @param position the position to set
     */
    public void setPosition(final Vector3D position) {
        this.position = position;
    }

    /**
     * Get the station velocity.
     * @return the station velocity (m/s)
     */
    public Vector3D getVelocity() {
        return velocity;
    }

    /**
     * Set the station velocity.
     * @param velocity the velocity to set
     */
    public void setVelocity(final Vector3D velocity) {
        this.velocity = velocity;
    }

    /**
     * Get the coordinates reference epoch.
     * @return the coordinates reference epoch
     */
    public AbsoluteDate getEpoch() {
        return epoch;
    }

    /**
     * Set the coordinates reference epoch.
     * @param epoch the epoch to set
     */
    public void setEpoch(final AbsoluteDate epoch) {
        this.epoch = epoch;
    }

    /** Eccentricity reference system. */
    public enum ReferenceSystem {

        /** Local reference system Up, North, East. */
        UNE("UNE"),

        /** Cartesian reference system X, Y, Z. */
        XYZ("XYZ");

        /** Codes map. */
        private static final Map<String, ReferenceSystem> CODES_MAP = new HashMap<>();
        static {
            for (final ReferenceSystem type : values()) {
                CODES_MAP.put(type.getName(), type);
            }
        }

        /** Name used to define the reference system in SINEX file. */
        private final String name;

        /**
         * Constructor.
         * @param name name used to define the reference system in SINEX file
         */
        ReferenceSystem(final String name) {
            this.name = name;
        }

        /**
         * Get the name used to define the reference system in SINEX file.
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * Get the eccentricity reference system corresponding to the given value.
         * @param value given value
         * @return the corresponding eccentricity reference system
         */
        public static ReferenceSystem getEccRefSystem(final String value) {
            return CODES_MAP.get(value);
        }

    }

}


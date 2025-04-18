/* Copyright 2002-2025 CS GROUP
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.GnssSignal;
import org.orekit.models.earth.displacement.PsdCorrection;
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
    private final TimeSpanMap<Vector3D> eccentricitiesTimeSpanMap;

    /** Antenna key.
     * @since 13.0
     */
    private final TimeSpanMap<AntennaKey> antennaKeysMap;

    /** Phase centers.
     * @since 13.0
     */
    private final TimeSpanMap<Map<GnssSignal, Vector3D>> phaseCentersMap;

    /** Post-Seismic Deformation.
     * @since 12.0
     */
    private final TimeSpanMap<List<PsdCorrection>> psdMap;

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
        this.antennaKeysMap            = new TimeSpanMap<>(null);
        this.phaseCentersMap           = new TimeSpanMap<>(null);
        this.psdMap                    = new TimeSpanMap<>(null);
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

    /** Get the TimeSpanMap of Post-Seismic Deformation.
     * @return the TimeSpanMap of Post-Seismic Deformation
     * @since 12.1
     */
    public TimeSpanMap<List<PsdCorrection>> getPsdTimeSpanMap() {
        return psdMap;
    }

    /** Add a Post-Seismic Deformation entry valid after a limit date.<br>
     * Using {@code addPsdCorrectionValidAfter(entry, t)} will make {@code entry}
     * valid in [t, +∞[ (note the closed bracket).
     * @param entry Post-Seismic Deformation entry
     * @param earliestValidityDate date after which the entry is valid
     * (must be different from <b>all</b> dates already used for transitions)
     * @since 12.1
     */
    public void addPsdCorrectionValidAfter(final PsdCorrection entry, final AbsoluteDate earliestValidityDate) {

        // get the list of corrections active just after earthquake date
        List<PsdCorrection> corrections = psdMap.get(earliestValidityDate.shiftedBy(1.0e-3));

        if (corrections == null ||
            earliestValidityDate.durationFrom(corrections.get(0).getEarthquakeDate()) > 1.0e-3) {
            // either this is the first earthquake we consider or
            // this earthquake is after another one already considered
            // we need to create a new list of corrections for this new earthquake
            corrections = new ArrayList<>();
            psdMap.addValidAfter(corrections, earliestValidityDate, false);
        }

        // add the entry to the current list
        corrections.add(entry);

    }

    /**
     * Get the antenna key for the given epoch.
     * If there is no antenna keys for the given epoch, an
     * exception is thrown.
     * @param date epoch
     * @return antenna key
     * @since 13.0
     */
    public AntennaKey getAntennaKey(final AbsoluteDate date) {
        final AntennaKey keyAtEpoch = antennaKeysMap.get(date);
        // If the entry is null, there is no valid type for the input epoch
        if (keyAtEpoch == null) {
            // Throw an exception
            throw new OrekitException(OrekitMessages.MISSING_STATION_DATA_FOR_EPOCH, date);
        }
        return keyAtEpoch;
    }

    /**
     * Get the TimeSpanMap of site antenna type.
     * @return the TimeSpanMap of site antenna type
     * @since 12.0
     */
    public TimeSpanMap<AntennaKey> getAntennaKeyTimeSpanMap() {
        return antennaKeysMap;
    }

    /** Add a antenna key entry valid before a limit date.<br>
     * Using <code>addAntennaKeyValidBefore(entry, t)</code> will make <code>entry</code>
     * valid in ]-∞, t[ (note the open bracket).
     * @param entry antenna key entry
     * @param latestValidityDate date before which the entry is valid
     * (must be different from <b>all</b> dates already used for transitions)
     * @since 12.0
     */
    public void addAntennaKeyValidBefore(final AntennaKey entry, final AbsoluteDate latestValidityDate) {
        antennaKeysMap.addValidBefore(entry, latestValidityDate, false);
    }

    /**
     * Get the TimeSpanMap of phase centers.
     * @return the TimeSpanMap of phase centers
     * @since 13.0
     */
    public TimeSpanMap<Map<GnssSignal, Vector3D>> getPhaseCentersMap() {
        return phaseCentersMap;
    }

    /**
     * Get the phase centers for the given epoch.
     * If there is no phase centers for the given epoch, an
     * exception is thrown.
     * @param date epoch
     * @return phase centers
     * @since 13.0
     */
    public Map<GnssSignal, Vector3D> getPhaseCenters(final AbsoluteDate date) {
        final Map<GnssSignal, Vector3D> phaseCentersAtEpoch = phaseCentersMap.get(date);
        // If the entry is null, there is no valid key for the input epoch
        if (phaseCentersAtEpoch == null) {
            // Throw an exception
            throw new OrekitException(OrekitMessages.MISSING_STATION_DATA_FOR_EPOCH, date);
        }
        return phaseCentersAtEpoch;
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


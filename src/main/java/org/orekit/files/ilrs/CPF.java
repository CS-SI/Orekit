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
package org.orekit.files.ilrs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.files.general.EphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * This class stores all the information of the Consolidated laser ranging Prediction File (CPF) parsed
 * by CPFParser. It contains the header and a list of ephemeris entry.
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class CPF implements EphemerisFile<CPF.CPFCoordinate, CPF.CPFEphemeris> {

    /** Default satellite ID, used if header is null when initializing the ephemeris. */
    public static final String DEFAULT_ID = "9999999";

    /** Gravitational coefficient. */
    private double mu;

    /** The interpolation sample. */
    private int interpolationSample;

    /** Time scale of dates in the ephemeris file. */
    private TimeScale timeScale;

    /** Indicates if data contains velocity or not. */
    private CartesianDerivativesFilter filter;

    /** CPF file header. */
    private CPFHeader header;

    /** Map containing satellite information. */
    private Map<String, CPFEphemeris> ephemeris;

    /** List of comments contained in the file. */
    private List<String> comments;

    /**
     * Constructor.
     */
    public CPF() {
        this.mu        = Double.NaN;
        this.ephemeris = new ConcurrentHashMap<>();
        this.header    = new CPFHeader();
        this.comments  = new ArrayList<>();
    }

    /** {@inheritDoc}
     * First key corresponds to String value of {@link CPFHeader#getIlrsSatelliteId()}
     */
    @Override
    public Map<String, CPFEphemeris> getSatellites() {
        // Return the map
        return Collections.unmodifiableMap(ephemeris);
    }

    /**
     * Get the CPF file header.
     * @return the CPF file header
     */
    public CPFHeader getHeader() {
        return header;
    }

    /**
     * Get the time scale used in CPF file.
     * @return the time scale used to parse epochs in CPF file.
     */
    public TimeScale getTimeScale() {
        return timeScale;
    }

    /**
     * Get the comments contained in the file.
     * @return the comments contained in the file
     */
    public List<String> getComments() {
        return comments;
    }

    /**
     * Adds a set of P/V coordinates to the satellite.
     * @param id satellite ILRS identifier
     * @param coord set of coordinates
     * @since 11.0.1
     */
    public void addSatelliteCoordinates(final String id, final List<CPFCoordinate> coord) {
        createIfNeeded(id);
        ephemeris.get(id).coordinates.addAll(coord);
    }

    /**
     * Add a new P/V coordinates to the satellite.
     * @param id satellite ILRS identifier
     * @param coord the P/V coordinate of the satellite
     * @since 11.0.1
     */
    public void addSatelliteCoordinate(final String id, final CPFCoordinate coord) {
        createIfNeeded(id);
        ephemeris.get(id).coordinates.add(coord);
    }

    /**
     * Add the velocity to the last CPF coordinate entry.
     * @param id satellite ILRS identifier
     * @param velocity the velocity vector of the satellite
     * @since 11.2
     */
    public void addSatelliteVelocityToCPFCoordinate(final String id, final Vector3D velocity) {
        // Get the last coordinate entry, which contains the position vector
        final CPFCoordinate lastCoordinate = ephemeris.get(id).coordinates.get(ephemeris.get(id).coordinates.size() - 1);

        // Create a new CPFCoordinate object with both position and velocity information
        final CPFCoordinate CPFCoordUpdated = new CPFCoordinate(lastCoordinate.getDate(),
                lastCoordinate.getPosition(),
                velocity,
                lastCoordinate.getLeap());

        // Patch the last record
        ephemeris.get(id).coordinates.set(ephemeris.get(id).coordinates.size() - 1, CPFCoordUpdated);
    }

    /**
     * Set the interpolation sample.
     * @param interpolationSample interpolation sample
     */
    public void setInterpolationSample(final int interpolationSample) {
        this.interpolationSample = interpolationSample;
    }

    /**
     * Set the gravitational coefficient.
     * @param mu the coefficient to be set
     */
    public void setMu(final double mu) {
        this.mu = mu;
    }

    /**
     * Set the time scale.
     * @param timeScale use to parse dates in this file.
     */
    public void setTimeScale(final TimeScale timeScale) {
        this.timeScale = timeScale;
    }

    /**
     * Set the derivatives filter.
     * @param filter that indicates which derivatives of position are available.
     */
    public void setFilter(final CartesianDerivativesFilter filter) {
        this.filter = filter;
    }

    /**
     * Create the satellite ephemeris corresponding to the given ID (if needed).
     * @param id satellite ILRS identifier
     */
    private void createIfNeeded(final String id) {
        if (ephemeris.get(id) == null) {
            ephemeris.put(id, new CPFEphemeris(id));
        }
    }

    /** An ephemeris entry  for a single satellite contains in a CPF file. */
    public class CPFEphemeris
        implements EphemerisFile.SatelliteEphemeris<CPFCoordinate, CPFEphemeris>,
                   EphemerisFile.EphemerisSegment<CPFCoordinate> {

        /** Satellite ID. */
        private final String id;

        /** Ephemeris Data. */
        private final List<CPFCoordinate> coordinates;

        /**
         * Constructor.
         * @param id satellite ID
         */
        public CPFEphemeris(final String id) {
            this.id          = id;
            this.coordinates = new ArrayList<>();
        }


        /** {@inheritDoc} */
        @Override
        public Frame getFrame() {
            return header.getRefFrame();
        }

        /** {@inheritDoc} */
        @Override
        public int getInterpolationSamples() {
            return interpolationSample;
        }

        /** {@inheritDoc} */
        @Override
        public CartesianDerivativesFilter getAvailableDerivatives() {
            return filter;
        }

        /** {@inheritDoc} */
        @Override
        public List<CPFCoordinate> getCoordinates() {
            return Collections.unmodifiableList(this.coordinates);
        }

        /** {@inheritDoc} */
        @Override
        public String getId() {
            return id == null ? DEFAULT_ID : id;
        }

        /** {@inheritDoc} */
        @Override
        public double getMu() {
            return mu;
        }

        /** Returns a list containing only {@code this}. */
        @Override
        public List<CPFEphemeris> getSegments() {
            return Collections.singletonList(this);
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getStart() {
            return coordinates.get(0).getDate();
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getStop() {
            return coordinates.get(coordinates.size() - 1).getDate();
        }

        /** {@inheritDoc} */
        @Override
        public BoundedPropagator getPropagator() {
            return EphemerisSegment.super.getPropagator();
        }

        /** {@inheritDoc} */
        @Override
        public BoundedPropagator getPropagator(final AttitudeProvider attitudeProvider) {
            return EphemerisSegment.super.getPropagator(attitudeProvider);
        }

        /** Get the list of Ephemerides data lines.
         * @return a reference to the internal list of Ephemerides data lines
         */
        public List<CPFCoordinate> getEphemeridesDataLines() {
            return this.coordinates;
        }

    }

    /** A single record of position and possibility velocity in an SP3 file. */
    public static class CPFCoordinate extends TimeStampedPVCoordinates {

        /** Serializable UID. */
        private static final long serialVersionUID = 20201016L;

        /** Leap second flag. */
        private final int leap;

        /**
         * Constructor with null velocity vector.
         * @param date date of coordinates validity
         * @param position position vector
         * @param leap leap second flag (= 0 or the value of the new leap second)
         */
        public CPFCoordinate(final AbsoluteDate date,
                             final Vector3D position,
                             final int leap) {
            this(date, position, Vector3D.ZERO, leap);
        }

        /**
         * Constructor.
         * @param date date of coordinates validity
         * @param position position vector
         * @param velocity velocity vector
         * @param leap leap second flag (= 0 or the value of the new leap second)
         */
        public CPFCoordinate(final AbsoluteDate date,
                             final Vector3D position,
                             final Vector3D velocity,
                             final int leap) {
            super(date, position, velocity);
            this.leap = leap;
        }

        /**
         * Get the leap second flag (= 0 or the value of the new leap second).
         * @return the leap second flag
         */
        public int getLeap() {
            return leap;
        }

    }

}

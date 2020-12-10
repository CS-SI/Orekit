/* Copyright 2002-2020 CS GROUP
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.files.general.EphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * This class stocks all the information of the Consolidated laser ranging Prediction File (CPF) parsed
 * by CPFParser. It contains the header and a list of ephemeris entry.
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class CPFFile implements EphemerisFile {

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

    /** List containing satellite information. */
    private CPFEphemeris ephemeris;

    /** List of comments contained in the file. */
    private List<String> comments;

    /**
     * Constructor.
     */
    public CPFFile() {
        this.mu        = Double.NaN;
        this.ephemeris = new CPFEphemeris();
        this.header    = new CPFHeader();
        this.comments  = new ArrayList<>();
    }

    /** {@inheritDoc}
     * First key corresponds to String value of {@link CPFHeader#getIlrsSatelliteId()}
     */
    @Override
    public Map<String, CPFEphemeris> getSatellites() {
        // Initialise an empty map
        final Map<String, CPFEphemeris> satellites = new HashMap<>();
        // Add the value
        satellites.put(ephemeris.getId(), ephemeris);
        // Return the map
        return Collections.unmodifiableMap(satellites);
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
     * Adds a new P/V coordinate to the satellite.
     * @param coord the P/V coordinate of the satellite
     */
    public void addSatelliteCoordinate(final CPFCoordinate coord) {
        ephemeris.coordinates.add(coord);
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

    /** An ephemeris entry  for a single satellite contains in a CPF file. */
    public class CPFEphemeris implements SatelliteEphemeris, EphemerisSegment {

        /** Ephemeris Data. */
        private final List<CPFCoordinate> coordinates;

        /** Constructor. */
        public CPFEphemeris() {
            this.coordinates = new ArrayList<>();
        }

        /** {@inheritDoc} */
        @Override
        public String getFrameCenterString() {
            // Unused by CPF files
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public String getFrameString() {
            // Unused by CPF files
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public Frame getFrame() {
            return header.getRefFrame();
        }

        /** {@inheritDoc} */
        @Override
        public String getTimeScaleString() {
            // Unused by CPF files
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public TimeScale getTimeScale() {
            return timeScale;
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
            return header.getIlrsSatelliteId();
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

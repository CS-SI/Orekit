/* Copyright 2016 Applied Defense Solutions (ADS)
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * ADS licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.files.general;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.CelestialBody;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * A class for encapsulating Orekit propagators within an {@link EphemerisFile}
 * complaint object that makes for easy serialization to external ephemeris
 * formats like OEM.
 *
 * @author Hank Grabowski
 * @since 9.0
 *
 */
public class OrekitEphemerisFile
    implements EphemerisFile<TimeStampedPVCoordinates, OrekitEphemerisFile.OrekitEphemerisSegment> {

    /** Hashmap of satellite ephemeris. **/
    private final Map<String, OrekitSatelliteEphemeris> satellites;

    /**
     * Standard default constructor.
     */
    public OrekitEphemerisFile() {
        this.satellites = new ConcurrentHashMap<String, OrekitSatelliteEphemeris>();
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, OrekitSatelliteEphemeris> getSatellites() {
        return Collections.unmodifiableMap(satellites);
    }

    /**
     * Adds a new satellite to this object.
     *
     * @param id
     *            ID to use for this satellite
     * @return the new satellite object
     */
    public OrekitSatelliteEphemeris addSatellite(final String id) {
        final OrekitSatelliteEphemeris newSat = new OrekitSatelliteEphemeris(id);
        this.satellites.put(id, newSat);
        return newSat;
    }

    /**
     * Inner class of {@link OrekitEphemerisFile} that defines the
     * {@link OrekitSatelliteEphemeris} corresponding object for this ephemeris type.
     */
    public static class OrekitSatelliteEphemeris
        implements EphemerisFile.SatelliteEphemeris<TimeStampedPVCoordinates, OrekitEphemerisSegment> {

        /**
         * Defines the default interpolation sample size if it is not specified
         * on a segment.
         **/
        public static final int DEFAULT_INTERPOLATION_SIZE = 2;

        /** ID of the space object encapsulated here. **/
        private final String id;

        /** Earliest date of this file. **/
        private AbsoluteDate startDate;

        /** Latest date of this file. **/
        private AbsoluteDate stopDate;

        /** List of segments in the file. **/
        private final List<OrekitEphemerisSegment> segments;

        /**
         * Standard constructor for building the satellite Ephemeris object.
         *
         * @param id
         *            the ID of the space object for this data
         */
        public OrekitSatelliteEphemeris(final String id) {
            this.id = id;
            this.segments = new ArrayList<>();
        }

        /** {@inheritDoc} */
        @Override
        public String getId() {
            return id;
        }

        /** {@inheritDoc} */
        @Override
        public double getMu() {
            if (this.segments.size() == 0) {
                return 0;
            } else {
                return this.segments.get(0).getMu();
            }
        }

        /** {@inheritDoc} */
        @Override
        public List<OrekitEphemerisSegment> getSegments() {
            return Collections.unmodifiableList(this.segments);
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getStart() {
            return this.startDate;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getStop() {
            return this.stopDate;
        }

        /**
         * Injects pre-computed satellite states into this ephemeris file
         * object, returning the generated {@link OrekitEphemerisSegment} that
         * has been stored internally. Defaults the celestial body to earth and
         * the interpolation size to the default.
         *
         * <p>This method uses the {@link DataContext#getDefault() default data context}.
         *
         * @param states
         *            a list of {@link SpacecraftState} that will comprise this
         *            new unit.
         * @return the generated {@link OrekitEphemerisSegment}
         * @see #addNewSegment(List, CelestialBody, int, TimeScale)
         */
        @DefaultDataContext
        public OrekitEphemerisSegment addNewSegment(final List<SpacecraftState> states) {
            return this.addNewSegment(states, DEFAULT_INTERPOLATION_SIZE);
        }

        /**
         * Injects pre-computed satellite states into this ephemeris file
         * object, returning the generated {@link OrekitEphemerisSegment} that
         * has been stored internally. Defaults the Celestial Body to be Earths
         *
         * <p>This method uses the {@link DataContext#getDefault() default data context}.
         *
         * @param states
         *            a list of {@link SpacecraftState} that will comprise this
         *            new unit.
         * @param interpolationSampleSize
         *            the number of interpolation samples that should be used
         *            when processed by another system
         * @return the generated {@link OrekitEphemerisSegment}
         * @see #addNewSegment(List, CelestialBody, int, TimeScale)
         */
        @DefaultDataContext
        public OrekitEphemerisSegment addNewSegment(final List<SpacecraftState> states,
                final int interpolationSampleSize) {
            return this.addNewSegment(
                    states,
                    DataContext.getDefault().getCelestialBodies().getEarth(),
                    interpolationSampleSize);
        }

        /**
         * Injects pre-computed satellite states into this ephemeris file
         * object, returning the generated {@link OrekitEphemerisSegment} that
         * has been stored internally.
         *
         * <p>This method uses the {@link DataContext#getDefault() default data context}.
         *
         * @param states
         *            a list of {@link SpacecraftState} that will comprise this
         *            new unit.
         * @param body
         *            the celestial body the state's frames are with respect to
         * @param interpolationSampleSize
         *            the number of interpolation samples that should be used
         *            when processed by another system
         * @return the generated {@link OrekitEphemerisSegment}
         * @see #addNewSegment(List, CelestialBody, int, TimeScale)
         */
        @DefaultDataContext
        public OrekitEphemerisSegment addNewSegment(final List<SpacecraftState> states, final CelestialBody body,
                                                    final int interpolationSampleSize) {
            return addNewSegment(states, body, interpolationSampleSize,
                    DataContext.getDefault().getTimeScales().getUTC());
        }

        /**
         * Injects pre-computed satellite states into this ephemeris file
         * object, returning the generated {@link OrekitEphemerisSegment} that
         * has been stored internally.
         *
         * @param states
         *            a list of {@link SpacecraftState} that will comprise this
         *            new unit.
         * @param body
         *            the celestial body from which the frames are defined
         * @param interpolationSampleSize
         *            the number of interpolation samples that should be used
         *            when processed by another system
         * @param timeScale
         *            the time scale used in the new segment.
         * @return the generated {@link OrekitEphemerisSegment}
         * @since 10.1
         */
        public OrekitEphemerisSegment addNewSegment(final List<SpacecraftState> states,
                                                    final CelestialBody body,
                                                    final int interpolationSampleSize,
                                                    final TimeScale timeScale) {
            final int minimumSampleSize = 2;
            if (states == null || states.size() == 0) {
                throw new OrekitIllegalArgumentException(OrekitMessages.NULL_ARGUMENT, "states");
            }

            if (interpolationSampleSize < minimumSampleSize) {
                throw new OrekitIllegalArgumentException(OrekitMessages.NOT_ENOUGH_DATA,
                        interpolationSampleSize);
            }

            final AbsoluteDate start = states.get(0).getDate();
            final AbsoluteDate stop = states.get(states.size() - 1).getDate();

            if (this.startDate == null || start.compareTo(this.startDate) < 0) {
                this.startDate = start;
            }

            if (this.stopDate == null || stop.compareTo(this.stopDate) > 0) {
                this.stopDate = stop;
            }

            final List<TimeStampedPVCoordinates> coordinates = new ArrayList<>();
            for (SpacecraftState state : states) {
                coordinates.add(state.getPVCoordinates());
            }

            final Frame frame = states.get(0).getFrame();

            final OrekitEphemerisSegment newSeg =
                            new OrekitEphemerisSegment(coordinates, frame, body.getGM(), interpolationSampleSize);
            this.segments.add(newSeg);

            return newSeg;
        }
    }

    /** Ephemeris segment. */
    public static class OrekitEphemerisSegment
        implements EphemerisFile.EphemerisSegment<TimeStampedPVCoordinates> {

        /** Coordinates for this ephemeris segment. **/
        private final List<TimeStampedPVCoordinates> coordinates;

        /** The reference frame for this ephemeris segment. **/
        private final Frame frame;

        /** Standard gravitational parameter for the satellite. **/
        private final double mu;

        /** The number of interpolation samples. */
        private final int interpolationSamples;

        /**
         * constructor for OrekitEphemerisSegment.
         *
         * @param coordinates
         *            coordinates making up the ephemeris for this segment
         * @param frame
         *            the frame the coordinates are in
         * @param mu
         *            the gravitational constant used in force model evaluations
         * @param interpolationSamples
         *            the number of samples to use during interpolation
         */
        public OrekitEphemerisSegment(final List<TimeStampedPVCoordinates> coordinates, final Frame frame,
                                      final double mu, final int interpolationSamples) {
            this.coordinates          = coordinates;
            this.frame                = frame;
            this.mu                   = mu;
            this.interpolationSamples = interpolationSamples;
        }

        /** {@inheritDoc} */
        @Override
        public double getMu() {
            return mu;
        }

        /** {@inheritDoc} */
        @Override
        public Frame getFrame() {
            return frame;
        }

        /** {@inheritDoc} */
        @Override
        public Frame getInertialFrame() {
            return frame;
        }

        /** {@inheritDoc} */
        @Override
        public int getInterpolationSamples() {
            return interpolationSamples;
        }

        /** {@inheritDoc} */
        @Override
        public CartesianDerivativesFilter getAvailableDerivatives() {
            return CartesianDerivativesFilter.USE_PV;
        }

        /** {@inheritDoc} */
        @Override
        public List<TimeStampedPVCoordinates> getCoordinates() {
            return Collections.unmodifiableList(coordinates);
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

    }
}

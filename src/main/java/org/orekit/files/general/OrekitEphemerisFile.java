/* Copyright 2016 Applied Defense Solutions (ADS)
 * Licensed to CS Group (CS) under one or more
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
public class OrekitEphemerisFile implements EphemerisFile {

    /** Hashmap of satellite ephemeris. **/
    private final Map<String, OrekitSatelliteEphemeris> satellites;

    /**
     * Standard default constructor.
     */
    public OrekitEphemerisFile() {
        this.satellites = new ConcurrentHashMap<String, OrekitSatelliteEphemeris>();
    }

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
     *
     */
    public static class OrekitSatelliteEphemeris implements SatelliteEphemeris {
        /**
         * Defines the default interpolation sample size if it is not specified
         * on a segment.
         **/
        public static final int DEFAULT_INTERPOLATION_SIZE = 7;

        /** ID of the space object encapsulated here. **/
        private final String id;

        /** Earlist date of this file. **/
        private AbsoluteDate startDate;

        /** Latest date of this file. **/
        private AbsoluteDate stopDate;

        /** List of segements in the file. **/
        private final List<OrekitEphemerisSegment> segments;

        /**
         * Standard constructor for building the satellite Ephemeris object.
         *
         * @param id
         *            the ID of the space object for this data
         */
        public OrekitSatelliteEphemeris(final String id) {
            this.id = id;
            this.segments = new ArrayList<OrekitEphemerisSegment>();
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public double getMu() {
            if (this.segments.size() == 0) {
                return 0;
            } else {
                return this.segments.get(0).getMu();
            }
        }

        @Override
        public List<? extends EphemerisSegment> getSegments() {
            return Collections.unmodifiableList(this.segments);
        }

        @Override
        public AbsoluteDate getStart() {
            return this.startDate;
        }

        @Override
        public AbsoluteDate getStop() {
            // TODO Auto-generated method stub
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
         *            the celestial body the state's frames are with respect to
         * @param interpolationSampleSize
         *            the number of interpolation samples that should be used
         *            when processed by another system
         * @param timeScale
         *            used in the new segment.
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
                throw new OrekitIllegalArgumentException(OrekitMessages.NOT_ENOUGH_DATA_FOR_INTERPOLATION,
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

            final List<TimeStampedPVCoordinates> coordinates = new ArrayList<TimeStampedPVCoordinates>();
            for (SpacecraftState state : states) {
                coordinates.add(state.getPVCoordinates());
            }

            final Frame frame = states.get(0).getFrame();

            final OrekitEphemerisSegment newSeg = new OrekitEphemerisSegment(coordinates, frame, body.getName(),
                    body.getGM(), timeScale, interpolationSampleSize);
            this.segments.add(newSeg);

            return newSeg;
        }
    }

    public static class OrekitEphemerisSegment implements EphemerisSegment {
        /** **/
        private final List<TimeStampedPVCoordinates> coordinates;

        /** **/
        private final Frame frame;

        /** **/
        private final String frameCenterString;

        /** **/
        private final double mu;

        /** **/
        private final String timeScaleString;

        /** **/
        private final TimeScale timeScale;

        /** **/
        private final int samples;

        /**
         * constructor for OrekitEphemerisSegment.
         *
         * @param coordinates
         *            coordinates making up the ephemeris for this segment
         * @param frame
         *            the frame the coordinates are in
         * @param frameCenterString
         *            the name of celestial body the frame is attached to
         * @param mu
         *            the gravitional constant used in force model evaluations
         * @param timeScale
         *            the time scale of these ephemeris points
         * @param samples
         *            the number of samples to use during interpolation
         */
        public OrekitEphemerisSegment(final List<TimeStampedPVCoordinates> coordinates, final Frame frame,
                final String frameCenterString, final double mu, final TimeScale timeScale, final int samples) {
            super();
            this.coordinates = coordinates;
            this.frame = frame;
            this.frameCenterString = frameCenterString;
            this.mu = mu;
            this.timeScale = timeScale;
            this.timeScaleString = timeScale.getName();
            this.samples = samples;
        }

        @Override
        public double getMu() {
            return mu;
        }

        @Override
        public String getFrameCenterString() {
            return frameCenterString;
        }

        @Override
        public String getFrameString() {
            return frame.getName();
        }

        @Override
        public Frame getFrame() {
            return frame;
        }

        @Override
        public Frame getInertialFrame() {
            return frame;
        }

        @Override
        public String getTimeScaleString() {
            return timeScaleString;
        }

        @Override
        public TimeScale getTimeScale() {
            return timeScale;
        }

        @Override
        public int getInterpolationSamples() {
            return samples;
        }

        @Override
        public CartesianDerivativesFilter getAvailableDerivatives() {
            return CartesianDerivativesFilter.USE_PV;
        }

        @Override
        public List<TimeStampedPVCoordinates> getCoordinates() {
            return Collections.unmodifiableList(coordinates);
        }

        @Override
        public AbsoluteDate getStart() {
            return coordinates.get(0).getDate();
        }

        @Override
        public AbsoluteDate getStop() {
            return coordinates.get(coordinates.size() - 1).getDate();
        }

    }
}

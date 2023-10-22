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
package org.orekit.files.general;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.orekit.attitudes.BoundedAttitudeProvider;
import org.orekit.attitudes.FixedFrameBuilder;
import org.orekit.attitudes.TabulatedProvider;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.TimeStampedAngularCoordinates;

/**
 * A class for encapsulating Orekit propagators within an {@link AttitudeEphemerisFile}
 * complaint object that makes for easy serialization to external ephemeris
 * formats like AEM.
 *
 * @author Raphaël Fermé
 * @since 10.3
 *
 */
public class OrekitAttitudeEphemerisFile
    implements AttitudeEphemerisFile<TimeStampedAngularCoordinates,
                                     OrekitAttitudeEphemerisFile.OrekitAttitudeEphemerisSegment> {

    /** Hashmap of satellite ephemeris. **/
    private final Map<String, OrekitSatelliteAttitudeEphemeris> satellites;

    /**
     * Standard default constructor.
     */
    public OrekitAttitudeEphemerisFile() {
        this.satellites = new ConcurrentHashMap<>();
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, OrekitSatelliteAttitudeEphemeris> getSatellites() {
        return Collections.unmodifiableMap(satellites);
    }

    /**
     * Adds a new satellite to this object.
     *
     * @param id
     *            ID to use for this satellite
     * @return the new satellite object
     */
    public OrekitSatelliteAttitudeEphemeris addSatellite(final String id) {
        final OrekitSatelliteAttitudeEphemeris newSat = new OrekitSatelliteAttitudeEphemeris(id);
        this.satellites.put(id, newSat);
        return newSat;
    }

    /**
     * Inner class of {@link OrekitAttitudeEphemerisFile} that defines the
     * {@link OrekitSatelliteAttitudeEphemeris} corresponding object for this ephemeris type.
     *
     */
    public static class OrekitSatelliteAttitudeEphemeris
        implements SatelliteAttitudeEphemeris<TimeStampedAngularCoordinates,
                                              OrekitAttitudeEphemerisFile.OrekitAttitudeEphemerisSegment> {

        /** Default interpolation sample size if it is not specified. **/
        public static final String DEFAULT_INTERPOLATION_METHOD = "LINEAR";

        /** Default interpolation sample size if it is not specified. **/
        public static final int DEFAULT_INTERPOLATION_SIZE = 2;

        /** ID of the space object encapsulated here. **/
        private final String id;

        /** Earliest date of this file. **/
        private AbsoluteDate startDate;

        /** Latest date of this file. **/
        private AbsoluteDate stopDate;

        /** List of segments in the file. **/
        private final List<OrekitAttitudeEphemerisSegment> segments;

        /**
         * Standard constructor for building the satellite Ephemeris object.
         *
         * @param id
         *            the ID of the space object for this data
         */
        public OrekitSatelliteAttitudeEphemeris(final String id) {
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
        public List<OrekitAttitudeEphemerisSegment> getSegments() {
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
         * Injects pre-computed satellite states into this attitude ephemeris file
         * object, returning the generated {@link OrekitAttitudeEphemerisSegment} that
         * has been stored internally.
         *
         * @param states
         *          a list of {@link SpacecraftState} that will comprise this
         *          new unit
         * @param interpolationMethod
         *          the interpolation method that should be used when processed
         *          by another system
         * @param interpolationSamples
         *          the number of interpolation samples that should be used
         *          when processed by another system
         * @param availableDerivatives derivatives to use for interpolation
         * @return the generated {@link OrekitAttitudeEphemerisSegment}
         */
        public OrekitAttitudeEphemerisSegment addNewSegment(final List<SpacecraftState> states,
                                                            final String interpolationMethod,
                                                            final int interpolationSamples,
                                                            final AngularDerivativesFilter availableDerivatives) {
            final int minimumSampleSize = 2;
            if (states == null || states.size() == 0) {
                throw new OrekitIllegalArgumentException(OrekitMessages.NULL_ARGUMENT, "states");
            }

            if (interpolationSamples < minimumSampleSize) {
                throw new OrekitIllegalArgumentException(OrekitMessages.NOT_ENOUGH_DATA,
                        interpolationSamples);
            }

            final AbsoluteDate start = states.get(0).getDate();
            final AbsoluteDate stop = states.get(states.size() - 1).getDate();

            if (this.startDate == null || start.compareTo(this.startDate) < 0) {
                this.startDate = start;
            }

            if (this.stopDate == null || stop.compareTo(this.stopDate) > 0) {
                this.stopDate = stop;
            }

            final List<TimeStampedAngularCoordinates> attitudeDataLines = new ArrayList<>();
            for (SpacecraftState state : states) {
                attitudeDataLines.add(state.getAttitude().getOrientation());
            }

            final OrekitAttitudeEphemerisSegment newSeg =
                            new OrekitAttitudeEphemerisSegment(attitudeDataLines, interpolationMethod, interpolationSamples,
                                                               states.get(0).getFrame(), availableDerivatives);
            this.segments.add(newSeg);
            return newSeg;
        }
    }

    /** Ephemeris segment. */
    public static class OrekitAttitudeEphemerisSegment
        implements AttitudeEphemerisFile.AttitudeEphemerisSegment<TimeStampedAngularCoordinates> {

        /** List of attitude data lines. */
        private List<TimeStampedAngularCoordinates> attitudeDataLines;

        /** The interpolation method to be used. */
        private String interpolationMethod;

        /** The number of interpolation samples. */
        private int interpolationSamples;

        /** Enumerate for selecting which derivatives to use in {@link #attitudeDataLines} interpolation. */
        private AngularDerivativesFilter availableDerivatives;

        /** Reference frame from which attitude is defined. */
        private Frame referenceFrame;

        /**
         * Constructor for OrekitAttitudeEphemerisSegment.
         *
         * @param attitudeDataLines
         *          attitude data lines for this segment.
         * @param interpolationMethod
         *          the interpolation method to use.
         * @param interpolationSamples
         *          the number of samples to use during interpolation.
         * @param referenceFrame
         *          reference frame from which the attitude is defined
         * @param availableDerivatives derivatives to use for interpolation
         */
        public OrekitAttitudeEphemerisSegment(final List<TimeStampedAngularCoordinates> attitudeDataLines,
                                              final String interpolationMethod,
                                              final int interpolationSamples,
                                              final Frame referenceFrame,
                                              final AngularDerivativesFilter availableDerivatives) {
            this.attitudeDataLines    = attitudeDataLines;
            this.interpolationMethod  = interpolationMethod;
            this.interpolationSamples = interpolationSamples;
            this.referenceFrame       = referenceFrame;
            this.availableDerivatives = availableDerivatives;
        }

        /** {@inheritDoc} */
        @Override
        public List<TimeStampedAngularCoordinates> getAngularCoordinates() {
            return Collections.unmodifiableList(attitudeDataLines);
        }

        /** {@inheritDoc} */
        @Override
        public Frame getReferenceFrame() {
            return referenceFrame;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getStart() {
            return attitudeDataLines.get(0).getDate();
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getStop() {
            return attitudeDataLines.get(attitudeDataLines.size() - 1).getDate();
        }

        /** {@inheritDoc} */
        @Override
        public String getInterpolationMethod() {
            return interpolationMethod;
        }

        /** {@inheritDoc} */
        @Override
        public int getInterpolationSamples() {
            return interpolationSamples;
        }

        /** {@inheritDoc} */
        @Override
        public AngularDerivativesFilter getAvailableDerivatives() {
            return availableDerivatives;
        }

        /** {@inheritDoc} */
        @Override
        public BoundedAttitudeProvider getAttitudeProvider() {
            return new TabulatedProvider(getAngularCoordinates(),
                                         getInterpolationSamples(), getAvailableDerivatives(),
                                         getStart(), getStop(),
                                         new FixedFrameBuilder(getReferenceFrame()));
        }

    }

}

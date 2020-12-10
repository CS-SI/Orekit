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
package org.orekit.files.general;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.CelestialBody;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.AEMAttitudeType;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
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
public class OrekitAttitudeEphemerisFile implements AttitudeEphemerisFile {

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
    public static class OrekitSatelliteAttitudeEphemeris implements SatelliteAttitudeEphemeris {

        /** Default interpolation sample size if it is not specified. **/
        public static final String DEFAULT_INTERPOLATION_METHOD = "LINEAR";

        /** Default interpolation sample size if it is not specified. **/
        public static final int DEFAULT_INTERPOLATION_SIZE = 2;

        /** Default quaternion order if it is not specified. **/
        public static final String DEFAULT_ATTITUDE_TYPE = "QUATERNION";

        /** Default quaternion order if it is not specified. **/
        public static final boolean DEFAULT_IS_FIRST = false;

        /** Default rotation order if it is not specified. **/
        public static final RotationOrder DEFAULT_ROTATION_ORDER = RotationOrder.ZYX;

        /** Default reference frame A name if it is not specified. **/
        public static final String DEFAULT_REF_FRAME_A = "EME2000";

        /** Default reference frame B name if it is not specified. **/
        public static final String DEFAULT_REF_FRAME_B = "SC_BODY_1";

        /** Default attitude rotation direction if it is not specified. **/
        public static final String DEFAULT_ATTITUDE_DIR = "A2B";

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
            this.segments = new ArrayList<OrekitAttitudeEphemerisSegment>();
        }

        /** {@inheritDoc} */
        @Override
        public String getId() {
            return id;
        }

        /** {@inheritDoc} */
        @Override
        public List<? extends AttitudeEphemerisSegment> getSegments() {
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
         * Injects pre-computed satellite states into this attitude ephemeris file object,
         * returning the generated {@link OrekitAttitudeEphemerisSegment} that has been stored
         * internally. Defaults the celestial body to earth, time scale to UTC, the interpolation
         * size and method, the attitude type, the reference frames to default values.
         *
         * <p>This method uses the {@link DataContext#getDefault() default data context}.
         *
         * @param states
         *            a list of {@link SpacecraftState} that will comprise this
         *            new unit.
         * @return the generated {@link OrekitAttitudeEphemerisSegment}
         */
        @DefaultDataContext
        public OrekitAttitudeEphemerisSegment addNewSegment(final List<SpacecraftState> states) {
            return this.addNewSegment(states, DEFAULT_INTERPOLATION_METHOD, DEFAULT_INTERPOLATION_SIZE);
        }

        /**
         * Injects pre-computed satellite states into this attitude ephemeris file object,
         * returning the generated {@link OrekitAttitudeEphemerisSegment} that has been stored
         * internally. Defaults the celestial body to earth, time scale to UTC, the attitude type,
         * the reference frames to default values.
         *
         * <p>This method uses the {@link DataContext#getDefault() default data context}.
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
         * @return the generated {@link OrekitAttitudeEphemerisSegment}
         */
        @DefaultDataContext
        public OrekitAttitudeEphemerisSegment addNewSegment(final List<SpacecraftState> states,
                                                final String interpolationMethod, final int interpolationSamples) {
            return this.addNewSegment(states, interpolationMethod, interpolationSamples, DEFAULT_ATTITUDE_TYPE,
                                      DEFAULT_IS_FIRST, DEFAULT_ROTATION_ORDER);
        }

        /**
         * Injects pre-computed satellite states into this attitude ephemeris file object,
         * returning the generated {@link OrekitAttitudeEphemerisSegment} that has been stored
         * internally. Defaults the celestial body to earth, time scale to UTC, the reference
         * frames to default values.
         *
         * <p>This method uses the {@link DataContext#getDefault() default data context}.
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
         * @param attitudeType
         *          type of attitude for the attitude ephemeris segment. Must correspond
         *          to the names in {@link AEMAttitudeType} enumerate.
         * @param isFirst
         *          flag for placement of the scalar part of the quaternion
         *          (used if quaternions are chosen in the attitude type)
         * @param rotationOrder
         *          the rotation order for Euler angles (used if Euler angles
         *          are chosen in the attitude type)
         * @return the generated {@link OrekitAttitudeEphemerisSegment}
         */
        @DefaultDataContext
        public OrekitAttitudeEphemerisSegment addNewSegment(final List<SpacecraftState> states, final String interpolationMethod,
                                                            final int interpolationSamples, final String attitudeType,
                                                            final boolean isFirst, final RotationOrder rotationOrder) {
            return this.addNewSegment(states, interpolationMethod, interpolationSamples, attitudeType,
                                      isFirst, rotationOrder, DEFAULT_REF_FRAME_A, DEFAULT_REF_FRAME_B,
                                      DEFAULT_ATTITUDE_DIR);
        }

        /**
         * Injects pre-computed satellite states into this attitude ephemeris file object,
         * returning the generated {@link OrekitAttitudeEphemerisSegment} that has been stored
         * internally. Defaults the celestial body to earth, time scale to UTC.
         *
         * <p>This method uses the {@link DataContext#getDefault() default data context}.
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
         * @param attitudeType
         *          type of attitude for the attitude ephemeris segment. Must correspond
         *          to the names in {@link AEMAttitudeType} enumerate.
         * @param isFirst
         *          flag for placement of the scalar part of the quaternion
         *          (used if quaternions are chosen in the attitude type)
         * @param rotationOrder
         *          the rotation order for Euler angles (used if Euler angles
         *          are chosen in the attitude type)
         * @param refFrameA
         *          name of reference frame A
         * @param refFrameB
         *          name of reference frame B
         * @param attitudeDir
         *          rotation direction of the attitude: "A2B" or "B2A"
         * @return the generated {@link OrekitAttitudeEphemerisSegment}
         */
        @DefaultDataContext
        public OrekitAttitudeEphemerisSegment addNewSegment(final List<SpacecraftState> states, final String interpolationMethod,
                                                            final int interpolationSamples, final String attitudeType,
                                                            final boolean isFirst, final RotationOrder rotationOrder,
                                                            final String refFrameA, final String refFrameB,
                                                            final String attitudeDir) {
            return this.addNewSegment(states, interpolationMethod, interpolationSamples, attitudeType,
                                      isFirst, rotationOrder, refFrameA, refFrameB,
                                      attitudeDir, DataContext.getDefault().getCelestialBodies().getEarth());
        }

        /**
         * Injects pre-computed satellite states into this attitude ephemeris file object,
         * returning the generated {@link OrekitAttitudeEphemerisSegment} that has been stored
         * internally. Defaults the time scale to UTC.
         *
         * <p>This method uses the {@link DataContext#getDefault() default data context}.
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
         * @param attitudeType
         *          type of attitude for the attitude ephemeris segment. Must correspond
         *          to the names in {@link AEMAttitudeType} enumerate.
         * @param isFirst
         *          flag for placement of the scalar part of the quaternion
         *          (used if quaternions are chosen in the attitude type)
         * @param rotationOrder
         *          the rotation order for Euler angles (used if Euler angles
         *          are chosen in the attitude type)
         * @param refFrameA
         *          name of reference frame A
         * @param refFrameB
         *          name of reference frame B
         * @param attitudeDir
         *          rotation direction of the attitude: "A2B" or "B2A"
         * @param body
         *          the celestial body from which the frames are defined
         * @return the generated {@link OrekitAttitudeEphemerisSegment}
         */
        @DefaultDataContext
        public OrekitAttitudeEphemerisSegment addNewSegment(final List<SpacecraftState> states, final String interpolationMethod,
                                                            final int interpolationSamples, final String attitudeType,
                                                            final boolean isFirst, final RotationOrder rotationOrder,
                                                            final String refFrameA, final String refFrameB,
                                                            final String attitudeDir, final CelestialBody body) {
            return this.addNewSegment(states, interpolationMethod, interpolationSamples, attitudeType,
                                      isFirst, rotationOrder, refFrameA, refFrameB, attitudeDir, body,
                                      DataContext.getDefault().getTimeScales().getUTC());
        }

        /**
         * Injects pre-computed satellite states into this attitude ephemeris file
         * object, returning the generated {@link OrekitAttitudeEphemerisSegment} that
         * has been stored internally.
         *
         * @param states
         *          a list of {@link SpacecraftState} that will comprise this
         *          new unit
         * @param body
         *          the celestial body from which the frames are defined
         * @param refFrameA
         *          name of reference frame A
         * @param refFrameB
         *          name of reference frame B
         * @param attitudeDir
         *          rotation direction of the attitude: "A2B" or "B2A"
         * @param attitudeType
         *          type of attitude for the attitude ephemeris segment.
         * @param isFirst
         *          flag for placement of the scalar part of the quaternion
         *          (used if quaternions are chosen in the attitude type)
         * @param rotationOrder
         *          the rotation order for Euler angles (used if Euler angles
         *          are chosen in the attitude type)
         * @param timeScale
         *          the time scale used in the new segment.
         * @param interpolationMethod
         *          the interpolation method that should be used when processed
         *          by another system
         * @param interpolationSamples
         *          the number of interpolation samples that should be used
         *          when processed by another system
         * @return the generated {@link OrekitAttitudeEphemerisSegment}
         */
        public OrekitAttitudeEphemerisSegment addNewSegment(final List<SpacecraftState> states, final String interpolationMethod,
                                                            final int interpolationSamples, final String attitudeType,
                                                            final boolean isFirst, final RotationOrder rotationOrder,
                                                            final String refFrameA, final String refFrameB,
                                                            final String attitudeDir, final CelestialBody body,
                                                            final TimeScale timeScale) {
            final int minimumSampleSize = 2;
            if (states == null || states.size() == 0) {
                throw new OrekitIllegalArgumentException(OrekitMessages.NULL_ARGUMENT, "states");
            }

            if (interpolationSamples < minimumSampleSize) {
                throw new OrekitIllegalArgumentException(OrekitMessages.NOT_ENOUGH_DATA_FOR_INTERPOLATION,
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

            final List<TimeStampedAngularCoordinates> attitudeDataLines = new ArrayList<TimeStampedAngularCoordinates>();
            for (SpacecraftState state : states) {
                attitudeDataLines.add(state.getAttitude().getOrientation());
            }

            final OrekitAttitudeEphemerisSegment newSeg = new OrekitAttitudeEphemerisSegment(attitudeDataLines, body.getName(), refFrameA,
                                                                        refFrameB, attitudeDir, attitudeType, isFirst, rotationOrder,
                                                                        timeScale, interpolationMethod, interpolationSamples, states.get(0).getFrame());
            this.segments.add(newSeg);
            return newSeg;
        }
    }

    public static class OrekitAttitudeEphemerisSegment implements AttitudeEphemerisSegment {

        /** List of attitude data lines. */
        private List<TimeStampedAngularCoordinates> attitudeDataLines;

        /** The name of the frame center. **/
        private final String frameCenterString;

        /** The reference frame A specifier, as it appeared in the file. */
        private String refFrameAString;

        /** The reference frame B specifier, as it appeared in the file. */
        private String refFrameBString;

        /** Rotation direction of the attitude. */
        private String attitudeDir;

        /** The format of the data lines in the message. */
        private String attitudeType;

        /** The placement of the scalar portion of the quaternion (QC) in the attitude data. */
        private boolean isFirst;

        /** The rotation order. */
        private RotationOrder rotationOrder;

        /** The time scale identifier, as specified in the ephemeris file. **/
        private final String timeScaleString;

        /** The time scale for this segment. **/
        private final TimeScale timeScale;

        /** The interpolation method to be used. */
        private String interpolationMethod;

        /** The number of interpolation samples. */
        private int interpolationSamples;

        /** Enumerate for selecting which derivatives to use in {@link #attitudeDataLines} interpolation. */
        private AngularDerivativesFilter angularDerivativesFilter;

        /** Reference frame from which attitude is defined. */
        private Frame referenceFrame;

        /**
         * Constructor for OrekitAttitudeEphemerisSegment.
         *
         * @param attitudeDataLines
         *          attitude data lines for this segment.
         * @param frameCenterString
         *          the name of celestial body the frame is attached to.
         * @param refFrameAString
         *          the name of the reference frame A.
         * @param refFrameBString
         *          the name of the reference frame B.
         * @param attitudeDir
         *          the rotation direction of the attitude.
         * @param attitudeType
         *          the format of the attitude data.
         * @param isFirst
         *          true if QC is the first element in the attitude data.
         * @param rotationOrder
         *          the rotation order for Euler angles.
         * @param timeScale
         *          the time scale of these ephemerides data.
         * @param interpolationMethod
         *          the interpolation method to use.
         * @param interpolationSamples
         *          the number of samples to use during interpolation.
         * @param referenceFrame
         *          reference frame from which the attitude is defined
         */
        public OrekitAttitudeEphemerisSegment(final List<TimeStampedAngularCoordinates> attitudeDataLines, final String frameCenterString,
                final String refFrameAString, final String refFrameBString, final String attitudeDir, final String attitudeType,
                final boolean isFirst, final RotationOrder rotationOrder, final TimeScale timeScale,
                final String interpolationMethod, final int interpolationSamples,
                final Frame referenceFrame) {
            this.attitudeDataLines        = attitudeDataLines;
            this.frameCenterString        = frameCenterString;
            this.refFrameAString          = refFrameAString;
            this.refFrameBString          = refFrameBString;
            this.attitudeDir              = attitudeDir;
            this.attitudeType             = attitudeType;
            this.isFirst                  = isFirst;
            this.rotationOrder            = rotationOrder;
            this.timeScaleString          = timeScale.getName();
            this.timeScale                = timeScale;
            this.interpolationMethod      = interpolationMethod;
            this.interpolationSamples     = interpolationSamples;
            this.referenceFrame           = referenceFrame;
            this.angularDerivativesFilter = AEMAttitudeType.getAttitudeType(attitudeType).getAngularDerivativesFilter();
        }

        /** {@inheritDoc} */
        @Override
        public List<TimeStampedAngularCoordinates> getAngularCoordinates() {
            return Collections.unmodifiableList(attitudeDataLines);
        }

        /** {@inheritDoc} */
        @Override
        public String getFrameCenterString() {
            return frameCenterString;
        }

        /** {@inheritDoc} */
        @Override
        public String getRefFrameAString() {
            return refFrameAString;
        }

        /** {@inheritDoc} */
        @Override
        public String getRefFrameBString() {
            return refFrameBString;
        }

        /** {@inheritDoc} */
        @Override
        public Frame getReferenceFrame() {
            return referenceFrame;
        }

        /** {@inheritDoc} */
        @Override
        public String getAttitudeDirection() {
            return attitudeDir;
        }

        /** {@inheritDoc} */
        @Override
        public String getAttitudeType() {
            return attitudeType;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isFirst() {
            return isFirst;
        }

        /** {@inheritDoc} */
        @Override
        public RotationOrder getRotationOrder() {
            return rotationOrder;
        }

        /** {@inheritDoc} */
        @Override
        public String getTimeScaleString() {
            return timeScaleString;
        }

        /** {@inheritDoc} */
        @Override
        public TimeScale getTimeScale() {
            return timeScale;
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
            return angularDerivativesFilter;
        }

    }

}

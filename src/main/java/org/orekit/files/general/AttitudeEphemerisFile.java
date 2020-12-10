/* Contributed in the public domain.
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
import java.util.List;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.orekit.attitudes.AggregateBoundedAttitudeProvider;
import org.orekit.attitudes.BoundedAttitudeProvider;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.TimeStampedAngularCoordinates;

/**
 * An interface for accessing the data stored in an attitude ephemeris file.
 *
 * <p> An {@link AttitudeEphemerisFile} consists of one or more satellites each with a unique ID
 * within the file. The ephemeris for each satellite consists of one or more segments.
 *
 * <p> Some attitude ephemeris file formats may supply additional information that is not available
 * via this interface. In those cases it is recommended that the parser return a subclass
 * of this interface to provide access to the additional information.
 *
 * @author Raphaël Fermé
 * @see SatelliteAttitudeEphemeris
 * @see AttitudeEphemerisSegment
 * @since 10.3
 */
public interface AttitudeEphemerisFile {

    /**
     * Get the loaded ephemeris for each satellite in the file.
     *
     * @return a map from the satellite's ID to the information about that satellite
     * contained in the file.
     */
    Map<String, ? extends SatelliteAttitudeEphemeris> getSatellites();

    /**
     * Contains the information about a single satellite from an {@link AttitudeEphemerisFile}.
     *
     * <p> A satellite ephemeris consists of one or more {@link AttitudeEphemerisSegment}.
     * Segments are typically used to split up an ephemeris at discontinuous events.
     *
     * @author Raphaël Fermé
     * @see AttitudeEphemerisFile
     * @see AttitudeEphemerisSegment
     * @since 10.3
     */
    interface SatelliteAttitudeEphemeris {

        /**
         * Get the satellite ID. The satellite ID is unique only within the same ephemeris
         * file.
         *
         * @return the satellite's ID, never {@code null}.
         */
        String getId();

        /**
         * Get the segments of the attitude ephemeris.
         *
         * <p> Attitude ephemeris segments are typically used to split an ephemeris around
         * discontinuous events.
         *
         * @return the segments contained in the attitude ephemeris file for this satellite.
         */
        List<? extends AttitudeEphemerisSegment> getSegments();

        /**
         * Get the start date of the ephemeris.
         *
         * @return ephemeris start date.
         */
        AbsoluteDate getStart();

        /**
         * Get the end date of the ephemeris.
         *
         * @return ephemeris end date.
         */
        AbsoluteDate getStop();

        /**
         * Get the attitude provider corresponding to this ephemeris, combining data from all {@link
         * #getSegments() segments}.
         *
         * @return an attitude provider for all the data in this attitude ephemeris file.
         */
        default BoundedAttitudeProvider getAttitudeProvider() {
            final List<BoundedAttitudeProvider> providers = new ArrayList<>();
            for (final AttitudeEphemerisSegment attitudeSegment : this.getSegments()) {
                providers.add(attitudeSegment.getAttitudeProvider());
            }
            return new AggregateBoundedAttitudeProvider(providers);
        }

    }

    /**
     * A segment of an attitude ephemeris for a satellite.
     *
     * <p> Segments are typically used to split an ephemeris around discontinuous events
     * such as maneuvers.
     *
     * @author Raphaël Fermé
     * @see AttitudeEphemerisFile
     * @see SatelliteAttitudeEphemeris
     * @since 10.3
     */
    interface AttitudeEphemerisSegment {

        /**
         * Get an unmodifiable list of attitude data lines.
         *
         * @return a list of attitude data
         */
        List<? extends TimeStampedAngularCoordinates> getAngularCoordinates();

        /**
         * Get the name of the center of the coordinate system the ephemeris is provided
         * in. This may be a natural origin, such as the center of the Earth, another
         * satellite, etc.
         *
         * @return the name of the frame center
         */
        String getFrameCenterString();

        /**
         * Get the reference frame A specifier as it appeared in the file.
         *
         * @return the frame name as it appeared in the file (A).
         */
        String getRefFrameAString();

        /**
         * Get the reference frame B specifier as it appeared in the file.
         *
         * @return the frame name as it appeared in the file (B).
         */
        String getRefFrameBString();

        /**
         * Get the reference frame from which attitude is defined.
         *
         * @return the reference frame from which attitude is defined
         */
        Frame getReferenceFrame();

        /**
         * Get the rotation direction of the attitude.
         *
         * @return the rotation direction of the attitude
         */
        String getAttitudeDirection();

        /**
         * Get the format of the data lines in the message.
         *
         * @return the format of the data lines in the message
         */
        String getAttitudeType();

        /**
         * Get the flag for the placement of the quaternion QC in the attitude data.
         *
         * @return true if QC is the first element in the attitude data
         */
        boolean isFirst();

        /**
         * Get the rotation order for Euler angles.
         *
         * @return rotation order
         */
        RotationOrder getRotationOrder();

        /**
         * Get the time scale for this ephemeris segment.
         *
         * @return the time scale identifier, as specified in the ephemeris file, or
         * {@code null} if the ephemeris file does not specify a time scale.
         */
        String getTimeScaleString();

        /**
         * Get the time scale for this ephemeris segment.
         *
         * @return the time scale for this segment. Never {@code null}.
         */
        TimeScale getTimeScale();

        /**
         * Get the start date of this ephemeris segment.
         *
         * @return ephemeris segment start date.
         */
        AbsoluteDate getStart();

        /**
         * Get the end date of this ephemeris segment.
         *
         * @return ephemeris segment end date.
         */
        AbsoluteDate getStop();

        /**
         * Get the interpolation method to be used.
         *
         * @return the interpolation method
         */
        String getInterpolationMethod();

        /**
         * Get the number of samples to use in interpolation.
         *
         * @return the number of points to use for interpolation.
         */
        int getInterpolationSamples();

        /**
         * Get which derivatives of angular data are available in this attitude ephemeris segment.
         *
         * @return a value indicating if the file contains rotation and/or rotation rate
         *         and/or acceleration data.
         */
        AngularDerivativesFilter getAvailableDerivatives();

        /**
         * Get the attitude provider for this attitude ephemeris segment.
         *
         * @return the attitude provider for this attitude ephemeris segment.
         */
        default BoundedAttitudeProvider getAttitudeProvider() {
            return new EphemerisSegmentAttitudeProvider(this);
        }

    }

}

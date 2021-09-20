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

import org.orekit.attitudes.AggregateBoundedAttitudeProvider;
import org.orekit.attitudes.BoundedAttitudeProvider;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
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
 * @param <C> type of the angular coordinates
 * @param <S> type of the segment
 * @author Raphaël Fermé
 * @see SatelliteAttitudeEphemeris
 * @see AttitudeEphemerisSegment
 * @since 10.3
 */
public interface AttitudeEphemerisFile<C extends TimeStampedAngularCoordinates,
                                       S extends AttitudeEphemerisFile.AttitudeEphemerisSegment<C>> {

    /**
     * Get the loaded ephemeris for each satellite in the file.
     *
     * @return a map from the satellite's ID to the information about that satellite
     * contained in the file.
     */
    Map<String, ? extends SatelliteAttitudeEphemeris<C, S>> getSatellites();

    /**
     * Contains the information about a single satellite from an {@link AttitudeEphemerisFile}.
     *
     * <p> A satellite ephemeris consists of one or more {@link AttitudeEphemerisSegment}.
     * Segments are typically used to split up an ephemeris at discontinuous events.
     * @param <C> type of the angular coordinates
     * @param <S> type of the segment
     * @author Raphaël Fermé
     * @see AttitudeEphemerisFile
     * @see AttitudeEphemerisSegment
     * @since 10.3
     */
    interface SatelliteAttitudeEphemeris<C extends TimeStampedAngularCoordinates,
                                         S extends AttitudeEphemerisSegment<C>> {

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
        List<S> getSegments();

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
            for (final AttitudeEphemerisSegment<C> attitudeSegment : this.getSegments()) {
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
     * @param <C> type of the angular coordinates
     * @author Raphaël Fermé
     * @see AttitudeEphemerisFile
     * @see SatelliteAttitudeEphemeris
     * @since 10.3
     */
    interface AttitudeEphemerisSegment<C extends TimeStampedAngularCoordinates> {

        /**
         * Get an unmodifiable list of attitude data lines.
         *
         * @return a list of attitude data
         */
        List<C> getAngularCoordinates();

        /**
         * Get the reference frame from which attitude is defined.
         *
         * @return the reference frame from which attitude is defined
         */
        Frame getReferenceFrame();

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
        BoundedAttitudeProvider getAttitudeProvider();

    }

}

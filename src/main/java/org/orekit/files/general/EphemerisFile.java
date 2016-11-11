/* Contributed in the public domain.
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.AggregateBoundedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * An interface for accessing the data stored in an ephemeris file and using the data to
 * create a working {@link org.orekit.propagation.Propagator Propagator}.
 *
 * <p> An {@link EphemerisFile} consists of one or more satellites each an ID unique
 * within the file. The ephemeris for each satellite consists of one or more segments.
 *
 * <p> Some ephermis file formats may supply additional information that is not available
 * via this interface. In those cases it is recommended that the parser return a subclass
 * of this interface to provide access to the additional information.
 *
 * @author Evan Ward
 */
public interface EphemerisFile {

    /**
     * Get the loaded ephemeris for each satellite in the file.
     *
     * @return a map from the satellite's ID to the information about that satellite
     * contained in the file.
     */
    Map<String, ? extends SatelliteEphemeris> getSatellites();

    /**
     * Contains the information about a single satellite from an {@link EphemerisFile}.
     *
     * <p> A satellite ephemeris consists of one or more {@link EphemerisSegment}s.
     * Segments are typically used to split up an ephemeris at discontinuous events, such
     * as a maneuver.
     *
     * @author Evan Ward
     */
    interface SatelliteEphemeris {

        /**
         * Get the satellite ID. The satellite ID is unique only within the same ephemeris
         * file.
         *
         * @return the satellite's ID, never {@code null}.
         */
        String getId();

        /**
         * Get the standard gravitational parameter for the satellite.
         *
         * @return the gravitational parameter use in {@link #getPropagator()}, in m^3 /
         * s^2.
         */
        double getMu();

        /**
         * Get the segments of the ephemeris.
         *
         * <p> Ephemeris segments are typically used to split an ephemeris around
         * discontinuous events, such as maneuvers.
         *
         * @return the segments contained in the ephemeris file for this satellite.
         */
        List<? extends EphemerisSegment> getSegments();

        /**
         * Get the start date of the ephemeris.
         *
         * <p> The date returned by this method is equivalent to {@code
         * getPropagator().getMinDate()}.
         *
         * @return ephemeris start date.
         */
        AbsoluteDate getStart();

        /**
         * Get the end date of the ephemeris.
         *
         * <p> The date returned by this method is equivalent to {@code
         * getPropagator().getMaxDate()}.
         *
         * @return ephemeris end date.
         */
        AbsoluteDate getStop();

        /**
         * View this ephemeris as a propagator, combining data from all {@link
         * #getSegments() segments}.
         *
         * <p>In order to view the ephemeris for this satellite as a {@link Propagator}
         * several conditions must be met. An Orekit {@link Frame} and {@link TimeScale}
         * must be constructable from the frame and time scale specification in the
         * ephemeris file. This condition is met when {@link EphemerisSegment#getFrame()}
         * and {@link EphemerisSegment#getTimeScale()} return normally for all {@link
         * #getSegments() segments}. If there are multiple segments they must be adjacent
         * such that there are no duplicates or gaps in the ephemeris. The definition of
         * adjacent depends on the ephemeris format as some formats define usable start
         * and stop times that are different from the ephemeris data start and stop times.
         * If these conditions are not met an {@link OrekitException} may be thrown by
         * this method or by one of the methods of the returned {@link Propagator}.
         *
         * <p> Each call to this method creates a new propagator.
         *
         * @return a propagator for all the data in this ephemeris file.
         * @throws OrekitException if any of the conditions are not met.
         */
        default BoundedPropagator getPropagator() throws OrekitException {
            final List<BoundedPropagator> propagators = new ArrayList<>();
            for (final EphemerisSegment segment : this.getSegments()) {
                propagators.add(segment.getPropagator());
            }
            return new AggregateBoundedPropagator(propagators);
        }

    }

    /**
     * A segment of an ephemeris for a satellite.
     *
     * <p> Segments are typically used to split an ephemeris around discontinuous events
     * such as maneuvers.
     */
    interface EphemerisSegment {

        /**
         * Get the standard gravitational parameter for the satellite.
         *
         * @return the gravitational parameter use in {@link #getPropagator()}, in m^3 /
         * s^2.
         */
        double getMu();

        /**
         * Get the defining frame for this ephemeris segment.
         *
         * @return the frame identifier, as specified in the ephemeris file, or {@code
         * null} if the ephemeris file does not specify a frame.
         */
        String getFrameString();

        /**
         * Get the reference frame for this ephemeris segment.
         *
         * @return the reference frame for this segment. Never {@code null}.
         * @throws OrekitException if a frame cannot be created from {@link
         *                         #getFrameString()} and there is no default frame.
         */
        Frame getFrame() throws OrekitException;

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
         * @throws OrekitException if a time scale can not be constructed based on {@link
         *                         #getTimeScaleString()} and there is no default time
         *                         scale.
         */
        TimeScale getTimeScale() throws OrekitException;

        /**
         * Get the number of samples to use in interpolation.
         *
         * @return the number of points to use for interpolation.
         */
        int getInterpolationSamples();

        /**
         * Get which derivatives of position are available in this ephemeris segment.
         *
         * <p> While {@link #getCoordinates()} always returns position, velocity, and
         * acceleration the return value from this method indicates which of those are in
         * the ephemeris file and are actually valid.
         *
         * @return a value indicating if the file contains velocity and/or acceleration
         * data.
         */
        CartesianDerivativesFilter getAvailableDerivatives();

        /**
         * Get the coordinates for this ephemeris segment.
         *
         * @return a list of state vectors in chronological order. The coordinates are not
         * necessarily evenly spaced in time.
         */
        List<? extends TimeStampedPVCoordinates> getCoordinates();

        /**
         * Get the start date of this ephemeris segment.
         *
         * <p> The date returned by this method is equivalent to {@code
         * getPropagator().getMinDate()}.
         *
         * @return ephemeris segment start date.
         */
        AbsoluteDate getStart();

        /**
         * Get the end date of this ephemeris segment.
         *
         * <p> The date returned by this method is equivalent to {@code
         * getPropagator().getMaxDate()}.
         *
         * @return ephemeris segment end date.
         */
        AbsoluteDate getStop();

        /**
         * View this ephemeris segment as a propagator.
         *
         * <p>In order to view the ephemeris for this satellite as a {@link Propagator}
         * several conditions must be met. An Orekit {@link Frame} and {@link TimeScale}
         * must be constructable from the frame and time scale specification in the
         * ephemeris file. This condition is met when {@link EphemerisSegment#getFrame()}
         * and {@link EphemerisSegment#getTimeScale()} return normally. Additionally,
         * {@link #getMu()} must return a valid value. If these conditions are not met an
         * {@link OrekitException} may be thrown by this method or by one of the methods
         * of the returned {@link Propagator}.
         *
         * <p> Each call to this method creates a new propagator.
         *
         * @return a propagator for this ephemeris segment.
         * @throws OrekitException if any of the conditions are not met.
         */
        default BoundedPropagator getPropagator() throws OrekitException {
            return new EphemerisSegmentPropagator(this);
        }

    }

}

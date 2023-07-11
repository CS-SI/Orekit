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

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.AggregateBoundedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * An interface for accessing the data stored in an ephemeris file and using the data to
 * create a working {@link org.orekit.propagation.Propagator Propagator}.
 *
 * <p> An {@link EphemerisFile} consists of one or more satellites each with a unique ID
 * within the file. The ephemeris for each satellite consists of one or more segments.
 *
 * <p> Some ephemeris file formats may supply additional information that is not available
 * via this interface. In those cases it is recommended that the parser return a subclass
 * of this interface to provide access to the additional information.
 *
 * @param <C> type of the Cartesian coordinates
 * @param <S> type of the segment
 * @author Evan Ward
 * @see SatelliteEphemeris
 * @see EphemerisSegment
 */
public interface EphemerisFile<C extends TimeStampedPVCoordinates,
                               S extends EphemerisFile.EphemerisSegment<C>> {

    /**
     * Get the loaded ephemeris for each satellite in the file.
     *
     * @return a map from the satellite's ID to the information about that satellite
     * contained in the file.
     */
    Map<String, ? extends SatelliteEphemeris<C, S>> getSatellites();

    /**
     * Contains the information about a single satellite from an {@link EphemerisFile}.
     *
     * <p> A satellite ephemeris consists of one or more {@link EphemerisSegment}s.
     * Segments are typically used to split up an ephemeris at discontinuous events, such
     * as a maneuver.
     * @param <C> type of the Cartesian coordinates
     * @param <S> type of the segment
     * @author Evan Ward
     * @see EphemerisFile
     * @see EphemerisSegment
     */
    interface SatelliteEphemeris<C extends TimeStampedPVCoordinates,
                                 S extends EphemerisSegment<C>> {

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
         * @return the gravitational parameter used in {@link #getPropagator(AttitudeProvider)}, in m³/s².
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
        List<S> getSegments();

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
         * <p> The date returned by this method is equivalent to {@code getPropagator().getMaxDate()}.
         *
         * @return ephemeris end date.
         */
        AbsoluteDate getStop();

        /**
         * View this ephemeris as a propagator, combining data from all {@link
         * #getSegments() segments}.
         *
         * <p>
         * In order to view the ephemeris for this satellite as a {@link Propagator}
         * several conditions must be met. An Orekit {@link Frame} must be constructable
         * from the frame specification in the ephemeris file. This condition is met when
         * {@link EphemerisSegment#getFrame()} return normally for all {@link
         * #getSegments() segments}. If there are multiple segments they must be adjacent
         * such that there are no duplicates or gaps in the ephemeris. The definition of
         * adjacent depends on the ephemeris format as some formats define usable start
         * and stop times that are different from the ephemeris data start and stop times.
         * If these conditions are not met an {@link OrekitException} may be thrown by
         * this method or by one of the methods of the returned {@link Propagator}.
         * </p>
         * <p>
         * The {@link AttitudeProvider attitude provider} used is a {@link FrameAlignedProvider}
         * aligned with the {@link EphemerisSegment#getInertialFrame() inertial frame} from the first segment.
         * </p>
         *
         * <p>Each call to this method creates a new propagator.</p>
         *
         * @return a propagator for all the data in this ephemeris file.
         */
        default BoundedPropagator getPropagator() {
            return getPropagator(new FrameAlignedProvider(getSegments().get(0).getInertialFrame()));
        }

        /**
         * View this ephemeris as a propagator, combining data from all {@link
         * #getSegments() segments}.
         *
         * <p>
         * In order to view the ephemeris for this satellite as a {@link Propagator}
         * several conditions must be met. An Orekit {@link Frame} must be constructable
         * from the frame specification in the ephemeris file. This condition is met when
         * {@link EphemerisSegment#getFrame()} return normally for all {@link
         * #getSegments() segments}. If there are multiple segments they must be adjacent
         * such that there are no duplicates or gaps in the ephemeris. The definition of
         * adjacent depends on the ephemeris format as some formats define usable start
         * and stop times that are different from the ephemeris data start and stop times.
         * If these conditions are not met an {@link OrekitException} may be thrown by
         * this method or by one of the methods of the returned {@link Propagator}.
         * </p>
         *
         * <p>Each call to this method creates a new propagator.</p>
         *
         * @param attitudeProvider provider for attitude computation
         * @return a propagator for all the data in this ephemeris file.
         * @since 12.0
         */
        default BoundedPropagator getPropagator(final  AttitudeProvider attitudeProvider) {
            final List<BoundedPropagator> propagators = new ArrayList<>();
            for (final EphemerisSegment<C> segment : this.getSegments()) {
                propagators.add(segment.getPropagator(attitudeProvider));
            }
            return new AggregateBoundedPropagator(propagators);
        }

    }

    /**
     * A segment of an ephemeris for a satellite.
     *
     * <p> Segments are typically used to split an ephemeris around discontinuous events
     * such as maneuvers.
     *
     * @param <C> type of the Cartesian coordinates
     * @author Evan Ward
     * @see EphemerisFile
     * @see SatelliteEphemeris
     */
    interface EphemerisSegment<C extends TimeStampedPVCoordinates> {

        /**
         * Get the standard gravitational parameter for the satellite.
         *
         * @return the gravitational parameter used in {@link #getPropagator(AttitudeProvider)}, in m³/s².
         */
        double getMu();

        /**
         * Get the reference frame for this ephemeris segment. The defining frame for
         * {@link #getCoordinates()}.
         *
         * @return the reference frame for this segment. Never {@code null}.
         */
        Frame getFrame();

        /**
         * Get the inertial reference frame for this ephemeris segment. Defines the
         * propagation frame for {@link #getPropagator(AttitudeProvider)}.
         *
         * <p>The default implementation returns {@link #getFrame()} if it is inertial.
         * Otherwise it returns {@link Frame#getRoot()}. Implementors are encouraged to
         * override this default implementation if a more suitable inertial frame is
         * available.
         *
         * @return an reference frame that is inertial, i.e. {@link
         * Frame#isPseudoInertial()} is {@code true}. May be the same as {@link
         * #getFrame()} if it is inertial.
         */
        default Frame getInertialFrame() {
            final Frame frame = getFrame();
            if (frame.isPseudoInertial()) {
                return frame;
            }
            return Frame.getRoot();
        }

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
         * Get the coordinates for this ephemeris segment in {@link #getFrame()}.
         *
         * @return a list of state vectors in chronological order. The coordinates are not
         * necessarily evenly spaced in time. The value of {@link
         * #getAvailableDerivatives()} indicates if the velocity or accelerations were
         * specified in the file. Any position, velocity, or acceleration coordinates that
         * are not specified in the ephemeris file are zero in the returned values.
         */
        List<C> getCoordinates();

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
         * <p>
         * In order to view the ephemeris for this satellite as a {@link Propagator}
         * several conditions must be met. An Orekit {@link Frame} must be constructable
         * from the frame specification in the ephemeris file. This condition is met when
         * {@link EphemerisSegment#getFrame()} return normally. Additionally,
         * {@link #getMu()} must return a valid value. If these conditions are not met an
         * {@link OrekitException} may be thrown by this method or by one of the methods
         * of the returned {@link Propagator}.
         * </p>
         * <p>
         * The {@link AttitudeProvider attitude provider} used is a {@link FrameAlignedProvider}
         * aligned with the {@link #getInertialFrame() inertial frame}
         * </p>
         *
         * <p>Each call to this method creates a new propagator.</p>
         *
         * @return a propagator for this ephemeris segment.
         */
        default BoundedPropagator getPropagator() {
            return new EphemerisSegmentPropagator<>(this, new FrameAlignedProvider(getInertialFrame()));
        }

        /**
         * View this ephemeris segment as a propagator.
         *
         * <p>
         * In order to view the ephemeris for this satellite as a {@link Propagator}
         * several conditions must be met. An Orekit {@link Frame} must be constructable
         * from the frame specification in the ephemeris file. This condition is met when
         * {@link EphemerisSegment#getFrame()} return normally. Additionally,
         * {@link #getMu()} must return a valid value. If these conditions are not met an
         * {@link OrekitException} may be thrown by this method or by one of the methods
         * of the returned {@link Propagator}.
         * </p>
         *
         * <p>Each call to this method creates a new propagator.</p>
         *
         * @param attitudeProvider provider for attitude computation
         * @return a propagator for this ephemeris segment.
         * @since 12.0
         */
        default BoundedPropagator getPropagator(final  AttitudeProvider attitudeProvider) {
            return new EphemerisSegmentPropagator<>(this, attitudeProvider);
        }

    }

}

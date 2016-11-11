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

import java.util.List;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.EphemerisFile.EphemerisSegment;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ImmutableTimeStampedCache;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * A {@link Propagator} based on a {@link EphemerisSegment}.
 *
 * <p> The {@link #getPVCoordinates(AbsoluteDate, Frame)} is implemented without using the
 * {@link #propagate(AbsoluteDate)} methods so using this class as a {@link
 * org.orekit.utils.PVCoordinatesProvider} still behaves as expected when the ephemeris
 * file did not have a valid gravitational parameter.
 *
 * @author Evan Ward
 */
class EphemerisSegmentPropagator extends AbstractAnalyticalPropagator
        implements BoundedPropagator {

    /** Default frame to use when creating orbits. */
    public static final Frame DEFAULT_INERTIAL_FRAME = FramesFactory.getGCRF();

    /**
     * Sorted cache of state vectors. A duplication of the information in {@link
     * #ephemeris} that could be avoided by duplicating the logic of {@link
     * ImmutableTimeStampedCache#getNeighbors(AbsoluteDate)} for a general {@link List}.
     */
    private final ImmutableTimeStampedCache<TimeStampedPVCoordinates> cache;
    /** Tabular data from which this propagator is built. */
    private final EphemerisSegment ephemeris;
    /** Inertial frame used for creating orbits. */
    private final Frame inertialFrame;
    /** Frame of the ephemeris data. */
    private final Frame ephemerisFrame;

    /**
     * Create a {@link Propagator} from an ephemeris segment.
     *
     * <p> If the {@link EphemerisSegment#getFrame() ephemeris frame} is not {@link
     * Frame#isPseudoInertial() inertial} then {@link #DEFAULT_INERTIAL_FRAME} is used as
     * the frame for orbits created by this propagator.
     *
     * @param ephemeris segment containing the data for this propagator.
     * @throws OrekitException if {@link EphemerisSegment#getFrame()} throws one.
     */
    EphemerisSegmentPropagator(final EphemerisSegment ephemeris) throws OrekitException {
        super(Propagator.DEFAULT_LAW);
        this.cache = new ImmutableTimeStampedCache<>(
                ephemeris.getInterpolationSamples(),
                ephemeris.getCoordinates());
        this.ephemeris = ephemeris;
        this.ephemerisFrame = ephemeris.getFrame();
        if (ephemerisFrame.isPseudoInertial()) {
            this.inertialFrame = ephemerisFrame;
        } else {
            this.inertialFrame = DEFAULT_INERTIAL_FRAME;
        }
    }

    @Override
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date,
                                                     final Frame frame) throws OrekitException {
        final List<TimeStampedPVCoordinates> neighbors = this.cache.getNeighbors(date);
        final TimeStampedPVCoordinates point =
                TimeStampedPVCoordinates.interpolate(date, ephemeris.getAvailableDerivatives(), neighbors);
        return ephemerisFrame.getTransformTo(frame, date).transformPVCoordinates(point);
    }

    @Override
    protected Orbit propagateOrbit(final AbsoluteDate date) throws OrekitException {
        final TimeStampedPVCoordinates pv = this.getPVCoordinates(date, inertialFrame);
        return new CartesianOrbit(pv, inertialFrame, this.ephemeris.getMu());
    }

    @Override
    public AbsoluteDate getMinDate() {
        return ephemeris.getStart();
    }

    @Override
    public AbsoluteDate getMaxDate() {
        return ephemeris.getStop();
    }

    @Override
    protected double getMass(final AbsoluteDate date) throws OrekitException {
        return DEFAULT_MASS;
    }

    @Override
    public SpacecraftState getInitialState() throws OrekitException {
        return this.basicPropagate(this.getMinDate());
    }

    @Override
    protected void resetIntermediateState(final SpacecraftState state,
                                          final boolean forward) throws OrekitException {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

}

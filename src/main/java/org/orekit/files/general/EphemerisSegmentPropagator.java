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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.EphemerisFile.EphemerisSegment;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterpolator;
import org.orekit.utils.ImmutableTimeStampedCache;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinatesHermiteInterpolator;

/**
 * A {@link Propagator} based on a {@link EphemerisSegment}.
 *
 * <p> The {@link #getPVCoordinates(AbsoluteDate, Frame)} is implemented without using the
 * {@link #propagate(AbsoluteDate)} methods so using this class as a {@link
 * org.orekit.utils.PVCoordinatesProvider} still behaves as expected when the ephemeris
 * file did not have a valid gravitational parameter.
 * @param <C> type of the Cartesian coordinates
 *
 * @author Evan Ward
 */
class EphemerisSegmentPropagator<C extends TimeStampedPVCoordinates> extends AbstractAnalyticalPropagator
        implements BoundedPropagator {

    /**
     * Sorted cache of state vectors. A duplication of the information in {@link
     * #ephemeris} that could be avoided by duplicating the logic of {@link
     * ImmutableTimeStampedCache#getNeighbors(AbsoluteDate)} for a general {@link List}.
     */
    private final ImmutableTimeStampedCache<C> cache;
    /** Tabular data from which this propagator is built. */
    private final EphemerisSegment<C> ephemeris;
    /** Inertial frame used for creating orbits. */
    private final Frame inertialFrame;
    /** Frame of the ephemeris data. */
    private final Frame ephemerisFrame;

    /**
     * Create a {@link Propagator} from an ephemeris segment.
     *
     * @param ephemeris segment containing the data for this propagator.
     * @param attitudeProvider provider for attitude computation
     */
    EphemerisSegmentPropagator(final EphemerisSegment<C> ephemeris,
                               final AttitudeProvider attitudeProvider) {
        super(attitudeProvider);
        this.cache = new ImmutableTimeStampedCache<>(
                ephemeris.getInterpolationSamples(),
                ephemeris.getCoordinates());
        this.ephemeris = ephemeris;
        this.ephemerisFrame = ephemeris.getFrame();
        this.inertialFrame = ephemeris.getInertialFrame();
        // set the initial state so getFrame() works
        final TimeStampedPVCoordinates ic = cache.getEarliest();
        final TimeStampedPVCoordinates icInertial = ephemerisFrame
                .getTransformTo(inertialFrame, ic.getDate())
                .transformPVCoordinates(ic);
        super.resetInitialState(
                new SpacecraftState(
                        new CartesianOrbit(
                                icInertial, inertialFrame, ephemeris.getMu()
                        ),
                        getAttitudeProvider().getAttitude(
                                icInertial.toTaylorProvider(inertialFrame),
                                ic.getDate(),
                                inertialFrame),
                        DEFAULT_MASS
                )
        );
    }

    @Override
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
        final Stream<C> neighbors = this.cache.getNeighbors(date);

        // cast stream to super type
        final Stream<TimeStampedPVCoordinates> castedNeighbors = neighbors.map(neighbor -> (TimeStampedPVCoordinates) neighbor);

        // convert to list
        final List<TimeStampedPVCoordinates> castedNeighborsList = castedNeighbors.collect(Collectors.toList());

        // create interpolator
        final TimeInterpolator<TimeStampedPVCoordinates> interpolator =
                new TimeStampedPVCoordinatesHermiteInterpolator(castedNeighborsList.size(), ephemeris.getAvailableDerivatives());

        final TimeStampedPVCoordinates point = interpolator.interpolate(date, castedNeighborsList);
        return ephemerisFrame.getTransformTo(frame, date).transformPVCoordinates(point);
    }

    @Override
    protected Orbit propagateOrbit(final AbsoluteDate date) {
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
    protected double getMass(final AbsoluteDate date) {
        return DEFAULT_MASS;
    }

    @Override
    public SpacecraftState getInitialState() {
        return this.basicPropagate(this.getMinDate());
    }

    @Override
    protected void resetIntermediateState(final SpacecraftState state,
                                          final boolean forward) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    @Override
    public void resetInitialState(final SpacecraftState state) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

}

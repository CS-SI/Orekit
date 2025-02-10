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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
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
import org.orekit.utils.SortedListTrimmer;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinatesHermiteInterpolator;

import java.util.ArrayList;
import java.util.List;

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
public class EphemerisSegmentPropagator<C extends TimeStampedPVCoordinates> extends AbstractAnalyticalPropagator
        implements BoundedPropagator {

    /** Tabular data from which this propagator is built. */
    private final EphemerisSegment<C> ephemeris;
    /** Interpolator to use.
     * @since 12.2
     */
    private final TimeStampedPVCoordinatesHermiteInterpolator interpolator;
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
    public EphemerisSegmentPropagator(final EphemerisSegment<C> ephemeris,
                                      final AttitudeProvider attitudeProvider) {
        super(attitudeProvider);
        this.ephemeris      = ephemeris;
        this.interpolator   = new TimeStampedPVCoordinatesHermiteInterpolator(ephemeris.getInterpolationSamples(),
                                                                              ephemeris.getAvailableDerivatives());
        this.ephemerisFrame = ephemeris.getFrame();
        this.inertialFrame  = ephemeris.getInertialFrame();
        // set the initial state so getFrame() works
        final TimeStampedPVCoordinates ic = ephemeris.getCoordinates().get(0);
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
        final TimeStampedPVCoordinates interpolatedPVCoordinates = interpolate(date);
        return ephemerisFrame.getTransformTo(frame, date).transformPVCoordinates(interpolatedPVCoordinates);
    }

    @Override
    public Vector3D getPosition(final AbsoluteDate date, final Frame frame) {
        final Vector3D interpolatedPosition = interpolate(date).getPosition();
        return ephemerisFrame.getStaticTransformTo(frame, date).transformPosition(interpolatedPosition);
    }

    @Override
    public Orbit propagateOrbit(final AbsoluteDate date) {
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

    /** Interpolate ephemeris segment at date.
     *
     * @param date interpolation date
     * @return interpolated position-velocity vector
     */
    private TimeStampedPVCoordinates interpolate(final AbsoluteDate date) {
        final List<C> neighbors = new SortedListTrimmer(interpolator.getNbInterpolationPoints()).
                                  getNeighborsSubList(date, ephemeris.getCoordinates());

        // cast stream to super type
        final List<TimeStampedPVCoordinates> castedNeighbors = new ArrayList<>(neighbors.size());
        castedNeighbors.addAll(neighbors);

        // create interpolator
        return interpolator.interpolate(date, castedNeighbors);
    }

}

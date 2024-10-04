/* Copyright Luc Maisonobe
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
package org.orekit.files.sp3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.analysis.interpolation.HermiteInterpolator;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.files.general.EphemerisFile;
import org.orekit.files.general.EphemerisSegmentPropagator;
import org.orekit.frames.Frame;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ClockModel;
import org.orekit.time.ClockOffset;
import org.orekit.time.SampledClockModel;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.SortedListTrimmer;

/** One segment of an {@link SP3Ephemeris}.
 * @author Thomas Neidhart
 * @author Evan Ward
 * @author Luc Maisonobe
 * @since 12.0
 */
public class SP3Segment implements EphemerisFile.EphemerisSegment<SP3Coordinate> {

    /** Standard gravitational parameter in m³ / s². */
    private final double mu;

    /** Reference frame. */
    private final Frame frame;

    /** Number of points to use for interpolation. */
    private final int interpolationSamples;

    /** Available derivatives. */
    private final CartesianDerivativesFilter filter;

    /** Ephemeris Data. */
    private final List<SP3Coordinate> coordinates;

    /** Simple constructor.
     * @param mu standard gravitational parameter to use for creating
     * {@link org.orekit.orbits.Orbit Orbits} from the ephemeris data.
     * @param frame reference frame
     * @param interpolationSamples number of points to use for interpolation
     * @param filter available derivatives
     */
    public SP3Segment(final double mu, final Frame frame,
                      final int interpolationSamples, final CartesianDerivativesFilter filter) {
        this.mu                   = mu;
        this.frame                = frame;
        this.interpolationSamples = interpolationSamples;
        this.filter               = filter;
        this.coordinates          = new ArrayList<>();
    }

    /** Extract the clock model.
     * <p>
     * If some clock or clock rate are present in the SP3 files as default values (999999.999999), then they
     * filtered out here when building the clock model, so interpolation will work if at least there are
     * some remaining regular values.
     * </p>
     * @return extracted clock model
     * @since 12.1
     */
    public ClockModel extractClockModel() {
        final List<ClockOffset> sample = new ArrayList<>(coordinates.size());
        coordinates.forEach(c -> {
            final AbsoluteDate date   = c.getDate();
            final double       offset = c.getClockCorrection();
            if (!Double.isNaN(offset)) {
                final double rate = filter.getMaxOrder() > 0 ? c.getClockRateChange() : Double.NaN;
                sample.add(new ClockOffset(date, offset, rate, Double.NaN));
            }
        });
        return new SampledClockModel(sample, interpolationSamples);
    }

    /** {@inheritDoc} */
    @Override
    public double getMu() {
        return mu;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getStart() {
        return coordinates.get(0).getDate();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getStop() {
        return coordinates.get(coordinates.size() - 1).getDate();
    }

    /** {@inheritDoc} */
    @Override
    public Frame getFrame() {
        return frame;
    }

    /** {@inheritDoc} */
    @Override
    public int getInterpolationSamples() {
        return interpolationSamples;
    }

    /** {@inheritDoc} */
    @Override
    public CartesianDerivativesFilter getAvailableDerivatives() {
        return filter;
    }

    /** {@inheritDoc} */
    @Override
    public List<SP3Coordinate> getCoordinates() {
        return Collections.unmodifiableList(this.coordinates);
    }

    /** Adds a new P/V coordinate.
     * @param coord the P/V coordinate of the satellite
     */
    public void addCoordinate(final SP3Coordinate coord) {
        coordinates.add(coord);
    }

    /** {@inheritDoc} */
    @Override
    public BoundedPropagator getPropagator() {
        return new PropagatorWithClock(new FrameAlignedProvider(getInertialFrame()));
    }

    /** {@inheritDoc} */
    @Override
    public BoundedPropagator getPropagator(final AttitudeProvider attitudeProvider) {
        return new PropagatorWithClock(attitudeProvider);
    }

    /** Propagator including clock.
     * @since 12.1
     */
    private class PropagatorWithClock extends EphemerisSegmentPropagator<SP3Coordinate> {

        /** Trimmer for coordinates list. */
        private final SortedListTrimmer trimmer;

        /** Simple constructor.
         * @param attitudeProvider attitude porovider
         */
        PropagatorWithClock(final AttitudeProvider attitudeProvider) {
            super(SP3Segment.this, attitudeProvider);
            this.trimmer = new SortedListTrimmer(getInterpolationSamples());
        }

        /** {@inheritDoc} */
        @Override
        protected SpacecraftState updateAdditionalStates(final SpacecraftState original) {

            final HermiteInterpolator interpolator = new HermiteInterpolator();

            // Fill interpolator with sample
            trimmer.
                getNeighborsSubList(original.getDate(), coordinates).
                forEach(c -> {
                    final double deltaT = c.getDate().durationFrom(original.getDate());
                    if (filter.getMaxOrder() < 1) {
                        // we use only clock offset
                        interpolator.addSamplePoint(deltaT,
                                                    new double[] { c.getClockCorrection() });
                    } else {
                        // we use both clock offset and clock rate
                        interpolator.addSamplePoint(deltaT,
                                                    new double[] { c.getClockCorrection() },
                                                    new double[] { c.getClockRateChange() });
                    }
                });

            // perform interpolation (we get derivatives even if we used only clock offset)
            final double[][] derivatives = interpolator.derivatives(0.0, 1);

            // add the clock offset and its first derivative
            return super.updateAdditionalStates(original).
                addAdditionalState(SP3Utils.CLOCK_ADDITIONAL_STATE, derivatives[0]).
                addAdditionalStateDerivative(SP3Utils.CLOCK_ADDITIONAL_STATE, derivatives[1]);

        }

    }

}

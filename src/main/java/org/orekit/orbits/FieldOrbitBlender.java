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
package org.orekit.orbits;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.polynomials.SmoothStepFactory;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalPropagator;
import org.orekit.propagation.analytical.FieldAbstractAnalyticalPropagator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;

import java.util.List;

/**
 * Orbit blender.
 * <p>
 * Its purpose is to interpolate orbit state between tabulated orbit states using the concept of blending, exposed in :
 * "Efficient Covariance Interpolation using Blending of Approximate State Error Transitions" by Sergei Tanygin, and applying
 * it to orbit states instead of covariances.
 * <p>
 * It propagates tabulated values to the interpolating time using given analytical propagator and then blend each propagated
 * states using a smoothstep function. It gives especially good results as explained
 * <a href="https://orekit.org/doc/technical-notes/Implementation_of_covariance_interpolation_in_Orekit.pdf">here</a>
 * compared to Hermite interpolation when time steps between tabulated values get significant (In LEO, &gt; 10 mn for
 * example).
 *
 * @param <KK> type of field element
 *
 * @author Vincent Cucchietti
 * @see org.hipparchus.analysis.polynomials.SmoothStepFactory
 * @see org.hipparchus.analysis.polynomials.SmoothStepFactory.FieldSmoothStepFunction
 */
public class FieldOrbitBlender<KK extends CalculusFieldElement<KK>> extends AbstractFieldOrbitInterpolator<KK> {

    /** Analytical propagator used to propagate tabulated orbits to interpolating time. */
    private final FieldAbstractAnalyticalPropagator<KK> analyticalPropagator;

    /** Blending function. */
    private final SmoothStepFactory.FieldSmoothStepFunction<KK> blendingFunction;

    /**
     * Default constructor.
     *
     * @param blendingFunction
     * {@link org.hipparchus.analysis.polynomials.SmoothStepFactory.SmoothStepFunction smoothstep function} used for
     * blending
     * @param analyticalPropagator analytical propagator used to propagate tabulated orbits to interpolating time
     * @param outputInertialFrame output inertial frame
     *
     * @throws OrekitException if output frame is not inertial
     */
    public FieldOrbitBlender(final SmoothStepFactory.FieldSmoothStepFunction<KK> blendingFunction,
                             final FieldAbstractAnalyticalPropagator<KK> analyticalPropagator,
                             final Frame outputInertialFrame) {
        super(DEFAULT_INTERPOLATION_POINTS, 0., outputInertialFrame);
        this.blendingFunction     = blendingFunction;
        this.analyticalPropagator = analyticalPropagator;
    }

    /** {@inheritDoc} */
    @Override
    public FieldOrbit<KK> interpolate(final InterpolationData interpolationData) {

        // Get interpolation date
        final FieldAbsoluteDate<KK> interpolationDate = interpolationData.getInterpolationDate();

        // Get first and last entry
        final List<FieldOrbit<KK>> neighborList  = interpolationData.getNeighborList();
        final FieldOrbit<KK>       previousOrbit = neighborList.get(0);
        final FieldOrbit<KK>       nextOrbit     = neighborList.get(1);

        // Propagate orbits
        final FieldOrbit<KK> forwardedOrbit  = propagateOrbitAnalytically(previousOrbit, interpolationDate);
        final FieldOrbit<KK> backwardedOrbit = propagateOrbitAnalytically(nextOrbit, interpolationDate);

        // Extract position-velocity-acceleration coordinates
        final FieldPVCoordinates<KK> forwardedPV  = forwardedOrbit.getPVCoordinates(getOutputInertialFrame());
        final FieldPVCoordinates<KK> backwardedPV = backwardedOrbit.getPVCoordinates(getOutputInertialFrame());

        // Blend PV coordinates
        final KK timeParameter = getTimeParameter(interpolationDate, previousOrbit.getDate(), nextOrbit.getDate());
        final KK blendingValue = blendingFunction.value(timeParameter);

        final FieldPVCoordinates<KK> blendedPV = forwardedPV.blendArithmeticallyWith(backwardedPV, blendingValue);

        // Output new blended instance
        return new FieldCartesianOrbit<>(blendedPV, getOutputInertialFrame(), interpolationDate, previousOrbit.getMu());
    }

    /**
     * Propagate orbit using predefined {@link AbstractAnalyticalPropagator analytical propagator}.
     *
     * @param tabulatedOrbit tabulated orbit to propagate
     * @param propagationDate propagation date
     *
     * @return orbit propagated to propagation date
     */
    private FieldOrbit<KK> propagateOrbitAnalytically(final FieldOrbit<KK> tabulatedOrbit,
                                                      final FieldAbsoluteDate<KK> propagationDate) {

        analyticalPropagator.resetInitialState(new FieldSpacecraftState<>(tabulatedOrbit));

        return analyticalPropagator.propagate(propagationDate).getOrbit();
    }
}

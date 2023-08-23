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
package org.orekit.propagation.integration;

import java.util.Arrays;
import java.util.List;

import org.hipparchus.ode.DenseOutputModel;
import org.hipparchus.ode.ODEStateAndDerivative;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.TimeStampedPVCoordinates;

/** This class stores sequentially generated orbital parameters for
 * later retrieval.
 *
 * <p>
 * Instances of this class are built automatically when the {@link
 * org.orekit.propagation.Propagator#getEphemerisGenerator()
 * getEphemerisGenerator} method has been called. They are created when propagation is over.
 * Random access to any intermediate state of the orbit throughout the propagation range is
 * possible afterwards through this object.
 * </p>
 * <p>
 * A typical use case is for numerically integrated orbits, which can be used by
 * algorithms that need to wander around according to their own algorithm without
 * cumbersome tight links with the integrator.
 * </p>
 * <p>
 * As this class implements the {@link org.orekit.propagation.Propagator Propagator}
 * interface, it can itself be used in batch mode to build another instance of the
 * same type. This is however not recommended since it would be a waste of resources.
 * </p>
 * <p>
 * Note that this class stores all intermediate states along with interpolation
 * models, so it may be memory intensive.
 * </p>
 *
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @author Mathieu Rom&eacute;ro
 * @author Luc Maisonobe
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class IntegratedEphemeris
    extends AbstractAnalyticalPropagator implements BoundedPropagator {

    /** Event detection requires evaluating the state slightly before / past an event. */
    private static final double EXTRAPOLATION_TOLERANCE = 1.0;

    /** Mapper between raw double components and spacecraft state. */
    private final StateMapper mapper;

    /** Type of orbit to output (mean or osculating).
     * <p>
     * This is used only in the case of semianalitical propagators where there is a clear separation between
     * mean and short periodic elements. It is ignored by the Numerical propagator.
     * </p>
     */
    private PropagationType type;

    /** Start date of the integration (can be min or max). */
    private final AbsoluteDate startDate;

    /** First date of the range. */
    private final AbsoluteDate minDate;

    /** Last date of the range. */
    private final AbsoluteDate maxDate;

    /** Underlying raw mathematical model. */
    private DenseOutputModel model;

    /** Unmanaged additional states that must be simply copied. */
    private final DoubleArrayDictionary unmanaged;

    /** Names of additional equations.
     * @since 11.2
     */
    private final String[] equations;

    /** Dimensions of additional equations.
     * @since 11.2
     */
    private final int[] dimensions;

    /** Creates a new instance of IntegratedEphemeris.
     * @param startDate Start date of the integration (can be minDate or maxDate)
     * @param minDate first date of the range
     * @param maxDate last date of the range
     * @param mapper mapper between raw double components and spacecraft state
     * @param type type of orbit to output (mean or osculating)
     * @param model underlying raw mathematical model
     * @param unmanaged unmanaged additional states that must be simply copied
     * @param providers providers for pre-integrated states
     * @param equations names of additional equations
     * @param dimensions dimensions of additional equations
     * @since 11.1.2
     */
    public IntegratedEphemeris(final AbsoluteDate startDate,
                               final AbsoluteDate minDate, final AbsoluteDate maxDate,
                               final StateMapper mapper, final PropagationType type,
                               final DenseOutputModel model,
                               final DoubleArrayDictionary unmanaged,
                               final List<AdditionalStateProvider> providers,
                               final String[] equations, final int[] dimensions) {

        super(mapper.getAttitudeProvider());

        this.startDate = startDate;
        this.minDate   = minDate;
        this.maxDate   = maxDate;
        this.mapper    = mapper;
        this.type      = type;
        this.model     = model;
        this.unmanaged = unmanaged;

        // set up the pre-integrated providers
        for (final AdditionalStateProvider provider : providers) {
            addAdditionalStateProvider(provider);
        }

        this.equations  = equations.clone();
        this.dimensions = dimensions.clone();

        // set up initial state
        super.resetInitialState(getInitialState());

    }

    /** Interpolate the model at some date.
     * @param date desired interpolation date
     * @return state interpolated at date
     */
    private ODEStateAndDerivative getInterpolatedState(final AbsoluteDate date) {

        // compare using double precision instead of AbsoluteDate.compareTo(...)
        // because time is expressed as a double when searching for events
        if (date.compareTo(minDate.shiftedBy(-EXTRAPOLATION_TOLERANCE)) < 0) {
            // date is outside of supported range
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_BEFORE,
                    date, minDate, maxDate, minDate.durationFrom(date));
        }
        if (date.compareTo(maxDate.shiftedBy(EXTRAPOLATION_TOLERANCE)) > 0) {
            // date is outside of supported range
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_AFTER,
                    date, minDate, maxDate, date.durationFrom(maxDate));
        }

        return model.getInterpolatedState(date.durationFrom(startDate));

    }

    /** {@inheritDoc} */
    @Override
    protected SpacecraftState basicPropagate(final AbsoluteDate date) {
        final ODEStateAndDerivative os = getInterpolatedState(date);
        SpacecraftState state = mapper.mapArrayToState(mapper.mapDoubleToDate(os.getTime(), date),
                                                       os.getPrimaryState(), os.getPrimaryDerivative(),
                                                       type);
        for (DoubleArrayDictionary.Entry initial : unmanaged.getData()) {
            state = state.addAdditionalState(initial.getKey(), initial.getValue());
        }
        return state;
    }

    /** {@inheritDoc} */
    protected Orbit propagateOrbit(final AbsoluteDate date) {
        return basicPropagate(date).getOrbit();
    }

    /** {@inheritDoc} */
    protected double getMass(final AbsoluteDate date) {
        return basicPropagate(date).getMass();
    }

    /** {@inheritDoc} */
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
        return propagate(date).getPVCoordinates(frame);
    }

    /** Get the first date of the range.
     * @return the first date of the range
     */
    public AbsoluteDate getMinDate() {
        return minDate;
    }

    /** Get the last date of the range.
     * @return the last date of the range
     */
    public AbsoluteDate getMaxDate() {
        return maxDate;
    }

    @Override
    public Frame getFrame() {
        return this.mapper.getFrame();
    }

    /** {@inheritDoc} */
    public void resetInitialState(final SpacecraftState state) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final SpacecraftState state, final boolean forward) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    @Override
    public void setAttitudeProvider(final AttitudeProvider attitudeProvider) {
        super.setAttitudeProvider(attitudeProvider);
        if (mapper != null) {
            // At the construction, the mapper is not set yet
            // However, if the attitude provider is changed afterwards, it must be changed in the mapper too
            mapper.setAttitudeProvider(attitudeProvider);
        }
    }

    /** {@inheritDoc} */
    public SpacecraftState getInitialState() {
        return updateAdditionalStates(basicPropagate(getMinDate()));
    }

    /** {@inheritDoc} */
    @Override
    protected SpacecraftState updateAdditionalStates(final SpacecraftState original) {

        SpacecraftState updated = super.updateAdditionalStates(original);

        if (equations.length > 0) {
            final ODEStateAndDerivative osd                = getInterpolatedState(updated.getDate());
            final double[]              combinedState      = osd.getSecondaryState(1);
            final double[]              combinedDerivative = osd.getSecondaryDerivative(1);
            int index = 0;
            for (int i = 0; i < equations.length; ++i) {
                final double[] state      = Arrays.copyOfRange(combinedState,      index, index + dimensions[i]);
                final double[] derivative = Arrays.copyOfRange(combinedDerivative, index, index + dimensions[i]);
                updated = updated.
                          addAdditionalState(equations[i], state).
                          addAdditionalStateDerivative(equations[i], derivative);
                index += dimensions[i];
            }
        }

        return updated;

    }

}

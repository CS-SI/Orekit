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
import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.ode.FieldDenseOutputModel;
import org.hipparchus.ode.FieldODEStateAndDerivative;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldAdditionalStateProvider;
import org.orekit.propagation.FieldBoundedPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.analytical.FieldAbstractAnalyticalPropagator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldArrayDictionary;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** This class stores sequentially generated orbital parameters for
 * later retrieval.
 *
 * <p>
 * Instances of this class are built automatically when the {@link
 * org.orekit.propagation.FieldPropagator#getEphemerisGenerator()
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
 * @param <T> type of the field elements
 */
public class FieldIntegratedEphemeris <T extends CalculusFieldElement<T>>
    extends FieldAbstractAnalyticalPropagator<T> implements FieldBoundedPropagator<T> {

    /** Event detection requires evaluating the state slightly before / past an event. */
    private static final double EXTRAPOLATION_TOLERANCE = 1.0;

    /** Mapper between raw double components and spacecraft state. */
    private final FieldStateMapper<T> mapper;

    /** Type of orbit to output (mean or osculating).
     * <p>
     * This is used only in the case of semianalitical propagators where there is a clear separation between
     * mean and short periodic elements. It is ignored by the Numerical propagator.
     * </p>
     */
    private PropagationType type;

    /** Start date of the integration (can be min or max). */
    private final FieldAbsoluteDate<T> startDate;

    /** First date of the range. */
    private final FieldAbsoluteDate<T> minDate;

    /** Last date of the range. */
    private final FieldAbsoluteDate<T> maxDate;

    /** Underlying raw mathematical model. */
    private FieldDenseOutputModel<T> model;

    /** Unmanaged additional states that must be simply copied. */
    private final FieldArrayDictionary<T> unmanaged;

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
     * @param providers generators for pre-integrated states
     * @param equations names of additional equations
     * @param dimensions dimensions of additional equations
     * @since 11.2
     */
    public FieldIntegratedEphemeris(final FieldAbsoluteDate<T> startDate,
                                    final FieldAbsoluteDate<T> minDate, final FieldAbsoluteDate<T> maxDate,
                                    final FieldStateMapper<T> mapper, final PropagationType type,
                                    final FieldDenseOutputModel<T> model,
                                    final FieldArrayDictionary<T> unmanaged,
                                    final List<FieldAdditionalStateProvider<T>> providers,
                                    final String[] equations, final int[] dimensions) {

        super(startDate.getField(), mapper.getAttitudeProvider());

        this.startDate = startDate;
        this.minDate   = minDate;
        this.maxDate   = maxDate;
        this.mapper    = mapper;
        this.type      = type;
        this.model     = model;
        this.unmanaged = unmanaged;

        // set up the pre-integrated providers
        for (final FieldAdditionalStateProvider<T> provider : providers) {
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
          * of supported range
     */
    private FieldODEStateAndDerivative<T> getInterpolatedState(final FieldAbsoluteDate<T> date) {

        // compare using double precision instead of FieldAbsoluteDate<T>.compareTo(...)
        // because time is expressed as a double when searching for events
        if (date.compareTo(minDate.shiftedBy(-EXTRAPOLATION_TOLERANCE)) < 0) {
            // date is outside of supported range
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_BEFORE,
                    date, minDate, maxDate, minDate.durationFrom(date).getReal());
        }
        if (date.compareTo(maxDate.shiftedBy(EXTRAPOLATION_TOLERANCE)) > 0) {
            // date is outside of supported range
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_AFTER,
                    date, minDate, maxDate, date.durationFrom(maxDate).getReal());
        }

        return model.getInterpolatedState(date.durationFrom(startDate));

    }

    /** {@inheritDoc} */
    @Override
    protected FieldSpacecraftState<T> basicPropagate(final FieldAbsoluteDate<T> date) {
        final FieldODEStateAndDerivative<T> os = getInterpolatedState(date);
        FieldSpacecraftState<T> state = mapper.mapArrayToState(mapper.mapDoubleToDate(os.getTime(), date),
                                                               os.getPrimaryState(), os.getPrimaryDerivative(),
                                                               type);
        for (FieldArrayDictionary<T>.Entry initial : unmanaged.getData()) {
            state = state.addAdditionalState(initial.getKey(), initial.getValue());
        }
        return state;
    }

    /** {@inheritDoc} */
    @Override
    protected FieldOrbit<T> propagateOrbit(final FieldAbsoluteDate<T> date, final T[] parameters) {
        return basicPropagate(date).getOrbit();
    }

    /** {@inheritDoc} */
    @Override
    protected T getMass(final FieldAbsoluteDate<T> date) {
        return basicPropagate(date).getMass();
    }

    /** {@inheritDoc} */
    @Override
    public TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date, final Frame frame) {
        return propagate(date).getPVCoordinates(frame);
    }

    /** Get the first date of the range.
     * @return the first date of the range
     */
    @Override
    public FieldAbsoluteDate<T> getMinDate() {
        return minDate;
    }

    /** Get the last date of the range.
     * @return the last date of the range
     */
    @Override
    public FieldAbsoluteDate<T> getMaxDate() {
        return maxDate;
    }

    @Override
    public Frame getFrame() {
        return this.mapper.getFrame();
    }

    /** {@inheritDoc} */
    @Override
    public void resetInitialState(final FieldSpacecraftState<T> state) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    @Override
    protected void resetIntermediateState(final FieldSpacecraftState<T> state, final boolean forward) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    @Override
    public FieldSpacecraftState<T> getInitialState() {
        return updateAdditionalStates(basicPropagate(getMinDate()));
    }

    /** {@inheritDoc} */
    @Override
    protected FieldSpacecraftState<T> updateAdditionalStates(final FieldSpacecraftState<T> original) {

        FieldSpacecraftState<T> updated = super.updateAdditionalStates(original);

        if (equations.length > 0) {
            final FieldODEStateAndDerivative<T> osd                = getInterpolatedState(updated.getDate());
            final T[]                           combinedState      = osd.getSecondaryState(1);
            final T[]                           combinedDerivative = osd.getSecondaryDerivative(1);
            int index = 0;
            for (int i = 0; i < equations.length; ++i) {
                final T[] state      = Arrays.copyOfRange(combinedState,      index, index + dimensions[i]);
                final T[] derivative = Arrays.copyOfRange(combinedDerivative, index, index + dimensions[i]);
                updated = updated.
                          addAdditionalState(equations[i], state).
                          addAdditionalStateDerivative(equations[i], derivative);
                index += dimensions[i];
            }
        }

        return updated;

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        // Integrated Ephemeris propagation model does not have parameter drivers.
        return Collections.emptyList();
    }

}

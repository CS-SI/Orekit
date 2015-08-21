/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.propagation.integration;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.ode.ContinuousOutputModel;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/** This class stores sequentially generated orbital parameters for
 * later retrieval.
 *
 * <p>
 * Instances of this class are built and then must be fed with the results
 * provided by {@link org.orekit.propagation.Propagator Propagator} objects
 * configured in {@link org.orekit.propagation.Propagator#setEphemerisMode()
 * ephemeris generation mode}. Once propagation is o, random access to any
 * intermediate state of the orbit throughout the propagation range is possible.
 * </p>
 * <p>
 * A typical use case is for numerically integrated orbits, which can be used by
 * algorithms that need to wander around according to their own algorithm without
 * cumbersome tight links with the integrator.
 * </p>
 * <p>
 * Another use case is persistence, as this class is one of the few propagators
 * to be serializable.
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
    extends AbstractAnalyticalPropagator implements BoundedPropagator, Serializable  {

    /** Serializable UID. */
    private static final long serialVersionUID = 20140213L;

    /** Mapper between raw double components and spacecraft state. */
    private final StateMapper mapper;

    /** Output only the mean orbit. <br/>
     * <p>
     * This is used only in the case of semianalitical propagators where there is a clear separation between
     * mean and short periodic elements. It is ignored by the Numerical propagator.
     * </p>
     */
    private boolean meanOrbit;

    /** Start date of the integration (can be min or max). */
    private final AbsoluteDate startDate;

    /** First date of the range. */
    private final AbsoluteDate minDate;

    /** Last date of the range. */
    private final AbsoluteDate maxDate;

    /** Underlying raw mathematical model. */
    private ContinuousOutputModel model;

    /** Unmanaged additional states that must be simply copied. */
    private final Map<String, double[]> unmanaged;

    /** Creates a new instance of IntegratedEphemeris.
     * @param startDate Start date of the integration (can be minDate or maxDate)
     * @param minDate first date of the range
     * @param maxDate last date of the range
     * @param mapper mapper between raw double components and spacecraft state
     * @param meanOrbit output only the mean orbit
     * @param model underlying raw mathematical model
     * @param unmanaged unmanaged additional states that must be simply copied
     * @param providers providers for pre-integrated states
     * @param equations names of additional equations
     * @exception OrekitException if several providers have the same name
     */
    public IntegratedEphemeris(final AbsoluteDate startDate,
                               final AbsoluteDate minDate, final AbsoluteDate maxDate,
                               final StateMapper mapper, final boolean meanOrbit,
                               final ContinuousOutputModel model,
                               final Map<String, double[]> unmanaged,
                               final List<AdditionalStateProvider> providers,
                               final String[] equations)
        throws OrekitException {

        super(mapper.getAttitudeProvider());

        this.startDate = startDate;
        this.minDate   = minDate;
        this.maxDate   = maxDate;
        this.mapper    = mapper;
        this.meanOrbit = meanOrbit;
        this.model     = model;
        this.unmanaged = unmanaged;

        // set up the pre-integrated providers
        for (final AdditionalStateProvider provider : providers) {
            addAdditionalStateProvider(provider);
        }

        // set up providers to map the final elements of the model array to additional states
        for (int i = 0; i < equations.length; ++i) {
            addAdditionalStateProvider(new LocalProvider(equations[i], i));
        }

    }

    /** Set up the model at some interpolation date.
     * @param date desired interpolation date
     * @exception PropagationException if specified date is outside
     * of supported range
     */
    private void setInterpolationDate(final AbsoluteDate date)
        throws PropagationException {

        if (date.equals(startDate.shiftedBy(model.getInterpolatedTime()))) {
            // the current model date is already the desired one
            return;
        }

        if ((date.compareTo(minDate) < 0) || (date.compareTo(maxDate) > 0)) {
            // date is outside of supported range
            throw new PropagationException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE,
                                           date, minDate, maxDate);
        }

        // reset interpolation model to the desired date
        model.setInterpolatedTime(date.durationFrom(startDate));

    }

    /** {@inheritDoc} */
    @Override
    protected SpacecraftState basicPropagate(final AbsoluteDate date)
        throws PropagationException {
        try {
            setInterpolationDate(date);
            SpacecraftState state = mapper.mapArrayToState(model.getInterpolatedTime(),
                                                           model.getInterpolatedState(),
                                                           meanOrbit);
            for (Map.Entry<String, double[]> initial : unmanaged.entrySet()) {
                state = state.addAdditionalState(initial.getKey(), initial.getValue());
            }
            return state;
        } catch (OrekitExceptionWrapper oew) {
            if (oew.getException() instanceof PropagationException) {
                throw (PropagationException) oew.getException();
            } else {
                throw new PropagationException(oew.getException());
            }
        } catch (OrekitException oe) {
            if (oe instanceof PropagationException) {
                throw (PropagationException) oe;
            } else {
                throw new PropagationException(oe);
            }
        }
    }

    /** {@inheritDoc} */
    protected Orbit propagateOrbit(final AbsoluteDate date)
        throws PropagationException {
        return basicPropagate(date).getOrbit();
    }

    /** {@inheritDoc} */
    protected double getMass(final AbsoluteDate date) throws PropagationException {
        return basicPropagate(date).getMass();
    }

    /** {@inheritDoc} */
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
        throws OrekitException {
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
    public void resetInitialState(final SpacecraftState state)
        throws PropagationException {
        throw new PropagationException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    public SpacecraftState getInitialState() throws PropagationException {
        return updateAdditionalStates(basicPropagate(getMinDate()));
    }

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     * @exception NotSerializableException if the state mapper cannot be serialized (typically for DSST propagator)
     */
    private Object writeReplace() throws NotSerializableException {

        // unmanaged additional states
        final String[]   unmanagedNames  = new String[unmanaged.size()];
        final double[][] unmanagedValues = new double[unmanaged.size()][];
        int i = 0;
        for (Map.Entry<String, double[]> entry : unmanaged.entrySet()) {
            unmanagedNames[i]  = entry.getKey();
            unmanagedValues[i] = entry.getValue();
            ++i;
        }

        // managed states providers
        final List<AdditionalStateProvider> serializableProviders = new ArrayList<AdditionalStateProvider>();
        final List<String> equationNames = new ArrayList<String>();
        for (final AdditionalStateProvider provider : getAdditionalStateProviders()) {
            if (provider instanceof LocalProvider) {
                equationNames.add(((LocalProvider) provider).getName());
            } else if (provider instanceof Serializable) {
                serializableProviders.add(provider);
            }
        }

        return new DataTransferObject(startDate, minDate, maxDate, mapper, meanOrbit, model,
                                      unmanagedNames, unmanagedValues,
                                      serializableProviders.toArray(new AdditionalStateProvider[serializableProviders.size()]),
                                      equationNames.toArray(new String[equationNames.size()]));

    }

    /** Local provider for additional state data. */
    private class LocalProvider implements AdditionalStateProvider {

        /** Name of the additional state. */
        private final String name;

        /** Index of the additional state. */
        private final int index;

        /** Simple constructor.
         * @param name name of the additional state
         * @param index index of the additional state
         */
        LocalProvider(final String name, final int index) {
            this.name  = name;
            this.index = index;
        }

        /** {@inheritDoc} */
        public String getName() {
            return name;
        }

        /** {@inheritDoc} */
        public double[] getAdditionalState(final SpacecraftState state)
            throws PropagationException {

            // set the model date
            setInterpolationDate(state.getDate());

            // extract the part of the interpolated array corresponding to the additional state
            return model.getInterpolatedSecondaryState(index);

        }

    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20140213L;

        /** Mapper between raw double components and spacecraft state. */
        private final StateMapper mapper;

        /** Indicator for mean orbit output. */
        private final boolean meanOrbit;

        /** Start date of the integration (can be min or max). */
        private final AbsoluteDate startDate;

        /** First date of the range. */
        private final AbsoluteDate minDate;

        /** Last date of the range. */
        private final AbsoluteDate maxDate;

        /** Underlying raw mathematical model. */
        private final ContinuousOutputModel model;

        /** Names of unmanaged additional states that must be simply copied. */
        private final String[] unmanagedNames;

        /** Values of unmanaged additional states that must be simply copied. */
        private final double[][] unmanagedValues;

        /** Names of additional equations. */
        private final String[] equations;

        /** Providers for pre-integrated states. */
        private final AdditionalStateProvider[] providers;

        /** Simple constructor.
         * @param startDate Start date of the integration (can be minDate or maxDate)
         * @param minDate first date of the range
         * @param maxDate last date of the range
         * @param mapper mapper between raw double components and spacecraft state
         * @param meanOrbit output only the mean orbit.
         * @param model underlying raw mathematical model
         * @param unmanagedNames names of unmanaged additional states that must be simply copied
         * @param unmanagedValues values of unmanaged additional states that must be simply copied
         * @param providers providers for pre-integrated states
         * @param equations names of additional equations
         */
        DataTransferObject(final AbsoluteDate startDate,
                                  final AbsoluteDate minDate, final AbsoluteDate maxDate,
                                  final StateMapper mapper, final boolean meanOrbit,
                                  final ContinuousOutputModel model,
                                  final String[] unmanagedNames, final double[][] unmanagedValues,
                                  final AdditionalStateProvider[] providers,
                                  final String[] equations) {
            this.startDate       = startDate;
            this.minDate         = minDate;
            this.maxDate         = maxDate;
            this.mapper          = mapper;
            this.meanOrbit       = meanOrbit;
            this.model           = model;
            this.unmanagedNames  = unmanagedNames;
            this.unmanagedValues = unmanagedValues;
            this.providers       = providers;
            this.equations       = equations;
        }

        /** Replace the deserialized data transfer object with a {@link IntegratedEphemeris}.
         * @return replacement {@link IntegratedEphemeris}
         */
        private Object readResolve() {
            try {
                final Map<String, double[]> unmanaged = new HashMap<String, double[]>(unmanagedNames.length);
                for (int i = 0; i < unmanagedNames.length; ++i) {
                    unmanaged.put(unmanagedNames[i], unmanagedValues[i]);
                }
                return new IntegratedEphemeris(startDate, minDate, maxDate, mapper, meanOrbit, model,
                                               unmanaged, Arrays.asList(providers), equations);
            } catch (OrekitException oe) {
                throw new OrekitInternalError(oe);
            }
        }

    }

}

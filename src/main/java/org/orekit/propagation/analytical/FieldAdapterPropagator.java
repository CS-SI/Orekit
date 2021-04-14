/* Copyright 2002-2021 CS GROUP
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
package org.orekit.propagation.analytical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

/**
 * Field Orbit propagator that adapts an underlying propagator, adding
 * {@link FieldDifferentialEffect differential effects}.
 * <p>
 * This propagator is used when a reference propagator does not handle some
 * effects that we need. A typical example would be an ephemeris that was
 * computed for a reference orbit, and we want to compute a station-keeping
 * maneuver on top of this ephemeris, changing its final state. The principal is
 * to add one or more
 * {@link org.orekit.forces.maneuvers.FieldSmallManeuverAnalyticalModel small
 * maneuvers analytical models} to it and use it as a new propagator, which
 * takes the maneuvers into account.
 * </p>
 * <p>
 * From a space flight dynamics point of view, this is a differential correction
 * approach. From a computer science point of view, this is a use of the
 * decorator design pattern.
 * </p>
 * 
 * @see FieldPropagator
 * @see org.orekit.forces.maneuvers.FieldSmallManeuverAnalyticalModel
 * @author Luc Maisonobe
 * @author Nicolas Fialton (field translation)
 */
public class FieldAdapterPropagator<T extends RealFieldElement<T>> extends FieldAbstractAnalyticalPropagator<T> {
	/** Interface for orbit differential effects. */
    public interface FieldDifferentialEffect<T extends RealFieldElement<T>>  {

        /** Apply the effect to a {@link FieldSpacecraftState spacecraft state}.
         * <p>
         * Applying the effect may be a no-op in some cases. A typical example
         * is maneuvers, for which the state is changed only for time <em>after</em>
         * the maneuver occurrence.
         * </p>
         * @param original original state <em>without</em> the effect
         * @return updated state at the same date, taking the effect
         * into account if meaningful
         */
        FieldSpacecraftState<T> apply(FieldSpacecraftState<T> original);

    }

    /** Underlying reference propagator. */
    private FieldPropagator<T> reference;

    /** Effects to add. */
    private List<FieldDifferentialEffect<T>> effects;

    /** Build a propagator from an underlying reference propagator.
     * <p>The reference propagator can be almost anything, numerical,
     * analytical, and even an ephemeris. It may already take some maneuvers
     * into account.</p>
     * @param field
     * @param reference reference propagator
     */
    public FieldAdapterPropagator(Field<T> field, final FieldPropagator<T> reference) {
        super(field, reference.getAttitudeProvider());
        this.reference = reference;
        this.effects = new ArrayList<FieldDifferentialEffect<T>>();
    }

    /** Add a differential effect.
     * @param effect differential effect
     */
    public void addEffect(final FieldDifferentialEffect<T> effect) {
        effects.add(effect);
    }

    /** Get the reference propagator.
     * @return reference propagator
     */
    public FieldPropagator<T> getPropagator() {
        return reference;
    }

    /** Get the differential effects.
     * @return differential effects models, as an unmodifiable list
     */
    public List<FieldDifferentialEffect<T>> getEffects() {
        return Collections.unmodifiableList(effects);
    }

    /** {@inheritDoc} */
    public FieldSpacecraftState<T> getInitialState() {
        return reference.getInitialState();
    }

    /** {@inheritDoc} */
    @Override
    public void resetInitialState(final FieldSpacecraftState<T> state) {
        reference.resetInitialState(state);
    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final FieldSpacecraftState<T> state, final boolean forward) {
        if (reference instanceof FieldAbstractAnalyticalPropagator<?>) {
            ((FieldAbstractAnalyticalPropagator<T>) reference).resetIntermediateState(state, forward);
        } else {
            throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected FieldSpacecraftState<T> basicPropagate(final FieldAbsoluteDate<T> date) {

        // compute reference state
        FieldSpacecraftState<T> state = reference.propagate(date);
        final Map<String, T[]> before = state.getAdditionalStates();

        // add all the effects
        for (final FieldDifferentialEffect<T> effect : effects) {
            state = effect.apply(state);
        }

        // forward additional states from the reference propagator
        for (final Map.Entry<String, T[]> entry : before.entrySet()) {
            if (!state.hasAdditionalState(entry.getKey())) {
                state = state.addAdditionalState(entry.getKey(), entry.getValue());
            }
        }

        return state;

    }

    /** {@inheritDoc} */
    protected FieldOrbit<T> propagateOrbit(final FieldAbsoluteDate<T> date) {
        return basicPropagate(date).getOrbit();
    }

    /** {@inheritDoc}*/
    protected T getMass(final FieldAbsoluteDate<T> date) {
        return basicPropagate(date).getMass();
    }

	@Override
	protected FieldOrbit<T> propagateOrbit(FieldAbsoluteDate<T> date, T[] parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	/** Get the parameters driver for the Field SBAS propagation model.
     * @return an empty list.
     */
	@Override
	protected List<ParameterDriver> getParametersDrivers() {
		// The Field Adapter propagation model does not have parameter drivers.
        return Collections.emptyList();
	}

}

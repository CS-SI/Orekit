/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.propagation.analytical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.errors.OrekitMessages;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Orbit propagator that adapts an underlying propagator, adding {@link
 * DifferentialEffect differential effects}.
 * <p>
 * This propagator is used when a reference propagator does not handle
 * some effects that we need. A typical example would be an ephemeris
 * that was computed for a reference orbit, and we want to compute a
 * station-keeping maneuver on top of this ephemeris, changing its
 * final state. The principal is to add one or more {@link
 * org.orekit.forces.maneuvers.SmallManeuverAnalyticalModel small maneuvers
 * analytical models} to it and use it as a new propagator, which takes the
 * maneuvers into account.
 * </p>
 * <p>
 * From a space flight dynamics point of view, this is a differential
 * correction approach. From a computer science point of view, this is
 * a use of the decorator design pattern.
 * </p>
 * @see Propagator
 * @see org.orekit.forces.maneuvers.SmallManeuverAnalyticalModel
 * @author Luc Maisonobe
 */
public class AdapterPropagator extends AbstractAnalyticalPropagator {

    /** Interface for orbit differential effects. */
    public interface DifferentialEffect {

        /** Apply the effect to a {@link SpacecraftState spacecraft state}.
         * <p>
         * Applying the effect may be a no-op in some cases. A typical example
         * is maneuvers, for which the state is changed only for time <em>after</em>
         * the maneuver occurrence.
         * </p>
         * @param original original state <em>without</em> the effect
         * @return updated state at the same date, taking the effect
         * into account if meaningful
         * @exception OrekitException if effect cannot be computed
         */
        SpacecraftState apply(SpacecraftState original) throws OrekitException;

    }

    /** Underlying reference propagator. */
    private Propagator reference;

    /** Effects to add. */
    private List<DifferentialEffect> effects;

    /** Build a propagator from an underlying reference propagator.
     * <p>The reference propagator can be almost anything, numerical,
     * analytical, and even an ephemeris. It may already take some maneuvers
     * into account.</p>
     * @param reference reference propagator
     */
    public AdapterPropagator(final Propagator reference) {
        super(reference.getAttitudeProvider());
        this.reference = reference;
        this.effects = new ArrayList<DifferentialEffect>();
    }

    /** Add a differential effect.
     * @param effect differential effect
     */
    public void addEffect(final DifferentialEffect effect) {
        effects.add(effect);
    }

    /** Get the reference propagator.
     * @return reference propagator
     */
    public Propagator getPropagator() {
        return reference;
    }

    /** Get the differential effects.
     * @return differential effects models, as an unmodifiable list
     */
    public List<DifferentialEffect> getEffects() {
        return Collections.unmodifiableList(effects);
    }

    /** {@inheritDoc} */
    public SpacecraftState getInitialState() throws OrekitException {
        return reference.getInitialState();
    }

    /** {@inheritDoc} */
    @Override
    public void resetInitialState(final SpacecraftState state)
        throws OrekitException {
        reference.resetInitialState(state);
    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final SpacecraftState state, final boolean forward)
        throws OrekitException {
        if (reference instanceof AbstractAnalyticalPropagator) {
            ((AbstractAnalyticalPropagator) reference).resetIntermediateState(state, forward);
        } else {
            throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected SpacecraftState basicPropagate(final AbsoluteDate date) throws OrekitException {

        try {

            // compute reference state
            SpacecraftState state = reference.propagate(date);
            final Map<String, double[]> before = state.getAdditionalStates();

            // add all the effects
            for (final DifferentialEffect effect : effects) {
                state = effect.apply(state);
            }

            // forward additional states from the reference propagator
            for (final Map.Entry<String, double[]> entry : before.entrySet()) {
                if (!state.hasAdditionalState(entry.getKey())) {
                    state = state.addAdditionalState(entry.getKey(), entry.getValue());
                }
            }

            return state;

        } catch (OrekitExceptionWrapper oew) {
            throw new OrekitException(oew.getException());
        }

    }

    /** {@inheritDoc} */
    protected Orbit propagateOrbit(final AbsoluteDate date)
        throws OrekitException {
        return basicPropagate(date).getOrbit();
    }

    /** {@inheritDoc}*/
    protected double getMass(final AbsoluteDate date)
        throws OrekitException {
        return basicPropagate(date).getMass();
    }

}

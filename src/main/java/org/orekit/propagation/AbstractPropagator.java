/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.propagation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.orekit.errors.PropagationException;
import org.orekit.time.AbsoluteDate;

/** Common handling of {@link Propagator} methods for analytical-like propagators.
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public abstract class AbstractPropagator implements Propagator {

    /** Mode-specific propagator. */
    private ModeSpecificPropagator propagator;

    /** Switching functions. */
    private final List switchingFunctions;

    /** Build a new instance.
     */
    protected AbstractPropagator() {
        switchingFunctions = new ArrayList();
        setSlaveMode();
    }

    /** {@inheritDoc} */
    public int getMode() {
        return propagator.getMode();
    }

    /** {@inheritDoc} */
    public void setSlaveMode() {
        propagator = new SlavePropagator();
    }

    /** {@inheritDoc} */
    public void setMasterMode(final double h,
                              final OrekitFixedStepHandler handler) {
        propagator = new FixedPropagator(h, handler);
    }

    /** {@inheritDoc} */
    public void setMasterMode(final OrekitStepHandler handler) {
        propagator = new VariablePropagator(handler);
    }

    /** {@inheritDoc} */
    public void setEphemerisMode() {
        propagator = new EphemerisGenerationPropagator();
    }

    /** {@inheritDoc} */
    public BoundedPropagator getGeneratedEphemeris() {
        return (EphemerisGenerationPropagator) propagator;
    }

    /** {@inheritDoc} */
    public void addSwitchingFunction(final OrekitSwitchingFunction switchingFunction) {
        switchingFunctions.add(switchingFunction);
    }

    /** {@inheritDoc} */
    public void removeSwitchingFunctions() {
        switchingFunctions.clear();
    }

    /** {@inheritDoc} */
    public SpacecraftState propagate(final AbsoluteDate date)
        throws PropagationException {
        return propagator.propagate(date);
    }

    /** Get the initial propagation date.
     * @return initial propagation date
     */
    protected abstract AbsoluteDate getInitialDate();

    /** Propagate an orbit without any fancy features.
     * <p>This method is similar in spirit to the {@link #propagate} method,
     * except that it does <strong>not</strong> call any handler during
     * propagation, nor any switching function. It always stop exactly at
     * the dpecified date.</p>
     * @param date
     * @return state at specified date
     * @exception PropagationException if propagation cannot reach specified date
     */
    protected abstract SpacecraftState basicPropagate(AbsoluteDate date)
        throws PropagationException;

    /** Reset the basic propagator initial state.
     * @param state new initial state to consider
     * @exception PropagationException if initial state cannot be reset
     */
    protected abstract void resetInitialState(SpacecraftState state)
        throws PropagationException;

    /** Mode-specific propagation interface. */
    private interface ModeSpecificPropagator extends Serializable {
        /** Get the propagation mode.
         * @return one of {@link Propagator#SLAVE_MODE}, {@link Propagator#MASTER_FIXED_MODE},
         * {@link Propagator#MASTER_VARIABLE_MODE} or {@link Propagator#EPHEMERIS_GENERATION_MODE}
         */
        int getMode();

        /** Get the spacecraft state at a specific date in some predefined mode.
         * @param date desired date for the orbit
         * @return the spacecraft state at the specified date
         * @exception PropagationException if state cannot be propagated
         */
        SpacecraftState propagate(AbsoluteDate date) throws PropagationException;

    }

    /** Propagator for slave mode. */
    private class SlavePropagator implements ModeSpecificPropagator {

        /** Serializable UID. */
        private static final long serialVersionUID = -1285661320156654761L;

        /** {@inheritDoc} */
        public int getMode() {
            return Propagator.SLAVE_MODE;
        }

        /** {@inheritDoc} */
        public SpacecraftState propagate(AbsoluteDate date)
            throws PropagationException {
            return basicPropagate(date);
        }

    }

    /** Propagator for master mode with fixed steps. */
    private class FixedPropagator implements ModeSpecificPropagator {

        /** Serializable UID. */
        private static final long serialVersionUID = -81235523448943708L;

        /** Step size. */
        private double h;

        /** Step handler. */
        private OrekitFixedStepHandler handler;

        /** Build an instance.
         * @param h step size
         * @param handler step handler
         */
        public FixedPropagator(final double h, final OrekitFixedStepHandler handler) {
            this.h       = Math.abs(h);
            this.handler = handler;
        }

        /** {@inheritDoc} */
        public int getMode() {
            return Propagator.MASTER_FIXED_MODE;
        }

        /** {@inheritDoc} */
        public SpacecraftState propagate(AbsoluteDate date)
            throws PropagationException {

            // compute the number of steps
            final AbsoluteDate start = getInitialDate();
            final double duration = date.minus(start);
            final int n = Math.max(1, (int) Math.round(Math.abs(duration) / h));
            final double signedH = (duration < 0) ? -h : h;

            // the n-1 first steps are exactly h seconds long
            for (int i = 0; i < (n - 1); ++i) {
                SpacecraftState state = basicPropagate(new AbsoluteDate(start, i * signedH));
                handler.handleStep(state, false);
            }

            // last step is exactly at the target date
            SpacecraftState state =  basicPropagate(date);
            handler.handleStep(state, true);

            return state;

        }

    }

    /** Propagator for master mode with variable steps. */
    private class VariablePropagator
        implements ModeSpecificPropagator, OrekitStepInterpolator {

        /** Serializable UID. */
        private static final long serialVersionUID = -3751614127887257329L;

        /** Previous date. */
        private AbsoluteDate previousDate;

        /** Current date. */
        private AbsoluteDate currentDate;

        /** Interpolated date. */
        private AbsoluteDate interpolatedDate;

        /** Interpolated state. */
        private SpacecraftState interpolatedState;

        /** Forward indicator. */
        private boolean forward;

        /** Step handler. */
        private OrekitStepHandler handler;

        /** Build an instance.
         * @param handler step handler
         */
        public VariablePropagator(final OrekitStepHandler handler) {
            this.handler = handler;
        }

        /** {@inheritDoc} */
        public int getMode() {
            return Propagator.MASTER_VARIABLE_MODE;
        }

        /** {@inheritDoc} */
        public SpacecraftState propagate(AbsoluteDate date)
            throws PropagationException {

            // initial parameters
            final AbsoluteDate initialDate = getInitialDate();
            setInterpolatedDate(initialDate);
            final double a  = interpolatedState.getA();
            final double mu = interpolatedState.getMu();
            final double duration = date.minus(initialDate);
            forward = (duration >= 0);
            final double period = 2.0 * Math.PI * a * Math.sqrt(a / mu);

            // compute the number of steps
            final int n = Math.max(1, (int) Math.round(Math.abs(duration) * 100 / period));
            final double signedH = duration / n;

            // all steps are exactly h seconds long
            for (int i = 0; i < n; ++i) {
                previousDate = currentDate;
                currentDate  = new AbsoluteDate(initialDate, i * signedH);
                setInterpolatedDate(currentDate);
                handler.handleStep(this, i == (n - 1));
            }

            return interpolatedState;

        }

        /** {@inheritDoc} */
        public AbsoluteDate getCurrentDate() {
            return currentDate;
        }

        /** {@inheritDoc} */
        public AbsoluteDate getInterpolatedDate() {
            return interpolatedDate;
        }

        /** {@inheritDoc} */
        public SpacecraftState getInterpolatedState() {
            return interpolatedState;
        }

        /** {@inheritDoc} */
        public AbsoluteDate getPreviousDate() {
            return previousDate;
        }

        /** {@inheritDoc} */
        public boolean isForward() {
            return forward;
        }

        /** {@inheritDoc} */
        public void setInterpolatedDate(AbsoluteDate date)
            throws PropagationException {
            interpolatedDate  = date;
            interpolatedState = basicPropagate(date);
        }

    }

    /** Propagator for ephemeris generation mode. */
    private class EphemerisGenerationPropagator
        implements ModeSpecificPropagator, BoundedPropagator {

        /** Serializable UID. */
        private static final long serialVersionUID = -3295493459759617818L;

        /** {@inheritDoc} */
        public int getMode() {
            return Propagator.EPHEMERIS_GENERATION_MODE;
        }

        /** {@inheritDoc} */
        public SpacecraftState propagate(AbsoluteDate date)
            throws PropagationException {
            return basicPropagate(date);
        }

        public AbsoluteDate getMaxDate() {
            // TODO Auto-generated method stub
            return null;
        }

        public AbsoluteDate getMinDate() {
            // TODO Auto-generated method stub
            return null;
        }

    }

}

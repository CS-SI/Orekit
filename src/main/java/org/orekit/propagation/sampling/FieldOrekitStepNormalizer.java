/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.propagation.sampling;

import org.hipparchus.CalculusFieldElement;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.FieldAbsoluteDate;

/**
 * This class wraps an object implementing {@link OrekitFixedStepHandler}
 * into a {@link OrekitStepHandler}.

 * <p>It mirrors the <code>StepNormalizer</code> interface from <a
 * href="http://commons.apache.org/math/">commons-math</a> but
 * provides a space-dynamics interface to the methods.</p>
 * @author Luc Maisonobe
 * @param <T> type of the field elements
 */
public class FieldOrekitStepNormalizer <T extends CalculusFieldElement<T>> implements FieldOrekitStepHandler<T> {

    /** Fixed time step. */
    private T h;

    /** Underlying step handler. */
    private FieldOrekitFixedStepHandler<T> handler;

    /** Last State vector. */
    private FieldSpacecraftState<T> lastState;

    /** Integration direction indicator. */
    private boolean forward;

    /** Simple constructor.
     * @param h fixed time step (sign is not used)
     * @param handler fixed time step handler to wrap
     */
    public FieldOrekitStepNormalizer(final T h, final FieldOrekitFixedStepHandler<T> handler) {
        this.h       = h.abs();
        this.handler = handler;
        lastState = null;
        forward   = true;
    }

    /** Get the fixed time step.
     * @return fixed time step
     * @since 11.0
     */
    public T getFixedTimeStep() {
        return h;
    }

    /** Get the underlying fixed step handler.
     * @return underlying fixed step handler
     * @since 11.0
     */
    public FieldOrekitFixedStepHandler<T> getFixedStepHandler() {
        return handler;
    }

    /** Determines whether this handler needs dense output.
     * This handler needs dense output in order to provide data at
     * regularly spaced steps regardless of the steps the propagator
     * uses, so this method always returns true.
     * @return always true
     */
    public boolean requiresDenseOutput() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final FieldSpacecraftState<T> s0, final FieldAbsoluteDate<T> t) {
        lastState = null;
        forward   = true;
        handler.init(s0, t, h);
    }

    /** {@inheritDoc} */
    @Override
    public void handleStep(final FieldOrekitStepInterpolator<T> interpolator) {

        if (lastState == null) {
            // initialize lastState in the first step case
            lastState = interpolator.getPreviousState();
        }
        // take the propagation direction into account
        T step = h;
        forward = interpolator.isForward();
        if (!forward) {
            step = h.multiply(-1);
        }


        // use the interpolator to push fixed steps events to the underlying handler
        FieldAbsoluteDate<T> nextTime = lastState.getDate().shiftedBy(step);
        boolean nextInStep = forward ^ nextTime.compareTo(interpolator.getCurrentState().getDate()) > 0;
        while (nextInStep) {

            // output the stored previous step
            handler.handleStep(lastState);

            // store the next step
            lastState = interpolator.getInterpolatedState(nextTime);

            // prepare next iteration
            nextTime = nextTime.shiftedBy(step);
            nextInStep = forward ^ nextTime.compareTo(interpolator.getCurrentState().getDate()) > 0;

        }
    }

    /** {@inheritDoc} */
    @Override
    public void finish(final FieldSpacecraftState<T> finalState) {

        // there will be no more steps,
        // the stored one should be handled now
        handler.handleStep(lastState);

        // and the final state handled too
        handler.finish(finalState);

    }

}

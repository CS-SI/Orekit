/* Copyright 2023 Luc Maisonobe
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
package org.orekit.propagation.sampling;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hipparchus.util.FastMath;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/**
 * This class wraps an object implementing {@link MultiSatFixedStepHandler}
 * into a {@link MultiSatStepHandler}.

 * <p>It mirrors the <code>StepNormalizer</code> interface from <a
 * href="https://hipparchus.org/">Hipparchus</a> but
 * provides a space-dynamics interface to the methods.</p>
 * @author Luc Maisonobe
 * @since 12.0
 */
public class MultisatStepNormalizer implements MultiSatStepHandler {

    /** Fixed time step. */
    private double h;

    /** Underlying fixed step handler. */
    private MultiSatFixedStepHandler handler;

    /** Last State vectors. */
    private List<SpacecraftState> lastStates;

    /** Integration direction indicator. */
    private boolean forward;

    /** Simple constructor.
     * @param h fixed time step (sign is not used)
     * @param handler fixed time step handler to wrap
     */
    public MultisatStepNormalizer(final double h, final MultiSatFixedStepHandler handler) {
        this.h          = FastMath.abs(h);
        this.handler    = handler;
        this.lastStates = null;
        this.forward    = true;
    }

    /** Get the fixed time step.
     * @return fixed time step
     */
    public double getFixedTimeStep() {
        return h;
    }

    /** Get the underlying fixed step handler.
     * @return underlying fixed step handler
     */
    public MultiSatFixedStepHandler getFixedStepHandler() {
        return handler;
    }

    /** {@inheritDoc} */
    public void init(final List<SpacecraftState> s0, final AbsoluteDate t) {
        lastStates = new ArrayList<>(s0);
        forward    = true;
        handler.init(s0, t, h);
    }

    /** {@inheritDoc} */
    public void handleStep(final List<OrekitStepInterpolator> interpolators) {

        if (lastStates == null) {
            // initialize lastState in the first step case
            lastStates = interpolators.stream().map(i -> i.getPreviousState()).collect(Collectors.toList());
        }

        // take the propagation direction into account
        double step = h;
        forward = interpolators.get(0).isForward();
        if (!forward) {
            step = -h;
        }


        // use the interpolator to push fixed steps events to the underlying handler
        AbsoluteDate nextTime = lastStates.get(0).getDate().shiftedBy(step);
        boolean nextInStep = forward ^ nextTime.compareTo(interpolators.get(0).getCurrentState().getDate()) > 0;
        while (nextInStep) {

            // output the stored previous step
            handler.handleStep(lastStates);

            // store the next step
            final AbsoluteDate time = nextTime;
            lastStates = interpolators.stream().map(i -> i.getInterpolatedState(time)).collect(Collectors.toList());

            // prepare next iteration
            nextTime = nextTime.shiftedBy(step);
            nextInStep = forward ^ nextTime.compareTo(interpolators.get(0).getCurrentState().getDate()) > 0;

        }

    }

    /** {@inheritDoc} */
    @Override
    public void finish(final List<SpacecraftState> finalStates) {

        // there will be no more steps,
        // the stored one should be handled now
        handler.handleStep(lastStates);

        // and the final state handled too
        handler.finish(finalStates);

    }

}

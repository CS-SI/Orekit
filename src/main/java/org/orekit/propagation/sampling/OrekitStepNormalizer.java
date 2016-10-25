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

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/**
 * This class wraps an object implementing {@link OrekitFixedStepHandler}
 * into a {@link OrekitStepHandler}.

 * <p>It mirrors the <code>StepNormalizer</code> interface from <a
 * href="https://hipparchus.org/">Hipparchus</a> but
 * provides a space-dynamics interface to the methods.</p>
 * @author Luc Maisonobe
 */
public class OrekitStepNormalizer implements OrekitStepHandler {

    /** Fixed time step. */
    private double h;

    /** Underlying step handler. */
    private OrekitFixedStepHandler handler;

    /** Last State vector. */
    private SpacecraftState lastState;

    /** Integration direction indicator. */
    private boolean forward;

    /** Simple constructor.
     * @param h fixed time step (sign is not used)
     * @param handler fixed time step handler to wrap
     */
    public OrekitStepNormalizer(final double h, final OrekitFixedStepHandler handler) {
        this.h         = FastMath.abs(h);
        this.handler   = handler;
        this.lastState = null;
        this.forward   = true;
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
    public void init(final SpacecraftState s0, final AbsoluteDate t)
        throws OrekitException {
        lastState = null;
        forward   = true;
        handler.init(s0, t, h);
    }

    /**
     * Handle the last accepted step.
     * @param interpolator interpolator for the last accepted step. For
     * efficiency purposes, the various propagators reuse the same
     * object on each call, so if the instance wants to keep it across
     * all calls (for example to provide at the end of the propagation a
     * continuous model valid throughout the propagation range), it
     * should build a local copy using the clone method and store this
     * copy.
     * @param isLast true if the step is the last one
     * @throws OrekitException this exception is propagated to the
     * caller if the underlying user function triggers one
     */
    public void handleStep(final OrekitStepInterpolator interpolator, final boolean isLast)
        throws OrekitException {

        if (lastState == null) {
            // initialize lastState in the first step case
            lastState = interpolator.getPreviousState();
        }

        // take the propagation direction into account
        double step = h;
        forward = interpolator.isForward();
        if (!forward) {
            step = -h;
        }


        // use the interpolator to push fixed steps events to the underlying handler
        AbsoluteDate nextTime = lastState.getDate().shiftedBy(step);
        boolean nextInStep = forward ^ (nextTime.compareTo(interpolator.getCurrentState().getDate()) > 0);
        while (nextInStep) {

            // output the stored previous step
            handler.handleStep(lastState, false);

            // store the next step
            lastState = interpolator.getInterpolatedState(nextTime);

            // prepare next iteration
            nextTime = nextTime.shiftedBy(step);
            nextInStep = forward ^ (nextTime.compareTo(interpolator.getCurrentState().getDate()) > 0);

        }

        if (isLast) {
            // there will be no more steps,
            // the stored one should be flagged as being the last
            handler.handleStep(lastState, true);
        }

    }

}

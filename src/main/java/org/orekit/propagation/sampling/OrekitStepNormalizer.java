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

import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/**
 * This class wraps an object implementing {@link OrekitFixedStepHandler}
 * into a {@link OrekitStepHandler}.

 * <p>It mirrors the <code>StepNormalizer</code> interface from <a
 * href="http://commons.apache.org/math/">commons-math</a> but
 * provides a space-dynamics interface to the methods.</p>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class OrekitStepNormalizer implements OrekitStepHandler {

    /** Serializable UID. */
    private static final long serialVersionUID = 6335110162884693078L;

    /** Fixed time step. */
    private double h;

    /** Underlying step handler. */
    private OrekitFixedStepHandler handler;

    /** Last step date. */
    private AbsoluteDate lastDate;

    /** Last State vector. */
    private SpacecraftState lastState;

    /** Integration direction indicator. */
    private boolean forward;

    /** Simple constructor.
     * @param h fixed time step (sign is not used)
     * @param handler fixed time step handler to wrap
     */
    public OrekitStepNormalizer(final double h, final OrekitFixedStepHandler handler) {
        this.h       = Math.abs(h);
        this.handler = handler;
        reset();
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

    /** Reset the step handler.
     * Initialize the internal data as required before the first step is
     * handled.
     */
    public void reset() {
        lastDate  = null;
        lastState = null;
        forward   = true;
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
     * @throws PropagationException this exception is propagated to the
     * caller if the underlying user function triggers one
     */
    public void handleStep(final OrekitStepInterpolator interpolator, final boolean isLast)
        throws PropagationException {
        try {

            if (lastState == null) {
                // initialize lastState in the first step case

                lastDate = interpolator.getPreviousDate();
                interpolator.setInterpolatedDate(lastDate);
                lastState = interpolator.getInterpolatedState();

                // take the propagation direction into account
                forward = interpolator.getCurrentDate().compareTo(lastDate) >= 0;
                if (!forward) {
                    h = -h;
                }

            }

            // use the interpolator to push fixed steps events to the underlying handler
            AbsoluteDate nextTime = lastDate.shiftedBy(h);
            boolean nextInStep = forward ^ (nextTime.compareTo(interpolator.getCurrentDate()) > 0);
            while (nextInStep) {

                // output the stored previous step
                handler.handleStep(lastState, false);

                // store the next step
                lastDate = nextTime;
                interpolator.setInterpolatedDate(lastDate);
                lastState = interpolator.getInterpolatedState();

                // prepare next iteration
                nextTime = nextTime.shiftedBy(h);
                nextInStep = forward ^ (nextTime.compareTo(interpolator.getCurrentDate()) > 0);

            }

            if (isLast) {
                // there will be no more steps,
                // the stored one should be flagged as being the last
                handler.handleStep(lastState, true);
            }

        } catch (OrekitException oe) {

            // recover a possible embedded PropagationException
            for (Throwable t = oe; t != null; t = t.getCause()) {
                if (t instanceof PropagationException) {
                    throw (PropagationException) t;
                }
            }

            throw new PropagationException(oe.getMessage(), oe);

        }
    }

}

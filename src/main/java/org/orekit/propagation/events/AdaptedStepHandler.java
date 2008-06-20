/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.propagation.events;

import java.io.Serializable;

import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.StepHandler;
import org.apache.commons.math.ode.StepInterpolator;
import org.orekit.attitudes.AttitudeLaw;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.propagation.numerical.ModeHandler;
import org.orekit.time.AbsoluteDate;

/** Adapt an {@link org.orekit.propagation.events.OrekitStepHandler}
 * to commons-math {@link StepHandler} interface.
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class AdaptedStepHandler
    implements StepHandler, ModeHandler, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -7846745534321472659L;

    /** Reference date. */
    private AbsoluteDate initializedReference;

    /** Reference frame. */
    private Frame initializedFrame;

    /** Central body attraction coefficient. */
    private double initializedMu;

    /** Attitude law. */
    private AttitudeLaw initializedAttitudeLaw;

    /** Underlying handler. */
    private final OrekitStepHandler handler;

    /** Build an instance.
     * @param handler underlying handler to wrap
     */
    public AdaptedStepHandler(final OrekitStepHandler handler) {
        this.handler = handler;
    }

    /** {@inheritDoc} */
    public void initialize(final AbsoluteDate reference, final Frame frame,
                           final double mu, final AttitudeLaw attitudeLaw) {
        this.initializedReference   = reference;
        this.initializedFrame       = frame;
        this.initializedAttitudeLaw = attitudeLaw;
        this.initializedMu          = mu;
    }

    /** {@inheritDoc} */
    public boolean requiresDenseOutput() {
        return handler.requiresDenseOutput();
    }

    /** {@inheritDoc} */
    public void reset() {
        handler.reset();
    }

    /** {@inheritDoc} */
    public void handleStep(final StepInterpolator interpolator, final boolean isLast)
        throws DerivativeException {
        try {
            final OrekitStepInterpolator orekitInterpolator =
                new AdaptedStepInterpolator(initializedReference, initializedFrame, initializedMu,
                                             initializedAttitudeLaw, interpolator);
            handler.handleStep(orekitInterpolator, isLast);
        } catch (PropagationException pe) {
            throw new DerivativeException(pe);
        }
    }

}

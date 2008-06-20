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

import org.apache.commons.math.ode.FixedStepHandler;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeLaw;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.ModeHandler;
import org.orekit.time.AbsoluteDate;

/** Adapt an {@link org.orekit.propagation.events.OrekitFixedStepHandler}
 * to commons-math {@link FixedStepHandler} interface.
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class AdaptedFixedStepHandler
    implements FixedStepHandler, ModeHandler, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -9160552720711798090L;

    /** Reference date. */
    private AbsoluteDate initializedReference;

    /** Reference frame. */
    private Frame initializedFrame;

    /** Central body attraction coefficient. */
    private double initializedMu;

    /** Attitude law. */
    private AttitudeLaw initializedAttitudeLaw;

    /** Underlying handler. */
    private final OrekitFixedStepHandler handler;

    /** Build an instance.
     * @param handler underlying handler to wrap
     */
    public AdaptedFixedStepHandler(final OrekitFixedStepHandler handler) {
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

    /** {@inheritDoc}  */
    public void handleStep(final double t, final double[] y, final boolean isLast) {
        try {
            final AbsoluteDate current = new AbsoluteDate(initializedReference, t);
            final Orbit orbit =
                new EquinoctialOrbit(y[0], y[1], y[2], y[3], y[4], y[5],
                                          EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT,
                                          initializedFrame, current, initializedMu);
            final Attitude attitude =
                initializedAttitudeLaw.getState(current, orbit.getPVCoordinates(), initializedFrame);
            final SpacecraftState state =
                new SpacecraftState(orbit, attitude, y[6]);
            handler.handleStep(state, isLast);
        } catch (OrekitException e) {
            throw new RuntimeException(e.getLocalizedMessage());
        }

    }

}

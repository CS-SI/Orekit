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
package org.orekit.propagation.conversion;

import org.orekit.errors.OrekitException;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;

/** This class converts osculating orbital elements into mean elements.
 *  <p>
 *  As this process depends on the force models used to average the orbit,
 *  a {@link Propagator} is given as input. The force models used will be
 *  those contained into the propagator. This propagator <em>must</em>
 *  support its initial state to be reset, and this initial state <em>must</em>
 *  represent some mean value. This implies that this method will not work
 *  with {@link org.orekit.propagation.analytical.tle.TLEPropagator TLE propagators}
 *  because their initial state cannot be reset, and it won't work either with
 *  {@link org.orekit.propagation.analytical.EcksteinHechlerPropagator Eckstein-Hechler
 *  propagator} as their initial state is osculating and not mean. As of 6.0, this
 *  works mainly for {@link org.orekit.propagation.semianalytical.dsst.DSSTPropagator
 *  DSST propagator}.
 *  </p>
 *  @author rdicosta
 *  @author Pascal Parraud
 */
public class OsculatingToMeanElementsConverter {

    /** Integrator maximum evaluation. */
    private static final int      MAX_EVALUATION = 1000;

    /** Initial orbit to convert. */
    private final SpacecraftState state;

    /** Number of satellite revolutions in the averaging interval. */
    private final int             satelliteRevolution;

    /** Propagator used to compute mean orbit. */
    private final Propagator      propagator;

    /** Scaling factor used for orbital parameters normalization. */
    private double positionScale;

    /** Constructor.
     *  @param state initial orbit to convert
     *  @param satelliteRevolution number of satellite revolutions in the averaging interval
     *  @param propagator propagator used to compute mean orbit
     *  @param positionScale scaling factor used for orbital parameters normalization
     *  (typically set to the expected standard deviation of the position)
     */
    public OsculatingToMeanElementsConverter(final SpacecraftState state,
                                             final int satelliteRevolution,
                                             final Propagator propagator,
                                             final double positionScale) {
        this.state = state;
        this.satelliteRevolution = satelliteRevolution;
        this.propagator = propagator;
        this.positionScale = positionScale;
    }

    /** Convert an osculating orbit into a mean orbit, in DSST sense.
     *  @return mean orbit state, in DSST sense
     *  @throws OrekitException if state cannot be propagated throughout range
     */
    public final SpacecraftState convert() throws OrekitException {

        final double timeSpan = state.getKeplerianPeriod() * satelliteRevolution;
        propagator.resetInitialState(state);
        final FiniteDifferencePropagatorConverter converter =
                new FiniteDifferencePropagatorConverter(new KeplerianPropagatorBuilder(state.getOrbit(),
                                                                                       PositionAngle.MEAN,
                                                                                       positionScale),
                                                        1.e-6, MAX_EVALUATION);
        final Propagator prop = converter.convert(propagator, timeSpan, satelliteRevolution * 36);
        return prop.getInitialState();
    }
}

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
import org.orekit.frames.Frame;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;

/** Builder for Keplerian propagator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class KeplerianPropagatorBuilder extends AbstractPropagatorBuilder {

    /** Build a new instance.
     * @param mu central attraction coefficient (m³/s²)
     * @param frame the frame in which the orbit is propagated
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @deprecated as of 7.1, replaced with {@link #KeplerianPropagatorBuilder(double,
     * Frame, OrbitType, PositionAngle)}
     */
    @Deprecated
    public KeplerianPropagatorBuilder(final double mu, final Frame frame) {
        this(mu, frame, OrbitType.KEPLERIAN, PositionAngle.TRUE);
    }

    /** Build a new instance.
     * @param mu central attraction coefficient (m³/s²)
     * @param frame the frame in which the orbit is propagated
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param orbitType orbit type to use
     * @param positionAngle position angle type to use
     * @since 7.1
     */
    public KeplerianPropagatorBuilder(final double mu, final Frame frame,
                                      final OrbitType orbitType, final PositionAngle positionAngle) {
        super(frame, mu, orbitType, positionAngle);
    }

    /** {@inheritDoc} */
    public Propagator buildPropagator(final AbsoluteDate date, final double[] parameters)
        throws OrekitException {
        checkParameters(parameters);
        return new KeplerianPropagator(createInitialOrbit(date, parameters));
    }

}

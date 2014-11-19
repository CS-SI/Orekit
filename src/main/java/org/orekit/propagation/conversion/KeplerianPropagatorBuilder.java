/* Copyright 2002-2014 CS Systèmes d'Information
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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.AbstractParameterizable;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** Builder for Keplerian propagator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class KeplerianPropagatorBuilder extends AbstractParameterizable
                                        implements PropagatorBuilder {

    /** Central attraction coefficient (m³/s²). */
    private final double mu;

    /** Frame in which the orbit is propagated. */
    private final Frame frame;

    /** List of the free parameters names. */
    private Collection<String> freeParameters;

    /** Build a new instance.
     * @param mu central attraction coefficient (m³/s²)
     * @param frame the frame in which the orbit is propagated
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     */
    public KeplerianPropagatorBuilder(final double mu,
                                      final Frame frame) {
        this.mu    = mu;
        this.frame = frame;
    }

    /** {@inheritDoc} */
    public Propagator buildPropagator(final AbsoluteDate date, final double[] parameters)
        throws OrekitException {

        if (parameters.length != (freeParameters.size() + 6)) {
            throw OrekitException.createIllegalArgumentException(LocalizedFormats.DIMENSIONS_MISMATCH);
        }

        final KeplerianOrbit orb = new KeplerianOrbit(new PVCoordinates(new Vector3D(parameters[0],
                                                                                     parameters[1],
                                                                                     parameters[2]),
                                                                        new Vector3D(parameters[3],
                                                                                     parameters[4],
                                                                                     parameters[5])),
                                                      frame, date, mu);

        return new KeplerianPropagator(orb);
    }

    /** {@inheritDoc} */
    public Frame getFrame() {
        return frame;
    }

    /** {@inheritDoc} */
    public void setFreeParameters(final Collection<String> parameters)
        throws IllegalArgumentException {
        freeParameters = new ArrayList<String>();
        for (String name : parameters) {
            complainIfNotSupported(name);
        }
        freeParameters.addAll(parameters);
    }

    /** {@inheritDoc} */
    public double getParameter(final String name)
        throws IllegalArgumentException {
        return 0;
    }

    /** {@inheritDoc} */
    public void setParameter(final String name, final double value)
        throws IllegalArgumentException {
    }

}

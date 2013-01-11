/* Copyright 2002-2013 CS Systèmes d'Information
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
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.orbits.CircularOrbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** Builder for Eckstein-Hechler propagator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class EcksteinHechlerPropagatorBuilder extends AbstractParameterizable
                                              implements PropagatorBuilder {

    /** Reference radius of the central body attraction model (m). */
    private final double referenceRadius;

    /** Central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private final double mu;

    /** Un-normalized zonal coefficient (about -1.08e-3 for Earth). */
    private final double c20;

    /** Un-normalized zonal coefficient (about +2.53e-6 for Earth). */
    private final double c30;

    /** Un-normalized zonal coefficient (about +1.62e-6 for Earth). */
    private final double c40;

    /** Un-normalized zonal coefficient (about +2.28e-7 for Earth). */
    private final double c50;

    /** Un-normalized zonal coefficient (about -5.41e-7 for Earth). */
    private final double c60;

    /** Frame in which the orbit is propagated. */
    private final Frame frame;

    /** List of the free parameters names. */
    private Collection<String> freeParameters;

    /** Build a new instance.
     * @param frame the frame in which the orbit is propagated
     *        (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60 un-normalized zonal coefficient (about -5.41e-7 for Earth)
     */
    public EcksteinHechlerPropagatorBuilder(final Frame frame,
                                            final double referenceRadius,
                                            final double mu,
                                            final double c20,
                                            final double c30,
                                            final double c40,
                                            final double c50,
                                            final double c60) {
        this.frame           = frame;
        this.referenceRadius = referenceRadius;
        this.mu              = mu;
        this.c20             = c20;
        this.c30             = c30;
        this.c40             = c40;
        this.c50             = c50;
        this.c60             = c60;
    }

    /** {@inheritDoc} */
    public Propagator buildPropagator(final AbsoluteDate date, final double[] parameters)
        throws PropagationException {

        if (parameters.length != (freeParameters.size() + 6)) {
            throw OrekitException.createIllegalArgumentException(LocalizedFormats.DIMENSIONS_MISMATCH);
        }

        final CircularOrbit orb = new CircularOrbit(new PVCoordinates(new Vector3D(parameters[0],
                                                                                   parameters[1],
                                                                                   parameters[2]),
                                                                      new Vector3D(parameters[3],
                                                                                   parameters[4],
                                                                                   parameters[5])),
                                                    frame, date, mu);

        return new EcksteinHechlerPropagator(orb, referenceRadius, mu, c20, c30, c40, c50, c60);
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

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;

/** Base class for propagator builders.
 * @author Pascal Parraud
 * @since 7.1
 */
public abstract class AbstractPropagatorBuilder implements PropagatorBuilder {

    /** Frame in which the orbit is propagated. */
    private final Frame frame;

    /** Central attraction coefficient (m³/s²). */
    private double mu;

    /** List of the supported parameters names. */
    private List<String> supportedParameters;

    /** List of the free parameters names. */
    private List<String> freeParameters;

    /** Orbit type to use. */
    private final OrbitType orbitType;

    /** Position angle type to use. */
    private final PositionAngle positionAngle;

    /** Build a new instance.
     * @param frame the frame in which the orbit is propagated
     *        (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param mu central attraction coefficient (m³/s²)
     * @param orbitType orbit type to use
     * @param positionAngle position angle type to use
     * @since 7.1
     */
    public AbstractPropagatorBuilder(final Frame frame, final double mu,
                                     final OrbitType orbitType, final PositionAngle positionAngle) {
        this.frame               = frame;
        this.mu                  = mu;
        this.supportedParameters = new ArrayList<String>();
        this.freeParameters      = new ArrayList<String>();
        this.orbitType           = orbitType;
        this.positionAngle       = positionAngle;
        addSupportedParameter(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT);
    }

    /** {@inheritDoc} */
    public OrbitType getOrbitType() {
        return orbitType;
    }

    /** {@inheritDoc} */
    public PositionAngle getPositionAngle() {
        return positionAngle;
    }

    /** {@inheritDoc} */
    public Frame getFrame() {
        return frame;
    }

    /** {@inheritDoc} */
    public List<String> getSupportedParameters() {
        return Collections.unmodifiableList(supportedParameters);
    }

    /** {@inheritDoc} */
    public void setFreeParameters(final List<String> parameters)
        throws OrekitIllegalArgumentException {
        for (String name : parameters) {
            if (!supportedParameters.contains(name)) {
                throw new OrekitIllegalArgumentException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME,
                                                         name, supportedAsString());
            }
        }
        freeParameters = new ArrayList<String>(parameters);
    }

    /** {@inheritDoc} */
    public List<String> getFreeParameters() {
        return Collections.unmodifiableList(freeParameters);
    }

    /** {@inheritDoc}
     * <p>
     * The abstract base class only supports {@link
     * NewtonianAttraction#CENTRAL_ATTRACTION_COEFFICIENT}, specialized propagator
     * builders may support more parameters.
     * </p>
     */
    public double getParameter(final String name)
        throws OrekitIllegalArgumentException {
        if (NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT.equals(name)) {
            return mu;
        }
        throw new OrekitIllegalArgumentException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME,
                                                 name, supportedAsString());
    }

    /** {@inheritDoc}
     * <p>
     * The abstract base class only supports {@link
     * NewtonianAttraction#CENTRAL_ATTRACTION_COEFFICIENT}, specialized propagator
     * builders may support more parameters.
     * </p>
     */
    public void setParameter(final String name, final double value)
        throws OrekitIllegalArgumentException {
        if (NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT.equals(name)) {
            this.mu = value;
        } else {
            throw new OrekitIllegalArgumentException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME,
                                                     name, supportedAsString());
        }
    }

    /** Check the size of the parameters array.
     * @param parameters to configure the propagator
     * @exception OrekitIllegalArgumentException if the number of
     * parameters is not 6 (for initial state) plus the number of free
     * parameters
     */
    protected void checkParameters(final double[] parameters)
        throws OrekitIllegalArgumentException {
        if (parameters.length != (freeParameters.size() + 6)) {
            throw new OrekitIllegalArgumentException(LocalizedFormats.DIMENSIONS_MISMATCH_SIMPLE,
                                                     parameters.length, freeParameters.size() + 6);
        }
    }

    /** Crate the orbit for the first 6 parameters.
     * @param date date associated to the parameters to configure the initial state
     * @param parameters set of position/velocity(/free) parameters to configure the propagator
     * @return initial orbit
     */
    protected Orbit createInitialOrbit(final AbsoluteDate date, final double[] parameters) {
        return getOrbitType().mapArrayToOrbit(parameters, positionAngle, date, mu, frame);
    }

    /** Add a supported parameter name.
     * @param name name of a supported parameter
     * @exception OrekitIllegalArgumentException if the name is already supported
     */
    protected void addSupportedParameter(final String name)
        throws OrekitIllegalArgumentException {
        if (supportedParameters.contains(name)) {
            throw new OrekitIllegalArgumentException(OrekitMessages.DUPLICATED_PARAMETER_NAME, name);
        }
        supportedParameters.add(name);
    }

    /** Create a string with the list of supported parameters.
     * @return string with the list of supported parameters
     */
    private String supportedAsString() {
        final StringBuilder supported = new StringBuilder();
        for (final String name : supportedParameters) {
            if (supported.length() > 0) {
                supported.append(", ");
            }
            supported.append(name);
        }
        return supported.toString();
    }

}

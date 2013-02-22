/* Copyright 2010-2011 Centre National d'Études Spatiales
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
package org.orekit.forces.gravity;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.AbstractParameterizable;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.RotationDS;
import org.orekit.utils.Vector3DDS;

/** Force model for Newtonian central body attraction.
 * @author Luc Maisonobe
 */
public class NewtonianAttraction extends AbstractParameterizable implements ForceModel {

    /** Name of the single parameter of this model: the central attraction coefficient. */
    public static final String CENTRAL_ATTRACTION_COEFFICIENT = "central attraction coefficient";

    /** Central attraction coefficient (m^3/s^2). */
    private double mu;

   /** Simple constructor.
     * @param mu central attraction coefficient (m^3/s^2)
     */
    public NewtonianAttraction(final double mu) {
        super(CENTRAL_ATTRACTION_COEFFICIENT);
        this.mu = mu;
    }

    /** {@inheritDoc} */
    public Vector3DDS accelerationDerivatives(final AbsoluteDate date, final Frame frame,
                                              final Vector3DDS position, final Vector3DDS velocity,
                                              final RotationDS rotation, final DerivativeStructure mass)
        throws OrekitException {

        final DerivativeStructure r2 = position.getNormSq();
        return new Vector3DDS(r2.sqrt().multiply(r2).reciprocal().multiply(-mu), position);

    }

    /** {@inheritDoc} */
    public Vector3DDS accelerationDerivatives(final SpacecraftState s, final String paramName)
        throws OrekitException {

        complainIfNotSupported(paramName);

        final Vector3D            position = s.getPVCoordinates().getPosition();
        final double              r2       = position.getNormSq();
        final DerivativeStructure muds     = new DerivativeStructure(1, 1, 0, mu);
        return new Vector3DDS(muds.divide(-r2 * FastMath.sqrt(r2)), position);

    }

    /** Get the central attraction coefficient &mu;.
     * @return mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     */
    public double getMu() {
        return mu;
    }

    /** {@inheritDoc} */
    public double getParameter(final String name)
        throws IllegalArgumentException {
        complainIfNotSupported(name);
        return mu;
    }

    /** {@inheritDoc} */
    public void setParameter(final String name, final double value)
        throws IllegalArgumentException {
        complainIfNotSupported(name);
        this.mu = value;
    }

    /** {@inheritDoc} */
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder)
        throws OrekitException {
        adder.addKeplerContribution(mu);
    }

    /** {@inheritDoc} */
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[0];
    }

}


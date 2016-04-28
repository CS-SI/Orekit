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

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.AbstractParameterizable;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;

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
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final AbsoluteDate date, final Frame frame,
                                                                      final FieldVector3D<DerivativeStructure> position, final FieldVector3D<DerivativeStructure> velocity,
                                                                      final FieldRotation<DerivativeStructure> rotation, final DerivativeStructure mass)
        throws OrekitException {

        final DerivativeStructure r2 = position.getNormSq();
        return new FieldVector3D<DerivativeStructure>(r2.sqrt().multiply(r2).reciprocal().multiply(-mu), position);

    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final SpacecraftState s, final String paramName)
        throws OrekitException {

        complainIfNotSupported(paramName);

        final Vector3D            position = s.getPVCoordinates().getPosition();
        final double              r2       = position.getNormSq();
        final DerivativeStructure muds     = new DerivativeStructure(1, 1, 0, mu);
        return new FieldVector3D<DerivativeStructure>(muds.divide(-r2 * FastMath.sqrt(r2)), position);

    }

    /** Get the central attraction coefficient μ.
     * @return mu central attraction coefficient (m³/s²)
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


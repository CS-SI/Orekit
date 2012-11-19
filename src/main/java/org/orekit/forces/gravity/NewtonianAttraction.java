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

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.AbstractParameterizable;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.AccelerationJacobiansProvider;
import org.orekit.propagation.numerical.TimeDerivativesEquations;

/** Force model for Newtonian central body attraction.
 * @author Luc Maisonobe
 */
public class NewtonianAttraction extends AbstractParameterizable
                                 implements AccelerationJacobiansProvider, ForceModel {

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
    public void addDAccDState(final SpacecraftState s,
                              final double[][] dAccdPos, final double[][] dAccdVel, final double[] dAccdM)
        throws OrekitException {

        final Vector3D position     = s.getPVCoordinates().getPosition();
        final double r2             = position.getNormSq();
        final Vector3D acceleration = new Vector3D(-mu / (r2 * FastMath.sqrt(r2)), position);

        final double x2 = position.getX() * position.getX();
        final double y2 = position.getY() * position.getY();
        final double z2 = position.getZ() * position.getZ();
        final double xy = position.getX() * position.getY();
        final double yz = position.getY() * position.getZ();
        final double zx = position.getZ() * position.getX();
        final double prefix = -Vector3D.dotProduct(acceleration, position) / (r2 * r2);

        // the only non-null contribution for this force is on dAcc/dPos
        dAccdPos[0][0] += prefix * (2 * x2 - y2 - z2);
        dAccdPos[0][1] += prefix * 3 * xy;
        dAccdPos[0][2] += prefix * 3 * zx;
        dAccdPos[1][0] += prefix * 3 * xy;
        dAccdPos[1][1] += prefix * (2 * y2 - z2 - x2);
        dAccdPos[1][2] += prefix * 3 * yz;
        dAccdPos[2][0] += prefix * 3 * zx;
        dAccdPos[2][1] += prefix * 3 * yz;
        dAccdPos[2][2] += prefix * (2 * z2 - x2 - y2);

    }

    /** {@inheritDoc} */
    public void addDAccDParam(final SpacecraftState s, final String paramName, final double[] dAccdParam)
        throws OrekitException {
        complainIfNotSupported(paramName);
        final Vector3D position = s.getPVCoordinates().getPosition();
        final double r2         = position.getNormSq();
        final double factor     = -1.0 / (r2 * FastMath.sqrt(r2));
        dAccdParam[0] += factor * position.getX();
        dAccdParam[1] += factor * position.getY();
        dAccdParam[2] += factor * position.getZ();
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


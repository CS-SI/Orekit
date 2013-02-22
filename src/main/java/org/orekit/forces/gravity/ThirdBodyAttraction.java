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
package org.orekit.forces.gravity;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.AbstractParameterizable;
import org.apache.commons.math3.util.FastMath;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.RotationDS;
import org.orekit.utils.Vector3DDS;

/** Third body attraction force model.
 *
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class ThirdBodyAttraction extends AbstractParameterizable implements ForceModel {

    /** The body to consider. */
    private final CelestialBody body;

    /** Local value for body attraction coefficient. */
    private double gm;

    /** Simple constructor.
     * @param body the third body to consider
     * (ex: {@link org.orekit.bodies.CelestialBodyFactory#getSun()} or
     * {@link org.orekit.bodies.CelestialBodyFactory#getMoon()})
     */
    public ThirdBodyAttraction(final CelestialBody body) {
        super(body.getName() + " attraction coefficient");
        this.body = body;
        this.gm   = body.getGM();
    }

    /** {@inheritDoc} */
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder)
        throws OrekitException {

        // compute bodies separation vectors and squared norm
        final Vector3D centralToBody = body.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
        final double r2Central       = Vector3D.dotProduct(centralToBody, centralToBody);
        final Vector3D satToBody     = centralToBody.subtract(s.getPVCoordinates().getPosition());
        final double r2Sat           = Vector3D.dotProduct(satToBody, satToBody);

        // compute relative acceleration
        final Vector3D gamma =
            new Vector3D(gm * FastMath.pow(r2Sat, -1.5), satToBody,
                        -gm * FastMath.pow(r2Central, -1.5), centralToBody);

        // add contribution to the ODE second member
        adder.addXYZAcceleration(gamma.getX(), gamma.getY(), gamma.getZ());

    }

    /** {@inheritDoc} */
    public Vector3DDS accelerationDerivatives(final AbsoluteDate date, final Frame frame,
                                              final Vector3DDS position, final Vector3DDS velocity,
                                              final RotationDS rotation, DerivativeStructure mass)
        throws OrekitException {

        // compute bodies separation vectors and squared norm
        final Vector3D centralToBody    = body.getPVCoordinates(date, frame).getPosition();
        final double r2Central          = Vector3D.dotProduct(centralToBody, centralToBody);
        final Vector3DDS satToBody      = position.subtract(centralToBody).negate();
        final DerivativeStructure r2Sat = Vector3DDS.dotProduct(satToBody, satToBody);

        // compute relative acceleration
        final Vector3DDS satAcc   = new Vector3DDS(r2Sat.pow(-1.5).multiply(gm), satToBody);
        final Vector3D centralAcc = new Vector3D(gm * FastMath.pow(r2Central, -1.5), centralToBody);
        return satAcc.subtract(centralAcc);

    }

    /** {@inheritDoc} */
    public Vector3DDS accelerationDerivatives(final SpacecraftState s, final String paramName)
        throws OrekitException {

        complainIfNotSupported(paramName);

        // compute bodies separation vectors and squared norm
        final Vector3D centralToBody = body.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
        final double r2Central       = Vector3D.dotProduct(centralToBody, centralToBody);
        final Vector3D satToBody     = centralToBody.subtract(s.getPVCoordinates().getPosition());
        final double r2Sat           = Vector3D.dotProduct(satToBody, satToBody);

        final DerivativeStructure gmds = new DerivativeStructure(1, 1, 0, gm);

        // compute relative acceleration
        return new Vector3DDS(gmds.multiply(FastMath.pow(r2Sat, -1.5)), satToBody,
                              gmds.multiply(-FastMath.pow(r2Central, -1.5)), centralToBody);

    }

    /** {@inheritDoc} */
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[0];
    }

    /** {@inheritDoc} */
    public double getParameter(final String name)
        throws IllegalArgumentException {
        complainIfNotSupported(name);
        return gm;
    }

    /** {@inheritDoc} */
    public void setParameter(final String name, final double value)
        throws IllegalArgumentException {
        complainIfNotSupported(name);
        gm = value;
    }

}

/*
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
import org.apache.commons.math3.geometry.euclidean.threed.FieldRotation;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.AbstractParameterizable;
import org.apache.commons.math3.ode.UnknownParameterException;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

/**
 * Post-Newtonian correction force due to general relativity. The main effect is the
 * precession of perigee by a few arcseconds per year.
 *
 * <p> Implemented from Montenbruck and Gill equation 3.146.
 *
 * @author Evan Ward
 * @see "Montenbruck, Oliver, and Gill, Eberhard. Satellite orbits : models, methods, and
 * applications. Berlin New York: Springer, 2000."
 */
public class Relativity extends AbstractParameterizable implements ForceModel {

    /** Earth's gravitational parameter. */
    private double gm;

    /**
     * Create a force model to add post-Newtonian acceleration corrections to an Earth
     * orbit.
     *
     * @param gm Earth's gravitational parameter.
     */
    public Relativity(final double gm) {
        super(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT);
        this.gm = gm;
    }

    @Override
    public void addContribution(final SpacecraftState s,
                                final TimeDerivativesEquations adder) throws OrekitException {

        final PVCoordinates pv = s.getPVCoordinates();
        final Vector3D p = pv.getPosition();
        final Vector3D v = pv.getVelocity();
        //radius
        final double r2 = p.getNormSq();
        final double r = FastMath.sqrt(r2);
        //speed
        final double s2 = v.getNormSq();
        final double c2 = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT;
        //eq. 3.146
        final Vector3D accel = new Vector3D(
                4 * this.gm / r - s2,
                p,
                4 * p.dotProduct(v),
                v)
                .scalarMultiply(this.gm / (r2 * r * c2));
        adder.addAcceleration(accel, s.getFrame());
    }

    @Override
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(
            final AbsoluteDate date,
            final Frame frame,
            final FieldVector3D<DerivativeStructure> position,
            final FieldVector3D<DerivativeStructure> velocity,
            final FieldRotation<DerivativeStructure> rotation,
            final DerivativeStructure mass) {

        //radius
        final DerivativeStructure r2 = position.getNormSq();
        final DerivativeStructure r = r2.sqrt();
        //speed squared
        final DerivativeStructure s2 = velocity.getNormSq();
        final double c2 = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT;
        //eq. 3.146
        return new FieldVector3D<DerivativeStructure>(
                r.reciprocal().multiply(4 * this.gm).subtract(s2),
                position,
                position.dotProduct(velocity).multiply(4),
                velocity)
                .scalarMultiply(r2.multiply(r).multiply(c2).reciprocal().multiply(this.gm));

    }

    @Override
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(
            final SpacecraftState s,
            final String paramName) throws OrekitException {

        complainIfNotSupported(paramName);
        final DerivativeStructure gmDS = new DerivativeStructure(1, 1, 0, this.gm);

        final PVCoordinates pv = s.getPVCoordinates();
        final Vector3D p = pv.getPosition();
        final Vector3D v = pv.getVelocity();
        //radius
        final double r2 = p.getNormSq();
        final double r = FastMath.sqrt(r2);
        //speed
        final double s2 = v.getNormSq();
        final double c2 = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT;
        //eq. 3.146
        return new FieldVector3D<DerivativeStructure>(
                gmDS.multiply(4 / r).subtract(s2),
                p,
                new DerivativeStructure(1, 1, 4 * p.dotProduct(v)),
                v)
                .scalarMultiply(gmDS.divide(r2 * r * c2));
    }

    @Override
    public EventDetector[] getEventsDetectors() {
        return null;
    }

    @Override
    public double getParameter(final String name) throws UnknownParameterException {
        complainIfNotSupported(name);
        return this.gm;
    }

    @Override
    public void setParameter(final String name, final double value) throws UnknownParameterException {
        complainIfNotSupported(name);
        this.gm = value;
    }
}

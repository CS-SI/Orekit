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

import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.forces.AbstractForceModel;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.numerical.FieldTimeDerivativesEquations;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterObserver;

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
public class Relativity extends AbstractForceModel {

    /** Central attraction scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double MU_SCALE = FastMath.scalb(1.0, 32);

    /** Drivers for force model parameters. */
    private final ParameterDriver[] parametersDrivers;

    /** Earth's gravitational parameter. */
    private double gm;

    /**
     * Create a force model to add post-Newtonian acceleration corrections to an Earth
     * orbit.
     *
     * @param gm Earth's gravitational parameter.
     */
    public Relativity(final double gm) {
        this.parametersDrivers = new ParameterDriver[1];
        try {
            parametersDrivers[0] = new ParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT,
                                                       gm, MU_SCALE,
                                                       0.0, Double.POSITIVE_INFINITY);
            parametersDrivers[0].addObserver(new ParameterObserver() {
                /** {@inheritDoc} */
                @Override
                public void valueChanged(final double previousValue, final ParameterDriver driver) {
                    Relativity.this.gm = driver.getValue();
                }
            });
        } catch (OrekitException oe) {
            // this should never occur as valueChanged above never throws an exception
            throw new OrekitInternalError(oe);
        };

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
    public Stream<EventDetector> getEventsDetectors() {
        return Stream.empty();
    }

    @Override
    /** {@inheritDoc} */
    public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(final Field<T> field) {
        return Stream.empty();
    }


    /** {@inheritDoc} */
    public ParameterDriver[] getParametersDrivers() {
        return parametersDrivers.clone();
    }

    @Override
    public <T extends RealFieldElement<T>> void
        addContribution(final FieldSpacecraftState<T> s,
                        final FieldTimeDerivativesEquations<T> adder)
            throws OrekitException {
        final FieldPVCoordinates<T> pv = s.getPVCoordinates();
        final FieldVector3D<T> p = pv.getPosition();
        final FieldVector3D<T> v = pv.getVelocity();
        //radius
        final T r2 = p.getNormSq();
        final T r = r2.sqrt();
        //speed
        final T s2 = v.getNormSq();
        final double c2 = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT;
        //eq. 3.146
        final FieldVector3D<T> accel = new FieldVector3D<T>(
                 r.reciprocal().multiply(4 * this.gm).subtract(s2),
                p,
                p.dotProduct(v).multiply(4),
                v)
                .scalarMultiply(r2.multiply(r).multiply(c2).reciprocal().multiply(this.gm));
        adder.addAcceleration(accel, s.getFrame()); //TODO NOT TESTED
    }

}

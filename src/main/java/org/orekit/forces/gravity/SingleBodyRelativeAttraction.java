/* Copyright 2002-2017 CS Systèmes d'Information
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
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBody;
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
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterObserver;

/** Body attraction force model computed as relative acceleration towards frame center.
 * @author Luc Maisonabe
 * @author Julio Hernanz
 */
public class SingleBodyRelativeAttraction extends AbstractForceModel {

    /** Suffix for parameter name for attraction coefficient enabling Jacobian processing. */
    public static final String ATTRACTION_COEFFICIENT_SUFFIX = " attraction coefficient";

    /** Central attraction scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double MU_SCALE = FastMath.scalb(1.0, 32);

    /** Drivers for force model parameters. */
    private final ParameterDriver[] parametersDrivers;

    /** The body to consider. */
    private final CelestialBody body;

    /** Local value for body attraction coefficient. */
    private double gm;

    /** Factory for the DerivativeStructure instances. */
    private final DSFactory factory;

    /** Simple constructor.
     * @param body the body to consider
     * (ex: {@link org.orekit.bodies.CelestialBodyFactory#getSun()} or
     * {@link org.orekit.bodies.CelestialBodyFactory#getMoon()})
     */
    public SingleBodyRelativeAttraction(final CelestialBody body) {
        this.parametersDrivers = new ParameterDriver[1];
        try {
            parametersDrivers[0] = new ParameterDriver(body.getName() + ATTRACTION_COEFFICIENT_SUFFIX,
                                                       body.getGM(), MU_SCALE,
                                                       0.0, Double.POSITIVE_INFINITY);
            parametersDrivers[0].addObserver(new ParameterObserver() {
                /** {@inheritDoc} */
                public void valueChanged(final double previousValue, final ParameterDriver driver) {
                    SingleBodyRelativeAttraction.this.gm = driver.getValue();
                }
            });
        } catch (OrekitException oe) {
            // this should never occur as valueChanged above never throws an exception
            throw new OrekitInternalError(oe);
        }

        this.body    = body;
        this.gm      = body.getGM();
        this.factory = new DSFactory(1, 1);

    }

    /** {@inheritDoc} */
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder)
        throws OrekitException {

        // compute bodies separation vectors and squared norm
        final Vector3D centralToBody = body.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
        final Vector3D satToBody     = centralToBody.subtract(s.getPVCoordinates().getPosition());
        final double r2Sat           = satToBody.getNormSq();

        // compute relative acceleration
        final Vector3D bodyAcc = body.getPVCoordinates(s.getDate(), s.getFrame()).getAcceleration();
        final double a = gm / r2Sat;
        final Vector3D acceleration = new Vector3D(a, satToBody.normalize()).add(bodyAcc);

        // add contribution to the ODE second member
        adder.addXYZAcceleration(acceleration.getX(), acceleration.getY(), acceleration.getZ());

    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final AbsoluteDate date, final Frame frame,
                                                                      final FieldVector3D<DerivativeStructure> position,
                                                                      final FieldVector3D<DerivativeStructure> velocity,
                                                                      final FieldRotation<DerivativeStructure> rotation,
                                                                      final DerivativeStructure mass)
        throws OrekitException {

        // compute bodies separation vectors and squared norm
        final Vector3D centralToBody    = body.getPVCoordinates(date, frame).getPosition();
        final FieldVector3D<DerivativeStructure> satToBody = position.subtract(centralToBody).negate();
        final DerivativeStructure r2Sat = satToBody.getNormSq();

        // compute relative acceleration
        final Vector3D bodyAcc = body.getPVCoordinates(date, frame).getAcceleration();
        final FieldVector3D<DerivativeStructure> satAcc =
                new FieldVector3D<>(r2Sat.reciprocal().multiply(gm), satToBody.normalize());
        return satAcc.add(bodyAcc);

    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final SpacecraftState s, final String paramName)
        throws OrekitException {

        complainIfNotSupported(paramName);

        // compute bodies separation vectors and squared norm
        final Vector3D centralToBody = body.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
        final Vector3D satToBody     = centralToBody.subtract(s.getPVCoordinates().getPosition());
        final double r2Sat           = satToBody.getNormSq();

        final DerivativeStructure gmds = factory.variable(0, gm);

        // compute relative acceleration
        final Vector3D bodyAcc = body.getPVCoordinates(s.getDate(), s.getFrame()).getAcceleration();
        final FieldVector3D<DerivativeStructure> satAcc = new FieldVector3D<>(gmds.divide(r2Sat), satToBody.normalize());
        return satAcc.add(bodyAcc);
    }

    /** {@inheritDoc} */
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

    /**{@inheritDoc} */
    public <T extends RealFieldElement<T>> void
        addContribution(final FieldSpacecraftState<T> s,
                        final FieldTimeDerivativesEquations<T> adder)
            throws OrekitException {
        // compute bodies separation vectors and squared norm
        final FieldVector3D<T> centralToBody = new FieldVector3D<>(s.getA().getField(),
                                                                   body.getPVCoordinates(s.getDate().toAbsoluteDate(), s.getFrame()).getPosition());
        final FieldVector3D<T> satToBody     = centralToBody.subtract(s.getPVCoordinates().getPosition());
        final T                r2Sat         = satToBody.getNormSq();

        // compute absolute acceleration
        final FieldVector3D<T> acceleration =
            new FieldVector3D<>(r2Sat.reciprocal().multiply(gm), satToBody.normalize());

        // relative acceleration of the body (2nd way) substract added
        final FieldVector3D<T> bodyAcc = body.getPVCoordinates(s.getDate(), s.getFrame()).getAcceleration();
        final FieldVector3D<T> finalAcceleration = acceleration.add(bodyAcc);

        adder.addXYZAcceleration(finalAcceleration.getX(), finalAcceleration.getY(), finalAcceleration.getZ());
    }

}

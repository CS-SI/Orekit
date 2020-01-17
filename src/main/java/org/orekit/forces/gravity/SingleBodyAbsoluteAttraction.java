/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBodies;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.forces.AbstractForceModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.utils.ParameterDriver;

/** Body attraction force model computed as absolute acceleration towards a body.
 * <p>
 * This force model represents the same physical principles as {@link NewtonianAttraction},
 * but has several major differences:
 * </p>
 * <ul>
 *   <li>the attracting body can be <em>away</em> from the integration frame center,</li>
 *   <li>several instances of this force model can be added when several bodies are involved,</li>
 *   <li>this force model is <em>never</em> automatically added by the numerical propagator</li>
 * </ul>
 * <p>
 * The possibility for the attracting body to be away from the frame center allows to use this force
 * model when integrating for example an interplanetary trajectory propagated in an Earth centered
 * frame (in which case an instance of {@link org.orekit.forces.inertia.InertialForces} must also be
 * added to take into account the coupling effect of relative frames motion).
 * </p>
 * <p>
 * The possibility to add several instances allows to use this in interplanetary trajectories or
 * in trajectories about Lagrangian points
 * </p>
 * <p>
 * The fact this force model is <em>never</em> automatically added by the numerical propagator differs
 * from {@link NewtonianAttraction} as {@link NewtonianAttraction} may be added automatically when
 * propagating a trajectory represented as an {@link org.orekit.orbits.Orbit}, which must always refer
 * to a central body, if user did not add the {@link NewtonianAttraction} or set the central attraction
 * coefficient by himself.
 * </p>
 * @see org.orekit.forces.inertia.InertialForces
 * @author Luc Maisonobe
 * @author Julio Hernanz
 */
public class SingleBodyAbsoluteAttraction extends AbstractForceModel {

    /** Suffix for parameter name for attraction coefficient enabling Jacobian processing. */
    public static final String ATTRACTION_COEFFICIENT_SUFFIX = " attraction coefficient";

    /** Central attraction scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double MU_SCALE = FastMath.scalb(1.0, 32);

    /** The body to consider. */
    private final CelestialBody body;

    /** Driver for gravitational parameter. */
    private final ParameterDriver gmParameterDriver;

    /** Simple constructor.
     * @param body the body to consider
     * (ex: {@link CelestialBodies#getSun()} or
     * {@link CelestialBodies#getMoon()})
     */
    public SingleBodyAbsoluteAttraction(final CelestialBody body) {
        try {
            gmParameterDriver = new ParameterDriver(body.getName() + ATTRACTION_COEFFICIENT_SUFFIX,
                                                    body.getGM(), MU_SCALE,
                                                    0.0, Double.POSITIVE_INFINITY);
        } catch (OrekitException oe) {
            // this should never occur
            throw new OrekitInternalError(oe);
        }

        this.body = body;

    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters) {

        // compute bodies separation vectors and squared norm
        final Vector3D bodyPosition = body.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
        final Vector3D satToBody     = bodyPosition.subtract(s.getPVCoordinates().getPosition());
        final double r2Sat           = satToBody.getNormSq();

        // compute absolute acceleration
        return new Vector3D(parameters[0] / (r2Sat * FastMath.sqrt(r2Sat)), satToBody);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters) {
         // compute bodies separation vectors and squared norm
        final FieldVector3D<T> centralToBody = new FieldVector3D<>(s.getA().getField(),
                                                                   body.getPVCoordinates(s.getDate().toAbsoluteDate(), s.getFrame()).getPosition());
        final FieldVector3D<T> satToBody     = centralToBody.subtract(s.getPVCoordinates().getPosition());
        final T                r2Sat         = satToBody.getNormSq();

        // compute absolute acceleration
        return new FieldVector3D<>(parameters[0].divide(r2Sat.multiply(r2Sat.sqrt())), satToBody);

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
        return new ParameterDriver[] {
            gmParameterDriver
        };
    }

}

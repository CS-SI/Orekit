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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.forces.AbstractForceModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.utils.ParameterDriver;

/** Third body attraction force model.
 *
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class ThirdBodyAttraction extends AbstractForceModel {

    /** Suffix for parameter name for attraction coefficient enabling Jacobian processing. */
    public static final String ATTRACTION_COEFFICIENT_SUFFIX = " attraction coefficient";

    /** Central attraction scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double MU_SCALE = FastMath.scalb(1.0, 32);

    /** Drivers for third body attraction coefficient. */
    private final ParameterDriver gmParameterDriver;

    /** The body to consider. */
    private final CelestialBody body;

    /** Simple constructor.
     * @param body the third body to consider
     * (ex: {@link org.orekit.bodies.CelestialBodyFactory#getSun()} or
     * {@link org.orekit.bodies.CelestialBodyFactory#getMoon()})
     */
    public ThirdBodyAttraction(final CelestialBody body) {
        try {
            gmParameterDriver = new ParameterDriver(body.getName() + ATTRACTION_COEFFICIENT_SUFFIX,
                                                    body.getGM(), MU_SCALE,
                                                    0.0, Double.POSITIVE_INFINITY);
        } catch (OrekitException oe) {
            // this should never occur as valueChanged above never throws an exception
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
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters)
        throws OrekitException {

        final double gm = parameters[0];

        // compute bodies separation vectors and squared norm
        final Vector3D centralToBody = body.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
        final double r2Central       = centralToBody.getNormSq();
        final Vector3D satToBody     = centralToBody.subtract(s.getPVCoordinates().getPosition());
        final double r2Sat           = satToBody.getNormSq();

        // compute relative acceleration
        return new Vector3D(gm / (r2Sat * FastMath.sqrt(r2Sat)), satToBody,
                           -gm / (r2Central * FastMath.sqrt(r2Central)), centralToBody);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters)
        throws OrekitException {

        final T gm = parameters[0];

        // compute bodies separation vectors and squared norm
        final FieldVector3D<T> centralToBody = new FieldVector3D<>(s.getA().getField(),
                                                                   body.getPVCoordinates(s.getDate().toAbsoluteDate(), s.getFrame()).getPosition());
        final T                r2Central     = centralToBody.getNormSq();
        final FieldVector3D<T> satToBody     = centralToBody.subtract(s.getPVCoordinates().getPosition());
        final T                r2Sat         = satToBody.getNormSq();

        // compute relative acceleration
        return new FieldVector3D<>(r2Sat.multiply(r2Sat.sqrt()).reciprocal().multiply(gm), satToBody,
                                   r2Central.multiply(r2Central.sqrt()).reciprocal().multiply(gm).negate(), centralToBody);

    }

    /** {@inheritDoc} */
    public Stream<EventDetector> getEventsDetectors() {
        return Stream.empty();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(final Field<T> field) {
        return Stream.empty();
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getParametersDrivers() {
        return new ParameterDriver[] {
            gmParameterDriver
        };
    }

}

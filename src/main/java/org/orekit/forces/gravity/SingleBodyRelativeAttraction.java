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
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

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

    /** Drivers for body attraction coefficient. */
    private final ParameterDriver gmDriver;

    /** The body to consider. */
    private final CelestialBody body;

    /** Simple constructor.
     * @param body the body to consider
     * (ex: {@link CelestialBodies#getSun()} or
     * {@link CelestialBodies#getMoon()})
     */
    public SingleBodyRelativeAttraction(final CelestialBody body) {

        this.body = body;

        try {
            gmDriver = new ParameterDriver(body.getName() + ATTRACTION_COEFFICIENT_SUFFIX,
                                           body.getGM(), MU_SCALE,
                                           0.0, Double.POSITIVE_INFINITY);
        } catch (OrekitException oe) {
            // this should never occur as valueChanged above never throws an exception
            throw new OrekitInternalError(oe);
        }

    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return true;
    }

    /** {@inheritDoc} */
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters) {

        // compute bodies separation vectors and squared norm
        final PVCoordinates bodyPV   = body.getPVCoordinates(s.getDate(), s.getFrame());
        final Vector3D satToBody     = bodyPV.getPosition().subtract(s.getPVCoordinates().getPosition());
        final double r2Sat           = satToBody.getNormSq();

        // compute relative acceleration
        final double gm = parameters[0];
        final double a = gm / r2Sat;
        return new Vector3D(a, satToBody.normalize()).add(bodyPV.getAcceleration());

    }

    /** {@inheritDoc} */
    public <T extends RealFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters) {

        // compute bodies separation vectors and squared norm
        final FieldPVCoordinates<T> bodyPV = body.getPVCoordinates(s.getDate(), s.getFrame());
        final FieldVector3D<T> satToBody   = bodyPV.getPosition().subtract(s.getPVCoordinates().getPosition());
        final T                r2Sat       = satToBody.getNormSq();

        // compute relative acceleration
        final T gm = parameters[0];
        final T a  = gm.divide(r2Sat);
        return new FieldVector3D<>(a, satToBody.normalize()).add(bodyPV.getAcceleration());

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
            gmDriver
        };
    }

}

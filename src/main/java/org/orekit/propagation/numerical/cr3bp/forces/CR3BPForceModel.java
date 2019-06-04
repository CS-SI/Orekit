/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.propagation.numerical.cr3bp.forces;

import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CR3BPSystem;
import org.orekit.errors.OrekitException;
import org.orekit.forces.AbstractForceModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.utils.ParameterDriver;

/** Class calculating the acceleration induced by CR3BP model.
 * @author Vincent Mouraux
 */
public class CR3BPForceModel extends AbstractForceModel {

    /** Suffix for parameter name for attraction coefficient enabling Jacobian processing. */
    public static final String ATTRACTION_COEFFICIENT_SUFFIX =  " attraction coefficient";

    /**
     * Central attraction scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction in the
     * multiplications/divisions sequences.
     * </p>
     */
    private static final double MU_SCALE = FastMath.scalb(1.0, 32);

    /** Driver for gravitational parameter. */
    private final ParameterDriver muParameterDriver;

    /** Simple constructor.
     * @param cr3bp Name of the CR3BP System
     */
    public CR3BPForceModel(final CR3BPSystem cr3bp) {
        muParameterDriver =
            new ParameterDriver(cr3bp.getName() + ATTRACTION_COEFFICIENT_SUFFIX,
                                cr3bp.getMu(), MU_SCALE, 0.0,
                                Double.POSITIVE_INFINITY);
    }

    /** {@inheritDoc} */
    public Vector3D acceleration(final SpacecraftState s,
                                 final double[] parameters)
        throws OrekitException {

        final double mu = parameters[0];
        final double d1 = mu;
        final double d2 = 1 - mu;

        final double x = s.getPVCoordinates().getPosition().getX();
        final double y = s.getPVCoordinates().getPosition().getY();
        final double z = s.getPVCoordinates().getPosition().getZ();

        final double vx = s.getPVCoordinates().getVelocity().getX();
        final double vy = s.getPVCoordinates().getVelocity().getY();

        final double r1 = FastMath.sqrt((x + d1) * (x + d1) + y * y + z * z);
        final double r2 = FastMath.sqrt((x - d2) * (x - d2) + y * y + z * z);

        final double dUdX =
            -(1 - mu) * (x + d1) / (r1 * r1 * r1) -
                            mu * (x - d2) / (r2 * r2 * r2) + x;
        final double dUdY =
            -y * ((1 - mu) / (r1 * r1 * r1) + mu / (r2 * r2 * r2)) + y;
        final double dUdZ =
            -z * ((1 - mu) / (r1 * r1 * r1) + mu / (r2 * r2 * r2));

        final double accx = dUdX + 2 * vy;

        final double accy = dUdY - 2 * vx;

        final double accz = dUdZ;

        // compute absolute acceleration
        return new Vector3D(accx, accy, accz);

    }

    /** {@inheritDoc} */
    public <T extends RealFieldElement<T>> FieldVector3D<T>
        acceleration(final FieldSpacecraftState<T> s, final T[] parameters)
            throws OrekitException {
        // compute bodies separation vectors and squared norm

        final T mu = parameters[0];
        final T d1 = mu;
        final T d2 = mu.negate().add(1);

        final T x = s.getPVCoordinates().getPosition().getX();
        final T y = s.getPVCoordinates().getPosition().getY();
        final T z = s.getPVCoordinates().getPosition().getZ();

        final T vx = s.getPVCoordinates().getVelocity().getX();
        final T vy = s.getPVCoordinates().getVelocity().getY();

        final T r1 =
            ((d1.add(x)).multiply(d1.add(x)).add(y.multiply(y))
                .add(z.multiply(z))).sqrt();
        final T r2 =
            ((x.subtract(d2)).multiply(x.subtract(d2)).add(y.multiply(y))
                .add(z.multiply(z))).sqrt();

        final T dUdX =
            (r1.pow(3)).reciprocal().multiply(mu.negate().add(1))
                .multiply(x.add(d1)).negate()
                .subtract(((r2.pow(3)).reciprocal()).multiply(mu)
                    .multiply(x.subtract(d2)))
                .add(x);

        final T dUdY =
            y.negate()
                .multiply((r1.pow(3).reciprocal()).multiply(mu.negate().add(1))
                    .add(mu.multiply(r2.pow(3).reciprocal())))
                .add(y);

        final T dUdZ =
            z.negate().multiply((r1.pow(3).reciprocal())
                .multiply(mu.negate().add(1)).add(mu.multiply(r2.pow(3))));

        final T accx = dUdX.add(vy.multiply(2));

        final T accy = dUdY.subtract(vx.multiply(2));

        final T accz = dUdZ;

        // compute absolute acceleration
        return new FieldVector3D<>(accx, accy, accz);

    }

    /** {@inheritDoc} */
    public Stream<EventDetector> getEventsDetectors() {
        return Stream.empty();
    }

    @Override
    /** {@inheritDoc} */
    public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>>
        getFieldEventsDetectors(final Field<T> field) {
        return Stream.empty();
    }

    /** {@inheritDoc} */
    public ParameterDriver[] getParametersDrivers() {
        return new ParameterDriver[] {
            muParameterDriver
        };
    }

    /** {@inheritDoc} */
    public boolean dependsOnPositionOnly() {
        return true;
    }
}

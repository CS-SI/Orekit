/* Copyright 2002-2020 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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
package org.orekit.propagation.numerical.cr3bp;

import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FDSFactory;
import org.hipparchus.analysis.differentiation.FieldDerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CR3BPSystem;
import org.orekit.forces.AbstractForceModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.utils.ParameterDriver;

/** Class calculating the acceleration induced by CR3BP model.
 * @see "Dynamical systems, the three-body problem, and space mission design, Koon, Lo, Marsden, Ross"
 * @author Vincent Mouraux
 * @since 10.2
 */
public class CR3BPForceModel extends AbstractForceModel {

    /** Suffix for parameter name for Mass Ratio enabling Jacobian processing. */
    public static final String MASS_RATIO_SUFFIX =  "CR3BP System Mass Ratio";

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
        muParameterDriver = new ParameterDriver(cr3bp.getName() + MASS_RATIO_SUFFIX,
                                                cr3bp.getMassRatio(), MU_SCALE, 0.0,
                                                Double.POSITIVE_INFINITY);
    }

    /** {@inheritDoc} */
    public Vector3D acceleration(final SpacecraftState s,
                                 final double[] parameters) {

        // Spacecraft Velocity
        final double vx = s.getPVCoordinates().getVelocity().getX();
        final double vy = s.getPVCoordinates().getVelocity().getY();

        // Spacecraft Potential
        final DerivativeStructure potential = getPotential(s);

        // Potential derivatives
        final double[] dU = potential.getAllDerivatives();

        // first order derivatives index
        final int idX = potential.getFactory().getCompiler().getPartialDerivativeIndex(1, 0, 0);
        final int idY = potential.getFactory().getCompiler().getPartialDerivativeIndex(0, 1, 0);
        final int idZ = potential.getFactory().getCompiler().getPartialDerivativeIndex(0, 0, 1);

        // Acceleration calculation according to CR3BP Analytical Model
        final double accx = dU[idX] + 2.0 * vy;
        final double accy = dU[idY] - 2.0 * vx;
        final double accz = dU[idZ];

        // compute absolute acceleration
        return new Vector3D(accx, accy, accz);

    }

    /** {@inheritDoc} */
    public <T extends RealFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters) {

        // Spacecraft Velocity
        final T vx = s.getPVCoordinates().getVelocity().getX();
        final T vy = s.getPVCoordinates().getVelocity().getY();

        // Spacecraft Potential
        final FieldDerivativeStructure<T> fieldPotential = getPotential(s);
        // Potential derivatives
        final T[] dU = fieldPotential.getAllDerivatives();

        // first order derivatives index
        final int idX = fieldPotential.getFactory().getCompiler().getPartialDerivativeIndex(1, 0, 0);
        final int idY = fieldPotential.getFactory().getCompiler().getPartialDerivativeIndex(0, 1, 0);
        final int idZ = fieldPotential.getFactory().getCompiler().getPartialDerivativeIndex(0, 0, 1);

        // Acceleration calculation according to CR3BP Analytical Model
        final T accx = dU[idX].add(vy.multiply(2.0));
        final T accy = dU[idY].subtract(vx.multiply(2.0));
        final T accz = dU[idZ];

        // compute absolute acceleration
        return new FieldVector3D<>(accx, accy, accz);

    }

    /**
     * Calculate spacecraft potential.
     * @param s SpacecraftState
     * @return Spacecraft Potential
     */
    public DerivativeStructure getPotential(final SpacecraftState s) {

        // Spacecraft Position
        final double x = s.getPVCoordinates().getPosition().getX();
        final double y = s.getPVCoordinates().getPosition().getY();
        final double z = s.getPVCoordinates().getPosition().getZ();

        final DSFactory factoryP = new DSFactory(3, 2);
        final DerivativeStructure fpx = factoryP.variable(0, x);
        final DerivativeStructure fpy = factoryP.variable(1, y);
        final DerivativeStructure fpz = factoryP.variable(2, z);

        final DerivativeStructure zero = fpx.getField().getZero();

        // Get CR3BP System mass ratio
        final DerivativeStructure mu = zero.add(muParameterDriver.getValue());

        // Normalized distances between primaries and barycenter in CR3BP
        final DerivativeStructure d1 = mu;
        final DerivativeStructure d2 = mu.negate().add(1.0);

        // Norm of the Spacecraft position relative to the primary body
        final DerivativeStructure r1 =
            FastMath.sqrt((fpx.add(d1)).multiply(fpx.add(d1)).add(fpy.multiply(fpy))
                .add(fpz.multiply(fpz)));

        // Norm of the Spacecraft position relative to the secondary body
        final DerivativeStructure r2 =
            FastMath.sqrt((fpx.subtract(d2)).multiply(fpx.subtract(d2))
                .add(fpy.multiply(fpy)).add(fpz.multiply(fpz)));

        // Potential of the Spacecraft
        return (mu.negate().add(1.0).divide(r1)).add(mu.divide(r2))
                .add(fpx.multiply(fpx).add(fpy.multiply(fpy)).multiply(0.5)).add(d1.multiply(d2).multiply(0.5));
    }

    /**
     * Calculate spacecraft potential.
     * @param <T> Field element
     * @param s SpacecraftState
     * @return Spacecraft Potential
     */
    public <T extends RealFieldElement<T>> FieldDerivativeStructure<T> getPotential(final FieldSpacecraftState<T> s) {

        // Spacecraft Position
        final T x = s.getPVCoordinates().getPosition().getX();
        final T y = s.getPVCoordinates().getPosition().getY();
        final T z = s.getPVCoordinates().getPosition().getZ();

        final FDSFactory<T> factoryP = new FDSFactory<>(s.getDate().getField(), 3, 2);
        final FieldDerivativeStructure<T> fpx = factoryP.variable(0, x);
        final FieldDerivativeStructure<T> fpy = factoryP.variable(1, y);
        final FieldDerivativeStructure<T> fpz = factoryP.variable(2, z);
        final FieldDerivativeStructure<T> zero = fpx.getField().getZero();

        // Get CR3BP System mass ratio
        final FieldDerivativeStructure<T> mu = zero.add(muParameterDriver.getValue());

        // Normalized distances between primaries and barycenter in CR3BP
        final FieldDerivativeStructure<T> d1 = mu;
        final FieldDerivativeStructure<T> d2 = mu.negate().add(1.0);

        // Norm of the Spacecraft position relative to the primary body
        final FieldDerivativeStructure<T> r1 =
            FastMath.sqrt((fpx.add(d1)).multiply(fpx.add(d1)).add(fpy.multiply(fpy))
                .add(fpz.multiply(fpz)));

        // Norm of the Spacecraft position relative to the secondary body
        final FieldDerivativeStructure<T> r2 =
            FastMath.sqrt((fpx.subtract(d2)).multiply(fpx.subtract(d2))
                .add(fpy.multiply(fpy)).add(fpz.multiply(fpz)));

        // Potential of the Spacecraft
        return (mu.negate().add(1.0).divide(r1)).add(mu.divide(r2))
                .add(fpx.multiply(fpx).add(fpy.multiply(fpy)).multiply(0.5)).add(d1.multiply(d2).multiply(0.5));
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
            muParameterDriver
        };
    }

    /** {@inheritDoc} */
    public boolean dependsOnPositionOnly() {
        return true;
    }
}

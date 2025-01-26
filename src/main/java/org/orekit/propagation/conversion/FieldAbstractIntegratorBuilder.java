/* Copyright 2022-2025 Romain Serra
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
package org.orekit.propagation.conversion;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.ode.AbstractFieldIntegrator;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/**
 * This abstract class implements some of the required methods for integrators in propagators conversion.

 * @author Vincent Cucchietti
 * @author Romain Serra
 * @since 13.0
 * @param <T> type of the field elements
 */
public abstract class FieldAbstractIntegratorBuilder<T extends CalculusFieldElement<T>, W extends AbstractFieldIntegrator<T>>
        implements FieldODEIntegratorBuilder<T> {

    /** {@inheritDoc} */
    @Override
    public W buildIntegrator(final Field<T> field, final Orbit orbit, final OrbitType orbitType) {
        return buildIntegrator(field, orbit, orbitType, PositionAngleType.MEAN);
    }

    /** {@inheritDoc} */
    public abstract W buildIntegrator(Field<T> field, Orbit orbit, OrbitType orbitType, PositionAngleType angleType);

    /** {@inheritDoc} */
    @Override
    public W buildIntegrator(final FieldOrbit<T> orbit, final OrbitType orbitType) {
        return buildIntegrator(orbit.getA().getField(), orbit.toOrbit(), orbitType);
    }

    /** {@inheritDoc} */
    @Override
    public W buildIntegrator(final FieldAbsolutePVCoordinates<T> fieldAbsolutePVCoordinates) {
        final TimeStampedFieldPVCoordinates<T> fieldPVCoordinates = fieldAbsolutePVCoordinates.getPVCoordinates();
        final FieldCartesianOrbit<T> fieldOrbit = new FieldCartesianOrbit<>(fieldPVCoordinates, fieldAbsolutePVCoordinates.getFrame(),
                fieldAbsolutePVCoordinates.getDate().getField().getOne());
        return buildIntegrator(fieldOrbit, OrbitType.CARTESIAN);
    }
}

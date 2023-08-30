/* Copyright 2002-2023 CS GROUP
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
package org.orekit.forces.gravity;

import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBodies;
import org.orekit.bodies.CelestialBody;
import org.orekit.forces.ForceModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ParameterDriver;

/** Third body attraction force model.
 *
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class ThirdBodyAttraction implements ForceModel {

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
     * (ex: {@link CelestialBodies#getSun()} or
     * {@link CelestialBodies#getMoon()})
     */
    public ThirdBodyAttraction(final CelestialBody body) {
        gmParameterDriver = new ParameterDriver(body.getName() + ATTRACTION_COEFFICIENT_SUFFIX,
                                                body.getGM(), MU_SCALE,
                                                0.0, Double.POSITIVE_INFINITY);

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

        final double gm = parameters[0];

        // compute bodies separation vectors and squared norm
        final Vector3D centralToBody = body.getPosition(s.getDate(), s.getFrame());
        final double r2Central       = centralToBody.getNormSq();
        final Vector3D satToBody     = centralToBody.subtract(s.getPosition());
        final double r2Sat           = satToBody.getNormSq();

        // compute relative acceleration
        return new Vector3D(gm / (r2Sat * FastMath.sqrt(r2Sat)), satToBody,
                           -gm / (r2Central * FastMath.sqrt(r2Central)), centralToBody);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters) {

        final T gm = parameters[0];

        // compute bodies separation vectors and squared norm
        final FieldVector3D<T> centralToBody = body.getPosition(s.getDate(), s.getFrame());
        final T                r2Central     = centralToBody.getNormSq();
        final FieldVector3D<T> satToBody     = centralToBody.subtract(s.getPosition());
        final T                r2Sat         = satToBody.getNormSq();

        // compute relative acceleration
        return new FieldVector3D<>(r2Sat.multiply(r2Sat.sqrt()).reciprocal().multiply(gm), satToBody,
                                   r2Central.multiply(r2Central.sqrt()).reciprocal().multiply(gm).negate(), centralToBody);

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.singletonList(gmParameterDriver);
    }

}

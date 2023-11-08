/* Contributed to the public domain
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
import org.orekit.forces.ForceModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

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
public class Relativity implements ForceModel {

    /** Central attraction scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double MU_SCALE = FastMath.scalb(1.0, 32);

    /** Driver for gravitational parameter. */
    private final ParameterDriver gmParameterDriver;

    /**
     * Create a force model to add post-Newtonian acceleration corrections to an Earth
     * orbit.
     *
     * @param gm Earth's gravitational parameter.
     */
    public Relativity(final double gm) {
        gmParameterDriver = new ParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT,
                                                gm, MU_SCALE,
                                                0.0, Double.POSITIVE_INFINITY);
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters) {

        final double gm = parameters[0];

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
        return new Vector3D(
                4 * gm / r - s2,
                p,
                4 * p.dotProduct(v),
                v)
                .scalarMultiply(gm / (r2 * r * c2));

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters) {

        final T gm = parameters[0];

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
        return new FieldVector3D<>(r.reciprocal().multiply(4).multiply(gm).subtract(s2),
                                   p,
                                   p.dotProduct(v).multiply(4),
                                   v).scalarMultiply(r2.multiply(r).multiply(c2).reciprocal().multiply(gm));

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.singletonList(gmParameterDriver);
    }

}

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
import org.orekit.forces.ForceModel;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

/**
 * Lense-Thirring post-Newtonian correction force due to general relativity.
 * <p>
 * Lense-Thirring term causes a precession of the orbital plane at a rate of
 * the order of 0.8 mas per year (geostationary) to 180 mas per year (low orbit).
 * </p>
 * @see "Petit, G. and Luzum, B. (eds.), IERS Conventions (2010), Chapter 10,
 * General relativistic models for space-time coordinates and equations of motion (2010)"
 *
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class LenseThirringRelativity implements ForceModel {

    /** Intensity of the Earth's angular momentum per unit mass [m²/s]. */
    private static final double J = 9.8e8;

    /** Central attraction scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double MU_SCALE = FastMath.scalb(1.0, 32);

    /** Driver for gravitational parameter. */
    private final ParameterDriver gmParameterDriver;

    /** Central body frame. */
    private final Frame bodyFrame;

    /**
     * Constructor.
     * @param gm Earth's gravitational parameter.
     * @param bodyFrame central body frame
     */
    public LenseThirringRelativity(final double gm, final Frame bodyFrame) {
        gmParameterDriver = new ParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT,
                                                gm, MU_SCALE,
                                                0.0, Double.POSITIVE_INFINITY);
        this.bodyFrame = bodyFrame;
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters) {

        // Useful constant
        final double c2 = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT;

        // Earth's gravitational parameter
        final double gm = parameters[0];

        // Satellite position and velocity with respect to the Earth
        final PVCoordinates pv = s.getPVCoordinates();
        final Vector3D p = pv.getPosition();
        final Vector3D v = pv.getVelocity();

        // Radius
        final double r  = p.getNorm();
        final double r2 = r * r;

        // Earth’s angular momentum per unit mass
        final StaticTransform t =
                bodyFrame.getStaticTransformTo(s.getFrame(), s.getDate());
        final Vector3D  j = t.transformVector(Vector3D.PLUS_K).scalarMultiply(J);

        // Eq. 10.12
        return new Vector3D(3.0 * p.dotProduct(j) / r2,
                            p.crossProduct(v),
                            1.0,
                            v.crossProduct(j))
                            .scalarMultiply((2.0 * gm) / (r2 * r * c2));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters) {

        // Useful constant
        final double c2 = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT;

        // Earth's gravitational parameter
        final T gm = parameters[0];

        // Satellite position and velocity with respect to the Earth
        final FieldPVCoordinates<T> pv = s.getPVCoordinates();
        final FieldVector3D<T> p = pv.getPosition();
        final FieldVector3D<T> v = pv.getVelocity();

        // Radius
        final T r  = p.getNorm();
        final T r2 = r.multiply(r);

        // Earth’s angular momentum per unit mass
        final FieldStaticTransform<T> t = bodyFrame.getStaticTransformTo(s.getFrame(), s.getDate());
        final FieldVector3D<T>        j = t.transformVector(Vector3D.PLUS_K).scalarMultiply(J);

        return new FieldVector3D<>(p.dotProduct(j).multiply(3.0).divide(r2),
                                   p.crossProduct(v),
                                   r.getField().getOne(),
                                   v.crossProduct(j))
                                   .scalarMultiply(gm.multiply(2.0).divide(r2.multiply(r).multiply(c2)));
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.singletonList(gmParameterDriver);
    }

}

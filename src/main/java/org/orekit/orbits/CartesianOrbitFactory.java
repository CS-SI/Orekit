/* Copyright 2022-2026 Thales Alenia Space
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
package org.orekit.orbits;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.utils.PVCoordinates;

/** Factory for Cartesian orbits.
 * @since 14.0
 */
public class CartesianOrbitFactory extends AbstractOrbitFactory<CartesianOrbit> {

    /** Simple constructor.
     * @param template template orbit
     * @param positionScale position scale used to scale the orbital drivers
     */
    public CartesianOrbitFactory(final CartesianOrbit template, final double positionScale) {
        super(template, positionScale, PositionAngleType.ECCENTRIC);
    }

    /** {@inheritDoc} */
    @Override
    public CartesianOrbit toParameters(final double[] array) {
        return new CartesianOrbit(new PVCoordinates(new Vector3D(array[0], array[1], array[2]),
                                                    new Vector3D(array[3], array[4], array[5])),
                                  getFrame(), getDate(), getMu());
    }

    /** {@inheritDoc} */
    @Override
    public final double[] toArray(final CartesianOrbit orbit) {
        final PVCoordinates pv = orbit.getPVCoordinates();
        final Vector3D      p  = pv.getPosition();
        final Vector3D      v  = pv.getVelocity();
        return new double[] {
            p.getX(), p.getY(), p.getZ(), v.getX(), v.getY(), v.getZ()
        };
    }

}

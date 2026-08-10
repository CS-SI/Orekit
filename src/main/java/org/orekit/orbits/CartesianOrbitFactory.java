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
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

import java.util.List;

/** Factory for Cartesian orbits.
 * @since 14.0
 */
public class CartesianOrbitFactory extends AbstractOrbitFactory<CartesianOrbit> {

    /** Simple constructor.
     * @param template template orbit
     * @param positionScale position scale used to scale the orbital drivers
     */
    public CartesianOrbitFactory(final CartesianOrbit template, final double positionScale) {
        super(positionScale, template, PositionAngleType.ECCENTRIC);
    }

    /** {@inheritDoc} */
    @Override
    public CartesianOrbit createFromDrivers() {
        final AbsoluteDate           date    = getDate();
        final List<DelegatingDriver> drivers = getOrbitalParametersDrivers().getDrivers();
        return new CartesianOrbit(new PVCoordinates(new Vector3D(drivers.get(0).getValue(date),
                                                                 drivers.get(1).getValue(date),
                                                                 drivers.get(2).getValue(date)),
                                                    new Vector3D(drivers.get(3).getValue(date),
                                                                 drivers.get(4).getValue(date),
                                                                 drivers.get(5).getValue(date))),
                                  getFrame(), getDate(), getMu());
    }

}

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

/** Factory for circular orbits.
 * @since 14.0
 */
public class CircularOrbitFactory extends AbstractOrbitFactory<CircularOrbit> {

    /** Simple constructor.
     * @param template template orbit
     * @param positionScale position scale used to scale the orbital drivers
     * @param positionAngleType position angle type to use
     */
    protected CircularOrbitFactory(final CircularOrbit template, final double positionScale,
                                   final PositionAngleType positionAngleType) {
        super(template, positionScale, positionAngleType);
    }

    /** {@inheritDoc} */
    @Override
    public CircularOrbit toParameters(final double[] array) {
        return new CircularOrbit(array[0], array[1], array[2], array[3], array[4], array[5],
                                 getPositionAngleType(), getFrame(), getDate(), getMu());
    }

    /** {@inheritDoc} */
    @Override
    public final double[] toArray(final CircularOrbit orbit) {
        return new double[] {
            orbit.getA(),
            orbit.getCircularEx(),
            orbit.getCircularEy(),
            orbit.getI(),
            orbit.getRightAscensionOfAscendingNode(),
            orbit.getAlpha(getPositionAngleType())
        };
    }

}

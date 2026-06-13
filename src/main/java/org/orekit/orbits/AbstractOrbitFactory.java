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

import org.orekit.utils.ParameterDriver;

/** Factory for orbits.
 * @param <P> type of the orbits
 * @since 14.0
 */
public abstract class AbstractOrbitFactory<P extends Orbit>
    extends AbstractOrbitalParameterFactory<P> {

    /** Orbit type to use. */
    private final OrbitType orbitType;

    /** Position angle type to use. */
    private final PositionAngleType positionAngleType;

    /** Simple constructor.
     * <p>
     * The template orbit is used as a model. It defines the
     * inertial frame, the central attraction coefficient, the orbit type, and is also
     * used together with the {@code positionScale} to convert from the {@link
     * ParameterDriver#setNormalizedValue(double) normalized} parameters used by the
     * callers of this factory to the real orbital parameters.
     * </p>
     * @param template template orbit
     * @param positionScale position scale used to scale the orbital drivers
     * @param positionAngleType position angle type to use
     */
    protected AbstractOrbitFactory(final P template, final double positionScale,
                                   final PositionAngleType positionAngleType) {
        super(template.getDate(), template.getFrame(), template.getMu(),
              template.getType().getDrivers(positionScale, template, positionAngleType),
              positionScale);
        this.orbitType         = template.getType();
        this.positionAngleType = positionAngleType;
    }

    /** Get the orbit type to use.
     * @return orbit type to use
     */
    public OrbitType getOrbitType() {
        return orbitType;
    }

    /** Get the position angle type to use.
     * @return position angle type to use
     */
    public PositionAngleType getPositionAngleType() {
        return positionAngleType;
    }

}

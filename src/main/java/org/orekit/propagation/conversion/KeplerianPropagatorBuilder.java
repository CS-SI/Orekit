/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.propagation.conversion;

import org.orekit.errors.OrekitException;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;

/** Builder for Keplerian propagator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class KeplerianPropagatorBuilder extends AbstractPropagatorBuilder {

    /** Build a new instance.
     * <p>
     * The template orbit is used as a model to {@link
     * #createInitialOrbit() create initial orbit}. It defines the
     * inertial frame, the central attraction coefficient, the orbit type, and is also
     * used together with the {@code positionScale} to convert from the {@link
     * org.orekit.utils.ParameterDriver#setNormalizedValue(double) normalized} parameters used by the
     * callers of this builder to the real orbital parameters.
     * </p>
     * @param templateOrbit reference orbit from which real orbits will be built
     * @param positionAngle position angle type to use
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @exception OrekitException if parameters drivers cannot be scaled
     * @since 8.0
     */
    public KeplerianPropagatorBuilder(final Orbit templateOrbit, final PositionAngle positionAngle,
                                      final double positionScale)
        throws OrekitException {
        super(templateOrbit, positionAngle, positionScale);
    }

    /** {@inheritDoc} */
    public Propagator buildPropagator(final double[] normalizedParameters)
        throws OrekitException {
        setParameters(normalizedParameters);
        return new KeplerianPropagator(createInitialOrbit());
    }

}

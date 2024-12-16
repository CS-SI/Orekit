/* Copyright 2002-2024 CS GROUP
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

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;


/** Builder for Keplerian propagator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class KeplerianPropagatorBuilder extends AbstractAnalyticalPropagatorBuilder<KeplerianPropagator> {

    /** Build a new instance.
     * <p>
     * The template orbit is used as a model to {@link
     * #createInitialOrbit() create initial orbit}. It defines the
     * inertial frame, the central attraction coefficient, the orbit type, and is also
     * used together with the {@code positionScale} to convert from the {@link
     * org.orekit.utils.ParameterDriver#setNormalizedValue(double) normalized} parameters used by the
     * callers of this builder to the real orbital parameters.
     * The default attitude provider is aligned with the orbit's inertial frame.
     * </p>
     *
     * @param templateOrbit reference orbit from which real orbits will be built
     * @param positionAngleType position angle type to use
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @since 8.0
     * @see #KeplerianPropagatorBuilder(Orbit, PositionAngleType, double, AttitudeProvider)
     */
    public KeplerianPropagatorBuilder(final Orbit templateOrbit, final PositionAngleType positionAngleType,
                                      final double positionScale) {
        this(templateOrbit, positionAngleType, positionScale,
             FrameAlignedProvider.of(templateOrbit.getFrame()));
    }

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
     * @param positionAngleType position angle type to use
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @param attitudeProvider attitude law to use.
     * @since 10.1
     */
    public KeplerianPropagatorBuilder(final Orbit templateOrbit,
                                      final PositionAngleType positionAngleType,
                                      final double positionScale,
                                      final AttitudeProvider attitudeProvider) {
        super(templateOrbit, positionAngleType, positionScale, true, attitudeProvider, Propagator.DEFAULT_MASS);
    }

    /** {@inheritDoc} */
    public KeplerianPropagator buildPropagator(final double[] normalizedParameters) {
        setParameters(normalizedParameters);
        final KeplerianPropagator propagator = new KeplerianPropagator(createInitialOrbit(), getAttitudeProvider(), getMu(), getMass());
        getImpulseManeuvers().forEach(propagator::addEventDetector);
        return propagator;
    }
}

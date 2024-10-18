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

import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.analytical.tle.generation.TleGenerationAlgorithm;
import org.orekit.utils.ParameterDriver;

import java.util.List;

/** Builder for TLEPropagator.
 * @author Pascal Parraud
 * @author Thomas Paulet
 * @since 6.0
 */
public class TLEPropagatorBuilder extends AbstractAnalyticalPropagatorBuilder {

    /** Data context used to access frames and time scales. */
    private final DataContext dataContext;

    /** Template TLE. */
    private final TLE templateTLE;

    /** TLE generation algorithm. */
    private final TleGenerationAlgorithm generationAlgorithm;

    /** Build a new instance. This constructor uses the {@link DataContext#getDefault()
     * default data context}.
     * <p>
     * The template TLE is used as a model to {@link
     * #createInitialOrbit() create initial orbit}. It defines the
     * inertial frame, the central attraction coefficient, orbit type, satellite number,
     * classification, .... and is also used together with the {@code positionScale} to
     * convert from the {@link ParameterDriver#setNormalizedValue(double) normalized}
     * parameters used by the callers of this builder to the real orbital parameters.
     * </p>
     * @param templateTLE reference TLE from which real orbits will be built
     * @param positionAngleType position angle type to use
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @param generationAlgorithm TLE generation algorithm
     * @since 12.0
     * @see #TLEPropagatorBuilder(TLE, PositionAngleType, double, DataContext, TleGenerationAlgorithm)
     * @see #TLEPropagatorBuilder(TLE, PositionAngleType, double, DataContext, TleGenerationAlgorithm, AttitudeProvider)
     */
    @DefaultDataContext
    public TLEPropagatorBuilder(final TLE templateTLE, final PositionAngleType positionAngleType,
                                final double positionScale, final TleGenerationAlgorithm generationAlgorithm) {
        this(templateTLE, positionAngleType, positionScale, DataContext.getDefault(), generationAlgorithm);
    }

    /** Build a new instance.
     * <p>
     * The template TLE is used as a model to {@link
     * #createInitialOrbit() create initial orbit}. It defines the
     * inertial frame, the central attraction coefficient, orbit type, satellite number,
     * classification, .... and is also used together with the {@code positionScale} to
     * convert from the {@link ParameterDriver#setNormalizedValue(double) normalized}
     * parameters used by the callers of this builder to the real orbital parameters.
     * The default attitude provider is aligned with the orbit's inertial frame.
     * </p>
     * @param templateTLE reference TLE from which real orbits will be built
     * @param positionAngleType position angle type to use
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @param dataContext used to access frames and time scales.
     * @param generationAlgorithm TLE generation algorithm
     * @since 12.0
     * @see #TLEPropagatorBuilder(TLE, PositionAngleType, double, DataContext, TleGenerationAlgorithm, AttitudeProvider)
     */
    public TLEPropagatorBuilder(final TLE templateTLE, final PositionAngleType positionAngleType,
                                final double positionScale, final DataContext dataContext,
                                final TleGenerationAlgorithm generationAlgorithm) {
        this(templateTLE, positionAngleType, positionScale, dataContext, generationAlgorithm, FrameAlignedProvider.of(dataContext.getFrames().getTEME()));
    }

    /** Build a new instance.
     * <p>
     * The template TLE is used as a model to {@link
     * #createInitialOrbit() create initial orbit}. It defines the
     * inertial frame, the central attraction coefficient, orbit type, satellite number,
     * classification, .... and is also used together with the {@code positionScale} to
     * convert from the {@link ParameterDriver#setNormalizedValue(double) normalized}
     * parameters used by the callers of this builder to the real orbital parameters.
     * </p>
     * @param templateTLE reference TLE from which real orbits will be built
     * @param positionAngleType position angle type to use
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @param dataContext used to access frames and time scales.
     * @param generationAlgorithm TLE generation algorithm
     * @param attitudeProvider attitude law to use
     * @since 12.2
     */
    public TLEPropagatorBuilder(final TLE templateTLE, final PositionAngleType positionAngleType,
                                final double positionScale, final DataContext dataContext,
                                final TleGenerationAlgorithm generationAlgorithm, final AttitudeProvider attitudeProvider) {
        super(TLEPropagator.selectExtrapolator(templateTLE, dataContext.getFrames().getTEME(), attitudeProvider).getInitialState().getOrbit(),
              positionAngleType, positionScale, false, attitudeProvider, Propagator.DEFAULT_MASS);

        // Supported parameters: Bstar
        addSupportedParameters(templateTLE.getParametersDrivers());

        this.templateTLE         = templateTLE;
        this.dataContext         = dataContext;
        this.generationAlgorithm = generationAlgorithm;
    }


    /** {@inheritDoc} */
    @Override
    public TLEPropagator buildPropagator(final double[] normalizedParameters) {

        // create the orbit
        setParameters(normalizedParameters);
        final Orbit           orbit = createInitialOrbit();
        final SpacecraftState state = new SpacecraftState(orbit);
        final Frame           teme  = dataContext.getFrames().getTEME();

        // TLE related to the orbit
        final TLE tle = generationAlgorithm.generate(state, templateTLE);
        final List<ParameterDriver> drivers = templateTLE.getParametersDrivers();
        for (int index = 0; index < drivers.size(); index++) {
            if (drivers.get(index).isSelected()) {
                tle.getParametersDrivers().get(index).setSelected(true);
            }
        }

        // propagator
        final TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle, getAttitudeProvider(), getMass(), teme);
        getImpulseManeuvers().forEach(propagator::addEventDetector);
        return propagator;
    }

    /** Getter for the template TLE.
     * @return the template TLE
     */
    public TLE getTemplateTLE() {
        return templateTLE;
    }
}

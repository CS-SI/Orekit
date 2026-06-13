/* Copyright 2002-2026 CS GROUP
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
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.analytical.tle.TleParametersFactory;
import org.orekit.propagation.analytical.tle.generation.TleGenerationAlgorithm;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

import java.util.List;

/** Builder for TLEPropagator.
 * @author Pascal Parraud
 * @author Thomas Paulet
 * @since 6.0
 */
public class TLEPropagatorBuilder
    extends AbstractAnalyticalPropagatorBuilder<TLEPropagator, TLE, TleParametersFactory> {

    /** Data context used to access frames and time scales. */
    private final DataContext dataContext;

    /** TLE generation algorithm. */
    private final TleGenerationAlgorithm generationAlgorithm;

    /** Build a new instance. This constructor uses the {@link DataContext#getDefault()
     * default data context}.
     * @param factory TLE parameters factory
     * @param generationAlgorithm TLE generation algorithm
     * @since 14.0
     * @see #TLEPropagatorBuilder(DataContext, TleParametersFactory, TleGenerationAlgorithm)
     * @see #TLEPropagatorBuilder(DataContext, TleParametersFactory, TleGenerationAlgorithm, AttitudeProvider)
     */
    @DefaultDataContext
    public TLEPropagatorBuilder(final TleParametersFactory factory,
                                final TleGenerationAlgorithm generationAlgorithm) {
        this(DataContext.getDefault(), factory, generationAlgorithm);
    }

    /** Build a new instance.
     * @param dataContext used to access frames and time scales.
     * @param factory TLE parameters factory
     * @param generationAlgorithm TLE generation algorithm
     * @since 14.0
     * @see #TLEPropagatorBuilder(DataContext, TleParametersFactory, TleGenerationAlgorithm, AttitudeProvider)
     */
    public TLEPropagatorBuilder(final DataContext dataContext,
                                final TleParametersFactory factory,
                                final TleGenerationAlgorithm generationAlgorithm) {
        this(dataContext, factory, generationAlgorithm,
             FrameAlignedProvider.of(dataContext.getFrames().getTEME()));
    }

    /** Build a new instance.
     * @param dataContext used to access frames and time scales.
     * @param factory TLE parameters factory
     * @param generationAlgorithm TLE generation algorithm
     * @param attitudeProvider attitude law to use
     * @since 14.0
     */
    public TLEPropagatorBuilder(final DataContext dataContext,
                                final TleParametersFactory factory,
                                final TleGenerationAlgorithm generationAlgorithm,
                                final AttitudeProvider attitudeProvider) {
        super(factory, false, attitudeProvider, Propagator.DEFAULT_MASS);
        this.dataContext         = dataContext;
        this.generationAlgorithm = generationAlgorithm;

        // Propagation parameters: Bstar
        addPropagationParameters(factory.createFromDrivers().getParametersDrivers());

    }

    /** Copy constructor.
     * @param builder builder to copy from
     */
    private TLEPropagatorBuilder(final TLEPropagatorBuilder builder) {
        this(builder.dataContext, builder.getOrbitalParameterFactory(),
             builder.generationAlgorithm, builder.getAttitudeProvider());
    }

    /** {@inheritDoc}. */
    @Override
    public TLEPropagatorBuilder clone() {
        // Call to super clone() method to avoid warning
        final TLEPropagatorBuilder clonedBuilder = (TLEPropagatorBuilder) super.clone();

        // Use copy constructor to unlink orbital drivers
        final TLEPropagatorBuilder builder = new TLEPropagatorBuilder(clonedBuilder);

        // Set mass
        builder.setMass(getMass());

        // Ensure drivers' selection consistency
        final ParameterDriversList propDrivers = clonedBuilder.getPropagationParametersDrivers();
        builder.getPropagationParametersDrivers().getDrivers().
                        forEach(driver -> driver.setSelected(propDrivers.findByName(driver.getName()).isSelected()));
        return new TLEPropagatorBuilder(clonedBuilder);
    }

    /** {@inheritDoc} */
    @Override
    public TLEPropagator buildPropagator(final double[] normalizedParameters) {

        // set all parameters (including both orbital parameters and propagation parameters)
        setParameters(normalizedParameters);

        // TLE related to the orbit
        final TLE tle = getOrbitalParameterFactory().createFromDrivers();
        final List<ParameterDriver> drivers = tle.getParametersDrivers();
        for (int index = 0; index < drivers.size(); index++) {
            if (drivers.get(index).isSelected()) {
                tle.getParametersDrivers().get(index).setSelected(true);
            }
        }

        // propagator
        final TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle, getAttitudeProvider(), getMass(),
                                                                          getOrbitalParameterFactory().getFrame());
        getImpulseManeuvers().forEach(propagator::addEventDetector);
        return propagator;
    }

}

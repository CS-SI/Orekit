/* Copyright 2002-2025 Mark Rutten
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Mark Rutten licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.propagation.analytical.tle.generation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.FieldTLE;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.conversion.osc2mean.LeastSquaresConverter;
import org.orekit.propagation.conversion.osc2mean.TLETheory;
import org.orekit.time.TimeScale;
import org.orekit.utils.ParameterDriver;

/**
 * Least squares method to generate a usable TLE from a spacecraft state.
 *
 * @author Mark Rutten
 * @since 12.0
 */
public class LeastSquaresTleGenerationAlgorithm implements TleGenerationAlgorithm {

    /** Default value for maximum number of iterations.*/
    public static final int DEFAULT_MAX_ITERATIONS = 1000;

    /** Osculating to mean orbit converter. */
    private final LeastSquaresConverter converter;

    /** UTC time scale. */
    private final TimeScale utc;

    /**
     * Default constructor.
     * <p>
     * Uses the {@link DataContext#getDefault() default data context}  as well as
     * {@link #DEFAULT_MAX_ITERATIONS}.
     * </p>
     */
    @DefaultDataContext
    public LeastSquaresTleGenerationAlgorithm() {
        this(DEFAULT_MAX_ITERATIONS);
    }

    /**
     * Default constructor.
     * <p>
     * Uses the {@link DataContext#getDefault() default data context}.
     * </p>
     * @param maxIterations maximum number of iterations for convergence
     */
    @DefaultDataContext
    public LeastSquaresTleGenerationAlgorithm(final int maxIterations) {
        this(maxIterations, DataContext.getDefault().getTimeScales().getUTC(),
             DataContext.getDefault().getFrames().getTEME());
    }

    /**
     * Constructor.
     * @param maxIterations maximum number of iterations for convergence
     * @param utc  UTC time scale
     * @param teme TEME frame
     */
    public LeastSquaresTleGenerationAlgorithm(final int maxIterations,
                                              final TimeScale utc,
                                              final Frame teme) {
        this.converter = new LeastSquaresConverter(new TLETheory(utc, teme),
                                                   new LevenbergMarquardtOptimizer(),
                                                   LeastSquaresConverter.DEFAULT_THRESHOLD,
                                                   maxIterations);
        this.utc       = utc;
    }

    /** {@inheritDoc} */
    @Override
    public TLE generate(final SpacecraftState state, final TLE templateTLE) {
        final KeplerianOrbit mean = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(converter.convertToMean(state.getOrbit()));
        final TLE tle = TleGenerationUtil.newTLE(mean, templateTLE, templateTLE.getBStar(mean.getDate()), utc);
        // reset estimated parameters from template to generated tle
        for (final ParameterDriver templateDrivers : templateTLE.getParametersDrivers()) {
            if (templateDrivers.isSelected()) {
                // set to selected for the new TLE
                tle.getParameterDriver(templateDrivers.getName()).setSelected(true);
            }
        }
        return tle;
    }

    /**
     * Get the Root Mean Square of the TLE estimation.
     * <p>
     * Be careful that the RMS is updated each time the
     * {@link LeastSquaresTleGenerationAlgorithm#generate(SpacecraftState, TLE)}
     * method is called.
     * </p>
     * @return the RMS
     */
    public double getRms() {
        return converter.getRMS();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTLE<T> generate(final FieldSpacecraftState<T> state,
                                                                    final FieldTLE<T> templateTLE) {
        throw new UnsupportedOperationException();
    }
}

/* Copyright 2002-2026 Mark Rutten
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

import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresOptimizer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.conversion.osc2mean.LeastSquaresConverter;
import org.orekit.propagation.conversion.osc2mean.TLETheory;
import org.orekit.time.TimeScale;

/**
 * Least squares method to generate a usable TLE from a spacecraft state.
 *
 * @author Mark Rutten
 * @since 12.0
 */
public class LeastSquaresTleGenerationAlgorithm extends TleGenerationAlgorithm {

    /** Default value for maximum number of iterations.*/
    public static final int DEFAULT_MAX_ITERATIONS = 1000;

    /** Converter to mean elements.
     * @since 14.0
     */
    private final LeastSquaresConverter converter;

    /**
     * Default constructor.
     * <p>Uses:
     * <ul>
     * <li>the {@link DataContext#getDefault() default data context}</li>
     * <li>{@link #DEFAULT_MAX_ITERATIONS}</li>
     * <li>the {@link LevenbergMarquardtOptimizer}</li>
     * </ul>
     * @param templateTLE template TLE
     * @since 14.0
     */
    @DefaultDataContext
    public LeastSquaresTleGenerationAlgorithm(final TLE templateTLE) {
        this(templateTLE, DEFAULT_MAX_ITERATIONS);
    }

    /**
     * Default constructor.
     * <p>Uses:
     * <ul>
     * <li>the {@link DataContext#getDefault() default data context}</li>
     * <li>the {@link LevenbergMarquardtOptimizer}</li>
     * </ul>
     * @param templateTLE template TLE
     * @param maxIterations maximum number of iterations for convergence
     * @since 14.0
     */
    @DefaultDataContext
    public LeastSquaresTleGenerationAlgorithm(final TLE templateTLE,
                                              final int maxIterations) {
        this(templateTLE, maxIterations, DataContext.getDefault().getTimeScales().getUTC(),
             DataContext.getDefault().getFrames().getTEME());
    }

    /**
     * Constructor.
     * <p>Uses the {@link LevenbergMarquardtOptimizer}.</p>
     * @param templateTLE template TLE
     * @param maxIterations maximum number of iterations for convergence
     * @param utc  UTC time scale
     * @param teme TEME frame
     * @since 14.0
     */
    public LeastSquaresTleGenerationAlgorithm(final TLE templateTLE,
                                              final int maxIterations,
                                              final TimeScale utc,
                                              final Frame teme) {
        this(templateTLE, utc, teme,
             new LeastSquaresConverter(new TLETheory(utc, teme),
                                       new LevenbergMarquardtOptimizer(),
                                       LeastSquaresConverter.DEFAULT_THRESHOLD,
                                       maxIterations));
    }

    /**
     * Constructor.
     * <p>Enables to select the {@link LeastSquaresOptimizer optimizer}
     * for the {@link LeastSquaresConverter least-squares converter}.</p>
     * @param templateTLE template TLE
     * @param utc  UTC time scale
     * @param teme TEME frame
     * @param converter osculating to mean orbit converter using a least-squares algorithm
     * @since 14.0
     */
    public LeastSquaresTleGenerationAlgorithm(final TLE templateTLE,
                                              final TimeScale utc,
                                              final Frame teme,
                                              final LeastSquaresConverter converter) {
        super(templateTLE, teme, converter);
        converter.setMeanTheory(new TLETheory(utc, teme));
        this.converter = converter;
    }

    /**
     * Get the Root Mean Square of the TLE estimation.
     * <p>
     * Be careful that the RMS is updated each time the
     * {@link #reset(Orbit)} method is called.
     * </p>
     * @return the RMS
     */
    public double getRms() {
        return converter.getRMS();
    }

}

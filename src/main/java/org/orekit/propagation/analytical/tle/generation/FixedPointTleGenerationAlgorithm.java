/* Copyright 2002-2025 CS GROUP
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
package org.orekit.propagation.analytical.tle.generation;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.conversion.osc2mean.FixedPointConverter;
import org.orekit.propagation.conversion.osc2mean.TLETheory;
import org.orekit.time.TimeScale;

/**
 * Fixed Point method to reverse SGP4 and SDP4 propagation algorithm
 * and generate a usable TLE from a spacecraft state.
 * <p>
 * Using this algorithm, the B* value is not computed. In other words,
 * the B* value from the template TLE is set to the generated one.
 * </p>
 * @author Thomas Paulet
 * @author Bryan Cazabonne
 * @since 12.0
 */
public class FixedPointTleGenerationAlgorithm extends TleGenerationAlgorithm {

    /** Default value for epsilon. */
    public static final double EPSILON_DEFAULT = 1.0e-10;

    /** Default value for maxIterations. */
    public static final int MAX_ITERATIONS_DEFAULT = 100;

    /** Default value for scale. */
    public static final double SCALE_DEFAULT = 1.0;

    /**
     * Default constructor.
     * <p>
     * Uses the {@link DataContext#getDefault() default data context}
     * as well as {@link #EPSILON_DEFAULT}, {@link #MAX_ITERATIONS_DEFAULT},
     * {@link #SCALE_DEFAULT} for method convergence.
     * </p>
     * @param templateTLE template TLE
     * @since 14.0
     */
    @DefaultDataContext
    public FixedPointTleGenerationAlgorithm(final TLE templateTLE) {
        this(templateTLE, EPSILON_DEFAULT, MAX_ITERATIONS_DEFAULT, SCALE_DEFAULT);
    }

    /**
     * Constructor.
     * <p>
     * Uses the {@link DataContext#getDefault() default data context}.
     * </p>
     * @param templateTLE template TLE
     * @param epsilon used to compute threshold for convergence check
     * @param maxIterations maximum number of iterations for convergence
     * @param scale scale factor of the Fixed Point algorithm
     * @since 14.0
     */
    @DefaultDataContext
    public FixedPointTleGenerationAlgorithm(final TLE templateTLE,
                                            final double epsilon,
                                            final int maxIterations,
                                            final double scale) {
        this(templateTLE, epsilon, maxIterations, scale,
             DataContext.getDefault().getTimeScales().getUTC(),
             DataContext.getDefault().getFrames().getTEME());
    }

    /**
     * Constructor.
     * @param templateTLE template TLE
     * @param epsilon used to compute threshold for convergence check
     * @param maxIterations maximum number of iterations for convergence
     * @param scale scale factor of the Fixed Point algorithm
     * @param utc UTC time scale
     * @param teme TEME frame
     * @since 14.0
     */
    public FixedPointTleGenerationAlgorithm(final TLE templateTLE,
                                            final double epsilon,
                                            final int maxIterations,
                                            final double scale,
                                            final TimeScale utc,
                                            final Frame teme) {
        super(templateTLE, teme,
              new FixedPointConverter(new TLETheory(utc, teme), epsilon, maxIterations, scale));
    }

}

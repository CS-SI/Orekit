/* Copyright 2020-2025 Exotrail
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
package org.orekit.propagation.conversion.averaging.converters;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.generation.FixedPointTleGenerationAlgorithm;
import org.orekit.time.UTCScale;
import org.orekit.propagation.conversion.averaging.SGP4OrbitalState;

/**
 * Class for osculating-to-averaged conversion according to "SGP4" theory, meant as the set of
 * models associated to Two-Line Elements.
 *
 * @author Romain Serra
 * @see org.orekit.propagation.analytical.tle.TLEPropagator
 * @see SGP4OrbitalState
 * @since 12.1
 */
public class OsculatingToSGP4Converter
        extends FixedPointOsculatingToAveragedConverter<SGP4OrbitalState> {

    /** First line of arbitrary TLE. Should not impact conversion. */
    private static final String TEMPLATE_LINE_1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
    /** Second line of arbitrary TLE. Should not impact conversion. */
    private static final String TEMPLATE_LINE_2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";

    /** Scale for fixed-point algorithm. */
    private final double scale;
    /** UTC time scale. */
    private final UTCScale utc;
    /** TEME frame. */
    private final Frame teme;

    /**
     * Constructor with default data context.
     */
    @DefaultDataContext
    public OsculatingToSGP4Converter() {
        this(DataContext.getDefault());
    }

    /**
     * Constructor with default parameters for fixed-point algorithm.
     * @param dataContext data context
     */
    public OsculatingToSGP4Converter(final DataContext dataContext) {
        this(DEFAULT_EPSILON, DEFAULT_MAX_ITERATIONS, FixedPointTleGenerationAlgorithm.SCALE_DEFAULT,
                dataContext);
    }

    /**
     * Constructor.
     * @param epsilon convergence threshold
     * @param maxIterations maximum number of iterations
     * @param scale scale
     * @param dataContext data context
     */
    public OsculatingToSGP4Converter(final double epsilon, final int maxIterations,
                                     final double scale, final DataContext dataContext) {
        super(epsilon, maxIterations);
        this.scale = scale;
        this.utc = dataContext.getTimeScales().getUTC();
        this.teme = dataContext.getFrames().getTEME();
    }

    /** {@inheritDoc} */
    @Override
    public SGP4OrbitalState convertToAveraged(final Orbit osculatingOrbit) {
        final FixedPointTleGenerationAlgorithm fixedPointAlgorithm = new FixedPointTleGenerationAlgorithm(getEpsilon(),
                getMaxIterations(), scale, utc, teme);
        final SpacecraftState osculatingState = new SpacecraftState(osculatingOrbit);
        final TLE templateTLe = new TLE(TEMPLATE_LINE_1, TEMPLATE_LINE_2, utc);
        final TLE tle = fixedPointAlgorithm.generate(osculatingState, templateTLe);
        return SGP4OrbitalState.of(tle, teme);
    }
}

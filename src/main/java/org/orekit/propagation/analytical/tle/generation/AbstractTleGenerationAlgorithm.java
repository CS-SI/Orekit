/* Copyright 2022-2025 Thales Alenia Space
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

import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.conversion.osc2mean.OsculatingToMeanConverter;
import org.orekit.utils.ParameterDriver;

/**
 * Base clase for TLE generation algorithm.
 * @param <C> type of the converter
 * @author Luc Maisonobe
 * @since 14.0
 */
public abstract class AbstractTleGenerationAlgorithm<C extends OsculatingToMeanConverter>
    implements TleGenerationAlgorithm {

    /** Osculating to mean orbit converter. */
    private final C converter;

    /** Default constructor.
     * @param converter osculating to mean orbit converter
     */
    protected AbstractTleGenerationAlgorithm(final C converter) {
        this.converter = converter;
    }

    /** Get the osculating to mean orbit converter.
     * @return osculating to mean orbit converter
     */
    protected C getConverter() {
        return converter;
    }

    /** {@inheritDoc} */
    @Override
    public TLE generate(final SpacecraftState state, final TLE templateTLE) {
        final KeplerianOrbit mean = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(converter.convertToMean(state.getOrbit()));
        final TLE tle = TleGenerationUtil.newTLE(mean, templateTLE, templateTLE.getBStar(mean.getDate()));
        // reset estimated parameters from template to generated tle
        for (final ParameterDriver templateDrivers : templateTLE.getParametersDrivers()) {
            if (templateDrivers.isSelected()) {
                // set to selected for the new TLE
                tle.getParameterDriver(templateDrivers.getName()).setSelected(true);
            }
        }
        return tle;
    }

}

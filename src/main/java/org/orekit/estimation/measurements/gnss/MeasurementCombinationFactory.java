/* Copyright 2002-2023 CS GROUP
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
package org.orekit.estimation.measurements.gnss;

import org.orekit.gnss.SatelliteSystem;

/** Factory for predefined combination of measurements.
 * <p>
 * This is a utility class, so its constructor is private.
 * </p>
 * @author Bryan Cazabonne
 * @since 10.1
 */
public class MeasurementCombinationFactory {

    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private MeasurementCombinationFactory() {
    }

    /**
     * Get the Wide-Lane combination of measurements.
     * @param system satellite system
     * @return Wide-Lane combination
     */
    public static WideLaneCombination getWideLaneCombination(final SatelliteSystem system) {
        return new WideLaneCombination(system);
    }

    /**
     * Get the Narrow-Lane combination of measurements.
     * @param system satellite system
     * @return Narrow-Lane combination
     */
    public static NarrowLaneCombination getNarrowLaneCombination(final SatelliteSystem system) {
        return new NarrowLaneCombination(system);
    }

    /**
     * Get the Ionosphere-Free combination of measurements.
     * @param system satellite system
     * @return Ionosphere-Lane combination
     */
    public static IonosphereFreeCombination getIonosphereFreeCombination(final SatelliteSystem system) {
        return new IonosphereFreeCombination(system);
    }

    /**
     * Get the Geometry-Free combination of measurements.
     * @param system satellite system
     * @return Geometry-Free combination
     */
    public static GeometryFreeCombination getGeometryFreeCombination(final SatelliteSystem system) {
        return new GeometryFreeCombination(system);
    }

    /**
     * Get the Melbourne-Wübbena combination of measurements.
     * @param system satellite system
     * @return Melbourne-Wübbena combination
     */
    public static MelbourneWubbenaCombination getMelbourneWubbenaCombination(final SatelliteSystem system) {
        return new MelbourneWubbenaCombination(system);
    }

    /**
     * Get the phase minus code combination of measurements.
     * @param system satellite system
     * @return phase minus code combination
     */
    public static PhaseMinusCodeCombination getPhaseMinusCodeCombination(final SatelliteSystem system) {
        return new PhaseMinusCodeCombination(system);
    }

    /**
     * Get the GRAPHIC combination of measurements.
     * @param system satellite system
     * @return phase minus code combination
     */
    public static GRAPHICCombination getGRAPHICCombination(final SatelliteSystem system) {
        return new GRAPHICCombination(system);
    }

}

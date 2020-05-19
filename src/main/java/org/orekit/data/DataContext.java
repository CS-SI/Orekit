/* Contributed in the public domain.
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
package org.orekit.data;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.CelestialBodies;
import org.orekit.forces.gravity.potential.GravityFields;
import org.orekit.frames.Frames;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.GeoMagneticFields;
import org.orekit.models.earth.ionosphere.KlobucharIonoCoefficientsLoader;
import org.orekit.time.TimeScales;
import org.orekit.time.TimeScalesFactory;

/**
 * Provides auxiliary data for portions of the application.
 *
 * @author Evan Ward
 * @since 10.1
 */
public interface DataContext {

    /**
     * Get the default data context that is used to implement the static factories ({@link
     * TimeScalesFactory}, {@link FramesFactory}, etc) and loaders that feed themselves
     * (e.g. {@link KlobucharIonoCoefficientsLoader}). It is used to maintain
     * compatibility with auxiliary data loading in Orekit 10.0.
     *
     * @return Orekit's default data context.
     */
    @DefaultDataContext
    static LazyLoadedDataContext getDefault() {
        return DefaultDataContextHolder.getInstance();
    }

    /**
     * Set the default data context that is used to implement Orekit's static factories.
     *
     * <p> Calling this method will not modify any instances already retrieved from
     * Orekit's static factories. In general this method should only be called at
     * application start up before any of the static factories are used.
     *
     * @param context the new data context.
     * @see #getDefault()
     */
    static void setDefault(final LazyLoadedDataContext context) {
        DefaultDataContextHolder.setInstance(context);
    }

    /**
     * Get a factory for constructing {@link org.orekit.time.TimeScale}s based on the auxiliary data in
     * this context.
     *
     * @return the set of common time scales using this data context.
     */
    TimeScales getTimeScales();

    /**
     * Get a factory constructing {@link org.orekit.frames.Frame}s based on the auxiliary data in this
     * context.
     *
     * @return the set of common reference frames using this data context.
     */
    Frames getFrames();

    /**
     * Get a factory constructing {@link org.orekit.bodies.CelestialBody}s based on the auxiliary data in
     * this context.
     *
     * @return the set of common celestial bodies using this data context.
     */
    CelestialBodies getCelestialBodies();

    /**
     * Get a factory constructing gravity fields based on the auxiliary data in this
     * context.
     *
     * @return the gravity fields using this data context.
     */
    GravityFields getGravityFields();

    /**
     * Get a factory constructing {@link org.orekit.models.earth.GeoMagneticField}s based on the auxiliary
     * data in this context.
     *
     * @return the geomagnetic fields using this data context.
     */
    GeoMagneticFields getGeoMagneticFields();

}

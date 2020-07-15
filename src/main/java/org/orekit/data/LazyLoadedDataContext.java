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

import org.orekit.bodies.LazyLoadedCelestialBodies;
import org.orekit.forces.gravity.potential.LazyLoadedGravityFields;
import org.orekit.frames.Frame;
import org.orekit.frames.LazyLoadedEop;
import org.orekit.frames.LazyLoadedFrames;
import org.orekit.models.earth.LazyLoadedGeoMagneticFields;
import org.orekit.time.LazyLoadedTimeScales;

/**
 * A data context that aims to match the behavior of Orekit 10.0 regarding auxiliary data.
 * This data context only loads auxiliary data when it is first accessed. It allows data
 * loaders to be added before the data is loaded.
 *
 * @author Evan Ward
 * @since 10.1
 */
public class LazyLoadedDataContext implements DataContext {

    /** The data provider manager. */
    private final DataProvidersManager dataProvidersManager;
    /** EOP loader. */
    private final LazyLoadedEop eop;
    /** The time scales. */
    private final LazyLoadedTimeScales timeScales;
    /** The reference frames. */
    private LazyLoadedFrames frames;
    /** The celestial bodies. */
    private LazyLoadedCelestialBodies bodies;
    /** The gravity fields. */
    private final LazyLoadedGravityFields gravityFields;
    /** The magnetic fields. */
    private final LazyLoadedGeoMagneticFields geoMagneticFields;

    /**
     * Create a new data context that only loads auxiliary data when it is first accessed
     * and allows configuration of the auxiliary data sources until then.
     */
    public LazyLoadedDataContext() {
        this.dataProvidersManager = new DataProvidersManager();
        this.eop = new LazyLoadedEop(dataProvidersManager);
        this.timeScales = new LazyLoadedTimeScales(eop);
        this.gravityFields =
                new LazyLoadedGravityFields(dataProvidersManager, timeScales.getTT());
        this.geoMagneticFields = new LazyLoadedGeoMagneticFields(dataProvidersManager);
        // creating Frames and CelestialBodies here creates an initialization problem for
        // DataContext.getDefault(). Delay creating them until they are used for the first
        // time.
    }

    /**
     * Get the provider of auxiliary data for this data context.
     *
     * @return the provider that supplies auxiliary data to all of the other methods of
     * this data context.
     */
    public DataProvidersManager getDataProvidersManager() {
        return dataProvidersManager;
    }

    @Override
    public LazyLoadedTimeScales getTimeScales() {
        return timeScales;
    }

    @Override
    public LazyLoadedFrames getFrames() {
        synchronized (this) {
            if (this.frames == null) {
                this.frames = new LazyLoadedFrames(
                                                   eop, getTimeScales(), getCelestialBodies());
            }
            return this.frames;
        }
    }

    @Override
    public LazyLoadedCelestialBodies getCelestialBodies() {
        synchronized (this) {
            if (this.bodies == null) {
                final Frame gcrf = Frame.getRoot();
                this.bodies = new LazyLoadedCelestialBodies(
                                                            getDataProvidersManager(), getTimeScales(), gcrf);
            }
            return this.bodies;
        }
    }

    @Override
    public LazyLoadedGravityFields getGravityFields() {
        return gravityFields;
    }

    @Override
    public LazyLoadedGeoMagneticFields getGeoMagneticFields() {
        return geoMagneticFields;
    }

}

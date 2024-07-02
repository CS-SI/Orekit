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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.potential.LazyLoadedGravityFields;
import org.orekit.frames.LazyLoadedFrames;
import org.orekit.models.earth.LazyLoadedGeoMagneticFields;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.LazyLoadedTimeScales;

/**
 * A data context that always throws a runtime exception when it's methods are used. Can
 * be useful for determining if the default data context is used. E.g. {@code
 * DataContext.setDefault(new ExceptionalDataContext());}. The following classes have
 * static fields that are initialized using the default data context:
 *
 * <ul>
 *     <li>{@link AbsoluteDate}
 * </ul>
 *
 * @author Evan Ward
 * @see DataContext#setDefault(LazyLoadedDataContext)
 * @since 10.1
 */
public class ExceptionalDataContext extends LazyLoadedDataContext implements DataContext {

    /** Empty constructor.
     * <p>
     * This constructor is not strictly necessary, but it prevents spurious
     * javadoc warnings with JDK 18 and later.
     * </p>
     * @since 12.0
     */
    public ExceptionalDataContext() {
        // nothing to do
    }

    @Override
    public LazyLoadedTimeScales getTimeScales() {
        throw new OrekitException(OrekitMessages.EXCEPTIONAL_DATA_CONTEXT);
    }

    @Override
    public LazyLoadedFrames getFrames() {
        throw new OrekitException(OrekitMessages.EXCEPTIONAL_DATA_CONTEXT);
    }

    @Override
    public LazyLoadedCelestialBodies getCelestialBodies() {
        throw new OrekitException(OrekitMessages.EXCEPTIONAL_DATA_CONTEXT);
    }

    @Override
    public LazyLoadedGravityFields getGravityFields() {
        throw new OrekitException(OrekitMessages.EXCEPTIONAL_DATA_CONTEXT);
    }

    @Override
    public LazyLoadedGeoMagneticFields getGeoMagneticFields() {
        throw new OrekitException(OrekitMessages.EXCEPTIONAL_DATA_CONTEXT);
    }

}

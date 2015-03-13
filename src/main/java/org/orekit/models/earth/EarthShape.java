/* Contributed in the public domain.
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.models.earth;

import org.orekit.bodies.BodyShape;
import org.orekit.models.earth.ReferenceEllipsoid;

/**
 * All models of Earth's shape have some common properties that are not shared with
 * arbitrary {@link BodyShape}s. In particular, an ellipsoidal (or spherical) model is
 * used to compute latitude and longitude.
 *
 * @author Evan Ward
 * @see #getEllipsoid()
 */
public interface EarthShape extends BodyShape {

    /**
     * Get the underlying ellipsoid model that defines latitude and longitude. If the
     * height component of a {@link org.orekit.bodies.GeodeticPoint} is not needed,
     * then using the ellipsoid will provide the quickest transformation.
     *
     * @return the reference ellipsoid. May be {@code this}, but never {@code null}.
     */
    ReferenceEllipsoid getEllipsoid();

}

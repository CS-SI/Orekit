/* Copyright 2022-2025 Luc Maisonobe
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
package org.orekit.utils;

/** Container for azimut/elevation/range coordinates as seen from a ground point.
 * @see org.orekit.frames.TopocentricFrame
 * @since 12.0
 */
public class TrackingCoordinates {

    /** Azimuth. */
    private final double azimuth;

    /** Elevation. */
    private final double elevation;

    /** Range. */
    private final double range;

    /** Simple constructor.
     * @param azimuth azimuth
     * @param elevation elevation
     * @param range range
     */
    public TrackingCoordinates(final double azimuth, final double elevation, final double range) {
        this.azimuth   = azimuth;
        this.elevation = elevation;
        this.range     = range;
    }

    /** Get the azimuth.
     * <p>The azimuth is the angle between the North direction at local point and
     * the projection in local horizontal plane of the direction from local point
     * to given point. Azimuth angles are counted clockwise, i.e positive towards the East.</p>
     * @return azimuth
     */
    public double getAzimuth() {
        return azimuth;
    }

    /** Get the elevation.
     * <p>The elevation is the angle between the local horizontal and
     * the direction from local point to given point.</p>
     * @return elevation
     */
    public double getElevation() {
        return elevation;
    }

    /** Get the range.
     * @return range
     */
    public double getRange() {
        return range;
    }

}




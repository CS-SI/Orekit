/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.bodies;

import java.io.Serializable;

import org.apache.commons.math.util.MathUtils;

/** Point location relative to a 2D body surface.
 * <p>This class is a simple immutable container,
 * it does not provide any processing method.</p>
 * @see BodyShape
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class GeodeticPoint implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -7038539219231406929L;

    /** Longitude of the point (rad). */
    private final double longitude;

    /** Latitude of the point (rad). */
    private final double latitude;

    /** Altitude of the point (m). */
    private final double altitude;

    /** Build a new instance.
     * @param longitude longitude of the point
     * @param latitude of the point
     * @param altitude altitude of the point
     */
    public GeodeticPoint(final double longitude, final double latitude,
                         final double altitude) {
        this.longitude = MathUtils.normalizeAngle(longitude, 0.);
        this.latitude  = MathUtils.normalizeAngle(latitude, 0.);
        this.altitude  = altitude;
    }

    /** Get the longitude.
     * @return longitude
     */
    public double getLongitude() {
        return longitude;
    }

    /** Get the latitude.
     * @return latitude
     */
    public double getLatitude() {
        return latitude;
    }

    /** Get the altitude.
     * @return altitude
     */
    public double getAltitude() {
        return altitude;
    }

}

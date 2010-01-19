/* Copyright 2002-2010 CS Communication & Systèmes
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

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.util.MathUtils;

/** Point location relative to a 2D body surface.
 * <p>Instance of this class are guaranteed to be immutable.</p>
 * @see BodyShape
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class GeodeticPoint implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 7862466825590075399L;

    /** Latitude of the point (rad). */
    private final double latitude;

    /** Longitude of the point (rad). */
    private final double longitude;

    /** Altitude of the point (m). */
    private final double altitude;

    /** Zenith direction. */
    private transient Vector3D zenith;

    /** Nadir direction. */
    private transient Vector3D nadir;

    /** North direction. */
    private transient Vector3D north;

    /** South direction. */
    private transient Vector3D south;

    /** East direction. */
    private transient Vector3D east;

    /** West direction. */
    private transient Vector3D west;

    /** Build a new instance.
     * @param latitude of the point
     * @param longitude longitude of the point
     * @param altitude altitude of the point
     */
    public GeodeticPoint(final double latitude, final double longitude,
                         final double altitude) {
        this.latitude  = MathUtils.normalizeAngle(latitude,  0);
        this.longitude = MathUtils.normalizeAngle(longitude, 0);
        this.altitude  = altitude;
    }

    /** Get the latitude.
     * @return latitude
     */
    public double getLatitude() {
        return latitude;
    }

    /** Get the longitude.
     * @return longitude
     */
    public double getLongitude() {
        return longitude;
    }

    /** Get the altitude.
     * @return altitude
     */
    public double getAltitude() {
        return altitude;
    }

    /** Get the direction above the point, expressed in parent shape frame.
     * <p>The zenith direction is defined as the normal to local horizontal plane.</p>
     * @return unit vector in the zenith direction
     * @see #getNadir()
     */
    public Vector3D getZenith() {
        if (zenith == null) {
            final double cosLat = Math.cos(latitude);
            final double sinLat = Math.sin(latitude);
            final double cosLon = Math.cos(longitude);
            final double sinLon = Math.sin(longitude);
            zenith = new Vector3D(cosLon * cosLat, sinLon * cosLat, sinLat);
        }
        return zenith;
    }

    /** Get the direction below the point, expressed in parent shape frame.
     * <p>The nadir direction is the opposite of zenith direction.</p>
     * @return unit vector in the nadir direction
     * @see #getZenith()
     */
    public Vector3D getNadir() {
        if (nadir == null) {
            nadir = getZenith().negate();
        }
        return nadir;
    }

    /** Get the direction to the north of point, expressed in parent shape frame.
     * <p>The north direction is defined in the horizontal plane
     * (normal to zenith direction) and following the local meridian.</p>
     * @return unit vector in the north direction
     * @see #getSouth()
     */
    public Vector3D getNorth() {
        if (north == null) {
            final double cosLat = Math.cos(latitude);
            final double sinLat = Math.sin(latitude);
            final double cosLon = Math.cos(longitude);
            final double sinLon = Math.sin(longitude);
            north = new Vector3D(-cosLon * sinLat, -sinLon * sinLat, cosLat);
        }
        return north;
    }

    /** Get the direction to the south of point, expressed in parent shape frame.
     * <p>The south direction is the opposite of north direction.</p>
     * @return unit vector in the south direction
     * @see #getNorth()
     */
    public Vector3D getSouth() {
        if (south == null) {
            south = getNorth().negate();
        }
        return south;
    }

    /** Get the direction to the east of point, expressed in parent shape frame.
     * <p>The east direction is defined in the horizontal plane
     * in order to complete direct triangle (east, north, zenith).</p>
     * @return unit vector in the east direction
     * @see #getWest()
     */
    public Vector3D getEast() {
        if (east == null) {
            east = new Vector3D(-Math.sin(longitude), Math.cos(longitude), 0);
        }
        return east;
    }

    /** Get the direction to the west of point, expressed in parent shape frame.
     * <p>The west direction is the opposite of east direction.</p>
     * @return unit vector in the west direction
     * @see #getEast()
     */
    public Vector3D getWest() {
        if (west == null) {
            west = getEast().negate();
        }
        return west;
    }

}

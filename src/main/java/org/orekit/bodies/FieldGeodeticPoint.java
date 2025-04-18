/* Copyright 2002-2025 CS GROUP
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
package org.orekit.bodies;

import java.text.NumberFormat;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.CompositeFormat;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathUtils;

/** Point location relative to a 2D body surface, using {@link CalculusFieldElement}.
 * <p>Instance of this class are guaranteed to be immutable.</p>
 * @param <T> the type of the field elements
 * @since 7.1
 * @see BodyShape
 * @author Luc Maisonobe
 */
public class FieldGeodeticPoint<T extends CalculusFieldElement<T>> {

    /** Latitude of the point (rad). */
    private final T latitude;

    /** Longitude of the point (rad). */
    private final T longitude;

    /** Altitude of the point (m). */
    private final T altitude;

    /** Zenith direction. */
    private FieldVector3D<T> zenith;

    /** Nadir direction. */
    private FieldVector3D<T> nadir;

    /** North direction. */
    private FieldVector3D<T> north;

    /** South direction. */
    private FieldVector3D<T> south;

    /** East direction. */
    private FieldVector3D<T> east;

    /** West direction. */
    private FieldVector3D<T> west;

    /** Build a new instance.
     * <p>
     * The angular coordinates will be normalized so that
     * the latitude is between ±π/2 and the longitude is between ±π.
     * </p>
     * @param latitude latitude of the point (rad)
     * @param longitude longitude of the point (rad)
     * @param altitude altitude of the point (m)
     */
    public FieldGeodeticPoint(final T latitude, final T longitude,
                              final T altitude) {
        final T zero = latitude.getField().getZero();
        final T pi   = zero.getPi();
        T lat = MathUtils.normalizeAngle(latitude,  pi.multiply(0.5));
        T lon = MathUtils.normalizeAngle(longitude, zero);
        if (lat.getReal() > pi.multiply(0.5).getReal()) {
            // latitude is beyond the pole -> add 180 to longitude
            lat = pi.subtract(lat);
            lon = MathUtils.normalizeAngle(longitude.add(pi), zero);
        }
        this.latitude  = lat;
        this.longitude = lon;
        this.altitude  = altitude;
    }

    /** Build a new instance from a {@link GeodeticPoint}.
     * @param field field to which the elements belong
     * @param geodeticPoint geodetic point to convert
     * @since 12.1
     */
    public FieldGeodeticPoint(final Field<T> field, final GeodeticPoint geodeticPoint) {
        this(field.getZero().newInstance(geodeticPoint.getLatitude()),
             field.getZero().newInstance(geodeticPoint.getLongitude()),
             field.getZero().newInstance(geodeticPoint.getAltitude()));
    }

    /** Get the latitude.
     * @return latitude, an angular value in the range [-π/2, π/2]
     */
    public T getLatitude() {
        return latitude;
    }

    /** Get the longitude.
     * @return longitude, an angular value in the range [-π, π]
     */
    public T getLongitude() {
        return longitude;
    }

    /** Get the altitude.
     * @return altitude
     */
    public T getAltitude() {
        return altitude;
    }

    /** Get the direction above the point, expressed in parent shape frame.
     * <p>The zenith direction is defined as the normal to local horizontal plane.</p>
     * @return unit vector in the zenith direction
     * @see #getNadir()
     */
    public FieldVector3D<T> getZenith() {
        if (zenith == null) {
            final FieldSinCos<T> scLat = FastMath.sinCos(latitude);
            final FieldSinCos<T> scLon = FastMath.sinCos(longitude);
            zenith = new FieldVector3D<>(scLon.cos().multiply(scLat.cos()),
                                         scLon.sin().multiply(scLat.cos()),
                                         scLat.sin());
        }
        return zenith;
    }

    /** Get the direction below the point, expressed in parent shape frame.
     * <p>The nadir direction is the opposite of zenith direction.</p>
     * @return unit vector in the nadir direction
     * @see #getZenith()
     */
    public FieldVector3D<T> getNadir() {
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
    public FieldVector3D<T> getNorth() {
        if (north == null) {
            final FieldSinCos<T> scLat = FastMath.sinCos(latitude);
            final FieldSinCos<T> scLon = FastMath.sinCos(longitude);
            north = new FieldVector3D<>(scLon.cos().multiply(scLat.sin()).negate(),
                                        scLon.sin().multiply(scLat.sin()).negate(),
                                        scLat.cos());
        }
        return north;
    }

    /** Get the direction to the south of point, expressed in parent shape frame.
     * <p>The south direction is the opposite of north direction.</p>
     * @return unit vector in the south direction
     * @see #getNorth()
     */
    public FieldVector3D<T> getSouth() {
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
    public FieldVector3D<T> getEast() {
        if (east == null) {
            final FieldSinCos<T> scLon = FastMath.sinCos(longitude);
            east = new FieldVector3D<>(scLon.sin().negate(),
                                       scLon.cos(),
                                       longitude.getField().getZero());
        }
        return east;
    }

    /** Get the direction to the west of point, expressed in parent shape frame.
     * <p>The west direction is the opposite of east direction.</p>
     * @return unit vector in the west direction
     * @see #getEast()
     */
    public FieldVector3D<T> getWest() {
        if (west == null) {
            west = getEast().negate();
        }
        return west;
    }

    /**
     * Get non-Field equivalent.
     * @return geodetic point
     * @since 12.2
     */
    public GeodeticPoint toGeodeticPoint() {
        return new GeodeticPoint(latitude.getReal(), longitude.getReal(), altitude.getReal());
    }

    @Override
    public boolean equals(final Object object) {
        if (object instanceof FieldGeodeticPoint<?>) {
            @SuppressWarnings("unchecked")
            final FieldGeodeticPoint<T> other = (FieldGeodeticPoint<T>) object;
            return getLatitude().equals(other.getLatitude()) &&
                   getLongitude().equals(other.getLongitude()) &&
                   getAltitude().equals(other.getAltitude());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getLatitude().hashCode() ^
               getLongitude().hashCode() ^
               getAltitude().hashCode();
    }

    @Override
    public String toString() {
        final NumberFormat format = CompositeFormat.getDefaultNumberFormat();
        return "{lat: " +
               format.format(FastMath.toDegrees(getLatitude().getReal())) +
               " deg, lon: " +
               format.format(FastMath.toDegrees(getLongitude().getReal())) +
               " deg, alt: " +
               format.format(getAltitude().getReal()) +
               "}";
    }

}

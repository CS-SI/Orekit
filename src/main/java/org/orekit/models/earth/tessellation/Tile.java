/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.models.earth.tessellation;

import java.io.Serializable;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;

/** Simple data structure for a quadrilateral tile shape on a body surface.
 * <p>
 * This class is devoted to simple usage only. It assumes the edges
 * are strictly between 0 and π radians and that the angles between
 * edges are also strictly between 0 and π radians.
 * </p>
 * @see AlongTrackAiming
 * @see ConstantAzimuthAiming
 * @author Luc Maisonobe
 */
public class Tile implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20150313L;

    /** First vertex. */
    private final GeodeticPoint v0;

    /** Second vertex. */
    private final GeodeticPoint v1;

    /** Third vertex. */
    private final GeodeticPoint v2;

    /** Fourth vertex. */
    private final GeodeticPoint v3;

    /** Create a tile.
     * <p>
     * It is caller responsibility o ensure the vertices define a
     * simple non-degenerated tile (i.e. edges are strictly between
     * 0 than π radians and angles between edges are also strictly
     * between 0 and π radians). No checks are performed here.
     * </p>
     * @param v0 first vertex
     * @param v1 second vertex
     * @param v2 third vertex
     * @param v3 fourth vertex
     */
    public Tile(final GeodeticPoint v0, final GeodeticPoint v1,
                final GeodeticPoint v2, final GeodeticPoint v3) {
        this.v0   = v0;
        this.v1   = v1;
        this.v2   = v2;
        this.v3   = v3;
    }

    /** Get the four vertices.
     * @return four vertices
     */
    public GeodeticPoint[] getVertices() {
        return new GeodeticPoint[] {
            v0, v1, v2, v3
        };
    }

    /** Get an interpolated point inside the tile.
     * <p>
     * The interpolated point is based on bilinear interpolations
     * along the body surface assumed to be <em>spherical</em>,
     * and along the vertical axis.
     * </p>
     * <p>
     * The interpolation parameters are chosen such that
     * (u = 0, v = 0) maps to vertex v0, (u = 1, v = 0) maps
     * to vertex v1, (u = 1, v = 1) maps to vertex v2 and
     * (u = 0, v = 1) maps to vertex v3.
     * </p>
     * @param u first interpolation parameter (should be between
     * 0 and 1 to remain inside the tile)
     * @param v second interpolation parameter (should be between
     * 0 and 1 to remain inside the tile)
     * @return interpolated point
     */
    public GeodeticPoint getInterpolatedPoint(final double u, final double v) {

        // bilinear interpolation along a spherical shape
        final Vector3D pu0 = interpolate(v0.getZenith(), v1.getZenith(), u);
        final Vector3D pu1 = interpolate(v3.getZenith(), v2.getZenith(), u);
        final Vector3D puv = interpolate(pu0, pu1, v);

        // bilinear interpolation of altitude
        final double hu0 = v1.getAltitude() * u + v0.getAltitude() * (1 - u);
        final double hu1 = v2.getAltitude() * u + v3.getAltitude() * (1 - u);
        final double huv = hu1 * v + hu0 * (1 - v);

        // create interpolated point
        return new GeodeticPoint(puv.getDelta(), puv.getAlpha(), huv);

    }

    /** Interpolate a vector along a unit sphere.
     * @param p0 first base unit vector
     * @param p1 second base unit vector
     * @param r interpolation parameter (0 for p0, 1 for p1)
     * @return interpolated unit vector
     */
    private Vector3D interpolate(final Vector3D p0, final Vector3D p1, final double r) {

        // find all interpolation angles
        final double theta       = Vector3D.angle(p0, p1);
        final double alpha       = r * theta;
        final double thetaMAlpha = (1 - r) * theta;

        final double sinTheta       = FastMath.sin(theta);
        final double sinAlpha       = FastMath.sin(alpha);
        final double sinThetaMAlpha = FastMath.sin(thetaMAlpha);

        // interpolate
        return new Vector3D(sinThetaMAlpha / sinTheta, p0, sinAlpha / sinTheta, p1);

    }

    /** Get the center point.
     * <p>
     * The center points corresponds to {@link
     * #getInterpolatedPoint(double, double) getInterpolatedPoint(0.5, 0.5)}
     * </p>
     * @return center point
     */
    public GeodeticPoint getCenter() {
        return getInterpolatedPoint(0.5, 0.5);
    }

}

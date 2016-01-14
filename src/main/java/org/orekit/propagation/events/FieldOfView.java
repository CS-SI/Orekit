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
package org.orekit.propagation.events;

import java.io.Serializable;

import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.geometry.enclosing.EnclosingBall;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.partitioning.Region;
import org.apache.commons.math3.geometry.partitioning.RegionFactory;
import org.apache.commons.math3.geometry.spherical.twod.S2Point;
import org.apache.commons.math3.geometry.spherical.twod.Sphere2D;
import org.apache.commons.math3.geometry.spherical.twod.SphericalPolygonsSet;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.SphericalPolygonsSetTransferObject;

/** Class representing a spacecraft sensor Field Of View.
 * <p>Fields Of View are zones defined on the unit sphere centered on the
 * spacecraft. They can have any shape, they can be split in several
 * non-connected patches and can have holes.</p>
 * @see org.orekit.propagation.events.FootprintOverlapDetector
 * @author Luc Maisonobe
 * @since 7.1
 */
public class FieldOfView implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20150113L;

    /** Spherical zone. */
    private final transient SphericalPolygonsSet zone;

    /** Margin to apply to the zone. */
    private final double margin;

    /** Spherical cap surrounding the zone. */
    private final transient EnclosingBall<Sphere2D, S2Point> cap;

    /** Build a new instance.
     * @param zone interior of the field of view, in spacecraft frame
     * @param margin angular margin to apply to the zone (if positive,
     * the Field Of View will consider points slightly outside of the
     * zone are still visible)
     */
    public FieldOfView(final SphericalPolygonsSet zone, final double margin) {
        this.zone   = zone;
        this.margin = margin;
        this.cap    = zone.getEnclosingCap();
    }

    /** Build a Field Of View with dihedral shape (i.e. rectangular shape).
     * @param center Direction of the FOV center, in spacecraft frame
     * @param axis1 FOV dihedral axis 1, in spacecraft frame
     * @param halfAperture1 FOV dihedral half aperture angle 1,
     * must be less than π/2, i.e. full dihedra must be smaller then
     * an hemisphere
     * @param axis2 FOV dihedral axis 2, in spacecraft frame
     * @param halfAperture2 FOV dihedral half aperture angle 2,
     * must be less than π/2, i.e. full dihedra must be smaller then
     * an hemisphere
     * @param margin angular margin to apply to the zone (if positive,
     * the Field Of View will consider points slightly outside of the
     * zone are still visible)
     * @exception OrekitException if half aperture is larger than π/2
     */
    public FieldOfView(final Vector3D center,
                       final Vector3D axis1, final double halfAperture1,
                       final Vector3D axis2, final double halfAperture2,
                       final double margin)
        throws OrekitException {

        // build zone
        final RegionFactory<Sphere2D> factory = new RegionFactory<Sphere2D>();
        final double tolerance = 1.0e-12 * FastMath.max(halfAperture1, halfAperture2);
        final Region<Sphere2D> dihedra1 = buildDihedra(factory, tolerance, center, axis1, halfAperture1);
        final Region<Sphere2D> dihedra2 = buildDihedra(factory, tolerance, center, axis2, halfAperture2);
        this.zone = (SphericalPolygonsSet) factory.intersection(dihedra1, dihedra2);

        this.margin = margin;
        this.cap    = zone.getEnclosingCap();

    }

    /** Build Field Of View with a regular polygon shape.
     * @param center center of the polygon (the center is in the inside part)
     * @param meridian point defining the reference meridian for middle of first edge
     * @param insideRadius distance of the edges middle points to the center
     * (the polygon vertices will therefore be farther away from the center)
     * @param n number of sides of the polygon
     * @param margin angular margin to apply to the zone (if positive,
     * the Field Of View will consider points slightly outside of the
     * zone are still visible)
     */
    public FieldOfView(final Vector3D center, final Vector3D meridian,
                       final double insideRadius, final int n, final double margin) {

        // convert the representation based on middle edge points
        // to Apache Commons Math convention based on vertices
        final Rotation r                = new Rotation(center, MathUtils.TWO_PI / n,
                                                       RotationConvention.VECTOR_OPERATOR);
        final Vector3D orthogonal       = Vector3D.crossProduct(Vector3D.crossProduct(center, meridian), center);
        final Vector3D firstEdgeNormal  = new Vector3D( FastMath.sin(insideRadius), center.normalize(),
                                                       -FastMath.cos(insideRadius), orthogonal.normalize());
        final Vector3D secondEdgeNormal = r.applyTo(firstEdgeNormal);
        final Vector3D vertex           = Vector3D.crossProduct(firstEdgeNormal, secondEdgeNormal);
        final double outsideRadius      = Vector3D.angle(center, vertex);
        this.zone = new SphericalPolygonsSet(center, vertex, outsideRadius, n, 1.0e-12 * insideRadius);

        this.margin = margin;

        final S2Point[] support = new S2Point[n];
        support[0] = new S2Point(vertex);
        for (int i = 1; i < n; ++i) {
            support[i] = new S2Point(r.applyTo(support[i - 1].getVector()));
        }
        this.cap = new EnclosingBall<Sphere2D, S2Point>(new S2Point(center), outsideRadius, support);

    }

    /** Build a dihedra.
     * @param factory factory for regions
     * @param tolerance tolerance below which points are considered equal
     * @param center Direction of the FOV center, in spacecraft frame
     * @param axis FOV dihedral axis, in spacecraft frame
     * @param halfAperture FOV dihedral half aperture angle,
     * must be less than π/2, i.e. full dihedra must be smaller then
     * an hemisphere
     * @return dihedra
     * @exception OrekitException if half aperture is larger than π/2
     */
    private Region<Sphere2D> buildDihedra(final RegionFactory<Sphere2D> factory,
                                          final double tolerance, final Vector3D center,
                                          final Vector3D axis, final double halfAperture)
        throws OrekitException {
        if (halfAperture > 0.5 * FastMath.PI) {
            throw new OrekitException(LocalizedFormats.NUMBER_TOO_LARGE,
                                      halfAperture, 0.5 * FastMath.PI);
        }

        final Rotation r = new Rotation(axis, halfAperture, RotationConvention.VECTOR_OPERATOR);
        final Vector3D normalCenterPlane = Vector3D.crossProduct(axis, center);
        final Vector3D normalSidePlus    = r.applyInverseTo(normalCenterPlane);
        final Vector3D normalSideMinus   = r.applyTo(normalCenterPlane.negate());

        return factory.intersection(new SphericalPolygonsSet(normalSidePlus,  tolerance),
                                    new SphericalPolygonsSet(normalSideMinus, tolerance));

    }

    /** Get the interior zone.
     * @return the interior zone
     */
    public SphericalPolygonsSet getZone() {
        return zone;
    }

    /** Get the angular margin to apply (radians).
     * @return the angular margin to apply (radians)
     */
    public double getMargin() {
        return margin;
    }

    /** Get the angular offset of target point with respect to the Field Of View Boundary.
     * <p>
     * The offset is roughly an angle with respect to the closest boundary point,
     * corrected by the margin and using some approximation far from the Field Of View.
     * It is positive if the target is outside of the Field Of view, negative inside,
     * and zero if the point is exactly on the boundary (always taking the margin
     * into account).
     * </p>
     * <p>
     * As Field Of View can have complex shapes that may require long computation,
     * when the target point can be proven to be outside of the Field Of View, a
     * faster but approximate computation is done, that underestimate the offset.
     * This approximation is only performed about 0.01 radians outside of the zone
     * and is designed to still return a positive value if the full accurate computation
     * would return a positive value. When target point is close to the zone (and
     * furthermore when it is inside the zone), the full accurate computation is
     * performed. This setup allows this offset to be used as a reliable way to
     * detect Field Of View boundary crossings, which correspond to sign changes of
     * the offset.
     * </p>
     * @param state current state of the spacecraft on which the Field Of View is defined
     * @param target position of the target in the same frame as the state
     * @return an angular offset negative if the target is visible within the Field Of
     * View and positive if it is outside of the Field Of View, including the margin
     * (note that this cannot take into account interposing bodies)
     */
    public double offsetFromBoundary(final SpacecraftState state, final Vector3D target) {

        // get line of sight in spacecraft frame
        final Vector3D lineOfSightInert = target.subtract(state.getPVCoordinates().getPosition());
        final S2Point lineOfSightSC     = new S2Point(state.toTransform().transformVector(lineOfSightInert));

        // for faster computation, we start using only the surrounding cap, to filter out
        // far away points (which correspond to most of the points if the Field Of View is small)
        final double crudeDistance = cap.getCenter().distance(lineOfSightSC) - cap.getRadius();
        if (crudeDistance - margin > FastMath.max(FastMath.abs(margin), 0.01)) {
            // we know we are strictly outside of the zone,
            // use the crude distance to compute the (positive) return value
            return crudeDistance - margin;
        }

        // we are close, we need to compute carefully the exact offset
        // project the point to the closest zone boundary
        return zone.projectToBoundary(lineOfSightSC).getOffset() - margin;

    }

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DTO(this);
    }

    /** Internal class used only for serialization. */
    private static class DTO implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20150113L;

        /** Proxy for interior zone. */
        private final SphericalPolygonsSetTransferObject zone;

        /** Angular margin. */
        private final double margin;

        /** Simple constructor.
         * @param fov instance to serialize
         */
        private DTO(final FieldOfView fov) {
            this.zone   = new SphericalPolygonsSetTransferObject(fov.zone);
            this.margin = fov.margin;
        }

        /** Replace the deserialized data transfer object with a {@link FieldOfView}.
         * @return replacement {@link FieldOfView}
         */
        private Object readResolve() {
            return new FieldOfView(zone.rebuildZone(), margin);
        }

    }

}

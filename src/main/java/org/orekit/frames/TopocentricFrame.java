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
package org.orekit.frames;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;


/** Topocentric frame.
 * <p>Frame associated to a position at the surface of a body shape.</p>
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public class TopocentricFrame extends Frame implements PVCoordinatesProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = -5997915708080966466L;

    /** Body shape on which the local point is defined. */
    private final BodyShape parentShape;

    /** Point where the topocentric frame is defined. */
    private final GeodeticPoint point;

    /** Simple constructor.
     * @param parentShape body shape on which the local point is defined
     * @param point local surface point where topocentric frame is defined
     * @param name the string representation
     */
    public TopocentricFrame(final BodyShape parentShape, final GeodeticPoint point,
                            final String name) {

        super(parentShape.getBodyFrame(), null, name, false);
        this.parentShape = parentShape;
        this.point = point;

        // Build transformation from body centered frame to topocentric frame:
        // 1. Translation from body center to geodetic point
        final Transform translation = new Transform(parentShape.transform(point).negate());

        // 2. Rotate axes
        final Vector3D Xtopo = getEast();
        final Vector3D Ztopo = getZenith();
        final Transform rotation = new Transform(new Rotation(Xtopo, Ztopo, Vector3D.PLUS_I, Vector3D.PLUS_K),
                                                 Vector3D.ZERO);

        // Compose both transformations
        setTransform(new Transform(translation, rotation));

    }

    /** Get the body shape on which the local point is defined.
     * @return body shape on which the local point is defined
     */
    public BodyShape getParentShape() {
        return parentShape;
    }

    /** Get the zenith direction of topocentric frame, expressed in parent shape frame.
     * <p>The zenith direction is defined as the normal to local horizontal plane.</p>
     * @return unit vector in the zenith direction
     * @see #getNadir()
     */
    public Vector3D getZenith() {
        return point.getZenith();
    }

    /** Get the nadir direction of topocentric frame, expressed in parent shape frame.
     * <p>The nadir direction is the opposite of zenith direction.</p>
     * @return unit vector in the nadir direction
     * @see #getZenith()
     */
    public Vector3D getNadir() {
        return point.getNadir();
    }

   /** Get the north direction of topocentric frame, expressed in parent shape frame.
     * <p>The north direction is defined in the horizontal plane
     * (normal to zenith direction) and following the local meridian.</p>
     * @return unit vector in the north direction
     * @see #getSouth()
     */
    public Vector3D getNorth() {
        return point.getNorth();
    }

    /** Get the south direction of topocentric frame, expressed in parent shape frame.
     * <p>The south direction is the opposite of north direction.</p>
     * @return unit vector in the south direction
     * @see #getNorth()
     */
    public Vector3D getSouth() {
        return point.getSouth();
    }

    /** Get the east direction of topocentric frame, expressed in parent shape frame.
     * <p>The east direction is defined in the horizontal plane
     * in order to complete direct triangle (east, north, zenith).</p>
     * @return unit vector in the east direction
     * @see #getWest()
     */
    public Vector3D getEast() {
        return point.getEast();
    }

    /** Get the west direction of topocentric frame, expressed in parent shape frame.
     * <p>The west direction is the opposite of east direction.</p>
     * @return unit vector in the west direction
     * @see #getEast()
     */
    public Vector3D getWest() {
        return point.getWest();
    }

    /** Get the elevation of a point with regards to the local point.
     * <p>The elevation is the angle between the local horizontal and
     * the direction from local point to given point.</p>
     * @param extPoint point for which elevation shall be computed
     * @param frame frame in which the point is defined
     * @param date computation date
     * @return elevation of the point
     * @exception OrekitException if frames transformations cannot be computed
     */
    public double getElevation(final Vector3D extPoint, final Frame frame,
                               final AbsoluteDate date)
        throws OrekitException {

        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final Vector3D extPointTopo = t.transformPosition(extPoint);

        // Elevation angle is PI/2 - angle between zenith and given point direction
        return Math.asin(extPointTopo.normalize().getZ());
    }

    /** Get the azimuth of a point with regards to the topocentric frame center point.
     * <p>The azimuth is the angle between the North direction at local point and
     * the projection in local horizontal plane of the direction from local point
     * to given point. Azimuth angles are counted clockwise, i.e positive towards the East.</p>
     * @param extPoint point for which elevation shall be computed
     * @param frame frame in which the point is defined
     * @param date computation date
     * @return azimuth of the point
     * @exception OrekitException if frames transformations cannot be computed
     */
    public double getAzimuth(final Vector3D extPoint, final Frame frame,
                             final AbsoluteDate date)
        throws OrekitException {

        // Transform given point from given frame to topocentric frame
        final Transform t = getTransformTo(frame, date).getInverse();
        final Vector3D extPointTopo = t.transformPosition(extPoint);

        // Compute azimuth
        double azimuth = Math.atan2(extPointTopo.getX(), extPointTopo.getY());
        if (azimuth < 0.) {
            azimuth += 2. * Math.PI;
        }
        return azimuth;

    }

    /** Get the range of a point with regards to the topocentric frame center point.
     * @param extPoint point for which range shall be computed
     * @param frame frame in which the point is defined
     * @param date computation date
     * @return range (distance) of the point
     * @exception OrekitException if frames transformations cannot be computed
     */
    public double getRange(final Vector3D extPoint, final Frame frame,
                           final AbsoluteDate date)
        throws OrekitException {

        // Transform given point from given frame to topocentric frame
        final Transform t = getTransformTo(frame, date).getInverse();
        final Vector3D extPointTopo = t.transformPosition(extPoint);

        // Compute range
        return extPointTopo.getNorm();

    }

    /** Get the range rate of a point with regards to the topocentric frame center point.
     * @param extPV point/velocity for which range rate shall be computed
     * @param frame frame in which the point is defined
     * @param date computation date
     * @return range rate of the point (positive if point departs from frame)
     * @exception OrekitException if frames transformations cannot be computed
     */
    public double getRangeRate(final PVCoordinates extPV, final Frame frame,
                               final AbsoluteDate date)
        throws OrekitException {

        // Transform given point from given frame to topocentric frame
        final Transform t = frame.getTransformTo(this, date);
        final PVCoordinates extPVTopo = t.transformPVCoordinates(extPV);

        // Compute range rate (doppler) : relative rate along the line of sight
        return Vector3D.dotProduct(extPVTopo.getPosition(), extPVTopo.getVelocity()) /
               extPVTopo.getPosition().getNorm();

    }

    /** Get the {@link PVCoordinates} of the topocentric frame origin in the selected frame.
     * @param date current date
     * @param frame the frame where to define the position
     * @return position/velocity of the topocentric frame origin (m and m/s)
     * @exception OrekitException if position cannot be computed in given frame
     */
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        return frame.getTransformTo(this, date).transformPVCoordinates(PVCoordinates.ZERO);
    }
}

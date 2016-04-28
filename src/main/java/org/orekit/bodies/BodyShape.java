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
package org.orekit.bodies;

import java.io.Serializable;

import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;


/** Interface representing the rigid surface shape of a natural body.
 * <p>The shape is not provided as a single complete geometric
 * model, but single points can be queried ({@link #getIntersectionPoint}).</p>
 * @author Luc Maisonobe
 */
public interface BodyShape extends Serializable {

    /** Get body frame related to body shape.
     * @return body frame related to body shape
     */
    Frame getBodyFrame();

    /** Get the intersection point of a line with the surface of the body.
     * <p>A line may have several intersection points with a closed
     * surface (we consider the one point case as a degenerated two
     * points case). The close parameter is used to select which of
     * these points should be returned. The selected point is the one
     * that is closest to the close point.</p>
     * @param line test line (may intersect the body or not)
     * @param close point used for intersections selection
     * @param frame frame in which line is expressed
     * @param date date of the line in given frame
     * @return intersection point at altitude zero or null if the line does
     * not intersect the surface
     * @exception OrekitException if line cannot be converted to body frame
     */
    GeodeticPoint getIntersectionPoint(Line line, Vector3D close,
                                       Frame frame, AbsoluteDate date)
        throws OrekitException;

    /** Project a point to the ground.
     * @param point point to project
     * @param date current date
     * @param frame frame in which moving point is expressed
     * @return ground point exactly at the local vertical of specified point,
     * in the same frame as specified point
     * @exception OrekitException if point cannot be converted to body frame
     * @see #projectToGround(TimeStampedPVCoordinates, Frame)
     * @since 7.0
     */
    Vector3D projectToGround(Vector3D point, AbsoluteDate date, Frame frame)
        throws OrekitException;

    /** Project a moving point to the ground.
     * @param pv moving point
     * @param frame frame in which moving point is expressed
     * @return ground point exactly at the local vertical of specified point,
     * in the same frame as specified point
     * @exception OrekitException if point cannot be converted to body frame
     * @see #projectToGround(Vector3D, AbsoluteDate, Frame)
     * @since 7.0
     */
    TimeStampedPVCoordinates projectToGround(TimeStampedPVCoordinates pv, Frame frame)
        throws OrekitException;

    /** Transform a cartesian point to a surface-relative point.
     * @param point cartesian point
     * @param frame frame in which cartesian point is expressed
     * @param date date of the computation (used for frames conversions)
     * @return point at the same location but as a surface-relative point
     * @exception OrekitException if point cannot be converted to body frame
     */
    GeodeticPoint transform(Vector3D point, Frame frame, AbsoluteDate date)
        throws OrekitException;

    /** Transform a surface-relative point to a cartesian point.
     * @param point surface-relative point
     * @return point at the same location but as a cartesian point
     */
    Vector3D transform(GeodeticPoint point);

}

/* Copyright 2002-2023 CS GROUP
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

import java.io.Serializable;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldLine;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
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
     */
    GeodeticPoint getIntersectionPoint(Line line, Vector3D close,
                                       Frame frame, AbsoluteDate date);

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
     * @param <T> type of the field elements
     * @return intersection point at altitude zero or null if the line does
     * not intersect the surface
     * @since 9.0
     */
    <T extends CalculusFieldElement<T>> FieldGeodeticPoint<T> getIntersectionPoint(FieldLine<T> line, FieldVector3D<T> close,
                                                                               Frame frame, FieldAbsoluteDate<T> date);

    /** Project a point to the ground.
     * @param point point to project
     * @param date current date
     * @param frame frame in which moving point is expressed
     * @return ground point exactly at the local vertical of specified point,
     * in the same frame as specified point
     * @see #projectToGround(TimeStampedPVCoordinates, Frame)
     * @since 7.0
     */
    Vector3D projectToGround(Vector3D point, AbsoluteDate date, Frame frame);

    /** Project a moving point to the ground.
     * @param pv moving point
     * @param frame frame in which moving point is expressed
     * @return ground point exactly at the local vertical of specified point,
     * in the same frame as specified point
     * @see #projectToGround(Vector3D, AbsoluteDate, Frame)
     * @since 7.0
     */
    TimeStampedPVCoordinates projectToGround(TimeStampedPVCoordinates pv, Frame frame);

    /** Transform a Cartesian point to a surface-relative point.
     * @param point Cartesian point
     * @param frame frame in which Cartesian point is expressed
     * @param date date of the computation (used for frames conversions)
     * @return point at the same location but as a surface-relative point
     */
    GeodeticPoint transform(Vector3D point, Frame frame, AbsoluteDate date);

    /** Transform a Cartesian point to a surface-relative point.
     * @param point Cartesian point
     * @param <T> type of the filed elements
     * @param frame frame in which Cartesian point is expressed
     * @param date date of the computation (used for frames conversions)
     * @return point at the same location but as a surface-relative point
     * @since 9.0
     */
    <T extends CalculusFieldElement<T>> FieldGeodeticPoint<T> transform(FieldVector3D<T> point, Frame frame,
                                                                    FieldAbsoluteDate<T> date);

    /** Transform a surface-relative point to a Cartesian point.
     * @param point surface-relative point
     * @return point at the same location but as a Cartesian point
     */
    Vector3D transform(GeodeticPoint point);

    /** Transform a surface-relative point to a Cartesian point.
     * @param point surface-relative point
     * @param <T> type of the filed elements
     * @return point at the same location but as a Cartesian point
     * @since 9.0
     */
    <T extends CalculusFieldElement<T>> FieldVector3D<T> transform(FieldGeodeticPoint<T> point);

}

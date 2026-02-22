/* Copyright 2022-2026 Romain Serra
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;


/** Position provider from a given fixed point w.r.t. to a body.
 * @author Romain Serra
 * @since 14.0
 * @see PVCoordinatesProvider
 * @see GeodeticPoint
 */
public class GeodeticExtendedPositionProvider extends ConstantPositionProvider {

    /** Body shape on which the local point is defined. */
    private final BodyShape bodyShape;

    /** Geodetic point. */
    private final GeodeticPoint geodeticPoint;

    /** Simple constructor.
     * @param bodyShape body shape on which the local point is defined
     * @param point local surface point
     */
    public GeodeticExtendedPositionProvider(final BodyShape bodyShape, final GeodeticPoint point) {
        super(bodyShape.transform(point), bodyShape.getBodyFrame());
        this.bodyShape = bodyShape;
        this.geodeticPoint = point;
    }

    /** Simple constructor.
     * @param bodyShape body shape on which the local point is defined
     * @param point local surface point
     */
    public GeodeticExtendedPositionProvider(final BodyShape bodyShape, final Vector3D point) {
        super(point, bodyShape.getBodyFrame());
        this.geodeticPoint = bodyShape.transform(point, bodyShape.getBodyFrame(), AbsoluteDate.ARBITRARY_EPOCH);
        this.bodyShape = bodyShape;
    }

    /** Get the body shape on which the local point is defined.
     * @return body shape on which the local point is defined
     */
    public BodyShape getBodyShape() {
        return bodyShape;
    }

    /** Get the surface point.
     * @return surface point
     */
    public GeodeticPoint getGeodeticPoint() {
        return geodeticPoint;
    }

}

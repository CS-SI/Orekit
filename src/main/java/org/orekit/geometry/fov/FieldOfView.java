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
package org.orekit.geometry.fov;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Transform;
import org.orekit.propagation.events.VisibilityTrigger;

/** Interface representing a spacecraft sensor Field Of View.
 * <p>Fields Of View are zones defined on the unit sphere centered on the
 * spacecraft. Different implementations may use specific modeling
 * depending on the shape.</p>
 * @author Luc Maisonobe
 * @since 10.1
 */
public interface FieldOfView {

    /** Get the angular margin to apply (radians).
     * If angular margin is positive, points outside of the raw FoV but close
     * enough to the boundary are considered visible. If angular margin is negative,
     * points inside of the raw FoV but close enough to the boundary are considered
     * not visible
     * @return angular margin
     * @see #offsetFromBoundary(Vector3D, double, VisibilityTrigger)
     */
    double getMargin();

    /** Get the offset of target body with respect to the Field Of View Boundary.
     * <p>
     * The offset is the signed angular distance between target body and closest boundary
     * point, taking into account {@link VisibilityTrigger} and {@link #getMargin() margin}.
     * </p>
     * <p>
     * As Field Of View can have complex shapes that may require long computation,
     * when the target point can be proven to be outside of the Field Of View, a
     * faster but approximate computation can be used. This approximation is only
     * performed about 0.01 radians outside of the Field Of View augmented by the
     * deadband defined by target body radius and Field Of View margin and should be
     * designed to still return a positive value if the full accurate computation
     * would return a positive value. When target point is close to the zone (and
     * furthermore when it is inside the zone), the full accurate computation is
     * performed. This design allows this offset to be used as a reliable way to
     * detect Field Of View boundary crossings (taking {@link VisibilityTrigger}
     * and {@link #getMargin() margin} into account), which correspond to sign
     * changes of the offset.
     * </p>
     * @param lineOfSight line of sight from the center of the Field Of View support
     * unit sphere to the target in spacecraft frame
     * @param angularRadius target body angular radius
     * @param trigger visibility trigger for spherical bodies
     * @return an offset negative if the target is visible within the Field Of
     * View and positive if it is outside of the Field Of View
     * (note that this cannot take into account interposing bodies)
     * @see #offsetFromBoundary(Vector3D, double, VisibilityTrigger)
     */
    double offsetFromBoundary(Vector3D lineOfSight, double angularRadius, VisibilityTrigger trigger);

    /** Find the direction on Field Of View Boundary closest to a line of sight.
     * @param lineOfSight line of sight from the center of the Field Of View support
     * unit sphere to the target in spacecraft frame
     * @return direction on Field Of View Boundary closest to a line of sight
     */
    Vector3D projectToBoundary(Vector3D lineOfSight);

    /** Get the footprint of the Field Of View on ground.
     * <p>
     * This method assumes the Field Of View is centered on some carrier,
     * which will typically be a spacecraft or a ground station antenna.
     * The points in the footprint boundary loops are all at altitude zero
     * with respect to the ellipsoid, they correspond either to projection
     * on ground of the edges of the Field Of View, or to points on the body
     * limb if the Field Of View goes past horizon. The points on the limb
     * see the carrier origin at zero elevation. If the Field Of View is so
     * large it contains entirely the body, all points will correspond to
     * points at limb. If the Field Of View looks away from body, the
     * boundary loops will be an empty list. The points within footprint
     * loops are sorted in trigonometric order as seen from the carrier.
     * This implies that someone traveling on ground from one point to the
     * next one will have the points visible from the carrier on his left
     * hand side, and the points not visible from the carrier on his right
     * hand side.
     * </p>
     * <p>
     * The truncation of Field Of View at limb can induce strange results
     * for complex Fields Of View. If for example a Field Of View is a
     * ring with a hole and part of the ring goes past horizon, then instead
     * of having a single loop with a C-shaped boundary, the method will
     * still return two loops truncated at the limb, one clockwise and one
     * counterclockwise, hence "closing" the C-shape twice. This behavior
     * is considered acceptable.
     * </p>
     * <p>
     * If the carrier is a spacecraft, then the {@code fovToBody} transform
     * can be computed from a {@link org.orekit.propagation.SpacecraftState}
     * as follows:
     * </p>
     * <pre>
     * Transform inertToBody = state.getFrame().getTransformTo(body.getBodyFrame(), state.getDate());
     * Transform fovToBody   = new Transform(state.getDate(),
     *                                       state.toTransform().getInverse(),
     *                                       inertToBody);
     * </pre>
     * <p>
     * If the carrier is a ground station, located using a topocentric frame
     * and managing its pointing direction using a transform between the
     * dish frame and the topocentric frame, then the {@code fovToBody} transform
     * can be computed as follows:
     * </p>
     * <pre>
     * Transform topoToBody = topocentricFrame.getTransformTo(body.getBodyFrame(), date);
     * Transform topoToDish = ...
     * Transform fovToBody  = new Transform(date,
     *                                      topoToDish.getInverse(),
     *                                      topoToBody);
     * </pre>
     * <p>
     * Only the raw zone is used, the angular margin is ignored here.
     * </p>
     * @param fovToBody transform between the frame in which the Field Of View
     * is defined and body frame.
     * @param body body surface the Field Of View will be projected on
     * @param angularStep step used for boundary loops sampling (radians),
     * beware this is generally <em>not</em> an angle on the unit sphere, but rather a
     * phase angle used by the underlying Field Of View boundary model
     * @return list footprint boundary loops (there may be several independent
     * loops if the Field Of View shape is complex)
     */
    List<List<GeodeticPoint>> getFootprint(Transform fovToBody,
                                           OneAxisEllipsoid body,
                                           double angularStep);

}

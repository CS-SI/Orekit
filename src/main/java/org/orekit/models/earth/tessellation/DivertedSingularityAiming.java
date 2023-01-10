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
package org.orekit.models.earth.tessellation;

import java.util.Collections;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.spherical.twod.S2Point;
import org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;

/** Class used to orient tiles such that there are no singularities within the zone of interest.
 * <p>
 * This class is mainly useful for {@link EllipsoidTessellator#sample(org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet,
 * double, double) sampling} a zone on ground when the grid directions is not really important
 * and when the zone contains the pole, which is a singular point for both
 * {@link ConstantAzimuthAiming} and {@link AlongTrackAiming}.
 * </p>
 * @see AlongTrackAiming
 * @see ConstantAzimuthAiming
 * @author Luc Maisonobe
 */
public class DivertedSingularityAiming implements TileAiming {

    /** Singularity location. */
    private final Vector3D singularity;

    /** Singularity location. */
    private final GeodeticPoint singularityGP;

    /** Dipole moment. */
    private final Vector3D moment;

    /** Simple constructor.
     * @param forbiddenZone zone out of which singularity should be diverted
     */
    public DivertedSingularityAiming(final SphericalPolygonsSet forbiddenZone) {
        final S2Point outside = forbiddenZone.getEnclosingCap().getCenter().negate();
        this.singularity      = outside.getVector();
        this.singularityGP    = new GeodeticPoint(0.5 * FastMath.PI - outside.getPhi(), outside.getTheta(), 0.0);
        this.moment           = singularity.orthogonal();
    }

    /** {@inheritDoc} */
    @Override
    public List<GeodeticPoint> getSingularPoints() {
        return Collections.singletonList(singularityGP);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D alongTileDirection(final Vector3D point, final GeodeticPoint gp) {

        // compute the dipole field at point
        final Vector3D p     = new S2Point(gp.getLongitude(), 0.5 * FastMath.PI - gp.getLatitude()).getVector();
        final Vector3D r     = p.subtract(singularity).normalize();
        final Vector3D field = new Vector3D(3.0 * Vector3D.dotProduct(moment, r), r, -1.0, moment);

        // the aiming direction is the horizontal component of the field
        final Vector3D horizontal = new Vector3D(1, field, -Vector3D.dotProduct(field, p), p);
        return horizontal.normalize();

    }

}

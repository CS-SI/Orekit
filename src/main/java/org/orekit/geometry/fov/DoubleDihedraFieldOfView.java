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

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.partitioning.Region;
import org.hipparchus.geometry.partitioning.RegionFactory;
import org.hipparchus.geometry.spherical.twod.Sphere2D;
import org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;

/** Class representing a spacecraft sensor Field Of View with dihedral shape (i.e. rectangular shape).
 * @author Luc Maisonobe
 * @since 10.1
 */
public class DoubleDihedraFieldOfView extends PolygonalFieldOfView {

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
     * points outside of the raw FoV but close enough to the boundary are
     * considered visible; if negative, points inside of the raw FoV
     * but close enough to the boundary are considered not visible)
     */
    public DoubleDihedraFieldOfView(final Vector3D center,
                                    final Vector3D axis1, final double halfAperture1,
                                    final Vector3D axis2, final double halfAperture2,
                                    final double margin) {
        super(createPolygon(center, axis1, halfAperture1, axis2, halfAperture2), margin);
    }

    /** Create polygon.
     * @param center Direction of the FOV center, in spacecraft frame
     * @param axis1 FOV dihedral axis 1, in spacecraft frame
     * @param halfAperture1 FOV dihedral half aperture angle 1,
     * must be less than π/2, i.e. full dihedra must be smaller then
     * an hemisphere
     * @param axis2 FOV dihedral axis 2, in spacecraft frame
     * @param halfAperture2 FOV dihedral half aperture angle 2,
     * must be less than π/2, i.e. full dihedra must be smaller then
     * an hemisphere
     * @return built polygon
     */
    private static SphericalPolygonsSet createPolygon(final Vector3D center,
                                                      final Vector3D axis1, final double halfAperture1,
                                                      final Vector3D axis2, final double halfAperture2) {
        final RegionFactory<Sphere2D> factory = new RegionFactory<Sphere2D>();
        final double tolerance = FastMath.max(FastMath.ulp(2.0 * FastMath.PI),
                                              1.0e-12 * FastMath.max(halfAperture1, halfAperture2));
        final Region<Sphere2D> dihedra1 = buildDihedra(factory, tolerance, center, axis1, halfAperture1);
        final Region<Sphere2D> dihedra2 = buildDihedra(factory, tolerance, center, axis2, halfAperture2);
        return (SphericalPolygonsSet) factory.intersection(dihedra1, dihedra2);
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
     */
    private static Region<Sphere2D> buildDihedra(final RegionFactory<Sphere2D> factory,
                                                 final double tolerance, final Vector3D center,
                                                 final Vector3D axis, final double halfAperture) {
        if (halfAperture > 0.5 * FastMath.PI) {
            throw new OrekitException(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE,
                                      halfAperture, 0.0, 0.5 * FastMath.PI);
        }

        final Rotation r = new Rotation(axis, halfAperture, RotationConvention.VECTOR_OPERATOR);
        final Vector3D normalCenterPlane = Vector3D.crossProduct(axis, center);
        final Vector3D normalSidePlus    = r.applyInverseTo(normalCenterPlane);
        final Vector3D normalSideMinus   = r.applyTo(normalCenterPlane.negate());

        return factory.intersection(new SphericalPolygonsSet(normalSidePlus,  tolerance),
                                    new SphericalPolygonsSet(normalSideMinus, tolerance));

    }

}

/* Copyright 2002-2026 CS GROUP
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
package org.orekit.propagation.relative.maneuver.rpo;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.List;

public interface RPO {
    /**
     * Get the radial unit vector direction of the target local orbital frame.
     *
     * @return RBar direction
     */
    Vector3D getRBarDirection();

    /**
     * Get the velocity unit vector direction of the target local orbital frame.
     *
     * @return VBar direction
     */
    Vector3D getVBarDirection();

    /**
     * Get the OutOfPlane unit vector direction of the target local orbital frame.
     *
     * @return OutOfPlane direction
     */
    Vector3D getOutOfPlaneDirection();

    /**
     * Get the rotation for the waypoint in the local orbital frame to be used in the computeForcedCircularMotionWaypoints method.
     *
     * @param inclination inclination of the relative orbit.
     * @param raan        raan of the relative orbit.
     * @return rotation to be applied to the waypoints.
     */
    default Rotation rotationInLof(final double inclination, final double raan) {
        final Vector3D vBar = getVBarDirection();
        final Vector3D localVerticalDirection = getOutOfPlaneDirection();
        // Create rotation for the waypoints.
        final Rotation inclinationRotation = new Rotation(vBar, inclination, RotationConvention.VECTOR_OPERATOR);
        final Rotation raanRotation = new Rotation(localVerticalDirection, raan, RotationConvention.VECTOR_OPERATOR);
        return raanRotation.compose(inclinationRotation, RotationConvention.VECTOR_OPERATOR);
    }

    /**
     * Get the rotation for the waypoint in the local orbital frame to be used in the computeForcedCircularMotionWaypoints method.
     *
     * @param inclination inclination of the relative orbit.
     * @param raan        raan of the relative orbit.
     * @param <T>         type of the field elements.
     * @return rotation to be applied to the waypoints.
     */
    default <T extends CalculusFieldElement<T>> FieldRotation<T> rotationInLof(final T inclination, final T raan) {
        final FieldVector3D<T> vBar = new FieldVector3D<>(inclination.getField(), getVBarDirection());
        final FieldVector3D<T> localVerticalDirection = new FieldVector3D<>(inclination.getField(), getOutOfPlaneDirection());
        // Create rotation  for the waypoints.
        final FieldRotation<T> inclinationRotation = new FieldRotation<>(vBar, inclination, RotationConvention.VECTOR_OPERATOR);
        final FieldRotation<T> raanRotation = new FieldRotation<>(localVerticalDirection, raan, RotationConvention.VECTOR_OPERATOR);
        return raanRotation.compose(inclinationRotation, RotationConvention.VECTOR_OPERATOR);
    }

    /**
     * Computes the coordinates of a point on inPlane circle in the associated LocalOrbitalFrame.
     *
     * @param radius radius of the circle.
     * @param angle  angle of the point relative to the inPlane axis of the LocalOrbitalFrame.
     * @return position of the circle point in the related LocalOrbitalFrame.
     */
    default Vector3D circularPosition(final double radius, final double angle) {
        final Vector3D rBar = getRBarDirection();
        final Vector3D vBar = getVBarDirection();
        return vBar.scalarMultiply(radius * FastMath.cos(angle)).add(rBar.scalarMultiply(radius * FastMath.sin(angle)));
    }

    /**
     * Computes the coordinates of a point on inPlane circle in the associated LocalOrbitalFrame.
     *
     * @param radius radius of the circle.
     * @param angle  angle of the point relative to the inPlane axis of the LocalOrbitalFrame.
     * @param <T>    type of the field elements.
     * @return position of the circle point in the related LocalOrbitalFrame.
     */
    default <T extends CalculusFieldElement<T>> FieldVector3D<T> circularPosition(final T radius, final T angle) {
        final FieldVector3D<T> rBar = new FieldVector3D<>(radius.getField(), getRBarDirection());
        final FieldVector3D<T> vBar = new FieldVector3D<>(radius.getField(), getVBarDirection());
        return vBar.scalarMultiply(radius.multiply(angle.cos())).add(rBar.scalarMultiply(radius.multiply(angle.sin())));
    }

    /**
     * Computes the waypoints along the linear path.
     *
     * @param initialPVT     initial TimeStampedPVCoordinates of the chaser.
     * @param finalPVT       last waypoint on the line path.
     * @param numberOfPoints number of waypoints along the linear path (including start and end points).
     * @return list of waypoints along the linear path.
     */
    default List<TimeStampedPVCoordinates> computeForcedLinearWaypoints(final TimeStampedPVCoordinates initialPVT,
                                                                        final TimeStampedPVCoordinates finalPVT,
                                                                        final int numberOfPoints) {

    }


    /**
     * Computes the waypoints to perform a forced circular orbit.
     *
     * @param startDate           Start date of the first waypoint of the forced circular motion.
     * @param centerOffset        Offset of the forced circular orbit center relative to the target. If no Offset, the center is the target position.
     * @param radius              Radius of the forced circular orbit.
     * @param inclination         Inclination of the relative orbit.
     * @param raan                Relative Longitude of ascending node of the relative circular orbit in radians. If raan = 0, the line of apsides is along the positive RBar direction (X+ axis in QSW frame, Z- in LVLH CCSDS frame).
     * @param orbitDuration       Relative orbital Period, the time it takes to the chaser to perform a complete force circular orbit.
     * @param pointsOnOrbit       Number of waypoints along the circular orbit.
     * @param numberOfRevolutions Number of revolutions.
     * @param startAngle          Angle of the first waypoints.
     * @param retrograde          Make the relative circular motion retrograde (clockwise viewed from the target's angular momentum direction).
     * @return list of waypoints.
     */
    default List<TimeStampedPVCoordinates> computeForcedCircularMotionWaypoints(final TimeStampedPVCoordinates startDate,
                                                                                final Vector3D centerOffset,
                                                                                final double radius,
                                                                                final double inclination,
                                                                                final double raan,
                                                                                final double orbitDuration,
                                                                                final int pointsOnOrbit,
                                                                                final int numberOfRevolutions,
                                                                                final double startAngle,
                                                                                final boolean retrograde) {

    }

    /**
     * <p>Computes the injection PVT for a natural circumnavigation around a target in a <b>circular</b> orbit.</p>
     * <p>The inclination must be in the range ]-90° ; +90°[.</p>
     * <p>For i = ±60°, the circumnavigation orbit is circular. Otherwise, it is an ellipse.</p>
     *
     * @param startDate         Date of the injection into the natural circumnavigation orbit.
     * @param distanceAlongVBar
     * @param inclination
     * @param targetMeanMotion
     * @return
     */
    default TimeStampedPVCoordinates computeNaturalCircumnavigationInjectionCircular(final AbsoluteDate startDate,
                                                                                     final double distanceAlongVBar,
                                                                                     final double inclination,
                                                                                     final double targetMeanMotion) {

    }

    /**
     * Computes the waypoints along the linear path.
     *
     * @param initialPVT     initial TimeStampedPVCoordinates of the chaser.
     * @param finalPVT       last waypoint on the line path.
     * @param numberOfPoints number of waypoints along the linear path (including start and end points).
     * @param <T>            type of the field elements.
     * @return list of waypoints along the linear path.
     */
    default <T extends CalculusFieldElement<T>> List<TimeStampedFieldPVCoordinates<T>> computeForcedLinearWaypoints(final TimeStampedFieldPVCoordinates<T> initialPVT,
                                                                                                                    final TimeStampedFieldPVCoordinates<T> finalPVT,
                                                                                                                    final int numberOfPoints) {

    }

    /**
     * Computes the waypoints to perform a forced circular orbit.
     *
     * @param startDate           Start date of the first waypoint of the forced circular motion.
     * @param centerOffset        Offset of the forced circular orbit center relative to the target. If no Offset, the center is the target position.
     * @param radius              Radius of the forced circular orbit.
     * @param inclination         Inclination of the relative orbit.
     * @param raan                Relative Longitude of ascending node of the relative circular orbit in radians. If raan = 0, the line of apsides is along the positive RBar direction (X+ axis in QSW frame, Z- in LVLH CCSDS frame).
     * @param orbitDuration       Relative orbital Period, the time it takes to the chaser to perform a complete force circular orbit.
     * @param pointsOnOrbit       Number of waypoints along the circular orbit.
     * @param numberOfRevolutions Number of revolutions.
     * @param startAngle          Angle of the first waypoints.
     * @param retrograde          Make the relative circular motion retrograde (clockwise viewed from the target's angular momentum direction).
     * @param <T>                 type of the field elements.
     * @return list of waypoints.
     */
    default <T extends CalculusFieldElement<T>> List<TimeStampedFieldPVCoordinates<T>> computeForcedCircularMotionWaypoints(final TimeStampedFieldPVCoordinates<T> startDate,
                                                                                                                            final FieldVector3D<T> centerOffset,
                                                                                                                            final T radius,
                                                                                                                            final T inclination,
                                                                                                                            final T raan,
                                                                                                                            final T orbitDuration,
                                                                                                                            final int pointsOnOrbit,
                                                                                                                            final int numberOfRevolutions,
                                                                                                                            final T startAngle,
                                                                                                                            final boolean retrograde) {

    }


    /**
     * <p>Computes the injection PVT for a natural circumnavigation around a target in a <b>circular</b> orbit.</p>
     * <p>The inclination must be in the range ]-90° ; +90°[.</p>
     * <p>For i = ±60°, the circumnavigation orbit is circular. Otherwise, it is an ellipse.</p>
     *
     * @param startDate         Date of the injection into the natural circumnavigation orbit.
     * @param distanceAlongVBar
     * @param inclination
     * @param targetMeanMotion
     * @param <T>                 type of the field elements.
     * @return
     */
    default <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates computeNaturalCircumnavigationInjectionCircular(
            final FieldAbsoluteDate startDate,
            final T distanceAlongVBar,
            final T inclination,
            final T targetMeanMotion) {

    }
}

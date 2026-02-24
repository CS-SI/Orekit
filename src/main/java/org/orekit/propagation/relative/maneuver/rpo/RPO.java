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
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.SinCos;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Interface used for the computation of the waypoints of the following relative maneuver sequences:
 * - Linear transfer,
 * - Forced Circular Motion,
 * - Natural Circumnavigation.
 * The computeWaypoints  are default methods shared by the different models.
 * The only differences between two models in the computation of these waypoints come from the local orbital frame used.
 * This is handled by the getters defined in enum:{@link RPOModel RPOModel }.
 *
 * @author Romain Cuvillon
 * @since 14.0
 */
public interface RPO {

    /**
     * Get the radial unit vector direction of the target local orbital frame.
     *
     * @return rBar direction
     */
    Vector3D getRBarDirection();

    /**
     * Get the velocity unit vector direction of the target local orbital frame.
     *
     * @return vBar direction
     */
    Vector3D getVBarDirection();

    /**
     * Get the LocalVertical unit vector direction of the target local orbital frame.
     *
     * @return LocalVertical direction
     */
    Vector3D getOutOfPlaneDirection();

    /**
     * Computes the waypoints along the linear path.
     *
     * @param initialPVT     initial TimeStampedPVCoordinates of the chaser.
     * @param finalPVT       last waypoint on the line path.
     * @param numberOfPoints number of waypoints along the linear path (including start and end points).
     * @return list of waypoints along the linear path.
     */
    default List<TimeStampedPVCoordinates> computeLinearWaypoints(final TimeStampedPVCoordinates initialPVT,
                                                                  final TimeStampedPVCoordinates finalPVT,
                                                                  final int numberOfPoints) {
        // Create a list of waypoints.
        final List<TimeStampedPVCoordinates> waypointsList = new ArrayList<>();
        // Add the initial TimeStampedPVCoordinates of the chaser to the list.
        waypointsList.add(initialPVT);
        //Compute the waypoints along the linear path.
        for (int i = 1; i < numberOfPoints - 1; i++) {
            final double timeStep = (finalPVT.getDate().toDouble() - initialPVT.getDate().toDouble()) / (numberOfPoints - 1);
            final Vector3D hopLength = finalPVT.getPosition().subtract(initialPVT.getPosition()).scalarMultiply(1. / (numberOfPoints - 1));
            final TimeStampedPVCoordinates waypoint = new TimeStampedPVCoordinates(initialPVT.getDate().shiftedBy(i * timeStep), initialPVT.getPosition().add(hopLength.scalarMultiply(i)), Vector3D.ZERO);
            waypointsList.add(waypoint);
        }
        waypointsList.add(finalPVT);
        return waypointsList;
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
    default <T extends CalculusFieldElement<T>> List<TimeStampedFieldPVCoordinates<T>> computeLinearWaypoints(final TimeStampedFieldPVCoordinates<T> initialPVT,
                                                                                                              final TimeStampedFieldPVCoordinates<T> finalPVT,
                                                                                                              final int numberOfPoints) {
        // Create a list of waypoints.
        final List<TimeStampedFieldPVCoordinates<T>> waypointsList = new ArrayList<>();
        // Add the initial TimeStampedPVCoordinates of the chaser to the list.
        waypointsList.add(initialPVT);
        //Compute the waypoints along the linear path.
        for (int i = 1; i < numberOfPoints - 1; i++) {
            final T timeStep = (finalPVT.getDate().durationFrom(initialPVT.getDate())).divide(numberOfPoints - 1);
            final FieldVector3D<T> hopLength = finalPVT.getPosition().subtract(initialPVT.getPosition()).scalarMultiply(1. / (numberOfPoints - 1));
            final TimeStampedFieldPVCoordinates<T> waypoint = new TimeStampedFieldPVCoordinates<>(initialPVT.getDate().shiftedBy(timeStep.multiply(i)), new FieldPVCoordinates<>(initialPVT.getPosition().add(hopLength.scalarMultiply(i)), FieldVector3D.getZero(finalPVT.getDate().getField())));
            waypointsList.add(waypoint);
        }
        waypointsList.add(finalPVT);
        return waypointsList;
    }

    /**
     * Get the rotation for the waypoint in the local orbital frame to be used in the computeForcedCircularMotionWaypoints method.
     *
     * @param inclination inclination of the relative orbit.
     * @param raan        raan of the relative orbit.
     * @return rotation to be applied to the waypoints.
     */
    default Rotation rotationInLof(final double inclination, final double raan) {
        final Vector3D vBar = getVBarDirection();
        final Vector3D outOfPlaneDirection = getOutOfPlaneDirection();

        // Create rotation for the waypoints.
        final Rotation inclinationRotation = new Rotation(vBar, inclination, RotationConvention.VECTOR_OPERATOR);
        final Rotation raanRotation = new Rotation(outOfPlaneDirection, raan, RotationConvention.VECTOR_OPERATOR);
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
        final FieldVector3D<T> outOfPlaneDirection = new FieldVector3D<>(inclination.getField(), getOutOfPlaneDirection());

        // Create rotation  for the waypoints.
        final FieldRotation<T> inclinationRotation = new FieldRotation<>(vBar, inclination, RotationConvention.VECTOR_OPERATOR);
        final FieldRotation<T> raanRotation = new FieldRotation<>(outOfPlaneDirection, raan, RotationConvention.VECTOR_OPERATOR);
        return raanRotation.compose(inclinationRotation, RotationConvention.VECTOR_OPERATOR);
    }

    /**
     * Computes the coordinates of a point on inPlane circle in the associated LocalOrbitalFrame.
     * 0°: position aligned with vBar direction.
     * 90°: position aligned with rBar direction.
     * Clockwise rotation from above.
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
     * Computes the waypoints of the chaser on a forced circular orbit.
     * The satellite is injected at the angle defined by the injection angle. 0° is aligned with vBar, 90° with rBar.
     * @param startingDate starting date of the forced circular motion (Date of the first waypoint).
     * @param centerOffset offset of the forced circular orbit center relative to the target. If no Offset, the center is the target position.
     * @param radius radius of the forced circular orbit.
     * @param inclination inclination of the relative orbit.
     * @param raan Relative Longitude of ascending node of the relative circular orbit in radians. If raan = 0, the line of apsides is along the positive RBar direction (X+ axis in QSW frame, Z- in LVLH CCSDS frame).
     * @param orbitDuration Relative orbital Period, the time it takes to the chaser to perform a complete force circular orbit.
     * @param pointsOnOrbit number of waypoints along the circular orbit. (e.g. number of maneuvers)
     * @param numberOfOrbits Number of revolutions.
     * @param injectionAngle injection angle in the relative orbit (0° is aligned with vBar, 90° with rBar).
     * @param retrograde boolean: true if the circular trajectory is retrograde.
     * @return list of waypoints (only the circular phase).
     */
    default List<TimeStampedPVCoordinates> computeForcedCircularMotionWaypoints(final AbsoluteDate startingDate,
                                                                                final Vector3D centerOffset,
                                                                                final double radius,
                                                                                final double inclination,
                                                                                final double raan,
                                                                                final double orbitDuration,
                                                                                final int pointsOnOrbit,
                                                                                final int numberOfOrbits,
                                                                                final double injectionAngle,
                                                                                final boolean retrograde) {
        final double angleStep = 2 * FastMath.PI / pointsOnOrbit;
        final double timeStep = orbitDuration / pointsOnOrbit;
        final List<PVCoordinates> points = new ArrayList<>();
        // Compute the rotation depending on the set of equations used.
        final Rotation rotation = rotationInLof(inclination, raan);
        for (int i = 0; i < pointsOnOrbit; i++) {
            // Computes the point position in plane, accounting for injection angle. (0° aligned with vBar)
            final Vector3D position = circularPosition(radius, i * angleStep + injectionAngle);
            // Rotate the point using inclination and raan and add orbit center offset
            final Vector3D finalPosition = rotation.applyTo(position).add(centerOffset);
            points.add(new PVCoordinates(finalPosition, Vector3D.ZERO));
        }
        final List<TimeStampedPVCoordinates> waypoints = new ArrayList<>();
        // Reorder the waypoints on the circular path. The first waypoint is the previous closest waypoint.
        for (int i = 0; i < points.size(); i++) {
            final TimeStampedPVCoordinates pvt = new TimeStampedPVCoordinates(startingDate.shiftedBy(timeStep * i), points.get(i));
            waypoints.add(pvt);
        }
        if (retrograde) {
            Collections.reverse(waypoints);
        }
        // Add the first waypoint of the circle to the end in order to close the path.
        waypoints.add(new TimeStampedPVCoordinates(startingDate.shiftedBy(orbitDuration), points.get(0)));
        // Add the waypoints with modified date for the successive orbits.
        if (numberOfOrbits > 1) {
            // Extract the waypoints circle excluding the first to avoid having twice this point at the beginning of each revolution.
            final List<TimeStampedPVCoordinates> revolutionWaypoints = waypoints.subList(1, waypoints.size());
            final List<TimeStampedPVCoordinates> revPoints = new ArrayList<>();
            for (int i = 1; i < numberOfOrbits; i++) {
                for (TimeStampedPVCoordinates revolutionWaypoint : revolutionWaypoints) {
                    final AbsoluteDate waypointDate = revolutionWaypoint.getDate().shiftedBy(i * orbitDuration);
                    final TimeStampedPVCoordinates waypoint = new TimeStampedPVCoordinates(waypointDate, revolutionWaypoint.getPosition(), revolutionWaypoint.getVelocity());
                    revPoints.add(waypoint);
                }
            }
            waypoints.addAll(revPoints);
        }
        return waypoints;
    }

    /**
     * Computes the waypoints of the chaser to go linearly from an initial position to a forced circular orbit, then perform the forced circular orbit.
     * If a two impulse transfer is preferred to go from the initial position to the circular orbit, set the numberOfLinearWaypoints to 2.
     * The satellite is injected in the closest point of the circular path to the initial position.
     *
     * @param initialPVT              initial TimeStampedPVCoordinates of the chaser.
     * @param centerOffset            offset of the forced circular orbit center relative to the target. If no Offset, the center is the target position.
     * @param radius                  radius of the forced circular orbit.
     * @param inclination             inclination of the relative orbit.
     * @param raan                    Relative Longitude of ascending node of the relative circular orbit in radians. If raan = 0, the line of apsides is along the positive RBar direction (X+ axis in QSW frame, Z- in LVLH CCSDS frame).
     * @param orbitDuration           Relative orbital Period, the time it takes to the chaser to perform a complete force circular orbit.
     * @param pointsOnOrbit           number of waypoints along the circular orbit.
     * @param numberOfOrbits          Number of revolutions.
     * @param numberOfLinearWaypoints Number of linear waypoints to go from the initial position to the relative orbit.
     * @param linearDuration          duration of the linear phase.
     * @param retrograde              boolean: true if the circular trajectory is retrograde.
     * @return list of waypoints (including the linear phase).
     */
    default List<TimeStampedPVCoordinates> computeForcedCircularMotionWaypoints(final TimeStampedPVCoordinates initialPVT,
                                                                                final Vector3D centerOffset,
                                                                                final double radius,
                                                                                final double inclination,
                                                                                final double raan,
                                                                                final double orbitDuration,
                                                                                final int pointsOnOrbit,
                                                                                final int numberOfOrbits,
                                                                                final int numberOfLinearWaypoints,
                                                                                final double linearDuration,
                                                                                final boolean retrograde) {
        final double angleStep = 2 * FastMath.PI / pointsOnOrbit;
        final double timeStep = orbitDuration / pointsOnOrbit;
        final List<PVCoordinates> points = new ArrayList<>();
        // Compute the rotation depending on the set of equations used.
        int index = 0;
        double norm = Double.POSITIVE_INFINITY;
        final Rotation rotation = rotationInLof(inclination, raan);
        for (int i = 0; i < pointsOnOrbit; i++) {
            final Vector3D position = circularPosition(radius, i * angleStep);
            // Rotate the point using inclination and raan and add orbit center offset
            final Vector3D finalPosition = rotation.applyTo(position).add(centerOffset);
            points.add(new PVCoordinates(finalPosition, Vector3D.ZERO));
            // Searching for the closest waypoint from the initial position.
            if (finalPosition.subtract(initialPVT.getPosition()).getNorm() < norm) {
                index = i;
                norm = finalPosition.subtract(initialPVT.getPosition()).getNorm();
            }
        }
        // Compute the linear path to go from the initial position to the closest point on the circular path.
        final List<TimeStampedPVCoordinates> waypoints = computeLinearWaypoints(initialPVT, new TimeStampedPVCoordinates(initialPVT.getDate().shiftedBy(linearDuration), points.get(index)), numberOfLinearWaypoints);
        List<PVCoordinates> end = points.subList(0, index);
        List<PVCoordinates> beginning = points.subList(index, points.size());
        // Reverse the order of the waypoints of the circular path if retrograde is true.
        if (retrograde) {
            beginning = points.subList(0, index);
            end = points.subList(index, points.size());
            Collections.reverse(beginning);
            Collections.reverse(end);
        }
        // Remove the final point of the linear transfer, to avoid having it twice as it is the same as the first circular waypoint.
        waypoints.remove(waypoints.size() - 1);
        final int linearPoints = waypoints.size();
        // Reorder the waypoints on the circular path. The first waypoint is the previous closest waypoint.
        for (int i = 0; i < beginning.size(); i++) {
            final TimeStampedPVCoordinates pvt = new TimeStampedPVCoordinates(initialPVT.getDate().shiftedBy(linearDuration + timeStep * i), beginning.get(i));
            waypoints.add(pvt);
        }
        for (int i = 0; i < end.size(); i++) {
            final TimeStampedPVCoordinates pvt = new TimeStampedPVCoordinates(initialPVT.getDate().shiftedBy(linearDuration + timeStep * (i + beginning.size())), end.get(i));
            waypoints.add(pvt);
        }
        // Add the first waypoint of the circle to the end in order to close the path.
        waypoints.add(new TimeStampedPVCoordinates(initialPVT.getDate().shiftedBy(orbitDuration + linearDuration), beginning.get(0)));
        // Add the waypoints with modified date for the successive orbits.
        if (numberOfOrbits > 1) {
            // Extract the waypoints circle excluding the first to avoid having twice this point at the beginning of each revolution.
            final List<TimeStampedPVCoordinates> revolutionWaypoints = waypoints.subList(linearPoints + 1, waypoints.size());
            final List<TimeStampedPVCoordinates> revPoints = new ArrayList<>();
            for (int i = 1; i < numberOfOrbits; i++) {
                for (final TimeStampedPVCoordinates revolutionWaypoint : revolutionWaypoints) {
                    final AbsoluteDate waypointDate = revolutionWaypoint.getDate().shiftedBy(i * orbitDuration);
                    final TimeStampedPVCoordinates waypoint = new TimeStampedPVCoordinates(waypointDate, revolutionWaypoint.getPosition(), revolutionWaypoint.getVelocity());
                    revPoints.add(waypoint);
                }
            }
            waypoints.addAll(revPoints);
        }
        return waypoints;
    }

    /**
     * Computes the waypoints of the chaser to go linearly from an initial position to a forced circular orbit, then perform the forced circular orbit.
     * If a two impulse transfer is preferred to go from the initial position to the circular orbit, set the numberOfLinearWaypoints to 2.
     * By default, the circular trajectory is prograde.
     * The satellite is injected in the closest point of the circular path to the initial position.
     *
     * @param initialPVT              initial TimeStampedPVCoordinates of the chaser.
     * @param centerOffset            offset of the forced circular orbit center relative to the target. If no Offset, the center is the target position.
     * @param radius                  radius of the forced circular orbit.
     * @param inclination             inclination of the relative orbit.
     * @param raan                    Relative Longitude of ascending node of the relative circular orbit in radians. If raan = 0, the line of apsides is along the positive RBar direction (X+ axis in QSW frame, Z- in LVLH CCSDS frame).
     * @param orbitDuration           Relative orbital Period, the time it takes to the chaser to perform a complete force circular orbit.
     * @param pointsOnOrbit           number of waypoints along the circular orbit.
     * @param numberOfOrbits          Number of revolutions.
     * @param numberOfLinearWaypoints Number of linear waypoints to go from the initial position to the relative orbit.
     * @param linearDuration          duration of the linear phase.
     * @return list of waypoints (including the linear phase).
     */
    default List<TimeStampedPVCoordinates> computeForcedCircularMotionWaypoints(final TimeStampedPVCoordinates initialPVT,
                                                                                final Vector3D centerOffset,
                                                                                final double radius,
                                                                                final double inclination,
                                                                                final double raan,
                                                                                final double orbitDuration,
                                                                                final int pointsOnOrbit,
                                                                                final int numberOfOrbits,
                                                                                final int numberOfLinearWaypoints,
                                                                                final double linearDuration) {
        return computeForcedCircularMotionWaypoints(initialPVT, centerOffset, radius, inclination, raan, orbitDuration,
                pointsOnOrbit, numberOfOrbits, numberOfLinearWaypoints, linearDuration, false);
    }

    /**
     * Computes the waypoints of the chaser to go linearly from an initial position to a forced circular orbit, then perform the forced circular orbit.
     * If a two impulse transfer is preferred to go from the initial position to the circular orbit, set the numberOfLinearWaypoints to 2.
     * The satellite is injected in the closest point of the circular path to the initial position.
     *
     * @param initialPVT              initial TimeStampedPVCoordinates of the chaser.
     * @param centerOffset            offset of the forced circular orbit center relative to the target. If no Offset, the center is the target position.
     * @param radius                  radius of the forced circular orbit.
     * @param inclination             inclination of the relative orbit.
     * @param raan                    Relative Longitude of ascending node of the relative circular orbit in radians. If raan = 0, the line of apsides is along the positive RBar direction (X+ axis in QSW frame, Z- in LVLH CCSDS frame).
     * @param orbitDuration           Relative orbital Period, the time it takes to the chaser to perform a complete force circular orbit.
     * @param pointsOnOrbit           number of waypoints along the circular orbit.
     * @param numberOfOrbits          Number of revolutions.
     * @param numberOfLinearWaypoints Number of linear waypoints to go from the initial position to the relative orbit.
     * @param linearDuration          duration of the linear phase.
     * @param retrograde              boolean: true if the circular trajectory is retrograde.
     * @param <T>                     type of the field elements.
     * @return list of waypoints (including the linear phase).
     */
    default <T extends CalculusFieldElement<T>> List<TimeStampedFieldPVCoordinates<T>> computeForcedCircularMotionWaypoints(final TimeStampedFieldPVCoordinates<T> initialPVT,
                                                                                                                            final FieldVector3D<T> centerOffset,
                                                                                                                            final T radius,
                                                                                                                            final T inclination,
                                                                                                                            final T raan,
                                                                                                                            final T orbitDuration,
                                                                                                                            final int pointsOnOrbit,
                                                                                                                            final int numberOfOrbits,
                                                                                                                            final int numberOfLinearWaypoints,
                                                                                                                            final T linearDuration,
                                                                                                                            final boolean retrograde) {
        final T angleStep = radius.getPi().multiply(2).divide(pointsOnOrbit);
        final T timeStep = orbitDuration.divide(pointsOnOrbit);
        final List<FieldPVCoordinates<T>> points = new ArrayList<>();
        // Compute the rotation depending on the set of equations used.
        int index = 0;
        double norm = Double.POSITIVE_INFINITY;
        final FieldRotation<T> rotation = rotationInLof(inclination, raan);
        for (int i = 0; i < pointsOnOrbit; i++) {
            final FieldVector3D<T> position = circularPosition(radius, angleStep.multiply(i));
            // Rotate the point using inclination and raan and add orbit center offset
            final FieldVector3D<T> finalPosition = rotation.applyTo(position).add(centerOffset);
            points.add(new FieldPVCoordinates<>(finalPosition, FieldVector3D.getZero(initialPVT.getDate().getField())));
            // Searching for the closest waypoint from the initial position.
            if (finalPosition.subtract(initialPVT.getPosition()).getNorm().getReal() < norm) {
                index = i;
                norm = finalPosition.subtract(initialPVT.getPosition()).getNorm().getReal();
            }
        }
        // Compute the linear path to go from the initial position to the closest point on the circular path.
        final List<TimeStampedFieldPVCoordinates<T>> waypoints = computeLinearWaypoints(initialPVT, new TimeStampedFieldPVCoordinates<>(initialPVT.getDate().shiftedBy(linearDuration), points.get(index)), numberOfLinearWaypoints);
        List<FieldPVCoordinates<T>> end = points.subList(0, index);
        List<FieldPVCoordinates<T>> beginning = points.subList(index, points.size());
        // Reverse the order of the waypoints of the circular path if retrograde is true.
        if (retrograde) {
            beginning = points.subList(0, index);
            end = points.subList(index, points.size());
            Collections.reverse(beginning);
            Collections.reverse(end);
        }
        // Remove the final point of the linear transfer, to avoid having it twice as it is the same as the first circular waypoint.
        waypoints.remove(waypoints.size() - 1);
        final int linearPoints = waypoints.size();
        // Reorder the waypoints on the circular path. The first waypoint is the previous closest waypoint.
        for (int i = 0; i < beginning.size(); i++) {
            final TimeStampedFieldPVCoordinates<T> pvt = new TimeStampedFieldPVCoordinates<>(initialPVT.getDate().shiftedBy(linearDuration.add(timeStep.multiply(i))), beginning.get(i));
            waypoints.add(pvt);
        }
        for (int i = 0; i < end.size(); i++) {
            final TimeStampedFieldPVCoordinates<T> pvt = new TimeStampedFieldPVCoordinates<>(initialPVT.getDate().shiftedBy(linearDuration.add(timeStep.multiply(i + beginning.size()))), end.get(i));
            waypoints.add(pvt);
        }
        // Add the first waypoint of the circle to the end in order to close the path.
        waypoints.add(new TimeStampedFieldPVCoordinates<>(initialPVT.getDate().shiftedBy(orbitDuration.add(linearDuration)), beginning.get(0)));
        // Add the waypoints with modified date for the successive orbits.
        if (numberOfOrbits > 1) {
            // Extract the waypoints circle excluding the first to avoid having twice this point at the beginning of each revolution.
            final List<TimeStampedFieldPVCoordinates<T>> revolutionWaypoints = waypoints.subList(linearPoints + 1, waypoints.size());
            final List<TimeStampedFieldPVCoordinates<T>> revPoints = new ArrayList<>();
            for (int i = 1; i < numberOfOrbits; i++) {
                for (TimeStampedFieldPVCoordinates<T> revolutionWaypoint : revolutionWaypoints) {
                    final FieldAbsoluteDate<T> waypointDate = revolutionWaypoint.getDate().shiftedBy(orbitDuration.multiply(i));
                    final TimeStampedFieldPVCoordinates<T> waypoint = new TimeStampedFieldPVCoordinates<>(waypointDate, new FieldPVCoordinates<>(revolutionWaypoint.getPosition(), revolutionWaypoint.getVelocity()));
                    revPoints.add(waypoint);
                }
            }
            waypoints.addAll(revPoints);
        }
        return waypoints;
    }

    /**
     * Computes the waypoints of the chaser to go linearly from an initial position to a forced circular orbit, then perform the forced circular orbit.
     * If a two impulse transfer is preferred to go from the initial position to the circular orbit, set the numberOfLinearWaypoints to 2.
     * The satellite is injected in the closest point of the circular path to the initial position.
     *
     * @param initialPVT              initial TimeStampedPVCoordinates of the chaser.
     * @param centerOffset            offset of the forced circular orbit center relative to the target. If no Offset, the center is the target position.
     * @param radius                  radius of the forced circular orbit.
     * @param inclination             inclination of the relative orbit.
     * @param raan                    Relative Longitude of ascending node of the relative circular orbit in radians. If raan = 0, the line of apsides is along the positive RBar direction (X+ axis in QSW frame, Z- in LVLH CCSDS frame).
     * @param orbitDuration           Relative orbital Period, the time it takes to the chaser to perform a complete force circular orbit.
     * @param pointsOnOrbit           number of waypoints along the circular orbit.
     * @param numberOfOrbits          Number of revolutions.
     * @param numberOfLinearWaypoints Number of linear waypoints to go from the initial position to the relative orbit.
     * @param linearDuration          duration of the linear phase.
     * @param <T>                     type of the field elements.
     * @return list of waypoints (including the linear phase).
     */
    default <T extends CalculusFieldElement<T>> List<TimeStampedFieldPVCoordinates<T>> computeForcedCircularMotionWaypoints(final TimeStampedFieldPVCoordinates<T> initialPVT,
                                                                                                                            final FieldVector3D<T> centerOffset,
                                                                                                                            final T radius,
                                                                                                                            final T inclination,
                                                                                                                            final T raan,
                                                                                                                            final T orbitDuration,
                                                                                                                            final int pointsOnOrbit,
                                                                                                                            final int numberOfOrbits,
                                                                                                                            final int numberOfLinearWaypoints,
                                                                                                                            final T linearDuration) {
        return computeForcedCircularMotionWaypoints(initialPVT, centerOffset, radius, inclination, raan, orbitDuration,
                pointsOnOrbit, numberOfOrbits, numberOfLinearWaypoints, linearDuration, false);
    }

    /**
     * Computes the waypoints of the chaser to go linearly from an initial position to a NaturalCircumnavigation Orbit (NCO) when the target is in a Circular Orbit.
     * The NCO depends only on the value of its semi-minor axis. The injection point is located behind the target at a distance of twice the semi-minor axis.
     * If a two impulse transfer is preferred to go from the initial position to the relative NCO orbit, set the numberOfLinearWaypoints to 2.
     * Note: This is the simplest waypoint generator for natural circumnavigation Orbit in case where the target is in a circular orbit. More complicated NCO
     * and NCO in elliptic cases can be computed using Elliptic NCO class.
     *
     * @param initialPVT            TimeStampedPVCoordinates of the chaser at the beginning of the maneuvers' scenario.
     * @param injectionDate         Date of injection in the NCO.
     * @param targetMeanMotion      Keplerian mean motion of the target.
     * @param relativeSemiMinorAxis semi minor axis of the relative NCO.
     * @param inclination           inclination (in rad) of the relative NCO.
     * @param numberOfLinearPoints  number of linear Waypoints to reach the NCO injection Point at the injection Date.
     * @return List of waypoints to put the chaser satellite into a natural circumnavigation orbit from an initial point.
     */
    default List<TimeStampedPVCoordinates> computeCircularNaturalCircumnavigationWaypoints(final TimeStampedPVCoordinates initialPVT, final AbsoluteDate injectionDate, final double targetMeanMotion, final double relativeSemiMinorAxis, final double inclination, final int numberOfLinearPoints) {
        final Vector3D rBar = getRBarDirection();
        final Vector3D vBar = getVBarDirection();
        final TimeStampedPVCoordinates injectionPoint = new TimeStampedPVCoordinates(injectionDate, vBar.scalarMultiply(-2 * relativeSemiMinorAxis), Vector3D.ZERO);
        final double deltaV = relativeSemiMinorAxis * targetMeanMotion;
        // Create the impulse vector along rBar to perform the NaturalCircumnavigation.
        final Vector3D delta = rBar.scalarMultiply(-deltaV);
        Vector3D velocityAfterMan = injectionPoint.getVelocity().add(delta);
        if (inclination != 0.0) {
            // Compute sine and cosine of the relative inclination
            final SinCos sci = FastMath.sinCos(inclination);

            // Compute the rotation of the inclination around the vBar axis of the target's LOF
            // The scale factor 1/cos(i) ensures that the starting distance is the one specified when the object was constructed
            final Rotation rotation = new Rotation(vBar, inclination, RotationConvention.VECTOR_OPERATOR);
            velocityAfterMan = rotation.applyTo(velocityAfterMan).scalarMultiply(1.0 / sci.cos());
        }
        return computeLinearWaypoints(initialPVT, new TimeStampedPVCoordinates(injectionDate, injectionPoint.getPosition(), velocityAfterMan), numberOfLinearPoints);
    }

    /**
     * Computes the waypoints of the chaser to go linearly from an initial position to a NaturalCircumnavigation Orbit (NCO) when the target is in a Circular Orbit.
     * The NCO depends only on the value of its semi-minor axis. The injection point is located behind the target at a distance of twice the semi-minor axis.
     * If a two impulse transfer is preferred to go from the initial position to the relative NCO orbit, set the numberOfLinearWaypoints to 2.
     * Note: This is the simplest waypoint generator for natural circumnavigation Orbit in case where the target is in a circular orbit. More complicated NCO
     * and NCO in elliptic cases can be computed using Elliptic NCO class.
     *
     * @param initialPVT            TimeStampedFieldPVCoordinates of the chaser at the beginning of the maneuvers' scenario.
     * @param injectionDate         Date of injection in the NCO.
     * @param targetMeanMotion      Keplerian mean motion of the target.
     * @param relativeSemiMinorAxis semi minor axis of the relative NCO.
     * @param inclination           inclination (in rad) of the relative NCO.
     * @param numberOfLinearPoints  number of linear Waypoints to reach the NCO injection Point at the injection Date.
     * @param <T>                   type of the field elements.
     * @return List of waypoints to put the chaser satellite into a natural circumnavigation orbit from an initial point.
     */
    default <T extends CalculusFieldElement<T>> List<TimeStampedFieldPVCoordinates<T>> computeCircularNaturalCircumnavigationWaypoints(final TimeStampedFieldPVCoordinates<T> initialPVT,
                                                                                                                                       final FieldAbsoluteDate<T> injectionDate,
                                                                                                                                       final T targetMeanMotion,
                                                                                                                                       final T relativeSemiMinorAxis,
                                                                                                                                       final T inclination,
                                                                                                                                       final int numberOfLinearPoints) {
        final Field<T> field = injectionDate.getField();
        final FieldVector3D<T> rBar = new FieldVector3D<>(field, getRBarDirection());
        final FieldVector3D<T> vBar = new FieldVector3D<>(field, getVBarDirection());
        final TimeStampedFieldPVCoordinates<T> injectionPoint = new TimeStampedFieldPVCoordinates<>(injectionDate, new FieldPVCoordinates<>(vBar.scalarMultiply(relativeSemiMinorAxis.multiply(-2)), FieldVector3D.getZero(field)));
        final T deltaV = relativeSemiMinorAxis.multiply(targetMeanMotion);
        // Create the impulse vector along rBar to perform the NaturalCircumnavigation.
        final FieldVector3D<T> delta = rBar.scalarMultiply(deltaV.negate());
        FieldVector3D<T> velocityAfterMan = injectionPoint.getVelocity().add(delta);
        if (inclination.getReal() != 0) {
            final FieldSinCos<T> sci = inclination.sinCos();
            final FieldRotation<T> rotation = new FieldRotation<>(vBar, inclination, RotationConvention.VECTOR_OPERATOR);

            // Compute the rotation of the inclination around the vBar axis of the target's LOF
            // The scale factor 1/cos(i) ensures that the starting distance is the one specified when the object was constructed
            velocityAfterMan = rotation.applyTo(velocityAfterMan).scalarMultiply(field.getOne().divide(sci.cos()));
        }
        return computeLinearWaypoints(initialPVT, new TimeStampedFieldPVCoordinates<>(injectionDate, new FieldPVCoordinates<>(injectionPoint.getPosition(), velocityAfterMan)), numberOfLinearPoints);
    }

    /**
     * Computes the waypoints of the chaser to go linearly from an initial position to a natural circular orbit when the target is in a Circular Orbit.
     * The circular orbit depends only on the value of its radius axis.
     * If a two impulse transfer is preferred to go from the initial position to the circular orbit, set the numberOfLinearWaypoints to 2.
     * The computation is based on : A critical study of Linear and NonLinear Satellite Formation Flying Control Methodologies from a Fuel Consumption Perspective, Pradipto Ghosh, 2007.
     *
     * @param initialPVT           TimeStampedPVCoordinates of the chaser at the beginning of the maneuvers' scenario.
     * @param injectionDate        Date of injection in the NCO.
     * @param targetMeanMotion     Keplerian mean motion of the target.
     * @param radius               radius of the circular motion.
     * @param numberOfLinearPoints number of linear Waypoints to reach the NCO injection Point at the injection Date.
     * @return List of waypoints to put the chaser satellite into a natural circumnavigation orbit from an initial point.
     */
    default List<TimeStampedPVCoordinates> computeNaturalCircularWaypoints(final TimeStampedPVCoordinates initialPVT, final AbsoluteDate injectionDate, final double targetMeanMotion, final double radius, final int numberOfLinearPoints) {
        final Vector3D rBar = getRBarDirection();
        final Vector3D vBar = getVBarDirection();
        final Vector3D localVertical = getOutOfPlaneDirection();
        final TimeStampedPVCoordinates injectionPoint = new TimeStampedPVCoordinates(injectionDate, vBar.scalarMultiply(-radius), Vector3D.ZERO);
        // Create the impulse to inject the satellite in the relative circular orbit.
        final Vector3D delta = rBar.scalarMultiply(-targetMeanMotion * radius / 2).add(localVertical.scalarMultiply(targetMeanMotion * radius * FastMath.sqrt(3) / 2));
        final Vector3D velocityAfterMan = injectionPoint.getVelocity().add(delta);
        return computeLinearWaypoints(initialPVT, new TimeStampedPVCoordinates(injectionDate, injectionPoint.getPosition(), velocityAfterMan), numberOfLinearPoints);
    }

    /**
     * Computes the waypoints of the chaser to go linearly from an initial position to a natural circular orbit when the target is in a Circular Orbit.
     * The circular orbit depends only on the value of its radius axis.
     * If a two impulse transfer is preferred to go from the initial position to the circular orbit, set the numberOfLinearWaypoints to 2.
     * The computation is based on : A critical study of Linear and NonLinear Satellite Formation Flying Control Methodologies from a Fuel Consumption Perspective, Pradipto Ghosh, 2007.
     *
     * @param initialPVT           TimeStampedPVCoordinates of the chaser at the beginning of the maneuvers' scenario.
     * @param injectionDate        Date of injection in the NCO.
     * @param targetMeanMotion     Keplerian mean motion of the target.
     * @param radius               radius of the circular motion.
     * @param numberOfLinearPoints number of linear Waypoints to reach the NCO injection Point at the injection Date.
     * @param <T>                  field.
     * @return List of waypoints to put the chaser satellite into a natural circumnavigation orbit from an initial point.
     */
    default <T extends CalculusFieldElement<T>> List<TimeStampedFieldPVCoordinates<T>> computeNaturalCircularWaypoints(final TimeStampedFieldPVCoordinates<T> initialPVT, final FieldAbsoluteDate<T> injectionDate, final T targetMeanMotion, final T radius, final int numberOfLinearPoints) {
        final Field<T> field = targetMeanMotion.getField();
        final FieldVector3D<T> rBar = new FieldVector3D<>(field, getRBarDirection());
        final FieldVector3D<T> vBar = new FieldVector3D<>(field, getVBarDirection());
        final FieldVector3D<T> localVertical = new FieldVector3D<>(field, getOutOfPlaneDirection());
        final TimeStampedFieldPVCoordinates<T> injectionPoint = new TimeStampedFieldPVCoordinates<>(injectionDate, new FieldPVCoordinates<>(vBar.scalarMultiply(radius.negate()), new FieldVector3D<>(field, Vector3D.ZERO)));
        // Create the impulse to inject the satellite in the relative circular orbit.
        final FieldVector3D<T> delta = rBar.scalarMultiply(targetMeanMotion.negate().multiply(radius).divide(2)).add(localVertical.scalarMultiply(targetMeanMotion.multiply(radius).multiply(FastMath.sqrt(3) / 2)));
        final FieldVector3D<T> velocityAfterMan = injectionPoint.getVelocity().add(delta);
        return computeLinearWaypoints(initialPVT, new TimeStampedFieldPVCoordinates<>(injectionDate, new FieldPVCoordinates<>(injectionPoint.getPosition(), velocityAfterMan)), numberOfLinearPoints);
    }
}

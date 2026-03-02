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
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.relative.maneuver.rpoOLD.RPOModel;
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
        final Rotation inclinationRotation = new Rotation(vBar.scalarMultiply(-1), inclination, RotationConvention.VECTOR_OPERATOR);
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
        final FieldRotation<T> inclinationRotation = new FieldRotation<>(vBar.scalarMultiply(-1), inclination, RotationConvention.VECTOR_OPERATOR);
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
        return vBar.scalarMultiply(-radius * FastMath.cos(angle)).add(rBar.scalarMultiply(radius * FastMath.sin(angle)));
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
        return vBar.scalarMultiply(radius.negate().multiply(angle.cos())).add(rBar.scalarMultiply(radius.multiply(angle.sin())));
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
    default List<TimeStampedPVCoordinates> computeForcedCircularMotionWaypoints(final AbsoluteDate startDate,
                                                                                final Vector3D centerOffset,
                                                                                final double radius,
                                                                                final double inclination,
                                                                                final double raan,
                                                                                final double orbitDuration,
                                                                                final int pointsOnOrbit,
                                                                                final int numberOfRevolutions,
                                                                                final double startAngle,
                                                                                final boolean retrograde) {
        final double angleStep = 2 * FastMath.PI / pointsOnOrbit;
        final double timeStep = orbitDuration / pointsOnOrbit;
        final List<PVCoordinates> points = new ArrayList<>();
        // Compute the rotation depending on the set of equations used.
        final Rotation rotation = rotationInLof(inclination, raan);
        for (int i = 0; i < pointsOnOrbit; i++) {
            // Computes the point position in plane, accounting for injection angle. (0° aligned with vBar)
            final Vector3D position = circularPosition(radius, i * angleStep + startAngle);
            // Rotate the point using inclination and raan and add orbit center offset
            final Vector3D finalPosition = rotation.applyTo(position).add(centerOffset);
            points.add(new PVCoordinates(finalPosition, Vector3D.ZERO));
        }
        // Add the first waypoint of the circle to the end in order to close the path.
        points.add(points.get(0));
        if (retrograde) {
            Collections.reverse(points);
        }
        final List<TimeStampedPVCoordinates> waypoints = new ArrayList<>();
        // Reorder the waypoints on the circular path. The first waypoint is the previous closest waypoint.
        for (int i = 0; i < points.size(); i++) {
            final TimeStampedPVCoordinates pvt = new TimeStampedPVCoordinates(startDate.shiftedBy(timeStep * i), points.get(i));
            waypoints.add(pvt);
        }
        // Add the waypoints with modified date for the successive orbits.
        if (numberOfRevolutions > 1) {
            // Extract the waypoints circle excluding the first to avoid having twice this point at the beginning of each revolution.
            final List<TimeStampedPVCoordinates> revolutionWaypoints = waypoints.subList(1, waypoints.size());
            final List<TimeStampedPVCoordinates> revPoints = new ArrayList<>();
            for (int i = 1; i < numberOfRevolutions; i++) {
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
     * <p>Computes the injection PVT for a natural circumnavigation around a target in a <b>circular</b> orbit.</p>
     * <p>The inclination must be in the range ]-90° ; +90°[.</p>
     * <p>For i = ±60°, the circumnavigation orbit is circular. Otherwise, it is an ellipse.</p>
     *
     * @param startDate         Date of the injection into the natural circumnavigation orbit.
     * @param distanceAlongVBar Distance between the target and the point of the relative orbit lying on vBar.
     * @param inclination       Inclination of the relative orbit.
     * @param targetMeanMotion  Mean motion of the target orbit.
     * @return injection PVT (i.e. after the injection maneuver).
     */
    default TimeStampedPVCoordinates computeNaturalCircumnavigationInjectionCircular(final AbsoluteDate startDate,
                                                                                     final double distanceAlongVBar,
                                                                                     final double inclination,
                                                                                     final double targetMeanMotion) {
        final Vector3D rBar = getRBarDirection();
        final Vector3D vBar = getVBarDirection();
        final TimeStampedPVCoordinates injectionPoint = new TimeStampedPVCoordinates(startDate, vBar.scalarMultiply(-distanceAlongVBar), Vector3D.ZERO);
        final double deltaV = distanceAlongVBar / 2. * targetMeanMotion;
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
        return new TimeStampedPVCoordinates(startDate, injectionPoint.getPosition(), velocityAfterMan);
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
    default <T extends CalculusFieldElement<T>> List<TimeStampedFieldPVCoordinates<T>> computeForcedCircularMotionWaypoints(final FieldAbsoluteDate<T> startDate,
                                                                                                                            final FieldVector3D<T> centerOffset,
                                                                                                                            final T radius,
                                                                                                                            final T inclination,
                                                                                                                            final T raan,
                                                                                                                            final T orbitDuration,
                                                                                                                            final int pointsOnOrbit,
                                                                                                                            final int numberOfRevolutions,
                                                                                                                            final T startAngle,
                                                                                                                            final boolean retrograde) {
        final T angleStep = radius.getPi().multiply(2).divide(pointsOnOrbit);
        final T timeStep = orbitDuration.divide(pointsOnOrbit);
        final List<FieldPVCoordinates<T>> points = new ArrayList<>();
        // Compute the rotation depending on the set of equations used.
        final FieldRotation<T> rotation = rotationInLof(inclination, raan);
        for (int i = 0; i < pointsOnOrbit; i++) {
            // Computes the point position in plane, accounting for injection angle. (0° aligned with vBar)
            final FieldVector3D<T> position = circularPosition(radius, angleStep.multiply(i).add(startAngle));
            // Rotate the point using inclination and raan and add orbit center offset
            final FieldVector3D<T> finalPosition = rotation.applyTo(position).add(centerOffset);
            points.add(new FieldPVCoordinates<>(finalPosition, FieldVector3D.getZero(startDate.getDate().getField())));
        }
        // Add the first waypoint of the circle to the end in order to close the path.
        points.add(points.get(0));
        if (retrograde) {
            Collections.reverse(points);
        }
        final List<TimeStampedFieldPVCoordinates<T>> waypoints = new ArrayList<>();
        // Reorder the waypoints on the circular path. The first waypoint is the previous closest waypoint.
        for (int i = 0; i < points.size(); i++) {
            final TimeStampedFieldPVCoordinates<T> pvt = new TimeStampedFieldPVCoordinates<>(startDate.shiftedBy(timeStep.multiply(i)), points.get(i));
            waypoints.add(pvt);
        }
        // Add the waypoints with modified date for the successive orbits.
        if (numberOfRevolutions > 1) {
            // Extract the waypoints circle excluding the first to avoid having twice this point at the beginning of each revolution.
            final List<TimeStampedFieldPVCoordinates<T>> revolutionWaypoints = waypoints.subList(1, waypoints.size());
            final List<TimeStampedFieldPVCoordinates<T>> revPoints = new ArrayList<>();
            for (int i = 1; i < numberOfRevolutions; i++) {
                for (TimeStampedFieldPVCoordinates<T> revolutionWaypoint : revolutionWaypoints) {
                    final FieldAbsoluteDate<T> waypointDate = revolutionWaypoint.getDate().shiftedBy(orbitDuration.multiply(i));
                    final TimeStampedFieldPVCoordinates<T> waypoint = new TimeStampedFieldPVCoordinates<>(waypointDate,
                            new FieldPVCoordinates<>(revolutionWaypoint.getPosition(), revolutionWaypoint.getVelocity()));
                    revPoints.add(waypoint);
                }
            }
            waypoints.addAll(revPoints);
        }
        return waypoints;

    }


    /**
     * <p>Computes the injection PVT for a natural circumnavigation around a target in a <b>circular</b> orbit.</p>
     * <p>The inclination must be in the range ]-90° ; +90°[.</p>
     * <p>For i = ±60°, the circumnavigation orbit is circular. Otherwise, it is an ellipse.</p>
     *
     * @param startDate         Date of the injection into the natural circumnavigation orbit.
     * @param distanceAlongVBar Distance between the target and the point of the relative orbit lying on vBar.
     * @param inclination       Inclination of the relative orbit.
     * @param targetMeanMotion  Mean motion of the target orbit.
     * @param <T>               type of the field elements.
     * @return injection PVT (i.e. after the injection maneuver).
     */
    default <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T> computeNaturalCircumnavigationInjectionCircular(
            final FieldAbsoluteDate<T> startDate,
            final T distanceAlongVBar,
            final T inclination,
            final T targetMeanMotion) {
        final Field<T> field = startDate.getField();
        final FieldVector3D<T> rBar = new FieldVector3D<>(field, getRBarDirection());
        final FieldVector3D<T> vBar = new FieldVector3D<>(field, getVBarDirection());
        final TimeStampedFieldPVCoordinates<T> injectionPoint = new TimeStampedFieldPVCoordinates<>(startDate, new FieldPVCoordinates<>(vBar.scalarMultiply(distanceAlongVBar.negate()), FieldVector3D.getZero(field)));
        final T deltaV = distanceAlongVBar.divide(2.).multiply(targetMeanMotion);
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
        return new TimeStampedFieldPVCoordinates<>(startDate, new FieldPVCoordinates<>(injectionPoint.getPosition(), velocityAfterMan));
    }

    /**
     * Computes teardrop waypoints.
     *
     * <p>The injection point is the turn-around point of the teardrop (the round end).</p>
     * <p>All maneuvers are performed at the pointy end of the teardrop.</p>
     *
     * @param injectionDate date of injection in the teardrop.
     * @param targetOrbit orbit of the target.
     * @param turnAroundDistance Turn-around distance. This is the "round" end of the orbit. Note that this distance is signed : negative means below the target spacecraft (in between the planet and the target), while positive means above the target (target is in between the chaser and the planet).
     * @param maneuverDistance Maneuver distance of the teardrop orbit. This is the "pointy" end of the orbit. Note that this distance is signed : negative means below the target spacecraft (in between the planet and the target), while positive means above the target (target is in between the chaser and the planet).
     * @param numberOfTeardrops Number of teardrop orbits to perform. Must be ≥ 1.
     * @return List of waypoints in time. Date, position, and velocity are non-zero.
     */
    List<TimeStampedPVCoordinates> computeTeardropWaypoints(AbsoluteDate injectionDate, Orbit targetOrbit, double turnAroundDistance, double maneuverDistance, int numberOfTeardrops);

    /**
     * Computes teardrop waypoints.
     *
     * <p>The injection point is the turn-around point of the teardrop (the round end).</p>
     * <p>All maneuvers are performed at the pointy end of the teardrop.</p>
     *
     * @param injectionDate date of injection in the teardrop.
     * @param targetOrbit orbit of the target.
     * @param turnAroundDistance Turn-around distance. This is the "round" end of the orbit. Note that this distance is signed : negative means below the target spacecraft (in between the planet and the target), while positive means above the target (target is in between the chaser and the planet).
     * @param maneuverDistance Maneuver distance of the teardrop orbit. This is the "pointy" end of the orbit. Note that this distance is signed : negative means below the target spacecraft (in between the planet and the target), while positive means above the target (target is in between the chaser and the planet).
     * @param numberOfTeardrops Number of teardrop orbits to perform. Must be ≥ 1.
     * @param <T> field.
     * @return List of waypoints in time. Date, position, and velocity are non-zero.
     */
    <T extends CalculusFieldElement<T>> List<TimeStampedFieldPVCoordinates<T>> computeTeardropWaypoints(FieldAbsoluteDate<T> injectionDate, FieldOrbit<T> targetOrbit, T turnAroundDistance, T maneuverDistance, int numberOfTeardrops);
}

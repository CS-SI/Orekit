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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.relative.FieldRelativeProvider;
import org.orekit.propagation.relative.RelativeProvider;
import org.orekit.propagation.relative.clohessywiltshire.ClohessyWiltshireEquations;
import org.orekit.propagation.relative.clohessywiltshire.ClohessyWiltshireMatrices;
import org.orekit.propagation.relative.clohessywiltshire.ClohessyWiltshireProvider;
import org.orekit.propagation.relative.clohessywiltshire.ClohessyWiltshireRendezVous;
import org.orekit.propagation.relative.clohessywiltshire.FieldClohessyWiltshireEquations;
import org.orekit.propagation.relative.clohessywiltshire.FieldClohessyWiltshireMatrices;
import org.orekit.propagation.relative.clohessywiltshire.FieldClohessyWiltshireProvider;
import org.orekit.propagation.relative.clohessywiltshire.FieldClohessyWiltshireRendezVous;
import org.orekit.propagation.relative.maneuver.ClohessyWiltshireManeuver;
import org.orekit.propagation.relative.maneuver.FieldClohessyWiltshireManeuver;
import org.orekit.propagation.relative.maneuver.FieldRelativeManeuver;
import org.orekit.propagation.relative.maneuver.FieldYamanakaAnkersenManeuver;
import org.orekit.propagation.relative.maneuver.RelativeManeuver;
import org.orekit.propagation.relative.maneuver.YamanakaAnkersenManeuver;
import org.orekit.propagation.relative.yamanakaankersen.FieldYamanakaAnkersenProvider;
import org.orekit.propagation.relative.yamanakaankersen.FieldYamanakaAnkersenRendezVous;
import org.orekit.propagation.relative.yamanakaankersen.YamanakaAnkersenProvider;
import org.orekit.propagation.relative.yamanakaankersen.YamanakaAnkersenRendezVous;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Enumeration used to compute the relative maneuvers based on Clohessy-Wiltshire equations (only Circular cases) or Yamanaka-Ankersen equations.
 *
 * @author Romain Cuvillon
 * @since 14.0
 */
public enum RPOModel implements RPO {
    /**
     * CW: Clohessy-Wiltshire.
     */
    CW {
        /** {@inheritDoc} */
        public Vector3D getRBarDirection() {
            return Vector3D.PLUS_I;
        }

        /** {@inheritDoc} */
        public Vector3D getVBarDirection() {
            return Vector3D.PLUS_J;
        }

        /** {@inheritDoc} */
        public Vector3D getOutOfPlaneDirection() {
            return Vector3D.PLUS_K;
        }

        /** {@inheritDoc} */
        public LOFType getLOFType() { return LOFType.QSW; }

        /**
         ** Computes the waypoints of the teardrop relative orbit in QSW Local Orbital Frame to use them with Clohessy-Wiltshire maneuvers.
         *
         * <p>The injection point is the turn-around point of the teardrop (the round end).</p>
         * <p>All maneuvers are performed at the pointy end of the teardrop.</p>
         *
         * @param injectionDate Date of injection in the teardrop.
         * @param targetOrbit target's orbit.
         * @param turnAroundDistance Turn-around distance. This is the "round" end of the orbit. Note that this distance is signed : negative means below the target spacecraft (in between the planet and the target), while positive means above the target (target is in between the chaser and the planet).
         * @param maneuverDistance Maneuver distance of the teardrop orbit. This is the "pointy" end of the orbit. Note that this distance is signed : negative means below the target spacecraft (in between the planet and the target), while positive means above the target (target is in between the chaser and the planet).
         * @param numberOfTeardrops Number of teardrop orbits to perform. Must be ≥ 1.
         * @return List of waypoints in time. Date, position, and velocity are non-zero.
         */
        public List<TimeStampedPVCoordinates> computeTeardropWaypoints(final AbsoluteDate injectionDate, final Orbit targetOrbit, final double turnAroundDistance, final double maneuverDistance, final int numberOfTeardrops) {
            return new TeardropCircularWaypointCalculator(targetOrbit.getKeplerianMeanMotion(), turnAroundDistance, maneuverDistance, numberOfTeardrops).computeTearDropWaypoints(injectionDate);
        }

        /**
         ** Computes the waypoints of the teardrop relative orbit in QSW Local Orbital Frame to use them with Clohessy-Wiltshire maneuvers.
         *
         * <p>The injection point is the turn-around point of the teardrop (the round end).</p>
         * <p>All maneuvers are performed at the pointy end of the teardrop.</p>
         *
         * @param injectionDate Date of injection in the teardrop.
         * @param targetOrbit target's orbit.
         * @param turnAroundDistance Turn-around distance. This is the "round" end of the orbit. Note that this distance is signed : negative means below the target spacecraft (in between the planet and the target), while positive means above the target (target is in between the chaser and the planet).
         * @param maneuverDistance Maneuver distance of the teardrop orbit. This is the "pointy" end of the orbit. Note that this distance is signed : negative means below the target spacecraft (in between the planet and the target), while positive means above the target (target is in between the chaser and the planet).
         * @param numberOfTeardrops Number of teardrop orbits to perform. Must be ≥ 1.
         * @param <T> field.
         * @return List of waypoints in time. Date, position, and velocity are non-zero.
         */
        public <T extends CalculusFieldElement<T>> List<TimeStampedFieldPVCoordinates<T>> computeTeardropWaypoints(final FieldAbsoluteDate<T> injectionDate, final FieldOrbit<T> targetOrbit, final T turnAroundDistance, final T maneuverDistance, final int numberOfTeardrops) {
            return new FieldTeardropCircularWaypointCalculator<T>(targetOrbit.getKeplerianMeanMotion(), turnAroundDistance, maneuverDistance, numberOfTeardrops).computeTearDropWaypoints(injectionDate);
        }

        /**
         * Compute relative maneuvers to realize a forced trajectory defined by the waypoints (ForcedLinear/ForcedCircular)
         * using Clohessy-Wiltshire model.
         * @param waypoints Waypoints of the trajectory in QSW frame.
         * @param initialVelocity Initial velocity in the Clohessy-Wiltshire (QSW) frame.
         * @param targetOrbit Orbit of the target.
         * @param cwProvider Clohessy-Wiltshire provider.
         * @return list of relative maneuvers in Clohessy-Wiltshire frame (QSW).
         */
        public List<RelativeManeuver> computeForcedManeuvers(final List<TimeStampedPVCoordinates> waypoints, final Vector3D initialVelocity, final Orbit targetOrbit, final RelativeProvider cwProvider) {
            final List<ClohessyWiltshireManeuver> maneuvers = new ArrayList<>();
            Vector3D velocityBeforeManeuver = initialVelocity;
            final LocalOrbitalFrame targetLof = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.QSW, targetOrbit, LOFType.QSW.getName());
            for (int i = 0; i < waypoints.size() - 1; i++) {
                // Define Current waypoint and next waypoint in QSW
                final TimeStampedPVCoordinates currentWaypoint = waypoints.get(i);
                final TimeStampedPVCoordinates nextWaypoint = waypoints.get(i + 1);
                // Define Date Detector to trigger the maneuver
                final EventDetector firstImpulseTrigger = new DateDetector(currentWaypoint.getDate());
                // Compute the velocity after the maneuver to arrive at the following waypoint at the correct date using RendezVous.
                final Vector3D velocityAfterManeuver = ClohessyWiltshireRendezVous.computeRendezVous(currentWaypoint, nextWaypoint, targetLof, targetOrbit).getPvt1().getVelocity();
                // Compute the impulsion at the current waypoint to reach the next waypoint.
                final Vector3D deltaV = velocityAfterManeuver.subtract(velocityBeforeManeuver);
                // Create the Clohessy-Wiltshire maneuver and add it to the list.
                final ClohessyWiltshireManeuver maneuver = new ClohessyWiltshireManeuver(firstImpulseTrigger, deltaV, (ClohessyWiltshireProvider) cwProvider);
                maneuvers.add(maneuver);
                // Compute the velocity before the maneuver at the next waypoint.
                final double transferDuration = nextWaypoint.getDate().durationFrom(currentWaypoint.getDate());
                final ClohessyWiltshireMatrices matrices = ClohessyWiltshireEquations.computeMatrices(transferDuration, targetOrbit.getKeplerianMeanMotion());
                velocityBeforeManeuver = new Vector3D(matrices.getPhiVR().operate(MatrixUtils.createRealVector(currentWaypoint.getPosition().toArray()))
                        .add(matrices.getPhiVV().operate(MatrixUtils.createRealVector(velocityAfterManeuver.toArray()))).toArray());
            }
            return new ArrayList<>(maneuvers);
        }

        /**
         * Compute relative maneuvers to realize a forced trajectory defined by the waypoints (ForcedLinear/ForcedCircular)
         * using Clohessy-Wiltshire model.
         * @param waypoints Waypoints of the trajectory in QSW frame.
         * @param initialVelocity Initial velocity in the Clohessy-Wiltshire (QSW) frame.
         * @param targetOrbit Orbit of the target.
         * @param cwProvider Clohessy-Wiltshire provider.
         * @param <T> field.
         * @return list of relative maneuvers in Clohessy-Wiltshire frame (QSW).
         */
        public <T extends CalculusFieldElement<T>> List<FieldRelativeManeuver<T>> computeForcedManeuvers(
                final List<TimeStampedFieldPVCoordinates<T>> waypoints, final FieldVector3D<T> initialVelocity,
                final FieldOrbit<T> targetOrbit, final FieldRelativeProvider<T> cwProvider) {
            final List<FieldClohessyWiltshireManeuver<T>> maneuvers = new ArrayList<>();
            FieldVector3D<T> velocityBeforeManeuver = initialVelocity;
            final LocalOrbitalFrame targetLof = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.QSW, targetOrbit.toOrbit(), LOFType.QSW.getName());
            for (int i = 0; i < waypoints.size() - 1; i++) {
                // Define Current waypoint and next waypoint in QSW
                final TimeStampedFieldPVCoordinates<T> currentWaypoint = waypoints.get(i);
                final TimeStampedFieldPVCoordinates<T> nextWaypoint = waypoints.get(i + 1);
                // Define Date Detector to trigger the maneuver
                final FieldEventDetector<T> firstImpulseTrigger = new FieldDateDetector<>(currentWaypoint.getDate());
                // Compute the velocity after the maneuver to arrive at the following waypoint at the correct date using RendezVous.
                final FieldVector3D<T> velocityAfterManeuver = (new FieldClohessyWiltshireRendezVous<T>()).computeRendezVous(currentWaypoint, nextWaypoint, targetLof, targetOrbit).getPvt1().getVelocity();
                // Compute the impulsion at the current waypoint to reach the next waypoint.
                final FieldVector3D<T> deltaV = velocityAfterManeuver.subtract(velocityBeforeManeuver);
                // Create the Clohessy-Wiltshire maneuver and add it to the list.
                final FieldClohessyWiltshireManeuver<T> maneuver = new FieldClohessyWiltshireManeuver<>(firstImpulseTrigger, deltaV, (FieldClohessyWiltshireProvider<T>) cwProvider);
                maneuvers.add(maneuver);
                // Compute the velocity before the maneuver at the next waypoint.
                final T transferDuration = nextWaypoint.getDate().durationFrom(currentWaypoint.getDate());
                final FieldClohessyWiltshireMatrices<T> matrices = (new FieldClohessyWiltshireEquations<T>()).computeMatrices(transferDuration, targetOrbit.getKeplerianMeanMotion());
                velocityBeforeManeuver = new FieldVector3D<>(matrices.getPhiVR().operate(MatrixUtils.createFieldVector(currentWaypoint.getPosition().toArray()))
                        .add(matrices.getPhiVV().operate(MatrixUtils.createFieldVector(velocityAfterManeuver.toArray()))).toArray());
            }
            return new ArrayList<>(maneuvers);
        }

        /**
         * Computes the Clohessy-Wilstshire based maneuvers of the teardrop relative orbit in QSW Local Orbital Frame.
         * <p>The injection point is the turn-around point of the teardrop (the round end).</p>
         * <p>All maneuvers happen at the pointy end of the teardrop.</p>
         * @param waypoints List of the successive waypoints of the target.
         * @param cwProvider Clohessy-Wiltshire provider.
         * @return list of Clohessy-Wiltshire maneuvers.
         */
        public List<RelativeManeuver> computeTeardropManeuvers(final List<TimeStampedPVCoordinates> waypoints, final RelativeProvider cwProvider) {
            final List<ClohessyWiltshireManeuver> maneuvers = new ArrayList<>();
            // Creation of the maneuvers at the maneuver point of the teardrop.
            for (int i = 1; i < waypoints.size(); i++) {
                final TimeStampedPVCoordinates maneuverWaypoint = waypoints.get(i);
                final Vector3D maneuverVelocity = maneuverWaypoint.getVelocity();
                final EventDetector maneuverDate = new DateDetector(maneuverWaypoint.getDate());
                final TimeStampedPVCoordinates pvtBeforeMan = new TimeStampedPVCoordinates(maneuverWaypoint.getDate(),
                        maneuverWaypoint.getPosition(),
                        new Vector3D(-maneuverVelocity.getX(), maneuverVelocity.getY(), maneuverVelocity.getZ()));
                final Vector3D deltaV =  maneuverVelocity.subtract(pvtBeforeMan.getVelocity());
                final ClohessyWiltshireManeuver maneuver = new ClohessyWiltshireManeuver(maneuverDate, deltaV, (ClohessyWiltshireProvider) cwProvider);
                maneuvers.add(maneuver);
            }
            return new ArrayList<>(maneuvers);
        }

        /**
         * Computes the Clohessy-Wilstshire based maneuvers of the teardrop relative orbit in QSW Local Orbital Frame.
         * <p>The injection point is the turn-around point of the teardrop (the round end).</p>
         * <p>All maneuvers happen at the pointy end of the teardrop.</p>
         * @param waypoints List of the successive waypoints of the target.
         * @param cwProvider Clohessy-Wiltshire provider.
         * @param <T> field.
         * @return list of Clohessy-Wiltshire maneuvers.
         */
        public <T extends CalculusFieldElement<T>> List<FieldRelativeManeuver<T>> computeTeardropManeuvers(
                final List<TimeStampedFieldPVCoordinates<T>> waypoints, final FieldRelativeProvider<T> cwProvider) {
            final List<FieldClohessyWiltshireManeuver<T>> maneuvers = new ArrayList<>();
            // Creation of the maneuvers at the maneuver point of the teardrop.
            for (int i = 1; i < waypoints.size(); i++) {
                final TimeStampedFieldPVCoordinates<T> maneuverWaypoint = waypoints.get(i);
                final FieldVector3D<T> maneuverVelocity = maneuverWaypoint.getVelocity();
                final FieldEventDetector<T> maneuverDate = new FieldDateDetector<>(maneuverWaypoint.getDate());
                final TimeStampedFieldPVCoordinates<T> pvtBeforeMan = new TimeStampedFieldPVCoordinates<>(maneuverWaypoint.getDate(),
                        new FieldPVCoordinates<>(maneuverWaypoint.getPosition(),
                        new FieldVector3D<>(maneuverVelocity.getX().negate(), maneuverVelocity.getY(), maneuverVelocity.getZ())));
                final FieldVector3D<T> deltaV =  maneuverVelocity.subtract(pvtBeforeMan.getVelocity());
                final FieldClohessyWiltshireManeuver<T> maneuver = new FieldClohessyWiltshireManeuver<>(maneuverDate, deltaV, (FieldClohessyWiltshireProvider<T>) cwProvider);
                maneuvers.add(maneuver);
            }
            return new ArrayList<>(maneuvers);
        }
    },
    /**
     * YA : Yamanaka-Ankersen.
     */
    YA {
        /** {@inheritDoc} */
        public Vector3D getRBarDirection() {
            return Vector3D.MINUS_K;
        }

        /** {@inheritDoc} */
        public Vector3D getVBarDirection() {
            return Vector3D.PLUS_I;
        }

        /** {@inheritDoc} */
        public Vector3D getOutOfPlaneDirection() {
            return Vector3D.MINUS_J;
        }

        /** {@inheritDoc} */
        public LOFType getLOFType() {  return LOFType.LVLH_CCSDS; }

        /**
         ** Computes the waypoints of the teardrop relative orbit in LVLH Local Orbital Frame to use them with Yamanaka-Ankersen maneuvers.
         * A teardrop is analytically feasible only if the target's orbit is circular.
         * If the orbit of the target is eccentric, an error is thrown to prevent it.
         *
         * <p>The injection point is the turn-around point of the teardrop (the round end).</p>
         * <p>All maneuvers are performed at the pointy end of the teardrop.</p>
         *
         * @param injectionDate Date of injection in the teardrop.
         * @param targetOrbit target's orbit.
         * @param turnAroundDistance Turn-around distance. This is the "round" end of the orbit. Note that this distance is signed : negative means below the target spacecraft (in between the planet and the target), while positive means above the target (target is in between the chaser and the planet).
         * @param maneuverDistance Maneuver distance of the teardrop orbit. This is the "pointy" end of the orbit. Note that this distance is signed : negative means below the target spacecraft (in between the planet and the target), while positive means above the target (target is in between the chaser and the planet).
         * @param numberOfTeardrops Number of teardrop orbits to perform. Must be ≥ 1.
         * @return List of waypoints in time. Date, position, and velocity are non-zero.
         */
        public List<TimeStampedPVCoordinates> computeTeardropWaypoints(final AbsoluteDate injectionDate, final Orbit targetOrbit, final double turnAroundDistance, final double maneuverDistance, final int numberOfTeardrops) {
            if (targetOrbit.getE() != 0) {
                throw new UnsupportedOperationException("A teardrop is not analytically feasible around an eccentric orbit.");
            } else {
                final List<TimeStampedPVCoordinates> waypointsQSW = new TeardropCircularWaypointCalculator(targetOrbit.getKeplerianMeanMotion(), turnAroundDistance, maneuverDistance, numberOfTeardrops).computeTearDropWaypoints(injectionDate);
                final List<TimeStampedPVCoordinates> waypointsLVLH = new ArrayList<>();
                for (TimeStampedPVCoordinates waypoint : waypointsQSW) {
                    final TimeStampedPVCoordinates waypointLVLH = LOFType.LVLH_CCSDS.transformFromLOF(LOFType.QSW, waypoint.getDate(), waypoint).transformPVCoordinates(waypoint);
                    waypointsLVLH.add(waypointLVLH);
                }
                return waypointsLVLH;
            }
        }

        /**
         ** Computes the waypoints of the teardrop relative orbit in LVLH Local Orbital Frame to use them with Yamanaka-Ankersen maneuvers.
         * A teardrop is analytically feasible only if the target's orbit is circular.
         * If the orbit of the target is eccentric, an error is thrown to prevent it.
         *
         * <p>The injection point is the turn-around point of the teardrop (the round end).</p>
         * <p>All maneuvers are performed at the pointy end of the teardrop.</p>
         *
         * @param injectionDate Date of injection in the teardrop.
         * @param targetOrbit target's orbit.
         * @param turnAroundDistance Turn-around distance. This is the "round" end of the orbit. Note that this distance is signed : negative means below the target spacecraft (in between the planet and the target), while positive means above the target (target is in between the chaser and the planet).
         * @param maneuverDistance Maneuver distance of the teardrop orbit. This is the "pointy" end of the orbit. Note that this distance is signed : negative means below the target spacecraft (in between the planet and the target), while positive means above the target (target is in between the chaser and the planet).
         * @param numberOfTeardrops Number of teardrop orbits to perform. Must be ≥ 1.
         * @param <T> field.
         * @return List of waypoints in time. Date, position, and velocity are non-zero.
         */
        public <T extends CalculusFieldElement<T>> List<TimeStampedFieldPVCoordinates<T>> computeTeardropWaypoints(final FieldAbsoluteDate<T> injectionDate, final FieldOrbit<T> targetOrbit, final T turnAroundDistance, final T maneuverDistance, final int numberOfTeardrops) {
            if (targetOrbit.getE() != injectionDate.getField().getZero()) {
                throw new UnsupportedOperationException("A teardrop is not analytically feasible around an eccentric orbit");
            } else {
                final List<TimeStampedFieldPVCoordinates<T>> waypointsQSW = new FieldTeardropCircularWaypointCalculator<>(targetOrbit.getKeplerianMeanMotion(), turnAroundDistance, maneuverDistance, numberOfTeardrops).computeTearDropWaypoints(injectionDate);
                final List<TimeStampedFieldPVCoordinates<T>> waypointsLVLH = new ArrayList<>();
                for (TimeStampedFieldPVCoordinates<T> waypoint : waypointsQSW) {
                    final TimeStampedFieldPVCoordinates<T> waypointLVLH = LOFType.LVLH_CCSDS.transformFromLOF(LOFType.QSW, waypoint.getDate(), waypoint).transformPVCoordinates(waypoint);
                    waypointsLVLH.add(waypointLVLH);
                }
                return waypointsLVLH;
            }
        }

        /**
         * Compute relative maneuvers to realize a forced trajectory defined by the waypoints (ForcedLinear/ForcedCircular)
         * using Yamanaka-Ankersen model.
         * @param waypoints Waypoints of the trajectory in LVLH_CCSDS frame.
         * @param initialVelocity Initial velocity in the Yamanaka-Ankersen (LVLH_CCSDS) frame.
         * @param targetOrbit Orbit of the target.
         * @param yaProvider Yamanaka-Ankersen provider.
         * @return list of relative maneuvers in Yamanaka-Ankersen model's frame (LVLH_CCSDS).
         */
        public List<RelativeManeuver> computeForcedManeuvers(final List<TimeStampedPVCoordinates> waypoints, final Vector3D initialVelocity, final Orbit targetOrbit,
                                                                     final RelativeProvider yaProvider) {
            final List<YamanakaAnkersenManeuver> maneuvers = new ArrayList<>();
            Vector3D velocityBeforeManeuver = initialVelocity;
            KeplerianOrbit orbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(targetOrbit);
            yaProvider.setTargetOrbit(orbit);
            final KeplerianPropagator targetPropagator = new KeplerianPropagator(targetOrbit);
            for (int i = 0; i < waypoints.size() - 1; i++) {
                final LocalOrbitalFrame lofUpdated = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.LVLH_CCSDS, orbit, LOFType.LVLH_CCSDS.getName());
                // Define Current waypoint and next waypoint in QSW
                final TimeStampedPVCoordinates currentWaypoint = waypoints.get(i);
                final TimeStampedPVCoordinates nextWaypoint = waypoints.get(i + 1);
                // Define Date Detector to trigger the maneuver
                final EventDetector firstImpulseTrigger = new DateDetector(currentWaypoint.getDate());
                // Compute the velocity after the maneuver to arrive at the following waypoint at the correct date using RendezVous.
                final Vector3D velocityAfterManeuver = YamanakaAnkersenRendezVous.computeRendezVous(currentWaypoint, nextWaypoint, lofUpdated, orbit, targetPropagator).
                        getPvt1().getVelocity();
                // Compute the impulsion at the current waypoint to reach the next waypoint.
                final Vector3D deltaV = velocityAfterManeuver.subtract(velocityBeforeManeuver);
                // Create the Yamanaka-Ankersen maneuver and add it to the list.
                final YamanakaAnkersenManeuver maneuver = new YamanakaAnkersenManeuver(firstImpulseTrigger, deltaV, (YamanakaAnkersenProvider) yaProvider);
                maneuvers.add(maneuver);
                // Create a Yamanaka-Ankersen provider to propagate to the next waypoint.
                final YamanakaAnkersenProvider yaProviderManeuver = new YamanakaAnkersenProvider(orbit, new TimeStampedPVCoordinates(currentWaypoint.getDate(), currentWaypoint.getPosition(), velocityAfterManeuver), "FieldProvider_" + i);
                targetPropagator.addAdditionalDataProvider(yaProviderManeuver);
                // Propagate the waypoint.
                final SpacecraftState propagated = targetPropagator.propagate(nextWaypoint.getDate());
                // Update the target orbit and the velocity before the next maneuver.
                final double trueAnomaly = ((KeplerianOrbit) OrbitType.KEPLERIAN.convertType(propagated.getOrbit())).getTrueAnomaly();
                orbit = new KeplerianOrbit(orbit.getA(), orbit.getE(), orbit.getI(), orbit.getPerigeeArgument(), orbit.getRightAscensionOfAscendingNode(), trueAnomaly, PositionAngleType.TRUE, orbit.getFrame(), nextWaypoint.getDate(), orbit.getMu());
                velocityBeforeManeuver = new Vector3D(Arrays.copyOfRange(yaProviderManeuver.getAdditionalData(propagated), 3, 6));
            }
            return new ArrayList<>(maneuvers);
        }

        /**
         * Compute relative maneuvers to realize a forced trajectory defined by the waypoints (ForcedLinear/ForcedCircular)
         * using Yamanaka-Ankersen model.
         * @param waypoints Waypoints of the trajectory in LVLH_CCSDS frame.
         * @param initialVelocity Initial velocity in the Yamanaka-Ankersen (LVLH_CCSDS) frame.
         * @param targetOrbit Orbit of the target.
         * @param yaProvider Yamanaka-Ankersen provider.
         * @param <T> field.
         * @return list of relative maneuvers in Yamanaka-Ankersen model's frame (LVLH_CCSDS).
         */
        public <T extends CalculusFieldElement<T>> List<FieldRelativeManeuver<T>> computeForcedManeuvers(
                final List<TimeStampedFieldPVCoordinates<T>> waypoints, final FieldVector3D<T> initialVelocity,
                final FieldOrbit<T> targetOrbit, final FieldRelativeProvider<T> yaProvider) {
            final List<FieldYamanakaAnkersenManeuver<T>> maneuvers = new ArrayList<>();
            FieldVector3D<T> velocityBeforeManeuver = initialVelocity;
            FieldKeplerianOrbit<T> orbit = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(targetOrbit);
            yaProvider.setTargetOrbit(orbit);
            final FieldKeplerianPropagator<T> targetPropagator = new FieldKeplerianPropagator<>(targetOrbit);
            for (int i = 0; i < waypoints.size() - 1; i++) {
                final LocalOrbitalFrame lofUpdated = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.LVLH_CCSDS, orbit.toOrbit(), LOFType.LVLH_CCSDS.getName());
                // Define Current waypoint and next waypoint in QSW
                final TimeStampedFieldPVCoordinates<T> currentWaypoint = waypoints.get(i);
                final TimeStampedFieldPVCoordinates<T> nextWaypoint = waypoints.get(i + 1);
                // Define Date Detector to trigger the maneuver
                final FieldEventDetector<T> firstImpulseTrigger = new FieldDateDetector<>(currentWaypoint.getDate());
                // Compute the velocity after the maneuver to arrive at the following waypoint at the correct date using RendezVous.
                final FieldVector3D<T> velocityAfterManeuver = (new FieldYamanakaAnkersenRendezVous<T>()).computeRendezVous(currentWaypoint, nextWaypoint, lofUpdated, orbit, targetPropagator).
                        getPvt1().getVelocity();
                // Compute the impulsion at the current waypoint to reach the next waypoint.
                final FieldVector3D<T> deltaV = velocityAfterManeuver.subtract(velocityBeforeManeuver);
                // Create the Yamanaka-Ankersen maneuver and add it to the list.
                final FieldYamanakaAnkersenManeuver<T> maneuver = new FieldYamanakaAnkersenManeuver<>(firstImpulseTrigger, deltaV, (FieldYamanakaAnkersenProvider<T>) yaProvider);
                maneuvers.add(maneuver);
                // Create a Yamanaka-Ankersen provider to propagate to the next waypoint.
                final FieldYamanakaAnkersenProvider<T> yaProviderManeuver = new FieldYamanakaAnkersenProvider<>(orbit, new TimeStampedFieldPVCoordinates<>(currentWaypoint.getDate(), new FieldPVCoordinates<>(currentWaypoint.getPosition(), velocityAfterManeuver)), "Provider_" + i);
                targetPropagator.addAdditionalDataProvider(yaProviderManeuver);
                // Propagate the waypoint.
                final FieldSpacecraftState<T> propagated = targetPropagator.propagate(nextWaypoint.getDate());
                // Update the target orbit and the velocity before the next maneuver.
                final T trueAnomaly = ((FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(propagated.getOrbit())).getTrueAnomaly();
                orbit = new FieldKeplerianOrbit<>(orbit.getA(), orbit.getE(), orbit.getI(), orbit.getPerigeeArgument(), orbit.getRightAscensionOfAscendingNode(), trueAnomaly, PositionAngleType.TRUE, orbit.getFrame(), nextWaypoint.getDate(), orbit.getMu());
                velocityBeforeManeuver = new FieldVector3D<>(Arrays.copyOfRange(yaProviderManeuver.getAdditionalData(propagated), 3, 6));
            }
            return new ArrayList<>(maneuvers);
        }

        /**
         * Computes the Yamanaka-Ankersen based maneuvers of the teardrop relative orbit in LVLH_CCSDS Local Orbital Frame.
         * <p>The injection point is the turn-around point of the teardrop (the round end).</p>
         * <p>All maneuvers happen at the pointy end of the teardrop.</p>
         * @param waypoints List of the successive waypoints of the target.
         * @param yaProvider Clohessy-Wiltshire provider.
         * @return list of Clohessy-Wiltshire maneuvers.
         */
        public List<RelativeManeuver> computeTeardropManeuvers(final List<TimeStampedPVCoordinates> waypoints, final RelativeProvider yaProvider) {
            final List<YamanakaAnkersenManeuver> maneuvers = new ArrayList<>();
            // Creation of the maneuvers at the maneuver point of the teardrop.
            for (int i = 1; i < waypoints.size(); i++) {
                final TimeStampedPVCoordinates maneuverWaypoint = waypoints.get(i);
                final Vector3D maneuverVelocity = maneuverWaypoint.getVelocity();
                final EventDetector maneuverDate = new DateDetector(maneuverWaypoint.getDate());
                final TimeStampedPVCoordinates pvtBeforeMan = new TimeStampedPVCoordinates(maneuverWaypoint.getDate(),
                        maneuverWaypoint.getPosition(),
                        new Vector3D(maneuverVelocity.getX(), -maneuverVelocity.getY(), -maneuverVelocity.getZ()));
                final Vector3D deltaV =  maneuverVelocity.subtract(pvtBeforeMan.getVelocity());
                final YamanakaAnkersenManeuver maneuver = new YamanakaAnkersenManeuver(maneuverDate, deltaV, (YamanakaAnkersenProvider) yaProvider);
                maneuvers.add(maneuver);
            }
            return new ArrayList<>(maneuvers);
        }

        /**
         * Computes the Yamanaka-Ankersen based maneuvers of the teardrop relative orbit in LVLH_CCSDS Local Orbital Frame.
         * <p>The injection point is the turn-around point of the teardrop (the round end).</p>
         * <p>All maneuvers happen at the pointy end of the teardrop.</p>
         * @param waypoints List of the successive waypoints of the target.
         * @param yaProvider Clohessy-Wiltshire provider.
         * @param <T> field.
         * @return list of Clohessy-Wiltshire maneuvers.
         */
        public <T extends CalculusFieldElement<T>> List<FieldRelativeManeuver<T>> computeTeardropManeuvers(
                final List<TimeStampedFieldPVCoordinates<T>> waypoints, final FieldRelativeProvider<T> yaProvider) {
            final List<FieldYamanakaAnkersenManeuver<T>> maneuvers = new ArrayList<>();
            // Creation of the maneuvers at the maneuver point of the teardrop.
            for (int i = 1; i < waypoints.size(); i++) {
                final TimeStampedFieldPVCoordinates<T> maneuverWaypoint = waypoints.get(i);
                final FieldVector3D<T> maneuverVelocity = maneuverWaypoint.getVelocity();
                final FieldEventDetector<T> maneuverDate = new FieldDateDetector<>(maneuverWaypoint.getDate());
                final TimeStampedFieldPVCoordinates<T> pvtBeforeMan = new TimeStampedFieldPVCoordinates<>(maneuverWaypoint.getDate(),
                        new FieldPVCoordinates<>(maneuverWaypoint.getPosition(),
                        new FieldVector3D<>(maneuverVelocity.getX(), maneuverVelocity.getY().negate(), maneuverVelocity.getZ().negate())));
                final FieldVector3D<T> deltaV =  maneuverVelocity.subtract(pvtBeforeMan.getVelocity());
                final FieldYamanakaAnkersenManeuver<T> maneuver = new FieldYamanakaAnkersenManeuver<>(maneuverDate, deltaV, (FieldYamanakaAnkersenProvider<T>) yaProvider);
                maneuvers.add(maneuver);
            }
            return new ArrayList<>(maneuvers);
        }
    }
}

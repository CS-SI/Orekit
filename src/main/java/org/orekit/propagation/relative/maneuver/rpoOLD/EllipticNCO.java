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
package org.orekit.propagation.relative.maneuver.rpoOLD;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.forces.maneuvers.FieldImpulseManeuver;
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.relative.maneuver.FieldYamanakaAnkersenManeuver;
import org.orekit.propagation.relative.maneuver.YamanakaAnkersenManeuver;
import org.orekit.propagation.relative.yamanakaankersen.FieldYamanakaAnkersenProvider;
import org.orekit.propagation.relative.yamanakaankersen.YamanakaAnkersenProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the orbit of a chaser which naturally circumnavigates around a target satellite in an elliptic orbit.
 * Source : Generating Orbital Elements for Natural Motion Circumnavigation Guidance, Donald Tong, April 2024.
 * <a href="https://www.researchgate.net/publication/379606783_GENERATIING_ORBITAL_ELEMENTS_FOR_NATURAL_MOTION_CIRCUMNAVIGATION_GUIDANCE">...</a>
 *
 * @author Romain Cuvillon
 * @since 14.0
 */
public class EllipticNCO {

    private EllipticNCO() {
    }

    /**
     * Computes the chaser orbit to naturally circumnavigate around a target satellite in an elliptic orbit.
     *
     * @param targetOrbit        orbit of the target.
     * @param semiMinorAxis      semi minor axis of the relative circumnavigation orbit.
     * @param xPlaneOffset       cross plane extent due to tilting the relative orbit. (expressed in meters)
     * @param vBarOffset         desired chaser ellipse offset from the target v-bar location. (expressed in meters)
     * @param driftPerOrbit      corkscrew v-bar motion, drift in meters per orbit.
     * @param xPlaneIVectorPhase clockwise angle measured from the velocity vector in the radial-velocity plane, defines the tilt-axis of the cross plane.
     * @return KeplerianOrbit fo the chaser to naturally circumnavigate around the target.
     */
    public static KeplerianOrbit computeChaserOrbit(final KeplerianOrbit targetOrbit, final double semiMinorAxis, final double xPlaneOffset, final double vBarOffset, final double driftPerOrbit, final double xPlaneIVectorPhase) {

        final double deltaA = -driftPerOrbit / (3 * FastMath.PI);
        final double chaserA = targetOrbit.getA() + deltaA;

        final double deltaE = 2 * semiMinorAxis / (2 * targetOrbit.getA());
        final double chaserE = targetOrbit.getE() + deltaE;

        final double deltaI = xPlaneOffset / targetOrbit.getA();
        final double theta = xPlaneIVectorPhase + targetOrbit.getPerigeeArgument();
        double chaserRaan = 0.;
        double chaserI = 0.;

        if (FastMath.toDegrees(theta) < 90) {
            final double B = targetOrbit.getI();
            final double C = FastMath.acos(FastMath.sin(deltaI) * FastMath.sin(B) * FastMath.cos(theta) - FastMath.cos(deltaI) * FastMath.cos(B));
            chaserI = FastMath.PI - C;
            final double alpha = FastMath.asin(FastMath.sin(deltaI) * FastMath.sin(theta) / FastMath.sin(C));
            chaserRaan = targetOrbit.getRightAscensionOfAscendingNode() + alpha;
        } else {
            if (FastMath.toDegrees(theta) < 180) {
                final double gamma = FastMath.PI - theta;
                final double B = FastMath.PI - targetOrbit.getI();
                final double C = FastMath.acos(FastMath.sin(deltaI) * FastMath.sin(B) * FastMath.cos(gamma) - FastMath.cos(deltaI) * FastMath.cos(B));
                chaserI = C;
                final double alpha = FastMath.asin(FastMath.sin(deltaI) * FastMath.sin(gamma) / FastMath.sin(C));
                chaserRaan = targetOrbit.getRightAscensionOfAscendingNode() + alpha;
            } else if (FastMath.toDegrees(theta) < 270) {
                final double gamma = theta - FastMath.PI;
                final double B = FastMath.PI - targetOrbit.getI();
                final double C = FastMath.acos(FastMath.sin(deltaI) * FastMath.sin(B) * FastMath.cos(gamma) - FastMath.cos(deltaI) * FastMath.cos(B));
                chaserI = C;
                final double alpha = FastMath.asin(FastMath.sin(deltaI) * FastMath.sin(gamma) / FastMath.sin(C));
                chaserRaan = targetOrbit.getRightAscensionOfAscendingNode() - alpha;
            } else if (FastMath.toDegrees(theta) < 360) {
                final double gamma = 2 * FastMath.PI - theta;
                final double B = targetOrbit.getI();
                final double C = FastMath.acos(FastMath.sin(deltaI) * FastMath.sin(B) * FastMath.cos(gamma) - FastMath.cos(deltaI) * FastMath.cos(B));
                chaserI = FastMath.PI - C;
                final double alpha = FastMath.asin(FastMath.sin(deltaI) * FastMath.sin(gamma) / FastMath.sin(C));
                chaserRaan = targetOrbit.getRightAscensionOfAscendingNode() - alpha;
            }
        }
        final double chaserPerigeeArgument = targetOrbit.getPerigeeArgument();

        final double f_xPlaneCenter = targetOrbit.getTrueAnomaly() - (chaserRaan - targetOrbit.getRightAscensionOfAscendingNode()) * FastMath.cos(targetOrbit.getI());
        final double deltaFinPlaneCenter = 2 * deltaE * FastMath.sin(targetOrbit.getTrueAnomaly());
        final double deltaFVBarOffset = vBarOffset / targetOrbit.getA();

        final double chaserTrueAnomaly = f_xPlaneCenter + deltaFinPlaneCenter + deltaFVBarOffset;

        return new KeplerianOrbit(chaserA, chaserE, chaserI, chaserPerigeeArgument, chaserRaan, chaserTrueAnomaly,
                PositionAngleType.TRUE, targetOrbit.getFrame(), targetOrbit.getDate(), targetOrbit.getMu());
    }

    /**
     * This method computes the waypoints of the Natural Circumnavigation orbit when the target orbit is in an elliptic orbit.
     * It has to be used with Yamanaka-Ankersen Maneuver and Provider as the waypoints are returned in LVLH CCSDS Frame.
     * First waypoint corresponds to the injection point. Following waypoints are the same TimeStampedPVCoordinates shifted by orbital periods.
     *
     * @param targetPropagator   propagator of the target.
     * @param injectionDate      injection date into the relative natural circumnavigation orbit.
     * @param relativeSma        semi minor axis of the relative orbit.
     * @param xPlaneOffset       cross plane extent due to tilting the relative orbit. (expressed in meters)
     * @param vBarOffset         desired chaser ellipse offset from the target v-bar location. (expressed in meters)
     * @param driftPerOrbit      corkscrew v-bar motion, drift in meters per orbit.
     * @param xPlaneIVectorPhase clockwise angle measured from the velocity vector in the radial-velocity plane, defines the tilt-axis of the cross plane.
     * @param numberOfOrbits     number of relative circumnavigation orbit to perform.
     * @return waypoints of the natural circumnavigation orbit.
     */
    public static List<TimeStampedPVCoordinates> createNaturalCircumnavigationWaypoints(final Propagator targetPropagator, final AbsoluteDate injectionDate, final double relativeSma, final double xPlaneOffset, final double vBarOffset, final double driftPerOrbit, final double xPlaneIVectorPhase, final int numberOfOrbits) {
        // Propagate the target to the desired start date
        final SpacecraftState targetStateAtInjection = targetPropagator.propagate(injectionDate);

        // Get target osculating Keplerian orbit
        final KeplerianOrbit targetOrbitAtInjection = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(targetStateAtInjection.getOrbit());
        // Build osculating Keplerian orbit for the chaser.
        final KeplerianOrbit chaserOrbit = computeChaserOrbit(targetOrbitAtInjection, relativeSma, xPlaneOffset, vBarOffset, driftPerOrbit, xPlaneIVectorPhase);

        // Compute target's LVLH CCSDS LOF to use Yamanaka-Ankersen equations.
        final LocalOrbitalFrame targetLof = new LocalOrbitalFrame(targetPropagator.getFrame(), LOFType.LVLH_CCSDS, targetPropagator, " LVLH CCSDS LOF target");

        // Get chaser PVT in target LVLH CCSDS LOF at injection date
        final TimeStampedPVCoordinates chaserInjectionPVTLVLH = chaserOrbit.getPVCoordinates(injectionDate, targetLof);

        // Create the list of waypoints
        final List<TimeStampedPVCoordinates> waypoints = new ArrayList<>();

        // Add injection point
        waypoints.add(chaserInjectionPVTLVLH);

        // Compute target's orbital period
        final double targetOrbitalPeriod = targetOrbitAtInjection.getKeplerianPeriod();

        // Ensure that the number of orbits is ≥ 1.
        final int actualNumberOfOrbits = FastMath.max(1, numberOfOrbits);

        // For each point, including the final point, add a waypoint
        for (int i = 1; i < actualNumberOfOrbits + 1; i++) {
            waypoints.add(new TimeStampedPVCoordinates(injectionDate.shiftedBy(i * targetOrbitalPeriod), chaserInjectionPVTLVLH.getPosition(), chaserInjectionPVTLVLH.getVelocity()));
        }
        return waypoints;
    }

    /**
     * Computes the chaser orbit to naturally circumnavigate around a target satellite in an elliptic orbit.
     *
     * @param targetOrbit        orbit of the target.
     * @param semiMinorAxis      semi minor axis of the relative circumnavigation orbit.
     * @param xPlaneOffset       cross plane extent due to tilting the relative orbit. (expressed in meters)
     * @param vBarOffset         desired chaser ellipse offset from the target v-bar location. (expressed in meters)
     * @param driftPerOrbit      corkscrew v-bar motion, drift in meters per orbit.
     * @param xPlaneIVectorPhase clockwise angle measured from the velocity vector in the radial-velocity plane, defines the tilt-axis of the cross plane.
     * @param <T>                type of the field elements.
     * @return KeplerianOrbit fo the chaser to naturally circumnavigate around the target.
     */
    public static <T extends CalculusFieldElement<T>> FieldKeplerianOrbit<T> computeChaserOrbit(final FieldKeplerianOrbit<T> targetOrbit, final T semiMinorAxis, final T xPlaneOffset, final T vBarOffset, final T driftPerOrbit, final T xPlaneIVectorPhase) {

        final Field<T> field = targetOrbit.getA().getField();
        final T deltaA = driftPerOrbit.negate().divide(3 * FastMath.PI);
        final T chaserA = targetOrbit.getA().add(deltaA);

        final T deltaE = semiMinorAxis.multiply(2).divide(targetOrbit.getA().multiply(2));
        final T chaserE = targetOrbit.getE().add(deltaE);

        final T deltaI = xPlaneOffset.divide(targetOrbit.getA());
        final T theta = xPlaneIVectorPhase.add(targetOrbit.getPerigeeArgument());
        T chaserRaan = field.getZero();
        T chaserI = field.getZero();
        final T pi = field.getOne().multiply(FastMath.PI);

        if (FastMath.toDegrees(theta.getReal()) < 90) {
            final T B = targetOrbit.getI();
            final T C = (deltaI.sin().multiply(B.sin().multiply(theta.cos())).subtract(deltaI.cos().multiply(B.cos()))).acos();
            chaserI = pi.subtract(C);
            final T alpha = (deltaI.sin().multiply(theta.sin()).divide(C.sin())).asin();
            chaserRaan = targetOrbit.getRightAscensionOfAscendingNode().add(alpha);
        } else {
            if (FastMath.toDegrees(theta.getReal()) < 180) {
                final T gamma = pi.subtract(theta);
                final T B = pi.subtract(targetOrbit.getI());
                final T C = (deltaI.sin().multiply(B.sin()).multiply(gamma.cos()).subtract(deltaI.cos().multiply(B.cos()))).acos();
                chaserI = C;
                final T alpha = (deltaI.sin().multiply(gamma.sin()).divide(C.sin())).asin();
                chaserRaan = targetOrbit.getRightAscensionOfAscendingNode().add(alpha);
            } else if (FastMath.toDegrees(theta.getReal()) < 270) {
                final T gamma = theta.subtract(pi);
                final T B = pi.subtract(targetOrbit.getI());
                final T C = (deltaI.sin().multiply(B.sin()).multiply(gamma.cos()).subtract(deltaI.cos().multiply(B.cos()))).acos();
                chaserI = C;
                final T alpha = (deltaI.sin().multiply(gamma.sin()).divide(C.sin())).asin();
                chaserRaan = targetOrbit.getRightAscensionOfAscendingNode().subtract(alpha);
            } else if (FastMath.toDegrees(theta.getReal()) < 360) {
                final T gamma = pi.multiply(2).subtract(theta);
                final T B = targetOrbit.getI();
                final T C = (deltaI.sin().multiply(B.sin()).multiply(gamma.cos()).subtract(deltaI.cos().multiply(B.cos()))).acos();
                chaserI = pi.subtract(C);
                final T alpha = (deltaI.sin().multiply(gamma.sin()).divide(C.sin())).asin();
                chaserRaan = targetOrbit.getRightAscensionOfAscendingNode().subtract(alpha);
            }
        }
        final T chaserPerigeeArgument = targetOrbit.getPerigeeArgument();

        final T f_xPlaneCenter = targetOrbit.getTrueAnomaly().subtract(chaserRaan.subtract(targetOrbit.getRightAscensionOfAscendingNode()).multiply(targetOrbit.getI().cos()));
        final T deltaFinPlaneCenter = deltaE.multiply(2).multiply(targetOrbit.getTrueAnomaly().sin());
        final T deltaFVBarOffset = vBarOffset.divide(targetOrbit.getA());

        final T chaserTrueAnomaly = f_xPlaneCenter.add(deltaFinPlaneCenter).add(deltaFVBarOffset);

        return new FieldKeplerianOrbit<>(chaserA, chaserE, chaserI, chaserPerigeeArgument, chaserRaan, chaserTrueAnomaly,
                PositionAngleType.TRUE, targetOrbit.getFrame(), targetOrbit.getDate(), targetOrbit.getMu());
    }

    /**
     * This method computes the waypoints of the Natural Circumnavigation orbit when the target orbit is in an elliptic orbit.
     * It has to be used with Yamanaka-Ankersen Maneuver and Provider as the waypoints are returned in LVLH CCSDS Frame.
     * First waypoint corresponds to the injection point. Following waypoints are the same TimeStampedPVCoordinates shifted by orbital periods.
     *
     * @param targetPropagator   propagator of the target.
     * @param injectionDate      injection date into the relative natural circumnavigation orbit.
     * @param relativeSma        semi minor axis of the relative orbit.
     * @param xPlaneOffset       cross plane extent due to tilting the relative orbit. (expressed in meters)
     * @param vBarOffset         desired chaser ellipse offset from the target v-bar location. (expressed in meters)
     * @param driftPerOrbit      corkscrew v-bar motion, drift in meters per orbit.
     * @param xPlaneIVectorPhase clockwise angle measured from the velocity vector in the radial-velocity plane, defines the tilt-axis of the cross plane.
     * @param numberOfOrbits     number of relative circumnavigation orbit to perform.
     * @param <T>                type of the field elements.
     * @return waypoints of the natural circumnavigation orbit.
     */
    public static <T extends CalculusFieldElement<T>> List<TimeStampedFieldPVCoordinates<T>> createNaturalCircumnavigationWaypoints(final FieldPropagator<T> targetPropagator, final FieldAbsoluteDate<T> injectionDate, final T relativeSma, final T xPlaneOffset, final T vBarOffset, final T driftPerOrbit, final T xPlaneIVectorPhase, final int numberOfOrbits) {
        // Propagate the target to the desired start date
        final FieldSpacecraftState<T> targetStateAtInjection = targetPropagator.propagate(injectionDate);

        // Get target osculating Keplerian orbit
        final FieldKeplerianOrbit<T> targetOrbitAtInjection = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(targetStateAtInjection.getOrbit());
        // Build osculating Keplerian orbit for the chaser.
        final FieldKeplerianOrbit<T> chaserOrbit = computeChaserOrbit(targetOrbitAtInjection, relativeSma, xPlaneOffset, vBarOffset, driftPerOrbit, xPlaneIVectorPhase);

        // Compute target's LVLH CCSDS LOF to use Yamanaka-Ankersen equations.
        final LocalOrbitalFrame targetLof = new LocalOrbitalFrame(targetPropagator.getFrame(), LOFType.LVLH_CCSDS, targetOrbitAtInjection.toOrbit(), "LVLH CCSDS LOF target");

        // Get chaser PVT in target LVLH CCSDS LOF at injection date.
        final TimeStampedFieldPVCoordinates<T> chaserInjectionPVTLVLH = chaserOrbit.getPVCoordinates(injectionDate, targetLof);

        // Create the list of waypoints.
        final List<TimeStampedFieldPVCoordinates<T>> waypoints = new ArrayList<>();

        // Add injection point.
        waypoints.add(chaserInjectionPVTLVLH);

        // Compute target's orbital period.
        final T targetOrbitalPeriod = targetOrbitAtInjection.getKeplerianPeriod();

        // Ensure that the number of orbits is ≥ 1.
        final int actualNumberOfOrbits = FastMath.max(1, numberOfOrbits);

        // For each point, including the final point, add a waypoint.
        for (int i = 1; i < actualNumberOfOrbits + 1; i++) {
            waypoints.add(new TimeStampedFieldPVCoordinates<>(injectionDate.shiftedBy(targetOrbitalPeriod.multiply(i)), new FieldPVCoordinates<>(chaserInjectionPVTLVLH.getPosition(), chaserInjectionPVTLVLH.getVelocity())));
        }
        return waypoints;
    }

    /**
     * Computes the Yamanaka-Ankersen injection maneuver in the Co-elliptic orbit in LVLH_CCSDS frame.
     * @param pvtBeforeInjection TimeStampedPVCoordinates before the injection.
     * @param pvtInjection TimeStampedPVCoordinates after the injection.
     * @param yaProvider Yamanaka-Ankersen relative provider.
     * @return Yamanaka-Ankersen maneuver.
     */
    public static YamanakaAnkersenManeuver computeRelativeInjectionManeuver(final TimeStampedPVCoordinates pvtBeforeInjection,
                                                                     final TimeStampedPVCoordinates pvtInjection,
                                                                     final YamanakaAnkersenProvider yaProvider) {
        final Vector3D deltaV = pvtInjection.getVelocity().subtract(pvtBeforeInjection.getVelocity());
        final DateDetector lastImpulse = new DateDetector(pvtInjection.getDate());
        return new YamanakaAnkersenManeuver(lastImpulse, deltaV, yaProvider);
    }

    /**
     * Computes the ImpulseManeuevr to inject in the Co-elliptic orbit in the desired Frame.
     * @param pvtBeforeInjection TimeStampedPVCoordinates before the injection.
     * @param pvtInjection TimeStampedPVCoordinates after the injection.
     * @param targetOrbit orbit of the target.
     * @param frame Desired frame.
     * @param Isp specific impulse of the chaser.
     * @return impulse maneuver to inject the chaser in the orbit.
     */
    public static ImpulseManeuver computeInjectionImpulseManeuver(final TimeStampedPVCoordinates pvtBeforeInjection,
                                                           final TimeStampedPVCoordinates pvtInjection,
                                                           final KeplerianOrbit targetOrbit,
                                                           final Frame frame,
                                                           final double Isp) {
        final LocalOrbitalFrame lof = new LocalOrbitalFrame(frame, LOFType.LVLH_CCSDS, targetOrbit, LOFType.LVLH_CCSDS.getName());
        final TimeStampedPVCoordinates pvtBeforeInjectionInertial = lof.getTransformTo(frame, pvtBeforeInjection.getDate()).transformPVCoordinates(pvtBeforeInjection);
        final TimeStampedPVCoordinates pvtInjectionInertial = lof.getTransformTo(frame, pvtInjection.getDate()).transformPVCoordinates(pvtInjection);
        final Vector3D deltaV = pvtInjectionInertial.getVelocity().subtract(pvtBeforeInjectionInertial.getVelocity());
        final DateDetector detector = new DateDetector(pvtInjection.getDate());
        return new ImpulseManeuver(detector, deltaV, Isp);
    }

    /**
     * Computes the Yamanaka-Ankersen injection maneuver in the Co-elliptic orbit in LVLH_CCSDS frame.
     * @param pvtBeforeInjection TimeStampedPVCoordinates before the injection.
     * @param pvtInjection TimeStampedPVCoordinates after the injection.
     * @param yaProvider Yamanaka-Ankersen relative provider.
     * @param <T> type of the field.
     * @return Yamanaka-Ankersen maneuver.
     */
    public static <T extends CalculusFieldElement<T>> FieldYamanakaAnkersenManeuver<T> computeRelativeInjectionManeuver(final TimeStampedFieldPVCoordinates<T> pvtBeforeInjection,
                                                                                                                        final TimeStampedFieldPVCoordinates<T> pvtInjection,
                                                                                                                        final FieldYamanakaAnkersenProvider<T> yaProvider) {
        final FieldVector3D<T> deltaV = pvtInjection.getVelocity().subtract(pvtBeforeInjection.getVelocity());
        final FieldDateDetector<T> lastImpulse = new FieldDateDetector<>(pvtInjection.getDate());
        return new FieldYamanakaAnkersenManeuver<>(lastImpulse, deltaV, yaProvider);
    }

    /**
     * Computes the ImpulseManeuevr to inject in the Co-elliptic orbit in the desired Frame.
     * @param pvtBeforeInjection TimeStampedPVCoordinates before the injection.
     * @param pvtInjection TimeStampedPVCoordinates after the injection.
     * @param targetOrbit orbit of the target.
     * @param frame Desired frame.
     * @param Isp specific impulse of the chaser.
     * @param <T> type of the field.
     * @return impulse maneuver to inject the chaser in the orbit.
     */
    public static <T extends CalculusFieldElement<T>> FieldImpulseManeuver<T> computeInjectionImpulseManeuver(final TimeStampedFieldPVCoordinates<T> pvtBeforeInjection,
                                                                                                              final TimeStampedFieldPVCoordinates<T> pvtInjection,
                                                                                                              final FieldKeplerianOrbit<T> targetOrbit,
                                                                                                              final Frame frame,
                                                                                                              final T Isp) {
        final LocalOrbitalFrame lof = new LocalOrbitalFrame(frame, LOFType.LVLH_CCSDS, targetOrbit.toOrbit(), LOFType.LVLH_CCSDS.getName());
        final TimeStampedFieldPVCoordinates<T> pvtBeforeInjectionInertial = lof.getTransformTo(frame, pvtBeforeInjection.getDate()).transformPVCoordinates(pvtBeforeInjection);
        final TimeStampedFieldPVCoordinates<T> pvtInjectionInertial = lof.getTransformTo(frame, pvtInjection.getDate()).transformPVCoordinates(pvtInjection);
        final FieldVector3D<T> deltaV = pvtInjectionInertial.getVelocity().subtract(pvtBeforeInjectionInertial.getVelocity());
        final FieldDateDetector<T> detector = new FieldDateDetector<>(pvtInjection.getDate());
        return new FieldImpulseManeuver<>(detector, deltaV, Isp);
    }
}

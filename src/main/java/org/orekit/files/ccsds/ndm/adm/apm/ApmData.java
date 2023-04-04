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
package org.orekit.files.ccsds.ndm.adm.apm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.complex.Quaternion;
import org.orekit.attitudes.Attitude;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.adm.AttitudeType;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.section.Data;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;

/**
 * Container for Attitude Parameter Message data.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class ApmData implements Data {

    /** General comments block. */
    private final CommentsContainer commentsBlock;

    /** Epoch of the data. */
    private final AbsoluteDate epoch;

    /** Quaternion block. */
    private final ApmQuaternion quaternionBlock;

    /** Euler angles block. */
    private final Euler eulerBlock;

    /** Angular velocity block.
     * @since 12.0
     */
    private final AngularVelocity angularVelocityBlock;

    /** Spin-stabilized block. */
    private final SpinStabilized spinStabilizedBlock;

    /** Inertia block. */
    private final Inertia inertia;

    /** Maneuvers. */
    private final List<Maneuver> maneuvers;

    /** Simple constructor.
     * @param commentsBlock general comments block
     * @param epoch epoch of the data
     * @param quaternionBlock quaternion logical block (may be null in ADM V2 or later)
     * @param eulerBlock Euler angles logicial block (may be null)
     * @param angularVelocityBlock angular velocity block (may be null)
     * @param spinStabilizedBlock spin-stabilized logical block (may be null)
     * @param inertia inertia logical block (may be null)
     */
    public ApmData(final CommentsContainer commentsBlock,
                   final AbsoluteDate epoch,
                   final ApmQuaternion quaternionBlock,
                   final Euler eulerBlock,
                   final AngularVelocity angularVelocityBlock,
                   final SpinStabilized spinStabilizedBlock,
                   final Inertia inertia) {
        this.commentsBlock        = commentsBlock;
        this.epoch                = epoch;
        this.quaternionBlock      = quaternionBlock;
        this.eulerBlock           = eulerBlock;
        this.angularVelocityBlock = angularVelocityBlock;
        this.spinStabilizedBlock  = spinStabilizedBlock;
        this.inertia              = inertia;
        this.maneuvers            = new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {

        if (version < 2.0) {
            // quaternion block is mandatory in ADM V1
            if (quaternionBlock == null) {
                // generate a dummy entry just for triggering the exception
                new ApmQuaternion().validate(version);
            }
        } else {
            // at least one logical block is mandatory in ADM V2
            if (quaternionBlock == null && eulerBlock == null && angularVelocityBlock == null &&
                spinStabilizedBlock == null && inertia == null) {
                throw new OrekitException(OrekitMessages.CCSDS_INCOMPLETE_DATA);
            }
        }

        if (quaternionBlock != null) {
            quaternionBlock.validate(version);
        }
        if (eulerBlock != null) {
            eulerBlock.validate(version);
        }
        if (angularVelocityBlock != null) {
            angularVelocityBlock.validate(version);
        }
        if (spinStabilizedBlock != null) {
            spinStabilizedBlock.validate(version);
        }
        if (inertia != null) {
            inertia.validate(version);
        }
        for (final Maneuver maneuver : maneuvers) {
            maneuver.validate(version);
        }

    }

    /** Get the comments.
     * @return comments
     */
    public List<String> getComments() {
        return commentsBlock.getComments();
    }

    /**
     * Get the epoch of the data.
     * @return epoch the epoch
     * @since 12.0
     */
    public AbsoluteDate getEpoch() {
        return epoch;
    }

    /** Get the quaternion logical block.
     * @return quaternion block
     */
    public ApmQuaternion getQuaternionBlock() {
        return quaternionBlock;
    }

    /** Get the Euler angles logical block.
     * @return Euler angles block (may be null)
     */
    public Euler getEulerBlock() {
        return eulerBlock;
    }

    /** Get the angular velocity logical block.
     * @return angular velocity block (may be null)
     * @since 12.0
     */
    public AngularVelocity getAngularVelocityBlock() {
        return angularVelocityBlock;
    }

    /** Get the spin-stabilized logical block.
     * @return spin-stabilized block (may be null)
     */
    public SpinStabilized getSpinStabilizedBlock() {
        return spinStabilizedBlock;
    }

    /** Get the inertia logical block.
     * @return inertia block (may be null)
     */
    public Inertia getInertiaBlock() {
        return inertia;
    }

    /**
     * Get the number of maneuvers present in the APM.
     * @return the number of maneuvers
     */
    public int getNbManeuvers() {
        return maneuvers.size();
    }

    /**
     * Get a list of all maneuvers.
     * @return unmodifiable list of all maneuvers.
     */
    public List<Maneuver> getManeuvers() {
        return Collections.unmodifiableList(maneuvers);
    }

    /**
     * Get a maneuver.
     * @param index maneuver index, counting from 0
     * @return maneuver
     */
    public Maneuver getManeuver(final int index) {
        return maneuvers.get(index);
    }

    /**
     * Add a maneuver.
     * @param maneuver maneuver to be set
     */
    public void addManeuver(final Maneuver maneuver) {
        maneuvers.add(maneuver);
    }

    /**
     * Get boolean testing whether the APM contains at least one maneuver.
     * @return true if APM contains at least one maneuver
     *         false otherwise
     */
    public boolean hasManeuvers() {
        return !maneuvers.isEmpty();
    }

    /** Get the attitude.
     * @param frame reference frame with respect to which attitude must be defined,
     * (may be null if attitude is <em>not</em> orbit-relative and one wants
     * attitude in the same frame as used in the attitude message)
     * @param pvProvider provider for spacecraft position and velocity
     * (may be null if attitude is <em>not</em> orbit-relative)
     * @return attitude
     * @since 12.0
     */
    public Attitude getAttitude(final Frame frame, final PVCoordinatesProvider pvProvider) {

        if (quaternionBlock != null) {
            // we have a quaternion
            final Quaternion q = quaternionBlock.getQuaternion();

            final TimeStampedAngularCoordinates tac;
            if (quaternionBlock.hasRates()) {
                // quaternion logical block includes everything we need
                final Quaternion qDot = quaternionBlock.getQuaternionDot();
                tac = AttitudeType.QUATERNION_DERIVATIVE.build(true, quaternionBlock.getEndpoints().isExternal2SpacecraftBody(),
                                                               null, true, epoch,
                                                               q.getQ0(), q.getQ1(), q.getQ2(), q.getQ3(),
                                                               qDot.getQ0(), qDot.getQ1(), qDot.getQ2(), qDot.getQ3());
            } else if (angularVelocityBlock != null) {
                // get derivatives from the angular velocity logical block
                tac = AttitudeType.QUATERNION_ANGVEL.build(true,
                                                           quaternionBlock.getEndpoints().isExternal2SpacecraftBody(),
                                                           null, true, epoch,
                                                           q.getQ0(), q.getQ1(), q.getQ2(), q.getQ3(),
                                                           angularVelocityBlock.getAngVelX(),
                                                           angularVelocityBlock.getAngVelY(),
                                                           angularVelocityBlock.getAngVelZ());
            } else if (eulerBlock != null && eulerBlock.hasRates()) {
                // get derivatives from the Euler logical block
                final double[] rates = eulerBlock.getRotationRates();
                if (eulerBlock.hasAngles()) {
                    // the Euler block has everything we need
                    final double[] angles = eulerBlock.getRotationAngles();
                    tac = AttitudeType.EULER_ANGLE_DERIVATIVE.build(true,
                                                                    eulerBlock.getEndpoints().isExternal2SpacecraftBody(),
                                                                    eulerBlock.getEulerRotSeq(), eulerBlock.isSpacecraftBodyRate(), epoch,
                                                                    angles[0], angles[1], angles[2],
                                                                    rates[0], rates[1], rates[2]);
                } else {
                    // the Euler block has only the rates (we are certainly using an ADM V1 message)
                    // we need to rebuild the rotation from the quaternion
                    tac = AttitudeType.QUATERNION_EULER_RATES.build(true,
                                                                    eulerBlock.getEndpoints().isExternal2SpacecraftBody(),
                                                                    eulerBlock.getEulerRotSeq(), eulerBlock.isSpacecraftBodyRate(), epoch,
                                                                    q.getQ0(), q.getQ1(), q.getQ2(), q.getQ3(),
                                                                    rates[0], rates[1], rates[2]);
                }

            } else {
                // we rely only on the quaternion logical block, despite it doesn't include rates
                tac = AttitudeType.QUATERNION.build(true, quaternionBlock.getEndpoints().isExternal2SpacecraftBody(),
                                                    null, true, epoch,
                                                    q.getQ0(), q.getQ1(), q.getQ2(), q.getQ3());
            }

            // build the attitude
            return quaternionBlock.getEndpoints().build(frame, pvProvider, tac);

        } else if (eulerBlock != null) {
            // we have Euler angles
            final double[] angles = eulerBlock.getRotationAngles();

            final TimeStampedAngularCoordinates tac;
            if (eulerBlock.hasRates()) {
                // the Euler block has everything we need
                final double[] rates = eulerBlock.getRotationRates();
                tac = AttitudeType.EULER_ANGLE_DERIVATIVE.build(true,
                                                                eulerBlock.getEndpoints().isExternal2SpacecraftBody(),
                                                                eulerBlock.getEulerRotSeq(), eulerBlock.isSpacecraftBodyRate(), epoch,
                                                                angles[0], angles[1], angles[2],
                                                                rates[0], rates[1], rates[2]);
            } else if (angularVelocityBlock != null) {
                // get derivatives from the angular velocity logical block
                tac = AttitudeType.EULER_ANGLE_ANGVEL.build(true,
                                                            eulerBlock.getEndpoints().isExternal2SpacecraftBody(),
                                                            eulerBlock.getEulerRotSeq(), eulerBlock.isSpacecraftBodyRate(), epoch,
                                                            angles[0], angles[1], angles[2],
                                                            angularVelocityBlock.getAngVelX(),
                                                            angularVelocityBlock.getAngVelY(),
                                                            angularVelocityBlock.getAngVelZ());
            } else {
                // we rely only on the Euler logical block, despite it doesn't include rates
                tac = AttitudeType.EULER_ANGLE.build(true,
                                                     eulerBlock.getEndpoints().isExternal2SpacecraftBody(),
                                                     eulerBlock.getEulerRotSeq(), eulerBlock.isSpacecraftBodyRate(), epoch,
                                                     angles[0], angles[1], angles[2]);
            }

            // build the attitude
            return eulerBlock.getEndpoints().build(frame, pvProvider, tac);

        } else if (spinStabilizedBlock != null) {
            // we have a spin block

            final TimeStampedAngularCoordinates tac;
            if (spinStabilizedBlock.hasNutation()) {
                // we rely only on nutation
                tac = AttitudeType.SPIN_NUTATION.build(true, true, null, true, epoch,
                                                       spinStabilizedBlock.getSpinAlpha(),
                                                       spinStabilizedBlock.getSpinDelta(),
                                                       spinStabilizedBlock.getSpinAngle(),
                                                       spinStabilizedBlock.getSpinAngleVel(),
                                                       spinStabilizedBlock.getNutation(),
                                                       spinStabilizedBlock.getNutationPeriod(),
                                                       spinStabilizedBlock.getNutationPhase());
            } else if (spinStabilizedBlock.hasMomentum()) {
                // we rely only on momentum
                tac = AttitudeType.SPIN_NUTATION_MOMENTUM.build(true, true, null, true, epoch,
                                                                spinStabilizedBlock.getSpinAlpha(),
                                                                spinStabilizedBlock.getSpinDelta(),
                                                                spinStabilizedBlock.getSpinAngle(),
                                                                spinStabilizedBlock.getSpinAngleVel(),
                                                                spinStabilizedBlock.getMomentumAlpha(),
                                                                spinStabilizedBlock.getMomentumDelta(),
                                                                spinStabilizedBlock.getNutationVel());
            } else {
                // we rely only on the spin logical block, despite it doesn't include rates
                tac = AttitudeType.SPIN.build(true, true, null, true, epoch,
                                              spinStabilizedBlock.getSpinAlpha(),
                                              spinStabilizedBlock.getSpinDelta(),
                                              spinStabilizedBlock.getSpinAngle(),
                                              spinStabilizedBlock.getSpinAngleVel());
            }

            // build the attitude
            return spinStabilizedBlock.getEndpoints().build(frame, pvProvider, tac);

        } else {
            throw new OrekitException(OrekitMessages.CCSDS_INCOMPLETE_DATA);
        }

    }

}

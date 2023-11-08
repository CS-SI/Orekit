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

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.adm.AttitudeEndpoints;
import org.orekit.files.ccsds.section.CommentsContainer;

/**
 * Container for Attitude Parameter Message data lines.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class SpinStabilized extends CommentsContainer {

    /** Endpoints (i.e. frames A, B and their relationship). */
    private final AttitudeEndpoints endpoints;

    /** Right ascension of spin axis vector (rad). */
    private double spinAlpha;

    /** Declination of the spin axis vector (rad). */
    private double spinDelta;

    /** Phase of the satellite about the spin axis (rad). */
    private double spinAngle;

    /** Angular velocity of satellite around spin axis (rad/s). */
    private double spinAngleVel;

    /** Nutation angle of spin axis (rad). */
    private double nutation;

    /** Body nutation period of the spin axis (s). */
    private double nutationPer;

    /** Inertial nutation phase (rad). */
    private double nutationPhase;

    /** Right ascension of angular momentum vector (rad).
     * @since 12.0
     */
    private double momentumAlpha;

    /** Declination of the angular momentum vector (rad).
     * @since 12.0
     */
    private double momentumDelta;

    /** Angular velocity of spin vector around the angular momentum vector (rad/s).
     * @since 12.0
     */
    private double nutationVel;

    /** Simple constructor.
     */
    public SpinStabilized() {
        endpoints      = new AttitudeEndpoints();
        spinAlpha      = Double.NaN;
        spinDelta      = Double.NaN;
        spinAngle      = Double.NaN;
        spinAngleVel   = Double.NaN;
        nutation       = Double.NaN;
        nutationPer    = Double.NaN;
        nutationPhase  = Double.NaN;
        momentumAlpha  = Double.NaN;
        momentumDelta  = Double.NaN;
        nutationVel    = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        endpoints.checkMandatoryEntriesExceptExternalFrame(version,
                                                           SpinStabilizedKey.SPIN_FRAME_A,
                                                           SpinStabilizedKey.SPIN_FRAME_B,
                                                           SpinStabilizedKey.SPIN_DIR);
        endpoints.checkExternalFrame(SpinStabilizedKey.SPIN_FRAME_A, SpinStabilizedKey.SPIN_FRAME_B);
        checkNotNaN(spinAlpha,    SpinStabilizedKey.SPIN_ALPHA.name());
        checkNotNaN(spinDelta,    SpinStabilizedKey.SPIN_DELTA.name());
        checkNotNaN(spinAngle,    SpinStabilizedKey.SPIN_ANGLE.name());
        checkNotNaN(spinAngleVel, SpinStabilizedKey.SPIN_ANGLE_VEL.name());
        if (Double.isNaN(nutation + nutationPer + nutationPhase)) {
            // if at least one is NaN, all must be NaN (i.e. not initialized)
            if (!(Double.isNaN(nutation) && Double.isNaN(nutationPer) && Double.isNaN(nutationPhase))) {
                throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, "NUTATION*");
            }
        }
        if (Double.isNaN(momentumAlpha + momentumDelta + nutationVel)) {
            // if at least one is NaN, all must be NaN (i.e. not initialized)
            if (!(Double.isNaN(momentumAlpha) && Double.isNaN(momentumDelta) && Double.isNaN(nutationVel))) {
                throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, "MOMENTUM*/NUTATION_VEL");
            }
        }
    }

    /** Get the endpoints (i.e. frames A, B and their relationship).
     * @return endpoints
     */
    public AttitudeEndpoints getEndpoints() {
        return endpoints;
    }

    /**
     * Get the right ascension of spin axis vector (rad).
     * @return the right ascension of spin axis vector
     */
    public double getSpinAlpha() {
        return spinAlpha;
    }

    /**
     * Set the right ascension of spin axis vector (rad).
     * @param spinAlpha value to be set
     */
    public void setSpinAlpha(final double spinAlpha) {
        refuseFurtherComments();
        this.spinAlpha = spinAlpha;
    }

    /**
     * Get the declination of the spin axis vector (rad).
     * @return the declination of the spin axis vector (rad).
     */
    public double getSpinDelta() {
        return spinDelta;
    }

    /**
     * Set the declination of the spin axis vector (rad).
     * @param spinDelta value to be set
     */
    public void setSpinDelta(final double spinDelta) {
        refuseFurtherComments();
        this.spinDelta = spinDelta;
    }

    /**
     * Get the phase of the satellite about the spin axis (rad).
     * @return the phase of the satellite about the spin axis
     */
    public double getSpinAngle() {
        return spinAngle;
    }

    /**
     * Set the phase of the satellite about the spin axis (rad).
     * @param spinAngle value to be set
     */
    public void setSpinAngle(final double spinAngle) {
        refuseFurtherComments();
        this.spinAngle = spinAngle;
    }

    /**
     * Get the angular velocity of satellite around spin axis (rad/s).
     * @return the angular velocity of satellite around spin axis
     */
    public double getSpinAngleVel() {
        return spinAngleVel;
    }

    /**
     * Set the angular velocity of satellite around spin axis (rad/s).
     * @param spinAngleVel value to be set
     */
    public void setSpinAngleVel(final double spinAngleVel) {
        refuseFurtherComments();
        this.spinAngleVel = spinAngleVel;
    }

    /**
     * Get the nutation angle of spin axis (rad).
     * @return the nutation angle of spin axis
     */
    public double getNutation() {
        return nutation;
    }

    /**
     * Set the nutation angle of spin axis (rad).
     * @param nutation the nutation angle to be set
     */
    public void setNutation(final double nutation) {
        refuseFurtherComments();
        this.nutation = nutation;
    }

    /**
     * Get the body nutation period of the spin axis (s).
     * @return the body nutation period of the spin axis
     */
    public double getNutationPeriod() {
        return nutationPer;
    }

    /**
     * Set the body nutation period of the spin axis (s).
     * @param period the nutation period to be set
     */
    public void setNutationPeriod(final double period) {
        refuseFurtherComments();
        this.nutationPer = period;
    }

    /**
     * Get the inertial nutation phase (rad).
     * @return the inertial nutation phase
     */
    public double getNutationPhase() {
        return nutationPhase;
    }

    /**
     * Set the inertial nutation phase (rad).
     * @param nutationPhase the nutation phase to be set
     */
    public void setNutationPhase(final double nutationPhase) {
        refuseFurtherComments();
        this.nutationPhase = nutationPhase;
    }

    /**
     * Get the right ascension of angular momentum vector (rad).
     * @return the right ascension of angular momentum vector
     * @since 12.0
     */
    public double getMomentumAlpha() {
        return momentumAlpha;
    }

    /**
     * Set the right ascension of angular momentum vector (rad).
     * @param momentumAlpha value to be set
     * @since 12.0
     */
    public void setMomentumAlpha(final double momentumAlpha) {
        refuseFurtherComments();
        this.momentumAlpha = momentumAlpha;
    }

    /**
     * Get the declination of the angular momentum vector (rad).
     * @return the declination of the angular momentum vector (rad).
     * @since 12.0
     */
    public double getMomentumDelta() {
        return momentumDelta;
    }

    /**
     * Set the declination of the angular momentum vector (rad).
     * @param momentumDelta value to be set
     * @since 12.0
     */
    public void setMomentumDelta(final double momentumDelta) {
        refuseFurtherComments();
        this.momentumDelta = momentumDelta;
    }

    /**
     * Get the angular velocity of spin vector around angular momentum vector.
     * @return angular velocity of spin vector around angular momentum vector (rad/s)
     * @since 12.0
     */
    public double getNutationVel() {
        return nutationVel;
    }

    /**
     * Set the angular velocity of spin vector around angular momentum vector.
     * @param nutationVel angular velocity of spin vector around angular momentum vector (rad/s)
     * @since 12.0
     */
    public void setNutationVel(final double nutationVel) {
        refuseFurtherComments();
        this.nutationVel = nutationVel;
    }

    /** Check if the logical block includes nutation.
     * @return true if logical block includes nutation
     * @since 12.0
     */
    public boolean hasNutation() {
        return !Double.isNaN(nutation + nutationPer + nutationPhase);
    }

    /** Check if the logical block includes momentum.
     * @return true if logical block includes momentum
     * @since 12.0
     */
    public boolean hasMomentum() {
        return !Double.isNaN(momentumAlpha + momentumDelta + nutationVel);
    }

}

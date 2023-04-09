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

package org.orekit.files.ccsds.ndm.odm.ocm;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.files.ccsds.definitions.OnOff;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/** Maneuver entry.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OrbitManeuver implements TimeStamped {

    /** Maneuver date. */
    private AbsoluteDate date;

    /** Duration. */
    private double duration;

    /** Mass change. */
    private double deltaMass;

    /** Acceleration. */
    private double[] acceleration;

    /** Interpolation mode between current and next acceleration line. */
    private OnOff accelerationInterpolation;

    /** One σ percent error on acceleration magnitude. */
    private double accelerationMagnitudeSigma;

    /** One σ angular off-nominal acceleration direction. */
    private double accelerationDirectionSigma;

    /** Velocity increment. */
    private double[] dV;

    /** One σ percent error on ΔV magnitude. */
    private double dvMagSigma;

    /** One σ angular off-nominal ΔV direction. */
    private double dvDirSigma;

    /** Thrust. */
    private double[] thrust;

    /** Thrust efficiency η typically between 0.0 and 1.0. */
    private double thrustEfficiency;

    /** Interpolation mode between current and next acceleration line. */
    private OnOff thrustInterpolation;

    /** Thrust specific impulse. */
    private double thrustIsp;

    /** One σ percent error on thrust magnitude. */
    private double thrustMagnitudeSigma;

    /** One σ angular off-nominal thrust direction. */
    private double thrustDirectionSigma;

    /** Identifier of resulting "child" object deployed from this host. */
    private String deployId;

    /** Velocity increment of deployed "child" object. */
    private double[] deployDv;

    /** Decrement in host mass as a result of deployment (shall be ≤ 0). */
    private double deployMass;

    /** One σ percent error on deployment ΔV magnitude. */
    private double deployDvSigma;

    /** One σ angular off-nominal deployment vector direction. */
    private double deployDirSigma;

    /** Ratio of child-to-host ΔV vectors. */
    private double deployDvRatio;

    /** Typical (50th percentile) product of drag coefficient times cross-sectional area of deployed "child" object. */
    private double deployDvCda;

    /** Build an uninitialized maneuver.
     */
    public OrbitManeuver() {
        acceleration = new double[3];
        dV           = new double[3];
        thrust       = new double[3];
        deployDv     = new double[3];
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    /** Set date.
     * @param date maneuver date
     */
    public void setDate(final AbsoluteDate date) {
        this.date = date;
    }

    /** Get duration.
     * @return duration
     */
    public double getDuration() {
        return duration;
    }

    /** Set duration.
     * @param duration duration
     */
    public void setDuration(final double duration) {
        this.duration = duration;
    }

    /** Get mass change.
     * @return mass change
     */
    public double getDeltaMass() {
        return deltaMass;
    }

    /** Set mass change.
     * @param deltaMass mass change
     */
    public void setDeltaMass(final double deltaMass) {
        this.deltaMass = deltaMass;
    }

    /** Get acceleration.
     * @return acceleration
     */
    public Vector3D getAcceleration() {
        return new Vector3D(acceleration);
    }

    /** Set acceleration component.
     * @param i component index
     * @param ai i<sup>th</sup> component of acceleration
     */
    public void setAcceleration(final int i, final double ai) {
        acceleration[i] = ai;
    }

    /** Get interpolation mode between current and next acceleration line.
     * @return interpolation mode between current and next acceleration line
     */
    public OnOff getAccelerationInterpolation() {
        return accelerationInterpolation;
    }

    /** Set interpolation mode between current and next acceleration line.
     * @param accelerationInterpolation interpolation mode between current and next acceleration line
     */
    public void setAccelerationInterpolation(final OnOff accelerationInterpolation) {
        this.accelerationInterpolation = accelerationInterpolation;
    }

    /** Get one σ percent error on acceleration magnitude.
     * @return one σ percent error on acceleration magnitude
     */
    public double getAccelerationMagnitudeSigma() {
        return accelerationMagnitudeSigma;
    }

    /** Set one σ percent error on acceleration magnitude.
     * @param accelerationMagnitudeSigma one σ percent error on acceleration magnitude
     */
    public void setAccelerationMagnitudeSigma(final double accelerationMagnitudeSigma) {
        this.accelerationMagnitudeSigma = accelerationMagnitudeSigma;
    }

    /** Get one σ angular off-nominal acceleration direction.
     * @return one σ angular off-nominal acceleration direction
     */
    public double getAccelerationDirectionSigma() {
        return accelerationDirectionSigma;
    }

    /** Set one σ angular off-nominal acceleration direction.
     * @param accelerationDirectionSigma one σ angular off-nominal acceleration direction
     */
    public void setAccelerationDirectionSigma(final double accelerationDirectionSigma) {
        this.accelerationDirectionSigma = accelerationDirectionSigma;
    }

    /** Get velocity increment.
     * @return velocity increment
     */
    public Vector3D getDv() {
        return new Vector3D(dV);
    }

    /** Set velocity increment component.
     * @param i component index
     * @param dVi i<sup>th</sup> component of velocity increment
     */
    public void setDv(final int i, final double dVi) {
        dV[i] = dVi;
    }

    /** Get one σ percent error on ΔV  magnitude.
     * @return one σ percent error on ΔV  magnitude
     */
    public double getDvMagSigma() {
        return dvMagSigma;
    }

    /** Set one σ percent error on ΔV  magnitude.
     * @param dvMagSigma one σ percent error on ΔV  magnitude
     */
    public void setDvMagSigma(final double dvMagSigma) {
        this.dvMagSigma = dvMagSigma;
    }

    /** Get one σ angular off-nominal ΔV direction.
     * @return one σ angular off-nominal ΔV direction
     */
    public double getDvDirSigma() {
        return dvDirSigma;
    }

    /** Set one σ angular off-nominal ΔV direction.
     * @param dvDirSigma one σ angular off-nominal ΔV direction
     */
    public void setDvDirSigma(final double dvDirSigma) {
        this.dvDirSigma = dvDirSigma;
    }

    /** Get thrust.
     * @return thrust
     */
    public Vector3D getThrust() {
        return new Vector3D(thrust);
    }

    /** Set thrust component.
     * @param i component index
     * @param ti i<sup>th</sup> component of thrust
     */
    public void setThrust(final int i, final double ti) {
        thrust[i] = ti;
    }

    /** Get thrust efficiency η.
     * @return thrust efficiency η (typically between 0.0 and 1.0)
     */
    public double getThrustEfficiency() {
        return thrustEfficiency;
    }

    /** Set thrust efficiency η.
     * @param thrustEfficiency thrust efficiency η (typically between 0.0 and 1.0)
     */
    public void setThrustEfficiency(final double thrustEfficiency) {
        this.thrustEfficiency = thrustEfficiency;
    }

    /** Get interpolation mode between current and next thrust line.
     * @return interpolation mode between current and next thrust line
     */
    public OnOff getThrustInterpolation() {
        return thrustInterpolation;
    }

    /** Set interpolation mode between current and next thrust line.
     * @param thrustInterpolation interpolation mode between current and next thrust line
     */
    public void setThrustInterpolation(final OnOff thrustInterpolation) {
        this.thrustInterpolation = thrustInterpolation;
    }

    /** Get thrust specific impulse.
     * @return thrust specific impulse
     */
    public double getThrustIsp() {
        return thrustIsp;
    }

    /** Set thrust specific impulse.
     * @param thrustIsp thrust specific impulse
     */
    public void setThrustIsp(final double thrustIsp) {
        this.thrustIsp = thrustIsp;
    }

    /** Get one σ percent error on thrust magnitude.
     * @return one σ percent error on thrust magnitude
     */
    public double getThrustMagnitudeSigma() {
        return thrustMagnitudeSigma;
    }

    /** Set one σ percent error on thrust magnitude.
     * @param thrustMagnitudeSigma one σ percent error on thrust magnitude
     */
    public void setThrustMagnitudeSigma(final double thrustMagnitudeSigma) {
        this.thrustMagnitudeSigma = thrustMagnitudeSigma;
    }

    /** Get one σ angular off-nominal thrust direction.
     * @return one σ angular off-nominal thrust direction
     */
    public double getThrustDirectionSigma() {
        return thrustDirectionSigma;
    }

    /** Set one σ angular off-nominal thrust direction.
     * @param thrustDirectionSigma one σ angular off-nominal thrust direction
     */
    public void setThrustDirectionSigma(final double thrustDirectionSigma) {
        this.thrustDirectionSigma = thrustDirectionSigma;
    }

    /** Get identifier of resulting "child" object deployed from this host.
     * @return identifier of resulting "child" object deployed from this host
     */
    public String getDeployId() {
        return deployId;
    }

    /** Set identifier of resulting "child" object deployed from this host.
     * @param deployId identifier of resulting "child" object deployed from this host
     */
    public void setDeployId(final String deployId) {
        this.deployId = deployId;
    }

    /** Get velocity increment of deployed "child" object.
     * @return velocity increment of deployed "child" object
     */
    public Vector3D getDeployDv() {
        return new Vector3D(deployDv);
    }

    /** Set velocity increment component of deployed "child" object.
     * @param i component index
     * @param deployDvi i<sup>th</sup> component of velocity increment of deployed "child" object
     */
    public void setDeployDv(final int i, final double deployDvi) {
        deployDv[i] = deployDvi;
    }

    /** Get decrement in host mass as a result of deployment.
     * @return decrement in host mass as a result of deployment (shall be ≤ 0)
     */
    public double getDeployMass() {
        return deployMass;
    }

    /** Set decrement in host mass as a result of deployment.
     * @param deployMass decrement in host mass as a result of deployment (shall be ≤ 0)
     */
    public void setDeployMass(final double deployMass) {
        this.deployMass = deployMass;
    }

    /** Get one σ percent error on deployment ΔV magnitude.
     * @return one σ percent error on deployment ΔV magnitude
     */
    public double getDeployDvSigma() {
        return deployDvSigma;
    }

    /** Set one σ percent error on deployment ΔV magnitude.
     * @param deployDvSigma one σ percent error on deployment ΔV magnitude
     */
    public void setDeployDvSigma(final double deployDvSigma) {
        this.deployDvSigma = deployDvSigma;
    }

    /** Get one σ angular off-nominal deployment vector direction.
     * @return one σ angular off-nominal deployment vector direction
     */
    public double getDeployDirSigma() {
        return deployDirSigma;
    }

    /** Set one σ angular off-nominal deployment vector direction.
     * @param deployDirSigma one σ angular off-nominal deployment vector direction
     */
    public void setDeployDirSigma(final double deployDirSigma) {
        this.deployDirSigma = deployDirSigma;
    }

    /** Get ratio of child-to-host ΔV vectors.
     * @return ratio of child-to-host ΔV vectors
     */
    public double getDeployDvRatio() {
        return deployDvRatio;
    }

    /** Set ratio of child-to-host ΔV vectors.
     * @param deployDvRatio ratio of child-to-host ΔV vectors
     */
    public void setDeployDvRatio(final double deployDvRatio) {
        this.deployDvRatio = deployDvRatio;
    }

    /** Get typical (50th percentile) product of drag coefficient times cross-sectional area of deployed "child" object.
     * @return typical (50th percentile) product of drag coefficient times cross-sectional area of deployed "child" object
     */
    public double getDeployDvCda() {
        return deployDvCda;
    }

    /** Set typical (50th percentile) product of drag coefficient times cross-sectional area of deployed "child" object.
     * @param deployDvCda typical (50th percentile) product of drag coefficient times cross-sectional area of deployed "child" object
     */
    public void setDeployDvCda(final double deployDvCda) {
        this.deployDvCda = deployDvCda;
    }

}

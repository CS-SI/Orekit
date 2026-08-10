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

package org.orekit.files.ccsds.ndm.odm.ocm;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hipparchus.linear.DefaultRealMatrixChangingVisitor;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.annotation.Nullable;
import org.orekit.files.ccsds.definitions.CcsdsFrameMapper;
import org.orekit.files.ccsds.ndm.CommonPhysicalProperties;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/** Spacecraft physical properties.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OrbitPhysicalProperties extends CommonPhysicalProperties {

    /** Satellite manufacturer name. */
    @Nullable
    private String manufacturer;

    /** Bus model name. */
    @Nullable
    private String busModel;

    /** Other space objects this object is docked to. */
    private List<String> dockedWith;

    /** Attitude-independent drag cross-sectional area, not already into attitude-dependent area along OEB. */
    @Nullable
    private Double dragConstantArea;

    /** Nominal drag coefficient. */
    @Nullable
    private Double dragCoefficient;

    /** Drag coefficient 1σ uncertainty. */
    @Nullable
    private Double dragUncertainty;

    /** Total mass at beginning of life. */
    @Nullable
    private Double initialWetMass;

    /** Total mass at T₀. */
    @Nullable
    private Double wetMass;

    /** Mass without propellant. */
    @Nullable
    private Double dryMass;

    /** Minimum cross-sectional area for collision probability estimation purposes. */
    @Nullable
    private Double minAreaForCollisionProbability;

    /** Maximum cross-sectional area for collision probability estimation purposes. */
    @Nullable
    private Double maxAreaForCollisionProbability;

    /** Typical (50th percentile) cross-sectional area for collision probability estimation purposes. */
    @Nullable
    private Double typAreaForCollisionProbability;

    /** Attitude-independent SRP area, not already into attitude-dependent area along OEB. */
    @Nullable
    private Double srpConstantArea;

    /** Nominal SRP coefficient. */
    @Nullable
    private Double srpCoefficient;

    /** SRP coefficient 1σ uncertainty. */
    @Nullable
    private Double srpUncertainty;

    /** Attitude control mode. */
    @Nullable
    private String attitudeControlMode;

    /** Type of actuator for attitude control. */
    @Nullable
    private String attitudeActuatorType;

    /** Accuracy of attitude knowledge. */
    @Nullable
    private Double attitudeKnowledgeAccuracy;

    /** Accuracy of attitude control. */
    @Nullable
    private Double attitudeControlAccuracy;

    /** Overall accuracy of spacecraft to maintain attitude. */
    @Nullable
    private Double attitudePointingAccuracy;

    /** Average average frequency of orbit or attitude maneuvers (in SI units, hence per second). */
    @Nullable
    private Double maneuversFrequency;

    /** Maximum composite thrust the spacecraft can accomplish. */
    @Nullable
    private Double maxThrust;

    /** Total ΔV capability at beginning of life. */
    @Nullable
    private Double bolDv;

    /** Total ΔV remaining for spacecraft. */
    @Nullable
    private Double remainingDv;

    /** Inertia matrix. */
    @Nullable
    private RealMatrix inertiaMatrix;

    /**
     * Simple constructor.
     *
     * @param epochT0     T0 epoch from file metadata
     * @param frameMapper for creating a {@link Frame}.
     * @since 13.1.5
     */
    public OrbitPhysicalProperties(final AbsoluteDate epochT0,
                                   final CcsdsFrameMapper frameMapper) {

        // Call to CommonPhysicalProperties constructor
        super(epochT0, frameMapper);
        // we don't call the setXxx() methods in order to avoid
        // calling refuseFurtherComments as a side effect
        dockedWith = new ArrayList<>();
    }

    /** Get manufacturer name.
     * @return manufacturer name
     */
    public Optional<String> getManufacturer() {
        return Optional.ofNullable(manufacturer);
    }

    /** Set manufacturer name.
     * @param manufacturer manufacturer name
     */
    public void setManufacturer(final String manufacturer) {
        refuseFurtherComments();
        this.manufacturer = manufacturer;
    }

    /** Get the bus model name.
     * @return bus model name
     */
    public Optional<String> getBusModel() {
        return Optional.ofNullable(busModel);
    }

    /** Set the bus model name.
     * @param busModel bus model name
     */
    public void setBusModel(final String busModel) {
        refuseFurtherComments();
        this.busModel = busModel;
    }

    /** Get the other space objects this object is docked to.
     * @return the oother space objects this object is docked to
     */
    public List<String> getDockedWith() {
        return dockedWith;
    }

    /** Set the other space objects this object is docked to.
     * @param dockedWith the other space objects this object is docked to
     */
    public void setDockedWith(final List<String> dockedWith) {
        refuseFurtherComments();
        this.dockedWith = dockedWith;
    }

    /** Get the attitude-independent drag cross-sectional area, not already into attitude-dependent area along OEB.
     * @return attitude-independent drag cross-sectional area, not already into attitude-dependent area along OEB
     */
    public Optional<Double> getDragConstantArea() {
        return Optional.ofNullable(dragConstantArea);
    }

    /** Set the attitude-independent drag cross-sectional area, not already into attitude-dependent area along OEB.
     * @param dragConstantArea attitude-independent drag cross-sectional area, not already into attitude-dependent area along OEB
     */
    public void setDragConstantArea(final double dragConstantArea) {
        refuseFurtherComments();
        this.dragConstantArea = dragConstantArea;
    }

    /** Get the nominal drag coefficient.
     * @return the nominal drag coefficient
     */
    public Optional<Double> getDragCoefficient() {
        return Optional.ofNullable(dragCoefficient);
    }

    /** Set the the nominal drag coefficient.
     * @param dragCoefficient the nominal drag coefficient
     */
    public void setDragCoefficient(final double dragCoefficient) {
        refuseFurtherComments();
        this.dragCoefficient = dragCoefficient;
    }

    /** Get the drag coefficient 1σ uncertainty.
     * @return drag coefficient 1σ uncertainty (in %)
     */
    public Optional<Double> getDragUncertainty() {
        return Optional.ofNullable(dragUncertainty);
    }

    /** Set the drag coefficient 1σ uncertainty.
     * @param dragUncertainty drag coefficient 1σ uncertainty (in %)
     */
    public void setDragUncertainty(final double dragUncertainty) {
        refuseFurtherComments();
        this.dragUncertainty = dragUncertainty;
    }

    /** Get the total mass at beginning of life.
     * @return total mass at beginning of life
     */
    public Optional<Double> getInitialWetMass() {
        return Optional.ofNullable(initialWetMass);
    }

    /** Set the total mass at beginning of life.
     * @param initialWetMass total mass at beginning of life
     */
    public void setInitialWetMass(final double initialWetMass) {
        refuseFurtherComments();
        this.initialWetMass = initialWetMass;
    }

    /** Get the total mass at T₀.
     * @return total mass at T₀
     */
    public Optional<Double> getWetMass() {
        return Optional.ofNullable(wetMass);
    }

    /** Set the total mass at T₀.
     * @param wetMass total mass at T₀
     */
    public void setWetMass(final double wetMass) {
        refuseFurtherComments();
        this.wetMass = wetMass;
    }

    /** Get the mass without propellant.
     * @return mass without propellant
     */
    public Optional<Double> getDryMass() {
        return Optional.ofNullable(dryMass);
    }

    /** Set the mass without propellant.
     * @param dryMass mass without propellant
     */
    public void setDryMass(final double dryMass) {
        refuseFurtherComments();
        this.dryMass = dryMass;
    }

    /** Get the minimum cross-sectional area for collision probability estimation purposes.
     * @return minimum cross-sectional area for collision probability estimation purposes
     */
    public Optional<Double> getMinAreaForCollisionProbability() {
        return Optional.ofNullable(minAreaForCollisionProbability);
    }

    /** Set the minimum cross-sectional area for collision probability estimation purposes.
     * @param minAreaForCollisionProbability minimum cross-sectional area for collision probability estimation purposes
     */
    public void setMinAreaForCollisionProbability(final double minAreaForCollisionProbability) {
        refuseFurtherComments();
        this.minAreaForCollisionProbability = minAreaForCollisionProbability;
    }

    /** Get the maximum cross-sectional area for collision probability estimation purposes.
     * @return maximum cross-sectional area for collision probability estimation purposes
     */
    public Optional<Double> getMaxAreaForCollisionProbability() {
        return Optional.ofNullable(maxAreaForCollisionProbability);
    }

    /** Set the maximum cross-sectional area for collision probability estimation purposes.
     * @param maxAreaForCollisionProbability maximum cross-sectional area for collision probability estimation purposes
     */
    public void setMaxAreaForCollisionProbability(final double maxAreaForCollisionProbability) {
        refuseFurtherComments();
        this.maxAreaForCollisionProbability = maxAreaForCollisionProbability;
    }

    /** Get the typical (50th percentile) cross-sectional area for collision probability estimation purposes.
     * @return typical (50th percentile) cross-sectional area for collision probability estimation purposes
     */
    public Optional<Double> getTypAreaForCollisionProbability() {
        return Optional.ofNullable(typAreaForCollisionProbability);
    }

    /** Get the typical (50th percentile) cross-sectional area for collision probability estimation purposes.
     * @param typAreaForCollisionProbability typical (50th percentile) cross-sectional area for collision probability estimation purposes
     */
    public void setTypAreaForCollisionProbability(final double typAreaForCollisionProbability) {
        refuseFurtherComments();
        this.typAreaForCollisionProbability = typAreaForCollisionProbability;
    }

    /** Get the attitude-independent SRP area, not already into attitude-dependent area along OEB.
     * @return attitude-independent SRP area, not already into attitude-dependent area along OEB
     */
    public Optional<Double> getSrpConstantArea() {
        return Optional.ofNullable(srpConstantArea);
    }

    /** Set the attitude-independent SRP area, not already into attitude-dependent area along OEB.
     * @param srpConstantArea attitude-independent SRP area, not already into attitude-dependent area along OEB
     */
    public void setSrpConstantArea(final double srpConstantArea) {
        refuseFurtherComments();
        this.srpConstantArea = srpConstantArea;
    }

    /** Get the nominal SRP coefficient.
     * @return nominal SRP coefficient
     */
    public Optional<Double> getSrpCoefficient() {
        return Optional.ofNullable(srpCoefficient);
    }

    /** Set the nominal SRP coefficient.
     * @param srpCoefficient nominal SRP coefficient
     */
    public void setSrpCoefficient(final double srpCoefficient) {
        refuseFurtherComments();
        this.srpCoefficient = srpCoefficient;
    }

    /** Get the SRP coefficient 1σ uncertainty.
     * @return SRP coefficient 1σ uncertainty
     */
    public Optional<Double> getSrpUncertainty() {
        return Optional.ofNullable(srpUncertainty);
    }

    /** Set the SRP coefficient 1σ uncertainty.
     * @param srpUncertainty SRP coefficient 1σ uncertainty.
     */
    public void setSrpUncertainty(final double srpUncertainty) {
        refuseFurtherComments();
        this.srpUncertainty = srpUncertainty;
    }

    /** Get the attitude control mode.
     * @return attitude control mode
     */
    public Optional<String> getAttitudeControlMode() {
        return Optional.ofNullable(attitudeControlMode);
    }

    /** Set the attitude control mode.
     * @param attitudeControlMode attitude control mode
     */
    public void setAttitudeControlMode(final String attitudeControlMode) {
        refuseFurtherComments();
        this.attitudeControlMode = attitudeControlMode;
    }

    /** Get the type of actuator for attitude control.
     * @return type of actuator for attitude control
     */
    public Optional<String> getAttitudeActuatorType() {
        return Optional.ofNullable(attitudeActuatorType);
    }

    /** Set the type of actuator for attitude control.
     * @param attitudeActuatorType type of actuator for attitude control
     */
    public void setAttitudeActuatorType(final String attitudeActuatorType) {
        refuseFurtherComments();
        this.attitudeActuatorType = attitudeActuatorType;
    }

    /** Get the accuracy of attitude knowledge.
     * @return accuracy of attitude knowledge
     */
    public Optional<Double> getAttitudeKnowledgeAccuracy() {
        return Optional.ofNullable(attitudeKnowledgeAccuracy);
    }

    /** Set the accuracy of attitude knowledge.
     * @param attitudeKnowledgeAccuracy accuracy of attitude knowledge
     */
    public void setAttitudeKnowledgeAccuracy(final double attitudeKnowledgeAccuracy) {
        refuseFurtherComments();
        this.attitudeKnowledgeAccuracy = attitudeKnowledgeAccuracy;
    }

    /** Get the accuracy of attitude control.
     * @return accuracy of attitude control
     */
    public Optional<Double> getAttitudeControlAccuracy() {
        return Optional.ofNullable(attitudeControlAccuracy);
    }

    /** Set the accuracy of attitude control.
     * @param attitudeControlAccuracy accuracy of attitude control
     */
    public void setAttitudeControlAccuracy(final double attitudeControlAccuracy) {
        refuseFurtherComments();
        this.attitudeControlAccuracy = attitudeControlAccuracy;
    }

    /** Get the overall accuracy of spacecraft to maintain attitude.
     * @return overall accuracy of spacecraft to maintain attitude
     */
    public Optional<Double> getAttitudePointingAccuracy() {
        return Optional.ofNullable(attitudePointingAccuracy);
    }

    /** Set the overall accuracy of spacecraft to maintain attitude.
     * @param attitudePointingAccuracy overall accuracy of spacecraft to maintain attitude
     */
    public void setAttitudePointingAccuracy(final double attitudePointingAccuracy) {
        refuseFurtherComments();
        this.attitudePointingAccuracy = attitudePointingAccuracy;
    }

    /** Get the average number of orbit or attitude maneuvers per year.
     * @return average number of orbit or attitude maneuvers per year.
     */
    public Optional<Double> getManeuversPerYear() {
        return maneuversFrequency != null ? Optional.of(maneuversFrequency * Constants.JULIAN_YEAR) : Optional.empty();
    }

    /** Get the average frequency of orbit or attitude maneuvers (in SI units, hence per second).
     * @return average frequency of orbit or attitude maneuvers (in SI units, hence per second).
     */
    public Optional<Double> getManeuversFrequency() {
        return Optional.ofNullable(maneuversFrequency);
    }

    /** Set the average frequency of orbit or attitude maneuvers (in SI units, hence per second).
     * @param maneuversFrequency average frequency of orbit or attitude (in SI units, hence per second).
     */
    public void setManeuversFrequency(final double maneuversFrequency) {
        refuseFurtherComments();
        this.maneuversFrequency = maneuversFrequency;
    }

    /** Get the maximum composite thrust the spacecraft can accomplish.
     * @return maximum composite thrust the spacecraft can accomplish
     */
    public Optional<Double> getMaxThrust() {
        return Optional.ofNullable(maxThrust);
    }

    /** Set the maximum composite thrust the spacecraft can accomplish.
     * @param maxThrust maximum composite thrust the spacecraft can accomplish
     */
    public void setMaxThrust(final double maxThrust) {
        refuseFurtherComments();
        this.maxThrust = maxThrust;
    }

    /** Get the total ΔV capability at beginning of life.
     * @return total ΔV capability at beginning of life
     */
    public Optional<Double> getBolDv() {
        return Optional.ofNullable(bolDv);
    }

    /** Set the total ΔV capability at beginning of life.
     * @param bolDv total ΔV capability at beginning of life
     */
    public void setBolDv(final double bolDv) {
        refuseFurtherComments();
        this.bolDv = bolDv;
    }

    /** Get the total ΔV remaining for spacecraft.
     * @return total ΔV remaining for spacecraft
     */
    public Optional<Double> getRemainingDv() {
        return Optional.ofNullable(remainingDv);
    }

    /** Set the total ΔV remaining for spacecraft.
     * @param remainingDv total ΔV remaining for spacecraft
     */
    public void setRemainingDv(final double remainingDv) {
        refuseFurtherComments();
        this.remainingDv = remainingDv;
    }

    /** Get the inertia matrix.
     * @return the inertia matrix
     */
    public Optional<RealMatrix> getInertiaMatrix() {
        return Optional.ofNullable(inertiaMatrix);
    }

    /** Set an entry in the inertia matrix.
     * <p>
     * Both I(j, k) and I(k, j) are set.
     * </p>
     * @param j row index (must be between 0 and 3 (inclusive)
     * @param k column index (must be between 0 and 3 (inclusive)
     * @param entry value of the matrix entry
     */
    public void setInertiaMatrixEntry(final int j, final int k, final double entry) {
        refuseFurtherComments();
        if (inertiaMatrix == null) {
            inertiaMatrix = MatrixUtils.createRealMatrix(3, 3);
            // set all values to NaN
            inertiaMatrix.walkInOptimizedOrder(new DefaultRealMatrixChangingVisitor() {
                @Override
                public double visit(final int i, final int j, final double v) {
                    return Double.NaN;
                }
            });
        }
        inertiaMatrix.setEntry(j, k, entry);
        inertiaMatrix.setEntry(k, j, entry);
    }

}

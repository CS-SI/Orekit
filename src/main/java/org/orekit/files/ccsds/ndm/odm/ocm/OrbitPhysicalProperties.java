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

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.files.ccsds.ndm.CommonPhysicalProperties;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/** Spacecraft physical properties.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OrbitPhysicalProperties extends CommonPhysicalProperties {

    /** Satellite manufacturer name. */
    private String manufacturer;

    /** Bus model name. */
    private String busModel;

    /** Other space objects this object is docked to. */
    private List<String> dockedWith;

    /** Attitude-independent drag cross-sectional area, not already into attitude-dependent area along OEB. */
    private double dragConstantArea;

    /** Nominal drag coefficient. */
    private double dragCoefficient;

    /** Drag coefficient 1σ uncertainty. */
    private double dragUncertainty;

    /** Total mass at beginning of life. */
    private double initialWetMass;

    /** Total mass at T₀. */
    private double wetMass;

    /** Mass without propellant. */
    private double dryMass;

    /** Minimum cross-sectional area for collision probability estimation purposes. */
    private double minAreaForCollisionProbability;

    /** Maximum cross-sectional area for collision probability estimation purposes. */
    private double maxAreaForCollisionProbability;

    /** Typical (50th percentile) cross-sectional area for collision probability estimation purposes. */
    private double typAreaForCollisionProbability;

    /** Attitude-independent SRP area, not already into attitude-dependent area along OEB. */
    private double srpConstantArea;

    /** Nominal SRP coefficient. */
    private double srpCoefficient;

    /** SRP coefficient 1σ uncertainty. */
    private double srpUncertainty;

    /** Attitude control mode. */
    private String attitudeControlMode;

    /** Type of actuator for attitude control. */
    private String attitudeActuatorType;

    /** Accuracy of attitude knowledge. */
    private double attitudeKnowledgeAccuracy;

    /** Accuracy of attitude control. */
    private double attitudeControlAccuracy;

    /** Overall accuracy of spacecraft to maintain attitude. */
    private double attitudePointingAccuracy;

    /** Average average frequency of orbit or attitude maneuvers (in SI units, hence per second). */
    private double maneuversFrequency;

    /** Maximum composite thrust the spacecraft can accomplish. */
    private double maxThrust;

    /** Total ΔV capability at beginning of life. */
    private double bolDv;

    /** Total ΔV remaining for spacecraft. */
    private double remainingDv;

    /** Inertia matrix. */
    private RealMatrix inertiaMatrix;

    /** Simple constructor.
     * @param epochT0 T0 epoch from file metadata
     */
    public OrbitPhysicalProperties(final AbsoluteDate epochT0) {

        // Call to CommonPhysicalProperties constructor
        super();

        // we don't call the setXxx() methods in order to avoid
        // calling refuseFurtherComments as a side effect
        dockedWith                     = new ArrayList<>();
        dragConstantArea               = Double.NaN;
        dragCoefficient                = Double.NaN;
        dragUncertainty                = 0.0;
        initialWetMass                 = Double.NaN;
        wetMass                        = Double.NaN;
        dryMass                        = Double.NaN;
        minAreaForCollisionProbability = Double.NaN;
        maxAreaForCollisionProbability = Double.NaN;
        typAreaForCollisionProbability = Double.NaN;
        attitudeKnowledgeAccuracy      = Double.NaN;
        attitudeControlAccuracy        = Double.NaN;
        attitudePointingAccuracy       = Double.NaN;
        maneuversFrequency             = Double.NaN;
        maxThrust                      = Double.NaN;
        bolDv                          = Double.NaN;
        remainingDv                    = Double.NaN;
        inertiaMatrix                  = MatrixUtils.createRealMatrix(3, 3);
    }

    /** Get manufacturer name.
     * @return manufacturer name
     */
    public String getManufacturer() {
        return manufacturer;
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
    public String getBusModel() {
        return busModel;
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
    public double getDragConstantArea() {
        return dragConstantArea;
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
    public double getDragCoefficient() {
        return dragCoefficient;
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
    public double getDragUncertainty() {
        return dragUncertainty;
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
    public double getInitialWetMass() {
        return initialWetMass;
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
    public double getWetMass() {
        return wetMass;
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
    public double getDryMass() {
        return dryMass;
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
    public double getMinAreaForCollisionProbability() {
        return minAreaForCollisionProbability;
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
    public double getMaxAreaForCollisionProbability() {
        return maxAreaForCollisionProbability;
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
    public double getTypAreaForCollisionProbability() {
        return typAreaForCollisionProbability;
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
    public double getSrpConstantArea() {
        return srpConstantArea;
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
    public double getSrpCoefficient() {
        return srpCoefficient;
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
    public double getSrpUncertainty() {
        return srpUncertainty;
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
    public String getAttitudeControlMode() {
        return attitudeControlMode;
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
    public String getAttitudeActuatorType() {
        return attitudeActuatorType;
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
    public double getAttitudeKnowledgeAccuracy() {
        return attitudeKnowledgeAccuracy;
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
    public double getAttitudeControlAccuracy() {
        return attitudeControlAccuracy;
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
    public double getAttitudePointingAccuracy() {
        return attitudePointingAccuracy;
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
    public double getManeuversPerYear() {
        return maneuversFrequency * Constants.JULIAN_YEAR;
    }

    /** Get the average frequency of orbit or attitude maneuvers (in SI units, hence per second).
     * @return average frequency of orbit or attitude maneuvers (in SI units, hence per second).
     */
    public double getManeuversFrequency() {
        return maneuversFrequency;
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
    public double getMaxThrust() {
        return maxThrust;
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
    public double getBolDv() {
        return bolDv;
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
    public double getRemainingDv() {
        return remainingDv;
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
    public RealMatrix getInertiaMatrix() {
        return inertiaMatrix;
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
        inertiaMatrix.setEntry(j, k, entry);
        inertiaMatrix.setEntry(k, j, entry);
    }

}

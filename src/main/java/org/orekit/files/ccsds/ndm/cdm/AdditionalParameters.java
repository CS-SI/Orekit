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
package org.orekit.files.ccsds.ndm.cdm;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.CommonPhysicalProperties;

/**
 * Container for additional parameters data block.
 * @author Melina Vanel
 * @since 11.2
 */
public class AdditionalParameters extends CommonPhysicalProperties {

    /** The actual area of the object. */
    private double areaPC;

    /** The minimum area of the object to be used to compute the collision probability. */
    private double areaPCMin;

    /** The maximum area of the object to be used to compute the collision probability. */
    private double areaPCMax;

    /** The effective area of the object exposed to atmospheric drag. */
    private double areaDRG;

    /** The effective area of the object exposed to solar radiation pressure. */
    private double areaSRP;

    /** The object hard body radius. */
    private double hbr;

    /** The mass of the object. */
    private double mass;

    /** The object’s Cd x A/m used to propagate the state vector and covariance to TCA. */
    private double cdAreaOverMass;

    /** The object’s Cr x A/m used to propagate the state vector and covariance to TCA. */
    private double crAreaOverMass;

    /** The object’s acceleration due to in-track thrust used to propagate the state vector and covariance to TCA. */
    private double thrustAcceleration;

    /** The amount of energy being removed from the object’s orbit by atmospheric drag. This value is an average calculated during the OD. */
    private double sedr;

    /** The distance of the furthest point in the objects orbit above the equatorial radius of the central body. */
    private double apoapsisAltitude;

    /** The distance of the closest point in the objects orbit above the equatorial radius of the central body . */
    private double periapsisAltitude;

    /** The angle between the objects orbit plane and the orbit centers equatorial plane. */
    private double inclination;

    /** A measure of the confidence in the covariance errors matching reality. */
    private double covConfidence;

    /** The method used for the calculation of COV_CONFIDENCE. */
    private String covConfidenceMethod;

    /** Simple constructor.
     */
    public AdditionalParameters() {

       // Call to CommonPhysicalProperties constructor
        super();

        // we don't call the setXxx() methods in order to avoid
        // calling refuseFurtherComments as a side effect
        areaPC              = Double.NaN;
        areaDRG             = Double.NaN;
        areaSRP             = Double.NaN;
        mass                = Double.NaN;
        cdAreaOverMass      = Double.NaN;
        crAreaOverMass      = Double.NaN;
        thrustAcceleration  = Double.NaN;
        sedr                = Double.NaN;
        hbr                 = Double.NaN;
        apoapsisAltitude  = Double.NaN;
        periapsisAltitude = Double.NaN;
        inclination       = Double.NaN;
        covConfidence       = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
    }

    /**
     * Get the actual area of the object.
     * @return the object area (in m²)
     */
    public double getAreaPC() {
        return areaPC;
    }

    /**
     * Set the actual area of the object.
     * @param areaPC area  (in m²) value to be set
     */
    public void setAreaPC(final double areaPC) {
        refuseFurtherComments();
        this.areaPC = areaPC;
    }

    /**
     * Get the effective area of the object exposed to atmospheric drag.
     * @return the object area (in m²) exposed to atmospheric drag
     */
    public double getAreaDRG() {
        return areaDRG;
    }

    /**
     * Set the effective area of the object exposed to atmospheric drag.
     * @param areaDRG area (in m²) value to be set
     */
    public void setAreaDRG(final double areaDRG) {
        refuseFurtherComments();
        this.areaDRG = areaDRG;
    }

    /**
     * Get the effective area of the object exposed to solar radiation pressure.
     * @return the object area (in m²) exposed to solar radiation pressure
     */
    public double getAreaSRP() {
        return areaSRP;
    }

    /**
     * Set the effective area of the object exposed to solar radiation pressure.
     * @param areaSRP area (in m²) to be set
     */
    public void setAreaSRP(final double areaSRP) {
        refuseFurtherComments();
        this.areaSRP = areaSRP;
    }

    /**
     * Get the mass of the object.
     * @return the mass (in kg) of the object
     */
    public double getMass() {
        return mass;
    }

    /**
     * Set the mass of the object.
     * @param mass mass (in kg) of the object to be set
     */
    public void setMass(final double mass) {
        refuseFurtherComments();
        this.mass = mass;
    }

    /**
     * Get the object’s Cd x A/m used to propagate the state vector and covariance to TCA.
     * @return the object’s Cd x A/m (in m²/kg)
     */
    public double getCDAreaOverMass() {
        return cdAreaOverMass;
    }

    /**
     * Set the object’s Cd x A/m used to propagate the state vector and covariance to TCA.
     * @param CDAreaOverMass object’s Cd x A/m (in m²/kg) value to be set
     */
    public void setCDAreaOverMass(final double CDAreaOverMass) {
        refuseFurtherComments();
        this.cdAreaOverMass = CDAreaOverMass;
    }

    /**
     * Get the object’s Cr x A/m used to propagate the state vector and covariance to TCA.
     * @return the object’s Cr x A/m (in m²/kg)
     */
    public double getCRAreaOverMass() {
        return crAreaOverMass;
    }

    /**
     * Set the object’s Cr x A/m used to propagate the state vector and covariance to TCA.
     * @param CRAreaOverMass object’s Cr x A/m (in m²/kg) value to be set
     */
    public void setCRAreaOverMass(final double CRAreaOverMass) {
        refuseFurtherComments();
        this.crAreaOverMass = CRAreaOverMass;
    }

    /**
     * Get the object’s acceleration due to in-track thrust used to propagate the state vector and covariance to TCA.
     * @return the object’s acceleration (in m/s²) due to in-track thrust
     */
    public double getThrustAcceleration() {
        return thrustAcceleration;
    }

    /**
     * Set the object’s acceleration due to in-track thrust used to propagate the state vector and covariance to TCA.
     * @param thrustAcceleration object’s acceleration (in m/s²) due to in-track thrust
     */
    public void setThrustAcceleration(final double thrustAcceleration) {
        refuseFurtherComments();
        this.thrustAcceleration = thrustAcceleration;
    }

    /**
     * Get the amount of energy being removed from the object’s orbit by atmospheric drag. This value is an average
     * calculated during the OD. SEDR = Specific Energy Dissipation Rate.
     * @return the amount of energy (in W/kg) being removed from the object’s orbit by atmospheric drag
     */
    public double getSedr() {
        return sedr;
    }

    /**
     * Set the amount of energy being removed from the object’s orbit by atmospheric drag. This value is an average
     * calculated during the OD. SEDR = Specific Energy Dissipation Rate.
     * @param SEDR amount of energy (in W/kg) being removed from the object’s orbit by atmospheric drag
     */
    public void setSedr(final double SEDR) {
        refuseFurtherComments();
        this.sedr = SEDR;
    }

    /** Set the minimum area of the object to be used to compute the collision probability.
     * @return the areaPCMin
     */
    public double getAreaPCMin() {
        return areaPCMin;
    }

    /** Get the minimum area of the object to be used to compute the collision probability.
     * @param areaPCMin the areaPCMin to set
     */
    public void setAreaPCMin(final double areaPCMin) {
        this.areaPCMin = areaPCMin;
    }

    /** Get the maximum area of the object to be used to compute the collision probability.
     * @return the areaPCMax
     */
    public double getAreaPCMax() {
        return areaPCMax;
    }

    /** Set the maximum area for the object to be used to compute the collision probability.
     * @param areaPCMax the areaPCMax to set
     */
    public void setAreaPCMax(final double areaPCMax) {
        this.areaPCMax = areaPCMax;
    }

     /** Get the object hard body radius.
     * @return the object hard body radius.
     */
    public double getHbr() {
        return hbr;
    }

    /** Set the object hard body radius.
     * @param hbr the object hard body radius.
     */
    public void setHbr(final double hbr) {
        refuseFurtherComments();
        this.hbr = hbr;
    }

    /** Get the distance of the furthest point in the objects orbit above the equatorial radius of the central body.
     * @return the apoapsisAltitude
     */
    public double getApoapsisAltitude() {
        return apoapsisAltitude;
    }

    /** Set the distance of the furthest point in the objects orbit above the equatorial radius of the central body.
     * @param apoapsisAltitude the apoapsisHeight to set
     */
    public void setApoapsisAltitude(final double apoapsisAltitude) {
        refuseFurtherComments();
        this.apoapsisAltitude = apoapsisAltitude;
    }

    /** Get the distance of the closest point in the objects orbit above the equatorial radius of the central body.
     * @return the periapsissAltitude
     */
    public double getPeriapsisAltitude() {
        return periapsisAltitude;
    }

    /** Set the distance of the closest point in the objects orbit above the equatorial radius of the central body.
     * @param periapsisAltitude the periapsissHeight to set
     */
    public void setPeriapsisAltitude(final double periapsisAltitude) {
        refuseFurtherComments();
        this.periapsisAltitude = periapsisAltitude;
    }

    /** Get the angle between the objects orbit plane and the orbit centers equatorial plane.
     * @return the inclination
     */
    public double getInclination() {
        return inclination;
    }

    /** Set the angle between the objects orbit plane and the orbit centers equatorial plane.
     * @param inclination the inclination to set
     */
    public void setInclination(final double inclination) {
        refuseFurtherComments();
        this.inclination = inclination;
    }

    /** Get the measure of the confidence in the covariance errors matching reality.
     * @return the covConfidence
     */
    public double getCovConfidence() {
        return covConfidence;
    }

    /** Set the measure of the confidence in the covariance errors matching reality.
     * @param covConfidence the covConfidence to set
     */
    public void setCovConfidence(final double covConfidence) {
        refuseFurtherComments();
        this.covConfidence = covConfidence;
    }

    /** Get the method used for the calculation of COV_CONFIDENCE.
     * @return the covConfidenceMethod
     */
    public String getCovConfidenceMethod() {
        return covConfidenceMethod;
    }

    /** Set the method used for the calculation of COV_CONFIDENCE.
     * @param covConfidenceMethod the covConfidenceMethod to set
     */
    public void setCovConfidenceMethod(final String covConfidenceMethod) {
        refuseFurtherComments();

        // Check key condition
        if (Double.isNaN(getCovConfidence())) {
            throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD, AdditionalParametersKey.COV_CONFIDENCE);
        }

        this.covConfidenceMethod = covConfidenceMethod;
    }
}

/* Copyright 2002-2022 CS GROUP
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

import org.hipparchus.complex.Quaternion;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.time.AbsoluteDate;

/**
 * Container for additional parameters data block.
 * @author Melina Vanel
 * @since 11.2
 */
public class AdditionalParameters extends CommentsContainer {

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

    /** Optimally Enclosing Box parent reference frame. */
    private FrameFacade oebParentFrame;

    /** Optimally Enclosing Box parent reference frame epoch. */
    private AbsoluteDate oebParentFrameEpoch;

    /** Quaternion defining Optimally Enclosing Box. */
    private final double[] oebQ;

    /** Maximum physical dimension of Optimally Enclosing Box. */
    private double oebMax;

    /** Intermediate physical dimension of Optimally Enclosing Box. */
    private double oebInt;

    /** Minimum physical dimension of Optimally Enclosing Box. */
    private double oebMin;

    /** Cross-sectional area of Optimally Enclosing Box when viewed along the maximum OEB direction. */
    private double oebAreaAlongMax;

    /** Cross-sectional area of Optimally Enclosing Box when viewed along the intermediate OEB direction. */
    private double oebAreaAlongInt;

    /** Cross-sectional area of Optimally Enclosing Box when viewed along the minimum OEB direction. */
    private double oebAreaAlongMin;

        /** Typical (50th percentile) radar cross-section. */
    private double rcs;

    /** Minimum radar cross-section. */
    private double minRcs;

    /** Maximum radar cross-section. */
    private double maxRcs;

    /** Typical (50th percentile) visual magnitude. */
    private double vmAbsolute;

    /** Minimum apparent visual magnitude. */
    private double vmApparentMin;

    /** Typical (50th percentile) apparent visual magnitude. */
    private double vmApparent;

    /** Maximum apparent visual magnitude. */
    private double vmApparentMax;

    /** Typical (50th percentile) coefficient of reflectivity. */
    private double reflectance;

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
    private double periapsissAltitude;

    /** The angle between the objects orbit plane and the orbit centers equatorial plane. */
    private double inclination;

    /** A measure of the confidence in the covariance errors matching reality. */
    private double covConfidence;

    /** The method used for the calculation of COV_CONFIDENCE. */
    private String covConfidenceMethod;

    /** Simple constructor.
     */
    public AdditionalParameters() {

        areaPC              = Double.NaN;
        areaDRG             = Double.NaN;
        areaSRP             = Double.NaN;
        mass                = Double.NaN;
        cdAreaOverMass      = Double.NaN;
        crAreaOverMass      = Double.NaN;
        thrustAcceleration  = Double.NaN;
        sedr                = Double.NaN;

        oebParentFrameEpoch = AbsoluteDate.ARBITRARY_EPOCH;
        oebQ                = new double[4];
        oebMax              = Double.NaN;
        oebInt              = Double.NaN;
        oebMin              = Double.NaN;
        oebAreaAlongMax     = Double.NaN;
        oebAreaAlongInt     = Double.NaN;
        oebAreaAlongMin     = Double.NaN;
        rcs                 = Double.NaN;
        minRcs              = Double.NaN;
        maxRcs              = Double.NaN;
        vmAbsolute          = Double.NaN;
        vmApparentMin       = Double.NaN;
        vmApparent          = Double.NaN;
        vmApparentMax       = Double.NaN;
        reflectance        = Double.NaN;
        hbr                 = Double.NaN;
        apoapsisAltitude      = Double.NaN;
        periapsissAltitude    = Double.NaN;
        inclination         = Double.NaN;
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

    /** Get the Optimally Enclosing Box parent reference frame.
     * @return Optimally Enclosing Box parent reference frame
     */
    public FrameFacade getOebParentFrame() {
        return oebParentFrame;
    }

    /** Set the Optimally Enclosing Box parent reference frame.
     * @param oebParentFrame Optimally Enclosing Box parent reference frame
     */
    public void setOebParentFrame(final FrameFacade oebParentFrame) {
        refuseFurtherComments();
        this.oebParentFrame = oebParentFrame;
    }

    /** Get the Optimally Enclosing Box parent reference frame epoch.
     * @return Optimally Enclosing Box parent reference frame epoch
     */
    public AbsoluteDate getOebParentFrameEpoch() {
        return oebParentFrameEpoch;
    }

    /** Set the Optimally Enclosing Box parent reference frame epoch.
     * @param oebParentFrameEpoch Optimally Enclosing Box parent reference frame epoch
     */
    public void setOebParentFrameEpoch(final AbsoluteDate oebParentFrameEpoch) {
        refuseFurtherComments();
        this.oebParentFrameEpoch = oebParentFrameEpoch;
    }

    /** Get the quaternion defining Optimally Enclosing Box.
     * @return quaternion defining Optimally Enclosing Box
     */
    public Quaternion getOebQ() {
        return new Quaternion(oebQ[0], oebQ[1], oebQ[2], oebQ[3]);
    }

    /** set the component of quaternion defining Optimally Enclosing Box.
     * @param i index of the component
     * @param qI component of quaternion defining Optimally Enclosing Box
     */
    public void setOebQ(final int i, final double qI) {
        refuseFurtherComments();
        oebQ[i] = qI;
    }

    /** Get the maximum physical dimension of the OEB.
     * @return maximum physical dimension of the OEB.
     */
    public double getOebMax() {
        return oebMax;
    }

    /** Set the maximum physical dimension of the OEB.
     * @param oebMax maximum physical dimension of the OEB.
     */
    public void setOebMax(final double oebMax) {
        refuseFurtherComments();
        this.oebMax = oebMax;
    }

    /** Get the intermediate physical dimension of the OEB.
     * @return intermediate physical dimension of the OEB.
     */
    public double getOebInt() {
        return oebInt;
    }

    /** Set the intermediate physical dimension of the OEB.
     * @param oebInt intermediate physical dimension of the OEB.
     */
    public void setOebInt(final double oebInt) {
        refuseFurtherComments();
        this.oebInt = oebInt;
    }

    /** Get the minimum physical dimension of the OEB.
     * @return dimensions the minimum physical dimension of the OEB.
     */
    public double getOebMin() {
        return oebMin;
    }

    /** Set the minimum physical dimension of the OEB.
     * @param oebMin the minimum physical dimension of the OEB.
     */
    public void setOebMin(final double oebMin) {
        refuseFurtherComments();
        this.oebMin = oebMin;
    }

    /** Get the cross-sectional area of Optimally Enclosing Box when viewed along the maximum OEB direction.
     * @return cross-sectional area of Optimally Enclosing Box when viewed along the maximum OEB direction.
     */
    public double getOebAreaAlongMax() {
        return oebAreaAlongMax;
    }

    /** Set the cross-sectional area of Optimally Enclosing Box when viewed along the maximum OEB direction.
     * @param oebAreaAlongMax cross-sectional area of Optimally Enclosing Box when viewed along the maximum OEB direction.
     */
    public void setOebAreaAlongMax(final double oebAreaAlongMax) {
        refuseFurtherComments();
        this.oebAreaAlongMax = oebAreaAlongMax;
    }

    /** Get the cross-sectional area of Optimally Enclosing Box when viewed along the intermediate OEB direction.
     * @return cross-sectional area of Optimally Enclosing Box when viewed along the intermediate OEB direction.
     */
    public double getOebAreaAlongInt() {
        return oebAreaAlongInt;
    }

    /** Set the cross-sectional area of Optimally Enclosing Box when viewed along the intermediate OEB direction.
     * @param oebAreaAlongInt cross-sectional area of Optimally Enclosing Box when viewed along the intermediate OEB direction.
     */
    public void setOebAreaAlongInt(final double oebAreaAlongInt) {
        refuseFurtherComments();
        this.oebAreaAlongInt = oebAreaAlongInt;
    }

    /** Get the cross-sectional area of Optimally Enclosing Box when viewed along the minimum OEB direction.
     * @return cross-sectional area of Optimally Enclosing Box when viewed along the minimum OEB direction.
     */
    public double getOebAreaAlongMin() {
        return oebAreaAlongMin;
    }

    /** Set the cross-sectional area of Optimally Enclosing Box when viewed along the minimum OEB direction.
     * @param oebAreaAlongMin cross-sectional area of Optimally Enclosing Box when viewed along the minimum OEB direction.
     */
    public void setOebAreaAlongMin(final double oebAreaAlongMin) {
        refuseFurtherComments();
        this.oebAreaAlongMin = oebAreaAlongMin;
    }


    /** Get the typical (50th percentile) radar cross-section.
     * @return typical (50th percentile) radar cross-section
     */
    public double getRcs() {
        return rcs;
    }

    /** Set the typical (50th percentile) radar cross-section.
     * @param rcs typical (50th percentile) radar cross-section
     */
    public void setRcs(final double rcs) {
        refuseFurtherComments();
        this.rcs = rcs;
    }

    /** Get the minimum radar cross-section.
     * @return minimum radar cross-section
     */
    public double getMinRcs() {
        return minRcs;
    }

    /** Set the minimum radar cross-section.
     * @param minRcs minimum radar cross-section
     */
    public void setMinRcs(final double minRcs) {
        refuseFurtherComments();
        this.minRcs = minRcs;
    }

    /** Get the maximum radar cross-section.
     * @return maximum radar cross-section
     */
    public double getMaxRcs() {
        return maxRcs;
    }

    /** Set the maximum radar cross-section.
     * @param maxRcs maximum radar cross-section
     */
    public void setMaxRcs(final double maxRcs) {
        refuseFurtherComments();
        this.maxRcs = maxRcs;
    }

    /** Get the typical (50th percentile) visual magnitude.
     * @return typical (50th percentile) visual magnitude
     */
    public double getVmAbsolute() {
        return vmAbsolute;
    }

    /** Set the typical (50th percentile) visual magnitude.
     * @param vmAbsolute typical (50th percentile) visual magnitude
     */
    public void setVmAbsolute(final double vmAbsolute) {
        refuseFurtherComments();
        this.vmAbsolute = vmAbsolute;
    }

    /** Get the minimum apparent visual magnitude.
     * @return minimum apparent visual magnitude
     */
    public double getVmApparentMin() {
        return vmApparentMin;
    }

    /** Set the minimum apparent visual magnitude.
     * @param vmApparentMin minimum apparent visual magnitude
     */
    public void setVmApparentMin(final double vmApparentMin) {
        refuseFurtherComments();
        this.vmApparentMin = vmApparentMin;
    }

    /** Get the typical (50th percentile) apparent visual magnitude.
     * @return typical (50th percentile) apparent visual magnitude
     */
    public double getVmApparent() {
        return vmApparent;
    }

    /** Set the typical (50th percentile) apparent visual magnitude.
     * @param vmApparent typical (50th percentile) apparent visual magnitude
     */
    public void setVmApparent(final double vmApparent) {
        refuseFurtherComments();
        this.vmApparent = vmApparent;
    }

    /** Get the maximum apparent visual magnitude.
     * @return maximum apparent visual magnitude
     */
    public double getVmApparentMax() {
        return vmApparentMax;
    }

    /** Set the maximum apparent visual magnitude.
     * @param vmApparentMax maximum apparent visual magnitude
     */
    public void setVmApparentMax(final double vmApparentMax) {
        refuseFurtherComments();
        this.vmApparentMax = vmApparentMax;
    }

    /** Get the typical (50th percentile) coefficient of reflectance.
     * @return typical (50th percentile) coefficient of reflectance
     */
    public double getReflectance() {
        return reflectance;
    }

    /** Set the typical (50th percentile) coefficient of reflectance.
     * @param reflectance typical (50th percentile) coefficient of reflectance
     */
    public void setReflectance(final double reflectance) {
        refuseFurtherComments();
        this.reflectance = reflectance;
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
    public double getPeriapsissAltitude() {
        return periapsissAltitude;
    }

    /** Set the distance of the closest point in the objects orbit above the equatorial radius of the central body.
     * @param periapsissAltitude the periapsissHeight to set
     */
    public void setPeriapsissAltitude(final double periapsissAltitude) {
        refuseFurtherComments();
        this.periapsissAltitude = periapsissAltitude;
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

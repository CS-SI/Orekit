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

import java.util.Collections;
import java.util.List;

import org.orekit.bodies.CelestialBodies;
import org.orekit.files.ccsds.definitions.BodyFacade;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.time.AbsoluteDate;

/** Perturbation parameters.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class Perturbations extends CommentsContainer {

    /** Name of atmospheric model. */
    private String atmosphericModel;

    /** Gravity model name. */
    private String gravityModel;

    /** Degree of the gravity model. */
    private int gravityDegree;

    /** Order of the gravity model. */
    private int gravityOrder;

    /** Oblate spheroid equatorial radius of central body. */
    private double equatorialRadius;

    /** Gravitational coefficient of attracting body. */
    private double gm;

    /** N-body perturbation bodies. */
    private List<BodyFacade> nBodyPerturbations;

    /** Central body angular rotation rate. */
    private double centralBodyRotation;

    /** Central body oblate spheroid oblateness. */
    private double oblateFlattening;

    /** Ocean tides model. */
    private String oceanTidesModel;

    /** Solid tides model. */
    private String solidTidesModel;

    /** Reduction theory used for precession and nutation modeling. */
    private String reductionTheory;

    /** Albedo model. */
    private String albedoModel;

    /** Albedo grid size. */
    private int albedoGridSize;

    /** Shadow model used for solar radiation pressure. */
    private ShadowModel shadowModel;

    /** Celestial bodies casting shadow. */
    private List<BodyFacade> shadowBodies;

    /** Solar Radiation Pressure model. */
    private String srpModel;

    /** Space Weather data source. */
    private String spaceWeatherSource;

    /** Epoch of the Space Weather data. */
    private AbsoluteDate spaceWeatherEpoch;

    /** Interpolation method for Space Weather data. */
    private String interpMethodSW;

    /** Fixed (time invariant) value of the planetary 3-hour-range geomagnetic index Kₚ. */
    private double fixedGeomagneticKp;

    /** Fixed (time invariant) value of the planetary 3-hour-range geomagnetic index aₚ. */
    private double fixedGeomagneticAp;

    /** Fixed (time invariant) value of the planetary 1-hour-range geomagnetic index Dst. */
    private double fixedGeomagneticDst;

    /** Fixed (time invariant) value of the Solar Flux Unit daily proxy F10.7. */
    private double fixedF10P7;

    /** Fixed (time invariant) value of the Solar Flux Unit 81-day running center-average proxy F10.7. */
    private double fixedF10P7Mean;

    /** Fixed (time invariant) value of the Solar Flux daily proxy M10.7. */
    private double fixedM10P7;

    /** Fixed (time invariant) value of the Solar Flux 81-day running center-average proxy M10.7. */
    private double fixedM10P7Mean;

    /** Fixed (time invariant) value of the Solar Flux daily proxy S10.7. */
    private double fixedS10P7;

    /** Fixed (time invariant) value of the Solar Flux 81-day running center-average proxy S10.7. */
    private double fixedS10P7Mean;

    /** Fixed (time invariant) value of the Solar Flux daily proxy Y10.7. */
    private double fixedY10P7;

    /** Fixed (time invariant) value of the Solar Flux 81-day running center-average proxy Y10.7. */
    private double fixedY10P7Mean;

    /** Simple constructor.
     * @param celestialBodies factory for celestial bodies
     */
    public Perturbations(final CelestialBodies celestialBodies) {
        // we don't call the setXxx() methods in order to avoid
        // calling refuseFurtherComments as a side effect
        equatorialRadius    = Double.NaN;
        gm                  = Double.NaN;
        centralBodyRotation = Double.NaN;
        oblateFlattening    = Double.NaN;
        fixedGeomagneticKp  = Double.NaN;
        fixedGeomagneticAp  = Double.NaN;
        fixedGeomagneticDst = Double.NaN;
        fixedF10P7          = Double.NaN;
        fixedF10P7Mean      = Double.NaN;
        fixedM10P7          = Double.NaN;
        fixedM10P7Mean      = Double.NaN;
        fixedS10P7          = Double.NaN;
        fixedS10P7Mean      = Double.NaN;
        fixedY10P7          = Double.NaN;
        fixedY10P7Mean      = Double.NaN;
        shadowBodies = Collections.singletonList(new BodyFacade(celestialBodies.getEarth().getName(),
                                                                celestialBodies.getEarth()));
    }

    /** Get name of atmospheric model.
     * @return name of atmospheric model
     */
    public String getAtmosphericModel() {
        return atmosphericModel;
    }

    /** Set name of atmospheric model.
     * @param atmosphericModel name of atmospheric model
     */
    public void setAtmosphericModel(final String atmosphericModel) {
        this.atmosphericModel = atmosphericModel;
    }

    /** Get gravity model name.
     * @return gravity model name
     */
    public String getGravityModel() {
        return gravityModel;
    }

    /** Get degree of the gravity model.
     * @return degree of the gravity model
     */
    public int getGravityDegree() {
        return gravityDegree;
    }

    /** Get order of the gravity model.
     * @return order of the gravity model
     */
    public int getGravityOrder() {
        return gravityOrder;
    }

    /** Set gravity model.
     * @param name name of the model
     * @param degree degree of the model
     * @param order order of the model
     */
    public void setGravityModel(final String name, final int degree, final int order) {
        this.gravityModel  = name;
        this.gravityDegree = degree;
        this.gravityOrder  = order;
    }

    /** Get oblate spheroid equatorial radius of central body.
     * @return oblate spheroid equatorial radius of central body
     */
    public double getEquatorialRadius() {
        return equatorialRadius;
    }

    /** Set oblate spheroid equatorial radius of central body.
     * @param equatorialRadius oblate spheroid equatorial radius of central body
     */
    public void setEquatorialRadius(final double equatorialRadius) {
        this.equatorialRadius = equatorialRadius;
    }

    /** Get gravitational coefficient of attracting body.
     * @return gravitational coefficient of attracting body
     */
    public double getGm() {
        return gm;
    }

    /** Set gravitational coefficient of attracting body.
     * @param gm gravitational coefficient of attracting body
     */
    public void setGm(final double gm) {
        this.gm = gm;
    }

    /** Get n-body perturbation bodies.
     * @return n-body perturbation bodies
     */
    public List<BodyFacade> getNBodyPerturbations() {
        return nBodyPerturbations;
    }

    /** Set n-body perturbation bodies.
     * @param nBody n-body perturbation bodies
     */
    public void setNBodyPerturbations(final List<BodyFacade> nBody) {
        this.nBodyPerturbations = nBody;
    }

    /** Get central body angular rotation rate.
     * @return central body angular rotation rate
     */
    public double getCentralBodyRotation() {
        return centralBodyRotation;
    }

    /** Set central body angular rotation rate.
     * @param centralBodyRotation central body angular rotation rate
     */
    public void setCentralBodyRotation(final double centralBodyRotation) {
        this.centralBodyRotation = centralBodyRotation;
    }

    /** Get central body oblate spheroid oblateness.
     * @return central body oblate spheroid oblateness
     */
    public double getOblateFlattening() {
        return oblateFlattening;
    }

    /** Set central body oblate spheroid oblateness.
     * @param oblateFlattening central body oblate spheroid oblateness
     */
    public void setOblateFlattening(final double oblateFlattening) {
        this.oblateFlattening = oblateFlattening;
    }

    /** Get ocean tides model.
     * @return ocean tides model
     */
    public String getOceanTidesModel() {
        return oceanTidesModel;
    }

    /** Set ocean tides model.
     * @param oceanTidesModel ocean tides model
     */
    public void setOceanTidesModel(final String oceanTidesModel) {
        this.oceanTidesModel = oceanTidesModel;
    }

    /** Get solid tides model.
     * @return solid tides model
     */
    public String getSolidTidesModel() {
        return solidTidesModel;
    }

    /** Set solid tides model.
     * @param solidTidesModel solid tides model
     */
    public void setSolidTidesModel(final String solidTidesModel) {
        this.solidTidesModel = solidTidesModel;
    }

    /** Get reduction theory used for precession and nutation modeling.
     * @return reduction theory used for precession and nutation modeling
     */
    public String getReductionTheory() {
        return reductionTheory;
    }

    /** Set reduction theory used for precession and nutation modeling.
     * @param reductionTheory reduction theory used for precession and nutation modeling
     */
    public void setReductionTheory(final String reductionTheory) {
        this.reductionTheory = reductionTheory;
    }

    /** Get albedo model.
     * @return albedo model
     */
    public String getAlbedoModel() {
        return albedoModel;
    }

    /** Set albedo model.
     * @param albedoModel albedo model
     */
    public void setAlbedoModel(final String albedoModel) {
        this.albedoModel = albedoModel;
    }

    /** Get albedo grid size.
     * @return albedo grid size
     */
    public int getAlbedoGridSize() {
        return albedoGridSize;
    }

    /** Set albedo grid size.
     * @param albedoGridSize albedo grid size
     */
    public void setAlbedoGridSize(final int albedoGridSize) {
        this.albedoGridSize = albedoGridSize;
    }

    /** Get shadow model used for solar radiation pressure.
     * @return shadow model used for solar radiation pressure
     */
    public ShadowModel getShadowModel() {
        return shadowModel;
    }

    /** Set shadow model used for solar radiation pressure.
     * @param shadowModel shadow model used for solar radiation pressure
     */
    public void setShadowModel(final ShadowModel shadowModel) {
        this.shadowModel = shadowModel;
    }

    /** Get celestial bodies casting shadows.
     * @return celestial bodies casting shadows
     */
    public List<BodyFacade> getShadowBodies() {
        return shadowBodies;
    }

    /** Set celestial bodies casting shadows.
     * @param shadowBodies celestial bodies casting shadows
     */
    public void setShadowBodies(final List<BodyFacade> shadowBodies) {
        this.shadowBodies = shadowBodies;
    }

    /** Get Solar Radiation Pressure model.
     * @return Solar Radiation Pressure model
     */
    public String getSrpModel() {
        return srpModel;
    }

    /** Set Solar Radiation Pressure model.
     * @param srpModel Solar Radiation Pressure model
     */
    public void setSrpModel(final String srpModel) {
        this.srpModel = srpModel;
    }

    /** Get Space Weather data source.
     * @return Space Weather data source
     */
    public String getSpaceWeatherSource() {
        return spaceWeatherSource;
    }

    /** Set Space Weather data source.
     * @param spaceWeatherSource Space Weather data source
     */
    public void setSpaceWeatherSource(final String spaceWeatherSource) {
        this.spaceWeatherSource = spaceWeatherSource;
    }

    /** Get epoch of the Space Weather data.
     * @return epoch of the Space Weather data
     */
    public AbsoluteDate getSpaceWeatherEpoch() {
        return spaceWeatherEpoch;
    }

    /** Set epoch of the Space Weather data.
     * @param spaceWeatherEpoch epoch of the Space Weather data
     */
    public void setSpaceWeatherEpoch(final AbsoluteDate spaceWeatherEpoch) {
        this.spaceWeatherEpoch = spaceWeatherEpoch;
    }

    /** Get the interpolation method for Space Weather data.
     * @return interpolation method for Space Weather data
     */
    public String getInterpMethodSW() {
        return interpMethodSW;
    }

    /** Set the interpolation method for Space Weather data.
     * @param interpMethodSW interpolation method for Space Weather data
     */
    public void setInterpMethodSW(final String interpMethodSW) {
        refuseFurtherComments();
        this.interpMethodSW = interpMethodSW;
    }

    /** Get fixed (time invariant) value of the planetary 3-hour-range geomagnetic index Kₚ.
     * @return fixed (time invariant) value of the planetary 3-hour-range geomagnetic index Kₚ
     */
    public double getFixedGeomagneticKp() {
        return fixedGeomagneticKp;
    }

    /** Set fixed (time invariant) value of the planetary 3-hour-range geomagnetic index Kₚ.
     * @param fixedGeomagneticKp fixed (time invariant) value of the planetary 3-hour-range geomagnetic index Kₚ
     */
    public void setFixedGeomagneticKp(final double fixedGeomagneticKp) {
        this.fixedGeomagneticKp = fixedGeomagneticKp;
    }

    /** Get fixed (time invariant) value of the planetary 3-hour-range geomagnetic index aₚ.
     * @return fixed (time invariant) value of the planetary 3-hour-range geomagnetic index aₚ
     */
    public double getFixedGeomagneticAp() {
        return fixedGeomagneticAp;
    }

    /** Set fixed (time invariant) value of the planetary 3-hour-range geomagnetic index aₚ.
     * @param fixedGeomagneticAp fixed (time invariant) value of the planetary 3-hour-range geomagnetic index aₚ
     */
    public void setFixedGeomagneticAp(final double fixedGeomagneticAp) {
        this.fixedGeomagneticAp = fixedGeomagneticAp;
    }

    /** Get fixed (time invariant) value of the planetary 1-hour-range geomagnetic index Dst.
     * @return fixed (time invariant) value of the planetary 1-hour-range geomagnetic index Dst
     */
    public double getFixedGeomagneticDst() {
        return fixedGeomagneticDst;
    }

    /** Set fixed (time invariant) value of the planetary 1-hour-range geomagnetic index Dst.
     * @param fixedGeomagneticDst fixed (time invariant) value of the planetary 1-hour-range geomagnetic index Dst
     */
    public void setFixedGeomagneticDst(final double fixedGeomagneticDst) {
        this.fixedGeomagneticDst = fixedGeomagneticDst;
    }

    /** Get fixed (time invariant) value of the Solar Flux Unit daily proxy F10.7.
     * @return fixed (time invariant) value of the Solar Flux Unit daily proxy F10.7
     */
    public double getFixedF10P7() {
        return fixedF10P7;
    }

    /** Set fixed (time invariant) value of the Solar Flux Unit daily proxy F10.7.
     * @param fixedF10P7 fixed (time invariant) value of the Solar Flux Unit daily proxy F10.7
     */
    public void setFixedF10P7(final double fixedF10P7) {
        this.fixedF10P7 = fixedF10P7;
    }

    /** Get fixed (time invariant) value of the Solar Flux Unit 81-day running center-average proxy F10.7.
     * @return fixed (time invariant) value of the Solar Flux Unit 81-day running center-average proxy F10.7
     */
    public double getFixedF10P7Mean() {
        return fixedF10P7Mean;
    }

    /** Set fixed (time invariant) value of the Solar Flux Unit 81-day running center-average proxy F10.7.
     * @param fixedF10P7Mean fixed (time invariant) value of the Solar Flux Unit 81-day running center-average proxy F10.7
     */
    public void setFixedF10P7Mean(final double fixedF10P7Mean) {
        this.fixedF10P7Mean = fixedF10P7Mean;
    }

    /** Get fixed (time invariant) value of the Solar Flux daily proxy M10.7.
     * @return fixed (time invariant) value of the Solar Flux daily proxy M10.7
     */
    public double getFixedM10P7() {
        return fixedM10P7;
    }

    /** Set fixed (time invariant) value of the Solar Flux daily proxy M10.7.
     * @param fixedM10P7 fixed (time invariant) value of the Solar Flux daily proxy M10.7
     */
    public void setFixedM10P7(final double fixedM10P7) {
        this.fixedM10P7 = fixedM10P7;
    }

    /** Get fixed (time invariant) value of the Solar Flux 81-day running center-average proxy M10.7.
     * @return fixed (time invariant) value of the Solar Flux 81-day running center-average proxy M10.7
     */
    public double getFixedM10P7Mean() {
        return fixedM10P7Mean;
    }

    /** Set fixed (time invariant) value of the Solar Flux 81-day running center-average proxy M10.7.
     * @param fixedM10P7Mean fixed (time invariant) value of the Solar Flux 81-day running center-average proxy M10.7
     */
    public void setFixedM10P7Mean(final double fixedM10P7Mean) {
        this.fixedM10P7Mean = fixedM10P7Mean;
    }

    /** Get fixed (time invariant) value of the Solar Flux daily proxy S10.7.
     * @return fixed (time invariant) value of the Solar Flux daily proxy S10.7
     */
    public double getFixedS10P7() {
        return fixedS10P7;
    }

    /** Set fixed (time invariant) value of the Solar Flux daily proxy S10.7.
     * @param fixedS10P7 fixed (time invariant) value of the Solar Flux daily proxy S10.7
     */
    public void setFixedS10P7(final double fixedS10P7) {
        this.fixedS10P7 = fixedS10P7;
    }

    /** Get fixed (time invariant) value of the Solar Flux 81-day running center-average proxy S10.7.
     * @return fixed (time invariant) value of the Solar Flux 81-day running center-average proxy S10.7
     */
    public double getFixedS10P7Mean() {
        return fixedS10P7Mean;
    }

    /** Set fixed (time invariant) value of the Solar Flux 81-day running center-average proxy S10.7.
     * @param fixedS10P7Mean fixed (time invariant) value of the Solar Flux 81-day running center-average proxy S10.7
     */
    public void setFixedS10P7Mean(final double fixedS10P7Mean) {
        this.fixedS10P7Mean = fixedS10P7Mean;
    }

    /** Get fixed (time invariant) value of the Solar Flux daily proxy Y10.7.
     * @return fixed (time invariant) value of the Solar Flux daily proxy Y10.7
     */
    public double getFixedY10P7() {
        return fixedY10P7;
    }

    /** Set fixed (time invariant) value of the Solar Flux daily proxy Y10.7.
     * @param fixedY10P7 fixed (time invariant) value of the Solar Flux daily proxy Y10.7
     */
    public void setFixedY10P7(final double fixedY10P7) {
        this.fixedY10P7 = fixedY10P7;
    }

    /** Get fixed (time invariant) value of the Solar Flux 81-day running center-average proxy Y10.7.
     * @return fixed (time invariant) value of the Solar Flux 81-day running center-average proxy Y10.7
     */
    public double getFixedY10P7Mean() {
        return fixedY10P7Mean;
    }

    /** Set fixed (time invariant) value of the Solar Flux 81-day running center-average proxy Y10.7.
     * @param fixedY10P7Mean fixed (time invariant) value of the Solar Flux 81-day running center-average proxy Y10.7
     */
    public void setFixedY10P7Mean(final double fixedY10P7Mean) {
        this.fixedY10P7Mean = fixedY10P7Mean;
    }

}

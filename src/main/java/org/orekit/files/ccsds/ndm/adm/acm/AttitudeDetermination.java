/* Copyright 2023 Luc Maisonobe
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

package org.orekit.files.ccsds.ndm.adm.acm;

import java.util.Arrays;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.AdMethodType;
import org.orekit.files.ccsds.ndm.adm.AttitudeEndpoints;
import org.orekit.files.ccsds.section.CommentsContainer;

/** Attitude determination data.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class AttitudeDetermination extends CommentsContainer {

    /** Endpoints (i.e. frames A, B and their relationship). */
    private final AttitudeEndpoints endpoints;

    /** Identification number. */
    private String id;

    /** Identification of previous orbit determination. */
    private String prevId;

    /** Attitude determination method. */
    private AdMethodType method;

    /** Source of attitude estimate. */
    private String source;

    /** Rotation order for Euler angles. */
    private RotationOrder eulerRotSeq;

    /** Number of states for {@link AdMethodType#EKF}, {@link AdMethodType#BATCH} or {@link AdMethodType#FILTER_SMOOTHER}. */
    private int nbStates;

    /** Attitude states. */
    private AttitudeElementsType attitudeStates;

    /** Type of attitude error state. */
    private AttitudeCovarianceType covarianceType;

    /** Attitude rate states. */
    private RateElementsType rateStates;

    /** Rate random walk if {@link #rateStates} is {@link RateElementsType#GYRO_BIAS}. */
    private double sigmaU;

    /** Angle random walk if {@link #rateStates} is {@link RateElementsType#GYRO_BIAS}. */
    private double sigmaV;

    /** Process noise standard deviation if {@link #rateStates} is {@link RateElementsType#ANGVEL}. */
    private double rateProcessNoiseStdDev;

    /** Number of sensors used. */
    private int nbSensorsUsed;

    /** Sensors used. */
    private String[] sensorsUsed;

    /** Number of noise elements for sensor i. */
    private int[] nbSensorNoiseCovariance;

    /** Standard deviation of sensor noises for sensor i. */
    private double[][] sensorNoiseCovariance;

    /** Frequency of sensor i data. */
    private double[] sensorFrequency;

    /** Simple constructor.
     */
    public AttitudeDetermination() {
        endpoints = new AttitudeEndpoints();
        nbStates  = -1;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        checkNotNull(attitudeStates, AttitudeDeterminationKey.ATTITUDE_STATES.name());
        endpoints.checkExternalFrame(AttitudeDeterminationKey.REF_FRAME_A,
                                     AttitudeDeterminationKey.REF_FRAME_B);
        for (int i = 0; i < nbSensorsUsed; ++i) {
            checkNotNull(sensorsUsed[i], AttitudeDeterminationSensorKey.SENSORS_USED.name() + "_" + i);
            if (nbSensorNoiseCovariance[i] >= 0) {
                final int n = sensorNoiseCovariance[i] == null ? 0 : sensorNoiseCovariance[i].length;
                if (nbSensorNoiseCovariance[i] != n) {
                    throw new OrekitException(OrekitMessages.INCONSISTENT_NUMBER_OF_ELEMENTS,
                                              nbSensorNoiseCovariance[i], n);
                }
            }
        }
    }

    /** Get the endpoints (i.e. frames A, B and their relationship).
     * @return endpoints
     */
    public AttitudeEndpoints getEndpoints() {
        return endpoints;
    }

    /** Get identification number.
     * @return identification number
     */
    public String getId() {
        return id;
    }

    /** Set identification number.
     * @param id identification number
     */
    public void setId(final String id) {
        this.id = id;
    }

    /** Get identification of previous orbit determination.
     * @return identification of previous orbit determination
     */
    public String getPrevId() {
        return prevId;
    }

    /** Set identification of previous orbit determination.
     * @param prevId identification of previous orbit determination
     */
    public void setPrevId(final String prevId) {
        this.prevId = prevId;
    }

    /** Get attitude determination method.
     * @return attitude determination method
     */
    public AdMethodType getMethod() {
        return method;
    }

    /** Set attitude determination method.
     * @param method attitude determination method
     */
    public void setMethod(final AdMethodType method) {
        this.method = method;
    }

    /** Get source of attitude estimate.
     * @return source of attitude estimate
     */
    public String getSource() {
        return source;
    }

    /** Set source of attitude estimate.
     * @param source source of attitude estimate
     */
    public void setSource(final String source) {
        this.source = source;
    }

    /** Get the rotation order for Euler angles.
     * @return rotation order for Euler angles
     */
    public RotationOrder getEulerRotSeq() {
        return eulerRotSeq;
    }

    /** Set the rotation order for Euler angles.
     * @param eulerRotSeq rotation order for Euler angles
     */
    public void setEulerRotSeq(final RotationOrder eulerRotSeq) {
        this.eulerRotSeq = eulerRotSeq;
    }

    /** Get number of states for {@link AdMethodType#EKF}, {@link AdMethodType#BATCH} or {@link AdMethodType#FILTER_SMOOTHER}.
     * @return number of states
     */
    public int getNbStates() {
        return nbStates;
    }

    /** Set number of states for {@link AdMethodType#EKF}, {@link AdMethodType#BATCH} or {@link AdMethodType#FILTER_SMOOTHER}.
     * @param nbStates number of states
     */
    public void setNbStates(final int nbStates) {
        this.nbStates = nbStates;
    }

    /** Get attitude states.
     * @return attitude states
     */
    public AttitudeElementsType getAttitudeStates() {
        return attitudeStates;
    }

    /** Set attitude states.
     * @param attitudeStates attitude states
     */
    public void setAttitudeStates(final AttitudeElementsType attitudeStates) {
        this.attitudeStates = attitudeStates;
    }

    /** Get type of attitude error state.
     * @return type of attitude error state
     */
    public AttitudeCovarianceType getCovarianceType() {
        return covarianceType;
    }

    /** Set type of attitude error state.
     * @param covarianceType type of attitude error state
     */
    public void setCovarianceType(final AttitudeCovarianceType covarianceType) {
        this.covarianceType = covarianceType;
    }

    /** Get attitude rate states.
     * @return attitude rate states
     */
    public RateElementsType getRateStates() {
        return rateStates;
    }

    /** Set attitude rate states.
     * @param rateStates attitude rate states
     */
    public void setRateStates(final RateElementsType rateStates) {
        this.rateStates = rateStates;
    }

    /** Get rate random walk if {@link #rateStates} is {@link RateElementsType#GYRO_BIAS}.
     * @return rate random walk if {@link #rateStates} is {@link RateElementsType#GYRO_BIAS}
     */
    public double getSigmaU() {
        return sigmaU;
    }

    /** Set rate random walk if {@link #rateStates} is {@link RateElementsType#GYRO_BIAS}.
     * @param sigmaU rate random walk if {@link #rateStates} is {@link RateElementsType#GYRO_BIAS}
     */
    public void setSigmaU(final double sigmaU) {
        this.sigmaU = sigmaU;
    }

    /** Get angle random walk if {@link #rateStates} is {@link RateElementsType#GYRO_BIAS}.
     * @return angle random walk if {@link #rateStates} is {@link RateElementsType#GYRO_BIAS}
     */
    public double getSigmaV() {
        return sigmaV;
    }

    /** Set angle random walk if {@link #rateStates} is {@link RateElementsType#GYRO_BIAS}.
     * @param sigmaV angle random walk if {@link #rateStates} is {@link RateElementsType#GYRO_BIAS}
     */
    public void setSigmaV(final double sigmaV) {
        this.sigmaV = sigmaV;
    }

    /** Get process noise standard deviation if {@link #rateStates} is {@link RateElementsType#ANGVEL}.
     * @return process noise standard deviation if {@link #rateStates} is {@link RateElementsType#ANGVEL}
     */
    public double getRateProcessNoiseStdDev() {
        return rateProcessNoiseStdDev;
    }

    /** Set process noise standard deviation if {@link #rateStates} is {@link RateElementsType#ANGVEL}.
     * @param rateProcessNoiseStdDev process noise standard deviation if {@link #rateStates} is {@link RateElementsType#ANGVEL}
     */
    public void setRateProcessNoiseStdDev(final double rateProcessNoiseStdDev) {
        this.rateProcessNoiseStdDev = rateProcessNoiseStdDev;
    }

    /** Get number of sensors used.
     * @return number of sensors used
     */
    public int getNbSensorsUsed() {
        return nbSensorsUsed;
    }

    /** Set number of sensors used.
     * @param nbSensorsUsed number of sensors used
     */
    public void setNbSensorsUsed(final int nbSensorsUsed) {

        this.nbSensorsUsed      = nbSensorsUsed;

        sensorsUsed             = new String[nbSensorsUsed];
        nbSensorNoiseCovariance = new int[nbSensorsUsed];
        sensorNoiseCovariance   = new double[nbSensorsUsed][];
        sensorFrequency         = new double[nbSensorsUsed];

        Arrays.fill(nbSensorNoiseCovariance, -1);
        Arrays.fill(sensorFrequency, Double.NaN);

    }

    /** Ensure a sensor index is within range.
     * @param i sensor index, counting from 1
     * @exception OrekitException if index is out of range
     */
    private void ensureSensorIndexInRange(final int i) {
        if (i < 1 || i > nbSensorsUsed) {
            throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_SENSOR_INDEX, i, nbSensorsUsed);
        }
    }

    /** Get sensor used.
     * @param i sensor index, counting from 1 (not 0!)
     * @return sensor used
     */
    public String getSensorUsed(final int i) {
        ensureSensorIndexInRange(i);
        return sensorsUsed[i - 1];
    }

    /** Set sensor used.
     * @param i sensor index, counting from 1 (not 0!)
     * @param sensor sensor used
     */
    public void setSensorUsed(final int i, final String sensor) {
        ensureSensorIndexInRange(i);
        sensorsUsed[i - 1] = sensor;
    }

    /** Get number of noise elements for sensor.
     * @param i sensor index, counting from 1 (not 0!)
     * @return number of noise elements for sensor
     */
    public int getNbSensorNoiseCovariance(final int i) {
        ensureSensorIndexInRange(i);
        return nbSensorNoiseCovariance[i - 1];
    }

    /** Set number of noise elements for sensor.
     * @param i sensor index, counting from 1 (not 0!)
     * @param n number of noise elements for sensor
     */
    public void setNbSensorNoiseCovariance(final int i, final int n) {
        ensureSensorIndexInRange(i);
        nbSensorNoiseCovariance[i - 1] = n;
    }

    /** Get standard deviation of sensor noise for sensor.
     * @param i sensor index, counting from 1 (not 0!)
     * @return standard deviation of sensor noise for sensor
     */
    public double[] getSensorNoiseCovariance(final int i) {
        ensureSensorIndexInRange(i);
        return sensorNoiseCovariance[i - 1] == null ? null : sensorNoiseCovariance[i - 1].clone();
    }

    /** Set standard deviation of sensor noise for sensor.
     * @param i sensor index, counting from 1 (not 0!)
     * @param stddev standard deviation of sensor noise
     */
    public void setSensorNoiseCovariance(final int i, final double[] stddev) {
        ensureSensorIndexInRange(i);
        if (stddev.length != nbSensorNoiseCovariance[i - 1]) {
            throw new OrekitException(OrekitMessages.INCONSISTENT_NUMBER_OF_ELEMENTS,
                                      nbSensorNoiseCovariance[i - 1], stddev.length);
        }
        sensorNoiseCovariance[i - 1] = stddev.clone();
    }

    /** Get frequency of sensor data for sensor.
     * @param i sensor index, counting from 1 (not 0!)
     * @return frequency of sensor data for sensor
     */
    public double getSensorFrequency(final int i) {
        ensureSensorIndexInRange(i);
        return sensorFrequency[i - 1];
    }

    /** Set frequency of sensor data for sensor.
     * @param i sensor index, counting from 1 (not 0!)
     * @param frequency frequency of sensor data for sensor
     */
    public void setSensorFrequency(final int i, final double frequency) {
        ensureSensorIndexInRange(i);
        sensorFrequency[i - 1] = frequency;
    }

}

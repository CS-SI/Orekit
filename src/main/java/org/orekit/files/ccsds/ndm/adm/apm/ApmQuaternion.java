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
package org.orekit.files.ccsds.ndm.adm.apm;

import java.util.Arrays;

import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.complex.Quaternion;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.Attitude;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.adm.AttitudeEndoints;
import org.orekit.files.ccsds.section.Section;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;

/**
 * Container for Attitude Parameter Message quaternion logical block.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class ApmQuaternion implements Section {

    /** Epoch of the data. */
    private AbsoluteDate epoch;

    /** Endpoints (i.e. frames A, B and their relationship). */
    private final AttitudeEndoints endpoints;

    /** Quaternion. */
    private double[] q;

    /** Quaternion derivative. */
    private double[] qDot;

    /** Simple constructor.
     */
    public ApmQuaternion() {
        endpoints = new AttitudeEndoints();
        q         = new double[4];
        qDot      = new double[4];
        Arrays.fill(q,    Double.NaN);
        Arrays.fill(qDot, Double.NaN);
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        endpoints.checkMandatoryEntriesExceptExternalFrame(ApmQuaternionKey.Q_FRAME_A,
                                                           ApmQuaternionKey.Q_FRAME_B,
                                                           ApmQuaternionKey.Q_DIR);
        endpoints.checkExternalFrame(ApmQuaternionKey.Q_FRAME_A, ApmQuaternionKey.Q_FRAME_B);
        if (Double.isNaN(q[0] + q[1] + q[2] + q[3])) {
            throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, "Q{C|1|2|3}");
        }
    }

    /**
     * Get the epoch of the data.
     * @return epoch the epoch
     */
    public AbsoluteDate getEpoch() {
        return epoch;
    }

    /**
     * Set the epoch of the data.
     * @param epoch the epoch to be set
     */
    public void setEpoch(final AbsoluteDate epoch) {
        this.epoch = epoch;
    }

    /** Get the endpoints (i.e. frames A, B and their relationship).
     * @return endpoints
     */
    public AttitudeEndoints getEndpoints() {
        return endpoints;
    }

    /**
     * Get the quaternion.
     * @return quaternion
     */
    public Quaternion getQuaternion() {
        return new Quaternion(q[0], q[1], q[2], q[3]);
    }

    /**
     * Set quaternion component.
     * @param index component index (0 is scalar part)
     * @param value quaternion component
     */
    public void setQ(final int index, final double value) {
        this.q[index] = value;
    }

    /**
     * Get the quaternion derivative.
     * @return quaternion derivative
     */
    public Quaternion getQuaternionDot() {
        return new Quaternion(qDot[0], qDot[1], qDot[2], qDot[3]);
    }

    /**
     * Set quaternion derivative component.
     * @param index component index (0 is scalar part)
     * @param derivative quaternion derivative component
     */
    public void setQDot(final int index, final double derivative) {
        this.qDot[index] = derivative;
    }

    /** Check if the logical block includes rates.
     * @return true if logical block includes rates
     */
    public boolean hasRates() {
        return !Double.isNaN(qDot[0] + qDot[1] + qDot[2] + qDot[3]);
    }

    /** Get the attitude.
     * @param frame reference frame with respect to which attitude must be defined
     * (may be null if attitude is <em>not</em> orbit-relative and one wants
     * attitude in the same frame as used in the attitude message)
     * @param pvProvider provider for spacecraft position and velocity
     * (may be null if attitude is <em>not</em> orbit-relative)
     * @return attitude
     */
    public Attitude getAttitude(final Frame frame, final PVCoordinatesProvider pvProvider) {

        if (Double.isNaN(qDot[0])) {
            // rotation as it is stored in the APM
            final Rotation r = new Rotation(q[0], q[1], q[2], q[3], true);

            // attitude without rotation rate
            return endpoints.build(frame, pvProvider,
                                   new TimeStampedAngularCoordinates(epoch, r, Vector3D.ZERO, Vector3D.ZERO));

        } else {
            // attitude as it is stored in the APM
            final UnivariateDerivative1                q0 = new UnivariateDerivative1(q[0], qDot[0]);
            final UnivariateDerivative1                q1 = new UnivariateDerivative1(q[1], qDot[1]);
            final UnivariateDerivative1                q2 = new UnivariateDerivative1(q[2], qDot[2]);
            final UnivariateDerivative1                q3 = new UnivariateDerivative1(q[3], qDot[3]);
            final FieldRotation<UnivariateDerivative1> rd = new FieldRotation<>(q0, q1, q2, q3, true);

            // attitude converted to Orekit conventions
            return endpoints.build(frame, pvProvider, new TimeStampedAngularCoordinates(epoch, rd));
        }

    }

}

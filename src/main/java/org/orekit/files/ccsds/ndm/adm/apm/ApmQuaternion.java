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

import java.util.Arrays;

import org.hipparchus.complex.Quaternion;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.adm.AttitudeEndpoints;
import org.orekit.files.ccsds.section.CommentsContainer;

/**
 * Container for Attitude Parameter Message quaternion logical block.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class ApmQuaternion extends CommentsContainer {

    /** Endpoints (i.e. frames A, B and their relationship). */
    private final AttitudeEndpoints endpoints;

    /** Quaternion. */
    private double[] q;

    /** Quaternion derivative. */
    private double[] qDot;

    /** Simple constructor.
     */
    public ApmQuaternion() {
        endpoints = new AttitudeEndpoints();
        q         = new double[4];
        qDot      = new double[4];
        Arrays.fill(q,    Double.NaN);
        Arrays.fill(qDot, Double.NaN);
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        if (version < 2.0) {
            endpoints.checkMandatoryEntriesExceptExternalFrame(version,
                                                               ApmQuaternionKey.Q_FRAME_A,
                                                               ApmQuaternionKey.Q_FRAME_B,
                                                               ApmQuaternionKey.Q_DIR);
            endpoints.checkExternalFrame(ApmQuaternionKey.Q_FRAME_A, ApmQuaternionKey.Q_FRAME_B);
        } else {
            endpoints.checkMandatoryEntriesExceptExternalFrame(version,
                                                               ApmQuaternionKey.REF_FRAME_A,
                                                               ApmQuaternionKey.REF_FRAME_B,
                                                               ApmQuaternionKey.Q_DIR);
            endpoints.checkExternalFrame(ApmQuaternionKey.REF_FRAME_A, ApmQuaternionKey.REF_FRAME_B);
        }
        if (Double.isNaN(q[0] + q[1] + q[2] + q[3])) {
            throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, "Q{C|1|2|3}");
        }
    }

    /** Get the endpoints (i.e. frames A, B and their relationship).
     * @return endpoints
     */
    public AttitudeEndpoints getEndpoints() {
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
        refuseFurtherComments();
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
        refuseFurtherComments();
        this.qDot[index] = derivative;
    }

    /** Check if the logical block includes rates.
     * @return true if logical block includes rates
     */
    public boolean hasRates() {
        return !Double.isNaN(qDot[0] + qDot[1] + qDot[2] + qDot[3]);
    }

}

/* Copyright 2002-2021 CS GROUP
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

package org.orekit.files.ccsds.ndm.odm;

import java.util.Arrays;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.time.AbsoluteDate;

/** Container for state vector data.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class StateVector extends CommentsContainer {

    /** Epoch of state vector and optional Keplerian elements. */
    private AbsoluteDate epoch;

    /** Position vector (m). */
    private double[] position;

    /** Velocity vector (m/s). */
    private double[] velocity;

    /** Create an empty data set.
     */
    public StateVector() {
        position = new double[3];
        velocity = new double[3];
        Arrays.fill(position, Double.NaN);
        Arrays.fill(velocity, Double.NaN);
    }

    /** {@inheritDoc} */
    @Override
    public void checkMandatoryEntries() {
        super.checkMandatoryEntries();
        checkNotNull(epoch, StateVectorKey.EPOCH);
        if (Double.isNaN(position[0] + position[1] + position[2])) {
            throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, "{X|Y|Z}");
        }
        if (Double.isNaN(velocity[0] + velocity[1] + velocity[2])) {
            throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, "{X|Y|Z}_DOT");
        }
    }

    /** Get epoch of state vector, Keplerian elements and covariance matrix data.
     * @return epoch the epoch
     */
    public AbsoluteDate getEpoch() {
        return epoch;
    }

    /** Set epoch of state vector, Keplerian elements and covariance matrix data.
     * @param epoch the epoch to be set
     */
    public void setEpoch(final AbsoluteDate epoch) {
        refuseFurtherComments();
        this.epoch = epoch;
    }

    /** Get position vector.
     * @return the position vector
     */
    public Vector3D getPosition() {
        return new Vector3D(position);
    }

    /**
     * Set position component.
     * @param index component index (counting from 0)
     * @param value position component
     */
    public void setP(final int index, final double value) {
        refuseFurtherComments();
        position[index] = value;
    }

    /** Get velocity vector.
     * @return the velocity vector
     */
    public Vector3D getVelocity() {
        return new Vector3D(velocity);
    }

    /**
     * Set velocity component.
     * @param index component index (counting from 0)
     * @param value velocity component
     */
    public void setV(final int index, final double value) {
        refuseFurtherComments();
        velocity[index] = value;
    }

}


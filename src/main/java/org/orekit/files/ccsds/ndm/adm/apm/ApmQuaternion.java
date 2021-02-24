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
package org.orekit.files.ccsds.ndm.adm.apm;

import java.util.Arrays;

import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.complex.Quaternion;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.orekit.attitudes.Attitude;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedAngularCoordinates;

/**
 * Container for Attitude Parameter Message quaternion logical block.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class ApmQuaternion extends CommentsContainer {

    /** Epoch of the data. */
    private AbsoluteDate epoch;

    /** Frame A. */
    private FrameFacade frameA;

    /** Frame B. */
    private FrameFacade frameB;

    /** Flag for frames direction. */
    private Boolean a2b;

    /** Quaternion. */
    private double[] q;

    /** Quaternion derivative. */
    private double[] qDot;

    /** Simple constructor.
     */
    public ApmQuaternion() {
        q         = new double[4];
        qDot      = new double[4];
        Arrays.fill(q,    Double.NaN);
    }

    /** {@inheritDoc} */
    @Override
    public void checkMandatoryEntries() {
        super.checkMandatoryEntries();
        checkNotNull(frameA, ApmQuaternionKey.Q_FRAME_A);
        checkNotNull(frameB, ApmQuaternionKey.Q_FRAME_B);
        checkNotNull(a2b,    ApmQuaternionKey.Q_DIR);
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
        refuseFurtherComments();
        this.epoch = epoch;
    }

    /** Set frame A.
     * @param frameA frame A
     */
    public void setFrameA(final FrameFacade frameA) {
        this.frameA = frameA;
    }

    /** Get frame A.
     * @return frame A
     */
    public FrameFacade getFrameA() {
        return frameA;
    }

    /** Set frame B.
     * @param frameB frame B
     */
    public void setFrameB(final FrameFacade frameB) {
        this.frameB = frameB;
    }

    /** Get frame B.
     * @return frame B
     */
    public FrameFacade getFrameB() {
        return frameB;
    }

    /** Set rotation direction.
     * @param a2b if true, rotation is from {@link #getFrameA() frame A}
     * to {@link #getFrameB() frame B}
     */
    public void setA2b(final boolean a2b) {
        this.a2b = a2b;
    }

    /** Check if rotation direction is from {@link #getFrameA() frame A} to {@link #getFrameB() frame B}.
     * @return true if rotation direction is from {@link #getFrameA() frame A} to {@link #getFrameB() frame B}
     */
    public boolean isA2b() {
        return a2b == null ? true : a2b;
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

    /** Get the attitude.
     * @return attitude
     */
    public Attitude getAttitude() {

        final FrameFacade ext = frameA.asSpacecraftBodyFrame() == null ? frameA : frameB;
        if (ext.asFrame() == null) {
            // external frame has no Orekit mapping
            throw new OrekitException(OrekitMessages.CCSDS_INVALID_FRAME, ext.getName());
        }
        final boolean ext2local = a2b ^ (ext == frameB);

        // attitude has it is stored in the APM
        final FieldRotation<UnivariateDerivative1> raw =
                        new FieldRotation<>(new UnivariateDerivative1(q[0], qDot[0]),
                                            new UnivariateDerivative1(q[1], qDot[1]),
                                            new UnivariateDerivative1(q[2], qDot[2]),
                                            new UnivariateDerivative1(q[3], qDot[3]),
                                            true);

        // attitude converted to Orekit conventions
        return new Attitude(ext.asFrame(),
                            new TimeStampedAngularCoordinates(epoch,
                                                              ext2local ? raw : raw.revert()));

    }

}

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
package org.orekit.files.ccsds.ndm.adm.aem;

import org.orekit.files.ccsds.ndm.adm.AttitudeType;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedAngularCoordinates;

/** Container for one attitude entry.
 * @author Luc Maisonobe
 * @since 11.0
 */
class AttitudeEntry {

    /** Metadata used to interpret the data fields. */
    private final AemMetadata metadata;

    /** Epoch. */
    private AbsoluteDate epoch;

    /** Attitude components. */
    private double[] components;

    /** Build an uninitialized entry.
     * @param metadata metadata used to interpret the data fields
     */
    AttitudeEntry(final AemMetadata metadata) {
        this.metadata   = metadata;
        this.components = new double[8];
    }

    /** Get the metadata.
     * @return metadata
     */
    public AemMetadata getMetadata() {
        return metadata;
    }

    /** Set epoch.
     * @param epoch epoch to set
     */
    public void setEpoch(final AbsoluteDate epoch) {
        this.epoch = epoch;
    }

    /** Set one component.
     * @param i index of the component
     * @param value value of the component
     */
    public void setComponent(final int i, final double value) {
        components[i] = value;
    }

    /** Get component index of first rotation.
     * @return component index of first rotation
     */
    int firstRotationIndex() {
        return metadata.getAttitudeType() == AttitudeType.QUATERNION_RATE ? 4 : 3;
    }

    /** Get the angular coordinates entry.
     * @return angular coordinates entry
     */
    public TimeStampedAngularCoordinates getCoordinates() {
        return metadata.getAttitudeType().build(metadata.isFirst(),
                                                metadata.getEndpoints().isExternal2SpacecraftBody(),
                                                metadata.getEulerRotSeq(),
                                                metadata.isSpacecraftBodyRate(),
                                                epoch,
                                                components);
    }

}

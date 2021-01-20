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

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.adm.ADMSegment;
import org.orekit.files.general.AttitudeEphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedAngularCoordinates;

/**
 * This class stores the metadata and data for one attitude segment.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class AEMSegment extends ADMSegment<AEMMetadata, AEMData> implements AttitudeEphemerisFile.AttitudeEphemerisSegment {

    /** IERS conventions to use. */
    private final IERSConventions conventions;

    /** Data context. */
    private final DataContext dataContext;

    /** Simple constructor.
     * @param metadata segment metadata
     * @param data segment data
     * @param conventions IERS conventions to use
     * @param dataContext data context to use
     */
    public AEMSegment(final AEMMetadata metadata, final AEMData data,
                      final IERSConventions conventions, final DataContext dataContext) {
        super(metadata, data);
        this.conventions = conventions;
        this.dataContext = dataContext;
    }

    /** {@inheritDoc} */
    @Override
    public List<? extends TimeStampedAngularCoordinates> getAngularCoordinates() {
        return getData().getAngularCoordinates();
    }

    /** {@inheritDoc} */
    @Override
    public String getCenterName() {
        return getMetadata().getCenterName();
    }

    /** {@inheritDoc} */
    @Override
    public String getRefFrameAString() {
        return getMetadata().getRefFrameAString();
    }

    /** {@inheritDoc} */
    @Override
    public String getRefFrameBString() {
        return getMetadata().getRefFrameBString();
    }

    /** {@inheritDoc} */
    @Override
    public Frame getReferenceFrame() {
        return getMetadata().getReferenceFrame();
    }

    /** {@inheritDoc} */
    @Override
    public String getAttitudeDirection() {
        return getMetadata().getAttitudeDirection();
    }

    /** {@inheritDoc} */
    @Override
    public String getAttitudeType() {
        return getMetadata().getAttitudeType();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isFirst() {
        return getMetadata().isFirst();
    }

    /** {@inheritDoc} */
    @Override
    public RotationOrder getRotationOrder() {
        return getMetadata().getRotationOrder();
    }

    /** {@inheritDoc} */
    @Override
    public String getTimeScaleString() {
        return getMetadata().getTimeSystem().toString();
    }

    /** {@inheritDoc} */
    @Override
    public TimeScale getTimeScale() {
        return getMetadata().getTimeSystem().getTimeScale(conventions, dataContext.getTimeScales());
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getStart() {
        return getMetadata().getStart();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getStop() {
        return getMetadata().getStop();
    }

    /** {@inheritDoc} */
    @Override
    public String getInterpolationMethod() {
        return getMetadata().getInterpolationMethod();
    }

    /** {@inheritDoc} */
    @Override
    public int getInterpolationSamples() {
        return getMetadata().getInterpolationSamples();
    }

    /** {@inheritDoc} */
    @Override
    public AngularDerivativesFilter getAvailableDerivatives() {
        return getData().getAvailableDerivatives();
    }

}

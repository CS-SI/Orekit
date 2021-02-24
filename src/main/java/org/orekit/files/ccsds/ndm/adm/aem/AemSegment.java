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

import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.general.AttitudeEphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.TimeStampedAngularCoordinates;

/**
 * This class stores the metadata and data for one attitude segment.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class AemSegment extends Segment<AemMetadata, AemData>
    implements AttitudeEphemerisFile.AttitudeEphemerisSegment<TimeStampedAngularCoordinates> {

    /** Simple constructor.
     * @param metadata segment metadata
     * @param data segment data
     */
    public AemSegment(final AemMetadata metadata, final AemData data) {
        super(metadata, data);
    }

    /** {@inheritDoc} */
    @Override
    public List<TimeStampedAngularCoordinates> getAngularCoordinates() {
        return getData().getAngularCoordinates();
    }

    /** {@inheritDoc} */
    @Override
    public Frame getReferenceFrame() {
        final FrameFacade ff = getMetadata().getFrameA().asSpacecraftBodyFrame() == null ?
                               getMetadata().getFrameA() : getMetadata().getFrameB();
        return ff.asFrame();
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
        return getMetadata().getAttitudeType().getAngularDerivativesFilter();
    }

}

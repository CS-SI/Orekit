/* Copyright 2002-2021 CS GROUP
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.util.List;

import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.OrbitRelativeFrame;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.units.Unit;

/** Metadata for maneuver history.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class ManeuverHistoryMetadata extends CommentsContainer {

    /** Basis of this maneuver data. */
    private String manBasis;

    /** Reference frame of the maneuver. */
    private FrameFacade manReferenceFrame;

    /** Epoch of the maneuver reference frame. */
    private AbsoluteDate manFrameEpoch;

    /** Units of covariance element set. */
    private List<Unit> manUnits;

    /** Simple constructor.
     * @param epochT0 T0 epoch from file metadata
     */
    ManeuverHistoryMetadata(final AbsoluteDate epochT0) {
        // we don't call the setXxx() methods in order to avoid
        // calling refuseFurtherComments as a side effect
        manBasis            = "PLANNED";
        manReferenceFrame   = new FrameFacade(null, null,
                                              OrbitRelativeFrame.TNW_INERTIAL, null,
                                              OrbitRelativeFrame.TNW_INERTIAL.name());
        manFrameEpoch       = epochT0;
    }

    /** {@inheritDoc} */
    @Override
    public void checkMandatoryEntries() {
        super.checkMandatoryEntries();
        checkNotNull(manUnits, CovarianceHistoryMetadataKey.COV_UNITS);
    }

}

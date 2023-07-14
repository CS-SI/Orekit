/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.estimation.measurements.modifiers;

import org.hipparchus.geometry.euclidean.threed.Vector3D;

/** On-board antenna offset effect on phase measurements.
 * @author David Soulard
 * @since 10.2
 * @deprecated as of 12.0, replaced by {@link PhaseCentersPhaseModifier}
 */
@Deprecated
public class OnBoardAntennaPhaseModifier extends PhaseCentersPhaseModifier {

    /** Simple constructor.
     * @param antennaPhaseCenter position of the Antenna Phase Center in satellite frame
     */
    public OnBoardAntennaPhaseModifier(final Vector3D antennaPhaseCenter) {
        super(Vector3D.ZERO, null, antennaPhaseCenter, null);
    }

}


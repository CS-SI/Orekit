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
package org.orekit.files.rinex.observation;
import java.util.List;

import org.orekit.gnss.ObservationType;

/** Scale Factor to be applied.
 * Contains the scale factors of 10 applied to the data before
 * being stored into the RINEX file.
 * @since 12.0
 */
public class ScaleFactorCorrection {

    /** List of Observations types that have been scaled. */
    private final List<ObservationType> typesObsScaleFactor;

    /** Factor to divide stored observations with before use. */
    private final double scaleFactor;

    /** Simple constructor.
     * @param scaleFactor Factor to divide stored observations (1,10,100,1000)
     * @param typesObsScaleFactor List of Observations types that have been scaled
     */
    public ScaleFactorCorrection(final double scaleFactor,
                                 final List<ObservationType> typesObsScaleFactor) {
        this.scaleFactor = scaleFactor;
        this.typesObsScaleFactor = typesObsScaleFactor;
    }

    /** Get the Scale Factor.
     * @return Scale Factor
     */
    public double getCorrection() {
        return scaleFactor;
    }

    /** Get the list of Observation Types scaled.
     * @return List of Observation types scaled
     */
    public List<ObservationType> getTypesObsScaled() {
        return typesObsScaleFactor;
    }

}

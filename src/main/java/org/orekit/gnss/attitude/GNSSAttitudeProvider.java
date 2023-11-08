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
package org.orekit.gnss.attitude;

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.time.AbsoluteDate;

/**
 * Attitude providers for navigation satellites.
 *
 * <p>
 * The attitude mode is compliant with IGS conventions for
 * spacecraft frame, i.e. the +Z axis is towards Earth and
 * the +X axis is in the Sun direction. This may be different
 * from some manufacturers conventions, for example for
 * GPS blocks IIR/IIRM whose X axis convention is opposite.
 * </p>
 *
 * @author Luc Maisonobe
 * @since 9.2
 */
public interface GNSSAttitudeProvider extends AttitudeProvider {

    /** Get start of validity for this provider.
     * @return start of validity for this provider
     */
    AbsoluteDate validityStart();

    /** Get end of validity for this provider.
     * @return end of validity for this provider
     */
    AbsoluteDate validityEnd();

}

/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.utils.drivers;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.MathArrays;

import java.util.List;

/** Provider for {@link FieldParameterDriver parameters drivers}.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 14.0
 */
public interface FieldParameterDriversProvider<T extends CalculusFieldElement<T>>
    extends BaseParameterDriversProvider<FieldParameterDriver<T>, FieldParameterObserver<T>> {

    /** Get model parameters.
     * @return model parameters
     */
    default T[] getParameters() {
        final List<FieldParameterDriver<T>> drivers = getParametersDrivers();
        final T[] parameters = MathArrays.buildArray(drivers.getFirst().getValue().getField(), drivers.size());
        for (int i = 0; i < drivers.size(); ++i) {
            parameters[i] = drivers.get(i).getValue();
        }
        return parameters;
    }

}

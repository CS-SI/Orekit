/* Copyright 2002-2026 CS GROUP
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
package org.orekit.estimation;

import org.orekit.models.earth.atmosphere.HarrisPriester;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.utils.Constants;

public enum DSSTForce {

    ZONAL() {
        public DSSTForceModel getForceModel(Context context) {
            return new DSSTZonal(context.unnormalizedProvider, 4, 3, 9);
        }
    },

    TESSERAL() {
        public DSSTForceModel getForceModel(Context context) {
            return new DSSTTesseral(context.earth.getBodyFrame(), Constants.WGS84_EARTH_ANGULAR_VELOCITY, context.unnormalizedProvider, 4, 4, 4, 8, 4, 4, 2);
        }
    },

    THIRD_BODY_SUN() {
        public DSSTForceModel getForceModel(Context context) {
            return new DSSTThirdBody(context.sun, context.unnormalizedProvider.getMu());
        }
    },

    THIRD_BODY_MOON() {
        public DSSTForceModel getForceModel(Context context) {
            return new DSSTThirdBody(context.moon, context.unnormalizedProvider.getMu());
        }
    },

    DRAG() {
        public DSSTForceModel getForceModel(Context context) {
            return new DSSTAtmosphericDrag(new HarrisPriester(context.sun, context.earth), context.dragSensitive, context.unnormalizedProvider.getMu());
        }
    },

    SOLAR_RADIATION_PRESSURE() {
        public DSSTForceModel getForceModel(Context context) {
            return new DSSTSolarRadiationPressure(context.sun, context.earth,
                                                  context.radiationSensitive, context.unnormalizedProvider.getMu());
        }
    };

    public abstract DSSTForceModel getForceModel(Context context);
}

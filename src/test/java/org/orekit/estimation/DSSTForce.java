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
        public DSSTForceModel getForceModel(DSSTContext context) {
            return new DSSTZonal(context.gravity, 4, 3, 9);
        }
    },

    TESSERAL() {
        public DSSTForceModel getForceModel(DSSTContext context) {
            return new DSSTTesseral(context.earth.getBodyFrame(), Constants.WGS84_EARTH_ANGULAR_VELOCITY, context.gravity, 4, 4, 4, 8, 4, 4, 2);
        }
    },

    THIRD_BODY_SUN() {
        public DSSTForceModel getForceModel(DSSTContext context) {
            return new DSSTThirdBody(context.sun, context.gravity.getMu());
        }
    },

    THIRD_BODY_MOON() {
        public DSSTForceModel getForceModel(DSSTContext context) {
            return new DSSTThirdBody(context.moon, context.gravity.getMu());
        }
    },

    DRAG() {
        public DSSTForceModel getForceModel(DSSTContext context) {
            return new DSSTAtmosphericDrag(new HarrisPriester(context.sun, context.earth), context.dragSensitive, context.gravity.getMu());
        }
    },

    SOLAR_RADIATION_PRESSURE() {
        public DSSTForceModel getForceModel(DSSTContext context) {
            return new DSSTSolarRadiationPressure(context.sun, context.earth,
                                                  context.radiationSensitive, context.gravity.getMu());
        }
    };

    public abstract DSSTForceModel getForceModel(DSSTContext context);
}

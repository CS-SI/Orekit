/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.estimation;

import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.atmosphere.HarrisPriester;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.OceanTides;
import org.orekit.forces.gravity.Relativity;
import org.orekit.forces.gravity.SolidTides;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.radiation.SolarRadiationPressure;

public enum Force {

    POTENTIAL() {
        public ForceModel getForceModel(Context context) {
            return new HolmesFeatherstoneAttractionModel(context.earth.getBodyFrame(), context.gravity);
        }
    },

    THIRD_BODY_SUN() {
        public ForceModel getForceModel(Context context) {
            return new ThirdBodyAttraction(context.sun);
        }
    },

    THIRD_BODY_MOON() {
        public ForceModel getForceModel(Context context) {
            return new ThirdBodyAttraction(context.moon);
        }
    },

    DRAG() {
        public ForceModel getForceModel(Context context) {
            return new DragForce(new HarrisPriester(context.sun, context.earth), context.dragSensitive);
        }
    },

    SOLAR_RADIATION_PRESSURE() {
        public ForceModel getForceModel(Context context) {
            return new SolarRadiationPressure(context.sun, context.earth.getEquatorialRadius(),
                                              context.radiationSensitive);
        }
    },

    OCEAN_TIDES() {
        public ForceModel getForceModel(Context context) throws OrekitException {
            return new OceanTides(context.earth.getBodyFrame(), context.gravity.getAe(), context.gravity.getMu(),
                                  7, 7, context.conventions, context.ut1);
        }
    },

    SOLID_TIDES() {
        public ForceModel getForceModel(Context context) throws OrekitException {
            return new SolidTides(context.earth.getBodyFrame(), context.gravity.getAe(), context.gravity.getMu(),
                                  context.gravity.getTideSystem(),
                                  context.conventions, context.ut1, context.sun, context.moon);
        }
    },

    RELATIVITY() {
        public ForceModel getForceModel(Context context) {
            return new Relativity(context.gravity.getMu());
        }
    };

    public abstract ForceModel getForceModel(Context context) throws OrekitException;

}

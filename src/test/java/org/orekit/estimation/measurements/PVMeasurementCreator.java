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
package org.orekit.estimation.measurements;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.orekit.propagation.SpacecraftState;

public class PVMeasurementCreator extends MeasurementCreator {

    public void handleStep(final SpacecraftState currentState, final boolean isLast) {
        final Vector3D p = currentState.getPVCoordinates().getPosition();
        final Vector3D v = currentState.getPVCoordinates().getVelocity();
        final PV measurement = new PV(currentState.getDate(), p, v, 1.0, 0.001, 1.0);
        Assert.assertEquals(0.0, Vector3D.distance(p, measurement.getPosition()), 1.0e-10);
        Assert.assertEquals(0.0, Vector3D.distance(v, measurement.getVelocity()), 1.0e-10);
        addMeasurement(measurement);
    }

}

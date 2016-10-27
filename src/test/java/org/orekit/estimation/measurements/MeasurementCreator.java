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

import java.util.ArrayList;
import java.util.List;

import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;

public abstract class MeasurementCreator implements OrekitFixedStepHandler {

    private final List<ObservedMeasurement<?>> measurements;

    protected MeasurementCreator() {
        measurements = new ArrayList<ObservedMeasurement<?>>();
    }

    public List<ObservedMeasurement<?>> getMeasurements() {
        return measurements;
    }

    public void init(final SpacecraftState s0, final AbsoluteDate t, final double step) {
        measurements.clear();
    }

    protected void addMeasurement(final ObservedMeasurement<?> measurement) {
        measurements.add(measurement);
    }

}

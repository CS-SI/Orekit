/* Copyright 2002-2015 CS Systèmes d'Information
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

import java.util.Map;
import java.util.SortedSet;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Parameter;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


public class RangeRate extends AbstractMeasurement {

    private final TopocentricFrame station;
    
    public RangeRate(final TopocentricFrame station, final AbsoluteDate date, final double rangerate, final double sigma) {
        super(date,
              new double[] {
                            rangerate
        }, 
              new double[] {
                            sigma
        });
        this.station = station;
    }
    @Override
    public double[][] getPartialDerivatives(SpacecraftState state,
                                            Map<String, double[]> parameters)
        throws OrekitException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected double[] getTheoreticalValue(SpacecraftState state,
                                           SortedSet<Parameter> parameters)
        throws OrekitException {
        PVCoordinates scInStationFrame = state.getPVCoordinates(station);
        Vector3D lignevisee = scInStationFrame.getPosition().normalize();
        
        return new double[] {
                             lignevisee.dotProduct(scInStationFrame.getVelocity())
                             };
    }

}

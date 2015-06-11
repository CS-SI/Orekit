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

/** Class modeling a range measurement from a ground station.
 * @author Thierry Ceolin
 * @since 7.1
 */
public class RangeRate extends AbstractMeasurement {

    /** Ground station from which measurement is performed. */
    private final TopocentricFrame station;

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param rangeRate observed value
     * @param sigma theoretical standard deviation
     */
    public RangeRate(final TopocentricFrame station, final AbsoluteDate date,
                     final double rangeRate, final double sigma) {
        super(date,
              new double[] {
                  rangeRate
              }, new double[] {
                  sigma
              });
        this.station = station;
    }

    /** {@inheritDoc} */
    @Override
    public double[][] getPartialDerivatives(final SpacecraftState state,
                                            final Map<String, double[]> parameters)
        throws OrekitException {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected double[] getTheoreticalValue(final SpacecraftState state,
                                           final SortedSet<Parameter> parameters)
        throws OrekitException {
        final PVCoordinates scInStationFrame = state.getPVCoordinates(station);
        final Vector3D lineOfSight = scInStationFrame.getPosition().normalize();

        return new double[] {
            Vector3D.dotProduct(scInStationFrame.getVelocity(), lineOfSight)
        };

    }

}

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

import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.displacement.StationDisplacement;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.generation.FixedPointTleGenerationAlgorithm;
import org.orekit.propagation.conversion.TLEPropagatorBuilder;
import org.orekit.time.TimeScale;
import org.orekit.time.UT1Scale;
import org.orekit.utils.IERSConventions;

import java.util.List;
import java.util.Map;

public class TLEContext implements StationDataProvider {
    public IERSConventions                      conventions;
    public OneAxisEllipsoid                     earth;
    public CelestialBody                        sun;
    public CelestialBody                        moon;
    public NormalizedSphericalHarmonicsProvider gravity;
    public TimeScale                            utc;
    public UT1Scale                             ut1;
    public TLE                                  initialTLE;
    public StationDisplacement[]                displacements;
    public List<GroundStation>                  stations;
    // Stations for turn-around range
    // Map entry = primary station
    // Map value = secondary station associated
    public Map<GroundStation, GroundStation>     TARstations;

    public TLEPropagatorBuilder createBuilder(final double minStep, final double maxStep, final double dP) {

        final TLEPropagatorBuilder propagatorBuilder =
                        new TLEPropagatorBuilder(initialTLE, PositionAngleType.MEAN, dP,
                                                 new FixedPointTleGenerationAlgorithm());

        return propagatorBuilder;

    }

    GroundStation createStation(double latitudeInDegrees, double longitudeInDegrees,
                                double altitude, String name) {
        final GeodeticPoint gp = new GeodeticPoint(FastMath.toRadians(latitudeInDegrees),
                                                   FastMath.toRadians(longitudeInDegrees),
                                                   altitude);
        return new GroundStation(new TopocentricFrame(earth, gp, name),
                                 ut1.getEOPHistory(), displacements);
    }

    @Override
    public List<GroundStation> getStations() {
        return stations;
    }

}

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

import java.util.Arrays;
import java.util.List;

import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.EOPHistory;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.displacement.StationDisplacement;
import org.orekit.models.earth.displacement.TidalDisplacement;
import org.orekit.orbits.Orbit;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UT1Scale;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class EphemerisContext implements StationDataProvider {

    public IERSConventions                        conventions;
    public OneAxisEllipsoid                       earth;
    public CelestialBody                          sun;
    public CelestialBody                          moon;
    public UnnormalizedSphericalHarmonicsProvider gravity;
    public TimeScale                              utc;
    public UT1Scale                               ut1;
    public Orbit                                  initialOrbit;
    public StationDisplacement[]                  displacements;
    public List<GroundStation>                    stations;

    public EphemerisContext() {
        this.conventions = IERSConventions.IERS_2010;
        this.earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                          Constants.WGS84_EARTH_FLATTENING,
                                          FramesFactory.getITRF(conventions, true));
        final EOPHistory eopHistory = FramesFactory.getEOPHistory(conventions, true);
        this.ut1 = TimeScalesFactory.getUT1(eopHistory);
        this.displacements = new StationDisplacement[] {
                new TidalDisplacement(Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                      Constants.JPL_SSD_SUN_EARTH_PLUS_MOON_MASS_RATIO,
                                      Constants.JPL_SSD_EARTH_MOON_MASS_RATIO,
                                      CelestialBodyFactory.getSun(), CelestialBodyFactory.getMoon(),
                                      conventions, false)
                };
        this.stations = Arrays.asList(createStation(-53.05388,  -75.01551, 1750.0, "Isla Desolación"),
                                      createStation( 62.29639,   -7.01250,  880.0, "Slættaratindur"));

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

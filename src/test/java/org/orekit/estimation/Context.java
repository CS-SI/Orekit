/* Copyright 2002-2021 CS GROUP
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

import java.util.List;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.displacement.StationDisplacement;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.conversion.DormandPrince853IntegratorBuilder;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.TimeScale;
import org.orekit.time.UT1Scale;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class Context {
    public IERSConventions                      conventions;
    public OneAxisEllipsoid                     earth;
    public CelestialBody                        sun;
    public CelestialBody                        moon;
    public RadiationSensitive                   radiationSensitive;
    public DragSensitive                        dragSensitive;
    public NormalizedSphericalHarmonicsProvider gravity;
    public TimeScale                            utc;
    public UT1Scale                             ut1;
    public Orbit                                initialOrbit;
    public StationDisplacement[]                displacements;
    public List<GroundStation>                  stations;
    // Stations for turn-around range
    // Map entry = primary station
    // Map value = secondary station associated
    public Map<GroundStation, GroundStation>     TARstations;

    public NumericalPropagatorBuilder createBuilder(final OrbitType orbitType, final PositionAngle positionAngle,
                                                    final boolean perfectStart,
                                                    final double minStep, final double maxStep, final double dP,
                                                    final Force... forces) {

        final Orbit startOrbit;
        if (perfectStart) {
            // orbit estimation will start from a perfect orbit
            startOrbit = initialOrbit;
        } else {
            // orbit estimation will start from a wrong point
            final Vector3D initialPosition = initialOrbit.getPVCoordinates().getPosition();
            final Vector3D initialVelocity = initialOrbit.getPVCoordinates().getVelocity();
            final Vector3D wrongPosition   = initialPosition.add(new Vector3D(1000.0, 0, 0));
            final Vector3D wrongVelocity   = initialVelocity.add(new Vector3D(0, 0, 0.01));
            startOrbit                     = new CartesianOrbit(new PVCoordinates(wrongPosition, wrongVelocity),
                                                                initialOrbit.getFrame(),
                                                                initialOrbit.getDate(),
                                                                initialOrbit.getMu());
        }
        final NumericalPropagatorBuilder propagatorBuilder =
                        new NumericalPropagatorBuilder(orbitType.convertType(startOrbit),
                                                       new DormandPrince853IntegratorBuilder(minStep, maxStep, dP),
                                                       positionAngle, dP);
        for (Force force : forces) {
            propagatorBuilder.addForceModel(force.getForceModel(this));
        }

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

}

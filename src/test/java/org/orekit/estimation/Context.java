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

import java.util.List;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Pair;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.measurements.EarthBasedStation;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObserverSatellite;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.displacement.StationDisplacement;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.EquinoctialOrbitFactory;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.analytical.BrouwerLyddanePropagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.generation.FixedPointTleGenerationAlgorithm;
import org.orekit.propagation.conversion.BrouwerLyddanePropagatorBuilder;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.conversion.DormandPrince853IntegratorBuilder;
import org.orekit.propagation.conversion.EcksteinHechlerPropagatorBuilder;
import org.orekit.propagation.conversion.KeplerianPropagatorBuilder;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.conversion.TLEPropagatorBuilder;
import org.orekit.time.TimeScale;
import org.orekit.time.UT1Scale;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class Context implements StationDataProvider {

    /** IERS conventions. */
    public IERSConventions conventions;

    /** Central body shape. */
    public OneAxisEllipsoid earth;

    /** Sun model. */
    public CelestialBody sun;

    /** Moon model. */
    public CelestialBody moon;

    /** Radiation pressure model. */
    public RadiationSensitive radiationSensitive;

    /** Drag model. */
    public DragSensitive dragSensitive;

    /** Normalized spherical harmonics coefficients. */
    public NormalizedSphericalHarmonicsProvider normalizedProvider;

    /** Unnormalized spherical harmonics coefficients. */
    public UnnormalizedSphericalHarmonicsProvider unnormalizedProvider;

    /** UTC. */
    public TimeScale utc;

    /** UT1. */
    public UT1Scale ut1;

    /** Initial orbit. */
    public Orbit initialOrbit;

    /** Initial TLE for SGP4 orbit determination. */
    public TLE initialTLE;

    /** Reference points displacement models. */
    public StationDisplacement[] displacements;

    /** Ground stations for orbit determination. */
    public List<EarthBasedStation> stations;

    /** Stations for turn-around range.
     * Map entry = primary station
     * Map value = secondary station associated
     */
    public List<ObserverSatellite> satellites;

    /** Stations for turn-around range.
     * Map entry = primary station
     * Map value = secondary station associated
     */
    public Map<GroundStation, GroundStation> TARstations;

    /** Stations for bi-static range rate.
     * Map entry = primary station
     * Map value = secondary station associated
     */
    public Pair<GroundStation, GroundStation> BRRstations;

    /** Stations for TDOA.
     * Map entry = primary station
     * Map value = secondary station associated
     */
    public Pair<GroundStation, GroundStation> TDOAstations;

    /** Stations for FDOA.
     * Map entry = primary station
     * Map value = secondary station associated
     */
    public Pair<GroundStation, GroundStation> FDOAstations;

    /**
     * Creates a DSST propagator builder.
     * <p>
     *  By default propagation type and initial state type are defined as {@link PropagationType#MEAN MEAN}.
     * </p>
     * @param perfectStart if false, orbit estimation will start from a wrong point
     * @param minStep the minimum step size for numerical integration
     * @param maxStep the maximum step size for numerical integration
     * @param dP position error
     * @param forces the set of force models to include in the numerical propagator
     * @return a configured DSSTPropagatorBuilder instance
     */
    public DSSTPropagatorBuilder createDsst(final boolean perfectStart,
                                            final double minStep, final double maxStep, final double dP,
                                            final DSSTForce... forces) {
        return createDsst(PropagationType.MEAN, PropagationType.MEAN, perfectStart, minStep, maxStep, dP, forces);
    }

    /**
     * Creates a DSST propagator builder.
     *
     * @param propagationType type of the orbit used for the propagation (mean or osculating)
     * @param stateType  type of the elements used to define the initial orbital state (mean or osculating)
     * @param perfectStart if false, orbit estimation will start from a wrong point
     * @param minStep the minimum step size for numerical integration
     * @param maxStep the maximum step size for numerical integration
     * @param dP position error
     * @param forces the set of force models to include in the numerical propagator
     * @return a configured DSSTPropagatorBuilder instance
     */
    public DSSTPropagatorBuilder createDsst(final PropagationType propagationType,
                                            final PropagationType stateType, final boolean perfectStart,
                                            final double minStep, final double maxStep, final double dP,
                                            final DSSTForce... forces) {
        // Initialize builder
        final EquinoctialOrbit eo =
            (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(createInitialOrbit(perfectStart));
        final DSSTPropagatorBuilder propagatorBuilder =
                new DSSTPropagatorBuilder(new EquinoctialOrbitFactory(eo, dP, PositionAngleType.MEAN),
                                          new DormandPrince853IntegratorBuilder(minStep, maxStep, dP),
                                          propagationType, stateType);
        // Add force models
        for (DSSTForce force : forces) {
            propagatorBuilder.addForceModel(force.getForceModel(this));
        }
        // Return the configured builder
        return propagatorBuilder;
    }

    /**
     * Creates a numerical propagator builder.
     *
     * @param orbitType the type of orbit to be used by the builder
     * @param positionAngleType the position angle type to be used by the builder
     * @param perfectStart if false, orbit estimation will start from a wrong point
     * @param minStep the minimum step size for numerical integration
     * @param maxStep the maximum step size for numerical integration
     * @param dP position error
     * @param forces the set of force models to include in the numerical propagator
     * @return a configured NumericalPropagatorBuilder instance
     */
    public NumericalPropagatorBuilder createNumerical(final OrbitType orbitType, final PositionAngleType positionAngleType,
                                                      final boolean perfectStart,
                                                      final double minStep, final double maxStep, final double dP,
                                                      final Force... forces) {
        // Initialize builder
        final NumericalPropagatorBuilder propagatorBuilder =
                new NumericalPropagatorBuilder(orbitType.convertType(createInitialOrbit(perfectStart)).
                                                                   factory(positionAngleType, dP),
                                               new DormandPrince853IntegratorBuilder(minStep, maxStep, dP));
        // Add force models
        for (Force force : forces) {
            propagatorBuilder.addForceModel(force.getForceModel(this));
        }
        // Return the configured builder
        return propagatorBuilder;
    }

    /**
     * Creates a Keplerian propagator builder.
     *
     * @param angleType the position angle type to be used by the builder
     * @param perfectStart if false, orbit estimation will start from a wrong point
     * @param dP scaling factor used for orbital parameters normalization
     * @return a configured KeplerianPropagatorBuilder instance
     */
    public KeplerianPropagatorBuilder createKeplerian(final PositionAngleType angleType, final boolean perfectStart, final double dP) {
        // Return the configured builder
        return new KeplerianPropagatorBuilder(createInitialOrbit(perfectStart).factory(angleType, dP));
    }

    /**
     * Creates a Eckstein-Hechler propagator builder.
     *
     * @param angleType the position angle type to be used by the builder
     * @param perfectStart if false, orbit estimation will start from a wrong point
     * @param dP scaling factor used for orbital parameters normalization
     * @return a configured EcksteinHechlerPropagatorBuilder instance
     */
    public EcksteinHechlerPropagatorBuilder createEcksteinHechler(final PositionAngleType angleType, final boolean perfectStart, final double dP) {
        // Return the configured builder
        return new EcksteinHechlerPropagatorBuilder(createInitialOrbit(perfectStart).factory(angleType, dP),
                                                    unnormalizedProvider);
    }

    /**
     * Creates a Brouwer-Lyddane propagator builder.
     *
     * @param angleType the position angle type to be used by the builder
     * @param perfectStart if false, orbit estimation will start from a wrong point
     * @param dP scaling factor used for orbital parameters normalization
     * @return a configured BrouwerLyddanePropagatorBuilder instance
     */
    public BrouwerLyddanePropagatorBuilder createBrouwerLyddane(final PositionAngleType angleType, final boolean perfectStart, final double dP) {
        // Return the configured builder
        return new BrouwerLyddanePropagatorBuilder(createInitialOrbit(perfectStart).factory(angleType, dP),
                                                   unnormalizedProvider, BrouwerLyddanePropagator.M2);
    }

    /**
     * Creates a TLE propagator builder.
     *
     * @param dP scaling factor used for orbital parameters normalization
     * @return a configured TLEPropagatorBuilder instance
     */
    public TLEPropagatorBuilder createTleBuilder(final double dP) {
        return new TLEPropagatorBuilder(new FixedPointTleGenerationAlgorithm(initialTLE, PositionAngleType.MEAN, dP));
    }

    /**
     * Creates the initial orbit.
     *
     * @param perfectStart if false, orbit estimation will start from a wrong point
     * @return the initial orbit.
     */
    public Orbit createInitialOrbit(boolean perfectStart) {
        final Orbit startOrbit;
        if (perfectStart) {
            // orbit estimation will start from a perfect orbit
            startOrbit = initialOrbit;
        } else {
            // orbit estimation will start from a wrong point
            final Vector3D initialPosition = initialOrbit.getPosition();
            final Vector3D initialVelocity = initialOrbit.getVelocity();
            final Vector3D wrongPosition   = initialPosition.add(new Vector3D(1000.0, 0, 0));
            final Vector3D wrongVelocity   = initialVelocity.add(new Vector3D(0, 0, 0.01));
            startOrbit                     = new CartesianOrbit(new PVCoordinates(wrongPosition, wrongVelocity),
                                                                initialOrbit.getFrame(),
                                                                initialOrbit.getDate(),
                                                                initialOrbit.getMu());
        }
        return startOrbit;
    }

    /**
     * Creates a ground station given its geographic coordinates and name.
     *
     * @param latitudeInDegrees  the latitude of the station in degrees
     * @param longitudeInDegrees the longitude of the station in degrees
     * @param altitude           the altitude of the station in meters
     * @param name               the name of the station
     * @return a new instance of {@code GroundStation} created with the provided parameters
     */
    public EarthBasedStation createStation(double latitudeInDegrees, double longitudeInDegrees,
                                           double altitude, String name) {
        final GeodeticPoint gp = new GeodeticPoint(FastMath.toRadians(latitudeInDegrees),
                                                   FastMath.toRadians(longitudeInDegrees),
                                                   altitude);
        return new EarthBasedStation(new TopocentricFrame(earth, gp, name), ut1.getEOPHistory(), displacements);
    }

    ObserverSatellite createObserverSatellite(final Vector3D deltaPosition, final Vector3D deltaVelocity,
            final String name, final Force... forces) {

        final Vector3D initialPosition = initialOrbit.getPosition();
        final Vector3D initialVelocity = initialOrbit.getVelocity();
        final Vector3D finalPosition = initialPosition.add(deltaPosition);
        final Vector3D finalVelocity = initialVelocity.add(deltaVelocity);

        final Orbit orbit = new CartesianOrbit(new PVCoordinates(finalPosition, finalVelocity),
                initialOrbit.getFrame(),
                initialOrbit.getDate(),
                initialOrbit.getMu());

        final NumericalPropagatorBuilder propagatorBuilder =
            new NumericalPropagatorBuilder(orbit.factory(PositionAngleType.TRUE, 10.0),
                                           new DormandPrince853IntegratorBuilder(1.0, 5.0, 10.0));
        for (Force force : forces) {
            propagatorBuilder.addForceModel(force.getForceModel(this));
        }

        return new ObserverSatellite(name, propagatorBuilder.buildPropagator());
    }

    @Override
    public List<EarthBasedStation> getStations() {
        return stations;
    }

    @Override
    public List<ObserverSatellite> getSatellites() {
        return satellites;
    }

    @Override
    public OneAxisEllipsoid getEarth() {
        return earth;
    }

}

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
package org.orekit.propagation.conversion;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

public class EcksteinHechlerConverterTest {

    private Orbit orbit;
    private UnnormalizedSphericalHarmonicsProvider provider;

    @Test
    public void testConversionPositionVelocity() throws OrekitException {
        checkFit(orbit, 86400, 300, 1.0e-3, false, 1.699e-8);
    }

    @Test
    public void testConversionPositionOnly() throws OrekitException {
        checkFit(orbit, 86400, 300, 1.0e-3, true, 6.405e-8);
    }

    protected void checkFit(final Orbit orbit,
                            final double duration,
                            final double stepSize,
                            final double threshold,
                            final boolean positionOnly,
                            final double expectedRMS)
        throws OrekitException {

        // shift position by 3m
        CircularOrbit modified = new CircularOrbit(new TimeStampedPVCoordinates(orbit.getDate(),
                                                                                new Vector3D(1, orbit.getPVCoordinates().getPosition(),
                                                                                             3.0, Vector3D.PLUS_J),
                                                                                orbit.getPVCoordinates().getVelocity()),
                                                   orbit.getFrame(),
                                                   orbit.getMu());
        Propagator p = new EcksteinHechlerPropagator(modified, provider);
        List<SpacecraftState> sample = new ArrayList<SpacecraftState>();
        for (double dt = 0; dt < duration; dt += stepSize) {
            sample.add(p.propagate(modified.getDate().shiftedBy(dt)));
        }

        UnnormalizedSphericalHarmonics harmonics = provider.onDate(orbit.getDate());
        PropagatorBuilder builder = new EcksteinHechlerPropagatorBuilder(orbit,
                                                                         provider.getAe(),
                                                                         provider.getMu(),
                                                                         provider.getTideSystem(),
                                                                         harmonics.getUnnormalizedCnm(2, 0),
                                                                         harmonics.getUnnormalizedCnm(3, 0),
                                                                         harmonics.getUnnormalizedCnm(4, 0),
                                                                         harmonics.getUnnormalizedCnm(5, 0),
                                                                         harmonics.getUnnormalizedCnm(6, 0),
                                                                         OrbitType.CIRCULAR,
                                                                         PositionAngle.TRUE,
                                                                         1.0);

        FiniteDifferencePropagatorConverter fitter = new FiniteDifferencePropagatorConverter(builder,
                                                                                             threshold,
                                                                                             1000);

        fitter.convert(sample, positionOnly);

        Assert.assertEquals(expectedRMS, fitter.getRMS(), 0.01 * expectedRMS);

        EcksteinHechlerPropagator prop = (EcksteinHechlerPropagator)fitter.getAdaptedPropagator();
        Orbit fitted = prop.getInitialState().getOrbit();

        final double eps = 1.0e-12;
        Assert.assertEquals(modified.getPVCoordinates().getPosition().getX(),
                            fitted.getPVCoordinates().getPosition().getX(),
                            eps * modified.getPVCoordinates().getPosition().getX());
        Assert.assertEquals(modified.getPVCoordinates().getPosition().getY(),
                            fitted.getPVCoordinates().getPosition().getY(),
                            eps * modified.getPVCoordinates().getPosition().getY());
        Assert.assertEquals(modified.getPVCoordinates().getPosition().getZ(),
                            fitted.getPVCoordinates().getPosition().getZ(),
                            eps * modified.getPVCoordinates().getPosition().getZ());

        Assert.assertEquals(modified.getPVCoordinates().getVelocity().getX(),
                            fitted.getPVCoordinates().getVelocity().getX(),
                            eps * modified.getPVCoordinates().getVelocity().getX());
        Assert.assertEquals(modified.getPVCoordinates().getVelocity().getY(),
                            fitted.getPVCoordinates().getVelocity().getY(),
                            -eps * modified.getPVCoordinates().getVelocity().getY());
        Assert.assertEquals(modified.getPVCoordinates().getVelocity().getZ(),
                            fitted.getPVCoordinates().getVelocity().getZ(),
                            -eps * modified.getPVCoordinates().getVelocity().getZ());

    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data");

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);

         double mu  = 3.9860047e14;
         double ae  = 6.378137e6;
         double[][] cnm = new double[][] {
             { 0 }, { 0 }, { -1.08263e-3 }, { 2.54e-6 }, { 1.62e-6 }, { 2.3e-7 }, { -5.5e-7 }
            };
         double[][] snm = new double[][] {
             { 0 }, { 0 }, { 0 }, { 0 }, { 0 }, { 0 }, { 0 }
            };
         provider = GravityFieldFactory.getUnnormalizedProvider(ae, mu, TideSystem.UNKNOWN, cnm, snm);

        orbit = new EquinoctialOrbit(new PVCoordinates(new Vector3D(3220103., 69623., 6449822.),
                                                       new Vector3D(6414.7, -2006., -3180.)),
                                     FramesFactory.getEME2000(), initDate, mu);
    }

}


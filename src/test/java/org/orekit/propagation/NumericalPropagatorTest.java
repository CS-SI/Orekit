/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.propagation;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.ode.DormandPrince853Integrator;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


public class NumericalPropagatorTest extends TestCase {

    // Body mu
    private double mu;
    
    public NumericalPropagatorTest(String name) {
        super(name);
    }

    public void testNoExtrapolation() throws OrekitException {

        // Definition of initial conditions
        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                  Frame.getJ2000(), initDate, mu);


        // Extrapolator definition
        NumericalModel extrapolator =
            new NumericalModel(mu, new DormandPrince853Integrator(0.0, 10000.0,
                                                                       1.0e-8, 1.0e-8));

        // Extrapolation of the initial at the initial date
        SpacecraftState finalOrbit =
            extrapolator.propagate(new SpacecraftState(initialOrbit, mu),
                                   initialOrbit.getDate());

        // Initial orbit definition
        Vector3D initialPosition = initialOrbit.getPVCoordinates().getPosition();
        Vector3D initialVelocity = initialOrbit.getPVCoordinates().getVelocity();

        // Final orbit definition
        Vector3D finalPosition   = finalOrbit.getPVCoordinates().getPosition();
        Vector3D finalVelocity   = finalOrbit.getPVCoordinates().getVelocity();

        // Check results
        assertEquals(initialPosition.getX(), finalPosition.getX(), 1.0e-10);
        assertEquals(initialPosition.getY(), finalPosition.getY(), 1.0e-10);
        assertEquals(initialPosition.getZ(), finalPosition.getZ(), 1.0e-10);
        assertEquals(initialVelocity.getX(), finalVelocity.getX(), 1.0e-10);
        assertEquals(initialVelocity.getY(), finalVelocity.getY(), 1.0e-10);
        assertEquals(initialVelocity.getZ(), finalVelocity.getZ(), 1.0e-10);

    }

    public void testKepler() throws OrekitException {

        // Definition of initial conditions
        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                  Frame.getJ2000(), initDate, mu);

        // Extrapolator definition
        NumericalModel extrapolator =
            new NumericalModel(mu, new DormandPrince853Integrator(0.0, 10000.0,
                                                                       1.0e-8, 1.0e-8));
        double dt = 3200;

        // Extrapolation of the initial at t+dt
        SpacecraftState finalOrbit = 
            extrapolator.propagate(new SpacecraftState(initialOrbit),
                                   new AbsoluteDate(initialOrbit.getDate(), dt));

        // Check results
        double n = Math.sqrt(initialOrbit.getMu() / initialOrbit.getA()) / initialOrbit.getA();
        assertEquals(initialOrbit.getA(),    finalOrbit.getA(),    1.0e-10);
        assertEquals(initialOrbit.getEquinoctialEx(),    finalOrbit.getEquinoctialEx(),    1.0e-10);
        assertEquals(initialOrbit.getEquinoctialEy(),    finalOrbit.getEquinoctialEy(),    1.0e-10);
        assertEquals(initialOrbit.getHx(),    finalOrbit.getHx(),    1.0e-10);
        assertEquals(initialOrbit.getHy(),    finalOrbit.getHy(),    1.0e-10);
        assertEquals(initialOrbit.getLM() + n * dt, finalOrbit.getLM(), 4.0e-10);

    }

    public void setUp() {
        mu  = 3.9860047e14;
    }

    public void tearDown() {
        mu   = Double.NaN;
    }
    
    public static Test suite() {
        return new TestSuite(NumericalPropagatorTest.class);
    }

}


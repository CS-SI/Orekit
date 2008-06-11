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

import java.io.FileNotFoundException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.GraggBulirschStoerIntegrator;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.numerical.IntegratedEphemeris;
import org.orekit.propagation.numerical.NumericalModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


public class IntegratedEphemerisTest extends TestCase {

    public void testNormalKeplerIntegration() throws OrekitException, FileNotFoundException {

        // Definition of initial conditions with position and velocity

        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        double mu = 3.9860047e14;

        AbsoluteDate initDate = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 584.);
        SpacecraftState initialOrbit =
            new SpacecraftState(new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                     Frame.getJ2000(), initDate, mu));

        // Keplerian propagator definition

        KeplerianPropagator keplerEx = new KeplerianPropagator(initialOrbit);

        // Numerical propagator definition

        FirstOrderIntegrator integrator = new GraggBulirschStoerIntegrator(1, 86400, 0, 10e-13);
        NumericalModel numericEx = new NumericalModel(mu, integrator);

        // Integrated ephemeris

        IntegratedEphemeris ephemeris = new IntegratedEphemeris();

        // Propagation

        AbsoluteDate finalDate = new AbsoluteDate(initDate , 86400);
        numericEx.propagate(initialOrbit , finalDate , ephemeris );
        SpacecraftState keplerIntermediateOrbit;
        SpacecraftState numericIntermediateOrbit;
        AbsoluteDate intermediateDate;

        // tests

        for (int i = 1; i<=86400; i++) {
            intermediateDate = new AbsoluteDate(initDate , i);
            keplerIntermediateOrbit = keplerEx.propagate(intermediateDate);
            numericIntermediateOrbit = ephemeris.propagate(intermediateDate);

            Vector3D test = keplerIntermediateOrbit.getPVCoordinates().getPosition().subtract(numericIntermediateOrbit.getPVCoordinates().getPosition());
            assertEquals(0, test.getNorm(), 10e-2);
        }

        // test inv
        intermediateDate = new AbsoluteDate(initDate , 41589);
        keplerIntermediateOrbit = keplerEx.propagate(intermediateDate);
        initialOrbit = keplerEx.propagate(finalDate);
        numericEx.propagate(initialOrbit , initDate , ephemeris );
        numericIntermediateOrbit = ephemeris.propagate(intermediateDate);

        Vector3D test = keplerIntermediateOrbit.getPVCoordinates().getPosition().subtract(numericIntermediateOrbit.getPVCoordinates().getPosition());
        assertEquals(0, test.getNorm(), 10e-2);

    }

    public static Test suite() {
        return new TestSuite(IntegratedEphemerisTest.class);
    }
}

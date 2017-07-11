/* Copyright 2002-2017 CS Systèmes d'Information
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
package fr.cs.examples.propagation;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.AbstractParameterizable;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.inertia.InertialForces;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.numerical.FieldTimeDerivativesEquations;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

/** Swing-by trajectory about Jupiter compared in EME2000, ICRF and
 * Jupiter-centered inertial reference frame.
 *
 * <p>
 * Case 1:
 * <ul>
 * <li>Integration frame: EME2000</li>
 * <li>Representation of the trajectory: AbsolutePVCoordinates</li>
 * <li>No central body</li>
 * <li>ThirdBodyAttraction with Jupiter</li>
 * </ul>
 *
 * Case 2:
 * <ul>
 * <li>Integration frame: ICRF</li>
 * <li> Representation of the trajectory: AbsolutePVCoordinates</li>
 * <li>No central body</li>
 * <li>ThirdBodyAttraction with Jupiter</li>
 * </ul>
 *
 * Case 3:
 * <ul>
 * <li>Integration frame: Jupiter-centered inertial</li>
 * <li>Representation of the trajectory: Orbit</li>
 * <li>Central body: Jupiter</li>
 * </ul>
 *
 * All trajectories output in ICRF.
 *
 * @author Guillaume Obrecht
 *
 */

public class testCaseJupiter {

    @Test
    public static void main(String[] args) throws OrekitException {

        // configure Orekit
        File home       = new File(System.getProperty("user.home"));
        File orekitData = new File(home, "orekit-data");
        if (!orekitData.exists()) {
            System.err.format(Locale.US, "Failed to find %s folder%n",
                              orekitData.getAbsolutePath());
            System.err.format(Locale.US, "You need to download %s from the %s page and unzip it in %s for this tutorial to work%n",
                              "orekit-data.zip", "https://www.orekit.org/forge/projects/orekit/files",
                              home.getAbsolutePath());
            System.exit(1);
        }
        DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.addProvider(new DirectoryCrawler(orekitData));

        // Time settings
        final AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 0, 0, 00.000, TimeScalesFactory.getUTC());
        double integrationTime = 100000;
        double outputStep = 500;

        // Initial conditions
        final double x = 69911000 + 100000000;
        final double y = -2000000000;
        final double z = 0;
        final double Vx = 0;
        final double Vy = 50000;
        final double Vz = 0;
        final PVCoordinates initialScWrthJupiter = new PVCoordinates(new Vector3D(x,y,z), new Vector3D(Vx,Vy,Vz));

        // Integration parameters
        final double minStep = 0.001;
        final double maxstep = 120.0;
        final double positionTolerance = 100.0;
        final double initialStepSize = 1.0;

        // Load Celestial bodies
        final CelestialBody sun     = CelestialBodyFactory.getSun();
        final CelestialBody jupiter = CelestialBodyFactory.getJupiter();

        // Create frames to compare
        final Frame eme2000     = FramesFactory.getEME2000();
        final Frame icrf        = FramesFactory.getICRF();
        final Frame jovianFrame = jupiter.getInertiallyOrientedFrame();

        final Frame outputFrame = jovianFrame;

        // 1: Propagation in Earth-centered inertial reference frame

        final Frame integrationFrame1 = eme2000;

        System.out.print("1- Propagation in Earth-centered inertial reference frame (pseudo_inertial: " + integrationFrame1.isPseudoInertial() + ")\n");

        final Transform initialTransform1 = jovianFrame.getTransformTo(integrationFrame1, initialDate);
        final PVCoordinates initialConditions1 = initialTransform1.transformPVCoordinates(initialScWrthJupiter);

        final AbsolutePVCoordinates initialAbsPva1 = new AbsolutePVCoordinates(integrationFrame1, initialDate, initialConditions1);
        final double[][] tolerances1 = NumericalPropagator.tolerances(positionTolerance, initialAbsPva1);

        AdaptiveStepsizeIntegrator integrator1 =
                new DormandPrince853Integrator(minStep, maxstep, tolerances1[0], tolerances1[1]);
        integrator1.setInitialStepSize(initialStepSize);

        NumericalPropagator propagator1 = new NumericalPropagator(integrator1);
        propagator1.setOrbitType(null);  // propagate as absolute position-velocity-acceleration
        propagator1.setInitialState(new SpacecraftState(initialAbsPva1));
        propagator1.addForceModel(new InertialForces(icrf));
        propagator1.addForceModel(new ThirdBodyAttraction(jupiter));
        propagator1.setMasterMode(outputStep, new TutorialStepHandler("testJupiter1.txt", "jupiter1.txt", outputFrame, jupiter));

        SpacecraftState finalState1 = propagator1.propagate(initialDate.shiftedBy(integrationTime));
        final PVCoordinates pv1 = finalState1.getPVCoordinates(outputFrame);



        // 2: Propagation in Celestial reference frame

        final Frame integrationFrame2 = icrf;

        System.out.print("2- Propagation in Celestial reference frame (pseudo_inertial: " + integrationFrame2.isPseudoInertial() + ")\n");

        final Transform initialTransform2 = jovianFrame.getTransformTo(integrationFrame2, initialDate);
        final PVCoordinates initialConditions2 = initialTransform2.transformPVCoordinates(initialScWrthJupiter);

        final AbsolutePVCoordinates initialAbsPva2 = new AbsolutePVCoordinates(integrationFrame2, initialDate, initialConditions2);

        final double[][] tolerances2 = NumericalPropagator.tolerances(positionTolerance, initialAbsPva2);

        AdaptiveStepsizeIntegrator integrator2 =
                new DormandPrince853Integrator(minStep, maxstep, tolerances2[0], tolerances2[1]);
        integrator2.setInitialStepSize(initialStepSize);

        NumericalPropagator propagator2 = new NumericalPropagator(integrator2);
        propagator2.setOrbitType(null);  // propagate as absolute position-velocity-acceleration
        propagator2.setInitialState(new SpacecraftState(initialAbsPva2));
        propagator2.addForceModel(new AbsoluteBodyAttraction(jupiter));
        propagator1.addForceModel(new AbsoluteBodyAttraction(sun));
        propagator2.setMasterMode(outputStep, new TutorialStepHandler("testJupiter2.txt", "jupiter2.txt", outputFrame, jupiter));


        SpacecraftState finalState2 = propagator2.propagate(initialDate.shiftedBy(integrationTime));
        final PVCoordinates pv2 = finalState2.getPVCoordinates(outputFrame);



        // 3: Propagation in Jupiter centered frame: inertial, central body

        final Frame integrationFrame3 = jovianFrame;

        System.out.print("3- Propagation in Jupiter-centred reference frame (pseudo_inertial: " + integrationFrame3.isPseudoInertial() + ")\n");

        final Orbit initialOrbit3 = new CartesianOrbit(initialScWrthJupiter, integrationFrame3, initialDate, jupiter.getGM());

        final double[][] tolerances3 = NumericalPropagator.tolerances(positionTolerance, initialOrbit3, OrbitType.CARTESIAN);
        AdaptiveStepsizeIntegrator integrator3 =
                new DormandPrince853Integrator(minStep, maxstep, tolerances3[0], tolerances3[1]);
        integrator3.setInitialStepSize(initialStepSize);

        NumericalPropagator propagator3 = new NumericalPropagator(integrator3);
        propagator3.setOrbitType(OrbitType.CARTESIAN);     // propagate as regular orbit
        propagator3.setInitialState(new SpacecraftState(initialOrbit3));
        propagator3.addForceModel(new InertialForces(icrf));
        propagator3.addForceModel(new ThirdBodyAttraction(sun));
        propagator3.setMu(jupiter.getGM());
        propagator3.setMasterMode(outputStep, new TutorialStepHandler("testJupiter3.txt", "jupiter3.txt", outputFrame, jupiter));

        SpacecraftState finalState3 = propagator3.propagate(initialDate.shiftedBy(integrationTime));
        final PVCoordinates pv3 = finalState3.getPVCoordinates(outputFrame);


        // Compare final position
        final Vector3D pos1 = pv1.getPosition();
        final Vector3D pos2 = pv2.getPosition();
        final Vector3D pos3 = pv3.getPosition();

        final Vector3D diff13 = pos1.subtract(pos3);
        final Vector3D diff23 = pos2.subtract(pos3);
        final Vector3D diff12 = pos1.subtract(pos2);

        System.out.print("Errors from reference trajectory "
                + "(3: Jupiter-centered inertial frame) [m]\n");
        System.out.print("1/3: " + diff13 + "\n"
                + "   norm: "+ diff13.getNorm() + "\n");
        System.out.print("2/3: " + diff23 + "\n"
                        + "   norm: "+ diff23.getNorm() + "\n");
        System.out.print("1/2: " + diff12 + "\n"
                        + "   norm: "+ diff12.getNorm() + "\n");

        Assert.assertEquals(0.0, diff13.getNorm(), 1.05e5);
        Assert.assertEquals(0.0, diff23.getNorm(), 1.05e5);
        Assert.assertEquals(0.0, diff12.getNorm(), 3.0e3);

    }






private static class TutorialStepHandler implements OrekitFixedStepHandler {

        private PrintWriter file;

        private PrintWriter bodyFile;

        private Frame outputFrame;

        private CelestialBody centralBody;

        private TutorialStepHandler(final String fileName, final String bodyFileName, final Frame frame, final CelestialBody body) throws OrekitException {
            try {
                file = new PrintWriter(fileName);
                bodyFile = new PrintWriter(bodyFileName);
                outputFrame = frame;
                centralBody = body;
            } catch (IOException ioe) {
                throw new OrekitException(ioe, LocalizedCoreFormats.SIMPLE_MESSAGE, ioe.getLocalizedMessage());
            }
        }

        public void init(final SpacecraftState s0, final AbsoluteDate t) {
        }

        public void handleStep(SpacecraftState currentState, boolean isLast) {
            try {
                // Choose here in which reference frame to output the trajectory

                System.out.print(".");

                final PVCoordinates pv = currentState.getPVCoordinates(outputFrame);
                final AbsoluteDate d = currentState.getDate();
                final PVCoordinates bodyPV = centralBody.getPVCoordinates(d, outputFrame);

                file.format(Locale.US, "%12.6f %12.6f %12.6f %12.6f %12.6f %12.6f%n",
                            pv.getPosition().getX(),
                            pv.getPosition().getY(),
                            pv.getPosition().getZ(),
                            pv.getVelocity().getX(),
                            pv.getVelocity().getY(),
                            pv.getVelocity().getZ());


                bodyFile.format(Locale.US, "%12.6f %12.6f %12.6f %12.6f %12.6f %12.6f%n",
                            bodyPV.getPosition().getX(),
                            bodyPV.getPosition().getY(),
                            bodyPV.getPosition().getZ(),
                            bodyPV.getVelocity().getX(),
                            bodyPV.getVelocity().getY(),
                            bodyPV.getVelocity().getZ());

                if (isLast) {
                    final PVCoordinates finalPv = currentState.getPVCoordinates(outputFrame);
                    System.out.println();
                    System.out.format(Locale.US, "%s %12.0f %12.0f %12.0f %12.0f %12.0f %12.0f%n",
                            currentState.getDate(),
                            finalPv.getPosition().getX(),
                            finalPv.getPosition().getY(),
                            finalPv.getPosition().getZ(),
                            finalPv.getVelocity().getX(),
                            finalPv.getVelocity().getY(),
                            finalPv.getVelocity().getZ());
                    System.out.println();
                }
            } catch (OrekitException oe) {
                System.err.println(oe.getMessage());
            }
        }
    }

}

class AbsoluteBodyAttraction extends AbstractParameterizable implements ForceModel  {

    /** Attracting celestial body. */
    private CelestialBody body;

    /** Simple constructor.
     * @param body attracting celestial body
     */
    public AbsoluteBodyAttraction(final CelestialBody body) {
        this.body = body;
    }

    /** {@inheritDoc} */
    public void addContribution(final SpacecraftState s,
                                final TimeDerivativesEquations adder)
        throws OrekitException {

        final Vector3D bodyPosition = body.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
        final Vector3D satPosition  = s.getPVCoordinates().getPosition();
        final Vector3D delta        = bodyPosition.subtract(satPosition);
        final double r2             = delta.getNormSq();

        // compute absolute acceleration
        final Vector3D gamma = new Vector3D(body.getGM() / (r2 * FastMath.sqrt(r2)), delta);

        adder.addXYZAcceleration(gamma.getX(), gamma.getY(), gamma.getZ());

    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final AbsoluteDate date, final Frame frame,
                                                                      final FieldVector3D<DerivativeStructure> position,
                                                                      final FieldVector3D<DerivativeStructure> velocity,
                                                                      final FieldRotation<DerivativeStructure> rotation,
                                                                      final DerivativeStructure mass) throws OrekitException {
        // this method will be removed some time later
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final SpacecraftState s,
                                                                      final String paramName)
        throws OrekitException {
        // TODO implement this method
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventsDetectors() {
        return Stream.empty();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> void
        addContribution(FieldSpacecraftState<T> s,
                        FieldTimeDerivativesEquations<T> adder)
            throws OrekitException {
        // TODO implement this method
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>>
        getFieldEventsDetectors(Field<T> field) {
        return Stream.empty();
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getParametersDrivers() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver getParameterDriver(String name)
        throws OrekitException {
        // TODO Auto-generated method stub
        return null;
    }

}

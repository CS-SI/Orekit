/* Copyright 2002-2019 CS Systèmes d'Information
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
import java.util.Locale;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.orekit.attitudes.Attitude;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.SingleBodyAbsoluteAttraction;
import org.orekit.forces.inertia.InertialForces;
import org.orekit.frames.Frame;
import org.orekit.frames.L2Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinates;

/** The goal of this tutorial is to introduce users to orbital integration using SingleBodyAttraction. <br>
* This class should replace all the different kinds of 
* point mass interactions (ThirdBodyAttraction, NewtonianAttraction) in the future. <br> 
* Using SingleBodyAttraction and InertiaForces will enable a richer modelling, 
* allowing the user to compute the motion in a reference frame that is not necessarily
* centered on the main attractor and does not necessarily possess inertial axis.
* @since 10.0 
* @author Laurene Beauvalet
*/
public class PropagationInNonInertialFrame {
    public static void main(String[] args) throws OrekitException {
        
        // configure Orekit data provider
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
        final AbsoluteDate initialDate = new AbsoluteDate(2000, 01, 01, 0, 0, 00.000, TimeScalesFactory.getUTC());
        double integrationTime = 12000.;
        double outputStep = 600.0;

        // Initial conditions
        // We want to integrate our point from the L2 point, and with a null velocity relative to this point. 
        // The output is *very* sensitive to these conditions, as L2 point is unstable,
        // using slightly different initial conditions may give you huge differences in the trajectory.
        final PVCoordinates initialConditions = new PVCoordinates(new Vector3D(0.0, 0.0, 0.0),
                new Vector3D(0.0, 0.0, 0.0));
       
        // Load Bodies
        // We will integrate the motion of a spacecraft moving in the gravitationnal fields of the Earth and the Moon, 
        // the Earth-Moon barycenter is needed since it will be the center of the inertial frame that comes naturally 
        // from the considered bodies. 
        final CelestialBody earth = CelestialBodyFactory.getEarth();
        final CelestialBody moon  = CelestialBodyFactory.getMoon();
        final CelestialBody earthMoonBary = CelestialBodyFactory.getEarthMoonBarycenter();
        
        // Create frames
        // The motion of the spacecraft will be integrated in the rotating reference frame centered on the L2 point
        // of the Earth-Moon system. 
        final Frame l2Frame = new L2Frame(earth, moon);
        final Frame earthMoonBaryFrame = earthMoonBary.getInertiallyOrientedFrame();
       
        //final Frame inertiaFrame = earthMoonBaryFrame;
        final Frame integrationFrame = l2Frame;
        final Frame outputFrame = l2Frame;
        
       
        // We will now compute the propagation itself.     
        // We transform the coordinates from its initial frame to the integration frame. 
        // Here this step is superfluous since the coordinates are already given in the L2 frame, 
        // but we keep it as indication for the user.  
        final AbsolutePVCoordinates initialAbsPV =
            new AbsolutePVCoordinates(integrationFrame, initialDate,
                                      initialConditions);
        
        // Defining the satellite attitude
        Attitude arbitraryAttitude =
            new Attitude(integrationFrame,
                         new TimeStampedAngularCoordinates(initialDate,
                                                           new PVCoordinates(Vector3D.PLUS_I,
                                                                             Vector3D.PLUS_J),
                                                           new PVCoordinates(Vector3D.PLUS_I,
                                                                             Vector3D.PLUS_J)));
        
        // Creating the initial spacecraftstate that will be given to the propagator 
        final SpacecraftState initialState =
            new SpacecraftState(initialAbsPV, arbitraryAttitude);

        
        // Integration parameters
        // These parameters are used for the Dormand-Prince integrator, a variable step integrator, 
        // these limits prevent the integrator to spend too much time when the equations are too stiff, 
        // as well as the reverse situation.
        final double minStep = 0.001;
        final double maxstep = 3600.0;
        
        // tolerances for integrators
        // Used by the integrator to estimate its variable integration step
        final double positionTolerance = 0.001;
        final double velocityTolerance = 0.00001;
        final double massTolerance     = 1.0e-6;
        final double[] vecAbsoluteTolerances = {
            positionTolerance, positionTolerance, positionTolerance,
            velocityTolerance, velocityTolerance, velocityTolerance,
            massTolerance
        };
        final double[] vecRelativeTolerances = new double[vecAbsoluteTolerances.length];
        
        
        // Defining the numerical integrator that will be used by the propagator
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(minStep, maxstep,
                                           vecAbsoluteTolerances,
                                           vecRelativeTolerances);
        
        // Defining the propagator:
        //  *integrator, 
        //  *force models, 
        //  *initial spacecraftstate
        //  *mode: slave, master, ephemeris
        //  *step handler, to save the intermediary results of the integration in specified file
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(null);
        propagator.setIgnoreCentralAttraction(true);
        propagator.addForceModel(new InertialForces(earthMoonBaryFrame));
        propagator.addForceModel(new SingleBodyAbsoluteAttraction(earth));
        propagator.addForceModel(new SingleBodyAbsoluteAttraction(moon));
        propagator.setInitialState(initialState);
        propagator.setMasterMode(outputStep, new TutorialStepHandler(outputFrame));

        // The orbit propagation itself
        SpacecraftState finalState = propagator.propagate(initialDate.shiftedBy(integrationTime));
        final PVCoordinates pv = finalState.getPVCoordinates(outputFrame);
        System.out.println("conditions initiales: " + initialConditions);
        System.out.println("conditions finales: " + pv);
        
        
    }  
    
    //This step handler prints the result of the integration at each step, first the 
    //date of the step, then the position and velocity of the integrated spacecraft.  
    private static class TutorialStepHandler implements OrekitFixedStepHandler {
      
        
        private Frame outputFrame;
        private TutorialStepHandler( final Frame frame) {
        outputFrame = frame;
        }
        
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t, final double step) {
            System.out.format(Locale.US,
                                      "%s %s %s %s %s %s %s %s %s %s %n",
                                      "date", "                           X", "                 Y", 
                                      "                 Z", "                 Vx", "                Vy",
                                      "                Vz", "                ax", "                ay",
                                      "                az");
        }

        public void handleStep(SpacecraftState currentState, boolean isLast) {
            try {
//                final TimeScale utc = TimeScalesFactory.getUTC();
//                final AbsoluteDate initialDate = new AbsoluteDate(2000, 01, 01, 0, 0, 00.000,              
//                                                                  TimeScalesFactory.getUTC());
                final AbsoluteDate d = currentState.getDate();
                final PVCoordinates pv = currentState.getPVCoordinates(outputFrame);
                System.out.format(Locale.US,
                                  "%s %18.12f %18.12f %18.12f %18.12f %18.12f %18.12f %18.12f %18.12f %18.12f%n",
                                  d, pv.getPosition().getX(),
                                  pv.getPosition().getY(), pv.getPosition().getZ(),
                                  pv.getVelocity().getX(), pv.getVelocity().getY(),
                                  pv.getVelocity().getZ(), pv.getAcceleration().getX(),
                                  pv.getAcceleration().getY(),
                                  pv.getAcceleration().getZ()
                                  );

                if (isLast) {
                    final PVCoordinates finalPv =
                                    currentState.getPVCoordinates(outputFrame);
                    System.out.println();
                    System.out.format(Locale.US,
                                      "%s %12.0f %12.0f %12.0f %12.0f %12.0f %12.0f%n",
                                      d, finalPv.getPosition().getX(),
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
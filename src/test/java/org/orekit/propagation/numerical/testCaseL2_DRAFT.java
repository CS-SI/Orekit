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
package org.orekit.propagation.numerical;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.orekit.attitudes.Attitude;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.inertia.InertialForces;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinates;

import fr.cs.examples.Autoconfiguration;

/** 
 * 
 * @author Guillaume Obrecht
 *
 */
     
public class testCaseL2_DRAFT {

	public static void main(String[] args) throws OrekitException {
		
		// Configure Orekit
		Autoconfiguration.configureOrekit();
		
		// Time settings
		final AbsoluteDate initialDate = new AbsoluteDate(2000, 01, 01, 0, 0, 00.000, TimeScalesFactory.getUTC());
		double integrationTime = 5000000;
        double integrationStep = 50000;
		
		// Initial conditions 
        final double x = -2;
        final double y = -1;
        final double z = 0;
        final double Vx = 1000;
        final double Vy = -1000;
        final double Vz = 0;
        final PVCoordinates initialScWrtBary = new PVCoordinates(new Vector3D(x,y,z), new Vector3D(Vx,Vy,Vz));
        
        // Integration parameters
        final double minStep = 0.001;
        final double maxstep = 1000.0;
        final double positionTolerance = 10.0;
        final OrbitType propagationType = OrbitType.CARTESIAN;
		
		// Load Bodies
		final CelestialBody earth = CelestialBodyFactory.getEarth();
		final CelestialBody moon = CelestialBodyFactory.getMoon();
		final CelestialBody earthMoonBary = CelestialBodyFactory.getEarthMoonBarycenter();
		final double muEarth = earth.getGM();
		final double muMoon = moon.getGM();
		
		// Create frames to compare
		final Frame frame1 = FramesFactory.getEME2000();
		final Frame frame2 = FramesFactory.getICRF();
		final Frame frame3 = earthMoonBary.getBodyOrientedFrame();
		
		final Frame referenceFrame = frame1;
		final Frame outputFrame = frame3;
		
		// Compute position of L2
		final Vector3D posMoon = moon.getPVCoordinates(initialDate, frame3).getPosition();
		final Vector3D posEarth = earth.getPVCoordinates(initialDate, frame3).getPosition();
		final double r2 = posMoon.getNorm();
		
		final Vector3D earthToMoon = (posMoon.subtract(posEarth));
		final double R = earthToMoon.getNorm();
		final Vector3D unitVector = earthToMoon.normalize();

		final double q = moon.getGM() / earth.getGM();
		final double epsilon = Math.pow(q/3, 1/3);
		
		final double L2 = r2 + R * (-epsilon + 1/3 * Math.pow(epsilon, 2)) + 1/9 * Math.pow(epsilon, 3);
		
		final Vector3D posL2 = new Vector3D(L2,unitVector);
		System.out.print(posL2.getX() + ", " + posL2.getY() + ", " + posL2.getZ() +"\n");
		
		
		// Initial position
		Vector3D initialPosition = posL2.add( new Vector3D(x,y,z) );
		final PVCoordinates initialPV = new PVCoordinates(initialPosition, new Vector3D(Vx,Vy,Vz));
		
		
		final Orbit referenceOrbit = new CartesianOrbit(initialScWrtBary, referenceFrame, initialDate, muEarth);

		
        // 1: Propagation in Earth-centered inertial reference frame
        
        final Frame integrationFrame1 = frame1;
        
        System.out.print("1- Propagation in Earth-centered inertial reference frame (pseudo_inertial: " + integrationFrame1.isPseudoInertial() + ")\n");
		
		final org.orekit.frames.Transform initialTransform1 = frame3.getTransformTo(integrationFrame1, initialDate);
        final PVCoordinates initialConditions1 = initialTransform1.transformPVCoordinates(initialPV);
        
        final CartesianOrbit initialOrbit1 = new CartesianOrbit(initialConditions1, integrationFrame1, initialDate, muEarth);
        final SpacecraftState initialState1 = new SpacecraftState(initialOrbit1);

		final double[][] tolerances1 = NumericalPropagator.tolerances(positionTolerance, initialOrbit1, propagationType);
		
		AdaptiveStepsizeIntegrator integrator1 =
				new DormandPrince853Integrator(minStep, maxstep, tolerances1[0], tolerances1[1]);

		NumericalPropagator propagator1 = new NumericalPropagator(integrator1);
		propagator1.setOrbitType(propagationType);            
		propagator1.setInitialState(initialState1);

		propagator1.setMasterMode(integrationStep, new TutorialStepHandler("L2sc1.txt", "L2earth1.txt", "L2moon1.txt", outputFrame, earth, moon));

		final ForceModel moonAttraction1 = new ThirdBodyAttraction(moon);
    	propagator1.addForceModel(moonAttraction1);
		
		SpacecraftState finalState1 = propagator1.propagate(initialDate.shiftedBy(integrationTime));
		final PVCoordinates pv1 = finalState1.getPVCoordinates(outputFrame);
		
		
		
		// 2: Propagation in Celestial reference frame
		
		final Frame integrationFrame2 = frame2;
        
        System.out.print("2- Propagation in Celestial reference frame (pseudo_inertial: " + integrationFrame2.isPseudoInertial() + ")\n");
		
        final org.orekit.frames.Transform initialTransform2 = frame3.getTransformTo(integrationFrame2, initialDate);
        final PVCoordinates initialConditions2 = initialTransform1.transformPVCoordinates(initialPV);
		
        final AbsolutePVCoordinates initialOrbit2 = new AbsolutePVCoordinates(integrationFrame2, initialDate, initialConditions2);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit2);

		final double[][] tolerances2 = NumericalPropagator.tolerances(positionTolerance, referenceOrbit, propagationType);
		
		AdaptiveStepsizeIntegrator integrator2 =
				new DormandPrince853Integrator(minStep, maxstep, tolerances2[0], tolerances2[1]);

		NumericalPropagator propagator2 = new NumericalPropagator(integrator2);
		propagator2.setOrbitType(propagationType);            
		propagator2.setInitialState(initialState);

		propagator2.setMasterMode(integrationStep, new TutorialStepHandler("L2sc2.txt", "L2earth2.txt", "L2moon2.txt", outputFrame, earth, moon));

		final ForceModel earthAttraction2 = new ThirdBodyAttraction(moon);
		final ForceModel moonAttraction2 = new ThirdBodyAttraction(moon);
		propagator2.addForceModel(earthAttraction2);
    	propagator2.addForceModel(moonAttraction2);
		
		SpacecraftState finalState2 = propagator2.propagate(initialDate.shiftedBy(integrationTime));
		final PVCoordinates pv2 = finalState2.getPVCoordinates(outputFrame);
		
		
		
		// 3: Propagation in barycentric frame
		// TODO: replace by frame centered on L2		
		final Frame integrationFrame3 = frame3;
		
		System.out.print("3- Propagation in barycentric reference frame (pseudo_inertial: " + integrationFrame3.isPseudoInertial() + ")\n");
		
		final PVCoordinates initialConditions3 = initialPV;
        
        final AbsolutePVCoordinates initialOrbit3 = new AbsolutePVCoordinates(integrationFrame3, initialDate, initialConditions3);
        Attitude arbitraryAttitude3 = new Attitude( integrationFrame3, 
				new TimeStampedAngularCoordinates( initialDate, new PVCoordinates( Vector3D.PLUS_I, Vector3D.ZERO ), new PVCoordinates( Vector3D.PLUS_I, Vector3D.ZERO )) );
		final SpacecraftState initialState3 = new SpacecraftState(initialOrbit3, arbitraryAttitude3);
        
        
        final double[][] tolerances3 = NumericalPropagator.tolerances(positionTolerance, referenceOrbit, propagationType);
        AdaptiveStepsizeIntegrator integrator3 =
                new DormandPrince853Integrator(minStep, maxstep, tolerances3[0], tolerances3[1]);
        
        NumericalPropagator propagator3 = new NumericalPropagator(integrator3);
        propagator3.setOrbitType(propagationType);            
        propagator3.setInitialState(initialState3);
        
        final ForceModel earthAttraction3 = new ThirdBodyAttraction(moon);
		final ForceModel moonAttraction3 = new ThirdBodyAttraction(moon);
		propagator3.addForceModel(earthAttraction3);
    	propagator3.addForceModel(moonAttraction3);
    	
    	final ForceModel inertial = new InertialForces(referenceFrame);
    	propagator3.addForceModel(inertial);
    	
        propagator3.setMasterMode(integrationStep, new TutorialStepHandler("L2sc3.txt", "L2earth3.txt", "L2moon3.txt", outputFrame, earth, moon));
        
        SpacecraftState finalState3 = propagator3.propagate(initialDate.shiftedBy(integrationTime));
        final PVCoordinates pv3 = finalState3.getPVCoordinates(outputFrame);
        
        
        // Compare final position
        final Vector3D pos1 = pv1.getPosition();
        final Vector3D pos2 = pv2.getPosition();
        final Vector3D pos3 = pv3.getPosition();
        
        final Vector3D diff1 = pos1.subtract(pos3);
        final Vector3D diff2 = pos2.subtract(pos3);
        
        double ratio1[] = new double[3];
        ratio1[0] = diff1.getX() / pos3.getX() * 100;
        ratio1[1] = diff1.getY() / pos3.getY() * 100;
        ratio1[2] = diff1.getZ() / pos3.getZ() * 100;
        
        double ratio2[] = new double[3];
        ratio2[0] = diff2.getX() / pos3.getX() * 100;
        ratio2[1] = diff2.getY() / pos3.getY() * 100;
        ratio2[2] = diff2.getZ() / pos3.getZ() * 100;

        
        System.out.print("Errors from reference trajectory "
        		+ "(3: Jupiter-centered inertial frame) [m]\n");
        System.out.print("1: " + diff1 + "\n"
        		+ "   norm: "+ diff1.getNorm() + "\n");
        System.out.print("2: " + diff2 + "\n"
        		+ "   norm: "+ diff2.getNorm() + "\n");
	}
	
	
	
	
	
	
private static class TutorialStepHandler implements OrekitFixedStepHandler {
    	
    	private PrintWriter file;
    	
    	private PrintWriter body1File;
    	
    	private PrintWriter body2File;
    	
    	private Frame outputFrame;
    	
    	private CelestialBody body1;
    	
    	private CelestialBody body2;

        private TutorialStepHandler(final String fileName, final String body1FileName, final String body2FileName, final Frame frame, final CelestialBody body1_, final CelestialBody body2_) 
        		throws OrekitException {
            try {
                file = new PrintWriter(fileName);
                body1File = new PrintWriter(body1FileName);
                body2File = new PrintWriter(body2FileName);
                outputFrame = frame;
                body1 = body1_;
                body2 = body2_;
            } catch (IOException ioe) {
                throw new OrekitException(ioe, LocalizedFormats.SIMPLE_MESSAGE, ioe.getLocalizedMessage());
            }
        }

        public void init(final SpacecraftState s0, final AbsoluteDate t) {
        }

        public void handleStep(SpacecraftState currentState, boolean isLast) {
        	try {
                // Choose here in which reference frame to output the trajectory
                
        		System.out.print(".");
        		
        		final AbsoluteDate d = currentState.getDate();
        		final PVCoordinates pv = currentState.getPVCoordinates(outputFrame);
        		final PVCoordinates bodyPV1 = body1.getPVCoordinates(d, outputFrame);
        		final PVCoordinates bodyPV2 = body2.getPVCoordinates(d, outputFrame);


        		
        			
        			file.format(Locale.US, "%12.6f %12.6f %12.6f %12.6f %12.6f %12.6f%n",
        					pv.getPosition().getX(),
        					pv.getPosition().getY(),
        					pv.getPosition().getZ(),
        					pv.getVelocity().getX(),
        					pv.getVelocity().getY(),
        					pv.getVelocity().getZ());
            			
        		

        			body1File.format(Locale.US, "%12.6f %12.6f %12.6f %12.6f %12.6f %12.6f%n",
        					bodyPV1.getPosition().getX(),
        					bodyPV1.getPosition().getY(),
        					bodyPV1.getPosition().getZ(),
        					bodyPV1.getVelocity().getX(),
        					bodyPV1.getVelocity().getY(),
        					bodyPV1.getVelocity().getZ());
            			
        		

        		body2File.format(Locale.US, "%12.6f %12.6f %12.6f %12.6f %12.6f %12.6f%n",
        					bodyPV2.getPosition().getX(),
        					bodyPV2.getPosition().getY(),
        					bodyPV2.getPosition().getZ(),
        					bodyPV2.getVelocity().getX(),
        					bodyPV2.getVelocity().getY(),
        					bodyPV2.getVelocity().getZ());
            			


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

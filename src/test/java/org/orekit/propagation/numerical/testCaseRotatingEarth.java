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
import org.junit.Assert;
import org.junit.Test;
import org.orekit.attitudes.Attitude;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.inertia.InertialForces;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Predefined;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinates;

import fr.cs.examples.Autoconfiguration;

/** Compared propagation of a LEO in Earth-centered inertial and
 * non-inertial frames: EME2000 and ITRF.
 * <p>
 * All trajectories output in EME2000.
 * @author Guillaume Obrecht
 *
 */
public class testCaseRotatingEarth {

	/**
	 * @param args
	 */
	@Test
	public static void main(String[] args) {
		try {

			// configure Orekit
			Autoconfiguration.configureOrekit();

			// gravitation coefficient
			double mu =  3.986004415e+14;

			final Frame inertialFrame = FramesFactory.getEME2000();
			final Frame nonInertialFrame = FramesFactory.getFrame(Predefined.ITRF_CIO_CONV_2010_SIMPLE_EOP);
			final Frame outputFrame = inertialFrame;

			// Initial date
			AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, TimeScalesFactory.getUTC());

			// Initial orbit 
			double x = 6371000 + 300000; 
			final double y = 0;
			final double z = 1000;
			double Vx = 0;
			double Vy = 7800;
			double Vz = 0;

			// Integrator parameters
			double integrationStep = 30;
			final double minStep = 0.001;
			final double maxstep = 1000.0;
			final double positionTolerance = 0.001;
			final OrbitType propagationType = OrbitType.CARTESIAN;

			// Convert initial vector to integration frame
			final Vector3D initialPosition = new Vector3D(x,y,z);
			final Vector3D initialVelocity = new Vector3D(Vx,Vy,Vz);
			final PVCoordinates initialPV = new PVCoordinates(initialPosition, initialVelocity);
			
			final org.orekit.frames.Transform initialTransform = inertialFrame.getTransformTo(nonInertialFrame, initialDate);
			final PVCoordinates rotatedInitialPV = initialTransform.transformPVCoordinates(initialPV);

			// Get orbital period
			final Orbit referenceOrbit = new KeplerianOrbit(initialPV, inertialFrame, initialDate, mu);
			final double integrationTime = referenceOrbit.getKeplerianPeriod();

			

			// Integration in inertial Earth-centered frame

			final Frame integrationFrame1 = inertialFrame;
			
			System.out.print("1- Propagation in EME2000 (pseudo_inertial: " + integrationFrame1.isPseudoInertial() + ")\n");

			// Define orbits with and without central body
			final Orbit initialOrbit1 = new CartesianOrbit(initialPV, integrationFrame1, initialDate, mu);

			// Initial spacecraft state definition
			SpacecraftState initialState1 = new SpacecraftState(initialOrbit1);

			// Create integrator
			double[][] tolerances1 = NumericalPropagator.tolerances(positionTolerance, initialOrbit1, propagationType);
			AdaptiveStepsizeIntegrator integrator =
					new DormandPrince853Integrator(minStep, maxstep, tolerances1[0], tolerances1[1]);

			// Create propagator
			NumericalPropagator propagator1 = new NumericalPropagator(integrator);
			propagator1.setOrbitType(propagationType); 
			propagator1.setInitialState(initialState1);
			propagator1.setMasterMode(integrationStep, new TutorialStepHandler("testEarth1.txt", outputFrame));
			
			// Propagation
			SpacecraftState finalState1 = propagator1.propagate(initialDate.shiftedBy(integrationTime));
			final PVCoordinates pv1 = finalState1.getPVCoordinates(outputFrame);



			// Integration in rotating (non-inertial) Earth-centered frame

			final Frame integrationFrame2 = nonInertialFrame;
			
			System.out.print("2- Propagation in ITRF (pseudo_inertial: " + integrationFrame2.isPseudoInertial() + ")\n");
			
			// Define orbits with and without central body
			final Orbit initialOrbit2 = new CartesianOrbit(rotatedInitialPV, integrationFrame2, initialDate, mu);
			
			// Initial spacecraft state definition
			// Arbitrary attitude to define SpacecraftState (not yet modified to provide default attitude in non-inertial frames)
			Attitude arbitraryAttitude2 = new Attitude( integrationFrame2, 
					new TimeStampedAngularCoordinates( initialDate, new PVCoordinates( Vector3D.PLUS_I, Vector3D.ZERO ), new PVCoordinates( Vector3D.PLUS_I, Vector3D.ZERO )) );

			SpacecraftState initialState2 = new SpacecraftState(initialOrbit2, arbitraryAttitude2);

			// Create integrator
			double[][] tolerances2 = NumericalPropagator.tolerances(positionTolerance, initialOrbit2, propagationType);
			AdaptiveStepsizeIntegrator integrator2 =
					new DormandPrince853Integrator(minStep, maxstep, tolerances2[0], tolerances2[1]);

			// Create propagator
			NumericalPropagator propagator2 = new NumericalPropagator(integrator2);
			propagator2.setOrbitType(propagationType); 
			propagator2.setInitialState(initialState2);
			propagator2.setMasterMode(integrationStep, new TutorialStepHandler("testEarth2.txt", outputFrame));

			// Inertial force model
			
			ForceModel inertia = new InertialForces(inertialFrame);
			propagator2.addForceModel(inertia);
			
			// Propagation
			SpacecraftState finalState2 = propagator2.propagate(initialDate.shiftedBy(integrationTime));
			final PVCoordinates pv2 = finalState2.getPVCoordinates(outputFrame);
			
			
			
			// Compare final position
	        final Vector3D pos1 = pv1.getPosition();
	        final Vector3D pos2 = pv2.getPosition();
	        
	        final Vector3D diff = pos2.subtract(pos1);
	        
	        double ratio1[] = new double[3];
	        ratio1[0] = diff.getX() / pos1.getX() * 100;
	        ratio1[1] = diff.getY() / pos1.getY() * 100;
	        ratio1[2] = diff.getZ() / pos1.getZ() * 100;
	        
	        System.out.print("Errors of trajectory 2 wrt trajectory 1 [m]\n");
	        System.out.print(diff + "\n"
	        		+ "   norm: "+ diff.getNorm() + "\n");
	        
	        Assert.assertEquals(0.0, Vector3D.distance(pos1, pos2), 1.56e-3);

		} catch (OrekitException oe) {
			System.err.println(oe.getMessage());
		}
	}



	/** Specialized step handler.
	 * <p>This class extends the step handler in order to print on the output stream at the given step.<p>
	 * @author Pascal Parraud
	 */
	private static class TutorialStepHandler implements OrekitFixedStepHandler {

		private PrintWriter out;

		private Frame outputFrame;

		private TutorialStepHandler(final String file, final Frame frame) throws OrekitException {
		    try {
		        out = new PrintWriter(file);
		        outputFrame = frame;
		    } catch (IOException ioe) {
		        throw new OrekitException(ioe, LocalizedFormats.SIMPLE_MESSAGE, ioe.getLocalizedMessage());
		    }
		}

		public void init(final SpacecraftState s0, final AbsoluteDate t) {
		}

		public void handleStep(SpacecraftState currentState, boolean isLast) {
			try {

				System.out.print(".");

				final PVCoordinates pv = currentState.getPVCoordinates(outputFrame);

				out.format(Locale.US, "%12.6f %12.6f %12.6f %12.6f %12.6f %12.6f%n",
				           pv.getPosition().getX(),
				           pv.getPosition().getY(),
				           pv.getPosition().getZ(),
				           pv.getVelocity().getX(),
				           pv.getVelocity().getY(),
				           pv.getVelocity().getZ());


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

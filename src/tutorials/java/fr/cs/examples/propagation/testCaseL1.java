package fr.cs.examples.propagation;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import org.hipparchus.exception.LocalizedCoreFormats;
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
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.inertia.InertialForces;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.L1Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinates;

/**
*
* @author Luc Maisonabe
* @author Julio Hernanz
*/
public class testCaseL1 {

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
          final AbsoluteDate initialDate = new AbsoluteDate(2000, 01, 01, 0, 0, 00.000, TimeScalesFactory.getUTC());
          double integrationTime = 5.0e6;
          double outputStep = 600.0;

          // Initial conditions
          // The output is *very* sensitive to these conditions, as L1 point in unstable

          // with these initial conditions, we have a trajectory starting below L1 and not fast
          // enough to stay there, so it falls back towards Earth/Moon and first makes several
          // loops around Moon before falling to Earth and starting looping around Earth
//          final PVCoordinates initialPVInL1 = new PVCoordinates(new Vector3D(-1420966.0, 16.0, 26880.0),
//                                                                new Vector3D(0.0, 11.856, -0.001));

//          // with these initial conditions, Earth-centered propagations fall back to Earth,
//          // while L2-centered propagation remains near Moon until the end
//          final PVCoordinates initialPVInL1 = new PVCoordinates(new Vector3D(-1420966.0, 16.0, 26880.0),
//                                                                new Vector3D(0.0, 26.7, -0.001));

//          // with these initial conditions, Earth-centered propagations remain near Moon until the end,
//          // while L2-centered propagation falls back to Earth
//          final PVCoordinates initialPVInL1 = new PVCoordinates(new Vector3D(-1420966.0, 16.0, 26880.0),
//                                                                new Vector3D(0.0, 26.8, -0.001));

//          // with these initial conditions, Earth-centered propagations leave the Earth/Moon system immediately,
//          // while L2-centered propagation falls back to Earth
//          final PVCoordinates initialPVInL1 = new PVCoordinates(new Vector3D(-1420966.0, 16.0, 26880.0),
//                                                                new Vector3D(0.0, 26.9, -0.001));

//          // with these initial conditions, all propagations leave the Earth/Moon system immediately
          final PVCoordinates initialPVInL1 = new PVCoordinates(new Vector3D(-1420966.0, 16.0, 26880.0),
                                                                new Vector3D(0.0, 27.0, -0.001));

          // Integration parameters
          final double minStep = 0.001;
          final double maxstep = 3600.0;

          // Load Bodies
          final CelestialBody earth = CelestialBodyFactory.getEarth();
          final CelestialBody moon  = CelestialBodyFactory.getMoon();
          final CelestialBody earthMoonBary = CelestialBodyFactory.getEarthMoonBarycenter();
          final double muEarth = earth.getGM();

          // Create frames to compare
          final Frame eme2000 = FramesFactory.getEME2000();
          final Frame gcrf = FramesFactory.getGCRF();
          final Frame l1Frame = new L1Frame(earth, moon);
          final Frame earthMoonBaryFrame = earthMoonBary.getInertiallyOrientedFrame();
          final Frame outputFrame = l1Frame;

          // tolerances for integrators
          final double positionTolerance = 10.0;
          final double velocityTolerance = 0.01;
          final double massTolerance     = 1.0e-6;
          final double[] vecAbsoluteTolerances = {
              positionTolerance, positionTolerance, positionTolerance,
              velocityTolerance, velocityTolerance, velocityTolerance,
              massTolerance
          };
          final double[] vecRelativeTolerances = new double[vecAbsoluteTolerances.length];

          // 1: Propagation in Earth-centered inertial reference frame

          final Frame integrationFrame1 = eme2000;

          System.out.println("1- Propagation in Earth-centered inertial reference frame (pseudo_inertial: " +
                             integrationFrame1.isPseudoInertial() + ")");

          final PVCoordinates initialConditions1 = l1Frame.getTransformTo(integrationFrame1, initialDate).
                                                   transformPVCoordinates(initialPVInL1);

          final CartesianOrbit initialOrbit1 = new CartesianOrbit(initialConditions1, integrationFrame1,
                                                                  initialDate, muEarth);
          final SpacecraftState initialState1 = new SpacecraftState(initialOrbit1);


          AdaptiveStepsizeIntegrator integrator1 =
                  new DormandPrince853Integrator(minStep, maxstep, vecAbsoluteTolerances, vecRelativeTolerances);

          NumericalPropagator propagator1 = new NumericalPropagator(integrator1);
          propagator1.setOrbitType(OrbitType.CARTESIAN);
          propagator1.addForceModel(new ThirdBodyAttraction(moon));
          propagator1.setInitialState(initialState1);
          propagator1.setMasterMode(outputStep, new TutorialStepHandler("testL1_1.txt", outputFrame));

          SpacecraftState finalState1 = propagator1.propagate(initialDate.shiftedBy(integrationTime));
          final PVCoordinates pv1 = finalState1.getPVCoordinates(outputFrame);


          // 2: Propagation in Celestial reference frame

          final Frame integrationFrame2 = gcrf;

          System.out.println("2- Propagation in Celestial reference frame (pseudo_inertial: " +
                             integrationFrame2.isPseudoInertial() + ")");

          final PVCoordinates initialConditions2 = l1Frame.getTransformTo(integrationFrame2, initialDate).
                                                   transformPVCoordinates(initialPVInL1);

          final AbsolutePVCoordinates initialAbsPV2 = new AbsolutePVCoordinates(integrationFrame2, initialDate,
                                                                                initialConditions2);
          final SpacecraftState initialState2 = new SpacecraftState(initialAbsPV2);

          AdaptiveStepsizeIntegrator integrator2 =
                  new DormandPrince853Integrator(minStep, maxstep, vecAbsoluteTolerances, vecRelativeTolerances);

          NumericalPropagator propagator2 = new NumericalPropagator(integrator2);
          propagator2.setOrbitType(null);
          propagator2.setIgnoreCentralAttraction(true);
          propagator2.addForceModel(new ThirdBodyAttraction(moon));
          propagator2.addForceModel(new SingleBodyAbsoluteAttraction(earth));
          propagator2.setInitialState(initialState2);
          propagator2.setMasterMode(outputStep, new TutorialStepHandler("testL1_2.txt", outputFrame));

          SpacecraftState finalState2 = propagator2.propagate(initialDate.shiftedBy(integrationTime));
          final PVCoordinates pv2 = finalState2.getPVCoordinates(outputFrame);

          // 3: Propagation in L2 centered frame

          final Frame integrationFrame3 = l1Frame;

          System.out.println("3- Propagation in L2 reference frame (pseudo_inertial: " +
                             integrationFrame3.isPseudoInertial() + ")");

          final PVCoordinates initialConditions3 = l1Frame.getTransformTo(integrationFrame3,  initialDate).
                                                   transformPVCoordinates(initialPVInL1);

          final AbsolutePVCoordinates initialAbsPV3 = new AbsolutePVCoordinates(integrationFrame3, initialDate,
                                                                                initialConditions3);
          Attitude arbitraryAttitude3 = new Attitude(integrationFrame3,
                  new TimeStampedAngularCoordinates(initialDate,
                                                    new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J),
                                                    new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J)));
          final SpacecraftState initialState3 = new SpacecraftState(initialAbsPV3, arbitraryAttitude3);

          AdaptiveStepsizeIntegrator integrator3 =
                  new DormandPrince853Integrator(minStep, maxstep, vecAbsoluteTolerances, vecRelativeTolerances);

          NumericalPropagator propagator3 = new NumericalPropagator(integrator3);
          propagator3.setOrbitType(null);
          propagator3.setIgnoreCentralAttraction(true);
          propagator3.addForceModel(new SingleBodyAbsoluteAttraction(moon));
          propagator3.addForceModel(new SingleBodyAbsoluteAttraction(earth));
          propagator3.addForceModel(new InertialForces(earthMoonBaryFrame));
          propagator3.setInitialState(initialState3);
          propagator3.setMasterMode(outputStep, new TutorialStepHandler("testL2_3.txt", outputFrame));

          SpacecraftState finalState3 = propagator3.propagate(initialDate.shiftedBy(integrationTime));
          final PVCoordinates pv3 = finalState3.getPVCoordinates(outputFrame);


          // Compare final position
          final Vector3D pos1 = pv1.getPosition();
          final Vector3D pos2 = pv2.getPosition();
          final Vector3D pos3 = pv3.getPosition();
          System.out.format(Locale.US, "Differences between trajectories:%n");
          System.out.format(Locale.US, "    1/3: %10.6f [m]%n", Vector3D.distance(pos1, pos3));
          System.out.format(Locale.US, "    2/3: %10.6f [m]%n", Vector3D.distance(pos2, pos3));
          System.out.format(Locale.US, "    1/2: %10.6f [m]%n", Vector3D.distance(pos1, pos2));

      }

      private static class TutorialStepHandler implements OrekitFixedStepHandler {

          private File        outFile;
          private PrintWriter out;

          private Frame outputFrame;

          private TutorialStepHandler(final String fileName, final Frame frame)
              throws OrekitException {
              try {
                  outFile = new File(new File(System.getProperty("user.home")), fileName);
                  out = new PrintWriter(outFile);
                  outputFrame = frame;
              } catch (IOException ioe) {
                  throw new OrekitException(ioe, LocalizedCoreFormats.SIMPLE_MESSAGE, ioe.getLocalizedMessage());
              }
          }

          public void handleStep(SpacecraftState currentState, boolean isLast) {
              try {
                  // Choose here in which reference frame to output the trajectory

                  System.out.print(".");

                  final AbsoluteDate d = currentState.getDate();
                  final PVCoordinates pv = currentState.getPVCoordinates(outputFrame);

                  out.format(Locale.US, "%s %12.6f %12.6f %12.6f %12.6f %12.6f %12.6f%n",
                             d,
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
                                        d,
                                        finalPv.getPosition().getX(),
                                        finalPv.getPosition().getY(),
                                        finalPv.getPosition().getZ(),
                                        finalPv.getVelocity().getX(),
                                        finalPv.getVelocity().getY(),
                                        finalPv.getVelocity().getZ());
                      System.out.println();
                      out.close();
                      System.out.println("trajectory saved in " + outFile.getAbsolutePath());
                  }
              } catch (OrekitException oe) {
                  System.err.println(oe.getMessage());
              }
          }
      }

}

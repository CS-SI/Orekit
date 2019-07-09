<!--- Copyright 2002-2019 CS Systèmes d'Information
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
    http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Propagation in non-inertial Frame


  The goal of this tutorial is to introduce orbital integration using `SingleBodyAttraction` class.  
  This class can replace all the different kinds of  point mass interactions (`ThirdBodyAttraction`, `NewtonianAttraction`).  
  Using `SingleBodyAttraction` and `InertialForces` enables a richer modelling, allowing the user to compute the motion of a satellite in a reference frame that is not necessarily centered on the main attractor and does not necessarily possess inertial axis.

  
## Initialization
We will propagate the trajectory of a satellite departing from the Lagrange point L2 
of the Earth-Moon system.  
The equations of motion will be computed in a reference Frame centered on L2, its X-axis continuously oriented toward Earth and Moon.  
The rotation of this frame means that we cannot simply use the fundamental principle of dynamics (Newton's second law).  
Since we will consider only the gravitational attractions of the Earth and the Moon, the inertial reference frame most closely 
related to our problem is a frame that is centered on the Earth-Moon barycenter, with inertial axis.

Let’s initialize our program.  
First, the time settings : the initial moment of integration, the length of 
integration (here in seconds), and the time interval between each output.

    final AbsoluteDate initialDate = new AbsoluteDate(2000, 01, 01, 0, 0, 00.000, TimeScalesFactory.getUTC());
    double integrationTime = 600000.;
    double outputStep = 600.0;

Then the initial position and velocity of the satellite, located exactly at L2 point.
We still haven't introduced a reference Frame, so the user must keep in mind in which
reference Frame he wants to define the initial conditions of its satellite. 

    final PVCoordinates initialConditions = new PVCoordinates(new Vector3D(0.0, 0.0, 0.0), new Vector3D(0.0, 0.0, 0.0));
                
## Frames setting
We need to load a few Celestial bodies, those we consider in gravitational interaction 
with our satellite, as well as the Earth-Moon barycenter, since we will compute our 
inertia forces with respect to its attached frame.  
We then create the reference frame attached to the L2 point, and the inertial reference frame attached to the Earth-Moon 
barycenter. 

    final CelestialBody earth = CelestialBodyFactory.getEarth();
    final CelestialBody moon  = CelestialBodyFactory.getMoon();
    final CelestialBody earthMoonBary = CelestialBodyFactory.getEarthMoonBarycenter();
        
    final Frame l2Frame = new L2Frame(earth, moon);
    final Frame earthMoonBaryFrame = earthMoonBary.getInertiallyOrientedFrame();
       
    final Frame inertiaFrame = earthMoonBaryFrame;
    final Frame integrationFrame = l2Frame;
    final Frame outputFrame = l2Frame;
   
## Propagator preparations
We now transform our `PVCoordinates` into `AbsolutePVCoordinates`, define 
the satellite attitude, and use all of it to build the `SpacecraftState` 

    final AbsolutePVCoordinates initialAbsPV = new AbsolutePVCoordinates(integrationFrame, initialDate, initialConditions);
    Attitude arbitraryAttitude = new Attitude(integrationFrame, 
                                                 new TimeStampedAngularCoordinates(initialDate, 
                                                 new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J), 
                                                 new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J)));
    final SpacecraftState initialState = new SpacecraftState(initialAbsPV, arbitraryAttitude);
        
We will use a variable-step 8(5,3) Dormand-Prince integrator. This integrator needs a
few initialization parameters. First, let's set boundaries on the integration steps, 
to prevent a too long computing or a too long integration step. 

    final double minStep = 0.001;
    final double maxstep = 3600.0;
        
We also need to set the acceptable error of our integration. This error is used to 
adjust the integration step and does not stand for the overall error of the 
propagation. 

    final double positionTolerance = 0.001;
    final double velocityTolerance = 0.00001;
    final double massTolerance     = 1.0e-6;
    final double[] vecAbsoluteTolerances = {
            positionTolerance, positionTolerance, positionTolerance,
            velocityTolerance, velocityTolerance, velocityTolerance,
            massTolerance
            };
    final double[] vecRelativeTolerances = new double[vecAbsoluteTolerances.length];
    
 We can now define the numerical integrator of our propagator. 
 
    AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxstep,
                                                                              vecAbsoluteTolerances,
                                                                              vecRelativeTolerances);

## Propagator building and use

And finally, we can build our propagator. First we use the `NumericalPropagator` 
constructor with the previously defined numerical integrator, and then we specify the 
properties of the evolution model. The use of `SingleBodyAttraction` means that 
we do not constrain the type of orbit, and that we need to use `setOrbitType(null)` 
as well as `setIgnoreCentralAttraction(true)`.  
The non-inertial aspect of our integration reference frame is handled by `InertialForces`, which will compute the 
inertial accelerations that appear in this frame. Each attracting body will be provided 
to the propagator by calling `SingleBodyAttraction` for each body in gravitational 
interaction with our spacecraft.  
We then only need to set the inital spacecraft state, 
and the propagator mode, more details about propagator modes can be found in the [Propagation tutorial](../tutorial/propagation.html) .


    NumericalPropagator propagator = new NumericalPropagator(integrator);
    propagator.setOrbitType(null);
    propagator.setIgnoreCentralAttraction(true);
    propagator.addForceModel(new InertialForces(earthMoonBaryFrame));
    propagator.addForceModel(new SingleBodyAbsoluteAttraction(earth));
    propagator.addForceModel(new SingleBodyAbsoluteAttraction(moon));
    propagator.setInitialState(initialState);
    propagator.setMasterMode(outputStep, new TutorialStepHandler("test.dat", outputFrame));
    
In the end we can do the propagation itself, from an initial date, for a given duration.

    SpacecraftState finalState = propagator.propagate(initialDate.shiftedBy(integrationTime));
    final PVCoordinates pv = finalState.getPVCoordinates(outputFrame);
    System.out.println("initial conditions: " + initialConditions);
    System.out.println("final conditions: " + pv);

## Stephandling
Now, we can deal with our stephandler, it will print the position, velocities and acceleration at each output step of the integration. 

     private static class TutorialStepHandler implements OrekitFixedStepHandler {
     private Frame outputFrame;
     private TutorialStepHandler( final Frame frame) {
        outputFrame = frame;
        }
        
     public void init(final SpacecraftState s0, final AbsoluteDate t) {
            System.out.format(Locale.US,
                                      "%s %s %s %s %s %s %s %s %s %s %n",
                                      "date", "                           X", "                 Y", 
                                      "                 Z", "                 Vx", "                Vy",
                                      "                Vz", "                ax", "                ay",
                                      "                az");
        }

        public void handleStep(SpacecraftState currentState, boolean isLast) {
            try {
                final TimeScale utc = TimeScalesFactory.getUTC();
                final AbsoluteDate initialDate = new AbsoluteDate(2000, 01, 01, 0, 0, 00.000,              
                                                                  TimeScalesFactory.getUTC());
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
    
## Results
The printed results are shown below:

    date                            X                  Y                  Z                  Vx                 Vy                 Vz                 ax                 ay                 az 
    2000-01-01T00:00:00.000     0.000000000000     0.000000000000     0.000000000000     0.000000000000     0.000000000000     0.000000000000     0.000007203264    -0.000023617334     0.000000988884
    2000-01-01T00:10:00.000     1.310181754844    -4.509937950657     0.177995376805     0.004389441414    -0.015464490795     0.000593311195     0.000007424953    -0.000027930975     0.000000988817
    2000-01-01T00:20:00.000     5.292761075816   -19.075027371572     0.711964514771     0.008906039828    -0.033517172552     0.001186577792     0.000007627122    -0.000032244627     0.000000988736
    2000-01-01T00:30:00.000    12.020526802001   -45.248275712283     1.601878056606     0.013538115033    -0.054158399174     0.001779791095     0.000007710027    -0.000035388726     0.000000989294
    2000-01-01T00:40:00.000    21.178373170978   -80.022897721805     2.850254273796     0.017011800885    -0.062143736699     0.002381480483     0.000005918985    -0.000015387558     0.000001002821
    2000-01-01T00:50:00.000    32.466478604114  -120.337158982044     4.459645552958     0.020640742565    -0.072667569026     0.002983148787     0.000006174238    -0.000019691906     0.000001002738
    2000-01-01T01:00:00.000    45.976718715462  -167.740438088863     6.430022015915     0.024416959249    -0.085773707784     0.003584763069     0.000006410327    -0.000024000459     0.000001002638
    2000-01-01T01:10:00.000    61.793504290626  -223.776772118858     8.761351689337     0.028327931996    -0.101452129474     0.004186320438     0.000006626318    -0.000028300879     0.000001002529
    2000-01-01T01:20:00.000    79.995099948214  -290.000479343336    11.453591760838     0.032363731793    -0.119724013059     0.004787800539     0.000006823101    -0.000032605395     0.000001002403
    2000-01-01T01:30:00.000   100.652428154023  -367.962135000874    14.506696359194     0.036511612039    -0.140576870435     0.005389201731     0.000006303102    -0.000028624115     0.000001006865
    2000-01-01T01:40:00.000   123.451861739137  -454.645479496582    17.923153578986     0.039511558100    -0.148775025605     0.005998989925     0.000005129778    -0.000015774359     0.000001016276
    2000-01-01T01:50:00.000   148.097431329154  -547.007595557232    21.705469789230     0.042665311712    -0.159528224853     0.006608717996     0.000005379490    -0.000020069660     0.000001016148
    2000-01-01T02:00:00.000   174.679033538614  -646.594792449283    25.853598980322     0.045963054147    -0.172858638623     0.007218365096     0.000005609742    -0.000024365065     0.000001016006
    2000-01-01T02:10:00.000   203.279559120647  -754.953413963500    30.367489987968     0.049393110759    -0.188766311565     0.007827922613     0.000005820538    -0.000028660515     0.000001015850
    2000-01-01T02:20:00.000   233.974894931263  -873.629820586518    35.247086470272     0.052943809883    -0.207251254684     0.008437381900     0.000006011884    -0.000032955956     0.000001015679
    2000-01-01T02:30:00.000   266.833927466020 -1004.170390063609    40.492326874541     0.056603523474    -0.228313934164     0.009046734005     0.000006203223    -0.000037487428     0.000001015363
    2000-01-01T02:40:00.000   301.547225264656 -1143.577979776640    46.105651401557     0.059129787256    -0.236758043587     0.009664358716     0.000004333863    -0.000016135286     0.000001029339
    2000-01-01T02:50:00.000   337.820135753835 -1288.794334653753    52.089537502703     0.061804333216    -0.247725104421     0.010281911014     0.000004578052    -0.000020421603     0.000001029166
    2000-01-01T03:00:00.000   375.740556403237 -1441.362400813750    58.443923059929     0.064619473654    -0.261262891438     0.010899355930     0.000004806508    -0.000024752997     0.000001028954
    2000-01-01T03:10:00.000   415.387681993014 -1602.804245773708    65.168752226799     0.067560866331    -0.277337889071     0.011516703202     0.000005008336    -0.000028994444     0.000001028777
    2000-01-01T03:20:00.000   456.837148988767 -1774.683165398515    72.263941309722     0.070622616256    -0.296020485822     0.012133905439     0.000005194262    -0.000033280871     0.000001028561

    2000-01-01T03:20:00.000          457        -1775           72            0           -0            0

    initial conditions: {P(0.0, 0.0, 0.0), V(0.0, 0.0, 0.0), A(0.0, 0.0, 0.0)}
    final conditions: {2000-01-01T03:20:00.000, P(456.8371489887673, -1774.6831653985146, 72.2639413097221), V(0.0706226162556273, -0.29602048582231744, 0.012133905438764159), A(5.194261804654593E-6, -3.328087090313115E-5, 1.02856099808902E-6)}

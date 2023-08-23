/* Copyright 2002-2023 CS GROUP
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
package org.orekit.utils;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince54Integrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.forces.gravity.ThirdBodyAttractionEpoch;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.EpochDerivativesEquations;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;

public class MultipleShooterTest {

    /** arbitrary date */
    private static final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
    /** arbitrary inertial frame */
    private static final Frame eci = FramesFactory.getGCRF();

    /** unused propagator */
    private NumericalPropagator propagator;
    /** mock force model */
    private ForceModel forceModel;
    /** arbitrary PV */
    private PVCoordinates pv;
    /** arbitrary state */
    private SpacecraftState state;
    /** subject under test */
    private EpochDerivativesEquations pde;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        propagator = new NumericalPropagator(new DormandPrince54Integrator(1, 500, 0.001, 0.001));
        forceModel = new ThirdBodyAttractionEpoch(CelestialBodyFactory.getSun());
        propagator.addForceModel(forceModel);
        propagator.addForceModel(new NewtonianAttraction(Constants.WGS84_EARTH_MU));
        pde = new EpochDerivativesEquations("ede", propagator);
        Vector3D p = new Vector3D(7378137, 0, 0);
        Vector3D v = new Vector3D(0, 7500, 0);
        pv = new PVCoordinates(p, v);
        state = new SpacecraftState(new AbsolutePVCoordinates(eci,date,pv))
                .addAdditionalState("ede", new double[2 * 3 * 6]);
        pde.setInitialJacobians(state);
    }

    @Test
    public void testHaloAllFree() {

        final double mu          = CelestialBodyFactory.getEarth().getGM();
        final CelestialBody sun  = CelestialBodyFactory.getSun();
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final Frame gcrf         = FramesFactory.getGCRF();

        final int nPoints = 10;
        final int maxIter = 10;
        final double tol  = 1e-10;
        final double lc   = 3.844000000000000000e+08;
        final double tc   = 3.751902619517228450e+05;

        final List<NumericalPropagator> propagators = initEarthMoonSunPropagators(nPoints - 1, mu, moon, sun);
        final List<EpochDerivativesEquations> equations = new ArrayList<>(propagators.size());
        for (NumericalPropagator propagator : propagators) {
            equations.add(new EpochDerivativesEquations("derivatives", propagator));
        }

        // test data obtained with JPL DE430/431
        final double[] et0 = {1.710703442857210263e-04, 5.521063246199335861e-01, 1.104041578895581521e+00, 1.655976833171229456e+00, 2.207912087446877614e+00, 2.759847341722525105e+00, 3.311782595998173040e+00, 3.863717850273821419e+00, 4.415653104549469354e+00, 4.967588358825117290e+00};
        final double[] et1 = {-1.796169380098704702e-03, 5.470184571079018676e-01, 1.097404299615500456e+00, 1.648186198102782551e+00, 2.197318288791112018e+00, 2.745223576457172854e+00, 3.297173875907084550e+00, 3.852539493367840073e+00, 4.408833053659896528e+00, 4.960207272596693251e+00};
        final double[][] x0 = {
                {-6.636791666144311597e-01, -6.260186134512231160e-01, -1.322010746905091794e-01, 4.498401249487761211e-01, -4.348504831983970309e-01, -1.991286587261782703e-01},
                {-3.563290965131453158e-01, -8.050631004524881895e-01, -2.656997224191921525e-01, 7.228103064165417591e-01, -1.934475577528897705e-01, -2.416539834034401868e-01},
                {1.098184711973764494e-01, -8.053622845679441200e-01, -3.602005336919254508e-01, 9.098567623336283328e-01, 1.916336076537600019e-01, -6.060160556588414099e-02},
                {5.810304535892597544e-01, -5.805083872470057083e-01, -3.140931328806939593e-01, 7.267619945080785460e-01, 5.591157250721158212e-01, 2.078723035480255221e-01},
                {8.511594815115839374e-01, -2.184655189886064441e-01, -1.452196112147020324e-01, 2.764460316439459331e-01, 6.792727248391994266e-01, 3.435161727260280795e-01},
                {8.656453384917532912e-01, 1.422373279447306793e-01, 2.789154212007919839e-02, -1.126618112920192566e-01, 6.371377921724492577e-01, 2.474009737450874324e-01},
                {6.673229084498149000e-01, 4.925742384041856825e-01, 1.352994194677949458e-01, -5.535397829401387249e-01, 6.313518169281054915e-01, 1.658941924000199852e-01},
                {2.167591512036503576e-01, 7.332934877933906526e-01, 2.118061268351077719e-01, -1.000485493999077935e+00, 2.235984969671991895e-01, 1.044173549698478864e-01},
                {-3.424970816467489132e-01, 6.712307128588804739e-01, 2.348071650613363925e-01, -9.451394206937679954e-01, -4.291136994999154575e-01, -2.116238800304106119e-02},
                {-7.384375730467791499e-01, 3.332794983104613862e-01, 1.912348151159817267e-01, -4.203331994507656377e-01, -7.496229685877604521e-01, -1.295258347191886872e-01}
        };
        final double[][] x1 = {
                {-6.803468015023907967e-01, -6.231753538808705306e-01, -1.325123048243259660e-01, 4.505197326315822925e-01, -4.236560256767275545e-01, -2.151096814166310323e-01},
                {-3.585165741918596161e-01, -8.011086789648022011e-01, -2.701184279913985131e-01, 7.406352431753232546e-01, -1.971670671036328815e-01, -2.450359650417210933e-01},
                {1.094834744447150815e-01, -8.058433173992786136e-01, -3.604109686230322351e-01, 9.096267867116588635e-01, 1.982496079470497108e-01, -5.564241828264352568e-02},
                {5.742787607645793990e-01, -5.863310092002363971e-01, -3.170077188056009687e-01, 7.149855074288306023e-01, 5.725268828715595060e-01, 2.092171421218035066e-01},
                {8.463344697038307496e-01, -2.233959739105416953e-01, -1.532105781075839557e-01, 2.560440242422340473e-01, 6.994698633070792759e-01, 3.536022781049431574e-01},
                {8.663016276475256072e-01, 1.405856328724370274e-01, 2.670516445215945223e-02, -1.534401350638382455e-01, 6.255659208276135308e-01, 2.674117539371450025e-01},
                {6.763925185053241140e-01, 4.839035852230476054e-01, 1.375506108826720642e-01, -5.701472936531262192e-01, 5.993822145517496702e-01, 1.566172126580687718e-01},
                {2.261529014816243965e-01, 7.297512407532273926e-01, 2.096459167731141993e-01, -1.007151524667494025e+00, 2.089061069087984612e-01, 9.724123886319198384e-02},
                {-3.456120955621188040e-01, 6.673089654288644201e-01, 2.355685852340503872e-01, -9.403300811798358527e-01, -4.240235974228969140e-01, -1.077159350352129355e-02},
                {-7.285979589317068683e-01, 3.247340047992977596e-01, 1.962862505492455889e-01, -4.136044496527234160e-01, -7.416599672545861610e-01, -1.317088643161054562e-01}
        };

        final List<SpacecraftState> initialGuess = initStates(gcrf, et0, x0, lc, tc);
        final MultipleShooter shooter = new MultipleShooter(initialGuess, propagators, equations, tol, maxIter);
        shooter.setScaleLength(lc);
        shooter.setScaleTime(tc);
        final List<SpacecraftState> actualSol   = shooter.compute();
        final List<SpacecraftState> expectedSol = initStates(gcrf, et1, x1, lc, tc);

        AbsolutePVCoordinates actPva, expPva;
        for (int i = 0; i < actualSol.size(); i++) {
            actPva = actualSol.get(i).getAbsPVA();
            expPva = expectedSol.get(i).getAbsPVA();
            Assertions.assertEquals(0.0, expPva.getDate().durationFrom(actPva.getDate()), 1e-2);
            Assertions.assertEquals(expPva.getPosition().getX(), actPva.getPosition().getX(), 100.);
            Assertions.assertEquals(expPva.getPosition().getY(), actPva.getPosition().getY(), 100.);
            Assertions.assertEquals(expPva.getPosition().getZ(), actPva.getPosition().getZ(), 100.);
            Assertions.assertEquals(expPva.getVelocity().getX(), actPva.getVelocity().getX(), 1e-4);
            Assertions.assertEquals(expPva.getVelocity().getY(), actPva.getVelocity().getY(), 1e-4);
            Assertions.assertEquals(expPva.getVelocity().getZ(), actPva.getVelocity().getZ(), 1e-4);
        }

    }

    @Test
    public void testDROFix() {

        final double mu          = CelestialBodyFactory.getEarth().getGM();
        final CelestialBody sun  = CelestialBodyFactory.getSun();
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final Frame gcrf         = FramesFactory.getGCRF();

        final int nPoints = 10;
        final int maxIter = 10;
        final double tol  = 1e-10;
        final double lc   = 3.844000000000000000e+08;
        final double tc   = 3.751902619517228450e+05;

        final List<NumericalPropagator> propagators = initEarthMoonSunPropagators(nPoints - 1, mu, moon, sun);
        final List<EpochDerivativesEquations> equations = new ArrayList<>(propagators.size());
        for (NumericalPropagator propagator : propagators) {
            equations.add(new EpochDerivativesEquations("derivatives", propagator));
        }

        // test data obtained with JPL DE430/431
        final double[] et0 = {1.710703442857210263e-04, 6.401099020190278432e-01, 1.280048733693769814e+00, 1.919987565368511895e+00, 2.559926397043253754e+00, 3.199865228717996057e+00, 3.839804060392737473e+00, 4.479742892067479332e+00, 5.119681723742222523e+00, 5.759620555416963938e+00};
        final double[] et1 = {1.710703442857210263e-04, 6.369651402484260982e-01, 1.263074448727465660e+00, 1.884704047732583598e+00, 2.519182974234986716e+00, 3.161038394788097339e+00, 3.810850363361452775e+00, 4.467489429292855085e+00, 5.138495930593371952e+00, 5.759620555416963938e+00};
        final double[][] x0 = {
                {-6.205665299789002720e-01, -5.677666702327113235e-01, -1.620164304649940601e-01, 8.801210381220824219e-01, -8.507935096808968423e-01, -3.895991311533151258e-01},
                {8.594211891534283068e-03, -9.613434330906381886e-01, -3.604220999513738644e-01, 9.667551459176509931e-01, -2.863661310760957091e-01, -1.858583609454188823e-01},
                {5.449001398257361517e-01, -1.006345878374465208e+00, -4.211123604515295549e-01, 7.058882948370835964e-01, 1.608309988576805960e-01, 2.439889201595837453e-03},
                {8.570670771840185331e-01, -7.608622654188437195e-01, -3.548637009097703188e-01, 3.640254092671863506e-01, 6.071770386444899081e-01, 1.972024755600756962e-01},
                {9.372205965769488945e-01, -2.284638365625301648e-01, -1.623539522636843757e-01, -5.713543244527782838e-02, 9.956773249005800297e-01, 3.768816268658942703e-01},
                {6.478420697347909707e-01, 4.469981579129221894e-01, 1.138861720133452077e-01, -7.831196600920956596e-01, 1.021343277693360863e+00, 4.460532549328141694e-01},
                {1.137788077504750181e-02, 8.569480197255274767e-01, 3.194028646001272342e-01, -1.070549091240241113e+00, 3.226163614097921073e-01, 2.085527607298647834e-01},
                {-6.088464687313427381e-01, 8.256822453209395896e-01, 3.587105138401033844e-01, -7.743620002318486462e-01, -2.969001811984433026e-01, -4.730663624584079130e-02},
                {-9.699880169486997383e-01, 4.936295234918476882e-01, 2.642958295427927928e-01, -2.184068214992320733e-01, -7.243654563367226684e-01, -2.527891462308263781e-01},
                {-9.776102590881000642e-01, -4.040836299579693425e-02, 6.531034067567952073e-02, 3.636140791997471422e-01, -9.249338955185112399e-01, -3.756390974973350949e-01}
        };
        final double[][] x1 = {
                {-6.205665299789002720e-01, -5.677666702327113235e-01, -1.620164304649940601e-01, 8.412975101741027029e-01, -8.739388985399625387e-01, -3.955295819076500852e-01},
                {-9.634200415225608119e-03, -9.465736736061085566e-01, -3.535612085069844701e-01, 9.777766696274003966e-01, -2.964902228079116520e-01, -1.907355111592746733e-01},
                {5.368534726159553960e-01, -9.787771085919785286e-01, -4.102816324424908845e-01, 7.293100641078464896e-01, 1.662316893556100572e-01, 2.460973279629753794e-03},
                {8.675932212494770202e-01, -7.470863024726539514e-01, -3.507395860672002930e-01, 3.289109728852876446e-01, 5.934400919229084748e-01, 1.949521822999009668e-01},
                {9.430984992389462862e-01, -2.352522368968300581e-01, -1.655246700441468655e-01, -1.214796662506790748e-01, 9.928347708388319814e-01, 3.812615727607484573e-01},
                {6.478420697347909707e-01, 4.469981579129221894e-01, 1.138861720133452077e-01, -8.080078931444031332e-01, 1.001466686928228045e+00, 4.409668196413770724e-01},
                {-1.832946937378971400e-06, 8.707595351622473556e-01, 3.256493723035326360e-01, -1.060212353240261196e+00, 2.738473162259585370e-01, 1.896404709629285201e-01},
                {-6.177060288818128075e-01, 8.511984984514407993e-01, 3.691668340229080081e-01, -7.666575867611478134e-01, -2.881931671852284715e-01, -4.468330459242809971e-02},
                {-9.702801203452571244e-01, 5.086192196957112222e-01, 2.700423736651194617e-01, -2.790304646682810752e-01, -7.192718117251766241e-01, -2.460945293026643999e-01},
                {-9.854710345116055592e-01, -1.962770730716009368e-02, 7.369777292674678515e-02, 2.605310500397909346e-01, -9.391682851946963062e-01, -3.727274360817400267e-01}
        };

        final List<SpacecraftState> initialGuess = initStates(gcrf, et0, x0, lc, tc);
        final MultipleShooter shooter = new MultipleShooter(initialGuess, propagators, equations, tol, maxIter);

        shooter.setScaleLength(lc);
        shooter.setScaleTime(tc);
        shooter.setEpochFreedom(0, false);
        shooter.setEpochFreedom(9, false);
        shooter.setPatchPointComponentFreedom(0, 0, false);
        shooter.setPatchPointComponentFreedom(0, 1, false);
        shooter.setPatchPointComponentFreedom(0, 2, false);
        shooter.setPatchPointComponentFreedom(5, 0, false);
        shooter.setPatchPointComponentFreedom(5, 1, false);
        shooter.setPatchPointComponentFreedom(5, 2, false);

        final List<SpacecraftState> actualSol   = shooter.compute();
        final List<SpacecraftState> expectedSol = initStates(gcrf, et1, x1, lc, tc);

        AbsolutePVCoordinates actPva, expPva;
        for (int i = 0; i < actualSol.size(); i++) {
            actPva = actualSol.get(i).getAbsPVA();
            expPva = expectedSol.get(i).getAbsPVA();
            Assertions.assertEquals(0.0, expPva.getDate().durationFrom(actPva.getDate()), 1e-1);
            Assertions.assertEquals(expPva.getPosition().getX(), actPva.getPosition().getX(), 100.);
            Assertions.assertEquals(expPva.getPosition().getY(), actPva.getPosition().getY(), 100.);
            Assertions.assertEquals(expPva.getPosition().getZ(), actPva.getPosition().getZ(), 100.);
            Assertions.assertEquals(expPva.getVelocity().getX(), actPva.getVelocity().getX(), 1e-4);
            Assertions.assertEquals(expPva.getVelocity().getY(), actPva.getVelocity().getY(), 1e-4);
            Assertions.assertEquals(expPva.getVelocity().getZ(), actPva.getVelocity().getZ(), 1e-4);
        }

        Assertions.assertEquals(et0[0] * tc, actualSol.get(0).getDate().durationFrom(AbsoluteDate.J2000_EPOCH), 1e-15);
        Assertions.assertEquals(et0[9] * tc, actualSol.get(9).getDate().durationFrom(AbsoluteDate.J2000_EPOCH), 1e-15);
        Assertions.assertEquals(x0[0][0] * lc, actualSol.get(0).getAbsPVA().getPosition().getX(), 1e-15);
        Assertions.assertEquals(x0[0][1] * lc, actualSol.get(0).getAbsPVA().getPosition().getY(), 1e-15);
        Assertions.assertEquals(x0[0][2] * lc, actualSol.get(0).getAbsPVA().getPosition().getZ(), 1e-15);
        Assertions.assertEquals(x0[5][0] * lc, actualSol.get(5).getAbsPVA().getPosition().getX(), 1e-15);
        Assertions.assertEquals(x0[5][1] * lc, actualSol.get(5).getAbsPVA().getPosition().getY(), 1e-15);
        Assertions.assertEquals(x0[5][2] * lc, actualSol.get(5).getAbsPVA().getPosition().getZ(), 1e-15);

    }

    @Test
    public void testTooSmallDimension() {
        Assertions.assertThrows(OrekitException.class, () -> {
            final EpochDerivativesEquations partials = new EpochDerivativesEquations("partials", propagator);
            partials.setInitialJacobians(state, new double[5][6], new double[6][2]);
        });
    }

    @Test
    public void testTooLargeDimension() {
        Assertions.assertThrows(OrekitException.class, () -> {
            final EpochDerivativesEquations partials = new EpochDerivativesEquations("partials", propagator);
            partials.setInitialJacobians(state, new double[8][6], new double[6][2]);
        });
    }

    @Test
    public void testMismatchedDimensions() {
        Assertions.assertThrows(OrekitException.class, () -> {
            final EpochDerivativesEquations partials = new EpochDerivativesEquations("partials", propagator);
            partials.setInitialJacobians(state, new double[6][6], new double[7][2]);
        });
    }

    private static List<NumericalPropagator> initEarthMoonSunPropagators(final int nArcs,
                                                                         final double mu,
                                                                         final CelestialBody moon,
                                                                         final CelestialBody sun) {
        final List<NumericalPropagator> propagators = new ArrayList<>(nArcs);
        for (int i = 0; i < nArcs; i++) {
            final ODEIntegrator integrator = new DormandPrince853Integrator(1e-16, 1e16, 1e-14, 3e-14);
            final NumericalPropagator propagator1 = new NumericalPropagator(integrator);
            propagator1.setOrbitType(null);
            propagator1.setMu(mu);
            propagator1.addForceModel(new ThirdBodyAttractionEpoch(moon));
            propagator1.addForceModel(new ThirdBodyAttractionEpoch(sun));
            propagators.add(propagator1);
        }
        return propagators;
    }

    private static List<SpacecraftState> initStates(final Frame frame,
                                                    final double[] et, final double[][] x,
                                                    final double lc, final double tc) {
        final List<SpacecraftState> states = new ArrayList<>(et.length);
        for (int i = 0; i < et.length; i++) {
            final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(et[i] * tc);
            final Vector3D pos = new Vector3D(x[i][0], x[i][1], x[i][2]).scalarMultiply(lc);
            final Vector3D vel = new Vector3D(x[i][3], x[i][4], x[i][5]).scalarMultiply(lc / tc);
            states.add(new SpacecraftState(new AbsolutePVCoordinates(frame, date1, pos, vel)));
        }
        return states;
    }

}

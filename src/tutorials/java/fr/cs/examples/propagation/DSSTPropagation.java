/*
 * Copyright 2002-2011 CS Communication & Systèmes Licensed to CS Communication & Systèmes (CS)
 * under one or more contributor license agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership. CS licenses this file to You under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package fr.cs.examples.propagation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Formatter;
import java.util.Locale;

import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math.ode.nonstiff.DormandPrince853Integrator;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.PropagationException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.SphericalSpacecraft;
import org.orekit.forces.drag.Atmosphere;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.SimpleExponentialAtmosphere;
import org.orekit.forces.gravity.CunninghamAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.PotentialCoefficientsProvider;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTCentralBody;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTThirdBody;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.OrbitFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * Orekit tutorial for semi-analytical extrapolation using the DSST. Some of the parameters can be
 * set just below, in the class description, to generate comparatives files between a numerical
 * propagator and the DSST propagator.
 * 
 * @author Romain Di Costanzo
 */
public class DSSTPropagation {

    /**
     * Customization of the tutorial :
     */
    // Force model used :
    private static boolean             centralBody        = true;
    private static boolean             tesseralTerms      = true;
    private static boolean             moon               = false;
    private static boolean             sun                = false;
    private static boolean             drag               = false;
    private static boolean             radiationPressure  = false;

    // output file path : example : "D:/DSSTValidation/result/". By default, results will be
    // generated in the Orekit project, if the 'generateFileResult' is set at true.
    private static boolean             generateFileResult = true;
    private static String              outputFilePath     = new String(".").concat(System.getProperty("file.separator"));
    // print one point every xxx seconds
    private static double              printStep          = 1000;

    // extrapolation time
    private static double              extrapolationTime  = 10 * 86400d;
    /**
     * End of tutorial customization
     */

    private static AbsoluteDate        initDate;
    private static DSSTPropagator      propaDSST;
    private static NumericalPropagator propaNUM;

    // Print result with the following date :
    // date (in days, from the initialDate), px, py, pz, vx, vy, vz, a, ex, ey, hx, hy, lm
    private static String              format             = new String("%14.10f %20.10f %20.10f %20.10f %20.10f %20.10f %20.10f %20.10f %20.10f %20.10f %20.10f %20.10f %20.10f");

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // Potential data
        Utils.setDataRoot("tutorial-orekit-data");
        PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        final double mu = provider.getMu();
        final double ae = provider.getAe();

        // Orbit definition
        AbsoluteDate date = new AbsoluteDate("2005-01-01T12:00:00.000", TimeScalesFactory.getUTC());

        /**
         * WARNING !! We are here using an heliosynchronous orbit. As the DSST doen't take yet short
         * periodic variations create a fake mean orbit from the oscullating parameters. If you want
         * to use an other orbit, please comment the code below, and uncomment next example :
         */
        // !!! COMMENT THIS UNTIL ...
        // SpacecraftState orbitOsc = new SpacecraftState(OrbitFactory.getHeliosynchronousOrbit(ae,
        // 800000, 1e-3, 0d, Math.PI / 2d, Math.PI, mu, FramesFactory.getGCRF(), date));
        // // Set the numrical propagator to compute the mean orbit from the osculating one :
        // setNumProp(orbitOsc);
        // // Add a default gravitational model
        // propaNUM.addForceModel(new CunninghamAttractionModel(FramesFactory.getITRF2005(), ae, mu,
        // provider.getC(2, 0, false), provider.getS(2, 0, false)));
        // // Create the mean orbit from the osculating :
        // SpacecraftState[] orbits = OrbitFactory.getMeanOrbitFromOsculating(propaNUM, 1 * 86400,
        // 14, 10);
        // SpacecraftState mean = orbits[0];
        // SpacecraftState osc = orbits[1];
        //
        // // As the DSST propagator doesn't take short periodic variation in account actually, we
        // need
        // // to use the 'mean' orbit for DSSTPropagator and the 'osc' orbit for the numerical
        // // propagator :
        // setDSSTProp(mean);
        // // Reset the numerical propagator with new orbit (remove every force model)
        // setNumProp(osc);
        // ... HERE //

        // UNCOMMENT THIS UNTIL ...
        SpacecraftState orbitOsc = new SpacecraftState(OrbitFactory.getGeostationnaryOrbit(mu, FramesFactory.getGCRF(), date));
        setDSSTProp(orbitOsc);
        setNumProp(orbitOsc);
        // ... HERE //
        /**
         * FORCES :
         */
        if (centralBody) {
            // Central Body Force Model with un-normalized coefficients
            double[][] CnmNotNorm;
            double[][] SnmNotNorm;
            if (tesseralTerms){
                CnmNotNorm = provider.getC(5, 5, false);
                SnmNotNorm = provider.getS(5, 5, false);
            }else {
                CnmNotNorm = provider.getC(5, 0, false);
                SnmNotNorm = provider.getS(5, 0, false);
            }

            // Resonant couple list is here set to null : we're taking in account every tesseral
            // harmonic :
            DSSTForceModel centralBodyDSST = new DSSTCentralBody(ae, mu, CnmNotNorm, CnmNotNorm, null, 1e-4);
            ForceModel centralBodyNUM = new CunninghamAttractionModel(FramesFactory.getITRF2005(), ae, mu, CnmNotNorm, SnmNotNorm);
            propaDSST.addForceModel(centralBodyDSST);
            propaNUM.addForceModel(centralBodyNUM);
        }

        if (sun) {
            DSSTForceModel sunDSST = new DSSTThirdBody(CelestialBodyFactory.getSun());
            ForceModel sunNUM = new ThirdBodyAttraction(CelestialBodyFactory.getSun());
            propaDSST.addForceModel(sunDSST);
            propaNUM.addForceModel(sunNUM);
        }

        if (moon) {
            DSSTForceModel moonDSST = new DSSTThirdBody(CelestialBodyFactory.getMoon());
            ForceModel moonNUM = new ThirdBodyAttraction(CelestialBodyFactory.getMoon());
            propaDSST.addForceModel(moonDSST);
            propaNUM.addForceModel(moonNUM);
        }

        if (drag) {
            // Drag Force Model
            OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF2005());
            earth.setAngularThreshold(1.e-6);
            Atmosphere atm = new SimpleExponentialAtmosphere(earth, 4.e-13, 500000.0, 60000.0);
            final double cd = 2.0;
            final double sf = 5.0;
            DSSTForceModel dragDSST = new DSSTAtmosphericDrag(atm, cd, sf);
            ForceModel dragNUM = new DragForce(atm, new SphericalSpacecraft(sf, cd, 0., 0.));
            propaDSST.addForceModel(dragDSST);
            propaNUM.addForceModel(dragNUM);
        }

        if (radiationPressure) {
            // Solar Radiation Pressure Force Model
            PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
            double sf = 5.0;
            double kA = 0.5;
            double kR = 0.5;
            double cR = 2. * (1. + (1. - kA) * (1. - kR) * 4. / 9.);
            // DSST radiation pressure force
            DSSTForceModel pressureDSST = new DSSTSolarRadiationPressure(cR, sf, sun, ae);
            // NUMERICAL radiation pressure force
            SphericalSpacecraft spc = new SphericalSpacecraft(sf, 0., kA, kR);
            ForceModel pressureNUM = new SolarRadiationPressure(sun, ae, spc);
            propaDSST.addForceModel(pressureDSST);
            propaNUM.addForceModel(pressureNUM);
        }

        if (generateFileResult) {
            String fileNameExtention = "";
            if (centralBody) {
                fileNameExtention = fileNameExtention.concat("earth");
            }
            if (sun) {
                fileNameExtention = fileNameExtention.concat("_sun");
            }
            if (moon) {
                fileNameExtention = fileNameExtention.concat("_moon");
            }
            if (drag) {
                fileNameExtention = fileNameExtention.concat("_drag");
            }
            if (radiationPressure) {
                fileNameExtention = fileNameExtention.concat("_radPres");
            }

            propaDSST.setMasterMode(printStep, new PrintStepHandler(outputFilePath, fileNameExtention.concat("_DSST"), format, initDate));
            propaNUM.setMasterMode(printStep, new PrintStepHandler(outputFilePath, fileNameExtention.concat("_NUM"), format, initDate));
        }

        // DSST Propagation
        double dsstStart = System.currentTimeMillis();
        propaDSST.propagate(initDate.shiftedBy(extrapolationTime));
        double dsstEnd = System.currentTimeMillis();

        System.out.println("execution time DSST : " + (dsstEnd - dsstStart) / 1000.);

        // Numerical Propagation
        double NUMStart = System.currentTimeMillis();
        propaNUM.propagate(initDate.shiftedBy(extrapolationTime));
        double NUMEnd = System.currentTimeMillis();

        System.out.println("execution time NUM : " + (NUMEnd - NUMStart) / 1000.);

    }

    private static void setNumProp(SpacecraftState initialState) {
        final double[][] tol = NumericalPropagator.tolerances(1.0, initialState.getOrbit(), initialState.getOrbit().getType());
        final double minStep = 10.;
        final double maxStep = 1000.;
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
        integrator.setInitialStepSize(100.);
        propaNUM = new NumericalPropagator(integrator);
        propaNUM.setInitialState(initialState);
    }

    private static void setDSSTProp(SpacecraftState initialState) throws PropagationException {
        initDate = initialState.getDate();
        final double minStep = 800.;
        final double maxStep = 86400.;
        final double[][] tol = DSSTPropagator.tolerances(1.0, initialState.getOrbit());
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
        integrator.setInitialStepSize(minStep);
        propaDSST = new DSSTPropagator(integrator, initialState.getOrbit());

    }

    /**
     * Specialized step handler.
     * <p>
     * This class extends the step handler in order to print on the output stream at the given step.
     * <p>
     */
    private static class PrintStepHandler implements OrekitFixedStepHandler {

        /**
         * Output format.
         */
        private final String         format;

        /**
         * Buffer
         */
        private final BufferedWriter buffer;

        /** Starting date */
        private AbsoluteDate         dateIni;

        /** Serializable UID. */
        private static final long    serialVersionUID = -8909135870522456848L;

        private PrintStepHandler(final String outputPath,
                                 final String name,
                                 final String format,
                                 final AbsoluteDate initDate)
                                                             throws IOException {
            this.buffer = new BufferedWriter(new FileWriter(outputPath + name));
            this.format = format;
            this.dateIni = initDate;
        }

        public void handleStep(SpacecraftState currentState,
                               boolean isLast) {
            final StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb, Locale.ENGLISH);

            // PV printer
            final Vector3D pos = currentState.getOrbit().getPVCoordinates().getPosition();
            final Vector3D vel = currentState.getOrbit().getPVCoordinates().getVelocity();
            final double px = pos.getX();
            final double py = pos.getY();
            final double pz = pos.getZ();
            final double vx = vel.getX();
            final double vy = vel.getY();
            final double vz = vel.getZ();
            // Equinoctial printer :
            EquinoctialOrbit orb = new EquinoctialOrbit(currentState.getOrbit());
            final double a = orb.getA();
            final double ex = orb.getEquinoctialEx();
            final double ey = orb.getEquinoctialEy();
            final double hx = orb.getHx();
            final double hy = orb.getHy();
            final double lm = orb.getLM();
            // Date printer
            final double deltaDay = currentState.getDate().durationFrom(dateIni) / 86400d;
            formatter.format(this.format, deltaDay, px, py, pz, vx, vy, vz, a, ex, ey, hx, hy, lm);
            try {
                this.buffer.write(formatter.toString());
                this.buffer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (isLast) {
                try {
                    buffer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}

/* Copyright 2002-2026 CS GROUP
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
package org.orekit.control.relative;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.FastMath;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;

/**
 * Helper class to calculate a natural circumnavigation orbit around a target in an elliptic orbit.
 *
 * @author Romain Cuvillon
 * @since 14.0
 */
public class CoellipticOrbit {
    private CoellipticOrbit() {
    }

    /**
     * Computes the chaser orbit to naturally circumnavigate around a target satellite in an elliptic orbit.
     *
     * @param targetOrbit        orbit of the target.
     * @param semiMinorAxis      semi-minor axis of the relative circumnavigation orbit.
     * @param xPlaneOffset       cross plane extent due to tilting the relative orbit. (expressed in meters)
     * @param vBarOffset         desired chaser ellipse offset from the target v-bar location. (expressed in meters)
     * @param driftPerOrbit      corkscrew v-bar motion, drift in meters per orbit.
     * @param xPlaneIVectorPhase clockwise angle measured from the velocity vector in the radial-velocity plane, defines the tilt-axis of the cross plane.
     * @return KeplerianOrbit for the chaser to naturally circumnavigate around the target.
     */
    public static KeplerianOrbit computeChaserOrbit(final KeplerianOrbit targetOrbit, final double semiMinorAxis, final double xPlaneOffset, final double vBarOffset, final double driftPerOrbit, final double xPlaneIVectorPhase) {

        final double deltaA = -driftPerOrbit / (3 * FastMath.PI);
        final double chaserA = targetOrbit.getA() + deltaA;

        final double deltaE = 2 * semiMinorAxis / (2 * targetOrbit.getA());
        final double chaserE = targetOrbit.getE() + deltaE;

        final double deltaI = xPlaneOffset / targetOrbit.getA();
        final double theta = xPlaneIVectorPhase + targetOrbit.getPerigeeArgument();
        double chaserRaan = 0.;
        double chaserI = 0.;

        if (FastMath.toDegrees(theta) < 90) {
            final double B = targetOrbit.getI();
            final double C = FastMath.acos(FastMath.sin(deltaI) * FastMath.sin(B) * FastMath.cos(theta) - FastMath.cos(deltaI) * FastMath.cos(B));
            chaserI = FastMath.PI - C;
            final double alpha = FastMath.asin(FastMath.sin(deltaI) * FastMath.sin(theta) / FastMath.sin(C));
            chaserRaan = targetOrbit.getRightAscensionOfAscendingNode() + alpha;
        } else {
            if (FastMath.toDegrees(theta) < 180) {
                final double gamma = FastMath.PI - theta;
                final double B = FastMath.PI - targetOrbit.getI();
                final double C = FastMath.acos(FastMath.sin(deltaI) * FastMath.sin(B) * FastMath.cos(gamma) - FastMath.cos(deltaI) * FastMath.cos(B));
                chaserI = C;
                final double alpha = FastMath.asin(FastMath.sin(deltaI) * FastMath.sin(gamma) / FastMath.sin(C));
                chaserRaan = targetOrbit.getRightAscensionOfAscendingNode() + alpha;
            } else if (FastMath.toDegrees(theta) < 270) {
                final double gamma = theta - FastMath.PI;
                final double B = FastMath.PI - targetOrbit.getI();
                final double C = FastMath.acos(FastMath.sin(deltaI) * FastMath.sin(B) * FastMath.cos(gamma) - FastMath.cos(deltaI) * FastMath.cos(B));
                chaserI = C;
                final double alpha = FastMath.asin(FastMath.sin(deltaI) * FastMath.sin(gamma) / FastMath.sin(C));
                chaserRaan = targetOrbit.getRightAscensionOfAscendingNode() - alpha;
            } else if (FastMath.toDegrees(theta) < 360) {
                final double gamma = 2 * FastMath.PI - theta;
                final double B = targetOrbit.getI();
                final double C = FastMath.acos(FastMath.sin(deltaI) * FastMath.sin(B) * FastMath.cos(gamma) - FastMath.cos(deltaI) * FastMath.cos(B));
                chaserI = FastMath.PI - C;
                final double alpha = FastMath.asin(FastMath.sin(deltaI) * FastMath.sin(gamma) / FastMath.sin(C));
                chaserRaan = targetOrbit.getRightAscensionOfAscendingNode() - alpha;
            }
        }
        final double chaserPerigeeArgument = targetOrbit.getPerigeeArgument();

        final double f_xPlaneCenter = targetOrbit.getTrueAnomaly() - (chaserRaan - targetOrbit.getRightAscensionOfAscendingNode()) * FastMath.cos(targetOrbit.getI());
        final double deltaFinPlaneCenter = 2 * deltaE * FastMath.sin(targetOrbit.getTrueAnomaly());
        final double deltaFVBarOffset = vBarOffset / targetOrbit.getA();

        final double chaserTrueAnomaly = f_xPlaneCenter + deltaFinPlaneCenter + deltaFVBarOffset;

        return new KeplerianOrbit(chaserA, chaserE, chaserI, chaserPerigeeArgument, chaserRaan, chaserTrueAnomaly,
                PositionAngleType.TRUE, targetOrbit.getFrame(), targetOrbit.getDate(), targetOrbit.getMu());
    }

    /**
     * Computes the chaser orbit to naturally circumnavigate around a target satellite in an elliptic orbit.
     *
     * @param targetOrbit        orbit of the target.
     * @param semiMinorAxis      semi minor axis of the relative circumnavigation orbit.
     * @param xPlaneOffset       cross plane extent due to tilting the relative orbit. (expressed in meters)
     * @param vBarOffset         desired chaser ellipse offset from the target v-bar location. (expressed in meters)
     * @param driftPerOrbit      corkscrew v-bar motion, drift in meters per orbit.
     * @param xPlaneIVectorPhase clockwise angle measured from the velocity vector in the radial-velocity plane, defines the tilt-axis of the cross plane.
     * @param <T>                type of the field elements.
     * @return KeplerianOrbit fo the chaser to naturally circumnavigate around the target.
     */
    public static <T extends CalculusFieldElement<T>> FieldKeplerianOrbit<T> computeChaserOrbit(final FieldKeplerianOrbit<T> targetOrbit, final T semiMinorAxis, final T xPlaneOffset, final T vBarOffset, final T driftPerOrbit, final T xPlaneIVectorPhase) {

        final Field<T> field = targetOrbit.getA().getField();
        final T deltaA = driftPerOrbit.negate().divide(3 * FastMath.PI);
        final T chaserA = targetOrbit.getA().add(deltaA);

        final T deltaE = semiMinorAxis.multiply(2).divide(targetOrbit.getA().multiply(2));
        final T chaserE = targetOrbit.getE().add(deltaE);

        final T deltaI = xPlaneOffset.divide(targetOrbit.getA());
        final T theta = xPlaneIVectorPhase.add(targetOrbit.getPerigeeArgument());
        T chaserRaan = field.getZero();
        T chaserI = field.getZero();
        final T pi = field.getOne().multiply(FastMath.PI);

        if (FastMath.toDegrees(theta.getReal()) < 90) {
            final T B = targetOrbit.getI();
            final T C = (deltaI.sin().multiply(B.sin().multiply(theta.cos())).subtract(deltaI.cos().multiply(B.cos()))).acos();
            chaserI = pi.subtract(C);
            final T alpha = (deltaI.sin().multiply(theta.sin()).divide(C.sin())).asin();
            chaserRaan = targetOrbit.getRightAscensionOfAscendingNode().add(alpha);
        } else {
            if (FastMath.toDegrees(theta.getReal()) < 180) {
                final T gamma = pi.subtract(theta);
                final T B = pi.subtract(targetOrbit.getI());
                final T C = (deltaI.sin().multiply(B.sin()).multiply(gamma.cos()).subtract(deltaI.cos().multiply(B.cos()))).acos();
                chaserI = C;
                final T alpha = (deltaI.sin().multiply(gamma.sin()).divide(C.sin())).asin();
                chaserRaan = targetOrbit.getRightAscensionOfAscendingNode().add(alpha);
            } else if (FastMath.toDegrees(theta.getReal()) < 270) {
                final T gamma = theta.subtract(pi);
                final T B = pi.subtract(targetOrbit.getI());
                final T C = (deltaI.sin().multiply(B.sin()).multiply(gamma.cos()).subtract(deltaI.cos().multiply(B.cos()))).acos();
                chaserI = C;
                final T alpha = (deltaI.sin().multiply(gamma.sin()).divide(C.sin())).asin();
                chaserRaan = targetOrbit.getRightAscensionOfAscendingNode().subtract(alpha);
            } else if (FastMath.toDegrees(theta.getReal()) < 360) {
                final T gamma = pi.multiply(2).subtract(theta);
                final T B = targetOrbit.getI();
                final T C = (deltaI.sin().multiply(B.sin()).multiply(gamma.cos()).subtract(deltaI.cos().multiply(B.cos()))).acos();
                chaserI = pi.subtract(C);
                final T alpha = (deltaI.sin().multiply(gamma.sin()).divide(C.sin())).asin();
                chaserRaan = targetOrbit.getRightAscensionOfAscendingNode().subtract(alpha);
            }
        }
        final T chaserPerigeeArgument = targetOrbit.getPerigeeArgument();

        final T f_xPlaneCenter = targetOrbit.getTrueAnomaly().subtract(chaserRaan.subtract(targetOrbit.getRightAscensionOfAscendingNode()).multiply(targetOrbit.getI().cos()));
        final T deltaFinPlaneCenter = deltaE.multiply(2).multiply(targetOrbit.getTrueAnomaly().sin());
        final T deltaFVBarOffset = vBarOffset.divide(targetOrbit.getA());

        final T chaserTrueAnomaly = f_xPlaneCenter.add(deltaFinPlaneCenter).add(deltaFVBarOffset);

        return new FieldKeplerianOrbit<>(chaserA, chaserE, chaserI, chaserPerigeeArgument, chaserRaan, chaserTrueAnomaly,
                PositionAngleType.TRUE, targetOrbit.getFrame(), targetOrbit.getDate(), targetOrbit.getMu());
    }
}

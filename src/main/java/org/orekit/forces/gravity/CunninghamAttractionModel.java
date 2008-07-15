/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.forces.gravity;


import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.TimeDerivativesEquations;

/** This class represents the gravitational field of a celestial body.
 * <p>The algorithm implemented in this class has been designed by
 * Leland E. Cunningham (Lockheed Missiles and Space Company, Sunnyvale
 * and Astronomy Department University of California, Berkeley) in
 * his 1969 paper: <em>On the computation of the spherical harmonic
 * terms needed during the numerical integration of the orbital motion
 * of an artificial satellite</em> (Celestial Mechanics 2, 1970).</p>
 *
 * @author Fabien Maussion
 * @author Luc Maisonobe
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */

public class CunninghamAttractionModel implements ForceModel {

    /** Serializable UID. */
    private static final long serialVersionUID = 2609845747545125479L;

    /** Equatorial radius of the Central Body. */
    private final double equatorialRadius;

    /** Central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private double mu;

    /** First normalized potential tesseral coefficients array. */
    private final double[][] C;

    /** Second normalized potential tesseral coefficients array. */
    private final double[][] S;

    /** Degree of potential. */
    private final int degree;

    /** Order of potential. */
    private final int order;

    /** Rotating body. */
    private final Frame bodyFrame;

    /** Creates a new instance.
     *
     * @param centralBodyFrame rotating body frame
     * @param equatorialRadius reference equatorial radius of the potential
     * @param mu central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param C un-normalized coefficients array (cosine part)
     * @param S un-normalized coefficients array (sine part)
     * @exception IllegalArgumentException if coefficients array do not match
     */
    public CunninghamAttractionModel(final Frame centralBodyFrame,
                                     final double equatorialRadius, final double mu,
                                     final double[][] C, final double[][] S)
        throws IllegalArgumentException {

        this.bodyFrame = centralBodyFrame;
        this.equatorialRadius = equatorialRadius;
        this.mu = mu;
        degree  = C.length - 1;
        order   = C[degree].length - 1;

        if ((C.length != S.length) ||
            (C[C.length - 1].length != S[S.length - 1].length)) {
            throw OrekitException.createIllegalArgumentException("potential arrays sizes mismatch (C: {0}x{1}, S: {2}x{3})",
                                                                 new Object[] {
                                                                     Integer.valueOf(C.length),
                                                                     Integer.valueOf(C[degree].length),
                                                                     Integer.valueOf(S.length),
                                                                     Integer.valueOf(S[degree].length)
                                                                 });
        }

        if (C.length < 1) {
            this.C = new double[1][1];
            this.S = new double[1][1];
        } else {
            // invert the arrays (optimization for later "line per line" seeking)
            this.C = new double[C[degree].length][C.length];
            this.S = new double[S[degree].length][S.length];

            for (int i = 0; i <= degree; i++) {
                final double[] cT = C[i];
                final double[] sT = S[i];
                for (int j = 0; j < cT.length; j++) {
                    this.C[j][i] = cT[j];
                    this.S[j][i] = sT[j];
                }
            }
        }

        // do not compute keplerian evolution
        this.C[0][0] = 0.0;

    }

    /** {@inheritDoc} */
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder)
        throws OrekitException {
        // get the position in body frame
        final Transform fromBodyFrame = bodyFrame.getTransformTo(s.getFrame(), s.getDate());
        final Transform toBodyFrame   = fromBodyFrame.getInverse();
        final Vector3D relative = toBodyFrame.transformPosition(s.getPVCoordinates().getPosition());

        final double x = relative.getX();
        final double y = relative.getY();
        final double z = relative.getZ();

        final double x2 = x * x;
        final double y2 = y * y;
        final double z2 = z * z;
        final double r2 = x2 + y2 + z2;
        final double r = Math.sqrt(r2);
        if (r <= equatorialRadius) {
            throw new OrekitException("trajectory inside the Brillouin sphere (r = {0})",
                                      new Object[] {
                                          new Double(r)
                                      });
        }

        // define some intermediate variables
        final double onR2 = 1 / r2;
        final double onR3 = onR2 / r;
        final double onR4 = onR2 * onR2;

        double cmx   = -x * onR2;
        double cmy   = -y * onR2;
        double cmz   = -z * onR2;

        final double dx   = -2 * cmx;
        final double dy   = -2 * cmy;
        final double dz   = -2 * cmz;

        // intermediate variables gradients
        // since dcy/dx = dcx/dy, dcz/dx = dcx/dz and dcz/dy = dcy/dz,
        // we reuse the existing variables

        double dcmxdx = (x2 - y2 - z2) * onR4;
        double dcmxdy =  dx * y * onR2;
        double dcmxdz =  dx * z * onR2;
        double dcmydy = (y2 - x2 - z2) * onR4;
        double dcmydz =  dy * z * onR2;
        double dcmzdz = (z2 - x2 - y2) * onR4;

        final double ddxdx = -2 * dcmxdx;
        final double ddxdy = -2 * dcmxdy;
        final double ddxdz = -2 * dcmxdz;
        final double ddydy = -2 * dcmydy;
        final double ddydz = -2 * dcmydz;
        final double ddzdz = -2 * dcmzdz;

        final double donr2dx = -dx * onR2;
        final double donr2dy = -dy * onR2;
        final double donr2dz = -dz * onR2;

        // potential coefficients (4 per matrix)
        double Vrn  = 0.0;
        double Vin  = 0.0;
        double Vrd  = 1.0 / r;;
        double Vid  = 0.0;
        double Vrn1 = 0.0;
        double Vin1 = 0.0;
        double Vrn2 = 0.0;
        double Vin2 = 0.0;

        // gradient coefficients (4 per matrix)
        double gradXVrn  = 0.0;
        double gradXVin  = 0.0;
        double gradXVrd  = -x * onR3;
        double gradXVid  = 0.0;
        double gradXVrn1 = 0.0;
        double gradXVin1 = 0.0;
        double gradXVrn2 = 0.0;
        double gradXVin2 = 0.0;

        double gradYVrn  = 0.0;
        double gradYVin  = 0.0;
        double gradYVrd  = -y * onR3;
        double gradYVid  = 0.0;
        double gradYVrn1 = 0.0;
        double gradYVin1 = 0.0;
        double gradYVrn2 = 0.0;
        double gradYVin2 = 0.0;

        double gradZVrn  = 0.0;
        double gradZVin  = 0.0;
        double gradZVrd  = -z * onR3;
        double gradZVid  = 0.0;
        double gradZVrn1 = 0.0;
        double gradZVin1 = 0.0;
        double gradZVrn2 = 0.0;
        double gradZVin2 = 0.0;

        // acceleration coefficients
        double vdX = 0.0;
        double vdY = 0.0;
        double vdZ = 0.0;
        double rm  = 1.0;

        // start calculating
        for (int m = 0; m <= order; m++) {

            // intermediate variables to compute incrementation
            final double[] Cm = C[m];
            final double[] Sm = S[m];

            double rn = rm;
            double cx = cmx;
            double cy = cmy;
            double cz = cmz;

            double dcxdx = dcmxdx;
            double dcxdy = dcmxdy;
            double dcxdz = dcmxdz;
            double dcydy = dcmydy;
            double dcydz = dcmydz;
            double dczdz = dcmzdz;

            Vrn1 = Vrd;
            Vin1 = Vid;

            gradXVrn1 = gradXVrd;
            gradXVin1 = gradXVid;
            gradYVrn1 = gradYVrd;
            gradYVin1 = gradYVid;
            gradZVrn1 = gradZVrd;
            gradZVin1 = gradZVid;

            for (int n = m; n <= degree; n++) {

                if (n == m) {
                    // calculate the first element of the next column
                    Vrd = (cx + dx) * Vrn1 - (cy + dy) * Vin1;
                    Vid = (cy + dy) * Vrn1 + (cx + dx) * Vin1;

                    gradXVrd = (cx + dx) * gradXVrn1 - (cy + dy) * gradXVin1 + (dcxdx + ddxdx) * Vrn1 - (dcxdy + ddxdy) * Vin1;
                    gradXVid = (cy + dy) * gradXVrn1 + (cx + dx) * gradXVin1 + (dcxdy + ddxdy) * Vrn1 + (dcxdx + ddxdx) * Vin1;

                    gradYVrd = (cx + dx) * gradYVrn1 - (cy + dy) * gradYVin1 + (dcxdy + ddxdy) * Vrn1 - (dcydy + ddydy) * Vin1;
                    gradYVid = (cy + dy) * gradYVrn1 + (cx + dx) * gradYVin1 + (dcydy + ddydy) * Vrn1 + (dcxdy + ddxdy) * Vin1;

                    gradZVrd = (cx + dx) * gradZVrn1 - (cy + dy) * gradZVin1 + (dcxdz + ddxdz) * Vrn1 - (dcydz + ddydz) * Vin1;
                    gradZVid = (cy + dy) * gradZVrn1 + (cx + dx) * gradZVin1 + (dcydz + ddydz) * Vrn1 + (dcxdz + ddxdz) * Vin1;
                    // initialize the current column
                    Vrn = Vrn1;
                    Vin = Vin1;

                    gradXVrn = gradXVrn1;
                    gradXVin = gradXVin1;
                    gradYVrn = gradYVrn1;
                    gradYVin = gradYVin1;
                    gradZVrn = gradZVrn1;
                    gradZVin = gradZVin1;

                }

                if (n == m + 1) {
                    // calculate the second element of the column
                    Vrn = cz * Vrn1;
                    Vin = cz * Vin1;

                    gradXVrn = cz * gradXVrn1 + dcxdz * Vrn1;
                    gradXVin = cz * gradXVin1 + dcxdz * Vin1;

                    gradYVrn = cz * gradYVrn1 + dcydz * Vrn1;
                    gradYVin = cz * gradYVin1 + dcydz * Vin1;

                    gradZVrn = cz * gradZVrn1 + dczdz * Vrn1;
                    gradZVin = cz * gradZVin1 + dczdz * Vin1;

                } else if (n >= m + 2) {
                    // calculate the other elements of the column
                    final double inv   = 1.0 / (n - m);
                    final double coeff = n + m - 1.0;

                    Vrn = (cz * Vrn1 - coeff * onR2 * Vrn2) * inv;
                    Vin = (cz * Vin1 - coeff * onR2 * Vin2) * inv;

                    gradXVrn = (cz * gradXVrn1 - coeff * onR2 * gradXVrn2 + dcxdz * Vrn1 - coeff * donr2dx * Vrn2) * inv;
                    gradXVin = (cz * gradXVin1 - coeff * onR2 * gradXVin2 + dcxdz * Vin1 - coeff * donr2dx * Vin2) * inv;
                    gradYVrn = (cz * gradYVrn1 - coeff * onR2 * gradYVrn2 + dcydz * Vrn1 - coeff * donr2dy * Vrn2) * inv;
                    gradYVin = (cz * gradYVin1 - coeff * onR2 * gradYVin2 + dcydz * Vin1 - coeff * donr2dy * Vin2) * inv;
                    gradZVrn = (cz * gradZVrn1 - coeff * onR2 * gradZVrn2 + dczdz * Vrn1 - coeff * donr2dz * Vrn2) * inv;
                    gradZVin = (cz * gradZVin1 - coeff * onR2 * gradZVin2 + dczdz * Vin1 - coeff * donr2dz * Vin2) * inv;
                }

                // increment variables
                cx += dx;
                cy += dy;
                cz += dz;

                dcxdx += ddxdx;
                dcxdy += ddxdy;
                dcxdz += ddxdz;
                dcydy += ddydy;
                dcydz += ddydz;
                dczdz += ddzdz;

                Vrn2 = Vrn1;
                Vin2 = Vin1;
                gradXVrn2 = gradXVrn1;
                gradXVin2 = gradXVin1;
                gradYVrn2 = gradYVrn1;
                gradYVin2 = gradYVin1;
                gradZVrn2 = gradZVrn1;
                gradZVin2 = gradZVin1;

                Vrn1 = Vrn;
                Vin1 = Vin;
                gradXVrn1 = gradXVrn;
                gradXVin1 = gradXVin;
                gradYVrn1 = gradYVrn;
                gradYVin1 = gradYVin;
                gradZVrn1 = gradZVrn;
                gradZVin1 = gradZVin;

                // compute the acceleration due to the Cnm and Snm coefficients
                // ( as the matrix is inversed, Cnm actually is Cmn )

                if (Cm[n] != 0.0 || Sm[n] != 0.0) { // avoid doing the processing if not necessary
                    vdX += rn * (Cm[n] * gradXVrn + Sm[n] * gradXVin);
                    vdY += rn * (Cm[n] * gradYVrn + Sm[n] * gradYVin);
                    vdZ += rn * (Cm[n] * gradZVrn + Sm[n] * gradZVin);
                }

                rn *= equatorialRadius;

            }

            // increment variables
            rm *= equatorialRadius;

            cmx += dx;
            cmy += dy;
            cmz += dz;

            dcmxdx += ddxdx;
            dcmxdy += ddxdy;
            dcmxdz += ddxdz;
            dcmydy += ddydy;
            dcmydz += ddydz;
            dcmzdz += ddzdz;

        }

        // compute acceleration in inertial frame
        final Vector3D acceleration =
            fromBodyFrame.transformVector(new Vector3D(mu * vdX, mu * vdY, mu * vdZ));
        adder.addXYZAcceleration(acceleration.getX(), acceleration.getY(), acceleration.getZ());

    }

    /** {@inheritDoc} */
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[0];
    }

}

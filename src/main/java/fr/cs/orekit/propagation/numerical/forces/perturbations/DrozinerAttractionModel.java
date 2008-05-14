package fr.cs.orekit.propagation.numerical.forces.perturbations;

import org.apache.commons.math.geometry.Vector3D;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.frames.Transform;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.propagation.numerical.OrekitSwitchingFunction;
import fr.cs.orekit.propagation.numerical.TimeDerivativesEquations;
import fr.cs.orekit.propagation.numerical.forces.ForceModel;

/** This class represents the gravitational field of a celestial body.
 * <p>The algorithm implemented in this class has been designed by
 * Andrzej Droziner (Institute of Mathematical Machines, Warsaw) in
 * his 1976 paper: <em>An algorithm for recurrent calculation of gravitational
 * acceleration</em> (artificial satellites, Vol. 12, No 2, June 1977).</p>
 *
 * @author F. Maussion
 * @author L. Maisonobe
 */

public class DrozinerAttractionModel implements ForceModel {

    /** Serializable UID. */
    private static final long serialVersionUID = 4257498117810293391L;

    /** Reference equatorial radius of the potential. */
    private final double equatorialRadius;

    /** First normalized potential tesseral coefficients array. */
    private final double[][]   C;

    /** Second normalized potential tesseral coefficients array. */
    private final double[][]   S;

    /** Frame for the central body. */
    private final Frame centralBodyFrame;

    /** Number of zonal coefficients. */
    private final int degree;

    /** Number of tesseral coefficients. */
    private final int order;

    /** Creates a new instance.
     *
     * @param centralBodyFrame rotating body frame
     * @param equatorialRadius reference equatorial radius of the potential
     * @param C un-normalized coefficients array (cosine part)
     * @param S un-normalized coefficients array (sine part)
     * @exception IllegalArgumentException if coefficients array do not match
     */
    public DrozinerAttractionModel(final Frame centralBodyFrame,
                                   final double equatorialRadius,
                                   final double[][] C, final double[][] S)
        throws IllegalArgumentException {

        this.equatorialRadius = equatorialRadius;
        this.centralBodyFrame = centralBodyFrame;
        degree = C.length - 1;
        order = C[degree].length - 1;

        if ((C.length != S.length) || (C[C.length - 1].length != S[S.length - 1].length)) {
            OrekitException.throwIllegalArgumentException("potential arrays sizes mismatch (C: {0}x{1}, S: {2}x{3})",
                                                          new Object[] {
                                                              new Integer(C.length),
                                                              new Integer(C[degree].length),
                                                              new Integer(S.length),
                                                              new Integer(S[degree].length)
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
    }

    /** {@inheritDoc} */
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder,
                                final double mu)
        throws OrekitException {
        // Get the position in body frame
        final Transform bodyToInertial = centralBodyFrame.getTransformTo(s.getFrame(), s.getDate());
        final Vector3D posInBody =
            bodyToInertial.getInverse().transformVector(s.getPVCoordinates(mu).getPosition());
        final double xBody = posInBody.getX();
        final double yBody = posInBody.getY();
        final double zBody = posInBody.getZ();

        // Computation of intermediate variables
        final double r12 = xBody * xBody + yBody * yBody;
        final double r1 = Math.sqrt(r12);
        if (r1 <= 10e-2) {
            throw new OrekitException("polar trajectory (distance to polar axis: {0})",
                                      new Object[] {
                                          new Double(r1)
                                      });
        }
        final double r2 = r12 + zBody * zBody;
        final double r  = Math.sqrt(r2);
        if (r <= equatorialRadius) {
            throw new OrekitException("trajectory inside the Brillouin sphere (r = {0})",
                                      new Object[] {
                                          new Double(r)
                                      });
        }
        final double r3    = r2  * r;
        final double aeOnr = equatorialRadius / r;
        final double zOnr  = zBody / r;
        final double r1Onr = r1 / r;

        // Definition of the first acceleration terms
        final double mMuOnr3  = -mu / r3;
        final double xDotDotk = xBody * mMuOnr3;
        final double yDotDotk = yBody * mMuOnr3;

        // Zonal part of acceleration
        double sumA = 0.0;
        double sumB = 0.0;
        double bk1 = zOnr;
        double bk0 = aeOnr * (3 * bk1 * bk1 - 1.0);
        final double[] cC = C[0];
        double jk = -cC[1];

        // first zonal term
        sumA += jk * (2 * aeOnr * bk1 - zOnr * bk0);
        sumB += jk * bk0;

        // other terms
        for (int k = 2; k <= degree; k++) {
            final double bk2 = bk1;
            bk1 = bk0;
            final double p = (1.0 + k) / k;
            bk0 = aeOnr * ((1 + p) * zOnr * bk1 - (k * aeOnr * bk2) / (k - 1));
            final double ak0 = p * aeOnr * bk1 - zOnr * bk0;
            jk = -cC[k];
            sumA += jk * ak0;
            sumB += jk * bk0;
        }

        // calculate the acceleration
        final double p = -sumA / (r1Onr * r1Onr);
        double aX = xDotDotk * p;
        double aY = yDotDotk * p;
        double aZ = mu * sumB / r2;


        // Tessereal-sectorial part of acceleration
        if (order > 0) {
            // latitude and longitude in body frame
            final double cosL = xBody / r1;
            final double sinL = yBody / r1;
            // intermediate variables
            double betaKminus1 = aeOnr;

            double cosjm1L = cosL;
            double sinjm1L = sinL;

            double sinjL = sinL;
            double cosjL = cosL;
            double betaK = 0;
            double Bkj = 0.0;
            double Bkm1j = 3 * betaKminus1 * zOnr * r1Onr;
            double Bkm2j = 0;
            double Bkminus1kminus1 = Bkm1j;

            // first terms
            double Gkj  = C[1][1] * cosL + S[1][1] * sinL;
            double Hkj  = C[1][1] * sinL - S[1][1] * cosL;
            double Akj  = 2 * r1Onr * betaKminus1 - zOnr * Bkminus1kminus1;
            double Dkj  = (Akj + zOnr * Bkminus1kminus1) * 0.5;
            double sum1 = Akj * Gkj;
            double sum2 = Bkminus1kminus1 * Gkj;
            double sum3 = Dkj * Hkj;

            // the other terms
            for (int j = 1; j <= order; ++j) {

                double innerSum1 = 0.0;
                double innerSum2 = 0.0;
                double innerSum3 = 0.0;
                final double[] cJ = C[j];
                final double[] sJ = S[j];

                for (int k = 2; k <= degree; ++k) {

                    if (k < cJ.length) {

                        Gkj = cJ[k] * cosjL + sJ[k] * sinjL;
                        Hkj = cJ[k] * sinjL - sJ[k] * cosjL;

                        if (j <= (k - 2)) {
                            Bkj = aeOnr * (zOnr * Bkm1j * (2.0 * k + 1.0) / (k - j) -
                                           aeOnr * Bkm2j * (k + j) / (k - 1 - j));
                            Akj = aeOnr * Bkm1j * (k + 1.0) / (k - j) - zOnr * Bkj;
                        } else if (j == (k - 1)) {
                            betaK =  aeOnr * (2.0 * k - 1.0) * r1Onr * betaKminus1;
                            Bkj = aeOnr * (2.0 * k + 1.0) * zOnr * Bkm1j - betaK;
                            Akj = aeOnr *  (k + 1.0) * Bkm1j - zOnr * Bkj;
                            betaKminus1 = betaK;
                        } else if (j == k) {
                            Bkj = (2 * k + 1) * aeOnr * r1Onr * Bkminus1kminus1;
                            Akj = (k + 1) * r1Onr * betaK - zOnr * Bkj;
                            Bkminus1kminus1 = Bkj;
                        }

                        Dkj =  (Akj + zOnr * Bkj) * j / (k + 1.0);

                        Bkm2j = Bkm1j;
                        Bkm1j = Bkj;

                        innerSum1 += Akj * Gkj;
                        innerSum2 += Bkj * Gkj;
                        innerSum3 += Dkj * Hkj;
                    }
                }

                sum1 += innerSum1;
                sum2 += innerSum2;
                sum3 += innerSum3;

                sinjL = sinjm1L * cosL + cosjm1L * sinL;
                cosjL = cosjm1L * cosL - sinjm1L * sinL;
                sinjm1L = sinjL;
                cosjm1L = cosjL;
            }

            // compute the acceleration
            final double r2Onr12 = r2 / (r1 * r1);
            final double p1 = r2Onr12 * xDotDotk;
            final double p2 = r2Onr12 * yDotDotk;
            aX += p1 * sum1 - p2 * sum3;
            aY += p2 * sum1 + p1 * sum3;
            aZ -= mu * sum2 / r2;

        }

        // provide the perturbing acceleration to the derivatives adder in inertial frame
        final Vector3D accInInert =
            bodyToInertial.transformVector(new Vector3D(aX, aY, aZ));
        adder.addXYZAcceleration(accInInert.getX(), accInInert.getY(), accInInert.getZ());

    }

    /** {@inheritDoc} */
    public OrekitSwitchingFunction[] getSwitchingFunctions() {
        return new OrekitSwitchingFunction[0];
    }

}

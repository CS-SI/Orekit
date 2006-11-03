package fr.cs.aerospace.orekit.perturbations;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.SynchronizedFrame;
import fr.cs.aerospace.orekit.frames.Transform;
import fr.cs.aerospace.orekit.propagation.EquinoctialGaussEquations;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

/** This class represents the gravitational field of a celestial body.
 * <p>The algorithm implemented in this class has been designed by
 * Andrzej Droziner (Institute of Mathematical Machines, Warsaw) in
 * his 1976 paper: <em>An algorithm for recurrent calculation of gravitational
 * acceleration</em> (artificial satellites, Vol. 12, No 2, June 1977).</p>
 * @version $Id$
 * @author L. Maisonobe
 * @author F. Maussion
 */

public class DrozinerAttractionModel implements ForceModel {

  /**
   * Creates a new instance of CentralBodyPotential.
   * @param mu central body attraction coefficient
   * @param centralBodyFrame frame for the central body
   * @param body rotating body
   * @param equatorialRadius reference equatorial radius of the potential
   * @param C denormalized coefficients array (cosine part)
   * @param S denormalized coefficients array (sine part)
   * @throws OrekitException 
   */
  public DrozinerAttractionModel(double mu, SynchronizedFrame centralBodyFrame, 
                                 double equatorialRadius,
                                 double[][] C, double[][] S)
  throws OrekitException {

    this.mu = mu;
    this.equatorialRadius = equatorialRadius;
    this.C = C;
    this.S = S;
    this.centralBodyFrame = centralBodyFrame;
    this.degree = C.length - 1;
    this.order = C[degree].length-1;
  }

  /**
   * Computes the contribution of the central body potential to the perturbing
   * acceleration, using the Drozyner algorithm. The central part of the
   * acceleration (&mu;/r<sup>2</sup> term) is not computed here, only the
   * <em>perturbing</em> acceleration is considered, not the main part.
   * @param t current date
   * @param pvCoordinates the {@link PVCoordinates}
   * @param adder object where the contribution should be added
   */

  public void addContribution(AbsoluteDate t, PVCoordinates pvCoordinates, 
                              EquinoctialGaussEquations adder)
  throws OrekitException {

    if (C.length == 0) {
      return;
    }

    // Coordinates in centralBodyFrame
    Transform bodyToInertial = centralBodyFrame.getTransformTo(adder.getFrame(), t);
    Vector3D posInBody =
      bodyToInertial.getInverse().transformVector(pvCoordinates.getPosition());
    double xBody = posInBody.getX();
    double yBody = posInBody.getY();
    double zBody = posInBody.getZ();

    // Computation of intermediate variables
    double r12 = xBody * xBody + yBody * yBody;
    double r1 = Math.sqrt(r12);
    if (r1 <= 10e-2) {
      throw new OrekitException("polar trajectory (r1 = {0})",
                                new String[] { Double.toString(r1) });
    }

    double r2 = r12 + zBody * zBody;
    double r  = Math.sqrt(r2);

    if (r <= equatorialRadius) {
      throw new OrekitException("trajectory inside the Brillouin sphere (r = {0})",
                                new String[] { Double.toString(r) });
    }
    double r3    = r2  * r;
    double aeOnr = equatorialRadius / r;
    double zOnr  = zBody/ r;
    double r1Onr = r1 / r;

    // Definition of the first acceleration terms
    double mMuOnr3  = -mu / r3;
    double xDotDotk = xBody * mMuOnr3;
    double yDotDotk = yBody * mMuOnr3;

    // Zonal part of acceleration
    double sumA = 0.0;
    double sumB = 0.0;
    double bk1 = zOnr;
    double bk0 = aeOnr * (3 * bk1 * bk1 - 1.0);
    double jk = -C[1][0];
    
    sumA += jk *( 2 * aeOnr * bk1 - zOnr * bk0);
    sumB += jk * bk0;
    
    for (int k = 2; k <= degree; k++) {
      double bk2 = bk1;
      bk1 = bk0;
      double p = (1.0 + k) / k;
      bk0 = aeOnr * ((1 + p) * zOnr * bk1 - (k * aeOnr * bk2) / (k - 1));
      double ak0 = p * aeOnr * bk1 - zOnr * bk0;
      jk = -C[k][0];
      sumA += jk * ak0;
      sumB += jk * bk0;
    }

    double p = -sumA / (r1Onr * r1Onr);
    double aX = xDotDotk * p;
    double aY = yDotDotk * p;
    double aZ = mu * sumB / r2;

    // Tessereal-sectorial part of acceleration
    if (order>0) {

      double cosL = xBody / r1;
      double sinL = yBody / r1;

      double[] sinl = new double[C.length];
      double[] cosl = new double[C.length];

      cosl[1] = cosL;
      sinl[1] = sinL;

      double Akj = 0;
      double betaKminus1 = aeOnr;
      double betaK = 0;
      double sum1 = 0.0;
      double sum2 = 0.0;
      double sum3 = 0.0;
      double Dkj;
      double innerSum1;
      double innerSum2;
      double innerSum3;



      double Gkj;
      double Hkj;

      double[] Bkm2 = new double[order+1]; 
      // as we only need bkm2 once, it is also used as Bk
      double[] Bkm1 = new double[order+1];

      Bkm1[1] = 3 * betaKminus1 * zOnr * r1Onr;

      double Bkminus1kminus1 = Bkm1[1];
      
      Gkj = C[1][1] * cosL + S[1][1] * sinL;
      Hkj = C[1][1] * sinL - S[1][1] * cosL;   
      
      Akj = 2*r1Onr*betaKminus1-zOnr*Bkminus1kminus1;
      
      Dkj =  (Akj + zOnr * Bkminus1kminus1 ) * 0.5 ;

      sum1 += Akj * Gkj;
      sum2 += Bkminus1kminus1 * Gkj;
      sum3 += Dkj * Hkj;
      
      for (int k = 2; k <= degree; k++) {

        innerSum1 = 0.0;
        innerSum2 = 0.0;
        innerSum3 = 0.0;

        sinl[k] = sinl[k-1]*cosL + cosl[k-1]*sinL;
        cosl[k] = cosl[k-1]*cosL - sinl[k-1]*sinL;      

        for (int j = 1; j <= k; j++) {
          if (j<=order) {          
            Gkj = C[k][j] * cosl[j] + S[k][j] * sinl[j];
            Hkj = C[k][j] * sinl[j] - S[k][j] * cosl[j];          

            if (j <= (k - 2)) {

              Bkm2[j] = aeOnr * ( zOnr * Bkm1[j] * (2.0 * k + 1.0) / (k - j)
                  - aeOnr * Bkm2[j] * (k + j) / (k - 1 - j) ) ;
              Akj = aeOnr * Bkm1[j] * (k + 1.0) / (k - j) - zOnr * Bkm2[j];

            }
            if (j == (k - 1)) {
              betaK =  aeOnr * (2.0 * k - 1.0) * r1Onr * betaKminus1;
              Bkm2[j] = aeOnr * (2.0 * k + 1.0) * zOnr * Bkm1[j] - betaK;
              Akj = aeOnr *  (k + 1.0) * Bkm1[j] - zOnr * Bkm2[j];          
              betaKminus1 = betaK; 
            }
            if (j == k) {
              Bkm2[j] = (2 * k + 1) * aeOnr * r1Onr * Bkminus1kminus1;
              Akj = (k + 1) * r1Onr * betaK - zOnr * Bkm2[j];
              Bkminus1kminus1 = Bkm2[j];
            }
            Dkj =  (Akj + zOnr * Bkm2[j]) * j / (k + 1.0) ;

            innerSum1 += Akj * Gkj;
            innerSum2 += Bkm2[j] * Gkj;
            innerSum3 += Dkj * Hkj;
          }
        }

        double[] temp = Bkm1;
        Bkm1 = Bkm2;
        Bkm2 = temp;

        sum1 += innerSum1;
        sum2 += innerSum2;
        sum3 += innerSum3;

      }

      double r2Onr12 = r2 / (r1 * r1);
      double p1 = r2Onr12 * xDotDotk;
      double p2 = r2Onr12 * yDotDotk;
      aX += p1 * sum1 - p2 * sum3;
      aY += p2 * sum1 + p1 * sum3;
      aZ -= mu * sum2 / r2;
    }

    // provide the perturbing acceleration to the derivatives adder
    Vector3D accInInert = bodyToInertial.transformVector(new Vector3D(aX, aY, aZ));
    adder.addXYZAcceleration(accInInert.getX(), accInInert.getY(), accInInert.getZ());

  }

  public SWF[] getSwitchingFunctions() {
    return null;
  }

  /** Central body attraction coefficient. */
  private double mu;

  /** Reference equatorial radius of the potential. */
  private double equatorialRadius;

  /** First normalized potential tesseral coefficients array. */
  private double[][]   C;

  /** Second normalized potential tesseral coefficients array. */
  private double[][]   S;

  /** Frame for the central body. */  
  private SynchronizedFrame centralBodyFrame;

  /** Number of zonal coefficients */
  private int degree;

  /** Number of tessereal coefficients. */
  private int order;

}

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
    this.centralBodyFrame = centralBodyFrame;
    this.degree = C.length - 1;
    this.order = C[degree].length-1;
    
    // in line seeking optimization 
    if ((C.length == 0)||(S.length == 0)) {
      this.C = new double[0][0];
      this.S = new double[0][0];
    }
    else {
      this.C = new double[C[degree].length][C.length];
      this.S = new double[S[degree].length][S.length];
      for (int i=0; i<=degree; i++) {
        double[] cT = C[i];
        double[] sT = S[i];      
        for (int j=0; j<cT.length; j++) {
          this.C[j][i] = cT[j];
          this.S[j][i] = sT[j];
        }
      }
    }
    
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
    double[] cC = C[0];
    double jk = -cC[1];

    // first zonal term
    sumA += jk *( 2 * aeOnr * bk1 - zOnr * bk0);
    sumB += jk * bk0;

    // other terms
    for (int k = 2; k <= degree; k++) {
      double bk2 = bk1;
      bk1 = bk0;
      double p = (1.0 + k) / k;
      bk0 = aeOnr * ((1 + p) * zOnr * bk1 - (k * aeOnr * bk2) / (k - 1));
      double ak0 = p * aeOnr * bk1 - zOnr * bk0;
      jk = -cC[k];
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

      

      double cosjm1L = cosL;
      double sinjm1L = sinL;

      double sinjL = sinL;
      double cosjL = cosL;

      double Bkj = 0.0;
      double Bkm1j = 3 * betaKminus1 * zOnr * r1Onr;
      double Bkm2j= 0;
      double Bkminus1kminus1 = Bkm1j;

      // first terms
      
      Gkj = C[1][1] * cosL + S[1][1] * sinL;
      Hkj = C[1][1] * sinL - S[1][1] * cosL;   
      
      Akj = 2*r1Onr*betaKminus1-zOnr*Bkminus1kminus1;
      Dkj =  (Akj + zOnr * Bkminus1kminus1 ) * 0.5 ;
      sum1 += Akj * Gkj;
      sum2 += Bkminus1kminus1 * Gkj;
      sum3 += Dkj * Hkj;

      // the other terms      
      for(int j=1; j<=order; j++) {

        innerSum1 = 0.0;
        innerSum2 = 0.0;
        innerSum3 = 0.0;
        
        double[] cJ = C[j];
        double[] sJ = S[j];
        
        for(int k=2; k<=degree; k++) {

          if (k<cJ.length) {
            
            Gkj = cJ[k] * cosjL + sJ[k] * sinjL;
            Hkj = cJ[k] * sinjL - sJ[k] * cosjL;     
          
          if (j <= (k - 2)) {
            Bkj = aeOnr * ( zOnr * Bkm1j * (2.0 * k + 1.0) / (k - j)
                - aeOnr * Bkm2j * (k + j) / (k - 1 - j) ) ;
            Akj = aeOnr * Bkm1j * (k + 1.0) / (k - j) - zOnr * Bkj;
          }
          if (j == (k - 1)) {
            betaK =  aeOnr * (2.0 * k - 1.0) * r1Onr * betaKminus1;
            Bkj = aeOnr * (2.0 * k + 1.0) * zOnr * Bkm1j - betaK;
            Akj = aeOnr *  (k + 1.0) * Bkm1j - zOnr * Bkj;          
            betaKminus1 = betaK; 
          }

          if (j == k) {
            Bkj = (2 * k + 1) * aeOnr * r1Onr * Bkminus1kminus1;
            Akj = (k + 1) * r1Onr * betaK - zOnr * Bkj;
            Bkminus1kminus1 = Bkj;
          }

          Dkj =  (Akj + zOnr * Bkj) * j / (k + 1.0) ;

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

        sinjL = sinjm1L*cosL + cosjm1L*sinL;
        cosjL = cosjm1L*cosL - sinjm1L*sinL; 
        sinjm1L = sinjL;
        cosjm1L = cosjL;
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

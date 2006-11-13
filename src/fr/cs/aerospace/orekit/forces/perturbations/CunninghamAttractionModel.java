package fr.cs.aerospace.orekit.forces.perturbations;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.forces.ForceModel;
import fr.cs.aerospace.orekit.forces.SWF;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.frames.Transform;
import fr.cs.aerospace.orekit.propagation.EquinoctialGaussEquations;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

import org.spaceroots.mantissa.geometry.Vector3D;

/**
 * This class represents the gravitational field of a celestial body.
 * <p>The algorithm implemented in this class has been designed by
 * Leland E. Cunningham (Lockheed Missiles and Space Company, Sunnyvale
 * and Astronomy Department University of California, Berkeley) in
 * his 1969 paper: <em>On the computation of the spherical harmonic
 * terms needed during the numerical integration of the orbital motion
 * of an artificial satellite</em> (Celestial Mechanics 2, 1970).</p>
 * 
 * @author F. Maussion
 * @author L. Maisonobe
 */

public class CunninghamAttractionModel implements ForceModel {

  /** Creates a new instance.
   * 
   * @param mu central body attraction coefficient
   * @param centralBodyFrame rotating body frame
   * @param equatorialRadius reference equatorial radius of the potential
   * @param C un-normalized coefficients array (cosine part)
   * @param S un-normalized coefficients array (sine part)
   * @throws OrekitException 
   */
  public CunninghamAttractionModel(double mu, Frame centralBodyFrame,
                                   double equatorialRadius, double[][] C, double[][] S)
  throws OrekitException {

    if (C.length!=S.length||C[C.length-1].length!=S[S.length-1].length) {
      throw new OrekitException("C and S should have the same size :" +
                                " (C = [{0}][{1}] ; S = [{2}][{3}])",
                                new String[] { Integer.toString(C.length) , 
          Integer.toString(C[degree].length) , 
          Integer.toString(S.length) ,           
          Integer.toString(S[degree].length) });
    }

    this.bodyFrame = centralBodyFrame;
    this.equatorialRadius = equatorialRadius;
    this.mu = mu;
    degree  = C.length - 1;
    order   = C[degree].length - 1;

    if(C.length<1) {
      this.C = new double[1][1];
      this.S = new double[1][1];
    }
    else {
      // invert the arrays (optimization for later "line per line" seeking) 
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

    // do not calculate keplerian evolution
    this.C[0][0] = 0.0;

  }

  /**
   * Compute the contribution of the central body potential to the perturbing
   * acceleration.
   * <p>
   * The central part of the acceleration (mu/r<sup>2</sup> term) is not
   * computed here, only the <em>perturbing</em> acceleration is considered.
   * </p>
   * 
   * @param date current date
   * @param pvCoordinates the position end velocity
   * @param adder object where the contribution should be added
   */
  public void addContribution(AbsoluteDate date, PVCoordinates pvCoordinates,
                              EquinoctialGaussEquations adder)
  throws OrekitException {

    // get the position in body frame
    Transform fromBodyFrame = bodyFrame.getTransformTo(adder.getFrame(), date);
    Transform toBodyFrame   = fromBodyFrame.getInverse();
    Vector3D relative = toBodyFrame.transformPosition(pvCoordinates.getPosition());

    double x = relative.getX();
    double y = relative.getY();
    double z = relative.getZ();

    double x2 = x * x;
    double y2 = y * y;
    double z2 = z * z;
    double r2 = x2 + y2 + z2;
    double r = Math.sqrt(r2);
    if (r <= equatorialRadius) {
      throw new OrekitException("trajectory inside the Brillouin sphere (r = {0})",
                                new String[] { Double.toString(r) });
    }

    // define of some intermediate variables
    double onR2 = 1 / r2;
    double onR3 = onR2 / r;
    double onR4 = onR2 * onR2;

    double cmx   = -x * onR2; 
    double cmy   = -y * onR2;
    double cmz   = -z * onR2;

    double dx   = -2 * cmx;
    double dy   = -2 * cmy;
    double dz   = -2 * cmz;

    // intermediate variables gradients
    // since dcy/dx = dcx/dy, dcz/dx = dcx/dz and dcz/dy = dcy/dz,
    // we reuse the existing variables

    double dcmxdx = (x2 - y2 - z2) * onR4;
    double dcmxdy =  dx * y * onR2;
    double dcmxdz =  dx * z * onR2;
    double dcmydy = (y2 - x2 - z2) * onR4;
    double dcmydz =  dy * z * onR2;
    double dcmzdz = (z2 - x2 - y2) * onR4;

    double ddxdx = -2 * dcmxdx;
    double ddxdy = -2 * dcmxdy;
    double ddxdz = -2 * dcmxdz;
    double ddydy = -2 * dcmydy;
    double ddydz = -2 * dcmydz;
    double ddzdz = -2 * dcmzdz;

    double donr2dx = -dx * onR2;
    double donr2dy = -dy * onR2;
    double donr2dz = -dz * onR2;

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
      double[] Cm = C[m];
      double[] Sm = S[m];

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

        if(n==m) {
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

        if(n==m+1) {
          // calculate the second element of the column
          Vrn = cz*Vrn1;
          Vin = cz*Vin1;          

          gradXVrn = cz * gradXVrn1 + dcxdz * Vrn1;
          gradXVin = cz * gradXVin1 + dcxdz * Vin1;

          gradYVrn = cz * gradYVrn1 + dcydz * Vrn1;
          gradYVin = cz * gradYVin1 + dcydz * Vin1;

          gradZVrn = cz * gradZVrn1 + dczdz * Vrn1;
          gradZVin = cz * gradZVin1 + dczdz * Vin1;

        }        

        if(n>=m+2) {
          // calculate the other elements of the column
          double inv   = 1.0 / (n - m);
          double coeff = n + m - 1.0;

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

        // calculate the acceleration due to the Cnm and Snm coefficients
        // ( as the matrix is inversed, Cnm actually is Cmn )

        if (Cm[n]!=0.0||Sm[n]!=0.0) { // avoid doing the calcul if not necessary
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
    Vector3D acceleration =
      fromBodyFrame.transformVector(new Vector3D(mu * vdX, mu * vdY, mu * vdZ));
    adder.addXYZAcceleration(acceleration.getX(), acceleration.getY(), acceleration.getZ());

  }

  public SWF[] getSwitchingFunctions() {
    return null;
  }

  /** Equatorial radius of the Central Body. */
  private double equatorialRadius;

  /** Central attraction. */
  private double mu;

  /** First normalized potential tesseral coefficients array. */
  private double[][] C;

  /** Second normalized potential tesseral coefficients array. */
  private double[][] S;

  /** Degree of potential. */
  private int degree;

  /** Order of potential. */
  private int order;

  /** Rotating body. */
  private Frame bodyFrame;

}


///** Potential arrays. */
//private double[][] vr;
//private double[][] vi;

///** Potential gradient arrays. */
//private double[][] gradXr;
//private double[][] gradXi;
//private double[][] gradYr;
//private double[][] gradYi;
//private double[][] gradZr;
//private double[][] gradZi;

////initialize term (0,0) of the potential
//double curVr = 1.0 / r;
//double curVi = 0.0;
//vr[0][0] = curVr;
//vi[0][0] = curVi;

////initialize term (0,0) of the potential gradient
//double curGradXr = -x * onR3;
//double curGradXi = 0;
//double curGradYr = -y * onR3;
//double curGradYi = 0;
//double curGradZr = -z * onR3;
//double curGradZi = 0;
//gradXr[0][0] = curGradXr;
//gradXi[0][0] = curGradXi;
//gradYr[0][0] = curGradYr;
//gradYi[0][0] = curGradYi;
//gradZr[0][0] = curGradZr;
//gradZi[0][0] = curGradZi;

//if (order==0) {
//vr[1][0] = -curVr*cz;
//vi[1][0] = curVi;
//gradXr[1][0] = -curGradXr*cz-dcxdz*vr[0][0];
//gradXi[1][0] = curGradXi;
//gradYr[1][0] = -curGradYr*cz-dcydz*vr[0][0];
//gradYi[1][0] = curGradYi;
//gradZr[1][0] = -curGradZr*cz-dczdz*vr[0][0];
//gradZi[1][0] = curGradZi;
//}

////compute the two first diagonals, terms (n,n) and (n,n-1)
//for (int n = 1; n <= order; ++n) {

////update potential coefficients for current iteration
//double prevVr = curVr;
//double prevVi = curVi;
//cx += dx; // cx = (2n-1)*x/r2
//cy += dy;
//cz += dz;

////potential
//curVr = cx * prevVr - cy * prevVi;
//curVi = cy * prevVr + cx * prevVi;
//double[] vrn = vr[n];
//double[] vin = vi[n];
//vrn[n]   = curVr;
//vin[n]   = curVi; 
//vrn[n-1] = cz * prevVr;
//vin[n-1] = cz * prevVi;

////update potential gradient coefficients for current iteration
//double prevGradXr = curGradXr;
//double prevGradXi = curGradXi;
//double prevGradYr = curGradYr;
//double prevGradYi = curGradYi;
//double prevGradZr = curGradZr;
//double prevGradZi = curGradZi;
//dcxdx += ddxdx;
//dcxdy += ddxdy;
//dcxdz += ddxdz;
//dcydy += ddydy;
//dcydz += ddydz;
//dczdz += ddzdz;

////gradient along X
//curGradXr = cx * prevGradXr - cy * prevGradXi + dcxdx * prevVr - dcxdy * prevVi;
//curGradXi = cy * prevGradXr + cx * prevGradXi + dcxdy * prevVr + dcxdx * prevVi;
//double[] xrn = gradXr[n];
//double[] xin = gradXi[n];
//xrn[n]   = curGradXr;
//xin[n]   = curGradXi; 
//xrn[n-1] = cz * prevGradXr + dcxdz * prevVr;
//xin[n-1] = cz * prevGradXi + dcxdz * prevVi;

////gradient along Y
//curGradYr = cx * prevGradYr - cy * prevGradYi + dcxdy * prevVr - dcydy * prevVi;
//curGradYi = cy * prevGradYr + cx * prevGradYi + dcydy * prevVr + dcxdy * prevVi;
//double[] yrn = gradYr[n];
//double[] yin = gradYi[n];
//yrn[n]   = curGradYr;
//yin[n]   = curGradYi; 
//yrn[n-1] = cz * prevGradYr + dcydz * prevVr;
//yin[n-1] = cz * prevGradYi + dcydz * prevVi;

////gradient along Z
//curGradZr = cx * prevGradZr - cy * prevGradZi + dcxdz * prevVr - dcydz * prevVi;
//curGradZi = cy * prevGradZr + cx * prevGradZi + dcydz * prevVr + dcxdz * prevVi;
//double[] zrn = gradZr[n];
//double[] zin = gradZi[n];
//zrn[n]   = curGradZr;
//zin[n]   = curGradZi; 
//zrn[n-1] = cz * prevGradZr + dczdz * prevVr;
//zin[n-1] = cz * prevGradZi + dczdz * prevVi;

//}

////compute the remaining elements, terms (n,m) with m < n-1
//cz    = z * onR2;
//dcxdz = -dx * z * onR2;
//dcydz = -dy * z * onR2;
//dczdz = -(z2 - x2 - y2) * onR4;

//double[] vrn1 = vr[0];
//double[] vin1 = vi[0];
//double[] vrn  = vr[1];
//double[] vin  = vi[1];
//double[] xrn1 = gradXr[0];
//double[] xin1 = gradXi[0];
//double[] xrn  = gradXr[1];
//double[] xin  = gradXi[1];
//double[] yrn1 = gradYr[0];
//double[] yin1 = gradYi[0];
//double[] yrn  = gradYr[1];
//double[] yin  = gradYi[1];
//double[] zrn1 = gradZr[0];
//double[] zin1 = gradZi[0];
//double[] zrn  = gradZr[1];
//double[] zin  = gradZi[1];

//for (int n = 2; n <= degree; ++n) {

////update potential coefficients for current iteration
//cz += dz;

////update potential gradients coefficients for current iteration
//dcxdz += ddxdz;
//dcydz += ddydz;
//dczdz += ddzdz;

//double[] vrn2 = vrn1;
//double[] vin2 = vin1;
//vrn1 = vrn;
//vin1 = vin;
//vrn  = vr[n];
//vin  = vi[n];
//double[] xrn2 = xrn1;
//double[] xin2 = xin1;
//xrn1 = xrn;
//xin1 = xin;
//xrn  = gradXr[n];
//xin  = gradXi[n];
//double[] yrn2 = yrn1;
//double[] yin2 = yin1;
//yrn1 = yrn;
//yin1 = yin;
//yrn  = gradYr[n];
//yin  = gradYi[n];
//double[] zrn2 = zrn1;
//double[] zin2 = zin1;
//zrn1 = zrn;
//zin1 = zin;
//zrn  = gradZr[n];
//zin  = gradZi[n];

//for (int m = 0; m <= n - 2; ++m) {

//double inv   = 1.0 / (n - m);
//double coeff = n + m - 1.0;

////potential
//vrn[m] = (cz * vrn1[m] - coeff * onR2 * vrn2[m]) * inv;
//vin[m] = (cz * vin1[m] - coeff * onR2 * vin2[m]) * inv;

////gradient
//xrn[m] = (cz * xrn1[m] - coeff * onR2 * xrn2[m] + dcxdz * vrn1[m] - coeff * donr2dx * vrn2[m]) * inv;
//xin[m] = (cz * xin1[m] - coeff * onR2 * xin2[m] + dcxdz * vin1[m] - coeff * donr2dx * vin2[m]) * inv;
//yrn[m] = (cz * yrn1[m] - coeff * onR2 * yrn2[m] + dcydz * vrn1[m] - coeff * donr2dy * vrn2[m]) * inv;
//yin[m] = (cz * yin1[m] - coeff * onR2 * yin2[m] + dcydz * vin1[m] - coeff * donr2dy * vin2[m]) * inv;
//zrn[m] = (cz * zrn1[m] - coeff * onR2 * zrn2[m] + dczdz * vrn1[m] - coeff * donr2dz * vrn2[m]) * inv;
//zin[m] = (cz * zin1[m] - coeff * onR2 * zin2[m] + dczdz * vin1[m] - coeff * donr2dz * vin2[m]) * inv;

//}

//}

////compute acceleration in body frame using the potential model coefficients
//double vdX = 0.0;
//double vdY = 0.0;
//double vdZ = 0.0;
//double rn = 1.0;
//for (int n = 1; n <= degree; ++n) {
//rn *= equatorialRadius;
//double[] cn  = C[n];
//double[] sn  = S[n];
//xrn = gradXr[n];
//xin = gradXi[n];
//yrn = gradYr[n];
//yin = gradYi[n];
//zrn = gradZr[n];
//zin = gradZi[n];
//for (int m = 0; m < cn.length; ++m) {
//double cnm = cn[m];
//double snm = sn[m];
//vdX += rn * (cnm * xrn[m] + snm * xin[m]);
//vdY += rn * (cnm * yrn[m] + snm * yin[m]);
//vdZ += rn * (cnm * zrn[m] + snm * zin[m]);
//}
//}






////tests

////Potential V
//double VR44 = x*x*x*x - 6*x*x*y*y + y*y*y*y;
//VR44 = VR44*105*onR4*onR3*onR2;
//double VI44 = 4*x*y*(x*x-y*y);
//VI44 = VI44*105*onR4*onR3*onR2;
//double VR40 = 8*z*z*z*z - 24*z*z*(x*x+y*y) - 3*(x*x+y*y)*(x*x+y*y); 
//VR40 = VR40/8*onR4*onR3*onR2;
//double VR20 = 2*z*z - (x*x+y*y); 
//VR20 = VR20/2*onR3*onR2;
//double VR10 = z; 
//VR10 = z*onR3;
////derivatives : 
//double DVRX33 = -vr[4][4]/2 + vr[4][2]; 
//double DVRX41 = -vr[5][2]/2 + 5.0*4.0*vr[5][0]/2.0; 
//double DVRZ40 = -vr[5][0]*5.0; 
//double DVRZ10 = -vr[2][0]*2.0; 
//double DVRZ32 = -vr[4][2]*2.0;
//double DVRZ33 = -vr[4][3];

////if ( ((VR44-vr[4][4])*10e36>=10e-10)||((VR44-vr[4][4])*10e36<=-10e-10) ) {
////throw new OrekitException("putain fais chier VR44 : {0}",
////new String[] {Double.toString((VR44-vr[4][4])*10e36)});
////}
////if ( ((VI44-vi[4][4])*10e36>=10e-10)||((VI44-vi[4][4])*10e36<=-10e-10) ) {
////throw new OrekitException("putain fais chier VI44 : {0}",
////new String[] {Double.toString((VI44-vi[4][4])*10e36)});
////}    

//if ( ((VR10-vr[1][0])>=10e-30)||((VR10-vr[1][0])<=-10e-30) ) {
//throw new OrekitException("putain fais chier VR10 : {0} ",
//new String[] {Double.toString(VR10-vr[1][0])});
//}
//if ( ((VR20-vr[2][0])*10e21>=10e-10)||((VR20-vr[2][0])*10e21<=-10e-10) ) {
//throw new OrekitException("putain fais chier VR20 : {0} ",
//new String[] {Double.toString( VR20-vr[2][0])});
//}



//if ( ((DVRZ10-gradZr[1][0])*1e21>=1e-10)||((DVRZ10-gradZr[1][0])*1e21<=-1e-10) ) {
//throw new OrekitException("putain fais chier DVRZ10 : {0} " + "  gradZ  : " + "{1}",
//new String[] {Double.toString( DVRZ10),
//Double.toString( gradZr[1][0])  });
//}

//if ( ((DVRZ40-gradZr[4][0])*10e42>=10e-10)||((DVRZ40-gradZr[4][0])*10e42<=-10e-10) ) {
//throw new OrekitException("putain fais chier DVRZ40 : {0} " + "  gradZ  : " + "{1}",
//new String[] {Double.toString( DVRZ40),
//Double.toString( gradZr[4][0])  });
//}

////if ( ((DVRZ33-gradZr[3][3])*1e36>=1e-10)||((DVRZ33-gradZr[3][3])*1e36<=-1e-10) ) {
////throw new OrekitException("putain fais chier DVRZ33 : {0} " + "  gradZ  : " + "{1}",
////new String[] {Double.toString( DVRZ33),
////Double.toString( gradZr[3][3])  });
////}
////if ( ((DVRZ32-gradZr[3][2])*1e34>=1e-10)||((DVRZ32-gradZr[3][2])*1e34<=-1e-10) ) {
////throw new OrekitException("putain fais chier DVRZ32 : {0} " + "  gradZ  : " + "{1}",
////new String[] {Double.toString( DVRZ32),
////Double.toString( gradZr[3][2])  });
////}
////if ( ((DVRX33-gradXr[3][3])*10e35>=10e-10)||((DVRX33-gradXr[3][3])*10e35<=-10e-10) ) {
////throw new OrekitException("putain fais chier DVRX33 : {0} " + "  gradX  : " + "{1}",
////new String[] {Double.toString( DVRX33),
////Double.toString( gradXr[3][3])  });
////}
////if ( ((DVRX41-gradXr[4][1])*10e41>=1e-10)||((DVRX41-gradXr[4][1])*10e41<=-1e-10) ) {
////throw new OrekitException("putain fais chier DVRX41 : {0} " + "  gradX  : " + "{1}",
////new String[] {Double.toString( DVRX41),
////Double.toString( gradXr[4][1])  });
////}




package fr.cs.aerospace.orekit.models.perturbations;


public class JB2006Atmosphere {

  public JB2006Atmosphere() {

  }

  /** Get the local density with initial entries.
   * @param dateMJD date and time, in modified julian days and fraction
   * @param sunRA Right Ascension of Sun (radians)
   * @param sunDecli Declination of Sun (radians)
   * @param satLon Right Ascension of position (radians)
   * @param satLat Geocentric latitude of position (radians)
   * @param satAlt Height of position (m)
   * @param f10 10.7-cm Solar flux (1e<sup>-22</sup>*Watt/(m<sup>2</sup>*Hertz)). 
   *            Tabular time 1.0 day earlier
   * @param f10B 10.7-cm Solar Flux, average 81-day centered on the input time
   * @param ap Geomagnetic planetary 3-hour index A<sub>p</sub>
   *            for a tabular time 6.7 hours earlier
   * @param s10 EUV index (26-34 nm) scaled to F10. Tabular time 1 day earlier.
   * @param s10B UV 81-day averaged centered index
   * @param xm10 MG2 index scaled to F10
   * @param xm10B MG2 81-day ave. centered index. Tabular time 5.0 days earlier.
   * @return total mass-Density at input position (kg/m<sup>3</sup>)
   */
  public double getDensity(double dateMJD,double sunRA, double sunDecli,double satLon,
                           double satLat, double satAlt, double f10,double f10B, double ap ,
                           double s10,double s10B,double xm10,double xm10B) {
    
    satAlt /= 1000;
    
    double[] TC = new double[5];
    double[] ALN = new double[7];
    double[] AL10N = new double[7];

    // Equation (14)

    double tc = 379 + 3.353*f10B + 0.358*(f10-f10B)
                    + 2.094*(s10-s10B) + 0.343*(xm10-xm10B);

    // Equation (15)

    double eta =   0.5 * Math.abs(satLat - sunDecli);
    double theta = 0.5 * Math.abs(satLat + sunDecli);

    // Equation (16)
    double h = satLon - sunRA;
    double tau = h - 0.64577182 + 0.10471976 * Math.sin(h + 0.75049158);
    double solTimeHour = ((h + PI)/DEGRAD)*(24./360.);                   
    if(solTimeHour >= 24) {
      solTimeHour = solTimeHour - 24.;              
    }
    if(solTimeHour < 0) {
      solTimeHour = solTimeHour + 24.;             
    }

    // Equation (17)

    double C = Math.pow(Math.cos(eta),2.5);
    double S = Math.pow(Math.sin(theta),2.5);

    double DF = S + (C - S) * Math.pow(Math.abs(Math.cos(0.5 * tau)),3);
    double TSUBL = tc * (1. + 0.31 * DF);

    // Equation (18)

    double EXPAP = Math.exp(-0.08 * ap);
    double DTG = ap + 100. * (1. - EXPAP);

    // Compute correction to dTc for local solar time and lat correction
    
    double DTCLST = dTc(f10,solTimeHour,satLat,satAlt);

    // Compute the local exospheric temperature.

    double TINF = TSUBL + DTG + DTCLST;
    TEMP[1] = TINF;

    // Equation (9)

    double TSUBX = 444.3807 + 0.02385 * TINF - 392.8292 *
                        Math.exp(-0.0021357 * TINF);

    // Equation (11)

    double GSUBX = 0.054285714 * (TSUBX - 183.);

    // The TC array will be an argument in the call to
    // XLOCAL, which evaluates Equation (10) or Equation (13)

    TC[1] = TSUBX;
    TC[2] = GSUBX;

    //   A AND GSUBX/A OF Equation (13)

    TC[3] = (TINF - TSUBX)/PIOV2;
    TC[4] = GSUBX/TC[3];

    // Equation (5)

    double Z1 = 90.;
    double Z2 = Math.min(satAlt,105.);
    double AL = Math.log(Z2/Z1);
    int N = (int)Math.floor(AL/R1) + 1;
    double ZR = Math.exp(AL/(double)(N));
    double AMBAR1 = XAMBAR(Z1);
    double TLOC1 = XLOCAL(Z1,TC);
    double ZEND = Z1;
    double SUM2 = 0.;
    double AIN = AMBAR1 * XGRAV(Z1)/TLOC1;
    double AMBAR2 =0;
    double TLOC2 =0;
    double Z = 0;
    double GRAVL = 0;
    
    for(int i = 1; i<=N; i++) {
      Z = ZEND;
      ZEND = ZR * Z;
      double DZ = 0.25 * (ZEND-Z);
      double SUM1 = WT[1]*AIN;
      for (int j = 2; j<=5; j++) {
        Z = Z + DZ;
        AMBAR2 = XAMBAR(Z);
        TLOC2 = XLOCAL(Z,TC);
        GRAVL = XGRAV(Z);
        AIN = AMBAR2 * GRAVL/TLOC2;  
        SUM1 = SUM1 + WT[j] * AIN;
      }
      SUM2 = SUM2 + DZ * SUM1;
    }
    double FACT1 = 1000.0/RSTAR;
    RHO = 3.46e-6 * AMBAR2 * TLOC1 * Math.exp(-FACT1*SUM2) /AMBAR1 /TLOC2;

    // Equation (2)

    double ANM = AVOGAD * RHO;
    double AN  = ANM/AMBAR2;

    // Equation (3)

    double FACT2  = ANM/28.960;
    ALN[1] = Math.log(FRAC[1]*FACT2);
    ALN[4] = Math.log(FRAC[3]*FACT2);
    ALN[5] = Math.log(FRAC[4]*FACT2);

    // Equation (4)

    ALN[2] = Math.log(FACT2 * (1. + FRAC[2]) - AN);
    ALN[3] = Math.log(2. * (AN - FACT2));

    if(satAlt <= 105.) {
      TEMP[2] = TLOC2;
      // Put in negligible hydrogen for use in DO-LOOP 13
      ALN[6] = ALN[5] - 25.;
    }
    else {  
      // Equation (6)
      double Z3 = Math.min(satAlt,500.);
      AL = Math.log(Z3/Z);
      N =(int)Math.floor(AL/R2) + 1;
      ZR = Math.exp(AL/(double)(N));
      SUM2 = 0.;
      AIN = GRAVL/TLOC2;

      double TLOC3 = 0;
      for(int I = 1; I<= N; I++) {
        Z = ZEND;
        ZEND = ZR * Z;
        double DZ = 0.25 * (ZEND - Z);
        double SUM1 = WT[1] * AIN;
        for(int J = 2; J<= 5; J++) {
          Z = Z + DZ;
          TLOC3 = XLOCAL(Z,TC);
          GRAVL = XGRAV(Z);
          AIN = GRAVL/TLOC3;
          SUM1 = SUM1 + WT[J] * AIN;
        }
        SUM2 = SUM2 + DZ * SUM1;
      }

      double Z4 = Math.max(satAlt,500.);
      AL = Math.log(Z4/Z);
      double R = R2;
      if (satAlt > 500.) {
        R = R3;
      }
      N = (int)Math.floor(AL/R) + 1;
      ZR = Math.exp(AL/(double)(N));
      double SUM3 = 0.;
      double TLOC4 = 0;
      for(int I = 1; I<= N; I++) {
        Z = ZEND;
        ZEND = ZR * Z;
        double DZ = 0.25 * (ZEND - Z);
        double SUM1 = WT[1] * AIN;
        for(int J = 2; J<= 5; J++) {
          Z = Z + DZ;
          TLOC4 = XLOCAL(Z,TC);
          GRAVL = XGRAV(Z);
          AIN = GRAVL/TLOC4;
          SUM1 = SUM1 + WT[J] * AIN;
        }
        SUM3 = SUM3 + DZ * SUM1;
      }
      double ALTR, HSIGN;
      if (satAlt <= 500.) {
        TEMP[2] = TLOC3;
        ALTR = Math.log(TLOC3/TLOC2);
        FACT2 = FACT1 * SUM2;
        HSIGN = 1.;

      } 
      else {
        TEMP[2] = TLOC4;
        ALTR = Math.log(TLOC4/TLOC2);
        FACT2 = FACT1 * (SUM2 + SUM3);
        HSIGN = -1.;
      }
      for (int I = 1; I<= 5; I++) {
        ALN[I] = ALN[I] - (1.0 + ALPHA[I]) * ALTR - FACT2 * AMW[I];
      }

      // Equation (7) - Note that in CIRA72, AL10T5 = DLOG10(T500)
      double AL10T5 = Math.log10(TINF);
      double ALNH5 = (5.5 * AL10T5 - 39.40) * AL10T5 + 73.13;
      ALN[6] = AL10 * (ALNH5 + 6.) + HSIGN * (Math.log(TLOC4/TLOC3)
          + FACT1 * SUM3 * AMW[6]);

    }

    // Equation (24)  - J70 Seasonal-Latitudinal Variation

    double TRASH = (dateMJD - 36204.) / 365.2422;
    double CAPPHI = TRASH % 1;

    double signum = Math.signum(satLat);
    if (signum == 0) {
      signum = 1;
    }
    double DLRSL = 0.02 * (satAlt - 90.)
    * Math.exp(-0.045 * (satAlt - 90.))
    * signum * Math.sin(TWOPI * CAPPHI+ 1.72)
    * Math.sin(satLat)*Math.sin(satLat);

    // Equation (23) - Computes the semiannual variation
    double DLRSA = 0;
    if (Z<2000.) {
      double D1950 = dateMJD - 33281.;
      // Use new semiannual model DELTA LOG RHO
      DLRSA = SEMIAN(TMOUTD(D1950),satAlt,f10B);
    }

    // Sum the delta-log-rhos and apply to the number densities.
    // In CIRA72 the following equation contains an actual sum,
    // namely DLR = AL10 * (DLRGM + DLRSA + DLRSL)
    // However, for Jacchia 70, there is no DLRGM or DLRSA.

    double DLR = AL10 * (DLRSL + DLRSA);

    for (int i=1; i<=6; i++) {
      ALN[i] = ALN[i] + DLR;
    }

    // Compute mass-density and mean-molecular-weight and
    // convert number density logs from natural to common.

    double SUMN = 0.;
    double SUMNM = 0.;

    for (int I=1; I<=6; I++) {
      AN = Math.exp(ALN[I]);
      SUMN = SUMN + AN;
      SUMNM = SUMNM + AN*AMW[I];
      AL10N[I] = ALN[I]/AL10;

    }

    RHO = SUMNM/AVOGAD;

    // Compute the high altitude exospheric density correction factor

    double FEX = 1.;

    if ((satAlt>=1000.)&(satAlt<1500.)) {
      double ZETA   = (satAlt - 1000.) * 0.002;
      double ZETA2  =  ZETA * ZETA;
      double ZETA3  =  ZETA * ZETA2;
      double F15C   = CHT[1] + CHT[2]*f10B + CHT[3]*1500.
                        + CHT[4]*f10B*1500.;
      double F15C_ZETA = (CHT[3] + CHT[4]*f10B) * 500.;
      double FEX2   = 3. * F15C - F15C_ZETA - 3.;
      double FEX3   = F15C_ZETA - 2. * F15C + 2.;
      FEX    = 1. + FEX2 * ZETA2 + FEX3 * ZETA3;
    }
    if (satAlt >= 1500.) {
      FEX    = CHT[1] + CHT[2]*f10B + CHT[3]*satAlt + CHT[4]*f10B*satAlt;
    }

    // Apply the exospheric density correction factor.

    RHO    = FEX * RHO;

    return RHO;
  }

  /** Compute dTc correction for Jacchia-Bowman model.
   * @param f10 solar flux index
   * @param solTimeHour local solar time (hours 0-23.999)
   * @param satLat sat lat (radians)
   * @param satAlt height (km)
   * @return dTc correction
   */
  private static double dTc(double f10,double solTimeHour,double satLat,double satAlt) {

    double dTc = 0;
    double tx  = solTimeHour/24.;
    double tx2 = tx*tx;
    double tx3 = tx2*tx;
    double tx4 = tx3*tx;
    double tx5 = tx4*tx;
    double ycs = Math.cos(satLat);
    double f   = (f10 - 100.)/100.;
    double h;
    double sum;
    
    // Calculates dTc
    if ((satAlt>=120)&(satAlt<=200)) {
      double DTC200 =
        + CdtSub[17]             + CdtSub[18]*tx*ycs      + CdtSub[19]*tx2*ycs
        + CdtSub[20]*tx3*ycs   + CdtSub[21]*f*ycs       + CdtSub[22]*tx*f*ycs
        + CdtSub[23]*tx2*f*ycs;
      sum = CdtSub[1] + BdtSub[2]*f + CdtSub[3]*tx*f     + CdtSub[4]*tx2*f
      + CdtSub[5]*tx3*f    + CdtSub[6]*tx4*f    + CdtSub[7]*tx5*f
      + CdtSub[8]*tx*ycs     + CdtSub[9]*tx2*ycs  + CdtSub[10]*tx3*ycs
      + CdtSub[11]*tx4*ycs + CdtSub[12]*tx5*ycs + CdtSub[13]*ycs
      + CdtSub[14]*f*ycs     + CdtSub[15]*tx*f*ycs  + CdtSub[16]*tx2*f*ycs;
      double DTC200DZ = sum; 
      double CC  = 3.*DTC200 - DTC200DZ;
      double DD  = DTC200 - CC;
      double ZP  = (satAlt-120.)/80.;
      dTc = CC*ZP*ZP + DD*ZP*ZP*ZP;
    }

    if (satAlt>200.0&satAlt<=240.0) {
      h = (satAlt - 200.)/50.;
      sum = CdtSub[1]*h + BdtSub[2]*f*h + CdtSub[3]*tx*f*h     + CdtSub[4]*tx2*f*h
      + CdtSub[5]*tx3*f*h    + CdtSub[6]*tx4*f*h    + CdtSub[7]*tx5*f*h
      + CdtSub[8]*tx*ycs*h     + CdtSub[9]*tx2*ycs*h  + CdtSub[10]*tx3*ycs*h
      + CdtSub[11]*tx4*ycs*h + CdtSub[12]*tx5*ycs*h + CdtSub[13]*ycs*h
      + CdtSub[14]*f*ycs*h     + CdtSub[15]*tx*f*ycs*h  + CdtSub[16]*tx2*f*ycs*h
      + CdtSub[17]             + CdtSub[18]*tx*ycs      + CdtSub[19]*tx2*ycs
      + CdtSub[20]*tx3*ycs   + CdtSub[21]*f*ycs       + CdtSub[22]*tx*f*ycs
      + CdtSub[23]*tx2*f*ycs;
      dTc = sum;
    }

    if (satAlt>240.0&satAlt<=300.0) {
      h = 40./50.;
      sum = CdtSub[1]*h + BdtSub[2]*f*h + CdtSub[3]*tx*f*h     + CdtSub[4]*tx2*f*h
      + CdtSub[5]*tx3*f*h    + CdtSub[6]*tx4*f*h    + CdtSub[7]*tx5*f*h
      + CdtSub[8]*tx*ycs*h     + CdtSub[9]*tx2*ycs*h  + CdtSub[10]*tx3*ycs*h
      + CdtSub[11]*tx4*ycs*h + CdtSub[12]*tx5*ycs*h + CdtSub[13]*ycs*h
      + CdtSub[14]*f*ycs*h     + CdtSub[15]*tx*f*ycs*h  + CdtSub[16]*tx2*f*ycs*h
      + CdtSub[17]             + CdtSub[18]*tx*ycs      + CdtSub[19]*tx2*ycs
      + CdtSub[20]*tx3*ycs   + CdtSub[21]*f*ycs       + CdtSub[22]*tx*f*ycs
      + CdtSub[23]*tx2*f*ycs;
      double AA = sum;
      double BB = CdtSub[1] + BdtSub[2]*f  + CdtSub[3]*tx*f       + CdtSub[4]*tx2*f
      + CdtSub[5]*tx3*f    + CdtSub[6]*tx4*f    + CdtSub[7]*tx5*f
      + CdtSub[8]*tx*ycs     + CdtSub[9]*tx2*ycs  + CdtSub[10]*tx3*ycs
      + CdtSub[11]*tx4*ycs + CdtSub[12]*tx5*ycs + CdtSub[13]*ycs
      + CdtSub[14]*f*ycs     + CdtSub[15]*tx*f*ycs  + CdtSub[16]*tx2*f*ycs;
      h   = 300./100.;
      sum = BdtSub[1]    + BdtSub[2]*f  + BdtSub[3]*tx*f         + BdtSub[4]*tx2*f
      + BdtSub[5]*tx3*f      + BdtSub[6]*tx4*f      + BdtSub[7]*tx5*f
      + BdtSub[8]*tx*ycs       + BdtSub[9]*tx2*ycs    + BdtSub[10]*tx3*ycs
      + BdtSub[11]*tx4*ycs   + BdtSub[12]*tx5*ycs   + BdtSub[13]*h*ycs
      + BdtSub[14]*tx*h*ycs    + BdtSub[15]*tx2*h*ycs + BdtSub[16]*tx3*h*ycs
      + BdtSub[17]*tx4*h*ycs + BdtSub[18]*tx5*h*ycs + BdtSub[19]*ycs;
      double DTC300 = sum;
      sum = BdtSub[13]*ycs
      + BdtSub[14]*tx*ycs    + BdtSub[15]*tx2*ycs + BdtSub[16]*tx3*ycs
      + BdtSub[17]*tx4*ycs + BdtSub[18]*tx5*ycs;
      double DTC300DZ = sum;
      double CC = 3.*DTC300 - DTC300DZ - 3.*AA - 2.*BB;
      double  DD = DTC300 - AA - BB - CC;
      double ZP  = (satAlt-240.)/60.;
      dTc = AA + BB*ZP + CC*ZP*ZP + DD*ZP*ZP*ZP;
    }

    if (satAlt>300.0&satAlt<=600.0) {
      h   = satAlt/100.;
      sum = BdtSub[1]    + BdtSub[2]*f  + BdtSub[3]*tx*f         + BdtSub[4]*tx2*f
      + BdtSub[5]*tx3*f      + BdtSub[6]*tx4*f      + BdtSub[7]*tx5*f
      + BdtSub[8]*tx*ycs       + BdtSub[9]*tx2*ycs    + BdtSub[10]*tx3*ycs
      + BdtSub[11]*tx4*ycs   + BdtSub[12]*tx5*ycs   + BdtSub[13]*h*ycs
      + BdtSub[14]*tx*h*ycs    + BdtSub[15]*tx2*h*ycs + BdtSub[16]*tx3*h*ycs
      + BdtSub[17]*tx4*h*ycs + BdtSub[18]*tx5*h*ycs + BdtSub[19]*ycs;
      dTc = sum;
    }

    if (satAlt>600.0&satAlt<=800.0) {
      double ZP = (satAlt - 600.)/100.;
      double HP = 600./100.;
      double AA  = BdtSub[1]    + BdtSub[2]*f  + BdtSub[3]*tx*f         + BdtSub[4]*tx2*f
      + BdtSub[5]*tx3*f      + BdtSub[6]*tx4*f      + BdtSub[7]*tx5*f
      + BdtSub[8]*tx*ycs       + BdtSub[9]*tx2*ycs    + BdtSub[10]*tx3*ycs
      + BdtSub[11]*tx4*ycs   + BdtSub[12]*tx5*ycs   + BdtSub[13]*HP*ycs
      + BdtSub[14]*tx*HP*ycs   + BdtSub[15]*tx2*HP*ycs+ BdtSub[16]*tx3*HP*ycs
      + BdtSub[17]*tx4*HP*ycs + BdtSub[18]*tx5*HP*ycs + BdtSub[19]*ycs;
      double BB  = BdtSub[13]*ycs
      + BdtSub[14]*tx*ycs    + BdtSub[15]*tx2*ycs + BdtSub[16]*tx3*ycs
      + BdtSub[17]*tx4*ycs + BdtSub[18]*tx5*ycs;
      double CC  = -(3.*AA+4.*BB)/4.;
      double DD  = (AA+BB)/4.;
      dTc = AA + BB*ZP + CC*ZP*ZP + DD*ZP*ZP*ZP;
    }

    return dTc;
  }

  /** Evaluates Equation (1)
   * @param z
   * @return
   */
  private static double XAMBAR(double z) {
    double dz = z - 100.;
    double amb = CXAMB[7];
    for (int i=6; i>=1; i--) {
      amb = dz * amb + CXAMB[i];
    }
    return amb;
  }

  /**  Evaluates Equation (10) or Equation (13), depending on Z
   * @param Z
   * @param TC
   * @return
   */
  private static double XLOCAL(double Z,double[] TC) {
    double DZ = Z - 125;
    if (DZ <= 0) {
      return ((-9.8204695e-6 * DZ - 7.3039742e-4) * DZ*DZ + 1.0)
      * DZ * TC[2] + TC[1];
    }
    else {
      return TC[1] + TC[3] * Math.atan(TC[4]*DZ*(1 + 4.5e-6*Math.pow(DZ,2.5)));
    }
  }

  /** Evaluates Equation (8) of gravity field
   * @param Z altitude
   * @return the gravity fiels
   */
  private static double XGRAV(double Z) {
    double temp = (1.0 + Z/6356.766);
    return 9.80665/(temp*temp);        
  }

  /** COMPUTE SEMIANNUAL VARIATION (DELTA LOG RHO)
   * @param DAY DAY OF YEAR
   * @param HT HEIGHT (KM)
   * @param F10BAR  AVE 81-DAY CENTERED F10
   */
  private static double SEMIAN (double DAY,double HT,double F10BAR) {

    double F10B = F10BAR;
    double FB2  = F10BAR*F10BAR;

    double HTZ = HT/1000.;
    // SEMIANNUAL AMPLITUDE
    double FZZ = FZM[1] + FZM[2]*F10B  + FZM[3]*F10B*HTZ
    + FZM[4]*F10B*HTZ*HTZ + FZM[5]*F10B*F10B*HTZ
    + FZM[6]*F10B*F10B*HTZ*HTZ;

    double TAU   = (DAY-1.)/365;
    double SIN1P = Math.sin(TWOPI*TAU);
    double COS1P = Math.cos(TWOPI*TAU);
    double SIN2P = Math.sin(2.*TWOPI*TAU);
    double COS2P = Math.cos(2.*TWOPI*TAU);
    double SIN3P = Math.sin(3.*TWOPI*TAU);
    double COS3P = Math.cos(3.*TWOPI*TAU);
    double SIN4P = Math.sin(4.*TWOPI*TAU);
    double COS4P = Math.cos(4.*TWOPI*TAU);
    // SEMIANNUAL PHASE FUNCTION 
    double GTZ = GTM[1] + GTM[2]*SIN1P + GTM[3]*COS1P
    + GTM[4]*SIN2P + GTM[5]*COS2P
    + GTM[6]*SIN3P + GTM[7]*COS3P
    + GTM[8]*SIN4P + GTM[9]*COS4P
    + GTM[10]*F10B + GTM[11]*F10B*SIN1P + GTM[12]*F10B*COS1P
    + GTM[13]*F10B*SIN2P + GTM[14]*F10B*COS2P
    + GTM[15]*F10B*SIN3P + GTM[16]*F10B*COS3P
    + GTM[17]*F10B*SIN4P + GTM[18]*F10B*COS4P
    + GTM[19]*FB2  + GTM[20]*FB2 *SIN1P + GTM[21]*FB2 *COS1P
    + GTM[22]*FB2 *SIN2P + GTM[23]*FB2 *COS2P;

    if (FZZ<1.e-6) {
      FZZ = 1.e-6;
    }
    return FZZ*GTZ;
  } 
  
  /** Compute day of year.
   * @param D1950 (days since 1950)
   * @return the numebr days in year
   */
  private static double TMOUTD(double D1950) {
    int IYDAY = (int)D1950;
    double FRACO = D1950 - IYDAY;
    IYDAY = IYDAY + 364;
    int ITEMP = IYDAY/1461;
    IYDAY = IYDAY - ITEMP*1461; 
    ITEMP = IYDAY/365;
    if (ITEMP>=3) {
      ITEMP = 3;
    }
    IYDAY = IYDAY - 365*ITEMP + 1;
    return IYDAY + FRACO;
  }

  // OUTPUT:

  /** TEMP(1): Exospheric Temperature above Input Position (deg K)
      TEMP(2): Temperature at Input Position (deg K)*/
  public double[] TEMP = new double[3];
  /** Total Mass-Desnity at Input Position (kg/m**3) */
  double RHO;    

  // DATAS :

  /** The alpha are the thermal diffusion coefficients in Eq. (6) */
  private static final double[] ALPHA = new double[] {0,0,0,0,0,-0.38};
  /** ln(10.0) */
  private static final double AL10  = 2.3025851;
  /** Molecular weights in order: N2, O2, O, Ar, He & H */
  private static final double[] AMW = new double[] {0, 28.0134, 31.9988, 15.9994, 39.9480, 4.0026, 1.00797};
  /** Avogadro's number in mks units (molecules/kmol) */
  private static final double AVOGAD = 6.02257e26;

  private static final double TWOPI = 6.2831853;
  private static final double PI    = 3.1415927;
  private static final double PIOV2 = 1.5707963;
  private static final double DEGRAD  =   Math.PI / 180.0;

  /** The FRAC are the assumed sea-level volume fractions in order: N2, O2, Ar, and He */
  private static final double[] FRAC = new double[] {0,0.78110,0.20955,9.3400e-3,1.2890e-5};
  /** Universal gas-constant in mks units (joules/K/kmol) */
  private static final double RSTAR = 8314.32;
  /** Values used to establish height step sizes in the regimes 90km to 105km,
   * 105km to 500km and 500km upward. */
  private static final double R1=0.010;
  private static final double R2=0.025;
  private static final double R3=0.075;
  /** Weights for the Newton-Cotes Five-Point Quad. formula. */
  private static final double[] WT = new double[] {0, 0.311111111111111, 1.422222222222222,
                              0.533333333333333, 1.422222222222222,
                              0.311111111111111};
  /** Coefficients for high altitude density correction */
  private static final double[] CHT= new double[] {0, 0.22,-0.20e-02,0.115e-02,-0.211e-05};

  /** FZ global model values (1978-2004 fit):  */
  private static final double[] FZM = new double[] { 0,
       0.111613e+00,-0.159000e-02, 0.126190e-01,
      -0.100064e-01,-0.237509e-04, 0.260759e-04};

  /** gt global model values 1978-2004 fit: */
  private static final double[] GTM = new double[] {0,
      -0.833646e+00,-0.265450e+00, 0.467603e+00,-0.299906e+00,
      -0.105451e+00,-0.165537e-01,-0.380037e-01,-0.150991e-01,
      -0.541280e-01, 0.119554e-01, 0.437544e-02,-0.369016e-02,
       0.206763e-02,-0.142888e-02,-0.867124e-05, 0.189032e-04,
       0.156988e-03, 0.491286e-03,-0.391484e-04,-0.126854e-04,
       0.134078e-04,-0.614176e-05, 0.343423e-05};
  
  /** XAMBAR relative datas */
  private static final double[] CXAMB = new double[] {0, 28.15204,-8.5586e-2,+1.2840e-4,-1.0056e-5,
      -1.0210e-5,+1.5044e-6,+9.9826e-8};
  /** DTSUB relative datas */
  private static final double[] BdtSub = new double[] { 0,
      -0.457512297e+01, -0.512114909e+01, -0.693003609e+02,
      0.203716701e+03,  0.703316291e+03, -0.194349234e+04,
      0.110651308e+04, -0.174378996e+03,  0.188594601e+04,
      -0.709371517e+04,  0.922454523e+04, -0.384508073e+04,
      -0.645841789e+01,  0.409703319e+02, -0.482006560e+03,
      0.181870931e+04, -0.237389204e+04,  0.996703815e+03,
      0.361416936e+02 };
  /** DTSUB relative datas */
  private static final double[] CdtSub = new double[] { 0,  
      -0.155986211e+02, -0.512114909e+01, -0.693003609e+02,
      0.203716701e+03,  0.703316291e+03, -0.194349234e+04,
      0.110651308e+04, -0.220835117e+03,  0.143256989e+04,
      -0.318481844e+04,  0.328981513e+04, -0.135332119e+04,
      0.199956489e+02, -0.127093998e+02,  0.212825156e+02,
      -0.275555432e+01,  0.110234982e+02,  0.148881951e+03,
      -0.751640284e+03,  0.637876542e+03,  0.127093998e+02,
      -0.212825156e+02,  0.275555432e+01};
  
}

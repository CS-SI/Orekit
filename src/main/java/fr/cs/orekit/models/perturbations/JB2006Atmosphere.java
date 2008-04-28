package fr.cs.orekit.models.perturbations;

/** This is the realization of the Jaccia-Bowman 2006 atmospheric model.
 * <p>
 * It is described in the paper : <br>
 *
 * <a href="http://sol.spacenvironment.net/~JB2006/pubs/JB2006_AIAA-6166_model.pdf">A
 * New Empirical Thermospheric Density Model JB2006 Using New Solar Indices</a><br>
 *
 * <i>Bruce R. Bowman, W. Kent Tobiska and Frank A. Marcos</i> <br>
 *
 * AIAA 2006-6166<br>
 *</p>
 * <p>
 * Two computation methods are proposed to the user :
 * <ul>
 * <li> one OREKIT independent and compliant with initial FORTRAN routine entry values :
 *        {@link #getDensity(double, double, double, double, double, double, double, double, double, double, double, double, double)}. </li>
 * <li> one compliant with OREKIT Atmosphere interface, necessary to the
 *        {@link fr.cs.orekit.propagation.numerical.forces.perturbations.AtmosphericDrag
 *        drag force model} computation. This implementation is realized
 *        by the subclass {@link JB2006AtmosphereModel}</li>
 * </ul>
 * </p>
 * <p>
 * This model provides dense output for all altidudes and positions. Output data are :
 * <ul>
 * <li>Exospheric Temperature above Input Position (deg K)</li>
 * <li>Temperature at Input Position (deg K)</li>
 * <li>Total Mass-Density at Input Position (kg/m<sup>3</sup>)</li>
 * </ul>
 * </p>
 * <p>
 * The model needs geographical and time information to compute general values,
 * but also needs space weather data : mean and daily solar flux, retrieved threw
 * different indices, and planetary geomagnetic incides. <br>
 * More information on these indices can be found on the  <a
 * href="http://sol.spacenvironment.net/~JB2006/JB2006_index.html">
 * official JB2006 website.</a>
 *</p>
 *
 * @author Bruce R Bowman (HQ AFSPC, Space Analysis Division), Feb 2006 : FORTRAN routine
 * @author F. Maussion : JAVA adaptation
 */
public class JB2006Atmosphere {

    // data :

    /** The alpha are the thermal diffusion coefficients in Eq. (6) */
    private static final double[] ALPHA = {
        0, 0, 0, 0, 0, -0.38
    };

    /** ln(10.0) */
    private static final double AL10  = 2.3025851;

    /** Molecular weights in order: N2, O2, O, Ar, He & H */
    private static final double[] AMW = new double[] {0, 28.0134, 31.9988, 15.9994, 39.9480, 4.0026, 1.00797};

    /** Avogadro's number in mks units (molecules/kmol) */
    private static final double AVOGAD = 6.02257e26;

    private static final double TWOPI   = 6.2831853;
    private static final double PI      = 3.1415927;
    private static final double PIOV2   = 1.5707963;
    private static final double DEGRAD  = Math.PI / 180.0;

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
    private static final double[] WT = {
        0, 0.311111111111111, 1.422222222222222,
        0.533333333333333, 1.422222222222222,
        0.311111111111111
    };

    /** Coefficients for high altitude density correction */
    private static final double[] CHT= {
        0, 0.22, -0.20e-02, 0.115e-02, -0.211e-05
    };

    /** FZ global model values (1978-2004 fit):  */
    private static final double[] FZM = {
        0,
        0.111613e+00,-0.159000e-02, 0.126190e-01,
        -0.100064e-01,-0.237509e-04, 0.260759e-04
    };

    /** gt global model values 1978-2004 fit: */
    private static final double[] GTM = {
        0,
        -0.833646e+00,-0.265450e+00, 0.467603e+00,-0.299906e+00,
        -0.105451e+00,-0.165537e-01,-0.380037e-01,-0.150991e-01,
        -0.541280e-01, 0.119554e-01, 0.437544e-02,-0.369016e-02,
        0.206763e-02,-0.142888e-02,-0.867124e-05, 0.189032e-04,
        0.156988e-03, 0.491286e-03,-0.391484e-04,-0.126854e-04,
        0.134078e-04,-0.614176e-05, 0.343423e-05
    };

    /** XAMBAR relative data */
    private static final double[] CXAMB = {
        0,
        28.15204,-8.5586e-2,+1.2840e-4,-1.0056e-5,
        -1.0210e-5,+1.5044e-6,+9.9826e-8
    };

    /** DTSUB relative data */
    private static final double[] BdtSub = {
        0,
        -0.457512297e+01, -0.512114909e+01, -0.693003609e+02,
        0.203716701e+03,  0.703316291e+03, -0.194349234e+04,
        0.110651308e+04, -0.174378996e+03,  0.188594601e+04,
        -0.709371517e+04,  0.922454523e+04, -0.384508073e+04,
        -0.645841789e+01,  0.409703319e+02, -0.482006560e+03,
        0.181870931e+04, -0.237389204e+04,  0.996703815e+03,
        0.361416936e+02
    };

    /** DTSUB relative data */
    private static final double[] CdtSub = {
        0,
        -0.155986211e+02, -0.512114909e+01, -0.693003609e+02,
        0.203716701e+03,  0.703316291e+03, -0.194349234e+04,
        0.110651308e+04, -0.220835117e+03,  0.143256989e+04,
        -0.318481844e+04,  0.328981513e+04, -0.135332119e+04,
        0.199956489e+02, -0.127093998e+02,  0.212825156e+02,
        -0.275555432e+01,  0.110234982e+02,  0.148881951e+03,
        -0.751640284e+03,  0.637876542e+03,  0.127093998e+02,
        -0.212825156e+02,  0.275555432e+01
    };

    private static double[] TC    = new double[5];
    private static double[] ALN   = new double[7];
    private static double[] AL10N = new double[7];

    /** TEMP(1): Exospheric Temperature above Input Position (deg K)
      * TEMP(2): Temperature at Input Position (deg K)*/
    private double[] temp = new double[3];

    /** Total Mass-Density at Input Position (kg/m<sup>3</sup>) */
    private double rho;

    /** Simple constructor. */
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
     * @param f10B 10.7-cm Solar Flux, averaged 81-day centered on the input time
     * @param ap Geomagnetic planetary 3-hour index A<sub>p</sub>
     *            for a tabular time 6.7 hours earlier
     * @param s10 EUV index (26-34 nm) scaled to F10. Tabular time 1 day earlier.
     * @param s10B UV 81-day averaged centered index
     * @param xm10 MG2 index scaled to F10
     * @param xm10B MG2 81-day ave. centered index. Tabular time 5.0 days earlier.
     * @return total mass-Density at input position (kg/m<sup>3</sup>)
     */
    public double getDensity(double dateMJD, double sunRA, double sunDecli, double satLon,
                             double satLat, double satAlt, double f10, double f10B, double ap,
                             double s10, double s10B, double xm10, double xm10B) {

        satAlt /= 1000.0;

        // Equation (14)

        final double tc = 379 + 3.353 * f10B + 0.358 * (f10 - f10B) +
                          2.094 * (s10 - s10B) + 0.343 * (xm10 - xm10B);

        // Equation (15)

        final double eta =   0.5 * Math.abs(satLat - sunDecli);
        final double theta = 0.5 * Math.abs(satLat + sunDecli);

        // Equation (16)
        final double h     = satLon - sunRA;
        final double tau   = h - 0.64577182 + 0.10471976 * Math.sin(h + 0.75049158);
        double solTimeHour = ((h + PI)/DEGRAD)*(24./360.);
        if (solTimeHour >= 24) {
            solTimeHour = solTimeHour - 24.;
        }
        if(solTimeHour < 0) {
            solTimeHour = solTimeHour + 24.;
        }

        // Equation (17)
        final double C = Math.pow(Math.cos(eta), 2.5);
        final double S = Math.pow(Math.sin(theta), 2.5);
        final double tmp = Math.abs(Math.cos(0.5 * tau));
        final double DF = S + (C - S) * tmp * tmp * tmp;
        final double TSUBL = tc * (1. + 0.31 * DF);

        // Equation (18)
        final double EXPAP = Math.exp(-0.08 * ap);
        final double DTG = ap + 100. * (1. - EXPAP);

        // Compute correction to dTc for local solar time and lat correction
        final double DTCLST = dTc(f10, solTimeHour, satLat, satAlt);

        // Compute the local exospheric temperature.
        final double TINF = TSUBL + DTG + DTCLST;
        temp[1] = TINF;

        // Equation (9)
        final double TSUBX = 444.3807 + 0.02385 * TINF - 392.8292 * Math.exp(-0.0021357 * TINF);

        // Equation (11)
        final double GSUBX = 0.054285714 * (TSUBX - 183.);

        // The TC array will be an argument in the call to
        // XLOCAL, which evaluates Equation (10) or Equation (13)
        TC[1] = TSUBX;
        TC[2] = GSUBX;

        //   A AND GSUBX/A OF Equation (13)
        TC[3] = (TINF - TSUBX)/PIOV2;
        TC[4] = GSUBX/TC[3];

        // Equation (5)
        final double Z1 = 90.;
        final double Z2 = Math.min(satAlt, 105.0);
        double AL = Math.log(Z2 / Z1);
        int N = (int) Math.floor(AL / R1) + 1;
        double ZR = Math.exp(AL / N);
        final double AMBAR1 = xAmbar(Z1);
        final double TLOC1 = xLocal(Z1, TC);
        double ZEND   = Z1;
        double SUM2   = 0.;
        double AIN    = AMBAR1 * xGrav(Z1) / TLOC1;
        double AMBAR2 = 0;
        double TLOC2  = 0;
        double Z      = 0;
        double GRAVL  = 0;

        for (int i = 1; i <= N; ++i) {
            Z = ZEND;
            ZEND = ZR * Z;
            final double DZ = 0.25 * (ZEND - Z);
            double SUM1 = WT[1] * AIN;
            for (int j = 2; j <= 5; ++j) {
                Z += DZ;
                AMBAR2 = xAmbar(Z);
                TLOC2  = xLocal(Z, TC);
                GRAVL  = xGrav(Z);
                AIN    = AMBAR2 * GRAVL / TLOC2;
                SUM1  += WT[j] * AIN;
            }
            SUM2 = SUM2 + DZ * SUM1;
        }
        final double FACT1 = 1000.0 / RSTAR;
        rho = 3.46e-6 * AMBAR2 * TLOC1 * Math.exp(-FACT1 * SUM2) / (AMBAR1 * TLOC2);

        // Equation (2)
        final double ANM = AVOGAD * rho;
        double AN  = ANM / AMBAR2;

        // Equation (3)
        double FACT2  = ANM / 28.960;
        ALN[1] = Math.log(FRAC[1] * FACT2);
        ALN[4] = Math.log(FRAC[3] * FACT2);
        ALN[5] = Math.log(FRAC[4] * FACT2);

        // Equation (4)
        ALN[2] = Math.log(FACT2 * (1. + FRAC[2]) - AN);
        ALN[3] = Math.log(2. * (AN - FACT2));

        if (satAlt <= 105.0) {
            temp[2] = TLOC2;
            // Put in negligible hydrogen for use in DO-LOOP 13
            ALN[6] = ALN[5] - 25.0;
        } else {
            // Equation (6)
            final double Z3 = Math.min(satAlt, 500.0);
            AL   = Math.log(Z3 / Z);
            N    = (int) Math.floor(AL / R2) + 1;
            ZR   = Math.exp(AL / N);
            SUM2 = 0.;
            AIN  = GRAVL / TLOC2;

            double TLOC3 = 0;
            for (int I = 1; I <= N; ++I) {
                Z = ZEND;
                ZEND = ZR * Z;
                final double DZ = 0.25 * (ZEND - Z);
                double SUM1 = WT[1] * AIN;
                for (int J = 2; J <= 5; ++J) {
                    Z    += DZ;
                    TLOC3 = xLocal(Z, TC);
                    GRAVL = xGrav(Z);
                    AIN   = GRAVL / TLOC3;
                    SUM1  = SUM1 + WT[J] * AIN;
                }
                SUM2 = SUM2 + DZ * SUM1;
            }

            final double Z4 = Math.max(satAlt, 500.0);
            AL = Math.log(Z4 / Z);
            double R = R2;
            if (satAlt > 500.0) {
                R = R3;
            }
            N = (int) Math.floor(AL / R) + 1;
            ZR = Math.exp(AL / N);
            double SUM3 = 0.;
            double TLOC4 = 0;
            for (int I = 1; I <= N; ++I) {
                Z = ZEND;
                ZEND = ZR * Z;
                final double DZ = 0.25 * (ZEND - Z);
                double SUM1 = WT[1] * AIN;
                for (int J = 2; J <= 5; ++J) {
                    Z    += DZ;
                    TLOC4 = xLocal(Z, TC);
                    GRAVL = xGrav(Z);
                    AIN   = GRAVL / TLOC4;
                    SUM1  = SUM1 + WT[J] * AIN;
                }
                SUM3 = SUM3 + DZ * SUM1;
            }
            double ALTR;
            double HSIGN;
            if (satAlt <= 500.) {
                temp[2] = TLOC3;
                ALTR = Math.log(TLOC3 / TLOC2);
                FACT2 = FACT1 * SUM2;
                HSIGN = 1.0;

            } else {
                temp[2] = TLOC4;
                ALTR = Math.log(TLOC4 / TLOC2);
                FACT2 = FACT1 * (SUM2 + SUM3);
                HSIGN = -1.0;
            }
            for (int I = 1; I <= 5; ++I) {
                ALN[I] = ALN[I] - (1.0 + ALPHA[I]) * ALTR - FACT2 * AMW[I];
            }

            // Equation (7) - Note that in CIRA72, AL10T5 = DLOG10(T500)
            final double AL10T5 = Math.log(TINF) / Math.log(10);
            final double ALNH5 = (5.5 * AL10T5 - 39.40) * AL10T5 + 73.13;
            ALN[6] = AL10 * (ALNH5 + 6.) + HSIGN * (Math.log(TLOC4 / TLOC3) + FACT1 * SUM3 * AMW[6]);

        }

        // Equation (24)  - J70 Seasonal-Latitudinal Variation
        final double TRASH = (dateMJD - 36204.0) / 365.2422;
        final double CAPPHI = TRASH % 1;
        final int signum = (satLat >= 0) ? 1 : -1;
        final double sinLat = Math.sin(satLat);
        final double DLRSL = 0.02 * (satAlt - 90.) * Math.exp(-0.045 * (satAlt - 90.)) *
                             signum * Math.sin(TWOPI * CAPPHI + 1.72) * sinLat * sinLat;

        // Equation (23) - Computes the semiannual variation
        double DLRSA = 0;
        if (Z < 2000.0) {
            final double D1950 = dateMJD - 33281.0;
            // Use new semiannual model DELTA LOG RHO
            DLRSA = semian(dayOfYear(D1950), satAlt, f10B);
        }

        // Sum the delta-log-rhos and apply to the number densities.
        // In CIRA72 the following equation contains an actual sum,
        // namely DLR = AL10 * (DLRGM + DLRSA + DLRSL)
        // However, for Jacchia 70, there is no DLRGM or DLRSA.
        final double DLR = AL10 * (DLRSL + DLRSA);
        for (int i = 1; i <= 6; ++i) {
            ALN[i] += DLR;
        }

        // Compute mass-density and mean-molecular-weight and
        // convert number density logs from natural to common.

        double SUMN  = 0.0;
        double SUMNM = 0.0;

        for (int I = 1; I <= 6; ++I) {
            AN = Math.exp(ALN[I]);
            SUMN += AN;
            SUMNM += AN * AMW[I];
            AL10N[I] = ALN[I] / AL10;
        }

        rho = SUMNM / AVOGAD;

        // Compute the high altitude exospheric density correction factor
        double FEX = 1.0;
        if ((satAlt >= 1000.0) && (satAlt < 1500.0)) {
            final double ZETA   = (satAlt - 1000.) * 0.002;
            final double ZETA2  =  ZETA * ZETA;
            final double ZETA3  =  ZETA * ZETA2;
            final double F15C   = CHT[1] + CHT[2] * f10B + CHT[3] * 1500.0 + CHT[4] * f10B * 1500.0;
            final double F15C_ZETA = (CHT[3] + CHT[4] * f10B) * 500.0;
            final double FEX2   = 3.0 * F15C - F15C_ZETA - 3.0;
            final double FEX3   = F15C_ZETA - 2.0 * F15C + 2.0;
            FEX    = 1.0 + FEX2 * ZETA2 + FEX3 * ZETA3;
        }
        if (satAlt >= 1500.0) {
            FEX    = CHT[1] + CHT[2] * f10B + CHT[3] * satAlt + CHT[4] * f10B * satAlt;
        }

        // Apply the exospheric density correction factor.
        rho  *= FEX;

        return rho;

    }

    /** Compute dTc correction for Jacchia-Bowman model.
     * @param f10 solar flux index
     * @param solTimeHour local solar time (hours 0-23.999)
     * @param satLat sat lat (radians)
     * @param satAlt height (km)
     * @return dTc correction
     */
    private static double dTc(double f10, double solTimeHour, double satLat, double satAlt) {

        double dTc = 0;
        final double tx  = solTimeHour / 24.0;
        final double tx2 = tx * tx;
        final double tx3 = tx2 * tx;
        final double tx4 = tx3 * tx;
        final double tx5 = tx4 * tx;
        final double ycs = Math.cos(satLat);
        final double f   = (f10 - 100.0) / 100.0;
        double h;
        double sum;

        // Calculates dTc
        if ((satAlt >= 120) && (satAlt <= 200)) {
            final double DTC200 = CdtSub[17] + CdtSub[18] * tx * ycs + CdtSub[19] * tx2 * ycs +
                                  CdtSub[20] * tx3 * ycs + CdtSub[21] * f * ycs + CdtSub[22] * tx * f * ycs +
                                  CdtSub[23] * tx2 * f * ycs;
            sum = CdtSub[1] + BdtSub[2] * f + CdtSub[3] * tx * f + CdtSub[4] * tx2 * f +
                  CdtSub[5] * tx3 * f + CdtSub[6] * tx4 * f + CdtSub[7] * tx5 * f +
                  CdtSub[8] * tx * ycs + CdtSub[9] * tx2 * ycs + CdtSub[10] * tx3 * ycs +
                  CdtSub[11] * tx4 * ycs + CdtSub[12] * tx5 * ycs + CdtSub[13] * ycs +
                  CdtSub[14] * f * ycs + CdtSub[15] * tx * f * ycs  + CdtSub[16] * tx2 * f * ycs;
            final double DTC200DZ = sum;
            final double CC  = 3.0 * DTC200 - DTC200DZ;
            final double DD  = DTC200 - CC;
            final double ZP  = (satAlt - 120.0) / 80.0;
            dTc = CC * ZP * ZP + DD * ZP * ZP * ZP;
        }

        if ((satAlt > 200.0) && (satAlt <= 240.0)) {
            h = (satAlt - 200.0) / 50.0;
            sum = CdtSub[1] * h + BdtSub[2] * f * h + CdtSub[3] * tx * f * h + CdtSub[4] * tx2 * f * h +
                  CdtSub[5] * tx3 * f * h + CdtSub[6] * tx4 * f * h + CdtSub[7] * tx5 * f * h +
                  CdtSub[8] * tx * ycs * h + CdtSub[9] * tx2 * ycs * h + CdtSub[10] * tx3 * ycs * h +
                  CdtSub[11] * tx4 * ycs * h + CdtSub[12] * tx5 * ycs * h + CdtSub[13] * ycs * h +
                  CdtSub[14] * f * ycs * h + CdtSub[15] * tx * f * ycs * h  + CdtSub[16] * tx2 * f * ycs * h +
                  CdtSub[17] + CdtSub[18] * tx * ycs + CdtSub[19] * tx2 * ycs +
                  CdtSub[20] * tx3 * ycs + CdtSub[21] * f * ycs + CdtSub[22] * tx * f * ycs +
                  CdtSub[23] * tx2 * f * ycs;
            dTc = sum;
        }

        if ((satAlt > 240.0) && (satAlt <= 300.0)) {
            h = 40.0 / 50.0;
            sum = CdtSub[1] * h + BdtSub[2] * f * h + CdtSub[3] * tx * f * h + CdtSub[4] * tx2 * f * h +
                  CdtSub[5] * tx3 * f * h + CdtSub[6] * tx4 * f * h + CdtSub[7] * tx5 * f * h +
                  CdtSub[8] * tx * ycs * h + CdtSub[9] * tx2 * ycs * h + CdtSub[10] * tx3 * ycs * h +
                  CdtSub[11] * tx4 * ycs * h + CdtSub[12] * tx5 * ycs * h + CdtSub[13] * ycs * h +
                  CdtSub[14] * f * ycs * h + CdtSub[15] * tx * f * ycs * h  + CdtSub[16] * tx2 * f * ycs * h +
                  CdtSub[17] + CdtSub[18] * tx * ycs + CdtSub[19] * tx2 * ycs +
                  CdtSub[20] * tx3 * ycs + CdtSub[21] * f * ycs + CdtSub[22] * tx * f * ycs +
                  CdtSub[23] * tx2 * f * ycs;
            final double AA = sum;
            final double BB = CdtSub[1] + BdtSub[2] * f + CdtSub[3] * tx * f + CdtSub[4] * tx2 * f +
                        CdtSub[5] * tx3 * f + CdtSub[6] * tx4 * f + CdtSub[7] * tx5 * f +
                        CdtSub[8] * tx * ycs + CdtSub[9] * tx2 * ycs + CdtSub[10] * tx3 * ycs +
                        CdtSub[11] * tx4 * ycs + CdtSub[12] * tx5 * ycs + CdtSub[13] * ycs +
                        CdtSub[14] * f * ycs + CdtSub[15] * tx * f * ycs + CdtSub[16] * tx2 * f * ycs;
            h   = 300.0 / 100.0;
            sum = BdtSub[1] + BdtSub[2] * f  + BdtSub[3] * tx * f         + BdtSub[4] * tx2 * f +
                  BdtSub[5] * tx3 * f      + BdtSub[6] * tx4 * f      + BdtSub[7] * tx5 * f +
                  BdtSub[8] * tx * ycs       + BdtSub[9] * tx2 * ycs    + BdtSub[10] * tx3 * ycs +
                  BdtSub[11] * tx4 * ycs   + BdtSub[12] * tx5 * ycs   + BdtSub[13] * h * ycs +
                  BdtSub[14] * tx * h * ycs    + BdtSub[15] * tx2 * h * ycs + BdtSub[16] * tx3 * h * ycs +
                  BdtSub[17] * tx4 * h * ycs + BdtSub[18] * tx5 * h * ycs + BdtSub[19] * ycs;
            final double DTC300 = sum;
            sum = BdtSub[13] * ycs +
                  BdtSub[14] * tx * ycs    + BdtSub[15] * tx2 * ycs + BdtSub[16] * tx3 * ycs +
                  + BdtSub[17] * tx4 * ycs + BdtSub[18] * tx5 * ycs;
            final double DTC300DZ = sum;
            final double CC = 3.0 * DTC300 - DTC300DZ - 3.0 * AA - 2.0 * BB;
            final double  DD = DTC300 - AA - BB - CC;
            final double ZP  = (satAlt - 240.0) / 60.0;
            dTc = AA + BB * ZP + CC * ZP * ZP + DD * ZP * ZP * ZP;
        }

        if ((satAlt > 300.0) && (satAlt <= 600.0)) {
            h   = satAlt / 100.0;
            sum = BdtSub[1]    + BdtSub[2] * f  + BdtSub[3] * tx * f         + BdtSub[4] * tx2 * f +
                  BdtSub[5] * tx3 * f      + BdtSub[6] * tx4 * f      + BdtSub[7] * tx5 * f +
                  BdtSub[8] * tx * ycs       + BdtSub[9] * tx2 * ycs    + BdtSub[10] * tx3 * ycs +
                  BdtSub[11] * tx4 * ycs   + BdtSub[12] * tx5 * ycs   + BdtSub[13] * h * ycs +
                  BdtSub[14] * tx * h * ycs    + BdtSub[15] * tx2 * h * ycs + BdtSub[16] * tx3 * h * ycs +
                  BdtSub[17] * tx4 * h * ycs + BdtSub[18] * tx5 * h * ycs + BdtSub[19] * ycs;
            dTc = sum;
        }

        if ((satAlt > 600.0) && (satAlt <= 800.0)) {
            final double ZP = (satAlt - 600.0) / 100.0;
            final double HP = 600.0 / 100.0;
            final double AA  = BdtSub[1]    + BdtSub[2] * f  + BdtSub[3] * tx * f         + BdtSub[4] * tx2 * f +
                               BdtSub[5] * tx3 * f      + BdtSub[6] * tx4 * f      + BdtSub[7] * tx5 * f +
                               BdtSub[8] * tx * ycs       + BdtSub[9] * tx2 * ycs    + BdtSub[10] * tx3 * ycs +
                               BdtSub[11] * tx4 * ycs   + BdtSub[12] * tx5 * ycs   + BdtSub[13] * HP * ycs +
                               BdtSub[14] * tx * HP * ycs   + BdtSub[15] * tx2 * HP * ycs+ BdtSub[16] * tx3 * HP * ycs +
                               BdtSub[17] * tx4 * HP * ycs + BdtSub[18] * tx5 * HP * ycs + BdtSub[19] * ycs;
            final double BB  = BdtSub[13] * ycs +
                               BdtSub[14] * tx * ycs    + BdtSub[15] * tx2 * ycs + BdtSub[16] * tx3 * ycs +
                               BdtSub[17] * tx4 * ycs + BdtSub[18] * tx5 * ycs;
            final double CC  = -(3.0 * AA + 4.0 * BB) / 4.0;
            final double DD  = (AA + BB) / 4.0;
            dTc = AA + BB * ZP + CC * ZP * ZP + DD * ZP * ZP * ZP;
        }

        return dTc;
    }

    /** Evaluates Equation (1)
     * @param z
     * @return
     */
    private static double xAmbar(double z) {
        final double dz = z - 100.;
        double amb = CXAMB[7];
        for (int i = 6; i >= 1; --i) {
            amb = dz * amb + CXAMB[i];
        }
        return amb;
    }

    /**  Evaluates Equation (10) or Equation (13), depending on Z
     * @param Z
     * @param TC
     * @return result of equation (10)
     */
    private static double xLocal(double Z,double[] TC) {
        final double DZ = Z - 125;
        if (DZ <= 0) {
            return ((-9.8204695e-6 * DZ - 7.3039742e-4) * DZ * DZ + 1.0) * DZ * TC[2] + TC[1];
        } else {
            return TC[1] + TC[3] * Math.atan(TC[4] * DZ * (1 + 4.5e-6 * Math.pow(DZ, 2.5)));
        }
    }

    /** Evaluates Equation (8) of gravity field
     * @param Z altitude
     * @return the gravity field
     */
    private static double xGrav(double Z) {
        final double temp = 1.0 + Z / 6356.766;
        return 9.80665 / (temp * temp);
    }

    /** COMPUTE SEMIANNUAL VARIATION (DELTA LOG RHO)
     * @param day DAY OF YEAR
     * @param height HEIGHT (KM)
     * @param f10Bar2  AVE 81-DAY CENTERED F10
     */
    private static double semian (final double day, final double height, final double f10Bar) {

        final double f10Bar2 = f10Bar * f10Bar;
        final double htz = height / 1000.0;

        // SEMIANNUAL AMPLITUDE
        double fzz = FZM[1] + FZM[2] * f10Bar  + FZM[3] * f10Bar * htz +
                     FZM[4] * f10Bar * htz * htz + FZM[5] * f10Bar * f10Bar * htz +
                     FZM[6] * f10Bar * f10Bar * htz * htz;

        // SEMIANNUAL PHASE FUNCTION
        final double tau   = TWOPI * (day - 1.0) / 365;
        final double sin1P = Math.sin(tau);
        final double cos1P = Math.cos(tau);
        final double sin2P = Math.sin(2.0 * tau);
        final double cos2P = Math.cos(2.0 * tau);
        final double sin3P = Math.sin(3.0 * tau);
        final double cos3P = Math.cos(3.0 * tau);
        final double sin4P = Math.sin(4.0 * tau);
        final double cos4P = Math.cos(4.0 * tau);
        final double gtz = GTM[1] + GTM[2] * sin1P + GTM[3] * cos1P +
                           GTM[4] * sin2P + GTM[5] * cos2P +
                           GTM[6] * sin3P + GTM[7] * cos3P +
                           GTM[8] * sin4P + GTM[9] * cos4P +
                           GTM[10] * f10Bar + GTM[11] * f10Bar * sin1P + GTM[12] * f10Bar * cos1P +
                           GTM[13] * f10Bar * sin2P + GTM[14] * f10Bar * cos2P +
                           GTM[15] * f10Bar * sin3P + GTM[16] * f10Bar * cos3P +
                           GTM[17] * f10Bar * sin4P + GTM[18] * f10Bar * cos4P +
                           GTM[19] * f10Bar2  + GTM[20] * f10Bar2  * sin1P + GTM[21] * f10Bar2  * cos1P +
                           GTM[22] * f10Bar2  * sin2P + GTM[23] * f10Bar2  * cos2P;

        return Math.max(1.0e-6, fzz) * gtz;

    }

    /** Compute day of year.
     * @param d1950 (days since 1950)
     * @return the number days in year
     */
    private static double dayOfYear(double d1950) {

        int iyday = (int) d1950;
        final double frac = d1950 - iyday;
        iyday = iyday + 364;

        int itemp = iyday / 1461;

        iyday = iyday - itemp * 1461;
        itemp = iyday / 365;
        if (itemp >= 3) {
            itemp = 3;
        }
        iyday = iyday - 365 * itemp + 1;
        return iyday + frac;
    }

    // OUTPUT:

    /** Get the exospheric temperature above input position.
     * {@link #getDensity(double, double, double, double, double, double, double, double, double, double, double, double, double)}
     * <b> must </b> must be called before calling this function.
     * @return the exospheric temperature (deg K)
     */
    public double getExosphericTemp() {
        return temp[1];
    }

    /** Get the temperature at input position.
     * {@link #getDensity(double, double, double, double, double, double, double, double, double, double, double, double, double)}
     * <b> must </b> must be called before calling this function.
     * @return the local temperature (deg K)
     */
    public double getLocalTemp() {
        return temp[2];
    }

}

package fr.cs.aerospace.orekit.models.perturbations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import org.spaceroots.mantissa.geometry.Vector3D;
import fr.cs.aerospace.orekit.FindFile;
import fr.cs.aerospace.orekit.Utils;
import fr.cs.aerospace.orekit.bodies.BodyShape;
import fr.cs.aerospace.orekit.bodies.GeodeticPoint;
import fr.cs.aerospace.orekit.bodies.ThirdBody;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;

public class DTM2000Atmosphere implements Atmosphere {

  public DTM2000Atmosphere(DTM2000InputParameters parameters, 
                             ThirdBody sun, BodyShape earth, Frame earthFixed) {

    this.earth = earth;
    this.sun = sun;
    this.inputParams = parameters;
    this.bodyFrame = earthFixed;    
    if(isFirstTime) {
      readcoefficients();
      isFirstTime = false;
    }
  }

  /** Store the DTM model coefficients in internal arrays */
  private void readcoefficients() {
    File rootDir;
    InputStream in;
    try {
      rootDir = FindFile.find(dtm2000, "/");
      in = new FileInputStream(rootDir.getAbsolutePath());

      BufferedReader r = new BufferedReader(new InputStreamReader(in));
      r.readLine();
      r.readLine();
      for (String line = r.readLine(); line != null; line = r.readLine()) {
        int num = Integer.parseInt(line.substring(0,4));
        
        tt[num] = Double.parseDouble(line.substring(4,17).replace('E','e')); 
        h[num] = Double.parseDouble(line.substring(0,4));
        he[num] = Double.parseDouble(line.substring(0,4));
        o[num] = Double.parseDouble(line.substring(0,4));
        az2[num] = Double.parseDouble(line.substring(0,4));
        o2[num] = Double.parseDouble(line.substring(0,4));
        az[num] = Double.parseDouble(line.substring(0,4));
        t0[num] = Double.parseDouble(line.substring(0,4));
        tp[num] = Double.parseDouble(line.substring(0,4));
      }
    } catch (FileNotFoundException e) {
      throw new RuntimeException("ressources not found");
    } catch (IOException e) {
      throw new RuntimeException("ressources not found");
    }
  }

  public double getDensity(AbsoluteDate date, Vector3D position, Frame frame) throws OrekitException {
    setParameters(date,position,frame);
    return 0;
  }

  public Vector3D getVelocity(AbsoluteDate date, Vector3D position, Frame frame)
    throws OrekitException {
    
    return null;
  }
  
  public void dtm() {

    double zlb = zlb0; // + dzlb ?? ; // TODO 

//  calcul des polynomes de legendre
    double c = Math.sin(alat);
    double c2 = c*c;
    double c4 = c2*c2;
    double s = Math.cos(alat);
    double s2 = s*s;
    p10=c;
    p20=1.5*c2-0.5;
    p30=c*(2.5*c2-1.5);
    p40=4.375*c4-3.75*c2+0.375;
    p50=c*(7.875*c4-8.75*c2+1.875);
    p60=(5.5*c*p50-2.5*p40)/3.;
    p11=s;
    p21=3.*c*s;
    p31=s*(7.5*c2-1.5);
    p41=c*s*(17.5*c2-7.5);
    p51=s*(39.375*c4-26.25*c2+1.875);
    p22=3.*s2;
    p32=15.*c*s2;
    p42=s2*(52.5*c2-7.5);
    p52=3.*c*p42-2.*p32;
    p62=2.75*c*p52-1.75*p42;
    p33=15.*s*s2;

//  calcul des polynomes de legendre / pole magnetique (79n,71w)
    double clmlmg = Math.cos(xlon-xlmg);
    double sp = s*cpmg*clmlmg+c*spmg;
    double cmg = sp;                                // pole magnetique
    double cmg2 = cmg*cmg;
    double cmg4 = cmg2*cmg2;
    p10mg = cmg;
    p20mg = 1.5*cmg2-0.5;
    p40mg = 4.375*cmg4-3.75*cmg2+0.375;

//  heure locale 
    hl0=hl;
    ch=Math.cos(hl0);
    sh=Math.sin(hl0);
    c2h=ch*ch-sh*sh;
    s2h=2.*ch*sh;
    c3h=c2h*ch-s2h*sh;
    s3h=s2h*ch+c2h*sh;

//  calcul de la fonction g(l) / tinf, t120, tp120
    int kleq=1;
    
    double gdelt = gldtm(tt,dtt,1,kleq);
    dtt[1]=1.+gdelt;
    tinf=tt[1]*dtt[1];

    kleq = 0; //equinox
    
    if(day<59. || day>284.) {
      kleq=-1; //hiver nord
    }
    if(day>99. & day<244.) {
      kleq= 1; //ete nord
    }

    double gdelt0 =  gldtm(t0,dt0,0,kleq);
    dt0[1]=(t0[1]+gdelt0)/t0[1];
    double t120=t0[1]+gdelt0; // todo t120 ???
    double gdeltp = gldtm(tp,dtp,0,kleq);
    dtp[1]=(tp[1]+gdeltp)/tp[1];
    tp120=tp[1]+gdeltp;

//  calcul des concentrations n(z): H, HE, O, N2, O2, N
    double sigma=tp120/(tinf-t120);
    double dzeta=(re+zlb)/(re+alti);
    double zeta=(alti-zlb)*dzeta;
// TODO ?    double dzeta2=dzeta*dzeta;
    double sigzeta=sigma*zeta;
    double expsz=Math.exp(-sigzeta);
    tz=tinf-(tinf-t120)*expsz;
    
    double[] dbase = new double[6+1];
    
    kleq=1;

    double gdelh = gldtm(h,dh,0,kleq);
    dh[1]=Math.exp(gdelh);
    dbase[1]=h[1]*dh[1];

    double gdelhe = gldtm(he,dhe,0,kleq);
    dhe[1]=Math.exp(gdelhe);
    dbase[2]=he[1]*dhe[1];
    double gdelo = gldtm(o,dox,1,kleq);
    dox[1]=Math.exp(gdelo);
    dbase[3]=o[1]*dox[1];

    double gdelaz2 = gldtm(az2,daz2,1,kleq);
    daz2[1]=Math.exp(gdelaz2);
    dbase[4]=az2[1]*daz2[1];

    double gdelo2 = gldtm(o2,do2,1,kleq);;
    do2[1]=Math.exp(gdelo2);
    dbase[5]=o2[1]*do2[1];

    double gdelaz = gldtm(az,daz,1,kleq);
    daz[1]=Math.exp(gdelaz);
    dbase[6]=az[1]*daz[1];

    double glb=gsurf/((1.+zlb/re)*(1.+zlb/re)); // TODO **2 ?
    glb=glb/(sigma*rgas*tinf);
    double t120tz=t120/tz;
// TODO ?  double tinftz=tinf/tz;
    
    double[] cc = new double[6+1];
    double[] fz = new double[6+1];
    for (int i = 1; i<=6; i++) {
      double gamma=ma[i]*glb;
      double upapg=1.+alefa[i]+gamma;
      fz[i]=Math.pow(t120tz, upapg)*Math.exp(-sigzeta*gamma);
//    concentrations of H, HE, O, N2, O2, N (particles/cm3)
      cc[i]=dbase[i]*fz[i];
//    densities of H, HE, O, N2, O2, N (g/cm3)
      d[i]=cc[i]*vma[i];

//    densite totale
      ro=ro+d[i];
    }

//  masse atomique moyenne
    wmm=ro/(vma[1]*(cc[1]+cc[2]+cc[3]+cc[4]+cc[5]+cc[6]));

  }

  /** Computation of function G
   * @param a vector of coefficients for computation
   * @param da vector of partial derivatives
   * @param ff0 coefficient flag (1 for Ox, Az, He, T°; 0 for H and tp120) 
   * @param kle_eq season indicator flag (summer, winter, equinox)
   * @return value of G
   */
  private double gldtm(double[] a, double[] da, int ff0, double kle_eq) {

    double[] fmfb = new double[2+1];
    double[] fbm150 = new double[2+1];

//  termes de latitude  
    da[2] =p20; 
    da[3] =p40 ;
    da[74]=p10 ;
    double a74=a[74];
    double a77=a[77];
    double a78=a[78];
    if(kle_eq == -1) {     // hiver
      a74=-a74;
      a77=-a77;
      a78=-a78;
    }
    if(kle_eq == 0 ) {    // equinox
      a74 = coefintp(day,a74);
      a77 = coefintp(day,a77);
      a78 = coefintp(day,a78);
    }
    da[77]=p30 ;
    da[78]=p50 ;
    da[79]=p60 ;
//  termes de flux
    fmfb[1]=f[1]-fbar[1];
    fmfb[2]=f[2]-fbar[2];
    fbm150[1]=fbar[1]-150.;
    fbm150[2]=fbar[2];
    da[4]=fmfb[1];
    da[6]=fbm150[1];
    da[4]=da[4]+a[70]*fmfb[2];
    da[6]=da[6]+a[71]*fbm150[2];
    da[70]=fmfb[2]*(a[4]+2.*a[5]*da[4]+a[82]*p10+a[83]*p20+a[84]*p30);
    da[71]=fbm150[2]*(a[6]+2.*a[69]*da[6]+a[85]*p10+a[86]*p20+a[87]*p30);
    da[5]=da[4]*da[4];
    da[69]=da[6]*da[6];
    da[82]=da[4]*p10;
    da[83]=da[4]*p20;
    da[84]=da[4]*p30;
    da[85]=da[6]*p20;
    da[86]=da[6]*p30;
    da[87]=da[6]*p40;
//  termes de kp
    int ikp=62;
    int ikpm=67;
    double c2fi=1.-p10mg*p10mg;
    double dkp=akp[1]+(a[ikp]+c2fi*a[ikp+1])*akp[2];
    double dakp=a[7]+a[8]*p20mg+a[68]*p40mg+2.*dkp*(a[60]+a[61]*p20mg+a[75]*2.*dkp*dkp);
    da[ikp]=dakp*akp[2];
    da[ikp+1]=da[ikp]*c2fi;
    double dkpm=akp[3]+a[ikpm]*akp[4];
    double dakpm=a[64]+a[65]*p20mg+a[72]*p40mg+2.*dkpm*(a[66]+a[73]*p20mg+a[76]*2.*dkpm*dkpm);
    da[ikpm]=dakpm*akp[4];
    da[7]=dkp;
    da[8]=p20mg*dkp;
    da[68]=p40mg*dkp;
    da[60]=dkp*dkp;
    da[61]=p20mg*da[60];
    da[75]=da[60]*da[60];
    da[64]=dkpm;
    da[65]=p20mg*dkpm;
    da[72]=p40mg*dkpm;
    da[66]=dkpm*dkpm;
    da[73]=p20mg*da[66];
    da[76]=da[66]*da[66];
//  fonction g(l) non periodique
    double f0=a[4]*da[4]+a[5]*da[5]+a[6]*da[6]+a[69]*da[69]
                                                        +a[82]*da[82]+a[83]*da[83]+a[84]*da[84]+a[85]*da[85]+a[86]*da[86]+a[87]*da[87];
    double f1f=1.+f0*ff0;

    f0=f0+a[2]*da[2]+a[3]*da[3]+a74*da[74]+a77*da[77]
                                                  +a[7]*da[7]+a[8]*da[8]
                                                                      +a[60]*da[60]+a[61]*da[61]+a[68]*da[68]
                                                                                                          +a[64]*da[64]+a[65]*da[65]+a[66]*da[66]
                                                                                                                                              +a[72]*da[72]+a[73]*da[73]+a[75]*da[75]+a[76]*da[76]
                                                                                                                                                                                               +a78*da[78]+a[79]*da[79];
//  termes annuels symetriques en latitude
    da[9]=Math.cos(rot*(day-a[11]));
    da[10]=p20*da[9];
//  termes semi-annuels symetriques en latitude
    da[12]=Math.cos(rot2*(day-a[14]));
    da[13]=p20*da[12];
//  termes annuels non symetriques en latitude
    double coste=Math.cos(rot*(day-a[18]));
    da[15]=p10*coste;
    da[16]=p30*coste;
    da[17]=p50*coste;
//  terme  semi-annuel  non symetrique  en latitude
    double cos2te=Math.cos(rot2*(day-a[20]));
    da[19]=p10*cos2te;
    da[39]=p30*cos2te;
    da[59]=p50*cos2te;
//  termes diurnes [et couples annuel]
    da[21]=p11*ch;
    da[22]=p31*ch;
    da[23]=p51*ch;
    da[24]=da[21]*coste;
    da[25]=p21*ch*coste;
    da[26]=p11*sh;
    da[27]=p31*sh;
    da[28]=p51*sh;
    da[29]=da[26]*coste;
    da[30]=p21*sh*coste;
//  termes semi-diurnes [et couples annuel]
    da[31]=p22*c2h ;
    da[37]=p42*c2h ;
    da[32]=p32*c2h*coste;
    da[33]=p22*s2h ;
    da[38]=p42*s2h ;
    da[34]=p32*s2h*coste;
    da[88]=p32*c2h  ;
    da[89]=p32*s2h ;
    da[90]=p52*c2h ;
    da[91]=p52*s2h;
    double a88=a[88];
    double a89=a[89];
    double a90=a[90];
    double a91=a[91];
    if(kle_eq == -1) {            //hiver
      a88=-a88;
      a89=-a89;
      a90=-a90;
      a91=-a91;
    }
    if(kle_eq == 0) {             //equinox
      a88 = coefintp(day,a88);
      a89 = coefintp(day,a89);
      a90 = coefintp(day,a90);
      a91 = coefintp(day,a91);
    }
    da[92]=p62*c2h ;
    da[93]=p62*s2h ;
//  termes ter-diurnes
    da[35]=p33*c3h;
    da[36]=p33*s3h;
//  fonction g[l] periodique
    double fp=a[9]*da[9]+a[10]*da[10]+a[12]*da[12]+a[13]*da[13]
                                                            +a[15]*da[15]+a[16]*da[16]+a[17]*da[17]+a[19]*da[19]
                                                                                                             +a[21]*da[21]+a[22]*da[22]+a[23]*da[23]+a[24]*da[24]
                                                                                                                                                              +a[25]*da[25]+a[26]*da[26]+a[27]*da[27]+a[28]*da[28]
                                                                                                                                                                                                               +a[29]*da[29]+a[30]*da[30]+a[31]*da[31]+a[32]*da[32]
                                                                                                                                                                                                                                                                +a[33]*da[33]+a[34]*da[34]+a[35]*da[35]+a[36]*da[36]
                                                                                                                                                                                                                                                                                                                 +a[37]*da[37]+a[38]*da[38]+a[39]*da[39]+a[59]*da[59]
                                                                                                                                                                                                                                                                                                                                                                  +a88*da[88]+a89*da[89]+a90*da[90]+a91*da[91]
                                                                                                                                                                                                                                                                                                                                                                                                           +a[92]*da[92]+a[93]*da[93];

//  termes d'activite magnetique
    da[40]=p10*coste*dkp;
    da[41]=p30*coste*dkp;
    da[42]=p50*coste*dkp;
    da[43]=p11*ch*dkp;
    da[44]=p31*ch*dkp;
    da[45]=p51*ch*dkp;
    da[46]=p11*sh*dkp;
    da[47]=p31*sh*dkp;
    da[48]=p51*sh*dkp;

//  fonction g[l] periodique supplementaire
    fp=fp+a[40]*da[40]+a[41]*da[41]+a[42]*da[42]+a[43]*da[43]
                                                          +a[44]*da[44]+a[45]*da[45]+a[46]*da[46]+a[47]*da[47]+a[48]*da[48];

    dakp = (a[40]*p10+a[41]*p30+a[42]*p50)*coste
    +(a[43]*p11+a[44]*p31+a[45]*p51)*ch
    +(a[46]*p11+a[47]*p31+a[48]*p51)*sh;
    da[ikp]=da[ikp]+dakp*akp[2];
    da[ikp+1]=da[ikp]+dakp*c2fi*akp[2];
//  termes de longitude
    double clfl= Math.cos(xlon);
    da[49]=p11*clfl;
    da[50]=p21*clfl;
    da[51]=p31*clfl;
    da[52]=p41*clfl;
    da[53]=p51*clfl;
    double slfl=Math.sin(xlon);
    da[54]=p11*slfl;
    da[55]=p21*slfl;
    da[56]=p31*slfl;
    da[57]=p41*slfl;
    da[58]=p51*slfl;

//  fonction g[l] periodique supplementaire
    fp=fp +a[49]*da[49]+a[50]*da[50]+a[51]*da[51]+a[52]*da[52]+a[53]*da[53]
                                                                        +a[54]*da[54]+a[55]*da[55]+a[56]*da[56]+a[57]*da[57]+a[58]*da[58];

//  fonction g(l) totale (couplage avec le flux)
    return f0+fp*f1f;

  }

  private double coefintp(double day,double coef) {
    int debeq_pr = 59;
    int debeq_au = 244;
    double xmult = 0;
    if(day >= 100.) {
      xmult=(day-debeq_au)/40.;
      coef=coef-2.*coef*xmult;
    }
    else {
      xmult=(day-debeq_pr)/40.;
      coef=2.*coef*xmult-coef;
    }
    return coef;
  }
  
  /** Initializes current state and datas before computing.
   * @param date current date   
   * @param position current position in inertial frame
   * @param frame inertial frame
   * @throws OrekitException
   */
  private void setParameters(AbsoluteDate date, Vector3D position, Frame frame) 
    throws OrekitException {
    
    // check if datas are available :
    if(date.compareTo(inputParams.getMaxDate())>0 ||
       date.compareTo(inputParams.getMinDate())<0) {
      throw new OrekitException("Current date is out of range. " + 
                                  "Solar activity datas are not available",
                                new String[0]);      
    }
    
    // compute day number in current year
    String dateS = date.toString();
    AbsoluteDate year = null;
    try {
      year = new AbsoluteDate(dateS.substring(0, 4)+"-01-01T00:00:00", UTCScale.getInstance());
    } catch (ParseException e) {
      // should not happen
      throw new RuntimeException(e.getLocalizedMessage());
    } 
    double offset = date.minus(year);
    day = (int)Math.floor(offset/86400.0);
    
    // compute geodetic position
    Vector3D posInBody = frame.getTransformTo(bodyFrame, date).transformPosition(position);
    GeodeticPoint inBody = earth.transform(posInBody);
    alti = inBody.altitude/1000.0;
    xlon = inBody.longitude;
    alat = inBody.latitude;
    
    // compute local solar time
    Vector3D sunPos = sun.getPosition(date, frame);
    hl = Math.PI + 
      Math.atan2(sunPos.getX()*position.getY() - sunPos.getY()*position.getX(), 
                 sunPos.getX()*position.getX() + sunPos.getY()*position.getY());
    hl = Utils.trimAngle(hl, Math.PI);
    // get current solar activity datas
    f[1] = inputParams.getInstantFlux(date); 
    fbar[1] = inputParams.getMeanFlux(date); 
    akp[1] =inputParams.getThreeHourlyKP(date);
    akp[3] =inputParams.get24HoursKp(date);  
    
  }

  
  //---- Entry values. INPUT :
  
  /** Number of days in current year. */
  private int day;
  /** Instant solar flux. f[1]=instantaneous flux; f[2]=0. (not used). */
  private double[] f = new double[3];
  /** Mean solar flux. fbar[1]=mean flux; fbar[2]=0. (not used) */
  private double[] fbar = new double[3];
  /** Kp. akp[1]=3-hourly kp; akp[3]=mean kp of last 24 hours; akp[2]=akp[4]=0 (not used */ 
  private double[] akp = new double[5];
  /** Geodetic altitude in km (minimum altitude: 120 km) */
  private double alti;
  /** Local solar time (rad) */
  private double hl;
  /** Geodetic Latitude (rad). */
  private double alat;
  /** Geodetic longitude (rad). */
  private double xlon;
  /** Sun position */
  private final ThirdBody sun;
  /** External data container */
  private final DTM2000InputParameters inputParams;
  /** Earth body shape */
  private final BodyShape earth;
  /** Earth fixed frame */
  private final Frame bodyFrame;  
  
  //---- Values to compute. OUTPUT :

  /** Temperature at altitude z (K). */
  private double tz;
  /** Exospheric temperature. */
  private double tinf;
  /** Vertical gradient of T a 120 km. */
  private double tp120;
  /** Total density (g/cm3). */
  private double ro;
  /** Mean atomic mass. */
  private double wmm;
  /** Partial densities in (g/cm3) :
   d(1) = hydrogen 
   d(2) = helium
   d(3) = atomic oxygen
   d(4) = molecular nitrogen
   d(5) = molecular oxygen
   d(6) = atomic nitrogen */
  private double[] d = new double[6+1];

  // Intermediate coefficients :
  
  /** Legendre coefficients */
  private double p10,p20,p30,p40,p50,p60,p11,p21,p31,p41,p51,p22,p32,
  p42,p52,p62,p33,p10mg,p20mg,p40mg;
  /** Local time intermediate values */
  private double hl0,ch,sh,c2h,s2h,c3h,s3h;

  // Constants :
  
  /** Number of parameters. */
  private static final int nlatm = 96;
  /** Thermal diffusion coefficient. */
  private static final double[] alefa = new double[]{0, -0.40,-0.38,0.,0.,0.,0.}; 
  /** Atomic mass  H, HE, O, N2, O2, N */
  private static final double[] ma = new double[]{0, 1,4,16,28,32,14}; 
  /** Atomic mass  H, HE, O, N2, O2, N */
  private static final double[] vma = new double[]{0, 1.6606e-24,6.6423e-24,26.569e-24,46.4958e-24,53.1381e-24,23.2479e-24};
  /** Polar Earth radius */
  private static final double re = 6356.77;
  /** Reference altitude. */
  private static final double zlb0 = 120.;
  /** Magnetic Pole coordinates (79n,71w) */
  private static final double cpmg = .19081 ,spmg = .98163, xlmg = -1.2392; 
  /** Gravity acceleration at 120 km altitude. */
  private static final double gsurf = 980.665;
  // TODO determine what this is
  private static final double rgas = 831.4; 
  /** 2*pi/365 */
  private static final double rot = .017214206;
  /** é*rot */
  private static final double rot2 = .034428412;  
  
  // Resources file :
  /** Resources text file. */
  private static final String dtm2000 = "/fr/cs/aerospace/orekit/resources/dtm_2000";
  /** Flag to avoid useless computation. */
  private static boolean isFirstTime = true;
  
  // Dtm ressources :
  
  private static double[] tt = new double[nlatm+1]; 
  private static double[] h = new double[nlatm+1];
  private static double[] he = new double[nlatm+1];
  private static double[] o = new double[nlatm+1];
  private static double[] az2 = new double[nlatm+1];
  private static double[] o2 = new double[nlatm+1];
  private static double[] az = new double[nlatm+1];
  private static double[] t0 = new double[nlatm+1];
  private static double[] tp = new double[nlatm+1];

  private static double[] dtt = new double[nlatm+1]; 
  private static double[] dh = new double[nlatm+1];
  private static double[] dhe = new double[nlatm+1];
  private static double[] dox = new double[nlatm+1];
  private static double[] daz2 = new double[nlatm+1];
  private static double[] do2 = new double[nlatm+1];
  private static double[] daz = new double[nlatm+1];
  private static double[] dt0 = new double[nlatm+1];
  private static double[] dtp = new double[nlatm+1]; 
  
  
  /**
   * @return the d
   */
  public double[] getD() {
    return d;
  }
  
  /**
   * @return the ro
   */
  public double getRo() {
    return ro;
  }
  
  /**
   * @return the tinf
   */
  public double getTinf() {
    return tinf;
  }

  /**
   * @return the tp120
   */
  public double getTp120() {
    return tp120;
  }
  
  /**
   * @return the tz
   */
  public double getTz() {
    return tz;
  }
  
  /**
   * @return the wmm
   */
  public double getWmm() {
    return wmm;
  }


}

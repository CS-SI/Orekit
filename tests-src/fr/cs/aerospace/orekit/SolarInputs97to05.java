package fr.cs.aerospace.orekit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.models.perturbations.DTM2000InputParameters;
import fr.cs.aerospace.orekit.models.perturbations.JB2006InputParameters;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;

public class SolarInputs97to05 implements JB2006InputParameters,
                                           DTM2000InputParameters {
    
  private SolarInputs97to05() throws OrekitException {
    
    datas = new TreeSet();
    InputStream in ;
    try {
      File rootDir = FindFile.find("/tests-src/fr/cs/aerospace/orekit/data" +
                                   "/atmosphere/JB_All_97-05.txt", "/");
      in = new FileInputStream(rootDir.getAbsolutePath());
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    
    BufferedReader rFlux = new BufferedReader(new InputStreamReader(in));

    
    try {
      File rootDir = FindFile.find("/tests-src/fr/cs/aerospace/orekit/data" +
                                   "/atmosphere/NOAA_ap_97-05.dat.txt", "/");
      in = new FileInputStream(rootDir.getAbsolutePath());
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    
    BufferedReader rAp = new BufferedReader(new InputStreamReader(in));
     
    try {
      read(rFlux, rAp);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }     
  }
  
  public static SolarInputs97to05 getInstance() throws OrekitException {
    if ( instance == null ) {
      instance = new SolarInputs97to05();
    }
    return instance;
  }

  private void read(BufferedReader rFlux, BufferedReader rAp) throws IOException, OrekitException {
    
    rFlux.readLine();
    rFlux.readLine();
    rFlux.readLine();
    rFlux.readLine();
    rAp.readLine();
    String lineAp;
    String[] flux;
    String[] ap;
    Calendar cal = new GregorianCalendar();
    cal.setTimeZone(TimeZone.getTimeZone("UTC"));
    cal.set(0, 0, 0, 0, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    
    AbsoluteDate date = null;
    boolean first = true;
    
    for(String lineFlux = rFlux.readLine(); lineFlux!=null; lineFlux = rFlux.readLine()) {
      lineAp = rAp.readLine();
      flux = lineFlux.trim().split("\\s+") ;
      ap = lineAp.trim().split("\\s+") ;
           
      int year = Integer.parseInt(flux[0]);           
      int day = Integer.parseInt(flux[1]);   
      
      if(day != Integer.parseInt(ap[0])) {
        throw new OrekitException(" Tain ca craint ", new String[0]);
      }
      if( (year<2000 & (year-1900)!=Integer.parseInt(ap[11])) ||
          (year>=2000 & (year-2000)!=Integer.parseInt(ap[11])) ) {
        throw new OrekitException(" Tain ca craint ", new String[0]);
      }

      cal.set(Calendar.YEAR, year);
      cal.set(Calendar.DAY_OF_YEAR, day);
      
      date = new AbsoluteDate(cal.getTime(), UTCScale.getInstance());
           
      if (first) {
        first = false;
        firstDate = date;
      }
            
      datas.add(new LineParameters(date, 
                                   new double[] {
                                     Double.parseDouble(ap[3]),
                                     Double.parseDouble(ap[4]),
                                     Double.parseDouble(ap[5]),
                                     Double.parseDouble(ap[6]),
                                     Double.parseDouble(ap[7]),
                                     Double.parseDouble(ap[8]),
                                     Double.parseDouble(ap[9]),
                                     Double.parseDouble(ap[10]),
                                     
                                   },
                                   Double.parseDouble(flux[3]),
                                   Double.parseDouble(flux[4]),
                                   Double.parseDouble(flux[5]), 
                                   Double.parseDouble(flux[6]), 
                                   Double.parseDouble(flux[7]),
                                   Double.parseDouble(flux[8]),                                    
                                   0,
                                   0));
      
    }
    lastDate = date;
  
  }
  
 private void findClosestLine(AbsoluteDate date) throws OrekitException {
   
   if(date.minus(firstDate)<0 || date.minus(lastDate)>86400) {
     throw new OrekitException("out of range" , new String[0]);
   }
   
   // don't search if the cached selection is fine
   if ((currentParam != null) && (date.minus(currentParam.date) >= 0) && 
                                 (date.minus(currentParam.date) < 86400 )) {
    return;
   }
   LineParameters before =
     new LineParameters(new AbsoluteDate(date, -86400), null, 0, 0, 0, 0, 0, 0, 0, 0);

   // search starting from entries a few steps before the target date
   SortedSet tailSet = datas.tailSet(before);
   
   if (tailSet != null) {
     currentParam = (LineParameters)tailSet.first();
     if(currentParam.date.minus(date)==-86400) {
       before = new LineParameters(date, null, 0, 0, 0, 0, 0, 0, 0, 0);
       tailSet = datas.tailSet(before);
       currentParam = (LineParameters)tailSet.first();
     }
   } else {
     throw new OrekitException("big problem" , new String[0]);
   }
 }
  
  /** All entries. */
  private TreeSet datas;
  
  private LineParameters currentParam;
  
  private AbsoluteDate firstDate;
  private AbsoluteDate lastDate;
  
  private static SolarInputs97to05 instance = null;
  
  /** Container class for Solar activity indexes.  */
  private class LineParameters  implements Comparable {

    /** Entries */
    public final AbsoluteDate date;
    public final double[] ap;
    public final double f10;
    public final double f10B;
    public final double s10;
    public final double s10B;
    public final double xm10;
    public final double xm10B;
    public final double kp3;
    public final double kp24;
    
    
    /** Simple constructor. */
    public LineParameters (AbsoluteDate date, double[]  ap, double f10,
                           double f10B, double s10, double s10B,
                           double xm10, double xm10B, double kp3,
                           double kp24) {
      this.date = date;
      this.ap = ap;
      this.f10 = f10;
      this.f10B = f10B;
      this.s10 = s10;
      this.s10B = s10B;
      this.xm10 = xm10;
      this.xm10B = xm10B;
      this.kp3 = kp3;
      this.kp24 = kp24;
    }

    /** Compare an entry with another one, according to date. */
    public int compareTo(Object entry) {
      return date.compareTo(((LineParameters) entry).date);
    }

  }


  public double getAp(AbsoluteDate date) {
    double[] tab = null;
    double result = Double.NaN;
    try {
      findClosestLine(date);
      tab=currentParam.ap;
      Calendar cal = new GregorianCalendar();
      cal.setTimeZone(TimeZone.getTimeZone("UTC"));
      cal.setTime(date.toDate(UTCScale.getInstance()));
      int hour = cal.get(Calendar.HOUR_OF_DAY);
      for(int i= 0; i<8; i++) {
        if(hour>=i*3 & hour<(i+1)*3) {
          result = tab[i];
        }
      }     
    } 
    catch (OrekitException e) {
      // nothing
    }    
    return result;
  }

  public double getF10(AbsoluteDate date) {
    double result = Double.NaN;    
    try {
      findClosestLine(date);
      result=currentParam.f10;
    } 
    catch (OrekitException e) {
      // nothing
    }    
    return result;
  }

  public double getF10B(AbsoluteDate date) {
    double result = Double.NaN;
    try {
      findClosestLine(date);
      result=currentParam.f10B;
    } 
    catch (OrekitException e) {
      // nothing
    }    
    return result;
  }

  public AbsoluteDate getMaxDate() {
    return new AbsoluteDate(lastDate, 86400);
  }

  public AbsoluteDate getMinDate() {
    return firstDate;
  }

  public double getS10(AbsoluteDate date) {
    double result = Double.NaN;
    try {
      findClosestLine(date);
      result=currentParam.s10;
    } 
    catch (OrekitException e) {
      // nothing
    }    
    return result;
  }

  public double getS10B(AbsoluteDate date) {
    double result = Double.NaN;
    try {
      findClosestLine(date);
      result=currentParam.s10B;
    } 
    catch (OrekitException e) {
      // nothing
    }    
    return result;
  }

  public double getXM10(AbsoluteDate date) {
    double result = Double.NaN;
    try {
      findClosestLine(date);
      result=currentParam.xm10;
    } 
    catch (OrekitException e) {
      // nothing
    }    
    return result;
  }

  public double getXM10B(AbsoluteDate date) {
    double result = Double.NaN;
    try {
      findClosestLine(date);
      result=currentParam.xm10B;
    } 
    catch (OrekitException e) {
      // nothing
    }    
    return result;
  }

  public double get24HoursKp(AbsoluteDate date) {
    // TODO Auto-generated method stub
    return 0;
  }

  public double getInstantFlux(AbsoluteDate date) {
    return getF10(date);
  }

  public double getMeanFlux(AbsoluteDate date) {
    return getF10B(date);
  }
  
//  K, Kp, and ap Indices
//
//  The K-index is quasi-logarithmic local index of the 3-hourly range in magnetic activity relative to an assumed quiet-day curve for a single geomagnetic observatory site. First introduced by J. Bartels in 1938, it consists of a single-digit 0 thru 9 for each 3-hour interval of the universal time day (UT).
//
//  The planetary 3-hour-range index Kp is the mean standardized K-index from 13 geomagnetic observatories between 44 degrees and 60 degrees northern or southern geomagnetic latitude. The scale is O to 9 expressed in thirds of a unit, e.g. 5- is 4 2/3, 5 is 5 and 5+ is 5 1/3. This planetary index is designed to measure solar particle radiation by its magnetic effects. The 3-hourly ap (equivalent range) index is derived from the Kp index as follows:
//
//    Kp = 0o   0+   1-   1o   1+   2-   2o   2+   3-   3o   3+   4-   4o   4+
//    ap =  0    2    3    4    5    6    7    9   12   15   18   22   27   32
//    Kp = 5-   5o   5+   6-   6o   6+   7-   7o   7+   8-   8o   8+   9-   9o
//    ap = 39   48   56   67   80   94  111  132  154  179  207  236  300  400
  public double getThreeHourlyKP(AbsoluteDate date) {
    double ap = getAp(date);
    if( ap >= 0 & ap < 3) {
      return 0 ;
    }
    if( ap >= 3 & ap < 6) {
      return 1 ;
    }
    if( ap >= 6 & ap < 12) {
      return 2 ;
    }
    if( ap >= 12 & ap < 22) {
      return 3 ;
    }
    if( ap >= 22 & ap < 39) {
      return 4 ;
    }
    if( ap >= 39 & ap < 67) {
      return 5 ;
    }
    if( ap >=67 & ap < 111) {
      return 6 ;
    }
    if( ap >= 111 & ap < 179) {
      return 7 ;
    }
    if( ap >= 179 & ap < 300) {
      return 8 ;
    }
    else {
      return 9 ;
    }    
  }
}

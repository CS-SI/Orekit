package fr.cs.aerospace.orekit.frames;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.Utils;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.TTScale;
import fr.cs.aerospace.orekit.time.UTCScale;

/** Terrestrial Intermediate Reference Frame 2000.
 * <p> The pole motion is not considered : Pseudo Earth Fixed Frame. It handles
 * the earth rotation angle, its parent frame is the {@link IRF2000Frame}</p>
 */ 
class TIRF2000Frame extends Frame {

  /** Constructor for the singleton.
   * @param parent the IRF2000
   * @param date the current date
   * @param name the string reprensentation
   * @throws OrekitException
   */
  protected TIRF2000Frame(Frame parent, AbsoluteDate date, String name) throws OrekitException {
    super(parent, null, name);
    // everything is in place, we can now synchronize the frame
    updateFrame(date);    
  }
  
  /** Update the frame to the given date.
   * <p>The update considers the earth rotation from IERS data.</p>
   * @param date new value of the date
   * @exception OrekitException if the nutation model data embedded in the
   * library cannot be read
   */
  protected void updateFrame(AbsoluteDate date)
    throws OrekitException {
    
    if (cachedDate == null||cachedDate!=date) {

      //    offset from J2000 epoch in julian centuries
      double tts = date.minus(AbsoluteDate.J2000Epoch);
      
      // compute Earth Rotation Angle using Nicole Capitaine model (2000)
      double dtu1 = EarthOrientationHistory.getInstance().getUT1MinusUTC(date);
      double taiMinusTt  = TTScale.getInstance().offsetToTAI(tts + j2000MinusJava);
      double utcMinusTai = UTCScale.getInstance().offsetFromTAI(tts + taiMinusTt + j2000MinusJava);
      double tu = (tts + taiMinusTt + utcMinusTai + dtu1) / 86400 ;
      era  = era0 + era1A * tu + era1B * tu;
      era -= twoPi * Math.floor((era + Math.PI) / twoPi);
      
      double eraDeg = Utils.trimAngle(era, Math.PI);
      eraDeg = Math.toDegrees(eraDeg);
      // simple rotation around the Celestial Intermediate Pole
      Rotation rRot = new Rotation(Vector3D.plusK, era);

      Rotation combined = rRot.revert();
      
      // set up the transform from parent GCRS (J2000) to ITRF
      Vector3D rotationRate = new Vector3D((era1A + era1B) / 86400, Vector3D.plusK);
      updateTransform(new Transform(combined , rotationRate));      
      cachedDate = date;
    }
  }
  
  /** Get the Earth Rotation Angle at the current date.
   * @param  date the date
   * @return Earth Rotation Angle at the current date in radians
   * @throws OrekitException 
   */
  public double getEarthRotationAngle(AbsoluteDate date) throws OrekitException {
    updateFrame(date);
    return era;
  }
  
  
  /** Cached date to avoid useless calculus */
  private AbsoluteDate cachedDate;
  
  /** Earth Rotation Angle, in radians. */
  private double era;
  
  /** 2&pi;. */
  private static final double twoPi = 2.0 * Math.PI;
  
  /** Constant term of Capitaine's Earth Rotation Angle model. */
  private static final double era0 = twoPi * 0.7790572732640;
  
  /** Offset between J2000.0 epoch and Java epoch in seconds. */
  private static final double j2000MinusJava =
    AbsoluteDate.J2000Epoch.minus(AbsoluteDate.JavaEpoch);

  /** Rate term of Capitaine's Earth Rotation Angle model.
   * (radians per day, main part) */
  private static final double era1A = twoPi;

  /** Rate term of Capitaine's Earth Rotation Angle model.
   * (radians per day, fractional part) */
  private static final double era1B = era1A * 0.00273781191135448;

  private static final long serialVersionUID = 2950241661767626138L;
  
}

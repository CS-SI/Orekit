package fr.cs.orekit.frames.series;

/** Class for terms that do not depend on far planets and some other elements.
 * @author Luc Maisonobe
 */
class NoFarPlanetsTerm extends SeriesTerm {

  public NoFarPlanetsTerm(double sinCoeff, double cosCoeff,
                         int cL, int cF, int cD, int cOmega,
                         int cMe, int cVe, int cE, int cMa, int cJu, int cSa) {
    super(sinCoeff, cosCoeff);
    this.cL      = cL;
    this.cF      = cF;
    this.cD      = cD;
    this.cOmega  = cOmega;
    this.cMe     = cMe;
    this.cVe     = cVe;
    this.cE      = cE;
    this.cMa     = cMa;
    this.cJu     = cJu;
    this.cSa     = cSa;
  }

  protected double argument(BodiesElements elements) {
    return cL * elements.l + cF * elements.f
         + cD * elements.d + cOmega * elements.omega
         + cMe * elements.lMe + cVe * elements.lVe + cE  * elements.lE
         + cMa * elements.lMa + cJu * elements.lJu + cSa * elements.lSa;

  }

  private final int cL;
  private final int cF;
  private final int cD;
  private final int cOmega;
  private final int cMe;
  private final int cVe;
  private final int cE;
  private final int cMa;
  private final int cJu;
  private final int cSa;

  private static final long serialVersionUID = 3105481273138507585L;

}

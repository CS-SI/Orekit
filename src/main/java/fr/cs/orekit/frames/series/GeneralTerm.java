package fr.cs.orekit.frames.series;

/** Class for general terms.
 * @author Luc Maisonobe
 */
class GeneralTerm extends SeriesTerm {

  public GeneralTerm(double sinCoeff, double cosCoeff,
                     int cL, int cLPrime, int cF, int cD, int cOmega,
                     int cMe, int cVe, int cE, int cMa, int cJu,
                     int cSa, int cUr, int cNe, int cPa) {
    super(sinCoeff, cosCoeff);
    this.cL      = cL;
    this.cLPrime = cLPrime;
    this.cF      = cF;
    this.cD      = cD;
    this.cOmega  = cOmega;
    this.cMe     = cMe;
    this.cVe     = cVe;
    this.cE      = cE;
    this.cMa     = cMa;
    this.cJu     = cJu;
    this.cSa     = cSa;
    this.cUr     = cUr;
    this.cNe     = cNe;
    this.cPa     = cPa;
  }

  protected double argument(BodiesElements elements) {
    return cL * elements.l + cLPrime * elements.lPrime + cF * elements.f
         + cD * elements.d + cOmega * elements.omega
         + cMe * elements.lMe + cVe * elements.lVe + cE  * elements.lE
         + cMa * elements.lMa + cJu * elements.lJu
         + cSa * elements.lSa + cUr * elements.lUr
         + cNe * elements.lNe + cPa * elements.pa;

  }

  private final int cL;
  private final int cLPrime;
  private final int cF;
  private final int cD;
  private final int cOmega;
  private final int cMe;
  private final int cVe;
  private final int cE;
  private final int cMa;
  private final int cJu;
  private final int cSa;
  private final int cUr;
  private final int cNe;
  private final int cPa;

  private static final long serialVersionUID = 2084933766306957711L;

}

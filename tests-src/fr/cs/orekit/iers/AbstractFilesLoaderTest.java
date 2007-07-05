package fr.cs.orekit.iers;

import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.IERSDataResetter;
import junit.framework.TestCase;

public abstract class AbstractFilesLoaderTest extends TestCase {

  protected void setRoot(String directoryName) throws OrekitException {
    IERSDataResetter.setUp(directoryName);
    eop = new TreeSet(new ChronologicalEOPComparator());
  }

  protected int getMaxGap() {
    int maxGap = 0;
    EarthOrientationParameters current = null;
    for (Iterator iterator = eop.iterator(); iterator.hasNext();) {
      EarthOrientationParameters previous = current;
      current = (EarthOrientationParameters) iterator.next();
      if (previous != null) {
        maxGap = Math.max(maxGap, current.mjd - previous.mjd);
      }
    }
    return maxGap;
  }

  public void tearDown() {
    eop = null;
    IERSDataResetter.tearDown();
  }

  private static class ChronologicalEOPComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      EarthOrientationParameters eop1 = (EarthOrientationParameters) o1;
      EarthOrientationParameters eop2 = (EarthOrientationParameters) o2;
      return eop1.mjd - eop2.mjd;
    }
  }
  
  protected TreeSet eop;

}

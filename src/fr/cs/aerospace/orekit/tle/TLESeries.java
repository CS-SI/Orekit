package fr.cs.aerospace.orekit.tle;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

public class TLESeries {  

  public TLESeries() {
    tles = new TreeSet();
    internationalDesignator = null;
    satelliteNumber = 0;
    previous = null;
    next = null;
  }

  /** Read a TLE file.
   * <p> The read TLE entries, if they match, are stored into a treeset for later use. <p>
   * @param in the input to read (it can be compressed)
   * @throws IOException when the {@link InputStream} cannot be buffered.
   * @throws OrekitException when a format error occurs
   */
  public void read(InputStream in) throws IOException, OrekitException {
// TODO different formats, not portable enough
    BufferedReader r = new BufferedReader(new InputStreamReader(checkCompressed(in)));

    String line2;
    for (String line1 = r.readLine(); line1 != null; line1 = r.readLine()) {
      line2 = r.readLine();

      // Format checks :
      if(line2==null) {
        throw new OrekitException("Non pair line number", new String[0]);
      }
      if (TLE.isFormatOK(line1, line2)) {
        int satNum = Integer.parseInt(line1.substring(2,7).replace(" ", "0"));
        String iD = line1.substring(9,17);
        if(satelliteNumber==0&&internationalDesignator==null) {
          satelliteNumber = satNum;
          internationalDesignator = iD;
        }
        else {
          if(satNum!=satelliteNumber||(iD.equals(internationalDesignator))==false) {
            throw new OrekitException("The TLE's are not representing the same object.", new String[0]);
          }
        }
        // seems OK
        tles.add(new TLE(line1,line2)); 
      } else {
        throw new OrekitException("All the lines are not TLE's", new String[0]);
      }

    }
  }

  public TLE getClosestTLE(AbsoluteDate date) {

    //  don't search if the cached selection is fine
    if ((previous != null) && (date.minus(previous.getEpoch()) >= 0)
        && (next != null) && (date.minus(next.getEpoch()) <= 0)) {
      // the current selection is already good
      if(next.getEpoch().minus(date)>date.minus(previous.getEpoch())) {
        return previous;
      }
      else {
        return next;
      }
    }
    // reset the selection before the search phase
    previous = null;
    next     = null;
    TLE ideal = new TLE(date);

    SortedSet headSet = tles.headSet(ideal);
    SortedSet tailSet = tles.tailSet(ideal);


    if (headSet.isEmpty()) {
      return (TLE)tailSet.first();
    }
    if (tailSet.isEmpty()) {
      return (TLE)headSet.last();
    }
    previous = (TLE)headSet.last();
    next = (TLE)tailSet.first();

    if(next.getEpoch().minus(date)>date.minus(previous.getEpoch())) {
      return previous;
    }
    else {
      return next;
    }
  }

  public AbsoluteDate getFirstDate() throws OrekitException {      
    if (firstDate==null){
      firstDate = ((TLE)tles.first()).getEpoch();
    }
    return firstDate;
  }

  public AbsoluteDate getLastDate() throws OrekitException {      
    if (lastDate==null){
      lastDate = ((TLE)tles.last()).getEpoch();
    }
    return lastDate;
  }

  private BufferedInputStream checkCompressed(InputStream in) throws IOException {

    BufferedInputStream filter = new BufferedInputStream(in);
    filter.mark(1024 * 1024);

    boolean isCompressed = false;
    try {
      isCompressed = (new GZIPInputStream(filter).read() != -1);
    } catch (IOException e) {
      isCompressed = false;
    }
    filter.reset();

    if (isCompressed) {
      filter = new BufferedInputStream(new GZIPInputStream(filter));
    }
    filter.mark(1024 * 1024);
    return filter;
  }

  /** TLE entries. */
  private TreeSet tles;  
  private TLE previous;
  private TLE next;

  /** Bounds *. */
  private AbsoluteDate firstDate;
  private AbsoluteDate lastDate;

  /** The satellite id */
  private int satelliteNumber;
  /** International designator */
  private String internationalDesignator;
}

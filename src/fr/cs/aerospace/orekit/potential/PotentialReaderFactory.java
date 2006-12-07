package fr.cs.aerospace.orekit.potential;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import fr.cs.aerospace.orekit.errors.OrekitException;

/** This pattern determines which reader to use with the selected file.
 * @author F. Maussion
 */
public class PotentialReaderFactory {

  public PotentialReaderFactory() {
    readers = new ArrayList();
    readers.add(new SHMFormatReader());
    readers.add(new EGMFormatReader());
  }
  
  public void addPotentialReader(PotentialCoefficientsReader reader) {
     readers.add(reader);
  }
  
  /** Determines the proper reader to use wich the selected file.
   * @param in the file to check (it can be compressed)
   * @return the proper reader
   * @throws OrekitException when no known reader can read the file
   * @throws IOException when the {@link InputStream} is not valid. 
   */
  public PotentialCoefficientsReader getPotentialReader(InputStream in)
      throws OrekitException, IOException {

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
    PotentialCoefficientsReader result = null;

    // test the available readers
    for (Iterator iter = readers.iterator(); iter.hasNext();) {
      PotentialCoefficientsReader test = (PotentialCoefficientsReader) iter.next();
      if (test.isFileOK(filter)) {
        result = test;
      }
      filter.reset();
    }
    
    if (result == null) {
      throw new OrekitException("Unknown file format. ", new String[0]);
    }

    return result;

  }
  
  /** Potential readers tab */
  private ArrayList readers;

}

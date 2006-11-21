package fr.cs.aerospace.orekit.potential;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import fr.cs.aerospace.orekit.errors.OrekitException;

/** This pattern determines which reader to use with the selected file.
 * @author F. Maussion
 */
public class PotentialReaderFactory {
  
  /** Determines the proper reader to use wich the selected file.
   * @param in the file to check (it can be compressed)
   * @return the proper reader
   * @throws OrekitException when no known reader can read the file
   * @throws IOException when the {@link InputStream} is not valid. 
   */
  public static PotentialCoefficientsReader getPotentialReader(InputStream in) 
    throws OrekitException, IOException {
    
    BufferedInputStream filter = new BufferedInputStream(in);
    filter.mark(1024*1024);
    
    boolean isCompressed = false;
    try {
      new GZIPInputStream(filter).read(new byte[256]);
      isCompressed = true;
    } catch (IOException e) {
      isCompressed = false;
    }
    filter.reset();
    
    if(isCompressed) {
      filter = new BufferedInputStream(new GZIPInputStream(filter));
    }
    filter.mark(1024*1024);
    PotentialCoefficientsReader result = null;
    
    SHMFormatReader test1 = new SHMFormatReader();
    if (test1.isFileOK(filter)){
      result = test1;
    }
    filter.reset();
    EGMFormatReader test2 = new EGMFormatReader();
    if (test2.isFileOK(filter)){
      result = test2;
    }
    filter.reset();
    if (result==null) {
      throw new OrekitException("Unknown file format. " , new String[0]);
    }
    
    return result;
    
  }
  
  
}


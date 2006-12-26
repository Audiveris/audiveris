//----------------------------------------------------------------------------//
//                                                                            //
//                                  X S L T                                   //
//                                                                            //
//----------------------------------------------------------------------------//
//
import org.w3c.dom.*;

import org.xml.sax.*;

import java.io.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

/**
 * DOCUMENT ME!
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class XSLT
{
    //~ Methods ----------------------------------------------------------------

    public static void main (String... args)
    {
        if (args.length != 3) {
            System.err.println("Usage: XSLT in out xsl");
        } else {
            xsl(args[0], args[1], args[2]);
        }
    }

    // This method applies the xslFilename to inFilename and writes
    // the output to outFilename.
    public static void xsl (String inFilename,
                            String outFilename,
                            String xslFilename)
    {
        try {
            // Create transformer factory
            TransformerFactory factory = TransformerFactory.newInstance();

            // Use the factory to create a template containing the xsl file
            Templates   template = factory.newTemplates(
                new StreamSource(new FileInputStream(xslFilename)));

            // Use the template to create a transformer
            Transformer xformer = template.newTransformer();

            // Prepare the input and output files
            Source source = new StreamSource(new FileInputStream(inFilename));
            Result result = new StreamResult(new FileOutputStream(outFilename));

            // Apply the xsl file to the source file and write the result to the output file
            xformer.transform(source, result);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            // An error occurred in the XSL file
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();

            // An error occurred while applying the XSL file
            // Get location of error in input file
            SourceLocator locator = e.getLocator();
            int           col = locator.getColumnNumber();
            int           line = locator.getLineNumber();
            String        publicId = locator.getPublicId();
            String        systemId = locator.getSystemId();

            System.err.println(
                "Error" + " col=" + col + " line=" + line + " publicId=" +
                publicId + " systemId=" + systemId);
        }
    }
}

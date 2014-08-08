//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S c o r e E x p o r t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import static omr.score.ScoresManager.SCORE_EXTENSION;

import omr.util.FileUtil;

import com.audiveris.proxymusic.ScorePartwise;
import com.audiveris.proxymusic.mxl.Mxl;
import com.audiveris.proxymusic.mxl.RootFile;
import com.audiveris.proxymusic.util.Marshalling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Node;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

/**
 * Class {@code ScoreExporter} exports the provided score to a MusicXML file, stream or
 * DOM.
 *
 * @author Hervé Bitteur
 */
public class ScoreExporter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ScoreExporter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The related score. */
    private final Score score;

    //~ Constructors -------------------------------------------------------------------------------
    //---------------//
    // ScoreExporter //
    //---------------//
    /**
     * Create a new ScoreExporter object, on a related score instance.
     *
     * @param score the score to export (cannot be null)
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public ScoreExporter (Score score)
            throws InterruptedException, ExecutionException
    {
        if (score == null) {
            throw new IllegalArgumentException("Trying to export a null score");
        }

        this.score = score;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // export //
    //--------//
    /**
     * Export the score to a file.
     *
     * @param file            the xml or mxl file to write (cannot be null)
     * @param injectSignature should we inject out signature?
     * @param compressed      true for compressed output, false for uncompressed output
     */
    public void export (File file,
                        boolean injectSignature,
                        boolean compressed)
            throws Exception
    {
        String rootName = FileUtil.getNameSansExtension(file);
        export(new FileOutputStream(file), injectSignature, compressed, rootName);
    }

    //--------//
    // export //
    //--------//
    /**
     * Export the score to an output stream.
     *
     * @param os              the output stream where XML data is written (cannot be null)
     * @param injectSignature should we inject our signature?
     * @param compressed      true for compressed output
     * @param rootName        (for compressed only) simple root path name, without extension
     * @throws IOException
     * @throws Exception
     */
    public void export (OutputStream os,
                        boolean injectSignature,
                        boolean compressed,
                        String rootName)
            throws IOException, Exception
    {
        if (os == null) {
            throw new IllegalArgumentException("Trying to export a score to a null output stream");
        }

        // Build the ScorePartwise proxy
        ScorePartwise scorePartwise = new PartwiseBuilder(score).build();

        // Marshal the proxy
        if (compressed) {
            Mxl.Output mof = new Mxl.Output(os);
            OutputStream zos = mof.getOutputStream();

            if (rootName == null) {
                rootName = "score"; // Fall-back value
            }

            mof.addEntry(new RootFile(rootName + SCORE_EXTENSION, RootFile.MUSICXML_MEDIA_TYPE));
            Marshalling.marshal(scorePartwise, zos, injectSignature, 2);
            mof.close();
        } else {
            Marshalling.marshal(scorePartwise, os, injectSignature, 2);
            os.close();
        }
    }

    //--------//
    // export //
    //--------//
    /**
     * Export the score to DOM node.
     * (No longer used, it was meant for Audiveris->Zong pure java transfer)
     *
     * @param node            the DOM node to export to (cannot be null)
     * @param injectSignature should we inject our signature?
     * @throws java.io.IOException
     * @throws java.lang.Exception
     */
    public void export (Node node,
                        boolean injectSignature)
            throws IOException, Exception
    {
        if (node == null) {
            throw new IllegalArgumentException("Trying to export a score to a null DOM Node");
        }

        // Build the ScorePartwise proxy
        ScorePartwise scorePartwise = new PartwiseBuilder(score).build();

        // Marshal the proxy
        Marshalling.marshal(scorePartwise, node, injectSignature);
    }
}

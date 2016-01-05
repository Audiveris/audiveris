//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S c o r e E x p o r t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.OMR;

import com.audiveris.proxymusic.ScorePartwise;
import com.audiveris.proxymusic.mxl.Mxl;
import com.audiveris.proxymusic.mxl.RootFile;
import com.audiveris.proxymusic.util.Marshalling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Node;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Objects;

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
    /**
     * Create a new {@code ScoreExporter} object, on a related score instance.
     *
     * @param score the score to export (cannot be null)
     */
    public ScoreExporter (Score score)
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
     * @param path       the xml or mxl path to write (cannot be null)
     * @param scoreName  simple score name, without extension
     * @param signed     should we inject ProxyMusic signature?
     * @param compressed true for compressed output, false for uncompressed output
     * @throws java.lang.Exception
     */
    public void export (Path path,
                        String scoreName,
                        boolean signed,
                        boolean compressed)
            throws Exception
    {
        final OutputStream os = new FileOutputStream(path.toString());
        export(os, signed, scoreName, compressed);
        os.close();
        logger.info("Score {} exported to {}", scoreName, path);
    }

    //--------//
    // export //
    //--------//
    /**
     * Export the score to an output stream.
     *
     * @param os         the output stream where XML data is written (cannot be null)
     * @param signed     should we inject ProxyMusic signature?
     * @param scoreName  (for compressed only) simple score name, without extension
     * @param compressed true for compressed output
     * @throws Exception
     */
    public void export (OutputStream os,
                        boolean signed,
                        String scoreName,
                        boolean compressed)
            throws Exception
    {
        Objects.requireNonNull(os, "Trying to export a score to a null output stream");

        // Build the ScorePartwise proxy
        ScorePartwise scorePartwise = PartwiseBuilder.build(score);

        // Marshal the proxy
        if (compressed) {
            Mxl.Output mof = new Mxl.Output(os);
            OutputStream zos = mof.getOutputStream();

            if (scoreName == null) {
                scoreName = "score"; // Fall-back value
            }

            mof.addEntry(
                    new RootFile(scoreName + OMR.SCORE_EXTENSION, RootFile.MUSICXML_MEDIA_TYPE));
            Marshalling.marshal(scorePartwise, zos, signed, 2);
            mof.close();
        } else {
            Marshalling.marshal(scorePartwise, os, signed, 2);
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
     * @param node   the DOM node to export to (cannot be null)
     * @param signed should we inject ProxyMusic signature?
     * @throws Exception
     */
    public void export (Node node,
                        boolean signed)
            throws Exception
    {
        if (node == null) {
            throw new IllegalArgumentException("Trying to export a score to a null DOM Node");
        }

        // Build the ScorePartwise proxy
        ScorePartwise scorePartwise = PartwiseBuilder.build(score);

        // Marshal the proxy
        Marshalling.marshal(scorePartwise, node, signed);
    }
}

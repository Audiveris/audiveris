//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S c o r e E x p o r t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.score;

import org.audiveris.omr.OMR;
import org.audiveris.proxymusic.ScorePartwise;
import org.audiveris.proxymusic.mxl.Mxl;
import org.audiveris.proxymusic.mxl.RootFile;
import org.audiveris.proxymusic.util.Marshalling;

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
     * @throws Exception if something goes wrong
     */
    public void export (Path path,
                        String scoreName,
                        boolean signed,
                        boolean compressed)
            throws Exception
    {
        try (OutputStream os = new FileOutputStream(path.toString())) {
            export(os, signed, scoreName, compressed);
            logger.info("Score {} exported to {}", scoreName, path);
        }
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
     * @throws Exception if something goes wrong
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
     * (No longer used, it was meant for Audiveris&rarr;Zong pure java transfer)
     *
     * @param node   the DOM node to export to (cannot be null)
     * @param signed should we inject ProxyMusic signature?
     * @throws Exception if something goes wrong
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

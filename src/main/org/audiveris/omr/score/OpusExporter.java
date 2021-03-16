//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     O p u s E x p o r t e r                                    //
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
import org.audiveris.omr.sheet.Book;
import org.audiveris.proxymusic.ScorePartwise;
import org.audiveris.proxymusic.mxl.Mxl;
import org.audiveris.proxymusic.mxl.RootFile;
import org.audiveris.proxymusic.opus.ObjectFactory;
import org.audiveris.proxymusic.opus.Opus;
import org.audiveris.proxymusic.opus.YesNo;
import org.audiveris.proxymusic.util.Marshalling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Class {@code OpusExporter} is meant to export an Opus of scores (movements).
 *
 * @author Hervé Bitteur
 */
public class OpusExporter
{

    private static final Logger logger = LoggerFactory.getLogger(OpusExporter.class);

    /** The related book. */
    private final Book book;

    /**
     * Creates a new {@code OpusExporter} object on a related book.
     *
     * @param book the book of scores to export
     */
    public OpusExporter (Book book)
    {
        this.book = book;
    }

    //--------//
    // export //
    //--------//
    /**
     * Export the opus to a file.
     *
     * @param path     full target path to write (cannot be null)
     * @param rootName opus root name
     * @param signed   should we inject ProxyMusic signature?
     * @throws Exception if something goes wrong
     */
    public void export (Path path,
                        String rootName,
                        boolean signed)
            throws Exception
    {
        try (OutputStream os = new FileOutputStream(path.toString())) {
            export(os, signed, rootName);
            logger.info("Opus {} exported to {}", rootName, path);
        }
    }

    //--------//
    // export //
    //--------//
    /**
     * Export the opus to an output stream.
     *
     * @param os       the output stream where XML data is written (cannot be null)
     * @param signed   should we inject ProxyMusic signature?
     * @param rootName simple root path name, without extension
     * @throws Exception if something goes wrong
     */
    public void export (OutputStream os,
                        boolean signed,
                        String rootName)
            throws Exception
    {
        if (os == null) {
            throw new IllegalArgumentException("Trying to export a book to a null output stream");
        }

        // Storing
        Mxl.Output mof = new Mxl.Output(os);
        OutputStream zos = mof.getOutputStream();

        // Allocate the opus
        ObjectFactory opusFactory = new ObjectFactory();
        Opus opus = opusFactory.createOpus();
        opus.setTitle("Opus for " + book);
        opus.setVersion("3.0");

        if (rootName == null) {
            rootName = "opus"; // Fall-back value
        }

        final List<Score> scores = book.getScores();
        final boolean multi = scores.size() > 1; // Is this a multi-movement book?

        for (Score score : scores) {
            // Reference each score/movement in opus
            String entryName = rootName + (multi ? (".mvt" + score.getId()) : "")
                                       + OMR.SCORE_EXTENSION;
            org.audiveris.proxymusic.opus.Score oScore = opusFactory.createScore();
            oScore.setHref(entryName);
            oScore.setNewPage(YesNo.YES);
            opus.getOpusOrOpusLinkOrScore().add(oScore);

            // Marshal the score partwise
            ScorePartwise scorePartwise = PartwiseBuilder.build(score);
            mof.addEntry(new RootFile(entryName, RootFile.MUSICXML_MEDIA_TYPE));
            Marshalling.marshal(scorePartwise, zos, signed, 2);
        }

        // Store opus as root
        Marshalling.getContext(Opus.class);
        mof.addFirstEntry(new RootFile(rootName + ".opus.xml", RootFile.MUSICXML_MEDIA_TYPE));
        Marshalling.marshal(opus, zos);

        // The end
        mof.close();
    }
}

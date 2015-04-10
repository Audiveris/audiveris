//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     O p u s E x p o r t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.OMR;

import omr.sheet.Book;

import com.audiveris.proxymusic.ScorePartwise;
import com.audiveris.proxymusic.mxl.Mxl;
import com.audiveris.proxymusic.mxl.RootFile;
import com.audiveris.proxymusic.opus.ObjectFactory;
import com.audiveris.proxymusic.opus.Opus;
import com.audiveris.proxymusic.opus.YesNo;
import com.audiveris.proxymusic.util.Marshalling;

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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(OpusExporter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The related book. */
    private final Book book;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code OpusExporter} object on a related book.
     *
     * @param book the book of scores to export
     */
    public OpusExporter (Book book)
    {
        this.book = book;
    }

    //~ Methods ------------------------------------------------------------------------------------
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
        final OutputStream os = new FileOutputStream(path.toString());
        export(os, signed, rootName);
        os.close();
        logger.info("Opus {} exported to {}", rootName, path);
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
     * @throws Exception
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
            String entryName = rootName
                               + (multi ? (".mvt" + score.getId()) : "")
                               + OMR.SCORE_EXTENSION;
            com.audiveris.proxymusic.opus.Score oScore = opusFactory.createScore();
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

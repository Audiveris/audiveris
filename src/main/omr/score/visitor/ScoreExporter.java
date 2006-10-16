//----------------------------------------------------------------------------//
//                                                                            //
//                      E x p o r t i n g V i s i t o r                       //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.Main;

import omr.score.Barline;
import omr.score.Clef;
import omr.score.KeySignature;
import omr.score.Measure;
import omr.score.MusicNode;
import omr.score.Score;
import omr.score.ScoreFormat;
import omr.score.ScorePartWise;
import omr.score.Slur;
import omr.score.Staff;
import omr.score.StaffNode;
import omr.score.System;
import omr.score.TimeSignature;

import omr.util.Logger;

import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IXMLWriter;
import org.jibx.runtime.JiBXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Class <code>ScoreExporter</code> can visit the score hierarchy to export
 * the score to a MusicXML file
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScoreExporter
    implements Visitor
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = Logger.getLogger(ScoreExporter.class);

    //~ Instance fields --------------------------------------------------------

    /** The score avatar built precisely for export via JiBX */
    private ScorePartWise scorePartWise;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ScoreExporter object.
     *
     * @param score the score to export
     * @param xmlFile the xml file to write, or null
     */
    public ScoreExporter (Score score,
                          File  xmlFile)
    {
        // Where do we write the score xml file?
        if (xmlFile == null) {
            xmlFile = new File(
                Main.getOutputFolder(),
                score.getRadix() + ScoreFormat.MUSIC_XML.extension);
        }

        // Make sure the folder exists
        File folder = new File(xmlFile.getParent());

        if (!folder.exists()) {
            logger.info("Creating folder " + folder);
            folder.mkdirs();
        }

        try {
            // Prepare the marshalling context
            IBindingFactory     factory = BindingDirectory.getFactory(
                ScorePartWise.class);
            IMarshallingContext mctx = factory.createMarshallingContext();

            // Document prologue
            mctx.startDocument(
                "UTF-8", // encoding
                true, // standalone
                new FileOutputStream(xmlFile));
            mctx.setIndent(3);

            IXMLWriter writer = mctx.getXmlWriter();
            writer.writeDocType(
                "score-partwise", // root element name
                "http://www.musicxml.org/dtds/partwise.dtd", // system ID
                "-//Recordare//DTD MusicXML 1.1 Partwise//EN", //public ID
                null); // internal subset

            // Build and populate a score facade, thenmarshall the facade
            scorePartWise = new ScorePartWise(score);
            score.accept(this);
            mctx.marshalDocument(scorePartWise);

            // Document epilogue
            mctx.endDocument();
            logger.info("Score exported to " + xmlFile);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (JiBXException ex) {
            ex.printStackTrace();
        }
    }

    //~ Methods ----------------------------------------------------------------

    public boolean visit (Barline barline)
    {
        return true;
    }

    public boolean visit (Clef clef)
    {
        return true;
    }

    public boolean visit (KeySignature keySignature)
    {
        return true;
    }

    public boolean visit (Measure measure)
    {
        if (measure.getLeftX() != null) {
            logger.info("Adding " + measure);

            scorePartWise.getMeasures()
                         .add(measure);
        } else {
            logger.info("Skipping " + measure);
        }

        return true;
    }

    public boolean visit (MusicNode musicNode)
    {
        return true;
    }

    public boolean visit (Score score)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Exporting score ...");
        }

        score.acceptChildren(this);
        logger.info("measures built nb=" + scorePartWise.getMeasures().size());

        return false;
    }

    public boolean visit (Slur slur)
    {
        return true;
    }

    public boolean visit (Staff staff)
    {
        return true;
    }

    public boolean visit (StaffNode staffNode)
    {
        return true;
    }

    public boolean visit (System system)
    {
        return true;
    }

    public boolean visit (TimeSignature timeSignature)
    {
        return true;
    }
}

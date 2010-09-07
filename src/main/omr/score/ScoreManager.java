//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e M a n a g e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.midi.MidiAbstractions;
import omr.score.midi.MidiAgent;
import omr.score.ui.PaintingParameters;
import omr.score.ui.ScoreActions;
import omr.score.ui.ScoreOrientation;
import omr.score.ui.ScorePainter;
import omr.score.ui.ScoreView;

import omr.sheet.SheetBench;

import omr.ui.view.Zoom;

import com.itextpdf.text.Document;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.*;

/**
 * Class <code>ScoreManager</code> handles a collection of score instances.
 *
 * @author Hervé Bitteur and Brenton Partridge
 */
public class ScoreManager
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreManager.class);

    /** The extension used for score output files: {@value} */
    public static final String SCORE_EXTENSION = ".xml";

    /** The extension used for score bench files: {@value} */
    public static final String BENCH_EXTENSION = ".bench.properties";

    /** The single instance of this class */
    private static volatile ScoreManager INSTANCE;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // ScoreManager //
    //--------------//
    /**
     * Creates a Score Manager.
     */
    private ScoreManager ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this class
     *
     * @return the single instance
     */
    public static ScoreManager getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new ScoreManager();
        }

        return INSTANCE;
    }

    //----------------------//
    // getDefaultExportFile //
    //----------------------//
    /**
     * Report the file to which the score would be written by default
     * @param score the score to export
     * @return the default file
     */
    public File getDefaultExportFile (Score score)
    {
        return (score.getExportFile() != null) ? score.getExportFile()
               : new File(
            constants.defaultScoreDirectory.getValue(),
            score.getRadix() + SCORE_EXTENSION);
    }

    //--------------------//
    // getDefaultMidiFile //
    //--------------------//
    /**
     * Report the file to which the MIDI data would be written by default
     * @param score the score to export
     * @return the default file
     */
    public File getDefaultMidiFile (Score score)
    {
        return (score.getMidiFile() != null) ? score.getMidiFile()
               : new File(
            constants.defaultMidiDirectory.getValue(),
            score.getRadix() + MidiAbstractions.MIDI_EXTENSION);
    }

    //-------------------//
    // getDefaultPdfFile //
    //-------------------//
    /**
     * Report the file to which the PDF data would be written by default
     * @param score the score to export
     * @return the default file
     */
    public File getDefaultPdfFile (Score score)
    {
        return (score.getPdfFile() != null) ? score.getPdfFile()
               : new File(
            constants.defaultPdfDirectory.getValue(),
            score.getRadix() + ".pdf");
    }

    //--------//
    // export //
    //--------//
    /**
     * Export a score using the partwise structure of MusicXML to the default
     * file for the provided score
     *
     * @param score the score to export
     */
    public void export (Score score)
    {
        export(score, score.getExportFile(), null);
    }

    //--------//
    // export //
    //--------//
    /**
     * Export a score using the partwise structure of MusicXML to the provided
     * file
     *
     * @param score the score to export
     * @param exportFile the xml file to write, or null
     * @param injectSignature should we inject our signature?
     */
    public void export (Score   score,
                        File    exportFile,
                        Boolean injectSignature)
    {
        if (exportFile == null) {
            exportFile = getDefaultExportFile(score);
        }

        // Make sure the folder exists
        File folder = new File(exportFile.getParent());

        if (folder.mkdirs()) {
            logger.info("Creating folder " + folder);
        }

        // Actually export the score material
        try {
            ScoreExporter exporter = new ScoreExporter(score);

            if (injectSignature != null) {
                exporter.export(exportFile, injectSignature);
            } else {
                exporter.export(
                    exportFile,
                    constants.defaultInjectSignature.getValue());
            }

            logger.info("Score exported to " + exportFile);

            // Remember (even across runs) the selected directory
            constants.defaultScoreDirectory.setValue(exportFile.getParent());

            // Remember the file in the score itself
            score.setExportFile(exportFile);
        } catch (Exception ex) {
            logger.warning("Error storing score to " + exportFile, ex);
        }
    }

    //-----------//
    // midiClose //
    //-----------//
    /**
     * Cut any relationship between the provided score and the Midi interface
     * (MidiAgent & MidiReceiver) if any
     *
     * @param score the score being closed
     */
    public void midiClose (Score score)
    {
        try {
            MidiAgent agent = MidiAgent.getInstance();

            if (agent.getScore() == score) {
                agent.setScore(null);
            }
        } catch (Exception ex) {
            logger.warning("Error closing Midi interface ", ex);
        }
    }

    //-----------//
    // midiWrite //
    //-----------//
    /**
     * Write the Midi sequence of the score into the provided midi file.
     *
     * @param score the provided score
     * @param midiFile the Midi file to write
     * @throws Exception if the writing goes wrong
     */
    public void midiWrite (Score score,
                           File  midiFile)
        throws Exception
    {
        if (!ScoreActions.checkParameters(score)) {
            return;
        }

        if (midiFile == null) {
            midiFile = getDefaultMidiFile(score);
        }

        // Make sure the folder exists
        File folder = new File(midiFile.getParent());

        if (folder.mkdirs()) {
            logger.info("Creating folder " + folder);
        }

        // Actually write the Midi file
        try {
            MidiAgent agent = MidiAgent.getInstance();

            if (agent.getScore() != score) {
                agent.setScore(score);
            }

            agent.write(midiFile);
            score.setMidiFile(midiFile);
            logger.info("Midi written to " + midiFile);

            // Remember (even across runs) the selected directory
            constants.defaultMidiDirectory.setValue(midiFile.getParent());
        } catch (Exception ex) {
            logger.warning("Cannot write Midi to " + midiFile, ex);
            throw ex;
        }
    }

    //----------//
    // pdfWrite //
    //----------//
    /**
     * Print the score into the provided PDF file.
     *
     * @param score the provided score
     * @param pdfFile the PDF file to write
     */
    public void pdfWrite (Score score,
                          File  pdfFile)
    {
        if (pdfFile == null) {
            pdfFile = getDefaultPdfFile(score);
        }

        // Make sure the folder exists
        File folder = new File(pdfFile.getParent());

        if (folder.mkdirs()) {
            logger.info("Creating folder " + folder);
        }

        // Actually write the PDF file
        try {
            new PdfOutput(score, pdfFile).write();
            score.setPdfFile(pdfFile);
            logger.info("Score printed to " + pdfFile);

            // Remember (even across runs) the selected directory
            constants.defaultPdfDirectory.setValue(pdfFile.getParent());
        } catch (Exception ex) {
            logger.warning("Cannot write PDF to " + pdfFile, ex);
        }
    }

    //------------//
    // storeBench //
    //------------//
    /**
     * Store the sheet bench
     * @param bench the bench to write to disk
     * @param file the written file, or null
     * @param complete true if we need to complete the bench data
     */
    public void storeBench (SheetBench bench,
                            File       file,
                            boolean    complete)
    {
        // Check if we do save bench data
        if (!Main.hasBenchFlag() && !constants.saveBenchToDisk.getValue()) {
            return;
        }

        if (file == null) {
            file = new File(
                constants.defaultBenchDirectory.getValue(),
                bench.getRadix() + BENCH_EXTENSION);
        }

        // Make sure the folder exists
        File folder = new File(file.getParent());

        if (folder.mkdirs()) {
            logger.info("Creating folder " + folder);
        }

        // Actually store the score bench
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file);
            bench.store(fos, complete);

            if (complete) {
                logger.info("Complete score bench stored as " + file);
            }

            // Remember (even across runs) the selected directory
            constants.defaultBenchDirectory.setValue(file.getParent());
        } catch (Exception ex) {
            logger.warning("Error storing score bench to " + file, ex);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Default directory for saved scores */
        Constant.String defaultScoreDirectory = new Constant.String(
            "",
            "Default directory for saved scores");

        /** Should we save bench data to disk */
        Constant.Boolean saveBenchToDisk = new Constant.Boolean(
            false,
            "Should we save bench data to disk");

        /** Default directory for saved benches */
        Constant.String defaultBenchDirectory = new Constant.String(
            "",
            "Default directory for saved benches");

        /** Default directory for writing Midi files */
        Constant.String defaultMidiDirectory = new Constant.String(
            "",
            "Default directory for writing Midi files");

        /** Default directory for writing PDF files */
        Constant.String defaultPdfDirectory = new Constant.String(
            "",
            "Default directory for writing PDF files");

        /** Should we export our signature? */
        Constant.Boolean defaultInjectSignature = new Constant.Boolean(
            true,
            "Should we export our signature?");
    }

    //-----------//
    // PdfOutput //
    //-----------//
    private static class PdfOutput
        extends ScoreView
    {
        //~ Instance fields ----------------------------------------------------

        /** The file to print to */
        private File file;

        //~ Constructors -------------------------------------------------------

        public PdfOutput (Score score,
                          File  file)
        {
            super(
                score,
                score.getLayout(ScoreOrientation.VERTICAL),
                PaintingParameters.getInstance());
            this.file = file;
        }

        //~ Methods ------------------------------------------------------------

        public void write ()
            throws Exception
        {
            Document document = null;

            try {
                Dimension dim = scoreLayout.getScoreDimension();
                document = new Document(new Rectangle(dim.width, dim.height));

                FileOutputStream fos = new FileOutputStream(file);
                PdfWriter        writer = PdfWriter.getInstance(document, fos);
                document.open();

                PdfContentByte cb = writer.getDirectContent();
                Graphics2D     g2 = cb.createGraphics(dim.width, dim.height);
                g2.scale(1, 1);

                // Painting
                Zoom         zoom = new Zoom(1);
                ScorePainter painter = new ScorePainter(scoreLayout, g2, zoom);
                score.accept(painter);

                // This is the end...
                g2.dispose();
            } catch (Exception ex) {
                logger.warning("Error printing " + file, ex);
                throw ex;
            } finally {
                if (document != null) {
                    document.close();
                }
            }
        }
    }
}

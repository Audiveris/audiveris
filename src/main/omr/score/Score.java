//----------------------------------------------------------------------------//
//                                                                            //
//                                 S c o r e                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.text.Language;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import omr.math.Rational;

import omr.run.FilterDescriptor;

import omr.score.entity.MeasureId.MeasureRange;
import omr.score.entity.Page;
import omr.score.entity.ScoreNode;
import omr.score.entity.ScorePart;
import omr.score.entity.Tempo;
import omr.score.ui.ScoreTree;
import omr.score.visitor.ScoreVisitor;

import omr.script.ParametersTask.PartData;
import omr.script.Script;
import omr.script.ScriptActions;

import omr.sheet.Sheet;
import omr.sheet.picture.PictureLoader;
import omr.sheet.ui.SheetActions;
import omr.sheet.ui.SheetsController;

import omr.step.StepException;

import omr.util.Param;
import omr.util.FileUtil;
import omr.util.TreeNode;

import java.awt.image.RenderedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;

import javax.swing.JFrame;

/**
 * Class {@code Score} handles a score hierarchy, composed of one or
 * several pages.
 *
 * @author Hervé Bitteur
 */
public class Score
        extends ScoreNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Score.class);

    /** Number of lines in a staff */
    public static final int LINE_NB = 5;

    //~ Instance fields --------------------------------------------------------
    /** Input file of the related image(s) */
    private final File imageFile;

    /** The related file radix (name w/o extension) */
    private final String radix;

    /** True if the score contains several pages */
    private boolean multiPage;

    /** The recording of key processing data */
    private ScoreBench bench;

    /** Dominant text language in the score */
    private String language;

    /** Greatest duration divisor */
    private Integer durationDivisor;

    /** ScorePart list for the whole score */
    private List<ScorePart> partList;

    /** The specified volume, if any */
    private Integer volume;

    /** Potential measure range, if not all score is to be played */
    private MeasureRange measureRange;

    /** Browser tree on this score */
    private ScoreTree scoreTree;

    /** Where the MusicXML output is to be stored */
    private File exportFile;

    /** Where the script is to be stored */
    private File scriptFile;

    /** Where the MIDI data is to be stored */
    private File midiFile;

    /** Where the sheet PDF data is to be stored */
    private File printFile;

    /** The script of user actions on this score */
    private Script script;

    /** Handling of binarization filter parameter. */
    private final Param<FilterDescriptor> filterParam =
            new Param<>(FilterDescriptor.defaultFilter);

    /** Handling of tempo parameter. */
    private final Param<Integer> tempoParam =
            new Param<>(Tempo.defaultTempo);

    /** Handling of language parameter. */
    private final Param<String> textParam =
            new Param<>(Language.defaultSpecification);

    /** Handling of parts name and program. */
    private final Param<List<PartData>> partsParam = new PartsParam();

    //~ Constructors -----------------------------------------------------------
    //-------//
    // Score //
    //-------//
    /**
     * Create a Score with a path to an input image file.
     *
     * @param imageFile the input image file (which may contain several images)
     */
    public Score (File imageFile)
    {
        super(null); // No container

        this.imageFile = imageFile;
        radix = FileUtil.getNameSansExtension(imageFile);

        // Related bench
        bench = new ScoreBench(this);

        // Register this score instance
        ScoresManager.getInstance().addInstance(this);
    }

    //~ Methods ----------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //-------//
    // close //
    //-------//
    /**
     * Close this score instance, as well as its view if any.
     */
    public void close ()
    {
        logger.info("Closing {}", this);

        // Check whether the score script has been saved (or user has declined)
        if ((Main.getGui() != null) && !ScriptActions.checkStored(getScript())) {
            return;
        }

        // Close contained sheets (and pages)
        for (TreeNode pn : new ArrayList<>(getPages())) {
            Page page = (Page) pn;
            Sheet sheet = page.getSheet();
            sheet.remove(true);
        }

        // Close tree if any
        if (scoreTree != null) {
            scoreTree.close();
        }

        // Complete and store all bench data
        ScoresManager.getInstance().storeBench(bench, null, true);

        // Remove from score instances
        ScoresManager.getInstance().removeInstance(this);
    }

    //-------------//
    // createPages //
    //-------------//
    /**
     * Create as many pages (and related sheets) as there are images
     * in the input image file.
     *
     * @param pages set of page ids (1-based) explicitly included.
     *              if set is empty or null all pages are loaded
     */
    public void createPages (SortedSet<Integer> pages)
    {
        SortedMap<Integer, RenderedImage> images = PictureLoader.loadImages(
                imageFile,
                pages);

        if (images != null) {
            Page firstPage = null;
            setMultiPage(images.size() > 1); // Several images in the file

            for (Entry<Integer, RenderedImage> entry : images.entrySet()) {
                int index = entry.getKey();
                RenderedImage image = entry.getValue();
                Page page = null;

                try {
                    page = new Page(this, index, image);

                    if (firstPage == null) {
                        firstPage = page;

                        // Let the UI focus on first page
                        if (Main.getGui() != null) {
                            SheetsController.getInstance().showAssembly(firstPage.
                                    getSheet());
                        }
                    }
                } catch (StepException ex) {
                    // Remove page from score, if already included
                    if ((page != null) && getPages().remove(page)) {
                        logger.info("Page #{} removed", index);
                    }
                }
            }

            // Remember (even across runs) the parent directory
            ScoresManager.getInstance().setDefaultInputDirectory(getImageFile().
                    getParent());

            // Insert in sheet history
            ScoresManager.getInstance().getHistory().add(getImagePath());
            if (Main.getGui() != null) {
                SheetActions.HistoryMenu.getInstance().setEnabled(true);
            }
        }
    }

    //------//
    // dump //
    //------//
    /**
     * Dump a whole score hierarchy.
     */
    public void dump ()
    {
        System.out.println(
                "----------------------------------------------------------------");

        if (dumpNode()) {
            dumpChildren(1);
        }

        System.out.println(
                "----------------------------------------------------------------");
    }

    //----------------//
    // getFilterParam //
    //----------------//
    public Param<FilterDescriptor> getFilterParam ()
    {
        return filterParam;
    }

    //----------------//
    // getTempoParam //
    //----------------//
    public Param<Integer> getTempoParam ()
    {
        return tempoParam;
    }

    //--------------//
    // getTextParam //
    //--------------//
    public Param<String> getTextParam ()
    {
        return textParam;
    }

    //---------------//
    // getPartsParam //
    //---------------//
    public Param<List<PartData>> getPartsParam ()
    {
        return partsParam;
    }

    //------------------//
    // getDefaultVolume //
    //------------------//
    /**
     * Report default value for Midi volume.
     *
     * @return the default volume value
     */
    public static int getDefaultVolume ()
    {
        return constants.defaultVolume.getValue();
    }

    //--------------------//
    // getDurationDivisor //
    //--------------------//
    /**
     * Report the common divisor used for this score when
     * simplifying the durations.
     *
     * @return the computed divisor (GCD), or null if not computable
     */
    public Integer getDurationDivisor ()
    {
        if (durationDivisor == null) {
            accept(new ScoreReductor());
        }

        return durationDivisor;
    }

    //---------------//
    // getExportFile //
    //---------------//
    /**
     * Report to which file, if any, the score is to be exported.
     *
     * @return the exported xml file, or null
     */
    public File getExportFile ()
    {
        return exportFile;
    }

    //--------------//
    // getFirstPage //
    //--------------//
    public Page getFirstPage ()
    {
        if (children.isEmpty()) {
            return null;
        } else {
            return (Page) children.get(0);
        }
    }

    //--------------//
    // getImageFile //
    //--------------//
    /**
     * @return the imageFile
     */
    public File getImageFile ()
    {
        return imageFile;
    }

    //--------------//
    // getImagePath //
    //--------------//
    /**
     * Report the (canonical) file name of the score image(s).
     *
     * @return the file name
     */
    public String getImagePath ()
    {
        return imageFile.getPath();
    }

    //--------------------//
    // getMeasureIdOffset //
    //--------------------//
    /**
     * Report the offset to add to page-based measure ids of the
     * provided page to get absolute (score-based) ids.
     *
     * @param page the provided page
     * @return the measure id offset for the page
     */
    public Integer getMeasureIdOffset (Page page)
    {
        int offset = 0;

        for (TreeNode pn : getPages()) {
            Page p = (Page) pn;

            if (p == page) {
                return offset;
            } else {
                Integer delta = p.getDeltaMeasureId();

                if (delta != null) {
                    offset += delta;
                } else {
                    // This page has no measures yet, so ...
                    return null;
                }
            }
        }

        throw new IllegalArgumentException(page + " not found in score");
    }

    //-----------------//
    // getMeasureRange //
    //-----------------//
    /**
     * Report the potential range of selected measures.
     *
     * @return the selected measure range, perhaps null
     */
    public MeasureRange getMeasureRange ()
    {
        return measureRange;
    }

    //-------------//
    // getMidiFile //
    //-------------//
    /**
     * Report to which file, if any, the MIDI data is to be exported.
     *
     * @return the Midi file, or null
     */
    public File getMidiFile ()
    {
        return midiFile;
    }

    //---------//
    // getPage //
    //---------//
    /**
     * Report the page with provided page-index.
     *
     * @param pageIndex the desired value for page index
     * @return the proper page, or null if not found
     */
    public Page getPage (int pageIndex)
    {
        for (TreeNode pn : getPages()) {
            Page page = (Page) pn;

            if (page.getIndex() == pageIndex) {
                return page;
            }
        }

        return null;
    }

    //----------//
    // getPages //
    //----------//
    /**
     * Report the collection of pages in that score.
     *
     * @return the pages
     */
    public List<TreeNode> getPages ()
    {
        return getChildren();
    }

    //-------------//
    // isMultiPage //
    //-------------//
    /**
     * @return the multiPage
     */
    public boolean isMultiPage ()
    {
        return multiPage;
    }

    //--------//
    // isIdle //
    //--------//
    /**
     * Check whether this score is idle or not.
     * The score is busy when at least one of its pages/sheets is under a step
     * processing.
     *
     * @return true if idle, false if busy
     */
    public boolean isIdle ()
    {
        for (TreeNode pn : getPages()) {
            Page page = (Page) pn;
            Sheet sh = page.getSheet();
            if (sh != null && sh.getCurrentStep() != null) {
                return false;
            }
        }
        return true;
    }

    //-----------------//
    // setDefaultTempo //
    //-----------------//
    /**
     * Assign default value for Midi tempo.
     *
     * @param tempo the default tempo value
     */
    public static void setDefaultTempo (int tempo)
    {
        constants.defaultTempo.setValue(tempo);
    }

    //------------------//
    // setDefaultVolume //
    //------------------//
    /**
     * Assign default value for Midi volume.
     *
     * @param volume the default volume value
     */
    public static void setDefaultVolume (int volume)
    {
        constants.defaultVolume.setValue(volume);
    }

    //----------//
    // getBench //
    //----------//
    /**
     * Report the related sheet bench.
     *
     * @return the related bench
     */
    public ScoreBench getBench ()
    {
        return bench;
    }

    //-----------------//
    // getBrowserFrame //
    //-----------------//
    /**
     * Create a dedicated frame, where all score elements can be
     * browsed in the tree hierarchy.
     *
     * @return the created frame
     */
    public JFrame getBrowserFrame ()
    {
        if (scoreTree == null) {
            // Build the ScoreTree on the score
            scoreTree = new ScoreTree(this);
        }

        return scoreTree.getFrame();
    }

    //-------------//
    // getLastPage //
    //-------------//
    public Page getLastPage ()
    {
        if (children.isEmpty()) {
            return null;
        } else {
            return (Page) children.get(children.size() - 1);
        }
    }

    //------------------//
    // getMeasureOffset //
    //------------------//
    /**
     * Report the offset to add to page-based measure index of the
     * provided page to get absolute (score-based) indices.
     *
     * @param page the provided page
     * @return the measure index offset for the page
     */
    public int getMeasureOffset (Page page)
    {
        int offset = 0;

        for (TreeNode pn : getPages()) {
            Page p = (Page) pn;

            if (p == page) {
                return offset;
            } else {
                offset += p.getMeasureCount();
            }
        }

        throw new IllegalArgumentException(page + " not found in score");
    }

    //-------------//
    // getPartList //
    //-------------//
    /**
     * Report the global list of parts.
     *
     * @return partList the list of score parts
     */
    public List<ScorePart> getPartList ()
    {
        return partList;
    }

    //--------------//
    // getPrintFile //
    //--------------//
    /**
     * Report to which file, if any, the sheet PDF data is to be written.
     *
     * @return the sheet PDF file, or null
     */
    public File getPrintFile ()
    {
        return printFile;
    }

    //----------//
    // getRadix //
    //----------//
    /**
     * Report the radix of the file that corresponds to the score.
     * It is based on the simple file name of the score, with no path and no
     * extension.
     *
     * @return the score input file radix
     */
    public String getRadix ()
    {
        return radix;
    }

    //--------------//
    // getLogPrefix //
    //--------------//
    /**
     * Report the proper prefix to use when logging a message
     *
     * @return the proper prefix
     */
    public String getLogPrefix ()
    {
        if (ScoresManager.isMultiScore()) {
            return "[" + radix + "] ";
        } else {
            return "";
        }
    }

    //-----------//
    // getScript //
    //-----------//
    public Script getScript ()
    {
        if (script == null) {
            script = new Script(this);
        }

        return script;
    }

    //---------------//
    // getScriptFile //
    //---------------//
    /**
     * Report the file, if any, where the script should be written.
     *
     * @return the related script file or null
     */
    public File getScriptFile ()
    {
        return scriptFile;
    }

    //-----------//
    // getVolume //
    //-----------//
    /**
     * Report the assigned volume, if any.
     * If the value is not yet set, it is set to the default value and returned.
     *
     * @return the assigned volume, or null
     */
    public Integer getVolume ()
    {
        if (!hasVolume()) {
            volume = getDefaultVolume();
        }

        return volume;
    }

    //-------------//
    // hasLanguage //
    //-------------//
    /**
     * Check whether a language has been defined for this score.
     *
     * @return true if a language is defined
     */
    public boolean hasLanguage ()
    {
        return language != null;
    }

    //-----------//
    // hasVolume //
    //-----------//
    /**
     * Check whether a volumehas been defined for this score.
     *
     * @return true if a volume is defined
     */
    public boolean hasVolume ()
    {
        return volume != null;
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove a page
     */
    public void remove (Page page)
    {
        getPages().remove(page);
        setMultiPage(getPages().size() > 1);
    }

    //--------------------//
    // setDurationDivisor //
    //--------------------//
    /**
     * Remember the common divisor used for this score when
     * simplifying the durations.
     *
     * @param durationDivisor the computed divisor (GCD), or null
     */
    public void setDurationDivisor (Integer durationDivisor)
    {
        this.durationDivisor = durationDivisor;
    }

    //---------------//
    // setExportFile //
    //---------------//
    /**
     * Remember to which file the score is to be exported.
     *
     * @param exportFile the exported xml file
     */
    public void setExportFile (File exportFile)
    {
        this.exportFile = exportFile;
    }

    //-------------//
    // setLanguage //
    //-------------//
    /**
     * Set the score dominant language.
     *
     * @param language the dominant language
     */
    public void setLanguage (String language)
    {
        this.language = language;
    }

    //-----------------//
    // setMeasureRange //
    //-----------------//
    /**
     * Remember a range of measure for this score.
     *
     * @param measureRange the range of selected measures
     */
    public void setMeasureRange (MeasureRange measureRange)
    {
        this.measureRange = measureRange;
    }

    //-------------//
    // setMidiFile //
    //-------------//
    /**
     * Remember to which file the MIDI data is to be exported.
     *
     * @param midiFile the Midi file
     */
    public void setMidiFile (File midiFile)
    {
        this.midiFile = midiFile;
    }

    //-------------//
    // setPartList //
    //-------------//
    /**
     * Assign a part list valid for the whole score.
     *
     * @param partList the list of score parts
     */
    public void setPartList (List<ScorePart> partList)
    {
        this.partList = partList;
    }

    //--------------//
    // setPrintFile //
    //--------------//
    /**
     * Remember to which file the sheet PDF data is to be exported.
     *
     * @param sheetPdfFile the sheet PDF file
     */
    public void setPrintFile (File sheetPdfFile)
    {
        this.printFile = sheetPdfFile;
    }

    //---------------//
    // setScriptFile //
    //---------------//
    /**
     * Remember the file where the script is written.
     *
     * @param scriptFile the related script file
     */
    public void setScriptFile (File scriptFile)
    {
        this.scriptFile = scriptFile;
    }

    //-----------//
    // setVolume //
    //-----------//
    /**
     * Assign a volume value.
     *
     * @param volume the volume value to be assigned
     */
    public void setVolume (Integer volume)
    {
        this.volume = volume;
    }

    //------------------//
    // simpleDurationOf //
    //------------------//
    /**
     * Export a duration to its simplest form, based on the greatest
     * duration divisor of the score.
     *
     * @param value the raw duration
     * @return the simple duration expression, in the param of proper
     *         divisions
     */
    public int simpleDurationOf (Rational value)
    {
        return value.num * (getDurationDivisor() / value.den);
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a readable description.
     *
     * @return a string based on its XML file name
     */
    @Override
    public String toString ()
    {
        if (getRadix() != null) {
            return "{Score " + getRadix() + "}";
        } else {
            return "{Score }";
        }
    }

    //--------------//
    // setMultiPage //
    //--------------//
    /**
     * @param multiPage the multiPage to set.
     */
    private void setMultiPage (boolean multiPage)
    {
        this.multiPage = multiPage;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Integer defaultTempo = new Constant.Integer(
                "QuartersPerMn",
                120,
                "Default tempo, stated in number of quarters per minute");

        Constant.Integer defaultVolume = new Constant.Integer(
                "Volume",
                78,
                "Default Volume in 0..127 range");

    }

    //------------//
    // PartsParam //
    //------------//
    private class PartsParam
            extends Param<List<PartData>>
    {

        @Override
        public List<PartData> getSpecific ()
        {
            List<ScorePart> list = getPartList();
            if (list != null) {
                List<PartData> data = new ArrayList<>();
                for (ScorePart scorePart : list) {

                    // Initial setting for part midi program
                    int prog = (scorePart.getMidiProgram() != null)
                            ? scorePart.getMidiProgram()
                            : scorePart.getDefaultProgram();

                    data.add(new PartData(scorePart.getName(), prog));
                }
                return data;
            } else {
                return null;
            }
        }

        @Override
        public boolean setSpecific (List<PartData> specific)
        {
            try {
                for (int i = 0; i < specific.size(); i++) {
                    PartData data = specific.get(i);
                    ScorePart scorePart = getPartList().get(i);

                    // Part name
                    scorePart.setName(data.name);

                    // Part midi program
                    scorePart.setMidiProgram(data.program);
                }

                logger.info("Score parts have been updated");

                return true;
            } catch (Exception ex) {
                logger.warn("Error updating score parts", ex);
            }

            return false;
        }
    }
}

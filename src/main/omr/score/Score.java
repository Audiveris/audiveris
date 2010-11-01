//----------------------------------------------------------------------------//
//                                                                            //
//                                 S c o r e                                  //
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

import omr.glyph.text.Language;

import omr.log.Logger;

import omr.score.common.PixelDimension;
import omr.score.common.PixelPoint;
import omr.score.entity.ScoreNode;
import omr.score.entity.ScorePart;
import omr.score.entity.ScoreSystem;
import omr.score.entity.SlotPolicy;
import omr.score.entity.SystemPart;
import omr.score.ui.ScoreEditor;
import omr.score.ui.ScoreLayout;
import omr.score.ui.ScoreOrientation;
import omr.score.ui.ScoreTree;
import omr.score.ui.ScoreView;
import omr.score.visitor.ScoreVisitor;

import omr.selection.ScoreLocationEvent;

import omr.sheet.Scale;
import omr.sheet.Scale.InterlineFraction;
import omr.sheet.Sheet;

import omr.util.TreeNode;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;

import javax.swing.JFrame;

/**
 * Class <code>Score</code> handles a score hierarchy, composed of one or
 * several systems of staves.
 *
 * <p>There is no more notion of pages, since all sheet parts are supposed to
 * have been deskewed and concatenated beforehand in one single picture and thus
 * one single score.
 *
 * <p>All distances and coordinates are assumed to be expressed in Units
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
    private static final Logger logger = Logger.getLogger(Score.class);

    /** Number of lines in a staff */
    public static final int LINE_NB = 5;

    //~ Instance fields --------------------------------------------------------

    /** File of the related sheet image */
    private File imageFile;

    /** Link with image */
    private Sheet sheet;

    /** The related file radix (name w/o extension) */
    private String radix;

    /** Sheet dimension in units */
    private PixelDimension dimension;

    /** Sheet skew angle in radians */
    private double skewAngle;

    /** Sheet global scale */
    private Scale scale;

    /** Dominant text language */
    private String language;

    /** Greatest duration divisor */
    private Integer durationDivisor;

    /** ScorePart list for the whole score */
    private List<ScorePart> partList = new ArrayList<ScorePart>();

    /** The sequence of views on this score */
    private List<ScoreView> views = new ArrayList<ScoreView>();

    /** The specified tempo, if any */
    private Integer tempo;

    /** The specified volume, if any */
    private Integer volume;

    /** Potential measure range, if not all score is to be played */
    private MeasureRange measureRange;

    /** Browser tree on this score */
    private ScoreTree scoreTree;

    /** The most recent system pointed at */
    private WeakReference<ScoreSystem> recentSystemRef = null;

    /** The highest system top */
    private int highestSystemTop;

    /** Preferred orientation for system layout */
    private ScoreOrientation orientation;

    /** The score slot policy */
    private SlotPolicy slotPolicy;

    /** The score slot horizontal margin, expressed in interline fraction */
    private InterlineFraction slotMargin;

    /** Where the MusicXML output is to be stored */
    private File exportFile;

    /** Where the script is to be stored */
    private File scriptFile;

    /** Where the MIDI data is to be stored */
    private File midiFile;

    /** Where the sheet PDF data is to be stored */
    private File sheetPdfFile;

    /** Where the score PDF data is to be stored */
    private File scorePdfFile;

    /** Global score and systems layouts per orientation*/
    private final EnumMap<ScoreOrientation, ScoreLayout> layouts = new EnumMap<ScoreOrientation, ScoreLayout>(
        ScoreOrientation.class);

    /** Specific score editor view */
    private ScoreEditor editor;

    /** Average beam thickness, if known */
    private Integer beamThickness;

    //~ Constructors -----------------------------------------------------------

    //-------//
    // Score //
    //-------//
    /**
     * Create a Score, with the specified parameters
     *
     * @param imagePath full name of the original sheet file
     */
    public Score (String imagePath)
    {
        super(null); // No container

        setImagePath(imagePath);

        createLayouts();
    }

    //~ Methods ----------------------------------------------------------------

    //-------------------//
    // getActualDuration //
    //-------------------//
    /**
     * Report the total score duration
     *
     * @return the number of divisions in the score
     */
    public int getActualDuration ()
    {
        ScoreSystem lastSystem = getLastSystem();

        return lastSystem.getStartTime() + lastSystem.getActualDuration();
    }

    //------------------//
    // setBeamThickness //
    //------------------//
    public void setBeamThickness (int beamThickness)
    {
        this.beamThickness = beamThickness;
    }

    //------------------//
    // getBeamThickness //
    //------------------//
    public Integer getBeamThickness ()
    {
        return beamThickness;
    }

    //-----------------//
    // getBrowserFrame //
    //-----------------//
    /**
     * Create a dedicated frame, where all score elements can be browsed in the
     * tree hierarchy
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

    //----------------------//
    // setDefaultSlotMargin //
    //----------------------//
    /**
     * Assign the default slot margin
     * @param fraction the horizontal margin, expressed in interline fraction
     */
    public static void setDefaultSlotMargin (double fraction)
    {
        constants.defaultSlotMargin.setValue(fraction);
    }

    //----------------------//
    // getDefaultSlotMargin //
    //----------------------//
    /**
     * Report the default horizontal Slot margin
     * @return the slotMargin (in interline fraction)
     */
    public static InterlineFraction getDefaultSlotMargin ()
    {
        return constants.defaultSlotMargin.getWrappedValue();
    }

    //----------------------//
    // setDefaultSlotPolicy //
    //----------------------//
    /**
     * Assign the default slot policy
     * @param slotPolicy the slot policy
     */
    public static void setDefaultSlotPolicy (SlotPolicy slotPolicy)
    {
        constants.defaultSlotPolicy.setValue(slotPolicy);
    }

    //----------------------//
    // getDefaultSlotPolicy //
    //----------------------//
    /**
     * Report the default policy to be used for retrieval of time slots
     * @return the default time slot policy
     */
    public static SlotPolicy getDefaultSlotPolicy ()
    {
        return constants.defaultSlotPolicy.getValue();
    }

    //-----------------//
    // setDefaultTempo //
    //-----------------//
    /**
     * Assign default value for Midi tempo
     *
     * @param tempo the default tempo value
     */
    public static void setDefaultTempo (int tempo)
    {
        constants.defaultTempo.setValue(tempo);
    }

    //-----------------//
    // getDefaultTempo //
    //-----------------//
    /**
     * Report default value for Midi tempo
     *
     * @return the default tempo value
     */
    public static int getDefaultTempo ()
    {
        return constants.defaultTempo.getValue();
    }

    //------------------//
    // setDefaultVolume //
    //------------------//
    /**
     * Assign default value for Midi volume
     *
     * @param volume the default volume value
     */
    public static void setDefaultVolume (int volume)
    {
        constants.defaultVolume.setValue(volume);
    }

    //------------------//
    // getDefaultVolume //
    //------------------//
    /**
     * Report default value for Midi volume (volume)
     *
     * @return the default volume value
     */
    public static int getDefaultVolume ()
    {
        return constants.defaultVolume.getValue();
    }

    //-------------------//
    // getMinSlotSpacing //
    //-------------------//
    /**
     * Report the minimum acceptable spacing between slots
     * @return the minimum spacing (in interline fraction)
     */
    public static InterlineFraction getMinSlotSpacing ()
    {
        return constants.minSlotSpacing.getWrappedValue();
    }

    //--------------//
    // setDimension //
    //--------------//
    /**
     * Assign score dimension
     * @param dimension the score dimension, expressed in units
     */
    public void setDimension (PixelDimension dimension)
    {
        this.dimension = dimension;
    }

    //--------------//
    // getDimension //
    //--------------//
    /**
     * Report the dimension of the sheet/score
     *
     * @return the score/sheet dimension in units
     */
    public PixelDimension getDimension ()
    {
        return dimension;
    }

    //--------------------//
    // setDurationDivisor //
    //--------------------//
    /**
     * Remember the common divisor used for this score when simplifying the
     * durations
     *
     * @param durationDivisor the computed divisor (GCD), or null
     */
    public void setDurationDivisor (Integer durationDivisor)
    {
        this.durationDivisor = durationDivisor;
    }

    //--------------------//
    // getDurationDivisor //
    //--------------------//
    /**
     * Report the common divisor used for this score when simplifying the
     * durations
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

    //-----------//
    // setEditor //
    //-----------//
    /**
     * @param editor the editor to set
     */
    public void setEditor (ScoreEditor editor)
    {
        if (this.editor != null) {
            views.remove(this.editor);
        }

        this.editor = editor;

        if (editor != null) {
            addView(editor);
        }
    }

    //-----------//
    // setEditor //
    //-----------//
    /**
     * @return the editor
     */
    public ScoreEditor getEditor ()
    {
        return editor;
    }

    //---------------//
    // setExportFile //
    //---------------//
    /**
     * Remember to which file the score is to be exported
     * @param exportFile the exported xml file
     */
    public void setExportFile (File exportFile)
    {
        this.exportFile = exportFile;
    }

    //---------------//
    // getExportFile //
    //---------------//
    /**
     * Report to which file, if any, the score is to be exported
     * @return the exported xml file, or null
     */
    public File getExportFile ()
    {
        return exportFile;
    }

    //----------------//
    // getFirstSystem //
    //----------------//
    /**
     * Report the first system in the score
     *
     * @return the first system
     */
    public ScoreSystem getFirstSystem ()
    {
        if (children.isEmpty()) {
            return null;
        } else {
            return (ScoreSystem) children.get(0);
        }
    }

    //--------------//
    // setImagePath //
    //--------------//
    /**
     * Assign the (canonical) file name of the score image.
     *
     * @param path the file name
     */
    public void setImagePath (String path)
    {
        try {
            imageFile = new File(path).getCanonicalFile();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    //--------------//
    // getImagePath //
    //--------------//
    /**
     * Report the (canonical) file name of the score image.
     *
     * @return the file name
     */
    public String getImagePath ()
    {
        return imageFile.getPath();
    }

    //-------------//
    // setLanguage //
    //-------------//
    /**
     * Set the score dominant language
     * @param language the dominant language
     */
    public void setLanguage (String language)
    {
        this.language = language;
    }

    //-------------//
    // getLanguage //
    //-------------//
    /**
     * Report the dominant language in the score text
     * If the value is not yet set, it is set to the default value and returned.
     * @return the dominant language
     */
    public String getLanguage ()
    {
        if (!hasLanguage()) {
            language = Language.getDefaultLanguage();
        }

        return language;
    }

    //------------------//
    // getLastSoundTime //
    //------------------//
    /**
     * Report the time, counted from beginning of the score, when sound stops,
     * which means that ending rests are not counted.
     *
     * @param measureId a potential constraint on id of final measure,
     * null for no constraint
     * @return the time of last Midi "note off"
     */
    public int getLastSoundTime (Integer measureId)
    {
        // Browse systems backwards
        for (ListIterator it = getSystems()
                                   .listIterator(getSystems().size());
             it.hasPrevious();) {
            ScoreSystem system = (ScoreSystem) it.previous();
            int         time = system.getLastSoundTime(measureId);

            if (time > 0) {
                return system.getStartTime() + time;
            }
        }

        return 0;
    }

    //---------------//
    // getLastSystem //
    //---------------//
    /**
     * Report the last system in the score
     *
     * @return the last system
     */
    public ScoreSystem getLastSystem ()
    {
        if (children.isEmpty()) {
            return null;
        } else {
            return (ScoreSystem) children.get(children.size() - 1);
        }
    }

    //-----------//
    // getLayout //
    //-----------//
    /**
     * Report the proper layout for the provided score orientation
     * @param orientation the desired score orientation
     * @return the corresponding layout
     */
    public ScoreLayout getLayout (ScoreOrientation orientation)
    {
        return layouts.get(orientation);
    }

    //-------------------//
    // getMaxStaffNumber //
    //-------------------//
    /**
     * Report the maximum number of staves per system
     *
     * @return the maximum number of staves per system
     */
    public int getMaxStaffNumber ()
    {
        int nb = 0;

        for (TreeNode node : children) {
            ScoreSystem system = (ScoreSystem) node;
            int         sn = 0;

            for (TreeNode n : system.getParts()) {
                SystemPart part = (SystemPart) n;
                sn += part.getStaves()
                          .size();
            }

            nb = Math.max(nb, sn);
        }

        return nb;
    }

    //--------------------//
    // getMeanStaffHeight //
    //--------------------//
    /**
     * Report the mean staff height based on score interline. This should be
     * refined per system, if not per staff
     * @return the score-based average value of staff heights
     */
    public int getMeanStaffHeight ()
    {
        return (Score.LINE_NB - 1) * scale.interline();
    }

    //-----------------//
    // setMeasureRange //
    //-----------------//
    /**
     * Remember a range of measure for this score
     *
     * @param measureRange the range of selected measures
     */
    public void setMeasureRange (MeasureRange measureRange)
    {
        this.measureRange = measureRange;
    }

    //-----------------//
    // getMeasureRange //
    //-----------------//
    /**
     * Report the potential range of selected measures
     *
     * @return the selected measure range, perhaps null
     */
    public MeasureRange getMeasureRange ()
    {
        return measureRange;
    }

    //-------------//
    // setMidiFile //
    //-------------//
    /**
     * Remember to which file the MIDI data is to be exported
     * @param midiFile the Midi file
     */
    public void setMidiFile (File midiFile)
    {
        this.midiFile = midiFile;
    }

    //-------------//
    // getMidiFile //
    //-------------//
    /**
     * Report to which file, if any, the MIDI data is to be exported
     * @return the Midi file, or null
     */
    public File getMidiFile ()
    {
        return midiFile;
    }

    //----------------//
    // setOrientation //
    //----------------//
    public void setOrientation (ScoreOrientation orientation)
    {
        ScoreLayout layout = getLayout(orientation);

        for (ScoreView view : views) {
            view.setLayout(layout);
        }
    }

    //-------------//
    // setPartList //
    //-------------//
    /**
     * Assign a part list valid for the whole score
     *
     * @param partList the list of score parts
     */
    public void setPartList (List<ScorePart> partList)
    {
        this.partList = partList;
    }

    //-------------//
    // getPartList //
    //-------------//
    /**
     * Report the global list of parts
     *
     * @return partList the list of score parts
     */
    public List<ScorePart> getPartList ()
    {
        return partList;
    }

    //----------//
    // setRadix //
    //----------//
    /**
     * Set the radix name for this score
     *
     * @param radix (name w/o extension)
     */
    public void setRadix (String radix)
    {
        this.radix = radix;
    }

    //----------//
    // getRadix //
    //----------//
    /**
     * Report the radix of the file that corresponds to the score. It is based
     * on the name of the sheet of this score, with no extension.
     *
     * @return the score file radix
     */
    public String getRadix ()
    {
        if (radix == null) {
            if (getSheet() != null) {
                radix = getSheet()
                            .getRadix();
            }
        }

        return radix;
    }

    //----------//
    // setScale //
    //----------//
    /**
     * Assign proper scale for this score
     * @param scale the general scale for the score
     */
    public void setScale (Scale scale)
    {
        this.scale = scale;
    }

    //----------//
    // getScale //
    //----------//
    /**
     * Report the scale the score
     *
     * @return the score scale (basically: number of pixels for main interline)
     */
    @Override
    public Scale getScale ()
    {
        return scale;
    }

    //-----------------//
    // setScorePdfFile //
    //-----------------//
    /**
     * Remember to which file the score PDF data is to be exported
     * @param scorePdfFile the score PDF file
     */
    public void setScorePdfFile (File scorePdfFile)
    {
        this.scorePdfFile = scorePdfFile;
    }

    //-----------------//
    // getScorePdfFile //
    //-----------------//
    /**
     * Report to which file, if any, the score PDF data is to be written
     * @return the score PDF file, or null
     */
    public File getScorePdfFile ()
    {
        return scorePdfFile;
    }

    //---------------//
    // setScriptFile //
    //---------------//
    /**
     * Remember the file where the script is written
     * @param scriptFile the related script file
     */
    public void setScriptFile (File scriptFile)
    {
        this.scriptFile = scriptFile;
    }

    //---------------//
    // getScriptFile //
    //---------------//
    /**
     * Report the file, if any, where the script should be written
     * @return the related script file or null
     */
    public File getScriptFile ()
    {
        return scriptFile;
    }

    //----------//
    // setSheet //
    //----------//

    /**
     * Register the name of the corresponding sheet entity
     *
     * @param sheet the related sheet entity
     */
    public void setSheet (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //----------//
    // getSheet //
    //----------//
    /**
     * Report the related sheet entity
     *
     * @return the related sheet, or null if none
     */
    public Sheet getSheet ()
    {
        return sheet;
    }

    //-----------------//
    // setSheetPdfFile //
    //-----------------//
    /**
     * Remember to which file the sheet PDF data is to be exported
     * @param sheetPdfFile the sheet PDF file
     */
    public void setSheetPdfFile (File sheetPdfFile)
    {
        this.sheetPdfFile = sheetPdfFile;
    }

    //-----------------//
    // getSheetPdfFile //
    //-----------------//
    /**
     * Report to which file, if any, the shaat PDF data is to be written
     * @return the sheet PDF file, or null
     */
    public File getSheetPdfFile ()
    {
        return sheetPdfFile;
    }

    //--------------//
    // setSkewAngle //
    //--------------//
    /**
     * Assign score global skew angle
     * @param skewAngle the detected skew angle, in radians, clockwise
     */
    public void setSkewAngle (double skewAngle)
    {
        this.skewAngle = skewAngle;
    }

    //--------------//
    // getSkewAngle //
    //--------------//
    /**
     * Report the score skew angle
     *
     * @return skew angle, in radians, clock-wise
     */
    public double getSkewAngle ()
    {
        return skewAngle;
    }

    //---------------//
    // setSlotMargin //
    //---------------//
    /**
     * Assign the slot margin for this score
     * @param fraction the horizontal margin, expressed in interline fraction
     */
    public void setSlotMargin (InterlineFraction fraction)
    {
        this.slotMargin = new InterlineFraction(fraction.doubleValue());
    }

    //---------------//
    // getSlotMargin //
    //---------------//
    /**
     * Report the current horizontal Slot margin
     * If the value is not yet set, it is set to the default value and returned.
     * @return the slotMargin (in interline fraction)
     */
    public InterlineFraction getSlotMargin ()
    {
        if (!hasSlotMargin()) {
            slotMargin = getDefaultSlotMargin();
        }

        return slotMargin;
    }

    //---------------//
    // setSlotPolicy //
    //---------------//
    /**
     * Assign the slot policy for this score
     * @param slotPolicy the policy for determining slots
     */
    public void setSlotPolicy (SlotPolicy slotPolicy)
    {
        this.slotPolicy = slotPolicy;
    }

    //---------------//
    // getSlotPolicy //
    //---------------//
    /**
     * Report the policy used for retrieval of time slots in this score
     * @return the score time slot policy
     */
    public SlotPolicy getSlotPolicy ()
    {
        return slotPolicy;
    }

    //---------------//
    // getSystemById //
    //---------------//
    /**
     * Report the system for which id is provided
     * @param id id of desired system
     * @return the desired system
     */
    public ScoreSystem getSystemById (int id)
    {
        return (ScoreSystem) getSystems()
                                 .get(id - 1);
    }

    //------------//
    // getSystems //
    //------------//
    /**
     * Report the collection of systems in that score
     *
     * @return the systems
     */
    public List<TreeNode> getSystems ()
    {
        return getChildren();
    }

    //----------//
    // setTempo //
    //----------//
    /**
     * Assign a tempo value
     *
     * @param tempo the tempo value to be assigned
     */
    public void setTempo (Integer tempo)
    {
        this.tempo = tempo;
    }

    //----------//
    // getTempo //
    //----------//
    /**
     * Report the assigned tempo, if any
     * If the value is not yet set, it is set to the default value and returned.
     * @return the assigned tempo, or null
     */
    public Integer getTempo ()
    {
        if (!hasTempo()) {
            tempo = getDefaultTempo();
        }

        return tempo;
    }

    //--------------//
    // getViewIndex //
    //--------------//
    /**
     * Report the index of the provided view in the sequence of all views
     * @param view the view to look up
     * @return the sequence index, or -1 if not found
     */
    public int getViewIndex (ScoreView view)
    {
        return views.indexOf(view);
    }

    //-----------//
    // setVolume //
    //-----------//
    /**
     * Assign a volume value
     *
     * @param volume the volume value to be assigned
     */
    public void setVolume (Integer volume)
    {
        this.volume = volume;
    }

    //-----------//
    // getVolume //
    //-----------//
    /**
     * Report the assigned volume, if any
     * If the value is not yet set, it is set to the default value and returned.
     * @return the assigned volume, or null
     */
    public Integer getVolume ()
    {
        if (!hasVolume()) {
            volume = getDefaultVolume();
        }

        return volume;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //---------//
    // addView //
    //---------//
    /**
     * Define the related UI view
     *
     * @param view the dedicated ScoreView
     */
    public void addView (ScoreView view)
    {
        if (logger.isFineEnabled()) {
            logger.fine("addView view=" + view);
        }

        views.remove(view);
        views.add(view);
    }

    //-------//
    // close //
    //-------//
    /**
     * Close this score instance, as well as its view if any
     */
    public void close ()
    {
        // Close related views if any
        for (ScoreView view : views) {
            view.close();
        }

        // Close tree if any
        if (scoreTree != null) {
            scoreTree.close();
        }

        // Complete and Store score bench
        ScoreManager.getInstance()
                    .storeBench(getSheet().getBench(), null, true);

        // Close Midi interface if needed
        if (Main.getGui() != null) {
            ScoreManager.getInstance()
                        .midiClose(this);
        }
    }

    //------//
    // dump //
    //------//
    /**
     * Dump a whole score hierarchy
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

    //-------------------//
    // dumpMeasureCounts //
    //-------------------//
    /**
     * Print out the detailed number of measures inthe score
     * @param title an optional title, or null
     */
    public void dumpMeasureCounts (String title)
    {
        StringBuilder sb = new StringBuilder();

        if (title != null) {
            sb.append("(")
              .append(title)
              .append(") ");
        }

        sb.append("Measure counts:");

        for (TreeNode node : getSystems()) {
            ScoreSystem sys = (ScoreSystem) node;
            SystemPart  part = sys.getLastPart();
            sb.append(
                " System#" + sys.getId() + "=" + part.getMeasures().size());
        }

        logger.info(sb.toString());
    }

    //-------------//
    // hasLanguage //
    //-------------//
    /**
     * Check whether a language has been defined for this score
     * @return true if a language is defined
     */
    public boolean hasLanguage ()
    {
        return language != null;
    }

    //---------------//
    // hasSlotMargin //
    //---------------//
    /**
     * Check whether slotMargin is defined for this score
     * @return true if slotMargin is defined
     */
    public boolean hasSlotMargin ()
    {
        return slotMargin != null;
    }

    //---------------//
    // hasSlotPolicy //
    //---------------//
    public boolean hasSlotPolicy ()
    {
        return slotPolicy != null;
    }

    //----------//
    // hasTempo //
    //----------//
    /**
     * Check whether a tempo has been defined for this score
     * @return true if a tempo is defined
     */
    public boolean hasTempo ()
    {
        return tempo != null;
    }

    //-----------//
    // hasVolume //
    //-----------//
    /**
     * Check whether a volumehas been defined for this score
     * @return true if a volume is defined
     */
    public boolean hasVolume ()
    {
        return volume != null;
    }

    //------------------//
    // pageLocateSystem //
    //------------------//
    /**
     * Retrieve the system 'pagPt' is pointing to.
     *
     * @param pagPt the point, in score units, in the <b>SHEET</b> display
     *
     * @return the nearest system.
     */
    public ScoreSystem pageLocateSystem (PixelPoint pagPt)
    {
        return getSheet()
                   .getSystemOf(pagPt)
                   .getScoreSystem();
    }

    //--------------//
    // resetSystems //
    //--------------//
    /**
     * Reset the systems collection of a score entity
     */
    public void resetSystems ()
    {
        // Reset views on systems
        for (ScoreLayout layout : layouts.values()) {
            layout.reset();
        }

        // Discard systems
        getSystems()
            .clear();

        // Discard cached recent system
        recentSystemRef = null;

        // Discard current score location event (which contains a system id)
        if (Main.getGui() != null) {
            getSheet()
                .getSelectionService()
                .publish(new ScoreLocationEvent(this, null, null, null));
        }
    }

    //------------------//
    // simpleDurationOf //
    //------------------//
    /**
     * Export a duration to its simplest form, based on the greatest duration
     * divisor of the score
     *
     * @param value the raw duration
     * @return the simple duration expression, in the context of proper
     * divisions
     */
    public int simpleDurationOf (int value)
    {
        return value / getDurationDivisor();
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a readable description
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

    //-------------//
    // updateViews //
    //-------------//
    /**
     * Update all views for this score
     */
    public void updateViews ()
    {
        // Refresh the score layouts
        for (ScoreLayout layout : layouts.values()) {
            layout.computeLayout();
        }

        // Update the score views accordingly
        for (ScoreView view : views) {
            view.update();
        }
    }

    //---------------//
    // createLayouts //
    //---------------//
    private void createLayouts ()
    {
        for (ScoreOrientation orientation : ScoreOrientation.values()) {
            layouts.put(orientation, new ScoreLayout(this, orientation));
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

        // Default Tempo
        Constant.Integer             defaultTempo = new Constant.Integer(
            "QuartersPerMn",
            100,
            "Default tempo, stated in number of quarters per minute");

        // Default Velocity
        Constant.Integer             defaultVolume = new Constant.Integer(
            "Volume",
            25,
            "Default Volume in 0..127 range");

        /** Default slot policy */
        SlotPolicy.Constant defaultSlotPolicy = new SlotPolicy.Constant(
            SlotPolicy.HEAD_BASED,
            "Default policy for determining time slots (HEAD_BASED or SLOT_BASED)");

        /**
         * Default horizontal margin between a slot and a glyph candidate
         */
        Scale.Fraction defaultSlotMargin = new Scale.Fraction(
            0.5,
            "Default horizontal margin between a slot and a glyph candidate");

        /** Minimum spacing between slots before alerting user */
        private final Scale.Fraction minSlotSpacing = new Scale.Fraction(
            1.1d,
            "Minimum spacing between slots before alerting user");
    }
}

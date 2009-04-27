//----------------------------------------------------------------------------//
//                                                                            //
//                                 S c o r e                                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.common.PagePoint;
import omr.score.common.UnitDimension;
import omr.score.entity.ScoreNode;
import omr.score.entity.ScorePart;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;
import omr.score.ui.ScoreConstants;
import omr.score.ui.ScoreOrientation;
import omr.score.ui.ScoreTree;
import omr.score.ui.ScoreView;
import omr.score.visitor.ScoreReductor;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;
import omr.sheet.Sheet;

import omr.util.Dumper;
import omr.util.TreeNode;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Score
    extends ScoreNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Score.class);

    //~ Instance fields --------------------------------------------------------

    /** File of the related sheet image */
    private File imageFile;

    /** Link with image */
    private Sheet sheet;

    /** The related file radix (name w/o extension) */
    private String radix;

    /** Sheet dimension in units */
    private UnitDimension dimension;

    /** Sheet skew angle in radians */
    private int skewAngle;

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

    /** The specified velocity, if any */
    private Integer velocity;

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

    //~ Constructors -----------------------------------------------------------

    //-------//
    // Score //
    //-------//
    /**
     * Creates a blank score, to be fed with informations from sheet analysis or
     * from an XML binder.
     */
    public Score ()
    {
        super(null); // No container

        if (logger.isFineEnabled()) {
            logger.fine("Construction of an empty score");
        }
    }

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
        this();

        setImagePath(imagePath);

        if (logger.isFineEnabled()) {
            Dumper.dump(this, "Constructed");
        }
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

    //-----------------//
    // getDefaultTempo //
    //-----------------//
    /**
     * Report default value for Midi tempo
     *
     * @return the default tempo value
     */
    public int getDefaultTempo ()
    {
        return constants.defaultTempo.getValue();
    }

    //--------------------//
    // getDefaultVelocity //
    //--------------------//
    /**
     * Report default value for Midi velocity (volume)
     *
     * @return the default velocity value
     */
    public int getDefaultVelocity ()
    {
        return constants.defaultVelocity.getValue();
    }

    //--------------//
    // setDimension //
    //--------------//
    /**
     * Assign score dimension
     * @param dimension the score dimension, expressed in units
     */
    public void setDimension (UnitDimension dimension)
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
    public UnitDimension getDimension ()
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
    // getFirstView //
    //--------------//
    /**
     * Report the first view on this score
     * (actually the only one for the time being)
     * @return the (first) view on the score, if any
     */
    public ScoreView getFirstView ()
    {
        if (!views.isEmpty()) {
            return views.get(0);
        } else {
            return null;
        }
    }

    //---------------------//
    // setHighestSystemTop //
    //---------------------//
    public void setHighestSystemTop (int highestSystemTop)
    {
        this.highestSystemTop = highestSystemTop;
    }

    //---------------------//
    // getHighestSystemTop //
    //---------------------//
    public int getHighestSystemTop ()
    {
        return highestSystemTop;
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
     * @return the dominant language
     */
    public String getLanguage ()
    {
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

    //----------------//
    // setOrientation //
    //----------------//
    /**
     * @param orientation the orientation to set
     */
    public void setOrientation (ScoreOrientation orientation)
    {
        this.orientation = orientation;
        logger.info("New score system layout: " + orientation);

        for (ScoreView view : views) {
            view.setOrientation(orientation);
        }
    }

    //----------------//
    // getOrientation //
    //----------------//
    /**
     * @return the orientation
     */
    public ScoreOrientation getOrientation ()
    {
        return orientation;
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

    //--------------//
    // setSkewAngle //
    //--------------//
    /**
     * Assign score global skew angle
     * @param skewAngle the detected skew angle, in radians, clockwise
     */
    public void setSkewAngle (int skewAngle)
    {
        this.skewAngle = skewAngle;
    }

    //--------------//
    // getSkewAngle //
    //--------------//
    /**
     * Report the score skew angle
     *
     * @return skew angle, in 1/1024 of radians, clock-wise
     */
    public int getSkewAngle ()
    {
        return skewAngle;
    }

    //--------------------//
    // getSkewAngleDouble //
    //--------------------//
    /**
     * Report the score skew angle
     *
     * @return skew angle, in radians, clock-wise
     */
    public double getSkewAngleDouble ()
    {
        return (double) skewAngle / (double) ScoreConstants.BASE;
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
     *
     * @return the assigned tempo, or null
     */
    public Integer getTempo ()
    {
        return tempo;
    }

    //-------------//
    // setVelocity //
    //-------------//
    /**
     * Assign a velocity value
     *
     * @param velocity the velocity value to be assigned
     */
    public void setVelocity (Integer velocity)
    {
        this.velocity = velocity;
    }

    //-------------//
    // getVelocity //
    //-------------//
    /**
     * Report the assigned velocity, if any
     *
     * @return the assigned velocity, or null
     */
    public Integer getVelocity ()
    {
        return velocity;
    }

    //--------------//
    // getViewIndex //
    //--------------//
    /**
     * Report the index of the provided view in the sequence of all views
     * @param view the view ro look up
     * @return the sequence index, or -1 if not found
     */
    public int getViewIndex (ScoreView view)
    {
        return views.indexOf(view);
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

        // Close Midi interface if needed
        ScoreManager.getInstance()
                    .midiClose(this);
    }

    //--------------------//
    // createPartListFrom //
    //--------------------//
    /**
     * Create the list of score parts, based on the provided reference system
     *
     * @param refSystem the system taken as reference
     */
    public void createPartListFrom (ScoreSystem refSystem)
    {
        // Build a ScorePart list based on the parts of the ref system
        int index = 0;
        partList.clear();

        for (TreeNode node : refSystem.getParts()) {
            SystemPart sp = (SystemPart) node;
            Staff      firstStaff = sp.getFirstStaff();
            ScorePart  scorePart = new ScorePart(
                sp,
                this,
                ++index,
                firstStaff.getPageTopLeft().y - refSystem.getTopLeft().y);
            scorePart.setName("Part_" + index);
            partList.add(scorePart);
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

    //--------//
    // export //
    //--------//
    /**
     * Marshall the score to its MusicXML file
     */
    public void export ()
    {
        ScoreManager.getInstance()
                    .export(this);
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
    public ScoreSystem pageLocateSystem (PagePoint pagPt)
    {
        ScoreSystem recentSystem = (recentSystemRef == null) ? null
                                   : recentSystemRef.get();

        if (recentSystem != null) {
            // Check first with most recent system (loosely)
            switch (recentSystem.locate(pagPt)) {
            case -1 :

                // Check w/ previous system
                ScoreSystem prevSystem = (ScoreSystem) recentSystem.getPreviousSibling();

                if (prevSystem == null) { // Very first system

                    return recentSystem;
                } else if (prevSystem.locate(pagPt) > 0) {
                    return recentSystem;
                }

                break;

            case 0 :
                return recentSystem;

            case +1 :

                // Check w/ next system
                ScoreSystem nextSystem = (ScoreSystem) recentSystem.getNextSibling();

                if (nextSystem == null) { // Very last system

                    return recentSystem;
                } else if (nextSystem.locate(pagPt) < 0) {
                    return recentSystem;
                }

                break;
            }
        }

        if (logger.isFineEnabled()) {
            logger.fine("yLocateSystem. Not within recent system");
        }

        // Recent system is not OK, Browse though all the score systems
        ScoreSystem system = null;

        for (TreeNode node : children) {
            system = (ScoreSystem) node;

            // How do we locate the point wrt the system  ?
            switch (system.locate(pagPt)) {
            case -1 : // Point is above this system, give up.
            case 0 : // Point is within system.
                recentSystemRef = new WeakReference<ScoreSystem>(system);

                return system;

            case +1 : // Point is below this system, go on.
                break;
            }
        }

        // Return the last system in the score
        recentSystemRef = new WeakReference<ScoreSystem>(system);

        return system;
    }

    //-------//
    // reset //
    //-------//
    /**
     * Reset a score entity to its basic structure
     */
    public void reset ()
    {
        // Discard systems
        getSystems()
            .clear();
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
        for (ScoreView view : views) {
            view.update();
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
        Constant.Integer defaultTempo = new Constant.Integer(
            "QuartersPerMn",
            60,
            "Default tempo, stated in number of quarters per minute");

        // Default Velocity
        Constant.Integer defaultVelocity = new Constant.Integer(
            "Volume",
            100,
            "Default Velocity in 0..127 range");
    }
}

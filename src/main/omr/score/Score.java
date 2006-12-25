//----------------------------------------------------------------------------//
//                                                                            //
//                                 S c o r e                                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SheetManager;

import omr.util.Dumper;
import omr.util.Logger;
import omr.util.TreeNode;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
    private UnitDimension dimension = new UnitDimension(0, 0);

    /** Sheet skew angle in radians */
    private int skewAngle;

    /** Sheet global scale */
    private Scale scale;

    /**
     * ScorePart list for the whole score
     */
    private List<ScorePart> partList;

    /** The most recent system pointed at */
    private transient System recentSystem = null;

    /** The view on this score if any */
    private transient ScoreView view;

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
     * @param dimension the score dimension, expressed in units
     * @param skewAngle the detected skew angle, in radians, clockwise
     * @param scale the global scale
     * @param imagePath full name of the original sheet file
     */
    public Score (UnitDimension dimension,
                  int           skewAngle,
                  Scale         scale,
                  String        imagePath)
    {
        this();
        this.dimension = dimension;
        this.skewAngle = skewAngle;
        this.scale = scale;

        setImagePath(imagePath);

        if (logger.isFineEnabled()) {
            Dumper.dump(this, "Constructed");
        }
    }

    //~ Methods ----------------------------------------------------------------

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

    //----------------//
    // getFirstSystem //
    //----------------//
    /**
     * Report the first system in the score
     *
     * @return the first system
     */
    public System getFirstSystem ()
    {
        return (System) children.get(0);
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

    //---------------//
    // getLastSystem //
    //---------------//
    /**
     * Report the last system in the score
     *
     * @return the last system
     */
    public System getLastSystem ()
    {
        return (System) children.get(children.size() - 1);
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
            System system = (System) node;
            int    sn = 0;

            for (TreeNode n : system.getParts()) {
                SystemPart part = (SystemPart) n;
                sn += part.getStaves()
                          .size();
            }

            nb = Math.max(nb, sn);
        }

        return nb;
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
    // getScale //
    //----------//
    /**
     * Report the scale the score
     *
     * @return the score scale (basically: number of pixels for main interline)
     */
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

        // Make sure the containing score has been inserted in the score
        // instances
        if (sheet != null) {
            ScoreManager.getInstance()
                        .checkInserted(this);
        }
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

    //---------//
    // setView //
    //---------//
    /**
     * Define the related UI view
     *
     * @param view the dedicated ScoreView
     */
    public void setView (ScoreView view)
    {
        if (logger.isFineEnabled()) {
            logger.fine("setView view=" + view);
        }

        this.view = view;
    }

    //---------//
    // getView //
    //---------//
    /**
     * Report the UI view, if any
     *
     * @return the view, or null otherwise
     */
    public ScoreView getView ()
    {
        return view;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // addChild //
    //----------//
    /**
     * Overriding version, so that we can register the score in the list of
     * score instances.
     *
     * @param node a score node
     */
    @Override
    public void addChild (TreeNode node)
    {
        super.addChild(node);

        // Side effect on score
        ScoreManager.getInstance()
                    .checkInserted(this);
    }

    //-------//
    // close //
    //-------//
    /**
     * Close this score instance, as well as its view if any
     */
    public void close ()
    {
        ScoreManager.getInstance()
                    .close(this);

        // Close related view if any
        if (view != null) {
            view.close();
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
        java.lang.System.out.println(
            "-----------------------------------------------------------------------");

        if (dumpNode()) {
            dumpChildren(1);
        }

        java.lang.System.out.println(
            "-----------------------------------------------------------------------");
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

    //---------------//
    // linkWithSheet //
    //---------------//
    /**
     * Try to link this score with one of the sheets currently handled
     */
    public void linkWithSheet ()
    {
        if (getSheet() != null) {
            return;
        }

        logger.info("Linking " + this);

        for (Sheet sheet : SheetManager.getInstance()
                                       .getSheets()) {
            if (sheet.getPath()
                     .equals(getImagePath())) {
                if (sheet != getSheet()) {
                    this.setSheet(sheet);
                    sheet.setScore(this);

                    if (logger.isFineEnabled()) {
                        logger.fine(this + " linked to " + sheet);
                    }

                    return;
                }
            }
        }

        // No related sheet found in sheet manager. If we've deserialized
        // this score with some sheet info, let's use it
        if (getSheet() != null) {
            SheetManager.getInstance()
                        .insertInstance(getSheet());

            if (logger.isFineEnabled()) {
                logger.fine(this + " linked to newly inserted " + getSheet());
            }

            // Make the sheet assembly visible
            getSheet()
                .checkTransientSteps();
            getSheet()
                .displayAssembly();
        } else {
            if (logger.isFineEnabled()) {
                logger.fine(this + " not linked");
            }

            // Create a void related sheet
            try {
                new Sheet(this);
            } catch (Exception ex) {
                logger.warning(ex.toString());
            }
        }
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
    public System pageLocateSystem (PagePoint pagPt)
    {
        if (recentSystem != null) {
            // Check first with most recent system (loosely)
            switch (recentSystem.locate(pagPt)) {
            case -1 :

                // Check w/ previous system
                System prevSystem = (System) recentSystem.getPreviousSibling();

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
                System nextSystem = (System) recentSystem.getNextSibling();

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
        System system = null;

        for (TreeNode node : children) {
            system = (System) node;

            // How do we locate the point wrt the system  ?
            switch (system.locate(pagPt)) {
            case -1 : // Point is above this system, give up.
            case 0 : // Point is within system.
                return recentSystem = system;

            case +1 : // Point is below this system, go on.
                break;
            }
        }

        // Return the last system in the score
        return recentSystem = system;
    }

    //-------------------//
    // scoreLocateSystem //
    //-------------------//
    /**
     * Retrieve the system 'scrPt' is pointing to, knowing that Systems in the
     * <b>SCORE</b> display, are arranged horizontally one after the other,
     * while they were arranged vertically in the related Sheet.
     *
     * @param scrPt the point in the SCORE horizontal display
     *
     * @return the nearest system
     */
    public System scoreLocateSystem (ScorePoint scrPt)
    {
        if (recentSystem != null) {
            // Check first with most recent system (loosely)
            switch (recentSystem.locate(scrPt)) {
            case -1 :

                // Check w/ previous system
                System prevSystem = (System) recentSystem.getPreviousSibling();

                if (prevSystem == null) { // Very first system

                    return recentSystem;
                } else {
                    if (prevSystem.locate(scrPt) > 0) {
                        return recentSystem;
                    }
                }

                break;

            case 0 :
                return recentSystem;

            case +1 :

                // Check w/ next system
                System nextSystem = (System) recentSystem.getNextSibling();

                if (nextSystem == null) { // Very last system

                    return recentSystem;
                } else {
                    if (nextSystem.locate(scrPt) < 0) {
                        return recentSystem;
                    }
                }

                break;
            }
        }

        // Recent system is not OK, Browse though all the score systems
        System system = null;

        for (TreeNode node : children) {
            system = (System) node;

            // How do we locate the point wrt the system  ?
            switch (system.locate(scrPt)) {
            case -1 : // Point is on left of system, give up.
            case 0 : // Point is within system.
                return recentSystem = system;

            case +1 : // Point is on right of system, go on.
                break;
            }
        }

        // Return the last system in the score
        return recentSystem = system;
    }

    //-----------//
    // serialize //
    //-----------//
    /**
     * Serialize the score to its binary file
     *
     * @throws java.lang.Exception if anything goes wrong
     */
    public void serialize ()
        throws Exception
    {
        ScoreManager.getInstance()
                    .serialize(this);
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

    //-----------//
    // viewScore //
    //-----------//
    /**
     * Launch a dedicated frame, where all score elements can be browsed in the
     * tree hierarchy
     */
    public void viewScore ()
    {
        // Launch the ScoreTree application on the score
        ScoreTree.makeFrame(getRadix(), this);
    }
}

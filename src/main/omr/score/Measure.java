//----------------------------------------------------------------------------//
//                                                                            //
//                               M e a s u r e                                //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.lag.Lag;

import omr.score.visitor.Visitor;

import omr.util.Dumper;
import omr.util.Logger;
import omr.util.TreeNode;

import java.awt.*;
import java.util.Iterator;
import java.util.List;

/**
 * Class <code>Measure</code> handles a measure of a staff. As a MusicNode, the
 * children of a Measure are : Barline, TimeSignature, list of Clef(s), list of
 * KeySignature(s), list of Chord(s).
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Measure
    extends StaffNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Measure.class);

    //~ Instance fields --------------------------------------------------------

    /** Child: Ending bar line */
    private Barline barline;

    /** Child: Potential time signature */
    private TimeSignature timeSignature;

    /** Child: possibly several clefs */
    private ClefList clefs;

    /** Child: possibly several KeySignature's */
    private KeySignatureList keysigs;

    /** Child: possibly several Chord's */
    //     private ChordList chords;

    /** Left abscissa (in units) of this measure */
    private Integer leftX;

    /** For measure with no physical ending bar line */
    private boolean lineinvented;

    /** Measure Id */
    private int id = 0;

    //~ Constructors -----------------------------------------------------------

    //---------//
    // Measure //
    //---------//
    /**
     * Default constructor (needed by XML Binder)
     */
    public Measure ()
    {
        super(null, null);
        cleanupNode();
    }

    //---------//
    // Measure //
    //---------//
    /**
     * Create a measure with the specified parameters
     *
     * @param staff        the containing staff
     * @param lineinvented flag an artificial ending bar line if none existed
     */
    public Measure (Staff   staff,
                    boolean lineinvented)
    {
        super(staff, staff);

        this.lineinvented = lineinvented;

        cleanupNode();

        // Allocate children lists
        keysigs = new KeySignatureList(this, staff);

        if (logger.isFineEnabled()) {
            Dumper.dump(this, "Constructed");
        }
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // setBarline //
    //------------//
    /**
     * Set the ending bar line
     *
     * @param barline the ending bar line
     */
    public void setBarline (Barline barline)
    {
        this.barline = barline;
    }

    //------------//
    // getBarline //
    //------------//
    /**
     * Report the ending bar line
     *
     * @return the ending bar line
     */
    public Barline getBarline ()
    {
        return barline;
    }

    //----------//
    // getClefs //
    //----------//
    /**
     * Report the collection of clefs
     *
     * @return the list of clefs
     */
    public List<TreeNode> getClefs ()
    {
        return clefs.getChildren();
    }

    //------------------//
    // getKeySignatures //
    //------------------//
    /**
     * Report the collection of KeySignature's
     *
     * @return the list of KeySignature's
     */
    public List<TreeNode> getKeySignatures ()
    {
        return keysigs.getChildren();
    }

    //-------//
    // setId //
    //-------//
    /**
     * Assign the proper id to this measure
     *
     * @param id the proper measure id
     */
    public void setId (int id)
    {
        this.id = id;
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the measure id
     *
     * @return the measure id
     */
    public int getId ()
    {
        return id;
    }

    //----------//
    // getLeftX //
    //----------//
    /**
     * Report the abscissa of the start of the measure, relative to staff (so 0
     * for first measure in the staff)
     *
     * @return staff-based abscissa of left side of the measure
     */
    public Integer getLeftX ()
    {
        if (leftX == null) {
            // Start of the measure
            Measure prevMeasure = (Measure) getPreviousSibling();

            if (prevMeasure == null) { // Very first measure in the staff
                leftX = 0;
            } else {
                leftX = prevMeasure.getBarline()
                                   .getCenter().x;
            }
        }

        return leftX;
    }

    //------------------//
    // setTimeSignature //
    //------------------//
    /**
     * Assign a time signature to this measure
     *
     * @param timeSignature the time signature
     */
    public void setTimeSignature (TimeSignature timeSignature)
    {
        this.timeSignature = timeSignature;
    }

    //------------------//
    // getTimeSignature //
    //------------------//
    /**
     * Report the potential time signature of this measure
     *
     * @return the related time signature, or null if none
     */
    public TimeSignature getTimeSignature ()
    {
        return timeSignature;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the width, in units, of the measure
     *
     * @return the measure width
     */
    public int getWidth ()
    {
        return getBarline()
                   .getCenter().x - getLeftX();
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (Visitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // addChild //
    //----------//
    /**
     * Override normal behavior, so that a given child is stored in its proper
     * type collection (clef to clef list, etc...)
     *
     * @param node the child to insert in the staff
     */
    @Override
    public void addChild (TreeNode node)
    {
        if (node instanceof Clef) {
            clefs.addChild(node);
            node.setContainer(clefs);

        } else if (node instanceof KeySignature) {
            keysigs.addChild(node);
            node.setContainer(keysigs);
            
            //      } else if (node instanceof Lyricline) {
            //          lyriclines.addChild (node);
            //          node.setContainer (lyriclines);
            //      } else if (node instanceof Text) {
            //          texts.addChild (node);
            //          node.setContainer (texts);
            //      } else if (node instanceof Dynamic) {
            //          dynamics.addChild (node);
            //          node.setContainer (dynamics);
        } else if (node instanceof TreeNode) {
            // Meant for the 4 dummy lists
            children.add(node);
            node.setContainer(this);
        } else {
            // Programming error
            Dumper.dump(node);
            logger.severe("Staff node not known");
        }
    }

    //-------------//
    // cleanupNode //
    //-------------//
    /**
     * Get rid of all nodes of this measure, except the barlines
     */
    public void cleanupNode ()
    {
        // Remove all direct children except barlines
        for (Iterator it = children.iterator(); it.hasNext();) {
            MusicNode node = (MusicNode) it.next();

            if (!(node instanceof Barline)) {
                it.remove();
            }
        }

        // Invalidate data
        timeSignature = null;

        // (Re)Allocate specific children lists
        clefs = new ClefList(this, staff);
        keysigs = new KeySignatureList(this, staff);

        //         chords = new ChordList(this, staff);
    }

    //--------------//
    // leftMarginOf //
    //--------------//
    /**
     * Report the horizontal margin (in units) between left side of the measure
     * and the given point
     *
     * @param pt the given StaffPoint
     * @return the left margin
     */
    public int leftMarginOf (StaffPoint pt)
    {
        return pt.x - getLeftX();
    }

    //-------//
    // reset //
    //-------//
    /**
     * Reset the coordinates of the measure, they will be lazily recomputed when
     * needed
     */
    public void reset ()
    {
        leftX = null;
        barline.reset();
    }

    //---------------//
    // rightMarginOf //
    //---------------//
    /**
     * Report the horizontal margin (in units) between the given point and the
     * right side of the measure
     *
     * @param pt the given StaffPoint
     * @return the right margin
     */
    public int rightMarginOf (StaffPoint pt)
    {
        return barline.getLeftX() - pt.x;
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a readable description
     *
     * @return a string based on main members
     */
    @Override
    public String toString ()
    {
        return "{Measure id=" + id + "}";
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------//
    // ClefList //
    //----------//
    private static class ClefList
        extends StaffNode
    {
        ClefList (StaffNode container,
                  Staff     staff)
        {
            super(container, staff);
        }
    }

    //------------------//
    // KeySignatureList //
    //------------------//
    private static class KeySignatureList
        extends StaffNode
    {
        KeySignatureList (StaffNode container,
                          Staff     staff)
        {
            super(container, staff);
        }
    }
}

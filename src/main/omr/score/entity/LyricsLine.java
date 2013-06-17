//----------------------------------------------------------------------------//
//                                                                            //
//                            L y r i c s L i n e                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.constant.ConstantSet;

import omr.math.Population;


import omr.sheet.Scale;

import omr.util.TreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code LyricsLine} gathers one line of lyrics within a system
 * part.
 * A lyrics line is composed of instances of LyricsItem, which can be Syllables,
 * Hyphens, Extensions or Elisions
 *
 * @author Hervé Bitteur
 */
public class LyricsLine
        extends PartNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(LyricsLine.class);

    /** For comparing (TreeNode) LyricsLine instances on their ordinate */
    public static final Comparator<TreeNode> yComparator = new Comparator<TreeNode>()
    {
        @Override
        public int compare (TreeNode tn1,
                            TreeNode tn2)
        {
            LyricsLine l1 = (LyricsLine) tn1;
            LyricsLine l2 = (LyricsLine) tn2;

            return Integer.signum(l1.getY() - l2.getY());
        }
    };

    //~ Instance fields --------------------------------------------------------
    /** The line number */
    private int id;

    /** The mean ordinate (in units) within the system */
    private Integer y;

    /** The x-ordered collection of lyrics items */
    private final SortedSet<LyricsItem> items = new TreeSet<>();

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new LyricsLine object.
     *
     * @param systemPart the containing system part
     */
    public LyricsLine (SystemPart systemPart)
    {
        super(systemPart);
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // populate //
    //----------//
    /**
     * Populate a Lyrics line with this lyrics item
     *
     * @param item the lyrics item to host in a lyrics line
     * @param part the containing system part
     */
    public static void populate (LyricsItem item,
                                 SystemPart part)
    {
        logger.debug("LyricsLine. populate  with {}", item);

        // First look for a suitable lyrics line
        for (TreeNode node : part.getLyrics()) {
            LyricsLine line = (LyricsLine) node;

            if (line.isAlignedWith(item.getReferencePoint())) {
                line.addItem(item);
                logger.debug("Added {} into {}", item, line);
                return;
            }
        }

        // No compatible line, create a brand new one
        LyricsLine line = new LyricsLine(part);
        line.addItem(item);
        logger.debug("Created new {}", line);
    }

    //------------------//
    // getFollowingLine //
    //------------------//
    /**
     * Retrieve the corresponding lyrics line in the next part, if any
     *
     * @return the next lyrics line, or null
     */
    public LyricsLine getFollowingLine ()
    {
        LyricsLine nextLine = null;

        // Check existence of similar line in following system part
        SystemPart nextPart = getPart().getFollowing();

        if (nextPart != null) {
            // Retrieve the same lyrics line in the next (system) part
            if (nextPart.getLyrics().size() >= id) {
                nextLine = (LyricsLine) nextPart.getLyrics().get(id - 1);
            }
        }

        return nextLine;
    }

    //-------//
    // getId //
    //-------//
    public int getId ()
    {
        return id;
    }

    //------------------//
    // getPrecedingLine //
    //------------------//
    /**
     * Retrieve the corresponding lyrics line in the preceding part, if any
     *
     * @return the preceding lyrics line, or null
     */
    public LyricsLine getPrecedingLine ()
    {
        // Check existence of similar line in preceding system part
        SystemPart part = getPart();

        if ((part != null) && (part.getLyrics().size() >= id)) {
            return (LyricsLine) part.getLyrics().get(id - 1);
        }

        return null;
    }

    //------//
    // getY //
    //------//
    /**
     * Report the ordinate of this line
     *
     * @return the mean line ordinate, wrt the containing system
     */
    public int getY ()
    {
        if (y == null) {
            Population population = new Population();

            for (LyricsItem item : items) {
                population.includeValue(item.getReferencePoint().y);
            }

            if (population.getCardinality() > 0) {
                y = (int) Math.rint(population.getMeanValue());
            }
        }

        return y;
    }

    //----------------------//
    // refineLyricSyllables //
    //----------------------//
    public void refineLyricSyllables ()
    {
        // Last item of preceding line is any
        LyricsItem precedingItem = null;
        LyricsLine precedingLine = getPrecedingLine();

        if (precedingLine != null) {
            precedingItem = precedingLine.items.last();
        }

        // Now browse sequentially all our line items
        LyricsItem[] itemArray = items.toArray(new LyricsItem[items.size()]);

        for (int i = 0; i < itemArray.length; i++) {
            LyricsItem item = itemArray[i];

            // Following item (perhaps to be found in following line if needed)
            LyricsItem followingItem = null;

            if (i < (itemArray.length - 1)) {
                followingItem = itemArray[i + 1];
            } else {
                LyricsLine followingLine = getFollowingLine();

                if (followingLine != null) {
                    followingItem = followingLine.items.first();
                }
            }

            // We process only syllable items
            if (item.getItemKind() == LyricsItem.ItemKind.Syllable) {
                item.defineSyllabicType(precedingItem, followingItem);
            }

            precedingItem = item;
        }
    }

    //-------//
    // setId //
    //-------//
    public void setId (int id)
    {
        this.id = id;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("{LyricLine #").append(getId()).append(" y:").append(y).append(
                " items:").append(items).append("}");

        return sb.toString();
    }

    //--------------//
    // mapSyllables //
    //--------------//
    void mapSyllables ()
    {
        for (LyricsItem item : items) {
            item.mapToNote();
        }
    }

    //---------//
    // addItem //
    //---------//
    private void addItem (LyricsItem item)
    {
        items.add(item);
        item.setLyricLine(this);

        // Force recomputation of line mean ordinate
        y = null;
        getY();
    }

    //---------------//
    // isAlignedWith //
    //---------------//
    /**
     * Check whether a system point is roughly aligned with this line instance
     *
     * @param sysPt the system point to check
     * @return true if aligned
     */
    private boolean isAlignedWith (Point sysPt)
    {
        return Math.abs(sysPt.y - getY()) <= getScale().toPixels(
                constants.maxItemDy);
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction maxItemDy = new Scale.Fraction(
                2,
                "Maximum vertical distance between a lyrics line and a lyrics item");

    }
}

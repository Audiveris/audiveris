//----------------------------------------------------------------------------//
//                                                                            //
//                             L y r i c L i n e                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr.score.entity;

import omr.constant.ConstantSet;

import omr.math.Population;

import omr.score.common.SystemPoint;

import omr.sheet.Scale;

import omr.util.Logger;
import omr.util.TreeNode;

import java.util.*;

/**
 * Class <code>LyricLine</code> gathers one line of lyrics within a system part.
 * A lyrics line is composed of instances of LyricItem, which can be Syllables,
 * Hyphens, Extensions or Elisions
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class LyricLine
    extends PartNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LyricLine.class);

    //~ Instance fields --------------------------------------------------------

    /** The line number */
    private int id;

    /** The mean ordinate (in units) within the system */
    private Integer y;

    /** The x-ordered collection of lyric items */
    @Children
    private final SortedSet<LyricItem> items = new TreeSet<LyricItem>();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LyricLine object.
     *
     * @param systemPart the containing system part
     */
    public LyricLine (SystemPart systemPart)
    {
        super(systemPart);
    }

    //~ Methods ----------------------------------------------------------------

    //-------//
    // setId //
    //-------//
    public void setId (int id)
    {
        this.id = id;
    }

    //-------//
    // getId //
    //-------//
    public int getId ()
    {
        return id;
    }

    //-------------//
    // getNextLine //
    //-------------//
    /**
     * Retrieve the corresponding lyric line in the next part, if any
     *
     * @return the next lyric line, or null
     */
    public LyricLine getNextLine ()
    {
        LyricLine nextLine = null;

        // Check existence of similar line in previous system
        System nextSystem = (System) getSystem()
                                         .getNextSibling();

        if (nextSystem != null) {
            // We assume all systems have the same number of parts
            SystemPart nextPart = (SystemPart) nextSystem.getParts()
                                                         .get(
                getPart().getId() - 1);

            // Retrieve the same lyric line in the next (system) part
            if (nextPart.getLyrics()
                        .size() >= id) {
                nextLine = (LyricLine) nextPart.getLyrics()
                                               .get(id - 1);
            }
        }

        return nextLine;
    }

    //-----------------//
    // getPreviousLine //
    //-----------------//
    /**
     * Retrieve the corresponding lyric line in the previous part, if any
     *
     * @return the previous lyric line, or null
     */
    public LyricLine getPreviousLine ()
    {
        LyricLine prevLine = null;

        // Check existence of similar line in previous system
        System prevSystem = (System) getSystem()
                                         .getPreviousSibling();

        if (prevSystem != null) {
            // We assume all systems have the same number of parts
            SystemPart prevPart = (SystemPart) prevSystem.getParts()
                                                         .get(
                getPart().getId() - 1);

            // Retrieve the same lyric line in the previous (system) part
            if (prevPart.getLyrics()
                        .size() >= id) {
                prevLine = (LyricLine) prevPart.getLyrics()
                                               .get(id - 1);
            }
        }

        return prevLine;
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

            for (LyricItem item : items) {
                population.includeValue(item.getLocation().y);
            }

            if (population.getCardinality() > 0) {
                y = (int) Math.rint(population.getMeanValue());
            }
        }

        return y;
    }

    //----------//
    // populate //
    //----------//
    /**
     * Populate a Lyric line with this lyric item
     *
     * @param item the lyric item to host in a lyric line
     * @param part the containing system part
     */
    public static void populate (LyricItem  item,
                                 SystemPart part)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Populating LyricLine with " + item);
        }

        // First look for a suitable lyric line
        for (TreeNode node : part.getLyrics()) {
            LyricLine line = (LyricLine) node;

            if (line.isAlignedWith(item.getLocation())) {
                line.addItem(item);

                if (logger.isFineEnabled()) {
                    logger.fine("Added " + item + " into " + line);
                }

                return;
            }
        }

        // No compatible line, create a brand new one
        LyricLine line = new LyricLine(part);
        line.addItem(item);

        if (logger.isFineEnabled()) {
            logger.fine("Created new " + line);
        }
    }

    //----------------------//
    // refineLyricSyllables //
    //----------------------//
    public void refineLyricSyllables ()
    {
        // Last item of previous line is any
        LyricItem prevItem = null;
        LyricLine prevLine = getPreviousLine();

        if (prevLine != null) {
            prevItem = prevLine.items.last();
        }

        // Now browse sequentially all our line items
        LyricItem[] itemArray = items.toArray(new LyricItem[items.size()]);

        for (int i = 0; i < itemArray.length; i++) {
            LyricItem item = itemArray[i];

            // Next item (perhaps to be found in next line if needed)
            LyricItem nextItem = null;

            if (i < (itemArray.length - 1)) {
                nextItem = itemArray[i + 1];
            } else {
                LyricLine nextLine = getNextLine();

                if (nextLine != null) {
                    nextItem = nextLine.items.first();
                }
            }

            // We process only syllable items
            if (item.getItemKind() == LyricItem.ItemKind.Syllable) {
                item.defineSyllabicType(prevItem, nextItem);
            }

            prevItem = item;
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("{LyricLine #")
          .append(getId())
          .append(" y:")
          .append(y)
          .append(" items:")
          .append(items)
          .append("}");

        return sb.toString();
    }

    //-------------------------//
    // connectSyllablesToNotes //
    //-------------------------//
    void connectSyllablesToNotes ()
    {
        for (LyricItem item : items) {
            item.connectToNote();
        }
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
    private boolean isAlignedWith (SystemPoint sysPt)
    {
        return Math.abs(sysPt.y - getY()) <= getScale()
                                                 .toUnits(constants.maxItemDy);
    }

    //---------//
    // addItem //
    //---------//
    private void addItem (LyricItem item)
    {
        items.add(item);
        item.setLine(this);

        // Force recomputation of line mean ordinate
        y = null;
        getY();
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
            "Maximum vertical distance between a lyric line and a lyric item");
    }
}

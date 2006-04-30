//-----------------------------------------------------------------------//
//                                                                       //
//                         S y s t e m S p l i t                         //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.sheet;

import omr.glyph.GlyphSection;
import omr.stick.Stick;
import omr.util.Dumper;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Class <code>SystemSplit</code> is a set of methods dedicated to split
 * entities (sections, sticks, ...) according to the system areas
 * previously found in a sheet
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SystemSplit
{
    //~ Constructors ------------------------------------------------------

    // No constructor
    private SystemSplit ()
    {
    }

    //~ Methods -----------------------------------------------------------

    //------//
    // dump //
    //------//
    /**
     * Utility method, to dump all sheet systems
     *
     * @param sheet the containing sheet
     */
    public static void dump (Sheet sheet)
    {
        System.out.println("--- SystemInfos ---");

        int i = 0;
        for (SystemInfo info : sheet.getSystems()) {
            Dumper.dump(info, "#" + i++);
        }

        System.out.println("--- SystemInfos end ---");
    }

    //-----------//
    // splitBars //
    //-----------//
    /**
     * Split the various BarInfo entities
     *
     * @param sheet the containing sheet
     * @param bars the whole collection of bars to split
     */
    public static void splitBars (Sheet sheet,
                                  Collection<BarInfo> bars)
    {
        process(sheet,
                bars,
                new Adapter<BarInfo>()
                {
                    public List<BarInfo> getTarget (SystemInfo info)
                    {
                        return info.getBars();
                    }

                    public int getXMin (BarInfo bar)
                    {
                        Rectangle box = bar.getStick().getContourBox();
                        return box.x;
                    }

                    public int getYMin (BarInfo bar)
                    {
                        Rectangle box = bar.getStick().getContourBox();
                        return box.y;
                    }
                });
    }

    //-------------------------//
    // splitHorizontalSections //
    //-------------------------//
    /**
     * Split the various horizontal sections [Unused].
     * @param sheet the containing sheet
     */
    public static void splitHorizontalSections (Sheet sheet)
    {
        process(sheet,
                sheet.getHorizontalLag().getSections(),
                new Adapter<GlyphSection>()
                {
                    public List<GlyphSection> getTarget (SystemInfo info)
                    {
                        return info.getHorizontalSections();
                    }

                    public int getXMin (GlyphSection section)
                    {
                        Rectangle box = section.getContourBox();
                        return box.x;
                    }

                    public int getYMin (GlyphSection section)
                    {
                        Rectangle box = section.getContourBox();
                        return box.y;
                    }

                    public boolean isValid (GlyphSection section)
                    {
                        return section.getGlyph() == null;
                    }
                });
    }

    //-----------------------//
    // splitHorizontalSticks //
    //-----------------------//
    /**
     * Split the collection of provided horizontal sticks [Unused].
     *
     * @param sheet the containing sheet
     * @param sticks the entities to be dispatched
     */
    public static void splitHorizontalSticks (Sheet sheet,
                                              List<Stick> sticks)
    {
        process(sheet,
                sticks,
                new Adapter<Stick>()
                {
                    public List<Stick> getTarget (SystemInfo info)
                    {
                        return info.getHorizontalSticks();
                    }

                    public int getXMin (Stick stick)
                    {
                        Rectangle box = stick.getContourBox();
                        return box.x;
                    }

                    public int getYMin (Stick stick)
                    {
                        Rectangle box = stick.getContourBox();
                        return box.y;
                    }
                });
    }

    //------------------//
    // splitHorizontals //
    //------------------//
    /**
     * Split the various horizontals among systems (Used by Systems).
     * @param sheet the containing sheet
     */
    public static void splitHorizontals (Sheet sheet)
    {
        process(sheet,
                sheet.getHorizontals().getLedgers(),
                new Adapter<Ledger>()
                {
                    public List<Ledger> getTarget (SystemInfo info)
                    {
                        return info.getLedgers();
                    }

                    public int getXMin (Ledger ledger)
                    {
                        Rectangle box = ledger.getContourBox();
                        return box.x;
                    }

                    public int getYMin (Ledger ledger)
                    {
                        Rectangle box = ledger.getContourBox();
                        return box.y;
                    }
                });
        process(sheet,
                sheet.getHorizontals().getEndings(),
                new Adapter<Ending>()
                {
                    public List<Ending> getTarget (SystemInfo info)
                    {
                        return info.getEndings();
                    }

                    public int getXMin (Ending ending)
                    {
                        Rectangle box = ending.getContourBox();
                        return box.x;
                    }

                    public int getYMin (Ending ending)
                    {
                        Rectangle box = ending.getContourBox();
                        return box.y;
                    }
                });
    }

    //-----------------------//
    // splitVerticalSections //
    //-----------------------//
    /**
     * Split the various horizontal sections (Used by Glyphs).
     * @param sheet the containing sheet
     */
    public static void splitVerticalSections (Sheet sheet)
    {
        process(sheet,
                sheet.getVerticalLag().getSections(),
                new Adapter<GlyphSection>()
                {
                    public List<GlyphSection> getTarget (SystemInfo info)
                    {
                        return info.getVerticalSections();
                    }

                    public int getXMin (GlyphSection section)
                    {
                        Rectangle box = section.getContourBox();
                        return box.x;
                    }

                    public int getYMin (GlyphSection section)
                    {
                        Rectangle box = section.getContourBox();
                        return box.y;
                    }
                });
    }

    //---------------------//
    // splitVerticalSticks //
    //---------------------//
    /**
     * Split the collection of provided vertical sticks (Used by
     * VerticalsBuilder).
     *
     * @param sheet the containing sheet
     * @param sticks the entities to be dispatched
     */
    public static void splitVerticalSticks (Sheet sheet,
                                            List<Stick> sticks)
    {
        process(sheet,
                sticks,
                new Adapter<Stick>()
                {
                    public List<Stick> getTarget (SystemInfo info)
                    {
                        return info.getVerticalSticks();
                    }

                    public int getXMin (Stick stick)
                    {
                        Rectangle box = stick.getContourBox();
                        return box.x;
                    }

                    public int getYMin (Stick stick)
                    {
                        Rectangle box = stick.getContourBox();
                        return box.y;
                    }
                });
    }

    //---------------------//
    // computeSystemLimits //
    //---------------------//
    /**
     * Compute the top and bottom ordinates of the related area of each
     * system
     * @param sheet the containing sheet
     */
    public static void computeSystemLimits (Sheet sheet)
    {
        // Compute the dimensions of the picture area of every system
        SystemInfo prevInfo = null;

        for (SystemInfo info : sheet.getSystems()) {
            // Very first system
            if (prevInfo == null) {
                info.setAreaTop(0);
            } else {
                // Top of system area, defined as middle ordinate between
                // ordinate of last line of last staff of previous system
                // and ordinate of first line of first staff of current
                // system
                int middle = (prevInfo.getBottom() + info.getTop()) / 2;
                prevInfo.setAreaBottom(middle);
                info.setAreaTop(middle);
            }

            // Remember this info for next system
            prevInfo = info;
        }

        // Bottom of last system
        prevInfo.setAreaBottom(Integer.MAX_VALUE);
    }

    //---------//
    // process //
    //---------//
    /**
     * A generic method, meant to process a given collection of source
     * entities according to the different systems of the sheet.
     *
     * <p>Within a system, the various glyphs of same kind are then
     * X-sorted.
     *
     * @param entities collection ofentities to be split among the score
     * systems
     * @param adapter generic mediator to specify how each entity has to be
     *                 processed
     */
    private static <T> void process (Sheet sheet,
                                     Collection<T> entities,
                                     final Adapter<T> adapter)
    {
        // Sort entities according to their starting ordinate, in order to
        // split them vertically per systems.
        ArrayList<T> sortedEntities = new ArrayList<T>(entities);
        Collections.sort(sortedEntities,
                         new Comparator<T>()
                         {
                             public int compare (T o1,
                                                 T o2)
                             {
                                 return adapter.getYMin(o1)
                                     - adapter.getYMin(o2);
                             }
                         });

        // Iterator on sorted Entities
        Iterator<T> it = sortedEntities.iterator();
        if (!it.hasNext()) {
            // Nothing to split
            return;
        }

        T obj = it.next();

        // Iterator on systems (info)
        systemloop :
        for (SystemInfo info : sheet.getSystems()) {
            List<T> target = adapter.getTarget(info);

            while (true) {
                // Check end of system
                if (adapter.getYMin(obj) > info.getAreaBottom()) {
                    break; // Move to next system
                }

                // Include this entity, if valid, in the system area
                if (adapter.isValid(obj)) {
                    target.add(obj);
                }

                // Move to next entity if any
                if (it.hasNext()) {
                    obj = it.next();
                } else {
                    break systemloop;
                }
            }
        }

        // Sort entities, system by system, according to their left
        // abscissa.
        for (SystemInfo info : sheet.getSystems()) {
            List<T> target = adapter.getTarget(info);
            Collections.sort(target,
                             new Comparator<T>()
                             {
                                 public int compare (T o1,
                                                     T o2)
                                 {
                                     return adapter.getXMin(o1)
                                         - adapter.getXMin(o2);
                                 }
                             });
        }

    }

    //~ Classes -----------------------------------------------------------

    /**
     * Class <code>Adapter</code> is a generic adapter which allows to
     * specify how a given kind of entities has to be processed in the
     * split.
     */
    private abstract static class Adapter <T>
    {
        //~ Methods -------------------------------------------------------

        /**
         * Report the target collection within each system info structure
         * for each SystemInfo
         *
         * @param info the SystemInfo which contains the target collection
         *
         * @return the target list to fill
         */
        public abstract List<T> getTarget (SystemInfo info);

        /**
         * Report the first ordinate of the entity, to be able to split
         * them according to the system ordinates
         *
         * @param obj the entity to be put in the right system
         *
         * @return the top ordinate of the entity
         */
        public abstract int getYMin (T obj);

        /**
         * Report the first abscissa of the entity, to be able to split
         * them according to the system abscissae
         *
         * @param obj the entity to be put in the right system
         *
         * @return the left abscissa of the entity
         */
        public abstract int getXMin (T obj);

        /**
         * Check whether the entity is to be taken in the split
         *
         * @param obj the entity to be checked for validity
         *
         * @return true if valid
         */
        public boolean isValid (T obj)
        {
            return true;
        }
    }
}

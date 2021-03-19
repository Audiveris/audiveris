// </editor-fold>
package org.audiveris.omr.sheet.time;

import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.inter.Inter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Class meant to handle a column of time signatures outside system header.
 */
public class BasicTimeColumn
        extends TimeColumn
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The measure stack for which a column of times is checked. */
    final MeasureStack stack;

    /** Relevant time symbols found in stack. */
    final Set<Inter> timeSet;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code BasicColumn} object.
     *
     * @param stack   stack to be worked upon
     * @param timeSet set of time symbols found in stack
     */
    public BasicTimeColumn (MeasureStack stack,
                            Set<Inter> timeSet)
    {
        super(stack.getSystem());
        this.stack = stack;
        this.timeSet = new LinkedHashSet<Inter>(timeSet);
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    protected TimeBuilder allocateBuilder (Staff staff)
    {
        return new BasicTimeBuilder(staff, this);
    }

    @Override
    protected void cleanup ()
    {
        for (Inter inter : timeSet) {
            inter.remove();
        }
    }

    //----------------//
    // purgeUnaligned //
    //----------------//
    @Override
    protected void purgeUnaligned ()
    {
        // Maximum abscissa shift between de-skewed time items in column
        final int maxDxOffset = getMaxDxOffset(stack.getSystem().getSheet());

        class Item
        {

            final Inter time;

            final double xOffset;

            public Item (Inter time,
                         double xOffset)
            {
                this.time = time;
                this.xOffset = xOffset;
            }
        }

        class Line
        {

            List<Item> items = new ArrayList<Item>();

            Double meanOffset = null;

            void addItem (Item item)
            {
                items.add(item);
                meanOffset = null; // Reset cached value
            }

            double getOffset ()
            {
                if (meanOffset == null) {
                    double sum = 0;

                    for (Item item : items) {
                        sum += item.xOffset;
                    }

                    sum /= items.size();
                    meanOffset = sum;
                }

                return meanOffset;
            }
        }

        List<Line> lines = new ArrayList<>();

        for (Staff staff : system.getStaves()) {
            if (staff.isTablature()) {
                continue;
            }

            TimeBuilder builder = builders.get(staff);

            for (List<Inter> list : Arrays.asList(builder.wholes, builder.nums, builder.dens)) {
                for (Inter inter : list) {
                    double xOffset = stack.getXOffset(inter.getCenter(), inter.getStaff());
                    Item item = new Item(inter, xOffset);

                    // is there a compatible line?
                    boolean found = false;

                    for (Line line : lines) {
                        if (Math.abs(line.getOffset() - xOffset) <= maxDxOffset) {
                            line.addItem(item);
                            found = true;

                            break;
                        }
                    }

                    if (!found) {
                        // No compatible line found, create a brand new one
                        Line line = new Line();
                        line.addItem(item);
                        lines.add(line);
                    }
                }
            }
        }

        // Select a single vertical line (based on item count? or the left-most line?)
        Collections.sort(lines, (o1, o2) -> Double.compare(o1.getOffset(), o2.getOffset()));

        Line chosenLine = lines.get(0);
        List<Inter> kept = new ArrayList<>();

        for (Item item : chosenLine.items) {
            kept.add(item.time);
        }

        // Purge all entities non kept
        for (Staff staff : system.getStaves()) {
            if (staff.isTablature()) {
                continue;
            }

            TimeBuilder builder = builders.get(staff);

            for (List<Inter> list : Arrays.asList(builder.wholes, builder.nums, builder.dens)) {
                for (Iterator<Inter> it = list.iterator(); it.hasNext();) {
                    Inter inter = it.next();

                    if (!kept.contains(inter)) {
                        inter.remove();
                        it.remove();
                    }
                }
            }
        }
    }
}

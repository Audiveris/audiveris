//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S h e e t B e n c h                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.score.Score;

import omr.sheet.stem.StemScale;

import omr.step.Step;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Class {@code SheetBench} records all important information related
 * to the processing of a music sheet.
 * <p>
 * It delegates the actual recording to the containing score bench,
 * just prefixing the records with the sheet index.</p>
 *
 * @author Hervé Bitteur
 */
public class SheetBench
        extends Bench
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SheetBench.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The related sheet. */
    private final Sheet sheet;

    /** The prefix to use for this sheet. */
    private final String sheetPrefix;

    /** The related book. */
    private final Book book;

    /** Time stamp when this instance was created. */
    private final long startTime = System.currentTimeMillis();

    /** Starting date. */
    private final Date date = new Date(startTime);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SheetBench object.
     *
     * @param sheet the related sheet
     */
    public SheetBench (Sheet sheet)
    {
        this.sheet = sheet;

        sheetPrefix = String.format("p%02d.", sheet.getIndex());
        book = sheet.getBook();

        addProp("image", book.getImagePath() + "#" + sheet.getIndex());

        flushBench();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // getSheet //
    //----------//
    /**
     * @return the sheet
     */
    public Sheet getSheet ()
    {
        return sheet;
    }

    //--------------------//
    // recordCancellation //
    //--------------------//
    public void recordCancellation ()
    {
        addProp("whole.cancelled", "true");
    }

    //-------------//
    // recordDelta //
    //-------------//
    public void recordDelta (double delta)
    {
        addProp("delta", delta);
    }

    //----------------------//
    // recordImageDimension //
    //----------------------//
    public void recordImageDimension (int width,
                                      int height)
    {
        addProp("image.width", width);
        addProp("image.height", height);
    }

    //-----------------//
    // recordPartCount //
    //-----------------//
    public void recordPartCount (int partCount)
    {
        addProp("parts", partCount);
    }

    //-------------//
    // recordScale //
    //-------------//
    public void recordScale (Scale scale)
    {
        addProp("scale.mainFore", scale.getMainFore());
        addProp("scale.interline", scale.getInterline());
        addProp("scale.maxFore", scale.getMaxFore());
        addProp("scale.maxInterline", scale.getMaxInterline());
        addProp("scale.minInterline", scale.getMinInterline());

        if (scale.getSecondInterline() != null) {
            addProp("scale.secondInterline", scale.getSecondInterline());
            addProp("scale.maxSecondInterline", scale.getMaxSecondInterline());
            addProp("scale.minSecondInterline", scale.getMinSecondInterline());
        }

        addProp("scale.mainBeam", scale.getMainBeam());

        ///addProp("scale.mainStem", scale.getMainStem());
    }

    //-----------------//
    // recordStemScale //
    //-----------------//
    public void recordStemScale (StemScale stemScale)
    {
        addProp("stemScale.mainThickness", stemScale.getMainThickness());
        addProp("stemScale.maxThickness", stemScale.getMaxThickness());
    }

    //------------//
    // recordSkew //
    //------------//
    public void recordSkew (double skew)
    {
        addProp("skew", skew);
    }

    //------------------//
    // recordStaveCount //
    //------------------//
    public void recordStaveCount (int staveCount)
    {
        addProp("staves", staveCount);
    }

    //------------//
    // recordStep //
    //------------//
    public void recordStep (Step step,
                            long duration)
    {
        addProp("step." + step.name().toLowerCase() + ".duration", duration);
        flushBench();
    }

    //-------------------//
    // recordSystemCount //
    //-------------------//
    public void recordSystemCount (int systemCount)
    {
        addProp("systems", systemCount);
    }

    //---------//
    // addProp //
    //---------//
    /**
     * Redirect to the score bench, with the sheet prefix
     *
     * @param radix the provided radix
     * @param value the property value
     */
    @Override
    protected final void addProp (String radix,
                                  String value)
    {
        book.getBench().addProp(sheetPrefix + radix, value);
    }

    //------------//
    // flushBench //
    //------------//
    /**
     * Flush the score container.
     */
    @Override
    protected final void flushBench ()
    {
        book.getBench().flushBench();
    }
}

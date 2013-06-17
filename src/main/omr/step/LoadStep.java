//----------------------------------------------------------------------------//
//                                                                            //
//                              L o a d S t e p                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.score.Score;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.picture.PictureLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.RenderedImage;
import java.io.File;
import java.util.Collection;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code LoadStep} reloads the image for a sheet,
 * from a provided image file.
 * <p>This is simply a RE-loading, triggered by the user. The initial loading
 * is always done in {@link Score#createPages(SortedSet)}.</p>
 *
 * @author Hervé Bitteur
 */
public class LoadStep
        extends AbstractStep
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(LoadStep.class);

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new LoadStep object.
     */
    public LoadStep ()
    {
        super(
                Steps.LOAD,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                Step.PICTURE_TAB,
                "Reload the sheet picture");
    }

    //~ Methods ----------------------------------------------------------------
    //------//
    // doit //
    //------//
    @Override
    public void doit (Collection<SystemInfo> unused,
                      Sheet sheet)
            throws StepException
    {
        final Score score = sheet.getScore();
        final File imageFile = score.getImageFile();
        final int index = sheet.getPage()
                .getIndex();
        final SortedSet<Integer> set = new TreeSet<>();
        set.add(index);

        SortedMap<Integer, RenderedImage> images =
                PictureLoader.loadImages(imageFile, set);
        if (images != null) {
            sheet.setImage(images.get(index));
        }
    }
}

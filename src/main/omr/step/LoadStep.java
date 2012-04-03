//----------------------------------------------------------------------------//
//                                                                            //
//                              L o a d S t e p                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.log.Logger;

import omr.score.Score;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.picture.PictureLoader;

import java.io.File;
import java.util.Collection;

/**
 * Class {@code LoadStep} reloads the image for a sheet, from a provided image
 * file.
 * <p>This is simply a RE-loading, triggered by the user. The initial loading
 * is always done in {@link Score#createPages()}.</p>
 *
 * @author Hervé Bitteur
 */
public class LoadStep
    extends AbstractStep
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LoadStep.class);

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
            Redoable.REDOABLE,
            Step.PICTURE_TAB,
            "Reload the sheet picture");
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // doit //
    //------//
    @Override
    public void doit (Collection<SystemInfo> unused,
                      Sheet                  sheet)
        throws StepException
    {
        final Score score = sheet.getScore();
        final File  imageFile = score.getImageFile();
        final int   index = sheet.getPage()
                                 .getIndex();
        sheet.setImage(PictureLoader.loadImages(imageFile, index).get(index));
    }
}

/**
 * Class <code>Audiveris</code> is simply the entry point to OMR, which
 * delegates the call to {@link omr.Main#main}.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public final class Audiveris
{
    //~ Constructors -----------------------------------------------------------

    //-----------//
    // Audiveris //
    //-----------//
    /** To avoid instantiation */
    private Audiveris ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // main //
    //------//
    /**
     * The main entry point, which just calls {@link omr.Main#main}
     *
     * @param args These args are simply passed to Main
     */
    public static void main (final String[] args)
    {
        omr.Main.main(args);
    }
}

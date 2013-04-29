
package hudson.util.jna;

import com.sun.jna.Native;

///import hudson.Util;
import java.io.IOException;

/**
 * IOException originated from Windows API call.
 *
 * @author Kohsuke Kawaguchi
 */
public class WinIOException
    extends IOException
{
    //~ Instance fields --------------------------------------------------------

    private final int errorCode = Native.getLastError();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new WinIOException object.
     */
    public WinIOException ()
    {
    }

    /**
     * Creates a new WinIOException object.
     *
     * @param message DOCUMENT ME!
     */
    public WinIOException (String message)
    {
        super(message);
    }

    /**
     * Creates a new WinIOException object.
     *
     * @param message DOCUMENT ME!
     * @param cause DOCUMENT ME!
     */
    public WinIOException (String    message,
                           Throwable cause)
    {
        super(message);
        initCause(cause);
    }

    /**
     * Creates a new WinIOException object.
     *
     * @param cause DOCUMENT ME!
     */
    public WinIOException (Throwable cause)
    {
        initCause(cause);
    }

    //~ Methods ----------------------------------------------------------------

    public int getErrorCode ()
    {
        return errorCode;
    }

    @Override
    public String getMessage ()
    {
        ///return super.getMessage()+" error="+errorCode+":"+ Util.getWin32ErrorMessage(errorCode);
        return super.getMessage() + " error=" + errorCode;
    }
}

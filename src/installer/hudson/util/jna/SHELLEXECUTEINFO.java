//----------------------------------------------------------------------------//
//                                                                            //
//                      S H E L L E X E C U T E I N F O                       //
//                                                                            //
//----------------------------------------------------------------------------//
package hudson.util.jna;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

/**
 *
 * <pre>
       typedef struct _SHELLEXECUTEINFO {
         DWORD     cbSize;
         ULONG     fMask;
         HWND      hwnd;
         LPCTSTR   lpVerb;
         LPCTSTR   lpFile;
         LPCTSTR   lpParameters;
         LPCTSTR   lpDirectory;
         int       nShow;
         HINSTANCE hInstApp;
         LPVOID    lpIDList;
         LPCTSTR   lpClass;
         HKEY      hkeyClass;
         DWORD     dwHotKey;
         union {
           HANDLE hIcon;
           HANDLE hMonitor;
         } DUMMYUNIONNAME;
         HANDLE    hProcess;
       } SHELLEXECUTEINFO, *LPSHELLEXECUTEINFO;
 * </pre>
 * @author Kohsuke Kawaguchi
 * see http://msdn.microsoft.com/en-us/library/windows/desktop/bb759784%28v=vs.85%29.aspx
 */
public class SHELLEXECUTEINFO
    extends Structure
{
    //~ Static fields/initializers ---------------------------------------------

    public static final int SEE_MASK_NOCLOSEPROCESS = 0x40;
    public static final int SW_HIDE = 0;
    public static final int SW_SHOW = 0;

    //~ Instance fields --------------------------------------------------------

    public int     cbSize = size();
    public int     fMask;
    public Pointer hwnd;
    public String  lpVerb;
    public String  lpFile;
    public String  lpParameters;
    public String  lpDirectory;
    public int     nShow = 1;
    public Pointer hInstApp;
    public Pointer lpIDList;
    public String  lpClass;
    public Pointer hkeyClass;
    public int     dwHotKey;
    public Pointer hIcon;
    public Pointer hProcess;

    //~ Methods ----------------------------------------------------------------

    @Override
    protected List getFieldOrder ()
    {
        return Arrays.asList(
            new String[] {
                "cbSize", "fMask", "hwnd", "lpVerb", "lpFile", "lpParameters",
                "lpDirectory", "nShow", "hInstApp", "lpIDList", "lpClass",
                "hkeyClass", "dwHotKey", "hIcon", "hProcess"
            });
    }
}

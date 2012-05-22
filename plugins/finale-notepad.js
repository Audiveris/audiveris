/* -------------------------------------------------------------------------- */
/*                                                                            */
/*                      f i n a l e - n o t e p a d . j s                     */
/*                                                                            */
/* -------------------------------------------------------------------------- */

/* Variable to modify according to your environment */
var pathToExec = "P:/prog/Finale NotePad 2011/Finale NotePad.exe";

/* Title for menu item */
pluginTitle = 'Finale Notepad';

/* Long description for tool tip */
pluginTip = 'Invoke Finale Notepad on score XML';

/* Build sequence of command line parameters */
function pluginCli(exportFilePath) {
    importPackage(java.util);
    return Arrays.asList([pathToExec, exportFilePath]);
}

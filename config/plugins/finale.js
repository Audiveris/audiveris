/* -------------------------------------------------------------------------- */
/*                                                                            */
/*                              f i n a l e . j s                             */
/*                                                                            */
/* -------------------------------------------------------------------------- */

/* Variable to modify according to your environment */
var pathToExec = "C:/Program Files (x86)/Finale 2014/Finale.exe";

/* Title for menu item */
pluginTitle = 'Finale';

/* Long description for tool tip */
pluginTip = 'Invoke Finale on score XML';

/* Build sequence of command line parameters */
function pluginCli(exportFilePath) {
    importPackage(java.util);
    return Arrays.asList([pathToExec, exportFilePath]);
}

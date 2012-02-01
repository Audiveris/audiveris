/* -------------------------------------------------------------------------- */
/*                              h e a d e r . j s                             */
/* -------------------------------------------------------------------------- */
document.write("<header>");

// Navigation tabs
document.write("    <nav>");
document.write("        <ul id='tabs'>");
insert("Home",         "index.html");
insert("Snapshots",    "docs/manual/snapshots.html");
insert("Installation", "docs/manual/installation.html");
insert("Example",      "docs/manual/example.html");
insert("Operation",    "docs/manual/operation.html");
insert("Releases",     "docs/manual/releases.html");
document.write("        </ul>");
document.write("    </nav>            ");

// Logo
document.write("    <a id='logo' href='" + context.root + "index.html'>");
document.write("       <div id='logo-subtitle'>Open Music Scanner</div>");
document.write("    </a>");

// Download button
document.write("    <a id='download-button' href='" + context.root + "docs/manual/installation.html'>");
document.write("        <div id='download-version'>");
document.write("            <b>V4.1beta</b>");
document.write("        </div>");
document.write("    </a>");

document.write("</header>");

function insert(label, url) {
    if (label == context.page) {
        document.write("            <li><a class='current' href='" + context.root + url + "'>" + label + "</a></li>");        
    } else {
        document.write("            <li><a href='" + context.root + url + "'>" + label + "</a></li>");        
    }
}





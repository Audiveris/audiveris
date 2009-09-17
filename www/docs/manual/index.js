//----------------------------------------------------------------------------//
//                                                                            //
//                               i n d e x . j s                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//      $Id$

// Table of defined manuals, add any new manual to this list
var manuals = new Array("releases", "installation", "example", "operation", "design", "implementation", "notes", "api");

// Must be parallel to previous table
var manualNames = new Array("Releases", "Installation", "Example", "Operation", "Design", "Implementation", "Notes", "API-javadoc");

// Selected Manual
top.selectedManual = "";

// Select a manual by its name
function selectManual (manual)
{
    // Default value for manual
    if (manual == "") {
       manual = manuals[0];
    }
    
    // Remember the selected manual
    top.selectedManual = manual;
}

// Actually load the manual and its toc into the proper frames
function loadManual(manual)
{
    if (manual != "") {
        if (manual == "api") {
            // Load the API index in Pack frameset
            Pack.location.href  = "../api/index.html";
        } else if (manual == "notes") {
            // Load the tiddly wiki in Pack frameset
            Pack.location.href  = "../wiki/notebook.html";
	} else {
            Nav.location.href  = manual + "-nav.html";
            Main.location.href = manual +     ".html";
        }
    }
}

// Prepare the HTML for banner tabs
function buildRow (manual)
{
    ///alert("window.name=" + window.name);
    var buf = "";
    var tag;
    for (var i = 0; i < manuals.length; i++) {
        if (manuals[i] == manual) {
            tag = "TH";
        } else {
            tag = "TD";
        }
        buf += "<" + tag + ">";

	if (manuals[i] == "api") {
            buf += "<A TARGET='_top' HREF='../api/index.html' >";
	} else if (manuals[i] == "notes") {
            buf += "<A TARGET='_top' HREF='../wiki/notebook.html' >";
        } else {
            buf += "<A TARGET='_top' HREF='index.html?manual=" + manuals[i] + "' >";
        }
        buf += manualNames[i];
        buf += "</" + tag + ">";
    }
    ///alert("buf=" + buf);

    return buf;
}

// Check whether the URL contains a manual name
function checkSelection()
{
    
    var paramStr = "" + window.location.search;

    if (paramStr != "" && paramStr != "undefined") {
        paramStr = paramStr.substring(1); //Skip the '?' character
        var params = paramStr.split("&");
        for (var i = 0; i < params.length; i++) {
            var pair = params[i].split("=");
            if (pair[0] == "manual" && pair[1] != "") {
                selectManual(pair[1]);
            }
        }
    } else {
       selectManual(""); // Use default
    }
}

// Return a readable date from a CVS file tag like the following line
// $Id$
function getCVSDate(s)
{
    var as = s.split(" ");
    var d = new Date(as[3]);
    var months = new Array("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
    
    return d.getDate() + "-" + months[d.getMonth()] + "-" + d.getFullYear()
}

// Dummy implementation when displaying javadoc files
function loadFrames(){}

// To include other HTML fragments
// Copied from http://www.boutell.com/newfaq/creating/include.html
function clientSideInclude(doc, id, url) {
  var req = false;
  // For Safari, Firefox, and other non-MS browsers
  if (window.XMLHttpRequest) {
    try {
      req = new XMLHttpRequest();
    } catch (e) {
      req = false;
    }
  } else if (window.ActiveXObject) {
    // For Internet Explorer on Windows
    try {
      req = new ActiveXObject("Msxml2.XMLHTTP");
    } catch (e) {
      try {
        req = new ActiveXObject("Microsoft.XMLHTTP");
      } catch (e) {
        req = false;
      }
    }
  }
 var element = doc.getElementById(id);
 if (!element) {
  alert("Bad id '" + id +
   "' passed to clientSideInclude." +
   "You need a div or span element " +
   "with this id in your page.");
  return;
 }
  if (req) {
    // Synchronous request, wait till we have it all
    req.open('GET', url, false);
    req.send(null);
    element.innerHTML = req.responseText;
  } else {
    element.innerHTML =
   "Sorry, your browser does not support " +
      "XMLHTTPRequest objects. This page requires " +
      "Internet Explorer 5 or better for Windows, " +
      "or Firefox for any system, or Safari. Other " +
      "compatible browsers may also exist.";
  }
}


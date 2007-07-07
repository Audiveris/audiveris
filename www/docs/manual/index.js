//-----------------------------------------------------------------------//
//                                                                       //
//                            i n d e x . j s                            //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$

// Table of defined manuals, add any new manual to this list
var manuals = new Array("releases", "installation", "operation", "design", "implementation", "notes", "api");

// Must be parallel to previous table
var manualNames = new Array("Releases", "Installation", "Operation", "Design", "Implementation", "Notes", "API-javadoc");

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

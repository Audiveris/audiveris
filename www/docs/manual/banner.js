//-----------------------------------------------------------------------//
//                                                                       //
//                           b a n n e r . j s                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$

// Table of defined manuals, add any new manual to this list
var manuals = new Array("releases", "installation", "operation", "design", "implementation");

// Select a manual by its name
function selectManual (manual)
{
    // Make the banner tabs reflect the selection
    updateRow(manual);

	if (manual != "") {
	    // Load the proper file in Nav and Main frames
		parent.Nav.location.href  = manual + "-nav.html";
		parent.Main.location.href = manual +     ".html";
	}
}

// Prepare the HTML for banner tabs
function buildRow (manual)
{
    var buf = "";
    var tag;
    for (var i = 0; i < manuals.length; i++) {
        if (manuals[i] == manual) {
            tag = "TH";
        } else {
            tag = "TD";
        }
        buf += "<" + tag + ">";
        buf += "<A HREF=javascript:selectManual('" + manuals[i] + "'); >";
        buf += manuals[i];
        buf += "</" + tag + ">";
    }

    return buf;
}

// Update the row of tabs
function updateRow (manual)
{
    document.getElementById("bannerTabs").rows[0].innerHTML=buildRow(manual);
}

// Check whether the URL contains a manual name
function checkSelection()
{
	var urlStr = window.parent.document.location.href;
	var paramStr = urlStr.split("?")[1];

	if (paramStr) {
		var params = paramStr.split("&");
		for (var i = 0; i < params.length; i++) {
	   	    var pair = params[i].split("=");
		    if (pair[0] == "manual" && pair[1]) {
				// alert("Selection for Manual : " + pair[1]);
				selectManual(pair[1]);
		    }
	    }
	}
}

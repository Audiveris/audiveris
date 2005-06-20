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
var manuals = new Array("releases", "installation", "operation", "design", "implementation", "api");

// Selected Manual
selectedManual = "";

// Select a manual by its name
function selectManual (manual)
{
    selectedManual = manual;
    updateRow(manual);
    if (manual != "") {
        if (manual == "api") {
            // Load the API index in Pack frameset
            parent.Pack.location.href  = "../api/index.html";
        } else {
            parent.Pack.location.href  = "../manual/index-pack.html";
        }
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
//        buf += "<A HREF=javascript:top.selectManual('" + manuals[i] + "'); >";
        buf += "<A TARGET='_top' HREF='../manual/index.html?manual=" + manuals[i] + "' >";
        buf += manuals[i];
        buf += "</" + tag + ">";
    }

    return buf;
}

// Update the row of tabs
function updateRow (manual)
{
    Banner.document.getElementById("bannerTabs").rows[0].innerHTML=buildRow(manual);
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
    }
}

function loadFrames(){}

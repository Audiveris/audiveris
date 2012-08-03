package com.handfree.Integration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import omr.WellKnowns;
import omr.score.Score;
import omr.step.Step;
import omr.step.Stepping;
import omr.step.Steps;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AudiverisInteTest {
	private File image;


	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws IOException {
		String[] args1 = new String[] { "-batch", "-step", "EXPORT","-input",
				"examples/Invention5_1.JPG"};
		String[] args2 = new String[] { "-batch", "-step", "EXPORT",
				"examples/batuque.png", "examples/allegretto.png" };
		System.out.println("firstCall");
		// We need class WellKnowns to be elaborated before class Main
		WellKnowns.ensureLoaded();

		// Then we call Main...
		omr.Main.doMain(args1);

		// Step exportStep = Steps.valueOf(Steps.EXPORT);
		// Score score = new Score(image);
		// if (!score.getFirstPage().getSheet().isDone(exportStep)) {
		// System.out.println("Getting export from {0} ..."+ score);
		// Stepping.processScore(Collections.singleton(exportStep), score);
		// }
		// File target = score.getExportFile();
		// System.out.println(target.getCanonicalPath());

	}
}

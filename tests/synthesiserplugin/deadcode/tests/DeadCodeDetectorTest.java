package synthesiserplugin.deadcode.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import synthesiserplugin.deadcode.DeadCodeDetector;
import synthesiserplugin.parser.Parser;

public class DeadCodeDetectorTest {
	
	final static String FILE_PATH = "tests/synthesiserplugin/deadcode/to/test/JFrameExample.java";
	static File file;
	static CompilationUnit cu;
	static DeadCodeDetector detector;
	static ICompilationUnit compilationUnit;
	
	@BeforeClass
	public static void setUpClass() throws IOException {

		file = new File(FILE_PATH);
		String fileContent = Parser.readFileToString(FILE_PATH);
		detector = new DeadCodeDetector(compilationUnit);
		cu = Parser.parse(fileContent);

		
	}
	
	@Test
	public void testDetectProblems() {
//		List<IProblem> detectedProblems = detector.detectProblems();
//		assertEquals(0,detectedProblems.size());
	}
	
	
	@After
	public void tearDown() {

	}

	
	@AfterClass
	public static void tearDownClass() {
		
		file = null;
		cu = null;
		detector = null;
	}

}

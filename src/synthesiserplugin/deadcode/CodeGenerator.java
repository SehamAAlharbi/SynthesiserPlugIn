package synthesiserplugin.deadcode;

import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

public class CodeGenerator {
	
	private IPackageFragment documentationPackage;
	
	public CodeGenerator(IPackageFragment documentationPackage) {
		this.documentationPackage = documentationPackage;
	}

	/**
	 * 
	 * @param preview the returned value from proposal.getPreviewContent()
	 * @throws CoreException 
	 */
	public ICompilationUnit generate (CUCorrectionProposal removalProposal) throws CoreException {
		
		String preview = removalProposal.getPreviewContent();
		  // convert the returned string into code and create a java file out of it
        Pattern NEWLINE = Pattern.compile("\\R");
        String lines[] = NEWLINE.split(preview);
        StringBuffer buffer = new StringBuffer();
        for (String line : lines){
        	if (line == lines[0]) {
        		buffer.append("package documentation.usage.examples; \n\n");
        		continue;
        	}
        	buffer.append(line + "\n");
          }
        
        return process(buffer);
		
	}
	
	private ICompilationUnit process (StringBuffer codeBuffer) throws JavaModelException {
		
		// here you need to get the original CU name and use it to name the new CU, or call another method that refactor the CU name to the new one 
        String CUName = "ExampleOne.java";
        ICompilationUnit icu = documentationPackage.createCompilationUnit(CUName, codeBuffer.toString(), false, null);
	
		return icu;
	}

}

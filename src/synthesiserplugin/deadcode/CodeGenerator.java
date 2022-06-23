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
	public ICompilationUnit generate (CUCorrectionProposal removalProposal, String icuName) throws CoreException {
		
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
        
        return process(buffer, icuName);
		
	}
	
	/**
	 * for code with no dead code problems
	 * @param icu
	 * @return
	 * @throws JavaModelException 
	 */
	public ICompilationUnit placeInDocPackage(ICompilationUnit icu) throws JavaModelException {
		
		String oldName = icu.getElementName();
		// to remove the .java from the original icu name
        String newICUName = oldName.substring(0, oldName.length()-5) + "Doc.java";
        ICompilationUnit newICU = documentationPackage.createCompilationUnit(newICUName, icu.toString(), false, null);
       
		return newICU;
	}
	
	/**
	 * for code with dead code problems
	 * @param codeBuffer
	 * @param icuName
	 * @return
	 * @throws JavaModelException
	 */
	private ICompilationUnit process (StringBuffer codeBuffer, String icuName) throws JavaModelException {
		
		// to remove the .java from the original icu name
        String newICUName = icuName.substring(0, icuName.length()-5) + "Doc.java";
        ICompilationUnit icu = documentationPackage.createCompilationUnit(newICUName, codeBuffer.toString(), false, null);
	
		return icu;
	}

}

package synthesiserplugin.deadcode.detector;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;


public class DeadCodeDetector {

	private IJavaProject javaProject;
	private IPackageFragmentRoot sourceFolder;

	public DeadCodeDetector(IJavaProject javaProject, IPackageFragmentRoot sourceFolder) {
		this.javaProject = javaProject;
		this.sourceFolder = sourceFolder;
	}
	

	public void DetectIfThenDeadCode() throws CoreException {


		IPackageFragment package_ = sourceFolder.createPackageFragment("sample.deadcode", false, null);
		StringBuffer buf = new StringBuffer();
		buf.append("package sample.deadcode;\n");
		buf.append("public class DeadCodeExample {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (false) {\n");
		buf.append("            System.out.println(\"a\");\n");
		buf.append("        } else {\n");
		buf.append("            System.out.println(\"b\");\n");
		buf.append("        }\n");
		buf.append("        if (false) {\n");
		buf.append("            System.out.println(\"c\");\n");
		buf.append("        } else {\n");
		buf.append("            System.out.println(\"d\");\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");

		ICompilationUnit icu = package_.createCompilationUnit("DeadCodeExample.java", buf.toString(), false, null);

		CompilationUnit astRoot = getASTRoot(icu);
		IProblem [] problems = astRoot.getProblems();
		IProblem problem = problems [0];
		System.out.println(problem.getMessage());
		ArrayList<IJavaCompletionProposal> proposals = collectCorrections(icu, astRoot);
//        assertNumberOfProposals(proposals, 2);
//        assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal = (CUCorrectionProposal) proposals.get(0);
		//prints the message that appears to suggest the fix 
		System.out.println(proposal.getDisplayString());
        String preview1 = getPreviewContent(proposal);
        
        // To see what output this will generate
        System.out.println(preview1);
       
        
	}
	
	protected static String getPreviewContent(CUCorrectionProposal proposal) throws CoreException {
		return proposal.getPreviewContent();
	}

	protected static CompilationUnit getASTRoot(ICompilationUnit cu) {
		// this ASTResolving is also found in
		// org.eclipse.jdt.internal.core.manipulation.dom
		return createQuickFixAST(cu, null);
	}

//	public static AssistContext getCorrectionContext(ICompilationUnit cu, int offset, int length) {
//		AssistContext context= new AssistContext(cu, offset, length);
//		return context;
//	}

	protected static final ArrayList<IJavaCompletionProposal> collectCorrections(ICompilationUnit cu, CompilationUnit astRoot)
			throws CoreException {
		return collectCorrections(cu, astRoot, 1, null);
	}

	protected static final ArrayList<?> collectCorrections(ICompilationUnit cu, CompilationUnit astRoot, int nProblems)
			throws CoreException {
		return collectCorrections(cu, astRoot, nProblems, null);
	}

	protected static final ArrayList<IJavaCompletionProposal> collectCorrections(ICompilationUnit cu, CompilationUnit astRoot, int nProblems,
			AssistContext context) throws CoreException {
		IProblem[] problems = astRoot.getProblems();
		if (problems.length != nProblems) {
			StringBuffer buf = new StringBuffer("Wrong number of problems, is: ");
			buf.append(problems.length).append(", expected: ").append(nProblems).append('\n');
			for (int i = 0; i < problems.length; i++) {
				buf.append(problems[i]);
				buf.append('[').append(problems[i].getSourceStart()).append(" ,").append(problems[i].getSourceEnd())
						.append(']');
				buf.append('\n');
			}

		}

		return collectCorrections(cu, problems[0], context);
	}

	protected static final ArrayList<IJavaCompletionProposal> collectCorrections(ICompilationUnit cu, IProblem curr, IInvocationContext context)
			throws CoreException {
		int offset = curr.getSourceStart();
		int length = curr.getSourceEnd() + 1 - offset;
		if (context == null) {
			context = new AssistContext(cu, offset, length);
		}

		ProblemLocation problem = new ProblemLocation(curr);
		ArrayList<IJavaCompletionProposal> proposals = collectCorrections(context, problem);

		return proposals;
	}

	protected static ArrayList<IJavaCompletionProposal> collectCorrections(IInvocationContext context, IProblemLocation problem)
			throws CoreException {
		ArrayList<IJavaCompletionProposal> proposals = new ArrayList<IJavaCompletionProposal>();
		IStatus status = JavaCorrectionProcessor.collectCorrections(context, new IProblemLocation[] { problem },
				proposals);
		return proposals;
	}

	
	public static CompilationUnit createQuickFixAST(ICompilationUnit compilationUnit, IProgressMonitor monitor) {
		ASTParser astParser = ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);
		astParser.setSource(compilationUnit);
		astParser.setResolveBindings(true);
		astParser.setStatementsRecovery(ASTProvider.SHARED_AST_STATEMENT_RECOVERY);
		astParser.setBindingsRecovery(ASTProvider.SHARED_BINDING_RECOVERY);
		return (CompilationUnit) astParser.createAST(monitor);
	}

}

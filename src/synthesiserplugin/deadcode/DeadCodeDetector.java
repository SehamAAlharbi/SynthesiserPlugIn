package synthesiserplugin.deadcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
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

@SuppressWarnings("restriction")
public class DeadCodeDetector {
	
	private ICompilationUnit icu;
	private List<IProblem> deadCodeProblems;
	private Map<Integer,List<IJavaCompletionProposal>> ProblemCorrectionsMap; 
    
	
	
	public DeadCodeDetector (ICompilationUnit icu) {
		this.icu = icu;
		deadCodeProblems = new ArrayList<IProblem>();
		ProblemCorrectionsMap = new HashMap<Integer,List<IJavaCompletionProposal>>();
		
	}

	public ICompilationUnit getIcu() {
		return icu;
	}


	public void setIcu(ICompilationUnit icu) {
		this.icu = icu;
	}
	
	public void detect() {
		
		CompilationUnit astRoot = getASTRoot(icu);
		IProblem [] problems = astRoot.getProblems();
		
		for (IProblem problem : problems) {
			if(problem.getMessage().equalsIgnoreCase("Dead Code")) {
				deadCodeProblems.add(problem);
			}
		}
		
		// Specify the correction proposals for each problem
		collectCorrectionProposals();
		
	}
	
	public void generateCleanCode(IPackageFragment documentationPackage) throws JavaModelException {
		// code generator
		CodeGenerator generator = new CodeGenerator(documentationPackage);
		
		if(!deadCodeProblems.isEmpty()) {
		// for each problem, get the removal fix
		deadCodeProblems.stream().forEach(problem -> {
			CUCorrectionProposal proposal = getRemaovalProposal(problem.getID());
			try {
				generator.generate(proposal, icu.getElementName());
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		}
		
		else {
			generator.placeInDocPackage(icu);
		}
	}
	
	private void collectCorrectionProposals() {
		
		if (!deadCodeProblems.isEmpty()) {
			deadCodeProblems.stream().forEach(problem -> {
				int offset = problem.getSourceStart();
				int length = problem.getSourceEnd() + 1 - offset;
				IInvocationContext context = new AssistContext(icu, offset, length);
				ProblemLocation problemLocation = new ProblemLocation(problem);
				ArrayList<IJavaCompletionProposal> CompletionProposals = new ArrayList<IJavaCompletionProposal>();
				IStatus status = JavaCorrectionProcessor.collectCorrections(context, new IProblemLocation[] { problemLocation }, CompletionProposals);
				ProblemCorrectionsMap.put(problem.getID(), CompletionProposals);
			});
		}
	}
	
	// This method assumes that there is only one dead code problem, but what if there is more?
	private CUCorrectionProposal getRemaovalProposal (int problemID) {
		
		List<IJavaCompletionProposal> CompletionProposals = ProblemCorrectionsMap.get(problemID);
		// the removal proposal is always the first one
		CUCorrectionProposal proposal = (CUCorrectionProposal) CompletionProposals.get(0);
		
		return proposal;
		
		
	}
	
	
	private static CompilationUnit getASTRoot(ICompilationUnit cu) {
		// this ASTResolving is also found in
		// org.eclipse.jdt.internal.core.manipulation.dom
		return createQuickFixAST(cu, null);
	}
	
	private static CompilationUnit createQuickFixAST(ICompilationUnit iCompilationUnit, IProgressMonitor monitor) {
		ASTParser astParser = ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);
		astParser.setSource(iCompilationUnit);
		astParser.setResolveBindings(true);
		astParser.setStatementsRecovery(ASTProvider.SHARED_AST_STATEMENT_RECOVERY);
		astParser.setBindingsRecovery(ASTProvider.SHARED_BINDING_RECOVERY);
		return (CompilationUnit) astParser.createAST(monitor);
	}
}

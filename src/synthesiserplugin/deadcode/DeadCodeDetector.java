package synthesiserplugin.deadcode;

import java.util.ArrayList;			
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.CorrectionEngine;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import synthesiserplugin.parser.Parser;

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
	
	public DeadCodeDetector () {}

	public ICompilationUnit getIcu() {
		return icu;
	}


	public void setIcu(ICompilationUnit icu) {
		this.icu = icu;
	}
	
	public void detect()  {
		
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
				generator.generate(proposal.getPreviewContent(), icu.getElementName());
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
	
	
	
	// ----------------------- new dead code solution -------------------- //
	
	
	public ArrayList <IProblem> detectProblems() {
		
		CompilationUnit astRoot = getASTRoot(icu);
		
		// get problems
		IProblem [] problems = astRoot.getProblems();
		// make sure problems exist
		System.out.println(problems.length);
		// get the first one i.e dead code
		IProblem problemm = problems[0];
		// is it dead code?
		System.out.println(problemm.getMessage());
		// its location in source code
		int [] problemLocation = getProblemLocation(problemm);
		// print location
		System.out.println(problemLocation[0] + " " + problemLocation[1]);
		ASTNode affectedNode = getAffectedNode(astRoot,problemLocation[0] , problemLocation[1]);
		// the ASTNode causing dead code problem
		System.out.println(affectedNode);
		// the parent
		System.out.println(affectedNode.getParent());
		try {
			modifyAST(astRoot,affectedNode.getParent());
		} catch (JavaModelException | IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		// put all the dead code related problems in a list 
		ArrayList <IProblem> daedCodeProblems = new ArrayList<IProblem>();
		
		for (IProblem problem : problems) {
			if(problem.getMessage().equalsIgnoreCase("Dead Code")) {
				daedCodeProblems.add(problem);
			}
		}
		
		return daedCodeProblems;
	}
	
	public void generateCode(IPackageFragment documentationPackage) {
		
		// code generator
		CodeGenerator generator = new CodeGenerator(documentationPackage);
		generator.generateCode(this.icu);
		
	}
	
	private int [] getProblemLocation (IProblem problem) {
		int offset = problem.getSourceStart();
		int length = problem.getSourceEnd() + 1 - offset;
		int [] problemLocation = {offset, length};
		
		return problemLocation;
	}
	
	/**
	 * get the node where the dead code problem is located
	 * @param cu
	 * @param offset
	 * @param length
	 * @return
	 */
	private ASTNode getAffectedNode(CompilationUnit cu, int offset, int length) {
		NodeFinder finder= new NodeFinder(cu, offset, length);
		return finder.getCoveredNode();
	}
	
	/**
	 * removes the AST nodes and rewrite the AST back
	 * @param cu
	 * @param node
	 * @throws IllegalArgumentException 
	 * @throws JavaModelException 
	 */
	private void modifyAST (CompilationUnit cu, ASTNode node) throws JavaModelException, IllegalArgumentException {
		ASTRewrite rewriter = ASTRewrite.create(cu.getAST());
		rewriter.remove(node, null);
		this.icu.applyTextEdit(rewriter.rewriteAST(), new NullProgressMonitor());
	}
	
	
}

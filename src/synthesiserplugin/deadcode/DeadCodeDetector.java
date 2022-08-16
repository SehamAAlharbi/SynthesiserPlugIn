package synthesiserplugin.deadcode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.CorrectionEngine;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.internal.core.manipulation.dom.NecessaryParenthesesChecker;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.jdt.internal.ui.text.correction.AdvancedQuickAssistProcessor;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.text.correction.IProposalRelevance;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import synthesiserplugin.models.JavaProject;
import synthesiserplugin.parser.Parser;

@SuppressWarnings("restriction")
public class DeadCodeDetector {

	private ICompilationUnit icu;
	private CompilationUnit updatedCU;
	private JavaProject javaProject;
	private List<IProblem> deadCodeProblems;
	private Map<Integer, List<IJavaCompletionProposal>> ProblemCorrectionsMap;
	private ASTRewrite rewriter;

	public DeadCodeDetector(ICompilationUnit icu, JavaProject javaProject) {
		this.icu = icu;
		this.javaProject = javaProject;
		deadCodeProblems = new ArrayList<IProblem>();
		ProblemCorrectionsMap = new HashMap<Integer, List<IJavaCompletionProposal>>();

	}

	public DeadCodeDetector() {
	}

	public ICompilationUnit getIcu() {
		return icu;
	}

	public void setIcu(ICompilationUnit icu) {
		this.icu = icu;
	}

	public void detect() {

		CompilationUnit astRoot = getASTRoot(icu);
		IProblem[] problems = astRoot.getProblems();

		String message = "";
		for (IProblem problem : problems) {
			message = problem.getMessage();
			if (message.equalsIgnoreCase("Dead Code") || message.equalsIgnoreCase("Unreachable code")) {
				deadCodeProblems.add(problem);
			}
		}

		// Specify the correction proposals for each problem
		collectCorrectionProposals();

	}

	public void generateCleanCode(IPackageFragment documentationPackage) throws JavaModelException {
		// code generator
		CodeGenerator generator = new CodeGenerator(documentationPackage);

		if (!deadCodeProblems.isEmpty()) {
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
				IStatus status = JavaCorrectionProcessor.collectCorrections(context,
						new IProblemLocation[] { problemLocation }, CompletionProposals);
				ProblemCorrectionsMap.put(problem.getID(), CompletionProposals);

			});
		}
	}

	// This method assumes that there is only one dead code problem, but what if
	// there is more?
	private CUCorrectionProposal getRemaovalProposal(int problemID) {

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

	public void detectProblems(ICompilationUnit iCompilationUnit) {

		CompilationUnit cu = new Parser().parse(iCompilationUnit);
		IProblem[] problems = cu.getProblems();
		List<IProblem> deadCodeProblems = new ArrayList<IProblem>();
		
		String message = "";
		for (IProblem problem : problems) {
			message = problem.getMessage();
			if (message.equalsIgnoreCase("Dead Code") || message.equalsIgnoreCase("Unreachable code")) {
				deadCodeProblems.add(problem);
			}
		}
		
		if(deadCodeProblems.size()!=0) {

		deadCodeProblems.stream().forEach(problem -> {
			
			// get the problem offset and length
			int[] problemLocation = getProblemLocation(problem);

			// get context and ProblemLocation
			IInvocationContext context = new AssistContext(iCompilationUnit, problemLocation[0], problemLocation[1]);
			ProblemLocation location = new ProblemLocation(problem);

			
			try {
				// rewrite AST based on each dead/unreachable code problem
				modifyAST(context, location);
				// apply the removal changes on the ICU
				iCompilationUnit.applyTextEdit(rewriter.rewriteAST(), new NullProgressMonitor());
				// recursive call - to take the latest version if the ICU after removing the 1st dead/unreachable code problem
				detectProblems(iCompilationUnit);
				
			} catch (JavaModelException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();

			}
		});
		
		}

	}

	public void generateCode(IPackageFragment documentationPackage) {

		// code generator
		CodeGenerator generator = new CodeGenerator(documentationPackage);
		generator.generateCode(this.icu);

	}

	private int[] getProblemLocation(IProblem problem) {
		int offset = problem.getSourceStart();
		int length = problem.getSourceEnd() - offset + 1;
		int[] problemLocation = { offset, length };

		return problemLocation;
	}

	/**
	 * gets the node where the dead code problem is located
	 * 
	 * @param cu
	 * @param offset
	 * @param length
	 * @return
	 */
	private ASTNode getAffectedNode(CompilationUnit cu, int offset, int length) {
		NodeFinder finder = new NodeFinder(cu, offset, length);
		// If the AST contains nodes whose range is equal to the selection, returns the
		// innermost of those nodes.
		return finder.getCoveredNode();
	}

	/**
	 * rewrite the AST by calling a method that replace () a to-be-removed node with
	 * a an appropriate replacement node
	 * 
	 * @param cu
	 * @param node
	 * @throws IllegalArgumentException
	 * @throws JavaModelException
	 */
	private void modifyAST(IInvocationContext context, ProblemLocation problemLocation)
			throws JavaModelException, IllegalArgumentException {

		ArrayList<ICommandAccess> proposals = new ArrayList<ICommandAccess>();
		getUnreachableCodeRewrites(context, problemLocation, proposals);

	}

	/**
	 * finds all Java problem markers in a compilation unit.
	 * 
	 * @param cu
	 * @return
	 * @throws CoreException
	 */

	public IMarker[] findJavaProblemMarkers(ICompilationUnit cu) throws CoreException {
		IResource javaSourceFile = cu.getUnderlyingResource();
		IMarker[] markers = javaSourceFile.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true,
				IResource.DEPTH_INFINITE);

		return markers;
	}

	// -------------------- My Modification of the JDT dead code nodes removal - no
	// proposals just get the ASTRewrite --------------- //

	public void getUnreachableCodeRewrites(IInvocationContext context, IProblemLocation problem,
			Collection<ICommandAccess> proposals) {
		CompilationUnit root = context.getASTRoot();
		ASTNode selectedNode = problem.getCoveringNode(root);
		if (selectedNode == null) {
			return;
		}

		ASTNode parent = selectedNode.getParent();
		while (parent instanceof ExpressionStatement) {
			selectedNode = parent;
			parent = selectedNode.getParent();
		}

		if (parent instanceof WhileStatement) {
			addRemoveIncludingConditionRewrite(context, parent, null, proposals);

		} else if (selectedNode.getLocationInParent() == IfStatement.THEN_STATEMENT_PROPERTY) {
			Statement elseStatement = ((IfStatement) parent).getElseStatement();
			addRemoveIncludingConditionRewrite(context, parent, elseStatement, proposals);

		} else if (selectedNode.getLocationInParent() == IfStatement.ELSE_STATEMENT_PROPERTY) {
			Statement thenStatement = ((IfStatement) parent).getThenStatement();
			addRemoveIncludingConditionRewrite(context, parent, thenStatement, proposals);

		} else if (selectedNode.getLocationInParent() == ForStatement.BODY_PROPERTY) {
			Statement body = ((ForStatement) parent).getBody();
			addRemoveIncludingConditionRewrite(context, parent, body, proposals);

		} else if (selectedNode.getLocationInParent() == ConditionalExpression.THEN_EXPRESSION_PROPERTY) {
			Expression elseExpression = ((ConditionalExpression) parent).getElseExpression();
			addRemoveIncludingConditionRewrite(context, parent, elseExpression, proposals);

		} else if (selectedNode.getLocationInParent() == ConditionalExpression.ELSE_EXPRESSION_PROPERTY) {
			Expression thenExpression = ((ConditionalExpression) parent).getThenExpression();
			addRemoveIncludingConditionRewrite(context, parent, thenExpression, proposals);

		} else if (selectedNode.getLocationInParent() == InfixExpression.RIGHT_OPERAND_PROPERTY) {
			// also offer split && / || condition proposals:
			InfixExpression infixExpression = (InfixExpression) parent;
			Expression leftOperand = infixExpression.getLeftOperand();

			rewriter = ASTRewrite.create(parent.getAST());

			Expression replacement = ASTNodes.getUnparenthesedExpression(leftOperand);

			Expression toReplace = infixExpression;
			while (toReplace.getLocationInParent() == ParenthesizedExpression.EXPRESSION_PROPERTY) {
				toReplace = (Expression) toReplace.getParent();
			}

			if (NecessaryParenthesesChecker.needsParentheses(replacement, toReplace.getParent(),
					toReplace.getLocationInParent())) {
				if (leftOperand instanceof ParenthesizedExpression) {
					replacement = (Expression) replacement.getParent();
				} else if (infixExpression.getLocationInParent() == ParenthesizedExpression.EXPRESSION_PROPERTY) {
					toReplace = ((ParenthesizedExpression) toReplace).getExpression();
				}
			}

			rewriter.replace(toReplace, rewriter.createMoveTarget(replacement), null);

			String label = CorrectionMessages.LocalCorrectionsSubProcessor_removeunreachablecode_description;
			addRemoveRewrite(context, rewriter, label, proposals);

			AssistContext assistContext = new AssistContext(context.getCompilationUnit(),
					infixExpression.getRightOperand().getStartPosition() - 1, 0);
			assistContext.setASTRoot(root);
			AdvancedQuickAssistProcessor.getSplitAndConditionProposals(assistContext, infixExpression, proposals);
			AdvancedQuickAssistProcessor.getSplitOrConditionProposals(assistContext, infixExpression, proposals);

		} else if (selectedNode instanceof Statement && selectedNode.getLocationInParent().isChildListProperty()) {
			// remove all statements following the unreachable:
			List<Statement> statements = ASTNodes.<Statement>getChildListProperty(selectedNode.getParent(),
					(ChildListPropertyDescriptor) selectedNode.getLocationInParent());
			int idx = statements.indexOf(selectedNode);

			rewriter = ASTRewrite.create(selectedNode.getAST());
			String label = CorrectionMessages.LocalCorrectionsSubProcessor_removeunreachablecode_description;

			if (idx > 0) {
				Object prevStatement = statements.get(idx - 1);
				if (prevStatement instanceof IfStatement) {
					IfStatement ifStatement = (IfStatement) prevStatement;
					if (ifStatement.getElseStatement() == null) {
						// remove if (true), see https://bugs.eclipse.org/bugs/show_bug.cgi?id=261519
						Statement thenStatement = ifStatement.getThenStatement();
						label = CorrectionMessages.LocalCorrectionsSubProcessor_removeunreachablecode_including_condition_description;
						if (thenStatement instanceof Block) {
							// add all child nodes from Block node
							List<Statement> thenStatements = ((Block) thenStatement).statements();
							if (thenStatements.isEmpty()) {
								return;
							}
							ASTNode[] thenStatementsArray = new ASTNode[thenStatements.size()];
							for (int i = 0; i < thenStatementsArray.length; i++) {
								thenStatementsArray[i] = thenStatements.get(i);
							}
							ASTNode newThenStatement = rewriter.createGroupNode(thenStatementsArray);

							rewriter.replace(ifStatement, newThenStatement, null);
						} else {
							rewriter.replace(ifStatement, thenStatement, null);
						}
					}
				}
			}

			for (int i = idx; i < statements.size(); i++) {
				ASTNode statement = statements.get(i);
				if (statement instanceof SwitchCase)
					break; // stop at case *: and default:
				rewriter.remove(statement, null);
			}

			addRemoveRewrite(context, rewriter, label, proposals);

		} else {
			// no special case, just remove the node:
			addRemoveRewrite(context, selectedNode, proposals);
		}
	}

	private void addRemoveRewrite(IInvocationContext context, ASTNode selectedNode,
			Collection<ICommandAccess> proposals) {
		rewriter = ASTRewrite.create(selectedNode.getAST());
		rewriter.remove(selectedNode, null);

		String label = CorrectionMessages.LocalCorrectionsSubProcessor_removeunreachablecode_description;
		addRemoveRewrite(context, rewriter, label, proposals);
	}

	private void addRemoveIncludingConditionRewrite(IInvocationContext context, ASTNode toRemove,
			ASTNode replacement, Collection<ICommandAccess> proposals) {
		Image image = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
		String label = CorrectionMessages.LocalCorrectionsSubProcessor_removeunreachablecode_including_condition_description;
		AST ast = toRemove.getAST();
//		ASTRewrite rewrite= ASTRewrite.create(ast);
		rewriter = ASTRewrite.create(ast);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewriter, IProposalRelevance.REMOVE_UNREACHABLE_CODE_INCLUDING_CONDITION, image);

		if (replacement == null || replacement instanceof EmptyStatement
				|| replacement instanceof Block && ((Block) replacement).statements().size() == 0) {
			if (ASTNodes.isControlStatementBody(toRemove.getLocationInParent())) {
				rewriter.replace(toRemove, toRemove.getAST().newBlock(), null);
			} else {
				rewriter.remove(toRemove, null);
			}

		} else if (toRemove instanceof Expression && replacement instanceof Expression) {
			Expression moved = (Expression) rewriter.createMoveTarget(replacement);
			Expression toRemoveExpression = (Expression) toRemove;
			Expression replacementExpression = (Expression) replacement;
			ITypeBinding explicitCast = ASTNodes.getExplicitCast(replacementExpression, toRemoveExpression);
			if (explicitCast != null) {
				CastExpression cast = ast.newCastExpression();
				if (NecessaryParenthesesChecker.needsParentheses(replacementExpression, cast,
						CastExpression.EXPRESSION_PROPERTY)) {
					ParenthesizedExpression parenthesized = ast.newParenthesizedExpression();
					parenthesized.setExpression(moved);
					moved = parenthesized;
				}
				cast.setExpression(moved);
				ImportRewrite imports = proposal.createImportRewrite(context.getASTRoot());
				ImportRewriteContext importRewriteContext = new ContextSensitiveImportRewriteContext(toRemove, imports);
				cast.setType(imports.addImport(explicitCast, ast, importRewriteContext, TypeLocation.CAST));
				moved = cast;
			}
			rewriter.replace(toRemove, moved, null);

		} else {
			ASTNode parent = toRemove.getParent();
			ASTNode moveTarget;
			if ((parent instanceof Block || parent instanceof SwitchStatement) && replacement instanceof Block) {
				ListRewrite listRewrite = rewriter.getListRewrite(replacement, Block.STATEMENTS_PROPERTY);
				List<Statement> list = ((Block) replacement).statements();
				int lastIndex = list.size() - 1;
				moveTarget = listRewrite.createMoveTarget(list.get(0), list.get(lastIndex));
			} else {
				moveTarget = rewriter.createMoveTarget(replacement);
			}

			rewriter.replace(toRemove, moveTarget, null);
		}

		proposals.add(proposal);
	}

	private static void addRemoveRewrite(IInvocationContext context, ASTRewrite rewrite, String label,
			Collection<ICommandAccess> proposals) {
		Image image = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 10, image);
		proposals.add(proposal);
	}

}
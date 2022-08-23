package synthesiserplugin.deadcode;

import java.util.ArrayList;	
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
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
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.core.manipulation.dom.NecessaryParenthesesChecker;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import synthesiserplugin.cleancode.CodeGenerator;
import synthesiserplugin.parser.Parser;


public class DeadCodeDetector {

	private ICompilationUnit icu;
	private ASTRewrite rewriter;

	public DeadCodeDetector(ICompilationUnit icu) {
		this.icu = icu;
	}

	public ICompilationUnit getIcu() {
		return icu;
	}

	public void setIcu(ICompilationUnit icu) {
		this.icu = icu;
	}

	public ASTRewrite getRewriter() {
		return rewriter;
	}

	public void setRewriter(ASTRewrite rewriter) {
		this.rewriter = rewriter;
	}
	
	public void detectProblems() {
		detectProblems(this.icu);
	}

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
				// rewrite AST based on the location of the node causing the each dead/unreachable code problem
				modifyAST(context, location);
				// apply the removal changes on the ICU
				iCompilationUnit.applyTextEdit(rewriter.rewriteAST(), new NullProgressMonitor());
				// recursive call - to take the latest version of the ICU after removing the 1st dead/unreachable code problem
				detectProblems(iCompilationUnit);
				
			} catch (JavaModelException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();

			}
		});
		
		}

	}

	/**
	 * generate clean code from the transformed ICU
	 * @param documentationPackage
	 */
	public void generateCode(IPackageFragment documentationPackage) {
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

		getUnreachableCodeRewrites(context, problemLocation);

	}

	// ---- My replication and edits of the JDT dead code nodes detection and replacement: LocalCorrectionsSubProcessor.java line 1491 ---- //

	public void getUnreachableCodeRewrites(IInvocationContext context, IProblemLocation problem) {
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
			addRemoveIncludingConditionRewrite(context, parent, null);

		} else if (selectedNode.getLocationInParent() == IfStatement.THEN_STATEMENT_PROPERTY) {
			Statement elseStatement = ((IfStatement) parent).getElseStatement();
			addRemoveIncludingConditionRewrite(context, parent, elseStatement);

		} else if (selectedNode.getLocationInParent() == IfStatement.ELSE_STATEMENT_PROPERTY) {
			Statement thenStatement = ((IfStatement) parent).getThenStatement();
			addRemoveIncludingConditionRewrite(context, parent, thenStatement);

		} else if (selectedNode.getLocationInParent() == ForStatement.BODY_PROPERTY) {
			Statement body = ((ForStatement) parent).getBody();
			addRemoveIncludingConditionRewrite(context, parent, body);

		} else if (selectedNode.getLocationInParent() == ConditionalExpression.THEN_EXPRESSION_PROPERTY) {
			Expression elseExpression = ((ConditionalExpression) parent).getElseExpression();
			addRemoveIncludingConditionRewrite(context, parent, elseExpression);

		} else if (selectedNode.getLocationInParent() == ConditionalExpression.ELSE_EXPRESSION_PROPERTY) {
			Expression thenExpression = ((ConditionalExpression) parent).getThenExpression();
			addRemoveIncludingConditionRewrite(context, parent, thenExpression);

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

			

			AssistContext assistContext = new AssistContext(context.getCompilationUnit(),
					infixExpression.getRightOperand().getStartPosition() - 1, 0);
			assistContext.setASTRoot(root);

		} else if (selectedNode instanceof Statement && selectedNode.getLocationInParent().isChildListProperty()) {
			// remove all statements following the unreachable:
			List<Statement> statements = ASTNodes.<Statement>getChildListProperty(selectedNode.getParent(),
					(ChildListPropertyDescriptor) selectedNode.getLocationInParent());
			int idx = statements.indexOf(selectedNode);

			rewriter = ASTRewrite.create(selectedNode.getAST());
			

			if (idx > 0) {
				Object prevStatement = statements.get(idx - 1);
				if (prevStatement instanceof IfStatement) {
					IfStatement ifStatement = (IfStatement) prevStatement;
					if (ifStatement.getElseStatement() == null) {
						// remove if (true), see https://bugs.eclipse.org/bugs/show_bug.cgi?id=261519
						Statement thenStatement = ifStatement.getThenStatement();
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

		} else {
			// no special case, just remove the node:
			addRemoveRewrite(context, selectedNode);
		}
	}

	private void addRemoveRewrite(IInvocationContext context, ASTNode selectedNode) {
		rewriter = ASTRewrite.create(selectedNode.getAST());
		rewriter.remove(selectedNode, null);
	}

	private void addRemoveIncludingConditionRewrite(IInvocationContext context, ASTNode toRemove,
			ASTNode replacement) {
		AST ast = toRemove.getAST();
		rewriter = ASTRewrite.create(ast);

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
	}
}
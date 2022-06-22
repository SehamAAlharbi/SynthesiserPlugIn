package synthesiserplugin.models;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import synthesiserplugin.parser.Parser;

public class JavaProject {

	private String projectName;
	private IPackageFragmentRoot[] folders;
	private ArrayList<IPackageFragment> packages;
	private ArrayList<ICompilationUnit> iCompilationUnits;
	private ArrayList<CompilationUnit> compilationUnits;
	private IPackageFragmentRoot sourceFolder;
	private IPackageFragment documentationPackage;

	// constructor to create a JavaProject from the JDT Java Model IJavaProject
	public JavaProject(IJavaProject javaProject) throws JavaModelException {

		this.projectName = javaProject.getElementName();
		this.folders = javaProject.getAllPackageFragmentRoots();
		this.packages = new ArrayList<IPackageFragment>();
		this.iCompilationUnits = new ArrayList<ICompilationUnit>();

		// only source packages are considered
		IPackageFragment[] projectPackages = javaProject.getPackageFragments();
		for (IPackageFragment projectPackage : projectPackages) {
			if (projectPackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
				this.packages.add(projectPackage);
				for (ICompilationUnit icu : projectPackage.getCompilationUnits()) {
					this.iCompilationUnits.add(icu);
				}
			}
		}
		
		// source folder 
		for (IPackageFragmentRoot packageFragmentRoot : javaProject.getPackageFragmentRoots()) {
			if (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE) {
				this.sourceFolder = packageFragmentRoot;
			}
		}
		
		// a package for documentation examples
		this.documentationPackage = this.sourceFolder.createPackageFragment("documentation.usage.examples", false, null);

		// parsed versions of all the iCompilationUnit (s)
		Parser parser = new Parser();
		this.compilationUnits = new ArrayList<CompilationUnit>();
		for (ICompilationUnit icu : this.iCompilationUnits) {
			// Now create the AST for the ICompilationUnit (s)
			CompilationUnit cu = parser.parse(icu);
			this.compilationUnits.add(cu);
		}
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public IPackageFragmentRoot[] getFolders() {
		return folders;
	}

	public void setFolders(IPackageFragmentRoot[] folders) {
		this.folders = folders;
	}

	public ArrayList<IPackageFragment> getPackages() {
		return packages;
	}

	public void setPackages(ArrayList<IPackageFragment> packages) {
		this.packages = packages;
	}

	public ArrayList<ICompilationUnit> getiCompilationUnits() {
		return iCompilationUnits;
	}

	public void setiCompilationUnits(ArrayList<ICompilationUnit> iCompilationUnits) {
		this.iCompilationUnits = iCompilationUnits;
	}

	public ArrayList<CompilationUnit> getCompilationUnits() {
		return compilationUnits;
	}

	public void setCompilationUnits(ArrayList<CompilationUnit> compilationUnits) {
		this.compilationUnits = compilationUnits;
	}
	
	public IPackageFragmentRoot getSourceFolder() {
		return sourceFolder;
	}

	public void setSourceFolder(IPackageFragmentRoot sourceFolder) {
		this.sourceFolder = sourceFolder;
	}

	public IPackageFragment getDocumentationPackage() {
		return documentationPackage;
	}

	public void setDocumentationPackage(IPackageFragment documentationPackage) {
		this.documentationPackage = documentationPackage;
	}

	/**
	 * 
	 * @param name should be in full e.g., name.java
	 * @return the CompilationUnit with the same @param name
	 */
	public CompilationUnit getCUByName (String name) {
		CompilationUnit cu = this.compilationUnits.stream().filter(cUnit -> cUnit.getJavaElement().getElementName().equalsIgnoreCase(name)).findFirst().orElse(null);
		return cu;
	}
	
	/**
	 * 
	 * @param name should be in full e.g., name.java
	 * @return the ICompilationUnit with the same @param name
	 */
	public ICompilationUnit getICUByName(String name) {
		ICompilationUnit cu = this.iCompilationUnits.stream().filter(cUnit -> cUnit.getElementName().equalsIgnoreCase(name)).findFirst().orElse(null);
		return cu;
	}

	@Override
	public String toString() {
		return "JavaProject [projectName=" + projectName + ", folders=" + Arrays.toString(folders) + ", packages="
				+ packages + ", iCompilationUnits=" + iCompilationUnits + ", compilationUnits="
				+ compilationUnits + "]";
	}

}

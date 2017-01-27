package edu.uta.futureye.lib.assembler;

import edu.uta.futureye.algebra.SparseMatrixRowMajor;
import edu.uta.futureye.algebra.SparseVectorHashMap;
import edu.uta.futureye.algebra.intf.Matrix;
import edu.uta.futureye.algebra.intf.Vector;
import edu.uta.futureye.core.DOF;
import edu.uta.futureye.core.DOFOrder;
import edu.uta.futureye.core.Element;
import edu.uta.futureye.core.Mesh;
import edu.uta.futureye.function.operator.FOIntegrate;
import edu.uta.futureye.lib.weakform.WeakForm;
import edu.uta.futureye.util.container.DOFList;
import edu.uta.futureye.util.container.ElementList;

public class BasicAssembler {
	public WeakForm weakForm;
	public double[][] A; // domain local stiff matrix
	public double[] b;   // domain local load vector
	double[] params;
	int nDOFs;
	
	Matrix gA; // global stiff matrix
	Vector gb; // global load vector

	public BasicAssembler(WeakForm weakForm) {
		this.weakForm = weakForm;
		nDOFs = weakForm.getFiniteElement().getNumberOfDOFs();
		A = new double[nDOFs][nDOFs];
		b = new double[nDOFs];
		params = new double[weakForm.getFiniteElement().getArgsOrder().length];
	}
	
	/**
	 * Assemble local stiff matrix and load vector on a give element
	 * @param e
	 */
	public void assembleLocal(Element e) {
		e.adjustVerticeToCounterClockwise();

		double[] coords = e.getNodeCoords();
		System.arraycopy(coords, 0, params, 0, coords.length);

		weakForm.getCompiledJac().apply(params);

		if(weakForm.getFiniteElement().getNumberOfDOFs() == 2) {
		for(int j=0;j<nDOFs;j++) {
			for(int i=0;i<nDOFs;i++) {
				A[j][i] = FOIntegrate.intOnLinearRefElement(weakForm.getCompiledLHS()[j][i], 
						params, coords.length, 5);
			}
			b[j] = FOIntegrate.intOnLinearRefElement(weakForm.getCompiledRHS()[j], 
					params, coords.length, 5);
		}
		} else if(weakForm.getFiniteElement().getNumberOfDOFs() == 3) {
			for(int j=0; j<nDOFs; j++) {
				for(int i=0; i<nDOFs; i++) {
					A[j][i] = FOIntegrate.intOnTriangleRefElement(weakForm.getCompiledLHS()[j][i], 
							params, coords.length, 2);//Laplace Test: 2=80.839 3=80.966, 4=80.967
				}
				b[j] = FOIntegrate.intOnTriangleRefElement(weakForm.getCompiledRHS()[j], 
						params, coords.length, 2);
			}
		} else if(weakForm.getFiniteElement().getNumberOfDOFs() == 4) {
			for(int j=0; j<nDOFs; j++) {
				for(int i=0; i<nDOFs; i++) {
					A[j][i] = FOIntegrate.intOnRectangleRefElement(weakForm.getCompiledLHS()[j][i], 
							params, coords.length, 5);
				}
				b[j] = FOIntegrate.intOnRectangleRefElement(weakForm.getCompiledRHS()[j], 
						params, coords.length, 5);
			}
		}
	}
	
	/**
	 * Assemble global stiff matrix and load vector on a given mesh
	 * new matrix and vector are allocated. Use <tt>getGlobalStiffMatrix()</tt> and
	 * <tt>getGlobalLoadVector()</tt> to access them.
	 * 
	 * @param mesh
	 */
	public void assembleGlobal(Mesh mesh) {
		int dim = mesh.getNodeList().size();
		gA = new SparseMatrixRowMajor(dim,dim);
		gb = new SparseVectorHashMap(dim);
		assembleGlobal(mesh, gA, gb);
	}
	
	/**
	 * Assemble stiff matrix and load vector on a given mesh
	 * into parameter stiff and load.
	 * 
	 * Several assemblers can be chained by using this method
	 * to assemble stiff matrix and load vector
	 * 
	 * @param mesh
	 * @param stiff
	 * @param load
	 */
	public void assembleGlobal(Mesh mesh, Matrix stiff, Vector load) {
		ElementList eList = mesh.getElementList();
		for(Element e : eList) {
			assembleLocal(e);
			
			// Associate FiniteElement object with Element object
			this.weakForm.getFiniteElement().assignTo(e);

			DOFList DOFs = e.getAllDOFList(DOFOrder.NEFV);
			for(int j=0;j<nDOFs;j++) {
				DOF dofI = DOFs.at(j+1);
				int nGlobalRow = dofI.getGlobalIndex();
				for(int i=0;i<nDOFs;i++) {
					DOF dofJ = DOFs.at(i+1);
					int nGlobalCol = dofJ.getGlobalIndex();
					stiff.add(nGlobalRow, nGlobalCol, A[j][i]);
				}
				//Local load vector
				load.add(nGlobalRow, b[j]);
			}
		}
		//update gA and gb
		this.gA = stiff;
		this.gb = load;
	}
	
	public double[][] getLocalStiffMatrix() {
		return A;
	}
	
	public double[] getLocalLoadVector() {
		return b;
	}

	public Matrix getGlobalStiffMatrix() {
		return gA;
	}

	public Vector getGlobalLoadVector() {
		return gb;
	}
}

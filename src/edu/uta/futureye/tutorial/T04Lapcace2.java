package edu.uta.futureye.tutorial;

import static edu.uta.futureye.function.FMath.C0;
import static edu.uta.futureye.function.FMath.C1;
import static edu.uta.futureye.function.FMath.x;
import static edu.uta.futureye.function.FMath.y;

import java.util.HashMap;

import edu.uta.futureye.algebra.intf.Matrix;
import edu.uta.futureye.algebra.intf.Vector;
import edu.uta.futureye.algebra.solver.external.SolverJBLAS;
import edu.uta.futureye.core.Mesh;
import edu.uta.futureye.core.NodeType;
import edu.uta.futureye.core.intf.AssemblerOld;
import edu.uta.futureye.function.MultiVarFunc;
import edu.uta.futureye.function.intf.MathFunc;
import edu.uta.futureye.io.MeshReader;
import edu.uta.futureye.io.MeshWriter;
import edu.uta.futureye.lib.assembler.AssemblerScalar;
import edu.uta.futureye.lib.element.FELinearTriangleOld;
import edu.uta.futureye.lib.weakform.WeakFormLaplace2D;
import edu.uta.futureye.util.container.ElementList;

/**
 * <blockquote><pre>
 * Problem:
 *   -k*\Delta{u} + c*u*v = f in \Omega
 *   u=0,                     on \Gamma_D
 *   d*u + k*u_n = g          on \Gamma_N
 * where
 *   \Omega = [-3,3]*[-3,3]
 *   \Gamma_N = Right boundary
 *   \Gamma_D = Other boundary
 *   k = 1.0
 *   c = sqrt(x^2+y^2)
 *   f = -2.0*(x^2+y^2)+36.0
 *   g = 1.0
 *   d = 1.0
 * </blockquote></pre>
 * 
 * @author liuyueming
 */
public class T04Lapcace2 {
	public static void triangleTest() {
		String meshName = "triangle";
		MeshReader reader = new MeshReader("grids/"+meshName+".grd");
		Mesh mesh = reader.read2DMesh();
		mesh.computeNodeBelongsToElements();
		
		HashMap<NodeType, MathFunc> mapNTF = new HashMap<NodeType, MathFunc>();
//		mapNTF.put(NodeType.Robin, new AbstractMathFunc("x","y"){
//			@Override
//			public double apply(double... args) {
//				if(3.0-args[0]<0.01)
//					return 1.0;
//				else
//					return -1.0;
//			}
//		});
		mapNTF.put(NodeType.Dirichlet, null);		
		mesh.markBorderNode(mapNTF);

		//Asign degree of freedom to element
		//Use element library
		ElementList eList = mesh.getElementList();
		FELinearTriangleOld linearTriangle = new FELinearTriangleOld();
		for(int i=1;i<=eList.size();i++)
			linearTriangle.assignTo(eList.at(i));
		
		/**
		 *User defined weak form of PDE (including bounder conditions)
		 *Problem
		 *  -\Delta{u} = f, (x,y) in [-3,3]*[-3,3]
		 *  u(x,y)=0, (x,y) in \partial{\Omega}
		 *  
		 *Where
		 *  f=-2*(x^2+y^2)+36
		 *
		 */
//		WeakFormLaplace2D weakForm = new WeakFormLaplace2D();
//		weakForm.setF(FC.c(-2.0).M(
//						X.M(X).A(Y.M(Y))
//					).A(FC.c(36.0))
//				);
//		//d*u + k*u_n= q
//		weakForm.setParam(C1, sqrt(X.M(X).A(Y.M(Y))), C1, C1);
		
        //4.Weak form
        WeakFormLaplace2D weakForm = new WeakFormLaplace2D();
        //Right hand side(RHS): f = -2*(x^2+y^2)+36
        weakForm.setF(-2*(x*x+y*y)+36);
        weakForm.setParam(C1, C0, C0, C0);
		
		AssemblerOld assembler = new AssemblerScalar(mesh, weakForm);
		System.out.println("Begin Assemble...");
		long begin = System.currentTimeMillis();
		assembler.assemble();
		Matrix stiff = assembler.getStiffnessMatrix();
		//stiff.print();
		Vector load = assembler.getLoadVector();
		//load.print();
		assembler.imposeDirichletCondition(C0);
		long end = System.currentTimeMillis();
		System.out.println("Assemble done!");
		System.out.println("Time used:"+(end-begin));
		
		SolverJBLAS solver = new SolverJBLAS();
		Vector u = solver.solveDGESV(stiff, load);
		
	    System.out.println("u=");
	    for(int i=1;i<=u.getDim();i++)
	        System.out.println(String.format("%.3f", u.get(i)));	
	    
	    MeshWriter writer = new MeshWriter(mesh);
	    writer.writeTechplot(meshName+"_out.dat", u);
		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		triangleTest();

	}

}

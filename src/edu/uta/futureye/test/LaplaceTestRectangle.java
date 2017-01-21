package edu.uta.futureye.test;

import static edu.uta.futureye.function.FMath.C0;
import static edu.uta.futureye.function.FMath.grad;
import static edu.uta.futureye.function.FMath.x;
import static edu.uta.futureye.function.FMath.y;

import java.util.HashMap;

import edu.uta.futureye.algebra.intf.Matrix;
import edu.uta.futureye.algebra.intf.Vector;
import edu.uta.futureye.algebra.solver.external.SolverJBLAS;
import edu.uta.futureye.core.Element;
import edu.uta.futureye.core.Mesh;
import edu.uta.futureye.core.NodeType;
import edu.uta.futureye.function.MultiVarFunc;
import edu.uta.futureye.function.intf.MathFunc;
import edu.uta.futureye.function.intf.ScalarShapeFunction;
import edu.uta.futureye.io.MeshReader;
import edu.uta.futureye.io.MeshWriter;
import edu.uta.futureye.lib.assembler.Assembler;
import edu.uta.futureye.lib.element.FEBilinearRectangle;
import edu.uta.futureye.lib.element.FELinearTriangle;
import edu.uta.futureye.lib.weakform.WeakForm;
import edu.uta.futureye.lib.weakform.WeakFormBuilder.Type;
import edu.uta.futureye.util.Utils;

/**
 * <blockquote><pre>
 * Solve
 *   -k*\Delta{u} = f  in \Omega
 *   u(x,y) = 0,       on boundary x=3.0 of \Omega
 *   u_n + u = 0.01,   on other boundary of \Omega
 * where
 *   \Omega = [-3,3]*[-3,3]
 *   k = 2
 *   f = -4*(x^2+y^2)+72
 *   u_n = \frac{\partial{u}}{\partial{n}}
 *   n: outer unit normal of \Omega
 * </blockquote></pre>
 */

public class LaplaceTestRectangle {
	public void run() {
		// 1.Read mesh
		MeshReader reader = new MeshReader("grids/rectangle.grd");
		Mesh mesh = reader.read2DMesh();
		// Compute geometry relationship between nodes and elements
		mesh.computeNodeBelongsToElements();

		// 2.Mark border types
		HashMap<NodeType, MathFunc> mapNTF = new HashMap<NodeType, MathFunc>();
		//Robin type on boundary x=3.0 of \Omega
		mapNTF.put(NodeType.Robin, new MultiVarFunc("Robin", "x","y"){
			@Override
			public double apply(double... args) {
				if(3.0-args[this.argIdx[0]] < 0.01)
					return 1.0; //this is Robin condition
				else
					return -1.0;
			}
		});
		//Dirichlet type on other boundary of \Omega
		mapNTF.put(NodeType.Dirichlet, null);
		mesh.markBorderNode(mapNTF);

		// 3.Use finite element library to assign degrees of
		// freedom (DOF) to element
		FEBilinearRectangle fet = new FEBilinearRectangle();
		for(Element e : mesh.getElementList())
			fet.assignTo(e);

//		public MathFunc makeExpression(Element e, Type type) {
//			ScalarShapeFunction u = getScalarTrial();
//			ScalarShapeFunction v = getScalarTest();
//			//Call param() to get parameters, do NOT define functions here 
//			//except for constant functions (or class FC). Because functions 
//			//will be transformed to local coordinate system by param()
//			MathFunc fk = getParam("k",e);
//			MathFunc ff = getParam("f",e);
//			switch(type) {
//				case LHS_Domain:
//					// k*(u_x*v_x + u_y*v_y) in \Omega
//					return fk.M(grad(u, "x","y").dot(grad(v, "x","y")));
//					//return fk.M( u._d("x").M(v._d("x")) .A (u._d("y").M(v._d("y"))) );
//				case LHS_Border:
//					// k*u*v on Robin boundary
//					return fk.M(u.M(v));
//				case RHS_Domain:
//					return ff.M(v);
//				case RHS_Border:
//					return v.M(0.01);
//				default:
//					return null;
//			}
//		}
		//4. Weak form
		//Right hand side(RHS):
		final MathFunc f = - 4*(x*x + y*y) + 72;
		WeakForm wf = new WeakForm(
				fet, 
				(u,v) -> 2*grad(u, "x", "y").dot(grad(v, "x", "y")), 
				v -> f * v
			);
		wf.compile();

		// 5.Assembly process
		Assembler assembler = new Assembler(wf);
		assembler.assembleGlobal(mesh);
		Matrix stiff = assembler.getGlobalStiffMatrix();
		Vector load = assembler.getGlobalLoadVector();
		// Boundary condition
		Utils.imposeDirichletCondition(stiff, load, mesh, C0);

		// 6.Solve linear system
		SolverJBLAS solver = new SolverJBLAS();
		Vector u = solver.solveDGESV(stiff, load);
		System.out.println("u=");
		for (int i = 1; i <= u.getDim(); i++)
			System.out.println(String.format("%.3f ", u.get(i)));

		// 7.Output results to an Techplot format file
		MeshWriter writer = new MeshWriter(mesh);
		writer.writeTechplot("./tutorial/Laplace2DRectangle.dat", u);
	}

	public static void main(String[] args) {
		LaplaceTestRectangle ex1 = new LaplaceTestRectangle();
		ex1.run();
	}
}
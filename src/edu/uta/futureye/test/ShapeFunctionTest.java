package edu.uta.futureye.test;

import java.util.ArrayList;
import java.util.List;

import edu.uta.futureye.core.CoordinateTransform;
import edu.uta.futureye.core.Element;
import edu.uta.futureye.core.Node;
import edu.uta.futureye.function.Variable;
import edu.uta.futureye.function.intf.MathFunc;
import edu.uta.futureye.function.intf.ScalarShapeFunction;
import edu.uta.futureye.lib.shapefun.SFBilinearLocal2D;
import edu.uta.futureye.lib.shapefun.SFLinearLocal2D;
import edu.uta.futureye.lib.shapefun.SFLinearLocal2DRS;
import edu.uta.futureye.util.container.NodeList;

public class ShapeFunctionTest {
	public static double eps = 1e-5;
	public static void check(String info, double d1, double d2) {
		if(Math.abs(d1-d2) < eps) {
			System.out.println("pass");
		} else {
			System.out.println("!!!FAIL!!!   "+d1+"!="+d2+" "+info);
		}
	}
	
	public static void check(String a, String b) {
		if(a.equals(b))
			System.out.println("pass");
		else 
			System.out.println("!!!FAIL!!!   " + a + " != " + b);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		testSFLinearLocal2D();
		testSFBilinearLocal2D();
		testShapeFunctionPrint();

	}
	
	public static void testSFLinearLocal2D() {
		NodeList nodes = new NodeList();
		nodes.add(new Node(1, 0.0,0.0));
		nodes.add(new Node(2, 0.2,0.0));
		nodes.add(new Node(3, 0.0,0.2));
		Element e = new Element(nodes);
		
		System.out.println("Test coordinate transform and Jacbian on element "+e);
		CoordinateTransform trans = new CoordinateTransform(2);
		trans.transformLinear2D(e);
		trans.computeJacobianMatrix();
		trans.computeJacobian2D();
		MathFunc jac = trans.getJacobian();
		Variable v = new Variable();
		v.set("r", 0);
		v.set("s", 0);
		check(jac.toString(), jac.apply(v), 0.04);
		
		ScalarShapeFunction[] shapeFun = new ScalarShapeFunction[3];
		shapeFun[0] = new SFLinearLocal2DRS(1);
		shapeFun[1] = new SFLinearLocal2DRS(2);
		shapeFun[2] = new SFLinearLocal2DRS(3);
		
		System.out.println("Test the derivatives of shape function SFLinearLocal2DRS:");
		double[] rlt = new double[]{-5, -5, 5, 0, 0, 5};
		int j = 0;
		for(int i=0; i<3; i++) {
			System.out.println(shapeFun[i]);
			shapeFun[i].assignElement(e);
			MathFunc SFdx = shapeFun[i].diff("x");
			MathFunc SFdy = shapeFun[i].diff("y");
			check("shapeFun["+i+"].diff(\"x\")", SFdx.apply(), rlt[j++]);
			check("shapeFun["+i+"].diff(\"y\")", SFdy.apply(), rlt[j++]);
		}

		System.out.println("Test the derivatives of shape function SFLinearLocal2D:");
		shapeFun[0] = new SFLinearLocal2D(1);
		shapeFun[1] = new SFLinearLocal2D(2);
		shapeFun[2] = new SFLinearLocal2D(3);
		j = 0;
		for(int i=0; i<3; i++) {
			System.out.println(shapeFun[i]);
			shapeFun[i].assignElement(e);
			MathFunc SFdx = shapeFun[i].diff("x");
			MathFunc SFdy = shapeFun[i].diff("y");
			check("shapeFun["+i+"].diff(\"x\")="+SFdy, SFdx.apply(v), rlt[j++]);
			check("shapeFun["+i+"].diff(\"y\")="+SFdy, SFdy.apply(v), rlt[j++]);
		}
	}
	
	public static void testSFBilinearLocal2D() {
		NodeList nodes = new NodeList();
		nodes.add(new Node(1, -2.0,-2.0));
		nodes.add(new Node(2, 2.0,-2.0));
		nodes.add(new Node(3, 2.0,2.0));
		nodes.add(new Node(4, -2.0,2.0));
		Element e = new Element(nodes);

		System.out.println("Test coordinate transform and Jacbian on element "+e);
		CoordinateTransform trans = new CoordinateTransform(2);
		trans.transformLinear2D(e);
		trans.computeJacobianMatrix();
		trans.computeJacobian2D();
		MathFunc jac = trans.getJacobian();
		Variable v = new Variable();
		v.set("r", 0);
		v.set("s", 0);
		check(jac.toString(), jac.apply(v), 4.0);

		
		ScalarShapeFunction[] shapeFun = new ScalarShapeFunction[4];
		shapeFun[0] = new SFBilinearLocal2D(1);
		shapeFun[1] = new SFBilinearLocal2D(2);
		shapeFun[2] = new SFBilinearLocal2D(3);
		shapeFun[3] = new SFBilinearLocal2D(4);
		
		System.out.println("Test the evaluation of SFBilinearLocal2D:");
		double[] args = new double[]{0.1, 0.1};
		Variable v0 = new Variable();
		v0.set("r", args[0]);
		v0.set("s", args[1]);
		System.out.println(shapeFun[0]);
		check(shapeFun[0].toString(), shapeFun[0].apply(v0), 0.2025);
		check(shapeFun[0].toString(), shapeFun[0].apply(args), 0.2025);
		check(shapeFun[0].toString(), shapeFun[0].compile().apply(args), 0.2025);
		
		System.out.println("Test the derivatives of shape function SFBilinearLocal2D:");
		e.updateJacobin();
		double[] rlt = new double[]{-0.125, -0.125, 0.125, -0.125, 0.125, 0.125, -0.125, 0.125};
		int j = 0;
		for(int i=0; i<4; i++) {
			System.out.println(shapeFun[i]);
			shapeFun[i].assignElement(e);
			MathFunc SFdx = shapeFun[i].diff("x");
			MathFunc SFdy = shapeFun[i].diff("y");
			check("shapeFun["+i+"].diff(\"x\")="+SFdy, SFdx.apply(v), rlt[j++]);
			check("shapeFun["+i+"].diff(\"y\")="+SFdy, SFdy.apply(v), rlt[j++]);
		}
	}
	
	public static void testShapeFunctionPrint() {
		ScalarShapeFunction N1,N2,N3,N4;
		N1 = new SFLinearLocal2DRS(1);
		N2 = new SFLinearLocal2DRS(2);
		N3 = new SFLinearLocal2DRS(3);
		check(N1.toString(), "N1(r,s) = r");
		check(N2.toString(), "N2(r,s) = s");
		check(N3.toString(), "N3(r,s) = 1.0 - r - s");
		check((N1+1).toString(), "f(r,s) = N1(r,s) + 1.0");
		List<String> varNames = new ArrayList<String>();
		varNames.add("r");
		varNames.add("s");
		check("(N1+1).compile()", (N1+1).compile().apply(new double[]{1.0,2.0}), 2.0);
		
		N1 = new SFLinearLocal2D(1);
		N2 = new SFLinearLocal2D(2);
		N3 = new SFLinearLocal2D(3);
		check(N1.toString(), "N1(r,s,t) = r");
		check(N2.toString(), "N2(r,s,t) = s");
		check(N3.toString(), "N3(r,s,t) = t");
		check((N1+1).toString(), "f(r,s,t) = N1(r,s,t) + 1.0");
		
		N1 = new SFBilinearLocal2D(1);
		N2  = new SFBilinearLocal2D(2);
		N3 = new SFBilinearLocal2D(3);
		N4 = new SFBilinearLocal2D(4);
		check(N1.toString(), "N1(r,s) = (-0.5*r+0.5)*(-0.5*s+0.5)");
		check(N2.toString(), "N2(r,s) = (0.5*r+0.5)*(-0.5*s+0.5)");
		check(N3.toString(), "N3(r,s) = (0.5*r+0.5)*(0.5*s+0.5)");
		check(N4.toString(), "N4(r,s) = (-0.5*r+0.5)*(0.5*s+0.5)");
		check((N1+1).toString(), "f(r,s) = N1(r,s) + 1.0");
		
	}

//	public void testDOF() {
//		
//		//Asign degree of freedom to nodes
//		for(int j=1;j<=e.nodes.size();j++) {
//			//Asign shape function to DOF
//			DOF dof = new DOF(j,e.nodes.at(j).globalIndex,shapeFun[j-1]);
//			e.addNodeDOF(j,dof);
//		}		
//	}
}

package edu.uta.futureye.lib.weakform;


import edu.uta.futureye.core.DOF;
import edu.uta.futureye.core.Element;
import edu.uta.futureye.core.Node;
import edu.uta.futureye.function.Variable;
import edu.uta.futureye.function.basic.FC;
import edu.uta.futureye.function.basic.Vector2MathFunc;
import edu.uta.futureye.function.intf.MathFunc;
import edu.uta.futureye.function.intf.ScalarShapeFunction;
import edu.uta.futureye.util.container.DOFList;

/**
 * 用有限元方法求导数
 * Solve: (w, v) = (U_x, v)
 * 
 * 
 * Test for:
 *   \eps*(\nabla{w},\nabla{v}) + (w,v) = (U_x, v)
 * where
 *   \eps -> 0
 * 
 * where w is unknown
 *   U_x is the piecewise derivative on the mesh
 *   w is an approximation of U_x
 *   
 * @author liuyueming
 */
public class WeakFormDerivative extends AbstractScalarWeakForm {
	protected Vector2MathFunc g_U = null;
	protected String varName; // "x" or "y"
	protected double eps = -1.0;

	public WeakFormDerivative(String varName) {
		this.varName = varName;
	}
	
	@Override
	public MathFunc leftHandSide(Element e, ItemType itemType) {
		if(itemType==ItemType.Domain)  {
			//Integrand part of Weak Form on element e
			MathFunc integrand = null;
			if(eps > 0.0) {
				integrand = u.diff("x").M(v.diff("x")).A(
						    u.diff("y").M(v.diff("y"))).M(eps).A(
						    u.M(v));
			} else {
				integrand = u.M(v);
			}
			return integrand;
		}
		return null;
	}

	@Override
	public MathFunc rightHandSide(Element e, ItemType itemType) {
		if(itemType==ItemType.Domain)  {
			MathFunc rlt = new FC(0.0);
			int nNode = e.nodes.size();
			for(int i=1;i<=nNode;i++) {
				DOFList dofListI = e.getNodeDOFList(i);
				for(int k=1;k<=dofListI.size();k++) {
					DOF dofI = dofListI.at(k);
					Variable var = Variable.createFrom(g_U, (Node)dofI.getOwner(), dofI.getGlobalIndex());
					MathFunc PValue = new FC(g_U.apply(var));
					ScalarShapeFunction shape = dofI.getSSF();
					//以前版本需要调用shapeFun.asignElement(e)，现在版本不需要调用了
					rlt = rlt.A(PValue.M(shape.diff(varName)));
				}
			}
			
			MathFunc integrand = rlt.M(v);
			return integrand;
		}
		return null;
	}

	public void setParam(Vector2MathFunc U) {
		this.g_U = U;
	}
	
	public void setParam(Vector2MathFunc U, double eps) {
		this.g_U = U;
		this.eps = eps;
	}
}

package edu.uta.futureye.function.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.DALOAD;
import org.apache.bcel.generic.DASTORE;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LocalVariableGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NEWARRAY;
import org.apache.bcel.generic.PUSH;
import org.apache.bcel.generic.Type;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.uta.futureye.bytecode.CompiledFunc;
import edu.uta.futureye.core.Element;
import edu.uta.futureye.core.Node;
import edu.uta.futureye.function.MultiVarFunc;
import edu.uta.futureye.function.Variable;
import edu.uta.futureye.function.VariableArray;
import edu.uta.futureye.function.intf.MathFunc;
import edu.uta.futureye.lib.assembler.AssembleParam;
import edu.uta.futureye.test.BytecodeTest;
import edu.uta.futureye.util.BytecodeConst;
import edu.uta.futureye.util.BytecodeUtils;
import edu.uta.futureye.util.FuncClassLoader;
import edu.uta.futureye.util.FutureyeException;
import edu.uta.futureye.util.Utils;

/**
 * Composite function
 * <p><blockquote><pre>
 * For example:
 *  MathFunc f = r*s + 1;
 *  Map<String, MathFunc> fInners = new HashMap<String, MathFunc>();
 *  
 *  fInners.put("r", x*x); //inner function: r(x) = x*x
 *  fInners.put("s", y+1); //inner function: s(y) = y+1
 *  
 *  MathFunc fc = f.compose(fInners);
 *  System.out.println(fc); //f(x,y) = (x*x)*(y + 1.0) + 1.0
 * </pre></blockquote>
 * The active variable of the composite function is the variables
 * of the inner functions by default.
 */
public class FComposite extends MultiVarFunc {
	public MathFunc fOuter;
	public Map<String,MathFunc> fInners;
	boolean isOuterVariablesActive;
	
	public FComposite(MathFunc fOuter, Map<String,MathFunc> fInners) {
		this.fOuter = fOuter;
		
		//Get all the variable names in inner function 
		List<String> nameList = new ArrayList<String>();
		for(Entry<String, MathFunc> e : fInners.entrySet()) {
			nameList = Utils.mergeList(nameList, e.getValue().getVarNames());
		}
		
		//Reconstruct argsMap
		Map<String,MathFunc> fInners2 = new HashMap<String,MathFunc>();
		Map<String, Integer> argsMap = Utils.getIndexMap(nameList);
		
		//Update argIdx for the inner functions
		//Copy on change: if the argIdx changed for a inner function, then the 
		//function is copied so that the original function is not affected 
		for(Entry<String, MathFunc> e : fInners.entrySet()) {
			if(!Utils.isMapContain(argsMap, e.getValue().getArgIdxMap())) {
				MathFunc f = e.getValue().copy().setArgIdx(argsMap);
				fInners2.put(e.getKey(), f);
			} else {
				fInners2.put(e.getKey(), e.getValue());
			}
		}
		this.fInners = fInners2;
		
		this.setInnerVarActive();
		// Default to use free variables in fInners
//		this.setVarNames(nameList);
//		this.setArgIdx(Utils.getIndexMap(nameList));
//		//this.setVarNames(fOuter.getVarNames());
//		//this.setArgIdx(Utils.getIndexMap(fOuter.getVarNames()));
	}

	@Override
	public double apply(Variable v) {
		return apply(v,null);
	}

	@Override
	public double apply(double... args) {
		return apply(null, args);
	}

	@Override
	public double apply(AssembleParam ap, double... args) {
		if(this.isOuterVariablesActive) {
			return fOuter.apply(ap, args);
		} else {
			List<String> vn = fOuter.getVarNames();
			double[] newArgs = new double[vn.size()];
			for(int i=0; i<vn.size(); i++) {
				MathFunc fInner = fInners.get(vn.get(i));
				newArgs[i] = fInner.apply(ap, args);
			}
			return fOuter.apply(newArgs);
		}
	}
	
	@Override
	public double apply(Variable v, Map<Object,Object> cache) {
		
		//if(fOuter.varNames().size() == 0) {
		//	throw new FutureyeException("\nERROR:\n fOuter varNames list is empty!");
		//}
		
		//bugfix 增加或条件 
		//bugfix 3/19/12
		//bug?3/20/12  v=[r], fOuter.varNames()=[s,t], 但fOuter的表达式只有r, 这种情况下会进入else分支，
		//一般来说是不会有这种情况的，如果确实有这种情况，需要在函数类增加activeVarNames
		//if(fOuter.varNames().containsAll(v.getValues().keySet()) ||
		//		v.getValues().keySet().containsAll(fOuter.varNames())) {
		if(v.getNameValuePairs().keySet().containsAll(fOuter.getVarNames())) {
			return fOuter.apply(v,cache);
		//} else if(fOuter.varNames().size() == fInners.size()){
		} else {
			Variable newVar = new Variable();
			for(String varName : fOuter.getVarNames()) {
				MathFunc fInner = fInners.get(varName);
				if(fInner != null ) 
					newVar.set(varName, fInner.apply(v,cache));
				else //for mixed case: fOuter( x(r,s,t), y(r,s,t), r, s) bugfix 3/19/12
					newVar.set(varName, v.get(varName));
				//	throw new FutureyeException("\nERROR:\n Can not find "+varName+" in fInners.");
			}
			return fOuter.apply(newVar,cache);
		}
//		else {
//			throw new FutureyeException(
//					"\nERROR:\n Variable number mismatch of fOuter("+
//					fOuter.varNames()+") and fInner("+fInners+").");
//		}
	} 

	@Override
	public double[] applyAll(VariableArray v, Map<Object,Object> cache) {
		//bugfix 增加或条件
		if(v.getValues().keySet().containsAll(fOuter.getVarNames())) {
			return fOuter.applyAll(v,cache);
		} else {
			VariableArray newVar = new VariableArray();
			for(String varName : fOuter.getVarNames()) {
				MathFunc fInner = fInners.get(varName);
				if(fInner != null )
					newVar.set(varName, fInner.applyAll(v,cache));
				else //for mixed case: fOuter( x(r,s,t), y(r,s,t), r, s)
					newVar.set(varName, v.get(varName));
			}
			return fOuter.applyAll(newVar,cache);
		}
	}
	
	/**
	 * Chain rule of calculus
	 * 
	 * For example:
	 * f( x(r,s), y(r,s) )_r = f_x * x_r + f_y * y_r
	 */
	@Override
	public MathFunc diff(String varName) {
		MathFunc rlt = null;
		if(fOuter.getVarNames().contains(varName)) {
			//Return derivative of f(x,y) with respect to x or y
			rlt = fOuter.diff(varName);
			return rlt;
		} else {
			//Return derivative of f(x(r,s),y(r,s)) with respective to r or s
			rlt = new FC(0.0);
			for(String innerVarName : fOuter.getVarNames()) {
				MathFunc fInner = fInners.get(innerVarName);
				if(fInner != null) {
					MathFunc rltOuter = fOuter.diff(innerVarName);
					if(!(rltOuter.isConstant())) {
						rltOuter = rltOuter.compose(fInners);
						//No need. see MathFuncBase.compose()
						//rltOuter.setOuterVarActive();
						//we need this
						if(this.isOuterVarActive())
							rltOuter.setOuterVarActive();
					}
					MathFunc rltInner = fInner.diff(varName);
					//f_x * x_r + f_y * y_r
					rlt = rlt.A(
							rltOuter.M(rltInner)
							);
				}
			}
			return rlt;
		}
	}
	
	@Override
	public int getOpOrder() {
		return fOuter.getOpOrder();
	}
	
	@Override
	public String getExpr() {
		if(this.isOuterVariablesActive) {
			return fOuter.getExpr();
		} else {
			String rlt = fOuter.getExpr();
			for(Entry<String,MathFunc> map : fInners.entrySet()) {
	//			String names = map.getValue().getVarNames().toString();
	//			rlt = rlt.replace(map.getKey(), 
	//					map.getKey()+"("+names.substring(1,names.length()-1)+")");
				if(map.getValue().getOpOrder() == OP_ORDER0)
					rlt = rlt.replace(map.getKey(), map.getValue().getExpr());
				else
					rlt = rlt.replace(map.getKey(), "("+map.getValue().getExpr()+")");
			}
			return rlt;
		}
	}

	/**
	 * Generate an array which contains the results of inner functions
	 * Then generate the outer function and take the array as the dummy
	 * arguments
	 */
	@Override
	public InstructionHandle bytecodeGen(String clsName, MethodGen mg,
			ConstantPoolGen cp, InstructionFactory factory,
			InstructionList il, Map<String, Integer> argsMap, int argsStartPos, 
			Map<MathFunc, Integer> funcRefsMap) {
		if(this.isOuterVariablesActive) {
			return fOuter.bytecodeGen(clsName, mg, cp, factory, il, argsMap, argsStartPos, funcRefsMap);
		} else {
			// Prepare a double array as the arguments for the fOuter function
			// which is equal to call the outer function
			LocalVariableGen lg;

			//double[] aryArgOuter = null;
			lg = mg.addLocalVariable("aryArgOuter",
				new ArrayType(Type.DOUBLE, 1), null, null);
			int aryArgOuter = lg.getIndex();
			il.append(InstructionConstants.ACONST_NULL);
			lg.setStart(il.append(new ASTORE(aryArgOuter))); // "idxArg" valid from here

			//aryArgOuter = new double[size]
			//il.append(new PUSH(cp, fInners.size()));
			il.append(new PUSH(cp, fOuter.getVarNames().size()));
			il.append(new NEWARRAY(Type.DOUBLE));
			il.append(new ASTORE(aryArgOuter));

			//int index = 0;
			Map<String, Integer> fOuterArgMap = fOuter.getArgIdxMap();
			for(String name : fOuter.getVarNames()) {
				MathFunc f = fInners.get(name);
				//aryArgOuter[argIdx] = {value of the argument}
				il.append(new ALOAD(aryArgOuter));
				il.append(new PUSH(cp, fOuterArgMap.get(name))); //index++
				if(f != null) {
					List<String> args = f.getVarNames();
					HashMap<String, Integer> fArgsMap = new HashMap<String, Integer>();
					for(int i=0; i<args.size(); i++) {
						fArgsMap.put(args[i], argsMap.get(args[i]));
					}
					f.bytecodeGen(clsName, mg, cp, factory, il, fArgsMap, BytecodeConst.argOutIdx, funcRefsMap);
				} else {
					//il.append(new PUSH(cp, 0.0)); //pad 0.0 for undefined variables in fInners map
					//bugfix
					//args[argsMap.get(name)]
					il.append(new ALOAD(argsStartPos));
					il.append(new PUSH(cp, argsMap.get(name)));
					il.append(new DALOAD());
				}
				il.append(new DASTORE());
			}
			// Pass the generated double array to fOuter by specifying the start position to 'aryArgOuter'
			return fOuter.bytecodeGen(clsName, mg, cp, factory, il, fOuter.getArgIdxMap(), aryArgOuter, funcRefsMap);
		}
	}
	
	/**
	 * Generate a separate function for the outer function and pass the results of inner functions as
	 * arguments
	 * 
	 * @param clsName
	 * @param mg
	 * @param cp
	 * @param factory
	 * @param il
	 * @param argsMap
	 * @param argsStartPos
	 * @param funcRefsMap
	 * @return
	 */
	public InstructionHandle bytecodeGenSlow(String clsName, MethodGen mg,
			ConstantPoolGen cp, InstructionFactory factory,
			InstructionList il, Map<String, Integer> argsMap, int argsStartPos, 
			Map<MathFunc, Integer> funcRefsMap) {
		if(this.isOuterVariablesActive) {
			return fOuter.bytecodeGen(clsName, mg, cp, factory, il, argsMap, argsStartPos, funcRefsMap);
		} else {
			String outerName  = "fun_outer_"+java.util.UUID.randomUUID().toString().replaceAll("-", "");
			// Generate the outer function
			FuncClassLoader<CompiledFunc> fcl = FuncClassLoader.<CompiledFunc>getInstance(BytecodeTest.class.getClassLoader());
			ClassGen genClass = BytecodeUtils.genClass(fOuter, null, outerName, true, true);
			fcl.newInstance(genClass);
	
			// Prepare arguments for calling the outer function
			LocalVariableGen lg;
			//double[] arg = null;
			lg = mg.addLocalVariable("arg_"+outerName,
				new ArrayType(Type.DOUBLE, 1), null, null);
			int idxArg = lg.getIndex();
			il.append(InstructionConstants.ACONST_NULL);
			lg.setStart(il.append(new ASTORE(idxArg))); // "idxArg" valid from here
			//arg = new double[size]
			il.append(new PUSH(cp, fInners.size()));
			il.append(new NEWARRAY(Type.DOUBLE));
			il.append(new ASTORE(idxArg));
			
			int index = 0;
			for(String name : fOuter.getVarNames()) {
				il.append(new ALOAD(idxArg));
				il.append(new PUSH(cp, index++));
				MathFunc f = fInners.get(name);
				HashMap<String, Integer> fArgsMap = new HashMap<String, Integer>();
				List<String> args = f.getVarNames();
				for(int i=0; i<args.size(); i++) {
					fArgsMap.put(args[i], argsMap.get(args[i]));
				}
				f.bytecodeGen(clsName, mg, cp, factory, il, fArgsMap, BytecodeConst.argOutIdx, funcRefsMap);
				il.append(new DASTORE());
			}
			
			// Call the outer function
			il.append(InstructionConstants.ACONST_NULL);
			il.append(InstructionConstants.ACONST_NULL);
			il.append(new ALOAD(idxArg));
			return  il.append(factory.createInvoke("edu.uta.futureye.bytecode."+outerName, "apply",
					Type.DOUBLE, 
					new Type[] { 
						Type.getType(Element.class),
						Type.getType(Node.class),
						new ArrayType(Type.DOUBLE, 1)
					}, 
			Constants.INVOKESTATIC));
		}
	}
	
	@Override
	public MathFunc setArgIdx(Map<String, Integer> argsMap) {
		super.setArgIdx(argsMap);
		if(this.isOuterVariablesActive) {
			this.fOuter.setArgIdx(argsMap);
		} else {
		}
		return this;
	}
	
	@Override
	public MathFunc setActiveVarByNames(List<String> varNames) {
		if(Utils.isListEqualIgnoreOrder(fOuter.getVarNames(),  varNames)) {
			this.isOuterVariablesActive = true;
			this.setVarNames(varNames);
			this.setArgIdx(Utils.getIndexMap(varNames));
			return this;
		} else {
			List<String> list = new ArrayList<String>();
			for(Entry<String, MathFunc> e : fInners.entrySet()) {
				list = Utils.mergeList(list, e.getValue().getVarNames());
			}
			if(Utils.isListEqualIgnoreOrder(list, varNames)){
				this.isOuterVariablesActive = false;
				this.setVarNames(varNames);
				Map<String, Integer> argsMap = Utils.getIndexMap(list);
				for(Entry<String, MathFunc> e : fInners.entrySet()) {
					e.getValue().setArgIdx(argsMap);
				}
				this.setArgIdx(Utils.getIndexMap(list));
				return this;
			}
		}
		throw new FutureyeException("Active variable names are different from all existing variable names!");
	}

	@Override
	public List<String> getActiveVarNames() {
		if(this.isOuterVariablesActive)
			return fOuter.getVarNames();
		else {
			List<String> list = new ArrayList<String>();
			for(Entry<String, MathFunc> e : fInners.entrySet()) {
				list = Utils.mergeList(list, e.getValue().getVarNames());
			}
			return list;
		}
	}
	
	@Override
	public MathFunc setOuterVarActive() {
		this.isOuterVariablesActive = true;
		this.setActiveVarByNames(this.getActiveVarNames());
		return this;
	}
	
	@Override
	public MathFunc setInnerVarActive() {
		this.isOuterVariablesActive = false;
		this.setActiveVarByNames(this.getActiveVarNames());
		return this;
	}
	
	@Override
	public boolean isOuterVarActive() {
		return this.isOuterVariablesActive;
	}
	
	@Override
	public boolean isInnerVarActive() {
		return !this.isOuterVariablesActive;
	}
	
	@Override
	public void bytecodeGen(MethodVisitor mv, Map<String, Integer> argsMap,
			int argsStartPos, Map<MathFunc, Integer> funcRefsMap, String clsName) {
		if(this.isOuterVariablesActive) {
			fOuter.bytecodeGen(mv, argsMap, argsStartPos, funcRefsMap, clsName);
		} else {
			//TODO find a way to determine aryArgOuterLVTIdx and put the variable in LVT
			int aryArgOuterLVTIdx = 3;//???
			
			//define a local variable 
			//double[] aryArgOuter = new double[size];
			mv.visitLdcInsn(fOuter.getVarNames().size());
			mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_DOUBLE);
			mv.visitVarInsn(Opcodes.ASTORE, aryArgOuterLVTIdx);

			//int index = 0;
			Map<String, Integer> fOuterArgMap = fOuter.getArgIdxMap();
			for(String name : fOuter.getVarNames()) {
				MathFunc f = fInners.get(name);
				//aryArgOuter[argIdx] = {value of the argument}
				mv.visitVarInsn(Opcodes.ALOAD, aryArgOuterLVTIdx);
				mv.visitLdcInsn(fOuterArgMap.get(name));
				if(f != null) {
					List<String> args = f.getVarNames();
					HashMap<String, Integer> fArgsMap = new HashMap<String, Integer>();
					for(int i=0; i<args.size(); i++) {
						fArgsMap.put(args[i], argsMap.get(args[i]));
					}
					f.bytecodeGen(mv, fArgsMap, argsStartPos, funcRefsMap, clsName);
				} else {
					//f(r,x,y) = ((x*x + y*y)*-2.0 + 36.0)*r
					//fInner = {x=..., y=...} //no r
					//mv.visitLdcInsn(0.0); // //pad 0.0 for undefined variables in fInners map
					//get from 'argsMap' seems correct?
					mv.visitVarInsn(Opcodes.ALOAD, argsStartPos);
					mv.visitLdcInsn(argsMap.get(name));
					mv.visitInsn(Opcodes.DALOAD);
				}
				mv.visitInsn(Opcodes.DASTORE);
			}
			// Pass the generated double array to fOuter by specifying the start position to 'aryArgOuterLVTIdx'
			fOuter.bytecodeGen(mv, fOuter.getArgIdxMap(), aryArgOuterLVTIdx, funcRefsMap, clsName);
		}
	}

}

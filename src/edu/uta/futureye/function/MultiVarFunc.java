/**
 * Copyright (c) 2010, nkliuyueming@gmail.com. All rights reserved.
 * 
 * 
 */
package edu.uta.futureye.function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.uta.futureye.function.intf.MathFunc;
import edu.uta.futureye.util.FutureyeException;

/**
 * Template to implement multiple variables MathFunc 
 *
 */
public abstract class MultiVarFunc extends MathFuncBase {
	protected String fName;
	protected String[] varNames;
	protected int[] argIdx;
	
	public MultiVarFunc() {
	}
	
	public MultiVarFunc(String funcName, String ...varNames) {
		this.fName = funcName;
		this.varNames = varNames;
		this.argIdx = new int[varNames.length];
		for(int i = 0; i<argIdx.length; i++)
			argIdx[i] = i;
	}
	
	public MultiVarFunc(String funcName, List<String> varNames) {
		this.varNames = varNames.toArray(new String[0]);
		this.argIdx = new int[varNames.size()];
		for(int i = 0; i<argIdx.length; i++)
			argIdx[i] = i;
	}
	
	@Override
	public List<String> getVarNames() {
		List<String> list = new ArrayList<String>();
		if(null == varNames) return list;
		for(String s : varNames)
			list.add(s);
		return list;
	}

	@Override
	public MathFunc setVarNames(List<String> varNames) {
		this.varNames = varNames.toArray(new String[0]);
		//TODO Set argIdx according varNames as defaults? How to deal with only the change of variable name?
		//this.argIdx = null;
		return this;
	}
	
	@Override
	public MathFunc setArgIdx(Map<String, Integer> argsMap) {
		//Allocate new array each time due to the "copy on change"
		if(null == varNames) return this;
		int[] idx = new int[varNames.length];
		for(int i=0; i<varNames.length; i++) {
			Integer idx2 = argsMap.get(varNames[i]);
			if(idx2 == null)
				throw new FutureyeException("Cann't find index for variable "+varNames[i]);
			idx[i] = idx2;
		}
		this.argIdx = idx;
		return this;
	}
	
	@Override
	public Map<String, Integer> getArgIdxMap() {
		if(this.argIdx == null) return null;
		Map<String, Integer> ret = new HashMap<String, Integer>();
		for(int i=0; i<varNames.length; i++) {
			ret.put(varNames[i], argIdx[i]);
		}
		return ret;
	}
	
	@Override
	public String getName() {
		return this.fName;
	}
	
	@Override
	public MathFunc setName(String name) {
		this.fName = name;
		return this;
	}
	
	@Override
	public boolean isConstant() {
		return false;
	}
	
	@Override
	public boolean isInteger() {
		return false;
	}
	
	@Override
	public boolean isZero() {
		return false;
	}
	
	@Override
	public boolean isReal() {
		return false;
	}
}

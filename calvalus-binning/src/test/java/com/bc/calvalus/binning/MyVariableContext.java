package com.bc.calvalus.binning;

/**
* A simple VariableContext.
*
* @author Norman
*/
public class MyVariableContext implements VariableContext {
    private String[] varNames;

    public MyVariableContext(String ... varNames) {
        this.varNames = varNames;
    }

    @Override
    public int getVariableCount() {
        return varNames.length;
    }

    @Override
    public String getVariableName(int i) {
        return varNames[i];
    }

    @Override
    public int getVariableIndex(String varName) {
        for (int i = 0; i < varNames.length; i++) {
            if (varName.equals(varNames[i])) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String getVariableExpr(int i) {
        return null;
    }


    @Override
    public String getMaskExpr() {
        return null;
    }
}

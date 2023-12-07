package compile.vir.ir;

import compile.vir.VReg;
import compile.symbol.FuncSymbol;
import compile.vir.value.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CallVIR extends VIR {
    public final FuncSymbol func;
    public final VReg target;
    public final List<Value> params;

    public CallVIR(FuncSymbol func, VReg target, List<Value> params) {
        super(func.getType());
        this.func = func;
        this.target = target;
        this.params = params;
    }

    @Override
    public VIR copy() {
        return new CallVIR(func, target, new ArrayList<>(params));
    }

    @Override
    public List<VReg> getRead() {
        return params.stream().filter(VReg.class::isInstance).map(VReg.class::cast).collect(Collectors.toList());
    }

    @Override
    public VReg getWrite() {
        return target;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CALL    ").append(func.getName()).append(", ").append(target == null ? "$void" : target);
        params.forEach(param -> builder.append(", ").append(param));
        return builder.toString();
    }
}
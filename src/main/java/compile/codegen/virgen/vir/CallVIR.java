package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;
import compile.symbol.FuncSymbol;

import java.util.List;
import java.util.stream.Collectors;

public record CallVIR(FuncSymbol func, VReg target, List<VIRItem> params) implements VIR {
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

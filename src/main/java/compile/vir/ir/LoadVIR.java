package compile.vir.ir;

import compile.vir.VReg;
import compile.symbol.DataSymbol;
import compile.vir.type.BasicType;
import compile.vir.value.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LoadVIR extends VIR {
    public final DataSymbol symbol;
    public final List<Value> indexes;

    public LoadVIR(DataSymbol symbol, List<Value> indexes) {
        super(symbol.getDimensionSize() != indexes.size() ? BasicType.I32 : symbol.getType());
        this.symbol = symbol;
        this.indexes = indexes;
    }

    @Override
    public VIR copy() {
        return new LoadVIR(symbol, new ArrayList<>(indexes));
    }

    @Override
    public List<VReg> getRead() {
        return indexes.stream().filter(VReg.class::isInstance).map(VReg.class::cast).collect(Collectors.toList());
    }

    public boolean isSingle() {
        return indexes.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LOAD    ").append(getTag()).append(", ");
        builder.append(symbol.getName());
        indexes.forEach(dimension -> builder.append('[').append(dimension instanceof VIR ir ? ir.getTag() : dimension).append(']'));
        return builder.toString();
    }
}

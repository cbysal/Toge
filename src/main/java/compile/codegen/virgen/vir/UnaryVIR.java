package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;

import java.util.List;

public record UnaryVIR(Type type, VReg target, VIRItem source) implements VIR {
    public enum Type {
        ABS, F2I, I2F, L_NOT, NEG
    }

    @Override
    public List<VReg> getRead() {
        if (source instanceof VReg reg)
            return List.of(reg);
        return List.of();
    }

    @Override
    public VReg getWrite() {
        return target;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(type);
        builder.append(" ".repeat(8 - builder.length()));
        builder.append(target).append(", ").append(source);
        return builder.toString();
    }
}

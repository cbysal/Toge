package compile.codegen.virgen.vir;

import compile.codegen.virgen.VReg;

import java.util.List;

public record MovVIR(VReg target, VReg source) implements VIR {
    @Override
    public List<VReg> getRead() {
        return List.of(source);
    }

    @Override
    public VReg getWrite() {
        return target;
    }

    @Override
    public String toString() {
        return "MOV     " + target + ", " + source;
    }
}

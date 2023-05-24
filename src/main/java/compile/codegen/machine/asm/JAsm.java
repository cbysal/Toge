package compile.codegen.machine.asm;

import compile.codegen.machine.Block;
import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.VReg;

import java.util.List;
import java.util.Map;

public class JAsm implements Asm {
    private Block dest;

    public JAsm(Block dest) {
        this.dest = dest;
    }

    @Override
    public List<VReg> getVRegs() {
        return List.of();
    }

    @Override
    public void replaceVRegs(Map<VReg, MReg> vRegToMReg) {
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("j %s", dest.getTag());
    }
}
package compile.codegen.machine.asm;

import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record FnegASM(Reg dest, Reg src) implements Asm {
    @Override
    public List<VReg> getVRegs() {
        List<VReg> vRegs = new ArrayList<>();
        if (dest instanceof VReg vReg) {
            vRegs.add(vReg);
        }
        if (src instanceof VReg vReg) {
            vRegs.add(vReg);
        }
        return vRegs;
    }

    @Override
    public Asm replaceVRegs(Map<VReg, MReg> vRegToMReg) {
        if (dest instanceof VReg vDest && vRegToMReg.containsKey(vDest) && src instanceof VReg vSrc && vRegToMReg.containsKey(vSrc)) {
            return new FnegASM(vRegToMReg.get(vDest), vRegToMReg.get(vSrc));
        }
        if (dest instanceof VReg vDest && vRegToMReg.containsKey(vDest)) {
            return new FnegASM(vRegToMReg.get(vDest), src);
        }
        if (src instanceof VReg vSrc && vRegToMReg.containsKey(vSrc)) {
            return new FnegASM(dest, vRegToMReg.get(vSrc));
        }
        return this;
    }

    @Override
    public List<Asm> spill(Map<VReg, Integer> vRegToSpill) {
        return List.of(this);
    }

    @Override
    public String toString() {
        return String.format("fneg.s %s,%s", dest, src);
    }
}

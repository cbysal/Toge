package compile.codegen.virgen.vir;

import compile.codegen.virgen.Block;

public class JumpVIR extends VIR {
    public final Block target;

    public JumpVIR(Block target) {
        this.target = target;
    }

    @Override
    public VIR copy() {
        return new JumpVIR(target);
    }

    @Override
    public String toString() {
        return "B       " + target;
    }
}

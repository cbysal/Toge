package compile.codegen.mirgen.trans;

import compile.codegen.mirgen.MReg;
import compile.codegen.mirgen.mir.*;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.vir.UnaryVIR;
import compile.symbol.Type;

import java.util.List;

public final class MIRUnaryTrans {
    private static void transF2IRegReg(List<MIR> irs, VReg target, VReg source) {
        irs.add(new CvtMIR(target, source));
    }

    private static void transI2FRegReg(List<MIR> irs, VReg target, VReg source) {
        irs.add(new CvtMIR(target, source));
    }

    private static void transLNotRegReg(List<MIR> irs, VReg target, VReg source) {
        if (source.getType() == Type.FLOAT) {
            VReg midReg = new VReg(Type.FLOAT);
            irs.add(new CvtMIR(midReg, MReg.ZERO));
            irs.add(new RrrMIR(RrrMIR.Op.EQ, target, source, midReg));
        } else {
            irs.add(new RriMIR(RriMIR.Op.SLTIU, target, source, 1));
        }
    }

    private static void transNegRegReg(List<MIR> irs, VReg target, VReg source) {
        if (target.getType() == Type.FLOAT)
            irs.add(new NegMIR(target, source));
        else
            irs.add(new RrrMIR(RrrMIR.Op.SUB, target, MReg.ZERO, source));

    }

    static void transUnaryReg(List<MIR> irs, UnaryVIR unaryVIR, VReg reg) {
        switch (unaryVIR.getType()) {
            case F2I -> transF2IRegReg(irs, unaryVIR.getResult(), reg);
            case I2F -> transI2FRegReg(irs, unaryVIR.getResult(), reg);
            case L_NOT -> transLNotRegReg(irs, unaryVIR.getResult(), reg);
            case NEG -> transNegRegReg(irs, unaryVIR.getResult(), reg);
            default -> throw new RuntimeException();
        }
    }
}

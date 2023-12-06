package compile.codegen.mirgen.trans;

import compile.codegen.mirgen.MReg;
import compile.codegen.mirgen.mir.MIR;
import compile.codegen.mirgen.mir.RrMIR;
import compile.codegen.mirgen.mir.RriMIR;
import compile.codegen.mirgen.mir.RrrMIR;
import compile.vir.VReg;
import compile.vir.ir.UnaryVIR;
import compile.vir.type.BasicType;

import java.util.List;

public final class MIRUnaryTrans {
    private static void transF2IRegReg(List<MIR> irs, VReg target, VReg source) {
        irs.add(new RrMIR(RrMIR.Op.CVT, target, source));
    }

    private static void transI2FRegReg(List<MIR> irs, VReg target, VReg source) {
        irs.add(new RrMIR(RrMIR.Op.CVT, target, source));
    }

    private static void transLNotRegReg(List<MIR> irs, VReg target, VReg source) {
        if (source.getType() == BasicType.FLOAT) {
            VReg midReg = new VReg(BasicType.FLOAT, 4);
            irs.add(new RrMIR(RrMIR.Op.CVT, midReg, MReg.ZERO));
            irs.add(new RrrMIR(RrrMIR.Op.EQ, target, source, midReg));
        } else {
            irs.add(new RrMIR(RrMIR.Op.SEQZ, target, source));
        }
    }

    private static void transNegRegReg(List<MIR> irs, VReg target, VReg source) {
        irs.add(new RrMIR(RrMIR.Op.NEG, target, source));
    }

    private static void transAbsRegReg(List<MIR> irs, VReg target, VReg source) {
        if (target.getType() == BasicType.FLOAT)
            irs.add(new RrMIR(RrMIR.Op.FABS, target, source));
        else {
            VReg midReg1 = new VReg(BasicType.I32, 4);
            VReg midReg2 = new VReg(BasicType.I32, 4);
            irs.add(new RriMIR(RriMIR.Op.SRAIW, midReg1, source, 31));
            irs.add(new RrrMIR(RrrMIR.Op.XOR, midReg2, source, midReg1));
            irs.add(new RrrMIR(RrrMIR.Op.SUBW, target, midReg2, midReg1));
        }
    }

    static void transUnaryReg(List<MIR> irs, UnaryVIR unaryVIR, VReg reg) {
        switch (unaryVIR.type) {
            case ABS -> transAbsRegReg(irs, unaryVIR.target, reg);
            case F2I -> transF2IRegReg(irs, unaryVIR.target, reg);
            case I2F -> transI2FRegReg(irs, unaryVIR.target, reg);
            case L_NOT -> transLNotRegReg(irs, unaryVIR.target, reg);
            case NEG -> transNegRegReg(irs, unaryVIR.target, reg);
            default -> throw new RuntimeException();
        }
    }
}

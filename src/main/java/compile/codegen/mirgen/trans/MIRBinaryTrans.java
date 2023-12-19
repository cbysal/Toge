package compile.codegen.mirgen.trans;

import compile.codegen.VReg;
import compile.codegen.mirgen.mir.*;
import compile.llvm.contant.ConstantNumber;
import compile.llvm.ir.BinaryOperator;
import compile.llvm.ir.Instruction;
import compile.llvm.type.BasicType;

import java.util.List;
import java.util.Map;

public final class MIRBinaryTrans {
    private static void transAddRegImmF(List<MIR> irs, VReg target, VReg source, float imm) {
        VReg midReg = new VReg(BasicType.FLOAT);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transAddRegRegF(irs, target, source, midReg);
    }

    static void transAddRegImmI(List<MIR> irs, VReg target, VReg source, int imm) {
        if (imm >= -2048 && imm < 2048) {
            irs.add(new RriMIR(RriMIR.Op.ADDI, target, source, imm));
            return;
        }
        VReg midReg = new VReg(BasicType.I32);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transAddRegRegI(irs, target, source, midReg);
    }

    private static void transAddRegRegF(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.ADD, target, source1, source2));
    }

    private static void transAddRegRegI(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.ADD, target, source1, source2));
    }

    static void transBinaryImmReg(List<MIR> irs, Map<Instruction, VReg> instRegMap, BinaryOperator binaryOperator, ConstantNumber value, VReg reg) {
        VReg target = instRegMap.get(binaryOperator);
        switch (binaryOperator.type) {
            case ADD -> transAddRegImmI(irs, target, reg, value.intValue());
            case FADD -> transAddRegImmF(irs, target, reg, value.floatValue());
            case SDIV -> transDivImmRegI(irs, target, value.intValue(), reg);
            case FDIV -> transDivImmRegF(irs, target, value.floatValue(), reg);
            case EQ, GE, GT, LE, LT, NE -> {
                BinaryOperator.Type type = switch (binaryOperator.type) {
                    case EQ -> BinaryOperator.Type.EQ;
                    case GE -> BinaryOperator.Type.LT;
                    case GT -> BinaryOperator.Type.LE;
                    case LE -> BinaryOperator.Type.GT;
                    case LT -> BinaryOperator.Type.GE;
                    case NE -> BinaryOperator.Type.NE;
                    default -> throw new RuntimeException();
                };
                if (reg.getType() == BasicType.FLOAT)
                    transCmpRegImmF(irs, type, target, reg, value.floatValue());
                else
                    transCmpRegImmI(irs, type, target, reg, value.intValue());
            }
            case SREM -> transModImmReg(irs, target, value.intValue(), reg);
            case MUL -> transMulRegImmI(irs, target, reg, value.intValue());
            case FMUL -> transMulRegImmF(irs, target, reg, value.floatValue());
            case SUB -> transSubImmRegI(irs, target, value.intValue(), reg);
            case FSUB -> transSubImmRegF(irs, target, value.floatValue(), reg);
            default -> throw new RuntimeException();
        }
    }

    static void transBinaryRegImm(List<MIR> irs, Map<Instruction, VReg> instRegMap, BinaryOperator binaryOperator, VReg reg, ConstantNumber value) {
        VReg target = instRegMap.get(binaryOperator);
        switch (binaryOperator.type) {
            case ADD -> transAddRegImmI(irs, target, reg, value.intValue());
            case FADD -> transAddRegImmF(irs, target, reg, value.floatValue());
            case SDIV -> transDivRegImmI(irs, target, reg, value.intValue());
            case FDIV -> transDivRegImmF(irs, target, reg, value.floatValue());
            case EQ, GE, GT, LE, LT, NE -> {
                if (reg.getType() == BasicType.FLOAT)
                    transCmpRegImmF(irs, binaryOperator.type, target, reg, value.floatValue());
                else
                    transCmpRegImmI(irs, binaryOperator.type, target, reg, value.intValue());
            }
            case SREM -> transModRegImm(irs, target, reg, value.intValue());
            case MUL -> transMulRegImmI(irs, target, reg, value.intValue());
            case FMUL -> transMulRegImmF(irs, target, reg, value.floatValue());
            case SUB -> transSubRegImmI(irs, target, reg, value.intValue());
            case FSUB -> transSubRegImmF(irs, target, reg, value.floatValue());
            case XOR -> {
                VReg midReg = new VReg(BasicType.I32);
                MIROpHelper.loadImmToReg(irs, midReg, value.intValue());
                irs.add(new RrrMIR(RrrMIR.Op.XOR, target, reg, midReg));
            }
            default -> throw new RuntimeException();
        }
    }

    static void transBinaryRegReg(List<MIR> irs, Map<Instruction, VReg> instRegMap, BinaryOperator binaryOperator, VReg reg1, VReg reg2) {
        VReg target = instRegMap.get(binaryOperator);
        switch (binaryOperator.type) {
            case ADD -> transAddRegRegI(irs, target, reg1, reg2);
            case FADD -> transAddRegRegF(irs, target, reg1, reg2);
            case SDIV -> transDivRegRegI(irs, target, reg1, reg2);
            case FDIV -> transDivRegRegF(irs, target, reg1, reg2);
            case EQ, GE, GT, LE, LT, NE -> {
                if (reg1.getType() == BasicType.FLOAT)
                    transCmpRegRegF(irs, binaryOperator.type, target, reg1, reg2);
                else
                    transCmpRegRegI(irs, binaryOperator.type, target, reg1, reg2);
            }
            case SREM -> transModRegReg(irs, target, reg1, reg2);
            case MUL -> transMulRegRegI(irs, target, reg1, reg2);
            case FMUL -> transMulRegRegF(irs, target, reg1, reg2);
            case SUB -> transSubRegRegI(irs, target, reg1, reg2);
            case FSUB -> transSubRegRegF(irs, target, reg1, reg2);
            default -> throw new RuntimeException();
        }
    }

    private static void transCmpRegImmF(List<MIR> irs, BinaryOperator.Type type, VReg target, VReg source, float imm) {
        VReg midReg = new VReg(BasicType.FLOAT);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transCmpRegRegF(irs, type, target, source, midReg);
    }

    private static void transCmpRegImmI(List<MIR> irs, BinaryOperator.Type type, VReg target, VReg source, int imm) {
        VReg midReg = new VReg(BasicType.I32);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transCmpRegRegI(irs, type, target, source, midReg);
    }

    private static void transCmpRegRegF(List<MIR> irs, BinaryOperator.Type type, VReg target, VReg source1, VReg source2) {
        if (type == BinaryOperator.Type.NE) {
            irs.add(new RrrMIR(RrrMIR.Op.EQ, target, source1, source2));
            irs.add(new RriMIR(RriMIR.Op.XORI, target, target, 1));
            return;
        }
        irs.add(new RrrMIR(switch (type) {
            case EQ -> RrrMIR.Op.EQ;
            case GE -> RrrMIR.Op.GE;
            case GT -> RrrMIR.Op.GT;
            case LE -> RrrMIR.Op.LE;
            case LT -> RrrMIR.Op.LT;
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }, target, source1, source2));
    }

    private static void transCmpRegRegI(List<MIR> irs, BinaryOperator.Type type, VReg target, VReg source1, VReg source2) {
        switch (type) {
            case EQ -> {
                VReg midReg = new VReg(BasicType.I32);
                irs.add(new RrrMIR(RrrMIR.Op.SUB, midReg, source1, source2));
                irs.add(new RrMIR(RrMIR.Op.SEQZ, target, midReg));
            }
            case NE -> {
                VReg midReg = new VReg(BasicType.I32);
                irs.add(new RrrMIR(RrrMIR.Op.SUB, midReg, source1, source2));
                irs.add(new RrMIR(RrMIR.Op.SNEZ, target, midReg));
            }
            case GE -> {
                VReg midReg = new VReg(BasicType.I32);
                irs.add(new RrrMIR(RrrMIR.Op.SLT, midReg, source1, source2));
                irs.add(new RriMIR(RriMIR.Op.XORI, target, midReg, 1));
            }
            case GT -> irs.add(new RrrMIR(RrrMIR.Op.SGT, target, source1, source2));
            case LE -> {
                VReg midReg = new VReg(BasicType.I32);
                irs.add(new RrrMIR(RrrMIR.Op.SGT, midReg, source1, source2));
                irs.add(new RriMIR(RriMIR.Op.XORI, target, midReg, 1));
            }
            case LT -> irs.add(new RrrMIR(RrrMIR.Op.SLT, target, source1, source2));
        }
    }

    private static void transDivImmRegF(List<MIR> irs, VReg target, float imm, VReg source) {
        VReg midReg = new VReg(BasicType.FLOAT);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transDivRegRegF(irs, target, midReg, source);
    }

    private static void transDivImmRegI(List<MIR> irs, VReg target, int imm, VReg source) {
        VReg midReg = new VReg(BasicType.I32);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transDivRegRegI(irs, target, midReg, source);
    }

    private static void transDivRegImmF(List<MIR> irs, VReg target, VReg source, float imm) {
        VReg midReg = new VReg(BasicType.FLOAT);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transDivRegRegF(irs, target, source, midReg);
    }

    private static void transDivRegImmI(List<MIR> irs, VReg target, VReg source, int imm) {
        if (imm == 1) {
            irs.add(new RrMIR(RrMIR.Op.MV, target, source));
            return;
        }
        if (imm == -1) {
            irs.add(new RrMIR(RrMIR.Op.NEG, target, source));
            return;
        }
        int div = imm;
        boolean isPos = true;
        if (div < 0) {
            isPos = false;
            div = Math.abs(div);
        }
        int shift = 0;
        while (1L << (shift + 32) <= (0x7fffffffL - 0x80000000L % div) * (div - (1L << (shift + 32)) % div))
            shift++;
        int magic = (int) ((1L << (shift + 32)) / div + 1);
        VReg midReg1 = new VReg(BasicType.I32);
        VReg midReg2 = new VReg(BasicType.I32);
        VReg midReg3 = new VReg(BasicType.I32);
        VReg midReg4 = new VReg(BasicType.I32);
        MIROpHelper.loadImmToReg(irs, midReg1, magic);
        if (magic >= 0) {
            irs.add(new RrrMIR(RrrMIR.Op.MUL, midReg2, source, midReg1));
            irs.add(new RriMIR(RriMIR.Op.SRLI, midReg2, midReg2, 32));
        } else {
            irs.add(new RrrMIR(RrrMIR.Op.MUL, midReg2, source, midReg1));
            irs.add(new RriMIR(RriMIR.Op.SRLI, midReg2, midReg2, 32));
            irs.add(new RrrMIR(RrrMIR.Op.ADD, midReg2, midReg2, source));
        }
        if (shift != 0)
            irs.add(new RriMIR(RriMIR.Op.SRAIW, midReg3, midReg2, shift));
        else
            midReg3 = midReg2;
        if (isPos) {
            irs.add(new RriMIR(RriMIR.Op.SRLIW, midReg4, source, 31));
            irs.add(new RrrMIR(RrrMIR.Op.ADDW, target, midReg3, midReg4));
        } else {
            irs.add(new RriMIR(RriMIR.Op.SRAIW, midReg4, source, 31));
            irs.add(new RrrMIR(RrrMIR.Op.SUBW, target, midReg3, midReg4));
        }
    }

    private static void transDivRegRegF(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.DIV, target, source1, source2));
    }

    private static void transDivRegRegI(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.DIVW, target, source1, source2));
    }

    private static void transModImmReg(List<MIR> irs, VReg target, int imm, VReg source) {
        VReg midReg = new VReg(BasicType.I32);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transModRegReg(irs, target, midReg, source);
    }

    private static void transModRegImm(List<MIR> irs, VReg target, VReg source, int imm) {
        if (imm == 1) {
            irs.add(new RrMIR(RrMIR.Op.MV, target, source));
            return;
        }
        if (Integer.bitCount(imm) == 1) {
            VReg midReg1 = new VReg(BasicType.I32);
            if (imm == 2)
                irs.add(new RriMIR(RriMIR.Op.SRLIW, midReg1, source, 31));
            else {
                irs.add(new RriMIR(RriMIR.Op.SRAIW, midReg1, source, 31));
                irs.add(new RriMIR(RriMIR.Op.SRLIW, midReg1, midReg1, Integer.numberOfLeadingZeros(imm) + 1));
            }
            irs.add(new RrrMIR(RrrMIR.Op.ADD, midReg1, midReg1, source));
            if (Math.abs(imm) < 2048) {
                irs.add(new RriMIR(RriMIR.Op.ANDI, midReg1, midReg1, -imm));
            } else {
                VReg midReg2 = new VReg(BasicType.I32);
                irs.add(new LiMIR(midReg2, -imm));
                irs.add(new RrrMIR(RrrMIR.Op.AND, midReg1, midReg1, midReg2));
            }
            irs.add(new RrrMIR(RrrMIR.Op.SUBW, target, source, midReg1));
            return;
        }
        VReg midReg1 = new VReg(BasicType.I32);
        VReg midReg2 = new VReg(BasicType.I32);
        transDivRegImmI(irs, midReg1, source, imm);
        transMulRegImmI(irs, midReg2, midReg1, imm);
        irs.add(new RrrMIR(RrrMIR.Op.SUBW, target, source, midReg2));
    }

    private static void transModRegReg(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.REMW, target, source1, source2));
    }

    private static void transMulRegImmF(List<MIR> irs, VReg target, VReg source, float imm) {
        VReg midReg = new VReg(BasicType.FLOAT);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transMulRegRegF(irs, target, source, midReg);
    }

    public static void transMulRegImmI(List<MIR> irs, VReg target, VReg source, int imm) {
        if (imm == 0) {
            MIROpHelper.loadImmToReg(irs, target, 0);
            return;
        }
        if (imm == 1) {
            irs.add(new RrMIR(RrMIR.Op.MV, target, source));
            return;
        }
        if (imm == -1) {
            irs.add(new RrMIR(RrMIR.Op.NEG, target, source));
            return;
        }
        if (Integer.bitCount(imm) == 1) {
            irs.add(new RriMIR(RriMIR.Op.SLLIW, target, source, Integer.numberOfTrailingZeros(imm)));
            return;
        }
        if (Integer.bitCount(imm) == 2 && imm % 2 == 1) {
            VReg midReg = new VReg(BasicType.I32);
            irs.add(new RriMIR(RriMIR.Op.SLLIW, midReg, source, 31 - Integer.numberOfLeadingZeros(imm)));
            irs.add(new RrrMIR(RrrMIR.Op.ADDW, target, midReg, source));
            return;
        }
        if (Integer.numberOfTrailingZeros(imm) == 0 && Integer.numberOfLeadingZeros(imm) + Integer.bitCount(imm) == 32) {
            VReg midReg = new VReg(BasicType.I32);
            irs.add(new RriMIR(RriMIR.Op.SLLIW, midReg, source, 32 - Integer.numberOfLeadingZeros(imm)));
            irs.add(new RrrMIR(RrrMIR.Op.SUBW, target, midReg, source));
            return;
        }
        VReg midReg = new VReg(BasicType.I32);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transMulRegRegI(irs, target, source, midReg);
    }

    private static void transMulRegRegF(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.MUL, target, source1, source2));
    }

    private static void transMulRegRegI(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.MULW, target, source1, source2));
    }

    private static void transSubImmRegF(List<MIR> irs, VReg target, float imm, VReg source) {
        VReg midReg = new VReg(BasicType.FLOAT);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transSubRegRegF(irs, target, midReg, source);
    }

    private static void transSubImmRegI(List<MIR> irs, VReg target, int imm, VReg source) {
        VReg midReg = new VReg(BasicType.I32);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transSubRegRegI(irs, target, midReg, source);
    }

    private static void transSubRegImmF(List<MIR> irs, VReg target, VReg source, float imm) {
        VReg midReg = new VReg(BasicType.FLOAT);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transSubRegRegF(irs, target, source, midReg);
    }

    private static void transSubRegImmI(List<MIR> irs, VReg target, VReg source, int imm) {
        if (-imm >= -2048 && -imm < 2048) {
            irs.add(new RriMIR(RriMIR.Op.ADDI, target, source, -imm));
            return;
        }
        VReg midReg = new VReg(BasicType.I32);
        MIROpHelper.loadImmToReg(irs, midReg, imm);
        transSubRegRegI(irs, target, source, midReg);
    }

    private static void transSubRegRegF(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.SUB, target, source1, source2));
    }

    private static void transSubRegRegI(List<MIR> irs, VReg target, VReg source1, VReg source2) {
        irs.add(new RrrMIR(RrrMIR.Op.SUBW, target, source1, source2));
    }
}

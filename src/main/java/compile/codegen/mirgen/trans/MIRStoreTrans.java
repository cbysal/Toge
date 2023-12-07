package compile.codegen.mirgen.trans;

import compile.codegen.mirgen.mir.*;
import compile.vir.VReg;
import compile.vir.ir.StoreVIR;
import compile.vir.type.BasicType;
import compile.symbol.DataSymbol;
import compile.symbol.Symbol;
import compile.vir.value.Value;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

public final class MIRStoreTrans {
    private static void strRsRtImm(List<MIR> irs, VReg source, VReg target, int imm) {
        if (imm >= -2048 && imm < 2048) {
            irs.add(new StoreMIR(source, target, imm, 4));
            return;
        }
        VReg midReg = new VReg(BasicType.I32, 8);
        MIRBinaryTrans.transAddRegImmI(irs, midReg, target, imm);
        irs.add(new StoreMIR(source, midReg, 0, 4));
    }

    static void transStoreGlobal(List<MIR> irs, StoreVIR storeVIR) {
        DataSymbol symbol = storeVIR.symbol;
        VReg source = storeVIR.source;
        if (symbol.isSingle()) {
            transStoreGlobalSingle(irs, source, symbol);
            return;
        }
        transStoreGlobalElement(irs, source, storeVIR.indexes, symbol);
    }

    private static void transStoreGlobalElement(List<MIR> irs, VReg source, List<Value> dimensions, DataSymbol symbol) {
        Pair<Integer, List<Pair<VReg, Integer>>> offsetRegDimensions = MIROpHelper.calcDimension(dimensions, symbol.getSizes());
        int offset = offsetRegDimensions.getLeft();
        List<Pair<VReg, Integer>> regDimensions = offsetRegDimensions.getRight();
        if (regDimensions.isEmpty()) {
            VReg midReg = new VReg(BasicType.I32, 8);
            irs.add(new LlaMIR(midReg, symbol));
            strRsRtImm(irs, source, midReg, offset);
            return;
        }
        VReg midReg1 = new VReg(BasicType.I32, 8);
        irs.add(new LlaMIR(midReg1, symbol));
        VReg midReg2 = new VReg(BasicType.I32, 8);
        MIROpHelper.addRegDimensionsToReg(irs, midReg2, regDimensions, midReg1);
        strRsRtImm(irs, source, midReg2, offset);
    }

    private static void transStoreGlobalSingle(List<MIR> irs, VReg source, DataSymbol symbol) {
        VReg midReg = new VReg(BasicType.I32, 8);
        irs.add(new LlaMIR(midReg, symbol));
        irs.add(new StoreMIR(source, midReg, 0, 4));
    }

    static void transStoreLocal(List<MIR> irs, StoreVIR storeVIR, Map<Symbol, Integer> localOffsets) {
        DataSymbol symbol = storeVIR.symbol;
        VReg source = storeVIR.source;
        int offset = localOffsets.get(symbol);
        if (storeVIR.isSingle()) {
            transStoreLocalSingle(irs, source, offset);
            return;
        }
        transStoreLocalElement(irs, source, storeVIR.indexes, symbol, offset);
    }

    private static void transStoreLocalElement(List<MIR> irs, VReg source, List<Value> dimensions, DataSymbol symbol, int offset) {
        Pair<Integer, List<Pair<VReg, Integer>>> offsetRegDimensions = MIROpHelper.calcDimension(dimensions, symbol.getSizes());
        offset += offsetRegDimensions.getLeft();
        List<Pair<VReg, Integer>> regDimensions = offsetRegDimensions.getRight();
        if (regDimensions.isEmpty()) {
            irs.add(new StoreItemMIR(StoreItemMIR.Item.LOCAL, source, offset));
            return;
        }
        VReg midReg1 = new VReg(BasicType.I32, 8);
        irs.add(new AddRegLocalMIR(midReg1, offset));
        VReg midReg2 = new VReg(BasicType.I32, 8);
        MIROpHelper.addRegDimensionsToReg(irs, midReg2, regDimensions, midReg1);
        strRsRtImm(irs, source, midReg2, 0);
    }

    private static void transStoreLocalSingle(List<MIR> irs, VReg source, int offset) {
        irs.add(new StoreItemMIR(StoreItemMIR.Item.LOCAL, source, offset));
    }

    static void transStoreParam(List<MIR> irs, StoreVIR storeVIR, Map<Symbol, Pair<Boolean, Integer>> paramOffsets) {
        DataSymbol symbol = storeVIR.symbol;
        VReg source = storeVIR.source;
        Pair<Boolean, Integer> rawOffset = paramOffsets.get(symbol);
        if (symbol.isSingle()) {
            transStoreParamSingle(irs, source, rawOffset.getLeft(), rawOffset.getRight());
            return;
        }
        transStoreParamElement(irs, source, storeVIR.indexes, symbol, rawOffset.getLeft(), rawOffset.getRight());
    }

    private static void transStoreParamElement(List<MIR> irs, VReg source, List<Value> dimensions, DataSymbol symbol, Boolean isInner, Integer offset) {
        Pair<Integer, List<Pair<VReg, Integer>>> offsetRegDimensions = MIROpHelper.calcDimension(dimensions, symbol.getSizes());
        int innerOffset = offsetRegDimensions.getLeft();
        List<Pair<VReg, Integer>> regDimensions = offsetRegDimensions.getRight();
        VReg midReg = new VReg(BasicType.I32, 8);
        irs.add(new LoadItemMIR(isInner ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg, offset));
        for (Pair<VReg, Integer> regDimension : regDimensions) {
            VReg midReg1 = new VReg(BasicType.I32, 8);
            MIROpHelper.addRtRbRsImm(irs, midReg1, midReg, regDimension.getLeft(), regDimension.getRight());
            midReg = midReg1;
        }
        strRsRtImm(irs, source, midReg, innerOffset);
    }

    private static void transStoreParamSingle(List<MIR> irs, VReg source, Boolean isInner, Integer offset) {
        irs.add(new StoreItemMIR(isInner ? StoreItemMIR.Item.PARAM_INNER : StoreItemMIR.Item.PARAM_OUTER, source, offset));
    }
}

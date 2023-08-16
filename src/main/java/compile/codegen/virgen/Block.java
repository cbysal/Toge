package compile.codegen.virgen;

import compile.codegen.Label;
import compile.codegen.virgen.vir.VIR;
import compile.codegen.virgen.vir.VIRItem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Block implements Iterable<VIR> {
    public record Cond(Type type, VIRItem left, VIRItem right) {
        public enum Type {
            EQ, GE, GT, LE, LT, NE
        }
    }

    private final Label label;
    private final List<VIR> irs = new ArrayList<>();

    public Block() {
        this.label = new Label();
    }

    public Label getLabel() {
        return label;
    }

    public boolean add(VIR ir) {
        return irs.add(ir);
    }

    public void add(int index, VIR ir) {
        irs.add(index, ir);
    }

    public boolean addAll(Block block) {
        return irs.addAll(block.irs);
    }

    public boolean addAll(int index, List<VIR> irs) {
        return this.irs.addAll(index, irs);
    }

    public VIR get(int index) {
        return irs.get(index);
    }

    public VIR getLast() {
        if (irs.isEmpty())
            return null;
        return irs.get(irs.size() - 1);
    }

    public boolean isEmpty() {
        return irs.isEmpty();
    }

    public VIR remove(int index) {
        return irs.remove(index);
    }

    public VIR set(int index, VIR ir) {
        return irs.set(index, ir);
    }

    public int size() {
        return irs.size();
    }

    @Override
    public Iterator<VIR> iterator() {
        return irs.iterator();
    }

    @Override
    public String toString() {
        return "b" + label.getId();
    }
}

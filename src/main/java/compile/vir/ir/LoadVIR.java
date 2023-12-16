package compile.vir.ir;

import compile.vir.GlobalVariable;
import compile.vir.type.PointerType;
import compile.vir.value.Value;

public class LoadVIR extends VIR {
    public final Value pointer;

    public LoadVIR(Value pointer) {
        super(switch (pointer) {
            case GlobalVariable global -> pointer.getType();
            default -> pointer.getType().baseType();
        });
        this.pointer = pointer;
    }

    @Override
    public String toString() {
        return String.format("%s = load %s, %s %s", getName(), type, switch (pointer) {
            case GlobalVariable global -> new PointerType(pointer.getType());
            default -> pointer.getType();
        }, pointer.getName());
    }
}

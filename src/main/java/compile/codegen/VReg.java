package compile.codegen;

import compile.llvm.type.BasicType;
import compile.llvm.type.Type;

public class VReg extends Reg {
    private static int counter = 0;
    private final int id;

    public VReg(Type type) {
        super(type);
        this.id = counter++;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return switch (type) {
            case BasicType.I32 -> "$i";
            case BasicType.FLOAT -> "$f";
            default -> throw new RuntimeException("Invalid type");
        } + id;
    }
}

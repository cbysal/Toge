package compile.syntax.ast;

import compile.symbol.FuncSymbol;

import java.util.List;

public record FuncCallExpAST(FuncSymbol func, List<ExpAST> params) implements ExpAST {
    @Override
    public Number calc() {
        throw new RuntimeException();
    }

    @Override
    public void print(int depth) {
        System.out.println("  ".repeat(depth) + "FuncCallExp " + func);
        params.forEach(param -> param.print(depth + 1));
    }
}

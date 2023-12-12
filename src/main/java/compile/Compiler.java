package compile;

import compile.codegen.CodeGenerator;
import compile.codegen.mirgen.MIRGenerator;
import compile.codegen.mirgen.MachineFunction;
import compile.codegen.regalloc.RegAllocator;
import compile.vir.VirtualFunction;
import compile.symbol.GlobalSymbol;
import compile.sysy.SysYLexer;
import compile.sysy.SysYParser;
import execute.Executor;
import org.antlr.v4.runtime.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class Compiler {
    private final Executor.OptionPool options;
    private final String input;
    private final Path outputFile;

    public Compiler(Executor.OptionPool options, String input, Path outputFile) {
        this.options = options;
        this.input = input;
        this.outputFile = outputFile;
    }

    public void compile() {
        SysYLexer lexer = new SysYLexer(CharStreams.fromString(input));
        CommonTokenStream commonTokenStream = new CommonTokenStream(lexer);
        SysYParser parser = new SysYParser(commonTokenStream);
        SysYParser.RootContext rootContext = parser.root();
        VIRGenerator virGenerator = new VIRGenerator(rootContext);
        Set<GlobalSymbol> globals = virGenerator.getGlobals();
        Map<String, VirtualFunction> vFuncs = virGenerator.getFuncs();
        if (options.containsKey("emit-vir"))
            emitVIR(options.get("emit-vir"), vFuncs);
        if (options.containsKey("emit-opt-vir"))
            emitVIR(options.get("emit-opt-vir"), vFuncs);
        MIRGenerator mirGenerator = new MIRGenerator(globals, vFuncs);
        globals = mirGenerator.getGlobals();
        Map<String, MachineFunction> mFuncs = mirGenerator.getFuncs();
        if (options.containsKey("emit-mir"))
            emitMIR(options.get("emit-mir"), mFuncs);
        if (options.containsKey("emit-opt-mir"))
            emitMIR(options.get("emit-opt-mir"), mFuncs);
        RegAllocator regAllocator = new RegAllocator(mFuncs);
        regAllocator.allocate();
        CodeGenerator codeGenerator = new CodeGenerator(globals, mFuncs);
        String output = codeGenerator.getOutput();
        try {
            Files.writeString(outputFile, output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void emitMIR(String filePath, Map<String, MachineFunction> funcs) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (MachineFunction func : funcs.values()) {
                writer.write(func.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void emitVIR(String filePath, Map<String, VirtualFunction> funcs) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (VirtualFunction func : funcs.values()) {
                writer.write(func.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

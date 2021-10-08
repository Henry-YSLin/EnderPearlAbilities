package io.github.henryyslin.enderpearlabilities.utils;

import java.util.function.Consumer;

public class FunctionChain {
    final Consumer<Runnable>[] chain;
    int chainIndex = -1;

    @SafeVarargs
    public FunctionChain(Consumer<Runnable>... chain) {
        this.chain = chain;
    }

    private void executeOne() {
        chainIndex++;
        if (chainIndex < chain.length)
            chain[chainIndex].accept(this::executeOne);
    }

    public void execute() {
        executeOne();
    }
}

package io.github.henryyslin.enderpearlabilities.utils;

import java.util.function.Consumer;

/**
 * Chain and execute functions sequentially to allow non-linear execution.
 */
public class FunctionChain {

    final Consumer<Runnable>[] chain;
    int chainIndex = -1;

    /**
     * Construct a function chain with the given functions.
     *
     * @param chain The list of functions to be executed.
     */
    @SafeVarargs
    public FunctionChain(Consumer<Runnable>... chain) {
        this.chain = chain;
    }

    private void executeOne() {
        chainIndex++;
        if (chainIndex < chain.length)
            chain[chainIndex].accept(this::executeOne);
    }

    /**
     * Execute the chain of functions.
     */
    public void execute() {
        executeOne();
    }
}

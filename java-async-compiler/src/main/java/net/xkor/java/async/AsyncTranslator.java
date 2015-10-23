package net.xkor.java.async;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;

public class AsyncTranslator extends TreeTranslator {
    private final JavacTools tools;

    public AsyncTranslator(JavacTools tools) {
        this.tools = tools;
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
        result = jcClassDecl;
    }

    @Override
    public void visitBlock(JCTree.JCBlock jcBlock) {
        super.visitBlock(jcBlock);
    }
}

package io.github.bluelhf.relly.util;

public abstract class Operation {
    public abstract static class Enable extends Operation {
        
    }

    public abstract static class Disable extends Operation {

    }

    public abstract static class Reload extends Operation {

    }


    public abstract OperationResult getResult();
}

package com.traneptora.jxlatte.entropy;

public class EntropyState {
    private int state;
    private boolean hasState;

    public EntropyState() {
        hasState = false;
    }

    public boolean hasState() {
        return hasState;
    }

    public int getState() {
        if (hasState)
            return state;
        throw new IllegalStateException("ANS state has not been initialized");
    }

    public void setState(int state) {
        this.state = state;
        hasState = true;
    }

    public void reset() {
        hasState = false;
    }
}

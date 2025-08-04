package com.wpmapp.flow.model;

import java.io.Serializable;

public class TypingResult implements Serializable {

    public int typedWords;
    public int correctWords;

    public TypingResult(int typedWords,int correctWords) {
        this.typedWords = typedWords;
        this.correctWords = correctWords;
    }
}

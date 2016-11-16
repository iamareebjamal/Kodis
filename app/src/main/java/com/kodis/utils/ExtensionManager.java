package com.kodis.utils;

public class ExtensionManager {
    public enum Language {
        C, PYTHON, JAVA, XML, HTML, CSS, JAVASCRIPT, PHP, TEXT, NONE
    }

    public static Language getLanguage(String extension){
        if(extension==null) return Language.NONE;

        String normalized = extension.toLowerCase();
        if(normalized.equals("c")||normalized.equals("cpp")||normalized.equals("c++")||normalized.equals("h")||normalized.equals("hpp")||normalized.equals("h++")) {
            return Language.C;
        } else if (normalized.equals("py")) {
            return Language.PYTHON;
        } else if(normalized.equals("java")) {
            return Language.JAVA;
        } else if(normalized.equals("xml")) {
            return Language.XML;
        } else if(normalized.equals("html")||normalized.equals("htm")) {
            return Language.HTML;
        } else if(normalized.equals("css")||normalized.equals("sass")||normalized.equals("less")) {
            return Language.CSS;
        } else if(normalized.equals("js")) {
            return Language.JAVASCRIPT;
        } else if(normalized.equals("php")) {
            return Language.PHP;
        } else if(normalized.equals("txt")||normalized.equals("text")) {
            return Language.TEXT;
        } else {
            return Language.NONE;
        }
    }
}

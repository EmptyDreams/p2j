package top.kmar.p2j.abi.values;

import top.kmar.p2j.abi.PhpClass;

import java.util.Map;

public abstract class PhpObject {

    private Map<String, Object> dynamicProperties;

    public Object getProperty(PhpClass scope, String name) {
        return dynamicProperties.get(name);
    }

    public int compare(PhpObject other) {
        return 0;
    }

}
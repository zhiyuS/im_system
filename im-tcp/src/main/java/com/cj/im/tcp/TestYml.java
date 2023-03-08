package com.cj.im.tcp;

import lombok.Data;

@Data
public class TestYml {
    private String name;
    private Integer age;

    @Override
    public String toString() {
        return "TestYml{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}

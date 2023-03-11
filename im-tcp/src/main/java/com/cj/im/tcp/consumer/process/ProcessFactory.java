package com.cj.im.tcp.consumer.process;

public class ProcessFactory {

    private static BaseProcess defaultBaseProcess;
    static {
        defaultBaseProcess = new BaseProcess(){

            @Override
            public void before() {

            }

            @Override
            public void after() {

            }
        };
    }

    public static BaseProcess getMessageProcess(Integer command){

        return defaultBaseProcess;
    }

}

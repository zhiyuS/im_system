package com.cj.im.common.enums.command;

public enum UserEventCommand implements Command {

    //用户修改command 4000
    USER_MODIFY(4000),


    ;

    private int command;

    UserEventCommand(int command){
        this.command=command;
    }


    @Override
    public int getCommand() {
        return command;
    }
}

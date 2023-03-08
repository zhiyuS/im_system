package com.cj.im.service.user.model.resp;

import lombok.Data;

import java.util.List;

@Data
public class ImportUserResp {
    public List<String> successId;
    public List<String> errorId;
}

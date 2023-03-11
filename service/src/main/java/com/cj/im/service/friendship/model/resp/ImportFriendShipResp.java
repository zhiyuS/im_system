package com.cj.im.service.friendship.model.resp;

import lombok.Data;

import java.util.List;

@Data
public class ImportFriendShipResp {
   public List<String> successId;
   public List<String> errorId;
}

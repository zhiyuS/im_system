package com.cj.im.common.route.algorithm.consistenthash;


import com.cj.im.common.enums.UserErrorCode;
import com.cj.im.common.exception.ApplicationException;

import java.util.SortedMap;
import java.util.TreeMap;

public class TreeMapConsistentHash extends AbstractConsistentHash {
    private static TreeMap<Long,String> treeMap = new TreeMap<>();
    private  static final int NODE_SIZE = 2;
    @Override
    protected void add(Long key, String value) {
        for (Integer i = 0; i < NODE_SIZE; i++) {
            treeMap.put(super.hash("node"+key+i),value);
        }
        treeMap.put(key,value);
    }

    @Override
    public String getFirstNodeValue(String key) {
        SortedMap<Long, String> sortedMap = treeMap.tailMap(super.hash(key));
        if(sortedMap.size() == 0){
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE);
        }
        Long aLong = sortedMap.firstKey();

        return sortedMap.get(aLong);
    }

    @Override
    protected void processBefore() {
        treeMap.clear();
    }
}

package com.example.classify_video.Util;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapUtil {
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
    public static Map<String,Float> div_Map(Map<String, Float> map, int n){
        for(String str:map.keySet()){
            map.put(str,map.get(str)/n);
        }
        return map;
    }
    public static Map<String,Float> resetMap(Map<String, Float> map){
        for(String str:map.keySet()){
            map.put(str,0f);
        }
        return map;
    }
}

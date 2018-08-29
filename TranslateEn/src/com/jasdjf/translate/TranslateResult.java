package com.jasdjf.translate;

import java.util.ArrayList;
import java.util.List;

public class TranslateResult {
    String errorCode;//错误码，为0时表示翻译成功，否则翻译失败，json字段：errorCode
    String world;//要查询的单词,json字段：query
    String translation;//查询结果，json字段：translation
    List<String> explains;//单词释义，json字段：explains
    List<WebPhrase> webPhrases;//网络短语，json字段：web
    List<Wf> wfs;//过去式，json字段：wfs

    @Override
    public String toString() {
        String strExplains = "";
        if(explains!=null){
            for (int i = 0; i < explains.size(); i++) {
                strExplains += explains.get(i)+"\r\n";
            }
        }
        String strWeb = "";
        if (webPhrases!=null) {
            for (int i = 0; i < webPhrases.size(); i++) {
                strWeb += webPhrases.get(i).toString()+"\r\n";
            }
        }
        String strWfs = "";
        if (wfs!=null) {
            for (int i = 0; i < wfs.size(); i++) {
                strWfs += wfs.get(i).toString();
            }
        }
        return world + ": " + translation + "\r\n" + strExplains + strWeb + strWfs;
    }
}

//网络短语
class WebPhrase{
    private String key;
    private List<String> value;

    public String getKey() {
        return key;
    }

    public List<String> getValue() {
        return value;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(List<String> value) {
        this.value = value;
    }

    @Override
    public String toString() {
        String str = "";
        for (int i = 0; i < value.size(); i++) {
            str += value.get(i)+";";
        }
        return key+":"+str;
    }
}

//过去式、第三人称单数等形式
class Wf{
    String name;
    String value;

    @Override
    public String toString() {
        return name+":"+value;
    }
}

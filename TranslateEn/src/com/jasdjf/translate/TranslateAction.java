package com.jasdjf.translate;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;

import java.awt.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.http.util.TextUtils;


public class TranslateAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        final Editor mEditor = e.getData(PlatformDataKeys.EDITOR);
        if (null == mEditor) {
            return;
        }
        SelectionModel model = mEditor.getSelectionModel();
        final String selectedText = model.getSelectedText();
        if (TextUtils.isEmpty(selectedText)) {
            return;
        }
        String translateResult = translate(selectedText);
        TranslateResult result = parseJsonString(translateResult);
        if(result==null){
            showPopupBalloon(mEditor,"Error!");
        } else {
            showPopupBalloon(mEditor,result.toString());
        }
    }

    private void showPopupBalloon(final Editor editor, final String result) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                JBPopupFactory factory = JBPopupFactory.getInstance();
                factory.createHtmlTextBalloonBuilder(result, null, new JBColor(new Color(186, 238, 186), new Color(73, 117, 73)), null)
                        //.setFadeoutTime(5000)
                        //.setHideOnKeyOutside(true)
                        .createBalloon()
                        .show(factory.guessBestPopupLocation(editor), Balloon.Position.below);
            }
        });
    }

    private TranslateResult parseJsonString(String strJson){
        JsonParser parse =new JsonParser();
        Gson gson=new Gson();
        JsonObject object = (JsonObject) parse.parse(strJson);
        TranslateResult translateResult = new TranslateResult();
        translateResult.errorCode = object.get("errorCode").getAsString();
        if(translateResult.errorCode.equals("0")){
            translateResult.world = object.get("query").getAsString();
            translateResult.translation = object.get("translation").getAsString();
            JsonArray webArray = object.getAsJsonArray("web");
            List<WebPhrase> webPhraseList = new ArrayList<>();
            if(webArray!=null){
                for (JsonElement element : webArray) {
                    JsonObject webParaseObj = element.getAsJsonObject();
                    if(webParaseObj!=null){
                        WebPhrase webPhrase = new WebPhrase();
                        JsonElement je = webParaseObj.get("key");
                        if(je!=null){
                            webPhrase.setKey(je.getAsString());
                            JsonArray valueArray = webParaseObj.getAsJsonArray("value");
                            if(valueArray!=null){
                                List<String> valueList = new ArrayList<>();
                                for (JsonElement tmp : valueArray) {
                                    if(tmp!=null){
                                        valueList.add(tmp.getAsString());
                                    }
                                }
                                webPhrase.setValue(valueList);
                                webPhraseList.add(webPhrase);
                            }
                        }
                    }
                }
                translateResult.webPhrases = webPhraseList;
            }
            JsonObject baseObject = object.get("basic").getAsJsonObject();
            JsonArray wfsArray = baseObject.getAsJsonArray("wfs");
            if(wfsArray!=null){
                List<Wf> wfList = new ArrayList<>();
                for (JsonElement element : wfsArray) {
                    JsonObject wfObject = element.getAsJsonObject();
                    if(wfObject!=null){
                        Wf wf = gson.fromJson(wfObject.get("wf"), new TypeToken<Wf>() {}.getType());
                        wfList.add(wf);
                    }
                }
                translateResult.wfs = wfList;
            }
            JsonArray explainArray = baseObject.getAsJsonArray("explains");
            if(explainArray!=null){
                List<String> explainList = new ArrayList<>();
                for (JsonElement element : explainArray) {
                    if(element!=null){
                        explainList.add(element.getAsString());
                    }
                }
                translateResult.explains = explainList;
            }
            return translateResult;
        } else {
            return null;
        }
    }

    private String translate(String world){
        String appKey ="3e77dcde83a20359";
        String query = world;
        String salt = String.valueOf(System.currentTimeMillis());
        String from = "auto";
        String to = "auto";
        String sign = md5(appKey + query + salt+ "I4npYQQO9x3l37hZGXicUfB8swzIrR8L");
        Map params = new HashMap();
        params.put("q", query);
        params.put("from", from);
        params.put("to", to);
        params.put("sign", sign);
        params.put("salt", salt);
        params.put("appKey", appKey);
        String result = requestForHttp("http://openapi.youdao.com/api", params);
        System.out.println(requestForHttp("http://openapi.youdao.com/api", params));
        return result;
    }

    public String requestForHttp(String url,Map requestParams){
        CloseableHttpResponse httpResponse = null;
        String result = null;
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            /**HttpPost*/
            HttpPost httpPost = new HttpPost(url);
            //System.out.println(new JSONObject(requestParams).toString());
            List params = new ArrayList();
            Iterator it = requestParams.entrySet().iterator();
            while (it.hasNext()) {
                Entry en = (Entry) it.next();
                String key = (String) en.getKey();
                String value = (String) en.getValue();
                if (value != null) {
                    params.add(new BasicNameValuePair(key, value));
                }
            }
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            /**HttpResponse*/
            httpResponse = httpClient.execute(httpPost);

            HttpEntity httpEntity = httpResponse.getEntity();
            result = EntityUtils.toString(httpEntity, "utf-8");
            EntityUtils.consume(httpEntity);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (httpResponse != null) {
                    httpResponse.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 生成32位MD5摘要
     * @param string
     * @return
     */
    public static String md5(String string) {
        if(string == null){
            return null;
        }
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F'};

        try{
            byte[] btInput = string.getBytes("utf-8");
            /** 获得MD5摘要算法的 MessageDigest 对象 */
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            /** 使用指定的字节更新摘要 */
            mdInst.update(btInput);
            /** 获得密文 */
            byte[] md = mdInst.digest();
            /** 把密文转换成十六进制的字符串形式 */
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        }catch(NoSuchAlgorithmException | UnsupportedEncodingException e){
            return null;
        }
    }

    /**
     * 根据api地址和参数生成请求URL
     * @param url
     * @param params
     * @return
     */
    /*public static String getUrlWithQueryString(String url, Map params) {
        if (params == null) {
            return url;
        }

        StringBuilder builder = new StringBuilder(url);
        if (url.contains("?")) {
            builder.append("&");
        } else {
            builder.append("?");
        }

        int i = 0;
        for (String key : params.keySet()) {
            String value = params.get(key);
            if (value == null) { // 过滤空的key
                continue;
            }

            if (i != 0) {
                builder.append('&');
            }

            builder.append(key);
            builder.append('=');
            builder.append(encode(value));

            i++;
        }

        return builder.toString();
    }*/
    /**
     * 进行URL编码
     * @param input
     * @return
     */
    public static String encode(String input) {
        if (input == null) {
            return "";
        }

        try {
            return URLEncoder.encode(input, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return input;
    }
}

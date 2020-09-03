package com.nlscan.android.tempertest.util;



import android.os.Environment;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostUtil
{

    /**
     * 发送请求
     * @param url
     * @param params
     * @param encode
     * @param ifJson
     * @return
     */
    public static String sendPost(String url, Map<String,String> params, String encode,boolean ifJson)
    {
    String data = ifJson ? getRequestJSON(params, encode)  :  getRequestData(params, encode);//获得请求体
    //System.out.print(data);
    PrintWriter out = null;
    BufferedReader in = null;
    String result = "";
    try
    {

        URL realUrl = new URL(url);
        // 打开和URL之间的连接
        URLConnection conn = realUrl.openConnection();
        // 设置通用的请求属性
        conn.setRequestProperty("accept", "*/*");
        conn.setRequestProperty("connection", "Keep-Alive");
        if (ifJson) conn.setRequestProperty("Content-type","application/json");
        conn.setRequestProperty("user-agent",
                "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
        // 发送POST请求必须设置如下两行
        conn.setDoOutput(true);
        conn.setDoInput(true);
        // 获取URLConnection对象对应的输出流
        out = new PrintWriter(conn.getOutputStream());
        // 发送请求参数
        out.print(data);  // 向服务端输出参数
        // flush输出流的缓冲
        out.flush();
        // 定义BufferedReader输入流来读取URL的响应
        in = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = in.readLine()) != null)
        {
            result += "\n" + line;
        }
    }
    catch (Exception e)
    {
        System.out.println("发送POST请求出现异常！" + e);
        e.printStackTrace();
    }
    // 使用finally块来关闭输出流、输入流
    finally
    {
        try
        {
            if (out != null)
            {
                out.close();
            }
            if (in != null)
            {
                in.close();
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }
    return result;
    }


    /**
     * 获取json请求体
     * @param params
     * @param encode
     * @return
     */
    private static String getRequestJSON(Map<String, String> params, String encode)
    {

        JSONObject ClientKey = new JSONObject();

        try {
            for(Map.Entry<String, String> entry : params.entrySet()) {

                ClientKey.put(entry.getKey(),entry.getValue());

            }
        } catch (Exception e) {

            e.printStackTrace();
        }
        return String.valueOf(ClientKey);
    }



    /**
     * 获得请求体
     * @param params
     * @param encode
     * @return
     */
    private static String getRequestData(Map<String, String> params, String encode)
    {
      StringBuffer stringBuffer = new StringBuffer();        //存储封装好的请求体信息
      try {
          for(Map.Entry<String, String> entry : params.entrySet()) {
              stringBuffer.append(entry.getKey())
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), encode))
                    .append("&");
          }
          stringBuffer.deleteCharAt(stringBuffer.length() - 1);    //删除最后的一个"&"
      } catch (Exception e) {

        e.printStackTrace();
      }
      return stringBuffer.toString();
    }


    /**
     * 解析返回的json结果
     * @param result
     * @param jsonKey
     * @return
     */
    public static String parseJsonStr(String result , String jsonKey ,boolean ifObject)
    {

        String resultStr = "";

        try {
            JSONObject jsonObject = (JSONObject) JSON.parse(result);
            resultStr =  ifObject ?  jsonObject.getJSONObject(jsonKey).toString()
                    :jsonObject.getString(jsonKey);
        } catch (Exception e) {
            e.printStackTrace();
        }


        return resultStr;
    }





    /**
     * 解析json数据数组
     * @param result
     * @return
     */
    public static List<Map<String,String>> parseJson(String result,List<String> fields ,String arrayStr)
    {

//        JSONArray jsonArray = JSON.parseArray(result);
        //JSONArray jsonArray1 = JSONArray.parseArray(JSON_ARRAY_STR);//因为JSONArray继承了JSON，所以这样也是可以的

        List<Map<String,String>> mailInfoList = new ArrayList<>();

        try {
            JSONObject jsonResult = (JSONObject) JSON.parse(result);
            if ("0000".equals(jsonResult.getString("result"))) {
                JSONArray jsonArray = JSON.parseArray(jsonResult.getString(arrayStr));
                for (Object obj : jsonArray) {
                    JSONObject jsonObject = (JSONObject) obj;

                    Map<String,String> map = new HashMap<>();
                    for (String field : fields){
                        map.put(field,jsonObject.getString(field));
                    }

                    mailInfoList.add(map);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return mailInfoList;
    }





    /**
     * 下载网络文件
     * @param urlStr  请求的文件链接
     * @param cachePath 保存的路径
     * @return 返回文件的位置path
     */
    public static String getDownloadFile2Cache(String urlStr,String cachePath)
    {

        OutputStream output=null;
        String [] strArray = urlStr.split("/");
        String fileName = strArray[strArray.length - 1];

        String pathName = "";

        try {
            /*
             * 通过URL取得HttpURLConnection
             * 要网络连接成功，需在AndroidMainfest.xml中进行权限配置
             * <uses-permission android:name="android.permission.INTERNET" />
             */
            URL url=new URL(urlStr);
            HttpURLConnection conn=(HttpURLConnection)url.openConnection();
            //取得inputStream，并将流中的信息写入SDCard

            String SDCard= Environment.getExternalStorageDirectory()+"";
            pathName=SDCard+"/"+cachePath+"/"+fileName;//文件存储路径

            File file=new File(pathName);
            InputStream input=conn.getInputStream();
            if(file.exists()){
                Log.d(Constants.TAG,"exits");
            }else{
                String dir=SDCard+"/"+cachePath;
                new File(dir).mkdir();//新建文件夹
                file.createNewFile();//新建文件
                output=new FileOutputStream(file);
                //读取大文件
                byte[] buffer=new byte[4*1024];
                while(input.read(buffer)!=-1){
                    output.write(buffer);
                }
                output.flush();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            closeStream(output);
        }

        return pathName;
    }


    /**
     * 上传文件至服务器
     * @param uploadUrl
     * @param uploadFile
     * @return
     */
    public static String upload(String uploadUrl, String uploadFile) {
        String fileName = "";
        int pos = uploadFile.lastIndexOf("/");
        if (pos >= 0) {
            fileName = uploadFile.substring(pos + 1);
            Log.d(Constants.TAG,"fileName is " + fileName);
        }

        String end = "\r\n";
        String Hyphens = "--";
        String boundary = "**********";
        try {
            URL url = new URL(uploadUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Charset", "UTF-8");
            conn.setRequestProperty("Content-Type",
                    "multipart/form-data;boundary=" + boundary);

            DataOutputStream ds = new DataOutputStream(conn.getOutputStream());
            ds.writeBytes(Hyphens + boundary + end);
            ds.writeBytes("Content-Disposition: form-data; "
                    + "name=\"upFile\";filename=\"" + fileName + "\"" + end);
            ds.writeBytes(end);
            FileInputStream fStream = new FileInputStream(uploadFile);
            // 每次写入1024字节
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int length = -1;
            // 将文件数据写入到缓冲区
            while((length = fStream.read(buffer)) != -1) {
                ds.write(buffer, 0, length);
            }
            ds.writeBytes(end);
            ds.writeBytes(Hyphens + boundary + Hyphens + end);
            fStream.close();
            ds.flush();
            // 获取返回内容
            InputStream is = conn.getInputStream();
            int ch;
            StringBuffer b = new StringBuffer();
            while ((ch = is.read()) != -1) {
                b.append((char) ch);
            }
            closeStream(ds);
//            return "SUCC";
            return b.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "上传失败:" + e.getMessage();
        }
    }


    /**
     * 关闭流
     * @param closeable
     */
    private static void closeStream(Closeable closeable){
        if (closeable != null){
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}



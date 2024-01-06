package com.spms.common;

import com.alibaba.fastjson.JSONObject;
import com.ccsp.common.core.exception.ZAYKException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * @project ccsp
 * @description httpclient工具类
 * @author 18853
 * @date 2024/1/3 08:57:52
 * @version 1.0
 */
public class HttpClientUtil {

    /**传递Json格式的post请求*/
    public static String sendPostJson(String url, Map<String,Object> params ,String authorization) throws ZAYKException {
        CloseableHttpClient httpClient = null;
        HttpPost httpPost = null;
        CloseableHttpResponse response = null;
        String result = "";
        try {
            //httpClient = HttpClientBuilder.create().build();
             httpClient = getScontractHttpClient();
            httpPost = new HttpPost(url);
            // 设置参数
            if (null != params && params.size() > 0){
                String paramJson = JSONObject.toJSONString(params);
                StringEntity stringEntity = new StringEntity(paramJson,"utf-8");
                stringEntity.setContentType("application/json;charset=utf-8");
                httpPost.setEntity(stringEntity);
                httpPost.setHeader("Authorization", authorization);
            }
            response = httpClient.execute(httpPost);
            result = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            e.printStackTrace();
            throw new ZAYKException(e.getMessage()+url.split("//")[1].split("/")[0]);
        }finally {
            close(response,httpPost,httpClient);
        }
        return result;
    }

    public static void close(CloseableHttpResponse response, HttpRequestBase httpRequestBase, CloseableHttpClient httpClient){
        try {
            if (response != null) {
                response.close();
            }

            if (httpRequestBase!= null){
                httpRequestBase.releaseConnection();
            }

            if (httpClient != null){
                httpClient.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //绕过证书认证
    public static CloseableHttpClient getScontractHttpClient() {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    return true;
                }
            }).build();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        //创建httpClient
        return HttpClients.custom().setSSLContext(sslContext).
                setSSLHostnameVerifier(new NoopHostnameVerifier()).build();

    }
}

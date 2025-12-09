package com.webtool;

public class RequestPool {
    public String url;
    public  String method;
    public  String headers;
    public void setUrl(String url){
     this.url=url;
    }

    public void setMethod(String method){
     this.method=method;
    }

    public void setHeaders(String headers){
    this.headers=headers;
    }

    public String getUrl(){
        return url;
    }
    public String getMethod(){
       return method;
    }
    public String getHeaders(){
      return headers;
    }
}

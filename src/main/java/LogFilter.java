import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

class LogModel {
    String timestamp;
    String httpMethod;
    String endPoint;
    Map<String, Object> request;
    int responseCode;

    public LogModel(String timestamp, String httpMethod, String endPoint, Map<String, Object> request) {
        this.timestamp = timestamp;
        this.httpMethod = httpMethod;
        this.endPoint = endPoint;
        this.request = request;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }
}

public class LogFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws
            IOException, ServletException {
        response.setContentType("application/json");
        try {
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse res = (HttpServletResponse) response;

            Gson gson = new Gson();

            String date = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss aa").format(new Date());
            String reqString = req.getServletPath();

            Enumeration<String> headerElements = req.getHeaderNames();

            Map<String, Object> requestMap = new HashMap<>();
            Map<String, Object> headerMap = new HashMap<String, Object>();

            while (headerElements.hasMoreElements()) {
                String key = headerElements.nextElement();
                headerMap.put(key, req.getHeader(key));
            }

            requestMap.put("header", headerMap);
            JsonObject json = null;

            try {
                json = gson.fromJson(req.getReader(), JsonObject.class);
            } catch (Exception e) {
                System.out.println(e.toString());
            }

            if (((HttpServletRequest) req).getMethod()
                    .equals("POST") || ((HttpServletRequest) req).getMethod().equals("PUT")) {
                if (json != null) {
                    requestMap.put("body", json.asMap());
                    req.setAttribute("reqJson", json);
                } else {
                    BufferedReader br = req.getReader();
                    StringBuilder sb = new StringBuilder();
                    String line;

                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    requestMap.put("body", sb.toString());
                }
            }

            String reqMethod = req.getMethod();
            String protocol = req.getProtocol();
            LogModel log = new LogModel(date, reqMethod, reqString, requestMap);

            filterChain.doFilter(req, res);

            log.setResponseCode(res.getStatus());

            String logJson;
            JsonArray jsonArray = new JsonArray(1);

            try{
                JsonArray temp = gson.fromJson(
                        new FileReader("/Users/vasi-tt0524/eclipse-workspace/InvoiceApp/Log/log.json"),
                        JsonArray.class);
                jsonArray = temp != null ? temp : jsonArray;
            }catch (Exception e){
                System.out.println(e.toString());
            }

            jsonArray.add(gson.toJsonTree(log));

            FileWriter fileWriter = new FileWriter("/Users/vasi-tt0524/eclipse-workspace/InvoiceApp/Log/log.json");

            fileWriter.write(gson.toJson(jsonArray));
            fileWriter.close();
            System.out.println("test");

        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.PrintWriter;
import java.io.Reader;
import java.sql.*;

public class Util {
    static Connection con;
    static JsonObject data;
    static Statement stmt;

    public static void initGson(Reader json){
        data = new Gson().fromJson(json, JsonObject.class);
    }

    public static JsonElement get(String key){
        return data.get(key);
    }

    public static void initDB() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/InvoiceApp", "root", "");
            stmt = con.createStatement();
            con.setAutoCommit(false);
        } catch (Exception e) {
            System.out.println(e.toString());

        }
    }
}


class Response{
    boolean success;
    String payload;
    Error error;

    public Response(boolean success, String payload) {
        this.success = success;
        this.payload = payload;
    }

    public Response(boolean success, String payload, Error error) {
        this.success = success;
        this.payload = payload;
        this.error = error;
    }

    public Response(boolean success, Error error) {
        this.error = error;
        this.success = success;
    }
}

class Error{
    int code;
    String message;

    public Error(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

class ValidationException extends Exception{
    public ValidationException(String message) {
        super(message);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
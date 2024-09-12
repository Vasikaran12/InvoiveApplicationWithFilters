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
